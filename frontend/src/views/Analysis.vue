<template>
  <div class="analysis-page">
    <div class="analysis-layout">
      <!-- 左侧：分析记录列表 -->
      <div class="left-panel">
        <a-card :title="$t('analysis.records')" :bordered="false" class="records-card">
          <div class="records-header">
            <a-space>
              <a-button type="primary" size="small" @click="refreshRecords">
                <ReloadOutlined /> {{ $t('analysis.refresh') }}
              </a-button>
            </a-space>
          </div>

          <!-- 分析记录列表 -->
          <div class="records-list" v-if="analysisRecords.length > 0">
            <a-list :data-source="analysisRecords">
              <template #renderItem="{ item }">
                <a-list-item
                  :class="{ 
                    active: selectedRecord?.id === item.id,
                    'analyzing-item': item.status === 'ANALYZING'
                  }"
                  @click="selectRecord(item)"
                >
                  <a-list-item-meta>
                    <template #title>
                      <LoadingOutlined v-if="item.status === 'ANALYZING'" class="loading-spinner" />
                      <span v-else :class="['status-dot', item.status.toLowerCase()]"></span>
                      <span class="stock-code">{{ item.stockCode }}</span>
                      <span class="stock-name">{{ item.stockName }}</span>
                    </template>
                    <template #description>
                      <span class="time-info">
                        <ClockCircleOutlined /> {{ formatTime(item.createTime) }}
                      </span>
                      <span class="duration" v-if="item.duration">
                        <FieldTimeOutlined /> {{ item.duration }}{{ $t('analysis.seconds') }}
                      </span>
                    </template>
                  </a-list-item-meta>
                  <a-button
                    type="text"
                    danger
                    size="small"
                    class="delete-btn"
                    @click.stop="handleDelete(item.id)"
                  >
                    <DeleteOutlined />
                  </a-button>
                </a-list-item>
              </template>
            </a-list>
          </div>

          <div v-if="analysisRecords.length === 0" class="empty-tip">
            <BarChartOutlined />
            <p>{{ $t('analysis.noRecords') }}</p>
            <p class="empty-hint">{{ $t('analysis.noRecordsHint') }}</p>
          </div>
        </a-card>
      </div>

      <!-- 右侧：右上分析表单 + 右下概览 -->
      <div class="right-panel">
        <!-- 右上：分析操作区 -->
        <div class="top-section">
          <a-card :title="$t('menu.analysis')" class="analysis-form-card">
            <a-form :model="form" layout="horizontal" class="analysis-form-inline">
              <a-form-item class="form-item-compact">
                <a-auto-complete
                  v-model:value="form.stockCode"
                  :placeholder="$t('analysis.inputHint')"
                  :options="stockOptions"
                  @search="handleStockSearch"
                  @select="handleStockSelect"
                  allow-clear
                  class="stock-input-inline"
                />
              </a-form-item>
              <a-form-item class="form-item-compact">
                <a-checkbox v-model:checked="form.pushToFeishu" @change="handleFeishuCheck">
                  <SendOutlined /> {{ $t('analysis.pushToFeishu') }}
                </a-checkbox>
              </a-form-item>
              <a-form-item class="form-item-compact">
                <a-button 
                  type="primary" 
                  :loading="submitting"
                  :disabled="submitting"
                  @click="handleAnalyze"
                >
                  <PlayCircleOutlined /> {{ $t('analysis.startAnalysis') }}
                </a-button>
              </a-form-item>
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
            <template #extra>
              <a-space>
                <a-button
                  v-if="selectedRecord?.status === 'COMPLETED'"
                  size="small"
                  @click="handlePushToFeishu"
                  :loading="pushingFeishu"
                >
                  <SendOutlined /> {{ $t('analysis.pushToFeishu') }}
                </a-button>
                <a-button
                  v-if="selectedRecord?.status === 'COMPLETED'"
                  size="small"
                  @click="handleExportPdf"
                  :loading="exportingPdf"
                >
                  <FilePdfOutlined /> {{ $t('analysis.exportPdf') }}
                </a-button>
                <a-button type="primary" size="small" @click="viewDetail">
                  <FileTextOutlined /> {{ $t('analysis.fullReport') }}
                </a-button>
              </a-space>
            </template>
            <div v-if="analysisOverview" class="overview-content">

              <!-- ===== 全宽：价格概览条 ===== -->
              <div class="price-hero" v-if="analysisOverview.realTimeMarket">
                <div class="price-hero-main">
                  <div class="price-hero-current">
                    <div class="ph-label">{{ $t('analysis.currentPrice') }}</div>
                    <div class="ph-price" :class="(analysisOverview.realTimeMarket.changePercent ?? 0) >= 0 ? 'up' : 'down'">
                      ¥{{ formatPrice(analysisOverview.realTimeMarket.currentPrice) }}
                    </div>
                  </div>
                  <div class="price-hero-change">
                    <div class="ph-change-pct" :class="(analysisOverview.realTimeMarket.changePercent ?? 0) >= 0 ? 'up' : 'down'">
                      {{ (analysisOverview.realTimeMarket.changePercent ?? 0) >= 0 ? '+' : '' }}{{ formatPercent(analysisOverview.realTimeMarket.changePercent) }}%
                    </div>
                    <div class="ph-change-amt" :class="(analysisOverview.realTimeMarket.changePercent ?? 0) >= 0 ? 'up' : 'down'">
                      {{ $t('analysis.changeAmount') }} {{ (analysisOverview.realTimeMarket.changePercent ?? 0) >= 0 ? '+' : '' }}{{ formatPrice(analysisOverview.realTimeMarket.changeAmount) }}
                    </div>
                  </div>
                  <div class="price-hero-stats">
                    <div class="ph-stat">
                      <span class="ph-stat-label">{{ $t('analysis.open') }}</span>
                      <span class="ph-stat-value">{{ formatPrice(analysisOverview.realTimeMarket.openPrice) }}</span>
                    </div>
                    <div class="ph-stat">
                      <span class="ph-stat-label">{{ $t('analysis.high') }}</span>
                      <span class="ph-stat-value high">{{ formatPrice(analysisOverview.realTimeMarket.highPrice) }}</span>
                    </div>
                    <div class="ph-stat">
                      <span class="ph-stat-label">{{ $t('analysis.low') }}</span>
                      <span class="ph-stat-value low">{{ formatPrice(analysisOverview.realTimeMarket.lowPrice) }}</span>
                    </div>
                    <div class="ph-stat">
                      <span class="ph-stat-label">{{ $t('analysis.prevClose') }}</span>
                      <span class="ph-stat-value">{{ formatPrice(analysisOverview.realTimeMarket.prevClosePrice) }}</span>
                    </div>
                  </div>
                  <div class="price-hero-volume">
                    <div class="ph-vol-item">
                      <span class="ph-vol-label">{{ $t('analysis.volume') }}</span>
                      <span class="ph-vol-value">{{ formatVolume(analysisOverview.realTimeMarket.volume) }}</span>
                    </div>
                    <div class="ph-vol-divider"></div>
                    <div class="ph-vol-item">
                      <span class="ph-vol-label">{{ $t('analysis.turnover') }}</span>
                      <span class="ph-vol-value">{{ formatTurnover(analysisOverview.realTimeMarket.turnover) }}</span>
                    </div>
                    <div class="ph-vol-divider"></div>
                    <div class="ph-vol-item">
                      <span class="ph-vol-label">{{ $t('analysis.turnoverRate') }}</span>
                      <span class="ph-vol-value">{{ formatPercent(analysisOverview.realTimeMarket.turnoverRate) }}%</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- ===== 双栏：核心洞察 | 技术面 + 作战计划 + 筹码分布 ===== -->
              <div class="overview-columns">
                <!-- 左栏：核心洞察 -->
                <div class="overview-col overview-col-left">
                  <div class="section core-insight" v-if="analysisOverview.coreInsight">
                    <div class="section-title">
                      <InboxOutlined /> {{ $t('analysis.coreInsight') }}
                    </div>
                    <div class="core-insight-text markdown-content" v-html="renderMarkdown(analysisOverview.coreInsight)"></div>
                  </div>
                </div>

                <!-- 右栏：技术面关键指标 + 作战计划 + 筹码分布 -->
                <div class="overview-col overview-col-right">
                  <div class="section ma-section-block" v-if="analysisOverview.dataPivot">
                    <div class="section-title">
                      <BarChartOutlined /> {{ $t('analysis.techIndicators') }}
                    </div>
                    <div class="ma-bars">
                      <div class="ma-bar-item">
                        <div class="ma-bar-label">MA5</div>
                        <div class="ma-bar-track">
                          <div class="ma-bar-fill ma5" :style="{ width: getMaBarWidth(analysisOverview.dataPivot.ma5, analysisOverview.dataPivot) + '%' }"></div>
                        </div>
                        <div class="ma-bar-value">{{ formatPrice(analysisOverview.dataPivot.ma5) }}</div>
                      </div>
                      <div class="ma-bar-item">
                        <div class="ma-bar-label">MA20</div>
                        <div class="ma-bar-track">
                          <div class="ma-bar-fill ma20" :style="{ width: getMaBarWidth(analysisOverview.dataPivot.ma20, analysisOverview.dataPivot) + '%' }"></div>
                        </div>
                        <div class="ma-bar-value">{{ formatPrice(analysisOverview.dataPivot.ma20) }}</div>
                      </div>
                      <div class="ma-bar-item">
                        <div class="ma-bar-label">MA60</div>
                        <div class="ma-bar-track">
                          <div class="ma-bar-fill ma60" :style="{ width: getMaBarWidth(analysisOverview.dataPivot.ma60, analysisOverview.dataPivot) + '%' }"></div>
                        </div>
                        <div class="ma-bar-value">{{ formatPrice(analysisOverview.dataPivot.ma60) }}</div>
                      </div>
                    </div>
                    <div class="sr-levels">
                      <div class="sr-card support">
                        <div class="sr-icon">🛡️</div>
                        <div class="sr-body">
                          <div class="sr-label">{{ $t('analysis.supportLevel') }}</div>
                          <div class="sr-value">{{ formatPrice(analysisOverview.dataPivot.supportLevel) }}</div>
                        </div>
                      </div>
                      <div class="sr-card resistance">
                        <div class="sr-icon">🚧</div>
                        <div class="sr-body">
                          <div class="sr-label">{{ $t('analysis.resistanceLevel') }}</div>
                          <div class="sr-value">{{ formatPrice(analysisOverview.dataPivot.resistanceLevel) }}</div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="section" v-if="analysisOverview.battlePlan">
                    <div class="section-title">
                      <AimOutlined /> {{ $t('analysis.battlePlan') }}
                    </div>
                    <div class="bp-grid">
                      <div class="bp-card ideal">
                        <div class="bp-header">🎯 {{ $t('analysis.idealEntry') }}</div>
                        <div class="bp-price">{{ formatPrice(analysisOverview.battlePlan.idealEntryPrice) }}</div>
                        <div class="bp-desc" v-if="analysisOverview.battlePlan.idealEntryDesc">{{ analysisOverview.battlePlan.idealEntryDesc }}</div>
                      </div>
                      <div class="bp-card suboptimal">
                        <div class="bp-header">📌 {{ $t('analysis.suboptimalEntry') }}</div>
                        <div class="bp-price">{{ formatPrice(analysisOverview.battlePlan.suboptimalEntryPrice) }}</div>
                        <div class="bp-desc" v-if="analysisOverview.battlePlan.suboptimalEntryDesc">{{ analysisOverview.battlePlan.suboptimalEntryDesc }}</div>
                      </div>
                      <div class="bp-card stoploss">
                        <div class="bp-header">🛑 {{ $t('analysis.stopLoss') }}</div>
                        <div class="bp-price">{{ formatPrice(analysisOverview.battlePlan.stopLossPrice) }}</div>
                        <div class="bp-desc" v-if="analysisOverview.battlePlan.stopLossDesc">{{ analysisOverview.battlePlan.stopLossDesc }}</div>
                      </div>
                      <div class="bp-card target">
                        <div class="bp-header">🎊 {{ $t('analysis.target') }}</div>
                        <div class="bp-price">{{ formatPrice(analysisOverview.battlePlan.targetPrice) }}</div>
                        <div class="bp-desc" v-if="analysisOverview.battlePlan.targetDesc">{{ analysisOverview.battlePlan.targetDesc }}</div>
                      </div>
                    </div>
                    <div class="bp-rr" v-if="analysisOverview.battlePlan.riskRewardRatio">
                      <span class="bp-rr-label">{{ $t('analysis.riskReward') }}</span>
                      <span class="bp-rr-value">1:{{ analysisOverview.battlePlan.riskRewardRatio }}</span>
                    </div>
                  </div>

                  <div class="section" v-if="analysisOverview.dataPivot">
                    <div class="section-title">
                      <PieChartOutlined /> {{ $t('analysis.chipDistribution') }}
                    </div>
                    <div class="chip-profit-loss">
                      <div class="cpl-card profit">
                        <div class="cpl-label">{{ $t('analysis.profitChips') }}</div>
                        <div class="cpl-value">{{ formatPercent(analysisOverview.dataPivot.profitRatio) }}%</div>
                      </div>
                      <div class="cpl-card loss">
                        <div class="cpl-label">{{ $t('analysis.lossChips') }}</div>
                        <div class="cpl-value">{{ formatPercent(analysisOverview.dataPivot.lossRatio) }}%</div>
                      </div>
                    </div>
                    <div class="chip-metrics">
                      <div class="cm-item" v-if="analysisOverview.dataPivot.avgCostPrice">
                        <span class="cm-label">{{ $t('analysis.avgCost') }}</span>
                        <span class="cm-value">¥{{ formatPrice(analysisOverview.dataPivot.avgCostPrice) }}</span>
                      </div>
                      <div class="cm-item">
                        <span class="cm-label">{{ $t('analysis.chipConcentration') }}</span>
                        <span class="cm-value">{{ formatPercent(analysisOverview.dataPivot.chipConcentration) }}%</span>
                      </div>
                      <div class="cm-item" v-if="analysisOverview.dataPivot.cost90Low">
                        <span class="cm-label">{{ $t('analysis.cost90Range') }}</span>
                        <span class="cm-value">{{ formatPrice(analysisOverview.dataPivot.cost90Low) }} ~ {{ formatPrice(analysisOverview.dataPivot.cost90High) }}</span>
                      </div>
                      <div class="cm-item" v-if="analysisOverview.dataPivot.cost70Low">
                        <span class="cm-label">{{ $t('analysis.cost70Range') }}</span>
                        <span class="cm-value">{{ formatPrice(analysisOverview.dataPivot.cost70Low) }} ~ {{ formatPrice(analysisOverview.dataPivot.cost70High) }}</span>
                      </div>
                      <div class="cm-item" v-if="analysisOverview.dataPivot.chipSummary">
                        <span class="cm-label">{{ $t('analysis.chipSummary') }}</span>
                        <span class="cm-value cm-hint">{{ analysisOverview.dataPivot.chipSummary }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- ===== 全宽：相关新闻 ===== -->
              <div class="section section-full news-section" v-if="analysisOverview.newsList?.length">
                <div class="section-title">
                  <MessageOutlined /> {{ $t('analysis.relatedNews') }}
                </div>
                <div class="news-list-compact">
                  <div 
                    v-for="(news, index) in analysisOverview.newsList.slice(0, 3)" 
                    :key="index"
                    class="news-item-compact"
                  >
                    <div class="news-title-compact">
                      <span class="news-number-compact">{{ index + 1 }}.</span>
                      <span>{{ news.title }}</span>
                    </div>
                    <div class="news-summary-compact">{{ news.summary }}</div>
                    <div class="news-meta-compact">
                      <span class="news-source-compact">{{ news.source }}</span>
                      <span class="news-time-compact">{{ formatDateTime(news.publishTime) }}</span>
                    </div>
                    <a 
                      v-if="news.url"
                      :href="news.url" 
                      target="_blank" 
                      class="news-link"
                    >
                       {{ $t('analysis.viewOriginal') }} <LinkOutlined />
                    </a>
                  </div>
                </div>
                <div class="view-more-news" v-if="analysisOverview.newsList?.length > 3">
                  <a-button type="link" @click="showDetail = true">
                    {{ $t('analysis.moreNews') }} <RightOutlined />
                  </a-button>
                </div>
              </div>

            </div>

            <!-- 默认提示 -->
            <div v-else class="empty-result">
              <SearchOutlined />
              <p>{{ $t('analysis.selectRecordHint') }}</p>
            </div>
          </a-card>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <a-modal 
      v-if="showDetail"
      :title="`${selectedRecord?.stockCode} - ${selectedRecord?.stockName} ${$t('analysis.analysisReport')}`"
      :visible="showDetail"
      :footer="null"
      width="1200px"
      :style="{ top: '20px' }"
      class="detail-modal"
      @cancel="showDetail = false"
    >
      <div class="detail-content">
        <!-- 分析结果（Markdown格式） -->
        <div class="detail-section">
          <h3><FileTextOutlined /> {{ $t('analysis.analysisReport') }}</h3>
          <div class="markdown-content" v-html="renderMarkdown(analysisDetail?.analysisResult)"></div>
        </div>
        
        <!-- 新闻列表 -->
        <div class="detail-section" v-if="analysisDetail?.newsList?.length">
          <h3><MessageOutlined /> {{ $t('analysis.detailNews') }}</h3>
          <div class="news-list">
            <div 
              v-for="(news, index) in analysisDetail.newsList" 
              :key="index"
              class="news-item"
            >
              <div class="news-title">
                <span class="news-number">{{ index + 1 }}.</span>
                <span>{{ news.title }}</span>
              </div>
              <div class="news-summary">{{ news.summary }}</div>
              <div class="news-meta">
                <span class="news-source">{{ news.source }}</span>
                <span class="news-time">{{ formatDateTime(news.publishTime) }}</span>
              </div>
              <a 
                v-if="news.url"
                :href="news.url" 
                target="_blank" 
                class="news-link"
              >
                <FileTextOutlined /> {{ $t('analysis.viewDetail') }}
              </a>
            </div>
          </div>
        </div>
        
        <!-- 新闻摘要（文本格式） -->
        <div class="detail-section" v-if="analysisDetail?.newsSummary && !analysisDetail?.newsList?.length">
          <h3><MessageOutlined /> {{ $t('analysis.newsSummary') }}</h3>
          <div class="news-summary-text">{{ analysisDetail.newsSummary }}</div>
        </div>
        
        <div class="detail-meta">
          <span>{{ $t('analysis.analysisTime') }}{{ formatTime(analysisDetail?.createdAt || selectedRecord?.createTime) }}</span>
          <span>{{ $t('analysis.analysisType') }}{{ analysisDetail?.analysisType || selectedRecord?.analysisType }}</span>
          <span>{{ $t('analysis.confidence') }}{{ ((analysisDetail?.confidenceScore || selectedRecord?.confidenceScore) * 100).toFixed(0) }}%</span>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { marked } from 'marked';
import { useI18n } from 'vue-i18n';
import {
  createAnalysis,
  getAllAnalysis,
  getAnalysisById,
  deleteAnalysis,
  getNotificationConfigs,
  pushToFeishu as pushToFeishuApi,
  exportPdf as exportPdfApi,
  searchStocks,
  getAnalysisDetail,
  getQueueStatus
} from '../api';
import { message } from 'ant-design-vue';
import {
  BarChartOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  FieldTimeOutlined,
  FilePdfOutlined,
  LoadingOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SearchOutlined,
  SendOutlined,
  MessageOutlined,
  InboxOutlined,
  AimOutlined,
  UserOutlined,
  LinkOutlined,
  RightOutlined,
  PieChartOutlined
} from '@ant-design/icons-vue';

marked.use({
  gfm: true,
  breaks: true
});

const renderMarkdown = (content) => {
  if (!content) return '';

  try {
    // Preprocess: escape < that could be mistaken as HTML tags (e.g. "< 5日均量")
    // but preserve valid HTML tags like <br>, <div>, etc.
    const cleaned = content
      .replace(/<(?!\/?([a-zA-Z][a-zA-Z0-9]*|\/?[a-zA-Z]))/g, '&lt;')
      .replace(/\u201C|\u201D/g, '"')
      .replace(/\u2018|\u2019/g, "'");
    return marked.parse(cleaned);
  } catch (e) {
    console.error('[renderMarkdown] marked.parse failed:', e);
    // Fallback: escape HTML, preserve line breaks
    const safe = content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
    return safe
      .split(/\n{2,}/)
      .map(p => `<p>${p.replace(/\n/g, '<br>')}</p>`)
      .join('');
  }
};

const router = useRouter();
const { t } = useI18n();
const form = reactive({
  stockCode: '',
  analysisType: t('analysis.comprehensiveAnalysis'),
  pushToFeishu: false
});
const analysisRecords = ref([]);
const selectedRecord = ref(null);
const analysisOverview = ref(null);
const analysisDetail = ref(null);
const showDetail = ref(false);
const submitting = ref(false);
const pushingFeishu = ref(false);
const exportingPdf = ref(false);
const stockOptions = ref([]);
const queueStatus = ref(null);

let searchTimer = null;
let pollingTimer = null;

// 分析中记录
const analyzingRecords = computed(() => {
  return analysisRecords.value.filter(r => r.status === 'ANALYZING');
});

// 分析完成记录
const completedRecords = computed(() => {
  return analysisRecords.value.filter(r => r.status !== 'ANALYZING');
});

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
        message.warning(t('analysis.feishuNotConfigured'));
        form.pushToFeishu = false;
      }
    }
    catch {
      message.warning(t('analysis.feishuCheckFailed'));
      form.pushToFeishu = false;
    }
  }
};

