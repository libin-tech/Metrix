<template>
  <div class="market-review-page">
    <div class="market-review-layout">
      <!-- 左侧：复盘记录列表 -->
      <div class="left-panel">
        <a-card :bordered="false" class="records-card" :body-style="{ flex: '1', overflow: 'hidden', display: 'flex', flexDirection: 'column' }">
          <template #title>
            <div class="records-header">
              <span>{{ $t('marketReview.title') }}</span>
              <a-button type="primary" @click="handleTrigger" :loading="triggerLoading">
                <ReloadOutlined /> {{ $t('marketReview.trigger') }}
              </a-button>
            </div>
          </template>

          <div class="records-list" v-if="reviews.length > 0" @scroll="handleScroll">
            <a-list :data-source="reviews">
              <template #renderItem="{ item }">
                <a-list-item
                  :class="{ active: selectedRecord?.id === item.id }"
                  @click="selectRecord(item)"
                >
                  <a-list-item-meta>
                    <template #title>
                      <div class="title-row">
                        <span class="status-dot" :style="{ backgroundColor: statusDotColor(item.status) }"></span>
                        <span class="review-name">{{ item.reviewName }}</span>
                        <span class="summary-tag" v-if="item.summary">
                          <a-tag :color="summaryColor(item.summary)" size="small">{{ item.summary }}</a-tag>
                        </span>
                      </div>
                    </template>
                    <template #description>
                      <span class="time-info">
                        <ClockCircleOutlined /> {{ item.reviewDate }}
                      </span>
                    </template>
                  </a-list-item-meta>
                  <a-button
                    type="text"
                    danger
                    size="small"
                    class="delete-btn"
                    @click.stop="handleDelete(item.id)"
                  >
                    <DeleteOutlined />
                  </a-button>
                </a-list-item>
              </template>
            </a-list>
            <div v-if="allLoaded" class="end-hint end-loaded">—— 已经到底了 ——</div>
            <div v-else-if="loadingMore" class="end-hint"><ReloadOutlined /> 加载中...</div>
            <div v-else class="end-hint end-more" @click="loadMore(false)">↓ 点击加载更多</div>
          </div>

          <div v-if="reviews.length === 0" class="empty-tip">
            <FundOutlined />
            <p>{{ $t('marketReview.noRecords') }}</p>
            <p class="empty-hint">{{ $t('marketReview.triggerHint') }}</p>
          </div>
        </a-card>
      </div>

      <!-- 右侧：复盘详情 -->
      <div class="right-panel">
        <a-card v-if="selectedRecord" :bordered="false" class="detail-card">
          <template #title>
            <div class="review-brief-heading">
              <div>
                <p class="review-kicker">{{ $t('marketReview.dailyBrief') }}</p>
                <h1>{{ selectedRecord.reviewName }}</h1>
              </div>
              <time>{{ selectedRecord.reviewDate }}</time>
            </div>
          </template>
          <template #extra>
            <a-button type="text" class="review-refresh-button" @click="refreshCurrentRecord">
              <ReloadOutlined />
            </a-button>
          </template>

          <div class="review-meta">
            <span class="review-meta-item">
              <span class="status-dot" :style="{ backgroundColor: statusDotColor(selectedRecord.status) }"></span>
              {{ statusText(selectedRecord.status) }}
            </span>
            <span class="review-meta-item" v-if="selectedRecord.summary">
              <a-tag :color="summaryColor(selectedRecord.summary)">{{ selectedRecord.summary }}</a-tag>
            </span>
          </div>

          <div v-if="selectedRecord.status === 'FAILED'" class="review-error">
            {{ selectedRecord.errorMessage }}
          </div>

          <div v-if="selectedRecord.status === 'REVIEWING'" class="review-loading">
            <a-spin /> {{ $t('marketReview.reviewingHint') }}
          </div>

          <div v-if="selectedRecord.coreSummary" class="core-summary">
            <div class="core-summary-title">{{ $t('marketReview.coreSummary') }}</div>
            <div class="core-summary-content markdown-content" v-html="renderedCoreSummary"></div>
          </div>

          <div v-if="selectedRecord.detail" class="review-detail markdown-content" v-html="renderedDetail"></div>
        </a-card>

        <a-card v-else :bordered="false" class="detail-card">
          <div class="empty-detail">
            <FundOutlined style="font-size: 48px; color: #d9d9d9;" />
            <p>{{ $t('marketReview.selectHint') }}</p>
          </div>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message, Modal} from 'ant-design-vue'
