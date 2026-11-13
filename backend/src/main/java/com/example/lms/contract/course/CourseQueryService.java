package com.example.lms.contract.course;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Điểm giao 2, A cung cấp: đọc Course và Lesson cho Cart, checkout và Exam builder. */
public interface CourseQueryService {

    Optional<CourseSnapshot> findCourse(long courseId);

    List<CourseSnapshot> findCourses(Collection<Long> courseIds);

    Optional<LessonRef> findLesson(long lessonId);

    /**
     * @param purchasable    A quyết định: hiện tại là {@code status = PUBLISHED}
     * @param salePrice      giá bán hiện tại, VND số nguyên đồng
     * @param accessDurationDays {@code null} = học không giới hạn; B snapshot vào OrderItem
     */
    record CourseSnapshot(long id, String slug, String title, long salePrice, Integer accessDurationDays,
            boolean purchasable, String thumbnailUrl) {
    }

    /** {@code lessonType} là một trong VIDEO/TEXT/DOCUMENT/EXAM; Exam chỉ tạo được trên Lesson kiểu EXAM. */
    record LessonRef(long id, long courseId, long sectionId, String lessonType, String title) {

        public boolean isExamLesson() {
            return "EXAM".equals(lessonType);
        }
    }
}
