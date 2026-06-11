<template>
  <div class="register-page">
    <!-- Ambient Background -->
    <div class="ambient-bg" aria-hidden="true">
      <div class="ambient-orb ambient-orb--gold"></div>
      <div class="ambient-orb ambient-orb--blue"></div>
    </div>

    <!-- Main Card: Split Screen -->
    <main class="register-panel">
      <!-- Left: Cinematic Visual -->
      <div class="register-visual">
        <img
          class="register-visual__img"
          src="https://lh3.googleusercontent.com/aida-public/AB6AXuBsSCW5MrVQttud6qA3-DloqpCVAvw5wd6NmQkTYOg-YDBxBgq_L-oy2v7-Bywyp2NKUDwIqk2ZDz4-ZczEx-cMDIOH2tCTNIPg51ospu6YsuhIWSLvhd7r3NnikREk90hmjCludGuRyOEEn4YtyiRTO_Yeea_q-BwR8mnPNv7Yi6E5RNJbRYJD4US6VviHGsayt2WAD4FTohvScWF_VSrR-k9_0izVtCe-UtCg9jazMN_DIFHz8yRcsYKrGp__levTFNsXP1IQh3qy"
          alt="Cinema interior"
        />
        <div class="register-visual__overlay"></div>
        <div class="register-visual__content">
          <div class="register-visual__brand">
            <span class="material-symbols-outlined register-visual__icon">movie</span>
            <h1 class="register-visual__title">TTMS Cinema</h1>
          </div>
          <h2 class="register-visual__hero">每一帧都是<br/>生活的艺术</h2>
          <p class="register-visual__desc">开启您的影院级管理体验，让每一场放映都如大幕拉开般璀璨夺目。</p>
        </div>
      </div>

      <!-- Right: Registration Form -->
      <div class="register-form-wrap">
        <div class="register-form__header">
          <h3 class="register-form__title">创建新账号</h3>
          <p class="register-form__subtitle">加入 TTMS 影院管理系统，开启高效工作流。</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          class="register-form"
          @keyup.enter="handleRegister"
          autocomplete="off"
        >
          <!-- Row 1: Username + Nickname -->
          <div class="form-row">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                placeholder="设置登录ID"
                :prefix-icon="User"
                maxlength="30"
                autocomplete="off"
              />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input
                v-model="form.nickname"
                placeholder="显示名称"
                :prefix-icon="EditPen"
                maxlength="20"
                autocomplete="off"
              />
            </el-form-item>
          </div>

          <!-- Row 2: Phone + Email -->
          <div class="form-row">
            <el-form-item label="手机号" prop="phone">
              <el-input
                v-model="form.phone"
                placeholder="138 **** ****"
                :prefix-icon="Phone"
                maxlength="11"
                autocomplete="off"
              />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model="form.email"
                placeholder="example@ttms.com"
                :prefix-icon="Message"
                autocomplete="off"
              />
            </el-form-item>
          </div>

          <!-- Password with Strength Indicator -->
          <el-form-item label="密码" prop="password">
            <div class="password-label-row">
              <span>密码</span>
              <span class="strength-text" :class="strengthClass">{{ strengthLabel }}</span>
            </div>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入复杂密码"
              :prefix-icon="Lock"
              show-password
              autocomplete="new-password"
              @input="checkPasswordStrength"
            />
            <!-- Strength Bars -->
            <div class="strength-bars">
              <div class="strength-bar" :class="barClass(1)"></div>
              <div class="strength-bar" :class="barClass(2)"></div>
              <div class="strength-bar" :class="barClass(3)"></div>
            </div>
          </el-form-item>

          <!-- Confirm Password -->
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="再次输入密码"
              :prefix-icon="Lock"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>

          <!-- Terms Checkbox -->
          <label class="terms-checkbox">
            <div class="terms-checkbox__input">
              <input v-model="agreedToTerms" type="checkbox" />
              <span class="terms-checkbox__mark">
                <span class="material-symbols-outlined">check</span>
              </span>
            </div>
            <span class="terms-checkbox__text">
              我已阅读并同意 <a href="#" @click.prevent>服务条款</a> 与 <a href="#" @click.prevent>隐私政策</a>
            </span>
          </label>

          <!-- Submit Button -->
          <el-form-item>
            <button
              type="button"
              class="golden-btn"
              :disabled="loading"
              @click="handleRegister"
            >
              <span v-if="!loading">创建您的账户</span>
              <span v-else>注册中...</span>
              <span class="material-symbols-outlined golden-btn__arrow">arrow_forward</span>
            </button>
          </el-form-item>
        </el-form>

        <!-- Footer -->
        <div class="register-form__footer">
          <span>已经有账号了？</span>
          <router-link to="/login" class="register-form__login-link">立即登录</router-link>
        </div>

        <router-link to="/" class="register-form__back-link">
          <span class="material-symbols-outlined">arrow_back</span>
          <span>返回首页</span>
        </router-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone, Message, EditPen } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const agreedToTerms = ref(false)

