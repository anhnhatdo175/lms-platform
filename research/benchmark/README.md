# research/benchmark

Thực nghiệm ASSISTments (đánh giá Adaptive RAG, bootstrap CI), tách khỏi runtime.

- `experiments/assistments.py`: chạy cần `pip install -e`/`PYTHONPATH=../../ai-service` để import `lmsrag`.
- `results/`: kết quả JSON nhỏ đã chạy.
- `data/`: đặt `skill_builder_data.csv` (không commit, đã ignore).
Lưu ý: relevance là proxy; một phần query bị bỏ qua ở mỗi split.
