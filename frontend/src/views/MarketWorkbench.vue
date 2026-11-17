<template>
  <main class="market-workbench">
    <header class="workbench-header">
      <div>
        <h1 class="workspace-title">{{ $t('marketWorkbench.title') }}</h1>
        <p>{{ $t('marketWorkbench.eyebrow') }}</p>
      </div>
      <a-auto-complete
        v-model:value="query"
        class="instrument-search"
        :options="stockOptions"
        :placeholder="$t('marketWorkbench.searchPlaceholder')"
        @search="searchStocksDebounced"
        @select="loadOverview"
      >
        <a-input-search size="large" :loading="loading" :aria-label="$t('marketWorkbench.searchPlaceholder')" @search="loadOverview(query)" />
      </a-auto-complete>
    </header>

    <section class="account-summary" aria-labelledby="account-summary-heading">
      <h2 id="account-summary-heading">{{ $t('marketWorkbench.accountSummary') }}</h2>
      <dl class="account-summary-metrics">
        <div v-for="metric in accountMetrics" :key="metric.key" class="account-summary-metric">
          <dt>{{ $t(metric.label) }}</dt>
          <dd :class="metric.profitLoss ? changeClass(accountSummary?.[metric.key]) : null">
            {{ formatAccountAmount(accountSummary?.[metric.key]) }}
          </dd>
        </div>
      </dl>
      <p v-if="accountSummaryError" class="account-summary-error" role="status">
        {{ $t('marketWorkbench.accountSummaryLoadFailed') }}
      </p>
    </section>

    <section v-if="recentSearches.length" class="recent-searches" :aria-label="$t('marketWorkbench.recentSearches')">
      <span>{{ $t('marketWorkbench.recentSearches') }}</span>
      <button v-for="stock in recentSearches" :key="stock.thscode" type="button" @click="loadOverview(stock.thscode)">{{ stock.name }} · {{ stock.thscode }}</button>
    </section>

    <section v-if="overview" class="quote-overview" :aria-label="$t('marketWorkbench.quoteOverview')">
      <div class="instrument-identity">
        <h2>{{ overview.instrument.name }}</h2><span>{{ overview.instrument.thscode }}</span>
        <small>{{ overview.dataTime || $t('marketWorkbench.timeUnavailable') }}</small>
      </div>
      <div class="latest-price"><span>{{ $t('marketWorkbench.latestPrice') }}</span><strong>{{ formatPrice(overview.quote.lastPrice) }}</strong><em :class="changeClass(overview.quote.changePct)">{{ signedPercent(overview.quote.changePct) }}</em></div>
      <div class="metric-item"><span>{{ $t('marketWorkbench.yearReturn') }}</span><strong :class="changeClass(overview.metrics.yearReturn)">{{ signedPercent(overview.metrics.yearReturn) }}</strong></div>
      <div class="metric-item"><span>{{ $t('marketWorkbench.averageAmount') }}</span><strong>{{ formatAmount(overview.metrics.averageAmount20) }}</strong></div>
      <div class="metric-item drawdown-metrics">
        <span :title="$t('marketWorkbench.drawdownHint')">{{ $t('marketWorkbench.maxDrawdown') }}</span>
        <div class="drawdown-grid">
          <div v-for="window in windows" :key="window.key" class="drawdown-period">
            <span>{{ $t(`marketWorkbench.drawdown${window.key}`) }}</span>
            <strong :class="{negative: overview.metrics[`maxDrawdown${window.key}`] < 0}">{{ formatPercent(overview.metrics[`maxDrawdown${window.key}`]) }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="workbench-layout">
      <section class="workbench-primary">
        <section v-if="error" class="workbench-error" role="alert"><ExclamationCircleOutlined /><div><strong>{{ $t('marketWorkbench.loadFailed') }}</strong><span>{{ error }}</span></div></section>
        <section v-else-if="loading && !overview" class="workbench-loading"><a-skeleton active :paragraph="{rows: 9}" /></section>
        <section v-else-if="!overview" class="workbench-empty"><FundOutlined /><h2>{{ $t('marketWorkbench.emptyTitle') }}</h2><p>{{ $t('marketWorkbench.emptyDescription') }}</p></section>
        <article v-else class="chart-card">
          <div class="card-heading"><div><h2>{{ $t(chartMode === 'intraday' ? 'marketWorkbench.intraday' : 'marketWorkbench.priceTrend') }}</h2><small>{{ $t(chartMode === 'intraday' ? 'marketWorkbench.intradayHint' : 'marketWorkbench.chartHint') }}</small></div><div class="range-tabs" role="group" :aria-label="$t('marketWorkbench.range')"><button type="button" :aria-pressed="chartMode === 'intraday'" @click="chartMode = 'intraday'">{{ $t('marketWorkbench.intraday') }}</button><button v-for="window in windows" :key="window.key" type="button" :aria-pressed="chartMode === 'daily' && selectedWindow === window.key" @click="selectWindow(window)">{{ window.label }}</button></div></div>
          <IntradayChart v-if="chartMode === 'intraday'" :thscode="overview.instrument.thscode" />
          <div
            v-else
            ref="chartRoot"
            class="kline-chart"
            tabindex="0"
            role="application"
            :aria-label="$t('marketWorkbench.chartKeyboardHint')"
            @pointerdown="startPan"
            @pointermove="moveChart"
            @pointerup="endPan"
            @pointercancel="endPan"
            @pointerleave="endPan"
            @wheel.prevent="zoomChart"
            @keydown="handleChartKey"
          >
            <svg v-if="chartModel.candles.length" viewBox="0 0 1000 500" preserveAspectRatio="none" aria-hidden="true">
              <g class="chart-grid"><line v-for="line in chartModel.grid" :key="line" x1="52" :y1="line" x2="986" :y2="line" /></g>
              <g v-for="line in chartModel.maLines" :key="line.key" :class="`ma-line ${line.key}`"><polyline :points="line.points" /></g>
              <g v-for="candle in chartModel.candles" :key="candle.timestamp" :class="candle.up ? 'candle up' : 'candle down'"><line :x1="candle.x" :x2="candle.x" :y1="candle.highY" :y2="candle.lowY" /><rect :x="candle.x - candle.width / 2" :y="candle.bodyY" :width="candle.width" :height="candle.bodyHeight" /></g>
              <g class="volume-bars"><rect v-for="candle in chartModel.candles" :key="`volume-${candle.timestamp}`" :class="candle.up ? 'up' : 'down'" :x="candle.x - candle.width / 2" :y="candle.volumeY" :width="candle.width" :height="candle.volumeHeight" /></g>
              <g v-if="activeCandle" class="crosshair"><line :x1="activeCandle.x" :x2="activeCandle.x" y1="24" y2="470" /><line x1="52" :y1="activeCandle.closeY" x2="986" :y2="activeCandle.closeY" /></g>
            </svg>
            <div v-if="activeCandle" class="chart-tooltip" :style="tooltipStyle"><strong>{{ activeCandle.date }}</strong><span>{{ $t('marketWorkbench.open') }} {{ formatPrice(activeCandle.open) }}</span><span>{{ $t('marketWorkbench.high') }} {{ formatPrice(activeCandle.high) }}</span><span>{{ $t('marketWorkbench.low') }} {{ formatPrice(activeCandle.low) }}</span><span>{{ $t('marketWorkbench.close') }} {{ formatPrice(activeCandle.close) }}</span><span>{{ $t('marketWorkbench.amount') }} {{ formatAmount(activeCandle.amount) }}</span></div>
            <div class="chart-legend"><span class="ma5">MA5 {{ formatPrice(overview.metrics.ma5) }}</span><span class="ma20">MA20 {{ formatPrice(overview.metrics.ma20) }}</span><span class="ma30">MA30 {{ formatPrice(overview.metrics.ma30) }}</span><span class="ma60">MA60 {{ formatPrice(overview.metrics.ma60) }}</span></div>
          </div>
        </article>
      </section>
      <aside class="anomaly-column">
        <section class="holding-strip" aria-labelledby="holdings-heading">
          <h2 id="holdings-heading">{{ $t('marketWorkbench.holdings') }}</h2>
          <div v-if="displayedHoldings.length" class="holding-list">
            <button v-for="holding in displayedHoldings" :key="holding.stockCode" type="button" class="holding-chip" :aria-pressed="overview?.instrument.thscode === holding.stockCode" @click="loadOverview(holding.stockCode)">
              <span>{{ holding.stockName }}</span>
              <small>{{ holding.stockCode }}</small>
              <strong :class="changeClass(holding.profitLossPercent)">{{ signedPercent(holding.profitLossPercent) }}</strong>
            </button>
          </div>
          <span v-else class="holding-empty">{{ $t('marketWorkbench.noHoldings') }}</span>
        </section>
        <article v-if="overview" class="structure-card"><div class="card-heading"><div><h2>{{ $t('marketWorkbench.tradeStructure') }}</h2><small>{{ $t('marketWorkbench.tradeStructureHint') }}</small></div></div><dl><div><dt>{{ $t('marketWorkbench.open') }}</dt><dd>{{ formatPrice(overview.quote.open) }}</dd></div><div><dt>{{ $t('marketWorkbench.high') }}</dt><dd>{{ formatPrice(overview.quote.high) }}</dd></div><div><dt>{{ $t('marketWorkbench.low') }}</dt><dd>{{ formatPrice(overview.quote.low) }}</dd></div><div><dt>{{ $t('marketWorkbench.prevClose') }}</dt><dd>{{ formatPrice(overview.quote.prevClose) }}</dd></div><div><dt>{{ $t('marketWorkbench.volume') }}</dt><dd>{{ formatVolume(overview.quote.volume) }}</dd></div><div><dt>{{ $t('marketWorkbench.amount') }}</dt><dd>{{ formatAmount(overview.quote.amount) }}</dd></div><div><dt>{{ $t('marketWorkbench.amplitude') }}</dt><dd>{{ formatPercent(overview.quote.amplitude) }}</dd></div><div><dt>{{ $t('marketWorkbench.changeAmount') }}</dt><dd :class="changeClass(overview.quote.changeAmount)">{{ signedNumber(overview.quote.changeAmount) }}</dd></div></dl></article>
        <section class="anomaly-card" aria-labelledby="anomaly-heading">
          <div class="anomaly-heading"><div><h2 id="anomaly-heading">{{ $t('marketWorkbench.anomalyTitle') }}</h2><small>{{ anomalyDataTime || $t('marketWorkbench.anomalyHint') }}</small></div></div>
          <div v-if="anomalyLoading" class="anomaly-state">{{ $t('marketWorkbench.anomalyLoading') }}</div>
          <div v-else-if="anomalyError" class="anomaly-state anomaly-error">{{ anomalyError }}</div>
          <div v-else-if="anomalies.length" class="anomaly-list">
            <button v-for="anomaly in anomalies" :key="`${anomaly.thscode}-${anomaly.tag_name}-${anomaly.analysis_content}`" type="button" class="anomaly-item" @click="loadOverview(anomaly.thscode)">
              <span><strong>{{ anomaly.stock_name || anomaly.thscode }}</strong><small>{{ anomaly.thscode }}</small></span>
              <p>{{ anomaly.analysis_content || anomaly.tag_name }}</p>
              <em v-if="anomalyKeywords(anomaly)">{{ anomalyKeywords(anomaly) }}</em>
            </button>
          </div>
          <div v-else class="anomaly-state">{{ $t('marketWorkbench.noAnomalies') }}</div>
        </section>
      </aside>
    </section>

    <template v-if="overview">
      <section class="data-card" aria-labelledby="recent-data-heading"><div class="card-heading"><div><h2 id="recent-data-heading">{{ $t('marketWorkbench.recentData') }}</h2><small>{{ $t('marketWorkbench.recentDataHint') }}</small></div><span>{{ $t('marketWorkbench.adjustment') }} · {{ overview.adjustment }}</span></div><div class="data-table-wrap" tabindex="0" role="region" aria-labelledby="recent-data-heading"><table><thead><tr><th>{{ $t('marketWorkbench.date') }}</th><th>{{ $t('marketWorkbench.open') }}</th><th>{{ $t('marketWorkbench.high') }}</th><th>{{ $t('marketWorkbench.low') }}</th><th>{{ $t('marketWorkbench.close') }}</th><th>{{ $t('marketWorkbench.change') }}</th><th>{{ $t('marketWorkbench.amount') }}</th></tr></thead><tbody><tr v-for="bar in recentBars" :key="bar.timestamp"><td>{{ bar.date }}</td><td>{{ formatPrice(bar.open) }}</td><td>{{ formatPrice(bar.high) }}</td><td>{{ formatPrice(bar.low) }}</td><td>{{ formatPrice(bar.close) }}</td><td :class="changeClass(bar.changePct)">{{ signedPercent(bar.changePct) }}</td><td>{{ formatAmount(bar.amount) }}</td></tr></tbody></table></div></section>
      <footer class="data-disclaimer"><span>{{ $t('marketWorkbench.code') }} {{ overview.instrument.thscode }}</span><span>{{ $t('marketWorkbench.source') }} {{ overview.source }}</span><span>{{ $t('marketWorkbench.adjustment') }} {{ overview.adjustment }}</span><span>{{ $t('marketWorkbench.disclaimer') }}</span></footer>
    </template>
  </main>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {ExclamationCircleOutlined, FundOutlined} from '@ant-design/icons-vue'
import {getMarketWorkbenchAccountSummary, getMarketWorkbenchAnomalies, getMarketWorkbenchHoldings, getMarketWorkbenchOverview, searchStocks} from '../api'
import {accountMetrics, useAccountAmountFormatter} from '../composables/usePortfolioAccountSummary'

import IntradayChart from '../components/IntradayChart.vue'

const {t} = useI18n()
const formatAccountAmount = useAccountAmountFormatter()
const RECENT_SEARCHES_STORAGE_KEY = 'metrix:market-workbench:recent-searches'
const MAX_RECENT_SEARCHES = 8
const chartMode = ref('daily')
const query = ref('')
const overview = ref(null)
const holdingSnapshot = ref([])
const accountSummary = ref(null)
const accountSummaryError = ref(false)
const recentSearches = ref(readRecentSearches())
const anomalies = ref([])
const anomalyDataTime = ref('')
const anomalyError = ref('')
const anomalyLoading = ref(false)
const error = ref('')
const loading = ref(false)
const stockOptions = ref([])
const chartRoot = ref(null)
const selectedWindow = ref('1Y')
const rangeStart = ref(0)
const activeIndex = ref(null)
const pan = ref(null)
let searchTimer
const windows = computed(() => [{key: '1M', label: t('marketWorkbench.oneMonth'), count: 21}, {key: '3M', label: t('marketWorkbench.threeMonths'), count: 63}, {key: '6M', label: t('marketWorkbench.sixMonths'), count: 126}, {key: '1Y', label: t('marketWorkbench.oneYear'), count: 252}])
const bars = computed(() => overview.value?.bars || [])
const displayedHoldings = computed(() => (overview.value?.holdings || holdingSnapshot.value).slice(0, 8))
const rangeCount = computed(() => windows.value.find(window => window.key === selectedWindow.value)?.count || 252)
const visibleBars = computed(() => bars.value.slice(rangeStart.value, Math.min(bars.value.length, rangeStart.value + rangeCount.value)))
const recentBars = computed(() => visibleBars.value.slice(-60).reverse().map((bar, index, rows) => ({...bar, changePct: percentChange(rows[index + 1]?.close, bar.close)})))

const selectWindow = (window, mode = 'daily') => { chartMode.value = mode; selectedWindow.value = window.key; rangeStart.value = Math.max(0, bars.value.length - window.count); activeIndex.value = null }
const searchStocksDebounced = value => { clearTimeout(searchTimer); if (!value?.trim()) { stockOptions.value = []; return } searchTimer = setTimeout(async () => { try { const response = await searchStocks(value); stockOptions.value = (response.data || []).map(stock => ({value: stock.stockCode, label: `${stock.stockName} · ${stock.stockCode}`})) } catch { stockOptions.value = [] } }, 180) }
const loadOverview = async value => { let thscode = String(value || query.value).trim().toUpperCase(); if (!thscode) return; loading.value = true; error.value = ''; try { if (!/^[0-9]{6}\.(SH|SZ|BJ)$/.test(thscode)) { const response = await searchStocks(thscode); const candidates = response.data || []; const match = candidates.find(stock => stock.stockName === thscode) || candidates[0]; if (!match) throw new Error(t('marketWorkbench.notFound')); thscode = match.stockCode } query.value = thscode; const response = await getMarketWorkbenchOverview(thscode); overview.value = response.data; saveRecentSearch(response.data.instrument); selectWindow(windows.value.at(-1), chartMode.value); } catch (requestError) { overview.value = null; error.value = requestError.message || t('marketWorkbench.loadFailed') } finally { loading.value = false } }
const anomalyKeywords = anomaly => Array.isArray(anomaly.keyword_list) ? anomaly.keyword_list.join(' · ') : ''
const loadAnomalies = async () => { anomalyLoading.value = true; anomalyError.value = ''; try { const response = await getMarketWorkbenchAnomalies(); anomalies.value = response.data?.items || []; anomalyDataTime.value = response.data?.dataTime || '' } catch (requestError) { anomalies.value = []; anomalyError.value = requestError.message || t('marketWorkbench.anomalyLoadFailed') } finally { anomalyLoading.value = false } }
const formatPrice = value => value == null ? '—' : Number(value).toFixed(2)
const formatPercent = value => value == null ? '—' : `${Number(value).toFixed(2)}%`
const signedPercent = value => value == null ? '—' : `${Number(value) >= 0 ? '+' : ''}${formatPercent(value)}`
const signedNumber = value => value == null ? '—' : `${Number(value) >= 0 ? '+' : ''}${Number(value).toFixed(2)}`
const formatAmount = value => value == null ? '—' : `${(Number(value) / 100000000).toFixed(2)}${t('marketWorkbench.hundredMillion')}`
const formatVolume = value => value == null ? '—' : `${(Number(value) / 10000).toFixed(2)}${t('marketWorkbench.tenThousandShares')}`
const changeClass = value => value == null || Number(value) === 0 ? '' : Number(value) > 0 ? 'positive' : 'negative'
const percentChange = (start, end) => start == null || end == null || Number(start) === 0 ? null : (Number(end) / Number(start) - 1) * 100
const movingAverage = (rows, index, period) => index < period - 1 ? null : rows.slice(index - period + 1, index + 1).reduce((sum, item) => sum + Number(item.close), 0) / period
const chartModel = computed(() => {
  const rows = visibleBars.value.map((bar, index) => ({...bar, sourceIndex: rangeStart.value + index})).filter(bar => [bar.open, bar.high, bar.low, bar.close].every(value => value != null)); if (!rows.length) return {candles: [], maLines: [], grid: []}
  const lows = rows.map(row => Number(row.low)); const highs = rows.map(row => Number(row.high)); const min = Math.min(...lows); const max = Math.max(...highs); const span = max - min || Math.max(max * .02, 1); const y = price => 25 + (max - Number(price)) / span * 325; const volumeMax = Math.max(...rows.map(row => Number(row.volume) || 0), 1); const step = 934 / rows.length; const width = Math.max(1.4, Math.min(8, step * .62)); const candles = rows.map((bar, index) => { const openY = y(bar.open); const closeY = y(bar.close); const volumeHeight = (Number(bar.volume) || 0) / volumeMax * 82; return {...bar, x: 52 + step * (index + .5), width, highY: y(bar.high), lowY: y(bar.low), closeY, bodyY: Math.min(openY, closeY), bodyHeight: Math.max(1.4, Math.abs(closeY - openY)), up: Number(bar.close) >= Number(bar.open), volumeY: 470 - volumeHeight, volumeHeight} }); const maLines = [{key:'ma5', period:5}, {key:'ma20', period:20}, {key:'ma30', period:30}, {key:'ma60', period:60}].map(line => ({...line, points: rows.map((bar, index) => { const ma = movingAverage(bars.value, bar.sourceIndex, line.period); return ma == null ? null : `${52 + step * (index + .5)},${y(ma)}` }).filter(Boolean).join(' ')})); return {candles, maLines, grid: [25, 106, 187, 268, 350, 470]} })
const activeCandle = computed(() => activeIndex.value == null ? null : chartModel.value.candles[activeIndex.value] || null)
const tooltipStyle = computed(() => !activeCandle.value ? {} : {left: `${Math.min(78, Math.max(4, (activeCandle.value.x / 1000) * 100))}%`})
const clampStart = next => Math.max(0, Math.min(Math.max(0, bars.value.length - rangeCount.value), next))
const startPan = event => { event.currentTarget.setPointerCapture?.(event.pointerId); pan.value = {x: event.clientX, start: rangeStart.value}; activeIndex.value = chartIndex(event) }
const moveChart = event => { activeIndex.value = chartIndex(event); if (!pan.value || !chartRoot.value) return; const moved = Math.round((pan.value.x - event.clientX) / chartRoot.value.getBoundingClientRect().width * rangeCount.value); rangeStart.value = clampStart(pan.value.start + moved) }
const endPan = () => { pan.value = null }
const chartIndex = event => { if (!chartRoot.value || !chartModel.value.candles.length) return null; const rect = chartRoot.value.getBoundingClientRect(); return Math.max(0, Math.min(chartModel.value.candles.length - 1, Math.floor((event.clientX - rect.left) / rect.width * chartModel.value.candles.length))) }
const zoomChart = event => { const candidates = windows.value.map(window => window.count); const current = candidates.indexOf(rangeCount.value); const next = Math.max(0, Math.min(candidates.length - 1, current + (event.deltaY > 0 ? 1 : -1))); selectWindow(windows.value[next]) }
const handleChartKey = event => { if (event.key === 'ArrowLeft') rangeStart.value = clampStart(rangeStart.value - 5); else if (event.key === 'ArrowRight') rangeStart.value = clampStart(rangeStart.value + 5); else if (event.key === '+' || event.key === '=') zoomChart({deltaY: -1}); else if (event.key === '-') zoomChart({deltaY: 1}); else return; event.preventDefault() }
onBeforeUnmount(() => clearTimeout(searchTimer))
const loadHoldings = async () => {
  try {
    const response = await getMarketWorkbenchHoldings()
    holdingSnapshot.value = (response.data || []).slice(0, 8)
  } catch {
    holdingSnapshot.value = []
  }
}
const loadAccountSummary = async () => {
  try {
    const response = await getMarketWorkbenchAccountSummary()
    accountSummary.value = response.data
  } catch {
    accountSummaryError.value = true
  }
}
onMounted(() => {
  loadHoldings()
  loadAccountSummary()
  loadAnomalies()
})

function readRecentSearches() { try { const items = JSON.parse(localStorage.getItem(RECENT_SEARCHES_STORAGE_KEY) || '[]'); return Array.isArray(items) ? items.filter(item => item?.thscode && item?.name).slice(0, MAX_RECENT_SEARCHES) : [] } catch { return [] } }
function saveRecentSearch(instrument) { if (!instrument?.thscode || !instrument?.name) return; const next = [{thscode: instrument.thscode, name: instrument.name}, ...recentSearches.value.filter(item => item.thscode !== instrument.thscode)].slice(0, MAX_RECENT_SEARCHES); recentSearches.value = next; try { localStorage.setItem(RECENT_SEARCHES_STORAGE_KEY, JSON.stringify(next)) } catch { /* 浏览器禁用本地存储时仅保留当前会话内容 */ } }
</script>

<style scoped>
.market-workbench {
  display: grid;
  gap: clamp(14px, 2vw, 22px);
  color: var(--workspace-ink);
}

.workbench-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 14px 24px;
}

