<template>
  <div class="topics-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题管理</span>
          <div class="header-actions">
            <el-button type="primary" @click="showCreateDialog">创建选题</el-button>
            <el-button type="success" @click="openAiDialog">AI 生成选题</el-button>
          </div>
        </div>
      </template>

      <el-table :data="topics" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="选题标题" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'DRAFT'" type="info">草稿</el-tag>
            <el-tag v-else-if="row.status === 'PENDING_REVIEW'" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已驳回</el-tag>
            <el-tag v-else-if="row.status === 'OPEN'" type="success">已开放</el-tag>
            <el-tag v-else-if="row.status === 'CLOSED'" type="">已关闭</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentApplicants" label="已申请人数" width="120" />
        <el-table-column prop="maxApplicants" label="最大人数" width="100" />
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editTopic(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
              type="success"
              size="small"
              @click="submitReview(row)"
            >
              提交审核
            </el-button>
            <el-button type="info" size="small" @click="checkDuplicate(row)">去重检测</el-button>
            <el-button type="danger" size="small" @click="removeTopic(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑选题对话框：复用统一弹窗样式（蓝条标题 + 红色关闭） -->
    <AppDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="选题标题" required>
          <el-input v-model="form.title" placeholder="请输入选题标题" />
        </el-form-item>
        <el-form-item v-if="useStructuredEdit" label="编辑方式">
          <el-radio-group v-model="editMode" size="small">
            <el-radio-button value="structured">分段编辑</el-radio-button>
            <el-radio-button value="raw">全文编辑</el-radio-button>
          </el-radio-group>
          <div class="edit-mode-hint">分段编辑便于按研究背景、主要内容等逐项修改；全文编辑可查看/修改原始 Markdown。</div>
        </el-form-item>
        <template v-if="editMode === 'structured' && useStructuredEdit">
          <el-form-item label="研究背景">
            <el-input
              v-model="structuredFields.researchBackground"
              type="textarea"
              :rows="2"
              placeholder="结合行业/业务背景说明选题意义"
            />
          </el-form-item>
          <el-form-item label="主要内容">
            <el-input
              v-model="structuredFields.mainContent"
              type="textarea"
              :rows="2"
              placeholder="3–4 个关键研究要点"
            />
          </el-form-item>
          <el-form-item label="能力要求">
            <el-input
              v-model="structuredFields.abilityReq"
              type="textarea"
              :rows="2"
              placeholder="学生需掌握的算法/工具"
            />
          </el-form-item>
          <el-form-item label="数据/资源需求">
            <el-input
              v-model="structuredFields.dataReq"
              type="textarea"
              :rows="2"
              placeholder="完成该题需要的数据或资源"
            />
          </el-form-item>
          <el-form-item label="创新点">
            <el-input
              v-model="structuredFields.innovation"
              type="textarea"
              :rows="2"
              placeholder="与传统方法或常见选题的区别"
            />
          </el-form-item>
        </template>
        <el-form-item v-else label="选题描述" required>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="editMode === 'raw' ? 12 : 4"
            placeholder="请输入选题描述（若为 AI 生成后导入，可先保存再切到分段编辑）"
          />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="form.maxApplicants" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input
            v-model="form.tags"
            placeholder="多个标签用逗号分隔"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTopic" :loading="saving">保存</el-button>
      </template>
    </AppDialog>

    <!-- AI 生成候选选题：复用统一弹窗样式，大尺寸 + 左右分栏 -->
    <AppDialog
      v-model="aiDialogVisible"
      title="AI 生成候选选题"
      width="min(1100px, 92vw)"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="ai-dialog-content">
      <div class="ai-dialog-lr">
        <div class="ai-dialog-left">
          <el-scrollbar height="72vh">
            <el-form :model="aiForm" label-width="140px" style="margin-bottom: 12px">
              <el-form-item label="生成数量">
                <el-input-number v-model="aiForm.count" :min="1" :max="10" />
              </el-form-item>
              <div class="ai-demand-hint">以下为选题需求说明，均为选填。建议至少填写 2–3 项，系统会据此生成更贴近你需求的题目。</div>
              <el-form-item label="行业/业务背景">
                <el-input
                  v-model="aiForm.backgroundHint"
                  type="textarea"
                  :rows="2"
                  placeholder="例如：某类行业或场景下的常见问题、可改进的环节，或你希望学生关注的现实背景……"
                />
              </el-form-item>
              <el-form-item label="主要研究要点">
                <el-input
                  v-model="aiForm.contentHint"
                  type="textarea"
                  :rows="2"
                  placeholder="例如：① 问题分析或现状调研；② 方案/模型设计；③ 验证、评价或应用……"
                />
              </el-form-item>
              <el-form-item label="能力/工具要求">
                <el-input
                  v-model="aiForm.abilityHint"
                  type="textarea"
                  :rows="2"
                  placeholder="例如：数据分析、建模与计算、常用软件或编程基础等，按你课程要求写即可……"
                />
              </el-form-item>
              <el-form-item label="数据/资源">
                <el-input
                  v-model="aiForm.dataHint"
                  type="textarea"
                  :rows="2"
                  placeholder="例如：公开数据、问卷与访谈、案例材料、实验或仿真条件等……"
                />
              </el-form-item>
              <el-form-item label="创新点期望">
                <el-input
                  v-model="aiForm.innovationHint"
                  type="textarea"
                  :rows="2"
                  placeholder="例如：方法上的改进、跨领域结合、或针对新场景的应用思路……"
                />
              </el-form-item>
              <el-form-item label="其他说明">
                <el-input
                  v-model="aiForm.preferenceHint"
                  type="textarea"
                  :rows="2"
                  placeholder="可选：补充难度、学生层次（本科/专硕等）或其它约束"
                />
              </el-form-item>
              <el-form-item label="参与生成的标签" v-if="teacherTags.length">
                <div class="ai-tag-chooser">
                  <el-tag
                    v-for="tag in teacherTags"
                    :key="tag"
                    :type="aiForm.tagNames.includes(tag) ? 'success' : 'info'"
                    class="ai-tag-chip"
                    @click="toggleAiTag(tag)"
                  >
                    {{ tag }}
                  </el-tag>
                  <div class="ai-tag-hint">
                    不勾选时默认使用你画像中的全部标签；勾选后只会用这些标签来生成题目。
                  </div>
                </div>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="aiLoading" @click="generateAiTopicsClick">
                  生成候选选题
                </el-button>
              </el-form-item>
            </el-form>
          </el-scrollbar>
        </div>
        <div class="ai-dialog-right">
          <div v-if="aiTopics.length === 0 && !aiLoading" class="ai-empty-tip">
            暂无候选结果，请填写左侧信息后点击“生成候选选题”。
          </div>
          <template v-else>
            <el-alert
              v-if="aiTopics.length > 0"
              type="info"
              :closable="false"
              show-icon
              class="ai-right-alert"
              description="以下列表仅展示已经通过系统自动去重阈值的候选题目。你可以根据需要修改后再导入到选题列表中。"
            />
            <el-scrollbar v-if="aiTopics.length > 0" height="68vh">
              <div class="ai-topic-list">
                <el-card
                  v-for="(t, index) in aiTopics"
                  :key="index"
                  class="ai-topic-card"
                  shadow="hover"
                >
                  <div class="ai-topic-card-header">
                    <div class="ai-topic-title">{{ t.title }}</div>
                    <div class="ai-topic-meta" v-if="typeof t.maxSimilarity === 'number'">
                      与历史最相似选题相似度：
                      <strong>{{ Math.round((t.maxSimilarity || 0) * 100) }}%</strong>
                      <span v-if="t.similarTopicTitle" style="margin-left: 6px; color: #909399">
                        （参考题：{{ t.similarTopicTitle }}）
                      </span>
                    </div>
                  </div>
                  <div class="ai-topic-desc">{{ t.description || '暂无详细描述。' }}</div>
                  <div v-if="t.tags?.length" class="ai-topic-tags">
                    <span class="ai-topic-tags-label">关键词：</span>
                    <el-tag
                      v-for="tag in t.tags"
                      :key="tag"
                      size="small"
                      class="ai-topic-tag"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                  <div class="ai-topic-actions">
                    <el-button type="primary" size="small" @click="importAiTopic(t)">
                      导入为我的选题
                    </el-button>
                  </div>
                </el-card>
              </div>
            </el-scrollbar>
          </template>
        </div>
      </div>
      </div>
    </AppDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppDialog from '@/components/AppDialog.vue'
import {
  getTopics,
  createTopic,
  updateTopic,
  submitTopicForReview,
  checkTopicDuplicate,
  deleteTopic,
  generateAiTopics,
  type Topic,
  type AiGeneratedTopic
} from '@/api/topic'
import { getTeacherProfile, type TeacherProfileResponse } from '@/api/teacher'

const loading = ref(false)
const topics = ref<Topic[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  title: '',
  description: '',
  maxApplicants: 1,
  tags: ''
})

