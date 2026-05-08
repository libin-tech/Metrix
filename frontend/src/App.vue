<template>
  <div class="app-container">
    <!-- 登录页面 - 独立显示 -->
    <router-view v-if="isLoginPage" />
    
    <!-- 主应用布局 - 包含侧边栏和头部 -->
    <a-layout v-else class="layout">
      <!-- 侧边栏 -->
      <a-layout-sider v-model:collapsed="collapsed" :width="200" class="sider">
        <div class="logo">
          <BarChartOutlined class="logo-icon" />
          <span v-show="!collapsed" class="logo-text">量化交易决策分析</span>
        </div>
        <a-menu theme="dark" mode="inline" :selected-keys="[currentPath]" class="main-menu">
          <a-menu-item key="/" @click="navigate('/')">
            <HomeOutlined />
            <span>首页</span>
          </a-menu-item>
          <a-menu-item key="/analysis" @click="navigate('/analysis')">
            <LineChartOutlined />
            <span>股票分析</span>
          </a-menu-item>
          <a-sub-menu key="settings">
            <template #title>
              <SettingOutlined />
              <span>系统设置</span>
            </template>
            <a-menu-item key="/settings/ai-model" @click="navigate('/settings/ai-model')">
              <ApiOutlined />
              <span>AI模型配置</span>
            </a-menu-item>
            <a-menu-item key="/settings/notification" @click="navigate('/settings/notification')">
              <BellOutlined />
              <span>通知配置</span>
            </a-menu-item>
            <a-menu-item key="/settings/news-source" @click="navigate('/settings/news-source')">
              <FileTextOutlined />
              <span>新闻源配置</span>
            </a-menu-item>
            <a-menu-item key="/settings/market-data" @click="navigate('/settings/market-data')">
              <DatabaseOutlined />
              <span>行情数据配置</span>
            </a-menu-item>
            <a-menu-item key="/settings/stock-basic" @click="navigate('/settings/stock-basic')">
              <BookOutlined />
              <span>股票基本信息</span>
            </a-menu-item>
          </a-sub-menu>
        </a-menu>
        <a-menu theme="dark" mode="inline" class="logout-menu">
          <a-menu-item key="logout" @click="handleLogout">
            <LogoutOutlined />
            <span>退出登录</span>
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
                <a-button class="user-btn">
                  <UserOutlined />
                  <span>{{ currentUser }}</span>
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleLogout">
                      <LogoutOutlined />
                      退出登录
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </div>
        </a-layout-header>
        <a-layout-content class="content">
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </div>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {message} from 'ant-design-vue'
import {
  ApiOutlined,
  BarChartOutlined,
  BellOutlined,
  BookOutlined,
  DatabaseOutlined,
  DownOutlined,
  FileTextOutlined,
  HomeOutlined,
  LineChartOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  UserOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const collapsed = ref(false)
const currentUser = ref('管理员')

const isLoginPage = computed(() => route.path === '/login')

const currentPath = computed(() => route.path)

const pageTitleMap = {
  '/': '首页',
  '/home': '首页',
  '/analysis': '股票分析',
  '/settings/ai-model': 'AI模型配置',
  '/settings/notification': '通知配置',
  '/settings/news-source': '新闻源配置',
  '/settings/market-data': '行情数据配置',
  '/settings/stock-basic': '股票基本信息'
}

const pageTitle = computed(() => pageTitleMap[route.path] || '股票分析系统')

const navigate = (path) => {
  router.push(path)
}

const handleLogout = () => {
  localStorage.removeItem('token')
  message.success('退出成功')
  router.push('/login')
}

const handleRouteChange = () => {
  // 路由变化时的处理
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
  font-size: 24px;
  color: #1890ff;
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
  gap: 16px;
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
}
</style>
