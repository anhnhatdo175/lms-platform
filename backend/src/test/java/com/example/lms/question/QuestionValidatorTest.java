package com.example.lms.question;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.lms.contract.question.QuestionType;
import com.example.lms.question.Question.ValidatedOption;
import com.example.lms.question.dto.QuestionOptionRequest;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

class QuestionValidatorTest {

    private static QuestionOptionRequest opt(String content, boolean correct, Integer position) {
        return new QuestionOptionRequest(content, correct, position);
    }

    @Test
    void singleChoice_needsExactlyOneCorrect() {
        assertThrows(BusinessException.class, () -> QuestionValidator.validateOptions(QuestionType.SINGLE_CHOICE,
                List.of(opt("a", true, null), opt("b", true, null))));
        assertThrows(BusinessException.class, () -> QuestionValidator.validateOptions(QuestionType.SINGLE_CHOICE,
                List.of(opt("a", false, null), opt("b", false, null))));
    }

    @Test
    void multipleChoice_needsAtLeastOneCorrect() {
        assertThrows(BusinessException.class, () -> QuestionValidator.validateOptions(QuestionType.MULTIPLE_CHOICE,
                List.of(opt("a", false, null), opt("b", false, null))));
        assertEquals(2, QuestionValidator.validateOptions(QuestionType.MULTIPLE_CHOICE,
                List.of(opt("a", true, null), opt("b", true, null))).size());
    }

    @Test
    void needsAtLeastTwoOptions_andNonBlankContent() {
        assertThrows(BusinessException.class,
                () -> QuestionValidator.validateOptions(QuestionType.SINGLE_CHOICE, List.of(opt("a", true, null))));
        assertThrows(BusinessException.class, () -> QuestionValidator.validateOptions(QuestionType.SINGLE_CHOICE,
                List.of(opt("a", true, null), opt("  ", false, null))));
    }

    @Test
    void errorCodeIsQuestionInvalid() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> QuestionValidator.validateOptions(QuestionType.SINGLE_CHOICE, List.of()));
        assertEquals(ErrorCode.QUESTION_INVALID, e.code());
        assertEquals(422, e.httpStatus());
    }

    @Test
    void duplicatePositionIsRejected() {
        assertThrows(BusinessException.class, () -> QuestionValidator.validateOptions(QuestionType.SINGLE_CHOICE,
                List.of(opt("a", true, 1), opt("b", false, 1))));
    }

    @Test
    void positionsAreRenumberedContiguously_providedFirstThenSubmissionOrder() {
        List<ValidatedOption> result = QuestionValidator.validateOptions(QuestionType.SINGLE_CHOICE,
                List.of(opt("no-pos-1", false, null), opt("second", false, 7), opt("first", true, 3),
                        opt("no-pos-2", false, null)));
        assertEquals(List.of("first", "second", "no-pos-1", "no-pos-2"),
                result.stream().map(ValidatedOption::content).toList());
        assertEquals(List.of(1, 2, 3, 4), result.stream().map(ValidatedOption::position).toList());
    }

    @Test
    void skillCode_blankBecomesNull_andIsTrimmed() {
        assertNull(QuestionValidator.normalizeSkillCode("   "));
        assertNull(QuestionValidator.normalizeSkillCode(null));
        assertEquals("fractions", QuestionValidator.normalizeSkillCode("  fractions "));
    }
}
