# 生产环境部署指南

## ⚠️ 重要提示

**部署到公网前，必须完成所有安全检查！**

请参考 [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) 完成所有安全检查项。

---

## 🚀 快速部署步骤

### 1. 准备服务器

确保服务器已安装：
- Docker 20.10+
- Docker Compose 2.0+
- 至少 2GB 内存
- 至少 10GB 磁盘空间

### 2. 上传项目文件

```bash
# 使用 git 克隆或上传项目文件到服务器
git clone <your-repo-url>
cd menu
```

### 3. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，修改所有密码和密钥
nano .env  # 或使用 vim/vi
```

**必须修改的配置：**
- `MYSQL_ROOT_PASSWORD` - 数据库 root 密码（强密码）
- `MYSQL_PASSWORD` - 数据库用户密码（强密码）
- `DB_PASSWORD` - 后端连接数据库密码（强密码）
- `JWT_SECRET` - JWT 密钥（64 字节 Base64 编码）
- `ADMIN_PASSWORD` - 管理员密码（强密码）

**生成 JWT 密钥：**
```bash
# Linux/Mac
openssl rand -base64 64

# Windows PowerShell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

### 4. 修改 CORS 配置（重要）

编辑 `backend/src/main/java/com/example/graduation/config/CorsConfig.java`：

```java
// 将这一行：
config.addAllowedOriginPattern("*");

// 改为你的域名：
config.addAllowedOrigin("https://yourdomain.com");
// 如果有多个域名，可以添加多个
config.addAllowedOrigin("https://www.yourdomain.com");
```

### 5. 构建和启动

```bash
# 使用生产环境配置构建
docker-compose -f docker-compose.prod.yml build

# 启动所有服务
docker-compose -f docker-compose.prod.yml up -d

# 查看服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f
```

### 6. 配置 Nginx 反向代理（推荐）

创建 Nginx 配置文件 `/etc/nginx/sites-available/graduation`：

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # 重定向到 HTTPS（如果配置了 SSL）
    # return 301 https://$server_name$request_uri;

    # 前端
    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 后端 API
    location /api {
        proxy_pass http://localhost:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # CORS 配置（如果后端已配置，这里可以省略）
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods 'GET, POST, PUT, DELETE, OPTIONS';
        add_header Access-Control-Allow-Headers 'Authorization, Content-Type';
    }
}

# HTTPS 配置（使用 Let's Encrypt）
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # SSL 配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 前端
    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 后端 API
    location /api {
        proxy_pass http://localhost:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置：
```bash
sudo ln -s /etc/nginx/sites-available/graduation /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 7. 配置 SSL 证书（HTTPS）

使用 Let's Encrypt 免费证书：

```bash
# 安装 certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# 自动续期（已自动配置）
```

### 8. 配置防火墙

```bash
# Ubuntu/Debian (ufw)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# CentOS (firewalld)
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 9. 配置数据库备份

创建备份脚本 `backup-db.sh`：

```bash
#!/bin/bash
BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
docker exec mysql-graduation mysqldump -u root -p${MYSQL_ROOT_PASSWORD} graduation_topic > ${BACKUP_DIR}/backup_${DATE}.sql
# 保留最近 7 天的备份
find ${BACKUP_DIR} -name "backup_*.sql" -mtime +7 -delete
```

添加到 crontab（每天凌晨 2 点备份）：
```bash
crontab -e
# 添加：
0 2 * * * /path/to/backup-db.sh
```

---

## 🔍 验证部署

1. **访问前端**: http://yourdomain.com
2. **测试登录**: 使用管理员账号登录
3. **测试注册**: 注册新用户
4. **检查 API**: http://yourdomain.com/api/ping
5. **检查日志**: `docker-compose -f docker-compose.prod.yml logs -f`

---

## 📊 监控和维护

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
docker-compose -f docker-compose.prod.yml logs -f mysql
```

### 重启服务
```bash
docker-compose -f docker-compose.prod.yml restart
```

### 更新服务
```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose -f docker-compose.prod.yml up -d --build
```

---

## ⚠️ 常见问题

### 1. 服务无法启动
- 检查 `.env` 文件是否配置正确
- 查看日志：`docker-compose -f docker-compose.prod.yml logs`
- 检查端口是否被占用

### 2. 数据库连接失败
- 检查数据库容器是否运行
- 检查数据库密码是否正确
- 检查网络连接

### 3. 前端无法访问后端
- 检查 CORS 配置
- 检查 Nginx 配置
- 检查防火墙设置

---

## 🔒 安全建议

1. **定期更新**: 保持 Docker 镜像和系统更新
2. **监控日志**: 定期检查应用和系统日志
3. **备份数据**: 每天自动备份数据库
4. **限制访问**: 使用防火墙限制不必要的端口
5. **使用 HTTPS**: 生产环境必须使用 HTTPS
6. **强密码**: 所有密码至少 12 位，包含大小写字母、数字和特殊字符

---

**部署完成后，请删除或保护 `.env` 文件，不要提交到 Git！**

