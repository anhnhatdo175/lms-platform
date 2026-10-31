import type { HTMLAttributes } from 'react';
import styles from './Progress.module.css';

export interface ProgressProps extends HTMLAttributes<HTMLDivElement> {
  value: number; // 0 - 100
  max?: number;
  variant?: 'primary' | 'secondary' | 'accent' | 'success';
  size?: 'sm' | 'md' | 'lg';
  isLabelVisible?: boolean;
  label?: string;
}

export function Progress({
  value,
  max = 100,
  variant = 'primary',
  size = 'md',
  isLabelVisible = false,
  label,
  className = '',
  ...props
}: ProgressProps) {
  const percentage = Math.min(100, Math.max(0, Math.round((value / max) * 100)));

  return (
    <div className={`${styles.wrapper} ${className}`} {...props}>
      {(isLabelVisible || label) && (
        <div className={styles.wrapper__header}>
          {label && <span className={styles.label}>{label}</span>}
          {isLabelVisible && (
            <span className={styles.percentage}>{percentage}%</span>
          )}
        </div>
      )}

      <div
        className={`${styles.track} ${styles[`track--${size}`]}`}
        role="progressbar"
        aria-valuenow={percentage}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={label || 'Tiến độ học tập'}
      >
        <div
          className={`${styles.bar} ${styles[`bar--${variant}`]}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}
