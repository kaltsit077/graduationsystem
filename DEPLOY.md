# 公网部署指南

本项目为 **Vue 3 前端 + Spring Boot 后端 + MySQL**，已提供生产用 Docker 配置。按以下步骤可在公网服务器上部署。

---

## 一、部署前准备

### 1. 服务器要求

- **系统**：Linux（推荐 Ubuntu 22.04 / Debian 12 / CentOS 7+）
- **配置**：至少 2 核 CPU、2GB 内存、20GB 磁盘
- **软件**：已安装 Docker 与 Docker Compose

```bash
# 安装 Docker（Ubuntu/Debian 示例）
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 安装 Docker Compose 插件
sudo apt install docker-compose-plugin
```

### 2. 域名与端口

- 准备一个**域名**（如 `menu.yourdomain.com`），并做 DNS 解析到服务器公网 IP。
- 服务器**开放端口**：80（HTTP），若启用 HTTPS 再开放 443。

---

## 二、部署步骤

### 步骤 1：把代码放到服务器

在服务器上拉取或上传项目代码，例如：

```bash
# 若使用 Git
git clone <你的仓库地址> menu
cd menu
```

或在本机打包后上传：`frontend`、`backend`、`docker-compose.prod.yml`、`.env` 等（见下文）。

### 步骤 2：配置生产环境变量

在项目**根目录**创建 `.env` 文件（可复制 `.env.example` 再修改）：

```bash
cp .env.example .env
# 编辑 .env，填入下面各项
```

**.env 必须修改的项：**

| 变量 | 说明 | 示例 |
|------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码（强密码） | 随机 16 位以上 |
| `MYSQL_PASSWORD` | 业务库用户密码 | 同上 |
| `DB_PASSWORD` | 后端连接数据库的密码 | 与 MYSQL_PASSWORD 一致 |
| `JWT_SECRET` | JWT 签名密钥，至少 64 字节 Base64 | 见下方生成方式 |
| `ADMIN_PASSWORD` | 管理员登录密码 | 强密码 |
| `CORS_ALLOWED_ORIGINS` | 允许的前端域名（公网必配） | `https://menu.yourdomain.com` |

生成 JWT_SECRET（64 字节 Base64）：

```bash
# Linux/Mac
openssl rand -base64 64
```

```powershell
# Windows PowerShell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

**CORS 示例**（替换为你的实际域名）：

```env
# 仅 HTTP
CORS_ALLOWED_ORIGINS=https://menu.yourdomain.com,http://menu.yourdomain.com

# 若暂时用 IP 访问
CORS_ALLOWED_ORIGINS=http://你的服务器IP
```

### 步骤 3：使用生产 Compose 启动

在项目根目录执行：

```bash
docker compose -f docker-compose.prod.yml up -d
```

首次会构建镜像并拉取 MySQL，可能需要几分钟。查看运行状态：

```bash
docker compose -f docker-compose.prod.yml ps
```

### 步骤 4：验证访问

- **HTTP**：浏览器访问 `http://你的域名或IP`，应能打开前端并登录。
- 前端请求会走 `/api`，由容器内 Nginx 转发到后端，无需在公网暴露 9090、3306。

---

## 三、可选：HTTPS（推荐公网必做）

生产环境建议用 Nginx + Let’s Encrypt 做 HTTPS，在**宿主机**操作，而不是改容器内 Nginx。

### 1. 宿主机安装 Nginx 与 certbot

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nginx certbot python3-certbot-nginx
```

### 2. 申请证书并自动配置 Nginx

```bash
sudo certbot --nginx -d menu.yourdomain.com
```

按提示选择重定向 HTTP 到 HTTPS。

### 3. 反向代理到本机 80 端口

Certbot 通常会生成类似配置。若需手写，可参考：

```nginx
# /etc/nginx/sites-available/menu
server {
    listen 80;
    server_name menu.yourdomain.com;
    return 301 https://$server_name$request_uri;
}
server {
    listen 443 ssl;
    server_name menu.yourdomain.com;
    ssl_certificate /etc/letsencrypt/live/menu.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/menu.yourdomain.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:80;  # 转发到 Docker 映射的 80
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置并重载 Nginx：

```bash
sudo ln -s /etc/nginx/sites-available/menu /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

此时 `.env` 中 `CORS_ALLOWED_ORIGINS` 应包含 `https://menu.yourdomain.com`，改完后重启后端容器使 CORS 生效：

```bash
docker compose -f docker-compose.prod.yml restart backend
```

---

## 四、安全与维护

1. **防火墙**：只开放 80、443，禁止对外 3306、9090。
2. **强密码**：`.env` 中数据库、JWT、管理员密码务必强且不提交到 Git。
3. **备份**：定期备份 MySQL 数据卷（如 `mysql_data`）或做 `mysqldump`。
4. **更新**：代码更新后重新构建并启动：
   ```bash
   docker compose -f docker-compose.prod.yml up -d --build
   ```
5. **日志**：`docker compose -f docker-compose.prod.yml logs -f backend` 可查看后端日志。

---

## 五、常见问题

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| 前端能开，接口 404/502 | 后端未就绪或 Nginx 代理错误 | 看 `docker compose logs backend`，确认 `/api` 是否代理到 `backend:9090` |
| 登录后提示 CORS 错误 | 前端访问域名未加入 CORS | 在 `.env` 中设置 `CORS_ALLOWED_ORIGINS` 为实际访问的协议+域名，并重启 backend |
| 数据库连接失败 | 首次启动时 MySQL 未就绪 | 等待 MySQL healthcheck 通过后再启动 backend，或再次执行 `docker compose -f docker-compose.prod.yml up -d` |

---

## 六、架构简述（公网部署时）

```
用户浏览器
    ↓ HTTPS (可选，由宿主机 Nginx + certbot 处理)
宿主机 80/443 → Docker 映射 80
    ↓
frontend 容器 (Nginx)
    ├── /     → 前端静态资源
    └── /api  → 反向代理到 backend:9090
                    ↓
                backend 容器 (Spring Boot)
                    ↓
                mysql 容器 (仅内网，不暴露端口)
```

按上述步骤即可完成公网部署；启用 HTTPS 并正确配置 CORS 后即可通过域名正常访问前后端。
