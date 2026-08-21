<template>
  <a-drawer
    :open="open"
    :width="'min(1080px, 100vw)'"
    placement="right"
    class="settings-drawer"
    @update:open="$emit('update:open', $event)"
  >
    <template #title>
      <div class="drawer-title">
        <a-button v-if="selectedEntry" type="text" class="back-button" @click="selectedKey = null"><ArrowLeftOutlined /></a-button>
        <span>{{ selectedEntry?.title || $t('settingsHub.title') }}</span>
      </div>
    </template>

    <section v-if="!selectedEntry" class="settings-hub">
      <p class="hub-intro">{{ $t('settingsHub.description') }}</p>
      <template v-for="section in entrySections" :key="section.key">
        <div v-if="section.items.length" class="settings-section">
          <p>{{ section.title }}</p>
          <button v-for="entry in section.items" :key="entry.key" type="button" @click="selectedKey = entry.key">
            <component :is="entry.icon" />
            <span><strong>{{ entry.title }}</strong><small>{{ entry.description }}</small></span>
            <RightOutlined />
          </button>
        </div>
      </template>
      <a-empty v-if="!entries.length" :description="$t('settingsHub.noAccess')" />
    </section>

    <section v-else class="settings-page"><component :is="selectedEntry.component" /></section>
  </a-drawer>
</template>

<script setup>
import {computed, defineAsyncComponent, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {
  ApiOutlined,
  ArrowLeftOutlined,
  BellOutlined,
  BookOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  FundOutlined,
  MenuOutlined,
  RobotOutlined,
  RightOutlined,
  SafetyOutlined,
  TeamOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'

defineEmits(['update:open'])
const props = defineProps({
  open: {type: Boolean, default: false},
  permissions: {type: Array, default: () => []}
})

const {t} = useI18n()
const selectedKey = ref(null)
const permissionSet = computed(() => new Set(props.permissions))
const can = code => permissionSet.value.has(code)
const pageComponents = {
  aiModel: defineAsyncComponent(() => import('../views/AiModelConfig.vue')),
  notification: defineAsyncComponent(() => import('../views/NotificationConfig.vue')),
  newsSource: defineAsyncComponent(() => import('../views/NewsSourceConfig.vue')),
  marketData: defineAsyncComponent(() => import('../views/MarketDataConfig.vue')),
  stockBasic: defineAsyncComponent(() => import('../views/StockBasic.vue')),
  accounts: defineAsyncComponent(() => import('../views/AccountManagement.vue')),
  users: defineAsyncComponent(() => import('../views/admin/UserManagement.vue')),
  roles: defineAsyncComponent(() => import('../views/admin/RoleManagement.vue')),
  menus: defineAsyncComponent(() => import('../views/admin/MenuManagement.vue')),
  apis: defineAsyncComponent(() => import('../views/admin/ApiManagement.vue')),
  audit: defineAsyncComponent(() => import('../views/admin/AuditLog.vue'))
}

const entries = computed(() => [
  {key: 'aiModel', section: 'workspace', permission: 'system:ai-model:view', icon: RobotOutlined},
  {key: 'notification', section: 'workspace', permission: 'system:notification:view', icon: BellOutlined},
  {key: 'newsSource', section: 'workspace', permission: 'system:news-source:view', icon: FileTextOutlined},
  {key: 'marketData', section: 'workspace', permission: 'system:market-data:view', icon: DatabaseOutlined},
  {key: 'stockBasic', section: 'workspace', permission: 'system:stock-basic:view', icon: BookOutlined},
  {key: 'accounts', section: 'workspace', permission: 'system:account:view', icon: WalletOutlined},
  {key: 'users', section: 'management', permission: 'system:user:list', icon: TeamOutlined},
  {key: 'roles', section: 'management', permission: 'system:role:list', icon: SafetyOutlined},
  {key: 'menus', section: 'management', permission: 'system:menu:list', icon: MenuOutlined},
  {key: 'apis', section: 'management', permission: 'system:api:list', icon: ApiOutlined},
  {key: 'audit', section: 'management', permission: 'system:audit:view', icon: FundOutlined}
].filter(entry => can(entry.permission)).map(entry => ({
  ...entry,
  title: t(`settingsHub.entries.${entry.key}.title`),
  description: t(`settingsHub.entries.${entry.key}.description`),
  component: pageComponents[entry.key]
})))

const entrySections = computed(() => [
  {key: 'workspace', title: t('settingsHub.workspaceSection'), items: entries.value.filter(entry => entry.section === 'workspace')},
  {key: 'management', title: t('settingsHub.managementSection'), items: entries.value.filter(entry => entry.section === 'management')}
])
const selectedEntry = computed(() => entries.value.find(entry => entry.key === selectedKey.value) || null)

watch(() => props.open, isOpen => {
  if (!isOpen) selectedKey.value = null
})
</script>

<style scoped>
.drawer-title { display: flex; align-items: center; gap: 8px; color: #182336; font-weight: 700; }.back-button { margin-left: -10px; }
.settings-hub { max-width: 640px; margin: 0 auto; }.hub-intro { margin: 2px 0 30px; color: #718098; line-height: 1.7; }.settings-section + .settings-section { margin-top: 32px; }.settings-section > p { margin: 0 0 10px; color: #72809a; font-size: 11px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }.settings-section button { display: grid; grid-template-columns: 38px 1fr 18px; align-items: center; width: 100%; gap: 12px; padding: 14px 12px; color: #26354d; text-align: left; background: transparent; border: 1px solid transparent; border-bottom-color: #e8edf4; border-radius: 10px; cursor: pointer; }.settings-section button:hover { background: #f3f6fb; border-color: #e0e8f5; }.settings-section button > :first-child { color: #5d7dc5; font-size: 18px; }.settings-section strong, .settings-section small { display: block; }.settings-section strong { font-size: 14px; }.settings-section small { margin-top: 3px; color: #8090a7; font-size: 12px; }.settings-section button > :last-child { color: #9aa8bd; font-size: 12px; }
.settings-page { min-height: 100%; }.settings-page :deep(.config-container), .settings-page :deep(.page-container), .settings-page :deep(.stock-basic-container), .settings-page :deep(.account-management-page) { max-width: none !important; padding: 0 !important; }.settings-page :deep(.config-header), .settings-page :deep(.page-header), .settings-page :deep(.stock-basic-container > .stock-basic-header) { padding: 0 0 20px !important; }.settings-page :deep(.stock-basic-container > .stock-basic-header) { display: flex; align-items: center; justify-content: space-between; gap: 18px; }.settings-page :deep(.stock-basic-container > .stock-basic-header h3) { margin: 0; color: #182336; font-size: 24px; font-weight: 700; letter-spacing: -.03em; }.settings-page :deep(.config-header h2::before), .settings-page :deep(.config-header h3::before), .settings-page :deep(.page-title::before), .settings-page :deep(.stock-basic-container > .stock-basic-header h3::before) { display: none; }.settings-page :deep(.table-container) { overflow: auto; }.settings-page :deep(.market-review-page), .settings-page :deep(.analysis-page), .settings-page :deep(.chat-container) { height: auto !important; min-height: 560px; }
</style>
