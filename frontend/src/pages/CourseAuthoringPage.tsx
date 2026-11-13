import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../services/authApi';
import {
  createCourse,
  createLesson,
  createSection,
  deleteLesson,
  deleteSection,
  getTeacherCourse,
  listCourseCategories,
  listTeacherCourses,
  updateLesson,
  updateSection,
  type CourseCategory,
  type CourseCompletionRule,
  type CourseStatus,
  type LessonDetail,
  type LessonType,
  type ManagedCourse,
  type SectionSummary,
} from '../services/courseApi';
import styles from './CourseAuthoringPage.module.css';

interface CourseAuthoringPageProps {
  accessToken: string;
}

const statusLabels: Record<CourseStatus, string> = {
  DRAFT: 'Bản nháp',
  PENDING_REVIEW: 'Đang chờ duyệt',
  REJECTED: 'Bị từ chối',
  PUBLISHED: 'Đã xuất bản',
  HIDDEN: 'Đang ẩn',
  ARCHIVED: 'Đã lưu trữ',
};

const completionRules: Array<{ value: CourseCompletionRule; label: string }> = [
  { value: 'ALL_MANDATORY_LESSONS', label: 'Hoàn thành tất cả bài học bắt buộc' },
  {
    value: 'ALL_MANDATORY_LESSONS_AND_PASS_REQUIRED_EXAMS',
    label: 'Hoàn thành bài học và đạt bài kiểm tra yêu cầu',
  },
  { value: 'MANUAL_BY_TEACHER', label: 'Giảng viên xác nhận hoàn thành' },
];

const lessonTypes: Array<{ value: LessonType; label: string }> = [
  { value: 'VIDEO', label: 'Video' },
  { value: 'TEXT', label: 'Văn bản' },
  { value: 'DOCUMENT', label: 'Tài liệu' },
  { value: 'EXAM', label: 'Bài kiểm tra' },
];

