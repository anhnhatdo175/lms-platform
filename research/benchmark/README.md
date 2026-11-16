# research/benchmark

Thực nghiệm ASSISTments đánh giá Adaptive RAG (bootstrap CI), **tách khỏi runtime** (BR-AI-09). Backend và `ai-service`
không import thư mục này.

## Chạy

```bash
# từ thư mục gốc repo
pip install -r ai-service/requirements.txt -r research/benchmark/requirements.txt
# đặt skill_builder_data.csv vào research/benchmark/data/ (không commit, đã ignore)
export PYTHONPATH=ai-service:research/benchmark
python -m experiments.assistments --bootstrap 1000 --seed 0                    # dự đoán điểm (EMA)
python -m experiments.assistments --recommender-sweep                          # tuning trọng số xếp hạng
python -m experiments.assistments --next-item                                  # xếp hạng item
python -m experiments.assistments --adaptive-rag                               # Semantic vs Adaptive
```

Kiểm tra đường đánh giá không cần dataset: `cd ai-service && python -m pytest tests/test_benchmark_and_demo.py`.

## Baseline

| Baseline | Spec yêu cầu tối thiểu | Có trong mã |
|---|---|---|
| Random | có | có |
| Traditional RAG (PureSemantic) | có | có |
| Pure CB-ZPD | có | có |
| MostPopular, PureCF/ItemCF | không | có |
| Adaptive RAG (hybrid) | hệ thống đề xuất | có |

## Hằng số: runtime và benchmark khác nhau có chủ đích

| Tham số | Runtime (Spec) | Kết quả đã lưu trong `results/` |
|---|---|---|
| EMA α | 0.3 (BR-AI-14) | 0.1, chọn theo log-loss trên validation |
| Mức độ khó EASY/MEDIUM/HARD | 0.25 / 0.5 / 0.75 | 0.3 / 0.6 / 0.9 |

**Các file trong `results/` chạy bằng hằng số cũ của prototype và chưa được chạy lại với cấu hình đã chốt.** Khi viết báo cáo
phải ghi rõ điều này, hoặc chạy lại bằng dataset gốc rồi thay file. Việc chạy lại cần `skill_builder_data.csv` nên chưa làm trong
repo này.

## Hạn chế cần nêu trong báo cáo

- "Relevance" là proxy suy ra từ tương tác giữ lại, không phải nhãn do người đánh.
- Một phần truy vấn bị bỏ ở mỗi split (khoảng 3.100 truy vấn/tập val và test) vì thiếu đủ nhóm độ khó.
- ASSISTments không có nhãn EASY/MEDIUM/HARD: độ khó được bin theo tỉ lệ đúng của item.
- Ma trận học viên-item biểu diễn "chưa làm" bằng khuyết (có mask), không phải 0 (BR-AI-04).
- Chỉ một dataset (ASSISTments); Junyi và EdNet chưa thử.
