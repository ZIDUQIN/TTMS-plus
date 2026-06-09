<template>
  <div class="admin-layout">
    <NavBar />
    <div class="admin-content">
      <div class="page-header">
        <h2>订单管理</h2>
        <el-button type="primary" :icon="Plus" @click="openAssistCreate">代客下单</el-button>
      </div>

      <!-- Filters -->
      <div class="toolbar">
        <el-input v-model="searchText" placeholder="搜索订单号..." :prefix-icon="Search" clearable style="width: 240px;" />
        <el-select v-model="filterStatus" placeholder="订单状态" clearable style="width: 140px;">
          <el-option label="全部" value="" />
          <el-option label="待支付" :value="0" />
          <el-option label="待观影" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="已改签" :value="3" />
          <el-option label="已退票" :value="4" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px;"
        />
        <el-button :icon="Refresh" @click="fetchOrders">刷新</el-button>
      </div>

      <div class="card">
        <el-table :data="filteredOrders" v-loading="loading" stripe>
          <el-table-column prop="orderNo" label="订单号" width="170" show-overflow-tooltip />
          <el-table-column prop="movieName" label="影片" min-width="130" show-overflow-tooltip />
          <el-table-column prop="hallName" label="影厅" width="100" />
          <el-table-column label="座位" width="160">
            <template #default="{ row }"><span style="white-space: nowrap;">{{ formatSeats(row) }}</span></template>
          </el-table-column>
          <el-table-column label="金额" width="100">
            <template #default="{ row }">¥{{ row.totalAmount || row.totalPrice }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status ?? row.orderStatus)" size="small">
                {{ statusLabel(row.status ?? row.orderStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="场次时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.scheduleStartTime || row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime || row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="用户名" width="100">
            <template #default="{ row }">{{ row.username || '--' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="(row.status ?? row.orderStatus) === 0"
                type="primary"
                size="small"
                :loading="payingOrderId === row.id"
                @click="handlePay(row)"
              >
                支付
              </el-button>
              <span v-else style="color: var(--text-muted); font-size: 12px;">--</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Assist Create Dialog -->
      <el-dialog v-model="assistVisible" title="代客下单" width="560px" :close-on-click-modal="false" :before-close="handleAssistClose">
        <el-steps :active="assistStep" finish-status="success" align-center style="margin-bottom: 24px;">
          <el-step title="选择影片" />
          <el-step title="选择场次" />
          <el-step title="选择座位" />
        </el-steps>
        <div v-show="assistStep === 0">
          <el-table :data="movies" highlight-current-row @current-change="onSelectMovie" max-height="300">
            <el-table-column prop="name" label="影片名称" />
            <el-table-column prop="genre" label="类型" width="80" />
            <el-table-column label="票价" width="80">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
          </el-table>
        </div>
        <div v-show="assistStep === 1">
          <div v-if="assistSchedules.length === 0" style="padding: 40px; text-align: center; color: var(--text-muted);">
            该影片暂无可用场次
          </div>
          <el-table v-else :data="assistSchedules" highlight-current-row @current-change="onSelectSchedule" max-height="300">
            <el-table-column label="场次时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column prop="hallName" label="影厅" width="100">
              <template #default="{ row }">{{ row.hallName || row.hall?.name || '--' }}</template>
            </el-table-column>
            <el-table-column label="余座" width="80">
              <template #default="{ row }">{{ row.availableSeats || row.availableCount || 0 }}</template>
            </el-table-column>
          </el-table>
        </div>
        <div v-show="assistStep === 2">
          <p style="margin-bottom: 12px; color: var(--text-secondary);">请选择座位（最多6个）</p>
          <div class="assist-seats">
            <span
              v-for="seat in assistAvailableSeats"
              :key="seat"
              class="assist-seat-item"
              :class="{ picked: assistSelectedSeats.includes(seat) }"
              @click="toggleAssistSeat(seat)"
            >{{ seat }}</span>
          </div>
          <div v-if="assistAvailableSeats.length === 0" style="padding: 40px; text-align: center; color: var(--text-muted);">
            正在加载座位信息...
          </div>
        </div>
        <template #footer>
          <el-button @click="assistVisible = false">取消</el-button>
          <el-button v-if="assistStep > 0" @click="assistStep--">上一步</el-button>
          <el-button
            v-if="assistStep < 2"
            type="primary"
            :disabled="!canNextStep"
            @click="handleNextStep"
          >下一步</el-button>
          <el-button
            v-if="assistStep === 2"
            type="primary"
            :disabled="assistSelectedSeats.length === 0"
            :loading="assistSubmitting"
            @click="doAssistCreate"
          >确认下单</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAdminOrders, assistCreateOrder, assistPayOrder, getSchedulesByMovie, getScheduleSeats } from '@/api/order'
import { getMovieList } from '@/api/movie'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

const orders = ref([])
const loading = ref(false)
const searchText = ref('')
const filterStatus = ref('')
const dateRange = ref([])

// Assist create state
const assistVisible = ref(false)
const assistStep = ref(0)
const movies = ref([])
const assistSelectedMovie = ref(null)
const assistSchedules = ref([])
const assistSelectedSchedule = ref(null)
const assistAvailableSeats = ref([])
const assistSelectedSeats = ref([])
const assistSubmitting = ref(false)
const payingOrderId = ref(null)

const filteredOrders = computed(() => {
  let list = orders.value
  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    list = list.filter(o =>
      (o.orderNo || '').toLowerCase().includes(kw) ||
      (o.movieName || '').toLowerCase().includes(kw)
    )
  }
  if (filterStatus.value !== '' && filterStatus.value != null) {
    list = list.filter(o => (o.status ?? o.orderStatus) === Number(filterStatus.value))
  }
  if (dateRange.value && dateRange.value.length === 2) {
    list = list.filter(o => {
      try {
        const raw = o.createTime || o.createdAt || ''
        const createTime = typeof raw === 'string' ? raw.substring(0, 10) : ''
        return createTime >= dateRange.value[0] && createTime <= dateRange.value[1]
      } catch { return true }
    })
  }
  return list
})

const canNextStep = computed(() => {
  if (assistStep.value === 0) return !!assistSelectedMovie.value
  if (assistStep.value === 1) return !!assistSelectedSchedule.value
  return true
})

function statusLabel(s) {
  const map = { 0: '待支付', 1: '待观影', 2: '已完成', 3: '已改签', 4: '已退票', 5: '已过期' }
  return map[s] !== undefined ? map[s] : (s ?? '--')
}
function statusType(s) {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info', 4: 'danger', 5: 'info' }
  return map[s] ?? 'info'
}

function formatDateTime(s) {
  if (!s) return '--'
  try {
    const d = new Date(s)
    if (isNaN(d.getTime())) return '--'
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  } catch { return '--' }
}

// 格式化座位号：后端返回逗号分隔字符串（如 "A-01,B-02"），需分割后用顿号拼接
function formatSeats(row) {
  const seats = row.seatNumbers || row.seats
  if (!seats) return '--'
  if (Array.isArray(seats)) return seats.join('、')
  if (typeof seats === 'string') return seats.split(',').join('、')
  return String(seats)
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getAdminOrders({ size: 999 })
    // 兼容多种API响应格式
    if (Array.isArray(res.data)) {
      orders.value = res.data
    } else if (res.data?.records) {
      orders.value = res.data.records
    } else if (res.data) {
      orders.value = res.data
    } else {
      orders.value = []
    }
  } catch (err) {
    console.error('获取订单列表失败:', err)
    orders.value = []
  } finally { loading.value = false }
}

