-- Dữ liệu demo AI (sinh bởi research/demo/generate_demo_sql.py; không sửa tay).
-- Điền các id đã có trong DB (user/course/lesson thuộc phần A), rồi chạy trên DB đã migrate.
SET @teacher_id = 1;   -- Teacher sở hữu Course
SET @course_id = 1;
SET @lesson_id = 1;    -- Lesson thuộc Course (dùng cho tương tác mẫu)
SET @student_strong = 2;
SET @student_weak = 3;
SET @student_cold = 4; -- không có dòng nào: demo cold start
SET @now = NOW(6);

-- Cấu hình luyện tập do Teacher đặt (Course mặc định).
INSERT INTO adaptive_practice_configs (course_id, lesson_id, enabled, subject_code, grade_level, allowed_skills_json, min_difficulty, max_difficulty, allowed_task_types_json, max_tasks_per_session, socratic_enabled, max_hints_per_task, updated_by, created_at, updated_at)
VALUES (@course_id, NULL, TRUE, 'Toán', '5', '["fractions","decimals","percentages"]', 'EASY', 'HARD', '["SHORT_ANSWER"]', 5, TRUE, 3, @teacher_id, @now, @now)
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), allowed_skills_json = VALUES(allowed_skills_json), updated_at = VALUES(updated_at);

-- Tài liệu nguồn: PENDING, worker ingestion sẽ lập chỉ mục và chuyển READY.
INSERT INTO ai_knowledge_documents (course_id, source_text, title, source_type, language_code, subject_code, grade_level, index_status, index_version, attempt_count, created_by, created_at, updated_at)
SELECT @course_id, 'Phân số a/b biểu thị a phần của một đơn vị được chia thành b phần bằng nhau. Hai phân số bằng nhau khi chúng cùng biểu thị một giá trị, ví dụ 1/2 = 2/4 = 3/6. Muốn cộng hoặc trừ hai phân số khác mẫu số, ta quy đồng mẫu số rồi cộng hoặc trừ các tử số. Muốn nhân hai phân số, nhân tử với tử và mẫu với mẫu. Muốn chia cho một phân số, ta nhân với phân số nghịch đảo của nó. Ví dụ: 3/4 : 3/8 = 3/4 × 8/3 = 2. Kết quả nên được rút gọn về phân số tối giản.', 'Phân số: khái niệm và các phép tính', 'PASTED_TEXT', 'vi', 'Toán', '5', 'PENDING', 1, 0, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ai_knowledge_documents WHERE course_id = @course_id AND title = 'Phân số: khái niệm và các phép tính');
INSERT INTO ai_knowledge_documents (course_id, source_text, title, source_type, language_code, subject_code, grade_level, index_status, index_version, attempt_count, created_by, created_at, updated_at)
SELECT @course_id, 'Số thập phân gồm phần nguyên và phần thập phân ngăn cách bởi dấu phẩy. Để so sánh hai số thập phân, so sánh phần nguyên trước, nếu bằng nhau thì so sánh lần lượt từng chữ số phần thập phân từ trái sang phải. Khi nhân hai số thập phân, ta nhân như số tự nhiên rồi đếm tổng số chữ số ở phần thập phân của hai thừa số để đặt dấu phẩy. Khi chia cho một số thập phân, ta nhân cả số bị chia và số chia với 10, 100, ... để số chia thành số tự nhiên rồi thực hiện phép chia.', 'Số thập phân: so sánh và phép nhân chia', 'PASTED_TEXT', 'vi', 'Toán', '5', 'PENDING', 1, 0, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ai_knowledge_documents WHERE course_id = @course_id AND title = 'Số thập phân: so sánh và phép nhân chia');
INSERT INTO ai_knowledge_documents (course_id, source_text, title, source_type, language_code, subject_code, grade_level, index_status, index_version, attempt_count, created_by, created_at, updated_at)
SELECT @course_id, 'Tỉ số phần trăm của a so với b là a/b × 100%. Muốn tìm p% của một số, ta nhân số đó với p rồi chia cho 100. Muốn tìm một số khi biết p% của nó bằng m, ta lấy m chia cho p rồi nhân 100. Trong bài toán giảm giá, giá sau khi giảm p% bằng giá gốc nhân với (100 - p)/100. Trong bài toán tăng, giá mới bằng giá gốc nhân với (100 + p)/100. Ví dụ giá 200 000 đồng giảm 15% còn 200 000 × 0,85 = 170 000 đồng.', 'Tỉ số phần trăm và bài toán giảm giá', 'PASTED_TEXT', 'vi', 'Toán', '5', 'PENDING', 1, 0, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ai_knowledge_documents WHERE course_id = @course_id AND title = 'Tỉ số phần trăm và bài toán giảm giá');

