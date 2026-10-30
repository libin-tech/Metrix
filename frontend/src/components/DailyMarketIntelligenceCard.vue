<template>
  <article class="intelligence-card" :aria-label="t('dailyMarket.title')" :aria-busy="pending">
    <header><div><p class="eyebrow">{{ t('dailyMarket.eyebrow') }}</p><h3>{{ t('dailyMarket.title') }}</h3></div><a-tag v-if="report" :color="riskColor(effectiveRisk)">{{ t('dailyMarket.risk') }} · {{ label(effectiveRisk) }}</a-tag></header>
    <a-skeleton v-if="!report && pending" active :paragraph="{ rows: 6 }" />
    <div v-else-if="!report" role="status" class="empty-state"><p>{{ t('dailyMarket.unavailable') }}</p><a-button size="small" @click="load">{{ t('dailyMarket.retry') }}</a-button></div>
    <template v-if="report">
      <p v-if="stale || error" class="quality-warning" role="status">{{ t(stale ? 'dailyMarket.stale' : 'dailyMarket.refreshFailed') }}</p>
      <div class="core-judgment"><strong>{{ label(market.marketBreadth) }}</strong><span>{{ label(market.sentiment) }}</span></div>
      <p class="cycle-line">{{ label(regime.label) }} <small>{{ t('dailyMarket.confidence') }} · {{ label(unreliable ? 'LOW' : regime.confidence) }}</small></p>
      <dl class="metrics-grid">
        <div><dt><router-link to="/market-review">{{ t('dailyMarket.turnover') }}</router-link></dt><dd>{{ compactMoney(market.totalTurnover) }}</dd><small>{{ t('dailyMarket.previous') }} {{ pct(market.turnoverChangePct) }}</small></div>
        <div><dt>{{ t('dailyMarket.upDown') }}</dt><dd>{{ number(market.upCount, 0) }} / {{ number(market.downCount, 0) }}</dd><small>{{ t('dailyMarket.flat') }} {{ number(market.flatCount, 0) }} · {{ t('dailyMarket.ratio') }} {{ number(market.upDownRatio) }}</small></div>
      </dl>
      <div class="indices-strip"><span v-for="index in market.indices" :key="index.symbol">{{ t(`dailyMarket.indices.${index.symbol}`) }} <b :class="tone(index.changePct)">{{ pct(index.changePct) }}</b><small v-if="index.date !== market.date">{{ index.date || t('dailyMarket.missing') }}</small></span></div>
      <div class="portfolio-block"><router-link to="/portfolio">{{ t('dailyMarket.portfolio') }}</router-link><p v-if="!portfolio.holdingCount">{{ t('dailyMarket.noHoldings') }}</p><template v-else><dl class="metrics-grid"><div><dt>{{ t('dailyMarket.marketValue') }}</dt><dd>{{ compactMoney(portfolio.marketValue) }}</dd></div><div><dt>{{ t('dailyMarket.dailyPnl') }}</dt><dd :class="tone(portfolio.dailyPnl)">{{ compactMoney(portfolio.dailyPnl) }}</dd></div></dl><p>{{ t('dailyMarket.dailyReturn') }} {{ pct(portfolio.dailyReturnPct) }} · {{ t('dailyMarket.floatingReturn') }} {{ pct(portfolio.floatingPnlPct) }}</p><small>{{ label(portfolio.relativeStrength) }} · {{ label(portfolio.riskExposure) }}</small></template></div>
      <div class="action-block"><strong>{{ label(unreliable ? 'WAIT_CONFIRMATION' : report.todayAction.label) }}</strong><p>{{ note(unreliable ? 'REASON_WAIT_CONFIRMATION' : report.todayAction.reason) }}</p></div>
      <ul class="highlights"><li v-for="code in report.highlights.slice(0, 3)" :key="code">{{ note(code) }}</li></ul>
      <footer><small>{{ market.date }} · {{ label(market.phase) }}<br>{{ t('dailyMarket.scope') }} · {{ t('dailyMarket.updated') }} {{ timestamp(report.asOf) }}</small><a-button type="link" size="small" @click="expanded = true">{{ t('dailyMarket.details') }}</a-button></footer>
      <small v-if="report.dataQuality.missingFields.length" class="missing-note">{{ t('dailyMarket.partial') }}</small>
    </template>
  </article>
  <a-drawer v-model:open="expanded" :title="t('dailyMarket.title')" width="min(960px, 100vw)" class="intelligence-drawer" @after-open-change="handleDrawerChange">
    <div v-if="report" class="intelligence-details">
      <a-alert v-if="stale || error" type="warning" :message="t('dailyMarket.stale')" show-icon />
      <section><h3>{{ t('dailyMarket.judgment') }}</h3><p>{{ note(regime.summary) }}</p><a-tag :color="riskColor(effectiveRisk)">{{ label(effectiveRisk) }}</a-tag><a-tag>{{ label(regime.label) }}</a-tag><a-tag>{{ t('dailyMarket.confidence') }} {{ label(unreliable ? 'LOW' : regime.confidence) }}</a-tag><h4>{{ t('dailyMarket.evidence') }}</h4><ul><li v-for="code in regime.evidence" :key="code">{{ note(code) }}</li></ul><h4>{{ t('dailyMarket.counter') }}</h4><ul><li v-for="code in regime.counterEvidence" :key="code">{{ note(code) }}</li></ul><h4>{{ t('dailyMarket.triggers') }}</h4><ul><li v-for="code in report.todayAction.triggerConditions" :key="code">{{ note(code) }}</li></ul></section>
      <section><h3>{{ t('dailyMarket.marketDetails') }}</h3><dl class="details-metrics"><div v-for="metric in marketMetrics" :key="metric.key"><dt>{{ t(`dailyMarket.${metric.key}`) }}</dt><dd>{{ metric.value }}</dd></div></dl><h4>{{ t('dailyMarket.indexDetails') }}</h4><div class="table-scroll"><table><thead><tr><th>{{ t('dailyMarket.index') }}</th><th>{{ t('dailyMarket.date') }}</th><th>{{ t('dailyMarket.change') }}</th><th v-for="period in [20, 60, 120]" :key="period">{{ t('dailyMarket.ma', { period }) }}</th><th>{{ t('dailyMarket.structure') }}</th></tr></thead><tbody><tr v-for="index in market.indices" :key="index.symbol"><td>{{ t(`dailyMarket.indices.${index.symbol}`) }}</td><td>{{ index.date || '—' }}</td><td>{{ pct(index.changePct) }}</td><td v-for="period in [20, 60, 120]" :key="period">{{ number(index[`ma${period}`]) }}<small>{{ t('dailyMarket.slope') }} {{ pct(index[`slope${period}`]) }}</small></td><td>{{ label(index.highLowStructure) }}<small>{{ t('dailyMarket.samples', { count: index.historySize }) }}</small></td></tr></tbody></table></div><p class="muted">{{ t('dailyMarket.completedMa') }}</p></section>
      <section><h3>{{ t('dailyMarket.history') }}</h3><p class="muted">{{ t('dailyMarket.historyHint') }}</p><div ref="chartElement" class="history-chart" role="img" :aria-label="t('dailyMarket.history')" /><div class="table-scroll"><table><thead><tr><th>{{ t('dailyMarket.date') }}</th><th>{{ t('dailyMarket.turnover') }}</th><th>{{ t('dailyMarket.upDown') }}</th><th>{{ t('dailyMarket.upPct') }}</th></tr></thead><tbody><tr v-for="point in market.history.slice(-20).reverse()" :key="point.date"><td>{{ point.date }}</td><td>{{ money(point.turnover) }}</td><td>{{ number(point.upCount, 0) }} / {{ number(point.downCount, 0) }}</td><td>{{ pct(point.upPct) }}</td></tr></tbody></table></div></section>
      <section><h3>{{ t('dailyMarket.portfolio') }}</h3><p>{{ t('dailyMarket.notes.SNAPSHOT_NOT_ACCOUNT_RETURN') }}</p><dl class="details-metrics"><div v-for="metric in portfolioMetrics" :key="metric.key"><dt>{{ t(`dailyMarket.${metric.key}`) }}</dt><dd>{{ metric.value }}</dd></div></dl><div class="table-scroll"><table><thead><tr><th>{{ t('dailyMarket.stock') }}</th><th>{{ t('dailyMarket.industry') }}</th><th>{{ t('dailyMarket.quantity') }}</th><th>{{ t('dailyMarket.marketValue') }}</th><th>{{ t('dailyMarket.floatingPnl') }}</th><th>{{ t('dailyMarket.dailyPnl') }}</th><th>{{ t('dailyMarket.weight') }}</th></tr></thead><tbody><tr v-for="(holding, index) in portfolio.holdings" :key="`${holding.stockCode}-${index}`"><td>{{ holding.stockName }}<small>{{ holding.stockCode }}</small></td><td>{{ holding.industry || t('dailyMarket.missing') }}</td><td>{{ number(holding.quantity, 0) }}</td><td>{{ money(holding.marketValue) }}</td><td>{{ money(holding.floatingPnl) }}</td><td>{{ money(holding.dailyPnl) }}</td><td>{{ pct(holding.weightPct) }}</td></tr></tbody></table></div><router-link to="/portfolio">{{ t('dailyMarket.openPortfolio') }}</router-link></section>
      <section><h3>{{ t('dailyMarket.quality') }}</h3><p>{{ t('dailyMarket.source') }} {{ market.source }} · {{ t('dailyMarket.scope') }} · {{ label(market.phase) }}</p><p>{{ t('dailyMarket.updated') }} {{ timestamp(report.asOf) }} · {{ t('dailyMarket.generated') }} {{ timestamp(report.generatedAt) }}</p><p>{{ t(`dailyMarket.narrative.${report.narrativeSource}`) }}</p><p v-if="report.dataQuality.missingFields.length">{{ t('dailyMarket.missing') }}：{{ report.dataQuality.missingFields.map(field => t(`dailyMarket.fields.${field}`)).join(' / ') }}</p><ul><li v-for="code in report.dataQuality.limitations" :key="code">{{ note(code) }}</li></ul><p class="muted">{{ t('dailyMarket.rules') }}</p></section>
      <router-link to="/market-review">{{ t('dailyMarket.openReview') }}</router-link>
    </div>
  </a-drawer>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getDailyMarketIntelligence } from '../api'
