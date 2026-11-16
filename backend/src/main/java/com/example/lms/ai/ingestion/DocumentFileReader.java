package com.example.lms.ai.ingestion;

/** Cổng đọc nội dung tệp của một tài liệu LMS_FILE (cài đặt bằng kho lưu trữ tệp). */
public interface DocumentFileReader {

    /** @throws RuntimeException khi tệp không tồn tại, chưa sẵn sàng hoặc không đọc được */
    FileContent read(long fileResourceId);

    record FileContent(String name, byte[] content) {
    }
}
