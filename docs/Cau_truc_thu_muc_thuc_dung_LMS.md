# CẤU TRÚC THƯ MỤC THỰC DỤNG CHO DỰ ÁN LMS

## Hệ thống học trực tuyến và kinh doanh khóa học

**Phiên bản:** 1.0  
**Ngày lập:** 06/08/2026  
**Backend:** Java 21 + Spring Boot 3.x  
**Frontend:** React + Vite + TypeScript  
**Database:** MySQL 8.x  
**Kiến trúc:** Modular Monolith  
**Phạm vi:** MVP 3 tháng, nhóm 2 lập trình viên  

---

# 1. Mục tiêu thiết kế cấu trúc

Cấu trúc thư mục này được xây dựng theo hướng:

- Chia mã nguồn theo module nghiệp vụ.
- Giữ ranh giới tương đối rõ giữa các module.
- Không áp dụng Clean Architecture hoặc DDD đầy đủ một cách máy móc.
- Không tạo quá nhiều interface, command, result, mapper hoặc adapter không cần thiết.
- Ưu tiên luồng phát triển dễ hiểu:

```text
Controller
→ Service
→ Repository
→ Database
```

- Chỉ tạo abstraction riêng tại các điểm có giá trị thực tế:
  - Payment provider.
  - File storage provider.
  - Coupon calculator.
  - Enrollment/Lesson access policy.
  - Exam grader.
  - Course workflow.
  - Attempt snapshot.
- Phù hợp với nhóm hai người phát triển trong ba tháng.
- Có thể mở rộng sau MVP mà không cần tái cấu trúc toàn bộ dự án.

---

# 2. Cấu trúc tổng thể repository

```text
lms-platform/
├── backend/
├── frontend/
├── infrastructure/
├── docs/
├── scripts/
├── .github/
├── .env.example
├── docker-compose.yml
├── README.md
└── .gitignore
```

## Ý nghĩa

| Thư mục | Vai trò |
|---|---|
| `backend` | Spring Boot REST API |
| `frontend` | React + Vite + TypeScript |
| `infrastructure` | Nginx và cấu hình triển khai |
| `docs` | BRD, ERD, tài liệu kỹ thuật và API |
| `scripts` | Script phát triển, seed và deploy |
| `.github` | CI/CD và pull request template |

---

# 3. Cấu trúc backend tổng quát

```text
backend/
├── pom.xml
├── Dockerfile
├── mvnw
├── mvnw.cmd
├── .mvn/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/lms/
    │   │       ├── LmsApplication.java
    │   │       ├── identity/
    │   │       ├── course/
    │   │       ├── commerce/
    │   │       ├── payment/
    │   │       ├── learning/
    │   │       ├── exam/
    │   │       ├── storage/
    │   │       ├── reporting/
    │   │       ├── audit/
    │   │       ├── security/
    │   │       ├── shared/
    │   │       └── config/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-test.yml
    │       ├── application-prod.yml
    │       ├── db/
    │       │   └── migration/
    │       └── templates/
    │           └── email/
    └── test/
        ├── java/
        │   └── com/example/lms/
        └── resources/
            └── application-test.yml
```

Không tạo `static/` nếu frontend được triển khai riêng bằng React.

---

# 4. Quy tắc cấu trúc backend

Mỗi module không bắt buộc phải có đầy đủ mọi thư mục.

Cấu trúc mặc định:

```text
module/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── enums/
└── exception/
```

Chỉ tạo thêm khi có nhu cầu:

```text
provider/
policy/
calculator/
grader/
scheduler/
mapper/
event/
```

## Không áp dụng mặc định

Không tạo đồng loạt:

```text
application/
domain/
infrastructure/
command/
result/
port/
adapter/
```

Chỉ dùng nếu một module thực sự cần tách biệt phức tạp sau MVP.

---

# 5. Module Identity

Module này quản lý:

- Authentication.
- User profile.
- Role.
- Refresh token.
- Password reset.

```text
identity/
├── controller/
│   ├── AuthController.java
│   ├── ProfileController.java
│   └── AdminUserController.java
│
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── RoleService.java
│   └── PasswordResetService.java
│
├── repository/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── RefreshTokenRepository.java
│   └── PasswordResetTokenRepository.java
│
├── entity/
│   ├── User.java
│   ├── Role.java
│   ├── UserRole.java
│   ├── RefreshToken.java
│   └── PasswordResetToken.java
│
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── ForgotPasswordRequest.java
│   │   ├── ResetPasswordRequest.java
│   │   └── AssignRoleRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── UserProfileResponse.java
│       └── UserResponse.java
│
├── enums/
│   ├── UserStatus.java
│   └── RoleCode.java
│
├── exception/
│   ├── EmailAlreadyExistsException.java
│   ├── InvalidCredentialsException.java
│   ├── UserBlockedException.java
│   └── InvalidResetTokenException.java
│
└── token/
    └── SecureTokenGenerator.java
```

