<template>
  <div class="app-container">
    <!-- 登录页面 - 独立显示 -->
    <router-view v-if="isLoginPage" />

    <!-- 主应用布局 -->
    <a-config-provider v-else :theme="themeConfig">
      <a-layout class="layout">
      <!-- 侧边栏 -->
      <a-layout-sider v-model:collapsed="collapsed" :width="200" class="sider">
        <div class="logo">
          <img src="/Metrix.png" class="logo-icon" alt="Metrix" />
          <span v-show="!collapsed" class="logo-text">{{ $t('layout.logo') }}</span>
        </div>
        <a-menu theme="dark" mode="inline" :selected-keys="[currentPath]" class="main-menu">
          <a-menu-item key="/" @click="navigate('/')">
            <HomeOutlined />
            <span>{{ $t('menu.home') }}</span>
          </a-menu-item>
          <a-menu-item key="/analysis" @click="navigate('/analysis')">
            <LineChartOutlined />
            <span>{{ $t('menu.analysis') }}</span>
          </a-menu-item>
          <a-menu-item key="/portfolio" @click="navigate('/portfolio')">
            <WalletOutlined />
            <span>{{ $t('menu.portfolio') }}</span>
          </a-menu-item>
          <a-menu-item key="/chat" @click="navigate('/chat')">
            <MessageOutlined />
            <span>{{ $t('menu.chat') }}</span>
          </a-menu-item>
          <a-menu-item key="/market-review" @click="navigate('/market-review')">
            <FundOutlined />
            <span>{{ $t('menu.marketReview') }}</span>
          </a-menu-item>
          <a-sub-menu key="settings">
            <template #title>
              <SettingOutlined />
              <span>{{ $t('menu.settings') }}</span>
            </template>
            <a-menu-item key="/settings/ai-model" @click="navigate('/settings/ai-model')">
              <ApiOutlined />
              <span>{{ $t('menu.aiModel') }}</span>
            </a-menu-item>
            <a-menu-item key="/settings/notification" @click="navigate('/settings/notification')">
              <BellOutlined />
              <span>{{ $t('menu.notification') }}</span>
            </a-menu-item>
            <a-menu-item key="/settings/news-source" @click="navigate('/settings/news-source')">
              <FileTextOutlined />
              <span>{{ $t('menu.newsSource') }}</span>
            </a-menu-item>
            <a-menu-item key="/settings/market-data" @click="navigate('/settings/market-data')">
              <DatabaseOutlined />
              <span>{{ $t('menu.marketData') }}</span>
            </a-menu-item>
            <a-menu-item key="/settings/stock-basic" @click="navigate('/settings/stock-basic')">
              <BookOutlined />
              <span>{{ $t('menu.stockBasic') }}</span>
            </a-menu-item>
          </a-sub-menu>
        </a-menu>
        <a-menu theme="dark" mode="inline" class="logout-menu">
          <a-menu-item key="logout" @click="handleLogout">
            <LogoutOutlined />
            <span>{{ $t('menu.logout') }}</span>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>

      <!-- 主内容区 -->
      <a-layout>
        <a-layout-header class="header">
          <div class="header-content">
            <MenuFoldOutlined v-if="!collapsed" class="collapse-btn" @click="collapsed = !collapsed" />
            <MenuUnfoldOutlined v-else class="collapse-btn" @click="collapsed = !collapsed" />
            <span class="header-title">{{ pageTitle }}</span>
            <div class="header-actions">
              <a-dropdown>
                <a-button class="theme-btn">
                  <BgColorsOutlined />
                </a-button>
                <template #overlay>
                  <a-menu @click="({key}) => selectTheme(key)">
                    <a-menu-item v-for="t in themeList" :key="t.key">
                      <span class="theme-swatch" :style="{background: t.primary}"></span>
                      {{ t.name }}
                      <CheckOutlined v-if="t.key === currentTheme" class="theme-check" />
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-dropdown>
                <a-button class="lang-btn">
                  <GlobalOutlined />
                  <span>{{ locale === 'zh-CN' ? '中文' : 'English' }}</span>
                </a-button>
                <template #overlay>
                  <a-menu @click="switchLang">
                    <a-menu-item key="zh-CN">中文</a-menu-item>
                    <a-menu-item key="en">English</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-dropdown>
                <a-button class="user-btn">
                  <UserOutlined />
                  <span>{{ currentUser }}</span>
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleLogout">
                      <LogoutOutlined />
                      {{ $t('menu.logout') }}
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
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
    </a-layout>
    </a-config-provider>
  </div>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message} from 'ant-design-vue'
