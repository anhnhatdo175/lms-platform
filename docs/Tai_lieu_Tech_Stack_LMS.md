# TÀI LIỆU LỰA CHỌN TECH STACK

## HỆ THỐNG HỌC TRỰC TUYẾN VÀ KINH DOANH KHÓA HỌC

| **Backend**   | Java + Spring Boot                 |
|---------------|------------------------------------|
| **Frontend**  | React + Vite + TypeScript          |
| **Database**  | MySQL                              |
| **Kiến trúc** | Modular Monolith + REST API        |
| **Phạm vi**   | MVP 3 tháng, nhóm 2 lập trình viên |

Ngày lập: 06/08/2026

# 1. Mục đích tài liệu

Tài liệu này mô tả bộ công nghệ được lựa chọn để xây dựng hệ thống học
trực tuyến kết hợp kinh doanh khóa học. Nội dung tập trung vào vai trò
của từng công nghệ, kiến trúc tổng thể, cách các thành phần phối hợp,
tiêu chuẩn triển khai và lý do lựa chọn trong phạm vi MVP ba tháng.

Hệ thống cần hỗ trợ các luồng nghiệp vụ cốt lõi gồm quản lý tài khoản,
phân quyền, khóa học, chương và bài học, Enrollment, tiến độ học tập,
bài kiểm tra, giỏ hàng, đơn hàng, thanh toán QR, webhook và báo cáo quản
trị. Tech stack phải bảo đảm an toàn, dễ bảo trì, phù hợp với nhóm hai
lập trình viên và có khả năng mở rộng sau MVP.

# 2. Tổng quan Tech Stack

| **Lớp**        | **Công nghệ chính**        | **Vai trò**                               | **Lựa chọn đề xuất**                                        |
|----------------|----------------------------|-------------------------------------------|-------------------------------------------------------------|
| Backend        | Java, Spring Boot          | Xử lý nghiệp vụ, bảo mật, API, thanh toán | Java 21 LTS; Spring Boot 3.x                                |
| Frontend       | React, Vite, TypeScript    | Giao diện học viên, giáo viên và quản trị | React 19; Vite; TypeScript strict                           |
| Database       | MySQL                      | Lưu dữ liệu giao dịch và nghiệp vụ        | MySQL 8.x, InnoDB, UTF8MB4                                  |
| ORM            | Spring Data JPA, Hibernate | Ánh xạ entity và truy vấn dữ liệu         | JPA Repository + JPQL/QueryDSL khi cần                      |
| API            | REST, JSON, OpenAPI        | Giao tiếp frontend-backend                | RESTful API, Swagger/OpenAPI                                |
| Authentication | Spring Security, JWT       | Xác thực và phân quyền                    | Access token + refresh token httpOnly cookie                |
| Migration      | Flyway                     | Quản lý thay đổi schema                   | Migration có version, không dùng ddl-auto update production |
| File storage   | MinIO/S3 compatible        | Lưu video, ảnh, tài liệu                  | Database chỉ lưu metadata và object key                     |
| Build/Deploy   | Maven, Docker, Nginx       | Đóng gói và triển khai                    | Docker Compose cho MVP                                      |

# 3. Kiến trúc tổng thể đề xuất

Kiến trúc phù hợp nhất cho giai đoạn MVP là Modular Monolith. Toàn bộ
backend được triển khai dưới một ứng dụng Spring Boot, nhưng mã nguồn
được chia rõ theo module nghiệp vụ. Cách tiếp cận này đơn giản hơn
microservices, giảm chi phí vận hành và vẫn giữ được ranh giới module để
có thể tách dịch vụ trong tương lai.

## 3.1. Luồng giao tiếp

1.  Người dùng truy cập ứng dụng React thông qua trình duyệt.

2.  React gọi REST API của Spring Boot bằng HTTPS và JSON.

3.  Spring Security xác thực access token và kiểm tra quyền theo vai
    trò.

4.  Các service thực thi nghiệp vụ và sử dụng Spring Data JPA để làm
    việc với MySQL.

5.  Video, ảnh và tài liệu được lưu trên MinIO hoặc dịch vụ
    S3-compatible; MySQL chỉ lưu metadata.

