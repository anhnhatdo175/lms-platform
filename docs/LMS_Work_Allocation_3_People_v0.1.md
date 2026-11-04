# LMS — Phân công công việc cho 3 người
**Bản làm việc:** 0.1  
**Ngày:** 04/11/2026  
**Đồng bộ với:** Business Spec v3.2 (Mục 5.2, 21), ERD v2.2, OpenAPI v1.5 (137 operation), Common Modules v0.1  
**Quy ước tên:** Người **A** và **B** nặng nghiệp vụ phần mềm (LMS); Người **C** phụ trách AI và một phần nhỏ phần mềm. Hãy tự gán tên thật cho A và B.

> Số bảng và số operation dưới đây tính từ ERD và OpenAPI. Không có ước lượng ngày công; Spec và tài liệu hiện có không đủ cơ sở để ước lượng, nên bảng dưới đây chỉ so độ lớn và độ khó tương đối.

## 1. Nguyên tắc chia

1. **Chia theo module dọc, không chia theo tầng.** Mỗi người sở hữu một lát cắt trọn vẹn (bảng → service → API → giao diện), để không ai chờ người kia viết nốt tầng.
2. **Chia theo độ khó, không chỉ theo số lượng.** A nhiều operation nhưng phần lớn là CRUD; B ít hơn nhưng có Payment và Exam là hai chỗ khó nhất của LMS; C ít operation nhất nhưng pipeline AI nặng nhất về kỹ thuật.
3. **C bắt đầu độc lập từ sớm.** Phần AI cốt lõi (EMA, ZPD, ingestion, retrieval) viết được trên dữ liệu mẫu trước khi LMS xong, nên C không bị chặn.
4. **Điểm giao giữa người với người đi qua interface đã chốt** (Mục 4), có bản giả (stub) từ Sprint 2 để không ai chờ ai.
5. **Mỗi bảng, mỗi operation có đúng một chủ.** Sửa thứ của người khác thì qua pull request và báo trước.

## 2. Bảng tổng quan

| | **A: LMS nền tảng và nội dung** | **B: LMS thương mại và thi** | **C: AI và tiện ích** |
|---|---|---|---|
| Module | Identity, User, Course authoring, Workflow duyệt, Catalog, Learning/Progress, Audit, Admin quản trị | Cart, Coupon, Order, Payment, Webhook, Đối soát, Enrollment, Exam (builder, Attempt, chấm) | AI (toàn bộ), Question bank, Upload file, Lesson resource |
| Bảng | 16 | 17 | 14 (10 AI + 2 file + 2 Question) |
| Operation | 57 (49 MUST, 8 SHOULD) | 40 (33 MUST, 7 SHOULD) | 40 (33 MUST, 7 SHOULD) |
| Điểm khó nhất | Ownership authorization, state machine Course, prerequisite và drip | Idempotency thanh toán, concurrency webhook, Attempt snapshot và chấm | Ingestion, retrieval có kiểm soát quyền, ZPD, sinh và kiểm định nhiệm vụ, retry/lease |
| Giao diện | Auth, quản lý Course, catalog, trình phát bài học, trang Admin | Giỏ hàng, thanh toán/QR, đơn hàng, My Courses, làm bài thi, đối soát | Cấu hình AI của Teacher, quản lý tài liệu AI, luyện tập AI của Student, duyệt nhiệm vụ |
| Còn thêm | Khung giao diện chung (layout, router, auth shell), module Common `auth`/`user`/`course`/response-error | Module Common `enrollment` | Dữ liệu demo AI, benchmark (track nghiên cứu), docker AI |

## 2b. Đã đều chưa?

Số bảng 16/17/14 và số operation 57/40/40. Không đều tuyệt đối, và chủ ý như vậy:

- **A** nhiều operation nhất nhưng độ khó thấp nhất (đa số CRUD). Bù lại A giữ khung giao diện chung và các module Common nền tảng.
- **B** giữ phần khó nhất của LMS: tiền (Payment, webhook, đối soát) và thi (Attempt, chấm, hết giờ).
- **C** giữ phần khó nhất về kỹ thuật (AI) cộng thêm ngân hàng câu hỏi và upload.

