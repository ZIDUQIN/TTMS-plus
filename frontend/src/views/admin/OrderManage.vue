<template>
  <div class="order-page">
    <div class="order-container">
      <!-- Header -->
      <header class="page-hero">
        <div>
          <h2 class="page-hero__title">订单管理</h2>
          <p class="page-hero__desc">实时监控并管理全院线的票务交易与退款申请。</p>
        </div>
        <div class="page-hero__btns">
          <button class="golden-btn" @click="openAssistCreate">
            <span class="material-symbols-outlined">add</span>
            <span>代客下单</span>
          </button>
        </div>
      </header>

      <!-- Stats Row -->
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-card__label">今日订单数</span>
          <div class="stat-card__val">{{ todayCount }}</div>
          <span class="stat-card__trend">实时</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__label">今日总营收</span>
          <div class="stat-card__val accent">¥{{ fmtMoney(todayRevenue) }}</div>
          <span class="stat-card__trend">已支付订单</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__label">待支付订单</span>
          <div class="stat-card__val">{{ pendingCount }}</div>
          <span class="stat-card__trend warn">需处理</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__label">总订单数</span>
          <div class="stat-card__val">{{ orders.length }}</div>
          <span class="stat-card__trend">累计</span>
        </div>
      </div>

      <!-- Filter Bar -->
      <div class="filter-card">
        <div class="filter-tabs">
          <button v-for="t in statusTabs" :key="t.value" class="filter-tab"
            :class="{ active: filterStatus === t.value }" @click="filterStatus = t.value">{{ t.label }}</button>
        </div>
        <div class="filter-actions">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" size="small" style="width:240px" />
          <el-input v-model="searchText" placeholder="搜索订单号、电影..." :prefix-icon="Search" clearable size="small" style="width:220px" />
        </div>
      </div>

      <!-- Orders Table -->
      <div class="table-card">
        <el-table :data="filteredOrders" v-loading="loading" class="editorial-table">
          <el-table-column label="订单编号" width="170">
            <template #default="{ row }">
              <span class="order-no">{{ row.orderNo || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="电影信息" min-width="200">
            <template #default="{ row }">
              <div class="movie-cell">
                <img v-if="row.moviePoster" :src="row.moviePoster" class="movie-poster" @error="onImgError" />
                <div v-else class="movie-poster-placeholder">
                  <span class="material-symbols-outlined">movie</span>
                </div>
                <div>
                  <span class="movie-name">{{ row.movieName || '--' }}</span>
                  <span class="movie-sub">{{ row.hallName || '--' }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="场次 · 座位" width="200">
            <template #default="{ row }">
              <span class="cell-main">{{ formatDateTime(row.startTime || row.scheduleStartTime) }}</span>
              <span class="cell-sub">{{ formatSeats(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">
              <span class="price-val">¥{{ fmtMoney(row.totalAmount || row.totalPrice || 0) }}</span>
              <span class="cell-sub">{{ row.paymentMethod || payLabel(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="status-badge" :class="'badge-' + statusColor(row)">
                {{ statusLabel(row.status ?? row.orderStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="用户" width="100">
            <template #default="{ row }">{{ row.username || '--' }}</template>
          </el-table-column>
          <el-table-column label="时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.createTime || row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <button v-if="(row.status ?? row.orderStatus) === 0" class="pay-btn" :disabled="payingOrderId === row.id"
                @click="handlePay(row)">{{ payingOrderId === row.id ? '...' : '支付' }}</button>
              <button v-else-if="(row.status ?? row.orderStatus) === 1" class="refund-btn" :disabled="refundingOrderId === row.id"
                @click="handleRefund(row)">{{ refundingOrderId === row.id ? '...' : '退票' }}</button>
              <span v-else class="cell-sub">--</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- Assist Create Dialog -->
    <el-dialog v-model="assistVisible" title="代客下单" width="600px" :close-on-click-modal="false">
      <el-steps :active="assistStep" finish-status="success" align-center style="margin-bottom:24px">
        <el-step title="选择影片" /><el-step title="选择场次" /><el-step title="选择座位" />
      </el-steps>
      <div v-show="assistStep === 0">
        <el-table :data="movies" highlight-current-row @current-change="onSelectMovie" max-height="300">
          <el-table-column prop="name" label="影片名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="genre" label="类型" width="80" show-overflow-tooltip>
            <template #default="{ row }"><span class="cell-nowrap">{{ row.genre || '--' }}</span></template>
          </el-table-column>
          <el-table-column label="票价" width="100">
            <template #default="{ row }"><span class="cell-nowrap">¥{{ row.basePrice || row.price || '--' }}</span></template>
          </el-table-column>
        </el-table>
      </div>
      <div v-show="assistStep === 1">
        <div v-if="assistSchedules.length===0" style="padding:40px;text-align:center;color:var(--text-muted)">该影片暂无可用场次</div>
        <el-table v-else :data="assistSchedules" highlight-current-row @current-change="onSelectSchedule" max-height="300">
          <el-table-column label="时间" width="170"><template #default="{ row }">{{ formatDateTime(row.startTime) }}</template></el-table-column>
          <el-table-column label="影厅" width="100"><template #default="{ row }">{{ row.hallName || row.hall?.name || '--' }}</template></el-table-column>
          <el-table-column label="余座" width="80"><template #default="{ row }">{{ row.availableSeats || row.availableCount || 0 }}</template></el-table-column>
        </el-table>
      </div>
      <div v-show="assistStep === 2">
        <p style="margin-bottom:12px;color:var(--text-secondary)">请选择座位（最多6个）</p>
        <div class="assist-seats">
          <span v-for="seat in assistAvailableSeats" :key="seat" class="assist-seat"
            :class="{ picked: assistSelectedSeats.includes(seat) }" @click="toggleAssistSeat(seat)">{{ seat }}</span>
        </div>
        <div v-if="assistAvailableSeats.length===0" style="padding:40px;text-align:center">加载中...</div>
      </div>
      <template #footer>
        <el-button @click="assistVisible=false">取消</el-button>
        <el-button v-if="assistStep>0" @click="assistStep--">上一步</el-button>
        <el-button v-if="assistStep<2" type="primary" :disabled="!canNextStep" @click="handleNextStep">下一步</el-button>
        <el-button v-if="assistStep===2" type="primary" :disabled="assistSelectedSeats.length===0" :loading="assistSubmitting" @click="doAssistCreate">确认下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAdminOrders, assistCreateOrder, assistPayOrder, assistRefundOrder, getSchedulesByMovie, getScheduleSeats } from '@/api/order'
import { getMovieList } from '@/api/movie'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const orders = ref([]); const loading = ref(false); const searchText = ref('')
const filterStatus = ref(''); const dateRange = ref([]); const payingOrderId = ref(null); const refundingOrderId = ref(null)

const assistVisible = ref(false); const assistStep = ref(0); const movies = ref([])
const assistSelectedMovie = ref(null); const assistSchedules = ref([])
const assistSelectedSchedule = ref(null); const assistAvailableSeats = ref([])
const assistSelectedSeats = ref([]); const assistSubmitting = ref(false)

const statusTabs = [
  { label: '全部订单', value: '' }, { label: '待支付', value: 0 },
  { label: '已完成', value: 2 }, { label: '已退款', value: 4 }
]

const filteredOrders = computed(() => {
  let list = orders.value
  if (searchText.value) { const kw = searchText.value.toLowerCase(); list = list.filter(o => (o.orderNo||'').includes(kw) || (o.movieName||'').toLowerCase().includes(kw)) }
  if (filterStatus.value !== '' && filterStatus.value != null) list = list.filter(o => (o.status ?? o.orderStatus) === Number(filterStatus.value))
  if (dateRange.value?.length === 2) { const s = dateRange.value[0]; const e = dateRange.value[1]; list = list.filter(o => { const t = (o.createTime||o.createdAt||'').toString().substring(0,10); return t >= s && t <= e }) }
  return list
})

const today = new Date().toISOString().split('T')[0]

const pendingCount = computed(() => orders.value.filter(o => (o.status ?? o.orderStatus) === 0).length)

const todayStats = computed(() => {
  const todayOrders = orders.value.filter(o => (o.createTime || o.createdAt || '').toString().substring(0, 10) === today)
  const paid = todayOrders.filter(o => { const s = o.status ?? o.orderStatus; return s === 1 || s === 2 })
  const revenue = paid.reduce((s, o) => s + Number(o.totalAmount || o.totalPrice || 0), 0)
  return { count: todayOrders.length, revenue: isNaN(revenue) ? 0 : revenue }
})

const todayCount = computed(() => todayStats.value.count)
const todayRevenue = computed(() => todayStats.value.revenue)

function fmtMoney(n) { const v = Number(n || 0); return v >= 10000 ? (v/10000).toFixed(1)+'万' : v.toFixed(1) }

const canNextStep = computed(() => assistStep.value === 0 ? !!assistSelectedMovie.value : assistStep.value === 1 ? !!assistSelectedSchedule.value : true)

function statusLabel(s) { const m = {0:'待支付',1:'待观影',2:'已完成',3:'已改签',4:'已退票',5:'已过期'}; return m[s] ?? '--' }
function statusColor(row) { const m = {0:'warn',1:'ok',2:'ok',3:'info',4:'danger',5:'gray'}; return m[row.status ?? row.orderStatus] ?? 'gray' }
function payLabel(row) { const s = row.status ?? row.orderStatus; return s === 0 ? '--' : s === 4 ? '已退款' : '已支付' }

function formatDateTime(s) { if(!s) return '--'; try { const d = new Date(s); if(isNaN(d.getTime())) return '--'; return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` } catch { return '--' } }

function formatSeats(row) {
  const seats = row.seatNumbers || row.seats; if (!seats) return '--'
  if (Array.isArray(seats)) return seats.join('、'); if (typeof seats === 'string') return seats.split(',').join('、'); return String(seats)
}

function onImgError(e) { e.target.style.display = 'none'; e.target.nextElementSibling.style.display = 'flex' }

async function fetchOrders() { loading.value = true; try { const res = await getAdminOrders({ size: 999 }); orders.value = Array.isArray(res.data) ? res.data : (res.data?.records || []) } catch { orders.value = [] } finally { loading.value = false } }

function openAssistCreate() { assistStep.value = 0; assistSelectedMovie.value = null; assistSelectedSchedule.value = null; assistSelectedSeats.value = []; assistAvailableSeats.value = []; assistSchedules.value = []; movies.value = []; assistVisible.value = true; loadAssistMovies() }

async function loadAssistMovies() { try { const res = await getMovieList(); movies.value = Array.isArray(res.data) ? res.data : (res.data?.records || []) } catch { movies.value = [] } }
function onSelectMovie(row) { assistSelectedMovie.value = row }
function onSelectSchedule(row) { assistSelectedSchedule.value = row }

async function handleNextStep() {
  if (assistStep.value === 0) { assistStep.value = 1; try { const res = await getSchedulesByMovie(assistSelectedMovie.value.id); assistSchedules.value = res.data || [] } catch { assistSchedules.value = [] } }
  else if (assistStep.value === 1) {
    assistStep.value = 2; try {
      const res = await getScheduleSeats(assistSelectedSchedule.value.id); const data = res.data
      if (data.seats && Array.isArray(data.seats)) { const flat = data.seats[0]&&Array.isArray(data.seats[0]) ? data.seats.flat() : data.seats; assistAvailableSeats.value = flat.filter(s => s.status===0).map(s => s.seatNumber || `${String.fromCharCode(64+(s.seatRow||s.row||1))}-${String(s.seatCol||s.col||1).padStart(2,'0')}`) }
      else { const rows = data.rowCount||8; const cols = data.colCount||12; assistAvailableSeats.value = []; for(let r=1;r<=rows;r++) for(let c=1;c<=cols;c++) assistAvailableSeats.value.push(`${String.fromCharCode(64+r)}-${String(c).padStart(2,'0')}`) }
    } catch { assistAvailableSeats.value = [] }
  }
}

function toggleAssistSeat(seat) { const i = assistSelectedSeats.value.indexOf(seat); if(i>=0) assistSelectedSeats.value.splice(i,1); else { if(assistSelectedSeats.value.length>=6) { ElMessage.warning('最多选6个座位'); return } assistSelectedSeats.value.push(seat) } }

async function doAssistCreate() { assistSubmitting.value = true; try { await assistCreateOrder({ scheduleId: assistSelectedSchedule.value.id, seatNumbers: [...assistSelectedSeats.value] }); ElMessage.success('代客下单成功'); assistVisible.value = false; await fetchOrders() } catch {} finally { assistSubmitting.value = false } }

async function handlePay(row) {
  try { await ElMessageBox.confirm(`确认为订单 ${row.orderNo} 支付 ¥${row.totalAmount||row.totalPrice}？`, '确认支付', { confirmButtonText:'确认', cancelButtonText:'取消', type:'warning' }) } catch { return }
  payingOrderId.value = row.id; try { await assistPayOrder(row.id); ElMessage.success('支付成功'); await fetchOrders() } catch {} finally { payingOrderId.value = null }
}

async function handleRefund(row) {
  try { await ElMessageBox.confirm(`确认为订单 ${row.orderNo}（¥${row.totalAmount||row.totalPrice}）办理退票？退款将原路返回。`, '确认退票', { confirmButtonText:'确认退票', cancelButtonText:'取消', type:'warning' }) } catch { return }
  refundingOrderId.value = row.id; try { await assistRefundOrder(row.id); ElMessage.success('退票成功'); await fetchOrders() } catch {} finally { refundingOrderId.value = null }
}

onMounted(fetchOrders)
</script>

<style scoped>
.order-page { min-height: 100vh; background: var(--bg-primary); }
.order-container { max-width: 1400px; margin: 0 auto; padding: 24px 32px; }

/* ---- Hero ---- */
.page-hero { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
.page-hero__title { font-family: Georgia,'Noto Serif SC',serif; font-size: 32px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.page-hero__desc { font-size: 14px; color: var(--text-secondary); }
.golden-btn { display: inline-flex; align-items: center; gap: 6px; padding: 10px 22px; border: none; border-radius: var(--radius-lg); font-size: 13px; font-weight: 700; color: #fff; background: var(--color-primary); cursor: pointer; font-family: inherit; }
.golden-btn:hover { filter: brightness(1.1); }
[data-theme='dark'] .golden-btn { color: #2a1800; background: linear-gradient(135deg,#e8a850,#ffc67c,#e8a850); }

/* ---- Stats Row ---- */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl); padding: 20px 24px; box-shadow: var(--shadow-light); transition: transform 0.2s ease; }
.stat-card:hover { transform: translateY(-2px); }
.stat-card__label { display: block; font-size: 11px; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 6px; }
.stat-card__val { font-family: 'JetBrains Mono','Consolas',monospace; font-size: 28px; font-weight: 700; color: var(--text-primary); }
.stat-card__val.accent { color: var(--color-primary); }
.stat-card__trend { display: flex; align-items: center; gap: 2px; margin-top: 6px; font-size: 11px; font-weight: 600; color: var(--text-tertiary); }
.stat-card__trend.up { color: var(--color-emerald); }
.stat-card__trend.warn { color: #aa3240; }
.stat-card__trend .material-symbols-outlined { font-size: 14px; }

/* ---- Filter ---- */
.filter-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl); padding: 0 20px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.filter-tabs { display: flex; gap: 0; }
.filter-tab { padding: 16px 20px; border: none; border-bottom: 2px solid transparent; background: transparent; font-size: 13px; font-weight: 500; color: var(--text-secondary); cursor: pointer; font-family: inherit; transition: all 0.2s; }
.filter-tab:hover { color: var(--text-primary); }
.filter-tab.active { color: var(--color-primary); border-bottom-color: var(--color-primary); font-weight: 600; }
.filter-actions { display: flex; gap: 10px; align-items: center; padding: 10px 0; }

/* ---- Table ---- */
.table-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl); overflow: hidden; box-shadow: var(--shadow-light); }
.movie-cell { display: flex; align-items: center; gap: 10px; }
.movie-poster { width: 36px; height: 50px; border-radius: 4px; object-fit: cover; border: 1px solid var(--border-light); }
.movie-poster-placeholder { width: 36px; height: 50px; border-radius: 4px; background: var(--bg-hover); display: flex; align-items: center; justify-content: center; color: var(--text-tertiary); }
.movie-name { display: block; font-weight: 600; color: var(--text-primary); font-size: 13px; }
.movie-sub { display: block; font-size: 11px; color: var(--text-tertiary); margin-top: 1px; }

.order-no { font-family: 'JetBrains Mono',monospace; font-size: 12px; color: var(--text-secondary); }
.cell-main { display: block; font-size: 13px; color: var(--text-primary); }
.cell-sub { display: block; font-size: 11px; color: var(--text-tertiary); margin-top: 1px; }
.price-val { font-family: 'JetBrains Mono',monospace; font-weight: 700; color: var(--color-primary); font-size: 14px; }

.status-badge { display: inline-block; padding: 3px 10px; border-radius: var(--radius-pill); font-size: 11px; font-weight: 600; }
.badge-warn { background: rgba(245,166,35,0.1); color: var(--color-warning); }
.badge-ok { background: rgba(45,207,138,0.1); color: var(--color-emerald); }
.badge-info { background: rgba(91,141,239,0.1); color: var(--color-info); }
.badge-danger { background: rgba(232,64,64,0.08); color: var(--color-danger); }
.badge-gray { background: var(--bg-hover); color: var(--text-tertiary); }

.cell-nowrap { white-space: nowrap; }

.pay-btn { padding: 4px 14px; border: 1px solid var(--color-primary); border-radius: var(--radius-sm); background: transparent; color: var(--color-primary); font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; }
.pay-btn:hover { background: var(--color-primary); color: #fff; }
.pay-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.refund-btn { padding: 4px 14px; border: 1px solid var(--color-danger); border-radius: var(--radius-sm); background: transparent; color: var(--color-danger); font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; }
.refund-btn:hover { background: var(--color-danger); color: #fff; }
.refund-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.assist-seats { display: flex; flex-wrap: wrap; gap: 8px; max-height: 300px; overflow-y: auto; padding: 8px; background: var(--bg-secondary); border-radius: var(--radius-md); }
.assist-seat { width: 52px; text-align: center; padding: 6px 2px; border-radius: 4px; background: var(--bg-card); border: 1px solid var(--border-color); cursor: pointer; font-size: 11px; transition: all 0.15s; }
.assist-seat:hover { border-color: var(--color-primary); }
.assist-seat.picked { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }

@media (max-width: 768px) { .stats-row { grid-template-columns: repeat(2, 1fr); } .filter-card { flex-direction: column; } }
</style>
