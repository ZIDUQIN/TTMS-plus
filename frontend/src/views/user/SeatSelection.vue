<template>
  <div class="seat-selection-page">
    <NavBar />

    <div class="booking-container" v-loading="pageLoading">
      <template v-if="schedule">
        <!-- Left: Seat grid -->
        <div class="seat-area">
          <div class="seat-panel">
            <h3 class="panel-title">选择座位</h3>

            <div v-if="seatLoading" class="seat-loading">
              <el-skeleton animated />
            </div>

            <SeatGrid
              v-else-if="seatDataReady"
              :rowCount="rowCount"
              :colCount="colCount"
              :seatStatusMap="seatStatusMap"
              :selectedSeats="selectedSeats"
              :maxSelect="6"
              @select-seat="selectSeat"
              @deselect-seat="deselectSeat"
            />
            <el-empty v-else description="座位信息加载失败" :image-size="80" />
          </div>
        </div>

        <!-- Right: Info sidebar -->
        <div class="info-sidebar">
          <!-- Movie info -->
          <div class="sidebar-card">
            <div class="movie-summary">
              <img
                :src="movie?.poster || movie?.posterUrl || defaultPoster"
                :alt="movie?.name"
                class="summary-poster"
                @error="onImgError"
              />
              <div class="summary-info">
                <h4>{{ movie?.name || '--' }}</h4>
                <p>{{ movie?.genre || '' }} {{ movie?.duration ? movie.duration + '分钟' : '' }}</p>
              </div>
            </div>
          </div>

          <!-- Schedule info -->
          <div class="sidebar-card">
            <h4 class="card-title">场次信息</h4>
            <div class="info-row">
              <span class="info-label">日期：</span>
              <span>{{ formatDate(schedule.startTime) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">时间：</span>
              <span>{{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">影厅：</span>
              <span>{{ schedule.hallName || schedule.hall?.name || '--' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">票价：</span>
              <span class="price-highlight">${{ schedule.price || (movie && movie.price) || '--' }}</span>
            </div>
          </div>

          <!-- Selected seats -->
          <div class="sidebar-card">
            <h4 class="card-title">
              已选座位
              <span class="seat-count-badge">{{ selectedSeats.length }}/6</span>
            </h4>
            <div v-if="selectedSeats.length === 0" class="no-seats">
              请点击左侧座位图选择座位
            </div>
            <div v-else class="selected-list">
              <el-tag
                v-for="seat in selectedSeats"
                :key="seat"
                closable
                type="warning"
                size="large"
                @close="deselectSeat(seat)"
                class="seat-tag"
              >
                {{ seat }}
              </el-tag>
            </div>
            <div v-if="selectedSeats.length > 0" class="price-summary">
              <span>合计：</span>
              <span class="total-price">${{ totalPrice }}</span>
            </div>
          </div>

          <!-- Confirm button -->
          <el-button
            type="primary"
            size="large"
            class="confirm-btn"
            :disabled="selectedSeats.length === 0"
            :loading="submitting"
            @click="handleConfirm"
          >
            {{ selectedSeats.length > 0 ? `确认选座 (${selectedSeats.length}张)` : '请选择座位' }}
          </el-button>
        </div>
      </template>

      <el-empty v-else-if="!pageLoading" description="场次信息不存在" :image-size="200" />
    </div>

    <!-- Payment Dialog -->
    <el-dialog
      v-model="paymentVisible"
      title="确认支付"
      width="420px"
      :close-on-click-modal="false"
      center
    >
      <div class="payment-content">
        <div class="payment-movie">{{ movie?.name }}</div>
        <div class="payment-info">
          <div class="pay-row">
            <span>场次时间</span>
            <span>{{ schedule ? formatDateTime(schedule.startTime) : '' }}</span>
          </div>
          <div class="pay-row">
            <span>影厅</span>
            <span>{{ schedule?.hallName || schedule?.hall?.name || '--' }}</span>
          </div>
          <div class="pay-row">
            <span>座位</span>
            <span>{{ selectedSeats.join('、') }}</span>
          </div>
          <div class="pay-row">
            <span>数量</span>
            <span>{{ selectedSeats.length }} 张</span>
          </div>
          <div class="pay-row pay-total">
            <span>应付金额</span>
            <span class="pay-price">${{ totalPrice }}</span>
          </div>
        </div>
        <div class="payment-methods">
          <div class="pay-method active">
            <el-icon :size="24"><Wallet /></el-icon>
            <span>模拟支付</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="paymentVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="handlePay">
          确认支付 ${{ totalPrice }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getScheduleSeats, createOrder, payOrder } from '@/api/order'
import { getMovieDetail } from '@/api/movie'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'
import SeatGrid from '@/components/SeatGrid.vue'

const route = useRoute()
const router = useRouter()

const schedule = ref(null)
const movie = ref(null)
const seatStatusMap = ref({})
const selectedSeats = ref([])
const pageLoading = ref(true)
const seatLoading = ref(true)
const submitting = ref(false)
const paying = ref(false)
const paymentVisible = ref(false)
const currentOrderId = ref(null)

const defaultPoster = 'data:image/svg+xml,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="120" height="160" viewBox="0 0 120 160">
  <rect fill="#1a1a2e" width="120" height="160"/>
  <text fill="#7a8096" font-family="Arial" font-size="10" text-anchor="middle" x="60" y="85">暂无海报</text>
</svg>
`)

function onImgError(e) {
  e.target.src = defaultPoster
}

const rowCount = computed(() => schedule.value?.hallRowCount || schedule.value?.rowCount || 8)
const colCount = computed(() => schedule.value?.hallColCount || schedule.value?.colCount || 12)

const seatDataReady = computed(() => {
  return rowCount.value > 0 && colCount.value > 0
})

const unitPrice = computed(() => {
  return schedule.value?.price || movie.value?.price || 0
})

const totalPrice = computed(() => {
  return (unitPrice.value * selectedSeats.value.length).toFixed(2)
})

function selectSeat(seatKey) {
  if (selectedSeats.value.length >= 6) {
    ElMessage.warning('最多只能选择6个座位')
    return
  }
  if (!selectedSeats.value.includes(seatKey)) {
    selectedSeats.value.push(seatKey)
  }
}

function deselectSeat(seatKey) {
  selectedSeats.value = selectedSeats.value.filter(s => s !== seatKey)
}

function formatDate(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

function formatTime(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatDateTime(dateStr) {
  return `${formatDate(dateStr)} ${formatTime(dateStr)}`
}

async function fetchScheduleDetail() {
  const scheduleId = route.params.scheduleId
  if (!scheduleId) return

  pageLoading.value = true
  seatLoading.value = true

  try {
    const res = await getScheduleSeats(scheduleId)
    // The API response structure may vary; try to handle common patterns
    const data = res.data
    schedule.value = {
      id: data.id || data.scheduleId || scheduleId,
      startTime: data.startTime,
      endTime: data.endTime,
      hallName: data.hallName || data.hall?.name,
      hallRowCount: data.rowCount || data.hall?.rowCount || 8,
      hallColCount: data.colCount || data.hall?.colCount || 12,
      price: data.price,
      movieId: data.movieId
    }

    // Parse seat status
    if (data.seats) {
      // seats could be array of objects or a map
      if (Array.isArray(data.seats)) {
        data.seats.forEach(seat => {
          const key = `${seat.seatRow || seat.row}-${String(seat.seatCol || seat.col).padStart(2, '0')}`
          seatStatusMap.value[key] = seat.status || 'AVAILABLE'
        })
      } else if (typeof data.seats === 'object') {
        seatStatusMap.value = data.seats
      }
    } else if (data.seatStatusMap) {
      seatStatusMap.value = data.seatStatusMap
    }

    // Fetch movie detail
    if (schedule.value.movieId) {
      try {
        const movieRes = await getMovieDetail(schedule.value.movieId)
        movie.value = movieRes.data
      } catch (e) {
        // ignore
      }
    }
  } catch (err) {
    schedule.value = null
  } finally {
    pageLoading.value = false
    seatLoading.value = false
  }
}

async function handleConfirm() {
  if (selectedSeats.value.length === 0) return

  submitting.value = true
  try {
    const scheduleId = route.params.scheduleId
    const orderRes = await createOrder({
      scheduleId: Number(scheduleId),
      seatNumbers: [...selectedSeats.value]
    })

    currentOrderId.value = orderRes.data?.id || orderRes.data?.orderId
    paymentVisible.value = true
  } catch (err) {
    // handle error
  } finally {
    submitting.value = false
  }
}

async function handlePay() {
  if (!currentOrderId.value) return

  paying.value = true
  try {
    await payOrder(currentOrderId.value)
    paymentVisible.value = false
    ElMessage.success('支付成功！')
    router.push('/my-orders')
  } catch (err) {
    // handle error
  } finally {
    paying.value = false
  }
}

onMounted(fetchScheduleDetail)
</script>

<style scoped>
.seat-selection-page {
  min-height: 100vh;
  background: var(--bg-secondary);
}

.booking-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.seat-area {
  flex: 1;
  min-width: 0;
}

.seat-panel {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-light);
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.seat-loading {
  padding: 40px;
}

/* Sidebar */
.info-sidebar {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-light);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.seat-count-badge {
  font-size: 12px;
  background: var(--color-primary);
  color: #fff;
  padding: 2px 8px;
  border-radius: 10px;
}

.movie-summary {
  display: flex;
  gap: 12px;
}

.summary-poster {
  width: 80px;
  height: 106px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.summary-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.summary-info p {
  font-size: 12px;
  color: var(--text-muted);
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px solid var(--border-light);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: var(--text-muted);
}

.price-highlight {
  color: var(--color-accent);
  font-weight: 700;
  font-size: 16px;
}

.no-seats {
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
  padding: 12px 0;
}

.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.seat-tag {
  font-weight: 600;
}

.price-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
  font-size: 14px;
  color: var(--text-secondary);
}

.total-price {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-accent);
}

.confirm-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
}

/* Payment dialog */
.payment-content {
  padding: 8px 0;
}

.payment-movie {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
  text-align: center;
}

.payment-info {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  margin-bottom: 16px;
}

.pay-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.pay-total {
  border-top: 1px dashed var(--border-color);
  margin-top: 6px;
  padding-top: 10px;
  font-weight: 700;
  color: var(--text-primary);
}

.pay-price {
  color: var(--color-accent);
  font-size: 20px;
}

.payment-methods {
  display: flex;
  gap: 12px;
}

.pay-method {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 2px solid var(--border-color);
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.pay-method.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(64, 158, 255, 0.05);
}

@media (max-width: 768px) {
  .booking-container {
    flex-direction: column;
  }
  .info-sidebar {
    width: 100%;
  }
}
</style>
