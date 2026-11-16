CREATE TABLE lesson_resources (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    lesson_id BIGINT UNSIGNED NOT NULL,
    file_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL,
    resource_type ENUM('PDF','SLIDE','IMAGE','DOCUMENT','ATTACHMENT') NOT NULL,
    downloadable BOOLEAN NOT NULL DEFAULT TRUE,
    position INT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_lesson_resources_lesson_file UNIQUE (lesson_id, file_id),
    CONSTRAINT uk_lesson_resources_lesson_position UNIQUE (lesson_id, position),
    CONSTRAINT fk_lesson_resources_lesson FOREIGN KEY (lesson_id)
        REFERENCES lessons (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_resources_file FOREIGN KEY (file_id)
        REFERENCES file_resources (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
