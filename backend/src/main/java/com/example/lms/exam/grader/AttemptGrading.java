package com.example.lms.exam.grader;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.lms.contract.question.QuestionType;
import com.example.lms.exam.snapshot.OptionSnapshot;

/**
 * Chấm cả Attempt từ snapshot (Spec 14.6), hàm thuần. Câu đúng nhận đủ điểm, sai hoặc bỏ trống nhận 0;
 * điểm cuối là phần trăm trên tổng điểm tối đa đã snapshot.
 */
public final class AttemptGrading {

    private AttemptGrading() {
    }

    public record QuestionSnapshot(long attemptQuestionId, QuestionType type, BigDecimal maximumScore,
            List<OptionSnapshot> options) {
    }

    public record QuestionResult(boolean correct, BigDecimal scoreAwarded) {
    }

    public record Result(Map<Long, QuestionResult> byAttemptQuestion, BigDecimal awarded, BigDecimal score,
            boolean passed) {
    }

    /**
     * @param selections Option đã chọn theo {@code attempt_question_id}; câu không có trong map là bỏ trống
     */
    public static Result grade(List<QuestionSnapshot> questions, Map<Long, Set<Long>> selections,
            BigDecimal maximumScore, BigDecimal passingScore, QuestionGraderResolver graders) {
        Map<Long, QuestionResult> results = new LinkedHashMap<>();
        BigDecimal awarded = BigDecimal.ZERO;
        for (QuestionSnapshot question : questions) {
            Set<Long> correctIds = question.options().stream().filter(OptionSnapshot::correct)
                    .map(OptionSnapshot::id).collect(Collectors.toSet());
            Set<Long> selected = selections.getOrDefault(question.attemptQuestionId(), Set.of());
            boolean correct = graders.forType(question.type()).isCorrect(correctIds, selected);
            BigDecimal points = correct ? question.maximumScore() : BigDecimal.ZERO;
            results.put(question.attemptQuestionId(), new QuestionResult(correct, points));
            awarded = awarded.add(points);
        }
        BigDecimal score = AttemptScoring.percent(awarded, maximumScore);
        return new Result(results, awarded, score, AttemptScoring.passed(score, passingScore));
    }
}
