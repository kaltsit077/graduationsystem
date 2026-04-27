<template>
  <div class="collab-page">
    <collab-panel
      v-if="approvedApplication"
      identity="teacher"
      :application="approvedApplication"
      :messages="messages"
      :chat-stage="selectedChatStage"
      :show-thesis-upload-button="false"
      @refreshMessages="loadMessages"
      @sendMessage="handleSendMessage"
    >
      <template #headerExtra>
        <el-button
          v-if="approvedApplication?.status === 'COMPLETION_PENDING'"
          type="success"
          size="small"
          @click="openCompletionReview(true)"
        >
          通过结题
        </el-button>
        <el-button
          v-if="approvedApplication?.status === 'COMPLETION_PENDING'"
          type="danger"
          size="small"
          @click="openCompletionReview(false)"
        >
          退回结题
        </el-button>
        <el-button
          type="primary"
          size="small"
          :disabled="!approvedApplication"
          class="student-pick-btn"
          @click="studentSelectDialogVisible = true"
        >
          学生：{{ currentStudentText }}
        </el-button>
        <el-button
          class="win-btn"
          type="primary"
          size="small"
          :disabled="!approvedApplication"
          @click="openWindowDialog"
        >
          配置各环节时间
        </el-button>
      </template>
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
                        :disabled="!row.latestThesisId"
                        @click="row.latestThesisId && openThesisFile(row.latestThesisId)"
                      >
                        打开文件
                      </el-dropdown-item>
                      <el-dropdown-item :disabled="!canInputScore(row)" @click="openEvalDialog(row)">
                        录入成绩
                      </el-dropdown-item>
                      <el-dropdown-item
                        divided
                        :disabled="isCompletionPending || !row.latestThesisId || row.submissionStatus !== 'UNDER_REVIEW'"
                        @click="doWorkflow(row, 'APPROVE')"
                      >
                        审核通过
                      </el-dropdown-item>
                      <el-dropdown-item
                        :disabled="isCompletionPending || !row.latestThesisId || row.submissionStatus !== 'UNDER_REVIEW'"
                        @click="doWorkflow(row, 'NEED_REVISION')"
                      >
                        退回修改
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

    <el-dialog
      v-model="studentSelectDialogVisible"
      title="选择协作学生"
      width="720px"
      destroy-on-close
    >
      <el-table
        :data="applications"
        size="small"
        row-key="id"
        highlight-current-row
        @row-click="selectApplication"
      >
        <el-table-column label="学生" prop="studentName" min-width="220" show-overflow-tooltip />
        <el-table-column label="申请时间" prop="createdAt" min-width="180" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="studentSelectDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="evalDialogVisible" title="录入论文成绩" width="500px">
      <el-form :model="evalForm" label-width="120px">
        <el-form-item label="总评分（0-100）" required>
          <el-input v-model.number="evalForm.score" type="number" min="0" max="100" />
        </el-form-item>
        <el-form-item label="答辩成绩（可选）">
          <el-input v-model.number="evalForm.defenseScore" type="number" min="0" max="100" />
        </el-form-item>
        <el-form-item label="评阅成绩（可选）">
          <el-input v-model.number="evalForm.reviewScore" type="number" min="0" max="100" />
        </el-form-item>
        <el-form-item label="等级（如优/良）">
          <el-input v-model="evalForm.gradeLevel" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input
            v-model="evalForm.comment"
            type="textarea"
            :rows="3"
            placeholder="可简要说明优缺点和修改建议"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEval" :loading="savingEval">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="windowDialogVisible" title="配置该学生的各环节开放时间" width="720px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        class="win-alert"
        title="未启用的时间段可留空；启用时需同时选择开始与结束，且落在管理员设置的毕业季总时间范围内。"
      />
      <el-table :data="windowEditorRows" size="small" class="win-table" max-height="420">
        <el-table-column prop="stageLabel" label="环节" min-width="140" />
        <el-table-column label="开放起止" min-width="340">
          <template #default="{ row }">
            <el-date-picker
              v-model="row.range"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="windowDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingWindows" @click="submitWindows">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="completionDialogVisible" :title="completionApprove ? '通过结题申请' : '退回结题申请'" width="520px">
      <el-form :model="completionForm" label-width="90px">
        <el-form-item label="审核意见">
          <el-input v-model="completionForm.feedback" type="textarea" :rows="4" placeholder="可填写审核意见（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingCompletion" @click="submitCompletionReview">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import CollabPanel from '@/components/CollabPanel.vue'
