<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title"><MenuOutlined /> {{ $t('admin.menuManagement') }}</h3>
      </div>
      <div class="header-actions">
        <a-button type="primary" @click="openAddModal">
          <PlusOutlined /> {{ $t('admin.addMenu') }}
        </a-button>
      </div>
    </div>

    <a-row :gutter="16">
      <a-col :span="8">
        <div class="table-container">
          <div style="margin-bottom: 12px; font-weight: 600; color: #333;">{{ $t('admin.menuTree') }}</div>
          <a-spin :spinning="loadingTree">
            <a-tree
              :tree-data="displayTree"
              :replace-fields="{ title: 'menuName', key: 'id', children: 'children' }"
              :selected-keys="[selectedMenuId]"
              @select="onSelectMenu"
              default-expand-all
            >
              <template #title="{ menuName }">
                <span>{{ menuName || '---' }}</span>
              </template>
            </a-tree>
          </a-spin>
        </div>
      </a-col>
      <a-col :span="16">
        <div class="table-container">
          <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 600; color: #333;">
              {{ selectedMenu ? selectedMenu.menuName : $t('admin.selectMenuHint') }}
            </span>
            <a-space v-if="selectedMenu">
              <a-button size="small" @click="openEditModal(selectedMenu)">
                <EditOutlined /> {{ $t('admin.edit') }}
              </a-button>
              <a-button size="small" @click="openApiDrawer(selectedMenu)">
                <LinkOutlined /> {{ $t('admin.linkApis') }}
              </a-button>
              <a-popconfirm :title="$t('admin.confirmDelete')" @confirm="handleDelete(selectedMenu.id)">
                <a-button size="small" type="text" danger>
                  <DeleteOutlined /> {{ $t('admin.delete') }}
                </a-button>
              </a-popconfirm>
            </a-space>
          </div>
          <div v-if="selectedMenu">
            <a-descriptions :column="2" size="small" bordered>
              <a-descriptions-item :label="$t('admin.menuName')" :span="2">{{ selectedMenu.menuName }}</a-descriptions-item>
              <a-descriptions-item :label="$t('admin.menuType')">
                <a-tag>{{ selectedMenu.menuType }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item :label="$t('admin.permissionCode')">{{ selectedMenu.permissionCode || '-' }}</a-descriptions-item>
              <a-descriptions-item :label="$t('admin.path')" :span="2">{{ selectedMenu.path || '-' }}</a-descriptions-item>
              <a-descriptions-item :label="$t('admin.component')" :span="2">{{ selectedMenu.component || '-' }}</a-descriptions-item>
              <a-descriptions-item :label="$t('admin.icon')"><component :is="resolveIcon(selectedMenu.icon)" v-if="selectedMenu.icon" style="margin-right: 6px;" />{{ selectedMenu.icon || '-' }}</a-descriptions-item>
              <a-descriptions-item :label="$t('admin.sortOrder')">{{ selectedMenu.sortOrder }}</a-descriptions-item>
              <a-descriptions-item :label="$t('admin.status')">
                <a-tag :color="selectedMenu.status === 'ACTIVE' ? 'green' : 'red'">
                  {{ selectedMenu.status === 'ACTIVE' ? $t('admin.active') : $t('admin.disabled') }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item :label="$t('admin.visible')">{{ selectedMenu.visible ? $t('admin.yes') : $t('admin.no') }}</a-descriptions-item>
              <a-descriptions-item :label="$t('admin.linkedApis')" :span="2">{{ selectedMenu.apiIds?.length || 0 }}{{ $t('admin.itemsCount') }}</a-descriptions-item>
            </a-descriptions>
          </div>
          <div v-else style="text-align: center; padding: 60px 0; color: #999;">
            {{ $t('admin.selectMenuHint') }}
          </div>
        </div>
      </a-col>
    </a-row>

    <a-modal
      v-model:open="showModal"
      :title="isEditing ? $t('admin.editMenu') : $t('admin.addMenu')"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      @cancel="resetForm"
      :width="560"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item :label="$t('admin.menuType')" required>
          <a-select v-model:value="form.menuType">
            <a-select-option value="DIRECTORY">{{ $t('admin.directory') }}</a-select-option>
            <a-select-option value="MENU">{{ $t('admin.menu') }}</a-select-option>
            <a-select-option value="BUTTON">{{ $t('admin.button') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('admin.parentMenu')">
           <a-tree-select
              v-model:value="form.parentId"
              :tree-data="menuTreeForSelect"
              allow-clear
              :placeholder="$t('admin.parentMenuPlaceholder')"
              style="width: 100%"
              tree-default-expand-all
            />
        </a-form-item>
        <a-form-item :label="$t('admin.menuName')" required>
          <a-input v-model:value="form.menuName" :placeholder="$t('admin.menuNamePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('admin.permissionCode')">
          <a-input v-model:value="form.permissionCode" :placeholder="$t('admin.permissionCodePlaceholder')" />
        </a-form-item>
        <a-form-item v-if="form.menuType !== 'BUTTON'" :label="$t('admin.path')">
          <a-input v-model:value="form.path" :placeholder="$t('admin.pathPlaceholder')" />
        </a-form-item>
        <a-form-item v-if="form.menuType !== 'BUTTON'" :label="$t('admin.component')">
          <a-input v-model:value="form.component" :placeholder="$t('admin.componentPlaceholder')" />
        </a-form-item>
        <a-form-item v-if="form.menuType !== 'BUTTON'" :label="$t('admin.icon')">
          <a-input v-model:value="form.icon" readonly :placeholder="$t('admin.iconPlaceholder')" @click="showIconSelector = true">
            <template #prefix><component :is="resolveIcon(form.icon)" v-if="form.icon" /></template>
          </a-input>
        </a-form-item>
        <IconSelector :visible="showIconSelector" :selected="form.icon" @select="onIconSelect" @close="showIconSelector = false" />
        <a-form-item :label="$t('admin.sortOrder')">
          <a-input-number v-model:value="form.sortOrder" :min="0" />
        </a-form-item>
        <a-form-item v-if="form.menuType !== 'BUTTON'" :label="$t('admin.visible')">
          <a-switch v-model:checked="form.visible" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="showApiDrawer"
      :title="$t('admin.linkApis')"
      :width="480"
    >
      <a-spin :spinning="loadingApis">
        <a-collapse v-model:activeKey="expandedApiGroups" ghost>
          <a-collapse-panel v-for="(group, groupKey) in groupedApis" :key="groupKey" :header="group.label + ' (' + group.items.length + ')'">
            <a-checkbox-group v-model:value="selectedApiIds" style="width: 100%">
              <div v-for="api in group.items" :key="api.id" style="padding: 4px 0;">
                <a-checkbox :value="api.id">
                  <span style="font-weight: 500;">{{ api.apiName }}</span>
                  <span style="color: #999; margin-left: 8px; font-size: 12px;">[{{ api.httpMethod }}] {{ api.apiPath }}</span>
                </a-checkbox>
              </div>
            </a-checkbox-group>
          </a-collapse-panel>
        </a-collapse>
      </a-spin>
      <template #footer>
        <a-button @click="showApiDrawer = false" style="margin-right: 8px">{{ $t('admin.cancel') }}</a-button>
        <a-button type="primary" :loading="submittingApi" @click="handleSaveApis">
          {{ $t('admin.save') }}
        </a-button>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  MenuOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  LinkOutlined
} from '@ant-design/icons-vue'
import {
  getMenuTree,
  getMenuById,
  createMenu,
  updateMenu,
  deleteMenu,
  getAllApis,
  getMenuApis,
  assignMenuApis
} from '../../api'
import IconSelector from '../../components/IconSelector.vue'
import { resolveIcon } from '../../composables/iconMap'

const { t } = useI18n()

const rawTree = ref([])
const displayTree = ref([])
const loadingTree = ref(false)
const selectedMenuId = ref(null)
const selectedMenu = ref(null)

const showModal = ref(false)
const showIconSelector = ref(false)
const submitting = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const form = reactive({
  menuType: 'MENU',
  parentId: null,
  menuName: '',
  permissionCode: '',
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  visible: true
})

const showApiDrawer = ref(false)
const submittingApi = ref(false)
const loadingApis = ref(false)
const allApis = ref([])
const selectedApiIds = ref([])
const currentMenuIdForApi = ref(null)
const expandedApiGroups = ref([])

const groupedApis = computed(() => {
  const groupLabels = {
    role: '角色管理', menu: '菜单管理', api: '接口管理',
    user: '用户管理', stats: '数据统计', audit: '审计日志'
  }
  const map = {}
  for (const api of allApis.value) {
    const key = api.permission_code?.split(':')[1] || 'other'
    if (!map[key]) map[key] = { label: groupLabels[key] || '其他', items: [] }
    map[key].items.push(api)
  }
  return Object.values(map).filter(g => g.items.length > 0)
})

const filterButtons = (nodes) => {
  if (!nodes) return []
  return nodes
    .filter(n => n.menuType !== 'BUTTON')
    .map(n => ({ ...n, children: filterButtons(n.children) }))
}

const toTreeSelectData = (nodes) => {
  if (!nodes) return []
  return nodes.map(n => ({
    title: n.menuName,
    value: n.id,
    key: String(n.id),
    children: toTreeSelectData(n.children)
  }))
}

const menuTreeForSelect = computed(() => {
  return [{
    title: t('admin.rootNode'),
    value: null,
    key: '__root__',
    children: toTreeSelectData(filterButtons(displayTree.value))
  }]
})

const loadMenuTree = async () => {
  loadingTree.value = true
  try {
    const res = await getMenuTree()
    rawTree.value = res.data?.children || []
    displayTree.value = rawTree.value
  } catch {
    message.error(t('admin.loadFailed'))
  } finally {
    loadingTree.value = false
  }
}

const flattenTree = (nodes) => {
  const result = []
  const walk = (list) => {
    for (const node of list) {
      result.push(node)
      if (node.children && node.children.length > 0) walk(node.children)
    }
  }
  walk(nodes)
  return result
}

const onSelectMenu = async (keys) => {
  if (keys.length === 0) {
    selectedMenuId.value = null
    selectedMenu.value = null
    return
  }
  selectedMenuId.value = keys[0]
  const flat = flattenTree(rawTree.value)
  selectedMenu.value = flat.find(n => n.id === keys[0]) || null
}

const resetForm = () => {
  form.menuType = 'MENU'
  form.parentId = null
  form.menuName = ''
  form.permissionCode = ''
  form.path = ''
  form.component = ''
  form.icon = ''
  form.sortOrder = 0
  form.visible = true
  isEditing.value = false
  editingId.value = null
}

const onIconSelect = (name) => {
  form.icon = name
  showIconSelector.value = false
}

const openAddModal = () => {
  resetForm()
  if (selectedMenu.value && selectedMenu.value.menuType !== 'BUTTON') {
    form.parentId = selectedMenu.value.id
  }
  showModal.value = true
}

const openEditModal = async (record) => {
  isEditing.value = true
  editingId.value = record.id
  try {
    const res = await getMenuById(record.id)
    const menu = res.data
    form.menuType = menu.menuType
    form.parentId = menu.parentId
    form.menuName = menu.menuName
    form.permissionCode = menu.permissionCode || ''
    form.path = menu.path || ''
    form.component = menu.component || ''
    form.icon = menu.icon || ''
    form.sortOrder = menu.sortOrder || 0
    form.visible = menu.visible !== false
  } catch {
    message.error(t('admin.loadFailed'))
    return
  }
  showModal.value = true
}

const handleSubmit = async () => {
  if (!form.menuName.trim()) {
    message.warning(t('admin.fillRequired'))
    return
  }
  submitting.value = true
  try {
    const payload = {
      menuType: form.menuType,
      parentId: form.parentId || null,
      menuName: form.menuName.trim(),
      permissionCode: form.permissionCode.trim() || null,
      path: form.path.trim() || null,
      component: form.component.trim() || null,
      icon: form.icon.trim() || null,
      sortOrder: form.sortOrder || 0,
      visible: form.menuType === 'BUTTON' ? false : form.visible
    }
    if (isEditing.value) {
      payload.status = 'ACTIVE'
      await updateMenu(editingId.value, payload)
      message.success(t('admin.updateSuccess'))
    } else {
      await createMenu(payload)
      message.success(t('admin.addSuccess'))
    }
    showModal.value = false
    resetForm()
    await loadMenuTree()
    selectedMenu.value = null
    selectedMenuId.value = null
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.saveFailed'))
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await deleteMenu(id)
    message.success(t('admin.deleteSuccess'))
    selectedMenu.value = null
    selectedMenuId.value = null
    await loadMenuTree()
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.deleteFailed'))
  }
}

const openApiDrawer = async (record) => {
  currentMenuIdForApi.value = record.id
  showApiDrawer.value = true
  loadingApis.value = true
  try {
    const [apisRes, menuApisRes] = await Promise.all([
      getAllApis(),
      getMenuApis(record.id)
    ])
    allApis.value = apisRes.data || []
    selectedApiIds.value = menuApisRes.data || []
    expandedApiGroups.value = groupedApis.value.map((_, i) => i)
  } catch {
    message.error(t('admin.loadFailed'))
  } finally {
    loadingApis.value = false
  }
}

const handleSaveApis = async () => {
  submittingApi.value = true
  try {
    await assignMenuApis(currentMenuIdForApi.value, { apiIds: selectedApiIds.value })
    message.success(t('admin.linkApiSuccess'))
    showApiDrawer.value = false
    await loadMenuTree()
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.saveFailed'))
  } finally {
    submittingApi.value = false
  }
}

onMounted(() => {
  loadMenuTree()
})
</script>

<style scoped>
.page-container { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 12px; padding: 20px 24px;
  margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.header-actions { display: flex; gap: 8px; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; color: #1a1a2e; }
.page-title .anticon { margin-right: 8px; color: #1890ff; }
.table-container {
  background: #fff; border-radius: 12px; padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06); min-height: 400px;
}

:deep(.ant-tree-treenode) {
  display: flex;
  align-items: flex-start;
}
:deep(.ant-tree-switcher) {
  flex-shrink: 0;
  line-height: 22px;
}
:deep(.ant-tree-node-content-wrapper) {
  display: inline-flex;
  align-items: baseline;
  flex: 1;
  min-width: 0;
}
:deep(.ant-tree-iconEle) {
  flex-shrink: 0;
  margin-right: 4px;
}
:deep(.ant-tree-title) {
  display: inline;
  white-space: nowrap;
}
</style>
