import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(parseStoredUser())

function parseStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    return null
  }
}

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => {
    if (!user.value) return false
    const roleCode = user.value.roleCode
    return roleCode === 'ROLE_SUPER_ADMIN' || roleCode === 'ROLE_STAFF'
  })
  const isSuperAdmin = computed(() => {
    if (!user.value) return false
    return user.value.roleCode === 'ROLE_SUPER_ADMIN'
  })
  const username = computed(() => user.value?.username || '')
  const realName = computed(() => user.value?.realName || user.value?.nickname || user.value?.username || '')

  // Actions
  async function login(credentials, loginType) {
    const payload = {
      username: credentials.username,
      password: credentials.password,
      loginType: loginType
    }
    const res = await loginApi(payload)
    const data = res.data
    token.value = data.token
    user.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      roleCode: data.roleCode,
      roleName: data.roleName,
      permissions: data.permissions || [],
      theme: data.theme || 'white',
      nickname: data.nickname
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return data
  }

  async function register(data) {
    const res = await registerApi(data)
    return res
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function fetchUser() {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      user.value = JSON.parse(storedUser)
    }
    const storedToken = localStorage.getItem('token')
    if (storedToken) {
      token.value = storedToken
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    isSuperAdmin,
    username,
    realName,
    login,
    register,
    logout,
    fetchUser
  }
})