.workbench-header p { margin: 5px 0 0; color: var(--workspace-muted); font-size: 13px; }
.instrument-search { width: min(100%, 460px); }
.instrument-search :deep(.ant-input-group > .ant-input),
.instrument-search :deep(.ant-input-affix-wrapper),
.instrument-search :deep(.ant-input-search-button) { height: 40px; }
.instrument-search :deep(.ant-input-search-button) { display: inline-flex; align-items: center; justify-content: center; }

.account-summary h2, .holding-strip h2, .card-heading h2, .anomaly-heading h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 670;
  line-height: 1.35;
}

.account-summary h2 { margin-bottom: 8px; }
.account-summary-metrics, .quote-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 150px), 1fr));
  gap: 10px;
  margin: 0;
}

.account-summary-metric, .quote-overview > div, .workbench-primary > *, .anomaly-column > *, .data-card {
  min-width: 0;
  background: var(--workspace-surface);
  border: 1px solid var(--workspace-line);
  border-radius: var(--workspace-radius);
  box-shadow: var(--workspace-shadow);
}

.account-summary-metric { padding: 12px 14px; }
.account-summary-metric dt, .metric-item > span, .latest-price > span { color: var(--workspace-muted); font-size: 12px; }
.account-summary-metric dd { margin: 5px 0 0; font-size: clamp(1rem, 1.5vw, 1.2rem); font-weight: 700; overflow-wrap: anywhere; }
.account-summary-metric dd, .quote-overview strong, .holding-chip strong, .structure-card dd, .data-card table { font-variant-numeric: tabular-nums; }
.account-summary-error { margin: 8px 0 0; color: var(--theme-red); font-size: 12px; }

