package com.example.lms.question;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.contract.question.QuestionQueryService;

/** Điểm giao 7: B đọc Question và Option (kèm đáp án đúng) cho Exam builder và snapshot Attempt. */
@Service
public class QuestionQueryServiceImpl implements QuestionQueryService {

    private final QuestionRepository questions;

    public QuestionQueryServiceImpl(QuestionRepository questions) {
        this.questions = questions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionView> findQuestions(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return questions.findByIdIn(questionIds).stream().map(QuestionQueryServiceImpl::toView).toList();
    }

    private static QuestionView toView(Question q) {
        return new QuestionView(q.getId(), q.getCourseId(), q.getQuestionType(), q.getDifficulty(), q.getSkillCode(),
                q.getContent(), q.getExplanation(), q.isActive(),
                q.getOptions().stream()
                        .map(o -> new OptionView(o.getId(), o.getContent(), o.isCorrect(), o.getPosition())).toList());
    }
}
