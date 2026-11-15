package com.example.lms.ai.session;

import java.util.List;

/** Tài liệu AI đủ điều kiện truy xuất: READY, chưa gỡ, thuộc Course (và Lesson nếu có) (ERD 17.4). */
public interface KnowledgeScopeProvider {

    List<Long> readyDocumentIds(long courseId, Long lessonId);
}
