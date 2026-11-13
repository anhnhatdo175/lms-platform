import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import {
  ApiRequestError,
  changePassword,
  completeAvatarUpload,
  createAvatarUploadUrl,
  getFileResource,
  login,
  logout,
  register,
  refreshSession,
  requestPasswordReset,
  resetPassword,
  restoreSession,
  uploadAvatarFile,
  updateProfile,
  type AuthUser,
  type UserRole,
} from '../services/authApi';
import { Avatar } from '../components/ui/Avatar';
import { UsersPage } from './UsersPage';
import { AuditLogsPage } from './AuditLogsPage';
import { CourseAuthoringPage } from './CourseAuthoringPage';
import { CourseCategoriesPage } from './CourseCategoriesPage';
import areaStyles from './UserArea.module.css';
import styles from './LoginPage.module.css';

export function LoginPage() {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [notice, setNotice] = useState('');
  const [noticeIsSuccess, setNoticeIsSuccess] = useState(false);
  const [resetToken] = useState(() => new URLSearchParams(window.location.search).get('token'));
  const [mode, setMode] = useState<'login' | 'register' | 'forgot' | 'reset'>(
    () => (resetToken ? 'reset' : 'login'),
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRestoring, setIsRestoring] = useState(() => !resetToken);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [expiresIn, setExpiresIn] = useState<number | null>(null);

  useEffect(() => {
    if (resetToken) return;

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
  }, [resetToken]);

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

    if (mode === 'forgot') {
      setIsSubmitting(true);
      setNotice('');
      setNoticeIsSuccess(false);
      try {
        const result = await requestPasswordReset(email);
        setNotice(result.message);
        setNoticeIsSuccess(true);
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

    if (mode === 'reset') {
      const confirmPassword = String(formData.get('confirmPassword') ?? '');
      if (!resetToken) {
        setNotice('Liên kết đặt lại mật khẩu không hợp lệ. Hãy yêu cầu một email mới.');
        return;
      }
      if (password !== confirmPassword) {
        setNotice('Mật khẩu xác nhận chưa khớp.');
        return;
      }

      setIsSubmitting(true);
      setNotice('');
      setNoticeIsSuccess(false);
      try {
        await resetPassword(resetToken, password);
        window.history.replaceState({}, '', window.location.pathname);
        setMode('login');
        setNotice('Mật khẩu đã được cập nhật. Hãy đăng nhập bằng mật khẩu mới.');
        setNoticeIsSuccess(true);
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

    if (mode === 'register') {
      const fullName = String(formData.get('fullName') ?? '');
      const confirmPassword = String(formData.get('confirmPassword') ?? '');
      if (password !== confirmPassword) {
        setNotice('Mật khẩu xác nhận chưa khớp.');
        return;
      }

      setIsSubmitting(true);
      setNotice('');
      setNoticeIsSuccess(false);
      try {
        await register(fullName, email, password);
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
      return;
    }

    setIsSubmitting(true);
    setNotice('');
    setNoticeIsSuccess(false);
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
            {mode === 'login'
              ? 'CỔNG HỌC TẬP'
              : mode === 'register'
                ? 'TẠO TÀI KHOẢN HỌC VIÊN'
                : mode === 'forgot'
                  ? 'KHÔI PHỤC TÀI KHOẢN'
                  : 'ĐẶT MẬT KHẨU MỚI'}
          </p>
          <h2>
            {mode === 'login'
              ? 'Chào mừng bạn trở lại'
              : mode === 'register'
                ? 'Bắt đầu hành trình học tập'
                : mode === 'forgot'
                  ? 'Bạn quên mật khẩu?'
                  : 'Tạo mật khẩu mới'}
          </h2>
          <p className={styles.formDescription}>
            {mode === 'login'
              ? 'Đăng nhập để tiếp tục hành trình học tập của bạn.'
              : mode === 'register'
                ? 'Tạo tài khoản học viên để khám phá các khóa học.'
                : mode === 'forgot'
                  ? 'Nhập email đã đăng ký. Nếu tài khoản đang hoạt động, chúng tôi sẽ gửi liên kết khôi phục.'
                  : 'Liên kết chỉ dùng được một lần và sẽ hết hạn sau 30 phút.'}
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
                  maxLength={255}
                  name="fullName"
                  placeholder="Nguyễn Văn An"
                  required
                  type="text"
                  onChange={() => setNotice('')}
                />
              </div>
            )}
            {mode !== 'reset' && (
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
                onChange={() => {
                  setNotice('');
                  setNoticeIsSuccess(false);
                }}
              />
              </div>
            )}

            {mode !== 'forgot' && (
              <div className={styles.field}>
              <div className={styles.passwordLabel}>
                <label htmlFor="password">Mật khẩu</label>
                {mode === 'login' && (
                  <button
                    className={styles.forgotPassword}
                    onClick={() => {
                      setMode('forgot');
                      setNotice('');
                      setNoticeIsSuccess(false);
                    }}
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
                  minLength={mode === 'register' || mode === 'reset' ? 8 : undefined}
                  maxLength={mode === 'login' ? undefined : 72}
                  name="password"
                  placeholder={mode === 'login' ? 'Nhập mật khẩu của bạn' : 'Tối thiểu 8 ký tự'}
                  required
                  type={isPasswordVisible ? 'text' : 'password'}
                  onChange={() => {
                    setNotice('');
                    setNoticeIsSuccess(false);
                  }}
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
            )}

            {(mode === 'register' || mode === 'reset') && (
              <div className={styles.field}>
                <label htmlFor="confirmPassword">Xác nhận mật khẩu</label>
                <input
                  autoComplete="new-password"
                  id="confirmPassword"
                  minLength={8}
                  maxLength={72}
                  name="confirmPassword"
                  placeholder="Nhập lại mật khẩu"
                  required
                  type={isPasswordVisible ? 'text' : 'password'}
                  onChange={() => {
                    setNotice('');
                    setNoticeIsSuccess(false);
                  }}
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
                  : mode === 'login'
                    ? 'Đăng nhập'
                    : mode === 'register'
                      ? 'Tạo tài khoản'
                      : mode === 'forgot'
                        ? 'Gửi hướng dẫn'
                        : 'Cập nhật mật khẩu'}
              </span>
              {!isSubmitting && (
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M5 12h14M13 6l6 6-6 6" />
                </svg>
              )}
            </button>
            <p
              className={`${styles.notice} ${noticeIsSuccess ? styles.noticeSuccess : ''}`}
              aria-live={noticeIsSuccess ? 'polite' : 'assertive'}
              role={noticeIsSuccess ? 'status' : 'alert'}
            >
              {notice}
            </p>
          </form>

          <p className={styles.modeSwitch}>
            {mode === 'login' || mode === 'register'
              ? mode === 'login' ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'
              : 'Đã nhớ mật khẩu?'}
            <button
              disabled={isSubmitting || isRestoring}
              onClick={() => {
                setMode(mode === 'login' ? 'register' : 'login');
                setNotice('');
                setNoticeIsSuccess(false);
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

const roleLabels: Record<UserRole, string> = {
  ADMIN: 'Quản trị viên',
  TEACHER: 'Giảng viên',
  STUDENT: 'Học viên',
};

function formatRoles(roles: UserRole[]) {
  return roles.map((role) => roleLabels[role]).join(' · ');
}

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
  const [activeSection, setActiveSection] = useState<
    'profile' | 'security' | 'users' | 'audit' | 'courses' | 'course-categories'
  >('profile');
  const [fullName, setFullName] = useState(user.displayName);
  const [isSaving, setIsSaving] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [profileNotice, setProfileNotice] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [passwordNotice, setPasswordNotice] = useState('');
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [avatarError, setAvatarError] = useState('');
  const [loadedAvatar, setLoadedAvatar] = useState<{ id: number; url: string } | null>(null);
  const avatarInputRef = useRef<HTMLInputElement>(null);
  const avatarUrl = loadedAvatar?.id === user.avatarResourceId ? loadedAvatar.url : undefined;

  useEffect(() => {
    if (!user.avatarResourceId) return undefined;
    let isCurrent = true;
    let refreshTimer: number | undefined;

    async function refreshAvatarUrl() {
      try {
        const resource = await getFileResource(accessToken, user.avatarResourceId!);
        if (isCurrent && resource.downloadUrl) {
          setLoadedAvatar({ id: user.avatarResourceId!, url: resource.downloadUrl });
          setAvatarError('');
        }
      } catch (error) {
        if (isCurrent) {
          if (error instanceof ApiRequestError) {
            setAvatarError(error.message);
          } else {
            console.error('Unable to load the current user avatar.', error);
            setAvatarError('Không thể tải ảnh đại diện. Vui lòng thử lại.');
          }
        }
      } finally {
        if (isCurrent) {
          refreshTimer = window.setTimeout(() => void refreshAvatarUrl(), 4 * 60 * 1000);
        }
      }
    }

    void refreshAvatarUrl();
    return () => {
      isCurrent = false;
      if (refreshTimer !== undefined) window.clearTimeout(refreshTimer);
    };
  }, [accessToken, user.avatarResourceId]);

  async function handleAvatarSelection(event: ChangeEvent<HTMLInputElement>) {
    const file = event.currentTarget.files?.[0];
    event.currentTarget.value = '';
    if (!file) return;
    setAvatarError('');
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setAvatarError('Ảnh đại diện chỉ hỗ trợ JPEG, PNG hoặc WebP.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setAvatarError('Ảnh đại diện không được vượt quá 5 MiB.');
      return;
    }

    setIsUploadingAvatar(true);
    setProfileNotice('');
    try {
      const upload = await createAvatarUploadUrl(accessToken, file);
      await uploadAvatarFile(upload, file);
      const resource = await completeAvatarUpload(accessToken, upload.uploadId, file);
      const updatedProfile = await updateProfile(accessToken, {
        avatarResourceId: resource.id,
      });
      onUserUpdate(updatedProfile);
      setProfileNotice('Ảnh đại diện đã được cập nhật.');
    } catch (error) {
      if (error instanceof ApiRequestError) {
        setAvatarError(error.message);
      } else {
        console.error('Unable to update the current user avatar.', error);
        setAvatarError('Đã xảy ra lỗi khi cập nhật ảnh đại diện. Vui lòng thử lại.');
      }
    } finally {
      setIsUploadingAvatar(false);
    }
  }

  async function removeAvatar() {
    if (isUploadingAvatar || !user.avatarResourceId) return;
    setIsUploadingAvatar(true);
    setAvatarError('');
    setProfileNotice('');
    try {
      const updatedProfile = await updateProfile(accessToken, { avatarResourceId: null });
      onUserUpdate(updatedProfile);
      setProfileNotice('Ảnh đại diện đã được gỡ.');
    } catch (error) {
      if (error instanceof ApiRequestError) {
        setAvatarError(error.message);
      } else {
        console.error('Unable to remove the current user avatar.', error);
        setAvatarError('Đã xảy ra lỗi khi gỡ ảnh đại diện. Vui lòng thử lại.');
      }
    } finally {
      setIsUploadingAvatar(false);
    }
  }

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
      const updatedProfile = await updateProfile(accessToken, { displayName: nextName });
      setFullName(updatedProfile.displayName);
      onUserUpdate({ ...user, displayName: updatedProfile.displayName });
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

  async function savePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isChangingPassword) return;
    const form = event.currentTarget;
    const formData = new FormData(form);
    const currentPassword = String(formData.get('currentPassword') ?? '');
    const newPassword = String(formData.get('newPassword') ?? '');
    const confirmPassword = String(formData.get('confirmNewPassword') ?? '');
    if (newPassword !== confirmPassword) {
      setPasswordError('Mật khẩu xác nhận chưa khớp.');
      setPasswordNotice('');
      return;
    }

    setIsChangingPassword(true);
    setPasswordError('');
    setPasswordNotice('');
    try {
      await changePassword(accessToken, currentPassword, newPassword);
      setPasswordNotice('Mật khẩu đã được cập nhật. Hãy đăng xuất và đăng nhập lại trên các thiết bị đang sử dụng.');
      form.reset();
    } catch (error) {
      setPasswordNotice('');
      if (error instanceof ApiRequestError) {
        setPasswordError(error.message);
      } else {
        console.error('Unable to change the current user password.', error);
        setPasswordError('Đã xảy ra lỗi khi đổi mật khẩu. Vui lòng thử lại.');
      }
    } finally {
      setIsChangingPassword(false);
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
          <span className={areaStyles.roleBadge}>{formatRoles(user.roles)}</span>
          <Avatar
            className={areaStyles.avatar}
            name={user.displayName}
            size="sm"
            src={avatarUrl}
          />
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
        <button
          aria-current={activeSection === 'security' ? 'page' : undefined}
          className={activeSection === 'security' ? areaStyles.activeNav : ''}
          onClick={() => setActiveSection('security')}
          type="button"
        >
          Bảo mật
        </button>
        {user.roles.includes('TEACHER') && (
          <button
            aria-current={activeSection === 'courses' ? 'page' : undefined}
            className={activeSection === 'courses' ? areaStyles.activeNav : ''}
            onClick={() => setActiveSection('courses')}
            type="button"
          >
            Soạn khóa học
          </button>
        )}
        {user.roles.includes('ADMIN') && (
          <>
            <button
              aria-current={activeSection === 'course-categories' ? 'page' : undefined}
              className={activeSection === 'course-categories' ? areaStyles.activeNav : ''}
              onClick={() => setActiveSection('course-categories')}
              type="button"
            >
              Danh mục khóa học
            </button>
            <button
              aria-current={activeSection === 'users' ? 'page' : undefined}
              className={activeSection === 'users' ? areaStyles.activeNav : ''}
              onClick={() => setActiveSection('users')}
              type="button"
            >
              Quản lý người dùng
            </button>
            <button
              aria-current={activeSection === 'audit' ? 'page' : undefined}
              className={activeSection === 'audit' ? areaStyles.activeNav : ''}
              onClick={() => setActiveSection('audit')}
              type="button"
            >
              Nhật ký hoạt động
            </button>
          </>
        )}
      </nav>

      <main className={areaStyles.mainContent}>
        {notice && <div className={areaStyles.errorNotice} role="alert">{notice}</div>}
        {profileNotice && <div className={areaStyles.successNotice} role="status">{profileNotice}</div>}

        {activeSection === 'course-categories' && user.roles.includes('ADMIN') ? (
          <CourseCategoriesPage accessToken={accessToken} />
        ) : activeSection === 'courses' && user.roles.includes('TEACHER') ? (
          <CourseAuthoringPage accessToken={accessToken} />
        ) : activeSection === 'users' && user.roles.includes('ADMIN') ? (
          <UsersPage accessToken={accessToken} currentUserId={user.id} />
        ) : activeSection === 'audit' && user.roles.includes('ADMIN') ? (
          <AuditLogsPage accessToken={accessToken} />
        ) : activeSection === 'security' ? (
          <section className={areaStyles.profilePage} aria-labelledby="security-title">
            <div className={areaStyles.pageHeading}>
              <p className={areaStyles.eyebrow}>BẢO VỆ TÀI KHOẢN</p>
              <h1 id="security-title">Bảo mật</h1>
              <p>Đổi mật khẩu để giữ an toàn cho tài khoản LMS Platform của bạn.</p>
            </div>

            <form className={areaStyles.profileCard} onSubmit={savePassword}>
              <div className={areaStyles.cardHeading}>
                <div>
                  <h2>Đổi mật khẩu</h2>
                  <p>Mật khẩu mới cần có ít nhất 8 ký tự. Các phiên đăng nhập khác sẽ bị thu hồi.</p>
                </div>
              </div>

              <label className={areaStyles.field}>
                <span>Mật khẩu hiện tại</span>
                <input
                  autoComplete="current-password"
                  name="currentPassword"
                  required
                  type="password"
                  onChange={() => {
                    setPasswordError('');
                    setPasswordNotice('');
                  }}
                />
              </label>

              <label className={areaStyles.field}>
                <span>Mật khẩu mới</span>
                <input
                  autoComplete="new-password"
                  minLength={8}
                  maxLength={72}
                  name="newPassword"
                  required
                  type="password"
                  onChange={() => {
                    setPasswordError('');
                    setPasswordNotice('');
                  }}
                />
              </label>

              <label className={areaStyles.field}>
                <span>Xác nhận mật khẩu mới</span>
                <input
                  autoComplete="new-password"
                  minLength={8}
                  maxLength={72}
                  name="confirmNewPassword"
                  required
                  type="password"
                  onChange={() => {
                    setPasswordError('');
                    setPasswordNotice('');
                  }}
                />
              </label>

              <div className={areaStyles.profileFooter}>
                {passwordError && <p className={areaStyles.formError} role="alert">{passwordError}</p>}
                {passwordNotice && <p className={areaStyles.successNotice} role="status">{passwordNotice}</p>}
                <button className={areaStyles.saveProfile} disabled={isChangingPassword} type="submit">
                  {isChangingPassword ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
                </button>
              </div>
            </form>
          </section>
        ) : (
          <section className={areaStyles.profilePage} aria-labelledby="profile-title">
            <div className={areaStyles.pageHeading}>
              <p className={areaStyles.eyebrow}>TÀI KHOẢN CỦA BẠN</p>
              <h1 id="profile-title">Hồ sơ cá nhân</h1>
              <p>Quản lý thông tin hiển thị và danh tính của bạn trên LMS Platform.</p>
            </div>

            <div className={areaStyles.profileGrid}>
              <article className={areaStyles.identityCard}>
                <Avatar
                  className={areaStyles.largeAvatar}
                  name={user.displayName}
                  size="lg"
                  src={avatarUrl}
                />
                <h2>{user.displayName}</h2>
                <p>{user.email}</p>
                <span className={areaStyles.identityRole}>{formatRoles(user.roles)}</span>
                <div className={areaStyles.identityMeta}>
                  <span className={areaStyles.activeDot} aria-hidden="true" />
                  Tài khoản đang hoạt động
                </div>
              </article>

              <form className={areaStyles.profileCard} onSubmit={saveProfile}>
                <div className={areaStyles.cardHeading}>
                  <div>
                    <h2>Thông tin cơ bản</h2>
                    <p>Cập nhật tên hiển thị và ảnh đại diện của bạn.</p>
                  </div>
                </div>

                <div className={areaStyles.avatarEditor}>
                  <Avatar
                    className={areaStyles.largeAvatar}
                    name={user.displayName}
                    size="lg"
                    src={avatarUrl}
                  />
                  <div className={areaStyles.avatarActions}>
                    <input
                      ref={avatarInputRef}
                      accept="image/jpeg,image/png,image/webp"
                      aria-hidden="true"
                      className={areaStyles.hiddenFileInput}
                      disabled={isUploadingAvatar}
                      onChange={(event) => void handleAvatarSelection(event)}
                      tabIndex={-1}
                      type="file"
                    />
                    <button
                      className={areaStyles.saveProfile}
                      disabled={isUploadingAvatar}
                      onClick={() => avatarInputRef.current?.click()}
                      type="button"
                    >
                      {isUploadingAvatar ? 'Đang tải ảnh...' : 'Tải ảnh lên'}
                    </button>
                    {user.avatarResourceId && (
                      <button
                        className={areaStyles.removeAvatar}
                        disabled={isUploadingAvatar}
                        onClick={() => void removeAvatar()}
                        type="button"
                      >
                        Gỡ ảnh
                      </button>
                    )}
                    <small>JPEG, PNG hoặc WebP · tối đa 5 MiB</small>
                  </div>
                </div>
                {avatarError && <p className={areaStyles.formError} role="alert">{avatarError}</p>}

                <label className={areaStyles.field}>
                  <span>Họ và tên</span>
                  <input
                    autoComplete="name"
                    maxLength={255}
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