import {
  ApiOutlined,
  BarChartOutlined,
  BellOutlined,
  BgColorsOutlined,
  BookOutlined,
  CheckOutlined,
  ChromeOutlined,
  DatabaseOutlined,
  DownOutlined,
  FileTextOutlined,
  FundOutlined,
  GlobalOutlined,
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

import {useTheme} from './composables/useTheme.js'

const {locale, t} = useI18n()
const router = useRouter()
const route = useRoute()
const {currentTheme, THEMES, select: selectTheme, themeConfig} = useTheme()

const themeList = Object.entries(THEMES).map(([key, val]) => ({key, ...val}))

const collapsed = ref(false)
const currentUser = ref('管理员')

const isLoginPage = computed(() => route.path === '/login')
const currentPath = computed(() => route.path)

const pageTitleMap = {
  '/': () => t('menu.home'),
  '/home': () => t('menu.home'),
  '/analysis': () => t('menu.analysis'),
  '/portfolio': () => t('menu.portfolio'),
  '/chat': () => t('menu.chat'),
  '/market-review': () => t('menu.marketReview'),
  '/settings/ai-model': () => t('menu.aiModel'),
  '/settings/notification': () => t('menu.notification'),
  '/settings/news-source': () => t('menu.newsSource'),
  '/settings/market-data': () => t('menu.marketData'),
  '/settings/stock-basic': () => t('menu.stockBasic'),
  '/settings/account-management': () => t('menu.accountManagement')
}

const pageTitle = computed(() => {
  const keyFn = pageTitleMap[route.path]
  return keyFn ? keyFn() : t('layout.pageTitleDefault')
})

const navigate = (path) => {
  router.push(path)
}

const switchLang = ({key}) => {
  locale.value = key
  localStorage.setItem('locale', key)
}

const handleLogout = () => {
  localStorage.removeItem('token')
  message.success('退出成功')
  router.push('/login')
}

const handleRouteChange = () => {
}

onMounted(() => {
  router.afterEach(handleRouteChange)
})

onUnmounted(() => {
  router.afterEach(handleRouteChange)
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
  background-color: #f5f7fa;
}

.app-container {
  min-height: 100vh;
}

.layout {
  height: 100vh;
}

.sider {
  background: #001529;
  display: flex;
  flex-direction: column;
}

.main-menu {
  flex: 1;
  overflow-y: auto;
}

.logout-menu {
  position: fixed;
  bottom: 0;
  width: 200px;
  background: #001529;
  border-top: 1px solid #1f2f3d;
}

.sider.collapsed .logout-menu {
  width: 80px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 64px;
  padding: 16px;
  border-bottom: 1px solid #1f2f3d;
}

.logo-icon {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.logo-text {
  font-size: 16px;
  font-weight: bold;
  color: white;
}

.header {
  background: white;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.lang-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
}

.content {
  padding: 20px;
  background: #f5f7fa;
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

.theme-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  font-size: 18px;
  cursor: pointer;
  background: transparent;
  color: #666;
}

.theme-btn:hover {
  color: var(--primary-color);
}

.theme-swatch {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
  border: 1px solid rgba(0,0,0,0.1);
}

.theme-check {
  float: right;
  color: var(--primary-color);
  line-height: 22px;
}

/* Theme color variables */
:root,
html.theme-blue {
  --primary-color: #1890ff;
  --primary-hover: #40a9ff;
  --primary-bg: #e6f7ff;
}

html.theme-green {
  --primary-color: #52c41a;
  --primary-hover: #73d13d;
  --primary-bg: #f6ffed;
}

html.theme-purple {
  --primary-color: #722ed1;
  --primary-hover: #9254de;
  --primary-bg: #f9f0ff;
}

html.theme-orange {
  --primary-color: #fa8c16;
  --primary-hover: #ffa940;
  --primary-bg: #fff7e6;
}

html.theme-cyan {
  --primary-color: #13c2c2;
  --primary-hover: #36cfc9;
  --primary-bg: #e6fffb;
}

:root {
  --bg-body: #f5f7fa;
  --bg-content: #f5f7fa;
  --bg-header: #fff;
  --bg-card: #fff;
  --text-primary: #333;
  --text-secondary: #666;
  --border-color: #f0f0f0;
  --header-shadow: rgba(0, 0, 0, 0.1);
}
</style>
