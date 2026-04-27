<template>
  <div class="collab-page">
    <div class="collab-toolbar" v-if="approvedApplication">
      <span class="collab-toolbar-text">当前已绑定选题，如确需调整，可发起变更申请：</span>
      <el-button-group>
        <el-button size="small" type="primary" :disabled="!canOpenGlobalFeedback" @click="openFeedbackFromToolbar">评价导师</el-button>
        <el-button
          size="small"
          type="success"
          :disabled="!canSubmitCompletion"
          @click="submitCompletion"
        >
          {{ approvedApplication?.status === 'COMPLETION_PENDING' ? '结题审核中' : '申请结题' }}
        </el-button>
        <el-button size="small" @click="openChangeDialog('CHANGE_TOPIC')">申请更换选题</el-button>
        <el-button size="small" type="warning" @click="openChangeDialog('CHANGE_TEACHER')">申请更换导师</el-button>
      </el-button-group>
    </div>

    <collab-panel
      v-if="approvedApplication"
      identity="student"
      :application="approvedApplication"
      :messages="messages"
      :chat-stage="selectedChatStage"
      :show-thesis-upload-button="false"
      @refreshMessages="loadMessages"
      @sendMessage="handleSendMessage"
    >
      <template #thesis>
        <el-table
          :data="treeRows"
          v-loading="loadingProgress"
          size="small"
          highlight-current-row
          row-key="id"
          class="process-table"
          @row-click="onSelectStage"
          :row-class-name="tableRowClass"
          :tree-props="{ children: 'children' }"
          ref="processTableRef"
        >
          <el-table-column label="序号" width="58">
            <template #default="{ row }">
              <span v-if="row.type === 'stage'">{{ row.orderIndex }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="毕设环节" min-width="168">
            <template #default="{ row }">
              <div v-if="row.type === 'phase'" class="phase-title">{{ row.phaseLabel }}</div>
              <template v-else>
                <div class="stage-title">{{ row.stageLabel }}</div>
                <div class="stage-access" :class="accessClass(row.accessState)">{{ accessLabel(row.accessState) }}</div>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="时间计划" min-width="236" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.type === 'stage'">{{ row.timePlanText }}</span>
              <span v-else class="phase-hint">阶段内环节可并行；需上一阶段全部通过才解锁</span>
            </template>
          </el-table-column>
          <el-table-column label="提交与审核" width="132">
            <template #default="{ row }">
              <span v-if="row.type === 'stage'" :class="submitClass(row.submissionStatus)">{{
                row.submissionStatusLabel
              }}</span>
              <span v-else class="phase-hint">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right" align="center" header-align="center">
            <template #default="{ row }">
              <template v-if="row.type === 'stage'">
                <el-dropdown trigger="click" @click.stop>
                  <el-button link size="small" class="row-action-btn">
                    编辑
                    <el-icon class="row-action-icon"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        :disabled="
                          isCompletionPending ||
                          row.accessState !== 'OPEN' ||
                          row.submissionStatus === 'UNDER_REVIEW' ||
                          row.submissionStatus === 'BLOCKED_BY_PREVIOUS'
                        "
                        @click="openUploadForStage(row.stage)"
                      >
                        上传材料
                      </el-dropdown-item>
                      <el-dropdown-item
                        :disabled="!row.latestThesisId || !row.latestFileName"
                        @click="openProgressFile(row)"
                      >
                        打开文件
                      </el-dropdown-item>
                      <el-dropdown-item :disabled="!canRateTeacher(row)" @click="openFeedback(row)">
                        评价导师
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
              <span v-else class="phase-actions">—</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </collab-panel>

    <AppDialog
      v-model="uploadDialogVisible"
      title="上传环节材料"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="upload-hint" v-if="uploadTargetStage">
        当前环节：<strong>{{ stageLabel(uploadTargetStage) }}</strong>
      </div>
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
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { ArrowDown } from '@element-plus/icons-vue'
import AppDialog from '@/components/AppDialog.vue'
import CollabPanel from '@/components/CollabPanel.vue'
import { getMyApplications, submitCompletionRequest, type Application } from '@/api/application'
import { uploadThesisFile, getThesis } from '@/api/thesis'
import { getNotifications, sendChatMessage, type Notification } from '@/api/notification'
import { saveStudentThesisFeedback, getThesisEvaluation } from '@/api/evaluation'
import { createChangeRequest, type ChangeType } from '@/api/changeRequest'
import { getCollabProgress, type StageProgressItem } from '@/api/collab'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const applications = ref<Application[]>([])
const currentApplicationId = ref<number | null>(null)
const messages = ref<Notification[]>([])
const progressRows = ref<StageProgressItem[]>([])
const loadingProgress = ref(false)
const selectedChatStage = ref<string | null>(null)
const uploadDialogVisible = ref(false)
const uploadTargetStage = ref<string | null>(null)
const uploading = ref(false)
const processTableRef = ref()

const feedbackDialogVisible = ref(false)
const feedbackThesisId = ref<number | null>(null)
const feedbackForm = ref({
  score: undefined as number | undefined,
  comment: ''
})
const savingFeedback = ref(false)
const FEEDBACK_STAGE = 'THESIS_DEFENSE'

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

const approvedApplication = computed(() => {
  const manual = applications.value.find((a) => a.id === currentApplicationId.value)
  if (manual) return manual
  return applications.value.find((a) => a.status === 'APPROVED')
    || applications.value.find((a) => a.status === 'COMPLETION_PENDING')
    || applications.value.find((a) => a.status === 'COMPLETION_REJECTED')
    || null
})
const isCompletionPending = computed(() => approvedApplication.value?.status === 'COMPLETION_PENDING')
const canOpenGlobalFeedback = computed(() => {
  const defenseRow = progressRows.value.find((r) => r.stage === FEEDBACK_STAGE)
  return !!defenseRow?.latestThesisId && canRateTeacher(defenseRow)
})
const canSubmitCompletion = computed(() => {
  if (!approvedApplication.value) return false
  if (approvedApplication.value.status === 'COMPLETION_PENDING') return false
  return canRateAfterCompletion.value
})

function accessLabel(state: string) {
  if (state === 'NOT_CONFIGURED') return '未开放'
  if (state === 'NOT_OPEN_YET') return '未开始'
  if (state === 'OPEN') return '已开启'
  if (state === 'ENDED') return '已结束'
  return state
}

function accessClass(state: string) {
  if (state === 'OPEN') return 'acc-open'
  if (state === 'ENDED') return 'acc-ended'
  return 'acc-muted'
}

function submitClass(sub: string) {
  if (sub === 'UNDER_REVIEW') return 'sub-review'
  if (sub === 'NEED_REVISION') return 'sub-reject'
  if (sub === 'APPROVED') return 'sub-ok'
  if (sub === 'BLOCKED_BY_PREVIOUS') return 'sub-muted'
  return 'sub-muted'
}

function stageLabel(stage: string) {
  const row = progressRows.value.find((r) => r.stage === stage)
  return row?.stageLabel || stage
}

const canRateAfterCompletion = computed(() => {
  if (!progressRows.value.length) return false
  return progressRows.value.every((r) => r.submissionStatus === 'APPROVED')
})

function canRateTeacher(row: StageProgressItem) {
  return !!row.latestThesisId && row.stage === FEEDBACK_STAGE && canRateAfterCompletion.value
}

type TreeRow =
  | ({ type: 'phase'; id: string; phaseIndex: number; phaseLabel: string; children: TreeRow[] } & Record<string, any>)
  | ({ type: 'stage'; id: string } & StageProgressItem)

const treeRows = computed<TreeRow[]>(() => {
  const groups = new Map<number, { phaseLabel: string; items: StageProgressItem[] }>()
  for (const r of progressRows.value) {
    const g = groups.get(r.phaseIndex) || { phaseLabel: r.phaseLabel, items: [] }
    g.items.push(r)
    g.phaseLabel = r.phaseLabel
    groups.set(r.phaseIndex, g)
  }
  return Array.from(groups.entries())
    .sort((a, b) => a[0] - b[0])
    .map(([phaseIndex, g]) => {
      const children: TreeRow[] = g.items
        .sort((a, b) => a.orderIndex - b.orderIndex)
        .map((x) => ({ type: 'stage', id: `stage-${x.stage}`, ...x }))
      return {
        type: 'phase',
        id: `phase-${phaseIndex}`,
        phaseIndex,
        phaseLabel: g.phaseLabel,
        children
      } as TreeRow
    })
})

function tableRowClass({ row }: { row: any }) {
  if (row.type === 'phase') return 'is-phase-row'
  return selectedChatStage.value === row.stage ? 'is-active-stage' : ''
}

function onSelectStage(row: any) {
  if (row.type === 'phase') {
    ;(processTableRef.value as any)?.toggleRowExpansion?.(row)
    return
  }
  if (row.type !== 'stage') return
  selectedChatStage.value = row.stage
}

function openUploadForStage(stage: string) {
  uploadTargetStage.value = stage
  uploadDialogVisible.value = true
}

async function openProgressFile(row: StageProgressItem) {
  if (!row.latestThesisId) return
  try {
    const res = await getThesis(row.latestThesisId)
    const url = res.data?.fileUrl
    if (url) window.open(url, '_blank', 'noopener,noreferrer')
    else if (res.data?.downloadExpired) ElMessage.warning('该文件下载已过期')
    else ElMessage.warning('未找到文件地址')
  } catch {
    ElMessage.error('无法打开文件')
  }
}

function pickDefaultStage() {
  const openFirst = progressRows.value.find((r) => r.accessState === 'OPEN')
  if (openFirst) {
    selectedChatStage.value = openFirst.stage
    return
  }
  if (progressRows.value.length) {
    selectedChatStage.value = progressRows.value[0].stage
  }
}

const loadApplications = async () => {
  try {
    const res = await getMyApplications()
    applications.value = res.data || []
  } catch {
    ElMessage.error('加载申请记录失败')
  }
}

const loadProgress = async () => {
  if (!approvedApplication.value) {
    progressRows.value = []
    return
  }
  loadingProgress.value = true
  try {
    const res = await getCollabProgress(approvedApplication.value.id)
    progressRows.value = res.data || []
    if (!selectedChatStage.value || !progressRows.value.some((r) => r.stage === selectedChatStage.value)) {
      pickDefaultStage()
    }
  } catch {
    ElMessage.error('加载环节进度失败')
  } finally {
    loadingProgress.value = false
  }
}


const loadMessages = async () => {
  if (!approvedApplication.value || !selectedChatStage.value) {
    messages.value = []
    return
  }
  try {
    const res = await getNotifications({
      type: 'CHAT',
      relatedId: approvedApplication.value.id,
      collabStage: selectedChatStage.value,
      limit: 200
    })
    messages.value = res.data || []
  } catch {
    ElMessage.error('加载消息失败')
  }
}

const handleSendMessage = async (content: string, collabStage: string | null | undefined) => {
  if (!approvedApplication.value || !authStore.userId) return
  if (!collabStage) {
    ElMessage.warning('请先选择环节')
    return
  }
  try {
    await sendChatMessage(0, content, approvedApplication.value.id, collabStage)
    ElMessage.success('消息已发送')
    await loadMessages()
  } catch {
    ElMessage.error('发送失败')
  }
}

const beforeUpload = (file: File) => {
  const ok = file.size <= 30 * 1024 * 1024
  if (!ok) {
    ElMessage.error('文件过大，请上传 30MB 以内的文件')
    return false
  }
  return true
}

const doUpload = async (opts: { file?: File }) => {
  if (!approvedApplication.value || !uploadTargetStage.value || isCompletionPending.value) return
  const file = opts?.file
  if (!file) return
  uploading.value = true
  try {
    await uploadThesisFile(approvedApplication.value.topicId, file, uploadTargetStage.value)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    await loadProgress()
  } catch {
    /* 拦截器 */
  } finally {
    uploading.value = false
  }
}

const openFeedback = async (row: StageProgressItem) => {
  if (isCompletionPending.value) {
    ElMessage.warning('结题审核中，暂不可修改评价')
    return
  }
  if (!row.latestThesisId) return
  if (row.stage !== FEEDBACK_STAGE) {
    ElMessage.warning('请在「论文答辩」环节进行导师评价')
    return
  }
  if (!canRateAfterCompletion.value) {
    ElMessage.warning('需在选题全部环节完成并通过后，才能评价导师')
    return
  }
  feedbackThesisId.value = row.latestThesisId
  feedbackForm.value = {
    score: undefined,
    comment: ''
  }
  try {
    const res = await getThesisEvaluation(row.latestThesisId)
    if (res.data && res.data.studentScore != null) {
      feedbackForm.value.score = res.data.studentScore
      feedbackForm.value.comment = res.data.studentComment || ''
    }
  } catch {
    /* ignore */
  }
  feedbackDialogVisible.value = true
}

const submitFeedback = async () => {
  if (isCompletionPending.value) return
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

const openFeedbackFromToolbar = () => {
  const defenseRow = progressRows.value.find((r) => r.stage === FEEDBACK_STAGE && r.latestThesisId)
  if (!defenseRow) {
    ElMessage.warning('请先在论文答辩环节上传材料')
    return
  }
  openFeedback(defenseRow)
}

const submitCompletion = async () => {
  if (!approvedApplication.value) return
  if (!canSubmitCompletion.value) {
    ElMessage.warning('请先完成全流程审核通过，并完成导师评价后再申请结题')
    return
  }
  try {
    await ElMessageBox.confirm('确认提交结题申请？提交后将进入导师审核。', '结题申请', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await submitCompletionRequest(approvedApplication.value.id)
    ElMessage.success('结题申请已提交，请等待导师审核')
    await loadApplications()
    await loadProgress()
  } catch (e: any) {
    ElMessage.error(e?.message || '结题申请提交失败')
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
  } catch (e: unknown) {
    const msg = e && typeof e === 'object' && 'message' in e ? String((e as { message?: string }).message) : ''
    ElMessage.error(msg || '提交变更申请失败')
  } finally {
    submittingChange.value = false
  }
}

watch(selectedChatStage, () => {
  loadMessages()
})

onMounted(async () => {
  await loadApplications()
  await loadProgress()
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

.upload-hint {
  font-size: 13px;
  margin-bottom: 8px;
  color: #606266;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.process-table :deep(.is-active-stage) {
  background: #f0f9ff !important;
}

.process-table :deep(.is-phase-row) {
  background: #f6f8fb !important;
}

.process-table :deep(.el-table__expand-icon) {
  display: none;
}

.phase-title {
  font-weight: 600;
  color: #303133;
}

.phase-hint {
  font-size: 12px;
  color: #909399;
}

.phase-actions {
  color: #c0c4cc;
}

.row-action-btn {
  padding: 0;
}

.row-action-icon {
  margin-left: 2px;
  font-size: 12px;
}

.stage-title {
  font-weight: 500;
  font-size: 13px;
}
.stage-access {
  font-size: 12px;
  margin-top: 2px;
}
.acc-open {
  color: #409eff;
}
.acc-ended {
  color: #909399;
}
.acc-muted {
  color: #c0c4cc;
}

.sub-review {
  color: #e6a23c;
}
.sub-reject {
  color: #f56c6c;
}
.sub-ok {
  color: #67c23a;
}
.sub-muted {
  color: #909399;
}

</style>
