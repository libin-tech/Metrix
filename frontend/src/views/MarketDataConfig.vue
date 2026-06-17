<template>
  <div class="config-container">
    <div class="config-header">
      <h3>{{ $t('marketData.title') }}</h3>
      <a-button type="primary" @click="showAddModal = true">{{ $t('config.add') }}</a-button>
    </div>

    <a-table :dataSource="configs" :columns="columns" row-key="id" bordered>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sourceName'">
          <a-tag color="blue">{{ record.sourceName }}</a-tag>
        </template>
        <template v-if="column.key === 'apiKey'">
          <a-space>
            <span>{{ visibleKeys[record.id] ? record.apiKey : maskApiKey(record.apiKey) }}</span>
            <a-button
              type="text"
              size="small"
              @click="toggleKeyVisibility(record.id)"
            >
              <template v-if="visibleKeys[record.id]">
                <EyeInvisibleOutlined />
              </template>
              <template v-else>
                <EyeOutlined />
              </template>
            </a-button>
          </a-space>
        </template>
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
        <a-alert type="info" show-icon style="margin-bottom: 16px">
          <template #message>
            {{ $t('marketData.tickflowOnly') }}
            <a href="https://tickflow.org/auth/register?ref=DA54CXKKPB" target="_blank">tickflow.org</a>
            {{ $t('marketData.registerLink') }}
          </template>
        </a-alert>
        <a-form-item :label="$t('marketData.sourceType')">
          <a-input :value="form.sourceName" disabled />
        </a-form-item>
        <a-form-item :label="$t('marketData.apiUrl')">
          <a-input v-model:value="form.apiUrl" />
        </a-form-item>
        <a-form-item :label="$t('marketData.apiKey')">
          <a-input-password v-model:value="form.apiKey" />
        </a-form-item>
        <a-form-item :label="$t('marketData.dataType')">
          <a-select v-model:value="form.dataType">
            <a-select-option value="STOCK_QUOTE">STOCK_QUOTE</a-select-option>
            <a-select-option value="INDEX">INDEX</a-select-option>
            <a-select-option value="FUTURE">FUTURE</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('marketData.interval')">
          <a-input-number v-model:value="form.requestInterval" />
        </a-form-item>
        <a-form-item :label="$t('marketData.timeout')">
          <a-input-number v-model:value="form.timeout" :min="1" />
        </a-form-item>
        <a-form-item :label="$t('marketData.remark')">
          <a-textarea v-model:value="form.remark" :maxlength="100" :rows="2" show-count />
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
import {createMarketDataConfig, deleteMarketDataConfig, getMarketDataConfigs, updateMarketDataConfig} from '../api'
import {message, Modal} from 'ant-design-vue'
import {EyeInvisibleOutlined, EyeOutlined} from '@ant-design/icons-vue'

const columns = computed(() => [
  { title: t('config.id'), dataIndex: 'id', key: 'id', width: 80 },
  { title: t('marketData.sourceType'), key: 'sourceName', width: 120 },
  { title: t('marketData.apiUrl'), dataIndex: 'apiUrl', key: 'apiUrl' },
  { title: t('marketData.apiKey'), key: 'apiKey', width: 260 },
  { title: t('marketData.dataType'), dataIndex: 'dataType', key: 'dataType' },
  { title: t('marketData.interval'), dataIndex: 'requestInterval', key: 'requestInterval', width: 130 },
  { title: t('marketData.timeout'), dataIndex: 'timeout', key: 'timeout', width: 130 },
  { title: t('marketData.remark'), dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: t('config.status'), key: 'isActive', width: 80 },
  { title: t('config.action'), key: 'action', width: 200 }
])

const configs = ref([])
const showAddModal = ref(false)
const editing = ref(false)
const editingId = ref(null)
const visibleKeys = reactive({})
const {t} = useI18n()

const form = reactive({
  sourceName: 'TICKFLOW',
  apiUrl: 'https://api.tickflow.org',
  apiKey: '',
  dataType: 'STOCK_QUOTE',
  requestInterval: 30,
  timeout: 60,
  remark: '',
  isActive: true
})

const maskApiKey = (key) => {
  if (!key || key.length <= 8) return key
  return key.substring(0, 4) + '****' + key.substring(key.length - 4)
}

const toggleKeyVisibility = (id) => {
  visibleKeys[id] = !visibleKeys[id]
}

const loadConfigs = async () => {
  try {
    const response = await getMarketDataConfigs()
    configs.value = response.data
  } catch (error) {
    message.error(t('config.loadFailed'))
  }
}

const editConfig = (config) => {
  editing.value = true
  editingId.value = config.id
  form.sourceName = 'TICKFLOW'
  form.apiUrl = config.apiUrl
  form.apiKey = config.apiKey
  form.dataType = config.dataType
  form.requestInterval = config.requestInterval
  form.timeout = config.timeout
  form.remark = config.remark
  form.isActive = config.isActive
  showAddModal.value = true
}

const saveConfig = async () => {
  try {
    if (editing.value) {
      await updateMarketDataConfig(editingId.value, form)
    } else {
      await createMarketDataConfig(form)
    }
    showAddModal.value = false
    loadConfigs()
    resetForm()
    message.success(editing.value ? t('config.saveSuccess') : t('config.saveSuccess'))
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
        await deleteMarketDataConfig(id)
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
  form.sourceName = 'TICKFLOW'
  form.apiUrl = 'https://api.tickflow.org'
  form.apiKey = ''
  form.dataType = 'STOCK_QUOTE'
  form.requestInterval = 30
  form.timeout = 60
  form.remark = ''
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
