package com.example.lms.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CompleteUploadRequest(
        @NotBlank @Size(max = 64) String uploadId,
        @Size(max = 255) String fileName,
        @Size(max = 100) String contentType,
        @Positive Long sizeBytes) {
}
