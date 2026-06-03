<template>
  <div class="admin-layout">
    <NavBar />
    <div class="admin-content">
      <div class="dashboard-page">
        <!-- Welcome -->
        <div class="welcome-card">
          <div class="welcome-text">
            <h2>欢迎回来，{{ authStore.realName }}</h2>
            <p>{{ todayStr }} | {{ authStore.user?.roleName || '' }}</p>
          </div>
          <div class="welcome-stats">
            <div class="stat-mini">
              <el-icon :size="32" color="#67c23a"><Tickets /></el-icon>
              <div class="stat-mini-text">
                <span class="stat-mini-num">{{ todayOrders }}</span>
                <span class="stat-mini-label">今日订单</span>
              </div>
            </div>
            <div class="stat-mini">
              <el-icon :size="32" color="#409eff"><Money /></el-icon>
              <div class="stat-mini-text">
                <span class="stat-mini-num">¥{{ todayRevenue }}</span>
                <span class="stat-mini-label">今日收入</span>
              </div>
            </div>
            <div class="stat-mini">
              <el-icon :size="32" color="#e6a23c"><Film /></el-icon>
              <div class="stat-mini-text">
                <span class="stat-mini-num">{{ activeMovies }}</span>
                <span class="stat-mini-label">在映影片</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Quick stats -->
        <div class="stats-row">
          <el-row :gutter="16">
            <el-col :xs="12" :sm="6" v-for="stat in statsCards" :key="stat.label">
              <div class="stat-card">
                <div class="stat-icon" :style="{ background: stat.bg }">
                  <el-icon :size="24"><component :is="stat.icon" /></el-icon>
                </div>
                <div class="stat-info">
                  <span class="stat-value">{{ stat.value }}</span>
                  <span class="stat-label">{{ stat.label }}</span>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- Recent orders + Quick actions -->
        <el-row :gutter="16" style="margin-top: 16px;">
          <el-col :span="16">
            <div class="card">
              <div class="card-header">
                <h3>最近订单</h3>
                <el-button size="small" text @click="$router.push('/admin/orders')">查看全部</el-button>
              </div>
              <el-table :data="recentOrders" size="small" v-loading="loadingOrders">
                <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
                <el-table-column prop="movieName" label="影片" min-width="120" show-overflow-tooltip />
                <el-table-column label="金额" width="100">
                  <template #default="{ row }">¥{{ row.totalAmount || row.totalPrice }}</template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="statusType(row.status || row.orderStatus)" size="small">
                      {{ statusLabel(row.status || row.orderStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="时间" min-width="150">
                  <template #default="{ row }">{{ formatDateTime(row.createTime || row.createdAt) }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!loadingOrders && recentOrders.length === 0" description="暂无订单" :image-size="100" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="card">
              <div class="card-header"><h3>快捷操作</h3></div>
              <div class="quick-actions">
                <el-button
                  v-for="action in quickActions"
                  :key="action.path"
                  :type="action.type"
                  :icon="action.icon"
                  class="quick-btn"
                  @click="$router.push(action.path)"
                >
                  {{ action.label }}
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getAdminOrders } from '@/api/order'
import { getMovieList } from '@/api/movie'
import { getRevenueStats } from '@/api/statistics'
import { Tickets, Money, Film, VideoCameraFilled, List, Plus, TrendCharts } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

const router = useRouter()
const authStore = useAuthStore()

const todayOrders = ref(0)
const todayRevenue = ref(0)
const activeMovies = ref(0)
const recentOrders = ref([])
const loadingOrders = ref(true)

const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const statsCards = computed(() => [
  { icon: Tickets, value: todayOrders.value, label: '今日订单', bg: 'linear-gradient(135deg, #67c23a, #85ce61)' },
  { icon: Money, value: '$' + todayRevenue.value, label: '今日营收', bg: 'linear-gradient(135deg, #409eff, #66b1ff)' },
  { icon: Film, value: activeMovies.value, label: '在映影片', bg: 'linear-gradient(135deg, #e6a23c, #f0c674)' },
  { icon: TrendCharts, value: recentOrders.value.length, label: '最近订单', bg: 'linear-gradient(135deg, #f56c6c, #ff8585)' }
])

const quickActions = [
  { path: '/admin/movies', label: '影片管理', icon: VideoCameraFilled, type: 'primary' },
  { path: '/admin/schedules', label: '新增排片', icon: Plus, type: 'success' },
  { path: '/admin/orders', label: '订单管理', icon: List, type: 'warning' },
  { path: '/admin/statistics', label: '数据统计', icon: TrendCharts, type: 'danger' }
]

function statusLabel(status) {
  const map = { 0: '待支付', 1: '待观影', 2: '已完成', 3: '已改签', 4: '已退票', 5: '已过期' }
  return map[status] !== undefined ? map[status] : (status || '--')
}

function statusType(status) {
  const map = { 0: 'warning', 1: '', 2: 'success', 3: 'info', 4: 'danger', 5: 'info' }
  return map[status] || 'info'
}

function formatDateTime(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function fetchData() {
  try {
    const [orderResult, movieResult] = await Promise.allSettled([
      getAdminOrders({ size: 999 }),
      getMovieList()
    ])

    // 单独处理订单数据
    if (orderResult.status === 'fulfilled') {
      const allOrders = orderResult.value.data?.records || orderResult.value.data || []
      recentOrders.value = allOrders.slice(0, 10)

      // Calculate today's stats
      const today = new Date()
      const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
      const todayOrdersList = allOrders.filter(o => {
        const createDate = (o.createTime || o.createdAt || '').substring(0, 10)
        return createDate === todayStr
      })
      todayOrders.value = todayOrdersList.length
      // 只统计已支付(1)和已完成(2)的订单营收，排除退票(4)、过期(5)、待支付(0)、已改签(3)
      const paidOrders = todayOrdersList.filter(o => {
        const s = o.status ?? o.orderStatus
        return s === 1 || s === 2
      })
      const revenue = paidOrders.reduce((sum, o) => sum + (Number(o.totalAmount || o.totalPrice) || 0), 0)
      todayRevenue.value = isNaN(revenue) ? '0.00' : revenue.toFixed(2)
    }

    // 单独处理电影数据
    if (movieResult.status === 'fulfilled') {
      const movieList = movieResult.value.data?.records || movieResult.value.data || []
      activeMovies.value = movieList.filter(m => m.status === 1).length
    }
  } catch (err) {
    // unexpected fallback
  }
  loadingOrders.value = false
}

onMounted(fetchData)
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--bg-secondary);
}

.admin-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Welcome card */
.welcome-card {
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  border-radius: var(--radius-lg);
  padding: 28px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.welcome-text h2 {
  font-size: 22px;
  color: #fff;
  margin-bottom: 6px;
}

.welcome-text p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.welcome-stats {
  display: flex;
  gap: 24px;
}

.stat-mini {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-mini-text {
  display: flex;
  flex-direction: column;
}

.stat-mini-num {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.stat-mini-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

/* Stats row */
.stats-row {
  margin-bottom: 8px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-light);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* Cards */
.card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-light);
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

/* Quick actions */
.quick-actions {
  padding: 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.quick-btn {
  width: 100%;
  height: 48px;
  font-size: 14px;
}

@media (max-width: 768px) {
  .stats-row .el-col {
    margin-bottom: 12px;
  }
}
</style>
