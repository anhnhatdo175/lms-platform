# ĐẶC TẢ NGHIỆP VỤ MVP NÂNG CAO  
## Hệ thống học trực tuyến, kinh doanh khóa học và cá nhân hóa nhiệm vụ học tập bằng Adaptive Profile-Based RAG

**Phiên bản:** 3.2  
**Ngày cập nhật:** 30/10/2026  
**Đồng bộ với:** Decision Register v0.2  
**Thời gian phát triển:** 3 tháng  
**Nguồn lực:** 2 lập trình viên, có AI hỗ trợ  
**Kiến trúc dự kiến:** Modular Monolith  
**Backend:** Java Spring Boot  
**Frontend:** React + Vite + TypeScript  
**Database:** MySQL  

---

# 0A. Thay đổi so với v3.1

Bản 3.2 áp dụng Decision Register v0.1. Với mỗi quyết định, nhóm đã chọn hướng tốt nhất; **một số khác với đề xuất ban đầu** và được đánh dấu "Điều chỉnh".

## 0A.1. Quyết định

| ID | Chủ đề | Quyết định cuối | So với đề xuất ban đầu |
|---|---|---|---|
| DEC-001 | Độ khó phiên AI | Student **không** gửi độ khó. Server tính độ khó từng nhiệm vụ từ mức thành thạo (ZPD) và kẹp trong khoảng Teacher cho phép (20A.5) | Giữ nguyên; bổ sung công thức chọn độ khó |
| DEC-002 | Nguồn tài liệu AI | `LMS_FILE` ⇔ `fileResourceId`; `PASTED_TEXT` ⇔ `textContent`; loại trừ lẫn nhau. Có quy tắc phạm vi file (20A.7) | Điều chỉnh: thêm cột `source_text` và ràng buộc CHECK ở ERD, vì ERD cũ không có chỗ lưu văn bản dán |
| DEC-003 | Idempotency của hint | Header `Idempotency-Key`; cùng key trả lại đúng gợi ý cũ, không tăng lượt dùng (20A.15) | **Điều chỉnh:** dùng bảng riêng `adaptive_task_hint_events`, **không** ghi vào `learning_interactions`, để không làm nhiễu ma trận `R` và hồ sơ người học |
| DEC-004 | Retry phiên AI | Giữ 4 trạng thái; lease + fencing; tự retry tối đa 3 lần với lỗi tạm thời; retry thủ công tối đa 3 lần; task ghi nguyên tử (20A.14) | Điều chỉnh: thêm giới hạn retry thủ công, ghi task một lần ở cuối để lease chuyển worker không sinh trùng |
| DEC-005 | Payment → Enrollment | Một bộ điều kiện duy nhất, bảng quyết định đối soát đầy đủ, cho phép thanh toán trong `grace` sau hạn, `UNIQUE(order_item_id)` ở Enrollment (Mục 11.6, 11.9) | Điều chỉnh: thêm grace 15 phút; tách lý do đối soát (thiếu/thừa/Order không còn hiệu lực) |
| DEC-006 | Course bị từ chối | Chỉ `DRAFT → PENDING_REVIEW`. `REJECTED → DRAFT` bằng thao tác **mở lại (reopen)** rõ ràng | Điều chỉnh: reopen là endpoint riêng thay vì chuyển ngầm khi sửa, vì có nhiều endpoint sửa curriculum |
| DEC-007 | Kiểu ID | ID là integer int64 ≥ 1; mã nghiệp vụ là string (Mục 19.3) | Giữ nguyên |
| DEC-008 | Envelope | `{data}`, `{data, meta}`, `ErrorResponse` luôn có `traceId` (Mục 19.3) | Điều chỉnh nhỏ: `traceId` luôn có, không "nếu có" |
| DEC-009 | Ownership | Bảng quyền theo từng tài nguyên, chính sách 401/403/404, bắt buộc negative test (Mục 7.4) | Điều chỉnh: thêm quy tắc Teacher chỉ xem phần hồ sơ AI thuộc kỹ năng của Course mình |
| DEC-010 | Hết giờ Exam | `expiresAt` là nguồn sự thật, **không có ngưỡng nới**; Submit không gửi đáp án (Mục 14.8) | Giữ nguyên |
| DEC-011 | Enum `sourceType` | Một schema dùng chung | Việc của OpenAPI |
| DEC-012 | Phạm vi | Phân tầng MUST / SHOULD / LATER (Mục 5.2) và thứ tự cắt hợp nhất (Mục 23.1) | Điều chỉnh: thứ tự cắt đánh số duy nhất, hạng mục MUST không nằm trong danh sách cắt |
| DEC-013 | Kiểu tham số ID | Query ID là integer | Việc của OpenAPI |
| DEC-014 | Retry session ≠ attempt | Định nghĩa tách bạch (20A.14, 20A.15); mỗi task một lần nộp | Giữ nguyên |

## 0A.2. Việc cho tài liệu khác

**ERD (lên v2.2, đã cập nhật):**
- Bảng mới `adaptive_task_hint_events` (47 bảng).
- `ai_knowledge_documents`: `source_text`, CHECK theo `source_type`, cột điều khiển job (`attempt_count`, `lease_expires_at`...).
- `adaptive_task_sessions`: bỏ `requested_difficulty`; thêm cột lease/retry.
- `payments`: `review_reason_code`, `paid_webhook_log_id`. `payment_webhook_logs.unmatched_reason` tách lại.
- `enrollments`: `UNIQUE(order_item_id)`.
- Đánh dấu tầng MUST/SHOULD cho từng bảng.

**OpenAPI (lên v1.5, chưa làm):**
- `POST /teacher/courses/{id}/reopen`; `submit-review` chỉ từ `DRAFT`.
- Bỏ `targetDifficulty` khỏi tạo phiên AI; `sourceType` ⇔ payload (`oneOf` có discriminator); schema `KnowledgeDocumentSourceType` dùng chung.
- Hint: header `Idempotency-Key`, response có `duplicate`; thêm mã lỗi `AI_RETRY_LIMIT_REACHED`.
- `x-scope: MUST|SHOULD|LATER` cho từng operation.
- Query ID của knowledge documents là integer; kiểm tra lại mọi tham số ID.

---

# 0B. Lịch sử: thay đổi v3.0 → v3.1

Bản 3.1 giải quyết các mâu thuẫn nội bộ của v3.0, bổ sung các quy tắc còn bỏ ngỏ ở phần LMS, và viết lại Mục 20A theo tài liệu yêu cầu AI Tutor của nhóm.

## 0B.1. Quyết định thiết kế

| # | Chủ đề | Quyết định |
|---|---|---|
| 1 | Duyệt Course | Duyệt = xuất bản trong một bước (`PENDING_REVIEW → PUBLISHED`). Không có trạng thái `APPROVED`. Thao tác "xuất bản" riêng chỉ dùng để hiển thị lại Course `HIDDEN`. Teacher được rút yêu cầu (`PENDING_REVIEW → DRAFT`). |
| 2 | Enrollment | Bỏ trạng thái `PENDING`; Enrollment tạo ra là `ACTIVE`. |
| 3 | Attempt hết giờ | Bỏ trạng thái `EXPIRED` của Attempt; hết giờ được chấm và ghi `status = GRADED`, `submissionType = AUTO_EXPIRED`. |
| 4 | Coupon | Kiểm tra ở checkout, tăng `usedCount` khi Payment thành công, không reservation, không kiểm tra lại giá khi thanh toán; chấp nhận `usedCount` có thể vượt giới hạn nhẹ. |
| 5 | Hạn Order | Mặc định 24 giờ, cấu hình được. Tiền đến sau khi Order không còn thanh toán được thì vào hàng đợi đối soát; chỉ Admin được chuyển `EXPIRED/CANCELLED → PAID`. |
| 6 | Webhook | Luôn trả HTTP 2xx cho webhook đã xác thực hợp lệ (kể cả trùng, sai tiền, không khớp); chỉ trả 401 khi xác thực sai, 5xx khi lỗi hệ thống. |
| 7 | Checkout | Idempotent theo `Idempotency-Key`. |
| 8 | Tiền tệ | Chỉ VND, số nguyên đồng. |
| 9 | Teacher override tiến độ | Teacher được phân công vào Course có quyền override (kèm lý do, có audit); bỏ khái niệm "capability". |
| 10 | Độ khó | Một thang `EASY/MEDIUM/HARD` cho toàn hệ thống, kể cả AI. |
| 11 | Ràng buộc AI | Do Teacher cấu hình ở cấp Course/Lesson; Student không tự đặt môn học, khối/lớp, độ khó. |
| 12 | Môn học / khối lớp | Thuộc tính tùy chọn của Course (`subject_code`, `grade_level`), ghi đè được ở tài liệu và cấu hình AI. |
| 13 | Loại nhiệm vụ AI | `MULTIPLE_CHOICE` bắt buộc; `SHORT_ANSWER` là Should Have; chấm rule-based, không chấm bằng LLM. |
| 14 | API contract | OpenAPI là nguồn chuẩn; Mục 19 chỉ mô tả nhóm API và quy ước. |

## 0B.2. Việc phải cập nhật ở tài liệu khác

**ERD (lên v2.1):**
- `enrollments.status`: bỏ `PENDING`. `exam_attempts.status`: bỏ `EXPIRED`.
- `orders`: thêm `checkout_idempotency_key`, `checkout_request_hash`, `UNIQUE(user_id, checkout_idempotency_key)`.
- `courses`: thêm `subject_code`, `grade_level` (nullable). `questions`: thêm `skill_code` (nullable).
- Thêm bảng `adaptive_practice_configs` (cấu hình ràng buộc AI do Teacher đặt).
- Các bảng AI dùng thang độ khó `EASY/MEDIUM/HARD`.

**OpenAPI (lên v1.4):**
- Course: approve = publish; `publish` chỉ cho `HIDDEN`; thêm `archive` và `withdraw`.
- Teacher override tiến độ theo Course; endpoint cấu hình AI cho Teacher.
- Tạo phiên AI: bỏ `subjectCode`, `gradeLevel`, `targetDifficulty` tự do của Student; độ khó dùng `EASY/MEDIUM/HARD`; bỏ `currency` khỏi `Money`.
- `GeneratedTaskReview` cho Teacher phải có `answerKey` và `explanation`.
- Endpoint gợi ý Socratic (Should Have).

---

# 1. Mục đích tài liệu

Tài liệu này xác định phạm vi nghiệp vụ cho một phiên bản MVP đủ lớn để:

- Thể hiện rõ tính chất của một hệ thống LMS kết hợp thương mại điện tử.
- Có nhiều luồng nghiệp vụ liên kết, không chỉ là các chức năng CRUD độc lập.
- Thể hiện năng lực phân tích, thiết kế cơ sở dữ liệu, phân quyền, transaction, state machine và tích hợp hệ thống ngoài.
- Có độ phức tạp phù hợp để bảo vệ đồ án trước giảng viên.
- Vẫn có khả năng hoàn thành trong khoảng ba tháng bởi hai lập trình viên nếu quản lý phạm vi chặt chẽ.

MVP trong tài liệu này không được hiểu là một ứng dụng nhỏ chỉ gồm đăng nhập, CRUD khóa học và xem video. MVP được hiểu là **một lát cắt sản phẩm hoàn chỉnh**, trong đó người dùng có thể đi xuyên suốt các chuỗi nghiệp vụ:

```text
Tạo nội dung
→ Gửi duyệt
→ Xuất bản
→ Bán khóa học
→ Thanh toán
→ Đối soát
→ Cấp quyền học
→ Học theo lộ trình
→ Làm bài kiểm tra
→ Cập nhật hồ sơ học tập
→ Gợi ý/sinh nhiệm vụ cá nhân hóa
→ Phản hồi thích ứng
→ Hoàn thành khóa học
→ Báo cáo vận hành
```

---

# 2. Cơ sở xây dựng phạm vi

## 2.1. Nguồn nội bộ của dự án

Phạm vi được kế thừa và điều chỉnh từ ba tài liệu:

1. `Business Requirements Document.docx`
2. `MÔ TẢ THỰC THỂ, THUỘC TÍNH VÀ MỐI QUAN HỆ.md`
3. `Tai_lieu_Tech_Stack_LMS(1).md`

Các quyết định được giữ lại:

- Hệ thống có ba vai trò chính: `ADMIN`, `TEACHER`, `STUDENT`.
- Nội dung khóa học được tổ chức theo `Course → Section → Lesson`.
- Quyền học được kiểm soát bằng `Enrollment`.
- Luồng `Order → Payment → Enrollment` là luồng nghiệp vụ trọng yếu.
- Thanh toán được xác nhận tại backend thông qua webhook.
- Tiến độ học được lưu theo từng bài học.
- Bài kiểm tra được tự động chấm điểm.
- Backend dùng Spring Boot, frontend dùng React và database dùng MySQL.
- Kiến trúc là Modular Monolith, không sử dụng microservices trong MVP.

## 2.2. Tham khảo các hệ thống LMS thực tế

Phạm vi không sao chép toàn bộ tính năng của các sản phẩm thương mại, mà chọn các nghiệp vụ tiêu biểu có giá trị học thuật và kỹ thuật:

### Moodle

Moodle phân biệt:

- Hoàn thành hoạt động.
- Hoàn thành khóa học.
- Hoàn thành thủ công bởi học viên hoặc giáo viên.
- Hoàn thành theo điểm, hoạt động hoặc tiêu chí tổng hợp.
- Hạn chế truy cập dựa trên điều kiện học tập.

Điều này cho thấy `Progress` không nên chỉ là một phần trăm được cập nhật tùy ý. Tiến độ cần được suy ra từ các tiêu chí hoàn thành có cấu hình.

Tham khảo:

