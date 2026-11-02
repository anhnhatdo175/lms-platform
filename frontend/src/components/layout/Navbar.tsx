import type { ReactNode } from 'react';
import { Icons } from '../ui/Icons';
import { Badge } from '../ui/Badge';
import { Avatar } from '../ui/Avatar';
import styles from './Navbar.module.css';

export interface NavbarProps {
  cartCount?: number;
  userName?: string;
  userRole?: 'STUDENT' | 'TEACHER' | 'ADMIN';
  userAvatar?: string;
  onMenuToggle?: () => void;
  onCartClick?: () => void;
  onProfileClick?: () => void;
  activeNav?: string;
  onNavigate?: (path: string) => void;
  extraRight?: ReactNode;
}

export function Navbar({
  cartCount = 0,
  userName = 'Học viên',
  userRole = 'STUDENT',
  userAvatar,
  onMenuToggle,
  onCartClick,
  onProfileClick,
  activeNav = 'courses',
  onNavigate,
  extraRight,
}: NavbarProps) {
  const formatRoleName = (role: string) => {
    switch (role) {
      case 'ADMIN': return 'Quản trị';
      case 'TEACHER': return 'Giảng viên';
      case 'STUDENT': default: return 'Học viên';
    }
  };

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        {/* Left: Mobile Toggle & Brand */}
        <div className={styles.brandGroup}>
          {onMenuToggle && (
            <button
              type="button"
              className={styles.menuToggle}
              onClick={onMenuToggle}
              aria-label="Mở danh mục điều hướng"
            >
              <Icons.Menu size={22} />
            </button>
          )}

          <a
            href="/"
            className={styles.brand}
            onClick={(e) => {
              if (onNavigate) {
                e.preventDefault();
                onNavigate('/');
              }
            }}
          >
            <div className={styles.brandIcon} aria-hidden="true">
              <Icons.GraduationCap size={24} />
            </div>
            <div className={styles.brandText}>
              <span className={styles.brandTitle}>LMS Platform</span>
              <span className={styles.brandSubtitle}>AI-Adaptive RAG</span>
            </div>
          </a>
        </div>

        {/* Center: Main Navigation */}
        <nav className={styles.nav} aria-label="Điều hướng chính">
          <button
            type="button"
            className={`${styles.navItem} ${activeNav === 'courses' ? styles['navItem--active'] : ''}`}
            onClick={() => onNavigate?.('/courses')}
          >
            Khám phá khóa học
          </button>
          <button
            type="button"
            className={`${styles.navItem} ${activeNav === 'my-courses' ? styles['navItem--active'] : ''}`}
            onClick={() => onNavigate?.('/my-courses')}
          >
            Khóa học của tôi
          </button>
          <button
            type="button"
            className={`${styles.navItem} ${styles['navItem--ai']} ${activeNav === 'ai-rag' ? styles['navItem--active'] : ''}`}
            onClick={() => onNavigate?.('/ai-rag')}
          >
            <Icons.Sparkles size={16} />
            Luyện tập AI
            <span className={styles.aiBadge}>Adaptive</span>
          </button>
        </nav>

        {/* Right: Actions & User Info */}
        <div className={styles.actions}>
          {extraRight}

          {/* Cart Button */}
          <button
            type="button"
            className={styles.actionBtn}
            onClick={onCartClick}
            aria-label={`Giỏ hàng, có ${cartCount} khóa học`}
          >
            <Icons.ShoppingCart size={20} />
            {cartCount > 0 && (
              <span className={styles.cartBadge}>{cartCount}</span>
            )}
          </button>

          {/* Notifications */}
          <button
            type="button"
            className={styles.actionBtn}
            aria-label="Thông báo mới"
          >
            <Icons.Bell size={20} />
            <span className={styles.notificationDot} />
          </button>

          {/* User Profile */}
          <button
            type="button"
            className={styles.userProfile}
            onClick={onProfileClick}
            aria-label={`Hồ sơ người dùng ${userName}, vai trò ${formatRoleName(userRole)}`}
          >
            <Avatar src={userAvatar} name={userName} size="sm" isOnline />
            <div className={styles.userInfo}>
              <span className={styles.userName}>{userName}</span>
              <Badge
                variant={userRole === 'ADMIN' ? 'destructive' : userRole === 'TEACHER' ? 'accent' : 'primary'}
                size="sm"
              >
                {formatRoleName(userRole)}
              </Badge>
            </div>
            <Icons.ChevronDown size={14} className={styles.chevron} />
          </button>
        </div>
      </div>
    </header>
  );
}
