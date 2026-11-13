package com.example.lms.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PaymentReferenceGeneratorTest {

    private final PaymentReferenceGenerator generator = new PaymentReferenceGenerator("LMS", new Random(42));

    @Test
    void next_isPrefixPlusTenCrockfordCharacters() {
        for (int i = 0; i < 1_000; i++) {
            String reference = generator.next();
            assertEquals(13, reference.length());
            assertTrue(reference.startsWith("LMS"));
            for (char c : reference.substring(3).toCharArray()) {
                assertTrue(PaymentReferenceGenerator.ALPHABET.indexOf(c) >= 0, "unexpected character " + c);
            }
        }
    }

    @Test
    void next_doesNotRepeatInPractice() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertTrue(seen.add(generator.next()));
        }
    }

    @Test
    void extract_prefersSepayCodeField() {
        assertEquals(Optional.of("LMSABCDEFGH12"), generator.extract(" lmsabcdefgh12 ", "LMSZZZZZZZZZZ chuyen tien"));
    }

    @Test
    void extract_fallsBackToTransferContent_caseInsensitive() {
        assertEquals(Optional.of("LMSK7Q2M9XAPT"),
                generator.extract(null, "NGUYEN VAN A chuyen tien lmsk7q2m9xapt mua khoa hoc"));
    }

    @Test
    void extract_readsReferenceGluedToFollowingText() {
        assertEquals(Optional.of("LMSK7Q2M9XAPT"), generator.extract(null, "LMSK7Q2M9XAPTchuyen tien"));
    }

    @Test
    void extract_ignoresPrefixInsideAnotherWord() {
        assertEquals(Optional.empty(), generator.extract(null, "FILMSK7Q2M9XAPT"));
    }

    @Test
    void extract_returnsEmptyWhenNoReference() {
        assertEquals(Optional.empty(), generator.extract(null, "chuyen tien"));
        assertEquals(Optional.empty(), generator.extract("", null));
    }

    @Test
    void constructor_rejectsPrefixWithSymbols() {
        assertThrows(IllegalArgumentException.class, () -> new PaymentReferenceGenerator("LMS-", new Random()));
        assertThrows(IllegalArgumentException.class, () -> new PaymentReferenceGenerator("", new Random()));
    }
}