const handleAnalyze = async () => {
  if (!form.stockCode) {
    message.warning(t('analysis.inputStockCode'));
    return;
  }
  submitting.value = true;

  try {
    await createAnalysis(form);
    message.success(t('analysis.analysisCreated'));
  }
  catch (error) {
    message.error(error.response?.data?.message || t('analysis.analysisFailed'));
  }
  finally {
    submitting.value = false;
    await loadAnalysisRecords();
    await loadQueueStatus();
  }
};

const handlePushToFeishu = async () => {
  if (!selectedRecord.value?.id) return;
  pushingFeishu.value = true;
  try {
    await pushToFeishuApi(selectedRecord.value.id);
    message.success(t('analysis.feishuPushSuccess'));
  } catch (error) {
    message.error(error.response?.data?.message || t('analysis.feishuPushFailed'));
  } finally {
    pushingFeishu.value = false;
  }
};

const handleExportPdf = async () => {
  if (!selectedRecord.value?.id) return;
  exportingPdf.value = true;
  try {
    const response = await exportPdfApi(selectedRecord.value.id);
    const blob = new Blob([response], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const now = new Date();
    const dateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${String(now.getHours()).padStart(2, '0')}`;
    link.download = `${selectedRecord.value.stockName}（${selectedRecord.value.stockCode}）综合评估报告-${dateStr}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    message.success(t('analysis.pdfExportSuccess'));
  } catch (error) {
    message.error(t('analysis.pdfExportFailed'));
  } finally {
    exportingPdf.value = false;
  }
};

const loadAnalysisRecords = async () => {
  try {
    const response = await getAllAnalysis();
    const newRecords = response.data.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
    
    // 比较新旧数据是否有变化
    const hasChanges = compareRecords(analysisRecords.value, newRecords);
    
    if (hasChanges) {
      analysisRecords.value = newRecords;
      console.log('分析记录数据已更新');
    }
  }
  catch (error) {
    console.error('加载分析记录失败:', error);
  }
};

// 比较两个记录数组是否有变化
const compareRecords = (oldRecords, newRecords) => {
  if (oldRecords.length !== newRecords.length) {
    return true;
  }
  
  for (let i = 0; i < oldRecords.length; i++) {
    const oldRecord = oldRecords[i];
    const newRecord = newRecords[i];
    
    if (oldRecord.id !== newRecord.id ||
        oldRecord.status !== newRecord.status ||
        oldRecord.progress !== newRecord.progress) {
      return true;
    }
  }
  
  return false;
};

const loadQueueStatus = async () => {
  try {
    const response = await getQueueStatus();
    queueStatus.value = response.data;
  }
  catch (error) {
    console.error('加载队列状态失败:', error);
  }
};

const selectRecord = async (record) => {
  // 只有分析完成的记录才能查看详情
  if (record.status === 'ANALYZING') {
    message.info('该任务正在进行中，请稍后再查看');
    return;
  }
  selectedRecord.value = record;
  await loadAnalysisOverview(record.id);
};

const loadAnalysisOverview = async (id) => {
  try {
    const response = await getAnalysisDetail(id);
    const data = response.data;
    
    // 解析分析概览JSON
    let parsedOverview = {};
    if (data.analysisOverview) {
      try {
        parsedOverview = JSON.parse(data.analysisOverview);
      } catch (e) {
        console.error('解析分析概览失败:', e);
      }
    }
    
    analysisOverview.value = {
      currentPrice: data.currentPrice || '--',
      changePercent: data.changePercent || 0,
      changeAmount: data.changeAmount || 0,
      priceTimestamp: data.priceTimestamp || '',
      tradingStatus: data.tradingStatus || 'trading',
      suggestion: data.suggestion || '观望',
      shortTermSuggestion: data.shortTermSuggestion || '',
      mediumTermSuggestion: data.mediumTermSuggestion || '',
      riskLevel: data.riskLevel || 'medium',
      positionSizing: data.positionSizing || 50,
      trend: data.trend || '震荡',
      trendConfidence: data.trendConfidence || 50,
      dailyTrend: data.dailyTrend || 'neutral',
      weeklyTrend: data.weeklyTrend || 'neutral',
      monthlyTrend: data.monthlyTrend || 'neutral',
      technicalIndicators: data.technicalIndicators ? JSON.parse(data.technicalIndicators) : {},
      coreInsight: parsedOverview.coreInsight || '',
      relatedSectors: parsedOverview.relatedSectors || [],
      levels: data.levels ? JSON.parse(data.levels) : { support: '--', resistance: '--' },
      supportLevels: data.supportLevels ? JSON.parse(data.supportLevels) : [],
      resistanceLevels: data.resistanceLevels ? JSON.parse(data.resistanceLevels) : [],
      newsSummary: data.newsSummary || '暂无资讯',
      newsList: typeof data.newsList === 'string' ? (data.newsList ? JSON.parse(data.newsList) : []) : (data.newsList || []),
      
      // 新的分析概览模块
      realTimeMarket: parsedOverview.realTimeMarket || null,
      dataPivot: parsedOverview.dataPivot || null,
      battlePlan: parsedOverview.battlePlan || null,
      positionAdvice: parsedOverview.positionAdvice || null,
      checkList: parsedOverview.checkList || null
    };
  }
  catch (error) {
    analysisOverview.value = null;
  }
};
const viewDetail = async () => {
 if (!selectedRecord.value?.id) return;
 try {
 const response = await getAnalysisDetail(selectedRecord.value.id);
 analysisDetail.value = response.data;
 showDetail.value = true;
 } catch (error) {
 message.error('获取详情失败');
 }
};
const handleDelete = async (id) => {
 try {
 await deleteAnalysis(id);
  message.success(t('analysis.deleteSuccess'));
  analysisRecords.value = analysisRecords.value.filter(r => r.id !== id);
  if (selectedRecord.value?.id === id) {
    selectedRecord.value = null;
    analysisOverview.value = null;
    showDetail.value = false;
  }
  }
  catch (error) {
  message.error(t('analysis.deleteFailed'));
 }
};

const formatTime = (time) => {
 if (!time)
 return '-';
 return new Date(time).toLocaleString('zh-CN');
};
const formatDateTime = (time) => {
 if (!time) return '-';
 const date = new Date(time);
 const year = date.getFullYear();
 const month = String(date.getMonth() + 1).padStart(2, '0');
 const day = String(date.getDate()).padStart(2, '0');
 const hours = String(date.getHours()).padStart(2, '0');
 const minutes = String(date.getMinutes()).padStart(2, '0');
 const seconds = String(date.getSeconds()).padStart(2, '0');
 return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};


// 计算均线柱状图宽度（相对当前价的比例）
const getMaBarWidth = (maValue, dataPivot) => {
  if (!maValue || !dataPivot?.currentPrice) return 50;
  const ma = parseFloat(maValue);
  const price = parseFloat(dataPivot.currentPrice);
  if (isNaN(ma) || isNaN(price) || price === 0) return 50;
  const ratio = ma / price;
  // ratio > 1 means MA above current price, < 1 means below
  // cap between 30% and 100%
  return Math.min(Math.max(Math.round(ratio * 80), 30), 100);
};

// 格式化价格（2位小数）
const formatPrice = (price) => {
  if (!price || price === '--') return '--';
  const num = parseFloat(price);
  if (isNaN(num)) return price;
  return num.toFixed(2);
};

// 格式化百分比（2位小数）
const formatPercent = (percent) => {
  if (!percent && percent !== 0) return '--';
  const num = parseFloat(percent);
  if (isNaN(num)) return percent;
  return num.toFixed(2);
};

// 格式化筹码集中度（AKShare值0~1，转为百分比）(1位小数)
const formatChipConcentration = (concentration) => {
  if (!concentration && concentration !== 0) return '--';
  const num = parseFloat(concentration);
  if (isNaN(num)) return concentration;
  return (num * 100).toFixed(1);
};

// 格式化成交量
const formatVolume = (volume) => {
  if (!volume || volume === '--') return '--';
  const num = parseFloat(volume);
  if (isNaN(num)) return volume;
  if (num >= 100000000) {
    return (num / 100000000).toFixed(2) + '亿';
  } else if (num >= 10000) {
    return (num / 10000).toFixed(2) + '万';
  }
  return num.toString();
};

// 格式化成交额
const formatTurnover = (turnover) => {
  if (!turnover || turnover === '--') return '--';
  const num = parseFloat(turnover);
  if (isNaN(num)) return turnover;
  if (num >= 100000000) {
    return (num / 100000000).toFixed(2) + '亿';
  } else if (num >= 10000) {
    return (num / 10000).toFixed(2) + '万';
  }
  return num.toString();
};

// 计算均线折线图数据点
const getMaChartData = () => {
  if (!analysisOverview.value?.movingAverages) return [];
  return analysisOverview.value.movingAverages.map(ma => ({
    label: ma.label,
    value: parseFloat(ma.value) || 0,
    color: getMaColor(ma.label)
  }));
};

// 生成均线折线图点坐标
const getMaLinePoints = () => {
  const data = getMaChartData();
  if (data.length === 0) return '';
  
  const values = data.map(d => d.value);
  const min = Math.min(...values) * 0.95;
  const max = Math.max(...values) * 1.05;
  const range = max - min || 1;
  
  return data.map((d, i) => {
    const x = 30 + (i * (140 / (data.length - 1)));
    const y = 70 - ((d.value - min) / range) * 60;
    return `${x},${y}`;
  }).join(' ');
};

// 生成均线折线图点数组
const getMaLinePointsArray = () => {
  const data = getMaChartData();
  if (data.length === 0) return [];
  
  const values = data.map(d => d.value);
  const min = Math.min(...values) * 0.95;
  const max = Math.max(...values) * 1.05;
  const range = max - min || 1;
  
  return data.map((d, i) => ({
    x: 30 + (i * (140 / (data.length - 1))),
    y: 70 - ((d.value - min) / range) * 60,
    value: d.value,
    label: d.label
  }));
};

// 获取均线颜色
const getMaColor = (label) => {
  const colors = {
    'MA5': '#1a1a1a',
    'MA20': '#7c3aed',
    'MA60': '#ca8a04',
    'ma5': '#1a1a1a',
    'ma20': '#7c3aed',
    'ma60': '#ca8a04'
  };
  return colors[label] || '#666';
};

// 格式化时间戳
const formatTimestamp = (timestamp) => {
  if (!timestamp) return '--';
  try {
    return new Date(timestamp).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  } catch {
    return timestamp;
  }
};

// 获取相关性评分颜色
const getCorrelationColor = (score) => {
  const num = parseFloat(score);
  if (num >= 80) return 'green';
  if (num >= 60) return 'blue';
  if (num >= 40) return 'yellow';
  return 'gray';
};

const getStatusText = (status) => {
  const texts = {
    'ANALYZING': t('home.analyzing'),
    'COMPLETED': t('home.completed'),
    'FAILED': t('home.failed')
  };
  return texts[status] || status;
};

const refreshRecords = async () => {
  await loadAnalysisRecords();
  await loadQueueStatus();
  
  // 检查是否需要启动或停止轮询
  checkPollingStatus();
};

// 检查轮询状态，实现定时轮询
const checkPollingStatus = () => {
  // 如果轮询未启动，则启动轮询
  if (!pollingTimer) {
    pollingTimer = setInterval(() => {
      loadAnalysisRecords();
      loadQueueStatus();
    }, 30000); // 30秒间隔
    console.log('定时轮询已启动，间隔30秒');
  }
};

// 停止轮询
const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
    console.log('轮询已停止');
  }
};

