import { useEffect, useRef, useState, type FormEvent } from 'react';
import {
  ApiRequestError,
  listUsers,
  updateUser,
  type ManagedUser,
  type UserDirectory,
  type UserRole,
  type UserStatus,
} from '../services/authApi';
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
  LOCKED: 'Đã khóa',
};

export function UsersPage({ accessToken, currentUserId }: UsersPageProps) {
  const [directory, setDirectory] = useState<UserDirectory | null>(null);
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
  const [editedRole, setEditedRole] = useState<UserRole>('STUDENT');
  const [editedStatus, setEditedStatus] = useState<UserStatus>('ACTIVE');
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
    const focusableSelector = 'button:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';
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
    setEditedRole(user.role);
    setEditedStatus(user.status);
  }

  async function saveUser() {
    if (!selectedUser || isSaving) return;
    if (selectedUser.id === currentUserId && editedStatus === 'LOCKED') {
      setError('Bạn không thể tự khóa tài khoản của mình.');
      return;
    }

    setIsSaving(true);
    setError('');
    try {
      const updated = await updateUser(accessToken, selectedUser.id, {
        role: editedRole !== selectedUser.role ? editedRole : undefined,
        status: editedStatus !== selectedUser.status ? editedStatus : undefined,
      });
      setDirectory((current) => current && ({
        ...current,
        items: current.items.map((user) => user.id === updated.id ? updated : user),
        activeUsers: current.activeUsers + Number(updated.status === 'ACTIVE') - Number(selectedUser.status === 'ACTIVE'),
        lockedUsers: current.lockedUsers + Number(updated.status === 'LOCKED') - Number(selectedUser.status === 'LOCKED'),
        studentUsers: current.studentUsers + Number(updated.role === 'STUDENT') - Number(selectedUser.role === 'STUDENT'),
        teacherUsers: current.teacherUsers + Number(updated.role === 'TEACHER') - Number(selectedUser.role === 'TEACHER'),
        adminUsers: current.adminUsers + Number(updated.role === 'ADMIN') - Number(selectedUser.role === 'ADMIN'),
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
        <Metric label="Tổng tài khoản" value={directory?.totalElements} isLoading={isLoading} />
        <Metric label="Đang hoạt động" value={directory?.activeUsers} isLoading={isLoading} tone="green" />
        <Metric label="Học viên" value={directory?.studentUsers} isLoading={isLoading} tone="teal" />
        <Metric label="Giảng viên" value={directory?.teacherUsers} isLoading={isLoading} tone="amber" />
      </div>

      <div className={styles.directoryCard}>
        <div className={styles.directoryHeader}>
          <div>
            <h3>Danh sách tài khoản</h3>
            <p>{directory ? `${directory.totalElements.toLocaleString('vi-VN')} tài khoản` : 'Đang tải dữ liệu'}</p>
          </div>
          <span className={styles.adminCount}>
            {directory?.adminUsers ?? '—'} quản trị viên
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
              <option value="LOCKED">Đã khóa</option>
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
              {!isLoading && !error && directory?.items.length === 0 && (
                <tr><td className={styles.tableMessage} colSpan={5}>Không tìm thấy tài khoản phù hợp.</td></tr>
              )}
              {!isLoading && directory?.items.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div className={styles.userCell}>
                      <span className={styles.avatar} aria-hidden="true">{initials(user.fullName)}</span>
                      <span className={styles.userText}>
                        <strong>{user.fullName}</strong>
                        <small>{user.email}</small>
                      </span>
                      {user.id === currentUserId && <span className={styles.youTag}>Bạn</span>}
                    </div>
                  </td>
                  <td><RoleBadge role={user.role} /></td>
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
            {directory && directory.totalElements > 0
              ? `Trang ${directory.page + 1} / ${Math.max(directory.totalPages, 1)}`
              : 'Không có kết quả'}
          </span>
          <div>
            <button
              disabled={!directory || directory.page <= 0 || isLoading}
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
              disabled={!directory || directory.totalPages === 0 || directory.page >= directory.totalPages - 1 || isLoading}
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
            <h3 id="edit-user-title">{selectedUser.fullName}</h3>
            <p className={styles.dialogEmail}>{selectedUser.email}</p>

            <label className={styles.dialogField}>
              <span>Vai trò</span>
              <select value={editedRole} onChange={(event) => setEditedRole(event.target.value as UserRole)}>
                <option value="STUDENT">Học viên</option>
                <option value="TEACHER">Giảng viên</option>
                <option value="ADMIN">Quản trị viên</option>
              </select>
            </label>
            <label className={styles.dialogField}>
              <span>Trạng thái tài khoản</span>
              <select value={editedStatus} onChange={(event) => setEditedStatus(event.target.value as UserStatus)}>
                <option value="ACTIVE">Đang hoạt động</option>
                <option value="LOCKED">Đã khóa</option>
              </select>
            </label>
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

function RoleBadge({ role }: { role: UserRole }) {
  return <span className={`${styles.roleBadge} ${styles[`role_${role.toLowerCase()}`]}`}>{roleLabels[role]}</span>;
}

function StatusBadge({ status }: { status: UserStatus }) {
  return (
    <span className={`${styles.statusBadge} ${status === 'ACTIVE' ? styles.statusActive : styles.statusLocked}`}>
      <span aria-hidden="true" />
      {statusLabels[status]}
    </span>
  );
}

function initials(name: string) {
  return name.trim().split(/\s+/).slice(-2).map((part) => part[0]?.toLocaleUpperCase('vi-VN')).join('');
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: 'short', year: 'numeric' })
    .format(new Date(value));
}
