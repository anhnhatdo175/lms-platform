-- Người B — Commerce (ERD v2.2 Mục 6): carts, cart_items, coupons, coupon_courses, orders, order_items, coupon_redemptions.
-- Tiền là VND số nguyên đồng (BIGINT UNSIGNED), thời gian là UTC (DATETIME(6)).

CREATE TABLE carts (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6)     NOT NULL,
    updated_at DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
    id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cart_id   BIGINT UNSIGNED NOT NULL,
    course_id BIGINT UNSIGNED NOT NULL,
    added_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_cart_course UNIQUE (cart_id, course_id),
    -- CartItem chưa phát sinh lịch sử nên được CASCADE (ERD Mục 12)
    CONSTRAINT fk_cart_items_cart   FOREIGN KEY (cart_id)   REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE coupons (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code                    VARCHAR(50)     NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    discount_type           ENUM('PERCENTAGE','FIXED_AMOUNT') NOT NULL,
    discount_value          BIGINT UNSIGNED NOT NULL,
    minimum_order_amount    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    maximum_discount_amount BIGINT UNSIGNED NULL,
    total_usage_limit       INT UNSIGNED    NULL,
    per_user_limit          INT UNSIGNED    NULL,
    used_count              INT UNSIGNED    NOT NULL DEFAULT 0,
    scope                   ENUM('ALL_COURSES','SPECIFIC_COURSES') NOT NULL,
    starts_at               DATETIME(6)     NOT NULL,
    ends_at                 DATETIME(6)     NOT NULL,
    status                  ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              DATETIME(6)     NOT NULL,
    updated_at              DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupons_code UNIQUE (code),
    -- PERCENTAGE là số nguyên 1–100, FIXED_AMOUNT là số đồng ≥ 1 (ERD 6.3, OpenAPI CreateCouponRequest)
    CONSTRAINT chk_coupons_discount_value CHECK (discount_value >= 1 AND (discount_type <> 'PERCENTAGE' OR discount_value <= 100)),
    CONSTRAINT chk_coupons_period CHECK (ends_at > starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE coupon_courses (
    coupon_id  BIGINT UNSIGNED NOT NULL,
    course_id  BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6)     NOT NULL,
    PRIMARY KEY (coupon_id, course_id),
    CONSTRAINT fk_coupon_courses_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_coupon_courses_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE orders (
    id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code                     VARCHAR(50)     NOT NULL,
    user_id                  BIGINT UNSIGNED NOT NULL,
    coupon_id                BIGINT UNSIGNED NULL,
    coupon_code              VARCHAR(50)     NULL,
    subtotal                 BIGINT UNSIGNED NOT NULL,
    discount_amount          BIGINT UNSIGNED NOT NULL DEFAULT 0,
    total_amount             BIGINT UNSIGNED NOT NULL,
    checkout_idempotency_key VARCHAR(64)     NOT NULL,
    checkout_request_hash    CHAR(64)        NOT NULL,
    status                   ENUM('PENDING_PAYMENT','PAID','CANCELLED','EXPIRED') NOT NULL,
    expires_at               DATETIME(6)     NOT NULL,
    paid_at                  DATETIME(6)     NULL,
    cancelled_at             DATETIME(6)     NULL,
    version                  BIGINT          NOT NULL DEFAULT 0,
    created_at               DATETIME(6)     NOT NULL,
    updated_at               DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_code UNIQUE (code),
    -- Checkout idempotent: hai request cùng key do UNIQUE này chốt (Spec 10.7, ERD Mục 14)
    CONSTRAINT uk_orders_user_idempotency_key UNIQUE (user_id, checkout_idempotency_key),
    KEY idx_orders_user_created (user_id, created_at),
    KEY idx_orders_status_expires (status, expires_at),
    KEY idx_orders_expires (expires_at),
    CONSTRAINT chk_orders_total CHECK (total_amount + discount_amount = subtotal),
    CONSTRAINT fk_orders_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_items (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id             BIGINT UNSIGNED NOT NULL,
    course_id            BIGINT UNSIGNED NOT NULL,
    course_title         VARCHAR(255)    NOT NULL,
    original_price       BIGINT UNSIGNED NOT NULL,
    discount_amount      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    final_price          BIGINT UNSIGNED NOT NULL,
    access_duration_days INT UNSIGNED    NULL,
    created_at           DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_items_order_course UNIQUE (order_id, course_id),
    CONSTRAINT chk_order_items_price CHECK (final_price + discount_amount = original_price),
    CONSTRAINT fk_order_items_order  FOREIGN KEY (order_id)  REFERENCES orders (id),
    CONSTRAINT fk_order_items_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE coupon_redemptions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    coupon_id       BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NOT NULL,
    discount_amount BIGINT UNSIGNED NOT NULL,
    redeemed_at     DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    -- Webhook lặp hay Admin approve lần hai không ghi lượt dùng thêm (Spec 11.4)
    CONSTRAINT uk_coupon_redemptions_order UNIQUE (order_id),
    KEY idx_coupon_redemptions_coupon_user (coupon_id, user_id),
    CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_coupon_redemptions_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_coupon_redemptions_order  FOREIGN KEY (order_id)  REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
