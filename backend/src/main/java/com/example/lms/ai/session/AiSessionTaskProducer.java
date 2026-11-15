package com.example.lms.ai.session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.lms.ai.AdaptiveTaskType;
import com.example.lms.ai.SessionErrorCode;
import com.example.lms.ai.TaskOrigin;
import com.example.lms.ai.ValidationStatus;
import com.example.lms.contract.question.Difficulty;

/**
 * Tạo nhiệm vụ GENERATED (SHORT_ANSWER) qua AI service theo hồ sơ người học:
 * <ol>
 * <li>xếp hạng kỹ năng bằng Score_ZPD ({@link SkillPlanner}); độ khó sinh = M + δ trong khoảng Teacher;</li>
 * <li>dựng câu truy vấn theo hồ sơ ({@link ProfileQuery}): kỹ năng + đề mẫu của Course + trọng tâm theo mức thành
 * thạo;</li>
 * <li>gửi kèm hồ sơ kỹ năng của phiên; ghi lý do xếp hạng vào {@code retrieval_trace_json}.</li>
 * </ol>
 * Chọn nhiệm vụ BANK / MULTIPLE_CHOICE thuộc gói Recommender, chưa có ở đây.
 */
@Component
public class AiSessionTaskProducer implements SessionTaskProducer {

    private final AiServiceClient client;
    private final TaskVerifier verifier;
    private final LearnerContextProvider learners;
    private final KnowledgeScopeProvider knowledge;
    private final SkillExamplesProvider examples;

    static final int EXAMPLES_PER_SKILL = 3;

    public AiSessionTaskProducer(AiServiceClient client, TaskVerifier verifier, LearnerContextProvider learners,
            KnowledgeScopeProvider knowledge, SkillExamplesProvider examples) {
        this.client = client;
        this.verifier = verifier;
        this.learners = learners;
        this.knowledge = knowledge;
        this.examples = examples;
    }

    @Override
    public Result produce(SessionSnapshot session) {
        return produce(session, () -> {
        });
    }

