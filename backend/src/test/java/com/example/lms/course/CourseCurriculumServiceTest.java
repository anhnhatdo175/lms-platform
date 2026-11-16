package com.example.lms.course;

import com.example.lms.auth.AuthException;
import com.example.lms.auth.UserPrincipal;
import com.example.lms.exam.ExamRepository;
import com.example.lms.file.FileStorageService;
import com.example.lms.user.User;
import com.example.lms.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseCurriculumServiceTest {

    @Test
    void reorderRequiresEverySectionAndLessonAndMovesItemsToRequestedPositions() {
        CourseRepository courses = mock(CourseRepository.class);
        CourseSectionRepository sections = mock(CourseSectionRepository.class);
        LessonRepository lessons = mock(LessonRepository.class);
        Course course = courseWithId(10L);
        CourseSection firstSection = section(course, 101L, 1);
        CourseSection secondSection = section(course, 102L, 2);
        Lesson firstLesson = lesson(firstSection, 201L, 1);
        Lesson secondLesson = lesson(secondSection, 202L, 1);
        when(courses.findTeacherCourse(10L, 1L)).thenReturn(Optional.of(course));
        when(sections.findAllByCourse_IdOrderByPositionAscIdAsc(10L))
                .thenReturn(List.of(firstSection, secondSection));
        when(lessons.findAllByCourseIdOrderBySectionAndPosition(10L))
                .thenReturn(List.of(firstLesson, secondLesson));

        CourseCurriculumService service = service(courses, sections, lessons, mock(LessonPrerequisiteRepository.class));
        service.reorderCurriculum(
                principal(),
                10L,
                new CurriculumOrderRequest(List.of(
                        new CurriculumOrderRequest.SectionOrder(102L, List.of(202L)),
                        new CurriculumOrderRequest.SectionOrder(101L, List.of(201L)))));

        assertThat(secondSection.getPosition()).isEqualTo(1);
        assertThat(firstSection.getPosition()).isEqualTo(2);
        assertThat(secondLesson.getPosition()).isEqualTo(1);
        assertThat(firstLesson.getPosition()).isEqualTo(1);
        verify(sections, times(2)).saveAllAndFlush(anyList());
        verify(lessons, times(2)).saveAllAndFlush(anyList());
    }

    @Test
    void reorderRejectsAnIncompleteSectionListBeforeChangingPositions() {
        CourseRepository courses = mock(CourseRepository.class);
        CourseSectionRepository sections = mock(CourseSectionRepository.class);
        LessonRepository lessons = mock(LessonRepository.class);
        Course course = courseWithId(10L);
        CourseSection section = section(course, 101L, 7);
        when(courses.findTeacherCourse(10L, 1L)).thenReturn(Optional.of(course));
        when(sections.findAllByCourse_IdOrderByPositionAscIdAsc(10L)).thenReturn(List.of(section));
        when(lessons.findAllByCourseIdOrderBySectionAndPosition(10L)).thenReturn(List.of());

        CourseCurriculumService service = service(courses, sections, lessons, mock(LessonPrerequisiteRepository.class));
        assertThatThrownBy(() -> service.reorderCurriculum(
                        principal(),
                        10L,
                        new CurriculumOrderRequest(List.of())))
                .isInstanceOf(AuthException.class);

        assertThat(section.getPosition()).isEqualTo(7);
    }

    @Test
    void settingLessonPrerequisiteRejectsDependencyCycles() {
        CourseRepository courses = mock(CourseRepository.class);
        LessonRepository lessons = mock(LessonRepository.class);
        LessonPrerequisiteRepository prerequisites = mock(LessonPrerequisiteRepository.class);
        Course course = courseWithId(10L);
        CourseSection section = section(course, 101L, 1);
        Lesson target = lesson(section, 201L, 1);
        Lesson prerequisite = lesson(section, 202L, 2);
        LessonPrerequisite existing = new LessonPrerequisite(
                prerequisite, target, null, PrerequisiteConditionType.COMPLETE_LESSON);
        when(lessons.findById(201L)).thenReturn(Optional.of(target));
        when(lessons.findById(202L)).thenReturn(Optional.of(prerequisite));
        when(courses.findTeacherCourse(10L, 1L)).thenReturn(Optional.of(course));
        when(prerequisites.findAllByConditionType(PrerequisiteConditionType.COMPLETE_LESSON))
                .thenReturn(List.of(existing));

        CourseCurriculumService service = service(
                courses,
                mock(CourseSectionRepository.class),
                lessons,
                prerequisites);
        assertThatThrownBy(() -> service.setLessonPrerequisite(
                        principal(),
                        201L,
                        new PrerequisiteRequest(PrerequisiteConditionType.COMPLETE_LESSON, 202L, null)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("vòng lặp");
    }

    @Test
    void attachLessonResourceValidatesOwnershipAndReturnsTemporaryDownloadUrl() {
        CourseRepository courses = mock(CourseRepository.class);
        CourseSectionRepository sections = mock(CourseSectionRepository.class);
        LessonRepository lessons = mock(LessonRepository.class);
        LessonResourceRepository resources = mock(LessonResourceRepository.class);
        FileStorageService files = mock(FileStorageService.class);
        Course course = courseWithId(10L);
        CourseSection section = section(course, 101L, 1);
        Lesson lesson = lesson(section, 201L, 1);
        when(courses.findTeacherCourse(10L, 1L)).thenReturn(Optional.of(course));
        when(lessons.findById(201L)).thenReturn(Optional.of(lesson));
        when(resources.existsByLesson_IdAndResourceId(201L, 301L)).thenReturn(false);
        when(resources.findAllByLesson_IdOrderByPositionAscIdAsc(201L)).thenReturn(List.of());
        when(resources.existsByLesson_IdAndPosition(201L, 1)).thenReturn(false);
        when(resources.saveAndFlush(org.mockito.ArgumentMatchers.any(LessonResource.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(files.lessonResourceFile(301L))
                .thenReturn(new FileStorageService.LessonResourceFile(
                        "worksheet.pdf", "https://files.example/temporary", java.time.Instant.now()));

        CourseCurriculumService service = service(courses, sections, lessons, resources,
                mock(LessonPrerequisiteRepository.class), files);
        LessonResourceSummary result = service.attachLessonResource(
                principal(),
                201L,
                new AttachResourceRequest(301L, null, LessonResourceType.PDF, true, null));

        assertThat(result.resourceId()).isEqualTo(301L);
        assertThat(result.title()).isEqualTo("worksheet.pdf");
        assertThat(result.resourceType()).isEqualTo(LessonResourceType.PDF);
        assertThat(result.url()).isEqualTo("https://files.example/temporary");
        verify(files).validateLessonResource(301L, 1L);
    }

    @Test
    void detachLessonResourceOnlyRemovesResourceFromTheOwnedLesson() {
        CourseRepository courses = mock(CourseRepository.class);
        LessonRepository lessons = mock(LessonRepository.class);
        LessonResourceRepository resources = mock(LessonResourceRepository.class);
        Course course = courseWithId(10L);
        CourseSection section = section(course, 101L, 1);
        Lesson lesson = lesson(section, 201L, 1);
        LessonResource resource = new LessonResource(
                lesson, 301L, "worksheet.pdf", LessonResourceType.PDF, true, 1);
        when(courses.findTeacherCourse(10L, 1L)).thenReturn(Optional.of(course));
        when(lessons.findById(201L)).thenReturn(Optional.of(lesson));
        when(resources.findByLesson_IdAndResourceId(201L, 301L)).thenReturn(Optional.of(resource));

        CourseCurriculumService service = service(
                courses,
                mock(CourseSectionRepository.class),
                lessons,
                resources,
                mock(LessonPrerequisiteRepository.class),
                mock(FileStorageService.class));
        service.detachLessonResource(principal(), 201L, 301L);

        verify(resources).delete(resource);
    }

    private static CourseCurriculumService service(
            CourseRepository courses,
            CourseSectionRepository sections,
            LessonRepository lessons,
            LessonPrerequisiteRepository prerequisites) {
        return service(
                courses,
                sections,
                lessons,
                mock(LessonResourceRepository.class),
                prerequisites,
                mock(FileStorageService.class));
    }

    private static CourseCurriculumService service(
            CourseRepository courses,
            CourseSectionRepository sections,
            LessonRepository lessons,
            LessonResourceRepository resources,
            LessonPrerequisiteRepository prerequisites,
            FileStorageService files) {
        return new CourseCurriculumService(
                courses,
                sections,
                lessons,
                resources,
                prerequisites,
                mock(ExamRepository.class),
                files);
    }

    private static Course courseWithId(Long id) {
        Course course = new Course(
                new CourseCategory("General", "general", null),
                new User("Teacher", "teacher@example.com", "hash", UserStatus.ACTIVE),
                "Course",
                "course",
                null,
                null,
                1000,
                500,
                null,
                null,
                null,
                null,
                CourseCompletionRule.ALL_MANDATORY_LESSONS);
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private static CourseSection section(Course course, Long id, int position) {
        CourseSection section = new CourseSection(course, "Section", null, position, false, 0);
        ReflectionTestUtils.setField(section, "id", id);
        return section;
    }

    private static Lesson lesson(CourseSection section, Long id, int position) {
        Lesson lesson = new Lesson(
                section,
                "Lesson",
                null,
                null,
                LessonType.TEXT,
                null,
                null,
                position,
                false,
                false,
                true,
                CompletionMethod.MANUAL,
                null);
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }

    private static UserPrincipal principal() {
        return new UserPrincipal(1L, "Teacher", "teacher@example.com", Set.of());
    }
}
