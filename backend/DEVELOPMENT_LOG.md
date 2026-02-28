# 开发日志

记录项目开发过程中的重要变更和功能实现。

## 2024-12-XX - 基础功能模块完成

### 已完成功能模块

#### 1. 数据库层
- ✅ 完成11张数据库表的设计
- ✅ 创建SQL建表脚本（`src/main/resources/db/schema.sql`）
- ✅ 创建所有实体类（Entity）
- ✅ 创建所有Mapper接口（继承BaseMapper）

**表结构**：
- user - 用户表
- student_profile - 学生信息表
- teacher_profile - 导师信息表
- tag - 标签字典表
- user_tag - 用户标签关联表
- topic - 选题表
- topic_tag - 选题标签关联表
- topic_review - 选题审核记录表
- topic_application - 选题申请表
- thesis - 论文表
- notification - 通知表

#### 2. 认证与安全模块
- ✅ 实现JWT工具类（JwtUtil）
  - Token生成
  - Token解析
  - Token验证
- ✅ 实现JWT认证过滤器（JwtAuthenticationFilter）
- ✅ 更新SecurityConfig配置
  - 基于角色的访问控制
  - 路径权限配置（/api/admin/**、/api/teacher/**、/api/student/**）
- ✅ 实现用户登录功能（AuthService、AuthController）
- ✅ BCrypt密码加密集成

#### 3. 用户信息管理模块
- ✅ 实现StudentService
  - 完善学生信息
  - 自动生成标签（基于兴趣描述和专业）
- ✅ 实现TeacherService
  - 完善导师信息
  - 自动生成标签（研究方向标签权重0.9）
- ✅ 实现TagService
  - 标签自动提取
  - 标签权重管理
  - 标签合并去重

#### 4. 选题管理模块
- ✅ 实现TopicService
  - 选题CRUD操作
  - 选题去重检测（调用算法）
  - 选题审核流转
  - 状态管理（DRAFT → PENDING_REVIEW → OPEN/REJECTED）
- ✅ 实现AiTopicService
  - 基于标签生成候选选题（模拟实现）
  - 支持导师和学生两种生成方式
- ✅ 实现DuplicateCheckAlgorithm算法
  - Jaccard相似度计算
  - 余弦相似度计算
  - 融合相似度（权重：Jaccard 0.4，Cosine 0.6）
  - 相似度阈值：0.7

#### 5. 申请管理模块
- ✅ 实现ApplicationService
  - 学生提交申请
  - 申请冲突校验（人数限制、重复申请、已有通过申请）
  - 导师审核申请
  - 按匹配度排序
  - 自动更新选题当前申请人数

#### 6. 论文管理模块
- ✅ 实现ThesisService
  - 论文上传
  - 申请状态验证
  - 论文列表查询（学生/导师）

#### 7. 通知系统模块
- ✅ 实现NotificationService
  - 创建通知
  - 查询通知列表
  - 标记已读
  - 未读消息统计

#### 8. 工具与通用类
- ✅ 更新ApiResponse（统一响应封装）
  - 添加success()无参方法
  - 添加error(code, message)方法
- ✅ 创建UserContext（用户上下文工具）
- ✅ 创建GlobalExceptionHandler（全局异常处理）

#### 9. 项目配置
- ✅ 更新GraduationApplication（添加@MapperScan）
- ✅ 创建PROJECT_STRUCTURE.md（项目结构说明文档）

### 待完成工作

1. **Controller层补充**
   - StudentController - 学生端接口
   - TeacherController - 导师端接口
   - AdminController - 管理员端接口
   - TopicController - 选题相关接口
   - ApplicationController - 申请相关接口
   - ThesisController - 论文相关接口
   - NotificationController - 通知相关接口

2. **文件上传功能**
   - 实现文件存储（本地或OSS）
   - 文件上传Controller

3. **推荐算法**（按需求暂不实现）
   - 基于标签的匹配度计算
   - 个性化推荐列表

4. **其他优化**
   - 完善标签提取算法（可集成jieba分词）
   - AI选题生成可对接真实大模型API
   - 单元测试编写

### 技术要点

1. **模块化设计**：按功能划分Service，职责清晰
2. **事务管理**：关键操作使用@Transactional保证数据一致性
3. **异常处理**：统一的异常处理机制
4. **算法实现**：去重检测算法融合Jaccard和余弦相似度
5. **状态流转**：完整的业务状态流转逻辑

### 文件清单

**新增文件**：
- entity/*.java (11个实体类)
- mapper/*.java (11个Mapper接口)
- service/*.java (8个Service类)
- controller/AuthController.java
- controller/GlobalExceptionHandler.java
- security/JwtAuthenticationFilter.java
- util/JwtUtil.java
- algo/DuplicateCheckAlgorithm.java
- common/UserContext.java
- dto/LoginRequest.java, LoginResponse.java
- resources/db/schema.sql
- PROJECT_STRUCTURE.md
- DEVELOPMENT_LOG.md

**修改文件**：
- config/SecurityConfig.java (更新JWT配置)
- common/ApiResponse.java (添加方法)
- GraduationApplication.java (添加@MapperScan)