Bản v0.1 đầu tiên chia 57/45/35; đã chuyển ngân hàng câu hỏi (2 bảng, 5 operation) từ B sang C để B bớt tải và C có thêm một phần phần mềm LMS đúng chỗ AI cần. Chưa có ước lượng ngày công, nên "đều" ở đây chỉ là tương đối. Sau Sprint 2, so tiến độ thực tế rồi chuyển việc nếu một người trễ rõ rệt (ứng viên chuyển: SHOULD của A sang B/C, hoặc Admin Order/Coupon của B sang A).

## 3. Chi tiết từng người

### Người A — LMS nền tảng và nội dung

**Bảng (16):** `users`, `roles`, `user_roles`, `refresh_tokens`, `password_reset_tokens`, `course_categories`, `courses`, `course_teachers`, `course_review_requests`, `course_sections`, `lessons`, `lesson_prerequisites`, `lesson_progress`, `progress_override_logs`, `course_completions`, `audit_logs`.

**API:** `/auth/*`, `/me*`, `/admin/users*`, `/admin/categories*`, `/teacher/courses*` (trừ questions, ai-config), sections, lessons, prerequisite, `/admin/courses*`, `/courses*`, `/categories`, `/learning/lessons/*`, `/learning/courses/*/progress`, progress override, danh sách học viên và tiến độ cho Teacher, `/admin/audit-logs`, dashboard, export CSV, `/health`.

**Chủ sở hữu Common:** response/error format, `auth`, `user`, `course` (Common Modules v0.1, Mục 3–6).

**Lưu ý:** A có nhiều operation nhất nhưng đa số là CRUD đơn giản. Hai chỗ cần cẩn thận: máy trạng thái Course và quy tắc sở hữu (Teacher chỉ sửa Course được phân công), vì mọi người khác phụ thuộc vào đó.

### Người B — LMS thương mại và thi

**Bảng (17):** `carts`, `cart_items`, `coupons`, `coupon_courses`, `orders`, `order_items`, `coupon_redemptions`, `payments`, `payment_webhook_logs`, `payment_reconciliation_actions`, `enrollments`, `exams`, `exam_questions`, `exam_attempts`, `attempt_questions`, `attempt_answers`, `attempt_answer_options`.

**API:** `/cart*`, `/orders*`, `/webhooks/payments/sepay`, `/admin/payment-webhooks`, `/admin/reconciliation*`, `/admin/coupons*`, `/admin/orders`, `/my-courses*`, `/admin/enrollments*`, exam builder, `/exams/*`, `/exam-attempts/*`, thống kê Exam.

**Chủ sở hữu Common:** `enrollment` (gồm `EnrollmentAccessService`).

**Lưu ý:** B giữ hai phần khó nhất của LMS: luồng tiền (Spec 10–11) và thi (Spec 14). Hợp đồng Payment → Enrollment ở Spec 11.9 là điều kiện duy nhất cấp quyền học. Phần thi cũng là nguồn tín hiệu cho AI, nên B và C làm việc sát nhau ở Sprint 6.

### Người C — AI và tiện ích

**Bảng (14):** `file_resources`, `lesson_resources`, `questions`, `question_options`, `adaptive_practice_configs`, `ai_knowledge_documents`, `ai_document_chunks`, `learner_profiles`, `learner_skill_states`, `learning_interactions`, `adaptive_task_sessions`, `adaptive_generated_tasks`, `adaptive_task_attempts`, `adaptive_task_hint_events`.

**API:** `/teacher/files/*`, `/files/*`, lesson resource attach/detach, question bank (`/teacher/courses/{id}/questions`, `/teacher/questions/*`), toàn bộ `/teacher/ai/*`, `/student/ai/*`, `/admin/ai/*`, ai-config của Course và Lesson.

**Phần mềm LMS của C:** (1) upload file, cả A và AI cùng dùng, phải xong sớm (Sprint 2); (2) ngân hàng câu hỏi (`questions`, `question_options`). C là bên dùng nhiều nhất: nhiệm vụ AI có thể lấy từ ngân hàng, nhiệm vụ được duyệt thành Question chính thức, `skill_code` trên Question là đầu vào của hồ sơ. Exam builder của B chỉ đọc Question qua `QuestionQueryService`, và C phải xong ngân hàng câu hỏi ở Sprint 5.

**Track nghiên cứu:** benchmark với baseline Random, Traditional RAG và Pure CB-ZPD (Spec 20A.10), tách khỏi runtime sản phẩm, và dữ liệu demo AI.