6.  Nhà cung cấp thanh toán gửi webhook trực tiếp đến backend; frontend
    không được tự xác nhận trạng thái PAID.

## 3.2. Sơ đồ kiến trúc logic

```text
Browser
  ↓
React + Vite + TypeScript
  ↓ HTTPS / REST / JSON
Spring Boot API
├── Security & Authentication
├── User / Course / Learning / Exam
├── Cart / Order / Payment / Enrollment
├── File Storage Adapter
└── Reporting
  ↓
MySQL 8.x / MinIO-S3 / Payment Provider
```

# 4. Backend: Java và Spring Boot

## 4.1. Vai trò

Backend là thành phần trung tâm chịu trách nhiệm kiểm tra dữ liệu, thực
thi business rules, xác thực người dùng, phân quyền, xử lý đơn hàng,
nhận webhook thanh toán, tạo Enrollment và bảo vệ nội dung khóa học.
Những dữ liệu nhạy cảm như giá bán, trạng thái thanh toán, đáp án bài
thi và quyền học phải được quyết định ở backend.

## 4.2. Thành phần Spring đề xuất

| **Thành phần**      | **Mục đích**                    | **Áp dụng trong dự án**                                |
|---------------------|---------------------------------|--------------------------------------------------------|
| Spring Web          | Xây dựng REST Controller        | API tài khoản, khóa học, đơn hàng, thanh toán          |
| Spring Security     | Authentication và authorization | JWT, role ADMIN/TEACHER/STUDENT, method security       |
| Spring Data JPA     | Truy cập dữ liệu                | Repository, phân trang, transaction                    |
| Hibernate Validator | Kiểm tra DTO                    | Email, độ dài, giá, dữ liệu bắt buộc                   |
| Spring Transaction  | Đảm bảo tính nguyên tử          | Cập nhật Payment, Order và Enrollment cùng transaction |
| Springdoc OpenAPI   | Sinh tài liệu API               | Swagger UI và API contract với frontend                |
| Flyway              | Migration database              | Tạo bảng, index, constraint và dữ liệu role ban đầu    |
| Spring Actuator     | Theo dõi sức khỏe ứng dụng      | Health check và metric cơ bản                          |

## 4.3. Cấu trúc module backend

- `auth`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `user`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `course`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `learning`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `exam`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `cart`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `order`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `payment`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `enrollment`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `file-storage`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `reporting`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `notification`: module nghiệp vụ hoặc hạ tầng tương ứng.

- `common`: module nghiệp vụ hoặc hạ tầng tương ứng.

Mỗi module nên có các lớp controller, service, repository, entity, dto,
mapper và exception riêng. Không nên để controller truy cập repository
trực tiếp. Các module trao đổi thông qua service interface hoặc
application service thay vì truy cập tùy ý vào nội bộ của nhau.

## 4.4. Quy ước backend

- Sử dụng Java 21 LTS và Spring Boot 3.x.

- Sử dụng Maven để quản lý dependency và build.

- DTO riêng cho request/response; không trả entity JPA trực tiếp ra API.

- Dùng @Transactional cho các luồng ghi dữ liệu nhiều bước.

- Dùng exception handler tập trung với mã lỗi ổn định.

- Phân trang cho danh sách người dùng, khóa học, đơn hàng và giao dịch.

- Không lưu secret trong source code; dùng biến môi trường.

- Viết unit test cho business rules và integration test cho luồng thanh
  toán.

# 5. Frontend: React, Vite và TypeScript

## 5.1. Vai trò

Frontend cung cấp giao diện cho ba nhóm người dùng: học viên, giáo viên
và quản trị viên. Ứng dụng chịu trách nhiệm hiển thị dữ liệu, quản lý
trạng thái giao diện, kiểm tra hợp lệ cơ bản, điều hướng và gọi API.
Frontend không được giữ quyền quyết định đối với giá tiền, quyền truy
cập khóa học, điểm bài thi hoặc trạng thái thanh toán.

## 5.2. Lý do lựa chọn

