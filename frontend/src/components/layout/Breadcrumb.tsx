import type { ReactNode } from 'react';
import { Icons } from '../ui/Icons';
import styles from './Breadcrumb.module.css';

export interface BreadcrumbItem {
  label: string;
  href?: string;
  onClick?: () => void;
  icon?: ReactNode;
}

export interface BreadcrumbProps {
  items: BreadcrumbItem[];
  className?: string;
}

export function Breadcrumb({ items, className = '' }: BreadcrumbProps) {
  return (
    <nav aria-label="Đường dẫn trang" className={`${styles.nav} ${className}`}>
      <ol className={styles.list}>
        {items.map((item, index) => {
          const isLast = index === items.length - 1;
          return (
            <li key={item.label} className={styles.item}>
              {index > 0 && (
                <span className={styles.separator} aria-hidden="true">
                  <Icons.ChevronRight size={14} />
                </span>
              )}

              {isLast ? (
                <span
                  className={styles.current}
                  aria-current="page"
                >
                  {item.icon && <span className={styles.icon}>{item.icon}</span>}
                  {item.label}
                </span>
              ) : (
                <button
                  type="button"
                  className={styles.link}
                  onClick={item.onClick}
                >
                  {item.icon && <span className={styles.icon}>{item.icon}</span>}
                  {item.label}
                </button>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
