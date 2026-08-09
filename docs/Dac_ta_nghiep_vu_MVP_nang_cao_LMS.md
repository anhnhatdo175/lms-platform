# ĐẶC TẢ NGHIỆP VỤ MVP NÂNG CAO  
## Hệ thống học trực tuyến và kinh doanh khóa học

**Phiên bản:** 2.0  
**Ngày lập:** 06/08/2026  
**Thời gian phát triển:** 3 tháng  
**Nguồn lực:** 2 lập trình viên, có AI hỗ trợ  
**Kiến trúc dự kiến:** Modular Monolith  
**Backend:** Java Spring Boot  
**Frontend:** React + Vite + TypeScript  
**Database:** MySQL  

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
- Khóa học có danh mục, giá, mô tả, ảnh đại diện.
- Khóa học có nhiều Section.
- Section có nhiều Lesson.
- Sắp xếp Section và Lesson.
- Lesson hỗ trợ video, text, tài liệu và quiz.
- Lesson có thể là bắt buộc hoặc không bắt buộc.
- Lesson có thể cho xem thử.
- Lesson có thể có điều kiện tiên quyết.
- Section có thể mở theo số ngày sau khi Enrollment được tạo.
- Teacher gửi khóa học đi duyệt.
- Admin duyệt hoặc từ chối.
- Admin xuất bản, ẩn hoặc lưu trữ.
- Lưu lý do từ chối.
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
- AI recommendation.
- Chứng chỉ có blockchain hoặc chữ ký số.
- Chấm bài tự luận bằng AI.
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

### TEACHER

- Tạo Course.
- Quản lý Course do mình tạo hoặc được phân công.
- Quản lý Section, Lesson, Exam và Question.
- Gửi yêu cầu duyệt.
- Xem Enrollment của Course mình phụ trách.
- Xem tiến độ và kết quả thi.
- Override tiến độ nếu được cấp capability.
- Không được xem doanh thu toàn hệ thống.
- Không được tự xuất bản nếu chưa được Admin duyệt.

### STUDENT

