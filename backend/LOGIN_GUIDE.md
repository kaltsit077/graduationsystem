# 登录指南

## 🔐 默认管理员账号

系统会在**首次启动时自动创建**默认管理员账号：

- **用户名**：`admin`
- **密码**：`123456`
- **角色**：管理员（ADMIN）
- **真实姓名**：系统管理员

## 📋 登录流程

### 1. 前端登录页面
- 访问：http://localhost:3000
- 输入用户名和密码
- 点击"登录"按钮

### 2. 后端登录接口
- **接口地址**：`POST /api/auth/login`
- **请求体**：
  ```json
  {
    "username": "admin",
    "password": "123456"
  }
  ```

### 3. 登录验证流程

```
前端提交登录请求
  ↓
后端 AuthController.login()
  ↓
AuthService.login()
  ↓
1. 查询用户（根据用户名）
  ↓
2. 验证密码（BCrypt 加密比对）
  ↓
3. 检查账号状态（是否禁用）
  ↓
4. 生成 JWT Token
  ↓
5. 返回登录响应（token, role, realName, userId）
  ↓
前端存储 Token 到 localStorage
  ↓
根据角色跳转到对应页面
```

### 4. 登录响应

成功响应示例：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "role": "ADMIN",
    "realName": "系统管理员",
    "userId": 1
  }
}
```

## 🔑 账号管理

### 自动创建管理员账号

系统启动时，`AdminInitializer` 会自动运行：

1. 检查数据库中是否已存在 `admin` 用户
2. 如果不存在，自动创建管理员账号
3. 如果已存在，跳过创建（不会覆盖）

**配置位置**：`application.yml`
```yaml
admin:
  username: admin
  password: 123456
  real-name: 系统管理员
  auto-create: true
```

### 创建其他用户

#### 方法一：使用管理员接口（推荐）

1. 使用管理员账号登录
2. 调用接口：`POST /api/admin/users`
   ```json
   {
     "username": "student001",
     "password": "123456",
     "realName": "张三",
     "role": "STUDENT"
   }
   ```

#### 方法二：执行 SQL 脚本

执行 `backend/src/main/resources/db/init-data.sql` 创建测试账号。

## 🛡️ 安全机制

### 密码加密
- 使用 **BCrypt** 算法加密
- 强度：10（默认）
- 密码哈希存储在 `user.password_hash` 字段

### JWT Token
- **算法**：HMAC-SHA512
- **过期时间**：120分钟（可在配置中修改）
- **包含信息**：userId, username, role
- **存储位置**：前端 localStorage

### 认证流程
1. 登录成功后，前端将 Token 存储到 localStorage
2. 后续请求在 Header 中携带：`Authorization: Bearer <token>`
3. 后端 `JwtAuthenticationFilter` 拦截请求，验证 Token
4. 验证通过后，将用户信息设置到 SecurityContext

## 📝 常见问题

### Q: 忘记管理员密码怎么办？

A: 可以通过以下方式重置：

**方法1：修改配置文件后重启**
```yaml
# application.yml
admin:
  password: newpassword  # 修改为新密码
  auto-create: true      # 确保启用自动创建
```
重启后，如果账号不存在会重新创建。

**方法2：直接在数据库中更新**
```sql
-- 需要先使用 PasswordGenerator 生成新密码的 BCrypt 哈希
UPDATE `user` 
SET `password_hash` = '$2a$10$新的哈希值' 
WHERE `username` = 'admin';
```

### Q: 如何修改 JWT Token 过期时间？

A: 在 `application.yml` 中修改：
```yaml
jwt:
  expiration-minutes: 120  # 修改为所需分钟数
```

### Q: 登录后 Token 存储在哪里？

A: 前端使用 localStorage 存储：
- `token` - JWT Token
- `role` - 用户角色
- `realName` - 真实姓名
- `userId` - 用户ID

### Q: 如何查看当前登录用户信息？

A: 前端可以通过 `useAuthStore()` 获取：
```typescript
const authStore = useAuthStore()
console.log(authStore.token)      // Token
console.log(authStore.role)       // 角色
console.log(authStore.realName)   // 真实姓名
console.log(authStore.userId)     // 用户ID
```

## 🔧 故障排查

### 问题1：登录失败，提示"用户名或密码错误"

**可能原因**：
1. 用户名不存在
2. 密码错误
3. 账号被禁用（status=0）

**解决方法**：
1. 检查数据库中是否存在该用户
2. 确认密码是否正确
3. 检查用户状态：`SELECT * FROM user WHERE username = 'admin';`

### 问题2：登录成功但无法访问页面

**可能原因**：
1. Token 过期
2. Token 格式错误
3. 角色权限不足

**解决方法**：
1. 重新登录获取新 Token
2. 检查请求头格式：`Authorization: Bearer <token>`
3. 确认用户角色是否有访问权限

### 问题3：后端启动后没有自动创建管理员

**可能原因**：
1. 数据库中已存在 admin 用户
2. 自动创建被禁用（`admin.auto-create=false`）
3. 数据库连接失败

**解决方法**：
1. 检查数据库连接配置
2. 查看启动日志，确认 AdminInitializer 是否运行
3. 手动执行 SQL 创建管理员账号

---

## 相关文件

- 登录接口：`backend/src/main/java/com/example/graduation/controller/AuthController.java`
- 登录服务：`backend/src/main/java/com/example/graduation/service/AuthService.java`
- JWT工具：`backend/src/main/java/com/example/graduation/util/JwtUtil.java`
- 管理员初始化：`backend/src/main/java/com/example/graduation/config/AdminInitializer.java`
- 前端登录页面：`frontend/src/views/Login.vue`
- 前端认证API：`frontend/src/api/auth.ts`

