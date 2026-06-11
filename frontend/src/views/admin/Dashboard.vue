<template>
  <div class="dash-page">
    <div class="dash-container">
      <!-- Header -->
      <div class="dash-header">
        <div>
          <h2 class="dash-header__title">影院指挥中心</h2>
          <p class="dash-header__sub">实时业务监控与数据看板</p>
        </div>
        <div class="dash-header__right">
          <span class="dash-header__update">上次更新: {{ lastUpdate }}</span>
          <button class="refresh-btn" :class="{ spinning: refreshing }" :disabled="refreshing" @click="handleRefresh">
            <span class="material-symbols-outlined">refresh</span> {{ refreshing ? '刷新中...' : '刷新' }}
          </button>
        </div>
      </div>

      <!-- 4 KPI Cards -->
      <div class="kpi-grid">
        <div class="kpi-card" v-for="card in kpiCards" :key="card.label">
          <div class="kpi-card__top">
            <div class="kpi-card__icon" :style="{ background: card.iconBg }">
              <span class="material-symbols-outlined">{{ card.icon }}</span>
            </div>
            <span class="kpi-card__trend" :class="card.trendUp ? 'up' : (card.trendDown ? 'down' : '')">
              <span class="material-symbols-outlined">{{ card.trendUp ? 'trending_up' : (card.trendDown ? 'trending_down' : 'horizontal_rule') }}</span>
              {{ card.trendText }}
            </span>
          </div>
          <p class="kpi-card__label">{{ card.label }}</p>
          <h3 class="kpi-card__val">{{ card.value }}</h3>
          <svg class="kpi-card__spark" viewBox="0 0 100 20">
            <path :d="card.sparkline" fill="none" :stroke="card.sparkColor" stroke-width="1.5" stroke-dasharray="100" stroke-dashoffset="0" />
          </svg>
        </div>
      </div>

      <!-- Middle: Trend Chart + Ranking -->
      <div class="mid-grid">
        <!-- Trend -->
        <div class="glass-card trend-card">
          <div class="trend-card__header">
            <h4>近7天票房趋势</h4>
            <button class="pill-btn active">按天</button>
          </div>
          <div class="trend-chart" style="height:260px">
            <v-chart :option="chartOption" autoresize style="height:100%" />
          </div>
        </div>

        <!-- Ranking -->
        <div class="glass-card rank-card">
          <div class="rank-card__header">
            <h4>影片票房排行</h4>
            <router-link to="/admin/statistics" class="rank-card__link">查看全部<span class="material-symbols-outlined">chevron_right</span></router-link>
          </div>
          <div class="rank-list">
            <div v-for="(m, i) in rankings.slice(0, 5)" :key="i" class="rank-item">
              <div class="rank-item__info">
                <span class="rank-num" :class="'rank-' + (i+1)">{{ i + 1 }}</span>
                <span class="rank-name">{{ m.movieName }}</span>
              </div>
              <span class="rank-val">¥{{ shortK(m.revenue || 0) }}</span>
              <div class="rank-bar-wrap"><div class="rank-bar" :class="'bar-' + (i+1)" :style="{ width: barW(i) + '%' }"></div></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom: Recent Orders -->
      <div class="glass-card recent-card">
        <div class="recent-card__header">
          <h4>最近订单动态</h4>
          <router-link to="/admin/orders" class="rank-card__link">查看全部<span class="material-symbols-outlined">chevron_right</span></router-link>
        </div>
        <div class="timeline">
          <div class="timeline-line"></div>
          <div class="timeline-items">
            <div v-for="o in recentOrders.slice(0, 5)" :key="o.id" class="timeline-item">
              <div class="timeline-dot" :class="statusDotClass(o)"></div>
              <div class="timeline-body">
                <div class="timeline-top">
                  <span class="timeline-user">{{ o.username || '用户' }}</span>
                  <span class="timeline-action">{{ statusLabel(o) }}{{ (o.seatCount || 1) }}张票</span>
                  <span class="timeline-time">{{ shortTime(o.createTime || o.createdAt) }}</span>
                </div>
                <p class="timeline-detail">《{{ o.movieName }}》- {{ o.hallName }} - {{ formatSeats(o) }}</p>
                <span v-if="[0,1,2].includes(o.status ?? o.orderStatus)" class="timeline-amount">¥{{ o.totalAmount || o.totalPrice || '--' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- FAB -->
    <div class="fab" @click.stop="showFabMenu = !showFabMenu">
      <span class="material-symbols-outlined">{{ showFabMenu ? 'close' : 'add' }}</span>
      <div class="fab-menu" v-if="showFabMenu">
        <router-link to="/admin/pos" class="fab-menu__item" @click="showFabMenu=false">
          <span class="material-symbols-outlined">point_of_sale</span>
          <span>柜台售票</span>
        </router-link>
        <router-link to="/admin/movies" class="fab-menu__item">
          <span class="material-symbols-outlined">movie</span>
          <span>添加影片</span>
        </router-link>
        <router-link to="/admin/schedules" class="fab-menu__item">
          <span class="material-symbols-outlined">schedule</span>
          <span>新增排片</span>
        </router-link>
        <router-link to="/admin/snacks" class="fab-menu__item">
          <span class="material-symbols-outlined">fastfood</span>
          <span>添加卖品</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getAdminOrders } from '@/api/order'
import { getMovieList } from '@/api/movie'
import { getDailyRevenue, getMovieRanking } from '@/api/statistics'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const authStore = useAuthStore()
const todayOrders = ref(0); const todayRevenue = ref(0); const activeMovies = ref(0)
const recentOrders = ref([]); const dailyData = ref([]); const rankings = ref([])
const lastUpdate = ref('刚刚'); const refreshing = ref(false); const showFabMenu = ref(false)

const sparklines = [
  'M0,15 Q15,8 30,12 T60,5 T100,8',
  'M0,10 Q20,5 40,15 T70,8 T100,12',
  'M0,12 Q15,10 30,18 T50,5 T75,12 T100,15',
  'M0,10 Q25,10 50,10 T100,10',
]

const kpiCards = computed(() => [
  { label: '今日票房', value: '¥' + formatK(todayRevenue.value), icon: 'payments', iconBg: 'rgba(255,198,124,0.12)', trendText: '活跃', trendUp: true, sparkline: sparklines[0], sparkColor: '#ffc67c' },
  { label: '订单数', value: todayOrders.value, icon: 'shopping_cart', iconBg: 'rgba(199,197,213,0.1)', trendText: '稳定', trendUp: true, sparkline: sparklines[1], sparkColor: '#c7c5d5' },
  { label: '在映影片', value: activeMovies.value, icon: 'movie', iconBg: 'rgba(255,192,193,0.1)', trendUp: false, trendDown: false, trendText: '持平', sparkline: sparklines[3], sparkColor: '#e8a850' },
  { label: '总订单', value: recentOrders.value.length, icon: 'receipt_long', iconBg: 'rgba(232,168,80,0.15)', trendUp: true, trendText: '累计', sparkline: sparklines[1], sparkColor: '#e8a850' },
])

const chartOption = computed(() => ({
  grid: { top: 10, right: 20, bottom: 20, left: 50 },
  tooltip: { trigger: 'axis', formatter: p => `${p[0].axisValue}<br/>票房: ¥${Number(p[0].value).toLocaleString()}` },
  xAxis: {
    type: 'category',
    data: dailyData.value.map(d => d.date?.slice(5) || ''),
    axisLine: { lineStyle: { color: 'var(--border-color,#515151)' } },
    axisLabel: { color: 'var(--text-tertiary,#999)', fontSize: 10, fontFamily: 'JetBrains Mono,monospace' },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
    axisLabel: { color: 'var(--text-tertiary,#999)', fontSize: 10, fontFamily: 'JetBrains Mono,monospace', formatter: v => v >= 10000 ? (v/10000).toFixed(1)+'万' : v },
  },
  series: [{
    name: '票房',
    type: 'line',
    data: dailyData.value.map(d => Number(d.revenue || 0)),
    smooth: true,
    symbol: 'circle', symbolSize: 6,
    lineStyle: { color: '#ffc67c', width: 2.5 },
    itemStyle: { color: '#ffc67c', borderColor: '#2a1800', borderWidth: 2 },
    areaStyle: {
      color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: 'rgba(255,198,124,0.3)' }, { offset: 1, color: 'rgba(255,198,124,0)' }] }
    },
  }],
}))

