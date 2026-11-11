package com.example.lms.commerce;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.lms.payment.PaymentProperties;
import com.example.lms.payment.QrCodeUrlBuilder;

class OrderCodeGeneratorTest {

    @Test
    void orderCodeFormat_isDatedAndCrockford() {
        OrderCodeGenerator generator = new OrderCodeGenerator(new Random(42));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generator.next(Instant.parse("2026-11-10T23:59:59Z"));
            assertTrue(code.matches("ORD261110-[0-9A-HJKMNP-TV-Z]{8}"), code);
            seen.add(code);
        }
        assertTrue(seen.size() == 1000, "1000 codes, no repeats with 40 bits of randomness");
    }

    @Test
    void qrUrlEncodesEveryParameter() {
        PaymentProperties.Sepay sepay = new PaymentProperties.Sepay("k", "0123 456", "MB&B", "https://vietqr.app/img");
        String url = QrCodeUrlBuilder.build(sepay, 2_277_000, "LMS7K2M9QX4PZ");
        assertTrue(url.equals("https://vietqr.app/img?acc=0123+456&bank=MB%26B&amount=2277000&des=LMS7K2M9QX4PZ"), url);
    }
}
