<template>

  <!-- 登录页：完全独立，不渲染任何管理平台布局 -->
  <template v-if="route.path === '/login'">
    <RouterView />
  </template>

  <!-- 管理平台布局 -->
  <template v-else>
    <!-- 顶部导航 -->
    <header class="topbar">
    <div class="topbar-title"><span>◈</span> 景区数字人管理平台</div>
    <nav class="topbar-nav">
      <router-link to="/dashboard" :class="{active: $route.path==='/dashboard'}">数据大屏</router-link>
      <router-link to="/digital" :class="{active: $route.path==='/digital'}">数字人管理</router-link>
      <router-link to="/data" :class="{active: $route.path==='/data'}">数据分析</router-link>
      <router-link to="/content" :class="{active: $route.path==='/content'}">内容管理</router-link>
      <router-link to="/knowledge" :class="{active: $route.path==='/knowledge'}">知识库</router-link>
      <router-link to="/dialogue" :class="{active: $route.path==='/dialogue'}">对话分析</router-link>
      <router-link to="/settings" :class="{active: $route.path==='/settings'}">系统设置</router-link>
    </nav>
    <div class="topbar-right">
      <!-- 信箱：查看用户建议 -->
      <div class="mailbox-trigger" @click="toggleMailbox" title="用户建议">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
          <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
          <polyline points="22,6 12,13 2,6"/>
        </svg>
        <span class="mailbox-badge" v-if="suggestionUnread > 0">{{ suggestionUnread }}</span>
      </div>
      <div class="topbar-time">{{ currentTime }}</div>
      <!-- 用户信息 / 退出 -->
      <div class="user-menu" @click="showUserMenu = !showUserMenu">
        <div class="user-avatar">{{ userInitial }}</div>
        <div class="user-dropdown" v-if="showUserMenu" @click.stop>
          <div class="user-dropdown-header">
            <div class="user-avatar-lg">{{ userInitial }}</div>
            <div>
              <div class="user-name">{{ userName }}</div>
              <div class="user-email">{{ userEmail }}</div>
            </div>
          </div>
          <div class="user-dropdown-divider"></div>
          <button class="user-dropdown-item" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            退出登录
          </button>
        </div>
      </div>
    </div>
  </header>

  <!-- 用户建议面板 -->
  <transition name="fade">
    <div class="mailbox-overlay" v-if="mailboxOpen" @click.self="mailboxOpen = false">
      <div class="mailbox-panel">
        <div class="mailbox-panel-header">
          <span class="mailbox-panel-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15" style="vertical-align:-2px;margin-right:6px">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
              <polyline points="22,6 12,13 2,6"/>
            </svg>
            用户建议
          </span>
          <span class="mailbox-count" v-if="suggestions.length">共 {{ suggestions.length }} 条</span>
          <button class="mailbox-close" @click="mailboxOpen = false">✕</button>
        </div>
        <div class="mailbox-loading" v-if="suggestionLoading">
          <div class="spinner"></div>
        </div>
        <div class="mailbox-list" v-else-if="suggestions.length > 0">
          <div
            v-for="s in suggestions"
            :key="s.id"
            class="mailbox-item"
            :class="{ unread: s.status === 'unread' }"
            @click="markAsRead(s)"
          >
            <div class="mailbox-item-left">
              <div class="mailbox-item-dot" v-if="s.status === 'unread'"></div>
            </div>
            <div class="mailbox-item-body">
              <div class="mailbox-item-user">{{ s.user_name || '匿名用户' }}</div>
              <div class="mailbox-item-text">{{ s.content }}</div>
              <div class="mailbox-item-time">{{ mailboxFormatTime(s.created_at) }}</div>
            </div>
          </div>
        </div>
        <div class="mailbox-empty" v-else>
          <svg viewBox="0 0 24 24" fill="none" stroke="var(--text-dim)" stroke-width="1.5" width="36" height="36" style="opacity:0.3">
            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
            <polyline points="22,6 12,13 2,6"/>
          </svg>
          <div style="margin-top:8px;font-size:13px;color:var(--text-dim)">暂无用户建议</div>
        </div>
      </div>
    </div>
  </transition>

  <div class="main">
    <!-- 左侧边栏 -->
    <aside class="sidebar" :class="{ 'overlay-show': sidebarOverlay }">
      <div class="sidebar-section">
        <div class="sidebar-section-title">数据中心</div>
        <router-link class="sidebar-link" to="/dashboard" :class="{active: $route.path==='/dashboard'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
          数据大屏
        </router-link>
        <router-link class="sidebar-link" to="/data" :class="{active: $route.path==='/data'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
          数据分析
        </router-link>
      </div>
      <div class="sidebar-section">
        <div class="sidebar-section-title">内容管理</div>
        <router-link class="sidebar-link" to="/digital" :class="{active: $route.path==='/digital'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M6 21v-2a4 4 0 014-4h4a4 4 0 014 4v2"/></svg>
          数字人管理
        </router-link>
        <router-link class="sidebar-link" to="/content" :class="{active: $route.path==='/content'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          内容管理
        </router-link>
        <router-link class="sidebar-link" to="/knowledge" :class="{active: $route.path==='/knowledge'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z"/><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z"/></svg>
          知识库
        </router-link>
        <router-link class="sidebar-link" to="/kg3d" :class="{active: $route.path==='/kg3d'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>
          知识图谱
        </router-link>
        <router-link class="sidebar-link" to="/dialogue" :class="{active: $route.path==='/dialogue'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          对话分析
        </router-link>
      </div>
      <div class="sidebar-section">
        <div class="sidebar-section-title">系统</div>
        <router-link class="sidebar-link" to="/settings" :class="{active: $route.path==='/settings'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>
          系统设置
        </router-link>
      </div>
    </aside>

    <!-- 路由视图 -->
    <div class="content">
      <RouterView />
    </div>
  </div>
  </template>

  <!-- Toast（登录页也需要） -->
  <div class="toast" :class="[toast.type, {show: toast.visible}]">{{ toast.message }}</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, provide, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { marked } from 'marked'
import { getAuthHeaders } from './utils/api.js'

const route = useRoute()
const router = useRouter()

