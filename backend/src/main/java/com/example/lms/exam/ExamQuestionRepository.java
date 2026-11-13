package com.example.lms.exam;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, ExamQuestion.Key> {

    List<ExamQuestion> findByExamIdOrderByPositionAsc(long examId);

    long countByExamId(long examId);

    @Query("select coalesce(max(q.position), 0) from ExamQuestion q where q.examId = :examId")
    int findMaxPosition(@Param("examId") long examId);
}
