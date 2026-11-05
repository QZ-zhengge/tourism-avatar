const config = require('@/config/index.js')
const GUIDE_BASE = config.GUIDE_API_BASE
const API_BASE = config.API_BASE_URL  // 主后端API（含天气接口）

// 天气现象 → 图标映射
const WEATHER_ICONS = {
  '晴': '☀️',
  '多云': '⛅',
  '阴': '☁️',
  '小雨': '🌦',
  '中雨': '🌧',
  '大雨': '🌧',
  '阵雨': '🌦',
  '雷阵雨': '⛈',
  '暴雨': '🌊',
  '雪': '🌨',
  '小雪': '🌨',
  '中雪': '❄️',
  '大雪': '❄️',
  '雾': '🌫',
  '霾': '🌫',
  '浮尘': '🌫',
  '扬沙': '🌫',
  '大风': '💨',
  '风': '💨'
}

export default {
  data() {
    return {
      district: '灵山胜境',
      weatherText: '晴朗 · 24°C',
      weatherIcon: '☀️',
      // 缓存城市名，避免每次都重新解析
      weatherCity: '无锡',
      scenicSpots: [
        { name: '灵山胜境', city: '无锡', latitude: 31.42, longitude: 120.09 },
        { name: '西湖', city: '杭州', latitude: 30.25, longitude: 120.15 },
        { name: '故宫', city: '北京', latitude: 39.92, longitude: 116.40 },
        { name: '黄山', city: '黄山', latitude: 30.14, longitude: 118.18 },
        { name: '鼓浪屿', city: '厦门', latitude: 24.45, longitude: 118.07 }
      ],
      selectedSpotIndex: 0,
      banners: [
        '/static/assets/images/carousel1.jpg',
        '/static/assets/images/carousel2.jpg',
        '/static/assets/images/carousel3.jpg'
      ],
      functions: [
        { title: '热门景点', icon: 'photo',          color: '#e8f5f0', iconColor: '#2bb57b', page: '/pages/hotspots/index' },
        { title: '路线规划', icon: 'map',             color: '#e8edf8', iconColor: '#4a7ee0', page: '/pages/map/index' },
        { title: 'AI问答',   icon: 'chat',            color: '#f2eaf8', iconColor: '#8e5fd4', page: '/pages/ai-chat/index' },
        { title: '活动演出', icon: 'play-right',      color: '#fff3e0', iconColor: '#f09030', page: '/pages/events/index' },
        { title: '美食推荐', icon: 'star',            color: '#fdecea', iconColor: '#e85050', page: '/pages/food/index' },
        { title: '拍照打卡', icon: 'camera',          color: '#e8f4fd', iconColor: '#2196c8', page: '/pages/checkin/index' },
        { title: '游玩攻略', icon: 'bookmark',        color: '#edf7ee', iconColor: '#3aaa5c', page: '/pages/guide/index' },
        { title: '周边好店', icon: 'shopping-cart',   color: '#fef3e2', iconColor: '#e07820', page: '/pages/nearby/index' }
      ],
      posts: [
        {
          id: 'p1',
          title: '朝阳公园的日落真的好看',
          excerpt: '湖边风很舒服，傍晚拍照特别出片，适合和朋友慢慢逛。',
          image: '/static/assets/images/carousel1.jpg'
        },
        {
          id: 'p2',
          title: '景区夜游氛围感拉满',
          excerpt: '灯光亮起来以后特别梦幻，推荐穿浅色衣服拍照。',
          image: '/static/assets/images/carousel2.jpg'
        },
        {
          id: 'p3',
          title: '周末适合带家人来走走',
          excerpt: '路线不累，附近吃饭也方便，整体体验很轻松。',
          image: '/static/assets/images/carousel3.jpg'
        }
      ],
      hotSpotsLoaded: false
    }
  },
  onLoad() {
    this.loadWeather('无锡')
    this.loadHotSpots()
  },
  methods: {
    // 景区切换
    onSpotChange(e) {
      this.selectedSpotIndex = e.detail.value
      var spot = this.scenicSpots[this.selectedSpotIndex]
      this.district = spot.name
      this.loadWeather(spot.city)
    },

    // 从后端 weather API 加载实况天气
    loadWeather(city) {
      var self = this
      uni.request({
        url: API_BASE + '/weather/current',
        method: 'GET',
        data: { city: city || '北京' },
        success: function(res) {
          if (res.statusCode === 200 && res.data && res.data.code === 200 && res.data.data && res.data.data.length > 0) {
            var w = res.data.data[0]
            var weather = w.weather || ''
            var temp = w.temperature || ''
            var icon = WEATHER_ICONS[weather] || '☀️'
            self.weatherIcon = icon
            self.weatherText = weather + ' · ' + temp + '°C'
            console.log('[Home] 天气加载成功:', weather, temp + '°C')
          }
        },
        fail: function(err) {
          console.warn('[Home] 天气加载失败，使用默认数据:', err)
        }
      })
    },

    // 从 guide/hot-spots 加载热门景点数据到轮播图
    loadHotSpots() {
      uni.request({
        url: GUIDE_BASE + '/api/v1/guide/hot-spots',
        method: 'GET',
        data: {
          scenic_id: config.SCENIC_ID,
          scenic_name: config.SCENIC_NAME,
          page: 1,
          page_size: 3
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data && res.data.items && res.data.items.length > 0) {
            const items = res.data.items
            // 用热门景点更新轮播图
            this.banners = items.map(item => {
              return item.cover_image || '/static/assets/images/carousel1.jpg'
            }).slice(0, 3)

            this.hotSpotsLoaded = true
            console.log('[Home] hot-spots 加载成功，共', items.length, '条')
          }
        },
        fail: (err) => {
          console.warn('[Home] hot-spots 加载失败，使用默认数据')
        }
      })
    },

    onFunctionTap(item) {
      if (!item.page) {
        uni.showToast({ title: item.title + ' 功能即将上线', icon: 'none' })
        return
      }
      // tabBar 页面用 switchTab，非 tabBar 页面用 navigateTo（保留页面栈，返回键正常工作）
      const tabBarPages = ['/pages/home/index', '/pages/map/index', '/pages/digital-human/index', '/pages/moments/index', '/pages/mine/index']
      if (tabBarPages.includes(item.page)) {
        uni.switchTab({ url: item.page })
      } else {
        uni.navigateTo({ url: item.page })
      }
    },
    goMoments() {
      uni.switchTab({ url: '/pages/moments/index' })
    },
    goDigitalHuman() {
      uni.switchTab({ url: '/pages/digital-human/index' })
    },
    goItinerary() {
      uni.navigateTo({ url: '/pages/itinerary/index' })
    },
    goMap() {
      uni.switchTab({ url: '/pages/map/index' })
    },
    goSearch() {
      uni.navigateTo({ url: '/pages/search/index' })
    }
  }
}
