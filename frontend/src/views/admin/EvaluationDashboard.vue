<template>
  <div class="eval-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>全校选题质量分析</span>
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
            <div class="chart-title">平均成绩分布（Top10）</div>
            <div ref="scoreChartRef" class="chart"></div>
          </div>
          <div class="chart-box">
            <div class="chart-title">优秀率 / 不及格率分布（Top10）</div>
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

        <el-divider style="margin: 18px 0">导师维度质量概览</el-divider>
        <div v-if="teacherMetrics.length">
          <div class="charts-row">
            <div class="chart-box">
              <div class="chart-title">导师均分 / 学生满意度（Top10）</div>
              <div ref="teacherChartRef" class="chart"></div>
            </div>
            <div class="chart-box">
              <div class="chart-title">优秀率 / 不及格率（Top10）</div>
              <div ref="teacherRatioChartRef" class="chart"></div>
            </div>
          </div>
          <el-table :data="teacherMetrics" style="width: 100%; margin-top: 16px" size="small">
            <el-table-column prop="teacherName" label="导师" width="140" />
            <el-table-column prop="totalStudents" label="学生人数" width="100" />
            <el-table-column label="平均成绩" width="120">
              <template #default="{ row }">
                <span v-if="row.avgScore != null">{{ row.avgScore.toFixed(1) }}</span>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="满意度" width="120">
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
            <el-table-column label="风险提示" width="140">
              <template #default="{ row }">
                <el-tag v-if="(row.failRatio ?? 0) >= 0.2" type="danger">高风险</el-tag>
                <el-tag v-else-if="(row.excellentRatio ?? 0) >= 0.4" type="success">稳健</el-tag>
                <el-tag v-else type="info">一般</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="empty-tip">
          暂无导师维度评价数据。
        </div>
      </div>
      <div v-else class="empty-tip">
        暂无评价数据，请教师录入论文成绩后再查看。
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getAdminTopicMetrics, getAdminTeacherMetrics, type TopicMetrics, type TeacherMetrics } from '@/api/evaluation'

const metrics = ref<TopicMetrics[]>([])
const loading = ref(false)
const ratioChartMode = ref<'line' | 'bar'>('line')
const teacherMetrics = ref<TeacherMetrics[]>([])

const scoreChartRef = ref<HTMLDivElement | null>(null)
let scoreChart: echarts.ECharts | null = null

const ratioChartRef = ref<HTMLDivElement | null>(null)
let ratioChart: echarts.ECharts | null = null

const bubbleChartRef = ref<HTMLDivElement | null>(null)
let bubbleChart: echarts.ECharts | null = null

const teacherChartRef = ref<HTMLDivElement | null>(null)
let teacherChart: echarts.ECharts | null = null

const teacherRatioChartRef = ref<HTMLDivElement | null>(null)
let teacherRatioChart: echarts.ECharts | null = null

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
  if (teacherChartRef.value && !teacherChart) {
    teacherChart = echarts.init(teacherChartRef.value)
  }
  if (teacherRatioChartRef.value && !teacherRatioChart) {
    teacherRatioChart = echarts.init(teacherRatioChartRef.value)
  }
  updateCharts()
}

const updateCharts = () => {
  if (!metrics.value.length) return
  const sorted = [...metrics.value].sort((a, b) => (b.avgScore ?? 0) - (a.avgScore ?? 0)).slice(0, 10)
  const names = sorted.map((m) => m.topicTitle)
  const avgScores = sorted.map((m) => m.avgScore ?? 0)
  const excellent = sorted.map((m) => (m.excellentRatio ?? 0) * 100)
  const fail = sorted.map((m) => (m.failRatio ?? 0) * 100)

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
    const data = sorted.map((m, idx) => {
      const x = Number(m.totalStudents ?? 0)
      const y = Number(m.avgScore ?? 0)
      const failRatio = Number(m.failRatio ?? 0)
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
          const m = sorted[idx]
          const failP = ((m?.failRatio ?? 0) * 100).toFixed(0)
          const excP = ((m?.excellentRatio ?? 0) * 100).toFixed(0)
          const avg = m?.avgScore != null ? Number(m.avgScore).toFixed(1) : '—'
          return [
            `<div style="font-weight:600;margin-bottom:4px;">${m?.topicTitle || ''}</div>`,
            `学生人数：${m?.totalStudents ?? 0}`,
            `平均分：${avg}`,
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

  // 导师维度图表
  if (teacherChart || teacherRatioChart) {
    const tSorted = [...teacherMetrics.value]
      .sort((a, b) => (b.avgScore ?? 0) - (a.avgScore ?? 0))
      .slice(0, 10)
    const tNames = tSorted.map((t) => t.teacherName)
    const tAvg = tSorted.map((t) => t.avgScore ?? 0)
    const tSat = tSorted.map((t) => t.avgStudentScore ?? 0)
    const tExc = tSorted.map((t) => (t.excellentRatio ?? 0) * 100)
    const tFail = tSorted.map((t) => (t.failRatio ?? 0) * 100)

    teacherChart?.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['平均成绩', '满意度'] },
      xAxis: { type: 'category', data: tNames, axisLabel: { rotate: 30 } },
      yAxis: [
        { type: 'value', min: 0, max: 100 },
        { type: 'value', min: 0, max: 100 }
      ],
      series: [
        { name: '平均成绩', type: 'bar', data: tAvg, itemStyle: { color: '#409EFF' } },
        { name: '满意度', type: 'line', yAxisIndex: 1, data: tSat, smooth: true, itemStyle: { color: '#E6A23C' } }
      ]
    })

    teacherRatioChart?.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['优秀率', '不及格率'] },
      xAxis: { type: 'category', data: tNames, axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
      series: [
        { name: '优秀率', type: 'bar', data: tExc, itemStyle: { color: '#67C23A' } },
        { name: '不及格率', type: 'bar', data: tFail, itemStyle: { color: '#F56C6C' } }
      ]
    })
  }
}

const loadMetrics = async () => {
  loading.value = true
  try {
    const res = await getAdminTopicMetrics()
    metrics.value = res.data || []
    const tRes = await getAdminTeacherMetrics()
    teacherMetrics.value = tRes.data || []
    initCharts()
  } catch {
    ElMessage.error('加载全局选题质量数据失败')
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
  teacherChart?.resize()
  teacherRatioChart?.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  scoreChart?.dispose()
  ratioChart?.dispose()
  bubbleChart?.dispose()
  teacherChart?.dispose()
  teacherRatioChart?.dispose()
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

