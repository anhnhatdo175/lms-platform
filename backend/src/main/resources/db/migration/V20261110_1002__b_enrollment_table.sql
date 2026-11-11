-- Người B — Enrollment (ERD v2.2 Mục 8.1). Module Common: A và C đọc quyền học qua EnrollmentAccessService.
-- Không có UNIQUE(student_id, course_id) vì phải giữ lịch sử gia hạn; "một ACTIVE mỗi (User, Course)"
-- do service khóa theo (student, course) bảo đảm (ERD Mục 15, ý 1).

CREATE TABLE enrollments (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id     BIGINT UNSIGNED NOT NULL,
    course_id      BIGINT UNSIGNED NOT NULL,
    order_item_id  BIGINT UNSIGNED NULL,
    source         ENUM('PURCHASE','ADMIN_GRANTED','RENEWAL') NOT NULL,
    status         ENUM('ACTIVE','EXPIRED','REVOKED') NOT NULL DEFAULT 'ACTIVE',
    starts_at      DATETIME(6)     NOT NULL,
    expires_at     DATETIME(6)     NULL,
    revoked_at     DATETIME(6)     NULL,
    revoked_by     BIGINT UNSIGNED NULL,
    revoked_reason VARCHAR(500)    NULL,
    created_at     DATETIME(6)     NOT NULL,
    updated_at     DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    -- Mỗi OrderItem cấp tối đa một Enrollment; NULL (Admin grant) được lặp (DEC-005)
    CONSTRAINT uk_enrollments_order_item UNIQUE (order_item_id),
    KEY idx_enrollments_student_course_status (student_id, course_id, status),
    KEY idx_enrollments_status_expires (status, expires_at),
    KEY idx_enrollments_course_status (course_id, status),
    CONSTRAINT chk_enrollments_period CHECK (expires_at IS NULL OR expires_at > starts_at),
    CONSTRAINT fk_enrollments_student    FOREIGN KEY (student_id)    REFERENCES users (id),
    CONSTRAINT fk_enrollments_course     FOREIGN KEY (course_id)     REFERENCES courses (id),
    CONSTRAINT fk_enrollments_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT fk_enrollments_revoked_by FOREIGN KEY (revoked_by)    REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
