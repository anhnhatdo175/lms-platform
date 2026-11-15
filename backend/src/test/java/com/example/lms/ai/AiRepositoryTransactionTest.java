package com.example.lms.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

/**
 * UPDATE {@code @Modifying} khai báo trên interface không tự có transaction. Ingestion gọi chúng ngoài transaction, nên
 * thiếu {@code @Transactional} sẽ gây TransactionRequiredException và tài liệu không bao giờ READY.
 */
class AiRepositoryTransactionTest {

    @Test
    void everyModifyingQueryOpensItsOwnTransaction() {
        List<String> missing = new ArrayList<>();
        for (Class<?> repo : List.of(AiKnowledgeDocumentRepository.class, AdaptiveTaskSessionRepository.class)) {
            for (Method m : repo.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Modifying.class) && !m.isAnnotationPresent(Transactional.class)) {
                    missing.add(repo.getSimpleName() + "." + m.getName());
                }
            }
        }
        assertTrue(missing.isEmpty(), "missing @Transactional: " + missing);
    }
}
