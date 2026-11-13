package com.example.lms.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByAvatarFileId(Long avatarFileId);

    @Query("""
            select count(distinct user) from User user
            join user.roleAssignments assignment
            where assignment.role.code = :role and user.status = :status
            """)
    long countByRoleAndStatus(@Param("role") UserRole role, @Param("status") UserStatus status);

    @Query("""
            select distinct user from User user
            where (:query is null
                or lower(user.fullName) like lower(concat('%', :query, '%'))
                or lower(user.email) like lower(concat('%', :query, '%')))
              and (:role is null or exists (
                select assignment.id from UserRoleAssignment assignment
                where assignment.user = user and assignment.role.code = :role))
              and (:status is null or user.status = :status)
            """)
    Page<User> searchUsers(
            @Param("query") String query,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable);
}
