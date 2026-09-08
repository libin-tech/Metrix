<template>
  <div class="stock-basic-container">
    <div class="stock-basic-header">
      <h3>{{ $t('stockBasic.title') }}</h3>
      <div class="header-actions">
        <a-input-search
          v-model:value="keyword"
          :placeholder="$t('stockBasic.searchPlaceholder')"
          style="width: 360px"
          @search="handleSearch"
        />
        <a-button type="primary" :loading="syncing" @click="syncStocks"><SyncOutlined />{{ $t('stockBasic.sync') }}</a-button>
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
        showTotal: n => `${$t('stockBasic.total')} ${n} ${$t('stockBasic.items')}`,
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

  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {SyncOutlined} from '@ant-design/icons-vue'
import {message, Modal} from 'ant-design-vue'
import {useRouter} from 'vue-router'
import {getStockBasicPage, syncStockBasic} from '../api'

const {t} = useI18n()
const router = useRouter()

const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const loading = ref(false)
const syncing = ref(false)

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
    const body = await getStockBasicPage(keyword.value, page.value, size.value)
    records.value = body.data.records
    total.value = body.data.total
  } catch (e) {
    if (!e.notified) message.error(t('stockBasic.loadFailed'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const syncStocks = async () => {
  if (syncing.value) return
  syncing.value = true
  try {
    const response = await syncStockBasic()
    if (response.data.configurationRequired) {
      Modal.confirm({
        title: t('stockBasic.configRequired'),
        content: t('stockBasic.configRequiredHint'),
        okText: t('stockBasic.goConfigure'),
        cancelText: t('common.cancel'),
        onOk: () => router.push('/settings/market-data')
      })
      return
    }
    message.success(t('stockBasic.syncSuccess', response.data))
    await loadData()
  } catch (error) {
    if (!error.notified) message.error(error.message || t('stockBasic.syncFailed'))
  } finally {
    syncing.value = false
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

.stock-basic-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.stock-basic-header h3 {
  margin: 0;
  color: var(--theme-text, #333);
}

.stock-basic-header .header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
