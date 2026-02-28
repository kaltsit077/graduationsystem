# 前端项目说明

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - 类型安全的 JavaScript
- **Vite** - 下一代前端构建工具
- **Vue Router** - 官方路由管理器
- **Pinia** - 状态管理
- **Element Plus** - Vue 3 UI 组件库
- **Axios** - HTTP 客户端

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口定义
│   │   ├── auth.ts       # 认证相关接口
│   │   └── request.ts    # Axios 请求封装
│   ├── layouts/          # 布局组件
│   │   ├── StudentLayout.vue
│   │   ├── TeacherLayout.vue
│   │   └── AdminLayout.vue
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # Pinia 状态管理
│   │   └── auth.ts       # 认证状态
│   ├── views/            # 页面组件
│   │   ├── Login.vue
│   │   ├── student/      # 学生端页面
│   │   ├── teacher/      # 导师端页面
│   │   └── admin/        # 管理员端页面
│   ├── App.vue
│   ├── main.ts
│   └── style.css
├── index.html
├── package.json
├── vite.config.ts
└── tsconfig.json
```

## 快速开始

### 安装依赖

```bash
cd frontend
npm install
```

### 开发模式

```bash
npm run dev
```

访问：http://localhost:3000

### 构建生产版本

```bash
npm run build
```

## 功能模块

### 认证模块
- 登录页面
- JWT Token 管理
- 路由守卫

### 学生端
- 首页（统计信息）
- 选题中心（查看和申请选题）
- 我的申请（查看申请状态）
- 个人中心（完善信息、标签管理）

### 导师端
- 首页（统计信息）
- 选题管理（创建、编辑、提交审核）
- 申请处理（审核学生申请）
- 个人中心（完善信息、研究方向标签）

### 管理员端
- 首页
- 选题审核（审核导师提交的选题）

## 开发说明

### API 接口

所有 API 请求都通过 `src/api/request.ts` 封装，自动添加 JWT Token。

### 路由守卫

路由守卫会自动检查：
- 用户是否已登录
- 用户角色是否匹配页面权限

### 状态管理

使用 Pinia 管理全局状态：
- `auth` store：用户认证信息（token、角色等）

## 待完善功能

1. **API 接口对接**：连接后端真实接口
2. **文件上传**：论文上传功能
3. **通知系统**：实时通知显示
4. **AI 选题生成**：前端界面
5. **推荐算法**：个性化推荐展示
6. **响应式设计**：移动端适配

