package com.example.lms.exam;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lưới an toàn cho Attempt quá hạn mà Student không quay lại (Spec 14.6). Mỗi Attempt chốt trong transaction
 * riêng, khóa rồi kiểm tra lại trạng thái, nên chạy trùng với request nộp bài của Student cũng không chấm hai lần.
 */
@Component
public class ExpiredAttemptJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredAttemptJob.class);
    private static final int BATCH = 100;

    private final ExamAttemptRepository attempts;
    private final AttemptFinalizer finalizer;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public ExpiredAttemptJob(ExamAttemptRepository attempts, AttemptFinalizer finalizer,
            TransactionTemplate transactions, Clock clock) {
        this.attempts = attempts;
        this.finalizer = finalizer;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${exam.expired-attempt-job-delay:PT30S}")
    public void closeExpiredAttempts() {
        List<Long> ids = attempts.findExpiredInProgressIds(AttemptStatus.IN_PROGRESS, clock.instant(),
                PageRequest.of(0, BATCH));
        for (Long id : ids) {
            try {
                transactions.executeWithoutResult(status -> close(id));
            } catch (RuntimeException e) {
                log.warn("Could not close expired attempt {}", id, e);
            }
        }
    }

    /** @return {@code true} khi job đã chốt Attempt này */
    boolean close(long attemptId) {
        Instant now = clock.instant();
        return attempts.findByIdForUpdate(attemptId)
                .filter(a -> a.isInProgress() && a.isExpiredAt(now))
                .map(a -> {
                    finalizer.gradeAndClose(a, SubmissionType.AUTO_EXPIRED, now);
                    return true;
                }).orElse(false);
    }
}
