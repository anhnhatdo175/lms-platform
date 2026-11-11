package com.example.lms.exam;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    Optional<AttemptAnswer> findByAttemptIdAndAttemptQuestionId(long attemptId, long attemptQuestionId);

    List<AttemptAnswer> findByAttemptId(long attemptId);

    List<AttemptAnswer> findByAttemptIdIn(Collection<Long> attemptIds);
}
