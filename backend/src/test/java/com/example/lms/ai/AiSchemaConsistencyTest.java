package com.example.lms.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Giữ entity AI khớp migration V20261110_1010 mà không cần database: mỗi {@code @Table}/{@code @Column} phải có
 * trong câu CREATE TABLE tương ứng và mỗi enum Java phải trùng danh sách ENUM của cột. Lệch tên cột hay giá trị enum
 * thì test này fail trước khi Hibernate fail lúc chạy.
 */
class AiSchemaConsistencyTest {

    private static final List<Class<?>> ENTITIES = List.of(AdaptivePracticeConfig.class, AiKnowledgeDocument.class,
            AiDocumentChunk.class, LearnerProfile.class, LearnerSkillState.class, LearningInteraction.class,
            AdaptiveTaskSession.class, AdaptiveGeneratedTask.class, AdaptiveTaskAttempt.class,
            AdaptiveTaskHintEvent.class);

    private static String migration() throws IOException {
        try (InputStream in = AiSchemaConsistencyTest.class.getResourceAsStream(
                "/db/migration/V202611121003__c_ai_tables.sql")) {
            assertTrue(in != null, "migration file is missing from the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String tableBlock(String sql, String table) {
        Matcher m = Pattern.compile("CREATE TABLE " + table + " \\((.*?)\\) ENGINE", Pattern.DOTALL).matcher(sql);
        assertTrue(m.find(), "no CREATE TABLE for " + table);
        return m.group(1);
    }

    @Test
    void everyEntityTableAndColumnExistsInTheMigration() throws IOException {
        String sql = migration();
        for (Class<?> entity : ENTITIES) {
            String table = entity.getAnnotation(Table.class).name();
            String block = tableBlock(sql, table);
            for (Field field : entity.getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                if (column == null) {
                    continue;
                }
                Pattern columnLine = Pattern.compile("^\\s+" + column.name() + "\\s", Pattern.MULTILINE);
                assertTrue(columnLine.matcher(block).find(),
                        entity.getSimpleName() + "." + field.getName() + " -> column " + column.name()
                                + " is not defined in " + table);
            }
        }
    }

    @Test
    void everyMigrationTableHasAnEntity() throws IOException {
        String sql = migration();
        Matcher m = Pattern.compile("CREATE TABLE (\\w+) \\(").matcher(sql);
        int count = 0;
        while (m.find()) {
            String table = m.group(1);
            assertTrue(ENTITIES.stream().anyMatch(e -> e.getAnnotation(Table.class).name().equals(table)),
                    "table without entity: " + table);
            count++;
        }
        assertEquals(ENTITIES.size(), count);
    }

    @Test
    void enumConstantsMatchTheDatabaseEnumLists() throws IOException {
        String sql = migration();
        for (Class<?> entity : ENTITIES) {
            String block = tableBlock(sql, entity.getAnnotation(Table.class).name());
            for (Field field : entity.getDeclaredFields()) {
                if (field.getAnnotation(Enumerated.class) == null) {
                    continue;
                }
                String col = field.getAnnotation(Column.class).name();
                Matcher m = Pattern.compile("^\\s+" + col + "\\s+ENUM\\(([^)]*)\\)", Pattern.MULTILINE).matcher(block);
                assertTrue(m.find(), "no ENUM definition for " + col);
                List<String> sqlValues = Stream.of(m.group(1).split(","))
                        .map(s -> s.trim().replace("'", "")).toList();
                List<String> javaValues = Stream.of(field.getType().getEnumConstants()).map(Object::toString).toList();
                assertEquals(sqlValues, javaValues, entity.getSimpleName() + "." + field.getName());
            }
        }
    }
}
