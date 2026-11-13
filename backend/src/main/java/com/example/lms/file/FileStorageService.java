package com.example.lms.file;

import com.example.lms.auth.AuthException;
import com.example.lms.auth.UserPrincipal;
import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(5);
    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024;
    private static final long MAX_COURSE_THUMBNAIL_BYTES = 10 * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final FileResourceRepository fileResourceRepository;
    private final UserRepository userRepository;
    private final MinioClient storageClient;
    private final MinioClient publicUrlClient;
    private final String bucket;

    public FileStorageService(
            FileResourceRepository fileResourceRepository,
            UserRepository userRepository,
            @Value("${app.file-storage.endpoint:http://localhost:9000}") String endpoint,
            @Value("${app.file-storage.public-endpoint:http://localhost:9000}") String publicEndpoint,
            @Value("${app.file-storage.access-key:minioadmin}") String accessKey,
            @Value("${app.file-storage.secret-key:minioadmin}") String secretKey,
            @Value("${app.file-storage.region:us-east-1}") String region,
            @Value("${app.file-storage.bucket:lms-files}") String bucket) {
        this.fileResourceRepository = fileResourceRepository;
        this.userRepository = userRepository;
        this.bucket = bucket;
        storageClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
        publicUrlClient = endpoint.equals(publicEndpoint)
                ? storageClient
                : MinioClient.builder()
                        .endpoint(publicEndpoint)
                        .credentials(accessKey, secretKey)
                        .region(region)
                        .build();
    }

    @Transactional
    public UploadUrlResult createUploadUrl(UserPrincipal principal, CreateUploadUrlRequest request) {
        validateUpload(principal, request);
        User uploader = userRepository.findById(principal.id())
                .orElseThrow(() -> new AuthException(
                        "USER_NOT_FOUND", "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));
        String objectKey = request.purpose().name().toLowerCase() + "/" + UUID.randomUUID();
        FileResource resource = fileResourceRepository.saveAndFlush(new FileResource(
                uploader,
                safeFileName(request.fileName()),
                objectKey,
                request.contentType().trim().toLowerCase(),
                request.sizeBytes(),
                StorageProvider.MINIO,
                request.purpose()));

        ensureBucket();
        String uploadUrl = presignedUrl(
                publicUrlClient,
                resource,
                Method.PUT,
                UPLOAD_URL_TTL,
                Map.of("Content-Type", resource.getMimeType()));
        return new UploadUrlResult(
                resource.getId().toString(),
                resource.getId(),
                uploadUrl,
                Method.PUT.name(),
                Map.of("Content-Type", resource.getMimeType()),
                Instant.now().plus(UPLOAD_URL_TTL));
    }

    @Transactional
    public FileResourceSummary completeUpload(UserPrincipal principal, CompleteUploadRequest request) {
        FileResource resource = findOwnedResource(principal.id(), request.uploadId());
        if (resource.getStatus() != FileStatus.PENDING) {
            throw new AuthException(
                    "UPLOAD_ALREADY_COMPLETED", "Tệp tải lên đã được xác nhận.", HttpStatus.CONFLICT);
        }

        String checksum;
        try {
            if (request.fileName() != null
                    && !safeFileName(request.fileName()).equals(resource.getOriginalName())) {
                throw uploadMetadataMismatch();
            }
            if (request.contentType() != null
                    && !request.contentType().trim().equalsIgnoreCase(resource.getMimeType())) {
                throw uploadMetadataMismatch();
            }
            var object = storageClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(resource.getObjectKey())
                    .build());
            if (object.size() != resource.getFileSize()
                    || !resource.getMimeType().equalsIgnoreCase(object.contentType())) {
                discardObject(resource);
                throw uploadMetadataMismatch();
            }
            if (request.sizeBytes() != null && request.sizeBytes() != object.size()) {
                discardObject(resource);
                throw uploadMetadataMismatch();
            }
            if (isImage(resource.getPurpose())) {
                validateImageContent(resource);
            }
            checksum = calculateChecksum(resource);
        } catch (AuthException exception) {
            throw exception;
        } catch (ErrorResponseException exception) {
            if ("NoSuchKey".equals(exception.errorResponse().code())) {
                throw new AuthException(
                        "UPLOAD_NOT_FOUND", "Không tìm thấy nội dung tệp vừa tải lên.", HttpStatus.CONFLICT);
            }
            logger.error("Object storage rejected upload completion for resource {}.", resource.getId(), exception);
            throw storageUnavailable();
        } catch (Exception exception) {
            logger.error("Unable to verify uploaded resource {}.", resource.getId(), exception);
            throw storageUnavailable();
        }
        resource.complete(resource.getFileSize(), resource.getMimeType(), checksum);
        fileResourceRepository.save(resource);
        return FileResourceSummary.from(resource, downloadUrl(resource));
    }

    @Transactional(readOnly = true)
    public FileResourceSummary getResource(UserPrincipal principal, Long resourceId) {
        FileResource resource = fileResourceRepository.findById(resourceId)
                .orElseThrow(() -> new AuthException(
                        "FILE_NOT_FOUND", "Không tìm thấy tệp.", HttpStatus.NOT_FOUND));
        if (!resource.getUploadedBy().getId().equals(principal.id())
                && !principal.roles().contains(com.example.lms.user.UserRole.ADMIN)) {
            throw new AuthException("FORBIDDEN", "Bạn không có quyền truy cập tệp này.", HttpStatus.FORBIDDEN);
        }
        String url = resource.getStatus() == FileStatus.READY ? downloadUrl(resource) : null;
        return FileResourceSummary.from(resource, url);
    }

    @Transactional
    public void deleteResource(UserPrincipal principal, Long resourceId) {
        boolean isAdmin = principal.roles().contains(UserRole.ADMIN);
        if (!isAdmin && !principal.roles().contains(UserRole.TEACHER)) {
            throw new AuthException(
                    "FORBIDDEN", "Chỉ giảng viên hoặc quản trị viên mới có thể xóa tài nguyên.", HttpStatus.FORBIDDEN);
        }
        FileResource resource = fileResourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new AuthException(
                        "FILE_NOT_FOUND", "Không tìm thấy tệp.", HttpStatus.NOT_FOUND));
        if (!isAdmin && !resource.getUploadedBy().getId().equals(principal.id())) {
            throw new AuthException("FORBIDDEN", "Bạn không có quyền xóa tệp này.", HttpStatus.FORBIDDEN);
        }
        if (resource.getStatus() == FileStatus.DELETED) {
            return;
        }
        if (userRepository.existsByAvatarFileId(resourceId)) {
            throw new AuthException(
                    "FILE_RESOURCE_IN_USE",
                    "Hãy gỡ tài nguyên khỏi hồ sơ đang sử dụng trước khi xóa.",
                    HttpStatus.CONFLICT);
        }
        resource.markDeleted(Instant.now());
        fileResourceRepository.save(resource);
    }

    @Transactional
    public void validateAvatar(Long resourceId, Long userId) {
        if (resourceId == null) {
            return;
        }
        FileResource resource = fileResourceRepository.findByIdAndUploadedBy_Id(resourceId, userId)
                .filter(file -> file.getPurpose() == FilePurpose.USER_AVATAR)
                .filter(file -> file.getStatus() == FileStatus.READY)
                .orElseThrow(() -> new AuthException(
                        "INVALID_AVATAR_RESOURCE",
                        "Ảnh đại diện không hợp lệ hoặc không thuộc tài khoản của bạn.",
                        HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Transactional(readOnly = true)
    public void validateCourseThumbnail(Long resourceId) {
        if (resourceId == null) return;
        fileResourceRepository.findById(resourceId)
                .filter(file -> file.getPurpose() == FilePurpose.COURSE_THUMBNAIL)
                .filter(file -> file.getStatus() == FileStatus.READY)
                .orElseThrow(() -> new AuthException(
                        "INVALID_THUMBNAIL_RESOURCE",
                        "Ảnh khóa học phải là tài nguyên COURSE_THUMBNAIL đã tải lên thành công.",
                        HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Transactional(readOnly = true)
    public void validateLessonResource(Long resourceId, Long uploaderId) {
        if (resourceId == null) return;
        fileResourceRepository.findById(resourceId)
                .filter(file -> file.getPurpose() == FilePurpose.LESSON_RESOURCE)
                .filter(file -> file.getStatus() == FileStatus.READY)
                .filter(file -> file.getUploadedBy().getId().equals(uploaderId))
                .orElseThrow(() -> new AuthException(
                        "INVALID_LESSON_RESOURCE",
                        "Tài nguyên bài học phải là tệp LESSON_RESOURCE đã tải lên thành công bởi bạn.",
                        HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Transactional(readOnly = true)
    public String courseThumbnailUrl(Long resourceId) {
        if (resourceId == null) return null;
        return fileResourceRepository.findById(resourceId)
                .filter(file -> file.getPurpose() == FilePurpose.COURSE_THUMBNAIL)
                .filter(file -> file.getStatus() == FileStatus.READY)
                .map(this::downloadUrl)
                .orElse(null);
    }

    private void validateUpload(UserPrincipal principal, CreateUploadUrlRequest request) {
        if (request.purpose() == FilePurpose.USER_AVATAR) {
            if (request.sizeBytes() > MAX_AVATAR_BYTES) {
                throw new AuthException(
                        "AVATAR_TOO_LARGE", "Ảnh đại diện không được vượt quá 5 MiB.", HttpStatus.PAYLOAD_TOO_LARGE);
            }
            String contentType = request.contentType().trim().toLowerCase();
            if (!IMAGE_TYPES.contains(contentType)) {
                throw new AuthException(
                        "INVALID_AVATAR_TYPE",
                        "Ảnh đại diện chỉ hỗ trợ JPEG, PNG hoặc WebP.",
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
            return;
        }
        if (!principal.roles().contains(com.example.lms.user.UserRole.ADMIN)
                && !principal.roles().contains(com.example.lms.user.UserRole.TEACHER)) {
            throw new AuthException(
                    "FORBIDDEN", "Chỉ giảng viên mới có thể tải tài nguyên khóa học.", HttpStatus.FORBIDDEN);
        }
        if (request.purpose() == FilePurpose.COURSE_THUMBNAIL
                && (request.sizeBytes() > MAX_COURSE_THUMBNAIL_BYTES
                || !IMAGE_TYPES.contains(request.contentType().trim().toLowerCase()))) {
            throw new AuthException(
                    "INVALID_COURSE_THUMBNAIL",
                    "Ảnh khóa học phải là JPEG, PNG hoặc WebP và không vượt quá 10 MiB.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private FileResource findOwnedResource(Long userId, String uploadId) {
        long resourceId;
        try {
            resourceId = Long.parseLong(uploadId);
        } catch (NumberFormatException exception) {
            throw new AuthException("INVALID_UPLOAD_ID", "Mã tải lên không hợp lệ.", HttpStatus.BAD_REQUEST);
        }
        return fileResourceRepository.findByIdAndUploadedBy_Id(resourceId, userId)
                .orElseThrow(() -> new AuthException(
                        "UPLOAD_NOT_FOUND", "Không tìm thấy lượt tải lên.", HttpStatus.NOT_FOUND));
    }

    private String calculateChecksum(FileResource resource) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = storageClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(resource.getObjectKey())
                .build());
                DigestOutputStream output = new DigestOutputStream(OutputStream.nullOutputStream(), digest)) {
            input.transferTo(output);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void validateImageContent(FileResource resource) throws Exception {
        byte[] header;
        try (InputStream input = storageClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(resource.getObjectKey())
                .build())) {
            header = input.readNBytes(12);
        }
        String detectedType = detectImageType(header);
        if (!resource.getMimeType().equalsIgnoreCase(detectedType)) {
            discardObject(resource);
            throw new AuthException(
                    "UPLOAD_CONTENT_TYPE_MISMATCH",
                    "Nội dung ảnh không khớp với định dạng đã khai báo.",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private void discardObject(FileResource resource) {
        try {
            storageClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(resource.getObjectKey())
                    .build());
        } catch (Exception exception) {
            logger.warn("Unable to remove rejected uploaded object for resource {}.", resource.getId(), exception);
        }
    }

    private static boolean isImage(FilePurpose purpose) {
        return purpose == FilePurpose.USER_AVATAR || purpose == FilePurpose.COURSE_THUMBNAIL;
    }

    private static String detectImageType(byte[] header) {
        if (header.length >= 3
                && (header[0] & 0xff) == 0xff
                && (header[1] & 0xff) == 0xd8
                && (header[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        byte[] pngSignature = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
        if (header.length >= pngSignature.length) {
            boolean isPng = true;
            for (int i = 0; i < pngSignature.length; i++) {
                if (header[i] != pngSignature[i]) {
                    isPng = false;
                    break;
                }
            }
            if (isPng) return "image/png";
        }
        if (header.length >= 12
                && "RIFF".equals(new String(header, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(header, 8, 4, StandardCharsets.US_ASCII))) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private String downloadUrl(FileResource resource) {
        return presignedUrl(publicUrlClient, resource, Method.GET, DOWNLOAD_URL_TTL, Map.of());
    }

    private String presignedUrl(
            MinioClient client,
            FileResource resource,
            Method method,
            Duration ttl,
            Map<String, String> headers) {
        try {
            ensureBucket();
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(method)
                    .bucket(bucket)
                    .object(resource.getObjectKey())
                    .expiry((int) ttl.toSeconds(), TimeUnit.SECONDS)
                    .extraHeaders(headers)
                    .build());
        } catch (Exception exception) {
            logger.error("Unable to create a temporary storage URL for resource {}.", resource.getId(), exception);
            throw storageUnavailable();
        }
    }

    private void ensureBucket() {
        try {
            if (!storageClient.bucketExists(
                    io.minio.BucketExistsArgs.builder().bucket(bucket).build())) {
                try {
                    storageClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                } catch (Exception exception) {
                    if (!storageClient.bucketExists(
                            io.minio.BucketExistsArgs.builder().bucket(bucket).build())) {
                        throw exception;
                    }
                }
            }
        } catch (Exception exception) {
            logger.error("Object storage bucket '{}' is unavailable.", bucket, exception);
            throw storageUnavailable();
        }
    }

    private static String safeFileName(String fileName) {
        String normalized = fileName.trim().replace('\\', '/');
        int lastSeparator = normalized.lastIndexOf('/');
        String basename = lastSeparator >= 0 ? normalized.substring(lastSeparator + 1) : normalized;
        String safeName = basename.replaceAll("[\\p{Cntrl}]", "").trim();
        if (safeName.isBlank()) {
            throw new AuthException("INVALID_FILE_NAME", "Tên tệp không hợp lệ.", HttpStatus.BAD_REQUEST);
        }
        return safeName;
    }

    private static AuthException storageUnavailable() {
        return new AuthException(
                "FILE_STORAGE_UNAVAILABLE",
                "Kho lưu trữ tệp hiện không khả dụng. Vui lòng thử lại sau.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static AuthException uploadMetadataMismatch() {
        return new AuthException(
                "UPLOAD_METADATA_MISMATCH",
                "Thông tin tệp tải lên không khớp với dữ liệu đã khai báo.",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
