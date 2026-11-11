package com.example.lms.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Link ảnh QR chuyển khoản theo tài liệu SePay: {@code <base>?acc=&bank=&amount=&des=}. Nội dung chuyển khoản
 * ({@code des}) là mã thanh toán, để SePay tách được vào trường {@code code} của webhook.
 */
public final class QrCodeUrlBuilder {

    private QrCodeUrlBuilder() {
    }

    public static String build(PaymentProperties.Sepay sepay, long amount, String paymentReference) {
        return sepay.qrImageBaseUrl() + "?acc=" + encode(sepay.accountNumber()) + "&bank=" + encode(sepay.bankCode())
                + "&amount=" + amount + "&des=" + encode(paymentReference);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
