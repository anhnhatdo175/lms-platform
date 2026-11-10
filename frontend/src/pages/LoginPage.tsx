import { useEffect, useState, type FormEvent } from 'react';
import {
  ApiRequestError,
  login,
  logout,
  register,
  refreshSession,
  restoreSession,
  updateProfile,
  type AuthUser,
} from '../services/authApi';
import { UsersPage } from './UsersPage';
import areaStyles from './UserArea.module.css';
import styles from './LoginPage.module.css';

export function LoginPage() {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [notice, setNotice] = useState('');
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRestoring, setIsRestoring] = useState(true);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [expiresIn, setExpiresIn] = useState<number | null>(null);

  useEffect(() => {
    let isCurrent = true;
    restoreSession()
      .then((session) => {
        if (isCurrent && session) {
          setUser(session.user);
          setAccessToken(session.accessToken);
          setExpiresIn(session.expiresIn);
        }
      })
      .catch((error: unknown) => {
        if (error instanceof ApiRequestError) {
          if (isCurrent) setNotice(error.message);
          return;
        }
        throw error;
      })
      .finally(() => {
        if (isCurrent) setIsRestoring(false);
      });

    return () => {
      isCurrent = false;
    };
  }, []);

  useEffect(() => {
    if (!user || !accessToken || expiresIn === null) return;
    let isCurrent = true;
    const refreshTimer = window.setTimeout(() => {
      refreshSession()
        .then((session) => {
          if (isCurrent) {
            setUser(session.user);
            setAccessToken(session.accessToken);
            setExpiresIn(session.expiresIn);
          }
        })
        .catch((error: unknown) => {
          if (isCurrent && error instanceof ApiRequestError) {
            setNotice(error.message);
            setUser(null);
            setAccessToken(null);
            setExpiresIn(null);
          } else if (!(error instanceof ApiRequestError)) {
            throw error;
          }
        });
    }, Math.max(expiresIn - 60, 1) * 1000);

    return () => {
      isCurrent = false;
      window.clearTimeout(refreshTimer);
    };
  }, [accessToken, expiresIn, user]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSubmitting) return;
    const formData = new FormData(event.currentTarget);
    const email = String(formData.get('email') ?? '');
    const password = String(formData.get('password') ?? '');

    if (mode === 'register') {
      const fullName = String(formData.get('fullName') ?? '');
      const confirmPassword = String(formData.get('confirmPassword') ?? '');
      if (password !== confirmPassword) {
        setNotice('Mật khẩu xác nhận chưa khớp.');
        return;
      }

      setIsSubmitting(true);
      setNotice('');
      try {
        const session = await register(fullName, email, password);
        setUser(session.user);
        setAccessToken(session.accessToken);
        setExpiresIn(session.expiresIn);
      } catch (error) {
        if (error instanceof ApiRequestError) {
          setNotice(error.message);
        } else {
          throw error;
        }
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    setIsSubmitting(true);
    setNotice('');
    try {
      const session = await login(email, password);
      setUser(session.user);
      setAccessToken(session.accessToken);
      setExpiresIn(session.expiresIn);
    } catch (error) {
      if (error instanceof ApiRequestError) {
        setNotice(error.message);
      } else {
        throw error;
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleLogout() {
    if (!accessToken) return;
    setIsSubmitting(true);
    setNotice('');
    try {
      await logout(accessToken);
      setUser(null);
      setAccessToken(null);
      setExpiresIn(null);
      setMode('login');
    } catch (error) {
      if (error instanceof ApiRequestError) {
        setNotice(error.message);
      } else {
        throw error;
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  if (user && accessToken) {
    return (
      <AuthenticatedArea
        accessToken={accessToken}
        isSubmitting={isSubmitting}
        notice={notice}
        onLogout={handleLogout}
        onUserUpdate={setUser}
        user={user}
      />
    );
  }

  return (
    <div className={styles.loginPage}>
      <section className={styles.brandPanel} aria-label="Giới thiệu LMS Platform">
        <a className={styles.brand} href="/" aria-label="LMS Platform">
          <span className={styles.brandIcon} aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M3.5 5.5h5a3.5 3.5 0 0 1 3.5 3.5v10a3.5 3.5 0 0 0-3.5-3.5h-5z" />
              <path d="M20.5 5.5h-5A3.5 3.5 0 0 0 12 9v10a3.5 3.5 0 0 1 3.5-3.5h5z" />
            </svg>
          </span>
          <span className={styles.brandName}>LMS<span>Platform</span></span>
        </a>

        <div className={styles.brandContent}>
          <p className={styles.eyebrow}>NỀN TẢNG HỌC TẬP TRỰC TUYẾN</p>
          <h1>
            Nâng tầm năng lực.
            <br />
            <span>Mở lối tương lai.</span>
          </h1>
          <p className={styles.brandDescription}>
            Học đúng lộ trình, theo dõi từng bước tiến và phát triển những kỹ năng
            thực sự tạo nên khác biệt.
          </p>

          <div className={styles.featureList}>
            <div className={styles.feature}>
              <span className={styles.featureNumber}>01</span>
              <span>
                <strong>Nội dung được kiểm duyệt</strong>
                <small>Kiến thức chất lượng, rõ nguồn và có cấu trúc</small>
              </span>
            </div>
            <div className={styles.feature}>
              <span className={styles.featureNumber}>02</span>
              <span>
                <strong>Lộ trình học có định hướng</strong>
                <small>Tiến từng bước với điều kiện học tập minh bạch</small>
              </span>
            </div>
            <div className={styles.feature}>
              <span className={styles.featureNumber}>03</span>
              <span>
                <strong>Tiến độ luôn trong tầm mắt</strong>
                <small>Dễ dàng tiếp tục từ nơi bạn đã dừng lại</small>
              </span>
            </div>
          </div>
        </div>

        <div className={styles.brandFooter}>
          <span className={styles.statusDot} aria-hidden="true" />
          <span>Không gian học tập dành cho hành trình dài hạn</span>
        </div>
        <span className={styles.decorativeOrb} aria-hidden="true" />
      </section>

      <main className={styles.loginPanel}>
        <div className={styles.loginContent}>
          <p className={styles.formEyebrow}>
            {mode === 'login' ? 'CỔNG HỌC TẬP' : 'TẠO TÀI KHOẢN HỌC VIÊN'}
          </p>
          <h2>{mode === 'login' ? 'Chào mừng bạn trở lại' : 'Bắt đầu hành trình học tập'}</h2>
          <p className={styles.formDescription}>
            {mode === 'login'
              ? 'Đăng nhập để tiếp tục hành trình học tập của bạn.'
              : 'Tạo tài khoản học viên để khám phá các khóa học.'}
          </p>

          <form
            aria-busy={isSubmitting || isRestoring}
            className={styles.loginForm}
            onSubmit={handleSubmit}
          >
            {mode === 'register' && (
              <div className={styles.field}>
                <label htmlFor="fullName">Họ và tên</label>
                <input
                  autoComplete="name"
                  id="fullName"
                  maxLength={120}
                  name="fullName"
                  placeholder="Nguyễn Văn An"
                  required
                  type="text"
                  onChange={() => setNotice('')}
                />
              </div>
            )}
            <div className={styles.field}>
              <label htmlFor="email">Email</label>
              <input
                autoComplete="email"
                id="email"
                maxLength={254}
                name="email"
                placeholder="name@example.com"
                required
                type="email"
                onChange={() => setNotice('')}
              />
            </div>

            <div className={styles.field}>
              <div className={styles.passwordLabel}>
                <label htmlFor="password">Mật khẩu</label>
                {mode === 'login' && (
                  <button
                    className={styles.forgotPassword}
                    onClick={() =>
                      setNotice(
                        'Để khôi phục mật khẩu, vui lòng liên hệ quản trị viên hệ thống.',
                      )
                    }
                    type="button"
                  >
                    Quên mật khẩu?
                  </button>
                )}
              </div>
              <div className={styles.passwordInput}>
                <input
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  id="password"
                  minLength={mode === 'register' ? 12 : undefined}
                  name="password"
                  placeholder={mode === 'login' ? 'Nhập mật khẩu của bạn' : 'Tối thiểu 12 ký tự'}
                  required
                  type={isPasswordVisible ? 'text' : 'password'}
                  onChange={() => setNotice('')}
                />
                <button
                  aria-label={isPasswordVisible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  aria-pressed={isPasswordVisible}
                  className={styles.visibilityButton}
                  onClick={() => setIsPasswordVisible((visible) => !visible)}
                  type="button"
                >
                  {isPasswordVisible ? (
                    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M3 3l18 18M10.6 10.6a2 2 0 0 0 2.8 2.8" />
                      <path d="M9.9 5.2A10.7 10.7 0 0 1 12 5c5.5 0 9 7 9 7a15.8 15.8 0 0 1-3.1 3.8M6.2 6.2C3.9 7.8 3 12 3 12s3.5 7 9 7a9.8 9.8 0 0 0 3.1-.5" />
                    </svg>
                  ) : (
                    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M3 12s3.5-7 9-7 9 7 9 7-3.5 7-9 7-9-7-9-7Z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            {mode === 'register' && (
              <div className={styles.field}>
                <label htmlFor="confirmPassword">Xác nhận mật khẩu</label>
                <input
                  autoComplete="new-password"
                  id="confirmPassword"
                  minLength={12}
                  name="confirmPassword"
                  placeholder="Nhập lại mật khẩu"
                  required
                  type={isPasswordVisible ? 'text' : 'password'}
                  onChange={() => setNotice('')}
                />
              </div>
            )}

            <button
              className={styles.submitButton}
              disabled={isSubmitting || isRestoring}
              type="submit"
            >
              <span>
                {isSubmitting
                  ? 'Đang xử lý...'
                  : mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'}
              </span>
              {!isSubmitting && (
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M5 12h14M13 6l6 6-6 6" />
                </svg>
              )}
            </button>
            <p className={styles.notice} aria-live="assertive" role="alert">
              {notice}
            </p>
          </form>

          <p className={styles.modeSwitch}>
            {mode === 'login' ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'}
            <button
              disabled={isSubmitting || isRestoring}
              onClick={() => {
                setMode(mode === 'login' ? 'register' : 'login');
                setNotice('');
              }}
              type="button"
            >
              {mode === 'login' ? 'Đăng ký' : 'Đăng nhập'}
            </button>
          </p>

          {isRestoring && (
            <p className={styles.restoreNotice} role="status">Đang kiểm tra phiên đăng nhập...</p>
          )}
          <div className={styles.securityNote}>
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <rect x="4" y="10" width="16" height="11" rx="2" />
              <path d="M8 10V7a4 4 0 0 1 8 0v3" />
            </svg>
            <span>Quyền truy cập được kiểm soát theo vai trò và nội dung được phân công.</span>
          </div>

          <p className={styles.supportText}>
            Cần hỗ trợ đăng nhập? Vui lòng liên hệ quản trị viên hệ thống.
          </p>
        </div>

        <footer className={styles.pageFooter}>
          <span>© {new Date().getFullYear()} LMS Platform</span>
          <span>Học tập có định hướng. Phát triển bền vững.</span>
        </footer>
      </main>
    </div>
  );
}

const roleLabels: Record<AuthUser['role'], string> = {
  ADMIN: 'Quản trị viên',
  TEACHER: 'Giảng viên',
  STUDENT: 'Học viên',
};

function AuthenticatedArea({
  accessToken,
  isSubmitting,
  notice,
  onLogout,
  onUserUpdate,
  user,
}: {
  accessToken: string;
  isSubmitting: boolean;
  notice: string;
  onLogout: () => Promise<void>;
  onUserUpdate: (user: AuthUser) => void;
  user: AuthUser;
}) {
  const [activeSection, setActiveSection] = useState<'profile' | 'users'>('profile');
  const [fullName, setFullName] = useState(user.fullName);
  const [isSaving, setIsSaving] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [profileNotice, setProfileNotice] = useState('');

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSaving) return;
    const nextName = fullName.trim();
    if (!nextName) {
      setProfileError('Họ và tên không được để trống.');
      return;
    }

    setIsSaving(true);
    setProfileError('');
    setProfileNotice('');
    try {
      const updatedProfile = await updateProfile(accessToken, nextName);
      setFullName(updatedProfile.fullName);
      onUserUpdate({ ...user, fullName: updatedProfile.fullName });
      setProfileNotice('Thông tin hồ sơ đã được cập nhật.');
    } catch (error) {
      if (error instanceof ApiRequestError) {
        setProfileError(error.message);
      } else {
        console.error('Unable to update the current user profile.', error);
        setProfileError('Đã xảy ra lỗi khi cập nhật hồ sơ. Vui lòng thử lại.');
      }
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className={areaStyles.appShell}>
      <header className={areaStyles.topbar}>
        <div className={areaStyles.brandGroup}>
          <span className={areaStyles.brandIcon} aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M3.5 5.5h5a3.5 3.5 0 0 1 3.5 3.5v10a3.5 3.5 0 0 0-3.5-3.5h-5z" />
              <path d="M20.5 5.5h-5A3.5 3.5 0 0 0 12 9v10a3.5 3.5 0 0 1 3.5-3.5h5z" />
            </svg>
          </span>
          <span className={areaStyles.brandName}>LMS<span>Platform</span></span>
        </div>

        <div className={areaStyles.accountActions}>
          <span className={areaStyles.roleBadge}>{roleLabels[user.role]}</span>
          <span className={areaStyles.avatar} aria-hidden="true">
            {user.fullName.trim().split(/\s+/).slice(-2).map((part) => part[0]?.toLocaleUpperCase('vi-VN')).join('')}
          </span>
          <button
            className={areaStyles.logoutButton}
            disabled={isSubmitting}
            onClick={() => void onLogout()}
            type="button"
          >
            {isSubmitting ? 'Đang đăng xuất...' : 'Đăng xuất'}
          </button>
        </div>
      </header>

      <nav className={areaStyles.navigation} aria-label="Điều hướng tài khoản">
        <button
          aria-current={activeSection === 'profile' ? 'page' : undefined}
          className={activeSection === 'profile' ? areaStyles.activeNav : ''}
          onClick={() => setActiveSection('profile')}
          type="button"
        >
          Hồ sơ của tôi
        </button>
        {user.role === 'ADMIN' && (
          <button
            aria-current={activeSection === 'users' ? 'page' : undefined}
            className={activeSection === 'users' ? areaStyles.activeNav : ''}
            onClick={() => setActiveSection('users')}
            type="button"
          >
            Quản lý người dùng
          </button>
        )}
      </nav>

      <main className={areaStyles.mainContent}>
        {notice && <div className={areaStyles.errorNotice} role="alert">{notice}</div>}
        {profileNotice && <div className={areaStyles.successNotice} role="status">{profileNotice}</div>}

        {activeSection === 'users' && user.role === 'ADMIN' ? (
          <UsersPage accessToken={accessToken} currentUserId={user.id} />
        ) : (
          <section className={areaStyles.profilePage} aria-labelledby="profile-title">
            <div className={areaStyles.pageHeading}>
              <p className={areaStyles.eyebrow}>TÀI KHOẢN CỦA BẠN</p>
              <h1 id="profile-title">Hồ sơ cá nhân</h1>
              <p>Quản lý thông tin hiển thị và danh tính của bạn trên LMS Platform.</p>
            </div>

            <div className={areaStyles.profileGrid}>
              <article className={areaStyles.identityCard}>
                <span className={areaStyles.largeAvatar} aria-hidden="true">
                  {user.fullName.trim().split(/\s+/).slice(-2).map((part) => part[0]?.toLocaleUpperCase('vi-VN')).join('')}
                </span>
                <h2>{user.fullName}</h2>
                <p>{user.email}</p>
                <span className={areaStyles.identityRole}>{roleLabels[user.role]}</span>
                <div className={areaStyles.identityMeta}>
                  <span className={areaStyles.activeDot} aria-hidden="true" />
                  Tài khoản đang hoạt động
                </div>
              </article>

              <form className={areaStyles.profileCard} onSubmit={saveProfile}>
                <div className={areaStyles.cardHeading}>
                  <div>
                    <h2>Thông tin cơ bản</h2>
                    <p>Cập nhật tên hiển thị trong hệ thống.</p>
                  </div>
                </div>

                <label className={areaStyles.field}>
                  <span>Họ và tên</span>
                  <input
                    autoComplete="name"
                    maxLength={120}
                    onChange={(event) => {
                      setFullName(event.target.value);
                      setProfileError('');
                      setProfileNotice('');
                    }}
                    required
                    value={fullName}
                  />
                </label>

                <label className={areaStyles.field}>
                  <span>Email</span>
                  <input autoComplete="email" readOnly value={user.email} />
                  <small>Email được dùng để đăng nhập và hiện chưa thể thay đổi.</small>
                </label>

                <div className={areaStyles.profileFooter}>
                  {profileError && <p className={areaStyles.formError} role="alert">{profileError}</p>}
                  <button className={areaStyles.saveProfile} disabled={isSaving} type="submit">
                    {isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
                  </button>
                </div>
              </form>
            </div>
          </section>
        )}
      </main>
      <footer className={areaStyles.appFooter}>
        <span>© {new Date().getFullYear()} LMS Platform</span>
        <span>Học tập có định hướng. Phát triển bền vững.</span>
      </footer>
    </div>
  );
}
