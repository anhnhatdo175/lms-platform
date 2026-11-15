package com.example.lms.ai.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.lms.ai.DocumentSourceType;
import com.example.lms.ai.ingestion.KnowledgeDocumentViews.CreateRequest;
import com.example.lms.shared.exception.BusinessException;

class KnowledgeDocumentValidationTest {

    private static CreateRequest text(String content) {
        return new CreateRequest(DocumentSourceType.PASTED_TEXT, "Bài 1", 1L, null, null, null, null, null, null,
                content);
    }

    private static CreateRequest file(Long fileId, String content) {
        return new CreateRequest(DocumentSourceType.LMS_FILE, "Bài 1", 1L, null, null, null, null, null, fileId,
                content);
    }

    private static void rejects(CreateRequest r) {
        try {
            KnowledgeDocumentService.validate(r);
        } catch (BusinessException e) {
            assertEquals(422, e.httpStatus());
            return;
        }
        throw new AssertionError("expected 422 for " + r);
    }

    @Test
    void validTextAndFileRequestsPass() {
        KnowledgeDocumentService.validate(text("nội dung"));
        KnowledgeDocumentService.validate(file(9L, null));
    }

    @Test
    void oneOfIsMutuallyExclusive() {
        rejects(text(null));
        rejects(text("  "));
        rejects(new CreateRequest(DocumentSourceType.PASTED_TEXT, "t", 1L, null, null, null, null, null, 5L, "x"));
        rejects(file(null, null));
        rejects(file(9L, "also text"));
    }

    @Test
    void missingOrInvalidCommonFieldsAre422() {
        rejects(null);
        rejects(new CreateRequest(null, "t", 1L, null, null, null, null, null, null, "x"));
        rejects(new CreateRequest(DocumentSourceType.PASTED_TEXT, " ", 1L, null, null, null, null, null, null, "x"));
        rejects(new CreateRequest(DocumentSourceType.PASTED_TEXT, "t", 0L, null, null, null, null, null, null, "x"));
        rejects(new CreateRequest(DocumentSourceType.BENCHMARK, "t", 1L, null, null, null, null, null, null, "x"));
    }

    @Test
    void textLongerThanLimitIsRejected() {
        rejects(text("a".repeat(KnowledgeDocumentService.MAX_TEXT + 1)));
        KnowledgeDocumentService.validate(text("a".repeat(KnowledgeDocumentService.MAX_TEXT)));
        assertTrue(KnowledgeDocumentService.FILE_TYPES.contains(".pdf"));
    }
}
