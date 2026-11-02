# Hướng Dẫn Sử Dụng Thư Viện UI/UX Component Dùng Chung (LMS Platform)

Tài liệu này hướng dẫn các thành viên trong nhóm phát triển giao diện người dùng đồng bộ, chuẩn đẹp theo **UI/UX Pro Max Skill**, bám sát **Đặc tả nghiệp vụ LMS MVP v3.2** và **OpenAPI Spec v1.5** (trong thư mục `docs` của dự án).

---

## 1. Triết Lý Thiết Kế & Design Tokens

Hệ thống màu sắc và kiểu chữ được định nghĩa tập trung tại `src/styles/tokens.css` và `src/styles/global.css`:

- **Bảng màu chủ đạo (Brand Colors)**:
  - `--color-primary`: `#0D9488` (Teal đậm - tượng trưng cho tri thức, sự chuyên nghiệp và giáo dục)
  - `--color-secondary`: `#2DD4BF` (Teal sáng - màu bổ trợ)
  - `--color-accent`: `#D97706` (Amber/Cam - Call To Action, nổi bật cho Mua ngay, Giỏ hàng, AI Adaptive)
  - `--color-background`: `#F0FDFA` (Xanh ngọc rất nhạt - dịu mắt, chống mỏi mắt khi học lâu)
  - `--color-card`: `#FFFFFF` (Trắng tinh khiết)
  - `--color-border`: `#D1FAF5` / `--color-border-strong`: `#5EEAD4`
- **Typography**:
  - Tiêu đề (`h1` - `h6`): Font `Baloo 2` (thân thiện, năng động, mang tính giáo dục hiện đại)
  - Nội dung thường (`body`, `p`, `inputs`): Font `Inter` (rõ ràng, tương phản cao, dễ đọc trên mọi thiết bị)
  - Khối mã (`code`, `mono`): `Fira Code`

---

## 2. Quy Chuẩn Đặt Tên & Code Convention (Bắt buộc tuân thủ)

1. **Boolean Variables & Props**: Luôn có tiền tố `is`, `are`, `should`, `has` (ví dụ: `isOpen`, `isLoading`, `hasDot`, `isEnrolled`, `isLocked`).
2. **Event Props & Handlers**:
   - Event prop từ component cha truyền vào: `onAction` (ví dụ: `onClose`, `onSelect`, `onRowClick`).
   - Function xử lý nội bộ component: `handleAction` (ví dụ: `handleClose`, `handleSelect`).
3. **Event Listener Cleanup**: Bắt buộc giải phóng và remove listener trong hàm cleanup của `useEffect` (đã được áp dụng mẫu chuẩn trong `Modal.tsx`).
4. **Không dùng Emoji làm icon**: Dùng bộ SVG Icons nội bộ từ `Icons` (`src/components/ui/Icons`).

---

## 3. Danh Mục Base UI Components (`src/components/ui`)

Tất cả component được export trực tiếp qua barrel export `src/components/ui`:

```tsx
import {
  Button,
  Input,
  Card,
  CardHeader,
  CardBody,
  CardFooter,
  Badge,
  Modal,
  Alert,
  Progress,
  Select,
  Tabs,
  Table,
  Avatar,
  Skeleton,
  Icons,
} from '@/components/ui';
```

### 3.1. Button
Hỗ trợ các biến thể: `primary`, `secondary`, `accent`, `outline`, `ghost`, `destructive`.
Hỗ trợ kích thước: `sm`, `md`, `lg`, `icon`.
Hỗ trợ: `isLoading`, `leftIcon`, `rightIcon`, `fullWidth`.

```tsx
<Button variant="primary" leftIcon={<Icons.Sparkles size={16} />} isLoading={isSubmitting}>
  Luyện tập cùng AI
</Button>
```

### 3.2. Input
Tuân thủ UX: Nhãn luôn hiển thị (không dùng placeholder thay thế nhãn), hiển thị lỗi gần trường nhập liệu.

```tsx
<Input
  label="Mật khẩu"
  type="password"
  placeholder="Nhập tối thiểu 8 ký tự"
  errorMessage={passwordError}
  helperText="Bao gồm chữ in hoa, số và ký tự đặc biệt"
  leftIcon={<Icons.Lock size={18} />}
  required
/>
```

