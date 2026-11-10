package com.example.lms.user;

import org.springframework.data.domain.Page;

import java.util.List;

public record UserDirectoryResponse(
        List<UserSummary> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long activeUsers,
        long lockedUsers,
        long studentUsers,
        long teacherUsers,
        long adminUsers) {

    public static UserDirectoryResponse from(
            Page<User> page,
            long activeUsers,
            long lockedUsers,
            long studentUsers,
            long teacherUsers,
            long adminUsers) {
        return new UserDirectoryResponse(
                page.getContent().stream().map(UserSummary::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                activeUsers,
                lockedUsers,
                studentUsers,
                teacherUsers,
                adminUsers);
    }
}
