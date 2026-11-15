package com.example.lms.ai.session;

import java.math.BigDecimal;

import com.example.lms.ai.AdaptiveTaskType;
import com.example.lms.ai.TaskOrigin;
import com.example.lms.ai.ValidationStatus;
import com.example.lms.contract.question.Difficulty;

/** Nhiệm vụ do producer tạo, chưa có id; được ghi một lần ở cuối phiên (Spec 20A.14). */
public record TaskDraft(int position, TaskOrigin origin, Long sourceQuestionId, AdaptiveTaskType taskType,
        String skillCode, Difficulty difficulty, String contentJson, String answerKeyJson, String explanation,
        String personalizationReason, BigDecimal fitScore, String hintStepsJson, String evidenceChunkIdsJson,
        ValidationStatus validationStatus) {
}
