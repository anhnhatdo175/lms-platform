import type { ReactNode, HTMLAttributes } from 'react';
import styles from './Card.module.css';

type CardVariant = 'default' | 'elevated' | 'outlined' | 'ghost';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: CardVariant;
  isHoverable?: boolean;
  isClickable?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

interface CardHeaderProps extends HTMLAttributes<HTMLDivElement> {
  title?: ReactNode;
  subtitle?: ReactNode;
  action?: ReactNode;
}

interface CardBodyProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

interface CardFooterProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export function Card({
  variant = 'default',
  isHoverable = false,
  isClickable = false,
  padding = 'md',
  className = '',
  children,
  ...props
}: CardProps) {
  const classes = [
    styles.card,
    styles[`card--${variant}`],
    isHoverable ? styles['card--hoverable'] : '',
    isClickable ? styles['card--clickable'] : '',
    padding !== 'none' ? styles[`card--pad-${padding}`] : '',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={classes} {...props}>
      {children}
    </div>
  );
}

export function CardHeader({ title, subtitle, action, className = '', ...props }: CardHeaderProps) {
  return (
    <div className={`${styles.card__header} ${className}`} {...props}>
      <div className={styles['card__header-content']}>
        {title && <div className={styles['card__title']}>{title}</div>}
        {subtitle && <div className={styles['card__subtitle']}>{subtitle}</div>}
      </div>
      {action && <div className={styles['card__action']}>{action}</div>}
    </div>
  );
}

export function CardBody({ children, className = '', ...props }: CardBodyProps) {
  return (
    <div className={`${styles.card__body} ${className}`} {...props}>
      {children}
    </div>
  );
}

export function CardFooter({ children, className = '', ...props }: CardFooterProps) {
  return (
    <div className={`${styles.card__footer} ${className}`} {...props}>
      {children}
    </div>
  );
}
