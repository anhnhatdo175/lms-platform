package com.example.lms.payment.provider;

import java.math.BigDecimal;
import java.util.Map;

import com.example.lms.shared.json.JsonReader;

/**
 * Payload webhook SePay. {@code id} là mã giao dịch của SePay, giữ nguyên qua mọi lần gửi lại, nên dùng làm
 * khóa chống trùng; {@code code} là mã thanh toán SePay đã tách theo cấu hình (có thể {@code null});
 * {@code content} là nội dung chuyển khoản gốc; {@code transferType} là {@code in} hoặc {@code out}.
 */
public record SepayWebhookPayload(String transactionId, String gateway, String transactionDate, String accountNumber,
        String code, String content, String transferType, long transferAmount, String referenceCode) {

    /** @throws IllegalArgumentException JSON sai, thiếu {@code id} hoặc số tiền không hợp lệ */
    public static SepayWebhookPayload parse(String rawBody) {
        Map<String, Object> json = JsonReader.parseObject(rawBody);
        String id = integerText(json.get("id"), "id");
        Object amount = json.get("transferAmount");
        long transferAmount = amount == null ? 0 : Long.parseLong(integerText(amount, "transferAmount"));
        if (transferAmount < 0) {
            throw new IllegalArgumentException("transferAmount must not be negative");
        }
        return new SepayWebhookPayload(id, text(json.get("gateway")), text(json.get("transactionDate")),
                text(json.get("accountNumber")), text(json.get("code")), text(json.get("content")),
                text(json.get("transferType")), transferAmount, text(json.get("referenceCode")));
    }

    public boolean isIncoming() {
        return "in".equalsIgnoreCase(transferType);
    }

    public String dedupKey() {
        return "SEPAY:" + transactionId;
    }

    private static String integerText(Object value, String field) {
        if (value instanceof Long number) {
            return number.toString();
        }
        if (value instanceof BigDecimal decimal) {
            try {
                return decimal.toBigIntegerExact().toString();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(field + " must be an integer");
            }
        }
        if (value instanceof String string && string.matches("-?\\d+")) {
            return string;
        }
        throw new IllegalArgumentException(field + " is missing or not an integer");
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
