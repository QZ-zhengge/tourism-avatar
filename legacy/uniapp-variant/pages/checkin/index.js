const config = require('@/config/index.js')
const GUIDE_BASE = config.GUIDE_API_BASE

const COLOR_POOL = [
  '#F44336', '#E91E63', '#9C27B0', '#673AB7',
  '#3F51B5', '#2196F3', '#0097A7', '#00897B',
  '#43A047', '#689F38', '#EF6C00', '#FF5722',
  '#5D4037', '#455A64', '#546E7A', '#6A1B9A',
  '#D84315', '#C62828', '#37474F', '#1565C0'
]

function getColor(title, index) {
  if (!title) return COLOR_POOL[index % COLOR_POOL.length]
  var hash = 0
  for (var i = 0; i < title.length; i++) {
    hash = title.charCodeAt(i) + ((hash << 5) - hash)
  }
  return COLOR_POOL[Math.abs(hash) % COLOR_POOL.length]
}

function getLetter(title) {
  return title ? title.charAt(0) : '?'
}

// 打卡存储键
const CHECKIN_KEY = 'suixing_checkins'

// 保存打卡记录
function saveCheckin(record) {
  try {
    var raw = uni.getStorageSync(CHECKIN_KEY) || '[]'
    var list = JSON.parse(raw)
    list.unshift(record) // 最新的在前面
    if (list.length > 100) list = list.slice(0, 100) // 最多保留100条
    uni.setStorageSync(CHECKIN_KEY, JSON.stringify(list))
    return true
  } catch (e) {
    console.warn('[Checkin] 保存失败:', e)
    return false
  }
}

// 获取打卡记录
function getCheckins() {
  try {
    var raw = uni.getStorageSync(CHECKIN_KEY) || '[]'
    return JSON.parse(raw)
  } catch (e) {
    return []
  }
}

