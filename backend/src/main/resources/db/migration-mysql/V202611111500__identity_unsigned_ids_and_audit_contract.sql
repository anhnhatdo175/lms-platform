ALTER TABLE refresh_tokens
    DROP FOREIGN KEY fk_refresh_tokens_user;

ALTER TABLE user_roles
    DROP FOREIGN KEY fk_user_roles_user,
    DROP FOREIGN KEY fk_user_roles_role,
    DROP FOREIGN KEY fk_user_roles_assigned_by;

ALTER TABLE password_reset_tokens
    DROP FOREIGN KEY fk_password_reset_tokens_user;

ALTER TABLE audit_logs
    DROP FOREIGN KEY fk_audit_logs_actor;

ALTER TABLE users
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN avatar_file_id BIGINT UNSIGNED NULL,
    MODIFY COLUMN status ENUM('ACTIVE', 'INACTIVE', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE roles
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT;

ALTER TABLE user_roles
    MODIFY COLUMN user_id BIGINT UNSIGNED NOT NULL,
    MODIFY COLUMN role_id BIGINT UNSIGNED NOT NULL,
    MODIFY COLUMN assigned_by BIGINT UNSIGNED NULL;

ALTER TABLE refresh_tokens
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN user_id BIGINT UNSIGNED NOT NULL;

ALTER TABLE password_reset_tokens
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN user_id BIGINT UNSIGNED NOT NULL,
    MODIFY COLUMN token_hash VARCHAR(255) NOT NULL;

ALTER TABLE audit_logs
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN actor_user_id BIGINT UNSIGNED NULL,
    MODIFY COLUMN entity_id BIGINT UNSIGNED NULL,
    MODIFY COLUMN reason VARCHAR(500) NULL,
    ADD COLUMN user_agent VARCHAR(500) NULL AFTER ip_address;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES roles (id),
    ADD CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by)
        REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE audit_logs
    ADD CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id)
        REFERENCES users (id) ON DELETE SET NULL;
