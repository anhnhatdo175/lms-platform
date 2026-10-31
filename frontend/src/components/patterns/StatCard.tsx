import type { ReactNode } from 'react';
import styles from './StatCard.module.css';

export interface StatCardProps {
  title: string;
  value: string | number;
  icon: ReactNode;
  trend?: {
    value: string;
    isPositive?: boolean;
    label?: string;
  };
  variant?: 'primary' | 'secondary' | 'accent' | 'success';
}

export function StatCard({
  title,
  value,
  icon,
  trend,
  variant = 'primary',
}: StatCardProps) {
  return (
    <div className={styles.card}>
      <div className={styles.topRow}>
        <span className={styles.title}>{title}</span>
        <div className={`${styles.iconWrapper} ${styles[`iconWrapper--${variant}`]}`}>
          {icon}
        </div>
      </div>

      <div className={styles.value}>{value}</div>

      {trend && (
        <div className={styles.trendRow}>
          <span
            className={[
              styles.trendValue,
              trend.isPositive ? styles['trendValue--positive'] : styles['trendValue--negative'],
            ].join(' ')}
          >
            {trend.isPositive ? '↑' : '↓'} {trend.value}
          </span>
          {trend.label && <span className={styles.trendLabel}>{trend.label}</span>}
        </div>
      )}
    </div>
  );
}
