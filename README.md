# LMS Platform

Hệ thống học trực tuyến kết hợp kinh doanh khóa học, được xây dựng theo kiến trúc **Modular Monolith**.

## Tech stack

### Backend

* Java 25
* Spring Boot 4.1.x
* Spring Security
* Spring Data JPA / Hibernate
* Flyway Migration
* Maven

### Frontend

* React
* Vite
* TypeScript
* CSS Modules

### Database và hạ tầng

* MySQL 8.x
* MySQL Server local hoặc Docker Compose
* MinIO hoặc dịch vụ S3-compatible cho file, video và tài liệu
* Nginx cho môi trường triển khai

## Phạm vi nghiệp vụ

Hệ thống hỗ trợ ba vai trò chính:

* `ADMIN`
* `TEACHER`
* `STUDENT`

Các nhóm chức năng chính:

* Đăng ký, đăng nhập, refresh token và đăng xuất.
* Quản lý người dùng và phân quyền.
* Quản lý danh mục, khóa học, chương và bài học.
* Quy trình gửi duyệt, duyệt, từ chối và xuất bản khóa học.
* Catalog khóa học công khai.
* Giỏ hàng, coupon, checkout và đơn hàng.
* Thanh toán QR, webhook và đối soát giao dịch.
* Cấp quyền học thông qua Enrollment.
* Theo dõi tiến độ học tập.
* Điều kiện tiên quyết và nội dung mở theo thời gian.
* Bài kiểm tra, lượt làm bài, autosave và chấm điểm.
* Báo cáo quản trị và audit log.

### Chạy local với MySQL Server (không cần Docker)

MySQL Server đang cài trên máy có thể dùng trực tiếp. Docker Desktop chỉ cần thiết nếu bạn chọn chạy cả stack bằng Compose.

1. Mở MySQL Workbench và kết nối tới `localhost:3306`. Tạo database và tài khoản riêng cho ứng dụng bằng SQL dưới đây; thay mật khẩu mẫu bằng mật khẩu do bạn tự chọn:

   ```sql
   CREATE DATABASE IF NOT EXISTS lms_db
     CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   CREATE USER IF NOT EXISTS 'lms_user'@'localhost'
     IDENTIFIED BY 'THAY_BANG_MAT_KHAU_RIENG';
   GRANT ALL PRIVILEGES ON lms_db.* TO 'lms_user'@'localhost';
   ```

   Không dùng tài khoản `root` để chạy ứng dụng. Không gửi mật khẩu MySQL qua chat.

