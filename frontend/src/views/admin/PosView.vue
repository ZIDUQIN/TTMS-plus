<template>
  <div class="pos-page">
    <!-- Left: Film & Schedule -->
    <section class="pos-left">
      <div class="pl-search">
        <el-input v-model="searchText" placeholder="搜索影片或场次..." :prefix-icon="Search" clearable />
      </div>
      <div class="pl-tabs">
        <button class="pl-tab active">正在热映</button>
        <button class="pl-tab">即将上映</button>
      </div>

      <!-- Film Grid -->
      <div class="pl-films">
        <div v-for="m in movies" :key="m.id" class="pl-film-card" :class="{ active: selectedMovie?.id === m.id }"
          @click="selectMovie(m)">
          <div class="pl-film-poster">
            <img v-if="m.poster" :src="m.poster" @error="e => e.target.style.display='none'" />
            <el-icon v-else :size="28"><VideoCameraFilled /></el-icon>
            <div v-if="selectedMovie?.id === m.id" class="pl-film-check">
              <el-icon><Check /></el-icon>
            </div>
          </div>
          <h4>{{ m.name }}</h4>
          <p>{{ m.genre || '' }} · {{ m.duration || '--' }}分钟</p>
        </div>
      </div>

      <!-- Schedule List -->
      <div class="pl-schedules" v-if="selectedMovie">
        <h4 class="pl-section-title">可选场次</h4>
        <div v-if="schedules.length === 0" style="padding:20px;color:var(--text-tertiary);text-align:center">暂无可用场次</div>
        <div v-for="s in schedules" :key="s.scheduleId || s.id" class="pl-sch-item" :class="{ active: (selectedSchedule?.scheduleId || selectedSchedule?.id) === (s.scheduleId || s.id) }"
          @click="selectSchedule(s)">
          <div class="pl-sch-left">
            <span class="pl-sch-time">{{ fmtTime(s.startTime) }} - {{ fmtTime(s.endTime) }}</span>
            <span class="pl-sch-hall">{{ s.hallName }} · {{ s.availableSeats || 0 }}座可选</span>
          </div>
          <el-icon v-if="selectedSchedule?.id === s.id" color="var(--color-primary)"><CircleCheckFilled /></el-icon>
          <span v-else-if="(s.availableSeats || 0) < 10" class="pl-sch-filling">紧张</span>
        </div>
      </div>
    </section>

    <!-- Center: 3D Seat Map -->
    <section class="pos-center">
      <div class="pc-screen-area">
        <div class="pc-screen-glow"></div>
        <div class="pc-screen-line"></div>
        <p class="pc-screen-label">{{ selectedSchedule?.hallName || '影厅' }} SCREEN</p>
      </div>

      <div class="pc-seat-wrap" v-if="selectedSchedule">
        <div v-loading="seatsLoading" style="min-height:200px">
          <div v-if="!seatsLoading && seatRows.length" class="pc-seat-grid">
            <div v-for="row in seatRows" :key="row.label" class="pc-seat-row">
              <span class="pc-row-label">{{ row.label }}</span>
              <div class="pc-seat-cells">
                <span v-for="s in row.seats" :key="s.id"
                  :class="['pc-seat', seatClass(s)]"
                  :title="s.seatNumber || ''"
                  @click="toggleSeat(s)">{{ s.seatCol }}</span>
              </div>
              <span class="pc-row-label">{{ row.label }}</span>
            </div>
          </div>
          <el-empty v-else-if="!seatsLoading" description="请选择场次" :image-size="60" />
        </div>
      </div>
      <div v-else class="pc-empty-hint">← 请在左侧选择影片和场次</div>

      <!-- Legend -->
      <div class="pc-legend">
        <div class="pc-lg-item"><div class="pc-lg-box available"></div>可选</div>
        <div class="pc-lg-item"><div class="pc-lg-box selected"></div>已选</div>
        <div class="pc-lg-item"><div class="pc-lg-box sold"></div>已售</div>
        <div class="pc-lg-item"><div class="pc-lg-box locked"></div>锁定</div>
      </div>
    </section>

    <!-- Right: Checkout -->
    <section class="pos-right">
      <div class="pr-scroll">
        <!-- Order Summary -->
        <div class="pr-section">
          <h4 class="pr-section-title">订单摘要</h4>
          <div v-if="selectedMovie && selectedSchedule" class="pr-order-card">
            <div class="pr-order-info">
              <h3>{{ selectedMovie.name }}</h3>
              <p>{{ fmtDate(selectedSchedule.startTime) }} · {{ fmtTime(selectedSchedule.startTime) }} · {{ selectedSchedule.hallName }}</p>
              <div class="pr-seat-tags" v-if="pickSeats.length">
                <el-tag v-for="s in pickSeats" :key="s" type="warning" size="small" closable @close="deselectSeat(s)">{{ s }}</el-tag>
              </div>
              <span v-else class="pr-no-seats">未选择座位</span>
            </div>
            <div class="pr-order-price">
              <span>¥{{ totalPrice }}</span>
              <span class="pr-ticket-count">{{ pickSeats.length }}张</span>
            </div>
          </div>
          <div v-else class="pr-empty">请选择影片和场次</div>
        </div>

        <!-- Snacks -->
        <div class="pr-section">
          <div class="pr-section-head">
            <h4 class="pr-section-title">卖品加购</h4>
          </div>
          <div class="pr-snacks">
            <div v-for="sn in snacks" :key="sn.id" class="pr-snack-card"
              :class="{ picked: pickedSnacks.find(x => x.id === sn.id) }"
              @click="addSnack(sn)">
              <div class="pr-snack-icon">
                <el-icon :size="24"><GobletSquare /></el-icon>
              </div>
              <span>{{ sn.name }}</span>
              <span class="pr-snack-price">¥{{ sn.price }}</span>
              <div v-if="pickedSnacks.find(x => x.id === sn.id)" class="pr-snack-ctrls">
                <button class="pr-snack-del" @click.stop="decSnack(sn)">−</button>
                <span class="pr-snack-qty">x{{ pickedSnacks.find(x => x.id === sn.id).qty }}</span>
              </div>
            </div>
            <div v-if="snacks.length === 0" class="pr-empty">暂无可选卖品</div>
          </div>
        </div>

        <!-- Payment -->
        <div class="pr-section">
          <h4 class="pr-section-title">支付方式</h4>
          <div class="pr-payments">
            <button v-for="p in payMethods" :key="p.value"
              class="pr-pay-btn" :class="{ active: payMethod === p.value }"
              @click="payMethod = p.value">
              <el-icon :size="18"><component :is="p.icon" /></el-icon>
              <span>{{ p.label }}</span>
            </button>
          </div>
        </div>

        <!-- Totals -->
        <div class="pr-totals">
          <div class="pr-total-row"><span>小计</span><span>¥{{ totalPrice }}</span></div>
          <div class="pr-total-row"><span>卖品</span><span>¥{{ snackTotal }}</span></div>
          <div class="pr-total-final"><span>应付总额</span><span>¥{{ finalTotal }}</span></div>
        </div>
      </div>

      <!-- Confirm -->
      <div class="pr-footer">
        <button class="pr-confirm" :disabled="!canSubmit || submitting" @click="submitOrder">
          <el-icon :size="20"><Sell /></el-icon>
          <span>{{ submitting ? '处理中...' : '确认出票' }}</span>
        </button>
      </div>
    </section>

    <!-- Floating Tool Bar -->
    <div class="pos-float-bar">
      <button @click="resetAll"><el-icon><Refresh /></el-icon><span>重置</span></button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPosSchedules, getPosSeats, posCreateOrder } from '@/api/pos'
