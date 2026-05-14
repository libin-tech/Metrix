<template>
  <div class="stock-basic-container">
    <div class="header">
      <h3>{{ $t('stockBasic.title') }}</h3>
      <div class="header-actions">
        <a-input-search
          v-model:value="keyword"
          :placeholder="$t('stockBasic.searchPlaceholder')"
          style="width: 360px"
          @search="handleSearch"
        />
        <a-button type="primary" @click="showImport = true">{{ $t('stockBasic.importCsv') }}</a-button>
        <a-button
          type="text"
          size="small"
          class="mask-toggle-btn"
          @click="toggleMask"
          :title="masked ? $t('analysis.showData') : $t('analysis.maskData')"
        >
          <EyeOutlined v-if="!masked" />
          <EyeInvisibleOutlined v-else />
        </a-button>
      </div>
    </div>

    <a-table
      :class="{ 'masked-table': masked }"
      :dataSource="records"
      :columns="columns"
      row-key="id"
      bordered
      :pagination="{
        current: page,
        pageSize: size,
        total,
        showSizeChanger: true,
        showTotal: n => `${$t('stockBasic.total')} ${n} ${$t('stockBasic.items')}`,
        onChange: (p, s) => { page = p; size = s; loadData(); }
      }"
      :loading="loading"
      size="small"
    >
      <template #bodyCell="{ column, text }">
        <template v-if="column.key === 'tsCode'">
          <span class="cell-ts-code">{{ text }}</span>
        </template>
        <template v-if="column.key === 'symbol'">
          <span class="cell-symbol">{{ text }}</span>
        </template>
        <template v-if="column.key === 'name'">
          <span class="cell-name">{{ text }}</span>
        </template>
        <template v-if="column.key === 'listDate'">
          {{ text || '-' }}
        </template>
      </template>
    </a-table>

    <a-modal :title="$t('stockBasic.importCsv')" v-model:visible="showImport" @ok="importCsv" @cancel="showImport = false" :confirmLoading="importing">
      <a-upload-dragger
        :beforeUpload="file => { importFile = file; return false }"
        accept=".csv"
        :maxCount="1"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">{{ $t('stockBasic.uploadHint') }}</p>
        <p class="ant-upload-hint">{{ $t('stockBasic.uploadDesc') }}</p>
      </a-upload-dragger>
    </a-modal>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {InboxOutlined, EyeOutlined, EyeInvisibleOutlined} from '@ant-design/icons-vue'
import {message} from 'ant-design-vue'
import axios from 'axios'

const {t} = useI18n()

const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const loading = ref(false)
const showImport = ref(false)
const importFile = ref(null)
const importing = ref(false)
const masked = ref(true)

const toggleMask = () => {
  masked.value = !masked.value
}

const columns = computed(() => [
  { title: t('stockBasic.id'), dataIndex: 'id', key: 'id', width: 70 },
  { title: t('stockBasic.tsCode'), dataIndex: 'tsCode', key: 'tsCode', width: 120 },
  { title: t('stockBasic.stockCode'), dataIndex: 'symbol', key: 'symbol', width: 90 },
  { title: t('stockBasic.stockName'), dataIndex: 'name', key: 'name', width: 120 },
  { title: t('stockBasic.area'), dataIndex: 'area', key: 'area', width: 80 },
  { title: t('stockBasic.industry'), dataIndex: 'industry', key: 'industry', width: 100 },
  { title: t('stockBasic.spellAbbr'), dataIndex: 'cnspell', key: 'cnspell', width: 80 },
  { title: t('stockBasic.market'), dataIndex: 'market', key: 'market', width: 80 },
  { title: t('stockBasic.listDate'), dataIndex: 'listDate', key: 'listDate', width: 100 },
  { title: t('stockBasic.controller'), dataIndex: 'actName', key: 'actName', width: 120, ellipsis: true },
  { title: t('stockBasic.enterpriseType'), dataIndex: 'actEntType', key: 'actEntType', width: 100, ellipsis: true },
])

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
    message.error(t('stockBasic.loadFailed'))
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
    message.warning(t('stockBasic.selectFile'))
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
    message.error(e.response?.data?.message || t('stockBasic.importFailed'))
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

.masked-table .cell-ts-code,
.masked-table .cell-symbol,
.masked-table .cell-name {
  filter: blur(6px);
  user-select: none;
  transition: filter 0.3s ease;
}

.mask-toggle-btn {
  color: #8c8c8c;
  font-size: 16px;
}

.mask-toggle-btn:hover {
  color: #1890ff !important;
}
</style>
