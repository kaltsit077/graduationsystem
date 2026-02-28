# 公网部署检查清单

## ⚠️ 部署前必须完成的安全配置

### 1. 修改默认密码和密钥

#### 数据库密码
- [ ] 修改 `docker-compose.yml` 中的数据库密码（MYSQL_ROOT_PASSWORD）
- [ ] 修改数据库用户密码（MYSQL_PASSWORD）
- [ ] 修改后端配置中的数据库连接密码（DB_PASSWORD）

#### JWT 密钥
- [ ] 生成新的 JWT 密钥（至少 64 字节）
- [ ] 更新 `application-prod.yml` 中的 JWT_SECRET
- [ ] 更新 `docker-compose.yml` 中的 JWT_SECRET 环境变量

#### 管理员账号
- [ ] 修改默认管理员密码（admin/123456）
- [ ] 更新数据库中的 admin 账号密码哈希

### 2. 环境变量配置

- [ ] 创建 `.env` 文件（不要提交到 Git）
- [ ] 配置所有敏感信息到环境变量
- [ ] 确保 `.env` 文件在 `.gitignore` 中

### 3. 网络安全配置

- [ ] 配置防火墙，只开放必要端口（80, 443, 22）
- [ ] 不要直接暴露数据库端口（3306）到公网
- [ ] 配置 Nginx 反向代理
- [ ] 启用 HTTPS（SSL 证书）

### 4. CORS 配置

- [ ] 配置后端 CORS，只允许特定域名访问
- [ ] 生产环境不要使用 `*` 允许所有域名

### 5. 前端配置

- [ ] 配置生产环境的 API 地址
- [ ] 确保前端能正确连接到后端

### 6. 数据库安全

- [ ] 创建专用数据库用户（不要使用 root）
- [ ] 限制数据库用户权限
- [ ] 配置数据库只允许本地连接

### 7. 日志和监控

- [ ] 配置日志轮转
- [ ] 配置错误监控（可选）
- [ ] 配置访问日志

### 8. 备份策略

- [ ] 配置数据库自动备份
- [ ] 配置备份文件存储位置
- [ ] 测试备份恢复流程

---

## 📋 部署步骤

### 步骤 1: 准备服务器

```bash
# 安装 Docker 和 Docker Compose
# Ubuntu/Debian
sudo apt update
sudo apt install docker.io docker-compose

# CentOS
sudo yum install docker docker-compose
sudo systemctl start docker
sudo systemctl enable docker
```

### 步骤 2: 配置环境变量

创建 `.env` 文件：

```env
# 数据库配置
MYSQL_ROOT_PASSWORD=你的强密码
MYSQL_PASSWORD=你的强密码
DB_PASSWORD=你的强密码

# JWT 配置
JWT_SECRET=你的64字节密钥
JWT_EXPIRATION=120

# 管理员配置
ADMIN_PASSWORD=你的强密码
```

### 步骤 3: 修改配置文件

1. 更新 `docker-compose.yml` 使用环境变量
2. 更新 `application-prod.yml` 使用环境变量
3. 配置 CORS 允许的域名

### 步骤 4: 构建和启动

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 步骤 5: 配置 Nginx（推荐）

参考 `backend/nginx.conf.example` 配置反向代理和 HTTPS

### 步骤 6: 验证部署

- [ ] 访问前端页面
- [ ] 测试登录功能
- [ ] 测试注册功能
- [ ] 检查 API 接口
- [ ] 检查数据库连接

---

## 🔒 安全建议

1. **定期更新**: 保持 Docker 镜像和依赖包最新
2. **监控日志**: 定期检查应用日志和访问日志
3. **备份数据**: 每天自动备份数据库
4. **限制访问**: 使用防火墙限制不必要的端口访问
5. **使用 HTTPS**: 生产环境必须使用 HTTPS
6. **强密码策略**: 所有密码至少 12 位，包含大小写字母、数字和特殊字符

---

## ⚡ 快速部署命令

```bash
# 1. 克隆项目
git clone <your-repo-url>
cd menu

# 2. 创建 .env 文件
cp .env.example .env
# 编辑 .env 文件，填入你的配置

# 3. 构建并启动
docker-compose up -d --build

# 4. 查看状态
docker-compose ps

# 5. 查看日志
docker-compose logs -f
```

---

**注意**: 部署到公网前，请务必完成所有安全检查项！

