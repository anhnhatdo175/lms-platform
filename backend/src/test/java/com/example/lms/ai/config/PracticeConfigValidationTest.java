package com.example.lms.ai.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.lms.ai.AdaptivePracticeConfig;
import com.example.lms.ai.AdaptiveTaskType;
import com.example.lms.ai.config.PracticeConfigViews.Request;
import com.example.lms.ai.session.JsonSupport;
import com.example.lms.contract.question.Difficulty;
import com.example.lms.shared.exception.BusinessException;

class PracticeConfigValidationTest {

    private static Request ok() {
        return new Request(true, "Toán", "5", List.of("fractions"), Difficulty.EASY, Difficulty.HARD,
                List.of(AdaptiveTaskType.SHORT_ANSWER), 5, false, 3);
    }

    private static void rejects(Request r) {
        try {
            PracticeConfigService.validate(r);
        } catch (BusinessException e) {
            assertEquals(422, e.httpStatus());
            return;
        }
        throw new AssertionError("expected 422");
    }

    @Test
    void validRequestPasses() {
        PracticeConfigService.validate(ok());
        PracticeConfigService.validate(new Request(false, null, null, List.of("a"), Difficulty.MEDIUM,
                Difficulty.MEDIUM, List.of(AdaptiveTaskType.SHORT_ANSWER), null, null, null));
    }

    @Test
    void requiredFieldsAndNonEmptyListsAreEnforced() {
        rejects(null);
        rejects(new Request(null, null, null, List.of("a"), Difficulty.EASY, Difficulty.HARD,
                List.of(AdaptiveTaskType.SHORT_ANSWER), null, null, null));
        rejects(new Request(true, null, null, List.of(), Difficulty.EASY, Difficulty.HARD,
                List.of(AdaptiveTaskType.SHORT_ANSWER), null, null, null));
        rejects(new Request(true, null, null, List.of(" "), Difficulty.EASY, Difficulty.HARD,
                List.of(AdaptiveTaskType.SHORT_ANSWER), null, null, null));
        rejects(new Request(true, null, null, List.of("a"), Difficulty.EASY, Difficulty.HARD, List.of(), null, null,
                null));
    }

    @Test
    void minDifficultyMustNotExceedMax() {
        rejects(new Request(true, null, null, List.of("a"), Difficulty.HARD, Difficulty.EASY,
                List.of(AdaptiveTaskType.SHORT_ANSWER), null, null, null));
    }

    @Test
    void numericLimitsFollowOpenApi() {
        for (int bad : new int[] {0, 11}) {
            rejects(new Request(true, null, null, List.of("a"), Difficulty.EASY, Difficulty.HARD,
                    List.of(AdaptiveTaskType.SHORT_ANSWER), bad, null, null));
        }
        for (int bad : new int[] {0, 6}) {
            rejects(new Request(true, null, null, List.of("a"), Difficulty.EASY, Difficulty.HARD,
                    List.of(AdaptiveTaskType.SHORT_ANSWER), null, null, bad));
        }
    }

    private static void assignId(AdaptivePracticeConfig c, long id) throws Exception {
        java.lang.reflect.Field f = AdaptivePracticeConfig.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(c, id);
    }

    @Test
    void lessonOverrideInheritsSubjectAndGradeFromCourse() throws Exception {
        java.time.Instant now = java.time.Instant.parse("2026-11-15T00:00:00Z");
        AdaptivePracticeConfig course = AdaptivePracticeConfig.create(1, null, now);
        course.setSubjectCode("Toán");
        course.setGradeLevel("5");
        AdaptivePracticeConfig lesson = AdaptivePracticeConfig.create(1, 9L, now);
        assignId(course, 1L);
        assignId(lesson, 2L);
        lesson.setAllowedSkillsJson(JsonSupport.toJson(List.of("fractions")));
        lesson.setAllowedTaskTypesJson(JsonSupport.toJson(List.of("SHORT_ANSWER")));
        PracticeConfigViews.View v = PracticeConfigService.view(lesson, course);
        assertEquals("Toán", v.effectiveSubjectCode());
        assertEquals("5", v.effectiveGradeLevel());
        assertEquals(null, v.subjectCode());
        lesson.setSubjectCode("Vật lý");
        assertEquals("Vật lý", PracticeConfigService.view(lesson, course).effectiveSubjectCode());
        assertTrue(v.lessonId() == 9L);
    }
}
