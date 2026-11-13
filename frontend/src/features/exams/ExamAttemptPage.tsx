import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useMemo, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router'

import { Alert, Button, Icons, Modal, Skeleton } from '@/components/ui'
import type { ApiError } from '@/lib/api-client'
import { apiErrorMessage } from '@/utils/apiErrorMessage'

import { ChoiceQuestion } from './ChoiceQuestion'
import { clockOffsetMs } from './examClock'
import styles from './exams.module.css'
import { examKeys, useAttemptAnswers, useAttemptSession, useSubmitAttempt } from './exams.hooks'
import { ExamTimer } from './ExamTimer'
import { QuestionNavigator } from './QuestionNavigator'
import { useExamTimer } from './useExamTimer'

/**
 * Màn hình làm bài (Spec 14.5). Mỗi lựa chọn được autosave; hết giờ trên đồng hồ server thì gọi nộp, server
 * chấm AUTO_EXPIRED bằng các đáp án đã lưu. Lựa chọn chưa kịp lưu lúc hết giờ không được tính.
 */
export function ExamAttemptPage() {
  const attemptId = Number(useParams().attemptId)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const session = useAttemptSession(attemptId)
  const submit = useSubmitAttempt(attemptId)
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isConfirmOpen, setIsConfirmOpen] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)

  const goToResult = useCallback(
    () => void navigate(`/exam-attempts/${attemptId}/result`, { replace: true }),
    [navigate, attemptId],
  )

  const handleFatal = useCallback(
    (error: ApiError) => {
      if (error.code === 'CONFLICT') {
        setNotice('Bài làm vừa được lưu từ một cửa sổ khác. Đã tải lại các đáp án mới nhất.')
        void queryClient.invalidateQueries({ queryKey: examKeys.attempt(attemptId) })
      } else if (error.code === 'ATTEMPT_EXPIRED' || error.code === 'ATTEMPT_NOT_IN_PROGRESS') {
        goToResult()
      } else {
        setNotice(apiErrorMessage(error))
      }
    },
    [queryClient, attemptId, goToResult],
  )

  const answers = useAttemptAnswers(session.data, handleFatal)
  const attempt = session.data?.session.attempt
  const isInProgress = attempt?.status === 'IN_PROGRESS'
  const offset = useMemo(
    () =>
      session.data
        ? clockOffsetMs(session.data.session.attempt.serverTime, session.data.receivedAt)
        : 0,
    [session.data],
  )

  const handleElapsed = useCallback(() => {
    answers.stop()
    void submit
      .mutateAsync()
      .catch(() => undefined)
      .finally(goToResult)
  }, [answers, submit, goToResult])

  const remaining = useExamTimer(isInProgress ? attempt.expiresAt : null, offset, handleElapsed)

  const handleSubmit = async () => {
    await answers.flush()
    await submit.mutateAsync()
    goToResult()
  }

  if (session.isPending) {
    return (
      <main className={styles.page}>
        <Skeleton height={48} />
        <Skeleton height={320} />
      </main>
    )
  }
  if (session.isError) {
    return (
      <main className={styles.page}>
        <Alert variant="destructive" title="Không mở được bài làm">
          {apiErrorMessage(session.error)}
        </Alert>
      </main>
    )
  }
  if (!isInProgress) {
    return <Navigate to={`/exam-attempts/${attemptId}/result`} replace />
  }

  const { questions } = session.data.session
  const question = questions[Math.min(currentIndex, questions.length - 1)]
  const unanswered = questions.filter((q) => (answers.selections[q.id] ?? []).length === 0).length
  const states = Object.values(answers.saveStates)
  const saveLabel = states.includes('error')
    ? 'Chưa lưu được, đang thử lại…'
    : states.includes('saving') || states.includes('pending')
      ? 'Đang lưu…'
      : 'Đã lưu'

  return (
    <main className={styles.attemptPage}>
      <header className={styles.attemptHeader}>
        <span className={styles.muted}>Lần làm thứ {attempt.attemptNumber}</span>
        <span className={styles.saveState} aria-live="polite">
          {saveLabel}
        </span>
        {remaining !== null && <ExamTimer remainingMs={remaining} />}
        <Button
          variant="accent"
          onClick={() => setIsConfirmOpen(true)}
          isLoading={submit.isPending}
        >
          Nộp bài
        </Button>
      </header>

      {notice && (
        <Alert variant="warning" onClose={() => setNotice(null)}>
          {notice}
        </Alert>
      )}

      <div className={styles.attemptLayout}>
        <aside>
          <QuestionNavigator
            questions={questions}
            currentIndex={currentIndex}
            selections={answers.selections}
            saveStates={answers.saveStates}
            onSelect={setCurrentIndex}
          />
        </aside>
        <section className={styles.questionPanel}>
          {question && (
            <ChoiceQuestion
              key={question.id}
              question={question}
              selected={answers.selections[question.id] ?? []}
              onChange={(ids) => answers.choose(question.id, ids)}
            />
          )}
          <div className={styles.actions}>
            <Button
              variant="outline"
              leftIcon={<Icons.ChevronLeft size={16} />}
              disabled={currentIndex === 0}
              onClick={() => setCurrentIndex(currentIndex - 1)}
            >
              Câu trước
            </Button>
            <Button
              variant="outline"
              rightIcon={<Icons.ChevronRight size={16} />}
              disabled={currentIndex >= questions.length - 1}
              onClick={() => setCurrentIndex(currentIndex + 1)}
            >
              Câu sau
            </Button>
          </div>
        </section>
      </div>

      <Modal
        isOpen={isConfirmOpen}
        onClose={() => setIsConfirmOpen(false)}
        title="Nộp bài?"
        description={
          unanswered > 0
            ? `Bạn còn ${unanswered} câu chưa trả lời. Sau khi nộp sẽ không sửa được nữa.`
            : 'Sau khi nộp sẽ không sửa được nữa.'
        }
        footer={
          <>
            <Button variant="ghost" onClick={() => setIsConfirmOpen(false)}>
              Làm tiếp
            </Button>
            <Button
              variant="accent"
              isLoading={submit.isPending}
              onClick={() => void handleSubmit().catch(() => undefined)}
            >
              Nộp bài
            </Button>
          </>
        }
      >
        {submit.isError && <Alert variant="destructive">{apiErrorMessage(submit.error)}</Alert>}
      </Modal>
    </main>
  )
}