## Ghi chú

- `UserRepository` có thể trực tiếp kế thừa `JpaRepository`.
- Không cần tạo domain repository và JPA adapter riêng.
- Không cần `RegisterCommand` hoặc `AuthResult` nếu DTO hiện tại đã đủ.
- Mapping đơn giản có thể dùng static factory trong response DTO.

---

# 6. Module Course

Course là module lớn nhất, nên chia theo capability bên trong.

```text
course/
├── category/
├── authoring/
├── curriculum/
├── review/
└── shared/
```

## 6.1. Category

```text
course/category/
├── CategoryController.java
├── CategoryService.java
├── CourseCategoryRepository.java
├── CourseCategory.java
├── CategoryRequest.java
├── CategoryResponse.java
└── CategoryNotFoundException.java
```

Không cần chia tiếp thành `controller`, `service`, `repository` nếu số file còn ít.

## 6.2. Course authoring

```text
course/authoring/
├── controller/
│   ├── PublicCourseController.java
│   ├── TeacherCourseController.java
│   └── AdminCourseController.java
│
├── service/
│   ├── CourseService.java
│   ├── CourseQueryService.java
│   └── CourseTeacherService.java
│
├── repository/
│   ├── CourseRepository.java
│   └── CourseTeacherRepository.java
│
├── entity/
│   ├── Course.java
│   └── CourseTeacher.java
│
├── dto/
│   ├── CreateCourseRequest.java
│   ├── UpdateCourseRequest.java
│   ├── CourseSummaryResponse.java
│   └── CourseDetailResponse.java
│
└── exception/
    ├── CourseNotFoundException.java
    ├── CourseAccessDeniedException.java
    └── InvalidCourseStateException.java
```

## 6.3. Curriculum

```text
course/curriculum/
├── controller/
│   ├── SectionController.java
│   └── LessonController.java
│
├── service/
│   ├── SectionService.java
│   ├── LessonService.java
│   └── LessonPrerequisiteService.java
│
├── repository/
│   ├── CourseSectionRepository.java
│   ├── LessonRepository.java
│   ├── LessonPrerequisiteRepository.java
│   └── LessonResourceRepository.java
│
├── entity/
│   ├── CourseSection.java
│   ├── Lesson.java
│   ├── LessonPrerequisite.java
│   └── LessonResource.java
│
├── dto/
│   ├── CreateSectionRequest.java
│   ├── UpdateSectionRequest.java
│   ├── CreateLessonRequest.java
│   ├── UpdateLessonRequest.java
│   ├── CurriculumResponse.java
│   └── LessonResponse.java
│
├── enums/
│   ├── LessonType.java
│   ├── CompletionMethod.java
│   └── ResourceType.java
│
└── exception/
    ├── LessonNotFoundException.java
    ├── SectionNotFoundException.java
    └── InvalidPrerequisiteException.java
```

## 6.4. Review workflow

```text
course/review/
├── CourseReviewController.java
├── CourseWorkflowService.java
├── CourseReviewRequestRepository.java
├── CourseReviewRequest.java
├── CourseReviewStatus.java
├── SubmitReviewRequest.java
├── ReviewCourseRequest.java
└── CourseReviewResponse.java
```

`CourseWorkflowService` chịu trách nhiệm chuyển trạng thái:

```text
DRAFT
→ PENDING_REVIEW
→ PUBLISHED hoặc REJECTED
→ HIDDEN
→ ARCHIVED
```

Không đặt toàn bộ state transition vào CRUD `CourseService`.

## 6.5. Course shared

```text
course/shared/
├── CourseStatus.java
├── CompletionRule.java
└── CourseManagementPolicy.java
```

`CourseManagementPolicy` chỉ nên tồn tại nếu logic ownership được dùng tại nhiều nơi.

---

# 7. Module Storage

Storage chỉ xử lý kỹ thuật lưu file.

```text
storage/
├── controller/
│   └── FileUploadController.java
│
├── service/
│   └── FileStorageService.java
│
├── repository/
│   └── FileResourceRepository.java
│
├── entity/
│   └── FileResource.java
│
├── provider/
│   ├── StorageProvider.java
│   ├── MinioStorageProvider.java
│   └── StorageProperties.java
│
├── dto/
│   ├── FileUploadResponse.java
│   └── PresignedUrlResponse.java
│
├── enums/
│   ├── FileStatus.java
│   └── StorageProviderType.java
│
└── exception/
    ├── FileNotFoundException.java
    ├── InvalidFileTypeException.java
    └── FileStorageException.java
```

