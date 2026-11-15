# ai-service

Python AI service (Adaptive Profile-Based RAG) của Người C. Stateless: Spring Boot giữ quyền truy cập, MySQL và phiên bất đồng bộ; service này chỉ nhận ngữ cảnh đã được Spring lọc quyền rồi trả kết quả.

- `lmsrag/`: code runtime (layer1–6, app FastAPI, llm_client, embedder, vectorstore).
- `tests/`: pytest.
- Cấu hình: copy `lmsrag/.env.example` thành `lmsrag/.env` (không commit; không để API key trong repo).

Chạy test: `cd ai-service && pip install -r requirements.txt && python -m pytest tests`

Trạng thái: mới chuyển từ prototype. Còn phải làm: embedder multilingual-e5-base, vector store Qdrant, chế độ LLM giả (`fake`), Dockerfile, API theo OpenAPI.
Benchmark nằm ở `research/benchmark/` (tách khỏi runtime, BR-AI-09).
