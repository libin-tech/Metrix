<template>
  <a-drawer
    :open="open"
    :width="'min(1240px, 100vw)'"
    :body-style="{padding: 0, background: 'var(--workspace-canvas)'}"
    placement="right"
    class="settings-drawer"
    @update:open="$emit('update:open', $event)"
  >
    <template #title>
      <div class="drawer-title">
        <span class="drawer-title-icon"><SettingOutlined /></span>
        <span>{{ $t('settingsHub.title') }}</span>
        <template v-if="selectedEntry"><span class="title-divider" aria-hidden="true">/</span><span class="title-current">{{ selectedEntry.title }}</span></template>
      </div>
    </template>

    <section v-if="!selectedEntry" class="settings-hub">
      <div class="hub-intro"><p>{{ $t('settingsHub.overview') }}</p><span>{{ $t('settingsHub.description') }}</span></div>
      <template v-for="section in entrySections" :key="section.key">
        <section v-if="section.items.length" class="settings-section" :aria-label="section.title">
          <div class="section-heading"><h3>{{ section.title }}</h3><span aria-hidden="true">{{ section.items.length }}</span></div>
          <div class="settings-entry-grid">
            <button v-for="entry in section.items" :key="entry.key" type="button" class="settings-entry" :class="`entry-${entry.section}`" @click="selectedKey = entry.key">
              <span class="entry-icon"><component :is="entry.icon" /></span>
              <span class="entry-copy"><strong>{{ entry.title }}</strong><small>{{ entry.description }}</small></span>
              <RightOutlined class="entry-arrow" />
            </button>
          </div>
        </section>
      </template>
      <a-empty v-if="!entries.length" :description="$t('settingsHub.noAccess')" />
    </section>

    <div v-else class="settings-layout">
      <nav class="settings-nav" :aria-label="$t('settingsHub.navigation')">
        <button type="button" class="nav-overview" @click="selectedKey = null"><ArrowLeftOutlined /><span>{{ $t('settingsHub.overview') }}</span></button>
        <template v-for="section in entrySections" :key="section.key">
          <div v-if="section.items.length" class="nav-section">
            <p>{{ section.title }}</p>
            <button v-for="entry in section.items" :key="entry.key" type="button" :class="{selected: selectedKey === entry.key}" :aria-current="selectedKey === entry.key ? 'page' : undefined" @click="selectedKey = entry.key">
              <component :is="entry.icon" /><span>{{ entry.title }}</span>
            </button>
          </div>
        </template>
      </nav>
      <section class="settings-page" :aria-label="selectedEntry.title">
        <p class="settings-page-description">{{ selectedEntry.description }}</p>
        <component :is="selectedEntry.component" :key="selectedEntry.key" />
      </section>
    </div>
  </a-drawer>
</template>

