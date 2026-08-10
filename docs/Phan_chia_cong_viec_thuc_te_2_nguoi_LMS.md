# PHÂN CHIA CÔNG VIỆC THỰC TẾ CHO 2 LẬP TRÌNH VIÊN

## Dự án LMS kết hợp kinh doanh khóa học

**Phiên bản:** 1.0  
**Ngày lập:** 06/08/2026  
**Thời gian phát triển:** 3 tháng  
**Nhân sự:** 2 lập trình viên  
**Backend:** Java Spring Boot  
**Frontend:** React + Vite + TypeScript  
**Database:** MySQL  
**Kiến trúc:** Modular Monolith

---

# 1. Mục tiêu phân chia

Phương án phân chia công việc cần đáp ứng các yêu cầu sau:

- Hai người có thể phát triển song song.
- Cả hai đều có phần backend, frontend, database và kiểm thử.
- Mỗi người có một nhóm nghiệp vụ đủ lớn để trình bày khi bảo vệ đồ án.
- Hạn chế việc người này phải chờ API của người kia.
- Hạn chế hai người cùng sửa một file hoặc một module.
- Đặt các nghiệp vụ cần transaction trong cùng một người phụ trách.
- Không tạo quá nhiều abstraction nội bộ không cần thiết.
- Phù hợp với nhóm hai người làm MVP trong ba tháng.

Không chia theo kiểu:

```text
Người 1: toàn bộ backend
Người 2: toàn bộ frontend
```

Thay vào đó, mỗi người phụ trách một nhóm chức năng hoàn chỉnh theo chiều dọc:

```text
Database
→ Entity
→ Repository
→ Service
→ REST API
→ React UI
→ Test
```

---

# 2. Phân chia tổng thể

## 2.1. Lập trình viên 1 — Quản trị khóa học và nội dung

Lập trình viên 1 phụ trách toàn bộ phần tạo, quản lý, kiểm duyệt và xuất bản khóa học.

### Chuỗi nghiệp vụ chính

```text
Đăng nhập
→ Teacher tạo Course
→ Tạo Section và Lesson
→ Upload tài nguyên
→ Gửi duyệt
→ Admin duyệt hoặc từ chối
→ Xuất bản Course
→ Course xuất hiện trong Catalog
```

### Module backend

```text
identity
security
course
storage
```

### Chức năng backend

#### Authentication và User

- Đăng ký tài khoản.
- Đăng nhập.
- Làm mới access token.
- Đăng xuất.
- Quản lý hồ sơ người dùng.
- Quản lý role `ADMIN`, `TEACHER`, `STUDENT`.
- Khóa hoặc mở khóa tài khoản.
- Phân quyền endpoint theo role.

#### Course và Category

- CRUD danh mục khóa học.
- Teacher tạo khóa học.
- Teacher sửa khóa học được phân công.
- Admin xem và quản lý tất cả khóa học.
- Phân công Teacher cho Course.
- Kiểm tra ownership khi sửa Course.
- Quản lý giá, ảnh đại diện, mô tả và trạng thái Course.

#### Curriculum

- Tạo, sửa, xóa Section.
- Tạo, sửa, xóa Lesson.
- Sắp xếp Section.
- Sắp xếp Lesson.
- Cấu hình Lesson bắt buộc hoặc không bắt buộc.
- Cấu hình Lesson preview.
- Cấu hình prerequisite cơ bản.
- Cấu hình drip content theo Section.

#### File và tài nguyên

- Upload ảnh đại diện.
- Upload video.
- Upload tài liệu.
- Lưu metadata trong MySQL.
- Lưu file tại MinIO hoặc object storage tương thích S3.
- Tạo presigned URL khi cần.
- Không cung cấp endpoint download công khai cho nội dung private.

#### Course workflow

- Teacher gửi Course đi duyệt.
- Admin duyệt Course.
- Admin từ chối Course và nhập lý do.
- Admin publish Course.
- Admin hide Course.
- Admin archive Course.
- Chặn sửa curriculum khi Course đang chờ duyệt.
- Chặn bán Course chưa được publish.

