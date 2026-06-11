<template>
  <div class="admin-layout">
    <div class="admin-content box-office-page">
      <!-- ========== 顶部区域 ========== -->
      <div class="bo-top-bar">
        <div class="bo-top-left">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :clearable="false"
            class="bo-date-picker"
            @change="onDateChange"
          />
          <div class="bo-tabs">
            <span class="bo-tab active">电影票房</span>
          </div>
        </div>
        <div class="bo-top-right">
          <el-button size="small" @click="toggleFullscreen">
            <el-icon><FullScreen /></el-icon>
            <span>全屏</span>
          </el-button>
          <el-button size="small" @click="exitFullscreen">
            <el-icon><CopyDocument /></el-icon>
            <span>返回常规版</span>
          </el-button>
        </div>
      </div>

      <!-- ========== 主体：左右分栏 ========== -->
      <div class="bo-main" v-loading="loading">
        <!-- ===== 左侧：电影票房榜单区 ===== -->
        <div class="bo-left">
          <div class="bo-section-header">
            <h2 class="bo-section-title">电影票房</h2>
            <div class="bo-type-switch">
              <el-button
                :type="boxOfficeType === 'comprehensive' ? 'primary' : ''"
                size="small"
                @click="switchType('comprehensive')"
              >
                综合票房
              </el-button>
              <el-button
                :type="boxOfficeType === 'share' ? 'primary' : ''"
                size="small"
                @click="switchType('share')"
              >
                分账票房
              </el-button>
            </div>
            <span class="bo-section-hint">影片 (点击 ★ 切换右侧展示)</span>
          </div>

          <!-- 数据表格 -->
          <el-table
            :data="rankingData"
            stripe
            highlight-current-row
            class="bo-table"
            @row-click="onRowClick"
          >
            <el-table-column label="影片" min-width="240">
              <template #default="{ row }">
                <div class="movie-cell">
                  <span class="movie-rank">{{ row.rank }}</span>
                  <div class="movie-info">
                    <span class="movie-name">{{ row.movieName }}</span>
                    <span class="movie-meta">上映{{ row.daysSinceRelease }}天 累计¥{{ row.cumulativeBoxOffice }}</span>
                  </div>
                  <span
                    class="movie-star"
                    :class="{ 'is-active': selectedMovieId === row.movieId }"
                    @click.stop="selectMovie(row)"
                  >★</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="综合票房" width="130" align="right">
              <template #default="{ row }">
                <span class="box-office-value">¥{{ row.boxOffice }}</span>
              </template>
            </el-table-column>

            <el-table-column label="票房占比" width="110" align="right">
              <template #default="{ row }">
                <span>{{ row.boxOfficeRatio }}%</span>
              </template>
            </el-table-column>

            <el-table-column label="排片场次" width="110" align="right">
              <template #default="{ row }">
                <span>{{ row.scheduleCount }}</span>
              </template>
            </el-table-column>

            <el-table-column label="排片占比" width="110" align="right">
              <template #default="{ row }">
                <span>{{ row.scheduleRatio }}%</span>
              </template>
            </el-table-column>

            <el-table-column label="场均人次" width="110" align="right">
              <template #default="{ row }">
                <span>{{ row.avgAttendance }}</span>
              </template>
            </el-table-column>

            <el-table-column label="上座率" width="100" align="right">
              <template #default="{ row }">
                <span>{{ row.occupancyRate }}%</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- ===== 右侧：双卡片布局 ===== -->
        <div class="bo-right">
          <!-- 大盘卡片 -->
          <div class="card bo-card bo-dashboard-card">
            <div class="card-header"><h3>实时大盘</h3></div>
            <div class="card-body">
              <div class="dashboard-main">
                <span class="dashboard-label">大盘总票房</span>
                <span class="dashboard-value accent">¥{{ dashboard.totalBoxOffice }}</span>
              </div>
              <div class="dashboard-sub">
                <div class="dashboard-sub-item">
                  <span class="sub-label">总出票</span>
                  <span class="sub-value">{{ dashboard.totalTickets }}张</span>
                </div>
                <div class="dashboard-sub-item">
                  <span class="sub-label">总场次</span>
                  <span class="sub-value">{{ dashboard.totalSchedules }}场</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 影片详情卡片 -->
          <div class="card bo-card bo-movie-card">
            <div class="card-body">
              <!-- 影片信息 -->
              <div class="movie-detail-header" v-if="selectedMovie">
                <div class="movie-poster">
                  <img
                    v-if="selectedMovie.posterUrl"
                    :src="selectedMovie.posterUrl"
                    :alt="selectedMovie.movieName"
                    class="poster-img"
                    @error="onPosterError"
                  />
                  <el-icon v-else :size="48"><VideoCameraFilled /></el-icon>
                </div>
                <div class="movie-detail-info">
                  <h3 class="movie-detail-name">{{ selectedMovie.movieName }} <span class="star-icon">★</span></h3>
                  <p class="movie-detail-genre">{{ formatGenre(selectedMovie.genre) }}</p>
                  <p class="movie-detail-meta">上映{{ selectedMovie.daysSinceRelease }}天 累计¥{{ selectedMovie.cumulativeBoxOffice }}</p>
                </div>
              </div>
              <div class="movie-detail-header" v-else>
                <div class="movie-poster empty-poster">
                  <el-icon :size="48"><VideoCameraFilled /></el-icon>
                </div>
                <div class="movie-detail-info">
                  <p class="no-movie-hint">点击左侧影片 ★ 查看详情</p>
                </div>
              </div>

              <!-- 核心数据 -->
              <div class="movie-stats" v-if="selectedMovie">
                <div class="stat-row">
                  <span class="stat-label">综合票房</span>
                  <span class="stat-value accent">¥{{ selectedMovie.boxOffice }}</span>
                </div>
                <div class="stat-row">
                  <span class="stat-label">票房占比</span>
                  <span class="stat-value accent">{{ selectedMovie.boxOfficeRatio }}%</span>
                </div>
                <div class="stat-row">
                  <span class="stat-label">排片场次</span>
                  <span class="stat-value">{{ selectedMovie.scheduleCount }}</span>
                </div>
                <div class="stat-row">
                  <span class="stat-label">排片占比</span>
                  <span class="stat-value">{{ selectedMovie.scheduleRatio }}%</span>
                </div>
                <div class="stat-row">
                  <span class="stat-label">场均人次</span>
                  <span class="stat-value">{{ selectedMovie.avgAttendance }}</span>
                </div>
                <div class="stat-row">
                  <span class="stat-label">上座率</span>
                  <span class="stat-value">{{ selectedMovie.occupancyRate }}%</span>
                </div>
              </div>
              <div class="movie-stats" v-else>
                <div class="stat-row" v-for="label in statLabels" :key="label">
                  <span class="stat-label">{{ label }}</span>
                  <span class="stat-value">--</span>
                </div>
              </div>

              <!-- 趋势图（纯CSS柱状图，无ECharts依赖） -->
              <div class="trend-section" v-if="trendData.length > 0">
                <h4 class="trend-title">近日票房趋势</h4>
                <div class="trend-chart">
                  <div class="trend-bar-item" v-for="item in trendData" :key="item.date">
                    <div class="trend-bar-value">¥{{ item.revenue }}</div>
                    <div
                      class="trend-bar"
                      :style="{
                        height: getBarHeight(item.revenue) + 'px',
                        background: getBarGradient(item.date)
                      }"
                    ></div>
                    <div class="trend-bar-label">{{ formatDateDisplay(item.date) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import {
  getBoxOfficeRanking,
  getBoxOfficeDashboard,
  getBoxOfficeMovieTrend
} from '@/api/boxOffice'
import { FullScreen, CopyDocument, VideoCameraFilled } from '@element-plus/icons-vue'

// ========== 持久化 key ==========
const STORAGE_KEY_DATE = 'boxoffice_dateRange'
const STORAGE_KEY_TYPE = 'boxoffice_type'

function loadDateRange() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_DATE)
    if (raw) {
      const arr = JSON.parse(raw)
      if (Array.isArray(arr) && arr.length === 2 && arr[0] && arr[1]) {
        return arr
      }
    }
  } catch {}
  const t = getTodayStr()
  return [t, t]
}

