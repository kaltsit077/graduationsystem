# 部署文档

本文档说明如何将毕业论文选题系统部署到生产环境（公网服务器）。

## 目录

1. [前置要求](#前置要求)
2. [数据库准备](#数据库准备)
3. [后端部署](#后端部署)
4. [Nginx 反向代理配置](#nginx-反向代理配置)
5. [环境变量配置](#环境变量配置)
6. [SSL 证书配置（HTTPS）](#ssl-证书配置https)
7. [防火墙配置](#防火墙配置)
8. [系统服务配置（Linux）](#系统服务配置linux)
9. [常见问题](#常见问题)

---

## 前置要求

### 服务器要求

- **操作系统**：Linux（CentOS 7+/Ubuntu 18+）或 Windows Server
- **Java**：JDK 17 或更高版本
- **数据库**：MySQL 5.7+ 或 8.0+
- **Web 服务器**：Nginx（推荐）或 Apache（用于反向代理）

### 软件安装

#### Linux

```bash
# 安装 JDK 17
sudo yum install java-17-openjdk java-17-openjdk-devel  # CentOS
# 或
sudo apt install openjdk-17-jdk  # Ubuntu

# 安装 MySQL
sudo yum install mysql-server  # CentOS
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 安装 Nginx
sudo yum install nginx  # CentOS
# 或
sudo apt install nginx  # Ubuntu
```

#### Windows

- 下载并安装 JDK 17：https://adoptium.net/
- 下载并安装 MySQL：https://dev.mysql.com/downloads/mysql/
- 下载并安装 Nginx：http://nginx.org/en/download.html

---

## 数据库准备

### 1. 创建数据库

```sql
CREATE DATABASE graduation_topic DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 创建数据库用户（推荐）

```sql
CREATE USER 'graduation_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON graduation_topic.* TO 'graduation_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 执行数据库迁移脚本

（后续会提供 SQL 建表脚本，此处预留）

---

## 后端部署

### 方式一：使用 JAR 包部署（推荐）

#### 1. 打包应用

在项目根目录执行：

```bash
cd backend
mvn clean package -DskipTests
```

生成的 JAR 文件位于：`backend/target/graduation-topic-backend-0.0.1-SNAPSHOT.jar`

#### 2. 上传到服务器

使用 `scp` 或 `FTP` 工具上传 JAR 文件到服务器，例如：

```bash
scp backend/target/graduation-topic-backend-0.0.1-SNAPSHOT.jar user@your-server:/opt/graduation/
```

#### 3. 创建部署目录结构

```bash
mkdir -p /opt/graduation/{logs,config}
```

#### 4. 配置环境变量

创建配置文件 `/opt/graduation/config/application-prod.yml`，或使用环境变量（见下方“环境变量配置”章节）。

#### 5. 启动应用

**Linux 方式（使用 systemd 服务，见下方“系统服务配置”）**：

```bash
java -jar -Dspring.profiles.active=prod \
  -DDB_HOST=localhost \
  -DDB_NAME=graduation_topic \
  -DDB_USER=graduation_user \
  -DDB_PASSWORD=your_password \
  -DJWT_SECRET=your-jwt-secret-key \
  /opt/graduation/graduation-topic-backend-0.0.1-SNAPSHOT.jar
```

**Windows 方式**：

使用提供的 `start.bat` 脚本，或手动执行：

```cmd
java -jar -Dspring.profiles.active=prod graduation-topic-backend-0.0.1-SNAPSHOT.jar
```

### 方式二：使用 Docker（可选，后续提供）

---

## Nginx 反向代理配置

### 1. 复制配置示例

将 `backend/nginx.conf.example` 复制到 Nginx 配置目录：

**Linux**：
```bash
sudo cp backend/nginx.conf.example /etc/nginx/sites-available/graduation
sudo ln -s /etc/nginx/sites-available/graduation /etc/nginx/sites-enabled/
```

**Windows**：
复制到 Nginx 安装目录的 `conf/` 文件夹，并在主配置文件中引入。

### 2. 修改配置

编辑配置文件，修改以下内容：

- `server_name`：改为你的域名
- `upstream graduation_backend`：确认后端地址和端口
- 前端静态文件路径（如果有前端）

### 3. 测试并重载配置

```bash
# 测试配置语法
sudo nginx -t

# 重载配置
sudo nginx -s reload
```

---

## 环境变量配置

生产环境建议使用环境变量而非硬编码密码。可以通过以下方式设置：

### Linux（使用 systemd 服务）

在 systemd 服务文件中设置（见下方“系统服务配置”）。

### Windows

在系统环境变量中设置，或在启动脚本中设置：

```cmd
set DB_HOST=localhost
set DB_NAME=graduation_topic
set DB_USER=graduation_user
set DB_PASSWORD=your_password
set JWT_SECRET=your-jwt-secret-key
```

### 环境变量列表

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `DB_HOST` | 数据库主机 | `localhost` |
| `DB_PORT` | 数据库端口 | `3306` |
| `DB_NAME` | 数据库名 | `graduation_topic` |
| `DB_USER` | 数据库用户名 | `graduation_user` |
| `DB_PASSWORD` | 数据库密码 | `your_password` |
| `JWT_SECRET` | JWT 密钥（必须修改） | `随机生成的密钥` |
| `JWT_EXPIRATION` | JWT 过期时间（分钟） | `120` |
| `ALLOWED_ORIGINS` | CORS 允许的域名 | `https://your-domain.com` |

---

## SSL 证书配置（HTTPS）

### 使用 Let's Encrypt（免费证书，推荐）

```bash
# 安装 certbot
sudo yum install certbot python3-certbot-nginx  # CentOS
# 或
sudo apt install certbot python3-certbot-nginx  # Ubuntu

# 申请证书
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# 自动续期（已自动配置）
sudo certbot renew --dry-run
```

### 使用商业证书

1. 购买 SSL 证书
2. 将证书文件上传到服务器（如 `/etc/nginx/ssl/`）
3. 在 Nginx 配置中配置证书路径：

```nginx
ssl_certificate /etc/nginx/ssl/cert.pem;
ssl_certificate_key /etc/nginx/ssl/key.pem;
```

---

## 防火墙配置

### Linux（firewalld）

```bash
# 开放 HTTP 和 HTTPS
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### Linux（ufw）

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw reload
```

**注意**：不要直接开放 8080 端口，只通过 Nginx（80/443）访问。

---

## 系统服务配置（Linux）

### 创建 systemd 服务文件

创建 `/etc/systemd/system/graduation-backend.service`：

```ini
[Unit]
Description=Graduation Topic Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=your-user
WorkingDirectory=/opt/graduation
ExecStart=/usr/bin/java -jar \
  -Dspring.profiles.active=prod \
  -DDB_HOST=localhost \
  -DDB_NAME=graduation_topic \
  -DDB_USER=graduation_user \
  -DDB_PASSWORD=your_password \
  -DJWT_SECRET=your-jwt-secret-key \
  /opt/graduation/graduation-topic-backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### 使用服务

```bash
# 重载 systemd 配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start graduation-backend

# 设置开机自启
sudo systemctl enable graduation-backend

# 查看状态
sudo systemctl status graduation-backend

# 查看日志
sudo journalctl -u graduation-backend -f
```

---

## 常见问题

### 1. 服务无法启动

- 检查 Java 版本：`java -version`
- 检查端口占用：`netstat -tlnp | grep 8080`
- 查看日志：`tail -f logs/graduation-backend.log`

### 2. 数据库连接失败

- 确认数据库服务运行：`systemctl status mysqld`
- 检查数据库用户名、密码、数据库名
- 检查防火墙是否允许数据库端口访问

### 3. Nginx 502 Bad Gateway

- 检查后端服务是否运行：`curl http://localhost:8080/api/ping`
- 检查 Nginx 错误日志：`tail -f /var/log/nginx/error.log`
- 确认 upstream 配置的后端地址正确

### 4. 文件上传失败

- 检查文件大小是否超过 200MB
- 检查 Nginx 的 `client_max_body_size` 配置
- 检查后端 `spring.servlet.multipart` 配置

### 5. JWT 密钥安全性

**重要**：生产环境必须修改 `JWT_SECRET`，使用随机生成的强密钥：

```bash
# Linux 生成随机密钥
openssl rand -base64 32
```

---

## 下一步

- 配置前端静态文件部署（后续补充）
- 配置日志轮转和监控
- 配置数据库备份策略
- 配置 HTTPS 强制跳转

