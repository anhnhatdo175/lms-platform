package com.example.lms.course;

import com.example.lms.auth.AuthException;
import com.example.lms.auth.UserPrincipal;
import com.example.lms.file.FileStorageService;
import tools.jackson.databind.JsonNode;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class CourseCurriculumService {

    private static final Set<String> SECTION_UPDATE_FIELDS =
            Set.of("title", "description", "published", "releaseAfterDays");
    private static final Set<String> LESSON_UPDATE_FIELDS =
            Set.of("title", "description", "content", "videoResourceId", "durationSeconds",
                    "preview", "published", "mandatory", "completionMethod", "completionThreshold");

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final FileStorageService fileStorageService;

    public CourseCurriculumService(
            CourseRepository courseRepository,
            CourseSectionRepository sectionRepository,
            LessonRepository lessonRepository,
            FileStorageService fileStorageService) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public SectionSummary createSection(UserPrincipal principal, Long courseId, CreateSectionRequest request) {
        Course course = requireTeacherCourse(principal.id(), courseId);
        assertEditable(course);
        int position = request.position() == null
                ? sectionRepository.findLastPosition(courseId) + 1
                : request.position();
        if (sectionRepository.existsByCourse_IdAndPosition(courseId, position)) {
            throw positionConflict();
        }
        CourseSection section = new CourseSection(
                course,
                request.title().trim(),
                trimToNull(request.description()),
                position,
                Boolean.TRUE.equals(request.published()),
                request.releaseAfterDays() == null ? 0 : request.releaseAfterDays());
        try {
            return SectionSummary.from(sectionRepository.saveAndFlush(section));
        } catch (DataIntegrityViolationException exception) {
            if (!violatesConstraint(exception, "uk_course_sections_position")) throw exception;
            throw positionConflict();
        }
    }

    @Transactional
    public SectionSummary updateSection(UserPrincipal principal, Long sectionId, JsonNode changes) {
        CourseSection section = requireTeacherSection(principal.id(), sectionId);
        assertEditable(section.getCourse());
        validateFields(changes, SECTION_UPDATE_FIELDS);

        String title = stringValue(changes, "title", section.getTitle(), false);
        boolean updateDescription = changes.has("description");
        String description = updateDescription
                ? trimToNull(nullableString(changes.get("description")))
                : section.getDescription();
        Boolean published = booleanValue(changes, "published");
        Integer releaseAfterDays = integerValue(changes, "releaseAfterDays", 0);
        section.update(title, description, updateDescription, published, releaseAfterDays);
        return SectionSummary.from(sectionRepository.saveAndFlush(section));
    }

    @Transactional
    public void deleteSection(UserPrincipal principal, Long sectionId) {
        CourseSection section = requireTeacherSection(principal.id(), sectionId);
        assertEditable(section.getCourse());
        if (lessonRepository.countBySection_Id(sectionId) > 0) {
            throw unsafeDelete("Không thể xóa chương đang có bài học.");
        }
        sectionRepository.delete(section);
    }

    @Transactional
    public LessonDetail createLesson(UserPrincipal principal, Long sectionId, CreateLessonRequest request) {
        CourseSection section = requireTeacherSection(principal.id(), sectionId);
        assertEditable(section.getCourse());
        int position = request.position() == null
                ? lessonRepository.findLastPosition(sectionId) + 1
                : request.position();
        if (lessonRepository.existsBySection_IdAndPosition(sectionId, position)) {
            throw positionConflict();
        }
        fileStorageService.validateLessonResource(request.videoResourceId(), principal.id());
        Lesson lesson = new Lesson(
                section,
                request.title().trim(),
                trimToNull(request.description()),
                request.content(),
                request.lessonType(),
                request.videoResourceId(),
                request.durationSeconds(),
                position,
                Boolean.TRUE.equals(request.preview()),
                Boolean.TRUE.equals(request.published()),
                request.mandatory() == null || request.mandatory(),
                request.completionMethod() == null ? CompletionMethod.MANUAL : request.completionMethod(),
                request.completionThreshold());
        try {
            return LessonDetail.from(lessonRepository.saveAndFlush(lesson));
        } catch (DataIntegrityViolationException exception) {
            if (!violatesConstraint(exception, "uk_lessons_position")) throw exception;
            throw positionConflict();
        }
    }

    @Transactional
    public LessonDetail updateLesson(UserPrincipal principal, Long lessonId, JsonNode changes) {
        Lesson lesson = requireTeacherLesson(principal.id(), lessonId);
        assertEditable(lesson.getSection().getCourse());
        validateFields(changes, LESSON_UPDATE_FIELDS);

        String title = stringValue(changes, "title", lesson.getTitle(), false);
        boolean updateDescription = changes.has("description");
        String description = updateDescription
                ? trimToNull(nullableString(changes.get("description")))
                : lesson.getDescription();
        boolean updateContent = changes.has("content");
        String content = updateContent ? nullableString(changes.get("content")) : lesson.getContent();
        boolean updateVideoResource = changes.has("videoResourceId");
        Long videoResourceId = updateVideoResource
                ? nullableLong(changes.get("videoResourceId"))
                : lesson.getVideoResourceId();
        if (updateVideoResource) fileStorageService.validateLessonResource(videoResourceId, principal.id());
        boolean updateDuration = changes.has("durationSeconds");
        Integer durationSeconds = updateDuration
                ? nullableInteger(changes.get("durationSeconds"))
                : lesson.getDurationSeconds();
        Boolean preview = booleanValue(changes, "preview");
        Boolean published = booleanValue(changes, "published");
        Boolean mandatory = booleanValue(changes, "mandatory");
        CompletionMethod completionMethod = enumValue(
                changes, "completionMethod", lesson.getCompletionMethod(), CompletionMethod.class);
        boolean updateThreshold = changes.has("completionThreshold");
        BigDecimal threshold = updateThreshold
                ? nullableDecimal(changes.get("completionThreshold"), "completionThreshold")
                : lesson.getCompletionThreshold();
        validateThreshold(threshold);

        lesson.update(
                title,
                updateDescription,
                description,
                updateContent,
                content,
                updateVideoResource,
                videoResourceId,
                updateDuration,
                durationSeconds,
                preview,
                published,
                mandatory,
                completionMethod,
                updateThreshold,
                threshold);
        return LessonDetail.from(lessonRepository.saveAndFlush(lesson));
    }

    @Transactional
    public void deleteLesson(UserPrincipal principal, Long lessonId) {
        Lesson lesson = requireTeacherLesson(principal.id(), lessonId);
        assertEditable(lesson.getSection().getCourse());
        if (lesson.isPublished() || lesson.getSection().getCourse().getStatus() != CourseStatus.DRAFT) {
            throw unsafeDelete("Chỉ xóa được bài học chưa phát hành trong khóa học nháp.");
        }
        lessonRepository.delete(lesson);
    }

    private Course requireTeacherCourse(Long teacherId, Long courseId) {
        return courseRepository.findTeacherCourse(courseId, teacherId)
                .orElseThrow(CourseCurriculumService::courseNotFound);
    }

    private CourseSection requireTeacherSection(Long teacherId, Long sectionId) {
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(CourseCurriculumService::courseNotFound);
        requireTeacherCourse(teacherId, section.getCourse().getId());
        return section;
    }

    private Lesson requireTeacherLesson(Long teacherId, Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(CourseCurriculumService::courseNotFound);
        requireTeacherCourse(teacherId, lesson.getSection().getCourse().getId());
        return lesson;
    }

    private static void assertEditable(Course course) {
        if (!course.getStatus().allowsContentChanges()) {
            throw new AuthException(
                    "STATE_CONFLICT", "Trạng thái khóa học hiện không cho phép chỉnh sửa.",
                    HttpStatus.CONFLICT);
        }
    }

    private static void validateFields(JsonNode changes, Set<String> supportedFields) {
        if (changes == null || !changes.isObject()
                || changes.isEmpty()
                || supportedFields.stream().filter(changes::has).count() != changes.size()) {
            throw invalidCurriculum();
        }
    }

    private static String stringValue(JsonNode changes, String field, String current, boolean nullable) {
        if (!changes.has(field)) return current;
        String value = nullableString(changes.get(field));
        if (value == null && !nullable || value != null && value.isBlank()) throw invalidCurriculum();
        return value == null ? null : value.trim();
    }

    private static String nullableString(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw invalidCurriculum();
        return value.textValue();
    }

    private static Boolean booleanValue(JsonNode changes, String field) {
        if (!changes.has(field)) return null;
        JsonNode value = changes.get(field);
        if (!value.isBoolean()) throw invalidCurriculum();
        return value.booleanValue();
    }

    private static Integer integerValue(JsonNode changes, String field, int minimum) {
        if (!changes.has(field)) return null;
        JsonNode value = changes.get(field);
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < minimum) {
            throw invalidCurriculum();
        }
        return value.intValue();
    }

    private static Integer nullableInteger(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0) {
            throw invalidCurriculum();
        }
        return value.intValue();
    }

    private static Long nullableLong(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 1) {
            throw invalidCurriculum();
        }
        return value.longValue();
    }

    private static BigDecimal nullableDecimal(JsonNode value, String field) {
        if (value == null || value.isNull()) return null;
        if (!value.isNumber()) throw invalidCurriculum();
        return value.decimalValue();
    }

    private static void validateThreshold(BigDecimal threshold) {
        if (threshold != null
                && (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw invalidCurriculum();
        }
    }

    private static <T extends Enum<T>> T enumValue(
            JsonNode changes, String field, T current, Class<T> enumType) {
        if (!changes.has(field)) return current;
        JsonNode value = changes.get(field);
        if (!value.isTextual()) throw invalidCurriculum();
        try {
            return Enum.valueOf(enumType, value.textValue());
        } catch (IllegalArgumentException exception) {
            throw invalidCurriculum();
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean violatesConstraint(Throwable exception, String constraintName) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private static AuthException courseNotFound() {
        return new AuthException(
                "COURSE_NOT_FOUND", "Không tìm thấy khóa học được phân công.", HttpStatus.NOT_FOUND);
    }

    private static AuthException invalidCurriculum() {
        return new AuthException(
                "INVALID_CURRICULUM", "Thông tin chương trình học không hợp lệ.",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static AuthException positionConflict() {
        return new AuthException(
                "CURRICULUM_POSITION_CONFLICT", "Vị trí chương hoặc bài học đã được sử dụng.",
                HttpStatus.CONFLICT);
    }

    private static AuthException unsafeDelete(String message) {
        return new AuthException("CURRICULUM_DELETE_CONFLICT", message, HttpStatus.CONFLICT);
    }
}
