"""Sinh script SQL dữ liệu demo cho phần AI (deterministic, idempotent, không có dữ liệu thật).

Chạy:  python research/demo/generate_demo_sql.py > research/demo/seed_demo_ai.sql

Script SQL KHÔNG tạo user/course/lesson (thuộc phần A). Điền các biến @teacher_id, @course_id, @lesson_id,
@student_strong, @student_weak, @student_cold ở đầu file bằng id đã có, rồi chạy trên DB đã migrate.
Hồ sơ được tính bằng đúng công thức của backend (ProfileMath: EMA alpha 0.3, cold start 0.5, phạt gợi ý 0.3).
"""
from __future__ import annotations

import math
import sys

ALPHA, HINT_PENALTY, COLD = 0.3, 0.3, 0.5
STRONG_AT, WEAK_BELOW, MIN_EVIDENCE = 0.7, 0.4, 3

SKILLS = {
    "fractions": "Phân số",
    "decimals": "Số thập phân",
    "percentages": "Phần trăm",
}

# (skill, difficulty, content, [(option, correct)], explanation)
QUESTIONS = [
    ("fractions", "EASY", "Phân số nào bằng 1/2?", [("2/4", 1), ("2/3", 0), ("3/4", 0), ("1/3", 0)],
     "2/4 rút gọn cho 2 được 1/2."),
    ("fractions", "MEDIUM", "Tính 1/3 + 1/6.", [("1/2", 1), ("2/9", 0), ("1/9", 0), ("2/3", 0)],
     "Quy đồng: 2/6 + 1/6 = 3/6 = 1/2."),
    ("fractions", "HARD", "Tính 3/4 : 3/8.", [("2", 1), ("1/2", 0), ("9/32", 0), ("4", 0)],
     "3/4 × 8/3 = 2."),
    ("decimals", "EASY", "Số nào lớn nhất: 0,5; 0,45; 0,405; 0,54?", [("0,54", 1), ("0,5", 0), ("0,45", 0), ("0,405", 0)],
     "So sánh phần thập phân: 0,54 lớn nhất."),
    ("decimals", "MEDIUM", "Tính 2,5 × 0,4.", [("1", 1), ("10", 0), ("0,1", 0), ("2,9", 0)],
     "25 × 4 = 100, có hai chữ số thập phân nên bằng 1."),
    ("decimals", "HARD", "Tính 7,2 : 0,06.", [("120", 1), ("12", 0), ("1,2", 0), ("1200", 0)],
     "Nhân cả hai số với 100: 720 : 6 = 120."),
    ("percentages", "EASY", "25% của 80 là bao nhiêu?", [("20", 1), ("25", 0), ("32", 0), ("5", 0)],
     "80 × 25 / 100 = 20."),
    ("percentages", "MEDIUM", "Giá 200 000 đồng giảm 15%. Giá sau giảm?", [("170 000", 1), ("185 000", 0), ("30 000", 0), ("215 000", 0)],
     "200 000 × 0,85 = 170 000."),
    ("percentages", "HARD", "Số nào tăng 20% thì được 90?", [("75", 1), ("72", 0), ("108", 0), ("70", 0)],
     "x × 1,2 = 90 nên x = 75."),
]

DOCS = [
    ("Phân số: khái niệm và các phép tính", "Toán", "5",
     "Phân số a/b biểu thị a phần của một đơn vị được chia thành b phần bằng nhau. Hai phân số bằng nhau khi "
     "chúng cùng biểu thị một giá trị, ví dụ 1/2 = 2/4 = 3/6. Muốn cộng hoặc trừ hai phân số khác mẫu số, ta quy "
     "đồng mẫu số rồi cộng hoặc trừ các tử số. Muốn nhân hai phân số, nhân tử với tử và mẫu với mẫu. Muốn chia "
     "cho một phân số, ta nhân với phân số nghịch đảo của nó. Ví dụ: 3/4 : 3/8 = 3/4 × 8/3 = 2. Kết quả nên được "
     "rút gọn về phân số tối giản."),
    ("Số thập phân: so sánh và phép nhân chia", "Toán", "5",
     "Số thập phân gồm phần nguyên và phần thập phân ngăn cách bởi dấu phẩy. Để so sánh hai số thập phân, so sánh "
     "phần nguyên trước, nếu bằng nhau thì so sánh lần lượt từng chữ số phần thập phân từ trái sang phải. Khi nhân "
     "hai số thập phân, ta nhân như số tự nhiên rồi đếm tổng số chữ số ở phần thập phân của hai thừa số để đặt dấu "
     "phẩy. Khi chia cho một số thập phân, ta nhân cả số bị chia và số chia với 10, 100, ... để số chia thành số "
     "tự nhiên rồi thực hiện phép chia."),
    ("Tỉ số phần trăm và bài toán giảm giá", "Toán", "5",
     "Tỉ số phần trăm của a so với b là a/b × 100%. Muốn tìm p% của một số, ta nhân số đó với p rồi chia cho 100. "
     "Muốn tìm một số khi biết p% của nó bằng m, ta lấy m chia cho p rồi nhân 100. Trong bài toán giảm giá, giá "
     "sau khi giảm p% bằng giá gốc nhân với (100 - p)/100. Trong bài toán tăng, giá mới bằng giá gốc nhân với "
     "(100 + p)/100. Ví dụ giá 200 000 đồng giảm 15% còn 200 000 × 0,85 = 170 000 đồng."),
]

