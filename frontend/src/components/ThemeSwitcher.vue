<template>
  <el-dropdown trigger="click" @command="handleChange">
    <span class="theme-switcher-trigger">
      <el-icon :size="18"><BrushFilled /></el-icon>
      <span class="theme-label">主题</span>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="white" :class="{ active: current === 'white' }">
          <div class="theme-option">
            <span class="theme-dot white-dot"></span>
            <span>日间模式</span>
            <el-icon v-if="current === 'white'" class="check-icon"><Check /></el-icon>
          </div>
        </el-dropdown-item>
        <el-dropdown-item command="dark" :class="{ active: current === 'dark' }">
          <div class="theme-option">
            <span class="theme-dot dark-dot"></span>
            <span>夜间模式</span>
            <el-icon v-if="current === 'dark'" class="check-icon"><Check /></el-icon>
          </div>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { BrushFilled, Check } from '@element-plus/icons-vue'

const themeStore = useThemeStore()
const current = computed(() => themeStore.currentTheme)

function handleChange(theme) {
  themeStore.setTheme(theme)
}
</script>

<style scoped>
.theme-switcher-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--text-secondary);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
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
  gap: 8px;
  min-width: 120px;
}

.theme-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid var(--border-color);
  flex-shrink: 0;
}

.white-dot {
  background: linear-gradient(135deg, #ffffff 50%, #409eff 50%);
}

.dark-dot {
  background: linear-gradient(135deg, #1a1a2e 50%, #e94560 50%);
}

.check-icon {
  margin-left: auto;
  color: var(--color-primary);
}

.active {
  color: var(--color-primary);
}
</style>
