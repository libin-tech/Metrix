<template>
  <div class="portfolio-page">
    <!-- Top Section -->
    <div class="page-top-section">
      <div class="top-row">
        <div class="top-left">
          <h3 class="page-title">
            <WalletOutlined /> {{ $t('portfolio.title') }}
            <a-button type="text" class="eye-btn" @click="showSensitiveInfo = !showSensitiveInfo">
              <EyeOutlined v-if="showSensitiveInfo" />
              <EyeInvisibleOutlined v-else />
            </a-button>
            <span class="title-count" v-if="holdings.length > 0">{{ uniqueStockCount }}</span>
          </h3>
        </div>
        <div class="top-actions">
          <div class="refresh-group">
            <a-button :loading="refreshing" @click="handleRefreshPrices">
              <ReloadOutlined /> {{ $t('portfolio.refreshPrices') }}
            </a-button>
            <span class="refresh-hint">{{ $t('portfolio.refreshHint') }}</span>
          </div>
          <a-button @click="router.push('/settings/account-management')">
            <SettingOutlined /> {{ $t('portfolio.manageAccount') }}
          </a-button>
          <a-button type="primary" ghost class="batch-evaluate-button" :loading="batchEvaluating" @click="handleBatchEvaluate">
            <PlayCircleOutlined /> {{ $t('portfolio.batchEvaluate') }}
          </a-button>
          <a-button type="primary" @click="showAddHoldingModal = true">
            <PlusOutlined /> {{ $t('portfolio.addHolding') }}
          </a-button>
        </div>
      </div>
      <div class="disclaimer-row">
        <InfoCircleOutlined class="disclaimer-icon" />
        <span class="disclaimer-text">{{ $t('portfolio.disclaimer') }}</span>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="search-section">
      <a-space class="search-bar" wrap>
        <a-select
          v-model:value="searchAccountId"
          :placeholder="$t('portfolio.searchAccountPlaceholder')"
          allow-clear
          class="search-account-select"
          @change="loadHoldings"
        >
          <a-select-option :value="null">{{ $t('portfolio.allAccounts') }}</a-select-option>
          <a-select-option v-for="a in accounts" :key="a.id" :value="a.id">
            {{ a.brokerName }}{{ a.accountNumber ? ' (' + getLastFour(a.accountNumber) + ')' : '' }}
          </a-select-option>
        </a-select>
        <a-input-search
          v-model:value="searchKeyword"
          :placeholder="$t('portfolio.searchPlaceholder')"
          allow-clear
          class="search-input"
          @search="loadHoldings"
          @press-enter="loadHoldings"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input-search>
      </a-space>
    </div>

    <!-- Portfolio Summary -->
    <div v-if="summary" class="summary-section">
      <a-row :gutter="16">
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-label">{{ $t('portfolio.totalMarketValue') }}</div>
            <div class="summary-value" :class="{ 'sensitive-hidden-text': !showSensitiveInfo }">
              <template v-if="showSensitiveInfo">{{ formatPrice(summary.totalMarketValue) }}</template>
              <template v-else>***</template>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-label">{{ $t('portfolio.totalProfitLossPct') }}</div>
            <div class="summary-value" :class="getProfitClass(summary.totalProfitLossPercent)">
              {{ formatSignedPercent(summary.totalProfitLossPercent) }}
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-label">{{ $t('portfolio.totalProfitLossAmt') }}</div>
            <div class="summary-value" :class="getProfitClass(summary.totalProfitLossAmount)">
              <template v-if="showSensitiveInfo">{{ formatSignedPrice(summary.totalProfitLossAmount) }}</template>
              <template v-else>***</template>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-label">{{ $t('portfolio.lastRefreshTime') }}</div>
            <div class="summary-value summary-time">
              {{ summary.refreshTime ? formatTime(summary.refreshTime) : '-' }}
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <a-spin :tip="$t('portfolio.loading')" size="large" />
    </div>

    <!-- Holdings List -->
    <div v-else-if="holdings.length > 0" class="holdings-list">
      <a-table
        :dataSource="holdings"
        :columns="columns"
        row-key="id"
        :pagination="false"
        class="holdings-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'brokerName'">
            <div class="broker-cell">
              <span class="broker-name">{{ record.brokerName || '-' }}</span>
              <span v-if="record.accountNumber" class="account-suffix">
                {{ $t('portfolio.accountSuffix') }}{{ getLastFour(record.accountNumber) }}
              </span>
            </div>
          </template>
          <template v-if="column.key === 'stockCode'">
            <span class="stock-code">{{ record.stockCode }}</span>
          </template>
          <template v-if="column.key === 'stockName'">
            <span class="stock-name">{{ record.stockName }}</span>
          </template>
          <template v-if="column.key === 'quantity'">
            <span v-if="showSensitiveInfo">{{ formatNum(record.quantity) }}</span>
            <span v-else class="sensitive-hidden">***</span>
          </template>
          <template v-if="column.key === 'cost'">
            <span :class="getCostClass(record)">{{ formatPrice(record.cost) }}</span>
          </template>
          <template v-if="column.key === 'marketValue'">
            <span v-if="showSensitiveInfo && record.currentPrice && record.quantity" :class="getMarketValueClass(record)">
              {{ formatPrice(record.currentPrice * record.quantity) }}
            </span>
            <span v-else-if="showSensitiveInfo" class="price-na">--</span>
            <span v-else class="sensitive-hidden">***</span>
          </template>
          <template v-if="column.key === 'currentPrice'">
            <span v-if="record.currentPrice" :class="getPriceClass(record)">
              {{ formatPrice(record.currentPrice) }}
            </span>
            <span v-else class="price-na">--</span>
          </template>
          <template v-if="column.key === 'profitLossPercent'">
            <span v-if="record.profitLossPercent != null" :class="getProfitClass(record.profitLossPercent)">
              {{ formatSignedPercent(record.profitLossPercent) }}
            </span>
            <span v-else class="price-na">--</span>
          </template>
          <template v-if="column.key === 'profitLossAmount'">
            <span v-if="showSensitiveInfo && record.profitLossAmount != null" :class="getProfitClass(record.profitLossAmount)">
              {{ formatSignedPrice(record.profitLossAmount) }}
            </span>
            <span v-else-if="showSensitiveInfo" class="price-na">--</span>
            <span v-else class="sensitive-hidden">***</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" :loading="evaluating.has(record.stockCode)" @click="handleEvaluate(record)">
                <PlayCircleOutlined /> {{ $t('portfolio.evaluate') }}
              </a-button>
              <a-popconfirm
                :title="$t('portfolio.confirmDelete')"
                :description="$t('portfolio.confirmDeleteDesc')"
                @confirm="handleDelete(record.id)"
                ok-text="确定"
                cancel-text="取消"
              >
                <a-button type="text" danger size="small">
                  <DeleteOutlined />
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
        <template #headerCell="{ column }">
          <template v-if="column.key === 'currentPrice' || column.key === 'marketValue' || column.key === 'profitLossPercent' || column.key === 'profitLossAmount'">
            {{ column.title }} <a-tag color="blue" style="font-size:10px; line-height:16px; margin-left:2px;">{{ $t('portfolio.realtimeTag') }}</a-tag>
          </template>
        </template>
      </a-table>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <InboxOutlined class="empty-icon" />
      <p class="empty-text">{{ $t('portfolio.noHoldings') }}</p>
      <p class="empty-hint">{{ $t('portfolio.noHoldingsHint') }}</p>
    </div>

    <!-- Batch Add Holding Modal -->
    <a-modal
      v-model:open="showAddHoldingModal"
      :title="$t('portfolio.addHolding')"
      :confirm-loading="submittingHolding"
      @ok="handleAddHolding"
      @cancel="resetHoldingForm"
      ok-text="确定"
      cancel-text="取消"
      width="760px"
    >
      <a-form layout="vertical">
        <a-form-item :label="$t('portfolio.selectAccount')" required>
          <a-select
            v-model:value="batchAccountId"
            :placeholder="$t('portfolio.selectAccountPlaceholder')"
            style="width: 100%"
            :options="accountOptions"
          />
        </a-form-item>

        <div class="batch-hint">{{ $t('portfolio.batchInputHint') }}</div>

        <div v-for="(item, index) in batchItems" :key="item.key" class="batch-row">
          <a-row :gutter="8" align="middle">
            <a-col :span="7">
              <a-auto-complete
                v-model:value="item.stockCode"
                :placeholder="$t('portfolio.selectStockPlaceholder')"
                :options="item.stockOptions"
                @search="(val) => handleStockSearch(val, index)"
                @select="(val) => handleStockSelect(val, index)"
                allow-clear
                style="width: 100%"
              />
            </a-col>
            <a-col :span="6">
              <a-input v-model:value="item.stockName" disabled :placeholder="$t('portfolio.stockNamePlaceholder')" />
            </a-col>
            <a-col :span="5">
              <a-input-number v-model:value="item.cost" :placeholder="$t('portfolio.costPlaceholder')" :min="0" :precision="3" style="width: 100%" />
            </a-col>
            <a-col :span="4">
              <a-input-number v-model:value="item.quantity" :placeholder="$t('portfolio.quantityPlaceholder')" :min="0" :precision="2" style="width: 100%" />
            </a-col>
            <a-col :span="2" class="batch-row-action">
              <a-button type="text" danger @click="removeBatchRow(index)" :disabled="batchItems.length === 1">
                <DeleteOutlined />
              </a-button>
            </a-col>
          </a-row>
        </div>

        <a-button type="dashed" block @click="addBatchRow" class="add-row-btn">
          <PlusOutlined /> {{ $t('portfolio.addRow') }}
        </a-button>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message} from 'ant-design-vue'
