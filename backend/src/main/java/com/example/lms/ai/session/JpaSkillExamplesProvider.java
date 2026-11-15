package com.example.lms.ai.session;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.question.QuestionRepository;

@Component
public class JpaSkillExamplesProvider implements SkillExamplesProvider {

    private final QuestionRepository questions;

    public JpaSkillExamplesProvider(QuestionRepository questions) {
        this.questions = questions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> examples(long courseId, String skillCode, int limit) {
        return questions.findActiveContentsByCourseAndSkill(courseId, skillCode, PageRequest.of(0, limit));
    }
}