import {ClockCircleOutlined, DeleteOutlined, FundOutlined, ReloadOutlined} from '@ant-design/icons-vue'
import {marked} from 'marked'
import {
  createMarketReview,
  deleteMarketReview,
  getMarketReviewCursor,
  getMarketReviewDetail,
  triggerMarketReview
} from '../api'

const {t} = useI18n()
const route = useRoute()

const reviews = ref([])
const loading = ref(false)
const triggerLoading = ref(false)
const selectedRecord = ref(null)
const refreshTimer = ref(null)
const cursor = ref(null)
const hasMore = ref(true)
const loadingMore = ref(false)
const allLoaded = ref(false)

const statusDotColor = (status) => {
  if (!status) return '#d9d9d9'
  if (status === 'REVIEWING') return '#fadb14'
  if (status === 'COMPLETED') return '#52c41a'
  if (status === 'FAILED') return '#ff4d4f'
  return '#d9d9d9'
}

const statusText = (status) => {
  if (!status) return ''
  if (status === 'REVIEWING') return t('marketReview.statusReviewing')
  if (status === 'COMPLETED') return t('marketReview.statusCompleted')
  if (status === 'FAILED') return t('marketReview.statusFailed')
  return status
}

const summaryColor = (summary) => {
  if (!summary) return 'default'
  if (summary === '暴涨') return '#722ed1'
  if (summary === '暴跌') return '#006d2c'
  if (summary === '大涨') return '#f5222d'
  if (summary === '大跌') return '#006d2c'
  if (summary === '小涨') return '#ff4d4f'
  if (summary === '小跌') return '#006d2c'
  if (summary === '微涨') return '#ffa39e'
  if (summary === '微跌') return '#006d2c'
  return 'default'
}

const renderedDetail = computed(() => {
  if (!selectedRecord.value?.detail) return ''
  return marked(selectedRecord.value.detail)
})

const renderedCoreSummary = computed(() => {
  if (!selectedRecord.value?.coreSummary) return ''
  return marked(selectedRecord.value.coreSummary)
})

const loadMore = async (reset = false) => {
  if (!reset && (!hasMore.value || loadingMore.value)) return
  if (reset) {
    cursor.value = null
    hasMore.value = true
    allLoaded.value = false
  }
  loadingMore.value = true
  try {
    const res = await getMarketReviewCursor(cursor.value)
    const result = res.data
    const items = result.items || []
    if (reset) {
      reviews.value = items
    } else {
      reviews.value = reviews.value.concat(items)
    }
    hasMore.value = result.hasMore
    cursor.value = result.nextCursor
    if (!hasMore.value) {
      allLoaded.value = true
    }
  } catch {
    message.error(t('marketReview.loadFailed'))
  } finally {
    loadingMore.value = false
  }
}

const handleScroll = (e) => {
  const el = e.target
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 50) {
    loadMore(false)
  }
}

const selectRecord = async (record) => {
  try {
    const res = await getMarketReviewDetail(record.id)
    selectedRecord.value = res.data
    if (selectedRecord.value.status === 'REVIEWING') {
      startPolling()
    }
  } catch {
    message.error(t('marketReview.loadFailed'))
  }
}

const refreshCurrentRecord = async () => {
  if (!selectedRecord.value) return
  await selectRecord(selectedRecord.value)
}

