<template>
  <div class="profile-page">
    <div class="profile-container">
      <!-- Page Title -->
      <div class="page-header">
        <h1 class="page-title">
          <span class="material-symbols-outlined title-icon">account_circle</span>
          个人中心
        </h1>
        <router-link to="/home" class="back-link">
          <span class="material-symbols-outlined">arrow_back</span>
          <span>返回首页</span>
        </router-link>
      </div>

      <div class="profile-grid">
        <!-- ===== Left Column ===== -->
        <div class="left-column">
          <!-- Profile Card -->
          <div class="profile-card">
            <div class="profile-card__glow"></div>
            <div class="profile-card__avatar-wrap">
              <div class="profile-card__avatar-glow"></div>
              <el-avatar :size="96" :icon="UserFilled" class="profile-card__avatar" />
            </div>
            <h2 class="profile-card__name">{{ authStore.realName || authStore.username }}</h2>

            <div class="profile-card__badge" v-if="membership">
              <span class="material-symbols-outlined badge-star">stars</span>
              {{ membership.levelName || '普通用户' }}
            </div>
            <div class="profile-card__badge profile-card__badge--default" v-else>
              普通用户
            </div>

            <div class="profile-card__info">
              <div class="info-row">
                <span class="info-label">手机号码</span>
                <span class="info-value">{{ authStore.user?.phone || '--' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ authStore.user?.createTime ? formatDate(authStore.user.createTime) : '--' }}</span>
              </div>
            </div>

            <button class="profile-card__edit-btn" @click="scrollToSection('password-section')">
              <span class="material-symbols-outlined">edit</span>
              编辑个人资料
            </button>
          </div>

          <!-- Quick Stats -->
          <div class="stats-row">
            <div class="stat-card" @click="$router.push('/my-orders')">
              <span class="stat-num">{{ orderCount }}</span>
              <span class="stat-label">订单记录</span>
            </div>
            <div class="stat-card">
              <span class="stat-num">{{ membership?.points || 0 }}</span>
              <span class="stat-label">可用积分</span>
            </div>
          </div>
        </div>

        <!-- ===== Right Column ===== -->
        <div class="right-column">
          <!-- Membership Card -->
          <div class="section-card membership-card" v-loading="membershipLoading">
            <h3 class="section-title">
              <span class="material-symbols-outlined">card_membership</span>
              我的会员
            </h3>

            <template v-if="membership">
              <div class="membership-stats">
                <div class="m-stat">
                  <span class="m-stat__label">当前折扣</span>
                  <span class="m-stat__value m-stat__value--discount">
                    {{ ((1 - membership.discountRate) * 100).toFixed(0) }}% off
                  </span>
                </div>
                <div class="m-stat">
                  <span class="m-stat__label">积分加速</span>
                  <span class="m-stat__value">{{ membership.pointsRate || 1 }}x</span>
                </div>
                <div class="m-stat">
                  <span class="m-stat__label">储值余额</span>
                  <span class="m-stat__value m-stat__value--balance">¥{{ membership.balance || 0 }}</span>
                </div>
              </div>

              <!-- Level Progress -->
              <div class="level-progress" v-if="membership.nextLevelName && membership.nextLevelName !== '已是最高等级'">
                <div class="level-progress__header">
                  <span>距离 {{ membership.nextLevelName }}</span>
                  <span class="level-progress__points">{{ membership.pointsToNext }} 积分</span>
                </div>
                <div class="level-progress__bar">
                  <div class="level-progress__fill" :style="{ width: membership.progressPercent + '%' }"></div>
                </div>
                <span class="level-progress__tip">
                  再消费 ¥{{ membership.pointsToNext }} 即可升级为 {{ membership.nextLevelName }}
                </span>
              </div>
              <div class="level-progress" v-else>
                <div class="level-progress__header">🎉 已达最高会员等级</div>
                <div class="level-progress__bar">
                  <div class="level-progress__fill max-fill" style="width:100%"></div>
                </div>
              </div>
            </template>
            <el-empty v-else-if="!membershipLoading" description="加载失败" :image-size="60" />
          </div>

          <!-- Change Password -->
          <div class="section-card" id="password-section">
            <h3 class="section-title">
              <span class="material-symbols-outlined">lock</span>
              修改密码
            </h3>
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
              size="default"
              class="password-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- Theme Settings -->
          <div class="section-card">
            <h3 class="section-title">
              <span class="material-symbols-outlined">palette</span>
              主题设置
            </h3>
            <div class="theme-cards">
              <div class="theme-card" :class="{ active: themeStore.currentTheme === 'light' }"
                @click="themeStore.setTheme('light')">
                <div class="theme-preview light-preview">
                  <div class="tp-bar"></div>
                  <div class="tp-body">
                    <div class="tp-side"></div>
                    <div class="tp-main"></div>
                  </div>
                </div>
                <span>日间模式</span>
                <span v-if="themeStore.currentTheme === 'light'" class="material-symbols-outlined check-mark">check_circle</span>
              </div>
              <div class="theme-card" :class="{ active: themeStore.currentTheme === 'dark' }"
                @click="themeStore.setTheme('dark')">
                <div class="theme-preview dark-preview">
                  <div class="tp-bar"></div>
                  <div class="tp-body">
                    <div class="tp-side"></div>
                    <div class="tp-main"></div>
                  </div>
                </div>
                <span>夜间模式</span>
                <span v-if="themeStore.currentTheme === 'dark'" class="material-symbols-outlined check-mark">check_circle</span>
              </div>
            </div>
          </div>

          <!-- Logout -->
          <button class="logout-btn" @click="handleLogout">
            <span class="material-symbols-outlined">logout</span>
            退出登录
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { changePassword } from '@/api/auth'
import { getMyMembership } from '@/api/member'
import { getMyOrders } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const changingPwd = ref(false)
const membership = ref(null)
const membershipLoading = ref(true)
const orderCount = ref(0)
const passwordFormRef = ref(null)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPwd = (_rule, value, callback) => {
  callback(value !== passwordForm.newPassword ? new Error('两次输入的密码不一致') : undefined)
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在6到30个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

function formatDate(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

function scrollToSection(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  changingPwd.value = true
  try {
    await changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    authStore.logout()
    router.push('/login')
  } catch {} finally { changingPwd.value = false }
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/home')
  }).catch(() => {})
}

