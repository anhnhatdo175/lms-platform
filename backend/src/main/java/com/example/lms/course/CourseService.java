package com.example.lms.course;

import com.example.lms.auth.AuthException;
import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.PageMeta;
import com.example.lms.common.PageResponse;
import com.example.lms.file.FileStorageService;
import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import tools.jackson.databind.JsonNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class CourseService {

    private static final Set<String> UPDATE_FIELDS = Set.of(
            "title",
            "shortDescription",
            "description",
            "categoryId",
            "originalPrice",
            "salePrice",
            "thumbnailResourceId",
            "subjectCode",
            "gradeLevel",
            "accessDurationDays",
            "completionRule");

    private final CourseRepository courseRepository;
    private final CourseCategoryRepository categoryRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public CourseService(
            CourseRepository courseRepository,
            CourseCategoryRepository categoryRepository,
            CourseTeacherRepository courseTeacherRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public CourseManaged create(UserPrincipal principal, CreateCourseRequest request) {
        CourseCategory category = activeCategory(request.categoryId());
        long originalPrice = request.originalPrice();
        long salePrice = request.salePrice();
        validatePrices(originalPrice, salePrice);
        fileStorageService.validateCourseThumbnail(request.thumbnailResourceId());
        User teacher = userRepository.findById(principal.id())
                .orElseThrow(() -> new AuthException(
                        "USER_NOT_FOUND", "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));
        String title = request.title().trim();
        String slug = request.slug().trim();
        if (title.isEmpty() || slug.isEmpty()) throw invalidCourse();
        if (courseRepository.existsBySlug(slug)) throw courseSlugConflict();

        Course course = courseRepository.saveAndFlush(new Course(
                category,
                teacher,
                title,
                slug,
                trimToNull(request.shortDescription()),
                request.description(),
                originalPrice,
                salePrice,
                request.thumbnailResourceId(),
                trimToNull(request.subjectCode()),
                trimToNull(request.gradeLevel()),
                request.accessDurationDays(),
                request.completionRule()));
        courseTeacherRepository.saveAndFlush(new CourseTeacher(course, teacher));
        return summarize(course);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseManaged> listTeacherCourses(
            UserPrincipal principal,
            CourseStatus status,
            String query,
            int page,
            int size,
            String sortExpression) {
        Page<Course> courses = courseRepository.searchTeacherCourses(
                principal.id(),
                status,
                blankToNull(query),
                PageRequest.of(page, size, parseSort(sortExpression)));
        return new PageResponse<>(
                courses.map(this::summarize).getContent(),
                new PageMeta(
                        courses.getNumber(),
                        courses.getSize(),
                        courses.getTotalElements(),
                        courses.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public CourseManaged getTeacherCourse(UserPrincipal principal, Long courseId) {
        return summarize(requireTeacherCourse(principal.id(), courseId));
    }

    @Transactional
    public CourseManaged update(
            UserPrincipal principal,
            Long courseId,
            JsonNode changes) {
        Course course = requireTeacherCourse(principal.id(), courseId);
        if (!course.getStatus().allowsContentChanges()) {
            throw stateConflict();
        }
        validateUpdateFields(changes);

        String title = stringValue(changes, "title", course.getTitle(), false, 255);
        String shortDescription = stringValue(
                changes, "shortDescription", course.getShortDescription(), true, 500);
        String description = stringValue(changes, "description", course.getDescription(), true, Integer.MAX_VALUE);
        CourseCategory category = course.getCategory();
        if (changes.has("categoryId")) {
            category = activeCategory(longValue(changes.get("categoryId"), "categoryId", false));
        }
        long originalPrice = longValue(changes, "originalPrice", course.getOriginalPrice());
        long salePrice = longValue(changes, "salePrice", course.getSalePrice());
        validatePrices(originalPrice, salePrice);
        Long thumbnailResourceId = nullableLongValue(
                changes, "thumbnailResourceId", course.getThumbnailFileId());
        fileStorageService.validateCourseThumbnail(thumbnailResourceId);
        String subjectCode = stringValue(changes, "subjectCode", course.getSubjectCode(), true, 100);
        String gradeLevel = stringValue(changes, "gradeLevel", course.getGradeLevel(), true, 50);
        Integer accessDurationDays = nullableIntegerValue(
                changes, "accessDurationDays", course.getAccessDurationDays());
        CourseCompletionRule completionRule = enumValue(
                changes,
                "completionRule",
                course.getCompletionRule(),
                CourseCompletionRule.class);

        course.update(
                category,
                title,
                shortDescription,
                description,
                originalPrice,
                salePrice,
                thumbnailResourceId,
                subjectCode,
                gradeLevel,
                accessDurationDays,
                completionRule);
        return summarize(courseRepository.saveAndFlush(course));
    }

    private Course requireTeacherCourse(Long teacherId, Long courseId) {
        return courseRepository.findTeacherCourse(courseId, teacherId)
                .orElseThrow(() -> new AuthException(
                        "COURSE_NOT_FOUND", "Không tìm thấy khóa học được phân công.", HttpStatus.NOT_FOUND));
    }

    private CourseCategory activeCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(CourseCategory::isActive)
                .orElseThrow(() -> new AuthException(
                        "INVALID_CATEGORY", "Danh mục không tồn tại hoặc đã bị tắt.", HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private CourseManaged summarize(Course course) {
        return CourseManaged.from(
                course,
                fileStorageService.courseThumbnailUrl(course.getThumbnailFileId()),
                null);
    }

    private static void validateUpdateFields(JsonNode changes) {
        if (changes == null || !changes.isObject()) throw invalidCourse();
        long supportedFields = UPDATE_FIELDS.stream().filter(changes::has).count();
        if (supportedFields != changes.size()) throw invalidCourse();
    }

    private static String stringValue(
            JsonNode changes,
            String field,
            String current,
            boolean nullable,
            int maxLength) {
        if (!changes.has(field)) return current;
        JsonNode value = changes.get(field);
        if (value.isNull() && nullable) return null;
        if (!value.isTextual()) throw invalidCourse();
        String text = value.textValue().trim();
        if (text.isEmpty() && !nullable || text.length() > maxLength) throw invalidCourse();
        return text.isEmpty() ? null : text;
    }

    private static long longValue(JsonNode changes, String field, long current) {
        return changes.has(field) ? longValue(changes.get(field), field, false) : current;
    }

    private static long longValue(JsonNode value, String field, boolean nullable) {
        if (value == null || value.isNull()) {
            if (nullable) return 0;
            throw invalidCourse();
        }
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
            throw invalidCourse();
        }
        return value.longValue();
    }

    private static Long nullableLongValue(JsonNode changes, String field, Long current) {
        if (!changes.has(field)) return current;
        JsonNode value = changes.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 1) {
            throw invalidCourse();
        }
        return value.longValue();
    }

    private static Integer nullableIntegerValue(JsonNode changes, String field, Integer current) {
        if (!changes.has(field)) return current;
        JsonNode value = changes.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isIntegralNumber()
                || !value.canConvertToInt()
                || value.intValue() < 1) {
            throw invalidCourse();
        }
        return value.intValue();
    }

    private static <T extends Enum<T>> T enumValue(
            JsonNode changes,
            String field,
            T current,
            Class<T> enumType) {
        if (!changes.has(field)) return current;
        JsonNode value = changes.get(field);
        if (!value.isTextual()) throw invalidCourse();
        try {
            return Enum.valueOf(enumType, value.textValue());
        } catch (IllegalArgumentException exception) {
            throw invalidCourse();
        }
    }

    private static void validatePrices(long originalPrice, long salePrice) {
        if (originalPrice < 0 || salePrice < 0 || salePrice > originalPrice) {
            throw new AuthException(
                    "INVALID_COURSE_PRICE",
                    "Giá bán phải là số nguyên không âm và không vượt quá giá gốc.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private static Sort parseSort(String sortExpression) {
        if (sortExpression == null || sortExpression.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));
        }
        String[] parts = sortExpression.split(",", 2);
        String property = switch (parts[0]) {
            case "title" -> "title";
            case "status" -> "status";
            case "updatedAt" -> "updatedAt";
            case "createdAt" -> "createdAt";
            default -> throw new AuthException(
                    "INVALID_SORT", "Trường sắp xếp không được hỗ trợ.", HttpStatus.BAD_REQUEST);
        };
        Sort.Direction direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property).and(Sort.by("id"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static AuthException invalidCourse() {
        return new AuthException(
                "INVALID_COURSE", "Thông tin khóa học không hợp lệ.", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static AuthException courseSlugConflict() {
        return new AuthException(
                "COURSE_SLUG_CONFLICT", "Slug khóa học đã được sử dụng.", HttpStatus.CONFLICT);
    }

    private static AuthException stateConflict() {
        return new AuthException(
                "STATE_CONFLICT", "Trạng thái khóa học hiện không cho phép chỉnh sửa.", HttpStatus.CONFLICT);
    }
}
