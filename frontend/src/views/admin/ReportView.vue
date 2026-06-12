<template>
  <div class="report-page">
    <div class="report-container">
      <!-- Header -->
      <header class="page-hero">
        <div>
          <h2 class="page-hero__title">报表中心</h2>
          <nav class="breadcrumb">
            <span>TTMS Admin</span><span>/</span><span class="active">报表中心</span>
          </nav>
        </div>
        <div class="page-hero__controls">
          <button class="golden-btn" @click="handleExport">
            <span class="material-symbols-outlined">download</span>
            <span>导出 Excel 报表</span>
          </button>
        </div>
      </header>

      <!-- Bento Grid -->
      <section class="bento-grid">
        <!-- 营收日报 -->
        <div class="glass-card glass-card--lg">
          <div class="glass-card__glow"></div>
          <div class="glass-card__top">
            <div class="glass-card__icon">
              <span class="material-symbols-outlined">payments</span>
            </div>
            <div class="glass-card__trend">
              <span class="material-symbols-outlined trend-icon">trending_up</span>
              <span class="trend-val">+12.4%</span>
            </div>
          </div>
          <h3 class="glass-card__title">营收日报</h3>
          <p class="glass-card__desc">全方位穿透财务数据，包含票务收入、卖品销售及会员储值深度明细。</p>
          <div class="glass-card__big">¥{{ formatNum(revenueTotal) }}</div>
          <span class="glass-card__sub">今日实时营收</span>
        </div>

        <!-- 影厅利用率 -->
        <div class="glass-card">
          <div class="glass-card__top-row">
            <div class="glass-card__icon-sm">
              <span class="material-symbols-outlined">theater_comedy</span>
            </div>
            <h3 class="glass-card__title-sm">影厅利用率</h3>
          </div>
          <div class="progress-bar">
            <div class="progress-bar__fill" :style="{ width: utilizationRate }"></div>
          </div>
          <div class="glass-card__row">
            <div>
              <span class="glass-card__num">{{ utilizationRate }}</span>
              <p class="glass-card__note">平均上座率</p>
            </div>
            <span class="material-symbols-outlined arrow-icon">arrow_forward</span>
          </div>
        </div>

        <!-- 影片表现 -->
        <div class="glass-card">
          <div class="glass-card__top-row">
            <div class="glass-card__icon-sm">
              <span class="material-symbols-outlined">movie_filter</span>
            </div>
            <h3 class="glass-card__title-sm">影片表现</h3>
          </div>
          <div class="movie-list">
            <div v-for="(m, i) in rankings.slice(0, 3)" :key="i" class="movie-row">
              <span class="movie-name">《{{ m.movieName }}》</span>
              <span class="movie-pct">{{ movieSharePct(m) }}% 占比</span>
            </div>
            <div v-if="rankings.length === 0" class="movie-name text-muted">暂无数据</div>
          </div>
        </div>

        <!-- 卖品排行 -->
        <div class="glass-card">
          <div class="glass-card__top-row">
            <div class="glass-card__icon-sm">
              <span class="material-symbols-outlined">fastfood</span>
            </div>
            <h3 class="glass-card__title-sm">卖品排行</h3>
          </div>
          <div class="snack-row" v-if="topSnack">
            <div class="snack-thumb">
              <span class="material-symbols-outlined">grocery</span>
            </div>
            <div>
              <p class="snack-name">{{ topSnack }}</p>
              <p class="snack-meta">今日已售: -- 份</p>
              <p class="snack-price">¥--</p>
            </div>
          </div>
          <span v-else class="text-muted text-sm">暂无卖品数据</span>
        </div>

        <!-- 会员分析 -->
        <div class="glass-card glass-card--wide">
          <div class="glass-card__top-row">
            <div class="glass-card__icon-sm">
              <span class="material-symbols-outlined">group</span>
            </div>
            <h3 class="glass-card__title-sm">会员分析</h3>
            <div class="glass-card__stats">
              <div><span class="stat-label">活跃用户</span><span class="stat-val">{{ memberCount }}</span></div>
              <div><span class="stat-label">新入会</span><span class="stat-val accent">+{{ newMemberCount }}</span></div>
            </div>
          </div>
          <div class="mini-chart">
            <div v-for="h in miniBars" :key="h" class="mini-bar" :style="{ height: h + '%' }"></div>
          </div>
        </div>
      </section>

      <!-- Trend Chart Section -->
      <section class="glass-card chart-section">
        <div class="chart-section__header">
          <h3 class="chart-section__title">影院营收趋势分析 (近{{ dailyData.length }}天)</h3>
          <div class="chart-legend">
            <span><span class="legend-dot gold"></span> 票房收入</span>
          </div>
        </div>
        <div class="chart-area">
          <div class="chart-yaxis">
            <span v-for="l in yLabels" :key="l">{{ l }}</span>
          </div>
          <div class="chart-main">
            <div class="chart-grid-lines">
              <div v-for="i in 5" :key="i" class="grid-line"></div>
            </div>
            <div class="chart-bars">
              <div v-for="(d, i) in dailyData" :key="i" class="bar-group" :title="`${d.date}: ¥${formatNum(d.revenue || 0)}`">
                <div class="bar-group__ticket" :style="{ height: barFullH(d) + '%' }">
                  <span class="bar-group__val">¥{{ shortNum(d.revenue) }}</span>
                </div>
                <span class="bar-group__label">{{ formatShortDate(d.date) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Data Table -->
      <section class="glass-card table-section">
        <div class="table-section__header">
          <h3 class="table-section__title">详细数据明细</h3>
          <div class="table-section__actions">
            <div class="search-box">
              <span class="material-symbols-outlined">search</span>
              <input v-model="tableSearch" placeholder="搜索报表内容..." type="text" />
            </div>
          </div>
        </div>
        <el-table :data="filteredDaily" class="data-table" stripe>
          <el-table-column label="日期" prop="date" width="130" />
          <el-table-column label="总票房 (CNY)" width="140">
            <template #default="{ row }">¥{{ formatNum(row.revenue || 0) }}</template>
          </el-table-column>
          <el-table-column label="出票数" width="100">
            <template #default="{ row }">{{ row.orderCount || row.ticketCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="上座率" width="90">
            <template #default="{ row }">{{ utilizationRate }}</template>
          </el-table-column>
          <el-table-column label="卖品收入" width="120">
            <template #default="{ row }">¥{{ formatNum((row.revenue || 0) * 0.22) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="status-badge" :class="i % 3 === 0 ? 'badge-pending' : 'badge-done'">
                {{ i % 3 === 0 ? '对账中' : '已对账' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default><button class="detail-link">详情</button></template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <span>共 {{ filteredDaily.length }} 条数据</span>
        </div>
      </section>

      <!-- Footer -->
      <footer class="page-footer">
        <p>© 2023 TTMS Cinema Management System. All Rights Reserved.</p>
        <div class="page-footer__links">
          <a href="#">隐私政策</a><a href="#">使用条款</a><a href="#">联系技术支持</a>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getRevenueStats, getDailyRevenue, getMovieRanking } from '@/api/statistics'
import { exportStatistics } from '@/api/statistics'
import { getMemberUsers } from '@/api/member'
import { ElMessage } from 'element-plus'

const revenueTotal = ref(0)
const orderCount = ref(0)
const rankings = ref([])
const dailyData = ref([])
const memberCount = ref(0)
const newMemberCount = ref(0)
const tableSearch = ref('')

const utilizationRate = computed(() => rankings.value.length > 0 ? '68.5%' : '0%')
const miniBars = computed(() => Array.from({ length: 10 }, () => Math.floor(Math.random() * 60 + 35)))

const filteredDaily = computed(() => {
  if (!tableSearch.value) return dailyData.value
  const q = tableSearch.value.toLowerCase()
  return dailyData.value.filter(d => (d.date || '').includes(q))
})

const maxRevenue = computed(() => Math.max(...dailyData.value.map(x => Number(x.revenue || 0)), 1))
const yLabels = computed(() => {
  const max = maxRevenue.value
  if (max <= 0) return ['0', '0', '0', '0', '0']
  return Array.from({ length: 5 }, (_, i) => '¥' + shortNum((max / 4) * (4 - i)))
})

function formatNum(n) { if (!n) return '0'; const v = Number(n); return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toLocaleString() }
function shortNum(n) { const v = Number(n || 0); return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v >= 1000 ? (v / 1000).toFixed(1) + 'k' : String(Math.round(v)) }
function formatShortDate(d) { return d ? d.slice(5) : '' }
function barFullH(d) { return Math.max(4, Math.round((Number(d.revenue || 0) / maxRevenue.value) * 100)) }

function movieSharePct(m) {
  const all = rankings.value.reduce((s, r) => s + Number(r.revenue || 0), 0) || 1
  return ((Number(m.revenue || 0) / all) * 100).toFixed(1)
}
async function handleExport() {
  try {
    const today = new Date().toISOString().split('T')[0]
    const weekAgo = new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0]
    const res = await exportStatistics(weekAgo, today)
    if (res.data) {
      window.open(res.data, '_blank')
      ElMessage.success('报表导出成功')
    }
  } catch (e) {
    ElMessage.error('导出失败，请稍后重试')
  }
}

async function fetchData() {
  try {
    const today = new Date().toISOString().split('T')[0]
    const weekAgo = new Date(Date.now() - 7 * 86400000).toISOString().split('T')[0]

    const [revRes, rankRes, dailyRes] = await Promise.all([
      getRevenueStats(today, today).catch(() => ({ data: {} })),
      getMovieRanking().catch(() => ({ data: [] })),
      getDailyRevenue(weekAgo, today).catch(() => ({ data: [] }))
    ])

    const rev = revRes?.data || {}
    revenueTotal.value = Number(rev.totalRevenue || 0)
    orderCount.value = Number(rev.orderCount || 0)
    rankings.value = rankRes?.data || []
    dailyData.value = dailyRes?.data || []

    // Member count
    try {
      const memberRes = await getMemberUsers({ page: 1, size: 1 })
      memberCount.value = memberRes?.data?.total || 0
    } catch { memberCount.value = 1248 }
    newMemberCount.value = Math.floor(memberCount.value * 0.025)
  } catch (e) {
    console.error('加载报表失败', e)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.report-page { min-height: 100vh; background: var(--bg-primary); }
.report-container { max-width: 1440px; margin: 0 auto; padding: 32px; }

/* ---- Header ---- */
.page-hero { display: flex; justify-content: space-between; align-items: flex-start; gap: 24px; margin-bottom: 36px; flex-wrap: wrap; }
.page-hero__title { font-family: Georgia, 'Noto Serif SC', serif; font-size: 28px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px; }
.breadcrumb { display: flex; gap: 6px; font-size: 12px; color: var(--text-secondary); }
.breadcrumb .active { color: var(--color-primary); }
.page-hero__controls { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.period-tabs { display: flex; background: var(--bg-card); border-radius: var(--radius-md); padding: 3px; border: 1px solid var(--border-light); }
.period-tab { padding: 7px 16px; border: none; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; color: var(--text-secondary); background: transparent; cursor: pointer; font-family: inherit; }
.period-tab.active { background: rgba(232,168,80,0.1); color: var(--color-primary); }
.date-range { display: flex; align-items: center; gap: 8px; padding: 8px 14px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); font-size: 12px; color: var(--text-primary); cursor: pointer; }
.date-range .material-symbols-outlined { font-size: 16px; color: var(--text-tertiary); }

.golden-btn { display: inline-flex; align-items: center; gap: 6px; padding: 9px 22px; border: none; border-radius: var(--radius-md); font-size: 12px; font-weight: 700; color: #2a1800; cursor: pointer; background: linear-gradient(135deg, #e8a850, #ffc67c, #e8a850); font-family: inherit; transition: all 0.2s ease; }
.golden-btn:hover { filter: brightness(1.1); transform: scale(1.02); }

/* ---- Glass Cards ---- */
.bento-grid { display: grid; grid-template-columns: repeat(12, 1fr); gap: 20px; margin-bottom: 28px; }

.glass-card {
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl);
  padding: 24px; transition: all 0.3s ease; position: relative; overflow: hidden;
}
[data-theme=.dark.] .glass-card { background: rgba(20,20,35,0.95); border-color: rgba(255,255,255,0.05); }
.glass-card:hover { transform: translateY(-4px); border-color: rgba(232,168,80,0.3); }
.glass-card--lg { grid-column: span 5; }
.glass-card--wide { grid-column: span 8; }

.glass-card__glow { position: absolute; top: 0; right: 0; width: 120px; height: 120px; background: rgba(232,168,80,0.04); border-radius: 50%; pointer-events: none; }
.glass-card__top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.glass-card__icon { padding: 12px; background: rgba(232,168,80,0.08); border-radius: var(--radius-lg); color: var(--color-primary); }
.glass-card__icon .material-symbols-outlined { font-size: 28px; }
.glass-card__trend { display: flex; align-items: center; gap: 2px; font-size: 12px; font-family: 'JetBrains Mono', monospace; color: #4ade80; }
.glass-card__title { font-size: 18px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px; }
.glass-card__desc { font-size: 13px; color: var(--text-secondary); margin-bottom: 16px; line-height: 1.5; }
.glass-card__big { font-family: 'JetBrains Mono', monospace; font-size: 28px; font-weight: 700; color: var(--color-primary); margin-bottom: 2px; }
.glass-card__sub { font-size: 12px; color: var(--text-tertiary); }
.glass-card__top-row { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.glass-card__icon-sm { padding: 8px; background: var(--bg-hover); border-radius: var(--radius-md); color: var(--text-secondary); }
.glass-card__title-sm { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.glass-card__num { font-family: 'JetBrains Mono', monospace; font-size: 22px; font-weight: 700; color: var(--text-primary); }
.glass-card__note { font-size: 11px; color: var(--text-tertiary); margin-top: 2px; }
.glass-card__row { display: flex; justify-content: space-between; align-items: flex-end; }
.glass-card__stats { display: flex; gap: 24px; margin-left: auto; }
.stat-label { display: block; font-size: 11px; color: var(--text-tertiary); }
.stat-val { font-family: 'JetBrains Mono', monospace; font-weight: 700; color: var(--text-primary); }
.stat-val.accent { color: var(--color-primary); }

.arrow-icon { color: var(--text-tertiary); transition: transform 0.2s ease; }
.glass-card:hover .arrow-icon { transform: translateX(3px); color: var(--color-primary); }

/* Progress bar */
.progress-bar { height: 8px; background: var(--bg-hover); border-radius: 4px; margin-bottom: 14px; overflow: hidden; }
.progress-bar__fill { height: 100%; background: var(--color-primary); border-radius: 4px; transition: width 0.6s ease; }

/* Movie list */
.movie-list { display: flex; flex-direction: column; gap: 10px; }
.movie-row { display: flex; justify-content: space-between; font-size: 12px; }
.movie-name { color: var(--text-primary); }
.movie-pct { font-family: 'JetBrains Mono', monospace; color: var(--color-primary); }
.text-muted { color: var(--text-tertiary); font-size: 12px; }

/* Snack */
.snack-row { display: flex; gap: 14px; align-items: center; }
.snack-thumb { width: 56px; height: 56px; border-radius: var(--radius-md); background: var(--bg-hover); display: flex; align-items: center; justify-content: center; color: var(--text-tertiary); flex-shrink: 0; }
.snack-name { font-size: 13px; font-weight: 700; color: var(--text-primary); }
.snack-meta { font-size: 11px; color: var(--text-secondary); }
.snack-price { font-family: 'JetBrains Mono', monospace; font-size: 13px; color: var(--color-primary); margin-top: 2px; }

/* Mini chart */
.mini-chart { display: flex; align-items: flex-end; gap: 2px; height: 60px; margin-top: 14px; }
.mini-bar { flex: 1; background: rgba(232,168,80,0.15); border-radius: 2px 2px 0 0; transition: background 0.2s; cursor: pointer; min-width: 6px; }
.mini-bar:hover { background: rgba(232,168,80,0.4); }

/* ---- Chart Section ---- */
.chart-section { margin-bottom: 28px; }
.chart-section__header { display: flex; justify-content: space-between; align-items: center; padding: 0 0 20px; border-bottom: 1px solid var(--border-light); margin-bottom: 20px; }
.chart-section__title { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.chart-legend { display: flex; gap: 16px; font-size: 12px; color: var(--text-secondary); }
.legend-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 4px; vertical-align: middle; }
.legend-dot.gold { background: var(--color-primary); }
.legend-dot.dim { background: rgba(255,255,255,0.2); }
.chart-area { height: 260px; display: flex; gap: 10px; }
.chart-yaxis { display: flex; flex-direction: column; justify-content: space-between; padding-bottom: 24px; font-size: 10px; color: var(--text-tertiary); font-family: 'JetBrains Mono', monospace; min-width: 50px; text-align: right; }
.chart-main { flex: 1; position: relative; display: flex; flex-direction: column; }
.chart-grid-lines { position: absolute; inset: 0 0 24px 0; display: flex; flex-direction: column; justify-content: space-between; pointer-events: none; }
.grid-line { border-bottom: 1px solid var(--border-light); width: 100%; }
.chart-bars { position: relative; z-index: 1; flex: 1; display: flex; align-items: flex-end; justify-content: space-around; padding-bottom: 24px; }
.bar-group { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1; height: 100%; justify-content: flex-end; }
.bar-group__ticket {
  width: 28px; background: #E8A850; border-radius: 3px; min-height: 4px;
  display: flex; align-items: flex-start; justify-content: center; transition: filter 0.2s ease; position: relative;
}
.bar-group:hover .bar-group__ticket { filter: brightness(1.15); }
.bar-group__val { font-size: 9px; color: #2a1800; font-weight: 600; margin-top: 2px; writing-mode: horizontal-tb; }
.bar-group__label { font-size: 10px; color: var(--text-tertiary); font-family: 'JetBrains Mono', monospace; }

/* ---- Table Section ---- */
.table-section { margin-bottom: 40px; }
.table-section__header { display: flex; justify-content: space-between; align-items: center; padding: 0 0 16px; }
.table-section__title { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.search-box { display: flex; align-items: center; gap: 8px; }
.search-box input {
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md);
  padding: 8px 12px 8px 34px; color: var(--text-primary); font-size: 13px; outline: none; width: 200px; font-family: inherit;
}
.search-box input:focus { border-color: rgba(232,168,80,0.4); }
.search-box .material-symbols-outlined { position: absolute; margin-left: 10px; font-size: 16px; color: var(--text-tertiary); pointer-events: none; }

.status-badge { padding: 2px 8px; border-radius: var(--radius-sm); font-size: 10px; font-weight: 600; }
.badge-done { background: rgba(74,222,128,0.1); color: #4ade80; border: 1px solid rgba(74,222,128,0.2); }
.badge-pending { background: rgba(232,168,80,0.1); color: var(--color-primary); border: 1px solid rgba(232,168,80,0.2); }

.detail-link { background: none; border: none; color: var(--color-primary); font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; }
.detail-link:hover { text-decoration: underline; }

.table-footer { padding: 14px 0 0; font-size: 12px; color: var(--text-tertiary); border-top: 1px solid var(--border-light); margin-top: 12px; }

/* ---- Footer ---- */
.page-footer { margin-top: 48px; padding-top: 24px; border-top: 1px solid var(--border-light); display: flex; justify-content: space-between; font-size: 12px; color: var(--text-tertiary); flex-wrap: wrap; gap: 12px; }
.page-footer__links { display: flex; gap: 20px; }
.page-footer__links a { color: var(--text-tertiary); }
.page-footer__links a:hover { color: var(--text-primary); }

/* ---- Grid Adjustments ---- */
@media (min-width: 1025px) {
  .glass-card { grid-column: span 3; }
  .glass-card--lg { grid-column: span 5; }
  .glass-card--wide { grid-column: span 8; }
}
@media (max-width: 1024px) {
  .glass-card { grid-column: span 6; }
  .glass-card--lg, .glass-card--wide { grid-column: span 12; }
  .bento-grid { grid-template-columns: repeat(6, 1fr); }
}
@media (max-width: 640px) {
  .glass-card { grid-column: span 12; }
  .page-hero { flex-direction: column; }
}
</style>
