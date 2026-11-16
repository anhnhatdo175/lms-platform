package com.example.lms.course;

import com.example.lms.auth.AuthException;
import com.example.lms.common.PageMeta;
import com.example.lms.common.PageResponse;
import com.example.lms.file.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseCatalogService {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final FileStorageService fileStorageService;

    public CourseCatalogService(
            CourseRepository courseRepository,
            CourseSectionRepository sectionRepository,
            LessonRepository lessonRepository,
            CourseTeacherRepository courseTeacherRepository,
            FileStorageService fileStorageService) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseSummary> list(
            String query,
            Long categoryId,
            Long minPrice,
            Long maxPrice,
            int page,
            int size,
            String sort) {
        if (minPrice != null && minPrice < 0 || maxPrice != null && maxPrice < 0
                || minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new AuthException(
                    "INVALID_PRICE_RANGE", "Khoảng giá khóa học không hợp lệ.", HttpStatus.BAD_REQUEST);
        }
        Page<Course> courses = courseRepository.searchPublicCourses(
                categoryId,
                blankToNull(query),
                minPrice,
                maxPrice,
                PageRequest.of(page, size, parseSort(sort)));
        return new PageResponse<>(
                courses.getContent().stream()
                        .map(course -> CourseSummary.from(
                                course, fileStorageService.courseThumbnailUrl(course.getThumbnailFileId())))
                        .toList(),
                new PageMeta(
                        courses.getNumber(),
                        courses.getSize(),
                        courses.getTotalElements(),
                        courses.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public CourseDetail get(String slug) {
        Course course = courseRepository.findBySlugAndStatus(slug, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new AuthException(
                        "COURSE_NOT_FOUND", "Không tìm thấy khóa học.", HttpStatus.NOT_FOUND));
        List<CourseDetail.SectionDetail> sections =
                sectionRepository.findAllByCourse_IdAndPublishedTrueOrderByPositionAscIdAsc(course.getId()).stream()
                        .map(section -> new CourseDetail.SectionDetail(
                                section.getId(),
                                section.getTitle(),
                                section.getDescription(),
                                section.getPosition(),
                                lessonRepository.findAllBySection_IdAndPublishedTrueOrderByPositionAscIdAsc(
                                                section.getId()).stream()
                                        .map(lesson -> new CourseDetail.LessonSummary(
                                                lesson.getId(),
                                                lesson.getTitle(),
                                                lesson.getLessonType(),
                                                lesson.getPosition(),
                                                lesson.getDurationSeconds(),
                                                lesson.isPreview(),
                                                lesson.isMandatory(),
                                                lesson.isPublished()))
                                        .toList(),
                                section.getReleaseAfterDays()))
                        .toList();
        List<CourseDetail.TeacherSummary> teachers =
                courseTeacherRepository.findAllByIdCourseIdAndActiveTrueOrderByPrimaryDescIdTeacherIdAsc(
                                course.getId()).stream()
                        .map(assignment -> new CourseDetail.TeacherSummary(
                                assignment.getTeacher().getId(),
                                assignment.getTeacher().getFullName()))
                        .toList();
        return new CourseDetail(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                CategorySummary.from(course.getCategory()),
                fileStorageService.courseThumbnailUrl(course.getThumbnailFileId()),
                teachers,
                sections,
                course.getCreatedAt(),
                course.getOriginalPrice(),
                course.getSalePrice(),
                course.getSubjectCode(),
                course.getGradeLevel(),
                course.getAccessDurationDays(),
                course.getCompletionRule(),
                course.getPublishedAt());
    }

    private static Sort parseSort(String sort) {
        return switch (sort == null || sort.isBlank() ? "newest" : sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "salePrice").and(Sort.by(Sort.Direction.ASC, "id"));
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "salePrice").and(Sort.by(Sort.Direction.DESC, "id"));
            default -> throw new AuthException(
                    "INVALID_SORT", "Thứ tự sắp xếp không được hỗ trợ.", HttpStatus.BAD_REQUEST);
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
