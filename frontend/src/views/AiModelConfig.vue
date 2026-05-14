<template>
  <div class="config-container">
    <div class="config-header">
      <h3>{{ $t('aiModel.title') }}</h3>
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
        <a-form-item :label="$t('aiModel.modelType')">
          <a-select v-model:value="form.modelType">
            <a-select-option value="OPENAI">OpenAI</a-select-option>
            <a-select-option value="OLLAMA">{{ $t('aiModel.ollama') }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="Base URL">
          <a-input v-model:value="form.apiBaseUrl" />
        </a-form-item>
        <a-form-item :label="$t('aiModel.apiKey')">
          <a-input v-model:value="form.apiKey" />
          <span class="help-text">{{ $t('aiModel.apiKeyHelp') }}</span>
        </a-form-item>
        <a-form-item label="Temperature">
          <a-input-number v-model:value="form.temperature" :step="0.1" :min="0" :max="2" />
          <span class="help-text">{{ $t('aiModel.temperatureHelp') }}</span>
        </a-form-item>
        <a-form-item :label="$t('aiModel.modelName')">
          <a-input v-model:value="form.modelName" />
          <a-space style="margin-top: 8px">
            <a-button :loading="testing" @click="handleTestConnection">{{ $t('aiModel.testConnection') }}</a-button>
          </a-space>
        </a-form-item>
        <a-form-item :label="$t('aiModel.timeout')">
          <a-input-number v-model:value="form.timeout" :min="10" :max="600" :step="10" />
          <span class="help-text">{{ $t('aiModel.timeoutHelp') }}</span>
        </a-form-item>
        <a-form-item :label="$t('config.enable')">
          <a-switch v-model:checked="form.isActive" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {
  createAiModelConfig,
  deleteAiModelConfig,
  getAiModelConfigs,
  testAiModelConfig,
  updateAiModelConfig
} from '../api'
import {message, Modal} from 'ant-design-vue'
import {useI18n} from 'vue-i18n'

const {t} = useI18n()

const columns = computed(() => [
  { title: t('config.id'), dataIndex: 'id', key: 'id', width: 80 },
  { title: t('aiModel.modelType'), dataIndex: 'modelType', key: 'modelType' },
  { title: t('aiModel.modelName'), dataIndex: 'modelName', key: 'modelName' },
  { title: t('aiModel.apiBaseUrl'), dataIndex: 'apiBaseUrl', key: 'apiBaseUrl' },
  { title: t('aiModel.timeoutShort'), dataIndex: 'timeout', key: 'timeout', width: 100 },
  { title: 'Temperature', dataIndex: 'temperature', key: 'temperature', width: 100 },
  { title: t('config.status'), key: 'isActive', width: 80 },
  { title: t('config.action'), key: 'action', width: 200 }
])

const configs = ref([])
const showAddModal = ref(false)
const editing = ref(false)
const editingId = ref(null)

const form = reactive({
  modelType: 'OPENAI',
  modelName: '',
  apiBaseUrl: '',
  apiKey: '',
  temperature: 0.7,
  maxTokens: 4096,
  timeout: 120,
  isActive: true
})

const loadConfigs = async () => {
  try {
    const response = await getAiModelConfigs()
    configs.value = response.data
  } catch (error) {
    message.error(t('config.loadFailed'))
  }
}

const editConfig = (config) => {
  editing.value = true
  editingId.value = config.id
  form.modelType = config.modelType
  form.modelName = config.modelName
  form.apiBaseUrl = config.apiBaseUrl
  form.apiKey = config.apiKey
  form.temperature = config.temperature
  form.maxTokens = config.maxTokens
  form.timeout = config.timeout ?? 120
  form.isActive = config.isActive
  showAddModal.value = true
}

const saveConfig = () => {
  if (form.isActive) {
    const conflict = configs.value.find(c =>
      c.modelType === form.modelType &&
      c.isActive &&
      (!editing.value || c.id !== editingId.value)
    )
    if (conflict) {
      Modal.confirm({
        title: t('aiModel.confirmAction'),
        content: t('aiModel.conflictMessage', { name: conflict.modelName, type: conflict.modelType }),
        onOk: doSave
      })
      return
    }
  }
  doSave()
}

const doSave = async () => {
  try {
    if (editing.value) {
      await updateAiModelConfig(editingId.value, form)
    } else {
      await createAiModelConfig(form)
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
        await deleteAiModelConfig(id)
        loadConfigs()
        message.success(t('config.deleteSuccess'))
      } catch (error) {
        message.error(t('config.deleteFailed'))
      }
    }
  })
}

const testing = ref(false)
const isOllama = ref(false)

watch(() => form.modelType, (val) => {
  isOllama.value = val === 'OLLAMA'
})

const handleTestConnection = async () => {
  if (!form.apiBaseUrl || !form.modelName) {
    message.warning(t('aiModel.fillRequired'))
    return
  }
  testing.value = true
  try {
    const response = await testAiModelConfig({
      modelType: form.modelType,
      modelName: form.modelName,
      apiBaseUrl: form.apiBaseUrl,
      apiKey: form.apiKey,
      temperature: form.temperature,
      timeout: form.timeout
    })
    const { modelName, elapsedMs } = response.data
    message.success(t('aiModel.testSuccess', { name: modelName, time: elapsedMs }))
  } catch (error) {
    message.error(error.response?.data?.message || t('aiModel.testFailed'))
  } finally {
    testing.value = false
  }
}

const resetForm = () => {
  editing.value = false
  editingId.value = null
  form.modelType = 'OPENAI'
  form.modelName = ''
  form.apiBaseUrl = ''
  form.apiKey = ''
  form.temperature = 0.7
  form.maxTokens = 4096
  form.timeout = 120
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

.help-text {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
