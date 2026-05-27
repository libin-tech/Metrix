<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title"><ApiOutlined /> {{ $t('admin.apiManagement') }}</h3>
      </div>
      <div class="header-actions">
        <a-button type="primary" @click="openAddModal">
          <PlusOutlined /> {{ $t('admin.addApi') }}
        </a-button>
      </div>
    </div>

    <div class="table-container">
      <a-table
        :dataSource="apis"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :pagination="{ current: page, pageSize: size, total, showSizeChanger: true, showTotal: t => $t('admin.total', { count: t }) }"
        @change="handleTableChange"
        :locale="{ emptyText: $t('admin.noData') }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'httpMethod'">
            <a-tag :color="methodColor(record.httpMethod)">{{ record.httpMethod }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'red'">
              {{ record.status === 'ACTIVE' ? $t('admin.active') : $t('admin.disabled') }}
            </a-tag>
          </template>
          <template v-if="column.key === 'actions'">
            <a-space>
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
      :title="isEditing ? $t('admin.editApi') : $t('admin.addApi')"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      @cancel="resetForm"
      :width="560"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item :label="$t('admin.apiName')" required>
          <a-input v-model:value="form.apiName" :placeholder="$t('admin.apiNamePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('admin.apiPath')" required>
          <a-input v-model:value="form.apiPath" :placeholder="$t('admin.apiPathPlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('admin.httpMethod')" required>
          <a-select v-model:value="form.httpMethod">
            <a-select-option value="GET">GET</a-select-option>
            <a-select-option value="POST">POST</a-select-option>
            <a-select-option value="PUT">PUT</a-select-option>
            <a-select-option value="DELETE">DELETE</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('admin.permissionCode')">
          <a-input v-model:value="form.permissionCode" :placeholder="$t('admin.permissionCodePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('admin.description')">
          <a-textarea v-model:value="form.description" :rows="2" :placeholder="$t('admin.descriptionPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  ApiOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import {
  getApiPage,
  getApiById,
  createApi,
  updateApi,
  deleteApi
} from '../../api'

const { t } = useI18n()

const apis = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const showModal = ref(false)
const submitting = ref(false)
const isEditing = ref(false)
const editingId = ref(null)

const form = reactive({
  apiName: '',
  apiPath: '',
  httpMethod: 'GET',
  permissionCode: '',
  description: ''
})

const methodColor = (method) => {
  const colors = { GET: 'green', POST: 'blue', PUT: 'orange', DELETE: 'red' }
  return colors[method] || 'default'
}

const columns = computed(() => [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: t('admin.apiName'), dataIndex: 'apiName', key: 'apiName', width: 150 },
  { title: t('admin.apiPath'), dataIndex: 'apiPath', key: 'apiPath', width: 250 },
  { title: t('admin.httpMethod'), dataIndex: 'httpMethod', key: 'httpMethod', width: 100 },
  { title: t('admin.permissionCode'), dataIndex: 'permissionCode', key: 'permissionCode', width: 150 },
  { title: t('admin.status'), dataIndex: 'status', key: 'status', width: 100 },
  { title: t('admin.actions'), key: 'actions', width: 160, align: 'center' }
])

const loadApis = async () => {
  loading.value = true
  try {
    const res = await getApiPage(page.value, size.value, '')
    apis.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    apis.value = []
    total.value = 0
    message.error(t('admin.loadFailed'))
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  page.value = pag.current
  size.value = pag.pageSize
  loadApis()
}

const resetForm = () => {
  form.apiName = ''
  form.apiPath = ''
  form.httpMethod = 'GET'
  form.permissionCode = ''
  form.description = ''
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
  form.apiName = record.apiName
  form.apiPath = record.apiPath
  form.httpMethod = record.httpMethod
  form.permissionCode = record.permissionCode || ''
  form.description = record.description || ''
  showModal.value = true
}

const handleSubmit = async () => {
  if (!form.apiName.trim() || !form.apiPath.trim()) {
    message.warning(t('admin.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (isEditing.value) {
      await updateApi(editingId.value, {
        apiName: form.apiName.trim(),
        apiPath: form.apiPath.trim(),
        httpMethod: form.httpMethod,
        permissionCode: form.permissionCode.trim() || null,
        description: form.description.trim() || null,
        status: 'ACTIVE'
      })
      message.success(t('admin.updateSuccess'))
    } else {
      await createApi({
        apiName: form.apiName.trim(),
        apiPath: form.apiPath.trim(),
        httpMethod: form.httpMethod,
        permissionCode: form.permissionCode.trim() || null,
        description: form.description.trim() || null
      })
      message.success(t('admin.addSuccess'))
    }
    showModal.value = false
    resetForm()
    await loadApis()
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.saveFailed'))
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await deleteApi(id)
    message.success(t('admin.deleteSuccess'))
    await loadApis()
  } catch (error) {
    message.error(error.response?.data?.message || t('admin.deleteFailed'))
  }
}

onMounted(() => {
  loadApis()
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
