<template>
  <div class="app-container">
    <!-- 登录页面 - 独立显示 -->
    <router-view v-if="isLoginPage" />

    <!-- 主应用布局 -->
    <a-config-provider v-else :theme="workspaceTheme">
      <a-layout class="layout">
      <!-- 侧边栏 -->
      <a-layout-sider v-model:collapsed="collapsed" :width="200" class="sider">
        <div class="logo">
          <div class="logo-icon-box">M</div>
          <span v-show="!collapsed" class="logo-text">{{ $t('layout.logo') }}</span>
        </div>
        <a-menu theme="dark" mode="inline" :selected-keys="[currentPath]" class="main-menu">
          <a-menu-item key="/" @click="navigate('/')">
            <HomeOutlined />
            <span>{{ $t('menu.home') }}</span>
          </a-menu-item>
          <a-menu-item v-if="hasPerm('system:analysis:view')" key="/analysis" @click="navigate('/analysis')">
            <LineChartOutlined />
            <span>{{ $t('menu.analysis') }}</span>
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
        <div class="personal-center">
          <a-popover v-model:open="personalPopoverOpen" placement="topLeft" trigger="click" overlay-class-name="personal-center-popover">
            <template #content>
              <div class="personal-actions">
                <a-button block type="primary" @click="openSettings">
                  <SettingOutlined />{{ $t('settingsHub.openSettings') }}
                </a-button>
                <a-button block type="text" danger @click="handleLogout">
                  <LogoutOutlined />{{ $t('settingsHub.logout') }}
                </a-button>
              </div>
            </template>
            <button type="button" class="personal-trigger">
              <a-avatar :size="34" class="personal-avatar"><UserOutlined /></a-avatar>
              <span v-show="!collapsed" class="personal-copy"><strong>{{ currentUser || $t('settingsHub.personalCenter') }}</strong><small>{{ $t('settingsHub.personalCenter') }}</small></span>
              <DownOutlined v-show="!collapsed" class="personal-arrow" />
            </button>
          </a-popover>
        </div>
      </a-layout-sider>

      <!-- 主内容区 -->
      <a-layout>
        <a-layout-header class="header">
          <div class="header-content">
            <MenuFoldOutlined v-if="!collapsed" class="collapse-btn" @click="collapsed = !collapsed" />
            <MenuUnfoldOutlined v-else class="collapse-btn" @click="collapsed = !collapsed" />
            <span class="header-title">{{ pageTitle }}</span>
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
    </a-layout>
    </a-config-provider>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message} from 'ant-design-vue'
import {
  ChromeOutlined,
  DownOutlined,
  FundOutlined,
  HomeOutlined,
  LineChartOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MessageOutlined,
  SettingOutlined,
  UserOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'

import {getCurrentUser, getPermissions} from './api'
import SettingsDrawer from './components/SettingsDrawer.vue'

const {t} = useI18n()
const router = useRouter()
const route = useRoute()
const workspaceTheme = {token: {colorPrimary: '#5878c2'}}

const collapsed = ref(false)
const currentUser = ref('')
const settingsDrawerOpen = ref(false)
const personalPopoverOpen = ref(false)

const isLoginPage = computed(() => route.path === '/login')
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
  } catch { /* ignore */ }
}

const pageTitleMap = {
  '/': () => t('menu.home'),
  '/home': () => t('menu.home'),
  '/dashboard': () => t('menu.home'),
  '/analysis': () => t('menu.analysis'),
  '/portfolio': () => t('menu.portfolio'),
  '/chat': () => t('menu.chat'),
  '/market-review': () => t('menu.marketReview'),
  '/settings/ai-model': () => t('menu.aiModel'),
  '/settings/notification': () => t('menu.notification'),
  '/settings/news-source': () => t('menu.newsSource'),
  '/settings/market-data': () => t('menu.marketData'),
  '/settings/stock-basic': () => t('menu.stockBasic'),
  '/settings/account-management': () => t('menu.accountManagement'),
  '/admin/users': () => t('menu.userManagement'),
  '/admin/roles': () => t('menu.roleManagement'),
  '/admin/menus': () => t('menu.menuManagement'),
  '/admin/apis': () => t('menu.apiManagement'),
  '/admin/audit-log': () => t('menu.auditLog')
}

const pageTitle = computed(() => {
  const keyFn = pageTitleMap[route.path]
  return keyFn ? keyFn() : t('layout.pageTitleDefault')
})

const navigate = (path) => {
  router.push(path)
}

const openSettings = () => {
  personalPopoverOpen.value = false
  settingsDrawerOpen.value = true
}

const handleLogout = () => {
  personalPopoverOpen.value = false
  localStorage.removeItem('token')
  message.success('退出成功')
  router.push('/login')
}

onMounted(() => {
  document.documentElement.classList.remove('theme-blue', 'theme-green', 'theme-purple', 'theme-orange', 'theme-cyan')
  localStorage.removeItem('metrix-theme')
  fetchUser()
})

watch(() => route.path, () => {
  fetchUser()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f7f9fc;
  color: #182336;
}

.app-container {
  min-height: 100vh;
}

.layout {
  height: 100vh;
}

.sider {
  background: #111d31;
}

.sider .ant-layout-sider-children {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.main-menu {
  flex: 1;
  overflow-y: auto;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 64px;
  padding: 16px;
  border-bottom: 1px solid #273651;
}


.logo-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: linear-gradient(135deg, #4c77c7, #7e9edc);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: bold;
  color: white;
}

.header {
  background: rgba(255, 255, 255, .88);
  padding: 0;
  border-bottom: 1px solid #e7ebf1;
  box-shadow: none;
  backdrop-filter: blur(12px);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  height: 64px;
  padding: 0 24px;
}

.collapse-btn {
  font-size: 16px;
  margin-right: 16px;
  cursor: pointer;
  color: #666;
}

.header-title {
  font-size: 15px;
  font-weight: 650;
  color: #46536a;
}

.personal-center {
  flex-shrink: 0;
  padding: 12px 8px;
  border-top: 1px solid #273651;
}

.personal-trigger {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
  padding: 8px;
  color: #eef4ff;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 9px;
  cursor: pointer;
}

.personal-trigger:hover { background: rgba(255, 255, 255, .07); }

.personal-avatar {
  flex-shrink: 0;
  color: #e8f0ff;
  background: linear-gradient(135deg, #5e7ec5, #8fa7d9);
}

.personal-copy {
  display: grid;
  flex: 1;
  min-width: 0;
  gap: 1px;
}

.personal-copy strong { overflow: hidden; font-size: 12px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.personal-copy small { color: #9fb0cb; font-size: 10px; }
.personal-arrow { color: #8da0bf; font-size: 11px; }

.personal-actions { display: grid; min-width: 174px; gap: 4px; }
.personal-actions .ant-btn { display: flex; align-items: center; justify-content: flex-start; gap: 8px; }
.personal-actions .ant-btn-primary { justify-content: center; }
.personal-center-popover .ant-popover-inner { padding: 8px; border-radius: 12px; }
.personal-center-popover .ant-popover-inner-content { padding: 0; }

@media (max-width: 800px) {
  .header-content { padding: 0 16px; }
}

.content {
  padding: 22px 32px;
  background: #f7f9fc;
  min-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
}

.app-footer {
  text-align: center;
  padding: 16px 0 8px;
  font-size: 12px;
  color: #bbb;
  margin-top: auto;
}

.footer-chrome {
  margin-bottom: 2px;
}

.footer-copyright {
  color: #ccc;
}

</style>
