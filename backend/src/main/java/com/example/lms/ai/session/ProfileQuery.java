package com.example.lms.ai.session;

import java.util.List;

/**
 * Dựng câu truy vấn truy xuất từ hồ sơ người học (phần "Profile-Based" của RAG). Mã kỹ năng thường là tiếng Anh ngắn
 * ("fractions") còn tài liệu là tiếng Việt, nên câu truy vấn ghép thêm đề mẫu cùng kỹ năng trong ngân hàng câu hỏi
 * của Course và cụm từ theo mức thành thạo: người yếu cần khái niệm và ví dụ từng bước, người mạnh cần vận dụng.
 * Chỉ dùng nội dung đề (không bao giờ dùng đáp án) và chỉ gửi sang AI service để truy xuất.
 */
public final class ProfileQuery {

    static final int MAX_LENGTH = 1500;
    static final int MAX_EXAMPLE_LENGTH = 300;

    private ProfileQuery() {
    }

    public static String focus(SkillPlanner.SkillPlan plan) {
        if (plan.coldStart()) {
            return "khái niệm cơ bản và ví dụ minh hoạ";
        }
        if (plan.mastery() < 0.4) {
            return "khái niệm cơ bản, cách làm từng bước, ví dụ minh hoạ";
        }
        if (plan.mastery() >= 0.7) {
            return "bài toán vận dụng và mở rộng";
        }
        return "phương pháp giải và bài luyện tập";
    }

    public static String build(SkillPlanner.SkillPlan plan, List<String> examples) {
        StringBuilder q = new StringBuilder(plan.skill().replace('_', ' ')).append(". ").append(focus(plan)).append('.');
        for (String e : examples) {
            if (e == null || e.isBlank()) {
                continue;
            }
            String one = e.strip().replaceAll("\\s+", " ");
            if (one.length() > MAX_EXAMPLE_LENGTH) {
                one = one.substring(0, MAX_EXAMPLE_LENGTH);
            }
            if (q.length() + one.length() + 1 > MAX_LENGTH) {
                break;
            }
            q.append(' ').append(one);
        }
        return q.toString();
    }
}
