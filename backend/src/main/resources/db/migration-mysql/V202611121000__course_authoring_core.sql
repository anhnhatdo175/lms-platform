CREATE TABLE course_categories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_course_categories_slug UNIQUE (slug)
);

CREATE TABLE courses (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    thumbnail_file_id BIGINT UNSIGNED NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    short_description VARCHAR(500) NULL,
    description LONGTEXT NULL,
    original_price BIGINT UNSIGNED NOT NULL DEFAULT 0,
    sale_price BIGINT UNSIGNED NOT NULL DEFAULT 0,
    status ENUM('DRAFT','PENDING_REVIEW','REJECTED','PUBLISHED','HIDDEN','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    subject_code VARCHAR(100) NULL,
    grade_level VARCHAR(50) NULL,
    access_duration_days INT UNSIGNED NULL,
    completion_rule ENUM(
        'ALL_MANDATORY_LESSONS',
        'ALL_MANDATORY_LESSONS_AND_PASS_REQUIRED_EXAMS',
        'MANUAL_BY_TEACHER'
    ) NOT NULL DEFAULT 'ALL_MANDATORY_LESSONS',
    published_at DATETIME(6) NULL,
    hidden_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_courses_slug UNIQUE (slug),
    CONSTRAINT fk_courses_category FOREIGN KEY (category_id)
        REFERENCES course_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_courses_creator FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_courses_thumbnail FOREIGN KEY (thumbnail_file_id)
        REFERENCES file_resources (id) ON DELETE SET NULL,
    INDEX idx_courses_status_created (status, created_at),
    INDEX idx_courses_category_status (category_id, status)
);

CREATE TABLE course_teachers (
    course_id BIGINT UNSIGNED NOT NULL,
    teacher_id BIGINT UNSIGNED NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_by BIGINT UNSIGNED NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    removed_at DATETIME(6) NULL,
    PRIMARY KEY (course_id, teacher_id),
    CONSTRAINT fk_course_teachers_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_teachers_teacher FOREIGN KEY (teacher_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_teachers_assigned_by FOREIGN KEY (assigned_by)
        REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_course_teachers_teacher_active (teacher_id, active)
);

CREATE TABLE course_review_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    submitted_by BIGINT UNSIGNED NOT NULL,
    reviewed_by BIGINT UNSIGNED NULL,
    status ENUM('PENDING','APPROVED','REJECTED','WITHDRAWN') NOT NULL,
    submission_note TEXT NULL,
    review_note TEXT NULL,
    submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_course_reviews_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_reviews_submitter FOREIGN KEY (submitted_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_reviews_reviewer FOREIGN KEY (reviewed_by)
        REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_course_reviews_status_submitted (status, submitted_at),
    INDEX idx_course_reviews_course_created (course_id, created_at)
);

CREATE TABLE course_sections (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    position INT UNSIGNED NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    release_after_days INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_course_sections_position UNIQUE (course_id, position),
    CONSTRAINT fk_course_sections_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE lessons (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    section_id BIGINT UNSIGNED NOT NULL,
    video_file_id BIGINT UNSIGNED NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    lesson_type ENUM('VIDEO','TEXT','DOCUMENT','EXAM') NOT NULL,
    content LONGTEXT NULL,
    duration_seconds INT UNSIGNED NULL,
    position INT UNSIGNED NOT NULL,
    preview BOOLEAN NOT NULL DEFAULT FALSE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    completion_method ENUM('MANUAL','VIEW_CONTENT','VIDEO_PERCENT','PASS_EXAM','SUBMIT_EXAM') NOT NULL DEFAULT 'MANUAL',
    completion_threshold DECIMAL(5,2) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_lessons_position UNIQUE (section_id, position),
    CONSTRAINT fk_lessons_section FOREIGN KEY (section_id)
        REFERENCES course_sections (id) ON DELETE CASCADE,
    CONSTRAINT fk_lessons_video FOREIGN KEY (video_file_id)
        REFERENCES file_resources (id) ON DELETE SET NULL
);

CREATE INDEX idx_lessons_section_published_position
    ON lessons (section_id, published, position);
