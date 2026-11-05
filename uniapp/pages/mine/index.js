// 用户菜单配置
const MENU_GROUPS = [
  {
    title: '导览服务',
    items: [
      { id: 'favorites', icon: '♥', iconColor: '#ff6b6b', name: '我的收藏', badge: null, arrow: true },
      { id: 'footprint', icon: '✿', iconColor: '#a855f7', name: '我的足迹', badge: null, arrow: true },
      { id: 'history', icon: '⏱', iconColor: '#54a0ff', name: '导览历史', badge: null, arrow: true },
      { id: 'questions', icon: '❓', iconColor: '#2dba98', name: '常见问题', badge: null, arrow: true },
    ]
  },
  {
    title: '旅友圈',
    items: [
      { id: 'myposts', icon: '♫', iconColor: '#ff9f43', name: '我的帖子', badge: '3', arrow: true },
      { id: 'mylikes', icon: '♡', iconColor: '#ee5a24', name: '我的点赞', badge: null, arrow: true },
      { id: 'mycomments', icon: '◔', iconColor: '#10ac84', name: '我的评论', badge: null, arrow: true },
    ]
  },
  {
    title: '设置',
    items: [
      { id: 'aging', icon: '👤', iconColor: '#5f27cd', name: '适老化模式', badge: '未开启', arrow: true },
      { id: 'notifications', icon: '🔔', iconColor: '#00d2d3', name: '消息通知', badge: null, arrow: true },
      { id: 'privacy', icon: '🔒', iconColor: '#1dd1a1', name: '隐私设置', badge: null, arrow: true },
      { id: 'about', icon: 'ℹ', iconColor: '#576574', name: '关于我们', badge: null, arrow: true },
    ]
  }
]

// 快速服务
const QUICK_SERVICES = [
  { id: 'service_ticket', icon: '🎫', name: '我的门票', color: '#3b82f6', badge: null },
  { id: 'service_order', icon: '📋', name: '订单记录', color: '#8b5cf6', badge: '2' },
  { id: 'service_coupon', icon: '🎟', name: '优惠券', color: '#f59e0b', badge: '3' },
  { id: 'service_feedback', icon: '💡', name: '意见反馈', color: '#06b6d4', badge: null },
  { id: 'service_help', icon: '❓', name: '帮助中心', color: '#64748b', badge: null },
  { id: 'service_share', icon: '🔗', name: '分享应用', color: '#10ac84', badge: null },
]

// 省份访问数据
const INITIAL_PROVINCES = [
  { code: 'hb', name: '黑龙江', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'jl', name: '吉林', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'ln', name: '辽宁', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'bj', name: '北京', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'tj', name: '天津', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'heb', name: '河北', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'sx', name: '山西', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'nmg', name: '内蒙古', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'sx2', name: '陕西', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'gs', name: '甘肃', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'qh', name: '青海', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'nx', name: '宁夏', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'xj', name: '新疆', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'sd', name: '山东', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'js', name: '江苏', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'ah', name: '安徽', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'zj', name: '浙江', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'fj', name: '福建', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'jx', name: '江西', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'sh', name: '上海', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'henan', name: '河南', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'hubei', name: '湖北', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'hunan', name: '湖南', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'gd', name: '广东', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'gx', name: '广西', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'hn', name: '海南', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'sc', name: '四川', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'cq', name: '重庆', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'gz', name: '贵州', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'yn', name: '云南', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'xz', name: '西藏', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'hk', name: '香港', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'mo', name: '澳门', cities: [], visitCount: 0, totalDays: 0 },
  { code: 'tw', name: '台湾', cities: [], visitCount: 0, totalDays: 0 },
]

