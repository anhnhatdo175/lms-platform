import { useEffect, useState } from 'react';
import { ApiRequestError } from '../services/authApi';
import {
  decideCourseReview,
  listCourseReviewRequests,
  type CourseReviewRequestSummary,
} from '../services/courseApi';
import styles from './CourseCategoriesPage.module.css';

export function CourseReviewQueue({ accessToken }: { accessToken: string }) {
  const [requests, setRequests] = useState<CourseReviewRequestSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [busyCourseId, setBusyCourseId] = useState<number | null>(null);
  const [rejectingCourseId, setRejectingCourseId] = useState<number | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [rejectionError, setRejectionError] = useState('');
  const [notice, setNotice] = useState<{ message: string; error: boolean } | null>(null);

  useEffect(() => {
    let active = true;
    listCourseReviewRequests(accessToken)
      .then((page) => {
        if (active) setRequests(page.data);
      })
      .catch((error: unknown) => {
        if (active) {
          setNotice({
            message: error instanceof ApiRequestError
              ? error.message
              : 'Không thể tải các yêu cầu duyệt khóa học.',
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

  async function decideReview(
    request: CourseReviewRequestSummary,
    decision: 'approve' | 'reject',
    reason?: string,
  ) {
    const trimmedReason = reason?.trim();
    if (decision === 'reject' && !trimmedReason) {
      setRejectionError('Vui lòng nhập lý do từ chối.');
      return;
    }

    setBusyCourseId(request.courseId);
    setNotice(null);
    try {
      await decideCourseReview(accessToken, request.courseId, decision, trimmedReason);
      const page = await listCourseReviewRequests(accessToken);
      setRequests(page.data);
      setRejectingCourseId(null);
      setRejectionReason('');
      setRejectionError('');
      setNotice({
        message: decision === 'approve' ? 'Đã duyệt và xuất bản khóa học.' : 'Đã từ chối khóa học.',
        error: false,
      });
    } catch (error) {
      setNotice({
        message: error instanceof ApiRequestError ? error.message : 'Không thể xử lý yêu cầu duyệt.',
        error: true,
      });
    } finally {
      setBusyCourseId(null);
    }
  }

  function openRejectionForm(courseId: number) {
    setRejectingCourseId((current) => current === courseId ? null : courseId);
    setRejectionReason('');
    setRejectionError('');
  }

  return (
    <section className={styles.card} aria-labelledby="course-review-title">
      <div className={styles.cardHeading}>
        <h2 id="course-review-title">Yêu cầu duyệt khóa học <span>{requests.length}</span></h2>
        <p>Duyệt sẽ xuất bản ngay; từ chối cần ghi rõ lý do để giảng viên xem và mở lại khóa học.</p>
      </div>
      {notice && (
        <div className={notice.error ? styles.errorNotice : styles.successNotice} role={notice.error ? 'alert' : 'status'}>
          {notice.message}
        </div>
      )}
      {isLoading ? (
        <p className={styles.emptyState}>Đang tải yêu cầu duyệt...</p>
      ) : requests.length === 0 ? (
        <p className={styles.emptyState}>Hiện không có khóa học nào chờ duyệt.</p>
      ) : (
        <ul className={styles.categoryList}>
          {requests.map((request) => (
            <li className={styles.categoryRow} key={request.id}>
              <div className={styles.categoryInfo}>
                <strong>{request.courseTitle}</strong>
                <span>Giảng viên: {request.teacherName}</span>
                <small>Gửi lúc {new Date(request.submittedAt).toLocaleString('vi-VN')}</small>
              </div>
              <div className={styles.categoryActions}>
                <button
                  className={styles.primaryButton}
                  disabled={busyCourseId === request.courseId}
                  onClick={() => void decideReview(request, 'approve')}
                  type="button"
                >
                  {busyCourseId === request.courseId ? 'Đang xử lý...' : 'Duyệt & xuất bản'}
                </button>
                <button
                  className={styles.secondaryButton}
                  disabled={busyCourseId === request.courseId}
                  onClick={() => openRejectionForm(request.courseId)}
                  type="button"
                >
                  {rejectingCourseId === request.courseId ? 'Đóng biểu mẫu' : 'Từ chối'}
                </button>
              </div>
              {rejectingCourseId === request.courseId && (
                <form
                  className={styles.reviewRejectForm}
                  onSubmit={(event) => {
                    event.preventDefault();
                    void decideReview(request, 'reject', rejectionReason);
                  }}
                >
                  <label className={styles.field} htmlFor={`rejection-reason-${request.courseId}`}>
                    <span>Lý do từ chối khóa học</span>
                    <textarea
                      autoFocus
                      id={`rejection-reason-${request.courseId}`}
                      maxLength={2000}
                      onChange={(event) => {
                        setRejectionReason(event.target.value);
                        setRejectionError('');
                      }}
                      required
                      rows={3}
                      value={rejectionReason}
                    />
                  </label>
                  {rejectionError && <p className={styles.errorNotice} role="alert">{rejectionError}</p>}
                  <div className={styles.categoryActions}>
                    <button
                      className={styles.primaryButton}
                      disabled={busyCourseId === request.courseId}
                      type="submit"
                    >
                      {busyCourseId === request.courseId ? 'Đang xử lý...' : 'Xác nhận từ chối'}
                    </button>
                    <button
                      className={styles.secondaryButton}
                      disabled={busyCourseId === request.courseId}
                      onClick={() => openRejectionForm(request.courseId)}
                      type="button"
                    >
                      Hủy
                    </button>
                  </div>
                </form>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
