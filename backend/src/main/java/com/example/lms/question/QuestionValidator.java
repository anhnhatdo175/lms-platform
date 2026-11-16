package com.example.lms.question;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.lms.contract.question.QuestionType;
import com.example.lms.question.Question.ValidatedOption;
import com.example.lms.question.dto.QuestionOptionRequest;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** Quy tắc Question hợp lệ (OpenAPI CreateQuestionRequest): Exam builder của B chỉ nhận câu chấm được (QuestionRules). */
public final class QuestionValidator {

    private QuestionValidator() {
    }

    /**
     * Kiểm tra tập Option và gán {@code position} liên tục 1..n. Option có {@code position} được xếp theo đó, Option
     * không có {@code position} xếp sau theo thứ tự gửi lên; trùng {@code position} bị từ chối.
     */
    public static List<ValidatedOption> validateOptions(QuestionType type, List<QuestionOptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw invalid("A question needs at least 2 options");
        }
        Set<Integer> seen = new HashSet<>();
        for (QuestionOptionRequest o : options) {
            if (o.content() == null || o.content().isBlank()) {
                throw invalid("Option content must not be blank");
            }
            if (o.position() != null && !seen.add(o.position())) {
                throw invalid("Option positions must be unique");
            }
        }
        long correct = options.stream().filter(o -> Boolean.TRUE.equals(o.correct())).count();
        if (type == QuestionType.SINGLE_CHOICE && correct != 1) {
            throw invalid("SINGLE_CHOICE needs exactly one correct option");
        }
        if (type == QuestionType.MULTIPLE_CHOICE && correct < 1) {
            throw invalid("MULTIPLE_CHOICE needs at least one correct option");
        }
        // sắp ổn định: có position trước (theo position), chưa có position sau (theo thứ tự gửi)
        List<QuestionOptionRequest> ordered = new ArrayList<>(options);
        ordered.sort((a, b) -> {
            int pa = a.position() == null ? Integer.MAX_VALUE : a.position();
            int pb = b.position() == null ? Integer.MAX_VALUE : b.position();
            return Integer.compare(pa, pb);
        });
        List<ValidatedOption> result = new ArrayList<>();
        int position = 1;
        for (QuestionOptionRequest o : ordered) {
            result.add(new ValidatedOption(o.content().strip(), Boolean.TRUE.equals(o.correct()), position++));
        }
        return result;
    }

    /** Chuẩn hóa skillCode: cắt khoảng trắng, chuỗi rỗng thành {@code null} (không có kỹ năng). */
    public static String normalizeSkillCode(String skillCode) {
        if (skillCode == null || skillCode.isBlank()) {
            return null;
        }
        return skillCode.strip();
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.QUESTION_INVALID, message);
    }
}
