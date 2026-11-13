package com.example.lms.exam.grader;

import java.util.Set;

import com.example.lms.contract.question.QuestionType;

/** Chấm một câu theo snapshot (Spec 14.6). Không chấm từng phần: một câu chỉ đúng hoặc sai. */
public interface QuestionGrader {

    QuestionType supports();

    boolean isCorrect(Set<Long> correctOptionIds, Set<Long> selectedOptionIds);
}
