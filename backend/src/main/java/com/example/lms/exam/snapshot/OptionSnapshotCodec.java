package com.example.lms.exam.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.lms.shared.json.JsonReader;
import com.example.lms.shared.json.JsonWriter;

/**
 * Đọc/ghi {@code attempt_questions.option_snapshot_json}: mảng {@code [{"id", "content", "correct"}]} theo đúng
 * thứ tự hiển thị của Attempt. MySQL chuẩn hóa JSON khi lưu (đổi khoảng trắng, thứ tự khóa), nên khi đọc không
 * dựa vào thứ tự khóa trong object, chỉ dựa vào thứ tự phần tử của mảng.
 */
public final class OptionSnapshotCodec {

    private OptionSnapshotCodec() {
    }

    public static String write(List<OptionSnapshot> options) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < options.size(); i++) {
            OptionSnapshot option = options.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(option.id()).append(",\"content\":").append(JsonWriter.quote(option.content()))
                    .append(",\"correct\":").append(option.correct()).append('}');
        }
        return json.append(']').toString();
    }

    public static List<OptionSnapshot> read(String json) {
        List<OptionSnapshot> options = new ArrayList<>();
        for (Object element : JsonReader.parseArray(json)) {
            if (!(element instanceof Map<?, ?> map) || !(map.get("id") instanceof Long id)
                    || !(map.get("correct") instanceof Boolean correct)) {
                throw new IllegalArgumentException("Invalid option snapshot: " + element);
            }
            Object content = map.get("content");
            options.add(new OptionSnapshot(id, content == null ? "" : content.toString(), correct));
        }
        return options;
    }
}
