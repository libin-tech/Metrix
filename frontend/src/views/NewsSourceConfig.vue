<template>
  <div class="config-container">
    <div class="config-header">
      <h3>{{ $t('newsSource.title') }}</h3>
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
            {{ $t('newsSource.bochaOnly') }}
            <a href="https://open.bochaai.com" target="_blank">open.bochaai.com</a>
            {{ $t('newsSource.registerLink') }}
          </template>
        </a-alert>
        <a-form-item :label="$t('newsSource.sourceType')">
          <a-input :value="form.sourceName" disabled />
        </a-form-item>
        <a-form-item :label="$t('newsSource.apiUrl')">
          <a-input v-model:value="form.apiUrl" />
        </a-form-item>
        <a-form-item :label="$t('newsSource.apiKey')">
          <a-input-password v-model:value="form.apiKey" />
        </a-form-item>
        <a-form-item :label="$t('newsSource.interval')">
          <a-input-number v-model:value="form.requestInterval" />
        </a-form-item>
        <a-form-item :label="$t('newsSource.timeout')">
          <a-input-number v-model:value="form.timeout" :min="1" />
        </a-form-item>
        <a-form-item :label="$t('newsSource.remark')">
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
import {createNewsSourceConfig, deleteNewsSourceConfig, getNewsSourceConfigs, updateNewsSourceConfig} from '../api'
import {message, Modal} from 'ant-design-vue'
import {EyeOutlined, EyeInvisibleOutlined} from '@ant-design/icons-vue'

const columns = computed(() => [
  { title: t('config.id'), dataIndex: 'id', key: 'id', width: 80 },
  { title: t('newsSource.sourceType'), key: 'sourceName', width: 120 },
  { title: t('newsSource.apiUrl'), dataIndex: 'apiUrl', key: 'apiUrl' },
  { title: t('newsSource.apiKey'), key: 'apiKey', width: 260 },
  { title: t('newsSource.interval'), dataIndex: 'requestInterval', key: 'requestInterval', width: 130 },
  { title: t('newsSource.timeout'), dataIndex: 'timeout', key: 'timeout', width: 130 },
  { title: t('newsSource.remark'), dataIndex: 'remark', key: 'remark', ellipsis: true },
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
  sourceName: 'BOCHA',
  apiUrl: 'https://api.bochaai.com',
  apiKey: '',
  requestInterval: 60,
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
    const response = await getNewsSourceConfigs()
    configs.value = response.data
  } catch (error) {
    message.error(t('config.loadFailed'))
  }
}

const editConfig = (config) => {
  editing.value = true
  editingId.value = config.id
  form.sourceName = 'BOCHA'
  form.apiUrl = config.apiUrl
  form.apiKey = config.apiKey
  form.requestInterval = config.requestInterval
  form.timeout = config.timeout
  form.remark = config.remark
  form.isActive = config.isActive
  showAddModal.value = true
}

const saveConfig = async () => {
  try {
    if (editing.value) {
      await updateNewsSourceConfig(editingId.value, form)
    } else {
      await createNewsSourceConfig(form)
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
        await deleteNewsSourceConfig(id)
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
  form.sourceName = 'BOCHA'
  form.apiUrl = 'https://api.bochaai.com'
  form.apiKey = ''
  form.requestInterval = 60
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
