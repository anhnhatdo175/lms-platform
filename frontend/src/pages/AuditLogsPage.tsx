import { useEffect, useState, type FormEvent } from 'react';
import {
  ApiRequestError,
  listAuditLogs,
  type AuditLogEntry,
  type AuditLogPage,
} from '../services/authApi';
import styles from './AuditLogsPage.module.css';

interface AuditLogsPageProps {
  accessToken: string;
}

interface AuditFilters {
  action: string;
  entityType: string;
  actorUserId: string;
  from: string;
  to: string;
  page: number;
}

const emptyFilters: AuditFilters = {
  action: '',
  entityType: '',
  actorUserId: '',
  from: '',
  to: '',
  page: 0,
};

export function AuditLogsPage({ accessToken }: AuditLogsPageProps) {
  const [page, setPage] = useState<AuditLogPage | null>(null);
  const [filters, setFilters] = useState(emptyFilters);
  const [actionInput, setActionInput] = useState('');
  const [entityInput, setEntityInput] = useState('');
  const [actorInput, setActorInput] = useState('');
  const [fromInput, setFromInput] = useState('');
  const [toInput, setToInput] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    listAuditLogs(accessToken, {
      action: filters.action,
      entityType: filters.entityType,
      actorUserId: filters.actorUserId ? Number(filters.actorUserId) : undefined,
      from: filters.from ? startOfDay(filters.from) : undefined,
      to: filters.to ? startOfNextDay(filters.to) : undefined,
      page: filters.page,
    })
      .then((result) => {
        if (active) {
          setPage(result);
          setError('');
        }
      })
      .catch((cause: unknown) => {
        if (!active) return;
        if (cause instanceof ApiRequestError) {
          setError(cause.message);
        } else {
          console.error('Unable to load audit logs.', cause);
          setError('Đã xảy ra lỗi khi tải nhật ký. Vui lòng thử lại.');
        }
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [accessToken, filters]);

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (actorInput && (!Number.isInteger(Number(actorInput)) || Number(actorInput) < 1)) {
      setError('Mã người thực hiện phải là số nguyên dương.');
      return;
    }
    if (fromInput && toInput && fromInput > toInput) {
      setError('Ngày bắt đầu không thể sau ngày kết thúc.');
      return;
    }
    setError('');
    setIsLoading(true);
    setFilters({
      action: actionInput.trim(),
      entityType: entityInput.trim(),
      actorUserId: actorInput.trim(),
      from: fromInput,
      to: toInput,
      page: 0,
    });
  }

  function setCurrentPage(nextPage: number) {
    setIsLoading(true);
    setFilters((current) => ({ ...current, page: nextPage }));
  }

  return (
    <section className={styles.auditPage} aria-labelledby="audit-title">
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>KIỂM SOÁT VÀ MINH BẠCH</p>
          <h1 id="audit-title">Nhật ký hoạt động</h1>
          <p>Theo dõi các thay đổi quản trị và dấu vết thao tác trên hệ thống.</p>
        </div>
        <span className={styles.accessNote}>Chỉ quản trị viên</span>
      </header>

      <form className={styles.filterCard} onSubmit={applyFilters}>
        <label>
          <span>Hành động</span>
          <input
            onChange={(event) => setActionInput(event.target.value)}
            placeholder="Ví dụ: USER_STATUS_UPDATED"
            value={actionInput}
          />
        </label>
        <label>
          <span>Loại tài nguyên</span>
          <input
            onChange={(event) => setEntityInput(event.target.value)}
            placeholder="Ví dụ: USER"
            value={entityInput}
          />
        </label>
        <label>
          <span>Mã người thực hiện</span>
          <input
            inputMode="numeric"
            onChange={(event) => setActorInput(event.target.value)}
            value={actorInput}
          />
        </label>
        <label>
          <span>Từ ngày</span>
          <input onChange={(event) => setFromInput(event.target.value)} type="date" value={fromInput} />
        </label>
        <label>
          <span>Đến ngày</span>
          <input onChange={(event) => setToInput(event.target.value)} type="date" value={toInput} />
        </label>
        <button disabled={isLoading} type="submit">Lọc nhật ký</button>
      </form>

      {error && <div className={styles.error} role="alert">{error}</div>}

      <div className={styles.logCard} aria-busy={isLoading}>
        <div className={styles.logHeading}>
          <div>
            <h2>Hoạt động gần đây</h2>
            <p>{page ? `${page.meta.totalElements.toLocaleString('vi-VN')} bản ghi` : 'Đang tải dữ liệu'}</p>
          </div>
        </div>
        <div className={styles.tableWrap}>
          <table>
            <thead>
              <tr>
                <th scope="col">Thời gian</th>
                <th scope="col">Người thực hiện</th>
                <th scope="col">Hành động</th>
                <th scope="col">Tài nguyên</th>
                <th scope="col">Thay đổi</th>
                <th scope="col">Lý do</th>
                <th scope="col">IP</th>
              </tr>
            </thead>
            <tbody>
              {isLoading && <tr><td className={styles.message} colSpan={7}>Đang tải nhật ký...</td></tr>}
              {!isLoading && !error && page?.data.length === 0 && (
                <tr><td className={styles.message} colSpan={7}>Không tìm thấy hoạt động phù hợp.</td></tr>
              )}
              {!isLoading && page?.data.map((entry) => <AuditRow key={entry.id} entry={entry} />)}
            </tbody>
          </table>
        </div>
        <footer className={styles.pagination}>
          <span>
            {page && page.meta.totalElements > 0
              ? `Trang ${page.meta.page + 1} / ${Math.max(page.meta.totalPages, 1)}`
              : 'Không có kết quả'}
          </span>
          <div>
            <button
              disabled={!page || page.meta.page === 0 || isLoading}
              onClick={() => setCurrentPage(Math.max(0, (page?.meta.page ?? 0) - 1))}
              type="button"
            >
              Trước
            </button>
            <button
              disabled={!page || page.meta.totalPages === 0 || page.meta.page >= page.meta.totalPages - 1 || isLoading}
              onClick={() => setCurrentPage((page?.meta.page ?? 0) + 1)}
              type="button"
            >
              Tiếp
            </button>
          </div>
        </footer>
      </div>
    </section>
  );
}

