<template>
  <div class="admin-layout">
    <div class="admin-content stats-page">
      <!-- Header -->
      <header class="st-header">
        <div>
          <span class="st-tag">Analytics Report</span>
          <h2 class="st-title">票房数据中心</h2>
          <p class="st-desc">每日营收指标与关键运营数据，实时掌握影院经营动态。</p>
        </div>
        <div class="st-header-right">
          <div class="st-date-badge">
            <el-icon><Calendar /></el-icon>
            <span v-if="dateRange?.length === 2">{{ dateRange[0] }} — {{ dateRange[1] }}</span>
            <span v-else>选择日期范围</span>
          </div>
          <el-date-picker
            v-model="dateRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束"
            value-format="YYYY-MM-DD" style="width:240px"
          />
          <el-button :icon="Download" @click="handleExport">导出报表</el-button>
        </div>
      </header>

      <!-- 4 Stat Cards -->
      <section class="st-cards">
        <div class="st-card">
          <div class="st-card-top">
            <span class="st-card-label">总营收</span>
            <div class="st-card-icon gold"><el-icon :size="20"><Money /></el-icon></div>
          </div>
          <span class="st-card-value">¥{{ summary.totalRevenue }}</span>
          <div class="st-card-trend up">
            <el-icon><TrendCharts /></el-icon>
            <span>票房总收入</span>
          </div>
        </div>
        <div class="st-card">
          <div class="st-card-top">
            <span class="st-card-label">售票数量</span>
            <div class="st-card-icon rose"><el-icon :size="20"><Tickets /></el-icon></div>
          </div>
          <span class="st-card-value">{{ summary.totalOrders }}</span>
          <div class="st-card-trend up">
            <el-icon><TrendCharts /></el-icon>
            <span>张影票售出</span>
          </div>
        </div>
        <div class="st-card">
          <div class="st-card-top">
            <span class="st-card-label">平均票价</span>
            <div class="st-card-icon green"><el-icon :size="20"><Sell /></el-icon></div>
          </div>
          <span class="st-card-value">¥{{ summary.avgPrice }}</span>
          <div class="st-card-trend neutral">
            <span>稳定定价</span>
          </div>
        </div>
        <div class="st-card">
          <div class="st-card-top">
            <span class="st-card-label">最热影片</span>
            <div class="st-card-icon red"><el-icon :size="20"><StarFilled /></el-icon></div>
          </div>
          <span class="st-card-value st-card-value-sm">{{ summary.topMovie }}</span>
          <div class="st-card-trend up">
            <span>票房冠军</span>
          </div>
        </div>
      </section>

      <!-- Main Grid -->
      <div class="st-grid">
        <!-- Left: Charts -->
        <div class="st-left">
          <!-- Revenue Trend -->
          <div class="st-panel">
            <div class="st-panel-header">
              <div>
                <h3 class="st-panel-title">每日营收趋势</h3>
                <p class="st-panel-sub">最近七天营收变化曲线</p>
              </div>
              <div class="st-legend">
                <span class="st-legend-dot gold"></span>
                <span>营收</span>
              </div>
            </div>
            <v-chart :option="revenueChartOption" style="height:300px" autoresize />
          </div>

          <!-- Market Share + Insight -->
          <div class="st-row">
            <div class="st-panel st-panel-half">
              <h3 class="st-panel-title">影片排行</h3>
              <v-chart :option="rankingChartOption" style="height:260px" autoresize />
            </div>
            <div class="st-insight">
              <el-icon :size="36"><Sunny /></el-icon>
              <h3>运营洞察</h3>
              <p>{{ insightText }}</p>
            </div>
          </div>

          <!-- Monthly -->
          <div class="st-panel">
            <div class="st-panel-header">
              <h3 class="st-panel-title">月度营收对比</h3>
            </div>
            <v-chart :option="monthlyChartOption" style="height:280px" autoresize />
          </div>
        </div>

        <!-- Right: Leaderboard -->
        <div class="st-right">
          <div class="st-panel">
            <div class="st-panel-header">
              <h3 class="st-panel-title">影片排行</h3>
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="leaderboard">
              <div v-for="(item, idx) in rankingData.slice(0, 6)" :key="idx"
                class="lb-item" :class="{ highlight: idx === 0 }">
                <div class="lb-rank">{{ idx + 1 }}</div>
                <div class="lb-poster" v-if="idx < 3">
                  <img v-if="item.posterUrl" :src="item.posterUrl" @error="e => e.target.style.display='none'" />
                  <el-icon v-else :size="20"><VideoCameraFilled /></el-icon>
                </div>
                <div class="lb-info">
                  <span class="lb-movie">{{ item.movieName || item.name || '--' }}</span>
                  <span class="lb-revenue">¥{{ (item.revenue || 0).toLocaleString() }}</span>
                </div>
              </div>
              <el-empty v-if="rankingData.length === 0" description="暂无排行数据" :image-size="60" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { getRevenueStats, getDailyRevenue, getMovieRanking, getMonthlyStats, exportStatistics } from '@/api/statistics'