function handleAssistClose() {
  // Always allow closing the dialog
  assistVisible.value = false
}

function openAssistCreate() {
  assistStep.value = 0
  assistSelectedMovie.value = null
  assistSelectedSchedule.value = null
  assistSelectedSeats.value = []
  assistAvailableSeats.value = []
  assistSchedules.value = []
  movies.value = []
  assistVisible.value = true
  // Load movies with proper error handling
  loadAssistMovies()
}

async function loadAssistMovies() {
  try {
    const res = await getMovieList()
    movies.value = Array.isArray(res.data) ? res.data : (res.data?.records || [])
  } catch (err) {
    movies.value = []
  }
}

function onSelectMovie(row) {
  assistSelectedMovie.value = row
}

function onSelectSchedule(row) {
  assistSelectedSchedule.value = row
}

async function handleNextStep() {
  if (assistStep.value === 0) {
    assistStep.value = 1
    // Load schedules for selected movie
    try {
      const res = await getSchedulesByMovie(assistSelectedMovie.value.id)
      assistSchedules.value = res.data || []
    } catch (err) { assistSchedules.value = [] }
  } else if (assistStep.value === 1) {
    assistStep.value = 2
    // Load seat map
    try {
      const res = await getScheduleSeats(assistSelectedSchedule.value.id)
      const data = res.data
      if (data.seats && Array.isArray(data.seats)) {
        // Flatten 2D seat matrix to 1D list
        const flatSeats = data.seats[0] && Array.isArray(data.seats[0]) ? data.seats.flat() : data.seats
        assistAvailableSeats.value = flatSeats
          .filter(s => s.status === 0)
          .map(s => {
            // Use seatNumber from backend if available, otherwise construct from row letter + col
            if (s.seatNumber) return s.seatNumber
            const rowLetter = String.fromCharCode(64 + (s.seatRow || s.row || 1))
            const colNum = String(s.seatCol || s.col || 1).padStart(2, '0')
            return rowLetter + '-' + colNum
          })
      } else {
        // Generate placeholder seats
        const rows = data.rowCount || 8
        const cols = data.colCount || 12
        assistAvailableSeats.value = []
        for (let r = 1; r <= rows; r++) {
          for (let c = 1; c <= cols; c++) {
            assistAvailableSeats.value.push(`${String.fromCharCode(64+r)}-${String(c).padStart(2,'0')}`)
          }
        }
      }
    } catch (err) { assistAvailableSeats.value = [] }
  }
}

