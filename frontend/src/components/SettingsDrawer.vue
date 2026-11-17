<template>
  <a-drawer
    :open="open"
    :width="'min(1240px, 100vw)'"
    :body-style="{padding: 0}"
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
      <div class="hub-intro"><h2 class="workspace-title">{{ $t('settingsHub.overview') }}</h2><span>{{ $t('settingsHub.description') }}</span></div>
      <template v-for="section in entrySections" :key="section.key">
        <section v-if="section.items.length" class="settings-section" :aria-label="section.title">
          <h3 class="section-heading">{{ section.title }}</h3>
          <div class="settings-entry-grid">
            <button v-for="entry in section.items" :key="entry.key" type="button" class="settings-entry" @click="selectedKey = entry.key">
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
.drawer-title { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; color: var(--workspace-ink); font-size: 16px; font-weight: 700; }
.drawer-title-icon, .entry-icon { display: grid; place-items: center; flex: none; color: var(--workspace-accent); background: var(--workspace-soft-accent); border-radius: var(--workspace-control-radius); }
.drawer-title-icon { width: 32px; height: 32px; }
.title-divider, .title-current { color: var(--workspace-muted); font-size: 13px; font-weight: 500; }
.settings-hub { max-width: 1080px; margin: 0 auto; padding: clamp(16px, 3vw, 32px); }
.hub-intro { display: grid; gap: 6px; margin-bottom: 24px; }
.hub-intro span { color: var(--workspace-muted); font-size: 14px; line-height: 1.6; }
.settings-section + .settings-section { margin-top: 25px; }
.section-heading { margin: 0 0 10px; color: var(--workspace-muted); font-size: 13px; font-weight: 650; }
.settings-entry-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 270px), 1fr)); gap: 11px; }
.settings-entry {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
  min-height: 82px;
  padding: 14px;
  color: var(--workspace-ink);
  background: var(--workspace-surface);
  border: 1px solid var(--workspace-line);
  border-radius: var(--workspace-radius);
  box-shadow: var(--workspace-shadow);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.settings-entry:hover { border-color: var(--workspace-accent); }
.entry-icon { width: 38px; height: 38px; font-size: 17px; }
.entry-copy { flex: 1; min-width: 0; }
.entry-copy strong, .entry-copy small { display: block; overflow-wrap: anywhere; }
.entry-copy strong { font-size: 14px; font-weight: 650; }
.entry-copy small { margin-top: 4px; color: var(--workspace-muted); font-size: 12px; line-height: 1.45; }
.entry-arrow { flex: none; color: var(--workspace-accent); font-size: 11px; }

.settings-layout { display: grid; grid-template-columns: minmax(170px, 200px) minmax(0, 1fr); align-items: start; min-height: 100%; }
.settings-nav { position: sticky; top: 0; max-height: calc(100dvh - 75px); padding: 16px 12px; overflow-y: auto; scrollbar-width: thin; }
.settings-nav button {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  color: var(--workspace-muted);
  background: transparent;
  border: 0;
  border-radius: var(--workspace-control-radius);
  font: inherit;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.settings-nav button:hover, .settings-nav button.selected { color: var(--workspace-accent); background: var(--workspace-soft-accent); }
.settings-nav button.selected { font-weight: 680; }
.settings-nav button:focus-visible { outline-offset: -2px; }
.settings-nav .nav-overview { margin-bottom: 15px; }
.nav-section + .nav-section { margin-top: 16px; }
.nav-section p { margin: 0 10px 4px; color: var(--workspace-muted); font-size: 12px; font-weight: 620; }

.settings-page { min-width: 0; margin: 16px 16px 16px 0; padding: clamp(13px, 2vw, 22px); background: var(--workspace-surface); border: 1px solid var(--workspace-line); border-radius: var(--workspace-radius); box-shadow: var(--workspace-shadow); }
.settings-page-description { margin: 0 0 13px; color: var(--workspace-muted); font-size: 13px; line-height: 1.6; }
.settings-page :deep(.ant-table-content) { scrollbar-width: thin; }

@media (max-width: 650px) {
  .settings-layout { display: block; }
  .settings-nav { display: flex; gap: 6px; z-index: 2; max-height: none; padding: 8px; overflow-x: auto; background: var(--workspace-surface); border-bottom: 1px solid var(--workspace-line); }
  .settings-nav .nav-overview { margin: 0; }
  .settings-nav button { flex: none; width: auto; padding: 8px; white-space: nowrap; }
  .nav-section { display: flex; gap: 4px; }
  .nav-section + .nav-section { margin: 0; padding-left: 6px; border-left: 1px solid var(--workspace-line); }
  .nav-section p { display: none; }
  .settings-page { margin: 10px; }
}
</style>
