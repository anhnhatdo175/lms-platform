package com.example.lms.question;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.contract.identity.AccessPolicy;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.contract.question.Difficulty;
import com.example.lms.question.dto.CreateQuestionRequest;
import com.example.lms.question.dto.QuestionTeacherView;
import com.example.lms.question.dto.UpdateQuestionRequest;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;
import com.example.lms.shared.pagination.PageRequests;
import com.example.lms.shared.pagination.PageResponse;

/**
 * Ngân hàng câu hỏi của Course. Quyền xét theo Course của Question: {@code canManageCourse} (Spec 7.2, 7.4); Teacher
 * không được phân công nhận 403, Question không tồn tại nhận 404.
 */
@Service
public class QuestionService {

    private final QuestionRepository questions;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public QuestionService(QuestionRepository questions, AccessPolicy accessPolicy, Clock clock) {
        this.questions = questions;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Transactional
    public QuestionTeacherView create(CurrentUser user, long courseId, CreateQuestionRequest request) {
        requireCourseAccess(user, courseId);
        Instant now = clock.instant();
        Question question = new Question(courseId, request.content().strip(), request.questionType(),
                request.difficulty(), QuestionValidator.normalizeSkillCode(request.skillCode()),
                QuestionSource.MANUAL, blankToNull(request.explanation()), user.id(), now);
        question.addOptions(QuestionValidator.validateOptions(request.questionType(), request.options()), now);
        try {
            return QuestionTeacherView.from(questions.saveAndFlush(question));
        } catch (DataIntegrityViolationException e) {
            // courseId không tồn tại (FK) hoặc dữ liệu vi phạm ràng buộc
            throw BusinessException.notFound("Course");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionTeacherView> list(CurrentUser user, long courseId, Difficulty difficulty,
            String skillCode, Boolean active, String q, QuestionSource source, int page, int size) {
        requireCourseAccess(user, courseId);
        Specification<Question> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("courseId"), courseId));
            if (difficulty != null) {
                p.add(cb.equal(root.get("difficulty"), difficulty));
            }
            if (skillCode != null && !skillCode.isBlank()) {
                p.add(cb.equal(root.get("skillCode"), skillCode.strip()));
            }
            if (active != null) {
                p.add(cb.equal(root.get("active"), active));
            }
            if (source != null) {
                p.add(cb.equal(root.get("source"), source));
            }
            if (q != null && !q.isBlank()) {
                p.add(cb.like(cb.lower(root.get("content")), "%" + escapeLike(q.strip().toLowerCase()) + "%", '\\'));
            }
            return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<Question> result = questions.findAll(spec,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(result, QuestionTeacherView::from);
    }

    @Transactional(readOnly = true)
    public QuestionTeacherView get(CurrentUser user, long questionId) {
        return QuestionTeacherView.from(loadManaged(user, questionId));
    }

    @Transactional
    public QuestionTeacherView update(CurrentUser user, long questionId, UpdateQuestionRequest request) {
        Question question = loadManaged(user, questionId);
        Instant now = clock.instant();
        var effectiveType = request.getQuestionType() != null ? request.getQuestionType() : question.getQuestionType();
        // Đổi loại câu hỏi mà không gửi Option mới vẫn phải còn hợp lệ với Option hiện có
        List<Question.ValidatedOption> validated = null;
        if (request.getOptions() != null) {
            validated = QuestionValidator.validateOptions(effectiveType, request.getOptions());
        } else if (request.getQuestionType() != null && request.getQuestionType() != question.getQuestionType()) {
            long correct = question.getOptions().stream().filter(QuestionOption::isCorrect).count();
            if (effectiveType == com.example.lms.contract.question.QuestionType.SINGLE_CHOICE && correct != 1) {
                throw new BusinessException(ErrorCode.QUESTION_INVALID,
                        "SINGLE_CHOICE needs exactly one correct option; send options together with the new type");
            }
        }
        if (request.getContent() != null && request.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.QUESTION_INVALID, "Content must not be blank");
        }
        question.update(request.getContent() == null ? null : request.getContent().strip(),
                request.getQuestionType(), request.getDifficulty(),
                QuestionValidator.normalizeSkillCode(request.getSkillCode()), request.isSkillCodeProvided(),
                blankToNull(request.getExplanation()), request.isExplanationProvided(), request.getActive(), now);
        if (validated != null) {
            question.clearOptions();
            questions.flush(); // xóa Option cũ trước khi chèn lại: UNIQUE(question_id, position)
            question.addOptions(validated, now);
        }
        return QuestionTeacherView.from(questions.saveAndFlush(question));
    }

    @Transactional
    public void deactivate(CurrentUser user, long questionId) {
        Question question = loadManaged(user, questionId);
        if (questions.countAttemptSnapshots(questionId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Question already used in an attempt; it cannot be removed");
        }
        question.deactivate(clock.instant());
        questions.save(question);
    }

    private Question loadManaged(CurrentUser user, long questionId) {
        Question question = questions.findById(questionId).orElseThrow(() -> BusinessException.notFound("Question"));
        requireCourseAccess(user, question.getCourseId());
        return question;
    }

    private void requireCourseAccess(CurrentUser user, long courseId) {
        if (!accessPolicy.canManageCourse(user.id(), courseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You do not manage this course");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
