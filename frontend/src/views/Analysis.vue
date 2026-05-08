<template>
  <div class="analysis-page">
    <div class="analysis-layout">
      <!-- 左侧：分析记录列表 -->
      <div class="left-panel">
        <a-card title="分析记录" :bordered="false" class="records-card">
          <div class="records-header">
            <a-button type="primary" size="small" @click="refreshRecords">
              <ReloadOutlined /> 刷新
            </a-button>
          </div>
          <div class="records-list">
            <a-list
              v-if="analysisRecords.length > 0"
              :data-source="analysisRecords"
              :locale="{ emptyText: '暂无分析记录' }"
            >
              <template #renderItem="{ item }">
                <a-list-item
                  :class="{ active: selectedRecord?.id === item.id }"
                  @click="selectRecord(item)"
                >
                  <a-list-item-meta>
                    <template #title>
                      <span class="stock-code">{{ item.stockCode }}</span>
                      <span class="stock-name">{{ item.stockName }}</span>
                    </template>
                    <template #description>
                      <span class="time-info">
                        <ClockCircleOutlined /> {{ formatTime(item.createTime) }}
                      </span>
                      <span class="duration" v-if="item.duration">
                        <FieldTimeOutlined /> {{ item.duration }}秒
                      </span>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
            <div v-else class="empty-tip">
              <BarChartOutlined />
              <p>暂无分析记录</p>
              <p class="empty-hint">点击右上开始分析股票</p>
            </div>
          </div>
        </a-card>
      </div>

      <!-- 右侧：右上分析表单 + 右下概览 -->
      <div class="right-panel">
        <!-- 右上：分析操作区 -->
        <div class="top-section">
          <a-card title="股票分析" class="analysis-form-card">
            <a-form :model="form" layout="vertical" class="analysis-form">
              <div class="form-row">
                <a-form-item label="股票名称/代码" class="form-item-inline">
                  <a-auto-complete
                    v-model:value="form.stockCode"
                    placeholder="请输入股票名称/代码"
                    :options="stockOptions"
                    @search="handleStockSearch"
                    @select="handleStockSelect"
                    allow-clear
                    class="stock-input"
                  />
                </a-form-item>
                <a-form-item label="分析类型" class="form-item-inline">
                  <a-select v-model:value="form.analysisType" class="type-select">
                    <a-select-option value="综合分析">综合分析</a-select-option>
                    <a-select-option value="基本面分析">基本面分析</a-select-option>
                    <a-select-option value="技术面分析">技术面分析</a-select-option>
                    <a-select-option value="情绪分析">情绪分析</a-select-option>
                  </a-select>
                </a-form-item>
              </div>
              
              <div class="checkbox-row">
                <a-checkbox v-model:checked="form.includeMarketData">包含市场数据</a-checkbox>
                <a-checkbox v-model:checked="form.includeNews">包含新闻分析</a-checkbox>
                <a-checkbox v-model:checked="form.pushToFeishu" @change="handleFeishuCheck">
                  <SendOutlined /> 推送到飞书
                </a-checkbox>
              </div>
              
              <!-- 进度框 -->
              <div v-if="analyzing" class="progress-box">
                <div class="progress-header">
                  <LoadingOutlined :spin="true" />
                  <span>分析进行中...</span>
                </div>
                <div class="progress-steps">
                  <div 
                    v-for="(step, index) in progressSteps" 
                    :key="index"
                    :class="['step', { active: currentStep >= index, completed: currentStep > index }]"
                  >
                    <span class="step-icon">{{ index + 1 }}</span>
                    <span class="step-text">{{ step }}</span>
                  </div>
                </div>
                <a-progress :percent="progressPercent" :show-info="false" />
              </div>

              <div class="button-row">
                <a-button 
                  type="primary" 
                  :loading="analyzing"
                  :disabled="analyzing"
                  @click="handleAnalyze"
                >
                  <PlayCircleOutlined /> 开始分析
                </a-button>
                <a-button 
                  v-if="selectedRecord"
                  @click="viewDetail"
                >
                  <FileTextOutlined /> 查看详情
                </a-button>
              </div>
            </a-form>
          </a-card>
        </div>

        <!-- 右下：分析结果概览 -->
        <div class="bottom-section">
          <a-card 
            v-if="selectedRecord" 
            :title="`${selectedRecord.stockCode} - ${selectedRecord.stockName}`"
            class="result-card"
          >
            <div v-if="analysisOverview" class="overview-content">
              <!-- 价格信息 -->
              <div class="price-section">
                <div class="price-info">
                  <span class="current-price" :class="analysisOverview.changePercent >= 0 ? 'up' : 'down'">
                    {{ analysisOverview.currentPrice }}
                  </span>
                  <span class="change" :class="analysisOverview.changePercent >= 0 ? 'up' : 'down'">
                    {{ analysisOverview.changePercent >= 0 ? '+' : '' }}{{ analysisOverview.changePercent }}%
                  </span>
                </div>
              </div>

              <!-- 关键指标网格 -->
              <div class="metrics-grid">
                <div class="metric-card">
                  <div class="metric-label">恐惧贪婪指数</div>
                  <div class="metric-bar">
                    <div 
                      class="metric-fill" 
                      :style="{ width: analysisOverview.fearGreedIndex + '%' }"
                      :class="getIndexClass(analysisOverview.fearGreedIndex)"
                    ></div>
                  </div>
                  <div class="metric-value">{{ getIndexText(analysisOverview.fearGreedIndex) }}</div>
                </div>
                
                <div class="metric-card">
                  <div class="metric-label">操作建议</div>
                  <a-tag :color="getSuggestionColor(analysisOverview.suggestion)" class="metric-tag">
                    {{ analysisOverview.suggestion }}
                  </a-tag>
                </div>

                <div class="metric-card">
                  <div class="metric-label">趋势预测</div>
                  <div class="trend-info">
                    <ArrowUpOutlined :class="analysisOverview.trend === '上涨' ? 'up' : analysisOverview.trend === '下跌' ? 'down' : ''" />
                    <span>{{ analysisOverview.trend }}</span>
                  </div>
                  <div class="trend-confidence">置信度: {{ analysisOverview.trendConfidence }}%</div>
                </div>
              </div>

              <!-- 狙击点位 -->
              <div class="levels-section" v-if="analysisOverview.levels">
                <div class="section-title">
                  <LineChartOutlined /> 狙击点位
                </div>
                <div class="levels-grid">
                  <div class="level-card support">
                    <span class="level-label">支撑位</span>
                    <span class="level-value">{{ analysisOverview.levels.support }}</span>
                  </div>
                  <div class="level-card resistance">
                    <span class="level-label">压力位</span>
                    <span class="level-value">{{ analysisOverview.levels.resistance }}</span>
                  </div>
                </div>
              </div>

              <!-- 关联板块 -->
              <div class="section" v-if="analysisOverview.relatedSectors?.length">
                <div class="section-title">
                  <DatabaseOutlined /> 关联板块分析
                </div>
                <a-tag-group class="sector-tags">
                  <a-tag 
                    v-for="sector in analysisOverview.relatedSectors" 
                    :key="sector"
                    color="blue"
                  >
                    {{ sector }}
                  </a-tag>
                </a-tag-group>
              </div>

              <!-- 资讯摘要 -->
              <div class="section" v-if="analysisOverview.newsSummary">
                <div class="section-title">
                  <FileTextOutlined /> 相关资讯
                </div>
                <div class="news-content">{{ analysisOverview.newsSummary }}</div>
              </div>
            </div>

            <!-- 默认提示 -->
            <div v-else class="empty-result">
              <SearchOutlined />
              <p>选择一条分析记录查看详情</p>
            </div>
          </a-card>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <a-modal 
      v-if="showDetail"
      :title="`${selectedRecord?.stockCode} - ${selectedRecord?.stockName} 分析报告`"
      :visible="showDetail"
      :footer="null"
      width="900px"
      @cancel="showDetail = false"
    >
      <div class="detail-content">
        <div class="detail-section">
          <h3><FileTextOutlined /> 分析概述</h3>
          <pre>{{ selectedRecord?.analysisResult }}</pre>
        </div>
        <div class="detail-section" v-if="selectedRecord?.marketData">
          <h3><BarChartOutlined /> 市场数据</h3>
          <pre>{{ selectedRecord.marketData }}</pre>
        </div>
        <div class="detail-section" v-if="selectedRecord?.newsSummary">
          <h3><FileTextOutlined /> 新闻摘要</h3>
          <pre>{{ selectedRecord.newsSummary }}</pre>
        </div>
        <div class="detail-meta">
          <span>分析时间：{{ formatTime(selectedRecord?.createTime) }}</span>
          <span>分析类型：{{ selectedRecord?.analysisType }}</span>
          <span>置信度：{{ (selectedRecord?.confidenceScore * 100).toFixed(0) }}%</span>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>import {onMounted, reactive, ref} from 'vue';