import { formatIntelligenceNumber, formatIntelligencePercent, intelligenceNumber, reportIsStale, riskColor } from '../composables/marketIntelligence.js'

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])
const { t, locale } = useI18n()
const report = ref(null)
const pending = ref(true)
const error = ref(false)
const expanded = ref(false)
const drawerReady = ref(false)
const clock = ref(Date.now())
const chartElement = ref(null)
let chart, observer, timer, controller
let disposed = false
const market = computed(() => report.value?.marketSnapshot)
const portfolio = computed(() => report.value?.portfolioSnapshot)
const regime = computed(() => report.value?.marketRegime)
const stale = computed(() => report.value && reportIsStale(report.value, clock.value))
const unreliable = computed(() => stale.value || error.value)
const effectiveRisk = computed(() => unreliable.value ? 'UNKNOWN' : regime.value?.riskLevel)
const label = code => t(`dailyMarket.labels.${code || 'UNKNOWN'}`)
const note = code => t(`dailyMarket.notes.${code}`)
const number = (value, digits = 2) => formatIntelligenceNumber(value, locale.value, digits)
const pct = value => formatIntelligencePercent(value, locale.value)
const money = value => intelligenceNumber(value) === null ? '—' : t('dailyMarket.yuan', { value: number(value) })
const compactMoney = value => intelligenceNumber(value) === null ? '—' : new Intl.NumberFormat(locale.value, { style: 'currency', currency: 'CNY', notation: 'compact', maximumFractionDigits: 2 }).format(Number(value))
const tone = value => ({ positive: intelligenceNumber(value) > 0, negative: intelligenceNumber(value) < 0 })
const timestamp = value => Number.isFinite(Date.parse(value)) ? new Date(value).toLocaleString(locale.value, { timeZone: 'Asia/Shanghai', hour12: false }) : '—'
const marketMetrics = computed(() => {
  const data = market.value || {}
  return [
    { key: 'turnover', value: money(data.totalTurnover) },
    ...['turnoverChangePct', 'turnoverVs5Pct', 'turnoverVs20Pct', 'upPct', 'downPct', 'brokenRate'].map(key => ({ key, value: pct(data[key]) })),
    ...['upPctVs5', 'upPctVs20'].map(key => ({ key, value: number(data[key]) })),
    ...['breadthNet', 'limitUp', 'limitDown', 'streakHeight'].map(key => ({ key, value: number(data[key], 0) })),
    { key: 'volumePattern', value: data.volumePattern === 'INSUFFICIENT' ? t('dailyMarket.missing') : label(data.volumePattern) }, { key: 'sentiment', value: label(data.sentiment) }
  ]
})
const portfolioMetrics = computed(() => {
  const data = portfolio.value || {}
  return [...['totalCost', 'marketValue', 'floatingPnl', 'dailyPnl'].map(key => ({ key, value: money(data[key]) })),
    ...['floatingPnlPct', 'dailyReturnPct', 'stockConcentrationPct', 'industryConcentrationPct'].map(key => ({ key, value: pct(data[key]) })), { key: 'excessReturnPct', value: number(data.excessReturnPct) }]
})
const load = async () => {
  clearTimeout(timer)
  if (disposed || controller) return
  clock.value = Date.now()
  controller = new AbortController()
  try {
    const response = await getDailyMarketIntelligence(controller.signal)
    if (disposed) return
    const data = response.data
    report.value = data.report || report.value
    pending.value = data.status === 'LOADING'
    error.value = data.status === 'ERROR' || data.status === 'STALE'
  } catch (requestError) {
    if (disposed || controller.signal.aborted) return
    error.value = true
    pending.value = false
  } finally {
    controller = null
    if (!disposed) timer = setTimeout(load, pending.value ? 5000 : 30000)
  }
}
const renderChart = async () => {
  await nextTick()
  if (!drawerReady.value || !chartElement.value || !market.value) return
  chart ||= echarts.init(chartElement.value)
  const points = market.value.history.slice(-20)
  chart.setOption({ tooltip: { trigger: 'axis' }, legend: { data: [t('dailyMarket.turnover'), t('dailyMarket.upPct')] }, grid: { left: 75, right: 45, bottom: 32, top: 40 }, xAxis: { type: 'category', data: points.map(point => point.date) }, yAxis: [{ type: 'value', name: t('dailyMarket.yuanUnit') }, { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } }], series: [{ name: t('dailyMarket.turnover'), type: 'line', connectNulls: false, data: points.map(point => point.turnover), itemStyle: { color: '#5c7cc4' } }, { name: t('dailyMarket.upPct'), type: 'line', yAxisIndex: 1, connectNulls: false, data: points.map(point => point.upPct), itemStyle: { color: '#499d8b' } }] })
  chart.resize()
  observer ||= new ResizeObserver(() => chart?.resize())
  observer.observe(chartElement.value)
}
const handleDrawerChange = open => { drawerReady.value = open; if (open) renderChart() }
watch([report, locale, chartElement], renderChart)
onMounted(load)
onBeforeUnmount(() => { disposed = true; clearTimeout(timer); controller?.abort(); observer?.disconnect(); chart?.dispose() })
</script>

