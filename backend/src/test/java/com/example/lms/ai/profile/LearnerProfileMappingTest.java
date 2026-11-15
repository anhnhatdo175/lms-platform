package com.example.lms.ai.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.lms.ai.LearnerProfile;
import com.example.lms.ai.LearnerSkillState;
import com.example.lms.ai.profile.LearnerProfileViews.ProfileView;
import com.example.lms.ai.profile.LearnerProfileViews.SkillView;
import com.example.lms.contract.question.Difficulty;

class LearnerProfileMappingTest {

    private static final Instant NOW = Instant.parse("2026-11-15T00:00:00Z");

    private static LearnerSkillState skill(String code, String mastery, int evidence) {
        LearnerSkillState s = LearnerSkillState.create(7, code, NOW);
        s.setSkillName(code);
        s.setMasteryScore(new BigDecimal(mastery));
        s.setEvidenceCount(evidence);
        return s;
    }

    @Test
    void newStudentGetsAnEmptyColdStartProfile() {
        ProfileView p = LearnerProfileService.build(7, List.of(), null, true, NOW);
        assertEquals(1, p.profileVersion());
        assertEquals(0.5, p.overallMastery(), 1e-9);
        assertEquals(Difficulty.MEDIUM, p.recommendedDifficulty());
        assertTrue(p.skills().isEmpty() && p.strengths().isEmpty() && p.needsPractice().isEmpty());
        assertEquals(NOW, p.lastUpdatedAt());
    }

    @Test
    void coldStartSkillsShowNeutralMasteryAndAreNeverLabelled() {
        // Dù DB lưu 0.9, mới có 2 bằng chứng nên coi là 0.5 và không xếp mạnh/yếu.
        List<SkillView> v = LearnerProfileService.skillViews(List.of(skill("fractions", "0.90000", 2)));
        assertTrue(v.get(0).coldStart());
        assertEquals(0.5, v.get(0).mastery(), 1e-9);
        ProfileView p = LearnerProfileService.build(7, List.of(skill("fractions", "0.90000", 2)), null, true, NOW);
        assertTrue(p.strengths().isEmpty() && p.needsPractice().isEmpty());
    }

    @Test
    void strongAndWeakSkillsAreLabelledAndDifficultyFollowsMastery() {
        ProfileView p = LearnerProfileService.build(7,
                List.of(skill("fractions", "0.20000", 5), skill("decimals", "0.85000", 6)), null, true, NOW);
        assertEquals(List.of("decimals"), p.strengths());
        assertEquals(List.of("fractions"), p.needsPractice());
        SkillView fractions = p.skills().stream().filter(s -> s.skillCode().equals("fractions")).findFirst().get();
        SkillView decimals = p.skills().stream().filter(s -> s.skillCode().equals("decimals")).findFirst().get();
        assertEquals(Difficulty.EASY, fractions.recommendedDifficulty());
        assertEquals(Difficulty.HARD, decimals.recommendedDifficulty());
        assertFalse(fractions.coldStart());
        assertEquals(0.525, p.overallMastery(), 1e-9);
    }

    @Test
    void skillsAreSortedAndTeacherViewHidesGlobalStats() {
        LearnerProfile profile = LearnerProfile.create(7, NOW);
        profile.setAverageScore(new BigDecimal("0.60000"));
        profile.setScoreStddev(new BigDecimal("0.10000"));
        profile.setProfileVersion(9);
        List<LearnerSkillState> states = List.of(skill("b", "0.5", 4), skill("a", "0.5", 4));
        ProfileView own = LearnerProfileService.build(7, states, profile, true, NOW);
        ProfileView teacher = LearnerProfileService.build(7, states, profile, false, NOW);
        assertEquals("a", own.skills().get(0).skillCode());
        assertEquals(9, own.profileVersion());
        assertEquals(new BigDecimal("0.60000"), own.averageScore());
        assertNull(teacher.averageScore());
        assertNull(teacher.scoreStddev());
        assertNull(teacher.learningPace());
    }
}
