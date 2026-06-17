import axios from 'axios'
import {message, Modal} from 'ant-design-vue'
import i18n from '../i18n'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const body = response.data
    if (body && body.code != null && body.code !== 200) {
      if (body.code === 1001) {
        Modal.error({ title: i18n.global.t('common.accountFrozen'), content: body.message || i18n.global.t('common.accountFrozenDefault'), okText: i18n.global.t('common.ok') })
      } else {
        message.error(body.message || '请求失败')
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    if (error.response && error.response.status === 403) {
      message.error(error.response.data?.message || '无权限访问')
    }
    return Promise.reject(error)
  }
)

export const login = data => service.post('/auth/login', data)
export const loginByCode = code => service.post('/auth/login-by-code', { code })
export const verifyCode = code => service.get('/auth/verify-code', { params: { code } })

export const getAnalysisById = id => service.get(`/analysis/${id}`)
export const getAllAnalysis = () => service.get('/analysis')
export const getAnalysisCursor = (cursor, limit = 10) => service.get('/analysis/cursor', { params: { cursor, limit } })
export const createAnalysis = data => service.post('/analysis', data)
export const deleteAnalysis = id => service.delete(`/analysis/${id}`)
export const pushToFeishu = id => service.post(`/analysis/${id}/push-feishu`)
export const exportPdf = id => service.get(`/analysis/${id}/pdf`, { responseType: 'blob' })
export const getAnalysisDetail = id => service.get(`/analysis/${id}/detail`)
export const getQueueStatus = () => service.get('/analysis/queue/status')

export const searchStocks = keyword => service.get('/stocks/search', { params: { keyword } })

export const getAiModelConfigs = () => service.get('/ai-model')
export const createAiModelConfig = data => service.post('/ai-model', data)
export const updateAiModelConfig = (id, data) => service.put(`/ai-model/${id}`, data)
export const deleteAiModelConfig = id => service.delete(`/ai-model/${id}`)

export const getNotificationConfigs = () => service.get('/notification')
export const createNotificationConfig = data => service.post('/notification', data)
export const updateNotificationConfig = (id, data) => service.put(`/notification/${id}`, data)
export const deleteNotificationConfig = id => service.delete(`/notification/${id}`)

export const getNewsSourceConfigs = () => service.get('/news-source')
export const createNewsSourceConfig = data => service.post('/news-source', data)
export const updateNewsSourceConfig = (id, data) => service.put(`/news-source/${id}`, data)
export const deleteNewsSourceConfig = id => service.delete(`/news-source/${id}`)

export const getMarketDataConfigs = () => service.get('/market-data')
export const createMarketDataConfig = data => service.post('/market-data', data)
export const updateMarketDataConfig = (id, data) => service.put(`/market-data/${id}`, data)
export const deleteMarketDataConfig = id => service.delete(`/market-data/${id}`)

export const testAiModelConfig = data => service.post('/ai-model/test', data)

export const getStockBasicPage = (keyword, page, size) => service.get('/stock-basic/page', { params: { keyword, page, size } })
export const getBrokerAccounts = () => service.get('/portfolio/accounts')
export const createBrokerAccount = data => service.post('/portfolio/accounts', data)
export const updateBrokerAccount = (id, data) => service.put(`/portfolio/accounts/${id}`, data)
export const deleteBrokerAccount = id => service.delete(`/portfolio/accounts/${id}`)

export const getPortfolioHoldings = (keyword, accountId) => service.get('/portfolio/holdings', { params: { keyword, accountId } })
export const createPortfolioHolding = data => service.post('/portfolio/holdings', data)
export const batchCreatePortfolioHolding = (accountId, items) => service.post('/portfolio/holdings/batch', items, { params: { accountId } })
export const deletePortfolioHolding = id => service.delete(`/portfolio/holdings/${id}`)
export const refreshPortfolioPrices = () => service.post('/portfolio/holdings/refresh-prices')
export const pollRefreshedPrices = (ids) => service.post('/portfolio/holdings/poll-refreshed', ids)

export const createChatSession = data => service.post('/chat/session', data)
export const listChatSessions = () => service.get('/chat/sessions')
export const deleteChatSession = id => service.delete(`/chat/session/${id}`)
export const getChatSessionMessages = id => service.get(`/chat/session/${id}/messages`)

export const deleteChatSessions = (ids) => service.post('/chat/sessions/delete', ids)

