package com.example.lms.course;

import com.example.lms.contract.identity.AccessPolicy;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;
import org.springframework.stereotype.Component;

@Component
public class CourseAccessPolicy implements AccessPolicy {

    private final UserRepository users;
    private final CourseTeacherRepository courseTeachers;

    public CourseAccessPolicy(UserRepository users, CourseTeacherRepository courseTeachers) {
        this.users = users;
        this.courseTeachers = courseTeachers;
    }

    @Override
    public boolean canManageCourse(long userId, long courseId) {
        return users.findById(userId).map(user -> user.hasRole(UserRole.ADMIN)).orElse(false)
                || courseTeachers.existsByIdCourseIdAndIdTeacherIdAndActiveTrue(courseId, userId);
    }
}
