import { defineStore } from 'pinia'
import { ref } from 'vue'

const THEME_KEY = 'ttms-theme'

export const useThemeStore = defineStore('theme', () => {
  // State: 'light' | 'dark'
  const currentTheme = ref('light')

  // Actions
  function setTheme(theme) {
    if (!['light', 'dark'].includes(theme)) return
    currentTheme.value = theme
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem(THEME_KEY, theme)
  }

  function initTheme() {
    const saved = localStorage.getItem(THEME_KEY)

    if (saved === 'light' || saved === 'dark') {
      // User has explicitly chosen a theme
      setTheme(saved)
    } else {
      // No saved preference — follow system color scheme
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      setTheme(prefersDark ? 'dark' : 'light')
    }

    // Listen for system theme changes (only when user hasn't manually set)
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      const currentSaved = localStorage.getItem(THEME_KEY)
      // Only auto-switch if user hasn't manually set a preference
      if (currentSaved !== 'light' && currentSaved !== 'dark') {
        setTheme(e.matches ? 'dark' : 'light')
      }
    })
  }

  // Convenience computed-like helpers
  function toggleTheme() {
    setTheme(currentTheme.value === 'light' ? 'dark' : 'light')
  }

  return {
    currentTheme,
    setTheme,
    initTheme,
    toggleTheme
  }
})
