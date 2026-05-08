<template>
  <div class="home-container">
    <h2>决策仪表盘</h2>
    
    <div class="stats-cards">
      <a-card class="stat-card">
        <div class="stat-icon blue">
          <LineChartOutlined />
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ analysisCount }}</p>
          <p class="stat-label">分析记录</p>
        </div>
      </a-card>
      
      <a-card class="stat-card">
        <div class="stat-icon green">
          <SettingOutlined />
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ modelCount }}</p>
          <p class="stat-label">AI模型配置</p>
        </div>
      </a-card>
      
      <a-card class="stat-card">
        <div class="stat-icon orange">
          <BellOutlined />
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ notificationCount }}</p>
          <p class="stat-label">通知配置</p>
        </div>
      </a-card>
      
      <a-card class="stat-card">
        <div class="stat-icon purple">
          <FileTextOutlined />
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ newsCount }}</p>
          <p class="stat-label">新闻源配置</p>
        </div>
      </a-card>

      <a-card class="stat-card">
        <div class="stat-icon" style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white">
          <DatabaseOutlined />
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stockCount }}</p>
          <p class="stat-label">股票总数</p>
        </div>
      </a-card>
    </div>
    
    <div class="content-row">
      <a-card class="recent-analysis" title="最近分析记录">
        <a-table :columns="columns" :data-source="recentAnalysis" bordered row-key="id">
          <template #bodyCell="{ column, text }">
            <template v-if="column.key === 'confidenceScore'">
              <a-progress :percent="Number((text * 100).toFixed(0))" :show-info="false" :stroke-width="8" />
            </template>
            <template v-if="column.key === 'createTime'">
              {{ formatTime(text) }}
            </template>
          </template>
        </a-table>
      </a-card>
      
      <a-card class="quick-actions" title="快速操作">
        <div class="action-grid">
          <a-button type="primary" class="action-btn" @click="goToAnalysis">
            <LineChartOutlined />
            <span>股票分析</span>
          </a-button>
          
          <a-button type="primary" class="action-btn" ghost @click="goToAiConfig">
            <SettingOutlined />
            <span>AI模型配置</span>
          </a-button>
          
          <a-button type="primary" class="action-btn" ghost @click="goToNotification">
            <BellOutlined />
            <span>通知配置</span>
          </a-button>
          
          <a-button type="primary" class="action-btn" ghost @click="goToNewsSource">
            <FileTextOutlined />
            <span>新闻源配置</span>
          </a-button>
          <a-button type="primary" class="action-btn" ghost @click="goToStockBasic">
            <DatabaseOutlined />
            <span>股票列表</span>
          </a-button>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
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

const analysisCount = ref(0)
const modelCount = ref(0)
const notificationCount = ref(0)
const newsCount = ref(0)
const stockCount = ref(0)
const recentAnalysis = ref([])

const columns = [
  { title: '股票代码', dataIndex: 'stockCode', key: 'stockCode' },
  { title: '股票名称', dataIndex: 'stockName', key: 'stockName' },
  { title: '分析类型', dataIndex: 'analysisType', key: 'analysisType' },
  { title: '置信度', dataIndex: 'confidenceScore', key: 'confidenceScore', width: 120 },
  { title: '分析时间', dataIndex: 'createTime', key: 'createTime', width: 150 }
]

const loadData = async () => {
  try {
    const analysisResponse = await getAllAnalysis()
    recentAnalysis.value = analysisResponse.data.slice(0, 5)
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
    console.error('加载数据失败:', error)
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
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