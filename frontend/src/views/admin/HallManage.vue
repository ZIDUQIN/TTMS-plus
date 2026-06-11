<template>
  <div class="admin-layout">
    <div class="admin-content">
      <div class="page-header">
        <h2>影厅管理</h2>
        <el-button type="primary" :icon="Plus" @click="openAdd">添加影厅</el-button>
      </div>

      <div class="card">
        <el-table :data="halls" v-loading="loading" stripe>
          <el-table-column prop="name" label="影厅名称" min-width="120" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag size="small">{{ hallTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="排数 x 列数" width="120">
            <template #default="{ row }">{{ row.rows }} x {{ row.cols }}</template>
          </el-table-column>
          <el-table-column label="可用座位" width="90">
            <template #default="{ row }">
              {{ availableSeatCount(row) }} / {{ row.rows * row.cols }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'danger' : 'success'" size="small">
                {{ row.status === 0 ? '维护中' : '正常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text :type="row.status === 0 ? 'success' : 'warning'" @click="toggleStatus(row)">
                {{ row.status === 0 ? '恢复' : '维护' }}
              </el-button>
              <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Add/Edit Dialog -->
      <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑影厅' : '添加影厅'"
        width="720px"
        :close-on-click-modal="false"
        @opened="initSeatEditor"
      >
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="影厅名称" prop="name">
            <el-input v-model="form.name" placeholder="如：1号标准厅" />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="form.type" placeholder="请选择影厅类型" style="width: 100%">
              <el-option v-for="t in hallTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="排数" prop="rowCount">
                <el-input-number v-model="form.rowCount" :min="3" :max="50" style="width: 100%" @change="rebuildSeatGrid" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="列数" prop="colCount">
                <el-input-number v-model="form.colCount" :min="3" :max="50" style="width: 100%" @change="rebuildSeatGrid" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="座位布局">
            <div class="seat-layout-info">
              <span>总座位: {{ form.rowCount * form.colCount }} | 可用: {{ usableCount }} | 已禁用: {{ disabledSeats.size }}</span>
              <span style="color: var(--text-muted); font-size: 12px;">点击座位切换 启用/禁用</span>
            </div>
            <div class="seat-layout-editor" v-if="seatGrid.length > 0">
              <!-- Column numbers -->
              <div class="seat-layout-header">
                <span class="row-label-spacer"></span>
                <span v-for="c in form.colCount" :key="c" class="col-label">{{ c }}</span>
              </div>
              <!-- Seat rows -->
              <div v-for="(row, rIdx) in seatGrid" :key="rIdx" class="seat-layout-row">
                <span class="row-label">{{ rowLabel(rIdx + 1) }}</span>
                <div
                  v-for="(cell, cIdx) in row"
                  :key="cIdx"
                  class="layout-seat"
                  :class="{ disabled: !cell.active, active: cell.active }"
                  :title="`${rowLabel(rIdx + 1)}-${String(cIdx + 1).padStart(2, '0')} ${cell.active ? '可用' : '禁用'}`"
                  @click="toggleLayoutSeat(rIdx, cIdx)"
                ></div>
              </div>
            </div>
            <div v-else class="seat-layout-placeholder">
              请先设置排数和列数
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '添加影厅' }}
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { getHallList, addHall, updateHall, deleteHall } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const halls = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const hallTypes = [
  { label: '普通厅', value: 'STANDARD' },
  { label: 'IMAX厅', value: 'IMAX' },
  { label: 'VIP厅', value: 'VIP' },
  { label: '4D厅', value: '4DX' }
]

function hallTypeLabel(t) { return hallTypes.find(h => h.value === t)?.label || t || '普通厅' }
function rowLabel(row) { return String.fromCharCode(64 + row) }

const form = reactive({ name: '', type: 'STANDARD', rowCount: 8, colCount: 12 })

// Seat layout editor state
const seatGrid = ref([])        // 2D array: [{active: true}, ...]
const disabledSeats = ref(new Set())  // Set of "row-col" keys

const usableCount = computed(() => {
  const total = form.rowCount * form.colCount
  return total - disabledSeats.value.size
})

function availableSeatCount(row) {
  if (!row.seatLayout) return row.rows * row.cols
  try {
    const disabled = JSON.parse(row.seatLayout)
    return (row.rows * row.cols) - (Array.isArray(disabled) ? disabled.length : 0)
  } catch { return row.rows * row.cols }
}

const rules = {
  name: [{ required: true, message: '请输入影厅名称', trigger: 'blur' }],
  rowCount: [{ required: true, message: '请输入排数', trigger: 'blur' }],
  colCount: [{ required: true, message: '请输入列数', trigger: 'blur' }]
}

function buildSeatGrid(rowCount, colCount, disabledSet) {
  const grid = []
  for (let r = 1; r <= rowCount; r++) {
    const row = []
    for (let c = 1; c <= colCount; c++) {
      row.push({ active: !disabledSet.has(`${r}-${c}`) })
    }
    grid.push(row)
  }
  seatGrid.value = grid
}

function rebuildSeatGrid() {
  buildSeatGrid(form.rowCount, form.colCount, disabledSeats.value)
}

function initSeatEditor() {
  if (!isEdit.value) {
    disabledSeats.value = new Set()
    buildSeatGrid(form.rowCount, form.colCount, new Set())
  }
}

function toggleLayoutSeat(rIdx, cIdx) {
  const row = rIdx + 1
  const col = cIdx + 1
  const key = `${row}-${col}`
  const cell = seatGrid.value[rIdx][cIdx]
  cell.active = !cell.active
  if (cell.active) {
    disabledSeats.value.delete(key)
  } else {
    disabledSeats.value.add(key)
  }
}

function getLayoutJSON() {
  if (disabledSeats.value.size === 0) return ''
  // 过滤掉超出当前行列范围的无效条目（缩小行列后可能残留）
  const validSeats = [...disabledSeats.value].filter(key => {
    const [r, c] = key.split('-').map(Number)
    return r >= 1 && r <= form.rowCount && c >= 1 && c <= form.colCount
  })
  return validSeats.length > 0 ? JSON.stringify(validSeats) : ''
}

function parseLayoutToSet(layoutJSON) {
  const set = new Set()
  if (!layoutJSON) return set
  try {
    const arr = JSON.parse(layoutJSON)
    if (Array.isArray(arr)) {
      arr.forEach(k => set.add(k))
    }
  } catch { /* ignore */ }
  return set
}

function resetForm() {
  Object.assign(form, { name: '', type: 'STANDARD', rowCount: 8, colCount: 12 })
  disabledSeats.value = new Set()
  buildSeatGrid(8, 12, new Set())
}

function openAdd() {
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.type = row.type || 'STANDARD'
  form.rowCount = row.rows
  form.colCount = row.cols
  // Parse existing layout
  const layoutJSON = row.seatLayout || ''
  disabledSeats.value = parseLayoutToSet(layoutJSON)
  buildSeatGrid(form.rowCount, form.colCount, disabledSeats.value)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value || submitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      name: form.name,
      type: form.type,
      rows: form.rowCount,
      cols: form.colCount,
      capacity: usableCount.value,
      seatLayout: getLayoutJSON()
    }
    if (isEdit.value) {
      await updateHall({ id: editingId.value, ...payload })
      ElMessage.success('影厅更新成功')
    } else {
      await addHall(payload)
      ElMessage.success('影厅添加成功')
    }
    dialogVisible.value = false
    fetchHalls()
  } catch (err) { /* handled */ } finally { submitting.value = false }
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm(`确定要删除影厅"${row.name}"吗？`, '删除确认', { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }) } catch { return }
  try { await deleteHall(row.id); ElMessage.success('已删除'); fetchHalls() } catch (err) { /* handled */ }
}

