<template>
  <div class="teacher-load-page">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>导师负荷概览</span>
              <el-button size="small" @click="loadTeacherLoad" :loading="loadingLoad">刷新</el-button>
            </div>
          </template>
          <el-table :data="teacherLoad" v-loading="loadingLoad" style="width: 100%">
            <el-table-column prop="realName" label="导师姓名" width="120" />
            <el-table-column prop="currentStudents" label="当前学生数" width="120" />
            <el-table-column prop="maxStudents" label="最大学生数" width="120" />
            <el-table-column prop="openTopics" label="开放选题数" width="120" />
            <el-table-column prop="totalTopics" label="总选题数" width="120" />
          </el-table>
          <div v-if="!teacherLoad.length && !loadingLoad" class="empty-tip">
            暂无导师负荷数据。
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>更换导师申请</span>
              <el-button size="small" @click="loadChangeRequests" :loading="loadingChanges">刷新</el-button>
            </div>
          </template>
          <el-table :data="changeRequests" v-loading="loadingChanges" style="width: 100%">
            <el-table-column prop="id" label="申请ID" width="80" />
            <el-table-column prop="studentName" label="学生姓名" width="120" />
            <el-table-column prop="currentTeacherName" label="当前导师" width="120" />
            <el-table-column prop="topicTitle" label="当前选题" min-width="200" />
            <el-table-column prop="reason" label="申请原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="提交时间" width="180" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="handleDecision(row, 'APPROVED')">
                  同意解除绑定
                </el-button>
                <el-button type="danger" size="small" @click="handleDecision(row, 'REJECTED')">
                  拒绝申请
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!changeRequests.length && !loadingChanges" class="empty-tip">
            当前没有待处理的更换导师申请。
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminPendingChangeRequests, submitAdminChangeDecision, type ChangeRequest, type Decision } from '@/api/changeRequest'
import request from '@/api/request'

interface TeacherLoadItem {
  teacherId: number
  realName: string
  currentStudents: number
  maxStudents: number
  openTopics: number
  totalTopics: number
}

interface ChangeRequestView extends ChangeRequest {
  studentName?: string
  currentTeacherName?: string
  topicTitle?: string
}

const teacherLoad = ref<TeacherLoadItem[]>([])
const loadingLoad = ref(false)

const changeRequests = ref<ChangeRequestView[]>([])
const loadingChanges = ref(false)

const loadTeacherLoad = async () => {
  loadingLoad.value = true
  try {
    const res = await request.get('/admin/teacher-load')
    teacherLoad.value = res.data || []
  } catch {
    teacherLoad.value = []
    ElMessage.error('加载导师负荷数据失败')
  } finally {
    loadingLoad.value = false
  }
}

const loadChangeRequests = async () => {
  loadingChanges.value = true
  try {
    const res = await getAdminPendingChangeRequests()
    const list = res.data || []
    const enriched: ChangeRequestView[] = []
    for (const item of list) {
      let studentName: string | undefined
      let currentTeacherName: string | undefined
      let topicTitle: string | undefined
      try {
        const appRes = await request.get('/applications/' + item.currentApplicationId)
        const app = appRes.data
        studentName = app?.studentName
        topicTitle = app?.topicTitle
        // 通过选题获取导师姓名
        if (app?.topicId) {
          const topicRes = await request.get('/topics/' + app.topicId)
          currentTeacherName = topicRes.data?.teacherName
        }
      } catch {
        // 忽略补充失败
      }
      enriched.push({
        ...item,
        studentName,
        currentTeacherName,
        topicTitle
      })
    }
    changeRequests.value = enriched
  } catch {
    ElMessage.error('加载更换导师申请失败')
  } finally {
    loadingChanges.value = false
  }
}

const handleDecision = async (row: ChangeRequestView, decision: Decision) => {
  const actionText = decision === 'APPROVED' ? '同意解除当前导师绑定' : '拒绝本次更换导师申请'
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
    await submitAdminChangeDecision(row.id, decision, comment || undefined)
    ElMessage.success('已提交审批结果')
    loadChangeRequests()
  } catch (err: any) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error(err?.message || '操作失败')
  }
}

onMounted(() => {
  loadTeacherLoad()
  loadChangeRequests()
})
</script>

<style scoped>
.teacher-load-page {
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

