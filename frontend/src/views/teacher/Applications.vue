<template>
  <div class="applications-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>申请处理</span>
        </div>
      </template>

      <el-table :data="applications" style="width: 100%" v-loading="loading">
        <el-table-column prop="topicTitle" label="选题标题" min-width="200" />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="matchScore" label="匹配度" width="120">
          <template #default="{ row }">
            <el-tag :type="row.matchScore > 0.7 ? 'success' : 'info'">
              {{ (row.matchScore * 100).toFixed(0) }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="申请备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.status === 'APPROVED'" type="success">已通过</el-tag>
            <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              size="small"
              @click="processApplicationClick(row, 'APPROVED')"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="danger"
              size="small"
              @click="processApplicationClick(row, 'REJECTED')"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 处理申请对话框 -->
    <el-dialog v-model="processDialogVisible" title="处理申请" width="500px">
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="processForm.result">
            <el-radio label="APPROVED">通过</el-radio>
            <el-radio label="REJECTED">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="反馈意见">
          <el-input
            v-model="processForm.feedback"
            type="textarea"
            :rows="4"
            placeholder="请输入反馈意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcess" :loading="processing">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTopics, type Topic } from '@/api/topic'
import { getTopicApplications, processApplication, type Application } from '@/api/application'

const loading = ref(false)
const applications = ref<Application[]>([])
const processDialogVisible = ref(false)
const processing = ref(false)
const currentApplication = ref<Application | null>(null)
const currentTopicId = ref<number | null>(null)

const processForm = ref({
  result: 'APPROVED' as 'APPROVED' | 'REJECTED',
  feedback: ''
})

onMounted(() => {
  loadTopicsAndApplications()
})

const loadTopicsAndApplications = async () => {
  loading.value = true
  try {
    // 先加载导师的所有选题
    const topicsRes = await getTopics()
    const topics = topicsRes.data || []
    
    // 加载每个选题的申请列表
    const allApplications: Application[] = []
    for (const topic of topics) {
      const appsRes = await getTopicApplications(topic.id, true)
      allApplications.push(...(appsRes.data || []))
    }
    
    applications.value = allApplications.sort((a, b) => {
      const scoreA = a.matchScore || 0
      const scoreB = b.matchScore || 0
      return scoreB - scoreA
    })
  } catch (error) {
    ElMessage.error('加载申请失败')
  } finally {
    loading.value = false
  }
}

const processApplicationClick = (application: Application, result: string) => {
  currentApplication.value = application
  currentTopicId.value = application.topicId
  processForm.value = {
    result: result as 'APPROVED' | 'REJECTED',
    feedback: ''
  }
  processDialogVisible.value = true
}

const submitProcess = async () => {
  if (!currentApplication.value) return
  
  processing.value = true
  try {
    await processApplication(currentApplication.value.id, {
      status: processForm.value.result,
      feedback: processForm.value.feedback
    })
    ElMessage.success('处理成功')
    processDialogVisible.value = false
    loadTopicsAndApplications()
  } catch (error: any) {
    ElMessage.error(error.message || '处理失败')
  } finally {
    processing.value = false
  }
}
</script>

<style scoped>
.applications-container {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

