## 选题匹配算法升级方案（标签重合度 → 向量语义匹配）

本文档用于记录从**基于标签重合度的基础匹配算法**，升级到**“标签匹配 + 语义向量匹配”融合模型**的工程方案和工作流，作为实现与论文撰写的统一依据。

---

## 一、目标与约束

- **目标**
  - 在不破坏现有业务流程的前提下，将当前的 `match_score` 从“标签重合度单一来源”升级为“标签重合度 + 文本语义向量相似度”的综合得分。
  - 保持与现有数据库结构（`topic_application.match_score`）和前端展示方式兼容，支持灰度切换与回滚。

- **约束与假设**
  - 优先维持当前「学生提交选题申请时实时计算匹配度」的交互体验。
  - 算法升级应通过**配置开关**控制，可在 `TAG_ONLY` 与 `HYBRID`（标签+向量融合）之间切换。
  - 允许调用本地或远程的向量模型服务（如 sentence-transformers / bge），调用失败时必须能够退回到纯标签方案。

---

## 二、总体方案概览

### 2.1 算法形态

升级后的匹配度由两部分组成：

- **标签匹配分数**：沿用现有“标签重合度 + 权重叠加”方案，得到 `score_tag ∈ [0, 1]`。
- **语义向量分数**：对“学生画像文本”和“选题文本”分别编码为向量，计算余弦相似度，得到 `score_semantic ∈ [0, 1]`。

两者融合为最终分数：

- `score_final_raw = w_tag * score_tag + w_sem * score_semantic`，其中 `w_tag + w_sem = 1`（初始可设为 `w_tag = 0.6, w_sem = 0.4`）。
- 再映射到现有前端使用的 \[0.30, 1.00] 区间：
  - `score_final = 0.30 + score_final_raw * 0.70`（必要时可根据实际使用反馈调整区间）。

### 2.2 数据流与调用点

- **写入点**：学生提交选题申请时，后端在创建 `topic_application` 记录前后调用统一的匹配服务，计算 `score_final` 写入 `topic_application.match_score`。
- **读取点**：导师端“申请处理”列表继续按 `match_score` 排序，前端逻辑无需改变。
- **兼容性**：当语义向量暂不可用时（如模型服务异常），仅使用标签分 `score_tag`，并照旧映射为 \[0.30, 1.00]，保证系统可用性。

---

## 三、数据与存储设计

### 3.1 新增/扩展字段

在选题表或相关扩展表中保存选题文本向量，推荐两种实现方式（二选一）：

1. **在 `topic` 表中增加向量字段**
   - 新增字段示例：
     - `embedding`：存储向量，类型可为 `VARBINARY` 或 `TEXT` / `JSON`（序列化后的浮点数组）。
   - 优点：结构简单，读取方便。
   - 风险：`topic` 表行较多时，单行变大，可能稍微影响 I/O。

2. **单独创建选题向量表 `topic_embedding`**
   - 字段示例：
     - `topic_id`：关联 `topic.id`。
     - `vector`：向量数据（同样用二进制或 JSON 存储）。
   - 优点：更灵活，后续可方便替换/重建。
   - 缺点：查询时需要一次 JOIN 或二次查询。

> 实际实现中可根据当前数据库规模和习惯选择一种方式。无论采用哪种方式，本方案中统称为“选题 embedding 存储”。

### 3.2 文本来源与更新触发

- **选题向量文本构成**
  - `title + description + 主要标签文本（如 topic_tag / 手工标签）`
  - 统一转换为小写，并做适度清洗（去掉 HTML 标签、多余空白等）。

- **更新触发条件**
  - 新建选题：创建 `topic` 后，异步生成对应的 embedding。
  - 编辑选题：当 `title` / `description` / 关键标签发生变化时，标记该选题为“待重算 embedding”，并在后台任务中异步处理。

---

## 四、服务封装与工具层设计

### 4.1 EmbeddingService 抽象

- 定义统一接口（伪代码）：

```java
public interface EmbeddingService {
    /**
     * 对给定文本生成语义向量。
     * @return 归一化前或归一化后的 float[]，异常时返回 null。
     */
    float[] embedText(String text);
}
```

- 实现要求：
  - 支持连接**本地模型服务**或**远程 API**。
  - 加入**超时、重试、错误日志**，超时或失败时返回 `null`，上层逻辑自动退回纯标签方案。
  - 后续若更换模型，只需替换实现类或配置，不影响调用方。

