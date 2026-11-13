package com.example.lms.payment.provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Xác thực webhook SePay qua header {@code Authorization: Apikey <KEY>} (OpenAPI {@code SepayApiKey}).
 * So bằng {@link MessageDigest#isEqual} để thời gian so không lộ key đúng tới ký tự thứ mấy.
 * Key cấu hình rỗng thì từ chối mọi webhook, không bao giờ cho qua mặc định.
 */
public final class SepayApiKeyVerifier {

    private static final String SCHEME = "Apikey ";

    private final byte[] expectedKey;

    public SepayApiKeyVerifier(String configuredApiKey) {
        this.expectedKey = configuredApiKey == null || configuredApiKey.isBlank() ? null
                : configuredApiKey.trim().getBytes(StandardCharsets.UTF_8);
    }

    public boolean verify(String authorizationHeader) {
        if (expectedKey == null || authorizationHeader == null) {
            return false;
        }
        String header = authorizationHeader.trim();
        if (!header.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            return false;
        }
        byte[] providedKey = header.substring(SCHEME.length()).trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedKey, providedKey);
    }
}
