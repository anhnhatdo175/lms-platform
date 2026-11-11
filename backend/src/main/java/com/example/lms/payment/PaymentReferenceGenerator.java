package com.example.lms.payment;

import java.util.Locale;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sinh và đọc mã thanh toán (nội dung chuyển khoản). Mã = tiền tố + 10 ký tự Crockford base32.
 * Mã phải khó đoán, nên dùng SecureRandom, không dùng số tuần tự hay timestamp. Chỉ có chữ và số vì
 * một số ngân hàng bỏ ký tự đặc biệt trong nội dung chuyển khoản.
 *
 * <p>Tiền tố phải khớp "Cấu trúc mã thanh toán" cấu hình trên SePay, để SePay tự tách mã vào trường {@code code}.
 */
public class PaymentReferenceGenerator {

    /** Crockford base32: bỏ I, L, O, U để người đọc không nhầm với 1, 0, V. */
    static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    static final int RANDOM_LENGTH = 10;

    private final String prefix;
    private final RandomGenerator random;
    private final Pattern contentPattern;

    public PaymentReferenceGenerator(String prefix, RandomGenerator random) {
        if (prefix == null || !prefix.matches("[A-Za-z0-9]{1,20}")) {
            throw new IllegalArgumentException("Payment reference prefix must be 1-20 letters or digits");
        }
        this.prefix = prefix.toUpperCase(Locale.ROOT);
        this.random = random;
        // Không cho chữ/số đứng ngay trước tiền tố, để "FILMS..." không bị đọc nhầm thành "LMS..."
        this.contentPattern = Pattern.compile(
                "(?<![A-Za-z0-9])" + Pattern.quote(this.prefix) + "([A-Za-z0-9]{" + RANDOM_LENGTH + "})",
                Pattern.CASE_INSENSITIVE);
    }

    public String next() {
        StringBuilder reference = new StringBuilder(prefix.length() + RANDOM_LENGTH).append(prefix);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            reference.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return reference.toString();
    }

    /**
     * Đọc mã từ webhook. Ưu tiên trường {@code code} SePay đã tách; nếu {@code null} thì tìm trong nội dung
     * chuyển khoản gốc. Không tìm thấy thì rỗng (dòng 1, NO_REFERENCE); tìm thấy nhưng sai ký tự vẫn trả về
     * để tra cứu thất bại thành UNKNOWN_REFERENCE (Spec 11.6).
     */
    public Optional<String> extract(String sepayCode, String content) {
        if (sepayCode != null && !sepayCode.isBlank()) {
            return Optional.of(sepayCode.trim().toUpperCase(Locale.ROOT));
        }
        if (content == null) {
            return Optional.empty();
        }
        Matcher matcher = contentPattern.matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(prefix + matcher.group(1).toUpperCase(Locale.ROOT));
    }

    public String prefix() {
        return prefix;
    }
}
