<template>
  <main class="research-desk">
    <header class="desk-heading">
      <div>
        <p class="eyebrow">{{ $t('homeDesk.eyebrow') }}</p>
        <h1>{{ $t('homeDesk.title') }}</h1>
        <p class="desk-description">{{ $t('homeDesk.dashboardDescription') }}</p>
      </div>
      <div class="desk-actions">
        <a class="market-cloud-link" href="https://quote.eastmoney.com/stockhotmap/" target="_blank" rel="noopener noreferrer"><CloudOutlined />{{ $t('homeDesk.marketCloudMap') }}</a>
        <a-button type="primary" @click="goToAnalysis">{{ $t('homeDesk.newResearch') }} <ArrowRightOutlined /></a-button>
      </div>
    </header>

    <section class="decision-layout" :aria-label="$t('homeDesk.currentFocus')">
      <article class="focus-panel">
        <p class="focus-label">{{ $t('homeDesk.currentFocus') }}</p>
        <h2>{{ $t('homeDesk.focusQuestion') }}</h2>
        <p>{{ $t('homeDesk.focusDescription') }}</p>
        <div class="focus-actions">
          <a-button class="focus-action" @click="goToAnalysis">{{ $t('homeDesk.continueResearch') }} <ArrowRightOutlined /></a-button>
          <span>{{ $t('homeDesk.useSignalsHint') }}</span>
        </div>
      </article>

      <aside class="signal-panel" :aria-label="$t('homeDesk.verificationSignals')">
        <div class="panel-heading"><div><p class="panel-kicker">{{ $t('homeDesk.verificationSignals') }}</p><h2>{{ $t('homeDesk.signalChecklist') }}</h2></div><span class="live-indicator"><i />{{ $t('homeDesk.live') }}</span></div>
        <div class="signal-list">
          <div class="signal-row"><span>{{ $t('homeDesk.marketTurnover') }}</span><strong>{{ formatTurnover(marketTurnover?.amount) }}</strong></div>
          <div class="signal-row"><span>{{ $t('homeDesk.marketBreadth') }}</span><strong><em class="positive">{{ marketUpRatio ? `${marketUpRatio.toFixed(0)}%` : '--' }}</em> / {{ marketDownRatio ? `${marketDownRatio.toFixed(0)}%` : '--' }}</strong></div>
          <div class="signal-row"><span>{{ $t('homeDesk.fundFlow') }}</span><strong :class="primaryFundFlow.netAmount == null ? '' : (primaryFundFlow.netAmount >= 0 ? 'positive' : 'negative')">{{ primaryFundFlow.netAmount == null ? '--' : formatAmount(primaryFundFlow.netAmount) }}</strong></div>
        </div>
      </aside>
    </section>

    <section class="market-pulse" :aria-label="$t('homeDesk.marketPulse')">
      <div class="pulse-heading"><div><p class="eyebrow">{{ $t('homeDesk.marketPulse') }}</p><h2>{{ $t('homeDesk.marketPulseTitle') }}</h2></div><a-button type="link" @click="goToMarketReview">{{ $t('homeDesk.readReview') }} <ArrowRightOutlined /></a-button></div>
      <div class="pulse-grid">
        <article class="pulse-card breadth-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.marketBreadth') }}</p><small>{{ $t('homeDesk.marketActivityHint') }}</small></div><span>{{ marketActivity?.limitUp ?? '--' }} / {{ marketActivity?.limitDown ?? '--' }}</span></div>
          <div class="breadth-values"><div class="positive"><strong>{{ marketActivity?.up ?? '--' }}</strong><span>{{ $t('home.up') }}</span></div><div class="negative"><strong>{{ marketActivity?.down ?? '--' }}</strong><span>{{ $t('home.down') }}</span></div></div>
          <div class="market-breadth-bar" :aria-label="$t('homeDesk.marketBreadth')"><i class="breadth-up" :style="{width: `${marketUpRatio}%`}" /><i class="breadth-down" :style="{width: `${marketDownRatio}%`}" /></div>
        </article>
        <article class="pulse-card index-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.marketSnapshot') }}</p><small>{{ $t('homeDesk.indexesHint') }}</small></div></div>
          <div v-if="marketIndexList.length" class="quick-index-list"><div v-for="index in marketIndexList" :key="index.symbol"><span>{{ index.name }}</span><strong>{{ index.value }}</strong><em :class="index.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(index.changePct) }}</em></div></div>
          <a-skeleton v-else :paragraph="{rows: 3}" active />
        </article>
        <article class="pulse-card turnover-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.marketTurnover') }}</p><small>{{ marketTurnoverAsOf ? $t('homeDesk.asOf', {date: formatTurnoverDate(marketTurnoverAsOf)}) : $t('homeDesk.marketTurnoverHint') }}</small></div><strong v-if="marketTurnoverHistory.length">{{ formatTurnover(marketTurnover?.amount) }}</strong></div>
          <div v-if="marketTurnoverHistory.length" ref="marketTurnoverChartRef" class="market-turnover-chart" />
          <a-skeleton v-else :paragraph="{rows: 5}" active />
        </article>
        <article class="pulse-card sector-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.industrySectors') }}</p><small>{{ $t('homeDesk.industrySectorsHint') }}</small></div><span>{{ $t('homeDesk.industryNetInflow') }}</span></div>
          <div v-if="sectorPulse.length" class="sector-grid"><div v-for="sector in sectorPulse" :key="sector.name" class="sector-tile" :class="sector.changePct >= 0 ? 'sector-up' : 'sector-down'"><strong>{{ sector.name }}</strong><span>{{ formatChange(sector.changePct) }}</span></div></div>
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="pulse-card flow-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.fundFlow') }}</p><small>{{ $t('homeDesk.fundFlowHint') }}</small></div><span>{{ $t('homeDesk.mainNetInflow') }}</span></div>
          <div v-if="fundFlowList.length" class="flow-list"><div v-for="item in fundFlowList.slice(0, 3)" :key="item.code" class="flow-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.sector ? ` · ${item.sector}` : '' }}</small></div><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }}</b></div></div>
          <a-skeleton v-else-if="isLoadingMarketInsights" :paragraph="{rows: 3}" active />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
      </div>
    </section>

    <section class="market-data-board" :aria-label="$t('homeDesk.marketInsights')">
      <div class="pulse-heading"><div><p class="eyebrow">{{ $t('homeDesk.marketInsights') }}</p><h2>{{ $t('homeDesk.capitalSignals') }}</h2></div><span class="insights-as-of">{{ $t('homeDesk.asOf', {date: marketInsights?.asOf || '-'}) }}</span></div>
      <div class="market-data-grid">
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.dragonTiger') }}</p><small>{{ $t('homeDesk.dragonTigerHint') }}</small></div><span>{{ $t('homeDesk.netBuy') }}</span></div>
          <a-skeleton v-if="!dragonTigerList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="dragonTigerList.length" class="market-data-list"><div v-for="item in pagedDragonTigerList" :key="`${item.code}-${item.listedDate}`" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.listedDate }}</small></div><p>{{ item.reason || '-' }}</p><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }}</b></div></div>
          <a-pagination v-if="dragonTigerList.length > insightPageSize" v-model:current="dragonTigerPage" simple size="small" :page-size="insightPageSize" :total="dragonTigerList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.fundFlow') }}</p><small>{{ $t('homeDesk.fundFlowHint') }}</small></div><span>{{ $t('homeDesk.mainNetInflow') }}</span></div>
          <a-skeleton v-if="!fundFlowList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="fundFlowList.length" class="market-data-list"><div v-for="item in pagedFundFlowList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.sector ? ` · ${item.sector}` : '' }}</small></div><p>{{ $t('homeDesk.latestPrice') }} {{ formatPrice(item.price) }}</p><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }}</b></div></div>
          <a-pagination v-if="fundFlowList.length > insightPageSize" v-model:current="fundFlowPage" simple size="small" :page-size="insightPageSize" :total="fundFlowList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.popularityRank') }}</p><small>{{ $t('homeDesk.popularityRankHint') }}</small></div><span>{{ $t('homeDesk.currentRank') }}</span></div>
          <a-skeleton v-if="!popularityRankList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="popularityRankList.length" class="market-data-list"><div v-for="item in pagedPopularityRankList" :key="item.code" class="market-data-row popularity-row"><span class="rank-number">{{ item.rank }}</span><div><strong>{{ item.name }}</strong><small>{{ item.code }}</small></div><b :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="popularityRankList.length > insightPageSize" v-model:current="popularityRankPage" simple size="small" :page-size="insightPageSize" :total="popularityRankList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.industrySectors') }}</p><small>{{ $t('homeDesk.industrySectorsHint') }}</small></div><span>{{ $t('homeDesk.industryNetInflow') }}</span></div>
          <a-skeleton v-if="!industrySectorsList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="industrySectorsList.length" class="market-data-list"><div v-for="item in pagedIndustrySectorsList" :key="item.name" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.leadingStock || '-' }}</small></div><p :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</p><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }}</b></div></div>
          <a-pagination v-if="industrySectorsList.length > insightPageSize" v-model:current="industrySectorsPage" simple size="small" :page-size="insightPageSize" :total="industrySectorsList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.limitUpPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div>
          <a-skeleton v-if="!limitUpPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="limitUpPoolList.length" class="market-data-list"><div v-for="item in pagedLimitUpPoolList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p>{{ item.detail || '-' }}</p><b class="positive">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="limitUpPoolList.length > insightPageSize" v-model:current="limitUpPoolPage" simple size="small" :page-size="insightPageSize" :total="limitUpPoolList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.limitDownPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div>
          <a-skeleton v-if="!limitDownPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="limitDownPoolList.length" class="market-data-list"><div v-for="item in pagedLimitDownPoolList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p>{{ item.detail || '-' }}</p><b class="negative">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="limitDownPoolList.length > insightPageSize" v-model:current="limitDownPoolPage" simple size="small" :page-size="insightPageSize" :total="limitDownPoolList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.strongPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div>
          <a-skeleton v-if="!strongPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="strongPoolList.length" class="market-data-list"><div v-for="item in pagedStrongPoolList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p>{{ item.detail || '-' }}</p><b :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="strongPoolList.length > insightPageSize" v-model:current="strongPoolPage" simple size="small" :page-size="insightPageSize" :total="strongPoolList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.brokenPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div>
          <a-skeleton v-if="!brokenPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="brokenPoolList.length" class="market-data-list"><div v-for="item in pagedBrokenPoolList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p>{{ item.detail || '-' }}</p><b :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="brokenPoolList.length > insightPageSize" v-model:current="brokenPoolPage" simple size="small" :page-size="insightPageSize" :total="brokenPoolList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {Empty} from 'ant-design-vue'