- React phù hợp với giao diện nhiều màn hình và thành phần tái sử dụng.

- Vite có thời gian khởi động và build nhanh, cấu hình đơn giản cho dự
  án React.

- TypeScript phát hiện lỗi kiểu dữ liệu sớm và giúp đồng bộ API contract
  tốt hơn.

- Hệ sinh thái phong phú cho routing, form, query cache, UI component và
  biểu đồ.

- Phù hợp với việc chia module theo feature cho nhóm hai người.

## 5.3. Thư viện đề xuất

| **Thư viện**             | **Vai trò**       | **Ghi chú sử dụng**                               |
|--------------------------|-------------------|---------------------------------------------------|
| React Router             | Điều hướng        | Public route, protected route, route theo role    |
| TanStack Query           | Server state      | Cache, loading, retry, invalidate query           |
| Axios hoặc Fetch wrapper | HTTP client       | Interceptor gắn token và xử lý refresh            |
| React Hook Form          | Quản lý form      | Form đăng nhập, khóa học, bài thi                 |
| Zod                      | Validation schema | Có thể đồng bộ schema form và TypeScript type     |
| Zustand                  | Client state nhỏ  | Auth state, UI state; không thay thế server cache |
| Tailwind CSS + shadcn/ui | UI component      | Phát triển nhanh, dễ tùy biến                     |
| Sonner                   | Thông báo         | Thông báo thao tác thành công hoặc thất bại       |

## 5.4. Cấu trúc frontend

- app: router, provider và cấu hình toàn cục

- features: auth, courses, learning, exams, cart, orders, admin

- components: UI dùng chung và layout

- api: HTTP client và API function

- hooks: custom hooks dùng chung

- types: kiểu dữ liệu API

- utils: xử lý lỗi, format ngày và tiền

- constants: route, endpoint và enum hiển thị

## 5.5. Quy ước frontend

- Bật TypeScript strict mode.

- Không sử dụng kiểu any trừ trường hợp được giải thích rõ.

- Tách server state sang TanStack Query; không sao chép dữ liệu API vào
  Zustand.

- Mỗi feature sở hữu page, component, hook, API và type liên quan.

- Hiển thị đầy đủ trạng thái loading, empty, error và success.

- Kiểm tra quyền ở giao diện để cải thiện UX nhưng backend vẫn là nơi
  quyết định cuối cùng.

- Không lưu refresh token trong localStorage; sử dụng cookie httpOnly do
  backend thiết lập.

# 6. Database: MySQL

## 6.1. Vai trò

MySQL lưu dữ liệu có cấu trúc và cần tính nhất quán cao như người dùng,
khóa học, Enrollment, tiến độ, bài kiểm tra, đơn hàng, thanh toán và
webhook log. Đây là hệ quản trị cơ sở dữ liệu quan hệ phù hợp với các
ràng buộc khóa ngoại, unique constraint và transaction của hệ thống.

## 6.2. Cấu hình đề xuất

- MySQL 8.x, storage engine InnoDB.

- Character set utf8mb4 và collation phù hợp tiếng Việt.

- Dùng BIGINT cho khóa chính của các bảng nghiệp vụ chính.

- Dùng DECIMAL(12,2) cho tiền; không dùng FLOAT hoặc DOUBLE.

- Lưu thời gian theo UTC tại database và chuyển đổi theo múi giờ ở lớp
  hiển thị.

- Tạo index cho email, slug, order code, payment reference, foreign key
  và các trường lọc phổ biến.

- Dùng soft delete hoặc trạng thái ARCHIVED cho dữ liệu đã phát sinh
  giao dịch.

## 6.3. Nhóm bảng chính

- Người dùng: users, roles, user_roles, refresh_tokens.

- Khóa học: course_categories, courses, course_teachers,
  course_sections, lessons.

- Tài nguyên: file_resources, lesson_resources.

- Học tập: enrollments, lesson_progress.

- Bài kiểm tra: questions, question_options, exams, exam_questions,
  exam_attempts, attempt_answers.

- Thương mại: carts, cart_items, orders, order_items.

- Thanh toán: payments, payment_webhook_logs.

