<template>
  <div class="member-page">
    <div class="member-container">
      <!-- Hero Header -->
      <header class="page-hero">
        <div class="page-hero__text">
          <h2 class="page-hero__title">会员管理中心</h2>
          <p class="page-hero__desc">管理会员等级体系、追踪用户消费行为，优化会员运营策略。</p>
        </div>
        <button class="golden-btn" @click="activeTab = 'users'; openSetLevel({})">
          <span class="material-symbols-outlined">person_add</span>
          <span>新增会员</span>
        </button>
      </header>

      <!-- Tab Switcher -->
      <div class="tab-switcher">
        <button class="tab-btn" :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">
          <span class="material-symbols-outlined">group</span> 会员名册
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'levels' }" @click="activeTab = 'levels'">
          <span class="material-symbols-outlined">stars</span> 等级配置
        </button>
      </div>

      <!-- ========== TAB 1: Member Roster ========== -->
      <div v-show="activeTab === 'users'">
        <!-- Filter & Search -->
        <section class="filter-section">
          <div class="filter-section__left">
            <h3 class="filter-section__title">会员名册</h3>
            <div class="search-box">
              <input
                v-model="userSearch"
                type="text"
                placeholder="搜索会员姓名、手机号或 ID..."
                class="search-input"
              />
              <span class="material-symbols-outlined search-icon">search</span>
            </div>
          </div>
          <div class="filter-section__right">
            <div class="level-pills">
              <button
                v-for="lvl in levelFilterOptions"
                :key="lvl.value"
                class="level-pill"
                :class="{ active: userLevelFilter === lvl.value }"
                @click="userLevelFilter = lvl.value"
              >
                {{ lvl.label }}
              </button>
            </div>
          </div>
        </section>

        <!-- Member Table -->
        <div class="table-card">
          <el-table
            :data="filteredMemberUsers"
            v-loading="usersLoading"
            class="member-table"
            @row-click="openSetLevel"
            row-class-name="member-row"
          >
            <el-table-column label="会员信息" min-width="220">
              <template #default="{ row }">
                <div class="member-cell">
                  <el-avatar :size="44" :icon="UserFilled" class="member-avatar" />
                  <div class="member-info">
                    <span class="member-name">{{ row.realName || row.username || '--' }}</span>
                    <span class="member-id">ID: {{ row.id || '--' }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="会员等级" width="140">
              <template #default="{ row }">
                <span class="level-badge" :class="'level-badge--' + levelColor(row)">
                  <span class="material-symbols-outlined level-badge__icon">
                    {{ levelIcon(row) }}
                  </span>
                  {{ row.levelName || '普通会员' }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="积分" width="90" align="center">
              <template #default="{ row }">{{ row.points || 0 }}</template>
            </el-table-column>

            <el-table-column label="储值余额" width="110" align="center">
              <template #default="{ row }">
                <span class="balance-value">¥{{ row.balance || 0 }}</span>
              </template>
            </el-table-column>

            <el-table-column label="手机号" width="130">
              <template #default="{ row }">{{ formatPhone(row.phone) }}</template>
            </el-table-column>

            <el-table-column label="最近活跃" width="120">
              <template #default="{ row }">{{ row.lastActive || formatDate(row.createTime) || '--' }}</template>
            </el-table-column>

            <el-table-column label="操作" width="200" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-btns">
                  <button class="action-btn" @click.stop="openSetLevel(row)" title="设置等级">
                    <span class="material-symbols-outlined">upgrade</span>
                  </button>
                  <button class="action-btn" @click.stop="openAdjustPoints(row)" title="调整积分">
                    <span class="material-symbols-outlined">toll</span>
                  </button>
                  <button class="action-btn action-btn--danger" @click.stop="handleDeleteUser(row)" title="删除用户">
                    <span class="material-symbols-outlined">person_remove</span>
                  </button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- Pagination -->
          <div class="table-footer">
            <span class="table-footer__info">
              共 {{ usersTotal }} 条记录
            </span>
            <div class="pagination-bar">
              <button class="page-arrow" :disabled="usersPage <= 1" @click="usersPage--; fetchUsers()">
                <span class="material-symbols-outlined">chevron_left</span>
              </button>
              <button
                v-for="p in pageRange"
                :key="p"
                class="page-num"
                :class="{ active: p === usersPage }"
                @click="usersPage = p; fetchUsers()"
              >
                {{ p }}
              </button>
              <button class="page-arrow" :disabled="usersPage >= totalPages" @click="usersPage++; fetchUsers()">
                <span class="material-symbols-outlined">chevron_right</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Stats Bento Grid -->
        <section class="stats-grid">
          <div class="stat-card">
            <p class="stat-card__label">活跃会员</p>
            <div class="stat-card__row">
              <span class="stat-card__num">{{ usersTotal }}</span>
              <span class="stat-card__change up">会员总数</span>
            </div>
            <p class="stat-card__note">系统注册用户总数</p>
          </div>
          <div class="stat-card">
            <p class="stat-card__label">等级分布</p>
            <div class="stat-card__row">
              <span class="stat-card__num">{{ levels.length }}</span>
              <span class="stat-card__change">个等级</span>
            </div>
            <div class="stat-bar-wrap">
              <div
                v-for="(lvl, i) in levels"
                :key="lvl.id"
                class="stat-bar-seg"
                :style="{ flex: 1, background: levelBarColor(i) }"
              ></div>
            </div>
          </div>
          <div class="stat-card stat-card--highlight">
            <p class="stat-card__label">最高等级</p>
            <div class="stat-card__row">
              <span class="stat-card__num stat-card__num--accent">{{ topLevelName }}</span>
            </div>
            <p class="stat-card__note">{{ topLevelDiscount }} 折扣 | {{ topLevelPoints }}x 积分加速</p>
          </div>
        </section>
      </div>

      <!-- ========== TAB 2: Level Configuration ========== -->
      <div v-show="activeTab === 'levels'">
        <div style="margin-bottom:16px">
          <button class="golden-btn" style="padding:10px 20px;font-size:13px" @click="openLevelDialog()">
            <span class="material-symbols-outlined">add</span>
            <span>添加等级</span>
          </button>
        </div>
        <div class="table-card">
          <el-table :data="levels" v-loading="levelsLoading" class="member-table">
            <el-table-column prop="levelName" label="等级名称" min-width="140">
              <template #default="{ row }">
                <span class="level-badge" :class="'level-badge--' + levelColorByName(row.levelName)">
                  <span class="material-symbols-outlined level-badge__icon">
                    {{ levelIconByName(row.levelName) }}
                  </span>
                  {{ row.levelName }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="折扣率" width="120">
              <template #default="{ row }">{{ ((1 - row.discountRate) * 100).toFixed(0) }}% off</template>
            </el-table-column>
            <el-table-column label="积分加速" width="100">
              <template #default="{ row }">{{ row.pointsRate || 1 }}x</template>
            </el-table-column>
            <el-table-column label="最低消费" width="120">
              <template #default="{ row }">¥{{ row.minSpending || 0 }}</template>
            </el-table-column>
            <el-table-column label="排序" width="80" prop="sortOrder" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <div class="action-btns">
                  <button class="action-btn" @click="openLevelDialog(row)" title="编辑">
                    <span class="material-symbols-outlined">edit</span>
                  </button>
                  <button class="action-btn action-btn--danger" @click="handleLevelDelete(row.id)" title="删除">
                    <span class="material-symbols-outlined">delete</span>
                  </button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- ===== Dialogs ===== -->

      <!-- Level Dialog -->
      <el-dialog v-model="showLevelDialog" :title="editingLevel ? '编辑等级' : '添加等级'" width="440px" :close-on-click-modal="false">
        <el-form ref="levelFormRef" :model="levelForm" :rules="levelRules" label-width="100px">
          <el-form-item label="等级名称" prop="levelName">
            <el-input v-model="levelForm.levelName" placeholder="如：金卡会员" />
          </el-form-item>
          <el-form-item label="折扣率" prop="discountRate">
            <el-input-number v-model="levelForm.discountRate" :min="0.1" :max="1" :step="0.05" :precision="2" style="width:100%" />
            <div style="font-size:12px;color:var(--text-tertiary);margin-top:4px">
              享受原价 {{ (levelForm.discountRate * 100).toFixed(0) }}% 的折扣价格
            </div>
          </el-form-item>
          <el-form-item label="积分加速" prop="pointsRate">
            <el-input-number v-model="levelForm.pointsRate" :min="1" :max="10" style="width:100%" />
          </el-form-item>
          <el-form-item label="最低消费" prop="minSpending">
            <el-input-number v-model="levelForm.minSpending" :min="0" :step="100" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="levelForm.sortOrder" :min="0" style="width:100%" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showLevelDialog = false">取消</el-button>
          <el-button type="primary" :loading="levelSubmitting" @click="handleLevelSubmit">保存</el-button>
        </template>
      </el-dialog>

      <!-- Set Level Dialog -->
      <el-dialog v-model="showSetLevel" title="设置会员等级" width="380px">
        <div style="margin-bottom:14px;color:var(--text-secondary);font-size:13px">
          用户：<b>{{ selectedUser?.username || selectedUser?.realName }}</b>
          <span v-if="selectedUser?.levelName">，当前等级：<b>{{ selectedUser.levelName }}</b></span>
        </div>
        <el-select v-model="selectedLevelId" placeholder="选择等级" style="width:100%">
          <el-option
            v-for="l in levels"
            :key="l.id"
            :label="l.levelName + ' (' + ((1 - l.discountRate) * 100).toFixed(0) + '%折扣)'"
            :value="l.id"
          />
        </el-select>
        <template #footer>
          <el-button @click="showSetLevel = false">取消</el-button>
          <el-button type="primary" :loading="setLevelLoading" @click="handleSetLevel">确认</el-button>
        </template>
      </el-dialog>

      <!-- Adjust Points Dialog -->
      <el-dialog v-model="showAdjustPoints" title="调整积分" width="360px">
        <div style="margin-bottom:12px;color:var(--text-secondary);font-size:13px">
          用户：<b>{{ selectedUser?.username }}</b>
        </div>
        <el-input-number v-model="pointsDelta" :min="-10000" :max="10000" style="width:100%" />
        <div style="margin-top:10px;padding:10px 14px;background:var(--bg-secondary);border-radius:var(--radius-md);font-size:13px">
          当前积分：<b>{{ selectedUser?.points || 0 }}</b>
          <span v-if="pointsDelta !== 0" style="margin-left:10px;color:var(--color-primary);font-weight:600">
            → {{ Math.max(0, (selectedUser?.points || 0) + pointsDelta) }}
          </span>
        </div>
        <template #footer>
          <el-button @click="showAdjustPoints = false">取消</el-button>
          <el-button type="primary" :loading="adjustLoading" @click="handleAdjustPoints">确认</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getMemberLevels, addMemberLevel, updateMemberLevel, deleteMemberLevel, getMemberUsers, setUserLevel, adjustUserPoints, deleteUser } from '@/api/member'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const activeTab = ref('users')
const userSearch = ref('')
const userLevelFilter = ref('')

// Level management
const levels = ref([]); const levelsLoading = ref(false)
const showLevelDialog = ref(false); const editingLevel = ref(null); const levelSubmitting = ref(false)
const levelFormRef = ref(null)
const levelForm = reactive({ levelName: '', discountRate: 0.9, minSpending: 0, pointsRate: 1, sortOrder: 0 })
const levelRules = {
  levelName: [{ required: true, message: '请输入等级名称' }],
  discountRate: [{ required: true, message: '请输入折扣率' }]
}

// Member users
const memberUsers = ref([]); const usersLoading = ref(false)
const usersPage = ref(1); const usersTotal = ref(0)
const showSetLevel = ref(false); const showAdjustPoints = ref(false)
const selectedUser = ref(null); const selectedLevelId = ref(null)
const pointsDelta = ref(0); const setLevelLoading = ref(false); const adjustLoading = ref(false)

const levelFilterOptions = [
  { label: '全部', value: '' },
  { label: '钻石', value: '钻石' },
  { label: '黄金', value: '黄金' },
  { label: '白银', value: '白银' },
]

const filteredMemberUsers = computed(() => {
  let list = memberUsers.value
  if (userLevelFilter.value) list = list.filter(u => (u.levelName || '').includes(userLevelFilter.value))
  if (userSearch.value) {
    const q = userSearch.value.toLowerCase()
    list = list.filter(u =>
      (u.username || '').toLowerCase().includes(q) ||
      (u.realName || '').toLowerCase().includes(q) ||
      (u.phone || '').includes(q) ||
      String(u.id || '').includes(q)
    )
  }
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(usersTotal.value / 10)))
const pageRange = computed(() => {
  const total = totalPages.value; const cur = usersPage.value
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1)
  if (cur <= 3) return [1, 2, 3, 4, 5]
  if (cur >= total - 2) return [total - 4, total - 3, total - 2, total - 1, total]
  return [cur - 2, cur - 1, cur, cur + 1, cur + 2]
})

const topLevelName = computed(() => levels.value.length > 0 ? levels.value[levels.value.length - 1]?.levelName || '--' : '--')
const topLevelDiscount = computed(() => {
  if (levels.value.length === 0) return '--'
  const lvl = levels.value[levels.value.length - 1]
  return ((1 - (lvl.discountRate || 1)) * 100).toFixed(0) + '%'
})
const topLevelPoints = computed(() => levels.value.length > 0 ? levels.value[levels.value.length - 1]?.pointsRate || 1 : 1)

function levelColor(row) {
  const name = (row.levelName || '').toLowerCase()
  if (name.includes('钻石') || name.includes('diamond')) return 'diamond'
  if (name.includes('金') || name.includes('gold')) return 'gold'
  if (name.includes('银') || name.includes('silver')) return 'silver'
  return 'default'
}
function levelIcon(row) {
  const name = (row.levelName || '').toLowerCase()
  if (name.includes('钻石') || name.includes('diamond')) return 'diamond'
  if (name.includes('金') || name.includes('gold')) return 'stars'
  if (name.includes('银') || name.includes('silver')) return 'workspace_premium'
  return 'person'
}
function levelColorByName(name) {
  const n = (name || '').toLowerCase()
  if (n.includes('钻石') || n.includes('diamond')) return 'diamond'
  if (n.includes('金') || n.includes('gold')) return 'gold'
  if (n.includes('银') || n.includes('silver')) return 'silver'
  return 'default'
}
function levelIconByName(name) {
  const n = (name || '').toLowerCase()
  if (n.includes('钻石') || n.includes('diamond')) return 'diamond'
  if (n.includes('金') || n.includes('gold')) return 'stars'
  if (n.includes('银') || n.includes('silver')) return 'workspace_premium'
  return 'person'
}
function levelBarColor(i) {
  const colors = ['#845400', '#C8960C', '#B0A090', '#D5C4B2']
  return colors[i] || '#e0ddd8'
}
function formatPhone(phone) { if (!phone) return '--'; return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') }
function formatDate(d) { if (!d) return ''; const dt = new Date(d); return `${dt.getFullYear()}-${String(dt.getMonth()+1).padStart(2,'0')}-${String(dt.getDate()).padStart(2,'0')}` }

// === API ===
async function fetchLevels() { levelsLoading.value = true; try { const r = await getMemberLevels(); levels.value = r.data || [] } catch {} levelsLoading.value = false }
async function fetchUsers() {
  usersLoading.value = true
  try { const r = await getMemberUsers({ page: usersPage.value, size: 10 }); memberUsers.value = r.data?.records || r.data || []; usersTotal.value = r.data?.total || 0 }
  catch { memberUsers.value = [] }
  usersLoading.value = false
}

function openLevelDialog(row) {
  editingLevel.value = row || null
  if (row) { levelForm.levelName = row.levelName || ''; levelForm.discountRate = row.discountRate || 0.9; levelForm.minSpending = row.minSpending || 0; levelForm.pointsRate = row.pointsRate || 1; levelForm.sortOrder = row.sortOrder || 0 }
  else { levelForm.levelName = ''; levelForm.discountRate = 0.9; levelForm.minSpending = 0; levelForm.pointsRate = 1; levelForm.sortOrder = 0 }
  showLevelDialog.value = true
}

async function handleLevelSubmit() {
  if (!levelFormRef.value) return
  const valid = await levelFormRef.value.validate().catch(() => false); if (!valid) return
  levelSubmitting.value = true
  try {
    const data = { levelName: levelForm.levelName, discountRate: levelForm.discountRate, minSpending: levelForm.minSpending, pointsRate: levelForm.pointsRate, sortOrder: levelForm.sortOrder }
    if (editingLevel.value) { await updateMemberLevel({ id: editingLevel.value.id, ...data }); ElMessage.success('等级已更新') }
    else { await addMemberLevel(data); ElMessage.success('等级已添加') }
    showLevelDialog.value = false; fetchLevels()
  } catch {} finally { levelSubmitting.value = false }
}

async function handleLevelDelete(id) { try { await deleteMemberLevel(id); ElMessage.success('已删除'); fetchLevels() } catch {} }

function openSetLevel(user) {
  if (!user || !user.id) { ElMessage.warning('请先选择一个用户'); return }
  selectedUser.value = user; selectedLevelId.value = user.memberLevelId || user.levelId || ''; showSetLevel.value = true
}

async function handleSetLevel() {
  if (!selectedLevelId.value) { ElMessage.warning('请选择等级'); return }
  setLevelLoading.value = true
  try { await setUserLevel(selectedUser.value.id, selectedLevelId.value); ElMessage.success('设置成功'); showSetLevel.value = false; fetchUsers() }
  catch {} finally { setLevelLoading.value = false }
}

function openAdjustPoints(user) { selectedUser.value = user; pointsDelta.value = 0; showAdjustPoints.value = true }
async function handleAdjustPoints() {
  if (pointsDelta.value === 0) { ElMessage.warning('请输入积分变动值'); return }
  adjustLoading.value = true
  try { await adjustUserPoints(selectedUser.value.id, pointsDelta.value); ElMessage.success(`积分已${pointsDelta.value >= 0 ? '增加' : '减少'}${Math.abs(pointsDelta.value)}`); showAdjustPoints.value = false; fetchUsers() }
  catch {} finally { adjustLoading.value = false }
}

async function handleDeleteUser(user) { try { await deleteUser(user.id); ElMessage.success('已删除'); fetchUsers() } catch {} }

onMounted(() => { fetchLevels(); fetchUsers() })
</script>

<style scoped>
/* ============================================================
   Member Management — Editorial Light Edition
   ============================================================ */

.member-page { min-height: 100vh; background: var(--bg-primary); }
.member-container { max-width: 1280px; margin: 0 auto; padding: 24px 32px; }

/* ---- Hero ---- */
.page-hero { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 28px; }
.page-hero__title { font-family: Georgia, 'Noto Serif SC', serif; font-size: 36px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.page-hero__desc { font-size: 14px; color: var(--text-secondary); max-width: 420px; }

.golden-btn {
  display: inline-flex; align-items: center; gap: 8px; padding: 12px 26px; border: none;
  border-radius: var(--radius-lg); font-size: 14px; font-weight: 700; color: #fff; cursor: pointer;
  background: var(--color-primary); transition: all 0.3s ease; font-family: inherit; white-space: nowrap;
}
[data-theme='dark'] .golden-btn { color: #2a1800; background: linear-gradient(135deg, #e8a850 0%, #ffc67c 50%, #e8a850 100%); }
.golden-btn:hover { filter: brightness(1.1); box-shadow: 0 0 20px rgba(132, 84, 0, 0.3); }

/* ---- Tab Switcher ---- */
.tab-switcher { display: flex; gap: 4px; margin-bottom: 28px; border-bottom: 1px solid var(--border-light); }
.tab-btn {
  display: inline-flex; align-items: center; gap: 6px; padding: 10px 22px; border: none;
  border-bottom: 2px solid transparent; background: transparent; color: var(--text-secondary);
  font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s ease; font-family: inherit; margin-bottom: -1px;
}
.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active { color: var(--color-primary); border-bottom-color: var(--color-primary); font-weight: 600; }
.tab-btn .material-symbols-outlined { font-size: 18px; }

/* ---- Filter Section ---- */
.filter-section { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; margin-bottom: 24px; flex-wrap: wrap; }
.filter-section__title { font-family: Georgia, 'Noto Serif SC', serif; font-size: 28px; font-weight: 600; color: var(--text-primary); margin-bottom: 14px; }

.search-box { position: relative; max-width: 420px; }
.search-input {
  width: 100%; padding: 10px 40px 10px 0; background: transparent; border: none;
  border-bottom: 1px solid var(--border-color); color: var(--text-primary);
  font-size: 15px; font-family: inherit; outline: none; transition: border-color 0.2s ease;
}
.search-input:focus { border-bottom-color: var(--color-primary); }
.search-input::placeholder { color: var(--text-tertiary); }
.search-icon { position: absolute; right: 0; top: 50%; transform: translateY(-50%); font-size: 20px; color: var(--text-tertiary); }

.level-pills { display: flex; background: var(--bg-secondary); padding: 4px; border-radius: var(--radius-pill); border: 1px solid var(--border-light); }
.level-pill {
  padding: 8px 20px; border: none; border-radius: var(--radius-pill); font-size: 13px; font-weight: 500;
  color: var(--text-secondary); background: transparent; cursor: pointer; transition: all 0.2s ease; font-family: inherit;
}
.level-pill:hover { color: var(--text-primary); }
.level-pill.active { background: var(--bg-card); color: var(--color-primary); box-shadow: var(--shadow-light); font-weight: 600; }

/* ---- Table Card ---- */
.table-card { background: var(--bg-card); border-radius: var(--radius-xl); border: 1px solid var(--border-light); box-shadow: var(--shadow-light); overflow: hidden; }

/* ---- Level Badges ---- */
.level-badge {
  display: inline-flex; align-items: center; gap: 5px; padding: 4px 14px; border-radius: var(--radius-pill);
  font-size: 12px; font-weight: 600; white-space: nowrap;
}
.level-badge__icon { font-size: 14px; font-variation-settings: 'FILL' 1; }

.level-badge--diamond { background: rgba(132, 84, 0, 0.1); color: #845400; }
.level-badge--gold { background: rgba(200, 150, 12, 0.1); color: #B8860B; }
.level-badge--silver { background: var(--bg-secondary); color: var(--text-secondary); }
.level-badge--default { background: var(--bg-secondary); color: var(--text-tertiary); }

/* ---- Member Cell ---- */
.member-cell { display: flex; align-items: center; gap: 12px; }
.member-avatar { flex-shrink: 0; }
.member-info { display: flex; flex-direction: column; }
.member-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.member-id { font-size: 11px; color: var(--text-tertiary); }

.balance-value { font-weight: 700; color: var(--color-primary); }

/* ---- Action Buttons ---- */
.action-btns { display: flex; gap: 4px; justify-content: center; }
.action-btn {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  border: none; border-radius: var(--radius-md); background: transparent; color: var(--text-secondary);
  cursor: pointer; transition: all 0.15s ease;
}
.action-btn:hover { background: var(--bg-hover); color: var(--color-primary); }
.action-btn--danger:hover { color: var(--color-danger); }
.action-btn .material-symbols-outlined { font-size: 18px; }

/* ---- Table Footer ---- */
.table-footer { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; border-top: 1px solid var(--border-light); }
.table-footer__info { font-size: 12px; color: var(--text-tertiary); }

.pagination-bar { display: flex; align-items: center; gap: 4px; }
.page-arrow {
  width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  border: 1px solid var(--border-light); border-radius: 50%; background: transparent; color: var(--text-secondary);
  cursor: pointer; transition: all 0.15s ease;
}
.page-arrow:hover:not(:disabled) { background: var(--bg-hover); color: var(--color-primary); }
.page-arrow:disabled { opacity: 0.3; cursor: not-allowed; }
.page-num {
  width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  border: none; border-radius: 50%; background: transparent; font-size: 13px; font-weight: 500;
  color: var(--text-secondary); cursor: pointer; transition: all 0.15s ease; font-family: inherit;
}
.page-num:hover { background: var(--bg-hover); }
.page-num.active { background: var(--color-primary); color: #fff; font-weight: 700; box-shadow: var(--shadow-light); }
[data-theme='dark'] .page-num.active { color: #1A1814; }

/* ---- Stats Grid ---- */
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-top: 32px; }
.stat-card {
  background: var(--bg-card); border-radius: var(--radius-xl); border: 1px solid var(--border-light);
  box-shadow: var(--shadow-light); padding: 28px;
}
.stat-card--highlight { background: rgba(132, 84, 0, 0.04); border-color: rgba(132, 84, 0, 0.12); }
[data-theme='dark'] .stat-card--highlight { background: rgba(232, 168, 80, 0.04); border-color: rgba(232, 168, 80, 0.12); }

.stat-card__label { font-size: 12px; font-weight: 600; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 10px; }
.stat-card__row { display: flex; align-items: baseline; gap: 10px; margin-bottom: 6px; }
.stat-card__num { font-family: Georgia, 'Noto Serif SC', serif; font-size: 36px; font-weight: 700; color: var(--text-primary); }
.stat-card__num--accent { color: var(--color-primary); }
.stat-card__change { font-size: 13px; font-weight: 500; }
.stat-card__change.up { color: var(--color-emerald); }
.stat-card__note { font-size: 12px; color: var(--text-tertiary); }
.stat-bar-wrap { display: flex; gap: 4px; height: 6px; border-radius: 3px; overflow: hidden; margin-top: 14px; }

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .page-hero { flex-direction: column; align-items: flex-start; gap: 16px; }
  .page-hero__title { font-size: 26px; }
  .filter-section { flex-direction: column; }
  .stats-grid { grid-template-columns: 1fr; }
  .pagination-bar { gap: 2px; }
}
</style>
