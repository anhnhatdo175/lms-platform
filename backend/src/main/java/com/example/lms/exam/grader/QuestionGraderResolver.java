package com.example.lms.exam.grader;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.lms.contract.question.QuestionType;

/** Chọn grader theo loại câu hỏi (strategy). Thêm loại câu mới chỉ cần thêm một {@link QuestionGrader}. */
@Component
public class QuestionGraderResolver {

    private final Map<QuestionType, QuestionGrader> graders = new EnumMap<>(QuestionType.class);

    public QuestionGraderResolver(List<QuestionGrader> graders) {
        graders.forEach(grader -> this.graders.put(grader.supports(), grader));
    }

    public QuestionGrader forType(QuestionType type) {
        QuestionGrader grader = graders.get(type);
        if (grader == null) {
            throw new IllegalStateException("No grader for " + type);
        }
        return grader;
    }
}
