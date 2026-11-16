CREATE TABLE lesson_progress (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    course_id BIGINT UNSIGNED NOT NULL,
    lesson_id BIGINT UNSIGNED NOT NULL,
    status ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'NOT_STARTED',
    watched_seconds INT UNSIGNED NOT NULL DEFAULT 0,
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    completion_source ENUM('STUDENT_MANUAL','SYSTEM_VIDEO','SYSTEM_EXAM','TEACHER_OVERRIDE','ADMIN_OVERRIDE') NULL,
    first_accessed_at DATETIME(6) NULL,
    last_accessed_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    last_heartbeat_event_id VARCHAR(64) NULL,
    last_completion_event_id VARCHAR(64) NULL,
    overridden_by BIGINT UNSIGNED NULL,
    override_reason VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_lesson_progress_student_course_lesson UNIQUE (student_id, course_id, lesson_id),
    CONSTRAINT fk_lesson_progress_student FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lesson_progress_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lesson_progress_lesson FOREIGN KEY (lesson_id)
        REFERENCES lessons (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lesson_progress_overridden_by FOREIGN KEY (overridden_by)
        REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_lesson_progress_student_course_status (student_id, course_id, status),
    INDEX idx_lesson_progress_student_accessed (student_id, last_accessed_at)
);

CREATE TABLE course_completions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    course_id BIGINT UNSIGNED NOT NULL,
    completion_rule ENUM(
        'ALL_MANDATORY_LESSONS',
        'ALL_MANDATORY_LESSONS_AND_PASS_REQUIRED_EXAMS',
        'MANUAL_BY_TEACHER'
    ) NOT NULL,
    progress_percent_snapshot DECIMAL(5,2) NOT NULL,
    mandatory_lessons_snapshot INT UNSIGNED NOT NULL,
    completed_lessons_snapshot INT UNSIGNED NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_course_completions_student_course UNIQUE (student_id, course_id),
    CONSTRAINT fk_course_completions_student FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_completions_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_completions_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_course_completions_course_completed (course_id, completed_at)
);
