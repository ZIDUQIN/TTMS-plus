<template>
  <div class="register-page">
    <div class="register-bg"></div>

    <div class="register-card">
      <div class="card-header">
        <router-link to="/home" class="back-home">
          <el-icon><ArrowLeft /></el-icon>
          返回首页
        </router-link>
        <h2>创建账号</h2>
        <p>加入TTMS，开启您的观影之旅</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @keyup.enter="handleRegister"
      >
        <div class="form-row">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              maxlength="30"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="请输入昵称（选填）"
              :prefix-icon="EditPen"
              maxlength="20"
            />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              :prefix-icon="Phone"
              maxlength="11"
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱（选填）"
              :prefix-icon="Message"
            />
          </el-form-item>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            class="register-btn"
            :loading="loading"
            @click="handleRegister"
            round
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="form-footer">
        已有账号？
        <router-link to="/login" class="login-link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, User, Lock, Phone, Message, EditPen
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validatePhone = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
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

async function handleRegister() {
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
  } catch (err) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.register-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 70%, #1a1a2e 100%);
  z-index: 0;
}

.register-bg::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background:
    radial-gradient(circle at 20% 80%, rgba(233, 69, 96, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(64, 158, 255, 0.08) 0%, transparent 50%);
}

.register-card {
  position: relative;
  z-index: 1;
  width: 680px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.4);
  padding: 40px 48px;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.back-home {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  margin-bottom: 16px;
  transition: color 0.2s;
}

.back-home:hover {
  color: #e94560;
}

.card-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.card-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.form-row {
  display: flex;
  gap: 20px;
}

.form-row .el-form-item {
  flex: 1;
}

.register-btn {
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

.login-link {
  color: #e94560;
  font-weight: 600;
  margin-left: 4px;
}

.login-link:hover {
  text-decoration: underline;
}

/* Override form label color */
:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.7) !important;
}

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
  .register-card {
    width: 90%;
    padding: 24px;
  }
  .form-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
