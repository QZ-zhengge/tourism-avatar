import { createRouter, createWebHistory } from 'vue-router'

// 懒加载所有页面组件（生产环境自动代码分割）
const LoginView      = () => import('../views/login/LoginView.vue')
const DashboardView  = () => import('../views/dashboard/DashboardView.vue')
const DigitalView    = () => import('../views/digital/DigitalView.vue')
const DataView       = () => import('../views/data/DataView.vue')
const ContentView    = () => import('../views/content/ContentView.vue')
const KnowledgeView  = () => import('../views/knowledge/KnowledgeView.vue')
const KG3DView       = () => import('../views/kg3d/KG3DView.vue')
const DialogueView   = () => import('../views/dialogue/DialogueView.vue')
const SettingsView   = () => import('../views/settings/SettingsView.vue')

const routes = [
  { path: '/login', name: 'login', component: LoginView },
  { path: '/', redirect: '/login' },
  { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
  { path: '/digital',   name: 'digital',   component: DigitalView, meta: { requiresAuth: true } },
  { path: '/data',      name: 'data',       component: DataView, meta: { requiresAuth: true } },
  { path: '/content',   name: 'content',    component: ContentView, meta: { requiresAuth: true } },
  { path: '/knowledge', name: 'knowledge',  component: KnowledgeView, meta: { requiresAuth: true } },
  { path: '/kg3d',      name: 'kg3d',       component: KG3DView, meta: { requiresAuth: true } },
  { path: '/dialogue',  name: 'dialogue',   component: DialogueView, meta: { requiresAuth: true } },
  { path: '/settings',  name: 'settings',   component: SettingsView, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录时跳转到登录页
router.beforeEach((to, from, next) => {
  const isLoggedIn = localStorage.getItem('user_token')
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && isLoggedIn) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
