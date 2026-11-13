package com.example.lms.commerce;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    /** Lượt dùng của một User chỉ tính các Order đã thanh toán, vì redemption chỉ ghi khi Payment thành công. */
    long countByCouponIdAndUserId(long couponId, long userId);

    boolean existsByOrderId(long orderId);
}