function AuditRow({ entry }: { entry: AuditLogEntry }) {
  return (
    <tr>
      <td className={styles.dateCell}>{formatDate(entry.createdAt)}</td>
      <td>
        <strong>{entry.actorEmail ?? 'Người dùng đã xóa'}</strong>
        {entry.actorUserId && <small className={styles.secondary}>ID {entry.actorUserId}</small>}
      </td>
      <td><span className={styles.action}>{entry.action}</span></td>
      <td>{entry.entityType}{entry.entityId ? ` #${entry.entityId}` : ''}</td>
      <td>
        <ChangeSummary oldValue={entry.oldValue} newValue={entry.newValue} />
      </td>
      <td>{entry.reason ?? '—'}</td>
      <td>{entry.ipAddress ?? '—'}</td>
    </tr>
  );
}

function ChangeSummary({
  oldValue,
  newValue,
}: {
  oldValue: Record<string, unknown> | null;
  newValue: Record<string, unknown> | null;
}) {
  if (!oldValue && !newValue) return <span>—</span>;
  return (
    <details className={styles.changeDetails}>
      <summary>Xem chi tiết</summary>
      <pre>{JSON.stringify({ oldValue, newValue }, null, 2)}</pre>
    </details>
  );
}

function startOfDay(date: string) {
  return new Date(`${date}T00:00:00`).toISOString();
}

function startOfNextDay(date: string) {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(year, month - 1, day + 1).toISOString();
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}
