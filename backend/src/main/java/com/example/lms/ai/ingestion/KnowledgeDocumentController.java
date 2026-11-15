package com.example.lms.ai.ingestion;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.ai.ingestion.KnowledgeDocumentViews.CreateRequest;
import com.example.lms.ai.ingestion.KnowledgeDocumentViews.DocumentView;
import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.shared.pagination.PageRequests;
import com.example.lms.shared.pagination.PageResponse;
import com.example.lms.shared.web.ApiResponse;

/** OpenAPI tag {@code teacher}: tài liệu tri thức AI. */
@RestController
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService service;
    private final CurrentUserProvider currentUser;

    public KnowledgeDocumentController(KnowledgeDocumentService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/teacher/ai/knowledge-documents")
    public ResponseEntity<ApiResponse<DocumentView>> create(@RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(service.create(currentUser.current(), request)));
    }

    @GetMapping("/teacher/ai/knowledge-documents")
    public PageResponse<DocumentView> list(@RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long lessonId, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(service.list(currentUser.current(), courseId, lessonId, status,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"))), v -> v);
    }

    @GetMapping("/teacher/ai/knowledge-documents/{documentId}")
    public ApiResponse<DocumentView> get(@PathVariable long documentId) {
        return ApiResponse.of(service.get(currentUser.current(), documentId));
    }

    @DeleteMapping("/teacher/ai/knowledge-documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable long documentId) {
        service.delete(currentUser.current(), documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teacher/ai/knowledge-documents/{documentId}/reindex")
    public ResponseEntity<ApiResponse<DocumentView>> reindex(@PathVariable long documentId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(service.reindex(currentUser.current(), documentId)));
    }
}
