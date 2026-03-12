# 毕业论文选题与反馈系统 - 完整项目结构说明

本文档详细说明整个项目的目录结构、文件组织以及每个部分的职责。

---

## 📁 项目整体结构

```
menu/
├── backend/                    # 后端项目（Spring Boot）
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/graduation/
│   │   │   │   ├── entity/              # 数据库实体层
│   │   │   │   ├── mapper/              # 数据访问层（MyBatis-Plus）
│   │   │   │   ├── dto/                 # 数据传输对象
│   │   │   │   ├── service/             # 业务逻辑层
│   │   │   │   ├── controller/          # 控制器层（REST API）
│   │   │   │   ├── config/              # 配置类
│   │   │   │   ├── security/            # 安全认证模块
│   │   │   │   ├── util/                # 工具类
│   │   │   │   ├── algo/                # 算法模块
│   │   │   │   ├── common/              # 通用类
│   │   │   │   └── GraduationApplication.java
│   │   │   └── resources/
│   │   │       ├── db/
│   │   │       ├── application.yml
│   │   │       └── application-prod.yml
│   │   └── test/                        # 测试代码
│   ├── pom.xml                          # Maven依赖配置
│   ├── (启动建议见根目录 run.bat)        # 入口启动脚本在项目根目录
│   ├── nginx.conf.example               # Nginx反向代理配置示例
│   ├── DEPLOY.md                        # 部署文档
│   ├── DEVELOPMENT_LOG.md               # 开发日志
│   └── PROJECT_STRUCTURE.md             # 后端项目结构说明
├── frontend/                    # 前端项目（Vue 3 + TypeScript）
│   ├── src/
│   │   ├── api/                         # API接口定义
│   │   ├── layouts/                     # 布局组件
│   │   ├── router/                      # 路由配置
│   │   ├── stores/                      # 状态管理（Pinia）
│   │   ├── views/                       # 页面组件
│   │   ├── App.vue
│   │   ├── main.ts
│   │   └── style.css
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── README.md
├── config/                      # 现有配置文件（Node.js，可移除）
├── module/                      # 预留目录
└── README.md                    # 项目主文档
```

---

## 🎯 后端项目详细结构（backend/）

### 1. 实体层（entity/）

**职责**：定义数据库表对应的 Java 实体类，使用 MyBatis-Plus 注解映射数据库表。

| 文件名 | 对应表 | 职责说明 |
|--------|--------|---------|
| `User.java` | user | 用户基础信息实体（学生/导师/管理员通用）<br>- 字段：id, username, passwordHash, realName, role, status<br>- 角色枚举：STUDENT, TEACHER, ADMIN |
| `StudentProfile.java` | student_profile | 学生详细信息实体<br>- 字段：userId, major, grade, interestDesc<br>- 关联 User 表 |
| `TeacherProfile.java` | teacher_profile | 导师详细信息实体<br>- 字段：userId, title, researchDirection, maxStudentCount<br>- 关联 User 表 |
| `Tag.java` | tag | 标签字典实体<br>- 字段：id, name, type<br>- 标签类型：TEACHER, STUDENT, TOPIC |
| `UserTag.java` | user_tag | 用户标签关联实体<br>- 字段：userId, tagName, weight<br>- 权重范围：0-1，研究方向标签权重0.9 |
| `Topic.java` | topic | 选题实体<br>- 字段：teacherId, title, description, status, maxApplicants, currentApplicants<br>- 状态枚举：DRAFT, PENDING_REVIEW, REJECTED, OPEN, CLOSED |
| `TopicTag.java` | topic_tag | 选题标签关联实体<br>- 字段：topicId, tagName, weight |
| `TopicReview.java` | topic_review | 选题审核记录实体<br>- 字段：topicId, adminId, result, comment<br>- 审核结果：PASS, REJECT |
| `TopicApplication.java` | topic_application | 选题申请实体<br>- 字段：topicId, studentId, status, remark, teacherFeedback, matchScore<br>- 申请状态：PENDING, APPROVED, REJECTED |
| `Thesis.java` | thesis | 论文实体<br>- 字段：topicId, studentId, fileUrl, fileName, fileSize, status<br>- 状态：UPLOADED, REVIEWED |
| `Notification.java` | notification | 通知实体<br>- 字段：userId, type, title, content, isRead, relatedId<br>- 通知类型：SYSTEM, APPLICATION_RESULT, TOPIC_OPEN 等 |

