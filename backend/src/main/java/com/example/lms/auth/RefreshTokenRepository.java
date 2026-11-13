package com.example.lms.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import java.time.Instant;
import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken token
            set token.revokedAt = :revokedAt, token.revokedReason = :reason
            where token.familyId = :familyId and token.revokedAt is null
            """)
    int revokeFamily(
            @Param("familyId") String familyId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);

    @Modifying
    @Query("""
            update RefreshToken token
            set token.revokedAt = :revokedAt, token.revokedReason = :reason
            where token.userId = :userId and token.revokedAt is null
              and (:exceptTokenHash is null or token.tokenHash <> :exceptTokenHash)
            """)
    int revokeOtherTokens(
            @Param("userId") Long userId,
            @Param("exceptTokenHash") String exceptTokenHash,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);
}
