-- 毕业论文选题与反馈系统 - 数据库表结构
-- 字符集：UTF8MB4
-- 时区：Asia/Shanghai

-- 1. 用户表（通用用户信息）
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（学号/工号）',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希（BCrypt）',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `background_url` VARCHAR(500) DEFAULT NULL COMMENT '用户自定义背景图 URL（静态资源路径）',
    `background_scale` INT NOT NULL DEFAULT 100 COMMENT '背景缩放百分比（50-200）',
    `background_pos_x` INT NOT NULL DEFAULT 50 COMMENT '背景水平位置百分比（0-100）',
    `background_pos_y` INT NOT NULL DEFAULT 50 COMMENT '背景垂直位置百分比（0-100）',
    `bg_overlay_alpha` DECIMAL(3,2) NOT NULL DEFAULT 0.78 COMMENT '背景遮罩透明度（0-1，越大越白）',
    `content_alpha` DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '内容容器白底透明度（0-1）',
    `content_blur` INT NOT NULL DEFAULT 0 COMMENT '内容容器毛玻璃模糊强度（px，0-24）',
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
    `major_courses` TEXT COMMENT '主修课程/已修课程（用于专业侧标签生成）',
    `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
    `interest_desc` TEXT COMMENT '兴趣描述',
    `tag_mode` ENUM('MAJOR', 'INTEREST', 'BOTH') DEFAULT 'BOTH' COMMENT '标签生成模式：仅专业/仅兴趣/综合',
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
    `tag_type` ENUM('MAJOR','INTEREST') NOT NULL DEFAULT 'INTEREST' COMMENT '标签类型：专业/兴趣（用于展示与匹配权重解释）',
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
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'COMPLETION_PENDING', 'COMPLETION_REJECTED', 'COMPLETED')
        NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：待审核/已通过/已拒绝/结题待审核/结题未通过/已结题',
    `approved_lock` TINYINT GENERATED ALWAYS AS (CASE WHEN `status` = 'APPROVED' THEN 1 ELSE NULL END) STORED COMMENT '用于约束同题仅一人通过（NULL 可重复）',
    `remark` TEXT COMMENT '学生申请备注',
    `teacher_feedback` TEXT COMMENT '导师反馈',
    `match_score` DECIMAL(5,4) DEFAULT NULL COMMENT '匹配度得分（0-1之间）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_student` (`topic_id`, `student_id`),
    UNIQUE KEY `uk_topic_approved` (`topic_id`, `approved_lock`),
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
    `stage` VARCHAR(64) DEFAULT NULL COMMENT '协作环节代码，见后端 CollabStage',
    `status` VARCHAR(32) NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED审核中/NEED_REVISION退回/REVIEWED已通过',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_topic_id` (`topic_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_thesis_topic` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_thesis_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论文表';

