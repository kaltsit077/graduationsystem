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
      <el-col :span="16">
        <el-card class="selection-card">
          <template #header>
            <div class="card-header">
              <span>选题系统开放设置</span>
            </div>
          </template>
          <div class="selection-row">
            <span class="selection-label">当前状态：</span>
            <el-tag :type="selectionStatusTagType">
              {{ selectionStatusText }}
            </el-tag>
          </div>
          <div class="selection-row">
            <span class="selection-label">全局开关：</span>
            <el-switch
              v-model="selectionEnabled"
              active-text="允许学生选题"
              inactive-text="关闭选题系统"
            />
          </div>
          <div class="selection-row">
            <span class="selection-label">开放时间：</span>
            <el-date-picker
              v-model="selectionRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :disabled="!selectionEnabled"
            />
          </div>
          <div class="selection-footer">
            <el-button type="primary" size="small" :loading="savingSelection" @click="saveSelectionSetting">
              保存设置
            </el-button>
            <span class="selection-tip">
              不设置时间则表示长期开放；关闭开关则无论时间如何都禁止学生选题。
            </span>
          </div>
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
import { computed, ref, onMounted } from 'vue'
import { Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getAdminStats } from '@/api/stats'
import { getSelectionSetting, updateSelectionSetting, type SelectionSetting } from '@/api/admin'

const authStore = useAuthStore()

const pendingReviewCount = ref(0)
const selectionEnabled = ref(true)
const selectionRange = ref<[string, string] | null>(null)
const selectionOpenNow = ref<boolean | null>(null)
const savingSelection = ref(false)

onMounted(() => {
  loadStats()
  loadSelectionSetting()
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

const loadSelectionSetting = async () => {
  try {
    const res = await getSelectionSetting()
    if (res.data) {
      selectionEnabled.value = res.data.enabled
      if (res.data.startTime && res.data.endTime) {
        selectionRange.value = [res.data.startTime, res.data.endTime]
      } else {
        selectionRange.value = null
      }
      selectionOpenNow.value = res.data.openNow
    }
  } catch (error) {
    console.error('加载选题开放设置失败', error)
  }
}

const selectionStatusText = computed(() => {
  if (!selectionEnabled.value) return '选题系统已关闭'
  if (selectionOpenNow.value === true) return '当前处于开放时间内'
  if (selectionRange.value && selectionRange.value.length === 2) return '当前不在设置的开放时间内'
  return '长期开放（未设置时间窗口）'
})

const selectionStatusTagType = computed<'success' | 'info' | 'warning' | 'danger'>(() => {
  if (!selectionEnabled.value) return 'danger'
  if (selectionOpenNow.value === true) return 'success'
  if (selectionRange.value && selectionRange.value.length === 2) return 'warning'
  return 'info'
})

const saveSelectionSetting = async () => {
  savingSelection.value = true
  try {
    const payload: {
      enabled: boolean
      startTime?: string | null
      endTime?: string | null
    } = {
      enabled: selectionEnabled.value
    }
    if (selectionEnabled.value && selectionRange.value && selectionRange.value.length === 2) {
      payload.startTime = selectionRange.value[0]
      payload.endTime = selectionRange.value[1]
    } else {
      payload.startTime = null
      payload.endTime = null
    }
    const res = await updateSelectionSetting(payload)
    selectionOpenNow.value = res.data.openNow
    ElMessage.success('选题开放设置已保存')
  } catch (error) {
    console.error('保存选题开放设置失败', error)
    ElMessage.error('保存选题开放设置失败')
  } finally {
    savingSelection.value = false
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

.selection-card {
  min-height: 100%;
}

.selection-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.selection-label {
  width: 90px;
  text-align: right;
  color: #666;
}

.selection-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.selection-tip {
  font-size: 12px;
  color: #999;
}
</style>

