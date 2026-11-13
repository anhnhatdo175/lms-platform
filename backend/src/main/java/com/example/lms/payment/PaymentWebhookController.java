package com.example.lms.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * {@code POST /webhooks/payments/sepay}. Không dùng JWT: cấu hình bảo mật chung phải cho endpoint này đi qua
 * không cần đăng nhập; xác thực bằng header {@code Authorization: Apikey <KEY>}.
 *
 * <p>SePay chỉ coi là thành công khi nhận HTTP 200/201 với body đúng {@code {"success": true}} trong 30 giây.
 */
@RestController
public class PaymentWebhookController {

    public record WebhookAck(boolean success) {
    }

    private final PaymentWebhookService webhooks;

    public PaymentWebhookController(PaymentWebhookService webhooks) {
        this.webhooks = webhooks;
    }

    @PostMapping(path = "/webhooks/payments/sepay", consumes = "application/json", produces = "application/json")
    public ResponseEntity<WebhookAck> sepay(@RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String rawBody) {
        if (webhooks.receive(authorization, rawBody) == PaymentWebhookService.Result.UNAUTHORIZED) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Invalid webhook API key");
        }
        return ResponseEntity.ok(new WebhookAck(true));
    }
}
