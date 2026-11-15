package com.example.lms.question;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.lms.contract.identity.AccessPolicy;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.contract.question.Difficulty;
import com.example.lms.contract.question.QuestionType;
import com.example.lms.question.dto.CreateQuestionRequest;
import com.example.lms.question.dto.QuestionOptionRequest;
import com.example.lms.question.dto.QuestionTeacherView;
import com.example.lms.question.dto.UpdateQuestionRequest;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** Negative test cho ownership (Spec 7.4, DEC-009): Teacher không được phân công, Question không tồn tại. */
class QuestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");
    private static final CurrentUser TEACHER = new CurrentUser(10, Set.of("TEACHER"));
    private static final long MANAGED_COURSE = 1;
    private static final long OTHER_COURSE = 2;

    private QuestionRepository repository;
    private QuestionService service;

    @BeforeEach
    void setUp() {
        repository = mock(QuestionRepository.class);
        AccessPolicy policy = (userId, courseId) -> userId == 10 && courseId == MANAGED_COURSE;
        service = new QuestionService(repository, policy, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.saveAndFlush(any(Question.class))).thenAnswer(i -> withIds(i.getArgument(0)));
        when(repository.save(any(Question.class))).thenAnswer(i -> withIds(i.getArgument(0)));
    }

    private static CreateQuestionRequest validCreate() {
        return new CreateQuestionRequest("2 + 2 = ?", QuestionType.SINGLE_CHOICE, Difficulty.EASY, " arithmetic ",
                null, List.of(new QuestionOptionRequest("3", false, null), new QuestionOptionRequest("4", true, null)));
    }

    /** Entity chưa qua DB nên chưa có id; gán giả để DTO (id kiểu long) dựng được. */
    private static Question withIds(Question q) {
        ReflectionTestUtils.setField(q, "id", 100L);
        long next = 1000;
        for (QuestionOption o : q.getOptions()) {
            ReflectionTestUtils.setField(o, "id", next++);
        }
        return q;
    }

    private static Question existing(long courseId) {
        Question q = new Question(courseId, "q", QuestionType.SINGLE_CHOICE, Difficulty.EASY, "s",
                QuestionSource.MANUAL, null, 10, NOW);
        q.addOptions(List.of(new Question.ValidatedOption("a", true, 1), new Question.ValidatedOption("b", false, 2)),
                NOW);
        return q;
    }

    @Test
    void create_forbiddenWhenTeacherDoesNotManageCourse() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(TEACHER, OTHER_COURSE, validCreate()));
        assertEquals(ErrorCode.FORBIDDEN, e.code());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_storesTrimmedSkillCodeAndSourceManual() {
        QuestionTeacherView view = service.create(TEACHER, MANAGED_COURSE, validCreate());
        assertEquals("arithmetic", view.skillCode());
        assertEquals(QuestionSource.MANUAL, view.source());
        assertEquals(1, view.version());
        assertTrue(view.active());
        assertEquals(List.of(1, 2), view.options().stream().map(QuestionTeacherView.OptionView::position).toList());
    }

    @Test
    void list_forbiddenWhenTeacherDoesNotManageCourse() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.list(TEACHER, OTHER_COURSE, null, null, null, null, null, 0, 20));
        assertEquals(ErrorCode.FORBIDDEN, e.code());
    }

    @Test
    void get_notFoundWhenQuestionMissing() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.get(TEACHER, 99)).code());
    }

    @Test
    void get_forbiddenWhenQuestionBelongsToUnmanagedCourse() {
        when(repository.findById(5L)).thenReturn(Optional.of(existing(OTHER_COURSE)));
        assertEquals(ErrorCode.FORBIDDEN,
                assertThrows(BusinessException.class, () -> service.get(TEACHER, 5)).code());
    }

    @Test
    void update_forbiddenForUnmanagedCourse_andDoesNotSave() {
        when(repository.findById(5L)).thenReturn(Optional.of(existing(OTHER_COURSE)));
        UpdateQuestionRequest request = new UpdateQuestionRequest();
        request.setContent("changed");
        assertEquals(ErrorCode.FORBIDDEN,
                assertThrows(BusinessException.class, () -> service.update(TEACHER, 5, request)).code());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void update_absentFieldsAreKept_nullSkillCodeClears_versionIncrements() {
        Question q = existing(MANAGED_COURSE);
        when(repository.findById(5L)).thenReturn(Optional.of(q));
        UpdateQuestionRequest request = new UpdateQuestionRequest();
        request.setSkillCode(null); // gửi tường minh null: xóa kỹ năng
        QuestionTeacherView view = service.update(TEACHER, 5, request);
        assertNull(view.skillCode());
        assertEquals("q", view.content());
        assertEquals(2, view.version());
    }

    @Test
    void update_changingToSingleChoiceWithoutOptions_failsWhenOptionsDoNotFit() {
        Question q = existing(MANAGED_COURSE);
        q.clearOptions();
        q.addOptions(List.of(new Question.ValidatedOption("a", true, 1), new Question.ValidatedOption("b", true, 2)),
                NOW);
        when(repository.findById(5L)).thenReturn(Optional.of(q));
        UpdateQuestionRequest request = new UpdateQuestionRequest();
        // Câu đang là MULTIPLE_CHOICE với hai đáp án đúng; đổi về SINGLE_CHOICE mà không gửi Option mới thì phải bị từ chối
        q.update(null, QuestionType.MULTIPLE_CHOICE, null, null, false, null, false, null, NOW);
        request.setQuestionType(QuestionType.SINGLE_CHOICE);
        assertEquals(ErrorCode.QUESTION_INVALID,
                assertThrows(BusinessException.class, () -> service.update(TEACHER, 5, request)).code());
    }

    @Test
    void deactivate_conflictWhenQuestionAlreadyInAnAttempt() {
        when(repository.findById(5L)).thenReturn(Optional.of(existing(MANAGED_COURSE)));
        when(repository.countAttemptSnapshots(5L)).thenReturn(3L);
        assertEquals(ErrorCode.CONFLICT,
                assertThrows(BusinessException.class, () -> service.deactivate(TEACHER, 5)).code());
        verify(repository, never()).save(any());
    }

    @Test
    void deactivate_setsInactive() {
        Question q = existing(MANAGED_COURSE);
        when(repository.findById(5L)).thenReturn(Optional.of(q));
        when(repository.countAttemptSnapshots(5L)).thenReturn(0L);
        service.deactivate(TEACHER, 5);
        assertFalse(q.isActive());
    }
}
