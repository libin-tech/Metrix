<template>
  <div class="home-container">
    <div class="top-section">
      <div class="top-row">
        <a-card class="market-activity-card" :title="$t('home.marketQuotes')">
          <div class="index-quotes" v-if="marketIndex">
            <div class="index-item" v-for="idx in marketIndexList" :key="idx.symbol">
              <span class="index-name">{{ idx.name }}</span>
              <span class="index-value" :class="idx.changePct >= 0 ? 'up' : 'down'">{{ idx.value }}</span>
              <span class="index-change" :class="idx.changePct >= 0 ? 'up' : 'down'">
                <CaretUpOutlined v-if="idx.changePct >= 0" /><CaretDownOutlined v-else />
                {{ Math.abs(idx.changePct).toFixed(2) }}%
              </span>
            </div>
          </div>
          <a-skeleton v-else :paragraph="{ rows: 4 }" />
        </a-card>

        <a-card class="activity-analysis-card" :title="$t('home.marketActivity')">
          <template #extra>
            <template v-if="activityShortLabel">
              <a-tag :color="activityLabel.color" style="margin-right:6px; font-size:12px; line-height:20px;">{{ activityShortLabel }}</a-tag>
            </template>
            <a-typography-text type="secondary" style="font-size:12px">{{ marketActivity?.statDate || '' }}</a-typography-text>
          </template>
          <template v-if="marketActivity">
            <div class="bar-group">
              <div class="bar-row">
                <div class="bar-row-labels">
                  <span class="bar-label-item red"><span class="bar-dot dot-up"></span>{{ $t('home.up') }} {{ marketActivity.up }}</span>
                  <span class="bar-label-item green"><span class="bar-dot dot-down"></span>{{ $t('home.down') }} {{ marketActivity.down }}</span>
                  <span class="bar-label-item gray"><span class="bar-dot dot-flat"></span>{{ $t('home.flat') }} {{ flatCount }}</span>
                </div>
                <div class="stacked-bar">
                  <div class="bar-seg bar-up" :style="{ width: upPct + '%' }"></div>
                  <div class="bar-seg bar-down" :style="{ width: downPct + '%' }"></div>
                  <div class="bar-seg bar-flat" :style="{ width: flatPct + '%' }"></div>
                </div>
              </div>
              <div class="bar-row">
                <div class="bar-row-labels">
                  <span class="bar-label-item red"><span class="bar-dot dot-up"></span>{{ $t('home.limitUp') }} {{ marketActivity.limitUp }}</span>
                  <span class="bar-label-item green"><span class="bar-dot dot-down"></span>{{ $t('home.limitDown') }} {{ marketActivity.limitDown }}</span>
                </div>
                <div class="stacked-bar">
                  <div class="bar-seg bar-up" :style="{ width: limitUpPct + '%' }"></div>
                  <div class="bar-seg bar-down" :style="{ width: limitDownPct + '%' }"></div>
                </div>
              </div>
            </div>
          </template>
          <a-skeleton v-else :paragraph="{ rows: 3 }" />
        </a-card>
      </div>

      <a-card class="dashboard-card" :title="$t('home.title')">
        <div class="stats-cards">
          <a-card class="stat-card">
            <div class="stat-icon blue">
              <LineChartOutlined />
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ analysisCount }}</p>
              <p class="stat-label">{{ $t('home.analysisRecords') }}</p>
            </div>
          </a-card>

          <a-card class="stat-card">
            <div class="stat-icon green">
              <FundOutlined />
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ reviewCount }}</p>
              <p class="stat-label">{{ $t('home.reviewRecords') }}</p>
            </div>
          </a-card>

          <a-card class="stat-card">
            <div class="stat-icon orange">
              <TeamOutlined />
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ accountCount }}</p>
              <p class="stat-label">{{ $t('home.accountCount') }}</p>
            </div>
          </a-card>

          <a-card class="stat-card">
            <div class="stat-icon purple">
              <PieChartOutlined />
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ holdingCount }}</p>
              <p class="stat-label">{{ $t('home.holdingCount') }}</p>
            </div>
          </a-card>

          <a-card class="stat-card">
            <div class="stat-icon" style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white">
              <SettingOutlined />
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ modelCount }}</p>
              <p class="stat-label">{{ $t('home.aiModelConfig') }}</p>
            </div>
          </a-card>
        </div>
      </a-card>
    </div>

    <div class="bottom-section">
      <div class="left-column">
        <a-card class="recent-analysis" :title="$t('home.recentAnalysis')">
        <a-table :columns="columns" :data-source="recentAnalysis" bordered row-key="id" :pagination="false">
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'stockCode'">
              <span>{{ text }}</span>
            </template>
            <template v-if="column.key === 'stockName'">
              <a class="review-link" @click="goToAnalysisDetail(record)">{{ text }}</a>
            </template>
            <template v-if="column.key === 'isHolding'">
              <a-tag v-if="text" color="blue">{{ $t('analysis.holdingTag') }}</a-tag>
              <span v-else>-</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
            </template>
            <template v-if="column.key === 'createTime'">
              {{ formatTime(text) }}
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <div class="right-column">
      <a-card class="quick-actions" :title="$t('home.quickActions')">
        <a class="heatmap-link" href="https://quote.eastmoney.com/stockhotmap/" target="_blank" rel="noopener noreferrer">
          <DashboardOutlined /> {{ $t('home.heatmap') }}
        </a>
        <div class="action-grid">
          <a-button type="primary" class="action-btn" @click="goToAnalysis">
            <LineChartOutlined />
            <span>{{ $t('home.stockAnalysis') }}</span>
          </a-button>

          <a-button type="primary" class="action-btn" ghost @click="goToMarketReview()">
            <FundOutlined />
            <span>{{ $t('menu.marketReview') }}</span>
          </a-button>

          <a-button type="primary" class="action-btn" ghost @click="goToPortfolio">
            <WalletOutlined />
            <span>{{ $t('menu.portfolio') }}</span>
          </a-button>

          <a-button type="primary" class="action-btn" ghost @click="goToAiConfig">
            <SettingOutlined />
            <span>{{ $t('home.aiModelConfig') }}</span>
          </a-button>
        </div>
      </a-card>

      <a-card class="recent-reviews" :title="$t('home.recentReviews')">
        <a-table :columns="reviewColumns" :data-source="recentReviews" bordered row-key="id" :pagination="false">
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'reviewName'">
              <a class="review-link" @click="goToMarketReview(record)">{{ text }}</a>
            </template>
            <template v-if="column.key === 'summary'">
              <a-tag v-if="text" :color="reviewSummaryColor(text)">{{ text }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message, Modal} from 'ant-design-vue'