const handleTrigger = async () => {
  triggerLoading.value = true
  try {
    const res = await triggerMarketReview()
    const data = res.data
    const targetDate = data.targetDate
    const reviewName = data.reviewName

    if (data.notClosed) {
      Modal.confirm({
        title: t('marketReview.notClosed'),
        content: t('marketReview.willReviewPrev', {date: targetDate}),
        onOk: async () => {
          if (data.existingReview) {
            Modal.confirm({
              title: t('marketReview.confirmReRun'),
              content: t('marketReview.reRunDesc', {name: reviewName}),
              onOk: async () => doExecute(targetDate),
            })
          } else {
            await doExecute(targetDate)
          }
        }
      })
    } else if (data.existingReview) {
      Modal.confirm({
        title: t('marketReview.confirmReRun'),
        content: t('marketReview.reRunDesc', {name: reviewName}),
        onOk: async () => doExecute(targetDate),
      })
    } else {
      await doExecute(targetDate)
    }
  } catch (e) {
    message.error(e.response?.data?.message || t('marketReview.triggerFailed'))
  } finally {
    triggerLoading.value = false
  }
}

const doExecute = async (reviewDate) => {
  try {
    const res = await createMarketReview(reviewDate)
    message.success(t('marketReview.executeSuccess'))
    await loadMore(true)
    selectedRecord.value = res.data
    startPolling()
  } catch (e) {
    message.error(e.response?.data?.message || t('marketReview.executeFailed'))
  }
}

const startPolling = () => {
  stopPolling()
  refreshTimer.value = setInterval(async () => {
    if (!selectedRecord.value || selectedRecord.value.status !== 'REVIEWING') {
      stopPolling()
      return
    }
    await refreshCurrentRecord()
    if (selectedRecord.value.status !== 'REVIEWING') {
      stopPolling()
      await loadMore(true)
    }
  }, 3000)
}

const stopPolling = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

const handleDelete = (id) => {
  Modal.confirm({
    title: t('config.confirmDelete'),
    content: t('marketReview.confirmDeleteDesc'),
    onOk: async () => {
      try {
        await deleteMarketReview(id)
        message.success(t('config.deleteSuccess'))
        if (selectedRecord.value?.id === id) {
          selectedRecord.value = null
        }
        await loadMore(true)
      } catch {
        message.error(t('config.deleteFailed'))
      }
    }
  })
}

