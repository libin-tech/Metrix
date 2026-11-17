<template>
  <main class="research-desk">
    <section class="market-pulse" :aria-label="$t('homeDesk.marketPulse')">
      <div class="pulse-heading"><div><h1 class="workspace-title">{{ $t('homeDesk.marketPulseTitle') }}</h1></div><div class="pulse-actions"><span role="status" class="connection-status" :class="marketDashboardConnectionState">{{ $t(`homeDesk.connection${marketDashboardConnectionState}`) }}</span><a-button type="link" @click="goToMarketReview">{{ $t('homeDesk.readReview') }} <ArrowRightOutlined /></a-button></div></div>
      <div class="market-dashboard">
        <div class="overview-grid">
          <article class="pulse-card turnover-card">
            <div class="card-heading"><div><p>{{ $t('homeDesk.marketTurnover') }}</p><small>{{ marketTurnoverAsOf ? $t('homeDesk.asOf', {date: formatTurnoverDate(marketTurnoverAsOf)}) : $t('homeDesk.marketTurnoverHint') }}</small></div><div v-if="marketTurnoverHistory.length" class="turnover-summary"><strong>{{ formatTurnover(marketTurnover?.amount) }}</strong><small>{{ $t('homeDesk.turnoverComparedPrevious') }}<span :class="{'positive': marketTurnoverDifference > 0, 'negative': marketTurnoverDifference < 0}">{{ turnoverDifferenceText }}</span></small></div></div>
            <div v-if="marketTurnoverHistory.length" ref="marketTurnoverChartRef" class="market-turnover-chart" />
            <a-skeleton v-else :paragraph="{rows: 5}" active />
          </article>
          <article class="pulse-card index-card">
            <div class="card-heading"><div><p>{{ $t('homeDesk.marketSnapshot') }}</p><small>{{ $t('homeDesk.indexesHint') }}</small></div></div>
            <div v-if="marketIndexList.length" class="quick-index-list">
              <div v-for="index in marketIndexList" :key="index.symbol" class="index-quote" :class="{'index-quote-up': index.changePct > 0, 'index-quote-down': index.changePct < 0}">
                <span class="index-quote-name" :title="index.name">{{ index.name }}</span>
                <strong class="index-quote-value">{{ index.value }}</strong>
                <em class="index-quote-change"><span aria-hidden="true" class="index-quote-direction" />{{ formatChange(index.changePct) }}</em>
              </div>
            </div>
            <a-skeleton v-else :paragraph="{rows: 3}" active />
          </article>
          <article class="pulse-card breadth-card">
            <div class="card-heading"><div><p>{{ $t('homeDesk.marketBreadth') }}</p><small>{{ $t('homeDesk.marketActivityHint') }}</small></div></div>
            <div class="breadth-values">
              <div class="breadth-metric breadth-positive"><span>{{ $t('home.up') }}</span><strong>{{ marketActivity?.up ?? '--' }}</strong></div>
              <div class="breadth-metric breadth-negative"><span>{{ $t('home.down') }}</span><strong>{{ marketActivity?.down ?? '--' }}</strong></div>
              <div class="breadth-metric breadth-positive breadth-limit"><span>{{ $t('home.limitUp') }}</span><strong>{{ marketActivity?.limitUp ?? '--' }}</strong></div>
              <div class="breadth-metric breadth-negative breadth-limit"><span>{{ $t('home.limitDown') }}</span><strong>{{ marketActivity?.limitDown ?? '--' }}</strong></div>
            </div>
            <div class="market-breadth-bar" :aria-label="$t('homeDesk.marketBreadth')"><i class="breadth-up" :style="{width: `${marketUpRatio}%`}" /><i class="breadth-down" :style="{width: `${marketDownRatio}%`}" /></div>
            <small v-if="marketActivity?.stale" class="breadth-cache-note">{{ $t('homeDesk.marketBreadthCached') }}</small>
          </article>
        </div>
        <aside class="intelligence-column" :aria-label="$t('dailyMarket.title')">
          <DailyMarketIntelligenceCard />
        </aside>
        <article class="pulse-card sector-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.industrySectors') }}</p><small>{{ $t('homeDesk.industrySectorsHint') }}</small></div><span class="sector-sort-label">{{ $t('homeDesk.industrySortByChange') }}</span></div>
          <div v-if="sectorPulse.length" class="sector-grid" tabindex="0" role="region" :aria-label="$t('homeDesk.industrySectors')">
            <div v-for="sector in sectorPulse" :key="sector.name" :title="`${sector.name} · ${formatChange(sector.changePct)}`" class="sector-tile" :class="{'sector-up': sector.changePct > 0, 'sector-down': sector.changePct < 0}">
              <strong>{{ sector.name }}</strong>
              <span class="sector-change">{{ formatChange(sector.changePct) }}</span>
            </div>
          </div>
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
      </div>
      <div class="signals-grid">
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.fundFlow') }}</p><small>{{ $t('homeDesk.fundFlowHint') }}</small></div><span>{{ $t('homeDesk.mainNetInflow') }}</span></div>
          <a-skeleton v-if="!fundFlowList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="fundFlowList.length" class="market-data-list"><div v-for="item in pagedFundFlowList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.sector ? ` · ${item.sector}` : '' }}</small></div><p>{{ $t('homeDesk.latestPrice') }} {{ formatPrice(item.price) }}</p><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }}</b></div></div>
          <a-pagination v-if="fundFlowList.length > insightPageSize" v-model:current="fundFlowPage" simple size="small" :page-size="insightPageSize" :total="fundFlowList.length" :show-size-changer="false" />
          <a-empty v-if="!fundFlowList.length && !isLoadingMarketInsights" :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.dragonTiger') }}</p><small>{{ $t('homeDesk.dragonTigerHint') }}</small></div><span>{{ $t('homeDesk.netBuy') }}</span></div>
          <a-skeleton v-if="!dragonTigerList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="dragonTigerList.length" class="market-data-list"><div v-for="(item, rowIndex) in pagedDragonTigerList" :key="`${item.code}-${item.listedDate}-${rowIndex}`" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.listedDate }}</small></div><p>{{ item.reason || '-' }}</p><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }}</b></div></div>
          <a-pagination v-if="dragonTigerList.length > insightPageSize" v-model:current="dragonTigerPage" simple size="small" :page-size="insightPageSize" :total="dragonTigerList.length" :show-size-changer="false" />
          <a-empty v-if="!dragonTigerList.length && !isLoadingMarketInsights" :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.limitUpPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div>
          <a-skeleton v-if="!limitUpPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="limitUpPoolList.length" class="market-data-list"><div v-for="item in pagedLimitUpPoolList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p>{{ item.detail || '-' }}</p><b class="positive">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="limitUpPoolList.length > insightPageSize" v-model:current="limitUpPoolPage" simple size="small" :page-size="insightPageSize" :total="limitUpPoolList.length" :show-size-changer="false" />
          <a-empty v-if="!limitUpPoolList.length && !isLoadingMarketInsights" :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.limitDownPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div>
          <a-skeleton v-if="!limitDownPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="limitDownPoolList.length" class="market-data-list"><div v-for="item in pagedLimitDownPoolList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p>{{ item.detail || '-' }}</p><b class="negative">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="limitDownPoolList.length > insightPageSize" v-model:current="limitDownPoolPage" simple size="small" :page-size="insightPageSize" :total="limitDownPoolList.length" :show-size-changer="false" />
          <a-empty v-if="!limitDownPoolList.length && !isLoadingMarketInsights" :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.strongPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div>
          <a-skeleton v-if="!strongPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="strongPoolList.length" class="market-data-list"><div v-for="item in pagedStrongPoolList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p>{{ item.detail || '-' }}</p><b :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="strongPoolList.length > insightPageSize" v-model:current="strongPoolPage" simple size="small" :page-size="insightPageSize" :total="strongPoolList.length" :show-size-changer="false" />
          <a-empty v-if="!strongPoolList.length && !isLoadingMarketInsights" :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.popularityRank') }}</p><small>{{ $t('homeDesk.popularityRankHint') }}</small></div><span>{{ $t('homeDesk.currentRank') }}</span></div>
          <a-skeleton v-if="!popularityRankList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="popularityRankList.length" class="market-data-list"><div v-for="item in pagedPopularityRankList" :key="item.code" class="market-data-row popularity-row"><span class="rank-number">{{ item.rank }}</span><div><strong>{{ item.name }}</strong><small>{{ item.code }}</small></div><b :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="popularityRankList.length > insightPageSize" v-model:current="popularityRankPage" simple size="small" :page-size="insightPageSize" :total="popularityRankList.length" :show-size-changer="false" />
          <a-empty v-if="!popularityRankList.length && !isLoadingMarketInsights" :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
        <article class="market-data-card">
          <div class="card-heading"><div><p>{{ $t('homeDesk.brokenPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div>
          <a-skeleton v-if="!brokenPoolList.length && isLoadingMarketInsights" :paragraph="{rows: 4}" active />
          <div v-else-if="brokenPoolList.length" class="market-data-list"><div v-for="item in pagedBrokenPoolList" :key="item.code" class="market-data-row"><div><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p>{{ item.detail || '-' }}</p><b :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</b></div></div>
          <a-pagination v-if="brokenPoolList.length > insightPageSize" v-model:current="brokenPoolPage" simple size="small" :page-size="insightPageSize" :total="brokenPoolList.length" :show-size-changer="false" />
          <a-empty v-if="!brokenPoolList.length && !isLoadingMarketInsights" :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import DailyMarketIntelligenceCard from '../components/DailyMarketIntelligenceCard.vue'
import {useTheme} from '../composables/useTheme'
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {Empty} from 'ant-design-vue'
import {ArrowRightOutlined} from '@ant-design/icons-vue'
import {init, use} from 'echarts/core'
import {BarChart, LineChart} from 'echarts/charts'
import {GridComponent, LegendComponent, TooltipComponent} from 'echarts/components'
import {CanvasRenderer} from 'echarts/renderers'
import {calculateMarketTurnoverAverage, getLatestTurnoverItem, hasMarketTurnoverHistory, MARKET_TURNOVER_THRESHOLD, unwrapMarketTurnover} from '../composables/marketData'

use([BarChart, LineChart, CanvasRenderer, GridComponent, LegendComponent, TooltipComponent])

const {isDark} = useTheme()
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
const marketDashboardConnectionState = ref('Connecting')
let marketDashboardSocket = null
let marketDashboardSocketReconnectTimer = null
let isMarketDashboardSocketActive = false
const isLoadingMarketInsights = ref(true)
let marketTurnoverChart = null
let marketTurnoverObserver = null
const marketTurnoverChartRef = ref(null)
const insightPageSize = 8
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
const marketTurnoverDifference = computed(() => {
  const history = marketTurnoverHistory.value
  if (history.length < 2) return null
  const current = marketTurnover.value?.amount
  const previous = history[history.length - 2]?.amount
  if (current == null || previous == null || !Number.isFinite(Number(current)) || !Number.isFinite(Number(previous))) return null
  return Number(current) - Number(previous)
})
const turnoverDifferenceText = computed(() => {
  const difference = marketTurnoverDifference.value
  if (difference == null) return '--'
  const labels = {'-1': 'homeDesk.turnoverDecreased', '0': 'homeDesk.turnoverUnchanged', '1': 'homeDesk.turnoverIncreased'}
  return t(labels[Math.sign(difference)], {amount: formatTurnoverDetail(Math.abs(difference))})
})
const dragonTigerList = computed(() => marketInsights.value?.dragonTiger || [])
const fundFlowList = computed(() => marketInsights.value?.fundFlow || [])
const industrySectorsList = computed(() => marketInsights.value?.industrySectors || [])
const sectorPulse = computed(() => [...industrySectorsList.value].sort((left, right) => Number(right.changePct) - Number(left.changePct)))
const popularityRankList = computed(() => marketInsights.value?.popularityRank || [])
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
  if (!isMarketDashboardSocketActive) return
  if (!url) { router.replace('/login'); return }
  marketDashboardSocket = new WebSocket(url)
  marketDashboardSocket.onopen = () => { marketDashboardConnectionState.value = 'Connected' }
  marketDashboardSocket.onmessage = event => { try { applyMarketDashboardMessage(JSON.parse(event.data)) } catch (error) { console.warn('Unable to apply market dashboard update', error) } }
  marketDashboardSocket.onerror = () => marketDashboardSocket?.close()
  marketDashboardSocket.onclose = () => { marketDashboardSocket = null; marketDashboardConnectionState.value = 'Reconnecting'; reconnectMarketDashboardSocket() }
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
  if (!marketTurnoverChart) {
    marketTurnoverChart = init(marketTurnoverChartRef.value)
    marketTurnoverObserver = new ResizeObserver(() => marketTurnoverChart?.resize())
    marketTurnoverObserver.observe(marketTurnoverChartRef.value)
  }
  const history = marketTurnoverHistory.value
  const marketTurnoverAverage = calculateMarketTurnoverAverage(history)
  const referenceSeries = value => history.map(() => value)
  const styles = getComputedStyle(document.documentElement)
  const color = name => styles.getPropertyValue(name).trim()
  marketTurnoverChart.setOption({
    animation: false,
    grid: {top: 28, right: 8, bottom: 18, left: 14},
    legend: {data: [t('homeDesk.turnoverThreshold'), t('homeDesk.turnoverAverage60')], top: 0, right: 0, itemWidth: 14, itemHeight: 2, textStyle: {color: color('--workspace-muted'), fontSize: 10}},
    tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}, backgroundColor: color('--workspace-surface'), borderColor: color('--workspace-line'), borderWidth: 1, padding: [8, 10], textStyle: {color: color('--workspace-ink'), fontSize: 12}, formatter: params => {
      const bar = params.find(param => param.seriesType === 'bar')
      const item = history[bar?.dataIndex]
      if (!item) return ''
      const averageText = marketTurnoverAverage == null ? '--' : formatTurnoverDetail(marketTurnoverAverage)
      return `${formatTurnoverDate(item.date)}<br/>${t('homeDesk.marketTurnover')}：${formatTurnoverDetail(item.amount)}<br/>${t('homeDesk.turnoverThreshold')}：${formatTurnoverDetail(MARKET_TURNOVER_THRESHOLD)}<br/>${t('homeDesk.turnoverAverage60')}：${averageText}`
    }},
    xAxis: {type: 'category', data: history.map(item => formatTurnoverDate(item.date).slice(5)), axisLine: {lineStyle: {color: color('--workspace-line')}}, axisTick: {show: false}, axisLabel: {color: color('--workspace-muted'), fontSize: 10, interval: Math.max(0, Math.ceil(history.length / 5) - 1)}},
    yAxis: {type: 'value', show: false},
    series: [
      {type: 'bar', data: history.map(item => Number(item.amount) || 0), barMaxWidth: 12, itemStyle: {color: color('--workspace-chart'), borderRadius: [3, 3, 0, 0]}, emphasis: {itemStyle: {color: color('--workspace-accent')}}},
      {name: t('homeDesk.turnoverThreshold'), type: 'line', data: referenceSeries(MARKET_TURNOVER_THRESHOLD), showSymbol: false, silent: true, z: 3, lineStyle: {color: color('--theme-amber'), type: 'dashed', width: 1.5}},
      {name: t('homeDesk.turnoverAverage60'), type: 'line', data: marketTurnoverAverage == null ? [] : referenceSeries(marketTurnoverAverage), showSymbol: false, silent: true, z: 3, lineStyle: {color: color('--theme-green'), width: 1.5}}
    ]
  }, {notMerge: true, lazyUpdate: true, silent: true})
}
const goToMarketReview = () => router.push('/market-review')