function saveDateRange(range) {
  try {
    localStorage.setItem(STORAGE_KEY_DATE, JSON.stringify(range))
  } catch {}
}

// ========== 状态 ==========
const dateRange = ref(loadDateRange())
const boxOfficeType = ref(localStorage.getItem(STORAGE_KEY_TYPE) || 'comprehensive')
const rankingData = ref([])
const dashboard = ref({ totalBoxOffice: 0, totalTickets: 0, totalSchedules: 0 })
const selectedMovieId = ref(null)
const selectedMovie = ref(null)
const trendData = ref([])
const loading = ref(false)
let refreshTimer = null

const statLabels = ['综合票房', '票房占比', '排片场次', '排片占比', '场均人次', '上座率']

// ========== 工具函数 ==========
function getTodayStr() {
  const d = new Date()
  return d.getFullYear() + '-' +
    String(d.getMonth() + 1).padStart(2, '0') + '-' +
    String(d.getDate()).padStart(2, '0')
}

function formatDateDisplay(dateStr) {
  if (!dateStr) return ''
  const p = dateStr.split('-')
  return (p[1] || '') + '-' + (p[2] || '')
}

function formatGenre(genre) {
  if (!genre) return ''
  return genre.replace(/,/g, '、')
}

function onPosterError(e) {
  e.target.style.display = 'none'
}