export function CourseAuthoringPage({ accessToken }: CourseAuthoringPageProps) {
  const [courses, setCourses] = useState<ManagedCourse[]>([]);
  const [categories, setCategories] = useState<CourseCategory[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<ManagedCourse | null>(null);
  const [sectionsByCourse, setSectionsByCourse] = useState<Record<number, SectionSummary[]>>({});
  const [lessonsBySection, setLessonsBySection] = useState<Record<number, LessonDetail[]>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isCreatingCourse, setIsCreatingCourse] = useState(false);
  const [isCreatingSection, setIsCreatingSection] = useState(false);
  const [busyKey, setBusyKey] = useState('');
  const [showCourseForm, setShowCourseForm] = useState(false);
  const [notice, setNotice] = useState<{ message: string; error: boolean } | null>(null);
  const [search, setSearch] = useState('');

  useEffect(() => {
    let active = true;
    Promise.all([listTeacherCourses(accessToken), listCourseCategories(accessToken)])
      .then(([coursePage, categoryList]) => {
        if (!active) return;
        setCourses(coursePage.data);
        setCategories(categoryList);
        setNotice(null);
      })
      .catch((error: unknown) => {
        if (!active) return;
        setNotice({
          message: error instanceof ApiRequestError
            ? error.message
            : 'Không thể tải dữ liệu khóa học. Vui lòng thử lại.',
          error: true,
        });
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [accessToken]);

  const filteredCourses = useMemo(() => {
    const normalized = search.trim().toLocaleLowerCase('vi');
    if (!normalized) return courses;
    return courses.filter((course) =>
      `${course.title} ${course.slug}`.toLocaleLowerCase('vi').includes(normalized),
    );
  }, [courses, search]);

  async function refreshCourses() {
    setBusyKey('refresh-courses');
    setNotice(null);
    try {
      const result = await listTeacherCourses(accessToken);
      setCourses(result.data);
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể làm mới danh sách khóa học.',
        error: true,
      });
    } finally {
      setBusyKey('');
    }
  }

  async function handleCreateCourse(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isCreatingCourse) return;
    const form = event.currentTarget;
    const values = new FormData(form);
    const title = String(values.get('title') ?? '').trim();
    const slug = String(values.get('slug') ?? '').trim();
    const categoryId = Number(values.get('categoryId'));
    const originalPrice = Number(values.get('originalPrice') ?? 0);
    const salePrice = Number(values.get('salePrice') ?? 0);
    if (!title || !slug || !Number.isInteger(categoryId) || categoryId < 1) {
      setNotice({ message: 'Vui lòng nhập tên, slug và chọn danh mục.', error: true });
      return;
    }
    if (!Number.isSafeInteger(originalPrice) || !Number.isSafeInteger(salePrice)
      || originalPrice < 0 || salePrice < 0 || salePrice > originalPrice) {
      setNotice({ message: 'Giá bán phải là số nguyên không âm và không vượt giá gốc.', error: true });
      return;
    }

    setIsCreatingCourse(true);
    setNotice(null);
    try {
      const course = await createCourse(accessToken, {
        title,
        slug,
        categoryId,
        shortDescription: nullableField(values.get('shortDescription')),
        description: nullableField(values.get('description')),
        originalPrice,
        salePrice,
        subjectCode: nullableField(values.get('subjectCode')),
        gradeLevel: nullableField(values.get('gradeLevel')),
        completionRule: String(values.get('completionRule')) as CourseCompletionRule,
      });
      setCourses((current) => [course, ...current.filter((item) => item.id !== course.id)]);
      setSelectedCourse(course);
      setSectionsByCourse((current) => ({ ...current, [course.id]: [] }));
      setShowCourseForm(false);
      setNotice({ message: 'Đã tạo khóa học ở trạng thái Bản nháp.', error: false });
      form.reset();
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể tạo khóa học.',
        error: true,
      });
    } finally {
      setIsCreatingCourse(false);
    }
  }

  async function openCourse(courseId: number) {
    setBusyKey(`course-${courseId}`);
    setNotice(null);
    try {
      const course = await getTeacherCourse(accessToken, courseId);
      setSelectedCourse(course);
      if (!(courseId in sectionsByCourse)) {
        setSectionsByCourse((current) => ({ ...current, [courseId]: [] }));
      }
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể mở khóa học.',
        error: true,
      });
    } finally {
      setBusyKey('');
    }
  }

  async function handleCreateSection(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedCourse || isCreatingSection) return;
    const form = event.currentTarget;
    const values = new FormData(form);
    const title = String(values.get('sectionTitle') ?? '').trim();
    const releaseAfterDays = Number(values.get('releaseAfterDays') ?? 0);
    if (!title || !Number.isSafeInteger(releaseAfterDays) || releaseAfterDays < 0) {
      setNotice({ message: 'Nhập tên chương và số ngày mở nội dung hợp lệ.', error: true });
      return;
    }

    setIsCreatingSection(true);
    setNotice(null);
    try {
      const section = await createSection(accessToken, selectedCourse.id, {
        title,
        description: nullableField(values.get('sectionDescription')),
        published: values.get('sectionPublished') === 'on',
        releaseAfterDays,
      });
      setSectionsByCourse((current) => ({
        ...current,
        [selectedCourse.id]: [...(current[selectedCourse.id] ?? []), section]
          .sort((left, right) => left.position - right.position),
      }));
      setNotice({ message: 'Đã thêm chương vào khóa học.', error: false });
      form.reset();
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể tạo chương.',
        error: true,
      });
    } finally {
      setIsCreatingSection(false);
    }
  }

  async function handleCreateLesson(event: FormEvent<HTMLFormElement>, section: SectionSummary) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = new FormData(form);
    const title = String(values.get('lessonTitle') ?? '').trim();
    const durationRaw = String(values.get('durationSeconds') ?? '').trim();
    const durationSeconds = durationRaw ? Number(durationRaw) : null;
    const content = nullableField(values.get('content'));
    if (!title || (durationSeconds !== null
      && (!Number.isSafeInteger(durationSeconds) || durationSeconds < 0))) {
      setNotice({ message: 'Nhập tên bài học và thời lượng hợp lệ.', error: true });
      return;
    }
    const lessonType = String(values.get('lessonType')) as LessonType;
    if ((lessonType === 'TEXT' || lessonType === 'DOCUMENT') && !content) {
      setNotice({ message: 'Bài học dạng văn bản/tài liệu cần có nội dung.', error: true });
      return;
    }

    const key = `create-lesson-${section.id}`;
    setBusyKey(key);
    setNotice(null);
    try {
      const lesson = await createLesson(accessToken, section.id, {
        title,
        lessonType,
        description: null,
        content,
        videoResourceId: null,
        durationSeconds,
        preview: values.get('preview') === 'on',
        published: values.get('lessonPublished') === 'on',
        mandatory: values.get('mandatory') === 'on',
        completionMethod: 'MANUAL',
        completionThreshold: null,
      });
      setLessonsBySection((current) => ({
        ...current,
        [section.id]: [...(current[section.id] ?? []), lesson]
          .sort((left, right) => left.position - right.position),
      }));
      setNotice({ message: 'Đã thêm bài học vào chương.', error: false });
      form.reset();
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể tạo bài học.',
        error: true,
      });
    } finally {
      setBusyKey('');
    }
  }

  async function toggleLesson(lesson: LessonDetail, field: 'preview' | 'published') {
    setBusyKey(`lesson-${lesson.id}`);
    setNotice(null);
    try {
      const updated = await updateLesson(accessToken, lesson.id, { [field]: !lesson[field] });
      setLessonsBySection((current) => ({
        ...current,
        [lesson.sectionId]: (current[lesson.sectionId] ?? []).map((item) =>
          item.id === lesson.id ? updated : item),
      }));
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể cập nhật bài học.',
        error: true,
      });
    } finally {
      setBusyKey('');
    }
  }

  async function removeLesson(lesson: LessonDetail) {
    if (!window.confirm(`Xóa bài học "${lesson.title}"?`)) return;
    setBusyKey(`lesson-${lesson.id}`);
    setNotice(null);
    try {
      await deleteLesson(accessToken, lesson.id);
      setLessonsBySection((current) => ({
        ...current,
        [lesson.sectionId]: (current[lesson.sectionId] ?? []).filter((item) => item.id !== lesson.id),
      }));
      setNotice({ message: 'Đã xóa bài học.', error: false });
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể xóa bài học.',
        error: true,
      });
    } finally {
      setBusyKey('');
    }
  }

  async function removeSection(section: SectionSummary) {
    if (!window.confirm(`Xóa chương "${section.title}"? Chỉ xóa được chương chưa có bài học.`)) return;
    setBusyKey(`section-${section.id}`);
    setNotice(null);
    try {
      await deleteSection(accessToken, section.id);
      setSectionsByCourse((current) => ({
        ...current,
        [section.courseId]: (current[section.courseId] ?? []).filter((item) => item.id !== section.id),
      }));
      setLessonsBySection((current) => {
        const next = { ...current };
        delete next[section.id];
        return next;
      });
      setNotice({ message: 'Đã xóa chương.', error: false });
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể xóa chương.',
        error: true,
      });
    } finally {
      setBusyKey('');
    }
  }

  async function saveSection(event: FormEvent<HTMLFormElement>, section: SectionSummary) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const title = String(values.get('editTitle') ?? '').trim();
    const releaseAfterDays = Number(values.get('editReleaseAfterDays') ?? 0);
    if (!title || !Number.isSafeInteger(releaseAfterDays) || releaseAfterDays < 0) {
      setNotice({ message: 'Tên chương và số ngày drip không hợp lệ.', error: true });
      return;
    }
    setBusyKey(`section-${section.id}`);
    setNotice(null);
    try {
      const updated = await updateSection(accessToken, section.id, {
        title,
        description: nullableField(values.get('editDescription')),
        published: values.get('editPublished') === 'on',
        releaseAfterDays,
      });
      setSectionsByCourse((current) => ({
        ...current,
        [section.courseId]: (current[section.courseId] ?? []).map((item) =>
          item.id === section.id ? updated : item),
      }));
      setNotice({ message: 'Đã cập nhật chương.', error: false });
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể cập nhật chương.',
        error: true,
      });
    } finally {
      setBusyKey('');
    }
  }

  const currentSections = selectedCourse ? sectionsByCourse[selectedCourse.id] ?? [] : [];

  return (
    <section className={styles.page} aria-labelledby="course-authoring-title">
      <header className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>KHÔNG GIAN GIẢNG VIÊN</p>
          <h1 id="course-authoring-title">Soạn khóa học</h1>
          <p>Tạo khóa học nháp, xây dựng chương và bài học theo đúng quy trình nội dung.</p>
        </div>
        <button
          className={styles.primaryButton}
          onClick={() => {
            setShowCourseForm((value) => !value);
            setNotice(null);
          }}
          type="button"
        >
          {showCourseForm ? 'Đóng biểu mẫu' : 'Tạo khóa học'}
        </button>
      </header>

      {notice && (
        <div className={notice.error ? styles.errorNotice : styles.successNotice} role={notice.error ? 'alert' : 'status'}>
          {notice.message}
        </div>
      )}

      {showCourseForm && (
        <form className={styles.panel} onSubmit={(event) => void handleCreateCourse(event)}>
          <div className={styles.panelHeading}>
            <div>
              <h2>Thông tin khóa học</h2>
              <p>Khóa học mới được tạo ở trạng thái Bản nháp.</p>
            </div>
          </div>
          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>Tên khóa học</span>
              <input autoComplete="off" maxLength={255} name="title" required />
            </label>
            <label className={styles.field}>
              <span>Slug</span>
              <input autoComplete="off" maxLength={255} name="slug" required />
              <small>Dùng đường dẫn không dấu, duy nhất.</small>
            </label>
            <label className={styles.field}>
              <span>Danh mục</span>
              <select defaultValue="" name="categoryId" required>
                <option disabled value="">Chọn danh mục</option>
                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
            </label>
            <label className={styles.field}>
              <span>Mã môn học</span>
              <input maxLength={100} name="subjectCode" />
            </label>
            <label className={styles.field}>
              <span>Khối lớp</span>
              <input maxLength={50} name="gradeLevel" />
            </label>
            <label className={styles.field}>
              <span>Quy tắc hoàn thành</span>
              <select defaultValue="ALL_MANDATORY_LESSONS" name="completionRule">
                {completionRules.map((rule) => <option key={rule.value} value={rule.value}>{rule.label}</option>)}
              </select>
            </label>
            <label className={styles.field}>
              <span>Giá gốc (VND)</span>
              <input defaultValue="0" min="0" name="originalPrice" required type="number" />
            </label>
            <label className={styles.field}>
              <span>Giá bán (VND)</span>
              <input defaultValue="0" min="0" name="salePrice" required type="number" />
            </label>
            <label className={`${styles.field} ${styles.fullWidth}`}>
              <span>Mô tả ngắn</span>
              <input maxLength={500} name="shortDescription" />
            </label>
            <label className={`${styles.field} ${styles.fullWidth}`}>
              <span>Mô tả chi tiết</span>
              <textarea name="description" rows={4} />
            </label>
          </div>
          <div className={styles.formActions}>
            <button className={styles.primaryButton} disabled={isCreatingCourse || categories.length === 0} type="submit">
              {isCreatingCourse ? 'Đang tạo...' : 'Tạo bản nháp'}
            </button>
          </div>
        </form>
      )}

      <div className={styles.workspace}>
        <aside className={styles.coursePanel} aria-label="Danh sách khóa học">
          <div className={styles.coursePanelHeading}>
            <h2>Khóa học của tôi</h2>
            <span>{courses.length}</span>
          </div>
          <label className={styles.searchField}>
            <span className="sr-only">Tìm khóa học</span>
            <input
              onChange={(event) => setSearch(event.currentTarget.value)}
              placeholder="Tìm khóa học..."
              type="search"
              value={search}
            />
          </label>
          {isLoading ? (
            <p className={styles.emptyState}>Đang tải khóa học...</p>
          ) : filteredCourses.length === 0 ? (
            <p className={styles.emptyState}>Chưa có khóa học phù hợp. Hãy tạo khóa học đầu tiên.</p>
          ) : (
            <ul className={styles.courseList}>
              {filteredCourses.map((course) => (
                <li key={course.id}>
                  <button
                    aria-current={selectedCourse?.id === course.id ? 'true' : undefined}
                    className={`${styles.courseChoice} ${selectedCourse?.id === course.id ? styles.courseChoiceActive : ''}`}
                    disabled={busyKey === `course-${course.id}`}
                    onClick={() => void openCourse(course.id)}
                    type="button"
                  >
                    <span className={styles.courseChoiceTitle}>{course.title}</span>
                    <span className={`${styles.statusBadge} ${styles[`status_${course.status}`]}`}>
                      {statusLabels[course.status]}
                    </span>
                    <span className={styles.courseChoiceMeta}>{course.category?.name ?? 'Chưa có danh mục'}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <button
            className={styles.refreshButton}
            disabled={isLoading || busyKey === 'refresh-courses'}
            onClick={() => void refreshCourses()}
            type="button"
          >
            {busyKey === 'refresh-courses' ? 'Đang làm mới...' : 'Làm mới danh sách'}
          </button>
        </aside>

        <div className={styles.editorColumn}>
          {!selectedCourse ? (
            <section className={`${styles.panel} ${styles.welcomePanel}`}>
              <span className={styles.stepMark}>01</span>
              <h2>Bắt đầu xây dựng nội dung</h2>
              <p>Tạo khóa học nháp hoặc chọn khóa học ở danh sách để tiếp tục soạn.</p>
              <ol className={styles.workflow}>
                <li><span>1</span><div><strong>Thông tin khóa học</strong><small>Danh mục, mô tả và giá</small></div></li>
                <li><span>2</span><div><strong>Chương học</strong><small>Cấu trúc nội dung và lịch mở drip</small></div></li>
                <li><span>3</span><div><strong>Bài học</strong><small>Loại bài, nội dung và điều kiện hoàn thành</small></div></li>
              </ol>
            </section>
          ) : (
            <>
              <section className={styles.selectedCourse}>
                <div>
                  <p className={styles.eyebrow}>ĐANG SOẠN</p>
                  <h2>{selectedCourse.title}</h2>
                  <p>{selectedCourse.shortDescription || selectedCourse.category?.name || 'Chưa có mô tả ngắn'}</p>
                </div>
                <span className={`${styles.statusBadge} ${styles[`status_${selectedCourse.status}`]}`}>
                  {statusLabels[selectedCourse.status]}
                </span>
              </section>

              <div className={styles.contractNotice} role="note">
                <strong>Lưu ý về dữ liệu chương trình học</strong>
                <span>Contract hiện có API tạo/cập nhật/xóa chương và bài học nhưng chưa định nghĩa API đọc curriculum. Danh sách dưới đây chỉ giữ nội dung được tạo trong phiên trình duyệt này.</span>
              </div>

              {selectedCourse.status !== 'DRAFT' && selectedCourse.status !== 'PUBLISHED' && selectedCourse.status !== 'HIDDEN' ? (
                <div className={styles.readOnlyNotice} role="status">
                  Trạng thái hiện tại không cho phép chỉnh sửa nội dung. Theo quy trình, chỉ Bản nháp, Đã xuất bản hoặc Đang ẩn được chỉnh sửa.
                </div>
              ) : (
                <form className={styles.panel} onSubmit={(event) => void handleCreateSection(event)}>
                  <div className={styles.panelHeading}>
                    <div>
                      <p className={styles.eyebrow}>BƯỚC 2</p>
                      <h2>Thêm chương</h2>
                    </div>
                    <span className={styles.countBadge}>{currentSections.length} chương</span>
                  </div>
                  <div className={styles.formGrid}>
                    <label className={styles.field}>
                      <span>Tên chương</span>
                      <input maxLength={255} name="sectionTitle" required />
                    </label>
                    <label className={styles.field}>
                      <span>Mở sau (ngày kể từ khi ghi danh)</span>
                      <input defaultValue="0" min="0" name="releaseAfterDays" required type="number" />
                    </label>
                    <label className={`${styles.field} ${styles.fullWidth}`}>
                      <span>Mô tả chương</span>
                      <textarea name="sectionDescription" rows={2} />
                    </label>
                    <label className={styles.checkboxField}>
                      <input name="sectionPublished" type="checkbox" />
                      <span>Phát hành chương</span>
                    </label>
                  </div>
                  <div className={styles.formActions}>
                    <button className={styles.primaryButton} disabled={isCreatingSection} type="submit">
                      {isCreatingSection ? 'Đang thêm...' : 'Thêm chương'}
                    </button>
                  </div>
                </form>
              )}

              <div className={styles.sectionList}>
                {currentSections.length === 0 ? (
                  <div className={styles.emptyPanel}>
                    <h3>Chưa có chương trong phiên này</h3>
                    <p>Thêm chương đầu tiên để bắt đầu tạo lộ trình học.</p>
                  </div>
                ) : currentSections.map((section) => (
                  <SectionEditor
                    key={section.id}
                    busyKey={busyKey}
                    canEdit={selectedCourse.status === 'DRAFT' || selectedCourse.status === 'PUBLISHED' || selectedCourse.status === 'HIDDEN'}
                    lessons={lessonsBySection[section.id] ?? []}
                    onCreateLesson={handleCreateLesson}
                    onDeleteLesson={removeLesson}
                    onDeleteSection={removeSection}
                    onSaveSection={saveSection}
                    onToggleLesson={toggleLesson}
                    section={section}
                  />
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </section>
  );
}

function SectionEditor({
  busyKey,
  canEdit,
  lessons,
  onCreateLesson,
  onDeleteLesson,
  onDeleteSection,
  onSaveSection,
  onToggleLesson,
  section,
}: {
  busyKey: string;
  canEdit: boolean;
  lessons: LessonDetail[];
  onCreateLesson: (event: FormEvent<HTMLFormElement>, section: SectionSummary) => Promise<void>;
  onDeleteLesson: (lesson: LessonDetail) => Promise<void>;
  onDeleteSection: (section: SectionSummary) => Promise<void>;
  onSaveSection: (event: FormEvent<HTMLFormElement>, section: SectionSummary) => Promise<void>;
  onToggleLesson: (lesson: LessonDetail, field: 'preview' | 'published') => Promise<void>;
  section: SectionSummary;
}) {
  const [isEditing, setIsEditing] = useState(false);
  const [showLessonForm, setShowLessonForm] = useState(false);

  return (
    <section className={styles.sectionCard} aria-labelledby={`section-${section.id}`}>
      <header className={styles.sectionHeader}>
        <div className={styles.sectionTitleBlock}>
          <span className={styles.sectionNumber}>{String(section.position).padStart(2, '0')}</span>
          <div>
            <h3 id={`section-${section.id}`}>{section.title}</h3>
            <p>Mở sau {section.releaseAfterDays} ngày · {section.published ? 'Đã phát hành' : 'Chưa phát hành'}</p>
          </div>
        </div>
        {canEdit && (
          <div className={styles.actionGroup}>
            <button className={styles.secondaryButton} onClick={() => setIsEditing((value) => !value)} type="button">
              {isEditing ? 'Đóng sửa' : 'Sửa chương'}
            </button>
            <button
              className={styles.dangerButton}
              disabled={busyKey === `section-${section.id}`}
              onClick={() => void onDeleteSection(section)}
              type="button"
            >
              Xóa
            </button>
          </div>
        )}
      </header>

      {isEditing && (
        <form className={styles.inlineForm} onSubmit={(event) => void onSaveSection(event, section)}>
          <label className={styles.field}>
            <span>Tên chương</span>
            <input defaultValue={section.title} maxLength={255} name="editTitle" required />
          </label>
          <label className={styles.field}>
            <span>Mở sau (ngày)</span>
            <input defaultValue={section.releaseAfterDays} min="0" name="editReleaseAfterDays" required type="number" />
          </label>
          <label className={`${styles.field} ${styles.fullWidth}`}>
            <span>Mô tả</span>
            <textarea defaultValue={section.description ?? ''} name="editDescription" rows={2} />
          </label>
          <label className={styles.checkboxField}>
            <input defaultChecked={section.published} name="editPublished" type="checkbox" />
            <span>Phát hành chương</span>
          </label>
          <button className={styles.primaryButton} disabled={busyKey === `section-${section.id}`} type="submit">
            Lưu thay đổi
          </button>
        </form>
      )}

      <div className={styles.lessonList}>
        {lessons.map((lesson) => (
          <article className={styles.lessonRow} key={lesson.id}>
            <div className={styles.lessonInfo}>
              <span className={styles.lessonType}>{lessonTypes.find((type) => type.value === lesson.lessonType)?.label}</span>
              <strong>{lesson.title}</strong>
              <small>
                {lesson.position}. {lesson.mandatory ? 'Bắt buộc' : 'Tùy chọn'}
                {lesson.durationSeconds !== null ? ` · ${lesson.durationSeconds} giây` : ''}
              </small>
            </div>
            <div className={styles.actionGroup}>
              <button
                aria-pressed={lesson.preview}
                className={lesson.preview ? styles.toggleActive : styles.secondaryButton}
                disabled={!canEdit || busyKey === `lesson-${lesson.id}`}
                onClick={() => void onToggleLesson(lesson, 'preview')}
                type="button"
              >
                {lesson.preview ? 'Đang xem thử' : 'Xem thử'}
              </button>
              <button
                aria-pressed={lesson.published}
                className={lesson.published ? styles.toggleActive : styles.secondaryButton}
                disabled={!canEdit || busyKey === `lesson-${lesson.id}`}
                onClick={() => void onToggleLesson(lesson, 'published')}
                type="button"
              >
                {lesson.published ? 'Đã phát hành' : 'Chưa phát hành'}
              </button>
              {canEdit && (
                <button
                  className={styles.dangerButton}
                  disabled={busyKey === `lesson-${lesson.id}`}
                  onClick={() => void onDeleteLesson(lesson)}
                  type="button"
                >
                  Xóa
                </button>
              )}
            </div>
          </article>
        ))}
        {lessons.length === 0 && <p className={styles.lessonEmpty}>Chương chưa có bài học.</p>}
      </div>

      {canEdit && (
        <div className={styles.lessonActions}>
          <button className={styles.addLessonButton} onClick={() => setShowLessonForm((value) => !value)} type="button">
            {showLessonForm ? 'Đóng biểu mẫu bài học' : 'Thêm bài học'}
          </button>
        </div>
      )}

      {showLessonForm && canEdit && (
        <form className={styles.inlineForm} onSubmit={(event) => void onCreateLesson(event, section)}>
          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>Tên bài học</span>
              <input maxLength={255} name="lessonTitle" required />
            </label>
            <label className={styles.field}>
              <span>Loại bài học</span>
              <select defaultValue="VIDEO" name="lessonType">
                {lessonTypes.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
              </select>
            </label>
            <label className={styles.field}>
              <span>Thời lượng (giây)</span>
              <input min="0" name="durationSeconds" type="number" />
            </label>
            <label className={styles.checkboxField}>
              <input defaultChecked name="mandatory" type="checkbox" />
              <span>Bài học bắt buộc</span>
            </label>
            <label className={styles.checkboxField}>
              <input name="preview" type="checkbox" />
              <span>Cho phép xem thử</span>
            </label>
            <label className={styles.checkboxField}>
              <input name="lessonPublished" type="checkbox" />
              <span>Phát hành bài học</span>
            </label>
            <label className={`${styles.field} ${styles.fullWidth}`}>
              <span>Nội dung</span>
              <textarea name="content" rows={4} />
            </label>
          </div>
          <p className={styles.helperText}>Video/tài liệu cần gắn tài nguyên đã tải lên riêng. Bài học dạng văn bản hoặc tài liệu cần có nội dung.</p>
          <div className={styles.formActions}>
            <button
              className={styles.primaryButton}
              disabled={busyKey === `create-lesson-${section.id}`}
              type="submit"
            >
              {busyKey === `create-lesson-${section.id}` ? 'Đang thêm...' : 'Tạo bài học'}
            </button>
          </div>
        </form>
      )}
    </section>
  );
}

function nullableField(value: FormDataEntryValue | null): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed || null;
}