**Lưu ý:** phần Should Have của AI (hint Socratic, `SHORT_ANSWER`, rerank, CF + Hybrid, duyệt nhiệm vụ) nằm đầu danh sách cắt (Spec 23.1). C nên xong luồng end-to-end tối thiểu trước khi làm các phần này.

## 4. Điểm giao giữa người với người

| # | Bên cung cấp | Bên dùng | Interface | Từ sprint | Bản giả (stub) |
|---|---|---|---|---|---|
| 1 | A | B, C | `CurrentUser`, `AccessPolicy.canManageCourse`, response/error, `BusinessException` | S1 | — (làm đầu tiên) |
| 2 | A | B, C | `CourseQueryService` (`isPurchasable`, `getSnapshotForOrder`, lấy Course/Lesson) | S2 | Dữ liệu cố định trong test |
| 3 | C | A | `FileResourceService` (upload, tải, đính kèm Lesson) | S2 | A dùng URL giả cho thumbnail/video |
| 4 | B | A, C | `EnrollmentAccessService.requireActiveAccess` | S2 (stub), S5 (thật) | Luôn cho qua trong profile dev |
| 5 | B | A | `EnrollmentGrantService` (cấp Enrollment theo OrderItem) | S5 | — |
| 6 | C | B | `LearningInteractionRecorder.recordExamInteractions(attemptId)` | S6 | B gọi hàm rỗng |
| 7 | C | B | `QuestionQueryService` (lấy Question và Option cho Exam builder và snapshot Attempt, kèm `skill_code`, `difficulty`) | S5 | B seed Question bằng tay |

**Quy tắc tại điểm giao 6:** khi nộp bài thi, B chấm và lưu xong mới gọi C; nếu phần AI lỗi thì **không** được làm hỏng việc chấm. C xử lý lại bằng job dựa trên `processed_at IS NULL` (ERD Mục 14, BR-AI-11).

## 5. Lịch theo sprint

Giữ nguyên 8 sprint của Spec Mục 21. Điểm khác: C làm sớm hơn Spec vì AI có thể phát triển trên dữ liệu mẫu, nên các việc của Spec Sprint 5–7 được kéo về trước.

| Sprint | A | B | C |
|---|---|---|---|
| **S1** Foundation | Khởi tạo Spring Boot/React/MySQL, Flyway, Common response/error, auth, user, khung giao diện | Baseline migration cho bảng của B; tìm hiểu SePay (API key, QR); thiết kế idempotency và decision table | Docker Compose AI (vector index, dịch vụ embedding); thư viện thuần EMA, ZPD, cold start + unit test; chuẩn bị dữ liệu mẫu |
| **S2** Course authoring | Category, Course, Section, Lesson, ownership, reorder, UI quản lý Course | Bảng `enrollments` + `EnrollmentAccessService` bản giả; thiết kế Cart/Order | **Upload file và Lesson resource (xong sớm)**; prototype ingestion trên file mẫu |
| **S3** Workflow và Catalog | Duyệt/từ chối/rút/mở lại/ẩn/lưu trữ, catalog, tìm kiếm, preview | Cart, coupon-preview | `ai_knowledge_documents`, pipeline parse → chunk → embed → index, trạng thái xử lý, lease và retry |
| **S4** Order và Payment | Prerequisite, drip, API học bài (khung) | Coupon, checkout idempotent, Order snapshot, Payment, QR, webhook log, idempotency | `adaptive_practice_configs` và UI cấu hình của Teacher; retrieval có kiểm soát quyền |
| **S5** Enrollment và Learning | Progress, heartbeat, hoàn thành, prerequisite, drip, Course completion, trình phát bài học | **Payment → Enrollment** (Spec 11.9), My Courses, `EnrollmentAccessService` thật, Admin grant/revoke | UI quản lý tài liệu AI; kiểm thử ingestion và quyền tài liệu; **ngân hàng câu hỏi (`QuestionQueryService`, điểm giao 7)** |
| **S6** Exam và hồ sơ | Tiến độ cho Teacher, UI Course completion, Admin quản trị | Exam builder, Attempt snapshot, autosave, submit, chấm, thống kê | `learning_interactions` từ ExamAttempt (**điểm giao 6**), `learner_profiles`, `learner_skill_states` (EMA), cập nhật idempotent |
| **S7** Admin và sinh nhiệm vụ | Dashboard, export CSV, Audit UI (phần SHOULD nếu còn chỗ) | Đối soát: hàng đợi, approve/reject; Admin Order/Coupon | Recommender ZPD, sinh nhiệm vụ bằng LLM, kiểm định, phiên bất đồng bộ, nộp/chấm, UI Student, UI Teacher duyệt nhiệm vụ thành Question |
| **S8** Hardening | Authorization test toàn hệ thống, E2E luồng học | Concurrency test webhook, E2E luồng mua → học → thi | Kiểm thử AI (quyền, schema LLM, không lộ đáp án, cold start), benchmark, dữ liệu demo |

