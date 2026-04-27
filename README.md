# 毕业论文选题与反馈系统

## 📖 项目简介

本项目是一个基于 Spring Boot + Vue 3 的毕业论文选题与反馈系统，支持学生选题、导师审核、管理员管理等完整功能。

## 🚀 快速开始

### 方式一：一键启动（推荐）

**Windows 用户**：
```bash
# 双击运行
run.bat
```

脚本会自动：
- ✅ 通过 Docker Compose 启动 mysql/backend/frontend（后台运行）

**访问地址**：
- 前端：http://localhost
- 后端API：http://localhost:9090/api
- 健康检查：http://localhost:9090/api/ping

### 方式二：Docker 开发模式

```bash
# 双击运行
scripts\docker-dev-up.bat
```

所有服务都在 Docker 容器中运行，支持代码热更新。

详细说明请参考：[docs/docker/DOCKER_DEV_GUIDE.md](docs/docker/DOCKER_DEV_GUIDE.md)

### 方式三：手动跑一遍 dev 构建/启动流程

在项目根目录执行以下命令：

```bash
# 前台启动（便于直接看日志）
docker compose -f docker-compose.dev.yml up
```

```bash
# 后台启动
docker compose -f docker-compose.dev.yml up -d
```

如需同时启动 embedding 服务：

```bash
docker compose -f docker-compose.dev.yml --profile embedding up -d --build
```

查看关键服务日志：

```bash
docker compose -f docker-compose.dev.yml logs -f backend
docker compose -f docker-compose.dev.yml logs -f frontend
```

需要干净重启时：

```bash
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.dev.yml up -d
```

仅验证前端生产打包（不走 Compose）：

```bash
cd frontend
npm run build
```

## 📚 文档导航

### 🎯 快速指南
- [docs/QUICK_START.md](docs/QUICK_START.md) - 快速开始指南
- [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md) - 项目结构说明

### 🗄️ 数据库
- [docs/database/DATABASE_GUIDE.md](docs/database/DATABASE_GUIDE.md) - 数据库架构指南
- [docs/database/DATABASE_OPERATIONS.md](docs/database/DATABASE_OPERATIONS.md) - 数据库操作指南（增删查改）

### 🐳 Docker
- [docs/docker/DOCKER_DEV_GUIDE.md](docs/docker/DOCKER_DEV_GUIDE.md) - Docker 开发模式指南
- [docs/docker/DOCKER_DEPLOY.md](docs/docker/DOCKER_DEPLOY.md) - Docker 部署指南

### 🚢 部署
- [docs/deployment/QUICK_DEPLOY.md](docs/deployment/QUICK_DEPLOY.md) - 快速部署指南
- [docs/deployment/PRODUCTION_DEPLOY.md](docs/deployment/PRODUCTION_DEPLOY.md) - 生产环境部署指南
- [docs/deployment/DEPLOYMENT_CHECKLIST.md](docs/deployment/DEPLOYMENT_CHECKLIST.md) - 部署检查清单
- [docs/DEPLOY_INTRANET.md](docs/DEPLOY_INTRANET.md) - **校园网内网部署**（机房/校内服务器）

### 📋 其他
- [docs/PROJECT_STRUCTURE_DETAILED.md](docs/PROJECT_STRUCTURE_DETAILED.md) - 详细项目结构
- [docs/IMPROVEMENTS.md](docs/IMPROVEMENTS.md) - 改进建议
- [docs/README.md](docs/README.md) - 完整文档索引

## 🛠️ 技术栈

### 后端
- Spring Boot 3.1.8
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8.0

### 前端
- Vue 3 + TypeScript
- Vite
- Element Plus
- Pinia

### 部署
- Docker & Docker Compose
- Nginx

## 📝 默认账号

- **管理员**：`admin` / `123456`
- **学生/导师**：可通过注册功能创建

## 🔧 开发环境要求

- JDK 17+
- Maven 3.6+
- Node.js 16+
- Docker Desktop（用于 MySQL）

## 📦 项目结构

```
menu/
├── backend/              # Spring Boot 后端
├── frontend/             # Vue 3 前端
├── docs/                 # 📚 文档目录
│   ├── database/        # 数据库相关文档
│   ├── deployment/      # 部署相关文档
│   └── docker/          # Docker 相关文档
├── scripts/              # 🔧 脚本文件目录
├── run.bat              # 🚀 一键启动（Docker Compose 后台运行）
└── README.md            # 📖 项目主文档
```

详细结构请参考：[docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md)

## 🚢 部署

### 快速部署
```bash
# 检查部署环境
scripts\check-deploy-ready.bat

# 一键部署
scripts\deploy.bat
```

详细部署步骤请参考：[docs/deployment/QUICK_DEPLOY.md](docs/deployment/QUICK_DEPLOY.md)

## 📄 许可证

本项目仅供学习和研究使用。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**最后更新**：2025-12-25
