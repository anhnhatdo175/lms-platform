# ai-service

Python AI service (Adaptive Profile-Based RAG) của Người C. Stateless: Spring Boot giữ quyền truy cập, MySQL và phiên bất đồng bộ; service này chỉ nhận ngữ cảnh đã được Spring lọc quyền rồi trả kết quả.

- `lmsrag/`: code runtime (layer1–6, app FastAPI, llm_client, embedder, vectorstore).
- `tests/`: pytest.
- Cấu hình: copy `lmsrag/.env.example` thành `lmsrag/.env` (không commit; không để API key trong repo).

## Chạy

```
pip install -r requirements-dev.txt
LLM_BACKEND=fake EMBEDDER=hash QDRANT_URL=:memory: uvicorn lmsrag.api:app --port 8000
```

Với Docker: `docker compose up qdrant ai-service` ở gốc repo (mặc định fake LLM + hash embedder, không cần key). Dùng thật: `AI_LLM_BACKEND=openai`, `AI_LLM_API_KEY`, `AI_EMBEDDER=e5`, `AI_WITH_E5=true` (build lại image; kéo theo torch).

## API nội bộ (Spring gọi, không public)

| Endpoint | Việc |
|---|---|
| `GET /health` | kiểm tra sống |
| `POST /v1/ingest` | nhận text đã trích xuất + `document_id`, `course_id`; chunk, embed, ghi Qdrant (ingest lại sẽ thay chunk cũ) |
| `POST /v1/extract?filename=...` | body là byte của tệp (txt, md, pdf, docx, tối đa 20 MiB); trả `{text, char_count}`; 422 `EXTRACT_FAILED` nếu tệp hỏng hoặc không có chữ (PDF scan cần OCR, chưa hỗ trợ) |
| `DELETE /v1/documents/{id}` | xoá chunk của tài liệu |
| `POST /v1/retrieve` | tìm chunk **trong** `allowed_course_ids` (rỗng thì không trả gì) |
| `POST /v1/generate` | retrieve + sinh nhiệm vụ + validate; lỗi model trả 502/422 để Spring đánh dấu phiên FAILED |

Đặt `AI_SERVICE_TOKEN` để bắt buộc header `X-Internal-Token`. Đây là hợp đồng nội bộ do C đề xuất; OpenAPI hiện chưa định nghĩa API Spring↔AI, cần nhóm xác nhận.

## Test

`python -m pytest tests -q` (cần `requirements-dev.txt`; test cũ của prototype còn cần `faiss-cpu scikit-learn scipy tiktoken`).
5 test cũ của pipeline prototype (`application.py`, `AdaptiveRetriever`) đã lệch API từ bản gốc (ví dụ test double `FakeRecommender` thiếu `profile_vector_for`). Runtime `lmsrag.api` không dùng các lớp này, nên chúng được đánh dấu `xfail` trong `tests/conftest.py` (không strict: sửa xong sẽ báo XPASS).

Trạng thái: còn phải làm bước verify đáp án độc lập (BR-AI-17) cho nhiệm vụ do LLM sinh, và các gói Spring phía sau.
Benchmark nằm ở `research/benchmark/` (tách khỏi runtime, BR-AI-09). Benchmark đã chạy với hằng số cũ (độ khó 0.3/0.6/0.9, EMA 0.25); cần chạy lại.
