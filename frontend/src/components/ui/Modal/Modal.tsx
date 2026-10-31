import { useEffect, type ReactNode } from 'react';
import { Icons } from '../Icons';
import styles from './Modal.module.css';

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: ReactNode;
  description?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  isCloseOnOverlayClick?: boolean;
}

export function Modal({
  isOpen,
  onClose,
  title,
  description,
  children,
  footer,
  size = 'md',
  isCloseOnOverlayClick = true,
}: ModalProps) {
  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget && isCloseOnOverlayClick) {
      onClose();
    }
  };

  const titleId = title ? 'modal-title' : undefined;
  const descriptionId = description ? 'modal-description' : undefined;

  return (
    <div
      className={styles.overlay}
      onClick={handleOverlayClick}
      role="presentation"
    >
      <div
        className={`${styles.modal} ${styles[`modal--${size}`]}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
      >
        <div className={styles.modal__header}>
          <div className={styles['modal__header-text']}>
            {title && (
              <h2 id={titleId} className={styles.modal__title}>
                {title}
              </h2>
            )}
            {description && (
              <p id={descriptionId} className={styles.modal__description}>
                {description}
              </p>
            )}
          </div>
          <button
            type="button"
            className={styles.modal__close}
            onClick={onClose}
            aria-label="Đóng cửa sổ"
          >
            <Icons.Close size={18} />
          </button>
        </div>

        <div className={styles.modal__body}>{children}</div>

        {footer && <div className={styles.modal__footer}>{footer}</div>}
      </div>
    </div>
  );
}
