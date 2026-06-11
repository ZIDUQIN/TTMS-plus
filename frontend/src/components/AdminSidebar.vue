<template>
  <aside class="admin-sidebar">
    <div class="sidebar-brand">
      <router-link to="/admin/dashboard" class="brand-link">
        <h1 class="brand-title">TTMS</h1>
        <p class="brand-subtitle">系统管理中心</p>
      </router-link>
    </div>

    <nav class="sidebar-nav">
      <router-link v-for="item in navItems" :key="item.path" :to="item.path"
        class="nav-item" :class="{ active: isActive(item.path) }">
        <el-icon :size="18"><component :is="item.icon" /></el-icon>
        <span class="nav-label">{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="sidebar-divider"></div>

    <div class="sidebar-footer">
      <div class="user-profile-card">
        <el-icon :size="28" color="var(--color-primary)"><UserFilled /></el-icon>
        <div class="user-info">
          <p class="user-name">{{ authStore.realName || authStore.username || '管理员' }}</p>
          <p class="user-role">{{ roleLabel }}</p>
        </div>
      </div>
      <router-link to="/admin/settings" class="nav-item settings-link">
        <el-icon :size="18"><Setting /></el-icon>
        <span class="nav-label">系统设置</span>
      </router-link>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  DataBoard, Sell, Film, Calendar, Grid, Tickets, TrendCharts,
  DataAnalysis, UserFilled, Avatar, Discount, ShoppingBag, Clock, Document, Setting
} from '@element-plus/icons-vue'

const route = useRoute()
const authStore = useAuthStore()

const navItems = [
  { path: '/admin/dashboard', icon: DataBoard, label: '仪表盘' },
  { path: '/admin/pos', icon: Sell, label: '柜台售票' },
  { path: '/admin/movies', icon: Film, label: '影片管理' },
  { path: '/admin/schedules', icon: Calendar, label: '排片管理' },
  { path: '/admin/halls', icon: Grid, label: '影厅管理' },
  { path: '/admin/orders', icon: Tickets, label: '订单管理' },
  { path: '/admin/box-office', icon: TrendCharts, label: '电影票房' },
  { path: '/admin/statistics', icon: DataAnalysis, label: '数据统计' },
  { path: '/admin/employees', icon: Avatar, label: '员工管理' },
  { path: '/admin/members', icon: UserFilled, label: '会员管理' },
  { path: '/admin/coupons-manage', icon: Discount, label: '优惠券管理' },
  { path: '/admin/snacks', icon: ShoppingBag, label: '卖品管理' },
  { path: '/admin/shifts', icon: Clock, label: '交接班' },
  { path: '/admin/reports', icon: Document, label: '报表中心' },
]

const roleLabel = computed(() => {
  const code = authStore.user?.roleCode
  if (code === 'ROLE_SUPER_ADMIN') return '超级管理员'
  if (code === 'ROLE_STAFF') return '影院员工'
  return '用户'
})

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<style scoped>
.sidebar-brand { padding: 20px 16px 20px; }
.brand-title {
  font-weight: 700; font-size: 22px; letter-spacing: 2px;
  background: linear-gradient(135deg, #E8A850, #F0C070);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.brand-subtitle { font-size: 11px; color: var(--text-tertiary); letter-spacing: 1px; margin-top: 2px; }
.sidebar-nav { flex: 1; display: flex; flex-direction: column; gap: 1px; padding: 0 8px; overflow-y: auto; }
.sidebar-divider { margin: 8px 16px; border-top: 1px solid var(--border-light); }
.sidebar-footer { padding: 8px 8px 16px; }
.user-profile-card { display: flex; align-items: center; gap: 10px; padding: 8px 12px; margin-bottom: 4px; border-radius: 8px; background: rgba(128,128,128,0.04); }
.user-info { overflow: hidden; }
.user-name { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.user-role { font-size: 11px; color: var(--text-tertiary); }
.nav-label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
