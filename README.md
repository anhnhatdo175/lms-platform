# LMS Platform

Hệ thống học trực tuyến kết hợp kinh doanh khóa học, được xây dựng theo kiến trúc **Modular Monolith**.

## Tech stack

### Backend

* Java 21
* Spring Boot 3.x
* Spring Security
* Spring Data JPA / Hibernate
* Flyway Migration
* Maven

### Frontend

* React
* Vite
* TypeScript
* React Router
* TanStack Query
* Axios
* Zustand

### Database và hạ tầng

* MySQL 8.x
* Docker Compose
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


## Yêu cầu môi trường

* Git 2.x
* Java 21
* Node.js 22 hoặc mới hơn
* npm 10 hoặc mới hơn
* Docker Desktop

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