.recent-searches { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; font-size: 12px; color: var(--workspace-muted); }
.recent-searches button {
  padding: 4px 8px;
  color: var(--workspace-accent);
  background: var(--workspace-soft-accent);
  border: 1px solid transparent;
  border-radius: var(--workspace-control-radius);
  cursor: pointer;
  font: inherit;
}

.recent-searches button:hover { border-color: var(--workspace-accent); }

.quote-overview { grid-template-columns: minmax(145px, 1.15fr) repeat(3, minmax(105px, .8fr)) minmax(240px, 1.55fr); }
.quote-overview > div { display: grid; align-content: center; gap: 5px; padding: 13px 15px; }
.quote-overview .latest-price { background: var(--workspace-soft-accent); }
.instrument-identity h2 { margin: 0; font-size: 19px; overflow-wrap: anywhere; }
.instrument-identity span, .instrument-identity small { color: var(--workspace-muted); font-size: 12px; overflow-wrap: anywhere; }
.quote-overview strong { font-size: 19px; line-height: 1.2; letter-spacing: -.025em; overflow-wrap: anywhere; }
.quote-overview .latest-price strong { font-size: clamp(1.4rem, 2.4vw, 1.85rem); }
.latest-price em { font-size: 13px; font-weight: 680; font-style: normal; }
.positive { color: var(--theme-red); }
.negative { color: var(--theme-green); }
.drawdown-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 7px 10px; }
.drawdown-period { display: grid; gap: 3px; min-width: 0; }
.drawdown-period span { color: var(--workspace-muted); font-size: 11px; }
.drawdown-period strong { font-size: 13px; }

