<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title"><TeamOutlined /> {{ $t('admin.userManagement') }}</h3>
      </div>
    </div>

    <div class="table-container">
      <a-table
        :dataSource="users"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :pagination="{ current: page, pageSize: size, total, showSizeChanger: true, showTotal: t => $t('admin.total', { count: t }) }"
        @change="handleTableChange"
        :locale="{ emptyText: $t('admin.noData') }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'FROZEN' ? 'red' : 'green'">
              {{ record.status === 'FROZEN' ? $t('admin.frozen') : $t('admin.normal') }}
            </a-tag>
          </template>
          <template v-if="column.key === 'createTime'">
            <span>{{ formatTime(record.createTime) }}</span>
          </template>
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openRoleModal(record)">
                <SafetyOutlined /> {{ $t('admin.assignRole') }}
              </a-button>
              <a-button v-if="record.status !== 'FROZEN'" type="link" danger size="small" @click="handleFreeze(record)">
                <LockOutlined /> {{ $t('admin.freeze') }}
              </a-button>
              <a-button v-else type="link" size="small" @click="handleUnfreeze(record)">
                <UnlockOutlined /> {{ $t('admin.unfreeze') }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="showRoleModal"
      :title="$t('admin.assignRole')"
      @ok="handleAssignRole"
      :confirm-loading="submitting"
    >
      <a-spin :spinning="loadingRoles">
        <a-checkbox-group v-model:value="selectedRoleIds">
          <a-row>
            <a-col v-for="role in allRoles" :key="role.id" :span="12" style="margin-bottom: 8px">
              <a-checkbox :value="role.id">{{ role.roleName }}</a-checkbox>
            </a-col>
          </a-row>
        </a-checkbox-group>
      </a-spin>
    </a-modal>

    <a-modal
      v-model:open="showFreezeModal"
      :title="$t('admin.freeze')"
      @ok="handleConfirmFreeze"
      :confirm-loading="submitting"
    >
      <a-form layout="vertical">
        <a-form-item :label="$t('admin.freezeReason')">
          <a-textarea v-model:value="freezeReason" :rows="3" :placeholder="$t('admin.freezeReasonPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {message} from 'ant-design-vue'
import {LockOutlined, SafetyOutlined, TeamOutlined, UnlockOutlined} from '@ant-design/icons-vue'
import {assignUserRoles, freezeUser, getAdminUsers, getAllRoles, getUserRoles, unfreezeUser} from '../../api'

const { t } = useI18n()

const users = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const showRoleModal = ref(false)
const submitting = ref(false)
const currentUser = ref(null)
const allRoles = ref([])
const selectedRoleIds = ref([])
const loadingRoles = ref(false)

const showFreezeModal = ref(false)
const freezeUserId = ref(null)
const freezeReason = ref('')

const columns = computed(() => [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: t('admin.username'), dataIndex: 'username', key: 'username', width: 150 },
  { title: t('admin.nickname'), dataIndex: 'nickname', key: 'nickname', width: 150 },
  { title: t('admin.role'), dataIndex: 'roleNames', key: 'roleNames', width: 120 },
  { title: t('admin.status'), dataIndex: 'status', key: 'status', width: 100 },
  { title: t('admin.createTime'), dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: t('admin.actions'), key: 'actions', width: 240, align: 'center' }
])

const formatTime = (val) => {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getAdminUsers(page.value, size.value, keyword.value)
    const data = res.data || {}
    const records = data.records || []
    const roleNames = data.userRoleNames || {}
    users.value = records.map(u => ({
      ...u,
      roleNames: (roleNames[u.id] || []).join(', ') || '-'
    }))
    total.value = data.total || 0
  } catch {
    users.value = []
    total.value = 0
    message.error(t('admin.loadFailed'))
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  page.value = pag.current
  size.value = pag.pageSize
  loadUsers()
}

const openRoleModal = async (record) => {
  currentUser.value = record
  showRoleModal.value = true
  loadingRoles.value = true
  try {
    const rolesRes = await getAllRoles()
    allRoles.value = rolesRes.data || []
    const userRolesRes = await getUserRoles(record.id)
    selectedRoleIds.value = userRolesRes.data || []
  } catch {
    message.error(t('admin.loadFailed'))
  } finally {
    loadingRoles.value = false
  }
}

const handleAssignRole = async () => {
  submitting.value = true
  try {
    await assignUserRoles(currentUser.value.id, { roleIds: selectedRoleIds.value })
    message.success(t('admin.assignRoleSuccess'))
    showRoleModal.value = false
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.assignRoleFailed'))
  } finally {
    submitting.value = false
  }
}

const handleFreeze = (record) => {
  freezeUserId.value = record.id
  freezeReason.value = ''
  showFreezeModal.value = true
}

const handleConfirmFreeze = async () => {
  submitting.value = true
  try {
    await freezeUser(freezeUserId.value, { freezeReason: freezeReason.value })
    message.success(t('admin.freezeSuccess'))
    showFreezeModal.value = false
    await loadUsers()
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.freezeFailed'))
  } finally {
    submitting.value = false
  }
}

const handleUnfreeze = async (record) => {
  try {
    await unfreezeUser(record.id)
    message.success(t('admin.unfreezeSuccess'))
    await loadUsers()
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.unfreezeFailed'))
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.page-container { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 12px; padding: 20px 24px;
  margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.page-title { margin: 0; font-size: 18px; font-weight: 600; color: #1a1a2e; }
.page-title .anticon { margin-right: 8px; color: #1890ff; }
.table-container {
  background: #fff; border-radius: 12px; padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
</style>
