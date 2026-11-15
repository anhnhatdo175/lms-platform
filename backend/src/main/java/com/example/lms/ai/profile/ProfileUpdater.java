package com.example.lms.ai.profile;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.lms.ai.LearnerProfile;
import com.example.lms.ai.LearnerProfileRepository;
import com.example.lms.ai.LearnerSkillState;
import com.example.lms.ai.LearnerSkillStateRepository;
import com.example.lms.ai.LearningInteraction;
import com.example.lms.ai.LearningInteractionRepository;
import com.example.lms.ai.session.JsonSupport;

/**
 * Cập nhật hồ sơ từ {@link LearningInteraction} chưa xử lý. Mỗi tương tác được áp dụng đúng một lần: đổi
 * {@code processed_at} và cập nhật trạng thái kỹ năng nằm trong cùng một transaction (BR-AI-08).
 * Chỉ tương tác có {@code skill_code} mới đổi mastery; mọi tương tác đều làm mới mốc hoạt động của hồ sơ.
 * <p>
 * Đồng thời: mọi cập nhật của một học viên được tuần tự hoá bằng khoá dòng {@code learner_profiles} (FOR UPDATE), và
 * {@code processed_at} được đặt bằng UPDATE có điều kiện, nên một tương tác không bao giờ bị áp dụng hai lần, kể cả khi
 * job xử lý bù chạy trên nhiều bản backend.
 */
@Service
public class ProfileUpdater {

    private static final Logger log = LoggerFactory.getLogger(ProfileUpdater.class);
    private static final int MIN_EVIDENCE_FOR_LABEL = 3;

    private final LearningInteractionRepository interactions;
    private final LearnerSkillStateRepository skills;
    private final LearnerProfileRepository profiles;
    private final Clock clock;
    private final TransactionTemplate tx;

    /**
     * Kết quả áp dụng một tương tác. {@code versionBefore} null khi hồ sơ vừa được tạo; cả hai được đọc dưới khoá
     * nên {@code versionAfter = versionBefore + 1} chính xác.
     */
    public record Applied(boolean applied, Integer versionBefore, Integer versionAfter) {

        static final Applied NONE = new Applied(false, null, null);
    }

    public ProfileUpdater(LearningInteractionRepository interactions, LearnerSkillStateRepository skills,
            LearnerProfileRepository profiles, Clock clock, PlatformTransactionManager transactionManager) {
        this.interactions = interactions;
        this.skills = skills;
        this.profiles = profiles;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /**
     * Áp dụng các tương tác còn treo, MỖI tương tác một transaction: một dòng lỗi chỉ bị bỏ qua (ghi log, thử lại ở
     * lượt sau) chứ không làm cả lô rollback mãi.
     *
     * @return số tương tác đã áp dụng
     */
    public int applyPending(int limit) {
        int applied = 0;
        for (Long id : interactions.findPendingIds(PageRequest.of(0, limit))) {
            try {
                Boolean ok = tx.execute(status -> interactions.findById(id).map(i -> apply(i).applied())
                        .orElse(false));
                if (Boolean.TRUE.equals(ok)) {
                    applied++;
                }
            } catch (RuntimeException e) {
                log.warn("interaction {} could not be applied; will retry", id, e);
            }
        }
        return applied;
    }

    /**
     * Phải chạy trong transaction của caller (cùng transaction ghi tương tác).
     *
     * Caller không được nạp {@code LearnerProfile} của học viên vào transaction trước lời gọi này (bản nạp trước khoá
     * có thể đã cũ).
     *
     * @return {@link Applied#NONE} nếu tương tác đã được áp dụng trước đó
     */
    @Transactional
    public Applied apply(LearningInteraction interaction) {
        if (interaction.getProcessedAt() != null) {
            return Applied.NONE;
        }
        Instant now = clock.instant();
        long studentId = interaction.getStudentId();
        boolean created = profiles.insertIfAbsent(studentId, now) == 1;
        LearnerProfile profile = profiles.lockByStudentId(studentId)
                .orElseThrow(() -> new IllegalStateException("learner profile missing for student " + studentId));
        if (interactions.markProcessed(interaction.getId(), now) != 1) {
            return Applied.NONE;
        }
        Integer before = created ? null : profile.getProfileVersion();
        interaction.setProcessedAt(now);
        String skill = interaction.getSkillCode();
        if (skill != null && !skill.isBlank()) {
            LearnerSkillState state = skills.findByStudentIdAndSkillCode(studentId, skill)
                    .orElseGet(() -> LearnerSkillState.create(studentId, skill, now));
            double outcome = ProfileMath.outcome(interaction.getScore(), interaction.getMaxScore(),
                    interaction.getCorrect(), interaction.getHintsUsed());
            double next = ProfileMath.nextMastery(state.getMasteryScore().doubleValue(), state.getEvidenceCount(),
                    outcome);
            state.setMasteryScore(ProfileMath.scale(next));
            state.setEvidenceCount(state.getEvidenceCount() + 1);
            state.setLastEvidenceAt(interaction.getOccurredAt());
            state.setModelVersion(ProfileMath.MODEL_VERSION);
            state.setUpdatedAt(now);
            skills.save(state);
        }
        refreshProfile(profile, interaction.getOccurredAt(), now);
        return new Applied(true, before, profile.getProfileVersion());
    }

    private void refreshProfile(LearnerProfile profile, Instant occurredAt, Instant now) {
        long studentId = profile.getStudentId();
        List<LearnerSkillState> all = skills.findByStudentId(studentId);
        List<String> strong = new ArrayList<>();
        List<String> weak = new ArrayList<>();
        double sum = 0;
        for (LearnerSkillState s : all) {
            double m = s.getMasteryScore().doubleValue();
            sum += m;
            if (s.getEvidenceCount() >= MIN_EVIDENCE_FOR_LABEL) {
                if (m >= ProfileMath.STRONG_AT) {
                    strong.add(s.getSkillCode());
                } else if (m < ProfileMath.WEAK_BELOW) {
                    weak.add(s.getSkillCode());
                }
            }
        }
        if (!all.isEmpty()) {
            double mean = sum / all.size();
            double var = 0;
            for (LearnerSkillState s : all) {
                var += Math.pow(s.getMasteryScore().doubleValue() - mean, 2);
            }
            profile.setAverageScore(ProfileMath.scale(mean));
            profile.setScoreStddev(ProfileMath.scale(Math.sqrt(var / all.size())));
        }
        profile.setStrengthTopicsJson(JsonSupport.toJson(strong));
        profile.setWeakTopicsJson(JsonSupport.toJson(weak));
        Instant last = profile.getLastInteractionAt();
        profile.setLastInteractionAt(last == null || occurredAt.isAfter(last) ? occurredAt : last);
        profile.setProfileVersion(profile.getProfileVersion() + 1);
        profile.setComputedAt(now);
        profile.setUpdatedAt(now);
        profiles.save(profile);
    }
}
