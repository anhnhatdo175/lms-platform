package com.example.lms.shared.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bộ đọc JSON nhỏ (RFC 8259) cho hai chỗ của phần B: payload webhook SePay và cột JSON snapshot đáp án.
 * Viết riêng để không phụ thuộc Jackson 2 hay 3 trong lúc chuyển lên Spring Boot 4. Kết quả: {@link Map}
 * (giữ thứ tự khóa), {@link List}, {@link String}, {@link Long} (số nguyên vừa long) hoặc {@link BigDecimal},
 * {@link Boolean}, hoặc {@code null}. JSON sai cú pháp ném {@link IllegalArgumentException}.
 */
public final class JsonReader {

    private final String text;
    private int pos;

    private JsonReader(String text) {
        this.text = text;
    }

    public static Object parse(String json) {
        if (json == null) {
            throw new IllegalArgumentException("JSON is null");
        }
        JsonReader reader = new JsonReader(json);
        reader.skipWhitespace();
        Object value = reader.readValue();
        reader.skipWhitespace();
        if (reader.pos != json.length()) {
            throw reader.error("unexpected trailing content");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object value = parse(json);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("JSON is not an object");
        }
        return (Map<String, Object>) value;
    }

    public static List<?> parseArray(String json) {
        Object value = parse(json);
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("JSON is not an array");
        }
        return list;
    }

    private Object readValue() {
        if (pos >= text.length()) {
            throw error("unexpected end");
        }
        char c = text.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield readNumber();
                }
                throw error("unexpected character '" + c + "'");
            }
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("expected a key");
            }
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return map;
            }
            if (c != ',') {
                throw error("expected ',' or '}'");
            }
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return list;
            }
            if (c != ',') {
                throw error("expected ',' or ']'");
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') {
                return out.toString();
            }
            if (c < 0x20) {
                throw error("control character in string");
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char escaped = next();
            switch (escaped) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (pos + 4 > text.length()) {
                        throw error("bad unicode escape");
                    }
                    try {
                        out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException e) {
                        throw error("bad unicode escape");
                    }
                    pos += 4;
                }
                default -> throw error("bad escape \\" + escaped);
            }
        }
    }

    private Object readNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        boolean integral = true;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c >= '0' && c <= '9') {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                integral = false;
                pos++;
            } else {
                break;
            }
        }
        String number = text.substring(start, pos);
        if (!number.matches("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?")) {
            throw error("bad number " + number); // ngữ pháp số của RFC 8259: không có số 0 đứng đầu, "1." hay "+1"
        }
        try {
            BigDecimal value = new BigDecimal(number);
            if (integral) {
                try {
                    return value.longValueExact();
                } catch (ArithmeticException tooLarge) {
                    return value;
                }
            }
            return value;
        } catch (NumberFormatException e) {
            throw error("bad number " + number);
        }
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, pos)) {
            throw error("unexpected token");
        }
        pos += word.length();
        return value;
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return;
            }
            pos++;
        }
    }

    private char peek() {
        if (pos >= text.length()) {
            throw error("unexpected end");
        }
        return text.charAt(pos);
    }

    private char next() {
        char c = peek();
        pos++;
        return c;
    }

    private void expect(char c) {
        if (next() != c) {
            throw error("expected '" + c + "'");
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("Invalid JSON at " + pos + ": " + message);
    }
}
