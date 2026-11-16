import { ApiRequestError } from './authApi';

export type CourseStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'REJECTED'
  | 'PUBLISHED'
  | 'HIDDEN'
  | 'ARCHIVED';

export type LessonType = 'VIDEO' | 'TEXT' | 'DOCUMENT' | 'EXAM';
export type CompletionMethod = 'MANUAL' | 'VIEW_CONTENT' | 'VIDEO_PERCENT' | 'PASS_EXAM' | 'SUBMIT_EXAM';
export type CourseCompletionRule =
  | 'ALL_MANDATORY_LESSONS'
  | 'ALL_MANDATORY_LESSONS_AND_PASS_REQUIRED_EXAMS'
  | 'MANUAL_BY_TEACHER';

export interface CourseCategory {
  id: number;
  name: string;
}

export interface AdminCourseCategory extends CourseCategory {
  slug: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ManagedCourse {
  id: number;
  slug: string;
  title: string;
  shortDescription: string | null;
  description: string | null;
  category: { id: number; name: string } | null;
  thumbnailUrl: string | null;
  status: CourseStatus;
  createdAt: string;
  updatedAt: string;
  originalPrice: number;
  salePrice: number;
  subjectCode: string | null;
  gradeLevel: string | null;
  accessDurationDays: number | null;
  completionRule: CourseCompletionRule;
  publishedAt: string | null;
  lastReviewNote: string | null;
}

export interface SectionSummary {
  id: number;
  courseId: number;
  title: string;
  description: string | null;
  position: number;
  createdAt: string;
  updatedAt: string;
  published: boolean;
  releaseAfterDays: number;
}

export interface LessonDetail {
  id: number;
  sectionId: number;
  title: string;
  description: string | null;
  content: string | null;
  lessonType: LessonType;
  position: number;
  durationSeconds: number | null;
  videoResourceId: number | null;
  preview: boolean;
  published: boolean;
  mandatory: boolean;
  completionMethod: CompletionMethod;
  completionThreshold: number | null;
  prerequisite: { lessonId: number | null; examId: number | null; conditionType: string } | null;
  examId: number | null;
  resources: Array<{
    id: number;
    resourceId: number;
    title: string;
    resourceType: string;
    downloadable: boolean;
    url: string | null;
    position: number;
  }>;
  createdAt: string;
  updatedAt: string;
}

export interface CourseCatalogSummary {
  id: number;
  slug: string;
  title: string;
  shortDescription: string | null;
  thumbnailUrl: string | null;
  status: CourseStatus;
  createdAt: string;
  originalPrice: number;
  salePrice: number;
  category: CourseCategory;
  subjectCode: string | null;
  gradeLevel: string | null;
  publishedAt: string | null;
}

export interface CourseCatalogDetail extends CourseCatalogSummary {
  description: string | null;
  teachers: Array<{ id: number; displayName: string }>;
  sections: Array<{
    id: number;
    title: string;
    description: string | null;
    position: number;
    releaseAfterDays: number;
    lessons: Array<{
      id: number;
      title: string;
      lessonType: LessonType;
      position: number;
      durationSeconds: number | null;
      preview: boolean;
      mandatory: boolean;
      published: boolean;
    }>;
  }>;
  accessDurationDays: number | null;
  completionRule: CourseCompletionRule;
}

export interface MyCourseSummary {
  id: number;
  courseId: number;
  courseSlug: string;
  courseTitle: string;
  status: 'ACTIVE' | 'EXPIRED' | 'REVOKED';
  enrolledAt: string;
  progressPercent: number | null;
  expiresAt: string | null;
  source: 'PURCHASE' | 'RENEWAL' | 'ADMIN_GRANTED';
}

export interface CourseLearningProgress {
  courseId: number;
  totalLessons: number;
  completedLessons: number;
  progressPercent: number;
  lastAccessedAt: string | null;
  completedAt: string | null;
}

export interface CourseOutlineLesson {
  id: number;
  title: string;
  lessonType: LessonType;
  position: number;
  durationSeconds: number | null;
  mandatory: boolean;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  completedAt: string | null;
  access: {
    state: 'AVAILABLE' | 'LOCKED_BY_PREREQUISITE' | 'LOCKED_UNTIL_RELEASE';
    releaseAt: string | null;
    prerequisite: { lessonId: number | null; examId: number | null; conditionType: string } | null;
  };
}

export interface CourseOutlineSection {
  id: number;
  title: string;
  position: number;
  releaseAt: string | null;
  lessons: CourseOutlineLesson[];
}

export interface MyCourseDetail {
  enrollment: MyCourseSummary;
  progress: CourseLearningProgress;
  sections: CourseOutlineSection[];
}

export interface LessonLearningView {
  id: number;
  courseId: number;
  sectionId: number;
  title: string;
  description: string | null;
  content: string | null;
  lessonType: LessonType;
  position: number;
  videoUrl: string | null;
  videoUrlExpiresAt: string | null;
  durationSeconds: number | null;
  mandatory: boolean;
  completionMethod: CompletionMethod;
  completionThreshold: number | null;
  examId: number | null;
  resources: Array<{
    id: number;
    resourceId: number;
    title: string;
    resourceType: string;
    downloadable: boolean;
    url: string | null;
    position: number;
  }>;
  progress: {
    status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
    progressSeconds: number;
    progressPercent: number;
    completedAt: string | null;
  };
}

interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface PageResponse<T> {
  data: T[];
  meta: PageMeta;
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

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/+$/, '');

export async function listTeacherCourses(accessToken: string): Promise<PageResponse<ManagedCourse>> {
  return request<PageResponse<ManagedCourse>>('/teacher/courses?page=0&size=100&sort=updatedAt,desc', accessToken);
}

export async function listCourseCategories(accessToken: string): Promise<CourseCategory[]> {
  return requestData<CourseCategory[]>('/categories', accessToken);
}

export async function listCourseCatalog(
  accessToken: string,
  filters: {
    q?: string;
    categoryId?: number;
    minPrice?: number;
    maxPrice?: number;
    page?: number;
    size?: number;
    sort?: 'newest' | 'price_asc' | 'price_desc';
  } = {},
): Promise<PageResponse<CourseCatalogSummary>> {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== '') query.set(key, String(value));
  });
  return request<PageResponse<CourseCatalogSummary>>(`/courses?${query}`, accessToken);
}

