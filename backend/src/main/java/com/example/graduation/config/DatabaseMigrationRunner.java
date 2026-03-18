package com.example.graduation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 轻量级数据库迁移：用于在不引入 Flyway/Liquibase 的情况下，
 * 让新增字段能平滑落地到已有数据库（避免 Unknown column）。
 *
 * 注意：该迁移是幂等的（会先检查列是否存在）。
 */
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final DataSource dataSource;

    public DatabaseMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 1) 启动时健康检查/报告：帮助定位“缺表/缺列导致接口异常”的同类问题
        SchemaReport report = new SchemaReport();
        try (Connection conn = dataSource.getConnection()) {
            report.databaseName = conn.getCatalog();

            // 关键表检查（按当前项目核心表列一份清单；如未来新增表，可在此处补充）
            String[] coreTables = new String[]{
                    "user",
                    "student_profile",
                    "teacher_profile",
                    "tag",
                    "user_tag",
                    "topic",
                    "topic_tag",
                    "topic_review",
                    "topic_application",
                    "thesis",
                    "thesis_evaluation",
                    "topic_metrics",
                    "notification",
                    "change_request",
                    "mentor_application",
                    "system_setting"
            };

            for (String t : coreTables) {
                if (!tableExists(conn, t)) {
                    report.missingTables.add(t);
                }
            }

            // 关键列检查（这里先覆盖近期变更/高风险字段）
            if (tableExists(conn, "student_profile") && !columnExists(conn, "student_profile", "tag_mode")) {
                report.missingColumns.add("student_profile.tag_mode");
            }
        }

        if (!report.missingTables.isEmpty() || !report.missingColumns.isEmpty()) {
            log.warn("数据库结构检查发现缺失项：db={}, missingTables={}, missingColumns={}",
                    report.databaseName,
                    report.missingTables,
                    report.missingColumns);
            log.warn("建议：优先确认 spring.sql.init 配置是否生效，并检查是否使用了正确的 profile（dev/prod）");
        } else {
            log.info("数据库结构检查通过：db={}，核心表/关键列均存在", report.databaseName);
        }

        // 2) 最小兜底迁移：对“影响面最大、最容易导致页面看似 403/500”的对象做自动补齐
        // system_setting：管理员“选题开放设置”依赖此表，缺表会导致接口失败
        ensureTableExists(
                "system_setting",
                "CREATE TABLE IF NOT EXISTS `system_setting` (\n" +
                        "    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（通常只使用一行记录）',\n" +
                        "    `selection_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '选题系统是否启用全局开关（0-关闭，1-开启）',\n" +
                        "    `selection_start_time` DATETIME DEFAULT NULL COMMENT '选题开放开始时间（可选，NULL 表示不限制开始时间）',\n" +
                        "    `selection_end_time` DATETIME DEFAULT NULL COMMENT '选题开放结束时间（可选，NULL 表示不限制结束时间）',\n" +
                        "    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',\n" +
                        "    PRIMARY KEY (`id`)\n" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统全局设置表（全局配置，例如选题开放时间）'"
        );

        // student_profile.tag_mode：学生可选择“专业/兴趣/综合”生成标签模式
        ensureColumnExists(
                "student_profile",
                "tag_mode",
                "ALTER TABLE student_profile " +
                        "ADD COLUMN tag_mode ENUM('MAJOR','INTEREST','BOTH') DEFAULT 'BOTH' " +
                        "COMMENT '标签生成模式：仅专业/仅兴趣/综合' AFTER interest_desc"
        );

        // student_profile.major_courses：主修课程，用于增强“仅专业/综合”的标签生成信号
        ensureColumnExists(
                "student_profile",
                "major_courses",
                "ALTER TABLE student_profile " +
                        "ADD COLUMN major_courses TEXT NULL " +
                        "COMMENT '主修课程/已修课程（用于专业侧标签生成）' AFTER major"
        );

        // user_tag.tag_type：用户可标注标签属于“专业/兴趣”，便于展示与个性化权重解释
        ensureColumnExists(
                "user_tag",
                "tag_type",
                "ALTER TABLE user_tag " +
                        "ADD COLUMN tag_type ENUM('MAJOR','INTEREST') NOT NULL DEFAULT 'INTEREST' " +
                        "COMMENT '标签类型：专业/兴趣' AFTER tag_name"
        );

        // user.background_url：用户自定义背景图（静态资源 URL）
        ensureColumnExists(
                "user",
                "background_url",
                "ALTER TABLE user " +
                        "ADD COLUMN background_url VARCHAR(500) NULL " +
                        "COMMENT '用户自定义背景图 URL（静态资源路径）' AFTER real_name"
        );

        // user 背景外观参数：缩放/位置/透明度/模糊（避免旧库缺列导致登录页/个人中心报 Unknown column）
        ensureColumnExists(
                "user",
                "background_scale",
                "ALTER TABLE user " +
                        "ADD COLUMN background_scale INT NOT NULL DEFAULT 100 " +
                        "COMMENT '背景缩放百分比（50-200）' AFTER background_url"
        );
        ensureColumnExists(
                "user",
                "background_pos_x",
                "ALTER TABLE user " +
                        "ADD COLUMN background_pos_x INT NOT NULL DEFAULT 50 " +
                        "COMMENT '背景水平位置百分比（0-100）' AFTER background_scale"
        );
        ensureColumnExists(
                "user",
                "background_pos_y",
                "ALTER TABLE user " +
                        "ADD COLUMN background_pos_y INT NOT NULL DEFAULT 50 " +
                        "COMMENT '背景垂直位置百分比（0-100）' AFTER background_pos_x"
        );
        ensureColumnExists(
                "user",
                "bg_overlay_alpha",
                "ALTER TABLE user " +
                        "ADD COLUMN bg_overlay_alpha DECIMAL(3,2) NOT NULL DEFAULT 0.78 " +
                        "COMMENT '背景遮罩透明度（0-1，越大越白）' AFTER background_pos_y"
        );
        ensureColumnExists(
                "user",
                "content_alpha",
                "ALTER TABLE user " +
                        "ADD COLUMN content_alpha DECIMAL(3,2) NOT NULL DEFAULT 1.00 " +
                        "COMMENT '内容容器白底透明度（0-1）' AFTER bg_overlay_alpha"
        );
        ensureColumnExists(
                "user",
                "content_blur",
                "ALTER TABLE user " +
                        "ADD COLUMN content_blur INT NOT NULL DEFAULT 0 " +
                        "COMMENT '内容容器毛玻璃模糊强度（px，0-24）' AFTER content_alpha"
        );
    }

    private static class SchemaReport {
        String databaseName;
        List<String> missingTables = new ArrayList<>();
        List<String> missingColumns = new ArrayList<>();
    }

    private void ensureColumnExists(String tableName, String columnName, String alterSql) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            if (columnExists(conn, tableName, columnName)) {
                return;
            }
            log.warn("检测到缺失列 {}.{}，将执行迁移 SQL", tableName, columnName);
            try (Statement st = conn.createStatement()) {
                st.execute(alterSql);
            }
            log.info("迁移完成：已添加列 {}.{}", tableName, columnName);
        }
    }

    private void ensureTableExists(String tableName, String createTableSql) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            if (tableExists(conn, tableName)) {
                return;
            }
            log.warn("检测到缺失表 {}，将执行建表 SQL", tableName);
            try (Statement st = conn.createStatement()) {
                st.execute(createTableSql);
            }
            log.info("迁移完成：已创建表 {}", tableName);
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws Exception {
        String sql = "SELECT COUNT(1) " +
                "FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
        String sql = "SELECT COUNT(1) " +
                "FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}

