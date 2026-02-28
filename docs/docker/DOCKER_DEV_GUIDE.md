# Docker 开发模式使用指南

## 概述

Docker 开发模式支持**代码挂载和热更新**，无需每次修改代码后重新构建镜像。

## 两种启动方式对比

### 方式一：本地开发（start.bat）
- ✅ 启动速度快
- ✅ 调试方便（可直接调试 Java/Node.js）
- ✅ 热更新即时
- ❌ 需要本地安装 Java、Maven、Node.js

### 方式二：Docker 开发模式（start-docker-dev.bat）
- ✅ 环境一致，无需本地安装开发工具
- ✅ 支持代码挂载和热更新
- ✅ 适合团队协作（环境统一）
- ⚠️ 首次启动较慢（需要下载镜像）
- ⚠️ 调试不如本地方便

## 使用方法

### 启动开发环境

```bash
# Windows
start-docker-dev.bat

# 或手动执行
docker-compose -f docker-compose.dev.yml up
```

### 停止服务

```bash
# 按 Ctrl+C 停止，或执行
docker-compose -f docker-compose.dev.yml down
```

### 查看日志

```bash
# 查看所有服务日志
docker-compose -f docker-compose.dev.yml logs -f

# 查看特定服务日志
docker-compose -f docker-compose.dev.yml logs -f backend
docker-compose -f docker-compose.dev.yml logs -f frontend
```

## 热更新说明

### 前端（Vue）
- ✅ **自动热更新**：修改代码后，Vite 会自动重新编译并刷新浏览器
- 访问地址：http://localhost:5173

### 后端（Spring Boot）
- ✅ **自动热更新**：使用 `mvn spring-boot:run`，修改代码后会自动重新编译和重启
- ⚠️ 重启可能需要几秒钟
- 访问地址：http://localhost:9090/api

## 代码挂载说明

开发模式会自动挂载源代码目录：

- **后端**：`./backend` → `/app`（容器内）
- **前端**：`./frontend` → `/app`（容器内）

修改本地代码后，容器内的代码会同步更新。

## 数据持久化

- **MySQL 数据**：存储在 Docker volume `mysql_data_dev` 中
- **Maven 缓存**：存储在 Docker volume `maven_cache` 中（加速构建）
- **Node 模块缓存**：存储在 Docker volume `node_modules_cache` 中（加速安装）

## 常见问题

### 1. 首次启动很慢？

这是正常的，因为需要：
- 下载 Docker 镜像（Maven、Node.js、MySQL）
- 安装后端依赖（Maven）
- 安装前端依赖（npm）

### 2. 修改代码后没有自动更新？

- **前端**：检查 Vite 是否正常运行，查看容器日志
- **后端**：检查 Maven 是否检测到文件变化，可能需要等待几秒

### 3. 如何重新构建？

如果需要完全重新构建（比如修改了 Dockerfile）：

```bash
docker-compose -f docker-compose.dev.yml build
docker-compose -f docker-compose.dev.yml up
```

### 4. 如何清理数据？

```bash
# 停止并删除容器、网络
docker-compose -f docker-compose.dev.yml down

# 删除所有数据（包括数据库）
docker-compose -f docker-compose.dev.yml down -v
```

## 与生产模式的区别

| 特性 | 开发模式 (dev) | 生产模式 (prod) |
|------|---------------|----------------|
| 代码挂载 | ✅ 支持 | ❌ 不支持 |
| 热更新 | ✅ 支持 | ❌ 不支持 |
| 镜像构建 | ❌ 不需要 | ✅ 需要 |
| 启动速度 | ⚠️ 较慢 | ✅ 较快 |
| 调试 | ⚠️ 不便 | ❌ 不支持 |
| 适用场景 | 开发、测试 | 生产部署 |

## 推荐使用场景

- **日常开发**：使用 `start.bat`（本地模式，更快更方便）
- **环境测试**：使用 `start-docker-dev.bat`（Docker 模式，环境一致）
- **生产部署**：使用 `docker-compose.prod.yml`（生产模式）

