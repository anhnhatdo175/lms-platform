package com.example.lms.ai.session;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Kiểm tra cấu trúc, tất định: có đề, có đáp án chuẩn, có bằng chứng thuộc đúng tập chunk đã truy xuất, và gợi ý/lý do
 * không lộ đáp án. CHƯA phải bước verify độc lập bằng LLM của BR-AI-17 cho MULTIPLE_CHOICE (chưa có loại bài này ở
 * nhánh GENERATED); sẽ thay bằng bản kiểm chứng riêng khi thêm loại bài đó.
 */
@Component
public class StructuralTaskVerifier implements TaskVerifier {

    @Override
    public List<String> verify(AiServiceClient.GenerateOutcome o) {
        List<String> errors = new ArrayList<>();
        if (o.prompt() == null || o.prompt().isBlank()) {
            errors.add("prompt is empty");
        }
        String answer = o.answerKey() == null ? "" : String.valueOf(o.answerKey()).trim();
        if (answer.isEmpty()) {
            errors.add("answer key is missing");
        }
        if (o.supportingDocIds().isEmpty()) {
            errors.add("no supporting evidence");
        } else if (!o.retrievedDocIds().containsAll(o.supportingDocIds())) {
            errors.add("evidence is not among retrieved chunks");
        }
        if (!answer.isEmpty()) {
            Pattern leak = Pattern.compile("(?<!\\w)(?<!\\d\\.)" + Pattern.quote(answer) + "(?!\\w)(?!\\.\\d)",
                    Pattern.CASE_INSENSITIVE);
            for (String hint : o.hints()) {
                if (leak.matcher(hint).find()) {
                    errors.add("hint reveals the answer");
                    break;
                }
            }
            if (o.personalizationReason() != null && leak.matcher(o.personalizationReason()).find()) {
                errors.add("personalization reason reveals the answer");
            }
        }
        return errors;
    }
}
