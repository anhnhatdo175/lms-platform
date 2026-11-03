# LMS — Consistency & Decision Register
**Bản làm việc:** 0.2 (OpenAPI v1.5 đã làm)  
**Ngày:** 30/10/2026  
**Đồng bộ với:** Business Spec v3.2, ERD v2.2. OpenAPI v1.5 (đã cập nhật, 137 operation: 115 MUST, 22 SHOULD).

> Bản 0.2 chốt hướng cho từng quyết định của v0.1. Cột "Điều chỉnh" ghi rõ chỗ khác với đề xuất ban đầu và lý do. Spec và ERD đã sửa theo; OpenAPI v1.5 còn chờ.

## 1. Nguyên tắc nguồn sự thật

| Chủ đề | Nguồn chuẩn |
|---|---|
| Hành vi, business rules, state machine | Business Spec |
| Bảng, cột, kiểu, FK, unique/index | ERD |
| HTTP method/path, request/response, auth, mã lỗi | OpenAPI (chỉ là hợp đồng API, không tự đặt nghiệp vụ) |
| Điều chưa được ghi ở đâu | Decision Register |

## 2. Quyết định đã chốt

| ID | Mức | Trạng thái | Quyết định cuối | Điều chỉnh so với v0.1 | Spec v3.2 | ERD v2.2 | OpenAPI |
|---|---|---|---|---|---|---|---|
| DEC-001 | P1 | CLOSED | Student không gửi độ khó; server chọn theo ZPD (`M+δ` ánh xạ EASY/MEDIUM/HARD, kẹp trong khoảng Teacher) | Thêm công thức chọn độ khó để hành vi kiểm thử được | 20A.5, AC-AI-11 | bỏ `requested_difficulty` (17.9) | cần bỏ `targetDifficulty` |
| DEC-002 | P1 | CLOSED | `LMS_FILE` ⇔ `fileResourceId`; `PASTED_TEXT` ⇔ `textContent`; kiểm tra quyền file | ERD v2.1 không có chỗ lưu văn bản dán → thêm `source_text` + CHECK | 20A.7, AC-AI-16 | 17.4 | cần `oneOf` + discriminator |
| DEC-003 | P1 | CLOSED | Hint idempotent bằng `Idempotency-Key` | **Khác:** bảng riêng `adaptive_task_hint_events`, không dùng `learning_interactions.item_type=AI_HINT`, vì sẽ làm nhiễu ma trận `R`, EMA và hồ sơ | 20A.15, BR-AI-15, AC-AI-14 | 17.12 (bảng mới, 47 bảng) | thêm header + `duplicate` |
| DEC-004 | P1 | CLOSED | 4 trạng thái, lease + fencing, tự retry ≤3 (lỗi tạm thời), retry thủ công ≤3, ghi task nguyên tử | Thêm giới hạn retry thủ công và ghi task một lần ở cuối | 20A.14, AC-AI-15 | 17.9 (cột lease/retry), Mục 14 | thêm `AI_RETRY_LIMIT_REACHED`, `AI_HINT_NOT_AVAILABLE` |
| DEC-005 | P1 | CLOSED | Một điều kiện cấp Enrollment duy nhất + bảng quyết định đối soát 8 dòng; `UNIQUE(order_item_id)` | **Khác:** cho thanh toán trong grace 15 phút sau hạn (webhook trễ không nên thành đối soát); tách `review_reason_code`; khóa dedup theo giao dịch | 10.6, 11.4–11.6, 11.9, AC-PAY-05/06 | 7.1, 7.2, 8.1, Mục 14 | cập nhật enum lý do |
| DEC-006 | P1 | CLOSED | Chỉ `DRAFT → PENDING_REVIEW`; `REJECTED → DRAFT` bằng **reopen** | **Khác:** reopen là thao tác riêng, không chuyển ngầm khi sửa (curriculum có nhiều endpoint sửa) | 8.2, 8.3, 7.3, AC-COURSE-05 | không đổi (`course_review_requests`) | cần `POST .../reopen` |
| DEC-007 | P1 | CLOSED | ID int64 ≥ 1, mã nghiệp vụ là string | — | 19.3 | đã khớp | rà lại toàn bộ |
| DEC-008 | P1 | CLOSED | `{data}`, `{data, meta}`, `ErrorResponse` luôn có `traceId` | `traceId` bắt buộc | 19.3 | — | rà lại |
| DEC-009 | P1 | CLOSED | Bảng ownership theo tài nguyên; 401/403/404; negative test bắt buộc | Teacher chỉ thấy kỹ năng AI thuộc Course mình | 7.4 | — | `x-ownership` mô tả |
| DEC-010 | P1 | CLOSED | `expiresAt` là sự thật, không ngưỡng nới; submit không mang đáp án | — | 14.5, 14.8, AC-EXAM-03 | đã khớp | — |
| DEC-011 | P2 | CLOSED | Schema dùng chung `KnowledgeDocumentSourceType` | — | — | — | v1.5 |
| DEC-012 | P2 | CLOSED | Phân tầng MUST/SHOULD/LATER, thứ tự cắt đánh số duy nhất | Hạng mục MUST không nằm trong danh sách cắt | 5.2, 23.1, 24 | Mục 2 (tầng bảng) | `x-scope` |
| DEC-013 | P2 | CLOSED | Query ID là integer | — | 19.3 | — | v1.5 |
| DEC-014 | P2 | CLOSED | Retry session ≠ attempt; mỗi task một lần nộp | — | 20A.14 | 17.11 | kiểm tra mã lỗi |

