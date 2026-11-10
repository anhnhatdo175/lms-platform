export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT';
export type UserStatus = 'ACTIVE' | 'LOCKED';

export interface AuthUser {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
}

export interface ManagedUser extends AuthUser {
  status: UserStatus;
  createdAt: string;
}

export interface UserDirectory {
  items: ManagedUser[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  activeUsers: number;
  lockedUsers: number;
  studentUsers: number;
  teacherUsers: number;
  adminUsers: number;
}

export interface AuthSession {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: AuthUser;
}

interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
  };
}

export class ApiRequestError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(
    message: string,
    status: number,
    code?: string,
  ) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = code;
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/+$/, '');
let sessionRefresh: Promise<AuthSession | null> | null = null;

export function login(email: string, password: string): Promise<AuthSession> {
  return request<AuthSession>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function register(fullName: string, email: string, password: string): Promise<AuthSession> {
  return request<AuthSession>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ fullName, email, password }),
  });
}

export function restoreSession(): Promise<AuthSession | null> {
  if (!sessionRefresh) {
    sessionRefresh = refreshSession()
      .then((session) => session)
      .catch((error: unknown) => {
        if (error instanceof ApiRequestError && error.status === 401) {
          return null;
        }
        throw error;
      })
      .finally(() => {
        sessionRefresh = null;
      });
  }
  return sessionRefresh;
}

export function refreshSession(): Promise<AuthSession> {
  return request<AuthSession>('/auth/refresh', { method: 'POST' });
}

export function logout(accessToken: string): Promise<void> {
  return request<void>('/auth/logout', {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

export function updateProfile(accessToken: string, fullName: string): Promise<ManagedUser> {
  return request<ManagedUser>('/users/me', {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({ fullName }),
  });
}

export function listUsers(
  accessToken: string,
  filters: { query?: string; role?: UserRole | ''; status?: UserStatus | ''; page?: number },
): Promise<UserDirectory> {
  const search = new URLSearchParams();
  if (filters.query) search.set('query', filters.query);
  if (filters.role) search.set('role', filters.role);
  if (filters.status) search.set('status', filters.status);
  search.set('page', String(filters.page ?? 0));
  search.set('size', '10');
  return request<UserDirectory>(`/users?${search.toString()}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

export function updateUser(
  accessToken: string,
  userId: number,
  changes: { role?: UserRole; status?: UserStatus },
): Promise<ManagedUser> {
  return request<ManagedUser>(`/users/${userId}`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify(changes),
  });
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
  let response: Response;
  try {
    const headers = new Headers(init.headers);
    headers.set('Accept', 'application/json');
    if (init.body) headers.set('Content-Type', 'application/json');
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      credentials: 'include',
      headers,
    });
  } catch (error) {
    if (error instanceof TypeError) {
      throw new ApiRequestError('Không thể kết nối dịch vụ. Vui lòng thử lại sau.', 0);
    }
    throw error;
  }

  if (!response.ok) {
    const body = await readErrorBody(response);
    throw new ApiRequestError(
      body.error?.message ?? 'Yêu cầu không thành công. Vui lòng thử lại.',
      response.status,
      body.error?.code,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }
  try {
    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new ApiRequestError('Dịch vụ trả về dữ liệu không hợp lệ. Vui lòng thử lại sau.', 502);
    }
    throw error;
  }
}

async function readErrorBody(response: Response): Promise<ApiErrorBody> {
  if (!response.headers.get('content-type')?.includes('application/json')) {
    return {};
  }
  try {
    return (await response.json()) as ApiErrorBody;
  } catch (error) {
    if (error instanceof SyntaxError) return {};
    throw error;
  }
}
