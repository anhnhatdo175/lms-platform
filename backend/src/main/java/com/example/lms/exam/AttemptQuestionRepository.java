package com.example.lms.exam;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, Long> {

    List<AttemptQuestion> findByAttemptIdOrderByPositionAsc(long attemptId);

    List<AttemptQuestion> findByAttemptIdIn(Collection<Long> attemptIds);

    /** Câu hỏi phải thuộc đúng Attempt đang lưu (Spec 14.5). */
    Optional<AttemptQuestion> findByIdAndAttemptId(long id, long attemptId);
}
