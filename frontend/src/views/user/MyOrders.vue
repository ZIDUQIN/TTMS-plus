<template>
  <div class="my-orders-page">
    <div class="orders-container">
      <!-- Page Header -->
      <div class="page-header">
        <h1 class="page-title">
          <span class="material-symbols-outlined title-icon">confirmation_number</span>
          我的订单
        </h1>
        <router-link to="/home" class="back-link">
          <span class="material-symbols-outlined">arrow_back</span>
          <span>返回首页</span>
        </router-link>
      </div>

      <!-- Tab Bar -->
      <div class="tab-bar">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          class="tab-btn"
          :class="{ active: filterStatus === tab.value }"
          @click="filterStatus = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="loading-area">
        <div v-for="i in 3" :key="i" class="skeleton-card">
          <div class="skeleton-poster"></div>
          <div class="skeleton-body">
            <div class="skeleton-line w-60"></div>
            <div class="skeleton-line w-40"></div>
            <div class="skeleton-line w-30"></div>
          </div>
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="filteredOrders.length === 0" class="empty-state">
        <span class="material-symbols-outlined empty-icon">theaters</span>
        <p>{{ filterStatus ? '暂无该类订单' : '暂无订单记录' }}</p>
        <el-button type="primary" @click="$router.push('/home')">去购票</el-button>
      </div>

      <!-- Ticket Card List -->
      <div v-else class="ticket-list">
        <div
          v-for="order in filteredOrders"
          :key="order.id"
          class="ticket-card"
          :class="{
            'ticket-card--expired': isExpired(order),
            'ticket-card--completed': isCompleted(order)
          }"
        >
          <!-- Left: Poster -->
          <div class="ticket-poster">
            <img
              :src="order.moviePoster || defaultPoster"
              :alt="order.movieName"
              @error="onImgError"
            />
          </div>

          <!-- Center: Info -->
          <div class="ticket-body" @click="toggleDetail(order)">
            <div class="ticket-top">
              <div class="ticket-info">
                <h3 class="ticket-movie-name">{{ order.movieName }}</h3>
                <div class="ticket-meta">
                  <span class="meta-item">
                    <span class="material-symbols-outlined">location_on</span>
                    {{ order.hallName || '--' }}
                  </span>
                  <span class="meta-item">
                    <span class="material-symbols-outlined">event</span>
                    {{ formatDate(order.scheduleStartTime || order.startTime) }}
                  </span>
                </div>
              </div>
              <span class="status-badge" :class="'status-badge--' + statusColor(order)">
                {{ statusLabel(order.status ?? order.orderStatus) }}
              </span>
            </div>

            <div class="ticket-bottom">
              <div class="ticket-seats">
                座位：<strong>{{ formatSeats(order) }}</strong>
              </div>
              <div class="ticket-price">
                <span class="price-label">实付金额</span>
                <span class="price-value">¥{{ order.totalAmount || order.totalPrice || '--' }}</span>
              </div>
            </div>
          </div>

          <!-- Right: Action arrow -->
          <div class="ticket-arrow" @click.stop="goToTicket(order)">
            <span class="material-symbols-outlined">arrow_forward_ios</span>
          </div>

          <!-- Expanded Detail Panel -->
          <div v-if="expandedId === order.id" class="ticket-detail">
            <dl class="detail-grid">
              <div class="detail-item"><dt>订单编号</dt><dd>{{ order.orderNo || order.id }}</dd></div>
              <div class="detail-item"><dt>状态</dt><dd>{{ statusLabel(order.status ?? order.orderStatus) }}</dd></div>
              <div class="detail-item"><dt>影片</dt><dd>{{ order.movieName }}</dd></div>
              <div class="detail-item"><dt>影厅</dt><dd>{{ order.hallName }}</dd></div>
              <div class="detail-item"><dt>场次时间</dt><dd>{{ formatDateTime(order.scheduleStartTime || order.startTime) }}</dd></div>
              <div class="detail-item"><dt>座位</dt><dd>{{ formatSeats(order) }}</dd></div>
              <div class="detail-item"><dt>金额</dt><dd class="detail-price">¥{{ order.totalAmount || order.totalPrice }}</dd></div>
              <div class="detail-item"><dt>创建时间</dt><dd>{{ formatDateTime(order.createTime || order.createdAt) }}</dd></div>
            </dl>

            <div class="detail-actions">
              <el-button v-if="canPay(order)" size="small" type="primary" :loading="payingOrderId === order.id" @click.stop="handlePay(order)">
                <span class="material-symbols-outlined">payment</span>
                支付
              </el-button>
              <el-button v-if="canCancel(order)" size="small" @click.stop="handleCancel(order)">
                取消订单
              </el-button>
              <el-button v-if="canReschedule(order)" size="small" @click.stop="openReschedule(order)">
                <span class="material-symbols-outlined">swap_horiz</span>
                改签
              </el-button>
              <el-button v-if="canRefund(order)" size="small" type="danger" @click.stop="handleRefund(order)">
                <span class="material-symbols-outlined">undo</span>
                退票
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="!loading && filteredOrders.length > 0" class="pagination-bar">
        <button class="page-arrow" disabled>
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
        <div class="page-numbers">
          <button class="page-num active">1</button>
        </div>
        <button class="page-arrow">
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
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
        <el-button type="primary" :disabled="!rescheduleScheduleId || rescheduleSeats.length === 0"
          :loading="rescheduling" @click="doReschedule">
          确认改签
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getMyOrders, refundOrder, cancelOrder, payOrder, rescheduleOrder as rescheduleOrderApi, getScheduleSeats, getSchedulesByMovie } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

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
const payingOrderId = ref(null)

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 0 },
  { label: '待观影', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已改签', value: 3 },
  { label: '已退票', value: 4 },
]

