<template>
  <div class="collab-page">
    <div class="collab-toolbar" v-if="approvedApplication">
      <span class="collab-toolbar-text">当前已绑定选题，如确需调整，可发起变更申请：</span>
      <el-button-group>
        <el-button size="small" @click="openChangeDialog('CHANGE_TOPIC')">申请更换选题</el-button>
        <el-button size="small" type="warning" @click="openChangeDialog('CHANGE_TEACHER')">申请更换导师</el-button>
      </el-button-group>
    </div>

    <collab-panel
      identity="student"
      :application="approvedApplication"
      :messages="messages"
      @refreshMessages="loadMessages"
      @sendMessage="handleSendMessage"
      @uploadThesis="openUpload"
    >
      <template #thesis>
        <el-table :data="theses" style="width: 100%" size="small" v-loading="loadingThesis">
          <el-table-column prop="fileName" label="文件名" min-width="200" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="createdAt" label="上传时间" width="180" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button
                v-if="row.fileUrl"
                type="primary"
                link
                size="small"
                @click="openFile(row.fileUrl)"
              >
                打开文件
              </el-button>
              <el-button type="primary" link size="small" @click="openFeedback(row)">评价论文与导师</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </collab-panel>

    <AppDialog
      v-model="uploadDialogVisible"
      title="上传作业"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="upload-box">
        <el-upload
          drag
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="doUpload"
        >
          <el-icon style="font-size: 34px; color: #409eff"><UploadFilled /></el-icon>
          <div class="el-upload__text">将文件拖到这里，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-tip">支持 pdf/doc/docx/ppt/pptx/zip/rar/7z，大小 ≤ 30MB</div>
          </template>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="uploadDialogVisible = false" :disabled="uploading">完成</el-button>
      </template>
    </AppDialog>

    <AppDialog
      v-model="feedbackDialogVisible"
      title="评价论文与导师"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="feedbackForm" label-width="110px">
        <el-form-item label="整体评分（0-100）" required>
          <el-input v-model.number="feedbackForm.score" type="number" min="0" max="100" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="feedbackForm.comment"
            type="textarea"
            :rows="3"
            placeholder="可以从论文难度、指导投入、收获体会等方面简单评价"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback" :loading="savingFeedback">提交</el-button>
      </template>
    </AppDialog>

    <AppDialog
      v-model="changeDialogVisible"
      :title="changeDialogTitle"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="changeForm" label-width="100px">
        <el-form-item label="申请类型">
          <el-tag type="info" v-if="changeForm.type === 'CHANGE_TOPIC'">更换选题</el-tag>
          <el-tag type="warning" v-else>更换导师</el-tag>
        </el-form-item>
        <el-form-item label="申请原因" required>
          <el-input
            v-model="changeForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请简要说明希望更换的原因，如题目难度/资源限制/发展规划调整等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingChange" @click="submitChangeRequest">提交申请</el-button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import AppDialog from '@/components/AppDialog.vue'
import CollabPanel from '@/components/CollabPanel.vue'
import { getMyApplications, type Application } from '@/api/application'
import { getMyTheses, uploadThesisFile, type Thesis } from '@/api/thesis'
import { getNotifications, sendChatMessage, type Notification } from '@/api/notification'
import { saveStudentThesisFeedback, getThesisEvaluation } from '@/api/evaluation'
import { createChangeRequest, type ChangeType } from '@/api/changeRequest'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// 学生名下所有申请记录
const applications = ref<Application[]>([])
// 当前协作所选中的申请（可能有多个通过记录时，允许手动切换）
const currentApplicationId = ref<number | null>(null)
const messages = ref<Notification[]>([])
const theses = ref<Thesis[]>([])
const loadingThesis = ref(false)
const uploadDialogVisible = ref(false)
const uploading = ref(false)

const feedbackDialogVisible = ref(false)
const feedbackThesisId = ref<number | null>(null)
const feedbackForm = ref({
  score: undefined as number | undefined,
  comment: ''
})
const savingFeedback = ref(false)

const changeDialogVisible = ref(false)
const submittingChange = ref(false)
const changeForm = ref<{
  type: ChangeType
  reason: string
}>({
  type: 'CHANGE_TOPIC',
  reason: ''
})

const changeDialogTitle = computed(() =>
  changeForm.value.type === 'CHANGE_TOPIC' ? '申请更换选题' : '申请更换导师'
)

// 当前用于协作的申请：优先使用手动选择，其次回退到第一条已通过申请
const approvedApplication = computed(() => {
  const manual = applications.value.find((a) => a.id === currentApplicationId.value)
  if (manual) return manual
  return applications.value.find((a) => a.status === 'APPROVED') || null
})