import {useRouter} from 'vue-router';
import {
  createAnalysis,
  getAllAnalysis,
  getAnalysisById,
  getNotificationConfigs,
  pushToFeishu,
  searchStocks
} from '../api';
import {message} from 'ant-design-vue';
import {
  ArrowUpOutlined,
  BarChartOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  FieldTimeOutlined,
  FileTextOutlined,
  LineChartOutlined,
  LoadingOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SearchOutlined,
  SendOutlined
} from '@ant-design/icons-vue';

const router = useRouter();
const form = reactive({
 stockCode: '',
 analysisType: '综合分析',
 includeMarketData: true,
 includeNews: true,
 pushToFeishu: false
});
const analysisRecords = ref([]);
const selectedRecord = ref(null);
const analysisOverview = ref(null);
const showDetail = ref(false);
const analyzing = ref(false);
const stockOptions = ref([]);
const progressSteps = ['获取股票数据', '分析市场行情', '执行AI分析', '生成报告'];
const currentStep = ref(0);
const progressPercent = ref(0);
let searchTimer = null;
const handleStockSearch = (value) => {
 if (searchTimer)
 clearTimeout(searchTimer);
 if (!value.trim()) {
 stockOptions.value = [];
 return;
 }
 searchTimer = setTimeout(async () => {
 try {
 const response = await searchStocks(value);
 stockOptions.value = (response.data || []).map(s => ({
 value: s.stockCode,
 label: `${s.stockCode} - ${s.stockName} (${s.market})`
 }));
 }
 catch {
 stockOptions.value = [];
 }
 }, 200);
};
const handleStockSelect = (value) => {
 form.stockCode = value;
};
const handleFeishuCheck = async (e) => {
 if (e.target.checked) {
 try {
 const response = await getNotificationConfigs();
 const feishuConfig = response.data.find(c => c.channelType === 'FEISHU' && c.isActive);
 if (!feishuConfig) {
 message.warning('飞书机器人未配置，请先在系统设置中配置');
 form.pushToFeishu = false;
 }
 }
 catch {
 message.warning('无法检测飞书配置');
 form.pushToFeishu = false;
 }
 }
};
const progressTimer = ref(null);