const defaultPoster = 'data:image/svg+xml,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="80" height="106" viewBox="0 0 80 106">
  <rect fill="#1a1a2e" width="80" height="106"/>
  <text fill="#7a8096" font-family="Arial" font-size="8" text-anchor="middle" x="40" y="56">暂无海报</text>
</svg>`)

function formatSeats(order) {
  const seats = order.seatNumbers || order.seats
  if (!seats) return '--'
  if (Array.isArray(seats)) return seats.join('、')
  if (typeof seats === 'string') return seats.split(',').join('、')
  return String(seats)
}

function onImgError(e) { e.target.src = defaultPoster }

const filteredOrders = computed(() => {
  if (filterStatus.value === '' || filterStatus.value === null) return orders.value
  return orders.value.filter(o => (o.status ?? o.orderStatus) === Number(filterStatus.value))
})

function statusLabel(status) {
  const map = { 0: '待支付', 1: '待观影', 2: '已完成', 3: '已改签', 4: '已退票', 5: '已过期' }
  return map[status] ?? '--'
}

function statusColor(order) {
  const status = order.status ?? order.orderStatus
  const map = { 0: 'warning', 1: 'active', 2: 'success', 3: 'info', 4: 'danger', 5: 'expired' }
  return map[status] ?? 'info'
}

function isExpired(order) {
  const status = order.status ?? order.orderStatus
  return status === 5 || status === 4
}

function isCompleted(order) {
  const status = order.status ?? order.orderStatus
  return status === 2
}

function canReschedule(order) { return (order.status ?? order.orderStatus) === 1 && new Date(order.scheduleStartTime || order.startTime) > new Date() }
function canPay(order) { return (order.status ?? order.orderStatus) === 0 && new Date(order.scheduleStartTime || order.startTime) > new Date() }
function canCancel(order) { return (order.status ?? order.orderStatus) === 0 && new Date(order.scheduleStartTime || order.startTime) > new Date() }
function canRefund(order) { return (order.status ?? order.orderStatus) === 1 && new Date(order.scheduleStartTime || order.startTime) > new Date() }

function formatDate(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function formatDateTime(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function toggleDetail(order) {
  expandedId.value = expandedId.value === order.id ? null : order.id
}

function goToTicket(order) {
  router.push(`/ticket/${order.id}`)
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getMyOrders()
    const list = res.data?.records || res.data || []
    orders.value = list.map(o => ({ ...o, id: o.id || o.orderId }))
  } catch { orders.value = [] }
  finally { loading.value = false }
}

async function handleRefund(order) {
  try { await ElMessageBox.confirm('确定要退票吗？退款将原路返回。', '退票确认', { confirmButtonText: '确定退票', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await refundOrder(order.id); ElMessage.success('退票成功'); fetchOrders() } catch {}
}

async function handleCancel(order) {
  try { await ElMessageBox.confirm('确定要取消该订单吗？座位将立即释放。', '取消订单', { confirmButtonText: '确定取消', cancelButtonText: '返回', type: 'warning' }) }
  catch { return }
  try { await cancelOrder(order.id); ElMessage.success('订单已取消'); fetchOrders() } catch {}
}

async function handlePay(order) {
  try { await ElMessageBox.confirm(`确认支付 ¥${order.totalAmount || order.totalPrice || '--'}？`, '确认支付', { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  payingOrderId.value = order.id
  try { await payOrder(order.id); ElMessage.success('支付成功'); fetchOrders() } catch {}
  finally { payingOrderId.value = null }
}

async function openReschedule(order) {
  rescheduleOrder.value = order
  rescheduleScheduleId.value = null
  rescheduleSeats.value = []
  rescheduleSeatOptions.value = []
  try { const res = await getSchedulesByMovie(order.movieId); rescheduleSchedules.value = (res.data || []).filter(s => s.id !== order.scheduleId) }
  catch { rescheduleSchedules.value = [] }
  rescheduleVisible.value = true
}

watch(rescheduleScheduleId, async (scheduleId) => {
  rescheduleSeats.value = []; rescheduleSeatOptions.value = []
  if (!scheduleId) return
  try {
    const res = await getScheduleSeats(scheduleId)
    const seats = res.data?.seats || res.data || []
    const flat = Array.isArray(seats) && seats.length > 0 && Array.isArray(seats[0]) ? seats.flat() : seats
    rescheduleSeatOptions.value = (Array.isArray(flat) ? flat : []).map(s => ({
      key: s.seatNumber || `${s.seatRow}-${s.seatCol}`, ...s
    })).filter(s => s.status === 0)
  } catch { rescheduleSeatOptions.value = [] }
})

async function doReschedule() {
  if (!rescheduleOrder.value || !rescheduleScheduleId.value) return
  rescheduling.value = true
  try {
    await rescheduleOrderApi({ orderId: rescheduleOrder.value.id, newScheduleId: rescheduleScheduleId.value, newSeatNumbers: rescheduleSeats.value })
    ElMessage.success('改签成功'); rescheduleVisible.value = false; fetchOrders()
  } catch {} finally { rescheduling.value = false }
}

onMounted(fetchOrders)
</script>

<style scoped>
/* ============================================================
   My Orders — Cinema Ticket Card Edition
   ============================================================ */

.my-orders-page {
  min-height: 100vh;
  background: var(--bg-primary);
}

.orders-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* ---- Page Header ---- */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.page-title {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 32px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 28px;
  color: var(--color-primary);
  font-variation-settings: 'FILL' 1;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-light);
  transition: all 0.2s ease;
}

.back-link:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.back-link .material-symbols-outlined { font-size: 16px; }

/* ---- Tab Bar ---- */
.tab-bar {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 28px;
  overflow-x: auto;
}

.tab-btn {
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
  font-family: inherit;
  margin-bottom: -1px;
}

.tab-btn:hover { color: var(--color-primary); }

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 600;
}

/* ---- Loading Skeleton ---- */
.loading-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-card {
  display: flex;
  gap: 16px;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: 16px;
  border: 1px solid var(--border-light);
}

.skeleton-poster {
  width: 96px;
  height: 128px;
  border-radius: var(--radius-md);
  background: var(--bg-hover);
  flex-shrink: 0;
}

.skeleton-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 8px;
}

.skeleton-line {
  height: 14px;
  border-radius: 4px;
  background: var(--bg-hover);
}

.skeleton-line.w-60 { width: 60%; }
.skeleton-line.w-40 { width: 40%; }
.skeleton-line.w-30 { width: 30%; }

/* ---- Empty State ---- */
.empty-state {
  text-align: center;
  padding: 64px 24px;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 56px;
  color: var(--text-tertiary);
  margin-bottom: 16px;
  display: block;
}

.empty-state p {
  font-size: 15px;
  margin-bottom: 20px;
}

/* ============================================================
   TICKET CARD — Core Design
   ============================================================ */
.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ticket-card {
  display: flex;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-light);
  overflow: hidden;
  position: relative;
  transition: all 0.3s ease;
  flex-wrap: wrap;
}

.ticket-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-1px);
}

/* Ticket cutout holes */
.ticket-card::before,
.ticket-card::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 18px;
  height: 18px;
  background: var(--bg-primary);
  border-radius: 50%;
  transform: translateY(-50%);
  z-index: 2;
}

.ticket-card::before {
  left: -9px;
  border-right: 1px solid var(--border-light);
}

.ticket-card::after {
  right: 71px;
  border-left: 1px solid var(--border-light);
}

/* ---- Ticket States ---- */
.ticket-card--expired {
  opacity: 0.7;
}

.ticket-card--expired .ticket-poster img {
  filter: grayscale(0.4);
}

.ticket-card--expired .ticket-movie-name,
.ticket-card--expired .price-value {
  opacity: 0.55;
}

/* ---- Poster ---- */
.ticket-poster {
  width: 110px;
  min-height: 148px;
  flex-shrink: 0;
  overflow: hidden;
}

.ticket-poster img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.ticket-card:hover .ticket-poster img {
  transform: scale(1.05);
}

/* ---- Body ---- */
.ticket-body {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  min-width: 0;
}

.ticket-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.ticket-info {
  min-width: 0;
}

.ticket-movie-name {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  line-height: 1.3;
}

.ticket-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-item .material-symbols-outlined {
  font-size: 16px;
  color: var(--text-tertiary);
}

/* ---- Status Badge ---- */
.status-badge {
  flex-shrink: 0;
  padding: 4px 14px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.status-badge--warning {
  background: rgba(245, 166, 35, 0.12);
  color: var(--color-warning);
}

.status-badge--active {
  background: rgba(232, 168, 80, 0.12);
  color: var(--color-primary);
}

.status-badge--success {
  background: rgba(45, 207, 138, 0.1);
  color: var(--color-emerald);
}

.status-badge--info {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.status-badge--danger {
  background: rgba(232, 64, 64, 0.1);
  color: var(--color-danger);
}

.status-badge--expired {
  background: rgba(128, 128, 128, 0.1);
  color: var(--text-tertiary);
}

/* ---- Ticket Bottom ---- */
.ticket-bottom {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: 16px;
}

.ticket-seats {
  font-size: 13px;
  color: var(--text-secondary);
}

.ticket-seats strong {
  color: var(--text-primary);
  font-weight: 600;
}

.ticket-price {
  text-align: right;
}

.price-label {
  display: block;
  font-size: 11px;
  color: var(--text-tertiary);
  margin-bottom: 2px;
}

.price-value {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
}

/* ---- Right Arrow ---- */
.ticket-arrow {
  width: 56px;
  border-left: 1px dashed var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  cursor: pointer;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.ticket-arrow:hover {
  background: var(--bg-hover);
}

.ticket-arrow .material-symbols-outlined {
  font-size: 18px;
  color: var(--text-tertiary);
  transition: transform 0.2s ease;
}

.ticket-card:hover .ticket-arrow .material-symbols-outlined {
  transform: translateX(3px);
  color: var(--color-primary);
}

/* ---- Detail Panel ---- */
.ticket-detail {
  width: 100%;
  padding: 0 20px 20px;
  border-top: 1px solid var(--border-light);
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 24px;
  padding: 16px 0;
}

.detail-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
}

.detail-item dt {
  color: var(--text-tertiary);
  min-width: 60px;
  flex-shrink: 0;
}

.detail-item dd {
  color: var(--text-primary);
  font-weight: 500;
}

.detail-price {
  color: var(--color-primary) !important;
  font-weight: 700 !important;
  font-size: 15px !important;
}

.detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 4px;
}

.detail-actions .el-button {
  border-radius: var(--radius-pill);
  font-size: 12px;
}

/* ---- Pagination ---- */
.pagination-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 40px;
}

.page-arrow {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-secondary);
}

.page-arrow:hover:not(:disabled) {
  background: rgba(232, 168, 80, 0.08);
  border-color: var(--color-primary);
}

.page-arrow:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-arrow .material-symbols-outlined { font-size: 18px; }

.page-numbers {
  display: flex;
  gap: 6px;
}

.page-num {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-num:hover {
  background: var(--bg-hover);
}

.page-num.active {
  background: var(--color-primary);
  color: #fff;
}

[data-theme='dark'] .page-num.active { color: #1A1814; }

/* ---- Reschedule Dialog ---- */
.reschedule-hint {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .ticket-card::after { right: 47px; }
  .ticket-card::before, .ticket-card::after { width: 14px; height: 14px; }
  .ticket-card::before { left: -7px; }
  .ticket-poster { width: 80px; min-height: 108px; }
  .ticket-body { padding: 14px; }
  .ticket-movie-name { font-size: 16px; }
  .ticket-meta { gap: 8px; }
  .ticket-arrow { width: 40px; }
  .ticket-bottom { flex-direction: column; align-items: flex-start; gap: 8px; }
  .ticket-price { text-align: left; }
  .tab-btn { padding: 10px 14px; font-size: 13px; }
  .detail-grid { grid-template-columns: 1fr; }
  .page-title { font-size: 24px; }
}
</style>