- Quản lý hồ sơ.
- Xem catalog.
- Mua khóa học.
- Xem Order và Payment của chính mình.
- Học khóa học có Enrollment hợp lệ.
- Cập nhật tiến độ của chính mình.
- Làm Exam.
- Xem kết quả theo cấu hình.

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
| Gửi duyệt | Có | Có | Không | Không |
| Duyệt Course | Có | Không | Không | Không |
| Xuất bản | Có | Không | Không | Không |
| Xem học viên Course | Có | Có | Không | Không |
| Xem toàn bộ doanh thu | Có | Không | Không | Không |
| Mua Course | Có thể | Có thể | Có thể | Có |
| Học Course có Enrollment | Có | Có | Có | Có |
| Xử lý Payment | Có | Không | Không | Không |

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
PENDING_REVIEW → PUBLISHED
PENDING_REVIEW → REJECTED
REJECTED → DRAFT
PUBLISHED → HIDDEN
HIDDEN → PUBLISHED
PUBLISHED → ARCHIVED
HIDDEN → ARCHIVED
```

## 8.3. Quy tắc

- Course mới luôn ở `DRAFT`.
- Teacher chỉ gửi duyệt khi Course có ít nhất một Section, một Lesson bắt buộc, giá hợp lệ và ảnh đại diện.
- Khi `PENDING_REVIEW`, Teacher không được thay đổi curriculum trừ khi rút yêu cầu.
- Admin từ chối phải nhập lý do.
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

Để giữ phạm vi khả thi, MVP chỉ cho phép tối đa một prerequisite trực tiếp trên mỗi Lesson.

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
- Lượt sử dụng chỉ được ghi nhận khi Order được thanh toán thành công hoặc theo chính sách đã chốt.
- MVP chọn chính sách: **chỉ tăng usedCount khi Payment thành công**.
- Khi Order hết hạn, quota tạm giữ được giải phóng nếu có cơ chế reservation.
- Để giảm phức tạp, MVP không reservation quota; Coupon được kiểm tra lại khi xử lý Payment.
- Nếu Coupon hết lượt trước khi Payment hoàn tất nhưng Order đã được tạo, Order vẫn giữ giá đã chốt.

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
11. Backend đối chiếu amount.
12. Trong transaction:
    - Payment → `PAID`
    - Order → `PAID`
    - Tạo Enrollment cho từng OrderItem
    - Ghi Coupon redemption
13. WebhookLog → `PROCESSED`.
14. Student nhìn thấy trạng thái mới qua polling.

## 11.4. Idempotency

Một webhook gửi lại không được:

- Tạo thêm Enrollment.
- Cộng Coupon usage nhiều lần.
- Cập nhật doanh thu nhiều lần.
- Tạo thêm Payment.

Khóa idempotency ưu tiên:

```text
(provider, providerEventId)
```

Nếu provider không có event ID ổn định:

```text
(provider, providerTransactionId)
```

## 11.5. Sai số tiền

### Trả thiếu

- Payment → `REVIEW_REQUIRED`.
- Order giữ `PENDING_PAYMENT`.
- Admin được chọn:
  - Từ chối.
  - Liên kết giao dịch bổ sung.
  - Xác nhận thủ công nếu chính sách cho phép.

### Trả thừa

- Payment → `REVIEW_REQUIRED`.
- Không tự động tạo Enrollment trong MVP.
- Admin xác nhận thủ công.
- Hệ thống lưu số tiền thực nhận và lý do xử lý.

## 11.6. Giao dịch không khớp

Webhook vẫn được lưu ngay cả khi:

- Không có payment reference.
- Reference sai định dạng.
- Không tìm thấy Order.
- Order đã hết hạn.
- Order đã được thanh toán.

Admin có thể xem hàng đợi `UNMATCHED`.

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
PENDING
ACTIVE
EXPIRED
REVOKED
```

## 12.3. Quy tắc

- Enrollment mua hàng tham chiếu `OrderItem`.
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
EXPIRED
GRADED
```

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
- Không cập nhật sau expiresAt.
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
- Chấm theo đáp án đã autosave.
- Đánh dấu `EXPIRED` rồi `GRADED`, hoặc lưu `submissionType = AUTO_EXPIRED`.

Không phụ thuộc hoàn toàn vào timer frontend.

---

# 15. Nghiệp vụ báo cáo

## 15.1. Doanh thu

Chỉ tính Payment `PAID`.

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

---

# 16. Audit nghiệp vụ

AuditLog chỉ ghi hành động quan trọng:

- Gán hoặc thu hồi role.
- Khóa tài khoản.
- Duyệt/từ chối/xuất bản Course.
- Thay đổi giá Course.
- Admin grant/revoke Enrollment.
- Override LessonProgress.
- Xử lý giao dịch thủ công.
- Thay đổi trạng thái Payment.
- Export dữ liệu nhạy cảm.

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

Tổng cộng khoảng **37 bảng**.

Số bảng lớn không đồng nghĩa phải tạo CRUD cho tất cả bảng. Nhiều bảng là bảng nội bộ phục vụ transaction và lịch sử.

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
UNIQUE(payment_webhook_logs.provider, payment_webhook_logs.provider_event_id)
UNIQUE(lesson_progress.student_id, lesson_progress.course_id, lesson_progress.lesson_id)
UNIQUE(exam_attempts.exam_id, exam_attempts.student_id, exam_attempts.attempt_number)
UNIQUE(attempt_answers.attempt_id, attempt_answers.attempt_question_id)
UNIQUE(attempt_answer_options.attempt_answer_id, attempt_answer_options.option_id)
```

Một Enrollment ACTIVE duy nhất trên một User/Course được bảo vệ bằng service transaction vì MySQL không có partial unique index đơn giản.

