# 快速启动指南

## 🚀 一键启动前后端服务

### Windows 系统

#### 方法 1：双击运行（最简单）

1. 双击项目根目录下的 `start-all.bat`
2. 等待服务启动完成
3. 自动打开浏览器访问 http://localhost:3000

#### 方法 2：命令行运行

```cmd
# 在项目根目录下
start-all.bat

# 或在 CMD 中
cd C:\Users\24484\Desktop\.code\menu
start-all.bat
```

#### 方法 3：PowerShell 运行

```powershell
# 在项目根目录下
.\start-all.bat

# 如果遇到执行策略问题，运行：
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\start-all.bat
```

---

## 🛑 停止服务

### 方法 1：双击运行

双击项目根目录下的 `stop-all.bat`

### 方法 2：手动停止

1. 找到窗口标题为 "Backend-Service-8080" 的窗口，按 `Ctrl+C`
2. 找到窗口标题为 "Frontend-Service-3000" 的窗口，按 `Ctrl+C`

### 方法 3：命令行停止

```cmd
stop-all.bat
```

---

## 📋 启动脚本功能说明

### `start-all.bat` 功能

1. ✅ **环境检查**：自动检测 Java、Node.js、npm、Maven
2. ✅ **智能启动**：
   - 先启动后端服务（后台运行）
   - **等待后端就绪**（检查健康检查接口 `/api/ping`）
   - 然后启动前端服务
3. ✅ **自动安装**：如果前端依赖未安装，自动运行 `npm install`
4. ✅ **状态提示**：显示启动进度和访问地址

### 启动流程

```
[步骤 1/4] 环境检查
    ↓
[步骤 2/4] 启动后端（等待就绪）
    ↓ (等待后端健康检查通过)
[步骤 3/4] 准备前端依赖
    ↓
[步骤 4/4] 启动前端
    ↓
完成！
```

---

## 🔍 访问地址

启动成功后，可以通过以下地址访问：

- **前端页面**：http://localhost:3000
- **后端API**：http://localhost:8080/api
- **健康检查**：http://localhost:8080/api/ping

---

## 🗄️ 数据库连接

### 查看数据库数据

如果遇到 "找不到 mysql 命令" 的错误，请参考：

**详细指南**：`backend/MYSQL_CONNECTION_GUIDE.md`

**快速方案**：

#### 方案 1：使用图形化工具（推荐）

1. **下载 MySQL Workbench**（免费）
   - 访问：https://dev.mysql.com/downloads/workbench/
   - 下载并安装

2. **连接数据库**
   ```
   主机: localhost
   端口: 3306
   用户: root
   密码: root
   数据库: graduation_topic
   ```

3. **查看数据**
   - 双击连接进入
   - 在左侧选择 `graduation_topic` 数据库
   - 双击表名查看数据

#### 方案 2：使用 API 接口

```bash
# 1. 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\"}"

# 2. 使用 Token 查询
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <your-token>"
```

#### 方案 3：安装 MySQL 命令行工具

1. 下载 MySQL Installer：https://dev.mysql.com/downloads/installer/
2. 安装时选择 "Add MySQL to PATH"
3. 安装后可以使用：
   ```cmd
   mysql -u root -p
   ```

---

## ⚠️ 常见问题

### Q1: 后端启动失败

**症状**：看到 "后端服务可能未完全启动" 警告

**解决方法**：
1. 检查后端窗口的日志信息
2. 确认数据库已启动且连接正常
3. 检查端口 8080 是否被占用

### Q2: 前端启动失败

**症状**：前端窗口显示错误信息

**解决方法**：
1. 检查是否已安装 Node.js 和 npm
2. 尝试手动安装依赖：`cd frontend && npm install`
3. 检查端口 3000 是否被占用

### Q3: 找不到 mysql 命令

**解决方法**：
- 参考 `backend/MYSQL_CONNECTION_GUIDE.md`
- 或使用图形化工具（MySQL Workbench）
- 或使用 API 接口查询数据

### Q4: 端口被占用

**解决方法**：
```cmd
# 查看端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :3000

# 停止占用进程（替换 PID 为实际进程ID）
taskkill /PID <PID> /F
```

---

## 📚 相关文档

- **数据库操作指南**：`backend/DATABASE_GUIDE.md`
- **MySQL 连接指南**：`backend/MYSQL_CONNECTION_GUIDE.md`
- **用户管理指南**：`backend/USER_GUIDE.md`
- **登录指南**：`backend/LOGIN_GUIDE.md`

---

## 🎯 快速命令参考

```cmd
# 启动所有服务
start-all.bat

# 停止所有服务
stop-all.bat

# 仅启动后端
cd backend
start.bat

# 仅启动前端
cd frontend
npm run dev

# 查看数据库（如果已安装 MySQL）
mysql -u root -p
USE graduation_topic;
SHOW TABLES;
```

---

## 💡 提示

1. **首次启动**：可能需要下载依赖，请耐心等待
2. **开发模式**：后端和前端会在各自的窗口中显示日志
3. **修改代码**：前端支持热重载，后端需要重启
4. **数据库**：首次使用前需要创建数据库并执行 `schema.sql`

如有问题，请查看详细文档或检查日志输出。



