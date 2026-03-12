-- 初始化测试数据（可选）
-- 所有账号默认密码：123456
-- 对应 BCrypt 哈希：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA

SET NAMES utf8mb4;
SET time_zone = '+08:00';

-- 1. 学生与导师账号
INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`)
SELECT 'student001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '张三', 'STUDENT', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'student001');

INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`)
SELECT 'student002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '李四', 'STUDENT', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'student002');

INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`)
SELECT 'teacher001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '王老师', 'TEACHER', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'teacher001');

INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`)
SELECT 'teacher002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '赵老师', 'TEACHER', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'teacher002');

-- 2. 学生 / 导师档案
INSERT IGNORE INTO `student_profile` (`user_id`, `major`, `grade`, `interest_desc`, `created_at`, `updated_at`)
SELECT u.id,
       '计算机科学与技术',
       '2022级',
       '对机器学习、数据挖掘和 Web 开发感兴趣',
       NOW(), NOW()
FROM `user` u
WHERE u.username = 'student001';

INSERT IGNORE INTO `student_profile` (`user_id`, `major`, `grade`, `interest_desc`, `created_at`, `updated_at`)
SELECT u.id,
       '软件工程',
       '2022级',
       '偏好前端开发、可视化与用户体验',
       NOW(), NOW()
FROM `user` u
WHERE u.username = 'student002';

INSERT IGNORE INTO `teacher_profile` (`user_id`, `title`, `research_direction`, `max_student_count`, `created_at`, `updated_at`)
SELECT u.id,
       '副教授',
       '机器学习、教育数据挖掘',
       8,
       NOW(), NOW()
FROM `user` u
WHERE u.username = 'teacher001';

INSERT IGNORE INTO `teacher_profile` (`user_id`, `title`, `research_direction`, `max_student_count`, `created_at`, `updated_at`)
SELECT u.id,
       '讲师',
       'Web 全栈开发、前端工程化',
       6,
       NOW(), NOW()
FROM `user` u
WHERE u.username = 'teacher002';

-- 3. 导师开放的选题（用于“选题中心”与质量统计）
INSERT INTO `topic` (`teacher_id`, `title`, `description`, `status`, `max_applicants`,
                     `current_applicants`, `created_at`, `updated_at`)
SELECT u.id,
       '基于学生行为数据的学习预警模型研究',
       '基于学习行为日志与成绩数据，构建机器学习模型对学习风险进行预测，并给出可解释特征。',
       'OPEN',
       2,
       0,
       NOW(), NOW()
FROM `user` u
WHERE u.username = 'teacher001'
  AND NOT EXISTS (
      SELECT 1 FROM `topic`
      WHERE `title` = '基于学生行为数据的学习预警模型研究'
  );

INSERT INTO `topic` (`teacher_id`, `title`, `description`, `status`, `max_applicants`,
                     `current_applicants`, `created_at`, `updated_at`)
SELECT u.id,
       '毕业论文选题管理系统的前端可视化优化',
       '从交互与可视化角度优化选题中心、导师列表与质量分析页面，提升学生选题体验。',
       'OPEN',
       3,
       0,
       NOW(), NOW()
FROM `user` u
WHERE u.username = 'teacher002'
  AND NOT EXISTS (
      SELECT 1 FROM `topic`
      WHERE `title` = '毕业论文选题管理系统的前端可视化优化'
  );

-- 4. 选题质量统计（用于导师列表中的“历史评价概览”，可选）
INSERT INTO `topic_metrics` (`topic_id`, `teacher_id`, `total_students`,
                             `avg_score`, `excellent_ratio`, `fail_ratio`, `last_updated_at`)
SELECT t.id,
       t.teacher_id,
       3,
       88.5,
       0.50,
       0.10,
       NOW()
FROM `topic` t
JOIN `user` u ON t.teacher_id = u.id
WHERE t.title = '基于学生行为数据的学习预警模型研究'
  AND NOT EXISTS (
      SELECT 1 FROM `topic_metrics` tm WHERE tm.topic_id = t.id
  );

INSERT INTO `topic_metrics` (`topic_id`, `teacher_id`, `total_students`,
                             `avg_score`, `excellent_ratio`, `fail_ratio`, `last_updated_at`)
SELECT t.id,
       t.teacher_id,
       2,
       90.0,
       0.60,
       0.00,
       NOW()
FROM `topic` t
JOIN `user` u ON t.teacher_id = u.id
WHERE t.title = '毕业论文选题管理系统的前端可视化优化'
  AND NOT EXISTS (
      SELECT 1 FROM `topic_metrics` tm WHERE tm.topic_id = t.id
  );