.workbench-layout { display: grid; grid-template-columns: minmax(0, 2fr) minmax(300px, .8fr); gap: 16px; align-items: start; }
.workbench-primary, .anomaly-column { display: grid; min-width: 0; gap: 16px; }
.chart-card, .structure-card, .data-card, .holding-strip, .anomaly-card, .workbench-loading { padding: clamp(13px, 2vw, 21px); }
.card-heading, .anomaly-heading { display: flex; align-items: start; justify-content: space-between; flex-wrap: wrap; gap: 9px 12px; padding-bottom: 12px; border-bottom: 1px solid var(--workspace-line); }
.card-heading small, .card-heading > span, .anomaly-heading small { display: block; margin-top: 4px; color: var(--workspace-muted); font-size: 12px; }
.holding-empty, .anomaly-state { display: block; padding-block: 12px 0; color: var(--workspace-muted); font-size: 13px; }
.holding-strip h2 { padding-bottom: 9px; border-bottom: 1px solid var(--workspace-line); }
.holding-chip, .anomaly-item {
  width: 100%;
  padding: 9px 6px;
  color: var(--workspace-ink);
  background: transparent;
  border: 0;
  cursor: pointer;
  text-align: left;
  font: inherit;
}

.holding-chip + .holding-chip, .anomaly-item + .anomaly-item { border-top: 1px solid var(--workspace-line); }
.holding-chip:hover, .holding-chip[aria-pressed='true'], .anomaly-item:hover {
  background: var(--workspace-soft-accent);
  border-radius: var(--workspace-control-radius);
}

