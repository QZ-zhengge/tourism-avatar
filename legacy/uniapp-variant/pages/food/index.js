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
      currentCat: 0,
      loading: false,
      noMore: false,
      page: 1,
      pageSize: 10,
      total: 0,
      categories: [
        { name: '全部', type: '' },
        { name: '本地特色', type: '本地' },
        { name: '小吃快餐', type: '小吃' },
        { name: '甜品饮品', type: '甜品' },
        { name: '火锅烧烤', type: '火锅' },
        { name: '异国料理', type: '料理' }
      ],
      foods: []
    }
  },

  computed: {
    filteredFoods() {
      if (this.currentCat === 0) return this.foods
      var type = this.categories[this.currentCat].type
      return this.foods.filter(function(f) {
        return f.tags && f.tags.indexOf(type) !== -1
      })
    }
  },

  onLoad() {
    var sys = uni.getSystemInfoSync()
    this.navTop = sys.statusBarHeight || 0
    this.navHeight = (sys.statusBarHeight || 0) + 48
    this.loadFoods()
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

    switchTab(index) {
      this.currentCat = index
    },

    onFoodTap(item) {
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

    loadFoods() {
      this.loading = (this.page === 1)
      var self = this
      uni.request({
        url: config.GUIDE_API_BASE + '/api/v1/guide/food',
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
            // 过滤掉"素斋"标签
            var rawTags = (item.tags || ['推荐']).filter(function(t) { return t !== '素斋' && t !== '素斋馆' })
            if (rawTags.length === 0) rawTags = ['推荐']
            return {
              id: item.poi_id || ('fd_' + self.page + '_' + idx),
              title: item.title || '美食推荐',
              summary: item.summary || '',
              tags: rawTags,
              tag: rawTags[0],
              link: item.source_link || ''
            }
          })

          if (self.page === 1) {
            self.foods = parsed
          } else {
            self.foods = self.foods.concat(parsed)
          }
          self.noMore = (parsed.length < self.pageSize)
          self.loading = false
        },
        fail: function(err) {
          console.warn('[Food] 加载失败:', err)
          self.loading = false
          if (self.page === 1) self.foods = []
          self.noMore = true
        }
      })
    },

    loadMore() {
      if (this.loading || this.noMore) return
      this.page++
      this.loadFoods()
    }
  }
}