watch(isDark, () => nextTick(renderMarketTurnoverChart))
watch(marketTurnoverHistory, () => { nextTick(renderMarketTurnoverChart) }, {flush: 'post'})
watch(dragonTigerList, () => { dragonTigerPage.value = 1 })
watch(fundFlowList, () => { fundFlowPage.value = 1 })
watch(popularityRankList, () => { popularityRankPage.value = 1 })
watch(limitUpPoolList, () => { limitUpPoolPage.value = 1 })
watch(limitDownPoolList, () => { limitDownPoolPage.value = 1 })
watch(strongPoolList, () => { strongPoolPage.value = 1 })
watch(brokenPoolList, () => { brokenPoolPage.value = 1 })
onMounted(() => { isMarketDashboardSocketActive = true; connectMarketDashboardSocket() })
onUnmounted(() => {
  isMarketDashboardSocketActive = false
  if (marketDashboardSocketReconnectTimer) window.clearTimeout(marketDashboardSocketReconnectTimer)
  marketDashboardSocket?.close()
  marketTurnoverObserver?.disconnect()
  marketTurnoverChart?.dispose()
  marketTurnoverChart = null
})
</script>

<style scoped>
.research-desk {
  --home-card-padding: clamp(16px, 1.5vw, 22px);
  --home-card-gap: clamp(16px, 1.5vw, 22px);
  color: var(--workspace-ink);
}