    @Override
    public Result produce(SessionSnapshot session, Runnable heartbeat) {
        Map<String, Object> constraints = JsonSupport.object(session.constraintsJson());
        List<String> allowedSkills = JsonSupport.stringsOf(constraints.get("allowedSkills"));
        List<String> requestedTopics = JsonSupport.stringList(session.requestedTopicsJson());
        List<String> skills = requestedTopics.isEmpty() ? allowedSkills
                : requestedTopics.stream().filter(allowedSkills::contains).toList();
        if (skills.isEmpty()) {
            throw new SessionFailure(SessionErrorCode.NO_CANDIDATES, "No skill available for this request");
        }
        List<String> types = JsonSupport.stringList(session.requestedTaskTypesJson());
        if (types.isEmpty()) {
            types = JsonSupport.stringsOf(constraints.get("allowedTaskTypes"));
        }
        if (!types.contains(AdaptiveTaskType.SHORT_ANSWER.name())) {
            throw new SessionFailure(SessionErrorCode.NO_CANDIDATES, "No candidate tasks for the requested task types");
        }
        Difficulty min = Difficulty.valueOf(String.valueOf(constraints.getOrDefault("minDifficulty", "EASY")));
        Difficulty max = Difficulty.valueOf(String.valueOf(constraints.getOrDefault("maxDifficulty", "HARD")));
        int maxHints = ((Number) constraints.getOrDefault("maxHintsPerTask", 3)).intValue();
        boolean socratic = Boolean.TRUE.equals(constraints.get("socraticEnabled"));

        List<Long> documents = knowledge.readyDocumentIds(session.courseId(), session.lessonId());
        if (documents.isEmpty()) {
            throw new SessionFailure(SessionErrorCode.INSUFFICIENT_EVIDENCE, "No indexed documents in scope");
        }

        Map<String, LearnerContextProvider.SkillMastery> mastery = learners.skills(session.studentId());
        List<SkillPlanner.SkillPlan> ranked = SkillPlanner.rank(skills, mastery, min, max);
        Map<String, Object> profile = profileSnapshot(ranked);
        Map<String, String> queries = new LinkedHashMap<>();
        for (SkillPlanner.SkillPlan plan : ranked) {
            queries.put(plan.skill(), ProfileQuery.build(plan,
                    examples.examples(session.courseId(), plan.skill(), EXAMPLES_PER_SKILL)));
        }

        List<TaskDraft> drafts = new ArrayList<>();
        List<Map<String, Object>> trace = new ArrayList<>();
        String backend = "unknown";
        for (int position = 1; position <= session.taskCount(); position++) {
            SkillPlanner.SkillPlan plan = ranked.get((position - 1) % ranked.size());
            String skill = plan.skill();
            Difficulty difficulty = plan.difficulty();
            int evidence = plan.evidenceCount();

            AiServiceClient.GenerateOutcome outcome = callAi(session, documents, plan, queries.get(skill), profile);
            heartbeat.run();
            backend = outcome.llmBackend();
            if (!outcome.sufficient()) {
                throw new SessionFailure(SessionErrorCode.INSUFFICIENT_EVIDENCE, "Not enough evidence for " + skill);
            }
            List<String> errors = verifier.verify(outcome);
            if (!errors.isEmpty()) {
                throw new SessionFailure(SessionErrorCode.LLM_INVALID_OUTPUT, "Generated task rejected: " + errors);
            }
            drafts.add(toDraft(position, skill, difficulty, outcome, evidence, socratic, maxHints));
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("position", position);
            entry.put("skill", skill);
            entry.put("difficulty", difficulty);
            entry.put("query", queries.get(skill));
            entry.put("supportingDocIds", outcome.supportingDocIds());
            trace.add(entry);
        }
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (SkillPlanner.SkillPlan p : ranked) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("skill", p.skill());
            r.put("mastery", p.mastery());
            r.put("evidenceCount", p.evidenceCount());
            r.put("coldStart", p.coldStart());
            r.put("difficulty", p.difficulty());
            r.put("delta", Math.round(p.delta() * 1000) / 1000.0);
            r.put("zpdScore", p.zpdScore());
            ranking.add(r);
        }
        Map<String, Object> retrieval = new LinkedHashMap<>();
        retrieval.put("skillRanking", ranking);
        retrieval.put("allowedDocumentIds", documents);
        retrieval.put("tasks", trace);
        return new Result(drafts, "ai-service/" + backend, JsonSupport.toJson(retrieval));
    }

    /** Hồ sơ gửi AI service: mastery hiệu lực của mọi kỹ năng trong phiên (không có dữ liệu định danh). */
    static Map<String, Object> profileSnapshot(List<SkillPlanner.SkillPlan> ranked) {
        Map<String, Object> skillsMap = new LinkedHashMap<>();
        for (SkillPlanner.SkillPlan p : ranked) {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("mastery", p.mastery());
            state.put("evidenceCount", p.evidenceCount());
            state.put("coldStart", p.coldStart());
            skillsMap.put(p.skill(), state);
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("skills", skillsMap);
        return profile;
    }

    private AiServiceClient.GenerateOutcome callAi(SessionSnapshot s, List<Long> documents,
            SkillPlanner.SkillPlan plan, String query, Map<String, Object> profile) {
        try {
            return client.generate(new AiServiceClient.GenerateCall(query, List.of(s.courseId()), documents,
                    plan.skill(), ZpdPolicy.numeric(plan.difficulty()), "vi", profile));
        } catch (AiServiceClient.AiServiceException e) {
            SessionErrorCode code = e.kind() == AiServiceClient.AiServiceException.Kind.INVALID_OUTPUT
                    ? SessionErrorCode.LLM_INVALID_OUTPUT : SessionErrorCode.AI_UNAVAILABLE;
            throw new SessionFailure(code, e.getMessage(), e);
        }
    }

    private static TaskDraft toDraft(int position, String skill, Difficulty difficulty,
            AiServiceClient.GenerateOutcome o, int evidence, boolean socratic, int maxHints) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("prompt", o.prompt());
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("responseType", o.responseType());
        answer.put("answer", o.answerKey());
        String reason = o.personalizationReason() == null ? "" : o.personalizationReason().trim();
        if (ZpdPolicy.isColdStart(evidence)) {
            reason = ("Chưa đủ dữ liệu về kỹ năng này nên bắt đầu ở mức vừa sức. " + reason).trim();
        }
        String hints = socratic && !o.hints().isEmpty()
                ? JsonSupport.toJson(o.hints().subList(0, Math.min(maxHints, o.hints().size()))) : null;
        return new TaskDraft(position, TaskOrigin.GENERATED, null, AdaptiveTaskType.SHORT_ANSWER, skill, difficulty,
                JsonSupport.toJson(content), JsonSupport.toJson(answer), null, reason, (BigDecimal) null, hints,
                JsonSupport.toJson(o.supportingDocIds()), ValidationStatus.VALID);
    }
}
