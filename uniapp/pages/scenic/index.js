const config = require('@/config/index.js')

// 知识图谱API基础URL
const KG_API_BASE = config.GUIDE_API_BASE + '/knowledge-graph'

// 节点类型配置
const NODE_COLORS = {
  ScenicSpot: '#F5A623',
  City: '#409EFF',
  Person: '#52c41a',
  Dynasty: '#faad14',
  Event: '#722ed1',
  Show: '#eb2f96',
  Food: '#FF6B6B',
  Hotel: '#2db7f5',
  Tag: '#8c8c8c'
}

// 关系类型中文映射
const RELATION_LABELS = {
  LOCATED_IN: '位于',
  NEAR_WITH: '附近',
  HAS_FOOD: '包含美食',
  HAS_ACCOMMODATION: '包含住宿',
  BELONG_TO_STYLE: '主题标签',
  RELATED_TO: '关联人物',
  BUILT_IN: '建于',
  HAS_EVENT: '关联事件',
  HAS_SHOW: '关联演出'
}

export default {
  data() {
    return {
      // 统计卡片
      nodeCount: 0,
      relationCount: 0,
      typeDistribution: [],

      // 图谱数据
      graphData: { nodes: [], edges: [] },

      // G6就绪标记
      g6Ready: false,

      // 搜索相关
      searchQuery: '',
      searchResults: [],
      searchTimer: null,
      showSearchResult: false,

      // 选中的节点
      selectedNode: null,
      selectedRelations: [],

      // 标签筛选
      tags: [],
      selectedTag: null,
      filterResults: [],

      // 路线推荐
      routeStart: null,
      routeList: [],

      // 知识探索
      exploreEntity: null,
      exploreGraph: null,
      webSearchResults: [],
      activeTab: 'graph',  // 'graph' | 'search'

      // 实体详情
      entityDetail: null,

      // 操作菜单
      showActionMenu: false,
      actionMenuNode: null,

      // 加载状态
      loading: false,
      loadingText: '加载中...',

      // 页面状态
      currentView: 'graph',  // 'graph' | 'search' | 'detail' | 'route' | 'filter' | 'explore'

      // 搜索显示
      showSearch: false,

      // 安全区域
      navTop: 0,
      safeTop: 0,
      safeBottom: 0
    }
  },

  onLoad(options) {
    // 获取安全区域
    this.getSystemInfo()

    // 如果有传入实体参数，加载实体详情
    if (options.entity) {
      this.handleEntityQuery(options.entity)
    }

    // 加载图谱概览
    this.loadOverview()

    // 加载标签
    this.loadTags()
  },

  onUnload() {
    // 清理
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
    }
  },

  methods: {
    // G6就绪回调（由renderjs在初始化完成后通过callMethod调用）
    onG6Ready() {
      console.log('[Scenic] G6 renderjs ready')
      this.g6Ready = true
      // 如果有数据，通过共享存储发送给renderjs
      if (this.graphData.nodes.length > 0) {
        this.sendDataToG6(this.graphData, null)
      }
    },

    // 接收来自renderjs的图事件（由renderjs通过callMethod调用）
    onGraphEvent(event) {
      console.log('[Scenic] Graph event:', event && event.type)
      if (!event) return
      const { type, payload } = event

      switch (type) {
        case 'nodeClick':
          this.handleNodeClick(payload)
          break
        case 'nodeDblclick':
          this.handleNodeDblclick(payload)
          break
        case 'nodeLongpress':
          this.handleNodeLongpress(payload)
          break
        case 'canvasClick':
          this.selectedNode = null
          break
      }
    },

    // 获取系统信息
    getSystemInfo() {
      const systemInfo = uni.getSystemInfoSync()
      this.navTop = systemInfo.statusBarHeight || 20
      this.safeTop = systemInfo.statusBarHeight || 20
      this.safeBottom = systemInfo.safeAreaInsets && systemInfo.safeAreaInsets.bottom || 20
    },

    // ==========================================
    // 向renderjs发送数据（通过共享存储）
    // ==========================================

    // 存储数据到共享存储（logic→renderjs通信的唯一可靠方式）
    sendDataToG6(data, center) {
      console.log('[Scenic] sendDataToG6:', data.nodes ? data.nodes.length : 0, 'nodes')
      var payload = {
        nodes: data.nodes || [],
        edges: data.edges || [],
        center: center
      }
      // 写入多种存储，确保renderjs至少能读到一种
      try {
        uni.setStorageSync('kg_graph_render', payload)
        console.log('[Scenic] Data stored via uni storage')
      } catch(e) {
        console.warn('[Scenic] uni storage failed:', e)
      }
      try {
        if (typeof localStorage !== 'undefined') {
          localStorage.setItem('kg_graph_render', JSON.stringify(payload))
        }
      } catch(e) {
        console.warn('[Scenic] localStorage failed:', e)
      }
      try {
        if (typeof plus !== 'undefined') {
          plus.storage.setItem('kg_graph_render', JSON.stringify(payload))
        }
      } catch(e) {
        console.warn('[Scenic] plus.storage failed:', e)
      }
    },

    // sendToGraph保留为数据发送的便捷入口（内部调用sendDataToG6）
    sendToGraph(message) {
      console.log('[Scenic] sendToGraph:', message.action, 'g6Ready:', this.g6Ready)
      if (message.action === 'renderGraph') {
        this.sendDataToG6(message.data, message.center)
      } else if (message.action === 'addNodes') {
        // 增量的也可以走存储
        this.sendDataToG6(message.data, null)
      } else if (message.action === 'clearGraph') {
        // 清除指令
        try {
          uni.setStorageSync('kg_graph_render', { nodes: [], edges: [], center: null, clear: true })
        } catch(e) {}
      } else if (message.action === 'focusNode') {
        // 聚焦节点：通过标记数据方式
        this.sendDataToG6({ nodes: [], edges: [] }, message.data && message.data.id ? message.data.id : message.center)
      }
    },

    // ==========================================
    // API 调用
    // ==========================================

    // API请求封装
    request(url, options = {}) {
      return new Promise((resolve, reject) => {
        uni.request({
          url: KG_API_BASE + url,
          ...options,
          success: (res) => {
            if (res.statusCode === 200 && res.data) {
              resolve(res.data)
            } else {
              reject(new Error(res.data && res.data.msg || '请求失败'))
            }
          },
          fail: (err) => {
            reject(err)
          }
        })
      })
    },

    // 接口1: 图谱概览
    async loadOverview() {
      this.loading = true
      this.loadingText = '加载图谱...'

      try {
        const res = await this.request('/overview', {
          method: 'GET',
          data: {
            scenic_id: config.SCENIC_ID
          }
        })

        this.nodeCount = res.node_count || 0
        this.relationCount = res.relation_count || 0
        this.typeDistribution = res.type_distribution || []

        // 转换数据格式给G6
        this.graphData = {
          nodes: (res.nodes || []).map(n => ({
            id: n.id || n.name,
            label: n.name,
            type: n.type || 'Tag',
            name: n.name
          })),
          edges: (res.edges || []).map((e, i) => ({
            id: e.id || `edge-${i}`,
            source: e.source,
            target: e.target,
            label: RELATION_LABELS[e.type] || e.type || '',
            type: e.type
          }))
        }

        // 如果G6已就绪，直接发送渲染
        if (this.g6Ready) {
          this.sendToGraph({
            action: 'renderGraph',
            data: this.graphData
          })
        }
      } catch (err) {
        console.error('[Scenic] loadOverview error:', err)
        // 使用模拟数据
        this.loadMockData()
      } finally {
        this.loading = false
      }
    },

    // 模拟数据（当API不可用时）
    loadMockData() {
      const mockNodes = [
        { id: 'lingshan', label: '灵山胜境', type: 'ScenicSpot' },
        { id: 'dafo', label: '灵山大佛', type: 'ScenicSpot' },
        { id: 'jiulong', label: '九龙灌浴', type: 'Show' },
        { id: 'chanxiu', label: '禅修中心', type: 'ScenicSpot' },
        { id: 'wuzhen', label: '拈花湾', type: 'City' },
        { id: 'shengmiao', label: '梵宫', type: 'Event' },
        { id: 'tang', label: '唐代', type: 'Dynasty' },
        { id: 'xuanzang', label: '玄奘', type: 'Person' },
        { id: 'sushi', label: '素斋', type: 'Food' }
      ]

      const mockEdges = [
        { id: 'e1', source: 'dafo', target: 'lingshan', label: '位于', type: 'LOCATED_IN' },
        { id: 'e2', source: 'jiulong', target: 'lingshan', label: '位于', type: 'LOCATED_IN' },
        { id: 'e3', source: 'dafo', target: 'tang', label: '建于', type: 'BUILT_IN' },
        { id: 'e4', source: 'dafo', target: 'xuanzang', label: '关联人物', type: 'RELATED_TO' },
        { id: 'e5', source: 'lingshan', target: 'wuzhen', label: '附近', type: 'NEAR_WITH' },
        { id: 'e6', source: 'lingshan', target: 'jiulong', label: '关联演出', type: 'HAS_SHOW' },
        { id: 'e7', source: 'lingshan', target: 'shengmiao', label: '位于', type: 'LOCATED_IN' },
        { id: 'e8', source: 'lingshan', target: 'sushi', label: '包含美食', type: 'HAS_FOOD' },
        { id: 'e9', source: 'lingshan', target: 'chanxiu', label: '位于', type: 'LOCATED_IN' }
      ]

      this.graphData = {
        nodes: mockNodes,
        edges: mockEdges
      }
      this.nodeCount = mockNodes.length
      this.relationCount = mockEdges.length

      if (this.g6Ready) {
        this.sendToGraph({
          action: 'renderGraph',
          data: this.graphData
        })
      }
    },

    // 接口2: 实体查询
    async queryEntity(entity, depth = 1) {
      this.loading = true
      this.loadingText = '加载实体...'

      try {
        const res = await this.request('/knowledge-graph', {
          method: 'GET',
          data: {
            entity,
            scenic_id: config.SCENIC_ID,
            depth
          }
        })

        this.entityDetail = res.entity
        this.selectedRelations = res.relations || []
        this.selectedNode = {
          id: res.entity && res.entity.name,
          label: res.entity && res.entity.name,
          type: res.entity && res.entity.type
        }

        // 切换到详情视图
        this.currentView = 'detail'

        // 更新图谱
        if (res.nodes && res.edges) {
          this.sendToGraph({
            action: 'renderGraph',
            data: {
              nodes: res.nodes.map(n => ({
                id: n.id || n.name,
                label: n.name,
                type: n.type
              })),
              edges: res.edges.map((e, i) => ({
                id: e.id || `edge-${i}`,
                source: e.source,
                target: e.target,
                label: RELATION_LABELS[e.type] || '',
                type: e.type
              }))
            },
            center: res.entity && res.entity.name
          })
        }

        return res
      } catch (err) {
        console.error('[Scenic] queryEntity error:', err)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.loading = false
      }
    },

    // 接口3: 节点展开
    async expandNode(entity) {
      try {
        const res = await this.request('/expand', {
          method: 'GET',
          data: {
            entity,
            scenic_id: config.SCENIC_ID
          }
        })

        // 增量添加节点
        if (res.nodes && res.edges) {
          this.sendToGraph({
            action: 'addNodes',
            data: {
              nodes: res.nodes.map(n => ({
                id: n.id || n.name,
                label: n.name,
                type: n.type
              })),
              edges: res.edges.map((e, i) => ({
                id: e.id || `expand-edge-${i}`,
                source: e.source,
                target: e.target,
                label: RELATION_LABELS[e.type] || '',
                type: e.type
              }))
            }
          })
        }

        uni.showToast({
          title: '展开成功',
          icon: 'success'
        })
      } catch (err) {
        console.error('[Scenic] expandNode error:', err)
        uni.showToast({
          title: '展开失败',
          icon: 'none'
        })
      }
    },

    // 接口4: 知识探索
    async exploreKnowledge(entity) {
      this.loading = true
      this.loadingText = '探索中...'
      this.exploreEntity = entity

      try {
        const res = await this.request('/explore', {
          method: 'GET',
          data: {
            entity,
            scenic_id: config.SCENIC_ID
          }
        })

        this.exploreGraph = res.knowledge_graph
        this.webSearchResults = res.web_search_results || []
        this.currentView = 'explore'
        this.activeTab = 'graph'

        // 渲染探索图谱
        if (this.exploreGraph) {
          this.sendToGraph({
            action: 'renderGraph',
            data: {
              nodes: (this.exploreGraph.nodes || []).map(n => ({
                id: n.id || n.name,
                label: n.name,
                type: n.type
              })),
              edges: (this.exploreGraph.edges || []).map((e, i) => ({
                id: e.id || `explore-edge-${i}`,
                source: e.source,
                target: e.target,
                label: RELATION_LABELS[e.type] || '',
                type: e.type
              }))
            },
            center: this.exploreGraph.entity && this.exploreGraph.entity.name
          })
        }
      } catch (err) {
        console.error('[Scenic] exploreKnowledge error:', err)
        uni.showToast({
          title: '探索失败',
          icon: 'none'
        })
      } finally {
        this.loading = false
      }
    },

    // 接口5: 路线推荐
    async getRoute(start) {
      this.loading = true
      this.loadingText = '规划路线...'
      this.routeStart = start

      try {
        const res = await this.request('/route', {
          method: 'GET',
          data: {
            start,
            scenic_id: config.SCENIC_ID
          }
        })

        this.routeList = res.route || []
        this.currentView = 'route'
      } catch (err) {
        console.error('[Scenic] getRoute error:', err)
        // 使用模拟数据
        this.routeList = [
          { name: '九龙灌浴', distance_km: 0.35, recommend: true, poi_id: 'lingshan:jiulong' },
          { name: '梵宫', distance_km: 0.5, recommend: true, poi_id: 'lingshan:fangong' },
          { name: '五印坛城', distance_km: 0.7, recommend: false, poi_id: 'lingshan:wuyin' }
        ]
        this.currentView = 'route'
      } finally {
        this.loading = false
      }
    },

    // 接口6: 标签筛选
    async filterByTag(tag) {
      this.loading = true
      this.loadingText = '筛选中...'
      this.selectedTag = tag

      try {
        const res = await this.request('/filter', {
          method: 'GET',
          data: {
            tag,
            scenic_id: config.SCENIC_ID
          }
        })

        this.filterResults = res.items || []
        this.currentView = 'filter'

        // 更新图谱显示筛选结果
        if (res.nodes && res.edges) {
          this.sendToGraph({
            action: 'renderGraph',
            data: {
              nodes: res.nodes.map(n => ({
                id: n.id || n.name,
                label: n.name,
                type: n.type
              })),
              edges: res.edges.map((e, i) => ({
                id: e.id || `filter-edge-${i}`,
                source: e.source,
                target: e.target,
                label: RELATION_LABELS[e.type] || '',
                type: e.type
              }))
            }
          })
        }
      } catch (err) {
        console.error('[Scenic] filterByTag error:', err)
        // 使用模拟数据
        this.filterResults = [
          { name: '灵山大佛', type: 'ScenicSpot', properties: { hotScore: 4.8 } },
          { name: '梵宫', type: 'Event', properties: { hotScore: 4.5 } }
        ]
        this.currentView = 'filter'
      } finally {
        this.loading = false
      }
    },

    // 接口7: 模糊搜索
    async searchEntity(q) {
      if (!q || q.length < 1) {
        this.searchResults = []
        this.showSearchResult = false
        return
      }

      try {
        const res = await this.request('/search', {
          method: 'GET',
          data: {
            q,
            scenic_id: config.SCENIC_ID,
            limit: 20
          }
        })

        this.searchResults = res.items || []
        this.showSearchResult = true
      } catch (err) {
        console.error('[Scenic] searchEntity error:', err)
        // 本地搜索
        this.localSearch(q)
      }
    },

    // 本地搜索（API不可用时）
    localSearch(q) {
      const keyword = q.toLowerCase()
      this.searchResults = this.graphData.nodes.filter(n => {
        const label = (n.label || '').toLowerCase()
        const name = (n.name || '').toLowerCase()
        return label.includes(keyword) || name.includes(keyword)
      }).slice(0, 20).map(n => ({
        id: n.id,
        name: n.label || n.name,
        type: n.type,
        properties: {}
      }))
      this.showSearchResult = true
    },

    // 加载标签
    async loadTags() {
      try {
        const res = await this.request('/tags', {
          method: 'GET',
          data: { scenic_id: config.SCENIC_ID }
        })
        this.tags = res.tags || []
      } catch (err) {
        // 使用默认标签
        this.tags = ['佛教文化', '历史古迹', '自然风光', '亲子游', '休闲度假', '美食推荐']
      }
    },

    // ==========================================
    // 事件处理
    // ==========================================

    handleNodeClick(payload) {
      this.selectedNode = payload
      this.selectedRelations = []
      this.queryEntity(payload.label)
    },

    handleNodeDblclick(payload) {
      this.expandNode(payload.label)
    },

    handleNodeLongpress(payload) {
      this.actionMenuNode = payload
      this.showActionMenu = true
      uni.vibrateShort()
    },

    handleEntityQuery(entity) {
      this.queryEntity(decodeURIComponent(entity))
    },

    // 搜索输入
    onSearchInput(e) {
      this.searchQuery = e.detail.value
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      this.searchTimer = setTimeout(() => {
        this.searchEntity(this.searchQuery)
      }, 300)
    },

    clearSearch() {
      this.searchQuery = ''
      this.searchResults = []
      this.showSearchResult = false
    },

    toggleSearch() {
      this.showSearch = !this.showSearch
      if (!this.showSearch) {
        this.clearSearch()
      }
    },

    onSearchConfirm() {
      if (this.searchQuery && this.searchResults.length > 0) {
        this.onSearchResultClick(this.searchResults[0])
      }
    },

    onSearchResultClick(item) {
      this.clearSearch()
      this.queryEntity(item.name || item.id)
    },

    onTagClick(tag) {
      if (this.selectedTag === tag) {
        this.selectedTag = null
        this.currentView = 'graph'
        if (this.graphData.nodes.length > 0) {
          this.sendToGraph({
            action: 'renderGraph',
            data: this.graphData
          })
        }
      } else {
        this.filterByTag(tag)
      }
    },

    onActionSelect(action) {
      this.showActionMenu = false
      const node = this.actionMenuNode

      switch (action) {
        case 'detail':
          this.queryEntity(node.label)
          break
        case 'expand':
          this.expandNode(node.label)
          break
        case 'route':
          this.goLiveGuide()
          break
        case 'search':
          this.exploreKnowledge(node.label)
          break
      }

      this.actionMenuNode = null
    },

    onBottomAction(action) {
      if (!this.selectedNode) {
        uni.showToast({ title: '请先选择节点', icon: 'none' })
        return
      }

      switch (action) {
        case 'detail':
          this.queryEntity(this.selectedNode.label)
          break
        case 'expand':
          this.expandNode(this.selectedNode.label)
          break
        case 'route':
          this.goLiveGuide()
          break
        case 'search':
          this.exploreKnowledge(this.selectedNode.label)
          break
      }
    },

    onRouteItemClick(item) {
      if (item.poi_id) {
        this.queryEntity(item.name)
      }
    },

    goBack() {
      this.currentView = 'graph'
      this.selectedNode = null
      this.entityDetail = null

      if (this.graphData.nodes.length > 0) {
        this.sendToGraph({
          action: 'renderGraph',
          data: this.graphData
        })
      }
    },

    goBackPage() {
      // 如果在子视图（筛选/详情/路线/探索）中，先回到图谱首页
      if (this.currentView !== 'graph') {
        this.goBack()
        return
      }
      // 在图谱首页时，才回应用首页
      uni.switchTab({
        url: '/pages/home/index'
      })
    },

    goLiveGuide() {
      uni.navigateTo({
        url: '/pages/live-guide/index',
        fail: function() {
          uni.showToast({ title: '页面跳转失败', icon: 'none' })
        }
      })
    },

    // ==========================================
    // 工具函数
    // ==========================================

    getNodeColor(type) {
      return NODE_COLORS[type] || '#8c8c8c'
    },

    formatDistance(km) {
      if (km < 1) {
        return Math.round(km * 1000) + 'm'
      }
      return km.toFixed(1) + 'km'
    },

    getTypeLabel(type) {
      const labels = {
        ScenicSpot: '景点',
        City: '区域',
        Person: '人物',
        Dynasty: '朝代',
        Event: '事件',
        Show: '演出',
        Food: '美食',
        Hotel: '住宿',
        Tag: '标签'
      }
      return labels[type] || type
    },

    openExternalLink(url) {
      if (!url) return
      uni.setClipboardData({
        data: url,
        success: () => {
          uni.showToast({
            title: '链接已复制',
            icon: 'success'
          })
        }
      })
    }
  }
}
