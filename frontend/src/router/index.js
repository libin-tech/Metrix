import {createRouter, createWebHistory} from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    name: 'HomeIndex',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/analysis',
    name: 'Analysis',
    component: () => import('../views/Analysis.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings/ai-model',
    name: 'AiModelConfig',
    component: () => import('../views/AiModelConfig.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings/notification',
    name: 'NotificationConfig',
    component: () => import('../views/NotificationConfig.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings/news-source',
    name: 'NewsSourceConfig',
    component: () => import('../views/NewsSourceConfig.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings/market-data',
    name: 'MarketDataConfig',
    component: () => import('../views/MarketDataConfig.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings/stock-basic',
    name: 'StockBasic',
    component: () => import('../views/StockBasic.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    next('/login')
  } else {
    next()
  }
})

export default router