.market-pulse { display: grid; gap: clamp(18px, 1.65vw, 24px); }

.pulse-heading, .card-heading {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px 14px;
}

.pulse-heading { align-items: center; }
.pulse-heading :deep(.ant-btn-link) { padding-inline-end: 0; }
.pulse-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 10px 16px; }
.connection-status { display: inline-flex; align-items: center; gap: 6px; color: var(--workspace-muted); font-size: 13px; }
.connection-status::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: var(--theme-amber); }
.connection-status.Connected::before { background: var(--theme-green); }

.market-dashboard, .overview-grid, .signals-grid { display: grid; gap: var(--home-card-gap); min-width: 0; }

.market-dashboard {
  grid-template-columns: minmax(0, 2.65fr) minmax(320px, 1.4fr);
  grid-template-rows: auto 1fr;
  grid-template-areas:
    'overview intelligence'
    'industry intelligence';
  align-items: start;
}

.overview-grid {
  grid-area: overview;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr) minmax(0, .7fr);
}

.sector-card { grid-area: industry; }
.intelligence-column { grid-area: intelligence; min-width: 0; }

.pulse-card, .market-data-card {
  min-width: 0;
  padding: var(--home-card-padding);
  background: var(--workspace-surface);
  border: 1px solid var(--workspace-line);
  border-radius: var(--workspace-radius);
  box-shadow: var(--workspace-shadow);
}