### 4.2 向量工具类

- 实现基本的向量操作工具：

```java
public final class VectorUtils {
    public static float[] normalize(float[] v) { ... }
    public static double cosineSimilarity(float[] a, float[] b) { ... } // 返回 [0,1]
}
```

- 要点：
  - 防止除以零（全零向量）时抛异常，返回一个中性值或 0.5。
  - 可以选择在存储前或计算前进行一次归一化，减轻后续计算负担。

### 4.3 简易向量检索

> 本阶段不强制引入 Faiss/PGVector 等专门的向量库，先用遍历方式完成 MVP。

- 在需要“为某个学生查找一批最匹配的题目”时：
  - 从数据库读出所有 `OPEN` 或候选状态的 `topic` 及其向量；
  - 逐个计算与学生向量的余弦相似度；
  - 排序后取 Top-N 返回前端。

> 若后续数据量增大，可单独扩展为基于 Faiss / PGVector / Elastic dense_vector 的高效近似最近邻检索。

---

## 五、离线任务与数据填充

### 5.1 选题向量批量生成 Job

- 编写一个后台任务或命令行入口（如 Spring Boot 定时任务 / 手动触发 Runner）：
  - 扫描所有 `topic` 记录；
  - 对当前缺失或标记“需要重算”的选题，调用 `EmbeddingService.embedText` 生成向量；
  - 成功则写回数据库，失败则记录日志并做简单重试。

- 设计要点：
  - 分批处理（按 `id` 或时间分页），避免一次性占满模型或数据库连接；
  - 记录进度，以便中断后可以从上次处理的 `topic_id` 继续；
  - 对外暴露一个“仅测试环境使用”的触发接口或命令，便于开发调试。

### 5.2 增量维护策略

- 在 `TopicService` 中，在“新增/修改选题”的业务逻辑后追加：
  - 提交一个异步任务，将该 `topic` 加入“待生成 embedding 队列”；
  - 由专门的消费者/定时任务消费队列，调用 `EmbeddingService` 生成和更新向量。

---

## 六、匹配服务升级与业务接入

### 6.1 统一 MatchService 接口

- 建议将所有匹配度计算通过一个服务类完成，例如：

```java
public interface MatchService {
    /**
     * 计算给定学生与选题之间的匹配度（0.30–1.00）。
     */
    double calculateMatchScore(Long studentId, Long topicId);
}
```

- 内部实现步骤：
  1. 根据 `studentId` 查出 `user_tag` 等信息，沿用现有逻辑计算 `score_tag ∈ [0,1]`。
  2. 尝试构造学生的语义文本（兴趣描述、专业、标签 Top-K 等）并编码为向量；
  3. 从缓存或数据库读取 `topic` 对应的向量；
  4. 如果向量齐全，则计算 `score_semantic = cosineSimilarity(studentVec, topicVec)`；否则设置为默认值（如 0.5）。
  5. 根据当前配置选择算法：
     - `TAG_ONLY`：忽略 `score_semantic`，只用标签分；
     - `HYBRID`：按配置好的 `w_tag`、`w_sem` 进行融合。
  6. 将 `score_final_raw` 映射到 \[0.30, 1.00] 区间，作为最终返回值。

### 6.2 与 `topic_application` 的结合

- 在学生提交选题申请的 Service 方法中：
  - 调用 `matchService.calculateMatchScore(studentId, topicId)`；
  - 将返回值写入 `topic_application.match_score`；
  - 其余流程保持不变。

- 可选扩展：
  - 增加 `match_score_tag` / `match_score_semantic` 字段，方便离线分析与论文示例；如不想改变表结构，可仅在日志中记录。

---

## 七、前端与产品层调整

### 7.1 展示方式

- 导师端“申请列表”仍然只展示一个总匹配度数值，避免 UI 震荡。
- 如需提升可解释性，可增加：
  - 在详情弹窗中列出“标签命中列表”（目前已有标签数据，易于呈现）；
  - 在 tooltip 中简单说明“该分数由标签匹配和语义匹配综合得出”。

### 7.2 配置开关与灰度策略

- 在后端配置中增加算法开关，例如：

```yaml
match:
  algorithm: HYBRID  # 可选：TAG_ONLY / HYBRID
  weight:
    tag: 0.6
    semantic: 0.4
```

