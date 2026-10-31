# LMS — Consistency & Decision Register
**Bản làm việc:** 0.2  
**Ngày:** 30/10/2026  
**Đồng bộ với:** Business Spec v3.2, ERD v2.2. OpenAPI v1.4 **chưa** cập nhật (xem Mục 4).

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
| DEC-011 | P2 | OPEN (OpenAPI) | Schema dùng chung `KnowledgeDocumentSourceType` | — | — | — | v1.5 |
| DEC-012 | P2 | CLOSED | Phân tầng MUST/SHOULD/LATER, thứ tự cắt đánh số duy nhất | Hạng mục MUST không nằm trong danh sách cắt | 5.2, 23.1, 24 | Mục 2 (tầng bảng) | `x-scope` |
| DEC-013 | P2 | OPEN (OpenAPI) | Query ID là integer | — | 19.3 | — | v1.5 |
| DEC-014 | P2 | CLOSED | Retry session ≠ attempt; mỗi task một lần nộp | — | 20A.14 | 17.11 | kiểm tra mã lỗi |

## 3. Điểm cần nhóm lưu ý

**Hiệu chỉnh sau tự rà soát (30/10/2026):** (1) bổ sung máy trạng thái Payment, vì bảng quyết định cần `CANCELLED → REVIEW_REQUIRED`; (2) sửa Mục 11.6 spec còn liệt kê "Order hết hạn" như unmatched, mâu thuẫn dòng 4; (3) Order có Payment `REVIEW_REQUIRED` không tự hết hạn; (4) nhiệm vụ `BANK` không có gợi ý; (5) vòng FK `payments` ↔ `payment_webhook_logs` cần ALTER khi migration.

- **Benchmark** vẫn là MUST ở dạng tối thiểu (Random, Traditional RAG, Pure CB-ZPD), tách khỏi runtime sản phẩm.
- **Số bảng** nay là 47 (37 LMS + 10 AI). Bảng SHOULD vẫn có migration để schema ổn định; cắt giảm chỉ bỏ logic và UI.
- **Sửa Course đã PUBLISHED** có hiệu lực ngay, không duyệt lại. **Nhóm đã xác nhận chấp nhận** (30/10/2026); có AuditLog.
- **Cart nhiều Course giữ là MUST**, không có phương án thay bằng Buy Now. Nhóm cam kết toàn bộ MUST; cắt giảm chỉ áp dụng cho SHOULD (Spec 23.1).
- Spec v3.1 có vài lỗi nội bộ đã sửa ở v3.2: dòng autosave lặp ở 14.8; khóa dedup webhook lệch ERD; ràng buộc UNIQUE cũ ở Mục 18.

## 4. Việc còn lại cho OpenAPI v1.5

1. `POST /teacher/courses/{id}/reopen`; `submit-review` chỉ từ `DRAFT`.
2. Bỏ `targetDifficulty` khỏi `CreateAdaptiveTaskSessionRequest`.
3. Knowledge document: `oneOf` theo `sourceType`; schema `KnowledgeDocumentSourceType` dùng chung; query `courseId`/`lessonId` là integer.
4. Hint: header `Idempotency-Key`, response có `duplicate`; mã lỗi `AI_HINTS_EXHAUSTED`, `AI_SOCRATIC_DISABLED`, `AI_RETRY_LIMIT_REACHED`.
5. Enum lý do đối soát: `review_reason_code` (3 giá trị) và `unmatched_reason` (4 giá trị).
6. `x-scope: MUST|SHOULD|LATER` cho từng operation.
7. Mô tả ownership và tầng quyền cho từng nhóm endpoint.

## 5. Tiêu chí bắt đầu implementation song song

- Không còn quyết định P1 ở trạng thái OPEN (hiện: đã đạt, các mục OPEN đều P2 và thuộc OpenAPI).
- OpenAPI v1.5 qua validator và khớp Spec v3.2/ERD v2.2.
- Mỗi endpoint MUST có security, ownership rule và negative test.
