# 数据库操作指南

## 概述

本指南介绍如何对 MySQL 数据库进行增删查改（CRUD）操作。

## 方式一：使用 Docker 命令（推荐）

### 1. 进入 MySQL 容器

```bash
docker exec -it mysql-graduation bash
```

### 2. 登录 MySQL

```bash
mysql -uroot -proot
```

### 3. 选择数据库

```sql
USE graduation_topic;
```

---

## 方式二：直接在 Windows 命令行执行（无需进入容器）

### 执行 SQL 命令

```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "你的SQL命令"
```

---

## 常用操作示例

### 查看所有表

```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SHOW TABLES;"
```

### 查看表结构

```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "DESCRIBE user;"
```

---

## 增删查改（CRUD）操作

### 1. 查询（SELECT）

#### 查询所有用户
```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT * FROM user;"
```

#### 查询特定用户
```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT * FROM user WHERE username='admin';"
```

#### 查询学生用户
```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT id, username, real_name, role, status FROM user WHERE role='STUDENT';"
```

#### 查询教师用户
```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT id, username, real_name, role, status FROM user WHERE role='TEACHER';"
```

#### 条件查询示例
```bash
# 查询启用的用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT * FROM user WHERE status=1;"

# 查询最近注册的用户（按创建时间排序）
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT * FROM user ORDER BY created_at DESC LIMIT 10;"
```

---

### 2. 插入（INSERT）

#### 插入新用户
```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "INSERT INTO user (username, password_hash, real_name, role, status) VALUES ('student001', '\$2a\$10\$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG', '张三', 'STUDENT', 1);"
```

**注意**：密码需要使用 BCrypt 加密后的哈希值。默认密码 `123456` 的哈希值是：`$2a$10$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG`

#### 插入多个用户（批量插入）
```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "INSERT INTO user (username, password_hash, real_name, role, status) VALUES ('student002', '\$2a\$10\$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG', '李四', 'STUDENT', 1), ('teacher001', '\$2a$10$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG', '王老师', 'TEACHER', 1);"
```

---

### 3. 更新（UPDATE）

#### 更新用户信息
```bash
# 更新用户真实姓名
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "UPDATE user SET real_name='新姓名' WHERE username='student001';"

# 更新用户密码（使用 BCrypt 哈希）
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "UPDATE user SET password_hash='\$2a\$10$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG' WHERE username='student001';"

# 禁用用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "UPDATE user SET status=0 WHERE username='student001';"

# 启用用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "UPDATE user SET status=1 WHERE username='student001';"
```

#### 批量更新
```bash
# 将所有学生的状态设为启用
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "UPDATE user SET status=1 WHERE role='STUDENT';"
```

---

### 4. 删除（DELETE）

#### 删除用户
```bash
# 删除特定用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "DELETE FROM user WHERE username='student001';"

# 删除所有禁用用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "DELETE FROM user WHERE status=0;"
```

**⚠️ 警告**：删除操作不可逆，请谨慎操作！

---

## 使用 SQL 文件执行复杂操作

### 1. 创建 SQL 文件

创建一个文件 `update-users.sql`：

```sql
-- 更新所有学生的真实姓名
UPDATE user SET real_name = CONCAT('学生', id) WHERE role = 'STUDENT' AND real_name IS NULL;

-- 查询更新结果
SELECT id, username, real_name, role FROM user WHERE role = 'STUDENT';
```

### 2. 执行 SQL 文件

```bash
docker exec -i mysql-graduation mysql -uroot -proot graduation_topic < update-users.sql
```

---

## 常用查询示例

### 统计信息

```bash
# 统计用户总数
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT COUNT(*) as total_users FROM user;"

# 按角色统计用户数
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT role, COUNT(*) as count FROM user GROUP BY role;"

# 统计启用和禁用的用户数
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT status, COUNT(*) as count FROM user GROUP BY status;"
```

### 复杂查询

```bash
# 查询最近注册的10个学生
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT id, username, real_name, created_at FROM user WHERE role='STUDENT' ORDER BY created_at DESC LIMIT 10;"

# 查询用户名包含特定字符的用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT * FROM user WHERE username LIKE '%123%';"
```

---

## 表结构说明

### user 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名（学号/教工号），唯一 |
| password_hash | VARCHAR(255) | 密码哈希（BCrypt） |
| real_name | VARCHAR(50) | 真实姓名 |
| role | ENUM | 角色：ADMIN, STUDENT, TEACHER |
| status | TINYINT | 状态：1=启用，0=禁用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

## 密码管理

### 生成 BCrypt 密码哈希

如果需要创建新用户或重置密码，可以使用以下方法：

#### 方法一：使用 Java 代码生成（推荐）

创建一个临时 Java 文件 `PasswordGenerator.java`：

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456"; // 你要设置的密码
        String hash = encoder.encode(password);
        System.out.println("密码: " + password);
        System.out.println("哈希: " + hash);
    }
}
```

#### 方法二：使用在线工具

访问 BCrypt 在线生成器（注意安全性，不要在生产环境使用）

#### 方法三：使用已知哈希

默认密码 `123456` 的哈希值：
```
$2a$10$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG
```

---

## 实用脚本

### 重置用户密码为 123456

```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "UPDATE user SET password_hash='\$2a\$10\$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG' WHERE username='admin';"
```

### 查看所有表的数据量

```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT 'user' as table_name, COUNT(*) as count FROM user UNION ALL SELECT 'topic', COUNT(*) FROM topic UNION ALL SELECT 'topic_application', COUNT(*) FROM topic_application;"
```

### 导出数据

```bash
# 导出整个数据库
docker exec mysql-graduation mysqldump -uroot -proot graduation_topic > backup.sql

# 导出特定表
docker exec mysql-graduation mysqldump -uroot -proot graduation_topic user > user_backup.sql
```

### 导入数据

```bash
docker exec -i mysql-graduation mysql -uroot -proot graduation_topic < backup.sql
```

---

## 注意事项

1. **密码哈希**：所有密码必须使用 BCrypt 加密，不能直接存储明文密码
2. **字符编码**：确保使用 UTF-8 编码，支持中文
3. **时间字段**：`created_at` 和 `updated_at` 会自动更新
4. **唯一约束**：`username` 字段有唯一约束，不能重复
5. **外键约束**：删除用户前，需要先删除相关的关联数据（如选题申请、通知等）

---

## 快速参考

```bash
# 查看所有用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SELECT id, username, real_name, role, status FROM user;"

# 添加新用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "INSERT INTO user (username, password_hash, real_name, role, status) VALUES ('新学号', '\$2a\$10\$p/DrTuiPqpr.JoA5/fGHBeWb.qVyt95xAdHOcL6kb6k6ickMtglhG', '姓名', 'STUDENT', 1);"

# 更新用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "UPDATE user SET real_name='新姓名' WHERE username='学号';"

# 删除用户
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "DELETE FROM user WHERE username='学号';"
```

---

## 故障排查

### 如果遇到 "Access denied" 错误

检查 MySQL 容器是否运行：
```bash
docker ps | findstr mysql
```

### 如果遇到 "Table doesn't exist" 错误

检查数据库名称是否正确：
```bash
docker exec mysql-graduation mysql -uroot -proot -e "SHOW DATABASES;"
```

### 如果遇到字符编码问题

确保使用 UTF-8：
```bash
docker exec mysql-graduation mysql -uroot -proot graduation_topic -e "SET NAMES utf8mb4;"
```

