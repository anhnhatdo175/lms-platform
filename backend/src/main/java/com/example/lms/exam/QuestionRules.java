package com.example.lms.exam;

import com.example.lms.contract.question.QuestionQueryService.OptionView;
import com.example.lms.contract.question.QuestionQueryService.QuestionView;
import com.example.lms.contract.question.QuestionType;

/** Câu hỏi chấm được: còn active, có Option, single choice đúng một đáp án, multiple choice ít nhất một. */
public final class QuestionRules {

    private QuestionRules() {
    }

    public static boolean isGradable(QuestionView question) {
        long correct = question.options().stream().filter(OptionView::correct).count();
        return question.active() && !question.options().isEmpty()
                && (question.type() == QuestionType.SINGLE_CHOICE ? correct == 1 : correct >= 1);
    }
}
