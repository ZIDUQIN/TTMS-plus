<template>
  <header
    class="navbar"
    :class="{
      'is-admin': isAdminRoute,
      'is-scrolled': isScrolled
    }"
  >
    <div class="navbar-inner">
      <!-- Logo -->
      <div class="navbar-left">
        <router-link to="/home" class="logo">
          <el-icon :size="24" color="var(--color-primary)"><VideoCameraFilled /></el-icon>
          <span class="logo-text">TTMS</span>
        </router-link>

        <!-- Admin horizontal nav -->
        <template v-if="isAdminRoute">
          <nav class="admin-nav">
            <router-link
              v-for="item in adminNavItems"
              :key="item.path"
              :to="item.path"
              class="admin-nav-item"
              :class="{ active: activeMenu === item.path }"
            >
              <el-icon :size="16"><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </router-link>
          </nav>
        </template>

        <!-- User nav links -->
        <template v-else>
          <router-link to="/home" class="nav-link">首页</router-link>
        </template>
      </div>

      <!-- Right side -->
      <div class="navbar-right">
        <!-- Search (user only) -->
        <div v-if="!isAdminRoute" class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索电影..."
            :prefix-icon="Search"
            size="small"
            class="search-input"
            @keyup.enter="doSearch"
          />
        </div>

        <!-- Theme switcher -->
        <ThemeSwitcher />

        <!-- My Orders (user only) -->
        <router-link v-if="authStore.isLoggedIn && !isAdminRoute" to="/my-orders" class="nav-link">
          <el-icon :size="16"><Tickets /></el-icon>
          <span>我的订单</span>
        </router-link>

        <!-- User menu -->
        <template v-if="authStore.isLoggedIn">
          <el-dropdown trigger="click">
            <span class="user-menu-trigger">
              <el-avatar :size="28" :icon="UserFilled" />
              <span class="username">{{ authStore.realName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="!isAdminRoute" @click="$router.push('/profile')">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item v-if="authStore.isAdmin" @click="$router.push('/admin/dashboard')">
                  <el-icon><DataBoard /></el-icon>管理后台
                </el-dropdown-item>
                <el-dropdown-item v-if="isAdminRoute" @click="$router.push('/home')">
                  <el-icon><HomeFilled /></el-icon>返回前台
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <!-- Login/Register (not logged in) -->
        <template v-else>
          <router-link to="/login" class="nav-link">登录</router-link>
          <router-link to="/register">
            <el-button type="primary" size="small" round class="register-btn">注册</el-button>
          </router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import ThemeSwitcher from './ThemeSwitcher.vue'
import {
  VideoCameraFilled, Search, Tickets, UserFilled,
  User, DataBoard, HomeFilled, SwitchButton, Film, Grid,
  Calendar, TrendCharts, Avatar, Setting
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const searchKeyword = ref('')
const isScrolled = ref(false)

const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const activeMenu = computed(() => route.path)

const adminNavItems = computed(() => {
  const items = [
    { path: '/admin/dashboard', label: '仪表盘', icon: 'DataBoard' },
    { path: '/admin/box-office', label: '票房', icon: 'TrendCharts' },
    { path: '/admin/orders', label: '订单', icon: 'Tickets' }
  ]
  if (authStore.isSuperAdmin) {
    items.push(
      { path: '/admin/movies', label: '影片', icon: 'Film' },
      { path: '/admin/halls', label: '影厅', icon: 'Grid' },
      { path: '/admin/schedules', label: '排片', icon: 'Calendar' },
      { path: '/admin/statistics', label: '统计', icon: 'TrendCharts' },
      { path: '/admin/employees', label: '员工', icon: 'Avatar' },
      { path: '/admin/settings', label: '设置', icon: 'Setting' }
    )
  }
  return items
})

function doSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/home', query: { keyword: searchKeyword.value.trim() } })
    searchKeyword.value = ''
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

function onScroll() {
  isScrolled.value = window.scrollY > 0
}

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
/* ============================================================
   NavBar — Apple Liquid Glass
   ============================================================ */
.navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  width: 100%;
  height: var(--header-height);
  /* Core glass effect */
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  /* Bottom border — hidden until scrolled */
  border-bottom: 1px solid transparent;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

/* Scrolled state: subtle divider appears */
.navbar.is-scrolled {
  border-bottom-color: var(--border-alpha);
}

/* Admin variant: darker glass */
.navbar.is-admin {
  background: var(--bg-card);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
}

[data-theme='dark'] .navbar.is-admin {
  background: rgba(29, 29, 31, 0.90);
}

.navbar-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  gap: 24px;
}

/* ---- Left Section ---- */
.navbar-left {
  display: flex;
  align-items: center;
  gap: 32px;
  flex: 1;
  min-width: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

/* ---- Admin Nav (replaces el-menu) ---- */
.admin-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
}

.admin-nav::-webkit-scrollbar {
  display: none;
}

.admin-nav-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  letter-spacing: -0.01em;
  white-space: nowrap;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.admin-nav-item:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.admin-nav-item.active {
  color: var(--color-primary);
  background: rgba(0, 122, 255, 0.08);
}

[data-theme='dark'] .admin-nav-item.active {
  background: rgba(10, 132, 255, 0.15);
}

/* ---- Right Section ---- */
.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.search-box {
  width: 200px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-pill);
  background: var(--bg-hover);
  border: none;
  box-shadow: none !important;
  transition: background 0.2s ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  background: var(--border-light);
}

.search-input :deep(.el-input__inner) {
  font-size: 13px;
}

/* ---- Nav Links ---- */
.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  padding: 6px 10px;
  border-radius: var(--radius-md);
  letter-spacing: -0.01em;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.nav-link:hover {
  color: var(--color-primary);
  background: var(--bg-hover);
}

/* ---- User Menu Trigger ---- */
.user-menu-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 2px 10px 2px 4px;
  border-radius: var(--radius-pill);
  transition: background 0.2s ease;
}

.user-menu-trigger:hover {
  background: var(--bg-hover);
}

.username {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- Register Button ---- */
.register-btn {
  font-weight: 500;
  letter-spacing: -0.01em;
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .navbar-inner {
    padding: 0 16px;
    gap: 12px;
  }
  .navbar-left {
    gap: 16px;
  }
  .search-box {
    display: none;
  }
  .admin-nav {
    gap: 0;
  }
  .admin-nav-item {
    padding: 6px 8px;
    font-size: 12px;
  }
}
</style>