export default {
  data() {
    return {
      navTop: 0,
      navHeight: 88,
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0,
      hotspots: [],
      photoSpots: [],

      // 打卡弹窗
      showCheckinModal: false,
      currentSpot: null,       // 当前选中的景点
      checkinPhoto: '',         // 选择的照片路径
      checkinNote: '',          // 打卡备注

      // 打卡记录弹窗
      showRecordsModal: false,
      checkinRecords: [],

      // 查看大图
      previewImageUrl: '',

      achievements: [
        { id: 'a1', name: '初次打卡', desc: '完成第一次打卡', target: 1, current: 0, unlocked: false },
        { id: 'a2', name: '打卡达人', desc: '累计打卡5次', target: 5, current: 0, unlocked: false },
        { id: 'a3', name: '摄影大师', desc: '发布10张照片到旅友圈', target: 10, current: 0, unlocked: false }
      ]
    }
  },
  onLoad() {
    const systemInfo = uni.getSystemInfoSync()
    this.navTop = systemInfo.statusBarHeight || 0
    this.navHeight = (systemInfo.statusBarHeight || 0) + 48
    this.loadPhotoSpots()
    this.refreshAchievements()
  },
  onShow() {
    this.refreshAchievements()
  },
  methods: {
    getColor, getLetter,

    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/home/index' }) })
    },

    // 刷新成就进度
    refreshAchievements() {
      var records = getCheckins()
      var count = records.length
      this.achievements[0].current = Math.min(count, 1)
      this.achievements[0].unlocked = count >= 1
      this.achievements[0].progress = count >= 1 ? 100 : 0

      this.achievements[1].current = Math.min(count, 5)
      this.achievements[1].unlocked = count >= 5
      this.achievements[1].progress = Math.min(count / 5 * 100, 100)

      // 带图片的打卡数
      var withPhoto = records.filter(function(r) { return r.photo }).length
      this.achievements[2].current = Math.min(withPhoto, 10)
      this.achievements[2].unlocked = withPhoto >= 10
      this.achievements[2].progress = Math.min(withPhoto / 10 * 100, 100)
    },

    // 点击打卡按钮 → 弹出打卡弹窗
    onCheckin(spot) {
      this.currentSpot = spot
      this.checkinPhoto = ''
      this.checkinNote = ''
      this.showCheckinModal = true
    },

    // 选择打卡照片
    choosePhoto() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['camera', 'album'],
        success: (res) => {
          this.checkinPhoto = res.tempFilePaths[0]
        },
        fail: () => {}
      })
    },

    // 拍照打卡
    takePhoto() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['camera'],
        success: (res) => {
          this.checkinPhoto = res.tempFilePaths[0]
        },
        fail: () => {}
      })
    },

    // 从相册选择
    chooseFromAlbum() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album'],
        success: (res) => {
          this.checkinPhoto = res.tempFilePaths[0]
        },
        fail: () => {}
      })
    },

    // 清除已选照片
    removePhoto() {
      this.checkinPhoto = ''
    },

    // 确认打卡
    confirmCheckin() {
      if (!this.currentSpot) return

      var now = new Date()
      var dateStr = now.getFullYear() + '-' +
        String(now.getMonth() + 1).padStart(2, '0') + '-' +
        String(now.getDate()).padStart(2, '0')
      var timeStr = String(now.getHours()).padStart(2, '0') + ':' +
        String(now.getMinutes()).padStart(2, '0')

      var record = {
        id: 'ck_' + Date.now(),
        spotName: this.currentSpot.name,
        spotSummary: this.currentSpot.summary || '',
        photo: this.checkinPhoto,
        note: this.checkinNote,
        date: dateStr,
        time: timeStr,
        timestamp: Date.now()
      }

      var ok = saveCheckin(record)
      if (ok) {
        this.showCheckinModal = false
        this.refreshAchievements()
        uni.showToast({ title: '打卡成功！📷', icon: 'none', duration: 2000 })
      } else {
        uni.showToast({ title: '打卡失败，请重试', icon: 'none' })
      }
    },

    // 取消打卡弹窗
    cancelCheckin() {
      this.showCheckinModal = false
      this.currentSpot = null
      this.checkinPhoto = ''
      this.checkinNote = ''
    },

    // 查看我的打卡记录
    onMyCheckins() {
      this.checkinRecords = getCheckins()
      this.showRecordsModal = true
    },

    // 关闭记录弹窗
    closeRecordsModal() {
      this.showRecordsModal = false
    },

    // 预览图片
    previewPhoto(url) {
      if (!url) return
      uni.previewImage({
        urls: [url],
        current: url
      })
    },

    // 删除打卡记录
    deleteCheckin(record) {
      uni.showModal({
        title: '删除确认',
        content: '确定删除这条打卡记录吗？',
        success: (res) => {
          if (res.confirm) {
            try {
              var raw = uni.getStorageSync(CHECKIN_KEY) || '[]'
              var list = JSON.parse(raw)
              list = list.filter(function(r) { return r.id !== record.id })
              uni.setStorageSync(CHECKIN_KEY, JSON.stringify(list))
              this.checkinRecords = list
              this.refreshAchievements()
              uni.showToast({ title: '已删除', icon: 'success' })
            } catch (e) {
              uni.showToast({ title: '删除失败', icon: 'none' })
            }
          }
        }
      })
    },

    // 加载更多
    loadMore() {
      this.page++
      this.loadPhotoSpots()
    },

    onSpotTap(spot) {
      if (spot.link) {
        plus.runtime.openURL(spot.link, function() {
          uni.setClipboardData({
            data: spot.link,
            success: function() { uni.showToast({ title: '链接已复制', icon: 'none' }) }
          })
        })
      } else if (spot.summary) {
        uni.showModal({ title: spot.name, content: spot.summary, showCancel: false })
      }
    },

    onPhotoTap(photo) {
      if (photo.link) {
        plus.runtime.openURL(photo.link, function() {
          uni.setClipboardData({
            data: photo.link,
            success: function() { uni.showToast({ title: '链接已复制', icon: 'none' }) }
          })
        })
      } else if (photo.summary) {
        uni.showModal({ title: photo.name, content: photo.summary, showCancel: false })
      }
    },

    loadPhotoSpots() {
      this.loading = true
      uni.request({
        url: GUIDE_BASE + '/api/v1/guide/photo-spots',
        method: 'GET',
        data: {
          scenic_id: config.SCENIC_ID,
          scenic_name: config.SCENIC_NAME,
          page: this.page,
          page_size: this.pageSize
        },
        success: (res) => {
          const data = res.data && res.data.data ? res.data.data : res.data
          const items = data && data.items ? data.items : []
          this.total = data.total || 0
          if (res.statusCode === 200 && items.length > 0) {
            var mapped = items.map(function(item, idx) {
              return {
                id: item.poi_id || ('hs' + idx),
                name: item.title || '打卡点',
                summary: item.summary || '',
                location: item.summary ? item.summary.substring(0, 12) : '',
                link: item.source_link || '',
                count: Math.floor(Math.random() * 2000) + 500,
                rating: (4.5 + Math.random() * 0.5).toFixed(1)
              }
            })
            if (this.page === 1) {
              this.hotspots = mapped.slice(0, 4)
              this.photoSpots = mapped
            } else {
              this.photoSpots = this.photoSpots.concat(mapped)
            }
          }
        },
        fail: (err) => {
          console.warn('[Checkin] 加载失败:', err)
        },
        complete: () => {
          this.loading = false
        }
      })
    }
  }
}
