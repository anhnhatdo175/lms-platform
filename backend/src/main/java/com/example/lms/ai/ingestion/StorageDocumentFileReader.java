package com.example.lms.ai.ingestion;

import org.springframework.stereotype.Component;

import com.example.lms.file.FileStorageService;

@Component
public class StorageDocumentFileReader implements DocumentFileReader {

    /** Khớp giới hạn của /v1/extract (20 MiB). */
    static final long MAX_BYTES = 20L * 1024 * 1024;

    private final FileStorageService storage;

    public StorageDocumentFileReader(FileStorageService storage) {
        this.storage = storage;
    }

    @Override
    public FileContent read(long fileResourceId) {
        FileStorageService.StoredFile f = storage.readContent(fileResourceId, MAX_BYTES);
        return new FileContent(f.name(), f.content());
    }
}
