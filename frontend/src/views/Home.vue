<template>
  <div class="home-container">
    <h2>{{ $t('home.title') }}</h2>
    
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
          <SettingOutlined />
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ modelCount }}</p>
          <p class="stat-label">{{ $t('home.aiModelConfig') }}</p>
        </div>
      </a-card>
      
      <a-card class="stat-card">
        <div class="stat-icon orange">
          <BellOutlined />
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ notificationCount }}</p>
          <p class="stat-label">{{ $t('home.notificationConfig') }}</p>
        </div>
      </a-card>
      
      <a-card class="stat-card">
        <div class="stat-icon purple">
          <FileTextOutlined />
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ newsCount }}</p>
          <p class="stat-label">{{ $t('home.newsSourceConfig') }}</p>
        </div>
      </a-card>

      <a-card class="stat-card">
        <div class="stat-icon" style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white">
          <DatabaseOutlined />
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stockCount }}</p>
          <p class="stat-label">{{ $t('home.stockTotal') }}</p>
        </div>
      </a-card>
    </div>
    
    <div class="content-row">
      <div class="left-col">
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

      <a-card class="quick-actions" :title="$t('home.quickActions')">
        <div class="action-grid">
          <a-button type="primary" class="action-btn" @click="goToAnalysis">
            <LineChartOutlined />
            <span>{{ $t('home.stockAnalysis') }}</span>
          </a-button>
          
          <a-button type="primary" class="action-btn" ghost @click="goToAiConfig">
            <SettingOutlined />
            <span>{{ $t('home.aiModelConfig') }}</span>
          </a-button>
          
          <a-button type="primary" class="action-btn" ghost @click="goToNotification">
            <BellOutlined />
            <span>{{ $t('home.notificationConfig') }}</span>
          </a-button>
          
          <a-button type="primary" class="action-btn" ghost @click="goToNewsSource">
            <FileTextOutlined />
            <span>{{ $t('home.newsSourceConfig') }}</span>
          </a-button>
          <a-button type="primary" class="action-btn" ghost @click="goToStockBasic">
            <DatabaseOutlined />
            <span>{{ $t('home.stockList') }}</span>
          </a-button>
          <a-button type="primary" class="action-btn" ghost @click="triggerReview">
            <FundOutlined />
            <span>{{ $t('menu.marketReview') }}</span>
          </a-button>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message, Modal} from 'ant-design-vue'
import {
  BellOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  FundOutlined,
  LineChartOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'
import {
  getAiModelConfigs,
  getAllAnalysis,
  getMarketReviewCursor,
  getNewsSourceConfigs,
  getNotificationConfigs,
  getStockBasicPage,
  triggerMarketReview,
  createMarketReview
} from '../api'

const router = useRouter()
const {t} = useI18n()

const analysisCount = ref(0)
const modelCount = ref(0)
const notificationCount = ref(0)
const newsCount = ref(0)
const stockCount = ref(0)
const recentAnalysis = ref([])
const recentReviews = ref([])
const triggerLoading = ref(false)

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
    const [analysisResponse, modelResponse, notificationResponse, newsResponse, stockResponse, reviewResponse] = await Promise.all([
      getAllAnalysis(),
      getAiModelConfigs(),
      getNotificationConfigs(),
      getNewsSourceConfigs(),
      getStockBasicPage('', 1, 1),
      getMarketReviewCursor(null, 5)
    ])
    recentAnalysis.value = analysisResponse.data.slice(0, 8)
    analysisCount.value = analysisResponse.data.length
    
    modelCount.value = modelResponse.data.length
    
    notificationCount.value = notificationResponse.data.length
    
    newsCount.value = newsResponse.data.length

    stockCount.value = stockResponse.data.total

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

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.home-container {
  padding: 20px;
}

.home-container h2 {
  margin-bottom: 20px;
  color: #333;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
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
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin: 5px 0 0 0;
}

.content-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  align-items: start;
}

.left-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.recent-analysis {
  flex-shrink: 0;
}

.recent-analysis :deep(.ant-table) {
  max-height: 340px;
  overflow-y: auto;
}

.recent-reviews {
  flex-shrink: 0;
}

.review-link {
  color: #1890ff;
  cursor: pointer;
}

.review-link:hover {
  color: #40a9ff;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  height: 100px;
}

.action-btn span {
  font-size: 14px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