async function fetchMembership() {
  membershipLoading.value = true
  try { const r = await getMyMembership(); membership.value = r.data }
  catch { membership.value = null }
  finally { membershipLoading.value = false }
}

async function fetchOrderCount() {
  try {
    const res = await getMyOrders()
    const list = res.data?.records || res.data || []
    orderCount.value = Array.isArray(list) ? list.length : 0
  } catch { orderCount.value = 0 }
}

onMounted(() => { fetchMembership(); fetchOrderCount() })
</script>

<style scoped>
/* ============================================================
   Profile — Cinema Edition
   ============================================================ */

.profile-page {
  min-height: 100vh;
  background: var(--bg-primary);
}

.profile-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* ---- Page Header ---- */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.page-title {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 32px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 28px;
  color: var(--color-primary);
  font-variation-settings: 'FILL' 1;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-light);
  transition: all 0.2s ease;
}

.back-link:hover { color: var(--color-primary); border-color: var(--color-primary); }
.back-link .material-symbols-outlined { font-size: 16px; }

/* ---- Grid ---- */
.profile-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 28px;
  align-items: start;
}

/* ============================================================
   LEFT COLUMN
   ============================================================ */

/* ---- Profile Card ---- */
.profile-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-light);
  padding: 32px 24px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.profile-card__glow {
  position: absolute;
  top: -48px;
  left: -48px;
  width: 128px;
  height: 128px;
  background: rgba(232, 168, 80, 0.06);
  border-radius: 50%;
  filter: blur(48px);
}

