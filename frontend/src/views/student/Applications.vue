<template>
  <div class="applications-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的申请记录</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="apps-tabs">
        <el-tab-pane label="选题申请" name="topic">
          <el-table :data="topicApplications" style="width: 100%" v-loading="loading">
            <el-table-column prop="topicTitle" label="选题标题" min-width="200" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'PENDING'" type="warning">待审核</el-tag>
                <el-tag v-else-if="row.status === 'APPROVED'" type="success">已通过</el-tag>
                <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已拒绝</el-tag>
                <el-tag v-else type="info">{{ row.status || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="申请备注" min-width="200" show-overflow-tooltip />
            <el-table-column prop="teacherFeedback" label="导师反馈" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="申请时间" width="180" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="viewTopicDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="导师申请" name="mentor">
          <el-table :data="mentorApplications" style="width: 100%" v-loading="loading">
            <el-table-column prop="teacherName" label="导师" width="160">
              <template #default="{ row }">
                <span>{{ row.teacherName || row.teacherId || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'PENDING'" type="warning">待处理</el-tag>
                <el-tag v-else-if="row.status === 'APPROVED'" type="success">已同意</el-tag>
                <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已拒绝</el-tag>
                <el-tag v-else type="info">已取消</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="个人说明" min-width="220" show-overflow-tooltip />
            <el-table-column prop="teacherComment" label="导师意见" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="申请时间" width="180" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="viewMentorDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyApplications, type Application } from '@/api/application'
import { getMyMentorApplications, type MentorApplication } from '@/api/mentorApplication'

const loading = ref(false)
const activeTab = ref<'topic' | 'mentor'>('topic')
const topicApplications = ref<Application[]>([])
const mentorApplications = ref<MentorApplication[]>([])

onMounted(() => {
  loadApplications()
})

const loadApplications = async () => {
  loading.value = true
  try {
    const [topicRes, mentorRes] = await Promise.all([getMyApplications(), getMyMentorApplications()])
    topicApplications.value = topicRes.data || []
    mentorApplications.value = mentorRes.data || []
  } catch (error) {
    ElMessage.error('加载申请记录失败')
  } finally {
    loading.value = false
  }
}

const viewTopicDetail = (application: Application) => {
  ElMessage.info(
    `类型：选题申请\n选题：${application.topicTitle}\n状态：${application.status}\n备注：${application.remark || '无'}\n导师反馈：${
      application.teacherFeedback || '无'
    }`
  )
}

const viewMentorDetail = (application: MentorApplication) => {
  ElMessage.info(
    `类型：导师申请\n导师：${(application as any).teacherName || application.teacherId}\n状态：${
      application.status
    }\n个人说明：${application.reason || '无'}\n导师意见：${(application as any).teacherComment || '无'}`
  )
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

