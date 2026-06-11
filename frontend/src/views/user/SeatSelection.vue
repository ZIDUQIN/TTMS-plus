<template>
  <div class="seat-page">
    <!-- Top Header Bar -->
    <header class="seat-topbar">
      <div class="stb-left">
        <router-link to="/home" class="stb-back">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回选片</span>
        </router-link>
        <div class="stb-divider"></div>
        <div class="stb-info" v-if="schedule">
          <h1>{{ movie?.name || '影片' }}</h1>
          <p>{{ schedule.hallName }} <span>•</span> {{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}</p>
        </div>
      </div>
      <div class="stb-right">
        <p class="stb-step">Step 02 / 03</p>
        <p class="stb-step-title">选座购票</p>
        <div class="stb-seat-icon"><el-icon :size="20"><Tickets /></el-icon></div>
      </div>
    </header>

    <div class="seat-main" v-loading="pageLoading">
      <template v-if="schedule">
        <!-- Left: Movie Info + Legend -->
        <div class="seat-left">
          <div class="glass-card movie-card">
            <div class="mc-poster">
              <img :src="movie?.poster || movie?.posterUrl || defaultPoster" @error="onImgError" />
            </div>
            <div class="mc-info" v-if="schedule">
              <h1>{{ movie?.name || '影片' }}</h1>
              <p>{{ schedule.hallName }}</p>
              <div class="mc-time">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}</span>
              </div>
            </div>
          </div>

          <div class="glass-card legend-card">
            <div class="legend-grid">
              <div class="legend-item"><div class="lg-box available"></div><span>可选</span></div>
              <div class="legend-item"><div class="lg-box selected"></div><span class="text-gold">已选</span></div>
              <div class="legend-item"><div class="lg-box sold"></div><span>已售</span></div>
              <div class="legend-item"><div class="lg-box vip"></div><span>VIP</span></div>
            </div>
          </div>
        </div>

        <!-- Center: 3D Seat Grid -->
        <div class="seat-center">
          <!-- Screen -->
          <div class="screen-area">
            <div class="screen-bloom"></div>
            <div class="screen-curve"></div>
            <p class="screen-label">银 幕 SCREEN</p>
          </div>

          <!-- 3D Perspective Grid -->
          <div class="perspective-wrap">
            <div v-if="seatLoading" style="padding:60px"><el-skeleton animated /></div>
            <div v-else-if="seatDataReady" class="theater-grid">
              <template v-for="r in rowCount" :key="r">
                <div v-for="c in colCount" :key="`${r}-${c}`"
                  :class="['seat-cell', seatClass(r, c)]"
                  :title="seatKey(r, c)"
                  @click="toggleSeat(r, c)">
                  <span v-if="isSold(r, c)">×</span>
                </div>
              </template>
            </div>
            <el-empty v-else description="座位加载失败" :image-size="60" />
          </div>
        </div>

        <!-- Right: Order Summary -->
        <div class="seat-right">
          <div class="glass-card order-card">
            <div class="oc-header">
              <el-icon><Tickets /></el-icon>
              <h3>订单摘要</h3>
            </div>

            <div class="oc-seats">
              <span class="oc-label">已选座位</span>
              <div class="oc-tags">
                <span v-if="selectedSeats.length === 0" class="oc-empty">未选择座位</span>
                <el-tag v-for="s in selectedSeats" :key="s" closable type="warning" size="small" @close="deselectSeat(s)">{{ s }}</el-tag>
              </div>
            </div>

            <div class="oc-prices">
              <div class="oc-row">
                <span>票价</span>
                <span>¥{{ unitPrice }} × {{ selectedSeats.length }}</span>
              </div>
              <div class="oc-row">
                <span>服务费</span>
                <span>¥1.50</span>
              </div>
              <div class="oc-total">
                <span>合计</span>
                <span>¥{{ totalPrice }}</span>
              </div>
            </div>

            <button class="oc-confirm" :disabled="selectedSeats.length === 0 || submitting" @click="handleConfirm">
              {{ selectedSeats.length > 0 ? `确认选座 (${selectedSeats.length}张)` : '请选择座位' }}
              <el-icon><ArrowRight /></el-icon>
            </button>
            <p class="oc-note">开场前30分钟停止退票，请提前15分钟入场。</p>
          </div>
        </div>
      </template>
      <el-empty v-else-if="!pageLoading" description="场次信息不存在" :image-size="150" />
    </div>

    <!-- Payment Dialog -->
    <el-dialog v-model="paymentVisible" title="确认支付" width="440px" :close-on-click-modal="false" center>
      <div class="pay-body">
        <div class="pay-movie">{{ movie?.name }}</div>
        <div class="pay-info">
          <div class="pay-row"><span>时间</span><span>{{ schedule ? formatDateTime(schedule.startTime) : '' }}</span></div>
          <div class="pay-row"><span>影厅</span><span>{{ schedule?.hallName || '--' }}</span></div>
          <div class="pay-row"><span>座位</span><span>{{ selectedSeats.join('、') }}</span></div>
          <div class="pay-row"><span>数量</span><span>{{ selectedSeats.length }} 张</span></div>
          <div class="pay-row pay-total"><span>应付</span><span class="pay-price">¥{{ finalPrice }}</span></div>
        </div>
        <div class="pay-method">模拟支付</div>
      </div>
      <template #footer>
        <el-button @click="paymentVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="handlePay">确认支付 ¥{{ finalPrice }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getScheduleSeats, createOrder, payOrder } from '@/api/order'
