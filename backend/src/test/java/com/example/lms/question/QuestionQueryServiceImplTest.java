package com.example.lms.question;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.lms.contract.question.Difficulty;
import com.example.lms.contract.question.QuestionQueryService.QuestionView;
import com.example.lms.contract.question.QuestionType;
import com.example.lms.exam.QuestionRules;

class QuestionQueryServiceImplTest {

    @Test
    void emptyInput_doesNotHitRepository() {
        QuestionRepository repository = mock(QuestionRepository.class);
        QuestionQueryServiceImpl service = new QuestionQueryServiceImpl(repository);
        assertTrue(service.findQuestions(Set.of()).isEmpty());
        verify(repository, never()).findByIdIn(Set.of());
    }

    @Test
    void mapsQuestionWithCorrectFlags_andIsGradableForExamBuilder() {
        Instant now = Instant.parse("2026-11-15T03:00:00Z");
        Question q = new Question(1, "content", QuestionType.SINGLE_CHOICE, Difficulty.HARD, "skill",
                QuestionSource.MANUAL, "why", 10, now);
        q.addOptions(List.of(new Question.ValidatedOption("a", false, 1), new Question.ValidatedOption("b", true, 2)),
                now);
        ReflectionTestUtils.setField(q, "id", 7L);
        long optionId = 70;
        for (QuestionOption o : q.getOptions()) {
            ReflectionTestUtils.setField(o, "id", optionId++);
        }
        QuestionRepository repository = mock(QuestionRepository.class);
        when(repository.findByIdIn(Set.of(7L))).thenReturn(List.of(q));

        List<QuestionView> views = new QuestionQueryServiceImpl(repository).findQuestions(Set.of(7L));

        assertEquals(1, views.size());
        QuestionView v = views.get(0);
        assertEquals(1L, v.courseId());
        assertEquals("skill", v.skillCode());
        assertEquals(Difficulty.HARD, v.difficulty());
        assertEquals(List.of(false, true), v.options().stream().map(o -> o.correct()).toList());
        assertTrue(QuestionRules.isGradable(v));
    }
}
