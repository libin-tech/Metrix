<template>
  <figure class="landing-preview">
    <img
      :src="source"
      :alt="$t(`landing.preview.screens.${featured ? 'chatHero' : view}`)"
      :width="featured ? 1320 : 1366"
      :height="featured ? 550 : 856"
      :loading="featured ? 'eager' : 'lazy'"
      decoding="async"
    />
    <figcaption>{{ $t('landing.preview.sample') }}</figcaption>
  </figure>
</template>

<script setup>
import {computed} from 'vue'
import {useI18n} from 'vue-i18n'
import {useTheme} from '../composables/useTheme'

const props = defineProps({
  view: {type: String, default: 'market'},
  featured: {type: Boolean, default: false}
})
const {locale} = useI18n()
const {currentTheme} = useTheme()
const source = computed(() => {
  const language = locale.value === 'en' ? 'en' : 'zh-CN'
  const image = props.featured ? 'chat-hero' : props.view
  return `${import.meta.env.BASE_URL}previews/${language}-${currentTheme.value}-${image}.webp`
})
</script>

<style scoped>
.landing-preview {
  min-width: 0;
  margin: 0;
}

.landing-preview img {
  display: block;
  width: 100%;
  height: auto;
  background: var(--workspace-surface);
  border: 1px solid var(--workspace-line);
  border-radius: var(--workspace-radius);
  box-shadow: var(--workspace-overlay-shadow);
}

.landing-preview figcaption {
  margin-top: 7px;
  color: var(--workspace-muted);
  font-size: 12px;
  text-align: end;
}
</style>