### 3.3. Badge (Trạng thái nghiệp vụ)
Đồng bộ các trạng thái từ OpenAPI v1.5:
- Khóa học: `DRAFT`, `PENDING_APPROVAL`, `PUBLISHED`, `REJECTED`
- Đơn hàng / Thanh toán: `PENDING`, `PAID`, `FAILED`
- Vai trò: `STUDENT`, `TEACHER`, `ADMIN`

```tsx
<Badge variant="success" hasDot>PUBLISHED</Badge>
<Badge variant="warning" leftIcon={<Icons.Clock size={14} />}>PENDING</Badge>
```

### 3.4. Modal
Hỗ trợ đóng bằng ESC (có cleanup chuẩn), click backdrop, khóa scroll trang, `aria-modal="true"`.

```tsx
<Modal
  isOpen={isModalOpen}
  onClose={() => setIsModalOpen(false)}
  title="Xác nhận thanh toán"
  footer={
    <>
      <Button variant="ghost" onClick={() => setIsModalOpen(false)}>Hủy</Button>
      <Button variant="accent" onClick={handleConfirmOrder}>Xác nhận qua SePay</Button>
    </>
  }
>
  <p>Vui lòng quét mã QR chuyển khoản chính xác nội dung đơn hàng.</p>
</Modal>
```

### 3.5. Table (Tích hợp phân trang OpenAPI Envelope)
Hỗ trợ định dạng phân trang `{ data: [...], meta: { page, size, totalElements, totalPages } }`.

```tsx
<Table
  columns={[
    { key: 'title', header: 'Tên khóa học' },
    { key: 'price', header: 'Học phí', render: (c) => `${c.price.toLocaleString()} đ` },
    { key: 'status', header: 'Trạng thái', render: (c) => <Badge variant="success">{c.status}</Badge> }
  ]}
  data={courseList}
  pagination={{
    meta: { page: currentPage, size: 10, totalElements: total, totalPages: pages },
    onPageChange: (newPage) => setCurrentPage(newPage)
  }}
/>
```

---

## 4. Các Mẫu Giao Diện Đặc Thù LMS (`src/components/patterns`)

### 4.1. `CourseCard`
Thẻ khóa học chuẩn hóa hiển thị thumbnail tỷ lệ 16:9, xếp hạng sao, giảng viên, giá tiền VND số nguyên đồng theo đặc tả OpenAPI, và tự động chuyển sang chế độ thanh tiến độ học tập khi học viên đã ghi danh (`isEnrolled`).

### 4.2. `LessonItem`
Mục bài học trong giáo trình:
- Hiển thị loại bài: `VIDEO`, `QUIZ`, `ARTICLE`.
- Trạng thái: `isCompleted` (Đã học xong), `isActive` (Đang học), `isLocked` (Khóa kèm `lockedReason` theo mã lỗi `LessonLockedError` của OpenAPI).
- Nhãn học thử `isPreview` cho khách vãng lai.

### 4.3. `StatCard`
Thẻ KPI thống kê cho màn hình Dashboard Quản trị và Giảng viên: Doanh thu SePay, học viên, tỷ lệ hoàn thành, số phiên luyện tập AI ZPD.

---

## 5. Layout Dùng Chung (`src/components/layout`)

- **`Navbar`**: Thanh điều hướng trên cùng, nhận diện thương hiệu LMS, menu theo ngữ cảnh, giỏ hàng, thông báo, chuyển đổi vai trò xem thử.
- **`Sidebar`**: Menu điều hướng bên hông, tự động đổi danh mục mục chức năng theo vai trò (`STUDENT`, `TEACHER`, `ADMIN`), responsive trên thiết bị di động.
- **`Breadcrumb`**: Đường dẫn ngữ cảnh giúp học viên biết mình đang ở cấp nào trong cây phân cấp khóa học.

---

## 6. Xem Demo Trực Tiếp

Toàn bộ thư viện component và các mẫu tương tác được kết nối sẵn tại `src/pages/Showcase.tsx` và chạy ngay trên màn hình chính của ứng dụng (`App.tsx`).
