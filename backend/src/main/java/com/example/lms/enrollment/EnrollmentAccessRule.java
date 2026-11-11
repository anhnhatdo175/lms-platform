package com.example.lms.enrollment;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.example.lms.shared.exception.ErrorCode;

/** Quy tắc quyền học, tách khỏi service để test không cần database. */
public final class EnrollmentAccessRule {

    private EnrollmentAccessRule() {
    }

    /** Hoặc có Enrollment đang hiệu lực, hoặc có mã lỗi giải thích vì sao không. */
    public record Decision(Enrollment enrollment, ErrorCode denial) {

        public boolean granted() {
            return enrollment != null;
        }
    }

    /**
     * @param enrollments mọi Enrollment của một (student, course), thứ tự bất kỳ
     */
    public static Decision evaluate(List<Enrollment> enrollments, Instant now) {
        Optional<Enrollment> effective = enrollments.stream().filter(e -> e.isEffectiveAt(now)).findFirst();
        if (effective.isPresent()) {
            return new Decision(effective.get(), null);
        }
        Optional<Enrollment> latest = enrollments.stream()
                .max(Comparator.comparing(Enrollment::getCreatedAt).thenComparing(Enrollment::getStartsAt));
        if (latest.isEmpty()) {
            return new Decision(null, ErrorCode.ENROLLMENT_REQUIRED);
        }
        Enrollment last = latest.get();
        ErrorCode denial = switch (last.getStatus()) {
            case REVOKED -> ErrorCode.ENROLLMENT_REVOKED;
            case EXPIRED -> ErrorCode.ENROLLMENT_EXPIRED;
            // Còn ACTIVE trong database: hoặc đã qua expiresAt mà job chưa chạy, hoặc chưa tới startsAt
            case ACTIVE -> last.isLapsedAt(now) ? ErrorCode.ENROLLMENT_EXPIRED : ErrorCode.ENROLLMENT_REQUIRED;
        };
        return new Decision(null, denial);
    }
}
