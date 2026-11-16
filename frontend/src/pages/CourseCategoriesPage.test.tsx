// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  decideCourseReview,
  listCourseReviewRequests,
} from '../services/courseApi';
import { CourseReviewQueue } from './CourseReviewQueue';

vi.mock('../services/courseApi', () => ({
  decideCourseReview: vi.fn(),
  listCourseReviewRequests: vi.fn(),
}));

const request = {
  id: 8,
  courseId: 12,
  courseTitle: 'React cơ bản',
  teacherId: 5,
  teacherName: 'Giảng viên thử nghiệm',
  status: 'PENDING' as const,
  submittedAt: '2026-11-16T00:00:00Z',
};

describe('CourseCategoriesPage review workflow', () => {
  beforeEach(() => {
    vi.mocked(listCourseReviewRequests)
      .mockResolvedValueOnce({
        data: [request],
        meta: { page: 0, size: 100, totalElements: 1, totalPages: 1 },
      })
      .mockResolvedValue({
        data: [],
        meta: { page: 0, size: 100, totalElements: 0, totalPages: 0 },
      });
    vi.mocked(decideCourseReview).mockResolvedValue();
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it('requires a rejection reason and submits the trimmed explanation', async () => {
    render(<CourseReviewQueue accessToken="test-token" />);

    fireEvent.click(await screen.findByRole('button', { name: 'Từ chối' }));
    const reason = await screen.findByRole('textbox', { name: 'Lý do từ chối khóa học' });
    fireEvent.change(reason, { target: { value: '  Cần bổ sung nội dung chi tiết.  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận từ chối' }));

    await waitFor(() => {
      expect(decideCourseReview).toHaveBeenCalledWith(
        'test-token',
        request.courseId,
        'reject',
        'Cần bổ sung nội dung chi tiết.',
      );
    });
    expect((await screen.findByRole('status')).textContent).toContain('Đã từ chối khóa học.');
  });
});
