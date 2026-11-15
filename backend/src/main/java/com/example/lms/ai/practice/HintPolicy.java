package com.example.lms.ai.practice;

import java.util.List;

import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** Quy tắc cấp gợi ý: gợi ý đã sinh sẵn theo từng bước, mỗi key mới mở bước kế tiếp, tối đa {@code maxHints}. */
final class HintPolicy {

    private HintPolicy() {
    }

    /** @return chỉ số (1-based) của gợi ý kế tiếp; ném 422 {@code AI_HINTS_EXHAUSTED} khi hết lượt */
    static int nextIndex(List<String> steps, int maxHints, long used) {
        int allowed = Math.min(Math.max(maxHints, 0), steps == null ? 0 : steps.size());
        if (used >= allowed) {
            throw new BusinessException(ErrorCode.AI_HINTS_EXHAUSTED, 422, "No more hints for this task");
        }
        return (int) used + 1;
    }

    static int remaining(List<String> steps, int maxHints, int used) {
        int allowed = Math.min(Math.max(maxHints, 0), steps == null ? 0 : steps.size());
        return Math.max(0, allowed - used);
    }

    static String validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 64) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header required (1-64 chars)");
        }
        return key;
    }
}