const handleAnalyze = async () => {
  if (!form.stockCode) {
    message.warning('请输入股票代码');
    return;
  }
  analyzing.value = true;
  currentStep.value = 0;
  progressPercent.value = 0;

  progressTimer.value = setInterval(() => {
    if (currentStep.value < progressSteps.length - 1) {
      currentStep.value++;
      progressPercent.value = (currentStep.value / (progressSteps.length - 1)) * 100;
    }
  }, 3000);

  try {
    const response = await createAnalysis(form);
    currentStep.value = progressSteps.length - 1;
    progressPercent.value = 100;
    const result = response.data;
    message.success('分析完成');
    await loadAnalysisRecords();
    selectedRecord.value = analysisRecords.value.find(r => r.id === result.id) || result;
    await loadAnalysisOverview(result.id);
    if (form.pushToFeishu) {
      try {
        await pushToFeishu(result.id);
        message.success('已推送到飞书');
      }
      catch {
        message.warning('飞书推送失败');
      }
    }
  }
  catch (error) {
    message.error(error.response?.data?.message || '分析失败');
  }
  finally {
    if (progressTimer.value) {
      clearInterval(progressTimer.value);
      progressTimer.value = null;
    }
    analyzing.value = false;
  }
};
const loadAnalysisRecords = async () => {
 try {
 const response = await getAllAnalysis();
 analysisRecords.value = response.data.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
 }
 catch (error) {
 console.error('加载分析记录失败:', error);
 }
};
const selectRecord = async (record) => {
 selectedRecord.value = record;
 await loadAnalysisOverview(record.id);
};
const loadAnalysisOverview = async (id) => {
 try {
 const response = await getAnalysisById(id);
 const data = response.data;
 analysisOverview.value = {
 currentPrice: data.currentPrice || '--',
 changePercent: data.changePercent || 0,
 fearGreedIndex: data.fearGreedIndex || 50,
 suggestion: data.suggestion || '观望',
 trend: data.trend || '震荡',
 trendConfidence: data.trendConfidence || 50,
 relatedSectors: data.relatedSectors ? JSON.parse(data.relatedSectors) : [],
 levels: data.levels ? JSON.parse(data.levels) : { support: '--', resistance: '--' },
 newsSummary: data.newsSummary || '暂无资讯'
 };
 }
 catch (error) {
 analysisOverview.value = null;
 }
};
const viewDetail = () => {
 showDetail.value = true;
};
const formatTime = (time) => {
 if (!time)
 return '-';
 return new Date(time).toLocaleString('zh-CN');
};
const getIndexClass = (index) => {
 if (index >= 70)
 return 'greed';
 if (index >= 50)
 return 'neutral';
 if (index >= 30)
 return 'fear';
 return 'extreme-fear';
};
const getIndexText = (index) => {
 if (index >= 80)
 return '极度贪婪';
 if (index >= 60)
 return '贪婪';
 if (index >= 40)
 return '中性';
 if (index >= 20)
 return '恐惧';
 return '极度恐惧';
};
const getSuggestionColor = (suggestion) => {
 const colors = {
 '买入': 'green',
 '持有': 'blue',
 '观望': 'gray',
 '卖出': 'red'
 };
 return colors[suggestion] || 'gray';
};
onMounted(() => {
 loadAnalysisRecords();
});
</script>