// ============================================================
// 讯飞官方数字人形象列表
// ============================================================
const AVATAR_LIST = [
  { id: '118801001', name: '依丹',   emoji: '👩', gender: '女', online: true,  bgColor: '#e879f9', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/Photorealistic_2D_digital_avat_2026-06-13T05-06-59.png', icon: '/images/avatars/Photorealistic_2D_digital_avat_2026-06-13T05-06-59.png', photoLoaded: true, photoError: false },
  { id: '110026010', name: '伊凡',   emoji: '👨', gender: '男', online: true,  bgColor: '#60a5fa', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/Photorealistic_2D_digital_avat_2026-06-13T05-07-23.png', icon: '/images/avatars/Photorealistic_2D_digital_avat_2026-06-13T05-07-23.png', photoLoaded: true, photoError: false },
  { id: '110022010', name: '晓云',   emoji: '👩', gender: '女', online: true,  bgColor: '#f472b6', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/Photorealistic_2D_digital_avat_2026-06-13T05-07-47.png', icon: '/images/avatars/Photorealistic_2D_digital_avat_2026-06-13T05-07-47.png', photoLoaded: true, photoError: false },
  { id: '110017006', name: '马可',   emoji: '👨', gender: '男', online: true,  bgColor: '#34d399', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/Photorealistic_2D_digital_avat_2026-06-13T05-08-11.png', icon: '/images/avatars/Photorealistic_2D_digital_avat_2026-06-13T05-08-11.png', photoLoaded: true, photoError: false },
  { id: '110005018', name: '知新',   emoji: '👨', gender: '男', online: true,  bgColor: '#a78bfa', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-03-40.png', icon: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-03-40.png', photoLoaded: true, photoError: false },
  { id: '110029003', name: '晓姿',   emoji: '👩', gender: '女', online: true,  bgColor: '#f9a8d4', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-04-05.png', icon: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-04-05.png', photoLoaded: true, photoError: false },
  { id: '110029002', name: '晓姿',   emoji: '👩', gender: '女', online: true,  bgColor: '#fde68a', textColor: '#333', tag: '', iconError: false, photoUrl: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-04-29.png', icon: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-04-29.png', photoLoaded: true, photoError: false },
  { id: '110021007', name: '晓娴',   emoji: '👩', gender: '女', online: true,  bgColor: '#1e293b', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-04-53.png', icon: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-04-53.png', photoLoaded: true, photoError: false },
  { id: '110021006', name: '晓娴',   emoji: '👩', gender: '女', online: true,  bgColor: '#94a3b8', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-05-17.png', icon: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-05-17.png', photoLoaded: true, photoError: false },
  { id: '110026013', name: '伊凡',   emoji: '👨', gender: '男', online: true,  bgColor: '#3b82f6', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-05-40.png', icon: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-05-40.png', photoLoaded: true, photoError: false },
  { id: '110023004', name: '小雅',   emoji: '👩', gender: '女', online: true,  bgColor: '#fb923c', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-06-05.png', icon: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-06-05.png', photoLoaded: true, photoError: false },
  { id: '111260002', name: '雨菲',   emoji: '👩', gender: '女', online: true,  bgColor: '#06b6d4', textColor: '#fff', tag: '', iconError: false, photoUrl: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-06-29.png', icon: '/images/avatars/A_high_quality_photorealistic__2026-06-13T05-06-29.png', photoLoaded: true, photoError: false },
]

const VCN_GROUPS = [
  {
    label: '中文普通话 · 交互',
    voices: [
      { id: 'x4_xiaoxuan',    name: '小璇',    gender: '女' },
      { id: 'x4_panting',     name: '潘婷',    gender: '女' },
      { id: 'x4_yezi',        name: '小露',    gender: '女' },
      { id: 'x4_xiaoguo',    name: '小果',    gender: '女' },
      { id: 'x4_yifei',       name: '一菲',    gender: '女' },
      { id: 'x4_mingge',      name: '明哥',    gender: '男' },
      { id: 'x4_chaoge',      name: '超哥',    gender: '男' },
      { id: 'x4_lingxiaoyu_assist',  name: '聆小瑜', gender: '女' },
      { id: 'x4_lingxiaoying_assist', name: '聆小璎', gender: '女' },
      { id: 'x4_lingxiaoxuan_assist', name: '聆小璇', gender: '女' },
      { id: 'x4_lingxiaoyao_assist',  name: '聆小瑶', gender: '女' },
      { id: 'x4_lingfeizhe_assist',   name: '聆飞哲', gender: '男' },
      { id: 'x4_lingfeichen_assist',  name: '聆飞晨', gender: '男' },
      { id: 'x4_lingfeichen_emo',     name: '聆飞晨(情感)', gender: '男' },
    ]
  },
  {
    label: '中文普通话 · 通用/阅读',
    voices: [
      { id: 'x4_lingyuyan',           name: '聆玉言',      gender: '女' },
      { id: 'x4_lingxiaoshan_profnews', name: '聆小珊',   gender: '女' },
      { id: 'x4_lingxiaolu_assist',  name: '聆小璐',      gender: '女' },
      { id: 'x4_lingxiaozhen_eclives', name: '聆小瑧',    gender: '女' },
      { id: 'x4_xiaozhong',          name: '小忠',        gender: '男' },
    ]
  },
  {
    label: '美式英语',
    voices: [
      { id: 'x4_EnUs_Luna_assist',      name: 'Luna',      gender: '女' },
      { id: 'x4_EnUs_Gavin_assist',     name: 'Gavin',      gender: '男' },
      { id: 'x4_EnUs_Laura_education',  name: 'Laura',     gender: '女' },
      { id: 'x4_EnUs_Catherine_profnews', name: 'Catherine', gender: '女' },
      { id: 'x4_EnUs_Lindsay_assist',   name: 'Lindsay',    gender: '女' },
      { id: 'x4_EnUs_Lucy_education',   name: 'Lucy',      gender: '女' },
      { id: 'x4_EnUs_Ryan_assist',      name: 'Ryan',      gender: '男' },
    ]
  },
  {
    label: '英式英语',
    voices: [
      { id: 'x4_EnUk_Amanda_education', name: 'Amanda',    gender: '女' },
      { id: 'x4_EnUk_Ashleigh_assist',  name: 'Ashleigh',  gender: '女' },
    ]
  },
]

const MAP_DATA = []
const CITY_SCATTER_DATA = []

const API_BASE = '/api/v1'

async function fetchAPI(path) {
  try {
    const url = '/api/v1/admin/dashboard' + path
    const res = await fetch(url, {
      signal: AbortSignal.timeout(10000),
      headers: getAuthHeaders()
    })
    if (!res.ok) return null
    return await res.json()
  } catch(e) {
    console.warn('API fetch failed:', path, e.message)
    return null
  }
}

async function fetchAllDashboardData() {
  const [overview, trend, hotQ, sentimentDist, visitorInsights, visitorRadar, questionCategories, realtimeStats, suggestionsData] = await Promise.all([
    fetchAPI('/overview'),
    fetchAPI('/sentiment-trend?period=week&granularity=hour'),
    fetchAPI('/hot-questions?period=week&top_n=10'),
    fetchAPI('/sentiment-distribution'),
    fetchAPI('/visitor-insights'),
    fetchAPI('/visitor-radar'),
    fetchAPI('/question-categories'),
    fetchAPI('/realtime-stats'),
    fetchAPI('/improvement-suggestions'),
  ])
  return { overview, trend, hotQ, sentimentDist, visitorInsights, visitorRadar, questionCategories, realtimeStats, suggestions: suggestionsData }
}

// ============================================================
// 响应式状态
// ============================================================
const currentTime = ref('')
const timer = ref(null)
const dashboardTimer = ref(null)
const toast = reactive({ visible: false, message: '', type: 'success' })
const sidebarOverlay = ref(false)

// 用户菜单
const showUserMenu = ref(false)
const userName = ref(localStorage.getItem('user_name') || '管理员')
const userEmail = ref(localStorage.getItem('user_email') || 'user@example.com')
const userInitial = computed(() => {
  const name = userName.value
  return name ? name.charAt(0).toUpperCase() : 'A'
})

function handleLogout() {
  localStorage.removeItem('user_token')
  localStorage.removeItem('user_email')
  localStorage.removeItem('user_name')
  showUserMenu.value = false
  router.push('/login')
}

// 点击外部关闭用户菜单
function _closeUserMenu(e) {
  if (!e.target.closest('.user-menu')) showUserMenu.value = false
}

// 数字人管理
const avatarList = ref(AVATAR_LIST)
const selectedAvatarId = ref('111260002')
const vcnGroups = ref(VCN_GROUPS)
const voiceConfig = reactive({ vcn: 'x4_xiaoxuan', speed: 50, pitch: 50, volume: 50 })
const sceneConfig = reactive({ serviceId: '315837983779590144', layout: 'fullscreen', displayMode: 'full' })
const isSaving = ref(false)
const syncStatus = ref('saved')
const lastUpdateTime = ref('--')
const showPhotoTips = ref(false)

// 仪表盘
const kpiAll = ref([
  { label: '总会话数', value: '--', unit: '次', color: '#3b82f6' },
  { label: '总轮次', value: '--', unit: '轮', color: '#60a5fa' },
  { label: '满意率', value: '--', unit: '%', color: '#10b981' },
  { label: '不满意率', value: '--', unit: '%', color: '#ef4444' },
  { label: '平均满意度', value: '--', unit: '分', color: '#f59e0b' },
  { label: '在线会话', value: '--', unit: '个', color: '#06b6d4' },
  { label: '今日提问', value: '--', unit: '条', color: '#8b5cf6' },
  { label: '今日访客', value: '--', unit: '人', color: '#ec4899' },
  { label: '负面预警', value: '--', unit: '条', color: '#ef4444' },
  { label: '响应时长', value: '--', unit: 's', color: '#14b8a6' },
])
const hotQuestions = ref([])
const apiCache = reactive({ overview: null, trend: null, hotQ: null, sentimentDist: null, visitorInsights: null, visitorRadar: null, questionCategories: null, realtimeStats: null })
const mapLoading = ref(true)

// 内容管理
const cmSpotName = ref('')
const cmFileName = ref('')
const cmFileData = ref(null)
const cmPreviewUrl = ref('')
const cmShowPreview = ref(false)
const cmImages = ref(JSON.parse(localStorage.getItem('cm_images') || '[]'))
const cmCanAdd = computed(() => cmSpotName.value.trim() && cmFileData.value)

// 知识库
const docTypeOptions = [
  { value: 'guide', label: '📋 导游指南' },
  { value: 'history', label: '📜 历史文化' },
  { value: 'faq', label: '❓ 常见问答' },
  { value: 'notice', label: '📢 景区公告' },
]
const newDocType = ref('')
const selectedDocId = ref('')
const fileName = ref('')
const previewing = ref(false)
const previewDone = ref(false)
const previewChunks = ref([])
const previewFileName = ref('')
const previewFileData = ref(null)
const uploadError = ref('')
const knoSearch = ref('')
const knoTypeFilter = ref('')
const knoExtFilter = ref('')
const docList = ref([])

const KNO_PREVIEW_URL = `${API_BASE}/admin/knowledge/preview`
const KNO_CONFIRM_URL = `${API_BASE}/admin/knowledge/confirm`
const KNO_UPLOAD_URL  = `${API_BASE}/guide/knowledge/upload`

function getExt(name) { const m = name.match(/\.(\w+)$/); return m ? m[1].toLowerCase() : '' }
const docExtOptions = computed(() => {
  const exts = new Set()
  docList.value.forEach(d => { const e = getExt(d.name); if (e) exts.add(e) })
  return [...exts].sort()
})
const previewTotalChars = computed(() => previewChunks.value.reduce((s, c) => s + (c.length || 0), 0))
const selectedDoc = computed(() => docList.value.find(d => d.id === selectedDocId.value) || null)
const canUpload = computed(() => newDocType.value && fileName.value)
const totalChars = computed(() => {
  if (!selectedDoc.value) return 0
  return selectedDoc.value.chunks.reduce((s, c) => s + (c.length || 0), 0)
})
const filteredDocs = computed(() => {
  let list = docList.value
  if (knoSearch.value) {
    const q = knoSearch.value.toLowerCase()
    list = list.filter(d => d.name.toLowerCase().includes(q))
  }
  if (knoTypeFilter.value) list = list.filter(d => d.type === knoTypeFilter.value)
  if (knoExtFilter.value) list = list.filter(d => getExt(d.name) === knoExtFilter.value)
  return list
})
function docIconBg(type) {
  const map = { guide: 'rgba(6,182,212,0.7)', history: 'rgba(245,158,11,0.7)', faq: 'rgba(139,92,246,0.7)', notice: 'rgba(239,68,68,0.7)' }
  return map[type] || 'rgba(100,116,139,0.5)'
}

// 对话分析
const dlgSearch = ref('')
const selectedUserId = ref('')
const dlgUsers = ref([])
const dlgLoading = ref(true)
const dlgError = ref('')
const selectedUser = computed(() => dlgUsers.value.find(u => u.id === selectedUserId.value) || {})
const filteredUsers = computed(() => {
  if (!dlgSearch.value) return dlgUsers.value
  const q = dlgSearch.value.toLowerCase()
  return dlgUsers.value.filter(u => u.id.toLowerCase().includes(q) || u.name.toLowerCase().includes(q))
})

// 数据分析
const analysisItems = ref([])
const analysisLoaded = ref(false)
const expandedSet = ref(new Set())
const urgentItems = computed(() => analysisItems.value.filter(i => i.type === '紧急问题'))
const improveItems = computed(() => analysisItems.value.filter(i => i.type === '服务改进'))
const optimizeItems = computed(() => analysisItems.value.filter(i => i.type === '优化建议'))
const keepItems = computed(() => analysisItems.value.filter(i => i.type === '正面保持'))

const topProvinces = computed(() => [...MAP_DATA].sort((a, b) => b.value - a.value).slice(0, 5))

const currentAvatar = computed(() => {
  let av = avatarList.value.find(a => a.id === selectedAvatarId.value)
  if (!av) {
    const nameMap = { '111260002': { name: '雨菲', emoji: '👩', gender: '女', bgColor: '#06b6d4', textColor: '#fff' } }
    av = { id: selectedAvatarId.value, iconError: true, photoError: true, photoLoaded: true, ...(nameMap[selectedAvatarId.value] || { name: '未知', emoji: '❓', gender: '?', bgColor: '#64748b', textColor: '#fff' }) }
  }
  return av
})
const selectedAvatarData = computed(() => avatarList.value.find(a => a.id === selectedAvatarId.value) || null)

// ECharts instances
let chartSentiment = null, chartEmotion = null, chartVisitor = null, chartCategory = null, chartMap = null, chartHotBar = null, chartRadar = null

// ============================================================
// 方法
// ============================================================
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { hour12: false })
}
function showToast(msg, type = 'success') {
  toast.message = msg; toast.type = type; toast.visible = true
  setTimeout(() => { toast.visible = false }, 2500)
}
function toggleSidebar() { sidebarOverlay.value = !sidebarOverlay.value }

// 数字人管理方法
function selectAvatar(av) {
  selectedAvatarId.value = av.id
  if (av) { av.photoLoaded = false; av.photoError = false }
}
function refreshPhoto() {
  const av = avatarList.value.find(a => a.id === selectedAvatarId.value)
  if (av) {
    av.photoLoaded = false; av.photoError = false
    setTimeout(() => { av.photoLoaded = true }, 500)
  }
}
async function saveConfig() {
  isSaving.value = true; syncStatus.value = 'saving'
  const config = {
    avatarId: selectedAvatarId.value, vcn: voiceConfig.vcn,
    speed: parseInt(voiceConfig.speed), pitch: parseInt(voiceConfig.pitch), volume: parseInt(voiceConfig.volume),
    serviceId: sceneConfig.serviceId, layout: sceneConfig.layout, displayMode: sceneConfig.displayMode,
    savedAt: new Date().toISOString(),
  }
  try {
    // 调用后端 API 保存配置
    const res = await fetch('/api/v1/admin/digital/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify({
        scenic_id: 'lingshan',
        avatar_id: selectedAvatarId.value,
        vcn: voiceConfig.vcn,
        speed: parseInt(voiceConfig.speed),
        pitch: parseInt(voiceConfig.pitch),
        volume: parseInt(voiceConfig.volume),
        service_id: sceneConfig.serviceId,
        layout: sceneConfig.layout,
        display_mode: sceneConfig.displayMode,
      }),
      signal: AbortSignal.timeout(10000),
    })
    const data = await res.json().catch(() => null)
    if (res.ok && data && (data.code === 200 || data.code === 0)) {
      localStorage.setItem('dh_sync_config', JSON.stringify(config))
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
      syncStatus.value = 'saved'
      showToast('配置已保存到服务器，游客端将在5秒后自动同步', 'success')
      isSaving.value = false; return
    }
    // API 不可用，降级到 localStorage
    localStorage.setItem('dh_sync_config', JSON.stringify(config))
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    syncStatus.value = 'saved'
    showToast('配置已本地保存（服务器暂不可用）', 'success')
  } catch(e) {
    localStorage.setItem('dh_sync_config', JSON.stringify(config))
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    syncStatus.value = 'saved'
    showToast('配置已保存到本地', 'success')
  }
  isSaving.value = false
}
async function loadDigitalConfig() {
  try {
    const res = await fetch('/api/v1/admin/digital/config?scenic_id=lingshan', {
      headers: getAuthHeaders(),
      signal: AbortSignal.timeout(8000),
    })
    if (!res.ok) return
    const data = await res.json().catch(() => null)
    const cfg = data?.data || data // 兼容直接返回或 {code,data} 格式
    if (!cfg || !cfg.avatar_id) return
    selectedAvatarId.value = cfg.avatar_id || selectedAvatarId.value
    voiceConfig.vcn = cfg.vcn || voiceConfig.vcn
    voiceConfig.speed = cfg.speed ?? voiceConfig.speed
    voiceConfig.pitch = cfg.pitch ?? voiceConfig.pitch
    voiceConfig.volume = cfg.volume ?? voiceConfig.volume
    sceneConfig.serviceId = cfg.service_id || sceneConfig.serviceId
    sceneConfig.layout = cfg.layout || sceneConfig.layout
    sceneConfig.displayMode = cfg.display_mode || sceneConfig.displayMode
    // 如果后端返回了 base64 头像，更新 avatarList 中对应项
    if (cfg.avatar_base64) {
      const av = avatarList.value.find(a => a.id === cfg.avatar_id)
      if (av) { av.icon = cfg.avatar_base64; av.photoUrl = cfg.avatar_base64 }
    }
  } catch(e) { console.log('Digital config API not available, using defaults') }
}
async function loadVCNVoices() {
  try {
    const res = await fetch('/api/v1/admin/vcn/voices', {
      headers: getAuthHeaders(),
      signal: AbortSignal.timeout(8000),
    })
    if (!res.ok) return
    const resp = await res.json().catch(() => null)
    const items = resp?.data || resp?.items || []
    if (!Array.isArray(items) || items.length === 0) return
    // 按 category 分组
    const groupMap = {}
    items.forEach(v => {
      const cat = v.category || '其他'
      if (!groupMap[cat]) groupMap[cat] = []
      groupMap[cat].push({ id: v.vcn || v.id, name: v.name, gender: v.gender || '' })
    })
    vcnGroups.value = Object.entries(groupMap).map(([label, voices]) => ({ label, voices }))
  } catch(_) {}
}
function resetConfig() {
  voiceConfig.vcn = 'x4_xiaoxuan'; voiceConfig.speed = 50; voiceConfig.pitch = 50; voiceConfig.volume = 50
  sceneConfig.serviceId = '315837983779590144'; sceneConfig.layout = 'fullscreen'; sceneConfig.displayMode = 'full'
}