import {
  CaretDownOutlined,
  CaretUpOutlined,
  DashboardOutlined,
  FundOutlined,
  LineChartOutlined,
  PieChartOutlined,
  SettingOutlined,
  TeamOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'
import {
  getAiModelConfigs,
  getAllAnalysis,
  getBrokerAccounts,
  getMarketActivity,
  getMarketIndex,
  getMarketReviews,
  getMarketReviewCursor,
  getPortfolioHoldings,
  triggerMarketReview,
  createMarketReview
} from '../api'

const router = useRouter()
const {t} = useI18n()

const analysisCount = ref(0)
const modelCount = ref(0)
const reviewCount = ref(0)
const accountCount = ref(0)
const holdingCount = ref(0)
const recentAnalysis = ref([])
const recentReviews = ref([])
const triggerLoading = ref(false)

const marketActivity = ref(null)
const marketIndex = ref(null)
let activityTimer = null

const INDEX_MAP = {
  sh000001: '上证指数',
  sz399001: '深证成指',
  sz399006: '创业板指',
  sh000688: '科创50'
}

const marketIndexList = computed(() => {
  if (!marketIndex.value) return []
  return Object.entries(marketIndex.value).map(([symbol, data]) => ({
    symbol,
    name: INDEX_MAP[symbol] || data.name || symbol,
    value: data.current?.toFixed(2) || '-',
    changePct: data.changePct || 0
  }))
})

const activityLabel = computed(() => {
  const limitUp = marketActivity.value?.limitUp
  if (limitUp == null) return null
  if (limitUp > 60) {
    return { text: '赚钱效应较好', color: 'red' }
  }
  return { text: '赚钱效应消退', color: 'default' }
})

const activityShortLabel = computed(() => {
  const label = activityLabel.value
  if (!label) return null
  return label.text.replace('赚钱效应', '')
})

const flatCount = computed(() => {
  return marketActivity.value?.flat ?? 0
})

const activityTotal = computed(() => {
  if (!marketActivity.value) return 1
  return (marketActivity.value.up || 0) + (marketActivity.value.down || 0) + (flatCount.value || 0)
})

const upPct = computed(() => {
  const total = activityTotal.value
  return ((marketActivity.value?.up || 0) / total) * 100
})

const downPct = computed(() => {
  const total = activityTotal.value
  return ((marketActivity.value?.down || 0) / total) * 100
})

const flatPct = computed(() => {
  const total = activityTotal.value
  return (flatCount.value / total) * 100
})

const limitUpPct = computed(() => {
  if (!marketActivity.value) return 50
  const total = (marketActivity.value.limitUp || 0) + (marketActivity.value.limitDown || 0)
  if (total === 0) return 50
  return ((marketActivity.value.limitUp || 0) / total) * 100
})

const limitDownPct = computed(() => {
  if (!marketActivity.value) return 50
  const total = (marketActivity.value.limitUp || 0) + (marketActivity.value.limitDown || 0)
  if (total === 0) return 50
  return ((marketActivity.value.limitDown || 0) / total) * 100
})

const loadMarketActivity = async () => {
  try {
    const res = await getMarketActivity()
    marketActivity.value = res.data.data
  } catch (error) {
    console.error('赚钱效应分析加载失败', error)
  }
}

const loadMarketIndex = async () => {
  try {
    const res = await getMarketIndex()
    marketIndex.value = res.data.data
  } catch (error) {
    console.error('大盘行情加载失败', error)
  }
}

const reviewColumns = computed(() => [
  { title: t('marketReview.reviewDate'), dataIndex: 'reviewDate', key: 'reviewDate', width: 120 },
  { title: t('marketReview.reviewName'), dataIndex: 'reviewName', key: 'reviewName', width: 200 },
  { title: t('marketReview.summary'), dataIndex: 'summary', key: 'summary' }
])

const reviewStatusDot = (status) => {
  if (!status) return '#d9d9d9'
  if (status === 'REVIEWING') return '#fadb14'
  if (status === 'COMPLETED') return '#52c41a'
  if (status === 'FAILED') return '#ff4d4f'
  return '#d9d9d9'
}

const reviewSummaryColor = (summary) => {
  if (!summary) return 'default'
  if (summary === '暴涨') return '#722ed1'
  if (summary === '暴跌') return '#8c8c8c'
  if (summary === '大涨') return '#f5222d'
  if (summary === '大跌') return '#006d2c'
  if (summary === '小涨') return '#ff4d4f'
  if (summary === '小跌') return '#52c41a'
  if (summary === '微涨') return '#ffa39e'
  if (summary === '微跌') return '#95de64'
  return 'default'
}

const columns = computed(() => [
  { title: t('home.stockCode'), dataIndex: 'stockCode', key: 'stockCode', width: 120 },
  { title: t('home.stockName'), dataIndex: 'stockName', key: 'stockName', width: 140 },
  { title: t('home.isHolding'), dataIndex: 'isHolding', key: 'isHolding', width: 90 },
  { title: t('home.analysisType'), dataIndex: 'analysisType', key: 'analysisType', width: 100 },
  { title: t('home.analysisStatus'), dataIndex: 'status', key: 'status', width: 100 },
  { title: t('home.analysisTime'), dataIndex: 'createTime', key: 'createTime', width: 170 }
])

const statusColor = (status) => {
  const colors = { ANALYZING: 'processing', COMPLETED: 'success', FAILED: 'error' }
  return colors[status] || 'default'
}

const statusText = (status) => {
  const texts = { ANALYZING: t('home.analyzing'), COMPLETED: t('home.completed'), FAILED: t('home.failed') }
  return texts[status] || status
}

const loadData = async () => {
  try {
    const [analysisResponse, modelResponse, reviewResponse, allReviewsResponse, accountsResponse, holdingsResponse] = await Promise.all([
      getAllAnalysis(),
      getAiModelConfigs(),
      getMarketReviewCursor(null, 5),
      getMarketReviews(),
      getBrokerAccounts(),
      getPortfolioHoldings()
    ])
    recentAnalysis.value = analysisResponse.data.slice(0, 8)
    analysisCount.value = analysisResponse.data.length

    modelCount.value = modelResponse.data.length

    reviewCount.value = allReviewsResponse.data?.length || 0

    accountCount.value = accountsResponse.data?.length || 0

    holdingCount.value = holdingsResponse.data?.length || 0

    recentReviews.value = reviewResponse.data.items || []
  } catch (error) {
    console.error(t('home.loadFailed'), error)
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const goToAnalysis = () => router.push('/analysis')
const goToAnalysisDetail = (record) => {
  router.push({ path: '/analysis', query: { id: record.id } })
}
const goToAiConfig = () => router.push('/settings/ai-model')
const goToPortfolio = () => router.push('/portfolio')
const goToNotification = () => router.push('/settings/notification')
const goToNewsSource = () => router.push('/settings/news-source')
const goToStockBasic = () => router.push('/settings/stock-basic')
const goToMarketReview = (record) => {
  if (record) {
    router.push({ path: '/market-review', query: { id: record.id } })
  } else {
    router.push('/market-review')
  }
}

const doCreateReview = async (reviewDate) => {
  await createMarketReview(reviewDate)
  message.success(t('marketReview.executeSuccess'))
  loadData()
}

const triggerReview = async () => {
  triggerLoading.value = true
  try {
    const res = await triggerMarketReview()
    const data = res.data
    const confirmRun = () => {
      if (data.existingReview) {
        Modal.confirm({
          title: t('marketReview.confirmReRun'),
          content: t('marketReview.reRunDesc', { name: data.reviewName }),
          onOk: () => doCreateReview(data.targetDate),
        })
      } else {
        doCreateReview(data.targetDate)
      }
    }
    if (data.notClosed) {
      Modal.confirm({
        title: t('marketReview.notClosed'),
        content: t('marketReview.willReviewPrev', { date: data.targetDate }),
        onOk: confirmRun,
      })
    } else {
      confirmRun()
    }
  } catch (e) {
    message.error(e.response?.data?.message || t('marketReview.triggerFailed'))
  } finally {
    triggerLoading.value = false
  }
}

const isTradingTime = () => {
  const now = new Date()
  const day = now.getDay()
  if (day === 0 || day === 6) return false
  const h = now.getHours()
  const m = now.getMinutes()
  const time = h * 100 + m
  return (time >= 930 && time < 1130) || (time >= 1300 && time < 1500)
}

onMounted(() => {
  loadData()
  loadMarketActivity()
  loadMarketIndex()
  if (isTradingTime()) {
    activityTimer = setInterval(() => {
      loadMarketActivity()
      loadMarketIndex()
    }, 20000)
  }
})

onUnmounted(() => {
  if (activityTimer) {
    clearInterval(activityTimer)
  }
})
</script>

<style scoped>
.home-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.top-section {
  display: flex;
  gap: 20px;
  align-items: stretch;
}

.top-section > .top-row {
  flex: 3;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  min-width: 0;
}

.top-section > .dashboard-card {
  flex: 1.5;
  min-width: 0;
}

.bottom-section {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.left-column {
  flex: 3;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.right-column {
  flex: 1.5;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.dashboard-card :deep(.ant-card-body) {
  padding: 16px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-card :deep(.ant-card-body) {
  padding: 12px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-icon.green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.stat-icon.orange {
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
  color: white;
}

.stat-icon.purple {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #666;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: bold;
  color: #333;
  margin: 0;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin: 4px 0 0 0;
  white-space: nowrap;
}

.recent-analysis :deep(.ant-table) {
  max-height: 340px;
  overflow-y: auto;
}

.review-link {
  color: #1890ff;
  cursor: pointer;
}

.review-link:hover {
  color: #40a9ff;
}

.heatmap-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px;
  margin-bottom: 12px;
  border-radius: 10px;
  border: 2px solid #722ed1;
  color: #722ed1;
  font-size: 15px;
  font-weight: 700;
  text-decoration: none;
  transition: all 0.2s;
}

.heatmap-link:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: transparent;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 8px 6px;
  height: 56px;
  font-size: 12px;
}

.action-btn span {
  font-size: 11px;
  line-height: 1.2;
}

.market-activity-card :deep(.ant-card-body) {
  padding: 6px 10px;
}

.index-quotes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.index-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  background: #f8f9fa;
}

.index-name {
  font-size: 12px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
}

.index-value {
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.index-value.up {
  color: #f5222d;
}

.index-value.down {
  color: #52c41a;
}

.index-change {
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 1px;
  margin-left: auto;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.index-change.up {
  color: #f5222d;
}

.index-change.down {
  color: #52c41a;
}

/* ── 赚钱效应分析 card ── */
.activity-analysis-card :deep(.ant-card-body) {
  padding: 6px 10px;
}

.bar-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bar-row-labels {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  font-weight: 600;
}

.bar-label-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  white-space: nowrap;
}

.bar-label-item.red { color: #f5222d; }
.bar-label-item.green { color: #52c41a; }
.bar-label-item.gray { color: #8c8c8c; }

.bar-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.bar-dot.dot-up { background: #f5222d; }
.bar-dot.dot-down { background: #52c41a; }
.bar-dot.dot-flat { background: #8c8c8c; }

.stacked-bar {
  display: flex;
  height: 6px;
  border-radius: 3px;
  overflow: hidden;
  background: #f0f0f0;
}

.bar-seg {
  height: 100%;
  transition: width 0.5s ease;
  min-width: 0;
}

.bar-seg.bar-up { background: linear-gradient(90deg, #ff4d4f, #f5222d); }
.bar-seg.bar-down { background: linear-gradient(90deg, #52c41a, #73d13d); }
.bar-seg.bar-flat { background: #bfbfbf; }
</style>
