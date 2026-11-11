package com.example.lms.exam;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Option đã chọn. {@code option_id} là {@code question_options.id} gốc; nội dung lấy từ snapshot. Dòng chỉ được
 * thêm hoặc xóa, không bao giờ sửa, nên {@link Persistable} báo "mới" để {@code save()} chèn thẳng thay vì
 * {@code merge()} (khóa ghép được gán sẵn sẽ khiến Spring Data SELECT trước mỗi lần ghi).
 */
@Entity
@Table(name = "attempt_answer_options")
@IdClass(AttemptAnswerOption.Key.class)
public class AttemptAnswerOption implements Persistable<AttemptAnswerOption.Key> {

    @Id
    @Column(name = "attempt_answer_id")
    private Long attemptAnswerId;

    @Id
    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "option_content_snapshot", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String optionContentSnapshot;

    @Column(name = "selected_at", nullable = false, updatable = false)
    private Instant selectedAt;

    @Transient
    private boolean persisted;

    protected AttemptAnswerOption() {
    }

    public AttemptAnswerOption(long attemptAnswerId, long optionId, String optionContentSnapshot, Instant selectedAt) {
        this.attemptAnswerId = attemptAnswerId;
        this.optionId = optionId;
        this.optionContentSnapshot = optionContentSnapshot;
        this.selectedAt = selectedAt;
    }

    @Override
    public Key getId() {
        return new Key(attemptAnswerId, optionId);
    }

    @Override
    public boolean isNew() {
        return !persisted;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        persisted = true;
    }

    public Long getAttemptAnswerId() {
        return attemptAnswerId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public String getOptionContentSnapshot() {
        return optionContentSnapshot;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }

    public static class Key implements Serializable {
        private Long attemptAnswerId;
        private Long optionId;

        public Key() {
        }

        public Key(Long attemptAnswerId, Long optionId) {
            this.attemptAnswerId = attemptAnswerId;
            this.optionId = optionId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(attemptAnswerId, key.attemptAnswerId)
                    && Objects.equals(optionId, key.optionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attemptAnswerId, optionId);
        }
    }
}