## 3. Điểm cần nhóm lưu ý

**Hiệu chỉnh sau tự rà soát (30/10/2026):** (1) bổ sung máy trạng thái Payment, vì bảng quyết định cần `CANCELLED → REVIEW_REQUIRED`; (2) sửa Mục 11.6 spec còn liệt kê "Order hết hạn" như unmatched, mâu thuẫn dòng 4; (3) Order có Payment `REVIEW_REQUIRED` không tự hết hạn; (4) nhiệm vụ `BANK` không có gợi ý; (5) vòng FK `payments` ↔ `payment_webhook_logs` cần ALTER khi migration.

- **Benchmark** vẫn là MUST ở dạng tối thiểu (Random, Traditional RAG, Pure CB-ZPD), tách khỏi runtime sản phẩm.
- **Số bảng** nay là 47 (37 LMS + 10 AI). Bảng SHOULD vẫn có migration để schema ổn định; cắt giảm chỉ bỏ logic và UI.
- **Sửa Course đã PUBLISHED** có hiệu lực ngay, không duyệt lại. **Nhóm đã xác nhận chấp nhận** (30/10/2026); có AuditLog.
- **Cart nhiều Course giữ là MUST**, không có phương án thay bằng Buy Now. Nhóm cam kết toàn bộ MUST; cắt giảm chỉ áp dụng cho SHOULD (Spec 23.1).
- Spec v3.1 có vài lỗi nội bộ đã sửa ở v3.2: dòng autosave lặp ở 14.8; khóa dedup webhook lệch ERD; ràng buộc UNIQUE cũ ở Mục 18.

## 4. Việc đã làm ở OpenAPI v1.5 (kèm `reopen`: 137 operation)

1. `POST /teacher/courses/{id}/reopen`; `submit-review` chỉ từ `DRAFT`.
2. Bỏ `targetDifficulty` khỏi `CreateAdaptiveTaskSessionRequest`.
3. Knowledge document: `oneOf` theo `sourceType`; schema `KnowledgeDocumentSourceType` dùng chung; query `courseId`/`lessonId` là integer.
4. Hint: header `Idempotency-Key`, response có `duplicate`; mã lỗi `AI_HINTS_EXHAUSTED`, `AI_SOCRATIC_DISABLED`, `AI_RETRY_LIMIT_REACHED`.
5. Enum lý do đối soát: `review_reason_code` (3 giá trị) và `unmatched_reason` (4 giá trị).
6. `x-scope: MUST|SHOULD|LATER` cho từng operation.
7. Mô tả ownership và tầng quyền cho từng nhóm endpoint.

