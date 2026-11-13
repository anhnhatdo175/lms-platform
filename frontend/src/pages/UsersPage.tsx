import { useEffect, useRef, useState, type FormEvent } from 'react';
import {
  ApiRequestError,
  getFileResource,
  listUsers,
  updateUser,
  type ManagedUser,
  type UserPage,
  type UserRole,
  type UserStatus,
} from '../services/authApi';
import { Avatar } from '../components/ui/Avatar';
import styles from './UsersPage.module.css';

interface UsersPageProps {
  accessToken: string;
  currentUserId: number;
}

const roleLabels: Record<UserRole, string> = {
  ADMIN: 'Quản trị viên',
  TEACHER: 'Giảng viên',
  STUDENT: 'Học viên',
};

const statusLabels: Record<UserStatus, string> = {
  ACTIVE: 'Đang hoạt động',
  INACTIVE: 'Đã vô hiệu hóa',
  BLOCKED: 'Đã chặn',
};

export function UsersPage({ accessToken, currentUserId }: UsersPageProps) {
  const [directory, setDirectory] = useState<UserPage | null>(null);
  const [queryInput, setQueryInput] = useState('');
  const [query, setQuery] = useState('');
  const [role, setRole] = useState<UserRole | ''>('');
  const [status, setStatus] = useState<UserStatus | ''>('');
  const [page, setPage] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [selectedUser, setSelectedUser] = useState<ManagedUser | null>(null);
  const [editedRoles, setEditedRoles] = useState<UserRole[]>(['STUDENT']);
  const [editedStatus, setEditedStatus] = useState<UserStatus>('ACTIVE');
  const [statusReason, setStatusReason] = useState('');
  const dialogRef = useRef<HTMLElement | null>(null);
  const editorTriggerRef = useRef<HTMLButtonElement | null>(null);
  const isSavingRef = useRef(false);

  useEffect(() => {
    let isCurrent = true;
    listUsers(accessToken, { query, role, status, page })
      .then((result) => {
        if (isCurrent) setDirectory(result);
      })
      .catch((cause: unknown) => {
        if (isCurrent) {
          if (cause instanceof ApiRequestError) {
            setError(cause.message);
          } else {
            console.error('Unable to load the user directory.', cause);
            setError('Đã xảy ra lỗi khi tải danh sách. Vui lòng thử lại.');
          }
        }
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false);
      });

    return () => {
      isCurrent = false;
    };
  }, [accessToken, page, query, refreshKey, role, status]);

  useEffect(() => {
    isSavingRef.current = isSaving;
  }, [isSaving]);

  useEffect(() => {
    if (!selectedUser || !dialogRef.current) return undefined;
    const dialog = dialogRef.current;
    const focusableSelector = 'button:not([disabled]), select:not([disabled]), input:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
    const focusableElements = () => Array.from(dialog.querySelectorAll<HTMLElement>(focusableSelector));

    focusableElements()[0]?.focus();
    function handleDialogKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault();
        if (!isSavingRef.current) setSelectedUser(null);
        return;
      }
      if (event.key !== 'Tab') return;

      const elements = focusableElements();
      const first = elements[0];
      const last = elements[elements.length - 1];
      if (!first || !last) return;
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener('keydown', handleDialogKeyDown);
    return () => {
      document.removeEventListener('keydown', handleDialogKeyDown);
      editorTriggerRef.current?.focus();
    };
  }, [selectedUser]);

  function applySearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextQuery = queryInput.trim();
    setIsLoading(true);
    setError('');
    if (nextQuery === query && page === 0) {
      setRefreshKey((current) => current + 1);
    } else {
      setPage(0);
      setQuery(nextQuery);
    }
  }

  function openEditor(user: ManagedUser, trigger: HTMLButtonElement) {
    editorTriggerRef.current = trigger;
    setSelectedUser(user);
    setEditedRoles(user.roles);
    setEditedStatus(user.status);
    setStatusReason('');
  }

  async function saveUser() {
    if (!selectedUser || isSaving) return;
    if (selectedUser.id === currentUserId && editedStatus !== 'ACTIVE') {
      setError('Bạn không thể tự vô hiệu hóa tài khoản của mình.');
      return;
    }

    setIsSaving(true);
    setError('');
    try {
      const updated = await updateUser(accessToken, selectedUser.id, {
        roles: !sameRoles(editedRoles, selectedUser.roles) ? editedRoles : undefined,
        status: editedStatus !== selectedUser.status ? editedStatus : undefined,
        reason: editedStatus !== selectedUser.status ? statusReason.trim() || undefined : undefined,
      });
      setDirectory((current) => current && ({
        ...current,
        data: current.data.map((user) => user.id === updated.id ? updated : user),
      }));
      setSelectedUser(null);
    } catch (cause) {
      if (cause instanceof ApiRequestError) {
        setError(cause.message);
      } else {
        console.error('Unable to update the selected user.', cause);
        setError('Đã xảy ra lỗi khi cập nhật tài khoản. Vui lòng thử lại.');
      }
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section className={styles.usersPage} aria-labelledby="users-title">
      <header className={styles.pageHeader}>
        <div>
          <p className={styles.eyebrow}>KHÔNG GIAN QUẢN TRỊ</p>
          <h2 id="users-title">Người dùng</h2>
          <p className={styles.pageDescription}>
            Quản lý tài khoản và vai trò trong hệ thống LMS.
          </p>
        </div>
        <span className={styles.accessNote}>
          <span aria-hidden="true" />
          Chỉ quản trị viên
        </span>
      </header>

      <div className={styles.metrics} aria-label="Thống kê người dùng">
        <Metric label="Tổng kết quả" value={directory?.meta.totalElements} isLoading={isLoading} />
        <Metric label="Kết quả trang này" value={directory?.data.length} isLoading={isLoading} tone="green" />
        <Metric label="Trang hiện tại" value={directory ? directory.meta.page + 1 : undefined} isLoading={isLoading} tone="teal" />
        <Metric label="Tổng số trang" value={directory?.meta.totalPages} isLoading={isLoading} tone="amber" />
      </div>

      <div className={styles.directoryCard}>
        <div className={styles.directoryHeader}>
          <div>
            <h3>Danh sách tài khoản</h3>
            <p>{directory ? `${directory.meta.totalElements.toLocaleString('vi-VN')} tài khoản` : 'Đang tải dữ liệu'}</p>
          </div>
          <span className={styles.adminCount}>
            {directory?.data.length ?? '—'} kết quả trang này
          </span>
        </div>

        <form className={styles.filters} onSubmit={applySearch}>
          <label className={styles.searchBox}>
            <span className={styles.srOnly}>Tìm theo tên hoặc email</span>
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="11" cy="11" r="7" />
              <path d="m16 16 4 4" />
            </svg>
            <input
              value={queryInput}
              onChange={(event) => setQueryInput(event.target.value)}
              placeholder="Tìm tên hoặc email..."
              type="search"
            />
          </label>
          <label className={styles.selectField}>
            <span className={styles.srOnly}>Lọc theo vai trò</span>
            <select
              value={role}
              onChange={(event) => {
                setRole(event.target.value as UserRole | '');
                setPage(0);
                setIsLoading(true);
                setError('');
              }}
            >
              <option value="">Tất cả vai trò</option>
              <option value="STUDENT">Học viên</option>
              <option value="TEACHER">Giảng viên</option>
              <option value="ADMIN">Quản trị viên</option>
            </select>
          </label>
          <label className={styles.selectField}>
            <span className={styles.srOnly}>Lọc theo trạng thái</span>
            <select
              value={status}
              onChange={(event) => {
                setStatus(event.target.value as UserStatus | '');
                setPage(0);
                setIsLoading(true);
                setError('');
              }}
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="INACTIVE">Đã vô hiệu hóa</option>
              <option value="BLOCKED">Đã chặn</option>
            </select>
          </label>
          <button className={styles.searchButton} type="submit">Tìm kiếm</button>
        </form>

        {error && (
          <div className={styles.errorBanner} role="alert">
            <span>{error}</span>
            <button
              type="button"
              onClick={() => {
                setIsLoading(true);
                setError('');
                setRefreshKey((current) => current + 1);
              }}
            >
              Thử tải lại
            </button>
          </div>
        )}

        <div className={styles.tableWrap} aria-busy={isLoading}>
          <table>
            <thead>
              <tr>
                <th scope="col">Người dùng</th>
                <th scope="col">Vai trò</th>
                <th scope="col">Trạng thái</th>
                <th scope="col">Ngày tham gia</th>
                <th scope="col"><span className={styles.srOnly}>Thao tác</span></th>
              </tr>
            </thead>
            <tbody>
              {isLoading && (
                <tr><td className={styles.tableMessage} colSpan={5}>Đang tải danh sách người dùng...</td></tr>
              )}
              {!isLoading && !error && directory?.data.length === 0 && (
                <tr><td className={styles.tableMessage} colSpan={5}>Không tìm thấy tài khoản phù hợp.</td></tr>
              )}
              {!isLoading && directory?.data.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div className={styles.userCell}>
                      <ManagedUserAvatar user={user} accessToken={accessToken} />
                      <span className={styles.userText}>
                        <strong>{user.displayName}</strong>
                        <small>{user.email}</small>
                      </span>
                      {user.id === currentUserId && <span className={styles.youTag}>Bạn</span>}
                    </div>
                  </td>
                  <td><RoleBadges roles={user.roles} /></td>
                  <td><StatusBadge status={user.status} /></td>
                  <td className={styles.dateCell}>{formatDate(user.createdAt)}</td>
                  <td className={styles.actionCell}>
                    <button
                      className={styles.editButton}
                      onClick={(event) => openEditor(user, event.currentTarget)}
                      type="button"
                    >
                      Chỉnh sửa
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <footer className={styles.pagination}>
          <span>
            {directory && directory.meta.totalElements > 0
              ? `Trang ${directory.meta.page + 1} / ${Math.max(directory.meta.totalPages, 1)}`
              : 'Không có kết quả'}
          </span>
          <div>
            <button
              disabled={!directory || directory.meta.page <= 0 || isLoading}
              onClick={() => {
                setIsLoading(true);
                setError('');
                setPage((current) => Math.max(0, current - 1));
              }}
              type="button"
            >
              Trước
            </button>
            <button
              disabled={!directory || directory.meta.totalPages === 0 || directory.meta.page >= directory.meta.totalPages - 1 || isLoading}
              onClick={() => {
                setIsLoading(true);
                setError('');
                setPage((current) => current + 1);
              }}
              type="button"
            >
              Tiếp
            </button>
          </div>
        </footer>
      </div>

      {selectedUser && (
        <div className={styles.modalBackdrop} onMouseDown={(event) => {
          if (event.target === event.currentTarget && !isSaving) setSelectedUser(null);
        }}>
          <section
            aria-labelledby="edit-user-title"
            aria-modal="true"
            className={styles.editDialog}
            ref={dialogRef}
            role="dialog"
          >
            <div className={styles.dialogHeader}>
              <span className={styles.dialogIcon} aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="M12 20h9" />
                  <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z" />
                </svg>
              </span>
              <button
                aria-label="Đóng"
                className={styles.closeButton}
                disabled={isSaving}
                onClick={() => setSelectedUser(null)}
                type="button"
              >
                ×
              </button>
            </div>
            <p className={styles.eyebrow}>CẬP NHẬT TÀI KHOẢN</p>
            <h3 id="edit-user-title">{selectedUser.displayName}</h3>
            <p className={styles.dialogEmail}>{selectedUser.email}</p>

            <fieldset className={`${styles.dialogField} ${styles.roleOptions}`}>
              <legend>Vai trò (có thể chọn nhiều)</legend>
              {(['STUDENT', 'TEACHER', 'ADMIN'] as const).map((role) => (
                <label key={role}>
                  <input
                    checked={editedRoles.includes(role)}
                    onChange={() => {
                      setError('');
                      if (editedRoles.includes(role) && editedRoles.length === 1) {
                        setError('Tài khoản phải có ít nhất một vai trò.');
                        return;
                      }
                      setEditedRoles(editedRoles.includes(role)
                        ? editedRoles.filter((value) => value !== role)
                        : [...editedRoles, role]);
                    }}
                    type="checkbox"
                  />
                  <span>{roleLabels[role]}</span>
                </label>
              ))}
            </fieldset>
            <label className={styles.dialogField}>
              <span>Trạng thái tài khoản</span>
              <select value={editedStatus} onChange={(event) => setEditedStatus(event.target.value as UserStatus)}>
                <option value="ACTIVE">Đang hoạt động</option>
                <option value="INACTIVE">Đã vô hiệu hóa</option>
                <option value="BLOCKED">Đã chặn</option>
              </select>
            </label>
            {editedStatus !== selectedUser.status && (
              <label className={styles.dialogField}>
                <span>Lý do thay đổi (không bắt buộc)</span>
                <textarea
                  maxLength={1000}
                  onChange={(event) => setStatusReason(event.target.value)}
                  rows={3}
                  value={statusReason}
                />
              </label>
            )}
            {error && <p className={styles.dialogError} role="alert">{error}</p>}
            <div className={styles.dialogActions}>
              <button
                className={styles.cancelButton}
                disabled={isSaving}
                onClick={() => setSelectedUser(null)}
                type="button"
              >
                Hủy
              </button>
              <button className={styles.saveButton} disabled={isSaving} onClick={saveUser} type="button">
                {isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

function Metric({
  label,
  value,
  isLoading,
  tone = 'default',
}: {
  label: string;
  value?: number;
  isLoading: boolean;
  tone?: 'default' | 'green' | 'teal' | 'amber';
}) {
  return (
    <div className={`${styles.metric} ${styles[`metric_${tone}`]}`}>
      <span>{label}</span>
      <strong>{isLoading ? '—' : (value ?? 0).toLocaleString('vi-VN')}</strong>
      <span className={styles.metricMark} aria-hidden="true" />
    </div>
  );
}

function ManagedUserAvatar({
  user,
  accessToken,
}: {
  user: ManagedUser;
  accessToken: string;
}) {
  const [avatarUrl, setAvatarUrl] = useState<string>();

  useEffect(() => {
    let isCurrent = true;
    let refreshTimer: number | undefined;
    setAvatarUrl(undefined);
    if (user.avatarResourceId === null) return undefined;

    async function refreshAvatarUrl() {
      try {
        const resource = await getFileResource(accessToken, user.avatarResourceId!);
        if (isCurrent) {
          setAvatarUrl(resource.status === 'READY' ? resource.downloadUrl ?? undefined : undefined);
          refreshTimer = window.setTimeout(refreshAvatarUrl, 4 * 60 * 1000);
        }
      } catch (cause) {
        if (isCurrent) {
          console.error(`Unable to load avatar for user ${user.id}.`, cause);
          refreshTimer = window.setTimeout(refreshAvatarUrl, 30 * 1000);
        }
      }
    }

    void refreshAvatarUrl();
    return () => {
      isCurrent = false;
      if (refreshTimer !== undefined) window.clearTimeout(refreshTimer);
    };
  }, [accessToken, user.avatarResourceId, user.id]);

  return <Avatar className={styles.avatar} name={user.displayName} size="sm" src={avatarUrl} />;
}

function RoleBadges({ roles }: { roles: UserRole[] }) {
  return (
    <span className={styles.roleList}>
      {roles.map((role) => (
        <span key={role} className={`${styles.roleBadge} ${styles[`role_${role.toLowerCase()}`]}`}>
          {roleLabels[role]}
        </span>
      ))}
    </span>
  );
}

function StatusBadge({ status }: { status: UserStatus }) {
  const statusClass = status === 'ACTIVE'
    ? styles.statusActive
    : status === 'INACTIVE' ? styles.statusInactive : styles.statusBlocked;
  return (
    <span className={`${styles.statusBadge} ${statusClass}`}>
      <span aria-hidden="true" />
      {statusLabels[status]}
    </span>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: 'short', year: 'numeric' })
    .format(new Date(value));
}

function sameRoles(left: UserRole[], right: UserRole[]) {
  return left.length === right.length && left.every((role) => right.includes(role));
}
