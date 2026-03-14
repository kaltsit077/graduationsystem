# 部署到 Ubuntu 云服务器（公网 IP）

本文以公网 IP **121.196.152.194** 为例，说明如何将「毕业论文选题系统」部署到一台 Ubuntu 云服务器上。  
部署完成后访问：**http://121.196.152.194** 即可使用系统。

---

## 一、服务器要求

- 系统：Ubuntu（你当前为 Ubuntu-ukiq）
- 建议配置：至少 2GB 内存、10GB 磁盘（你当前 2 vCPU / 2 GiB / 40 GiB 足够）
- 需开放端口：**22（SSH）**、**80（HTTP）**

---

## 二、在本地准备 .env 和 CORS

### 1. 复制并编辑 .env（在本地项目根目录）

```bash
# 在项目根目录 menu 下
cp .env.example .env
# 用记事本/VS Code 编辑 .env，填入下面说明的配置
```

**.env 必须修改的项：**

| 变量 | 说明 | 示例 |
|------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码（强密码） | 自设 |
| `MYSQL_PASSWORD` | 数据库用户密码 | 与下面 DB_PASSWORD 一致即可 |
| `DB_PASSWORD` | 后端连接数据库用的密码 | 自设 |
| `JWT_SECRET` | JWT 密钥，64 字节 Base64 | 见下方生成方式 |
| `ADMIN_PASSWORD` | 管理员登录密码 | 自设 |
| `CORS_ALLOWED_ORIGINS` | 允许访问的页面地址 | `http://121.196.152.194,http://121.196.152.194:80` |

**生成 JWT_SECRET（64 字节 Base64）：**

- Windows PowerShell：
  ```powershell
  [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
  ```
- Linux/Mac：
  ```bash
  openssl rand -base64 64
  ```

保存 `.env` 后**不要提交到 Git**。

---

## 三、把项目放到服务器上

### 方式 A：Git 克隆（推荐，服务器能访问你的仓库时）

1. 在控制台点击「**远程连接**」，用 SSH 登录服务器。
2. 安装 Git 和 Docker：

```bash
sudo apt update
sudo apt install -y git docker.io docker-compose
sudo systemctl start docker
sudo systemctl enable docker
# 把当前用户加入 docker 组，避免每次 sudo
sudo usermod -aG docker $USER
# 重新登录一次 SSH 后生效
```

3. 克隆项目（替换成你的仓库地址）：

```bash
cd ~
git clone https://github.com/你的用户名/menu.git
cd menu
```

4. 把在**本地**编辑好的 `.env` 上传到服务器项目根目录：

```bash
# 在你自己电脑上执行（PowerShell 或 CMD），把 .env 传到服务器
scp .env root@121.196.152.194:~/menu/.env
# 若不是 root 用户，把 root 换成你的 SSH 用户名
```

### 方式 B：本地上传整个项目

1. 在本地打包（不包含 node_modules、.git 等）：

```bash
# 在项目根目录 menu 下
# Windows 可用 7-Zip 或 tar（若已安装）
tar --exclude=node_modules --exclude=frontend/node_modules --exclude=.git -cvf menu.tar .
```

2. 用 SCP 上传到服务器：

```bash
scp menu.tar root@121.196.152.194:~/
```

3. SSH 登录服务器后解压并进入目录：

```bash
cd ~
mkdir -p menu && cd menu
tar -xvf ../menu.tar
```

4. 在服务器上安装 Docker 并创建 .env（若还没有）：

```bash
sudo apt update
sudo apt install -y docker.io docker-compose
sudo systemctl start docker && sudo systemctl enable docker
sudo usermod -aG docker $USER
# 重新登录 SSH 后
cp .env.example .env
nano .env   # 按上面“二”的说明修改并保存
```

---

## 四、在服务器上构建并启动

SSH 登录到服务器，进入项目目录：

```bash
cd ~/menu   # 或你解压后的目录
```

1. 使用生产配置构建并启动（首次会拉镜像、构建，需几分钟）：

```bash
docker-compose -f docker-compose.prod.yml build --no-cache
docker-compose -f docker-compose.prod.yml up -d
```

2. 查看是否都运行正常：

```bash
docker-compose -f docker-compose.prod.yml ps
```

应看到三个服务均为 **Up**：`mysql-graduation`、`graduation-backend`、`graduation-frontend`。

3. 查看日志（若有问题可排查）：

```bash
docker-compose -f docker-compose.prod.yml logs -f
# Ctrl+C 退出
```

---

## 五、开放防火墙（云控制台 + 系统防火墙）

1. **云控制台安全组**  
   在阿里云/腾讯云等控制台，为该实例开放：
   - 入方向：**22（SSH）**、**80（HTTP）**

2. **服务器本机防火墙（若启用了 ufw）：**

```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw reload
# 若未启用可：sudo ufw enable
```

---

## 六、验证部署

1. 浏览器访问：**http://121.196.152.194**  
   应能看到登录页。

2. 使用 .env 里配置的**管理员账号**（默认用户名 `admin`，密码为 `ADMIN_PASSWORD`）登录。

3. 若无法访问：
   - 检查 `docker-compose -f docker-compose.prod.yml ps` 是否三个服务都 Up；
   - 检查安全组是否放行 80；
   - 在服务器上执行：`curl -I http://localhost:80` 和 `curl http://localhost:80/api/ping` 看本机是否正常。

---

## 七、常用运维命令

```bash
cd ~/menu

# 查看状态
docker-compose -f docker-compose.prod.yml ps

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f
docker-compose -f docker-compose.prod.yml logs -f backend   # 仅后端

# 重启所有服务
docker-compose -f docker-compose.prod.yml restart

# 更新代码后重新部署
git pull   # 若用 Git
docker-compose -f docker-compose.prod.yml up -d --build
```

---

## 八、绑定域名（可选）

若你已有域名（如 `menu.example.com`），并解析到 **121.196.152.194**：

1. 在 `.env` 中把 `CORS_ALLOWED_ORIGINS` 改为你的域名，例如：
   ```env
   CORS_ALLOWED_ORIGINS=https://menu.example.com,http://menu.example.com
   ```
2. 在服务器上安装 Nginx + 证书（如 Let's Encrypt），用 Nginx 反向代理到 `http://127.0.0.1:80`，并配置 HTTPS。  
   详细可参考：`docs/deployment/PRODUCTION_DEPLOY.md`。

---

部署完成后，直接通过 **http://121.196.152.194** 即可使用系统；管理员账号与密码以你在 `.env` 中配置的为准。
