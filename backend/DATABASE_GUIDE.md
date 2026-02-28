# 数据库操作指南

## 📊 数据库连接信息

根据配置文件 `application.yml`：

- **数据库名**：`graduation_topic`
- **用户名**：`root`
- **密码**：`root`
- **主机**：`localhost:3306`
- **字符集**：`utf8mb4`

---

## 🗄️ 数据库表结构

系统包含以下11张表：

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `user` | 用户表 | id, username, password_hash, real_name, role, status |
| `student_profile` | 学生信息表 | id, user_id, major, grade, interest_desc |
| `teacher_profile` | 导师信息表 | id, user_id, title, research_direction, max_student_count |
| `tag` | 标签字典表 | id, name, type |
| `user_tag` | 用户标签关联表 | id, user_id, tag_name, weight |
| `topic` | 选题表 | id, teacher_id, title, description, status, max_applicants |
| `topic_tag` | 选题标签关联表 | id, topic_id, tag_name, weight |
| `topic_review` | 选题审核记录表 | id, topic_id, admin_id, result, comment |
| `topic_application` | 选题申请表 | id, topic_id, student_id, status, remark, match_score |
| `thesis` | 论文表 | id, topic_id, student_id, file_url, file_name, file_size |
| `notification` | 通知表 | id, user_id, type, title, content, is_read |

---

## 🔍 方法一：使用 MySQL 命令行

### 1. 连接数据库

```bash
mysql -u root -p
# 输入密码：root
```

### 2. 切换到目标数据库

```sql
USE graduation_topic;
```

### 3. 查看所有表

```sql
SHOW TABLES;
```

### 4. 查看表结构

```sql
-- 查看表结构
DESC user;
-- 或
DESCRIBE user;
-- 或查看完整创建语句
SHOW CREATE TABLE user;
```

### 5. 查询数据（SELECT）

```sql
-- 查看所有用户
SELECT * FROM user;

-- 查看用户（隐藏密码）
SELECT id, username, real_name, role, status, created_at 
FROM user;

-- 按条件查询
SELECT * FROM user WHERE role = 'STUDENT';
SELECT * FROM user WHERE status = 1;

-- 分页查询
SELECT * FROM user LIMIT 10 OFFSET 0;  -- 第1页，每页10条
SELECT * FROM user LIMIT 10 OFFSET 10; -- 第2页

-- 统计查询
SELECT COUNT(*) FROM user;
SELECT role, COUNT(*) as count FROM user GROUP BY role;
```

### 6. 插入数据（INSERT）

```sql
-- 插入用户（需要先加密密码）
-- 注意：password_hash 需要使用 BCrypt 加密
INSERT INTO user (username, password_hash, real_name, role, status) 
VALUES ('student001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA', '张三', 'STUDENT', 1);

-- 插入学生信息
INSERT INTO student_profile (user_id, major, grade, interest_desc) 
VALUES (1, '计算机科学与技术', '2021级', '对机器学习和深度学习感兴趣');

-- 插入选题
INSERT INTO topic (teacher_id, title, description, status, max_applicants) 
VALUES (2, '基于深度学习的图像识别研究', '研究内容...', 'OPEN', 3);
```

### 7. 更新数据（UPDATE）

```sql
-- 更新用户信息
UPDATE user SET real_name = '李四' WHERE username = 'student001';

-- 更新用户状态（禁用账号）
UPDATE user SET status = 0 WHERE id = 1;

-- 更新选题状态
UPDATE topic SET status = 'CLOSED' WHERE id = 1;

-- 批量更新
UPDATE user SET status = 1 WHERE role = 'STUDENT';
```

### 8. 删除数据（DELETE）

```sql
-- 删除用户（注意：会级联删除相关数据）
DELETE FROM user WHERE id = 1;

-- 删除选题申请
DELETE FROM topic_application WHERE id = 1;

-- 清空表（危险操作！）
-- TRUNCATE TABLE notification;
```

### 9. 多表关联查询

```sql
-- 查看学生及其详细信息
SELECT u.id, u.username, u.real_name, sp.major, sp.grade
FROM user u
LEFT JOIN student_profile sp ON u.id = sp.user_id
WHERE u.role = 'STUDENT';

-- 查看选题及其导师信息
SELECT t.id, t.title, t.status, u.real_name as teacher_name
FROM topic t
JOIN user u ON t.teacher_id = u.id;

-- 查看申请及其关联信息
SELECT 
    ta.id as application_id,
    t.title as topic_title,
    u1.real_name as student_name,
    u2.real_name as teacher_name,
    ta.status,
    ta.match_score
FROM topic_application ta
JOIN topic t ON ta.topic_id = t.id
JOIN user u1 ON ta.student_id = u1.id
JOIN user u2 ON t.teacher_id = u2.id;
```

---

## 🖥️ 方法二：使用图形化工具

### 推荐工具

1. **Navicat**（付费，功能强大）
2. **MySQL Workbench**（官方免费工具）
3. **DBeaver**（免费，跨平台）
4. **phpMyAdmin**（Web界面）

### 连接配置

- **连接类型**：MySQL
- **主机名/IP**：localhost 或 127.0.0.1
- **端口**：3306
- **用户名**：root
- **密码**：root
- **数据库**：graduation_topic

### 基本操作

1. **查看表**：左侧导航栏展开数据库，双击表名
2. **查看数据**：点击"查看数据"或"浏览"
3. **编辑数据**：双击单元格直接编辑
4. **执行SQL**：打开"查询"窗口，输入SQL执行
5. **导入/导出**：右键表 → 导入向导 / 导出向导