### Chức năng frontend

```text
LoginPage
RegisterPage
TeacherCoursesPage
CreateCoursePage
EditCoursePage
CurriculumPage
CourseForm
CurriculumEditor
SectionForm
LessonForm
FileUpload
AdminCourseReviewPage
PublicCourseCatalogPage
CourseDetailPage
```

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

Tổng cộng khoảng **14 bảng**.

---

## 2.2. Lập trình viên 2 — Mua khóa học, học tập và thi

Lập trình viên 2 phụ trách toàn bộ hành trình của học viên sau khi Course đã được publish.

### Chuỗi nghiệp vụ chính

```text
Student chọn Course
→ Thêm vào Cart
→ Áp dụng Coupon
→ Checkout
→ Tạo Order
→ Tạo Payment
→ Nhận webhook
→ Tạo Enrollment
→ Học Lesson
→ Ghi nhận Progress
→ Làm Exam
→ Chấm điểm
→ Báo cáo
```

### Module backend

```text
commerce
payment
learning
exam
reporting
audit
```

### Chức năng backend

#### Cart và Coupon

- Tạo Cart cho User.
- Thêm Course vào Cart.
- Xóa Course khỏi Cart.
- Không cho thêm Course đã sở hữu.
- Không cho thêm Course chưa publish.
- Áp dụng Coupon.
- Kiểm tra thời gian hiệu lực Coupon.
- Kiểm tra giới hạn sử dụng.
- Kiểm tra giá trị đơn tối thiểu.
- Tính discount.

#### Order và Checkout

- Validate Cart.
- Đọc lại giá Course từ database.
- Tạo Order.
- Tạo OrderItem.
- Snapshot tên và giá Course.
- Tính subtotal, discount và total.
- Hủy Order chưa thanh toán.
- Chuyển Order quá hạn sang `EXPIRED`.
- Hiển thị lịch sử Order.

#### Payment

- Tạo Payment cho Order.
- Sinh payment reference.
- Sinh QR chuyển khoản.
- Nhận webhook từ provider.
- Lưu raw webhook.
- Kiểm tra chữ ký hoặc API key.
- Chống webhook trùng.
- Kiểm tra đúng số tiền.
- Chuyển giao dịch sai tiền sang `REVIEW_REQUIRED`.
- Chuyển giao dịch không khớp sang `UNMATCHED`.
- Cập nhật Payment và Order trong transaction.
- Tạo Enrollment khi thanh toán thành công.

#### Reconciliation

- Danh sách webhook cần xử lý.
- Admin match thủ công webhook với Payment hoặc Order.
- Admin approve giao dịch.
- Admin reject giao dịch.
- Lưu lịch sử thao tác đối soát.

#### Enrollment và Learning

- Tạo Enrollment từ OrderItem.
- Admin grant Enrollment thủ công.
- Thu hồi Enrollment.
- Kiểm tra Enrollment còn hiệu lực.
- My Courses.
- Kiểm tra quyền mở Lesson.
- Kiểm tra drip content.
- Kiểm tra prerequisite.
- Ghi nhận lần truy cập đầu và gần nhất.
- Video heartbeat.
- Manual completion.
- Tính Course progress.
- Tạo Course completion.

#### Exam

- Teacher tạo Exam.
- Tạo Question và Option.
- Gắn Question vào Exam.
- Cấu hình thời gian thi.
- Cấu hình số lần thi.
- Cấu hình điểm đạt.
- Student bắt đầu Attempt.
- Snapshot Question và Option.
- Autosave đáp án.
- Submit thủ công.
- Tự đóng Attempt hết giờ.
- Chấm điểm backend.
- Xác định pass hoặc fail.
- Cập nhật LessonProgress sau khi thi.

#### Reporting và Audit

- Doanh thu đã xác nhận.
- Số paid Order.
- Số Enrollment.
- Course có doanh thu cao nhất.
- Course có nhiều Enrollment nhất.
- Tỷ lệ hoàn thành.
- Tỷ lệ đạt Exam.
- Số webhook duplicate.
- Số giao dịch unmatched.
- Audit các hành động quan trọng.