function barW(i) {
  const max = Math.max(...rankings.value.map(r => Number(r.revenue || 0)), 1)
  return Math.round((Number(rankings.value[i]?.revenue || 0) / max) * 100)
}

function formatK(n) { const v = Number(n || 0); return v >= 10000 ? (v/10000).toFixed(1)+'万' : v.toLocaleString() }
function shortK(n) { const v = Number(n || 0); return v >= 1000 ? (v/1000).toFixed(1)+'k' : String(Math.round(v)) }
function shortTime(d) { if (!d) return ''; const dt = new Date(d); const now = Date.now(); const diff = Math.floor((now - dt.getTime())/60000); return diff < 1 ? '刚刚' : diff < 60 ? diff+'分钟前' : diff < 1440 ? Math.floor(diff/60)+'小时前' : dt.toLocaleDateString('zh-CN', {month:'short',day:'numeric'}) }
function formatSeats(o) { const s = o.seatNumbers || o.seats; if (!s) return '--'; return Array.isArray(s) ? s.join('、') : String(s).split(',').join('、') }

function statusLabel(o) {
  const s = o.status ?? o.orderStatus
  const map = { 0: '下单 ', 1: '购买了 ', 2: '观看了 ', 3: '改签 ', 4: '退票 ' }
  return map[s] || ''
}
function statusDotClass(o) { const s = o.status ?? o.orderStatus; return s === 4 ? 'red' : s >= 2 ? 'green' : s === 1 ? 'gold' : '' }

