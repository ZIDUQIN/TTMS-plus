<template>
  <div class="admin-layout">
    <NavBar />
    <div class="admin-content">
      <div class="page-header">
        <h2>数据统计</h2>
        <div class="header-actions">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px;"
          />
          <el-button :icon="Download" type="success" @click="handleExport">导出Excel</el-button>
        </div>
      </div>

      <!-- Summary cards -->
      <el-row :gutter="16" style="margin-bottom: 16px;">
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #409eff, #66b1ff);">
              <el-icon :size="24"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">${{ summary.totalRevenue }}</span>
              <span class="stat-label">总营收</span>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #67c23a, #85ce61);">
              <el-icon :size="24"><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ summary.totalOrders }}</span>
              <span class="stat-label">总订单数</span>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c, #f0c674);">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">${{ summary.avgPrice }}</span>
              <span class="stat-label">平均票价</span>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f56c6c, #ff8585);">
              <el-icon :size="24"><StarFilled /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ summary.topMovie }}</span>
              <span class="stat-label">最热影片</span>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- Charts -->
      <el-row :gutter="16">
        <el-col :span="12">
          <div class="card chart-card">
            <div class="card-header"><h3>营收趋势</h3></div>
            <v-chart :option="revenueChartOption" style="height: 360px;" autoresize />
          </div>
        </el-col>
        <el-col :span="12">
          <div class="card chart-card">
            <div class="card-header"><h3>影片销售排行</h3></div>
            <v-chart :option="rankingChartOption" style="height: 360px;" autoresize />
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 16px;">
        <el-col :span="24">
          <div class="card chart-card">
            <div class="card-header"><h3>月度营收</h3></div>
            <v-chart :option="monthlyChartOption" style="height: 360px;" autoresize />
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent, GridComponent
} from 'echarts/components'
import { getRevenueStats, getMovieRanking, getMonthlyStats, exportStatistics } from '@/api/statistics'
import { ElMessage } from 'element-plus'
import { Money, Tickets, TrendCharts, StarFilled, Download } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

use([CanvasRenderer, BarChart, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const dateRange = ref([])
const revenueData = ref([])
const rankingData = ref([])
const monthlyData = ref([])

const summary = computed(() => {
  const totalRevenue = revenueData.value.reduce((s, d) => s + (Number(d.revenue || d.amount) || 0), 0).toFixed(2)
  const totalOrders = revenueData.value.reduce((s, d) => s + (Number(d.orderCount || d.count) || 0), 0)
  const avgPrice = totalOrders > 0 ? (totalRevenue / totalOrders).toFixed(2) : '0.00'
  const topMovie = rankingData.value.length > 0 ? (rankingData.value[0].movieName || rankingData.value[0].name || '--') : '--'
  return { totalRevenue, totalOrders, avgPrice, topMovie }
})

const revenueChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: revenueData.value.map(d => d.date || d.label || ''),
    axisLabel: { rotate: 30 }
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '营收',
      type: 'bar',
      data: revenueData.value.map(d => Number(d.revenue || d.amount) || 0),
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: '#409eff' }, { offset: 1, color: '#66b1ff' }]
        },
        borderRadius: [4, 4, 0, 0]
      }
    },
    {
      name: '订单数',
      type: 'line',
      yAxisIndex: 0,
      data: revenueData.value.map(d => Number(d.orderCount || d.count) || 0),
      smooth: true,
      lineStyle: { color: '#67c23a' },
      itemStyle: { color: '#67c23a' }
    }
  ]
}))

const rankingChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'value' },
  yAxis: {
    type: 'category',
    data: rankingData.value.map(d => d.movieName || d.name || '').reverse(),
    axisLabel: { interval: 0 }
  },
  series: [{
    name: '销售额',
    type: 'bar',
    data: rankingData.value.map(d => Number(d.sales || d.revenue || d.amount) || 0).reverse(),
    itemStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
        colorStops: [{ offset: 0, color: '#e6a23c' }, { offset: 1, color: '#f56c6c' }]
      }
    }
  }]
}))

const monthlyChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: monthlyData.value.map(d => d.month || d.label || ''),
    boundaryGap: false
  },
  yAxis: { type: 'value' },
  series: [{
    name: '月度营收',
    type: 'line',
    data: monthlyData.value.map(d => Number(d.revenue || d.amount) || 0),
    smooth: true,
    areaStyle: {
      color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }] }
    },
    lineStyle: { color: '#409eff', width: 3 },
    itemStyle: { color: '#409eff' }
  }]
}))

async function handleExport() {
  try {
    const res = await exportStatistics()
    const blob = new Blob([res], { type: 'application/vnd.ms-excel' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `statistics_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (err) { /* handled */ }
}

async function fetchStats() {
  try {
    const [revRes, rankRes, monRes] = await Promise.all([
      getRevenueStats(),
      getMovieRanking(),
      getMonthlyStats()
    ])
    revenueData.value = revRes.data || []
    rankingData.value = rankRes.data || []
    monthlyData.value = monRes.data || []
  } catch (err) {
    revenueData.value = []
    rankingData.value = []
    monthlyData.value = []
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.header-actions { display: flex; gap: 12px; align-items: center; }
.stat-card { background: var(--bg-card); border-radius: var(--radius-md); padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: var(--shadow-light); margin-bottom: 4px; }
.stat-icon { width: 48px; height: 48px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.stat-info { display: flex; flex-direction: column; min-width: 0; }
.stat-value { font-size: 18px; font-weight: 700; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 2px; }
.card { background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-light); }
.card-header { padding: 16px 20px; border-bottom: 1px solid var(--border-light); }
.card-header h3 { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.chart-card { margin-bottom: 4px; }
</style>
