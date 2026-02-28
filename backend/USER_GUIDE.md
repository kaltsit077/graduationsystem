# 用户创建指南

由于系统没有用户注册功能，用户账号需要由管理员创建。本文档说明如何创建用户账号。

## ✨ 自动创建管理员账号（推荐）

**系统会在首次启动时自动创建默认管理员账号**，无需手动操作！

### 默认管理员账号

- **用户名**：`admin`
- **密码**：`123456`
- **真实姓名**：系统管理员

### 配置说明

管理员账号信息在 `application.yml` 中配置：

```yaml
admin:
  username: admin          # 管理员用户名
  password: 123456         # 管理员密码
  real-name: 系统管理员     # 真实姓名
  auto-create: true        # 是否自动创建（设为 false 可禁用）
```

### 工作原理

1. 应用启动时，`AdminInitializer` 会自动运行
2. 检查数据库中是否已存在指定用户名的管理员账号
3. 如果不存在，自动创建管理员账号
4. 如果已存在，跳过创建（不会覆盖现有账号）

### 禁用自动创建

如果不想自动创建管理员账号，可以在配置文件中设置：

```yaml
admin:
  auto-create: false
```

**⚠️ 注意**：禁用自动创建后，需要手动创建管理员账号才能登录系统。

---

## 方法一：使用 SQL 脚本初始化（可选，用于创建测试账号）

### 1. 执行初始化脚本

在数据库中执行 `src/main/resources/db/init-data.sql` 脚本：

```bash
# 方式1：使用 MySQL 命令行
mysql -u root -p graduation_topic < src/main/resources/db/init-data.sql

# 方式2：在 MySQL 客户端中执行
source src/main/resources/db/init-data.sql
```

### 2. 默认账号信息

执行脚本后会创建以下测试账号：

| 用户名 | 密码 | 角色 | 真实姓名 |
|--------|------|------|----------|
| admin | 123456 | 管理员 | 系统管理员 |
| teacher001 | 123456 | 导师 | 张教授 |
| teacher002 | 123456 | 导师 | 李教授 |
| student001 | 123456 | 学生 | 张三 |
| student002 | 123456 | 学生 | 李四 |
| student003 | 123456 | 学生 | 王五 |

**⚠️ 重要提示**：生产环境请务必修改默认密码！

---

## 方法二：使用管理员接口创建用户（推荐用于日常管理）

### 1. 登录管理员账号

使用管理员账号（如 `admin`）登录系统。

### 2. 调用创建用户接口

**接口地址**：`POST /api/admin/users`

**请求头**：
```
Authorization: Bearer <管理员token>
Content-Type: application/json
```

**请求体**：
```json
{
  "username": "newuser",
  "password": "password123",
  "realName": "新用户",
  "role": "STUDENT"
}
```

**角色类型**：
- `STUDENT` - 学生
- `TEACHER` - 导师
- `ADMIN` - 管理员

**响应示例**：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 7,
    "username": "newuser",
    "realName": "新用户",
    "role": "STUDENT",
    "status": 1
  }
}
```

### 3. 使用 Postman 或 curl 测试

```bash
# 示例：创建学生账号
curl -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <管理员token>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student004",
    "password": "123456",
    "realName": "赵六",
    "role": "STUDENT"
  }'
```

---

## 方法三：直接在数据库中插入（不推荐，仅用于紧急情况）

### 1. 生成密码哈希

使用 `PasswordGenerator` 工具类生成 BCrypt 密码哈希：

```bash
# 方式1：运行 Java 类
cd backend
mvn compile exec:java -Dexec.mainClass="com.example.graduation.util.PasswordGenerator" -Dexec.args="your_password"

# 方式2：在代码中调用
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("your_password");
System.out.println(hash);
```

### 2. 执行 SQL 插入

```sql
INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`) 
VALUES ('newuser', '$2a$10$生成的哈希值', '真实姓名', 'STUDENT', 1);
```

---

## 密码管理

### 生成新密码哈希

如果需要为现有用户修改密码，可以使用以下方法：

**方法1：使用 PasswordGenerator 工具类**
```java
// 运行 PasswordGenerator.main() 方法
// 或调用 PasswordGenerator.generateHash("新密码")
```

**方法2：使用 Spring Security BCrypt**
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("新密码");
```

**方法3：在线工具（不推荐用于生产环境）**
- 使用 BCrypt 在线生成器（注意安全性）

### 修改用户密码

```sql
-- 更新用户密码（需要先生成新的 BCrypt 哈希）
UPDATE `user` 
SET `password_hash` = '$2a$10$新的哈希值' 
WHERE `username` = 'username';
```

---

## 常见问题

### Q: 如何批量创建用户？

A: 可以编写 SQL 脚本批量插入，或使用管理员接口循环调用。

### Q: 忘记管理员密码怎么办？

A: 可以直接在数据库中更新密码哈希：
```sql
-- 将管理员密码重置为 123456
UPDATE `user` 
SET `password_hash` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA' 
WHERE `username` = 'admin';
```

### Q: 如何禁用用户？

A: 更新用户状态：
```sql
UPDATE `user` SET `status` = 0 WHERE `username` = 'username';
```

### Q: 如何查看所有用户？

A: 查询用户表：
```sql
SELECT id, username, real_name, role, status, created_at 
FROM `user` 
ORDER BY created_at DESC;
```

---

## 安全建议

1. **生产环境必须修改默认密码**
2. **使用强密码策略**（至少8位，包含大小写字母、数字、特殊字符）
3. **定期更换密码**
4. **不要将密码哈希硬编码在代码中**
5. **使用 HTTPS 传输敏感信息**
6. **限制管理员账号数量**
7. **记录用户创建和修改日志**

---

## 相关文件

- 数据库表结构：`src/main/resources/db/schema.sql`
- 初始化数据脚本：`src/main/resources/db/init-data.sql`
- 密码生成工具：`src/main/java/com/example/graduation/util/PasswordGenerator.java`
- 用户创建接口：`src/main/java/com/example/graduation/controller/AdminController.java`
- 用户服务：`src/main/java/com/example/graduation/service/UserService.java`