**关键注解**：
- `@TableName("table_name")` - 指定数据库表名
- `@TableId(type = IdType.AUTO)` - 主键自增
- `@Data` (Lombok) - 自动生成 getter/setter

---

### 2. 数据访问层（mapper/）

**职责**：定义 MyBatis-Plus Mapper 接口，提供数据库 CRUD 操作。

| 文件名 | 对应实体 | 职责说明 |
|--------|---------|---------|
| `UserMapper.java` | User | 用户数据访问接口<br>- 继承 BaseMapper<User>，提供基础 CRUD<br>- 支持条件查询、分页等 |
| `StudentProfileMapper.java` | StudentProfile | 学生信息数据访问接口 |
| `TeacherProfileMapper.java` | TeacherProfile | 导师信息数据访问接口 |
| `UserTagMapper.java` | UserTag | 用户标签数据访问接口 |
| `TopicMapper.java` | Topic | 选题数据访问接口 |
| `TopicTagMapper.java` | TopicTag | 选题标签数据访问接口 |
| `TopicReviewMapper.java` | TopicReview | 选题审核记录数据访问接口 |
| `TopicApplicationMapper.java` | TopicApplication | 选题申请数据访问接口 |
| `ThesisMapper.java` | Thesis | 论文数据访问接口 |
| `NotificationMapper.java` | Notification | 通知数据访问接口 |

**关键特性**：
- 继承 `BaseMapper<T>`，自动获得基础 CRUD 方法
- 使用 `@Mapper` 注解，Spring 自动扫描
- 支持 Lambda 表达式条件查询

---

### 3. 数据传输对象（dto/）

**职责**：定义前后端交互的数据传输对象，用于 API 请求和响应。

| 文件名 | 类型 | 职责说明 |
|--------|------|---------|
| `LoginRequest.java` | Request | 登录请求 DTO<br>- 字段：username, password<br>- 使用 @NotBlank 验证 |
| `LoginResponse.java` | Response | 登录响应 DTO<br>- 字段：token, role, realName, userId |

**说明**：DTO 用于解耦实体类和 API 接口，避免直接暴露数据库结构。

---

### 4. 业务逻辑层（service/）

**职责**：实现核心业务逻辑，调用 Mapper 进行数据操作，处理复杂的业务规则。

| 文件名 | 职责说明 |
|--------|---------|
| `AuthService.java` | **用户认证服务**<br>- 用户登录验证<br>- 密码 BCrypt 加密校验<br>- 生成 JWT Token |
| `StudentService.java` | **学生信息服务**<br>- 完善学生信息（专业、年级、兴趣描述）<br>- 自动生成学生标签<br>- 标签管理 |
| `TeacherService.java` | **导师信息服务**<br>- 完善导师信息（职称、研究方向、最大学生数）<br>- 自动生成导师标签（研究方向权重0.9）<br>- 标签管理 |
| `TagService.java` | **标签管理服务**<br>- 从文本中提取标签（研究方向/兴趣描述）<br>- 标签权重管理<br>- 标签去重合并<br>- 自动生成标签逻辑 |
| `TopicService.java` | **选题管理服务**<br>- 选题 CRUD 操作<br>- 选题去重检测（调用算法）<br>- 选题审核流转（状态管理）<br>- 管理员审核选题 |
| `AiTopicService.java` | **AI 选题生成服务**<br>- 基于标签生成候选选题（模拟实现）<br>- 支持导师和学生两种生成方式<br>- 可扩展对接真实大模型 API |
| `ApplicationService.java` | **申请管理服务**<br>- 学生提交选题申请<br>- 申请冲突校验（人数限制、重复申请、已有通过申请）<br>- 导师审核申请（通过/拒绝）<br>- 按匹配度排序申请列表<br>- 自动更新选题申请人数 |
| `ThesisService.java` | **论文管理服务**<br>- 论文上传（验证申请状态）<br>- 论文列表查询（学生/导师）<br>- 文件信息管理 |
| `NotificationService.java` | **通知服务**<br>- 创建通知（选题审核、申请结果等）<br>- 查询通知列表<br>- 标记已读<br>- 未读消息统计 |

