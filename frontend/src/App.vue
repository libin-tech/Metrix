<template>
  <a-config-provider :theme="workspaceTheme">
  <div class="app-container">
    <!-- 登录页面 - 独立显示 -->
    <ThemeToggle v-if="isStandalonePage" class="standalone-theme-toggle" />
    <router-view v-if="isStandalonePage" />

    <!-- 主应用布局 -->
    <template v-else>
      <a-layout class="layout">
        <a-layout-header class="header">
          <div class="topbar-content">
            <button type="button" class="logo" aria-label="Metrix" @click="navigate('/workspace')">
          <div class="logo-icon-box"><RiseOutlined /></div>
              <span class="logo-text">{{ $t('layout.logo') }}</span>
            </button>
            <a-menu mode="horizontal" :selected-keys="[currentPath]" class="main-menu">
          <a-menu-item key="/workspace" @click="navigate('/workspace')">
            <HomeOutlined />
            <span>{{ $t('menu.home') }}</span>
          </a-menu-item>
          <a-menu-item v-if="hasPerm('system:analysis:view')" key="/analysis" @click="navigate('/analysis')">
            <LineChartOutlined />
            <span>{{ $t('menu.analysis') }}</span>
          </a-menu-item>
          <a-menu-item v-if="hasPerm('system:market-workbench:view')" key="/market-workbench" @click="navigate('/market-workbench')">
            <FundProjectionScreenOutlined />
            <span>{{ $t('menu.marketWorkbench') }}</span>
          </a-menu-item>
          <a-menu-item v-if="hasPerm('system:portfolio:view')" key="/portfolio" @click="navigate('/portfolio')">
            <WalletOutlined />
            <span>{{ $t('menu.portfolio') }}</span>
          </a-menu-item>
          <a-menu-item v-if="hasPerm('system:chat:view')" key="/chat" @click="navigate('/chat')">
            <MessageOutlined />
            <span>{{ $t('menu.chat') }}</span>
          </a-menu-item>
          <a-menu-item v-if="hasPerm('system:market-review:view')" key="/market-review" @click="navigate('/market-review')">
            <FundOutlined />
            <span>{{ $t('menu.marketReview') }}</span>
          </a-menu-item>
            </a-menu>
            <a class="nav-market-cloud" href="https://quote.eastmoney.com/stockhotmap/" target="_blank" rel="noopener noreferrer" :aria-label="$t('homeDesk.marketCloudMap')" :title="$t('homeDesk.marketCloudMap')">
              <CloudOutlined /><span>{{ $t('homeDesk.marketCloudMap') }}</span>
            </a>
            <ThemeToggle />
            <button type="button" class="language-switch" :aria-label="$t('common.language')" @click="switchLanguage">
              <GlobalOutlined />
              <span>{{ locale === 'zh-CN' ? $t('common.english') : $t('common.chinese') }}</span>
            </button>
            <div class="personal-center">
              <a-popover v-model:open="personalPopoverOpen" placement="bottomRight" trigger="click" overlay-class-name="personal-center-popover">
            <template #content>
              <div class="personal-actions">
                <div class="personal-popover-profile">
                  <a-avatar :size="36" class="personal-avatar"><UserOutlined /></a-avatar>
                  <strong>{{ currentUser || $t('settingsHub.personalCenter') }}</strong>
                </div>
                <div class="personal-menu-divider"></div>
                <button type="button" class="personal-menu-item" @click="openSettings"><SettingOutlined />{{ $t('settingsHub.openSettings') }}</button>
                <button type="button" class="personal-menu-item danger" @click="handleLogout"><LogoutOutlined />{{ $t('settingsHub.logout') }}</button>
              </div>
            </template>
                <div class="personal-trigger" role="button" tabindex="0" @keydown.enter="$event.currentTarget.click()">
              <a-avatar :size="34" class="personal-avatar"><UserOutlined /></a-avatar>
                  <span class="personal-copy"><strong>{{ currentUser || $t('settingsHub.personalCenter') }}</strong></span>
                </div>
          </a-popover>
            </div>
          </div>
        </a-layout-header>
        <a-layout-content class="content">
          <router-view />
          <div class="app-footer">
            <div class="footer-chrome">
              <ChromeOutlined /> {{ $t('common.recommendChrome') }}
            </div>
            <div class="footer-copyright">{{ $t('common.copyright', { year: new Date().getFullYear() }) }}</div>
          </div>
        </a-layout-content>
      </a-layout>
      <SettingsDrawer v-model:open="settingsDrawerOpen" :permissions="permissions" />
    </template>
  </div>
  </a-config-provider>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message, theme} from 'ant-design-vue'
