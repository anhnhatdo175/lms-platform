package com.example.lms.exam;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttemptAnswerOptionRepository
        extends JpaRepository<AttemptAnswerOption, AttemptAnswerOption.Key> {

    List<AttemptAnswerOption> findByAttemptAnswerIdIn(Collection<Long> attemptAnswerIds);

    /**
     * Autosave ghi đè: xóa lựa chọn cũ rồi ghi lựa chọn mới trong cùng transaction. Không xóa persistence context
     * sau khi chạy, vì Attempt đang khóa và đang được tăng version trong cùng transaction.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from AttemptAnswerOption o where o.attemptAnswerId = :attemptAnswerId")
    int deleteByAttemptAnswerId(@Param("attemptAnswerId") long attemptAnswerId);
}
