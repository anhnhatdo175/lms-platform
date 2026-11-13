import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState } from 'react'

import { ApiError } from '@/lib/api-client'

import { examsApi, type TimedSession } from './exams.api'
import { AutosaveQueue, type SaveState } from './autosaveQueue'

export const examKeys = {
  learning: (examId: number) => ['exams', 'learning', examId] as const,
  attempt: (attemptId: number) => ['exam-attempts', attemptId] as const,
  result: (attemptId: number) => ['exam-attempts', attemptId, 'result'] as const,
}

export function useLearningExam(examId: number) {
  return useQuery({
    queryKey: examKeys.learning(examId),
    queryFn: () => examsApi.learningExam(examId),
  })
}

export function useStartAttempt(examId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => examsApi.start(examId),
    onSuccess: (timed) => {
      queryClient.setQueryData(examKeys.attempt(timed.session.attempt.id), timed)
      void queryClient.invalidateQueries({ queryKey: examKeys.learning(examId) })
    },
  })
}

/**
 * Phiên làm bài. Không tự tải lại khi quay về tab: dữ liệu server sẽ đè lựa chọn chưa kịp lưu trên màn hình.
 */
export function useAttemptSession(attemptId: number) {
  return useQuery({
    queryKey: examKeys.attempt(attemptId),
    queryFn: () => examsApi.attempt(attemptId),
    staleTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })
}

export function useSubmitAttempt(attemptId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => examsApi.submit(attemptId),
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: examKeys.attempt(attemptId) })
      void queryClient.invalidateQueries({ queryKey: ['exams', 'learning'] })
    },
  })
}

export function useExamResult(attemptId: number) {
  return useQuery({
    queryKey: examKeys.result(attemptId),
    queryFn: () => examsApi.result(attemptId),
  })
}

const FATAL_CODES = new Set([
  'ATTEMPT_EXPIRED',
  'ATTEMPT_NOT_IN_PROGRESS',
  'CONFLICT',
  'NOT_FOUND',
  'FORBIDDEN',
])

/**
 * Lựa chọn trên màn hình và autosave. Lựa chọn hiển thị ngay (optimistic); server là bản lưu chính.
 * `onFatal` nhận lỗi không thử lại được: hết giờ hoặc đã nộp thì sang trang kết quả, xung đột thì tải lại.
 * Thay đổi cục bộ gắn với đúng phiên server đã sinh ra nó, nên tải lại phiên là tự bỏ phần cũ.
 */
export function useAttemptAnswers(
  timed: TimedSession | undefined,
  onFatal: (error: ApiError) => void,
) {
  const [local, setLocal] = useState<{
    owner: TimedSession | undefined
    picks: Record<number, number[]>
  }>({
    owner: undefined,
    picks: {},
  })
  const [states, setStates] = useState<{
    owner: TimedSession | undefined
    byQuestion: Record<number, SaveState>
  }>({
    owner: undefined,
    byQuestion: {},
  })
  const queue = useMemo(() => {
    if (!timed) return null
    return new AutosaveQueue(timed.session.attempt.version, {
      save: (aqId, ids, version) =>
        examsApi.saveAnswer(timed.session.attempt.id, aqId, ids, version),
      onStateChange: (aqId, state) =>
        setStates((prev) => ({
          owner: timed,
          byQuestion: { ...(prev.owner === timed ? prev.byQuestion : {}), [aqId]: state },
        })),
      isFatal: (error) => error instanceof ApiError && FATAL_CODES.has(error.code),
    })
  }, [timed])

  useEffect(() => {
    queue?.setFatalHandler((error) => onFatal(error as ApiError))
  }, [queue, onFatal])

  // Rời trang (hoặc StrictMode gỡ thử) thì gửi nốt câu đang chờ, không bỏ dở
  useEffect(() => {
    if (!queue) return
    return () => void queue.flush()
  }, [queue])

  // Nhắc khi đóng tab lúc còn câu chưa lưu
  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (queue?.hasUnsaved) event.preventDefault()
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [queue])

  const selections = useMemo(() => {
    const fromServer = Object.fromEntries(
      (timed?.session.questions ?? []).map((q) => [q.id, q.selectedOptionIds] as const),
    )
    return local.owner === timed ? { ...fromServer, ...local.picks } : fromServer
  }, [timed, local])

  const saveStates = states.owner === timed ? states.byQuestion : {}

  const choose = useCallback(
    (attemptQuestionId: number, selectedOptionIds: number[]) => {
      setLocal((prev) => ({
        owner: timed,
        picks: {
          ...(prev.owner === timed ? prev.picks : {}),
          [attemptQuestionId]: selectedOptionIds,
        },
      }))
      queue?.change(attemptQuestionId, selectedOptionIds)
    },
    [queue, timed],
  )

  const flush = useCallback(async () => {
    await queue?.flush()
  }, [queue])

  const stop = useCallback(() => queue?.stop(), [queue])

  return { selections, saveStates, choose, flush, stop }
}