import {
  DeleteOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  InboxOutlined,
  InfoCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SettingOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'
import {
  batchCreatePortfolioHolding,
  createAnalysis,
  createPortfolioHolding,
  deletePortfolioHolding,
  getAllAnalysis,
  getBrokerAccounts,
  getPortfolioHoldings,
  pollRefreshedPrices,
  refreshPortfolioPrices,
  searchStocks
} from '../api'

const router = useRouter()
const { t } = useI18n()

const holdings = ref([])
const summary = ref(null)
const accounts = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const searchAccountId = ref(null)
// 敏感信息显隐（数量、总市值、盈亏金额）
const showSensitiveInfo = ref(true)
// 刷新行情状态
const refreshing = ref(false)
const refreshingIds = ref(new Set())
let refreshPollTimer = null
const submittingHolding = ref(false)
const showAddHoldingModal = ref(false)
const evaluating = ref(new Set())
const batchEvaluating = ref(false)

const batchAccountId = ref(undefined)
const batchItems = ref([])

const createEmptyBatchItem = () => ({
  key: Date.now() + Math.random(),
  stockCode: '',
  stockName: '',
  cost: undefined,
  quantity: undefined,
  stockOptions: []
})

let batchSearchTimer = null

const uniqueStockCount = computed(() => {
  return [...new Set(holdings.value.map(h => h.stockCode))].length
})

// 账户下拉选项（券商名 + 账号后四位）
const accountOptions = computed(() => {
  return accounts.value.map(a => ({
    value: a.id,
    label: a.brokerName + (a.accountNumber ? ` (${getLastFour(a.accountNumber)})` : '')
  }))
})

const columns = computed(() => [
  { title: t('portfolio.colBroker'), dataIndex: 'brokerName', key: 'brokerName', width: 160 },
  { title: t('portfolio.colCode'), dataIndex: 'stockCode', key: 'stockCode', width: 120 },
  { title: t('portfolio.colName'), dataIndex: 'stockName', key: 'stockName', width: 140 },
  { title: t('portfolio.colQuantity'), dataIndex: 'quantity', key: 'quantity', width: 100, align: 'right' },
  { title: t('portfolio.colCost'), dataIndex: 'cost', key: 'cost', width: 100, align: 'right' },
  {
    title: t('portfolio.colMarketValue'),
    key: 'marketValue',
    width: 120,
    align: 'right',
    sorter: (a, b) => {
      const aVal = (parseFloat(a.currentPrice) || 0) * (parseFloat(a.quantity) || 0)
      const bVal = (parseFloat(b.currentPrice) || 0) * (parseFloat(b.quantity) || 0)
      return aVal - bVal
    }
  },
  { title: t('portfolio.colCurrentPrice'), dataIndex: 'currentPrice', key: 'currentPrice', width: 120, align: 'right' },
  {
    title: t('portfolio.colProfitLossPct'),
    dataIndex: 'profitLossPercent',
    key: 'profitLossPercent',
    width: 130,
    align: 'right',
    sorter: (a, b) => (parseFloat(a.profitLossPercent) || 0) - (parseFloat(b.profitLossPercent) || 0)
  },
  {
    title: t('portfolio.colProfitLossAmt'),
    dataIndex: 'profitLossAmount',
    key: 'profitLossAmount',
    width: 150,
    align: 'right',
    sorter: (a, b) => (parseFloat(a.profitLossAmount) || 0) - (parseFloat(b.profitLossAmount) || 0)
  },
  { title: t('portfolio.colAction'), key: 'action', width: 120, align: 'center' }
])

const getLastFour = (num) => {
  if (!num) return ''
  return num.slice(-4)
}

const formatPrice = (val) => {
  if (val == null) return '--'
  const num = parseFloat(val)
  if (isNaN(num)) return '--'
  return num.toFixed(3)
}

const formatNum = (val) => {
  if (val == null) return '--'
  const num = parseFloat(val)
  if (isNaN(num)) return '--'
  if (num === Math.floor(num)) return num.toFixed(0)
  return num.toFixed(2)
}

const formatSignedPercent = (val) => {
  if (val == null) return '--'
  const num = parseFloat(val)
  if (isNaN(num)) return '--'
  const sign = num >= 0 ? '+' : ''
  return `${sign}${num.toFixed(2)}%`
}

const formatSignedPrice = (val) => {
  if (val == null) return '--'
  const num = parseFloat(val)
  if (isNaN(num)) return '--'
  const sign = num >= 0 ? '+' : ''
  return `${sign}${num.toFixed(3)}`
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

// 现价列颜色：现价高于成本 → 红色，低于 → 绿色
const getPriceClass = (record) => {
  if (!record.currentPrice || !record.cost) return ''
  const price = parseFloat(record.currentPrice)
  const cost = parseFloat(record.cost)
  if (price > cost) return 'profit-up'
  if (price < cost) return 'profit-down'
  return ''
}

// 成本列颜色：成本低于现价 → 红色（盈利），高于 → 绿色（亏损）
const getCostClass = (record) => {
  if (!record.currentPrice || !record.cost) return ''
  const price = parseFloat(record.currentPrice)
  const cost = parseFloat(record.cost)
  if (cost < price) return 'profit-up'
  if (cost > price) return 'profit-down'
  return ''
}

// 总市值列颜色：市值低于成本总额 → 绿色，高于 → 红色
const getMarketValueClass = (record) => {
  if (!record.currentPrice || !record.cost || !record.quantity) return ''
  const mv = parseFloat(record.currentPrice) * parseFloat(record.quantity)
  const ct = parseFloat(record.cost) * parseFloat(record.quantity)
  if (mv < ct) return 'profit-down'
  if (mv > ct) return 'profit-up'
  return ''
}

const getProfitClass = (val) => {
  if (val == null) return ''
  const num = parseFloat(val)
  if (num > 0) return 'profit-up'
  if (num < 0) return 'profit-down'
  return ''
}

const handleStockSearch = (value, index) => {
  if (batchSearchTimer) clearTimeout(batchSearchTimer)
  if (!value.trim()) {
    batchItems.value[index].stockOptions = []
    return
  }
  batchSearchTimer = setTimeout(async () => {
    try {
      const response = await searchStocks(value)
      batchItems.value[index].stockOptions = (response.data || []).map(s => ({
        value: s.stockCode,
        label: `${s.stockCode} - ${s.stockName} (${s.market})`
      }))
    } catch {
      batchItems.value[index].stockOptions = []
    }
  }, 200)
}

const handleStockSelect = (value, index) => {
  const selected = batchItems.value[index].stockOptions.find(s => s.value === value)
  if (selected) {
    batchItems.value[index].stockCode = value
    batchItems.value[index].stockName = selected.label.split(' - ')[1]?.split(' (')[0] || ''
  }
}

const loadAccounts = async () => {
  try {
    const response = await getBrokerAccounts()
    accounts.value = response.data || []
  } catch {
    accounts.value = []
  }
}

const loadHoldings = async () => {
  loading.value = true
  try {
    const response = await getPortfolioHoldings(searchKeyword.value, searchAccountId.value)
    holdings.value = response.data?.holdings || []
    summary.value = response.data?.summary || null
  } catch {
    holdings.value = []
    summary.value = null
    message.error(t('portfolio.loadFailed'))
  } finally {
    loading.value = false
  }
}

const addBatchRow = () => {
  batchItems.value.push(createEmptyBatchItem())
}

const removeBatchRow = (index) => {
  if (batchItems.value.length <= 1) return
  batchItems.value.splice(index, 1)
}

const handleAddHolding = async () => {
  if (!batchAccountId.value) {
    message.warning(t('portfolio.selectAccountRequired'))
    return
  }
  const validItems = batchItems.value.filter(i => i.stockCode.trim() && i.stockName.trim())
  if (validItems.length === 0) {
    message.warning(t('portfolio.noValidItems'))
    return
  }
  for (const item of validItems) {
    if (!item.stockCode.trim()) {
      message.warning(t('portfolio.stockCodeRequired'))
      return
    }
    if (!item.stockName.trim()) {
      message.warning(t('portfolio.stockNameRequired'))
      return
    }
  }
  submittingHolding.value = true
  try {
    const payload = validItems.map(item => ({
      accountId: batchAccountId.value,
      stockCode: item.stockCode.trim(),
      stockName: item.stockName.trim(),
      cost: item.cost || null,
      quantity: item.quantity || null
    }))
    if (payload.length === 1) {
      await createPortfolioHolding(payload[0])
    } else {
      await batchCreatePortfolioHolding(batchAccountId.value, payload)
    }
    message.success(t('portfolio.addHoldingSuccess'))
    showAddHoldingModal.value = false
    resetHoldingForm()
    await loadHoldings()
  } catch (error) {
    message.error(error.response?.data?.message || t('portfolio.addHoldingFailed'))
  } finally {
    submittingHolding.value = false
  }
}

/** 一键评估：去重+去重已有分析标的，最多提交10只 */
const handleBatchEvaluate = async () => {
  const uniqueCodes = [...new Set(holdings.value.map(h => h.stockCode))]
  if (uniqueCodes.length === 0) {
    message.warning('没有可评估的标的')
    return
  }
  batchEvaluating.value = true
  let existing = new Set()
  try {
    const records = await getAllAnalysis()
    existing = new Set(records.map(r => r.stockCode))
  } catch {
    // continue even if fetch fails
  }
  const newCodes = uniqueCodes.filter(c => !existing.has(c))
  const existingCount = uniqueCodes.length - newCodes.length
  const targetCodes = (newCodes.length > 10 ? newCodes.slice(0, 10) : newCodes)
  if (targetCodes.length === 0) {
    message.info(`我的持仓已在评估列表中`)
    batchEvaluating.value = false
    return
  }
  for (const code of targetCodes) {
    try {
      await createAnalysis({ stockCode: code, analysisType: '综合评估', pushToFeishu: false })
    } catch {
      // skip failed ones
    }
  }
  const addedCount = targetCodes.length
  if (uniqueCodes.length > 10 && newCodes.length > 10) {
    message.success(`已添加到评估队列中${addedCount}个，其中${existingCount}个已在评估列表中。为了减少等待时长只添加10只，如果需要继续分析请手动分析`)
  } else {
    message.success(`已添加到评估队列中${addedCount}个，其中${existingCount}个已在评估列表中`)
  }
  batchEvaluating.value = false
}

const handleEvaluate = async (record) => {
  const stockCode = record.stockCode
  const stockName = record.stockName
  if (evaluating.value.has(stockCode)) return
  evaluating.value = new Set(evaluating.value).add(stockCode)
  try {
    await createAnalysis({ stockCode, analysisType: '综合评估', pushToFeishu: false })
    message.success(`${stockName}（${stockCode}）已经添加到评估队列，请到标的评估中查看`)
  } catch (error) {
    message.error(error.response?.data?.message || '评估创建失败')
  } finally {
    const next = new Set(evaluating.value)
    next.delete(stockCode)
    evaluating.value = next
  }
}

/** 启动2s轮询，逐行拉取异步行情结果并更新表格 */
const startRefreshPolling = () => {
  if (refreshPollTimer) clearInterval(refreshPollTimer)
  refreshPollTimer = setInterval(async () => {
    if (refreshingIds.value.size === 0) {
      clearInterval(refreshPollTimer)
      refreshPollTimer = null
      refreshing.value = false
      return
    }
    try {
      const ids = [...refreshingIds.value]
      const resp = await pollRefreshedPrices(ids)
      const priceMap = resp.data || {}
      const keys = Object.keys(priceMap)
      if (keys.length === 0) return
      for (const key of keys) {
        refreshingIds.value.delete(Number(key))
      }
      holdings.value = holdings.value.map(h => {
        const updated = priceMap[String(h.id)]
        if (updated) {
          return { ...h, ...updated }
        }
        return h
      })
      updateClientSummary()
    } catch (e) {
      console.warn('行情轮询异常:', e)
    }
    if (refreshingIds.value.size === 0) {
      clearInterval(refreshPollTimer)
      refreshPollTimer = null
      refreshing.value = false
    }
  }, 2000)
}

const updateClientSummary = () => {
  const h = holdings.value
  let totalMarketValue = 0
  let totalCostValue = 0
  let latestRefresh = null
  for (const item of h) {
    if (item.currentPrice && item.quantity && item.cost) {
      totalMarketValue += parseFloat(item.currentPrice) * parseFloat(item.quantity)
      totalCostValue += parseFloat(item.cost) * parseFloat(item.quantity)
    }
    if (item.cachedPriceTime) {
      const t = new Date(item.cachedPriceTime).getTime()
      if (!latestRefresh || t > latestRefresh) latestRefresh = t
    }
  }
  const plAmount = totalMarketValue - totalCostValue
  const plPercent = totalCostValue > 0 ? (plAmount / totalCostValue) * 100 : 0
  summary.value = {
    totalMarketValue,
    totalProfitLossPercent: plPercent,
    totalProfitLossAmount: plAmount,
    refreshTime: latestRefresh ? new Date(latestRefresh).toISOString() : null
  }
}

/** 刷新行情：异步提交+立即展示，启动轮询逐行补充实时数据 */
const handleRefreshPrices = async () => {
  refreshing.value = true
  try {
    const response = await refreshPortfolioPrices()
    const priceData = response.data || []
    refreshingIds.value = new Set(priceData.map(h => h.id))
    const priceMap = new Map(priceData.map(h => [h.id, h]))
    holdings.value = holdings.value.map(h => {
      const updated = priceMap.get(h.id)
      if (updated) {
        return { ...h, ...updated }
      }
      return h
    })
    updateClientSummary()
    if (refreshingIds.value.size > 0) {
      startRefreshPolling()
    } else {
      refreshing.value = false
    }
  } catch (error) {
    message.error(error.response?.data?.message || t('portfolio.refreshFailed'))
    refreshing.value = false
  }
}

onUnmounted(() => {
  if (refreshPollTimer) {
    clearInterval(refreshPollTimer)
    refreshPollTimer = null
  }
})

const handleDelete = async (id) => {
  try {
    await deletePortfolioHolding(id)
    message.success(t('portfolio.deleteSuccess'))
    await loadHoldings()
  } catch (error) {
    message.error(error.response?.data?.message || t('portfolio.deleteFailed'))
  }
}

const resetHoldingForm = () => {
  batchAccountId.value = undefined
  batchItems.value = [createEmptyBatchItem()]
}

onMounted(async () => {
  await loadAccounts()
  await loadHoldings()
  batchItems.value = [createEmptyBatchItem()]
})
</script>

<style scoped>
.portfolio-page {
  padding: 0;
}

.page-top-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.top-left {
  display: flex;
  align-items: center;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.page-title .anticon {
  margin-right: 8px;
  color: #1890ff;
}

.eye-btn {
  margin-left: 4px;
  color: #999;
  font-size: 16px;
  vertical-align: middle;
}

.eye-btn:hover {
  color: #1890ff;
}

.title-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  margin-left: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  background: #1890ff;
  border-radius: 11px;
  vertical-align: middle;
}

.top-actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.batch-evaluate-button {
  color: #fff !important;
}

.refresh-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.refresh-hint {
  font-size: 11px;
  color: #999;
  white-space: nowrap;
}

.disclaimer-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 8px;
  font-size: 13px;
  color: #ad8b00;
}

.disclaimer-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.disclaimer-text {
  line-height: 1.4;
}

.search-section {
  margin-bottom: 16px;
}

.search-bar {
  width: 100%;
}

.search-account-select {
  width: 220px;
}

.search-input {
  width: 320px;
}

.sensitive-hidden {
  color: #d9d9d9;
  font-size: 14px;
  letter-spacing: 2px;
}

.sensitive-hidden-text {
  color: #d9d9d9;
  font-size: 14px;
  letter-spacing: 2px;
}

/* Summary Section */
.summary-section {
  margin-bottom: 16px;
}

.summary-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  text-align: center;
}

