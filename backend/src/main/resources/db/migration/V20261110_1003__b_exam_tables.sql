-- Người B — Exam (ERD v2.2 Mục 9): exams, exam_questions, exam_attempts, attempt_questions, attempt_answers, attempt_answer_options.
-- questions và question_options là của C; lesson_prerequisites (của A) cần thêm FK tới exams SAU migration này.

CREATE TABLE exams (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    lesson_id           BIGINT UNSIGNED NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    description         TEXT            NULL,
    duration_minutes    INT UNSIGNED    NOT NULL,
    passing_score       DECIMAL(5,2)    NOT NULL,
    maximum_attempts    INT UNSIGNED    NULL,
    shuffle_questions   BOOLEAN         NOT NULL DEFAULT FALSE,
    shuffle_options     BOOLEAN         NOT NULL DEFAULT FALSE,
    result_display_mode ENUM('IMMEDIATE_SCORE_ONLY','IMMEDIATE_WITH_CORRECT_ANSWERS','AFTER_EXAM_CLOSED') NOT NULL DEFAULT 'IMMEDIATE_SCORE_ONLY',
    start_at            DATETIME(6)     NULL,
    end_at              DATETIME(6)     NULL,
    status              ENUM('DRAFT','PUBLISHED','CLOSED') NOT NULL,
    created_by          BIGINT UNSIGNED NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    -- Lesson kiểu EXAM có tối đa một Exam
    CONSTRAINT uk_exams_lesson UNIQUE (lesson_id),
    CONSTRAINT chk_exams_duration CHECK (duration_minutes >= 1),
    CONSTRAINT chk_exams_passing_score CHECK (passing_score BETWEEN 0 AND 100),
    CONSTRAINT chk_exams_window CHECK (start_at IS NULL OR end_at IS NULL OR end_at > start_at),
    CONSTRAINT fk_exams_lesson     FOREIGN KEY (lesson_id)  REFERENCES lessons (id),
    CONSTRAINT fk_exams_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE exam_questions (
    exam_id     BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    position    INT UNSIGNED    NOT NULL,
    score       DECIMAL(7,2)    NOT NULL,
    required    BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (exam_id, question_id),
    CONSTRAINT uk_exam_questions_exam_position UNIQUE (exam_id, position),
    CONSTRAINT chk_exam_questions_score CHECK (score >= 0),
    CONSTRAINT fk_exam_questions_exam     FOREIGN KEY (exam_id)     REFERENCES exams (id),
    CONSTRAINT fk_exam_questions_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE exam_attempts (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    exam_id         BIGINT UNSIGNED NOT NULL,
    student_id      BIGINT UNSIGNED NOT NULL,
    enrollment_id   BIGINT UNSIGNED NOT NULL,
    attempt_number  INT UNSIGNED    NOT NULL,
    status          ENUM('IN_PROGRESS','SUBMITTED','GRADED') NOT NULL,
    submission_type ENUM('MANUAL','AUTO_EXPIRED') NULL,
    started_at      DATETIME(6)     NOT NULL,
    expires_at      DATETIME(6)     NOT NULL,
    submitted_at    DATETIME(6)     NULL,
    graded_at       DATETIME(6)     NULL,
    score           DECIMAL(7,2)    NULL,
    maximum_score   DECIMAL(7,2)    NOT NULL,
    passing_score   DECIMAL(5,2)    NOT NULL,
    passed          BOOLEAN         NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    -- Hai lần bắt đầu song song tính trùng attempt_number: UNIQUE chặn một bên, bên đó trả lại Attempt đang có
    CONSTRAINT uk_exam_attempts_exam_student_number UNIQUE (exam_id, student_id, attempt_number),
    KEY idx_exam_attempts_status_expires (status, expires_at),
    CONSTRAINT chk_exam_attempts_window CHECK (expires_at > started_at),
    CONSTRAINT fk_exam_attempts_exam       FOREIGN KEY (exam_id)       REFERENCES exams (id),
    CONSTRAINT fk_exam_attempts_student    FOREIGN KEY (student_id)    REFERENCES users (id),
    CONSTRAINT fk_exam_attempts_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE attempt_questions (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    attempt_id           BIGINT UNSIGNED NOT NULL,
    source_question_id   BIGINT UNSIGNED NOT NULL,
    question_type        ENUM('SINGLE_CHOICE','MULTIPLE_CHOICE') NOT NULL,
    content_snapshot     TEXT            NOT NULL,
    explanation_snapshot TEXT            NULL,
    skill_code_snapshot  VARCHAR(150)    NULL,
    difficulty_snapshot  ENUM('EASY','MEDIUM','HARD') NOT NULL,
    position             INT UNSIGNED    NOT NULL,
    maximum_score        DECIMAL(7,2)    NOT NULL,
    -- [{"id": <question_options.id>, "content": "...", "correct": true|false}, ...] theo thứ tự hiển thị
    option_snapshot_json JSON            NOT NULL,
    created_at           DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attempt_questions_attempt_position UNIQUE (attempt_id, position),
    CONSTRAINT uk_attempt_questions_attempt_source   UNIQUE (attempt_id, source_question_id),
    CONSTRAINT fk_attempt_questions_attempt  FOREIGN KEY (attempt_id)         REFERENCES exam_attempts (id),
    CONSTRAINT fk_attempt_questions_question FOREIGN KEY (source_question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE attempt_answers (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    attempt_id          BIGINT UNSIGNED NOT NULL,
    attempt_question_id BIGINT UNSIGNED NOT NULL,
    correct             BOOLEAN         NULL,
    score_awarded       DECIMAL(7,2)    NULL,
    answered_at         DATETIME(6)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attempt_answers_attempt_question UNIQUE (attempt_id, attempt_question_id),
    CONSTRAINT fk_attempt_answers_attempt          FOREIGN KEY (attempt_id)          REFERENCES exam_attempts (id),
    CONSTRAINT fk_attempt_answers_attempt_question FOREIGN KEY (attempt_question_id) REFERENCES attempt_questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE attempt_answer_options (
    attempt_answer_id       BIGINT UNSIGNED NOT NULL,
    option_id               BIGINT UNSIGNED NOT NULL,
    option_content_snapshot TEXT            NOT NULL,
    selected_at             DATETIME(6)     NOT NULL,
    PRIMARY KEY (attempt_answer_id, option_id),
    CONSTRAINT fk_attempt_answer_options_answer FOREIGN KEY (attempt_answer_id) REFERENCES attempt_answers (id),
    CONSTRAINT fk_attempt_answer_options_option FOREIGN KEY (option_id)         REFERENCES question_options (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