.holding-chip { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 3px 8px; }
.holding-chip span { font-size: 13px; font-weight: 630; overflow-wrap: anywhere; }
.holding-chip small { grid-column: 1; color: var(--workspace-muted); font-size: 11px; }
.holding-chip strong { grid-column: 2; grid-row: 1 / 3; align-self: center; font-size: 13px; }
.anomaly-list { max-height: none; overflow-y: visible; }
.anomaly-item { display: grid; gap: 5px; font-size: 13px; overflow-wrap: anywhere; }
.anomaly-item span { display: flex; align-items: baseline; flex-wrap: wrap; gap: 3px 8px; }
.anomaly-item small, .anomaly-item em { color: var(--workspace-muted); font-size: 11px; font-style: normal; }
.anomaly-item p { overflow: visible; text-overflow: clip; white-space: normal; }
.anomaly-item p { margin: 0; color: var(--workspace-muted); line-height: 1.55; }
.anomaly-item em { color: var(--workspace-accent); }
.anomaly-error { color: var(--theme-red); }
.workbench-error { display: flex; align-items: start; gap: 10px; padding: 18px; color: var(--theme-red); }
.workbench-error span { display: block; margin-top: 4px; color: var(--workspace-muted); overflow-wrap: anywhere; }

.range-tabs { display: flex; flex-wrap: wrap; gap: 3px; padding: 4px; background: var(--workspace-raised); border-radius: var(--workspace-control-radius); }
.range-tabs button { min-height: 30px; padding: 4px 8px; color: var(--workspace-muted); font: inherit; font-size: 12px; white-space: nowrap; background: transparent; border: 0; border-radius: 6px; cursor: pointer; }
.range-tabs button:hover, .range-tabs button[aria-pressed='true'] { color: var(--workspace-accent); background: var(--workspace-soft-accent); }
.range-tabs button[aria-pressed='true'] { font-weight: 680; }