### Chức năng frontend

```text
CartPage
CheckoutPage
PaymentPage
PaymentStatus
OrderHistoryPage
OrderDetailPage
MyCoursesPage
LearningPage
VideoLesson
TextLesson
DocumentLesson
CourseProgressBar
ExamBuilderPage
QuestionForm
ExamStartPage
ExamAttemptPage
ExamTimer
ExamResultPage
AdminReconciliationPage
AdminDashboardPage
```

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

Tổng cộng khoảng **23 bảng**.

Số bảng của người 2 nhiều hơn, nhưng nhiều bảng là bảng snapshot, junction hoặc log, không có CRUD UI độc lập.

---

# 3. Vì sao chia như vậy thực tế hơn

## 3.1. Payment và Enrollment nằm cùng một người

Hai nghiệp vụ này cần xử lý trong cùng transaction:

```text
Payment = PAID
→ Order = PAID
→ Tạo Enrollment
→ Ghi CouponRedemption
```

Nếu Payment và Enrollment chia cho hai người khác nhau, việc tích hợp sẽ khó hơn và dễ xảy ra lỗi transaction.

## 3.2. Exam và Progress nằm cùng một người

Sau khi Exam được chấm:

```text
ExamAttempt = GRADED
→ xác định passed
→ cập nhật LessonProgress
→ tính lại CourseProgress
```

Đặt Exam và Progress trong cùng một người giúp giảm dependency và không cần event nội bộ phức tạp trong MVP.

## 3.3. Course workflow nằm cùng Course authoring

Teacher tạo Course, sửa curriculum, gửi duyệt và Admin publish là một chuỗi nghiệp vụ liên tục. Không nên chia Course CRUD cho một người và Course Review cho người còn lại.

## 3.4. Hai người đều có phần đủ mạnh để bảo vệ

### Người 1 có thể trình bày

- Spring Security.
- Role và ownership authorization.
- Course workflow.
- State transition.
- File storage.
- Curriculum và prerequisite.

### Người 2 có thể trình bày

- Checkout transaction.
- Order snapshot.
- Payment webhook.
- Idempotency và concurrency.
- Enrollment access.
- Exam snapshot và grading.
- Reporting.

---

# 4. Điểm phụ thuộc giữa hai người

Hai người không hoàn toàn độc lập. Tuy nhiên, chỉ có một số điểm giao nhau chính.

## 4.1. Người 2 đọc Course của người 1

Người 2 cần Course để:

- Hiển thị Cart.
- Kiểm tra Course có publish hay không.
- Lấy giá tại checkout.
- Snapshot tên Course và giá.

Trong MVP, `CheckoutService` có thể sử dụng trực tiếp `CourseRepository`.

Quy tắc:

- Người 2 được đọc Course.
- Người 2 không cập nhật nội dung Course.
- Người 2 không thay đổi trạng thái Course.
- Mọi thay đổi entity Course phải báo cho người 1.

Ví dụ:

```java
Course course = courseRepository.findById(courseId)
    .orElseThrow(CourseNotFoundException::new);

if (course.getStatus() != CourseStatus.PUBLISHED) {
    throw new CourseNotAvailableException();
}
```

## 4.2. Public catalog dùng API của người 1

Người 1 cung cấp tối thiểu:

```text
GET /api/v1/courses
GET /api/v1/courses/{slug}
```

Người 2 có thể dùng response này trong Cart và Checkout UI.

## 4.3. Người 1 cần dữ liệu Student progress

Khi làm trang Teacher xem học viên, người 1 cần đọc dữ liệu do người 2 quản lý:

```text
enrollments
lesson_progress
exam_attempts
```

Giải pháp thực tế:

- Người 2 tạo API báo cáo Teacher.
- Người 1 chỉ làm giao diện gọi API.
- Không để người 1 tự viết query trực tiếp vào repository Learning hoặc Exam.

API tối thiểu:

```text
GET /api/v1/teacher/courses/{courseId}/students
GET /api/v1/teacher/courses/{courseId}/progress
GET /api/v1/teacher/courses/{courseId}/exam-results
```

---

# 5. Kế hoạch sprint

## Sprint 1 — Khởi tạo và Authentication

### Lập trình viên 1

- Khởi tạo Spring Boot.
- Cấu hình MySQL.
- Cấu hình Flyway.
- Tạo User, Role, UserRole.
- Register.
- Login.
- Refresh token.
- Logout.
- Spring Security.
- GlobalExceptionHandler.
- Swagger/OpenAPI.

### Lập trình viên 2

- Khởi tạo React + Vite + TypeScript.
- React Router.
- Axios instance.
- Access token interceptor.
- Refresh token flow.
- TanStack Query.
- Zustand auth store.
- Login UI.
- Register UI.
- ProtectedRoute.
- RoleRoute.
- Shared UI component.
- Docker Compose.

### Kết quả cuối sprint

```text
Đăng ký
→ Đăng nhập
→ Refresh token
→ Điều hướng theo role
```

---

## Sprint 2 — Course và Commerce cơ bản

### Lập trình viên 1

- Category.
- Course entity và migration.
- Course CRUD.
- CourseTeacher.
- Ownership authorization.
- Teacher Course list UI.
- Create Course UI.
- Edit Course UI.

### Lập trình viên 2

- Cart entity và migration.
- Coupon entity và migration.
- Order entity và migration.
- Cart API.
- Coupon calculator.
- Order price calculator.
- Cart UI.
- Checkout UI skeleton.

### Cách làm song song

Người 2 có thể dùng Course seed hoặc mock response trong thời gian Course API chưa hoàn tất.

---

## Sprint 3 — Curriculum, Catalog và Checkout

### Lập trình viên 1

- Section.
- Lesson.
- Sắp xếp Section và Lesson.
- FileResource.
- Upload file.
- Curriculum editor.
- Public Course API.
- Catalog UI.
- Course detail UI.

### Lập trình viên 2

- Cart hoàn chỉnh.
- Checkout transaction.
- OrderItem snapshot.
- Coupon validation.
- Order history.
- Order detail.
- Payment entity.
- Payment reference.
- QR payload.

### Kết quả cuối sprint

```text
Course publish giả lập
→ xuất hiện Catalog
→ thêm Cart
→ Checkout
→ tạo Order và Payment
```

---

## Sprint 4 — Course workflow và Payment webhook

### Lập trình viên 1

- CourseReviewRequest.
- Submit review.
- Withdraw review nếu cần.
- Approve.
- Reject.
- Publish.
- Hide.
- Archive.
- Admin review UI.

### Lập trình viên 2

- PaymentWebhookLog.
- Nhận webhook.
- Verify webhook.
- Idempotency.
- Kiểm tra đúng số tiền.
- Payment polling.
- Tạo Enrollment từ OrderItem.
- Chuyển Order sang PAID.

### Kết quả cuối sprint

```text
Teacher tạo Course
→ Admin publish
→ Student mua
→ Webhook hợp lệ
→ Enrollment được tạo
```

---

## Sprint 5 — Learning và Reconciliation

### Lập trình viên 1

- Hoàn thiện file private.
- Hoàn thiện prerequisite cấu hình từ Course.
- Fix Course workflow.
- Admin Course management.

### Lập trình viên 2

- My Courses.
- Learning page.
- Enrollment access.
- LessonProgress.
- Video heartbeat.
- Manual completion.
- Drip content.
- Prerequisite access.
- Course progress.
- Course completion.
- Reconciliation queue.
- Manual match, approve và reject.

---

## Sprint 6 — Exam

### Lập trình viên 1

- Teacher xem danh sách học viên.
- Teacher xem tiến độ cơ bản.
- Course statistics UI.
- Hoàn thiện Course và Curriculum.

### Lập trình viên 2

- Exam.
- Question.
- QuestionOption.
- ExamQuestion.
- Exam builder UI.
- ExamAttempt.
- AttemptQuestion snapshot.
- Autosave đáp án.
- Exam timer.
- Submit.
- Grading.
- Result UI.
- Cập nhật LessonProgress.