import {ArrowRightOutlined, CloudOutlined} from '@ant-design/icons-vue'
import {init, use} from 'echarts/core'
import {BarChart} from 'echarts/charts'
import {GridComponent, TooltipComponent} from 'echarts/components'
import {CanvasRenderer} from 'echarts/renderers'
import {getLatestTurnoverItem, hasMarketTurnoverHistory, unwrapMarketTurnover} from '../composables/marketData'

use([BarChart, CanvasRenderer, GridComponent, TooltipComponent])

const router = useRouter()
const {t} = useI18n()
const aEmptyImage = Empty.PRESENTED_IMAGE_SIMPLE
const marketActivity = ref(null)
const marketIndex = ref(null)
const marketTurnover = ref(null)
const marketInsights = ref(null)
const marketDashboardWebSocketPath = '/api/ws/market-dashboard'
const marketDashboardWebSocketReconnectInterval = 5 * 1000
const marketDashboardOverviewMessageType = 'overview'
const marketDashboardInsightsMessageType = 'insights'
let marketDashboardSocket = null
let marketDashboardSocketReconnectTimer = null
let isMarketDashboardSocketActive = false
const isLoadingMarketInsights = ref(true)
let marketTurnoverChart = null
const marketTurnoverChartRef = ref(null)
const insightPageSize = 5
const dragonTigerPage = ref(1)
const fundFlowPage = ref(1)
const popularityRankPage = ref(1)
const industrySectorsPage = ref(1)
const limitUpPoolPage = ref(1)
const limitDownPoolPage = ref(1)
const strongPoolPage = ref(1)
const brokenPoolPage = ref(1)

