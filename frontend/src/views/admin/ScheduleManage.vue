<template>
  <div class="admin-layout">
    <div class="admin-content schedule-page">
      <!-- Page Header -->
      <div class="s-header">
        <div>
          <h1 class="s-title">排片管理</h1>
          <p class="s-subtitle">为影厅编排放映场次，打造优质观影体验</p>
        </div>
        <div class="s-header-actions">
          <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
            批量删除 ({{ selectedIds.length }})
          </el-button>
          <el-button type="primary" size="large" :icon="Plus" @click="openAdd">新增排片</el-button>
          <el-button :icon="Plus" size="large" @click="openBatch">批量排片</el-button>
        </div>
      </div>

      <!-- Date Selector Strip -->
      <div class="date-strip-wrap">
        <div class="date-strip-label">放映日历</div>
        <div class="date-strip" ref="dateStripRef">
          <div v-for="d in dateOptions" :key="d.value"
            class="date-card" :class="{ active: selectedDate === d.value }"
            @click="selectedDate = d.value">
            <span class="date-weekday">{{ d.weekday }}</span>
            <span class="date-day">{{ d.day }}</span>
            <span v-if="selectedDate === d.value" class="date-dot"></span>
          </div>
        </div>
      </div>

      <!-- Schedule Content: Grouped by Hall -->
      <div class="schedules-area" v-loading="loading">
        <el-empty v-if="!loading && hallSchedules.length === 0" description="暂无排片数据" :image-size="120" />

        <section v-for="hall in hallSchedules" :key="hall.id" class="hall-section">
          <div class="hall-header">
            <h2 class="hall-name">{{ hall.name }}</h2>
            <span class="hall-type-tag">{{ hall.type || 'STANDARD' }}</span>
          </div>
          <div class="screening-grid">
            <!-- Screening Cards -->
            <div v-for="s in hall.schedules" :key="s.id" class="screening-card"
              :class="{ conflict: s.hasConflict }">
              <div v-if="s.hasConflict" class="conflict-badge">时间冲突</div>
              <div class="sc-body">
                <div class="sc-top">
                  <div class="sc-poster">
                    <img v-if="s.posterUrl" :src="s.posterUrl" @error="e => e.target.style.display='none'" />
                    <el-icon v-if="!s.posterUrl" :size="28"><VideoCameraFilled /></el-icon>
                  </div>
                  <div class="sc-info">
                    <span class="sc-status" :class="statusClass(s.status)">{{ statusLabel(s.status) }}</span>
                    <h4 class="sc-movie">{{ s.movieName }}</h4>
                    <div class="sc-time">
                      <el-icon :size="12"><Clock /></el-icon>
                      <span>{{ fmtTime(s.startTime) }} - {{ fmtTime(s.endTime) }}</span>
                    </div>
                  </div>
                </div>
                <div class="sc-bottom">
                  <div class="sc-seats">
                    <span>{{ s.soldCount || 0 }}/{{ (s.hallRowCount || 0) * (s.hallColCount || 0) || 240 }}</span>
                    <span class="sc-price">¥{{ s.price }}</span>
                  </div>
                  <div class="sc-progress">
                    <div class="sc-progress-fill" :style="{ width: seatPercent(s) + '%' }"></div>
                  </div>
                  <div class="sc-actions">
                    <el-button size="small" text type="primary" @click="openEdit(s)">编辑</el-button>
                    <el-button size="small" text type="danger" @click="handleDelete(s)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
            <!-- Empty slot placeholder -->
            <div class="screening-card add-card" @click="openAddForHall(hall.id)">
              <el-icon :size="32"><Plus /></el-icon>
              <span>添加场次</span>
            </div>
          </div>
        </section>
      </div>

      <!-- Footer Stats -->
      <div class="s-footer">
        <div class="sf-stats">
          <div class="sf-stat">
            <span class="sf-stat-label">总场次数</span>
            <span class="sf-stat-value">{{ totalCount }}</span>
          </div>
          <div class="sf-stat">
            <span class="sf-stat-label">平均上座率</span>
            <span class="sf-stat-value accent">{{ avgOccupancy }}%</span>
          </div>
        </div>
        <div class="sf-pager">
          <el-pagination small background layout="prev, pager, next" :total="total" :page-size="pageSize" v-model:current-page="currentPage" @current-change="fetchAll" />
        </div>
      </div>

      <!-- Add Screening — Dark Mode Split Modal -->
      <Teleport to="body">
        <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
          <div class="modal-window">
            <!-- Left: Poster Preview + Guide -->
            <div class="modal-left">
              <div class="modal-left-bg">
                <img v-if="selectedMovie?.poster"
                  :src="selectedMovie.poster"
                  class="modal-poster-preview" />
              </div>
              <div class="modal-left-content">
                <span class="modal-badge">NEW ENTRY</span>
                <h3 class="modal-title-lg">{{ isEdit ? '编辑排片' : '创建新排片' }}</h3>
                <p class="modal-desc">请填写右侧信息以确立新的放映计划。确保影厅容量与预期观众量相匹配。</p>
              </div>
              <div class="modal-info-box">
                <el-icon><InfoFilled /></el-icon>
                <div>
                  <p class="modal-info-title">系统验证提示</p>
                  <p class="modal-info-text">系统将自动检测所选影厅的时间冲突。重复设置将按所选模式自动生成场次。</p>
                </div>
              </div>
            </div>
            <!-- Right: Form -->
            <div class="modal-right">
              <div class="modal-right-header">
                <h2 class="modal-form-title">排片详情</h2>
                <button class="modal-close-btn" @click="dialogVisible = false">
                  <el-icon :size="20"><Close /></el-icon>
                </button>
              </div>
              <el-form ref="formRef" :model="form" :rules="rules" class="modal-form">
                <!-- Film + Hall -->
                <div class="modal-form-row">
                  <div class="modal-form-col">
                    <label class="modal-label">选择影片</label>
                    <el-select v-model="form.movieId" placeholder="请选择影片" filterable class="modal-select" @change="onMovieChange">
                      <el-option v-for="m in movies" :key="m.id" :label="m.name" :value="m.id" />
                    </el-select>
                  </div>
                  <div class="modal-form-col">
                    <label class="modal-label">选择影厅</label>
                    <el-select v-model="form.hallId" placeholder="选择影厅" class="modal-select">
                      <el-option v-for="h in halls" :key="h.id" :label="h.name" :value="h.id" />
                    </el-select>
                  </div>
                </div>
                <!-- Price + Date + Time -->
                <div class="modal-form-row three-col">
                  <div class="modal-form-col">
                    <label class="modal-label">标准票价 (CNY)</label>
                    <div class="modal-price-wrap">
                      <span class="modal-price-prefix">¥</span>
                      <input class="modal-price-input" type="number" v-model.number="form.price" min="0" step="0.5" />
                    </div>
                  </div>
                  <div class="modal-form-col">
                    <label class="modal-label">放映日期</label>
                    <el-date-picker v-model="startDateOnly" type="date" placeholder="选择日期" class="modal-date-picker" value-format="YYYY-MM-DD" @change="onDateChange" />
                  </div>
                  <div class="modal-form-col">
                    <label class="modal-label">开始时间</label>
                    <el-time-picker v-model="startTimeOnly" placeholder="选择时间" class="modal-date-picker" value-format="HH:mm" @change="onTimeChange" />
                  </div>
                </div>
                <!-- Repeat Mode -->
                <div class="modal-section">
                  <label class="modal-label">重复模式</label>
                  <div class="repeat-cards">
                    <label class="repeat-card" :class="{ active: recurrence === 'none' }" @click="recurrence = 'none'">
                      <el-icon :size="22"><CircleClose /></el-icon>
                      <span>不重复</span>
                    </label>
                    <label class="repeat-card" :class="{ active: recurrence === 'daily' }" @click="recurrence = 'daily'">
                      <el-icon :size="22"><Calendar /></el-icon>
                      <span>每日</span>
                    </label>
                    <label class="repeat-card" :class="{ active: recurrence === 'weekly' }" @click="recurrence = 'weekly'">
                      <el-icon :size="22"><Refresh /></el-icon>
                      <span>每周</span>
                    </label>
                  </div>
                </div>
                <!-- Validation Hint -->
                <div class="modal-hint" :class="{ warning: timeConflict }">
                  <el-icon :size="18"><component :is="timeConflict ? 'WarningFilled' : 'CircleCheckFilled'" /></el-icon>
                  <p v-if="!timeConflict">
                    检测中：该时段暂无排片冲突。电影时长 {{ selectedMovie?.duration || '--' }} 分钟
                    <span v-if="form.endTime">，预计结束于 {{ fmtTime(form.endTime) }}</span>。
                  </p>
                  <p v-else>时间冲突：该时段与已有场次重叠，请调整时间或影厅。</p>
                </div>
              </el-form>
              <!-- Footer -->
              <div class="modal-footer">
                <button class="modal-btn-cancel" @click="dialogVisible = false">取消创建</button>
                <button class="modal-btn-submit" :disabled="submitting" @click="handleSubmit">
                  <span>{{ isEdit ? '保存修改' : '确立排片计划' }}</span>
                  <el-icon :size="18"><Lightning /></el-icon>
                </button>
              </div>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- Batch Dialog -->
      <el-dialog v-model="batchVisible" title="批量排片" width="640px" :close-on-click-modal="false" class="screening-dialog">
        <template #header><span class="dialog-title">批量排片</span></template>
        <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-position="top" class="screening-form">
          <el-form-item label="选择影片" prop="movieIds">
            <el-select v-model="batchForm.movieIds" placeholder="可多选影片" filterable multiple style="width:100%">
              <el-option v-for="m in movies" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="选择影厅" prop="hallIds">
            <el-select v-model="batchForm.hallIds" placeholder="可多选影厅" filterable multiple style="width:100%">
              <el-option v-for="h in halls" :key="h.id" :label="h.name" :value="h.id" />
            </el-select>
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="开始日期" prop="startDate">
                <el-date-picker v-model="batchForm.startDate" type="date" placeholder="选择日期" class="full-width" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束日期" prop="endDate">
                <el-date-picker v-model="batchForm.endDate" type="date" placeholder="选择日期" class="full-width" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="放映时段">
            <div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:8px">
              <el-tag v-for="(slot, idx) in batchForm.timeSlots" :key="idx" closable @close="removeTimeSlot(idx)" type="warning">{{ slot }}</el-tag>
              <span v-if="batchForm.timeSlots.length === 0" style="color:var(--text-tertiary);font-size:13px">请添加至少一个时段</span>
            </div>
            <div style="display:flex;gap:8px">
              <el-time-picker v-model="newTimeSlot" placeholder="选择时间" value-format="HH:mm" style="width:180px" />
              <el-button @click="addTimeSlot" :disabled="!newTimeSlot">添加时段</el-button>
            </div>
          </el-form-item>
          <el-alert type="info" show-icon :closable="false" style="margin-top:4px">
            预计生成 <b>{{ previewCount }}</b> 条排片（{{ batchForm.movieIds.length }}片 × {{ batchForm.hallIds.length }}厅 × {{ dayCount }}天 × {{ batchForm.timeSlots.length || 1 }}时段）
          </el-alert>
        </el-form>
        <template #footer>
          <el-button @click="batchVisible = false">取消</el-button>
          <el-button type="primary" :loading="batchSubmitting" @click="handleBatchSubmit">确认批量添加</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { getScheduleList, addSchedule, updateSchedule, deleteSchedule, getHallList, batchAddSchedule, batchDeleteSchedules } from '@/api/order'
