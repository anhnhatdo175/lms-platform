import { useNavigate, useParams } from 'react-router'

import { Alert, Badge, Button, Card, CardBody, CardHeader, Icons, Skeleton } from '@/components/ui'
import { apiErrorMessage } from '@/utils/apiErrorMessage'
import { formatDateTime, formatScore } from '@/utils/format'

import styles from './exams.module.css'
import { useLearningExam, useStartAttempt } from './exams.hooks'

const DISPLAY_MODE = {
  IMMEDIATE_SCORE_ONLY: 'Xem điểm ngay sau khi nộp',
  IMMEDIATE_WITH_CORRECT_ANSWERS: 'Xem điểm và đáp án ngay sau khi nộp',
  AFTER_EXAM_CLOSED: 'Điểm và đáp án công bố khi bài thi đóng',
} as const

/** Thông tin trước khi thi (`GET /learning/exams/{id}`); bắt đầu hoặc tiếp tục Attempt đang làm. */
export function ExamStartPage() {
  const navigate = useNavigate()
  const examId = Number(useParams().examId)
  const exam = useLearningExam(examId)
  const start = useStartAttempt(examId)

  if (exam.isPending) {
    return (
      <main className={styles.page}>
        <Skeleton height={240} />
      </main>
    )
  }
  if (exam.isError) {
    return (
      <main className={styles.page}>
        <Alert variant="destructive" title="Không mở được bài thi">
          {apiErrorMessage(exam.error)}
        </Alert>
      </main>
    )
  }

  const e = exam.data
  const attemptsLeft =
    e.maximumAttempts === null ? null : Math.max(0, e.maximumAttempts - e.attemptsUsed)

  const handleStart = async () => {
    const timed = await start.mutateAsync()
    await navigate(`/exam-attempts/${timed.session.attempt.id}`)
  }

  return (
    <main className={styles.page}>
      <Card>
        <CardHeader title={e.title} subtitle={e.description ?? undefined} />
        <CardBody>
          <dl className={styles.facts}>
            <dt>Thời gian</dt>
            <dd>{e.durationMinutes} phút</dd>
            <dt>Điểm đạt</dt>
            <dd>{formatScore(e.passingScore)}/100</dd>
            <dt>Số lần làm</dt>
            <dd>
              {e.attemptsUsed}
              {e.maximumAttempts !== null ? `/${e.maximumAttempts}` : ' (không giới hạn)'}
            </dd>
            {e.startAt && (
              <>
                <dt>Mở lúc</dt>
                <dd>{formatDateTime(e.startAt)}</dd>
              </>
            )}
            {e.endAt && (
              <>
                <dt>Đóng lúc</dt>
                <dd>{formatDateTime(e.endAt)}</dd>
              </>
            )}
            <dt>Kết quả</dt>
            <dd>{DISPLAY_MODE[e.resultDisplayMode]}</dd>
            {e.bestScore !== null && (
              <>
                <dt>Điểm cao nhất</dt>
                <dd>
                  {formatScore(e.bestScore)}{' '}
                  {e.passed ? (
                    <Badge variant="success">Đạt</Badge>
                  ) : (
                    <Badge variant="warning">Chưa đạt</Badge>
                  )}
                </dd>
              </>
            )}
          </dl>

          <Alert variant="info" className={styles.notice}>
            Đồng hồ chạy theo giờ máy chủ và không dừng khi bạn rời trang. Câu trả lời được lưu tự
            động; hết giờ, bài được nộp với những câu đã lưu.
          </Alert>

          {start.isError && (
            <Alert variant="destructive" title="Chưa bắt đầu được">
              {apiErrorMessage(start.error)}
            </Alert>
          )}

          <div className={styles.actions}>
            {e.inProgressAttemptId !== null ? (
              <Button
                variant="accent"
                size="lg"
                leftIcon={<Icons.Play size={18} />}
                onClick={() => void navigate(`/exam-attempts/${e.inProgressAttemptId}`)}
              >
                Tiếp tục bài đang làm
              </Button>
            ) : (
              <Button
                variant="accent"
                size="lg"
                leftIcon={<Icons.Play size={18} />}
                disabled={!e.canStart}
                isLoading={start.isPending}
                onClick={() => void handleStart().catch(() => undefined)}
              >
                Bắt đầu làm bài
              </Button>
            )}
            {!e.canStart && e.inProgressAttemptId === null && (
              <span className={styles.muted}>
                {attemptsLeft === 0
                  ? 'Bạn đã dùng hết số lần làm bài.'
                  : 'Bài thi hiện chưa thể bắt đầu.'}
              </span>
            )}
          </div>
        </CardBody>
      </Card>
    </main>
  )
}
