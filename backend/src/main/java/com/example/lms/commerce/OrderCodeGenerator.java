package com.example.lms.commerce;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.random.RandomGenerator;

/** Mã đơn hiển thị: {@code ORD} + ngày UTC + 8 ký tự Crockford base32, ví dụ {@code ORD261110-7K3Q9XAP}. */
public final class OrderCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneOffset.UTC);

    private final RandomGenerator random;

    public OrderCodeGenerator(RandomGenerator random) {
        this.random = random;
    }

    public String next(Instant now) {
        StringBuilder code = new StringBuilder("ORD").append(DAY.format(now)).append('-');
        for (int i = 0; i < 8; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
