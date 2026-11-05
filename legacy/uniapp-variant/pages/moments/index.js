import api from '@/utils/api.js'
import config from '@/config/index.js'

// ========== 随机图片颜色池（前端生成占位图） ==========
const IMAGE_COLORS = [
  ['#41b5a0', '#c7f0e6'], ['#3ea1ff', '#d4e6ff'], ['#a78bfa', '#e6dfff'],
  ['#f472b6', '#fce4ec'], ['#fb923c', '#fff0e0'], ['#34d399', '#d1fae5'],
  ['#f97316', '#fff4e6'], ['#6366f1', '#e0e7ff'], ['#ec4899', '#fdf2f8'],
  ['#14b8a6', '#ccfbf1'], ['#8b5cf6', '#ede9fe'], ['#f59e0b', '#fef3c7']
]

const SCENIC_NAMES = ['灵山大佛', '梵宫', '九龙灌浴', '五印坛城', '曼飞龙塔', '天下第一掌', '祥符禅寺', '百子戏弥勒']

function randomImageStyle() {
  return IMAGE_COLORS[Math.floor(Math.random() * IMAGE_COLORS.length)]
}

function randomScenicName() {
  return SCENIC_NAMES[Math.floor(Math.random() * SCENIC_NAMES.length)]
}

// ========== 评论头像配色池 ==========
const COMMENT_COLORS = [
  '#41b5a0', '#5b8def', '#a78bfa', '#f472b6',
  '#fb923c', '#34d399', '#f97316', '#6366f1',
  '#ec4899', '#14b8a6', '#8b5cf6', '#f59e0b'
]