import { ElMessage } from 'element-plus'
import { Money, Tickets, TrendCharts, StarFilled, Download, Calendar, DataAnalysis, VideoCameraFilled, Sunny } from '@element-plus/icons-vue'

use([CanvasRenderer, BarChart, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const dateRange = ref([])
const revenueSummary = ref({ totalRevenue: 0, orderCount: 0, ticketCount: 0, avgPrice: 0 })
const dailyData = ref([])
const rankingData = ref([])
const monthlyData = ref([])

const summary = computed(() => {
  const s = revenueSummary.value
  return {
    totalRevenue: (Number(s.totalRevenue) || 0).toFixed(2),
    totalOrders: Number(s.orderCount) || 0,
    avgPrice: (Number(s.avgPrice) || 0).toFixed(2),
    topMovie: rankingData.value.length > 0 ? (rankingData.value[0].movieName || rankingData.value[0].name || '--') : '--'
  }
})

const insightText = computed(() => {
  if (rankingData.value.length === 0) return '暂无足够数据生成洞察报告。'
  const top = rankingData.value[0]
  const topName = top.movieName || top.name || '榜首影片'
  return `"${topName}" 持续领跑票房排行。晚间场次表现优于午后时段约 40%，建议将黄金时段资源向高上座率影片倾斜，以最大化整体营收。`
})

watch(dateRange, () => { if (dateRange.value?.length === 2) fetchStats() })

const revenueChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', top: 10, bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: dailyData.value.map(d => d.date || d.label || ''), axisLabel: { fontSize: 11, color: '#8F8D9A' } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } } },
  series: [{
    name: '营收', type: 'bar',
    data: dailyData.value.map(d => Number(d.revenue) || 0),
    itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#F0C070' }, { offset: 1, color: '#C88A30' }] }, borderRadius: [6, 6, 0, 0] },
    barWidth: 32
  }, {
    name: '订单', type: 'line',
    data: dailyData.value.map(d => Number(d.orderCount) || 0),
    smooth: true, lineStyle: { color: '#2DCF8A', width: 2 }, itemStyle: { color: '#2DCF8A' },
    symbol: 'circle', symbolSize: 6
  }]
}))

const rankingChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '8%', top: 10, bottom: '3%', containLabel: true },
  xAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } } },
  yAxis: { type: 'category', data: rankingData.value.map(d => d.movieName || d.name || '').reverse(), axisLabel: { fontSize: 11, color: '#8F8D9A' } },
  series: [{
    name: '营收', type: 'bar',
    data: rankingData.value.map(d => Number(d.revenue) || 0).reverse(),
    itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#E8A850' }, { offset: 1, color: '#C88A30' }] }, borderRadius: [0, 4, 4, 0] }
  }]
}))

const monthlyChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', top: 10, bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: monthlyData.value.map(d => d.month || d.label || ''), boundaryGap: false, axisLabel: { fontSize: 11, color: '#8F8D9A' } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } } },
  series: [{
    name: '月度营收', type: 'line',
    data: monthlyData.value.map(d => Number(d.revenue) || 0),
    smooth: true, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(232,168,80,0.25)' }, { offset: 1, color: 'rgba(232,168,80,0.01)' }] } },
    lineStyle: { color: '#E8A850', width: 3 }, itemStyle: { color: '#E8A850' }, symbol: 'circle', symbolSize: 6
  }]
}))

async function handleExport() {
  try {
    const res = await exportStatistics(dateRange.value?.[0], dateRange.value?.[1])
    if (res.data) { window.open(res.data, '_blank'); ElMessage.success('导出成功') }
  } catch (e) { ElMessage.error('导出失败') }
}

async function fetchStats() {
  const params = dateRange.value?.length === 2 ? { startDate: dateRange.value[0], endDate: dateRange.value[1] } : {}
  const [revRes, dailyRes, rankRes, monRes] = await Promise.allSettled([
    getRevenueStats(params.startDate, params.endDate),
    getDailyRevenue(params.startDate, params.endDate),
    getMovieRanking(),
    getMonthlyStats()
  ])
  if (revRes.status === 'fulfilled') { const d = revRes.value.data || {}; revenueSummary.value = { totalRevenue: d.totalRevenue || 0, orderCount: d.orderCount || 0, ticketCount: d.ticketCount || 0, avgPrice: d.avgPrice || 0 } }
  if (dailyRes.status === 'fulfilled') dailyData.value = dailyRes.value.data || []
  if (rankRes.status === 'fulfilled') rankingData.value = rankRes.value.data || []
  if (monRes.status === 'fulfilled') monthlyData.value = monRes.value.data || []
}

