# 项目结构说明

本文档详细说明毕业论文选题与反馈系统的后端项目结构。

## 目录结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/graduation/
│   │   │   ├── entity/              # 实体类（对应数据库表）
│   │   │   │   ├── User.java
│   │   │   │   ├── StudentProfile.java
│   │   │   │   ├── TeacherProfile.java
│   │   │   │   ├── Tag.java
│   │   │   │   ├── UserTag.java
│   │   │   │   ├── Topic.java
│   │   │   │   ├── TopicTag.java
│   │   │   │   ├── TopicReview.java
│   │   │   │   ├── TopicApplication.java
│   │   │   │   ├── Thesis.java
│   │   │   │   └── Notification.java
│   │   │   ├── mapper/              # MyBatis-Plus Mapper接口
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── StudentProfileMapper.java
│   │   │   │   ├── TeacherProfileMapper.java
│   │   │   │   ├── UserTagMapper.java
│   │   │   │   ├── TopicMapper.java
│   │   │   │   ├── TopicTagMapper.java
│   │   │   │   ├── TopicReviewMapper.java
│   │   │   │   ├── TopicApplicationMapper.java
│   │   │   │   ├── ThesisMapper.java
│   │   │   │   └── NotificationMapper.java
│   │   │   ├── dto/                 # 数据传输对象
│   │   │   │   ├── LoginRequest.java
│   │   │   │   └── LoginResponse.java
│   │   │   ├── service/             # 业务逻辑层
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── StudentService.java
│   │   │   │   ├── TeacherService.java
│   │   │   │   ├── TagService.java
│   │   │   │   ├── TopicService.java
│   │   │   │   ├── AiTopicService.java
│   │   │   │   ├── ApplicationService.java
│   │   │   │   ├── ThesisService.java
│   │   │   │   └── NotificationService.java
│   │   │   ├── controller/          # 控制器层（REST API）
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── HelloController.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── config/              # 配置类
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── security/            # 安全相关
│   │   │   │   └── JwtAuthenticationFilter.java
│   │   │   ├── util/                # 工具类
│   │   │   │   └── JwtUtil.java
│   │   │   ├── algo/                # 算法类
│   │   │   │   └── DuplicateCheckAlgorithm.java
│   │   │   ├── common/              # 通用类
│   │   │   │   ├── ApiResponse.java
│   │   │   │   └── UserContext.java
│   │   │   └── GraduationApplication.java
│   │   └── resources/
│   │       ├── db/
│   │       │   └── schema.sql       # 数据库表结构SQL
│   │       ├── application.yml      # 开发环境配置
│   │       └── application-prod.yml # 生产环境配置
│   └── test/
├── pom.xml                          # Maven依赖配置
├── start.bat                        # Windows启动脚本
├── nginx.conf.example               # Nginx反向代理配置示例
├── DEPLOY.md                        # 部署文档
└── PROJECT_STRUCTURE.md             # 本文件
```

## 模块说明

### 1. Entity（实体层）

所有实体类对应数据库表，使用 MyBatis-Plus 注解：
- `@TableName` - 指定表名
- `@TableId(type = IdType.AUTO)` - 主键自增

**主要实体**：
- `User` - 用户表（学生/导师/管理员）
- `StudentProfile` - 学生信息表
- `TeacherProfile` - 导师信息表
- `Topic` - 选题表
- `TopicApplication` - 选题申请表
- `Thesis` - 论文表
- `Notification` - 通知表
- `UserTag` - 用户标签关联表
- `TopicTag` - 选题标签关联表

### 2. Mapper（数据访问层）

所有 Mapper 继承 `BaseMapper<T>`，MyBatis-Plus 提供基础 CRUD 方法。

### 3. Service（业务逻辑层）

**核心服务**：

- **AuthService** - 用户认证服务
  - 登录验证
  - 密码加密校验（BCrypt）

- **StudentService** - 学生信息服务
  - 完善学生信息
  - 标签管理

- **TeacherService** - 导师信息服务
  - 完善导师信息
  - 研究方向标签生成

- **TagService** - 标签服务
  - 自动生成标签（基于研究方向/兴趣描述）
  - 标签权重管理（研究方向权重0.9）

- **TopicService** - 选题管理服务
  - CRUD操作
  - 去重检测
  - 审核流转（DRAFT → PENDING_REVIEW → OPEN/REJECTED）

- **AiTopicService** - AI选题生成服务
  - 基于标签生成候选选题（模拟实现）
  - 可扩展对接真实大模型API

- **ApplicationService** - 申请管理服务
  - 提交申请
  - 导师审核申请
  - 匹配度排序

- **ThesisService** - 论文管理服务
  - 论文上传
  - 论文列表查询

- **NotificationService** - 通知服务
  - 创建通知
  - 未读消息统计

### 4. Controller（控制器层）

**主要控制器**：
- `AuthController` - 认证接口（`/api/auth/login`）
- `HelloController` - 健康检查（`/api/ping`）
- `GlobalExceptionHandler` - 全局异常处理

**待创建控制器**（需要补充）：
- `StudentController` - 学生端接口
- `TeacherController` - 导师端接口
- `AdminController` - 管理员端接口
- `TopicController` - 选题相关接口
- `ApplicationController` - 申请相关接口
- `ThesisController` - 论文相关接口
- `NotificationController` - 通知相关接口

### 5. Security（安全模块）

- **SecurityConfig** - Spring Security配置
  - JWT认证
  - 角色权限控制（ADMIN/TEACHER/STUDENT）
  - 路径访问规则

- **JwtAuthenticationFilter** - JWT过滤器
  - 从请求头提取Token
  - 验证Token并设置用户上下文

- **JwtUtil** - JWT工具类
  - Token生成
  - Token解析
  - Token验证

### 6. Algorithm（算法模块）

- **DuplicateCheckAlgorithm** - 去重检测算法
  - Jaccard相似度计算
  - 余弦相似度计算
  - 融合相似度（权重：Jaccard 0.4，Cosine 0.6）
  - 相似度阈值：0.7

### 7. Common（通用模块）

- **ApiResponse** - 统一响应封装
  - 成功响应：`ApiResponse.success(data)`
  - 错误响应：`ApiResponse.error(message)`

- **UserContext** - 用户上下文工具
  - 获取当前登录用户ID
  - 获取当前用户角色

## 数据流

### 登录流程
1. 客户端调用 `/api/auth/login`
2. `AuthController` 接收请求
3. `AuthService` 验证用户名密码
4. 生成JWT Token返回
5. 客户端存储Token，后续请求在Header中携带

### 请求认证流程
1. 请求到达 `JwtAuthenticationFilter`
2. 从Header提取Token
3. `JwtUtil` 验证Token
4. 设置用户信息到 `SecurityContext`
5. 继续处理业务逻辑

### 选题提交审核流程
1. 导师创建选题（状态：DRAFT）
2. 调用去重检测算法
3. 通过后提交审核（状态：PENDING_REVIEW）
4. 管理员审核（状态：OPEN/REJECTED）
5. 发送通知

### 申请流程
1. 学生查看开放选题
2. 提交申请（匹配度计算）
3. 导师按匹配度排序查看申请
4. 导师审核（通过/拒绝）
5. 发送通知给学生

## 待完善功能

1. **Controller层**：需要补充所有业务接口的Controller
2. **文件上传**：需要实现文件存储（本地或OSS）
3. **推荐算法**：匹配度计算算法（待实现）
4. **AI选题生成**：可以对接真实大模型API
5. **标签提取**：可以集成jieba分词等NLP工具

## 技术特点

1. **模块化设计**：按功能划分模块，职责清晰
2. **统一异常处理**：`GlobalExceptionHandler` 统一处理异常
3. **JWT无状态认证**：支持分布式部署
4. **事务管理**：关键操作使用 `@Transactional`
5. **参数校验**：使用 `@Valid` 和 `@NotBlank` 等注解

