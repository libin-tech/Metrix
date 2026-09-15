<template>
  <section class="intraday-panel" :aria-label="t('marketWorkbench.intraday')" :aria-busy="loading">
    <div class="intraday-meta">
      <span>{{ snapshot?.date }} · {{ t('marketWorkbench.intradaySource') }}</span>
      <a-button size="small" :loading="loading" @click="loadData">{{ t('marketWorkbench.refresh') }}</a-button>
    </div>
    <p v-if="error" class="intraday-state" role="alert">{{ t('marketWorkbench.intradayFailed') }}</p>
    <p v-else-if="loading && !snapshot" class="intraday-state">{{ t('marketWorkbench.intradayLoading') }}</p>
    <p v-else-if="!snapshot?.points?.length" class="intraday-state">{{ t('marketWorkbench.intradayEmpty') }}</p>
    <div v-show="snapshot?.points?.length && !error" ref="chartRoot" class="intraday-chart" />
    <small>{{ t('marketWorkbench.intradayHint') }}<template v-if="snapshot?.dataTime"> · {{ t('marketWorkbench.updatedAt') }} {{ snapshot.dataTime }}</template></small>
  </section>
</template>

<script setup>
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import * as echarts from 'echarts/core'
import {LineChart, BarChart} from 'echarts/charts'
import {GridComponent, TooltipComponent, AxisPointerComponent} from 'echarts/components'
import {CanvasRenderer} from 'echarts/renderers'
import {getMarketWorkbenchIntraday} from '../api'
import {buildIntradaySeries} from '../composables/intradayChart'

echarts.use([LineChart, BarChart, GridComponent, TooltipComponent, AxisPointerComponent, CanvasRenderer])

const props = defineProps({thscode: {type: String, default: ''}})
const {t, locale} = useI18n()
const chartRoot = ref(null)
const snapshot = ref(null)
const loading = ref(false)
const error = ref(false)
let chart
let observer
let timer
let requestId = 0

const renderChart = async () => {
  await nextTick()
  if (!chartRoot.value || !snapshot.value?.points?.length || error.value) return
  chart ||= echarts.init(chartRoot.value)
  const rows = buildIntradaySeries(snapshot.value.points)
  const styles = getComputedStyle(chartRoot.value)
  const color = (key, fallback) => styles.getPropertyValue(key).trim() || fallback
  const text = color('--theme-muted', '#7b8aa0')
  const line = color('--theme-line', '#e1e7f0')
  const axis = gridIndex => ({type: 'category', gridIndex, data: rows.map(row => row.time), boundaryGap: true,
    axisLabel: {color: text, interval: index => [0, 60, 120, 121, 181, 241].includes(index), hideOverlap: true},
    axisLine: {lineStyle: {color: line}}, axisTick: {show: false}})
  chart.setOption({
    animation: false,
    tooltip: {trigger: 'axis', axisPointer: {type: 'cross'}, confine: true},
    axisPointer: {link: [{xAxisIndex: 'all'}]},
    grid: [{left: 68, right: 20, top: 30, height: '54%'}, {left: 68, right: 20, top: '73%', bottom: 28}],
    xAxis: [axis(0), axis(1)],
    yAxis: [
      {type: 'value', scale: true, name: t('marketWorkbench.latestPrice'), axisLabel: {color: text}, nameTextStyle: {color: text}, splitLine: {lineStyle: {color: line}}},
      {type: 'value', gridIndex: 1, name: t('marketWorkbench.minuteVolume'), axisLabel: {color: text}, nameTextStyle: {color: text}, splitLine: {show: false}}
    ],
    series: [
      {name: t('marketWorkbench.latestPrice'), type: 'line', data: rows.map(row => row.price), showSymbol: false,
        connectNulls: false, itemStyle: {color: color('--theme-blue', '#4f83c7')}, lineStyle: {width: 2}},
      {name: t('marketWorkbench.minuteVolume'), type: 'bar', xAxisIndex: 1, yAxisIndex: 1, barMaxWidth: 5,
        data: rows.map(row => ({value: row.volume == null ? null : row.volume / 10000,
          itemStyle: {color: row.up ? color('--theme-red', '#c64652') : color('--theme-green', '#087043')}}))}
    ]
  }, true)
  chart.resize()
}

const loadData = async () => {
  const current = ++requestId
  if (!props.thscode) return
  loading.value = true
  error.value = false
  try {
    const response = await getMarketWorkbenchIntraday(props.thscode)
    if (current !== requestId) return
    snapshot.value = response.data
    await renderChart()
  } catch {
    if (current !== requestId) return
    error.value = true
  } finally {
    if (current === requestId) loading.value = false
  }
}

watch(() => props.thscode, () => { snapshot.value = null; loadData() }, {immediate: true})
watch(locale, renderChart)
onMounted(() => {
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(chartRoot.value)
  timer = setInterval(() => {
    if (!document.hidden && !loading.value) loadData()
  }, 60000)
})
onBeforeUnmount(() => { requestId++; clearInterval(timer); observer?.disconnect(); chart?.dispose() })
</script>

<style scoped>
.intraday-panel { margin-top: 12px; }
.intraday-meta { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.intraday-meta span, small { color: var(--theme-muted, #7b8aa0); font-size: 11px; }
.intraday-chart { height: 350px; width: 100%; }
.intraday-state { display: grid; place-items: center; height: 310px; color: var(--theme-muted, #7b8aa0); }
@media (max-width: 680px) { .intraday-chart { height: 310px; } }
</style>
