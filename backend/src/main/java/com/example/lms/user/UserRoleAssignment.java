package com.example.lms.user;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_roles")
public class UserRoleAssignment {

    @EmbeddedId
    private UserRoleAssignmentId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", columnDefinition = "BIGINT UNSIGNED")
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected UserRoleAssignment() {
    }

    UserRoleAssignment(User user, Role role, User assignedBy) {
        this.user = user;
        this.role = role;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
        if (user.getId() != null && role.getId() != null) {
            this.id = new UserRoleAssignmentId(user.getId(), role.getId());
        }
    }

    public UserRole getCode() {
        return role.getCode();
    }
}
