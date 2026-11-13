package com.example.lms.contract.question;

import java.util.Collection;
import java.util.List;

/**
 * Điểm giao 7, C cung cấp: đọc Question và Option cho Exam builder và snapshot Attempt.
 * Kết quả có đáp án đúng, nên chỉ B dùng ở phía server; không trả thẳng cho Student.
 */
public interface QuestionQueryService {

    /** Thứ tự trả về không quan trọng; Question không tồn tại thì không có trong danh sách. */
    List<QuestionView> findQuestions(Collection<Long> questionIds);

    record QuestionView(long id, long courseId, QuestionType type, Difficulty difficulty, String skillCode,
            String content, String explanation, boolean active, List<OptionView> options) {

        public QuestionView {
            options = List.copyOf(options);
        }
    }

    record OptionView(long id, String content, boolean correct, int position) {
    }
}
