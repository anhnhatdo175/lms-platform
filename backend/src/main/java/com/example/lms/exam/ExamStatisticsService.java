package com.example.lms.exam;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.exam.dto.ExamStatisticsResponse;
import com.example.lms.exam.dto.ExamStatisticsResponse.QuestionStat;

/**
 * {@code GET /teacher/exams/{id}/statistics} (SHOULD). Chỉ tính Attempt đã chấm; điểm theo thang 0–100. Thống kê theo câu gom theo Question
 * gốc; {@code answeredCount} là số lượt câu có mặt trong bài đã chấm, kể cả bỏ trống (bỏ trống tính là sai), nên
 * {@code correctRate} cùng mẫu số với nó.
 */
@Service
public class ExamStatisticsService {

    private final ExamLookup lookup;
    private final ExamAttemptRepository attempts;
    private final AttemptQuestionRepository attemptQuestions;
    private final AttemptAnswerRepository answers;

    public ExamStatisticsService(ExamLookup lookup, ExamAttemptRepository attempts,
            AttemptQuestionRepository attemptQuestions, AttemptAnswerRepository answers) {
        this.lookup = lookup;
        this.attempts = attempts;
        this.attemptQuestions = attemptQuestions;
        this.answers = answers;
    }

    @Transactional(readOnly = true)
    public ExamStatisticsResponse statistics(CurrentUser user, long examId) {
        lookup.forManager(user, examId);
        List<ExamAttempt> graded = attempts.findByExamIdAndStatus(examId, AttemptStatus.GRADED);
        List<BigDecimal> scores = graded.stream().map(ExamAttempt::getScore).filter(s -> s != null).toList();
        long passed = graded.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        int students = (int) graded.stream().map(ExamAttempt::getStudentId).distinct().count();

        return new ExamStatisticsResponse(examId, graded.size(), students, ExamStatistics.average(scores),
                ExamStatistics.rate(passed, graded.size()),
                ExamStatistics.distribution(scores),
                questionStats(graded));
    }

    private List<QuestionStat> questionStats(List<ExamAttempt> graded) {
        if (graded.isEmpty()) {
            return List.of();
        }
        List<Long> attemptIds = graded.stream().map(ExamAttempt::getId).toList();
        Map<Long, AttemptQuestion> questionById = attemptQuestions.findByAttemptIdIn(attemptIds).stream()
                .collect(Collectors.toMap(AttemptQuestion::getId, Function.identity()));

        Map<Long, long[]> tally = new LinkedHashMap<>(); // sourceQuestionId → [lượt, đúng]
        Map<Long, AttemptQuestion> sample = new LinkedHashMap<>();
        questionById.values().stream()
                .sorted(Comparator.comparingInt(AttemptQuestion::getPosition).thenComparing(AttemptQuestion::getId))
                .forEach(q -> sample.putIfAbsent(q.getSourceQuestionId(), q));
        for (AttemptAnswer answer : answers.findByAttemptIdIn(attemptIds)) {
            AttemptQuestion question = questionById.get(answer.getAttemptQuestionId());
            if (question == null || answer.getCorrect() == null) {
                continue;
            }
            long[] counts = tally.computeIfAbsent(question.getSourceQuestionId(), k -> new long[2]);
            counts[0]++;
            if (answer.getCorrect()) {
                counts[1]++;
            }
        }
        return sample.values().stream().map(q -> {
            long[] counts = tally.getOrDefault(q.getSourceQuestionId(), new long[2]);
            return new QuestionStat(q.getSourceQuestionId(), q.getContentSnapshot(),
                    ExamStatistics.rate(counts[1], counts[0]), (int) counts[0]);
        }).toList();
    }
}