onMounted(() => {
  loadAnalysisRecords();
  loadQueueStatus();
  
  // 页面加载完成后自动启动定时轮询
  setTimeout(() => {
    checkPollingStatus();
  }, 100);
});

onUnmounted(() => {
  stopPolling();
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
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.queue-status {
  font-size: 12px;
  color: #666;
}

.queue-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.queue-icon {
  font-size: 12px;
  color: #1890ff;
}

.pending-count {
  color: #faad14;
}

/* 状态小点 */
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  flex-shrink: 0;
}

.status-dot.analyzing {
  background-color: #1890ff;
  box-shadow: 0 0 6px rgba(24, 144, 255, 0.6);
  animation: pulse-blue 2s infinite;
}

.loading-spinner {
  margin-right: 8px;
  font-size: 14px;
  color: #1890ff;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-dot.completed {
  background-color: #52c41a;
  box-shadow: 0 0 6px rgba(82, 196, 26, 0.6);
}

.status-dot.failed {
  background-color: #f5222d;
  box-shadow: 0 0 6px rgba(245, 34, 45, 0.6);
}

@keyframes pulse-blue {
  0%, 100% {
    box-shadow: 0 0 6px rgba(24, 144, 255, 0.6);
  }
  50% {
    box-shadow: 0 0 12px rgba(24, 144, 255, 0.9);
  }
}

/* 分析中项目样式 */
.analyzing-item {
  background: #fffbe6 !important;
  border-left: 3px solid #faad14 !important;
  cursor: not-allowed !important;
}

.analyzing-item:hover {
  background: #fffbe6 !important;
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

.delete-btn {
 opacity: 0;
 transition: opacity 0.3s;
}

.records-list :deep(.ant-list-item:hover) .delete-btn {
 opacity: 1;
}

.delete-btn:hover {
 background: #fff2f0;
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
  min-height: 500px;
}

.top-section {
  flex-shrink: 0;
}

.analysis-form-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
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
  flex-wrap: wrap;
}

.form-item-inline {
  flex: 1;
  min-width: 200px;
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
  min-width: 200px;
}

.stock-input-short {
  width: 220px;
  max-width: 280px;
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
  flex-wrap: wrap;
}

.button-row .ant-btn {
  min-width: 120px;
}

/* 水平布局表单 */
.analysis-form-inline {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.form-item-compact {
  margin-bottom: 0;
}

.stock-input-inline {
  width: 220px;
  max-width: 280px;
}

/* 右下：分析结果概览 */
.bottom-section {
  flex: 1;
  overflow-y: auto;
  min-height: 300px;
}

.result-card {
  height: 100%;
  min-height: 300px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.result-card :deep(.ant-card-body) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.overview-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  max-width: 1100px;
  margin: 0 auto;
}

/* ===== 双栏布局容器 ===== */
.overview-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 16px;
}

.overview-col {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

/* ===== 价格概览条（Hero） ===== */
.price-hero {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  border-radius: 14px;
  padding: 20px 24px;
  margin-bottom: 16px;
  color: #fff;
}

.price-hero-main {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.price-hero-current {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 120px;
}

.ph-label {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.ph-price {
  font-size: 36px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -1px;
}

.ph-price.up { color: #ff4757; }
.ph-price.down { color: #2ed573; }

.price-hero-change {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding-left: 24px;
  border-left: 1px solid rgba(255,255,255,0.15);
}

.ph-change-pct {
  font-size: 24px;
  font-weight: 700;
}
.ph-change-pct.up { color: #ff4757; }
.ph-change-pct.down { color: #2ed573; }

.ph-change-amt {
  font-size: 13px;
  opacity: 0.8;
}
.ph-change-amt.up { color: #ff4757; }
.ph-change-amt.down { color: #2ed573; }

.price-hero-stats {
  display: flex;
  gap: 16px;
  padding-left: 24px;
  border-left: 1px solid rgba(255,255,255,0.15);
}

.ph-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.ph-stat-label {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
}

.ph-stat-value {
  font-size: 16px;
  font-weight: 600;
}
.ph-stat-value.high { color: #ff4757; }
.ph-stat-value.low { color: #2ed573; }

.price-hero-volume {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-left: 24px;
  border-left: 1px solid rgba(255,255,255,0.15);
  flex: 1;
  flex-wrap: wrap;
}

.ph-vol-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.ph-vol-label {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
}

.ph-vol-value {
  font-size: 15px;
  font-weight: 600;
}

.ph-vol-divider {
  width: 1px;
  height: 28px;
  background: rgba(255,255,255,0.1);
}

/* ===== 技术面关键指标 - 均线条形图 ===== */
.ma-section-block {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  border: 1px solid #e8e8e8;
}

.ma-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.ma-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ma-bar-label {
  font-size: 13px;
  font-weight: 600;
  color: #555;
  width: 44px;
  flex-shrink: 0;
}

.ma-bar-track {
  flex: 1;
  height: 20px;
  background: #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

.ma-bar-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 0.5s ease;
  min-width: 4px;
}

.ma-bar-fill.ma5 { background: linear-gradient(90deg, #1a1a1a, #444444); }
.ma-bar-fill.ma20 { background: linear-gradient(90deg, #7c3aed, #a78bfa); }
.ma-bar-fill.ma60 { background: linear-gradient(90deg, #ca8a04, #eab308); }

.ma-bar-value {
  font-size: 14px;
  font-weight: 700;
  color: #333;
  width: 70px;
  text-align: right;
  flex-shrink: 0;
}

/* 支撑 / 压力位卡片 */
.sr-levels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.sr-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px 14px;
}

.sr-card.support { border-left: 4px solid #52c41a; }
.sr-card.resistance { border-left: 4px solid #f5222d; }

.sr-icon {
  font-size: 22px;
}

.sr-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sr-label {
  font-size: 12px;
  color: #999;
}

.sr-value {
  font-size: 18px;
  font-weight: 700;
}
.sr-card.support .sr-value { color: #52c41a; }
.sr-card.resistance .sr-value { color: #f5222d; }

/* ===== 筹码分布 - 获利/套牢突出展示 ===== */
.chip-profit-loss {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 14px;
}

.cpl-card {
  text-align: center;
  border-radius: 12px;
  padding: 16px;
}

.cpl-card.profit {
  background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
}

.cpl-card.loss {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
}

.cpl-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}

.cpl-value {
  font-size: 28px;
  font-weight: 800;
}

.cpl-card.profit .cpl-value { color: #f5222d; }
.cpl-card.loss .cpl-value { color: #52c41a; }

.chip-metrics {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.cm-label {
  font-size: 12px;
  color: #999;
}

.cm-value {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.cm-hint {
  font-size: 12px;
  font-weight: 400;
  color: #666;
  text-align: right;
  max-width: 60%;
}

/* ===== 作战计划 ===== */
.bp-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.bp-card {
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px;
  text-align: center;
  border-top: 3px solid transparent;
}

.bp-card.ideal { border-top-color: #1890ff; background: #f0f5ff; }
.bp-card.suboptimal { border-top-color: #faad14; background: #fffbe6; }
.bp-card.stoploss { border-top-color: #f5222d; background: #fff1f0; }
.bp-card.target { border-top-color: #52c41a; background: #f6ffed; }

.bp-header {
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
}

.bp-price {
  font-size: 20px;
  font-weight: 700;
  color: #333;
}

.bp-desc {
  font-size: 11px;
  color: #888;
  margin-top: 6px;
  line-height: 1.4;
}

.bp-rr {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px dashed #e8e8e8;
}

.bp-rr-label {
  font-size: 13px;
  color: #999;
}

.bp-rr-value {
  font-size: 15px;
  font-weight: 700;
  color: #1890ff;
}

/* 通用区块 */
.section {
  margin-bottom: 14px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

/* 核心洞察 */
.core-insights {
  background: linear-gradient(135deg, #fffbe6 0%, #fff3c4 100%);
  border-radius: 12px;
  padding: 16px;
}

.insights-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.insight-icon {
  width: 24px;
  height: 24px;
  background: #faad14;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
}

.insight-content {
  flex: 1;
}

.insight-title {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
}

.insight-metric {
  font-size: 12px;
  color: #faad14;
  margin-top: 4px;
}

/* 核心洞察文本 */
.core-insight {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  border: 1px solid #e8e8e8;
  border-left: 3px solid #faad14;
}

.core-insight-text {
  font-size: 14px;
  line-height: 1.7;
  color: #333;
}

.core-insight-text p {
  margin: 0 0 6px;
}

.core-insight-text p:last-child {
  margin-bottom: 0;
}

/* 关联板块 */
.related-sectors {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  border: 1px solid #e8e8e8;
}

.sectors-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.sector-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.sectors-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
}

.sector-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.sector-card:hover {
  background: #e6f7ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.sector-name {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.sector-performance {
  font-size: 16px;
  font-weight: bold;
}

.sector-performance.up {
  color: #52c41a;
}

.sector-performance.down {
  color: #f5222d;
}

.sector-correlation {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.sector-linkage {
  font-size: 11px;
  color: #1890ff;
  margin-top: 4px;
  white-space: normal;
  line-height: 1.4;
}

.sector-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 相关资讯列表 - 紧凑型样式 */
.news-list-compact {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.news-item-compact {
  background: #ffffff;
  border-radius: 8px;
  padding: 14px;
  border: 1px solid #e8e8e8;
  transition: all 0.3s ease;
}

.news-item-compact:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}

.news-title-compact {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 6px;
  display: flex;
  align-items: flex-start;
  line-height: 1.5;
}

.news-num {
  color: #1890ff;
  font-weight: bold;
  margin-right: 6px;
  flex-shrink: 0;
}

.news-summary-compact {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-meta-compact {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #999;
}

.news-source-compact {
  padding: 1px 6px;
  background: #f5f5f5;
  border-radius: 3px;
}

.news-time-compact {
  display: flex;
  align-items: center;
}

.news-link-compact {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #1890ff;
  text-decoration: none;
  padding: 4px 10px;
  background: #e6f7ff;
  border-radius: 4px;
  transition: all 0.3s ease;
  margin-top: 8px;
}

.news-link-compact:hover {
  background: #1890ff;
  color: #ffffff;
  text-decoration: none;
}

/* 新闻列表模块样式 */
.news-section {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  border: 1px solid #e8e8e8;
}

.news-number-compact {
  color: #1890ff;
  font-weight: bold;
  margin-right: 6px;
  flex-shrink: 0;
}

.news-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #1890ff;
  text-decoration: none;
  padding: 4px 10px;
  background: #e6f7ff;
  border-radius: 4px;
  transition: all 0.3s ease;
  margin-top: 8px;
}

.news-link:hover {
  background: #1890ff;
  color: #ffffff;
  text-decoration: none;
}

.view-more-news {
  margin-top: 12px;
  text-align: right;
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
.detail-modal :deep(.ant-modal) {
  max-height: 90vh;
}

.detail-modal :deep(.ant-modal-content) {
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.detail-modal :deep(.ant-modal-body) {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.detail-modal :deep(.ant-modal-header) {
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-modal :deep(.ant-modal-title) {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.detail-content {
  max-height: 75vh;
  overflow-y: auto;
  padding-right: 8px;
}

.detail-content::-webkit-scrollbar {
  width: 6px;
}

.detail-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.detail-content::-webkit-scrollbar-thumb {
  background: #bfbfbf;
  border-radius: 3px;
}

.detail-content::-webkit-scrollbar-thumb:hover {
  background: #999;
}

.detail-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.detail-section h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 新闻列表样式 */
.news-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.news-item {
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e8e8e8;
  transition: all 0.3s ease;
}

.news-item:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}

.news-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 8px;
  display: flex;
  align-items: flex-start;
  line-height: 1.5;
}

.news-number {
  color: #1890ff;
  font-weight: bold;
  margin-right: 8px;
  flex-shrink: 0;
}

.news-summary {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.news-source {
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 4px;
}

.news-time {
  display: flex;
  align-items: center;
}

.news-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #1890ff;
  text-decoration: none;
  padding: 6px 12px;
  background: #e6f7ff;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.news-link:hover {
  background: #1890ff;
  color: #ffffff;
  text-decoration: none;
}

.news-summary-text {
  background: #fafafa;
  padding: 14px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #666;
}

.detail-meta {
  display: flex;
  gap: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #8c8c8c;
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
  
  .current-price {
    font-size: 28px;
  }
}

/* 分析概览模块样式 - 重构：紧凑布局 */

/* 实时行情 */
.real-time-market {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  border: 1px solid #e8e8e8;
}

.market-price-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e8e8e8;
  margin-bottom: 12px;
}

.price-label {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
}

.price-group {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.price-value.up {
  color: #f5222d;
}

.price-value.down {
  color: #52c41a;
}

.change-value {
  font-size: 18px;
  font-weight: 600;
}

.change-value.up {
  color: #f5222d;
}

.change-value.down {
  color: #52c41a;
}

.change-amount {
  font-size: 13px;
  color: #999;
}

.change-amount.up {
  color: #f5222d;
}

.change-amount.down {
  color: #52c41a;
}

.market-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.market-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px;
  text-align: center;
}

.market-label {
  font-size: 12px;
  color: #999;
  display: block;
  margin-bottom: 4px;
}

.market-value {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.market-value.high {
  color: #f5222d;
}

.market-value.low {
  color: #52c41a;
}

/* 数据透视 */
.data-pivot {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  border: 1px solid #e8e8e8;
}

.pivot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.pivot-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.pivot-card.support {
  border-left: 3px solid #52c41a;
}

.pivot-card.resistance {
  border-left: 3px solid #f5222d;
}

.pivot-label {
  font-size: 12px;
  color: #999;
  display: block;
  margin-bottom: 4px;
}

.pivot-value {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.pivot-value.up {
  color: #f5222d;
}

.pivot-value.down {
  color: #52c41a;
}

/* 筹码分布（独立区域） */
.chip-section {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  border: 1px solid #e8e8e8;
}

.chip-overview {
  margin-top: 10px;
}

.chip-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.chip-metric-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.chip-metric-label {
  font-size: 12px;
  color: #999;
  display: block;
  margin-bottom: 4px;
}

.chip-metric-value {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.chip-metric-value.chip-range-value {
  font-size: 13px;
}

.chip-metric-value.chip-profit {
  color: #f5222d;
}

.chip-metric-value.chip-loss {
  color: #52c41a;
}

.chip-distribution {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 13px;
}

.chip-distribution-label {
  font-weight: 500;
  color: #666;
}

.chip-distribution-text {
  color: #333;
}

.chip-extra {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f0f5ff;
  border-radius: 8px;
  border-left: 3px solid #1890ff;
}

.chip-extra-desc {
  font-size: 13px;
  color: #555;
  line-height: 1.7;
}

/* 作战计划 */
.battle-plan {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  border: 1px solid #e8e8e8;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.plan-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  border-left: 4px solid;
}

.plan-card.ideal {
  border-color: #1890ff;
}

.plan-card.suboptimal {
  border-color: #faad14;
}

.plan-card.stoploss {
  border-color: #f5222d;
}

.plan-card.target {
  border-color: #52c41a;
}

.plan-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.plan-icon {
  font-size: 16px;
}

.plan-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.plan-price {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin-bottom: 6px;
}

.plan-desc {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}

.risk-reward-info {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e8e8e8;
}

.rr-label {
  font-size: 13px;
  color: #666;
}

.rr-value {
  font-size: 14px;
  font-weight: 600;
  color: #1890ff;
}





/* 响应式适配 */
@media (max-width: 1024px) {
  .overview-columns {
    grid-template-columns: 1fr;
  }

  .price-hero-main {
    gap: 16px;
  }

  .price-hero-stats,
  .price-hero-change,
  .price-hero-volume {
    padding-left: 0;
    border-left: none;
    width: 100%;
  }

  .price-hero-stats {
    justify-content: space-around;
  }

  .price-hero-volume {
    justify-content: space-around;
    padding-top: 12px;
    border-top: 1px solid rgba(255,255,255,0.1);
  }
}

@media (max-width: 768px) {
  .market-grid,
  .market-detail-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .pivot-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .chip-metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .plan-grid,
  .bp-grid {
    grid-template-columns: 1fr;
  }

  .sr-levels {
    grid-template-columns: 1fr;
  }

  .ph-price {
    font-size: 28px;
  }

  .ph-change-pct {
    font-size: 20px;
  }

  .cpl-value {
    font-size: 22px;
  }
}

@media (max-width: 1100px) {
</style>

<style>
/* Markdown内容样式 - 非 scoped，因为 v-html 不会添加 data-v 属性 */
.markdown-content {
  background: #ffffff;
  padding: 20px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.core-insight-text .markdown-content {
  background: transparent;
  padding: 0;
}

.markdown-content h1 {
  font-size: 24px;
  font-weight: bold;
  color: #1a1a1a;
  margin: 24px 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #1890ff;
}

.markdown-content h2 {
  font-size: 20px;
  font-weight: bold;
  color: #262626;
  margin: 20px 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.markdown-content h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 16px 0 10px 0;
}

.markdown-content h4 {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 14px 0 8px 0;
}

.markdown-content h5,
.markdown-content h6 {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 12px 0 6px 0;
}

.markdown-content p {
  margin: 12px 0;
  line-height: 1.8;
}

.markdown-content ul,
.markdown-content ol {
  padding-left: 28px;
  margin: 12px 0;
}

.markdown-content ul {
  list-style-type: disc;
}

.markdown-content ol {
  list-style-type: decimal;
}

.markdown-content li {
  margin: 8px 0;
  line-height: 1.7;
}

.markdown-content blockquote {
  border-left: 4px solid #1890ff;
  padding: 12px 20px;
  margin: 16px 0;
  background: #f6f8fa;
  color: #555;
  font-style: italic;
  border-radius: 0 4px 4px 0;
}

.markdown-content code {
  background: #f4f4f4;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Monaco', 'Menlo', 'Consolas', 'Courier New', monospace;
  color: #c7254e;
  line-height: 1.5;
}

.markdown-content pre {
  background: #2d2d2d;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 16px 0;
  border: 1px solid #3d3d3d;
}

.markdown-content pre code {
  background: none;
  padding: 0;
  color: #e8e8e8;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'Monaco', 'Menlo', 'Consolas', 'Courier New', monospace;
}

.markdown-content a {
  color: #1890ff;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.3s ease;
}

.markdown-content a:hover {
  color: #40a9ff;
  border-bottom-color: #40a9ff;
}

.markdown-content strong {
  font-weight: bold;
  color: #1a1a1a;
}

.markdown-content em,
.markdown-content i {
  font-style: italic;
  color: #555;
}

.markdown-content hr {
  border: none;
  border-top: 1px solid #e8e8e8;
  margin: 24px 0;
}

.markdown-content table {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  font-size: 13px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.markdown-content th,
.markdown-content td {
  border: 1px solid #e8e8e8;
  padding: 10px 14px;
  text-align: left;
}

.markdown-content th {
  background: #fafafa;
  font-weight: 600;
  color: #1a1a1a;
}

.markdown-content tr:nth-child(even) {
  background: #fafafa;
}

.markdown-content tr:hover {
  background: #e6f7ff;
}

.markdown-content img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 12px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.markdown-content details {
  margin: 12px 0;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
}

.markdown-content summary {
  cursor: pointer;
  font-weight: 600;
  color: #1890ff;
  padding: 4px 0;
}

.markdown-content summary:hover {
  color: #40a9ff;
}

.markdown-content del {
  color: #999;
  text-decoration: line-through;
}

.markdown-content mark {
  background: #fff3cd;
  padding: 2px 4px;
  border-radius: 2px;
}
</style>  