async function fetchData() {
  const today = new Date().toISOString().split('T')[0]
  const weekAgo = new Date(Date.now() - 7 * 86400000).toISOString().split('T')[0]

  const [orderRes, movieRes, dailyRes, rankRes] = await Promise.allSettled([
    getAdminOrders({ size: 200 }),
    getMovieList(),
    getDailyRevenue(weekAgo, today),
    getMovieRanking()
  ])

  if (orderRes.status === 'fulfilled') {
    const orders = orderRes.value.data?.records || orderRes.value.data || []
    recentOrders.value = orders
    const todayOrdersList = orders.filter(o => (o.createTime||o.createdAt||'').substring(0,10) === today)
    todayOrders.value = todayOrdersList.length
    const paid = todayOrdersList.filter(o => { const s = o.status??o.orderStatus; return s === 1 || s === 2 })
    const rev = paid.reduce((s,o) => s + Number(o.totalAmount||o.totalPrice||0), 0)
    todayRevenue.value = isNaN(rev) ? 0 : rev
  }
  if (movieRes.status === 'fulfilled') {
    const movies = movieRes.value.data?.records || movieRes.value.data || []
    activeMovies.value = movies.filter(m => m.status === 1).length
  }
  if (dailyRes.status === 'fulfilled') dailyData.value = dailyRes.value.data || []
  if (rankRes.status === 'fulfilled') rankings.value = rankRes.value.data || []

  lastUpdate.value = shortTime(new Date().toISOString())
}

async function handleRefresh() {
  refreshing.value = true
  await fetchData()
  lastUpdate.value = '刚刚'
  refreshing.value = false
}

onMounted(fetchData)
</script>

<style scoped>
.dash-page { min-height: 100vh; background: var(--bg-primary); }
.dash-container { max-width: 1440px; margin: 0 auto; padding: 24px 32px; }

/* Header */
.dash-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.dash-header__title { font-size: 28px; font-weight: 600; color: var(--text-primary); }
.dash-header__sub { font-size: 14px; color: var(--text-secondary); margin-top: 2px; }
.dash-header__right { display: flex; align-items: center; gap: 12px; }
.dash-header__update { font-size: 12px; color: var(--text-tertiary); }
.refresh-btn { display: flex; align-items: center; gap: 4px; padding: 8px 14px; border-radius: var(--radius-md); border: 1px solid rgba(232,168,80,0.2); background: rgba(232,168,80,0.08); color: var(--color-primary); font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; }
.refresh-btn:hover { background: rgba(232,168,80,0.15); }
.refresh-btn .material-symbols-outlined { font-size: 16px; }
.refresh-btn.spinning .material-symbols-outlined { animation: spin 0.8s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* KPI Cards */
.kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
.kpi-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl); padding: 20px 24px; box-shadow: 0 0 15px rgba(232,168,80,0.04); transition: transform 0.2s; }
.kpi-card:hover { transform: translateY(-2px); }
[data-theme='dark'] .kpi-card { background: rgba(20,20,31,0.8); backdrop-filter: blur(12px); border-color: rgba(255,255,255,0.05); }
.kpi-card__top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.kpi-card__icon { width: 36px; height: 36px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; }
.kpi-card__icon .material-symbols-outlined { font-size: 20px; color: var(--color-primary); }
.kpi-card__trend { display: flex; align-items: center; gap: 2px; font-size: 12px; font-weight: 500; }
.kpi-card__trend.up { color: #4ade80; }
.kpi-card__trend.down { color: #ff8a80; }
.kpi-card__trend .material-symbols-outlined { font-size: 14px; }
.kpi-card__label { font-size: 12px; color: var(--text-tertiary); margin-bottom: 4px; }
.kpi-card__val { font-family: 'JetBrains Mono', monospace; font-size: 28px; font-weight: 700; color: var(--text-primary); }
.kpi-card__spark { width: 100%; height: 32px; margin-top: 8px; }

/* Mid Grid */
.mid-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 20px; margin-bottom: 24px; }
.glass-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl); padding: 24px; }
[data-theme='dark'] .glass-card { background: rgba(20,20,31,0.8); backdrop-filter: blur(12px); border-color: rgba(255,255,255,0.05); }

