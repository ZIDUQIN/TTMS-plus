<template>
  <div class="login-page">
    <router-link to="/" class="login-back">
      <span class="material-symbols-outlined">arrow_back</span>
      <span>返回首页</span>
    </router-link>
    <div class="login-container">
      <!-- Left: Cinema atmosphere panel -->
      <div class="login-left">
        <!-- Projection beam effect -->
        <div class="projection-beam"></div>
        <div class="film-texture"></div>

        <div class="brand-content">
          <div class="brand-icon-wrap">
            <el-icon :size="44" color="var(--color-primary)"><VideoCameraFilled /></el-icon>
          </div>
          <h1 class="brand-name">TTMS</h1>
          <p class="brand-tagline">智能影院综合管理平台</p>
        </div>

        <div class="feature-list">
          <div class="feature-item">
            <el-icon :size="16" color="var(--color-primary)"><Film /></el-icon>
            <span>海量影片 随心选择</span>
          </div>
          <div class="feature-item">
            <el-icon :size="16" color="var(--color-primary)"><Tickets /></el-icon>
            <span>在线选座 便捷购票</span>
          </div>
          <div class="feature-item">
            <el-icon :size="16" color="var(--color-primary)"><TrendCharts /></el-icon>
            <span>智能管理 高效运营</span>
          </div>
        </div>
      </div>

      <!-- Right: Login form — glass card -->
      <div class="login-right">
        <div class="form-card glass-card">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">登录您的账户，系统将自动识别身份</p>

          <!-- Hidden dummy fields to absorb browser autofill -->
            <input type="text" style="position:fixed;top:-9999px;left:-9999px;width:0;height:0;opacity:0" tabindex="-1" autocomplete="off" aria-hidden="true">
            <input type="password" style="position:fixed;top:-9999px;left:-9999px;width:0;height:0;opacity:0" tabindex="-1" autocomplete="off" aria-hidden="true">

            <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            @keyup.enter="handleLogin"
            size="large"
            autocomplete="off"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="用户名"
                :prefix-icon="User"
                autocomplete="off"
                name="login-username"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                :prefix-icon="Lock"
                show-password
                autocomplete="new-password"
                name="login-password"
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, VideoCameraFilled, Film, Tickets, TrendCharts } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

// 清除浏览器自动填充的内容
onMounted(() => {
  setTimeout(() => {
    form.username = ''
    form.password = ''
  }, 100)
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
   Cinema Login — 影院仪式感
   ============================================================ */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg-primary);
  position: relative;
  overflow: hidden;
  flex-direction: column;
}

.login-back {
  position: fixed; top: 20px; left: 24px; z-index: 10;
  display: flex; align-items: center; gap: 6px;
  color: var(--text-secondary); font-size: 13px; font-weight: 500;
  padding: 8px 16px; border-radius: var(--radius-pill);
  border: 1px solid var(--border-light); background: var(--bg-card);
  transition: all 0.2s ease;
}
.login-back:hover { color: var(--color-primary); border-color: var(--color-primary); }
.login-back .material-symbols-outlined { font-size: 16px; }

/* Subtle atmospheric glow behind login */
.login-page::before {
  content: '';
  position: absolute;
  top: -20%;
  left: 50%;
  transform: translateX(-50%);
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(232, 168, 80, 0.04), transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.login-container {
  display: flex;
  width: 920px;
  min-height: 560px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-heavy);
  position: relative;
  z-index: 1;
}

/* ---- Left Panel — Cinema atmosphere ---- */
.login-left {
  flex: 1;
  background: #0F0F1A;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px 48px;
  position: relative;
  overflow: hidden;
}
[data-theme='light'] .login-left { background: #F5F0E8; }
[data-theme='light'] .brand-tagline { color: #6B5E4A; }
[data-theme='light'] .feature-item .material-symbols-outlined { color: rgba(132,84,0,0.4); }

/* Projection beam — the core cinema visual */
.projection-beam {
  position: absolute;
  top: -30%;
  left: 50%;
  transform: translateX(-50%) rotate(-15deg);
  width: 4px;
  height: 160%;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(232, 168, 80, 0.15) 20%,
    rgba(232, 168, 80, 0.08) 50%,
    rgba(232, 168, 80, 0.02) 80%,
    transparent 100%
  );
  filter: blur(4px);
  animation: beamFlicker 8s ease-in-out infinite;
}

@keyframes beamFlicker {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* Film grain texture overlay */
.film-texture {
  position: absolute;
  inset: 0;
  opacity: 0.03;
  background-image: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(255, 255, 255, 0.05) 2px,
    rgba(255, 255, 255, 0.05) 4px
  );
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
  margin-bottom: 48px;
}

.brand-icon-wrap {
  margin-bottom: 16px;
}

.brand-icon {
  font-size: 48px;
  font-variation-settings: 'FILL' 1;
  background: linear-gradient(135deg, #E8A850, #F0C070);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-name {
  font-family: 'Playfair Display', 'Noto Serif SC', serif;
  font-size: 42px;
  font-weight: 700;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #E8A850, #F0C070);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.brand-tagline {
  font-size: 14px;
  color: var(--text-tertiary);
  font-weight: 500;
  letter-spacing: 2px;
}

.feature-list {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.feature-item .material-symbols-outlined {
  font-size: 18px;
  color: rgba(232, 168, 80, 0.5);
}

/* ---- Right Panel — Glass card form ---- */
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
  max-width: 360px;
  padding: 40px;
  border-radius: var(--radius-xl);
}

.form-title {
  font-family: 'Playfair Display', 'Noto Serif SC', serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
  letter-spacing: -0.5px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 36px;
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
  margin-top: 4px;
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
  .form-card {
    padding: 24px;
  }
}
</style>
