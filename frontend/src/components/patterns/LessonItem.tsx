import { Icons } from '../ui/Icons';
import { Badge } from '../ui/Badge';
import styles from './LessonItem.module.css';

export type LessonType = 'VIDEO' | 'ARTICLE' | 'QUIZ';

export interface LessonItemProps {
  id: number;
  title: string;
  type: LessonType;
  durationText: string;
  isCompleted?: boolean;
  isActive?: boolean;
  isLocked?: boolean;
  isPreview?: boolean;
  lockedReason?: string;
  orderNumber: number;
  onClick?: (id: number) => void;
}

export function LessonItem({
  id,
  title,
  type,
  durationText,
  isCompleted = false,
  isActive = false,
  isLocked = false,
  isPreview = false,
  lockedReason,
  orderNumber,
  onClick,
}: LessonItemProps) {
  const getTypeIcon = () => {
    switch (type) {
      case 'VIDEO':
        return <Icons.Video size={16} />;
      case 'QUIZ':
        return <Icons.FileText size={16} />;
      case 'ARTICLE':
      default:
        return <Icons.BookOpen size={16} />;
    }
  };

  const getStatusIcon = () => {
    if (isCompleted) {
      return (
        <span className={styles.statusCompleted} title="Đã hoàn thành">
          <Icons.CheckCircle size={18} />
        </span>
      );
    }
    if (isLocked) {
      return (
        <span className={styles.statusLocked} title={lockedReason || 'Bài học đang bị khóa theo lộ trình'}>
          <Icons.Lock size={16} />
        </span>
      );
    }
    if (isActive) {
      return (
        <span className={styles.statusActive} title="Đang học">
          <Icons.Play size={16} />
        </span>
      );
    }
    return (
      <span className={styles.statusPending} title="Chưa học">
        <span className={styles.orderDot}>{orderNumber}</span>
      </span>
    );
  };

  const handleClick = () => {
    if (isLocked) {
      if (lockedReason) alert(`Bài học bị khóa: ${lockedReason}`);
      return;
    }
    onClick?.(id);
  };

  return (
    <div
      className={[
        styles.item,
        isActive ? styles['item--active'] : '',
        isLocked ? styles['item--locked'] : '',
        isCompleted ? styles['item--completed'] : '',
      ]
        .filter(Boolean)
        .join(' ')}
      onClick={handleClick}
      role="button"
      tabIndex={isLocked ? -1 : 0}
      aria-disabled={isLocked}
      aria-current={isActive ? 'true' : undefined}
      onKeyDown={(e) => {
        if (!isLocked && (e.key === 'Enter' || e.key === ' ')) {
          handleClick();
        }
      }}
    >
      <div className={styles.leadingStatus}>{getStatusIcon()}</div>

      <div className={styles.info}>
        <div className={styles.titleRow}>
          <span className={styles.title}>{title}</span>
          {isPreview && !isCompleted && (
            <Badge variant="accent" size="sm">
              Học thử
            </Badge>
          )}
        </div>

        <div className={styles.metaRow}>
          <span className={styles.typeBadge}>
            {getTypeIcon()}
            <span>
              {type === 'VIDEO' ? 'Video' : type === 'QUIZ' ? 'Bài tập Quiz' : 'Tài liệu đọc'}
            </span>
          </span>
          <span className={styles.duration}>
            <Icons.Clock size={12} />
            {durationText}
          </span>
          {lockedReason && isLocked && (
            <span className={styles.lockedHint}>
              (Yêu cầu: {lockedReason})
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