# Lịch sử mô phỏng: (skill, difficulty, score_ratio, hints, days_ago). Cũ -> mới.
HISTORY = {
    "strong": [(s, d, r, 0, 30 - i) for i, (s, d, r) in enumerate([
        ("fractions", "EASY", 1), ("decimals", "EASY", 1), ("percentages", "EASY", 1),
        ("fractions", "MEDIUM", 1), ("decimals", "MEDIUM", 1), ("percentages", "MEDIUM", 1),
        ("fractions", "MEDIUM", 1), ("decimals", "HARD", 1), ("percentages", "MEDIUM", 0),
        ("fractions", "HARD", 1), ("decimals", "HARD", 1), ("percentages", "HARD", 1),
    ])],
    "weak": [(s, d, r, h, 25 - i) for i, (s, d, r, h) in enumerate([
        ("fractions", "EASY", 0, 0), ("decimals", "EASY", 1, 0), ("percentages", "EASY", 1, 0),
        ("fractions", "EASY", 0, 0), ("decimals", "MEDIUM", 1, 1), ("fractions", "EASY", 0, 0),
        ("decimals", "MEDIUM", 0, 0), ("fractions", "MEDIUM", 0, 0), ("fractions", "EASY", 1, 2),
        ("decimals", "MEDIUM", 1, 0),
    ])],
    "cold": [],
}
# "weak" thiếu percentages (chỉ 1 bằng chứng -> cold start cho kỹ năng đó); "cold" chưa có dữ liệu.


def outcome(ratio: float, hints: int) -> float:
    return max(0.0, min(1.0, ratio)) * max(0.0, 1.0 - HINT_PENALTY * hints)


def replay(history):
    states: dict[str, list[float]] = {}  # skill -> [mastery, evidence]
    for skill, _d, ratio, hints, _days in history:
        m, n = states.get(skill, [COLD, 0])
        start = COLD if n == 0 else m
        states[skill] = [(1 - ALPHA) * start + ALPHA * outcome(ratio, hints), n + 1]
    return states


def q(text: str) -> str:
    return "'" + text.replace("\\", "\\\\").replace("'", "''") + "'"


