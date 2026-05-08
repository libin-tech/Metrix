<template>
  <div class="config-container">
    <div class="config-header">
      <h3>市场数据配置</h3>
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
        <a-alert type="info" show-icon style="margin-bottom: 16px">
          <template #message>
            仅支持 TickFlow 数据源，请前往
            <a href="https://tickflow.org" target="_blank">tickflow.org</a>
            注册获取 API Key
          </template>
        </a-alert>
        <a-form-item label="API地址">
          <a-input v-model:value="form.apiUrl" />
        </a-form-item>
        <a-form-item label="API Key">
          <a-input v-model:value="form.apiKey" />
        </a-form-item>
        <a-form-item label="数据类型">
          <a-select v-model:value="form.dataType">
            <a-select-option value="STOCK_QUOTE">STOCK_QUOTE</a-select-option>
            <a-select-option value="INDEX">INDEX</a-select-option>
            <a-select-option value="FUTURE">FUTURE</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="请求间隔(秒)">
          <a-input-number v-model:value="form.requestInterval" />
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="form.isActive" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {onMounted, reactive, ref} from 'vue'
import {createMarketDataConfig, deleteMarketDataConfig, getMarketDataConfigs, updateMarketDataConfig} from '../api'
import {message, Modal} from 'ant-design-vue'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: 'API地址', dataIndex: 'apiUrl', key: 'apiUrl' },
  { title: '数据类型', dataIndex: 'dataType', key: 'dataType' },
  { title: '请求间隔(秒)', dataIndex: 'requestInterval', key: 'requestInterval', width: 150 },
  { title: '状态', key: 'isActive', width: 80 },
  { title: '操作', key: 'action', width: 200 }
]

const configs = ref([])
const showAddModal = ref(false)
const editing = ref(false)
const editingId = ref(null)

const form = reactive({
  sourceName: 'TICKFLOW',
  apiUrl: 'https://api.tickflow.org',
  apiKey: '',
  dataType: 'STOCK_QUOTE',
  requestInterval: 30,
  isActive: true
})

const loadConfigs = async () => {
  try {
    const response = await getMarketDataConfigs()
    configs.value = response.data
  } catch (error) {
    message.error('加载配置失败')
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
        await deleteMarketDataConfig(id)
        loadConfigs()
        message.success('删除成功')
      } catch (error) {
        message.error('删除失败')
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
