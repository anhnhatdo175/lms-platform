package com.example.lms.ai.practice;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Chấm rule-based cho đáp án ngắn, cùng quy tắc với prototype (layer 6): {@code exact} so khớp sau khi chuẩn hóa khoảng
 * trắng và chữ hoa/thường; {@code numeric} so sánh số (chấp nhận dấu phẩy thập phân kiểu Việt Nam); loại khác cần
 * giáo viên xem (không tự chấm, không cập nhật hồ sơ).
 */
public final class RuleBasedGrader {

    static final double NUMERIC_TOLERANCE = 1e-9;

    private RuleBasedGrader() {
    }

    /** @param correct {@code null} nghĩa là không tự chấm được */
    public record Grade(Boolean correct, boolean gradable) {

        static Grade of(boolean correct) {
            return new Grade(correct, true);
        }

        static Grade needsReview() {
            return new Grade(null, false);
        }
    }

    public static Grade grade(String responseType, Object expected, String answer) {
        if (expected == null || answer == null) {
            return Grade.needsReview();
        }
        String type = responseType == null ? "exact" : responseType;
        switch (type) {
            case "exact":
                return Grade.of(normalize(String.valueOf(expected)).equals(normalize(answer)));
            case "numeric":
                Double e = parseNumber(String.valueOf(expected));
                Double a = parseNumber(answer);
                if (e == null) {
                    return Grade.needsReview();
                }
                return Grade.of(a != null && Math.abs(a - e) <= NUMERIC_TOLERANCE);
            default:
                return Grade.needsReview();
        }
    }

    static String normalize(String s) {
        String t = Normalizer.normalize(s, Normalizer.Form.NFC).strip().replaceAll("\\s+", " ");
        return t.toLowerCase(Locale.ROOT);
    }

    /** Chấp nhận "0,5", "0.5", "-3", "1e2"; từ chối chuỗi rỗng, NaN, vô cực và dấu phân cách hàng nghìn mơ hồ. */
    static Double parseNumber(String s) {
        String t = s.strip().replace(" ", "");
        if (t.isEmpty()) {
            return null;
        }
        if (t.indexOf(',') >= 0 && t.indexOf('.') >= 0) {
            return null;
        }
        if (t.indexOf(',') != t.lastIndexOf(',')) {
            return null;
        }
        t = t.replace(',', '.');
        if (!t.matches("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?")) {
            return null;
        }
        try {
            double v = Double.parseDouble(t);
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