-- 11. 论文评价表（闭环评价数据）
CREATE TABLE IF NOT EXISTS `thesis_evaluation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `thesis_id` BIGINT NOT NULL COMMENT '论文ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID（冗余）',
    `teacher_id` BIGINT NOT NULL COMMENT '导师ID（冗余）',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '导师给出的论文总评分（0-100）',
    `defense_score` DECIMAL(5,2) DEFAULT NULL COMMENT '答辩成绩',
    `review_score` DECIMAL(5,2) DEFAULT NULL COMMENT '评阅成绩',
    `grade_level` VARCHAR(20) DEFAULT NULL COMMENT '等级：优/良/中/及格/不及格等',
    `comment` TEXT COMMENT '导师评价摘要',
    `student_score` DECIMAL(5,2) DEFAULT NULL COMMENT '学生对论文/指导的评分（0-100）',
    `student_comment` TEXT COMMENT '学生对论文质量和指导情况的评价',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_thesis` (`thesis_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    CONSTRAINT `fk_eval_thesis` FOREIGN KEY (`thesis_id`) REFERENCES `thesis` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论文评价表';

-- 12. 选题质量统计表（按选题聚合评价结果，用于可视化）
CREATE TABLE IF NOT EXISTS `topic_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `topic_id` BIGINT NOT NULL COMMENT '选题ID',
    `teacher_id` BIGINT NOT NULL COMMENT '导师ID（冗余）',
    `total_students` INT NOT NULL DEFAULT 0 COMMENT '选择该题的学生总数',
    `avg_score` DECIMAL(5,2) DEFAULT NULL COMMENT '平均分',
    `excellent_ratio` DECIMAL(5,4) DEFAULT NULL COMMENT '优秀率（0-1）',
    `fail_ratio` DECIMAL(5,4) DEFAULT NULL COMMENT '不及格率（0-1）',
    `last_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近统计时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic` (`topic_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    CONSTRAINT `fk_metrics_topic` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选题质量统计表';

-- 11. 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型：SYSTEM/APPLICATION_RESULT/TOPIC_OPEN/TOPIC_REVIEW等',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联对象ID（如选题ID、申请ID等）',
    `collab_stage` VARCHAR(64) DEFAULT NULL COMMENT '协作消息所属环节',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_type` (`type`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 13. 选题/导师变更申请表（学生发起的返工与换导师申请）
CREATE TABLE IF NOT EXISTS `change_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '变更申请ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `current_application_id` BIGINT NOT NULL COMMENT '当前生效的选题申请ID（topic_application.id）',
    `type` ENUM('CHANGE_TOPIC', 'CHANGE_TEACHER') NOT NULL COMMENT '变更类型：更换选题 / 更换导师',
    `reason` TEXT COMMENT '学生申请原因说明',
    `target_topic_id` BIGINT DEFAULT NULL COMMENT '学生建议的新选题ID（可选）',
    `target_teacher_id` BIGINT DEFAULT NULL COMMENT '学生建议的新导师ID（可选）',
    `status` ENUM('PENDING_TEACHER', 'PENDING_ADMIN', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING_TEACHER' COMMENT '当前处理状态',
    `teacher_decision` ENUM('APPROVED', 'REJECTED') DEFAULT NULL COMMENT '导师审批结果（仅更换选题场景使用）',
    `teacher_comment` TEXT COMMENT '导师审批意见',
    `admin_decision` ENUM('APPROVED', 'REJECTED') DEFAULT NULL COMMENT '管理员审批结果（仅更换导师或最终确认使用）',
    `admin_comment` TEXT COMMENT '管理员审批意见',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_change_request_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_change_request_application` FOREIGN KEY (`current_application_id`) REFERENCES `topic_application` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选题/导师变更申请表';

-- 14. 拜师申请表（学生发起的“先选导师”意向申请）
CREATE TABLE IF NOT EXISTS `mentor_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '拜师申请ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `teacher_id` BIGINT NOT NULL COMMENT '导师ID',
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '申请状态',
    `reason` TEXT COMMENT '学生申请说明（期望方向、个人计划等）',
    `teacher_comment` TEXT COMMENT '导师审批意见',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_mentor_app_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mentor_app_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拜师申请表';

-- 15. 系统设置表（全局配置，例如选题开放时间）
CREATE TABLE IF NOT EXISTS `system_setting` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（通常只使用一行记录）',
    `selection_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '选题系统是否启用全局开关（0-关闭，1-开启）',
    `selection_start_time` DATETIME DEFAULT NULL COMMENT '选题开放开始时间（可选，NULL 表示不限制开始时间）',
    `selection_end_time` DATETIME DEFAULT NULL COMMENT '选题开放结束时间（可选，NULL 表示不限制结束时间）',
    `graduation_season_start` DATETIME DEFAULT NULL COMMENT '毕业季总时间窗起（导师环节时间不得超出）',
    `graduation_season_end` DATETIME DEFAULT NULL COMMENT '毕业季总时间窗止',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统全局设置表';

CREATE TABLE IF NOT EXISTS `collab_stage_window` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `application_id` BIGINT NOT NULL COMMENT '选题申请ID',
    `stage` VARCHAR(64) NOT NULL COMMENT '环节代码',
    `window_start` DATETIME DEFAULT NULL COMMENT '环节开放开始',
    `window_end` DATETIME DEFAULT NULL COMMENT '环节开放结束',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_stage` (`application_id`, `stage`),
    KEY `idx_application_id` (`application_id`),
    CONSTRAINT `fk_csw_app` FOREIGN KEY (`application_id`) REFERENCES `topic_application` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导师为各协作环节配置的时间窗';

-- 插入初始数据（可选）

-- 插入管理员账号（密码：admin123，实际使用时请修改）
-- INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
-- VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '系统管理员', 'ADMIN', 1);