- 上线策略：
  - 开发 / 测试环境先启用 `HYBRID`，对比 TAG_ONLY 的效果；
  - 生产环境可先保守使用 TAG_ONLY，在验证稳定后再灰度切换到 HYBRID；
  - 出现异常时可快速切回 TAG_ONLY。

---

## 八、评估与调参与运维

### 8.1 效果评估

- 观察指标示例：
  - 高匹配度申请的**通过率**是否显著高于低匹配度申请；
  - 导师主观反馈：排序前几位学生与自身判断的贴合度；
  - 匹配度分布是否过于集中或两极分化。

- 根据评估结果调整：
  - `w_tag` / `w_sem` 权重；
  - 标签权重策略（对不同类型标签赋予不同权重）。

### 8.2 监控与告警

- 对 Embedding 调用的**成功率、平均时延、QPS**进行基础监控；
- 对 `match_score` 的整体分布做周期性统计，及时发现异常（如大面积为 0.30 或 1.00）。

---

## 九、本地部署 sentence-transformers / bge 模型说明

本节给出在本地部署 sentence-transformers / bge 模型，并以 HTTP 服务形式供后端调用的大致流程（以 Python + FastAPI 为例，便于后端通过 `RestTemplate` 或 WebClient 访问）。

### 9.1 环境准备

1. 安装 Python 与虚拟环境：
   - 安装 Python 3.9+；
   - 创建虚拟环境并激活（`python -m venv venv`，然后 `venv\Scripts\activate` / `source venv/bin/activate`）。
2. 安装依赖：
   - `pip install fastapi uvicorn sentence-transformers`  
   - 如使用 bge 模型，可安装：`pip install transformers accelerate` 并在模型加载时选择 bge 系列。

### 9.2 启动本地向量服务（示例）

示例代码结构（简化）：

```python
from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI()

# 选择模型：可用 bge-m3、bge-base-zh 等
model = SentenceTransformer("BAAI/bge-m3")

class EmbedRequest(BaseModel):
    text: str

class EmbedResponse(BaseModel):
    vector: list[float]

@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    emb = model.encode(req.text, normalize_embeddings=True)
    return EmbedResponse(vector=emb.tolist())
```

运行服务：

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

此时，后端可通过 `POST http://localhost:8000/embed` 调用得到向量。

### 9.3 后端接入本地向量服务

- 在 Spring Boot 配置中增加服务地址：

```yaml
embedding:
  base-url: http://localhost:8000
  timeout-ms: 3000
```

- 在 `EmbeddingService` 实现类中使用 `RestTemplate` / WebClient 调用 `/embed` 接口，将返回的 `vector` 字段解析为 `float[]`：

```java
// 伪代码示意
ResponseEntity<EmbedResponse> resp = restTemplate.postForEntity(
    baseUrl + "/embed",
    new EmbedRequest(text),
    EmbedResponse.class
);
float[] vector = toFloatArray(resp.getBody().getVector());
```

- 加入错误处理：
  - 超时或网络异常时返回 `null`；
  - 上层 `MatchService` 发现向量缺失时，仅使用标签匹配分。

### 9.4 性能与部署建议

- **GPU 与 CPU**：
  - 有 GPU 时优先使用 GPU 加速；如果没有，bge/sentence-transformers 在 CPU 上也可以，只是吞吐量有限。
- **并发与批量**：
  - 若调用频繁，可考虑在模型服务端支持一次性编码多个文本（batch），减少 HTTP 往返；
  - 对于“离线批量生成选题 embedding”的任务，建议直接在模型服务所在机器上运行脚本，写回数据库。
- **容器化**：
  - 可将上述 FastAPI + 模型打包为 Docker 镜像，配合 docker-compose 在内网统一启动，便于在不同环境（dev/test/prod）下复用。

---

## 十、后续扩展方向（注意力/加权机制）

在完成基础的“标签 + 语义向量”融合后，如需进一步研究和实现注意力或更复杂的加权机制，可以在现有框架上演进：

- 在标签侧为不同类型标签（研究方向、工具技能、发展需求等）引入不同的权重或注意力系数；
- 在语义侧引入“学生多段文本 + 选题多段文本”的多向量匹配，对不同段落赋予不同权重；
- 把历史评价数据（`topic_metrics`）作为额外因子，形成“匹配度 × 质量权重”的综合评分。

这些扩展都可以在不改变现有表结构和基础调用约定的前提下逐步引入，保证系统在“可用 → 好用 → 更智能”的路径上平滑演进。