// 预留：如需在学生端支持多申请协作切换，可在此处理 currentApplicationId 并刷新数据
// 当前版本中学生协作始终使用唯一已通过申请，因此暂未暴露切换入口。

const loadApplications = async () => {
  try {
    const res = await getMyApplications()
    applications.value = res.data || []
  } catch {
    ElMessage.error('加载申请记录失败')
  }
}

const loadTheses = async () => {
  if (!approvedApplication.value) return
  loadingThesis.value = true
  try {
    const res = await getMyTheses()
    const all = res.data || []
    theses.value = all.filter((t) => t.topicId === approvedApplication.value!.topicId)
  } catch {
    ElMessage.error('加载作业失败')
  } finally {
    loadingThesis.value = false
  }
}

const loadMessages = async () => {
  if (!approvedApplication.value) return
  try {
    const res = await getNotifications({
      type: 'CHAT',
      relatedId: approvedApplication.value.id,
      limit: 200
    })
    messages.value = res.data || []
  } catch {
    ElMessage.error('加载消息失败')
  }
}

const handleSendMessage = async (content: string) => {
  if (!approvedApplication.value || !authStore.userId) return
  // 学生端：不直接关心导师ID，由后端根据 relatedId + 当前角色推断接收人
  try {
    await sendChatMessage(0, content, approvedApplication.value.id)
    ElMessage.success('消息已发送')
    await loadMessages()
  } catch {
    ElMessage.error('发送失败')
  }
}

const openUpload = () => {
  if (!approvedApplication.value) {
    ElMessage.warning('请先确认选题')
    return
  }
  uploadDialogVisible.value = true
}

const beforeUpload = (file: File) => {
  const ok = file.size <= 30 * 1024 * 1024
  if (!ok) {
    ElMessage.error('文件过大，请上传 30MB 以内的文件')
    return false
  }
  return true
}

const doUpload = async (opts: any) => {
  if (!approvedApplication.value) return
  const file = opts?.file as File
  if (!file) return
  uploading.value = true
  try {
    await uploadThesisFile(approvedApplication.value.topicId, file)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    await loadTheses()
  } catch {
    // message 已由拦截器处理
  } finally {
    uploading.value = false
  }
}

const openFile = (url: string) => {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

const openFeedback = async (row: Thesis) => {
  feedbackThesisId.value = row.id
  feedbackForm.value = {
    score: undefined,
    comment: ''
  }
  // 如果之前评价过，预填
  try {
    const res = await getThesisEvaluation(row.id)
    if (res.data && res.data.studentScore != null) {
      feedbackForm.value.score = res.data.studentScore
      feedbackForm.value.comment = res.data.studentComment || ''
    }
  } catch {
    // 忽略加载失败，保持空表单
  }
  feedbackDialogVisible.value = true
}

const submitFeedback = async () => {
  if (!feedbackThesisId.value) return
  if (feedbackForm.value.score == null) {
    ElMessage.warning('请至少给出一个评分')
    return
  }
  savingFeedback.value = true
  try {
    await saveStudentThesisFeedback({
      thesisId: feedbackThesisId.value,
      score: feedbackForm.value.score,
      comment: feedbackForm.value.comment
    })
    ElMessage.success('评价已提交')
    feedbackDialogVisible.value = false
  } catch {
    ElMessage.error('提交失败')
  } finally {
    savingFeedback.value = false
  }
}

const openChangeDialog = (type: ChangeType) => {
  if (!approvedApplication.value) {
    ElMessage.warning('当前尚未绑定选题，无法发起变更')
    return
  }
  changeForm.value = {
    type,
    reason: ''
  }
  changeDialogVisible.value = true
}

const submitChangeRequest = async () => {
  if (!changeForm.value.reason.trim()) {
    ElMessage.warning('请填写申请原因')
    return
  }
  submittingChange.value = true
  try {
    await createChangeRequest({
      type: changeForm.value.type,
      reason: changeForm.value.reason.trim()
    })
    ElMessage.success('变更申请已提交，请等待审批')
    changeDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '提交变更申请失败')
  } finally {
    submittingChange.value = false
  }
}

onMounted(async () => {
  await loadApplications()
  await loadTheses()
  await loadMessages()
})
</script>

<style scoped>
.collab-page {
  max-width: 1400px;
  margin: 0 auto;
}

.collab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  background: #f0f9ff;
  border: 1px solid #d9ecff;
}

.collab-toolbar-text {
  font-size: 13px;
  color: #606266;
}

.upload-box {
  padding: 6px 0 2px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
</style>