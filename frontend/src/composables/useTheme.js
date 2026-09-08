import {computed, readonly, ref} from 'vue'

const themeStorageKey = 'metrix-theme'
export const normalizeTheme = value => value === 'dark' ? 'dark' : 'light'
const currentTheme = ref('light')
const isDark = computed(() => currentTheme.value === 'dark')

export const setTheme = value => {
  currentTheme.value = normalizeTheme(value)
  document.documentElement.dataset.theme = currentTheme.value
  document.documentElement.style.colorScheme = currentTheme.value
  try {
    localStorage.setItem(themeStorageKey, currentTheme.value)
  } catch {
    // Theme switching remains available when browser storage is disabled.
  }
}

export const initializeTheme = () => {
  let savedTheme = 'light'
  try {
    savedTheme = localStorage.getItem(themeStorageKey)
  } catch {
    // Use the default when browser storage is disabled.
  }
  setTheme(savedTheme)
}

export const useTheme = () => ({
  currentTheme: readonly(currentTheme),
  isDark,
  toggleTheme: () => setTheme(isDark.value ? 'light' : 'dark')
})
