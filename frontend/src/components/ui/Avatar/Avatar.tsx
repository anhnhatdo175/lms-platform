import { useState, type HTMLAttributes } from 'react';
import { Icons } from '../Icons';
import styles from './Avatar.module.css';

export interface AvatarProps extends HTMLAttributes<HTMLDivElement> {
  src?: string;
  name?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  isOnline?: boolean;
}

export function Avatar({
  src,
  name = '',
  size = 'md',
  isOnline,
  className = '',
  ...props
}: AvatarProps) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null);

  const getInitials = (text: string) => {
    if (!text.trim()) return '';
    const parts = text.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  const initials = getInitials(name);

  return (
    <div
      className={`${styles.avatar} ${styles[`avatar--${size}`]} ${className}`}
      {...props}
    >
      {src && failedSrc !== src ? (
        <img
          src={src}
          alt={name || 'Avatar người dùng'}
          className={styles.avatar__img}
          width="80"
          height="80"
          loading="lazy"
          onError={() => setFailedSrc(src)}
        />
      ) : initials ? (
        <span className={styles.avatar__initials}>{initials}</span>
      ) : (
        <span className={styles.avatar__fallback}>
          <Icons.User size={size === 'sm' ? 14 : size === 'lg' ? 24 : size === 'xl' ? 32 : 18} />
        </span>
      )}

      {isOnline !== undefined && (
        <span
          className={[
            styles.avatar__status,
            isOnline ? styles['avatar__status--online'] : styles['avatar__status--offline'],
          ].join(' ')}
          aria-label={isOnline ? 'Đang trực tuyến' : 'Ngoại tuyến'}
        />
      )}
    </div>
  );
}
