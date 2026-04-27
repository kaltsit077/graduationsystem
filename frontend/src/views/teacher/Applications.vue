<template>
  <div class="applications-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>申请处理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="apps-tabs">
        <el-tab-pane label="选题申请" name="topic">
          <el-table :data="topicApplications" style="width: 100%" v-loading="loading">
            <el-table-column prop="topicTitle" label="选题标题" min-width="200" />
            <el-table-column prop="studentName" label="学生姓名" width="120" />
            <el-table-column prop="matchScore" label="匹配度" width="120">
              <template #default="{ row }">
                <el-tag :type="(row.matchScore || 0) > 0.7 ? 'success' : 'info'">
                  {{ (((row.matchScore || 0) as number) * 100).toFixed(0) }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="申请备注" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'PENDING'" type="warning">待审核</el-tag>
                <el-tag v-else-if="row.status === 'APPROVED'" type="success">已通过</el-tag>
                <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已拒绝</el-tag>
                <el-tag v-else-if="row.status === 'COMPLETION_PENDING'" type="warning">结题待审核</el-tag>
                <el-tag v-else-if="row.status === 'COMPLETION_REJECTED'" type="danger">结题未通过</el-tag>
                <el-tag v-else-if="row.status === 'COMPLETED'" type="info">已结题</el-tag>
                <el-tag v-else type="info">{{ row.status || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'PENDING'"
                  type="success"
                  size="small"
                  @click="openProcessDialog('topic', row, 'APPROVED')"
                >
                  通过
                </el-button>
                <el-button
                  v-if="row.status === 'PENDING'"
                  type="danger"
                  size="small"
                  @click="openProcessDialog('topic', row, 'REJECTED')"
                >
                  拒绝
                </el-button>
                <el-button
                  v-if="row.status === 'COMPLETION_PENDING'"
                  type="success"
                  size="small"
                  @click="openProcessDialog('topic', row, 'APPROVED')"
                >
                  通过结题
                </el-button>
                <el-button
                  v-if="row.status === 'COMPLETION_PENDING'"
                  type="danger"
                  size="small"
                  @click="openProcessDialog('topic', row, 'REJECTED')"
                >
                  退回结题
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="导师申请" name="mentor">
          <el-table :data="mentorApplications" style="width: 100%" v-loading="loading">
            <el-table-column prop="studentName" label="学生姓名" width="120" />
            <el-table-column prop="reason" label="申请说明" min-width="240" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'PENDING'" type="warning">待处理</el-tag>
                <el-tag v-else-if="row.status === 'APPROVED'" type="success">已同意</el-tag>
                <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已拒绝</el-tag>
                <el-tag v-else type="info">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'PENDING'"
                  type="success"
                  size="small"
                  @click="openProcessDialog('mentor', row, 'APPROVED')"
                >
                  同意
                </el-button>
                <el-button
                  v-if="row.status === 'PENDING'"
                  type="danger"
                  size="small"
                  @click="openProcessDialog('mentor', row, 'REJECTED')"
                >
                  拒绝
                </el-button>
                <el-button
                  v-if="row.status === 'APPROVED'"
                  type="primary"
                  size="small"
                  @click="openAssignTopicDialog(row)"
                >
                  指派题目
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
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

    <el-dialog v-model="assignDialogVisible" title="为学生指派题目" width="520px">
      <el-form label-width="100px">
        <el-form-item label="学生">
          <span>{{ currentMentorApplication?.studentName || currentMentorApplication?.studentId || '-' }}</span>
        </el-form-item>
        <el-form-item label="题目" required>
          <el-select v-model="assignTopicId" placeholder="请选择题目" style="width: 100%" :loading="loadingAssignableTopics">
            <el-option
              v-for="t in assignableTopics"
              :key="t.id"
              :label="`${t.title}（匹配度 ${formatMatchScore(t.matchScore)}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignTopic" :loading="assigningTopic">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTopicApplications, processApplication, reviewCompletionRequest, type Application } from '@/api/application'
import {
  getTeacherMentorApplications,
  getTeacherPendingMentorApplications,
  submitMentorApplicationDecision,
  assignTopicForMentorApplication,
  getAssignableTopicsForMentorApplication,
  type MentorApplication
} from '@/api/mentorApplication'
import { getTopics, type Topic } from '@/api/topic'

const loading = ref(false)
const activeTab = ref<'topic' | 'mentor'>('topic')
const topicApplications = ref<Application[]>([])
const mentorApplications = ref<MentorApplication[]>([])
const assignableTopics = ref<Topic[]>([])
const processDialogVisible = ref(false)
const processing = ref(false)
const processType = ref<'topic' | 'mentor'>('topic')
const currentTopicApplication = ref<Application | null>(null)
const currentMentorApplication = ref<MentorApplication | null>(null)
const assignDialogVisible = ref(false)
const assignTopicId = ref<number | null>(null)
const assigningTopic = ref(false)
const loadingAssignableTopics = ref(false)

const processForm = ref({
  result: 'APPROVED' as 'APPROVED' | 'REJECTED',
  feedback: ''
})

const formatMatchScore = (score?: number) => {
  if (score == null) return '--'
  return `${(score * 100).toFixed(0)}%`
}

onMounted(() => {
  loadAllApplications()
})

const loadAllApplications = async () => {
  loading.value = true
  try {
    const topicsRes = await getTopics()
    const topics = topicsRes.data || []

    const allApplications: Application[] = []
    for (const topic of topics) {
      const appsRes = await getTopicApplications(topic.id, true)
      allApplications.push(...(appsRes.data || []))
    }
    topicApplications.value = allApplications.sort((a, b) => {
      const scoreA = a.matchScore || 0
      const scoreB = b.matchScore || 0
      return scoreB - scoreA
    })
    try {
      const mentorRes = await getTeacherMentorApplications()
      mentorApplications.value = mentorRes.data || []
    } catch {
      // 兼容旧后端：若尚未发布 /teacher 全量接口，则回退到 pending 列表
      const pendingRes = await getTeacherPendingMentorApplications()
      mentorApplications.value = pendingRes.data || []
    }
  } catch (error) {
    ElMessage.error('加载申请失败')
  } finally {
    loading.value = false
  }
}

const openProcessDialog = (
  type: 'topic' | 'mentor',
  application: Application | MentorApplication,
  result: 'APPROVED' | 'REJECTED'
) => {
  processType.value = type
  if (type === 'topic') {
    currentTopicApplication.value = application as Application
    currentMentorApplication.value = null
  } else {
    currentMentorApplication.value = application as MentorApplication
    currentTopicApplication.value = null
  }
  processForm.value = {
    result,
    feedback: ''
  }
  processDialogVisible.value = true
}

const submitProcess = async () => {
  if (processType.value === 'topic' && !currentTopicApplication.value) return
  if (processType.value === 'mentor' && !currentMentorApplication.value) return

  processing.value = true
  try {
    if (processType.value === 'topic') {
      const app = currentTopicApplication.value!
      if (app.status === 'COMPLETION_PENDING') {
        await reviewCompletionRequest(app.id, {
          status: processForm.value.result,
          feedback: processForm.value.feedback
        })
      } else {
        await processApplication(app.id, {
          status: processForm.value.result,
          feedback: processForm.value.feedback
        })
      }
    } else {
      await submitMentorApplicationDecision(
        currentMentorApplication.value!.id,
        processForm.value.result,
        processForm.value.feedback
      )
    }
    ElMessage.success('处理成功')
    processDialogVisible.value = false
    await loadAllApplications()
  } catch (error: any) {
    ElMessage.error(error.message || '处理失败')
  } finally {
    processing.value = false
  }
}

const openAssignTopicDialog = (application: MentorApplication) => {
  currentMentorApplication.value = application
  assignTopicId.value = null
  assignableTopics.value = []
  assignDialogVisible.value = true
  loadAssignableTopics(application.id)
}

const loadAssignableTopics = async (applicationId: number) => {
  loadingAssignableTopics.value = true
  try {
    const res = await getAssignableTopicsForMentorApplication(applicationId)
    assignableTopics.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载可指派题目失败')
  } finally {
    loadingAssignableTopics.value = false
  }
}

const submitAssignTopic = async () => {
  if (!currentMentorApplication.value) return
  if (!assignTopicId.value) {
    ElMessage.warning('请先选择要指派的题目')
    return
  }
  assigningTopic.value = true
  try {
    await assignTopicForMentorApplication(currentMentorApplication.value.id, assignTopicId.value)
    ElMessage.success('题目指派成功')
    assignDialogVisible.value = false
    await loadAllApplications()
  } catch (error: any) {
    ElMessage.error(error.message || '指派失败')
  } finally {
    assigningTopic.value = false
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

.apps-tabs {
  margin-top: 6px;
}
</style>

