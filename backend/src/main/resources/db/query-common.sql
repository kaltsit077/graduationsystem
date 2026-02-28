-- ============================================
-- 常用查询脚本
-- 使用方法：mysql -u root -p graduation_topic < query-common.sql
-- 或在 MySQL 客户端中执行
-- ============================================

-- 1. 查看所有用户及其角色统计
SELECT role, COUNT(*) as count 
FROM user 
GROUP BY role;

-- 2. 查看所有学生信息
SELECT 
    u.id,
    u.username,
    u.real_name,
    sp.major,
    sp.grade,
    sp.interest_desc,
    u.created_at
FROM user u
LEFT JOIN student_profile sp ON u.id = sp.user_id
WHERE u.role = 'STUDENT'
ORDER BY u.created_at DESC;

-- 3. 查看所有导师信息
SELECT 
    u.id,
    u.username,
    u.real_name,
    tp.title,
    tp.research_direction,
    tp.max_student_count,
    u.created_at
FROM user u
LEFT JOIN teacher_profile tp ON u.id = tp.user_id
WHERE u.role = 'TEACHER'
ORDER BY u.created_at DESC;

-- 4. 查看所有选题及其状态
SELECT 
    t.id,
    t.title,
    t.status,
    u.real_name as teacher_name,
    t.max_applicants,
    t.current_applicants,
    t.created_at
FROM topic t
JOIN user u ON t.teacher_id = u.id
ORDER BY t.created_at DESC;

-- 5. 查看所有申请及其状态
SELECT 
    ta.id,
    t.title as topic_title,
    u1.real_name as student_name,
    u2.real_name as teacher_name,
    ta.status,
    ta.match_score,
    ta.created_at
FROM topic_application ta
JOIN topic t ON ta.topic_id = t.id
JOIN user u1 ON ta.student_id = u1.id
JOIN user u2 ON t.teacher_id = u2.id
ORDER BY ta.created_at DESC;

-- 6. 查看未读通知
SELECT 
    n.id,
    u.real_name as user_name,
    n.type,
    n.title,
    n.content,
    n.created_at
FROM notification n
JOIN user u ON n.user_id = u.id
WHERE n.is_read = 0
ORDER BY n.created_at DESC;

-- 7. 统计各状态选题数量
SELECT status, COUNT(*) as count 
FROM topic 
GROUP BY status;

-- 8. 统计各状态申请数量
SELECT status, COUNT(*) as count 
FROM topic_application 
GROUP BY status;

-- 9. 查看最热门的选题（申请人数最多的）
SELECT 
    t.id,
    t.title,
    t.current_applicants,
    t.max_applicants,
    u.real_name as teacher_name
FROM topic t
JOIN user u ON t.teacher_id = u.id
WHERE t.status = 'OPEN'
ORDER BY t.current_applicants DESC
LIMIT 10;

-- 10. 查看学生的申请历史（替换 student_id 为实际学生ID）
-- SELECT 
--     ta.id,
--     t.title as topic_title,
--     ta.status,
--     ta.match_score,
--     ta.teacher_feedback,
--     ta.created_at
-- FROM topic_application ta
-- JOIN topic t ON ta.topic_id = t.id
-- WHERE ta.student_id = 1  -- 替换为实际学生ID
-- ORDER BY ta.created_at DESC;