import { getTopicApplications, reviewCompletionRequest, type Application } from '@/api/application'
import { getTopics } from '@/api/topic'
import { getNotifications, sendChatMessage, type Notification } from '@/api/notification'
import { saveThesisEvaluation, getThesisEvaluation } from '@/api/evaluation'
import { getCollabProgress, saveCollabStageWindows, type StageProgressItem } from '@/api/collab'
import { getThesis, reviewThesisWorkflow } from '@/api/thesis'

interface WindowEditorRow {
  stage: string
  stageLabel: string
  range: [string, string] | null
}

const applications = ref<Application[]>([])
const currentApplicationId = ref<number | null>(null)
const messages = ref<Notification[]>([])
const progressRows = ref<StageProgressItem[]>([])
const loadingProgress = ref(false)
const selectedChatStage = ref<string | null>(null)
const processTableRef = ref()

const evalDialogVisible = ref(false)
const evalThesisId = ref<number | null>(null)
const evalForm = ref({
  score: undefined as number | undefined,
  defenseScore: undefined as number | undefined,
  reviewScore: undefined as number | undefined,
  gradeLevel: '',
  comment: ''
})
const savingEval = ref(false)

const windowDialogVisible = ref(false)
/** 弹窗内编辑行 */
const windowEditorRows = ref<WindowEditorRow[]>([])
const savingWindows = ref(false)

const studentSelectDialogVisible = ref(false)
const SCORE_ENTRY_STAGE = 'THESIS_DEFENSE'

const approvedApplication = computed(() => {
  const manual = applications.value.find((a) => a.id === currentApplicationId.value)
  if (manual) return manual
  return applications.value.find((a) => a.status === 'APPROVED')
    || applications.value.find((a) => a.status === 'COMPLETION_PENDING')
    || applications.value.find((a) => a.status === 'COMPLETION_REJECTED')
    || null
})
const isCompletionPending = computed(() => approvedApplication.value?.status === 'COMPLETION_PENDING')

const completionDialogVisible = ref(false)
const completionApprove = ref(true)
const savingCompletion = ref(false)
const completionForm = ref({
  feedback: ''
})

const currentStudentText = computed(() => {
  return approvedApplication.value?.studentName || '未选择'
})

