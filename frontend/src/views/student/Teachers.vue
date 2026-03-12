<template>
  <div class="student-teachers-page">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent>
        <el-form-item label="导师姓名">
          <el-input v-model="filters.keyword" placeholder="按姓名或方向搜索" clearable @keyup.enter.native="loadData" />
        </el-form-item>
        <el-form-item label="研究方向">
          <el-input v-model="filters.direction" placeholder="输入研究方向关键词" clearable @keyup.enter.native="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>导师列表</span>
          <el-button type="primary" link @click="loadData" :loading="loading">刷新</el-button>
        </div>
      </template>

      <el-table :data="filteredTeachers" v-loading="loading" style="width: 100%">
        <el-table-column prop="realName" label="导师姓名" width="120" />
        <el-table-column prop="title" label="职称" width="120" />
        <el-table-column prop="researchDirection" label="研究方向" min-width="200" show-overflow-tooltip />
        <el-table-column label="带生情况" width="180">
          <template #default="{ row }">
            <span>{{ row.currentStudentCount ?? 0 }} / {{ row.maxStudentCount ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="选题情况" width="220">
          <template #default="{ row }">
            <div>开放选题：{{ row.openTopicCount ?? 0 }}</div>
            <div v-if="row.tags && row.tags.length" class="tag-list">
              <el-tag
                v-for="tag in row.tags"
                :key="tag"
                size="small"
                type="info"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="历史评价概览" min-width="220">
          <template #default="{ row }">
            <div v-if="row.totalStudents && row.totalStudents > 0">
              <div>累计学生数：{{ row.totalStudents }}</div>
              <div v-if="row.avgScore != null">平均成绩：{{ row.avgScore.toFixed(2) }}</div>
              <div v-if="row.excellentRatio != null">
                优秀率：{{ (row.excellentRatio * 100).toFixed(1) }}%
              </div>
              <div v-if="row.failRatio != null">
                不及格率：{{ (row.failRatio * 100).toFixed(1) }}%
              </div>
            </div>
            <span v-else class="text-muted">暂缺评价数据</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openApplyDialog(row)">
              申请成为该导师学员
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !filteredTeachers.length" class="empty-tip">
        暂无符合条件的导师。
      </div>
    </el-card>

    <el-dialog v-model="applyDialog.visible" title="申请成为该导师学员" width="720px" class="mentor-dialog">
      <div class="mentor-dialog-body" v-if="applyDialog.teacher">
        <div class="mentor-summary">
          <div class="mentor-name-row">
            <span class="mentor-name">{{ applyDialog.teacher.realName }}</span>
            <el-tag v-if="applyDialog.teacher.title" size="small" effect="plain">
              {{ applyDialog.teacher.title }}
            </el-tag>
          </div>
          <div class="mentor-direction" v-if="applyDialog.teacher.researchDirection">
            研究方向：{{ applyDialog.teacher.researchDirection }}
          </div>
          <div class="mentor-meta-row">
            <span>当前带生：{{ applyDialog.teacher.currentStudentCount ?? 0 }} / {{ applyDialog.teacher.maxStudentCount ?? '-' }}</span>
          </div>
          <div class="mentor-meta-row" v-if="applyDialog.teacher.avgScore != null">
            <span>历史平均成绩：{{ applyDialog.teacher.avgScore.toFixed(2) }}</span>
          </div>
        </div>
        <div class="mentor-form">
          <el-form :model="applyForm" label-width="90px">
            <el-form-item label="个人说明">
              <el-input
                v-model="applyForm.reason"
                type="textarea"
                :rows="6"
                maxlength="400"
                show-word-limit
                placeholder="建议从个人背景、研究兴趣、已有成果或经历、希望获得的指导等方面进行简要说明。"
              />
            </el-form-item>
          </el-form>
          <div class="mentor-remark-hint">
            已输入 {{ reasonLength }} / 400 字
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="applyDialog.visible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="applyDialog.submitting"
            @click="submitApplication"
            :disabled="reasonLength === 0"
          >
            提交申请
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getTeacherOverviewList,
  createMentorApplication,
  type TeacherOverview
} from '@/api/mentorApplication'

interface Filters {
  keyword: string
  direction: string
}

const loading = ref(false)
const teachers = ref<TeacherOverview[]>([])

const filters = reactive<Filters>({
  keyword: '',
  direction: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTeacherOverviewList()
    teachers.value = res.data || []
  } catch (err: any) {
    ElMessage.error(err?.message || '加载导师列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.keyword = ''
  filters.direction = ''
}

const filteredTeachers = computed(() => {
  const kw = filters.keyword.trim().toLowerCase()
  const dir = filters.direction.trim().toLowerCase()
  return teachers.value.filter((t) => {
    const nameMatch =
      !kw ||
      (t.realName && t.realName.toLowerCase().includes(kw)) ||
      (t.researchDirection && t.researchDirection.toLowerCase().includes(kw)) ||
      (t.tags && t.tags.some((tag) => tag.toLowerCase().includes(kw)))

    const dirMatch =
      !dir ||
      (t.researchDirection && t.researchDirection.toLowerCase().includes(dir)) ||
      (t.tags && t.tags.some((tag) => tag.toLowerCase().includes(dir)))

    return nameMatch && dirMatch
  })
})

const applyDialog = reactive<{
  visible: boolean
  teacher: TeacherOverview | null
  submitting: boolean
}>({
  visible: false,
  teacher: null,
  submitting: false
})

const applyForm = reactive<{
  reason: string
}>({
  reason: ''
})

const reasonLength = computed(() => applyForm.reason.trim().length)

const openApplyDialog = (teacher: TeacherOverview) => {
  applyDialog.teacher = teacher
  applyForm.reason = ''
  applyDialog.visible = true
}

const submitApplication = async () => {
  if (!applyDialog.teacher) return
  applyDialog.submitting = true
  try {
    await createMentorApplication({
      teacherId: applyDialog.teacher.teacherId,
      reason: applyForm.reason?.trim() || undefined
    })
    ElMessage.success('已提交拜师申请，请等待导师审批')
    applyDialog.visible = false
  } catch (err: any) {
    ElMessage.error(err?.message || '提交申请失败')
  } finally {
    applyDialog.submitting = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.student-teachers-page {
  max-width: 1200px;
  margin: 0 auto;
}

.filter-card {
  margin-bottom: 16px;
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

.tag-list {
  margin-top: 4px;
}

.tag-item + .tag-item {
  margin-left: 4px;
}

.text-muted {
  color: #909399;
}

.mentor-dialog :deep(.el-dialog) {
  border-radius: 14px;
}

.mentor-dialog-body {
  display: flex;
  gap: 18px;
}

.mentor-summary {
  flex: 1.1;
  padding: 10px 12px;
  border-radius: 8px;
  background-color: #f5f7fa;
  font-size: 13px;
}

.mentor-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.mentor-name {
  font-weight: 600;
}

.mentor-direction {
  margin-bottom: 6px;
}

.mentor-meta-row {
  margin-bottom: 4px;
  color: #606266;
}

.mentor-form {
  flex: 1.6;
}

.mentor-remark-hint {
  text-align: right;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>

