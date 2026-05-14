<template>
  <div class="config-container">
    <div class="config-header">
      <h3>{{ $t('notification.title') }}</h3>
      <a-button type="primary" @click="showAddModal = true">{{ $t('config.add') }}</a-button>
    </div>

    <a-table :dataSource="configs" :columns="columns" row-key="id" bordered>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'isActive'">
          <a-tag :color="record.isActive ? 'green' : 'red'">
            {{ record.isActive ? $t('config.active') : $t('config.inactive') }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="editConfig(record)">{{ $t('config.edit') }}</a-button>
            <a-button size="small" danger @click="deleteConfig(record.id)">{{ $t('config.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal :title="editing ? $t('config.editTitle') : $t('config.addTitle')" v-model:visible="showAddModal" @ok="saveConfig" @cancel="showAddModal = false">
      <a-form :model="form" layout="vertical">
        <a-form-item :label="$t('notification.channelType')">
          <a-input v-model:value="form.channelType" disabled />
        </a-form-item>
        <a-form-item :label="$t('notification.webhookUrl')">
          <a-input v-model:value="form.webhookUrl" />
        </a-form-item>
        <a-form-item :label="$t('notification.secret')">
          <a-input v-model:value="form.secret" />
        </a-form-item>
        <a-form-item :label="$t('config.enable')">
          <a-switch v-model:checked="form.isActive" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {
  createNotificationConfig,
  deleteNotificationConfig,
  getNotificationConfigs,
  updateNotificationConfig
} from '../api'
import {message, Modal} from 'ant-design-vue'

const columns = computed(() => [
  { title: t('config.id'), dataIndex: 'id', key: 'id', width: 80 },
  { title: t('notification.channelType'), dataIndex: 'channelType', key: 'channelType' },
  { title: t('notification.webhookUrl'), dataIndex: 'webhookUrl', key: 'webhookUrl' },
  { title: t('config.status'), key: 'isActive', width: 80 },
  { title: t('config.action'), key: 'action', width: 200 }
])

const configs = ref([])
const showAddModal = ref(false)
const {t} = useI18n()
const editing = ref(false)
const editingId = ref(null)

const form = reactive({
  channelType: 'FEISHU',
  webhookUrl: '',
  secret: '',
  isActive: true
})

const loadConfigs = async () => {
  try {
    const response = await getNotificationConfigs()
    configs.value = response.data
  } catch (error) {
    message.error(t('config.loadFailed'))
  }
}

const editConfig = (config) => {
  editing.value = true
  editingId.value = config.id
  form.channelType = config.channelType
  form.webhookUrl = config.webhookUrl
  form.secret = config.secret
  form.isActive = config.isActive
  showAddModal.value = true
}

const saveConfig = async () => {
  try {
    if (editing.value) {
      await updateNotificationConfig(editingId.value, form)
    } else {
      await createNotificationConfig(form)
    }
    showAddModal.value = false
    loadConfigs()
    resetForm()
    message.success(t('config.saveSuccess'))
  } catch (error) {
    message.error(error.response?.data?.message || t('config.saveFailed'))
  }
}

const deleteConfig = (id) => {
  Modal.confirm({
    title: t('config.confirmDelete'),
    content: t('config.confirmDeleteContent'),
    onOk: async () => {
      try {
        await deleteNotificationConfig(id)
        loadConfigs()
        message.success(t('config.deleteSuccess'))
      } catch (error) {
        message.error(t('config.deleteFailed'))
      }
    }
  })
}

const resetForm = () => {
  editing.value = false
  editingId.value = null
  form.channelType = 'FEISHU'
  form.webhookUrl = ''
  form.secret = ''
  form.isActive = true
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.config-container {
  padding: 0;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.config-header h3 {
  margin: 0;
  color: #333;
}
</style>