**关键特性**：
- 使用 `@Service` 注解，Spring 自动管理
- 关键操作使用 `@Transactional` 保证事务一致性
- 业务逻辑与数据访问分离

---

### 5. 控制器层（controller/）

**职责**：处理 HTTP 请求，调用 Service 处理业务逻辑，返回 JSON 响应。

| 文件名 | 职责说明 |
|--------|---------|
| `AuthController.java` | **认证控制器**<br>- `POST /api/auth/login` - 用户登录<br>- 返回 JWT Token 和用户信息 |
| `HelloController.java` | **健康检查控制器**<br>- `GET /api/ping` - 服务健康检查<br>- 返回 "backend-ok" |
| `GlobalExceptionHandler.java` | **全局异常处理器**<br>- 统一处理业务异常<br>- 返回友好的错误信息<br>- 使用 @RestControllerAdvice |

**待创建控制器**（Service 层已完成，需补充 Controller）：
- `StudentController.java` - 学生端接口
- `TeacherController.java` - 导师端接口
- `AdminController.java` - 管理员端接口
- `TopicController.java` - 选题相关接口
- `ApplicationController.java` - 申请相关接口
- `ThesisController.java` - 论文相关接口
- `NotificationController.java` - 通知相关接口

---

### 6. 配置类（config/）

**职责**：Spring 配置类，配置框架行为和安全规则。

