import { getCurrentDistrict } from '@/utils/location.js'
import config from '@/config/index.js'

console.log('[MAP] map/index.js 模块开始加载')

// 天气现象 → 图标映射
const WEATHER_ICONS = {
  '晴': '☀️', '多云': '⛅', '阴': '☁️',
  '小雨': '🌦', '中雨': '🌧', '大雨': '🌧', '阵雨': '🌦',
  '雷阵雨': '⛈', '暴雨': '🌊', '雪': '🌨', '小雪': '🌨',
  '中雪': '❄️', '大雪': '❄️', '雾': '🌫', '霾': '🌫',
  '浮尘': '🌫', '扬沙': '🌫', '大风': '💨', '风': '💨'
}

// ===================== 数据定义（必须在组件外部）=====================

const SCENIC_POIS = [
  { id: 1,  name: '景区主入口',      type: 'entrance',  latitude: 30.557410, longitude: 114.301810, description: '主入口 · 开放时间 08:00-18:00',          phone: '027-12345678' },
  { id: 2,  name: '数字人服务中心',  type: 'service',   latitude: 30.558010, longitude: 114.302410, description: '提供数字人导览服务，可租赁导览设备',      phone: '027-12345679' },
  { id: 3,  name: '山顶观景台',      type: 'attraction', latitude: 30.560010, longitude: 114.304010, description: '海拔480米 · 俯瞰全景区最佳位置',            phone: '' },
  { id: 4,  name: '游客餐厅',        type: 'food',       latitude: 30.557810, longitude: 114.303010, description: '中式快餐 · 可容纳300人同时就餐',            phone: '027-12345680' },
  { id: 5,  name: '景区卫生间',      type: 'toilet',     latitude: 30.558610, longitude: 114.303510, description: 'AAA旅游厕所 · 24小时开放',                  phone: '' },
  { id: 6,  name: '云端索道·上站',  type: 'attraction', latitude: 30.561010, longitude: 114.305010, description: '索道上站 · 全程约15分钟',                   phone: '027-12345681' },
  { id: 7,  name: '文创商店',        type: 'shop',       latitude: 30.557410, longitude: 114.302810, description: '景区纪念品、明信片、手工艺品',              phone: '027-12345682' },
  { id: 8,  name: '景区出口',        type: 'exit',       latitude: 30.556810, longitude: 114.302010, description: '出口通道 · 开放时间 08:00-19:00',           phone: '' },
  { id: 9,  name: '数字人导航站·A', type: 'digital',    latitude: 30.559410, longitude: 114.304510, description: '数字人导览点A · 支持扫码启动导览',        phone: '' },
  { id: 10, name: '数字人导航站·B', type: 'digital',    latitude: 30.560510, longitude: 114.303510, description: '数字人导览点B · 覆盖山顶路线',              phone: '' },
  { id: 11, name: '游客休息亭',      type: 'rest',       latitude: 30.559010, longitude: 114.303210, description: '免费休息点 · 提供饮水机',                   phone: '' },
  { id: 12, name: '景区急救站',      type: 'medical',    latitude: 30.558210, longitude: 114.302610, description: '24小时急救 · 紧急情况请拨打120',             phone: '027-12345683' },
]

// POI类型配置（使用 uView 图标名）
const POI_TYPE_CONFIG = {
  attraction: { color: '#FF6B6B', name: '景点',     icon: 'star', markerColor: '#FF6B6B' },
  food:       { color: '#FF9F43', name: '餐饮',     icon: 'shopping-cart', markerColor: '#FF9F43' },
  toilet:     { color: '#54A0FF', name: '卫生间',   icon: 'minus-circle', markerColor: '#54A0FF' },
  shop:       { color: '#A855F7', name: '商店',     icon: 'bag', markerColor: '#A855F7' },
  entrance:   { color: '#10AC84', name: '入口',     icon: 'checkmark-circle', markerColor: '#10AC84' },
  exit:       { color: '#EE5A24', name: '出口',     icon: 'close-circle', markerColor: '#EE5A24' },
  service:    { color: '#0ABDE3', name: '服务',     icon: 'info-circle', markerColor: '#0ABDE3' },
  digital:    { color: '#2ECC71', name: '数字人',   icon: 'mic', markerColor: '#2ECC71' },
  rest:       { color: '#64748B', name: '休息点',   icon: 'clock', markerColor: '#64748B' },
  medical:    { color: '#EF4444', name: '急救站',   icon: 'plus-circle', markerColor: '#EF4444' },
  destination:{ color: '#2dba98', name: '目的地',   icon: 'navigation', markerColor: '#2dba98' },
}

// 生成彩色标记图标（SVG Data URI）
function getMarkerIcon(type) {
  var cfg = POI_TYPE_CONFIG[type] || POI_TYPE_CONFIG.attraction
  var color = cfg.markerColor
  // 每种类型不同的图标形状和符号
  var icons = {
    entrance:  { shape: 'M22 10L10 30h24z', symbol: '🏛', size: 14, y: 24 },
    exit:      { shape: 'M22 2C10 2 2 12 2 24c0 14 20 26 20 26s20-12 20-26C42 12 34 2 22 2z', symbol: '🚪', size: 12, y: 24 },
    food:      { shape: 'M22 10L10 30h24z', symbol: '🍽', size: 12, y: 24 },
    shop:      { shape: 'M22 2C10 2 2 12 2 24c0 14 20 26 20 26s20-12 20-26C42 12 34 2 22 2z', symbol: '🛍', size: 12, y: 24 },
    toilet:    { shape: 'M22 10L10 30h24z', symbol: '🚻', size: 12, y: 24 },
    service:   { shape: 'M22 2A20 20 0 1 0 22 42a20 20 0 0 0 0-40z', symbol: 'ℹ', size: 14, y: 26 },
    digital:   { shape: 'M22 10L10 30h24z', symbol: '🤖', size: 12, y: 24 },
    rest:      { shape: 'M22 2C10 2 2 12 2 24c0 14 20 26 20 26s20-12 20-26C42 12 34 2 22 2z', symbol: '💺', size: 12, y: 24 },
    medical:   { shape: 'M22 10L10 30h24z', symbol: '🏥', size: 12, y: 24 },
    attraction:{ shape: 'm22 2 7 14 16 2-11 10 3 15-15-7-15 7 3-15L-1 18l16-2z', symbol: '⭐', size: 10, y: 22 },
    destination:{ shape: 'M22 2C10 2 2 12 2 24c0 14 20 26 20 26s20-12 20-26C42 12 34 2 22 2z', symbol: '📍', size: 12, y: 24 }
  }
  var icon = icons[type] || icons.attraction
  var svg = '<svg xmlns="http://www.w3.org/2000/svg" width="44" height="52" viewBox="0 0 44 52">'
    + '<path d="' + icon.shape + '" fill="' + color + '" stroke="#fff" stroke-width="2"/>'
    + '<text x="22" y="' + icon.y + '" text-anchor="middle" font-size="' + icon.size + '" fill="#fff">' + icon.symbol + '</text>'
    + '</svg>'
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
}

// 分类配置（使用 uView 图标名）
const CATEGORIES = [
  { key: 'all',        name: '全部',   icon: 'grid' },
  { key: 'attraction', name: '景点',   icon: 'star' },
  { key: 'food',       name: '餐饮',   icon: 'shopping-cart' },
  { key: 'shop',       name: '商店',   icon: 'bag' },
  { key: 'toilet',     name: '卫生间', icon: 'minus-circle' },
  { key: 'digital',    name: '数字人', icon: 'mic' },
  { key: 'service',    name: '服务',   icon: 'info-circle' },
  { key: 'rest',       name: '休息点', icon: 'clock' },
]