## Nguyên tắc

Storage không tự quyết định Student có được tải nội dung Course hay không.

Luồng tải nội dung private:

```text
LearningController
→ LessonAccessService
→ FileStorageService
→ StorageProvider
```

Không nên cung cấp endpoint download chung mà bỏ qua quyền Course/Enrollment.

---

# 8. Module Commerce

Commerce quản lý:

- Cart.
- Coupon.
- Order.
- Checkout.
- Order expiry.

```text
commerce/
├── controller/
│   ├── CartController.java
│   ├── OrderController.java
│   ├── AdminOrderController.java
│   └── AdminCouponController.java
│
├── service/
│   ├── CartService.java
│   ├── CheckoutService.java
│   ├── OrderService.java
│   ├── CouponService.java
│   └── CouponValidationService.java
│
├── repository/
│   ├── CartRepository.java
│   ├── CouponRepository.java
│   ├── CouponRedemptionRepository.java
│   └── OrderRepository.java
│
├── entity/
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Coupon.java
│   ├── CouponCourse.java
│   ├── CouponRedemption.java
│   ├── Order.java
│   └── OrderItem.java
│
├── calculator/
│   ├── OrderPriceCalculator.java
│   └── CouponDiscountCalculator.java
│
├── dto/
│   ├── request/
│   │   ├── AddCartItemRequest.java
│   │   ├── CheckoutRequest.java
│   │   ├── CreateCouponRequest.java
│   │   └── CancelOrderRequest.java
│   └── response/
│       ├── CartResponse.java
│       ├── CheckoutResponse.java
│       └── OrderResponse.java
│
├── enums/
│   ├── OrderStatus.java
│   ├── CouponStatus.java
│   ├── CouponScope.java
│   └── DiscountType.java
│
├── scheduler/
│   └── ExpiredOrderScheduler.java
│
└── exception/
    ├── CartEmptyException.java
    ├── CourseAlreadyOwnedException.java
    ├── InvalidCouponException.java
    └── InvalidOrderStateException.java
```

## Transaction checkout

```text
Validate Cart
→ Validate Course
→ Validate Enrollment hiện tại
→ Validate Coupon
→ Create Order
→ Create OrderItem snapshot
→ Create Payment
```

`CheckoutService` là nơi điều phối transaction này.

---

# 9. Module Payment

Payment quản lý:

- Payment.
- QR.
- Webhook.
- Idempotency.
- Reconciliation.

```text
payment/
├── controller/
│   ├── PaymentController.java
│   ├── PaymentWebhookController.java
│   └── AdminReconciliationController.java
│
├── service/
│   ├── PaymentService.java
│   ├── PaymentWebhookService.java
│   ├── PaymentCompletionService.java
│   └── PaymentReconciliationService.java
│
├── repository/
│   ├── PaymentRepository.java
│   ├── PaymentWebhookLogRepository.java
│   └── PaymentReconciliationActionRepository.java
│
├── entity/
│   ├── Payment.java
│   ├── PaymentWebhookLog.java
│   └── PaymentReconciliationAction.java
│
├── provider/
│   ├── PaymentProvider.java
│   ├── SepayPaymentProvider.java
│   ├── PaymentWebhookData.java
│   ├── PaymentQrData.java
│   └── SepayProperties.java
│
├── dto/
│   ├── SepayWebhookRequest.java
│   ├── PaymentResponse.java
│   ├── ReconciliationRequest.java
│   └── ReconciliationResponse.java
│
├── enums/
│   ├── PaymentStatus.java
│   ├── PaymentProviderType.java
│   ├── PaymentMethod.java
│   ├── WebhookProcessingStatus.java
│   └── ReconciliationActionType.java
│
└── exception/
    ├── InvalidWebhookException.java
    ├── DuplicateWebhookException.java
    ├── PaymentAmountMismatchException.java
    └── PaymentNotFoundException.java
```

## Payment provider abstraction

```java
public interface PaymentProvider {

    PaymentQrData createQr(Payment payment);

    boolean verifyWebhook(
        Map<String, String> headers,
        String rawBody
    );

    PaymentWebhookData parseWebhook(String rawBody);
}
```

## Transaction payment completion

`PaymentCompletionService` được phép gọi public service của module khác:

