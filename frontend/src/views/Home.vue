<template>
  <main class="research-desk">
    <header class="desk-heading">
      <div><p class="eyebrow">{{ $t('homeDesk.eyebrow') }}</p><h1>{{ $t('homeDesk.title') }}</h1></div>
      <div class="desk-actions"><a class="market-cloud-link" href="https://quote.eastmoney.com/stockhotmap/" target="_blank" rel="noopener noreferrer"><CloudOutlined />{{ $t('homeDesk.marketCloudMap') }}</a><a-button type="primary" size="large" class="new-research-button" @click="goToAnalysis"><PlusOutlined />{{ $t('homeDesk.newResearch') }}</a-button></div>
    </header>
    <section class="market-overview">
      <section class="focus-panel" :aria-label="$t('homeDesk.currentFocus')">
        <div class="focus-copy"><span class="focus-label">{{ $t('homeDesk.currentFocus') }}</span><h2>{{ $t('homeDesk.focusQuestion') }}</h2><p>{{ $t('homeDesk.focusDescription') }}</p><a-button type="link" class="review-link" @click="goToMarketReview">{{ $t('homeDesk.readReview') }} <ArrowRightOutlined /></a-button></div>
      </section>
      <section class="market-quick-card breadth-quick-card" :aria-label="$t('homeDesk.marketBreadth')"><p class="side-kicker">{{ $t('homeDesk.marketBreadth') }}</p><div class="quick-breadth-value"><strong>{{ marketBreadth }}<small>%</small></strong><span>{{ $t('homeDesk.upStocks', {count: marketActivity?.up ?? 0}) }}</span></div><div class="breadth-rail" aria-hidden="true"><i :style="{width: `${marketBreadth}%`}"></i></div></section>
      <section class="market-quick-card" :aria-label="$t('homeDesk.marketSnapshot')"><p class="side-kicker">{{ $t('homeDesk.marketSnapshot') }}</p><div v-if="marketIndexList.length" class="quick-index-list"><div v-for="index in marketIndexList" :key="index.symbol"><span>{{ index.name }}</span><strong>{{ index.value }}</strong><em :class="index.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(index.changePct) }}</em></div></div><a-skeleton v-else :paragraph="{rows: 3}" active /></section>
      <section class="market-quick-card market-activity-quick" :aria-label="$t('home.marketActivity')"><p class="side-kicker">{{ $t('home.marketActivity') }}</p><div class="activity-number"><strong>{{ marketActivity?.limitUp ?? '--' }}</strong><span>{{ $t('home.limitUp') }}</span></div><div class="activity-stats"><span><b>{{ marketActivity?.up ?? '--' }}</b>{{ $t('home.up') }}</span><span><b>{{ marketActivity?.down ?? '--' }}</b>{{ $t('home.down') }}</span></div></section>
    </section>
    <section class="market-insights" :aria-label="$t('homeDesk.marketInsights')">
      <div class="section-heading"><div><p class="eyebrow">{{ $t('homeDesk.marketInsights') }}</p><h2>{{ $t('homeDesk.capitalSignals') }}</h2></div><span class="insights-as-of">{{ $t('homeDesk.asOf', {date: marketInsights?.asOf || '-'}) }}</span></div>
      <div class="insight-grid">
        <section class="insight-panel" :aria-label="$t('homeDesk.dragonTiger')">
          <div class="insight-title"><div><p>{{ $t('homeDesk.dragonTiger') }}</p><small>{{ $t('homeDesk.dragonTigerHint') }}</small></div><span>{{ $t('homeDesk.netBuy') }}</span></div>
          <a-skeleton v-if="!dragonTigerList.length && isLoadingMarketSnapshot" :paragraph="{rows: 4}" active />
          <div v-else-if="dragonTigerList.length" class="insight-list">
            <article v-for="item in pagedDragonTigerList" :key="`${item.code}-${item.listedDate}`"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.listedDate }}</small></div><p class="insight-reason">{{ item.reason || '-' }}</p><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }}</b></div></article>
          </div>
          <a-pagination v-if="dragonTigerList.length > insightPageSize" v-model:current="dragonTigerPage" simple size="small" :page-size="insightPageSize" :total="dragonTigerList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </section>
        <section class="insight-panel" :aria-label="$t('homeDesk.fundFlow')">
          <div class="insight-title"><div><p>{{ $t('homeDesk.fundFlow') }}</p><small>{{ $t('homeDesk.fundFlowHint') }}</small></div><span>{{ $t('homeDesk.mainNetInflow') }}</span></div>
          <a-skeleton v-if="!fundFlowList.length && isLoadingMarketSnapshot" :paragraph="{rows: 4}" active />
          <div v-else-if="fundFlowList.length" class="insight-list">
            <article v-for="item in pagedFundFlowList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.sector ? ` · ${item.sector}` : '' }}</small></div><p class="insight-price">{{ $t('homeDesk.latestPrice') }} {{ formatPrice(item.price) }}</p><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b :class="item.netAmount >= 0 ? 'positive' : 'negative'">{{ formatAmount(item.netAmount) }} <small>{{ formatRatio(item.netRatio) }}</small></b></div></article>
          </div>
          <a-pagination v-if="fundFlowList.length > insightPageSize" v-model:current="fundFlowPage" simple size="small" :page-size="insightPageSize" :total="fundFlowList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </section>
        <section class="insight-panel" :aria-label="$t('homeDesk.popularityRank')">
          <div class="insight-title"><div><p>{{ $t('homeDesk.popularityRank') }}</p><small>{{ $t('homeDesk.popularityRankHint') }}</small></div><span>{{ $t('homeDesk.currentRank') }}</span></div>
          <a-skeleton v-if="!popularityRankList.length && isLoadingMarketSnapshot" :paragraph="{rows: 4}" active />
          <div v-else-if="popularityRankList.length" class="insight-list">
            <article v-for="item in pagedPopularityRankList" :key="item.code" class="popularity-item"><span class="popularity-rank">{{ item.rank }}</span><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}</small></div><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article>
          </div>
          <a-pagination v-if="popularityRankList.length > insightPageSize" v-model:current="popularityRankPage" simple size="small" :page-size="insightPageSize" :total="popularityRankList.length" :show-size-changer="false" />
          <a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" />
        </section>
        <section class="insight-panel" :aria-label="$t('homeDesk.industrySectors')">
          <div class="insight-title"><div><p>{{ $t('homeDesk.industrySectors') }}</p><small>{{ $t('homeDesk.industrySectorsHint') }}</small></div><span>{{ $t('homeDesk.industryNetInflow') }}</span></div>
          <a-skeleton v-if="!industrySectorsList.length && isLoadingMarketSnapshot" :paragraph="{rows: 4}" active />
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
    </section>
    <section class="limit-pools" :aria-label="$t('homeDesk.limitPoolMarket')">
      <div class="section-heading"><div><p class="eyebrow">{{ $t('homeDesk.limitPoolMarket') }}</p><h2>{{ $t('homeDesk.limitPoolMarket') }}</h2></div><span class="insights-as-of">{{ $t('homeDesk.limitPoolAsOf', {date: limitPoolTradeDate || '-'}) }}</span></div>
      <div class="limit-pool-grid">
        <section class="insight-panel" :aria-label="$t('homeDesk.limitUpPool')"><div class="insight-title"><div><p>{{ $t('homeDesk.limitUpPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div><a-skeleton v-if="!limitUpPoolList.length && isLoadingMarketSnapshot" :paragraph="{rows: 4}" active /><div v-else-if="limitUpPoolList.length" class="insight-list"><article v-for="item in pagedLimitUpPoolList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p class="insight-price">{{ item.detail }}</p><div class="insight-values"><em class="positive">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article></div><a-pagination v-if="limitUpPoolList.length > insightPageSize" v-model:current="limitUpPoolPage" simple size="small" :page-size="insightPageSize" :total="limitUpPoolList.length" :show-size-changer="false" /><a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" /></section>
        <section class="insight-panel" :aria-label="$t('homeDesk.limitDownPool')"><div class="insight-title"><div><p>{{ $t('homeDesk.limitDownPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div><a-skeleton v-if="!limitDownPoolList.length && isLoadingMarketSnapshot" :paragraph="{rows: 4}" active /><div v-else-if="limitDownPoolList.length" class="insight-list"><article v-for="item in pagedLimitDownPoolList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p class="insight-price">{{ item.detail }}</p><div class="insight-values"><em class="negative">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article></div><a-pagination v-if="limitDownPoolList.length > insightPageSize" v-model:current="limitDownPoolPage" simple size="small" :page-size="insightPageSize" :total="limitDownPoolList.length" :show-size-changer="false" /><a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" /></section>
        <section class="insight-panel" :aria-label="$t('homeDesk.strongPool')"><div class="insight-title"><div><p>{{ $t('homeDesk.strongPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div><a-skeleton v-if="!strongPoolList.length && isLoadingMarketSnapshot" :paragraph="{rows: 4}" active /><div v-else-if="strongPoolList.length" class="insight-list"><article v-for="item in pagedStrongPoolList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p class="insight-price">{{ item.detail }}</p><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article></div><a-pagination v-if="strongPoolList.length > insightPageSize" v-model:current="strongPoolPage" simple size="small" :page-size="insightPageSize" :total="strongPoolList.length" :show-size-changer="false" /><a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" /></section>
        <section class="insight-panel" :aria-label="$t('homeDesk.brokenPool')"><div class="insight-title"><div><p>{{ $t('homeDesk.brokenPool') }}</p><small>{{ $t('homeDesk.limitPoolHint') }}</small></div></div><a-skeleton v-if="!brokenPoolList.length && isLoadingMarketSnapshot" :paragraph="{rows: 4}" active /><div v-else-if="brokenPoolList.length" class="insight-list"><article v-for="item in pagedBrokenPoolList" :key="item.code"><div class="insight-stock"><strong>{{ item.name }}</strong><small>{{ item.code }}{{ item.industry ? ` · ${item.industry}` : '' }}</small></div><p class="insight-price">{{ item.detail }}</p><div class="insight-values"><em :class="item.changePct >= 0 ? 'positive' : 'negative'">{{ formatChange(item.changePct) }}</em><b>{{ formatPrice(item.price) }}</b></div></article></div><a-pagination v-if="brokenPoolList.length > insightPageSize" v-model:current="brokenPoolPage" simple size="small" :page-size="insightPageSize" :total="brokenPoolList.length" :show-size-changer="false" /><a-empty v-else :description="$t('homeDesk.noMarketData')" :image="aEmptyImage" /></section>
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
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {Empty} from 'ant-design-vue'
import {ArrowRightOutlined, CloudOutlined, PlusOutlined} from '@ant-design/icons-vue'
import {getMarketActivity, getMarketIndex, getMarketInsights} from '../api'

const router = useRouter()
const {t} = useI18n()
const aEmptyImage = Empty.PRESENTED_IMAGE_SIMPLE
const marketActivity = ref(null)
const marketIndex = ref(null)
const marketInsights = ref(null)
const marketSnapshotStorageKey = 'metrix.market-snapshot'
const marketRefreshInterval = 5 * 60 * 1000
const marketCloseHour = 15
let marketTimer = null
let isLoadingMarketSnapshot = false
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
const marketBreadth = computed(() => { const up = marketActivity.value?.up || 0; const down = marketActivity.value?.down || 0; return Math.round(up / (up + down || 1) * 100) })
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
const limitPoolTradeDate = computed(() => limitPools.value.tradeDate || '')
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
const isWeekend = date => [0, 6].includes(date.getDay())
const isTradingTime = (date = new Date()) => {
  if (isWeekend(date)) return false
  const time = date.getHours() * 100 + date.getMinutes()
  return (time >= 930 && time < 1130) || (time >= 1300 && time < 1500)
}
const hasRequiredMarketInsights = insights => Array.isArray(insights?.fundFlow) && insights.fundFlow.length > 0
  && Array.isArray(insights?.popularityRank) && insights.popularityRank.length > 0
const getLatestMarketCloseAt = now => {
  const closeAt = new Date(now)
  closeAt.setHours(marketCloseHour, 0, 0, 0)
  if (!isWeekend(closeAt) && closeAt.getTime() <= now.getTime()) return closeAt.getTime()

  do { closeAt.setDate(closeAt.getDate() - 1) } while (isWeekend(closeAt))
  return closeAt.getTime()
}
const isMarketSnapshotFresh = (snapshot, now = new Date()) => {
  const cachedAt = Date.parse(snapshot?.cachedAt || '')
  if (!Number.isFinite(cachedAt) || !hasRequiredMarketInsights(snapshot?.marketInsights)) return false
  if (isTradingTime(now)) return now.getTime() - cachedAt < marketRefreshInterval
  return cachedAt >= getLatestMarketCloseAt(now)
}
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
  marketInsights.value = snapshot?.marketInsights || null
}
const refreshMarketSnapshot = async () => {
  if (isLoadingMarketSnapshot) return
  isLoadingMarketSnapshot = true
  try {
    const [activityResult, indexResult, insightsResult] = await Promise.allSettled([getMarketActivity(), getMarketIndex(), getMarketInsights()])
    const resultData = result => result.status === 'fulfilled' ? result.value.data?.data || null : null
    const fetchedInsights = resultData(insightsResult)
    const hasFetchedRequiredInsights = hasRequiredMarketInsights(fetchedInsights)
    const snapshot = {
      marketActivity: resultData(activityResult) || marketActivity.value,
      marketIndex: resultData(indexResult) || marketIndex.value,
      marketInsights: hasFetchedRequiredInsights ? fetchedInsights : marketInsights.value
    }
    applyMarketSnapshot(snapshot)
    const failedResults = [activityResult, indexResult, insightsResult].filter(result => result.status === 'rejected')
    if (!failedResults.length && hasFetchedRequiredInsights) saveMarketSnapshot({...snapshot, cachedAt: new Date().toISOString()})
    failedResults.forEach(result => console.error(t('home.loadFailed'), result.reason))
  } finally { isLoadingMarketSnapshot = false }
}
const loadMarketSnapshot = async () => {
  const cachedSnapshot = getCachedMarketSnapshot()
  if (cachedSnapshot) applyMarketSnapshot(cachedSnapshot)

  if (!isMarketSnapshotFresh(cachedSnapshot)) await refreshMarketSnapshot()
}
const formatChange = value => `${value >= 0 ? '+' : ''}${Number(value || 0).toFixed(2)}%`
const formatAmount = value => `${Number(value || 0) >= 0 ? '+' : ''}${(Number(value || 0) / 100000000).toFixed(2)}${t('homeDesk.hundredMillion')}`
const formatPrice = value => Number(value || 0).toFixed(2)
const formatRatio = value => `${Number(value || 0) >= 0 ? '+' : ''}${Number(value || 0).toFixed(2)}%`
const goToAnalysis = () => router.push('/analysis')
const goToMarketReview = () => router.push('/market-review')
onMounted(() => {
  loadMarketSnapshot()
  marketTimer = window.setInterval(() => {
    const cachedSnapshot = getCachedMarketSnapshot()
    if (isTradingTime() || !isMarketSnapshotFresh(cachedSnapshot)) refreshMarketSnapshot()
  }, marketRefreshInterval)
})
onUnmounted(() => {
  if (marketTimer) window.clearInterval(marketTimer)
})
</script>

<style scoped>
.research-desk { width: 100%; max-width: none; padding: 24px 32px 52px; margin: 0; color: #182336; }.desk-heading, .section-heading { display: flex; align-items: end; justify-content: space-between; gap: 24px; }.desk-heading { padding: 12px 0 30px; }.eyebrow, .side-kicker { margin: 0 0 7px; color: #72809a; font-size: 11px; font-weight: 800; letter-spacing: .13em; text-transform: uppercase; }.desk-heading h1 { margin: 0; font-size: clamp(28px, 3vw, 36px); letter-spacing: -.045em; }.desk-actions { display: flex; align-items: center; gap: 12px; }.market-cloud-link { display: inline-flex; align-items: center; gap: 6px; padding: 8px 12px; color: #526178; font-size: 13px; font-weight: 600; text-decoration: none; border: 1px solid #dce2eb; border-radius: 8px; transition: color .16s, background .16s, border-color .16s; }.market-cloud-link:hover { color: #316bc0; background: #f1f5fb; border-color: #afc1df; }.new-research-button { min-width: 124px; }
.market-overview { display: grid; grid-template-columns: minmax(320px, 1.7fr) repeat(3, minmax(180px, 1fr)); gap: 20px; }.focus-panel { padding: 20px 24px; color: #fff; background: radial-gradient(circle at 87% 0%, #314a78 0, transparent 37%), #182336; border-radius: 12px; }.focus-label { color: #89a5d5; font-size: 10px; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }.focus-copy h2 { max-width: 680px; margin: 6px 0; font-size: clamp(18px, 1.7vw, 22px); line-height: 1.3; letter-spacing: -.025em; }.focus-copy p { max-width: 880px; margin: 0; color: #b4c2d9; font-size: 12px; line-height: 1.55; }.review-link { padding-left: 0; margin-top: 6px; color: #a7c1fb; }.breadth-rail { height: 5px; margin-top: 12px; overflow: hidden; background: #3a475d; border-radius: 999px; }.breadth-rail i { display: block; height: 100%; background: #80c9a1; border-radius: inherit; }.market-quick-card { padding: 20px 20px; background: #f5f7fa; border: 1px solid #e4e9f0; border-radius: 12px; }.market-quick-card .side-kicker { margin-bottom: 12px; }.breadth-quick-card { color: #fff; background: #263957; border-color: #263957; }.breadth-quick-card .side-kicker { color: #afc1df; }.quick-breadth-value { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }.quick-breadth-value strong { color: #fff; font-size: 32px; line-height: 1; }.quick-breadth-value strong small { margin-left: 2px; font-size: 14px; }.quick-breadth-value span { color: #c5d2e7; font-size: 12px; }.quick-index-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 12px; }.quick-index-list div { display: grid; grid-template-columns: 1fr auto; gap: 3px; }.quick-index-list span { color: #526178; font-size: 12px; }.quick-index-list strong, .quick-index-list em { justify-self: end; font-size: 14px; font-variant-numeric: tabular-nums; }.quick-index-list em { grid-column: 2; font-size: 12px; font-style: normal; }.market-activity-quick { color: #fff; background: #31415e; border-color: #31415e; }.market-activity-quick .side-kicker { color: #afc1df; }.market-activity-quick .activity-number { margin: 8px 0 12px; }.market-activity-quick .activity-number strong { font-size: 30px; }.market-activity-quick .activity-stats { padding-top: 9px; }.market-activity-quick .activity-stats b { font-size: 16px; }
.desk-manifesto { display: grid; gap: 9px; max-width: 830px; padding: 28px 0 4px; margin-top: 48px; border-top: 1px solid #dce2eb; }.manifesto-lead { margin: 0; color: #263957; font-size: clamp(18px, 2vw, 22px); font-weight: 700; line-height: 1.45; letter-spacing: -.025em; }.manifesto-body { max-width: 760px; margin: 0; color: #63718a; font-size: 14px; line-height: 1.8; }.desk-manifesto blockquote { max-width: 760px; padding-left: 14px; margin: 4px 0 0; color: #7c879a; font-size: 13px; font-style: italic; line-height: 1.75; border-left: 2px solid #a8b8d3; }
.section-heading { margin-bottom: 15px; }.section-heading h2 { margin: 0; font-size: 21px; letter-spacing: -.025em; }.section-heading :deep(.ant-btn-link) { padding-right: 0; }
.market-insights, .limit-pools { margin-top: 48px; }.insights-as-of { color: #8996aa; font-size: 12px; }.insight-grid, .limit-pool-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px; }.insight-panel { min-height: 334px; padding: 22px; background: #f5f7fa; border: 1px solid #e4e9f0; border-radius: 12px; }.insight-title { display: flex; align-items: start; justify-content: space-between; gap: 16px; padding-bottom: 13px; border-bottom: 1px solid #dce2eb; }.insight-title p { margin: 0; color: #182336; font-size: 16px; font-weight: 700; }.insight-title small { display: block; margin-top: 3px; color: #8996aa; font-size: 11px; }.insight-title > span { color: #71809a; font-size: 11px; white-space: nowrap; }.insight-list article { display: grid; grid-template-columns: minmax(98px, .8fr) minmax(0, 1.3fr) auto; align-items: center; gap: 12px; padding: 13px 0; border-bottom: 1px solid #e0e5ed; }.insight-list .popularity-item { grid-template-columns: 28px minmax(0, 1fr) auto; }.popularity-rank { display: grid; place-items: center; width: 24px; height: 24px; color: #526178; font-size: 12px; font-weight: 700; background: #e6ebf3; border-radius: 50%; }.insight-stock strong { display: block; color: #263957; font-size: 14px; }.insight-stock small, .insight-price, .insight-reason { display: block; margin: 3px 0 0; overflow: hidden; color: #8996aa; font-size: 11px; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }.insight-stock small em { margin-left: 3px; font-style: normal; }.insight-values { display: grid; justify-items: end; gap: 4px; font-variant-numeric: tabular-nums; }.insight-values em { font-size: 11px; font-style: normal; }.insight-values b { font-size: 12px; white-space: nowrap; }.insight-values b small { margin-left: 3px; font-size: 10px; font-weight: 500; }.insight-panel :deep(.ant-pagination) { display: flex; justify-content: end; margin: 16px 0 0; }.insight-panel :deep(.ant-empty) { padding-top: 72px; }.insight-panel :deep(.ant-empty-description) { color: #8996aa; font-size: 12px; }
.industry-sector-table { margin-top: 12px; }.industry-sector-table :deep(.ant-table) { background: transparent; }.industry-sector-table :deep(.ant-table-thead > tr > th) { padding: 9px 8px; color: #71809a; font-size: 12px; background: transparent; }.industry-sector-table :deep(.ant-table-tbody > tr > td) { padding: 11px 8px; color: #526178; font-size: 14px; background: transparent; }.industry-sector-table :deep(.ant-table-tbody small) { font-size: 12px; }.industry-sector-table :deep(.ant-pagination) { margin: 12px 0 0; }
.positive { color: #d7505d; }.negative { color: #006d2c; }.activity-number { display: flex; align-items: baseline; gap: 9px; margin: 20px 0 22px; }.activity-number strong { color: #ff98a2; font-size: 48px; line-height: .8; }.activity-number span { color: #c5d2e7; font-size: 12px; }.activity-stats { display: grid; grid-template-columns: 1fr 1fr; padding-top: 14px; border-top: 1px solid #536781; }.activity-stats span { display: grid; gap: 4px; color: #bbcae1; font-size: 11px; }.activity-stats span + span { padding-left: 16px; border-left: 1px solid #536781; }.activity-stats b { color: #fff; font-size: 19px; }
@media (max-width: 1000px) { .market-overview { grid-template-columns: repeat(2, minmax(0, 1fr)); }.focus-panel { grid-column: span 2; } } @media (max-width: 800px) { .research-desk { padding: 16px 16px 40px; }.desk-heading, .section-heading { align-items: start; flex-direction: column; }.desk-actions { width: 100%; }.market-cloud-link { flex: 1; justify-content: center; }.market-overview, .insight-grid, .limit-pool-grid { grid-template-columns: 1fr; }.focus-panel { grid-column: auto; }.quick-index-list { grid-template-columns: 1fr 1fr; row-gap: 12px; }.insight-list article { grid-template-columns: minmax(86px, .8fr) minmax(0, 1.2fr) auto; gap: 8px; } }
</style>
