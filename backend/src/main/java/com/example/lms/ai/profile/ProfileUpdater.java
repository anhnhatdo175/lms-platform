package com.example.lms.ai.profile;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 */
@Service
public class ProfileUpdater {

    private static final int MIN_EVIDENCE_FOR_LABEL = 3;

    private final LearningInteractionRepository interactions;
    private final LearnerSkillStateRepository skills;
    private final LearnerProfileRepository profiles;
    private final Clock clock;

    public ProfileUpdater(LearningInteractionRepository interactions, LearnerSkillStateRepository skills,
            LearnerProfileRepository profiles, Clock clock) {
        this.interactions = interactions;
        this.skills = skills;
        this.profiles = profiles;
        this.clock = clock;
    }

    /** @return số tương tác đã áp dụng */
    @Transactional
    public int applyPending(int limit) {
        List<LearningInteraction> pending = interactions.findByProcessedAtIsNullOrderByOccurredAt(
                PageRequest.of(0, limit));
        pending.forEach(this::apply);
        return pending.size();
    }

    @Transactional
    public void apply(LearningInteraction interaction) {
        if (interaction.getProcessedAt() != null) {
            return;
        }
        Instant now = clock.instant();
        long studentId = interaction.getStudentId();
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
        refreshProfile(studentId, interaction.getOccurredAt(), now);
        interaction.setProcessedAt(now);
        interactions.save(interaction);
    }

    private void refreshProfile(long studentId, Instant occurredAt, Instant now) {
        LearnerProfile profile = profiles.findByStudentId(studentId)
                .orElseGet(() -> LearnerProfile.create(studentId, now));
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
