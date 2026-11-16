SET @course_constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'course_categories'
      AND constraint_name = 'uk_course_categories_slug'
);
SET @course_constraint_sql = IF(
    @course_constraint_exists = 0,
    'ALTER TABLE course_categories ADD CONSTRAINT uk_course_categories_slug UNIQUE (slug)',
    'SELECT 1'
);
PREPARE course_constraint_stmt FROM @course_constraint_sql;
EXECUTE course_constraint_stmt;
DEALLOCATE PREPARE course_constraint_stmt;

SET @course_constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'courses'
      AND constraint_name = 'uk_courses_slug'
);
SET @course_constraint_sql = IF(
    @course_constraint_exists = 0,
    'ALTER TABLE courses ADD CONSTRAINT uk_courses_slug UNIQUE (slug)',
    'SELECT 1'
);
PREPARE course_constraint_stmt FROM @course_constraint_sql;
EXECUTE course_constraint_stmt;
DEALLOCATE PREPARE course_constraint_stmt;

SET @course_constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'courses'
      AND constraint_name = 'fk_courses_category'
);
SET @course_constraint_sql = IF(
    @course_constraint_exists = 0,
    'ALTER TABLE courses ADD CONSTRAINT fk_courses_category FOREIGN KEY (category_id) REFERENCES course_categories (id) ON DELETE RESTRICT',
    'SELECT 1'
);
PREPARE course_constraint_stmt FROM @course_constraint_sql;
EXECUTE course_constraint_stmt;
DEALLOCATE PREPARE course_constraint_stmt;

SET @course_constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'courses'
      AND constraint_name = 'fk_courses_creator'
);
SET @course_constraint_sql = IF(
    @course_constraint_exists = 0,
    'ALTER TABLE courses ADD CONSTRAINT fk_courses_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT',
    'SELECT 1'
);
PREPARE course_constraint_stmt FROM @course_constraint_sql;
EXECUTE course_constraint_stmt;
DEALLOCATE PREPARE course_constraint_stmt;

SET @course_constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'courses'
      AND constraint_name = 'fk_courses_thumbnail'
);
SET @course_constraint_sql = IF(
    @course_constraint_exists = 0,
    'ALTER TABLE courses ADD CONSTRAINT fk_courses_thumbnail FOREIGN KEY (thumbnail_file_id) REFERENCES file_resources (id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE course_constraint_stmt FROM @course_constraint_sql;
EXECUTE course_constraint_stmt;
DEALLOCATE PREPARE course_constraint_stmt;

SET @course_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'courses'
      AND index_name = 'idx_courses_status_created'
);
SET @course_index_sql = IF(
    @course_index_exists = 0,
    'CREATE INDEX idx_courses_status_created ON courses (status, created_at)',
    'SELECT 1'
);
PREPARE course_index_stmt FROM @course_index_sql;
EXECUTE course_index_stmt;
DEALLOCATE PREPARE course_index_stmt;

SET @course_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'courses'
      AND index_name = 'idx_courses_category_status'
);
SET @course_index_sql = IF(
    @course_index_exists = 0,
    'CREATE INDEX idx_courses_category_status ON courses (category_id, status)',
    'SELECT 1'
);
PREPARE course_index_stmt FROM @course_index_sql;
EXECUTE course_index_stmt;
DEALLOCATE PREPARE course_index_stmt;
