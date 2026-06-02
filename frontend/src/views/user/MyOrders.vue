<template>
  <div class="my-orders-page">
    <NavBar />

    <div class="orders-container">
      <div class="page-header">
        <h2>
          <el-icon :size="22"><Tickets /></el-icon>
          我的订单
        </h2>
        <router-link to="/home" class="back-link">
          <el-icon><ArrowLeft /></el-icon>
          返回首页
        </router-link>
      </div>

      <!-- Status filter tabs -->
      <div class="filter-tabs">
        <el-radio-group v-model="filterStatus" size="default">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button :value="0">待支付</el-radio-button>
          <el-radio-button :value="1">待观影</el-radio-button>
          <el-radio-button :value="2">已完成</el-radio-button>
          <el-radio-button :value="3">已改签</el-radio-button>
          <el-radio-button :value="4">已退票</el-radio-button>
        </el-radio-group>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="loading-area">
        <el-skeleton v-for="i in 3" :key="i" animated style="margin-bottom: 16px;">
          <template #template>
            <div style="display: flex; gap: 16px; padding: 16px;">
              <el-skeleton-item variant="image" style="width: 80px; height: 106px;" />
              <div style="flex: 1;">
                <el-skeleton-item variant="text" style="width: 60%;" />
                <el-skeleton-item variant="text" style="width: 40%; margin-top: 8px;" />
                <el-skeleton-item variant="text" style="width: 30%; margin-top: 8px;" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>

      <!-- Empty -->
      <el-empty
        v-else-if="filteredOrders.length === 0"
        :description="filterStatus ? '暂无该类订单' : '暂无订单记录'"
        :image-size="160"
      >
        <el-button type="primary" @click="$router.push('/home')">去购票</el-button>
      </el-empty>

      <!-- Order list -->
      <div v-else class="order-list">
        <div
          v-for="order in filteredOrders"
          :key="order.id"
          class="order-card"
        >
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo || order.id }}</span>
            <el-tag
              :type="statusType(order.status || order.orderStatus)"
              size="small"
              effect="dark"
            >
              {{ statusLabel(order.status || order.orderStatus) }}
            </el-tag>
          </div>

          <div class="order-body">
            <div class="order-movie">
              <img
                :src="order.moviePoster || order.poster || defaultPoster"
                :alt="order.movieName"
                class="order-poster"
                @error="onImgError"
              />
              <div class="order-info">
                <h4>{{ order.movieName }}</h4>
                <p>{{ formatDateTime(order.scheduleStartTime || order.startTime) }}</p>
                <p>{{ order.hallName || '--' }} | {{ (order.seatNumbers || order.seats || []).join('、') }}</p>
              </div>
            </div>

            <div class="order-price">
              <span class="price">${{ order.totalAmount || order.totalPrice || '--' }}</span>
            </div>
          </div>

          <div class="order-actions">
            <el-button
              v-if="canReschedule(order)"
              size="small"
              type="warning"
              @click="openReschedule(order)"
            >
              改签
            </el-button>
            <el-button
              v-if="canRefund(order)"
              size="small"
              type="danger"
              @click="handleRefund(order)"
            >
              退票
            </el-button>
            <el-button
              size="small"
              @click="toggleDetail(order)"
            >
              {{ expandedId === order.id ? '收起详情' : '查看详情' }}
            </el-button>
          </div>

          <!-- Expanded detail -->
          <div v-if="expandedId === order.id" class="order-detail">
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="订单编号">{{ order.orderNo || order.id }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ statusLabel(order.status || order.orderStatus) }}</el-descriptions-item>
              <el-descriptions-item label="影片">{{ order.movieName }}</el-descriptions-item>
              <el-descriptions-item label="影厅">{{ order.hallName }}</el-descriptions-item>
              <el-descriptions-item label="场次时间">{{ formatDateTime(order.scheduleStartTime || order.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="座位">{{ (order.seatNumbers || order.seats || []).join('、') }}</el-descriptions-item>
              <el-descriptions-item label="金额">${{ order.totalAmount || order.totalPrice }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(order.createTime || order.createdAt) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </div>
    </div>

    <!-- Reschedule Dialog -->
    <el-dialog v-model="rescheduleVisible" title="改签" width="500px" :close-on-click-modal="false">
      <div v-if="rescheduleOrder">
        <p class="reschedule-hint">
          当前场次：{{ rescheduleOrder.movieName }} - {{ formatDateTime(rescheduleOrder.scheduleStartTime || rescheduleOrder.startTime) }}
        </p>
        <el-divider />
        <el-form label-width="80px">
          <el-form-item label="选择场次">
            <el-select v-model="rescheduleScheduleId" placeholder="请选择新场次" style="width: 100%">
              <el-option
                v-for="sch in rescheduleSchedules"
                :key="sch.id"
                :label="`${formatDateTime(sch.startTime)} - ${sch.hallName || sch.hall?.name} (${sch.availableSeats || sch.availableCount || 0}座)`"
                :value="sch.id"
                :disabled="(sch.availableSeats || sch.availableCount) <= 0"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="选择座位">
            <el-select
              v-model="rescheduleSeats"
              multiple
              placeholder="请选择新座位"
              style="width: 100%"
              :disabled="!rescheduleScheduleId"
            >
              <el-option
                v-for="seat in rescheduleSeatOptions"
                :key="seat.key"
                :label="seat.key"
                :value="seat.key"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="rescheduleVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!rescheduleScheduleId || rescheduleSeats.length === 0"
          :loading="rescheduling"
          @click="doReschedule"
        >
          确认改签
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getMyOrders, getOrderDetail, refundOrder, rescheduleOrder as rescheduleOrderApi, getScheduleSeats, getSchedulesByMovie } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Tickets, ArrowLeft } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

const router = useRouter()

const orders = ref([])
const loading = ref(true)
const filterStatus = ref('')
const expandedId = ref(null)
const rescheduleVisible = ref(false)
const rescheduleOrder = ref(null)
const rescheduleScheduleId = ref(null)
const rescheduleSchedules = ref([])
const rescheduleSeats = ref([])
const rescheduleSeatOptions = ref([])
const rescheduling = ref(false)

const defaultPoster = 'data:image/svg+xml,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="80" height="106" viewBox="0 0 80 106">
  <rect fill="#1a1a2e" width="80" height="106"/>
  <text fill="#7a8096" font-family="Arial" font-size="8" text-anchor="middle" x="40" y="56">暂无海报</text>
</svg>
`)

function onImgError(e) {
  e.target.src = defaultPoster
}

const filteredOrders = computed(() => {
  if (filterStatus.value === '' || filterStatus.value === null) return orders.value
  return orders.value.filter(o =>
    (o.status ?? o.orderStatus) === filterStatus.value
  )
})

function statusLabel(status) {
  const map = {
    0: '待支付',
    1: '待观影',
    2: '已完成',
    3: '已改签',
    4: '已退票',
    5: '已过期'
  }
  return map[status] !== undefined ? map[status] : (status || '--')
}

function statusType(status) {
  const map = {
    0: 'warning',
    1: '',
    2: 'success',
    3: 'info',
    4: 'danger',
    5: 'info'
  }
  return map[status] || 'info'
}

function canReschedule(order) {
  const status = order.status ?? order.orderStatus
  if (status !== 1) return false
  const startTime = new Date(order.scheduleStartTime || order.startTime)
  return startTime > new Date()
}

function canRefund(order) {
  const status = order.status ?? order.orderStatus
  if (status !== 1) return false
  const startTime = new Date(order.scheduleStartTime || order.startTime)
  return startTime > new Date()
}

function formatDateTime(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function toggleDetail(order) {
  expandedId.value = expandedId.value === order.id ? null : order.id
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getMyOrders()
    const list = res.data?.records || res.data || []
    orders.value = list.map(o => ({
      ...o,
      id: o.id || o.orderId
    }))
  } catch (err) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function handleRefund(order) {
  try {
    await ElMessageBox.confirm(
      '确定要退票吗？退款将原路返回。',
      '退票确认',
      { confirmButtonText: '确定退票', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  try {
    await refundOrder(order.id)
    ElMessage.success('退票成功')
    fetchOrders()
  } catch (err) { /* handled */ }
}

async function openReschedule(order) {
  rescheduleOrder.value = order
  rescheduleScheduleId.value = null
  rescheduleSeats.value = []
  rescheduleSeatOptions.value = []

  // Fetch schedules for the same movie
  try {
    const res = await getSchedulesByMovie(order.movieId)
    rescheduleSchedules.value = (res.data || []).filter(s => s.id !== order.scheduleId)
  } catch (err) {
    rescheduleSchedules.value = []
  }
  rescheduleVisible.value = true
}

// Watch schedule selection to load available seats
watch(rescheduleScheduleId, async (scheduleId) => {
  rescheduleSeats.value = []
  rescheduleSeatOptions.value = []
  if (!scheduleId) return

  try {
    const res = await getScheduleSeats(scheduleId)
    const seats = res.data?.seats || res.data || []
    if (Array.isArray(seats) && seats.length > 0 && Array.isArray(seats[0])) {
      // 2D seat matrix - flatten it
      rescheduleSeatOptions.value = seats.flat().map(seat => ({
        key: seat.seatNumber || `${seat.seatRow}-${seat.seatCol}`,
        ...seat
      })).filter(s => s.status === 0)
    } else if (Array.isArray(seats)) {
      // Flat seat list
      rescheduleSeatOptions.value = seats.map(seat => ({
        key: seat.seatNumber || `${seat.seatRow}-${seat.seatCol}`,
        ...seat
      })).filter(s => s.status === 0)
    }
  } catch (err) {
    rescheduleSeatOptions.value = []
  }
})

async function doReschedule() {
  if (!rescheduleOrder.value || !rescheduleScheduleId.value) return

  rescheduling.value = true
  try {
    await rescheduleOrderApi({
      orderId: rescheduleOrder.value.id,
      newScheduleId: rescheduleScheduleId.value,
      newSeatNumbers: rescheduleSeats.value
    })
    ElMessage.success('改签成功')
    rescheduleVisible.value = false
    fetchOrders()
  } catch (err) { /* handled */ }
  finally {
    rescheduling.value = false
  }
}

onMounted(fetchOrders)
</script>

<style scoped>
.my-orders-page {
  min-height: 100vh;
  background: var(--bg-secondary);
}

.orders-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-muted);
}

.back-link:hover {
  color: var(--color-primary);
}

.filter-tabs {
  margin-bottom: 20px;
  overflow-x: auto;
  white-space: nowrap;
}

.loading-area {
  padding: 16px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-light);
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-light);
}

.order-no {
  font-size: 13px;
  color: var(--text-muted);
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
}

.order-movie {
  display: flex;
  gap: 12px;
  align-items: center;
}

.order-poster {
  width: 80px;
  height: 106px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.order-info h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.order-info p {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 2px;
}

.order-price .price {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-accent);
}

.order-actions {
  display: flex;
  gap: 8px;
  padding: 8px 16px 12px;
  justify-content: flex-end;
}

.order-detail {
  padding: 0 16px 16px;
}

.reschedule-hint {
  font-size: 13px;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .order-body {
    flex-direction: column;
    gap: 12px;
  }
  .filter-tabs :deep(.el-radio-button__inner) {
    padding: 6px 10px;
    font-size: 13px;
  }
}
</style>
