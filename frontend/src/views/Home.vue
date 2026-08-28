<template>
  <main class="research-desk">
    <header class="desk-heading">
      <div><p class="eyebrow">{{ $t('homeDesk.eyebrow') }}</p><h1>{{ $t('homeDesk.title') }}</h1></div>
      <div class="desk-actions"><a class="market-cloud-link" href="https://quote.eastmoney.com/stockhotmap/" target="_blank" rel="noopener noreferrer"><CloudOutlined />{{ $t('homeDesk.marketCloudMap') }}</a></div>
    </header>
    <section class="market-overview">
      <section class="focus-panel" :aria-label="$t('homeDesk.currentFocus')">
        <div class="focus-copy"><span class="focus-label">{{ $t('homeDesk.currentFocus') }}</span><h2>{{ $t('homeDesk.focusQuestion') }}</h2><p>{{ $t('homeDesk.focusDescription') }}</p><a-button type="link" class="review-link" @click="goToMarketReview">{{ $t('homeDesk.readReview') }} <ArrowRightOutlined /></a-button></div>
      </section>
      <section class="market-quick-card" :aria-label="$t('homeDesk.marketSnapshot')"><p class="side-kicker">{{ $t('homeDesk.marketSnapshot') }}</p><div v-if="marketIndexList.length" class="quick-index-list"><div v-for="index in marketIndexList" :key="index.symbol"><span>{{ index.name }}</span><strong>{{ index.value }}</strong><em :class="index.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(index.changePct) }}</em></div></div><a-skeleton v-else :paragraph="{rows: 3}" active /></section>
      <section class="market-quick-card market-activity-quick" :aria-label="$t('home.marketActivity')">
        <p class="side-kicker">{{ $t('home.marketActivity') }}</p>
        <div class="market-activity-stats">
          <div class="positive"><strong>{{ marketActivity?.up ?? '--' }}</strong><span>{{ $t('home.up') }}</span></div>
          <div class="negative"><strong>{{ marketActivity?.down ?? '--' }}</strong><span>{{ $t('home.down') }}</span></div>
          <div class="positive"><strong>{{ marketActivity?.limitUp ?? '--' }}</strong><span>{{ $t('home.limitUp') }}</span></div>
          <div class="negative"><strong>{{ marketActivity?.limitDown ?? '--' }}</strong><span>{{ $t('home.limitDown') }}</span></div>
        </div>
        <div class="market-breadth-bar" :aria-label="$t('homeDesk.marketBreadth')">
          <i class="breadth-up" :style="{width: `${marketUpRatio}%`}"></i>
          <i class="breadth-down" :style="{width: `${marketDownRatio}%`}"></i>
        </div>
      </section>
    </section>
    <section class="market-board" :aria-label="$t('homeDesk.marketInsights')">
      <div class="section-heading"><div><p class="eyebrow">{{ $t('homeDesk.marketInsights') }}</p><h2>{{ $t('homeDesk.capitalSignals') }}</h2></div><span class="insights-as-of">{{ $t('homeDesk.asOf', {date: marketInsights?.asOf || '-'}) }}</span></div>
      <div class="insight-grid">
        <section class="insight-panel market-turnover-panel" :aria-label="$t('homeDesk.marketTurnover')">
          <div class="insight-title"><div><p>{{ $t('homeDesk.marketTurnover') }}</p><small>{{ $t('homeDesk.marketTurnoverHint') }}</small><small v-if="marketTurnoverAsOf">{{ $t('homeDesk.asOf', {date: formatTurnoverDate(marketTurnoverAsOf)}) }}</small></div><strong v-if="marketTurnoverHistory.length" class="turnover-value">{{ formatTurnover(marketTurnover?.amount) }}</strong></div>
          <div v-if="marketTurnoverHistory.length" ref="marketTurnoverChartRef" class="market-turnover-chart" />
          <a-skeleton v-else :paragraph="{rows: 7}" active />
        </section>
        <section class="insight-panel dragon-tiger-panel" :aria-label="$t('homeDesk.dragonTiger')">
          <div class="insight-title"><div><p>{{ $t('homeDesk.dragonTiger') }}</p><small>{{ $t('homeDesk.dragonTigerHint') }}</small></div><span>{{ $t('homeDesk.netBuy') }}</span></div>
          <a-skeleton v-if="!dragonTigerList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="dragonTigerList.length" class="insight-list">
            <article v-for="item in pagedDragonTigerList" :key="`${item.code}-${item.listedDate}`"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.listedDate }}</small></div><p class="insight-reason">{{ item.reason || '-' }}</p><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }}</b></div></article>
          </div>
          <a-pagination v-if="dragonTigerList.length > insightPageSize" v-model:current="dragonTigerPage" simple size="small" :page-size="insightPageSize" :total="dragonTigerList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </section>
        <section class="insight-panel fund-flow-panel" :aria-label="$t('homeDesk.fundFlow')">
          <div class="insight-title"><div><p>{{ $t('homeDesk.fundFlow') }}</p><small>{{ $t('homeDesk.fundFlowHint') }}</small></div><span>{{ $t('homeDesk.mainNetInflow') }}</span></div>
          <a-skeleton v-if="!fundFlowList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="fundFlowList.length" class="insight-list">
            <article v-for="item in pagedFundFlowList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.sector ? ` · ${item.sector}` : '' }}</small></div><p class="insight-price">{{ $t('homeDesk.latestPrice') }} {{ formatPrice(item.price) }}</p><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }} <small>{{ formatRatio(item.netRatio) }}</small></b></div></article>
          </div>
          <a-pagination v-if="fundFlowList.length > insightPageSize" v-model:current="fundFlowPage" simple size="small" :page-size="insightPageSize" :total="fundFlowList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </section>
        <section class="insight-panel popularity-rank-panel" :aria-label="$t('homeDesk.popularityRank')">
          <div class="insight-title"><div><p>{{ $t('homeDesk.popularityRank') }}</p><small>{{ $t('homeDesk.popularityRankHint') }}</small></div><span>{{ $t('homeDesk.currentRank') }}</span></div>
          <a-skeleton v-if="!popularityRankList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="popularityRankList.length" class="insight-list">
            <article v-for="item in pagedPopularityRankList" :key="item.code" class="popularity-item"><span class="popularity-rank">{{ item.rank }}</span><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}</small></div><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article>
          </div>
          <a-pagination v-if="popularityRankList.length > insightPageSize" v-model:current="popularityRankPage" simple size="small" :page-size="insightPageSize" :total="popularityRankList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </section>
        <section class="insight-panel industry-sectors-panel" :aria-label="$t('homeDesk.industrySectors')">
          <div class="insight-title"><div><p>{{ $t('homeDesk.industrySectors') }}</p><small>{{ $t('homeDesk.industrySectorsHint') }}</small></div><span>{{ $t('homeDesk.industryNetInflow') }}</span></div>
          <a-skeleton v-if="!industrySectorsList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <a-table v-else-if="industrySectorsList.length" class="industry-sector-table" :columns="industrySectorColumns" :data-source="industrySectorsList" :pagination="{pageSize: insightPageSize, size: 'small', showSizeChanger: false}" :row-key="item => item.name" size="small" :scroll="{x: 540}">
            <template #bodyCell="{column, record}">
              <span v-if="column.key === 'changePct'" :class="record.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(record.changePct) }}</span>
              <span v-else-if="column.key === 'netAmount'" :class="record.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(record.netAmount) }}</span>
              <span v-else-if="column.key === 'leadingStock'">{{ record.leadingStock || '-' }} <small :class="record.leadingStockChangePct >= 0 ? 'positive' : 'negative'">{{ formatChange(record.leadingStockChangePct) }}</small></span>
            </template>
          </a-table>
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </section>
      </div>
      <div class="limit-pool-grid">
        <section class="insight-panel limit-up-panel" :aria-label="$t('homeDesk.limitUpPool')"><div class="insight-title"><div><p>{{ $t('homeDesk.limitUpPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div><a-skeleton v-if="!limitUpPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active /><div v-else-if="limitUpPoolList.length" class="insight-list"><article v-for="item in pagedLimitUpPoolList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p class="insight-price">{{ item.detail }}</p><div class="insight-values"><em class="positive">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article></div><a-pagination v-if="limitUpPoolList.length > insightPageSize" v-model:current="limitUpPoolPage" simple size="small" :page-size="insightPageSize" :total="limitUpPoolList.length" :show-size-changer="false" /><a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" /></section>
        <section class="insight-panel limit-down-panel" :aria-label="$t('homeDesk.limitDownPool')"><div class="insight-title"><div><p>{{ $t('homeDesk.limitDownPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div><a-skeleton v-if="!limitDownPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active /><div v-else-if="limitDownPoolList.length" class="insight-list"><article v-for="item in pagedLimitDownPoolList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p class="insight-price">{{ item.detail }}</p><div class="insight-values"><em class="negative">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article></div><a-pagination v-if="limitDownPoolList.length > insightPageSize" v-model:current="limitDownPoolPage" simple size="small" :page-size="insightPageSize" :total="limitDownPoolList.length" :show-size-changer="false" /><a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" /></section>
        <section class="insight-panel strong-pool-panel" :aria-label="$t('homeDesk.strongPool')"><div class="insight-title"><div><p>{{ $t('homeDesk.strongPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div><a-skeleton v-if="!strongPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active /><div v-else-if="strongPoolList.length" class="insight-list"><article v-for="item in pagedStrongPoolList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p class="insight-price">{{ item.detail }}</p><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article></div><a-pagination v-if="strongPoolList.length > insightPageSize" v-model:current="strongPoolPage" simple size="small" :page-size="insightPageSize" :total="strongPoolList.length" :show-size-changer="false" /><a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" /></section>
        <section class="insight-panel broken-pool-panel" :aria-label="$t('homeDesk.brokenPool')"><div class="insight-title"><div><p>{{ $t('homeDesk.brokenPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div><a-skeleton v-if="!brokenPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active /><div v-else-if="brokenPoolList.length" class="insight-list"><article v-for="item in pagedBrokenPoolList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p class="insight-price">{{ item.detail }}</p><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article></div><a-pagination v-if="brokenPoolList.length > insightPageSize" v-model:current="brokenPoolPage" simple size="small" :page-size="insightPageSize" :total="brokenPoolList.length" :show-size-changer="false" /><a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" /></section>
      </div>
    </section>
    <section class="desk-manifesto" :aria-label="$t('homeDesk.manifestoTitle')">
      <p class="manifesto-lead">{{ $t('homeDesk.manifestoLead') }}</p>
      <p class="manifesto-body">{{ $t('homeDesk.manifestoBody') }}</p>
      <blockquote>{{ $t('homeDesk.manifestoDisclaimer') }}</blockquote>
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
const popularityRankList = computed(() => marketInsights.value?.popularityRank || [])
const industrySectorsList = computed(() => marketInsights.value?.industrySectors || [])
const industrySectorColumns = computed(() => [
  {title: t('homeDesk.industrySectors'), dataIndex: 'name', key: 'name', ellipsis: true},
  {title: t('homeDesk.industryChange'), dataIndex: 'changePct', key: 'changePct', align: 'right'},
  {title: t('homeDesk.industryNetInflow'), dataIndex: 'netAmount', key: 'netAmount', align: 'right'},
  {title: t('homeDesk.leadingStock'), dataIndex: 'leadingStock', key: 'leadingStock', ellipsis: true}
])
const limitPools = computed(() => marketInsights.value?.limitPools || {})
const limitUpPoolList = computed(() => limitPools.value.limitUp || [])
const limitDownPoolList = computed(() => limitPools.value.limitDown || [])
const strongPoolList = computed(() => limitPools.value.strong || [])
const brokenPoolList = computed(() => limitPools.value.broken || [])
const pageItems = (items, currentPage) => items.slice((currentPage - 1) * insightPageSize, currentPage * insightPageSize)
const pagedDragonTigerList = computed(() => pageItems(dragonTigerList.value, dragonTigerPage.value))
const pagedFundFlowList = computed(() => pageItems(fundFlowList.value, fundFlowPage.value))
const pagedPopularityRankList = computed(() => pageItems(popularityRankList.value, popularityRankPage.value))
const pagedLimitUpPoolList = computed(() => pageItems(limitUpPoolList.value, limitUpPoolPage.value))
const pagedLimitDownPoolList = computed(() => pageItems(limitDownPoolList.value, limitDownPoolPage.value))
const pagedStrongPoolList = computed(() => pageItems(strongPoolList.value, strongPoolPage.value))
const pagedBrokenPoolList = computed(() => pageItems(brokenPoolList.value, brokenPoolPage.value))
watch(dragonTigerList, () => { dragonTigerPage.value = 1 })
watch(fundFlowList, () => { fundFlowPage.value = 1 })
watch(popularityRankList, () => { popularityRankPage.value = 1 })
watch(limitUpPoolList, () => { limitUpPoolPage.value = 1 })
watch(limitDownPoolList, () => { limitDownPoolPage.value = 1 })
watch(strongPoolList, () => { strongPoolPage.value = 1 })
watch(brokenPoolList, () => { brokenPoolPage.value = 1 })
const hasSameTurnover = (first, second) => first?.date === second?.date && Number(first?.amount) === Number(second?.amount)
const applyMarketTurnoverUpdate = turnover => {
  turnover = unwrapMarketTurnover(turnover)
  if (!hasMarketTurnoverHistory(turnover)) return

  const latestIncomingItem = getLatestTurnoverItem(turnover)
  const latestCachedItem = getLatestTurnoverItem(marketTurnover.value)
  if (!latestIncomingItem) return

  if (!latestCachedItem) {
    marketTurnover.value = turnover
    return
  }
  if (latestIncomingItem.date < latestCachedItem.date) return
  if (hasSameTurnover(latestIncomingItem, latestCachedItem)
    && Number(turnover.amount) === Number(marketTurnover.value?.amount)) return

  const history = [...marketTurnover.value.history]
  if (latestIncomingItem.date === latestCachedItem.date) history[history.length - 1] = latestIncomingItem
  else history.push(latestIncomingItem)

  marketTurnover.value = {
    ...marketTurnover.value,
    amount: turnover.amount,
    difference: turnover.difference,
    history
  }
}
const getMarketDashboardWebSocketUrl = () => {
  const token = localStorage.getItem('token')
  if (!token) return null
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}${marketDashboardWebSocketPath}?token=${encodeURIComponent(token)}`
}
const reconnectMarketDashboardSocket = () => {
  if (!isMarketDashboardSocketActive || marketDashboardSocketReconnectTimer) return
  marketDashboardSocketReconnectTimer = window.setTimeout(() => {
    marketDashboardSocketReconnectTimer = null
    connectMarketDashboardSocket()
  }, marketDashboardWebSocketReconnectInterval)
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
  if (message.type === marketDashboardInsightsMessageType) {
    marketInsights.value = messageData
    isLoadingMarketInsights.value = false
  }
}
const connectMarketDashboardSocket = () => {
  const url = getMarketDashboardWebSocketUrl()
  if (!url || !isMarketDashboardSocketActive) return

  marketDashboardSocket = new WebSocket(url)
  marketDashboardSocket.onmessage = event => {
    try {
      applyMarketDashboardMessage(JSON.parse(event.data))
    } catch (error) {
      console.warn('Unable to apply market dashboard update', error)
    }
  }
  marketDashboardSocket.onerror = () => marketDashboardSocket?.close()
  marketDashboardSocket.onclose = () => {
    marketDashboardSocket = null
    reconnectMarketDashboardSocket()
  }
}
const formatChange = value => `${value >= 0 ? '+' : ''}${Number(value || 0).toFixed(2)}%`
const formatAmount = value => `${Number(value || 0) >= 0 ? '+' : ''}${(Number(value || 0) / 100000000).toFixed(2)}${t('homeDesk.hundredMillion')}`
const formatTurnover = value => {
  const amount = Number(value || 0)
  if (!amount) return '--'
  return amount >= 1000000000000
    ? `${(amount / 1000000000000).toFixed(2)}${t('homeDesk.trillion')}`
    : `${(amount / 100000000).toFixed(0)}${t('homeDesk.hundredMillion')}`
}
const formatTurnoverDetail = value => `${(Number(value || 0) / 100000000).toFixed(2)}${t('homeDesk.hundredMillion')}`
const formatTurnoverDate = date => String(date || '').replace(/-/g, '/')
const renderMarketTurnoverChart = () => {
  if (!marketTurnoverChartRef.value || !marketTurnoverHistory.value.length) return

  if (!marketTurnoverChart) marketTurnoverChart = init(marketTurnoverChartRef.value)

  const history = marketTurnoverHistory.value
  const option = {
    animation: false,
    grid: {top: 18, right: 4, bottom: 30, left: 4},
    tooltip: {
      trigger: 'axis',
      axisPointer: {type: 'shadow'},
      backgroundColor: '#182336',
      borderWidth: 0,
      padding: [8, 10],
      textStyle: {color: '#ffffff', fontSize: 12},
      formatter: params => {
        const item = history[params[0]?.dataIndex]
        return item ? `${formatTurnoverDate(item.date)}<br/>${t('homeDesk.marketTurnover')}：${formatTurnoverDetail(item.amount)}` : ''
      }
    },
    xAxis: {
      type: 'category',
      data: history.map(item => formatTurnoverDate(item.date).slice(5)),
      axisLine: {lineStyle: {color: '#dce2eb'}},
      axisTick: {show: false},
      axisLabel: {color: '#8996aa', fontSize: 10, interval: Math.max(0, Math.ceil(history.length / 6) - 1)}
    },
    yAxis: {type: 'value', show: false},
    series: [{
      type: 'bar',
      data: history.map(item => Number(item.amount) || 0),
      barMaxWidth: 12,
      itemStyle: {color: '#5d8fd8', borderRadius: [3, 3, 0, 0]},
      emphasis: {itemStyle: {color: '#316bc0'}}
    }]
  }
  const hasSeries = Boolean(marketTurnoverChart.getOption()?.series?.length)
  marketTurnoverChart.setOption(option, {notMerge: !hasSeries, lazyUpdate: true, silent: true})
}
const resizeMarketTurnoverChart = () => marketTurnoverChart?.resize()
watch(marketTurnoverHistory, () => { nextTick(renderMarketTurnoverChart) }, {flush: 'post'})
const formatPrice = value => Number(value || 0).toFixed(2)
const formatRatio = value => `${Number(value || 0) >= 0 ? '+' : ''}${Number(value || 0).toFixed(2)}%`
const goToMarketReview = () => router.push('/market-review')
onMounted(() => {
  isMarketDashboardSocketActive = true
  connectMarketDashboardSocket()
  window.addEventListener('resize', resizeMarketTurnoverChart)
})
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
.research-desk { width: 100%; max-width: none; padding: 24px 32px 52px; margin: 0; color: #182336; }.desk-heading, .section-heading { display: flex; align-items: end; justify-content: space-between; gap: 24px; }.desk-heading { padding: 12px 0 30px; }.eyebrow, .side-kicker { margin: 0 0 7px; color: #72809a; font-size: 11px; font-weight: 800; letter-spacing: .13em; text-transform: uppercase; }.desk-heading h1 { margin: 0; font-size: clamp(28px, 3vw, 36px); letter-spacing: -.045em; }.desk-actions { display: flex; align-items: center; gap: 12px; }.market-cloud-link { display: inline-flex; align-items: center; gap: 6px; padding: 8px 12px; color: #526178; font-size: 13px; font-weight: 600; text-decoration: none; border: 1px solid #dce2eb; border-radius: 8px; transition: color .16s, background .16s, border-color .16s; }.market-cloud-link:hover { color: #316bc0; background: #f1f5fb; border-color: #afc1df; }
.market-overview { display: grid; grid-template-columns: minmax(320px, 1.7fr) repeat(2, minmax(220px, 1fr)); gap: 20px; }
.focus-panel { padding: 20px 24px; color: #fff; background: radial-gradient(circle at 87% 0%, #314a78 0, transparent 37%), #182336; border-radius: 12px; }
.focus-label { color: #89a5d5; font-size: 10px; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }
.focus-copy h2 { max-width: 680px; margin: 6px 0; font-size: clamp(18px, 1.7vw, 22px); line-height: 1.3; letter-spacing: -.025em; }
.focus-copy p { max-width: 880px; margin: 0; color: #b4c2d9; font-size: 12px; line-height: 1.55; }
.review-link { padding-left: 0; margin-top: 6px; color: #a7c1fb; }
.market-quick-card { padding: 20px; background: #f5f7fa; border: 1px solid #e4e9f0; border-radius: 12px; }
.market-quick-card .side-kicker { margin-bottom: 12px; }
.quick-index-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 14px; }
.quick-index-list div { display: grid; grid-template-columns: 1fr auto; gap: 4px; }
.quick-index-list span { color: #526178; font-size: 14px; }
.quick-index-list strong, .quick-index-list em { justify-self: end; font-variant-numeric: tabular-nums; }
.quick-index-list strong { font-size: 20px; line-height: 1.1; }
.quick-index-list em { grid-column: 2; font-size: 13px; font-style: normal; }
.market-activity-quick { color: #fff; background: #31415e; border-color: #31415e; }
.market-activity-quick .side-kicker { color: #afc1df; }
.market-activity-stats { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 18px; margin-top: 18px; }
.market-activity-stats div { display: flex; align-items: baseline; gap: 4px; min-width: 0; }
.market-activity-stats strong { color: #fff; font-size: 28px; line-height: 1; font-variant-numeric: tabular-nums; }
.market-activity-stats span { color: #c5d2e7; font-size: 12px; white-space: nowrap; }
.market-breadth-bar { display: flex; height: 8px; margin-top: 16px; overflow: hidden; background: #536781; border-radius: 999px; }
.market-breadth-bar i { display: block; min-width: 0; transition: width .2s ease; }
.market-breadth-bar .breadth-up { background: #d7505d; }
.market-breadth-bar .breadth-down { background: #00a15c; }
.desk-manifesto { display: grid; gap: 9px; max-width: 830px; padding: 28px 0 4px; margin-top: 48px; border-top: 1px solid #dce2eb; }.manifesto-lead { margin: 0; color: #263957; font-size: clamp(18px, 2vw, 22px); font-weight: 700; line-height: 1.45; letter-spacing: -.025em; }.manifesto-body { max-width: 760px; margin: 0; color: #63718a; font-size: 14px; line-height: 1.8; }.desk-manifesto blockquote { max-width: 760px; padding-left: 14px; margin: 4px 0 0; color: #7c879a; font-size: 13px; font-style: italic; line-height: 1.75; border-left: 2px solid #a8b8d3; }
.section-heading { margin-bottom: 15px; }.section-heading h2 { margin: 0; font-size: 21px; letter-spacing: -.025em; }.section-heading :deep(.ant-btn-link) { padding-right: 0; }
.market-board { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 22px; margin-top: 48px; }.market-board > .section-heading { grid-column: 1 / -1; margin-bottom: -7px; }.insights-as-of { color: #8996aa; font-size: 12px; }.insight-grid, .limit-pool-grid { display: contents; }.insight-panel { min-width: 0; min-height: 334px; padding: 22px; background: #f5f7fa; border: 1px solid #e4e9f0; border-radius: 12px; }.market-turnover-panel { order: 1; }.dragon-tiger-panel { order: 2; }.popularity-rank-panel { order: 3; }.industry-sectors-panel { order: 4; }.limit-up-panel { order: 5; }.strong-pool-panel { order: 6; }.fund-flow-panel { order: 7; }.limit-down-panel { order: 8; }.broken-pool-panel { order: 9; }.insight-title { display: flex; align-items: start; justify-content: space-between; gap: 16px; padding-bottom: 13px; border-bottom: 1px solid #dce2eb; }.insight-title p { margin: 0; color: #182336; font-size: 16px; font-weight: 700; }.insight-title small { display: block; margin-top: 3px; color: #8996aa; font-size: 11px; }.insight-title > span { color: #71809a; font-size: 11px; white-space: nowrap; }.turnover-value { color: #263957; font-size: 23px; line-height: 1.1; font-variant-numeric: tabular-nums; white-space: nowrap; }.market-turnover-chart { width: 100%; height: 246px; margin-top: 12px; }.insight-list article { display: grid; grid-template-columns: minmax(98px, .8fr) minmax(0, 1.3fr) auto; align-items: center; gap: 12px; padding: 13px 0; border-bottom: 1px solid #e0e5ed; }.insight-list .popularity-item { grid-template-columns: 28px minmax(0, 1fr) auto; }.popularity-rank { display: grid; place-items: center; width: 24px; height: 24px; color: #526178; font-size: 12px; font-weight: 700; background: #e6ebf3; border-radius: 50%; }.insight-stock strong { display: block; color: #263957; font-size: 14px; }.insight-stock small, .insight-price, .insight-reason { display: block; margin: 3px 0 0; overflow: hidden; color: #8996aa; font-size: 11px; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }.insight-stock small em { margin-left: 3px; font-style: normal; }.insight-values { display: grid; justify-items: end; gap: 4px; font-variant-numeric: tabular-nums; }.insight-values em { font-size: 11px; font-style: normal; }.insight-values b { font-size: 12px; white-space: nowrap; }.insight-values b small { margin-left: 3px; font-size: 10px; font-weight: 500; }.insight-panel :deep(.ant-pagination) { display: flex; justify-content: end; margin: 16px 0 0; }.insight-panel :deep(.ant-empty) { padding-top: 72px; }.insight-panel :deep(.ant-empty-description) { color: #8996aa; font-size: 12px; }
.industry-sector-table { margin-top: 12px; }.industry-sector-table :deep(.ant-table) { background: transparent; }.industry-sector-table :deep(.ant-table-thead > tr > th) { padding: 9px 8px; color: #71809a; font-size: 12px; background: transparent; }.industry-sector-table :deep(.ant-table-tbody > tr > td) { padding: 11px 8px; color: #526178; font-size: 14px; background: transparent; }.industry-sector-table :deep(.ant-table-tbody small) { font-size: 12px; }.industry-sector-table :deep(.ant-pagination) { margin: 12px 0 0; }
.positive { color: #d7505d; }.negative { color: #006d2c; }
.research-desk { padding: 18px 24px 36px; }.desk-heading { padding: 8px 0 22px; }.market-overview { gap: 16px; }.market-quick-card, .focus-panel { padding: 16px 18px; }.market-board { gap: 16px; margin-top: 32px; }.market-board > .section-heading { margin-bottom: -3px; }.insight-panel { min-height: 298px; padding: 18px; }.market-turnover-chart { height: 216px; margin-top: 8px; }.insight-list article { gap: 10px; padding: 10px 0; }.insight-panel :deep(.ant-pagination) { margin-top: 12px; }.industry-sector-table { margin-top: 8px; }.industry-sector-table :deep(.ant-table-thead > tr > th) { padding: 7px 6px; }.industry-sector-table :deep(.ant-table-tbody > tr > td) { padding: 9px 6px; }.desk-manifesto { padding-top: 22px; margin-top: 32px; }
@media (max-width: 1100px) { .market-overview { grid-template-columns: repeat(2, minmax(0, 1fr)); }.focus-panel { grid-column: span 2; }.market-board { grid-template-columns: repeat(2, minmax(0, 1fr)); } } @media (max-width: 800px) { .research-desk { padding: 16px 16px 40px; }.desk-heading, .section-heading { align-items: start; flex-direction: column; }.desk-actions { width: 100%; }.market-cloud-link { flex: 1; justify-content: center; }.market-overview, .market-board { grid-template-columns: 1fr; }.focus-panel { grid-column: auto; }.quick-index-list { grid-template-columns: 1fr 1fr; row-gap: 12px; }.insight-list article { grid-template-columns: minmax(86px, .8fr) minmax(0, 1.2fr) auto; gap: 8px; } }
</style>
