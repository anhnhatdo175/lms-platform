# PHÂN CÔNG CÔNG VIỆC CHO 2 LẬP TRÌNH VIÊN

## Lập trình viên 1 — Quản trị khóa học và nội dung

### Backend

- Khởi tạo Spring Boot, MySQL và Flyway.
- Authentication: đăng ký, đăng nhập, refresh token, đăng xuất.
- Quản lý User, Role và Spring Security.
- Quản lý danh mục khóa học.
- Quản lý Course và CourseTeacher.
- Quản lý Section và Lesson.
- Sắp xếp Section và Lesson.
- Cấu hình Lesson preview, bắt buộc, prerequisite và drip content.
- Upload ảnh, video và tài liệu lên MinIO hoặc S3-compatible storage.
- Quản lý Course workflow:
  - Gửi duyệt.
  - Duyệt.
  - Từ chối.
  - Xuất bản.
  - Ẩn.
  - Lưu trữ.
- Kiểm tra quyền Teacher theo Course ownership.
- Public API danh sách và chi tiết khóa học.

### Frontend

- Login và Register.
- Trang quản lý Course của Teacher.
- Trang tạo và sửa Course.
- Curriculum Editor.
- Form Section và Lesson.
- Giao diện upload tài nguyên.
- Trang Admin duyệt Course.
- Public Course Catalog.
- Course Detail.
- Admin quản lý User và Role.

### Bảng database phụ trách

```text
users
roles
user_roles
refresh_tokens
password_reset_tokens
course_categories
courses
course_teachers
course_review_requests
course_sections
lessons
lesson_prerequisites
file_resources
lesson_resources
```

---

## Lập trình viên 2 — Mua khóa học, học tập và thi

### Backend

- Quản lý Cart và CartItem.
- Quản lý Coupon và tính giảm giá.
- Checkout và tạo Order.
- Snapshot tên, giá và thời hạn Course trong OrderItem.
- Hủy và hết hạn Order.
- Tạo Payment và mã thanh toán.
- Sinh QR chuyển khoản.
- Nhận và lưu webhook thanh toán.
- Kiểm tra chữ ký hoặc API key webhook.
- Chống webhook trùng.
- Kiểm tra đúng số tiền.
- Xử lý giao dịch `REVIEW_REQUIRED` và `UNMATCHED`.
- Reconciliation thủ công cho Admin.
- Tạo Enrollment khi Payment thành công.
- Admin cấp và thu hồi Enrollment.
- Kiểm tra quyền học theo Enrollment.
- My Courses.
- Ghi nhận LessonProgress và video heartbeat.
- Tính CourseProgress và CourseCompletion.
- Kiểm tra prerequisite và drip content khi học.
- Quản lý Exam, Question và QuestionOption.
- Bắt đầu ExamAttempt.
- Snapshot đề thi.
- Autosave đáp án.
- Submit và chấm điểm.
- Xử lý Attempt hết giờ.
- Cập nhật LessonProgress sau khi thi.
- Dashboard và báo cáo cơ bản.
- Audit log và export CSV nếu còn thời gian.

### Frontend

- Cart.
- Checkout.
- Payment QR và trạng thái thanh toán.
- Order History và Order Detail.
- My Courses.
- Learning Page.
- Video, Text và Document Lesson.
- Course Progress.
- Exam Builder.
- Exam Attempt và Exam Timer.
- Exam Result.
- Admin Reconciliation.
- Admin Dashboard.
- Teacher xem học viên, tiến độ và kết quả thi.

### Bảng database phụ trách

```text
carts
cart_items
coupons
coupon_courses
coupon_redemptions
orders
order_items
payments
payment_webhook_logs
payment_reconciliation_actions
enrollments
lesson_progress
progress_override_logs
course_completions
exams
questions
question_options
exam_questions
exam_attempts
attempt_questions
attempt_answers
attempt_answer_options
audit_logs
```

---

## Bảng phân công tổng hợp

| Lập trình viên 1 | Lập trình viên 2 |
|---|---|
| Spring Boot foundation | React foundation |
| Auth backend | Auth frontend |
| User và Role | Cart |
| Spring Security | Coupon |
| Category | Checkout |
| Course | Order |
| CourseTeacher | Payment |
| Section | Payment webhook |
| Lesson | Reconciliation |
| File upload | Enrollment |
| Course review workflow | Learning progress |
| Teacher Course UI | Exam |
| Admin Course UI | Reporting và Audit |
| Public Catalog | Student Learning UI |