2. Tạo file cấu hình backend riêng trên máy:

   ```powershell
   Copy-Item backend\src\main\resources\application.properties.example `
     backend\src\main\resources\application.properties
   ```

   Mở `application.properties`, thay `YOUR_DB_USERNAME` bằng `lms_user`, thay password mẫu bằng mật khẩu vừa tạo, và tạo `app.jwt.secret-base64` riêng. Có thể tạo JWT secret bằng PowerShell:

   ```powershell
   [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   ```

   File `application.properties` bị Git ignore; không commit mật khẩu hay JWT secret. File `.env` ở thư mục gốc chỉ dành cho Docker Compose, Maven không tự đọc file đó.
   Để dùng ảnh đại diện khi chạy backend riêng, đặt `FILE_STORAGE_ENDPOINT`,
   `FILE_STORAGE_PUBLIC_ENDPOINT`, `FILE_STORAGE_ACCESS_KEY` và
   `FILE_STORAGE_SECRET_KEY` theo MinIO local; bucket mặc định là `lms-files`.
   Không dùng placeholder trong cấu hình mẫu làm mật khẩu thật.

3. Chạy backend và frontend trong hai cửa sổ PowerShell:

   ```powershell
   Set-Location backend
   mvn spring-boot:run
   ```

   ```powershell
   Set-Location frontend
   npm run dev
   ```

   Mở `http://localhost:5173`. Vite chuyển tiếp `/api` tới backend tại `http://localhost:8080`; Flyway tự tạo các bảng cần thiết khi backend khởi động.

4. Để tạo admin local, đặt `LMS_BOOTSTRAP_ADMIN_EMAIL` và `LMS_BOOTSTRAP_ADMIN_PASSWORD` trong môi trường PowerShell trước khi chạy backend. Bootstrap chỉ chạy với profile `dev`; không dùng thông tin đăng nhập mặc định và không đưa mật khẩu vào Git.

5. Để bật khôi phục mật khẩu, cấu hình các biến môi trường SMTP relay trước khi chạy backend:

   ```powershell
   $env:SMTP_ENABLED = "true"
   $env:SPRING_MAIL_HOST = "smtp.example.com"
   $env:SPRING_MAIL_PORT = "587"
   $env:SPRING_MAIL_USERNAME = "your-relay-username"
   $env:SPRING_MAIL_PASSWORD = "your-relay-password"
   $env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = "true"
   $env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = "true"
   $env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED = "true"
   $env:SMTP_FROM = "LMS Platform <no-reply@example.com>"
   $env:PASSWORD_RESET_URL = "http://localhost:5173/"
   ```

   Thay các giá trị ví dụ bằng thông tin relay do nhà cung cấp SMTP cấp; không gửi credentials qua chat hoặc commit vào repository. Khi chạy bằng Maven, đặt các biến `SMTP_*` và `SPRING_MAIL_*` như trên trong PowerShell vì Maven không tự đọc `.env` ở thư mục gốc. Khi chạy Docker Compose, đặt cùng các biến `SMTP_*` trong `.env`; Compose chuyển chúng thành cấu hình Spring. SMTP được tắt mặc định.

### Chạy bằng Docker Compose (tùy chọn)

Nếu chọn Docker, tạo `.env` từ `.env.example`, điền các mật khẩu riêng (bao gồm
`MINIO_ROOT_PASSWORD`) rồi chạy `docker compose up --build`. Backend tự tạo bucket
MinIO khi cần; Nginx proxy URL đã ký qua cùng origin `/lms-files/`, không yêu cầu
CORS từ trình duyệt. `FILE_STORAGE_PUBLIC_ENDPOINT` mặc định là `http://localhost`.
Có thể đặt `LMS_BOOTSTRAP_ADMIN_EMAIL`
và `LMS_BOOTSTRAP_ADMIN_PASSWORD` để tạo admin local trong profile `dev`; các
credentials phải giữ trong `.env` đã được Git ignore. MySQL trong Compose được
publish ra host port `3307` để không xung đột với MySQL Server local trên `3306`.
Frontend gọi `/api/v1` cùng domain qua Nginx tới backend.

Auth API (prefix `/api/v1`):

* `POST /auth/register` tạo tài khoản `STUDENT`; đăng nhập là bước riêng.
* `POST /auth/login`, `/auth/refresh` và `/auth/logout` quản lý phiên đăng nhập; refresh token nằm trong cookie HttpOnly.
* `POST /auth/forgot-password` yêu cầu gửi email khôi phục; phản hồi không tiết lộ email có tài khoản hay không.
* `POST /auth/reset-password` dùng token một lần, hết hạn sau 30 phút.
* `POST /me/password` đổi mật khẩu khi đã đăng nhập; mật khẩu mới tối thiểu 8 ký tự và các refresh session hiện có sẽ bị thu hồi.
* `GET/PATCH /me` xem và cập nhật hồ sơ của chính mình; `avatarResourceId` có thể được gỡ bằng `null`.
* `USER_AVATAR` được tải lên qua các API tài nguyên hiện có; ảnh JPEG, PNG hoặc WebP tối đa 5 MiB. Hồ sơ trả về ID tài nguyên, còn URL xem ảnh có thời hạn ngắn.
* `/admin/users` và `/admin/audit-logs` dành cho `ADMIN`.

Access token có thời hạn ngắn và chỉ được giữ trong bộ nhớ frontend. Refresh token có thời hạn dài hơn, được lưu dạng hash ở database và gửi qua cookie HttpOnly, SameSite=Strict. Khi triển khai HTTPS, đặt `REFRESH_COOKIE_SECURE=true` và cấu hình `CORS_ALLOWED_ORIGINS` thành origin thực tế của frontend. SMTP relay được cấu hình qua `.env` khi dùng Docker Compose; SMTP tắt mặc định cho đến khi `SMTP_ENABLED=true`.


## Yêu cầu môi trường

* Git 2.x
* Java 25
* Node.js 22 hoặc mới hơn
* npm 10 hoặc mới hơn
* MySQL Server 8.x (hoặc Docker Desktop nếu dùng Compose)

Kiểm tra phiên bản:

```bash
git --version
java --version
node --version
npm --version
docker --version
```

## License

Dự án phục vụ mục đích học tập và đồ án.
