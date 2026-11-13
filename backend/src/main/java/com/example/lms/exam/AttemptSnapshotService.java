package com.example.lms.exam;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.lms.contract.question.QuestionQueryService;
import com.example.lms.contract.question.QuestionQueryService.OptionView;
import com.example.lms.contract.question.QuestionQueryService.QuestionView;
import com.example.lms.exam.snapshot.OptionSnapshot;
import com.example.lms.exam.snapshot.OptionSnapshotCodec;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Chụp đề lúc bắt đầu Attempt (Spec 14.4): danh sách câu, thứ tự (đã xáo nếu bật), điểm từng câu và Option kèm cờ
 * đúng. Sau đó Teacher sửa Question gốc cũng không đổi Attempt đang diễn ra (AC-EXAM-02).
 */
@Service
public class AttemptSnapshotService {

    private final ExamQuestionRepository examQuestions;
    private final AttemptQuestionRepository attemptQuestions;
    private final QuestionQueryService questions;
    private final Random random = new SecureRandom();

    public AttemptSnapshotService(ExamQuestionRepository examQuestions, AttemptQuestionRepository attemptQuestions,
            QuestionQueryService questions) {
        this.examQuestions = examQuestions;
        this.attemptQuestions = attemptQuestions;
        this.questions = questions;
    }

    public record PlannedQuestion(ExamQuestion examQuestion, QuestionView question) {
    }

    public record Plan(List<PlannedQuestion> questions, BigDecimal maximumScore) {
    }

    /** Đề hiện tại của Exam; Exam không có câu chấm được hoặc tổng điểm bằng 0 thì {@code EXAM_NOT_READY}. */
    public Plan plan(Exam exam, long courseId) {
        List<ExamQuestion> rows = examQuestions.findByExamIdOrderByPositionAsc(exam.getId());
        Map<Long, QuestionView> byId = questions.findQuestions(rows.stream().map(ExamQuestion::getQuestionId).toList())
                .stream().collect(Collectors.toMap(QuestionView::id, Function.identity()));
        List<PlannedQuestion> planned = new ArrayList<>();
        BigDecimal maximum = BigDecimal.ZERO;
        for (ExamQuestion row : rows) {
            QuestionView question = byId.get(row.getQuestionId());
            if (question == null || question.courseId() != courseId || !QuestionRules.isGradable(question)) {
                throw new BusinessException(ErrorCode.EXAM_NOT_READY,
                        "Question " + row.getQuestionId() + " is missing, inactive or has no valid answer");
            }
            planned.add(new PlannedQuestion(row, question));
            maximum = maximum.add(row.getScore());
        }
        if (planned.isEmpty() || maximum.signum() <= 0) {
            throw new BusinessException(ErrorCode.EXAM_NOT_READY, "Exam has no question or a total score of 0");
        }
        return new Plan(planned, maximum);
    }

    public List<AttemptQuestion> write(ExamAttempt attempt, Plan plan, boolean shuffleQuestions,
            boolean shuffleOptions, Instant now) {
        List<PlannedQuestion> order = new ArrayList<>(plan.questions());
        if (shuffleQuestions) {
            Collections.shuffle(order, random);
        }
        List<AttemptQuestion> written = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            QuestionView question = order.get(i).question();
            List<OptionView> options = new ArrayList<>(question.options());
            options.sort(Comparator.comparingInt(OptionView::position));
            if (shuffleOptions) {
                Collections.shuffle(options, random);
            }
            String json = OptionSnapshotCodec.write(
                    options.stream().map(o -> new OptionSnapshot(o.id(), o.content(), o.correct())).toList());
            written.add(attemptQuestions.save(new AttemptQuestion(attempt.getId(), question.id(), question.type(),
                    question.content(), question.explanation(), question.skillCode(), question.difficulty(), i + 1,
                    order.get(i).examQuestion().getScore(), json, now)));
        }
        return written;
    }
}
