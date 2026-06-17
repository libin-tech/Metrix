<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title"><SafetyCertificateOutlined /> {{ $t('admin.roleManagement') }}</h3>
      </div>
      <div class="header-actions">
        <a-button type="primary" @click="openAddModal">
          <PlusOutlined /> {{ $t('admin.addRole') }}
        </a-button>
      </div>
    </div>

    <div class="table-container">
      <a-table
        :dataSource="roles"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :pagination="{ current: page, pageSize: size, total, showSizeChanger: true, showTotal: t => $t('admin.total', { count: t }) }"
        @change="handleTableChange"
        :locale="{ emptyText: $t('admin.noData') }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'red'">
              {{ record.status === 'ACTIVE' ? $t('admin.active') : $t('admin.disabled') }}
            </a-tag>
          </template>
          <template v-if="column.key === 'isSystem'">
            <a-tag v-if="record.isSystem" color="blue">{{ $t('admin.systemRole') }}</a-tag>
          </template>
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openPermissionDrawer(record)">
                <SafetyOutlined /> {{ $t('admin.setPermission') }}
              </a-button>
              <a-button type="link" size="small" @click="openEditModal(record)">
                <EditOutlined /> {{ $t('admin.edit') }}
              </a-button>
              <a-popconfirm
                :title="$t('admin.confirmDelete')"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="text" danger size="small">
                  <DeleteOutlined /> {{ $t('admin.delete') }}
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="showModal"
      :title="isEditing ? $t('admin.editRole') : $t('admin.addRole')"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      @cancel="resetForm"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item :label="$t('admin.roleCode')" required>
          <a-input v-model:value="form.roleCode" :disabled="isEditing" :placeholder="$t('admin.roleCodePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('admin.roleName')" required>
          <a-input v-model:value="form.roleName" :placeholder="$t('admin.roleNamePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('admin.description')">
          <a-textarea v-model:value="form.description" :rows="2" :placeholder="$t('admin.descriptionPlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('admin.sortOrder')">
          <a-input-number v-model:value="form.sortOrder" :min="0" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="showPermissionDrawer"
      :title="$t('admin.permissionSetting')"
      :width="520"
      @close="resetPermissionDrawer"
    >
      <a-tabs v-model:activeKey="permissionTab">
        <a-tab-pane key="menu" :tab="$t('admin.menuPermission')">
          <a-spin :spinning="loadingMenus">
            <div style="margin-bottom: 8px;">
              <a-space>
                <a-button size="small" @click="selectAllMenus">{{ $t('admin.selectAll') }}</a-button>
                <a-button size="small" @click="checkedMenuIds = []">{{ $t('admin.deselectAll') }}</a-button>
              </a-space>
            </div>
            <a-tree
              v-model:checkedKeys="checkedMenuIds"
              :tree-data="menuTreeData"
              checkable
              :replace-fields="{ title: 'menuName', key: 'id', children: 'children' }"
              default-expand-all
            />
          </a-spin>
        </a-tab-pane>
        <a-tab-pane key="api" :tab="$t('admin.apiPermission')">
          <a-spin :spinning="loadingApis">
            <div style="margin-bottom: 8px;">
              <a-space>
                <a-button size="small" @click="selectAllApis">{{ $t('admin.selectAll') }}</a-button>
                <a-button size="small" @click="checkedApiIds = []">{{ $t('admin.deselectAll') }}</a-button>
              </a-space>
            </div>
            <a-collapse v-model:activeKey="expandedApiGroups" ghost>
              <a-collapse-panel v-for="(group, groupKey) in groupedApis" :key="groupKey" :header="group.label + ' (' + group.items.length + ')'">
                <a-checkbox-group v-model:value="checkedApiIds">
                  <div v-for="api in group.items" :key="api.id" style="padding: 4px 0;">
                    <a-checkbox :value="api.id" style="width: 100%;">
                      <span style="font-weight: 500;">{{ api.apiName }}</span>
                      <span style="color: #999; margin-left: 8px; font-size: 12px;">[{{ api.httpMethod }}] {{ api.apiPath }}</span>
                    </a-checkbox>
                  </div>
                </a-checkbox-group>
              </a-collapse-panel>
            </a-collapse>
          </a-spin>
        </a-tab-pane>
      </a-tabs>
      <template #footer>
        <a-button @click="showPermissionDrawer = false" style="margin-right: 8px">{{ $t('admin.cancel') }}</a-button>
        <a-button type="primary" :loading="submittingMenu" @click="handleSaveMenus" style="margin-right: 8px">
          {{ $t('admin.saveMenu') }}
        </a-button>
        <a-button :loading="submittingApi" @click="handleSaveApis">
          {{ $t('admin.saveApi') }}
        </a-button>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {message} from 'ant-design-vue'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue'
import {
  assignRoleApis,
  assignRoleMenus,
  createRole,
  deleteRole,
  getAllApis,
  getMenuTree,
  getRoleDetail,
  getRolePage,
  updateRole
} from '../../api'

const { t } = useI18n()

const roles = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const showModal = ref(false)
const submitting = ref(false)
const isEditing = ref(false)
const editingId = ref(null)