export async function getCatalogCourse(
  accessToken: string,
  slug: string,
): Promise<CourseCatalogDetail> {
  return requestData<CourseCatalogDetail>(`/courses/${encodeURIComponent(slug)}`, accessToken);
}

export async function listMyCourses(accessToken: string, page = 0): Promise<PageResponse<MyCourseSummary>> {
  return request<PageResponse<MyCourseSummary>>(`/my-courses?page=${page}&size=12`, accessToken);
}

export async function getMyCourse(accessToken: string, courseId: number): Promise<MyCourseDetail> {
  return requestData<MyCourseDetail>(`/my-courses/${courseId}`, accessToken);
}

export async function getLearningLesson(accessToken: string, lessonId: number): Promise<LessonLearningView> {
  return requestData<LessonLearningView>(`/learning/lessons/${lessonId}`, accessToken);
}

export async function sendLessonHeartbeat(
  accessToken: string,
  lessonId: number,
  positionSeconds: number,
  clientEventId: string,
): Promise<{
  lessonId: number;
  progressSeconds: number;
  progressPercent: number;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  completed: boolean;
}> {
  return requestData(`/learning/lessons/${lessonId}/heartbeat`, accessToken, {
    method: 'POST',
    body: JSON.stringify({ positionSeconds, clientEventId }),
  });
}

export async function completeLearningLesson(
  accessToken: string,
  lessonId: number,
  clientEventId: string,
): Promise<{ lessonId: number; status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'; completedAt: string | null }> {
  return requestData(`/learning/lessons/${lessonId}/complete`, accessToken, {
    method: 'POST',
    body: JSON.stringify({ clientEventId }),
  });
}

export async function listAdminCourseCategories(accessToken: string): Promise<PageResponse<AdminCourseCategory>> {
  return request<PageResponse<AdminCourseCategory>>('/admin/categories?page=0&size=100', accessToken);
}