function getCommentAvatarColor(name) {
  if (!name) return COMMENT_COLORS[0]
  var hash = 0
  for (var i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return COMMENT_COLORS[Math.abs(hash) % COMMENT_COLORS.length]
}

function getCommentAvatarText(name) {
  return (name || '匿').charAt(0)
}

export default {
  data() {
    return {
      statusBarHeight: 20,
      safeBottom: 0,
      keyboardHeight: 0,
      showComposer: false,
      showCommentBar: false,
      commentFocus: false,
      commentPostId: null,
      commentText: '',
      activeSpotOnly: false,
      currentSpotName: config.SCENIC_NAME || '灵山胜境',
      draftText: '',
      draftImages: [],

      // 真实 API 数据
      feedList: [],
      displayFeedList: [],
      page: 1,
      pageSize: 10,
      hasMore: true,
      loadingMore: false,

      // 草稿箱
      showDraftBox: false,
      drafts: []
    }
  },

  onLoad() {
    this.initLayout()
    this.drafts = uni.getStorageSync('moments_drafts') || []
    this.loadPosts()
  },

  onShow() {
    this.initLayout()
    this.drafts = uni.getStorageSync('moments_drafts') || []
  },

  onPullDownRefresh() {
    this.page = 1
    this.hasMore = true
    this.loadPosts(true).finally(() => {
      uni.stopPullDownRefresh()
    })
  },

  onReachBottom() {
    if (this.hasMore && !this.loadingMore) {
      this.loadMorePosts()
    }
  },

  methods: {
    initLayout() {
      try {
        const info = uni.getSystemInfoSync()
        const safeBottom = info.safeArea ? Math.max(0, info.screenHeight - info.safeArea.bottom) : 0
        this.statusBarHeight = info.statusBarHeight || 20
        this.safeBottom = safeBottom
      } catch (e) {}
    },

    // ========== 加载帖子列表 ==========
    async loadPosts(isRefresh = false) {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const res = await api.getPosts(this.page, this.pageSize, userInfo.userId || 0)

        const newPosts = (res.data || []).map(p => this.formatPost(p))

        if (isRefresh || this.page === 1) {
          this.feedList = newPosts
        } else {
          this.feedList = [...this.feedList, ...newPosts]
        }

        this.hasMore = newPosts.length >= this.pageSize
        this.refreshDisplayFeed()

        // 加载新帖子的评论
        this.loadCommentsForPosts(newPosts)
      } catch (err) {
        console.error('加载帖子失败:', err)
      }
    },

    loadMorePosts() {
      if (this.loadingMore || !this.hasMore) return
      this.loadingMore = true
      this.page++
      this.loadPosts().finally(() => {
        this.loadingMore = false
      })
    },

    // ========== 批量加载帖子评论 ==========
    async loadCommentsForPosts(posts) {
      const self = this
      const promises = posts.map(post => {
        return api.getComments(post.id).then(res => {
          const apiComments = (res.data || []).map(c => {
            const name = c.nickname || c.username || '匿名'
            return {
              user: name,
              text: c.content,
              avatarText: getCommentAvatarText(name),
              avatarColor: getCommentAvatarColor(name)
            }
          })
          const idx = self.feedList.findIndex(p => p.id === post.id)
          if (idx === -1) return
          // 合并: API评论替换，但保留本地刚发的评论（无id标记）
          const localComments = (self.feedList[idx].comments || []).filter(c => !c._fromApi)
          const merged = [...apiComments.map(c => ({ ...c, _fromApi: true })), ...localComments]
          self.$set(self.feedList, idx, { ...self.feedList[idx], comments: merged })
        }).catch(() => {})
      })
      await Promise.all(promises)
      this.refreshDisplayFeed()
    },

    // ========== 数据格式转换 ==========
    formatPost(p) {
      const userInfo = uni.getStorageSync('userInfo') || {}

      // 前端随机生成占位图片（替代API真实图片）
      const apiImgCount = (p.images || []).length
      const randomImages = []
      for (let i = 0; i < apiImgCount && i < 9; i++) {
        const colors = randomImageStyle()
        randomImages.push({
          bgColor: colors[0],
          textColor: colors[1],
          label: randomScenicName().slice(0, 3)
        })
      }

      // 当前景点名：优先取 API 返回，无则用随机生成的景点名
      const spotName = p.location_name || randomScenicName()

      return {
        id: p.id,
        postId: p.id,
        userId: p.user_id,
        avatarText: (p.nickname || p.username || '游').charAt(0).toUpperCase(),
        name: p.nickname || p.username || '游客',
        isMine: (p.user_id === userInfo.userId),
        time: this.formatTime(p.create_time),
        spot: spotName,
        content: p.content || '',
        images: randomImages,
        liked: p.is_liked || false,
        likes: p.like_count || 0,
        likesText: p.like_count > 0 ? (p.is_liked ? '你和其他旅友等 ' + p.like_count + ' 人觉得很赞' : '和其他旅友等 ' + p.like_count + ' 人觉得很赞') : '',
        comments: [],
        commentCount: p.comment_count || 0,
        raw: p
      }
    },

    formatTime(timeStr) {
      if (!timeStr) return ''
      const now = Date.now()
      const t = new Date(timeStr).getTime()
      const diff = now - t
      const minute = 60 * 1000
      const hour = 60 * minute
      const day = 24 * hour

      if (diff < minute) return '刚刚'
      if (diff < hour) return Math.floor(diff / minute) + '分钟前'
      if (diff < day) return Math.floor(diff / hour) + '小时前'
      if (diff < 7 * day) return Math.floor(diff / day) + '天前'
      return timeStr.split(' ')[0]
    },

    refreshDisplayFeed() {
      const { feedList, activeSpotOnly, currentSpotName } = this
      this.displayFeedList = activeSpotOnly
        ? feedList.filter(item => item.spot === currentSpotName)
        : feedList
    },

    toggleSpotFilter() {
      this.activeSpotOnly = !this.activeSpotOnly
      this.refreshDisplayFeed()
    },

    openComposer() {
      this.closeCommentBar()
      this.showComposer = true
    },

    closeComposer() {
      const hasContent = (this.draftText || '').trim().length > 0 || (this.draftImages && this.draftImages.length > 0)
      if (hasContent) {
        uni.showModal({
          title: '保存草稿',
          content: '内容尚未发布，是否保存到草稿箱？',
          confirmText: '保存',
          cancelText: '不保存',
          success: (res) => {
            if (res.confirm) {
              this.saveDraft()
            } else {
              this.discardDraft()
            }
            this.showComposer = false
          }
        })
      } else {
        this.showComposer = false
      }
    },

    saveDraft() {
      const text = (this.draftText || '').trim()
      const images = this.draftImages || []
      if (!text && images.length === 0) return
      const drafts = uni.getStorageSync('moments_drafts') || []
      const newDraft = {
        id: Date.now(),
        text,
        images: images.slice(),
        time: new Date().toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit', month: '2-digit', day: '2-digit' })
      }
      drafts.unshift(newDraft)
      uni.setStorageSync('moments_drafts', drafts)
      uni.showToast({ title: '已存草稿箱', icon: 'success' })
      this.draftText = ''
      this.draftImages = []
    },

    discardDraft() {
      this.draftText = ''
      this.draftImages = []
    },

    loadDraft(draft) {
      this.draftText = draft.text
      this.draftImages = draft.images ? draft.images.slice() : []
      this.deleteDraft(draft.id, false)
      this.showDraftBox = false
      this.showComposer = true
    },

    deleteDraft(id, confirm = true) {
      const doDelete = () => {
        let drafts = uni.getStorageSync('moments_drafts') || []
        drafts = drafts.filter(d => d.id !== id)
        uni.setStorageSync('moments_drafts', drafts)
        this.drafts = drafts
      }
      if (confirm) {
        uni.showModal({
          title: '删除草稿',
          content: '确定删除这条草稿吗？',
          success: (res) => {
            if (res.confirm) doDelete()
          }
        })
      } else {
        doDelete()
      }
    },

    openDraftBox() {
      this.drafts = uni.getStorageSync('moments_drafts') || []
      this.showDraftBox = true
    },

    closeDraftBox() {
      this.showDraftBox = false
    },

    onDraftInput(e) {
      this.draftText = e.detail.value
    },

    chooseImages() {
      const remain = 9 - (this.draftImages ? this.draftImages.length : 0)
      if (remain <= 0) {
        uni.showToast({ title: '最多 9 张图片', icon: 'none' })
        return
      }
      uni.chooseImage({
        count: remain,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.draftImages = (this.draftImages || []).concat(res.tempFilePaths).slice(0, 9)
        }
      })
    },

    removeDraftImage(index) {
      const draftImages = this.draftImages.slice()
      draftImages.splice(index, 1)
      this.draftImages = draftImages
    },

    // ========== 发布帖子（对接真实 API）==========
    async publishPost() {
      const text = (this.draftText || '').trim()
      if (!text) {
        uni.showToast({ title: '先写点内容吧', icon: 'none' })
        return
      }

      const userInfo = uni.getStorageSync('userInfo')
      if (!userInfo || !userInfo.userId) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }

      uni.showLoading({ title: '发布中...', mask: true })

      try {
        // Step 1: 上传图片（如有）
        let uploadedImages = []
        if (this.draftImages && this.draftImages.length > 0) {
          uploadedImages = await api.uploadImages(this.draftImages)
        }

        // Step 2: 创建帖子
        const postRes = await api.createPost({
          user_id: userInfo.userId,
          content: text,
          location_name: this.currentSpotName,
          visibility: 0
        })

        const postId = postRes.data.post_id

        // Step 3: 绑定图片（如有）
        if (uploadedImages.length > 0) {
          await api.bindPostMedia(postId, {
            images: uploadedImages.map((img, i) => ({
              url: img.url,
              sort_order: i
            })),
            videos: []
          })
        }

        this.draftText = ''
        this.draftImages = []
        this.showComposer = false

        this.page = 1
        await this.loadPosts(true)

        uni.showToast({ title: '已发布', icon: 'success' })
      } catch (err) {
        console.error('发布失败:', err)
      }
    },

    // ========== 点赞（对接真实 API）==========
    async toggleLike(id) {
      const userInfo = uni.getStorageSync('userInfo')
      if (!userInfo || !userInfo.userId) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }

      // 乐观更新：先在本地更新 UI
      const list = this.feedList.map(item => {
        if (item.id !== id) return item
        const liked = !item.liked
        const likes = liked ? item.likes + 1 : Math.max(0, item.likes - 1)
        const likesText = liked
          ? (likes > 0 ? '你和其他旅友等 ' + likes + ' 人觉得很赞' : '')
          : (likes > 0 ? '和其他旅友等 ' + likes + ' 人觉得很赞' : '')
        return { ...item, liked, likes, likesText }
      })
      this.feedList = list
      this.refreshDisplayFeed()

      try {
        await api.togglePostLike(id, userInfo.userId)
      } catch (err) {
        // 失败时回滚
        console.error('点赞失败:', err)
        const list = this.feedList.map(item => {
          if (item.id !== id) return item
          const liked = !item.liked
          const likes = liked ? item.likes + 1 : Math.max(0, item.likes - 1)
          const likesText = liked
            ? (likes > 0 ? '你和其他旅友等 ' + likes + ' 人觉得很赞' : '')
            : (likes > 0 ? '和其他旅友等 ' + likes + ' 人觉得很赞' : '')
          return { ...item, liked, likes, likesText }
        })
        this.feedList = list
        this.refreshDisplayFeed()
      }
    },

    openCommentBar(id) {
      this.showComposer = false
      this.showCommentBar = true
      this.commentPostId = id
      this.commentText = ''
      // 等待 DOM 渲染后聚焦
      this.$nextTick(() => { this.commentFocus = true })
    },

    closeCommentBar() {
      this.showCommentBar = false
      this.commentFocus = false
      this.commentPostId = null
      this.commentText = ''
      uni.hideKeyboard()
      this.keyboardHeight = 0
    },

    onCommentInput(e) {
      this.commentText = e.detail.value
    },

    onCommentFocus() {
      this.showCommentBar = true
      this.commentFocus = true
    },

    onCommentBlur() {
      // adjust-position 模式下，blur 不主动关闭评论栏
      // 让用户点击遮罩或发送后才关闭
      this.commentFocus = false
    },

    onKeyboardHeightChange(e) {
      // adjust-position 模式下不再需要手动跟踪键盘高度
      // 保留空实现避免模板引用报错
    },

    async submitComment() {
      const text = (this.commentText || '').trim()
      if (!text || !this.commentPostId) {
        uni.showToast({ title: '先写评论内容', icon: 'none' })
        return
      }

      const userInfo = uni.getStorageSync('userInfo')
      if (!userInfo || !userInfo.userId) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }

      try {
        await api.createComment({
          post_id: this.commentPostId,
          user_id: userInfo.userId,
          content: text,
          parent_id: null
        })

        const list = this.feedList.map(item => {
          if (item.id !== this.commentPostId) return item
          const userName = userInfo.nickname || userInfo.username || '我'
          return {
            ...item,
            comments: (item.comments || []).concat([{
              user: userName,
              text,
              avatarText: getCommentAvatarText(userName),
              avatarColor: getCommentAvatarColor(userName)
            }]),
            commentCount: (item.commentCount || 0) + 1
          }
        })
        this.feedList = list
        this.refreshDisplayFeed()

        this.showCommentBar = false
        this.commentFocus = false
        this.commentPostId = null
        this.commentText = ''
        uni.hideKeyboard()
        this.keyboardHeight = 0

        uni.showToast({ title: '评论成功', icon: 'success' })
      } catch (err) {
        console.error('评论失败:', err)
      }
    },

    deletePost(id) {
      const userInfo = uni.getStorageSync('userInfo')
      if (!userInfo || !userInfo.userId) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }

      const post = this.feedList.find(item => item.id === id)
      if (post && post.userId !== userInfo.userId) {
        uni.showToast({ title: '只能删除自己的帖子', icon: 'none' })
        return
      }

      uni.showModal({
        title: '删除帖子',
        content: '确认删除这条动态吗？',
        success: async (res) => {
          if (!res.confirm) return

          try {
            await api.deletePost(id, userInfo.userId)
            const list = this.feedList.filter(item => item.id !== id)
            this.feedList = list
            this.refreshDisplayFeed()
            uni.showToast({ title: '已删除', icon: 'success' })
          } catch (err) {
            console.error('删除失败:', err)
          }
        }
      })
    },

    openPostDetail(id) {
      uni.showToast({ title: '帖子详情页已预留：' + id, icon: 'none' })
    },

    onPageTap() {
      if (this.showCommentBar || this.showComposer) {
        this.closeCommentBar()
        this.closeComposer()
      }
    },

    noop() {},

    onTabTap(name) {
      if (name === '首页') {
        uni.navigateBack({
          fail: () => uni.redirectTo({ url: '/pages/home/index' })
        })
        return
      }
      if (name === '数字人导航') {
        uni.navigateTo({ url: '/pages/digital-human/index' })
        return
      }
      if (name === '旅友圈') return
      if (name === '地图') {
        uni.navigateTo({ url: '/pages/map/index' })
        return
      }
      if (name === '我的') {
        uni.navigateTo({ url: '/pages/mine/index' })
        return
      }
      uni.showToast({ title: name + ' 待接页面', icon: 'none' })
    }
  }
}