- [Moodle Course completion](https://docs.moodle.org/502/en/Course_completion)
- [Moodle Activity completion](https://docs.moodle.org/38/en/Using_Activity_completion)
- [Moodle Course completion settings](https://docs.moodle.org/502/en/Course_completion_settings)

### Canvas LMS

Canvas hỗ trợ:

- Module, module item.
- Điều kiện tiên quyết.
- Yêu cầu phải hoàn thành trong module.
- Khóa module.
- Theo dõi tiến độ học viên.
- Bài kiểm tra và ngân hàng câu hỏi.
- Nội dung có điều kiện theo kết quả.

MVP không triển khai Mastery Paths đầy đủ, nhưng áp dụng ý tưởng **lesson prerequisite** và **sequential learning**.

Tham khảo:

- [Canvas Instructor Guide](https://community.canvaslms.com/t5/Instructor-Guide/How-do-I-set-usage-rights-and-user-access-for-a-course-file/m-p/796)
- [Canvas module requirements and prerequisites](https://community.canvaslms.com/t5/Instructor-Guide/How-do-I-delete-a-module/ta-p/1149)
- [Canvas Mastery Paths](https://community.canvaslms.com/t5/Instructor-Guide/How-do-I-use-Mastery-Paths-in-course-modules/ta-p/906)

### Thinkific

Thinkific có các khái niệm:

- Nội dung video, text, image, PDF, quiz.
- Khóa học private hoặc hidden.
- Giáo viên/quản trị viên phụ trách khóa học.
- Yêu cầu phần trăm xem video.
- Bài học tiên quyết.
- Bài học xem thử.
- Điểm đạt tối thiểu.
- Gửi yêu cầu xuất bản và phê duyệt.

MVP áp dụng workflow xuất bản và tiêu chí hoàn thành theo loại bài học.

Tham khảo:

- [Thinkific Course Builder](https://support.thinkific.com/hc/en-us/articles/37547732533655-Introducing-Thinkific-s-New-Course-Builder)

### Teachable

Teachable hỗ trợ phát hành nội dung theo:

- Ngày cụ thể.
- Số ngày sau khi học viên đăng ký.

MVP chỉ triển khai **drip theo số ngày sau Enrollment**, không triển khai scheduler phức tạp theo nhiều múi giờ.

Tham khảo:

- [Teachable Drip Content](https://support.teachable.com/en/articles/11682465-drip-content)

---

# 3. Định vị sản phẩm

Hệ thống là nền tảng web cho một trung tâm đào tạo hoặc đơn vị kinh doanh khóa học trực tuyến.

Hệ thống **không phải marketplace đa nhà bán hàng**. Toàn bộ doanh thu được nhận bởi chủ hệ thống. Teacher là người tạo và quản lý nội dung, không phải một merchant độc lập có ví và cơ chế chia doanh thu.

Mô hình vận hành:

- Admin quản lý hệ thống, người dùng, tài chính và kiểm duyệt nội dung.
- Teacher xây dựng khóa học và theo dõi học viên trong các khóa học được phân công.
- Student tìm kiếm, mua, học và làm bài kiểm tra.
- Payment Provider gửi dữ liệu giao dịch đến backend.
- Object Storage lưu video, hình ảnh và tài liệu.

---

# 4. Mục tiêu nghiệp vụ

## 4.1. Mục tiêu chính

- Số hóa quy trình tạo, kiểm duyệt và phát hành khóa học.
- Bán khóa học trực tiếp trên website.
- Tự động xác nhận chuyển khoản và cấp quyền học.
- Kiểm soát quyền truy cập nội dung trả phí.
- Tổ chức nội dung thành lộ trình có thứ tự và điều kiện mở khóa.
- Theo dõi tiến độ và kết quả học tập.
- Hỗ trợ quản trị viên xử lý giao dịch không khớp.
- Cung cấp báo cáo phục vụ vận hành và đánh giá kết quả kinh doanh.

## 4.2. Giá trị học thuật của đồ án

Đồ án phải thể hiện được các vấn đề kỹ thuật và nghiệp vụ sau:

1. Authentication bằng access token và refresh token.
2. Authorization kết hợp role và ownership.
3. Workflow có trạng thái và quyền chuyển trạng thái.
4. Transaction nhiều bước.
5. Idempotency khi nhận webhook lặp.
6. Dữ liệu snapshot tại thời điểm mua.
7. Quyền học độc lập với đơn hàng và thanh toán.
8. Điều kiện mở khóa nội dung.
9. Chấm điểm và kiểm soát lượt thi.
10. Báo cáo tổng hợp.
11. Upload và bảo vệ tài nguyên.
12. Audit các hành động quan trọng.
13. Cá nhân hóa nhiệm vụ học tập bằng Adaptive Profile-Based RAG: hồ sơ người học, truy xuất thích ứng có kiểm soát quyền, sinh nhiệm vụ có cấu trúc và vòng phản hồi cập nhật hồ sơ.

---

# 5. Phạm vi MVP nâng cao

## 5.1. Nhóm chức năng bắt buộc

### A. Identity và phân quyền

- Đăng ký tài khoản học viên.
- Đăng nhập.
- Đăng xuất.
- Làm mới access token.
- Đổi mật khẩu.
- Quên mật khẩu bằng token một lần.
- Cập nhật hồ sơ.
- Admin khóa hoặc mở khóa tài khoản.
- Admin gán hoặc thu hồi role.
- Người dùng có thể có nhiều role.
- Authorization theo role.
- Authorization theo tài nguyên khóa học.

### B. Quản lý khóa học và workflow xuất bản

- Teacher tạo khóa học.
- Teacher chỉnh sửa khóa học được phân công.
- Admin quản lý tất cả khóa học.
- Khóa học có danh mục, giá, mô tả, ảnh đại diện, môn học và khối/lớp (tùy chọn, dùng làm giá trị mặc định cho cá nhân hóa AI).
- Khóa học có nhiều Section.
- Section có nhiều Lesson.
- Sắp xếp Section và Lesson.
- Lesson hỗ trợ video, text, tài liệu và quiz.
- Lesson có thể là bắt buộc hoặc không bắt buộc.
- Lesson có thể cho xem thử.
- Lesson có thể có điều kiện tiên quyết.
- Section có thể mở theo số ngày sau khi Enrollment được tạo.
- Teacher gửi khóa học đi duyệt.
- Admin duyệt hoặc từ chối. Duyệt thành công thì Course được xuất bản ngay.
- Admin ẩn, hiển thị lại hoặc lưu trữ Course.
- Lưu lý do từ chối.
- Teacher rút yêu cầu duyệt khi Course đang chờ duyệt.
- Không cho phép sửa các trường quan trọng trong lúc khóa học đang chờ duyệt, trừ khi Teacher rút yêu cầu.

### C. Catalog và bán khóa học

- Xem danh sách khóa học đã xuất bản.
- Tìm kiếm theo tên.
- Lọc theo danh mục, khoảng giá.
- Xem chi tiết khóa học.
- Xem nội dung curriculum công khai.
- Xem thử Lesson được phép preview.
- Thêm nhiều khóa học vào Cart.
- Loại bỏ khóa học khỏi Cart.
- Checkout nhiều khóa học.
- Backend kiểm tra lại giá và trạng thái.
- Checkout idempotent: gửi lại cùng `Idempotency-Key` không tạo thêm Order.
- Tạo Order và OrderItem.
- Lưu snapshot tên khóa học và giá.
- Áp dụng Coupon.
- Kiểm tra Coupon theo thời gian, số lượt và giá trị đơn.
- Học viên xem lịch sử đơn hàng.
- Hủy đơn chưa thanh toán.
- Đơn hết hạn tự động.

### D. Thanh toán và đối soát

- Tạo Payment cho Order.
- Sinh payment reference duy nhất.
- Hiển thị QR chuyển khoản.
- Nhận webhook.
- Lưu raw webhook.
- Xác thực webhook.
- Match giao dịch theo payment reference.
- Đối chiếu số tiền.
- Ngăn xử lý trùng.
- Cập nhật Payment và Order.
- Tạo Enrollment trong transaction.
- Giao dịch sai số tiền chuyển sang `REVIEW_REQUIRED`.
- Giao dịch không tìm thấy Order chuyển sang `UNMATCHED`.
- Admin xem hàng đợi đối soát.
- Admin liên kết thủ công giao dịch với Order.
- Admin xác nhận hoặc từ chối xử lý.
- Lưu lịch sử thao tác đối soát.

### E. Enrollment và quyền học

- Enrollment được tạo từ OrderItem.
- Admin có thể cấp quyền thủ công.
- Enrollment có ngày bắt đầu và hết hạn.
- Enrollment có thể bị thu hồi.
- Chỉ Enrollment `ACTIVE` mới cho phép học.
- Teacher chỉ xem học viên của khóa học mình phụ trách.
- Học viên không thể mua lại khóa học đang có Enrollment hợp lệ.
- Nếu Enrollment hết hạn, học viên có thể mua lại theo chính sách của hệ thống.
- Enrollment mới không được làm mất lịch sử học cũ nếu Admin chọn gia hạn.
- Hệ thống ghi nhận nguồn cấp quyền.

### F. Học tập và tiến độ

- Xem danh sách khóa học đã sở hữu.
- Tiếp tục từ Lesson gần nhất.
- Kiểm tra Enrollment khi mở nội dung.
- Kiểm tra ngày phát hành Section.
- Kiểm tra prerequisite.
- Ghi nhận lần đầu truy cập.
- Ghi nhận lần truy cập gần nhất.
- Ghi nhận thời lượng xem video gần đúng.
- Học viên đánh dấu hoàn thành với Lesson cho phép manual completion.
- Video hoàn thành khi đạt tỷ lệ xem cấu hình.
- Document/Text hoàn thành khi người học xác nhận.
- Quiz hoàn thành khi đáp ứng điều kiện của Lesson.
- Tính tiến độ trên Lesson bắt buộc và đã phát hành.
- Teacher có thể override trạng thái hoàn thành kèm lý do.
- Lưu lịch sử override.
- Khóa học hoàn thành khi đạt toàn bộ tiêu chí cấu hình.

### G. Bài kiểm tra

- Teacher tạo Exam trong Lesson.
- Exam có nhiều Question.
- Hỗ trợ `SINGLE_CHOICE` và `MULTIPLE_CHOICE`.
- Question có nhiều Option.
- Cấu hình thời gian làm bài.
- Cấu hình số lần làm tối đa.
- Cấu hình điểm đạt.
- Cấu hình xáo trộn câu hỏi và đáp án.
- Cấu hình hiển thị kết quả.
- Học viên bắt đầu một Attempt.
- Attempt có thời điểm hết hạn.
- Lưu đáp án tạm thời.
- Nộp bài thủ công hoặc tự động khi hết giờ.
- Backend chấm điểm.
- Với multiple-choice, chỉ nhận điểm khi chọn đúng tập đáp án theo cấu hình.
- Lưu điểm từng câu.
- Xác định đạt hoặc không đạt.
- Chặn Attempt vượt quá giới hạn.
- Teacher xem thống kê kết quả cơ bản.

### H. Quản trị và báo cáo

- Dashboard tổng số người dùng.
- Tổng Student và Teacher.
- Tổng khóa học theo trạng thái.
- Số Enrollment đang hoạt động.
- Số Order theo trạng thái.
- Doanh thu đã xác nhận.
- Doanh thu theo ngày hoặc tháng.
- Khóa học có doanh thu cao nhất.
- Khóa học có nhiều Enrollment nhất.
- Tỷ lệ hoàn thành theo khóa học.
- Tỷ lệ đạt bài kiểm tra.
- Danh sách giao dịch cần đối soát.
- Export CSV cho Order hoặc Enrollment.
- Audit log cho hành động quan trọng.

### I. Cá nhân hóa nhiệm vụ học tập (Adaptive Profile-Based RAG)

Chi tiết tại Mục 20A. Gồm:

- Teacher tải tài liệu vào kho tri thức và theo dõi trạng thái ingestion.
- Teacher cấu hình ràng buộc sư phạm (môn học, khối/lớp, kỹ năng, khoảng độ khó, loại bài, số lượng) ở cấp Course/Lesson.
- Hệ thống xây dựng và cập nhật hồ sơ người học từ lịch sử tương tác.
- Student tạo phiên nhiệm vụ cá nhân hóa, làm bài, nhận phản hồi và lý do gợi ý.
- Hệ thống chấm bài và cập nhật hồ sơ theo cơ chế idempotent.
- Teacher xem xét nhiệm vụ AI và đưa vào ngân hàng đề khi muốn.
- Chạy benchmark và baseline trong môi trường tách biệt với dữ liệu thật.

## 5.2. Phân tầng phạm vi: MUST / SHOULD / LATER

Chỉ hạng mục **MUST** nằm trong tiêu chí nghiệm thu (Mục 24). **SHOULD** chỉ làm khi MUST của sprint đã qua integration test và cắt theo thứ tự ở Mục 23.1. **LATER** không làm trong MVP. Mỗi operation trong OpenAPI mang `x-scope` tương ứng.

| Nhóm | MUST | SHOULD | LATER |
|---|---|---|---|
| Identity | Đăng ký, đăng nhập, refresh, đăng xuất, hồ sơ cá nhân, RBAC + ownership | Quên/đặt lại mật khẩu bằng email, đổi mật khẩu | SSO |
| Course authoring | Course, Section, Lesson, tài nguyên, upload; gửi duyệt, rút, mở lại; reorder; **một** prerequisite; drip theo `releaseAfterDays` | | Learning path phân nhánh |
| Course workflow | Admin duyệt, từ chối, ẩn, hiển thị lại, lưu trữ; phân công Teacher | | |
| Catalog, Cart, Order | Catalog (tìm kiếm, lọc, chi tiết, preview); Cart nhiều Course; checkout idempotent; lịch sử và hủy Order | Coupon toàn hệ thống; Coupon theo Course | Marketplace nhiều bên bán, chia doanh thu, affiliate |
| Payment | Webhook SePay, idempotency, polling trạng thái; hàng đợi đối soát + approve/reject | Ghép (match) thủ công | Hoàn tiền tự động |
| Enrollment | Cấp tự động sau thanh toán, Admin grant/revoke, job hết hạn, gia hạn | | |
| Learning | Kiểm soát truy cập, heartbeat, hoàn thành, tiến độ, Course completion | Teacher override tiến độ | Chứng chỉ |
| Exam | Question bank single-choice, Exam builder, Attempt snapshot, autosave, submit, auto-expire, kết quả (2 chế độ hiển thị) | Multiple-choice, chế độ `AFTER_EXAM_CLOSED`, thống kê Exam | Nhiều kiểu câu hỏi khác |
| Admin | Quản lý User, Audit, dashboard cơ bản (doanh thu, Order, Enrollment, hàng chờ) | Dashboard nâng cao (top Course, doanh thu theo ngày), export CSV | Data warehouse |
| AI | Ingest text/file, cấu hình ràng buộc, retrieval có kiểm soát quyền, phiên/nhiệm vụ/nộp bài, hồ sơ (EMA), chấm rule-based, benchmark tối thiểu (track nghiên cứu, tách runtime) | Hint Socratic, `SHORT_ANSWER`, query rewriting + rerank, CF + Hybrid, Teacher duyệt nhiệm vụ → Question, chỉ số AI | Recommendation engine riêng, chấm bằng LLM |
| Khác | | | Notification service, message broker |

---

# 6. Các chức năng không thuộc MVP

- Livestream do hệ thống tự triển khai.
- Video transcoding đa độ phân giải.
- DRM hoặc chống quay màn hình.
- Chat thời gian thực.
- Diễn đàn.
- Marketplace nhiều nhà bán hàng.
- Ví điện tử cho Teacher.
- Chia doanh thu tự động.
- Affiliate.
- Subscription định kỳ.
- Hoàn tiền tự động qua ngân hàng.
- Mobile application.
- Recommendation ngoài phạm vi cá nhân hóa nhiệm vụ học tập tại Mục 20A (ví dụ gợi ý khóa học để bán, recommendation marketplace).
- Chứng chỉ có blockchain hoặc chữ ký số.
- Chấm bài tự luận bằng AI.
- Chấm nhiệm vụ AI bằng LLM (MVP chấm theo quy tắc).
- SCORM.
- LTI.
- Learning path phân nhánh theo điểm như Canvas Mastery Paths.
- Nhiều payment gateway trong cùng MVP.

---

# 7. Vai trò và mô hình phân quyền

## 7.1. Vai trò

### ADMIN

- Quản lý toàn bộ dữ liệu.
- Gán role.
- Phân công Teacher cho Course.
- Duyệt và xuất bản Course.
- Xem tài chính.
- Xử lý đối soát.
- Cấp hoặc thu hồi Enrollment.
- Override tiến độ.
- Xem AuditLog.
- Xem trạng thái ingestion và log AI trong toàn hệ thống.

### TEACHER

- Tạo Course.
- Quản lý Course do mình tạo hoặc được phân công.
- Quản lý Section, Lesson, Exam và Question.
- Gửi yêu cầu duyệt.
- Xem Enrollment của Course mình phụ trách.
- Xem tiến độ và kết quả thi.
- Override tiến độ của học viên trong Course mình được phân công (kèm lý do, có audit).
- Không được xem doanh thu toàn hệ thống.
- Không được tự xuất bản nếu chưa được Admin duyệt.
- Tải tài liệu vào kho tri thức AI và cấu hình ràng buộc sư phạm cho Course/Lesson mình phụ trách.
- Xem hồ sơ học tập AI của học viên trong Course mình phụ trách.
- Xem xét và duyệt/từ chối nhiệm vụ AI thành Question chính thức.

### STUDENT

- Quản lý hồ sơ.
- Xem catalog.
- Mua khóa học.
- Xem Order và Payment của chính mình.
- Học khóa học có Enrollment hợp lệ.
- Cập nhật tiến độ của chính mình.
- Làm Exam.
- Xem kết quả theo cấu hình.
- Dùng nhiệm vụ cá nhân hóa trong phạm vi Course đã ghi danh, xem hồ sơ AI của chính mình.

## 7.2. Phân quyền theo tài nguyên

Chỉ kiểm tra role là không đủ.

Ví dụ Teacher A không được sửa Course của Teacher B.

Quy tắc:

```text
canManageCourse(user, course):
    return user has ADMIN
        OR user is an active CourseTeacher of course
        OR user is course.createdBy and still assigned
```

Với API con như Section, Lesson và Exam, backend phải truy ngược về Course để kiểm tra ownership.

## 7.3. Ma trận quyền

| Chức năng | Admin | Teacher được phân công | Teacher khác | Student |
|---|---:|---:|---:|---:|
| Tạo Course | Có | Có | Có | Không |
| Sửa Course | Có | Có | Không | Không |
| Gửi duyệt, rút yêu cầu, mở lại Course bị từ chối | Có | Có | Không | Không |
| Duyệt Course | Có | Không | Không | Không |
| Xuất bản | Có | Không | Không | Không |
| Xem học viên Course | Có | Có | Không | Không |
| Xem toàn bộ doanh thu | Có | Không | Không | Không |
| Mua Course | Có thể | Có thể | Có thể | Có |
| Học Course có Enrollment | Có | Có | Có | Có |
| Xử lý Payment | Có | Không | Không | Không |
| Quản lý tài liệu và cấu hình AI của Course | Có | Có | Không | Không |
| Duyệt nhiệm vụ AI thành Question | Có | Có | Không | Không |
| Xem hồ sơ AI của học viên | Có | Có (Course mình phụ trách) | Không | Chỉ của mình |
| Dùng nhiệm vụ cá nhân hóa | Có nếu có Enrollment | Có nếu có Enrollment | Có nếu có Enrollment | Có |

## 7.4. Quy tắc sở hữu theo tài nguyên

Backend là nơi duy nhất quyết định quyền. Ẩn nút ở frontend không phải biện pháp bảo vệ. Mỗi endpoint phải khai báo: (1) yêu cầu xác thực, (2) role, (3) quy tắc sở hữu dưới đây, (4) ít nhất một negative test.

| Tài nguyên | Điều kiện truy cập |
|---|---|
| Course, Section, Lesson, tài nguyên Lesson, cấu hình AI | `canManageCourse` (Mục 7.2), và Course ở trạng thái cho phép sửa (Mục 8.3) |
| Question, Exam, ExamQuestion | Truy ngược về Course của Question/Exam, áp dụng `canManageCourse` |
| Danh sách học viên, tiến độ học viên, thống kê Exam, override tiến độ | `canManageCourse` hoặc Admin |
| Phân công Teacher, gán role, grant/revoke Enrollment, khóa tài khoản | Chỉ Admin |
| Cart, Order, Payment | Chủ sở hữu (`user_id` = người đang đăng nhập); Admin đọc và xử lý |
| Enrollment, LessonProgress, My Courses | `student_id` = người đang đăng nhập; Admin đọc |
| ExamAttempt, câu trả lời, kết quả | `student_id` = người đang đăng nhập. Teacher xem kết quả qua API của Course, không qua Attempt của người khác |
| Tài liệu tri thức AI | `canManageCourse`; Admin đọc toàn hệ thống |
| Phiên, nhiệm vụ, lần nộp, gợi ý AI | Chủ sở hữu (Student); cần Enrollment hợp lệ trong Course |
| Hồ sơ AI của học viên | Chính học viên; Teacher chỉ khi học viên có (hoặc từng có) Enrollment ở Course Teacher phụ trách, truyền kèm `courseId`, và **chỉ thấy các kỹ năng thuộc `allowedSkills` của Course đó** |
| Duyệt nhiệm vụ AI | `canManageCourse` của Course gắn với phiên |
| Webhook log, đối soát, doanh thu toàn hệ thống, export, AuditLog | Chỉ Admin |

**Chính sách mã lỗi:**

- `401`: chưa xác thực hoặc token hết hạn.
- `403`: sai role, hoặc đúng role nhưng không được phân công (ví dụ Teacher B sửa Course của Teacher A).
- `404`: tài nguyên **riêng tư của người khác** (Order, Attempt, phiên AI của Student khác) để không tiết lộ sự tồn tại.
- `409`/`422`: vi phạm trạng thái hoặc quy tắc nghiệp vụ.

Một User có thể vừa là Teacher của Course này vừa là Student của Course khác; quyền được xét theo từng tài nguyên, không theo "role cao nhất".

---

# 8. Workflow khóa học

## 8.1. Trạng thái

```text
DRAFT
PENDING_REVIEW
REJECTED
PUBLISHED
HIDDEN
ARCHIVED
```

## 8.2. Chuyển trạng thái

```text
DRAFT → PENDING_REVIEW
PENDING_REVIEW → PUBLISHED   (Admin duyệt = xuất bản)
PENDING_REVIEW → DRAFT       (Teacher rút yêu cầu)
PENDING_REVIEW → REJECTED
REJECTED → DRAFT            (Teacher mở lại, thao tác riêng)
PUBLISHED → HIDDEN
HIDDEN → PUBLISHED
PUBLISHED → ARCHIVED
HIDDEN → ARCHIVED
```

## 8.3. Quy tắc

- Course mới luôn ở `DRAFT`.
- Teacher chỉ gửi duyệt khi Course có ít nhất một Section, một Lesson bắt buộc, giá hợp lệ và ảnh đại diện.
- **Chỉ `DRAFT` mới được gửi duyệt.** Gửi duyệt từ `REJECTED`, `PUBLISHED`, `HIDDEN` hoặc `ARCHIVED` bị từ chối (`409`).
- Khi `PENDING_REVIEW`, Teacher không được thay đổi curriculum trừ khi rút yêu cầu.
- Course `REJECTED` chưa sửa được. Teacher phải **mở lại** (`REJECTED → DRAFT`) rồi mới chỉnh sửa và gửi duyệt lại. Mở lại là thao tác rõ ràng (có endpoint riêng, ghi AuditLog), không chuyển ngầm khi gọi một API sửa nào đó.
- Sửa nội dung được phép ở `DRAFT`, `PUBLISHED` và `HIDDEN`; không được ở `PENDING_REVIEW`, `REJECTED`, `ARCHIVED`. Sửa Course đã `PUBLISHED` có hiệu lực ngay, không cần duyệt lại trong MVP (nhóm đã xác nhận chấp nhận rủi ro này; có AuditLog thay đổi giá và Mục 13.6 về tiến độ).
- Teacher không bao giờ tự đưa Course sang `PUBLISHED`; chỉ có hai đường: Admin duyệt, hoặc Admin hiển thị lại Course `HIDDEN`.
- Admin từ chối phải nhập lý do.
- Duyệt là một bước duy nhất: duyệt thành công thì Course chuyển `PUBLISHED` và `published_at` được ghi. Không có trạng thái `APPROVED`. Thao tác "xuất bản" riêng chỉ dùng để hiển thị lại Course `HIDDEN`.
- Rút yêu cầu chuyển Course về `DRAFT` và đóng review request ở trạng thái `WITHDRAWN`.
- Chỉ `PUBLISHED` được hiển thị và bán.
- `HIDDEN` không xuất hiện trong catalog nhưng Enrollment cũ vẫn học được.
- `ARCHIVED` không được bán và chỉ được xem ở chế độ quản trị.
- Course đã có OrderItem không được xóa vật lý.
- Việc cập nhật giá không thay đổi giá trong OrderItem cũ.

---

# 9. Nghiệp vụ nội dung và lộ trình học

## 9.1. Cấu trúc

```text
Course
└── Section
    └── Lesson
        ├── Video
        ├── Text
        ├── File resources
        └── Exam
```

## 9.2. Lesson type

```text
VIDEO
TEXT
DOCUMENT
EXAM
```

## 9.3. Completion method

```text
MANUAL
VIEW_CONTENT
VIDEO_PERCENT
PASS_EXAM
SUBMIT_EXAM
```

## 9.4. Quy tắc prerequisite

Mỗi Lesson có thể yêu cầu:

- Hoàn thành Lesson trước đó.
- Hoàn thành một Lesson cụ thể.
- Đạt một Exam cụ thể.

Để giữ phạm vi khả thi, MVP chỉ cho phép tối đa một prerequisite trực tiếp trên mỗi Lesson. Prerequisite phải thuộc cùng Course, và hệ thống phải từ chối cấu hình tạo vòng lặp phụ thuộc.

## 9.5. Quy tắc drip content

Section có thể cấu hình:

```text
releaseAfterDays = 0 | positive integer
```

Ngày mở:

```text
Enrollment.enrolledAt + releaseAfterDays
```

Không triển khai lịch mở theo múi giờ riêng cho từng học viên.

## 9.6. Quy tắc course completion

Course có thể cấu hình:

```text
ALL_MANDATORY_LESSONS
ALL_MANDATORY_LESSONS_AND_PASS_REQUIRED_EXAMS
MANUAL_BY_TEACHER
```

Trạng thái completion:

```text
NOT_STARTED
IN_PROGRESS
COMPLETED
```

Course completion là kết quả suy ra từ LessonProgress và ExamAttempt, không được frontend gửi trực tiếp.

---

# 10. Nghiệp vụ Cart, Coupon và Order

## 10.1. Cart

- Mỗi User có một Cart hoạt động.
- Một Course chỉ xuất hiện một lần.
- Không thêm Course đã sở hữu.
- Không thêm Course không ở trạng thái `PUBLISHED`.
- Cart không lưu giá làm nguồn tính tiền cuối cùng.
- Giá được backend tính lại khi checkout.

## 10.2. Coupon

Coupon giúp tăng độ phức tạp nghiệp vụ nhưng vẫn có thể kiểm soát phạm vi.

### Loại Coupon

```text
PERCENTAGE
FIXED_AMOUNT
```

### Điều kiện

- Thời gian bắt đầu và kết thúc.
- Số lượt dùng tối đa toàn hệ thống.
- Số lượt dùng tối đa mỗi User.
- Giá trị Order tối thiểu.
- Chỉ áp dụng cho một số Course hoặc toàn hệ thống.
- Không cho tổng tiền âm.

### Quy tắc

- Coupon phải được kiểm tra trong transaction checkout.
- Coupon được kiểm tra đầy đủ tại checkout (thời gian, lượt dùng, giá trị đơn, phạm vi Course).
- `usedCount` và `CouponRedemption` chỉ được ghi khi Payment thành công (Mục 11.3).
- MVP không reservation quota và không kiểm tra lại Coupon khi xử lý Payment: Order đã tạo giữ nguyên giá đã chốt.
- Hệ quả được chấp nhận: nhiều Order cùng chờ thanh toán có thể làm `usedCount` vượt `totalUsageLimit` nhẹ.

## 10.3. Order snapshot

OrderItem phải lưu:

- `courseId`
- `courseTitle`
- `originalPrice`
- `discountAmount`
- `finalPrice`

Việc đổi giá Course không làm thay đổi Order đã tạo.

## 10.4. Trạng thái Order

```text
PENDING_PAYMENT
PAID
CANCELLED
EXPIRED
```

Không dùng `COMPLETED` vì sản phẩm số được fulfillment ngay sau khi thanh toán.

## 10.5. Chuyển trạng thái Order

```text
PENDING_PAYMENT → PAID
PENDING_PAYMENT → CANCELLED
PENDING_PAYMENT → EXPIRED
```

`PAID` là terminal state trong MVP.

Ngoại lệ duy nhất: Admin có thể chuyển `EXPIRED → PAID` hoặc `CANCELLED → PAID` thông qua đối soát (Mục 11.6) khi tiền đã thực nhận. Thao tác này phải có lý do, ghi `payment_reconciliation_actions` và AuditLog, và chạy trong cùng transaction tạo Enrollment.

## 10.6. Hạn thanh toán

- Order có `expiresAt`, mặc định 24 giờ sau khi tạo (cấu hình được). Đây là hạn hiển thị cho Student.
- Order còn nhận thanh toán tự động đến `expiresAt + grace`; `grace` mặc định 15 phút (`payment.late-grace-minutes`) để bù độ trễ webhook.
- Job định kỳ chuyển Order `PENDING_PAYMENT` quá `expiresAt + grace` sang `EXPIRED`, **trừ** Order đang có Payment `REVIEW_REQUIRED` (để Admin xử lý xong mới đóng) và Payment `PENDING` tương ứng sang `CANCELLED`.
- Student hủy Order: Order `CANCELLED`, Payment `CANCELLED` ngay. Không cho hủy khi Payment đang `REVIEW_REQUIRED`.
- Order `EXPIRED` hoặc `CANCELLED` không còn nhận thanh toán tự động. Tiền đến sau đó đi vào đối soát (Mục 11.6).

## 10.7. Idempotency của checkout

- Client gửi header `Idempotency-Key` (UUID) cho mỗi lần checkout logic.
- Gửi lại cùng key và cùng nội dung: trả lại kết quả của Order đã tạo, không tạo Order hay Payment mới.
- Gửi lại cùng key nhưng khác nội dung: từ chối với mã lỗi nghiệp vụ ổn định.
- Key được lưu cùng Order và duy nhất theo từng User.

## 10.8. Tiền tệ

Hệ thống chỉ dùng VND, lưu số tiền là số nguyên đồng. API không cần trường `currency`.

---

# 11. Nghiệp vụ Payment và đối soát

## 11.1. Trạng thái Payment

```text
PENDING
PAID
REVIEW_REQUIRED
FAILED
CANCELLED
```

**Chuyển trạng thái hợp lệ:**

```text
PENDING         → PAID              (webhook đủ điều kiện, dòng 8 ở 11.6)
PENDING         → REVIEW_REQUIRED   (thiếu/thừa tiền)
PENDING         → CANCELLED         (Student hủy Order, hoặc Order hết hạn)
CANCELLED       → REVIEW_REQUIRED   (tiền đến sau khi Order không còn hiệu lực, dòng 4)
REVIEW_REQUIRED → PAID              (Admin approve)
REVIEW_REQUIRED → FAILED            (Admin reject)
```

`PAID` và `FAILED` là trạng thái cuối. Mọi chuyển trạng thái khác bị từ chối.

## 11.2. Trạng thái webhook processing

```text
RECEIVED
VALIDATED
MATCHED
PROCESSED
DUPLICATE
UNMATCHED
FAILED
```

## 11.3. Luồng chuẩn

1. Student checkout.
2. Backend tạo Order.
3. Backend tạo Payment.
4. Backend sinh `paymentReference`.
5. Frontend hiển thị QR.
6. Provider gửi webhook.
7. Backend lưu raw webhook.
8. Backend xác thực API key hoặc signature.
9. Backend kiểm tra idempotency.
10. Backend tìm Payment.
11. Backend áp bảng quyết định ở Mục 11.6; các bước 12–13 chỉ chạy ở nhánh dòng 8 (đủ điều kiện).
12. Trong transaction:
    - Payment → `PAID`
    - Order → `PAID`
    - Tạo Enrollment cho từng OrderItem
    - Ghi Coupon redemption
13. WebhookLog → `PROCESSED`.
14. Student nhìn thấy trạng thái mới qua polling.

**Phản hồi cho provider:** sau khi xác thực thành công và lưu raw webhook, backend trả HTTP 2xx cho mọi trường hợp nghiệp vụ (xử lý thành công, trùng, sai số tiền, không khớp Order). Chỉ trả 401 khi xác thực sai và 5xx khi lỗi hệ thống để provider retry. Retry không được tạo dữ liệu trùng.

## 11.4. Idempotency

Một webhook gửi lại không được:

- Tạo thêm Enrollment.
- Cộng Coupon usage nhiều lần.
- Cập nhật doanh thu nhiều lần.
- Tạo thêm Payment.

Khóa idempotency ưu tiên (khóa theo **giao dịch**, vì hậu quả là tiền):

```text
(provider, providerTransactionId)
```

Nếu provider không có mã giao dịch ổn định, dùng:

```text
(provider, providerEventId)
```

Webhook trùng vẫn được ghi log (trạng thái `DUPLICATE`) và trả 2xx. Quyết định "lần nhận đầu tiên" do ràng buộc UNIQUE ở database chốt, không dựa vào kiểm tra trong code.

## 11.5. Sai số tiền

### Trả thiếu

- Payment → `REVIEW_REQUIRED`.
- Order giữ `PENDING_PAYMENT`.
- Admin được chọn:
  - Từ chối.
  - Xác nhận thủ công nếu chính sách cho phép (ghi nhận số tiền thực nhận).
- MVP không cộng dồn nhiều giao dịch cho một Payment. Khách chuyển bổ sung sẽ tạo giao dịch riêng, đi vào hàng đợi `UNMATCHED` (lý do `PAYMENT_NOT_PENDING`) để Admin xử lý.

### Trả thừa

- Payment → `REVIEW_REQUIRED`.
- Không tự động tạo Enrollment trong MVP.
- Admin xác nhận thủ công.
- Hệ thống lưu số tiền thực nhận và lý do xử lý.
- Khi Admin xác nhận thủ công (approve), Payment → `PAID`, Order → `PAID`, Enrollment và CouponRedemption được tạo trong cùng transaction; doanh thu chỉ tính từ thời điểm này. Khi Admin từ chối (reject), Payment → `FAILED` và lý do được lưu.

## 11.6. Giao dịch không khớp

Webhook luôn được lưu, kể cả khi không khớp. Admin có một hàng đợi đối soát gồm hai loại: webhook `UNMATCHED` và Payment `REVIEW_REQUIRED`. Webhook `DUPLICATE` không vào hàng đợi.

Reference sai định dạng được coi là `UNKNOWN_REFERENCE`.

Webhook đã xác thực và không trùng được xử lý theo bảng quyết định sau, **theo đúng thứ tự từ trên xuống**:

| # | Điều kiện | Webhook | Payment | Lý do (mã) |
|---|---|---|---|---|
| 1 | Không có payment reference | `UNMATCHED` | — | `NO_REFERENCE` |
| 2 | Reference không tìm thấy Payment | `UNMATCHED` | — | `UNKNOWN_REFERENCE` |
| 3 | Payment đã `PAID` | `UNMATCHED` | Giữ `PAID` | `ALREADY_PAID` |
| 4 | Order không ở `PENDING_PAYMENT` (`EXPIRED`/`CANCELLED`) | `MATCHED` | → `REVIEW_REQUIRED` | review: `ORDER_NOT_PAYABLE` |
| 5 | Order còn `PENDING_PAYMENT` nhưng Payment không ở `PENDING` (`REVIEW_REQUIRED`/`FAILED`) | `UNMATCHED` | Giữ nguyên | `PAYMENT_NOT_PENDING` |
| 6 | Số tiền nhận < số tiền cần | `MATCHED` | → `REVIEW_REQUIRED` | review: `AMOUNT_UNDERPAID` |
| 7 | Số tiền nhận > số tiền cần | `MATCHED` | → `REVIEW_REQUIRED` | review: `AMOUNT_OVERPAID` |
| 8 | Còn lại: Payment `PENDING`, Order `PENDING_PAYMENT`, đúng số tiền | `PROCESSED` | → `PAID` | — |

Hành động Admin trên hàng đợi:

- `REVIEW_REQUIRED`: **approve** (Payment → `PAID`, cấp Enrollment) hoặc **reject** (Payment → `FAILED`).
- `UNMATCHED`: **match** thủ công với một Payment (Should Have), sau đó xử lý như dòng 4–8, hoặc ghi nhận đã xử lý ngoài hệ thống (ví dụ hoàn tiền thủ công).
- Mọi hành động bắt buộc có lý do, ghi `payment_reconciliation_actions` và AuditLog.

## 11.7. Transaction boundary

Việc cập nhật các dữ liệu sau phải nguyên tử:

```text
Payment
Order
Enrollment
CouponRedemption
```

WebhookLog có thể được lưu trước transaction nghiệp vụ để không mất dấu request khi transaction chính rollback.

## 11.8. Xử lý concurrency

Tình huống:

- Hai webhook của cùng giao dịch đến đồng thời.
- Student polling trong lúc webhook đang xử lý.
- Admin xác nhận thủ công cùng lúc webhook tự động chạy.

Biện pháp:

- Unique constraint.
- Lock Payment/Order khi xử lý.
- Kiểm tra state trước update.
- Transaction isolation phù hợp.
- Optimistic locking bằng version hoặc pessimistic lock tại luồng payment.

## 11.9. Hợp đồng Payment → Order → Enrollment

Đây là điều kiện **duy nhất** để cấp quyền học khi mua hàng. Mọi module dùng cùng điều kiện này.

**Cấp tự động** (dòng 8 ở Mục 11.6) khi và chỉ khi tất cả đều đúng:

1. Webhook đã xác thực và là lần nhận đầu của giao dịch.
2. Reference khớp một Payment `PENDING`.
3. Order của Payment đang `PENDING_PAYMENT`.
4. Số tiền thực nhận bằng đúng số tiền Payment cần.

**Cấp thủ công** chỉ qua Admin approve ở hàng đợi đối soát, kèm lý do và audit.

Hệ quả khi đủ điều kiện, trong **một transaction**:

```text
Payment → PAID (paid_at, received_amount, paid_webhook_log_id)
Order   → PAID (kể cả từ EXPIRED/CANCELLED khi Admin approve)
Enrollment ACTIVE cho từng OrderItem
CouponRedemption + tăng Coupon.used_count (nếu có Coupon)
Webhook log → PROCESSED
```

Bảo đảm không cấp hai lần:

- `UNIQUE(provider, provider_transaction_id)` ở `payments`.
- Webhook trùng bị chặn bởi `primary_dedup_key` (Mục 11.4).
- `UNIQUE(order_item_id)` ở `enrollments` (mỗi OrderItem tạo tối đa một Enrollment; renewal là OrderItem mới).
- Lock Payment/Order và kiểm tra trạng thái trước khi cập nhật.

Doanh thu (Mục 15.1) tính theo **số tiền thực nhận** của Payment `PAID`, nên trường hợp Admin chấp nhận trả thiếu/thừa được ghi đúng số tiền đã thu.

---

# 12. Nghiệp vụ Enrollment

## 12.1. Nguồn

```text
PURCHASE
ADMIN_GRANTED
RENEWAL
```

## 12.2. Trạng thái

```text
ACTIVE
EXPIRED
REVOKED
```

## 12.3. Quy tắc

- Enrollment mua hàng tham chiếu `OrderItem`; mỗi `OrderItem` tạo tối đa một Enrollment (`UNIQUE(order_item_id)`).
- Enrollment được tạo ở trạng thái `ACTIVE` ngay khi thanh toán thành công hoặc Admin cấp quyền. MVP không dùng trạng thái `PENDING`.
- Admin grant có thể không có OrderItem.
- Một User không có quá một Enrollment `ACTIVE` trên cùng Course.
- Có thể lưu nhiều Enrollment lịch sử.
- Enrollment chỉ active khi `startsAt <= now < expiresAt`, hoặc không có expiresAt.
- Job định kỳ chuyển Enrollment quá hạn sang `EXPIRED`.
- Thu hồi phải có lý do.
- Học viên bị khóa tài khoản không được học dù Enrollment vẫn ACTIVE.
- Course HIDDEN vẫn cho Enrollment cũ học.
- Course ARCHIVED có thể cho học tiếp hoặc không theo cấu hình; MVP chọn **vẫn cho học** nếu Enrollment hợp lệ.

## 12.4. Gia hạn

Khi mua lại Course đã hết hạn:

- Tạo Enrollment mới với source `RENEWAL`.
- Lịch sử LessonProgress cũ được giữ.
- Enrollment mới có thể liên kết lại tiến độ cũ qua `studentId + courseId`, hoặc hệ thống sao chép trạng thái.
- MVP chọn thiết kế LessonProgress gắn với `studentId + courseId + lessonId`, không gắn cứng với một Enrollment lịch sử, để gia hạn không mất tiến độ.

Đây là thay đổi có chủ ý so với mô hình cũ gắn Progress trực tiếp vào Enrollment.

---

# 13. Nghiệp vụ Progress

## 13.1. LessonProgress

Theo dõi:

- Trạng thái.
- Lần truy cập đầu.
- Lần truy cập gần nhất.
- Watched seconds.
- Progress percent.
- Completion source.
- Completed at.
- Overridden by.
- Override reason.

## 13.2. Trạng thái

```text
NOT_STARTED
IN_PROGRESS
COMPLETED
```

## 13.3. Completion source

```text
STUDENT_MANUAL
SYSTEM_VIDEO
SYSTEM_EXAM
TEACHER_OVERRIDE
ADMIN_OVERRIDE
```

## 13.4. Video progress

Frontend gửi heartbeat định kỳ.

Backend phải:

- Chỉ nhận giá trị tăng hợp lý.
- Không tin tưởng tuyệt đối `watchedSeconds`.
- Giới hạn watchedSeconds không vượt quá duration.
- Không cộng dồn đơn giản nếu người học xem lại.
- Lưu mốc lớn nhất hoặc các segment nếu triển khai nâng cao.

MVP chọn:

```text
watchedSeconds = max(watchedSeconds hiện tại, giá trị hợp lệ mới)
```

Đây không chống gian lận hoàn toàn nhưng đủ cho đồ án.

## 13.5. Tính tiến độ Course

```text
courseProgress =
    số Lesson bắt buộc đã COMPLETED
    / tổng Lesson bắt buộc đã PUBLISHED và đã RELEASED
    × 100
```

Các Lesson chưa đến ngày drip không tính vào mẫu số cho đến khi được mở.

## 13.6. Khi nội dung thay đổi

Nếu Teacher thêm Lesson bắt buộc sau khi Student đã hoàn thành Course:

- Không tự động thu hồi trạng thái completed cũ.
- CourseCompletion lưu snapshot thời điểm hoàn thành.
- Progress hiện tại có thể giảm nhưng Completion lịch sử vẫn được giữ.
- MVP hiển thị:
  - `completedAt` lịch sử.
  - `currentProgress` hiện tại.

MVP chỉ lưu `CourseCompletion` lần đầu hoàn thành. Gia hạn Enrollment không tạo bản ghi hoàn thành mới.

Điểm này tạo chiều sâu nghiệp vụ và tránh thay đổi dữ liệu lịch sử.

---

# 14. Nghiệp vụ Exam

## 14.1. Cấu trúc

```text
Exam
├── ExamQuestion
│   └── Question
│       └── QuestionOption
└── ExamAttempt
    └── AttemptAnswer
        └── AttemptAnswerOption
```

## 14.2. Trạng thái Exam

```text
DRAFT
PUBLISHED
CLOSED
```

## 14.3. Trạng thái Attempt

```text
IN_PROGRESS
SUBMITTED
GRADED
```

Không có trạng thái `EXPIRED` của Attempt. Attempt hết giờ được đóng, chấm và ghi `status = GRADED`, `submissionType = AUTO_EXPIRED`.

## 14.4. Bắt đầu Attempt

Backend kiểm tra:

- Enrollment hợp lệ.
- Lesson đã mở.
- Prerequisite đã đạt.
- Exam đang `PUBLISHED`.
- Trong thời gian mở.
- Chưa vượt maximum attempts.
- Không có Attempt IN_PROGRESS còn hợp lệ.

Khi bắt đầu:

- Tạo Attempt.
- Snapshot danh sách Question.
- Snapshot thứ tự.
- Snapshot điểm từng câu.
- Tính expiresAt.

Snapshot giúp tránh Teacher sửa Question làm thay đổi Attempt đang diễn ra.

## 14.5. Lưu đáp án

- Học viên chỉ cập nhật Attempt của chính mình.
- Không cập nhật Attempt đã submit.
- Không cập nhật sau `expiresAt` (so với đồng hồ server, không có ngưỡng nới). Frontend nên gửi autosave cuối ít nhất vài giây trước khi hết giờ.
- Selected options phải thuộc Question của Attempt.
- Autosave có thể ghi đè đáp án cũ.

## 14.6. Chấm điểm

### Single choice

Đúng khi chọn đúng Option duy nhất.

### Multiple choice

MVP dùng chính sách exact match:

```text
selectedCorrectOptions = allCorrectOptions
AND noIncorrectOptionSelected
```

Không chấm điểm từng phần.

### Tổng điểm

```text
score =
    tổng scoreAwarded
    / tổng maximumScore
    × 100
```

### Pass

```text
passed = score >= passingScore
```

## 14.7. Hiển thị kết quả

Cấu hình:

```text
IMMEDIATE_SCORE_ONLY
IMMEDIATE_WITH_CORRECT_ANSWERS
AFTER_EXAM_CLOSED
```

MVP có thể triển khai hai chế độ đầu và để chế độ thứ ba ở mức Should Have.

## 14.8. Attempt hết giờ

Khi frontend phát hiện hết giờ, frontend gọi submit.

Backend luôn kiểm tra expiresAt. Nếu request đến sau hạn:

- Đóng Attempt.
- Chấm theo đáp án đã autosave và ghi `status = GRADED`, `submissionType = AUTO_EXPIRED`.
- Request submit không mang đáp án; vì vậy việc nộp trễ không thể bổ sung hay thay đổi đáp án và không gia hạn thời gian.
- Job định kỳ đóng các Attempt `IN_PROGRESS` đã quá `expiresAt` mà Student không gửi request nào.

`expiresAt` do server tính khi bắt đầu Attempt và là nguồn sự thật duy nhất; không phụ thuộc timer frontend.

---

# 15. Nghiệp vụ báo cáo

## 15.1. Doanh thu

Chỉ tính Payment `PAID`, theo số tiền thực nhận (`received_amount`).

Không tính:

- Order pending.
- Giao dịch review chưa xác nhận.
- Giao dịch failed.
- Payment bị hủy.

## 15.2. Chỉ số

- Gross revenue.
- Số paid orders.
- Average order value.
- Enrollment theo Course.
- Completion rate.
- Pass rate.
- Payment reconciliation rate.
- Webhook duplicate count.
- Số giao dịch unmatched.
- Số Course theo workflow state.

## 15.3. Công thức

```text
Average Order Value =
    Total Paid Revenue / Number of Paid Orders
```

```text
Completion Rate =
    Number of Students Completed Course
    / Number of Active or Historical Enrollments
    × 100
```

Cần ghi rõ mẫu số báo cáo. MVP chọn tất cả Enrollment từng ACTIVE, loại Admin test account nếu có cờ dữ liệu thử nghiệm.

## 15.4. Tính nhất quán

Dashboard có thể cập nhật gần thời gian thực bằng query tổng hợp trực tiếp.

Không cần message broker hoặc data warehouse trong MVP.

## 15.5. Chỉ số AI (Should Have)

- Số phiên nhiệm vụ AI theo trạng thái (thành công, thất bại, thiếu bằng chứng).
- Tỷ lệ nhiệm vụ AI qua validation.
- Số tài liệu theo trạng thái ingestion.
- Phân bố mức thành thạo theo kỹ năng của một Course (cho Teacher).

---

# 16. Audit nghiệp vụ

AuditLog chỉ ghi hành động quan trọng:

- Gán hoặc thu hồi role.
- Khóa tài khoản.
- Duyệt/từ chối/xuất bản/lưu trữ Course và Teacher mở lại Course bị từ chối.
- Thay đổi giá Course.
- Admin grant/revoke Enrollment.
- Override LessonProgress.
- Xử lý giao dịch thủ công.
- Thay đổi trạng thái Payment.
- Export dữ liệu nhạy cảm.
- Duyệt hoặc từ chối nhiệm vụ AI thành Question chính thức.
- Xóa hoặc gỡ tài liệu AI khỏi retrieval.
- Thay đổi cấu hình ràng buộc AI của Course/Lesson.

Không audit mọi request đọc dữ liệu.

AuditLog lưu:

- Actor.
- Action.
- Entity type.
- Entity ID.
- Old value.
- New value.
- Reason.
- IP.
- Timestamp.

---

# 17. Danh sách thực thể đề xuất

## Identity

1. `users`
2. `roles`
3. `user_roles`
4. `refresh_tokens`
5. `password_reset_tokens`

## Course authoring

6. `course_categories`
7. `courses`
8. `course_teachers`
9. `course_review_requests`
10. `course_sections`
11. `lessons`
12. `lesson_prerequisites`
13. `file_resources`
14. `lesson_resources`

## Commerce

15. `carts`
16. `cart_items`
17. `coupons`
18. `coupon_courses`
19. `coupon_redemptions`
20. `orders`
21. `order_items`

## Payment

22. `payments`
23. `payment_webhook_logs`
24. `payment_reconciliation_actions`

## Learning

25. `enrollments`
26. `lesson_progress`
27. `progress_override_logs`
28. `course_completions`

## Exam

29. `exams`
30. `questions`
31. `question_options`
32. `exam_questions`
33. `exam_attempts`
34. `attempt_questions`
35. `attempt_answers`
36. `attempt_answer_options`

## Supporting

37. `audit_logs`

## AI Adaptive Profile-Based RAG

38. `ai_knowledge_documents`
39. `ai_document_chunks`
40. `learner_profiles`
41. `learner_skill_states`
42. `learning_interactions`
43. `adaptive_practice_configs`
44. `adaptive_task_sessions`
45. `adaptive_generated_tasks`
46. `adaptive_task_attempts`
47. `adaptive_task_hint_events` (Should Have)

Tổng cộng **47 bảng**: 37 bảng LMS và 10 bảng AI.

## Thay đổi cột ở bảng hiện có (so với ERD v2.0)

- `courses`: thêm `subject_code`, `grade_level` (nullable).
- `questions`: thêm `skill_code` (nullable).
- `orders`: thêm `checkout_idempotency_key`, `checkout_request_hash`.
- `enrollments.status`: bỏ `PENDING`.
- `exam_attempts.status`: bỏ `EXPIRED`.

## Thay đổi so với ERD v2.1 (ERD v2.2)

- Bảng mới `adaptive_task_hint_events`.
- `ai_knowledge_documents`: `source_text`; điều khiển job (`attempt_count`, `lease_expires_at`, `next_retry_at`).
- `adaptive_task_sessions`: bỏ `requested_difficulty`; thêm `attempt_count`, `auto_retry_count`, `manual_retry_count`, `lease_expires_at`, `worker_id`, `next_retry_at`, `last_error_message`.
- `payments`: `review_reason_code`, `paid_webhook_log_id`.
- `payment_webhook_logs.unmatched_reason`: `NO_REFERENCE/UNKNOWN_REFERENCE/ALREADY_PAID/PAYMENT_NOT_PENDING`.
- `enrollments`: `UNIQUE(order_item_id)`.

Số bảng lớn không đồng nghĩa phải tạo CRUD cho tất cả bảng. Nhiều bảng là bảng nội bộ phục vụ transaction, lịch sử, pipeline AI và khả năng tái lập.

---

# 18. Các ràng buộc database quan trọng

```text
UNIQUE(users.email)
UNIQUE(roles.code)
UNIQUE(user_roles.user_id, user_roles.role_id)
UNIQUE(course_categories.slug)
UNIQUE(courses.slug)
UNIQUE(course_teachers.course_id, course_teachers.teacher_id)
UNIQUE(carts.user_id)
UNIQUE(cart_items.cart_id, cart_items.course_id)
UNIQUE(coupons.code)
UNIQUE(orders.code)
UNIQUE(payments.payment_reference)
UNIQUE(payments.provider, payments.provider_transaction_id)
UNIQUE(payment_webhook_logs.primary_dedup_key)   -- chỉ đặt ở lần nhận đầu của một giao dịch
UNIQUE(enrollments.order_item_id)
UNIQUE(lesson_progress.student_id, lesson_progress.course_id, lesson_progress.lesson_id)
UNIQUE(exam_attempts.exam_id, exam_attempts.student_id, exam_attempts.attempt_number)
UNIQUE(attempt_answers.attempt_id, attempt_answers.attempt_question_id)
UNIQUE(attempt_answer_options.attempt_answer_id, attempt_answer_options.option_id)
UNIQUE(orders.user_id, orders.checkout_idempotency_key)
UNIQUE(learner_profiles.student_id)
UNIQUE(learner_skill_states.student_id, learner_skill_states.skill_code)
UNIQUE(learning_interactions.event_key)
UNIQUE(adaptive_generated_tasks.session_id, adaptive_generated_tasks.position)
UNIQUE(ai_document_chunks.document_id, ai_document_chunks.index_version, ai_document_chunks.chunk_index)
UNIQUE(adaptive_practice_configs.course_id, adaptive_practice_configs.scope_key)   -- scope_key = COALESCE(lesson_id, 0)
UNIQUE(adaptive_task_sessions.student_id, adaptive_task_sessions.client_request_id)
UNIQUE(adaptive_task_attempts.task_id, adaptive_task_attempts.student_id)
UNIQUE(adaptive_task_attempts.student_id, adaptive_task_attempts.client_event_id)
UNIQUE(adaptive_task_hint_events.student_id, adaptive_task_hint_events.idempotency_key)
UNIQUE(adaptive_task_hint_events.task_id, adaptive_task_hint_events.student_id, adaptive_task_hint_events.hint_index)
```

Một Enrollment ACTIVE duy nhất trên một User/Course được bảo vệ bằng service transaction vì MySQL không có partial unique index đơn giản.

---

# 19. API nghiệp vụ

**OpenAPI là nguồn chuẩn của API contract** (xem `OpenAPI_LMS_MVP_AI_Adaptive_RAG`). Mục này chỉ mô tả nhóm API và các quy ước nghiệp vụ để tránh duy trì hai danh sách.

## 19.1. Nhóm API

| Nhóm | Phạm vi |
|---|---|
| Identity | Đăng ký, đăng nhập, refresh, đăng xuất, quên/đặt lại/đổi mật khẩu, hồ sơ cá nhân |
| Course authoring | Course, Section, Lesson, resource, prerequisite, sắp xếp curriculum (reorder cả danh sách), gửi duyệt, rút yêu cầu, mở lại |
| Course workflow (Admin) | Duyệt (= xuất bản), từ chối, ẩn, hiển thị lại, lưu trữ |
| Catalog | Danh sách (tìm kiếm, lọc danh mục, khoảng giá, sắp xếp), chi tiết, curriculum công khai, preview |
| Cart và Order | Giỏ hàng, kiểm tra Coupon, checkout, lịch sử đơn, hủy đơn |
| Payment | Trạng thái thanh toán (polling), webhook SePay, đối soát (match, approve, reject) |
| Learning | Khóa học của tôi, mở Lesson, heartbeat, hoàn thành, tiến độ, override tiến độ |
| Exam | Exam builder, Question/Option, bắt đầu Attempt, autosave, nộp bài, kết quả, thống kê |
| Admin | Người dùng, role, phân công Teacher, grant/revoke Enrollment, dashboard, export CSV, audit log |
| AI | Tài liệu tri thức, cấu hình ràng buộc, hồ sơ học tập, phiên nhiệm vụ, nộp bài, duyệt nhiệm vụ AI |

## 19.2. Quy ước

- Response thành công dùng envelope `{ "data": ... }`; lỗi dùng cấu trúc lỗi chuẩn kèm mã nghiệp vụ ổn định và `traceId`.
- Checkout bắt buộc header `Idempotency-Key` (Mục 10.7).
- Heartbeat, hoàn thành Lesson, nộp Attempt và nộp nhiệm vụ AI dùng `clientEventId` để idempotent.
- Webhook thanh toán không dùng JWT; xác thực bằng cơ chế của provider và luôn trả 2xx theo Mục 11.3.
- Server là nguồn sự thật cho thời lượng video, thời điểm hết hạn Attempt và trạng thái khóa Lesson; client không gửi các giá trị này.
- Phiên nhiệm vụ AI và ingestion tài liệu là bất đồng bộ: API trả trạng thái ban đầu, client theo dõi bằng GET.

## 19.3. Quy ước kỹ thuật chung

Các quy ước này được khóa chung giữa Đặc tả, ERD và OpenAPI (Decision Register v0.2).

**Định danh, thời gian, tiền**

- ID: MySQL `BIGINT UNSIGNED`; Java `Long`; JSON/OpenAPI `integer` + `int64` + `minimum: 1`. Không dùng chuỗi cho ID. Mã nghiệp vụ (`slug`, `orderCode`, `paymentReference`, `providerTransactionId`, `skillCode`) là chuỗi.
- Mọi tham số ID (path, query, body) là integer.
- Thời gian: lưu UTC `DATETIME(6)`; API dùng ISO 8601 có `Z` hoặc offset.
- Tiền: VND số nguyên đồng; Java `long`; không dùng `float`/`double`.

**Response**

- Thành công: `{ "data": ... }`. Danh sách: `{ "data": [...], "meta": { "page", "size", "totalElements", "totalPages" } }`. Không trả entity trần.
- Lỗi: một `ErrorResponse` với `code` ổn định, `message`, `details` (nếu có) và `traceId` (luôn có). Frontend phân nhánh theo `code`, không theo `message`.
- Webhook có ACK riêng.
- Phân trang: `page` bắt đầu từ 0, mặc định `size = 20`, tối đa 100.

**Idempotency**

| Thao tác | Cơ chế |
|---|---|
| Checkout | Header `Idempotency-Key` (UUID), duy nhất theo User, so khớp hash nội dung |
| Xin gợi ý AI (hint) | Header `Idempotency-Key` (UUID), duy nhất theo Student |
| Tạo phiên AI | `clientRequestId` trong body |
| Nộp nhiệm vụ AI | `clientEventId` trong body |
| Heartbeat, hoàn thành Lesson, nộp Exam | `clientEventId` trong body |
| Bắt đầu Attempt | Tự nhiên: trả Attempt `IN_PROGRESS` đang có |
| Webhook | Khóa theo giao dịch (Mục 11.4) |

Cùng khóa và cùng nội dung: trả lại kết quả cũ, không sinh tác dụng phụ lần hai. Cùng khóa nhưng khác nội dung: `409`.

**Phân tầng**

Mỗi operation OpenAPI mang `x-scope: MUST | SHOULD | LATER` theo Mục 5.2.

---

# 20. Acceptance criteria trọng yếu

## AC-PAY-01: Webhook lặp

**Given** một Payment đã PAID và Enrollment đã được tạo  
**When** provider gửi lại cùng webhook  
**Then**:

- Không tạo thêm Enrollment.
- Không cập nhật Coupon redemption lần hai.
- Không cộng doanh thu lần hai.
- Webhook log được đánh dấu DUPLICATE.
- API trả HTTP 2xx để provider không retry vô hạn.

## AC-PAY-02: Hai webhook đồng thời

**Given** hai request cùng providerTransactionId đến gần như đồng thời  
**When** backend xử lý  
**Then** chỉ một transaction được quyền chuyển Payment sang PAID.

## AC-PAY-03: Sai số tiền

**Given** Order cần thanh toán 500.000 VND  
**When** webhook báo nhận 450.000 VND  
**Then** Payment chuyển REVIEW_REQUIRED và không tạo Enrollment.

## AC-COURSE-01: Teacher sửa Course người khác

**Given** Teacher A không được phân công Course B  
**When** Teacher A gọi API sửa Lesson của Course B  
**Then** backend trả 403 dù Teacher A có role TEACHER.

## AC-COURSE-02: Gửi duyệt thiếu nội dung

**Given** Course chưa có Lesson bắt buộc  
**When** Teacher gửi duyệt  
**Then** hệ thống từ chối và trả mã lỗi nghiệp vụ ổn định.

## AC-LEARN-01: Truy cập không có Enrollment

**Given** Student không có Enrollment ACTIVE  
**When** Student gọi API xem video private  
**Then** backend không trả presigned URL hoặc nội dung file.

## AC-LEARN-02: Lesson chưa đến ngày phát hành

**Given** Section releaseAfterDays = 7  
**And** Enrollment mới được tạo 3 ngày  
**When** Student mở Lesson  
**Then** backend trả trạng thái LOCKED_WITH_RELEASE_DATE.

## AC-LEARN-03: Prerequisite

**Given** Lesson 2 yêu cầu Lesson 1 hoàn thành  
**When** Student chưa hoàn thành Lesson 1  
**Then** Student không được mở Lesson 2.

## AC-EXAM-01: Hết giờ

**Given** Attempt đã quá expiresAt  
**When** Student gửi đáp án mới  
**Then** backend từ chối cập nhật và đóng Attempt theo dữ liệu autosave.

## AC-EXAM-02: Sửa câu hỏi trong lúc thi

**Given** Teacher sửa Question gốc  
**And** Student đã bắt đầu Attempt trước đó  
**Then** Attempt vẫn được chấm theo snapshot cũ.

## AC-ORDER-01: Giá bị thay đổi

**Given** Course có giá 500.000 khi Order được tạo  
**When** Admin đổi giá Course thành 600.000  
**Then** Order cũ vẫn phải thanh toán 500.000.

## AC-ORDER-02: Checkout lặp

**Given** Student gửi checkout với `Idempotency-Key = K`
**When** request được gửi lại với cùng `K` và cùng nội dung
**Then** hệ thống trả lại Order đã tạo và không tạo thêm Order hay Payment.

## AC-PAY-04: Thanh toán sau khi Order hết hạn

**Given** Order đã `EXPIRED`
**When** webhook báo nhận đủ tiền theo payment reference của Order đó
**Then** Payment chuyển `REVIEW_REQUIRED`, không tạo Enrollment tự động, webhook vào hàng đợi đối soát, và chỉ Admin mới có thể chuyển Order sang `PAID`.

## AC-COURSE-03: Rút yêu cầu duyệt

**Given** Course đang `PENDING_REVIEW`
**When** Teacher rút yêu cầu
**Then** Course về `DRAFT`, review request ở `WITHDRAWN`, và Teacher chỉnh sửa lại được.

## AC-COURSE-04: Duyệt là xuất bản

**Given** Course đang `PENDING_REVIEW`
**When** Admin duyệt
**Then** Course chuyển `PUBLISHED` ngay, xuất hiện trong catalog và có thể bán.

## AC-COURSE-05: Course bị từ chối

**Given** Course đang `REJECTED`
**When** Teacher gửi duyệt ngay
**Then** hệ thống từ chối (`409`); Teacher phải mở lại (`REJECTED → DRAFT`), chỉnh sửa rồi mới gửi duyệt.

## AC-PAY-05: Admin approve đối soát

**Given** Payment `REVIEW_REQUIRED` do trả thiếu
**When** Admin approve kèm lý do
**Then** Payment `PAID` với `received_amount` thực nhận, Order `PAID`, Enrollment `ACTIVE` được tạo đúng một lần, doanh thu tính theo số tiền thực nhận, và có AuditLog. Approve lần hai không tạo thêm Enrollment.

## AC-PAY-06: Thanh toán trong thời gian ân hạn

**Given** Order `PENDING_PAYMENT` đã quá `expiresAt` nhưng chưa quá `expiresAt + grace`
**When** webhook đúng số tiền đến
**Then** thanh toán được xử lý bình thường và cấp Enrollment.

## AC-EXAM-03: Nộp sau hạn

**Given** Attempt `IN_PROGRESS` đã quá `expiresAt`
**When** Student gọi submit hoặc autosave
**Then** autosave bị từ chối `ATTEMPT_EXPIRED`; submit đóng Attempt, chấm theo đáp án đã lưu và ghi `GRADED` + `AUTO_EXPIRED`.

---


# 20A. Nghiệp vụ cá nhân hóa nhiệm vụ học tập bằng Adaptive Profile-Based RAG

> **Trạng thái phạm vi:** BẮT BUỘC. Đây là chức năng nghiên cứu và sản phẩm của đồ án. Phần này bổ sung cho các nghiệp vụ LMS hiện có; không thay thế workflow khóa học, thương mại, thanh toán, enrollment, tiến độ và bài kiểm tra.
>
> **Nguồn yêu cầu:** tài liệu yêu cầu AI Tutor của nhóm (`Nh_m-Thu_t_1.docx`, Chương 1–2). Mục này chuyển khung nghiên cứu thành yêu cầu nghiệp vụ có thể nghiệm thu. Các thuật toán trong khung nghiên cứu là giả thuyết kỹ thuật cần đánh giá bằng thực nghiệm (xem 20A.13).

## 20A.1. Mục tiêu

Hệ thống dùng dữ liệu học tập của từng Student để chọn và sinh các nhiệm vụ học tập phù hợp với kỹ năng, mức thành thạo và ràng buộc sư phạm do Teacher xác định. Nội dung sinh ra phải dựa trên tài liệu được phép truy xuất, có đáp án, có lý do cá nhân hóa giải thích được, và có thể kèm gợi ý kiểu Socratic.

Phạm vi bắt buộc gồm:
- Xây dựng hồ sơ trạng thái người học từ lịch sử tương tác và kết quả làm bài.
- Quản lý metadata kỹ năng/chủ đề, độ khó, môn học và khối/lớp cho nhiệm vụ và tài liệu.
- Truy xuất tài liệu theo ràng buộc môn học, khối/lớp và kỹ năng, kết hợp tìm kiếm ngữ nghĩa.
- Xếp hạng nhiệm vụ theo mức phù hợp với hồ sơ người học và mục tiêu sư phạm.
- Dùng LLM sinh nhiệm vụ có cấu trúc dựa trên ngữ cảnh truy xuất và nhiệm vụ ứng viên.
- Chấm kết quả và cập nhật hồ sơ để điều chỉnh các lượt gợi ý tiếp theo.
- Ghi nhận dữ liệu cần thiết để đánh giá thực nghiệm và so sánh baseline.

## 20A.2. Ánh xạ khung nghiên cứu sang hệ thống

Bài toán được phát biểu là: với người học `s_i` và ràng buộc sư phạm `C`, sinh tập `K` nhiệm vụ cá nhân hóa.

```text
F(s_i, C) = Ψ_LLM ∘ Ψ_RAG ∘ Ψ_RecSys
Ψ_RecSys : (R, P(s_i), C)           → L_rec   (Top-N nhiệm vụ ứng viên)
Ψ_RAG    : (W_i, D)                 → D'      (Top-K ngữ cảnh chuẩn)
Ψ_LLM    : (L_rec, D', P(s_i), C)   → Y       (nhiệm vụ có cấu trúc + lý do)
```

| Ký hiệu | Ý nghĩa trong tài liệu nghiên cứu | Thực thể / thành phần trong hệ thống |
|---|---|---|
| `S` | Tập người học | `users` có role STUDENT |
| `T` | Ngân hàng nhiệm vụ `(topic, difficulty, grade, type)` | `questions` đã duyệt và `adaptive_generated_tasks` đã VALID |
| `R` | Ma trận tương tác `R[i,j] = score/max_score`, khuyết thì không có giá trị | `learning_interactions` (không lưu 0 cho dữ liệu chưa làm) |
| `P(s_i)` | Hồ sơ `(M, Ā, σ, W, ST, pace)` | `learner_profiles`, `learner_skill_states` |
| `D`, `V_D` | Kho tài liệu và các chunk đã vector hóa | `ai_knowledge_documents`, `ai_document_chunks`, vector index |
| `C` | Ràng buộc `(subject*, grade*, K, difficulty*, topics*)` | `adaptive_practice_configs` (Teacher) và yêu cầu của Student trong phạm vi cho phép |

Sáu tầng của kiến trúc và vị trí trong hệ thống:

| Tầng | Vai trò | Luồng |
|---|---|---|
| 1. Data Layer | Tài liệu, kho bài tập, nhật ký tương tác, hồ sơ | Ngoại tuyến + trực tuyến |
| 2. Preprocessing & Embedding | Parse → Clean → Chunk (512 token, overlap 50 từ) → Embed → Index | Ngoại tuyến |
| 3. Retrieval (RAG) | Lọc theo môn/khối/kỹ năng, tìm kiếm ngữ nghĩa, rerank | Trực tuyến |
| 4. Recommender | Chấm ZPD, chọn Top-N nhiệm vụ ứng viên | Trực tuyến |
| 5. LLM Generation | Sinh JSON nhiệm vụ kèm lý do cá nhân hóa | Trực tuyến |
| 6. Adaptive Feedback Loop | Chấm điểm, cập nhật mức thành thạo, đồng bộ hồ sơ | Trực tuyến |

Hàm mục tiêu tham chiếu cho mức phù hợp của một nhiệm vụ:

```text
fit(t'_k, s_i) = w1 · Rel(topic_k, P(s_i)) + w2 · Score_ZPD(difficulty_k, M_i) + w3 · Cov(t'_k, D')
```

Các trọng số `w1, w2, w3` (và `α, β` của hybrid ranking nếu dùng) phải cấu hình được và được ghi vào `pipeline_version` để tái lập.

## 20A.3. Tác nhân và trách nhiệm

| Tác nhân | Trách nhiệm |
|---|---|
| Student | Xem nhiệm vụ được gợi ý, làm bài, nộp câu trả lời, xem phản hồi và lý do gợi ý, xin gợi ý Socratic (Should Have). |
| Teacher | Tải tài liệu vào kho tri thức; cấu hình ràng buộc sư phạm cho Course/Lesson mình phụ trách (môn học, khối/lớp, kỹ năng, khoảng độ khó, loại bài, số lượng tối đa); xem profile học viên trong Course của mình; duyệt/từ chối nhiệm vụ AI nếu muốn đưa vào ngân hàng đề. |
| Admin | Quản lý quyền truy cập và vận hành dữ liệu/hệ thống theo quyền quản trị hiện có; xem trạng thái ingestion và log AI. |
| Adaptive Learning Service | Tính/cập nhật hồ sơ, truy xuất tài liệu, xếp hạng nhiệm vụ, điều phối LLM và ghi kết quả. |
| LLM / Embedding / Vector Index | Thành phần kỹ thuật hỗ trợ sinh, vector hóa và truy xuất; không tự quyết định quyền truy cập nghiệp vụ. |

## 20A.4. Dữ liệu đầu vào nghiệp vụ

Các nhóm dữ liệu dưới đây là yêu cầu logic; ERD quyết định lưu bằng bảng nào.

1. **Người học:** định danh Student và phạm vi khóa học/nội dung mà Student được phép truy cập (qua Enrollment).
2. **Nhiệm vụ/bài tập:** nội dung, loại bài, kỹ năng/chủ đề, độ khó, môn học, khối/lớp, đáp án hoặc quy tắc chấm và nguồn gốc.
3. **Lịch sử tương tác:** bài đã làm, điểm/độ đúng, thời điểm, thời gian trả lời, trạng thái nộp bài; phân biệt dữ liệu chưa có với điểm bằng 0.
4. **Hồ sơ người học:** mức thành thạo theo kỹ năng, điểm trung bình và độ biến thiên khi có đủ dữ liệu, chủ đề mạnh/yếu và nhịp độ học.
5. **Kho tài liệu:** tài liệu gốc và các chunk đã xử lý, metadata, phiên bản, trạng thái lập chỉ mục và quyền truy cập.
6. **Ràng buộc sư phạm:** môn học, khối/lớp, tập kỹ năng/chủ đề, khoảng độ khó, số lượng và loại nhiệm vụ.
7. **Kết quả sinh/gợi ý:** nhiệm vụ ứng viên, thứ hạng, tài liệu/ngữ cảnh hỗ trợ, lý do cá nhân hóa, phiên bản mô hình/cấu hình cần thiết cho tái lập thực nghiệm và kết quả Student.

### Thang độ khó thống nhất

Toàn hệ thống (Question, nhiệm vụ AI, cấu hình) dùng một thang: `EASY`, `MEDIUM`, `HARD`. Để tính ZPD, mỗi mức được ánh xạ sang số thực trong [0,1] (đề xuất `EASY = 0.25`, `MEDIUM = 0.5`, `HARD = 0.75`; giá trị chính thức chốt trong tài liệu phương pháp). Không dùng thang `BEGINNER/INTERMEDIATE/ADVANCED`.

### Metadata môn học và khối/lớp

- `subject_code` và `grade_level` là thuộc tính tùy chọn của Course, dùng làm giá trị mặc định cho cấu hình AI và tài liệu của Course đó.
- Tài liệu AI và cấu hình AI có thể ghi đè giá trị này. Khi cả hai đều thiếu, giá trị được coi là **chưa biết** (BR-AI-02) và không được dùng như bộ lọc cứng cho đến khi Teacher bổ sung.

### Kỹ năng của câu hỏi bài thi

`Question` có thể gắn `skill_code` (tùy chọn). Chỉ câu hỏi có `skill_code` mới đóng góp vào `learner_skill_states`; câu hỏi không gắn kỹ năng vẫn được ghi nhận cho điểm trung bình tổng nhưng không cập nhật mức thành thạo theo kỹ năng.

## 20A.5. Cấu hình ràng buộc sư phạm do Teacher đặt

Teacher cấu hình `C` ở cấp **Course** (mặc định) hoặc **Lesson** (ghi đè). Một cấu hình gồm:

- Môn học, khối/lớp.
- Tập kỹ năng/chủ đề được phép.
- Khoảng độ khó cho phép (từ `EASY`..`HARD`).
- Loại nhiệm vụ cho phép.
- Số nhiệm vụ tối đa mỗi phiên (`K_max`).
- Cờ bật/tắt, và có cho phép xin gợi ý Socratic hay không.

Quy tắc:
- Student **không** tự đặt môn học, khối/lớp hay **độ khó**. Student chỉ chọn phạm vi (Course/Lesson đã ghi danh), số lượng `K ≤ K_max`, loại bài trong tập cho phép, và tùy chọn thu hẹp chủ đề trong `allowedSkills`.
- **Độ khó do server chọn cho từng nhiệm vụ** theo ZPD: mức mục tiêu = `M_k + δ` (`δ` cấu hình, đề xuất 0.1) được ánh xạ về mức gần nhất trong `EASY=0.25 / MEDIUM=0.5 / HARD=0.75`, rồi kẹp vào `[minDifficulty, maxDifficulty]` của cấu hình. Cold start dùng BR-AI-13. Việc điều khiển độ khó để thực nghiệm chỉ qua cấu hình nội bộ, không mở ra API Student.
- Nếu phạm vi chưa có cấu hình hợp lệ, chức năng trả trạng thái rõ ràng "chưa được cấu hình" thay vì tự suy diễn ràng buộc.
- Cấu hình ở Lesson ghi đè Course; không có ghi đè thì dùng cấu hình Course.

## 20A.6. Luồng nghiệp vụ trực tuyến

1. Student mở chức năng nhiệm vụ cá nhân hóa trong phạm vi Course/Lesson được phép.
2. Backend xác minh danh tính, vai trò, Enrollment hợp lệ và quyền truy cập tài liệu; không dựa vào kiểm tra chỉ ở frontend.
3. Hệ thống lấy cấu hình ràng buộc (20A.5), kiểm tra yêu cầu của Student nằm trong phạm vi, và lấy hồ sơ học tập hiện tại.
4. **Recommender (Ψ_RecSys):** chọn Top-N nhiệm vụ ứng viên từ ngân hàng nhiệm vụ theo hồ sơ, ZPD và ràng buộc.
5. **Adaptive Retrieval (Ψ_RAG):** tạo truy vấn từ chủ đề yếu và mục tiêu (có thể chuẩn hóa/mở rộng truy vấn), lọc ứng viên theo môn học/khối lớp/kỹ năng và quyền truy cập, tìm kiếm ngữ nghĩa, xếp hạng lại và chọn Top-K ngữ cảnh.
6. **LLM (Ψ_LLM):** nhận nhiệm vụ ứng viên, ngữ cảnh truy xuất, hồ sơ ở mức cần thiết và ràng buộc; trả về kết quả theo schema đã quy định, gồm lý do cá nhân hóa.
7. Backend xác thực schema, loại bỏ đầu ra không hợp lệ và kiểm tra nhiệm vụ phù hợp ràng buộc. Nếu không đủ bằng chứng hoặc sinh thất bại, hệ thống trả trạng thái không thể tạo nhiệm vụ đáng tin cậy hoặc phương án dự phòng đã kiểm tra; không giả vờ rằng nội dung đã được chứng minh bởi tài liệu.
8. Student làm và nộp bài; hệ thống chấm theo đáp án/quy tắc chấm đã xác định.
9. **Feedback loop:** hệ thống lưu kết quả, cập nhật mức thành thạo và chủ đề mạnh/yếu, đồng bộ hồ sơ dùng cho lượt gợi ý tiếp theo.
10. Hệ thống ghi log tối thiểu cần thiết để kiểm thử, phân tích lỗi và thực nghiệm, đồng thời bảo vệ dữ liệu cá nhân.

Phiên sinh nhiệm vụ chạy **bất đồng bộ**: tạo phiên trả trạng thái ban đầu, client theo dõi đến khi phiên kết thúc.

## 20A.7. Luồng xử lý ngoại tuyến

1. Teacher/Admin đăng ký tài liệu thuộc phạm vi được phép. `sourceType` quyết định dữ liệu đầu vào và loại trừ lẫn nhau:
   - `LMS_FILE`: bắt buộc `fileResourceId`, **cấm** `textContent`. Backend kiểm tra file tồn tại, ở trạng thái `READY`, định dạng trích xuất được (pdf, docx, txt, md), và người gọi có quyền: file do chính người gọi tải lên, hoặc đã đính kèm vào một Lesson của Course đó, hoặc người gọi là Admin.
   - `PASTED_TEXT`: bắt buộc `textContent`, **cấm** `fileResourceId`; nội dung lưu trong tài liệu, giới hạn độ dài cấu hình được (đề xuất 200.000 ký tự).
   - `BENCHMARK`: chỉ dùng nội bộ cho nghiên cứu, không tạo được qua API.
   - Luôn bắt buộc `courseId` mà người gọi quản lý được; `lessonId` (nếu có) phải thuộc Course đó.
2. Hệ thống lưu tài liệu gốc vào kho lưu trữ hiện có và metadata nghiệp vụ vào MySQL.
3. Pipeline trích xuất text, làm sạch, chia chunk đệ quy (mặc định 512 token, overlap 50 từ), gắn metadata và tạo embedding chuẩn hóa.
4. Chunk được lập chỉ mục trong vector index; trạng thái xử lý theo dõi được: `PENDING`, `PROCESSING`, `READY`, `FAILED`.
5. Khi tài liệu thay đổi hoặc bị gỡ, hệ thống đánh dấu phiên bản/index cũ không còn hợp lệ và tránh tiếp tục truy xuất nội dung đã bị thu hồi quyền.
6. Hồ sơ người học được khởi tạo từ lịch sử tương tác sẵn có (ExamAttempt, nhiệm vụ AI); việc khởi tạo từ dữ liệu lịch sử phải phân biệt với cập nhật trực tuyến sau mỗi lần làm bài.

## 20A.8. Quy tắc nghiệp vụ

- **BR-AI-01 — Phân quyền:** Student chỉ được truy xuất tài liệu thuộc khóa học/nội dung mà Student được phép xem. Vector index không phải nguồn quyết định quyền truy cập.
- **BR-AI-02 — Metadata:** Nhiệm vụ và tài liệu dùng trong cá nhân hóa phải có metadata kỹ năng/chủ đề và môn học; khối/lớp và độ khó phải có khi nguồn dữ liệu hỗ trợ. Giá trị thiếu phải được đánh dấu là chưa biết, không tự suy diễn thành nhãn chắc chắn.
- **BR-AI-03 — Ràng buộc Teacher:** Kết quả không được vi phạm môn học, khối/lớp, kỹ năng, loại bài và khoảng độ khó trong cấu hình của Teacher (20A.5).
- **BR-AI-04 — Hồ sơ người học:** Hồ sơ được suy ra từ dữ liệu tương tác hợp lệ. Dữ liệu chưa làm không được coi là kết quả sai.
- **BR-AI-05 — Cá nhân hóa có giải thích:** Mỗi nhiệm vụ cần có lý do ngắn gọn (ví dụ củng cố kỹ năng yếu hoặc luyện tập ở mức khó phù hợp); lý do không được trình bày như sự thật nếu dữ liệu không hỗ trợ.
- **BR-AI-06 — Grounding:** LLM phải nhận ngữ cảnh truy xuất và bị hướng dẫn không khẳng định nội dung ngoài bằng chứng. Khi không đủ ngữ cảnh, trả trạng thái không đủ bằng chứng hoặc yêu cầu Teacher bổ sung tài liệu.
- **BR-AI-07 — Kiểm tra đầu ra:** Đầu ra LLM phải được kiểm tra schema, trường bắt buộc, loại bài và tính hợp lệ của đáp án trước khi hiển thị/lưu thành nhiệm vụ.
- **BR-AI-17 — Đáp án chuẩn:** LLM không quyết định việc chấm; chấm luôn theo `answerKey` đã lưu. Với nhiệm vụ `GENERATED`, `MULTIPLE_CHOICE` phải có đúng một lựa chọn đúng và lựa chọn đó phải được ít nhất một chunk bằng chứng hỗ trợ, kiểm tra bởi một bước verify độc lập với bước sinh. Không qua kiểm tra thì nhiệm vụ bị `REJECTED` và không hiển thị. Nhiệm vụ `BANK` dùng đáp án của Question đã có.
- **BR-AI-08 — Cập nhật hồ sơ:** Kết quả sau khi nộp bài chỉ cập nhật một lần cho mỗi sự kiện nộp hợp lệ; gửi lại không được làm tăng/giảm mức thành thạo lặp (idempotency theo `clientEventId` / `event_key`).
- **BR-AI-09 — Tách biệt dữ liệu:** Dữ liệu benchmark công khai (ASSISTments, Junyi, EdNet-KT1) dùng cho nghiên cứu phải được phân biệt với dữ liệu người dùng thật của LMS. Không tự động nhập hồ sơ người học công khai thành hồ sơ Student thật.
- **BR-AI-10 — Khả năng truy vết:** Lưu thông tin tối thiểu để biết nhiệm vụ được sinh từ yêu cầu nào, kết quả truy xuất nào và cấu hình pipeline nào, trong giới hạn phù hợp với quyền riêng tư.
- **BR-AI-11 — Không làm gián đoạn LMS:** Lỗi LLM, embedding hoặc vector index không được làm hỏng thanh toán, enrollment, tiến độ học hay bài kiểm tra thông thường.
- **BR-AI-12 — Kiểm soát nội dung:** Nội dung sinh bởi AI phải được ghi nhận là nội dung AI tạo. Chỉ nội dung được Teacher duyệt mới được chuyển thành Question chính thức; không tự động đưa nội dung sinh vào ngân hàng đề.
- **BR-AI-13 — Cold start:** Khi một kỹ năng có ít hơn 3 tín hiệu hợp lệ, mức thành thạo mặc định là trung tính (0.5), độ khó mục tiêu lấy từ cấu hình của Teacher (mặc định `EASY`→`MEDIUM`), và lý do cá nhân hóa phải nêu rõ là "chưa đủ dữ liệu".
- **BR-AI-14 — Cập nhật mức thành thạo:** Mức thành thạo theo kỹ năng cập nhật bằng EMA: `M_k(t) = α_EMA · S_k(t) + (1 − α_EMA) · M_k(t−1)`, với `S_k(t) ∈ [0,1]` là tín hiệu từ lần nộp bài. `α_EMA` cấu hình được (đề xuất mặc định 0.3, chốt bằng thực nghiệm). Phiên bản thuật toán lưu ở `learner_skill_states.model_version`.
- **BR-AI-15 — Chế độ Socratic (Should Have):** Gợi ý được sinh sẵn theo từng bước cùng nhiệm vụ, dựa trên ngữ cảnh truy xuất, không tiết lộ đáp án; giới hạn số gợi ý mỗi nhiệm vụ (mặc định 3). Mỗi yêu cầu gợi ý idempotent (Mục 20A.15). Số gợi ý đã dùng do server đếm, không do client gửi, và làm giảm tín hiệu đưa vào EMA: `S' = S · (1 − λ_h · hints_used / max_hints)`, `λ_h` cấu hình (đề xuất 0.3).
- **BR-AI-16 — Chấm điểm:** MVP chấm nhiệm vụ AI bằng quy tắc (rule-based). Chấm bằng LLM hoặc Teacher nằm ngoài MVP, trừ khi được bổ sung kèm cơ chế độ tin cậy và xem lại.

## 20A.9. Đầu ra chức năng

Một kết quả gợi ý/sinh nhiệm vụ cần có tối thiểu:
- `taskId` hoặc định danh của kết quả sinh.
- `content` và `type`.
- `topic/skill` và `difficulty` (`EASY/MEDIUM/HARD`) đã được xác định.
- `answerKey` hoặc quy tắc chấm phù hợp loại bài; **không trả về client trước khi nộp**.
- `personalizationReason`.
- Tham chiếu nguồn/ngữ cảnh hỗ trợ ở mức có thể hiển thị an toàn.
- Trạng thái kiểm tra đầu ra.
- Trạng thái làm bài và kết quả sau khi Student nộp.

Loại nhiệm vụ MVP: `MULTIPLE_CHOICE` (một đáp án đúng) là bắt buộc; `SHORT_ANSWER` chấm theo danh sách đáp án chấp nhận sau chuẩn hóa là Should Have.

Teacher xem được đáp án, giải thích và căn cứ truy xuất của nhiệm vụ khi xem xét; Student chỉ thấy đáp án và giải thích sau khi nộp.

Không bắt buộc mọi trường phải là cột riêng; cách lưu cụ thể quyết định ở ERD/API contract.

## 20A.10. Thiết kế thực nghiệm và benchmark

Đây là phần nghiên cứu, chạy tách biệt với runtime LMS (BR-AI-09).

- **Bộ dữ liệu:** ASSISTments Skill Builder, Junyi Academy, EdNet-KT1.
- **Chia dữ liệu:** theo dòng thời gian (temporal split 70/15/15 hoặc 80/20) để chống rò rỉ dữ liệu.
- **Baselines:** Random, MostPopular, Traditional RAG, Pure CF, Pure CB-ZPD.
- **Chỉ số:** Difficulty Hit@K, Difficulty MRR, Precision@K, Recall@K, F1@K, NDCG@K, AUC-ROC, MAE, RMSE.
- **Phân tích:** kiểm định ý nghĩa thống kê (khoảng tin cậy 95%, Welch's t-test), ablation study, quét tham số (α, β), phân tích lỗi theo nhóm người học.
- **Yêu cầu tối thiểu cho đồ án:** chạy được pipeline trên ít nhất một bộ benchmark với ít nhất baseline Random, Traditional RAG và Pure CB-ZPD; các baseline và chỉ số còn lại là mục tiêu nghiên cứu, báo cáo rõ phần nào đã thực hiện.

## 20A.11. Tiêu chí nghiệm thu

**AC-AI-01 — Phân quyền tài liệu:** Student không có quyền truy cập nội dung không thể lấy nội dung đó thông qua API cá nhân hóa, kể cả khi nội dung đã có trong vector index.

**AC-AI-02 — Ràng buộc sư phạm:** Khi Teacher cấu hình môn học, khối/lớp và tập kỹ năng xác định, kết quả phải đáp ứng các bộ lọc cứng đã khai báo; trường hợp không có ứng viên phù hợp phải trả trạng thái rõ ràng.

**AC-AI-03 — Hồ sơ học tập:** Sau một lần nộp bài hợp lệ, kết quả được lưu và hồ sơ liên quan được cập nhật; gửi lại cùng sự kiện không cập nhật trùng.

**AC-AI-04 — Cá nhân hóa (test cụ thể):** Cho hai Student cùng yêu cầu trên cùng phạm vi, hồ sơ A yếu kỹ năng X và hồ sơ B mạnh kỹ năng X; nhiệm vụ xếp đầu của A phải thuộc kỹ năng X ở độ khó thấp hơn hoặc bằng mức ZPD của A, còn nhiệm vụ xếp đầu của B phải có độ khó cao hơn hoặc thuộc kỹ năng khác. Test dùng dữ liệu cố định để lặp lại được.

**AC-AI-05 — Đầu ra có cấu trúc:** Nếu LLM trả JSON sai schema, thiếu đáp án bắt buộc hoặc sai loại dữ liệu, hệ thống không hiển thị như một nhiệm vụ hợp lệ.

**AC-AI-06 — Thiếu bằng chứng:** Khi không có tài liệu đủ liên quan, hệ thống không được tuyên bố rằng câu trả lời/nhiệm vụ được hỗ trợ bởi nguồn; phải trả trạng thái thiếu bằng chứng hoặc phương án dự phòng đã kiểm tra.

**AC-AI-07 — Lỗi dịch vụ AI:** Khi LLM hoặc vector index không khả dụng, các nghiệp vụ LMS không liên quan vẫn hoạt động; chức năng AI trả lỗi có kiểm soát.

**AC-AI-08 — Khả năng tái lập:** Một lượt sinh/gợi ý được ghi nhận đủ metadata để nhóm phân tích đầu vào, cấu hình và kết quả trong môi trường thử nghiệm.

**AC-AI-09 — Vòng phản hồi:** Kết quả làm bài ảnh hưởng đến hồ sơ và lượt gợi ý sau theo quy tắc cập nhật EMA; có kiểm thử cho trường hợp đúng, sai và chưa có dữ liệu.

**AC-AI-10 — Cold start:** Student chưa có tín hiệu cho kỹ năng nào vẫn nhận được nhiệm vụ hợp lệ theo cấu hình, với lý do nêu rõ là chưa đủ dữ liệu.

**AC-AI-11 — Chưa cấu hình:** Phạm vi chưa có cấu hình ràng buộc của Teacher thì tạo phiên bị từ chối với mã lỗi rõ ràng; Student không gửi và không thể đặt môn học, khối/lớp hay độ khó; yêu cầu `targetTopics`, `taskTypes`, `taskCount` ngoài cấu hình bị từ chối.

**AC-AI-12 — Không lộ đáp án:** Mọi response gửi Student trước khi nộp bài không chứa `answerKey`; sau khi nộp mới có đáp án và giải thích.

**AC-AI-13 — Duyệt nhiệm vụ AI:** Nhiệm vụ AI chỉ trở thành Question chính thức sau khi Teacher của Course duyệt; việc duyệt được ghi AuditLog.

**AC-AI-14 — Socratic (Should Have):** Gợi ý không chứa đáp án đúng và bị giới hạn số lần. Gửi lại cùng `Idempotency-Key` trả đúng gợi ý cũ và không tăng số lần dùng; key mới trả gợi ý kế tiếp đến hết giới hạn.

**AC-AI-15 — Retry phiên:** Phiên `FAILED` do lỗi tạm thời được tự retry tối đa 3 lần rồi mới `FAILED`; lỗi `AI_NOT_CONFIGURED`, `NO_CANDIDATES`, `INSUFFICIENT_EVIDENCE` không tự retry. Retry thủ công chỉ áp dụng cho `FAILED` và tối đa 3 lần; phiên `SUCCEEDED` không retry được. Hai worker cùng nhận một phiên không sinh nhiệm vụ trùng.

**AC-AI-16 — Nguồn tài liệu:** `LMS_FILE` kèm `textContent`, hoặc `PASTED_TEXT` kèm `fileResourceId`, hoặc thiếu trường bắt buộc đều bị từ chối. File không thuộc quyền của người gọi bị từ chối.

## 20A.12. Quan hệ với module LMS

- Nguồn tín hiệu học tập: `exam_attempts`/`attempt_answers` (qua `skill_code` của Question) và `adaptive_task_attempts`.
- Nguồn quyền: `enrollments`, `courses`, `course_teachers`.
- Nhiệm vụ AI không thay thế `exam_attempts` và không ảnh hưởng `lesson_progress` hay `course_completions`.
- Lỗi AI không được chặn bất kỳ nghiệp vụ nào ở Mục 5.1 nhóm A–H (BR-AI-11).

## 20A.13. Phạm vi tối thiểu và lựa chọn kỹ thuật

Bắt buộc phải có luồng end-to-end: dữ liệu tương tác → hồ sơ người học → truy xuất thích ứng → chọn/sinh nhiệm vụ → nộp/chấm bài → cập nhật hồ sơ.

| Thành phần | Bắt buộc cho MVP | Nếu còn thời gian |
|---|---|---|
| Cập nhật mức thành thạo | EMA | BKT/DKT (chủ yếu trong benchmark) |
| Recommender | Content-Based + điểm ZPD theo hồ sơ | Collaborative Filtering, Hybrid Ranking `α·semantic + β·profile + (1−α−β)·CF` |
| Truy xuất | Lọc metadata + dense search | Query rewriting/expansion, Cross-Encoder rerank |
| Sinh nội dung | LLM trả JSON có schema + validation | Chế độ Socratic |
| Chấm điểm | Rule-based (`MULTIPLE_CHOICE`) | `SHORT_ANSWER` theo danh sách đáp án |
| Vector hồ sơ người học | Mức thành thạo theo kỹ năng (bảng) | Profile vector có trọng số recency/correctness/response time |

Các thuật toán BKT, DKT, Collaborative Filtering, Cross-Encoder và công thức Hybrid Ranking là lựa chọn/giả thuyết kỹ thuật cần được đánh giá trong tài liệu phương pháp và thực nghiệm. Không tuyên bố tất cả đã triển khai chỉ vì chúng xuất hiện trong khung nghiên cứu; nhóm phải chốt baseline và phiên bản thuật toán khả thi, sau đó báo cáo rõ phần đã thực hiện.


## 20A.14. Máy trạng thái phiên sinh nhiệm vụ và retry

```text
PENDING → PROCESSING → SUCCEEDED
                    ↘ FAILED
PROCESSING → PENDING   (retry tự động: lỗi tạm thời hoặc hết lease)
FAILED → PENDING       (retry thủ công)
SUCCEEDED              (kết thúc, không retry)
```

- **Nhận việc:** worker chuyển `PENDING → PROCESSING`, đặt `lease_expires_at` (mặc định 5 phút), tăng `attempt_count`. Mọi cập nhật sau đó của worker phải kèm điều kiện `status = PROCESSING AND attempt_count = <giá trị đã nhận>` (fencing), nên worker đến sau khi lease đã chuyển giao không ghi đè kết quả.
- **Lease hết hạn:** job định kỳ thu hồi phiên `PROCESSING` quá `lease_expires_at`, đưa về `PENDING` (tính vào `auto_retry_count`).
- **Phân loại lỗi:** *tạm thời* gồm `AI_UNAVAILABLE`, `LLM_INVALID_OUTPUT`, `INTERNAL_ERROR`: tự retry tối đa 3 lần với backoff (`next_retry_at`), hết lượt thì `FAILED`. *Nghiệp vụ* gồm `AI_NOT_CONFIGURED`, `NO_CANDIDATES`, `INSUFFICIENT_EVIDENCE`: `FAILED` ngay, không tự retry.
- **Retry thủ công** (Student): chỉ khi `FAILED`; kiểm tra lại Enrollment và cấu hình; tối đa 3 lần (`manual_retry_count`), vượt thì `409 AI_RETRY_LIMIT_REACHED`; đặt `PENDING`, đặt lại `auto_retry_count`, giữ `last_error_*` để truy vết.
- **Ghi nhiệm vụ nguyên tử:** nhiệm vụ của một phiên được ghi **một lần, trong một transaction, ở cuối** cùng với việc chuyển `SUCCEEDED`; `UNIQUE(session_id, position)` là lớp bảo vệ cuối. Nhờ vậy phiên `FAILED` không có nhiệm vụ dở dang và retry không sinh trùng.
- **Phân biệt:** *retry phiên* tạo lại danh sách nhiệm vụ cho một phiên lỗi; *attempt* là một lần nộp câu trả lời cho một nhiệm vụ. Mỗi nhiệm vụ chỉ có **một** lần nộp hợp lệ: gửi lại cùng `clientEventId` trả kết quả cũ; `clientEventId` khác trả `409 AI_TASK_ALREADY_ATTEMPTED`.
- **Ingestion tài liệu** dùng cùng cơ chế (`PENDING → PROCESSING → READY | FAILED`, lease, tự retry lỗi tạm thời tối đa 3 lần). Reindex tăng `index_version`; trong lúc xử lý lại, tài liệu không được truy xuất.

## 20A.15. Gợi ý Socratic và idempotency

- Gợi ý chỉ có ở nhiệm vụ `GENERATED`: được sinh sẵn khi tạo nhiệm vụ (`hint_steps`), nên gợi ý thứ `n` là xác định và có thể phát lại. Nhiệm vụ `BANK` không có gợi ý; xin gợi ý trả `422 AI_HINT_NOT_AVAILABLE`.
- Yêu cầu gợi ý bắt buộc header `Idempotency-Key` (UUID).
- Cùng key: trả lại đúng gợi ý đã trả, đánh dấu `duplicate`, **không** tăng số lượt dùng.
- Key mới: trả gợi ý kế tiếp, ghi một dòng `adaptive_task_hint_events`, tăng số lượt dùng; hết giới hạn thì `422 AI_HINTS_EXHAUSTED`; cấu hình tắt Socratic thì `422 AI_SOCRATIC_DISABLED`.
- Số gợi ý đã dùng khi nộp bài do server đếm từ `adaptive_task_hint_events`.
- Sự kiện gợi ý **không** ghi vào `learning_interactions`: bảng đó chỉ chứa tín hiệu kết quả làm bài để dựng ma trận `R` và cập nhật hồ sơ. Gợi ý chỉ tác động gián tiếp qua hệ số giảm tín hiệu ở BR-AI-15.

---

# 21. Kế hoạch triển khai ba tháng

Kế hoạch có hai luồng song song: **LMS core** và **AI track**. Một thành viên nghiêng về LMS core, một thành viên nghiêng về AI track; hai người review chéo và cùng làm giao diện. AI track dùng baseline đơn giản trước (EMA, ZPD, dense retrieval) và chỉ thêm kỹ thuật nâng cao khi luồng end-to-end đã chạy.

Chỉ hạng mục MUST (Mục 5.2) nằm trong tiêu chí nghiệm thu của từng sprint; hạng mục SHOULD chỉ làm khi MUST của sprint đã qua integration test.

## Sprint 1 — Foundation và Identity

- Khởi tạo Spring Boot, React, MySQL.
- Docker Compose môi trường local (thêm vector index và dịch vụ AI ở Sprint 5).
- Flyway.
- User, Role, UserRole.
- Register, login, refresh, logout.
- Security và exception contract.
- Frontend auth foundation.

## Sprint 2 — Course authoring

- Category, Course, CourseTeacher.
- Section, Lesson.
- Upload file cơ bản.
- Ownership authorization.
- UI quản lý Course.
- Sắp xếp curriculum.

## Sprint 3 — Workflow và Catalog

- Review request, duyệt/từ chối/rút yêu cầu, ẩn/hiển thị/lưu trữ.
- Public catalog.
- Search/filter.
- Preview.
- Cart.

## Sprint 4 — Order, Coupon và Payment

- Coupon.
- Checkout transaction và idempotency.
- Order snapshot.
- Payment reference và QR.
- Webhook log.
- Idempotency webhook.

## Sprint 5 — Enrollment, Learning và nền tảng AI

- LMS: Payment → Enrollment, access control, My Courses, Progress, Prerequisite, Drip content, Course completion.
- AI track: dịch vụ AI và vector index trong Docker Compose, `ai_knowledge_documents`, pipeline ingestion (parse → clean → chunk → embed → index), trạng thái xử lý, cấu hình ràng buộc của Teacher (`adaptive_practice_configs`).

## Sprint 6 — Exam và hồ sơ người học

- LMS: Exam builder, Question/Option (kèm `skill_code`), Attempt snapshot, autosave, submit và grading, Teacher statistics.
- AI track: `learning_interactions` từ ExamAttempt, `learner_profiles`, `learner_skill_states` với EMA, cold start, cập nhật idempotent.

## Sprint 7 — Admin, đối soát và pipeline sinh nhiệm vụ

- LMS: Reconciliation queue, manual match/approve/reject, Dashboard, Export CSV, Audit log.
- AI track: retrieval theo metadata và quyền, recommender ZPD, sinh nhiệm vụ bằng LLM, validation schema, phiên bất đồng bộ, nộp/chấm bài, vòng phản hồi cập nhật hồ sơ, giao diện Student và Teacher review.

## Sprint 8 — Hardening, benchmark và nghiệm thu

- Kiểm thử AI: phân quyền tài liệu, schema đầu ra LLM, thiếu bằng chứng, lỗi dịch vụ AI, không lộ đáp án, cold start.
- Chạy benchmark và baseline theo Mục 20A.10 trong môi trường tách biệt.
- Integration test.
- Concurrency test webhook.
- Authorization test.
- E2E luồng chính, gồm cả luồng AI.
- Fix bug.
- Deploy HTTPS.
- Hoàn thiện tài liệu và dữ liệu demo.

---

# 22. Dữ liệu demo khi bảo vệ

Để giảng viên thấy được độ sâu nghiệp vụ, dữ liệu demo nên chuẩn bị:

- 1 Admin.
- 2 Teacher.
- 5–10 Student.
- 3 Category.
- 5 Course với trạng thái khác nhau.
- 1 Course đang chờ duyệt.
- 1 Course bị từ chối (và minh họa: gửi duyệt ngay bị từ chối, phải mở lại).
- 2 Course published.
- 1 Course hidden.
- 1 Course có drip content.
- 1 Course có prerequisite.
- 2 Coupon.
- Nhiều Order ở các trạng thái.
- 1 webhook duplicate.
- 1 giao dịch thiếu tiền.
- 1 giao dịch đến sau khi Order hết hạn.
- 1 giao dịch unmatched.
- 1 Student hoàn thành khóa học.
- 1 Student bị khóa tại Lesson prerequisite.
- 1 Attempt hết giờ.
- 1 Attempt đạt và 1 Attempt không đạt.
- Ít nhất 2 hồ sơ học tập có mức thành thạo/chủ đề mạnh-yếu khác nhau.
- 1 kịch bản gợi ý/sinh nhiệm vụ theo kỹ năng yếu và độ khó mục tiêu.
- 1 trường hợp thiếu tài liệu truy xuất hoặc LLM trả đầu ra không hợp lệ.
- 1 minh chứng hồ sơ được cập nhật sau khi Student nộp bài.
- 1 Course có cấu hình ràng buộc AI do Teacher đặt, và 1 phạm vi chưa cấu hình bị từ chối.
- 1 Student cold start nhận nhiệm vụ với lý do "chưa đủ dữ liệu".
- 1 nhiệm vụ AI được Teacher duyệt thành Question chính thức.
- 1 kết quả benchmark (Random, Traditional RAG, Pure CB-ZPD) trình bày riêng với dữ liệu LMS.

Kịch bản bảo vệ nên trình diễn cả happy path và failure path.

---

# 23. Tự phản biện phạm vi

## 23.1. Phản biện: 47 bảng có quá nhiều không?

**Có**, nếu nhóm cố xây dựng màn hình CRUD cho mọi bảng.

**Không nhất thiết**, nếu hiểu rằng nhiều bảng là:

- Junction table.
- Snapshot table.
- Log table.
- Bảng nội bộ không có UI độc lập.

Tuy nhiên, 47 bảng (37 LMS và 10 AI) vẫn tạo áp lực lớn lên migration và integration test.

### Điều kiện để giữ phạm vi

- Không làm UI quản trị trực tiếp cho mọi bảng.
- Generate boilerplate bằng AI nhưng bắt buộc review.
- Dùng API contract sớm.
- Mỗi sprint có integration test.
- Không thêm chức năng ngoài tài liệu.

### Kế hoạch cắt giảm khẩn cấp

Chỉ hạng mục **SHOULD** (Mục 5.2) nằm trong danh sách cắt. Nếu chậm tiến độ, cắt **theo thứ tự đánh số, từ trên xuống**:

1. Gợi ý Socratic (BR-AI-15) và bảng `adaptive_task_hint_events`.
2. `SHORT_ANSWER` của nhiệm vụ AI, chỉ giữ `MULTIPLE_CHOICE`.
3. Query rewriting và Cross-Encoder rerank.
4. Collaborative Filtering và Hybrid Ranking, giữ Content-Based + ZPD.
5. Export CSV.
6. Dashboard nâng cao (top Course, doanh thu theo ngày) và chỉ số AI (Mục 15.5).
7. Teacher override tiến độ (Admin vẫn giữ).
8. Coupon theo Course, chỉ giữ Coupon toàn hệ thống.
9. Đặt lại mật khẩu bằng email; Admin đặt lại thủ công trong demo.
10. Multiple-choice của Exam, chỉ giữ single-choice.
11. Teacher duyệt nhiệm vụ AI thành Question.
12. Ghép thủ công trong đối soát, giữ approve/reject.
13. Coupon toàn hệ thống.

Nhóm cam kết giữ nguyên toàn bộ MUST, gồm Cart nhiều Course. Không có cắt giảm cấp 2; danh sách trên chỉ áp dụng cho SHOULD.

**Không cắt (MUST):**

- Ownership authorization (Mục 7.4).
- Workflow Course (Mục 8) và các máy trạng thái Order, Payment, Enrollment, Exam Attempt, phiên AI.
- Payment idempotency và hợp đồng Payment → Enrollment (Mục 11.9).
- Enrollment access control, prerequisite đơn, drip theo `releaseAfterDays`.
- Exam attempt, Progress, Course completion.
- Đối soát tối thiểu: hàng đợi, approve, reject.
- AI: kiểm tra quyền tài liệu trước retrieval (BR-AI-01).
- AI: grounding và validation đầu ra LLM, kiểm tra đáp án chuẩn (BR-AI-06, 07, 17).
- AI: cập nhật hồ sơ idempotent (BR-AI-08).
- AI: ràng buộc do Teacher cấu hình (BR-AI-03).
- AI: benchmark tối thiểu với Random, Traditional RAG và Pure CB-ZPD (track nghiên cứu).

---

## 23.2. Phản biện: Nghiệp vụ có đang “làm màu” không?

Một số chức năng có nguy cơ chỉ làm tăng số bảng nhưng không tăng giá trị:

- Notification.
- Audit toàn bộ request.
- Nhiều provider.
- Affiliate.
- Certificate.
- Recommendation AI ngoài phạm vi cá nhân hóa nhiệm vụ học tập bắt buộc tại Mục 20A (ví dụ recommendation marketplace tổng quát hoặc hệ gợi ý khóa học không phục vụ bài toán nghiên cứu).

Tài liệu đã loại các chức năng này hoặc giới hạn Audit vào hành động quan trọng.

Độ nặng chính được đặt vào:

- State transition.
- Authorization theo ownership.
- Snapshot.
- Transaction.
- Idempotency.
- Concurrency.
- Conditional access.
- Attempt lifecycle.

Đây là độ phức tạp có thể giải thích và demo rõ ràng.

## 23.3. Phản biện: Có giống Moodle/Canvas quá mức không?

Không.

Moodle và Canvas là các LMS tổng quát, hỗ trợ nhiều loại activity, plugin, gradebook, competency và learning path. MVP chỉ mượn các nguyên tắc:

- Completion criteria.
- Prerequisite.
- Module release.
- Attempt lifecycle.
- Role/capability.

Không triển khai plugin architecture, SCORM, gradebook tổng quát hoặc Mastery Paths.

## 23.4. Phản biện: Cart và Coupon có cần thiết không?

Về mục tiêu LMS, không bắt buộc.

Về mục tiêu “LMS kết hợp thương mại điện tử” và đánh giá độ nặng nghiệp vụ, Cart và Coupon giúp thể hiện:

- Snapshot giá.
- Validation khi checkout.
- Nhiều OrderItem.
- Discount rule.
- Transaction với Coupon redemption.

Vì vậy chúng được giữ ở MVP nâng cao, nhưng là nhóm đầu tiên có thể giản lược nếu tiến độ chậm.

## 23.5. Phản biện: Video progress có đáng tin không?

Không hoàn toàn.

Frontend heartbeat có thể bị giả mạo. Một hệ thống sản xuất cần:

- Theo dõi segment.
- Signed playback.
- Player event validation.
- Phân tích bất thường.

MVP chỉ lưu mốc xem gần đúng và không tuyên bố chống gian lận. Việc này vẫn đủ để thể hiện progress tracking.

## 23.6. Phản biện: Transaction Payment–Enrollment có đủ không?

Không hoàn toàn nếu xét hệ thống phân tán.

Provider đã nhận tiền là sự kiện ngoài database. Transaction MySQL chỉ bảo đảm nhất quán nội bộ. Nếu backend lỗi sau khi provider ghi nhận giao dịch nhưng trước khi xử lý xong, cần retry webhook hoặc hàng đợi đối soát.

MVP giải quyết bằng:

- Lưu raw webhook.
- Idempotency.
- Retry an toàn.
- Reconciliation queue.
- Admin xử lý thủ công.

Không cần message broker nhưng phải thừa nhận giới hạn.

## 23.7. Phản biện: Có đủ cho hai người trong ba tháng không?

Sau khi bổ sung phần AI bắt buộc (Mục 20A), phạm vi này ở mức **rất căng; chỉ bảo vệ được** nếu:

- Hai người đều làm full stack theo feature.
- Mỗi người có nền tảng Spring Boot và React.
- Dùng AI để sinh boilerplate, test case và tài liệu.
- Không dành quá nhiều thời gian làm giao diện cầu kỳ.
- Dùng một payment provider.
- Không tự xử lý video.
- Có dữ liệu demo và test sớm.
- Có kế hoạch cắt giảm.
- Chia hai luồng LMS core và AI track, review chéo.
- AI track dùng baseline đơn giản trước (EMA, ZPD, dense retrieval) và chỉ thêm Cross-Encoder, CF, Hybrid khi luồng end-to-end đã chạy.
- Dùng LLM và embedding qua API sẵn có, không tự huấn luyện mô hình.

Nếu hai thành viên mới học Spring Boot từ đầu, hoặc chưa từng làm RAG, phạm vi đầy đủ có nguy cơ không hoàn thành. Khi đó dùng kế hoạch cắt giảm ở cả hai danh sách nhưng giữ các luồng trọng tâm và luồng AI end-to-end tối thiểu.

---

# 24. Tiêu chí đánh giá hoàn thành

MVP được coi là hoàn thành khi:

- Role và ownership được kiểm tra đúng.
- Teacher tạo và gửi duyệt Course.
- Admin duyệt, từ chối và xuất bản.
- Student mua nhiều Course bằng một Order.
- Coupon (nếu còn trong phạm vi sau cắt giảm) được kiểm tra ở backend.
- QR được hiển thị.
- Webhook đúng tạo Enrollment.
- Webhook lặp không tạo dữ liệu trùng.
- Giao dịch sai tiền vào hàng đợi đối soát.
- Student không có Enrollment không xem được nội dung.
- Prerequisite và drip content hoạt động.
- Progress được tính từ Lesson bắt buộc.
- Student làm Exam, autosave, nộp và nhận điểm.
- Attempt hết giờ được xử lý ở backend.
- Teacher xem được tiến độ và kết quả của Course mình phụ trách.
- Admin xem dashboard và audit.
- Hệ thống deploy bằng HTTPS.
- Có migration và dữ liệu seed.
- Có integration test cho các luồng trọng yếu.
- Máy trạng thái Course, Order, Payment, Enrollment, LessonProgress, ExamAttempt và phiên AI có kiểm thử chuyển trạng thái hợp lệ và từ chối chuyển trạng thái không hợp lệ.
- Mỗi endpoint MUST có khai báo security, quy tắc sở hữu (Mục 7.4) và ít nhất một negative test.
- Không còn quyết định mức P1 ở trạng thái `OPEN` trong Decision Register.
- Teacher cấu hình được ràng buộc AI cho Course/Lesson; phạm vi chưa cấu hình bị từ chối rõ ràng.
- Tài liệu AI được ingest với trạng thái theo dõi được và chỉ tài liệu `READY` được truy xuất.
- Student tạo được phiên nhiệm vụ cá nhân hóa, làm bài, nhận phản hồi; hồ sơ được cập nhật đúng một lần cho mỗi lần nộp hợp lệ.
- Hai hồ sơ khác nhau cho kết quả khác nhau theo AC-AI-04.
- Đầu ra LLM sai schema, thiếu bằng chứng và lỗi dịch vụ AI được xử lý có kiểm soát mà không ảnh hưởng nghiệp vụ LMS.
- Response gửi Student trước khi nộp không lộ đáp án.
- Có kết quả benchmark tối thiểu theo Mục 20A.10, tách biệt dữ liệu LMS.

---

# 25. Kết luận

MVP nâng cao được thiết kế để tránh hai cực đoan:

- Quá nhỏ, chỉ gồm CRUD Course và xem video.
- Quá lớn, cố sao chép toàn bộ Moodle hoặc một marketplace thương mại.

Phạm vi tập trung vào năm chuỗi nghiệp vụ có khả năng thể hiện chiều sâu của đồ án:

```text
Course Authoring
→ Review
→ Publish
```

```text
Cart
→ Order
→ Payment
→ Reconciliation
→ Enrollment
```

```text
Enrollment
→ Conditional Access
→ Lesson Progress
→ Course Completion
```

```text
Exam
→ Attempt Snapshot
→ Autosave
→ Submit
→ Grade
```

```text
Teacher cấu hình ràng buộc + tài liệu
→ Ingestion và Index
→ Hồ sơ người học (EMA)
→ Retrieval + Recommender (ZPD)
→ LLM sinh nhiệm vụ + Validation
→ Student nộp bài
→ Chấm và cập nhật hồ sơ
```

Điểm mạnh của đồ án không nằm ở số lượng màn hình, mà ở việc các trạng thái, quyền, transaction và lịch sử dữ liệu được thiết kế nhất quán.

Nếu triển khai đúng các luồng trọng tâm, đây là một hệ thống đủ lớn để thể hiện kiến thức về phân tích nghiệp vụ, Spring Security, Spring Data JPA, transaction, database constraint, REST API, React state management và tích hợp webhook.
