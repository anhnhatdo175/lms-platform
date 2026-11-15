package com.example.lms.ai.session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.AdaptiveGeneratedTask;
import com.example.lms.ai.AdaptiveGeneratedTaskRepository;
import com.example.lms.ai.AdaptiveTaskSession;
import com.example.lms.ai.AdaptiveTaskSessionRepository;
import com.example.lms.ai.SessionErrorCode;
import com.example.lms.ai.TeacherReviewStatus;

/** Cài đặt JPA của {@link SessionStore}; fencing nằm ở các UPDATE có điều kiện trong repository. */
@Component
public class JpaSessionStore implements SessionStore {

    private final AdaptiveTaskSessionRepository sessions;
    private final AdaptiveGeneratedTaskRepository tasks;

    public JpaSessionStore(AdaptiveTaskSessionRepository sessions, AdaptiveGeneratedTaskRepository tasks) {
        this.sessions = sessions;
        this.tasks = tasks;
    }

    @Override
    @Transactional
    public Optional<SessionSnapshot> claimNext(Instant now, Instant leaseExpiresAt, String workerId) {
        for (AdaptiveTaskSession candidate : sessions.findClaimable(now, PageRequest.of(0, 5))) {
            if (sessions.claim(candidate.getId(), candidate.getAttemptCount(), leaseExpiresAt, workerId) == 1) {
                return Optional.of(snapshot(candidate, candidate.getAttemptCount() + 1));
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionSnapshot> findLeaseExpired(Instant now, int limit) {
        return sessions.findLeaseExpired(now, PageRequest.of(0, limit)).stream()
                .map(s -> snapshot(s, s.getAttemptCount())).toList();
    }

    @Override
    @Transactional
    public boolean complete(long sessionId, int attempt, List<TaskDraft> drafts, String pipelineVersion,
            String retrievalTraceJson, Instant now) {
        if (sessions.markSucceeded(sessionId, attempt, now, pipelineVersion, retrievalTraceJson) != 1) {
            return false;
        }
        Instant created = now;
        tasks.saveAll(drafts.stream().map(d -> toEntity(sessionId, d, created)).toList());
        return true;
    }

    @Override
    @Transactional
    public boolean fail(long sessionId, int attempt, SessionErrorCode code, String message, Instant now) {
        return sessions.markFailed(sessionId, attempt, now, code, message) == 1;
    }

    @Override
    @Transactional
    public boolean requeue(long sessionId, int attempt, SessionErrorCode code, String message, Instant nextRetryAt) {
        return sessions.markRequeued(sessionId, attempt, nextRetryAt, code, message) == 1;
    }

    @Override
    @Transactional
    public boolean extendLease(long sessionId, int attempt, Instant leaseExpiresAt) {
        return sessions.extendLease(sessionId, attempt, leaseExpiresAt) == 1;
    }

    @Override
    @Transactional
    public boolean manualRetry(long sessionId, int maxManualRetries) {
        return sessions.markManualRetry(sessionId, maxManualRetries) == 1;
    }

    private static SessionSnapshot snapshot(AdaptiveTaskSession s, int attempt) {
        return new SessionSnapshot(s.getId(), s.getStudentId(), s.getCourseId(), s.getLessonId(), s.getConfigId(),
                s.getTaskCount(), s.getConstraintsJson(), s.getRequestedTopicsJson(), s.getRequestedTaskTypesJson(),
                attempt, s.getAutoRetryCount());
    }

    private static AdaptiveGeneratedTask toEntity(long sessionId, TaskDraft d, Instant now) {
        AdaptiveGeneratedTask t = AdaptiveGeneratedTask.create(sessionId, d.position(), d.origin(),
                d.sourceQuestionId(), now);
        t.setTaskType(d.taskType());
        t.setSkillCode(d.skillCode());
        t.setDifficultyLevel(d.difficulty());
        t.setContentJson(d.contentJson());
        t.setAnswerKeyJson(d.answerKeyJson());
        t.setExplanation(d.explanation());
        t.setPersonalizationReason(d.personalizationReason());
        t.setFitScore(d.fitScore());
        t.setHintStepsJson(d.hintStepsJson());
        t.setEvidenceChunkIdsJson(d.evidenceChunkIdsJson());
        t.setValidationStatus(d.validationStatus());
        t.setTeacherReviewStatus(TeacherReviewStatus.NOT_REQUIRED);
        return t;
    }
}
