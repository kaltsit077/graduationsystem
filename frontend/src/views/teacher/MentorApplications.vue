<template>
  <div class="mentor-applications-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>拜师申请处理</span>
          <el-button type="primary" link @click="loadData" :loading="loading">刷新</el-button>
        </div>
      </template>

      <el-table :data="applications" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="申请ID" width="80" />
        <el-table-column prop="studentName" label="学生姓名" width="140" />
        <el-table-column prop="reason" label="学生说明" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="申请时间" width="180" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待处理</el-tag>
            <el-tag v-else-if="row.status === 'APPROVED'" type="success">已同意</el-tag>
            <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已拒绝</el-tag>
            <el-tag v-else type="info">已取消</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              size="small"
              @click="openDecisionDialog(row, 'APPROVED')"
            >
              同意并后续分配题目
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="danger"
              size="small"
              @click="openDecisionDialog(row, 'REJECTED')"
            >
              拒绝
            </el-button>
            <el-button
              v-if="row.status === 'APPROVED'"
              type="primary"
              size="small"
              @click="openAssignDialog(row)"
            >
              为其指派课题
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !applications.length" class="empty-tip">
        当前没有待处理的拜师申请。
      </div>
    </el-card>

    <!-- 审批对话框 -->
    <el-dialog v-model="decisionDialog.visible" title="审批拜师申请" width="520px">
      <div v-if="decisionDialog.application">
        <p>
          学生：<strong>{{ decisionDialog.application.studentName || decisionDialog.application.studentId }}</strong>
        </p>
        <p>学生说明：{{ decisionDialog.application.reason || '（无）' }}</p>
      </div>
      <el-form :model="decisionForm" label-width="100px">
        <el-form-item label="决策">
          <el-radio-group v-model="decisionForm.status">
            <el-radio label="APPROVED">同意（后续为其分配题目）</el-radio>
            <el-radio label="REJECTED">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="decisionForm.comment"
            type="textarea"
            :rows="4"
            placeholder="可选，简要说明您的考虑"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decisionDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="decisionDialog.submitting" @click="submitDecision">
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- 指派课题对话框（从现有题库中选择） -->
    <el-dialog v-model="assignDialog.visible" title="为学生指派课题" width="600px">
      <div v-if="assignDialog.application">
        <p>
          学生：<strong>{{ assignDialog.application.studentName || assignDialog.application.studentId }}</strong>
        </p>
      </div>
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="选择课题">
          <el-select
            v-model="assignForm.topicId"
            filterable
            placeholder="请选择要指派的课题"
            style="width: 100%"
          >
            <el-option
              v-for="topic in topics"
              :key="topic.id"
              :label="topic.title"
              :value="topic.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <span class="hint-text">
            仅支持从当前已开放或可用的题库中选择；如需为该学生新建题目，可先在“选题管理”中创建后再回到此处指派。
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="assignDialog.submitting" @click="submitAssign">
          确认指派
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getTeacherPendingMentorApplications,
  submitMentorApplicationDecision,
  assignTopicForMentorApplication,
  type MentorApplication
} from '@/api/mentorApplication'
import { getTopics, type Topic } from '@/api/topic'
import request from '@/api/request'

interface MentorApplicationView extends MentorApplication {
  studentName?: string
}

const loading = ref(false)
const applications = ref<MentorApplicationView[]>([])

const topics = ref<Topic[]>([])

const decisionDialog = reactive<{
  visible: boolean
  application: MentorApplicationView | null
  submitting: boolean
}>({
  visible: false,
  application: null,
  submitting: false
})

const decisionForm = reactive<{
  status: 'APPROVED' | 'REJECTED'
  comment: string
}>({
  status: 'APPROVED',
  comment: ''
})

const assignDialog = reactive<{
  visible: boolean
  application: MentorApplicationView | null
  submitting: boolean
}>({
  visible: false,
  application: null,
  submitting: false
})

const assignForm = reactive<{
  topicId: number | null
}>({
  topicId: null
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTeacherPendingMentorApplications()
    const list: MentorApplication[] = res.data || []

    const enriched: MentorApplicationView[] = []
    for (const item of list) {
      let studentName: string | undefined
      try {
        const userRes = await request.get(`/admin/users/students`)
        const allStudents = userRes.data || []
        const found = allStudents.find((s: any) => s.id === item.studentId)
        if (found) {
          studentName = found.realName || found.username
        }
      } catch {
        // ignore
      }
      enriched.push({
        ...item,
        studentName
      })
    }
    applications.value = enriched
  } catch (err: any) {
    ElMessage.error(err?.message || '加载拜师申请失败')
  } finally {
    loading.value = false
  }
}

const loadTopics = async () => {
  try {
    const res = await getTopics()
    topics.value = res.data || []
  } catch {
    topics.value = []
  }
}

const openDecisionDialog = (app: MentorApplicationView, status: 'APPROVED' | 'REJECTED') => {
  decisionDialog.application = app
  decisionForm.status = status
  decisionForm.comment = ''
  decisionDialog.visible = true
}

const submitDecision = async () => {
  if (!decisionDialog.application) return
  decisionDialog.submitting = true
  try {
    await submitMentorApplicationDecision(
      decisionDialog.application.id,
      decisionForm.status,
      decisionForm.comment || undefined
    )
    ElMessage.success('已提交审批结果')
    decisionDialog.visible = false
    loadData()
  } catch (err: any) {
    ElMessage.error(err?.message || '提交失败')
  } finally {
    decisionDialog.submitting = false
  }
}

const openAssignDialog = (app: MentorApplicationView) => {
  assignDialog.application = app
  assignForm.topicId = null
  assignDialog.visible = true
  loadTopics()
}

const submitAssign = async () => {
  if (!assignDialog.application || !assignForm.topicId) {
    ElMessage.warning('请选择要指派的课题')
    return
  }
  assignDialog.submitting = true
  try {
    await assignTopicForMentorApplication(assignDialog.application.id, assignForm.topicId)
    ElMessage.success('已为该学生指派课题，对应绑定关系已建立')
    assignDialog.visible = false
    loadData()
  } catch (err: any) {
    ElMessage.error(err?.message || '指派失败')
  } finally {
    assignDialog.submitting = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.mentor-applications-page {
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

.hint-text {
  font-size: 13px;
  color: #909399;
}
</style>