// 计算两点间距离（米）
function getDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLon = (lon2 - lon1) * Math.PI / 180
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) ** 2
  return Math.round(R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)))
}

// 格式化距离
function formatDistance(meters) {
  if (meters < 1000) return meters + '米'
  return (meters / 1000).toFixed(1) + '公里'
}

// 格式化时间（秒 → X小时X分钟X秒）
function formatDuration(seconds) {
  if (seconds < 60) return seconds + '秒'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  let result = ''
  if (h > 0) result += h + '小时'
  if (m > 0) result += m + '分钟'
  if (s > 0 && h === 0) result += s + '秒'
  return result
}

// ===================== 组件 =====================

export default {
  data() {
    return {
      district: '获取中…',
      mapCenter: { latitude: 30.558510, longitude: 114.303010 },
      mapScale: 16,
      mapType: 'standard',
      keyword: '',
      locationLoading: false,
      userLocation: null,

      // POI（初始为空，加载附近后填充）
      markers: [],
      activePolyline: [],
      selectedPoi: null,
      allPois: [],
      filteredPois: [],
      nearbyPois: [],

      // 分类
      categories: CATEGORIES,
      activeCategory: 'all',

      // ========== 底部功能网格 ==========
      quickTools: [
        { key: 'attraction', name: '景点', icon: 'star', bg: '#FF6B6B' },
        { key: 'food', name: '餐饮', icon: 'shopping-cart', bg: '#FF9F43' },
        { key: 'toilet', name: '卫生间', icon: 'minus-circle', bg: '#54A0FF' },
        { key: 'shop', name: '商店', icon: 'bag', bg: '#A855F7' },
        { key: 'service', name: '服务', icon: 'info-circle', bg: '#0ABDE3' },
        { key: 'digital', name: '数字人', icon: 'mic', bg: '#2ECC71' },
        { key: 'rest', name: '休息', icon: 'clock', bg: '#64748B' },
        { key: 'medical', name: '急救', icon: 'plus-circle', bg: '#EF4444' },
        { key: 'entrance', name: '入口', icon: 'checkmark-circle', bg: '#10AC84' },
        { key: 'exit', name: '出口', icon: 'close-circle', bg: '#EE5A24' },
      ],

      // 热门标签
      hotTags: ['景区入口', '山顶观景台', '游客餐厅', '索道站', '文创商店'],

      // ========== 搜索相关 ==========
      searchResults: [],
      showSearchPanel: false,
      searchFocused: false,
      searchLoading: false,
      hasSearched: false,

      // ========== 导航相关 ==========
      isNavigating: false,
      navTarget: null,
      navProgress: 0,
      currentDistance: 0,
      estimatedTime: 0,
      totalDistance: 0,
      navIcon: '↑',
      navInstruction: '向前行驶',
      arrived: false,
      // 导航模式
      navMode: 'walking',         // walking | driving | cycling
      navModeLabel: '步行',
      // 路线详情
      routeSteps: [],             // 导航步骤列表
      currentStepIndex: 0,       // 当前步骤索引
      routePolyline: [],          // 完整路线点列表
      remainingPolyline: [],       // 剩余路线（用于绘制）
      navLocationMarker: null,    // 当前位置标记
      bottomSheetCollapsed: false, // 底部面板收起状态
      sheetSnapIndex: 2,          // 0=收起 1=中等 2=展开（默认展开）
      showRouteOptions: false,    // 是否显示路线选择
      routeEstimates: {           // 各模式预估时间
        driving: '--',
        walking: '--',
        cycling: '--'
      },
      completedDistance: 0,       // 已走距离

      // ========== 天气 ==========
      weatherCity: '无锡',
      weatherSummary: '',           // "今日14°至23°，发布雷电黄色预警"
      weatherCurrentTemp: '',       // 当前温度
      weatherCurrentIcon: '☀️',    // 当前天气图标
      weatherWind: '',              // 风向
      weatherWindPower: '',         // 风力
      weatherHumidity: '',          // 湿度
      weatherHourly: [],            // 逐小时预报 [{time, icon, temp}]
      weatherLoaded: false,
    }
  },

  computed: {
    includedPoints() {
      console.log('[MAP] includedPoints 计算')
      const pts = []
      if (this.userLocation) {
        pts.push({ latitude: this.userLocation.latitude, longitude: this.userLocation.longitude })
      }
      if (this.navLocationMarker) {
        pts.push({ latitude: this.navLocationMarker.latitude, longitude: this.navLocationMarker.longitude })
      }
      this.markers.forEach(m => pts.push({ latitude: m.latitude, longitude: m.longitude }))
      return pts
    }
  },

  onLoad(options) {
    console.log('[MAP] onLoad 开始')
    try {
      console.log('[MAP] 调用 initLocation')
      this.initLocation()
      // 不预加载图标标记，用户在点击分类后才会显示
      // 加载天气
      this.loadMapWeather()

      // 外部跳转携带导航参数，自动开始导航
      if (options && options.name && options.lat && options.lng) {
        console.log('[MAP] 收到导航参数:', options)
        setTimeout(() => {
          const poi = {
            id: 99999,
            name: decodeURIComponent(options.name),
            latitude: parseFloat(options.lat),
            longitude: parseFloat(options.lng),
            type: options.type || 'attraction',
            description: options.desc ? decodeURIComponent(options.desc) : '',
            phone: options.phone || ''
          }
          this.openPoiPanel(poi)
          setTimeout(() => {
            this.startNavigation(poi, this.navMode)
          }, 300)
        }, 1500)
      }
      console.log('[MAP] onLoad 完成')
    } catch (e) {
      console.error('[MAP] onLoad 捕获异常:', e)
    }
  },

  onShow() {
    console.log('[MAP] onShow 开始')
    try {
      this.refreshUserLocation()
      console.log('[MAP] onShow 完成')
    } catch (e) {
      console.error('[MAP] onShow 捕获异常:', e)
    }
  },

  onReady() {
    console.log('[MAP] onReady 开始')
    try {
      this.buildMarkers(this.filteredPois)
      console.log('[MAP] onReady 完成')
    } catch (e) {
      console.error('[MAP] onReady 捕获异常:', e)
    }
  },

  methods: {
    // ========== 定位 ==========
    async initLocation() {
      console.log('[MAP] initLocation 开始')
      this.locationLoading = true
      try {
        const data = await getCurrentDistrict()
        console.log('[MAP] getCurrentDistrict 返回:', data)
        this.locationLoading = false
        if (data.location) {
          this.userLocation = data.location
          this.district = data.district || '当前位置'
          this.mapCenter = {
            latitude: data.location.latitude,
            longitude: data.location.longitude
          }
          // 定位成功后：搜索周边POI
          this.loadNearbyPoisFromAPI(data.location)
        } else {
          this.district = '当前位置'
        }
        console.log('[MAP] initLocation 完成')
      } catch (e) {
        console.error('[MAP] initLocation 异常:', e)
        this.locationLoading = false
        this.district = '当前位置'
      }
    },

    async refreshUserLocation() {
      const data = await getCurrentDistrict()
      if (data.location) {
        this.userLocation = data.location
        if (this.selectedPoi) {
          const d = getDistance(
            this.userLocation.latitude, this.userLocation.longitude,
            this.selectedPoi.latitude, this.selectedPoi.longitude
          )
          this.selectedPoi = { ...this.selectedPoi, distance: d }
        }
        this.updateNearbyPoisDistance()
      }
    },

    backToLocation() {
      uni.showToast({ title: '正在定位…', icon: 'loading', duration: 1000 })
      uni.getLocation({
        type: 'gcj02',
        isHighAccuracy: true,
        success: (res) => {
          const gps = { latitude: res.latitude, longitude: res.longitude }
          this.userLocation = gps
          this.mapCenter = { latitude: res.latitude, longitude: res.longitude }
          this.mapScale = 17
          uni.showToast({ title: '已回到位置', icon: 'success', duration: 1000 })
          console.log('[MAP] 回到位置:', gps)
        },
        fail: () => {
          uni.showToast({ title: '定位失败', icon: 'none' })
        }
      })
    },

    // 3档底部面板：收起 → 中等 → 展开 → 收起（点击用）
    toggleBottomSheet() {
      this.sheetSnapIndex = (this.sheetSnapIndex + 1) % 3
    },

    // 滑动切换面板（拖条触摸）
    sheetTouchStart(e) {
      this._sheetStartY = e.touches[0].clientY
    },
    sheetTouchMove(e) {
      if (!this._sheetStartY) return
      const deltaY = this._sheetStartY - e.touches[0].clientY
      const screenH = uni.getSystemInfoSync().windowHeight
      // deltaY > 0 上滑展开，deltaY < 0 下滑收起
      if (deltaY > screenH * 0.15) {
        this.sheetSnapIndex = 2
        this._sheetStartY = null
      } else if (deltaY < -screenH * 0.15) {
        this.sheetSnapIndex = 0
        this._sheetStartY = null
      }
    },
    sheetTouchEnd() {
      this._sheetStartY = null
    },

    // ========== 地图操作 ==========
    zoomIn() {
      if (this.mapScale < 19) this.mapScale++
    },
    zoomOut() {
      if (this.mapScale > 5) this.mapScale--
    },

    toggleMapType() {
      this.mapType = this.mapType === 'standard' ? 'satellite' : 'standard'
      uni.showToast({ title: '地图类型已切换', icon: 'none' })
    },

    onRegionChange(e) {
      // 拖动地图后更新中心点
      if (e.detail.causedBy === 'drag' && !this.isNavigating) {
        // 可选：更新中心点坐标
      }
    },

    // 地图加载失败时触发
    onMapError(e) {
      console.error('地图组件错误:', e)
      uni.showToast({ title: '地图加载失败，请在设置中开启位置权限', icon: 'none', duration: 3000 })
    },

    // ========== Markers ==========
    buildMarkers(pois) {
      console.log('[MAP] buildMarkers 开始, pois数量:', pois ? pois.length : 0)
      try {
        if (!pois || pois.length === 0) {
          // 空时不覆盖已有markers
          return
        }
        const marks = pois.map(poi => {
          const cfg = POI_TYPE_CONFIG[poi.type] || POI_TYPE_CONFIG.attraction
          return {
            id: poi.id,
            latitude: poi.latitude,
            longitude: poi.longitude,
            width: 44,
            height: 52,
            iconPath: getMarkerIcon(poi.type),
            callout: {
              content: cfg.name + ' · ' + poi.name,
              color: '#333',
              fontSize: 13,
              borderRadius: 10,
              padding: 8,
              display: 'ALWAYS',
              bgColor: 'rgba(255,255,255,0.97)',
              borderColor: cfg.markerColor,
              borderWidth: 2
            },
            anchor: { x: 0.5, y: 1 }
          }
        })

        this.markers = marks
        console.log('[MAP] buildMarkers 完成, markers数量:', marks.length)
      } catch (e) {
        console.error('[MAP] buildMarkers 异常:', e)
      }
    },

    // ========== POI点击 ==========
    onMarkerTap(e) {
      // 优先从搜索结果中找（真实POI）
      let poi = this.searchResults.find(p => p.id === e.detail.markerId)
      // 再从景区POI中找
      if (!poi) poi = this.allPois.find(p => p.id === e.detail.markerId)
      if (poi) this.openPoiPanel(poi)
    },

    onCalloutTap(e) {
      let poi = this.searchResults.find(p => p.id === e.detail.markerId)
      if (!poi) poi = this.allPois.find(p => p.id === e.detail.markerId)
      if (poi) this.openPoiPanel(poi)
    },

    openPoiPanel(poi) {
      let distance = null
      if (this.userLocation) {
        distance = getDistance(
          this.userLocation.latitude, this.userLocation.longitude,
          poi.latitude, poi.longitude
        )
      }
      this.selectedPoi = { ...poi, distance, collected: poi.collected || false }
      this.mapCenter = { latitude: poi.latitude, longitude: poi.longitude }
      this.mapScale = 17
      this.showSearchPanel = false
      this.keyword = ''
      // 绘制到标记的连线
      this.drawRouteLine(poi)
      // 加载附近同类
      this.loadNearbyPois(poi)
    },

    // 返回初始地图状态
    backToDefault() {
      this.showSearchPanel = false
      this.selectedPoi = null
      this.showRouteOptions = false
      this.activePolyline = []
      this.keyword = ''
      this.nearbyPois = []
      this.markers = [] // 返回默认时不显示图标
    },

    closePoiPanel() {
      this.selectedPoi = null
      this.activePolyline = []
      this.nearbyPois = []
      this.showRouteOptions = false
      this.buildMarkers(this.filteredPois)
    },

    // 显示路线选择面板（并行获取3种出行方式的预估时间）
    // 显示路线选择面板（并行获取3种出行方式的预估时间）
    async showRouteForPoi(poi) {
      this.showRouteOptions = true
      this.navTarget = poi
      // 默认驾车模式
      this.navMode = 'driving'
      this.navModeLabel = '驾车'

      // 并行获取3种模式的路线时间
      const modes = ['driving', 'walking', 'cycling']
      const results = await Promise.all(modes.map(m =>
          this.fetchRoute(m).then(r => ({ status: 'fulfilled', value: r }))
              .catch(() => ({ status: 'rejected' }))
      ))

      for (let i = 0; i < modes.length; i++) {
        const result = results[i]
        if (result.status === 'fulfilled' && result.value) {
          this.routeEstimates[modes[i]] = result.value.timeDisplay || '--'
          // 默认显示驾车的路线
          if (modes[i] === 'driving' && result.value.polyline && result.value.polyline.length > 1) {
            this.activePolyline = [{
              points: result.value.polyline,
              color: '#2dba98',
              colorList: ['#2dba98'],
              width: 8,
              arrowLine: true,
              borderColor: '#fff',
              borderWidth: 2
            }]
          }
        } else {
          this.routeEstimates[modes[i]] = '--'
        }
      }
    },

    closeRouteOptions() {
      this.showRouteOptions = false
      this.activePolyline = []
    },

    // 预览路线（切换模式时重新调用高德API获取真实路线）
    async previewRoute(mode) {
      this.navMode = mode
      this.navModeLabel = { driving: '驾车', walking: '步行', cycling: '骑行' }[mode] || '驾车'
      if (!this.selectedPoi) return
      this.navTarget = this.selectedPoi
      try {
        const route = await this.fetchRoute(mode)
        if (route) {
          this.routeEstimates[mode] = route.timeDisplay || '--'
          if (route.polyline && route.polyline.length > 1) {
            this.activePolyline = [{
              points: route.polyline,
              color: '#2dba98',
              colorList: ['#2dba98'],
              width: 8,
              arrowLine: true,
              borderColor: '#fff',
              borderWidth: 2
            }]
          }
        }
      } catch (e) {
        console.warn('[MAP] previewRoute failed:', e)
      }
    },

    // ========== 直线连线（无导航时） ==========
    drawRouteLine(poi) {
      if (!this.userLocation) return
      this.activePolyline = [{
        points: [
          { latitude: this.userLocation.latitude, longitude: this.userLocation.longitude },
          { latitude: poi.latitude, longitude: poi.longitude }
        ],
        color: '#2dba98',
        colorList: ['#2dba98'],
        width: 5,
        dottedLine: false,
        arrowLine: true,
        borderColor: '#fff',
        borderWidth: 1
      }]
    },

    // ========== 附近同类POI ==========
    loadNearbyPois(poi) {
      const nearby = this.allPois
        .filter(p => p.type === poi.type && p.id !== poi.id)
        .map(p => {
          let distance = null
          if (this.userLocation) {
            distance = getDistance(
              this.userLocation.latitude, this.userLocation.longitude,
              p.latitude, p.longitude
            )
          }
          return { ...p, distance }
        })
        .slice(0, 6)
      this.nearbyPois = nearby
    },

    // 从后端 nearby API 获取附近POI
    async loadNearbyFromBackend(center) {
      try {
        const [err, res] = await new Promise(r => uni.request({
          url: config.GUIDE_API_BASE + '/api/v1/guide/nearby',
          method: 'POST',
          header: { 'Content-Type': 'application/json' },
          data: {
            scenic_id: config.SCENIC_ID,
            longitude: center.longitude,
            latitude: center.latitude,
            radius: 3000
          },
          success: res => r([null, res]),
          fail: e => r([e, null])
        }))
        if (err || !res || res.statusCode !== 200) return null
        const data = res.data
        const items = data.items || data.data || []
        if (items.length === 0) return null
        return items.map((item, idx) => ({
          id: 30000 + idx,
          name: item.name || item.title || '附近设施',
          type: item.category || item.type || 'attraction',
          latitude: item.latitude || center.latitude,
          longitude: item.longitude || center.longitude,
          description: item.summary || item.description || '',
          distance: item.distance || null
        }))
      } catch (e) {
        console.warn('[MAP] 后端附近搜索失败:', e)
        return null
      }
    },

    // 从高德API搜索附近POI（基于用户当前位置）
    async loadNearbyPoisFromAPI(center) {
      console.log('[MAP] loadNearbyPoisFromAPI 开始，中心点:', center)
      // 先尝试后端 nearby API
      const backendPois = await this.loadNearbyFromBackend(center)
      if (backendPois && backendPois.length > 0) {
        this.nearbyPois = backendPois
        return
      }
      // 后端API无结果，降级到高德API
      const url = `https://restapi.amap.com/v3/place/around?key=${config.GAODE_MAP_KEY}&location=${center.longitude},${center.latitude}&keywords=&types=&radius=3000&offset=20&page=1&extensions=base`
      try {
        const [err, res] = await new Promise(r => uni.request({ url, success: res => r([null, res]), fail: e => r([e, null]) }))
        if (err || !res || res.statusCode !== 200 || res.data.status !== '1' || !res.data.pois) {
          console.warn('[MAP] 附近POI搜索失败，使用默认POI')
          this.loadDefaultPois(center)
          return
        }
        const pois = res.data.pois.map((item, idx) => {
          const loc = item.location || ''
          const parts = loc.split(',')
          const lng = parseFloat(parts[0]) || 0
          const lat = parseFloat(parts[1]) || 0
          if (!lng || !lat) return null  // 过滤掉无效坐标
          const dist = item.distance || null
          let type = 'attraction'
          const c = (item.type || '').toLowerCase()
          const name = (item.name || '').toLowerCase()
          if (c.includes('餐饮') || c.includes('美食')) type = 'food'
          else if (c.includes('厕所') || c.includes('卫生间')) type = 'toilet'
          else if (c.includes('购物') || c.includes('商店')) type = 'shop'
          else if (name.includes('入口')) type = 'entrance'
          else if (name.includes('出口')) type = 'exit'
          else if (c.includes('服务') || c.includes('中心')) type = 'service'
          else if (name.includes('数字人') || name.includes('导览')) type = 'digital'
          else if (c.includes('医疗') || name.includes('急救')) type = 'medical'
          else if (name.includes('休息')) type = 'rest'
          return {
            id: 20000 + idx,
            name: item.name || '未知地点',
            description: item.address || item.type || '',
            latitude: lat,
            longitude: lng,
            type,
            distance: dist ? parseInt(dist) : null,
            phone: item.tel || '',
            isExternal: true
          }
        }).filter(p => p !== null)  // 过滤无效坐标
        console.log('[MAP] 附近POI搜索成功:', pois.length + '个')
        this.allPois = pois
        this.filteredPois = pois
        this.buildMarkers(pois)
        // 地图跟随用户位置
        this.mapCenter = { latitude: center.latitude, longitude: center.longitude }
        this.mapScale = 16
      } catch (e) {
        console.error('[MAP] loadNearbyPoisFromAPI 异常', e)
        this.loadDefaultPois(center)
      }
    },

    // 如果API无结果，用当前位置附近生成虚拟POI（保障有内容可展示）
    loadDefaultPois(center) {
      const types = ['attraction', 'food', 'toilet', 'shop', 'rest', 'service']
      const names = ['附近景点', '附近餐饮', '附近厕所', '附近商店', '附近休息点', '附近服务']
      const pois = types.map((type, i) => {
        const angle = (i / types.length) * 2 * Math.PI
        const offset = 0.003 + Math.random() * 0.002
        return {
          id: 30000 + i,
          name: names[i],
          description: '基于您当前位置生成的参考点',
          latitude: center.latitude + offset * Math.cos(angle),
          longitude: center.longitude + offset * Math.sin(angle),
          type,
          distance: Math.round(offset * 111000),
          phone: '',
          isExternal: false
        }
      })
      console.log('[MAP] 使用默认POI:', pois.length + '个')
      this.allPois = pois
      this.filteredPois = pois
      this.buildMarkers(pois)
    },

    updateNearbyPoisDistance() {
      if (!this.userLocation || !this.selectedPoi) return
      this.nearbyPois = this.nearbyPois.map(p => ({
        ...p,
        distance: getDistance(
          this.userLocation.latitude, this.userLocation.longitude,
          p.latitude, p.longitude
        )
      }))
    },

    selectNearbyPoi(nearby) {
      this.openPoiPanel(nearby)
    },

    // ========== 分类筛选 ==========
    toggleCategory(key) {
      this.activeCategory = key
      if (key === 'all') {
        this.filteredPois = this.allPois
      } else {
        this.filteredPois = this.allPois.filter(p => p.type === key)
      }
      this.buildMarkers(this.filteredPois)
      this.selectedPoi = null
    },

    // ========== 底部功能网格 ==========
    onQuickToolTap(key) {
      // 筛选对应类型的POI
      this.activeCategory = key
      this.filteredPois = this.allPois.filter(p => p.type === key)
      this.buildMarkers(this.filteredPois)
      this.selectedPoi = null
      this.showSearchPanel = false
      this.keyword = ''
      uni.showToast({ title: '已显示' + (POI_TYPE_CONFIG[key]?.name || ''), icon: 'none' })
    },

    searchByTag(tag) {
      this.keyword = tag
      this.showSearchPanel = true
      this.doSearch(tag)
    },

    showMore() {
      uni.showActionSheet({
        itemList: ['切换地图类型', '查看全部景点', '刷新位置'],
        success: (res) => {
          if (res.tapIndex === 0) this.toggleMapType()
          if (res.tapIndex === 1) { this.filteredPois = this.allPois; this.buildMarkers(this.allPois); this.selectedPoi = null }
          if (res.tapIndex === 2) this.backToLocation()
        }
      })
    },

    openRoutePanel() {
      if (!this.selectedPoi) {
        uni.showToast({ title: '请先选择目的地', icon: 'none' })
        return
      }
      this.startNavigation(this.selectedPoi, this.navMode)
      if (this.selectedPoi) {
        const stillVisible = this.filteredPois.some(p => p.id === this.selectedPoi.id)
        if (!stillVisible) {
          this.selectedPoi = null
          this.activePolyline = []
        }
      }
    },

    // 切换导航模式
    setNavMode(mode) {
      if (mode === this.navMode) return
      this.navMode = mode
      this.navModeLabel = mode === 'walking' ? '步行' : mode === 'driving' ? '驾车' : '骑行'
      // 如果正在导航，重新规划路线
      if (this.isNavigating && this.navTarget) {
        this.startNavigation(this.navTarget, mode)
      }
    },

    // ========== 搜索 ==========
    onSearchFocus() {
      this.searchFocused = true
      if (this.keyword) {
        this.showSearchPanel = true
        this.doSearch(this.keyword)
      }
    },

    onSearchInput(e) {
      this.keyword = e.detail.value
      if (this.keyword && this._searchTimer) clearTimeout(this._searchTimer)
      if (this.keyword.trim()) {
        this.showSearchPanel = true
        this._searchTimer = setTimeout(() => {
          this.doSearch(this.keyword)
        }, 400)
      } else {
        this.searchResults = []
        this.showSearchPanel = false
        this.hasSearched = false
      }
    },

    onSearchConfirm() {
      if (this._searchTimer) clearTimeout(this._searchTimer)
      if (this.keyword.trim()) {
        this.showSearchPanel = true
        this.doSearch(this.keyword)
      }
    },

    clearKeyword() {
      this.keyword = ''
      this.searchResults = []
      this.showSearchPanel = false
      this.hasSearched = false
      this.buildMarkers(this.filteredPois)
    },

    // 高德POI搜索
    async doSearch(keyword) {
      if (!keyword.trim()) return
      this.searchLoading = true

      // 使用当前位置或景区中心作为搜索中心
      const center = this.userLocation || { latitude: 30.558510, longitude: 114.303010 }
      // 高德 Web服务 API：place/text
      const searchUrl = `https://restapi.amap.com/v3/place/text?keywords=${encodeURIComponent(keyword)}&location=${center.longitude},${center.latitude}&city=全国&citylimit=true&offset=20&page=1&key=${config.GAODE_MAP_KEY}`

      try {
        const [err, res] = await new Promise(resolve => {
          uni.request({
            url: searchUrl,
            success: r => resolve([null, r]),
            fail: e => resolve([e, null])
          })
        })

        this.searchLoading = false
        this.hasSearched = true

        if (err || !res || res.statusCode !== 200) {
          console.error('POI搜索失败', err, res)
          this.searchResults = []
          return
        }

        const data = res.data
        if (data.status === '1' && data.pois && data.pois.length > 0) {
          this.searchResults = data.pois.map((item, idx) => {
            const loc = item.location || ''
            const [lng, lat] = loc.split(',')
            const dist = item.distance || null
            // 判断类型
            let type = 'attraction'
            const c = (item.type || '').toLowerCase()
            const name = (item.name || '').toLowerCase()
            if (c.includes('餐饮') || c.includes('美食') || name.includes('餐厅') || name.includes('饭店')) type = 'food'
            else if (c.includes('厕所') || c.includes('卫生间') || c.includes('洗手间')) type = 'toilet'
            else if (c.includes('购物') || c.includes('商店') || c.includes('超市')) type = 'shop'
            else if (name.includes('入口')) type = 'entrance'
            else if (name.includes('出口')) type = 'exit'
            else if (c.includes('服务') || c.includes('中心')) type = 'service'
            else if (name.includes('数字人') || name.includes('导览')) type = 'digital'
            else if (c.includes('医疗') || name.includes('急救')) type = 'medical'
            else if (name.includes('休息')) type = 'rest'

            return {
              id: 10000 + idx,
              name: item.name,
              description: item.address || item.type || '',
              latitude: parseFloat(lat) || 0,
              longitude: parseFloat(lng) || 0,
              type,
              distance: dist ? parseInt(dist) : null,
              phone: item.tel || '',
              isExternal: true
            }
          })
        } else {
          this.searchResults = []
        }
      } catch (e) {
        this.searchLoading = false
        console.error('POI搜索异常', e)
        this.searchResults = []
      }
    },

    selectSearchResult(result) {
      this.keyword = ''
      this.showSearchPanel = false
      this.searchFocused = false
      this.openPoiPanel(result)
    },

    onLocationSelector() {
      uni.showActionSheet({
        itemList: ['当前位置', '重新定位'],
        success: (res) => {
          if (res.tapIndex === 0) {
            if (this.userLocation) {
              this.mapCenter = { latitude: this.userLocation.latitude, longitude: this.userLocation.longitude }
              this.mapScale = 17
            } else {
              uni.showToast({ title: '正在获取位置…', icon: 'loading' })
              this.refreshUserLocation()
            }
          } else if (res.tapIndex === 1) {
            uni.showToast({ title: '正在重新定位…', icon: 'loading' })
            this.refreshUserLocation()
          }
        }
      })
    },

    // ========== 导航 ==========
    async startNavigation(poi, mode) {
      mode = mode || this.navMode || 'walking'
      console.log('[MAP] startNavigation 起点: mode=' + mode)
      // 先获取最新GPS位置
      uni.showToast({ title: '正在定位…', icon: 'loading', duration: 1000 })
      const [err, loc] = await new Promise(resolve => {
        uni.getLocation({
          type: 'gcj02',
          isHighAccuracy: true,
          success: r => resolve([null, r]),
          fail: e => resolve([e, null])
        })
      })

      if (err || !loc) {
        uni.showToast({ title: '定位失败，请开启位置权限', icon: 'none' })
        return
      }

      const freshLoc = { latitude: loc.latitude, longitude: loc.longitude }
      console.log('[MAP] 导航起点GPS:', freshLoc, '目的地:', poi.latitude, poi.longitude)
      const dist直线 = getDistance(freshLoc.latitude, freshLoc.longitude, poi.latitude, poi.longitude)
      console.log('[MAP] 直线距离:', dist直线 + '米')
      this.userLocation = freshLoc
      this.selectedPoi = null
      this.showSearchPanel = false
      this.showRouteOptions = false
      this.sheetSnapIndex = 0  // 导航时收起底部面板
      this.navTarget = poi
      this.navMode = mode
      this.navModeLabel = mode === 'walking' ? '步行' : mode === 'driving' ? '驾车' : '骑行'

      // 高德路线规划
      const routeInfo = await this.fetchRoute(mode)

      if (routeInfo && routeInfo.steps && routeInfo.steps.length > 0) {
        this.routeSteps = routeInfo.steps
        this.currentStepIndex = 0
        this.totalDistance = routeInfo.distance
        this.currentDistance = routeInfo.distance
        this.estimatedTime = routeInfo.time
        this.routePolyline = routeInfo.polyline
        this.remainingPolyline = [...routeInfo.polyline]
        this.drawRealRoute(this.remainingPolyline)
        this.updateNavInstructionFromStep(0)
      } else {
        // 降级：无法规划路线，提示用户
        this.routeSteps = []
        this.currentStepIndex = 0
        const dist = getDistance(freshLoc.latitude, freshLoc.longitude, poi.latitude, poi.longitude)
        this.totalDistance = dist
        this.currentDistance = dist
        this.estimatedTime = Math.ceil(dist / (mode === 'driving' ? 300 : mode === 'cycling' ? 150 : 50))
        // 不画误导的直线，只显示直线距离
        this.routePolyline = []
        this.remainingPolyline = []
        this.activePolyline = []
        this.navInstruction = '无法规划路线，直线距离约' + formatDistance(dist)
        this.navIcon = '?'
        console.warn('[MAP] 路线规划失败，显示直线距离')
      }

      this.navProgress = 0
      this.arrived = false
      this.isNavigating = true
      this.mapScale = 17

      // 导航时清空所有POI标记，只保留用户位置和目的地
      this.markers = [
        {
          id: 99998,
          latitude: freshLoc.latitude,
          longitude: freshLoc.longitude,
          iconPath: '/static/assets/nav_dot.png',
          width: 32,
          height: 32,
          anchor: { x: 0.5, y: 0.5 }
        },
        {
          id: 99999,
          latitude: poi.latitude,
          longitude: poi.longitude,
          iconPath: getMarkerIcon('destination'),
          width: 44,
          height: 52,
          anchor: { x: 0.5, y: 1 },
          callout: {
            content: poi.name,
            color: '#fff',
            fontSize: 12,
            borderRadius: 8,
            padding: 6,
            display: 'ALWAYS',
            bgColor: '#2dba98',
            borderColor: '#2dba98',
            borderWidth: 1
          }
        }
      ]

      // 启动实时GPS追踪
      this.startRealTracking(freshLoc)

      uni.vibrateShort({ fail: () => {} })
    },

    // 启动实时GPS追踪（替代模拟动画）
    startRealTracking(startLoc) {
      // 先停止旧的
      this.stopRealTracking()

      // 实时GPS定位
      uni.startLocationUpdate({
        type: 'gcj02',
        success: () => {
          console.log('[MAP] GPS追踪已启动')
        },
        fail: (e) => {
          console.error('[MAP] GPS启动失败', e)
        }
      })

      // 监听位置变化（每米更新一次）
      this._onLocationChange = (res) => {
        if (!this.isNavigating || this.arrived) return
        const gps = { latitude: res.latitude, longitude: res.longitude }
        this.userLocation = gps
        this.onGpsUpdate(gps)
      }
      uni.onLocationChange(this._onLocationChange)
    },

    // 停止GPS追踪
    stopRealTracking() {
      if (this._onLocationChange) {
        uni.offLocationChange(this._onLocationChange)
        this._onLocationChange = null
      }
      uni.stopLocationUpdate({ fail: () => {} })
    },

    // GPS位置更新处理（实时计算剩余距离和路线）
    onGpsUpdate(gps) {
      if (!this.navTarget) return

      // 计算到终点的直线距离
      const distToDest = getDistance(
        gps.latitude, gps.longitude,
        this.navTarget.latitude, this.navTarget.longitude
      )
      this.currentDistance = distToDest
      this.completedDistance = Math.max(0, this.totalDistance - distToDest)

      // 根据导航模式设置速度估算
      const speedMap = { walking: 50, cycling: 150, driving: 300 }
      const speed = speedMap[this.navMode] || 50
      this.estimatedTime = Math.ceil(distToDest / speed)

      // 更新地图中心跟随用户
      this.mapCenter = { latitude: gps.latitude, longitude: gps.longitude }

      // 计算进度（基于剩余距离占总距离的比例）
      this.navProgress = Math.max(0, Math.min(1, 1 - distToDest / this.totalDistance))

      // 更新剩余路线（只画从当前位置到终点的部分）
      this.updateRemainingRoute(gps)

      // 更新步骤指引
      this.updateNavStep()

      // 更新当前位置标记
      this.navLocationMarker = gps
      this.buildMarkersWithUser(this.filteredPois, gps)

      // 数字人导航语音播报（每30秒播报一次进展）
      this.speakNavProgress(distToDest)

      // 到达检测（高德精度约10-20米，20米内算到达）
      if (distToDest < 20) {
        this.onArrived()
      }
    },

    // 更新剩余路线（当前位置之后的部分）
    updateRemainingRoute(fromGps) {
      if (!this.routePolyline || this.routePolyline.length < 2) {
        // 直线模式：只画剩余部分
        this.activePolyline = [{
          points: [fromGps, { latitude: this.navTarget.latitude, longitude: this.navTarget.longitude }],
          color: '#E53D3D',
          colorList: ['#E53D3D'],
          width: 8,
          arrowLine: true,
          borderColor: '#fff',
          borderWidth: 2
        }]
        return
      }

      // 找到路线上距离GPS最近的点及其后续点
      let minDist = Infinity
      let nearestIdx = 0
      for (let i = 0; i < this.routePolyline.length; i++) {
        const d = getDistance(
          fromGps.latitude, fromGps.longitude,
          this.routePolyline[i].latitude, this.routePolyline[i].longitude
        )
        if (d < minDist) {
          minDist = d
          nearestIdx = i
        }
      }

      // 剩余路线 = 从最近点开始到最后
      const remaining = this.routePolyline.slice(nearestIdx)
      if (remaining.length < 2) return

      this.remainingPolyline = remaining

      // 画剩余路线（红色）+ 已走路线路（灰色）
      const passed = this.routePolyline.slice(0, nearestIdx + 1)
      this.activePolyline = [
        // 已走过的路（灰色）
        ...(passed.length >= 2 ? [{
          points: passed,
          color: '#BBBBBB',
          width: 6,
          dottedLine: false,
          borderColor: '#fff',
          borderWidth: 1
        }] : []),
        // 剩余路线（红色，加粗）
        {
          points: remaining,
          color: '#E53D3D',
          colorList: ['#E53D3D'],
          width: 8,
          dottedLine: false,
          arrowLine: true,
          borderColor: '#fff',
          borderWidth: 2
        }
      ]
    },

    onArrived() {
      this.arrived = true
      this.navProgress = 1
      this.currentDistance = 0
      this.navIcon = '●'
      this.navInstruction = '已到达目的地'
      this.stopRealTracking()
      uni.vibrateShort({ fail: () => {} })
      uni.showToast({ title: '已到达目的地 🎉', icon: 'success' })
      setTimeout(() => {
        this.cancelNavigation()
      }, 3000)
    },

    // 高德路线规划API（步行/驾车/骑行）
    async fetchRoute(mode) {
      mode = mode || 'walking'
      if (!this.userLocation || !this.navTarget) {
        console.log('[MAP] fetchRoute 跳过：userLocation或navTarget为空', this.userLocation, this.navTarget)
        return null
      }

      const from = `${this.userLocation.longitude},${this.userLocation.latitude}`
      const to = `${this.navTarget.longitude},${this.navTarget.latitude}`
      // 高德 Web服务 API v3
      const typeMap = { walking: 'walking', driving: 'driving', cycling: 'riding' }
      const type = typeMap[mode] || 'walking'
      const url = `https://restapi.amap.com/v3/direction/${type}?origin=${from}&destination=${to}&key=${config.GAODE_MAP_KEY}`
      console.log('[MAP] fetchRoute 开始:', mode, url)

      try {
        const [err, res] = await new Promise(resolve => {
          uni.request({
            url,
            success: r => resolve([null, r]),
            fail: e => resolve([e, null])
          })
        })

        if (err || !res || res.statusCode !== 200) {
          console.error('[MAP] fetchRoute 请求失败', err, res)
          return null
        }

        const data = res.data
        console.log('[MAP] fetchRoute 响应:', JSON.stringify(data).substring(0, 300))

        if (data.status === '1' && data.route && data.route.paths && data.route.paths.length > 0) {
          const path = data.route.paths[0]
          const steps = path.steps || []

          // 解析高德路线polyline
          const polyline = []
          const navSteps = []

          steps.forEach(step => {
            // 骑行API: steps[].rides[] 展开；步行/驾车: steps[] 直接用
            const subSteps = step.rides || [step]
            subSteps.forEach(sub => {
              const pts = (sub.polyline || '').split(';').filter(p => p.includes(','))
              const stepPoints = pts.map(p => {
                const [lng, lat] = p.split(',')
                return { longitude: parseFloat(lng), latitude: parseFloat(lat) }
              })
              stepPoints.forEach(p => polyline.push(p))
              if (sub.instruction) {
                navSteps.push({
                  instruction: sub.instruction || '',
                  distance: parseInt(sub.distance) || 0,
                  road_name: sub.road || ''
                })
              }
            })
          })

          const durationMin = Math.floor(parseInt(path.duration) / 60)
          const timeDisplay = durationMin > 0 ? durationMin + '分钟' : '<1分钟'
          console.log('[MAP] fetchRoute 成功:', path.distance + '米', navSteps.length + '个步骤')
          return {
            steps: navSteps,
            distance: parseInt(path.distance) || 0,
            time: parseInt(path.duration) || 0,
            timeDisplay,
            polyline
          }
        } else {
          console.warn('[MAP] fetchRoute 无路线:', data.info, '起点:', from, '终点:', to)
          return null
        }
      } catch (e) {
        console.error('[MAP] fetchRoute 异常', e)
        return null
      }
    },

    // 绘制路线（绿色粗线）
    drawRealRoute(polyline) {
      if (!polyline || polyline.length < 2) return
      this.activePolyline = [{
        points: polyline,
        color: '#2dba98',       // 翠绿色
        colorList: ['#2dba98'],
        width: 16,
        dottedLine: false,
        arrowLine: true,
        borderColor: '#fff',
        borderWidth: 4
      }]
    },

    // 构建POI标记 + 用户位置标记
    buildMarkersWithUser(pois, userLoc) {
      const marks = (pois || []).map(poi => {
        const cfg = POI_TYPE_CONFIG[poi.type] || POI_TYPE_CONFIG.attraction
        return {
          id: poi.id,
          latitude: poi.latitude,
          longitude: poi.longitude,
          width: 44,
          height: 52,
          iconPath: getMarkerIcon(poi.type),
          callout: {
            content: cfg.name + ' · ' + poi.name,
            color: '#333',
            fontSize: 13,
            borderRadius: 10,
            padding: 8,
            display: 'ALWAYS',
            bgColor: 'rgba(255,255,255,0.97)',
            borderColor: cfg.markerColor,
            borderWidth: 2
          },
          anchor: { x: 0.5, y: 1 }
        }
      })

      // 导航时：添加用户位置蓝色导航标
      if (userLoc) {
        marks.push({
          id: 9999,
          latitude: userLoc.latitude,
          longitude: userLoc.longitude,
          width: 28,
          height: 28,
          iconPath: '/static/assets/nav_dot.png',
          anchor: { x: 0.5, y: 0.5 }
        })
      }

      this.markers = marks
    },

    // 从步骤更新导航指示
    updateNavInstructionFromStep(index) {
      if (!this.routeSteps || this.routeSteps.length === 0) {
        // 直线模式
        if (this.currentDistance < 30) {
          this.navIcon = '●'
          this.navInstruction = '已到达目的地'
        } else {
          this.navIcon = '↑'
          this.navInstruction = '向前行驶'
        }
        return
      }

      const step = this.routeSteps[index]
      if (!step) return

      this.navInstruction = step.instruction || '继续前行'
      const speedMap = { walking: 50, cycling: 150, driving: 300 }
      const speed = speedMap[this.navMode] || 50
      this.estimatedTime = Math.ceil(this.currentDistance / speed)

      // 根据关键词匹配方向图标
      const instr = step.instruction || ''
      if (instr.includes('左转')) this.navIcon = '←'
      else if (instr.includes('右转')) this.navIcon = '→'
      else if (instr.includes('向前') || instr.includes('直行')) this.navIcon = '↑'
      else if (instr.includes('到达') || instr.includes('目的')) this.navIcon = '●'
      else if (instr.includes('左')) this.navIcon = '↖'
      else if (instr.includes('右')) this.navIcon = '↘'
      else this.navIcon = this.navMode === 'driving' ? '🚗' : this.navMode === 'cycling' ? '🚴' : '↑'

      // 语音播报导航指令
      this.speakNavInstruction(instr)
    },

    // 导航中切换出行模式（不退出导航，直接重算路线）
    async switchNavMode(mode) {
      if (!this.navTarget || mode === this.navMode) return

      this.stopRealTracking()
      this.navMode = mode
      this.navModeLabel = mode === 'walking' ? '步行' : mode === 'driving' ? '驾车' : '骑行'

      // 从当前位置重新规划路线
      if (this.userLocation) {
        const freshLoc = this.userLocation
        const routeInfo = await this.fetchRoute(mode)

        if (routeInfo && routeInfo.steps && routeInfo.steps.length > 0) {
          this.routeSteps = routeInfo.steps
          this.currentStepIndex = 0
          this.totalDistance = routeInfo.distance
          this.currentDistance = routeInfo.distance
          this.estimatedTime = routeInfo.time
          this.routePolyline = routeInfo.polyline
          this.remainingPolyline = [...routeInfo.polyline]
          this.drawRealRoute(this.remainingPolyline)
          this.updateNavInstructionFromStep(0)
        } else {
          const dist = getDistance(freshLoc.latitude, freshLoc.longitude, this.navTarget.latitude, this.navTarget.longitude)
          this.totalDistance = dist
          this.currentDistance = dist
          this.estimatedTime = Math.ceil(dist / (mode === 'driving' ? 300 : mode === 'cycling' ? 150 : 50))
          this.routeSteps = []
          this.navInstruction = '无法规划路线，直线距离约' + formatDistance(dist)
        }

        this.navProgress = 0
        this.arrived = false
        this.startRealTracking(freshLoc)
      }
      uni.showToast({ title: '已切换' + this.navModeLabel + '路线', icon: 'none' })
    },

    // 语音播报导航指令（TTS）
    speakNavInstruction(text) {
      if (!text || this._lastSpoken === text) return
      this._lastSpoken = text
      var speech = text.replace(/<[^>]+>/g, '') + '。'
      if (this.currentDistance > 100) {
        speech = '前方' + (this.currentDistance >= 1000 ? (this.currentDistance/1000).toFixed(1) + '公里' : this.currentDistance + '米') + '，' + speech
      }
      this._doTts(speech)
    },

    // 数字人导航进展语音播报（每30秒）
    speakNavProgress(distToDest) {
      var now = Date.now()
      var modeName = { walking: '步行', driving: '驾车', cycling: '骑行' }[this.navMode] || ''
      var MIN_INTERVAL = 30000 // 30秒

      // 首次导航播报
      if (!this._lastNavVoiceTime) {
        this._lastNavVoiceTime = now
        var firstMsg = modeName + '导航开始，距离目的地约'
        firstMsg += distToDest >= 1000 ? (distToDest / 1000).toFixed(1) + '公里' : Math.round(distToDest) + '米'
        firstMsg += '，我将在途中为您播报进展'
        this._doTts(firstMsg)
        return
      }

      // 每30秒播报一次
      if (now - this._lastNavVoiceTime < MIN_INTERVAL) return
      this._lastNavVoiceTime = now

      var progress = Math.round(this.navProgress * 100)
      var remaining = distToDest
      var msg = ''

      // 里程碑播报
      if (progress >= 95) {
        msg = '即将到达目的地，导航即将结束，祝您游玩愉快'
      } else if (progress >= 75) {
        msg = '已完成百分之' + progress + '，剩余不到' + (remaining >= 1000 ? (remaining / 1000).toFixed(1) + '公里' : Math.round(remaining) + '米')
      } else if (progress >= 50) {
        msg = '已经走了一半了，剩余约' + (remaining >= 1000 ? (remaining / 1000).toFixed(1) + '公里' : Math.round(remaining) + '米') + '，加油！'
      } else {
        msg = '当前剩余约' + (remaining >= 1000 ? (remaining / 1000).toFixed(1) + '公里' : Math.round(remaining) + '米')
        if (this.navInstruction) {
          msg += '，' + this.navInstruction.replace(/<[^>]+>/g, '')
        }
      }
      this._doTts(msg)
    },

    _doTts(speech) {
      // #ifdef APP-PLUS
      try { if (typeof plus !== 'undefined' && plus.speech) { plus.speech.speak(speech, { rate: 1.0 }) } }
      catch(e) { console.warn('[MAP] TTS失败:', e) }
      // #endif
      // #ifdef H5
      try {
        if (window.speechSynthesis) {
          var utter = new SpeechSynthesisUtterance(speech)
          utter.lang = 'zh-CN'; utter.rate = 0.9
          window.speechSynthesis.speak(utter)
        }
      } catch(e) { console.warn('[MAP] H5 TTS失败:', e) }
      // #endif
    },

    cancelNavigation() {
      this.stopRealTracking()
      this.isNavigating = false
      this.navTarget = null
      this.navProgress = 0
      this.currentDistance = 0
      this.arrived = false
      this.navMode = 'walking'
      this.navModeLabel = '步行'
      this.routeSteps = []
      this.currentStepIndex = 0
      this.routePolyline = []
      this.remainingPolyline = []
      this.navLocationMarker = null
      this.activePolyline = []
      this.buildMarkers(this.filteredPois)
      // 恢复地图中心到用户位置
      if (this.userLocation) {
        this.mapCenter = { latitude: this.userLocation.latitude, longitude: this.userLocation.longitude }
        this.mapScale = 16
      }
    },

    // 根据GPS实时位置更新步骤指引
    updateNavStep() {
      if (!this.routeSteps || this.routeSteps.length === 0) {
        // 直线模式
        if (this.currentDistance < 20) {
          this.navIcon = '●'
          this.navInstruction = '已到达目的地'
        } else {
          this.navIcon = '↑'
          this.navInstruction = '沿路直行'
        }
        return
      }

      // 计算已走路程
      const walked = this.totalDistance - this.currentDistance
      let accumulated = 0
      let stepIdx = 0
      for (let i = 0; i < this.routeSteps.length; i++) {
        accumulated += this.routeSteps[i].distance
        if (accumulated >= walked) {
          stepIdx = i
          break
        }
        stepIdx = i
      }

      if (stepIdx !== this.currentStepIndex) {
        this.currentStepIndex = stepIdx
        this.updateNavInstructionFromStep(stepIdx)
      }
    },

    callPhone(poi) {
      if (!poi.phone) {
        uni.showToast({ title: '暂无电话', icon: 'none' })
        return
      }
      uni.makePhoneCall({ phoneNumber: poi.phone })
    },

    collectPoi(poi) {
      const updated = { ...poi, collected: !poi.collected }
      const idx = this.allPois.findIndex(p => p.id === poi.id)
      if (idx !== -1) this.allPois[idx] = updated
      if (this.selectedPoi?.id === poi.id) this.selectedPoi = updated
      uni.showToast({
        title: updated.collected ? '已收藏' : '已取消收藏',
        icon: 'success'
      })
    },

    viewDetail(poi) {
      uni.showModal({
        title: poi.name,
        content: `${poi.description}${poi.phone ? '\n电话：' + poi.phone : ''}`,
        showCancel: false
      })
    },

    // ========== 工具 ==========
    poiTypeConfig(type) {
      return POI_TYPE_CONFIG[type] || { color: '#888', name: '地点', icon: '✦' }
    },

    formatDist(meters) {
      return formatDistance(meters)
    },

    formatDur(seconds) {
      return formatDuration(seconds)
    },

    // 全览路线
    viewFullRoute() {
      if (!this.routePolyline || this.routePolyline.length < 2) return
      // 计算路线边界
      let minLat = Infinity, maxLat = -Infinity, minLng = Infinity, maxLng = -Infinity
      this.routePolyline.forEach(p => {
        minLat = Math.min(minLat, p.latitude)
        maxLat = Math.max(maxLat, p.latitude)
        minLng = Math.min(minLng, p.longitude)
        maxLng = Math.max(maxLng, p.longitude)
      })
      // 设置中心点和缩放
      this.mapCenter = {
        latitude: (minLat + maxLat) / 2,
        longitude: (minLng + maxLng) / 2
      }
      // 根据跨度计算合适的scale（粗略估算）
      const latDiff = maxLat - minLat
      const lngDiff = maxLng - minLng
      const maxDiff = Math.max(latDiff, lngDiff)
      if (maxDiff > 0.1) this.mapScale = 10
      else if (maxDiff > 0.05) this.mapScale = 12
      else if (maxDiff > 0.02) this.mapScale = 14
      else if (maxDiff > 0.01) this.mapScale = 15
      else this.mapScale = 16
    },

    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/home/index' }) })
    },

    // ========== 天气 ==========
    loadMapWeather(city) {
      var self = this
      var targetCity = city || self.weatherCity || '无锡'
      var API_BASE = config.API_BASE_URL

      // 1. 实况天气（当前温度+天气）
      uni.request({
        url: API_BASE + '/weather/current',
        method: 'GET',
        data: { city: targetCity },
        success: function(res) {
          if (res.statusCode === 200 && res.data && res.data.code === 200 && res.data.data && res.data.data.length > 0) {
            var w = res.data.data[0]
            var weather = w.weather || ''
            var temp = w.temperature || '--'
            self.weatherCurrentTemp = temp
            self.weatherCurrentIcon = WEATHER_ICONS[weather] || '☀️'
            self.weatherCity = w.city || targetCity
            self.weatherWind = w.winddirection || ''
            self.weatherWindPower = w.windpower ? w.windpower + '级' : ''
            self.weatherHumidity = w.humidity ? w.humidity + '%' : ''
          }
        },
        fail: function() {}
      })

      // 2. 预报天气（今日温度范围 + 逐小时模拟）
      uni.request({
        url: API_BASE + '/weather/forecast',
        method: 'GET',
        data: { city: targetCity },
        success: function(res) {
          if (res.statusCode === 200 && res.data && res.data.code === 200 && res.data.data && res.data.data.length > 0) {
            var city_data = res.data.data[0]
            var casts = city_data.casts || []
            if (casts.length > 0) {
              var today = casts[0]
              var minTemp = today.nighttemp || '--'
              var maxTemp = today.daytemp || '--'
              var dayWeather = today.dayweather || ''
              self.weatherSummary = '今日' + minTemp + '°至' + maxTemp + '°，' + dayWeather
              // 生成逐小时预报（模拟：当前+后5小时，基于日预报插值）
              var now = new Date()
              var hourly = []
              var baseTemp = parseInt(self.weatherCurrentTemp || maxTemp) || parseInt(maxTemp) || 20
              var min = parseInt(minTemp) || (baseTemp - 4)
              var max = parseInt(maxTemp) || (baseTemp + 3)
              // 天气图标序列（白天晴→晚上转阴）
              var dayIcon = WEATHER_ICONS[dayWeather] || '⛅'
              var nightIcon = WEATHER_ICONS[today.nightweather] || '🌙'
              for (var i = 0; i < 6; i++) {
                var h = new Date(now.getTime() + i * 3600 * 1000)
                var hour = h.getHours()
                var label = i === 0 ? '现在' : hour + '点'
                // 温度：白天高，夜间低（余弦插值）
                var progress = Math.cos(((hour - 14) / 24) * 2 * Math.PI) * 0.5 + 0.5
                var t = Math.round(min + (max - min) * progress)
                var icon = (hour >= 6 && hour < 18) ? dayIcon : nightIcon
                hourly.push({ time: label, icon: icon, temp: t + '°' })
              }
              self.weatherHourly = hourly
              self.weatherLoaded = true
            }
          }
        },
        fail: function() {
          // 天气加载失败，静默忽略
        }
      })
    }
  }
}
