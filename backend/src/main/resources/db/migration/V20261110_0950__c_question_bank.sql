-- Người C — Ngân hàng câu hỏi (ERD v2.2 Mục 9.2, 9.3): questions, question_options.
-- Phải đứng trước V20261110_1003 (exam_questions của B có FK tới questions). Cần courses và users đã tồn tại.

CREATE TABLE questions (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_id     BIGINT UNSIGNED NOT NULL,
    content       TEXT            NOT NULL,
    question_type ENUM('SINGLE_CHOICE','MULTIPLE_CHOICE') NOT NULL,
    difficulty    ENUM('EASY','MEDIUM','HARD') NOT NULL,
    -- Chỉ câu có skill_code mới cập nhật learner_skill_states (Spec 20A.4)
    skill_code    VARCHAR(150)    NULL,
    source        ENUM('MANUAL','AI_PROMOTED') NOT NULL DEFAULT 'MANUAL',
    explanation   TEXT            NULL,
    active        BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by    BIGINT UNSIGNED NOT NULL,
    version       INT UNSIGNED    NOT NULL DEFAULT 1,
    created_at    DATETIME(6)     NOT NULL,
    updated_at    DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_questions_course_active (course_id, active),
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
