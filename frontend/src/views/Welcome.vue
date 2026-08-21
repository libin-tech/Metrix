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
import {onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {FolderOutlined, MessageOutlined, SearchOutlined} from '@ant-design/icons-vue'
import {getPermissions} from '../api'

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
.welcome-container { max-width: 1080px; margin: 0 auto; padding: 48px 20px; }
.welcome-card { position: relative; min-height: 290px; padding: 28px 48px 28px 390px; margin-bottom: 42px; overflow: hidden; color: #fff; text-align: left; background: radial-gradient(circle at 85% 0%, #324d7d 0, transparent 34%), #182336; border-radius: 20px; }
.welcome-logo { position: absolute; left: 18px; bottom: -86px; width: 360px; height: 360px; object-fit: contain; opacity: .86; filter: drop-shadow(0 18px 30px rgba(0, 0, 0, .22)); }
.welcome-card h1 { font-size: 32px; font-weight: 700; color: #1a1a2e; margin: 0 0 8px 0; }
.welcome-desc { max-width: 560px; font-size: 15px; line-height: 1.8; color: #c0cde1; margin: 0; }

.guide-section h3 { font-size: 22px; margin-bottom: 24px; color: #182336; }
.guide-card { height: 100%; border-radius: 14px; }
.guide-card :deep(.ant-card-cover) { padding: 24px 0 0; text-align: center; }
.guide-icon { width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; margin: 0 auto; }
.guide-icon.blue { background: #edf3ff; color: #5878c2; }
.guide-icon.green { background: #eaf7f0; color: #258462; }
.guide-icon.purple { background: #f1edfb; color: #765ac2; }
@media (max-width: 720px) { .welcome-card { min-height: auto; padding: 30px; }.welcome-logo { display: none; } }
</style>
