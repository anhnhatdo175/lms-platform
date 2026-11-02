import type { ReactNode } from 'react';
import { Icons } from '../ui/Icons';
import styles from './Sidebar.module.css';

export interface NavLinkItem {
  id: string;
  label: string;
  icon: ReactNode;
  badge?: string | number;
  badgeVariant?: 'primary' | 'warning' | 'destructive' | 'accent';
}

export interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
  activeId: string;
  onSelect: (id: string) => void;
  role?: 'STUDENT' | 'TEACHER' | 'ADMIN';
}

export function Sidebar({
  isOpen = false,
  onClose,
  activeId,
  onSelect,
  role = 'STUDENT',
}: SidebarProps) {
  const studentItems: NavLinkItem[] = [
    { id: 'dashboard', label: 'Bảng học tập', icon: <Icons.BarChart size={18} /> },
    { id: 'my-courses', label: 'Khóa học của tôi', icon: <Icons.BookOpen size={18} /> },
    { id: 'ai-practice', label: 'Luyện tập AI ZPD', icon: <Icons.Sparkles size={18} />, badge: 'AI', badgeVariant: 'accent' },
    { id: 'exams', label: 'Bài kiểm tra & Thi', icon: <Icons.FileText size={18} /> },
    { id: 'orders', label: 'Lịch sử mua hàng', icon: <Icons.ShoppingCart size={18} /> },
  ];

  const teacherItems: NavLinkItem[] = [
    { id: 'teacher-overview', label: 'Tổng quan giảng dạy', icon: <Icons.BarChart size={18} /> },
    { id: 'teacher-courses', label: 'Khóa học soạn thảo', icon: <Icons.BookOpen size={18} /> },
    { id: 'teacher-curriculum', label: 'Soạn giáo trình', icon: <Icons.Layers size={18} /> },
    { id: 'question-bank', label: 'Ngân hàng câu hỏi', icon: <Icons.FileText size={18} /> },
    { id: 'ai-knowledge', label: 'Tài liệu tri thức RAG', icon: <Icons.Sparkles size={18} /> },
  ];

  const adminItems: NavLinkItem[] = [
    { id: 'admin-dashboard', label: 'Báo cáo doanh thu', icon: <Icons.BarChart size={18} /> },
    { id: 'admin-courses', label: 'Duyệt xuất bản khóa học', icon: <Icons.CheckCircle size={18} />, badge: 3, badgeVariant: 'warning' },
    { id: 'admin-users', label: 'Quản lý người dùng', icon: <Icons.User size={18} /> },
    { id: 'admin-reconcile', label: 'Đối soát SePay webhook', icon: <Icons.AlertCircle size={18} /> },
  ];

  const items = role === 'ADMIN' ? adminItems : role === 'TEACHER' ? teacherItems : studentItems;

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          className={styles.backdrop}
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      <aside
        className={`${styles.sidebar} ${isOpen ? styles['sidebar--open'] : ''}`}
        aria-label="Danh mục điều hướng phụ"
      >
        <div className={styles.sectionHeader}>
          <span className={styles.roleTag}>
            {role === 'ADMIN' ? 'KHÔNG GIAN QUẢN TRỊ' : role === 'TEACHER' ? 'KHÔNG GIAN GIẢNG VIÊN' : 'GÓC HỌC TẬP'}
          </span>
        </div>

        <nav className={styles.navList}>
          {items.map((item) => {
            const isActive = activeId === item.id;
            return (
              <button
                key={item.id}
                type="button"
                className={`${styles.navItem} ${isActive ? styles['navItem--active'] : ''}`}
                onClick={() => {
                  onSelect(item.id);
                  onClose?.();
                }}
              >
                <span className={styles.navItemIcon} aria-hidden="true">
                  {item.icon}
                </span>
                <span className={styles.navItemLabel}>{item.label}</span>
                {item.badge !== undefined && (
                  <span
                    className={`${styles.navItemBadge} ${item.badgeVariant ? styles[`navItemBadge--${item.badgeVariant}`] : ''}`}
                  >
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        <div className={styles.sidebarFooter}>
          <button
            type="button"
            className={styles.footerLink}
            onClick={() => onSelect('settings')}
          >
            <Icons.Settings size={18} />
            <span>Cài đặt tài khoản</span>
          </button>
          <button
            type="button"
            className={`${styles.footerLink} ${styles['footerLink--danger']}`}
            onClick={() => onSelect('logout')}
          >
            <Icons.LogOut size={18} />
            <span>Đăng xuất</span>
          </button>
        </div>
      </aside>
    </>
  );
}
