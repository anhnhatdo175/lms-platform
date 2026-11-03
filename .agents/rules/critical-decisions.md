# Critical Decisions — High-Risk Mistakes

Counterintuitive design decisions not obvious from code. See Decision Register v0.2 for full context.

## Backend

### Money is `BIGINT UNSIGNED`, not `DECIMAL`
All monetary columns (price, amount, discount_value) store VND as whole-number integers.
Never use `Double`, `Float`, or `DECIMAL` — floating-point rounding errors corrupt financial data.

### IDs are `Long` (int64 ≥ 1), not UUID or String
Every primary key is an integer. Business codes (courseCode, skillCode) are the only Strings.

### `traceId` is required in every `ErrorResponse`
Not optional — all error responses must include `traceId`. (DEC-008)

### Duplicate SePay webhooks → log + return `2xx`, never throw
Log every duplicate webhook to `payment_webhook_logs` then return 2xx.
Returning 4xx/5xx causes the payment provider to retry indefinitely.

### Payment grace: 15 min after `orders.expires_at`
Webhooks arriving within 15 minutes after order expiry are still processed normally — not pushed to REVIEW_REQUIRED.
Reason: webhook network delay can arrive after order expires. (DEC-005)

### Course `reopen` is a dedicated endpoint, not implicit
`REJECTED → DRAFT` only via `POST /teacher/courses/{id}/reopen`.
Editing curriculum endpoints (sections, lessons) must NOT silently reopen. (DEC-006)

### Editing a `PUBLISHED` course takes effect immediately
No re-review needed. Write an AuditLog entry to preserve history.

### Expired ExamAttempt = `GRADED` + `submission_type = AUTO_EXPIRED`
No separate `EXPIRED` status. Backend processes expiry and moves directly to GRADED.
`expiresAt` is the sole source of truth — no grace buffer. (DEC-010)

### Enrollment is created as `ACTIVE` — no `PENDING` state
Only created when payment conditions are fully met. `UNIQUE(order_item_id)` prevents duplicates.

## AI Adaptive

### Student does NOT send `targetDifficulty`
Server derives difficulty from mastery level (ZPD: `M + δ`), clamped to teacher-configured range.
No `targetDifficulty` field in the session creation request. (DEC-001)

### Socratic hints go to a separate table, NOT `learning_interactions`
Use `adaptive_task_hint_events`. Writing hints into `learning_interactions` pollutes matrix R and EMA profiles. (DEC-003)

### Answer key is hidden from student until after submission
Student-facing API responses must never include `answer_key_json` before the attempt is submitted.

### AI failures must not propagate to LMS business flows
LLM timeout, bad schema, vector DB outage — handle gracefully in isolation.
Must not corrupt: payments, enrollments, lesson progress, or exam attempts.

### `LMS_FILE` and `PASTED_TEXT` are mutually exclusive
- `LMS_FILE` → `file_resource_id` NOT NULL, `source_text` IS NULL
- `PASTED_TEXT` → `source_text` NOT NULL, `file_resource_id` IS NULL
- Enforced by DB CHECK constraint. (DEC-002)

## Frontend

### Never hardcode hex colors
Always use CSS variables (`--color-primary`, `--color-accent`, etc.) defined in `frontend/src/styles/tokens.css`.

### Never use emoji as icons
Use the `Icons` component from `src/components/ui/Icons` (SVG — Heroicons/Lucide).
