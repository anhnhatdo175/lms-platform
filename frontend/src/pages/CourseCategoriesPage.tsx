import { useEffect, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../services/authApi';
import {
  createCourseCategory,
  listAdminCourseCategories,
  updateCourseCategory,
  type AdminCourseCategory,
} from '../services/courseApi';
import styles from './CourseCategoriesPage.module.css';

export function CourseCategoriesPage({ accessToken }: { accessToken: string }) {
  const [categories, setCategories] = useState<AdminCourseCategory[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [busyCategoryId, setBusyCategoryId] = useState<number | null>(null);
  const [notice, setNotice] = useState<{ message: string; error: boolean } | null>(null);

  useEffect(() => {
    let active = true;
    listAdminCourseCategories(accessToken)
      .then((page) => {
        if (active) setCategories(page.data);
      })
      .catch((error: unknown) => {
        if (active) {
          setNotice({
            message: error instanceof ApiRequestError
              ? error.message
              : 'Không thể tải danh mục khóa học.',
            error: true,
          });
        }
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [accessToken]);

  async function submitCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isCreating) return;
    const form = event.currentTarget;
    const values = new FormData(form);
    const name = String(values.get('name') ?? '').trim();
    const slug = String(values.get('slug') ?? '').trim();
    const description = String(values.get('description') ?? '').trim() || null;
    if (!name || !slug) {
      setNotice({ message: 'Tên và slug danh mục là bắt buộc.', error: true });
      return;
    }

    setIsCreating(true);
    setNotice(null);
    try {
      const created = await createCourseCategory(accessToken, { name, slug, description });
      setCategories((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name, 'vi')));
      form.reset();
      setNotice({ message: 'Đã tạo danh mục khóa học.', error: false });
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể tạo danh mục.',
        error: true,
      });
    } finally {
      setIsCreating(false);
    }
  }

  async function toggleActive(category: AdminCourseCategory) {
    setBusyCategoryId(category.id);
    setNotice(null);
    try {
      const updated = await updateCourseCategory(accessToken, category.id, { active: !category.active });
      setCategories((current) => current.map((item) => item.id === category.id ? updated : item));
      setNotice({
        message: updated.active ? 'Đã bật danh mục.' : 'Đã tắt danh mục. Danh mục đã dùng vẫn được giữ nguyên.',
        error: false,
      });
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể cập nhật danh mục.',
        error: true,
      });
    } finally {
      setBusyCategoryId(null);
    }
  }

  return (
    <section className={styles.page} aria-labelledby="course-categories-title">
      <header className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>QUẢN TRỊ NỘI DUNG</p>
          <h1 id="course-categories-title">Danh mục khóa học</h1>
          <p>Danh mục đang hoạt động được giảng viên chọn khi tạo Course.</p>
        </div>
      </header>

      {notice && (
        <div className={notice.error ? styles.errorNotice : styles.successNotice} role={notice.error ? 'alert' : 'status'}>
          {notice.message}
        </div>
      )}

      <div className={styles.layout}>
        <form className={styles.card} onSubmit={(event) => void submitCategory(event)}>
          <div className={styles.cardHeading}>
            <h2>Tạo danh mục</h2>
            <p>Slug phải duy nhất, dùng chữ thường không dấu và dấu gạch ngang.</p>
          </div>
          <label className={styles.field}>
            <span>Tên danh mục</span>
            <input maxLength={255} name="name" required />
          </label>
          <label className={styles.field}>
            <span>Slug</span>
            <input
              maxLength={255}
              name="slug"
              pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
              required
              title="Dùng chữ thường không dấu, số và dấu gạch ngang."
            />
          </label>
          <label className={styles.field}>
            <span>Mô tả</span>
            <textarea name="description" rows={3} />
          </label>
          <button className={styles.primaryButton} disabled={isCreating} type="submit">
            {isCreating ? 'Đang tạo...' : 'Tạo danh mục'}
          </button>
        </form>

        <section className={styles.card} aria-labelledby="category-list-title">
          <div className={styles.cardHeading}>
            <h2 id="category-list-title">Danh mục hiện có <span>{categories.length}</span></h2>
            <p>Tắt danh mục sẽ ngăn tạo Course mới bằng danh mục đó, không xóa dữ liệu cũ.</p>
          </div>
          {isLoading ? (
            <p className={styles.emptyState}>Đang tải danh mục...</p>
          ) : categories.length === 0 ? (
            <p className={styles.emptyState}>Chưa có danh mục. Tạo một danh mục để giảng viên bắt đầu soạn Course.</p>
          ) : (
            <ul className={styles.categoryList}>
              {categories.map((category) => (
                <li className={styles.categoryRow} key={category.id}>
                  <div className={styles.categoryInfo}>
                    <strong>{category.name}</strong>
                    <span>{category.slug}</span>
                    {category.description && <small>{category.description}</small>}
                  </div>
                  <div className={styles.categoryActions}>
                    <span className={category.active ? styles.activeBadge : styles.inactiveBadge}>
                      {category.active ? 'Đang hoạt động' : 'Đã tắt'}
                    </span>
                    <button
                      className={styles.secondaryButton}
                      disabled={busyCategoryId === category.id}
                      onClick={() => void toggleActive(category)}
                      type="button"
                    >
                      {busyCategoryId === category.id
                        ? 'Đang lưu...'
                        : category.active ? 'Tắt danh mục' : 'Bật danh mục'}
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </section>
  );
}
