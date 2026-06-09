<template>
  <div class="login-page">
    <div class="login-container">
      <!-- Left: brand area -->
      <div class="login-left">
        <div class="brand-area">
          <div class="brand-icon">
            <el-icon :size="44"><VideoCameraFilled /></el-icon>
          </div>
          <h1 class="brand-name">TTMS</h1>
          <p class="brand-desc">电影院综合管理系统</p>
        </div>
        <div class="feature-list">
          <div class="feature-item">
            <el-icon :size="18"><Film /></el-icon>
            <span>海量影片 随心选择</span>
          </div>
          <div class="feature-item">
            <el-icon :size="18"><Select /></el-icon>
            <span>在线选座 便捷购票</span>
          </div>
          <div class="feature-item">
            <el-icon :size="18"><Management /></el-icon>
            <span>智能管理 高效运营</span>
          </div>
        </div>
      </div>

      <!-- Right: login form -->
      <div class="login-right">
        <div class="form-card">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">登录您的账户，系统将自动识别身份</p>

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
                placeholder="用户名"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
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
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
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
  VideoCameraFilled, User, Lock, Film, Select, Management
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

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
    { min: 6, max: 30, message: '密码长度在6到30个字符之间', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功，欢迎回来！')

    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else if (authStore.isAdmin) {
      router.push('/admin/dashboard')
    } else {
      router.push('/home')
    }
  } catch (err) {
    // handled by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ============================================================
   Login Page — Apple aesthetic, dual theme
   ============================================================ */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg-primary);
  transition: background 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.login-container {
  display: flex;
  width: 920px;
  min-height: 540px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-heavy);
  background: var(--bg-card);
  border: 1px solid var(--border-light);
}

/* ---- Left Panel ---- */
.login-left {
  flex: 1;
  background: var(--bg-secondary);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px 48px;
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -20%;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: var(--color-primary);
  opacity: 0.04;
  pointer-events: none;
}

.brand-area {
  margin-bottom: 48px;
  position: relative;
}

.brand-icon {
  color: var(--color-primary);
  margin-bottom: 20px;
  opacity: 0.85;
}

.brand-name {
  font-size: 36px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 3px;
  margin-bottom: 6px;
}

.brand-desc {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 500;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: relative;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.feature-item .el-icon {
  color: var(--color-primary);
  opacity: 0.6;
}

/* ---- Right Panel ---- */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 48px;
  background: var(--bg-card);
}

.form-card {
  width: 100%;
  max-width: 340px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
  letter-spacing: -0.022em;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 40px;
  line-height: 1.5;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
  border-radius: var(--radius-md);
  margin-top: 4px;
}

.form-footer {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 8px;
}

.register-link {
  color: var(--color-primary);
  font-weight: 600;
  margin-left: 4px;
  transition: opacity 0.2s ease;
}

.register-link:hover {
  opacity: 0.8;
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    width: 100%;
    min-height: auto;
  }
  .login-left {
    display: none;
  }
  .login-right {
    padding: 40px 28px;
  }
}
</style>