// ---- Password Strength ----
const passwordStrength = ref(0) // 0=none, 1=weak, 2=medium, 3=strong

function checkPasswordStrength() {
  const val = form.password
  if (!val || val.length === 0) { passwordStrength.value = 0; return }
  if (val.length < 6) { passwordStrength.value = 1; return }

  let score = val.length < 10 ? 2 : 3
  // Bonus for special chars
  if (/[!@#$%^&*(),.?":{}|<>]/.test(val) && score < 3) score++
  passwordStrength.value = Math.min(score, 3)
}

const strengthLabel = computed(() => {
  const map = { 0: '未输入', 1: '强度: 弱', 2: '强度: 中', 3: '强度: 强' }
  return map[passwordStrength.value] || '未输入'
})

const strengthClass = computed(() => {
  const map = { 0: '', 1: 'strength-text--weak', 2: 'strength-text--medium', 3: 'strength-text--strong' }
  return map[passwordStrength.value] || ''
})

function barClass(level) {
  const active = passwordStrength.value >= level
  if (!active) return ''
  const map = { 1: 'strength-bar--weak', 2: 'strength-bar--medium', 3: 'strength-bar--strong' }
  return map[passwordStrength.value] || ''
}

// ---- Form Data ----
const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: ''
})

// ---- Validation ----
const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validatePhone = (_rule, value, callback) => {
  if (!value) { callback(new Error('请输入手机号')) }
  else if (!/^1[3-9]\d{9}$/.test(value)) { callback(new Error('请输入正确的手机号')) }
  else { callback() }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '用户名长度在3到30个字符之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  nickname: [
    { max: 20, message: '昵称不能超过20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在6到30个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// ---- Submit ----
async function handleRegister() {
  if (!agreedToTerms.value) {
    ElMessage.warning('请先阅读并同意服务条款与隐私政策')
    return
  }
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const payload = {
      username: form.username,
      password: form.password,
      phone: form.phone,
      email: form.email,
      nickname: form.nickname || form.username
    }
    await authStore.register(payload)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ============================================================
   Register Page — Dark Cinema Gold Edition
   ============================================================ */

/* ---- Layout ---- */
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg-primary);
  position: relative;
  overflow-x: hidden;
}

/* ---- Ambient Background Orbs ---- */
.ambient-bg {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.ambient-orb {
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  filter: blur(120px);
}

.ambient-orb--gold {
  top: -25%;
  right: -25%;
  background: rgba(232, 168, 80, 0.05);
}

.ambient-orb--blue {
  bottom: -25%;
  left: -25%;
  background: rgba(43, 58, 94, 0.10);
}

/* ---- Main Panel (Glass) ---- */
.register-panel {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 1200px;
  min-height: 700px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-heavy);
}

/* Subtle glass effect in dark mode */
[data-theme='dark'] .register-panel {
  background: rgba(20, 20, 31, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

/* ---- Left: Cinematic Visual ---- */
.register-visual {
  display: none;
  position: relative;
  min-height: 100%;
}

@media (min-width: 769px) {
  .register-visual {
    display: block;
  }
}

.register-visual__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.register-visual__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    var(--bg-primary) 0%,
    rgba(10, 10, 16, 0.4) 30%,
    transparent 100%
  );
}

.register-visual__content {
  position: absolute;
  bottom: 48px;
  left: 48px;
  right: 48px;
  z-index: 2;
}

.register-visual__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.register-visual__icon {
  font-size: 36px;
  color: var(--color-primary);
  font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}

.register-visual__title {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: -0.5px;
}

.register-visual__hero {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 48px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.5px;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.register-visual__desc {
  color: var(--text-secondary);
  max-width: 320px;
  font-size: 15px;
  line-height: 1.6;
}

/* ---- Right: Form Container ---- */
.register-form-wrap {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 56px;
  background: var(--bg-card);
  position: relative;
}

[data-theme='dark'] .register-form-wrap {
  background: rgba(20, 20, 31, 0.85);
}

/* ---- Form Header ---- */
.register-form__header {
  margin-bottom: 32px;
}

.register-form__title {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
  letter-spacing: -0.5px;
}

.register-form__subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

/* ---- Form ---- */
.register-form {
  width: 100%;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}

/* ---- Password Strength ---- */
.password-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.strength-text {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  color: var(--text-tertiary);
  transition: color 0.3s ease;
}

.strength-text--weak   { color: #ff8a80; }
.strength-text--medium { color: var(--color-primary); }
.strength-text--strong { color: var(--color-emerald); }

/* Strength Bars */
.strength-bars {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.4s ease;
}

[data-theme='light'] .strength-bar {
  background: rgba(0, 0, 0, 0.06);
}

.strength-bar--weak   { background: #ff8a80; }
.strength-bar--medium { background: var(--color-primary); }
.strength-bar--strong { background: var(--color-emerald); }

/* ---- Terms Checkbox ---- */
.terms-checkbox {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
  margin-top: 4px;
  margin-bottom: 8px;
  user-select: none;
}

.terms-checkbox__input {
  position: relative;
  flex-shrink: 0;
  margin-top: 2px;
}

.terms-checkbox__input input[type="checkbox"] {
  appearance: none;
  -webkit-appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.20);
  background: var(--bg-input);
  cursor: pointer;
  transition: all 0.15s ease;
}

[data-theme='light'] .terms-checkbox__input input[type="checkbox"] {
  border: 1px solid rgba(0, 0, 0, 0.20);
}

.terms-checkbox__input input[type="checkbox"]:checked {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.terms-checkbox__mark {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.terms-checkbox__input input[type="checkbox"]:checked ~ .terms-checkbox__mark {
  opacity: 1;
}

.terms-checkbox__mark .material-symbols-outlined {
  font-size: 12px;
  color: #2a1800;
  font-variation-settings: 'FILL' 1, 'wght' 700, 'GRAD' 0, 'opsz' 24;
}

.terms-checkbox__text {
  font-size: 13px;
  color: var(--text-secondary);
  transition: color 0.2s ease;
  line-height: 1.5;
}

.terms-checkbox:hover .terms-checkbox__text {
  color: var(--text-primary);
}

.terms-checkbox__text a {
  color: var(--color-primary);
  font-weight: 500;
  transition: opacity 0.2s ease;
}

.terms-checkbox__text a:hover {
  opacity: 0.8;
  text-decoration: underline;
}

/* ---- Golden Button ---- */
.golden-btn {
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 15px;
  font-weight: 700;
  color: #2a1800;
  cursor: pointer;
  background: linear-gradient(135deg, #e8a850 0%, #ffc67c 50%, #e8a850 100%);
  transition: all 0.3s ease;
  margin-top: 4px;
}

.golden-btn:hover:not(:disabled) {
  filter: brightness(1.1);
  box-shadow: 0 0 20px rgba(232, 168, 80, 0.4);
}

.golden-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.golden-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.golden-btn__arrow {
  font-size: 18px;
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  transition: transform 0.2s ease;
}

.golden-btn:hover:not(:disabled) .golden-btn__arrow {
  transform: translateX(3px);
}

/* ---- Footer Links ---- */
.register-form__footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: var(--text-secondary);
}

.register-form__login-link {
  color: var(--color-primary);
  font-weight: 700;
  margin-left: 6px;
  transition: opacity 0.2s ease;
}

.register-form__login-link:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.register-form__back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 20px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  transition: color 0.2s ease;
  align-self: center;
}

.register-form__back-link:hover {
  color: var(--color-primary);
}

.register-form__back-link .material-symbols-outlined {
  font-size: 16px;
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .register-panel {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .register-form-wrap {
    padding: 32px 24px;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .register-form__title {
    font-size: 24px;
  }
}
</style>
