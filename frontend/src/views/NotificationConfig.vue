<template>
  <div class="config-container">
    <div class="config-header">
      <h3>通知配置</h3>
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
        <a-form-item label="渠道类型">
          <a-input v-model:value="form.channelType" disabled />
        </a-form-item>
        <a-form-item label="Webhook地址">
          <a-input v-model:value="form.webhookUrl" />
        </a-form-item>
        <a-form-item label="密钥">
          <a-input v-model:value="form.secret" />
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
import {
  createNotificationConfig,
  deleteNotificationConfig,
  getNotificationConfigs,
  updateNotificationConfig
} from '../api'
import {message, Modal} from 'ant-design-vue'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '渠道类型', dataIndex: 'channelType', key: 'channelType' },
  { title: 'Webhook地址', dataIndex: 'webhookUrl', key: 'webhookUrl' },
  { title: '状态', key: 'isActive', width: 80 },
  { title: '操作', key: 'action', width: 200 }
]

const configs = ref([])
const showAddModal = ref(false)
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
    message.error('加载配置失败')
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
        await deleteNotificationConfig(id)
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