export async function createCourseCategory(
  accessToken: string,
  input: { name: string; slug: string; description: string | null },
): Promise<AdminCourseCategory> {
  return requestData<AdminCourseCategory>('/admin/categories', accessToken, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export async function updateCourseCategory(
  accessToken: string,
  categoryId: number,
  input: { name?: string; description?: string | null; active?: boolean },
): Promise<AdminCourseCategory> {
  return requestData<AdminCourseCategory>(`/admin/categories/${categoryId}`, accessToken, {
    method: 'PATCH',
    body: JSON.stringify(input),
  });
}

export async function createCourse(
  accessToken: string,
  input: {
    title: string;
    slug: string;
    categoryId: number;
    shortDescription: string | null;
    description: string | null;
    originalPrice: number;
    salePrice: number;
    subjectCode: string | null;
    gradeLevel: string | null;
    completionRule: CourseCompletionRule;
  },
): Promise<ManagedCourse> {
  return requestData<ManagedCourse>('/teacher/courses', accessToken, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export async function getTeacherCourse(accessToken: string, courseId: number): Promise<ManagedCourse> {
  return requestData<ManagedCourse>(`/teacher/courses/${courseId}`, accessToken);
}

export async function updateTeacherCourse(
  accessToken: string,
  courseId: number,
  changes: { thumbnailResourceId?: number },
): Promise<ManagedCourse> {
  return requestData<ManagedCourse>(`/teacher/courses/${courseId}`, accessToken, {
    method: 'PATCH',
    body: JSON.stringify(changes),
  });
}

export interface CourseReviewRequestSummary {
  id: number;
  courseId: number;
  courseTitle: string;
  teacherId: number;
  teacherName: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN';
  submittedAt: string;
}

export async function submitCourseReview(
  accessToken: string,
  courseId: number,
  note?: string,
): Promise<CourseReviewRequestSummary> {
  return requestData<CourseReviewRequestSummary>(`/teacher/courses/${courseId}/submit-review`, accessToken, {
    method: 'POST',
    body: JSON.stringify(note ? { note } : {}),
  });
}

export async function withdrawCourseReview(accessToken: string, courseId: number): Promise<void> {
  await requestData<unknown>(`/teacher/courses/${courseId}/withdraw-review`, accessToken, { method: 'POST' });
}

export async function reopenCourse(accessToken: string, courseId: number): Promise<void> {
  await requestData<unknown>(`/teacher/courses/${courseId}/reopen`, accessToken, { method: 'POST' });
}

export async function listCourseReviewRequests(
  accessToken: string,
): Promise<{ data: CourseReviewRequestSummary[]; meta: PageMeta }> {
  return request<{ data: CourseReviewRequestSummary[]; meta: PageMeta }>(
    '/admin/course-review-requests?status=PENDING&page=0&size=100&sort=submittedAt,asc',
    accessToken,
  );
}

export async function decideCourseReview(
  accessToken: string,
  courseId: number,
  decision: 'approve' | 'reject',
  reason?: string,
): Promise<void> {
  await requestData<unknown>(`/admin/courses/${courseId}/${decision}`, accessToken, {
    method: 'POST',
    body: JSON.stringify(decision === 'reject' ? { reason } : {}),
  });
}

export async function createSection(
  accessToken: string,
  courseId: number,
  input: {
    title: string;
    description: string | null;
    position?: number;
    published: boolean;
    releaseAfterDays: number;
  },
): Promise<SectionSummary> {
  return requestData<SectionSummary>(`/teacher/courses/${courseId}/sections`, accessToken, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export async function updateSection(
  accessToken: string,
  sectionId: number,
  input: { title?: string; description?: string | null; published?: boolean; releaseAfterDays?: number },
): Promise<SectionSummary> {
  return requestData<SectionSummary>(`/teacher/sections/${sectionId}`, accessToken, {
    method: 'PATCH',
    body: JSON.stringify(input),
  });
}

export async function deleteSection(accessToken: string, sectionId: number): Promise<void> {
  await request<void>(`/teacher/sections/${sectionId}`, accessToken, { method: 'DELETE' });
}

export async function createLesson(
  accessToken: string,
  sectionId: number,
  input: {
    title: string;
    lessonType: LessonType;
    description: string | null;
    content: string | null;
    videoResourceId: number | null;
    durationSeconds: number | null;
    preview: boolean;
    published: boolean;
    mandatory: boolean;
    completionMethod: CompletionMethod;
    completionThreshold: number | null;
  },
): Promise<LessonDetail> {
  return requestData<LessonDetail>(`/teacher/sections/${sectionId}/lessons`, accessToken, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export async function updateLesson(
  accessToken: string,
  lessonId: number,
  input: {
    title?: string;
    description?: string | null;
    content?: string | null;
    videoResourceId?: number | null;
    durationSeconds?: number | null;
    preview?: boolean;
    published?: boolean;
    mandatory?: boolean;
    completionMethod?: CompletionMethod;
    completionThreshold?: number | null;
  },
): Promise<LessonDetail> {
  return requestData<LessonDetail>(`/teacher/lessons/${lessonId}`, accessToken, {
    method: 'PATCH',
    body: JSON.stringify(input),
  });
}

export async function deleteLesson(accessToken: string, lessonId: number): Promise<void> {
  await request<void>(`/teacher/lessons/${lessonId}`, accessToken, { method: 'DELETE' });
}

async function requestData<T>(path: string, accessToken: string, init: RequestInit = {}): Promise<T> {
  const envelope = await request<ApiEnvelope<T>>(path, accessToken, init);
  return envelope.data;
}

async function request<T>(path: string, accessToken: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    const headers = new Headers(init.headers);
    headers.set('Accept', 'application/json');
    headers.set('Authorization', `Bearer ${accessToken}`);
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