// 内容管理方法
function cmOnFileChange(e) {
  const f = e.target.files[0]; if (!f) return
  cmFileName.value = f.name; cmFileData.value = f
  const reader = new FileReader()
  reader.onload = ev => { cmPreviewUrl.value = ev.target.result }
  reader.readAsDataURL(f)
}
async function cmAddImage() {
  if (!cmCanAdd.value) return
  const spotName = cmSpotName.value.trim()
  // 先调后端 API 上传
  try {
    const formData = new FormData()
    formData.append('file', cmFileData.value)
    formData.append('spot_name', spotName)
    formData.append('scenic_id', 'lingshan')
    const res = await fetch('/api/v1/admin/content/image/upload', {
      method: 'POST',
      body: formData,
      signal: AbortSignal.timeout(30000),
    })
    const data = await res.json().catch(() => null)
    if (res.ok && data && (data.code === 200 || data.code === 0)) {
      const img = data.data || {}
      cmImages.value.push({
        id: img.id || Date.now(), spotName,
        name: img.file_name || cmFileName.value,
        url: img.file_url ? (img.file_url.startsWith('http') ? img.file_url : 'http://192.0.2.1:50085' + img.file_url) : cmPreviewUrl.value,
        time: img.created_at || new Date().toISOString(),
        size: img.file_size || cmFileData.value.size,
      })
      cmSpotName.value = ''; cmFileName.value = ''; cmFileData.value = null; cmPreviewUrl.value = ''
      showToast('✅ 已上传景点图片: ' + spotName, 'success'); return
    }
  } catch(_) { /* API 不可用，降级 */ }
  // 降级：存本地
  const now = new Date()
  const fileSize = cmFileData.value ? cmFileData.value.size : 0
  cmImages.value.push({ id: Date.now(), spotName, name: cmFileName.value, url: cmPreviewUrl.value, time: now.toLocaleDateString('zh-CN'), size: fileSize })
  cmSpotName.value = ''; cmFileName.value = ''; cmFileData.value = null; cmPreviewUrl.value = ''
  showToast('✅ 已添加景点: ' + spotName + ' (本地)', 'success')
}
async function cmDelete(i) {
  const img = cmImages.value[i]
  // 如果有后端 ID，调 API 删除
  if (img && img.id && typeof img.id === 'number') {
    try {
      await fetch('/api/v1/admin/content/image/' + img.id, {
        method: 'DELETE',
        headers: getAuthHeaders(),
        signal: AbortSignal.timeout(5000),
      })
    } catch(_) {}
  }
  cmImages.value.splice(i, 1)
}
async function loadContentImages() {
  try {
    const res = await fetch('/api/v1/admin/content/images?scenic_id=lingshan', {
      headers: getAuthHeaders(),
      signal: AbortSignal.timeout(8000),
    })
    if (!res.ok) return
    const data = await res.json().catch(() => null)
    const items = data?.data || data?.items || []
    if (Array.isArray(items) && items.length > 0) {
      cmImages.value = items.map(img => ({
        id: img.id,
        spotName: img.spot_name || '',
        name: img.file_name || '',
        url: img.file_url ? (img.file_url.startsWith('http') ? img.file_url : 'http://192.0.2.1:50085' + img.file_url) : '',
        time: img.created_at || '',
        size: img.file_size || 0,
      }))
    }
  } catch(_) {}
}
function cmCopyRef(img) {
  const refPath = '/static/assets/images/' + (img.name || 'spot.jpg')
  navigator.clipboard.writeText(refPath).then(() => showToast('✅ 已复制: ' + refPath, 'success')).catch(() => showToast('复制失败: ' + refPath, 'error'))
}

// ============================================================
// 知识库方法（全部重写，直接对接后端 API）
// ============================================================
let _knoFileData = null       // 当前选中的文件对象
let _lastPreviewId = ''       // 上次 preview 的 ID，供 confirm 使用

// 文档播放器状态
const docViewerMode = ref('')        // 'original' | 'chunks'
const originalFileType = ref('')     // 'pdf' | 'txt' | 'md' | 'docx'
const originalFileContent = ref('')   // 文本内容（txt/md 转文本后）
const originalFileHtml = ref('')      // HTML内容（md 渲染后）
const originalFileUrl = ref('')       // PDF / DOCX blob URL
const docZoomLevel = ref(100)
const docViewerBusy = ref(false)
const currentViewDoc = ref(null)

function detectFileType(name) {
  const ext = (name || '').match(/\.(\w+)$/i); return ext ? ext[1].toLowerCase() : ''
}

// ===================== 1. 选择文件 =====================
function onFileChange(e) {
  const f = e.target.files[0]; if (!f) return
  if (originalFileUrl.value) URL.revokeObjectURL(originalFileUrl.value)
  fileName.value = f.name
  _knoFileData = f
  previewChunks.value = []; previewDone.value = false; uploadError.value = ''
  // 自动在文档播放器中预览原始文件
  _readOriginalFile(f)
}

// ===================== 2. 读取原始文件用于播放器 =====================
async function _readOriginalFile(file) {
  const ext = detectFileType(file.name)
  originalFileType.value = ext
  originalFileContent.value = ''
  originalFileHtml.value = ''
  originalFileUrl.value = ''
  docViewerMode.value = 'original'
  docViewerBusy.value = true

  if (ext === 'pdf' || ext === 'docx') {
    originalFileUrl.value = URL.createObjectURL(file)
  } else if (ext === 'txt' || ext === 'md') {
    const text = await new Promise((resolve) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result || '')
      reader.onerror = () => resolve('')
      reader.readAsText(file)
    })
    originalFileContent.value = text
    if (ext === 'md') originalFileHtml.value = renderSimpleMd(text)
  }
  docViewerBusy.value = false
}

