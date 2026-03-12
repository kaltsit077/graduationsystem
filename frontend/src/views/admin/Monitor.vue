<template>
  <div class="monitor-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>运行状态</span>
          <el-button size="small" @click="loadStatus" :loading="loadingStatus">刷新状态</el-button>
        </div>
      </template>
      <div v-if="status" class="status-grid">
        <el-card shadow="never" class="status-item">
          <div class="status-label">运行时间</div>
          <div class="status-value">{{ status.uptime }}</div>
        </el-card>
        <el-card shadow="never" class="status-item">
          <div class="status-label">内存使用</div>
          <div class="status-value">
            {{ status.heapUsedMb.toFixed(1) }} MB / {{ status.heapMaxMb.toFixed(1) }} MB
          </div>
        </el-card>
        <el-card shadow="never" class="status-item">
          <div class="status-label">线程数</div>
          <div class="status-value">{{ status.threadCount }}</div>
        </el-card>
        <el-card shadow="never" class="status-item">
          <div class="status-label">系统负载</div>
          <div class="status-value">
            {{ status.systemLoadAverage && status.systemLoadAverage > 0 ? status.systemLoadAverage.toFixed(2) : 'N/A' }}
          </div>
        </el-card>
        <el-card shadow="never" class="status-item">
          <div class="status-label">数据库连接（活动/空闲）</div>
          <div class="status-value">
            {{ status.activeDbConnections }} / {{ status.idleDbConnections }}
          </div>
        </el-card>
      </div>
      <div v-else class="status-empty">暂无状态数据</div>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>错误日志</span>
          <div class="header-actions">
            <el-select v-model="logLevel" size="small" style="width: 130px">
              <el-option label="ERROR" value="ERROR" />
              <el-option label="WARN" value="WARN" />
              <el-option label="INFO" value="INFO" />
              <el-option label="ALL" value="ALL" />
            </el-select>
            <el-select v-model="logLines" size="small" style="width: 120px">
              <el-option v-for="n in [100, 200, 500, 1000]" :key="n" :label="`最近 ${n} 行`" :value="n" />
            </el-select>
            <el-button size="small" type="primary" @click="loadLogs" :loading="loadingLogs">刷新日志</el-button>
          </div>
        </div>
      </template>
      <el-scrollbar class="log-scroll">
        <pre class="log-text">
{{ logs.length ? logs.join('\n') : '暂无日志或日志文件未创建。' }}
        </pre>
      </el-scrollbar>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMonitorStatus, getMonitorLogs, type MonitorStatus } from '@/api/admin'

const status = ref<MonitorStatus | null>(null)
const logs = ref<string[]>([])
const logLevel = ref<'ERROR' | 'WARN' | 'INFO' | 'ALL'>('ERROR')
const logLines = ref<number>(200)
const loadingStatus = ref(false)
const loadingLogs = ref(false)

const loadStatus = async () => {
  loadingStatus.value = true
  try {
    const res = await getMonitorStatus()
    status.value = res.data || null
  } catch {
    ElMessage.error('加载运行状态失败')
  } finally {
    loadingStatus.value = false
  }
}

const loadLogs = async () => {
  loadingLogs.value = true
  try {
    const res = await getMonitorLogs({ level: logLevel.value, lines: logLines.value })
    logs.value = res.data || []
  } catch {
    ElMessage.error('加载日志失败')
  } finally {
    loadingLogs.value = false
  }
}

onMounted(() => {
  loadStatus()
  loadLogs()
})
</script>

<style scoped>
.monitor-container {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.status-item {
  text-align: left;
}

.status-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.status-value {
  font-size: 18px;
  font-weight: 600;
}

.status-empty {
  text-align: center;
  color: #999;
  padding: 20px;
}

.log-scroll {
  height: 400px;
}

.log-text {
  font-family: Menlo, Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>