- Hỗ trợ: notifications, audit_logs.

## 6.4. Ràng buộc quan trọng

- UNIQUE(users.email).

- UNIQUE(enrollments.student_id, enrollments.course_id).

- UNIQUE(cart_items.cart_id, cart_items.course_id).

- UNIQUE(orders.code).

- UNIQUE(payments.payment_reference).

- UNIQUE(payments.provider, payments.provider_transaction_id).

- Payment, Order và Enrollment phải được cập nhật trong cùng transaction
  khi thanh toán thành công.

# 7. Bảo mật và xác thực

- Mật khẩu được hash bằng BCrypt hoặc Argon2; không lưu mật khẩu thuần.

- Access token có thời hạn ngắn và được gửi trong Authorization header.

- Refresh token được lưu dạng hash ở database và gửi qua cookie
  httpOnly, secure trong production.

- Backend áp dụng RBAC với các role ADMIN, TEACHER và STUDENT.

- Webhook thanh toán được xác minh API key, chữ ký HMAC hoặc cơ chế của
  nhà cung cấp.

- Các endpoint upload kiểm tra MIME type, dung lượng và quyền người
  dùng.

- CORS chỉ cho phép các origin đã cấu hình.

- Không tin tưởng dữ liệu giá, userId, role hoặc trạng thái thanh toán
  từ frontend.

# 8. Quản lý file và video

Video, ảnh đại diện và tài liệu không nên lưu trực tiếp trong MySQL.
Backend cung cấp lớp FileStorageService để làm việc với MinIO hoặc dịch
vụ S3-compatible. Database lưu các trường như objectKey, originalName,
mimeType, fileSize, uploadedBy và createdAt.

- MVP có thể upload trực tiếp qua backend đối với file nhỏ.

- Đối với video lớn, nên sử dụng presigned URL để trình duyệt upload
  thẳng lên object storage.

- Khi tải nội dung riêng tư, backend phải kiểm tra Enrollment trước khi
  cấp URL tạm thời.

- Không công khai URL cố định của video khóa học trả phí.

# 9. Tích hợp thanh toán

Backend đóng gói tích hợp thông qua interface PaymentProvider. Phiên bản
MVP chỉ cần một provider, ví dụ SePay, nhưng thiết kế interface giúp
thay thế hoặc bổ sung VNPay, MoMo sau này mà không làm thay đổi module
Order và Enrollment.

## 9.1. Luồng xử lý

7.  Backend tạo Order và OrderItem từ giá trong database.

8.  Backend tạo Payment với paymentReference duy nhất và nội dung QR.

9.  Frontend chỉ hiển thị QR và kiểm tra trạng thái đơn hàng.

10. Provider gửi webhook đến backend.

11. Backend xác minh webhook, kiểm tra mã đơn, số tiền và idempotency.

12. Trong một transaction, backend cập nhật Payment, Order và tạo
    Enrollment.

13. Webhook gốc được lưu để phục vụ đối soát và xử lý lỗi.

# 10. Công cụ phát triển và triển khai

| **Hạng mục**     | **Công cụ**                           | **Mục đích**                                |
|------------------|---------------------------------------|---------------------------------------------|
| Source control   | Git + GitHub/GitLab                   | Quản lý phiên bản và pull request           |
| Backend build    | Maven                                 | Build, test và dependency management        |
| Frontend build   | npm hoặc pnpm                         | Quản lý package và build Vite               |
| Container        | Docker                                | Đóng gói backend, frontend, MySQL, MinIO    |
| Reverse proxy    | Nginx                                 | HTTPS, static frontend và proxy API         |
| API docs         | Swagger/OpenAPI                       | Thống nhất contract giữa hai lập trình viên |
| Testing backend  | JUnit 5, Mockito, Testcontainers      | Unit và integration test                    |
| Testing frontend | Vitest, React Testing Library         | Test component và logic                     |
| Code quality     | Checkstyle/Spotless, ESLint, Prettier | Thống nhất coding convention                |

# 11. Môi trường triển khai MVP

- Frontend React được build thành static files và phục vụ qua Nginx.

- Backend Spring Boot chạy dưới dạng Docker container.

