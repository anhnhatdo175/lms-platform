package com.example.lms.ai.session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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
 * Tạo nhiệm vụ GENERATED (SHORT_ANSWER) qua AI service. Độ khó theo ZPD ({@link ZpdPolicy}), kỹ năng yếu nhất trước.
 * Chọn nhiệm vụ BANK / MULTIPLE_CHOICE thuộc gói Recommender, chưa có ở đây.
 */
@Component
public class AiSessionTaskProducer implements SessionTaskProducer {

    private final AiServiceClient client;
    private final TaskVerifier verifier;
    private final LearnerContextProvider learners;
    private final KnowledgeScopeProvider knowledge;

    public AiSessionTaskProducer(AiServiceClient client, TaskVerifier verifier, LearnerContextProvider learners,
            KnowledgeScopeProvider knowledge) {
        this.client = client;
        this.verifier = verifier;
        this.learners = learners;
        this.knowledge = knowledge;
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
        List<String> ordered = new ArrayList<>(skills);
        ordered.sort(Comparator.comparingDouble(s -> {
            LearnerContextProvider.SkillMastery m = mastery.get(s);
            return m == null ? ZpdPolicy.COLD_START_MASTERY : ZpdPolicy.effectiveMastery(m.mastery(), m.evidenceCount());
        }));

        List<TaskDraft> drafts = new ArrayList<>();
        List<Map<String, Object>> trace = new ArrayList<>();
        String backend = "unknown";
        for (int position = 1; position <= session.taskCount(); position++) {
            String skill = ordered.get((position - 1) % ordered.size());
            LearnerContextProvider.SkillMastery m = mastery.get(skill);
            double mastered = m == null ? ZpdPolicy.COLD_START_MASTERY : m.mastery();
            int evidence = m == null ? 0 : m.evidenceCount();
            Difficulty difficulty = ZpdPolicy.target(mastered, evidence, min, max);

            AiServiceClient.GenerateOutcome outcome = callAi(session, documents, skill, difficulty, mastered, evidence);
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
            entry.put("supportingDocIds", outcome.supportingDocIds());
            trace.add(entry);
        }
        Map<String, Object> retrieval = new LinkedHashMap<>();
        retrieval.put("allowedDocumentIds", documents);
        retrieval.put("tasks", trace);
        return new Result(drafts, "ai-service/" + backend, JsonSupport.toJson(retrieval));
    }

    private AiServiceClient.GenerateOutcome callAi(SessionSnapshot s, List<Long> documents, String skill,
            Difficulty difficulty, double mastery, int evidence) {
        Map<String, Object> skillState = new LinkedHashMap<>();
        skillState.put("mastery", ZpdPolicy.effectiveMastery(mastery, evidence));
        skillState.put("evidenceCount", evidence);
        Map<String, Object> skillsMap = new LinkedHashMap<>();
        skillsMap.put(skill, skillState);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("skills", skillsMap);
        try {
            return client.generate(new AiServiceClient.GenerateCall(skill, List.of(s.courseId()), documents, skill,
                    ZpdPolicy.numeric(difficulty), "vi", profile));
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
