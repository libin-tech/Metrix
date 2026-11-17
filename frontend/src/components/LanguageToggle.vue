<template>
  <a-button
    class="language-toggle"
    :aria-label="$t('common.language')"
    :title="$t('common.language')"
    @click="switchLanguage"
  >
    <template #icon><GlobalOutlined /></template>
    <span class="language-label">{{ $t(locale === 'zh-CN' ? 'common.english' : 'common.chinese') }}</span>
  </a-button>
</template>

<script setup>
import {GlobalOutlined} from '@ant-design/icons-vue'
import {useI18n} from 'vue-i18n'

const {locale} = useI18n()

const switchLanguage = () => {
  locale.value = locale.value === 'zh-CN' ? 'en' : 'zh-CN'
  localStorage.setItem('locale', locale.value)
}
</script>

<style scoped>
.language-toggle { display: inline-flex; align-items: center; gap: 6px; flex: none; height: 36px; padding-inline: 10px; }
.language-toggle :deep(.anticon + span) { margin-inline-start: 0; }
@media (max-width: 900px) {
  .language-toggle { justify-content: center; width: 36px; padding: 0; }
  .language-label { display: none; }
}
</style>
