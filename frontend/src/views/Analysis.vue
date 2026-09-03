<template>
  <div class="analysis-page">
    <div class="analysis-layout">
      <!-- 左侧：分析记录列表 -->
      <div class="left-panel">
        <a-card :title="$t('analysis.records')" :bordered="false" class="records-card" :body-style="{ flex: '1', overflow: 'hidden', display: 'flex', flexDirection: 'column' }">
          <template #extra>
            <a-button type="text" size="small" class="records-refresh-button" @click="refreshRecords">
              <ReloadOutlined /> {{ $t('analysis.refresh') }}
            </a-button>
          </template>

          <!-- 分析记录列表 -->
          <div class="records-list" v-if="analysisRecords.length > 0" @scroll="handleScroll">
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
                      <div class="record-title">
                        <LoadingOutlined v-if="item.status === 'ANALYZING'" class="loading-spinner" />
                        <span v-else :class="['status-dot', item.status.toLowerCase()]"></span>
                        <span class="stock-code">{{ item.stockCode }}</span>
                        <span class="stock-name">{{ item.stockName }}</span>
                        <a-tag v-if="item.isHolding" color="blue" size="small" class="holding-tag">{{ $t('analysis.holdingTag') }}</a-tag>
                      </div>
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
            <div v-if="allLoaded" class="end-hint end-loaded">—— 已经到底了 ——</div>
            <div v-else-if="loadingMore" class="end-hint"><LoadingOutlined /> 加载中...</div>
            <div v-else class="end-hint end-more" @click="loadMore(false)">↓ 点击加载更多</div>
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
            :bordered="false"
            class="result-card"
          >
            <template #title>
              <div class="brief-heading">
                <div>
                  <p class="brief-kicker">{{ $t('analysis.researchBrief') }}</p>
                  <div class="brief-identity">
                    <span>{{ selectedRecord.stockCode }}</span>
                    <strong>{{ selectedRecord.stockName }}</strong>
                  </div>
                </div>
                <time>{{ formatTime(selectedRecord.createTime) }}</time>
              </div>
            </template>
            <template #extra>
              <a-space class="brief-actions">
                <a-button
                  v-if="selectedRecord?.status === 'COMPLETED'"
                  size="small"
                  @click="handlePushToFeishu"
                  :loading="pushingFeishu"
                >
                  <SendOutlined /> {{ $t('analysis.pushToFeishu') }}
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
                  <div class="price-hero-stock">
                    <div class="ph-stock-code">{{ selectedRecord?.stockCode }}</div>
                    <div class="ph-stock-name">{{ selectedRecord?.stockName }}</div>
                  </div>
                </div>
              </div>

              <!-- ===== 三栏：核心洞察/板块 | 技术面/筹码/作战 | 十大股东/新闻 ===== -->
              <div class="overview-cols">
                <div class="overview-col">
                  <div class="overview-section" v-if="analysisOverview.coreInsight">
                    <div class="section-title">
                      <InboxOutlined /> {{ $t('analysis.coreInsight') }}
                    </div>
                    <div class="core-insight-text markdown-content" v-html="renderMarkdown(analysisOverview.coreInsight)"></div>
                  </div>
                  <div class="overview-section" v-if="analysisOverview.relatedSectors?.length">
                    <div class="section-title">
                      <AppstoreOutlined /> {{ $t('analysis.relatedSectors') }}
                    </div>
                    <div class="sectors-inline">
                      <a-tag v-for="(sector, idx) in analysisOverview.relatedSectors" :key="idx" color="blue">{{ sector }}</a-tag>
                    </div>
                  </div>
                </div>

                <div class="overview-col">
                  <div class="overview-section" v-if="analysisOverview.dataPivot">
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
                    <div class="macd-section" v-if="analysisOverview.dataPivot.macdSignal">
                      <div class="macd-header">{{ $t('analysis.macdSignal') }}</div>
                      <div class="macd-grid">
                        <div class="macd-item">
                          <span class="macd-label">{{ $t('analysis.macdDif') }}</span>
                          <span class="macd-value" :class="(analysisOverview.dataPivot.macdDif ?? 0) >= 0 ? 'macd-up' : 'macd-down'">{{ formatPrice(analysisOverview.dataPivot.macdDif) }}</span>
                        </div>
                        <div class="macd-item">
                          <span class="macd-label">{{ $t('analysis.macdDea') }}</span>
                          <span class="macd-value" :class="(analysisOverview.dataPivot.macdDea ?? 0) >= 0 ? 'macd-up' : 'macd-down'">{{ formatPrice(analysisOverview.dataPivot.macdDea) }}</span>
                        </div>
                        <div class="macd-item">
                          <span class="macd-label">{{ $t('analysis.macdBar') }}</span>
                          <span class="macd-value" :class="(analysisOverview.dataPivot.macdBar ?? 0) >= 0 ? 'macd-up' : 'macd-down'">{{ formatPrice(analysisOverview.dataPivot.macdBar) }}</span>
                        </div>
                      </div>
                      <div class="macd-signal-text">{{ analysisOverview.dataPivot.macdSignal }}</div>
                    </div>
                  </div>

                  <div class="overview-section" v-if="analysisOverview.battlePlan">
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

                  <div class="overview-section" v-if="analysisOverview.dataPivot">
                    <div class="section-title">
                      <PieChartOutlined /> {{ $t('analysis.chipDistribution') }}
                    </div>
                    <div class="chip-compact">
                      <div class="cc-pl">
                        <span class="cc-pl-item cc-profit">{{ $t('analysis.profitChips') }} +{{ formatPercent(analysisOverview.dataPivot.profitRatio) }}%</span>
                        <span class="cc-pl-divider">|</span>
                        <span class="cc-pl-item cc-loss">{{ $t('analysis.lossChips') }} -{{ formatPercent(analysisOverview.dataPivot.lossRatio) }}%</span>
                      </div>
                      <div class="cc-grid">
                        <div class="cc-cell" v-if="analysisOverview.dataPivot.avgCostPrice">
                          <span class="cc-label">{{ $t('analysis.avgCost') }}</span>
                          <span class="cc-value">¥{{ formatPrice(analysisOverview.dataPivot.avgCostPrice) }}</span>
                        </div>
                        <div class="cc-cell">
                          <span class="cc-label">{{ $t('analysis.chipConcentration') }}</span>
                          <span class="cc-value">{{ formatPercent(analysisOverview.dataPivot.chipConcentration) }}%</span>
                        </div>
                        <div class="cc-cell" v-if="analysisOverview.dataPivot.cost90Low">
                          <span class="cc-label">{{ $t('analysis.cost90Range') }}</span>
                          <span class="cc-value">{{ formatPrice(analysisOverview.dataPivot.cost90Low) }}~{{ formatPrice(analysisOverview.dataPivot.cost90High) }}</span>
                        </div>
                        <div class="cc-cell" v-if="analysisOverview.dataPivot.cost70Low">
                          <span class="cc-label">{{ $t('analysis.cost70Range') }}</span>
                          <span class="cc-value">{{ formatPrice(analysisOverview.dataPivot.cost70Low) }}~{{ formatPrice(analysisOverview.dataPivot.cost70High) }}</span>
                        </div>
                      </div>
                      <div class="cc-summary" v-if="analysisOverview.dataPivot.chipSummary">
                        {{ analysisOverview.dataPivot.chipSummary }}
                      </div>
                    </div>
                  </div>
                </div>

                <div class="overview-col">
                  <div class="overview-section" v-if="analysisOverview.topFreeShareholdersData">
                    <div class="section-title">
                      <TeamOutlined /> {{ $t('analysis.topFreeShareholders') }}
                    </div>
                    <div class="shareholder-table">
                      <div class="sh-row sh-header">
                        <span class="sh-col-rank">{{ $t('analysis.rank') }}</span>
                        <span class="sh-col-name">{{ $t('analysis.holderName') }}</span>
                        <span class="sh-col-type">{{ $t('analysis.holderType') }}</span>
                        <span class="sh-col-share-type">{{ $t('analysis.shareType') }}</span>
                        <span class="sh-col-holdnum">{{ $t('analysis.holdNum') }}</span>
                        <span class="sh-col-ratio">{{ $t('analysis.freeHoldRatio') }}</span>
                        <span class="sh-col-change">{{ $t('analysis.changeNum') }}</span>
                        <span class="sh-col-change-ratio">{{ $t('analysis.changeRatio') }}</span>
                      </div>
                      <div
                        v-for="(holder, idx) in getTopShareholders(analysisOverview.topFreeShareholdersData)"
                        :key="idx"
                        class="sh-row"
                      >
                        <span class="sh-col-rank">{{ holder.rank }}</span>
                        <span class="sh-col-name">{{ holder.holder_name }}</span>
                        <span class="sh-col-type">
                          <a-tag :color="getHolderTypeColor(holder.holder_type)" class="sh-type-tag">{{ holder.holder_type }}</a-tag>
                        </span>
                        <span class="sh-col-share-type">{{ holder.share_type || '-' }}</span>
                        <span class="sh-col-holdnum">{{ formatLargeNum(holder.hold_num) }}</span>
                        <span class="sh-col-ratio">{{ formatPercent(holder.free_holdnum_ratio) }}%</span>
                        <span class="sh-col-change" :class="getChangeClass(holder.change_num)">{{ formatChangeNum(holder.change_num) }}</span>
                        <span class="sh-col-change-ratio" :class="getChangeClass(holder.change_num)">{{ formatChangeRatio(holder.change_ratio, holder.change_num) }}</span>
                      </div>
                    </div>
                    <div class="shareholder-analysis" v-if="analysisOverview.topFreeShareholdersAnalysis">
                      {{ analysisOverview.topFreeShareholdersAnalysis }}
                    </div>
                  </div>

                  <div class="overview-section" v-if="analysisOverview.newsList?.length">
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
import {computed, onMounted, onUnmounted, reactive, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {marked} from 'marked';
import {useI18n} from 'vue-i18n';
import {
  createAnalysis,
  deleteAnalysis,
  getAnalysisById,
  getAnalysisCursor,
  getAnalysisDetail,
  getNotificationConfigs,
  getQueueStatus,
  pushToFeishu as pushToFeishuApi,
  searchStocks
} from '../api';
import {message} from 'ant-design-vue';
import {
  AimOutlined,
  AppstoreOutlined,
  BarChartOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  FieldTimeOutlined,
  FileTextOutlined,
  InboxOutlined,
  LinkOutlined,
  LoadingOutlined,
  MessageOutlined,
  PieChartOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  RightOutlined,
  SearchOutlined,
  SendOutlined,
  TeamOutlined
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
const route = useRoute();
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
const stockOptions = ref([]);
const queueStatus = ref(null);
const cursor = ref(null);
const hasMore = ref(true);
const loadingMore = ref(false);
const allLoaded = ref(false);

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
    await loadMore(true);
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

const loadMore = async (reset = false) => {
  if (!reset && (!hasMore.value || loadingMore.value)) return;
  if (reset) {
    cursor.value = null;
    hasMore.value = true;
    allLoaded.value = false;
  }
  loadingMore.value = true;
  try {
    const response = await getAnalysisCursor(cursor.value);
    const result = response.data;
    const items = result.items || [];
    if (reset) {
      analysisRecords.value = items;
    } else {
      analysisRecords.value = analysisRecords.value.concat(items);
    }
    hasMore.value = result.hasMore;
    cursor.value = result.nextCursor;
    if (!hasMore.value) {
      allLoaded.value = true;
    }
  }
  catch (error) {
    console.error('加载分析记录失败:', error);
  }
  finally {
    loadingMore.value = false;
  }
};

const handleScroll = (e) => {
  const el = e.target;
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 50) {
    loadMore(false);
  }
};

// 轮询时仅刷新第一页，合并状态变化和新记录
const pollRefresh = async () => {
  try {
    const response = await getAnalysisCursor(null, 10);
    const result = response.data;
    const freshItems = result.items || [];
    if (freshItems.length === 0) return;
    const existingIds = new Set(analysisRecords.value.map(r => r.id));
    let changed = false;
    // 更新已有记录的状态
    for (const fresh of freshItems) {
      const index = analysisRecords.value.findIndex(r => r.id === fresh.id);
      if (index >= 0) {
        if (analysisRecords.value[index].status !== fresh.status) {
          analysisRecords.value[index] = fresh;
          changed = true;
        }
      }
    }
    // 将新记录（不在现有列表中的）添加到顶部
    const newItems = freshItems.filter(f => !existingIds.has(f.id));
    if (newItems.length > 0) {
      analysisRecords.value = newItems.concat(analysisRecords.value);
      changed = true;
    }
    if (changed) {
      console.log('分析记录数据已更新');
    }
  } catch (error) {
    console.error('轮询刷新失败:', error);
  }
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
      checkList: parsedOverview.checkList || null,
      topFreeShareholdersData: parsedOverview.topFreeShareholdersData || null,
      topFreeShareholdersAnalysis: parsedOverview.topFreeShareholdersAnalysis || ''
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

// 解析十大流通股东数据
const getTopShareholders = (data) => {
  if (!data) return [];
  try {
    const parsed = typeof data === 'string' ? JSON.parse(data) : data;
    const list = parsed?.data || [];
    return list.slice(0, 10);
  } catch {
    return [];
  }
};

// 股东类型颜色（仅区分个人与机构）
const getHolderTypeColor = (type) => {
  if (!type) return 'default';
  if (type === '个人') return 'green';
  return 'blue';
};

// 格式化大数字为 亿/万
const formatLargeNum = (num) => {
  if (num === undefined || num === null) return '-';
  const n = Number(num);
  if (isNaN(n)) return '-';
  const abs = Math.abs(n);
  if (abs >= 1e8) {
    return (n / 1e8).toFixed(2) + '亿';
  }
  if (abs >= 1e4) {
    return (n / 1e4).toFixed(2) + '万';
  }
  return String(n);
};

// 判断变动是否为 NaN 或无意义
const isChangeUnchanged = (changeNum) => {
  if (!changeNum || changeNum === '0' || changeNum === '--') return true;
  const s = String(changeNum).toLowerCase().replace(/,/g, '').trim();
  return s === '' || s === '0' || s === 'nan' || s === '--';
};

// 变动方向样式
const getChangeClass = (changeNum) => {
  if (isChangeUnchanged(changeNum)) return '';
  const n = parseFloat(String(changeNum).replace(/,/g, ''));
  if (n > 0) return 'change-up';
  if (n < 0) return 'change-down';
  return '';
};

// 格式化变动数量（含单位简化）
const formatChangeNum = (changeNum) => {
  if (isChangeUnchanged(changeNum)) return '不变';
  const num = parseFloat(String(changeNum).replace(/,/g, ''));
  const prefix = num > 0 ? '+' : '';
  return prefix + formatLargeNum(Math.abs(num));
};

// 格式化变动比率
const formatChangeRatio = (ratio, changeNum) => {
  if (isChangeUnchanged(changeNum)) return '不变';
  if (ratio === undefined || ratio === null) return '不变';
  const r = Number(ratio);
  if (isNaN(r) || r === 0) return '不变';
  const prefix = r > 0 ? '+' : '';
  return prefix + r.toFixed(2) + '%';
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
  await loadMore(true);
  await loadQueueStatus();
  
  // 检查是否需要启动或停止轮询
  checkPollingStatus();
};

// 检查轮询状态，实现定时轮询
const checkPollingStatus = () => {
  // 如果轮询未启动，则启动轮询
  if (!pollingTimer) {
    pollingTimer = setInterval(() => {
      pollRefresh();
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
  loadMore(true);
  loadQueueStatus();

  // 从 query 参数中读取标的代码（来自持仓页面跳转）
  const stockCode = route.query.stockCode;
  if (stockCode) {
    form.stockCode = stockCode;
    handleStockSearch(stockCode);
  }

  // 从 query 参数中读取分析记录ID（来自首页跳转）
  const recordId = route.query.id;
  if (recordId) {
    loadMore(true).then(async () => {
      const found = analysisRecords.value.find(r => r.id === Number(recordId))
      if (found) {
        await selectRecord(found)
      } else {
        try {
          const res = await getAnalysisById(Number(recordId))
          if (res.data) {
            await selectRecord(res.data)
          }
        } catch {
          // ignore
        }
      }
    })
  }

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
  height: calc(100vh - 84px);
  overflow: hidden;
  background: #f5f7fa;
}

.analysis-layout {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 20px;
  height: 100%;
  overflow: hidden;
}

/* 左侧面板 */
.left-panel {
  height: 100%;
  overflow: hidden;
}

.records-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.records-card :deep(.ant-card-body) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
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
  min-height: 0;
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

.record-title {
  display: flex;
  align-items: center;
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

.holding-tag {
  margin-left: auto;
  margin-right: 3px;
  font-size: 11px;
  line-height: 18px;
  vertical-align: middle;
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

.end-hint {
  text-align: center;
  padding: 16px;
  color: #bbb;
  font-size: 13px;
}

.end-more {
  cursor: pointer;
  transition: color 0.2s;
}

.end-more:hover {
  color: #1890ff;
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
  height: 100%;
  min-height: 500px;
  overflow: hidden;
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
  padding: 6px 8px;
}

/* ===== 三栏布局 ===== */
.overview-cols {
  display: grid;
  grid-template-columns: 4fr 3fr 3fr;
  gap: 8px;
  align-items: start;
}

.overview-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-section {
  background: #fff;
  border-radius: 8px;
  padding: 10px;
  border: 1px solid #e8e8e8;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.sectors-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* ===== 价格概览条（Hero） ===== */
.price-hero {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  border-radius: 10px;
  margin: 0 0 8px 0;
  padding: 12px 24px;
  color: #fff;
}

.price-hero-stock {
  margin-left: auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.ph-stock-code {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}

.ph-stock-name {
  font-size: 12px;
  font-weight: 600;
  opacity: 0.85;
  line-height: 1.2;
}

.price-hero-main {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.price-hero-current {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 100px;
}

.ph-label {
  font-size: 11px;
  color: rgba(255,255,255,0.6);
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.ph-price {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -1px;
}

.ph-price.up { color: #ff4757; }
.ph-price.down { color: #006d2c; }

.price-hero-change {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding-left: 20px;
  border-left: 1px solid rgba(255,255,255,0.15);
}

.ph-change-pct {
  font-size: 20px;
  font-weight: 700;
}
.ph-change-pct.up { color: #ff4757; }
.ph-change-pct.down { color: #006d2c; }

.ph-change-amt {
  font-size: 12px;
  opacity: 0.8;
}
.ph-change-amt.up { color: #ff4757; }
.ph-change-amt.down { color: #006d2c; }

.price-hero-stats {
  display: flex;
  gap: 14px;
  padding-left: 20px;
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
  font-size: 15px;
  font-weight: 600;
}
.ph-stat-value.high { color: #ff4757; }
.ph-stat-value.low { color: #006d2c; }

.price-hero-volume {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 20px;
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
  font-size: 14px;
  font-weight: 600;
}

.ph-vol-divider {
  width: 1px;
  height: 24px;
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
  gap: 6px;
  margin-bottom: 10px;
}

.ma-bar-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ma-bar-label {
  font-size: 12px;
  font-weight: 600;
  color: #555;
  width: 40px;
  flex-shrink: 0;
}

.ma-bar-track {
  flex: 1;
  height: 16px;
  background: #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.ma-bar-fill {
  height: 100%;
  border-radius: 8px;
  transition: width 0.5s ease;
  min-width: 4px;
}

.ma-bar-fill.ma5 { background: linear-gradient(90deg, #1a1a1a, #444444); }
.ma-bar-fill.ma20 { background: linear-gradient(90deg, #7c3aed, #a78bfa); }
.ma-bar-fill.ma60 { background: linear-gradient(90deg, #ca8a04, #eab308); }

.ma-bar-value {
  font-size: 13px;
  font-weight: 700;
  color: #333;
  width: 65px;
  text-align: right;
  flex-shrink: 0;
}

/* 支撑 / 压力位卡片 */
.sr-levels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 8px;
}

.sr-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border-radius: 8px;
  padding: 8px 12px;
}

.sr-card.support { border-left: 3px solid #52c41a; }
.sr-card.resistance { border-left: 3px solid #f5222d; }

.sr-icon {
  font-size: 18px;
}

.sr-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sr-label {
  font-size: 11px;
  color: #999;
}

.sr-value {
  font-size: 16px;
  font-weight: 700;
}
.sr-card.support .sr-value { color: #52c41a; }
.sr-card.resistance .sr-value { color: #f5222d; }

/* ===== 筹码分布 - 紧凑展示 ===== */
.chip-compact {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cc-pl {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 4px 0;
}

.cc-pl-item {
  font-size: 15px;
  font-weight: 700;
}

.cc-profit { color: #f5222d; }
.cc-loss { color: #006d2c; }

.cc-pl-divider {
  color: #ddd;
  font-size: 13px;
}

.cc-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3px;
}

.cc-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3px 4px;
  background: #f8fafc;
  border-radius: 4px;
}

.cc-label {
  font-size: 10px;
  color: #999;
  line-height: 1.2;
}

.cc-value {
  font-size: 12px;
  font-weight: 600;
  color: #333;
  line-height: 1.3;
}

.cc-summary {
  font-size: 11px;
  color: #666;
  line-height: 1.4;
  text-align: center;
  padding: 2px 0;
}

/* ===== 作战计划 ===== */
.bp-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin-bottom: 6px;
}

.bp-card {
  background: #f8fafc;
  border-radius: 6px;
  padding: 6px 8px;
  text-align: center;
  border-top: 2px solid transparent;
}

.bp-card.ideal { border-top-color: #1890ff; background: #f0f5ff; }
.bp-card.suboptimal { border-top-color: #faad14; background: #fffbe6; }
.bp-card.stoploss { border-top-color: #f5222d; background: #fff1f0; }
.bp-card.target { border-top-color: #52c41a; background: #f6ffed; }

.bp-header {
  font-size: 10px;
  color: #666;
  margin-bottom: 2px;
}

.bp-price {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.bp-desc {
  font-size: 11px;
  color: #888;
  margin-top: 4px;
  line-height: 1.3;
}

.bp-rr {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  padding-top: 6px;
  border-top: 1px dashed #e8e8e8;
}

.bp-rr-label {
  font-size: 12px;
  color: #999;
}

.bp-rr-value {
  font-size: 14px;
  font-weight: 700;
  color: #1890ff;
}

/* MACD 区块 */
.macd-section {
  margin-top: 6px;
  padding: 6px 8px;
  background: #fafbff;
  border-radius: 4px;
  border: 1px solid #e8ecf4;
}

.macd-header {
  font-size: 11px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.macd-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 4px;
  margin-bottom: 4px;
}

.macd-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  padding: 2px 0;
  background: #fff;
  border-radius: 4px;
}

.macd-label {
  font-size: 10px;
  color: #999;
}

.macd-value {
  font-size: 12px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
}

.macd-value.macd-up {
  color: #f5222d;
}

.macd-value.macd-down {
  color: #006d2c;
}

.macd-signal-text {
  font-size: 11px;
  color: #555;
  line-height: 1.4;
  text-align: center;
  word-break: break-all;
}

/* 十大流通股东 */
.shareholder-table {
  width: 100%;
  font-size: 11px;
}

.sh-row {
  display: flex;
  align-items: center;
  padding: 2px 0;
  border-bottom: 1px solid #f0f0f0;
  gap: 2px;
}

.sh-row.sh-header {
  font-weight: 600;
  color: #666;
  font-size: 10px;
  border-bottom: 2px solid #e8e8e8;
}

.sh-col-rank {
  width: 20px;
  min-width: 20px;
  text-align: center;
  color: #999;
}

.sh-col-name {
  flex: 1;
  min-width: 0;
  word-break: break-all;
  line-height: 1.3;
}

.sh-col-type {
  width: 48px;
  min-width: 48px;
  text-align: center;
}

.sh-col-share-type {
  width: 48px;
  min-width: 48px;
  text-align: center;
  color: #666;
}

.sh-col-holdnum {
  width: 72px;
  min-width: 72px;
  text-align: right;
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
  font-size: 10px;
}

.sh-col-ratio {
  width: 48px;
  min-width: 48px;
  text-align: right;
  font-weight: 600;
  color: #1a1a2e;
}

.sh-col-change {
  width: 72px;
  min-width: 72px;
  text-align: right;
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
  font-size: 10px;
}

.sh-col-change-ratio {
  width: 52px;
  min-width: 52px;
  text-align: right;
  font-size: 10px;
}

.change-up {
  color: #cf1322;
}

.change-down {
  color: #006d2c;
}

.sh-type-tag {
  font-size: 9px !important;
  line-height: 14px !important;
  padding: 0 3px !important;
}

.shareholder-analysis {
  margin-top: 4px;
  padding: 4px 8px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 11px;
  color: #555;
  line-height: 1.5;
}

/* 通用区块 */
.section {
  margin-bottom: 14px;
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
.overview-section.core-insight {
  border-left: 3px solid #faad14;
}

.core-insight-text {
  font-size: 13px;
  line-height: 1.6;
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
  color: #006d2c;
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
  gap: 6px;
}

.news-item-compact {
  background: #fafafa;
  border-radius: 4px;
  padding: 6px 8px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.news-item-compact:hover {
  border-color: #1890ff;
  box-shadow: 0 1px 4px rgba(24, 144, 255, 0.15);
}

.news-title-compact {
  font-size: 13px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
  display: flex;
  align-items: flex-start;
  line-height: 1.4;
}

.news-num {
  color: #1890ff;
  font-weight: bold;
  margin-right: 6px;
  flex-shrink: 0;
}

.news-summary-compact {
  font-size: 11px;
  color: #666;
  line-height: 1.4;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-meta-compact {
  display: flex;
  gap: 8px;
  font-size: 10px;
  color: #999;
}

.news-source-compact {
  padding: 0 4px;
  background: #f0f0f0;
  border-radius: 2px;
}

.news-time-compact {
  display: flex;
  align-items: center;
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
  font-size: 10px;
  color: #1890ff;
  text-decoration: none;
  padding: 1px 6px;
  background: #e6f7ff;
  border-radius: 3px;
  transition: all 0.3s ease;
  margin-top: 4px;
}

.news-link:hover {
  background: #1890ff;
  color: #ffffff;
  text-decoration: none;
}

.view-more-news {
  margin-top: 4px;
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
  color: #006d2c;
}

.change-value {
  font-size: 18px;
  font-weight: 600;
}

.change-value.up {
  color: #f5222d;
}

.change-value.down {
  color: #006d2c;
}

.change-amount {
  font-size: 13px;
  color: #999;
}

.change-amount.up {
  color: #f5222d;
}

.change-amount.down {
  color: #006d2c;
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
  color: #006d2c;
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
  color: #006d2c;
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
  color: #006d2c;
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
  .overview-cols {
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
}

/* 投研简报：以判断和证据为主线，弱化后台式卡片堆叠。 */
.analysis-page { background: #f7f9fc; }
.analysis-layout { gap: 32px; }
.records-card,
.analysis-form-card,
.result-card { box-shadow: none; }
.analysis-form-card { border: 1px solid #e2e8f1; border-radius: 14px; }
.result-card { background: transparent; }
.right-panel { gap: 28px; }
.result-card :deep(.ant-card-head) { min-height: 82px; padding: 0 16px 0 4px; border-bottom: 1px solid #dfe6ef; }
.result-card :deep(.ant-card-head-title) { padding: 18px 0; overflow: visible; white-space: normal; }
.result-card :deep(.ant-card-extra), .records-card :deep(.ant-card-extra) { display: flex; align-items: center; padding: 0; }
.result-card :deep(.ant-card-extra) { padding-left: 24px; }
.result-card :deep(.ant-card-body) { overflow: hidden; padding: 0; }
.brief-heading { display: flex; align-items: center; justify-content: space-between; gap: 28px; }
.brief-kicker { margin: 0 0 5px; color: #72809a; font-size: 10px; font-weight: 800; letter-spacing: .13em; text-transform: uppercase; }
.brief-identity { display: flex; align-items: baseline; gap: 10px; color: #17263d; }
.brief-identity span { color: #667894; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 13px; }
.brief-identity strong { font-size: 23px; letter-spacing: -.035em; }
.brief-heading time { color: #8491a5; font-size: 12px; white-space: nowrap; }
.brief-actions :deep(.ant-btn) { margin-left: 2px; }
.records-card :deep(.ant-card-head) { display: flex; align-items: center; min-height: 62px; padding: 0 18px; border-bottom: 1px solid #e1e7ef; }
.records-card :deep(.ant-card-head-title) { padding: 0; }
.records-card :deep(.ant-card-extra) { padding-left: 18px; }
.records-refresh-button { color: #5a7198; }
.overview-content { padding: 28px 4px 0; }
.price-hero { padding: 23px 26px; margin: 0 0 26px; color: #182a45; background: radial-gradient(circle at 90% 5%, #dfe9fb 0, transparent 31%), #edf3fb; border: 1px solid #dbe5f1; border-radius: 16px; }
.ph-label, .ph-stat-label, .ph-vol-label { color: #72819a; }
.ph-price { color: #1f3151; }
.ph-price.up, .ph-change-pct.up, .ph-change-amt.up, .ph-stat-value.high { color: #c94755; }
.ph-price.down, .ph-change-pct.down, .ph-change-amt.down, .ph-stat-value.low { color: #006d2c; }
.ph-change-amt { color: #697b94; opacity: 1; }
.price-hero-change, .price-hero-stats, .price-hero-volume { border-left-color: #ced9e8; }
.ph-stock-code, .ph-stock-name { color: #263a59; }
.overview-cols { grid-template-columns: minmax(0, 1.12fr) minmax(300px, .88fr); gap: 38px; }
.overview-col { gap: 0; }
.overview-col:nth-child(3) { grid-column: 1 / -1; display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(280px, .8fr); gap: 38px; }
.overview-section { padding: 22px 0; background: transparent; border: 0; border-top: 1px solid #dfe6ef; border-radius: 0; }
.overview-col:first-child .overview-section:first-child { padding: 22px; background: #f0f4fa; border: 0; border-radius: 15px; }
.section-title { gap: 8px; margin-bottom: 14px; color: #40536f; font-size: 11px; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; }
.section-title :deep(svg) { color: #6884b9; font-size: 15px; }
.core-insight-text { color: #35445b; font-size: 14px; line-height: 1.82; }
.sectors-inline :deep(.ant-tag) { padding: 3px 9px; margin: 0; color: #49658f; background: #edf3fb; border: 0; border-radius: 999px; }
.sr-card, .cc-cell { background: #f5f7fa; }
.bp-grid { gap: 9px; }
.bp-card { padding: 11px 10px; border-radius: 10px; }
.bp-header { margin-bottom: 5px; font-size: 11px; }
.bp-price { font-size: 18px; }
.bp-desc { line-height: 1.45; }
.shareholder-table { border-top: 1px solid #e0e6ef; }
.sh-row { padding: 7px 2px; border-bottom-color: #e7ecf2; }
.sh-row.sh-header { color: #77859a; background: transparent; border-bottom-color: #dce3ed; }
.news-item-compact { padding: 11px 12px; background: #f7f9fc; border: 0; border-radius: 10px; }
.news-item-compact:hover { border: 0; background: #edf3fb; box-shadow: none; }

@media (max-width: 1024px) {
  .overview-cols, .overview-col:nth-child(3) { grid-template-columns: 1fr; }
  .overview-col:nth-child(3) { display: flex; }
}

@media (max-width: 768px) {
  .analysis-page { height: auto; min-height: calc(100vh - 76px); overflow: visible; }
  .analysis-layout, .right-panel { gap: 20px; }
  .brief-heading { align-items: start; flex-direction: column; gap: 4px; }
  .brief-identity strong { font-size: 20px; }
  .result-card :deep(.ant-card-head) { min-height: auto; padding: 0; }
  .result-card :deep(.ant-card-extra) { padding: 0 0 14px; }
  .result-card :deep(.ant-card-extra) { padding-left: 0; }
  .overview-content { padding: 22px 0 0; }
  .price-hero { padding: 20px; }
}

/* 标的研究工作台：将任务导航与研究内容分层，避免后台表单的视觉感受。 */
.analysis-page { padding: 24px 28px; background: radial-gradient(circle at 92% 0%, #edf3ff 0, transparent 31%), #f7f9fc; }.analysis-layout { grid-template-columns: 350px minmax(0, 1fr); gap: 24px; }.records-card { overflow: hidden; color: #dce7f8; background: radial-gradient(circle at 100% 0%, #38547f 0, transparent 35%), #18263d; border: 1px solid #2d4261; border-radius: 16px; box-shadow: 0 16px 34px rgba(24, 38, 61, .16); }.records-card :deep(.ant-card-head) { border-bottom-color: rgba(194, 214, 242, .16); }.records-card :deep(.ant-card-head-title) { color: #f2f6fd; font-size: 15px; }.records-refresh-button { color: #b9cdea; }.records-refresh-button:hover { color: #fff; background: rgba(255, 255, 255, .08); }.records-list :deep(.ant-list-item) { margin: 3px 9px; border-radius: 10px; }.records-list :deep(.ant-list-item:hover) { background: rgba(255, 255, 255, .07); }.records-list :deep(.ant-list-item.active) { background: rgba(130, 167, 227, .24); box-shadow: inset 2px 0 #a9c7ff; }.records-list :deep(.ant-list-item.active .stock-code), .records-list :deep(.ant-list-item.active .stock-name) { color: #ffffff !important; }.records-list :deep(.ant-list-item.active .time-info), .records-list :deep(.ant-list-item.active .duration) { color: #d7e6ff !important; }.records-card .stock-code, .records-card .stock-name { color: #f1f5fc; }.records-card .time-info, .records-card .duration, .records-card .end-hint, .records-card .empty-tip, .records-card .empty-hint { color: #a9bbd5; }.records-card .holding-tag { border: 0; }.analysis-form-card { overflow: hidden; color: #e7effc; background: radial-gradient(circle at 92% 0%, #5476ae 0, transparent 37%), #263c61; border: 0; border-radius: 16px; box-shadow: 0 12px 27px rgba(41, 64, 102, .16); }.analysis-form-card :deep(.ant-card-head) { min-height: 58px; color: #fff; border-bottom-color: rgba(224, 236, 255, .2); }.analysis-form-card :deep(.ant-card-head-title) { color: #fff; font-weight: 700; }.analysis-form-card :deep(.ant-card-body) { padding: 16px 22px; }.analysis-form-card :deep(.ant-checkbox-wrapper) { color: #d9e6fb; }.analysis-form-card :deep(.ant-input), .analysis-form-card :deep(.ant-select-selector) { background: rgba(255, 255, 255, .94) !important; border-color: transparent !important; }.result-card { padding: 0 4px; }.result-card :deep(.ant-card-head) { border-bottom-color: #d7e1ed; }
.records-list :deep(.ant-list-item) { padding: 9px 8px; }.records-list :deep(.ant-list-item.active .stock-code), .records-list :deep(.ant-list-item.active .stock-name), .records-list :deep(.ant-list-item.active .time-info), .records-list :deep(.ant-list-item.active .duration) { color: #18263d !important; }
.records-list :deep(.ant-list-item.analyzing-item .stock-code), .records-list :deep(.ant-list-item.analyzing-item .stock-name), .records-list :deep(.ant-list-item.analyzing-item .time-info), .records-list :deep(.ant-list-item.analyzing-item .duration) { color: #18263d !important; }
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