---

# 19. API nghiệp vụ cấp cao

## Identity

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
GET    /api/v1/me
PATCH  /api/v1/me
```

## Course authoring

```text
POST   /api/v1/teacher/courses
PATCH  /api/v1/teacher/courses/{courseId}
POST   /api/v1/teacher/courses/{courseId}/submit-review
POST   /api/v1/admin/courses/{courseId}/approve
POST   /api/v1/admin/courses/{courseId}/reject
POST   /api/v1/admin/courses/{courseId}/publish
POST   /api/v1/admin/courses/{courseId}/hide
```

## Catalog và checkout

```text
GET    /api/v1/courses
GET    /api/v1/courses/{slug}
GET    /api/v1/cart
POST   /api/v1/cart/items
DELETE /api/v1/cart/items/{courseId}
POST   /api/v1/orders/checkout
GET    /api/v1/orders
GET    /api/v1/orders/{orderId}
POST   /api/v1/orders/{orderId}/cancel
```

## Payment

```text
GET    /api/v1/orders/{orderId}/payment
GET    /api/v1/orders/{orderId}/status
POST   /api/v1/webhooks/payments/sepay
GET    /api/v1/admin/payment-webhooks
GET    /api/v1/admin/reconciliation
POST   /api/v1/admin/reconciliation/{webhookId}/match
POST   /api/v1/admin/reconciliation/{webhookId}/approve
POST   /api/v1/admin/reconciliation/{webhookId}/reject
```

## Learning

```text
GET    /api/v1/my-courses
GET    /api/v1/my-courses/{courseId}
GET    /api/v1/learning/lessons/{lessonId}
POST   /api/v1/learning/lessons/{lessonId}/heartbeat
POST   /api/v1/learning/lessons/{lessonId}/complete
GET    /api/v1/learning/courses/{courseId}/progress
```

## Exam

```text
POST   /api/v1/exams/{examId}/attempts
GET    /api/v1/exam-attempts/{attemptId}
PUT    /api/v1/exam-attempts/{attemptId}/answers/{attemptQuestionId}
POST   /api/v1/exam-attempts/{attemptId}/submit
GET    /api/v1/exam-attempts/{attemptId}/result
```

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
- API vẫn trả response phù hợp để provider không retry vô hạn.

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

---

# 21. Kế hoạch triển khai ba tháng

## Sprint 1 — Foundation và Identity

- Khởi tạo Spring Boot, React, MySQL.
- Docker Compose môi trường local.
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

- Review request.
- Approve/reject/publish.
- Public catalog.
- Search/filter.
- Preview.
- Cart.

## Sprint 4 — Order, Coupon và Payment

- Coupon.
- Checkout transaction.
- Order snapshot.
- Payment reference và QR.
- Webhook log.
- Idempotency.

## Sprint 5 — Enrollment và Learning

- Payment → Enrollment.
- Access control.
- My Courses.
- Progress.
- Prerequisite.
- Drip content.
- Course completion.

## Sprint 6 — Exam

- Exam builder.
- Question/Option.
- Attempt snapshot.
- Autosave.
- Submit và grading.
- Teacher statistics.

## Sprint 7 — Admin và đối soát

- Reconciliation queue.
- Manual match/approve/reject.
- Dashboard.
- Export CSV.
- Audit log.

## Sprint 8 — Hardening

- Integration test.
- Concurrency test webhook.
- Authorization test.
- E2E luồng chính.
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
- 1 Course bị từ chối.
- 2 Course published.
- 1 Course hidden.
- 1 Course có drip content.
- 1 Course có prerequisite.
- 2 Coupon.
- Nhiều Order ở các trạng thái.
- 1 webhook duplicate.
- 1 giao dịch thiếu tiền.
- 1 giao dịch unmatched.
- 1 Student hoàn thành khóa học.
- 1 Student bị khóa tại Lesson prerequisite.
- 1 Attempt hết giờ.
- 1 Attempt đạt và 1 Attempt không đạt.

Kịch bản bảo vệ nên trình diễn cả happy path và failure path.

---

# 23. Tự phản biện phạm vi

## 23.1. Phản biện: 37 bảng có quá nhiều không?

**Có**, nếu nhóm cố xây dựng màn hình CRUD cho mọi bảng.

**Không nhất thiết**, nếu hiểu rằng nhiều bảng là:

- Junction table.
- Snapshot table.
- Log table.
- Bảng nội bộ không có UI độc lập.

Tuy nhiên, 37 bảng vẫn tạo áp lực lớn lên migration và integration test.

### Điều kiện để giữ phạm vi

- Không làm UI quản trị trực tiếp cho mọi bảng.
- Generate boilerplate bằng AI nhưng bắt buộc review.
- Dùng API contract sớm.
- Mỗi sprint có integration test.
- Không thêm chức năng ngoài tài liệu.

### Kế hoạch cắt giảm khẩn cấp

Nếu chậm tiến độ, cắt theo thứ tự:

1. Password reset email, thay bằng Admin reset trong demo.
2. Coupon theo Course, chỉ giữ Coupon toàn hệ thống.
3. Drip content.
4. Teacher override progress.
5. Multiple-choice, chỉ giữ single-choice.
6. Export CSV.
7. Course completion snapshot.
8. Cart nhiều Course, chuyển thành Buy Now.

Không cắt:

- Ownership authorization.
- Workflow Course.
- Payment idempotency.
- Enrollment access control.
- Exam attempt.
- Progress.
- Reconciliation cơ bản.

## 23.2. Phản biện: Nghiệp vụ có đang “làm màu” không?

Một số chức năng có nguy cơ chỉ làm tăng số bảng nhưng không tăng giá trị:

- Notification.
- Audit toàn bộ request.
- Nhiều provider.
- Affiliate.
- Certificate.
- Recommendation AI.

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

Phạm vi này ở mức **rất căng nhưng có thể bảo vệ được** nếu:

- Hai người đều làm full stack theo feature.
- Mỗi người có nền tảng Spring Boot và React.
- Dùng AI để sinh boilerplate, test case và tài liệu.
- Không dành quá nhiều thời gian làm giao diện cầu kỳ.
- Dùng một payment provider.
- Không tự xử lý video.
- Có dữ liệu demo và test sớm.
- Có kế hoạch cắt giảm.

Nếu hai thành viên mới học Spring Boot từ đầu, phạm vi đầy đủ có nguy cơ không hoàn thành. Khi đó nên dùng kế hoạch cắt giảm nhưng giữ các luồng trọng tâm.

---

# 24. Tiêu chí đánh giá hoàn thành

MVP được coi là hoàn thành khi:

- Role và ownership được kiểm tra đúng.
- Teacher tạo và gửi duyệt Course.
- Admin duyệt, từ chối và xuất bản.
- Student mua nhiều Course bằng một Order.
- Coupon được kiểm tra ở backend.
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

---

# 25. Kết luận

MVP nâng cao được thiết kế để tránh hai cực đoan:

- Quá nhỏ, chỉ gồm CRUD Course và xem video.
- Quá lớn, cố sao chép toàn bộ Moodle hoặc một marketplace thương mại.

Phạm vi tập trung vào bốn chuỗi nghiệp vụ có khả năng thể hiện chiều sâu của đồ án:

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

Điểm mạnh của đồ án không nằm ở số lượng màn hình, mà ở việc các trạng thái, quyền, transaction và lịch sử dữ liệu được thiết kế nhất quán.

Nếu triển khai đúng các luồng trọng tâm, đây là một hệ thống đủ lớn để thể hiện kiến thức về phân tích nghiệp vụ, Spring Security, Spring Data JPA, transaction, database constraint, REST API, React state management và tích hợp webhook.