-- Ngân hàng câu hỏi có skill_code.
INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, active, created_by, created_at, updated_at)
SELECT @course_id, 'Phân số nào bằng 1/2?', 'SINGLE_CHOICE', 'EASY', 'fractions', 'MANUAL', '2/4 rút gọn cho 2 được 1/2.', TRUE, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = 'Phân số nào bằng 1/2?');
SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = 'Phân số nào bằng 1/2?' LIMIT 1);
INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES (@q, '2/4', TRUE, 1, @now, @now), (@q, '2/3', FALSE, 2, @now, @now), (@q, '3/4', FALSE, 3, @now, @now), (@q, '1/3', FALSE, 4, @now, @now) ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);
INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, active, created_by, created_at, updated_at)
SELECT @course_id, 'Tính 1/3 + 1/6.', 'SINGLE_CHOICE', 'MEDIUM', 'fractions', 'MANUAL', 'Quy đồng: 2/6 + 1/6 = 3/6 = 1/2.', TRUE, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = 'Tính 1/3 + 1/6.');
SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = 'Tính 1/3 + 1/6.' LIMIT 1);
INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES (@q, '1/2', TRUE, 1, @now, @now), (@q, '2/9', FALSE, 2, @now, @now), (@q, '1/9', FALSE, 3, @now, @now), (@q, '2/3', FALSE, 4, @now, @now) ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);
INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, active, created_by, created_at, updated_at)
SELECT @course_id, 'Tính 3/4 : 3/8.', 'SINGLE_CHOICE', 'HARD', 'fractions', 'MANUAL', '3/4 × 8/3 = 2.', TRUE, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = 'Tính 3/4 : 3/8.');
SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = 'Tính 3/4 : 3/8.' LIMIT 1);
INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES (@q, '2', TRUE, 1, @now, @now), (@q, '1/2', FALSE, 2, @now, @now), (@q, '9/32', FALSE, 3, @now, @now), (@q, '4', FALSE, 4, @now, @now) ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);
INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, active, created_by, created_at, updated_at)
SELECT @course_id, 'Số nào lớn nhất: 0,5; 0,45; 0,405; 0,54?', 'SINGLE_CHOICE', 'EASY', 'decimals', 'MANUAL', 'So sánh phần thập phân: 0,54 lớn nhất.', TRUE, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = 'Số nào lớn nhất: 0,5; 0,45; 0,405; 0,54?');
SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = 'Số nào lớn nhất: 0,5; 0,45; 0,405; 0,54?' LIMIT 1);
INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES (@q, '0,54', TRUE, 1, @now, @now), (@q, '0,5', FALSE, 2, @now, @now), (@q, '0,45', FALSE, 3, @now, @now), (@q, '0,405', FALSE, 4, @now, @now) ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);
INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, active, created_by, created_at, updated_at)
SELECT @course_id, 'Tính 2,5 × 0,4.', 'SINGLE_CHOICE', 'MEDIUM', 'decimals', 'MANUAL', '25 × 4 = 100, có hai chữ số thập phân nên bằng 1.', TRUE, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = 'Tính 2,5 × 0,4.');
SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = 'Tính 2,5 × 0,4.' LIMIT 1);
INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES (@q, '1', TRUE, 1, @now, @now), (@q, '10', FALSE, 2, @now, @now), (@q, '0,1', FALSE, 3, @now, @now), (@q, '2,9', FALSE, 4, @now, @now) ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);
INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, active, created_by, created_at, updated_at)
SELECT @course_id, 'Tính 7,2 : 0,06.', 'SINGLE_CHOICE', 'HARD', 'decimals', 'MANUAL', 'Nhân cả hai số với 100: 720 : 6 = 120.', TRUE, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = 'Tính 7,2 : 0,06.');
SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = 'Tính 7,2 : 0,06.' LIMIT 1);
INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES (@q, '120', TRUE, 1, @now, @now), (@q, '12', FALSE, 2, @now, @now), (@q, '1,2', FALSE, 3, @now, @now), (@q, '1200', FALSE, 4, @now, @now) ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);
INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, active, created_by, created_at, updated_at)
SELECT @course_id, '25% của 80 là bao nhiêu?', 'SINGLE_CHOICE', 'EASY', 'percentages', 'MANUAL', '80 × 25 / 100 = 20.', TRUE, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = '25% của 80 là bao nhiêu?');
SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = '25% của 80 là bao nhiêu?' LIMIT 1);
INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES (@q, '20', TRUE, 1, @now, @now), (@q, '25', FALSE, 2, @now, @now), (@q, '32', FALSE, 3, @now, @now), (@q, '5', FALSE, 4, @now, @now) ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);
INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, active, created_by, created_at, updated_at)
SELECT @course_id, 'Giá 200 000 đồng giảm 15%. Giá sau giảm?', 'SINGLE_CHOICE', 'MEDIUM', 'percentages', 'MANUAL', '200 000 × 0,85 = 170 000.', TRUE, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = 'Giá 200 000 đồng giảm 15%. Giá sau giảm?');
SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = 'Giá 200 000 đồng giảm 15%. Giá sau giảm?' LIMIT 1);
INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES (@q, '170 000', TRUE, 1, @now, @now), (@q, '185 000', FALSE, 2, @now, @now), (@q, '30 000', FALSE, 3, @now, @now), (@q, '215 000', FALSE, 4, @now, @now) ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);
INSERT INTO questions (course_id, content, question_type, difficulty, skill_code, source, explanation, active, created_by, created_at, updated_at)
SELECT @course_id, 'Số nào tăng 20% thì được 90?', 'SINGLE_CHOICE', 'HARD', 'percentages', 'MANUAL', 'x × 1,2 = 90 nên x = 75.', TRUE, @teacher_id, @now, @now FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE course_id = @course_id AND content = 'Số nào tăng 20% thì được 90?');
SET @q = (SELECT id FROM questions WHERE course_id = @course_id AND content = 'Số nào tăng 20% thì được 90?' LIMIT 1);
INSERT INTO question_options (question_id, content, correct, position, created_at, updated_at) VALUES (@q, '75', TRUE, 1, @now, @now), (@q, '72', FALSE, 2, @now, @now), (@q, '108', FALSE, 3, @now, @now), (@q, '70', FALSE, 4, @now, @now) ON DUPLICATE KEY UPDATE content = VALUES(content), correct = VALUES(correct);