.card-heading { align-items: start; padding-bottom: 13px; border-bottom: 1px solid var(--workspace-line); }
.card-heading p { margin: 0; font-size: 15px; font-weight: 680; }
.card-heading small { display: block; margin-top: 4px; color: var(--workspace-muted); font-size: 11px; line-height: 1.5; }
.card-heading > span { color: var(--workspace-muted); font-size: 11px; }
.turnover-summary { text-align: end; }
.turnover-summary strong { font-size: 17px; font-weight: 700; font-variant-numeric: tabular-nums; }
.turnover-summary small { margin-top: 4px; }

.market-turnover-chart { width: 100%; height: 162px; margin-top: 12px; }
.turnover-card > :deep(.ant-skeleton), .index-card > :deep(.ant-skeleton) { margin-top: 14px; }

.quick-index-list, .breadth-values {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.index-quote, .breadth-metric, .sector-tile {
  min-width: 0;
  padding: 9px;
  background: var(--workspace-raised);
  border: 1px solid var(--workspace-line);
  border-radius: var(--workspace-control-radius);
}

.index-quote {
  --quote-color: var(--workspace-muted);
  display: grid;
  gap: 6px;
}

.index-quote-up { --quote-color: var(--theme-red); }
.index-quote-down { --quote-color: var(--theme-green); }
.index-quote-name { min-width: 0; color: var(--workspace-muted); font-size: 12px; overflow-wrap: anywhere; }
.index-quote-value { font-size: clamp(16px, 1.2vw, 23px); line-height: 1.15; font-weight: 700; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.index-quote-change { display: inline-flex; align-items: center; gap: 5px; color: var(--quote-color); font-size: 12px; font-style: normal; font-weight: 630; font-variant-numeric: tabular-nums; }
.index-quote-direction { width: 6px; height: 2px; background: currentColor; }
.index-quote-up .index-quote-direction, .index-quote-down .index-quote-direction { width: 0; height: 0; border-inline: 3px solid transparent; background: transparent; }
.index-quote-up .index-quote-direction { border-bottom: 5px solid currentColor; }
.index-quote-down .index-quote-direction { border-top: 5px solid currentColor; }

.breadth-metric {
  display: grid;
  gap: 8px;
  color: var(--breadth-color);
}

.breadth-positive { --breadth-color: var(--theme-red); }
.breadth-negative { --breadth-color: var(--theme-green); }
.breadth-metric span { display: inline-flex; align-items: center; gap: 5px; color: var(--workspace-muted); font-size: 11px; }
.breadth-metric span::before { content: ''; width: 5px; height: 5px; flex-shrink: 0; border-radius: 50%; background: var(--breadth-color); }
.breadth-metric strong { justify-self: end; font-size: clamp(19px, 1.6vw, 25px); line-height: 1; font-variant-numeric: tabular-nums; }
.breadth-limit strong { font-size: 19px; }
.market-breadth-bar { display: flex; height: 8px; overflow: hidden; margin-top: 14px; background: var(--workspace-raised); border-radius: 999px; }
.market-breadth-bar i { min-width: 0; transition: width .2s ease; }
.breadth-up { background: var(--theme-red); }
.breadth-down { background: var(--theme-green); }
.breadth-cache-note { display: block; margin-top: 8px; color: var(--workspace-muted); font-size: 12px; }

.sector-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 110px), 1fr));
  gap: 8px;
  max-height: 240px;
  padding: 1px;
  margin-top: 14px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--workspace-line) transparent;
  overscroll-behavior-y: contain;
}

