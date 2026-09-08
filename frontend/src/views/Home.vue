<template>
  <main class="research-desk">
    <section class="market-pulse" :aria-label="$t('homeDesk.marketPulse')">
      <div class="pulse-heading"><div><p class="eyebrow">{{ $t('homeDesk.marketPulse') }}</p><h2>{{ $t('homeDesk.marketPulseTitle') }}</h2></div><div class="pulse-actions"><span role="status" class="connection-status" :class="marketDashboardConnectionState">{{ $t(`homeDesk.connection${marketDashboardConnectionState}`) }}</span><a-button type="link" @click="goToMarketReview">{{ $t('homeDesk.readReview') }} <ArrowRightOutlined /></a-button></div></div>
      <div class="dashboard-grid overview-grid">
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
      <div class="dashboard-grid signals-grid">
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
      <div class="dashboard-grid industry-grid">
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
    </section>
  </main>
</template>

<script setup>
import {useTheme} from '../composables/useTheme'
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {Empty} from 'ant-design-vue'
import {ArrowRightOutlined} from '@ant-design/icons-vue'
import {init, use} from 'echarts/core'
import {BarChart} from 'echarts/charts'
import {GridComponent, TooltipComponent} from 'echarts/components'
import {CanvasRenderer} from 'echarts/renderers'
import {getLatestTurnoverItem, hasMarketTurnoverHistory, unwrapMarketTurnover} from '../composables/marketData'

