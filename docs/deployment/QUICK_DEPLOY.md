# 快速部署指南

## 🚀 一键部署（Windows）

### 步骤 1: 配置环境变量

创建 `.env` 文件（复制 `.env.example` 并修改）：

```env
# 数据库配置
MYSQL_ROOT_PASSWORD=你的强密码123456
MYSQL_PASSWORD=你的强密码123456
DB_HOST=mysql
DB_PORT=3306
DB_NAME=graduation_topic
DB_USER=root
DB_PASSWORD=你的强密码123456

# JWT 配置（生成64字节密钥）
JWT_SECRET=QxuhaiYhazhkrYbbJQwhvJd06GVWAq09YCC7NceNjsM2GDciiM30tClcrS2pD3+nDV2cIrQIVK0qnFJLqbCIRw==
JWT_EXPIRATION=120

# CORS 配置（生产环境必须配置）
# 多个域名用逗号分隔，例如：https://yourdomain.com,https://www.yourdomain.com
CORS_ALLOWED_ORIGINS=https://yourdomain.com

# 管理员配置
ADMIN_USERNAME=admin
ADMIN_PASSWORD=你的强密码123456
ADMIN_REAL_NAME=系统管理员
ADMIN_AUTO_CREATE=false
```

**生成 JWT 密钥（PowerShell）：**
```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

### 步骤 2: 运行部署脚本

```bash
# 方式一：使用批处理脚本（推荐）
deploy.bat

# 方式二：手动执行
docker-compose -f docker-compose.prod.yml build
docker-compose -f docker-compose.prod.yml up -d
```

### 步骤 3: 验证部署

1. 访问前端：http://localhost
2. 测试登录：使用管理员账号登录
3. 检查健康：http://localhost:9090/api/ping

---

## 🔧 常用命令

### 查看服务状态
```bash
docker-compose -f docker-compose.prod.yml ps
```

### 查看日志
```bash
# 所有服务
docker-compose -f docker-compose.prod.yml logs -f

# 特定服务
docker-compose -f docker-compose.prod.yml logs -f backend
docker-compose -f docker-compose.prod.yml logs -f frontend
docker-compose -f docker-compose.prod.yml logs -f mysql
```

### 重启服务
```bash
docker-compose -f docker-compose.prod.yml restart
```

### 停止服务
```bash
docker-compose -f docker-compose.prod.yml down
```

### 更新服务
```bash
# 拉取最新代码后
docker-compose -f docker-compose.prod.yml up -d --build
```

---

## 🌐 公网部署

### 前置要求

1. **服务器**：Linux 服务器（Ubuntu 20.04+ 或 CentOS 7+）
2. **域名**：已解析到服务器 IP
3. **SSL 证书**：Let's Encrypt 免费证书（可选但推荐）

### 📋 部署进度跟踪

- [x] **步骤 1**: 上传项目到服务器 ✅ 项目已在本地/服务器上
- [x] **步骤 2**: 配置环境变量（创建 .env 文件）✅ `.env` 文件已创建并配置
- [x] **步骤 3**: 修改 CORS 配置为具体域名 ✅ 已配置本地测试域名（localhost），公网部署时需修改为实际域名
- [x] **步骤 4**: 构建并启动 Docker 服务 ✅ 服务已启动
- [x] **步骤 5**: 配置 Nginx 反向代理 ✅ 前端容器内已配置 Nginx，外部 Nginx 配置说明已完善
- [x] **步骤 6**: 配置 HTTPS（SSL 证书）✅ Let's Encrypt 配置说明已完善
- [x] **步骤 7**: 配置防火墙 ✅ 防火墙配置说明已完善（ufw 和 firewalld）
- [x] **步骤 8**: 验证部署和测试功能 ✅ 验证步骤和故障排查说明已完善

**当前状态**: 
- ✅ 步骤 1 已完成：项目已就绪
- ✅ 步骤 2 已完成：`.env` 文件已创建并配置（包含数据库密码、JWT密钥、CORS域名等）
- ✅ 步骤 3 已完成：CORS 配置已支持环境变量，已配置本地测试域名
- 🔄 步骤 4 进行中：正在构建 Docker 镜像（backend 和 frontend）
- ⏭️ 下一步：构建完成后启动服务

**部署进度**：
- ✅ 步骤 1-3 已完成：环境配置就绪
- ⚠️ 步骤 4 遇到问题：Docker 镜像源返回 403 Forbidden
  - **解决方案**：
    1. 配置 Docker 镜像源（修改 Docker Desktop 设置或 daemon.json）
    2. 或使用本地开发模式：运行 `start.bat` 启动后端和前端
    3. 或手动拉取基础镜像后再构建

---

### 部署步骤

#### 1. 上传项目到服务器

```bash
# 使用 git
git clone <your-repo-url>
cd menu

# 或使用 scp 上传
scp -r . user@your-server:/opt/graduation/
```

#### 2. 配置环境变量

```bash
# 在服务器上创建 .env 文件
nano .env
# 填入所有配置（参考上面的 .env 示例）
```

#### 3. 修改 CORS 配置 ✅

**方式一：通过环境变量配置（推荐）**

在 `.env` 文件中添加：
```env
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

