<template>
  <div class="eval-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题质量总览</span>
          <el-button size="small" @click="loadMetrics" :loading="loading">刷新</el-button>
        </div>
      </template>
      <div v-if="metrics.length">
        <div class="charts-row">
          <div class="chart-box">
            <div class="chart-title">各选题平均成绩</div>
            <div ref="scoreChartRef" class="chart"></div>
          </div>
          <div class="chart-box">
            <div class="chart-title">优秀率 / 不及格率对比</div>
            <div ref="ratioChartRef" class="chart"></div>
          </div>
        </div>
        <el-table :data="metrics" style="width: 100%; margin-top: 16px" size="small">
          <el-table-column prop="topicTitle" label="选题标题" min-width="220" />
          <el-table-column prop="totalStudents" label="学生人数" width="100" />
          <el-table-column label="平均成绩" width="120">
            <template #default="{ row }">
              <span v-if="row.avgScore != null">{{ row.avgScore.toFixed(1) }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="优秀率" width="120">
            <template #default="{ row }">
              <el-tag type="success" v-if="row.excellentRatio != null">
                {{ (row.excellentRatio * 100).toFixed(0) }}%
              </el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="不及格率" width="120">
            <template #default="{ row }">
              <el-tag type="danger" v-if="row.failRatio != null">
                {{ (row.failRatio * 100).toFixed(0) }}%
              </el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else class="empty-tip">
        暂无评价数据，请在学生完成论文后录入成绩。
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getTeacherTopicMetrics, type TopicMetrics } from '@/api/evaluation'

const metrics = ref<TopicMetrics[]>([])
const loading = ref(false)

const scoreChartRef = ref<HTMLDivElement | null>(null)
let scoreChart: echarts.ECharts | null = null

const ratioChartRef = ref<HTMLDivElement | null>(null)
let ratioChart: echarts.ECharts | null = null

const initCharts = () => {
  if (scoreChartRef.value && !scoreChart) {
    scoreChart = echarts.init(scoreChartRef.value)
  }
  if (ratioChartRef.value && !ratioChart) {
    ratioChart = echarts.init(ratioChartRef.value)
  }
  updateCharts()
}

const updateCharts = () => {
  if (!metrics.value.length) return
  const names = metrics.value.map((m) => m.topicTitle)
  const avgScores = metrics.value.map((m) => m.avgScore ?? 0)
  const excellent = metrics.value.map((m) => (m.excellentRatio ?? 0) * 100)
  const fail = metrics.value.map((m) => (m.failRatio ?? 0) * 100)

  if (scoreChart) {
    scoreChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: names, axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', min: 0, max: 100 },
      series: [
        {
          name: '平均成绩',
          type: 'bar',
          data: avgScores,
          itemStyle: { color: '#409EFF' }
        }
      ]
    })
  }

  if (ratioChart) {
    ratioChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['优秀率', '不及格率'] },
      xAxis: { type: 'category', data: names, axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
      series: [
        {
          name: '优秀率',
          type: 'line',
          data: excellent,
          itemStyle: { color: '#67C23A' }
        },
        {
          name: '不及格率',
          type: 'line',
          data: fail,
          itemStyle: { color: '#F56C6C' }
        }
      ]
    })
  }
}

const loadMetrics = async () => {
  loading.value = true
  try {
    const res = await getTeacherTopicMetrics()
    metrics.value = res.data || []
    initCharts()
  } catch {
    ElMessage.error('加载选题质量数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMetrics()
  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
  scoreChart?.resize()
  ratioChart?.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  scoreChart?.dispose()
  ratioChart?.dispose()
})
</script>

<style scoped>
.eval-container {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.charts-row {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.chart-box {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chart-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.chart {
  width: 100%;
  height: 260px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 24px 0;
}
</style>

