package com.example.lms.exam.grader;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.lms.contract.question.QuestionType;

/** Đúng khi chọn đúng một Option và đó là Option đúng duy nhất. */
@Component
public class SingleChoiceGrader implements QuestionGrader {

    @Override
    public QuestionType supports() {
        return QuestionType.SINGLE_CHOICE;
    }

    @Override
    public boolean isCorrect(Set<Long> correctOptionIds, Set<Long> selectedOptionIds) {
        return correctOptionIds.size() == 1 && selectedOptionIds.size() == 1
                && correctOptionIds.containsAll(selectedOptionIds);
    }
}
