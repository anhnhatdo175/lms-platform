-- Người C — AI Adaptive Profile-Based RAG (ERD v2.2 Mục 17.3–17.12): 10 bảng.
-- Cần users, courses, lessons, file_resources, questions đã tồn tại (xem lưu ý thứ tự Flyway trong báo cáo).
-- Thứ tự theo ERD Mục 2 "Thứ tự migration": config -> tài liệu -> chunk -> hồ sơ -> kỹ năng -> tương tác
-- -> phiên -> nhiệm vụ -> lần làm -> gợi ý.

CREATE TABLE adaptive_practice_configs (
    id                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    course_id               BIGINT UNSIGNED   NOT NULL,
    lesson_id               BIGINT UNSIGNED   NULL,
    -- NULL lesson_id = cấu hình mặc định của Course; UNIQUE trên (course_id, lesson_id) không đủ vì MySQL cho nhiều NULL.
    scope_key               BIGINT UNSIGNED   GENERATED ALWAYS AS (COALESCE(lesson_id, 0)),
    enabled                 BOOLEAN           NOT NULL DEFAULT FALSE,
    subject_code            VARCHAR(100)      NULL,
    grade_level             VARCHAR(50)       NULL,
    allowed_skills_json     JSON              NOT NULL,
    min_difficulty          ENUM('EASY','MEDIUM','HARD') NOT NULL DEFAULT 'EASY',
    max_difficulty          ENUM('EASY','MEDIUM','HARD') NOT NULL DEFAULT 'HARD',
    allowed_task_types_json JSON              NOT NULL,
    max_tasks_per_session   SMALLINT UNSIGNED NOT NULL DEFAULT 5,
    socratic_enabled        BOOLEAN           NOT NULL DEFAULT FALSE,
    max_hints_per_task      TINYINT UNSIGNED  NOT NULL DEFAULT 3,
    updated_by              BIGINT UNSIGNED   NOT NULL,
    created_at              DATETIME(6)       NOT NULL,
    updated_at              DATETIME(6)       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_adaptive_configs_scope UNIQUE (course_id, scope_key),
    CONSTRAINT fk_adaptive_configs_course  FOREIGN KEY (course_id)  REFERENCES courses (id),
    CONSTRAINT fk_adaptive_configs_lesson  FOREIGN KEY (lesson_id)  REFERENCES lessons (id),
    CONSTRAINT fk_adaptive_configs_updater FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_knowledge_documents (
    id               BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    course_id        BIGINT UNSIGNED   NULL,
    lesson_id        BIGINT UNSIGNED   NULL,
    file_resource_id BIGINT UNSIGNED   NULL,
    source_text      MEDIUMTEXT        NULL,
    title            VARCHAR(255)      NOT NULL,
    source_type      ENUM('LMS_FILE','PASTED_TEXT','BENCHMARK') NOT NULL,
    source_uri       VARCHAR(1000)     NULL,
    content_hash     CHAR(64)          NULL,
    language_code    VARCHAR(10)       NULL,
    subject_code     VARCHAR(100)      NULL,
    grade_level      VARCHAR(50)       NULL,
    topic_tags_json  JSON              NULL,
    index_status     ENUM('PENDING','PROCESSING','READY','FAILED') NOT NULL DEFAULT 'PENDING',
    index_version    INT UNSIGNED      NOT NULL DEFAULT 1,
    attempt_count    SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    lease_expires_at DATETIME(6)       NULL,
    next_retry_at    DATETIME(6)       NULL,
    error_message    TEXT              NULL,
    deleted_at       DATETIME(6)       NULL,
    deleted_by       BIGINT UNSIGNED   NULL,
    created_by       BIGINT UNSIGNED   NOT NULL,
    created_at       DATETIME(6)       NOT NULL,
    updated_at       DATETIME(6)       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ai_docs_scope_status (course_id, lesson_id, index_status, deleted_at),
    KEY idx_ai_docs_subject_grade (subject_code, grade_level, index_status),
    KEY idx_ai_docs_file (file_resource_id),
    KEY idx_ai_docs_content_hash (content_hash),
    KEY idx_ai_docs_lease (lease_expires_at),
    KEY idx_ai_docs_deleted_at (deleted_at),
    CONSTRAINT fk_ai_docs_course     FOREIGN KEY (course_id)        REFERENCES courses (id),
    CONSTRAINT fk_ai_docs_lesson     FOREIGN KEY (lesson_id)        REFERENCES lessons (id),
    CONSTRAINT fk_ai_docs_file       FOREIGN KEY (file_resource_id) REFERENCES file_resources (id),
    CONSTRAINT fk_ai_docs_deleted_by FOREIGN KEY (deleted_by)       REFERENCES users (id),
    CONSTRAINT fk_ai_docs_created_by FOREIGN KEY (created_by)       REFERENCES users (id),
    -- AC-AI-16: LMS_FILE và PASTED_TEXT loại trừ nhau
    CONSTRAINT ck_ai_docs_source CHECK (
        (source_type = 'LMS_FILE'    AND file_resource_id IS NOT NULL AND source_text IS NULL) OR
        (source_type = 'PASTED_TEXT' AND source_text IS NOT NULL AND file_resource_id IS NULL) OR
        (source_type = 'BENCHMARK')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_document_chunks (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    document_id     BIGINT UNSIGNED NOT NULL,
    chunk_index     INT UNSIGNED    NOT NULL,
    chunk_text      MEDIUMTEXT      NOT NULL,
    token_count     INT UNSIGNED    NULL,
    page_number     INT UNSIGNED    NULL,
    section_title   VARCHAR(255)    NULL,
    skill_code      VARCHAR(150)    NULL,
    metadata_json   JSON            NULL,
    vector_id       VARCHAR(255)    NULL,
    embedding_model VARCHAR(150)    NULL,
    index_version   INT UNSIGNED    NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_chunks_doc_version_index UNIQUE (document_id, index_version, chunk_index),
    CONSTRAINT uk_ai_chunks_vector_id UNIQUE (vector_id),
    KEY idx_ai_chunks_skill (skill_code),
    CONSTRAINT fk_ai_chunks_document FOREIGN KEY (document_id) REFERENCES ai_knowledge_documents (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE learner_profiles (
    id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id             BIGINT UNSIGNED NOT NULL,
    average_score          DECIMAL(6,5)    NULL,
    score_stddev           DECIMAL(6,5)    NULL,
    learning_pace          DECIMAL(10,4)   NULL,
    strength_topics_json   JSON            NULL,
    weak_topics_json       JSON            NULL,
    profile_vector_id      VARCHAR(255)    NULL,
    profile_vector_version INT UNSIGNED    NOT NULL DEFAULT 1,
    profile_version        INT UNSIGNED    NOT NULL DEFAULT 1,
    last_interaction_at    DATETIME(6)     NULL,
    computed_at            DATETIME(6)     NULL,
    created_at             DATETIME(6)     NOT NULL,
    updated_at             DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_learner_profiles_student UNIQUE (student_id),
    CONSTRAINT uk_learner_profiles_vector UNIQUE (profile_vector_id),
    CONSTRAINT fk_learner_profiles_student FOREIGN KEY (student_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE learner_skill_states (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id       BIGINT UNSIGNED NOT NULL,
    skill_code       VARCHAR(150)    NOT NULL,
    skill_name       VARCHAR(255)    NOT NULL,
    subject_code     VARCHAR(100)    NULL,
    -- 0.5 là trung tính khi cold start (BR-AI-13); EMA theo BR-AI-14
    mastery_score    DECIMAL(6,5)    NOT NULL DEFAULT 0.5,
    evidence_count   INT UNSIGNED    NOT NULL DEFAULT 0,
    last_evidence_at DATETIME(6)     NULL,
    model_version    VARCHAR(100)    NULL,
    updated_at       DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_skill_state_student_skill UNIQUE (student_id, skill_code),
    KEY idx_skill_state_student_subject (student_id, subject_code),
    CONSTRAINT fk_skill_state_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT ck_skill_state_mastery CHECK (mastery_score BETWEEN 0 AND 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE learning_interactions (
    id                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    student_id        BIGINT UNSIGNED  NOT NULL,
    course_id         BIGINT UNSIGNED  NULL,
    lesson_id         BIGINT UNSIGNED  NULL,
    skill_code        VARCHAR(150)     NULL,
    item_type         ENUM('EXAM_QUESTION','ADAPTIVE_TASK') NOT NULL,
    -- tham chiếu đa hình (attempt_questions.id hoặc adaptive_generated_tasks.id): không có FK, service xác thực
    item_id           BIGINT UNSIGNED  NOT NULL,
    source_attempt_id BIGINT UNSIGNED  NULL,
    difficulty_level  ENUM('EASY','MEDIUM','HARD') NULL,
    score             DECIMAL(8,4)     NULL,
    max_score         DECIMAL(8,4)     NULL,
    is_correct        BOOLEAN          NULL,
    response_time_ms  BIGINT UNSIGNED  NULL,
    hints_used        TINYINT UNSIGNED NOT NULL DEFAULT 0,
    occurred_at       DATETIME(6)      NOT NULL,
    processed_at      DATETIME(6)      NULL,
    -- idempotency (BR-AI-08): EXAMQ:{attempt_question_id} hoặc TASK:{task_attempt_id}
    event_key         VARCHAR(150)     NOT NULL,
    metadata_json     JSON             NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_learning_interactions_event_key UNIQUE (event_key),
    KEY idx_interactions_student_time (student_id, occurred_at),
    KEY idx_interactions_skill_time (student_id, skill_code, occurred_at),
    KEY idx_interactions_unprocessed (processed_at, occurred_at),
    KEY idx_interactions_skill (skill_code),
    CONSTRAINT fk_interactions_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_interactions_course  FOREIGN KEY (course_id)  REFERENCES courses (id),
    CONSTRAINT fk_interactions_lesson  FOREIGN KEY (lesson_id)  REFERENCES lessons (id),
    CONSTRAINT ck_interactions_max_score CHECK (score IS NULL OR (max_score IS NOT NULL AND max_score > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE adaptive_task_sessions (
    id                        BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    student_id                BIGINT UNSIGNED   NOT NULL,
    course_id                 BIGINT UNSIGNED   NOT NULL,
    lesson_id                 BIGINT UNSIGNED   NULL,
    config_id                 BIGINT UNSIGNED   NULL,
    constraints_json          JSON              NULL,
    requested_topics_json     JSON              NULL,
    requested_task_types_json JSON              NULL,
    task_count                SMALLINT UNSIGNED NOT NULL,
    client_request_id         VARCHAR(64)       NULL,
    pipeline_version          VARCHAR(100)      NULL,
    retrieval_trace_json      JSON              NULL,
    profile_version           INT UNSIGNED      NULL,
    status                    ENUM('PENDING','PROCESSING','SUCCEEDED','FAILED') NOT NULL DEFAULT 'PENDING',
    error_code                ENUM('AI_NOT_CONFIGURED','NO_CANDIDATES','INSUFFICIENT_EVIDENCE','LLM_INVALID_OUTPUT','AI_UNAVAILABLE','INTERNAL_ERROR') NULL,
    last_error_message        TEXT              NULL,
    -- fencing token: mọi UPDATE của worker kèm WHERE status='PROCESSING' AND attempt_count=?
    attempt_count             SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    auto_retry_count          TINYINT UNSIGNED  NOT NULL DEFAULT 0,
    manual_retry_count        TINYINT UNSIGNED  NOT NULL DEFAULT 0,
    lease_expires_at          DATETIME(6)       NULL,
    worker_id                 VARCHAR(100)      NULL,
    next_retry_at             DATETIME(6)       NULL,
    created_at                DATETIME(6)       NOT NULL,
    completed_at              DATETIME(6)       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_adaptive_sessions_client_request UNIQUE (student_id, client_request_id),
    KEY idx_adaptive_sessions_student_time (student_id, created_at),
    KEY idx_adaptive_sessions_status (status, next_retry_at, lease_expires_at),
    CONSTRAINT fk_adaptive_sessions_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_adaptive_sessions_course  FOREIGN KEY (course_id)  REFERENCES courses (id),
    CONSTRAINT fk_adaptive_sessions_lesson  FOREIGN KEY (lesson_id)  REFERENCES lessons (id),
    CONSTRAINT fk_adaptive_sessions_config  FOREIGN KEY (config_id)  REFERENCES adaptive_practice_configs (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE adaptive_generated_tasks (
    id                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    session_id              BIGINT UNSIGNED   NOT NULL,
    position                SMALLINT UNSIGNED NOT NULL,
    origin                  ENUM('GENERATED','BANK') NOT NULL,
    source_question_id      BIGINT UNSIGNED   NULL,
    task_type               ENUM('MULTIPLE_CHOICE','SHORT_ANSWER') NOT NULL,
    skill_code              VARCHAR(150)      NOT NULL,
    difficulty_level        ENUM('EASY','MEDIUM','HARD') NOT NULL,
    content_json            JSON              NOT NULL,
    -- không trả Student trước khi nộp (AC-AI-12)
    answer_key_json         JSON              NOT NULL,
    explanation             TEXT              NULL,
    personalization_reason  TEXT              NOT NULL,
    fit_score               DECIMAL(6,5)      NULL,
    hint_steps_json         JSON              NULL,
    evidence_chunk_ids_json JSON              NULL,
    validation_status       ENUM('PENDING','VALID','REJECTED') NOT NULL DEFAULT 'PENDING',
    validation_errors_json  JSON              NULL,
    teacher_review_status   ENUM('NOT_REQUIRED','PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'NOT_REQUIRED',
    reviewed_by             BIGINT UNSIGNED   NULL,
    reviewed_at             DATETIME(6)       NULL,
    review_note             VARCHAR(500)      NULL,
    promoted_question_id    BIGINT UNSIGNED   NULL,
    created_at              DATETIME(6)       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_adaptive_tasks_session_position UNIQUE (session_id, position),
    CONSTRAINT uk_adaptive_tasks_promoted_question UNIQUE (promoted_question_id),
    KEY idx_adaptive_tasks_review (teacher_review_status, validation_status),
    CONSTRAINT fk_adaptive_tasks_session  FOREIGN KEY (session_id)           REFERENCES adaptive_task_sessions (id),
    CONSTRAINT fk_adaptive_tasks_source   FOREIGN KEY (source_question_id)   REFERENCES questions (id),
    CONSTRAINT fk_adaptive_tasks_reviewer FOREIGN KEY (reviewed_by)          REFERENCES users (id),
    CONSTRAINT fk_adaptive_tasks_promoted FOREIGN KEY (promoted_question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE adaptive_task_attempts (
    id                 BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    task_id            BIGINT UNSIGNED  NOT NULL,
    student_id         BIGINT UNSIGNED  NOT NULL,
    client_event_id    VARCHAR(64)      NOT NULL,
    answer_json        JSON             NULL,
    score              DECIMAL(8,4)     NULL,
    max_score          DECIMAL(8,4)     NULL,
    is_correct         BOOLEAN          NULL,
    feedback_text      TEXT             NULL,
    -- do server đếm từ adaptive_task_hint_events, không nhận từ client
    hints_used         TINYINT UNSIGNED NOT NULL DEFAULT 0,
    response_time_ms   BIGINT UNSIGNED  NULL,
    grading_method     ENUM('RULE_BASED','LLM_ASSISTED','TEACHER') NOT NULL DEFAULT 'RULE_BASED',
    grading_confidence DECIMAL(6,5)     NULL,
    submitted_at       DATETIME(6)      NULL,
    graded_at          DATETIME(6)      NULL,
    interaction_id     BIGINT UNSIGNED  NULL,
    created_at         DATETIME(6)      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_adaptive_attempts_task_student UNIQUE (task_id, student_id),
    CONSTRAINT uk_adaptive_attempts_client_event UNIQUE (student_id, client_event_id),
    CONSTRAINT uk_adaptive_attempts_interaction UNIQUE (interaction_id),
    CONSTRAINT fk_adaptive_attempts_task        FOREIGN KEY (task_id)        REFERENCES adaptive_generated_tasks (id),
    CONSTRAINT fk_adaptive_attempts_student     FOREIGN KEY (student_id)     REFERENCES users (id),
    CONSTRAINT fk_adaptive_attempts_interaction FOREIGN KEY (interaction_id) REFERENCES learning_interactions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- SHOULD HAVE: vẫn tạo bảng để schema ổn định, cắt giảm chỉ bỏ logic và UI (ERD Mục 2)
CREATE TABLE adaptive_task_hint_events (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    task_id         BIGINT UNSIGNED  NOT NULL,
    student_id      BIGINT UNSIGNED  NOT NULL,
    idempotency_key VARCHAR(64)      NOT NULL,
    hint_index      TINYINT UNSIGNED NOT NULL,
    created_at      DATETIME(6)      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_hint_events_idempotency UNIQUE (student_id, idempotency_key),
    CONSTRAINT uk_hint_events_task_student_index UNIQUE (task_id, student_id, hint_index),
    CONSTRAINT fk_hint_events_task    FOREIGN KEY (task_id)    REFERENCES adaptive_generated_tasks (id),
    CONSTRAINT fk_hint_events_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT ck_hint_events_index CHECK (hint_index >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