---

## Sprint 7 — Admin và Reporting

### Lập trình viên 1

- Admin User management.
- Gán và thu hồi role.
- Khóa và mở khóa User.
- Admin Course management.
- Teacher progress UI.

### Lập trình viên 2

- Revenue dashboard.
- Paid Order statistics.
- Enrollment statistics.
- Completion rate.
- Exam pass rate.
- Top Course.
- Export CSV nếu còn thời gian.
- Audit log.
- Reconciliation UI hoàn chỉnh.

---

## Sprint 8 — Hardening và chuẩn bị bảo vệ

### Lập trình viên 1 kiểm thử

- Teacher không được sửa Course người khác.
- Course thiếu nội dung không được gửi duyệt.
- Admin reject phải có lý do.
- Course chưa publish không xuất hiện Catalog.
- Course đã có OrderItem không được xóa vật lý.
- Student không truy cập được tài nguyên private khi chưa có quyền.

### Lập trình viên 2 kiểm thử

- Course đã sở hữu không được mua lại.
- Giá Course thay đổi không làm thay đổi Order cũ.
- Webhook gửi lại không tạo Enrollment lần hai.
- Hai webhook đồng thời chỉ xử lý một lần.
- Sai số tiền không tạo Enrollment.
- Attempt hết giờ không cho lưu đáp án mới.
- Question sửa sau khi bắt đầu thi không làm thay đổi Attempt snapshot.

### Cả hai

- Seed dữ liệu demo.
- Integration test luồng chính.
- Deploy HTTPS.
- Fix lỗi tích hợp.
- Hoàn thiện tài liệu.
- Chuẩn bị kịch bản demo.

---

# 6. Quy tắc Git và làm việc song song

## 6.1. Branch theo feature nhỏ

### Người 1

```text
user1/auth
user1/course-category
user1/course-crud
user1/course-curriculum
user1/course-review
user1/file-upload
```

### Người 2

```text
user2/cart
user2/coupon
user2/checkout
user2/payment
user2/payment-webhook
user2/learning
user2/exam
user2/reporting
```

Không nên tạo một branch kéo dài nhiều sprint.

## 6.2. Pull request nhỏ

Mỗi pull request chỉ nên hoàn thành một use case hoặc một nhóm use case liên quan chặt chẽ.

Ví dụ phù hợp:

```text
Add Course CRUD
Add Curriculum Management
Add Course Review Workflow
Add Cart Management
Add Checkout Transaction
Add Payment Webhook Processing
Add Exam Attempt Lifecycle
```

Ví dụ không phù hợp:

```text
Complete all backend features
Complete LMS module
Finish sprint 2 to sprint 5
```

## 6.3. Không sửa file chung tùy ý

Các file dễ conflict:

```text
SecurityConfig.java
application.yml
router.tsx
routes.ts
axios-instance.ts
query-client.ts
docker-compose.yml
pom.xml
package.json
```

Quy tắc:

- Người tạo file là người giữ ownership chính.
- Người còn lại cần thay đổi thì báo trước.
- Không refactor file chung trong PR không liên quan.
- Pull hoặc rebase branch trước khi bắt đầu sửa file chung.

## 6.4. Migration phải được đánh số trước

Đề xuất phân chia:

```text
V001__create_identity_tables.sql             → Người 1
V002__seed_roles.sql                         → Người 1
V003__create_file_resource_table.sql         → Người 1
V004__create_course_tables.sql               → Người 1
V005__create_course_review_tables.sql        → Người 1
V006__create_cart_and_coupon_tables.sql      → Người 2
V007__create_order_tables.sql                → Người 2
V008__create_payment_tables.sql              → Người 2
V009__create_enrollment_tables.sql           → Người 2
V010__create_learning_progress_tables.sql    → Người 2
V011__create_exam_tables.sql                 → Người 2
V012__create_audit_table.sql                 → Người 2
V013__create_indexes.sql                     → Chia theo bảng sở hữu
V014__seed_demo_data.sql                     → Cả hai phối hợp
```

