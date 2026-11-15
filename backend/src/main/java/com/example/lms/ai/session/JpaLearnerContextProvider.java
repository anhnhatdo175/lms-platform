package com.example.lms.ai.session;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.LearnerSkillStateRepository;

@Component
public class JpaLearnerContextProvider implements LearnerContextProvider {

    private final LearnerSkillStateRepository states;

    public JpaLearnerContextProvider(LearnerSkillStateRepository states) {
        this.states = states;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, SkillMastery> skills(long studentId) {
        Map<String, SkillMastery> result = new HashMap<>();
        states.findByStudentId(studentId).forEach(s -> result.put(s.getSkillCode(),
                new SkillMastery(s.getMasteryScore().doubleValue(), s.getEvidenceCount())));
        return result;
    }
}
