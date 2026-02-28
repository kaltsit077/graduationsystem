-- 更新管理员密码脚本
-- 将admin用户的密码更新为 123456
-- 使用BCrypt加密（强度10）

-- 更新admin用户密码为 123456
-- 注意：此哈希值已通过 BCrypt 验证，确保对应密码 123456
UPDATE `user` 
SET `password_hash` = '$2a$10$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG'
WHERE `username` = 'admin';

-- 注意：
-- 1. 这个哈希值对应密码: 123456
-- 2. 如果更新后仍无法登录，请使用 reset-admin.sql 脚本删除用户后重启服务