<script setup>
import {computed, defineAsyncComponent, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {useRoute} from 'vue-router'
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
  SettingOutlined,
  TeamOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'

const emit = defineEmits(['update:open'])
const props = defineProps({
  open: {type: Boolean, default: false},
  permissions: {type: Array, default: () => []}
})

const {t} = useI18n()
const route = useRoute()
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

watch(() => route.fullPath, () => emit('update:open', false))

watch(() => props.open, isOpen => {
  if (!isOpen) selectedKey.value = null
})
</script>

<style scoped>
.drawer-title { display: flex; align-items: center; gap: 10px; color: var(--theme-text, #263957); font-size: 16px; font-weight: 700; }
.drawer-title-icon { display: grid; place-items: center; width: 32px; height: 32px; color: var(--theme-blue, #5278bb); background: var(--theme-raised, #edf3fc); border-radius: 9px; }
.title-divider { color: var(--theme-text, #ccd4df); font-weight: 400; }
.title-current { color: var(--theme-muted, #7b899e); font-size: 13px; font-weight: 500; }
.settings-hub { max-width: 1080px; margin: 0 auto; padding: 30px; }
.hub-intro { margin-bottom: 28px; }
.hub-intro p { margin: 0 0 7px; color: var(--theme-text, #263957); font-size: 23px; font-weight: 700; letter-spacing: -.025em; }
.hub-intro span { color: var(--theme-muted, #7b899e); font-size: 13px; line-height: 1.7; }
.settings-section + .settings-section { margin-top: 28px; }
.section-heading { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.section-heading h3 { margin: 0; color: var(--theme-muted, #62728a); font-size: 12px; font-weight: 600; }
.section-heading > span { padding: 1px 6px; color: var(--theme-muted, #8997aa); font-size: 10px; background: var(--theme-raised, #e9edf4); border-radius: 5px; }
.settings-entry-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.settings-entry { display: flex; align-items: center; gap: 12px; min-width: 0; min-height: 100px; padding: 18px 15px; text-align: left; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e2e8f1); border-radius: 12px; cursor: pointer; transition: border-color .15s ease, box-shadow .15s ease; }
.entry-icon { display: grid; place-items: center; flex: 0 0 38px; height: 38px; color: var(--theme-blue, #5278bb); font-size: 19px; background: var(--theme-raised, #edf3fc); border-radius: 10px; }
.entry-management .entry-icon { color: var(--theme-muted, #78709f); background: var(--theme-raised, #f1eff8); }
.entry-copy { flex: 1; min-width: 0; }
.entry-copy strong { display: block; color: var(--theme-text, #263957); font-size: 14px; font-weight: 600; }
.entry-copy small { display: block; margin-top: 5px; color: var(--theme-muted, #8190a5); font-size: 11px; line-height: 1.6; }
.entry-arrow { flex-shrink: 0; color: var(--theme-muted, #aab6c7); font-size: 10px; }
.settings-entry:hover { border-color: var(--theme-line, #b8cbea); box-shadow: 0 4px 14px #26395708; }
.settings-entry:focus-visible, .settings-nav button:focus-visible { outline: 2px solid var(--theme-line, #638fcf); outline-offset: 3px; }
.settings-layout { display: grid; grid-template-columns: 172px minmax(0, 1fr); align-items: start; min-height: 100%; }
.settings-nav { position: sticky; top: 0; max-height: calc(100dvh - 88px); overflow-y: auto; padding: 20px 12px; scrollbar-width: thin; }
.settings-nav button { display: flex; align-items: center; gap: 10px; width: 100%; padding: 10px 12px; color: var(--theme-muted, #68788f); font: inherit; font-size: 13px; text-align: left; border: 0; border-radius: 8px; background: transparent; cursor: pointer; }
.settings-nav button:hover { background: var(--theme-raised, #eaf0f8); }
.settings-nav button.selected { color: var(--theme-blue, #3766a8); font-weight: 600; background: var(--theme-raised, #e4edf9); }
.settings-nav .nav-overview { margin-bottom: 20px; font-size: 12px; }
.nav-section + .nav-section { margin-top: 22px; }
.nav-section p { margin: 0 12px 8px; color: var(--theme-muted, #94a0b2); font-size: 10px; font-weight: 600; }
.settings-page { min-width: 0; min-height: calc(100dvh - 118px); margin: 20px 20px 20px 0; padding: 24px; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e2e8f1); border-radius: 12px; }
.settings-page-description { margin: 0 0 16px; color: var(--theme-muted, #8996aa); font-size: 12px; line-height: 1.6; }
.settings-page :deep(.config-container), .settings-page :deep(.page-container), .settings-page :deep(.stock-basic-container), .settings-page :deep(.account-management-page) { max-width: none !important; padding: 0 !important; }
.settings-page :deep(.config-header), .settings-page :deep(.page-header), .settings-page :deep(.stock-basic-header) { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; margin-bottom: 18px; padding: 0 0 16px !important; border-bottom: 1px solid var(--theme-line, #edf0f5); }
.settings-page :deep(.config-header h2), .settings-page :deep(.config-header h3), .settings-page :deep(.page-title), .settings-page :deep(.stock-basic-header h3) { margin: 0; color: var(--theme-text, #263957); font-size: 19px; font-weight: 600; letter-spacing: -.02em; }
.settings-page :deep(.header-actions) { flex-wrap: wrap; min-width: 0; max-width: 100%; gap: 8px; }
.settings-page :deep(.header-actions .ant-input-search) { max-width: 100%; }
.settings-page :deep(.config-header h2::before), .settings-page :deep(.config-header h3::before), .settings-page :deep(.page-title::before) { display: none; }
.settings-page :deep(.ant-table-wrapper), .settings-page :deep(.table-container) { min-width: 0; max-width: 100%; }
.settings-page :deep(.ant-table-content) { overflow-x: auto; scrollbar-width: thin; }
.settings-page :deep(.ant-table table) { min-width: 680px; }
.settings-page :deep(.ant-table-thead > tr > th) { padding: 12px; color: var(--theme-muted, #68788f); font-size: 12px; font-weight: 600; background: var(--theme-raised, #f7f9fc); }
.settings-page :deep(.ant-table-tbody > tr > td) { padding: 12px; font-size: 12px; }
.settings-page :deep(.ant-table-tbody > tr:hover > td) { background: var(--theme-surface, #f7faff); }
.settings-page :deep(.ant-btn) { border-radius: 7px; }
.settings-page :deep(.ant-pagination) { flex-wrap: wrap; row-gap: 8px; }
@media (max-width: 960px) {
  .settings-entry-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .settings-page { padding: 18px; margin-right: 14px; }
}
@media (max-width: 640px) {
  .settings-hub { padding: 22px 16px; }
  .settings-entry-grid { grid-template-columns: 1fr; gap: 9px; }
  .settings-entry { min-height: 82px; padding: 14px; }
  .settings-layout { display: block; }
  .settings-nav { display: flex; gap: 6px; position: sticky; z-index: 2; max-height: none; padding: 10px 12px; overflow-x: auto; background: var(--theme-raised, #f5f7fb); border-bottom: 1px solid var(--theme-line, #e2e8f1); }
  .settings-nav .nav-overview { margin: 0; }
  .settings-nav button { flex-shrink: 0; width: auto; padding: 9px 10px; white-space: nowrap; font-size: 12px; }
  .nav-section { display: flex; gap: 4px; }
  .nav-section + .nav-section { margin: 0; padding-left: 6px; border-left: 1px solid var(--theme-line, #dce3ed); }
  .nav-section p { display: none; }
  .settings-page { min-height: 0; margin: 12px; padding: 16px; }
}
@media (prefers-reduced-motion: reduce) { .settings-entry { transition: none; } }
</style>
