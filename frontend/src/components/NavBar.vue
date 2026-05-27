<template>
  <header class="navbar" :class="{ 'is-admin': isAdminRoute }">
    <div class="navbar-inner">
      <!-- Logo -->
      <div class="navbar-left">
        <router-link to="/home" class="logo">
          <el-icon :size="24" color="var(--color-primary)"><VideoCameraFilled /></el-icon>
          <span class="logo-text">TTMS</span>
        </router-link>

        <!-- Admin breadcrumb nav -->
        <template v-if="isAdminRoute">
          <el-menu
            :default-active="activeMenu"
            mode="horizontal"
            :ellipsis="false"
            class="admin-nav-menu"
            router
          >
            <el-menu-item index="/admin/dashboard">
              <el-icon><DataBoard /></el-icon>
              <span>仪表盘</span>
            </el-menu-item>
            <el-menu-item index="/admin/movies" v-if="authStore.isSuperAdmin">
              <el-icon><Film /></el-icon>
              <span>影片管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/halls" v-if="authStore.isSuperAdmin">
              <el-icon><Grid /></el-icon>
              <span>影厅管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/schedules" v-if="authStore.isSuperAdmin">
              <el-icon><Calendar /></el-icon>
              <span>排片管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/orders">
              <el-icon><Tickets /></el-icon>
              <span>订单管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/statistics" v-if="authStore.isSuperAdmin">
              <el-icon><TrendCharts /></el-icon>
              <span>数据统计</span>
            </el-menu-item>
            <el-menu-item index="/admin/employees" v-if="authStore.isSuperAdmin">
              <el-icon><Avatar /></el-icon>
              <span>员工管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/settings" v-if="authStore.isSuperAdmin">
              <el-icon><Setting /></el-icon>
              <span>系统设置</span>
            </el-menu-item>
          </el-menu>
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
          <el-icon><Tickets /></el-icon>
          <span>我的订单</span>
        </router-link>

        <!-- User menu -->
        <template v-if="authStore.isLoggedIn">
          <el-dropdown trigger="click">
            <span class="user-menu-trigger">
              <el-avatar :size="32" :icon="UserFilled" />
              <span class="username">{{ authStore.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
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
            <el-button type="primary" size="small" round>注册</el-button>
          </router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import ThemeSwitcher from './ThemeSwitcher.vue'
import {
  VideoCameraFilled, Search, Tickets, UserFilled, ArrowDown,
  User, DataBoard, HomeFilled, SwitchButton, Film, Grid,
  Calendar, TrendCharts, Avatar, Setting
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const searchKeyword = ref('')

const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const activeMenu = computed(() => route.path)

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
</script>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  height: var(--header-height);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  box-shadow: var(--shadow-light);
  backdrop-filter: blur(10px);
}

.navbar.is-admin {
  background: var(--bg-sidebar);
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

/* Admin navbar override: light text on dark sidebar background */
.navbar.is-admin .logo-text,
.navbar.is-admin .username,
.navbar.is-admin .nav-link {
  color: rgba(255, 255, 255, 0.85);
}

.navbar.is-admin .nav-link:hover,
.navbar.is-admin .user-menu-trigger:hover {
  color: var(--color-primary);
  background: rgba(255, 255, 255, 0.08);
}

.navbar-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
  min-width: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: 1px;
}

.admin-nav-menu {
  background: transparent !important;
  border-bottom: none !important;
  flex: 1;
}

.admin-nav-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.75) !important;
  border-bottom-color: transparent !important;
  transition: color 0.2s, background 0.2s;
}

.admin-nav-menu .el-menu-item:hover {
  color: rgba(255, 255, 255, 0.95) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

.admin-nav-menu .el-menu-item.is-active {
  color: var(--color-primary) !important;
  background: rgba(255, 255, 255, 0.06) !important;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.search-box {
  width: 220px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--text-secondary);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: color 0.2s;
}

.nav-link:hover {
  color: var(--color-primary);
}

.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 2px 8px 2px 4px;
  border-radius: var(--radius-md);
  transition: background 0.2s;
}

.user-menu-trigger:hover {
  background: var(--bg-hover);
}

.username {
  font-size: 13px;
  color: var(--text-secondary);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
