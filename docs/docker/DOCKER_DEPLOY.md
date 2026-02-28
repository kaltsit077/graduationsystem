# Docker 部署指南

本文档说明如何使用 Docker 部署毕业论文选题系统。

---

## 📋 目录

1. [前置要求](#前置要求)
2. [快速开始](#快速开始)
3. [开发环境部署](#开发环境部署)
4. [生产环境部署](#生产环境部署)
5. [常用命令](#常用命令)
6. [故障排查](#故障排查)

---

## 🔧 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 2GB 可用内存
- 至少 5GB 可用磁盘空间

---

## 🚀 快速开始

### 一键启动所有服务

```bash
# 构建并启动所有服务（数据库 + 后端 + 前端）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 访问服务

- **前端**: http://localhost
- **后端API**: http://localhost:9090/api
- **健康检查**: http://localhost:9090/api/ping

### 默认账号

- **用户名**: `admin`
- **密码**: `123456`

---

## 💻 开发环境部署

### 仅启动数据库（推荐开发使用）

```bash
# 启动数据库服务
docker-compose -f docker-compose.dev.yml up -d

# 后端和前端在本地运行（使用 start.bat）
# 数据库连接配置会自动使用 Docker 中的 MySQL
```

### 验证数据库连接

```bash
# 查看数据库容器状态
docker ps | findstr mysql

# 进入数据库
docker exec -it mysql-graduation mysql -u root -proot graduation_topic

# 查看表
SHOW TABLES;
```

---

## 🏭 生产环境部署

### 1. 配置环境变量

创建 `.env` 文件：

```env
# 数据库配置
DB_HOST=mysql
DB_PORT=3306
DB_NAME=graduation_topic
DB_USER=root
DB_PASSWORD=your-secure-password

# JWT 配置
JWT_SECRET=your-very-secure-secret-key-change-this
JWT_EXPIRATION=120

# 管理员配置
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your-secure-admin-password
ADMIN_REAL_NAME=系统管理员
ADMIN_AUTO_CREATE=true
```

### 2. 构建镜像

```bash
# 构建所有镜像
docker-compose build

# 或单独构建
docker-compose build backend
docker-compose build frontend
```

### 3. 启动服务

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
```

### 4. 配置 Nginx 反向代理（可选）

如果需要使用域名和 HTTPS，可以配置 Nginx：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端
    location / {
        proxy_pass http://frontend:80;
    }

    # 后端 API
    location /api {
        proxy_pass http://backend:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 📝 常用命令

### 服务管理

```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f [service_name]
```

### 数据库操作

```bash
# 进入数据库
docker exec -it mysql-graduation mysql -u root -proot graduation_topic

# 执行 SQL 脚本
Get-Content backend\src\main\resources\db\update-admin-password.sql | docker exec -i mysql-graduation mysql -u root -proot graduation_topic

# 备份数据库
docker exec mysql-graduation mysqldump -u root -proot graduation_topic > backup.sql

# 恢复数据库
docker exec -i mysql-graduation mysql -u root -proot graduation_topic < backup.sql
```

### 镜像管理

```bash
# 查看镜像
docker images

# 删除未使用的镜像
docker image prune -a

# 重新构建镜像（不缓存）
docker-compose build --no-cache
```

### 数据管理

```bash
# 查看数据卷
docker volume ls

# 删除数据卷（会删除所有数据！）
docker volume rm menu_mysql_data

# 备份数据卷
docker run --rm -v menu_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql_backup.tar.gz /data
```

---

## 🔍 故障排查

### 服务无法启动

```bash
# 查看服务日志
docker-compose logs [service_name]

# 查看容器状态
docker ps -a

# 检查端口占用
netstat -ano | findstr "3306\|9090\|80"
```

### 数据库连接失败

```bash
# 检查数据库容器是否运行
docker ps | findstr mysql

# 检查数据库日志
docker logs mysql-graduation

# 测试数据库连接
docker exec -it mysql-graduation mysql -u root -proot -e "SELECT 1;"
```

### 后端启动失败

```bash
# 查看后端日志
docker-compose logs backend

# 检查环境变量
docker-compose config

# 进入容器调试
docker exec -it graduation-backend sh
```

### 前端无法访问后端

```bash
# 检查网络连接
docker network ls
docker network inspect menu_graduation-network

# 检查服务健康状态
curl http://localhost:9090/api/ping
```

---

## 📊 性能优化

### 数据库优化

```yaml
# docker-compose.yml 中添加 MySQL 配置
command:
  - --innodb-buffer-pool-size=512M
  - --max-connections=200
```

### 后端优化

```yaml
# 调整 JVM 参数
environment:
  JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
```

---

## 🔒 安全建议

1. **修改默认密码**: 生产环境必须修改所有默认密码
2. **使用环境变量**: 敏感信息通过 `.env` 文件管理
3. **限制网络访问**: 只暴露必要的端口
4. **定期更新镜像**: 保持 Docker 镜像最新
5. **数据备份**: 定期备份数据库

---

## 📚 相关文档

- [数据库操作指南](DATABASE_GUIDE.md)
- [Docker SQL 命令](DOCKER_SQL_COMMANDS.md)
- [部署文档](backend/DEPLOY.md)

---

**最后更新**: 2025-01-XX


