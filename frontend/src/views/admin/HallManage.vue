<template>
  <div class="admin-layout">
    <NavBar />
    <div class="admin-content">
      <div class="page-header">
        <h2>影厅管理</h2>
        <el-button type="primary" :icon="Plus" @click="openAdd">添加影厅</el-button>
      </div>

      <div class="card">
        <el-table :data="halls" v-loading="loading" stripe>
          <el-table-column prop="hallName" label="影厅名称" min-width="120" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ hallTypeLabel(row.hallType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="排数 x 列数" width="120">
            <template #default="{ row }">{{ row.rowCount }} x {{ row.colCount }}</template>
          </el-table-column>
          <el-table-column label="座位数" width="80">
            <template #default="{ row }">{{ row.rowCount * row.colCount }}</template>
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

      <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑影厅' : '添加影厅'" width="480px" :close-on-click-modal="false">
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
                <el-input-number v-model="form.rowCount" :min="3" :max="30" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="列数" prop="colCount">
                <el-input-number v-model="form.colCount" :min="3" :max="30" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="座位数">
            <el-input :model-value="form.rowCount * form.colCount" disabled>
              <template #suffix>个座位（自动计算）</template>
            </el-input>
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
import { ref, onMounted, reactive } from 'vue'
import { getHallList, addHall, updateHall, deleteHall } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

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

const form = reactive({ name: '', type: 'STANDARD', rowCount: 8, colCount: 12 })
const rules = {
  name: [{ required: true, message: '请输入影厅名称', trigger: 'blur' }],
  rowCount: [{ required: true, message: '请输入排数', trigger: 'blur' }],
  colCount: [{ required: true, message: '请输入列数', trigger: 'blur' }]
}

function resetForm() { Object.assign(form, { name: '', type: 'STANDARD', rowCount: 8, colCount: 12 }) }
function openAdd() { isEdit.value = false; editingId.value = null; resetForm(); dialogVisible.value = true }

function openEdit(row) {
  isEdit.value = true; editingId.value = row.id
  form.name = row.hallName; form.type = row.hallType || 'STANDARD'; form.rowCount = row.rowCount; form.colCount = row.colCount
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = { name: form.name, type: form.type, rowCount: form.rowCount, colCount: form.colCount, capacity: form.rowCount * form.colCount }
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
  try { await ElMessageBox.confirm(`确定要删除影厅"${row.hallName}"吗？`, '删除确认', { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }) } catch { return }
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
</style>