.kline-chart { position: relative; height: clamp(263px, 33vw, 415px); margin-top: 14px; overflow: hidden; touch-action: pan-y; user-select: none; }
.kline-chart svg { display: block; width: 100%; height: 100%; }
.chart-grid line { stroke: var(--workspace-line); stroke-width: 1; vector-effect: non-scaling-stroke; }
.candle.up, .volume-bars .up { fill: var(--theme-red); stroke: var(--theme-red); }
.candle.down, .volume-bars .down { fill: var(--theme-green); stroke: var(--theme-green); }
.volume-bars rect { opacity: .38; }
.ma-line polyline { fill: none; stroke: currentColor; stroke-width: 1.65; vector-effect: non-scaling-stroke; }
.ma5 { color: var(--theme-blue); }
.ma20 { color: var(--theme-amber); }
.ma30 { color: var(--theme-purple); }
.ma60 { color: var(--theme-green); }
.crosshair line { stroke: var(--workspace-muted); stroke-width: 1; stroke-dasharray: 3 4; opacity: .75; }
.chart-tooltip { position: absolute; top: 12px; z-index: 1; display: grid; gap: 3px; min-width: 112px; padding: 8px; color: var(--workspace-ink); font-size: 11px; pointer-events: none; background: var(--workspace-surface); border: 1px solid var(--workspace-line); border-radius: 7px; box-shadow: var(--workspace-shadow); transform: translateX(-50%); }
.chart-legend { position: absolute; inset: auto 0 4px; display: flex; flex-wrap: wrap; gap: 4px 10px; font-size: 11px; font-variant-numeric: tabular-nums; }