/** 分段编辑：仅当 description 可解析为五维结构时显示 */
const useStructuredEdit = ref(false)
const editMode = ref<'structured' | 'raw'>('structured')
const structuredFields = ref({
  researchBackground: '',
  mainContent: '',
  abilityReq: '',
  dataReq: '',
  innovation: ''
})

/** 从选题描述中解析五维结构（与 AI 生成格式一致） */
function parseStructuredDescription(desc: string): Record<string, string> | null {
  if (!desc || typeof desc !== 'string') return null
  const s = desc.trim()
  if (!s.includes('研究背景') && !s.includes('主要内容')) return null
  const fields: Record<string, string> = {}
  const entries = [
    { key: 'researchBackground', label: '研究背景', nextLabel: '主要内容' },
    { key: 'mainContent', label: '主要内容', nextLabel: '能力要求' },
    { key: 'abilityReq', label: '能力要求', nextLabel: '数据/资源需求' },
    { key: 'dataReq', label: '数据/资源需求', nextLabel: '创新点' },
    { key: 'innovation', label: '创新点', nextLabel: null }
  ] as const
  let rest = s
  for (const e of entries) {
    const mark = '- ' + e.label + '：'
    const altMark = '- ' + e.label + ':'
    const start = rest.indexOf(mark) !== -1 ? rest.indexOf(mark) : rest.indexOf(altMark)
    if (start === -1) continue
    const contentStart = rest.indexOf('：', start) !== -1 ? rest.indexOf('：', start) + 1 : rest.indexOf(':', start) + 1
    const nextStart = e.nextLabel
      ? (rest.indexOf('- ' + e.nextLabel + '：', contentStart) !== -1
          ? rest.indexOf('- ' + e.nextLabel + '：', contentStart)
          : rest.indexOf('- ' + e.nextLabel + ':', contentStart))
      : -1
    const contentEnd = nextStart === -1 ? rest.length : nextStart
    fields[e.key] = rest.slice(contentStart, contentEnd).trim()
    if (nextStart !== -1) rest = rest.slice(nextStart)
  }
  return Object.keys(fields).length > 0 ? fields : null
}

