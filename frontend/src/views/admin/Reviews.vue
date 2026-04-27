<template>
  <div class="reviews-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题审核</span>
        </div>
      </template>

      <el-table :data="topics" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="选题标题" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="导师" width="120" />
        <el-table-column prop="maxApplicants" label="最大人数" width="100" />
        <el-table-column prop="createdAt" label="提交时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openTopicDetail(row)">详情</el-button>
            <el-button type="success" size="small" @click="reviewTopic(row, 'PASS')">通过</el-button>
            <el-button type="danger" size="small" @click="reviewTopic(row, 'REJECT')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 审核对话框 -->
    <AppDialog
      v-model="reviewDialogVisible"
      title="选题审核"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.result">
            <el-radio label="PASS">通过</el-radio>
            <el-radio label="REJECT">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview" :loading="reviewing">确定</el-button>
      </template>
    </AppDialog>

    <AppDialog
      v-model="detailDialogVisible"
      title="选题详情"
      width="720px"
      :close-on-click-modal="false"
    >
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailTopic" :column="2" border>
          <el-descriptions-item label="选题标题" :span="2">{{ detailTopic.title }}</el-descriptions-item>
          <el-descriptions-item label="导师">{{ detailTopic.teacherName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最大人数">{{ detailTopic.maxApplicants ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detailTopic.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detailTopic.status || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <template v-if="detailTopic.tags && detailTopic.tags.length">
              <el-tag v-for="tag in detailTopic.tags" :key="tag" size="small" class="detail-tag">{{ tag }}</el-tag>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="详细描述" :span="2">
            <div class="topic-description">{{ detailTopic.description || '暂无描述' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AppDialog from '@/components/AppDialog.vue'
import request from '@/api/request'
import { getTopic, type Topic } from '@/api/topic'

const loading = ref(false)
const topics = ref<Topic[]>([])
const reviewDialogVisible = ref(false)
const reviewing = ref(false)
const currentTopic = ref<Topic | null>(null)
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailTopic = ref<Topic | null>(null)

const reviewForm = ref({
  result: 'PASS',
  comment: ''
})

onMounted(() => {
  loadTopics()
})

const loadTopics = async () => {
  loading.value = true
  try {
    const res = await request.get<Topic[]>('/admin/topics/pending-review')
    topics.value = res.data || []
  } catch (error) {
    ElMessage.error('加载选题失败')
  } finally {
    loading.value = false
  }
}

const reviewTopic = (topic: Topic, result: string) => {
  currentTopic.value = topic
  reviewForm.value = {
    result: result,
    comment: ''
  }
  reviewDialogVisible.value = true
}

const openTopicDetail = async (topic: Topic) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  detailTopic.value = null
  try {
    const res = await getTopic(topic.id)
    detailTopic.value = res.data || topic
  } catch {
    detailTopic.value = topic
    ElMessage.warning('加载完整详情失败，已展示列表中的基础信息')
  } finally {
    detailLoading.value = false
  }
}

const submitReview = async () => {
  if (!currentTopic.value) return
  
  reviewing.value = true
  try {
    await request.post(`/admin/topics/${currentTopic.value.id}/review`, {
      result: reviewForm.value.result,
      comment: reviewForm.value.comment
    })
    ElMessage.success('审核成功')
    reviewDialogVisible.value = false
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    reviewing.value = false
  }
}
</script>

<style scoped>
.reviews-container {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topic-description {
  white-space: pre-wrap;
  line-height: 1.6;
}

.detail-tag {
  margin-right: 6px;
  margin-bottom: 6px;
}
</style>

