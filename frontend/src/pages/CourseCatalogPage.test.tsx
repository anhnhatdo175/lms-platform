// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  getCatalogCourse,
  listCourseCatalog,
  listCourseCategories,
  type CourseCatalogDetail,
  type CourseCatalogSummary,
} from '../services/courseApi';
import { CourseCatalogPage } from './CourseCatalogPage';

vi.mock('../services/courseApi', () => ({
  getCatalogCourse: vi.fn(),
  listCourseCatalog: vi.fn(),
  listCourseCategories: vi.fn(),
}));

const course: CourseCatalogSummary = {
  id: 12,
  slug: 'react-co-ban',
  title: 'React cơ bản',
  shortDescription: 'Làm quen với React.',
  thumbnailUrl: null,
  status: 'PUBLISHED',
  createdAt: '2026-11-16T00:00:00Z',
  originalPrice: 500000,
  salePrice: 400000,
  category: { id: 3, name: 'Lập trình web' },
  subjectCode: null,
  gradeLevel: null,
  publishedAt: '2026-11-16T00:00:00Z',
};

const detail: CourseCatalogDetail = {
  ...course,
  description: 'Khóa học React nhập môn.',
  teachers: [{ id: 5, displayName: 'Giảng viên thử nghiệm' }],
  sections: [],
  accessDurationDays: null,
  completionRule: 'ALL_MANDATORY_LESSONS',
};

describe('CourseCatalogPage', () => {
  beforeEach(() => {
    vi.mocked(listCourseCategories).mockResolvedValue([{ id: 3, name: 'Lập trình web' }]);
    vi.mocked(listCourseCatalog).mockResolvedValue({
      data: [course],
      meta: { page: 0, size: 12, totalElements: 1, totalPages: 1 },
    });
    vi.mocked(getCatalogCourse).mockResolvedValue(detail);
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it('loads course details without reloading the catalog list', async () => {
    render(<CourseCatalogPage accessToken="test-token" canPurchase={false} />);

    fireEvent.click(await screen.findByRole('button', { name: 'Xem chi tiết' }));
    await screen.findByText('Nội dung học');

    expect(getCatalogCourse).toHaveBeenCalledWith('test-token', course.slug);
    expect(listCourseCatalog).toHaveBeenCalledTimes(1);
  });
});
