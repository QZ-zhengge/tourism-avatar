const WEEK_NAMES = ['日', '一', '二', '三', '四', '五', '六']

export default {
  data() {
    return {
      navTop: 0,
      navHeight: 88,
      loading: false,
      dates: [],
      grouped: {},
      showAdd: false,
      editIndex: -1,
      editDateKey: '',
      addForm: {
        title: '',
        time: '',
        summary: '',
        type: 'route'
      },
      stats: { days: 0, items: 0, routes: 0, bookings: 0, notes: 0 },
      showDetail: false,
      detailItem: null
    }
  },

  onLoad() {
    var sys = uni.getSystemInfoSync()
    this.navTop = sys.statusBarHeight || 0
    this.navHeight = (sys.statusBarHeight || 0) + 48
    this.loadData()
  },

  onShow() {
    this.loadData()
  },

  methods: {
    loadData() {
      this.loading = true
      try {
        var key = 'suixing_itinerary'
        var raw = uni.getStorageSync(key) || '{}'
        var data = JSON.parse(raw)
        var keys = Object.keys(data).sort().reverse()
        this.dates = keys
        var grouped = {}
        var stats = { days: keys.length, items: 0, routes: 0, bookings: 0, notes: 0 }
        keys.forEach(function(k) {
          var items = data[k].items || []
          items.sort(function(a, b) { return (a.time || '').localeCompare(b.time || '') })
          grouped[k] = items
          stats.items += items.length
          items.forEach(function(it) {
            if (it.type === 'route') stats.routes++
            else if (it.type === 'booking') stats.bookings++
            else if (it.type === 'note') stats.notes++
          })
        })
        this.grouped = grouped
        this.stats = stats
      } catch(e) {
        console.warn('[Itinerary] 加载失败:', e)
        this.dates = []
        this.grouped = {}
        this.stats = { days: 0, items: 0, routes: 0, bookings: 0, notes: 0 }
      }
      this.loading = false
    },

    formatDate(dateStr) {
      var parts = dateStr.split('-')
      if (parts.length < 3) return dateStr
      return parseInt(parts[1]) + '月' + parseInt(parts[2]) + '日'
    },

    getWeekDay(dateStr) {
      var d = new Date(dateStr)
      var today = new Date()
      today = new Date(today.getFullYear(), today.getMonth(), today.getDate())
      var diff = Math.round((d - today) / 86400000)
      if (diff === 0) return '今天'
      if (diff === 1) return '明天'
      if (diff === -1) return '昨天'
      return '周' + WEEK_NAMES[d.getDay()]
    },

    isToday(dateStr) {
      var now = new Date()
      var today = now.getFullYear() + '-' + String(now.getMonth()+1).padStart(2,'0') + '-' + String(now.getDate()).padStart(2,'0')
      return dateStr === today
    },

    getTypeIcon(type, kind) {
      if (type === 'route') return '📍'
      if (type === 'booking' && kind === 'hotel') return '🏨'
      if (type === 'booking') return '🔖'
      return '📝'
    },

    getTypeTag(type, kind) {
      if (type === 'route') return { text: '路线', cls: 'tag-route' }
      if (type === 'booking' && kind === 'hotel') return { text: '酒店', cls: 'tag-hotel' }
      if (type === 'booking') return { text: '预订', cls: 'tag-booking' }
      return { text: '备忘', cls: 'tag-note' }
    },

    getTypeColor(type) {
      if (type === 'route') return '#2dba98'
      if (type === 'booking') return '#FF9F43'
      return '#5B8DEF'
    },

    showAddDialog() {
      this.addForm = { title: '', time: '', summary: '', type: 'route' }
      this.editIndex = -1
      this.editDateKey = ''
      this.showAdd = true
    },

    submitAdd() {
      if (!this.addForm.title.trim()) {
        uni.showToast({ title: '请输入标题', icon: 'none' })
        return
      }
      var now = new Date()
      var dateStr = now.getFullYear() + '-' + String(now.getMonth()+1).padStart(2,'0') + '-' + String(now.getDate()).padStart(2,'0')
      var timeStr = this.addForm.time || ''

      var entry = {
        type: this.addForm.type,
        title: this.addForm.title.trim(),
        summary: this.addForm.summary.trim(),
        time: timeStr,
        date: dateStr
      }

      if (this.editIndex >= 0 && this.editDateKey) {
        this.updateEntry(this.editDateKey, this.editIndex, entry)
      } else {
        this.saveEntry(dateStr, entry)
      }
      this.showAdd = false
      uni.showToast({ title: '已保存', icon: 'success' })
      this.loadData()
    },

    saveEntry(dateStr, entry) {
      try {
        var key = 'suixing_itinerary'
        var raw = uni.getStorageSync(key) || '{}'
        var data = JSON.parse(raw)
        if (!data[dateStr]) data[dateStr] = { date: dateStr, items: [] }
        data[dateStr].items.push(entry)
        uni.setStorageSync(key, JSON.stringify(data))
      } catch(e) {
        console.warn('[Itinerary] 保存失败:', e)
      }
    },

    updateEntry(dateStr, idx, entry) {
      try {
        var key = 'suixing_itinerary'
        var raw = uni.getStorageSync(key) || '{}'
        var data = JSON.parse(raw)
        if (data[dateStr] && data[dateStr].items[idx]) {
          data[dateStr].items[idx] = entry
          uni.setStorageSync(key, JSON.stringify(data))
        }
      } catch(e) {
        console.warn('[Itinerary] 更新失败:', e)
      }
    },

    deleteEntry(dateStr, idx) {
      var self = this
      uni.showModal({
        title: '删除行程',
        content: '确定要删除这项行程吗？',
        success: function(res) {
          if (res.confirm) {
            try {
              var key = 'suixing_itinerary'
              var raw = uni.getStorageSync(key) || '{}'
              var data = JSON.parse(raw)
              if (data[dateStr] && data[dateStr].items[idx]) {
                data[dateStr].items.splice(idx, 1)
                if (data[dateStr].items.length === 0) {
                  delete data[dateStr]
                }
                uni.setStorageSync(key, JSON.stringify(data))
                uni.showToast({ title: '已删除', icon: 'success' })
                self.loadData()
              }
            } catch(e) {
              console.warn('[Itinerary] 删除失败:', e)
            }
          }
        }
      })
    },

    onItemLongPress(item, dateKey, idx) {
      var self = this
      uni.showActionSheet({
        itemList: ['查看详情', '编辑', '删除'],
        success: function(res) {
          if (res.tapIndex === 0) {
            self.showDetail = true
            self.detailItem = item
          } else if (res.tapIndex === 1) {
            self.addForm = {
              title: item.title || '',
              time: item.time || '',
              summary: item.summary || '',
              type: item.type || 'note'
            }
            self.editIndex = idx
            self.editDateKey = dateKey
            self.showAdd = true
          } else if (res.tapIndex === 2) {
            self.deleteEntry(dateKey, idx)
          }
        }
      })
    },

    onItemTap(item, dateKey, idx) {
      this.detailItem = item
      this.showDetail = true
    },

    dayItemCount(items) {
      return (items || []).length + '项'
    },

    goBack() {
      uni.navigateBack({ fail: function() { uni.switchTab({ url: '/pages/home/index' }) } })
    },

    getDetailStr(item) {
      var lines = []
      if (item.type === 'route') {
        lines.push('📍 路线安排')
      } else if (item.type === 'booking' && item.kind === 'hotel') {
        lines.push('🏨 酒店预订')
        if (item.checkIn) lines.push('入住: ' + item.checkIn)
        if (item.checkOut) lines.push('离店: ' + item.checkOut)
        if (item.roomType) lines.push('房型: ' + item.roomType)
        if (item.price) lines.push('价格: ' + item.price)
        if (item.rating) lines.push('评分: ' + '⭐'.repeat(Math.round(parseFloat(item.rating) || 0)))
      } else {
        lines.push('📝 备忘')
      }
      if (item.summary) lines.push('\n' + item.summary)
      return lines.join('\n')
    },

    getHotelStatus(item) {
      if (item.type === 'booking' && item.kind === 'hotel') {
        if (item.checkIn && item.checkOut) {
          return item.checkIn + ' → ' + item.checkOut
        }
      }
      return ''
    }
  }
}