## 5. Tiêu chí bắt đầu implementation song song

- Không còn quyết định P1 ở trạng thái OPEN (đã đạt; toàn bộ DEC-001..014 đã CLOSED).
- OpenAPI v1.5 qua validator và khớp Spec v3.2/ERD v2.2.
- Mỗi endpoint MUST có security, ownership rule và negative test.

## 6. Ánh xạ trường API ↔ ERD (rà soát cấp trường, 03/11/2026)

Đã so toàn bộ 56 request schema của OpenAPI v1.5 với cột ERD v2.2. Các trường dưới đây khác tên hoặc không map 1–1; còn lại khớp theo quy tắc camelCase ↔ snake_case. Đây là bảng tham chiếu khi viết DTO/mapper.

| API (camelCase) | ERD | Ghi chú |
|---|---|---|
| `displayName` | `users.full_name` | Khác tên |
| `avatarResourceId` | `users.avatar_file_id` | Đã sửa ERD (trước là `avatar_url`) |
| `thumbnailResourceId` | `courses.thumbnail_file_id` | Khác tên |
| `videoResourceId` | `lessons.video_file_id` | Khác tên |
| `positionSeconds` (heartbeat) | `lesson_progress.watched_seconds` | Server tính mốc xem lớn nhất, không ghi đè lùi |
| `paymentMethod` | `payments.method` | |
| `textContent` | `ai_knowledge_documents.source_text` | |
| `topicTags` / `targetTopics` | `topic_tags_json` / `requested_topics_json` | |
| `taskTypes` | `adaptive_task_sessions.requested_task_types_json` | Đã sửa ERD (trước là một giá trị `requested_task_type`) |
| `allowedSkills` / `allowedTaskTypes` | `allowed_skills_json` / `allowed_task_types_json` | |
| `answer` (nộp nhiệm vụ AI) | `adaptive_task_attempts.answer_json` | |
| `selectedOptionIds` | `attempt_answer_options` | Bảng nối |
| `options` (Question) | `question_options` | Bảng con |
| `courseIds` (Coupon) | `coupon_courses` | Bảng nối |
| `roles` / `teachers` | `user_roles` / `course_teachers` | Bảng nối |
| `sections` (reorder) | `course_sections.position`, `lessons.position` | Cập nhật hàng loạt trong một transaction |
| `password`, `newPassword`, `currentPassword`, `token` | `users.password_hash`, `password_reset_tokens` | Không lưu bản rõ |
| `fileName`, `contentType`, `sizeBytes`, `purpose`, `uploadId` | `file_resources` | Luồng upload |
| `targetExamId` (duyệt nhiệm vụ AI) | `exam_questions` | Tham số hành động: thêm Question vừa tạo vào Exam, không phải cột |
| `paymentMethod`, `grading_method`, `storage_provider` | — | `grading_method` và `storage_provider` chưa lộ ra API (chủ ý) |

## 7. Bước bắt buộc ở sprint đầu: contract test

Phép đối chiếu trên chỉ bắt lệch tên và enum. Để không drift khi hai người code song song:

1. Sinh DTO/client từ OpenAPI v1.5 (openapi-generator hoặc tương đương); không viết tay DTO cho API đã có trong contract.
2. Backend có contract test (ví dụ Spring Cloud Contract hoặc test so response thật với schema OpenAPI) chạy trong CI; response sai schema thì build đỏ.
3. Frontend gọi API qua client sinh từ cùng file OpenAPI; mock server dựng từ file này khi backend chưa xong.
4. Mọi thay đổi contract phải kèm cập nhật Register, tăng phiên bản OpenAPI và chạy lại validator.
5. Cuối mỗi sprint chạy lại phép đối chiếu cấp trường ở Mục 6 (script có thể đưa vào CI).
