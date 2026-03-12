<template>
  <div class="topics-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题中心</span>
          <el-button type="primary" @click="refreshTopics">刷新</el-button>
        </div>
      </template>

      <el-table :data="topics" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="选题标题" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="导师" width="120" />
        <el-table-column prop="currentApplicants" label="已申请人数" width="120" />
        <el-table-column prop="maxApplicants" label="最大人数" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewTopic(row)">查看</el-button>
            <el-button type="success" size="small" @click="applyTopic(row)" :disabled="row.currentApplicants >= row.maxApplicants">
              申请
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 选题详情对话框 -->
    <el-dialog v-model="dialogVisible" title="选题详情" width="720px" class="topic-dialog">
      <div v-if="selectedTopic" class="topic-dialog-body">
        <div class="topic-dialog-main">
          <div class="topic-title-row">
            <h3 class="topic-title">{{ selectedTopic.title }}</h3>
            <el-tag size="small" type="success" v-if="selectedTopic.status === 'OPEN'">开放中</el-tag>
            <el-tag size="small" type="info" v-else>状态：{{ selectedTopic.status }}</el-tag>
          </div>
          <p class="topic-description">
            {{ selectedTopic.description || '暂无详细描述。' }}
          </p>
          <div v-if="selectedTopic.tags?.length" class="topic-tags">
            <span class="topic-tags-label">关键词：</span>
            <el-tag
              v-for="tag in selectedTopic.tags"
              :key="tag"
              size="small"
              effect="plain"
              class="topic-tag-item"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
        <div class="topic-dialog-side">
          <div class="topic-meta-block">
            <div class="topic-meta-row">
              <span class="meta-label">导师</span>
              <span class="meta-value">{{ selectedTopic.teacherName || '未填写' }}</span>
            </div>
            <div class="topic-meta-row">
              <span class="meta-label">容量 / 已申请</span>
              <span class="meta-value">
                {{ selectedTopic.currentApplicants }} / {{ selectedTopic.maxApplicants }}
              </span>
            </div>
            <el-progress
              :percentage="Math.min(100, Math.round((selectedTopic.currentApplicants / selectedTopic.maxApplicants) * 100))"
              :stroke-width="10"
              :status="selectedTopic.currentApplicants >= selectedTopic.maxApplicants ? 'exception' : 'success'"
              class="topic-progress"
            />
            <div class="topic-meta-row">
              <span class="meta-label">剩余名额</span>
              <el-tag
                :type="selectedTopic.currentApplicants >= selectedTopic.maxApplicants ? 'danger' : 'success'"
                size="small"
              >
                {{
                  Math.max(0, selectedTopic.maxApplicants - selectedTopic.currentApplicants)
                }} 人
              </el-tag>
            </div>
            <div class="topic-meta-row" v-if="selectedTopic.createdAt">
              <span class="meta-label">发布于</span>
              <span class="meta-value">{{ selectedTopic.createdAt }}</span>
            </div>
          </div>
          <div class="topic-hint">
            提示：请优先选择与你兴趣、基础和未来规划匹配的课题，申请通过后仍需与导师沟通确认具体方向。
          </div>
        </div>
      </div>
      <template #footer>
        <div class="topic-dialog-footer">
          <span class="topic-footer-tip">
            提交前可先与导师在“协作中心”中沟通确认。
          </span>
          <div class="topic-footer-actions">
            <el-button @click="dialogVisible = false">关闭</el-button>
            <el-button type="primary" @click="handleApply">去填写申请</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 申请对话框 -->
    <el-dialog v-model="applyDialogVisible" title="提交申请" width="640px" class="topic-dialog">
      <div class="apply-dialog-body">
        <div class="apply-dialog-summary" v-if="selectedTopic">
          <div class="summary-title">{{ selectedTopic.title }}</div>
          <div class="summary-sub">
            导师：{{ selectedTopic.teacherName || '未填写' }}
          </div>
          <div class="summary-capacity">
            当前：{{ selectedTopic.currentApplicants }} / {{ selectedTopic.maxApplicants }} 人
          </div>
        </div>
        <div class="apply-dialog-form">
          <el-form :model="applyForm" label-width="90px">
            <el-form-item label="申请备注">
              <el-input
                v-model="applyForm.remark"
                type="textarea"
                :rows="5"
                maxlength="300"
                show-word-limit
                placeholder="建议简要介绍自己的背景、兴趣方向、已有基础，以及选择该课题/导师的原因。"
              />
            </el-form-item>
          </el-form>
          <div class="apply-remark-hint">
            已输入 {{ remarkLength }} / 300 字
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply" :loading="submitting" :disabled="remarkLength === 0">
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOpenTopics, type Topic } from '@/api/topic'
import { submitApplication } from '@/api/application'

const loading = ref(false)
const topics = ref<Topic[]>([])
const dialogVisible = ref(false)
const selectedTopic = ref<Topic | null>(null)
const applyDialogVisible = ref(false)
const submitting = ref(false)

const applyForm = ref({
  remark: ''
})

const remarkLength = computed(() => applyForm.value.remark.trim().length)

onMounted(() => {
  loadTopics()
})

const loadTopics = async () => {
  loading.value = true
  try {
    const res = await getOpenTopics()
    topics.value = res.data || []
  } catch (error) {
    ElMessage.error('加载选题失败')
  } finally {
    loading.value = false
  }
}

const refreshTopics = () => {
  loadTopics()
}

const viewTopic = (topic: Topic) => {
  selectedTopic.value = topic
  dialogVisible.value = true
}

const applyTopic = (topic: Topic) => {
  selectedTopic.value = topic
  applyForm.value.remark = ''
  applyDialogVisible.value = true
}

const handleApply = () => {
  dialogVisible.value = false
  applyTopic(selectedTopic.value!)
}

const submitApply = async () => {
  if (!selectedTopic.value) return
  
  submitting.value = true
  try {
    await submitApplication({
      topicId: selectedTopic.value.id,
      remark: applyForm.value.remark
    })
    ElMessage.success('申请提交成功')
    applyDialogVisible.value = false
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '申请提交失败')
  } finally {
    submitting.value = false
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

.topic-dialog :deep(.el-dialog) {
  border-radius: 14px;
}

.topic-dialog-body {
  display: flex;
  gap: 24px;
}

.topic-dialog-main {
  flex: 2;
}

.topic-dialog-side {
  flex: 1.3;
  padding-left: 16px;
  border-left: 1px solid #f0f0f0;
}

.topic-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.topic-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.topic-description {
  margin: 4px 0 10px;
  line-height: 1.6;
  color: #606266;
}

.topic-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.topic-tags-label {
  font-size: 13px;
  color: #909399;
}

.topic-tag-item {
  margin-left: 2px;
}

.topic-meta-block {
  background: #f5f7fa;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
}

.topic-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.meta-label {
  color: #909399;
}

.meta-value {
  color: #303133;
  font-weight: 500;
  margin-left: 8px;
}

.topic-progress {
  margin: 6px 0 4px;
}

.topic-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.topic-dialog-footer {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topic-footer-tip {
  font-size: 12px;
  color: #909399;
}

.topic-footer-actions {
  display: flex;
  gap: 8px;
}

.apply-dialog-body {
  display: flex;
  gap: 16px;
}

.apply-dialog-summary {
  flex: 1.1;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f5f7fa;
}

.summary-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.summary-sub,
.summary-capacity {
  font-size: 13px;
  color: #606266;
}

.apply-dialog-form {
  flex: 1.6;
}

.apply-remark-hint {
  text-align: right;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>

