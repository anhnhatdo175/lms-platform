package com.example.lms.user;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_file_id", columnDefinition = "BIGINT UNSIGNED")
    private Long avatarFileId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UserRoleAssignment> roleAssignments = new LinkedHashSet<>();

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "ENUM('ACTIVE','INACTIVE','BLOCKED') DEFAULT 'ACTIVE'")
    private UserStatus status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected User() {
    }

    public User(String fullName, String email, String passwordHash, UserStatus status) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void setUpdatedAt() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Long getAvatarFileId() {
        return avatarFileId;
    }

    public Set<UserRole> getRoles() {
        return roleAssignments.stream()
                .map(UserRoleAssignment::getCode)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public boolean hasRole(UserRole role) {
        return roleAssignments.stream().anyMatch(assignment -> assignment.getCode() == role);
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void updateProfile(String fullName) {
        this.fullName = fullName;
    }

    public void updateProfile(String fullName, String phoneNumber, Long avatarFileId) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.avatarFileId = avatarFileId;
    }

    public void replaceRoles(Collection<Role> roles, User assignedBy) {
        Set<UserRole> nextCodes = roles.stream().map(Role::getCode)
                .collect(java.util.stream.Collectors.toSet());
        roleAssignments.removeIf(assignment -> !nextCodes.contains(assignment.getCode()));
        Set<UserRole> existingCodes = getRoles();
        roles.stream()
                .filter(role -> !existingCodes.contains(role.getCode()))
                .map(role -> new UserRoleAssignment(this, role, assignedBy))
                .forEach(roleAssignments::add);
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void recordLogin(Instant loginAt) {
        this.lastLoginAt = loginAt;
    }
}
