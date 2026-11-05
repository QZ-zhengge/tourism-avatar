const config = require('@/config/index.js')

// 颜色池（亮色系，适合白色文字叠加）
const COLOR_POOL = [
  '#F44336', '#E91E63', '#9C27B0', '#673AB7',
  '#3F51B5', '#2196F3', '#0097A7', '#00897B',
  '#43A047', '#689F38', '#EF6C00', '#FF5722',
  '#5D4037', '#455A64', '#546E7A', '#6A1B9A',
  '#D84315', '#C62828', '#37474F', '#1565C0'
]

// 来源标签
const SOURCE_LABELS = {
  knowledge_base: '🏛 官方攻略',
  web_search: '🌐 网络推荐',
  web: '🌐 网络推荐'
}

export default {
  data() {
    return {
      navTop: 0,
      navHeight: 88,
      loading: false,
      refreshing: false,
      noMore: false,
      page: 1,
      pageSize: 10,
      total: 0,
      results: [],
      tags: [],
      selectedTag: '',
      checklist: [
        { id: 'c1', text: '身份证/学生证等证件', checked: true },
        { id: 'c2', text: '手机、充电宝', checked: true },
        { id: 'c3', text: '换洗衣物、洗漱用品', checked: false },
        { id: 'c4', text: '相机/自拍杆', checked: false },
        { id: 'c5', text: '防晒霜、墨镜、帽子', checked: false },
        { id: 'c6', text: '常用药品（创可贴、肠胃药）', checked: false },
        { id: 'c7', text: '零食、饮用水', checked: false }
      ]
    }
  },

  onLoad() {
    var sys = uni.getSystemInfoSync()
    this.navTop = sys.statusBarHeight || 0
    this.navHeight = (sys.statusBarHeight || 0) + 48
    this.loadGuideTips(1)
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

    getSourceLabel(source) {
      return SOURCE_LABELS[source] || source || '📄 攻略'
    },

    // 搜索入口（导航栏按钮）
    onSearch() {
      var self = this
      uni.showModal({
        title: '搜索攻略',
        content: '',
        editable: true,
        placeholderText: '输入关键词，如：亲子游、美食...',
        success: function(res) {
          if (res.confirm && res.content && res.content.trim()) {
            self.selectedTag = ''
            self.loadGuideTips(1)
          }
        }
      })
    },

    // 点击分类标签
    onTagTap(tag) {
      this.selectedTag = (this.selectedTag === tag) ? '' : tag
      this.loadGuideTips(1)
    },

    // 加载游玩攻略（直接调用 guide-tips API）
    loadGuideTips(page) {
      this.loading = (page === 1)
      this.page = page

      var self = this
      uni.request({
        url: config.GUIDE_API_BASE + '/api/v1/guide/guide-tips',
        method: 'GET',
                data: {
          scenic_id: config.SCENIC_ID,
          scenic_name: config.SCENIC_NAME,
          page: page,
          page_size: this.pageSize
        },
        success: function(res) {
          if (res.statusCode !== 200 || !res.data) {
            self.loading = false
            self.refreshing = false
            return
          }

          var data = res.data
          var items = data.items || []
          self.total = data.total || 0

          // 提取所有标签（去重）
          var allTags = {}
          items.forEach(function(item) {
            if (item.tags && item.tags.length > 0) {
              item.tags.forEach(function(tag) {
                // 只取短标签（过滤冗长的描述性标签）
                if (tag.length <= 6) {
                  allTags[tag] = true
                }
              })
            }
          })
          var tagList = Object.keys(allTags)
          if (tagList.length > 0) {
            self.tags = tagList
          }

          // 根据选中的标签过滤
          var filtered = items
          if (self.selectedTag) {
            filtered = items.filter(function(item) {
              return item.tags && item.tags.indexOf(self.selectedTag) !== -1
            })
          }

          // 转为帖子格式
          var parsed = filtered.map(function(item, idx) {
            return {
              id: item.poi_id || ('guide_' + page + '_' + idx),
              title: item.title || '游玩攻略',
              summary: (item.summary || '').substring(0, 120) + ((item.summary || '').length > 120 ? '...' : ''),
              source: item.source || 'knowledge_base',
              link: item.source_link || '',
              type: item.type || 'article'
            }
          })

          if (page === 1) {
            self.results = parsed
          } else {
            self.results = self.results.concat(parsed)
          }

          self.noMore = (parsed.length < self.pageSize)
          self.loading = false
          self.refreshing = false
        },
        fail: function(err) {
          console.warn('[Guide] 接口请求失败:', err)
          self.loading = false
          self.refreshing = false
          if (page === 1) {
            self.results = []
          }
          self.noMore = true
        }
      })
    },

    // 点击帖子 → 打开网址
    onPostTap(item) {
      if (item.link) {
        // 有网址 → 打开系统浏览器或 WebView
        plus.runtime.openURL(item.link, function(err) {
          // plus 不可用时（如微信小程序）降级到复制链接
          uni.setClipboardData({
            data: item.link,
            success: function() {
              uni.showToast({ title: '链接已复制到剪贴板', icon: 'none' })
            }
          })
        })
      } else {
        // 无链接 → 显示详情弹窗
        uni.showModal({
          title: item.title,
          content: item.summary || '暂无详情',
          showCancel: false
        })
      }
    },

    // 下拉刷新
    onRefresh() {
      this.refreshing = true
      this.selectedTag = ''
      this.loadGuideTips(1)
    },

    // 加载更多
    loadMore() {
      if (this.loading || this.noMore) return
      this.loadGuideTips(this.page + 1)
    },

    goBack() {
      uni.navigateBack({ fail: function() { uni.switchTab({ url: '/pages/home/index' }) } })
    },

    onAddItem() {
      uni.showToast({ title: '添加清单功能开发中', icon: 'none' })
    },

    onToggleItem(index) {
      this.checklist[index].checked = !this.checklist[index].checked
    }
  }
}
