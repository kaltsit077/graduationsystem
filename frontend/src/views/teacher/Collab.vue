<template>
  <!-- 教师协作页面主布局，复用通用协作面板 -->
  <div class="collab-page">
    <!-- 顶部学生/申请切换条：导师可在名下多个学生之间快速切换协作对象 -->
    <div class="collab-header" v-if="applications.length">
      <span class="collab-header-label">当前协作学生：</span>
      <el-segmented
        v-model="currentApplicationId"
        :options="applications.map(a => ({ label: a.studentName + ' / ' + a.topicTitle, value: a.id }))"
        size="small"
        @change="handleSelectApplication"
      />
    </div>

    <collab-panel
      identity="teacher"
      :application="approvedApplication"
      :messages="messages"
      @refreshMessages="loadMessages"
      @sendMessage="handleSendMessage"
      @uploadThesis="() => {}"
    >
      <!-- 论文列表插槽：展示当前教师名下学生的论文，并提供录入成绩入口 -->
      <template #thesis>
        <el-table :data="theses" style="width: 100%" size="small" v-loading="loadingThesis">
          <el-table-column prop="studentName" label="学生" width="120" />
          <el-table-column prop="fileName" label="文件名" min-width="200" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="createdAt" label="上传时间" width="180" />
          <el-table-column label="操作" width="200">
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
              <el-button type="primary" link size="small" @click="openEvalDialog(row)">录入成绩</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </collab-panel>

    <!-- 录入/查看论文成绩的弹窗，对应单篇论文的教师评价 -->
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import CollabPanel from '@/components/CollabPanel.vue'
import { getTopicApplications, type Application } from '@/api/application'
import { getTopics } from '@/api/topic'
import { getTeacherTheses, type Thesis } from '@/api/thesis'
import { getNotifications, sendChatMessage, type Notification } from '@/api/notification'
import { saveThesisEvaluation, getThesisEvaluation } from '@/api/evaluation'

// 教师名下已通过的选题申请列表，用于确定当前协作上下文
const applications = ref<Application[]>([])
// 当前选中的申请ID（导师可在多个学生之间切换）
const currentApplicationId = ref<number | null>(null)
// 当前选题下的消息列表（由协作面板展示）
const messages = ref<Notification[]>([])
// 当前教师名下的所有学生论文列表
const theses = ref<Thesis[]>([])
const loadingThesis = ref(false)

// 论文成绩录入弹窗的显隐和表单状态
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

// 当前用于协作的“选题申请”（教师一旦有多个选题，可以根据需要调整选择逻辑）
const approvedApplication = computed(() => {
  const manual = applications.value.find((a) => a.id === currentApplicationId.value)
  if (manual) return manual
  return applications.value.find((a) => a.status === 'APPROVED') || null
})

// 导师切换当前协作的学生/申请
const handleSelectApplication = (id: number) => {
  currentApplicationId.value = id
  loadMessages()
}

// 加载当前教师名下所有已通过的选题申请
const loadApplications = async () => {
  try {
    const topicsRes = await getTopics()
    const topics = topicsRes.data || []
    const all: Application[] = []
    for (const t of topics) {
      const appsRes = await getTopicApplications(t.id, true)
      all.push(...(appsRes.data || []))
    }
    applications.value = all.filter((a) => a.status === 'APPROVED')
  } catch {
    ElMessage.error('加载申请记录失败')
  }
}

// 加载教师名下的学生论文列表
const loadTheses = async () => {
  loadingThesis.value = true
  try {
    const res = await getTeacherTheses()
    theses.value = res.data || []
  } catch {
    ElMessage.error('加载作业失败')
  } finally {
    loadingThesis.value = false
  }
}

// 加载当前选题下的协作消息
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

// 发送协作消息，由协作面板触发
const handleSendMessage = async (content: string) => {
  if (!approvedApplication.value) return
  try {
    // 导师端也可以不显式指定学生ID，由后端根据 relatedId + 当前角色推断接收人
    await sendChatMessage(0, content, approvedApplication.value.id)
    ElMessage.success('消息已发送')
    await loadMessages()
  } catch {
    ElMessage.error('发送失败')
  }
}

// 打开成绩录入弹窗，并尝试加载已有的教师评价进行预填
const openEvalDialog = (row: Thesis) => {
  evalThesisId.value = row.id
  evalForm.value = {
    score: undefined,
    defenseScore: undefined,
    reviewScore: undefined,
    gradeLevel: '',
    comment: ''
  }
  ;(async () => {
    try {
      const res = await getThesisEvaluation(row.id)
      if (res.data && res.data.score != null) {
        evalForm.value.score = res.data.score
        evalForm.value.defenseScore = res.data.defenseScore
        evalForm.value.reviewScore = res.data.reviewScore
        evalForm.value.gradeLevel = res.data.gradeLevel || ''
        evalForm.value.comment = res.data.comment || ''
      }
    } catch {
      // 忽略加载失败，保持空表单
    }
  })()
  evalDialogVisible.value = true
}

const openFile = (url: string) => {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

// 提交教师对论文的成绩评价
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
    await loadTheses()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingEval.value = false
  }
}

// 页面挂载时，加载教师协作所需的基础数据
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

.collab-header {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.collab-header-label {
  font-size: 13px;
  color: #606266;
}
</style>
