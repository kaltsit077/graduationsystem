# 论文 UML 图代码（PlantUML）

说明：本文件提供可直接转换的 UML 代码，覆盖用例图、系统设计架构图、类图、包图。  
使用方式：将任一代码块复制到 PlantUML 渲染器（本地插件/在线工具）即可出图。

---

## 1）系统角色用例图（Use Case）

```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle

actor 学生 as Student
actor 导师 as Teacher
actor 管理员 as Admin

rectangle "毕业论文选题协同系统" {
  usecase "维护个人画像与标签" as UC_Profile
  usecase "浏览选题列表\n（含匹配分数）" as UC_BrowseTopic
  usecase "浏览导师列表\n（含历史指标）" as UC_BrowseTeacher
  usecase "提交选题申请" as UC_ApplyTopic
  usecase "提交拜师申请" as UC_ApplyMentor
  usecase "提交变更申请\n（换题/换导师）" as UC_Change
  usecase "上传阶段文档/论文" as UC_Upload
  usecase "学生满意度反馈" as UC_Feedback

  usecase "创建/编辑选题" as UC_EditTopic
  usecase "AI 生成候选题目" as UC_AIGen
  usecase "候选题去重过滤" as UC_Dedup
  usecase "提交选题审核" as UC_SubmitReview
  usecase "处理学生申请\n（通过/拒绝）" as UC_ProcessApply
  usecase "审核阶段文档" as UC_ReviewThesis
  usecase "录入成绩与评语" as UC_Evaluate

  usecase "审核选题" as UC_AdminReview
  usecase "设置选题开放时间窗" as UC_SetWindow
  usecase "查看导师负荷与质量统计" as UC_Dashboard
  usecase "查看运行状态与日志" as UC_Monitor
}

Student --> UC_Profile
Student --> UC_BrowseTopic
Student --> UC_BrowseTeacher
Student --> UC_ApplyTopic
Student --> UC_ApplyMentor
Student --> UC_Change
Student --> UC_Upload
Student --> UC_Feedback

Teacher --> UC_EditTopic
Teacher --> UC_AIGen
Teacher --> UC_SubmitReview
Teacher --> UC_ProcessApply
Teacher --> UC_ReviewThesis
Teacher --> UC_Evaluate

Admin --> UC_AdminReview
Admin --> UC_SetWindow
Admin --> UC_Dashboard
Admin --> UC_Monitor

UC_AIGen --> UC_Dedup : <<include>>
UC_ApplyTopic --> UC_BrowseTopic : <<extend>>
UC_ApplyMentor --> UC_BrowseTeacher : <<extend>>

note right of UC_Profile
学生维护兴趣、能力、规划信息，
系统据此生成/调整标签画像。
end note

note right of UC_BrowseTopic
学生查看开放题目，
列表可附带匹配分数排序参考。
end note

note right of UC_ApplyTopic
学生按题目直接申请，
进入统一申请审批流程。
end note

note right of UC_AIGen
导师输入约束条件后，
系统批量生成候选题目草稿。
end note

note right of UC_Dedup
对候选题做相似度检测，
过滤高重复风险条目。
end note

note right of UC_ProcessApply
导师审批学生申请，
输出通过/拒绝并反馈原因。
end note

note right of UC_AdminReview
管理员审核题目规范性与可行性，
通过后题目进入开放状态。
end note

note right of UC_Monitor
管理员查看系统状态与日志，
用于故障定位与运行保障。
end note

note "整体关系说明：\n1) 学生、导师、管理员三类角色围绕统一申请实体协作；\n2) 导师命题链路包含 AI 生成与去重过滤；\n3) 管理员承担审核与系统治理职责。" as UC_ALL
UC_ALL .. UC_ApplyTopic
@enduml
```

---

## 2）系统设计架构图（Architecture / Component）

