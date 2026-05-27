<template>
  <div class="profile-page">
    <NavBar />

    <div class="profile-container">
      <h2 class="page-title">个人中心</h2>

      <div class="profile-grid">
        <!-- User info card -->
        <div class="info-card">
          <div class="card-header">
            <h3>账户信息</h3>
          </div>
          <div class="user-avatar-section">
            <el-avatar :size="80" :icon="UserFilled" />
            <div class="user-text">
              <h4>{{ authStore.realName || authStore.username }}</h4>
              <p>{{ authStore.user?.roleName || '普通用户' }}</p>
            </div>
          </div>
          <el-descriptions :column="1" border size="default">
            <el-descriptions-item label="用户名">{{ authStore.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ authStore.user?.nickname || '--' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ authStore.user?.phone || '--' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ authStore.user?.email || '--' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ authStore.user?.roleName || '--' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Right column -->
        <div class="right-column">
          <!-- Change password -->
          <div class="card">
            <div class="card-header">
              <h3>修改密码</h3>
            </div>
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
              size="default"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入原密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- Theme switcher -->
          <div class="card">
            <div class="card-header">
              <h3>主题设置</h3>
            </div>
            <div class="theme-cards">
              <div
                class="theme-card"
                :class="{ active: themeStore.currentTheme === 'white' }"
                @click="themeStore.setTheme('white')"
              >
                <div class="theme-preview white-preview">
                  <div class="preview-bar"></div>
                  <div class="preview-content">
                    <div class="preview-sidebar"></div>
                    <div class="preview-main"></div>
                  </div>
                </div>
                <span>白色商务</span>
                <el-icon v-if="themeStore.currentTheme === 'white'" class="check-icon"><CircleCheckFilled /></el-icon>
              </div>
              <div
                class="theme-card"
                :class="{ active: themeStore.currentTheme === 'dark' }"
                @click="themeStore.setTheme('dark')"
              >
                <div class="theme-preview dark-preview">
                  <div class="preview-bar"></div>
                  <div class="preview-content">
                    <div class="preview-sidebar"></div>
                    <div class="preview-main"></div>
                  </div>
                </div>
                <span>暗夜影院</span>
                <el-icon v-if="themeStore.currentTheme === 'dark'" class="check-icon"><CircleCheckFilled /></el-icon>
              </div>
              <div
                class="theme-card"
                :class="{ active: themeStore.currentTheme === 'purple' }"
                @click="themeStore.setTheme('purple')"
              >
                <div class="theme-preview purple-preview">
                  <div class="preview-bar"></div>
                  <div class="preview-content">
                    <div class="preview-sidebar"></div>
                    <div class="preview-main"></div>
                  </div>
                </div>
                <span>紫色幻影</span>
                <el-icon v-if="themeStore.currentTheme === 'purple'" class="check-icon"><CircleCheckFilled /></el-icon>
              </div>
            </div>
          </div>

          <!-- Logout button -->
          <el-button type="danger" class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { changePassword } from '@/api/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, CircleCheckFilled, SwitchButton } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const changingPwd = ref(false)
const passwordFormRef = ref(null)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validteConfirmPwd = (_rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在6到30个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validteConfirmPwd, trigger: 'blur' }
  ]
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  changingPwd.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    authStore.logout()
    router.push('/login')
  } catch (err) { /* handled */ }
  finally {
    changingPwd.value = false
  }
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/home')
  }).catch(() => {})
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: var(--bg-secondary);
}

.profile-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 24px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}

.info-card, .card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-light);
  overflow: hidden;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.user-text h4 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.user-text p {
  font-size: 13px;
  color: var(--text-muted);
}

.card {
  margin-bottom: 20px;
}

.card .el-form {
  padding: 20px;
}

/* Theme cards */
.theme-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 16px;
}

.theme-card {
  text-align: center;
  cursor: pointer;
  padding: 12px;
  border-radius: var(--radius-md);
  border: 2px solid var(--border-color);
  transition: all 0.2s;
  position: relative;
}

.theme-card.active {
  border-color: var(--color-primary);
}

.theme-card span {
  font-size: 13px;
  color: var(--text-secondary);
  display: block;
  margin-top: 8px;
}

.check-icon {
  position: absolute;
  top: 6px;
  right: 6px;
  color: var(--color-primary);
  font-size: 16px;
}

.theme-preview {
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
}

.preview-bar {
  height: 6px;
}

.preview-content {
  display: flex;
  height: 54px;
}

.preview-sidebar {
  width: 20px;
}

.preview-main {
  flex: 1;
}

/* White theme preview */
.white-preview { background: #f5f7fa; }
.white-preview .preview-bar { background: #fff; }
.white-preview .preview-sidebar { background: #e4e7ed; }
.white-preview .preview-main { background: #fff; }

/* Dark theme preview */
.dark-preview { background: #16213e; }
.dark-preview .preview-bar { background: #1a1a2e; }
.dark-preview .preview-sidebar { background: #0f0f23; }
.dark-preview .preview-main { background: #1e2a4a; }

/* Purple theme preview */
.purple-preview { background: #1f1147; }
.purple-preview .preview-bar { background: #2d1b69; }
.purple-preview .preview-sidebar { background: #190e3d; }
.purple-preview .preview-main { background: #3a2588; }

.logout-btn {
  width: 100%;
  height: 44px;
}

.right-column {
  display: flex;
  flex-direction: column;
}

@media (max-width: 768px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
  .theme-cards {
    grid-template-columns: 1fr;
  }
}
</style>
