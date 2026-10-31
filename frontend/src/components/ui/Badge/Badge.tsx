import type { HTMLAttributes, ReactNode } from 'react';
import styles from './Badge.module.css';

export type BadgeVariant =
  | 'default'
  | 'primary'
  | 'secondary'
  | 'success'
  | 'warning'
  | 'destructive'
  | 'info'
  | 'accent';

export type BadgeSize = 'sm' | 'md' | 'lg';

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  size?: BadgeSize;
  hasDot?: boolean;
  leftIcon?: ReactNode;
  children: ReactNode;
}

export function Badge({
  variant = 'default',
  size = 'md',
  hasDot = false,
  leftIcon,
  className = '',
  children,
  ...props
}: BadgeProps) {
  const classes = [
    styles.badge,
    styles[`badge--${variant}`],
    styles[`badge--${size}`],
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <span className={classes} {...props}>
      {hasDot && <span className={styles['badge__dot']} aria-hidden="true" />}
      {leftIcon && <span className={styles['badge__icon']} aria-hidden="true">{leftIcon}</span>}
      <span className={styles['badge__label']}>{children}</span>
    </span>
  );
}
