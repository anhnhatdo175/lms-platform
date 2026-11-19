import { api, type DataResponse, type PageResponse } from '@/lib/api-client'
import type { QuestionType } from './exams.types'

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED'
export type ResultDisplayMode =
  | 'IMMEDIATE_SCORE_ONLY'
  | 'IMMEDIATE_WITH_CORRECT_ANSWERS'
  | 'AFTER_EXAM_CLOSED'

export interface TeacherQuestion {
  id: number
  courseId: number
  content: string
  questionType: QuestionType
  difficulty: Difficulty
  skillCode: string | null
  source: string
  explanation: string | null
  active: boolean
  version: number
  options: Array<{ id: number; content: string; correct: boolean; position: number }>
}

export interface TeacherExamSummary {
  id: number
  lessonId: number
  title: string
  description: string | null
  durationMinutes: number
  passingScore: number
  maximumAttempts: number | null
  shuffleQuestions: boolean
  shuffleOptions: boolean
  resultDisplayMode: ResultDisplayMode
  startAt: string | null
  endAt: string | null
  questionCount: number
  status: ExamStatus
  maximumScore: number
}

export interface TeacherExamBuilder {
  exam: TeacherExamSummary
  questions: Array<{
    questionId: number
    position: number
    score: number
    required: boolean
    question: TeacherQuestion
  }>
}

export interface TeacherExamStatistics {
  examId: number
  attemptCount: number
  studentCount: number
  averageScore: number | null
  passRate: number | null
  scoreDistribution: Array<{ from: number; to: number; count: number }>
  questionStats: Array<{
    questionId: number
    content: string
    correctRate: number
    answeredCount: number
  }>
}

export const teacherExamsApi = {
  listQuestions: (courseId: number) =>
    api.get<PageResponse<TeacherQuestion>>(`/teacher/courses/${courseId}/questions`, {
      query: { page: 0, size: 100, active: true },
    }),

  createQuestion: (courseId: number, input: {
    content: string
    questionType: QuestionType
    difficulty: Difficulty
    skillCode: string | null
    explanation: string | null
    options: Array<{ content: string; correct: boolean; position: number }>
  }) =>
    api.post<DataResponse<TeacherQuestion>>(`/teacher/courses/${courseId}/questions`, input)
      .then((response) => response.data),

  deactivateQuestion: (questionId: number) =>
    api.delete(`/teacher/questions/${questionId}`),

  getExam: (examId: number) =>
    api.get<DataResponse<TeacherExamBuilder>>(`/teacher/exams/${examId}`)
      .then((response) => response.data),

  createExam: (lessonId: number, input: {
    title: string
    description: string | null
    durationMinutes: number
    passingScore: number
    maximumAttempts: number | null
    shuffleQuestions: boolean
    shuffleOptions: boolean
    resultDisplayMode: ResultDisplayMode
    startAt: string | null
    endAt: string | null
  }) =>
    api.post<DataResponse<TeacherExamSummary>>(`/teacher/lessons/${lessonId}/exams`, input)
      .then((response) => response.data),

  updateExam: (examId: number, input: {
      title: string
      description: string | null
      durationMinutes: number
      passingScore: number
      maximumAttempts: number | null
      shuffleQuestions: boolean
      shuffleOptions: boolean
      resultDisplayMode: ResultDisplayMode
  }) =>
      api.patch<DataResponse<TeacherExamSummary>>(`/teacher/exams/${examId}`, input)
        .then((response) => response.data),

  addQuestion: (examId: number, questionId: number, score: number) =>
    api.post<DataResponse<TeacherExamBuilder>>(`/teacher/exams/${examId}/questions`, {
      questionId,
      score,
      required: true,
    }).then((response) => response.data),

  removeQuestion: (examId: number, questionId: number) =>
    api.delete(`/teacher/exams/${examId}/questions/${questionId}`),

  publish: (examId: number) =>
    api.post<DataResponse<TeacherExamSummary>>(`/teacher/exams/${examId}/publish`)
      .then((response) => response.data),

  close: (examId: number) =>
    api.post<DataResponse<TeacherExamSummary>>(`/teacher/exams/${examId}/close`)
      .then((response) => response.data),

  statistics: (examId: number) =>
    api.get<DataResponse<TeacherExamStatistics>>(`/teacher/exams/${examId}/statistics`)
      .then((response) => response.data),
}
