-- =====================================================================
-- CHỈ DÙNG CHO TEST CỦA B. Không nằm trong db/migration nên không chạy ở dev/prod.
-- Bảng `users` lấy từ migration thật của A (V1__create_auth_tables). File này tạo các bảng còn lại của A
-- và C (theo ERD v2.2) mà bảng của B đặt khóa ngoại tới, để integration test của B chạy được trước khi
-- migration thật của A và C có mặt. Khi đã có, xóa file này và bỏ classpath:db/test-baseline khỏi
-- application-test.yml.
-- =====================================================================

-- Giả lập bản sửa A cần làm: ERD ghi users.id là BIGINT UNSIGNED, V1 của A đang là BIGINT, và MySQL
-- không cho khóa ngoại lệch kiểu. Khi A sửa V1 thì xóa khối này.
ALTER TABLE refresh_tokens DROP FOREIGN KEY fk_refresh_tokens_user;
ALTER TABLE users MODIFY id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT;
ALTER TABLE refresh_tokens MODIFY user_id BIGINT UNSIGNED NOT NULL;
ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE TABLE file_resources (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    uploaded_by      BIGINT UNSIGNED NOT NULL,
    original_name    VARCHAR(255)    NOT NULL,
    object_key       VARCHAR(500)    NOT NULL,
    mime_type        VARCHAR(100)    NOT NULL,
    file_size        BIGINT UNSIGNED NOT NULL,
    storage_provider ENUM('MINIO','S3','R2') NOT NULL,
    status           ENUM('PENDING','READY','FAILED','DELETED') NOT NULL DEFAULT 'READY',
    checksum         VARCHAR(128)    NULL,
    created_at       DATETIME(6)     NOT NULL,
    deleted_at       DATETIME(6)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_file_resources_object_key UNIQUE (object_key),
    KEY idx_file_resources_checksum (checksum),
    CONSTRAINT fk_file_resources_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE course_categories (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255)    NOT NULL,
    slug        VARCHAR(255)    NOT NULL,
    description TEXT            NULL,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)     NOT NULL,
    updated_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE courses (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id          BIGINT UNSIGNED NOT NULL,
    created_by           BIGINT UNSIGNED NOT NULL,
    thumbnail_file_id    BIGINT UNSIGNED NULL,
    title                VARCHAR(255)    NOT NULL,
    slug                 VARCHAR(255)    NOT NULL,
    short_description    VARCHAR(500)    NULL,
    description          LONGTEXT        NULL,
    original_price       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    sale_price           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    status               ENUM('DRAFT','PENDING_REVIEW','REJECTED','PUBLISHED','HIDDEN','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    subject_code         VARCHAR(100)    NULL,
    grade_level          VARCHAR(50)     NULL,
    access_duration_days INT UNSIGNED    NULL,
    completion_rule      ENUM('ALL_MANDATORY_LESSONS','ALL_MANDATORY_LESSONS_AND_PASS_REQUIRED_EXAMS','MANUAL_BY_TEACHER') NOT NULL DEFAULT 'ALL_MANDATORY_LESSONS',
    published_at         DATETIME(6)     NULL,
    hidden_at            DATETIME(6)     NULL,
    archived_at          DATETIME(6)     NULL,
    version              BIGINT          NOT NULL DEFAULT 0,
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_courses_slug UNIQUE (slug),
    KEY idx_courses_status_category_published (status, category_id, published_at),
    KEY idx_courses_status_sale_price (status, sale_price),
    CONSTRAINT fk_courses_category   FOREIGN KEY (category_id)       REFERENCES course_categories (id),
    CONSTRAINT fk_courses_created_by FOREIGN KEY (created_by)        REFERENCES users (id),
    CONSTRAINT fk_courses_thumbnail  FOREIGN KEY (thumbnail_file_id) REFERENCES file_resources (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE course_teachers (
    course_id   BIGINT UNSIGNED NOT NULL,
    teacher_id  BIGINT UNSIGNED NOT NULL,
    is_primary  BOOLEAN         NOT NULL DEFAULT FALSE,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    assigned_by BIGINT UNSIGNED NULL,
    assigned_at DATETIME(6)     NOT NULL,
    removed_at  DATETIME(6)     NULL,
    PRIMARY KEY (course_id, teacher_id),
    KEY idx_course_teachers_teacher_active_course (teacher_id, active, course_id),
    CONSTRAINT fk_course_teachers_course      FOREIGN KEY (course_id)   REFERENCES courses (id),
    CONSTRAINT fk_course_teachers_teacher     FOREIGN KEY (teacher_id)  REFERENCES users (id),
    CONSTRAINT fk_course_teachers_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE course_sections (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id          BIGINT UNSIGNED NOT NULL,
    title              VARCHAR(255)    NOT NULL,
    description        TEXT            NULL,
    position           INT UNSIGNED    NOT NULL,
    published          BOOLEAN         NOT NULL DEFAULT FALSE,
    release_after_days INT UNSIGNED    NOT NULL DEFAULT 0,
    created_at         DATETIME(6)     NOT NULL,
    updated_at         DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_sections_course_position UNIQUE (course_id, position),
    CONSTRAINT fk_course_sections_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE lessons (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    section_id           BIGINT UNSIGNED NOT NULL,
    video_file_id        BIGINT UNSIGNED NULL,
    title                VARCHAR(255)    NOT NULL,
    description          TEXT            NULL,
    lesson_type          ENUM('VIDEO','TEXT','DOCUMENT','EXAM') NOT NULL,
    content              LONGTEXT        NULL,
    duration_seconds     INT UNSIGNED    NULL,
    position             INT UNSIGNED    NOT NULL,
    preview              BOOLEAN         NOT NULL DEFAULT FALSE,
    published            BOOLEAN         NOT NULL DEFAULT FALSE,
    mandatory            BOOLEAN         NOT NULL DEFAULT TRUE,
    completion_method    ENUM('MANUAL','VIEW_CONTENT','VIDEO_PERCENT','PASS_EXAM','SUBMIT_EXAM') NOT NULL DEFAULT 'MANUAL',
    completion_threshold DECIMAL(5,2)    NULL,
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_lessons_section_position UNIQUE (section_id, position),
    CONSTRAINT fk_lessons_section    FOREIGN KEY (section_id)    REFERENCES course_sections (id),
    CONSTRAINT fk_lessons_video_file FOREIGN KEY (video_file_id) REFERENCES file_resources (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE questions (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id     BIGINT UNSIGNED NOT NULL,
    content       TEXT            NOT NULL,
    question_type ENUM('SINGLE_CHOICE','MULTIPLE_CHOICE') NOT NULL,
    difficulty    ENUM('EASY','MEDIUM','HARD') NOT NULL,
    skill_code    VARCHAR(150)    NULL,
    source        ENUM('MANUAL','AI_PROMOTED') NOT NULL DEFAULT 'MANUAL',
    explanation   TEXT            NULL,
    active        BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by    BIGINT UNSIGNED NOT NULL,
    version       INT UNSIGNED    NOT NULL DEFAULT 1,
    created_at    DATETIME(6)     NOT NULL,
    updated_at    DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_questions_skill_code (skill_code),
    CONSTRAINT fk_questions_course     FOREIGN KEY (course_id)  REFERENCES courses (id),
    CONSTRAINT fk_questions_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE question_options (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    question_id BIGINT UNSIGNED NOT NULL,
    content     TEXT            NOT NULL,
    correct     BOOLEAN         NOT NULL DEFAULT FALSE,
    position    INT UNSIGNED    NOT NULL,
    created_at  DATETIME(6)     NOT NULL,
    updated_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_question_options_question_position UNIQUE (question_id, position),
    CONSTRAINT fk_question_options_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
