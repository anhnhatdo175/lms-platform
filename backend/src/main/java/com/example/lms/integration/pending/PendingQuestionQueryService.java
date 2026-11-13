package com.example.lms.integration.pending;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.lms.contract.question.QuestionQueryService;

/** BẢN TẠM của B để ứng dụng khởi động được khi C chưa có ngân hàng câu hỏi: không tìm thấy Question nào, nên Exam builder báo câu hỏi không hợp lệ. C thay. */
@Component
public class PendingQuestionQueryService implements QuestionQueryService {

    @Override
    public List<QuestionView> findQuestions(Collection<Long> questionIds) {
        return List.of();
    }
}
