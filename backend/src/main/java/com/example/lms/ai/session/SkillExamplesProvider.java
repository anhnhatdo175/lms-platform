package com.example.lms.ai.session;

import java.util.List;

/** Đề mẫu (chỉ nội dung đề, không đáp án) của một kỹ năng trong ngân hàng câu hỏi của Course. */
public interface SkillExamplesProvider {

    List<String> examples(long courseId, String skillCode, int limit);
}
