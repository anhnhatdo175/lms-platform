export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';

export interface AuthUser {
  id: number;
  email: string;
  displayName: string;
  avatarResourceId: number | null;
  roles: UserRole[];
}

export interface FileResource {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  status: 'PENDING' | 'READY' | 'FAILED' | 'DELETED';
  downloadUrl: string | null;
  createdAt: string;
}

interface UploadUrlResult {
  uploadId: string;
  resourceId: number;
  uploadUrl: string;
  method: 'PUT';
  headers: Record<string, string>;
  expiresAt: string;
}

export interface ManagedUser extends AuthUser {
  status: UserStatus;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface UserPage {
  data: ManagedUser[];
  meta: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface AuditLogEntry {
  id: number;
  actorUserId: number | null;
  actorEmail: string | null;
  action: string;
  entityType: string;
  entityId: number | null;
  ipAddress: string | null;
  createdAt: string;
  oldValue: Record<string, unknown> | null;
  newValue: Record<string, unknown> | null;
  reason: string | null;
}

export interface AuditLogPage {
  data: AuditLogEntry[];
  meta: UserPage['meta'];
}

export interface AuthSession {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: AuthUser;
}

interface ApiEnvelope<T> {
  data: T;
}

interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
    traceId?: string;
  };
}

export class ApiRequestError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly traceId?: string;

  constructor(message: string, status: number, code?: string, traceId?: string) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = code;
    this.traceId = traceId;
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/+$/, '');
let sessionRefresh: Promise<AuthSession | null> | null = null;

export function login(email: string, password: string): Promise<AuthSession> {
  return requestData<AuthSession>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function register(displayName: string, email: string, password: string): Promise<AuthUser> {
  return requestData<AuthUser>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ displayName, email, password }),
  });
}

export function requestPasswordReset(email: string): Promise<{ accepted: boolean; message: string }> {
  return requestData<{ accepted: boolean; message: string }>('/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export function resetPassword(token: string, newPassword: string): Promise<void> {
  return request<void>('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  });
}

export function changePassword(
  accessToken: string,
  currentPassword: string,
  newPassword: string,
): Promise<void> {
  return request<void>('/me/password', {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

export function restoreSession(): Promise<AuthSession | null> {
  if (!sessionRefresh) {
    sessionRefresh = refreshSession()
      .then((session) => session)
      .catch((error: unknown) => {
        if (error instanceof ApiRequestError && error.status === 401) return null;
        throw error;
      })
      .finally(() => {
        sessionRefresh = null;
      });
  }
  return sessionRefresh;
}

export function refreshSession(): Promise<AuthSession> {
  return requestData<AuthSession>('/auth/refresh', { method: 'POST' });
}

export function logout(accessToken: string): Promise<void> {
  return request<void>('/auth/logout', {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

export function updateProfile(
  accessToken: string,
  changes: { displayName?: string; avatarResourceId?: number | null },
): Promise<AuthUser> {
  return requestData<AuthUser>('/me', {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify(changes),
  });
}

export function createAvatarUploadUrl(
  accessToken: string,
  file: File,
): Promise<UploadUrlResult> {
  return requestData<UploadUrlResult>('/teacher/files/upload-url', {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({
      fileName: file.name,
      contentType: file.type,
      sizeBytes: file.size,
      purpose: 'USER_AVATAR',
    }),
  });
}

export async function uploadAvatarFile(
  upload: UploadUrlResult,
  file: File,
): Promise<void> {
  let response: Response;
  try {
    response = await fetch(upload.uploadUrl, {
      method: upload.method,
      headers: upload.headers,
      body: file,
      credentials: 'omit',
    });
  } catch (error) {
    if (error instanceof TypeError) {
      throw new ApiRequestError('Không thể tải ảnh lên kho lưu trữ. Vui lòng thử lại.', 0);
    }
    throw error;
  }
  if (!response.ok) {
    throw new ApiRequestError('Kho lưu trữ không chấp nhận ảnh tải lên.', response.status);
  }
}

export function completeAvatarUpload(
  accessToken: string,
  uploadId: string,
  file: File,
): Promise<FileResource> {
  return requestData<FileResource>('/teacher/files/complete-upload', {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({
      uploadId,
      fileName: file.name,
      contentType: file.type,
      sizeBytes: file.size,
    }),
  });
}

export function getFileResource(accessToken: string, resourceId: number): Promise<FileResource> {
  return requestData<FileResource>(`/files/${resourceId}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

export function listUsers(
  accessToken: string,
  filters: { query?: string; role?: UserRole | ''; status?: UserStatus | ''; page?: number },
): Promise<UserPage> {
  const search = new URLSearchParams();
  if (filters.query) search.set('q', filters.query);
  if (filters.role) search.set('role', filters.role);
  if (filters.status) search.set('status', filters.status);
  search.set('page', String(filters.page ?? 0));
  search.set('size', '10');
  return request<UserPage>(`/admin/users?${search.toString()}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

export function listAuditLogs(
  accessToken: string,
  filters: {
    action?: string;
    entityType?: string;
    actorUserId?: number;
    from?: string;
    to?: string;
    page?: number;
  },
): Promise<AuditLogPage> {
  const search = new URLSearchParams();
  if (filters.action) search.set('action', filters.action);
  if (filters.entityType) search.set('entityType', filters.entityType);
  if (filters.actorUserId) search.set('actorUserId', String(filters.actorUserId));
  if (filters.from) search.set('from', filters.from);
  if (filters.to) search.set('to', filters.to);
  search.set('page', String(filters.page ?? 0));
  search.set('size', '20');
  return request<AuditLogPage>(`/admin/audit-logs?${search.toString()}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

export async function updateUser(
  accessToken: string,
  userId: number,
  changes: { roles?: UserRole[]; status?: UserStatus; reason?: string },
): Promise<ManagedUser> {
  let updated: ManagedUser | undefined;
  if (changes.roles) {
    updated = await requestData<ManagedUser>(`/admin/users/${userId}/roles`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${accessToken}` },
      body: JSON.stringify({ roles: changes.roles }),
    });
  }
  if (changes.status) {
    updated = await requestData<ManagedUser>(`/admin/users/${userId}/status`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${accessToken}` },
      body: JSON.stringify({ status: changes.status, reason: changes.reason }),
    });
  }
  if (!updated) throw new Error('No user changes were provided.');
  return updated;
}

async function requestData<T>(path: string, init: RequestInit): Promise<T> {
  const envelope = await request<ApiEnvelope<T>>(path, init);
  return envelope.data;
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
      body.error?.traceId,
    );
  }

  if (response.status === 204) return undefined as T;
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
  if (!response.headers.get('content-type')?.includes('application/json')) return {};
  try {
    return (await response.json()) as ApiErrorBody;
  } catch (error) {
    if (error instanceof SyntaxError) return {};
    throw error;
  }
}