.structure-card dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 7px; margin: 10px 0 0; }
.structure-card dl div { min-width: 0; padding: 7px 9px; border-radius: 7px; background: var(--workspace-raised); }
.structure-card dt { color: var(--workspace-muted); font-size: 11px; }
.structure-card dd { margin: 3px 0 0; font-size: 13px; font-weight: 640; overflow-wrap: anywhere; }

.data-table-wrap { max-height: 460px; margin-top: 8px; overflow: auto; }
.data-card table { width: 100%; min-width: 625px; border-spacing: 0; font-size: 13px; }
.data-card th, .data-card td { padding: 10px 12px; border-bottom: 1px solid var(--workspace-line); text-align: right; white-space: nowrap; }
.data-card th { position: sticky; top: 0; color: var(--workspace-muted); background: var(--workspace-raised); font-size: 12px; }
.data-card :is(th, td):first-child { text-align: left; }
.data-card tbody tr:hover { background: var(--workspace-raised); }
.data-disclaimer { display: flex; flex-wrap: wrap; gap: 5px 16px; color: var(--workspace-muted); font-size: 12px; }

.workbench-empty { display: grid; justify-items: center; align-content: center; gap: 8px; min-height: 300px; padding: 22px; text-align: center; }
.workbench-empty :deep(.anticon) { display: grid; place-items: center; width: 46px; height: 46px; color: var(--workspace-accent); background: var(--workspace-soft-accent); border-radius: 11px; font-size: 23px; }
.workbench-empty h2 { margin: 9px 0 0; font-size: 19px; }
.workbench-empty p { margin: 0; color: var(--workspace-muted); font-size: 13px; }

@media (max-width: 1000px) {
  .quote-overview { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .drawdown-metrics { grid-column: span 2; }
}

@media (max-width: 810px) {
  .workbench-layout { grid-template-columns: minmax(0, 1fr); }
  .anomaly-column { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .anomaly-card { grid-column: 1 / -1; }
}

@media (max-width: 540px) {
  .instrument-search { width: 100%; }
  .account-summary-metrics, .quote-overview { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .account-summary-metric, .quote-overview > div { padding: 10px 11px; }
  .anomaly-column { grid-template-columns: minmax(0, 1fr); }
  .chart-tooltip { display: none; }
}
</style>