.sector-grid:focus-visible { outline: 2px solid var(--workspace-accent); outline-offset: 3px; border-radius: 7px; }
.sector-tile { --sector-color: var(--workspace-muted); display: grid; align-content: center; gap: 6px; min-height: 54px; color: var(--sector-color); }
.sector-up { --sector-color: var(--theme-red); }
.sector-down { --sector-color: var(--theme-green); }
.sector-tile strong { overflow: hidden; color: var(--workspace-ink); font-size: 11px; font-weight: 550; text-overflow: ellipsis; white-space: nowrap; }
.sector-change { display: flex; align-items: center; justify-content: space-between; gap: 4px; font-size: 12px; font-weight: 680; font-variant-numeric: tabular-nums; }
.sector-change::before { content: ''; width: 12px; height: 3px; border-radius: 999px; background: currentColor; opacity: .55; }
.sector-sort-label { padding: 4px 8px; background: var(--workspace-raised); border-radius: 5px; }
@media (hover: hover) { .sector-tile:hover { border-color: var(--sector-color); } }
@media (prefers-reduced-motion: reduce) { .market-breadth-bar i { transition: none; } }

.signals-grid { grid-template-columns: repeat(7, minmax(0, 1fr)); }
.market-data-card { min-height: 300px; padding: clamp(14px, 1.2vw, 18px); }
.market-data-card :deep(.ant-pagination) { display: flex; justify-content: end; margin: 16px 0 0; }
.market-data-card :deep(.ant-empty), .sector-card :deep(.ant-empty) { margin: 30px 0 8px; }
.market-data-card :deep(.ant-empty-description), .sector-card :deep(.ant-empty-description) { color: var(--workspace-muted); font-size: 13px; }