onMounted(async () => {
  await loadMore(true)
  const reviewId = route.query.id
  if (reviewId) {
    const found = reviews.value.find(r => r.id === Number(reviewId))
    if (found) {
      await selectRecord(found)
    } else {
      try {
        const res = await getMarketReviewDetail(Number(reviewId))
        selectedRecord.value = res.data
      } catch {
        // ignore, user can select manually
      }
    }
  }
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.market-review-page {
  padding: 20px 24px;
  height: calc(100vh - 84px);
  overflow: hidden;
  background: #f7f9fc;
}

.market-review-layout {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  gap: 32px;
  height: 100%;
  overflow: hidden;
}

.left-panel {
  height: 100%;
  overflow: hidden;
}

.records-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #e1e7ef;
  border-radius: 14px;
  box-shadow: none;
}

.records-card :deep(.ant-card-head) { min-height: 62px; padding: 0 18px; border-bottom: 1px solid #e1e7ef; }
.records-card :deep(.ant-card-head-title) { padding: 0; }

.records-card :deep(.ant-card-body) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  color: #1b2a42;
  font-size: 15px;
}

.records-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.records-list :deep(.ant-list-item) { padding: 14px 16px; margin: 2px 8px; border-radius: 10px; border-bottom: 0; transition: background .16s ease; }
.records-list :deep(.ant-list-item:hover) { background: #f2f6fb; }
.records-list .active { background: #eaf1fb; }

.title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.summary-tag {
  margin-left: auto;
  flex-shrink: 0;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.review-name {
  font-size: 13px;
  vertical-align: middle;
}

.time-info {
  font-size: 12px;
  color: #999;
}

.summary-tag {
  margin-left: 8px;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.records-list :deep(.ant-list-item:hover) .delete-btn {
  opacity: 1;
}

.empty-tip {
  text-align: center;
  padding: 60px 0;
  color: #bbb;
}

.empty-tip p {
  margin-top: 12px;
  font-size: 14px;
}

.empty-hint {
  font-size: 12px;
  color: #d9d9d9;
}

.end-hint {
  text-align: center;
  padding: 16px;
  color: #bbb;
  font-size: 13px;
}

.end-more {
  cursor: pointer;
  transition: color 0.2s;
}

.end-more:hover {
  color: #1890ff;
}

.right-panel {
  height: 100%;
  overflow: hidden;
}

.detail-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
  box-shadow: none;
}

.detail-card :deep(.ant-card-head) { min-height: 84px; padding: 0 16px 0 2px; border-bottom: 1px solid #dfe6ef; }
.detail-card :deep(.ant-card-head-title) { padding: 18px 0; overflow: visible; white-space: normal; }
.detail-card :deep(.ant-card-extra) { display: flex; align-items: center; padding: 0 0 0 20px; }

.detail-card :deep(.ant-card-body) {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.review-brief-heading { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.review-kicker { margin: 0 0 5px; color: #72809a; font-size: 10px; font-weight: 800; letter-spacing: .13em; text-transform: uppercase; }
.review-brief-heading h1 { margin: 0; color: #1a2a43; font-size: 23px; line-height: 1.3; letter-spacing: -.03em; }
.review-brief-heading time { color: #8491a5; font-size: 12px; white-space: nowrap; }
.review-refresh-button { color: #5a7198; }

.review-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 24px 0 16px;
  font-size: 12px;
}

.review-meta-item { display: inline-flex; align-items: center; min-height: 28px; padding: 0 10px; color: #60718a; background: #eef3f9; border-radius: 999px; }
.review-meta-item .status-dot { margin-right: 7px; }
.review-meta-item :deep(.ant-tag) { margin: 0; border: 0; border-radius: 999px; }

.core-summary {
  margin: 0 0 28px;
  padding: 24px 26px;
  color: #e8effa;
  background: radial-gradient(circle at 88% 0%, #3c5787 0, transparent 34%), #1b2b45;
  border: 0;
  border-radius: 16px;
}

.core-summary-title {
  margin-bottom: 10px;
  color: #9eb7e3;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .12em;
  text-transform: uppercase;
}

.core-summary-content {
  font-size: 14px;
  line-height: 1.85;
  color: #edf3fc;
  white-space: pre-wrap;
}

.core-summary-content :deep(p) { margin: 0 0 10px; }
.core-summary-content :deep(p:last-child) { margin-bottom: 0; }

.review-error {
  padding: 12px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #cf1322;
  margin-bottom: 16px;
}

.review-loading {
  text-align: center;
  padding: 60px 0;
  color: #999;
  font-size: 14px;
}

.empty-detail {
  text-align: center;
  padding: 120px 0;
  color: #bbb;
}

.empty-detail p {
  margin-top: 16px;
  font-size: 14px;
}

.review-detail {
  padding: 6px 4px 32px;
  line-height: 1.9;
  font-size: 14px;
  color: #38475e;
}

.review-detail :deep(h1),
.review-detail :deep(h2),
.review-detail :deep(h3) {
  margin-top: 28px;
  margin-bottom: 12px;
  color: #243652;
}

.review-detail :deep(h1:first-child), .review-detail :deep(h2:first-child), .review-detail :deep(h3:first-child) { margin-top: 0; }

.review-detail :deep(p) {
  margin-bottom: 8px;
}

.review-detail :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.review-detail :deep(th),
.review-detail :deep(td) {
  border: 1px solid #e8e8e8;
  padding: 6px 12px;
  text-align: left;
}

.review-detail :deep(th) {
  background: #eef3f9;
  font-weight: bold;
}

.review-detail :deep(code) {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 13px;
}

.review-detail :deep(pre) {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
}

@media (max-width: 900px) {
  .market-review-page { height: auto; min-height: calc(100vh - 76px); padding: 18px; overflow: visible; }
  .market-review-layout { grid-template-columns: 1fr; gap: 20px; height: auto; }
  .left-panel, .right-panel, .records-card, .detail-card { height: auto; }
  .records-list { max-height: 360px; }
}

@media (max-width: 640px) {
  .detail-card :deep(.ant-card-head) { min-height: auto; padding: 0; }
  .detail-card :deep(.ant-card-extra) { padding: 0 0 14px; }
  .review-brief-heading { align-items: start; flex-direction: column; gap: 4px; }
  .review-brief-heading h1 { font-size: 20px; }
  .core-summary { padding: 20px; }
}
</style>