<style scoped>
.intelligence-card { min-width: 0; padding: 18px; background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e1e7f0); border-top: 3px solid var(--theme-primary, #597ac3); border-radius: 14px; color: var(--theme-text, #263957); }
header, footer { display: flex; align-items: center; justify-content: space-between; gap: 8px; flex-wrap: wrap; }
h3, p { margin: 0; } h3 { font-size: 17px; } .eyebrow, small, dt, .muted { color: var(--theme-muted, #7c899d); font-size: 11px; } .eyebrow { letter-spacing: .06em; margin-bottom: 4px; }
.core-judgment { display: flex; align-items: baseline; gap: 12px; margin-top: 12px; } .core-judgment strong { font-size: 25px; } .core-judgment span { font-size: 12px; }
.cycle-line { margin: 3px 0 12px; font-size: 13px; } .cycle-line small { margin-left: 5px; }
.metrics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 10px 0; } .metrics-grid div { min-width: 0; } dd { margin: 3px 0; font-size: 15px; font-weight: 600; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
.indices-strip { display: grid; grid-template-columns: 1fr 1fr; gap: 5px 10px; font-size: 10px; padding-bottom: 10px; } .indices-strip b { font-weight: 500; } .indices-strip small { display: block; }
.portfolio-block { border-top: 1px solid var(--theme-line, #e1e7f0); padding-top: 9px; font-size: 11px; } .portfolio-block > a { font-size: 12px; font-weight: 600; } .portfolio-block p { line-height: 1.8; }
.action-block { background: var(--theme-surface-soft, #f4f6fb); padding: 10px; margin: 10px 0; border-radius: 8px; font-size: 12px; } .action-block p { color: var(--theme-muted, #7c899d); margin-top: 4px; font-size: 11px; line-height: 1.6; }
.highlights { padding-left: 15px; font-size: 11px; line-height: 1.7; margin: 10px 0; } .highlights li + li { margin-top: 4px; }
footer { border-top: 1px solid var(--theme-line, #e1e7f0); padding-top: 9px; } .missing-note { display: block; margin-top: 6px; } .quality-warning { color: #9a6927; background: #fff7e6; border-radius: 6px; padding: 7px; font-size: 11px; margin-top: 10px; } .empty-state { padding: 50px 10px; text-align: center; } .empty-state p { margin-bottom: 16px; }
.positive { color: #b95b62; } .negative { color: #27836d; }
.intelligence-details { color: var(--theme-text, #263957); line-height: 1.7; } .intelligence-details section { padding: 18px 0; border-bottom: 1px solid var(--theme-line, #e1e7f0); } .intelligence-details h3 { margin-bottom: 12px; } .intelligence-details p { margin: 10px 0; } .intelligence-details ul { padding-left: 20px; } .intelligence-details small { display: block; } .intelligence-details .muted { font-size: 12px; }
.details-metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; } .table-scroll { overflow-x: auto; max-width: 100%; } table { width: 100%; border-collapse: collapse; font-size: 12px; } td, th { text-align: left; padding: 10px; border-bottom: 1px solid var(--theme-line, #e1e7f0); white-space: nowrap; } th { background: var(--theme-surface-soft, #f4f6fb); } .history-chart { height: 260px; width: 100%; }
@media (max-width: 600px) { .details-metrics { grid-template-columns: 1fr 1fr; } }
</style>
