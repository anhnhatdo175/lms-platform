package com.example.lms.file;

import com.example.lms.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "file_resources", uniqueConstraints = @UniqueConstraint(
        name = "uk_file_resources_object_key",
        columnNames = "object_key"))
public class FileResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20)
    private StorageProvider storageProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FilePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileStatus status;

    @Column(length = 128)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected FileResource() {
    }

    public FileResource(
            User uploadedBy,
            String originalName,
            String objectKey,
            String mimeType,
            long fileSize,
            StorageProvider storageProvider,
            FilePurpose purpose) {
        this.uploadedBy = uploadedBy;
        this.originalName = originalName;
        this.objectKey = objectKey;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.storageProvider = storageProvider;
        this.purpose = purpose;
        this.status = FileStatus.PENDING;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public FilePurpose getPurpose() {
        return purpose;
    }

    public FileStatus getStatus() {
        return status;
    }

    public String getChecksum() {
        return checksum;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void complete(long actualSize, String actualMimeType, String checksum) {
        fileSize = actualSize;
        mimeType = actualMimeType;
        this.checksum = checksum;
        status = FileStatus.READY;
    }

    public void markDeleted(Instant deletedAt) {
        if (status != FileStatus.DELETED) {
            status = FileStatus.DELETED;
            this.deletedAt = deletedAt;
        }
    }
}
