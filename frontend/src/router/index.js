import {createRouter, createWebHistory} from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Landing',
    component: () => import('../views/Landing.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/workspace',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/home',
    redirect: '/workspace'
  },
  {
    path: '/dashboard',
    redirect: '/workspace'
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
  },
  {
    path: '/settings/account-management',
    name: 'AccountManagement',
    component: () => import('../views/AccountManagement.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/portfolio',
    name: 'Portfolio',
    component: () => import('../views/Portfolio.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/Chat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/market-review',
    name: 'MarketReview',
    component: () => import('../views/MarketReview.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/users',
    name: 'UserManagement',
    component: () => import('../views/admin/UserManagement.vue'),
    meta: { requiresAuth: true, adminOnly: true }
  },
  {
    path: '/admin/roles',
    name: 'RoleManagement',
    component: () => import('../views/admin/RoleManagement.vue'),
    meta: { requiresAuth: true, adminOnly: true }
  },
  {
    path: '/admin/menus',
    name: 'MenuManagement',
    component: () => import('../views/admin/MenuManagement.vue'),
    meta: { requiresAuth: true, adminOnly: true }
  },
  {
    path: '/admin/apis',
    name: 'ApiManagement',
    component: () => import('../views/admin/ApiManagement.vue'),
    meta: { requiresAuth: true, adminOnly: true }
  },
  {
    path: '/admin/audit-log',
    name: 'AuditLog',
    component: () => import('../views/admin/AuditLog.vue'),
    meta: { requiresAuth: true, adminOnly: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    next('/login')
  } else if (to.meta.adminOnly && localStorage.getItem('userRole') !== 'ADMIN') {
    next('/workspace')
  } else {
    next()
  }
})

export default router
