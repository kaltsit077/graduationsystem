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
          <div class="status-label">数据库连接（活动/空闲/总数）</div>
          <div class="status-value">
            <template v-if="status.dbMetricsAvailable">
              {{ status.activeDbConnections }} / {{ status.idleDbConnections }} / {{ status.totalDbConnections }}
              <span class="status-sub">（最大 {{ status.maxDbConnections }}）</span>
            </template>
            <template v-else>
              N/A
            </template>
          </div>
        </el-card>
      </div>
      <div v-else class="status-empty">暂无状态数据</div>
    </el-card>

    <el-card style="margin-top: 20px" class="log-card">
      <template #header>
        <div class="card-header">
          <span>运行日志</span>
          <div class="header-actions">
            <el-select v-model="logLevel" size="small" style="width: 150px" @change="loadLogs">
              <el-option label="全部 (ALL)" value="ALL" />
              <el-option label="ERROR" value="ERROR" />
              <el-option label="WARN" value="WARN" />
              <el-option label="INFO" value="INFO" />
              <el-option label="DEBUG" value="DEBUG" />
            </el-select>
            <el-select v-model="logLines" size="small" style="width: 120px" @change="loadLogs">
              <el-option v-for="n in [100, 200, 500, 1000]" :key="n" :label="`最近 ${n} 行`" :value="n" />
            </el-select>
            <el-button size="small" @click="loadLogs" :loading="loadingLogs">刷新日志</el-button>
            <el-button size="small" type="warning" plain @click="handleClearLogs" :loading="clearing">
              手动清除日志
            </el-button>
            <el-select v-model="periodicClear" size="small" style="width: 140px" @change="onPeriodicClearChange">
              <el-option label="不自动清除" value="off" />
              <el-option label="每 1 小时清除" value="1" />
              <el-option label="每 6 小时清除" value="6" />
              <el-option label="每 24 小时清除" value="24" />
            </el-select>
          </div>
        </div>
      </template>
      <el-scrollbar class="log-scroll log-console">
        <pre class="log-text" v-if="logs.length"><span
          v-for="(line, i) in logs"
          :key="i"
          :class="lineLevelClass(line)"
        >{{ escapeHtml(line) }}</span></pre>
        <pre class="log-text log-empty" v-else>暂无日志或当前筛选无匹配条目。可尝试选择「全部 (ALL)」或点击「刷新日志」。</pre>
      </el-scrollbar>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMonitorStatus, getMonitorLogs, clearMonitorLogs, type MonitorStatus } from '@/api/admin'

const status = ref<MonitorStatus | null>(null)
const logs = ref<string[]>([])
const logLevel = ref<string>('ALL')
const logLines = ref<number>(200)
const loadingStatus = ref(false)
const loadingLogs = ref(false)
const clearing = ref(false)
const periodicClear = ref<string>('off')
let periodicTimer: ReturnType<typeof setInterval> | null = null

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

const handleClearLogs = async () => {
  try {
    await ElMessageBox.confirm('确定要清空当前日志文件内容吗？清空后仅保留之后新产生的日志。', '手动清除日志', {
      confirmButtonText: '确定清除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  clearing.value = true
  try {
    await clearMonitorLogs()
    ElMessage.success('日志已清除')
    await loadLogs()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '清除日志失败')
  } finally {
    clearing.value = false
  }
}

function lineLevelClass(line: string): string {
  if (!line) return 'log-line log-default'
  const u = line.toUpperCase()
  if (u.includes(' ERROR ') || u.includes('[ERROR]') || u.includes('.ERROR')) return 'log-line log-error'
  if (u.includes(' WARN ') || u.includes('[WARN]') || u.includes('.WARN')) return 'log-line log-warn'
  if (u.includes(' INFO ') || u.includes('[INFO]') || u.includes('.INFO')) return 'log-line log-info'
  if (u.includes(' DEBUG ') || u.includes('[DEBUG]') || u.includes('.DEBUG')) return 'log-line log-debug'
  return 'log-line log-default'
}

function escapeHtml(s: string): string {
  if (!s) return ''
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function onPeriodicClearChange() {
  if (periodicTimer) {
    clearInterval(periodicTimer)
    periodicTimer = null
  }
  const hours = periodicClear.value === 'off' ? 0 : parseInt(periodicClear.value, 10)
  if (hours <= 0) return
  const ms = hours * 60 * 60 * 1000
  periodicTimer = setInterval(async () => {
    try {
      await clearMonitorLogs()
      ElMessage.info(`已按设定每 ${hours} 小时自动清除日志`)
      await loadLogs()
    } catch {
      // 静默失败，避免刷屏
    }
  }, ms)
}

onMounted(() => {
  loadStatus()
  loadLogs()
})

onUnmounted(() => {
  if (periodicTimer) {
    clearInterval(periodicTimer)
    periodicTimer = null
  }
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
  flex-wrap: wrap;
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

.status-sub {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.status-empty {
  text-align: center;
  color: #999;
  padding: 20px;
}

/* CMD/终端风格：深色背景 + 彩色级别 */
.log-card :deep(.el-card__body) {
  padding: 0;
}

.log-scroll {
  height: 420px;
}

.log-console {
  background: #0c0c0c;
  border-radius: 4px;
}

.log-text {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  padding: 12px 16px;
  color: #d4d4d4;
}

.log-text .log-line {
  display: block;
}

.log-error {
  color: #f48771;
}

.log-warn {
  color: #dcdcaa;
}

.log-info {
  color: #4ec9b0;
}

.log-debug {
  color: #808080;
}

.log-default {
  color: #d4d4d4;
}

.log-empty {
  color: #6a737d;
}
</style>
