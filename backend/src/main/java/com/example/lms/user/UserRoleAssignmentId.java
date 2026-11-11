package com.example.lms.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserRoleAssignmentId implements Serializable {

    @Column(name = "user_id", columnDefinition = "BIGINT UNSIGNED")
    private Long userId;

    @Column(name = "role_id", columnDefinition = "BIGINT UNSIGNED")
    private Long roleId;

    protected UserRoleAssignmentId() {
    }

    public UserRoleAssignmentId(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserRoleAssignmentId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
