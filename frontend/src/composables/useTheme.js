import {computed, onMounted, ref} from 'vue'

const THEME_KEY = 'metrix-theme'

const THEMES = {
  blue:   { name: '天空蓝',  primary: '#1890ff', primaryHover: '#40a9ff', primaryBg: '#e6f7ff' },
  green:  { name: '翡翠绿',  primary: '#52c41a', primaryHover: '#73d13d', primaryBg: '#f6ffed' },
  purple: { name: '暮光紫',  primary: '#722ed1', primaryHover: '#9254de', primaryBg: '#f9f0ff' },
  orange: { name: '落日橙',  primary: '#fa8c16', primaryHover: '#ffa940', primaryBg: '#fff7e6' },
  cyan:   { name: '极光青',  primary: '#13c2c2', primaryHover: '#36cfc9', primaryBg: '#e6fffb' },
}

const currentTheme = ref('blue')

const themeConfig = computed(() => ({
  token: {
    colorPrimary: THEMES[currentTheme.value].primary,
  }
}))

export function useTheme() {
  function applyTheme(key) {
    currentTheme.value = key
    const html = document.documentElement
    for (const t of Object.keys(THEMES)) {
      html.classList.toggle('theme-' + t, t === key)
    }
  }

  function select(key) {
    if (THEMES[key]) {
      applyTheme(key)
      localStorage.setItem(THEME_KEY, key)
    }
  }

  function init() {
    const saved = localStorage.getItem(THEME_KEY)
    select(saved && THEMES[saved] ? saved : 'blue')
  }

  onMounted(init)

  return { currentTheme, THEMES, select, themeConfig }
}
