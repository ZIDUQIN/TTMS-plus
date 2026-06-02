<template>
  <div class="admin-layout">
    <NavBar />
    <div class="admin-content">
      <div class="page-header">
        <h2>员工管理</h2>
        <el-button type="primary" :icon="Plus" @click="openAdd">添加员工</el-button>
      </div>

      <div class="card">
        <el-table :data="employees" v-loading="loading" stripe>
          <el-table-column prop="employeeNo" label="工号" width="120" />
          <el-table-column prop="username" label="用户名" width="130" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="roleName" label="角色" width="120">
            <template #default="{ row }">
              <el-tag :type="roleType(row.roleCode)" size="small">{{ row.roleName || row.roleCode }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
                {{ row.status === 1 ? '已禁用' : '正常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime || row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text type="warning" @click="handleResetPwd(row)">重置密码</el-button>
              <el-button size="small" text :type="row.status === 1 ? 'success' : 'warning'" @click="handleToggleStatus(row)">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Add/Edit Dialog -->
      <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑员工' : '添加员工'"
        width="480px"
        :close-on-click-modal="false"
      >
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
          </el-form-item>
          <el-form-item v-if="!isEdit" label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="角色" prop="roleCode">
            <el-select v-model="form.roleCode" placeholder="请选择角色" style="width: 100%">
              <el-option label="超级管理员" value="ROLE_SUPER_ADMIN" />
              <el-option label="管理员" value="ROLE_ADMIN" />
              <el-option label="员工" value="ROLE_STAFF" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '添加员工' }}
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { getEmployeeList, addEmployee, updateEmployee, resetEmployeePassword, toggleEmployeeStatus } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

const employees = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  username: '', password: '', realName: '', phone: '', roleCode: 'ROLE_STAFF'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function roleType(roleCode) {
  const map = { 'ROLE_SUPER_ADMIN': 'danger', 'ROLE_ADMIN': 'warning', 'ROLE_STAFF': 'info' }
  return map[roleCode] || 'info'
}

function formatDateTime(s) {
  if (!s) return '--'
  const d = new Date(s)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function resetForm() {
  Object.assign(form, { username: '', password: '', realName: '', phone: '', roleCode: 'ROLE_STAFF' })
}

function openAdd() {
  isEdit.value = false; editingId.value = null; resetForm(); dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true; editingId.value = row.id
  form.username = row.username
  form.realName = row.realName
  form.phone = row.phone
  form.roleCode = row.roleCode
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  // For edit mode, skip password validation
  const validateFields = isEdit.value ? ['realName', 'phone', 'roleCode'] : undefined
  const valid = await formRef.value.validate(validateFields).catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      delete payload.username
      delete payload.password
    }
    if (isEdit.value) {
      await updateEmployee({ id: editingId.value, ...payload })
      ElMessage.success('员工信息已更新')
    } else {
      await addEmployee(payload)
      ElMessage.success('员工添加成功')
    }
    dialogVisible.value = false
    fetchEmployees()
  } catch (err) { /* handled */ }
  finally { submitting.value = false }
}

async function handleResetPwd(row) {
  try {
    await ElMessageBox.confirm(`确定要重置员工"${row.realName}"的密码吗？`, '重置密码', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  try {
    await resetEmployeePassword(row.id)
    ElMessage.success('密码已重置')
  } catch (err) { /* handled */ }
}

async function handleToggleStatus(row) {
  const action = row.status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}员工"${row.realName}"吗？`, `${action}确认`, {
      confirmButtonText: `确定${action}`, cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  try {
    await toggleEmployeeStatus(row.id)
    row.status = row.status === 1 ? 0 : 1
    ElMessage.success(`员工已${action}`)
  } catch (err) { /* handled */ }
}

async function fetchEmployees() {
  loading.value = true
  try {
    const res = await getEmployeeList()
    employees.value = res.data?.records || res.data || []
  } catch (err) { employees.value = [] }
  finally { loading.value = false }
}

onMounted(fetchEmployees)
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg-secondary); }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.card { background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-light); }
</style>