const selectApplication = (row: Application) => {
  currentApplicationId.value = row.id
  studentSelectDialogVisible.value = false
}

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
    const topicsRes = await getTopics()
    const topics = topicsRes.data || []
    const all: Application[] = []
    for (const t of topics) {
      const appsRes = await getTopicApplications(t.id, true)
      all.push(...(appsRes.data || []))
    }
    applications.value = all.filter(
      (a) => a.status === 'APPROVED' || a.status === 'COMPLETION_PENDING' || a.status === 'COMPLETION_REJECTED'
    )
    if (!currentApplicationId.value && applications.value.length) {
      currentApplicationId.value = applications.value[0].id
    }
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
  if (!approvedApplication.value) return
  if (!collabStage) {
    ElMessage.warning('请选择环节')
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

async function doWorkflow(row: StageProgressItem, decision: 'APPROVE' | 'NEED_REVISION') {
  if (!row.latestThesisId) return
  try {
    await ElMessageBox.confirm(
      decision === 'APPROVE' ? '确认将该稿件标记为「已通过」？' : '确认将该稿件退回修改？',
      '审核确认',
      { type: decision === 'APPROVE' ? 'success' : 'warning' }
    )
  } catch {
    return
  }
  try {
    await reviewThesisWorkflow(row.latestThesisId, decision)
    ElMessage.success('已更新审核状态')
    await loadProgress()
  } catch {
    /* 拦截器 */
  }
}

async function openThesisFile(id: number) {
  try {
    const res = await getThesis(id)
    const url = res.data?.fileUrl
    if (url) window.open(url, '_blank', 'noopener,noreferrer')
    else if (res.data?.downloadExpired) ElMessage.warning('该文件下载已过期')
    else ElMessage.warning('未找到文件地址')
  } catch {
    ElMessage.error('无法打开文件')
  }
}

const openEvalDialog = (row: StageProgressItem) => {
  if (isCompletionPending.value) {
    ElMessage.warning('结题审核中，评分入口已锁定')
    return
  }
  if (!row.latestThesisId) return
  if (row.stage !== SCORE_ENTRY_STAGE) {
    ElMessage.warning('仅允许在「论文答辩」环节录入论文总成绩')
    return
  }
  evalThesisId.value = row.latestThesisId
  evalForm.value = {
    score: undefined,
    defenseScore: undefined,
    reviewScore: undefined,
    gradeLevel: '',
    comment: ''
  }
  ;(async () => {
    try {
      const res = await getThesisEvaluation(row.latestThesisId!)
      if (res.data && res.data.score != null) {
        evalForm.value.score = Number(res.data.score)
        evalForm.value.defenseScore = res.data.defenseScore != null ? Number(res.data.defenseScore) : undefined
        evalForm.value.reviewScore = res.data.reviewScore != null ? Number(res.data.reviewScore) : undefined
        evalForm.value.gradeLevel = res.data.gradeLevel || ''
        evalForm.value.comment = res.data.comment || ''
      }
    } catch {
      /* ignore */
    }
  })()
  evalDialogVisible.value = true
}

const canInputScore = (row: StageProgressItem) => {
  return !isCompletionPending.value && !!row.latestThesisId && row.stage === SCORE_ENTRY_STAGE
}

const submitEval = async () => {
  if (!evalThesisId.value) return
  if (evalForm.value.score == null) {
    ElMessage.warning('请至少填写总评分')
    return
  }
  savingEval.value = true
  try {
    await saveThesisEvaluation({
      thesisId: evalThesisId.value,
      score: evalForm.value.score,
      defenseScore: evalForm.value.defenseScore,
      reviewScore: evalForm.value.reviewScore,
      gradeLevel: evalForm.value.gradeLevel,
      comment: evalForm.value.comment
    })
    ElMessage.success('成绩已保存')
    evalDialogVisible.value = false
    await loadProgress()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingEval.value = false
  }
}

function openWindowDialog() {
  if (!approvedApplication.value || isCompletionPending.value) return
  windowEditorRows.value = progressRows.value.map((r) => {
    let range: [string, string] | null = null
    if (r.windowStart && r.windowEnd) {
      range = [r.windowStart, r.windowEnd]
    }
    return {
      stage: r.stage,
      stageLabel: r.stageLabel,
      range
    }
  })
  windowDialogVisible.value = true
}

const openCompletionReview = (approve: boolean) => {
  if (!approvedApplication.value) return
  if (approvedApplication.value.status !== 'COMPLETION_PENDING') {
    ElMessage.warning('当前不在结题待审核状态')
    return
  }
  completionApprove.value = approve
  completionForm.value = { feedback: '' }
  completionDialogVisible.value = true
}

const submitCompletionReview = async () => {
  if (!approvedApplication.value) return
  savingCompletion.value = true
  try {
    await reviewCompletionRequest(approvedApplication.value.id, {
      status: completionApprove.value ? 'APPROVED' : 'REJECTED',
      feedback: completionForm.value.feedback
    })
    ElMessage.success('结题审核已提交')
    completionDialogVisible.value = false
    await loadApplications()
    await loadProgress()
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    savingCompletion.value = false
  }
}

const submitWindows = async () => {
  if (!approvedApplication.value || isCompletionPending.value) return
  savingWindows.value = true
  try {
    const items = windowEditorRows.value.map((row) => {
      if (row.range && row.range.length === 2) {
        return {
          stage: row.stage,
          windowStart: row.range[0],
          windowEnd: row.range[1]
        }
      }
      return { stage: row.stage, windowStart: null, windowEnd: null }
    })
    await saveCollabStageWindows(approvedApplication.value.id, items)
    ElMessage.success('环节时间已保存')
    windowDialogVisible.value = false
    await loadProgress()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingWindows.value = false
  }
}

watch(selectedChatStage, () => {
  loadMessages()
})

watch(
  currentApplicationId,
  async (id) => {
    selectedChatStage.value = null
    if (!id) {
      progressRows.value = []
      messages.value = []
      return
    }
    await loadProgress()
    await loadMessages()
  },
  { immediate: true }
)

onMounted(async () => {
  await loadApplications()
})
</script>

<style scoped>
.collab-page {
  max-width: 1400px;
  margin: 0 auto;
}

.collab-header {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.win-btn {
  margin-left: auto;
}

.collab-header-label {
  font-size: 13px;
  color: #606266;
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

.win-alert {
  margin-bottom: 12px;
}

.row-action-btn {
  padding: 0;
}

.row-action-icon {
  margin-left: 2px;
  font-size: 12px;
}

</style>