import { getMovieList } from '@/api/movie'
import { getSnacks } from '@/api/snack'
import { ElMessage } from 'element-plus'
import { Search, Check, CircleCheckFilled, VideoCameraFilled, GobletSquare, Sell, Refresh } from '@element-plus/icons-vue'

const movies = ref([]); const schedules = ref([]); const snacks = ref([])
const selectedMovie = ref(null); const selectedSchedule = ref(null)
const seats = ref([]); const seatsLoading = ref(false)
const pickSeats = ref([]); const pickedSnacks = ref([]) // [{id, name, price, qty}]
const payMethod = ref('CASH'); const submitting = ref(false)
const searchText = ref('')

const filteredMovies = computed(() => {
  if (!searchText.value) return movies.value
  const kw = searchText.value.toLowerCase()
  return movies.value.filter(m => m.name?.toLowerCase().includes(kw))
})

const payMethods = [
  { value: 'CASH', label: '现金', icon: 'Money' },
  { value: 'WECHAT', label: '微信', icon: 'ChatDotSquare' },
  { value: 'ALIPAY', label: '支付宝', icon: 'CreditCard' },
]

const seatRows = computed(() => {
  const list = seats.value; if (!list?.length) return []
  const flat = Array.isArray(list[0]) ? list.flat() : list
  const grouped = {}
  flat.forEach(s => {
    const label = s.seatNumber ? s.seatNumber.split('-')[0] : (s.seatRow ? String.fromCharCode(64 + s.seatRow) : '')
    if (!label) return
    if (!grouped[label]) grouped[label] = []
    grouped[label].push(s)
  })
  return Object.entries(grouped).map(([l, s]) => ({ label: l, seats: s }))
})

