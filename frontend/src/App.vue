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
          <a-sub-menu v-if="showSettings" key="settings">
            <template #title>
              <SettingOutlined />
              <span>{{ $t('menu.settings') }}</span>
            </template>
            <a-menu-item v-if="hasPerm('system:ai-model:view')" key="/settings/ai-model" @click="navigate('/settings/ai-model')">
              <ApiOutlined />
              <span>{{ $t('menu.aiModel') }}</span>
            </a-menu-item>
            <a-menu-item v-if="hasPerm('system:notification:view')" key="/settings/notification" @click="navigate('/settings/notification')">
              <BellOutlined />
              <span>{{ $t('menu.notification') }}</span>
            </a-menu-item>
            <a-menu-item v-if="hasPerm('system:news-source:view')" key="/settings/news-source" @click="navigate('/settings/news-source')">
              <FileTextOutlined />
              <span>{{ $t('menu.newsSource') }}</span>
            </a-menu-item>
            <a-menu-item v-if="hasPerm('system:market-data:view')" key="/settings/market-data" @click="navigate('/settings/market-data')">
              <DatabaseOutlined />
              <span>{{ $t('menu.marketData') }}</span>
            </a-menu-item>
            <a-menu-item v-if="hasPerm('system:stock-basic:view')" key="/settings/stock-basic" @click="navigate('/settings/stock-basic')">
              <BookOutlined />
              <span>{{ $t('menu.stockBasic') }}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu v-if="showAdmin" key="admin">
            <template #title>
              <SafetyCertificateOutlined />
              <span>{{ $t('menu.admin') }}</span>
            </template>
            <a-menu-item v-if="hasPerm('system:user:list')" key="/admin/users" @click="navigate('/admin/users')">
              <TeamOutlined />
              <span>{{ $t('menu.userManagement') }}</span>
            </a-menu-item>
            <a-menu-item v-if="hasPerm('system:role:list')" key="/admin/roles" @click="navigate('/admin/roles')">
              <SafetyOutlined />
              <span>{{ $t('menu.roleManagement') }}</span>
            </a-menu-item>
            <a-menu-item v-if="hasPerm('system:menu:list')" key="/admin/menus" @click="navigate('/admin/menus')">
              <MenuOutlined />
              <span>{{ $t('menu.menuManagement') }}</span>
            </a-menu-item>
            <a-menu-item v-if="hasPerm('system:api:list')" key="/admin/apis" @click="navigate('/admin/apis')">
              <ApiOutlined />
              <span>{{ $t('menu.apiManagement') }}</span>
            </a-menu-item>
            <a-menu-item v-if="hasPerm('system:audit:view')" key="/admin/audit-log" @click="navigate('/admin/audit-log')">
              <FileTextOutlined />
              <span>{{ $t('menu.auditLog') }}</span>
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
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message} from 'ant-design-vue'
import {
  ApiOutlined,
  BellOutlined,
  BookOutlined,
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
  MenuOutlined,
  MenuUnfoldOutlined,
  MessageOutlined,
  SafetyCertificateOutlined,
  SafetyOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'

import {getCurrentUser, getPermissions} from './api'

const {locale, t} = useI18n()
const router = useRouter()
const route = useRoute()
const workspaceTheme = {token: {colorPrimary: '#5878c2'}}

const collapsed = ref(false)
const currentUser = ref('')

const isLoginPage = computed(() => route.path === '/login')
const currentPath = computed(() => route.path)

const permissions = ref([])
const permSet = computed(() => new Set(permissions.value))
const hasPerm = (code) => permSet.value.has(code)

const showSettings = computed(() =>
  ['system:ai-model:view', 'system:notification:view', 'system:news-source:view',
   'system:market-data:view', 'system:stock-basic:view', 'system:account:view']
    .some(code => permSet.value.has(code)))

const showAdmin = computed(() =>
  ['system:user:list', 'system:role:list', 'system:menu:list',
   'system:api:list', 'system:stats:view', 'system:audit:view']
    .some(code => permSet.value.has(code)))

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

const switchLang = ({key}) => {
  locale.value = key
  localStorage.setItem('locale', key)
}

const handleLogout = () => {
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
  background: #111d31;
  border-top: 1px solid #273651;
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
  font-size: 15px;
  font-weight: 650;
  color: #46536a;
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