.market-data-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: baseline;
  gap: 6px 14px;
  padding: 13px 0;
  border-bottom: 1px solid var(--workspace-line);
}

.market-data-row:last-child { border-bottom: 0; }
.market-data-row > div { grid-column: 1 / -1; }
.market-data-row strong, .market-data-row small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.market-data-row strong { font-size: 14px; }
.market-data-row small, .market-data-row p { color: var(--workspace-muted); font-size: 12px; line-height: 1.5; }
.market-data-row small { margin-top: 3px; }
.market-data-row p { grid-column: 1; margin: 0; overflow-wrap: anywhere; }
.market-data-row b { grid-column: 2; justify-self: end; font-size: 12px; font-variant-numeric: tabular-nums; white-space: nowrap; }

.popularity-row { grid-template-columns: 25px minmax(0, 1fr); }
.popularity-row > div, .popularity-row b { grid-column: 2; }
.rank-number { display: grid; place-items: center; width: 24px; height: 24px; color: var(--workspace-muted); background: var(--workspace-raised); border-radius: 50%; font-size: 12px; font-weight: 650; }
.positive { color: var(--theme-red); }
.negative { color: var(--theme-green); }

@media (max-width: 1600px) {
  .signals-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}

@media (max-width: 1200px) {
  .market-dashboard { grid-template-columns: minmax(0, 1fr); grid-template-rows: auto; grid-template-areas: 'overview' 'intelligence' 'industry'; }
  .overview-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .turnover-card { grid-column: 1 / -1; }
  .signals-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 720px) {
  .overview-grid { grid-template-columns: minmax(0, 1fr); }
  .signals-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .sector-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .market-turnover-chart { height: 145px; }
}

@media (max-width: 420px) {
  .signals-grid { grid-template-columns: minmax(0, 1fr); }
}
</style>
