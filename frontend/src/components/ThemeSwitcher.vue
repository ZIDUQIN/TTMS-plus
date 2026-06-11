<template>
  <el-dropdown trigger="click" @command="handleChange">
    <span class="theme-trigger" title="切换主题">
      <el-icon :size="18"><Sunny v-if="current === 'light'" /><Moon v-else /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="dark">
          <el-icon :size="14"><Moon /></el-icon> 深色模式
          <el-icon v-if="current === 'dark'" :size="14" style="margin-left:auto" color="var(--color-primary)"><Check /></el-icon>
        </el-dropdown-item>
        <el-dropdown-item command="light">
          <el-icon :size="14"><Sunny /></el-icon> 浅色模式
          <el-icon v-if="current === 'light'" :size="14" style="margin-left:auto" color="var(--color-primary)"><Check /></el-icon>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { Sunny, Moon, Check } from '@element-plus/icons-vue'

const themeStore = useThemeStore()
const current = computed(() => themeStore.currentTheme)

function handleChange(theme) { themeStore.setTheme(theme) }
</script>

<style scoped>
.theme-trigger { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; border-radius: 6px; cursor: pointer; color: var(--text-secondary); transition: all 0.15s; }
.theme-trigger:hover { color: var(--color-primary); background: rgba(128,128,128,0.06); }
</style>