// 从 Markdown 文本解析知识块（完整正则版本）
function _parseChunksFromMarkdown(text) {
  if (!text) return []
  const chunks = []
  // 按一级标题或二级标题拆分
  const segments = text.split(/(?=^#{1,6}\s+)/m)
  segments.forEach((seg, i) => {
    const t = seg.trim()
    if (!t) return
    const lines = t.split('\n')
    const headerMatch = lines[0].match(/^#{1,6}\s+(.+)/)
    const heading = headerMatch ? headerMatch[1].trim() : ''
    const content = headerMatch ? lines.slice(1).join('\n').trim() : t
    // 清洗人工插入的 "第 N 段" / "Chunk N" 标题
    const cleaned = content.replace(/^[#*]{1,6}\s*(第\s*\d+\s*段|Chunk\s+\d+|第\s*\d+\s*块)\s*[*#]*\s*$/gm, '').replace(/^\s*(第\s*\d+\s*段|Chunk\s+\d+|第\s*\d+\s*块)\s*$/gm, '').trim()
    if (cleaned || heading) {
      chunks.push({
        chunk_id: 'chunk-' + (i + 1),
        heading: heading || ('第' + (i + 1) + '段'),
        content: cleaned || heading || t,
        length: (cleaned || heading || t).length,
      })
    }
  })
  // 如果拆分段太少，尝试按连续空行拆分
  if (chunks.length <= 2 && text.length > 500) {
    const altChunks = []
    const paras = text.split(/\n\n+/)
    let running = ''
    let paraIdx = 0
    paras.forEach((para) => {
      if (para.match(/^#{1,6}\s/)) {
        if (running.trim()) { altChunks.push({ chunk_id: 'chunk-' + (altChunks.length + 1), heading: '', content: running.trim(), length: running.trim().length }); running = '' }
        altChunks.push({ chunk_id: 'chunk-' + (altChunks.length + 1), heading: para.replace(/^#+\s*/, '').trim(), content: para.trim(), length: para.trim().length })
      } else {
        running += (running ? '\n\n' : '') + para
        if (para.length > 800 || (++paraIdx % 3 === 0 && running.length > 300)) {
          altChunks.push({ chunk_id: 'chunk-' + (altChunks.length + 1), heading: '', content: running.trim(), length: running.trim().length })
          running = ''
        }
      }
    })
    if (running.trim()) altChunks.push({ chunk_id: 'chunk-' + (altChunks.length + 1), heading: '', content: running.trim(), length: running.trim().length })
    if (altChunks.length > chunks.length) return altChunks
  }
  return chunks.length > 0 ? chunks : [{ chunk_id: 'chunk-1', heading: '全文', content: text, length: text.length }]
}

// ===================== 3. 预览文档 =====================
async function previewDoc() {
  if (!canUpload.value) return
  previewing.value = true; previewDone.value = false; uploadError.value = ''
  previewFileName.value = fileName.value
  previewFileData.value = _knoFileData

  try {
    const formData = new FormData()
    formData.append('file', _knoFileData)
    formData.append('scenic_id', 'lingshan')
    const res = await fetch(KNO_PREVIEW_URL, {
      method: 'POST', body: formData,
      signal: AbortSignal.timeout(60000),
      headers: { 'Authorization': `Bearer ${localStorage.getItem('user_token') || ''}` }
    })
    if (!res.ok) throw new Error('预览失败: ' + res.status)
    const data = await res.json()
    console.log('[知识库] preview 响应:', data)
    const mdContent = data.content || data.data?.content || ''
    previewChunks.value = _parseChunksFromMarkdown(mdContent)
    previewDone.value = true
  } catch(e) {
    console.error('[知识库] 预览失败:', e)
    uploadError.value = '预览失败: ' + e.message
  } finally {
    previewing.value = false
  }
}

// ===================== 4. 加载文件列表（从 localStorage） =====================

// ===================== 6. 持久化 =====================
function _saveDocList() {
  try {
    const lite = docList.value.map(d => ({
      id: d.id, name: d.name, type: d.type, typeLabel: d.typeLabel,
      size: d.size, time: d.time, stored: d.stored, collection: d.collection,
      chunkCount: d.chunkCount,
      chunks: (d.chunks || []).map(c => ({ content: c.content, length: c.length }))
    }))
    localStorage.setItem('kno_doc_list', JSON.stringify(lite))
  } catch(e) { /* ignore */ }
}
function _loadDocList() {
  // 优先从后端 API 加载
  loadKnowledgeFiles()
  // 同时加载 localStorage 作为缓存/降级
  try {
    const raw = localStorage.getItem('kno_doc_list')
    if (raw && docList.value.length === 0) { const list = JSON.parse(raw); if (Array.isArray(list)) docList.value = list }
  } catch(e) { /* ignore */ }
}
async function loadKnowledgeFiles() {
  try {
    const res = await fetch('/api/v1/admin/knowledge/files?scenic_id=lingshan&page=1&page_size=100', {
      headers: getAuthHeaders(),
      signal: AbortSignal.timeout(8000),
    })
    if (!res.ok) return
    const data = await res.json().catch(() => null)
    const items = data?.data?.items || data?.data || data?.items || []
    if (Array.isArray(items) && items.length > 0) {
      docList.value = items.map(f => ({
        id: f.id, name: f.name, type: f.file_ext || '', typeLabel: (f.file_ext || '').toUpperCase(),
        size: f.file_size ? (f.file_size / 1024).toFixed(1) + ' KB' : '',
        time: f.created_at || '', stored: true, collection: '', chunkCount: 0, chunks: [],
      }))
      _saveDocList()
    }
  } catch(_) {}
}

// ===================== 7. 清理 / 关闭 / 查看 / 导出 / 删除 =====================
function clearPreview() {
  previewChunks.value = []; previewDone.value = false; previewFileName.value = ''
  previewFileData.value = null; fileName.value = ''; newDocType.value = ''
  _knoFileData = null; uploadError.value = ''
  docViewerMode.value = ''; currentViewDoc.value = null
}
// confirmUpload: 调后端 POST /knowledge/upload，降级到旧接口
async function confirmUpload() {
  if (!previewDone.value) {
    await previewDoc()
    if (!previewDone.value) return
  }
  previewing.value = true
  try {
    const formData = new FormData()
    formData.append('file', previewFileData.value)
    formData.append('scenic_id', 'lingshan')
    // 优先调后端上传接口
    let res = await fetch('/api/v1/admin/knowledge/upload', {
      method: 'POST', body: formData,
      signal: AbortSignal.timeout(30000),
      headers: { 'Authorization': `Bearer ${localStorage.getItem('user_token') || ''}` }
    })
    // 降级到旧 confirm 接口
    if (!res.ok) res = await fetch(KNO_CONFIRM_URL, { method: 'POST', body: formData, signal: AbortSignal.timeout(30000), headers: { 'Authorization': `Bearer ${localStorage.getItem('user_token') || ''}` } })
    // 再降级到 guide upload
    if (!res.ok) res = await fetch(KNO_UPLOAD_URL, { method: 'POST', body: formData, signal: AbortSignal.timeout(30000), headers: { 'Authorization': `Bearer ${localStorage.getItem('user_token') || ''}` } })
    if (!res.ok) throw new Error('上传失败: ' + res.status)
    const data = await res.json()
    const uploaded = data.data || data
    const sizeKB = previewFileData.value ? (previewFileData.value.size / 1024).toFixed(1) : '未知'
    const typeInfo = docTypeOptions.find(t => t.value === newDocType.value)
    const newDoc = {
      id: uploaded.id || Date.now(),
      name: uploaded.name || previewFileName.value,
      type: newDocType.value,
      typeLabel: typeInfo ? typeInfo.label : newDocType.value,
      icon: '📄',
      size: sizeKB + ' KB',
      time: new Date().toISOString().slice(0, 10),
      stored: true,
      chunks: previewChunks.value,
    }
    docList.value.unshift(newDoc)
    _saveDocList()
    clearPreview()
    if (selectedDocId.value === newDoc.id) viewDocDetail(newDoc)
    showToast(`✅ 文档「${newDoc.name}」已上传`, 'success')
  } catch(e) {
    console.error('[知识库] 上传失败:', e)
    showToast('❌ 上传失败: ' + e.message, 'error')
  } finally {
    previewing.value = false
  }
}
function closeViewer() {
  if (originalFileUrl.value) URL.revokeObjectURL(originalFileUrl.value)
  docViewerMode.value = ''
  originalFileContent.value = ''
  originalFileHtml.value = ''
  originalFileUrl.value = ''
  currentViewDoc.value = null
  clearPreview()
}
function viewDocDetail(doc) {
  selectedDocId.value = doc.id; clearPreview()
  currentViewDoc.value = doc
  if (doc.chunks && doc.chunks.length > 0) {
    const fullMD = doc.chunks.map((chunk) => {
      return _cleanChunkContent(chunk.content || '')
    }).join('\n\n---\n\n')
    originalFileHtml.value = renderSimpleMd(fullMD)
    originalFileType.value = 'md'
    docViewerMode.value = 'original'
  }
}
function exportDocMD(doc) {
  let md = `# ${doc.name}\n\n> 文档类型：${doc.typeLabel} ｜ 上传时间：${doc.time} ｜ 大小：${doc.size}\n\n---\n\n`
  doc.chunks.forEach((chunk, i) => {
    md += `\n\n---\n\n${_cleanChunkContent(chunk.content || '')}`
  })
  const blob = new Blob([md], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url
  a.download = doc.name.replace(/\.\w+$/, '') + '_切分导出.md'
  document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url)
  showToast('✅ 已导出为 Markdown 文件', 'success')
}
function deleteDoc(doc) {
  if (!doc || !doc.id) return
  const confirmed = window.confirm(`确定要删除文档「${doc.name}」吗？\n\n此操作将同时删除该文档的所有知识块，且无法恢复！`)
  if (!confirmed) return
  // 调后端 API 删除
  if (typeof doc.id === 'number') {
    fetch('/api/v1/admin/knowledge/files/' + doc.id, {
      method: 'DELETE',
      headers: getAuthHeaders(),
      signal: AbortSignal.timeout(5000),
    }).catch(() => {})
  }
  const idx = docList.value.findIndex(d => d.id === doc.id)
  if (idx > -1) docList.value.splice(idx, 1)
  _saveDocList()
  if (selectedDocId.value === doc.id) {
    selectedDocId.value = ''
    closeViewer()
  }
  showToast(`🗑️ 文档「${doc.name}」已删除`, 'success')
}

// 通用工具
function escapeHtml(str) {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}
function renderSimpleMd(text) {
  if (!text) return ''
  return marked.parse(text, { async: false })
}
// 清洗 chunk 内容中的"第 N 段"/"Chunk N"等人工插入标题（数据脏），保留原始正文
function _cleanChunkContent(text) {
  if (!text) return ''
  let t = String(text).trim()
  let prev
  do {
    prev = t
    t = t.replace(/^[#*]{1,6}\s*(第\s*\d+\s*段|Chunk\s+\d+|第\s*\d+\s*块)\s*[*#]*\s*$/gm, '').trim()
    t = t.replace(/^\s*(第\s*\d+\s*段|Chunk\s+\d+|第\s*\d+\s*块)\s*$/gm, '').trim()
  } while (t !== prev)
  return t
}
function changeZoom(delta) {
  docZoomLevel.value = Math.max(40, Math.min(200, docZoomLevel.value + delta))
}
function resetZoom() {
  docZoomLevel.value = 100
}
function toggleFullscreen() {
  const el = document.querySelector('.doc-viewer')
  if (!el) return
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    el.requestFullscreen().catch(() => {})
  }
}

// 对话分析方法
function selectUser(u) { selectedUserId.value = u.id }
function formatTime(isoStr, offset) {
  try {
    const d = new Date(isoStr); const mins = Math.floor(offset * 5)
    d.setMinutes(d.getMinutes() + mins)
    return String(d.getHours()).padStart(2,'0') + ':' + String(d.getMinutes()).padStart(2,'0')
  } catch(e) { return '--' }
}
function formatRelative(isoStr) {
  try {
    const d = new Date(isoStr); const now = new Date(); const diffMs = now - d
    const diffMin = Math.floor(diffMs / 60000); const diffHour = Math.floor(diffMs / 3600000); const diffDay = Math.floor(diffMs / 86400000)
    if (diffMin < 60) return diffMin + '分钟前'
    if (diffHour < 24) return diffHour + '小时前'
    if (diffDay < 7) return diffDay + '天前'
    return d.toLocaleDateString('zh-CN')
  } catch(e) { return '--' }
}
function toggleAnalysis(msg, userData) {
  if (!userData) return
  if (!msg._analysisLoaded) {
    msg.showAnalysis = true; msg._analysisLoaded = true
    const reasonSections = userData.reasonSections || {}
    const suggestionSections = userData.suggestionSections || {}
    const emotion = userData.satisfaction >= 80 ? '正向' : userData.satisfaction >= 50 ? '中性' : '负向'
    const analysis = { '情感倾向': emotion }
    Object.entries(reasonSections).forEach(([k, v]) => { analysis[k] = v })
    Object.entries(suggestionSections).forEach(([k, v]) => { analysis[k] = v })
    msg.analysis = analysis
  } else { msg.showAnalysis = !msg.showAnalysis }
}

// 数据分析方法
function toggleItem(item) {
  const s = expandedSet.value
  if (s.has(item)) s.delete(item); else s.add(item)
  expandedSet.value = new Set(s)
}
function isOpen(item) { return expandedSet.value.has(item) }
function scrollToType(type) {
  const map = { urgent: '紧急问题', improve: '服务改进', optimize: '优化建议', keep: '正面保持' }
  const items = analysisItems.value.filter(i => i.type === map[type])
  if (items.length > 0) toggleItem(items[0])
}

// 3D知识图谱
const kg3dLoaded = ref(false)
let gData = null

async function fetchKGGraph() {
  try {
    const headers = getAuthHeaders()
    const res = await fetch('/api/v1/guide/knowledge-graph/overview', {
      signal: AbortSignal.timeout(10000),
      headers
    })
    if (!res.ok) return null
    const data = await res.json()
    // 取第一个景区的 expand 来获取 nodes/edges
    const scenic = data.scenics?.[0]
    if (!scenic) return null
    const expandRes = await fetch(`/api/v1/guide/knowledge-graph/expand?entity=${scenic.scenic_id}`, {
      signal: AbortSignal.timeout(10000),
      headers
    })
    if (!expandRes.ok) return null
    const expandData = await expandRes.json()
    if (expandData.nodes?.length > 0 || expandData.edges?.length > 0) {
      return { nodes: expandData.nodes, links: expandData.edges.map(e => ({ source: e.from, target: e.to })) }
    }
    return null
  } catch(e) {
    console.log('KG API not available, using local data:', e.message)
    return null
  }
}

const FALLBACK_GRAPH = {
  nodes: [
    { id: 'lingshan', name: '灵山胜境', type: 'ScenicSpot', val: 28 },
    { id: 'dafo', name: '灵山大佛', type: 'ScenicSpot', val: 24 },
    { id: 'fanguan', name: '梵宫', type: 'ScenicSpot', val: 20 },
    { id: 'jiulong', name: '九龙灌浴', type: 'Show', val: 16 },
    { id: 'puti', name: '菩提大道', type: 'ScenicSpot', val: 12 },
    { id: 'xuanzang', name: '玄奘', type: 'Person', val: 12 },
    { id: 'tang', name: '唐代', type: 'Dynasty', val: 8 },
    { id: 'nianhua', name: '拈花湾', type: 'ScenicSpot', val: 16 },
    { id: 'suzhai', name: '素斋馆', type: 'Food', val: 8 },
    { id: 'jiudian', name: '灵山精舍', type: 'Hotel', val: 8 },
    { id: 'tag_fojiao', name: '佛教文化', type: 'Tag', val: 4 },
    { id: 'tag_sheyings', name: '拍照打卡', type: 'Tag', val: 4 },
    { id: 'bowuguan', name: '佛教博物馆', type: 'ScenicSpot', val: 10 },
    { id: 'chanxing', name: '禅行表演', type: 'Show', val: 10 },
  ],
  links: [
    { source: 'lingshan', target: 'dafo' }, { source: 'lingshan', target: 'fanguan' },
    { source: 'dafo', target: 'jiulong' }, { source: 'dafo', target: 'puti' },
    { source: 'dafo', target: 'xuanzang' }, { source: 'xuanzang', target: 'tang' },
    { source: 'lingshan', target: 'nianhua' }, { source: 'lingshan', target: 'suzhai' },
    { source: 'lingshan', target: 'jiudian' }, { source: 'dafo', target: 'tag_fojiao' },
    { source: 'jiulong', target: 'tag_sheyings' }, { source: 'lingshan', target: 'bowuguan' },
    { source: 'nianhua', target: 'chanxing' },
  ]
}
async function load3DKnowledgeGraph() {
  if (kg3dLoaded.value) return
  kg3dLoaded.value = true
  // 优先从 API 获取图谱数据，失败则用本地数据
  const apiGraph = await fetchKGGraph()
  gData = apiGraph || FALLBACK_GRAPH
  await loadScript('https://unpkg.com/3d-force-graph@1.70.2/dist/3d-force-graph.min.js')
  initKG3D()
}
function loadScript(url) {
  return new Promise((resolve, reject) => {
    const s = document.createElement('script'); s.src = url
    s.onload = resolve; s.onerror = reject; document.body.appendChild(s)
  })
}
function initKG3D() {
  const container = document.getElementById('kg3d-graph')
  if (!container) return
  if (!gData) { console.log('KG data not ready'); return }
  const legend = document.getElementById('kg3dLegend')
  if (legend) {
    [['景点','#F5A623'],['人物','#52c41a'],['朝代','#faad14'],['演出','#eb2f96'],['美食','#FF6B6B'],['住宿','#2db7f5'],['标签','#8c8c8c']].forEach(([label,color]) => {
      const el = document.createElement('div'); el.className = 'kg3d-legend-item'
      el.innerHTML = '<span class="kg3d-legend-dot" style="background:'+color+'"></span>'+label
      legend.appendChild(el)
    })
  }
  const types = { ScenicSpot:'#F5A623', Person:'#52c41a', Dynasty:'#faad14', Show:'#eb2f96', Food:'#FF6B6B', Hotel:'#2db7f5', Tag:'#8c8c8c' }
  const nodeCountEl = document.getElementById('kg3dNodeCount')
  const edgeCountEl = document.getElementById('kg3dEdgeCount')
  if (nodeCountEl) nodeCountEl.textContent = gData.nodes.length
  if (edgeCountEl) edgeCountEl.textContent = gData.links.length
  const Graph = ForceGraph3D()(container)
    .graphData(gData).nodeId('id').nodeVal(n => n.val || 8).nodeColor(n => types[n.type] || '#8c8c8c')
    .nodeRelSize(5).linkColor(() => 'rgba(148,163,184,0.15)').linkWidth(0.3)
    .linkDirectionalParticles(0.5).linkDirectionalParticleWidth(1.5).linkDirectionalParticleColor(() => '#06b6d4')
    .linkDirectionalParticleSpeed(0.005).backgroundColor('#0a0e27')
    .d3AlphaDecay(0.03).d3VelocityDecay(0.25).warmupTicks(80).cooldownTicks(30)
    .nodeLabel(n => n.name)
  Graph.cameraPosition({ x: 50, y: 35, z: 40 })
  let autoRotate = true
  Graph.onEngineTick(() => {
    if (autoRotate) {
      const cam = Graph.camera(); const t = Date.now() / 10000
      cam.position.x = 50 * Math.cos(t); cam.position.z = 50 * Math.sin(t); cam.lookAt(0, 0, 0)
    }
  })
  container.addEventListener('pointerdown', () => { autoRotate = false })
  container.addEventListener('pointerup', () => setTimeout(() => { autoRotate = true }, 5000))
  document.getElementById('kg3dLoading')?.classList.add('hide')
}

// 图表
let mapLoaded = false
async function loadChinaMap() {
  if (mapLoaded) return true
  try {
    const res = await fetch('/china.json')
    const geoJson = await res.json(); echarts.registerMap('china', geoJson); mapLoaded = true; return true
  } catch(e) { console.log('地图加载失败:', e); return false }
}

function initCharts() {
  if (route.path !== '/dashboard') return
  const commonTooltip = { backgroundColor: 'rgba(2,6,23,0.9)', borderColor: '#1e2d4a', textStyle: { color: '#e2e8f0' } }
  // 1. 情感趋势（柱线组合图）
  const sentimentEl = document.getElementById('chart-sentiment')
  if (!sentimentEl) return
  chartSentiment?.dispose()
  chartSentiment = echarts.init(sentimentEl)
  const trendSeries = apiCache.trend?.series || []
  const trendHours = trendSeries.map(s => {
    const bucket = s.bucket || ''
    const match = bucket.match(/(\d{2}):(\d{2})$/)
    return match ? match[1] + ':' + match[2] : bucket.substring(5, 16) || '--:--'
  })
  const posData = trendSeries.map(s => s.positive || 0)
  const negData = trendSeries.map(s => s.negative || 0)
  // 计算正面率（百分比）
  const posRateData = trendSeries.map(s => {
    const p = s.positive || 0
    const n = s.negative || 0
    const total = p + n
    return total > 0 ? +((p / total) * 100).toFixed(1) : 0
  })

  if (trendSeries.length > 0) {
    chartSentiment.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(2,6,23,0.92)',
        borderColor: '#1e2d4a',
        borderWidth: 1,
        textStyle: { color: '#e2e8f0', fontSize: 12 },
        axisPointer: { type: 'cross', crossStyle: { color: '#64748b' } }
      },
      legend: {
        data: ['正面', '负面', '正面率'],
        textStyle: { color: '#64748b', fontSize: 10 },
        top: 0,
        icon: 'roundRect',
        itemWidth: 14,
        itemHeight: 4
      },
      grid: { top: 30, left: 50, right: 56, bottom: 18 },
      xAxis: {
        type: 'category',
        data: trendHours,
        axisPointer: { type: 'shadow' },
        axisLine: { lineStyle: { color: '#1e2d4a' } },
        axisLabel: { color: '#64748b', fontSize: 10 },
        splitLine: { show: false },
        axisTick: { show: false }
      },
      yAxis: [
        {
          type: 'value',
          name: '次数',
          nameTextStyle: { color: '#64748b', fontSize: 9 },
          axisLine: { show: false },
          splitLine: { lineStyle: { color: 'rgba(30,45,74,0.2)' } },
          axisLabel: { color: '#64748b', fontSize: 9 }
        },
        {
          type: 'value',
          name: '%',
          min: 0, max: 100,
          nameTextStyle: { color: '#64748b', fontSize: 9 },
          axisLine: { show: false },
          splitLine: { show: false },
          axisLabel: { color: '#64748b', fontSize: 9, formatter: '{value}%' }
        }
      ],
      series: [
        {
          name: '正面',
          type: 'bar',
          stack: 'total',
          barWidth: 20,
          data: posData,
          itemStyle: {
            color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#34d399' }, { offset: 1, color: '#10b981' }] },
            borderRadius: [3, 3, 0, 0]
          },
          emphasis: { focus: 'series' }
        },
        {
          name: '负面',
          type: 'bar',
          stack: 'total',
          barWidth: 20,
          data: negData,
          itemStyle: {
            color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#f87171' }, { offset: 1, color: '#ef4444' }] },
            borderRadius: [3, 3, 0, 0]
          },
          emphasis: { focus: 'series' }
        },
        {
          name: '正面率',
          type: 'line',
          yAxisIndex: 1,
          data: posRateData,
          smooth: true,
          symbol: 'diamond',
          symbolSize: 8,
          lineStyle: { color: '#60a5fa', width: 2.5, shadowBlur: 6, shadowColor: 'rgba(96,165,250,0.3)' },
          itemStyle: { color: '#60a5fa', borderColor: '#1e293b', borderWidth: 1 },
          areaStyle: {
            color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(96,165,250,0.2)' }, { offset: 1, color: 'rgba(96,165,250,0.02)' }] }
          },
          label: {
            show: true,
            position: 'top',
            color: '#60a5fa',
            fontSize: 9,
            fontWeight: 600,
            formatter: '{c}%'
          },
          emphasis: { focus: 'series' }
        }
      ]
    })
  } else {
    chartSentiment.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#64748b', fontSize: 14, fontWeight: 400 } } })
  }
  // 2. 地图
  const mapWrapper = document.getElementById('chart-map')
  if (mapWrapper) {
    chartMap?.dispose(); chartMap = echarts.init(mapWrapper); mapLoading.value = true
    loadChinaMap().then(ok => {
      mapLoading.value = false
      if (ok) {
        const totalVisitors = MAP_DATA.reduce((s, d) => s + d.value, 0)
        chartMap.setOption({
          tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(2,6,23,0.92)',
            borderColor: '#1e2d4a',
            borderWidth: 1,
            textStyle: { color: '#e2e8f0', fontSize: 12 },
            formatter: function(params) {
              if (!params.data) return ''
              const val = params.data.value || 0
              const pct = totalVisitors > 0 ? ((val / totalVisitors) * 100).toFixed(1) : '0.0'
              const sorted = [...MAP_DATA].sort((a, b) => b.value - a.value)
              const rank = sorted.findIndex(d => d.name === params.name) + 1
              const rankEmoji = rank <= 3 ? ['🥇','🥈','🥉'][rank-1] : `#${rank}`
              return `<div style="font-size:13px;font-weight:600;margin-bottom:4px">${params.name}</div>
                <div style="display:flex;justify-content:space-between;gap:20px;font-size:12px;line-height:1.8">
                  <span>游客数</span><span style="color:#06b6d4;font-weight:600">${val} 人</span>
                </div>
                <div style="display:flex;justify-content:space-between;gap:20px;font-size:12px;line-height:1.8">
                  <span>占比</span><span style="color:#3b82f6;font-weight:600">${pct}%</span>
                </div>
                <div style="display:flex;justify-content:space-between;gap:20px;font-size:12px;line-height:1.8;border-top:1px solid rgba(30,45,74,0.4);padding-top:4px;margin-top:2px">
                  <span>全国排名</span><span style="color:${rank<=3?'#f59e0b':'#64748b'};font-weight:600">${rankEmoji}</span>
                </div>`
            }
          },
          visualMap: {
            min: 0, max: 500,
            left: 4, bottom: 4,
            text: ['多', '少'],
            textStyle: { color: '#64748b', fontSize: 9 },
            itemWidth: 10, itemHeight: 60,
            inRange: { color: ['#0c1929', '#0f2847', '#1a4a7a', '#3b82f6', '#06b6d4'] },
            outOfRange: { color: '#0c1929' },
            calculable: true,
            hoverLink: true
          },
          geo: {
            map: 'china',
            roam: true,
            zoom: 1.1,
            center: [104, 35],
            label: {
              show: true,
              color: '#94a3b8',
              fontSize: 8,
              fontWeight: 300
            },
            emphasis: {
              label: { show: true, color: '#fff', fontSize: 10, fontWeight: 600 },
              itemStyle: {
                areaColor: '#06b6d4',
                shadowBlur: 20,
                shadowColor: 'rgba(6,182,212,0.4)'
              }
            },
            itemStyle: {
              areaColor: '#0d1321',
              borderColor: '#1e3a5f',
              borderWidth: 1.2,
              shadowBlur: 4,
              shadowColor: 'rgba(6,182,212,0.1)'
            },
            select: {
              itemStyle: { areaColor: '#3b82f6' }
            }
          },
          series: [
            {
              name: '游客来源',
              type: 'map',
              geoIndex: 0,
              data: MAP_DATA,
              z: 2
            },
            {
              name: '客源城市',
              type: 'scatter',
              coordinateSystem: 'geo',
              data: CITY_SCATTER_DATA,
              z: 10,
              symbol: 'circle',
              symbolSize: function(val) {
                return Math.max(4, Math.min(14, val[2] / 35));
              },
              itemStyle: {
                color: '#06b6d4',
                shadowBlur: 8,
                shadowColor: 'rgba(6,182,212,0.5)'
              },
              label: {
                show: true,
                formatter: '{b}',
                color: '#e2e8f0',
                fontSize: 9,
                position: 'right',
                fontWeight: 400
              },
              emphasis: {
                itemStyle: {
                  color: '#3b82f6',
                  shadowBlur: 16,
                  shadowColor: 'rgba(59,130,246,0.6)'
                }
              }
            },
            // 发光轮廓效果
            {
              type: 'scatter',
              coordinateSystem: 'geo',
              data: CITY_SCATTER_DATA,
              z: 1,
              symbol: 'circle',
              symbolSize: function(val) {
                return Math.max(8, Math.min(18, val[2] / 25));
              },
              itemStyle: {
                color: 'rgba(6,182,212,0.08)',
                borderColor: 'rgba(6,182,212,0.15)',
                borderWidth: 2
              },
              silent: true
            }
          ]
        })
      } else {
        // 地图加载失败 - 降级为矩形树图
        MAP_DATA.sort((a, b) => b.value - a.value)
        chartMap.setOption({
          tooltip: { trigger: 'item', backgroundColor: 'rgba(2,6,23,0.9)', borderColor: '#1e2d4a', textStyle: { color: '#e2e8f0' }, formatter: function(p) {
            const val = p.value || 0
            const pct = totalVisitors > 0 ? ((val / totalVisitors) * 100).toFixed(1) : '0.0'
            return `${p.name}<br/>游客: <b>${val}</b> 人<br/>占比: <b>${pct}%</b>`
          }},
          series: [{
            type: 'treemap', width: '100%', height: '100%',
            breadcrumb: { show: false },
            label: { color: '#fff', fontSize: 10 },
            itemStyle: { borderColor: '#020617', borderWidth: 2, gapWidth: 2 },
            data: MAP_DATA.map(d => ({
              name: d.name,
              value: d.value,
              itemStyle: {
                color: d.value > 200 ? '#3b82f6' : d.value > 100 ? '#06b6d4' : d.value > 50 ? '#10b981' : '#1e3a5f'
              }
            }))
          }]
        })
      }
    })
  }
  // 3. 情感分布 — 优先使用独立API，降级到overview
  chartEmotion?.dispose(); chartEmotion = echarts.init(document.getElementById('chart-emotion'))
  const sd = apiCache.sentimentDist || apiCache.overview?.hot_topics?.today_sentiment_distribution || null
  const emotionData = sd && sd.data ? [
    { value: sd.data[0], name: '满意', itemStyle: { color: '#10b981' } },
    { value: sd.data[1], name: '中性', itemStyle: { color: '#f59e0b' } },
    { value: sd.data[2], name: '不满', itemStyle: { color: '#ef4444' } },
  ] : []
  if (emotionData.length > 0) {
    chartEmotion.setOption({ tooltip: { trigger: 'item', ...commonTooltip }, legend: { orient: 'vertical', right: 2, top: 'center', textStyle: { color: '#64748b', fontSize: 10 } }, series: [{ type: 'pie', radius: ['42%', '68%'], center: ['38%', '50%'], label: { show: true, color: '#94a3b8', fontSize: 10, formatter: '{b}\n{d}%' }, data: emotionData }] })
  } else {
    chartEmotion.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#64748b', fontSize: 14, fontWeight: 400 } } })
  }
  // 4. 热点问题
  const hqItems = apiCache.hotQ?.items || []
  const hqNames = hqItems.map(q => q.query).reverse()
  const hqValues = hqItems.map(q => q.count).reverse()
  chartHotBar?.dispose(); chartHotBar = echarts.init(document.getElementById('chart-hot-bar'))
  if (hqNames.length > 0) {
    chartHotBar.setOption({ tooltip: { trigger: 'axis', ...commonTooltip }, grid: { top: 6, left: 90, right: 24, bottom: 6 }, xAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: 'rgba(30,45,74,0.2)' } }, axisLabel: { color: '#64748b', fontSize: 10 } }, yAxis: { type: 'category', data: hqNames, axisLine: { show: false }, axisLabel: { color: '#94a3b8', fontSize: 10 }, splitLine: { show: false }, axisTick: { show: false } }, series: [{ type: 'bar', data: hqValues, itemStyle: { color: '#3b82f6', borderRadius: [0,3,3,0] }, barWidth: 9 }] })
  } else {
    chartHotBar.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#64748b', fontSize: 14, fontWeight: 400 } } })
  }
  // 5. 游客兴趣雷达 — 优先从 visitor-radar 获取，降级到 visitor-insights
  const vr = apiCache.visitorRadar || {}
  const vi = apiCache.visitorInsights || {}
  const radarIndicators = vr.indicators || vi.top_interests?.map(t => ({ name: t.name, max: Math.max(100, t.count * 1.5) })) || []
  const radarValues = vr.values || vi.top_interests?.map(t => t.count) || []
  chartRadar?.dispose(); chartRadar = echarts.init(document.getElementById('chart-radar'))
  if (radarIndicators.length > 0) {
    chartRadar.setOption({ tooltip: { ...commonTooltip }, radar: { indicator: radarIndicators, axisName: { color: '#94a3b8', fontSize: 9 }, splitArea: { areaStyle: { color: ['rgba(13,19,33,0.5)', 'rgba(13,19,33,0.3)'] } }, axisLine: { lineStyle: { color: 'rgba(30,45,74,0.4)' } }, splitLine: { lineStyle: { color: 'rgba(30,45,74,0.25)' } }, radius: '65%' }, series: [{ type: 'radar', data: [{ value: radarValues, name: '兴趣度', areaStyle: { color: 'rgba(59,130,246,0.2)' }, lineStyle: { color: '#3b82f6', width: 2 }, itemStyle: { color: '#3b82f6' }, symbolSize: 4 }] }] })
  } else {
    chartRadar.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#64748b', fontSize: 14, fontWeight: 400 } } })
  }
  // 6. 访客类型 — 从 visitor-insights 获取
  const vTypes = vi.types || []
  const visitorNames = vTypes.map(t => t.name).reverse()
  const visitorValues = vTypes.map(t => t.count).reverse()
  chartVisitor?.dispose(); chartVisitor = echarts.init(document.getElementById('chart-visitor'))
  if (visitorNames.length > 0) {
    chartVisitor.setOption({ tooltip: { trigger: 'axis', ...commonTooltip }, grid: { top: 6, left: 72, right: 24, bottom: 6 }, xAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: 'rgba(30,45,74,0.2)' } }, axisLabel: { color: '#64748b', fontSize: 10 } }, yAxis: { type: 'category', data: visitorNames, axisLine: { show: false }, axisLabel: { color: '#94a3b8', fontSize: 10 }, splitLine: { show: false }, axisTick: { show: false } }, series: [{ type: 'bar', data: visitorValues, itemStyle: { color: '#06b6d4', borderRadius: [0,3,3,0] }, barWidth: 9 }] })
  } else {
    chartVisitor.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#64748b', fontSize: 14, fontWeight: 400 } } })
  }
  // 7. 问题分类 — 从 question-categories 获取
  const qc = apiCache.questionCategories || {}
  const qcItems = qc.items || []
  const colors = ['#3b82f6','#06b6d4','#10b981','#f59e0b','#8b5cf6','#ec4899','#ef4444','#14b8a6','#f97316','#64748b']
  const catData = qcItems.length > 0 ? qcItems.map((t, i) => ({ value: t.value, name: t.name, itemStyle: { color: colors[i % colors.length] } })) : []
  chartCategory?.dispose(); chartCategory = echarts.init(document.getElementById('chart-category'))
  if (catData.length > 0) {
    chartCategory.setOption({ tooltip: { trigger: 'item', ...commonTooltip }, series: [{ type: 'pie', radius: ['32%', '62%'], center: ['50%', '50%'], roseType: 'area', label: { color: '#94a3b8', fontSize: 10, formatter: '{b}\n{d}%' }, data: catData }] })
  } else {
    chartCategory.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#64748b', fontSize: 14, fontWeight: 400 } } })
  }
}

