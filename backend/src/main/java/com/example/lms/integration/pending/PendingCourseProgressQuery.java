package com.example.lms.integration.pending;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.lms.contract.course.CourseProgressQuery;

/** BẢN TẠM của B để ứng dụng khởi động được khi A chưa có tiến độ học: My Courses hiện tiến độ trống. A thay. */
@Component
public class PendingCourseProgressQuery implements CourseProgressQuery {

    @Override
    public Map<Long, BigDecimal> progressPercents(long studentId, Collection<Long> courseIds) {
        return Map.of();
    }

    @Override
    public MyCourseOutline outline(long studentId, long courseId) {
        return new MyCourseOutline(null, List.of());
    }
}
