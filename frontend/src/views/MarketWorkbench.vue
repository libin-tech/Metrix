<template>
  <main class="market-workbench">
    <header class="workbench-header">
      <div>
        <p class="eyebrow">{{ $t('marketWorkbench.eyebrow') }}</p>
        <h1>{{ $t('marketWorkbench.title') }}</h1>
      </div>
      <a-auto-complete
        v-model:value="query"
        class="instrument-search"
        :options="stockOptions"
        :placeholder="$t('marketWorkbench.searchPlaceholder')"
        @search="searchStocksDebounced"
        @select="loadOverview"
      >
        <a-input-search :loading="loading" :aria-label="$t('marketWorkbench.searchPlaceholder')" @search="loadOverview(query)" />
      </a-auto-complete>
    </header>

    <section class="holding-strip" :aria-label="$t('marketWorkbench.holdings')">
      <div v-if="displayedHoldings.length" class="holding-list">
        <button v-for="holding in displayedHoldings" :key="holding.stockCode" type="button" class="holding-chip" @click="loadOverview(holding.stockCode)">
          <span>{{ holding.stockName }}</span>
          <small>{{ holding.stockCode }}</small>
          <strong :class="changeClass(holding.profitLossPercent)">{{ signedPercent(holding.profitLossPercent) }}</strong>
        </button>
      </div>
      <span v-else class="holding-empty">{{ $t('marketWorkbench.noHoldings') }}</span>
    </section>

    <section v-if="error" class="workbench-error" role="alert"><ExclamationCircleOutlined /><div><strong>{{ $t('marketWorkbench.loadFailed') }}</strong><span>{{ error }}</span></div></section>
    <section v-else-if="loading && !overview" class="workbench-loading"><a-skeleton active :paragraph="{rows: 9}" /></section>
    <section v-else-if="overview" class="dashboard">
      <section class="quote-overview" :aria-label="$t('marketWorkbench.quoteOverview')">
        <div class="instrument-identity">
          <p>{{ overview.instrument.name }}</p><span>{{ overview.instrument.thscode }}</span>
          <small>{{ overview.dataTime || $t('marketWorkbench.timeUnavailable') }}</small>
        </div>
        <div class="latest-price"><span>{{ $t('marketWorkbench.latestPrice') }}</span><strong>{{ formatPrice(overview.quote.lastPrice) }}</strong><em :class="changeClass(overview.quote.changePct)">{{ signedPercent(overview.quote.changePct) }}</em></div>
        <div class="metric-item"><span>{{ $t('marketWorkbench.yearReturn') }}</span><strong :class="changeClass(overview.metrics.yearReturn)">{{ signedPercent(overview.metrics.yearReturn) }}</strong></div>
        <div class="metric-item drawdown-metrics">
          <span :title="$t('marketWorkbench.drawdownHint')">{{ $t('marketWorkbench.maxDrawdown') }}</span>
          <div class="drawdown-grid">
            <div v-for="window in windows" :key="window.key" class="drawdown-period">
              <span>{{ $t(`marketWorkbench.drawdown${window.key}`) }}</span>
              <strong :class="{negative: overview.metrics[`maxDrawdown${window.key}`] < 0}">{{ formatPercent(overview.metrics[`maxDrawdown${window.key}`]) }}</strong>
            </div>
          </div>
        </div>
        <div class="metric-item"><span>{{ $t('marketWorkbench.averageAmount') }}</span><strong>{{ formatAmount(overview.metrics.averageAmount20) }}</strong></div>
      </section>

      <section class="market-panels">
        <article class="chart-card">
          <div class="card-heading"><div><p>{{ $t('marketWorkbench.priceTrend') }}</p><small>{{ $t('marketWorkbench.chartHint') }}</small></div><div class="range-tabs" role="group" :aria-label="$t('marketWorkbench.range')"><button v-for="window in windows" :key="window.key" type="button" :class="{active: selectedWindow === window.key}" @click="selectWindow(window)">{{ window.label }}</button></div></div>
          <div
            ref="chartRoot"
            class="kline-chart"
            tabindex="0"
            role="application"
            :aria-label="$t('marketWorkbench.chartKeyboardHint')"
            @pointerdown="startPan"
            @pointermove="moveChart"
            @pointerup="endPan"
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
        <article class="structure-card"><div class="card-heading"><div><p>{{ $t('marketWorkbench.tradeStructure') }}</p><small>{{ $t('marketWorkbench.tradeStructureHint') }}</small></div></div><dl><div><dt>{{ $t('marketWorkbench.open') }}</dt><dd>{{ formatPrice(overview.quote.open) }}</dd></div><div><dt>{{ $t('marketWorkbench.high') }}</dt><dd>{{ formatPrice(overview.quote.high) }}</dd></div><div><dt>{{ $t('marketWorkbench.low') }}</dt><dd>{{ formatPrice(overview.quote.low) }}</dd></div><div><dt>{{ $t('marketWorkbench.prevClose') }}</dt><dd>{{ formatPrice(overview.quote.prevClose) }}</dd></div><div><dt>{{ $t('marketWorkbench.volume') }}</dt><dd>{{ formatVolume(overview.quote.volume) }}</dd></div><div><dt>{{ $t('marketWorkbench.amount') }}</dt><dd>{{ formatAmount(overview.quote.amount) }}</dd></div><div><dt>{{ $t('marketWorkbench.amplitude') }}</dt><dd>{{ formatPercent(overview.quote.amplitude) }}</dd></div><div><dt>{{ $t('marketWorkbench.changeAmount') }}</dt><dd :class="changeClass(overview.quote.changeAmount)">{{ signedNumber(overview.quote.changeAmount) }}</dd></div></dl></article>
      </section>

      <section class="data-card"><div class="card-heading"><div><p>{{ $t('marketWorkbench.recentData') }}</p><small>{{ $t('marketWorkbench.recentDataHint') }}</small></div><span>{{ $t('marketWorkbench.adjustment') }} · {{ overview.adjustment }}</span></div><div class="data-table-wrap"><table><thead><tr><th>{{ $t('marketWorkbench.date') }}</th><th>{{ $t('marketWorkbench.open') }}</th><th>{{ $t('marketWorkbench.high') }}</th><th>{{ $t('marketWorkbench.low') }}</th><th>{{ $t('marketWorkbench.close') }}</th><th>{{ $t('marketWorkbench.change') }}</th><th>{{ $t('marketWorkbench.amount') }}</th></tr></thead><tbody><tr v-for="bar in recentBars" :key="bar.timestamp"><td>{{ bar.date }}</td><td>{{ formatPrice(bar.open) }}</td><td>{{ formatPrice(bar.high) }}</td><td>{{ formatPrice(bar.low) }}</td><td>{{ formatPrice(bar.close) }}</td><td :class="changeClass(bar.changePct)">{{ signedPercent(bar.changePct) }}</td><td>{{ formatAmount(bar.amount) }}</td></tr></tbody></table></div></section>
      <footer class="data-disclaimer"><span>{{ $t('marketWorkbench.code') }} {{ overview.instrument.thscode }}</span><span>{{ $t('marketWorkbench.source') }} {{ overview.source }}</span><span>{{ $t('marketWorkbench.adjustment') }} {{ overview.adjustment }}</span><span>{{ $t('marketWorkbench.disclaimer') }}</span></footer>
    </section>
    <section v-else class="workbench-empty"><FundOutlined /><h2>{{ $t('marketWorkbench.emptyTitle') }}</h2><p>{{ $t('marketWorkbench.emptyDescription') }}</p></section>
  </main>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {ExclamationCircleOutlined, FundOutlined} from '@ant-design/icons-vue'