- MySQL chạy container riêng hoặc managed database.

- MinIO chạy container riêng cho môi trường đồ án; production có thể
  thay bằng S3/R2.

- Nginx terminate HTTPS và proxy /api đến Spring Boot.

- Biến môi trường chứa database credentials, JWT secret, storage key và
  payment secret.

- Có cơ chế backup database và object storage tối thiểu định kỳ.

# 12. Phân chia công việc theo module cho hai lập trình viên

| **Nhóm module** | **Lập trình viên A**                     | **Lập trình viên B**                    |
|-----------------|------------------------------------------|-----------------------------------------|
| Nền tảng        | Auth, User, Role, Security               | Frontend foundation, routing, shared UI |
| Khóa học        | Course, Section, Lesson, File API        | Course management UI, upload UI         |
| Học tập         | Enrollment, access control, Progress API | My Courses, lesson player, progress UI  |
| Bài kiểm tra    | Exam, Question, grading                  | Exam taking UI, result UI               |
| Thương mại      | Order, Payment, webhook                  | Cart, checkout, payment status UI       |
| Quản trị        | Reporting API, audit                     | Admin dashboard và bảng quản lý         |
| Chung           | Review API contract, integration test    | Review UX, end-to-end flow              |

Nên chia theo feature dọc thay vì một người chỉ làm backend và một người
chỉ làm frontend. Mỗi người chịu trách nhiệm hoàn thiện một module từ
database, API đến giao diện, sau đó review chéo. Cách này giảm phụ thuộc
và phù hợp với đồ án cần thể hiện năng lực backend của cả hai thành
viên.

# 13. Các quyết định kỹ thuật chính

| **Quyết định**     | **Lựa chọn**              | **Lý do**                                   |
|--------------------|---------------------------|---------------------------------------------|
| Kiến trúc backend  | Modular Monolith          | Phù hợp nhóm nhỏ và thời gian ba tháng      |
| Giao tiếp          | REST API                  | Dễ triển khai và tài liệu hóa               |
| ORM                | Spring Data JPA           | Tăng tốc CRUD và hỗ trợ transaction         |
| Database migration | Flyway                    | Schema có lịch sử và triển khai ổn định     |
| Auth               | JWT + refresh cookie      | Phù hợp SPA và bảo mật tốt hơn localStorage |
| File               | Object storage            | Không làm database phình lớn                |
| Thanh toán         | Webhook là nguồn xác nhận | Tránh frontend giả mạo trạng thái PAID      |
| Deployment         | Docker Compose            | Đơn giản, tái lập môi trường nhanh          |

# 14. Kết luận

Bộ công nghệ Java Spring Boot, React + Vite + TypeScript và MySQL phù
hợp với hệ thống LMS kết hợp thương mại điện tử trong phạm vi đồ án ba
tháng. Spring Boot đảm nhiệm các nghiệp vụ phức tạp và yêu cầu bảo mật;
React cung cấp giao diện linh hoạt; MySQL bảo đảm tính toàn vẹn và
transaction cho các luồng Order - Payment - Enrollment.

Để dự án thành công, nhóm nên ưu tiên kiến trúc Modular Monolith, API
contract rõ ràng, migration database, kiểm thử luồng thanh toán và phân
quyền nội dung. Các công nghệ nâng cao như microservices, message
broker, Kubernetes hoặc video transcoding chưa cần đưa vào MVP nếu không
có yêu cầu cụ thể.

# Phụ lục A. Phiên bản tham khảo

- Java: 21 LTS

- Spring Boot: 3.x

- Node.js: 22 LTS

- React: 19.x

- Vite: phiên bản ổn định tương thích React

- TypeScript: 5.x

- MySQL: 8.x

- Maven: 3.9+

- Docker Engine và Docker Compose phiên bản ổn định

# Phụ lục B. Tài liệu đầu vào

- Business Requirements Document - Hệ thống học trực tuyến và kinh doanh
  khóa học, phiên bản 1.0.

- Mô tả thực thể, thuộc tính và mối quan hệ - Hệ thống học trực tuyến và
  kinh doanh khóa học.