/* Trend */
.trend-card__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.trend-card__header h4 { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.pill-btn { padding: 4px 12px; border: none; border-radius: var(--radius-md); font-size: 12px; font-weight: 500; background: var(--bg-hover); color: var(--text-primary); cursor: pointer; font-family: inherit; }
.pill-btn.active { background: rgba(232,168,80,0.15); color: var(--color-primary); }
.trend-chart { position: relative; }
.trend-chart__grid { position: absolute; inset: 0 20px 24px 20px; display: flex; flex-direction: column; justify-content: space-between; pointer-events: none; }
.trend-chart__gridline { border-bottom: 1px solid rgba(255,255,255,0.04); }
.trend-chart__svg { width: 100%; height: 180px; }
.trend-chart__labels { display: flex; justify-content: space-between; padding: 0 20px; font-family: 'JetBrains Mono', monospace; font-size: 11px; color: var(--text-tertiary); }

/* Ranking */
.rank-card__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.rank-card__header h4 { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.rank-card__link { display: flex; align-items: center; gap: 2px; font-size: 12px; color: var(--color-primary); font-weight: 500; }
.rank-card__link:hover { text-decoration: underline; }
.rank-card__link .material-symbols-outlined { font-size: 14px; }
.rank-list { display: flex; flex-direction: column; gap: 18px; }
.rank-item { display: grid; grid-template-columns: auto 1fr 60px; gap: 10px; align-items: center; }
.rank-item__info { display: flex; align-items: center; gap: 8px; }
.rank-num { width: 22px; height: 22px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; }
.rank-1 { background: #ffc67c; color: #2a1800; }
.rank-2 { background: rgba(199,197,213,0.3); color: var(--text-primary); }
.rank-3 { background: rgba(255,255,255,0.1); color: var(--text-secondary); }
.rank-name { font-size: 13px; font-weight: 500; color: var(--text-primary); }
.rank-val { font-family: 'JetBrains Mono', monospace; font-size: 13px; text-align: right; }
.rank-bar-wrap { grid-column: 2 / -1; height: 6px; background: var(--bg-hover); border-radius: 3px; overflow: hidden; }
.rank-bar { height: 100%; border-radius: 3px; }
.bar-1 { background: linear-gradient(90deg, #ffc67c, #e8a850); }
.bar-2 { background: rgba(199,197,213,0.6); }
.bar-3 { background: rgba(255,255,255,0.3); }

/* Recent */
.recent-card__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.recent-card__header h4 { font-size: 18px; font-weight: 600; color: var(--text-primary); }

.timeline { position: relative; padding-left: 20px; }
.timeline-line { position: absolute; left: 5px; top: 0; bottom: 0; width: 1px; background: var(--border-color); }
.timeline-items { display: flex; flex-direction: column; gap: 24px; }
.timeline-item { display: flex; gap: 14px; position: relative; }
.timeline-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--color-primary); flex-shrink: 0; margin-left: -20px; margin-top: 4px; }
.timeline-dot.green { background: var(--color-emerald); box-shadow: 0 0 0 3px rgba(26,107,76,0.2); }
.timeline-dot.red { background: var(--color-danger); }
.timeline-dot.gold { background: var(--color-primary); box-shadow: 0 0 0 3px rgba(232,168,80,0.2); }
.timeline-body { flex: 1; min-width: 0; }
.timeline-top { display: flex; gap: 8px; align-items: baseline; margin-bottom: 4px; }
.timeline-user { font-weight: 600; font-size: 13px; color: var(--text-primary); }
.timeline-action { font-size: 13px; color: var(--text-secondary); }
.timeline-time { margin-left: auto; font-size: 11px; color: var(--text-tertiary); white-space: nowrap; }
.timeline-detail { font-size: 12px; color: var(--text-tertiary); }
.timeline-amount { font-family: 'JetBrains Mono', monospace; font-size: 12px; color: var(--color-primary); font-weight: 600; display: block; margin-top: 4px; }

/* FAB */
.fab { position: fixed; bottom: 32px; right: 32px; width: 56px; height: 56px; border-radius: 50%; background: var(--color-primary); color: #2a1800; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 20px rgba(232,168,80,0.3); transition: all 0.2s; z-index: 50; cursor: pointer; }
.fab:hover { transform: scale(1.1); }
.fab .material-symbols-outlined { font-size: 28px; transition: transform 0.3s; }
.fab-menu { position: absolute; bottom: 68px; right: 0; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-lg); box-shadow: var(--shadow-heavy); padding: 8px; display: flex; flex-direction: column; gap: 2px; min-width: 160px; }
.fab-menu__item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-radius: var(--radius-md); color: var(--text-primary); font-size: 13px; font-weight: 500; transition: background 0.15s; }
.fab-menu__item:hover { background: var(--bg-hover); color: var(--color-primary); }
.fab-menu__item .material-symbols-outlined { font-size: 18px; }

@media (max-width: 1024px) { .kpi-grid { grid-template-columns: repeat(2, 1fr); } .mid-grid { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .kpi-grid { grid-template-columns: 1fr; } }
</style>
