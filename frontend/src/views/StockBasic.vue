<template>
  <div class="stock-basic-container">
    <div class="header">
      <h3>股票列表</h3>
      <div class="header-actions">
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索 TS代码 / 股票代码 / 名称 / 拼音缩写"
          style="width: 360px"
          @search="handleSearch"
        />
        <a-button type="primary" @click="showImport = true">导入CSV</a-button>
      </div>
    </div>

    <a-table
      :dataSource="records"
      :columns="columns"
      row-key="id"
      bordered
      :pagination="{
        current: page,
        pageSize: size,
        total,
        showSizeChanger: true,
        showTotal: t => `共 ${t} 条`,
        onChange: (p, s) => { page = p; size = s; loadData(); }
      }"
      :loading="loading"
      size="small"
    >
      <template #bodyCell="{ column, text }">
        <template v-if="column.key === 'listDate'">
          {{ text || '-' }}
        </template>
      </template>
    </a-table>

    <a-modal title="导入CSV" v-model:visible="showImport" @ok="importCsv" @cancel="showImport = false" :confirmLoading="importing">
      <a-upload-dragger
        :beforeUpload="file => { importFile = file; return false }"
        accept=".csv"
        :maxCount="1"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">点击或拖拽 CSV 文件到此区域</p>
        <p class="ant-upload-hint">以 ts_code 为唯一标识，存在则更新，不存在则新增</p>
      </a-upload-dragger>
    </a-modal>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {InboxOutlined} from '@ant-design/icons-vue'
import {message} from 'ant-design-vue'
import axios from 'axios'

const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const loading = ref(false)
const showImport = ref(false)
const importFile = ref(null)
const importing = ref(false)

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: 'TS代码', dataIndex: 'tsCode', key: 'tsCode', width: 120 },
  { title: '股票代码', dataIndex: 'symbol', key: 'symbol', width: 90 },
  { title: '股票名称', dataIndex: 'name', key: 'name', width: 120 },
  { title: '地域', dataIndex: 'area', key: 'area', width: 80 },
  { title: '行业', dataIndex: 'industry', key: 'industry', width: 100 },
  { title: '拼音缩写', dataIndex: 'cnspell', key: 'cnspell', width: 80 },
  { title: '市场', dataIndex: 'market', key: 'market', width: 80 },
  { title: '上市日期', dataIndex: 'listDate', key: 'listDate', width: 100 },
  { title: '实际控制人', dataIndex: 'actName', key: 'actName', width: 120, ellipsis: true },
  { title: '企业性质', dataIndex: 'actEntType', key: 'actEntType', width: 100, ellipsis: true },
]

const loadData = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get('/api/stock-basic/page', {
      params: { keyword: keyword.value, page: page.value, size: size.value },
      headers: { Authorization: `Bearer ${token}` }
    })
    const body = res.data
    records.value = body.data.records
    total.value = body.data.total
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const importCsv = async () => {
  if (!importFile.value) {
    message.warning('请选择文件')
    return
  }
  importing.value = true
  try {
    const token = localStorage.getItem('token')
    const formData = new FormData()
    formData.append('file', importFile.value)
    const res = await axios.post('/api/stock-basic/import', formData, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    })
    message.success(res.data.data)
    showImport.value = false
    importFile.value = null
    loadData()
  } catch (e) {
    message.error(e.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.stock-basic-container {
  padding: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h3 {
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
