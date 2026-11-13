package com.example.lms.integration.pending;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.lms.contract.course.CourseQueryService;

/** BẢN TẠM của B để ứng dụng khởi động được khi A chưa có Course và Lesson. Không tìm thấy gì, nên API của B trả 404 thay vì lỗi 500. A thay. */
@Component
public class PendingCourseQueryService implements CourseQueryService {

    @Override
    public Optional<CourseSnapshot> findCourse(long courseId) {
        return Optional.empty();
    }

    @Override
    public List<CourseSnapshot> findCourses(Collection<Long> courseIds) {
        return List.of();
    }

    @Override
    public Optional<LessonRef> findLesson(long lessonId) {
        return Optional.empty();
    }
}