function getBarHeight(value) {
  const max = Math.max(...trendData.value.map(d => d.revenue), 1)
  return Math.max((value / max) * 120, 4)
}

function getBarGradient(date) {
  return date === dateRange.value[1]
    ? 'linear-gradient(to top, #ff6b35, #ff8c5a)'
    : 'linear-gradient(to top, #c0c4cc, #dcdfe6)'
}

// ========== 数据请求 ==========
async function fetchAllData() {
  loading.value = true
  const [startDate, endDate] = dateRange.value
  const type = boxOfficeType.value

  try {
    const [rankRes, dashRes] = await Promise.allSettled([
      getBoxOfficeRanking(startDate, endDate, type),
      getBoxOfficeDashboard(startDate, endDate, type)
    ])

    if (rankRes.status === 'fulfilled' && rankRes.value && rankRes.value.data) {
      rankingData.value = rankRes.value.data
      if (rankingData.value.length > 0) {
        if (!selectedMovieId.value || !rankingData.value.find(r => r.movieId === selectedMovieId.value)) {
          setSelectedMovie(rankingData.value[0], false)
        } else {
          const cur = rankingData.value.find(r => r.movieId === selectedMovieId.value)
          if (cur) setSelectedMovie(cur, false)
        }
      }
    }

    if (dashRes.status === 'fulfilled' && dashRes.value && dashRes.value.data) {
      dashboard.value = dashRes.value.data
    }
  } catch (e) {
    console.error('fetchAllData error:', e)
  } finally {
    loading.value = false
    if (selectedMovieId.value) {
      try {
        const trendRes = await getBoxOfficeMovieTrend(selectedMovieId.value, endDate, type, 7)
        if (trendRes && trendRes.data) {
          trendData.value = trendRes.data
        }
      } catch (e) {
        console.error('fetchTrend error:', e)
      }
    }
  }
}

function setSelectedMovie(row, fetchTrendData) {
  if (!row) return
  selectedMovieId.value = row.movieId
  selectedMovie.value = { ...row }
  if (fetchTrendData !== false) {
    fetchTrend()
  }
}

function selectMovie(row) { setSelectedMovie(row, true) }
function onRowClick(row) { selectMovie(row) }

async function fetchTrend() {
  if (!selectedMovieId.value) return
  try {
    const endDate = dateRange.value[1]
    const res = await getBoxOfficeMovieTrend(selectedMovieId.value, endDate, boxOfficeType.value, 7)
    if (res && res.data) {
      trendData.value = res.data
    }
  } catch (e) {
    console.error('fetchTrend error:', e)
  }
}

function switchType(type) {
  if (boxOfficeType.value === type) return
  boxOfficeType.value = type
  localStorage.setItem(STORAGE_KEY_TYPE, type)
  fetchAllData()
}

function onDateChange() {
  saveDateRange(dateRange.value)
  fetchAllData()
}

// ========== 定时器 ==========
function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(fetchAllData, 60000)
}

function stopAutoRefresh() {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
}

function toggleFullscreen() {
  const el = document.querySelector('.box-office-page')
  if (!el) return
  if (document.fullscreenElement) {
    document.exitFullscreen().catch(() => {})
  } else {
    el.requestFullscreen().catch(() => {})
  }
}

function exitFullscreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen().catch(() => {})
  }
}

