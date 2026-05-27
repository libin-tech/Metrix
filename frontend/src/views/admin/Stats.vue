<template>
  <div class="page-container">
    <div class="page-header">
      <h3 class="page-title"><BarChartOutlined /> 数据统计</h3>
    </div>

    <div class="table-container">
      <div class="toolbar">
        <a-radio-group v-model:value="viewMode" button-style="solid" size="small">
          <a-radio-button value="today">今日</a-radio-button>
          <a-radio-button value="range">日期范围</a-radio-button>
        </a-radio-group>
        <a-range-picker
          v-if="viewMode === 'range'"
          v-model:value="dateRange"
          :disabled-date="disabledDate"
          size="small"
          style="margin-left: 12px;"
          @change="onRangeChange"
        />
        <a-button size="small" type="primary" style="margin-left: 12px;" :loading="loading" @click="loadData">
          <ReloadOutlined /> 刷新
        </a-button>
      </div>

      <a-table
        :dataSource="statsData"
        :columns="columns"
        row-key="userId"
        :loading="loading"
        :pagination="false"
        :locale="{ emptyText: '暂无数据' }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'analysisCount'">
            <a-tag :color="record.analysisCount > 0 ? 'blue' : 'default'">{{ record.analysisCount }}</a-tag>
          </template>
          <template v-if="column.key === 'reviewCount'">
            <a-tag :color="record.reviewCount > 0 ? 'green' : 'default'">{{ record.reviewCount }}</a-tag>
          </template>
          <template v-if="column.key === 'total'">
            <span style="font-weight: 600;">{{ record.total }}</span>
          </template>
        </template>

        <template #summary>
          <a-table-summary fixed>
            <a-table-summary-row>
              <a-table-summary-cell>合计</a-table-summary-cell>
              <a-table-summary-cell>{{ statsData.length }} 人</a-table-summary-cell>
              <a-table-summary-cell>
                <a-tag :color="totalAnalysis > 0 ? 'blue' : 'default'">{{ totalAnalysis }}</a-tag>
              </a-table-summary-cell>
              <a-table-summary-cell>
                <a-tag :color="totalReview > 0 ? 'green' : 'default'">{{ totalReview }}</a-tag>
              </a-table-summary-cell>
              <a-table-summary-cell><span style="font-weight: 600;">{{ totalAll }}</span></a-table-summary-cell>
            </a-table-summary-row>
          </a-table-summary>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { BarChartOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { getTodayStats, getStatsByRange } from '../../api'

const viewMode = ref('today')
const dateRange = ref([])
const loading = ref(false)
const statsData = ref([])

const columns = [
  { title: '统计日期', dataIndex: 'statDate', key: 'statDate', width: 120 },
  { title: '用户', dataIndex: 'username', key: 'username', width: 150 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: '标的评估', key: 'analysisCount', width: 100, align: 'center' },
  { title: '大盘复盘', key: 'reviewCount', width: 100, align: 'center' },
  { title: '合计', key: 'total', width: 80, align: 'center' }
]

const totalAnalysis = computed(() => statsData.value.reduce((s, r) => s + (r.analysisCount || 0), 0))
const totalReview = computed(() => statsData.value.reduce((s, r) => s + (r.reviewCount || 0), 0))
const totalAll = computed(() => totalAnalysis.value + totalReview.value)

const disabledDate = (current) => current && current > dayjs().endOf('day')

const onRangeChange = () => {
  if (dateRange.value && dateRange.value.length === 2) {
    loadData()
  }
}

const loadData = async () => {
  loading.value = true
  try {
    if (viewMode.value === 'range' && dateRange.value && dateRange.value.length === 2) {
      const [start, end] = dateRange.value
      const res = await getStatsByRange(start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD'))
      statsData.value = (res.data || []).map(r => ({ ...r, total: (r.analysisCount || 0) + (r.reviewCount || 0) }))
    } else {
      const res = await getTodayStats()
      statsData.value = (res.data || []).map(r => ({ ...r, total: (r.analysisCount || 0) + (r.reviewCount || 0) }))
    }
  } catch {
    message.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 12px; padding: 20px 24px;
  margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.page-title { margin: 0; font-size: 18px; font-weight: 600; color: #1a1a2e; }
.page-title .anticon { margin-right: 8px; color: #1890ff; }
.table-container {
  background: #fff; border-radius: 12px; padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.toolbar {
  margin-bottom: 16px; display: flex; align-items: center;
}
</style>