.summary-label {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Mono', monospace;
}

.summary-time {
  font-size: 13px;
  font-weight: 400;
  color: #666;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.holdings-list {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.holdings-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  font-size: 13px;
  color: #555;
}

.holdings-table :deep(.ant-table-tbody > tr > td) {
  font-size: 14px;
}

.holdings-table :deep(.ant-table-tbody > tr:hover) {
  background: #f0f5ff;
}

.broker-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.broker-name {
  font-weight: 600;
  color: #333;
}

.account-suffix {
  font-size: 11px;
  color: #999;
}

.stock-code {
  font-weight: 600;
  color: #333;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Mono', monospace;
}

.stock-name {
  color: #555;
}

.price-value {
  font-weight: 600;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Mono', monospace;
}

.price-na {
  color: #bbb;
}

.profit-up {
  color: #ff4d4f;
  font-weight: 600;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Mono', monospace;
}

.profit-down {
  color: #006d2c;
  font-weight: 600;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Mono', monospace;
}

.batch-hint {
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
}

.batch-row {
  margin-bottom: 8px;
  padding: 8px 4px;
  background: #fafafa;
  border-radius: 6px;
}

.batch-row-action {
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-row-btn {
  margin-top: 8px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.empty-icon {
  font-size: 48px;
  color: #d9d9d9;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #999;
  margin-bottom: 4px;
}

.empty-hint {
  font-size: 13px;
  color: #bbb;
}
</style>
