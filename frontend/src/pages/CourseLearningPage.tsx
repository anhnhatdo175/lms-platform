import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { Icons } from '../components/ui/Icons';
import { ApiRequestError } from '../services/authApi';
import {
  completeLearningLesson,
  getLearningLesson,
  getMyCourse,
  sendLessonHeartbeat,
  type CourseOutlineLesson,
  type LessonLearningView,
  type MyCourseDetail,
} from '../services/courseApi';
import { formatDateTime, formatDuration } from '../utils/format';
import styles from './CourseLearningPage.module.css';

interface CourseLearningPageProps {
  accessToken: string;
}

export function CourseLearningPage({ accessToken }: CourseLearningPageProps) {
  const { courseId: rawCourseId } = useParams();
  const navigate = useNavigate();
  const courseId = Number(rawCourseId);
  const isValidCourseId = Number.isSafeInteger(courseId) && courseId > 0;
  const [course, setCourse] = useState<MyCourseDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    if (!isValidCourseId) return undefined;
    getMyCourse(accessToken, courseId)
      .then((result) => {
        if (active) {
          setCourse(result);
          setError('');
        }
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setError(cause instanceof ApiRequestError ? cause.message : 'Không thể tải lộ trình học.');
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [accessToken, courseId, isValidCourseId]);

  if (!isValidCourseId) {
    return <div className={styles.error} role="alert">Đường dẫn khóa học không hợp lệ.</div>;
  }
  if (isLoading) {
    return <p className={styles.stateMessage}>Đang tải lộ trình học...</p>;
  }
  if (error || !course) {
    return (
      <section className={styles.page} aria-labelledby="learning-title">
        <button className={styles.backButton} onClick={() => void navigate('/my-courses')} type="button">
          ← Khóa học của tôi
        </button>
        <div className={styles.error} role="alert">{error || 'Không tìm thấy khóa học.'}</div>
      </section>
    );
  }

  return (
    <section className={styles.page} aria-labelledby="learning-title">
      <button className={styles.backButton} onClick={() => void navigate('/my-courses')} type="button">
        ← Khóa học của tôi
      </button>
      <header className={styles.courseHeader}>
        <div>
          <p className={styles.eyebrow}>LỘ TRÌNH HỌC</p>
          <h1 id="learning-title">{course.enrollment.courseTitle}</h1>
          <p>
            {course.progress.completedLessons} / {course.progress.totalLessons} bài bắt buộc hoàn thành
            {course.enrollment.expiresAt ? ` · Hạn học ${formatDateTime(course.enrollment.expiresAt)}` : ''}
          </p>
        </div>
        <div className={styles.progressSummary} aria-label={`Tiến độ ${course.progress.progressPercent}%`}>
          <strong>{Math.round(course.progress.progressPercent)}%</strong>
          <span>hoàn thành</span>
          <div className={styles.progressTrack}>
            <span style={{ width: `${Math.min(100, Math.max(0, course.progress.progressPercent))}%` }} />
          </div>
        </div>
      </header>

      {course.sections.length === 0 ? (
        <div className={styles.emptyState}>Nội dung khóa học chưa được xuất bản.</div>
      ) : (
        <div className={styles.sections}>
          {course.sections.map((section, index) => (
            <section className={styles.sectionCard} key={section.id} aria-labelledby={`section-${section.id}`}>
              <header className={styles.sectionHeader}>
                <div>
                  <p className={styles.sectionNumber}>CHƯƠNG {index + 1}</p>
                  <h2 id={`section-${section.id}`}>{section.title}</h2>
                </div>
                {section.releaseAt && (
                  <span className={styles.lockBadge}>Mở theo lịch: {formatDateTime(section.releaseAt)}</span>
                )}
              </header>
              <ol className={styles.lessonList}>
                {section.lessons.map((lesson) => (
                  <LessonRow
                    key={lesson.id}
                    lesson={lesson}
                    onOpen={() => void navigate(`/learning/lessons/${lesson.id}`)}
                  />
                ))}
              </ol>
            </section>
          ))}
        </div>
      )}
    </section>
  );
}

function LessonRow({ lesson, onOpen }: { lesson: CourseOutlineLesson; onOpen: () => void }) {
  const isLocked = lesson.access.state !== 'AVAILABLE';
  return (
    <li className={styles.lessonRow}>
      <div className={styles.lessonIcon} aria-hidden="true">
        {lesson.status === 'COMPLETED'
          ? <Icons.CheckCircle size={16} />
          : lesson.lessonType === 'VIDEO'
            ? <Icons.Video size={16} />
            : lesson.lessonType === 'EXAM'
              ? <Icons.FileText size={16} />
              : <Icons.BookOpen size={16} />}
      </div>
      <div className={styles.lessonInfo}>
        <strong>{lesson.title}</strong>
        <span>
          {lesson.lessonType === 'VIDEO' ? 'Video' : lesson.lessonType === 'EXAM' ? 'Bài kiểm tra' : 'Bài học'}
          {lesson.durationSeconds ? ` · ${formatDuration(lesson.durationSeconds * 1000)}` : ''}
          {lesson.mandatory ? ' · Bắt buộc' : ''}
        </span>
        {isLocked && (
          <small>
            {lesson.access.state === 'LOCKED_UNTIL_RELEASE'
              ? `Mở vào ${lesson.access.releaseAt ? formatDateTime(lesson.access.releaseAt) : 'thời điểm được lên lịch'}`
              : lesson.access.prerequisite?.lessonId
                ? `Cần hoàn thành bài #${lesson.access.prerequisite.lessonId} trước`
                : 'Cần đạt điều kiện tiên quyết trước'}
          </small>
        )}
      </div>
      <span className={`${styles.statusBadge} ${styles[`status_${lesson.status}`]}`}>
        {lesson.status === 'COMPLETED' ? 'Hoàn thành' : lesson.status === 'IN_PROGRESS' ? 'Đang học' : 'Chưa học'}
      </span>
      <button
        className={styles.openButton}
        disabled={isLocked}
        onClick={onOpen}
        type="button"
      >
        {isLocked ? 'Đang khóa' : lesson.status === 'COMPLETED' ? 'Ôn lại' : 'Mở bài'}
      </button>
    </li>
  );
}

interface LessonLearningPageProps {
  accessToken: string;
}

export function LessonLearningPage({ accessToken }: LessonLearningPageProps) {
  const { lessonId: rawLessonId } = useParams();
  const navigate = useNavigate();
  const lessonId = Number(rawLessonId);
  const isValidLessonId = Number.isSafeInteger(lessonId) && lessonId > 0;
  const [lesson, setLesson] = useState<LessonLearningView | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCompleting, setIsCompleting] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const lastHeartbeatAt = useRef(0);

  useEffect(() => {
    let active = true;
    if (!isValidLessonId) return undefined;
    lastHeartbeatAt.current = 0;
    getLearningLesson(accessToken, lessonId)
      .then((result) => {
        if (active) {
          setLesson(result);
          setError('');
        }
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setError(cause instanceof ApiRequestError ? cause.message : 'Không thể tải bài học.');
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [accessToken, isValidLessonId, lessonId]);

  async function recordHeartbeat(positionSeconds: number, force = false) {
    if (!lesson || lesson.lessonType !== 'VIDEO' || lesson.completionMethod !== 'VIDEO_PERCENT') return;
    const now = Date.now();
    if (!force && now - lastHeartbeatAt.current < 10_000) return;
    lastHeartbeatAt.current = now;
    try {
      const boundedPosition = lesson.durationSeconds === null
        ? positionSeconds
        : Math.min(positionSeconds, lesson.durationSeconds);
      const progress = await sendLessonHeartbeat(
        accessToken,
        lesson.id,
        boundedPosition,
        crypto.randomUUID(),
      );
      setLesson((current) => current && ({
        ...current,
        progress: {
          ...current.progress,
          status: progress.status,
          progressSeconds: progress.progressSeconds,
          progressPercent: progress.progressPercent,
        },
      }));
      setError('');
    } catch (cause) {
      setError(cause instanceof ApiRequestError ? cause.message : 'Không thể lưu tiến độ video.');
    }
  }

  async function markComplete() {
    if (!lesson || isCompleting) return;
    setIsCompleting(true);
    setError('');
    setNotice('');
    try {
      const result = await completeLearningLesson(accessToken, lesson.id, crypto.randomUUID());
      setLesson((current) => current && ({
        ...current,
        progress: { ...current.progress, status: result.status, completedAt: result.completedAt },
      }));
      setNotice('Đã ghi nhận hoàn thành bài học.');
    } catch (cause) {
      setError(cause instanceof ApiRequestError ? cause.message : 'Không thể hoàn thành bài học.');
    } finally {
      setIsCompleting(false);
    }
  }

  if (!isValidLessonId) {
    return <div className={styles.error} role="alert">Đường dẫn bài học không hợp lệ.</div>;
  }
  if (isLoading) return <p className={styles.stateMessage}>Đang tải bài học...</p>;
  if (error && !lesson) {
    return (
      <section className={styles.page}>
        <button className={styles.backButton} onClick={() => void navigate('/my-courses')} type="button">
          ← Khóa học của tôi
        </button>
        <div className={styles.error} role="alert">{error}</div>
      </section>
    );
  }
  if (!lesson) return null;

  return (
    <article className={styles.lessonPage} aria-labelledby="lesson-title">
      <button
        className={styles.backButton}
        onClick={() => void navigate(`/learning/courses/${lesson.courseId}`)}
        type="button"
      >
        ← Quay lại lộ trình
      </button>
      <header className={styles.lessonHeader}>
        <p className={styles.eyebrow}>
          {lesson.lessonType === 'VIDEO' ? 'BÀI HỌC VIDEO' : lesson.lessonType === 'EXAM' ? 'BÀI KIỂM TRA' : 'NỘI DUNG HỌC'}
        </p>
        <h1 id="lesson-title">{lesson.title}</h1>
        {lesson.description && <p>{lesson.description}</p>}
      </header>

      {error && <div className={styles.error} role="alert">{error}</div>}
      {notice && <div className={styles.notice} role="status">{notice}</div>}

      {lesson.lessonType === 'VIDEO' && lesson.videoUrl ? (
        <video
          className={styles.video}
          controls
          playsInline
          onTimeUpdate={(event) => {
            const seconds = Math.floor(event.currentTarget.currentTime);
            if (seconds > 0) void recordHeartbeat(seconds);
          }}
          onEnded={(event) => void recordHeartbeat(Math.floor(event.currentTarget.currentTime), true)}
          src={lesson.videoUrl}
        >
          Trình duyệt của bạn không hỗ trợ phát video.
        </video>
      ) : lesson.lessonType === 'EXAM' ? (
        <section className={styles.examCard}>
          <h2>Bài kiểm tra của chương</h2>
          <p>Hoàn thành bài thi để ghi nhận kết quả và cập nhật tiến độ khóa học.</p>
          {lesson.examId ? (
            <button
              className={styles.primaryButton}
              onClick={() => void navigate(`/learning/exams/${lesson.examId}`)}
              type="button"
            >
              Bắt đầu bài thi
            </button>
          ) : (
            <p className={styles.stateMessage}>Bài thi chưa được cấu hình.</p>
          )}
        </section>
      ) : (
        <div className={styles.textContent}>
          {lesson.content?.trim() || 'Nội dung bài học đang được cập nhật.'}
        </div>
      )}

      {lesson.resources.length > 0 && (
        <section className={styles.resources} aria-labelledby="lesson-resources-title">
          <h2 id="lesson-resources-title">Tài liệu đính kèm</h2>
          <ul>
            {lesson.resources.map((resource) => (
              <li key={resource.id}>
                <span>{resource.title}</span>
                {resource.url
                  ? <a href={resource.url} rel="noreferrer" target="_blank">{resource.downloadable ? 'Tải tài liệu' : 'Mở tài liệu'}</a>
                  : <span className={styles.resourceUnavailable}>Chưa có liên kết</span>}
              </li>
            ))}
          </ul>
        </section>
      )}

      {lesson.completionMethod === 'MANUAL' && lesson.progress.status !== 'COMPLETED' && (
        <div className={styles.lessonActions}>
          <button className={styles.primaryButton} disabled={isCompleting} onClick={() => void markComplete()} type="button">
            {isCompleting ? 'Đang lưu...' : 'Đánh dấu hoàn thành'}
          </button>
        </div>
      )}
      <p className={styles.progressNote}>
        {lesson.progress.status === 'COMPLETED'
          ? `Đã hoàn thành${lesson.progress.completedAt ? ` lúc ${formatDateTime(lesson.progress.completedAt)}` : ''}.`
          : `Tiến độ video: ${Math.round(lesson.progress.progressPercent)}%`}
      </p>
    </article>
  );
}