```text
PaymentCompletionService
→ OrderService.markPaid()
→ EnrollmentService.grantFromOrder()
→ CouponService.redeemForOrder()
→ AuditLogService.record()
```

Không cần tạo port/adapter cho mọi lời gọi trong MVP.

---

# 10. Module Learning

Learning quản lý:

- Enrollment.
- Quyền học.
- Lesson progress.
- Drip content.
- Prerequisite.
- Course completion.
- Progress override.

```text
learning/
├── controller/
│   ├── MyCourseController.java
│   ├── LearningController.java
│   ├── ProgressController.java
│   ├── TeacherProgressController.java
│   └── AdminEnrollmentController.java
│
├── service/
│   ├── EnrollmentService.java
│   ├── LessonAccessService.java
│   ├── LessonProgressService.java
│   ├── CourseProgressService.java
│   ├── CourseCompletionService.java
│   └── ProgressOverrideService.java
│
├── policy/
│   ├── EnrollmentAccessPolicy.java
│   ├── LessonReleasePolicy.java
│   ├── LessonCompletionPolicy.java
│   └── CourseCompletionPolicy.java
│
├── repository/
│   ├── EnrollmentRepository.java
│   ├── LessonProgressRepository.java
│   ├── ProgressOverrideLogRepository.java
│   └── CourseCompletionRepository.java
│
├── entity/
│   ├── Enrollment.java
│   ├── LessonProgress.java
│   ├── ProgressOverrideLog.java
│   └── CourseCompletion.java
│
├── dto/
│   ├── request/
│   │   ├── LessonHeartbeatRequest.java
│   │   ├── CompleteLessonRequest.java
│   │   ├── GrantEnrollmentRequest.java
│   │   └── OverrideProgressRequest.java
│   └── response/
│       ├── MyCourseResponse.java
│       ├── LearningLessonResponse.java
│       ├── CourseProgressResponse.java
│       └── EnrollmentResponse.java
│
├── enums/
│   ├── EnrollmentSource.java
│   ├── EnrollmentStatus.java
│   ├── LessonProgressStatus.java
│   └── CompletionSource.java
│
├── scheduler/
│   └── EnrollmentExpirationScheduler.java
│
└── exception/
    ├── EnrollmentRequiredException.java
    ├── EnrollmentExpiredException.java
    ├── LessonLockedException.java
    └── LessonNotReleasedException.java
```

## Lưu ý scheduler

Không phải trạng thái theo thời gian nào cũng bắt buộc có scheduler.

- Enrollment access luôn phải kiểm tra `expiresAt` lúc request.
- Scheduler chỉ giúp cập nhật trạng thái để báo cáo dễ hơn.
- Backend không được phụ thuộc hoàn toàn vào scheduler để chặn quyền truy cập.

---

# 11. Module Exam

Exam quản lý:

- Exam builder.
- Question bank.
- Attempt.
- Autosave.
- Snapshot.
- Grading.
- Result.

```text
exam/
├── controller/
│   ├── TeacherExamController.java
│   ├── TeacherQuestionController.java
│   ├── ExamAttemptController.java
│   └── TeacherExamReportController.java
│
├── service/
│   ├── ExamService.java
│   ├── QuestionService.java
│   ├── ExamAttemptService.java
│   ├── AttemptSnapshotService.java
│   ├── AttemptAnswerService.java
│   ├── ExamGradingService.java
│   └── ExamResultService.java
│
├── grader/
│   ├── QuestionGrader.java
│   ├── SingleChoiceGrader.java
│   ├── MultipleChoiceGrader.java
│   └── GradingResult.java
│
├── repository/
│   ├── ExamRepository.java
│   ├── QuestionRepository.java
│   ├── ExamAttemptRepository.java
│   └── AttemptAnswerRepository.java
│
├── entity/
│   ├── Exam.java
│   ├── Question.java
│   ├── QuestionOption.java
│   ├── ExamQuestion.java
│   ├── ExamAttempt.java
│   ├── AttemptQuestion.java
│   ├── AttemptAnswer.java
│   └── AttemptAnswerOption.java
│
├── dto/
│   ├── request/
│   │   ├── CreateExamRequest.java
│   │   ├── CreateQuestionRequest.java
│   │   ├── SaveAnswerRequest.java
│   │   └── SubmitAttemptRequest.java
│   └── response/
│       ├── ExamResponse.java
│       ├── AttemptResponse.java
│       ├── AttemptQuestionResponse.java
│       └── ExamResultResponse.java
│
├── enums/
│   ├── ExamStatus.java
│   ├── QuestionType.java
│   ├── QuestionDifficulty.java
│   ├── AttemptStatus.java
│   ├── SubmissionType.java
│   └── ResultDisplayMode.java
│
├── event/
│   ├── ExamGradedEvent.java
│   └── ExamGradedEventHandler.java
│
└── exception/
    ├── ExamNotAvailableException.java
    ├── MaximumAttemptsExceededException.java
    ├── AttemptExpiredException.java
    └── AttemptAlreadySubmittedException.java
```