export const sendChatMessage = async (sessionId, content, callbacks) => {
  const token = localStorage.getItem('token')
  const { onThinking, onReport, onDone, onError, onStep } = callbacks
  try {
    const response = await fetch('/api/chat/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ sessionId, content })
    })
    if (!response.ok) {
      const err = await response.text()
      onError && onError(err)
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''

      for (const part of parts) {
        const lines = part.split('\n')
        let eventType = 'report'
        let data = ''
        for (const line of lines) {
          if (line.startsWith('id:')) {
            // ignore id field
          } else if (line.startsWith('event:')) {
            eventType = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            data = line.slice(5).replace(/^ /, '')
          }
        }
        if (eventType === 'done') {
          onDone && onDone(data ? JSON.parse(data) : {})
        } else if (eventType === 'error') {
          onError && onError(data)
        } else if (eventType === 'step') {
          onStep && onStep(data)
        } else if (eventType === 'thinking') {
          onThinking && onThinking(data)
        } else if (eventType === 'report' || eventType === 'message' || eventType === '') {
          onReport && onReport(data)
        }
      }
    }
  } catch (err) {
    onError && onError(err.message || '连接失败')
  }
}

export const getMarketReviews = () => service.get('/market-review')
export const getMarketReviewCursor = (cursor, limit = 10) => service.get('/market-review/cursor', { params: { cursor, limit } })
export const getMarketReviewDetail = (id) => service.get(`/market-review/${id}`)
export const deleteMarketReview = (id) => service.delete(`/market-review/${id}`)
export const triggerMarketReview = () => service.post('/market-review/trigger')
export const createMarketReview = (reviewDate) => service.post('/market-review/create', null, { params: { reviewDate } })

export const getMarketActivity = () => service.get('/market-activity')
export const getMarketIndex = () => service.get('/market-index')

export const getAdminUsers = (page, size, keyword) => service.get('/admin/users', { params: { page, size, keyword } })
export const freezeUser = (id, data) => service.put(`/admin/users/${id}/freeze`, data)
export const unfreezeUser = (id) => service.put(`/admin/users/${id}/unfreeze`)

export const getRolePage = (page, size, keyword) => service.get('/admin/roles', { params: { page, size, keyword } })
export const getRoleDetail = (id) => service.get(`/admin/roles/${id}`)
export const createRole = (data) => service.post('/admin/roles', data)
export const updateRole = (id, data) => service.put(`/admin/roles/${id}`, data)
export const deleteRole = (id) => service.delete(`/admin/roles/${id}`)
export const getAllRoles = () => service.get('/admin/roles/list-all')
export const assignRoleMenus = (roleId, data) => service.post(`/admin/roles/${roleId}/menus`, data)
export const assignRoleApis = (roleId, data) => service.post(`/admin/roles/${roleId}/apis`, data)
export const getRoleMenus = (roleId) => service.get(`/admin/roles/${roleId}/menus`)
export const getRoleApis = (roleId) => service.get(`/admin/roles/${roleId}/apis`)

export const getMenuTree = () => service.get('/admin/menus/tree')
export const getMenuById = (id) => service.get(`/admin/menus/${id}`)
export const createMenu = (data) => service.post('/admin/menus', data)
export const updateMenu = (id, data) => service.put(`/admin/menus/${id}`, data)
export const deleteMenu = (id) => service.delete(`/admin/menus/${id}`)
export const getMenuApis = (menuId) => service.get(`/admin/menus/${menuId}/apis`)
export const assignMenuApis = (menuId, data) => service.post(`/admin/menus/${menuId}/apis`, data)

export const getApiPage = (page, size, keyword) => service.get('/admin/apis', { params: { page, size, keyword } })
export const getApiById = (id) => service.get(`/admin/apis/${id}`)
export const createApi = (data) => service.post('/admin/apis', data)
export const updateApi = (id, data) => service.put(`/admin/apis/${id}`, data)
export const deleteApi = (id) => service.delete(`/admin/apis/${id}`)
export const getAllApis = () => service.get('/admin/apis/list-all')

export const getUserRoles = (userId) => service.get(`/admin/users/${userId}/roles`)
export const assignUserRoles = (userId, data) => service.post(`/admin/users/${userId}/roles`, data)

export const getPermissions = () => service.get('/auth/permissions')

export const getCurrentUser = () => service.get('/auth/me')


export const getAuditLogs = (page, size, userId, action, startTime, endTime) =>
  service.get('/admin/audit-logs', { params: { page, size, userId, action, startTime, endTime } })