package com.example.lms.integration;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.contract.identity.UserQueryService;
import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserStatus;

/** Đọc bảng {@code users} của A qua {@link UserRepository}; B không ghi vào bảng này. */
@Component
@Transactional(readOnly = true)
public class UserRepositoryQueryService implements UserQueryService {

    private final UserRepository users;

    public UserRepositoryQueryService(UserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<UserSummary> findById(long userId) {
        return users.findById(userId).map(UserRepositoryQueryService::summary);
    }

    @Override
    public boolean isActive(long userId) {
        return users.findById(userId).map(u -> u.getStatus() == UserStatus.ACTIVE).orElse(false);
    }

    @Override
    public Optional<Long> findIdByEmail(String email) {
        return users.findByEmail(email).map(User::getId);
    }

    @Override
    public Map<Long, String> emailsByIds(Collection<Long> userIds) {
        return users.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, User::getEmail));
    }

    private static UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(),
                user.getStatus() == UserStatus.ACTIVE);
    }
}
