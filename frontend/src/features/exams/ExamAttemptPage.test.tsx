// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ExamAttemptPage } from './ExamAttemptPage'

vi.mock('@/services/authApi', () => ({
  restoreSession: () => Promise.resolve({ accessToken: 'test-token' }),
}))
import type { ExamAttemptSession } from './exams.types'

type Call = { method: string; url: string; body: unknown }

function session(expiresInMs: number): ExamAttemptSession {
  const now = Date.now()
  return {
    attempt: {
      id: 7,
      examId: 3,
      attemptNumber: 1,
      status: 'IN_PROGRESS',
      submissionType: null,
      startedAt: new Date(now).toISOString(),
      expiresAt: new Date(now + expiresInMs).toISOString(),
      submittedAt: null,
      serverTime: new Date(now).toISOString(),
      score: null,
      maximumScore: 2,
      passingScore: 50,
      passed: null,
      version: 3,
    },
    questions: [
      {
        id: 101,
        position: 1,
        questionType: 'SINGLE_CHOICE',
        content: 'Java chạy trên gì?',
        maximumScore: 1,
        selectedOptionIds: [],
        options: [
          { id: 1, content: 'JVM' },
          { id: 2, content: 'CLR' },
        ],
      },
      {
        id: 102,
        position: 2,
        questionType: 'MULTIPLE_CHOICE',
        content: 'Kiểu nguyên thủy?',
        maximumScore: 1,
        selectedOptionIds: [],
        options: [
          { id: 3, content: 'int' },
          { id: 4, content: 'String' },
        ],
      },
    ],
  }
}

function serve(handler: (call: Call) => { status: number; body: unknown }) {
  const calls: Call[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string, init: RequestInit) => {
      const call = {
        method: init.method ?? 'GET',
        url,
        body: init.body ? (JSON.parse(init.body as string) as unknown) : undefined,
      }
      calls.push(call)
      const { status, body } = handler(call)
      return Promise.resolve(new Response(JSON.stringify(body), { status }))
    }),
  )
  return calls
}

function renderAt(path: string) {
  const router = createMemoryRouter(
    [
      { path: '/exam-attempts/:attemptId', element: <ExamAttemptPage /> },
      { path: '/exam-attempts/:attemptId/result', element: <p>trang kết quả</p> },
    ],
    { initialEntries: [path] },
  )
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

const ok = (data: unknown) => ({ status: 200, body: { data } })
const ack = (aq: number, ids: number[], version: number) =>
  ok({
    attemptQuestionId: aq,
    selectedOptionIds: ids,
    savedAt: new Date().toISOString(),
    attemptVersion: version,
  })

describe('ExamAttemptPage', () => {
  beforeEach(() => vi.spyOn(console, 'error').mockImplementation(() => undefined))
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('autosaves with the version token, then flushes and submits', async () => {
    let version = 3
    const calls = serve(({ method, url, body }) => {
      if (method === 'GET') return ok(session(30 * 60_000))
      if (method === 'PUT') {
        const aq = Number(url.split('/').pop())
        version += 1
        return ack(aq, (body as { selectedOptionIds: number[] }).selectedOptionIds, version)
      }
      return ok({ ...session(0).attempt, status: 'GRADED' })
    })
    renderAt('/exam-attempts/7')

    fireEvent.click(await screen.findByLabelText('JVM'))
    await waitFor(() => expect(calls.some((c) => c.method === 'PUT')).toBe(true), { timeout: 2000 })
    expect(calls.find((c) => c.method === 'PUT')).toMatchObject({
      url: '/api/v1/exam-attempts/7/answers/101',
      body: { selectedOptionIds: [1], version: 3 },
    })
    await waitFor(() => expect(screen.getByText('Đã lưu')).toBeTruthy())

    // Câu 2 chọn rồi nộp ngay, trước khi hết debounce: phải được gửi (version mới) trước lệnh nộp
    fireEvent.click(screen.getByRole('button', { name: /Câu 2/ }))
    fireEvent.click(screen.getByLabelText('int'))
    fireEvent.click(screen.getByRole('button', { name: 'Nộp bài' }))
    const dialogSubmit = await screen.findAllByRole('button', { name: 'Nộp bài' })
    fireEvent.click(dialogSubmit[dialogSubmit.length - 1])

    await screen.findByText('trang kết quả')
    const writes = calls
      .filter((c) => c.method !== 'GET')
      .map((c) => `${c.method} ${c.url.replace('/api/v1', '')}`)
    expect(writes).toEqual([
      'PUT /exam-attempts/7/answers/101',
      'PUT /exam-attempts/7/answers/102',
      'POST /exam-attempts/7/submit',
    ])
    expect(calls.filter((c) => c.method === 'PUT')[1].body).toMatchObject({
      selectedOptionIds: [3],
      version: 4,
    })
  })

  it('goes to the result page when the server says time is up', async () => {
    serve(({ method }) =>
      method === 'GET'
        ? ok(session(30 * 60_000))
        : {
            status: 409,
            body: { error: { code: 'ATTEMPT_EXPIRED', message: 'Time is up', traceId: 't' } },
          },
    )
    renderAt('/exam-attempts/7')
    fireEvent.click(await screen.findByLabelText('JVM'))
    await screen.findByText('trang kết quả', undefined, { timeout: 2000 })
  })

  it('submits by itself when the server clock runs out', async () => {
    const calls = serve(({ method }) =>
      method === 'GET'
        ? ok(session(1200))
        : ok({ ...session(0).attempt, status: 'GRADED', submissionType: 'AUTO_EXPIRED' }),
    )
    renderAt('/exam-attempts/7')
    await screen.findByRole('timer')
    await screen.findByText('trang kết quả', undefined, { timeout: 3000 })
    expect(calls.filter((c) => c.method === 'POST').map((c) => c.url)).toEqual([
      '/api/v1/exam-attempts/7/submit',
    ])
  })

  it('redirects to the result when the attempt is already graded', async () => {
    serve(() => ok({ ...session(0), attempt: { ...session(0).attempt, status: 'GRADED' } }))
    renderAt('/exam-attempts/7')
    await screen.findByText('trang kết quả')
  })
})
