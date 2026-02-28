-- 重置管理员账号脚本
-- 删除现有的admin用户，系统会在启动时自动创建新的admin账号
-- 用户名: admin, 密码: 123456

-- 删除现有的admin用户
DELETE FROM `user` WHERE `username` = 'admin';

-- 注意：删除后重启后端服务，系统会自动创建新的admin账号
-- 新账号信息：
-- 用户名: admin
-- 密码: 123456
-- 真实姓名: 系统管理员