const form = reactive({
  roleCode: '',
  roleName: '',
  description: '',
  sortOrder: 0
})

const showPermissionDrawer = ref(false)
const submittingMenu = ref(false)
const submittingApi = ref(false)
const permissionTab = ref('menu')
const currentRoleId = ref(null)

const menuTreeData = ref([])
const allApis = ref([])
const checkedMenuIds = ref([])
const checkedApiIds = ref([])
const loadingMenus = ref(false)
const loadingApis = ref(false)
const expandedApiGroups = ref([])

const collectAllMenuIds = (nodes) => {
  if (!nodes) return []
  return nodes.flatMap(n => [n.id, ...collectAllMenuIds(n.children)])
}

const selectAllMenus = () => {
  checkedMenuIds.value = collectAllMenuIds(menuTreeData.value)
}

const selectAllApis = () => {
  checkedApiIds.value = allApis.value.map(a => a.id)
}

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

const columns = computed(() => [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: t('admin.roleCode'), dataIndex: 'roleCode', key: 'roleCode', width: 120 },
  { title: t('admin.roleName'), dataIndex: 'roleName', key: 'roleName', width: 150 },
  { title: t('admin.description'), dataIndex: 'description', key: 'description', width: 200 },
  { title: t('admin.isSystem'), dataIndex: 'isSystem', key: 'isSystem', width: 100 },
  { title: t('admin.status'), dataIndex: 'status', key: 'status', width: 100 },
  { title: t('admin.actions'), key: 'actions', width: 320, align: 'center' }
])

const loadRoles = async () => {
  loading.value = true
  try {
    const res = await getRolePage(page.value, size.value, '')
    roles.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    roles.value = []
    total.value = 0
    message.error(t('admin.loadFailed'))
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  page.value = pag.current
  size.value = pag.pageSize
  loadRoles()
}

const resetForm = () => {
  form.roleCode = ''
  form.roleName = ''
  form.description = ''
  form.sortOrder = 0
  isEditing.value = false
  editingId.value = null
}

const openAddModal = () => {
  resetForm()
  showModal.value = true
}

const openEditModal = async (record) => {
  isEditing.value = true
  editingId.value = record.id
  form.roleCode = record.roleCode
  form.roleName = record.roleName
  form.description = record.description || ''
  form.sortOrder = record.sortOrder || 0
  showModal.value = true
}

const handleSubmit = async () => {
  if (!form.roleCode.trim() || !form.roleName.trim()) {
    message.warning(t('admin.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (isEditing.value) {
      const payload = {
        roleName: form.roleName.trim(),
        description: form.description.trim() || null,
        sortOrder: form.sortOrder
      }
      await updateRole(editingId.value, payload)
      message.success(t('admin.updateSuccess'))
    } else {
      await createRole({
        roleCode: form.roleCode.trim(),
        roleName: form.roleName.trim(),
        description: form.description.trim() || null,
        sortOrder: form.sortOrder
      })
      message.success(t('admin.addSuccess'))
    }
    showModal.value = false
    resetForm()
    await loadRoles()
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.saveFailed'))
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await deleteRole(id)
    message.success(t('admin.deleteSuccess'))
    await loadRoles()
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.deleteFailed'))
  }
}

const openPermissionDrawer = async (record) => {
  currentRoleId.value = record.id
  showPermissionDrawer.value = true
  loadingMenus.value = true
  loadingApis.value = true
  try {
    const [menuRes, apiRes, detailRes] = await Promise.all([
      getMenuTree(),
      getAllApis(),
      getRoleDetail(record.id)
    ])
    menuTreeData.value = menuRes.data?.children || []
    allApis.value = apiRes.data || []
    checkedMenuIds.value = detailRes.data?.menuIds || []
    checkedApiIds.value = detailRes.data?.apiIds || []
    expandedApiGroups.value = groupedApis.value.map((_, i) => i)
  } catch {
    message.error(t('admin.loadFailed'))
  } finally {
    loadingMenus.value = false
    loadingApis.value = false
  }
}

const resetPermissionDrawer = () => {
  checkedMenuIds.value = []
  checkedApiIds.value = []
  currentRoleId.value = null
}

const handleSaveMenus = async () => {
  if (checkedMenuIds.value.length === 0) {
    message.warning(t('admin.atLeastOneMenu'))
    return
  }
  submittingMenu.value = true
  try {
    await assignRoleMenus(currentRoleId.value, { menuIds: checkedMenuIds.value })
    message.success(t('admin.menuSaveSuccess'))
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.saveFailed'))
  } finally {
    submittingMenu.value = false
  }
}

const handleSaveApis = async () => {
  if (checkedApiIds.value.length === 0) {
    message.warning(t('admin.atLeastOneApi'))
    return
  }
  submittingApi.value = true
  try {
    await assignRoleApis(currentRoleId.value, { apiIds: checkedApiIds.value })
    message.success(t('admin.apiSaveSuccess'))
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.saveFailed'))
  } finally {
    submittingApi.value = false
  }
}

onMounted(() => {
  loadRoles()
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
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
</style>
