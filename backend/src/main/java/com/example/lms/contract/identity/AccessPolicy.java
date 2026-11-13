package com.example.lms.contract.identity;

/** Điểm giao 1, A cung cấp: ADMIN, hoặc Teacher đang được phân công Course (Spec 7.2). */
public interface AccessPolicy {

    boolean canManageCourse(long userId, long courseId);
}
