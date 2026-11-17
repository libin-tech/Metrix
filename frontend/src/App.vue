<template>
  <a-config-provider :theme="workspaceTheme">
    <router-view v-if="isStandalonePage" />
    <a-layout v-else class="layout">
      <a-layout-header class="header">
        <div class="topbar-content">
          <MetrixBrand to="/workspace" />
          <nav class="main-menu" :aria-label="$t('layout.navigation')">
            <RouterLink v-for="item in navigationItems" :key="item.path" class="nav-link" :to="item.path">
              <component :is="item.icon" aria-hidden="true" />
              <span>{{ $t(item.label) }}</span>
            </RouterLink>
          </nav>

          <div class="topbar-actions">
            <a
              class="market-map"
              href="https://quote.eastmoney.com/stockhotmap/"
              target="_blank"
              rel="noopener noreferrer"
              :aria-label="$t('homeDesk.marketCloudMap')"
              :title="$t('homeDesk.marketCloudMap')"
            ><CloudOutlined /></a>
            <ThemeToggle />
            <LanguageToggle />
            <a-popover v-model:open="personalPopoverOpen" placement="bottomRight" trigger="click" overlay-class-name="personal-center-popover">
              <template #content>
                <div class="personal-actions">
                  <div class="personal-popover-profile">
                    <a-avatar :size="34" class="personal-avatar"><UserOutlined /></a-avatar>
                    <strong>{{ currentUser || $t('settingsHub.personalCenter') }}</strong>
                  </div>
                  <button type="button" class="personal-menu-item" @click="openSettings"><SettingOutlined />{{ $t('settingsHub.openSettings') }}</button>
                  <button type="button" class="personal-menu-item danger" @click="handleLogout"><LogoutOutlined />{{ $t('settingsHub.logout') }}</button>
                </div>
              </template>
              <button
                type="button"
                class="personal-trigger"
                :aria-label="currentUser ? `${$t('settingsHub.personalCenter')} · ${currentUser}` : $t('settingsHub.personalCenter')"
                :aria-expanded="personalPopoverOpen"
              >
                <a-avatar :size="30" class="personal-avatar"><UserOutlined /></a-avatar>
                <span v-if="currentUser" class="personal-name">{{ currentUser }}</span>
              </button>
            </a-popover>
          </div>
        </div>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
        <footer class="app-footer">
          <span><ChromeOutlined /> {{ $t('common.recommendChrome') }}</span>
          <span>{{ $t('common.copyright', {year: new Date().getFullYear()}) }}</span>
        </footer>
      </a-layout-content>
      <SettingsDrawer v-model:open="settingsDrawerOpen" :permissions="permissions" />
    </a-layout>
  </a-config-provider>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message, theme} from 'ant-design-vue'
import {
  ChromeOutlined, CloudOutlined, FundOutlined, FundProjectionScreenOutlined, HomeOutlined,
  LineChartOutlined, LogoutOutlined, MessageOutlined, SettingOutlined, UserOutlined, WalletOutlined
} from '@ant-design/icons-vue'
import {getCurrentUser, getPermissions} from './api'
import LanguageToggle from './components/LanguageToggle.vue'
import MetrixBrand from './components/MetrixBrand.vue'
import SettingsDrawer from './components/SettingsDrawer.vue'
import ThemeToggle from './components/ThemeToggle.vue'
import {useTheme} from './composables/useTheme'

const {t} = useI18n()
const router = useRouter()
const route = useRoute()
const {isDark} = useTheme()
const workspaceTheme = computed(() => {
  const algorithm = isDark.value ? theme.darkAlgorithm : theme.defaultAlgorithm
  const styles = getComputedStyle(document.documentElement)
  const css = name => styles.getPropertyValue(name).trim()
  return {
    algorithm,
    token: {
      fontFamily: css('--workspace-font'),
      borderRadius: parseInt(css('--workspace-control-radius'), 10),
      colorPrimary: css('--workspace-primary'),
      colorPrimaryHover: css('--workspace-primary-hover'),
      colorTextLightSolid: css('--workspace-on-primary'),
      colorLink: css('--workspace-accent'),
      colorLinkHover: css('--workspace-accent'),
      colorBgBase: css('--workspace-canvas'),
      colorBgContainer: css('--workspace-surface'),
      colorBgElevated: css('--workspace-surface'),
      colorText: css('--workspace-ink'),
      colorTextSecondary: css('--workspace-muted'),
      colorBorder: css('--workspace-line'),
      colorBorderSecondary: css('--workspace-line')
    },
    components: {
      Button: {primaryShadow: 'none'},
      Table: {
        headerBg: css('--workspace-raised'),
        rowHoverBg: css('--workspace-raised'),
        borderColor: css('--workspace-line')
      }
    }
  }
})

