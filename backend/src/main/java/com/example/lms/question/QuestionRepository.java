package com.example.lms.question;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    @EntityGraph(attributePaths = "options")
    List<Question> findByIdIn(Collection<Long> ids);

    /** Question đã nằm trong snapshot của Attempt thì không được xóa logic (OpenAPI deactivateQuestion). */
    @Query(value = "SELECT COUNT(*) FROM attempt_questions WHERE source_question_id = :questionId", nativeQuery = true)
    long countAttemptSnapshots(long questionId);
}