## Strategy chấm điểm

```java
public interface QuestionGrader {

    boolean supports(QuestionType type);

    GradingResult grade(
        AttemptQuestion question,
        Set<Long> selectedOptionIds
    );
}
```

Strategy Pattern có giá trị ở đây vì hệ thống có nhiều loại Question và có khả năng mở rộng.

## Event Exam → Learning

Sau khi chấm bài:

```text
ExamGradedEvent
→ Learning handler
→ Cập nhật LessonProgress
→ Tính lại CourseProgress
```

Trong MVP có thể dùng Spring application event đồng bộ.

---

# 12. Module Reporting

Bắt đầu với cấu trúc nhỏ.

```text
reporting/
├── AdminDashboardController.java
├── TeacherReportController.java
├── AdminDashboardService.java
├── TeacherCourseReportService.java
├── DashboardQueryRepository.java
├── ReportExportService.java
├── projection/
│   ├── RevenueByDateProjection.java
│   ├── TopCourseProjection.java
│   └── CourseCompletionProjection.java
└── dto/
    ├── AdminDashboardResponse.java
    ├── TeacherCourseReportResponse.java
    └── RevenueChartResponse.java
```

Chỉ tách thành nhiều repository khi số lượng query báo cáo thực sự tăng.

---

# 13. Module Audit

Không dùng AOP tổng quát trong MVP.

```text
audit/
├── AdminAuditController.java
├── AuditLogService.java
├── AuditLogRepository.java
├── AuditLog.java
├── AuditAction.java
└── AuditLogResponse.java
```

Service nghiệp vụ gọi rõ ràng:

```java
auditLogService.recordCourseApproved(...);
auditLogService.recordPaymentReconciled(...);
auditLogService.recordEnrollmentRevoked(...);
```

Cách này dễ hiểu và giữ được đầy đủ ngữ cảnh nghiệp vụ.

---

# 14. Security

```text
security/
├── config/
│   └── SecurityConfig.java
├── jwt/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthenticationEntryPoint.java
│   └── JwtProperties.java
├── principal/
│   └── AuthenticatedUser.java
├── handler/
│   ├── RestAccessDeniedHandler.java
│   └── RestAuthenticationEntryPoint.java
└── annotation/
    └── CurrentUser.java
```

Không cần tự tạo `@RequireRole` nếu `@PreAuthorize` đã đáp ứng tốt.

---

# 15. Shared và Config

## Shared

```text
shared/
├── exception/
│   ├── ApiException.java
│   ├── ErrorCode.java
│   ├── ErrorResponse.java
│   └── GlobalExceptionHandler.java
├── pagination/
│   └── PageResponse.java
├── persistence/
│   ├── BaseEntity.java
│   └── AuditableEntity.java
└── web/
    └── ApiResponse.java
```

Không tạo `ApplicationConstants`, `ValidationUtils` hoặc `DateTimeUtils` quá lớn.

Logic nghiệp vụ đặt tại module tương ứng:

```text
CouponDiscountCalculator  → commerce
LessonReleasePolicy       → learning
ExamScoreCalculator       → exam
PaymentReferenceGenerator → payment
```

## Config

```text
config/
├── JpaConfig.java
├── CorsConfig.java
├── JacksonConfig.java
├── OpenApiConfig.java
├── SchedulerConfig.java
└── ClockConfig.java
```

Nên inject `Clock` để test expiry dễ hơn.

---

# 16. Flyway migration

```text
backend/src/main/resources/db/migration/
├── V001__create_identity_tables.sql
├── V002__seed_roles.sql
├── V003__create_file_resource_table.sql
├── V004__create_course_tables.sql
├── V005__create_course_review_tables.sql
├── V006__create_cart_and_coupon_tables.sql
├── V007__create_order_tables.sql
├── V008__create_payment_tables.sql
├── V009__create_enrollment_tables.sql
├── V010__create_learning_progress_tables.sql
├── V011__create_exam_tables.sql
├── V012__create_audit_table.sql
├── V013__create_indexes.sql
└── V014__seed_demo_data.sql
```

## Quy tắc

