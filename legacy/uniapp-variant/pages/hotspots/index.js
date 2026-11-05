import config from '@/config/index.js'

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

function trimTag(tag) {
  if (!tag) return ''
  return tag.length > 4 ? tag.substring(0, 4) + '…' : tag
}

// 解析 summary 为结构化段落
function parseSections(summary) {
  if (!summary) return []
  const sections = []
  // 优先按换行分割
  const lines = summary.split('\n').filter(l => l.trim())
  if (lines.length > 1) {
    lines.forEach(line => {
      const t = line.trim()
      if (!t) return
      if (t.includes('位于') || t.includes('坐落') || t.includes('在') || t.includes('位置')) {
        sections.push({ type: 'location', text: t })
      } else if (t.includes('特色') || t.includes('特点') || t.includes('值得') || t.includes('推荐')) {
        sections.push({ type: 'feature', text: t })
      } else if (t.includes('时间') || t.includes('开放') || t.includes('门票') || t.includes('费用')) {
        sections.push({ type: 'info', text: t })
      } else if (t.includes('建议') || t.includes('提示') || t.includes('注意')) {
        sections.push({ type: 'tip', text: t })
      } else {
        sections.push({ type: 'desc', text: t })
      }
    })
    return sections
  }
  // 没有换行则整段作为描述
  return [{ type: 'desc', text: summary }]
}

export default {
  data() {
    return {
      navTop: 0,
      navHeight: 88,
      loading: false,
      loadingMore: false,
      noMore: false,
      page: 1,
      pageSize: 10,
      spots: [],
      currentTag: 'all',
      selectedSpot: null,
      // 互动状态
      likedSpots: new Set(),
      commentedText: '',
      showCommentInput: false
    }
  },

  computed: {
    categories() {
      const tagSet = new Set()
      this.spots.forEach(s => (s.tags || []).forEach(t => tagSet.add(t)))
      const tags = Array.from(tagSet)
      return [{ key: 'all', name: '全部' }, ...tags.map(t => ({ key: t, name: trimTag(t) }))]
    },
    filteredSpots() {
      if (this.currentTag === 'all') return this.spots
      return this.spots.filter(s => s.tags && s.tags.includes(this.currentTag))
    },
    // 详情页结构化段落
    detailSections() {
      if (!this.selectedSpot) return []
      return parseSections(this.selectedSpot.summary)
    },
    isCurrentSpotLiked() {
      return this.selectedSpot && this.likedSpots.has(this.selectedSpot.id)
    },
    isCurrentSpotFaved() {
      if (!this.selectedSpot) return false
      const favs = this.getFavSpots()
      return favs.includes(this.selectedSpot.id)
    }
  },

  onLoad() {
    const systemInfo = uni.getSystemInfoSync()
    this.navTop = systemInfo.statusBarHeight || 0
    this.navHeight = (systemInfo.statusBarHeight || 0) + 48
    // 从缓存恢复喜欢状态
    const saved = uni.getStorageSync('hotspot_likes')
    if (saved) this.likedSpots = new Set(saved)
    this.loadSpots()
  },

  methods: {
    getColor, getLetter,

    loadSpots(append = false) {
      if (append) {
        if (this.noMore || this.loadingMore) return
        this.loadingMore = true
      } else {
        this.loading = true
        this.page = 1
        this.noMore = false
      }
      uni.request({
        url: GUIDE_BASE + '/api/v1/guide/hot-spots',
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
          if (res.statusCode === 200 && items.length > 0) {
            const mapped = items.map((item, idx) => ({
              id: item.poi_id || ('hs_' + Date.now() + '_' + idx),
              name: item.title || '景点',
              summary: item.summary || '',
              tags: (item.tags || []).map(t => trimTag(t)),
              type: item.type || 'spot',
              link: item.source_link || '',
              likes: Math.floor(Math.random() * 500) + 100,
              comments: Math.floor(Math.random() * 80) + 10
            }))
            if (append) {
              this.spots = this.spots.concat(mapped)
            } else {
              this.spots = mapped
            }
            this.page++
          } else {
            if (append) this.noMore = true
          }
        },
        fail: () => { if (append) this.noMore = true },
        complete: () => { this.loading = false; this.loadingMore = false }
      })
    },

    onRefresh() { this.loadSpots(false) },
    loadMore() { this.loadSpots(true) },
    switchTag(key) { this.currentTag = key },

    // === 详情页 ===
    onSpotTap(spot) {
      this.selectedSpot = spot
      this.showCommentInput = false
      this.commentedText = ''
    },
    closeDetail() { this.selectedSpot = null },

    // 点赞
    toggleLike() {
      if (!this.selectedSpot) return
      const id = this.selectedSpot.id
      if (this.likedSpots.has(id)) {
        this.likedSpots.delete(id)
        this.selectedSpot.likes--
      } else {
        this.likedSpots.add(id)
        this.selectedSpot.likes++
      }
      uni.setStorageSync('hotspot_likes', Array.from(this.likedSpots))
    },

    // 评论
    openComment() {
      this.showCommentInput = true
    },
    submitComment() {
      if (!this.commentedText.trim()) return
      this.selectedSpot.comments++
      uni.showToast({ title: '评论成功', icon: 'success' })
      this.commentedText = ''
      this.showCommentInput = false
    },
    cancelComment() {
      this.commentedText = ''
      this.showCommentInput = false
    },

    // 收藏
    toggleFav() {
      if (!this.selectedSpot) return
      const id = this.selectedSpot.id
      let favs = this.getFavSpots()
      if (favs.includes(id)) {
        favs = favs.filter(f => f !== id)
        uni.showToast({ title: '已取消收藏', icon: 'none' })
      } else {
        favs.push(id)
        uni.showToast({ title: '已收藏', icon: 'success' })
      }
      uni.setStorageSync('hotspot_favs', favs)
    },
    getFavSpots() {
      try { return JSON.parse(uni.getStorageSync('hotspot_favs') || '[]') }
      catch { return [] }
    },

    // 打开外部链接
    openLink() {
      if (!this.selectedSpot || !this.selectedSpot.link) return
      plus.runtime.openURL(this.selectedSpot.link, function() {
        uni.setClipboardData({
          data: this.selectedSpot.link,
          success: function() { uni.showToast({ title: '链接已复制', icon: 'none' }) }
        })
      })
    },

    // 分享
    shareSpot() {
      if (!this.selectedSpot) return
      const name = this.selectedSpot.name
      // #ifdef APP-PLUS
      uni.share({
        provider: 'weixin',
        type: 0,
        title: name + ' - 随行推荐',
        summary: this.selectedSpot.summary?.substring(0, 50) || '',
        href: 'https://lingshan.com/scenic/' + this.selectedSpot.id,
        imageUrl: this.selectedSpot.cover,
        success: () => uni.showToast({ title: '分享成功' }),
        fail: () => {
          // 降级：复制链接
          uni.setClipboardData({
            data: 'https://lingshan.com/scenic/' + this.selectedSpot.id,
            success: () => uni.showToast({ title: '链接已复制' })
          })
        }
      })
      // #endif
      // #ifndef APP-PLUS
      uni.setClipboardData({
        data: name + ' - ' + (this.selectedSpot.summary?.substring(0, 50) || '') + ' https://lingshan.com/scenic/' + this.selectedSpot.id,
        success: () => uni.showToast({ title: '链接已复制到剪贴板' })
      })
      // #endif
    },

    goBack() {
      if (this.selectedSpot) {
        this.closeDetail()
        return
      }
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/home/index' }) })
    }
  }
}