function toggleAssistSeat(seat) {
  const idx = assistSelectedSeats.value.indexOf(seat)
  if (idx >= 0) {
    assistSelectedSeats.value.splice(idx, 1)
  } else {
    if (assistSelectedSeats.value.length >= 6) {
      ElMessage.warning('最多选6个座位')
      return
    }
    assistSelectedSeats.value.push(seat)
  }
}

async function doAssistCreate() {
  assistSubmitting.value = true
  try {
    await assistCreateOrder({
      scheduleId: assistSelectedSchedule.value.id,
      seatNumbers: [...assistSelectedSeats.value]
    })
    ElMessage.success('代客下单成功')
    assistVisible.value = false
    // 必须等待订单列表刷新完毕，避免支付时拿到旧数据
    await fetchOrders()
  } catch (err) {
    console.error('代客下单失败:', err)
  } finally {
    assistSubmitting.value = false
  }
}

async function handlePay(row) {
  try {
    await ElMessageBox.confirm(
      `确认为订单 ${row.orderNo} 支付 ¥${row.totalAmount || row.totalPrice}？`,
      '确认支付',
      { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  if (!row.id) {
    ElMessage.error('订单ID无效，请刷新页面后重试')
    return
  }
  payingOrderId.value = row.id
  try {
    await assistPayOrder(row.id)
    ElMessage.success('支付成功')
    await fetchOrders()
  } catch (err) {
    console.error('代客支付失败:', err, '订单ID:', row.id)
  } finally {
    payingOrderId.value = null
  }
}

onMounted(fetchOrders)
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
.card { background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-light); }
.assist-seats { display: flex; flex-wrap: wrap; gap: 8px; max-height: 300px; overflow-y: auto; padding: 8px; background: var(--bg-secondary); border-radius: var(--radius-md); }
.assist-seat-item { width: 56px; text-align: center; padding: 6px 4px; border-radius: 4px; background: var(--bg-card); border: 1px solid var(--border-color); cursor: pointer; font-size: 12px; transition: all 0.2s; }
.assist-seat-item:hover { border-color: var(--color-primary); }
.assist-seat-item.picked { background: var(--color-warning); color: #fff; border-color: var(--color-warning); }
</style>