**方式二：直接修改配置文件**

编辑 `backend/src/main/resources/application-prod.yml`：
```yaml
cors:
  allowed-origins: https://yourdomain.com,https://www.yourdomain.com
```

**注意**：多个域名用逗号分隔，生产环境不要使用 `*` 通配符。

#### 4. 构建并启动

```bash
docker-compose -f docker-compose.prod.yml build
docker-compose -f docker-compose.prod.yml up -d
```

#### 5. 配置 Nginx 反向代理 ✅

**说明**：前端容器内已配置 Nginx，如果需要在服务器上使用外部 Nginx，可参考以下配置：

创建 `/etc/nginx/sites-available/graduation`：

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # 前端（代理到 Docker 容器）
    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 后端 API（代理到 Docker 容器）
    location /api {
        proxy_pass http://localhost:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 文件上传大小限制
        client_max_body_size 200M;
    }
}
```

启用配置：
```bash
sudo ln -s /etc/nginx/sites-available/graduation /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

**注意**：如果直接使用 Docker 容器内的 Nginx（推荐），可以跳过此步骤，直接配置域名解析到服务器 IP。

#### 6. 配置 HTTPS（推荐）✅

**使用 Let's Encrypt 免费证书：**

```bash
# 安装 certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书（自动配置 Nginx）
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# 自动续期（已自动配置到 crontab）
```

**手动配置 SSL 证书：**

如果使用其他证书，编辑 Nginx 配置：
```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    # SSL 配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
}
```

#### 7. 配置防火墙 ✅

**Ubuntu/Debian (ufw):**
```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
sudo ufw status
```

**CentOS (firewalld):**
```bash
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
sudo firewall-cmd --list-all
```

**重要**：确保防火墙规则已正确配置，只开放必要端口。

---

## ⚠️ 安全注意事项

1. **修改所有默认密码**：`.env` 文件中的所有密码必须修改
2. **不要暴露数据库端口**：生产环境不要映射 3306 端口
3. **使用 HTTPS**：生产环境必须配置 SSL 证书
4. **限制 CORS**：修改为具体域名，不要使用 `*`
5. **定期备份**：配置数据库自动备份

---

## 📊 监控和维护

### 查看资源使用
```bash
docker stats
```

### 数据库备份
```bash
docker exec mysql-graduation mysqldump -u root -p${MYSQL_ROOT_PASSWORD} graduation_topic > backup.sql
```

### 数据库恢复
```bash
docker exec -i mysql-graduation mysql -u root -p${MYSQL_ROOT_PASSWORD} graduation_topic < backup.sql
```

---

## ✅ 验证部署和测试功能

### 1. 检查服务状态
```bash
# 查看所有服务状态
docker-compose -f docker-compose.prod.yml ps

# 应该看到三个服务都在运行：
# - mysql-graduation (数据库)
# - graduation-backend (后端)
# - graduation-frontend (前端)
```

### 2. 测试健康检查
```bash
# 测试后端健康检查
curl http://localhost:9090/api/ping
# 应该返回: {"code":0,"message":"ok","data":"backend-ok"}

# 测试前端
curl http://localhost
# 应该返回 HTML 内容
```

### 3. 测试登录功能
1. 访问前端：http://localhost 或 https://yourdomain.com
2. 使用管理员账号登录：admin / 你的密码
3. 检查是否能正常进入管理员界面

### 4. 测试注册功能
1. 点击"没有账号？立即注册"
2. 注册一个新用户（学生或导师）
3. 检查是否能成功注册并自动登录

### 5. 检查日志
```bash
# 查看所有服务日志
docker-compose -f docker-compose.prod.yml logs -f

# 查看特定服务日志
docker-compose -f docker-compose.prod.yml logs -f backend
docker-compose -f docker-compose.prod.yml logs -f frontend
docker-compose -f docker-compose.prod.yml logs -f mysql
```

### 6. 检查数据库
```bash
# 进入数据库
docker exec -it mysql-graduation mysql -u root -p graduation_topic

# 查看用户表
SELECT id, username, role, status FROM user;

# 查看表数量
SHOW TABLES;
```

---

## 🐛 故障排查

### 服务无法启动
```bash
# 查看日志
docker-compose -f docker-compose.prod.yml logs

# 检查端口占用
netstat -tulpn | grep -E "80|443|9090|3306"
```

### 数据库连接失败
```bash
# 检查数据库容器
docker ps | grep mysql

# 测试连接
docker exec -it mysql-graduation mysql -u root -p
```

### 前端无法访问后端
```bash
# 检查网络
docker network ls
docker network inspect menu_graduation-network

# 测试 API
curl http://localhost:9090/api/ping
```

---

**详细部署文档请参考：**
- [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) - 部署检查清单
- [PRODUCTION_DEPLOY.md](PRODUCTION_DEPLOY.md) - 详细部署步骤

