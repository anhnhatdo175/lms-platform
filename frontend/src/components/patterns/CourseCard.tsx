import { Icons } from '../ui/Icons';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { Progress } from '../ui/Progress';
import styles from './CourseCard.module.css';

export interface CourseCardProps {
  id: number;
  title: string;
  thumbnailUrl?: string;
  teacherName: string;
  categoryName: string;
  price: number; // Tiền VND số nguyên đồng theo OpenAPI spec v1.5
  discountPrice?: number;
  rating?: number;
  totalLessons?: number;
  durationMinutes?: number;
  progressPercent?: number; // Nếu > 0 hoặc đã mua
  isEnrolled?: boolean;
  onEnrollClick?: (id: number) => void;
  onCardClick?: (id: number) => void;
  onAddToCart?: (id: number) => void;
}

export function CourseCard({
  id,
  title,
  thumbnailUrl,
  teacherName,
  categoryName,
  price,
  discountPrice,
  rating = 4.8,
  totalLessons = 24,
  durationMinutes = 360,
  progressPercent,
  isEnrolled = false,
  onEnrollClick,
  onCardClick,
  onAddToCart,
}: CourseCardProps) {
  const formatPriceVND = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  const hours = Math.floor(durationMinutes / 60);

  return (
    <article
      className={styles.card}
      onClick={() => onCardClick?.(id)}
      tabIndex={0}
      role="region"
      aria-label={`Khóa học: ${title}`}
      onKeyDown={(e) => {
        if (e.key === 'Enter') onCardClick?.(id);
      }}
    >
      {/* Thumbnail Container */}
      <div className={styles.thumbnailWrapper}>
        {thumbnailUrl ? (
          <img
            src={thumbnailUrl}
            alt={title}
            className={styles.thumbnail}
            loading="lazy"
            width="640"
            height="360"
          />
        ) : (
          <div className={styles.thumbnailPlaceholder}>
            <Icons.GraduationCap size={40} />
          </div>
        )}
        <div className={styles.badgeTopLeft}>
          <Badge variant="secondary" size="sm">
            {categoryName}
          </Badge>
        </div>
      </div>

      {/* Content */}
      <div className={styles.content}>
        <div className={styles.metaRow}>
          <span className={styles.teacher}>{teacherName}</span>
          <div className={styles.rating}>
            <Icons.Star size={14} className={styles.starIcon} />
            <span className={styles.ratingValue}>{rating.toFixed(1)}</span>
          </div>
        </div>

        <h3 className={styles.title} title={title}>
          {title}
        </h3>

        <div className={styles.statsRow}>
          <span className={styles.statItem}>
            <Icons.BookOpen size={14} />
            {totalLessons} bài học
          </span>
          <span className={styles.statItem}>
            <Icons.Clock size={14} />
            {hours} giờ
          </span>
        </div>

        {/* Progress if Enrolled */}
        {isEnrolled && progressPercent !== undefined ? (
          <div className={styles.progressSection}>
            <Progress
              value={progressPercent}
              isLabelVisible
              label="Tiến độ"
              size="sm"
              variant={progressPercent === 100 ? 'success' : 'primary'}
            />
            <Button
              variant="primary"
              size="sm"
              fullWidth
              onClick={(e) => {
                e.stopPropagation();
                onEnrollClick?.(id);
              }}
            >
              {progressPercent === 100 ? 'Xem lại bài' : 'Tiếp tục học'}
            </Button>
          </div>
        ) : (
          <div className={styles.footerRow}>
            <div className={styles.priceGroup}>
              {discountPrice ? (
                <>
                  <span className={styles.priceActive}>
                    {formatPriceVND(discountPrice)}
                  </span>
                  <span className={styles.priceOriginal}>
                    {formatPriceVND(price)}
                  </span>
                </>
              ) : (
                <span className={styles.priceActive}>
                  {price === 0 ? 'Miễn phí' : formatPriceVND(price)}
                </span>
              )}
            </div>

            <div className={styles.actionGroup}>
              {onAddToCart && price > 0 && (
                <Button
                  variant="outline"
                  size="icon"
                  aria-label="Thêm vào giỏ hàng"
                  onClick={(e) => {
                    e.stopPropagation();
                    onAddToCart(id);
                  }}
                >
                  <Icons.ShoppingCart size={16} />
                </Button>
              )}
              <Button
                variant="accent"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  onEnrollClick?.(id);
                }}
              >
                {price === 0 ? 'Học ngay' : 'Mua ngay'}
              </Button>
            </div>
          </div>
        )}
      </div>
    </article>
  );
}
