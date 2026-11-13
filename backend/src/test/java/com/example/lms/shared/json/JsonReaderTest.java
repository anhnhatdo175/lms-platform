package com.example.lms.shared.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonReaderTest {

    @Test
    void parsesNestedValues_keepingKeyOrder() {
        Map<String, Object> json = JsonReader.parseObject(
                " {\"b\": [1, -2.5, true, null], \"a\": {\"x\": \"y\"}, \"big\": 12345678901234567890} ");
        assertEquals(List.of("b", "a", "big"), List.copyOf(json.keySet()));
        List<?> b = (List<?>) json.get("b");
        assertEquals(1L, b.get(0));
        assertEquals(new BigDecimal("-2.5"), b.get(1));
        assertEquals(Boolean.TRUE, b.get(2));
        assertNull(b.get(3));
        assertEquals(Map.of("x", "y"), json.get("a"));
        assertEquals(new BigDecimal("12345678901234567890"), json.get("big"));
    }

    @Test
    void decodesEscapes_includingSurrogatePairs() {
        Object value = JsonReader.parse("\"Thanh to\\u00e1n \\\"LMS\\\"\\n\\ud83d\\ude00\\/\"");
        assertEquals("Thanh toán \"LMS\"\n😀/", value);
        assertEquals("Thanh toán", JsonReader.parse("\"Thanh toán\""));
    }

    @Test
    void rejectsMalformedJson() {
        for (String bad : new String[] { "", "{", "{\"a\":}", "[1,]", "{\"a\":1} x", "tru", "01", "\"\\x\"",
                "{'a':1}", "\"unterminated", "1.", "-" }) {
            assertThrows(IllegalArgumentException.class, () -> JsonReader.parse(bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> JsonReader.parse(null));
    }

    @Test
    void typedEntryPointsCheckTheRootType() {
        assertThrows(IllegalArgumentException.class, () -> JsonReader.parseObject("[]"));
        assertThrows(IllegalArgumentException.class, () -> JsonReader.parseArray("{}"));
        assertTrue(JsonReader.parseArray("[]").isEmpty());
    }

    @Test
    void writerQuoteRoundTrips() {
        String nasty = "a\"b\\c\nd\te\u0001 tiếng Việt 😀";
        assertEquals(nasty, JsonReader.parse(JsonWriter.quote(nasty)));
    }
}
