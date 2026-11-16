import { useEffect, useState, type FormEvent } from 'react';
import { cartApi } from '../features/commerce/commerce.api';
import { ApiRequestError } from '../services/authApi';
import {
  getCatalogCourse,
  listCourseCatalog,
  listCourseCategories,
  type CourseCatalogDetail,
  type CourseCatalogSummary,
  type CourseCategory,
} from '../services/courseApi';
import { formatVnd } from '../utils/format';
import styles from './CourseCatalogPage.module.css';

interface CourseCatalogPageProps {
  accessToken: string;
  canPurchase: boolean;
}

interface CatalogFilters {
  q: string;
  categoryId: number | undefined;
  minPrice: number | undefined;
  maxPrice: number | undefined;
  page: number;
  sort: 'newest' | 'price_asc' | 'price_desc';
}

const initialFilters: CatalogFilters = {
  q: '',
  categoryId: undefined,
  minPrice: undefined,
  maxPrice: undefined,
  page: 0,
  sort: 'newest',
};

export function CourseCatalogPage({ accessToken, canPurchase }: CourseCatalogPageProps) {
  const [categories, setCategories] = useState<CourseCategory[]>([]);
  const [courses, setCourses] = useState<{ data: CourseCatalogSummary[]; meta: { page: number; totalPages: number; totalElements: number } } | null>(null);
  const [filters, setFilters] = useState(initialFilters);
  const [queryInput, setQueryInput] = useState('');
  const [categoryInput, setCategoryInput] = useState('');
  const [minPriceInput, setMinPriceInput] = useState('');
  const [maxPriceInput, setMaxPriceInput] = useState('');
  const [selectedSlug, setSelectedSlug] = useState<string | null>(null);
  const [selectedCourse, setSelectedCourse] = useState<CourseCatalogDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [addingCourseId, setAddingCourseId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    let active = true;
    listCourseCategories(accessToken)
      .then((result) => {
        if (active) setCategories(result);
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof ApiRequestError ? cause.message : 'Không thể tải danh mục khóa học.');
      });
    return () => {
      active = false;
    };
  }, [accessToken]);

  useEffect(() => {
    let active = true;
    listCourseCatalog(accessToken, filters)
      .then((result) => {
        if (!active) return;
        setCourses(result);
        setError('');
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setError(cause instanceof ApiRequestError ? cause.message : 'Không thể tải danh sách khóa học.');
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [accessToken, filters]);

  useEffect(() => {
    if (!selectedSlug) return undefined;
    let active = true;
    getCatalogCourse(accessToken, selectedSlug)
      .then((course) => {
        if (active) setSelectedCourse(course);
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setError(cause instanceof ApiRequestError ? cause.message : 'Không thể tải chi tiết khóa học.');
      })
      .finally(() => {
        if (active) setIsLoadingDetail(false);
      });
    return () => {
      active = false;
    };
  }, [accessToken, selectedSlug]);

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const minPrice = minPriceInput.trim() ? Number(minPriceInput) : undefined;
    const maxPrice = maxPriceInput.trim() ? Number(maxPriceInput) : undefined;
    if ((minPrice !== undefined && (!Number.isSafeInteger(minPrice) || minPrice < 0))
      || (maxPrice !== undefined && (!Number.isSafeInteger(maxPrice) || maxPrice < 0))) {
      setError('Khoảng giá phải là số nguyên không âm.');
      return;
    }
    if (minPrice !== undefined && maxPrice !== undefined && minPrice > maxPrice) {
      setError('Giá thấp nhất không thể lớn hơn giá cao nhất.');
      return;
    }
    setError('');
    setNotice('');
    setIsLoading(true);
    setFilters({
      ...filters,
      q: queryInput.trim(),
      categoryId: categoryInput ? Number(categoryInput) : undefined,
      minPrice,
      maxPrice,
      page: 0,
    });
  }

  function selectCourse(slug: string) {
    const nextSlug = selectedSlug === slug ? null : slug;
    setSelectedSlug(nextSlug);
    setSelectedCourse(null);
    setIsLoadingDetail(nextSlug !== null);
  }

  function changeSort(sort: CatalogFilters['sort']) {
    setIsLoading(true);
    setFilters((current) => ({ ...current, sort, page: 0 }));
  }

  function changePage(page: number) {
    setIsLoading(true);
    setFilters((current) => ({ ...current, page }));
  }

  async function addToCart(course: CourseCatalogSummary) {
    if (!canPurchase || addingCourseId !== null) return;
    setAddingCourseId(course.id);
    setError('');
    setNotice('');
    try {
      await cartApi.addItem(course.id);
      setNotice(`Đã thêm “${course.title}” vào giỏ hàng.`);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể thêm khóa học vào giỏ hàng.');
    } finally {
      setAddingCourseId(null);
    }
  }

  return (
    <section className={styles.page} aria-labelledby="catalog-title">
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>KHÁM PHÁ TRI THỨC</p>
          <h1 id="catalog-title">Danh mục khóa học</h1>
          <p>Chọn lộ trình phù hợp và xem trước nội dung trước khi đăng ký.</p>
        </div>
        <div className={styles.totalCount} aria-live="polite">
          {courses ? `${courses.meta.totalElements.toLocaleString('vi-VN')} khóa học` : 'Đang tải'}
        </div>
      </header>

      <form className={styles.filters} onSubmit={applyFilters}>
        <label className={styles.searchField}>
          <span>Tìm khóa học</span>
          <input
            maxLength={200}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="Tên khóa học hoặc chủ đề"
            value={queryInput}
          />
        </label>
        <label>
          <span>Danh mục</span>
          <select onChange={(event) => setCategoryInput(event.target.value)} value={categoryInput}>
            <option value="">Tất cả danh mục</option>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>
        </label>
        <label>
          <span>Giá từ</span>
          <input
            min="0"
            onChange={(event) => setMinPriceInput(event.target.value)}
            type="number"
            value={minPriceInput}
          />
        </label>
        <label>
          <span>Đến</span>
          <input
            min="0"
            onChange={(event) => setMaxPriceInput(event.target.value)}
            type="number"
            value={maxPriceInput}
          />
        </label>
        <label>
          <span>Sắp xếp</span>
          <select
            onChange={(event) => changeSort(event.target.value as CatalogFilters['sort'])}
            value={filters.sort}
          >
            <option value="newest">Mới nhất</option>
            <option value="price_asc">Giá thấp đến cao</option>
            <option value="price_desc">Giá cao đến thấp</option>
          </select>
        </label>
        <button className={styles.filterButton} type="submit">Áp dụng</button>
      </form>

      {error && <div className={styles.error} role="alert">{error}</div>}
      {notice && <div className={styles.notice} role="status">{notice}</div>}

      <div className={styles.catalogLayout}>
        <section className={styles.results} aria-label="Khóa học" aria-busy={isLoading}>
          {isLoading ? (
            <p className={styles.stateMessage}>Đang tải khóa học...</p>
          ) : !courses?.data.length ? (
            <p className={styles.stateMessage}>Không tìm thấy khóa học phù hợp. Hãy thử thay đổi bộ lọc.</p>
          ) : (
            <div className={styles.courseGrid}>
              {courses.data.map((course) => (
                <article className={styles.courseCard} key={course.id}>
                  {course.thumbnailUrl
                    ? <img className={styles.thumbnail} src={course.thumbnailUrl} alt="" />
                    : <div className={styles.thumbnailFallback} aria-hidden="true">LMS</div>}
                  <div className={styles.courseBody}>
                    <p className={styles.category}>{course.category.name}</p>
                    <h2>{course.title}</h2>
                    <p className={styles.description}>{course.shortDescription || 'Khám phá nội dung và lộ trình học.'}</p>
                    <div className={styles.priceRow}>
                      {course.originalPrice > course.salePrice && (
                        <del>{formatVnd(course.originalPrice)}</del>
                      )}
                      <strong>{formatVnd(course.salePrice)}</strong>
                    </div>
                    <div className={styles.cardActions}>
                      <button
                        className={styles.secondaryButton}
                        aria-pressed={selectedSlug === course.slug}
                        onClick={() => selectCourse(course.slug)}
                        type="button"
                      >
                        {selectedSlug === course.slug ? 'Đang xem' : 'Xem chi tiết'}
                      </button>
                      {canPurchase && (
                        <button
                          className={styles.primaryButton}
                          disabled={addingCourseId !== null}
                          onClick={() => void addToCart(course)}
                          type="button"
                        >
                          {addingCourseId === course.id ? 'Đang thêm...' : 'Thêm vào giỏ'}
                        </button>
                      )}
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}

          {courses && courses.meta.totalPages > 1 && (
            <nav className={styles.pagination} aria-label="Phân trang danh mục khóa học">
              <button
                disabled={filters.page <= 0 || isLoading}
                onClick={() => changePage(filters.page - 1)}
                type="button"
              >
                Trước
              </button>
              <span>Trang {courses.meta.page + 1} / {courses.meta.totalPages}</span>
              <button
                disabled={filters.page + 1 >= courses.meta.totalPages || isLoading}
                onClick={() => changePage(filters.page + 1)}
                type="button"
              >
                Tiếp
              </button>
            </nav>
          )}
        </section>

        <aside className={styles.detailPanel} aria-label="Chi tiết khóa học" aria-live="polite">
          {!selectedSlug ? (
            <div className={styles.detailEmpty}>
              <h2>Chọn khóa học để xem nội dung</h2>
              <p>Chi tiết lộ trình, giảng viên và điều kiện hoàn thành sẽ hiển thị tại đây.</p>
            </div>
          ) : isLoadingDetail ? (
            <p className={styles.stateMessage}>Đang tải chi tiết...</p>
          ) : selectedCourse ? (
            <CourseDetails course={selectedCourse} />
          ) : (
            <p className={styles.stateMessage}>Không thể tải khóa học này.</p>
          )}
        </aside>
      </div>
    </section>
  );
}

function CourseDetails({ course }: { course: CourseCatalogDetail }) {
  const lessonCount = course.sections.reduce((total, section) => total + section.lessons.length, 0);
  return (
    <article className={styles.detailCard}>
      <div className={styles.detailHeading}>
        <p className={styles.eyebrow}>CHI TIẾT KHÓA HỌC</p>
        <h2>{course.title}</h2>
        <p>{course.category.name}{course.gradeLevel ? ` · ${course.gradeLevel}` : ''}</p>
      </div>
      <p className={styles.fullDescription}>{course.description || course.shortDescription || 'Nội dung khóa học đang được cập nhật.'}</p>
      <div className={styles.detailMetrics}>
        <span><strong>{course.sections.length}</strong> chương</span>
        <span><strong>{lessonCount}</strong> bài học</span>
        <span><strong>{course.accessDurationDays ?? 'Không giới hạn'}</strong> {course.accessDurationDays ? 'ngày truy cập' : ''}</span>
      </div>
      <div className={styles.teacherList}>
        <h3>Giảng viên</h3>
        {course.teachers.length
          ? course.teachers.map((teacher) => <span key={teacher.id}>{teacher.displayName}</span>)
          : <span>Thông tin giảng viên đang cập nhật.</span>}
      </div>
      <div className={styles.curriculum}>
        <h3>Nội dung học</h3>
        {course.sections.length === 0 ? (
          <p>Chưa có nội dung được xuất bản.</p>
        ) : course.sections.map((section) => (
          <details key={section.id}>
            <summary>{section.title}<span>{section.lessons.length} bài</span></summary>
            <ul>
              {section.lessons.map((lesson) => (
                <li key={lesson.id}>
                  <span>{lesson.title}</span>
                  {lesson.preview && <small>Bài xem trước</small>}
                </li>
              ))}
            </ul>
            {section.releaseAfterDays > 0 && (
              <p className={styles.dripNote}>Mở sau {section.releaseAfterDays} ngày kể từ khi bắt đầu học.</p>
            )}
          </details>
        ))}
      </div>
      <div className={styles.detailPrice}>
        {course.originalPrice > course.salePrice && <del>{formatVnd(course.originalPrice)}</del>}
        <strong>{formatVnd(course.salePrice)}</strong>
      </div>
    </article>
  );
}
