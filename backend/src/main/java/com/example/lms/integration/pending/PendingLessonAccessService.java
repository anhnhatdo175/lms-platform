package com.example.lms.integration.pending;

import org.springframework.stereotype.Component;

import com.example.lms.contract.course.LessonAccessService;

/** BẢN TẠM của B để ứng dụng khởi động được khi A chưa có drip và prerequisite: coi mọi Lesson là đã mở. A thay. */
@Component
public class PendingLessonAccessService implements LessonAccessService {

    @Override
    public LessonAccess checkOpen(long studentId, long lessonId) {
        return LessonAccess.opened();
    }
}
