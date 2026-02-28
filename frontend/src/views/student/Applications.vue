<template>
  <div class="applications-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的申请记录</span>
        </div>
      </template>

      <el-table :data="applications" style="width: 100%" v-loading="loading">
        <el-table-column prop="topicTitle" label="选题标题" min-width="200" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.status === 'APPROVED'" type="success">已通过</el-tag>
            <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="申请备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="teacherFeedback" label="导师反馈" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="申请时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyApplications, type Application } from '@/api/application'

const loading = ref(false)
const applications = ref<Application[]>([])

onMounted(() => {
  loadApplications()
})

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getMyApplications()
    applications.value = res.data || []
  } catch (error) {
    ElMessage.error('加载申请记录失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (application: Application) => {
  ElMessage.info(`选题：${application.topicTitle}\n状态：${application.status}\n备注：${application.remark || '无'}`)
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
</style>