import {getMarketWorkbenchHoldings, getMarketWorkbenchOverview, searchStocks} from '../api'

const {t} = useI18n()
const query = ref('')
const overview = ref(null)
const holdingSnapshot = ref([])
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

const selectWindow = window => { selectedWindow.value = window.key; rangeStart.value = Math.max(0, bars.value.length - window.count); activeIndex.value = null }
const searchStocksDebounced = value => { clearTimeout(searchTimer); if (!value?.trim()) { stockOptions.value = []; return } searchTimer = setTimeout(async () => { try { const response = await searchStocks(value); stockOptions.value = (response.data || []).map(stock => ({value: stock.stockCode, label: `${stock.stockName} · ${stock.stockCode}`})) } catch { stockOptions.value = [] } }, 180) }
const loadOverview = async value => { let thscode = String(value || query.value).trim().toUpperCase(); if (!thscode) return; loading.value = true; error.value = ''; try { if (!/^[0-9]{6}\.(SH|SZ|BJ)$/.test(thscode)) { const response = await searchStocks(thscode); const candidates = response.data || []; const match = candidates.find(stock => stock.stockName === thscode) || candidates[0]; if (!match) throw new Error(t('marketWorkbench.notFound')); thscode = match.stockCode } query.value = thscode; const response = await getMarketWorkbenchOverview(thscode); overview.value = response.data; selectWindow(windows.value.at(-1)); } catch (requestError) { overview.value = null; error.value = requestError.message || t('marketWorkbench.loadFailed') } finally { loading.value = false } }
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
onMounted(async () => { try { const response = await getMarketWorkbenchHoldings(); holdingSnapshot.value = (response.data || []).slice(0, 8) } catch { holdingSnapshot.value = [] } })
</script>

