package com.example.lms.ai.session;

import java.util.Map;

/** Mức thành thạo hiện tại theo kỹ năng của một Student (từ learner_skill_states). */
public interface LearnerContextProvider {

    Map<String, SkillMastery> skills(long studentId);

    record SkillMastery(double mastery, int evidenceCount) {
    }
}