<style scoped>
.analysis-page {
  padding: 20px;
  min-height: calc(100vh - 84px);
  background: #f5f7fa;
}

.analysis-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  min-height: calc(100vh - 124px);
}

/* 左侧面板 */
.left-panel {
  height: calc(100vh - 124px);
  overflow-y: auto;
}

.records-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.records-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.records-list {
  flex: 1;
  overflow-y: auto;
}

.records-list :deep(.ant-list-item) {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 6px;
  margin-bottom: 4px;
}

.records-list :deep(.ant-list-item:hover) {
  background: #f5f5f5;
}

.records-list :deep(.ant-list-item.active) {
  background: #e6f7ff;
  border-left: 3px solid #1890ff;
}

.stock-code {
  font-weight: bold;
  color: #333;
  margin-right: 8px;
}

.stock-name {
  color: #666;
  font-size: 14px;
}

.time-info {
  color: #999;
  font-size: 12px;
  margin-right: 12px;
}

.duration {
  color: #1890ff;
  font-size: 12px;
}

.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #999;
}

.empty-tip :deep(.anticon) {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-tip p {
  margin: 4px 0;
}

.empty-hint {
  font-size: 12px;
}

/* 右侧面板 */
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: calc(100vh - 124px);
}

.top-section {
  flex-shrink: 0;
}

.analysis-form-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.analysis-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.form-item-inline {
  flex: 1;
  margin-bottom: 0;
}

