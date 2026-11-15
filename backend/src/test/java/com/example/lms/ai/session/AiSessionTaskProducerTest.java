package com.example.lms.ai.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.example.lms.ai.SessionErrorCode;
import com.example.lms.ai.ValidationStatus;
import com.example.lms.contract.question.Difficulty;

class AiSessionTaskProducerTest {

    private static final String CONSTRAINTS = "{\"allowedSkills\":[\"frac\",\"geo\"],\"minDifficulty\":\"EASY\","
            + "\"maxDifficulty\":\"HARD\",\"allowedTaskTypes\":[\"SHORT_ANSWER\"],\"socraticEnabled\":false,"
            + "\"maxHintsPerTask\":3}";

    private final List<AiServiceClient.GenerateCall> calls = new ArrayList<>();
    private Function<AiServiceClient.GenerateCall, AiServiceClient.GenerateOutcome> ai = c -> good(c);
    private Map<String, LearnerContextProvider.SkillMastery> skills = Map.of();
    private List<Long> docs = List.of(11L, 12L);

    private static AiServiceClient.GenerateOutcome good(AiServiceClient.GenerateCall c) {
        return new AiServiceClient.GenerateOutcome(true, "Tính 12 + 30.", 42, "numeric", List.of("Cộng hai số."),
                "Luyện kỹ năng " + c.targetSkill(), List.of("11:0"), List.of("11:0", "11:1"), "fake");
    }

    private SessionSnapshot session(int count, String topics, String types, String constraints) {
        return new SessionSnapshot(1, 7, 10, null, 5L, count, constraints, topics, types, 1, 0);
    }

    private AiSessionTaskProducer producer() {
        AiServiceClient client = call -> {
            calls.add(call);
            return ai.apply(call);
        };
        return new AiSessionTaskProducer(client, new StructuralTaskVerifier(), id -> skills, (c, l) -> docs);
    }

    // ---------- ZPD ----------
    @Test
    void zpdTargetsMasteryPlusDeltaAndClampsToTeacherRange() {
        assertEquals(Difficulty.MEDIUM, ZpdPolicy.target(0.40, 10, Difficulty.EASY, Difficulty.HARD)); // 0.5
        assertEquals(Difficulty.EASY, ZpdPolicy.target(0.10, 10, Difficulty.EASY, Difficulty.HARD)); // 0.2
        assertEquals(Difficulty.HARD, ZpdPolicy.target(0.70, 10, Difficulty.EASY, Difficulty.HARD)); // 0.8
        assertEquals(Difficulty.MEDIUM, ZpdPolicy.target(0.10, 10, Difficulty.MEDIUM, Difficulty.HARD)); // clamp up
        assertEquals(Difficulty.MEDIUM, ZpdPolicy.target(0.95, 10, Difficulty.EASY, Difficulty.MEDIUM)); // clamp down
    }

    @Test
    void coldStartTreatsMasteryAsNeutral() {
        assertTrue(ZpdPolicy.isColdStart(2));
        assertFalse(ZpdPolicy.isColdStart(3));
        assertEquals(0.5, ZpdPolicy.effectiveMastery(0.99, 2));
        assertEquals(Difficulty.MEDIUM, ZpdPolicy.target(0.99, 0, Difficulty.EASY, Difficulty.HARD));
    }

    // ---------- producer ----------
    @Test
    void weakestSkillFirstAndDifficultyFollowsMastery() {
        skills = Map.of("frac", new LearnerContextProvider.SkillMastery(0.15, 8),
                "geo", new LearnerContextProvider.SkillMastery(0.80, 8));
        var result = producer().produce(session(2, null, null, CONSTRAINTS));
        assertEquals(2, result.tasks().size());
        assertEquals("frac", result.tasks().get(0).skillCode());
        assertEquals(Difficulty.EASY, result.tasks().get(0).difficulty());
        assertEquals("geo", result.tasks().get(1).skillCode());
        assertEquals(Difficulty.HARD, result.tasks().get(1).difficulty());
        assertEquals(ValidationStatus.VALID, result.tasks().get(0).validationStatus());
        assertEquals("ai-service/fake", result.pipelineVersion());
    }

    @Test
    void permissionScopeIsSentToTheAiService() {
        producer().produce(session(1, null, null, CONSTRAINTS));
        assertEquals(List.of(10L), calls.get(0).allowedCourseIds());
        assertEquals(List.of(11L, 12L), calls.get(0).allowedDocumentIds());
    }

