package com.example.lms.file;

import java.time.Instant;

public record FileResourceSummary(
        Long id,
        String fileName,
        String contentType,
        long sizeBytes,
        FileStatus status,
        String downloadUrl,
        Instant createdAt) {

    public static FileResourceSummary from(FileResource resource, String downloadUrl) {
        return new FileResourceSummary(
                resource.getId(),
                resource.getOriginalName(),
                resource.getMimeType(),
                resource.getFileSize(),
                resource.getStatus(),
                downloadUrl,
                resource.getCreatedAt());
    }
}
