<template>
  <div class="change-requests-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题变更申请（更换选题）</span>
          <el-button size="small" @click="loadData" :loading="loading">刷新</el-button>
        </div>
      </template>

      <el-table :data="requests" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="申请ID" width="90" />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="topicTitle" label="当前选题" min-width="220" />
        <el-table-column prop="createdAt" label="提交时间" width="180" />
        <el-table-column label="原因说明" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.reason }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              type="success"
              size="small"
              @click="handleDecision(row, 'APPROVED')"
            >
              同意解除绑定
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDecision(row, 'REJECTED')"
            >
              拒绝申请
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!requests.length && !loading" class="empty-tip">
        当前没有待处理的选题变更申请。
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherPendingChangeRequests, submitTeacherChangeDecision, type ChangeRequest, type Decision } from '@/api/changeRequest'
import request from '@/api/request'

interface ChangeRequestView extends ChangeRequest {
  studentName?: string
  topicTitle?: string
}

const loading = ref(false)
const requests = ref<ChangeRequestView[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTeacherPendingChangeRequests()
    const list = res.data || []

    // 补充学生姓名与当前选题标题（通过额外请求获取，当前为简化实现）
    const enriched: ChangeRequestView[] = []
    for (const item of list) {
      let studentName: string | undefined
      let topicTitle: string | undefined
      try {
        const appRes = await request.get('/applications/' + item.currentApplicationId)
        const app = appRes.data
        studentName = app?.studentName
        topicTitle = app?.topicTitle
      } catch {
        // 忽略补充信息失败
      }
      enriched.push({
        ...item,
        studentName,
        topicTitle
      })
    }
    requests.value = enriched
  } catch {
    ElMessage.error('加载变更申请失败')
  } finally {
    loading.value = false
  }
}

const handleDecision = async (row: ChangeRequestView, decision: Decision) => {
  const actionText = decision === 'APPROVED' ? '同意解除该选题绑定' : '拒绝本次变更'
  try {
    const { value: comment } = await ElMessageBox.prompt(
      `确定要${actionText}吗？可填写简要意见（可选）。`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '可填写审批意见（可留空）',
        inputType: 'textarea',
        inputValue: ''
      }
    )
    await submitTeacherChangeDecision(row.id, decision, comment || undefined)
    ElMessage.success('已提交审批结果')
    loadData()
  } catch (err: any) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error(err?.message || '操作失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.change-requests-page {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-tip {
  text-align: center;
  padding: 18px 0;
  color: #909399;
}
</style>

