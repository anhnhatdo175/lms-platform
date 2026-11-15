package com.example.lms.ai.session;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.example.lms.shared.json.JsonReader;
import com.example.lms.shared.json.JsonWriter;

/** Ghi Map/List/String/Number/Boolean/null thành JSON (cặp với {@link JsonReader}); không phụ thuộc thư viện ngoài. */
public final class JsonSupport {

    private JsonSupport() {
    }

    public static String toJson(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value);
        return out.toString();
    }

    private static void write(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            out.append(JsonWriter.quote(s));
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            out.append(value);
        } else if (value instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("JSON number must be finite");
            }
            out.append(n instanceof java.math.BigDecimal bd ? bd.toPlainString() : String.valueOf(d));
        } else if (value instanceof Enum<?> e) {
            out.append(JsonWriter.quote(e.name()));
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append(JsonWriter.quote(String.valueOf(entry.getKey()))).append(':');
                write(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof Collection<?> items) {
            out.append('[');
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(out, item);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
        }
    }

    public static List<String> stringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return JsonReader.parseArray(json).stream().map(String::valueOf).toList();
    }

    /** Đổi giá trị đã parse (List hoặc null) thành List<String>. */
    public static List<String> stringsOf(Object parsed) {
        return parsed instanceof List<?> items ? items.stream().map(String::valueOf).toList() : List.of();
    }

    public static Map<String, Object> object(String json) {
        return json == null || json.isBlank() ? Map.of() : JsonReader.parseObject(json);
    }
}