```plantuml
@startuml
skinparam componentStyle rectangle
left to right direction

actor 学生端 as Student
actor 导师端 as Teacher
actor 管理端 as Admin

package "Frontend (Vue3 + TS)" {
  [Student UI]
  [Teacher UI]
  [Admin UI]
}

package "Backend (Spring Boot)" {
  [Controller Layer] as C
  [Service Layer] as S
  [MatchServiceImpl]
  [AiTagService]
  [AiTopicService]
  [EmbeddingServiceImpl]
  [Mapper Layer] as M
}

database "MySQL" as DB
cloud "LLM API" as LLM
node "Embedding API\n(/embed)" as EMB

Student --> [Student UI]
Teacher --> [Teacher UI]
Admin --> [Admin UI]

[Student UI] --> C : REST API
[Teacher UI] --> C : REST API
[Admin UI] --> C : REST API

C --> S
S --> [MatchServiceImpl]
S --> [AiTagService]
S --> [AiTopicService]
S --> [EmbeddingServiceImpl]
S --> M
M --> DB

[AiTagService] --> LLM : 标签抽取
[AiTopicService] --> LLM : 选题生成
[EmbeddingServiceImpl] --> EMB : 语义向量
[MatchServiceImpl] --> [EmbeddingServiceImpl] : 可降级调用

note right of [MatchServiceImpl]
1) 标签重合度
2) 语义相似度（可用时）
3) 导师画像相似度
融合后映射到 [0.30, 1.00]
end note

note right of C
控制层：
接收 HTTP 请求，
完成参数校验与返回包装。
end note

note right of S
服务层：
编排业务流程、事务与规则，
调用 AI/匹配/数据访问能力。
end note

note right of M
数据访问层：
封装 CRUD 与查询，
对接数据库持久化。
end note

note right of [AiTagService]
AI 标签抽取服务：
将自然语言画像转为标签集合。
end note

note right of [AiTopicService]
AI 选题生成服务：
按约束生成候选题目文本。
end note

note right of [EmbeddingServiceImpl]
向量适配服务：
调用 /embed 接口，
失败时返回空触发上层降级。
end note

note "整体关系说明：\n前端三端统一经 REST 调用后端；\n后端按 Controller -> Service -> Mapper 分层；\nAI 与向量能力位于业务边缘，不阻断核心流程。" as ARCH_ALL
ARCH_ALL .. S
@enduml
```

---

## 3）功能模块类图（Class Diagram）

