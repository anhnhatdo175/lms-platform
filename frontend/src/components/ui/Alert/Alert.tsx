import type { ReactNode } from 'react';
import { Icons } from '../Icons';
import styles from './Alert.module.css';

export type AlertVariant = 'info' | 'success' | 'warning' | 'destructive';

export interface AlertProps {
  variant?: AlertVariant;
  title?: ReactNode;
  children: ReactNode;
  onClose?: () => void;
  action?: ReactNode;
  className?: string;
}

export function Alert({
  variant = 'info',
  title,
  children,
  onClose,
  action,
  className = '',
}: AlertProps) {
  const getIcon = () => {
    switch (variant) {
      case 'success':
        return <Icons.CheckCircle size={20} />;
      case 'warning':
        return <Icons.AlertCircle size={20} />;
      case 'destructive':
        return <Icons.AlertCircle size={20} />;
      case 'info':
      default:
        return <Icons.Info size={20} />;
    }
  };

  return (
    <div
      role="alert"
      className={`${styles.alert} ${styles[`alert--${variant}`]} ${className}`}
    >
      <div className={styles.alert__icon} aria-hidden="true">
        {getIcon()}
      </div>

      <div className={styles.alert__content}>
        {title && <h5 className={styles.alert__title}>{title}</h5>}
        <div className={styles.alert__description}>{children}</div>
      </div>

      {action && <div className={styles.alert__action}>{action}</div>}

      {onClose && (
        <button
          type="button"
          onClick={onClose}
          className={styles.alert__close}
          aria-label="Đóng thông báo"
        >
          <Icons.Close size={16} />
        </button>
      )}
    </div>
  );
}