-- Học viên strong: 12 tương tác, đã xử lý.
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'fractions', 'EXAM_QUESTION', 1, 'EASY', 1, 1, TRUE, 0, @now - INTERVAL 30 DAY, @now - INTERVAL 30 DAY, 'DEMO:strong:1');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'decimals', 'EXAM_QUESTION', 2, 'EASY', 1, 1, TRUE, 0, @now - INTERVAL 29 DAY, @now - INTERVAL 29 DAY, 'DEMO:strong:2');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'percentages', 'EXAM_QUESTION', 3, 'EASY', 1, 1, TRUE, 0, @now - INTERVAL 28 DAY, @now - INTERVAL 28 DAY, 'DEMO:strong:3');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'fractions', 'EXAM_QUESTION', 4, 'MEDIUM', 1, 1, TRUE, 0, @now - INTERVAL 27 DAY, @now - INTERVAL 27 DAY, 'DEMO:strong:4');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'decimals', 'EXAM_QUESTION', 5, 'MEDIUM', 1, 1, TRUE, 0, @now - INTERVAL 26 DAY, @now - INTERVAL 26 DAY, 'DEMO:strong:5');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'percentages', 'EXAM_QUESTION', 6, 'MEDIUM', 1, 1, TRUE, 0, @now - INTERVAL 25 DAY, @now - INTERVAL 25 DAY, 'DEMO:strong:6');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'fractions', 'EXAM_QUESTION', 7, 'MEDIUM', 1, 1, TRUE, 0, @now - INTERVAL 24 DAY, @now - INTERVAL 24 DAY, 'DEMO:strong:7');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'decimals', 'EXAM_QUESTION', 8, 'HARD', 1, 1, TRUE, 0, @now - INTERVAL 23 DAY, @now - INTERVAL 23 DAY, 'DEMO:strong:8');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'percentages', 'EXAM_QUESTION', 9, 'MEDIUM', 0, 1, FALSE, 0, @now - INTERVAL 22 DAY, @now - INTERVAL 22 DAY, 'DEMO:strong:9');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'fractions', 'EXAM_QUESTION', 10, 'HARD', 1, 1, TRUE, 0, @now - INTERVAL 21 DAY, @now - INTERVAL 21 DAY, 'DEMO:strong:10');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'decimals', 'EXAM_QUESTION', 11, 'HARD', 1, 1, TRUE, 0, @now - INTERVAL 20 DAY, @now - INTERVAL 20 DAY, 'DEMO:strong:11');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_strong, @course_id, @lesson_id, 'percentages', 'EXAM_QUESTION', 12, 'HARD', 1, 1, TRUE, 0, @now - INTERVAL 19 DAY, @now - INTERVAL 19 DAY, 'DEMO:strong:12');
INSERT INTO learner_skill_states (student_id, skill_code, skill_name, subject_code, mastery_score, evidence_count, last_evidence_at, model_version, updated_at)
VALUES (@student_strong, 'fractions', 'Phân số', 'Toán', 0.87995, 4, @now, 'ema-0.3', @now)
ON DUPLICATE KEY UPDATE mastery_score = VALUES(mastery_score), evidence_count = VALUES(evidence_count), last_evidence_at = VALUES(last_evidence_at), model_version = VALUES(model_version), updated_at = VALUES(updated_at);
INSERT INTO learner_skill_states (student_id, skill_code, skill_name, subject_code, mastery_score, evidence_count, last_evidence_at, model_version, updated_at)
VALUES (@student_strong, 'decimals', 'Số thập phân', 'Toán', 0.87995, 4, @now, 'ema-0.3', @now)
ON DUPLICATE KEY UPDATE mastery_score = VALUES(mastery_score), evidence_count = VALUES(evidence_count), last_evidence_at = VALUES(last_evidence_at), model_version = VALUES(model_version), updated_at = VALUES(updated_at);
INSERT INTO learner_skill_states (student_id, skill_code, skill_name, subject_code, mastery_score, evidence_count, last_evidence_at, model_version, updated_at)
VALUES (@student_strong, 'percentages', 'Phần trăm', 'Toán', 0.66995, 4, @now, 'ema-0.3', @now)
ON DUPLICATE KEY UPDATE mastery_score = VALUES(mastery_score), evidence_count = VALUES(evidence_count), last_evidence_at = VALUES(last_evidence_at), model_version = VALUES(model_version), updated_at = VALUES(updated_at);
INSERT INTO learner_profiles (student_id, average_score, score_stddev, strength_topics_json, weak_topics_json, profile_vector_version, profile_version, last_interaction_at, computed_at, created_at, updated_at)
VALUES (@student_strong, 0.80995, 0.09899, '["fractions","decimals"]', '[]', 1, 13, @now - INTERVAL 19 DAY, @now, @now, @now)
ON DUPLICATE KEY UPDATE average_score = VALUES(average_score), score_stddev = VALUES(score_stddev), strength_topics_json = VALUES(strength_topics_json), weak_topics_json = VALUES(weak_topics_json), profile_version = VALUES(profile_version), computed_at = VALUES(computed_at), updated_at = VALUES(updated_at);

