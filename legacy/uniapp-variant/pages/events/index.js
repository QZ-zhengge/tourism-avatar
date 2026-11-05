const config = require('@/config/index.js')

// 颜色池
const COLOR_POOL = [
  '#F44336', '#E91E63', '#9C27B0', '#673AB7',
  '#3F51B5', '#2196F3', '#0097A7', '#00897B',
  '#43A047', '#689F38', '#EF6C00', '#FF5722',
  '#5D4037', '#455A64', '#546E7A', '#6A1B9A',
  '#D84315', '#C62828', '#37474F', '#1565C0'
]

export default {
  data() {
    return {
      navTop: 0,
      navHeight: 88,
      currentCat: 0,
      loading: false,
      noMore: false,
      page: 1,
      pageSize: 10,
      total: 0,
      categories: [
        { name: '全部', type: '' },
        { name: '文艺演出', type: '演出' },
        { name: '民俗活动', type: '民俗' },
        { name: '主题展览', type: '展览' },
        { name: '节庆活动', type: '节庆' }
      ],
      events: []
    }
  },

  computed: {
    filteredEvents() {
      if (this.currentCat === 0) return this.events
      var type = this.categories[this.currentCat].type
      return this.events.filter(function(e) {
        return e.tags && e.tags.indexOf(type) !== -1
      })
    }
  },

  onLoad() {
    var sys = uni.getSystemInfoSync()
    this.navTop = sys.statusBarHeight || 0
    this.navHeight = (sys.statusBarHeight || 0) + 48
    this.loadEvents()
  },

  methods: {
    // 随机颜色（基于标题哈希）
    getColor(title, index) {
      if (!title) return COLOR_POOL[index % COLOR_POOL.length]
      var hash = 0
      for (var i = 0; i < title.length; i++) {
        hash = title.charCodeAt(i) + ((hash << 5) - hash)
      }
      return COLOR_POOL[Math.abs(hash) % COLOR_POOL.length]
    },

    getLetter(title) {
      return title ? title.charAt(0) : '?'
    },

    goBack() {
      uni.navigateBack({ fail: function() { uni.switchTab({ url: '/pages/home/index' }) } })
    },

    switchTab(index) {
      this.currentCat = index
      this.noMore = false
    },

    // 点击活动
    onEventTap(event) {
      if (event.link) {
        // 有链接 → 打开系统浏览器
        plus.runtime.openURL(event.link, function() {
          uni.setClipboardData({
            data: event.link,
            success: function() {
              uni.showToast({ title: '链接已复制', icon: 'none' })
            }
          })
        })
      } else if (event.summary) {
        // 有文字简介 → 弹窗展示
        uni.showModal({
          title: event.title,
          content: event.summary,
          showCancel: false
        })
      }
    },

    // 加载活动数据
    loadEvents() {
      this.loading = (this.page === 1)

      var self = this
      uni.request({
        url: config.GUIDE_API_BASE + '/api/v1/guide/events',
        method: 'GET',
                data: {
          scenic_id: config.SCENIC_ID,
          scenic_name: config.SCENIC_NAME,
          page: this.page,
          page_size: this.pageSize
        },
        success: function(res) {
          if (res.statusCode !== 200 || !res.data) {
            self.loading = false
            return
          }

          var data = res.data && res.data.data ? res.data.data : res.data
          var items = data && data.items ? data.items : []
          self.total = data.total || 0

          var parsed = items.map(function(item, idx) {
            var tags = item.tags || []
            return {
              id: item.poi_id || ('ev_' + self.page + '_' + idx),
              title: item.title || '精彩活动',
              summary: item.summary || '',
              tags: tags,
              tag: tags[0] || '推荐',
              link: item.source_link || '',
              type: tags[1] || tags[0] || 'show'
            }
          })

          if (self.page === 1) {
            self.events = parsed
          } else {
            self.events = self.events.concat(parsed)
          }

          self.noMore = (parsed.length < self.pageSize)
          self.loading = false
        },
        fail: function(err) {
          console.warn('[Events] 加载失败:', err)
          self.loading = false
          if (self.page === 1) self.events = []
          self.noMore = true
        }
      })
    },

    // 加载更多
    loadMore() {
      if (this.loading || this.noMore) return
      this.page++
      this.loadEvents()
    }
  }
}
