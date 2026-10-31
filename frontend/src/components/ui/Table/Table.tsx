import type { ReactNode } from 'react';
import styles from './Table.module.css';

export interface Column<T> {
  key: string;
  header: ReactNode;
  render?: (row: T, index: number) => ReactNode;
  align?: 'left' | 'center' | 'right';
  width?: string;
}

export interface TablePaginationMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TableProps<T> {
  columns: Column<T>[];
  data: T[];
  isLoading?: boolean;
  emptyMessage?: ReactNode;
  onRowClick?: (row: T) => void;
  pagination?: {
    meta: TablePaginationMeta;
    onPageChange: (page: number) => void;
  };
  className?: string;
}

export function Table<T extends Record<string, unknown>>({
  columns,
  data,
  isLoading = false,
  emptyMessage = 'Không có dữ liệu',
  onRowClick,
  pagination,
  className = '',
}: TableProps<T>) {
  return (
    <div className={`${styles.container} ${className}`}>
      <div className={styles['table-wrapper']}>
        <table className={styles.table}>
          <thead className={styles.thead}>
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={`${styles.th} ${styles[`th--${col.align || 'left'}`]}`}
                  style={{ width: col.width }}
                  scope="col"
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className={styles.tbody}>
            {isLoading ? (
              <tr>
                <td colSpan={columns.length} className={styles['td--loading']}>
                  <div className={styles.loading}>
                    <span className={styles.spinner} aria-hidden="true" />
                    <span>Đang tải dữ liệu…</span>
                  </div>
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className={styles['td--empty']}>
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              data.map((row, rowIndex) => (
                <tr
                  key={(row.id as string | number) ?? rowIndex}
                  className={[
                    styles.tr,
                    onRowClick ? styles['tr--clickable'] : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => onRowClick?.(row)}
                >
                  {columns.map((col) => {
                    const value = col.render
                      ? col.render(row, rowIndex)
                      : (row[col.key] as ReactNode);
                    return (
                      <td
                        key={col.key}
                        className={`${styles.td} ${styles[`td--${col.align || 'left'}`]}`}
                      >
                        {value}
                      </td>
                    );
                  })}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {pagination && (
        <div className={styles.pagination}>
          <div className={styles.pagination__info}>
            Hiển thị trang <strong>{pagination.meta.page + 1}</strong> /{' '}
            <strong>{Math.max(1, pagination.meta.totalPages)}</strong> (Tổng{' '}
            <strong>{pagination.meta.totalElements}</strong> mục)
          </div>
          <div className={styles.pagination__controls}>
            <button
              type="button"
              className={styles.pagination__btn}
              disabled={pagination.meta.page <= 0 || isLoading}
              onClick={() => pagination.onPageChange(pagination.meta.page - 1)}
            >
              Trang trước
            </button>
            <button
              type="button"
              className={styles.pagination__btn}
              disabled={
                pagination.meta.page >= pagination.meta.totalPages - 1 || isLoading
              }
              onClick={() => pagination.onPageChange(pagination.meta.page + 1)}
            >
              Trang sau
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