use([BarChart, CanvasRenderer, GridComponent, TooltipComponent])

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
  if (!marketTurnoverChart) marketTurnoverChart = init(marketTurnoverChartRef.value)
  const history = marketTurnoverHistory.value
  marketTurnoverChart.setOption({
    animation: false,
    grid: {top: 8, right: 2, bottom: 18, left: 2},
    tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}, backgroundColor: '#182336', borderWidth: 0, padding: [8, 10], textStyle: {color: '#ffffff', fontSize: 12}, formatter: params => {
      const item = history[params[0]?.dataIndex]
      return item ? `${formatTurnoverDate(item.date)}<br/>${t('homeDesk.marketTurnover')}：${formatTurnoverDetail(item.amount)}` : ''
    }},
    xAxis: {type: 'category', data: history.map(item => formatTurnoverDate(item.date).slice(5)), axisLine: {lineStyle: {color: isDark.value ? '#34445b' : '#dce2eb'}}, axisTick: {show: false}, axisLabel: {color: isDark.value ? '#a3b0c4' : '#8996aa', fontSize: 10, interval: Math.max(0, Math.ceil(history.length / 5) - 1)}},
    yAxis: {type: 'value', show: false},
    series: [{type: 'bar', data: history.map(item => Number(item.amount) || 0), barMaxWidth: 12, itemStyle: {color: '#5d8fd8', borderRadius: [3, 3, 0, 0]}, emphasis: {itemStyle: {color: '#316bc0'}}}]
  }, {notMerge: true, lazyUpdate: true, silent: true})
}
const resizeMarketTurnoverChart = () => marketTurnoverChart?.resize()
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
.research-desk { width: 100%; max-width: none; padding: 28px 32px 52px; color: var(--theme-text, #182336); }
.pulse-heading h2 { margin: 0; letter-spacing: -.045em; }
.market-pulse { margin-top: 8px; }.pulse-heading { margin-bottom: 15px; }.pulse-heading h2 { font-size: 22px; }.pulse-heading :deep(.ant-btn-link) { padding-right: 0; }.pulse-card { padding: 18px; }.card-heading { align-items: start; padding-bottom: 12px; border-bottom: 1px solid var(--theme-line, #edf0f5); }.card-heading p { margin: 0; color: var(--theme-text, #263957); font-size: 15px; font-weight: 700; }.card-heading small { display: block; margin-top: 3px; color: var(--theme-muted, #8996aa); font-size: 11px; line-height: 1.35; }.card-heading > span, .card-heading > strong { color: var(--theme-muted, #71809a); font-size: 11px; font-variant-numeric: tabular-nums; white-space: nowrap; }.card-heading > strong { color: var(--theme-text, #263957); font-size: 17px; }
.breadth-values { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin: 12px 0 14px; }
.breadth-metric { display: grid; gap: 10px; min-width: 0; padding: 12px 10px; border: 1px solid var(--breadth-border); border-radius: 9px; background: var(--breadth-tint); color: var(--breadth-color); }
.breadth-positive { --breadth-color: var(--theme-red, #c74452); --breadth-tint: var(--theme-raised, #fff7f7); --breadth-border: var(--theme-line, #f5e4e6); }
.breadth-negative { --breadth-color: var(--theme-green, #087043); --breadth-tint: var(--theme-raised, #f2faf6); --breadth-border: var(--theme-line, #dfeee6); }
.breadth-metric span { display: flex; align-items: center; gap: 5px; color: var(--theme-muted, #68788f); font-size: 11px; line-height: 1.35; }
.breadth-metric span::before { content: ''; width: 5px; height: 5px; flex-shrink: 0; border-radius: 50%; background: var(--breadth-color); }
.breadth-metric strong { justify-self: end; font-size: clamp(20px, 1.8vw, 28px); line-height: 1; letter-spacing: -.025em; font-variant-numeric: tabular-nums; }
.breadth-limit { gap: 7px; padding-top: 9px; padding-bottom: 9px; }
.breadth-limit strong { font-size: 20px; }
.market-breadth-bar { display: flex; height: 8px; overflow: hidden; background: var(--theme-raised, #e9edf3); border-radius: 999px; }
.market-breadth-bar i { display: block; min-width: 0; transition: width .2s ease; }
.market-breadth-bar .breadth-up { background: #d7505d; }
.market-breadth-bar .breadth-down { background: #159263; }
.breadth-cache-note { display: block; margin-top: 9px; color: var(--theme-muted, #8996aa); font-size: 11px; line-height: 1.5; }
@media (prefers-reduced-motion: reduce) { .market-breadth-bar i { transition: none; } }

 .quick-index-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; padding-top: 12px; }
.index-quote { --quote-color: var(--theme-muted, #68788f); --quote-tint: var(--theme-raised, #f5f7fa); --quote-border: var(--theme-line, #e9edf3); display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 7px 4px; padding: 10px; border: 1px solid var(--quote-border); border-radius: 9px; background: var(--quote-tint); }
.index-quote-up { --quote-color: var(--theme-red, #c74452); --quote-tint: var(--theme-raised, #fff7f7); --quote-border: var(--theme-line, #f5e4e6); }
.index-quote-down { --quote-color: var(--theme-green, #087043); --quote-tint: var(--theme-raised, #f2faf6); --quote-border: var(--theme-line, #dfeee6); }
.index-quote-name { grid-column: 1 / -1; overflow: hidden; color: var(--theme-text, #526178); font-size: 12px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.index-quote-value { grid-column: 1 / -1; color: var(--theme-text, #263957); font-size: clamp(18px, 1.5vw, 23px); font-weight: 700; line-height: 1.1; letter-spacing: -.025em; font-variant-numeric: tabular-nums; }
.index-quote-change { display: inline-flex; align-items: center; gap: 4px; color: var(--quote-color); font-size: 12px; font-style: normal; font-weight: 600; line-height: 1; font-variant-numeric: tabular-nums; }
.index-quote-direction { width: 6px; height: 2px; background: currentColor; }
.index-quote-up .index-quote-direction, .index-quote-down .index-quote-direction { width: 0; height: 0; border-right: 3px solid transparent; border-left: 3px solid transparent; background: transparent; }
.index-quote-up .index-quote-direction { border-bottom: 5px solid currentColor; }
.index-quote-down .index-quote-direction { border-top: 5px solid currentColor; }
.market-turnover-chart { width: 100%; height: 162px; margin-top: 8px; }
.sector-card { align-self: start; }
.sector-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(110px, 1fr)); gap: 6px; max-height: 240px; overflow-y: auto; margin-top: 10px; padding: 2px; scrollbar-width: thin; scrollbar-color: #ccd5e2 transparent; overscroll-behavior-y: contain; }
.sector-grid:focus-visible { outline: 2px solid var(--theme-line, #8aa9d4); outline-offset: 3px; border-radius: 7px; }
.sector-tile { --sector-color: var(--theme-muted, #68788f); --sector-tint: var(--theme-raised, #f5f7fa); --sector-border: var(--theme-line, #e9edf3); display: grid; gap: 6px; min-width: 0; min-height: 52px; align-content: center; padding: 8px; border: 1px solid var(--sector-border); border-radius: 7px; color: var(--sector-color); background: var(--sector-tint); transition: border-color .15s ease, box-shadow .15s ease; }
.sector-tile strong { overflow: hidden; color: var(--theme-text, #44536b); font-size: 11px; font-weight: 500; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }
.sector-change { display: flex; align-items: center; justify-content: space-between; gap: 4px; font-size: 12px; font-weight: 700; line-height: 1.2; font-variant-numeric: tabular-nums; }
.sector-change::before { content: ''; width: 12px; height: 3px; border-radius: 999px; background: currentColor; opacity: .55; }
.sector-up { --sector-color: var(--theme-red, #c74452); --sector-tint: var(--theme-raised, #fff7f7); --sector-border: var(--theme-line, #f5e4e6); }
.sector-down { --sector-color: var(--theme-green, #087043); --sector-tint: var(--theme-raised, #f2faf6); --sector-border: var(--theme-line, #dfeee6); }
.sector-sort-label { padding: 3px 6px; border-radius: 4px; background: var(--theme-raised, #f4f6fa); }
@media (hover: hover) { .sector-tile:hover { border-color: var(--sector-color); box-shadow: 0 2px 5px #2639570a; } }
@media (prefers-reduced-motion: reduce) { .sector-tile { transition: none; } }
.positive { color: var(--theme-red, #d7505d) !important; }.negative { color: var(--theme-green, #006d2c) !important; }.sector-card :deep(.ant-empty) { padding: 30px 0 8px; }.sector-card :deep(.ant-empty-description) { color: var(--theme-muted, #8996aa); font-size: 12px; }
@media (max-width: 720px) { .research-desk { padding: 18px 16px 40px; }.pulse-heading { align-items: start; flex-direction: column; }.sector-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }.market-turnover-chart { height: 145px; } }.market-data-card { min-width: 0; min-height: 300px; padding: 18px; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e1e7f0); border-radius: 14px; box-shadow: 0 6px 18px rgba(34, 51, 79, .035); }.market-data-card :deep(.ant-pagination) { display: flex; justify-content: end; margin: 13px 0 0; }.market-data-card :deep(.ant-empty) { padding-top: 58px; }.market-data-card :deep(.ant-empty-description) { color: var(--theme-muted, #8996aa); font-size: 12px; }.market-data-list { margin-top: 1px; }.market-data-row { display: grid; grid-template-columns: minmax(90px, .85fr) minmax(0, 1.25fr) auto; align-items: center; gap: 10px; padding: 11px 0; border-bottom: 1px solid var(--theme-line, #edf0f5); }.market-data-row:last-child { border-bottom: 0; }.market-data-row > div { min-width: 0; }.market-data-row strong { display: block; overflow: hidden; color: var(--theme-text, #263957); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }.market-data-row small, .market-data-row p { display: block; overflow: hidden; margin: 3px 0 0; color: var(--theme-muted, #8996aa); font-size: 11px; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }.market-data-row p { margin: 0; }.market-data-row b { font-size: 12px; font-variant-numeric: tabular-nums; white-space: nowrap; }.rank-number { display: grid; place-items: center; width: 23px; height: 23px; color: var(--theme-muted, #5d6e86); font-size: 11px; font-weight: 700; background: var(--theme-raised, #edf2f8); border-radius: 50%; }.popularity-row { grid-template-columns: 25px minmax(0, 1fr) auto; }
@media (max-width: 720px) {.market-data-row { grid-template-columns: minmax(84px, .8fr) minmax(0, 1.2fr) auto; gap: 8px; } }

.pulse-heading, .card-heading { display: flex; align-items: start; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 7px; color: var(--theme-muted, #72809a); font-size: 11px; font-weight: 800; letter-spacing: .13em; text-transform: uppercase; }
.dashboard-grid { display: grid; gap: 16px; margin-bottom: 16px; }
.overview-grid { grid-template-columns: minmax(0, 5fr) minmax(0, 3fr) minmax(0, 2fr); }
.signals-grid { grid-template-columns: repeat(7, minmax(0, 1fr)); }
.industry-grid { grid-template-columns: minmax(0, 1fr); }
.pulse-card { min-width: 0; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e1e7f0); border-radius: 14px; }
.signals-grid > article { padding: 12px; }
.signals-grid .card-heading { flex-wrap: wrap; gap: 4px; }
.signals-grid .market-data-row { grid-template-columns: minmax(0, 1fr) auto; gap: 4px 8px; padding: 9px 0; }
.signals-grid .market-data-row > div { grid-column: 1 / -1; }
.signals-grid .market-data-row > b { justify-self: end; }
.signals-grid .popularity-row { grid-template-columns: 23px minmax(0, 1fr); }
.signals-grid .popularity-row > div { grid-column: 2; }
.signals-grid .popularity-row > b { grid-column: 2; }
@media (max-width: 1200px) {
  .signals-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .overview-grid .quick-index-list { gap: 6px; }
  .index-quote { padding: 8px; }
}
@media (max-width: 720px) {
  .overview-grid { grid-template-columns: 1fr; }
  .signals-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .overview-grid .quick-index-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 420px) {
  .signals-grid { grid-template-columns: 1fr; }
}
.turnover-summary { text-align: right; flex-shrink: 0; }
.turnover-summary strong { color: var(--theme-text, #263957); font-size: 17px; font-variant-numeric: tabular-nums; }
.turnover-summary small { margin-top: 4px; font-size: 11px; white-space: nowrap; }
.pulse-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.connection-status { display: inline-flex; align-items: center; gap: 6px; color: var(--theme-muted, #8996aa); font-size: 12px; }
.connection-status::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: #b98b2b; }
.connection-status.Connected::before { background: #159263; }
</style>