async function loadDashboardData() {
  const data = await fetchAllDashboardData()
  // 缓存所有API数据
  if (data.overview) apiCache.overview = data.overview
  if (data.trend) apiCache.trend = data.trend
  if (data.hotQ) apiCache.hotQ = data.hotQ
  if (data.sentimentDist) apiCache.sentimentDist = data.sentimentDist
  if (data.visitorInsights) apiCache.visitorInsights = data.visitorInsights
  if (data.visitorRadar) apiCache.visitorRadar = data.visitorRadar
  if (data.questionCategories) apiCache.questionCategories = data.questionCategories
  if (data.realtimeStats) apiCache.realtimeStats = data.realtimeStats

  const ov = apiCache.overview
  const ht = ov?.hot_topics || {}
  const rt = apiCache.realtimeStats || {}

  // KPI 数据：优先从 realtime-stats 获取实时数据，降级到 overview
  const kpiSessions = ov?.total_sessions ?? '--'
  const kpiTurns = ov?.total_turns ?? '--'
  const kpiPositiveRate = ov?.positive_rate != null ? (ov.positive_rate * 100).toFixed(1) : '--'
  const kpiNegativeRate = ov?.negative_rate != null ? (ov.negative_rate * 100).toFixed(1) : '--'
  const kpiAvgSatisfaction = ov?.avg_satisfaction != null ? ov.avg_satisfaction.toFixed(1) : '--'
  const kpiOnline = rt.active_sessions ?? ht.current_online ?? '--'
  const kpiTodayQ = rt.today_turns ?? ht.today_questions ?? '--'
  const kpiTodayVisitors = rt.today_visitors ?? ht.today_visitors ?? '--'
  const kpiNegAlerts = rt.negative_alerts ?? Math.round((ov?.negative_rate || 0) * (ov?.total_sessions || 0))
  const kpiAvgSatisfactionRt = rt.avg_satisfaction != null && rt.avg_satisfaction > 0 ? rt.avg_satisfaction.toFixed(1) : '--'

  kpiAll.value = [
    { label: '总会话数', value: String(kpiSessions), unit: '次', color: '#3b82f6' },
    { label: '总轮次', value: String(kpiTurns), unit: '轮', color: '#60a5fa' },
    { label: '满意率', value: kpiPositiveRate, unit: '%', color: '#10b981' },
    { label: '不满意率', value: kpiNegativeRate, unit: '%', color: '#ef4444' },
    { label: '平均满意度', value: kpiAvgSatisfaction, unit: '分', color: '#f59e0b' },
    { label: '在线会话', value: String(kpiOnline), unit: '个', color: '#06b6d4' },
    { label: '今日提问', value: String(kpiTodayQ), unit: '条', color: '#8b5cf6' },
    { label: '今日访客', value: String(kpiTodayVisitors), unit: '人', color: '#ec4899' },
    { label: '负面预警', value: String(kpiNegAlerts), unit: '条', color: '#ef4444' },
    { label: '响应时长', value: '--', unit: 's', color: '#14b8a6' },
  ]

  // 热点问题表格
  if (data.hotQ?.items?.length > 0) {
    hotQuestions.value = data.hotQ.items.map(q => ({ text: q.query, count: q.count, trend: q.trend ?? 0 }))
  } else {
    hotQuestions.value = []
  }

  if (route.path === '/dashboard') setTimeout(initCharts, 100)
  return true
}