onMounted(() => {
  fetchAllData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.admin-content { max-width: 1600px; margin: 0 auto; padding: 16px 24px; }

/* 顶部栏 */
.bo-top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.bo-top-left { display: flex; align-items: center; gap: 16px; }
.bo-date-picker { width: 260px; }
.bo-tabs { display: flex; }
.bo-tab { padding: 6px 16px; font-size: 14px; font-weight: 500; color: var(--text-secondary); cursor: pointer; border-bottom: 2px solid transparent; }
.bo-tab.active { color: var(--color-primary); border-bottom-color: var(--color-primary); }
.bo-top-right { display: flex; gap: 8px; }

/* 主体 */
.bo-main { display: flex; gap: 16px; align-items: flex-start; }
.bo-left { flex: 1; min-width: 0; }
.bo-right { width: 360px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; }

/* 区域头部 */
.bo-section-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.bo-section-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.bo-type-switch { display: flex; }
.bo-type-switch .el-button:first-child { border-radius: var(--radius-sm) 0 0 var(--radius-sm); }
.bo-type-switch .el-button:last-child { border-radius: 0 var(--radius-sm) var(--radius-sm) 0; }
.bo-section-hint { font-size: 12px; color: var(--text-muted); margin-left: auto; }

/* 表格 */
.bo-table { background: var(--bg-card); border-radius: var(--radius-md); overflow: hidden; }
.bo-table :deep(.el-table__header th) { background: var(--bg-secondary); }
.bo-table :deep(.el-table__row) { cursor: pointer; }
.bo-table :deep(.el-table__row.current-row) { background-color: rgba(255, 107, 53, 0.06) !important; }
.bo-table :deep(.el-table__row.current-row td) { background-color: transparent !important; }

/* 影片列 */
.movie-cell { display: flex; align-items: center; gap: 10px; padding: 4px 0; }
.movie-rank { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; color: var(--text-muted); flex-shrink: 0; background: var(--bg-secondary); border-radius: 4px; }
.movie-info { display: flex; flex-direction: column; min-width: 0; }
.movie-name { font-size: 14px; font-weight: 500; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.movie-meta { font-size: 11px; color: var(--text-muted); }
.movie-star { font-size: 20px; color: #ccc; cursor: pointer; flex-shrink: 0; margin-left: auto; transition: color 0.2s; }
.movie-star:hover, .movie-star.is-active { color: #f5a623; }

/* 重点数据 */
.box-office-value { color: var(--color-accent); font-weight: 700; font-size: 15px; }

/* 卡片 */
.card { background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-light); }
.card-header { padding: 14px 20px; border-bottom: 1px solid var(--border-light); }
.card-header h3 { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.card-body { padding: 16px 20px; }

/* 大盘卡片 */
.bo-dashboard-card .card-body { display: flex; flex-direction: column; gap: 16px; }
.dashboard-main { display: flex; flex-direction: column; gap: 4px; }
.dashboard-label { font-size: 13px; color: var(--text-muted); }
.dashboard-value { font-size: 36px; font-weight: 700; line-height: 1.2; }
.dashboard-value.accent { color: var(--color-accent); }
.dashboard-sub { display: flex; gap: 32px; }
.dashboard-sub-item { display: flex; flex-direction: column; gap: 4px; }
.sub-label { font-size: 12px; color: var(--text-muted); }
.sub-value { font-size: 16px; font-weight: 600; color: var(--text-primary); }

/* 影片详情卡片 */
.movie-detail-header { display: flex; gap: 14px; margin-bottom: 14px; }
.movie-poster { width: 80px; height: 112px; border-radius: var(--radius-sm); overflow: hidden; flex-shrink: 0; background: var(--bg-secondary); display: flex; align-items: center; justify-content: center; color: var(--text-muted); }
.poster-img { width: 100%; height: 100%; object-fit: cover; }
.movie-detail-info { display: flex; flex-direction: column; justify-content: center; gap: 4px; }
.movie-detail-name { font-size: 18px; font-weight: 700; color: var(--text-primary); }
.star-icon { color: #f5a623; font-size: 16px; }
.movie-detail-genre { font-size: 13px; color: var(--text-muted); }
.movie-detail-meta { font-size: 12px; color: var(--text-muted); }
.no-movie-hint { font-size: 14px; color: var(--text-muted); }

/* 核心数据 */
.movie-stats { display: flex; flex-direction: column; gap: 8px; padding: 12px 0; border-top: 1px solid var(--border-light); margin-bottom: 12px; }
.stat-row { display: flex; justify-content: space-between; align-items: center; }
.stat-label { font-size: 13px; color: var(--text-muted); }
.stat-value { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.stat-value.accent { color: var(--color-accent); }

/* 纯CSS趋势图 */
.trend-section { border-top: 1px solid var(--border-light); padding-top: 12px; }
.trend-title { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 10px; }
.trend-chart { display: flex; align-items: flex-end; justify-content: space-around; height: 160px; gap: 4px; }
.trend-bar-item { display: flex; flex-direction: column; align-items: center; flex: 1; height: 100%; justify-content: flex-end; gap: 4px; }
.trend-bar-value { font-size: 10px; color: var(--text-muted); white-space: nowrap; }
.trend-bar { width: 100%; max-width: 48px; border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.3s; }
.trend-bar-label { font-size: 10px; color: var(--text-muted); margin-top: 4px; }

/* 响应式 */
@media (max-width: 1200px) {
  .bo-main { flex-direction: column; }
  .bo-right { width: 100%; flex-direction: row; flex-wrap: wrap; }
  .bo-right .bo-card { flex: 1; min-width: 300px; }
}
</style>
