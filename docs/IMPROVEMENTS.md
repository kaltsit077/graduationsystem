# 项目改进建议

本文档列出了对毕业论文选题与反馈系统的改进建议，按优先级和类别分类。

## 🔴 高优先级改进（安全和稳定性）

### 1. 日志系统改进

**问题**：
- `GlobalExceptionHandler` 使用 `printStackTrace()` 输出异常堆栈
- `AdminInitializer` 使用 `System.out.println()` 输出信息
- 缺少统一的日志记录机制

**改进建议**：
```java
// 在 GlobalExceptionHandler 中
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception e) {
        logger.error("服务器内部错误", e);  // 使用日志框架
        return ApiResponse.error("服务器内部错误");
    }
}
```

**影响**：提高问题排查效率，符合生产环境最佳实践

---

### 2. 异常处理细化

**问题**：
- 所有业务异常都使用 `RuntimeException`，不够具体
- 缺少自定义异常类型
- 异常信息可能暴露内部实现细节

**改进建议**：
```java
// 创建自定义异常类
public class BusinessException extends RuntimeException {
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = -1;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}

// 在 GlobalExceptionHandler 中处理
@ExceptionHandler(BusinessException.class)
public ApiResponse<Object> handleBusinessException(BusinessException e) {
    return ApiResponse.error(e.getCode(), e.getMessage());
}
```

**影响**：更好的错误分类和处理，提升代码可维护性

---

### 3. 敏感信息保护

**问题**：
- `application.yml` 中数据库密码硬编码
- JWT secret 使用默认值
- 管理员密码在配置文件中明文存储（虽然只是默认值）

**改进建议**：
1. 使用环境变量或配置中心管理敏感信息
2. 在 `application-prod.yml` 中通过环境变量注入：
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:}
jwt:
  secret: ${JWT_SECRET:}
```
3. 添加配置验证，确保生产环境不使用默认值

**影响**：提高系统安全性，防止敏感信息泄露

---

### 4. 性能优化 - 去重检测算法

**问题**：
- `TopicService.checkDuplicate()` 查询所有历史选题，数据量大时性能差
- 缺少分页和索引优化

**改进建议**：
```java
// 1. 添加分页查询
public DuplicateCheckResult checkDuplicate(Long topicId, String title, String description) {
    // 只查询最近N条或使用分页
    Page<Topic> page = new Page<>(1, 100);  // 限制查询数量
    List<Topic> recentTopics = topicMapper.selectPage(page, wrapper).getRecords();
    
    // 2. 或者添加数据库索引
    // CREATE INDEX idx_topic_title ON topic(title);
    // CREATE FULLTEXT INDEX idx_topic_fulltext ON topic(title, description);
}
```

**影响**：提升系统性能，特别是在数据量大的情况下

---

## 🟡 中优先级改进（代码质量和可维护性）

### 5. API 文档生成

**问题**：
- 缺少 API 接口文档
- 前端开发需要手动查看代码了解接口

**改进建议**：
添加 Swagger/OpenAPI 支持：
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

```java
// 在 Controller 中添加注解
@Operation(summary = "用户登录", description = "通过用户名和密码登录")
@PostMapping("/login")
public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    // ...
}
```

**影响**：提升开发效率，便于前后端协作

---

### 6. 输入验证增强

**问题**：
- 部分接口缺少完整的输入验证
- 验证错误信息不够友好

**改进建议**：
```java
// 在 DTO 中添加更多验证注解
public class TopicRequest {
    @NotBlank(message = "选题标题不能为空")
    @Size(min = 5, max = 100, message = "选题标题长度应在5-100字符之间")
    private String title;
    
    @NotBlank(message = "选题描述不能为空")
    @Size(min = 10, max = 2000, message = "选题描述长度应在10-2000字符之间")
    private String description;
}
```

**影响**：提高数据质量，减少无效请求

---

### 7. 单元测试

**问题**：
- 缺少单元测试
- 核心业务逻辑没有测试覆盖

**改进建议**：
```java
// 示例：为 Service 层添加测试
@SpringBootTest
class TopicServiceTest {
    @Autowired
    private TopicService topicService;
    
