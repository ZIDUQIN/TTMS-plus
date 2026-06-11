<template>
  <div class="ticket-page">
    <!-- Top Nav -->
    <nav class="ticket-nav">
      <div class="ticket-nav__inner">
        <router-link to="/my-orders" class="ticket-nav__back">
          <span class="material-symbols-outlined">arrow_back</span>
          <span>返回订单列表</span>
        </router-link>
        <div class="ticket-nav__brand">TTMS</div>
        <div class="ticket-nav__actions">
          <router-link to="/profile" class="ticket-nav__icon">
            <span class="material-symbols-outlined">person</span>
          </router-link>
        </div>
      </div>
    </nav>

    <!-- Floating Back Button -->
    <button class="floating-back" @click="$router.push('/my-orders')" title="返回">
      <span class="material-symbols-outlined">arrow_back</span>
    </button>

    <!-- Main Content -->
    <main class="ticket-main" v-if="order">
      <!-- Ticket Card -->
      <div class="ticket-card">
        <!-- Top: Poster + Info -->
        <div class="ticket-top">
          <div class="ticket-poster">
            <img
              :src="order.moviePoster || defaultPoster"
              :alt="order.movieName"
              class="ticket-poster__img"
              @error="onImgError"
            />
          </div>
          <h1 class="ticket-movie">{{ order.movieName }}</h1>
          <p class="ticket-brand">TTMS PREMIUM CINEMA</p>
          <div class="ticket-meta-row">
            <div class="ticket-meta">
              <span class="ticket-meta__label">Date & Time</span>
              <span class="ticket-meta__value">{{ formatDateTime(order.scheduleStartTime || order.startTime) }}</span>
            </div>
            <div class="ticket-meta ticket-meta--right">
              <span class="ticket-meta__label">Hall</span>
              <span class="ticket-meta__value">{{ order.hallName || '--' }}</span>
            </div>
          </div>
        </div>

        <!-- Serrated Divider -->
        <div class="ticket-divider-wrap">
          <div class="ticket-divider-cut ticket-divider-cut--left"></div>
          <div class="ticket-divider-line"></div>
          <div class="ticket-divider-cut ticket-divider-cut--right"></div>
        </div>

        <!-- Bottom: Seat Info + QR -->
        <div class="ticket-bottom">
          <!-- Seat Details -->
          <div class="ticket-seat-row">
            <div class="ticket-seat">
              <span class="ticket-seat__label">Row</span>
              <span class="ticket-seat__value">{{ seatRow }}</span>
            </div>
            <div class="ticket-seat">
              <span class="ticket-seat__label">Seat</span>
              <span class="ticket-seat__value">{{ seatNums }}</span>
            </div>
            <div class="ticket-seat">
              <span class="ticket-seat__label">Status</span>
              <span class="ticket-seat__value ticket-seat__value--status" :class="statusClass">
                {{ statusLabel(order.status ?? order.orderStatus) }}
              </span>
            </div>
          </div>

          <!-- QR Code -->
          <div class="ticket-qr">
            <div class="ticket-qr__box">
              <div class="ticket-qr__placeholder">
                <span class="material-symbols-outlined">qr_code_2</span>
                <span class="ticket-qr__text">SCAN TO ENTER</span>
              </div>
            </div>
          </div>

          <!-- Booking Details -->
          <div class="ticket-detail-row">
            <div class="ticket-detail">
              <span class="ticket-detail__label">Order ID</span>
              <span class="ticket-detail__value">{{ order.orderNo || order.id }}</span>
            </div>
            <div class="ticket-detail">
              <span class="ticket-detail__label">Total Amount</span>
              <span class="ticket-detail__value ticket-detail__value--price">
                ¥{{ order.totalAmount || order.totalPrice || '--' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Policy -->
        <div class="ticket-policy">
          <span class="ticket-policy__title">Ticket Policy</span>
          <p class="ticket-policy__text">
            This digital ticket is valid for one entry only. Please arrive 15 minutes prior to showtime.
            Cancellations are permitted up to 4 hours before the screening.
          </p>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="ticket-actions">
        <button class="action-btn action-btn--primary" @click="saveToPhone">
          <span class="material-symbols-outlined">save_alt</span>
          <span>保存到手机</span>
        </button>
        <button class="action-btn action-btn--outline" @click="printTicket">
          <span class="material-symbols-outlined">print</span>
          <span>打印票据</span>
        </button>
      </div>
      <button class="share-btn" @click="shareTicket">
        <span class="material-symbols-outlined">share</span>
        <span>分享订票信息</span>
      </button>
    </main>

    <!-- Loading -->
    <div v-else-if="loading" class="ticket-loading">
      <div class="skeleton-card">
        <div class="skeleton-poster"></div>
        <div class="skeleton-line w-50"></div>
        <div class="skeleton-line w-30"></div>
        <div class="skeleton-line w-40"></div>
      </div>
    </div>

    <!-- Footer -->
    <footer class="ticket-footer">
      <div class="ticket-footer__brand">TTMS</div>
      <div class="ticket-footer__links">
        <a href="#">Privacy Policy</a>
        <a href="#">Terms of Service</a>
        <a href="#">Contact Support</a>
      </div>
      <p>© 2024 TTMS Premium Cinema. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/order'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(true)

const defaultPoster = 'data:image/svg+xml,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="80" height="106" viewBox="0 0 80 106">
  <rect fill="#1a1a2e" width="80" height="106"/>
  <text fill="#7a8096" font-family="Arial" font-size="8" text-anchor="middle" x="40" y="56">暂无海报</text>
</svg>`)

const seatRow = computed(() => {
  const seats = order.value?.seatNumbers || order.value?.seats
  if (!seats) return '--'
  if (Array.isArray(seats) && seats.length > 0) {
    return String(seats[0]).split('-')[0] || '--'
  }
  if (typeof seats === 'string') {
    return seats.split(',')[0]?.split('-')[0] || '--'
  }
  return '--'
})

const seatNums = computed(() => {
  const seats = order.value?.seatNumbers || order.value?.seats
  if (!seats) return '--'
  const list = Array.isArray(seats) ? seats
    : typeof seats === 'string' ? seats.split(',') : [String(seats)]
  return list.map(s => String(s).split('-')[1] || s).join(', ')
})

const statusClass = computed(() => {
  const s = order.value?.status ?? order.value?.orderStatus
  const map = { 0: 'warn', 1: 'ok', 2: 'ok', 3: 'info', 4: 'warn', 5: 'warn' }
  return map[s] || ''
})

function statusLabel(s) {
  const map = { 0: '待支付', 1: '已支付', 2: '已完成', 3: '已改签', 4: '已退票', 5: '已过期' }
  return map[s] ?? '--'
}

function formatDateTime(d) {
  if (!d) return '--'
  const dt = new Date(d)
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
  return `${months[dt.getMonth()]} ${dt.getDate()}, ${dt.getFullYear()} · ${String(dt.getHours()).padStart(2,'0')}:${String(dt.getMinutes()).padStart(2,'0')}`
}

function onImgError(e) { e.target.src = defaultPoster }

function saveToPhone() { ElMessage.info('保存到手机功能开发中') }
function printTicket() { window.print() }
function shareTicket() {
  const text = `TTMS - ${order.value?.movieName} | ${formatDateTime(order.value?.scheduleStartTime || order.value?.startTime)}`
  if (navigator.share) { navigator.share({ title: 'TTMS E-Ticket', text }) }
  else { ElMessage.info('已复制订票信息'); navigator.clipboard?.writeText(text) }
}

async function fetchOrder() {
  loading.value = true
  try {
    const id = route.params.id
    const res = await getOrderDetail(id)
    order.value = res.data || null
  } catch {
    ElMessage.error('订单不存在或已被删除')
    router.push('/my-orders')
  } finally { loading.value = false }
}

onMounted(fetchOrder)
</script>

<style scoped>
/* ============================================================
   E-Ticket Detail — Cinema Editorial Edition
   ============================================================ */
.ticket-page { min-height: 100vh; background: var(--bg-primary); }

/* ---- Nav ---- */
.ticket-nav {
  position: fixed; top: 0; left: 0; right: 0; z-index: 50;
  background: var(--bg-card); border-bottom: 1px solid var(--border-light);
}
[data-theme='dark'] .ticket-nav { background: rgba(20,20,31,0.7); backdrop-filter: blur(20px); }

.ticket-nav__inner {
  max-width: 1280px; margin: 0 auto; padding: 0 24px; height: 56px;
  display: flex; align-items: center; justify-content: space-between;
}
.ticket-nav__back { display: flex; align-items: center; gap: 6px; color: var(--text-secondary); font-size: 13px; font-weight: 500; }
.ticket-nav__back:hover { color: var(--color-primary); }
.ticket-nav__back .material-symbols-outlined { font-size: 18px; }
.ticket-nav__brand { font-family: Georgia, 'Noto Serif SC', serif; font-size: 20px; font-weight: 700; color: var(--color-primary); }
.ticket-nav__icon { color: var(--text-secondary); padding: 4px; }
.ticket-nav__icon:hover { color: var(--color-primary); }

/* ---- Floating Back ---- */
.floating-back {
  position: fixed; top: 80px; left: max(24px, calc((100vw - 556px) / 2));
  z-index: 40; width: 40px; height: 40px; border-radius: 50%; border: 1px solid var(--border-color);
  background: var(--bg-card); color: var(--text-secondary); cursor: pointer; display: flex;
  align-items: center; justify-content: center; box-shadow: var(--shadow-light); transition: all 0.2s ease;
}
.floating-back:hover { border-color: var(--color-primary); color: var(--color-primary); box-shadow: var(--shadow-medium); }
.floating-back .material-symbols-outlined { font-size: 20px; }
@media (max-width: 640px) { .floating-back { left: 16px; top: 72px; } }

/* ---- Main ---- */
.ticket-main { padding-top: 100px; padding-bottom: 48px; display: flex; flex-direction: column; align-items: center; padding-left: 16px; padding-right: 16px; }

/* ---- Ticket Card ---- */
.ticket-card {
  width: 100%; max-width: 460px; background: var(--bg-card); border-radius: var(--radius-xl);
  border: 1px solid var(--border-light); box-shadow: 0 4px 40px rgba(0,0,0,0.06); overflow: hidden;
}
[data-theme='dark'] .ticket-card { box-shadow: 0 4px 40px rgba(0,0,0,0.3); }

/* ---- Top ---- */
.ticket-top { padding: 32px 28px 24px; text-align: center; }

.ticket-poster { width: 108px; height: 148px; margin: 0 auto 20px; border-radius: var(--radius-md); overflow: hidden; box-shadow: var(--shadow-medium); }
.ticket-poster__img { width: 100%; height: 100%; object-fit: cover; }

.ticket-movie { font-family: Georgia, 'Noto Serif SC', serif; font-size: 26px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.ticket-brand { font-size: 13px; font-weight: 600; color: var(--color-primary); letter-spacing: 0.2em; margin-bottom: 20px; }

.ticket-meta-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; padding-top: 16px; border-top: 1px solid var(--border-light); }
.ticket-meta--right { text-align: right; }
.ticket-meta__label { display: block; font-size: 11px; color: var(--text-tertiary); text-transform: uppercase; margin-bottom: 4px; font-weight: 500; }
.ticket-meta__value { font-size: 14px; font-weight: 600; color: var(--text-primary); }

/* ---- Serrated Divider ---- */
.ticket-divider-wrap {
  display: flex; align-items: center; position: relative; height: 32px;
}
.ticket-divider-cut {
  width: 24px; height: 24px; border-radius: 50%; background: var(--bg-primary);
  border: 1px solid var(--border-light); position: absolute; z-index: 1;
}
.ticket-divider-cut--left { left: -12px; }
.ticket-divider-cut--right { right: -12px; }
.ticket-divider-line {
  flex: 1; height: 0; margin: 0 20px;
  background-image: linear-gradient(to right, var(--border-color) 33%, transparent 0%);
  background-position: bottom; background-size: 12px 1px; background-repeat: repeat-x;
}

/* ---- Bottom ---- */
.ticket-bottom { padding: 24px 28px 28px; text-align: center; }

.ticket-seat-row { display: flex; justify-content: space-between; margin-bottom: 24px; }
.ticket-seat { text-align: center; }
.ticket-seat:nth-child(1) { text-align: left; }
.ticket-seat:nth-child(3) { text-align: right; }
.ticket-seat__label { display: block; font-size: 11px; color: var(--text-tertiary); text-transform: uppercase; margin-bottom: 4px; font-weight: 500; }
.ticket-seat__value { font-family: Georgia, 'Noto Serif SC', serif; font-size: 26px; font-weight: 700; color: var(--text-primary); }
.ticket-seat__value--status { font-size: 18px; }
.ticket-seat__value--status.ok { color: var(--color-emerald); }
.ticket-seat__value--status.warn { color: var(--color-warning); }
.ticket-seat__value--status.info { color: var(--text-secondary); }

/* ---- QR ---- */
.ticket-qr { display: flex; justify-content: center; margin-bottom: 24px; }
.ticket-qr__box {
  width: 144px; height: 144px; background: var(--bg-secondary); border-radius: var(--radius-lg);
  border: 1px solid var(--border-light); display: flex; align-items: center; justify-content: center;
  flex-direction: column; gap: 8px;
}
.ticket-qr__placeholder { text-align: center; color: var(--text-tertiary); }
.ticket-qr__placeholder .material-symbols-outlined { font-size: 56px; }
.ticket-qr__text { display: block; font-size: 10px; font-weight: 500; letter-spacing: 0.1em; margin-top: 4px; }

/* ---- Detail Row ---- */
.ticket-detail-row { padding-top: 20px; border-top: 1px solid var(--border-light); }
.ticket-detail { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.ticket-detail__label { font-size: 11px; color: var(--text-tertiary); text-transform: uppercase; font-weight: 500; }
.ticket-detail__value { font-size: 14px; font-weight: 500; color: var(--text-primary); font-family: 'JetBrains Mono', 'Consolas', monospace; }
.ticket-detail__value--price { font-family: Georgia, 'Noto Serif SC', serif; font-size: 20px; font-weight: 700; color: var(--color-primary); }

/* ---- Policy ---- */
.ticket-policy { padding: 16px 28px; background: var(--bg-secondary); text-align: center; }
.ticket-policy__title { display: block; font-size: 10px; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.1em; margin-bottom: 6px; }
.ticket-policy__text { font-size: 11px; color: var(--text-tertiary); line-height: 1.5; opacity: 0.7; }

/* ---- Action Buttons ---- */
.ticket-actions { display: flex; gap: 12px; width: 100%; max-width: 460px; margin-top: 32px; }
.action-btn { flex: 1; padding: 14px; border-radius: var(--radius-pill); font-size: 14px; font-weight: 600; display: flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; transition: all 0.2s ease; font-family: inherit; }
.action-btn--primary { background: var(--color-primary); color: #fff; border: none; box-shadow: 0 4px 16px rgba(132,84,0,0.15); }
.action-btn--primary:hover { transform: scale(1.02); }
.action-btn--outline { background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border-color); }
.action-btn--outline:hover { background: var(--bg-secondary); }

.share-btn { display: flex; align-items: center; gap: 6px; margin-top: 24px; padding: 12px; border: none; background: none; color: var(--text-secondary); font-size: 12px; cursor: pointer; font-family: inherit; opacity: 0.6; transition: opacity 0.2s; }
.share-btn:hover { opacity: 1; }

/* ---- Loading ---- */
.ticket-loading { padding-top: 120px; display: flex; justify-content: center; }
.skeleton-card { width: 100%; max-width: 400px; display: flex; flex-direction: column; align-items: center; gap: 16px; }
.skeleton-poster { width: 108px; height: 148px; border-radius: var(--radius-md); background: var(--bg-hover); }
.skeleton-line { height: 16px; border-radius: 4px; background: var(--bg-hover); }
.skeleton-line.w-50 { width: 50%; }
.skeleton-line.w-40 { width: 40%; }
.skeleton-line.w-30 { width: 30%; }

/* ---- Footer ---- */
.ticket-footer { text-align: center; padding: 48px 24px; border-top: 1px solid var(--border-light); margin-top: 48px; }
.ticket-footer__brand { font-family: Georgia, 'Noto Serif SC', serif; font-size: 28px; font-weight: 700; color: var(--color-primary); margin-bottom: 16px; }
.ticket-footer__links { display: flex; justify-content: center; flex-wrap: wrap; gap: 24px; margin-bottom: 12px; }
.ticket-footer__links a { font-size: 12px; color: var(--text-secondary); }
.ticket-footer__links a:hover { color: var(--color-primary); }
.ticket-footer p { font-size: 12px; color: var(--text-tertiary); opacity: 0.6; }

/* ---- Responsive ---- */
@media (max-width: 480px) {
  .ticket-top { padding: 24px 20px; }
  .ticket-bottom { padding: 20px; }
  .ticket-actions { flex-direction: column; }
}
</style>