- Chia migration theo module hoặc sprint.
- Không tạo một migration duy nhất cho 37 bảng.
- Không tạo mỗi bảng thành một migration nếu các bảng thuộc cùng một feature.
- Không sửa migration đã chạy trên môi trường dùng chung; tạo migration mới.

---

# 17. Cấu trúc test backend

Bắt đầu đơn giản theo module.

```text
backend/src/test/java/com/example/lms/
├── identity/
├── course/
├── commerce/
├── payment/
├── learning/
├── exam/
└── support/
    ├── IntegrationTestBase.java
    ├── TestContainerConfig.java
    └── TestDataFactory.java
```

Ví dụ tên test:

```text
CouponValidationServiceTest.java
CheckoutIntegrationTest.java
DuplicateWebhookIntegrationTest.java
PaymentAmountMismatchIntegrationTest.java
CourseOwnershipAuthorizationTest.java
LessonPrerequisiteIntegrationTest.java
ExpiredAttemptIntegrationTest.java
QuestionSnapshotIntegrationTest.java
```

Không cần chia ngay thành `unit`, `integration`, `architecture`, `e2e` nếu số test còn ít.

---

# 18. Cấu trúc frontend tổng quát

```text
frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── eslint.config.js
├── Dockerfile
├── public/
└── src/
    ├── app/
    ├── features/
    ├── components/
    ├── layouts/
    ├── lib/
    ├── hooks/
    ├── stores/
    ├── utils/
    ├── assets/
    ├── styles/
    ├── main.tsx
    └── vite-env.d.ts
```

---

# 19. App frontend

```text
src/app/
├── App.tsx
├── router/
│   ├── router.tsx
│   ├── routes.ts
│   ├── ProtectedRoute.tsx
│   └── RoleRoute.tsx
├── providers/
│   ├── AppProviders.tsx
│   ├── QueryProvider.tsx
│   └── AuthInitializer.tsx
└── config/
    ├── env.ts
    └── query-client.ts
```

---

# 20. Features frontend

```text
src/features/
├── auth/
├── account/
├── courses/
│   ├── catalog/
│   ├── authoring/
│   └── review/
├── commerce/
│   ├── cart/
│   ├── checkout/
│   ├── orders/
│   └── payment/
├── learning/
├── exams/
└── admin/
    ├── users/
    ├── courses/
    ├── payments/
    ├── reports/
    └── audit/
```

## Nguyên tắc

- Bắt đầu feature với cấu trúc phẳng.
- Chỉ tạo thư mục con khi có ít nhất khoảng 3–5 file cùng trách nhiệm.
- Không bắt buộc mọi feature phải có `api`, `hooks`, `pages`, `components`, `types`, `utils`.

Ví dụ feature Auth ban đầu:

```text
features/auth/
├── LoginPage.tsx
├── RegisterPage.tsx
├── LoginForm.tsx
├── RegisterForm.tsx
├── auth.api.ts
├── auth.hooks.ts
├── auth.schema.ts
└── auth.types.ts
```

Khi file tăng mới tách:

```text
features/auth/
├── api/
├── components/
├── hooks/
├── pages/
├── schemas/
└── types/
```

---

# 21. Feature Courses frontend

```text
features/courses/
├── catalog/
│   ├── CourseCatalogPage.tsx
│   ├── CourseDetailPage.tsx
│   ├── CourseCard.tsx
│   ├── course-catalog.api.ts
│   ├── course-catalog.hooks.ts
│   └── course-catalog.types.ts
│
├── authoring/
│   ├── TeacherCoursesPage.tsx
│   ├── CreateCoursePage.tsx
│   ├── EditCoursePage.tsx
│   ├── CurriculumPage.tsx
│   ├── CourseForm.tsx
│   ├── CurriculumEditor.tsx
│   ├── course-authoring.api.ts
│   ├── course-authoring.hooks.ts
│   └── course-authoring.types.ts
│
└── review/
    ├── CourseReviewPage.tsx
    ├── ReviewCourseDialog.tsx
    ├── course-review.api.ts
    └── course-review.hooks.ts
```

---

# 22. Feature Commerce frontend

```text
features/commerce/
├── cart/
│   ├── CartPage.tsx
│   ├── CartItem.tsx
│   ├── cart.api.ts
│   └── cart.hooks.ts
│
├── checkout/
│   ├── CheckoutPage.tsx
│   ├── CouponInput.tsx
│   ├── OrderSummary.tsx
│   ├── checkout.api.ts
│   └── checkout.hooks.ts
│
├── orders/
│   ├── OrderHistoryPage.tsx
│   ├── OrderDetailPage.tsx
│   ├── orders.api.ts
│   └── orders.hooks.ts
│
└── payment/
    ├── PaymentPage.tsx
    ├── PaymentQr.tsx
    ├── PaymentStatus.tsx
    ├── payment.api.ts
    └── payment.hooks.ts
```

