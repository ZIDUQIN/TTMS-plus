<template>
  <header class="cinema-navbar" :class="{ 'is-scrolled': isScrolled }">
    <div class="navbar-inner">
      <router-link to="/home" class="navbar-logo">
        <el-icon :size="22" color="var(--color-primary)"><VideoCameraFilled /></el-icon>
        <span class="logo-text">TTMS</span>
      </router-link>

      <nav class="navbar-links">
        <router-link to="/home" class="nav-link" :class="{ active: isHome }">首页</router-link>
      </nav>

      <div class="navbar-right">
        <ThemeSwitcher />

        <router-link v-if="authStore.isLoggedIn" to="/my-orders" class="nav-icon-link" title="我的订单">
          <el-icon :size="18"><Tickets /></el-icon>
        </router-link>
        <router-link v-if="authStore.isLoggedIn" to="/my-coupons" class="nav-icon-link" title="优惠券">
          <el-icon :size="18"><Discount /></el-icon>
        </router-link>

        <template v-if="authStore.isLoggedIn">
          <el-dropdown trigger="click">
            <div class="user-trigger">
              <el-icon :size="20" color="var(--color-primary)"><UserFilled /></el-icon>
              <span class="username-text">{{ authStore.realName }}</span>
              <el-icon :size="14"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon :size="14"><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item v-if="authStore.isAdmin" @click="$router.push('/admin/dashboard')">
                  <el-icon :size="14"><DataBoard /></el-icon> 管理后台
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon :size="14" color="var(--color-danger)"><SwitchButton /></el-icon>
                  <span style="color:var(--color-danger)">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import ThemeSwitcher from './ThemeSwitcher.vue'
import { VideoCameraFilled, Tickets, Discount, UserFilled, User, ArrowDown, DataBoard, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute(); const router = useRouter()
const authStore = useAuthStore()
const isScrolled = ref(false)
const isHome = computed(() => route.path === '/home')

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(() => { authStore.logout(); ElMessage.success('已退出登录'); router.push('/home') }).catch(() => {})
}
function onScroll() { isScrolled.value = window.scrollY > 20 }
onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.cinema-navbar {
  position: sticky; top: 0; z-index: 1000; width: 100%; height: var(--header-height);
  background: var(--bg-card); border-bottom: 1px solid var(--border-light);
}
.navbar-inner { max-width: 1400px; margin: 0 auto; height: 100%; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; }
.navbar-logo { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.logo-text { font-weight: 700; font-size: 20px; letter-spacing: 1px; color: var(--color-primary); }
.navbar-links { display: flex; align-items: center; gap: 4px; }
.nav-link { display: inline-flex; align-items: center; font-size: 13px; font-weight: 500; color: var(--text-secondary); padding: 6px 12px; border-radius: var(--radius-md); transition: all 0.15s; }
.nav-link:hover { color: var(--color-primary); background: rgba(128,128,128,0.06); }
.nav-link.active { color: var(--color-primary); }
.navbar-right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.nav-icon-link { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; border-radius: 6px; color: var(--text-secondary); transition: all 0.15s; }
.nav-icon-link:hover { color: var(--color-primary); background: rgba(128,128,128,0.06); }
.user-trigger { display: flex; align-items: center; gap: 6px; padding: 3px 10px 3px 4px; border-radius: 20px; cursor: pointer; }
.user-trigger:hover { background: rgba(128,128,128,0.06); }
.username-text { font-size: 13px; color: var(--text-secondary); max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 768px) { .navbar-inner { padding: 0 12px; } .username-text { display: none; } }
</style>
