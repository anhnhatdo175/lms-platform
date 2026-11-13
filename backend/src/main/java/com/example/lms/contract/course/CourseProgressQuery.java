package com.example.lms.contract.course;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface M2, A cung cấp. {@code GET /my-courses*} là endpoint của B nhưng tiến độ, curriculum và trạng thái
 * khóa Lesson là dữ liệu của A, nên B chỉ ghép phần Enrollment với kết quả A trả về.
 */
public interface CourseProgressQuery {

    /** Phần trăm 0–100 theo Spec 13.5; Course chưa học trả 0. */
    Map<Long, BigDecimal> progressPercents(long studentId, Collection<Long> courseIds);

    /**
     * @return {@code progress} theo schema {@code CourseProgress} và {@code sections} theo schema
     *         {@code MySection} của OpenAPI; kiểu cụ thể do A định nghĩa, B chỉ chuyển nguyên ra JSON
     */
    MyCourseOutline outline(long studentId, long courseId);

    record MyCourseOutline(Object progress, List<?> sections) {
    }
}
