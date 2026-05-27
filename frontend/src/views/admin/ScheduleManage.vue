<template>
  <div class="admin-layout">
    <NavBar />
    <div class="admin-content">
      <div class="page-header">
        <h2>排片管理</h2>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增排片</el-button>
      </div>

      <div class="card">
        <el-table :data="schedules" v-loading="loading" stripe>
          <el-table-column prop="movieName" label="影片" min-width="140" show-overflow-tooltip />
          <el-table-column prop="hallName" label="影厅" width="100">
            <template #default="{ row }">{{ row.hallName || row.hall?.name || '--' }}</template>
          </el-table-column>
          <el-table-column label="开始时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="结束时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
          </el-table-column>
          <el-table-column label="票价" width="80">
            <template #default="{ row }">${{ row.price || '--' }}</template>
          </el-table-column>
          <el-table-column label="已售/总座位" width="120">
            <template #default="{ row }">
              {{ row.soldCount || 0 }} / {{ row.totalSeats || (row.hall?.rowCount || row.rowCount || 8) * (row.hall?.colCount || row.colCount || 12) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Add/Edit Dialog -->
      <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑排片' : '新增排片'"
        width="560px"
        :close-on-click-modal="false"
      >
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="影片" prop="movieId">
            <el-select v-model="form.movieId" placeholder="请选择影片" filterable style="width: 100%" @change="onMovieChange">
              <el-option v-for="m in movies" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="影厅" prop="hallId">
            <el-select v-model="form.hallId" placeholder="请选择影厅" filterable style="width: 100%">
              <el-option v-for="h in availableHalls" :key="h.id" :label="`${h.name} (${h.rows || h.rowCount}x${h.cols || h.colCount})`" :value="h.id" />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="开始时间" prop="startTime">
                <el-date-picker
                  v-model="form.startTime"
                  type="datetime"
                  placeholder="选择开始时间"
                  style="width: 100%"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  @change="calcEndTime"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束时间" prop="endTime">
                <el-input :model-value="form.endTime" disabled placeholder="自动计算" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="票价" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '确认添加' }}
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { getSchedulesByMovie } from '@/api/order'
import { getMovieList } from '@/api/movie'
import { addSchedule, updateSchedule, deleteSchedule, getHallList } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

const schedules = ref([])
const movies = ref([])
const halls = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  movieId: null, hallId: null, startTime: '', endTime: '', price: 0
})

const rules = {
  movieId: [{ required: true, message: '请选择影片', trigger: 'change' }],
  hallId: [{ required: true, message: '请选择影厅', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  price: [{ required: true, message: '请输入票价', trigger: 'blur' }]
}

const availableHalls = computed(() => halls.value)

function statusLabel(s) {
  const map = { 0: '已取消', 1: '正常放映', 2: '已结束' }
  return map[s] !== undefined ? map[s] : (s || '--')
}

function statusType(s) {
  const map = { 0: 'danger', 1: 'success', 2: 'info' }
  return map[s] || 'info'
}

function formatDateTime(s) {
  if (!s) return '--'
  const d = new Date(s)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function onMovieChange(movieId) {
  const movie = movies.value.find(m => m.id === movieId)
  if (movie) {
    form.price = movie.basePrice || movie.price || 0
    calcEndTime()
  }
}

function calcEndTime() {
  if (!form.startTime || !form.movieId) return
  const movie = movies.value.find(m => m.id === form.movieId)
  if (!movie || !movie.duration) return
  const start = new Date(form.startTime)
  start.setMinutes(start.getMinutes() + movie.duration)
  const y = start.getFullYear()
  const mo = String(start.getMonth() + 1).padStart(2, '0')
  const d = String(start.getDate()).padStart(2, '0')
  const h = String(start.getHours()).padStart(2, '0')
  const mi = String(start.getMinutes()).padStart(2, '0')
  form.endTime = `${y}-${mo}-${d} ${h}:${mi}:00`
}

function resetForm() {
  Object.assign(form, { movieId: null, hallId: null, startTime: '', endTime: '', price: 0 })
}

function openAdd() {
  isEdit.value = false; editingId.value = null; resetForm(); dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true; editingId.value = row.id
  form.movieId = row.movieId
  form.hallId = row.hallId
  form.startTime = row.startTime
  form.endTime = row.endTime
  form.price = row.price
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      await updateSchedule({ id: editingId.value, ...payload })
      ElMessage.success('排片更新成功')
    } else {
      await addSchedule(payload)
      ElMessage.success('排片添加成功')
    }
    dialogVisible.value = false
    fetchSchedules()
  } catch (err) { /* handled */ }
  finally { submitting.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除此排片吗？', '删除确认', {
      confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  try {
    await deleteSchedule(row.id)
    ElMessage.success('排片已删除')
    fetchSchedules()
  } catch (err) { /* handled */ }
}

async function fetchSchedules() {
  loading.value = true
  try {
    const { getScheduleList } = await import('@/api/order')
    const res = await getScheduleList()
    schedules.value = res.data?.records || res.data || []
  } catch (err) { schedules.value = [] }
  finally { loading.value = false }
}

async function fetchMovies() {
  try {
    const res = await getMovieList()
    movies.value = res.data?.records || res.data || []
  } catch (err) { movies.value = [] }
}

async function fetchHalls() {
  try {
    const res = await getHallList()
    halls.value = res.data?.records || res.data || []
  } catch (err) { halls.value = [] }
}

onMounted(() => {
  fetchSchedules()
  fetchMovies()
  fetchHalls()
})
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.card { background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-light); }
</style>