Tất cả cùng làm ở S8: integration test, fix bug, deploy HTTPS, hoàn thiện tài liệu.

## 6. Review chéo

Mỗi pull request cần ít nhất một người duyệt không phải tác giả. Gán người duyệt theo nơi **rủi ro cao nhất**, không chỉ theo thứ tự:

| Phần | Người duyệt | Vì sao |
|---|---|---|
| Common (`auth`, `user`, `course`, response/error) | B và C | Cả hai phụ thuộc vào đó |
| Common `enrollment` | A và C | Cổng quyền học dùng ở Learning và AI |
| Thanh toán, webhook, đối soát | **A** | Mắt mới cho luồng tiền |
| Kiểm tra quyền tài liệu trước retrieval, ownership hồ sơ AI (BR-AI-01) | **A** | Lỗi phân quyền là lỗi nặng nhất của AI |
| Chấm bài và Attempt snapshot | C | C dùng kết quả này làm tín hiệu |
| Phần LMS nhỏ của C (upload) | B | |
| Migration | Cả hai người còn lại | Migration sai khó sửa |

## 7. Rủi ro và cách xử lý

| Rủi ro | Cách xử lý |
|---|---|
| A có nhiều operation nhất, dễ trễ | Chia nhỏ theo sprint; phần SHOULD của A (dashboard nâng cao, export, override tiến độ) cắt trước. B hoặc C hỗ trợ giao diện khi có chỗ trống |
| B giữ hai phần khó nhất (Payment, Exam) | Payment làm trước ở S4–S5 và có A review; Exam ở S6, tách riêng khỏi Payment để không dồn |
| C bị chặn vì chờ LMS | Dùng bản giả ở Mục 4 và dữ liệu mẫu; luồng AI end-to-end chạy trên dữ liệu cố định trước khi tích hợp |
| Phần AI vượt thời gian (LLM, retrieval khó chỉnh) | Giữ baseline đơn giản (EMA, ZPD, dense retrieval); cắt theo thứ tự ở Spec 23.1; benchmark là track nghiên cứu, không chặn sản phẩm |
| Một người nghỉ hoặc quá tải | Mỗi module có người duyệt ở Mục 6, người đó đủ hiểu để tiếp quản; ưu tiên chốt tài liệu hơn lời nói |
| Drift giữa các phần | Contract test và client sinh từ OpenAPI (Register Mục 7); mọi đổi contract đi qua Register |
| Chi phí và hạn mức LLM | Đặt giới hạn số nhiệm vụ mỗi phiên và số lần retry (đã có ở Spec 20A.14); dùng dữ liệu nhỏ khi phát triển |

## 8. Việc cả ba làm trước khi tách ra (Sprint 1, ngày đầu)

1. Cùng chốt các mục đề xuất còn mở ở Common Modules Mục 8 (thời hạn token, xoay vòng token, cách reorder...).
2. Dựng repo, cấu trúc package theo module, CI chạy validator OpenAPI.
3. Tạo baseline migration cho bảng dùng chung (`users`, `roles`, `user_roles`, `file_resources`, `courses`...).
4. Thống nhất quy tắc nhánh: mỗi người một tiền tố (`a/`, `b/`, `c/`), merge qua pull request, **không sửa thư mục Common khi chưa báo**.
5. Chốt người duyệt cho từng module theo Mục 6.

## 9. Tài liệu cần cập nhật theo bản phân công này

- Spec Mục 21 hiện viết cho hai người ("một thành viên nghiêng về LMS core, một thành viên nghiêng về AI track"). Cần sửa thành ba người nếu muốn Spec phản ánh thực tế.
- Decision Register: ghi bản phân công này là một quyết định (DEC-015) khi nhóm xác nhận.
