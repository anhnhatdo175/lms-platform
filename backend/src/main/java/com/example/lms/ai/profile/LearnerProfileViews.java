package com.example.lms.ai.profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.example.lms.contract.question.Difficulty;

public final class LearnerProfileViews {

    private LearnerProfileViews() {
    }

    public record SkillView(String skillCode, String skillName, double mastery, int evidenceCount,
            Instant lastObservedAt, Difficulty recommendedDifficulty, String subjectCode, boolean coldStart) {
    }

    public record ProfileView(long studentId, int profileVersion, double overallMastery,
            Difficulty recommendedDifficulty, List<String> strengths, List<String> needsPractice,
            List<SkillView> skills, Instant lastUpdatedAt, BigDecimal averageScore, BigDecimal scoreStddev,
            BigDecimal learningPace) {
    }
}