async function loadDialogueData() {
  dlgLoading.value = true; dlgError.value = ''
  try {
    // 调后端 API 获取会话列表
    const res = await fetch('/api/v1/admin/dialogue/sessions?scenic_id=lingshan&page=1&page_size=200', {
      signal: AbortSignal.timeout(15000),
      headers: getAuthHeaders()
    })
    if (!res.ok) throw new Error('服务器错误: ' + res.status)
    const resp = await res.json()
    const sessionsData = resp?.data?.items || resp?.data || resp?.items || []
    if (!Array.isArray(sessionsData) || sessionsData.length === 0) { dlgUsers.value = []; dlgLoading.value = false; return }

    // 按 user_name 分组会话
    const userMap = {}
    sessionsData.forEach((s) => {
      const userName = s.user_name || s.session_id || '未知'
      if (!userMap[userName]) {
        userMap[userName] = { id: userName, name: userName, sessions: [], totalPositive: 0, totalNegative: 0, totalNeutral: 0 }
      }
      const convItem = {
        sessionId: s.session_id, turnId: 0,
        userQuery: '', botAnswer: '',
        intent: '', intentCategory: '',
        timestamp: s.last_updated || new Date().toISOString(),
        sentimentScore: s.satisfaction_score ?? null,
        positivityRate: null,
        problemDiagnosis: s.reason || '',
        improvementSuggestion: s.suggestion || '',
        coreQuestion: '',
        sentimentLabel: '',
        categories: s.categories || [],
        messageCount: s.message_count || 0,
      }
      userMap[userName].sessions.push(convItem)
    })
    dlgUsers.value = Object.entries(userMap).map(([userId, uData]) => {
      const sessions = uData.sessions
      const lastSession = sessions[sessions.length - 1]
      const avgScore = sessions.reduce((s, c) => s + (c.sentimentScore || 0), 0) / Math.max(sessions.filter(c => c.sentimentScore != null).length, 1)
      return {
        id: userId, name: uData.name,
        lastTime: formatRelative(lastSession?.timestamp || new Date().toISOString()),
        msgCount: sessions.reduce((s, c) => s + (c.messageCount || 0), 0),
        satisfaction: Math.round(avgScore) || 85,
        sessions,
        positiveCount: uData.totalPositive, negativeCount: uData.totalNegative,
        neutralCount: uData.totalNeutral, totalCount: sessions.length,
      }
    })
    dlgLoading.value = false
  } catch(e) {
    console.warn('对话分析加载失败:', e.message)
    dlgError.value = '加载失败: ' + e.message; dlgLoading.value = false; dlgUsers.value = []
  }
}
// 加载单个会话的消息详情
async function loadSessionMessages(sessionId) {
  try {
    const res = await fetch('/api/v1/admin/dialogue/sessions/' + encodeURIComponent(sessionId) + '/messages', {
      headers: getAuthHeaders(),
      signal: AbortSignal.timeout(10000),
    })
    if (!res.ok) return null
    const resp = await res.json()
    return resp?.data?.messages || resp?.data || []
  } catch(_) { return null }
}