.profile-card__avatar-wrap {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.profile-card__avatar-glow {
  position: absolute;
  inset: -8px;
  background: rgba(232, 168, 80, 0.15);
  border-radius: 50%;
  filter: blur(16px);
}

.profile-card__avatar {
  position: relative;
  border: 3px solid var(--bg-card);
  box-shadow: var(--shadow-medium);
}

.profile-card__name {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.profile-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 16px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  background: rgba(232, 168, 80, 0.1);
  border: 1px solid rgba(232, 168, 80, 0.2);
  margin-bottom: 24px;
}

.profile-card__badge--default {
  color: var(--text-secondary);
  background: var(--bg-secondary);
  border-color: var(--border-light);
}

.badge-star {
  font-size: 14px;
  font-variation-settings: 'FILL' 1;
}

.profile-card__info {
  text-align: left;
  border-top: 1px solid var(--border-light);
  padding-top: 20px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 14px;
}

.info-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.profile-card__edit-btn {
  width: 100%;
  margin-top: 8px;
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.profile-card__edit-btn:hover {
  background: var(--bg-secondary);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.profile-card__edit-btn .material-symbols-outlined { font-size: 16px; }

/* ---- Stats Row ---- */
.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 20px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-light);
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.stat-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-1px);
}

.stat-num {
  display: block;
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 500;
}

/* ============================================================
   RIGHT COLUMN
   ============================================================ */

.section-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-light);
  padding: 24px;
  margin-bottom: 20px;
}

.section-title {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light);
}

.section-title .material-symbols-outlined {
  font-size: 20px;
  color: var(--color-primary);
}

/* ---- Membership ---- */
.membership-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.m-stat {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 14px;
  text-align: center;
}

.m-stat__label {
  display: block;
  font-size: 11px;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}

.m-stat__value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.m-stat__value--discount { color: var(--color-emerald); }
.m-stat__value--balance { color: var(--color-primary); }

/* Level Progress */
.level-progress { margin-top: 8px; }

.level-progress__header {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
  margin-bottom: 8px;
}

.level-progress__points {
  color: var(--color-primary);
  font-weight: 600;
}

.level-progress__bar {
  height: 8px;
  background: var(--bg-hover);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 6px;
}

.level-progress__fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary), #f0c070);
  border-radius: 4px;
  transition: width 0.6s ease;
}

.level-progress__fill.max-fill {
  background: linear-gradient(90deg, var(--color-emerald), #85ce61);
}

.level-progress__tip {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* ---- Password Form ---- */
.password-form {
  max-width: 420px;
}

/* ---- Theme Cards ---- */
.theme-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.theme-card {
  text-align: center;
  cursor: pointer;
  padding: 16px 12px;
  border-radius: var(--radius-lg);
  border: 2px solid var(--border-light);
  transition: all 0.2s ease;
  position: relative;
}

.theme-card.active { border-color: var(--color-primary); }

.theme-card span {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  display: block;
  margin-top: 10px;
}

.check-mark {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 20px;
  color: var(--color-primary);
  font-variation-settings: 'FILL' 1;
}

.theme-preview {
  height: 56px;
  border-radius: 6px;
  overflow: hidden;
}

.tp-bar { height: 6px; }
.tp-body { display: flex; height: 50px; }
.tp-side { width: 18px; }
.tp-main { flex: 1; }

.light-preview { background: #f5f7fa; }
.light-preview .tp-bar { background: #fff; }
.light-preview .tp-side { background: #e4e7ed; }
.light-preview .tp-main { background: #fff; }

.dark-preview { background: #16213e; }
.dark-preview .tp-bar { background: #1a1a2e; }
.dark-preview .tp-side { background: #0f0f23; }
.dark-preview .tp-main { background: #1e2a4a; }

/* ---- Logout ---- */
.logout-btn {
  width: 100%;
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid rgba(232, 64, 64, 0.25);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  color: var(--color-danger);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.logout-btn:hover {
  background: rgba(232, 64, 64, 0.06);
  border-color: var(--color-danger);
}

.logout-btn .material-symbols-outlined { font-size: 18px; }

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
  .membership-stats {
    grid-template-columns: 1fr;
  }
  .theme-cards {
    grid-template-columns: 1fr;
  }
  .page-title { font-size: 24px; }
}
</style>