Không để hai người cùng tạo một version migration.

---

# 7. Quy tắc API contract

Trước khi frontend sử dụng một API, cần chốt tối thiểu:

```text
HTTP method
Path
Request body
Response body
Authorization
Business error code
Pagination
```

Ví dụ checkout:

```http
POST /api/v1/orders/checkout
```

Request:

```json
{
  "couponCode": "SUMMER20"
}
```

Response:

```json
{
  "orderId": 10,
  "orderCode": "ORD-20260806-001",
  "subtotal": 600000,
  "discountAmount": 100000,
  "totalAmount": 500000,
  "expiresAt": "2026-08-06T16:00:00Z",
  "payment": {
    "paymentId": 20,
    "paymentReference": "LMS001",
    "qrContent": "...",
    "status": "PENDING"
  }
}
```

Ví dụ lỗi nghiệp vụ:

```json
{
  "code": "COURSE_ALREADY_OWNED",
  "message": "You already have access to this course",
  "timestamp": "2026-08-06T15:00:00Z"
}
```

---

# 8. Shared component và code dùng chung

## 8.1. Frontend component dùng chung

Người 2 tạo ban đầu:

```text
Button
Input
Select
Dialog
Table
Pagination
Badge
PageLoader
ErrorState
EmptyState
UnauthorizedState
PageHeader
```

Người 1 sử dụng lại, không tạo bộ component thứ hai.

## 8.2. Backend shared code

Người 1 tạo ban đầu:

```text
GlobalExceptionHandler
ErrorCode
ErrorResponse
PageResponse
BaseEntity
SecurityConfig
JwtAuthenticationFilter
AuthenticatedUser
```

Người 2 sử dụng lại, không tạo exception contract hoặc pagination contract riêng.

## 8.3. Không đặt business logic vào shared

Không đưa các logic sau vào `shared`:

```text
CouponDiscountCalculator
CourseWorkflowService
LessonAccessService
ExamGradingService
PaymentReferenceGenerator
```

Các class này phải nằm trong module nghiệp vụ tương ứng.

---

# 9. Kịch bản demo theo người phụ trách

## 9.1. Kịch bản của lập trình viên 1

```text
1. Đăng nhập Teacher.
2. Tạo Course mới.
3. Tạo Section.
4. Tạo Lesson video và Lesson text.
5. Upload tài nguyên.
6. Cấu hình Lesson bắt buộc.
7. Gửi Course đi duyệt.
8. Đăng nhập Admin.
9. Từ chối Course và nhập lý do.
10. Teacher sửa Course và gửi lại.
11. Admin duyệt và publish.
12. Course xuất hiện trong Catalog.
```

Các điểm cần giải thích:

- Role authorization.
- Ownership authorization.
- Course state machine.
- Không cho sửa khi đang chờ duyệt.
- File metadata và object storage.
- Course đã phát sinh giao dịch không bị xóa vật lý.

## 9.2. Kịch bản của lập trình viên 2

```text
1. Đăng nhập Student.
2. Thêm Course vào Cart.
3. Áp dụng Coupon.
4. Checkout.
5. Hiển thị QR.
6. Gửi webhook giả lập.
7. Payment và Order chuyển PAID.
8. Enrollment được tạo.
9. Student mở My Courses.
10. Học Lesson và cập nhật Progress.
11. Bắt đầu Exam.
12. Autosave đáp án.
13. Nộp bài.
14. Backend chấm điểm.
15. Lesson và Course progress được cập nhật.
16. Admin xem Dashboard.
```

Các điểm cần giải thích:

- Order snapshot.
- Transaction Payment–Order–Enrollment.
- Webhook idempotency.
- Xử lý sai số tiền.
- Enrollment access control.
- Attempt snapshot.
- Backend grading.
- Reporting chỉ tính Payment `PAID`.

---

# 10. Tự phản biện phương án

## 10.1. Người 2 có nhiều module hơn

