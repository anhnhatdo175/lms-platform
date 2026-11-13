package com.example.lms.exam.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class OptionSnapshotCodecTest {

    @Test
    void roundTripKeepsOrderAndContent() {
        List<OptionSnapshot> options = List.of(new OptionSnapshot(30, "C \"quoted\"", false),
                new OptionSnapshot(10, "A\nmultiline", true), new OptionSnapshot(20, "", false));
        assertEquals(options, OptionSnapshotCodec.read(OptionSnapshotCodec.write(options)));
    }

    @Test
    void readsMysqlNormalisedJson_keyOrderDoesNotMatter() {
        // MySQL lưu JSON đã chuẩn hóa: khóa sắp lại, có khoảng trắng
        String stored = "[{\"id\": 7, \"correct\": true, \"content\": \"x\"}, {\"content\": \"y\", \"id\": 8, \"correct\": false}]";
        assertEquals(List.of(new OptionSnapshot(7, "x", true), new OptionSnapshot(8, "y", false)),
                OptionSnapshotCodec.read(stored));
    }

    @Test
    void rejectsBrokenSnapshots() {
        assertThrows(IllegalArgumentException.class, () -> OptionSnapshotCodec.read("[{\"id\":\"7\",\"correct\":true}]"));
        assertThrows(IllegalArgumentException.class, () -> OptionSnapshotCodec.read("[{\"id\":7}]"));
        assertThrows(IllegalArgumentException.class, () -> OptionSnapshotCodec.read("[1]"));
        assertThrows(IllegalArgumentException.class, () -> OptionSnapshotCodec.read("{}"));
    }
}
