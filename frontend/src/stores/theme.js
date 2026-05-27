import { defineStore } from 'pinia'
import { ref } from 'vue'

const THEME_KEY = 'ttms-theme'

export const useThemeStore = defineStore('theme', () => {
  // State
  const currentTheme = ref('white')

  // Actions
  function setTheme(theme) {
    if (!['white', 'dark', 'purple'].includes(theme)) return
    currentTheme.value = theme
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem(THEME_KEY, theme)
  }

  function initTheme() {
    const saved = localStorage.getItem(THEME_KEY) || 'white'
    setTheme(saved)
  }

  return {
    currentTheme,
    setTheme,
    initTheme
  }
})
