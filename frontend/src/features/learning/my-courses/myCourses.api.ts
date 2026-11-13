import { useQuery } from '@tanstack/react-query'

import { api, type PageResponse } from '@/lib/api-client'

export type EnrollmentStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED'

export type MyCourse = {
  id: number
  courseId: number
  courseSlug: string
  courseTitle: string
  status: EnrollmentStatus
  enrolledAt: string
  progressPercent: number | null
  expiresAt: string | null
  source: 'PURCHASE' | 'RENEWAL' | 'ADMIN_GRANTED'
}

export function useMyCourses(page: number) {
  return useQuery({
    queryKey: ['my-courses', page],
    queryFn: () => api.get<PageResponse<MyCourse>>('/my-courses', { query: { page, size: 12 } }),
  })
}

/** Đường dẫn trình học bài của A; đổi ở một chỗ nếu router của A đặt khác. */
export const learningPath = (course: Pick<MyCourse, 'courseId' | 'courseSlug'>) =>
  `/learning/courses/${course.courseId}`