const indexLabels = computed(() => ({sh000001: t('homeDesk.shanghaiIndex'), sz399001: t('homeDesk.shenzhenIndex'), sz399006: t('homeDesk.chinextIndex'), sh000688: t('homeDesk.star50Index')}))
const marketIndexList = computed(() => Object.entries(marketIndex.value || {}).map(([symbol, data]) => ({symbol, name: indexLabels.value[symbol] || data.name || symbol, value: data.current?.toFixed(2) || '-', changePct: data.changePct || 0})))
const marketUpCount = computed(() => Number(marketActivity.value?.up) || 0)
const marketDownCount = computed(() => Number(marketActivity.value?.down) || 0)
const marketBreadthTotal = computed(() => marketUpCount.value + marketDownCount.value)
const marketUpRatio = computed(() => marketBreadthTotal.value ? marketUpCount.value / marketBreadthTotal.value * 100 : 0)
const marketDownRatio = computed(() => marketBreadthTotal.value ? marketDownCount.value / marketBreadthTotal.value * 100 : 0)
const marketTurnoverHistory = computed(() => Array.isArray(marketTurnover.value?.history) ? marketTurnover.value.history : [])
const marketTurnoverAsOf = computed(() => getLatestTurnoverItem(marketTurnover.value)?.date || '')
const dragonTigerList = computed(() => marketInsights.value?.dragonTiger || [])
const fundFlowList = computed(() => marketInsights.value?.fundFlow || [])
const industrySectorsList = computed(() => marketInsights.value?.industrySectors || [])
const sectorPulse = computed(() => industrySectorsList.value.slice(0, 4))
const popularityRankList = computed(() => marketInsights.value?.popularityRank || [])
const limitPools = computed(() => marketInsights.value?.limitPools || {})
const limitUpPoolList = computed(() => limitPools.value.limitUp || [])
const limitDownPoolList = computed(() => limitPools.value.limitDown || [])
const strongPoolList = computed(() => limitPools.value.strong || [])
const brokenPoolList = computed(() => limitPools.value.broken || [])
const primaryFundFlow = computed(() => fundFlowList.value[0] || {})
const pageItems = (items, currentPage) => items.slice((currentPage - 1) * insightPageSize, currentPage * insightPageSize)
const pagedDragonTigerList = computed(() => pageItems(dragonTigerList.value, dragonTigerPage.value))
const pagedFundFlowList = computed(() => pageItems(fundFlowList.value, fundFlowPage.value))
const pagedPopularityRankList = computed(() => pageItems(popularityRankList.value, popularityRankPage.value))
const pagedIndustrySectorsList = computed(() => pageItems(industrySectorsList.value, industrySectorsPage.value))
const pagedLimitUpPoolList = computed(() => pageItems(limitUpPoolList.value, limitUpPoolPage.value))
const pagedLimitDownPoolList = computed(() => pageItems(limitDownPoolList.value, limitDownPoolPage.value))
const pagedStrongPoolList = computed(() => pageItems(strongPoolList.value, strongPoolPage.value))
const pagedBrokenPoolList = computed(() => pageItems(brokenPoolList.value, brokenPoolPage.value))

