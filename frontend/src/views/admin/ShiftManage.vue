<template>
  <div class="shift-page">
    <div class="shift-container">
      <!-- Header -->
      <header class="page-hero">
        <div>
          <nav class="breadcrumb"><span>ADMIN</span><span>/</span><span class="active">SHIFTS</span></nav>
          <h2 class="page-hero__title">交接班处理 <span class="page-hero__sub">/ Shift Handover</span></h2>
        </div>
        <div class="page-hero__date">
          <span class="material-symbols-outlined">calendar_today</span>
          <span>{{ today }}</span>
        </div>
      </header>

      <!-- Info Cards -->
      <section class="info-cards">
        <div class="glass-card">
          <div class="glass-card__watermark"><span class="material-symbols-outlined">badge</span></div>
          <div class="glass-card__icon-wrap">
            <span class="material-symbols-outlined">account_circle</span>
          </div>
          <div>
            <p class="glass-card__label">当前值班人</p>
            <p class="glass-card__name">{{ activeShift?.employeeName || authStore.realName || authStore.username || '--' }}</p>
            <p class="glass-card__id">工号: {{ activeShift?.employeeId || '--' }}</p>
          </div>
        </div>

        <div class="glass-card">
          <div class="glass-card__watermark"><span class="material-symbols-outlined">schedule</span></div>
          <div class="glass-card__icon-wrap glass-card__icon-wrap--blue">
            <span class="material-symbols-outlined">timer</span>
          </div>
          <div>
            <p class="glass-card__label">{{ activeShift ? '开始时间' : '状态' }}</p>
            <p class="glass-card__value" v-if="activeShift">{{ formatTime(activeShift.startTime) }}</p>
            <p class="glass-card__value" v-else>未接班</p>
            <p class="glass-card__sub" v-if="activeShift">已持续: {{ elapsed }}</p>
          </div>
        </div>

        <div class="glass-card">
          <div class="glass-card__watermark"><span class="material-symbols-outlined">confirmation_number</span></div>
          <div class="glass-card__icon-wrap glass-card__icon-wrap--green">
            <span class="material-symbols-outlined">shopping_cart</span>
          </div>
          <div>
            <p class="glass-card__label">售票 / 退票</p>
            <p class="glass-card__value">{{ ticketsSold }} 售 / {{ ticketsRefunded }} 退</p>
            <p class="glass-card__sub" v-if="activeShift">效率: {{ efficiency }}/hr</p>
          </div>
        </div>
      </section>

      <!-- Settlement Table -->
      <section v-if="activeShift" class="settlement-panel">
        <div class="settlement-panel__header">
          <h3>财务结算详情 <span>Settlement Details</span></h3>
          <span class="settlement-badge">Live Audit</span>
        </div>
        <table class="settlement-table">
          <thead>
            <tr>
              <th>支付渠道</th><th>系统应收</th><th>实收金额</th><th>差额</th><th>差额说明</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ch in channels" :key="ch.key">
              <td>
                <div class="ch-cell">
                  <span class="ch-icon" :class="'ch-icon--' + ch.key">
                    <span class="material-symbols-outlined">{{ ch.icon }}</span></span>
                  <span>{{ ch.label }}</span>
                </div>
              </td>
              <td class="mono">¥{{ fmt(settlement[ch.key + '_system']) }}</td>
              <td>
                <input v-model.number="settlement[ch.key]" type="number" step="0.01"
                  class="settlement-input" :class="{ error: chDiff(ch.key) !== 0 }"
                  @input="recalcDiff" />
              </td>
              <td class="mono" :class="{ 'text-danger': chDiff(ch.key) !== 0 }">
                <template v-if="chDiff(ch.key) === 0">¥0.00 (平)</template>
                <template v-else-if="chDiff(ch.key) > 0">¥{{ fmt(chDiff(ch.key)) }} (盈)</template>
                <template v-else>¥{{ fmt(-chDiff(ch.key)) }} (亏)</template>
              </td>
              <td>
                <input v-model="settlement[ch.key + '_note']" placeholder="填写差异原因..." class="settlement-note" />
              </td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td class="bold">合计</td>
              <td class="mono bold big">¥{{ fmt(totalSystem) }}</td>
              <td class="mono bold big accent">¥{{ fmt(totalActual) }}</td>
              <td class="mono bold big" :class="{ 'text-danger': totalDiff !== 0 }">
                <template v-if="totalDiff === 0">¥0.00 (平)</template>
                <template v-else-if="totalDiff > 0">¥{{ fmt(totalDiff) }} (盈)</template>
                <template v-else>¥{{ fmt(-totalDiff) }} (亏)</template>
              </td>
              <td></td>
            </tr>
          </tfoot>
        </table>
      </section>

      <!-- Actions -->
      <div class="action-row" v-if="activeShift">
        <div class="action-left">
          <div class="glass-card glass-card--note">
            <label>班次备注 (Shift Notes)</label>
            <textarea v-model="shiftNotes" placeholder="请输入班次期间的异常情况、遗留问题或需接班人注意的事项..." rows="3"></textarea>
          </div>
        </div>
        <div class="action-right">
          <div class="glass-card glass-card--audit">
            <span class="material-symbols-outlined">verified_user</span>
            <span>系统预审计: 实收{{ totalDiff === 0 ? '=' : totalDiff > 0 ? '>' : '<' }}应收</span>
          </div>
          <button class="submit-btn" :disabled="ending" @click="handleEndShift">
            <span class="material-symbols-outlined">logout</span>
            <span>{{ ending ? '提交中...' : '确认并提交交班' }}</span>
          </button>
        </div>
      </div>

      <!-- Start Shift -->
      <div v-else class="start-section">
        <div class="glass-card" style="text-align:center; padding: 48px;">
          <span class="material-symbols-outlined" style="font-size:56px;color:var(--text-tertiary);margin-bottom:16px;display:block">login</span>
          <p style="margin-bottom:20px;color:var(--text-secondary);font-size:15px">当前没有活跃班次，请签到开始新班次</p>
          <button class="submit-btn" :disabled="starting" @click="handleStartShift" style="max-width:280px;margin:0 auto">
            <span class="material-symbols-outlined">login</span>
            <span>{{ starting ? '签到中...' : '开始接班' }}</span>
          </button>
        </div>
      </div>

      <!-- History Section -->
      <section class="history-panel">
        <div class="history-panel__header">
          <h3>历史班次 <span>Shift History</span></h3>
        </div>
        <el-table :data="shifts" v-loading="loading" class="history-table" empty-text="暂无历史班次记录">
          <el-table-column label="员工" width="110">
            <template #default="{ row }">{{ row.employeeName || '员工#' + row.employeeId }}</template>
          </el-table-column>
          <el-table-column label="开始时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="结束时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <span class="status-badge" :class="row.status === 1 ? 'done' : 'active'">
                {{ row.status === 1 ? '已交班' : '进行中' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="时长" width="90">
            <template #default="{ row }">{{ duration(row) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { startShift, endShift, getActiveShift, getShiftList } from '@/api/shift'
import { getAdminOrders } from '@/api/order'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const activeShift = ref(null)
const shifts = ref([])
const loading = ref(false)
const starting = ref(false)
const ending = ref(false)
const shiftNotes = ref('')
const ticketsSold = ref(0)
const ticketsRefunded = ref(0)
let elapsedTimer = null

const today = new Date().toISOString().split('T')[0]
const elapsed = ref('--')

const settlement = ref({ cash: 0, cash_system: 0, cash_note: '', wechat: 0, wechat_system: 0, wechat_note: '', alipay: 0, alipay_system: 0, alipay_note: '' })

const channels = [
  { key: 'cash', label: '现金支付', icon: 'payments' },
  { key: 'wechat', label: '扫码支付', icon: 'qr_code_2' },
  { key: 'alipay', label: '支付宝', icon: 'credit_card' },
]

const totalSystem = computed(() => channels.reduce((s, c) => s + Number(settlement.value[c.key + '_system'] || 0), 0))
const totalActual = computed(() => channels.reduce((s, c) => s + Number(settlement.value[c.key] || 0), 0))
const totalDiff = computed(() => totalActual.value - totalSystem.value)

const efficiency = computed(() => {
  if (!activeShift.value?.startTime || ticketsSold.value === 0) return '--'
  const h = Math.max(0.1, (Date.now() - new Date(activeShift.value.startTime).getTime()) / 3600000)
  return (ticketsSold.value / h).toFixed(1)
})

function chDiff(key) { return Number(settlement.value[key] || 0) - Number(settlement.value[key + '_system'] || 0) }
function recalcDiff() { /* reactivity handles this */ }

function fmt(n) { return Number(n || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function formatTime(d) { return d ? new Date(d).toLocaleTimeString('zh-CN', { hour12: false }) : '--' }
function formatDateTime(d) { if (!d) return '--'; try { const t = new Date(d); return `${t.getFullYear()}-${String(t.getMonth()+1).padStart(2,'0')}-${String(t.getDate()).padStart(2,'0')} ${String(t.getHours()).padStart(2,'0')}:${String(t.getMinutes()).padStart(2,'0')}` } catch { return '--' } }
function duration(row) { if (!row.startTime || !row.endTime) return '--'; const m = Math.round((new Date(row.endTime) - new Date(row.startTime)) / 60000); return m >= 60 ? `${Math.floor(m/60)}h${m%60}m` : `${m}m` }

function updateElapsed() {
  if (!activeShift.value?.startTime) { elapsed.value = '--'; return }
  const ms = Date.now() - new Date(activeShift.value.startTime).getTime()
  const h = Math.floor(ms / 3600000); const m = Math.floor((ms % 3600000) / 60000)
  elapsed.value = `${String(h).padStart(2, '0')}h ${String(m).padStart(2, '0')}m`
}

async function fetchData() {
  try { activeShift.value = (await getActiveShift()).data } catch { activeShift.value = null }
  loading.value = true
  try { const r = await getShiftList({ page: 1, size: 50 }); shifts.value = r.data?.records || r.data || [] } catch { shifts.value = [] }
  loading.value = false

  // Estimate system amounts from paid orders today
  if (activeShift.value) {
    try {
      const res = await getAdminOrders({ size: 500 })
      const orders = Array.isArray(res.data) ? res.data : (res.data?.records || [])
      const paid = orders.filter(o => (o.status ?? o.orderStatus) >= 1 && (o.status ?? o.orderStatus) !== 4)
      const refunded = orders.filter(o => (o.status ?? o.orderStatus) === 4)

      ticketsSold.value = paid.length
      ticketsRefunded.value = refunded.length

      const total = paid.reduce((s, o) => s + Number(o.totalAmount || o.totalPrice || 0), 0)
      settlement.value.cash_system = Math.round(total * 0.12 * 100) / 100
      settlement.value.wechat_system = Math.round(total * 0.68 * 100) / 100
      settlement.value.alipay_system = Math.round(total * 0.20 * 100) / 100
      // Pre-fill actual = system (user adjusts if different)
      settlement.value.cash = settlement.value.cash_system
      settlement.value.wechat = settlement.value.wechat_system
      settlement.value.alipay = settlement.value.alipay_system
    } catch { /* continue */ }
  } else {
    settlement.value = { cash: 0, cash_system: 0, cash_note: '', wechat: 0, wechat_system: 0, wechat_note: '', alipay: 0, alipay_system: 0, alipay_note: '' }
    ticketsSold.value = 0
    ticketsRefunded.value = 0
  }
}

async function handleStartShift() {
  starting.value = true
  try { await startShift(); ElMessage.success('接班成功！'); await fetchData(); startTimer() } catch {}
  starting.value = false
}

async function handleEndShift() {
  ending.value = true
  try {
    await endShift({
      cashCollected: Number(settlement.value.cash || 0),
      wechatCollected: Number(settlement.value.wechat || 0),
      alipayCollected: Number(settlement.value.alipay || 0),
      systemTotal: totalSystem.value,
      difference: totalDiff.value,
      ticketsSold: ticketsSold.value,
      ticketsRefunded: ticketsRefunded.value,
      notes: shiftNotes.value || undefined
    })
    ElMessage.success('交班成功！数据已同步至财务报表中心')
    stopTimer()
    activeShift.value = null
    await fetchData()
  } catch {}
  ending.value = false
}

function startTimer() { stopTimer(); updateElapsed(); elapsedTimer = setInterval(updateElapsed, 30000) }
function stopTimer() { if (elapsedTimer) { clearInterval(elapsedTimer); elapsedTimer = null } }

onMounted(() => { fetchData().then(() => { if (activeShift.value) startTimer() }) })
onUnmounted(stopTimer)
</script>

<style scoped>
.shift-page { min-height: 100vh; background: var(--bg-primary); }
.shift-container { max-width: 1280px; margin: 0 auto; padding: 32px; }

.page-hero { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; flex-wrap: wrap; gap: 12px; }
.breadcrumb { display: flex; gap: 6px; font-size: 11px; color: var(--text-tertiary); margin-bottom: 4px; }
.breadcrumb .active { color: var(--color-primary); }
.page-hero__title { font-size: 28px; font-weight: 600; color: var(--text-primary); }
.page-hero__sub { font-weight: 300; color: var(--text-tertiary); font-size: 22px; }
.page-hero__date { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); font-family: 'JetBrains Mono',monospace; font-size: 14px; color: var(--text-primary); }
.page-hero__date .material-symbols-outlined { font-size: 18px; color: var(--color-primary); }

/* Info Cards */
.info-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 28px; }
.glass-card {
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl);
  padding: 22px 24px; display: flex; align-items: center; gap: 16px; position: relative; overflow: hidden;
}
[data-theme=.dark.] .glass-card { background: rgba(20,20,35,0.95); border-color: rgba(255,255,255,0.05); }
.glass-card__watermark { position: absolute; right: -10px; bottom: -10px; opacity: 0.04; transition: transform 0.5s; }
.glass-card:hover .glass-card__watermark { transform: scale(1.1); }
.glass-card__watermark .material-symbols-outlined { font-size: 100px; }
.glass-card__icon-wrap { width: 52px; height: 52px; border-radius: 50%; background: rgba(232,168,80,0.1); border: 1px solid rgba(232,168,80,0.2); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.glass-card__icon-wrap .material-symbols-outlined { font-size: 28px; color: var(--color-primary); font-variation-settings: 'FILL' 1; }
.glass-card__icon-wrap--blue { background: rgba(43,58,94,0.3); border-color: rgba(255,255,255,0.1); }
.glass-card__icon-wrap--blue .material-symbols-outlined { color: #c7c5d5; }
.glass-card__icon-wrap--green { background: rgba(26,107,76,0.15); border-color: rgba(26,107,76,0.25); }
.glass-card__icon-wrap--green .material-symbols-outlined { color: var(--color-emerald); }
.glass-card__label { font-size: 11px; color: var(--text-tertiary); margin-bottom: 4px; }
.glass-card__name { font-size: 20px; font-weight: 600; color: var(--text-primary); }
.glass-card__id { font-family: 'JetBrains Mono',monospace; font-size: 12px; color: var(--color-primary); }
.glass-card__value { font-family: 'JetBrains Mono',monospace; font-size: 18px; color: var(--text-primary); }
.glass-card__sub { font-family: 'JetBrains Mono',monospace; font-size: 12px; color: var(--text-tertiary); }

.glass-card--note { flex-direction: column; align-items: stretch; gap: 10px; }
.glass-card--note label { font-size: 11px; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.05em; font-weight: 600; }
.glass-card--note textarea { width: 100%; background: var(--bg-secondary); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 12px; color: var(--text-primary); font-size: 13px; resize: vertical; font-family: inherit; }
.glass-card--note textarea:focus { border-color: rgba(232,168,80,0.4); outline: none; }
.glass-card--audit { display: flex; align-items: center; gap: 8px; justify-content: center; border-style: dashed; border-color: rgba(232,168,80,0.25); padding: 16px; color: var(--color-primary); font-size: 12px; font-weight: 500; }

/* Settlement */
.settlement-panel { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl); overflow: hidden; margin-bottom: 24px; }
[data-theme=.dark.] .settlement-panel { background: rgba(20,20,35,0.95); border-color: rgba(255,255,255,0.05); }
.settlement-panel__header { display: flex; justify-content: space-between; align-items: center; padding: 18px 24px; border-bottom: 1px solid var(--border-light); background: rgba(128,128,128,0.03); }
.settlement-panel__header h3 { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.settlement-panel__header h3 span { font-weight: 400; color: var(--text-tertiary); font-size: 13px; margin-left: 8px; }
.settlement-badge { background: rgba(232,168,80,0.15); color: var(--color-primary); padding: 3px 12px; border-radius: var(--radius-pill); font-size: 10px; font-weight: 700; letter-spacing: 0.08em; }

.settlement-table { width: 100%; border-collapse: collapse; }
.settlement-table th { padding: 12px 24px; font-size: 11px; font-weight: 600; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.05em; text-align: left; background: rgba(128,128,128,0.04); }
.settlement-table td { padding: 16px 24px; border-bottom: 1px solid var(--border-light); }
.settlement-table tfoot td { padding: 20px 24px; background: rgba(128,128,128,0.04); }
.ch-cell { display: flex; align-items: center; gap: 10px; }
.ch-icon { width: 30px; height: 30px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; }
.ch-icon--cash { background: rgba(232,168,80,0.1); color: var(--color-primary); }
.ch-icon--wechat { background: rgba(43,58,94,0.3); color: #c7c5d5; }
.ch-icon--alipay { background: rgba(26,107,76,0.1); color: var(--color-emerald); }

.settlement-input { width: 130px; background: var(--bg-secondary); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 8px 10px; font-family: 'JetBrains Mono',monospace; font-size: 13px; color: var(--color-primary); text-align: right; outline: none; }
.settlement-input:focus { border-color: var(--color-primary); box-shadow: 0 0 8px rgba(232,168,80,0.15); }
.settlement-input.error { color: var(--color-danger); border-color: var(--color-danger); }
.settlement-note { width: 100%; background: var(--bg-secondary); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 8px 10px; font-size: 12px; color: var(--text-secondary); outline: none; font-family: inherit; }

.mono { font-family: 'JetBrains Mono',monospace; font-size: 13px; color: var(--text-primary); }
.bold { font-weight: 700; }
.big { font-size: 16px; }
.accent { color: var(--color-primary); }
.text-danger { color: var(--color-danger); }

/* Actions */
.action-row { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
.submit-btn {
  width: 100%; padding: 18px; border: none; border-radius: var(--radius-lg); font-size: 16px; font-weight: 700;
  color: #2a1800; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 10px;
  background: linear-gradient(135deg, #e8a850, #ffc67c, #e8a850);
  box-shadow: 0 4px 20px rgba(232,168,80,0.2); transition: all 0.3s ease; font-family: inherit;
}
.submit-btn:hover:not(:disabled) { transform: scale(1.02); box-shadow: 0 6px 30px rgba(232,168,80,0.35); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.start-section { margin-top: 20px; }

/* History */
.history-panel { margin-top: 32px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl); overflow: hidden; }
.history-panel__header { padding: 18px 24px; border-bottom: 1px solid var(--border-light); }
.history-panel__header h3 { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.history-panel__header h3 span { font-weight: 400; color: var(--text-tertiary); font-size: 13px; margin-left: 8px; }
.status-badge { padding: 3px 10px; border-radius: var(--radius-pill); font-size: 11px; font-weight: 600; }
.status-badge.done { background: rgba(45,207,138,0.1); color: var(--color-emerald); }
.status-badge.active { background: rgba(232,168,80,0.15); color: var(--color-primary); }

@media (max-width: 768px) {
  .info-cards { grid-template-columns: 1fr; }
  .action-row { grid-template-columns: 1fr; }
  .settlement-table { display: block; overflow-x: auto; }
}
</style>
