import { api, type DataResponse } from '@/lib/api-client'

import type {
  ExamAnswerAck,
  ExamAttempt,
  ExamAttemptSession,
  ExamResult,
  ExamView,
} from './exams.types'

/** Phiên làm bài kèm mốc máy nhận response, để tính chênh lệch đồng hồ máy với server. */
export interface TimedSession {
  session: ExamAttemptSession
  receivedAt: number
}

const timed = (promise: Promise<DataResponse<ExamAttemptSession>>): Promise<TimedSession> =>
  promise.then((r) => ({ session: r.data, receivedAt: Date.now() }))

export const examsApi = {
  learningExam: (examId: number) =>
    api.get<DataResponse<ExamView>>(`/learning/exams/${examId}`).then((r) => r.data),
  start: (examId: number) =>
    timed(
      api.post<DataResponse<ExamAttemptSession>>(`/exams/${examId}/attempts`, {
        clientRequestId: crypto.randomUUID(),
      }),
    ),
  attempt: (attemptId: number) =>
    timed(api.get<DataResponse<ExamAttemptSession>>(`/exam-attempts/${attemptId}`)),
  saveAnswer: (
    attemptId: number,
    attemptQuestionId: number,
    selectedOptionIds: number[],
    version: number,
  ) =>
    api
      .put<DataResponse<ExamAnswerAck>>(
        `/exam-attempts/${attemptId}/answers/${attemptQuestionId}`,
        {
          selectedOptionIds,
          version,
        },
      )
      .then((r) => r.data),
  submit: (attemptId: number) =>
    api
      .post<DataResponse<ExamAttempt>>(`/exam-attempts/${attemptId}/submit`, {
        clientEventId: crypto.randomUUID(),
      })
      .then((r) => r.data),
  result: (attemptId: number) =>
    api.get<DataResponse<ExamResult>>(`/exam-attempts/${attemptId}/result`).then((r) => r.data),
}
