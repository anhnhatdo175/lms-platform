-- Người B — Payment và đối soát (ERD v2.2 Mục 7): payments, payment_webhook_logs, payment_reconciliation_actions.

CREATE TABLE payments (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id                BIGINT UNSIGNED NOT NULL,
    provider                ENUM('SEPAY','MANUAL') NOT NULL,
    method                  ENUM('BANK_QR','BANK_TRANSFER') NOT NULL,
    expected_amount         BIGINT UNSIGNED NOT NULL,
    received_amount         BIGINT UNSIGNED NULL,
    status                  ENUM('PENDING','PAID','REVIEW_REQUIRED','FAILED','CANCELLED') NOT NULL,
    payment_reference       VARCHAR(100)    NOT NULL,
    provider_transaction_id VARCHAR(255)    NULL,
    qr_content              TEXT            NULL,
    paid_at                 DATETIME(6)     NULL,
    review_reason_code      ENUM('ORDER_NOT_PAYABLE','AMOUNT_UNDERPAID','AMOUNT_OVERPAID') NULL,
    review_reason           VARCHAR(500)    NULL,
    paid_webhook_log_id     BIGINT UNSIGNED NULL,
    failed_reason           VARCHAR(500)    NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              DATETIME(6)     NOT NULL,
    updated_at              DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_reference UNIQUE (payment_reference),
    -- Một giao dịch ngân hàng chỉ gắn được vào một Payment (Spec 11.9)
    CONSTRAINT uk_payments_provider_transaction UNIQUE (provider, provider_transaction_id),
    KEY idx_payments_order_status (order_id, status),
    KEY idx_payments_status_review_reason (status, review_reason_code),
    KEY idx_payments_status_paid_at (status, paid_at),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payment_webhook_logs (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    payment_id              BIGINT UNSIGNED NULL,
    provider                ENUM('SEPAY','MANUAL') NOT NULL,
    provider_event_id       VARCHAR(255)    NULL,
    provider_transaction_id VARCHAR(255)    NULL,
    payment_reference       VARCHAR(100)    NULL,
    received_amount         BIGINT UNSIGNED NULL,
    payload                 LONGTEXT        NOT NULL,
    dedup_key               VARCHAR(300)    NOT NULL,
    -- Bằng dedup_key chỉ ở lần nhận đầu; các lần sau và webhook sai key để NULL.
    -- Quyết định "lần nhận đầu" do UNIQUE này chốt, không do code kiểm tra trước (Spec 11.4).
    primary_dedup_key       VARCHAR(300)    NULL,
    authenticated           BOOLEAN         NOT NULL DEFAULT FALSE,
    processing_status       ENUM('RECEIVED','VALIDATED','MATCHED','PROCESSED','DUPLICATE','UNMATCHED','FAILED') NOT NULL,
    unmatched_reason        ENUM('NO_REFERENCE','UNKNOWN_REFERENCE','ALREADY_PAID','PAYMENT_NOT_PENDING') NULL,
    error_message           TEXT            NULL,
    received_at             DATETIME(6)     NOT NULL,
    processed_at            DATETIME(6)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_webhook_logs_primary_dedup UNIQUE (primary_dedup_key),
    KEY idx_webhook_logs_reference (payment_reference),
    KEY idx_webhook_logs_dedup_key (dedup_key),
    KEY idx_webhook_logs_received_at (received_at),
    KEY idx_webhook_logs_status_received (processing_status, received_at),
    KEY idx_webhook_logs_provider_transaction (provider, provider_transaction_id),
    KEY idx_webhook_logs_status_reason_received (processing_status, unmatched_reason, received_at),
    CONSTRAINT fk_webhook_logs_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Vòng FK payments ↔ payment_webhook_logs: thêm sau khi cả hai bảng đã có (ERD Mục 15, ý 9b)
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_paid_webhook_log FOREIGN KEY (paid_webhook_log_id) REFERENCES payment_webhook_logs (id);

CREATE TABLE payment_reconciliation_actions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    webhook_log_id  BIGINT UNSIGNED NOT NULL,
    payment_id      BIGINT UNSIGNED NULL,
    order_id        BIGINT UNSIGNED NULL,
    performed_by    BIGINT UNSIGNED NOT NULL,
    action_type     ENUM('MATCH','APPROVE','REJECT','UNMATCH') NOT NULL,
    previous_status VARCHAR(50)     NULL,
    new_status      VARCHAR(50)     NULL,
    note            TEXT            NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_reconciliation_actions_webhook (webhook_log_id, created_at),
    CONSTRAINT fk_reconciliation_actions_webhook      FOREIGN KEY (webhook_log_id) REFERENCES payment_webhook_logs (id),
    CONSTRAINT fk_reconciliation_actions_payment      FOREIGN KEY (payment_id)     REFERENCES payments (id),
    CONSTRAINT fk_reconciliation_actions_order        FOREIGN KEY (order_id)       REFERENCES orders (id),
    CONSTRAINT fk_reconciliation_actions_performed_by FOREIGN KEY (performed_by)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
