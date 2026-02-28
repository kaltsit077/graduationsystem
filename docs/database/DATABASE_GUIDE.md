# 数据库操作完整指南

本文档详细说明数据库的正确操作流程，以及数据写入的位置和方式。

---

## 📋 目录

1. [数据库架构概览](#数据库架构概览)
2. [数据库初始化流程](#数据库初始化流程)
3. [数据写入位置和方式](#数据写入位置和方式)
4. [常用数据库操作](#常用数据库操作)
5. [数据维护和备份](#数据维护和备份)

---

## 🗄️ 数据库架构概览

### 数据库信息

- **数据库名**: `graduation_topic`
- **字符集**: `utf8mb4`
- **排序规则**: `utf8mb4_unicode_ci`
- **时区**: `Asia/Shanghai`

### 数据表结构（11张表）

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `user` | 用户表 | id, username, password_hash, real_name, role, status |
| `student_profile` | 学生信息表 | user_id, major, grade, interest_desc |
| `teacher_profile` | 导师信息表 | user_id, title, research_direction, max_student_count |
| `tag` | 标签字典表 | id, name, type |
| `user_tag` | 用户标签关联表 | user_id, tag_name, weight |
| `topic` | 选题表 | id, teacher_id, title, description, status |
| `topic_tag` | 选题标签关联表 | topic_id, tag_name, weight |
| `topic_review` | 选题审核记录表 | topic_id, admin_id, result, comment |
| `topic_application` | 选题申请表 | topic_id, student_id, status, match_score |
| `thesis` | 论文表 | topic_id, student_id, file_url, file_name |
| `notification` | 通知表 | user_id, type, title, content, is_read |

---

## 🚀 数据库初始化流程

### 方式一：使用 Docker Compose（推荐）

```bash
# 1. 启动所有服务（包括数据库）
docker-compose up -d

# 2. 查看数据库日志，确认初始化完成
docker logs mysql-graduation

# 3. 验证数据库是否创建成功
docker exec -it mysql-graduation mysql -u root -proot -e "SHOW DATABASES;"
```

**说明**：
- Docker Compose 会自动执行 `schema.sql` 和 `init-data.sql`
- 数据库会在容器首次启动时自动初始化

### 方式二：手动初始化

```bash
# 1. 连接 MySQL
docker exec -it mysql-graduation mysql -u root -proot

# 2. 创建数据库
CREATE DATABASE graduation_topic DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 3. 执行建表脚本
USE graduation_topic;
SOURCE /path/to/schema.sql;

# 4. （可选）执行初始化数据脚本
SOURCE /path/to/init-data.sql;
```

### 方式三：使用 SQL 脚本文件

```bash
# Windows PowerShell
Get-Content backend\src\main\resources\db\schema.sql | docker exec -i mysql-graduation mysql -u root -proot graduation_topic

# Linux/Mac
docker exec -i mysql-graduation mysql -u root -proot graduation_topic < backend/src/main/resources/db/schema.sql
```

---

## 📝 数据写入位置和方式

### 1. 系统自动写入（应用层）

#### 1.1 用户注册和登录

**位置**: `backend/src/main/java/com/example/graduation/service/AuthService.java`

```java
// 登录时不会写入数据，只验证
public LoginResponse login(LoginRequest request) {
    User user = userMapper.selectOne(...);
    // 验证密码...
    return new LoginResponse(...);
}
```

#### 1.2 管理员账号自动创建

**位置**: `backend/src/main/java/com/example/graduation/config/AdminInitializer.java`

```java
// 应用启动时自动执行
@Override
public void run(String... args) {
    if (admin == null) {
        userMapper.insert(newAdmin);  // 自动创建管理员
    }
}
```

**配置位置**: `backend/src/main/resources/application-prod.yml`
```yaml
admin:
  username: admin
  password: 123456
  auto-create: true
```

#### 1.3 学生/导师信息完善

**位置**: 
- `backend/src/main/java/com/example/graduation/service/StudentService.java`
- `backend/src/main/java/com/example/graduation/service/TeacherService.java`

**API 接口**:
- `PUT /api/student/profile` - 更新学生信息
- `PUT /api/teacher/profile` - 更新导师信息

**数据写入流程**:
```
前端表单提交 
  → StudentController/TeacherController 
  → StudentService/TeacherService 
  → StudentProfileMapper/TeacherProfileMapper.insert() 
  → 数据库 student_profile/teacher_profile 表
```

#### 1.4 选题创建和管理

**位置**: `backend/src/main/java/com/example/graduation/service/TopicService.java`

**API 接口**:
- `POST /api/topics` - 创建选题
- `PUT /api/topics/{id}` - 更新选题
- `POST /api/topics/{id}/submit-review` - 提交审核

**数据写入流程**:
```
导师创建选题 
  → TopicController 
  → TopicService.createTopic() 
  → TopicMapper.insert() + TopicTagMapper.insert() 
  → 数据库 topic 和 topic_tag 表
```

#### 1.5 申请提交和处理

**位置**: `backend/src/main/java/com/example/graduation/service/ApplicationService.java`

**API 接口**:
- `POST /api/applications` - 提交申请
- `POST /api/applications/{id}/process` - 处理申请

**数据写入流程**:
```
学生提交申请 
  → ApplicationController 
  → ApplicationService.submitApplication() 
  → TopicApplicationMapper.insert() 
  → 数据库 topic_application 表
```

#### 1.6 通知创建

**位置**: `backend/src/main/java/com/example/graduation/service/NotificationService.java`

**自动触发场景**:
- 选题审核通过/驳回
- 申请通过/拒绝
- 选题开放通知

**数据写入流程**:
```
业务操作完成 
  → NotificationService.createNotification() 
  → NotificationMapper.insert() 
  → 数据库 notification 表
```

### 2. 手动写入（数据库层）

#### 2.1 使用 SQL 脚本

**位置**: `backend/src/main/resources/db/`

- `schema.sql` - 建表脚本（表结构）
- `init-data.sql` - 初始化数据（测试账号）
- `update-admin-password.sql` - 更新管理员密码
- `reset-admin.sql` - 重置管理员账号

**执行方式**:
```bash
# 执行 SQL 脚本
Get-Content backend\src\main\resources\db\init-data.sql | docker exec -i mysql-graduation mysql -u root -proot graduation_topic
```

#### 2.2 使用 MySQL 命令行

```bash
# 进入 MySQL 交互式命令行
docker exec -it mysql-graduation mysql -u root -proot graduation_topic

# 执行 SQL
INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
VALUES ('newuser', '$2a$10$...', '新用户', 'STUDENT', 1);
```

#### 2.3 使用管理员 API（推荐）

**位置**: `backend/src/main/java/com/example/graduation/controller/AdminController.java`

**API 接口**:
```http
POST /api/admin/users
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "realName": "新用户",
  "role": "STUDENT"
}
```

---

## 🔧 常用数据库操作

### 查看数据

```sql
-- 查看所有用户
SELECT id, username, real_name, role, status FROM user;

-- 查看学生信息
SELECT u.username, u.real_name, sp.major, sp.grade 
FROM user u 
JOIN student_profile sp ON u.id = sp.user_id;

-- 查看选题列表
SELECT t.id, t.title, t.status, u.real_name as teacher_name 
FROM topic t 
JOIN user u ON t.teacher_id = u.id;
```

### 更新数据

```sql
-- 更新用户状态
UPDATE user SET status = 0 WHERE username = 'student001';

-- 更新选题状态
UPDATE topic SET status = 'OPEN' WHERE id = 1;

-- 更新管理员密码
UPDATE user 
SET password_hash = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA' 
WHERE username = 'admin';
```

### 删除数据

```sql
-- 删除用户（会级联删除相关数据）
DELETE FROM user WHERE id = 1;

-- 删除选题申请
DELETE FROM topic_application WHERE id = 1;
```

### 统计查询

```sql
-- 统计各角色用户数量
SELECT role, COUNT(*) as count FROM user GROUP BY role;

-- 统计选题状态
SELECT status, COUNT(*) as count FROM topic GROUP BY status;

-- 统计申请状态
SELECT status, COUNT(*) as count FROM topic_application GROUP BY status;
```

---

## 💾 数据维护和备份

### 备份数据库

```bash
# 备份整个数据库
docker exec mysql-graduation mysqldump -u root -proot graduation_topic > backup_$(date +%Y%m%d).sql

# 备份特定表
docker exec mysql-graduation mysqldump -u root -proot graduation_topic user topic > backup_tables.sql
```

### 恢复数据库

```bash
# 恢复数据库
docker exec -i mysql-graduation mysql -u root -proot graduation_topic < backup_20231215.sql
```

### 数据迁移

```bash
# 导出数据
docker exec mysql-graduation mysqldump -u root -proot graduation_topic > migration.sql

# 导入到新数据库
docker exec -i mysql-graduation mysql -u root -proot new_database < migration.sql
```

---

## 📍 数据写入位置总结

### 应用层（自动写入）

| 数据类型 | Service 类 | Mapper 类 | 数据库表 |
|---------|-----------|----------|---------|
| 用户信息 | UserService | UserMapper | user |
| 学生信息 | StudentService | StudentProfileMapper | student_profile |
| 导师信息 | TeacherService | TeacherProfileMapper | teacher_profile |
| 标签 | TagService | UserTagMapper, TopicTagMapper | user_tag, topic_tag |
| 选题 | TopicService | TopicMapper | topic |
| 申请 | ApplicationService | TopicApplicationMapper | topic_application |
| 通知 | NotificationService | NotificationMapper | notification |
| 论文 | ThesisService | ThesisMapper | thesis |

### 数据库层（手动写入）

- **SQL 脚本**: `backend/src/main/resources/db/*.sql`
- **MySQL 命令行**: 直接执行 SQL
- **管理员 API**: `POST /api/admin/users`

---

## 🎯 最佳实践

1. **开发环境**: 使用初始化脚本快速创建测试数据
2. **生产环境**: 通过 API 接口写入数据，避免直接操作数据库
3. **数据备份**: 定期备份数据库，特别是生产环境
4. **密码管理**: 使用 BCrypt 加密，不要存储明文密码
5. **事务处理**: 关键操作使用 `@Transactional` 保证数据一致性

---

## 🔗 相关文档

- [数据库操作指南](backend/DATABASE_GUIDE.md) - 详细的数据库操作说明
- [用户创建指南](backend/USER_GUIDE.md) - 用户账号管理
- [Docker SQL 命令](DOCKER_SQL_COMMANDS.md) - Docker 环境下的数据库操作

---

**最后更新**: 2025-01-XX