async function loadAnalysisData() {
  try {
    const res = await fetch('/api/v1/admin/dashboard/improvement-suggestions', {
      signal: AbortSignal.timeout(15000),
      headers: getAuthHeaders()
    })
    if (!res.ok) throw new Error('服务器错误: ' + res.status)
    const data = await res.json()
    if (data.suggestions && data.suggestions.length > 0) {
      analysisItems.value = data.suggestions.map(s => {
        let type = '优化建议', level = 'low'
        const priority = s.priority || ''
        if (priority === '紧急问题' || priority.includes('紧急')) { type = '紧急问题'; level = 'high' }
        else if (priority === '服务改进' || priority.includes('服务')) { type = '服务改进'; level = 'mid' }
        else if (priority === '优化建议' || priority.includes('优化')) { type = '优化建议'; level = 'low' }
        else if (priority === '正面发现' || priority.includes('正面')) { type = '正面保持'; level = 'low' }
        // 构建 parsed 以便点击展开查看详情
        const parsed = {}
        if (s.category) parsed['问题类别'] = s.category
        if (s.evidence) parsed['数据支撑'] = s.evidence
        if (s.suggestion) parsed['改进建议'] = s.suggestion
        if (s.impact) parsed['预期影响'] = s.impact
        if (s.affected_queries && s.affected_queries.length > 0) parsed['涉及问题'] = s.affected_queries.slice(0, 5).join(', ')
        return {
          type, level,
          text: s.evidence || s.title || '',
          summary: s.title || '',
          parsed,
        }
      })
    }
    analysisLoaded.value = true
  } catch(e) {
    console.warn('分析数据加载失败:', e.message); analysisLoaded.value = true
  }
}

