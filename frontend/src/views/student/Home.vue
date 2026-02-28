<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <span>欢迎，{{ authStore.realName }}</span>
        </div>
      </template>
      <el-alert
        title="系统提示"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>选题开放状态：{{ topicOpenStatus }}</p>
          <p v-if="!topicOpen">选题尚未开放，请先完善个人信息</p>
        </template>
      </el-alert>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card class="stat-card">
          <el-statistic title="我的申请" :value="applicationCount">
            <template #prefix>
              <el-icon style="vertical-align: -0.125em">
                <Document />
              </el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <el-statistic title="未读通知" :value="unreadCount">
            <template #prefix>
              <el-icon style="vertical-align: -0.125em">
                <Bell />
              </el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <el-statistic title="已通过选题" :value="approvedCount">
            <template #prefix>
              <el-icon style="vertical-align: -0.125em">
                <SuccessFilled />
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
          <el-button type="primary" @click="$router.push('/student/topics')" style="width: 100%">
            <el-icon><Document /></el-icon>
            查看选题
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="success" @click="$router.push('/student/applications')" style="width: 100%">
            <el-icon><List /></el-icon>
            我的申请
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="info" @click="$router.push('/student/profile')" style="width: 100%">
            <el-icon><User /></el-icon>
            个人中心
          </el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Document, Bell, SuccessFilled, User, List } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getStudentStats } from '@/api/stats'

const authStore = useAuthStore()

const topicOpen = ref(true)
const topicOpenStatus = ref('已开放')
const applicationCount = ref(0)
const unreadCount = ref(0)
const approvedCount = ref(0)

onMounted(() => {
  loadStats()
})

const loadStats = async () => {
  try {
    const res = await getStudentStats()
    if (res.data) {
      applicationCount.value = res.data.applicationCount || 0
      unreadCount.value = res.data.unreadNotificationCount || 0
      approvedCount.value = res.data.approvedApplicationCount || 0
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

