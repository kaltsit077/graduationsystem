<template>
  <div class="student-teachers-page page-container page-scope">
    <el-card class="filter-card">
      <div class="filter-header">
        <div class="filter-title">
          <div class="page-title">导师列表</div>
        </div>
        <el-button type="primary" plain @click="loadData" :loading="loading">刷新</el-button>
      </div>
      <el-form class="filter-form" :model="filters" label-position="top" @submit.prevent>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="关键词">
              <el-input
                v-model="filters.keyword"
                placeholder="姓名 / 方向 / 标签"
                clearable
                @keyup.enter="loadData"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="研究方向">
              <el-input
                v-model="filters.direction"
                placeholder="输入方向关键词"
                clearable
                @keyup.enter="loadData"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8" class="filter-actions-col">
            <el-form-item label=" ">
              <div class="filter-actions">
                <el-button type="primary" @click="loadData">搜索</el-button>
                <el-button @click="resetFilters">重置</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="page-card" shadow="never">
      <el-table
        :data="filteredTeachers"
        v-loading="loading"
        style="width: 100%"
        stripe
        size="large"
        table-layout="fixed"
      >
        <el-table-column label="导师" min-width="220">
          <template #default="{ row }">
            <div class="teacher-cell">
              <div class="teacher-name-row">
                <span class="teacher-name">{{ row.realName }}</span>
                <el-tag v-if="row.title" size="small" effect="plain">{{ row.title }}</el-tag>
                <el-tag v-if="row.matchScore != null" size="small" :type="getMatchTagType(row.matchScore || 0)">
                  {{ ((row.matchScore || 0) * 100).toFixed(0) }}%
                </el-tag>
                <span v-if="isStrongRecommend(row)" class="strong-recommend" title="匹配度较高且与研究方向/标签有关联，推荐">
                  👍
                </span>
              </div>
              <div v-if="row.researchDirection" class="teacher-sub">
                方向：{{ row.researchDirection }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="带生 / 容量" width="140">
          <template #default="{ row }">
            <span>{{ row.currentStudentCount ?? 0 }} / {{ row.maxStudentCount ?? '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="开放选题" width="110">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.openTopicCount ?? 0 }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="标签" min-width="220">
          <template #default="{ row }">
            <div v-if="row.tags && row.tags.length" class="tag-list">
              <el-tag
                v-for="tag in row.tags.slice(0, 4)"
                :key="tag"
                size="small"
                type="info"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
              <el-tag v-if="row.tags.length > 4" size="small" effect="plain" class="tag-more">
                +{{ row.tags.length - 4 }}
              </el-tag>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button type="primary" link @click="openDetail(row)">详情</el-button>
              <el-button type="primary" size="small" @click="openApplyDialog(row)">申请</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !filteredTeachers.length" class="empty-tip">
        暂无符合条件的导师。
      </div>
    </el-card>

    <el-drawer v-model="detailDrawerVisible" size="520px" :with-header="false">
      <div class="drawer-header">
        <div class="drawer-title">
          <div class="drawer-name">{{ selectedTeacher?.realName || '导师详情' }}</div>
          <div class="drawer-sub" v-if="selectedTeacher?.researchDirection">
            研究方向：{{ selectedTeacher?.researchDirection }}
          </div>
        </div>
        <el-button text @click="detailDrawerVisible = false">关闭</el-button>
      </div>

      <el-card shadow="never" class="drawer-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="职称">
            {{ selectedTeacher?.title || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="匹配度">
            <span v-if="selectedTeacher?.matchScore != null">
              {{ ((selectedTeacher?.matchScore || 0) * 100).toFixed(0) }}%
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="带生情况">
            {{ selectedTeacher?.currentStudentCount ?? 0 }} / {{ selectedTeacher?.maxStudentCount ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="开放选题">
            {{ selectedTeacher?.openTopicCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="历史评价概览">
            <div v-if="selectedTeacher?.totalStudents && selectedTeacher.totalStudents > 0">
              <div>累计学生数：{{ selectedTeacher.totalStudents }}</div>
              <div v-if="selectedTeacher.avgScore != null">平均成绩：{{ selectedTeacher.avgScore.toFixed(2) }}</div>
              <div v-if="selectedTeacher.excellentRatio != null">
                优秀率：{{ (selectedTeacher.excellentRatio * 100).toFixed(1) }}%
              </div>
              <div v-if="selectedTeacher.failRatio != null">
                不及格率：{{ (selectedTeacher.failRatio * 100).toFixed(1) }}%
              </div>
            </div>
            <span v-else class="text-muted">暂缺评价数据</span>
          </el-descriptions-item>
          <el-descriptions-item label="研究标签">
            <div v-if="selectedTeacher?.tags && selectedTeacher.tags.length" class="tag-list drawer-tags">
              <el-tag
                v-for="tag in selectedTeacher.tags"
                :key="tag"
                size="small"
                type="info"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
            </div>
            <span v-else class="text-muted">-</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="drawer-actions">
          <el-button @click="detailDrawerVisible = false">返回</el-button>
          <el-button type="primary" @click="selectedTeacher && openApplyDialog(selectedTeacher)">去申请</el-button>
        </div>
      </el-card>
    </el-drawer>

    <AppDialog
      v-model="applyDialog.visible"
      title="申请成为该导师学员"
      width="860px"
      :close-on-click-modal="false"
      body-max-height="70vh"
    >
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
    </AppDialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppDialog from '@/components/AppDialog.vue'
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
  const list = teachers.value.filter((t) => {
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

  // 若后端返回了匹配度，则按匹配度从高到低排序，让更契合当前学生的导师排在前面
  return [...list].sort((a, b) => {
    const ma = a.matchScore ?? -1
    const mb = b.matchScore ?? -1
    return mb - ma
  })
})

const getMatchTagType = (score: number) => {
  // 匹配度越高越好：越高越绿、越低越红
  if (score >= 0.8) return 'success'
  if (score >= 0.6) return 'warning'
  if (score >= 0.4) return 'danger'
  return 'info'
}

const isStrongRecommend = (teacher: TeacherOverview) => {
  const score = teacher.matchScore ?? 0
  const hasAssociation = Boolean(teacher.tags && teacher.tags.length)
  // “高度吻合”且“有关联”（这里用命中标签作为关联证据）
  return score >= 0.75 && hasAssociation
}

const detailDrawerVisible = ref(false)
const selectedTeacher = ref<TeacherOverview | null>(null)

const openDetail = (teacher: TeacherOverview) => {
  selectedTeacher.value = teacher
  detailDrawerVisible.value = true
}

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
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.filter-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.filter-actions-col :deep(.el-form-item__content) {
  display: flex;
  justify-content: flex-end;
}

.filter-actions {
  display: inline-flex;
  gap: 8px;
}

.page-card {
  border-radius: 12px;
}

.empty-tip {
  text-align: center;
  padding: 18px 0;
  color: #909399;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item {
  margin: 0;
}

.tag-more {
  color: #606266;
}

.strong-recommend {
  font-size: 18px;
  line-height: 1;
}

.teacher-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.teacher-name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.teacher-name {
  font-weight: 700;
  color: #1f2329;
}

.teacher-sub {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 6px 4px 12px;
}

.drawer-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.drawer-name {
  font-size: 18px;
  font-weight: 700;
  color: #1f2329;
  line-height: 1.2;
}

.drawer-sub {
  font-size: 12px;
  color: #606266;
  line-height: 1.2;
}

.drawer-card {
  border-radius: 12px;
}

.drawer-tags {
  padding-top: 4px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
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

@media (max-width: 900px) {
  .mentor-dialog-body {
    flex-direction: column;
  }

  .mentor-summary {
    padding: 12px 14px;
  }
}
</style>

