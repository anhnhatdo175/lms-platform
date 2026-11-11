package com.example.lms.exam.grader;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.lms.contract.question.QuestionType;

/** Exact match: chọn đủ mọi Option đúng và không chọn Option sai nào (Spec 14.6). */
@Component
public class MultipleChoiceGrader implements QuestionGrader {

    @Override
    public QuestionType supports() {
        return QuestionType.MULTIPLE_CHOICE;
    }

    @Override
    public boolean isCorrect(Set<Long> correctOptionIds, Set<Long> selectedOptionIds) {
        return !correctOptionIds.isEmpty() && selectedOptionIds.equals(correctOptionIds);
    }
}