/** 将五维字段组装为与 AI 输出一致的 Markdown 描述 */
function buildStructuredDescription(f: typeof structuredFields.value): string {
  const lines: string[] = []
  if (f.researchBackground) lines.push('- 研究背景：' + f.researchBackground.trim())
  if (f.mainContent) lines.push('- 主要内容：' + f.mainContent.trim())
  if (f.abilityReq) lines.push('- 能力要求：' + f.abilityReq.trim())
  if (f.dataReq) lines.push('- 数据/资源需求：' + f.dataReq.trim())
  if (f.innovation) lines.push('- 创新点：' + f.innovation.trim())
  return lines.join('\n\n')
}

const aiDialogVisible = ref(false)
const aiLoading = ref(false)
const aiForm = ref({
  count: 5,
  preferenceHint: '',
  backgroundHint: '',
  contentHint: '',
  abilityHint: '',
  dataHint: '',
  innovationHint: '',
  tagNames: [] as string[]
})
const aiTopics = ref<AiGeneratedTopic[]>([])
const teacherTags = ref<string[]>([])

const dialogTitle = computed(() => {
  return editingId.value ? '编辑选题' : '创建选题'
})

onMounted(() => {
  loadTopics()
})

const loadTopics = async () => {
  loading.value = true
  try {
    const res = await getTopics()
    topics.value = res.data || []
  } catch (error) {
    ElMessage.error('加载选题失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  editingId.value = null
  form.value = {
    title: '',
    description: '',
    maxApplicants: 1,
    tags: ''
  }
  useStructuredEdit.value = false
  editMode.value = 'raw'
  structuredFields.value = {
    researchBackground: '',
    mainContent: '',
    abilityReq: '',
    dataReq: '',
    innovation: ''
  }
  dialogVisible.value = true
}

const openAiDialog = () => {
  aiDialogVisible.value = true
  aiTopics.value = []
  // 首次打开时拉取导师标签
  if (teacherTags.value.length === 0) {
    loadTeacherTags()
  }
}

const loadTeacherTags = async () => {
  try {
    const res = await getTeacherProfile()
    teacherTags.value = res.data?.tags?.map((t) => t.tagName).filter((n) => !!n) || []
  } catch (e) {
    // 忽略错误，仅意味着无法提供勾选标签
  }
}

const editTopic = (topic: Topic) => {
  editingId.value = topic.id
  const desc = topic.description || ''
  form.value = {
    title: topic.title,
    description: desc,
    maxApplicants: topic.maxApplicants,
    tags: topic.tags?.join(',') || ''
  }
  const parsed = parseStructuredDescription(desc)
  if (parsed) {
    useStructuredEdit.value = true
    editMode.value = 'structured'
    structuredFields.value = {
      researchBackground: parsed.researchBackground ?? '',
      mainContent: parsed.mainContent ?? '',
      abilityReq: parsed.abilityReq ?? '',
      dataReq: parsed.dataReq ?? '',
      innovation: parsed.innovation ?? ''
    }
  } else {
    useStructuredEdit.value = false
    editMode.value = 'raw'
    structuredFields.value = {
      researchBackground: '',
      mainContent: '',
      abilityReq: '',
      dataReq: '',
      innovation: ''
    }
  }
  dialogVisible.value = true
}

const saveTopic = async () => {
  if (!form.value.title) {
    ElMessage.warning('请输入选题标题')
    return
  }
  let descriptionToSave = form.value.description
  if (useStructuredEdit.value && editMode.value === 'structured') {
    descriptionToSave = buildStructuredDescription(structuredFields.value)
  }

  saving.value = true
  try {
    const tags = form.value.tags ? form.value.tags.split(',').map(t => t.trim()).filter(t => t) : []
    const data = {
      title: form.value.title,
      description: descriptionToSave,
      maxApplicants: form.value.maxApplicants,
      tags: tags
    }

    if (editingId.value) {
      await updateTopic(editingId.value, data)
    } else {
      await createTopic(data)
    }

    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const generateAiTopicsClick = async () => {
  aiLoading.value = true
  aiTopics.value = []
  try {
    const res = await generateAiTopics({
      count: aiForm.value.count,
      tagNames: aiForm.value.tagNames.length ? aiForm.value.tagNames : undefined,
      preferenceHint: aiForm.value.preferenceHint?.trim() || undefined,
      backgroundHint: aiForm.value.backgroundHint?.trim() || undefined,
      contentHint: aiForm.value.contentHint?.trim() || undefined,
      abilityHint: aiForm.value.abilityHint?.trim() || undefined,
      dataHint: aiForm.value.dataHint?.trim() || undefined,
      innovationHint: aiForm.value.innovationHint?.trim() || undefined
    })
    aiTopics.value = res.data || []
    if (aiTopics.value.length === 0) {
      ElMessage.info('暂未生成合适的候选题目，请补充选题需求说明后重试')
    } else {
      ElMessage.success(`已生成 ${aiTopics.value.length} 个候选题目`)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '生成候选选题失败')
  } finally {
    aiLoading.value = false
  }
}

const toggleAiTag = (tag: string) => {
  const exists = aiForm.value.tagNames.includes(tag)
  if (exists) {
    aiForm.value.tagNames = aiForm.value.tagNames.filter((t) => t !== tag)
  } else {
    aiForm.value.tagNames = [...aiForm.value.tagNames, tag]
  }
}

const importAiTopic = async (t: AiGeneratedTopic) => {
  if (!t.title) {
    ElMessage.warning('候选题目标题为空，无法导入')
    return
  }
  try {
    saving.value = true
    await createTopic({
      title: t.title,
      description: t.description || '',
      maxApplicants: 1,
      tags: t.tags || []
    })
    ElMessage.success('已导入为我的选题草稿，可在列表中继续编辑')
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    saving.value = false
  }
}

const submitReview = async (topic: Topic) => {
  try {
    await ElMessageBox.confirm('确定要提交审核吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await submitTopicForReview(topic.id)
    ElMessage.success('提交成功')
    loadTopics()
  } catch (error: any) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  }
}

const checkDuplicate = async (topic: Topic) => {
  try {
    const res = await checkTopicDuplicate({
      topicId: topic.id,
      title: topic.title,
      description: topic.description
    })
    if (res.data.passed) {
      ElMessage.success(`去重检测通过，相似度：${(res.data.maxSimilarity * 100).toFixed(2)}%`)
    } else {
      ElMessage.warning(`检测到相似选题：${res.data.similarTopicTitle}，相似度：${(res.data.maxSimilarity * 100).toFixed(2)}%`)
    }
  } catch (error: any) {
    ElMessage.warning(error.message || '检测到相似选题')
  }
}

const removeTopic = async (topic: Topic) => {
  try {
    await ElMessageBox.confirm(
      '仅草稿或已驳回的选题可以删除，删除后将无法恢复。是否确认删除？',
      '删除选题',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  try {
    await deleteTopic(topic.id)
    ElMessage.success('选题已删除')
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}
</script>

<style scoped>
.topics-container {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

/* AI 弹窗内容区：由内部控制高度与左右滚动 */
.ai-dialog-content {
  max-height: 82vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.ai-dialog-lr {
  display: flex;
  gap: 24px;
  min-height: 0;
  flex: 1;
}

.ai-dialog-left {
  flex: 0 0 420px;
  border-right: 1px solid var(--el-border-color-lighter);
  padding-right: 20px;
}

.ai-dialog-right {
  flex: 1;
  min-width: 0;
}

.ai-empty-tip {
  text-align: center;
  color: #909399;
  padding: 24px 16px;
  font-size: 13px;
}

.ai-right-alert {
  margin-bottom: 10px;
}

.edit-mode-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.ai-topic-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 4px;
}

.ai-tag-chooser {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-tag-chip {
  cursor: pointer;
}

.ai-tag-hint {
  width: 100%;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.ai-topic-card {
  border-radius: 10px;
}

.ai-topic-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}

.ai-topic-title {
  font-weight: 600;
  font-size: 15px;
}

.ai-topic-meta {
  font-size: 12px;
  color: #909399;
}

.ai-topic-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
  white-space: pre-wrap;
}
.ai-demand-hint {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
  line-height: 1.5;
}

.ai-topic-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.ai-topic-tags-label {
  font-size: 12px;
  color: #909399;
}

.ai-topic-tag {
  margin-left: 2px;
}

.ai-topic-actions {
  display: flex;
  justify-content: flex-end;
}
</style>

