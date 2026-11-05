// ============================================================
//  AI问答页面 - 连接 guide/chat API
// ============================================================

const config = require('@/config/index.js')
const GUIDE_BASE = config.GUIDE_API_BASE

// 快捷建议问题
const DEFAULT_SUGGESTIONS = [
  '灵山大佛有多高？',
  '景区开放时间是什么？',
  '怎么坐公交去景区？',
  '景区有什么好吃的？',
  '数字人导览怎么用？',
  '推荐几个必去景点',
]

export default {
  data() {
    return {
      // 导航栏适配
      navTop: 0,

      // 侧边栏
      showSidebar: false,
      currentHistoryIndex: -1,
      historyList: [],

      // 聊天消息
      messages: [],

      // 输入
      inputText: '',
      isRecording: false,

      // 状态
      isTyping: false,
      scrollTop: 0,
      sessionId: '',          // guide/chat 会话ID

      // 建议问题
      suggestions: DEFAULT_SUGGESTIONS,
      showSuggestions: true,
      showQuickActions: true,

      // 预订卡片 - 千问风格内嵌在消息中（不再使用弹窗）
      showBookingCard: false,
    }
  },

  onLoad() {
    // 获取状态栏高度
    const systemInfo = uni.getSystemInfoSync()
    this.navTop = systemInfo.statusBarHeight || 0
    // 生成新会话ID
    this.sessionId = 'suixing_' + this.generateUUID()
    // 同步适老化模式到后端
    this.syncElderlyMode()
    // 首次加载时同步数字人历史（如果有的话）
    this.syncDigitalHumanHistory()
  },

  onShow() {
    // 每次页面显示时同步数字人聊天记录
    this.syncDigitalHumanHistory()
  },

  methods: {
    // ========== 工具 ==========
    generateUUID() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0
        var v = c === 'x' ? r : (r & 0x3 | 0x8)
        return v.toString(16)
      })
    },

    // 从数字人页面同步聊天记录（追加模式，同步后清除缓存）
    syncDigitalHumanHistory() {
      try {
        const raw = uni.getStorageSync('dh_chat_history') || '[]'
        const history = JSON.parse(raw)
        if (history.length > 0) {
          // 追加到当前消息列表末尾（而非覆盖）
          for (var i = 0; i < history.length; i++) {
            var h = history[i]
            // 跳过空消息
            if (!h.content || h.content.trim() === '') continue
            // 避免重复：检查最后一条消息是否和当前相同
            var lastMsg = this.messages[this.messages.length - 1]
            if (lastMsg && lastMsg.content === h.content && lastMsg.role === h.role) continue
            this.messages.push({
              role: h.role,
              type: h.type || 'text',
              content: h.content,
              time: h.time || this.formatTime()
            })
          }
          this.showSuggestions = false
          this.$nextTick(() => this.scrollToBottom())
          // 同步后立即清除，防止重复加载
          uni.removeStorageSync('dh_chat_history')
        }
      } catch (e) {
        console.warn('[AI-Chat] 同步DH历史失败:', e)
      }
    },

    // ========== 侧边栏 ==========
    toggleSidebar() {
      this.showSidebar = !this.showSidebar
      // 打开侧边栏时刷新历史列表
      if (this.showSidebar) {
        this.loadHistoryList()
      }
    },

    // 从存储的消息中提取历史列表
    loadHistoryList() {
      try {
        const raw = uni.getStorageSync('dh_chat_history') || '[]'
        const history = JSON.parse(raw)
        // 每2条（一问一答）为一组历史记录
        const sessions = []
        for (let i = 0; i < history.length; i += 2) {
          if (history[i] && history[i].content) {
            sessions.push({
              title: history[i].content.substring(0, 20) + (history[i].content.length > 20 ? '...' : ''),
              time: history[i].time || ''
            })
          }
        }
        this.historyList = sessions.reverse()
      } catch (e) {
        console.warn('[AI-Chat] loadHistoryList failed:', e)
      }
    },

    selectHistory(index) {
      this.currentHistoryIndex = index
      this.showSidebar = false
      uni.showToast({ title: '加载历史会话', icon: 'none' })
    },

    createNewChat() {
      this.messages = []
      this.inputText = ''
      this.currentHistoryIndex = -1
      this.showSidebar = false
      this.showSuggestions = true
      // 重新生成会话ID
      this.sessionId = 'suixing_' + this.generateUUID()
      uni.showToast({ title: '新建对话', icon: 'success' })
    },

    // ========== 发送消息 ==========
    sendMessage() {
      const text = this.inputText.trim()
      if (!text) return

      // 添加用户消息
      this.messages.push({
        role: 'user',
        type: 'text',
        content: text,
        time: this.formatTime()
      })

      this.inputText = ''
      this.showSuggestions = false
      this.scrollToBottom()

      // 显示AI正在输入
      this.isTyping = true
      this.scrollToBottom()

      // 调用 guide/chat API
      this.callGuideChat(text)
    },

    // 发送建议问题
    sendSuggestion(suggestion) {
      this.inputText = suggestion
      this.sendMessage()
    },

    // 同步适老化模式到后端
    syncElderlyMode() {
      const agingMode = uni.getStorageSync('agingMode')
      if (agingMode) {
        uni.request({
          url: GUIDE_BASE + '/api/v1/guide/elderly-mode',
          method: 'POST',
          header: { 'Content-Type': 'application/json' },
          data: { enabled: true, session_id: this.sessionId },
          fail: function() { console.warn('[AI-Chat] 适老化设置失败') }
        })
      }
    },

    // 调用 guide/chat API
    callGuideChat(query) {
      const BOOKING_KEYWORDS = ['订酒店', '预订', '订房', '订餐', '订票', '买票', '预约', '定酒店', '住哪', '吃饭', '住宿', '餐厅', '酒店']
      uni.request({
        url: GUIDE_BASE + '/api/v1/guide/chat',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        data: {
          session_id: this.sessionId,
          query: query,
          scenic_id: config.SCENIC_ID,
          scenic_name: config.SCENIC_NAME,
          with_memory: true
        },
        success: (res) => {
          this.isTyping = false

          if (res.statusCode === 200 && res.data) {
            const data = res.data
            const answer = data.answer || data.speech_text || '抱歉，暂时无法回答您的问题。'
            const speechText = data.speech_text || data.answer || answer
            const intent = data.intent || 'knowledge_qa'

            // 添加AI回复
            this.messages.push({
              role: 'ai',
              type: 'text',
              content: answer,
              speechText: speechText,
              time: this.formatTime()
            })
            this.scrollToBottom()

            // ========== 自动添加到行程 ==========
            // 如果用户明确要求添加到行程中
            if (this.isAddToItinerary(query)) {
              this.autoParseAndSaveItinerary(query, answer)
            }

            // 检测预订意图 → 在聊天记录中内嵌千问/美团风格卡片
            const isBooking = intent === 'agent_task' ||
                BOOKING_KEYWORDS.some(kw => query.indexOf(kw) >= 0)
            if (isBooking) {
              // 先检查是否有美团酒店卡片列表
              var hotelCards = null
              var resultData = data.data || data
              var cardData = resultData.card_data
              if (cardData && cardData.type === 'booking_cards' && cardData.items && cardData.items.length > 0) {
                hotelCards = cardData.items
              } else if (resultData.result && resultData.result.cards && resultData.result.cards.length > 0) {
                hotelCards = resultData.result.cards
              } else if (resultData.result && resultData.result.meituan && resultData.result.meituan.items && resultData.result.meituan.items.length > 0) {
                hotelCards = resultData.result.meituan.items
              }

              if (hotelCards && hotelCards.length > 0) {
                // 渲染美团风格多酒店卡片列表
                this.messages.push({
                  role: 'ai',
                  type: 'hotel-list',
                  content: answer,
                  cards: hotelCards,
                  time: this.formatTime()
                })
              } else {
                // 降级：单卡片预订信息
                var cardDataSimple = {
                  name: '',
                  roomType: '',
                  price: '',
                  checkIn: '',
                  checkOut: '',
                  note: '',
                  rating: '',
                  tag: '',
                  address: ''
                }
                if (cardData && cardData.items && cardData.items.length > 0) {
                  var first = cardData.items[0]
                  cardDataSimple.name = first.title || ''
                  cardDataSimple.price = first.price || ''
                  cardDataSimple.rating = first.rating || ''
                  cardDataSimple.tag = (first.kind || 'hotel')
                  cardDataSimple.address = first.distance || ''
                }
                this.messages.push({
                  role: 'ai',
                  type: 'card',
                  cardType: 'booking',
                  content: '为您找到以下预订信息，请确认或修改：',
                  data: cardDataSimple,
                  confirmed: false,
                  time: this.formatTime()
                })
              }
              this.scrollToBottom()
            }
          } else {
            // API错误
            this.messages.push({
              role: 'ai',
              type: 'text',
              content: '网络请求失败，请稍后再试。',
              time: this.formatTime()
            })
            this.scrollToBottom()
          }
        },
        fail: (err) => {
          this.isTyping = false
          console.error('[AI-Chat] guide/chat 请求失败:', JSON.stringify(err))

          this.messages.push({
            role: 'ai',
            type: 'text',
            content: '网络连接失败，请检查网络后重试。',
            time: this.formatTime()
          })
          this.scrollToBottom()
        }
      })
    },

    // ========== 输入处理 ==========
    onInput(e) {
      this.inputText = e.detail.value
    },

    // ========== 美团酒店卡片操作 ==========
    openHotelLink(url) {
      if (!url) {
        uni.showToast({ title: '暂无预订链接', icon: 'none' })
        return
      }
      uni.setClipboardData({
        data: url,
        success: () => {
          uni.showToast({ title: '预订链接已复制', icon: 'success' })
        }
      })
    },

    onHotelAction(action, card) {
      if (action.type === 'book_now' && action.url) {
        this.openHotelLink(action.url)
      } else if (action.type === 'call') {
        this.callHotel(action.phone || card.phone)
      }
    },

    callHotel(phone) {
      if (!phone) {
        uni.showToast({ title: '暂无联系电话', icon: 'none' })
        return
      }
      uni.makePhoneCall({
        phoneNumber: phone,
        fail: () => {
          uni.showToast({ title: '拨号失败', icon: 'none' })
        }
      })
    },

    goDigitalHuman() {
      uni.switchTab({ url: '/pages/digital-human/index' })
    },

    // ========== 语音输入 ==========
    toggleVoice() {
      uni.showModal({
        title: '语音输入',
        content: '语音功能需要授权麦克风权限，是否开启？',
        confirmText: '开启',
        success: (res) => {
          if (res.confirm) {
            this.startVoiceInput()
          }
        }
      })
    },

    startVoiceInput() {
      this.isRecording = true
      setTimeout(() => {
        this.isRecording = false
        this.inputText = '帮我查一下景区门票价格'
        this.sendMessage()
      }, 2000)
    },

    // ========== 图片选择 ==========
    chooseImage() {
      uni.chooseImage({
        count: 1,
        success: (res) => {
          this.messages.push({
            role: 'user',
            type: 'text',
            content: '[图片]',
            time: this.formatTime()
          })
          this.scrollToBottom()

          // 用图片识别 API（暂用文字回复）
          this.isTyping = true
          this.scrollToBottom()

          setTimeout(() => {
            this.isTyping = false
            this.messages.push({
              role: 'ai',
              type: 'text',
              content: '收到您的图片，我来为您识别分析。',
              time: this.formatTime()
            })
            this.scrollToBottom()
          }, 1500)
        }
      })
    },

    // ========== 快捷功能 ==========
    onQuickAction(type) {
      switch (type) {
        case 'fast':
          this.inputText = '快速问答'
          this.sendMessage()
          break
        case 'photo':
          this.chooseImage()
          break
        case 'video':
          this.goDigitalHuman()
          break
        case 'itinerary':
          this.addInputToItinerary()
          break
      }
    },

    // 将输入框文字添加到行程
    addInputToItinerary() {
      var text = this.inputText.trim()
      if (!text) {
        // 如果输入框为空，用快捷方式发送一句固定文案触发AI
        this.inputText = '帮我推荐一下今天的游玩路线并添加到行程'
        this.sendMessage()
        return
      }
      // 如果输入框有内容，将其添加到行程
      try {
        var now = new Date()
        var dateStr = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0')
        var timeStr = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0')

        var entry = {
          type: 'route',
          title: text.substring(0, 40),
          summary: text.length > 40 ? text.substring(0, 100) : '',
          time: timeStr,
          date: dateStr
        }

        var key = 'suixing_itinerary'
        var raw = uni.getStorageSync(key) || '{}'
        var data = JSON.parse(raw)
        if (!data[dateStr]) data[dateStr] = { date: dateStr, items: [] }
        data[dateStr].items.push(entry)
        uni.setStorageSync(key, JSON.stringify(data))

        uni.showToast({ title: '已添加到行程', icon: 'success' })

        // 添加用户消息
        this.messages.push({
          role: 'user',
          type: 'text',
          content: '添加到行程：' + text,
          time: this.formatTime()
        })
        this.messages.push({
          role: 'ai',
          type: 'text',
          content: '✅ 已帮你把「' + text.substring(0, 20) + '」添加到行程中，可前往"我的行程"查看。',
          time: this.formatTime()
        })
        this.inputText = ''
        this.showSuggestions = false
        this.scrollToBottom()
      } catch (e) {
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
    },

    // ========== 导航 ==========
    goBack() {
      uni.navigateBack({ fail: function() { uni.switchTab({ url: '/pages/home/index' }) } })
    },

    goDigitalHuman() {
      uni.switchTab({ url: '/pages/digital-human/index' })
    },

    showMore() {
      uni.showActionSheet({
        itemList: ['清空对话', '清空会话记忆', '设置'],
        success: (res) => {
          switch (res.tapIndex) {
            case 0:
              this.messages = []
              this.showSuggestions = true
              uni.showToast({ title: '已清空', icon: 'success' })
              break
            case 1:
              this.clearMemory()
              break
            case 2:
              uni.showToast({ title: '设置功能开发中', icon: 'none' })
              break
          }
        }
      })
    },

    // 清除会话记忆
    clearMemory() {
      uni.request({
        url: GUIDE_BASE + '/api/v1/guide/memory/' + this.sessionId,
        method: 'DELETE',
        success: (res) => {
          uni.showToast({ title: '会话记忆已清除', icon: 'success' })
        },
        fail: () => {
          uni.showToast({ title: '清除失败', icon: 'none' })
        }
      })
    },

    showMoreInput() {
      uni.showActionSheet({
        itemList: ['相册', '拍照', '位置', '文件'],
        success: (res) => {
          uni.showToast({ title: ['相册', '拍照', '位置', '文件'][res.tapIndex] + '功能开发中', icon: 'none' })
        }
      })
    },

    // ========== 工具方法 ==========
    scrollToBottom() {
      this.$nextTick(() => {
        const query = uni.createSelectorQuery().in(this)
        query.select('.chat-content').boundingClientRect()
        query.exec((res) => {
          if (res && res[0]) {
            this.scrollTop = res[0].height + 9999
          }
        })
      })
    },

    formatTime() {
      const now = new Date()
      const hours = now.getHours().toString().padStart(2, '0')
      const minutes = now.getMinutes().toString().padStart(2, '0')
      return `${hours}:${minutes}`
    },

    // ========== 自动保存行程（从AI回答中解析路线）==========
    // 检测用户是否要求添加到行程
    isAddToItinerary(query) {
      var keywords = ['添加到行程', '加入行程', '添加到行程中', '记入行程', '规划行程', '帮我安排', '帮我规划', '设计路线', '制定行程']
      return keywords.some(function(kw) {
        return query.indexOf(kw) >= 0
      })
    },

    // 从AI回答中解析路线项并保存到行程
    autoParseAndSaveItinerary(query, answer) {
      try {
        var now = new Date()
        var today = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0')

        // 解析AI回答中的路线项
        var items = this.extractRouteItems(answer)

        if (items.length === 0) {
          // 如果AI回答中没有解析出结构化路线，则将整个回答作为一条路线备忘保存
          var title = this.extractTitle(query)
          items.push({
            type: 'route',
            title: title,
            summary: answer.substring(0, 100) + (answer.length > 100 ? '...' : ''),
            time: '',
            date: today
          })
        }

        // 写入行程存储
        var key = 'suixing_itinerary'
        var raw = uni.getStorageSync(key) || '{}'
        var data = JSON.parse(raw)

        items.forEach(function(item) {
          var dateKey = item.date || today
          if (!data[dateKey]) data[dateKey] = { date: dateKey, items: [] }
          // 避免重复
          var exists = data[dateKey].items.some(function(ex) {
            return ex.title === item.title && ex.time === item.time
          })
          if (!exists) {
            data[dateKey].items.push(item)
          }
        })

        uni.setStorageSync(key, JSON.stringify(data))
        uni.showToast({ title: '已添加到行程（共' + items.length + '项）', icon: 'success' })

        // 在聊天中显示提示
        var count = items.length
        this.messages.push({
          role: 'ai',
          type: 'text',
          content: '✅ 已将 ' + count + ' 项行程安排保存到「我的行程」中，可前往查看。',
          time: this.formatTime()
        })
        this.scrollToBottom()
      } catch (e) {
        console.warn('[AI-Chat] 自动保存行程失败:', e)
      }
    },

    // 从AI回答文本中提取路线项
    extractRouteItems(text) {
      var items = []
      var lines = text.split('\n')

      // 尝试解析带时间的行，如 "9:00 参观灵山大佛" 或 "- 09:00 灵山大佛"
      var timePattern = /(?:^|\s)([0-2]?[0-9]:[0-5][0-9])\s*(?:[-–—：:]+\s*)?(.+)/

      // 尝试解析编号列表，如 "1. 上午去灵山大佛"
      var numberedPattern = /^\s*(?:\d+[\.\)、]\s*)(.+)/

      // 尝试解析时间词，如 "上午"、"下午"、"中午"
      var timeWordPattern = /^\s*(上午|下午|中午|傍晚|晚上|早晨|清晨|早上)\s*(.+)/

      var now = new Date()
      var today = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0')

      lines.forEach(function(line) {
        line = line.trim()
        if (!line) return

        // 跳过明显的标题行和说明行
        if (/^(方案[一二三]|行程|路线|推荐|建议|以下|第[一二三四五六日]天)/.test(line)) return
        if (line.length < 4) return

        var match

        // 模式1: 带具体时间的行 "9:00 参观大佛"
        match = line.match(timePattern)
        if (match) {
          items.push({
            type: 'route',
            title: match[2].trim().substring(0, 40),
            summary: line.substring(0, 80),
            time: match[1],
            date: today
          })
          return
        }

        // 模式2: 时间词开头的行 "上午去大佛"
        match = line.match(timeWordPattern)
        if (match) {
          var timeMap = { '早晨': '07:00', '早上': '07:00', '清晨': '06:00', '上午': '09:00', '中午': '12:00', '下午': '14:00', '傍晚': '17:00', '晚上': '19:00' }
          items.push({
            type: 'route',
            title: match[2].trim().substring(0, 40),
            summary: line.substring(0, 80),
            time: timeMap[match[1]] || '09:00',
            date: today
          })
          return
        }

        // 模式3: 编号行 "1. 参观灵山大佛"
        match = line.match(numberedPattern)
        if (match) {
          var titleText = match[1].replace(/^(去|参观|游览|逛|玩|前往|到)\s*/, '').trim()
          if (titleText.length > 2) {
            items.push({
              type: 'route',
              title: titleText.substring(0, 40),
              summary: line.substring(0, 80),
              time: '',
              date: today
            })
          }
        }
      })

      return items
    },

    // 从用户查询中提取行程标题
    extractTitle(query) {
      // 去除"帮我添加到行程"等前缀
      var cleaned = query.replace(/^(请|帮我|帮我把|把)\s*/, '')
        .replace(/添加(到|至)?行程|加入行程|记入行程|规划行程|安排行程/g, '')
        .replace(/\s+/g, '')
        .trim()
      if (cleaned.length > 20) cleaned = cleaned.substring(0, 20) + '...'
      return cleaned || '旅行安排'
    },

    // ========== 预订卡片（千问风格内嵌）==========
    updateCardField(index, field, value) {
      var msg = this.messages[index]
      if (msg && msg.data) {
        msg.data[field] = value
      }
    },

    confirmBookingFromCard(index) {
      var msg = this.messages[index]
      if (!msg || !msg.data) return
      var d = msg.data

      if (!d.name) {
        uni.showToast({ title: '请填写酒店名称', icon: 'none' })
        return
      }
      var now = new Date()
      var dateStr = d.checkIn || (
        now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0')
      )
      var timeStr = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0')

      var summaryParts = []
      if (d.roomType) summaryParts.push(d.roomType)
      if (d.checkOut) summaryParts.push('离店: ' + d.checkOut)
      if (d.price) summaryParts.push(d.price)
      if (d.note) summaryParts.push(d.note)

      var entry = {
        type: 'booking',
        kind: 'hotel',
        title: d.name,
        price: d.price,
        rating: d.rating,
        summary: summaryParts.join(' · '),
        time: timeStr,
        date: dateStr,
        checkIn: d.checkIn,
        checkOut: d.checkOut,
        roomType: d.roomType,
        note: d.note
      }

      try {
        var key = 'suixing_itinerary'
        var raw = uni.getStorageSync(key) || '{}'
        var data = JSON.parse(raw)
        if (!data[dateStr]) data[dateStr] = { date: dateStr, items: [] }
        data[dateStr].items.push(entry)
        uni.setStorageSync(key, JSON.stringify(data))
        // 标记该卡片已确认
        this.$set(msg, 'confirmed', true)
        uni.showToast({ title: '已添加到行程', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
    },

    // 旧弹窗方法（保留兼容，不再调用）
    closeBookingCard() {
      this.showBookingCard = false
    },

    getBcColor(title) {
      const colors = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
        'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
        'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)'
      ]
      let hash = 0
      const s = title || '?'
      for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash)
      return colors[Math.abs(hash) % colors.length]
    },

    getBcLetter(title) {
      if (!title) return '?'
      const first = title.trim().charAt(0)
      return /[\u4e00-\u9fa5]/.test(first) ? first : first.toUpperCase()
    }
  }
}