```plantuml
@startuml
skinparam classAttributeIconSize 0

class TopicController {
  +getOpenTopicsWithScore() : 获取开放选题并计算匹配分
  +generateAiTopics() : 生成AI候选题目
  +submitForReview() : 提交选题进入审核流程
}

class ApplicationController {
  +submitApplication() : 学生提交选题申请
  +processApplication() : 导师处理申请结果
}

class EvaluationController {
  +saveEvaluation() : 导师录入成绩与评语
  +saveStudentFeedback() : 学生提交满意度反馈
}

class TopicService {
  +createTopic() : 创建选题
  +updateTopic() : 更新选题
  +checkDuplicate() : 执行选题去重检测
  +submitForReview() : 提交选题审核
}

class ApplicationService {
  +submitApplication() : 提交申请并写入事务
  +processApplication() : 审批申请并更新状态
  +calculateMatchScore() : 计算申请匹配分
}

class EvaluationService {
  +saveEvaluation() : 保存导师评价
  +saveStudentFeedback() : 保存学生反馈
  +recalcTopicMetricsForTeacher() : 重算导师名下选题指标
}

class MatchServiceImpl {
  +calculateMatchScore(topic, studentId) : 计算融合匹配分
  -calculateOverlapScore() : 计算标签重合度
  -calculateSemanticSimilarity() : 计算语义相似度
  -calculateTeacherStudentSimilarity() : 计算师生画像相似度
}

class EmbeddingServiceImpl {
  +embedText(text) : 获取文本向量
  -normalizeText(text) : 文本归一化处理
}

class AiTagService {
  +extractTagNames(text) : 抽取标签关键词
}

class AiTopicService {
  +generateTopicsForTeacher() : 生成导师候选选题
  -parseAiTopics() : 解析大模型返回结果
}

class DuplicateCheckAlgorithm {
  +calculateSimilarity(text1, text2) : 计算文本相似度
  +isPassed(similarity) : 判断是否通过去重阈值
}

class Topic {
  +id: Long
  +teacherId: Long
  +title: String
  +description: String
  +status: TopicStatus
}

class TopicApplication {
  +id: Long
  +topicId: Long
  +studentId: Long
  +status: ApplicationStatus
  +matchScore: BigDecimal
}

class Thesis {
  +id: Long
  +topicId: Long
  +studentId: Long
  +status: ThesisStatus
}

class ThesisEvaluation {
  +id: Long
  +thesisId: Long
  +teacherId: Long
  +score: BigDecimal
  +studentScore: BigDecimal
}

class TopicMetrics {
  +topicId: Long
  +avgScore: BigDecimal
  +excellentRatio: BigDecimal
  +failRatio: BigDecimal
}

note right of TopicController
控制层：
负责接收前端请求、
参数校验与响应返回。
end note

note right of TopicService
业务层（选题）：
负责选题创建、更新、
去重检测与提审流程。
end note

note right of ApplicationService
业务层（申请）：
负责申请提交/处理、
匹配分数计算调用与
事务一致性控制。
end note

note right of MatchServiceImpl
匹配核心：
标签重合度 + 语义相似度 +
导师画像相似度融合。
end note

note right of EmbeddingServiceImpl
向量服务适配层：
调用 /embed 接口；
失败时返回 null，
由上层走降级逻辑。
end note

note right of DuplicateCheckAlgorithm
去重算法层：
Jaccard 与余弦相似度融合，
用于题目重复风险判定。
end note

note right of TopicApplication
申请关系锚点：
关联学生、导师与选题，
承载匹配分数与状态。
end note

note right of ThesisEvaluation
评价闭环实体：
保存导师评分与学生反馈，
支撑质量统计与复盘。
end note

TopicController --> TopicService
TopicController --> AiTopicService
TopicController --> ApplicationService

ApplicationController --> ApplicationService
EvaluationController --> EvaluationService

TopicService --> DuplicateCheckAlgorithm
ApplicationService --> MatchServiceImpl
EvaluationService --> TopicMetrics

MatchServiceImpl --> EmbeddingServiceImpl
AiTagService --> MatchServiceImpl : <<辅助标签建模>>
AiTopicService --> DuplicateCheckAlgorithm : <<去重依赖>>

ApplicationService --> TopicApplication
TopicService --> Topic
EvaluationService --> Thesis
EvaluationService --> ThesisEvaluation

note "依赖关系说明：\nController -> Service -> Entity/Algorithm。\n其中 MatchServiceImpl 依赖 EmbeddingServiceImpl，\n在向量不可用场景下可降级回标签匹配。" as N1
N1 .. MatchServiceImpl
@enduml
```

---

## 4）包图（Package Diagram）

