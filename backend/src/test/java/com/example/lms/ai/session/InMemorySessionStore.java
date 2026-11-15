package com.example.lms.ai.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.lms.ai.SessionErrorCode;
import com.example.lms.ai.SessionStatus;

/** Cài đặt trong bộ nhớ của {@link SessionStore} với đúng ngữ nghĩa fencing, dùng để test máy trạng thái. */
class InMemorySessionStore implements SessionStore {

    static class Row {
        long id;
        SessionStatus status = SessionStatus.PENDING;
        int attemptCount;
        int autoRetryCount;
        int manualRetryCount;
        Instant leaseExpiresAt;
        Instant nextRetryAt;
        SessionErrorCode errorCode;
        String lastErrorMessage;
        List<TaskDraft> tasks = new ArrayList<>();
        int taskWrites;

        SessionSnapshot snapshot() {
            return new SessionSnapshot(id, 7L, 10L, null, null, 2, "{}", null, null, attemptCount, autoRetryCount);
        }
    }

    final List<Row> rows = new ArrayList<>();

    Row add() {
        Row r = new Row();
        r.id = rows.size() + 1;
        rows.add(r);
        return r;
    }

    private Row find(long id) {
        return rows.stream().filter(r -> r.id == id).findFirst().orElseThrow();
    }

    @Override
    public Optional<SessionSnapshot> claimNext(Instant now, Instant leaseExpiresAt, String workerId) {
        for (Row r : rows) {
            if (r.status == SessionStatus.PENDING && (r.nextRetryAt == null || !r.nextRetryAt.isAfter(now))) {
                r.status = SessionStatus.PROCESSING;
                r.attemptCount++;
                r.leaseExpiresAt = leaseExpiresAt;
                return Optional.of(r.snapshot());
            }
        }
        return Optional.empty();
    }

    @Override
    public List<SessionSnapshot> findLeaseExpired(Instant now, int limit) {
        return rows.stream().filter(r -> r.status == SessionStatus.PROCESSING && !r.leaseExpiresAt.isAfter(now))
                .limit(limit).map(Row::snapshot).toList();
    }

    private boolean fenced(Row r, int attempt) {
        return r.status == SessionStatus.PROCESSING && r.attemptCount == attempt;
    }

    @Override
    public boolean extendLease(long id, int attempt, Instant leaseExpiresAt) {
        Row r = rows.get((int) id - 1);
        if (!fenced(r, attempt)) {
            return false;
        }
        r.leaseExpiresAt = leaseExpiresAt;
        return true;
    }

    @Override
    public boolean complete(long id, int attempt, List<TaskDraft> tasks, String pipelineVersion, String trace,
            Instant now) {
        Row r = find(id);
        if (!fenced(r, attempt)) {
            return false;
        }
        r.tasks = new ArrayList<>(tasks);
        r.taskWrites++;
        r.status = SessionStatus.SUCCEEDED;
        return true;
    }

    @Override
    public boolean fail(long id, int attempt, SessionErrorCode code, String message, Instant now) {
        Row r = find(id);
        if (!fenced(r, attempt)) {
            return false;
        }
        r.status = SessionStatus.FAILED;
        r.errorCode = code;
        r.lastErrorMessage = message;
        return true;
    }

    @Override
    public boolean requeue(long id, int attempt, SessionErrorCode code, String message, Instant nextRetryAt) {
        Row r = find(id);
        if (!fenced(r, attempt)) {
            return false;
        }
        r.status = SessionStatus.PENDING;
        r.autoRetryCount++;
        r.errorCode = code;
        r.lastErrorMessage = message;
        r.nextRetryAt = nextRetryAt;
        r.leaseExpiresAt = null;
        return true;
    }

    @Override
    public boolean manualRetry(long id, int max) {
        Row r = find(id);
        if (r.status != SessionStatus.FAILED || r.manualRetryCount >= max) {
            return false;
        }
        r.status = SessionStatus.PENDING;
        r.manualRetryCount++;
        r.autoRetryCount = 0;
        r.nextRetryAt = null;
        return true;
    }
}
