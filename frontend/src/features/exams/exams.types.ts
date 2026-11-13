/** Kiểu dữ liệu theo OpenAPI v1.5 (tag exams). Điểm là phần trăm 0–100, server gửi dạng number. */
export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED'
export type SubmissionType = 'MANUAL' | 'AUTO_EXPIRED'
export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE'
export type ResultDisplayMode =
  'IMMEDIATE_SCORE_ONLY' | 'IMMEDIATE_WITH_CORRECT_ANSWERS' | 'AFTER_EXAM_CLOSED'

export type ExamView = {
  id: number
  lessonId: number
  title: string
  description: string | null
  durationMinutes: number
  passingScore: number
  maximumAttempts: number | null
  attemptsUsed: number
  resultDisplayMode: ResultDisplayMode
  startAt: string | null
  endAt: string | null
  canStart: boolean
  inProgressAttemptId: number | null
  bestScore: number | null
  passed: boolean | null
}

export type ExamAttempt = {
  id: number
  examId: number
  attemptNumber: number
  status: AttemptStatus
  submissionType: SubmissionType | null
  startedAt: string
  expiresAt: string
  submittedAt: string | null
  serverTime: string
  score: number | null
  maximumScore: number
  passingScore: number
  passed: boolean | null
  version: number
}

export type OptionView = {
  id: number
  content: string
}

export type AttemptQuestionView = {
  id: number
  position: number
  questionType: QuestionType
  content: string
  options: OptionView[]
  maximumScore: number
  selectedOptionIds: number[]
}

export type ExamAttemptSession = {
  attempt: ExamAttempt
  questions: AttemptQuestionView[]
}

export type ExamAnswerAck = {
  attemptQuestionId: number
  selectedOptionIds: number[]
  savedAt: string
  attemptVersion: number
}

export type AnswerResult = {
  attemptQuestionId: number
  selectedOptionIds: number[]
  correctOptionIds: number[]
  correct: boolean | null
  scoreAwarded: number | null
  explanation: string | null
}

export type ExamResult = {
  attemptId: number
  examId: number
  status: AttemptStatus
  submissionType: SubmissionType | null
  score: number | null
  maximumScore: number
  passingScore: number
  passed: boolean | null
  submittedAt: string | null
  resultDisplayMode: ResultDisplayMode
  answers: AnswerResult[] | null
}