```plantuml
@startuml
skinparam packageStyle rectangle

package "com.example.graduation.controller" as P1 {
  [TopicController]
  [ApplicationController]
  [ThesisController]
  [EvaluationController]
  [AdminController]
  [StudentController]
  [MentorApplicationController]
  [CollabController]
  [NotificationController]
}

package "com.example.graduation.service" as P2 {
  [TopicService]
  [ApplicationService]
  [ThesisService]
  [EvaluationService]
  [StudentService]
  [MentorApplicationService]
  [AiTagService]
  [AiTopicService]
  [NotificationService]
  [SystemSettingService]
  [CollabService]
}

package "com.example.graduation.service.impl" as P2I {
  [MatchServiceImpl]
  [EmbeddingServiceImpl]
}

package "com.example.graduation.mapper" as P3 {
  [TopicMapper]
  [TopicApplicationMapper]
  [ThesisMapper]
  [ThesisEvaluationMapper]
  [TopicMetricsMapper]
  [UserMapper]
  [MentorApplicationMapper]
}

package "com.example.graduation.entity" as P4 {
  [Topic]
  [TopicApplication]
  [Thesis]
  [ThesisEvaluation]
  [TopicMetrics]
  [User]
  [StudentProfile]
  [TeacherProfile]
  [ChangeRequest]
  [MentorApplication]
}

package "com.example.graduation.dto" as P5 {
  [TopicResponse]
  [ApplicationResponse]
  [ThesisResponse]
  [...]
}

package "com.example.graduation.config/security" as P6 {
  [SecurityConfig]
  [CorsConfig]
  [RestTemplateConfig]
  [JwtAuthenticationFilter]
}

package "com.example.graduation.algo" as P7 {
  [DuplicateCheckAlgorithm]
}

P1 --> P2 : 调用业务服务
P1 --> P5 : 请求/响应对象
P2 --> P2I : 具体实现
P2 --> P3 : 数据访问
P2 --> P4 : 领域实体
P2 --> P7 : 算法能力
P3 --> P4 : ORM 映射
P6 --> P1 : 鉴权/跨域/HTTP 客户端

note right of P1
接口层包：
按业务域暴露 REST API，
承接前端请求入口。
end note

note right of P2
业务层包：
封装选题、申请、协作、
评价、通知等核心逻辑。
end note

note right of P2I
实现层包：
放置匹配与向量能力的
具体实现类。
end note

note right of P3
持久化层包：
Mapper 负责实体读写与查询。
end note

note right of P4
领域实体包：
承载数据库映射对象与状态。
end note

note right of P5
传输对象包：
承载请求/响应 DTO，
隔离接口模型与实体模型。
end note

note right of P6
基础设施包：
安全鉴权、跨域配置、
HTTP 客户端等通用能力。
end note

note right of P7
算法包：
提供去重算法等可复用能力。
end note

note "整体关系说明：\n代码遵循分层与分包组织；\n上层依赖下层、同层解耦协作；\n业务层是系统能力聚合中心。" as PKG_ALL
PKG_ALL .. P2
@enduml
```

---

## 5）可选：选题申请时序图（Sequence Diagram）

```plantuml
@startuml
actor 学生 as Student
participant "ApplicationController" as AC
participant "ApplicationService" as AS
participant "MatchServiceImpl" as MS
participant "EmbeddingServiceImpl" as ES
participant "TopicApplicationMapper" as APM
participant "TopicMapper" as TM
participant "NotificationService" as NS

Student -> AC : 提交选题申请(topicId, remark)
AC -> TM : 查询 Topic
AC -> AS : submitApplication(topicId, studentId, remark, matchScore)
AS -> MS : calculateMatchScore(topic, studentId)
MS -> ES : embedText(studentText/topicText)
alt embedding 服务可用
  ES --> MS : vector
else embedding 服务不可用/超时
  ES --> MS : null（回退）
end
MS --> AS : matchScore
AS -> APM : insert(topic_application)
AS -> TM : update current_applicants
AS -> NS : createNotification(teacherId,...)
AS --> AC : application
AC --> Student : 返回申请结果

note right of AC
控制器职责：
接收申请请求并调用服务层。
end note

note right of AS
服务职责：
完成事务写入（申请记录 + 名额更新）
并触发通知。
end note

note right of MS
匹配职责：
融合标签与语义分数，
输出最终匹配结果。
end note

note right of ES
向量职责：
提供 embedding；
不可用时返回空以便降级。
end note

note "整体关系说明：\n该时序展示了“提交申请”主链路；\n向量调用存在可用/不可用分支；\n无论分支如何，主流程都可完成申请入库。" as SEQ_ALL
SEQ_ALL .. AS
@enduml
```

