import { restoreSession } from '@/services/authApi'

/**
 * API client cho các trang của B (OpenAPI `/api/v1`). Token lấy theo cách của A: access token giữ trong bộ nhớ,
 * khi chưa có hoặc hết hạn (401) thì gọi `restoreSession()` của A (refresh bằng cookie, dùng chung một lượt
 * refresh với app của A) rồi thử lại đúng một lần. A có thể đưa token đang có vào qua `setAccessToken` để đỡ
 * một lần refresh.
 */
export interface ApiErrorDetail {
  field?: string
  reason?: string
  [key: string]: unknown
}

/** Lỗi theo `ErrorResponse` của OpenAPI: `{ error: { code, message, details, traceId } }`. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: ApiErrorDetail[]
  readonly traceId?: string

  constructor(
    status: number,
    code: string,
    message: string,
    details: ApiErrorDetail[] = [],
    traceId?: string,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
    this.traceId = traceId
  }
}

export interface RequestOptions {
  query?: Record<string, string | number | boolean | undefined | null>
  headers?: Record<string, string>
  signal?: AbortSignal
}

const BASE_URL = (import.meta.env.VITE_API_V1_BASE_URL as string | undefined) ?? '/api/v1'

let accessToken: string | null = null

/** A gọi khi đăng nhập, refresh hoặc đăng xuất (null). */
export function setAccessToken(token: string | null): void {
  accessToken = token
}

async function refreshAccessToken(): Promise<string | null> {
  try {
    const session = await restoreSession()
    accessToken = session?.accessToken ?? null
  } catch {
    accessToken = null
  }
  return accessToken
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const params = new URLSearchParams()
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.set(key, String(value))
  })
  const search = params.toString()
  return `${BASE_URL}${path}${search ? `?${search}` : ''}`
}

async function send<T>(
  method: string,
  path: string,
  body: unknown,
  options: RequestOptions,
  token: string | null,
): Promise<T> {
  let response: Response
  try {
    response = await fetch(buildUrl(path, options.query), {
      method,
      credentials: 'include',
      signal: options.signal,
      headers: {
        Accept: 'application/json',
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') throw cause
    throw new ApiError(0, 'NETWORK_ERROR', 'Không kết nối được máy chủ')
  }
  if (response.status === 204) return undefined as T
  const text = await response.text()
  const json: unknown = text ? JSON.parse(text) : undefined
  if (!response.ok) {
    const error = (
      json as {
        error?: { code?: string; message?: string; details?: ApiErrorDetail[]; traceId?: string }
      }
    )?.error
    throw new ApiError(
      response.status,
      error?.code ?? 'INTERNAL_ERROR',
      error?.message ?? response.statusText,
      error?.details ?? [],
      error?.traceId,
    )
  }
  return json as T
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  options: RequestOptions = {},
): Promise<T> {
  const token = accessToken ?? (await refreshAccessToken())
  try {
    return await send<T>(method, path, body, options, token)
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) throw error
    const fresh = await refreshAccessToken()
    if (!fresh || fresh === token) throw error
    return send<T>(method, path, body, options, fresh)
  }
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => request<T>('GET', path, undefined, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('POST', path, body ?? {}, options),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PUT', path, body ?? {}, options),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PATCH', path, body ?? {}, options),
  delete: <T = void>(path: string, options?: RequestOptions) =>
    request<T>('DELETE', path, undefined, options),
}

/** Envelope của OpenAPI. */
export interface DataResponse<T> {
  data: T
}

export interface PageMeta {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PageResponse<T> {
  data: T[]
  meta: PageMeta
}
