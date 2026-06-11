<template>
  <div class="employee-page">
    <div class="employee-container">
      <!-- Sticky Glass Header -->
      <header class="sticky-header">
        <div class="sticky-header__left">
          <h2 class="sticky-header__title">员工管理</h2>
        </div>
        <div class="sticky-header__right">
          <div class="search-box">
            <span class="material-symbols-outlined search-box__icon">search</span>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索工号、姓名..."
              class="search-box__input"
            />
          </div>
          <button class="golden-btn" @click="openAdd">
            <span class="material-symbols-outlined">add</span>
            <span>添加员工</span>
          </button>
        </div>
      </header>

      <!-- Stats Bento Grid -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-card__top">
            <span class="stat-card__label">总员工数</span>
            <span class="material-symbols-outlined stat-card__icon">groups</span>
          </div>
          <div class="stat-card__value">
            <span class="stat-card__num">{{ employees.length }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-card__top">
            <span class="stat-card__label">当前在岗</span>
            <span class="material-symbols-outlined stat-card__icon">event_seat</span>
          </div>
          <div class="stat-card__value">
            <span class="stat-card__num">{{ activeCount }}</span>
            <span class="stat-card__sub">正在服务</span>
          </div>
        </div>
        <div class="stat-card stat-card--wide">
          <div class="stat-card__top">
            <span class="stat-card__label">管理提示</span>
          </div>
          <p class="stat-card__notice">
            员工密码重置后，初始密码统一为
            <code class="notice-code">123456</code>。
            角色权限变更将在下次登录时生效。
          </p>
        </div>
      </div>

      <!-- Employee Table -->
      <div class="table-panel">
        <el-table
          :data="filteredEmployees"
          v-loading="loading"
          class="employee-table"
          row-class-name="emp-row"
        >
          <el-table-column label="工号" width="130">
            <template #default="{ row }">
              <span class="emp-no">{{ row.employeeNo || '--' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="姓名" min-width="160">
            <template #default="{ row }">
              <div class="emp-name-cell">
                <div class="emp-avatar" :class="{ 'emp-avatar--disabled': row.status === 1 }">
                  {{ initials(row.realName || row.username) }}
                </div>
                <span class="emp-name">{{ row.realName || row.username }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="手机" width="140">
            <template #default="{ row }">{{ formatPhone(row.phone) }}</template>
          </el-table-column>

          <el-table-column label="角色" width="140">
            <template #default="{ row }">
              <span
                class="role-badge"
                :class="isSuperAdmin(row) ? 'role-badge--admin' : 'role-badge--staff'"
              >
                {{ row.roleName || (isSuperAdmin(row) ? '超级管理员' : '普通员工') }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <div class="status-cell">
                <span
                  class="status-dot"
                  :class="row.status === 1 ? 'status-dot--off' : 'status-dot--on'"
                ></span>
                <span class="status-text" :class="{ 'status-text--off': row.status === 1 }">
                  {{ row.status === 1 ? '已禁用' : '启用中' }}
                </span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="{ row }">
              <div class="action-btns">
                <button class="action-btn" title="编辑" @click="openEdit(row)">
                  <span class="material-symbols-outlined">edit</span>
                </button>
                <button class="action-btn" title="重置密码" @click="handleResetPwd(row)">
                  <span class="material-symbols-outlined">lock_reset</span>
                </button>
                <button
                  class="action-btn"
                  :class="row.status === 1 ? 'action-btn--green' : 'action-btn--danger'"
                  :title="row.status === 1 ? '启用' : '禁用'"
                  @click="handleToggleStatus(row)"
                >
                  <span class="material-symbols-outlined">
                    {{ row.status === 1 ? 'check_circle' : 'do_not_disturb_on' }}
                  </span>
                </button>
              </div>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无员工数据" :image-size="80" />
          </template>
        </el-table>

        <!-- Pagination -->
        <div class="table-footer">
          <span class="table-footer__info">共 {{ employees.length }} 条数据</span>
        </div>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <Teleport to="body">
      <div class="modal-overlay" :class="{ show: dialogVisible }" @click.self="dialogVisible = false">
        <div class="modal-panel" :class="{ show: dialogVisible }">
          <div class="modal-header">
            <h3 class="modal-title">{{ isEdit ? '编辑员工' : '添加新员工' }}</h3>
            <button class="modal-close" @click="dialogVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <!-- Info Alert (add mode only) -->
          <div v-if="!isEdit" class="modal-alert">
            <span class="material-symbols-outlined">info</span>
            <p>系统将自动生成初始工号。密码默认设置为 <code>123456</code>，请告知员工首次登录后修改。</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="modal-form">
            <div class="form-row-2">
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="form.realName" placeholder="输入真实姓名" />
              </el-form-item>
              <el-form-item label="手机号码" prop="phone">
                <el-input v-model="form.phone" placeholder="11位手机号" maxlength="11" />
              </el-form-item>
            </div>
            <div class="form-row-2">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="form.username" placeholder="登录用户名" :disabled="isEdit" />
              </el-form-item>
              <el-form-item v-if="!isEdit" label="密码" prop="password">
                <el-input v-model="form.password" type="password" placeholder="默认密码" show-password />
              </el-form-item>
            </div>
            <el-form-item label="分配角色" prop="roleCode">
              <div class="role-radios">
                <label class="role-radio" :class="{ active: form.roleCode === 'ROLE_STAFF' }">
                  <input v-model="form.roleCode" type="radio" value="ROLE_STAFF" />
                  <span class="material-symbols-outlined role-radio__icon">person</span>
                  <span class="role-radio__label">普通员工</span>
                  <span class="role-radio__sub">STAFF</span>
                </label>
                <label class="role-radio" :class="{ active: form.roleCode === 'ROLE_SUPER_ADMIN' }">
                  <input v-model="form.roleCode" type="radio" value="ROLE_SUPER_ADMIN" />
                  <span class="material-symbols-outlined role-radio__icon">shield_person</span>
                  <span class="role-radio__label">超级管理员</span>
                  <span class="role-radio__sub">ADMIN</span>
                </label>
              </div>
            </el-form-item>
          </el-form>

          <div class="modal-actions">
            <button class="btn-cancel" @click="dialogVisible = false">取消</button>
            <button class="golden-btn" :disabled="submitting" @click="handleSubmit">
              <span>{{ isEdit ? '保存修改' : '确认添加' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getEmployeeList, addEmployee, updateEmployee, resetEmployeePassword, toggleEmployeeStatus } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const employees = ref([])
const loading = ref(false)
const searchQuery = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({ username: '', password: '', realName: '', phone: '', roleCode: 'ROLE_STAFF' })

const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少6位' }],
  realName: [{ required: true, message: '请输入姓名' }],
  phone: [{ required: true, message: '请输入手机号' }],
  roleCode: [{ required: true, message: '请选择角色' }],
}

const filteredEmployees = computed(() => {
  if (!searchQuery.value) return employees.value
  const q = searchQuery.value.toLowerCase()
  return employees.value.filter(e =>
    (e.employeeNo || '').toLowerCase().includes(q) ||
    (e.realName || '').toLowerCase().includes(q) ||
    (e.username || '').toLowerCase().includes(q)
  )
})

const activeCount = computed(() => employees.value.filter(e => e.status !== 1).length)

function isSuperAdmin(row) { return (row.roleCode || '').includes('SUPER_ADMIN') }
function initials(name) { return (name || '?').slice(0, 2).toUpperCase() }
function formatPhone(p) { if (!p) return '--'; return p.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') }

function resetForm() { Object.assign(form, { username: '', password: '', realName: '', phone: '', roleCode: 'ROLE_STAFF' }) }

function openAdd() { isEdit.value = false; editingId.value = null; resetForm(); dialogVisible.value = true }

function openEdit(row) {
  isEdit.value = true; editingId.value = row.id
  form.username = row.username; form.realName = row.realName
  form.phone = row.phone; form.roleCode = row.roleCode
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const validateFields = isEdit.value ? ['realName', 'phone', 'roleCode'] : undefined
  const valid = await formRef.value.validate(validateFields).catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) { delete payload.username; delete payload.password }
    if (isEdit.value) { await updateEmployee({ id: editingId.value, ...payload }); ElMessage.success('已更新') }
    else { await addEmployee(payload); ElMessage.success('添加成功') }
    dialogVisible.value = false; fetchEmployees()
  } catch {} finally { submitting.value = false }
}

async function handleResetPwd(row) {
  try { await ElMessageBox.confirm(`确定要重置"${row.realName}"的密码吗？`, '重置密码', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await resetEmployeePassword(row.id); ElMessage.success('密码已重置') } catch {}
}

async function handleToggleStatus(row) {
  const action = row.status === 1 ? '启用' : '禁用'
  try { await ElMessageBox.confirm(`确定要${action}"${row.realName}"吗？`, `${action}确认`, { confirmButtonText: `确定${action}`, cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await toggleEmployeeStatus(row.id); row.status = row.status === 1 ? 0 : 1; ElMessage.success(`已${action}`) } catch {}
}

async function fetchEmployees() {
  loading.value = true
  try { const res = await getEmployeeList(); employees.value = res.data?.records || res.data || [] }
  catch { employees.value = [] }
  finally { loading.value = false }
}

onMounted(fetchEmployees)
</script>

<style scoped>
/* ============================================================
   Employee Management — Dark Glass Edition
   ============================================================ */
.employee-page { min-height: 100vh; }
.employee-container { max-width: 1340px; margin: 0 auto; padding: 24px 32px; }

/* ---- Sticky Glass Header ---- */
.sticky-header {
  position: sticky; top: 0; z-index: 30; display: flex; justify-content: space-between;
  align-items: center; padding: 16px 0; margin-bottom: 24px; gap: 16px; flex-wrap: wrap;
  background: var(--bg-primary);
}
.sticky-header__title { font-family: Georgia, 'Noto Serif SC', serif; font-size: 28px; font-weight: 700; color: var(--text-primary); }
.sticky-header__right { display: flex; align-items: center; gap: 12px; }

.search-box { position: relative; }
.search-box__icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); font-size: 16px; color: var(--text-tertiary); }
.search-box__input {
  width: 220px; padding: 8px 14px 8px 36px; background: var(--bg-card); border: 1px solid var(--border-light);
  border-radius: var(--radius-pill); color: var(--text-primary); font-size: 13px; font-family: inherit; outline: none;
  transition: all 0.2s ease;
}
.search-box__input:focus { border-color: rgba(232, 168, 80, 0.4); box-shadow: 0 0 0 3px rgba(232,168,80,0.06); }
.search-box__input::placeholder { color: var(--text-tertiary); }

.golden-btn {
  display: inline-flex; align-items: center; gap: 6px; padding: 9px 22px; border: none; border-radius: var(--radius-pill);
  font-size: 13px; font-weight: 700; color: #2a1800; cursor: pointer;
  background: linear-gradient(135deg, #e8a850, #ffc67c, #e8a850); transition: all 0.3s ease; font-family: inherit;
}
.golden-btn:hover { filter: brightness(1.1); box-shadow: 0 0 20px rgba(232,168,80,0.3); }
.golden-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ---- Stats Bento Grid ---- */
.stats-grid { display: grid; grid-template-columns: 1fr 1fr 2fr; gap: 16px; margin-bottom: 24px; }

.stat-card {
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl);
  padding: 20px 24px; display: flex; flex-direction: column; justify-content: space-between;
  transition: border-color 0.2s ease;
}
.stat-card:hover { border-color: rgba(232, 168, 80, 0.2); }
.stat-card__top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.stat-card__label { font-size: 11px; font-weight: 600; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.05em; }
.stat-card__icon { font-size: 20px; color: rgba(232,168,80,0.3); transition: color 0.2s; }
.stat-card:hover .stat-card__icon { color: rgba(232,168,80,0.7); }
.stat-card__num { font-family: 'JetBrains Mono', 'Consolas', monospace; font-size: 32px; font-weight: 700; color: var(--text-primary); }
.stat-card__sub { font-size: 12px; color: var(--text-tertiary); margin-left: 8px; }
.stat-card__notice { font-size: 13px; color: var(--text-secondary); line-height: 1.6; }
.notice-code { background: rgba(255,255,255,0.08); padding: 1px 6px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; color: var(--color-primary); font-size: 12px; }
[data-theme='light'] .notice-code { background: rgba(0,0,0,0.06); }

/* ---- Table Panel ---- */
.table-panel {
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-xl);
  overflow: hidden;
}
[data-theme='dark'] .table-panel { background: rgba(20,20,31,0.7); backdrop-filter: blur(20px); }

/* ---- Employee Table Overrides ---- */
:deep(.emp-row) { transition: background 0.15s ease; }
:deep(.emp-row:hover) { background: rgba(255,255,255,0.02); }
[data-theme='dark'] :deep(.emp-row:hover) { background: rgba(255,255,255,0.03); }

.emp-no { font-family: 'JetBrains Mono', 'Consolas', monospace; font-size: 13px; color: var(--color-primary); font-weight: 500; }

.emp-name-cell { display: flex; align-items: center; gap: 10px; }
.emp-avatar {
  width: 32px; height: 32px; border-radius: 50%; background: var(--bg-hover);
  display: flex; align-items: center; justify-content: center; font-size: 11px;
  font-weight: 700; color: var(--text-primary); flex-shrink: 0;
}
.emp-avatar--disabled { opacity: 0.4; }
.emp-name { font-weight: 500; color: var(--text-primary); }

/* ---- Role Badge ---- */
.role-badge {
  display: inline-block; padding: 3px 12px; border-radius: var(--radius-pill);
  font-size: 11px; font-weight: 600; letter-spacing: 0.04em;
}
.role-badge--admin { background: rgba(232,168,80,0.12); color: var(--color-primary); border: 1px solid rgba(232,168,80,0.25); }
.role-badge--staff { background: var(--bg-hover); color: var(--text-secondary); border: 1px solid var(--border-light); }

/* ---- Status ---- */
.status-cell { display: flex; align-items: center; gap: 8px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot--on { background: var(--color-emerald); animation: pulse-dot 2s ease-in-out infinite; }
.status-dot--off { background: var(--text-tertiary); }
.status-text { font-size: 12px; color: var(--text-secondary); }
.status-text--off { color: var(--text-tertiary); }
@keyframes pulse-dot { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

/* ---- Action Buttons ---- */
.action-btns { display: flex; gap: 2px; justify-content: center; }
.action-btn {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  border: none; border-radius: var(--radius-md); background: transparent; color: var(--text-secondary);
  cursor: pointer; transition: all 0.15s ease;
}
.action-btn:hover { background: var(--bg-hover); color: var(--color-primary); }
.action-btn--danger:hover { color: var(--color-danger); }
.action-btn--green:hover { color: var(--color-emerald); }
.action-btn .material-symbols-outlined { font-size: 18px; }

/* ---- Table Footer ---- */
.table-footer { display: flex; justify-content: center; padding: 16px; border-top: 1px solid var(--border-light); }
.table-footer__info { font-size: 12px; color: var(--text-tertiary); }

/* ============================================================
   MODAL — Glass Panel
   ============================================================ */
.modal-overlay {
  position: fixed; inset: 0; z-index: 1000; display: flex; align-items: center; justify-content: center;
  padding: 24px; background: rgba(0,0,0,0.7); backdrop-filter: blur(4px);
  opacity: 0; pointer-events: none; transition: opacity 0.3s ease;
}
.modal-overlay.show { opacity: 1; pointer-events: auto; }

.modal-panel {
  width: 100%; max-width: 520px; max-height: 90vh; overflow-y: auto;
  background: var(--bg-card); border-radius: var(--radius-xl); border: 1px solid rgba(255,255,255,0.06);
  box-shadow: 0 0 100px rgba(232,168,80,0.08), 0 24px 64px rgba(0,0,0,0.5);
  transform: scale(0.95) translateY(12px); transition: transform 0.3s ease;
}
[data-theme='dark'] .modal-panel { background: rgba(20,20,31,0.92); backdrop-filter: blur(20px); }
.modal-panel.show { transform: scale(1) translateY(0); }

.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 24px 24px 0; }
.modal-title { font-size: 20px; font-weight: 700; color: var(--text-primary); }
.modal-close {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; border: none;
  border-radius: 50%; background: transparent; color: var(--text-secondary); cursor: pointer; transition: all 0.15s ease;
}
.modal-close:hover { background: var(--bg-hover); color: var(--color-primary); }
.modal-close .material-symbols-outlined { font-size: 20px; }

.modal-alert {
  margin: 16px 24px 0; padding: 12px 16px; border-radius: var(--radius-md);
  background: rgba(232,168,80,0.06); border: 1px solid rgba(232,168,80,0.2);
  display: flex; gap: 10px; font-size: 12px; color: var(--text-secondary); line-height: 1.5;
}
.modal-alert .material-symbols-outlined { font-size: 18px; color: var(--color-primary); flex-shrink: 0; margin-top: 1px; }
.modal-alert code { background: rgba(255,255,255,0.08); padding: 1px 6px; border-radius: 3px; font-family: 'JetBrains Mono', monospace; color: var(--color-primary); font-weight: 600; }

.modal-form { padding: 20px 24px 0; }
.form-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }

/* Role radio cards */
.role-radios { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; width: 100%; }
.role-radio {
  display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 16px 12px;
  border-radius: var(--radius-lg); border: 1px solid var(--border-light); cursor: pointer;
  transition: all 0.2s ease; text-align: center;
}
.role-radio:hover { background: var(--bg-hover); }
.role-radio.active { border-color: var(--color-primary); background: rgba(232,168,80,0.06); }
.role-radio input { display: none; }
.role-radio__icon { font-size: 28px; color: var(--text-secondary); transition: color 0.2s; }
.role-radio.active .role-radio__icon { color: var(--color-primary); }
.role-radio__label { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.role-radio__sub { font-size: 11px; color: var(--text-tertiary); }

.modal-actions { display: flex; gap: 12px; padding: 20px 24px 24px; }
.modal-actions .golden-btn { flex: 1; justify-content: center; padding: 14px; font-size: 14px; }
.btn-cancel {
  flex: 1; padding: 14px; border: 1px solid var(--border-color); border-radius: var(--radius-lg);
  background: transparent; color: var(--text-primary); font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.2s ease; font-family: inherit;
}
.btn-cancel:hover { background: var(--bg-hover); }

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .sticky-header { flex-direction: column; align-items: flex-start; }
  .stats-grid { grid-template-columns: 1fr 1fr; }
  .stat-card--wide { grid-column: span 2; }
  .form-row-2 { grid-template-columns: 1fr; }
  .role-radios { grid-template-columns: 1fr; }
  .search-box__input { width: 160px; }
}
</style>
