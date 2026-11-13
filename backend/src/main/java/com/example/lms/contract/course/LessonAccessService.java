package com.example.lms.contract.course;

import java.time.Instant;

import com.example.lms.shared.exception.ErrorCode;

/**
 * Interface M1, A cung cấp: Lesson đã mở cho Student chưa, xét đã phát hành, drip và prerequisite.
 * B gọi trước khi cho bắt đầu Attempt (Spec 14.4) và khi tính {@code canStart}. Không xét Enrollment:
 * B tự kiểm tra Enrollment.
 */
public interface LessonAccessService {

    LessonAccess checkOpen(long studentId, long lessonId);

    /**
     * @param lockReason {@code LESSON_NOT_PUBLISHED}, {@code LESSON_LOCKED_UNTIL_RELEASE} hoặc
     *                   {@code LESSON_LOCKED_BY_PREREQUISITE}; {@code null} khi mở
     * @param releaseAt  ngày mở theo drip, nếu bị khóa vì chưa tới ngày
     */
    record LessonAccess(boolean open, ErrorCode lockReason, Instant releaseAt) {

        public static LessonAccess opened() {
            return new LessonAccess(true, null, null);
        }
    }
}
