import { useState } from 'react';
import {
  Button,
  Input,
  Card,
  CardHeader,
  CardBody,
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
  type Column,
} from '../components/ui';
import { Navbar, Sidebar, Breadcrumb } from '../components/layout';
import { CourseCard, StatCard, LessonItem } from '../components/patterns';
import styles from './Showcase.module.css';

interface DemoUser {
  id: number;
  name: string;
  email: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  status: 'ACTIVE' | 'LOCKED';
  joinedDate: string;
}

export function Showcase() {
  // Navigation & Role states
  const [currentRole, setCurrentRole] = useState<'STUDENT' | 'TEACHER' | 'ADMIN'>('STUDENT');
  const [activeSidebarId, setActiveSidebarId] = useState('dashboard');
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [cartCount, setCartCount] = useState(2);

  // Component states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isAlertVisible, setIsAlertVisible] = useState(true);
  const [isLoadingBtn, setIsLoadingBtn] = useState(false);
  const [inputVal, setInputVal] = useState('nguyenvana@gmail.com');
  const [inputError, setInputError] = useState('');
  const [selectCategory, setSelectCategory] = useState('frontend');
  const [progressVal, setProgressVal] = useState(65);
  const [activeTab, setActiveTab] = useState('all');

  // Sample Table Data (User list)
  const sampleUsers: DemoUser[] = [
    { id: 1, name: 'Nguyễn Văn An', email: 'an.nguyen@lms.edu.vn', role: 'STUDENT', status: 'ACTIVE', joinedDate: '2026-10-15' },
    { id: 2, name: 'ThS. Trần Thị Mai', email: 'mai.tran@lms.edu.vn', role: 'TEACHER', status: 'ACTIVE', joinedDate: '2026-09-01' },
    { id: 3, name: 'Lê Hoàng Admin', email: 'admin@lms.edu.vn', role: 'ADMIN', status: 'ACTIVE', joinedDate: '2026-08-10' },
    { id: 4, name: 'Phạm Minh Đức', email: 'duc.pham@lms.edu.vn', role: 'STUDENT', status: 'LOCKED', joinedDate: '2026-10-20' },
  ];

  const columns: Column<DemoUser>[] = [
    {
      key: 'name',
      header: 'Người dùng',
      render: (user) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Avatar name={user.name} size="sm" isOnline={user.status === 'ACTIVE'} />
          <div>
            <div style={{ fontWeight: 600 }}>{user.name}</div>
            <div style={{ fontSize: '12px', color: 'var(--color-muted-foreground)' }}>{user.email}</div>
          </div>
        </div>
      ),
    },
    {
      key: 'role',
      header: 'Vai trò',
      render: (user) => (
        <Badge
          variant={user.role === 'ADMIN' ? 'destructive' : user.role === 'TEACHER' ? 'accent' : 'primary'}
          size="sm"
        >
          {user.role}
        </Badge>
      ),
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (user) => (
        <Badge
          variant={user.status === 'ACTIVE' ? 'success' : 'destructive'}
          hasDot
          size="sm"
        >
          {user.status === 'ACTIVE' ? 'Hoạt động' : 'Đã khóa'}
        </Badge>
      ),
    },
    {
      key: 'joinedDate',
      header: 'Ngày tham gia',
    },
    {
      key: 'action',
      header: 'Thao tác',
      align: 'right',
      render: (user) => (
        <Button
          variant="outline"
          size="sm"
          onClick={() => alert(`Xem chi tiết tài khoản: ${user.name}`)}
        >
          Chi tiết
        </Button>
      ),
    },
  ];

  const handleTestSubmit = () => {
    setIsLoadingBtn(true);
    setTimeout(() => {
      setIsLoadingBtn(false);
      if (!inputVal.includes('@')) {
        setInputError('Email không hợp lệ. Vui lòng nhập lại!');
      } else {
        setInputError('');
        alert('Dữ liệu hợp lệ!');
      }
    }, 1000);
  };

  return (
    <div className={styles.page}>
      {/* 1. Global Navbar */}
      <Navbar
        cartCount={cartCount}
        userName={currentRole === 'ADMIN' ? 'Admin LMS' : currentRole === 'TEACHER' ? 'ThS. Trần Mai' : 'Nguyễn Văn An'}
        userRole={currentRole}
        onMenuToggle={() => setIsSidebarOpen(!isSidebarOpen)}
        onCartClick={() => alert(`Xem giỏ hàng: ${cartCount} sản phẩm`)}
        onProfileClick={() => setIsModalOpen(true)}
        extraRight={
          <div className={styles.roleSwitcher}>
            <span className={styles.roleLabel}>Chế độ vai trò:</span>
            <select
              value={currentRole}
              onChange={(e) => setCurrentRole(e.target.value as 'STUDENT' | 'TEACHER' | 'ADMIN')}
              className={styles.roleSelect}
              aria-label="Chọn vai trò xem thử"
            >
              <option value="STUDENT">Student</option>
              <option value="TEACHER">Teacher</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
        }
      />

      <div className={styles.bodyWrapper}>
        {/* 2. Responsive Sidebar */}
        <Sidebar
          isOpen={isSidebarOpen}
          onClose={() => setIsSidebarOpen(false)}
          activeId={activeSidebarId}
          onSelect={(id) => setActiveSidebarId(id)}
          role={currentRole}
        />

        {/* 3. Main Showcase Content */}
        <main className={styles.mainContent}>
          <div className={styles.container}>
            {/* Breadcrumb Context */}
            <Breadcrumb
              items={[
                { label: 'Trang chủ', onClick: () => setActiveSidebarId('home') },
                { label: 'Hệ thống thiết kế UI/UX', onClick: () => setActiveSidebarId('ui-design') },
                { label: 'Base Components & Mẫu LMS' },
              ]}
              className={styles.breadcrumb}
            />

            {/* Hero Banner */}
            <section className={styles.heroBanner}>
              <div className={styles.heroText}>
                <div className={styles.heroBadge}>
                  <Icons.Sparkles size={16} />
                  <span>UI/UX PRO MAX & OPENAPI 1.5 ALIGNED</span>
                </div>
                <h1 className={styles.heroTitle}>
                  Thư Viện Component Chung & Mẫu Giao Diện LMS
                </h1>
                <p className={styles.heroDesc}>
                  Bộ quy chuẩn giao diện người dùng thống nhất cho dự án Đồ án tốt nghiệp LMS Platform.
                  Thiết kế theo chuẩn giáo dục hiện đại (Teal & Amber), tương thích hoàn toàn với
                  đặc tả nghiệp vụ v3.2 và tài liệu OpenAPI v1.5 trong nhánh <code>docs</code>.
                </p>
              </div>
            </section>

            {/* Notification Alert Test */}
            {isAlertVisible && (
              <Alert
                variant="info"
                title="Quy ước Contract Backend & Frontend"
                onClose={() => setIsAlertVisible(false)}
                className={styles.specAlert}
              >
                Mọi API trả về thành công đều tuân thủ envelope <code>{'{ data: ... }'}</code>, phân trang dùng <code>{'{ data: [...], meta: { page, size, totalElements, totalPages } }'}</code>. Tiền tệ là VND số nguyên đồng.
              </Alert>
            )}

            {/* Section: KPI Stats (Dashboard Pattern) */}
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <div>
                  <h2 className={styles.sectionTitle}>1. Mẫu Thẻ Chỉ Số KPI (Dashboard StatCard)</h2>
                  <p className={styles.sectionSubtitle}>Dùng cho trang Tổng quan của Quản trị viên, Giảng viên và Học viên</p>
                </div>
              </div>

              <div className={styles.grid4}>
                <StatCard
                  title="Doanh thu SePay"
                  value="128.450.000 đ"
                  icon={<Icons.ShoppingCart size={20} />}
                  variant="primary"
                  trend={{ value: '18.4%', isPositive: true, label: 'vs tháng trước' }}
                />
                <StatCard
                  title="Học viên đã mua"
                  value="1,420"
                  icon={<Icons.User size={20} />}
                  variant="secondary"
                  trend={{ value: '12.1%', isPositive: true, label: 'vs tuần trước' }}
                />
                <StatCard
                  title="Tỷ lệ hoàn thành"
                  value="84.6%"
                  icon={<Icons.Award size={20} />}
                  variant="success"
                  trend={{ value: '4.2%', isPositive: true, label: 'tăng đều' }}
                />
                <StatCard
                  title="Phiên Luyện Tập AI ZPD"
                  value="3,892"
                  icon={<Icons.Sparkles size={20} />}
                  variant="accent"
                  trend={{ value: '31.5%', isPositive: true, label: 'tương tác cao' }}
                />
              </div>
            </section>

            {/* Section: Buttons & Badges */}
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <div>
                  <h2 className={styles.sectionTitle}>2. Nút Bấm & Thẻ Trạng Thái (Button & Badge)</h2>
                  <p className={styles.sectionSubtitle}>Đầy đủ các biến thể màu sắc, kích cỡ, trạng thái hover, focus, disabled và loading</p>
                </div>
              </div>

              <Card>
                <CardBody>
                  <h4 style={{ marginBottom: '12px' }}>Button Variants & Sizes</h4>
                  <div className={styles.btnRow}>
                    <Button variant="primary">Primary Button</Button>
                    <Button variant="secondary">Secondary</Button>
                    <Button variant="accent">Accent CTA</Button>
                    <Button variant="outline">Outline</Button>
                    <Button variant="ghost">Ghost</Button>
                    <Button variant="destructive">Destructive</Button>
                    <Button
                      variant="primary"
                      isLoading={isLoadingBtn}
                      onClick={handleTestSubmit}
                    >
                      Bấm để thử Loading
                    </Button>
                    <Button variant="primary" leftIcon={<Icons.Sparkles size={16} />}>
                      Gợi ý AI
                    </Button>
                  </div>

                  <h4 style={{ marginTop: '24px', marginBottom: '12px' }}>Badge Trạng Thái (LMS & OpenAPI Statuses)</h4>
                  <div className={styles.badgeRow}>
                    <Badge variant="primary" hasDot>DRAFT (Bản nháp)</Badge>
                    <Badge variant="warning" hasDot>PENDING_APPROVAL (Chờ duyệt)</Badge>
                    <Badge variant="success" hasDot>PUBLISHED (Đã xuất bản)</Badge>
                    <Badge variant="destructive" hasDot>REJECTED (Bị từ chối)</Badge>
                    <Badge variant="info">STUDENT</Badge>
                    <Badge variant="accent">TEACHER</Badge>
                    <Badge variant="destructive">ADMIN</Badge>
                    <Badge variant="success" leftIcon={<Icons.Check size={14} />}>PAID (Đã thanh toán)</Badge>
                    <Badge variant="warning" leftIcon={<Icons.Clock size={14} />}>PENDING (Chờ chuyển khoản)</Badge>
                    <Badge variant="accent" leftIcon={<Icons.Sparkles size={14} />}>ZPD Adaptive AI</Badge>
                  </div>
                </CardBody>
              </Card>
            </section>

            {/* Section: Form Controls (Input & Select) */}
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <div>
                  <h2 className={styles.sectionTitle}>3. Điều Khiển Nhập Liệu (Input, Select, Form UX)</h2>
                  <p className={styles.sectionSubtitle}>Tuân thủ nguyên tắc UX Pro: Nhãn luôn hiển thị (no placeholder-only), thông báo lỗi rõ ràng</p>
                </div>
              </div>

              <Card>
                <CardBody>
                  <div className={styles.grid2}>
                    <Input
                      label="Email tài khoản"
                      placeholder="vidu@domain.com"
                      value={inputVal}
                      onChange={(e) => {
                        setInputVal(e.target.value);
                        if (inputError) setInputError('');
                      }}
                      errorMessage={inputError}
                      helperText="Địa chỉ email dùng để nhận mã xác nhận và thông báo thanh toán."
                      leftIcon={<Icons.User size={18} />}
                      required
                    />

                    <Select
                      label="Danh mục khóa học"
                      value={selectCategory}
                      onChange={(e) => setSelectCategory(e.target.value)}
                      helperText="Chọn danh mục để lọc giáo trình tương ứng."
                      leftIcon={<Icons.BookOpen size={18} />}
                      options={[
                        { value: 'frontend', label: 'Lập trình Frontend (React, Vite, CSS)' },
                        { value: 'backend', label: 'Lập trình Backend (Java Spring Boot 3)' },
                        { value: 'ai-rag', label: 'AI Adaptive Learning & RAG Engine' },
                        { value: 'database', label: 'Cơ sở dữ liệu MySQL 8 & JPA' },
                      ]}
                      required
                    />
                  </div>

                  <div style={{ marginTop: '20px' }}>
                    <Progress
                      value={progressVal}
                      label={`Tiến độ hoàn thành hồ sơ học viên`}
                      isLabelVisible
                      variant="primary"
                    />
                    <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                      <Button size="sm" variant="outline" onClick={() => setProgressVal(Math.max(0, progressVal - 15))}>
                        Giảm tiến độ
                      </Button>
                      <Button size="sm" variant="outline" onClick={() => setProgressVal(Math.min(100, progressVal + 15))}>
                        Tăng tiến độ
                      </Button>
                    </div>
                  </div>
                </CardBody>
              </Card>
            </section>

            {/* Section: LMS Course Card Pattern */}
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <div>
                  <h2 className={styles.sectionTitle}>4. Mẫu Thẻ Khóa Học (CourseCard Pattern)</h2>
                  <p className={styles.sectionSubtitle}>Hiển thị danh mục, giảng viên, giá tiền chuẩn OpenAPI, tiến độ học viên</p>
                </div>
              </div>

              <div className={styles.grid3}>
                <CourseCard
                  id={101}
                  title="Fullstack LMS: Spring Boot 3 & React 19 Hiện Đại"
                  teacherName="ThS. Trần Thị Mai"
                  categoryName="Fullstack"
                  price={890000}
                  discountPrice={690000}
                  rating={4.9}
                  totalLessons={36}
                  durationMinutes={540}
                  onAddToCart={(id) => setCartCount((prev) => prev + 1)}
                  onEnrollClick={(id) => alert(`Bắt đầu thanh toán khóa học #${id}`)}
                />

                <CourseCard
                  id={102}
                  title="Hệ Thống Đánh Giá & Sinh Nhiệm Vụ Thích Ứng AI RAG"
                  teacherName="TS. Đỗ Anh Nhật"
                  categoryName="AI & Machine Learning"
                  price={1200000}
                  rating={5.0}
                  totalLessons={28}
                  durationMinutes={420}
                  onAddToCart={(id) => setCartCount((prev) => prev + 1)}
                  onEnrollClick={(id) => alert(`Bắt đầu thanh toán khóa học #${id}`)}
                />

                <CourseCard
                  id={103}
                  title="Kiến Trúc Modular Monolith & MySQL 8 Thực Chiến"
                  teacherName="ThS. Trần Thị Mai"
                  categoryName="Backend Architecture"
                  price={750000}
                  isEnrolled
                  progressPercent={68}
                  totalLessons={42}
                  durationMinutes={600}
                  onEnrollClick={(id) => alert(`Vào phòng học khóa #${id}`)}
                />
              </div>
            </section>

            {/* Section: Lesson Curriculum & Prerequisite Locking */}
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <div>
                  <h2 className={styles.sectionTitle}>5. Mẫu Danh Sách Bài Học & Khóa Tiên Quyết (Curriculum & Prerequisite)</h2>
                  <p className={styles.sectionSubtitle}>Đồng bộ với lỗi LessonLockedError và điều kiện học tuần tự trong đặc tả v3.2</p>
                </div>
              </div>

              <Card>
                <CardHeader
                  title="Chương 1: Tổng quan Kiến trúc LMS & Authentication"
                  subtitle="3 bài học • Hoàn thành 1/3 bài"
                  action={<Badge variant="primary">Chương hiện tại</Badge>}
                />
                <CardBody>
                  <div className={styles.lessonList}>
                    <LessonItem
                      id={1}
                      orderNumber={1}
                      title="1.1 Tổng quan kiến trúc Modular Monolith Spring Boot & React"
                      type="VIDEO"
                      durationText="15:30"
                      isCompleted
                    />
                    <LessonItem
                      id={2}
                      orderNumber={2}
                      title="1.2 Cơ chế Access Token (Bearer) & Refresh Token HttpOnly Cookie"
                      type="VIDEO"
                      durationText="22:15"
                      isActive
                    />
                    <LessonItem
                      id={3}
                      orderNumber={3}
                      title="1.3 Bài tập Quiz: Kiểm tra an toàn bảo mật JWT & Phân quyền RBAC"
                      type="QUIZ"
                      durationText="10 câu • 15 phút"
                      isLocked
                      lockedReason="Cần hoàn thành video 1.2 đạt tối thiểu 80% thời lượng"
                    />
                    <LessonItem
                      id={4}
                      orderNumber={4}
                      title="1.4 Đọc tài liệu: Chiến lược Idempotency khi nhận Webhook thanh toán"
                      type="ARTICLE"
                      durationText="8 phút đọc"
                      isLocked
                      lockedReason="Cần vượt qua bài Quiz 1.3 với điểm số >= 7.0"
                    />
                  </div>
                </CardBody>
              </Card>
            </section>

            {/* Section: Data Table */}
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <div>
                  <h2 className={styles.sectionTitle}>6. Mẫu Bảng Quản Trị & Phân Trang (Table & Pagination)</h2>
                  <p className={styles.sectionSubtitle}>Áp dụng cho Quản lý khóa học, Học viên, Đơn hàng SePay và Ngân hàng câu hỏi</p>
                </div>
              </div>

              <Table
                columns={columns}
                data={sampleUsers}
                pagination={{
                  meta: { page: 0, size: 10, totalElements: 4, totalPages: 1 },
                  onPageChange: (p) => console.log('Chuyển trang:', p),
                }}
              />
            </section>

            {/* Section: Tabs & Skeletons */}
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <div>
                  <h2 className={styles.sectionTitle}>7. Điều Hướng Tab & Trạng Thái Tải (Tabs & Skeleton)</h2>
                  <p className={styles.sectionSubtitle}>Tabs linh hoạt (Underline/Pill) và Skeleton chống Cumulative Layout Shift (CLS)</p>
                </div>
              </div>

              <Card>
                <CardBody>
                  <Tabs
                    items={[
                      { id: 'all', label: 'Tất cả bài học', badge: <Badge size="sm" variant="primary">24</Badge> },
                      { id: 'video', label: 'Video bài giảng', icon: <Icons.Video size={16} /> },
                      { id: 'quiz', label: 'Bài kiểm tra', icon: <Icons.FileText size={16} /> },
                      { id: 'ai', label: 'Nhiệm vụ AI ZPD', icon: <Icons.Sparkles size={16} /> },
                    ]}
                    activeId={activeTab}
                    onChange={(id) => setActiveTab(id)}
                    variant="pills"
                  >
                    {(currentTab) => (
                      <div style={{ marginTop: '16px' }}>
                        <p style={{ marginBottom: '12px', fontSize: '14px', color: 'var(--color-muted-foreground)' }}>
                          Đang hiển thị nội dung cho tab: <strong>{currentTab}</strong>
                        </p>
                        {/* Shimmer skeleton demo */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          <Skeleton variant="text" width="60%" height={20} />
                          <Skeleton variant="text" width="100%" height={16} />
                          <Skeleton variant="text" width="80%" height={16} />
                        </div>
                      </div>
                    )}
                  </Tabs>
                </CardBody>
              </Card>
            </section>
          </div>
        </main>
      </div>

      {/* Demo Modal for User Profile or Checkout Confirmation */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Thông Tin Hồ Sơ & Cài Đặt Hệ Thống"
        description="Quản lý phiên đăng nhập và các đặc quyền của tài khoản trong hệ thống LMS."
        footer={
          <>
            <Button variant="ghost" onClick={() => setIsModalOpen(false)}>
              Đóng lại
            </Button>
            <Button variant="primary" onClick={() => { setIsModalOpen(false); alert('Đã lưu thay đổi!'); }}>
              Lưu thay đổi
            </Button>
          </>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <Avatar name="Nguyễn Văn An" size="lg" isOnline />
            <div>
              <h4 style={{ margin: 0 }}>Nguyễn Văn An</h4>
              <p style={{ margin: 0, fontSize: '13px', color: 'var(--color-muted-foreground)' }}>an.nguyen@lms.edu.vn</p>
              <div style={{ marginTop: '6px' }}>
                <Badge variant="primary" size="sm">Học viên Kỹ thuật phần mềm</Badge>
              </div>
            </div>
          </div>
          <Alert variant="success" title="Xác thực 2 lớp bảo mật">
            Phiên đăng nhập đang được bảo vệ bằng Token JWT và Cookie HttpOnly.
          </Alert>
        </div>
      </Modal>
    </div>
  );
}
