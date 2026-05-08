<template>
  <div class="config-container">
    <div class="config-header">
      <h3>AI模型配置</h3>
      <a-button type="primary" @click="showAddModal = true">添加配置</a-button>
    </div>

    <a-table :dataSource="configs" :columns="columns" row-key="id" bordered>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'isActive'">
          <a-tag :color="record.isActive ? 'green' : 'red'">
            {{ record.isActive ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="editConfig(record)">编辑</a-button>
            <a-button size="small" danger @click="deleteConfig(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal :title="editing ? '编辑配置' : '添加配置'" v-model:visible="showAddModal" @ok="saveConfig" @cancel="showAddModal = false">
      <a-form :model="form" layout="vertical">
        <a-form-item label="模型类型">
          <a-select v-model:value="form.modelType">
            <a-select-option value="OPENAI">OpenAI</a-select-option>
            <a-select-option value="OLLAMA">Ollama（本地）</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="Base URL">
          <a-input v-model:value="form.apiBaseUrl" />
        </a-form-item>
        <a-form-item label="API Key">
          <a-input v-model:value="form.apiKey" />
          <span class="help-text">Ollama 本地模型可留空</span>
        </a-form-item>
        <a-form-item label="Temperature">
          <a-input-number v-model:value="form.temperature" :step="0.1" :min="0" :max="2" />
          <span class="help-text">控制模型输出随机性，0 为确定性输出，2 为最大随机性，推荐 0.7</span>
        </a-form-item>
        <a-form-item label="模型名称">
          <a-input v-model:value="form.modelName" />
          <a-space style="margin-top: 8px">
            <a-button :loading="testing" @click="handleTestConnection">测试连接</a-button>
          </a-space>
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="form.isActive" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {onMounted, reactive, ref, watch} from 'vue'
import {
  createAiModelConfig,
  deleteAiModelConfig,
  getAiModelConfigs,
  testAiModelConfig,
  updateAiModelConfig
} from '../api'
import {message, Modal} from 'ant-design-vue'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '模型类型', dataIndex: 'modelType', key: 'modelType' },
  { title: '模型名称', dataIndex: 'modelName', key: 'modelName' },
  { title: 'API地址', dataIndex: 'apiBaseUrl', key: 'apiBaseUrl' },
  { title: 'Temperature', dataIndex: 'temperature', key: 'temperature', width: 100 },
  { title: '状态', key: 'isActive', width: 80 },
  { title: '操作', key: 'action', width: 200 }
]

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
  isActive: true
})

const loadConfigs = async () => {
  try {
    const response = await getAiModelConfigs()
    configs.value = response.data
  } catch (error) {
    message.error('加载配置失败')
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
        title: '确认操作',
        content: `已存在启用的「${conflict.modelName}」(${conflict.modelType})，是否将其禁用并启用当前配置？`,
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
    message.success(editing.value ? '更新成功' : '创建成功')
  } catch (error) {
    message.error(error.response?.data?.message || '保存失败')
  }
}

const deleteConfig = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定删除该配置吗？',
    onOk: async () => {
      try {
        await deleteAiModelConfig(id)
        loadConfigs()
        message.success('删除成功')
      } catch (error) {
        message.error('删除失败')
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
    message.warning('请先填写 Base URL 和模型名称')
    return
  }
  testing.value = true
  try {
    const response = await testAiModelConfig({
      modelType: form.modelType,
      modelName: form.modelName,
      apiBaseUrl: form.apiBaseUrl,
      apiKey: form.apiKey,
      temperature: form.temperature
    })
    const { modelName, elapsedMs } = response.data
    message.success(`连接成功 · ${modelName} · ${elapsedMs} ms`)
  } catch (error) {
    message.error(error.response?.data?.message || '连接失败')
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
