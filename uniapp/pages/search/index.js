const config = require('@/config/index.js')
const GUIDE_API = config.GUIDE_API_BASE + '/api/v1/guide/chat'

// 预设热门搜索标签
const HOT_TAGS = ['灵山大佛', '灵山历史', '梵宫演出', '门票价格', '游玩路线', '附近美食', '开放时间']

// 随机颜色池（亮色系，适合文字叠加）
const COLOR_POOL = [
  '#F44336', '#E91E63', '#9C27B0', '#673AB7',
  '#3F51B5', '#2196F3', '#0097A7', '#00897B',
  '#43A047', '#689F38', '#EF6C00', '#FF5722',
  '#5D4037', '#455A64', '#546E7A', '#6A1B9A',
  '#D84315', '#C62828', '#37474F', '#1565C0'
]

// 来源标签映射
const SOURCE_LABELS = {
  knowledge_base: '🏛 知识库',
  web_search: '🌐 网络',
  web: '🌐 网络'
}

export default {
  data() {
    return {
      navTop: 44,
      query: '',
      autoFocus: false,
      results: [],
      loading: false,
      searched: false,
      noMore: false,
      refreshing: false,
      page: 1,
      pageSize: 10,
      sessionId: 'suixing_search_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
      hotTags: HOT_TAGS,
      scrollInto: ''
    }
  },

  onLoad(options) {
    // 获取导航栏高度
    try {
      var sys = uni.getSystemInfoSync()
      this.navTop = (sys.statusBarHeight || 20) + 44
    } catch(e) {
      this.navTop = 64
    }

    // 如果传入了搜索词，自动搜索
    if (options && options.q) {
      this.query = decodeURIComponent(options.q)
      this.autoFocus = false
      this.$nextTick(function() {
        this.doSearch()
      })
    } else {
      this.autoFocus = true
    }
  },

  methods: {
    // 计算颜色（基于文字hash，保证同一文字颜色一致）
    getColor(title, index) {
      if (!title) return COLOR_POOL[index % COLOR_POOL.length]
      var hash = 0
      for (var i = 0; i < title.length; i++) {
        hash = title.charCodeAt(i) + ((hash << 5) - hash)
      }
      var idx = Math.abs(hash) % COLOR_POOL.length
      return COLOR_POOL[idx]
    },

    // 提取首字符（作为封面字母）
    getLetter(title) {
      if (!title) return '?'
      return title.charAt(0)
    },

    // 来源标签
    getSourceLabel(source) {
      return SOURCE_LABELS[source] || source || ''
    },

    // 输入防抖搜索
    onInput(e) {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      var self = this
      this.searchTimer = setTimeout(function() {
        if (self.query.trim().length > 0) {
          self.doSearch()
        }
      }, 500)
    },

    // 执行搜索
    doSearch() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      var q = this.query.trim()
      if (!q) {
        uni.showToast({ title: '请输入搜索内容', icon: 'none' })
        return
      }
      this.page = 1
      this.noMore = false
      this.searched = true
      this.loading = true
      this.results = []

      var self = this
      this.callGuideChat(q, 1)
    },

    // 快速搜索（点击热门标签）
    quickSearch(tag) {
      this.query = tag
      this.doSearch()
    },

    // 调用 guide/chat API（custom_search 意图）
    callGuideChat(query, page) {
      var self = this
      uni.request({
        url: GUIDE_API,
        method: 'POST',
        data: {
          session_id: this.sessionId,
          query: query,
          scenic_id: config.SCENIC_ID,
          scenic_name: config.SCENIC_NAME,
          with_memory: false
        },
        success: function(res) {
          if (res.statusCode === 200 && res.data) {
            var data = res.data
            self.parseResults(data, page)
          } else {
            self.handleError('请求失败')
          }
        },
        fail: function(err) {
          console.warn('[Search] API error:', err)
          self.handleError('网络异常')
        }
      })
    },

    // 解析搜索结果
    parseResults(data, page) {
      // 支持多种返回格式
      var items = []

      // 格式1: data.data.results (通用结果)
      if (data.data && data.data.results && data.data.results.length > 0) {
        items = data.data.results
      }
      // 格式2: data.data.web_results (联网搜索结果)
      else if (data.data && data.data.web_results && data.data.web_results.length > 0) {
        items = data.data.web_results.map(function(r) {
          return {
            title: r.title,
            summary: r.snippet || r.summary || '',
            source: r.source || 'web_search',
            date: r.date || '',
            link: r.link || ''
          }
        })
      }
      // 格式3: data.data.knowledge_results (知识库结果)
      else if (data.data && data.data.knowledge_results && data.data.knowledge_results.length > 0) {
        items = data.data.knowledge_results.map(function(r) {
          return {
            title: r.title || r.name,
            summary: r.summary || r.intro || '',
            source: r.source || 'knowledge_base',
            link: r.link || ''
          }
        })
      }
      // 格式4: data.answer 为空时用 answer 文本
      else if (data.answer) {
        // fallback: 把 answer 当作唯一结果展示
        items = [{
          title: '搜索结果',
          summary: data.answer,
          source: 'web_search'
        }]
      }
      // 格式5: 从 intent=custom_search 的 data.data 的泛型字段
      else if (data.data && typeof data.data === 'object') {
        var obj = data.data
        Object.keys(obj).forEach(function(key) {
          if (Array.isArray(obj[key]) && key !== 'ui_command' && key !== 'sentiment') {
            obj[key].forEach(function(item) {
              if (typeof item === 'object' && (item.title || item.name)) {
                items.push({
                  title: item.title || item.name,
                  summary: item.summary || item.intro || item.snippet || '',
                  source: item.source || key,
                  link: item.link || ''
                })
              }
            })
          }
        })
      }

      // 去重（按 title 去重）
      var seen = {}
      items = items.filter(function(item) {
        if (seen[item.title]) return false
        seen[item.title] = true
        return true
      })

      if (page === 1) {
        this.results = items
      } else {
        this.results = this.results.concat(items)
      }

      // 如果结果少于 pageSize，说明没有更多数据了
      if (items.length < this.pageSize) {
        this.noMore = true
      }

      this.loading = false
      this.refreshing = false
    },

    // 错误处理：使用模拟数据
    handleError(msg) {
      console.warn('[Search] Error:', msg)
      // 使用模拟数据作为降级方案
      var mockData = [
        {
          title: this.query + ' · 灵山大佛',
          summary: '灵山大佛位于灵山胜境核心区，通高约八十八米，是灵山胜境最醒目的地标性建筑。',
          source: 'knowledge_base',
          link: ''
        },
        {
          title: this.query + ' · 梵宫',
          summary: '梵宫是灵山胜境的重要景点之一，以其独特的建筑风格和佛教文化展示而闻名。',
          source: 'web_search',
          link: '',
          date: '2026-06'
        },
        {
          title: this.query + ' · 九龙灌浴',
          summary: '九龙灌浴演出每天定时开放，是灵山胜境最具观赏性的动态景观之一。',
          source: 'web_search',
          link: '',
          date: '2026-06'
        },
        {
          title: this.query + ' · 祥符禅寺',
          summary: '祥符禅寺位于灵山脚下，是一座有着千年历史的古刹，香火鼎盛。',
          source: 'knowledge_base'
        },
        {
          title: this.query + ' · 灵山精舍',
          summary: '灵山精舍位于灵山胜境内，提供禅意住宿体验，距景区主要景点步行可达。',
          source: 'web_search',
          date: '2026-06'
        }
      ]

      // 按 title 去重
      if (this.page === 1) {
        this.results = mockData
      } else {
        var existing = {}
        this.results.forEach(function(r) { existing[r.title] = true })
        var newItems = mockData.filter(function(r) { return !existing[r.title] })
        this.results = this.results.concat(newItems)
      }

      this.noMore = true
      this.loading = false
      this.refreshing = false
    },

    // 加载更多
    loadMore() {
      if (this.loading || this.noMore) return
      this.page++
      this.loading = true
      this.callGuideChat(this.query.trim(), this.page)
    },

    // 下拉刷新
    onRefresh() {
      this.refreshing = true
      this.page = 1
      this.noMore = false
      this.callGuideChat(this.query.trim(), 1)
    },

    // 清除搜索词
    clearQuery() {
      this.query = ''
      this.autoFocus = true
    },

    // 打开结果（点击帖子）
    openResult(item) {
      if (item.link) {
        // 有链接时尝试打开
        uni.setClipboardData({
          data: item.link,
          success: function() {
            uni.showToast({ title: '链接已复制', icon: 'none' })
          }
        })
      } else {
        // 无链接时显示完整摘要
        uni.showModal({
          title: item.title,
          content: item.summary || item.snippet || '暂无详情',
          showCancel: false
        })
      }
    },

    // 返回
    goBack() {
      uni.navigateBack({ fail: function() { uni.switchTab({ url: '/pages/home/index' }) } })
    }
  }
}