---

## 🌐 方法三：通过 API 接口

系统已提供完整的 REST API，可以通过 HTTP 请求操作数据。

### 1. 用户管理（管理员接口）

**查看所有用户**
```bash
GET http://localhost:8080/api/admin/users
Authorization: Bearer <token>
```

**创建用户**
```bash
POST http://localhost:8080/api/admin/users
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "student001",
  "password": "123456",
  "realName": "张三",
  "role": "STUDENT"
}
```

**删除用户**
```bash
DELETE http://localhost:8080/api/admin/users/{userId}
Authorization: Bearer <token>
```

### 2. 选题管理

**查看选题列表**
```bash
GET http://localhost:8080/api/topics
Authorization: Bearer <token>
```

**创建选题**
```bash
POST http://localhost:8080/api/topics
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "基于深度学习的图像识别研究",
  "description": "研究内容...",
  "maxApplicants": 3
}
```

### 3. 申请管理

**查看申请列表**
```bash
GET http://localhost:8080/api/applications
Authorization: Bearer <token>
```

**提交申请**
```bash
POST http://localhost:8080/api/applications
Authorization: Bearer <token>
Content-Type: application/json

{
  "topicId": 1,
  "remark": "我对这个选题很感兴趣"
}
```

### 4. 使用 Postman 或 curl

**curl 示例**：
```bash
# 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 使用 Token 查询用户
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <your-token>"
```

---

## 📝 方法四：常用 SQL 脚本

### 快速查询脚本

创建 `backend/src/main/resources/db/query-common.sql`：

```sql
-- ============================================
-- 常用查询脚本
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

-- 10. 查看学生的申请历史
SELECT 
    ta.id,
    t.title as topic_title,
    ta.status,
    ta.match_score,
    ta.teacher_feedback,
    ta.created_at
FROM topic_application ta
JOIN topic t ON ta.topic_id = t.id
WHERE ta.student_id = 1  -- 替换为实际学生ID
ORDER BY ta.created_at DESC;
```

### 数据维护脚本

```sql
-- ============================================
-- 数据维护脚本
-- ============================================

-- 1. 重置密码为 123456（BCrypt哈希）
UPDATE user 
SET password_hash = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA' 
WHERE username = 'admin';

-- 2. 启用所有用户
UPDATE user SET status = 1;

-- 3. 关闭所有已开放的选题
UPDATE topic SET status = 'CLOSED' WHERE status = 'OPEN';

-- 4. 清理测试数据（谨慎使用！）
-- DELETE FROM notification WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
-- DELETE FROM topic_application WHERE status = 'REJECTED' AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

---

## 🔐 密码处理

### 问题：密码是加密的，如何插入用户？

**方案1：使用 API 接口（推荐）**
```bash
POST /api/admin/users
{
  "username": "student001",
  "password": "123456",  # 明文密码，后端会自动加密
  "realName": "张三",
  "role": "STUDENT"
}
```

**方案2：使用 BCrypt 在线工具**
访问 https://bcrypt-generator.com/ 生成哈希值

**方案3：使用系统提供的工具类**
```java
// 后端已提供 PasswordGenerator 工具类
// 可以生成密码哈希用于SQL插入
```

### 已知密码哈希对照表

| 明文密码 | BCrypt 哈希（强度10） |
|---------|---------------------|
| 123456 | `$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA` |
| admin123 | `$2a$10$8K1p/a0dL9Hr.ZYHz0Y.3u7dX3Y5dOqGQ5VJqR3xQ7yZ0wC1vE2` |

---

## 🛠️ 常用数据库维护命令

```sql
-- 查看数据库大小
SELECT 
    table_schema AS '数据库',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS '大小(MB)'
FROM information_schema.tables
WHERE table_schema = 'graduation_topic'
GROUP BY table_schema;

-- 查看表大小
SELECT 
    table_name AS '表名',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS '大小(MB)'
FROM information_schema.tables
WHERE table_schema = 'graduation_topic'
ORDER BY (data_length + index_length) DESC;

-- 优化表（回收空间）
OPTIMIZE TABLE user, topic, topic_application;

-- 查看索引使用情况
SHOW INDEX FROM user;

-- 分析表（更新统计信息）
ANALYZE TABLE user;
```

---

## ⚠️ 注意事项

1. **删除操作**：删除 `user` 表中的用户会级联删除相关数据（学生信息、选题、申请等）
2. **密码加密**：直接插入用户时，必须使用 BCrypt 加密的密码哈希
3. **外键约束**：删除数据时注意外键约束，先删除子表数据
4. **字符编码**：确保使用 UTF8MB4 字符集，支持 emoji 等特殊字符
5. **备份数据**：重要操作前建议先备份数据库

---

## 📚 相关文件

- 数据库结构：`backend/src/main/resources/db/schema.sql`
- 初始化数据：`backend/src/main/resources/db/init-data.sql`
- 配置文件：`backend/src/main/resources/application.yml`

---

## 💡 快速开始

1. **连接数据库**
   ```bash
   mysql -u root -p
   USE graduation_topic;
   ```

2. **查看所有用户**
   ```sql
   SELECT id, username, real_name, role, status FROM user;
   ```

3. **查看所有选题**
   ```sql
   SELECT * FROM topic;
   ```

4. **查看所有申请**
   ```sql
   SELECT * FROM topic_application;
   ```

如果需要更多帮助，请查看具体的 API 文档或联系开发团队。