def main() -> None:
    out: list[str] = []
    w = out.append
    w("-- Dữ liệu demo AI (sinh bởi research/demo/generate_demo_sql.py; không sửa tay).")
    w("-- Điền các id đã có trong DB (user/course/lesson thuộc phần A), rồi chạy trên DB đã migrate.")
    w("SET @teacher_id = 1;   -- Teacher sở hữu Course")
    w("SET @course_id = 1;")
    w("SET @lesson_id = 1;    -- Lesson thuộc Course (dùng cho tương tác mẫu)")
    w("SET @student_strong = 2;")
    w("SET @student_weak = 3;")
    w("SET @student_cold = 4; -- không có dòng nào: demo cold start")
    w("SET @now = NOW(6);")
    w("")
    w("-- Cấu hình luyện tập do Teacher đặt (Course mặc định).")
    skills_json = "[" + ",".join('"%s"' % s for s in SKILLS) + "]"
    w("INSERT INTO adaptive_practice_configs (course_id, lesson_id, enabled, subject_code, grade_level, "
      "allowed_skills_json, min_difficulty, max_difficulty, allowed_task_types_json, max_tasks_per_session, "
      "socratic_enabled, max_hints_per_task, updated_by, created_at, updated_at)")
    w("VALUES (@course_id, NULL, TRUE, 'Toán', '5', %s, 'EASY', 'HARD', '[\"SHORT_ANSWER\"]', 5, TRUE, 3, "
      "@teacher_id, @now, @now)" % q(skills_json))
    w("ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), allowed_skills_json = VALUES(allowed_skills_json), "
      "updated_at = VALUES(updated_at);")
    w("")
    w("-- Tài liệu nguồn: PENDING, worker ingestion sẽ lập chỉ mục và chuyển READY.")
    for title, subject, grade, text in DOCS:
        w("INSERT INTO ai_knowledge_documents (course_id, source_text, title, source_type, language_code, "
          "subject_code, grade_level, index_status, index_version, attempt_count, created_by, created_at, updated_at)")
        w("SELECT @course_id, %s, %s, 'PASTED_TEXT', 'vi', %s, %s, 'PENDING', 1, 0, @teacher_id, @now, @now FROM DUAL"
          % (q(text), q(title), q(subject), q(grade)))
        w("WHERE NOT EXISTS (SELECT 1 FROM ai_knowledge_documents WHERE course_id = @course_id AND title = %s);" % q(title))
    w("")
    w("-- Ngân hàng câu hỏi có skill_code.")
    for skill, diff, content, options, expl in QUESTIONS:
        w("INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, "
          "active, created_by, created_at, updated_at)")
        w("SELECT @course_id, %s, 'SINGLE_CHOICE', '%s', '%s', 'MANUAL', %s, TRUE, @teacher_id, @now, @now FROM DUAL"
          % (q(content), diff, skill, q(expl)))
        w("WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = %s);" % q(content))
        w("SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = %s LIMIT 1);" % q(content))
        rows = ", ".join("(@q, %s, %s, %d, @now, @now)" % (q(t), "TRUE" if c else "FALSE", i + 1)
                         for i, (t, c) in enumerate(options))
        w("INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES "
          + rows + " ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);")
    w("")
    for key, student in (("strong", "@student_strong"), ("weak", "@student_weak")):
        history = HISTORY[key]
        w("-- Học viên %s: %d tương tác, đã xử lý." % (key, len(history)))
        for n, (skill, diff, ratio, hints, days) in enumerate(history, 1):
            w("INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, "
              "item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)")
            w("VALUES (%s, @course_id, @lesson_id, '%s', 'EXAM_QUESTION', %d, '%s', %s, 1, %s, %d, "
              "@now - INTERVAL %d DAY, @now - INTERVAL %d DAY, 'DEMO:%s:%d');"
              % (student, skill, n, diff, ratio, "TRUE" if ratio >= 0.5 else "FALSE", hints, days, days, key, n))
        states = replay(history)
        for skill, (mastery, evidence) in states.items():
            w("INSERT INTO learner_skill_states (student_id, skill_code, skill_name, subject_code, mastery_score, "
              "evidence_count, last_evidence_at, model_version, updated_at)")
            w("VALUES (%s, '%s', %s, 'Toán', %.5f, %d, @now, 'ema-0.3', @now)" % (student, skill, q(SKILLS[skill]), mastery, evidence))
            w("ON DUPLICATE KEY UPDATE mastery_score = VALUES(mastery_score), evidence_count = VALUES(evidence_count), "
              "last_evidence_at = VALUES(last_evidence_at), model_version = VALUES(model_version), updated_at = VALUES(updated_at);")
        values = [m for m, _ in states.values()]
        mean = sum(values) / len(values)
        std = math.sqrt(sum((v - mean) ** 2 for v in values) / len(values))
        strong = [s for s, (m, n) in states.items() if n >= MIN_EVIDENCE and m >= STRONG_AT]
        weak = [s for s, (m, n) in states.items() if n >= MIN_EVIDENCE and m < WEAK_BELOW]
        j = lambda xs: q("[" + ",".join('"%s"' % x for x in xs) + "]")
        w("INSERT INTO learner_profiles (student_id, average_score, score_stddev, strength_topics_json, "
          "weak_topics_json, profile_vector_version, profile_version, last_interaction_at, computed_at, created_at, updated_at)")
        w("VALUES (%s, %.5f, %.5f, %s, %s, 1, %d, @now - INTERVAL %d DAY, @now, @now, @now)"
          % (student, mean, std, j(strong), j(weak), len(history) + 1, min(d for *_, d in history)))
        w("ON DUPLICATE KEY UPDATE average_score = VALUES(average_score), score_stddev = VALUES(score_stddev), "
          "strength_topics_json = VALUES(strength_topics_json), weak_topics_json = VALUES(weak_topics_json), "
          "profile_version = VALUES(profile_version), computed_at = VALUES(computed_at), updated_at = VALUES(updated_at);")
        w("")
    w("-- @student_cold cố ý không có dòng nào trong learner_*: kỹ năng nào cũng cold start (mastery 0.5).")
    sys.stdout.write("\n".join(out) + "\n")


if __name__ == "__main__":
    main()
