CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code)
);

INSERT INTO roles (id, code, name, description) VALUES
    (1, 'ADMIN', 'Quản trị viên', 'Quản trị toàn bộ hệ thống'),
    (2, 'TEACHER', 'Giảng viên', 'Quản lý nội dung khóa học được phân công'),
    (3, 'STUDENT', 'Học viên', 'Học các khóa học đã đăng ký');

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_by BIGINT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES roles (id),
    CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by)
        REFERENCES users (id) ON DELETE SET NULL
);

INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT users.id, roles.id, users.created_at
FROM users
JOIN roles ON roles.code = users.role;

ALTER TABLE users MODIFY COLUMN full_name VARCHAR(255) NOT NULL;
ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NOT NULL;
ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NOT NULL;
ALTER TABLE users ADD COLUMN phone_number VARCHAR(20) NULL;
ALTER TABLE users ADD COLUMN avatar_file_id BIGINT NULL;
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE users SET status = 'BLOCKED' WHERE status = 'LOCKED';
ALTER TABLE users DROP COLUMN role;

ALTER TABLE refresh_tokens ADD COLUMN family_id VARCHAR(64) NULL;
ALTER TABLE refresh_tokens ADD COLUMN revoked_reason VARCHAR(255) NULL;
ALTER TABLE refresh_tokens MODIFY COLUMN token_hash VARCHAR(255) NOT NULL;
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    actor_email VARCHAR(255) NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NULL,
    ip_address VARCHAR(45) NULL,
    old_value JSON NULL,
    new_value JSON NULL,
    reason VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id)
        REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
