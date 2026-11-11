import { Link, useParams } from 'react-router'

import { Alert, Badge, Card, CardBody, CardHeader, Icons, Skeleton } from '@/components/ui'
import { apiErrorMessage } from '@/utils/apiErrorMessage'
import { formatDateTime, formatScore } from '@/utils/format'

import styles from './exams.module.css'
import { useAttemptSession, useExamResult } from './exams.hooks'
import type { AnswerResult, AttemptQuestionView } from './exams.types'

/** Kết quả theo chế độ hiển thị của Exam (Spec 14.7): server đã ẩn những gì chưa được công bố. */
export function ExamResultPage() {
  const attemptId = Number(useParams().attemptId)
  const result = useExamResult(attemptId)
  const session = useAttemptSession(attemptId)

  if (result.isPending) {
    return (
      <main className={styles.page}>
        <Skeleton height={200} />
      </main>
    )
  }
  if (result.isError) {
    return (
      <main className={styles.page}>
        <Alert variant="destructive" title="Chưa xem được kết quả">
          {apiErrorMessage(result.error)}
        </Alert>
      </main>
    )
  }

  const r = result.data
  const questions = session.data?.session.questions ?? []
  const answerByQuestion = new Map((r.answers ?? []).map((a) => [a.attemptQuestionId, a]))

  return (
    <main className={styles.page}>
      <Card>
        <CardHeader
          title="Kết quả bài làm"
          subtitle={`Nộp lúc ${formatDateTime(r.submittedAt)}${r.submissionType === 'AUTO_EXPIRED' ? ' (tự nộp khi hết giờ)' : ''}`}
          action={
            r.passed === null ? undefined : r.passed ? (
              <Badge variant="success" leftIcon={<Icons.Award size={14} />}>
                Đạt
              </Badge>
            ) : (
              <Badge variant="warning">Chưa đạt</Badge>
            )
          }
        />
        <CardBody>
          {r.score === null ? (
            <Alert variant="info">Điểm và đáp án sẽ được công bố khi bài thi đóng.</Alert>
          ) : (
            <p className={styles.score}>
              {formatScore(r.score)}
              <span className={styles.muted}> / 100 · điểm đạt {formatScore(r.passingScore)}</span>
            </p>
          )}
          <Link to={`/learning/exams/${r.examId}`}>Về trang bài thi</Link>
        </CardBody>
      </Card>

      {r.answers && (
        <section className={styles.review} aria-label="Đáp án chi tiết">
          {questions.map((q) => (
            <ReviewItem key={q.id} question={q} answer={answerByQuestion.get(q.id)} />
          ))}
        </section>
      )}
    </main>
  )
}

function ReviewItem({
  question,
  answer,
}: {
  question: AttemptQuestionView
  answer: AnswerResult | undefined
}) {
  const selected = new Set(answer?.selectedOptionIds ?? [])
  const correct = new Set(answer?.correctOptionIds ?? [])
  return (
    <Card>
      <CardBody>
        <div className={styles.reviewHead}>
          <p className={styles.questionContent}>
            <span className={styles.questionNumber}>Câu {question.position}.</span>{' '}
            {question.content}
          </p>
          {answer?.correct ? (
            <Badge variant="success">+{formatScore(answer.scoreAwarded)}</Badge>
          ) : (
            <Badge variant="destructive">{selected.size === 0 ? 'Bỏ trống' : 'Sai'}</Badge>
          )}
        </div>
        <ul className={styles.reviewOptions}>
          {question.options.map((o) => (
            <li
              key={o.id}
              className={`${styles.reviewOption} ${correct.has(o.id) ? styles.reviewCorrect : ''} ${selected.has(o.id) && !correct.has(o.id) ? styles.reviewWrong : ''}`}
            >
              {correct.has(o.id) ? (
                <Icons.CheckCircle size={16} />
              ) : selected.has(o.id) ? (
                <Icons.Close size={16} />
              ) : null}
              <span>{o.content}</span>
              {selected.has(o.id) && <span className={styles.muted}> (bạn chọn)</span>}
            </li>
          ))}
        </ul>
        {answer?.explanation && <p className={styles.explanation}>{answer.explanation}</p>}
      </CardBody>
    </Card>
  )
}
