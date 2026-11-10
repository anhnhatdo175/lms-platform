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

4. Để mở màn quản trị lần đầu, hãy đăng ký tài khoản qua giao diện, sau đó dùng MySQL Workbench để cấp quyền `ADMIN` cho đúng email trong môi trường local:

   ```sql
   UPDATE users SET role = 'ADMIN' WHERE email = 'email-cua-ban@example.com';
   ```

   Đăng xuất rồi đăng nhập lại để nhận quyền mới. Đây là thao tác bootstrap local qua DB; hệ thống không cung cấp endpoint công khai tự cấp quyền quản trị.

### Chạy bằng Docker Compose (tùy chọn)

Nếu chọn Docker, tạo `.env` từ `.env.example`, điền cấu hình theo mẫu rồi chạy `docker compose up --build`. MySQL trong Compose được publish ra host port `3307` để không xung đột với MySQL Server local trên `3306`. Frontend dùng `/api` qua Nginx tới backend.

Auth API:

* `POST /api/auth/register` tạo tài khoản `STUDENT` và đăng nhập ngay.
* `POST /api/auth/login` đăng nhập bằng email và mật khẩu.
* `POST /api/auth/refresh` xoay vòng refresh token trong cookie HttpOnly.
* `POST /api/auth/logout` thu hồi refresh token hiện tại.
* `GET /api/auth/me` trả về hồ sơ của access token hiện tại.
* `GET/PATCH /api/users/me` xem và cập nhật hồ sơ của chính mình.
* `GET /api/users` tìm kiếm, lọc và phân trang danh sách người dùng (chỉ `ADMIN`).
* `PATCH /api/users/{id}` cập nhật vai trò/trạng thái tài khoản (chỉ `ADMIN`).

Access token có thời hạn ngắn và chỉ được giữ trong bộ nhớ frontend. Refresh token có thời hạn dài hơn, được lưu dạng hash ở database và gửi qua cookie HttpOnly, SameSite=Strict. Khi triển khai HTTPS, đặt `REFRESH_COOKIE_SECURE=true` và cấu hình `CORS_ALLOWED_ORIGINS` thành origin thực tế của frontend.


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
