<template>
  <div class="welcome-container">
    <div class="welcome-card">
      <img src="/Metrix.png" alt="Metrix" class="welcome-logo" />

      <br/>
      <p class="welcome-desc">{{ $t('welcome.tagline1') }}</p>
      <br/>
      <p class="welcome-desc">{{ $t('welcome.tagline2') }}</p>
      <br/>
      <p class="welcome-desc">{{ $t('welcome.tagline3') }}</p>


    </div>
    <div class="guide-section">
      <h3>{{ $t('welcome.guide') }}</h3>
      <a-row :gutter="24">
        <a-col :span="8">
          <a-card class="guide-card">
            <template #cover><div class="guide-icon blue"><SearchOutlined /></div></template>
            <a-card-meta :title="$t('welcome.analysisTitle')" :description="$t('welcome.analysisDesc')" />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card class="guide-card">
            <template #cover><div class="guide-icon green"><FolderOutlined /></div></template>
            <a-card-meta :title="$t('welcome.portfolioTitle')" :description="$t('welcome.portfolioDesc')" />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card class="guide-card">
            <template #cover><div class="guide-icon purple"><MessageOutlined /></div></template>
            <a-card-meta :title="$t('welcome.chatTitle')" :description="$t('welcome.chatDesc')" />
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { SearchOutlined, FolderOutlined, MessageOutlined } from '@ant-design/icons-vue'
import { getPermissions } from '../api'

const router = useRouter()

onMounted(async () => {
  try {
    const res = await getPermissions()
    const perms = new Set(res.data || [])
    if (perms.has('system:dashboard:view')) {
      router.replace('/dashboard')
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.welcome-container { max-width: 900px; margin: 0 auto; padding: 40px 20px; }
.welcome-card { text-align: center; margin-bottom: 48px; }
.welcome-logo { width: 500px; height: 500px; object-fit: contain; margin-bottom: 5px; }
.welcome-card h1 { font-size: 32px; font-weight: 700; color: #1a1a2e; margin: 0 0 8px 0; }
.welcome-desc { font-size: 16px; color: #999; margin: 0; }

.guide-section h3 { text-align: center; font-size: 20px; margin-bottom: 24px; color: #333; }
.guide-card { border-radius: 12px; }
.guide-card :deep(.ant-card-cover) { padding: 24px 0 0; text-align: center; }
.guide-icon { width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; margin: 0 auto; }
.guide-icon.blue { background: #e6f7ff; color: #1890ff; }
.guide-icon.green { background: #f6ffed; color: #52c41a; }
.guide-icon.purple { background: #f9f0ff; color: #722ed1; }
</style>
