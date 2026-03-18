<template>
  <div class="eval-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题质量总览</span>
          <div class="header-actions">
            <el-segmented
              v-model="ratioChartMode"
              size="small"
              :options="[
                { label: '折线', value: 'line' },
                { label: '双柱', value: 'bar' }
              ]"
            />
            <el-button size="small" @click="loadMetrics" :loading="loading">刷新</el-button>
          </div>
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
        <div class="charts-row charts-row-second">
          <div class="chart-box">
            <div class="chart-title">规模与成绩（气泡：人数，颜色：不及格率）</div>
            <div ref="bubbleChartRef" class="chart chart-tall"></div>
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
          <el-table-column label="学生满意度" width="120">
            <template #default="{ row }">
              <span v-if="row.avgStudentScore != null">{{ row.avgStudentScore.toFixed(1) }}</span>
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
const ratioChartMode = ref<'line' | 'bar'>('line')

const scoreChartRef = ref<HTMLDivElement | null>(null)
let scoreChart: echarts.ECharts | null = null

const ratioChartRef = ref<HTMLDivElement | null>(null)
let ratioChart: echarts.ECharts | null = null

const bubbleChartRef = ref<HTMLDivElement | null>(null)
let bubbleChart: echarts.ECharts | null = null

const initCharts = () => {
  if (scoreChartRef.value && !scoreChart) {
    scoreChart = echarts.init(scoreChartRef.value)
  }
  if (ratioChartRef.value && !ratioChart) {
    ratioChart = echarts.init(ratioChartRef.value)
  }
  if (bubbleChartRef.value && !bubbleChart) {
    bubbleChart = echarts.init(bubbleChartRef.value)
  }
  updateCharts()
}

const updateCharts = () => {
  if (!metrics.value.length) return
  const names = metrics.value.map((m) => m.topicTitle)
  const avgScores = metrics.value.map((m) => m.avgScore ?? 0)
  const excellent = metrics.value.map((m) => (m.excellentRatio ?? 0) * 100)
  const fail = metrics.value.map((m) => (m.failRatio ?? 0) * 100)
  const totals = metrics.value.map((m) => m.totalStudents ?? 0)

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
    const common = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['优秀率', '不及格率'] },
      xAxis: { type: 'category' as const, data: names, axisLabel: { rotate: 30 } },
      yAxis: { type: 'value' as const, min: 0, max: 100, axisLabel: { formatter: '{value}%' } }
    }
    ratioChart.setOption({
      ...common,
      series:
        ratioChartMode.value === 'bar'
          ? [
              {
                name: '优秀率',
                type: 'bar',
                data: excellent,
                itemStyle: { color: '#67C23A' }
              },
              {
                name: '不及格率',
                type: 'bar',
                data: fail,
                itemStyle: { color: '#F56C6C' }
              }
            ]
          : [
              {
                name: '优秀率',
                type: 'line',
                data: excellent,
                itemStyle: { color: '#67C23A' },
                smooth: true
              },
              {
                name: '不及格率',
                type: 'line',
                data: fail,
                itemStyle: { color: '#F56C6C' },
                smooth: true
              }
            ]
    })
  }

  if (bubbleChart) {
    const data = metrics.value.map((m, idx) => {
      const x = Number(m.totalStudents ?? 0)
      const y = Number(m.avgScore ?? 0)
      const failRatio = Number(m.failRatio ?? 0) // 0-1
      const size = Math.max(10, Math.min(60, 10 + x * 6))
      return {
        value: [x, y, failRatio, idx],
        symbolSize: size,
        name: m.topicTitle
      }
    })
    bubbleChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (p: any) => {
          const v = p?.data?.value || []
          const idx = v[3]
          const m = metrics.value[idx]
          const failP = ((m?.failRatio ?? 0) * 100).toFixed(0)
          const excP = ((m?.excellentRatio ?? 0) * 100).toFixed(0)
          const avg = m?.avgScore != null ? Number(m.avgScore).toFixed(1) : '—'
          const sat = m?.avgStudentScore != null ? Number(m.avgStudentScore).toFixed(1) : '—'
          return [
            `<div style="font-weight:600;margin-bottom:4px;">${m?.topicTitle || ''}</div>`,
            `学生人数：${m?.totalStudents ?? 0}`,
            `平均分：${avg}`,
            `满意度：${sat}`,
            `优秀率：${excP}%`,
            `不及格率：${failP}%`
          ].join('<br/>')
        }
      },
      grid: { left: 46, right: 24, top: 10, bottom: 46 },
      xAxis: {
        type: 'value',
        name: '学生人数',
        minInterval: 1
      },
      yAxis: {
        type: 'value',
        name: '平均分',
        min: 0,
        max: 100
      },
      visualMap: {
        type: 'continuous',
        min: 0,
        max: 1,
        dimension: 2,
        right: 8,
        top: 10,
        text: ['不及格率高', '不及格率低'],
        inRange: {
          color: ['#67C23A', '#E6A23C', '#F56C6C']
        },
        calculable: true
      },
      series: [
        {
          type: 'scatter',
          data,
          emphasis: { focus: 'series' }
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
  bubbleChart?.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  scoreChart?.dispose()
  ratioChart?.dispose()
  bubbleChart?.dispose()
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.charts-row {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.charts-row-second {
  margin-top: 8px;
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

.chart-tall {
  height: 300px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 24px 0;
}
</style>