import {
  ChromeOutlined,
  CloudOutlined,
  FundOutlined,
  FundProjectionScreenOutlined,
  GlobalOutlined,
  HomeOutlined,
  LineChartOutlined,
  LogoutOutlined,
  MessageOutlined,
  RiseOutlined,
  SettingOutlined,
  UserOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'

import {getCurrentUser, getPermissions} from './api'
import SettingsDrawer from './components/SettingsDrawer.vue'
import ThemeToggle from './components/ThemeToggle.vue'
import {useTheme} from './composables/useTheme'

const {locale} = useI18n()
const router = useRouter()
const route = useRoute()
const {isDark} = useTheme()
const workspaceTheme = computed(() => ({
  algorithm: isDark.value ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: {
    colorPrimary: isDark.value ? '#5278ba' : '#5878c2',
    ...(isDark.value ? {colorLink: '#94b9ff', colorLinkHover: '#b5ceff', colorBgBase: '#101824', colorBgContainer: '#172231', colorBgElevated: '#1d2b3f', colorText: '#e0e8f3', colorTextSecondary: '#a3b0c4', colorBorder: '#34445b'} : {})
  }
}))

const currentUser = ref('')
const settingsDrawerOpen = ref(false)
const personalPopoverOpen = ref(false)

const isStandalonePage = computed(() => route.path === '/' || route.path === '/login')
const currentPath = computed(() => route.path)

const permissions = ref([])
const permSet = computed(() => new Set(permissions.value))
const hasPerm = (code) => permSet.value.has(code)

const fetchUser = async () => {
  if (!localStorage.getItem('token')) return
  try {
    const res = await getPermissions()
    permissions.value = res.data || []
  } catch { /* ignore */ }
  try {
    const me = await getCurrentUser()
    currentUser.value = me.data?.nickname || me.data?.username || ''
    if (me.data?.role) localStorage.setItem('userRole', me.data.role)
  } catch { /* ignore */ }
}

const navigate = (path) => {
  router.push(path)
}

const openSettings = () => {
  personalPopoverOpen.value = false
  settingsDrawerOpen.value = true
}

const switchLanguage = () => {
  const nextLocale = locale.value === 'zh-CN' ? 'en' : 'zh-CN'
  locale.value = nextLocale
  localStorage.setItem('locale', nextLocale)
}

const handleLogout = () => {
  personalPopoverOpen.value = false
  localStorage.removeItem('token')
  localStorage.removeItem('userRole')
  message.success('退出成功')
  router.push('/login')
}

onMounted(() => {
  fetchUser()
})

watch(() => route.path, () => {
  fetchUser()
})
</script>

<style>
.standalone-theme-toggle { position: fixed; bottom: 20px; right: 20px; z-index: 1100; }
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: var(--theme-raised, #f7f9fc);
  color: var(--theme-text, #182336);
}

.app-container {
  min-height: 100vh;
}

.layout {
  min-height: 100vh;
}

.logo {
  display: flex;
  align-items: center;
  height: 76px;
  gap: 8px;
  padding: 0;
  color: inherit;
  background: transparent;
  border: 0;
  cursor: pointer;
}


.logo-icon-box {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(135deg, #c94755, #eb7f8a);
  color: var(--theme-text, #fff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-icon-box :deep(svg) { font-size: 17px; }

.logo-text {
  font-size: 13px;
  font-weight: 700;
  line-height: 76px;
  color: var(--theme-text, #1d2b43);
}

.layout > .header {
  height: 76px !important;
  min-height: 76px;
  line-height: normal;
  background: var(--theme-surface, rgba(255, 255, 255, .92));
  padding: 0;
  border-bottom: 1px solid var(--theme-line, #dfe6ef);
  box-shadow: 0 2px 14px rgba(34, 51, 79, .04);
  backdrop-filter: blur(12px);
}

.topbar-content {
  display: flex;
  align-items: center;
  gap: 30px;
  height: 76px;
  max-width: 1440px;
  padding: 0 32px;
  margin: 0 auto;
}

.main-menu {
  flex: 1;
  min-width: 0;
  height: 76px;
  overflow-x: hidden;
  overflow-y: hidden;
  scrollbar-width: none;
  line-height: 76px;
  color: var(--theme-text, #526178);
  background: transparent;
  border-bottom: 0;
}

.main-menu .ant-menu-item {
  height: 76px;
  margin-top: 0;
  margin-bottom: 0;
  line-height: 76px;
  padding-inline: 13px;
  color: var(--theme-muted, #66758b);
  font-size: 13px;
}

.main-menu .ant-menu-item:hover,
.main-menu .ant-menu-item-selected { color: var(--theme-text, #263d65) !important; font-weight: 650; }
.main-menu .ant-menu-item::after { border-bottom-color: var(--theme-line, #5878c2); border-bottom-width: 2px; }
.main-menu::-webkit-scrollbar { display: none; }

.nav-market-cloud { display: inline-flex; align-items: center; flex-shrink: 0; gap: 6px; color: var(--theme-text, #526178); font-size: 13px; white-space: nowrap; text-decoration: none; }
.nav-market-cloud:hover { color: var(--theme-text, #263d65); }

.language-switch {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 10px;
  color: var(--theme-text, #526178);
  font: inherit;
  font-size: 13px;
  background: var(--theme-surface, #f8faff);
  border: 1px solid var(--theme-line, #dce4ee);
  border-radius: 8px;
  cursor: pointer;
  transition: color .18s ease, background .18s ease, border-color .18s ease;
}

.language-switch:hover { color: var(--theme-text, #263d65); background: var(--theme-raised, #edf3ff); border-color: var(--theme-line, #b9cbe7); }

.personal-center {
  flex-shrink: 0;
  padding: 0;
  border: 0;
}

.personal-trigger {
  display: flex;
  align-items: center;
  height: 76px;
  min-width: 174px;
  gap: 10px;
  padding: 0 4px;
  color: var(--theme-text, #263957);
  text-align: left;
  cursor: pointer;
  transition: color .18s ease;
}

.personal-trigger:hover .personal-avatar { box-shadow: 0 0 0 3px rgba(143, 175, 231, .22); transform: scale(1.04); }
.personal-trigger:hover .personal-copy strong { color: var(--theme-text, #263d65); }

.personal-avatar {
  flex-shrink: 0;
  color: var(--theme-text, #edf3ff);
  background: linear-gradient(135deg, #5878c2, var(--theme-blue-surface, #87a1d8));
  transition: box-shadow .18s ease, transform .18s ease;
}

.personal-copy {
  display: block;
  flex: 1;
  min-width: 0;
}

.personal-copy strong { display: block; overflow: hidden; color: inherit; font-size: 13px; font-weight: 650; line-height: 76px; text-overflow: ellipsis; white-space: nowrap; }
.personal-actions { min-width: 222px; padding: 4px; }
.personal-popover-profile { display: flex; align-items: center; gap: 10px; padding: 8px; color: var(--theme-text, #1d2a3e); }
.personal-popover-profile strong { overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.personal-menu-divider { height: 1px; margin: 6px 4px; background: var(--theme-raised, #e7ebf1); }
.personal-menu-item { display: flex; align-items: center; width: 100%; gap: 9px; padding: 9px 8px; color: var(--theme-text, #334158); font: inherit; font-size: 13px; text-align: left; background: transparent; border: 0; border-radius: 7px; cursor: pointer; }
.personal-menu-item:hover { background: var(--theme-raised, #f2f5fa); }.personal-menu-item.danger { color: var(--theme-red, #c74956); }.personal-menu-item.danger:hover { background: var(--theme-raised, #fff1f2); }
.personal-center-popover .ant-popover-inner { padding: 4px; border: 1px solid var(--theme-line, #e2e8f1); border-radius: 12px; box-shadow: 0 12px 28px rgba(12, 26, 49, .18); }
.personal-center-popover .ant-popover-inner-content { padding: 0; }

@media (max-width: 800px) {
  .topbar-content { gap: 16px; padding: 0 16px; }
  .logo-text, .personal-copy { display: none; }
  .language-switch span, .nav-market-cloud span { display: none; }
  .nav-market-cloud { justify-content: center; width: 34px; height: 34px; }
  .language-switch { width: 34px; justify-content: center; padding: 0; }
  .personal-trigger { min-width: auto; padding: 6px; }
  .main-menu .ant-menu-item { padding-inline: 10px; }
  .main-menu { overflow-x: auto; }
}

.content {
  padding: 22px 32px;
  background-color: var(--theme-raised, #f3f6fb);
  background-image:
    radial-gradient(circle at 8% -12%, rgba(126, 161, 220, .20) 0, rgba(126, 161, 220, 0) 34%),
    radial-gradient(circle at 96% 8%, var(--theme-blue-surface, rgba(198, 216, 244, .54)) 0, rgba(198, 216, 244, 0) 31%),
    linear-gradient(rgba(139, 162, 199, .055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(139, 162, 199, .055) 1px, transparent 1px);
  background-position: center, center, -1px -1px, -1px -1px;
  background-size: auto, auto, 34px 34px, 34px 34px;
  background-attachment: fixed;
  min-height: calc(100vh - 76px);
  display: flex;
  flex-direction: column;
}

.app-footer {
  text-align: center;
  padding: 16px 0 8px;
  font-size: 12px;
  color: var(--theme-muted, #bbb);
  margin-top: auto;
}

.footer-chrome {
  margin-bottom: 2px;
}

.footer-copyright {
  color: var(--theme-text, #ccc);
}

</style>