const hasSameTurnover = (first, second) => first?.date === second?.date && Number(first?.amount) === Number(second?.amount)
const applyMarketTurnoverUpdate = turnover => {
  turnover = unwrapMarketTurnover(turnover)
  if (!hasMarketTurnoverHistory(turnover)) return
  const latestIncomingItem = getLatestTurnoverItem(turnover)
  const latestCachedItem = getLatestTurnoverItem(marketTurnover.value)
  if (!latestIncomingItem) return
  if (!latestCachedItem) { marketTurnover.value = turnover; return }
  if (latestIncomingItem.date < latestCachedItem.date) return
  if (hasSameTurnover(latestIncomingItem, latestCachedItem) && Number(turnover.amount) === Number(marketTurnover.value?.amount)) return
  const history = [...marketTurnover.value.history]
  if (latestIncomingItem.date === latestCachedItem.date) history[history.length - 1] = latestIncomingItem
  else history.push(latestIncomingItem)
  marketTurnover.value = {...marketTurnover.value, amount: turnover.amount, difference: turnover.difference, history}
}
const getMarketDashboardWebSocketUrl = () => {
  const token = localStorage.getItem('token')
  if (!token) return null
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}${marketDashboardWebSocketPath}?token=${encodeURIComponent(token)}`
}
const reconnectMarketDashboardSocket = () => {
  if (!isMarketDashboardSocketActive || marketDashboardSocketReconnectTimer) return
  marketDashboardSocketReconnectTimer = window.setTimeout(() => { marketDashboardSocketReconnectTimer = null; connectMarketDashboardSocket() }, marketDashboardWebSocketReconnectInterval)
}
const applyMarketDashboardMessage = message => {
  const messageData = message?.data
  if (!messageData || typeof messageData !== 'object') return
  if (message.type === marketDashboardOverviewMessageType) {
    marketActivity.value = messageData.marketActivity || marketActivity.value
    marketIndex.value = messageData.marketIndex || marketIndex.value
    applyMarketTurnoverUpdate(messageData.marketTurnover)
    return
  }
  if (message.type === marketDashboardInsightsMessageType) { marketInsights.value = messageData; isLoadingMarketInsights.value = false }
}
const connectMarketDashboardSocket = () => {
  const url = getMarketDashboardWebSocketUrl()
  if (!url || !isMarketDashboardSocketActive) return
  marketDashboardSocket = new WebSocket(url)
  marketDashboardSocket.onmessage = event => { try { applyMarketDashboardMessage(JSON.parse(event.data)) } catch (error) { console.warn('Unable to apply market dashboard update', error) } }
  marketDashboardSocket.onerror = () => marketDashboardSocket?.close()
  marketDashboardSocket.onclose = () => { marketDashboardSocket = null; reconnectMarketDashboardSocket() }
}
const formatChange = value => `${Number(value || 0) >= 0 ? '+' : ''}${Number(value || 0).toFixed(2)}%`
const formatAmount = value => `${Number(value || 0) >= 0 ? '+' : ''}${(Number(value || 0) / 100000000).toFixed(2)}${t('homeDesk.hundredMillion')}`
const formatTurnover = value => {
  const amount = Number(value || 0)
  if (!amount) return '--'
  return amount >= 1000000000000 ? `${(amount / 1000000000000).toFixed(2)}${t('homeDesk.trillion')}` : `${(amount / 100000000).toFixed(0)}${t('homeDesk.hundredMillion')}`
}
const formatTurnoverDetail = value => `${(Number(value || 0) / 100000000).toFixed(2)}${t('homeDesk.hundredMillion')}`
const formatTurnoverDate = date => String(date || '').replace(/-/g, '/')
const formatPrice = value => Number(value || 0).toFixed(2)
const renderMarketTurnoverChart = () => {
  if (!marketTurnoverChartRef.value || !marketTurnoverHistory.value.length) return
  if (!marketTurnoverChart) marketTurnoverChart = init(marketTurnoverChartRef.value)
  const history = marketTurnoverHistory.value
  marketTurnoverChart.setOption({
    animation: false,
    grid: {top: 8, right: 2, bottom: 18, left: 2},
    tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}, backgroundColor: '#182336', borderWidth: 0, padding: [8, 10], textStyle: {color: '#ffffff', fontSize: 12}, formatter: params => {
      const item = history[params[0]?.dataIndex]
      return item ? `${formatTurnoverDate(item.date)}<br/>${t('homeDesk.marketTurnover')}：${formatTurnoverDetail(item.amount)}` : ''
    }},
    xAxis: {type: 'category', data: history.map(item => formatTurnoverDate(item.date).slice(5)), axisLine: {lineStyle: {color: '#dce2eb'}}, axisTick: {show: false}, axisLabel: {color: '#8996aa', fontSize: 10, interval: Math.max(0, Math.ceil(history.length / 5) - 1)}},
    yAxis: {type: 'value', show: false},
    series: [{type: 'bar', data: history.map(item => Number(item.amount) || 0), barMaxWidth: 12, itemStyle: {color: '#5d8fd8', borderRadius: [3, 3, 0, 0]}, emphasis: {itemStyle: {color: '#316bc0'}}}]
  }, {notMerge: true, lazyUpdate: true, silent: true})
}
const resizeMarketTurnoverChart = () => marketTurnoverChart?.resize()
const goToAnalysis = () => router.push('/analysis')
const goToMarketReview = () => router.push('/market-review')

watch(marketTurnoverHistory, () => { nextTick(renderMarketTurnoverChart) }, {flush: 'post'})
watch(dragonTigerList, () => { dragonTigerPage.value = 1 })
watch(fundFlowList, () => { fundFlowPage.value = 1 })
watch(popularityRankList, () => { popularityRankPage.value = 1 })
watch(industrySectorsList, () => { industrySectorsPage.value = 1 })
watch(limitUpPoolList, () => { limitUpPoolPage.value = 1 })
watch(limitDownPoolList, () => { limitDownPoolPage.value = 1 })
watch(strongPoolList, () => { strongPoolPage.value = 1 })
watch(brokenPoolList, () => { brokenPoolPage.value = 1 })
onMounted(() => { isMarketDashboardSocketActive = true; connectMarketDashboardSocket(); window.addEventListener('resize', resizeMarketTurnoverChart) })
onUnmounted(() => {
  isMarketDashboardSocketActive = false
  if (marketDashboardSocketReconnectTimer) window.clearTimeout(marketDashboardSocketReconnectTimer)
  marketDashboardSocket?.close()
  window.removeEventListener('resize', resizeMarketTurnoverChart)
  marketTurnoverChart?.dispose()
  marketTurnoverChart = null
})
</script>

<style scoped>
.research-desk { width: 100%; max-width: none; padding: 28px 32px 52px; color: #182336; }
.desk-heading, .pulse-heading, .panel-heading, .card-heading { display: flex; align-items: end; justify-content: space-between; gap: 20px; }
.desk-heading { padding: 4px 0 26px; }.eyebrow, .panel-kicker { margin: 0 0 7px; color: #72809a; font-size: 11px; font-weight: 800; letter-spacing: .13em; text-transform: uppercase; }.desk-heading h1, .pulse-heading h2 { margin: 0; letter-spacing: -.045em; }.desk-heading h1 { font-size: clamp(28px, 3vw, 36px); }.desk-description { margin: 7px 0 0; color: #718098; font-size: 14px; }.desk-actions { display: flex; align-items: center; gap: 10px; }.market-cloud-link { display: inline-flex; align-items: center; gap: 6px; padding: 8px 12px; color: #526178; font-size: 13px; font-weight: 600; text-decoration: none; border: 1px solid #dce2eb; border-radius: 8px; transition: color .16s, background .16s, border-color .16s; }.market-cloud-link:hover { color: #316bc0; background: #f1f5fb; border-color: #afc1df; }
.decision-layout { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(280px, .8fr); gap: 18px; }.focus-panel { min-height: 205px; padding: 23px 25px; color: #fff; background: radial-gradient(circle at 89% 0%, #3b5a8e 0, transparent 35%), linear-gradient(128deg, #17263d, #263d64); border-radius: 14px; }.focus-label { color: #aabddb; font-size: 10px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }.focus-panel h2 { max-width: 700px; margin: 8px 0; color: #fff; font-size: clamp(20px, 2vw, 25px); line-height: 1.32; letter-spacing: -.035em; }.focus-panel > p { max-width: 720px; margin: 0; color: #c1cde0; font-size: 13px; line-height: 1.6; }.focus-actions { display: flex; align-items: center; gap: 11px; margin-top: 18px; }.focus-action { color: #263d65; border-color: #f0f5ff; background: #f0f5ff; }.focus-action:hover { color: #1a3157 !important; border-color: #fff !important; background: #fff !important; }.focus-actions span { padding: 4px 7px; color: #d1ddf0; font-size: 11px; background: rgba(212, 226, 248, .12); border: 1px solid rgba(212, 226, 248, .14); border-radius: 5px; }
.signal-panel, .pulse-card { min-width: 0; background: #fff; border: 1px solid #e1e7f0; border-radius: 14px; }.signal-panel { padding: 19px; }.panel-heading { align-items: start; }.panel-kicker { margin-bottom: 4px; font-size: 10px; }.panel-heading h2 { margin: 0; color: #263957; font-size: 16px; letter-spacing: -.02em; }.live-indicator { display: inline-flex; align-items: center; gap: 5px; color: #7c8ba1; font-size: 10px; white-space: nowrap; }.live-indicator i { width: 6px; height: 6px; background: #28a274; border-radius: 50%; }.signal-list { margin-top: 12px; }.signal-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 11px 0; color: #607089; font-size: 12px; border-bottom: 1px solid #edf0f5; }.signal-row:last-child { border-bottom: 0; }.signal-row strong { color: #263957; font-size: 13px; font-variant-numeric: tabular-nums; white-space: nowrap; }.signal-row em { font-style: normal; }
.market-pulse { margin-top: 34px; }.pulse-heading { margin-bottom: 15px; }.pulse-heading h2 { font-size: 22px; }.pulse-heading :deep(.ant-btn-link) { padding-right: 0; }.pulse-grid { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 16px; }.pulse-card { padding: 18px; }.breadth-card, .index-card { grid-column: span 3; }.turnover-card { grid-column: span 6; }.sector-card, .flow-card { grid-column: span 6; }.card-heading { align-items: start; padding-bottom: 12px; border-bottom: 1px solid #edf0f5; }.card-heading p { margin: 0; color: #263957; font-size: 15px; font-weight: 700; }.card-heading small { display: block; margin-top: 3px; color: #8996aa; font-size: 11px; line-height: 1.35; }.card-heading > span, .card-heading > strong { color: #71809a; font-size: 11px; font-variant-numeric: tabular-nums; white-space: nowrap; }.card-heading > strong { color: #263957; font-size: 17px; }.breadth-values { display: flex; gap: 19px; margin: 19px 0 15px; }.breadth-values div { display: grid; gap: 2px; }.breadth-values strong { font-size: 25px; line-height: 1; font-variant-numeric: tabular-nums; }.breadth-values span { color: #7f8da1; font-size: 11px; }.market-breadth-bar { display: flex; height: 7px; overflow: hidden; background: #dfe6ef; border-radius: 999px; }.market-breadth-bar i { display: block; min-width: 0; transition: width .2s ease; }.market-breadth-bar .breadth-up { background: #d7505d; }.market-breadth-bar .breadth-down { background: #00a15c; }
.quick-index-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 13px 15px; padding-top: 14px; }.quick-index-list div { display: grid; grid-template-columns: 1fr auto; gap: 3px; }.quick-index-list span { color: #526178; font-size: 12px; }.quick-index-list strong, .quick-index-list em { justify-self: end; font-variant-numeric: tabular-nums; }.quick-index-list strong { color: #263957; font-size: 17px; line-height: 1.1; }.quick-index-list em { grid-column: 2; font-size: 11px; font-style: normal; }.market-turnover-chart { width: 100%; height: 162px; margin-top: 8px; }.sector-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; padding-top: 14px; }.sector-tile { display: grid; gap: 4px; min-height: 74px; align-content: center; padding: 10px; border-radius: 8px; }.sector-tile strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.sector-tile span { font-size: 11px; font-variant-numeric: tabular-nums; }.sector-up { color: #9f404a; background: #fff0f0; }.sector-down { color: #087043; background: #edf9f3; }.flow-list { padding-top: 3px; }.flow-row { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 11px 0; border-bottom: 1px solid #edf0f5; }.flow-row:last-child { border-bottom: 0; }.flow-row strong { display: block; color: #263957; font-size: 13px; }.flow-row small { display: block; margin-top: 3px; overflow: hidden; color: #8996aa; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.flow-row b { font-size: 12px; font-variant-numeric: tabular-nums; white-space: nowrap; }.positive { color: #d7505d !important; }.negative { color: #006d2c !important; }.sector-card :deep(.ant-empty), .flow-card :deep(.ant-empty) { padding: 30px 0 8px; }.sector-card :deep(.ant-empty-description), .flow-card :deep(.ant-empty-description) { color: #8996aa; font-size: 12px; }
@media (max-width: 1080px) { .decision-layout { grid-template-columns: 1fr; }.pulse-card { grid-column: span 6; }.turnover-card { grid-column: span 12; } }
@media (max-width: 720px) { .research-desk { padding: 18px 16px 40px; }.desk-heading, .pulse-heading { align-items: start; flex-direction: column; }.desk-actions { width: 100%; }.desk-actions .market-cloud-link, .desk-actions :deep(.ant-btn) { flex: 1; justify-content: center; }.focus-panel { padding: 19px; }.focus-actions { align-items: start; flex-direction: column; }.pulse-grid { grid-template-columns: 1fr; }.pulse-card, .turnover-card { grid-column: auto; }.sector-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.market-turnover-chart { height: 145px; } }

/* 默认浅色工作台：焦点通过柔和底色和层级区分，而非深色反差。 */
.focus-panel { color: #243652; background: radial-gradient(circle at 89% 0%, #d7e5fb 0, transparent 38%), linear-gradient(128deg, #f8fbff, #edf3fb); border: 1px solid #d7e3f2; box-shadow: 0 10px 26px rgba(47, 80, 125, .06); }.focus-label { color: #6380af; }.focus-panel h2 { color: #1e3455; }.focus-panel > p { color: #657790; }.focus-action { color: #fff; border-color: #5878c2; background: #5878c2; }.focus-action:hover { color: #fff !important; border-color: #4869b2 !important; background: #4869b2 !important; }.focus-actions span { color: #5d7394; background: #e3edf9; border-color: #d5e2f1; }
.market-data-board { margin-top: 40px; }.insights-as-of { color: #8996aa; font-size: 12px; }.market-data-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }.market-data-card { min-width: 0; min-height: 300px; padding: 18px; background: #fff; border: 1px solid #e1e7f0; border-radius: 14px; box-shadow: 0 6px 18px rgba(34, 51, 79, .035); }.market-data-card :deep(.ant-pagination) { display: flex; justify-content: end; margin: 13px 0 0; }.market-data-card :deep(.ant-empty) { padding-top: 58px; }.market-data-card :deep(.ant-empty-description) { color: #8996aa; font-size: 12px; }.market-data-list { margin-top: 1px; }.market-data-row { display: grid; grid-template-columns: minmax(90px, .85fr) minmax(0, 1.25fr) auto; align-items: center; gap: 10px; padding: 11px 0; border-bottom: 1px solid #edf0f5; }.market-data-row:last-child { border-bottom: 0; }.market-data-row > div { min-width: 0; }.market-data-row strong { display: block; overflow: hidden; color: #263957; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }.market-data-row small, .market-data-row p { display: block; overflow: hidden; margin: 3px 0 0; color: #8996aa; font-size: 11px; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }.market-data-row p { margin: 0; }.market-data-row b { font-size: 12px; font-variant-numeric: tabular-nums; white-space: nowrap; }.rank-number { display: grid; place-items: center; width: 23px; height: 23px; color: #5d6e86; font-size: 11px; font-weight: 700; background: #edf2f8; border-radius: 50%; }.popularity-row { grid-template-columns: 25px minmax(0, 1fr) auto; }
@media (max-width: 1200px) { .market-data-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 720px) { .market-data-board { margin-top: 30px; }.market-data-grid { grid-template-columns: 1fr; }.market-data-row { grid-template-columns: minmax(84px, .8fr) minmax(0, 1.2fr) auto; gap: 8px; } }
</style>