Đúng. Người 2 sở hữu Commerce, Payment, Learning và Exam nên có phạm vi lớn hơn về số bảng.

Tuy nhiên:

- Nhiều bảng chỉ là bảng trung gian hoặc snapshot.
- Người 1 phải xử lý Security, Course ownership, workflow và file storage.
- Phần của người 1 có ít bảng hơn nhưng nhiều rule phân quyền và trạng thái.

Nếu người 2 bị quá tải, cắt giảm theo thứ tự:

1. Coupon theo từng Course, chỉ giữ Coupon toàn hệ thống.
2. Drip content.
3. Progress override.
4. Multiple-choice, chỉ giữ single-choice.
5. Export CSV.
6. Cart nhiều Course, chuyển sang Buy Now.

Không nên cắt:

- Payment idempotency.
- Order snapshot.
- Enrollment access.
- Lesson progress.
- Exam attempt.
- Course workflow.
- Ownership authorization.

## 10.2. Người 1 có ít phần Student hơn

Đúng. Người 1 tập trung vào Teacher/Admin và người 2 tập trung vào Student.

Đây là sự chia theo hành trình người dùng thực tế và giảm conflict. Khi bảo vệ, cả hai vẫn có thể trình bày một phần của luồng tổng thể.

## 10.3. Người 2 dùng CourseRepository trực tiếp có phá kiến trúc không?

Trong Modular Monolith MVP, việc một service đọc repository của module khác có thể chấp nhận nếu nhóm thống nhất rõ quyền sở hữu và không tạo phụ thuộc vòng.

Quy tắc thực dụng:

- Chỉ đọc dữ liệu Course.
- Không cập nhật Course từ Commerce.
- Không gọi ngược Commerce từ Course.
- Khi dự án lớn hơn mới tách query service hoặc facade.

## 10.4. Có nên dùng event cho Exam và Progress không?

Chưa cần trong MVP.

Người 2 sở hữu cả Exam và Learning nên có thể gọi service trực tiếp:

```java
if (attempt.isPassed()) {
    lessonProgressService.completeByExam(
        attempt.getStudentId(),
        exam.getLesson().getId()
    );
}
```

Spring event chỉ cần khi hai module được giao cho hai nhóm khác nhau hoặc cần giảm coupling sau này.

---

# 11. Phân công cuối cùng

| Lập trình viên 1 | Lập trình viên 2 |
|---|---|
| Spring Boot foundation | React foundation |
| Auth backend | Auth frontend |
| User và Role | Cart |
| Spring Security | Coupon |
| Category | Checkout |
| Course | Order |
| CourseTeacher | Payment |
| Section | Webhook |
| Lesson | Reconciliation |
| File upload | Enrollment |
| Course review workflow | Learning progress |
| Teacher UI | Exam |
| Admin Course UI | Reporting |
| Public Catalog | Student learning UI |

---

# 12. Kết luận

Phương án phù hợp nhất cho nhóm hai người là:

```text
LẬP TRÌNH VIÊN 1
Auth backend
+ User/Role
+ Course
+ Curriculum
+ Storage
+ Review workflow
+ Teacher/Admin Course UI
+ Public Catalog
```

```text
LẬP TRÌNH VIÊN 2
Auth frontend
+ Cart/Coupon
+ Order/Payment
+ Reconciliation
+ Enrollment/Learning
+ Exam
+ Reporting
+ Student UI
```

Cách chia này không cân bằng tuyệt đối theo số bảng, nhưng cân bằng hơn theo độ khó thực tế:

```text
Người 1: Security, ownership, workflow, content và file
Người 2: Transaction, webhook, progress, exam và reporting
```

Hai người có thể làm song song phần lớn thời gian. Các điểm giao nhau được giới hạn tại:

```text
Course publish
→ Commerce đọc Course

Payment thành công
→ tạo Enrollment

Teacher xem tiến độ
→ gọi API Learning/Exam
```

Đây là cách chia phù hợp với đồ án ba tháng, giảm conflict code và vẫn giúp cả hai thành viên có đủ nội dung kỹ thuật để trình bày khi bảo vệ.
