<template>
  <el-dropdown trigger="click" @command="handleChange">
    <span class="theme-switcher-trigger">
      <el-icon :size="18">
        <Sunny v-if="current === 'light'" />
        <Moon v-else />
      </el-icon>
      <span class="theme-label">外观</span>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="light" :class="{ active: current === 'light' }">
          <div class="theme-option">
            <el-icon class="option-icon"><Sunny /></el-icon>
            <span>浅色</span>
            <el-icon v-if="current === 'light'" class="check-icon"><Check /></el-icon>
          </div>
        </el-dropdown-item>
        <el-dropdown-item command="dark" :class="{ active: current === 'dark' }">
          <div class="theme-option">
            <el-icon class="option-icon"><Moon /></el-icon>
            <span>深色</span>
            <el-icon v-if="current === 'dark'" class="check-icon"><Check /></el-icon>
          </div>
        </el-dropdown-item>
        <el-dropdown-item command="auto" divided>
          <div class="theme-option">
            <el-icon class="option-icon"><Monitor /></el-icon>
            <span>自动</span>
            <span class="auto-hint">跟随系统</span>
          </div>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { Sunny, Moon, Monitor, Check } from '@element-plus/icons-vue'

const themeStore = useThemeStore()
const current = computed(() => themeStore.currentTheme)

function handleChange(theme) {
  if (theme === 'auto') {
    // Clear saved preference — triggers system follow
    localStorage.removeItem('ttms-theme')
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    themeStore.setTheme(prefersDark ? 'dark' : 'light')
  } else {
    themeStore.setTheme(theme)
  }
}
</script>

<style scoped>
.theme-switcher-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  padding: 6px 10px;
  border-radius: var(--radius-md);
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.theme-switcher-trigger:hover {
  color: var(--color-primary);
  background: var(--bg-hover);
}

.theme-label {
  font-size: 13px;
}

.theme-option {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 140px;
  font-size: 14px;
}

.option-icon {
  font-size: 16px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.check-icon {
  margin-left: auto;
  color: var(--color-primary);
  font-size: 16px;
}

.auto-hint {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-tertiary);
}

.active {
  color: var(--color-primary);
}
</style>
