<template>
  <main class="research-desk">
    <header class="desk-heading">
      <div><p class="eyebrow">{{ $t('homeDesk.eyebrow') }}</p><h1>{{ $t('homeDesk.title') }}</h1></div>
      <a-button type="primary" size="large" class="new-research-button" @click="goToAnalysis"><PlusOutlined />{{ $t('homeDesk.newResearch') }}</a-button>
    </header>
    <section class="focus-panel" :aria-label="$t('homeDesk.currentFocus')">
      <div class="focus-copy"><span class="focus-label">{{ $t('homeDesk.currentFocus') }}</span><h2>{{ $t('homeDesk.focusQuestion') }}</h2><p>{{ $t('homeDesk.focusDescription') }}</p><a-button type="link" class="review-link" @click="goToMarketReview">{{ $t('homeDesk.readReview') }} <ArrowRightOutlined /></a-button></div>
      <div class="breadth-card"><span>{{ $t('homeDesk.marketBreadth') }}</span><strong>{{ marketBreadth }}<small>%</small></strong><p>{{ $t('homeDesk.upStocks', {count: marketActivity?.up ?? 0}) }}</p><div class="breadth-rail" aria-hidden="true"><i :style="{width: `${marketBreadth}%`}"></i></div></div>
    </section>
    <section class="desk-content">
      <section class="research-inbox" :aria-label="$t('home.recentAnalysis')">
        <div class="section-heading"><div><p class="eyebrow">{{ $t('homeDesk.researchInbox') }}</p><h2>{{ $t('home.recentAnalysis') }}</h2></div><a-button type="link" @click="goToAnalysis">{{ $t('homeDesk.openWorkspace') }}</a-button></div>
        <div v-if="recentAnalysis.length" class="research-list"><button v-for="record in recentAnalysis" :key="record.id" type="button" @click="goToAnalysisDetail(record)"><span class="record-code">{{ record.stockCode }}</span><div><strong>{{ record.stockName || record.stockCode }}</strong><small>{{ statusText(record.status) }}</small></div><time>{{ formatTime(record.createTime) }}</time><ArrowRightOutlined /></button></div>
        <a-empty v-else :description="$t('analysis.noRecords')" :image="aEmptyImage"><a-button type="primary" @click="goToAnalysis">{{ $t('homeDesk.newResearch') }}</a-button></a-empty>
      </section>
      <aside class="desk-sidebar">
        <section class="side-panel" :aria-label="$t('homeDesk.marketSnapshot')"><p class="side-kicker">{{ $t('homeDesk.marketSnapshot') }}</p><div v-if="marketIndexList.length" class="index-list"><div v-for="index in marketIndexList" :key="index.symbol" class="index-item"><span>{{ index.name }}</span><strong>{{ index.value }}</strong><em :class="index.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(index.changePct) }}</em></div></div><a-skeleton v-else :paragraph="{rows: 4}" active /></section>
        <nav class="side-panel workspace-panel" :aria-label="$t('homeDesk.workspace')"><p class="side-kicker">{{ $t('homeDesk.workspace') }}</p><button type="button" @click="goToPortfolio"><WalletOutlined /><span>{{ $t('menu.portfolio') }}</span><b>{{ holdingCount }}</b></button><button type="button" @click="goToMarketReview"><FundOutlined /><span>{{ $t('menu.marketReview') }}</span><b>{{ reviewCount }}</b></button><button type="button" @click="goToChat"><MessageOutlined /><span>{{ $t('menu.chat') }}</span><ArrowRightOutlined /></button></nav>
        <section class="side-panel activity-panel" :aria-label="$t('home.marketActivity')"><p class="side-kicker">{{ $t('home.marketActivity') }}</p><div class="activity-number"><strong>{{ marketActivity?.limitUp ?? '--' }}</strong><span>{{ $t('home.limitUp') }}</span></div><div class="activity-stats"><span><b>{{ marketActivity?.up ?? '--' }}</b>{{ $t('home.up') }}</span><span><b>{{ marketActivity?.down ?? '--' }}</b>{{ $t('home.down') }}</span></div></section>
      </aside>
    </section>
  </main>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {Empty} from 'ant-design-vue'
import {ArrowRightOutlined, FundOutlined, MessageOutlined, PlusOutlined, WalletOutlined} from '@ant-design/icons-vue'
import {getAllAnalysis, getMarketActivity, getMarketIndex, getMarketReviews, getPortfolioHoldings} from '../api'

const router = useRouter()
const {t} = useI18n()
const aEmptyImage = Empty.PRESENTED_IMAGE_SIMPLE
const recentAnalysis = ref([])
const reviewCount = ref(0)
const holdingCount = ref(0)
const marketActivity = ref(null)
const marketIndex = ref(null)
const marketSnapshotStorageKey = 'metrix.market-snapshot'
const marketRefreshInterval = 5 * 60 * 1000
let marketTimer = null
let isLoadingMarketSnapshot = false

