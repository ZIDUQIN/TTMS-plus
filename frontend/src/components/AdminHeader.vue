<template>
  <header class="admin-header">
    <div class="header-left">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">
          <el-icon :size="14"><DataBoard /></el-icon> 管理后台
        </el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentPage">{{ currentPage }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <button class="icon-btn" title="通知">
        <el-icon :size="18"><Bell /></el-icon>
      </button>
      <button class="icon-btn" title="切换主题" @click="toggleTheme">
        <el-icon :size="18"><Sunny v-if="themeStore.currentTheme === 'light'" /><Moon v-else /></el-icon>
      </button>
      <div class="header-divider"></div>

      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-menu-trigger">
          <span>{{ authStore.realName || authStore.username || '管理员' }}</span>
          <el-icon :size="16"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon :size="14"><User /></el-icon> 个人中心
            </el-dropdown-item>
            <el-dropdown-item command="home">
              <el-icon :size="14"><HomeFilled /></el-icon> 返回前台
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon :size="14" color="var(--color-danger)"><SwitchButton /></el-icon>
              <span style="color:var(--color-danger)">退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { ElMessage } from 'element-plus'
import { DataBoard, Bell, Sunny, Moon, ArrowDown, User, HomeFilled, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute(); const router = useRouter()
const authStore = useAuthStore(); const themeStore = useThemeStore()
const searchText = ref('')

const currentPage = computed(() => route.meta?.title || '')

function toggleTheme() { themeStore.toggleTheme() }
function handleCommand(cmd) {
  switch (cmd) {
    case 'profile': router.push('/profile'); break
    case 'home': router.push('/home'); break
    case 'logout': authStore.logout(); ElMessage.success('已退出登录'); router.push('/login'); break
  }
}
</script>

<style scoped>
.header-left { display: flex; align-items: center; flex: 1; }
.header-right { display: flex; align-items: center; gap: 8px; }
.icon-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border: none; background: transparent; color: var(--text-secondary); border-radius: 6px; cursor: pointer; transition: all 0.15s; }
.icon-btn:hover { color: var(--color-primary); background: rgba(128,128,128,0.08); }
.header-divider { width: 1px; height: 20px; background: var(--border-light); margin: 0 4px; }
.user-menu-trigger { display: flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 500; color: var(--text-primary); }
.user-menu-trigger:hover { background: rgba(128,128,128,0.06); }
</style>
