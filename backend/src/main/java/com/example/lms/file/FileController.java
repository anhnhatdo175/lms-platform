package com.example.lms.file;

import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/teacher/files/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadUrlResult> createUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateUploadUrlRequest request) {
        return new ApiResponse<>(fileStorageService.createUploadUrl(principal, request));
    }

    @PostMapping("/teacher/files/complete-upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileResourceSummary> completeUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompleteUploadRequest request) {
        return new ApiResponse<>(fileStorageService.completeUpload(principal, request));
    }

    @GetMapping("/files/{resourceId}")
    public ApiResponse<FileResourceSummary> getResource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long resourceId) {
        return new ApiResponse<>(fileStorageService.getResource(principal, resourceId));
    }

    @DeleteMapping("/teacher/files/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long resourceId) {
        fileStorageService.deleteResource(principal, resourceId);
    }
}
