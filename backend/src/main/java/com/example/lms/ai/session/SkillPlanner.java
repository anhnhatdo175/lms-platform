package com.example.lms.ai.session;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.example.lms.contract.question.Difficulty;

/**
 * Xếp hạng kỹ năng ứng viên cho một phiên theo hồ sơ (thuần, không phụ thuộc Spring/DB).
 * <p>
 * Với mỗi kỹ năng: M = mastery hiệu lực (cold start → 0.5), độ khó sinh = M + δ làm tròn rồi kẹp trong khoảng của
 * Teacher ({@link ZpdPolicy#target}), và điểm xếp hạng = Score_ZPD(Δ = độ khó − M). Kỹ năng có điểm cao hơn (vùng
 * phát triển gần thật sự đạt được trong giới hạn Teacher) đứng trước; bằng điểm thì kỹ năng yếu hơn trước (nhu cầu
 * học lớn hơn), rồi ít bằng chứng hơn, rồi theo mã kỹ năng để kết quả tất định.
 */
public final class SkillPlanner {

    private SkillPlanner() {
    }

    public record SkillPlan(String skill, double mastery, int evidenceCount, boolean coldStart, Difficulty difficulty,
            double delta, double zpdScore) {
    }

    public static List<SkillPlan> rank(List<String> skills, Map<String, LearnerContextProvider.SkillMastery> mastery,
            Difficulty min, Difficulty max) {
        List<SkillPlan> plans = new ArrayList<>();
        for (String skill : skills) {
            LearnerContextProvider.SkillMastery m = mastery.get(skill);
            double raw = m == null ? ZpdPolicy.COLD_START_MASTERY : m.mastery();
            int evidence = m == null ? 0 : m.evidenceCount();
            double effective = ZpdPolicy.effectiveMastery(raw, evidence);
            Difficulty d = ZpdPolicy.target(raw, evidence, min, max);
            double delta = ZpdPolicy.numeric(d) - effective;
            plans.add(new SkillPlan(skill, effective, evidence, ZpdPolicy.isColdStart(evidence), d, delta,
                    ZpdPolicy.score(delta)));
        }
        plans.sort(Comparator.comparingDouble(SkillPlan::zpdScore).reversed()
                .thenComparingDouble(SkillPlan::mastery)
                .thenComparingInt(SkillPlan::evidenceCount)
                .thenComparing(SkillPlan::skill));
        return plans;
    }
}