const indexLabels = computed(() => ({sh000001: t('homeDesk.shanghaiIndex'), sz399001: t('homeDesk.shenzhenIndex'), sz399006: t('homeDesk.chinextIndex'), sh000688: t('homeDesk.star50Index')}))
const marketIndexList = computed(() => Object.entries(marketIndex.value || {}).map(([symbol, data]) => ({symbol, name: indexLabels.value[symbol] || data.name || symbol, value: data.current?.toFixed(2) || '-', changePct: data.changePct || 0})))
const marketBreadth = computed(() => { const up = marketActivity.value?.up || 0; const down = marketActivity.value?.down || 0; return Math.round(up / (up + down || 1) * 100) })
const loadDashboard = async () => {
  try {
    const [analysisResponse, reviewResponse, holdingResponse] = await Promise.all([getAllAnalysis(), getMarketReviews(), getPortfolioHoldings()])
    recentAnalysis.value = analysisResponse.data?.slice(0, 8) || []
    reviewCount.value = reviewResponse.data?.length || 0
    holdingCount.value = holdingResponse.data?.holdings?.length || 0
  } catch (error) { console.error(t('home.loadFailed'), error) }
}
const isTradingTime = () => { const date = new Date(); if ([0, 6].includes(date.getDay())) return false; const time = date.getHours() * 100 + date.getMinutes(); return (time >= 930 && time < 1130) || (time >= 1300 && time < 1500) }
const getCachedMarketSnapshot = () => {
  try {
    const snapshot = window.localStorage.getItem(marketSnapshotStorageKey)
    return snapshot ? JSON.parse(snapshot) : null
  } catch (error) {
    console.warn('Unable to read cached market snapshot', error)
    return null
  }
}
const saveMarketSnapshot = snapshot => {
  try { window.localStorage.setItem(marketSnapshotStorageKey, JSON.stringify(snapshot)) } catch (error) { console.warn('Unable to cache market snapshot', error) }
}
const applyMarketSnapshot = snapshot => {
  marketActivity.value = snapshot?.marketActivity || null
  marketIndex.value = snapshot?.marketIndex || null
}
const refreshMarketSnapshot = async () => {
  if (isLoadingMarketSnapshot) return
  isLoadingMarketSnapshot = true
  try {
    const [activityResponse, indexResponse] = await Promise.all([getMarketActivity(), getMarketIndex()])
    const snapshot = {marketActivity: activityResponse.data?.data || null, marketIndex: indexResponse.data?.data || null}
    applyMarketSnapshot(snapshot)
    saveMarketSnapshot(snapshot)
  } catch (error) { console.error(t('home.loadFailed'), error) } finally { isLoadingMarketSnapshot = false }
}
const loadMarketSnapshot = async () => {
  const cachedSnapshot = getCachedMarketSnapshot()
  if (cachedSnapshot) applyMarketSnapshot(cachedSnapshot)

  // 非交易时段沿用最近一次快照；仅首次没有本地快照时才请求一次，以便初始化展示收盘数据。
  if (isTradingTime() || !cachedSnapshot) await refreshMarketSnapshot()
}
const formatTime = value => value ? new Date(value).toLocaleString() : '-'
const formatChange = value => `${value >= 0 ? '+' : ''}${Number(value || 0).toFixed(2)}%`
const statusText = status => ({PENDING: t('home.analyzing'), PROCESSING: t('home.analyzing'), COMPLETED: t('home.completed'), FAILED: t('home.failed')}[status] || status || '-')
const goToAnalysis = () => router.push('/analysis')
const goToAnalysisDetail = record => router.push({path: '/analysis', query: {id: record.id}})
const goToPortfolio = () => router.push('/portfolio')
const goToMarketReview = () => router.push('/market-review')
const goToChat = () => router.push('/chat')
onMounted(() => {
  loadDashboard()
  loadMarketSnapshot()
  marketTimer = window.setInterval(() => { if (isTradingTime()) refreshMarketSnapshot() }, marketRefreshInterval)
})
onUnmounted(() => { if (marketTimer) window.clearInterval(marketTimer) })
</script>

