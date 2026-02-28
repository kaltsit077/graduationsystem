-- 毕业论文选题与反馈系统 - 数据库表结构
-- 字符集：UTF8MB4
-- 时区：Asia/Shanghai

-- 1. 用户表（通用用户信息）
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（学号/工号）',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希（BCrypt）',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `role` ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL COMMENT '角色：学生/导师/管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 学生信息表
CREATE TABLE IF NOT EXISTS `student_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（关联user表）',
    `major` VARCHAR(100) DEFAULT NULL COMMENT '专业',
    `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
    `interest_desc` TEXT COMMENT '兴趣描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息表';

-- 3. 导师信息表
CREATE TABLE IF NOT EXISTS `teacher_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（关联user表）',
    `title` VARCHAR(50) DEFAULT NULL COMMENT '职称',
    `research_direction` TEXT COMMENT '研究方向',
    `max_student_count` INT NOT NULL DEFAULT 10 COMMENT '最大可带学生数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_teacher_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导师信息表';

-- 4. 标签表（系统标签字典）
CREATE TABLE IF NOT EXISTS `tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name` VARCHAR(20) NOT NULL COMMENT '标签名称（2-6字）',
    `type` ENUM('TEACHER', 'STUDENT', 'TOPIC') NOT NULL COMMENT '标签类型：导师/学生/选题',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_type` (`name`, `type`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签字典表';

-- 5. 用户标签关联表
CREATE TABLE IF NOT EXISTS `user_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tag_name` VARCHAR(20) NOT NULL COMMENT '标签名称（冗余存储，便于查询）',
    `weight` DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT '标签权重（0-1之间，研究方向标签权重0.9）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tag_name` (`tag_name`),
    CONSTRAINT `fk_user_tag_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户标签关联表';

-- 6. 选题表
CREATE TABLE IF NOT EXISTS `topic` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '选题ID',
    `teacher_id` BIGINT NOT NULL COMMENT '导师ID',
    `title` VARCHAR(200) NOT NULL COMMENT '选题标题',
    `description` TEXT COMMENT '选题描述',
    `status` ENUM('DRAFT', 'PENDING_REVIEW', 'REJECTED', 'OPEN', 'CLOSED') NOT NULL DEFAULT 'DRAFT' COMMENT '状态：草稿/待审核/已驳回/已开放/已关闭',
    `max_applicants` INT NOT NULL DEFAULT 1 COMMENT '最大申请人数',
    `current_applicants` INT NOT NULL DEFAULT 0 COMMENT '当前申请人数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_topic_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选题表';

-- 7. 选题标签关联表
CREATE TABLE IF NOT EXISTS `topic_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `topic_id` BIGINT NOT NULL COMMENT '选题ID',
    `tag_name` VARCHAR(20) NOT NULL COMMENT '标签名称',
    `weight` DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT '标签权重',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_topic_id` (`topic_id`),
    KEY `idx_tag_name` (`tag_name`),
    CONSTRAINT `fk_topic_tag_topic` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选题标签关联表';

-- 8. 选题审核记录表
CREATE TABLE IF NOT EXISTS `topic_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核记录ID',
    `topic_id` BIGINT NOT NULL COMMENT '选题ID',
    `admin_id` BIGINT NOT NULL COMMENT '管理员ID',
    `result` ENUM('PASS', 'REJECT') NOT NULL COMMENT '审核结果：通过/驳回',
    `comment` TEXT COMMENT '审核意见',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_topic_id` (`topic_id`),
    KEY `idx_admin_id` (`admin_id`),
    CONSTRAINT `fk_topic_review_topic` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_topic_review_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选题审核记录表';

-- 9. 选题申请表
CREATE TABLE IF NOT EXISTS `topic_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `topic_id` BIGINT NOT NULL COMMENT '选题ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：待审核/已通过/已拒绝',
    `remark` TEXT COMMENT '学生申请备注',
    `teacher_feedback` TEXT COMMENT '导师反馈',
    `match_score` DECIMAL(5,4) DEFAULT NULL COMMENT '匹配度得分（0-1之间）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_student` (`topic_id`, `student_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_status` (`status`),
    KEY `idx_match_score` (`match_score`),
    CONSTRAINT `fk_application_topic` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_application_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选题申请表';

-- 10. 论文表
CREATE TABLE IF NOT EXISTS `thesis` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '论文ID',
    `topic_id` BIGINT NOT NULL COMMENT '选题ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件原名',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `status` ENUM('UPLOADED', 'REVIEWED') NOT NULL DEFAULT 'UPLOADED' COMMENT '状态：已上传/已评审',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_topic_id` (`topic_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_thesis_topic` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_thesis_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论文表';

-- 11. 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型：SYSTEM/APPLICATION_RESULT/TOPIC_OPEN/TOPIC_REVIEW等',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联对象ID（如选题ID、申请ID等）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_type` (`type`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 插入初始数据（可选）

-- 插入管理员账号（密码：admin123，实际使用时请修改）
-- INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
-- VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '系统管理员', 'ADMIN', 1);

