package com.example.lms.ai;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiDocumentChunkRepository extends JpaRepository<AiDocumentChunk, Long> {

    List<AiDocumentChunk> findByDocumentIdAndIndexVersionOrderByChunkIndex(long documentId, int indexVersion);

    long deleteByDocumentIdAndIndexVersionLessThan(long documentId, int indexVersion);
}
