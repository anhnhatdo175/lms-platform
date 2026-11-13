package com.example.lms.commerce;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Băm nội dung checkout để so khi gặp lại cùng {@code Idempotency-Key} (Spec 10.7). Chuẩn hóa trước khi băm,
 * nên " sale10 " và "SALE10", hoặc bỏ trống {@code paymentMethod} và gửi giá trị mặc định, cho cùng hash.
 */
public final class CheckoutRequestHasher {

    public static final String DEFAULT_PAYMENT_METHOD = "SEPAY_BANK_TRANSFER";

    private CheckoutRequestHasher() {
    }

    /** @return SHA-256 dạng hex thường, 64 ký tự, khớp {@code orders.checkout_request_hash CHAR(64)} */
    public static String hash(String couponCode, String paymentMethod) {
        String normalizedCoupon = couponCode == null || couponCode.isBlank() ? "" : Coupon.normalizeCode(couponCode);
        String normalizedMethod = paymentMethod == null || paymentMethod.isBlank() ? DEFAULT_PAYMENT_METHOD
                : paymentMethod.trim();
        String canonical = "couponCode=" + normalizedCoupon + "\npaymentMethod=" + normalizedMethod;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
        }
    }
}