<style scoped>
.research-desk { max-width: 1240px; padding: 16px 4px 44px; margin: 0 auto; color: #182336; }.desk-heading, .section-heading { display: flex; align-items: end; justify-content: space-between; gap: 24px; }.desk-heading { padding: 12px 0 30px; }.eyebrow, .side-kicker { margin: 0 0 7px; color: #72809a; font-size: 11px; font-weight: 800; letter-spacing: .13em; text-transform: uppercase; }.desk-heading h1 { margin: 0; font-size: clamp(28px, 3vw, 36px); letter-spacing: -.045em; }.new-research-button { min-width: 124px; }
.focus-panel { display: grid; grid-template-columns: minmax(0, 1fr) 276px; gap: 34px; padding: 34px; color: #fff; background: radial-gradient(circle at 87% 0%, #314a78 0, transparent 37%), #182336; border-radius: 18px; }.focus-label { color: #89a5d5; font-size: 11px; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }.focus-copy h2 { max-width: 680px; margin: 12px 0; font-size: clamp(20px, 2.4vw, 26px); line-height: 1.35; letter-spacing: -.025em; }.focus-copy p { max-width: 680px; margin: 0; color: #b4c2d9; line-height: 1.75; }.review-link { padding-left: 0; margin-top: 14px; color: #a7c1fb; }.breadth-card { align-self: stretch; padding: 9px 0 6px 32px; border-left: 1px solid #43516a; }.breadth-card > span, .breadth-card p { display: block; margin: 0; color: #adbad0; font-size: 12px; }.breadth-card strong { display: block; margin: 12px 0 4px; font-size: 48px; line-height: 1; }.breadth-card strong small { margin-left: 2px; font-size: 19px; }.breadth-rail { height: 7px; margin-top: 18px; overflow: hidden; background: #3a475d; border-radius: 999px; }.breadth-rail i { display: block; height: 100%; background: #80c9a1; border-radius: inherit; }
.desk-content { display: grid; grid-template-columns: minmax(0, 1fr) 310px; gap: 48px; margin-top: 42px; }.section-heading { margin-bottom: 15px; }.section-heading h2 { margin: 0; font-size: 21px; letter-spacing: -.025em; }.section-heading :deep(.ant-btn-link) { padding-right: 0; }.research-list { border-top: 1px solid #dce2eb; }.research-list button { display: grid; grid-template-columns: 92px minmax(0, 1fr) auto 16px; align-items: center; width: 100%; gap: 15px; padding: 18px 8px; color: inherit; text-align: left; background: transparent; border: 0; border-bottom: 1px solid #dce2eb; cursor: pointer; transition: background .16s; }.research-list button:hover { background: #f1f5fb; }.record-code { color: #61728e; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }.research-list strong { display: block; font-size: 15px; }.research-list small { display: block; margin-top: 3px; color: #8996aa; font-size: 11px; }.research-list time { color: #8996aa; font-size: 12px; }.research-list :deep(svg) { color: #8190a5; }
.desk-sidebar { display: grid; align-content: start; gap: 20px; }.side-panel { padding: 22px; background: #f5f7fa; border-radius: 12px; }.side-kicker { display: block; margin-bottom: 17px; }.index-item { display: grid; grid-template-columns: 1fr auto; gap: 3px; padding: 10px 0; border-top: 1px solid #e0e5ed; }.index-item span { color: #526178; font-size: 12px; }.index-item strong, .index-item em { justify-self: end; text-align: right; font-variant-numeric: tabular-nums; }.index-item strong { font-size: 13px; }.index-item em { grid-column: 2; font-size: 11px; font-style: normal; }.positive { color: #d7505d; }.negative { color: #1a9a6a; }.workspace-panel button { display: grid; grid-template-columns: 22px 1fr auto; align-items: center; width: 100%; padding: 12px 0; color: #344156; text-align: left; background: transparent; border: 0; border-top: 1px solid #e0e5ed; cursor: pointer; }.workspace-panel button:hover { color: #316bc0; }.workspace-panel button b { color: #17243a; }.workspace-panel button :deep(svg) { color: #6782ae; }.activity-panel { color: #fff; background: #31415e; }.activity-panel .side-kicker { color: #afc1df; }.activity-number { display: flex; align-items: baseline; gap: 9px; margin: 20px 0 22px; }.activity-number strong { color: #ff98a2; font-size: 48px; line-height: .8; }.activity-number span { color: #c5d2e7; font-size: 12px; }.activity-stats { display: grid; grid-template-columns: 1fr 1fr; padding-top: 14px; border-top: 1px solid #536781; }.activity-stats span { display: grid; gap: 4px; color: #bbcae1; font-size: 11px; }.activity-stats span + span { padding-left: 16px; border-left: 1px solid #536781; }.activity-stats b { color: #fff; font-size: 19px; }
@media (max-width: 800px) { .desk-heading, .section-heading { align-items: start; flex-direction: column; }.focus-panel, .desk-content { grid-template-columns: 1fr; }.breadth-card { padding: 20px 0 0; border-top: 1px solid #43516a; border-left: 0; }.research-list button { grid-template-columns: 75px minmax(0, 1fr) 16px; }.research-list time { display: none; } }
</style>
