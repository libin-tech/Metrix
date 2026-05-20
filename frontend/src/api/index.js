import axios from 'axios'

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
    return response.data
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const login = data => service.post('/auth/login', data)

export const getAnalysisById = id => service.get(`/analysis/${id}`)
export const getAllAnalysis = () => service.get('/analysis')
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

export const getMarketReviews = () => service.get('/market-review')
export const getMarketReviewDetail = (id) => service.get(`/market-review/${id}`)
export const deleteMarketReview = (id) => service.delete(`/market-review/${id}`)
export const triggerMarketReview = () => service.post('/market-review/trigger')
export const createMarketReview = (reviewDate) => service.post('/market-review/create', null, { params: { reviewDate } })