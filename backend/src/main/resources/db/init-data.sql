-- 毕业论文选题与反馈系统 - 初始数据脚本
-- 用于创建测试用户账号
-- 注意：密码使用 BCrypt 加密，默认密码都是 123456

-- 插入管理员账号
-- 用户名: admin, 密码: 123456
-- 注意：此哈希值已通过 BCrypt 验证，确保对应密码 123456
INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
VALUES ('admin', '$2a$10$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG', '系统管理员', 'ADMIN', 1);

-- 插入导师账号（示例）
-- 用户名: teacher001, 密码: 123456
INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
VALUES ('teacher001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '张教授', 'TEACHER', 1);

-- 用户名: teacher002, 密码: 123456
INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
VALUES ('teacher002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '李教授', 'TEACHER', 1);

-- 插入学生账号（示例）
-- 用户名: student001, 密码: 123456
INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
VALUES ('student001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '张三', 'STUDENT', 1);

-- 用户名: student002, 密码: 123456
INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
VALUES ('student002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '李四', 'STUDENT', 1);

-- 用户名: student003, 密码: 123456
INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
VALUES ('student003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '王五', 'STUDENT', 1);

-- 注意：
-- 1. 所有默认密码都是 123456
-- 2. 密码已使用 BCrypt 加密（强度 10）
-- 3. 如需修改密码，可以使用以下 Java 代码生成新的 BCrypt 哈希：
--    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
--    String hash = encoder.encode("新密码");
-- 4. 生产环境请务必修改默认密码！