    @Test
    void testCreateTopic() {
        // 测试创建选题功能
    }
}
```

**影响**：提高代码质量，减少 bug，便于重构

---

### 8. 分页查询支持

**问题**：
- 列表查询接口缺少分页功能
- 数据量大时可能影响性能

**改进建议**：
```java
// 在 Controller 中添加分页参数
@GetMapping("/topics")
public ApiResponse<PageResult<TopicResponse>> getTopics(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) Topic.TopicStatus status) {
    Page<Topic> pageObj = new Page<>(page, size);
    // 分页查询逻辑
}
```

**影响**：提升用户体验，优化性能

---

## 🟢 低优先级改进（功能增强和优化）

### 9. 缓存机制

**问题**：
- 频繁查询的数据没有缓存
- 标签、用户信息等相对静态的数据每次都查数据库

**改进建议**：
```java
// 使用 Spring Cache
@Cacheable(value = "topics", key = "#status")
public List<Topic> getTopics(Topic.TopicStatus status, Long teacherId) {
    // ...
}
```

**影响**：减少数据库压力，提升响应速度

---

### 10. 文件上传功能完善

**问题**：
- 论文上传功能缺少文件类型验证
- 缺少文件存储路径配置
- 没有文件大小限制的友好提示

**改进建议**：
```java
// 添加文件类型验证
@PostMapping("/upload")
public ApiResponse<ThesisResponse> uploadThesis(
        @RequestParam("file") MultipartFile file) {
    // 验证文件类型
    String contentType = file.getContentType();
    if (!isAllowedFileType(contentType)) {
        throw new BusinessException("不支持的文件类型");
    }
    // ...
}
```

**影响**：提高安全性，改善用户体验

---

### 11. 前端错误处理优化

**问题**：
- 部分页面可能缺少加载状态提示
- 错误信息展示可以更友好

**改进建议**：
```typescript
// 在 API 调用中添加 loading 状态
const loading = ref(false)

const fetchTopics = async () => {
  loading.value = true
  try {
    const res = await topicApi.getTopics()
    // ...
  } catch (error) {
    // 更详细的错误处理
  } finally {
    loading.value = false
  }
}
```

**影响**：提升用户体验

---

### 12. 数据库连接池优化

**问题**：
- 连接池配置可能需要根据实际负载调整
- 缺少连接池监控

**改进建议**：
```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000  # 添加连接泄漏检测
```

**影响**：提高数据库连接管理效率

---

### 13. 代码规范统一

**问题**：
- 部分代码风格不统一
- 缺少代码格式化配置

**改进建议**：
1. 添加 `.editorconfig` 文件
2. 配置 IDE 代码格式化规则
3. 使用 Checkstyle 或 SpotBugs 进行代码检查

**影响**：提高代码可读性和可维护性

---

### 14. 监控和健康检查

**问题**：
- 缺少应用监控指标
- 健康检查接口过于简单

**改进建议**：
```java
// 使用 Spring Boot Actuator
@GetMapping("/actuator/health")
// 添加数据库连接检查、磁盘空间检查等
```

**影响**：便于运维监控和问题排查

---

## 📋 改进优先级总结

### 立即实施（高优先级）
1. ✅ 日志系统改进
2. ✅ 异常处理细化
3. ✅ 敏感信息保护
4. ✅ 性能优化 - 去重检测算法

### 近期实施（中优先级）
5. ✅ API 文档生成
6. ✅ 输入验证增强
7. ✅ 单元测试
8. ✅ 分页查询支持

### 长期规划（低优先级）
9. ✅ 缓存机制
10. ✅ 文件上传功能完善
11. ✅ 前端错误处理优化
12. ✅ 数据库连接池优化
13. ✅ 代码规范统一
14. ✅ 监控和健康检查

---

## 🛠️ 实施建议

1. **分阶段实施**：先解决高优先级问题，再逐步完善其他功能
2. **测试驱动**：每次改进后都要进行充分测试
3. **文档更新**：改进后及时更新相关文档
4. **代码审查**：重要改进建议进行代码审查

---

**最后更新**：2025-01-XX

