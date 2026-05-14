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
      <a-card class="recent-analysis" :title="$t('home.recentAnalysis')">
        <a-table :columns="columns" :data-source="recentAnalysis" bordered row-key="id" :pagination="false">
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
            </template>
            <template v-if="column.key === 'createTime'">
              {{ formatTime(text) }}
            </template>
          </template>
        </a-table>
      </a-card>
      
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
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {
  BellOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  LineChartOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'
import {
  getAiModelConfigs,
  getAllAnalysis,
  getNewsSourceConfigs,
  getNotificationConfigs,
  getStockBasicPage
} from '../api'

const router = useRouter()
const {t} = useI18n()

const analysisCount = ref(0)
const modelCount = ref(0)
const notificationCount = ref(0)
const newsCount = ref(0)
const stockCount = ref(0)
const recentAnalysis = ref([])

const columns = computed(() => [
  { title: t('home.stockCode'), dataIndex: 'stockCode', key: 'stockCode', width: 120 },
  { title: t('home.stockName'), dataIndex: 'stockName', key: 'stockName', width: 140 },
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
    const analysisResponse = await getAllAnalysis()
    recentAnalysis.value = analysisResponse.data.slice(0, 10)
    analysisCount.value = analysisResponse.data.length
    
    const modelResponse = await getAiModelConfigs()
    modelCount.value = modelResponse.data.length
    
    const notificationResponse = await getNotificationConfigs()
    notificationCount.value = notificationResponse.data.length
    
    const newsResponse = await getNewsSourceConfigs()
    newsCount.value = newsResponse.data.length

    const stockResponse = await getStockBasicPage('', 1, 1)
    stockCount.value = stockResponse.data.total
  } catch (error) {
    console.error(t('home.loadFailed'), error)
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const goToAnalysis = () => router.push('/analysis')
const goToAiConfig = () => router.push('/settings/ai-model')
const goToNotification = () => router.push('/settings/notification')
const goToNewsSource = () => router.push('/settings/news-source')
const goToStockBasic = () => router.push('/settings/stock-basic')

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
}

.recent-analysis {
  height: 400px;
}

.recent-analysis :deep(.ant-table) {
  height: calc(100% - 60px);
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
</style>