.form-item-inline .ant-form-item-label {
  padding: 0 0 4px 0;
}

.form-item-inline .ant-form-item-label > label {
  font-size: 13px;
  color: #555;
  font-weight: 500;
}

.stock-input,
.type-select {
  width: 100%;
}

.checkbox-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  padding: 8px 0;
  align-items: center;
}

.checkbox-row .ant-checkbox-wrapper {
  margin-left: 0;
  display: inline-flex;
  align-items: center;
}

.button-row {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}

.button-row .ant-btn {
  min-width: 120px;
}

.progress-box {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #52c41a;
  font-weight: bold;
  margin-bottom: 12px;
}

.progress-steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  opacity: 0.4;
}

.step.active,
.step.completed {
  opacity: 1;
}

.step-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
}

.step.active .step-icon {
  background: #1890ff;
  color: white;
}

.step.completed .step-icon {
  background: #52c41a;
  color: white;
}

.step-text {
  font-size: 11px;
  color: #666;
}

/* 右下：分析结果概览 */
.bottom-section {
  flex: 1;
  overflow-y: auto;
}

.result-card {
  height: 100%;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.overview-content {
  flex: 1;
  overflow-y: auto;
}

/* 价格信息 */
.price-section {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.price-info {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.current-price {
  font-size: 36px;
  font-weight: bold;
}

.current-price.up {
  color: #f5222d;
}

.current-price.down {
  color: #52c41a;
}

.change {
  font-size: 18px;
  font-weight: bold;
}

.change.up {
  color: #f5222d;
}

.change.down {
  color: #52c41a;
}

/* 关键指标网格 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.metric-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.metric-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.metric-bar {
  height: 6px;
  background: #e8e8e8;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.metric-fill {
  height: 100%;
  transition: width 0.3s;
}

.metric-fill.greed {
  background: linear-gradient(90deg, #f5222d, #ff7875);
}

.metric-fill.neutral {
  background: linear-gradient(90deg, #faad14, #ffc53d);
}

.metric-fill.fear {
  background: linear-gradient(90deg, #1890ff, #69c0ff);
}

.metric-fill.extreme-fear {
  background: linear-gradient(90deg, #722ed1, #9254de);
}

.metric-value {
  font-size: 13px;
  color: #333;
  font-weight: bold;
}

.metric-tag {
  font-size: 14px;
  padding: 4px 12px;
}

.trend-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.trend-info :deep(.anticon) {
  font-size: 18px;
}

.trend-info :deep(.anticon.up) {
  color: #f5222d;
}

.trend-info :deep(.anticon.down) {
  color: #52c41a;
}

.trend-confidence {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

/* 狙击点位 */
.levels-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 12px;
}

.levels-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.level-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  border-left: 4px solid;
}

.level-card.support {
  border-color: #52c41a;
}

.level-card.resistance {
  border-color: #f5222d;
}

.level-label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}

.level-value {
  font-size: 20px;
  font-weight: bold;
}

.level-card.support .level-value {
  color: #52c41a;
}

.level-card.resistance .level-value {
  color: #f5222d;
}

/* 通用区块 */
.section {
  margin-bottom: 20px;
}

.sector-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.news-content {
  background: #fafafa;
  padding: 14px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #666;
}

.empty-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-result :deep(.anticon) {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-result p {
  margin: 0;
}

/* 详情弹窗 */
.detail-content {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h3 {
  font-size: 14px;
  color: #333;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-section pre {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.6;
  color: #666;
  margin: 0;
}

.detail-meta {
  display: flex;
  gap: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #999;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .analysis-layout {
    grid-template-columns: 280px 1fr;
  }
  
  .metrics-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .analysis-layout {
    grid-template-columns: 1fr;
  }
  
  .left-panel,
  .right-panel {
    height: auto;
  }
  
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  
  .levels-grid {
    grid-template-columns: 1fr;
  }
  
  .current-price {
    font-size: 28px;
  }
}
</style>
