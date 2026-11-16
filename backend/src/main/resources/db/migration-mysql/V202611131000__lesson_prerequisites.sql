CREATE TABLE lesson_prerequisites (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    lesson_id BIGINT UNSIGNED NOT NULL,
    prerequisite_lesson_id BIGINT UNSIGNED NULL,
    prerequisite_exam_id BIGINT UNSIGNED NULL,
    condition_type ENUM('COMPLETE_LESSON','PASS_EXAM') NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_lesson_prerequisites_lesson UNIQUE (lesson_id),
    CONSTRAINT chk_lesson_prerequisites_target CHECK (
        (condition_type = 'COMPLETE_LESSON'
            AND prerequisite_lesson_id IS NOT NULL
            AND prerequisite_exam_id IS NULL)
        OR
        (condition_type = 'PASS_EXAM'
            AND prerequisite_lesson_id IS NULL
            AND prerequisite_exam_id IS NOT NULL)
    ),
    CONSTRAINT chk_lesson_prerequisites_not_self CHECK (
        prerequisite_lesson_id IS NULL OR lesson_id <> prerequisite_lesson_id
    ),
    CONSTRAINT fk_lesson_prerequisites_lesson FOREIGN KEY (lesson_id)
        REFERENCES lessons (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_prerequisites_prerequisite_lesson FOREIGN KEY (prerequisite_lesson_id)
        REFERENCES lessons (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_prerequisites_prerequisite_exam FOREIGN KEY (prerequisite_exam_id)
        REFERENCES exams (id) ON DELETE CASCADE,
    INDEX idx_lesson_prerequisites_lesson_target (prerequisite_lesson_id),
    INDEX idx_lesson_prerequisites_exam_target (prerequisite_exam_id)
);
