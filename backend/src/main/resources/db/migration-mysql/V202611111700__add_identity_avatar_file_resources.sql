CREATE TABLE file_resources (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    uploaded_by BIGINT UNSIGNED NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT UNSIGNED NOT NULL,
    storage_provider ENUM('MINIO', 'S3', 'R2') NOT NULL,
    purpose ENUM('LESSON_RESOURCE', 'COURSE_THUMBNAIL', 'AI_SOURCE', 'USER_AVATAR') NOT NULL,
    status ENUM('PENDING', 'READY', 'FAILED', 'DELETED') NOT NULL DEFAULT 'PENDING',
    checksum VARCHAR(128) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_file_resources_object_key UNIQUE (object_key),
    CONSTRAINT fk_file_resources_uploader FOREIGN KEY (uploaded_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_file_resources_uploader (uploaded_by),
    INDEX idx_file_resources_checksum (checksum),
    INDEX idx_file_resources_purpose_status (purpose, status)
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_avatar_file FOREIGN KEY (avatar_file_id)
        REFERENCES file_resources (id) ON DELETE SET NULL;