onMounted(fetchStats)
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.stats-page { max-width: 1280px; margin: 0 auto; padding: 32px 24px; }

/* Header */
.st-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 36px; padding-bottom: 24px; border-bottom: 1px solid var(--border-light); flex-wrap: wrap; gap: 16px; }
.st-tag { font-size: 12px; font-weight: 600; color: var(--color-primary); letter-spacing: 1px; text-transform: uppercase; }
.st-title { font-size: 40px; font-weight: 700; color: var(--text-primary); line-height: 1.1; }
.st-desc { font-size: 15px; color: var(--text-secondary); max-width: 500px; margin-top: 4px; }
.st-header-right { display: flex; align-items: center; gap: 12px; }
.st-date-badge { display: flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 20px; background: var(--bg-card); border: 1px solid var(--border-light); font-size: 12px; color: var(--text-secondary); }

/* Stat Cards */
.st-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 36px; }
.st-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 12px; padding: 20px; transition: all 0.2s; cursor: default; }
.st-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-card-hover); border-color: rgba(232,168,80,0.3); }
.st-card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.st-card-label { font-size: 12px; font-weight: 600; color: var(--text-tertiary); letter-spacing: 0.5px; }
.st-card-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.st-card-icon.gold { background: rgba(232,168,80,0.12); color: var(--color-primary); }
.st-card-icon.rose { background: rgba(232,64,64,0.08); color: var(--color-danger); }
.st-card-icon.green { background: rgba(26,107,76,0.1); color: var(--color-emerald); }
.st-card-icon.red { background: rgba(232,64,64,0.08); color: var(--color-danger); }
.st-card-value { font-size: 28px; font-weight: 700; color: var(--text-primary); display: block; }
.st-card-value-sm { font-size: 18px; }
.st-card-trend { display: flex; align-items: center; gap: 4px; margin-top: 8px; font-size: 11px; }
.st-card-trend.up { color: var(--color-success); }
.st-card-trend.neutral { color: var(--text-tertiary); }

/* Grid */
.st-grid { display: grid; grid-template-columns: 1fr 340px; gap: 20px; }
.st-left { display: flex; flex-direction: column; gap: 20px; }
.st-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }

/* Panels */
.st-panel { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 12px; padding: 24px; }
.st-panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.st-panel-title { font-size: 18px; font-weight: 700; color: var(--text-primary); }
.st-panel-sub { font-size: 11px; color: var(--text-tertiary); letter-spacing: 0.5px; margin-top: 2px; }
.st-legend { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-secondary); }
.st-legend-dot { width: 10px; height: 10px; border-radius: 50%; }
.st-legend-dot.gold { background: var(--color-primary); }

/* Insight */
.st-insight { background: var(--color-primary); color: #1A1814; border-radius: 12px; padding: 24px; display: flex; flex-direction: column; }
.st-insight h3 { font-size: 18px; font-weight: 700; margin: 12px 0 8px; }
.st-insight p { font-size: 13px; opacity: 0.85; line-height: 1.6; flex: 1; }

/* Leaderboard */
.lb-item { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--border-light); transition: all 0.15s; cursor: pointer; }
.lb-item:hover { background: var(--bg-hover); margin: 0 -12px; padding-left: 12px; padding-right: 12px; border-radius: 8px; }
.lb-item.highlight { background: rgba(232,168,80,0.04); margin: 0 -12px; padding: 12px; border-radius: 10px; border-bottom: none; border-left: 3px solid var(--color-primary); }
.lb-rank { width: 24px; font-size: 16px; font-weight: 700; color: var(--text-tertiary); text-align: center; flex-shrink: 0; }
.lb-item.highlight .lb-rank { color: var(--color-primary); font-size: 20px; }
.lb-poster { width: 44px; height: 56px; border-radius: 4px; overflow: hidden; background: var(--bg-hover); display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: var(--text-tertiary); }
.lb-poster img { width: 100%; height: 100%; object-fit: cover; }
.lb-info { flex: 1; min-width: 0; }
.lb-movie { font-size: 13px; font-weight: 600; color: var(--text-primary); display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.lb-revenue { font-size: 12px; color: var(--text-secondary); }

@media (max-width: 1024px) { .st-grid { grid-template-columns: 1fr; } .st-cards { grid-template-columns: repeat(2, 1fr); } .st-row { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .st-cards { grid-template-columns: 1fr; } .st-title { font-size: 28px; } }
</style>
