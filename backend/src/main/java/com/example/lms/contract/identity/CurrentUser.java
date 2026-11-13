package com.example.lms.contract.identity;

import java.util.Set;

/**
 * Người đang gọi API (điểm giao 1, A cung cấp). Một User có thể vừa là Teacher của Course này vừa là
 * Student của Course khác, nên quyền xét theo tài nguyên, không theo "role cao nhất" (Spec 7.4).
 */
public record CurrentUser(long id, Set<String> roles) {

    public CurrentUser {
        roles = Set.copyOf(roles);
    }

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }
}
