package com.example.lms.payment.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SepayWebhookPayloadTest {

    /** Payload mẫu theo tài liệu SePay. */
    private static final String SAMPLE = """
            {"id":92704,"gateway":"Vietcombank","transactionDate":"2026-11-10 14:02:37","accountNumber":"0123499999",
             "code":"LMS7K2M9QX4PZ","content":"chuyen tien LMS7K2M9QX4PZ","transferType":"in",
             "transferAmount":2277000,"accumulated":19077000,"subAccount":null,"referenceCode":"MBVCB.3278907687",
             "description":""}
            """;

    @Test
    void parsesSample() {
        SepayWebhookPayload p = SepayWebhookPayload.parse(SAMPLE);
        assertEquals("92704", p.transactionId());
        assertEquals("SEPAY:92704", p.dedupKey());
        assertEquals("LMS7K2M9QX4PZ", p.code());
        assertEquals(2_277_000L, p.transferAmount());
        assertEquals("0123499999", p.accountNumber());
        assertTrue(p.isIncoming());
    }

    @Test
    void outgoingAndNullCode() {
        SepayWebhookPayload p = SepayWebhookPayload
                .parse("{\"id\":\"5\",\"transferType\":\"out\",\"transferAmount\":100,\"code\":null}");
        assertFalse(p.isIncoming());
        assertNull(p.code());
        assertEquals("5", p.transactionId());
    }

    @Test
    void rejectsMissingIdOrBadAmount() {
        assertThrows(IllegalArgumentException.class, () -> SepayWebhookPayload.parse("{\"transferAmount\":1}"));
        assertThrows(IllegalArgumentException.class, () -> SepayWebhookPayload.parse("{\"id\":1.5}"));
        assertThrows(IllegalArgumentException.class,
                () -> SepayWebhookPayload.parse("{\"id\":1,\"transferAmount\":-1}"));
        assertThrows(IllegalArgumentException.class,
                () -> SepayWebhookPayload.parse("{\"id\":1,\"transferAmount\":10.5}"));
        assertThrows(IllegalArgumentException.class, () -> SepayWebhookPayload.parse("not json"));
    }
}
