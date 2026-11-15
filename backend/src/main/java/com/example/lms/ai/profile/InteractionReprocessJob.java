package com.example.lms.ai.profile;

import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.lms.ai.LearningInteractionRepository;
import com.example.lms.contract.ai.LearningInteractionRecorder;

/**
 * Xử lý bù (BR-AI-11): (1) áp dụng các tương tác còn {@code processed_at IS NULL}; (2) ghi tương tác cho Attempt đã
 * chấm mà recorder bỏ lỡ. Mỗi bước tự bắt lỗi: lỗi ở đây không bao giờ ảnh hưởng luồng chấm bài của B.
 */
@Component
public class InteractionReprocessJob {

    private static final Logger log = LoggerFactory.getLogger(InteractionReprocessJob.class);
    private static final int BATCH = 50;
    private static final Duration LOOKBACK = Duration.ofDays(3);

    private final ProfileUpdater updater;
    private final LearningInteractionRepository interactions;
    private final LearningInteractionRecorder recorder;
    private final Clock clock;
    private final boolean enabled;

    public InteractionReprocessJob(ProfileUpdater updater, LearningInteractionRepository interactions,
            LearningInteractionRecorder recorder, Clock clock,
            @Value("${ai.profile.reprocess-enabled:true}") boolean enabled) {
        this.updater = updater;
        this.interactions = interactions;
        this.recorder = recorder;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${ai.profile.reprocess-delay:PT5M}")
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            updater.applyPending(BATCH);
        } catch (RuntimeException e) {
            log.warn("pending interaction processing failed", e);
        }
        try {
            for (Long attemptId : interactions.findGradedAttemptIdsWithoutInteractions(
                    clock.instant().minus(LOOKBACK), PageRequest.of(0, BATCH))) {
                try {
                    recorder.recordExamInteractions(attemptId);
                } catch (RuntimeException e) {
                    log.warn("backfill of attempt {} failed", attemptId, e);
                }
            }
        } catch (RuntimeException e) {
            log.warn("attempt backfill query failed", e);
        }
    }
}