-- Học viên weak: 10 tương tác, đã xử lý.
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'fractions', 'EXAM_QUESTION', 1, 'EASY', 0, 1, FALSE, 0, @now - INTERVAL 25 DAY, @now - INTERVAL 25 DAY, 'DEMO:weak:1');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'decimals', 'EXAM_QUESTION', 2, 'EASY', 1, 1, TRUE, 0, @now - INTERVAL 24 DAY, @now - INTERVAL 24 DAY, 'DEMO:weak:2');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'percentages', 'EXAM_QUESTION', 3, 'EASY', 1, 1, TRUE, 0, @now - INTERVAL 23 DAY, @now - INTERVAL 23 DAY, 'DEMO:weak:3');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'fractions', 'EXAM_QUESTION', 4, 'EASY', 0, 1, FALSE, 0, @now - INTERVAL 22 DAY, @now - INTERVAL 22 DAY, 'DEMO:weak:4');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'decimals', 'EXAM_QUESTION', 5, 'MEDIUM', 1, 1, TRUE, 1, @now - INTERVAL 21 DAY, @now - INTERVAL 21 DAY, 'DEMO:weak:5');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'fractions', 'EXAM_QUESTION', 6, 'EASY', 0, 1, FALSE, 0, @now - INTERVAL 20 DAY, @now - INTERVAL 20 DAY, 'DEMO:weak:6');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'decimals', 'EXAM_QUESTION', 7, 'MEDIUM', 0, 1, FALSE, 0, @now - INTERVAL 19 DAY, @now - INTERVAL 19 DAY, 'DEMO:weak:7');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'fractions', 'EXAM_QUESTION', 8, 'MEDIUM', 0, 1, FALSE, 0, @now - INTERVAL 18 DAY, @now - INTERVAL 18 DAY, 'DEMO:weak:8');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'fractions', 'EXAM_QUESTION', 9, 'EASY', 1, 1, TRUE, 2, @now - INTERVAL 17 DAY, @now - INTERVAL 17 DAY, 'DEMO:weak:9');
INSERT IGNORE INTO learning_interactions (student_id, course_id, lesson_id, skill_code, item_type, item_id, difficulty_level, score, max_score, is_correct, hints_used, occurred_at, processed_at, event_key)
VALUES (@student_weak, @course_id, @lesson_id, 'decimals', 'EXAM_QUESTION', 10, 'MEDIUM', 1, 1, TRUE, 0, @now - INTERVAL 16 DAY, @now - INTERVAL 16 DAY, 'DEMO:weak:10');
INSERT INTO learner_skill_states (student_id, skill_code, skill_name, subject_code, mastery_score, evidence_count, last_evidence_at, model_version, updated_at)
VALUES (@student_weak, 'fractions', 'Phân số', 'Toán', 0.20403, 5, @now, 'ema-0.3', @now)
ON DUPLICATE KEY UPDATE mastery_score = VALUES(mastery_score), evidence_count = VALUES(evidence_count), last_evidence_at = VALUES(last_evidence_at), model_version = VALUES(model_version), updated_at = VALUES(updated_at);
INSERT INTO learner_skill_states (student_id, skill_code, skill_name, subject_code, mastery_score, evidence_count, last_evidence_at, model_version, updated_at)
VALUES (@student_weak, 'decimals', 'Số thập phân', 'Toán', 0.62585, 4, @now, 'ema-0.3', @now)
ON DUPLICATE KEY UPDATE mastery_score = VALUES(mastery_score), evidence_count = VALUES(evidence_count), last_evidence_at = VALUES(last_evidence_at), model_version = VALUES(model_version), updated_at = VALUES(updated_at);
INSERT INTO learner_skill_states (student_id, skill_code, skill_name, subject_code, mastery_score, evidence_count, last_evidence_at, model_version, updated_at)
VALUES (@student_weak, 'percentages', 'Phần trăm', 'Toán', 0.65000, 1, @now, 'ema-0.3', @now)
ON DUPLICATE KEY UPDATE mastery_score = VALUES(mastery_score), evidence_count = VALUES(evidence_count), last_evidence_at = VALUES(last_evidence_at), model_version = VALUES(model_version), updated_at = VALUES(updated_at);
INSERT INTO learner_profiles (student_id, average_score, score_stddev, strength_topics_json, weak_topics_json, profile_vector_version, profile_version, last_interaction_at, computed_at, created_at, updated_at)
VALUES (@student_weak, 0.49329, 0.20478, '[]', '["fractions"]', 1, 11, @now - INTERVAL 16 DAY, @now, @now, @now)
ON DUPLICATE KEY UPDATE average_score = VALUES(average_score), score_stddev = VALUES(score_stddev), strength_topics_json = VALUES(strength_topics_json), weak_topics_json = VALUES(weak_topics_json), profile_version = VALUES(profile_version), computed_at = VALUES(computed_at), updated_at = VALUES(updated_at);

-- @student_cold cố ý không có dòng nào trong learner_*: kỹ năng nào cũng cold start (mastery 0.5).