// 监听路由变化，切换到 dashboard 时初始化图表，知识图谱由页面自行加载
watch(() => route.path, (newPath) => {
  if (newPath === '/dashboard') setTimeout(initCharts, 100)
  sidebarOverlay.value = false
  document.body.classList.toggle('dashboard-fullscreen', newPath === '/dashboard')
})

// ============================================================
// provide 所有状态和方法给子组件
// ============================================================
provide('currentTime', currentTime)
provide('toast', toast)
provide('showToast', showToast)
provide('sidebarOverlay', sidebarOverlay)
provide('toggleSidebar', toggleSidebar)

// 数字人
provide('avatarList', avatarList)

provide('selectedAvatarId', selectedAvatarId)
provide('selectedAvatarData', selectedAvatarData)
provide('currentAvatar', currentAvatar)
provide('voiceConfig', voiceConfig)
provide('sceneConfig', sceneConfig)
provide('isSaving', isSaving)
provide('syncStatus', syncStatus)
provide('lastUpdateTime', lastUpdateTime)
provide('showPhotoTips', showPhotoTips)
provide('vcnGroups', vcnGroups)
provide('selectAvatar', selectAvatar)
provide('saveConfig', saveConfig)
provide('resetConfig', resetConfig)
provide('refreshPhoto', refreshPhoto)

// 仪表盘
provide('kpiAll', kpiAll)
provide('topProvinces', topProvinces)
provide('hotQuestions', hotQuestions)
provide('mapLoading', mapLoading)
provide('initCharts', initCharts)

// 内容管理
provide('cmSpotName', cmSpotName)
provide('cmFileName', cmFileName)
provide('cmPreviewUrl', cmPreviewUrl)
provide('cmCanAdd', cmCanAdd)
provide('cmImages', cmImages)
provide('cmShowPreview', cmShowPreview)
provide('cmOnFileChange', cmOnFileChange)
provide('cmAddImage', cmAddImage)
provide('cmCopyRef', cmCopyRef)
provide('cmDelete', cmDelete)

// 知识库
provide('docList', docList)
provide('selectedDocId', selectedDocId)
provide('selectedDoc', selectedDoc)
provide('newDocType', newDocType)
provide('fileName', fileName)
provide('canUpload', canUpload)
provide('previewing', previewing)
provide('previewDone', previewDone)
provide('previewChunks', previewChunks)
provide('previewFileName', previewFileName)
provide('previewTotalChars', previewTotalChars)
provide('totalChars', totalChars)
provide('uploadError', uploadError)
provide('knoSearch', knoSearch)
provide('knoTypeFilter', knoTypeFilter)
provide('knoExtFilter', knoExtFilter)
provide('filteredDocs', filteredDocs)
provide('docTypeOptions', docTypeOptions)
provide('docExtOptions', docExtOptions)
provide('docIconBg', docIconBg)
provide('onFileChange', onFileChange)
provide('previewDoc', previewDoc)
provide('confirmUpload', confirmUpload)
provide('clearPreview', clearPreview)
provide('viewDocDetail', viewDocDetail)
provide('exportDocMD', exportDocMD)
provide('deleteDoc', deleteDoc)

// 文档播放器
provide('docViewerMode', docViewerMode)
provide('originalFileType', originalFileType)
provide('originalFileContent', originalFileContent)
provide('originalFileHtml', originalFileHtml)
provide('originalFileUrl', originalFileUrl)
provide('docZoomLevel', docZoomLevel)
provide('docViewerBusy', docViewerBusy)
provide('currentViewDoc', currentViewDoc)
provide('changeZoom', changeZoom)
provide('resetZoom', resetZoom)
provide('toggleFullscreen', toggleFullscreen)
provide('closeViewer', closeViewer)

// 对话分析
provide('dlgSearch', dlgSearch)
provide('dlgLoading', dlgLoading)
provide('dlgError', dlgError)
provide('filteredUsers', filteredUsers)
provide('selectedUserId', selectedUserId)
provide('selectedUser', selectedUser)
provide('selectUser', selectUser)

// 数据分析
provide('urgentItems', urgentItems)
provide('improveItems', improveItems)
provide('optimizeItems', optimizeItems)
provide('keepItems', keepItems)
provide('analysisLoaded', analysisLoaded)
provide('expandedSet', expandedSet)
provide('isOpen', isOpen)
provide('toggleItem', toggleItem)
provide('scrollToType', scrollToType)

// 系统设置
const toggleAuto = ref(true)
provide('toggleAuto', toggleAuto)

// ============================================================
// 用户建议（信箱）
// ============================================================
const mailboxOpen = ref(false)
const suggestions = ref([])
const suggestionUnread = ref(0)
const suggestionLoading = ref(false)

function toggleMailbox() {
  mailboxOpen.value = !mailboxOpen.value
  if (mailboxOpen.value && suggestions.value.length === 0) {
    loadSuggestions()
  }
}

async function loadSuggestions() {
  suggestionLoading.value = true
  try {
    const res = await fetch('/api/v1/admin/suggestions', {
      signal: AbortSignal.timeout(10000),
      headers: getAuthHeaders()
    })
    if (!res.ok) { suggestions.value = []; suggestionUnread.value = 0; return }
    const data = await res.json()
    suggestions.value = data.items || data.suggestions || []
    suggestionUnread.value = suggestions.value.filter(s => s.status === 'unread').length
  } catch(e) {
    console.warn('loadSuggestions failed:', e.message)
    suggestions.value = []
    suggestionUnread.value = 0
  } finally {
    suggestionLoading.value = false
  }
}

async function markAsRead(s) {
  if (s.status !== 'unread') return
  s.status = 'read'
  suggestionUnread.value = Math.max(0, suggestionUnread.value - 1)
  try {
    await fetch(`/api/v1/admin/suggestions/${encodeURIComponent(s.id)}/read`, {
      method: 'PUT', signal: AbortSignal.timeout(5000),
      headers: getAuthHeaders()
    })
  } catch(e) {
    console.warn('markAsRead failed:', e.message)
  }
}

function mailboxFormatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const dateStr = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
  if (dateStr === `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}`) {
    return `今天 ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
  const yesterday = new Date(now); yesterday.setDate(yesterday.getDate() - 1)
  const yStr = `${yesterday.getFullYear()}-${pad(yesterday.getMonth()+1)}-${pad(yesterday.getDate())}`
  if (dateStr === yStr) return `昨天 ${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${dateStr} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  updateTime()
  timer.value = setInterval(updateTime, 1000)
  _loadDocList()
  loadDashboardData().then((ok) => {
    if (ok !== false) dashboardTimer.value = setInterval(loadDashboardData, 60000)
  })
  loadDialogueData()
  loadAnalysisData()
  loadDigitalConfig()
  loadContentImages()
  loadVCNVoices()
  window.addEventListener('resize', onResizeCharts)
  document.addEventListener('click', _closeUserMenu)
  if (route.path === '/dashboard') setTimeout(initCharts, 200)
})
onUnmounted(() => {
  clearInterval(timer.value)
  if (dashboardTimer.value) clearInterval(dashboardTimer.value)
  window.removeEventListener('resize', onResizeCharts)
})

function onResizeCharts() {
  chartSentiment?.resize(); chartEmotion?.resize(); chartVisitor?.resize()
  chartCategory?.resize(); chartMap?.resize(); chartHotBar?.resize(); chartRadar?.resize()
}
</script>

<style>
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
:root {
  --bg: #080c14;
  --card: #0d1321;
  --card-hover: #111827;
  --border: #1e2d4a;
  --accent: #3b82f6;
  --accent-dim: rgba(59,130,246,0.15);
  --text: #e2e8f0;
  --text-dim: #64748b;
  --green: #10b981;
  --green-dim: rgba(16,185,129,0.15);
  --red: #ef4444;
  --red-dim: rgba(239,68,68,0.15);
  --amber: #f59e0b;
  --amber-dim: rgba(245,158,11,0.15);
  --cyan: #06b6d4;
  --sidebar-w: 220px;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg);
  color: var(--text);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes fadeIn { from { opacity:0; transform:translateY(8px); } to { opacity:1; transform:none; } }
@keyframes pulse { 0%,100% { opacity:1; } 50% { opacity:0.5; } }
</style>

<style scoped>
/* ===== 顶部导航 ===== */
.topbar {
  height: 56px;
  background: var(--card);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 32px;
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
}
.topbar-title { font-size: 16px; font-weight: 600; color: var(--text); white-space: nowrap; }
.topbar-title span { color: var(--cyan); margin-right: 6px; }
.topbar-nav { display: flex; gap: 4px; flex: 1; }
.topbar-nav a {
  padding: 6px 16px;
  border-radius: 6px;
  color: var(--text-dim);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}
.topbar-nav a:hover { color: var(--text); background: var(--accent-dim); }
.topbar-nav a.active,
.topbar-nav a.router-link-active { color: var(--cyan); background: var(--accent-dim); }
.topbar-time { font-size: 13px; color: var(--text-dim); font-variant-numeric: tabular-nums; }

/* ===== 主体布局 ===== */
.main { margin-top: 56px; display: flex; min-height: calc(100vh - 56px); overflow-y: auto; }

/* ===== 侧边栏 ===== */
.sidebar {
  width: var(--sidebar-w);
  background: var(--card);
  border-right: 1px solid var(--border);
  padding: 20px 0;
  position: fixed;
  top: 56px; left: 0; bottom: 0;
  overflow-y: auto;
  z-index: 90;
}
.sidebar-section { margin-bottom: 8px; }
.sidebar-section-title {
  font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em;
  color: var(--text-dim); padding: 8px 20px 4px;
}
.sidebar-link {
  display: flex; align-items: center; gap: 10px; padding: 10px 20px;
  color: var(--text-dim); text-decoration: none; font-size: 14px; transition: all 0.15s; cursor: pointer;
}
.sidebar-link:hover { color: var(--text); background: var(--accent-dim); }
.sidebar-link.active,
.sidebar-link.router-link-active { color: var(--cyan); background: var(--accent-dim); }
.sidebar-link svg { width: 18px; height: 18px; flex-shrink: 0; }

/* ===== 内容区 ===== */
.content { margin-left: var(--sidebar-w); flex: 1; padding: 24px; overflow-x: hidden; min-width: 0; }

/* ===== Toast ===== */
.toast {
  position: fixed; bottom: 24px; right: 24px; padding: 12px 20px;
  border-radius: 8px; font-size: 14px; z-index: 9999;
  transform: translateY(100px); opacity: 0; transition: all 0.3s;
  background: var(--card); border: 1px solid var(--border); color: var(--text);
}
.toast.show { transform: translateY(0); opacity: 1; }
.toast.success { border-color: var(--green); color: var(--green); }
.toast.error { border-color: var(--red); color: var(--red); }
</style>