import { getMovieList } from '@/api/movie'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Clock, VideoCameraFilled, WarningFilled, InfoFilled, CircleCheckFilled, CircleClose, Calendar, Refresh, Lightning, Close } from '@element-plus/icons-vue'

// Data
const schedules = ref([]); const movies = ref([]); const halls = ref([])
const loading = ref(false); const dialogVisible = ref(false); const isEdit = ref(false)
const editingId = ref(null); const submitting = ref(false); const formRef = ref(null)
const currentPage = ref(1); const pageSize = ref(20); const total = ref(0)
const selectedIds = ref([]); const selectedDate = ref('')

// New form state for Stitch design
const startDateOnly = ref(''); const startTimeOnly = ref(''); const recurrence = ref('none')
const timeConflict = ref(false)

// Batch
const batchVisible = ref(false); const batchSubmitting = ref(false); const batchFormRef = ref(null)
const newTimeSlot = ref('')
const batchForm = reactive({ movieIds: [], hallIds: [], startDate: '', endDate: '', timeSlots: [] })

const form = reactive({ movieId: null, hallId: null, startTime: '', endTime: '', price: 0 })

const selectedMovie = computed(() => movies.value.find(m => m.id === form.movieId) || null)
const rules = {
  movieId: [{ required: true, message: '请选择影片', trigger: 'change' }],
  hallId: [{ required: true, message: '请选择影厅', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  price: [{ required: true, message: '请输入票价', trigger: 'blur' }]
}
const batchRules = {
  movieIds: [{ required: true, message: '请选择影片', trigger: 'change' }],
  hallIds: [{ required: true, message: '请选择影厅', trigger: 'change' }]
}

// Computed
const hallSchedules = computed(() => {
  const map = {}
  halls.value.forEach(h => { map[h.id] = { id: h.id, name: h.name, type: h.type, schedules: [] } })
  schedules.value.forEach(s => {
    const hid = s.hallId
    if (!map[hid]) map[hid] = { id: hid, name: s.hallName || '影厅 ' + hid, type: '', schedules: [] }
    map[hid].schedules.push(s)
  })
  return Object.values(map)
})

const totalCount = computed(() => schedules.value.length)
const avgOccupancy = computed(() => {
  if (schedules.value.length === 0) return 0
  const total = schedules.value.reduce((sum, s) => {
    const sold = s.soldCount || 0; const total = s.totalSeats || 240
    return sum + (total > 0 ? (sold / total) * 100 : 0)
  }, 0)
  return (total / schedules.value.length).toFixed(1)
})

const dayCount = computed(() => {
  if (batchForm.startDate && batchForm.endDate) {
    return Math.max(1, Math.ceil((new Date(batchForm.endDate) - new Date(batchForm.startDate)) / 86400000) + 1)
  }
  return 1
})
const previewCount = computed(() => {
  const m = batchForm.movieIds.length || 1; const h = batchForm.hallIds.length || 1
  return m * h * dayCount.value * (batchForm.timeSlots.length || 1)
})

const dateOptions = computed(() => {
  const options = []
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const today = new Date()
  for (let i = -3; i <= 10; i++) {
    const d = new Date(today); d.setDate(d.getDate() + i)
    const y = d.getFullYear(); const m = String(d.getMonth() + 1).padStart(2, '0'); const day = String(d.getDate()).padStart(2, '0')
    options.push({ value: `${y}-${m}-${day}`, weekday: weekdays[d.getDay()], day: d.getDate() })
  }
  return options
})

// Helpers
function fmtTime(t) { if (!t) return '--'; const d = new Date(t); return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}` }
function statusLabel(s) { const m = { 0: '已取消', 1: '热映中', 2: '已结束' }; return m[s] ?? '待定' }
function statusClass(s) { const m = { 0: 'cancelled', 1: 'showing', 2: 'ended' }; return m[s] ?? '' }
function seatPercent(s) { const sold = s.soldCount || 0; const total = (s.hallRowCount || 0) * (s.hallColCount || 0) || 240; return total > 0 ? Math.min(100, (sold / total) * 100) : 0 }

// Actions
function onMovieChange(mid) { const m = movies.value.find(x => x.id === mid); if (m) { form.price = m.price || 0; calcEndTime() } }
function calcEndTime() { if (!form.startTime || !form.movieId) return; const m = movies.value.find(x => x.id === form.movieId); if (!m?.duration) return; const d = new Date(form.startTime); d.setMinutes(d.getMinutes() + m.duration); form.endTime = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:00` }
function onDateChange() { syncDateTime(); checkConflict() }
function onTimeChange() { syncDateTime(); checkConflict() }
function syncDateTime() {
  if (startDateOnly.value && startTimeOnly.value) {
    form.startTime = `${startDateOnly.value} ${startTimeOnly.value}:00`
    calcEndTime()
  }
}
function checkConflict() {
  if (!form.startTime || !form.hallId) { timeConflict.value = false; return }
  const newStart = new Date(form.startTime).getTime()
  const newEnd = form.endTime ? new Date(form.endTime).getTime() : newStart
  timeConflict.value = schedules.value.some(s =>
    s.hallId === form.hallId && s.id !== editingId.value &&
    new Date(s.startTime).getTime() < newEnd && new Date(s.endTime).getTime() > newStart
  )
}
function resetForm() {
  Object.assign(form, { movieId: null, hallId: null, startTime: '', endTime: '', price: 0 })
  startDateOnly.value = ''; startTimeOnly.value = ''; recurrence.value = 'none'; timeConflict.value = false
}

function openAdd() { isEdit.value = false; editingId.value = null; resetForm(); dialogVisible.value = true }
function openAddForHall(hallId) { isEdit.value = false; editingId.value = null; resetForm(); form.hallId = hallId; dialogVisible.value = true }
function openEdit(row) {
  isEdit.value = true; editingId.value = row.id
  form.movieId = row.movieId; form.hallId = row.hallId; form.startTime = row.startTime; form.endTime = row.endTime; form.price = row.price
  // Parse startTime into date and time
  if (row.startTime) {
    const d = new Date(row.startTime)
    startDateOnly.value = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    startTimeOnly.value = `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value || !await formRef.value.validate().catch(() => false)) return
  submitting.value = true
  try {
    if (isEdit.value) { await updateSchedule({ id: editingId.value, ...form }); ElMessage.success('排片已更新') }
    else { await addSchedule({ ...form }); ElMessage.success('排片已添加') }
    dialogVisible.value = false; fetchAll()
  } catch (e) { /* handled */ }
  submitting.value = false
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm('确定删除此排片？', '确认', { type: 'warning' }) } catch { return }
  try { await deleteSchedule(row.id); ElMessage.success('已删除'); fetchAll() } catch (e) { /* handled */ }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  try { await ElMessageBox.confirm(`确定删除 ${selectedIds.value.length} 条记录？`, '批量删除', { type: 'warning' }) } catch { return }
  try { await batchDeleteSchedules(selectedIds.value); ElMessage.success(`已删除 ${selectedIds.value.length} 条`); selectedIds.value = []; fetchAll() } catch (e) { /* handled */ }
}

function openBatch() { Object.assign(batchForm, { movieIds: [], hallIds: [], startDate: '', endDate: '', timeSlots: [] }); batchVisible.value = true }
function addTimeSlot() { if (newTimeSlot.value && !batchForm.timeSlots.includes(newTimeSlot.value)) { batchForm.timeSlots.push(newTimeSlot.value); newTimeSlot.value = '' } }
function removeTimeSlot(i) { batchForm.timeSlots.splice(i, 1) }

async function handleBatchSubmit() {
  if (!batchFormRef.value || !await batchFormRef.value.validate().catch(() => false)) return
  if (batchForm.timeSlots.length === 0) { ElMessage.warning('请添加至少一个放映时段'); return }
  batchSubmitting.value = true
  try {
    // Build hallMovies: Cartesian product of movieIds × hallIds
    const hallMovies = []
    for (const mid of batchForm.movieIds) {
      for (const hid of batchForm.hallIds) {
        hallMovies.push({ movieId: mid, hallId: hid })
      }
    }
    await batchAddSchedule({
      hallMovies,
      startDate: batchForm.startDate,
      endDate: batchForm.endDate,
      timeSlots: batchForm.timeSlots
    })
    ElMessage.success('批量排片成功'); batchVisible.value = false; fetchAll()
  } catch (e) { /* handled */ }
  batchSubmitting.value = false
}

async function fetchAll() {
  loading.value = true
  try {
    const [sRes, mRes, hRes] = await Promise.all([
      getScheduleList({ page: currentPage.value, size: pageSize.value }),
      getMovieList({ page: 1, size: 200 }),
      getHallList()
    ])
    schedules.value = sRes.data?.records || sRes.data || []; total.value = sRes.data?.total || 0
    movies.value = mRes.data?.records || mRes.data || []
    halls.value = hRes.data?.records || hRes.data || []
  } catch (e) { schedules.value = []; movies.value = []; halls.value = [] }
  loading.value = false
}

onMounted(() => { selectedDate.value = dateOptions.value[3]?.value || ''; fetchAll() })
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.admin-content.schedule-page { max-width: 1280px; margin: 0 auto; padding: 32px 24px; }

/* Header */
.s-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 32px; flex-wrap: wrap; gap: 16px; }
.s-title { font-size: 36px; font-weight: 700; color: var(--color-primary); letter-spacing: -0.5px; }
.s-subtitle { color: var(--text-secondary); font-size: 15px; margin-top: 4px; }
.s-header-actions { display: flex; gap: 10px; align-items: center; }

/* Date Strip */
.date-strip-wrap { margin-bottom: 36px; }
.date-strip-label { font-size: 12px; font-weight: 600; color: var(--color-primary); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px; }
.date-strip { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 8px; -webkit-overflow-scrolling: touch; }
.date-strip::-webkit-scrollbar { height: 0; }
.date-card { flex: 0 0 80px; padding: 12px 4px; border-radius: 12px; border: 1px solid var(--border-color); display: flex; flex-direction: column; align-items: center; cursor: pointer; transition: all 0.2s; background: var(--bg-card); }
.date-card:hover { border-color: var(--color-primary); }
.date-card.active { border-color: var(--color-primary); background: rgba(232,168,80,0.08); color: var(--color-primary); }
.date-weekday { font-size: 11px; color: var(--text-tertiary); text-transform: uppercase; }
.date-card.active .date-weekday { color: var(--color-primary); }
.date-day { font-size: 22px; font-weight: 700; }
.date-dot { width: 4px; height: 4px; background: var(--color-primary); border-radius: 50%; margin-top: 4px; }

/* Hall Sections */
.hall-section { margin-bottom: 48px; }
.hall-header { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; padding-left: 8px; border-left: 4px solid var(--color-primary); }
.hall-name { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.hall-type-tag { font-size: 11px; padding: 3px 10px; border-radius: 20px; background: rgba(232,168,80,0.1); color: var(--color-primary); }

/* Screening Grid */
.screening-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }

/* Screening Card */
.screening-card { position: relative; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 12px; overflow: hidden; transition: all 0.2s; cursor: pointer; }
.screening-card:hover { box-shadow: var(--shadow-card-hover); transform: translateY(-2px); }
.screening-card.conflict { border-color: rgba(232,64,64,0.5); background: rgba(232,64,64,0.03); }
.conflict-badge { position: absolute; top: 0; right: 0; background: var(--color-danger); color: #fff; font-size: 10px; font-weight: 700; padding: 2px 10px; border-radius: 0 0 0 8px; }
.sc-body { padding: 16px; display: flex; flex-direction: column; min-height: 180px; }
.sc-top { display: flex; gap: 12px; flex: 1; }
.sc-poster { width: 56px; height: 80px; border-radius: 6px; overflow: hidden; background: var(--bg-hover); display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: var(--text-tertiary); }
.sc-poster img { width: 100%; height: 100%; object-fit: cover; }
.sc-info { flex: 1; min-width: 0; }
.sc-status { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; display: inline-block; }
.sc-status.showing { color: var(--color-success); }
.sc-status.ended { color: var(--text-tertiary); }
.sc-status.cancelled { color: var(--color-danger); }
.sc-movie { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sc-time { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--text-secondary); }
.sc-bottom { margin-top: 12px; }
.sc-seats { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; }
.sc-price { font-weight: 700; font-size: 16px; color: var(--color-primary); }
.sc-progress { height: 4px; background: var(--bg-hover); border-radius: 2px; overflow: hidden; margin-bottom: 10px; }
.sc-progress-fill { height: 100%; background: var(--color-primary); border-radius: 2px; transition: width 0.3s; }
.sc-actions { display: flex; gap: 4px; justify-content: flex-end; }

/* Add Card */
.add-card { border: 2px dashed var(--border-color); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 32px; color: var(--text-tertiary); min-height: 180px; }
.add-card:hover { border-color: var(--color-primary); color: var(--color-primary); }

/* Footer */
.s-footer { margin-top: 48px; padding-top: 24px; border-top: 1px solid var(--border-light); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; }
.sf-stats { display: flex; gap: 32px; }
.sf-stat { display: flex; flex-direction: column; }
.sf-stat-label { font-size: 10px; font-weight: 700; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.5px; }
.sf-stat-value { font-size: 28px; font-weight: 700; color: var(--text-primary); }
.sf-stat-value.accent { color: var(--color-primary); }

/* === Dark Mode Split Modal === */
.modal-overlay {
  position: fixed; inset: 0; z-index: 2000; display: flex; align-items: center; justify-content: center;
  padding: 24px; background: rgba(0,0,0,0.55); backdrop-filter: blur(6px);
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.modal-window {
  display: flex; width: 100%; max-width: 1000px; max-height: 90vh; border-radius: 20px; overflow: hidden;
  background: var(--bg-card); border: 1px solid var(--border-color);
  box-shadow: var(--shadow-heavy);
  animation: modalIn 0.3s cubic-bezier(0.34,1.56,0.64,1);
}
@keyframes modalIn { from { opacity: 0; transform: scale(0.95) translateY(12px); } to { opacity: 1; transform: scale(1) translateY(0); } }

/* Left Panel */
.modal-left {
  width: 320px; flex-shrink: 0; background: var(--bg-primary); position: relative; overflow: hidden;
  display: flex; flex-direction: column; padding: 32px 28px; border-right: 1px solid var(--border-light);
}
.modal-left-bg { position: absolute; inset: 0; opacity: 0.35; }
.modal-left-bg img { width: 100%; height: 100%; object-fit: cover; filter: grayscale(100%); }
.modal-left-content { position: relative; z-index: 1; flex: 1; }
.modal-badge {
  display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 10px; font-weight: 700;
  letter-spacing: 2px; color: var(--color-primary); background: rgba(232,168,80,0.12);
  border: 1px solid rgba(232,168,80,0.2); margin-bottom: 20px;
}
.modal-title-lg { font-size: 28px; font-weight: 700; color: var(--text-primary); line-height: 1.2; margin-bottom: 12px; }
.modal-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.6; }
.modal-info-box {
  position: relative; z-index: 1; margin-top: 24px; padding: 14px 16px; border-radius: 14px;
  background: var(--bg-hover); border: 1px solid var(--border-light);
  display: flex; gap: 10px; align-items: flex-start;
}
.modal-info-box .el-icon { color: var(--color-primary); flex-shrink: 0; margin-top: 2px; }
.modal-info-title { font-size: 12px; font-weight: 700; color: var(--color-primary); margin-bottom: 4px; }
.modal-info-text { font-size: 11px; color: var(--text-tertiary); line-height: 1.5; }

/* Right Panel */
.modal-right { flex: 1; display: flex; flex-direction: column; min-width: 0; background: var(--bg-card); }
.modal-right-header { display: flex; align-items: center; justify-content: space-between; padding: 24px 28px 0; }
.modal-form-title { font-size: 22px; font-weight: 700; color: var(--text-primary); font-style: italic; }
.modal-close-btn {
  width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; color: var(--text-secondary); cursor: pointer; transition: all 0.15s;
}
.modal-close-btn:hover { background: var(--bg-hover); color: var(--text-primary); }

.modal-form { flex: 1; overflow-y: auto; padding: 24px 28px; display: flex; flex-direction: column; gap: 22px; }
.modal-form::-webkit-scrollbar { width: 4px; }
.modal-form::-webkit-scrollbar-thumb { background: var(--scrollbar-thumb); border-radius: 10px; }

.modal-label { display: block; font-size: 11px; font-weight: 600; color: var(--text-secondary); letter-spacing: 0.5px; margin-bottom: 6px; }
.modal-form-row { display: flex; gap: 14px; }
.modal-form-row.three-col .modal-form-col { flex: 1; }
.modal-form-col { flex: 1; }

.modal-select :deep(.el-input__wrapper) {
  background: var(--bg-input) !important; border: 1px solid var(--border-light) !important;
  border-radius: 12px !important; box-shadow: none !important; transition: all 0.15s;
}
.modal-select :deep(.el-input__wrapper:hover) { border-color: var(--color-primary) !important; }
.modal-select :deep(.el-input__inner) { color: var(--text-primary) !important; }

.modal-price-wrap { display: flex; align-items: center; background: var(--bg-input); border: 1px solid var(--border-light); border-radius: 12px; overflow: hidden; transition: all 0.15s; }
.modal-price-wrap:focus-within { border-color: rgba(232,168,80,0.4); box-shadow: 0 0 0 3px rgba(232,168,80,0.08); }
.modal-price-prefix { padding: 0 14px; font-weight: 700; color: var(--color-primary); font-size: 15px; border-right: 1px solid var(--border-light); }
.modal-price-input { flex: 1; border: none; padding: 10px 14px; background: transparent; color: var(--text-primary); font-size: 15px; outline: none; width: 100%; font-family: 'Consolas', monospace; }

.modal-date-picker { width: 100%; }
.modal-date-picker :deep(.el-input__wrapper) {
  background: var(--bg-input) !important; border: 1px solid var(--border-light) !important;
  border-radius: 12px !important; box-shadow: none !important;
}

/* Repeat Cards */
.repeat-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.repeat-card {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
  padding: 16px; border-radius: 14px; cursor: pointer; transition: all 0.2s;
  background: var(--bg-input); border: 2px solid transparent; color: var(--text-secondary); font-size: 12px; font-weight: 600;
}
.repeat-card:hover { border-color: var(--color-primary); }
.repeat-card.active { border-color: var(--color-primary); background: rgba(232,168,80,0.06); color: var(--color-primary); }
.repeat-card .el-icon { transition: color 0.2s; }

/* Validation Hint */
.modal-hint {
  display: flex; align-items: flex-start; gap: 10px; padding: 12px 16px; border-radius: 12px;
  background: rgba(26,107,76,0.08); border: 1px solid rgba(26,107,76,0.2);
}
.modal-hint .el-icon { color: var(--color-emerald); flex-shrink: 0; margin-top: 1px; font-size: 18px; }
.modal-hint p { font-size: 12px; color: var(--text-secondary); line-height: 1.5; font-style: italic; }
.modal-hint.warning { background: rgba(232,64,64,0.06); border-color: rgba(232,64,64,0.2); }
.modal-hint.warning .el-icon { color: var(--color-danger); }
.modal-hint.warning p { color: var(--color-danger); }

/* Footer */
.modal-footer {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 20px 28px; border-top: 1px solid var(--border-light); background: var(--bg-secondary);
}
.modal-btn-cancel {
  padding: 10px 28px; border-radius: 12px; border: 1px solid var(--border-color);
  background: transparent; color: var(--text-secondary); font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.15s;
}
.modal-btn-cancel:hover { color: var(--text-primary); background: var(--bg-hover); }
.modal-btn-submit {
  display: flex; align-items: center; gap: 8px; padding: 10px 32px; border-radius: 12px; border: none;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark)); color: #fff; font-size: 13px; font-weight: 700;
  cursor: pointer; transition: all 0.15s; box-shadow: var(--shadow-gold-glow);
}
.modal-btn-submit:hover { transform: scale(1.02); }
.modal-btn-submit:active { transform: scale(0.98); }
.modal-btn-submit:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

/* Theme overrides */
[data-theme='dark'] .modal-btn-submit { color: #1A1814; }
[data-theme='light'] .date-card.active { background: rgba(132,84,0,0.06); }
[data-theme='light'] .hall-type-tag { background: rgba(132,84,0,0.06); }
[data-theme='light'] .modal-badge { background: rgba(132,84,0,0.06); border-color: rgba(132,84,0,0.15); }
[data-theme='light'] .repeat-card.active { background: rgba(132,84,0,0.04); }
[data-theme='light'] .modal-overlay { background: rgba(0,0,0,0.3); }
[data-theme='light'] .modal-hint { background: rgba(26,107,76,0.04); }

@media (max-width: 768px) {
  .modal-window { flex-direction: column; max-height: 95vh; }
  .modal-left { display: none; }
  .modal-form-row { flex-direction: column; }
}

@media (max-width: 768px) {
  .s-title { font-size: 26px; }
  .screening-grid { grid-template-columns: 1fr; }
  .sf-stats { gap: 20px; }
}
</style>
