# 项目结构说明

## 📁 目录结构

```
menu/
├── backend/                    # 后端服务（Spring Boot）
│   ├── src/                   # 源代码
│   │   ├── main/
│   │   │   ├── java/         # Java 源代码
│   │   │   └── resources/   # 配置文件和资源
│   │   │       ├── application.yml          # 开发环境配置
│   │   │       ├── application-prod.yml     # 生产环境配置
│   │   │       └── db/                      # 数据库脚本
│   │   │           ├── schema.sql          # 数据库表结构
│   │   │           ├── init-data.sql       # 初始化数据
│   │   │           └── *.sql               # 其他 SQL 脚本
│   │   └── test/            # 测试代码
│   ├── target/               # 编译输出（已忽略）
│   ├── logs/                 # 日志文件（已忽略）
│   ├── pom.xml               # Maven 配置文件
│   ├── Dockerfile            # Docker 镜像构建文件
│   └── *.md                  # 后端相关文档
│
├── frontend/                  # 前端服务（Vue 3）
│   ├── src/                  # 源代码
│   │   ├── api/             # API 接口定义
│   │   ├── views/           # 页面组件
│   │   ├── layouts/         # 布局组件
│   │   ├── stores/          # Pinia 状态管理
│   │   ├── router/          # 路由配置
│   │   └── App.vue          # 根组件
│   ├── node_modules/        # 依赖包（已忽略）
│   ├── dist/                # 构建输出（已忽略）
│   ├── package.json         # npm 配置文件
│   ├── Dockerfile           # Docker 镜像构建文件
│   └── nginx.conf           # Nginx 配置
│
├── docs/                      # 📚 文档目录（新建）
│   └── README.md            # 文档索引
│
├── config/                    # 配置文件目录（可选）
│
├── docker-compose.yml         # Docker Compose 开发配置
├── docker-compose.prod.yml    # Docker Compose 生产配置
├── docker-compose.dev.yml     # Docker Compose 开发模式配置
│
├── start.bat                  # 🚀 一键启动脚本（开发模式）
├── start-docker-dev.bat      # 🐳 Docker 开发模式启动脚本
├── init-database.bat         # 📊 数据库初始化脚本
├── deploy.bat                # 🚢 部署脚本
├── check-deploy-ready.bat    # ✅ 部署前检查脚本
│
├── README.md                 # 📖 项目主文档
├── QUICK_START.md            # ⚡ 快速开始指南
├── DATABASE_GUIDE.md         # 🗄️ 数据库指南
├── DATABASE_OPERATIONS.md    # 🔧 数据库操作指南
├── DOCKER_DEV_GUIDE.md       # 🐳 Docker 开发指南
├── DOCKER_DEPLOY.md          # 🚢 Docker 部署指南
├── QUICK_DEPLOY.md           # ⚡ 快速部署指南
├── PRODUCTION_DEPLOY.md      # 🏭 生产部署指南
├── DEPLOYMENT_CHECKLIST.md   # ✅ 部署检查清单
├── PROJECT_STRUCTURE_DETAILED.md  # 📋 详细项目结构
├── IMPROVEMENTS.md           # 💡 改进建议
├── SCRIPT_ENCODING.md        # 📝 脚本编码说明
│
└── .gitignore                # Git 忽略文件配置
```

## 📝 文件说明

### 🚀 启动脚本
- **start.bat** - 一键启动开发环境（本地运行后端+前端，Docker 运行 MySQL）
- **start-docker-dev.bat** - Docker 开发模式启动（所有服务都在 Docker 中，支持代码挂载）

### 🗄️ 数据库脚本
- **init-database.bat** - 初始化数据库（执行 schema.sql 和 init-data.sql）

### 🚢 部署相关
- **deploy.bat** - 一键部署脚本
- **check-deploy-ready.bat** - 部署前环境检查

### 📚 文档文件
- **README.md** - 项目主文档，包含完整的功能说明和开发记录
- **QUICK_START.md** - 快速开始指南
- **DATABASE_GUIDE.md** - 数据库架构和操作指南
- **DATABASE_OPERATIONS.md** - 数据库增删查改操作指南
- **DOCKER_DEV_GUIDE.md** - Docker 开发模式使用指南
- **DOCKER_DEPLOY.md** - Docker 部署完整指南
- **QUICK_DEPLOY.md** - 快速部署指南
- **PRODUCTION_DEPLOY.md** - 生产环境部署详细指南
- **DEPLOYMENT_CHECKLIST.md** - 部署检查清单
- **PROJECT_STRUCTURE_DETAILED.md** - 详细项目结构说明
- **IMPROVEMENTS.md** - 改进建议和未来计划
- **SCRIPT_ENCODING.md** - 脚本编码问题说明

### 🐳 Docker 配置
- **docker-compose.yml** - 开发环境 Docker Compose 配置
- **docker-compose.prod.yml** - 生产环境 Docker Compose 配置
- **docker-compose.dev.yml** - Docker 开发模式配置（支持代码挂载）

## 🗑️ 可删除的文件

以下文件可以考虑删除或移动到 `docs/` 目录：

1. **start.bat验证报告.md** - 验证报告，可以删除
2. **update-admin-docker.bat** - 如果不再需要，可以删除
3. **backend/db-quick-ref.bat** - 如果不再需要，可以删除

## 📦 已忽略的文件/目录

以下文件/目录已被 `.gitignore` 忽略，不会提交到版本控制：

- `backend/target/` - Maven 编译输出
- `backend/logs/` - 日志文件
- `frontend/node_modules/` - npm 依赖包
- `frontend/dist/` - 前端构建输出
- `.env` - 环境变量文件（包含敏感信息）
- `.idea/`, `.vscode/` - IDE 配置
- `*.log` - 日志文件

## 🔧 维护建议

1. **文档整理**：定期整理文档，删除过时的内容
2. **脚本管理**：保持脚本文件简洁，删除不再使用的脚本
3. **配置管理**：敏感配置使用环境变量，不要提交到版本控制
4. **日志管理**：定期清理日志文件，避免占用过多空间

