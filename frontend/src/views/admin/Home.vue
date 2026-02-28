<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <span>欢迎，{{ authStore.realName }} 管理员</span>
        </div>
      </template>
      <el-alert
        title="系统提示"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>您可以审核选题、管理用户账号等</p>
        </template>
      </el-alert>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card class="stat-card">
          <el-statistic title="待审核选题" :value="pendingReviewCount">
            <template #prefix>
              <el-icon style="vertical-align: -0.125em">
                <Document />
              </el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>快速操作</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="primary" @click="$router.push('/admin/reviews')" style="width: 100%">
            <el-icon><Document /></el-icon>
            选题审核
          </el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Document } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getAdminStats } from '@/api/stats'

const authStore = useAuthStore()

const pendingReviewCount = ref(0)

onMounted(() => {
  loadStats()
})

const loadStats = async () => {
  try {
    const res = await getAdminStats()
    if (res.data) {
      pendingReviewCount.value = res.data.pendingReviewTopicCount || 0
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-card {
  text-align: center;
}
</style>

