const config = require('@/config/index.js')

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
      currentFilter: 0,
      loading: false,
      noMore: false,
      page: 1,
      pageSize: 10,
      total: 0,
      filters: [
        { name: '全部', type: '' },
        { name: '餐饮', type: '餐饮' },
        { name: '住宿', type: '住宿' },
        { name: '购物', type: '购物' },
        { name: '服务', type: '服务' }
      ],
      shops: []
    }
  },

  computed: {
    filteredShops() {
      if (this.currentFilter === 0) return this.shops
      var type = this.filters[this.currentFilter].type
      return this.shops.filter(function(s) {
        return s.tags && s.tags.indexOf(type) !== -1
      })
    }
  },

  onLoad() {
    var sys = uni.getSystemInfoSync()
    this.navTop = sys.statusBarHeight || 0
    this.navHeight = (sys.statusBarHeight || 0) + 48
    this.loadShops()
  },

  methods: {
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

    onFilterChange(index) {
      this.currentFilter = index
    },

    onShopTap(item) {
      if (item.link) {
        plus.runtime.openURL(item.link, function() {
          uni.setClipboardData({
            data: item.link,
            success: function() { uni.showToast({ title: '链接已复制', icon: 'none' }) }
          })
        })
      } else if (item.summary) {
        uni.showModal({ title: item.title, content: item.summary, showCancel: false })
      }
    },

    loadShops() {
      this.loading = (this.page === 1)
      var self = this
      uni.request({
        url: config.GUIDE_API_BASE + '/api/v1/guide/nearby-shops',
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
            return {
              id: item.poi_id || ('ns_' + self.page + '_' + idx),
              title: item.title || '周边好店',
              summary: item.summary || '',
              tags: item.tags || ['推荐'],
              tag: (item.tags && item.tags[0]) || '推荐',
              link: item.source_link || ''
            }
          })

          if (self.page === 1) {
            self.shops = parsed
          } else {
            self.shops = self.shops.concat(parsed)
          }
          self.noMore = (parsed.length < self.pageSize)
          self.loading = false
        },
        fail: function(err) {
          console.warn('[Nearby] 加载失败:', err)
          self.loading = false
          if (self.page === 1) self.shops = []
          self.noMore = true
        }
      })
    },

    loadMore() {
      if (this.loading || this.noMore) return
      this.page++
      this.loadShops()
    }
  }
}
