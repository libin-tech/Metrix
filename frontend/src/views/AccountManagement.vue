<template>
  <div class="account-management-page">
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title"><BankOutlined /> {{ $t('accountManagement.title') }}</h3>
      </div>
      <div class="header-actions">
        <a-button type="primary" @click="openAddModal">
          <PlusOutlined /> {{ $t('accountManagement.addAccount') }}
        </a-button>
      </div>
    </div>

    <div v-if="loading" class="loading-container">
      <a-spin :tip="$t('portfolio.loading')" size="large" />
    </div>

    <div v-else class="table-container">
      <a-table
        :dataSource="accounts"
        :columns="columns"
        row-key="id"
        :pagination="{ pageSize: 10 }"
        :locale="{ emptyText: $t('accountManagement.noData') }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createTime' || column.key === 'updateTime'">
            <span>{{ formatTime(record[column.key]) }}</span>
          </template>
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)">
                <EditOutlined /> {{ $t('accountManagement.edit') }}
              </a-button>
              <a-popconfirm
                :title="$t('accountManagement.confirmDelete')"
                :description="`${$t('accountManagement.confirmDeleteDesc')}${$t('accountManagement.confirmDeleteCascade')}`"
                @confirm="handleDelete(record.id)"
                ok-text="确定"
                cancel-text="取消"
              >
                <a-button type="text" danger size="small">
                  <DeleteOutlined /> {{ $t('accountManagement.delete') }}
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="showModal"
      :title="isEditing ? $t('accountManagement.editAccount') : $t('accountManagement.addAccount')"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      @cancel="resetForm"
      :ok-text="$t('accountManagement.save')"
      :cancel-text="$t('accountManagement.cancel')"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item :label="$t('accountManagement.brokerName')" required>
          <a-input
            v-model:value="form.brokerName"
            :maxlength="10"
            :placeholder="$t('accountManagement.brokerNamePlaceholder')"
            show-count
          />
        </a-form-item>
        <a-form-item :label="$t('accountManagement.accountNumber')">
          <a-input
            v-model:value="form.accountNumber"
            :maxlength="30"
            :placeholder="$t('accountManagement.accountNumberPlaceholder')"
            show-count
          />
        </a-form-item>
        <a-form-item :label="$t('accountManagement.remark')">
          <a-input
            v-model:value="form.remark"
            :maxlength="50"
            :placeholder="$t('accountManagement.remarkPlaceholder')"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {message} from 'ant-design-vue'
import {BankOutlined, DeleteOutlined, EditOutlined, PlusOutlined} from '@ant-design/icons-vue'
import {createBrokerAccount, deleteBrokerAccount, getBrokerAccounts, updateBrokerAccount} from '../api'

const { t } = useI18n()

const accounts = ref([])
const loading = ref(false)
const showModal = ref(false)
const submitting = ref(false)
const isEditing = ref(false)
const editingId = ref(null)

const form = reactive({
  brokerName: '',
  accountNumber: '',
  remark: ''
})

const columns = computed(() => [
  { title: t('accountManagement.brokerName'), dataIndex: 'brokerName', key: 'brokerName', width: 200 },
  { title: t('accountManagement.accountNumber'), dataIndex: 'accountNumber', key: 'accountNumber', width: 200 },
  { title: t('accountManagement.remark'), dataIndex: 'remark', key: 'remark', width: 250 },
  { title: t('accountManagement.createTime'), dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: t('accountManagement.updateTime'), dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: t('accountManagement.actions'), key: 'actions', width: 160, align: 'center' }
])

const formatTime = (val) => {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const loadAccounts = async () => {
  loading.value = true
  try {
    const response = await getBrokerAccounts()
    accounts.value = response.data || []
  } catch {
    accounts.value = []
    message.error(t('accountManagement.loadFailed'))
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.brokerName = ''
  form.accountNumber = ''
  form.remark = ''
  isEditing.value = false
  editingId.value = null
}

const openAddModal = () => {
  resetForm()
  showModal.value = true
}

const openEditModal = (record) => {
  isEditing.value = true
  editingId.value = record.id
  form.brokerName = record.brokerName || ''
  form.accountNumber = record.accountNumber || ''
  form.remark = record.remark || ''
  showModal.value = true
}

const handleSubmit = async () => {
  if (!form.brokerName.trim()) {
    message.warning(t('accountManagement.brokerNameRequired'))
    return
  }
  submitting.value = true
  try {
    const payload = {
      brokerName: form.brokerName.trim(),
      accountNumber: form.accountNumber.trim() || null,
      remark: form.remark.trim() || null
    }
    if (isEditing.value) {
      await updateBrokerAccount(editingId.value, payload)
      message.success(t('accountManagement.updateSuccess'))
    } else {
      await createBrokerAccount(payload)
      message.success(t('accountManagement.addSuccess'))
    }
    showModal.value = false
    resetForm()
    await loadAccounts()
  } catch (error) {
    const key = isEditing.value ? 'updateFailed' : 'addFailed'
    message.error(error.response?.data?.message || t(`accountManagement.${key}`))
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await deleteBrokerAccount(id)
    message.success(t('accountManagement.deleteSuccess'))
    await loadAccounts()
  } catch (error) {
    message.error(error.response?.data?.message || t('accountManagement.deleteFailed'))
  }
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped>
.account-management-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.page-title .anticon {
  margin-right: 8px;
  color: #1890ff;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.table-container {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
</style>
