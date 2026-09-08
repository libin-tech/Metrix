<template>
  <button
    type="button"
    class="theme-toggle"
    :aria-label="$t(isDark ? 'common.switchToLight' : 'common.switchToDark')"
    :title="$t(isDark ? 'common.switchToLight' : 'common.switchToDark')"
    :aria-pressed="isDark"
    @click="toggleTheme"
  >
    <BulbFilled v-if="isDark" />
    <BulbOutlined v-else />
    <span>{{ $t(isDark ? 'common.darkTheme' : 'common.lightTheme') }}</span>
  </button>
</template>

<script setup>
import {BulbFilled, BulbOutlined} from '@ant-design/icons-vue'
import {useTheme} from '../composables/useTheme'
const {isDark, toggleTheme} = useTheme()
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  gap: 6px;
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  color: var(--workspace-ink);
  background: var(--workspace-surface);
  border: 1px solid var(--workspace-line);
  border-radius: 8px;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
}
.theme-toggle:hover { background: var(--workspace-soft-accent); }
.theme-toggle:focus-visible { outline: 2px solid var(--workspace-accent); outline-offset: 3px; }
@media (max-width: 1100px) {
  .theme-toggle { padding: 0 9px; }
  .theme-toggle span { display: none; }
}
</style>