<style scoped>
.market-workbench { width: 100%; max-width: 1440px; margin: 0 auto; padding: 28px 32px 44px; color: var(--theme-text, #20304a); }.workbench-header { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin-bottom: 18px; }.eyebrow { margin: 0 0 6px; color: var(--theme-muted, #7b8aa0); font-size: 11px; font-weight: 700; letter-spacing: .12em; }.workbench-header h1 { margin: 0; font-size: 25px; letter-spacing: -.04em; }.instrument-search { width: min(420px, 100%); }.instrument-search :deep(.ant-input-affix-wrapper), .instrument-search :deep(.ant-input-search-button) { height: 40px; border-radius: 8px; }.workbench-top { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(300px, .9fr); gap: 16px; margin-bottom: 16px; }.holding-strip { display: flex; align-items: center; gap: 13px; min-height: 118px; padding: 11px 14px; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e1e7f0); border-radius: 12px; }.holding-strip-title { display: grid; gap: 1px; min-width: 68px; color: var(--theme-text, #53647d); font-size: 12px; }.holding-strip-title > :first-child { color: var(--theme-blue, #637fc0); font-size: 17px; }.holding-strip-title small { color: var(--theme-muted, #9aa6b7); font-size: 10px; }.holding-list { display: flex; min-width: 0; gap: 7px; overflow-x: auto; }.holding-chip { display: grid; grid-template-columns: auto auto; align-items: center; gap: 1px 8px; min-width: 118px; padding: 8px 10px; color: var(--theme-text, #32435e); text-align: left; background: var(--theme-raised, #f8fafc); border: 1px solid var(--theme-line, #eef2f6); border-radius: 7px; cursor: pointer; }.holding-chip span { overflow: hidden; font-size: 12px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.holding-chip small { color: var(--theme-muted, #9aa6b7); font-size: 10px; }.holding-chip strong { grid-column: 2; grid-row: 1 / 3; justify-self: end; font-size: 11px; }.holding-empty { color: var(--theme-muted, #8996a8); font-size: 12px; }.anomaly-card { min-width: 0; padding: 12px 14px; overflow: hidden; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e1e7f0); border-radius: 12px; }.anomaly-heading { display: flex; align-items: start; justify-content: space-between; gap: 8px; padding-bottom: 7px; border-bottom: 1px solid var(--theme-line, #edf0f5); }.anomaly-heading p { margin: 0; color: var(--theme-text, #2a3d59); font-size: 12px; font-weight: 700; }.anomaly-heading small, .anomaly-heading > span { display: block; margin-top: 2px; color: var(--theme-muted, #8996a9); font-size: 10px; }.anomaly-heading > span { margin-top: 0; color: var(--theme-blue, #6883af); }.anomaly-list { display: grid; max-height: 88px; overflow-y: auto; }.anomaly-item { display: grid; grid-template-columns: minmax(78px, auto) minmax(0, 1fr); align-items: center; gap: 2px 9px; width: 100%; padding: 7px 0; color: var(--theme-text, #50617a); text-align: left; background: transparent; border: 0; border-bottom: 1px solid var(--theme-line, #f0f3f7); cursor: pointer; }.anomaly-item:last-child { border-bottom: 0; }.anomaly-item span { display: grid; gap: 1px; }.anomaly-item strong { color: var(--theme-text, #31435f); font-size: 11px; }.anomaly-item small, .anomaly-item em { overflow: hidden; color: var(--theme-muted, #96a2b3); font-size: 9px; font-style: normal; text-overflow: ellipsis; white-space: nowrap; }.anomaly-item p { overflow: hidden; margin: 0; color: var(--theme-muted, #62738a); font-size: 10px; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }.anomaly-item em { grid-column: 2; color: var(--theme-muted, #6c85ad); }.anomaly-state { display: flex; align-items: center; gap: 6px; min-height: 70px; color: var(--theme-muted, #96a2b3); font-size: 11px; }.anomaly-error { color: var(--theme-red, #b56b72); line-height: 1.45; }.workbench-error { display: flex; gap: 12px; max-width: 780px; margin: 80px auto; padding: 18px; color: var(--theme-red, #a3414b); background: var(--theme-surface, #fff8f8); border: 1px solid var(--theme-line, #f0dfe1); border-radius: 10px; }.workbench-error span { display: block; margin-top: 4px; color: var(--theme-muted, #7d6870); font-size: 13px; }.workbench-loading { padding: 30px; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e1e7f0); border-radius: 12px; }.dashboard { display: grid; gap: 16px; }.quote-overview { display: grid; grid-template-columns: minmax(190px, 1.35fr) repeat(2, minmax(0, 1fr)) minmax(220px, 1.6fr) minmax(0, 1fr); gap: 0; overflow: hidden; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e1e7f0); border-radius: 12px; }.instrument-identity, .latest-price, .metric-item { display: grid; align-content: center; min-height: 108px; padding: 19px; border-right: 1px solid var(--theme-line, #edf0f5); }.instrument-identity p { margin: 0; color: var(--theme-text, #263957); font-size: 17px; font-weight: 700; }.instrument-identity span { margin-top: 4px; color: var(--theme-muted, #76869e); font-size: 12px; }.instrument-identity small { margin-top: 8px; color: var(--theme-muted, #a0abba); font-size: 10px; }.latest-price span, .metric-item span { color: var(--theme-muted, #7f8da1); font-size: 11px; }.latest-price strong { margin: 5px 0 1px; color: var(--theme-text, #263957); font-size: 29px; letter-spacing: -.04em; font-variant-numeric: tabular-nums; }.latest-price em { font-size: 12px; font-style: normal; font-weight: 600; }.metric-item strong { margin-top: 8px; color: var(--theme-text, #263957); font-size: 18px; letter-spacing: -.025em; font-variant-numeric: tabular-nums; }.positive { color: var(--theme-red, #c64652) !important; }.negative { color: var(--theme-green, #087043) !important; }.market-panels { display: grid; grid-template-columns: minmax(0, 1.9fr) minmax(270px, .7fr); gap: 16px; }.chart-card, .structure-card, .data-card { min-width: 0; padding: 18px; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e1e7f0); border-radius: 12px; }.card-heading { display: flex; align-items: start; justify-content: space-between; gap: 12px; padding-bottom: 13px; border-bottom: 1px solid var(--theme-line, #edf0f5); }.card-heading p { margin: 0; color: var(--theme-text, #2a3d59); font-size: 14px; font-weight: 700; }.card-heading small, .card-heading > span { display: block; margin-top: 3px; color: var(--theme-muted, #8996a9); font-size: 11px; }.range-tabs { display: flex; gap: 3px; padding: 3px; background: var(--theme-raised, #f3f6fa); border-radius: 7px; }.range-tabs button { padding: 4px 7px; color: var(--theme-muted, #7a899f); font-size: 10px; background: transparent; border: 0; border-radius: 5px; cursor: pointer; }.range-tabs button.active { color: var(--theme-blue, #3565a8); font-weight: 700; background: var(--theme-surface, #fff); box-shadow: 0 1px 3px #26395718; }.kline-chart { position: relative; height: 400px; margin-top: 12px; overflow: hidden; touch-action: none; outline: none; user-select: none; }.kline-chart:focus-visible { outline: 2px solid var(--theme-line, #6d93ca); outline-offset: 3px; }.kline-chart svg { width: 100%; height: 100%; }.chart-grid line { stroke: var(--theme-text, #edf1f6); stroke-width: 1; }.candle line { stroke-width: 1.5; }.candle rect { opacity: .88; }.candle.up, .volume-bars .up { fill: var(--theme-red, #d55761); stroke: var(--theme-red, #d55761); }.candle.down, .volume-bars .down { fill: var(--theme-green, #189464); stroke: var(--theme-green, #189464); }.volume-bars rect { opacity: .45; }.ma-line polyline { fill: none; stroke-width: 1.8; vector-effect: non-scaling-stroke; }.ma5 polyline { stroke: var(--theme-blue, #4f83c7); }.ma20 polyline { stroke: var(--theme-amber, #e59546); }.ma30 polyline { stroke: var(--theme-purple, #8b68bb); }.ma60 polyline { stroke: var(--theme-green, #32856e); }.crosshair line { stroke: var(--theme-muted, #7d8fa8); stroke-width: 1; stroke-dasharray: 3 4; opacity: .75; }.chart-tooltip { position: absolute; top: 12px; z-index: 1; display: grid; gap: 3px; min-width: 112px; padding: 8px; color: var(--theme-text, #586980); font-size: 10px; pointer-events: none; background: var(--theme-surface, #fffffff2); border: 1px solid var(--theme-line, #dce4ef); border-radius: 6px; box-shadow: 0 4px 12px #26395712; transform: translateX(-50%); }.chart-tooltip strong { color: var(--theme-text, #2c3d57); }.chart-legend { position: absolute; bottom: 4px; left: 2px; display: flex; flex-wrap: wrap; gap: 9px; font-size: 10px; }.chart-legend span { font-variant-numeric: tabular-nums; }.chart-legend .ma5 { color: var(--theme-blue, #4f83c7); }.chart-legend .ma20 { color: var(--theme-amber, #e59546); }.chart-legend .ma30 { color: var(--theme-purple, #8b68bb); }.chart-legend .ma60 { color: var(--theme-green, #32856e); }.structure-card dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1px; margin: 13px 0 0; }.structure-card dl div { padding: 12px 9px; background: var(--theme-raised, #f8fafc); }.structure-card dt { color: var(--theme-muted, #8593a7); font-size: 10px; }.structure-card dd { margin: 6px 0 0; color: var(--theme-text, #32445f); font-size: 14px; font-weight: 600; font-variant-numeric: tabular-nums; }.data-table-wrap { overflow-x: auto; margin-top: 3px; }.data-card table { width: 100%; min-width: 760px; border-collapse: collapse; }.data-card th { padding: 11px 9px; color: var(--theme-muted, #7e8ca1); font-size: 11px; font-weight: 600; text-align: right; background: var(--theme-raised, #f7f9fc); }.data-card th:first-child, .data-card td:first-child { text-align: left; }.data-card td { padding: 10px 9px; color: var(--theme-text, #42536c); font-size: 12px; text-align: right; font-variant-numeric: tabular-nums; border-bottom: 1px solid var(--theme-line, #edf0f5); }.data-card tbody tr:last-child td { border-bottom: 0; }.data-disclaimer { display: flex; flex-wrap: wrap; gap: 10px 18px; padding: 1px 2px; color: var(--theme-muted, #8996a8); font-size: 10px; }.data-disclaimer span:last-child { color: var(--theme-muted, #a0767b); }.workbench-empty { display: grid; place-items: center; min-height: 420px; color: var(--theme-muted, #92a0b3); text-align: center; }.workbench-empty :deep(svg) { color: var(--theme-muted, #91a8d2); font-size: 31px; }.workbench-empty h2 { margin: 12px 0 5px; color: var(--theme-text, #53657e); font-size: 17px; }.workbench-empty p { font-size: 12px; }
.workbench-header { align-items: center; }.instrument-search :deep(.ant-input-group-wrapper), .instrument-search :deep(.ant-input-group), .instrument-search :deep(.ant-input-affix-wrapper), .instrument-search :deep(.ant-input-search-button) { height: 40px; }.instrument-search :deep(.ant-input-affix-wrapper) { display: flex; align-items: center; border-radius: 8px 0 0 8px; }.instrument-search :deep(.ant-input-search-button) { display: inline-flex; align-items: center; justify-content: center; border-radius: 0 8px 8px 0; }.holding-strip { gap: 0; }.holding-strip-title { display: none; }
@media (max-width: 980px) { .workbench-top { grid-template-columns: 1fr; }.quote-overview { grid-template-columns: repeat(3, minmax(0, 1fr)); }.instrument-identity { grid-column: span 2; }.market-panels { grid-template-columns: 1fr; }.structure-card { display: none; } }
@media (max-width: 680px) { .market-workbench { padding: 20px 14px 34px; }.workbench-header { align-items: stretch; flex-direction: column; gap: 16px; }.instrument-search { width: 100%; }.holding-strip { align-items: start; flex-direction: column; }.holding-list { width: 100%; }.quote-overview { grid-template-columns: repeat(2, minmax(0, 1fr)); }.instrument-identity { grid-column: span 2; }.metric-item, .latest-price { min-height: 88px; padding: 14px; }.instrument-identity { min-height: 82px; }.latest-price strong { font-size: 25px; }.kline-chart { height: 340px; }.range-tabs button { padding: 4px 5px; }.card-heading { align-items: start; flex-direction: column; }.chart-tooltip { display: none; } }
.drawdown-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 16px; margin-top: 12px; }
.drawdown-period { display: grid; min-width: 0; }
.drawdown-period strong { margin-top: 4px; font-size: 16px; }
@media (max-width: 680px) { .drawdown-metrics { grid-column: span 2; } }
</style>
