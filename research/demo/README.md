# Dữ liệu demo AI

`seed_demo_ai.sql` được sinh bởi `generate_demo_sql.py` (deterministic, không dữ liệu thật):

```bash
python research/demo/generate_demo_sql.py > research/demo/seed_demo_ai.sql
```

**Cách dùng:** DB đã migrate và đã có 1 Teacher, 3 Student, 1 Course, 1 Lesson (phần A). Sửa 6 biến `@...` ở đầu file thành id thật,
rồi chạy file bằng MySQL client. Chạy lại nhiều lần không tạo trùng (`NOT EXISTS`, `INSERT IGNORE`, `ON DUPLICATE KEY UPDATE`).

Nội dung: cấu hình luyện tập của Teacher, 3 tài liệu (Phân số, Số thập phân, Phần trăm; trạng thái PENDING nên worker
ingestion sẽ lập chỉ mục), 9 câu hỏi có `skill_code` kèm đáp án, và hồ sơ cho 3 học viên:

| Học viên | Tình huống | Kỳ vọng khi demo |
|---|---|---|
| `@student_strong` | 12 tương tác, đúng gần hết | mastery cao, bài sinh ra khó hơn (HARD) |
| `@student_weak` | 10 tương tác, sai nhiều ở Phân số | Phân số mastery ~0.2 → bài EASY; Phần trăm chỉ 1 bằng chứng → cold start |
| `@student_cold` | không có dòng nào | mọi kỹ năng cold start (0.5), lý do nêu "chưa đủ dữ liệu" |

Giá trị mastery trong script được tính bằng đúng công thức backend (EMA α=0.3, cold start 0.5, phạt gợi ý 0.3), có test đối chiếu
trong `ai-service/tests/test_benchmark_and_demo.py`.