import { getMovieDetail } from '@/api/movie'
import { getMyCoupons } from '@/api/coupon'
import { ElMessage } from 'element-plus'
import { Clock, Tickets, ArrowRight, ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute(); const router = useRouter()
const schedule = ref(null); const movie = ref(null)
const seatStatusMap = ref({})
const selectedSeats = ref([]); const pageLoading = ref(true); const seatLoading = ref(true)
const submitting = ref(false); const paying = ref(false)
const paymentVisible = ref(false); const currentOrderId = ref(null)
const availableCoupons = ref([]); const selectedCoupon = ref(null); const showCouponPicker = ref(false)

const defaultPoster = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="120" height="160"><rect fill="#1a1a2e" width="120" height="160"/><text fill="#7a8096" font-family="Arial" font-size="10" text-anchor="middle" x="60" y="85">暂无</text></svg>')
function onImgError(e) { e.target.src = defaultPoster }

const rowCount = computed(() => schedule.value?.hallRowCount || 8)
const colCount = computed(() => schedule.value?.hallColCount || 12)
const seatDataReady = computed(() => rowCount.value > 0 && colCount.value > 0)
const unitPrice = computed(() => schedule.value?.price || movie.value?.price || 0)
const totalPrice = computed(() => (unitPrice.value * selectedSeats.value.length).toFixed(2))

const couponDiscount = computed(() => {
  if (!selectedCoupon.value) return 0
  const v = parseFloat(selectedCoupon.value.value || 0)
  const t = parseFloat(totalPrice.value)
  return selectedCoupon.value.type === 'FIXED' ? Math.min(v, t) : parseFloat((t * v).toFixed(2))
})
const finalPrice = computed(() => Math.max(0, parseFloat(totalPrice.value) - couponDiscount.value).toFixed(2))

function seatKey(r, c) { return `${String.fromCharCode(64 + r)}-${String(c).padStart(2, '0')}` }
function isSold(r, c) { return seatStatusMap.value[seatKey(r, c)] === 'OCCUPIED' }
function isLocked(r, c) { return seatStatusMap.value[seatKey(r, c)] === 'LOCKED' }
function isAisle(r, c) { return seatStatusMap.value[seatKey(r, c)] === 'AISLE' }
function isVip(r, c) { return r >= 5 && r <= 7 && c >= 4 && c <= 9 }
function isSelected(r, c) { return selectedSeats.value.includes(seatKey(r, c)) }

function seatClass(r, c) {
  const s = seatStatusMap.value[seatKey(r, c)]
  if (isSelected(r, c)) return 'selected'
  if (s === 'OCCUPIED') return 'sold'
  if (s === 'LOCKED') return 'locked'
  if (s === 'AISLE') return 'aisle'
  if (isVip(r, c)) return 'vip'
  return 'available'
}

function toggleSeat(r, c) {
  const key = seatKey(r, c)
  if (isSold(r, c) || isLocked(r, c) || isAisle(r, c)) return
  const idx = selectedSeats.value.indexOf(key)
  if (idx >= 0) { selectedSeats.value.splice(idx, 1) }
  else if (selectedSeats.value.length < 6) { selectedSeats.value.push(key) }
  else { ElMessage.warning('最多选择6个座位') }
}

function deselectSeat(key) { selectedSeats.value = selectedSeats.value.filter(s => s !== key) }
function formatTime(d) { if (!d) return '--'; const t = new Date(d); return `${String(t.getHours()).padStart(2,'0')}:${String(t.getMinutes()).padStart(2,'0')}` }
function formatDate(d) { if (!d) return '--'; const t = new Date(d); return `${t.getFullYear()}年${t.getMonth()+1}月${t.getDate()}日` }
function formatDateTime(d) { return formatDate(d) + ' ' + formatTime(d) }

async function fetchScheduleDetail() {
  const id = route.params.scheduleId; if (!id) return
  pageLoading.value = true; seatLoading.value = true
  try {
    const res = await getScheduleSeats(id); const data = res.data
    const sch = data.schedule || data
    schedule.value = { id: sch.id || id, startTime: sch.startTime, endTime: sch.endTime, hallName: sch.hallName || '', hallRowCount: data.rowCount || 8, hallColCount: data.colCount || 12, price: sch.price, movieId: sch.movieId }
    if (data.seats && Array.isArray(data.seats)) {
      data.seats.forEach(row => {
        if (Array.isArray(row)) {
          row.forEach(seat => {
            let key = seat.seatNumber
            if (!key) { key = `${String.fromCharCode(64 + (seat.seatRow || 1))}-${String(seat.seatCol || 1).padStart(2, '0')}` }
            const m = { 0: 'AVAILABLE', 1: 'LOCKED', 2: 'OCCUPIED', 3: 'AISLE' }
            seatStatusMap.value[key] = m[seat.status] || 'AISLE'
          })
        }
      })
    }
    if (schedule.value.movieId) { try { movie.value = (await getMovieDetail(schedule.value.movieId)).data } catch {} }
  } catch { schedule.value = null }
  pageLoading.value = false; seatLoading.value = false
}

async function loadCoupons() {
  try {
    const r = await getMyCoupons(); const now = new Date()
    availableCoupons.value = (r.data || []).filter(c => c.status === 0 && (!c.expireTime || new Date(c.expireTime) >= now))
  } catch { availableCoupons.value = [] }
}

watch(paymentVisible, v => { if (v) { selectedCoupon.value = null; showCouponPicker.value = false; loadCoupons() } })

async function handleConfirm() {
  if (!selectedSeats.value.length || submitting.value) return
  submitting.value = true
  try {
    const res = await createOrder({ scheduleId: Number(route.params.scheduleId), seatNumbers: [...selectedSeats.value] })
    currentOrderId.value = res.data?.id || res.data?.orderId
    if (!currentOrderId.value) { ElMessage.error('订单创建失败'); submitting.value = false; return }
    paymentVisible.value = true; submitting.value = false
  } catch { submitting.value = false }
}

async function handlePay() {
  if (!currentOrderId.value) return; paying.value = true
  try {
    await payOrder(currentOrderId.value)
    if (selectedCoupon.value) { try { const { useCoupon } = await import('@/api/coupon'); await useCoupon(selectedCoupon.value.id, currentOrderId.value) } catch {} }
    paymentVisible.value = false; ElMessage.success('支付成功'); router.push('/my-orders')
  } catch {}
  paying.value = false
}

onMounted(fetchScheduleDetail)
</script>

<style scoped>
.seat-page { min-height: 100vh; background: #0A0A10; }

/* Top Header */
.seat-topbar {
  position: sticky; top: 0; z-index: 100; display: flex; justify-content: space-between; align-items: center;
  padding: 14px 32px; background: rgba(20,20,31,0.85); backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.stb-left { display: flex; align-items: center; gap: 16px; }
.stb-back { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-secondary); transition: color 0.15s; }
.stb-back:hover { color: var(--color-primary); }
.stb-divider { width: 1px; height: 28px; background: rgba(255,255,255,0.1); }
.stb-info h1 { font-size: 17px; font-weight: 700; color: var(--text-primary); }
.stb-info p { font-size: 11px; color: var(--text-tertiary); letter-spacing: 1px; margin-top: 1px; }
.stb-right { text-align: right; display: flex; align-items: center; gap: 12px; }
.stb-step { font-size: 10px; color: var(--text-tertiary); letter-spacing: 0.5px; }
.stb-step-title { font-size: 12px; font-weight: 700; color: var(--color-primary); letter-spacing: 1px; }
.stb-seat-icon { width: 36px; height: 36px; border-radius: 50%; background: rgba(232,168,80,0.1); display: flex; align-items: center; justify-content: center; color: var(--color-primary); }

.seat-main { display: flex; gap: 20px; padding: 16px 32px; max-width: 1440px; margin: 0 auto; height: calc(100vh - 120px); align-items: stretch; }

/* Glass Card */
.glass-card { background: rgba(20,20,31,0.75); backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 20px; }

/* Left */
.seat-left { width: 260px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; }
.mc-poster { aspect-ratio: 2/3; border-radius: 8px; overflow: hidden; background: #1f1f24; }
.mc-poster img { width: 100%; height: 100%; object-fit: cover; }
.mc-info h1 { font-size: 20px; font-weight: 700; color: var(--color-primary); margin-top: 12px; }
.mc-info p { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.mc-time { display: flex; align-items: center; gap: 6px; margin-top: 10px; font-size: 14px; color: var(--color-primary); font-family: 'Consolas', monospace; }
.text-gold { color: var(--color-primary); }

.legend-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 11px; color: var(--text-tertiary); }
.lg-box { width: 22px; height: 22px; border-radius: 4px; }
.lg-box.available { border: 1px solid rgba(255,255,255,0.15); background: #1f1f24; }
.lg-box.selected { background: #E8A850; box-shadow: 0 0 10px rgba(232,168,80,0.5); }
.lg-box.sold { background: #34343a; display: flex; align-items: center; justify-content: center; color: #8F8D9A; font-size: 16px; }
.lg-box.sold::after { content: '×'; }
.lg-box.vip { border: 2px solid rgba(255,151,155,0.5); background: rgba(255,151,155,0.1); }

/* Center */
.seat-center { flex: 1; display: flex; flex-direction: column; align-items: center; min-width: 0; }
.screen-area { width: 100%; max-width: 600px; text-align: center; margin-bottom: 40px; position: relative; }
.screen-bloom { position: absolute; top: -20px; left: 50%; transform: translateX(-50%); width: 80%; height: 40px; background: linear-gradient(to bottom, rgba(232,168,80,0.2), transparent); filter: blur(16px); border-radius: 50%; }
.screen-curve { height: 4px; background: linear-gradient(90deg, transparent 5%, var(--color-primary) 20%, var(--color-primary) 80%, transparent 95%); border-radius: 50% / 100% 100% 0 0; box-shadow: 0 0 20px rgba(232,168,80,0.5), 0 4px 12px rgba(232,168,80,0.3); }
.screen-label { margin-top: 14px; font-size: 10px; color: rgba(232,168,80,0.6); letter-spacing: 8px; font-weight: 500; }

.perspective-wrap { perspective: 1000px; width: 100%; display: flex; justify-content: center; }
.theater-grid { display: grid; gap: 6px; transform: rotateX(45deg); transform-origin: center top; padding: 0 20px; }
.theater-grid { grid-template-columns: repeat(v-bind(colCount), 32px); }

.seat-cell { width: 32px; height: 32px; border-radius: 4px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.seat-cell.available { border: 1px solid rgba(255,255,255,0.15); background: #1f1f24; }
.seat-cell.available:hover { transform: translateZ(8px); box-shadow: 0 0 12px rgba(232,168,80,0.25); }
.seat-cell.selected { background: #E8A850; box-shadow: 0 0 15px rgba(232,168,80,0.5); border: none; }
.seat-cell.sold { background: #34343a; color: #8F8D9A; cursor: not-allowed; }
.seat-cell.locked { background: #34343a; cursor: not-allowed; }
.seat-cell.aisle { background: transparent; border: none; cursor: default; }
.seat-cell.vip { border: 2px solid rgba(255,151,155,0.5); background: rgba(255,151,155,0.1); }

/* Right */
.seat-right { width: 300px; flex-shrink: 0; }
.order-card { display: flex; flex-direction: column; gap: 16px; height: 100%; position: sticky; top: 20px; }
.oc-header { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 700; color: var(--text-primary); letter-spacing: 2px; }
.oc-header .el-icon { color: var(--color-primary); font-size: 20px; }
.oc-label { font-size: 11px; color: var(--text-tertiary); letter-spacing: 0.5px; display: block; margin-bottom: 6px; }
.oc-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.oc-empty { font-size: 12px; color: var(--text-tertiary); font-style: italic; }

.oc-prices { border-top: 1px dashed rgba(255,255,255,0.08); border-bottom: 1px dashed rgba(255,255,255,0.08); padding: 14px 0; }
.oc-row { display: flex; justify-content: space-between; font-size: 13px; color: var(--text-secondary); padding: 4px 0; }
.oc-total { display: flex; justify-content: space-between; margin-top: 8px; font-size: 16px; font-weight: 700; color: var(--text-primary); }
.oc-total span:last-child { font-size: 22px; color: var(--color-primary); font-family: 'Consolas', monospace; }

.oc-confirm { width: 100%; padding: 14px; border: none; border-radius: 10px; background: linear-gradient(135deg, #E8A850, #B8860B); color: #1A1814; font-size: 14px; font-weight: 700; letter-spacing: 2px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; transition: all 0.2s; box-shadow: 0 4px 20px rgba(232,168,80,0.3); }
.oc-confirm:hover:not(:disabled) { filter: brightness(1.1); }
.oc-confirm:disabled { opacity: 0.4; cursor: not-allowed; }
.oc-note { font-size: 10px; color: rgba(255,255,255,0.2); text-align: center; line-height: 1.5; font-style: italic; }

/* Payment */
.pay-body { padding: 4px 0; }
.pay-movie { font-size: 16px; font-weight: 700; text-align: center; color: var(--text-primary); margin-bottom: 14px; }
.pay-info { background: var(--bg-secondary); border-radius: 8px; padding: 12px 16px; margin-bottom: 12px; }
.pay-row { display: flex; justify-content: space-between; padding: 5px 0; font-size: 13px; color: var(--text-secondary); }
.pay-total { border-top: 1px dashed var(--border-color); margin-top: 4px; padding-top: 8px; font-weight: 700; color: var(--text-primary); }
.pay-price { color: var(--color-primary); font-size: 18px; }
.pay-method { text-align: center; padding: 12px; border: 2px solid var(--color-primary); border-radius: 8px; color: var(--color-primary); font-size: 14px; font-weight: 600; background: rgba(232,168,80,0.05); }

/* Light Mode Overrides */
[data-theme='light'] .seat-page { background: #fff8f4; }
[data-theme='light'] .seat-topbar { background: rgba(255,255,255,0.8); border-bottom-color: rgba(0,0,0,0.06); }
[data-theme='light'] .stb-divider { background: rgba(0,0,0,0.1); }
[data-theme='light'] .glass-card { background: rgba(255,255,255,0.75); border-color: rgba(0,0,0,0.06); }
[data-theme='light'] .lg-box.available { border-color: rgba(0,0,0,0.15); background: #fff; }
[data-theme='light'] .lg-box.sold { background: #e4d8ce; }
[data-theme='light'] .lg-box.vip { border-color: rgba(132,84,0,0.4); background: rgba(132,84,0,0.04); }
[data-theme='light'] .screen-bloom { background: linear-gradient(to bottom, rgba(132,84,0,0.15), transparent); }
[data-theme='light'] .screen-curve { background: linear-gradient(90deg, transparent 5%, #845400 20%, #845400 80%, transparent 95%); box-shadow: 0 0 16px rgba(132,84,0,0.3); }
[data-theme='light'] .screen-label { color: rgba(132,84,0,0.5); }
[data-theme='light'] .seat-cell.available { border-color: rgba(0,0,0,0.15); background: #fff; }
[data-theme='light'] .seat-cell.available:hover { box-shadow: 0 0 10px rgba(132,84,0,0.2); }
[data-theme='light'] .seat-cell.selected { background: #e8a850; box-shadow: 0 0 12px rgba(232,168,80,0.4); }
[data-theme='light'] .seat-cell.sold { background: #e4d8ce; color: #9B9590; }
[data-theme='light'] .seat-cell.locked { background: #e4d8ce; }
[data-theme='light'] .seat-cell.vip { border-color: rgba(132,84,0,0.4); background: rgba(132,84,0,0.04); }
[data-theme='light'] .oc-prices { border-color: rgba(0,0,0,0.08); }
[data-theme='light'] .oc-empty { color: #9B9590; }
[data-theme='light'] .oc-note { color: rgba(0,0,0,0.25); }
[data-theme='light'] .stb-back:hover { color: #845400; }

@media (max-width: 960px) {
  .seat-topbar { padding: 10px 16px; }
  .stb-step, .stb-step-title { display: none; }
  .seat-main { flex-direction: column; height: auto; padding: 10px; }
  .seat-left { width: 100%; flex-direction: row; }
  .seat-left .mc-poster { width: 80px; }
  .seat-right { width: 100%; }
  .theater-grid { gap: 3px; }
  .seat-cell { width: 24px; height: 24px; font-size: 10px; }
}
</style>