---

# 23. Feature Learning frontend

```text
features/learning/
├── MyCoursesPage.tsx
├── LearningPage.tsx
├── CourseSidebar.tsx
├── SectionAccordion.tsx
├── VideoLesson.tsx
├── TextLesson.tsx
├── DocumentLesson.tsx
├── LessonLockedState.tsx
├── CourseProgressBar.tsx
├── learning.api.ts
├── learning.hooks.ts
├── useVideoHeartbeat.ts
└── learning.types.ts
```

Frontend gửi heartbeat, nhưng backend vẫn quyết định tính hợp lệ.

---

# 24. Feature Exam frontend

```text
features/exams/
├── ExamStartPage.tsx
├── ExamAttemptPage.tsx
├── ExamResultPage.tsx
├── ExamTimer.tsx
├── QuestionNavigator.tsx
├── SingleChoiceQuestion.tsx
├── MultipleChoiceQuestion.tsx
├── ExamSubmitDialog.tsx
├── exams.api.ts
├── exams.hooks.ts
├── useExamTimer.ts
└── exams.types.ts
```

Zustand chỉ lưu UI state:

```text
currentQuestionIndex
questionNavigatorOpen
localSavingState
```

Đáp án chính được autosave ở backend.

---

# 25. Shared frontend

## Components

```text
components/
├── ui/
│   ├── Button.tsx
│   ├── Input.tsx
│   ├── Select.tsx
│   ├── Dialog.tsx
│   ├── Table.tsx
│   ├── Pagination.tsx
│   ├── Badge.tsx
│   └── Spinner.tsx
├── feedback/
│   ├── PageLoader.tsx
│   ├── ErrorState.tsx
│   ├── EmptyState.tsx
│   └── UnauthorizedState.tsx
└── navigation/
    ├── Breadcrumb.tsx
    └── PageHeader.tsx
```

## Layouts

```text
layouts/
├── PublicLayout.tsx
├── StudentLayout.tsx
├── TeacherLayout.tsx
└── AdminLayout.tsx
```

## Lib

```text
lib/
├── http/
│   ├── axios-instance.ts
│   ├── auth-interceptor.ts
│   └── api-error.ts
└── query/
    └── query-client.ts
```

## Stores

```text
stores/
├── auth.store.ts
└── ui.store.ts
```

Không đưa server state vào Zustand:

```text
Course
Order
Payment
Enrollment
ExamAttempt
```

Các dữ liệu trên sử dụng TanStack Query.

## Hooks toàn cục

Chỉ chứa hook thực sự dùng chung:

```text
hooks/
├── useDebounce.ts
├── useDisclosure.ts
└── useMediaQuery.ts
```

Hook nghiệp vụ nằm trong feature tương ứng.

---

# 26. Infrastructure

```text
infrastructure/
└── nginx/
    ├── nginx.conf
    └── conf.d/
        └── lms.conf
```

Dockerfile đặt ngay trong từng ứng dụng:

```text
backend/Dockerfile
frontend/Dockerfile
```

Không tạo thêm Dockerfile trùng trong `infrastructure/docker`.

`docker-compose.yml` ở root:

```text
services:
├── mysql
├── minio
├── backend
├── frontend
└── nginx
```

Trong local có thể chỉ chạy MySQL và MinIO bằng Docker.

---

# 27. Docs

```text
docs/
├── requirements/
│   ├── BRD.md
│   └── MVP-business-specification.md
├── architecture/
│   ├── tech-stack.md
│   ├── folder-structure.md
│   └── module-dependencies.md
├── database/
│   ├── ERD.md
│   └── migration-plan.md
├── api/
│   ├── api-conventions.md
│   ├── error-codes.md
│   └── payment-webhook.md
├── testing/
│   ├── test-plan.md
│   └── acceptance-test-cases.md
└── presentation/
    ├── demo-scenarios.md
    └── defense-questions.md
```

---

# 28. Scripts

```text
scripts/
├── dev/
│   ├── start-local.ps1
│   └── stop-local.ps1
├── database/
│   ├── backup.ps1
│   ├── restore.ps1
│   └── reset-local.ps1
├── seed/
│   └── seed-demo-data.ps1
└── deploy/
    └── deploy.sh
```

Chỉ tạo script khi thật sự sử dụng; không tạo thư mục rỗng.

