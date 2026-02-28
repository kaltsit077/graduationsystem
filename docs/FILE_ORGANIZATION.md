# 文件整理说明

## ✅ 整理完成

项目文件已按类别整理到相应目录，结构更加清晰。

## 📁 新的目录结构

```
menu/
├── backend/                    # 后端服务
├── frontend/                   # 前端服务
│
├── docs/                       # 📚 文档目录
│   ├── README.md              # 文档索引
│   ├── database/              # 数据库相关文档
│   │   ├── DATABASE_GUIDE.md
│   │   └── DATABASE_OPERATIONS.md
│   ├── deployment/            # 部署相关文档
│   │   ├── QUICK_DEPLOY.md
│   │   ├── PRODUCTION_DEPLOY.md
│   │   └── DEPLOYMENT_CHECKLIST.md
│   ├── docker/                # Docker 相关文档
│   │   ├── DOCKER_DEV_GUIDE.md
│   │   └── DOCKER_DEPLOY.md
│   ├── QUICK_START.md         # 快速开始
│   ├── PROJECT_STRUCTURE.md   # 项目结构
│   ├── PROJECT_STRUCTURE_DETAILED.md
│   ├── IMPROVEMENTS.md        # 改进建议
│   ├── SCRIPT_ENCODING.md     # 脚本编码说明
│   └── PROJECT_CLEANUP.md     # 项目整理说明
│
├── scripts/                    # 🔧 脚本文件目录
│   ├── start.bat              # 主要启动脚本
│   ├── start-docker-dev.bat  # Docker 开发模式
│   ├── init-database.bat     # 数据库初始化
│   ├── deploy.bat             # 部署脚本
│   ├── check-deploy-ready.bat # 部署前检查
│   └── update-admin-docker.bat # 管理员密码更新
│
├── start.bat                   # 🚀 快速启动（重定向到 scripts/start.bat）
├── README.md                   # 📖 项目主文档
│
└── docker-compose*.yml         # Docker Compose 配置文件
```

## 📝 文件分类说明

### 文档文件（docs/）
- **数据库相关**：`docs/database/` - 数据库架构、操作指南
- **部署相关**：`docs/deployment/` - 快速部署、生产部署、检查清单
- **Docker 相关**：`docs/docker/` - Docker 开发模式、部署指南
- **其他文档**：`docs/` - 快速开始、项目结构、改进建议等

### 脚本文件（scripts/）
- **启动脚本**：`start.bat`, `start-docker-dev.bat`
- **数据库脚本**：`init-database.bat`, `update-admin-docker.bat`
- **部署脚本**：`deploy.bat`, `check-deploy-ready.bat`

### 根目录文件
- `README.md` - 项目主文档
- `start.bat` - 快速启动入口（重定向到 scripts/start.bat）
- `docker-compose*.yml` - Docker Compose 配置文件

## 🎯 使用说明

### 启动项目
```bash
# 方式一：使用根目录的快速启动脚本
start.bat

# 方式二：直接使用 scripts 目录中的脚本
scripts\start.bat
```

### 查看文档
- 所有文档都在 `docs/` 目录下
- 查看文档索引：[docs/README.md](README.md)

### 运行脚本
- 所有脚本都在 `scripts/` 目录下
- 可以直接运行，例如：`scripts\init-database.bat`

## ✨ 整理优势

1. **结构清晰**：文档和脚本分类明确，易于查找
2. **易于维护**：相关文件集中管理，便于更新
3. **根目录简洁**：只保留必要的入口文件
4. **向后兼容**：根目录的 `start.bat` 重定向到 scripts 目录，不影响现有使用习惯

## 📋 注意事项

1. **路径引用**：如果其他文档中有路径引用，需要更新为新的路径
2. **脚本路径**：脚本中的相对路径可能需要调整
3. **文档链接**：README.md 中的文档链接已更新

## 🔄 后续维护

1. **新增文档**：根据类别放入对应的 `docs/` 子目录
2. **新增脚本**：放入 `scripts/` 目录
3. **定期整理**：每季度检查一次，删除不再使用的文件

---

**整理日期**：2025-12-25