| 文件名 | 职责说明 |
|--------|---------|
| `SecurityConfig.java` | **Spring Security 配置**<br>- JWT 认证配置<br>- 路径权限控制（/api/admin/**、/api/teacher/**、/api/student/**）<br>- BCrypt 密码编码器<br>- 禁用 CSRF（使用 JWT）<br>- 无状态会话管理 |

---

### 7. 安全模块（security/）

**职责**：JWT 认证和安全过滤器。

| 文件名 | 职责说明 |
|--------|---------|
| `JwtAuthenticationFilter.java` | **JWT 认证过滤器**<br>- 从请求头提取 Token（Authorization: Bearer xxx）<br>- 验证 Token 有效性<br>- 解析用户信息设置到 SecurityContext<br>- 将用户信息存储到 request 属性中 |

---

### 8. 工具类（util/）

**职责**：提供通用工具方法。

| 文件名 | 职责说明 |
|--------|---------|
| `JwtUtil.java` | **JWT 工具类**<br>- 生成 JWT Token（包含 userId, username, role）<br>- 解析 Token 获取用户信息<br>- 验证 Token 有效性<br>- 从 Authorization header 提取 Token<br>- 使用 HMAC-SHA512 算法签名 |

---

### 9. 算法模块（algo/）

**职责**：实现核心算法逻辑。

| 文件名 | 职责说明 |
|--------|---------|
| `DuplicateCheckAlgorithm.java` | **选题去重检测算法**<br>- Jaccard 相似度计算（词集合交集/并集）<br>- 余弦相似度计算（词向量点积）<br>- 融合相似度（权重：Jaccard 0.4，Cosine 0.6）<br>- 文本分词处理<br>- 相似度阈值：0.7<br>- 返回相似度值和相似选题信息 |

**算法流程**：
1. 文本预处理（分词、去停用词）
2. 计算 Jaccard 相似度
3. 计算余弦相似度
4. 融合计算最终相似度
5. 与阈值比较判断是否通过

---

### 10. 通用类（common/）

**职责**：定义通用的响应格式和上下文工具。

| 文件名 | 职责说明 |
|--------|---------|
| `ApiResponse.java` | **统一 API 响应封装**<br>- 泛型响应：ApiResponse<T><br>- 标准字段：code, message, data<br>- 静态方法：success(data), error(message)<br>- 成功 code=200，失败 code=-1 |
| `UserContext.java` | **用户上下文工具类**<br>- 从 request 中获取当前登录用户信息<br>- 提供 getUserId(), getUsername(), getRole() 方法<br>- 使用 ThreadLocal 存储用户信息<br>- 支持清除 ThreadLocal（防止内存泄漏） |

---

### 11. 主启动类

| 文件名 | 职责说明 |
|--------|---------|
| `GraduationApplication.java` | **Spring Boot 主启动类**<br>- @SpringBootApplication 注解<br>- @MapperScan("com.example.graduation.mapper") 扫描 Mapper<br>- main 方法启动应用 |

---

### 12. 配置文件（resources/）

| 文件名/目录 | 职责说明 |
|------------|---------|
| `application.yml` | **开发环境配置**<br>- 服务端口：8080<br>- MySQL 数据库连接<br>- MyBatis-Plus 配置<br>- JWT 配置<br>- Jackson 时区配置 |
| `application-prod.yml` | **生产环境配置**<br>- 支持环境变量注入<br>- HikariCP 连接池配置<br>- 文件上传限制（200MB）<br>- 日志配置（文件输出、轮转）<br>- 服务监听地址：0.0.0.0 |
| `db/schema.sql` | **数据库表结构 SQL**<br>- 11 张表的建表语句<br>- 外键约束<br>- 索引定义<br>- 表注释 |

---

### 13. 构建和部署文件

| 文件名 | 职责说明 |
|--------|---------|
| `pom.xml` | **Maven 依赖配置**<br>- Spring Boot 3.2.0<br>- Spring Security, Web, Validation<br>- MyBatis-Plus 3.5.6<br>- MySQL Connector<br>- JWT (jjwt 0.11.5)<br>- Lombok<br>- 使用 dependencyManagement 管理版本 |
| `run.bat` | **Windows 一键启动（推荐）**<br>- 调用 `scripts/docker-up.bat`<br>- 通过 Docker Compose 后台启动 mysql/backend/frontend |
| `scripts/docker-up.bat` | **Docker Compose 后台启动**<br>- `docker compose up -d`（可通过 `COMPOSE_FILE` 切换 compose 文件） |
| `scripts/docker-dev-up.bat` | **Docker 开发模式前台启动**<br>- `docker compose -f docker-compose.dev.yml up`（便于查看日志、热更新） |
| `scripts/backend-local.bat` | **本机启动后端（JAR）**<br>- 检测 Java/Maven<br>- 端口保护：默认只会结束本机 Java 进程，避免误杀 Docker |
| `scripts/frontend-dev.bat` | **本机启动前端（Vite）**<br>- 检测 Node/npm<br>- 自动安装依赖并启动 dev server |
| `nginx.conf.example` | **Nginx 反向代理配置示例**<br>- 后端 API 代理（/api/）<br>- 前端静态文件服务<br>- 文件上传支持（200MB）<br>- HTTPS/SSL 配置示例<br>- CORS 处理说明 |
| `DEPLOY.md` | **部署文档**<br>- 前置要求<br>- 数据库准备<br>- 后端部署步骤<br>- Nginx 配置<br>- SSL 证书配置<br>- 常见问题 |
| `DEVELOPMENT_LOG.md` | **开发日志**<br>- 记录开发过程中的重要变更<br>- 功能模块完成状态<br>- 技术要点说明 |
| `PROJECT_STRUCTURE.md` | **后端项目结构说明**<br>- 目录结构说明<br>- 模块职责<br>- 数据流说明 |

---

## 前端项目详细结构（frontend/）

### 1. API 接口层（api/）

**职责**：定义与后端通信的 API 接口。

| 文件名 | 职责说明 |
|--------|---------|
| `request.ts` | **Axios 请求封装**<br>- 创建 axios 实例（baseURL: /api）<br>- 请求拦截器：自动添加 JWT Token 到请求头<br>- 响应拦截器：统一处理错误，401 自动跳转登录<br>- 错误消息提示（Element Plus Message） |
| `auth.ts` | **认证相关接口**<br>- login(data: LoginRequest): 用户登录接口<br>- 定义 LoginRequest 和 LoginResponse 类型 |

---

### 2. 状态管理（stores/）

**职责**：使用 Pinia 管理全局状态。

| 文件名 | 职责说明 |
|--------|---------|
| `auth.ts` | **认证状态管理**<br>- 存储：token, role, realName, userId<br>- 方法：setAuth(), clearAuth()<br>- 方法：isAuthenticated(), isStudent(), isTeacher(), isAdmin()<br>- 持久化：localStorage 存储用户信息 |

---

### 3. 路由配置（router/）

**职责**：定义前端路由和页面访问规则。

| 文件名 | 职责说明 |
|--------|---------|
| `index.ts` | **路由配置**<br>- 定义所有路由（登录、学生端、导师端、管理员端）<br>- 路由守卫：检查登录状态和角色权限<br>- 自动重定向：角色不匹配时重定向到对应首页<br>- 使用 Vue Router 4 |

**路由结构**：
- `/login` - 登录页（无需认证）
- `/student/*` - 学生端（需要 STUDENT 角色）
- `/teacher/*` - 导师端（需要 TEACHER 角色）
- `/admin/*` - 管理员端（需要 ADMIN 角色）

---

### 4. 布局组件（layouts/）

**职责**：定义不同角色的页面布局（侧边栏、顶部导航栏）。

| 文件名 | 职责说明 |
|--------|---------|
| `StudentLayout.vue` | **学生端布局**<br>- 顶部导航栏（系统名称、用户名、退出）<br>- 侧边栏菜单（首页、选题中心、我的申请、个人中心）<br>- 主内容区域（router-view） |
| `TeacherLayout.vue` | **导师端布局**<br>- 顶部导航栏（标注"导师端"）<br>- 侧边栏菜单（首页、选题管理、申请处理、个人中心）<br>- 主内容区域 |
| `AdminLayout.vue` | **管理员端布局**<br>- 顶部导航栏（标注"管理员端"）<br>- 侧边栏菜单（首页、选题审核）<br>- 主内容区域 |

**共同特性**：
- 响应式布局（el-container, el-header, el-aside, el-main）
- 菜单高亮当前路由
- 退出登录确认对话框

---

### 5. 页面组件（views/）

#### 5.1 登录页面

| 文件名 | 职责说明 |
|--------|---------|
| `Login.vue` | **登录页面**<br>- 用户名/密码输入表单<br>- 表单验证<br>- 调用登录 API<br>- 登录成功后根据角色跳转<br>- 现代化 UI 设计（渐变背景、卡片样式） |

#### 5.2 学生端页面（views/student/）

| 文件名 | 职责说明 |
|--------|---------|
| `Home.vue` | **学生首页**<br>- 欢迎信息<br>- 统计卡片（我的申请、未读通知、已通过选题）<br>- 快速操作按钮<br>- 选题开放状态提示 |
| `Topics.vue` | **选题中心**<br>- 选题列表表格<br>- 查看选题详情（对话框）<br>- 提交申请功能<br>- 申请备注输入<br>- 刷新选题列表 |
| `Applications.vue` | **我的申请**<br>- 申请记录表格<br>- 状态标签显示（待审核/已通过/已拒绝）<br>- 导师反馈展示<br>- 申请详情查看 |
| `Profile.vue` | **个人中心**<br>- 个人信息表单（专业、年级、兴趣描述）<br>- 保存个人信息<br>- 标签展示（带权重）<br>- 自动生成标签功能 |

#### 5.3 导师端页面（views/teacher/）

| 文件名 | 职责说明 |
|--------|---------|
| `Home.vue` | **导师首页**<br>- 欢迎信息<br>- 统计卡片（我的选题、待处理申请、已通过申请）<br>- 快速操作按钮 |
| `Topics.vue` | **选题管理**<br>- 选题列表表格<br>- 创建选题（对话框）<br>- 编辑选题<br>- 提交审核功能<br>- 去重检测功能<br>- 状态标签显示 |
| `Applications.vue` | **申请处理**<br>- 申请列表表格（按匹配度排序）<br>- 匹配度标签显示<br>- 通过/拒绝申请功能<br>- 反馈意见输入 |
| `Profile.vue` | **个人中心**<br>- 个人信息表单（职称、研究方向、最大学生数）<br>- 保存信息（自动生成标签）<br>- 标签展示（研究方向权重0.9） |

#### 5.4 管理员端页面（views/admin/）

| 文件名 | 职责说明 |
|--------|---------|
| `Home.vue` | **管理员首页**<br>- 欢迎信息<br>- 统计卡片（待审核选题）<br>- 快速操作按钮 |
| `Reviews.vue` | **选题审核**<br>- 待审核选题列表<br>- 通过/驳回选题功能<br>- 审核意见输入<br>- 导师信息显示 |

---

### 6. 配置文件

| 文件名 | 职责说明 |
|--------|---------|
| `package.json` | **npm 依赖配置**<br>- Vue 3, Vue Router, Pinia<br>- Element Plus, Element Plus Icons<br>- Axios<br>- TypeScript, Vite<br>- 开发脚本：dev, build, preview |
| `vite.config.ts` | **Vite 构建配置**<br>- Vue 插件配置<br>- 路径别名（@ → src）<br>- 开发服务器配置（端口 3000）<br>- API 代理配置（/api → http://localhost:8080） |
| `tsconfig.json` | **TypeScript 配置**<br>- 编译选项（ES2020, strict mode）<br>- 路径映射<br>- Vue 文件支持 |
| `index.html` | **HTML 入口文件**<br>- 页面标题<br>- 挂载点（#app）<br>- 引入 main.ts |

---

### 7. 核心文件

| 文件名 | 职责说明 |
|--------|---------|
| `main.ts` | **应用入口文件**<br>- 创建 Vue 应用实例<br>- 注册 Pinia（状态管理）<br>- 注册 Vue Router（路由）<br>- 注册 Element Plus（UI 组件库）<br>- 注册所有图标组件<br>- 挂载到 #app |
| `App.vue` | **根组件**<br>- 包含 router-view（路由视图）<br>- 应用的最外层容器 |
| `style.css` | **全局样式**<br>- 重置样式<br>- 全局字体设置<br>- 基础样式定义 |

---

## 📊 数据流转图

### 1. 用户登录流程

```
前端 Login.vue
  ↓ (POST /api/auth/login)
后端 AuthController
  ↓ (调用)
AuthService.login()
  ↓ (查询数据库)
UserMapper
  ↓ (验证密码)
BCryptPasswordEncoder
  ↓ (生成 Token)
JwtUtil.generateToken()
  ↓ (返回)
LoginResponse (token, role, realName, userId)
  ↓ (存储到 localStorage)
前端 auth store
  ↓ (路由跳转)
对应角色首页
```

### 2. 选题创建流程（导师）

```
前端 Teacher/Topics.vue
  ↓ (创建选题表单)
POST /api/teacher/topics
  ↓
后端 TopicController
  ↓ (调用)
TopicService.createTopic()
  ↓ (保存选题)
TopicMapper.insert()
  ↓ (保存标签)
TopicTagMapper.insert()
  ↓ (返回)
前端刷新选题列表
```

### 3. 选题审核流程

```
导师提交审核
  ↓
TopicService.submitForReview()
  ↓ (去重检测)
DuplicateCheckAlgorithm
  ↓ (通过后)
更新状态为 PENDING_REVIEW
  ↓
管理员审核
  ↓
TopicService.reviewTopic()
  ↓ (更新状态)
状态变为 OPEN 或 REJECTED
  ↓ (发送通知)
NotificationService.createNotification()
```

### 4. 申请流程（学生）

```
前端 Student/Topics.vue
  ↓ (点击申请)
POST /api/applications
  ↓
后端 ApplicationController
  ↓ (调用)
ApplicationService.submitApplication()
  ↓ (校验)
- 选题是否开放
- 申请人数是否已满
- 是否重复申请
- 是否已有通过申请
  ↓ (创建申请)
TopicApplicationMapper.insert()
  ↓ (更新选题申请人数)
TopicMapper.updateById()
  ↓ (发送通知给导师)
NotificationService.createNotification()
```

---

##  安全机制

### 1. 认证流程

1. **用户登录**：用户名 + 密码 → 后端验证 → 返回 JWT Token
2. **Token 存储**：前端 localStorage 存储 Token
3. **请求携带**：每次请求在 Header 中携带 `Authorization: Bearer <token>`
4. **后端验证**：JwtAuthenticationFilter 拦截请求，验证 Token
5. **权限检查**：SecurityConfig 根据路径和角色判断权限

### 2. 权限控制

- **前端路由守卫**：检查登录状态和角色，自动重定向
- **后端路径权限**：SecurityConfig 配置不同路径的角色要求
- **角色枚举**：STUDENT, TEACHER, ADMIN

---

## 依赖关系

### 后端依赖层次

```
Controller
  ↓ (调用)
Service
  ↓ (调用)
Mapper
  ↓ (操作)
Entity (数据库表)
```

### 前端依赖层次

```
Views (页面组件)
  ↓ (调用)
API (接口定义)
  ↓ (使用)
Request (Axios 封装)
  ↓ (请求)
后端 API
```

---

##  核心算法说明

### 去重检测算法（DuplicateCheckAlgorithm）

**输入**：当前选题标题和描述、历史选题列表  
**输出**：相似度值（0-1）、是否通过（阈值0.7）

**步骤**：
1. 文本分词（简化版，按空格和标点分割）
2. 计算 Jaccard 相似度：`交集大小 / 并集大小`
3. 计算余弦相似度：词向量点积 / (向量模长乘积)
4. 融合相似度：`0.4 * Jaccard + 0.6 * Cosine`
5. 判断：相似度 < 0.7 则通过

---

##  开发规范

### 命名规范

- **实体类**：大驼峰，如 `User.java`
- **Service 类**：大驼峰 + Service，如 `UserService.java`
- **Controller 类**：大驼峰 + Controller，如 `UserController.java`
- **Mapper 接口**：大驼峰 + Mapper，如 `UserMapper.java`
- **前端组件**：大驼峰，如 `Login.vue`

### 目录规范

- **后端**：按功能分层（entity, mapper, service, controller）
- **前端**：按角色和功能划分（views/student, views/teacher）

---

## 启动顺序

1. **启动数据库**：MySQL 服务
2. **执行 SQL 脚本**：`backend/src/main/resources/db/schema.sql`
3. **启动后端（本机）**：运行 `scripts\backend-local.bat` 或 `cd backend && mvn spring-boot:run`
4. **启动前端**：`cd frontend && npm install && npm run dev`
5. **访问系统**：http://localhost:3000

---

## 相关文档

- [项目主文档](README.md) - 项目整体说明
- [后端项目结构](backend/PROJECT_STRUCTURE.md) - 后端详细说明
- [后端开发日志](backend/DEVELOPMENT_LOG.md) - 开发记录
- [部署文档](backend/DEPLOY.md) - 生产部署指南
- [前端项目说明](frontend/README.md) - 前端开发说明

---

**最后更新**：2025-12-2