---

# 29. Quy tắc phụ thuộc module

```text
identity
    không phụ thuộc module nghiệp vụ khác

course
    có thể đọc identity để kiểm tra Teacher/User

commerce
    dùng course để lấy giá và trạng thái Course
    dùng learning để kiểm tra Course đã sở hữu

payment
    điều phối commerce và learning khi hoàn tất thanh toán

learning
    dùng course để kiểm tra Course, Section và Lesson

exam
    dùng course để kiểm tra Lesson
    dùng learning để kiểm tra Enrollment

reporting
    chỉ đọc dữ liệu từ nhiều module
    không thay đổi nghiệp vụ

audit
    được service nghiệp vụ gọi để ghi lịch sử

storage
    chỉ xử lý file kỹ thuật
    quyền truy cập do course hoặc learning quyết định
```

## Tránh phụ thuộc vòng

Không để:

```text
course → exam → learning → course
```

Đề xuất:

- Course không gọi Exam.
- Exam tham chiếu `lessonId`.
- Exam phát `ExamGradedEvent`.
- Learning nhận event và cập nhật progress.
- Learning gọi Course repository/service chỉ để đọc cấu trúc Lesson.

---

# 30. Quy tắc tránh overengineering

## Không tạo interface cho mọi service

Không cần:

```text
CourseService
CourseServiceImpl
```

Nếu chỉ có một implementation.

Chỉ cần:

```text
CourseService
```

Interface chỉ tạo khi:

- Có nhiều implementation.
- Cần provider abstraction.
- Cần boundary rõ ràng với hệ thống ngoài.
- Cần fake implementation đặc biệt.

## Không tạo mapper cho mọi entity

Mapping đơn giản:

```java
public record CategoryResponse(
    Long id,
    String name
) {
    public static CategoryResponse from(CourseCategory category) {
        return new CategoryResponse(
            category.getId(),
            category.getName()
        );
    }
}
```

Chỉ tạo mapper riêng khi mapping phức tạp hoặc tái sử dụng nhiều.

## Không tạo command/result cho mọi API

Request DTO có thể được truyền trực tiếp vào service trong MVP.

Chỉ tạo command cho use case phức tạp:

```text
CheckoutCommand
ProcessPaymentWebhookCommand
ReconcilePaymentCommand
SubmitExamAttemptCommand
```

## Không tạo thư mục con quá sớm

Bắt đầu phẳng, tách khi số file tăng.

## Không tạo module theo từng bảng

Ví dụ không tạo:

```text
cart-item/
order-item/
question-option/
```

Các bảng này thuộc module nghiệp vụ lớn hơn.

---

# 31. Cấu trúc tối thiểu khi khởi tạo dự án

Không cần tạo toàn bộ cây thư mục ngay ngày đầu.

Backend ban đầu:

```text
backend/src/main/java/com/example/lms/
├── identity/
├── course/
├── commerce/
├── payment/
├── learning/
├── exam/
├── storage/
├── security/
├── shared/
└── config/
```

Frontend ban đầu:

```text
frontend/src/
├── app/
├── features/
├── components/
├── layouts/
├── lib/
├── stores/
├── hooks/
├── utils/
├── assets/
└── styles/
```

Tạo thư mục và class theo từng sprint.

---

# 32. Kết luận

Cấu trúc này giữ được các ưu điểm của Modular Monolith:

- Chia theo module nghiệp vụ.
- Dễ xác định ownership của code.
- Dễ chia việc cho hai lập trình viên.
- Có thể mở rộng sau MVP.
- Hạn chế phụ thuộc trực tiếp giữa các module.

Đồng thời tránh overengineering bằng cách:

- Không áp dụng Clean Architecture đầy đủ cho mọi module.
- Không tách domain entity và JPA entity.
- Không tạo repository port và adapter đại trà.
- Không tạo interface cho mọi service.
- Không dùng command/result cho toàn bộ CRUD.
- Không bắt buộc mọi feature frontend có nhiều tầng thư mục.
- Chỉ dùng abstraction ở những điểm thực sự phức tạp.

Luồng mặc định vẫn là:

```text
Controller
→ Service
→ Repository
```

Các cấu trúc nâng cao chỉ được dùng tại:

```text
CourseWorkflowService
PaymentProvider
StorageProvider
PaymentCompletionService
CouponDiscountCalculator
EnrollmentAccessPolicy
QuestionGrader
AttemptSnapshotService
ExamGradedEvent
```

Đây là mức cân bằng phù hợp giữa chất lượng kiến trúc và khả năng hoàn thành MVP trong ba tháng.
