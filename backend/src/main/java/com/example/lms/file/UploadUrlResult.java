package com.example.lms.file;

import java.time.Instant;
import java.util.Map;

public record UploadUrlResult(
        String uploadId,
        Long resourceId,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Instant expiresAt) {
}
