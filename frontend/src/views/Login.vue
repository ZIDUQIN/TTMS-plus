<template>
  <div class="login-page">
    <!-- Background overlay -->
    <div class="login-bg"></div>

    <div class="login-container">
      <!-- Left: decorative -->
      <div class="login-left">
        <div class="brand-area">
          <div class="brand-icon">
            <el-icon :size="48"><VideoCameraFilled /></el-icon>
          </div>
          <h1 class="brand-name">TTMS</h1>
          <p class="brand-desc">Cinema Management System</p>
          <p class="brand-sub">电影院综合管理系统</p>
        </div>
        <div class="feature-list">
          <div class="feature-item">
            <el-icon><Film /></el-icon>
            <span>海量影片 随心选择</span>
          </div>
          <div class="feature-item">
            <el-icon><Select /></el-icon>
            <span>在线选座 便捷购票</span>
          </div>
          <div class="feature-item">
            <el-icon><Management /></el-icon>
            <span>智能管理 高效运营</span>
          </div>
        </div>
      </div>

      <!-- Right: login form -->
      <div class="login-right">
        <div class="login-form-wrapper">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">登录您的账户</p>

          <!-- Login type tabs -->
          <div class="login-tabs">
            <div
              class="tab-item"
              :class="{ active: loginType === 'USER' }"
              @click="loginType = 'USER'"
            >
              <el-icon><User /></el-icon>
              <span>用户登录</span>
            </div>
            <div
              class="tab-item"
              :class="{ active: loginType === 'ADMIN' }"
              @click="loginType = 'ADMIN'"
            >
              <el-icon><Avatar /></el-icon>
              <span>管理员登录</span>
            </div>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            @keyup.enter="handleLogin"
            size="large"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
                round
              >
                {{ loginType === 'USER' ? '登 录' : '管理员登录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div v-if="loginType === 'USER'" class="form-footer">
            还没有账号？
            <router-link to="/register" class="register-link">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import {
  VideoCameraFilled, User, Lock, Avatar, Film, Select, Management
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const loginType = ref('USER')

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 30, message: '用户名长度在2到30个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, max: 30, message: '密码长度在4到30个字符之间', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(
      { username: form.username, password: form.password },
      loginType.value
    )
    ElMessage.success('登录成功，欢迎回来！')

    // Redirect based on role
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else if (authStore.isAdmin) {
      router.push('/admin/dashboard')
    } else {
      router.push('/home')
    }
  } catch (err) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 70%, #1a1a2e 100%);
  z-index: 0;
}

.login-bg::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background:
    radial-gradient(circle at 30% 70%, rgba(233, 69, 96, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 70% 30%, rgba(64, 158, 255, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 50% 50%, rgba(212, 168, 83, 0.05) 0%, transparent 50%);
  animation: bgMove 20s ease-in-out infinite;
}

@keyframes bgMove {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(2%, -1%); }
  66% { transform: translate(-1%, 2%); }
}

.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  width: 1000px;
  min-height: 560px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.4);
  overflow: hidden;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, rgba(233, 69, 96, 0.15), rgba(64, 158, 255, 0.1));
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 40px;
}

.brand-area {
  margin-bottom: 48px;
}

.brand-icon {
  color: #e94560;
  margin-bottom: 16px;
}

.brand-name {
  font-size: 40px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 4px;
  margin-bottom: 8px;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 4px;
}

.brand-sub {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.feature-item .el-icon {
  color: #d4a853;
  font-size: 18px;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
}

.login-form-wrapper {
  width: 100%;
  max-width: 360px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.form-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 32px;
}

.login-tabs {
  display: flex;
  background: rgba(255, 255, 255, 0.06);
  border-radius: var(--radius-md);
  padding: 4px;
  margin-bottom: 32px;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: rgba(255, 255, 255, 0.65);
  font-size: 14px;
  transition: all 0.3s ease;
}

.tab-item.active {
  background: rgba(233, 69, 96, 0.2);
  color: #e94560;
  font-weight: 600;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
  margin-top: 8px;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.register-link {
  color: #e94560;
  font-weight: 600;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
}

/* Override Element Plus input styles for dark background */
:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  box-shadow: none !important;
}

:deep(.el-input__inner) {
  color: #fff !important;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    width: 90%;
    min-height: auto;
  }
  .login-left {
    display: none;
  }
}
</style>