async function toggleStatus(row) {
  const newStatus = row.status === 0 ? 1 : 0
  try {
    await updateHall({ id: row.id, status: newStatus })
    row.status = newStatus
    ElMessage.success(newStatus === 0 ? '影厅已设为维护' : '影厅已恢复')
  } catch (err) { /* handled */ }
}

async function fetchHalls() {
  loading.value = true
  try { const res = await getHallList(); halls.value = res.data?.records || res.data || [] } catch (err) { halls.value = [] } finally { loading.value = false }
}

onMounted(fetchHalls)
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.card { background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-light); }

/* Seat Layout Editor */
.seat-layout-info {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 8px; font-size: 13px; color: var(--text-secondary);
}
.seat-layout-editor {
  background: var(--bg-secondary); border-radius: var(--radius-md);
  padding: 12px; overflow: auto; max-height: 380px;
}
.seat-layout-header {
  display: flex; align-items: center; margin-bottom: 4px; padding-left: 28px;
}
.col-label {
  width: 22px; text-align: center; font-size: 10px; color: var(--text-muted); flex-shrink: 0;
}
.seat-layout-row {
  display: flex; align-items: center; margin-bottom: 2px;
}
.row-label-spacer { width: 28px; flex-shrink: 0; }
.row-label {
  width: 28px; text-align: center; font-size: 11px; font-weight: 600;
  color: var(--text-muted); flex-shrink: 0;
}
.layout-seat {
  width: 20px; height: 20px; margin: 1px; border-radius: 3px;
  cursor: pointer; transition: all 0.15s; flex-shrink: 0;
  border: 1px solid transparent;
}
.layout-seat.active {
  background: #67c23a; border-color: #5daf34;
}
.layout-seat.active:hover {
  background: #85ce61; transform: scale(1.2);
}
.layout-seat.disabled {
  background: #909399; border-color: #808389;
}
.layout-seat.disabled:hover {
  background: #b0b3b8;
}
.seat-layout-placeholder {
  padding: 40px; text-align: center; color: var(--text-muted);
  background: var(--bg-secondary); border-radius: var(--radius-md);
}
</style>
