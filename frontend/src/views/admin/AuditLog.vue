<template>
  <div class="page-container">
    <div class="page-header">
      <h3 class="page-title"><FileTextOutlined /> 审计日志</h3>
    </div>

    <div class="table-container">
      <div class="toolbar">
        <a-space>
          <a-input
            v-model:value="filters.action"
            placeholder="操作类型"
            style="width: 160px;"
            allow-clear
            @press-enter="loadData"
          />
          <a-range-picker
            v-model:value="dateRange"
            show-time
            format="YYYY-MM-DD HH:mm"
            :placeholder="['开始时间', '结束时间']"
            style="width: 340px;"
          />
          <a-button type="primary" size="small" :loading="loading" @click="loadData">
            <ReloadOutlined /> 查询
          </a-button>
        </a-space>
      </div>

      <a-table
        :dataSource="logs"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :pagination="{ current: page, pageSize: size, total, showSizeChanger: true, showTotal: t => `共 ${t} 条` }"
        @change="handleTableChange"
        :locale="{ emptyText: '暂无数据' }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-tag :color="actionColor(record.action)">{{ record.action }}</a-tag>
          </template>
          <template v-if="column.key === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-if="column.key === 'detail'">
            <a-typography-text
              :content="record.detail || '-'"
              :ellipsis="{ tooltip: true }"
              style="max-width: 200px;"
            />
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import {onMounted, reactive, ref} from 'vue'
import dayjs from 'dayjs'
import {message} from 'ant-design-vue'
import {FileTextOutlined, ReloadOutlined} from '@ant-design/icons-vue'
import {getAuditLogs} from '../../api'

const loading = ref(false)
const logs = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dateRange = ref([])

const filters = reactive({
  userId: null,
  action: '',
  startTime: '',
  endTime: ''
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '用户', dataIndex: 'username', key: 'username', width: 100 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 120 },
  { title: '资源类型', dataIndex: 'resourceType', key: 'resourceType', width: 100 },
  { title: '资源ID', dataIndex: 'resourceId', key: 'resourceId', width: 80 },
  { title: '详情', dataIndex: 'detail', key: 'detail' },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 130 },
  { title: '操作时间', dataIndex: 'createTime', key: 'createTime', width: 170 }
]

const actionColor = (action) => {
  const map = {
    '创建': 'green', '新增': 'green', '添加': 'green',
    '更新': 'blue', '编辑': 'blue', '修改': 'blue',
    '删除': 'red', '移除': 'red',
    '冻结': 'orange', '解冻': 'cyan',
    '登录': 'purple', '登录失败': 'magenta', '退出': 'default'
  }
  for (const [key, color] of Object.entries(map)) {
    if (action && action.includes(key)) return color
  }
  return 'default'
}

const handleTableChange = (pag) => {
  page.value = pag.current
  size.value = pag.pageSize
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      filters.startTime = dateRange.value[0].format('YYYY-MM-DD HH:mm:ss')
      filters.endTime = dateRange.value[1].format('YYYY-MM-DD HH:mm:ss')
    } else {
      filters.startTime = ''
      filters.endTime = ''
    }
    const res = await getAuditLogs(
      page.value, size.value,
      filters.userId || undefined,
      filters.action || undefined,
      filters.startTime || undefined,
      filters.endTime || undefined
    )
    logs.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    message.error('加载审计日志失败')
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