// 演示数据 - 模拟用户访问过的省份
const DEMO_VISITED = {
  'hubei': { cities: ['武汉', '宜昌', '恩施'], visitCount: 5, totalDays: 12 },
  'gd': { cities: ['广州', '深圳', '珠海'], visitCount: 8, totalDays: 15 },
  'bj': { cities: ['北京'], visitCount: 3, totalDays: 7 },
  'sh': { cities: ['上海'], visitCount: 2, totalDays: 4 },
  'zj': { cities: ['杭州', '乌镇'], visitCount: 4, totalDays: 6 },
  'sc': { cities: ['成都', '九寨沟'], visitCount: 3, totalDays: 8 },
  'gx': { cities: ['桂林', '阳朔'], visitCount: 2, totalDays: 5 },
  'yn': { cities: ['大理', '丽江', '昆明'], visitCount: 4, totalDays: 10 },
  'js': { cities: ['南京', '苏州'], visitCount: 3, totalDays: 6 },
  'fj': { cities: ['厦门', '福州'], visitCount: 2, totalDays: 5 },
}

export default {
  data() {
    return {
      userInfo: {
        avatar: '/static/assets/avatar_default.png',
        nickname: '游客用户',
        level: 'V0',
        levelPoints: 0,
        points: 0,
        scenicName: '东湖风景区',
        isLoggedIn: false
      },

      menuGroups: MENU_GROUPS,
      quickServices: QUICK_SERVICES,

      currentYear: new Date().getFullYear(),

      ecoData: {
        carbonReduced: 12.5,
        walk: 8,
        bike: 3,
        bus: 5,
        monthCarbon: 3.2,
        rank: 'Lv.2'
      },

      reportData: {
        travelCount: 0,
        spotsCount: 0,
        totalHours: 0,
        likesCount: 0,
        percentRank: 0
      },

      // 点亮城市相关数据
      provinceData: [],
      visitedProvinces: [],
      selectedProvince: null,
      mapScale: 1,
      mapTranslateX: 0,
      mapTranslateY: 0,
      lastTouchDistance: 0,
      lastTouchCenter: null,

      statsItems: [
        { label: '积分', value: 0 },
        { label: '关注', value: 0 },
        { label: '收藏', value: 0 },
        { label: '足迹', value: 0 },
        { label: '帖子', value: 0 }
      ],

      // 打卡记录
      showCheckinRecords: false,
      checkinRecords: [],
    }
  },

  onLoad() {
    this.loadUserInfo()
    this.loadProvinceData()
  },

  onShow() {
    this.loadUserInfo()
    this.loadProvinceData()
    this.loadCheckinRecords()
  },

  methods: {
    loadUserInfo() {
      const token = uni.getStorageSync('token')
      const storedUserInfo = uni.getStorageSync('userInfo')
      if (token && storedUserInfo) {
        this.userInfo.isLoggedIn = true
        this.userInfo.nickname = storedUserInfo.nickname || storedUserInfo.username || '用户'
        this.userInfo.level = storedUserInfo.level || 'V1'
        this.userInfo.points = storedUserInfo.points || 0
        this.userInfo.levelPoints = this.userInfo.points
        this.statsItems = [
          { label: '积分', value: this.userInfo.points },
          { label: '关注', value: this.getFollowCount() },
          { label: '收藏', value: 5 },
          { label: '足迹', value: this.visitedProvinces.length },
          { label: '帖子', value: 3 }
        ]
        this.reportData = {
          travelCount: 12,
          spotsCount: 7,
          totalHours: 48,
          likesCount: 86,
          percentRank: 73
        }
        this.ecoData = {
          carbonReduced: 12.5,
          walk: 8,
          bike: 3,
          bus: 5,
          monthCarbon: 3.2,
          rank: 'Lv.2'
        }
      } else {
        this.userInfo.isLoggedIn = false
        this.userInfo.level = 'V0'
        this.userInfo.levelPoints = 0
        this.userInfo.points = 0
        this.statsItems = [
          { label: '积分', value: 0 },
          { label: '关注', value: this.getFollowCount() },
          { label: '收藏', value: 0 },
          { label: '足迹', value: this.visitedProvinces.length },
          { label: '帖子', value: 0 }
        ]
      }
    },

    // 获取关注数量
    getFollowCount() {
      try {
        const stored = uni.getStorageSync('followed_spots') || '[]'
        return JSON.parse(stored).length
      } catch { return 0 }
    },

    // 加载省份数据
    loadProvinceData() {
      // 初始化省份数据
      this.provinceData = JSON.parse(JSON.stringify(INITIAL_PROVINCES))

      // 加载已访问数据（优先从本地存储读取）
      const stored = uni.getStorageSync('province_visited')
      const visitedData = stored ? JSON.parse(stored) : DEMO_VISITED

      // 合并数据
      this.provinceData.forEach(p => {
        if (visitedData[p.code]) {
          p.cities = visitedData[p.code].cities || []
          p.visitCount = visitedData[p.code].visitCount || 0
          p.totalDays = visitedData[p.code].totalDays || 0
        }
      })

      // 计算已访问省份
      this.visitedProvinces = this.provinceData.filter(p => p.visitCount > 0)
        this.statsItems = [
          { label: '积分', value: this.userInfo.points },
          { label: '关注', value: this.getFollowCount() },
          { label: '收藏', value: 5 },
          { label: '足迹', value: this.visitedProvinces.length },
          { label: '帖子', value: 3 }
        ]
    },

    // 检查省份是否已访问
    isVisited(provinceCode) {
      const province = this.provinceData.find(p => p.code === provinceCode)
      return province && province.visitCount > 0
    },

    // 选择省份
    selectProvince(code, name) {
      const province = this.provinceData.find(p => p.code === code)
      if (province && province.visitCount > 0) {
        this.selectedProvince = province
      } else {
        uni.showToast({ title: '还没有去过 ' + name, icon: 'none' })
      }
    },

    // 关闭省份详情
    closeProvinceDetail() {
      this.selectedProvince = null
    },

    // 获取城市数量
    getCityCount(province) {
      return province && province.cities ? province.cities.length : 0
    },

    // 缩放地图
    zoomMap(direction) {
      const minScale = 1
      const maxScale = 3
      const step = 0.3

      if (direction > 0) {
        this.mapScale = Math.min(maxScale, this.mapScale + step)
      } else {
        this.mapScale = Math.max(minScale, this.mapScale - step)
        // 重置位置当缩小到原始大小时
        if (this.mapScale === 1) {
          this.mapTranslateX = 0
          this.mapTranslateY = 0
        }
      }
    },

    // 地图触摸事件 - 双指缩放
    onMapTouchStart(e) {
      if (e.touches.length === 2) {
        const touch1 = e.touches[0]
        const touch2 = e.touches[1]
        this.lastTouchDistance = Math.hypot(touch2.clientX - touch1.clientX, touch2.clientY - touch1.clientY)
        this.lastTouchCenter = {
          x: (touch1.clientX + touch2.clientX) / 2,
          y: (touch1.clientY + touch2.clientY) / 2
        }
      }
    },

    onMapTouchMove(e) {
      if (e.touches.length === 2) {
        const touch1 = e.touches[0]
        const touch2 = e.touches[1]
        const distance = Math.hypot(touch2.clientX - touch1.clientX, touch2.clientY - touch1.clientY)

        if (this.lastTouchDistance > 0) {
          const scaleChange = distance / this.lastTouchDistance
          const newScale = this.mapScale * scaleChange

          // 限制缩放范围
          if (newScale >= 1 && newScale <= 3) {
            this.mapScale = newScale
          }

          // 更新中心点位置实现平移
          const center = {
            x: (touch1.clientX + touch2.clientX) / 2,
            y: (touch1.clientY + touch2.clientY) / 2
          }

          if (this.lastTouchCenter) {
            const dx = center.x - this.lastTouchCenter.x
            const dy = center.y - this.lastTouchCenter.y
            this.mapTranslateX += dx * 0.5
            this.mapTranslateY += dy * 0.5
          }

          this.lastTouchCenter = center
          this.lastTouchDistance = distance
        }

        e.preventDefault()
      }
    },

    onMapTouchEnd(e) {
      if (e.touches.length < 2) {
        this.lastTouchDistance = 0
        this.lastTouchCenter = null
      }
    },

    goLogin() {
      if (!this.userInfo.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/index' })
      }
    },

    editProfile() {
      if (!this.userInfo.isLoggedIn) {
        this.goLogin()
        return
      }
      uni.showToast({ title: '编辑资料功能开发中', icon: 'none' })
    },

    onMenuTap(menuId) {
      if (!this.userInfo.isLoggedIn && menuId !== 'about' && menuId !== 'footprint') {
        uni.showModal({
          title: '提示',
          content: '请先登录后再操作',
          confirmText: '去登录',
          success: (res) => {
            if (res.confirm) this.goLogin()
          }
        })
        return
      }

      const toastMap = {
        favorites: '我的收藏',
        footprint: '我的足迹',
        history: '导览历史',
        questions: '常见问题',
        myposts: '我的帖子',
        mylikes: '我的点赞',
        mycomments: '我的评论',
        aging: '适老化模式',
        notifications: '消息通知',
        privacy: '隐私设置',
        about: '关于我们',
        service_ticket: '我的门票',
        service_order: '订单记录',
        service_coupon: '优惠券',
        service_feedback: '意见反馈',
        service_help: '帮助中心',
        service_share: '分享应用'
      }

      if (menuId === 'aging') {
        uni.navigateTo({ url: '/pages/aging/index' })
        return
      }

      if (menuId === 'about') {
        this.showAbout()
        return
      }

      if (menuId === 'footprint') {
        this.loadCheckinRecords()
        this.showCheckinRecords = true
        return
      }

      uni.showToast({ title: toastMap[menuId] || menuId, icon: 'none' })
    },

    // ========== 打卡记录 ==========
    loadCheckinRecords() {
      try {
        var raw = uni.getStorageSync('suixing_checkins') || '[]'
        this.checkinRecords = JSON.parse(raw)
      } catch (e) {
        this.checkinRecords = []
      }
    },

    closeCheckinRecords() {
      this.showCheckinRecords = false
    },

    previewCheckinPhoto(url) {
      if (!url) return
      uni.previewImage({ urls: [url], current: url })
    },

    getCheckinColor(title, index) {
      var POOL = ['#F44336','#E91E63','#9C27B0','#673AB7','#3F51B5','#2196F3','#0097A7','#00897B','#43A047','#EF6C00']
      if (!title) return POOL[index % POOL.length]
      var hash = 0
      for (var i = 0; i < title.length; i++) hash = title.charCodeAt(i) + ((hash << 5) - hash)
      return POOL[Math.abs(hash) % POOL.length]
    },

    getCheckinLetter(title) {
      return title ? title.charAt(0) : '?'
    },

    showAbout() {
      uni.showModal({
        title: '关于我们',
        content: '随行数字人导览 v1.0.0\n\n您的智能景区虚拟向导\n\n© 2026 随行',
        showCancel: false
      })
    },

    onAvatarError() {
      this.userInfo.avatar = '/static/assets/avatar_default.png'
    },

    logout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (!res.confirm) return
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.removeStorageSync('nickname')
          uni.removeStorageSync('level')
          uni.removeStorageSync('points')
          this.userInfo = {
            avatar: '/static/assets/avatar_default.png',
            nickname: '游客用户',
            level: 'V0',
            levelPoints: 0,
            points: 0,
            scenicName: '东湖风景区',
            isLoggedIn: false
          }
          this.statsItems = [
            { label: '积分', value: 0 },
            { label: '关注', value: 0 },
            { label: '收藏', value: 0 },
            { label: '足迹', value: this.visitedProvinces.length },
            { label: '帖子', value: 0 }
          ]
          uni.showToast({ title: '已退出登录', icon: 'success' })
        }
      })
    },

    callService() {
      uni.makePhoneCall({
        phoneNumber: '027-12345678',
        fail: () => {
          uni.showToast({ title: '拨打失败', icon: 'none' })
        }
      })
    }
  }
}