const unitPrice = computed(() => parseFloat(selectedSchedule.value?.price) || parseFloat(selectedMovie.value?.price) || 0)
const totalPrice = computed(() => selectedSchedule.value ? (unitPrice.value * pickSeats.value.length).toFixed(2) : '0.00')
const snackTotal = computed(() => pickedSnacks.value.reduce((s, sn) => s + parseFloat(sn.price || 0) * (sn.qty || 1), 0).toFixed(2))
const finalTotal = computed(() => (parseFloat(totalPrice.value) + parseFloat(snackTotal.value)).toFixed(2))
const canSubmit = computed(() => selectedSchedule.value && pickSeats.value.length > 0)

function fmtTime(t) { if (!t) return '--'; const d = new Date(t); return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` }
function fmtDate(t) { if (!t) return ''; const d = new Date(t); return `${d.getMonth()+1}月${d.getDate()}日` }
function seatClass(s) {
  if (pickSeats.value.includes(s.seatNumber)) return 'selected'
  if (s.status === 2) return 'sold'
  if (s.status === 1) return 'locked'
  if (s.status === 3) return 'aisle'
  return 'available'
}

const allSchedules = ref([])

async function fetchMovies() { try { const r = await getMovieList({ page: 1, size: 200 }); movies.value = r.data?.records || r.data?.data || [] } catch {} }
async function fetchSnacks() { try { const r = await getSnacks(); snacks.value = (r.data?.data || r.data || []).slice(0, 4) } catch {} }
async function fetchAllSchedules() {
  try { const r = await getPosSchedules(); allSchedules.value = r.data?.data || r.data || [] } catch { allSchedules.value = [] }
}

async function selectMovie(m) {
  selectedMovie.value = m; selectedSchedule.value = null; pickSeats.value = []; pickedSnacks.value = []
  schedules.value = allSchedules.value.filter(s => {
    return s.movieName === m.name || s.movieId === m.id || String(s.movieId) === String(m.id)
  })
}

async function selectSchedule(s) {
  const sid = s.scheduleId || s.id
  selectedSchedule.value = s; pickSeats.value = []; seatsLoading.value = true
  try {
    const r = await getPosSeats(sid)
    const raw = r.data?.data || r.data || {}
    seats.value = raw.seats || raw || []
  } catch { seats.value = [] }
  seatsLoading.value = false
}

function toggleSeat(seat) {
  if (seat.status === 2 || seat.status === 1 || seat.status === 3) return
  const idx = pickSeats.value.indexOf(seat.seatNumber)
  if (idx >= 0) pickSeats.value.splice(idx, 1)
  else if (pickSeats.value.length < 6) pickSeats.value.push(seat.seatNumber)
  else ElMessage.warning('最多选择6个座位')
}
function deselectSeat(s) { pickSeats.value = pickSeats.value.filter(x => x !== s) }
function addSnack(sn) {
  const idx = pickedSnacks.value.findIndex(x => x.id === sn.id)
  if (idx >= 0) {
    pickedSnacks.value[idx].qty = (pickedSnacks.value[idx].qty || 1) + 1
  } else {
    pickedSnacks.value.push({ id: sn.id, name: sn.name, price: sn.price, qty: 1 })
  }
}
function decSnack(sn) {
  const idx = pickedSnacks.value.findIndex(x => x.id === sn.id)
  if (idx < 0) return
  if (pickedSnacks.value[idx].qty > 1) {
    pickedSnacks.value[idx].qty--
  } else {
    pickedSnacks.value.splice(idx, 1)
  }
}
function resetAll() { selectedMovie.value = null; selectedSchedule.value = null; pickSeats.value = []; pickedSnacks.value = [] }
function removeSnack(snId) { pickedSnacks.value = pickedSnacks.value.filter(x => x.id !== snId) }

async function submitOrder() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    await posCreateOrder({ scheduleId: selectedSchedule.value.scheduleId || selectedSchedule.value.id, seatNumbers: [...pickSeats.value], payMethod: payMethod.value })
    ElMessage.success('出票成功！')
    resetAll()
  } catch (e) { /* handled */ }
  submitting.value = false
}

onMounted(() => { fetchMovies(); fetchSnacks(); fetchAllSchedules() })
</script>

<style scoped>
.pos-page { display: flex; height: calc(100vh - var(--header-height)); background: #0A0A10; overflow: hidden; }

/* Left Panel */
.pos-left { width: 300px; flex-shrink: 0; background: #0d0e13; border-right: 1px solid rgba(255,255,255,0.06); display: flex; flex-direction: column; overflow: hidden; }
.pl-search { padding: 16px; }
.pl-search :deep(.el-input__wrapper) { background: #1b1b20; border-color: rgba(255,255,255,0.08); }
.pl-tabs { display: flex; gap: 4px; padding: 0 16px; }
.pl-tab { flex: 1; padding: 8px; border-radius: 8px; border: none; background: #1b1b20; color: var(--text-secondary); font-size: 11px; font-weight: 600; cursor: pointer; }
.pl-tab.active { background: var(--color-primary); color: #1A1814; }

.pl-films { flex: 1; overflow-y: auto; padding: 12px 16px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.pl-film-card { cursor: pointer; background: #1b1b20; border-radius: 10px; padding: 8px; border: 2px solid transparent; transition: all 0.15s; }
.pl-film-card:hover { border-color: rgba(232,168,80,0.3); }
.pl-film-card.active { border-color: var(--color-primary); box-shadow: 0 0 12px rgba(232,168,80,0.15); }
.pl-film-poster { aspect-ratio: 2/3; border-radius: 6px; overflow: hidden; background: #0d0e13; position: relative; display: flex; align-items: center; justify-content: center; color: var(--text-tertiary); margin-bottom: 6px; }
.pl-film-poster img { width: 100%; height: 100%; object-fit: cover; }
.pl-film-check { position: absolute; top: 4px; right: 4px; width: 22px; height: 22px; border-radius: 50%; background: var(--color-primary); color: #1A1814; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.pl-film-card h4 { font-size: 12px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pl-film-card p { font-size: 10px; color: var(--text-tertiary); }

.pl-schedules { border-top: 1px solid rgba(255,255,255,0.06); padding: 12px 16px; }
.pl-section-title { font-size: 10px; color: var(--text-tertiary); letter-spacing: 2px; margin-bottom: 8px; }
.pl-sch-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 8px; margin-bottom: 4px; cursor: pointer; transition: all 0.15s; background: #1b1b20; border-left: 3px solid transparent; }
.pl-sch-item:hover { background: #23232b; }
.pl-sch-item.active { border-left-color: var(--color-primary); background: rgba(232,168,80,0.06); }
.pl-sch-time { font-size: 14px; font-weight: 600; color: var(--text-primary); font-family: monospace; display: block; }
.pl-sch-hall { font-size: 10px; color: var(--text-tertiary); }
.pl-sch-item.active .pl-sch-time { color: var(--color-primary); }
.pl-sch-filling { font-size: 10px; color: var(--color-danger); font-weight: 600; }

/* Center */
.pos-center { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #0A0A10; position: relative; min-width: 0; }
.pc-screen-area { width: 60%; max-width: 500px; text-align: center; margin-bottom: 30px; }
.pc-screen-glow { height: 30px; background: linear-gradient(to bottom, rgba(232,168,80,0.2), transparent); border-radius: 50%; filter: blur(4px); }
.pc-screen-line { height: 2px; background: linear-gradient(90deg, transparent, var(--color-primary), transparent); border-radius: 1px; box-shadow: 0 2px 12px rgba(232,168,80,0.4); margin-top: -10px; }
.pc-screen-label { margin-top: 10px; font-size: 10px; color: rgba(255,255,255,0.3); letter-spacing: 6px; }

.pc-seat-wrap { flex: 1; display: flex; align-items: center; justify-content: center; overflow: auto; }
.pc-seat-grid { perspective: 1000px; }
.pc-seat-row { display: flex; align-items: center; gap: 4px; margin-bottom: 5px; }
.pc-row-label { width: 24px; text-align: center; font-size: 10px; color: var(--text-tertiary); font-family: monospace; flex-shrink: 0; }
.pc-seat-cells { display: flex; gap: 4px; flex-wrap: nowrap; }
.pc-seat { width: 26px; height: 26px; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 10px; cursor: pointer; transition: all 0.15s; }
.pc-seat.available { border: 1px solid rgba(255,255,255,0.2); background: #1b1b20; }
.pc-seat.available:hover { border-color: var(--color-primary); transform: scale(1.2); }
.pc-seat.selected { background: var(--color-primary); box-shadow: 0 0 10px rgba(232,168,80,0.5); color: #1A1814; }
.pc-seat.sold { background: #2a2a30; opacity: 0.5; cursor: not-allowed; }
.pc-seat.locked { background: #2a2a30; cursor: not-allowed; }
.pc-seat.aisle { background: transparent; border: none; cursor: default; }

.pc-legend { display: flex; gap: 20px; padding: 12px 0; }
.pc-lg-item { display: flex; align-items: center; gap: 6px; font-size: 10px; color: var(--text-tertiary); }
.pc-lg-box { width: 14px; height: 14px; border-radius: 3px; }
.pc-lg-box.available { border: 1px solid rgba(255,255,255,0.2); background: #1b1b20; }
.pc-lg-box.selected { background: var(--color-primary); }
.pc-lg-box.sold { background: #2a2a30; opacity: 0.5; }
.pc-lg-box.locked { background: #2a2a30; }
.pc-empty-hint { color: var(--text-tertiary); font-size: 13px; }

/* Right */
.pos-right { width: 320px; flex-shrink: 0; background: #0d0e13; border-left: 1px solid rgba(255,255,255,0.06); display: flex; flex-direction: column; }
.pr-scroll { flex: 1; overflow-y: auto; padding: 16px; }
.pr-section { margin-bottom: 20px; }
.pr-section-title { font-size: 10px; color: var(--text-tertiary); letter-spacing: 2px; margin-bottom: 10px; }
.pr-order-card { background: #1b1b20; border-radius: 10px; padding: 12px; display: flex; gap: 12px; align-items: center; border-left: 4px solid var(--color-primary); }
.pr-order-info { flex: 1; min-width: 0; }
.pr-order-info h3 { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.pr-order-info p { font-size: 10px; color: var(--text-tertiary); margin-top: 2px; }
.pr-seat-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
.pr-no-seats { font-size: 11px; color: var(--text-tertiary); font-style: italic; }
.pr-order-price { text-align: right; border-left: 1px dashed rgba(255,255,255,0.1); padding-left: 12px; flex-shrink: 0; }
.pr-order-price span:first-child { font-size: 18px; font-weight: 700; color: var(--color-primary); font-family: monospace; display: block; }
.pr-ticket-count { font-size: 10px; color: var(--text-tertiary); }

.pr-snacks { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.pr-snack-card { background: #1b1b20; border-radius: 8px; padding: 10px; text-align: center; cursor: pointer; border: 1px solid transparent; transition: all 0.15s; font-size: 11px; color: var(--text-secondary); }
.pr-snack-card:hover { border-color: rgba(232,168,80,0.3); }
.pr-snack-icon { margin-bottom: 4px; color: rgba(232,168,80,0.3); }
.pr-snack-price { display: block; color: var(--color-primary); font-weight: 600; margin-top: 3px; }
.pr-snack-card.picked { border-color: var(--color-primary); background: rgba(232,168,80,0.08); position: relative; }
.pr-snack-card.picked::after { content: '✓'; position: absolute; top: 6px; right: 8px; font-size: 12px; font-weight: 700; color: var(--color-primary); }
.pr-snack-ctrls { display: flex; align-items: center; justify-content: center; gap: 6px; margin-top: 6px; }
.pr-snack-del { width: 22px; height: 22px; border: none; border-radius: 50%; background: rgba(232,64,64,0.15); color: var(--color-danger); font-size: 16px; font-weight: 700; cursor: pointer; display: flex; align-items: center; justify-content: center; line-height: 1; padding: 0; transition: all 0.15s; flex-shrink: 0; }
.pr-snack-del:hover { background: var(--color-danger); color: #fff; }
.pr-snack-qty { font-size: 13px; font-weight: 700; color: var(--color-primary); }

.pr-payments { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.pr-pay-btn { display: flex; align-items: center; gap: 6px; padding: 10px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.08); background: #1b1b20; color: var(--text-secondary); font-size: 12px; cursor: pointer; transition: all 0.15s; }
.pr-pay-btn:hover { border-color: rgba(232,168,80,0.3); }
.pr-pay-btn.active { border-color: var(--color-primary); background: rgba(232,168,80,0.08); color: var(--color-primary); }

.pr-totals { background: #1b1b20; border-radius: 10px; padding: 12px; margin-top: 12px; }
.pr-total-row { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-secondary); padding: 3px 0; }
.pr-total-final { display: flex; justify-content: space-between; align-items: center; padding-top: 8px; margin-top: 6px; border-top: 1px solid rgba(255,255,255,0.08); font-size: 14px; font-weight: 700; color: var(--text-primary); }
.pr-total-final span:last-child { font-size: 22px; color: var(--color-primary); font-family: monospace; }

.pr-footer { padding: 12px 16px; background: #0d0e13; border-top: 1px solid rgba(255,255,255,0.06); }
.pr-confirm { width: 100%; padding: 14px; border: none; border-radius: 10px; background: linear-gradient(135deg, #E8A850, #B8860B); color: #1A1814; font-size: 15px; font-weight: 700; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; transition: all 0.15s; box-shadow: 0 6px 20px rgba(232,168,80,0.3); }
.pr-confirm:hover:not(:disabled) { transform: scale(1.02); }
.pr-confirm:disabled { opacity: 0.4; cursor: not-allowed; }

.pr-empty { padding: 16px; text-align: center; font-size: 12px; color: var(--text-tertiary); font-style: italic; }

.pos-float-bar { position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); background: rgba(20,20,31,0.98); border: 1px solid rgba(255,255,255,0.08); border-radius: 20px; padding: 8px 16px; display: flex; gap: 8px; z-index: 100; }
.pos-float-bar button { display: flex; align-items: center; gap: 4px; padding: 6px 12px; border-radius: 12px; border: none; background: transparent; color: var(--text-secondary); font-size: 11px; cursor: pointer; transition: all 0.15s; }
.pos-float-bar button:hover { color: var(--color-primary); background: rgba(128,128,128,0.08); }

/* Light Mode */
[data-theme='light'] .pos-page { background: #fff8f4; }
[data-theme='light'] .pos-left { background: #fef2e7; border-right-color: rgba(0,0,0,0.06); }
[data-theme='light'] .pos-right { background: #fff; border-left-color: rgba(0,0,0,0.06); }
[data-theme='light'] .pl-search :deep(.el-input__wrapper) { background: #fff; border-color: rgba(0,0,0,0.1); }
[data-theme='light'] .pl-tab { background: #fff; color: #514537; border: 1px solid rgba(0,0,0,0.08); }
[data-theme='light'] .pl-tab.active { background: #845400; color: #fff; }
[data-theme='light'] .pl-film-card { background: #fff; border-color: rgba(0,0,0,0.06); }
[data-theme='light'] .pl-film-card:hover { border-color: rgba(132,84,0,0.3); }
[data-theme='light'] .pl-film-card.active { border-color: #845400; background: #fff; }
[data-theme='light'] .pl-film-poster { background: #f8ece1; }
[data-theme='light'] .pl-sch-item { background: #fff; }
[data-theme='light'] .pl-sch-item:hover { background: #fef2e7; }
[data-theme='light'] .pl-sch-item.active { background: rgba(132,84,0,0.05); }
[data-theme='light'] .pos-center { background: #fff8f4; }
[data-theme='light'] .pc-seat.available { border-color: rgba(0,0,0,0.15); background: #fff; }
[data-theme='light'] .pc-seat.available:hover { border-color: #845400; }
[data-theme='light'] .pc-seat.sold { background: #e4d8ce; }
[data-theme='light'] .pc-seat.locked { background: #e4d8ce; }
[data-theme='light'] .pc-lg-box.available { border-color: rgba(0,0,0,0.15); background: #fff; }
[data-theme='light'] .pc-lg-box.sold { background: #e4d8ce; }
[data-theme='light'] .pr-order-card { background: #fff; }
[data-theme='light'] .pr-snack-card.picked { background: rgba(132,84,0,0.05); }
[data-theme='light'] .pr-snack-card { background: #fff; }
[data-theme='light'] .pr-pay-btn { background: #fff; border-color: rgba(0,0,0,0.08); }
[data-theme='light'] .pr-totals { background: #fff; }
[data-theme='light'] .pr-footer { background: #fff; }
[data-theme='light'] .pos-float-bar { background: rgba(255,255,255,0.9); border-color: rgba(0,0,0,0.08); }

@media (max-width: 1100px) { .pos-left { width: 240px; } .pos-right { width: 260px; } }
@media (max-width: 860px) { .pos-page { flex-direction: column; height: auto; } .pos-left, .pos-right { width: 100%; } .pos-center { min-height: 400px; } }
</style>
