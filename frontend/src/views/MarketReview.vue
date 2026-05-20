<template>
  <div class="market-review-page">
    <div class="market-review-layout">
      <!-- 左侧：复盘记录列表 -->
      <div class="left-panel">
        <a-card :bordered="false" class="records-card">
          <template #title>
            <div class="records-header">
              <span>{{ $t('marketReview.title') }}</span>
              <a-button type="primary" @click="handleTrigger" :loading="triggerLoading">
                <ReloadOutlined /> {{ $t('marketReview.trigger') }}
              </a-button>
            </div>
          </template>

          <div class="records-list" v-if="reviews.length > 0">
            <a-list :data-source="reviews">
              <template #renderItem="{ item }">
                <a-list-item
                  :class="{ active: selectedRecord?.id === item.id }"
                  @click="selectRecord(item)"
                >
                  <a-list-item-meta>
                    <template #title>
                      <a-tag :color="statusColor(item.status)" size="small">{{ statusText(item.status) }}</a-tag>
                      <span class="review-name">{{ item.reviewName }}</span>
                    </template>
                    <template #description>
                      <span class="time-info">
                        <ClockCircleOutlined /> {{ item.reviewDate }}
                      </span>
                      <span class="summary-tag" v-if="item.summary">
                        <a-tag :color="summaryColor(item.summary)" size="small">{{ item.summary }}</a-tag>
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
            <div class="detail-header">
              <span>{{ selectedRecord.reviewName }}</span>
              <a-button size="small" @click="refreshCurrentRecord">
                <ReloadOutlined />
              </a-button>
            </div>
          </template>

          <div class="review-meta">
            <span><b>{{ $t('marketReview.status') }}：</b>
              <a-tag :color="statusColor(selectedRecord.status)">{{ statusText(selectedRecord.status) }}</a-tag>
            </span>
            <span style="margin-left: 24px"><b>{{ $t('marketReview.reviewDate') }}：</b>{{ selectedRecord.reviewDate }}</span>
            <span style="margin-left: 24px" v-if="selectedRecord.summary"><b>{{ $t('marketReview.summary') }}：</b>
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
import {useI18n} from 'vue-i18n'
import {message, Modal} from 'ant-design-vue'
import {ClockCircleOutlined, DeleteOutlined, FundOutlined, ReloadOutlined} from '@ant-design/icons-vue'
import {marked} from 'marked'
import {
  getMarketReviews,
  getMarketReviewDetail,
  deleteMarketReview,
  triggerMarketReview,
  createMarketReview
} from '../api'

const {t} = useI18n()

const reviews = ref([])
const loading = ref(false)
const triggerLoading = ref(false)
const selectedRecord = ref(null)
const refreshTimer = ref(null)

const statusColor = (status) => {
  if (!status) return 'default'
  if (status === 'REVIEWING') return 'processing'
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'error'
  return 'default'
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
  if (summary.includes('大幅上涨')) return 'red'
  if (summary.includes('小幅上涨')) return 'orange'
  if (summary.includes('大幅下跌')) return 'green'
  if (summary.includes('小幅下跌')) return 'blue'
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

const loadReviews = async () => {
  loading.value = true
  try {
    const res = await getMarketReviews()
    reviews.value = res.data || []
  } catch {
    message.error(t('marketReview.loadFailed'))
  } finally {
    loading.value = false
  }
}

const selectRecord = async (record) => {
  try {
    const res = await getMarketReviewDetail(record.id)
    selectedRecord.value = res.data
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
    await loadReviews()
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
      await loadReviews()
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
        await loadReviews()
      } catch {
        message.error(t('config.deleteFailed'))
      }
    }
  })
}

onMounted(() => {
  loadReviews()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.market-review-page {
  padding: 20px;
  min-height: calc(100vh - 84px);
  background: #f5f7fa;
}

.market-review-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  min-height: calc(100vh - 124px);
}

.left-panel {
  height: calc(100vh - 124px);
  overflow-y: auto;
}

.records-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.records-list {
  flex: 1;
  overflow-y: auto;
}

.records-list .active {
  background: #e6f7ff;
}

.review-name {
  margin-left: 6px;
  font-size: 13px;
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

.right-panel {
  height: calc(100vh - 124px);
  overflow-y: auto;
}

.detail-card {
  min-height: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-meta {
  margin-bottom: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 14px;
}

.core-summary {
  margin-bottom: 20px;
  padding: 16px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
}

.core-summary-title {
  font-weight: bold;
  font-size: 15px;
  color: #d46b08;
  margin-bottom: 8px;
}

.core-summary-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}

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
  line-height: 1.8;
  font-size: 14px;
}

.review-detail :deep(h1),
.review-detail :deep(h2),
.review-detail :deep(h3) {
  margin-top: 16px;
  margin-bottom: 8px;
}

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
  background: #fafafa;
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
</style>