const currentUser = ref('')
const settingsDrawerOpen = ref(false)
const personalPopoverOpen = ref(false)
const permissions = ref([])

const isStandalonePage = computed(() => route.path === '/' || route.path === '/login')
const navigation = [
  {path: '/workspace', label: 'menu.home', icon: HomeOutlined},
  {path: '/market-workbench', label: 'menu.marketWorkbench', icon: FundProjectionScreenOutlined, permission: 'system:market-workbench:view'},
  {path: '/analysis', label: 'menu.analysis', icon: LineChartOutlined, permission: 'system:analysis:view'},
  {path: '/portfolio', label: 'menu.portfolio', icon: WalletOutlined, permission: 'system:portfolio:view'},
  {path: '/chat', label: 'menu.chat', icon: MessageOutlined, permission: 'system:chat:view'},
  {path: '/market-review', label: 'menu.marketReview', icon: FundOutlined, permission: 'system:market-review:view'}
]
const navigationItems = computed(() => {
  const allowed = new Set(permissions.value)
  return navigation.filter(item => !item.permission || allowed.has(item.permission))
})

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

const openSettings = () => {
  personalPopoverOpen.value = false
  settingsDrawerOpen.value = true
}

const handleLogout = () => {
  personalPopoverOpen.value = false
  localStorage.removeItem('token')
  localStorage.removeItem('userRole')
  message.success(t('layout.logoutSuccess'))
  router.push('/login')
}

onMounted(fetchUser)
watch(() => route.path, fetchUser)
</script>

<style scoped>
.layout { min-height: 100dvh; background: var(--workspace-canvas); }

.layout > .header {
  position: sticky;
  top: 0;
  z-index: 10;
  height: auto;
  padding: 0;
  line-height: normal;
  background: var(--workspace-surface);
  border-bottom: 1px solid var(--workspace-line);
}

.topbar-content {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0 clamp(12px, 2.3vw, 32px);
  min-height: 64px;
  padding-inline: var(--workspace-gutter);
}

.main-menu, .topbar-actions { display: flex; align-items: center; gap: 6px; }

.main-menu {
  min-width: 0;
  min-height: 54px;
  overflow-x: auto;
  scrollbar-width: thin;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  flex: none;
  gap: 7px;
  min-height: 38px;
  padding: 8px 11px;
  color: var(--workspace-muted);
  border-bottom: 2px solid transparent;
  font-size: 13px;
  font-weight: 550;
  text-decoration: none;
  white-space: nowrap;
}

.nav-link:hover { color: var(--workspace-accent); }
.nav-link.router-link-exact-active { color: var(--workspace-accent); border-bottom-color: currentColor; font-weight: 680; }
.nav-link:focus-visible { outline-offset: -2px; }

.market-map, .personal-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: 36px;
  min-height: 36px;
  padding: 0;
  color: var(--workspace-muted);
  background: transparent;
  border: 0;
  border-radius: var(--workspace-control-radius);
}

.personal-trigger { padding-inline: 3px; cursor: pointer; }
.market-map:hover, .personal-trigger:hover { color: var(--workspace-accent); background: var(--workspace-soft-accent); }
.personal-avatar { flex: none; color: var(--workspace-accent); background: var(--workspace-soft-accent); }
.personal-name { max-width: 7rem; overflow: hidden; color: var(--workspace-ink); font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }

.personal-actions { width: min(220px, calc(100vw - 40px)); }
.personal-popover-profile { display: flex; align-items: center; gap: 10px; padding: 8px 6px 12px; border-bottom: 1px solid var(--workspace-line); }
.personal-popover-profile strong { min-width: 0; overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.personal-menu-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  margin-top: 4px;
  padding: 9px 8px;
  color: var(--workspace-ink);
  font: inherit;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: var(--workspace-control-radius);
  cursor: pointer;
}

.personal-menu-item:hover { background: var(--workspace-soft-accent); }
.personal-menu-item.danger { color: var(--theme-red); }
.personal-menu-item.danger:hover { background: var(--theme-red-surface); }

.content {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
  padding: clamp(16px, 3vw, 32px) var(--workspace-gutter) 14px;
  background: var(--workspace-canvas);
}

.app-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 6px 16px;
  margin-top: auto;
  padding-block: 12px 2px;
  color: var(--workspace-muted);
  border-top: 1px solid var(--workspace-line);
  font-size: 12px;
}

@media (max-width: 1040px) {
  .topbar-content { grid-template-columns: minmax(0, 1fr) auto; padding-top: 8px; }
  .main-menu { grid-row: 2; grid-column: 1 / -1; min-height: 48px; }
  .topbar-actions { grid-row: 1; grid-column: 2; }
}

@media (max-width: 540px) {
  .personal-name { display: none; }
  .topbar-actions { gap: 3px; }
  .nav-link { padding-inline: 9px; }
}
</style>