    @Test
    void coldStartReasonSaysThereIsNotEnoughData() {
        var result = producer().produce(session(1, null, null, CONSTRAINTS));
        assertTrue(result.tasks().get(0).personalizationReason().startsWith("Chưa đủ dữ liệu"));
    }

    @Test
    void answerKeyStaysInAnswerKeyJsonOnly() {
        var t = producer().produce(session(1, null, null, CONSTRAINTS)).tasks().get(0);
        assertFalse(t.contentJson().contains("42"));
        assertTrue(t.answerKeyJson().contains("42"));
    }

    @Test
    void requestedTopicsNarrowTheSkills() {
        var result = producer().produce(session(2, "[\"geo\"]", null, CONSTRAINTS));
        assertEquals("geo", result.tasks().get(0).skillCode());
        assertEquals("geo", result.tasks().get(1).skillCode());
    }

    @Test
    void noIndexedDocumentsMeansInsufficientEvidence() {
        docs = List.of();
        assertFailure(SessionErrorCode.INSUFFICIENT_EVIDENCE, session(1, null, null, CONSTRAINTS));
    }

    @Test
    void aiReportingInsufficientContextFailsWithoutRetryClass() {
        ai = c -> AiServiceClient.GenerateOutcome.insufficient(List.of(), "fake");
        assertFailure(SessionErrorCode.INSUFFICIENT_EVIDENCE, session(1, null, null, CONSTRAINTS));
    }

    @Test
    void unsupportedTaskTypeHasNoCandidates() {
        assertFailure(SessionErrorCode.NO_CANDIDATES, session(1, null, "[\"MULTIPLE_CHOICE\"]", CONSTRAINTS));
    }

    @Test
    void noMatchingSkillHasNoCandidates() {
        assertFailure(SessionErrorCode.NO_CANDIDATES, session(1, "[\"algebra\"]", null, CONSTRAINTS));
    }

    @Test
    void serviceErrorsMapToTransientCodes() {
        ai = c -> {
            throw new AiServiceClient.AiServiceException(AiServiceClient.AiServiceException.Kind.UNAVAILABLE, "down");
        };
        assertFailure(SessionErrorCode.AI_UNAVAILABLE, session(1, null, null, CONSTRAINTS));
        ai = c -> {
            throw new AiServiceClient.AiServiceException(AiServiceClient.AiServiceException.Kind.INVALID_OUTPUT, "bad");
        };
        assertFailure(SessionErrorCode.LLM_INVALID_OUTPUT, session(1, null, null, CONSTRAINTS));
    }

    @Test
    void verifierRejectsLeakedAnswerAndFabricatedEvidence() {
        var verifier = new StructuralTaskVerifier();
        var leaky = new AiServiceClient.GenerateOutcome(true, "Tính 12 + 30.", 42, "numeric",
                List.of("Đáp án là 42."), "ok", List.of("11:0"), List.of("11:0"), "fake");
        assertTrue(verifier.verify(leaky).contains("hint reveals the answer"));
        var fabricated = new AiServiceClient.GenerateOutcome(true, "Tính", 42, "numeric", List.of(), "ok",
                List.of("99:9"), List.of("11:0"), "fake");
        assertTrue(verifier.verify(fabricated).contains("evidence is not among retrieved chunks"));
        var noAnswer = new AiServiceClient.GenerateOutcome(true, "Tính", null, "numeric", List.of(), "ok",
                List.of("11:0"), List.of("11:0"), "fake");
        assertTrue(verifier.verify(noAnswer).contains("answer key is missing"));
        var valid = new AiServiceClient.GenerateOutcome(true, "Tính 12 + 30.", 42, "numeric", List.of("Cộng."),
                "ok", List.of("11:0"), List.of("11:0"), "fake");
        assertTrue(verifier.verify(valid).isEmpty());
    }

    @Test
    void rejectedTaskFailsTheSessionAsInvalidOutput() {
        ai = c -> new AiServiceClient.GenerateOutcome(true, "Tính", 42, "numeric", List.of("42"), "ok",
                List.of("11:0"), List.of("11:0"), "fake");
        assertFailure(SessionErrorCode.LLM_INVALID_OUTPUT, session(1, null, null, CONSTRAINTS));
    }

    private void assertFailure(SessionErrorCode expected, SessionSnapshot s) {
        try {
            producer().produce(s);
        } catch (SessionFailure f) {
            assertEquals(expected, f.code());
            return;
        }
        throw new AssertionError("expected SessionFailure " + expected);
    }
}
