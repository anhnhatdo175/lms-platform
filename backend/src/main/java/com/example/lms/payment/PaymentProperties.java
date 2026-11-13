package com.example.lms.payment;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình thanh toán của phần B.
 *
 * @param lateGraceMinutes     ân hạn sau {@code orders.expires_at} vẫn nhận thanh toán tự động (DEC-005, mặc định 15)
 * @param orderPaymentWindowHours hạn thanh toán hiển thị cho Student (Spec 10.6, mặc định 24)
 * @param referencePrefix      tiền tố mã thanh toán; phải khớp "Cấu trúc mã thanh toán" đã cấu hình trên SePay
 * @param sepay                thông tin tài khoản nhận tiền và API key webhook
 */
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        @DefaultValue("15") int lateGraceMinutes,
        @DefaultValue("24") int orderPaymentWindowHours,
        @DefaultValue("LMS") String referencePrefix,
        @DefaultValue Sepay sepay) {

    public Duration lateGrace() {
        return Duration.ofMinutes(lateGraceMinutes);
    }

    public Duration orderPaymentWindow() {
        return Duration.ofHours(orderPaymentWindowHours);
    }

    /**
     * @param apiKey          so với header {@code Authorization: Apikey <KEY>} của webhook
     * @param accountNumber   số tài khoản nhận; webhook về tài khoản khác bị bỏ qua
     * @param bankCode        mã ngắn ngân hàng dùng trong link ảnh QR
     * @param qrImageBaseUrl  host ảnh QR theo tài liệu SePay; để trong cấu hình vì host có thể đổi
     */
    public record Sepay(
            @DefaultValue("") String apiKey,
            @DefaultValue("") String accountNumber,
            @DefaultValue("") String bankCode,
            @DefaultValue("https://vietqr.app/img") String qrImageBaseUrl) {
    }
}
