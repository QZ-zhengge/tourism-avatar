<template>
  <view class="page">
    <!-- 顶部导航 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px' }">
      <view class="nav-content">
        <view class="nav-left">
          <view class="back-btn" @tap="goBackPage">
            <u-icon name="arrow-left" size="20" color="#333"></u-icon>
          </view>
        </view>
        <view class="nav-title">知识图谱</view>
        <view class="nav-right">
          <view class="search-btn" @tap="toggleSearch">
            <u-icon name="search" size="20" color="#333"></u-icon>
          </view>
        </view>
      </view>
    </view>

    <!-- 搜索栏 -->
    <view class="search-bar" v-if="showSearch">
      <view class="search-input-wrap">
        <u-icon name="search" size="18" color="#999" class="search-icon"></u-icon>
        <input
          class="search-input"
          type="text"
          v-model="searchQuery"
          placeholder="搜索景点、人物、美食..."
          confirm-type="search"
          @input="onSearchInput"
          @confirm="onSearchConfirm"
        />
        <view class="search-clear" v-if="searchQuery" @tap="clearSearch">
          <u-icon name="close-circle-fill" size="16" color="#ccc"></u-icon>
        </view>
      </view>
      <view class="search-cancel" @tap="toggleSearch">取消</view>
    </view>

    <!-- 搜索结果 -->
    <view class="search-results" v-if="showSearchResult && searchResults.length > 0">
      <view
        class="search-result-item"
        v-for="item in searchResults"
        :key="item.id"
        @tap="onSearchResultClick(item)"
      >
        <view class="result-color-bar" :style="{ background: getNodeColor(item.type) }"></view>
        <view class="result-info">
          <view class="result-name">{{ item.name }}</view>
          <view class="result-meta">
            <text class="result-type" :style="{ color: getNodeColor(item.type) }">{{ getTypeLabel(item.type) }}</text>
            <text class="result-hot" v-if="item.properties && item.properties.hotScore">
              <u-icon name="star-fill" size="12" color="#FFD700"></u-icon>
              {{ item.properties.hotScore }}
            </text>
          </view>
        </view>
        <u-icon name="arrow-right" size="16" color="#ccc"></u-icon>
      </view>
    </view>

    <!-- 顶部信息栏：统计 + 标签（flex 文档流，避免重叠） -->
    <view class="header-bar" v-if="currentView === 'graph'">
      <view class="stats-card">
        <view class="stat-item">
          <view class="stat-value">{{ nodeCount }}</view>
          <view class="stat-label">节点</view>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <view class="stat-value">{{ relationCount }}</view>
          <view class="stat-label">关系</view>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <view class="stat-value">{{ typeDistribution.length || 6 }}</view>
          <view class="stat-label">类型</view>
        </view>
      </view>

      <view class="tag-cloud" v-if="tags.length > 0">
        <scroll-view class="tag-scroll" scroll-x>
          <view
            class="tag-item"
            :class="{ active: selectedTag === tag }"
            v-for="tag in tags"
            :key="tag"
            @tap="onTagClick(tag)"
          >
            {{ tag }}
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- G6 图谱容器（renderjs渲染） -->
    <view class="graph-container">
      <view id="g6-container" class="g6-canvas"></view>
    </view>

    <!-- 悬浮讲解入口按钮（始终可见） -->
    <view class="live-guide-fab" @tap="goLiveGuide">
      <text class="live-guide-fab-icon">📷</text>
      <text class="live-guide-fab-text">讲解</text>
    </view>

    <!-- 底部信息栏 -->
    <view class="bottom-bar" v-if="selectedNode && currentView === 'graph'">
      <view class="node-info">
        <view class="node-color" :style="{ background: getNodeColor(selectedNode.type) }"></view>
        <view class="node-details">
          <view class="node-name">{{ selectedNode.label }}</view>
          <view class="node-type" :style="{ color: getNodeColor(selectedNode.type) }">
            {{ getTypeLabel(selectedNode.type) }}
          </view>
        </view>
      </view>
      <view class="node-actions">
        <view class="action-btn" @tap="onBottomAction('detail')">
          <u-icon name="info-circle" size="20" color="#4A90D9"></u-icon>
          <text>详情</text>
        </view>
        <view class="action-btn" @tap="onBottomAction('expand')">
          <u-icon name="share" size="20" color="#52c41a"></u-icon>
          <text>展开</text>
        </view>
        <view class="action-btn" @tap="goLiveGuide">
          <u-icon name="camera" size="20" color="#2dba98"></u-icon>
          <text style="color:#2dba98">讲解</text>
        </view>
      </view>
    </view>

    <!-- 操作菜单弹窗 -->
    <view class="action-menu-mask" v-if="showActionMenu" @tap="showActionMenu = false">
      <view class="action-menu" @tap.stop>
        <view class="menu-title">选择操作</view>
        <view class="menu-item" @tap="onActionSelect('detail')">
          <u-icon name="info-circle" size="20" color="#4A90D9"></u-icon>
          <text>查看详情</text>
        </view>
        <view class="menu-item" @tap="onActionSelect('expand')">
          <u-icon name="share" size="20" color="#52c41a"></u-icon>
          <text>展开关系</text>
        </view>
        <view class="menu-item" @tap="goLiveGuide">
          <u-icon name="camera" size="20" color="#2dba98"></u-icon>
          <text>拍照讲解</text>
        </view>
        <view class="menu-item" @tap="onActionSelect('search')">
          <u-icon name="search" size="20" color="#722ed1"></u-icon>
          <text>联网搜索</text>
        </view>
        <view class="menu-cancel" @tap="showActionMenu = false">取消</view>
      </view>
    </view>

    <!-- 详情视图 -->
    <view class="detail-view" v-if="currentView === 'detail' && entityDetail">
      <view class="detail-header" :style="{ background: 'linear-gradient(135deg, ' + getNodeColor(entityDetail.type) + ' 0%, ' + getNodeColor(entityDetail.type) + '99 100%)' }">
        <view class="detail-back" @tap="goBack">
          <u-icon name="arrow-left" size="20" color="#fff"></u-icon>
        </view>
        <view class="detail-title">{{ entityDetail.name }}</view>
        <view class="detail-type-tag">{{ getTypeLabel(entityDetail.type) }}</view>
      </view>

      <view class="detail-content">
        <view class="detail-card" v-if="entityDetail.properties">
          <view class="card-title">基本信息</view>
          <view class="card-item" v-if="entityDetail.properties.hotScore">
            <text class="item-label">热度评分</text>
            <view class="item-value stars">
              <text v-for="i in 5" :key="i" :class="{ active: i <= Math.floor(entityDetail.properties.hotScore) }">★</text>
              <text class="score">{{ entityDetail.properties.hotScore }}</text>
            </view>
          </view>
          <view class="card-item" v-if="entityDetail.properties.playTime">
            <text class="item-label">建议游玩</text>
            <text class="item-value">{{ entityDetail.properties.playTime }}分钟</text>
          </view>
          <view class="card-item" v-if="entityDetail.properties.intro">
            <text class="item-label">简介</text>
            <text class="item-value intro">{{ entityDetail.properties.intro }}</text>
          </view>
        </view>

        <view class="detail-card" v-if="selectedRelations.length > 0">
          <view class="card-title">关联关系</view>
          <view
            class="relation-item"
            v-for="(rel, index) in selectedRelations"
            :key="index"
            @tap="queryEntity(rel.target.name)"
          >
            <view class="rel-line" v-if="index > 0"></view>
            <view class="rel-badge" :style="{ background: getNodeColor(rel.target.type) }">
              {{ rel.label }}
            </view>
            <view class="rel-target">
              <view class="rel-name">{{ rel.target.name }}</view>
              <view class="rel-type" :style="{ color: getNodeColor(rel.target.type) }">
                {{ getTypeLabel(rel.target.type) }}
              </view>
            </view>
            <view class="rel-expand" v-if="rel.expandable" @tap.stop="expandNode(rel.target.name)">
              <u-icon name="plus" size="16" color="#4A90D9"></u-icon>
            </view>
          </view>
        </view>

        <view class="detail-actions">
          <view class="detail-action-btn" @tap="onBottomAction('expand')">
            <u-icon name="share" size="20" color="#52c41a"></u-icon>
            <text>展开关系</text>
          </view>
          <view class="detail-action-btn" @tap="goLiveGuide">
            <u-icon name="camera" size="20" color="#2dba98"></u-icon>
            <text>拍照讲解</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 标签筛选视图 -->
    <view class="filter-view" v-if="currentView === 'filter'">
      <view class="filter-header">
        <view class="filter-back" @tap="goBack">
          <u-icon name="arrow-left" size="20" color="#333"></u-icon>
        </view>
        <view class="filter-title">标签筛选</view>
        <view class="filter-tag">{{ selectedTag }}</view>
      </view>

      <view class="filter-content">
        <view class="filter-grid">
          <view
            class="filter-card"
            v-for="item in filterResults"
            :key="item.name"
            @tap="queryEntity(item.name)"
          >
            <view class="card-color-bar" :style="{ background: getNodeColor(item.type) }"></view>
            <view class="card-body">
              <view class="card-name">{{ item.name }}</view>
              <view class="card-meta">
                <text class="card-type" :style="{ color: getNodeColor(item.type) }">
                  {{ getTypeLabel(item.type) }}
                </text>
                <text class="card-hot" v-if="item.properties && item.properties.hotScore">
                  ★ {{ item.properties.hotScore }}
                </text>
              </view>
            </view>
          </view>
        </view>

        <view class="filter-empty" v-if="filterResults.length === 0">
          <text>暂无相关结果</text>
        </view>
      </view>
    </view>

    <!-- 知识探索视图 -->
    <view class="explore-view" v-if="currentView === 'explore'">
      <view class="explore-header">
        <view class="explore-back" @tap="goBack">
          <u-icon name="arrow-left" size="20" color="#333"></u-icon>
        </view>
        <view class="explore-title">知识探索</view>
        <view class="explore-entity">{{ exploreEntity }}</view>
      </view>

      <view class="explore-tabs">
        <view
          class="tab-item"
          :class="{ active: activeTab === 'graph' }"
          @tap="activeTab = 'graph'"
        >
          知识图谱
        </view>
        <view
          class="tab-item"
          :class="{ active: activeTab === 'search' }"
          @tap="activeTab = 'search'"
        >
          网络搜索
          <text class="tab-count" v-if="webSearchResults.length > 0">{{ webSearchResults.length }}</text>
        </view>
      </view>

      <view class="web-results" v-if="activeTab === 'search'">
        <view
          class="web-result-item"
          v-for="(item, index) in webSearchResults"
          :key="index"
          @tap="openExternalLink(item.link)"
        >
          <view class="web-title">{{ item.title }}</view>
          <view class="web-summary">{{ item.summary }}</view>
          <view class="web-link">{{ item.link }}</view>
        </view>

        <view class="web-empty" v-if="webSearchResults.length === 0">
          <text>暂无搜索结果</text>
        </view>
      </view>
    </view>

    <!-- 加载状态 -->
    <view class="loading-mask" v-if="loading">
      <view class="loading-content">
        <view class="loading-spinner"></view>
        <text class="loading-text">{{ loadingText }}</text>
      </view>
    </view>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<script module="g6Renderer" lang="renderjs">
// renderjs 视图层脚本 - 运行在WebView视图层，可操作DOM
// 负责加载和渲染G6图，并与逻辑层通信

var G6_CDN = 'https://unpkg.com/@antv/g6@4.8.24/dist/g6.min.js'
var graph = null
var G6 = null
var hasRenderedOnce = false

// 动态加载G6
function loadG6() {
  return new Promise(function(resolve, reject) {
    if (typeof window.G6 !== 'undefined') {
      G6 = window.G6
      resolve(G6)
      return
    }
    var script = document.createElement('script')
    script.src = G6_CDN
    script.onload = function() {
      G6 = window.G6
      resolve(G6)
    }
    script.onerror = function(e) {
      console.error('[G6 Render] Load G6 failed:', e)
      reject(e)
    }
    document.head.appendChild(script)
  })
}

// 节点类型配色（与index.js一致）
var NODE_COLORS = {
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
var RELATION_LABELS = {
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

// 获取节点颜色
function getNodeColor(type) {
  return NODE_COLORS[type] || '#8c8c8c'
}

export default {
  mounted: function() {
    // 初始加载G6
    var self = this
    loadG6().then(function() {
      self.initG6()
      console.log('[G6 Render] Graph initialized')

      // 确保canvas接收触摸事件
      self.enableCanvasTouch()

      // 延迟修复容器尺寸（flex布局可能还未计算完成）
      setTimeout(function() { self.resizeGraph() }, 300)
      setTimeout(function() { self.resizeGraph() }, 800)

      // 通知逻辑层G6已就绪
      self.$ownerInstance.callMethod('onG6Ready')

      // 启动数据轮询
      self.pollForData()
    }).catch(function(e) {
      console.error('[G6 Render] Init failed:', e)
    })
  },
  methods: {
    // 确保canvas能接收触摸事件
    enableCanvasTouch: function() {
      if (!graph) return
      try {
        var canvas = graph.get('canvas')
        if (canvas) {
          canvas.set('draggable', true)
          var el = canvas.get('el')
          if (el) {
            el.style.touchAction = 'none'
            el.style.webkitTouchCallout = 'none'
            el.style.webkitUserSelect = 'none'
            el.style.pointerEvents = 'auto'
            el.setAttribute('touch-action', 'none')
          }
        }
        // 同时给容器设置触摸属性
        var container = document.getElementById('g6-container')
        if (container) {
          container.style.touchAction = 'none'
          container.style.pointerEvents = 'auto'
          var cvs = container.querySelector('canvas')
          if (cvs) {
            cvs.style.touchAction = 'none'
            cvs.style.pointerEvents = 'auto'
          }
        }
      } catch(e) {
        console.log('[G6 Render] Canvas touch setup:', e)
      }
    },
    // 轮询获取逻辑层的数据（通过共享存储）
    pollForData: function() {
      var self = this
      setInterval(function() {
        var data = null
        // 尝试从多种存储读取
        try { data = uni.getStorageSync('kg_graph_render') } catch(e) {}
        if (!data && typeof localStorage !== 'undefined') {
          try { 
            var raw = localStorage.getItem('kg_graph_render')
            if (raw) data = JSON.parse(raw)
          } catch(e) {}
        }
        if (!data && typeof plus !== 'undefined') {
          try { data = plus.storage.getItem('kg_graph_render'); if(data) data = JSON.parse(data) } catch(e) {}
        }

        if (data && data.nodes && data.nodes.length > 0) {
          // 清除已读取数据
          try { uni.removeStorageSync('kg_graph_render') } catch(e) {}
          try { localStorage.removeItem('kg_graph_render') } catch(e) {}
          try { plus.storage.removeItem('kg_graph_render') } catch(e) {}
          console.log('[G6 Render] Polled data, rendering', data.nodes.length, 'nodes')
          self.renderGraph(data, data.center)
        }
      }, 300) // 每300ms轮询一次
    },

    // 记录上次尺寸，避免频繁 changeSize 导致渲染异常
    lastGraphSize: { w: 0, h: 0 },

    // 自适应容器尺寸（解决移动端初始化尺寸为0的问题）
    resizeGraph: function() {
      if (!graph) return
      var container = document.getElementById('g6-container')
      if (!container) return
      var w = container.clientWidth || container.offsetWidth || 600
      var h = container.clientHeight || container.offsetHeight || 400
      if (w > 0 && h > 0) {
        // 只在尺寸变化超过 10px 时才 changeSize，避免频繁 resize 导致渲染残留
        var last = this.lastGraphSize
        if (Math.abs(last.w - w) > 10 || Math.abs(last.h - h) > 10) {
          graph.changeSize(w, h)
          this.lastGraphSize = { w: w, h: h }
          // 只在首次或容器尺寸明显变化时 fitView，不打断用户拖拽
          if (!hasRenderedOnce) {
            graph.fitView(40)
          }
          console.log('[G6 Render] Resized to:', w, 'x', h)
        }
      }
    },

    // 初始化G6图
    initG6: function() {
      var container = document.getElementById('g6-container')
      if (!container) {
        console.error('[G6 Render] Container not found')
        return
      }

      // 获取容器尺寸
      var width = container.clientWidth || 600
      var height = container.clientHeight || 400

      // ========== 注册自定义节点形状 ==========
      // 菱形（City用）
      if (!G6.Util.findNodeById || true) {
        try {
          G6.registerNode('diamond', {
            draw: function(cfg, group) {
              var s = cfg.size || 50
              var w = typeof s === 'number' ? s : (s[0] || 50)
              var h = typeof s === 'number' ? w * 1.2 : (s[1] || w * 1.2)
              var color = (cfg.style && cfg.style.stroke) || '#409EFF'
              var fill = (cfg.style && cfg.style.fill) || color + '33'
              var key = group.addShape('path', {
                attrs: {
                  path: [
                    ['M', 0, -h / 2],
                    ['L', w / 2, 0],
                    ['L', 0, h / 2],
                    ['L', -w / 2, 0],
                    ['Z']
                  ],
                  fill: fill,
                  stroke: color,
                  lineWidth: (cfg.style && cfg.style.lineWidth) || 2,
                  shadowColor: (cfg.style && cfg.style.shadowColor) || color + '66',
                  shadowBlur: (cfg.style && cfg.style.shadowBlur) || 12
                }
              })
              return key
            }
          }, 'single-node')

          // 三角形（Show用）
          G6.registerNode('triangle', {
            draw: function(cfg, group) {
              var s = cfg.size || 42
              var sz = typeof s === 'number' ? s : (s[0] || 42)
              var h = sz * 0.866
              var color = (cfg.style && cfg.style.stroke) || '#eb2f96'
              var fill = (cfg.style && cfg.style.fill) || color + '33'
              var key = group.addShape('path', {
                attrs: {
                  path: [
                    ['M', 0, -h / 2],
                    ['L', sz / 2, h / 2],
                    ['L', -sz / 2, h / 2],
                    ['Z']
                  ],
                  fill: fill,
                  stroke: color,
                  lineWidth: (cfg.style && cfg.style.lineWidth) || 2,
                  shadowColor: (cfg.style && cfg.style.shadowColor) || color + '66',
                  shadowBlur: (cfg.style && cfg.style.shadowBlur) || 12
                }
              })
              return key
            }
          }, 'single-node')

          // 圆角矩形（Tag用）
          G6.registerNode('rounded-rect', {
            draw: function(cfg, group) {
              var s = cfg.size || 32
              var sz = typeof s === 'number' ? s : s[0]
              var w = sz * 1.5
              var h = sz * 0.6
              var color = (cfg.style && cfg.style.stroke) || '#8c8c8c'
              var fill = (cfg.style && cfg.style.fill) || color + '33'
              var key = group.addShape('rect', {
                attrs: {
                  x: -w / 2, y: -h / 2, width: w, height: h, radius: 6,
                  fill: fill,
                  stroke: color,
                  lineWidth: (cfg.style && cfg.style.lineWidth) || 2,
                  shadowColor: (cfg.style && cfg.style.shadowColor) || color + '66',
                  shadowBlur: (cfg.style && cfg.style.shadowBlur) || 12
                }
              })
              return key
            }
          }, 'single-node')
        } catch(e) {
          console.warn('[G6 Render] Register shapes warning:', e)
        }
      }

      graph = new G6.Graph({
        container: 'g6-container',
        width: width,
        height: height,
        fitView: true,
        fitViewPadding: [40, 20],
        animate: false,
        minZoom: 0.2,
        maxZoom: 5,
        defaultNode: {
          type: 'circle',
          size: 50,
          style: {
            fill: '#1a1a3e',
            stroke: '#409EFF',
            lineWidth: 2,
            shadowColor: 'rgba(64, 158, 255, 0.3)',
            shadowBlur: 10
          },
          labelCfg: {
            style: {
              fill: '#fff',
              fontSize: 12,
              shadowColor: 'rgba(0,0,0,0.5)',
              shadowBlur: 4
            }
          }
        },
        defaultEdge: {
          type: 'quadratic',
          style: {
            stroke: 'rgba(255,255,255,0.25)',
            lineWidth: 1.5,
            endArrow: {
              path: G6.Arrow.triangle(6, 8, 0),
              fill: 'rgba(255,255,255,0.25)'
            }
          },
          labelCfg: {
            style: {
              fill: 'rgba(255,255,255,0.5)',
              fontSize: 10,
              background: {
                fill: 'rgba(10,14,39,0.8)',
                padding: [2, 6]
              }
            }
          }
        },
        modes: {
          default: [
            'drag-canvas',
            'zoom-canvas'
          ]
        },
        layout: {
          type: 'force',
          preventOverlap: true,
          nodeStrength: -300,
          edgeStrength: 0.05,
          linkDistance: 150,
          gravity: 3,
          alpha: 0.1,
          alphaDecay: 0.05,
          alphaMin: 0.01,
          collideStrength: 0.8,
          nodeSpacing: function(d) { return d.size ? d.size + 20 : 50 }
        }
      })

      // 节点点击事件
      graph.on('node:click', function(evt) {
        var model = evt.item.getModel()
        self.$ownerInstance.callMethod('onGraphEvent', {
          type: 'nodeClick',
          payload: {
            id: model.id,
            label: model.label,
            type: model.type || 'default'
          }
        })
      })

      // 双击展开
      graph.on('node:dblclick', function(evt) {
        var model = evt.item.getModel()
        self.$ownerInstance.callMethod('onGraphEvent', {
          type: 'nodeDblclick',
          payload: {
            id: model.id,
            label: model.label,
            type: model.type || 'default'
          }
        })
      })

      // 长按菜单
      graph.on('node:contextmenu', function(evt) {
        var model = evt.item.getModel()
        self.$ownerInstance.callMethod('onGraphEvent', {
          type: 'nodeLongpress',
          payload: {
            id: model.id,
            label: model.label,
            type: model.type || 'default'
          }
        })
      })

      // 点击空白区域
      graph.on('canvas:click', function() {
        self.$ownerInstance.callMethod('onGraphEvent', {
          type: 'canvasClick'
        })
      })

      // 拖拽/缩放结束后强制刷新，修复移动端标签消失问题
      graph.on('canvas:dragend', function() {
        // 延迟 150ms 等 G6 内部渲染完成后再 refresh
        setTimeout(function() {
          if (graph) {
            graph.refresh()
            console.log('[G6 Render] Refreshed after drag/zoom')
          }
        }, 150)
      })

      console.log('[G6 Render] Graph initialized')
    },

    // 根据节点类型获取形状
    getNodeShape: function(type) {
      var shapes = {
        ScenicSpot: 'circle',
        City: 'diamond',
        Person: 'circle',
        Dynasty: 'rect',
        Event: 'ellipse',
        Show: 'triangle',
        Food: 'circle',
        Hotel: 'circle',
        Tag: 'rounded-rect'
      }
      return shapes[type] || 'circle'
    },

    // 根据关系类型获取边样式
    getEdgeStyle: function(type) {
      var styles = {
        LOCATED_IN: { stroke: '#409EFF', lineWidth: 2, lineDash: null },
        NEAR_WITH: { stroke: '#52c41a', lineWidth: 1.5, lineDash: [5, 5] },
        HAS_FOOD: { stroke: '#FF6B6B', lineWidth: 2, lineDash: null },
        HAS_ACCOMMODATION: { stroke: '#2db7f5', lineWidth: 1.5, lineDash: null },
        BELONG_TO_STYLE: { stroke: '#8c8c8c', lineWidth: 1, lineDash: [3, 3] },
        RELATED_TO: { stroke: '#52c41a', lineWidth: 2, lineDash: null },
        BUILT_IN: { stroke: '#faad14', lineWidth: 2, lineDash: null },
        HAS_EVENT: { stroke: '#722ed1', lineWidth: 2, lineDash: null },
        HAS_SHOW: { stroke: '#eb2f96', lineWidth: 2, lineDash: null }
      }
      return styles[type] || { stroke: 'rgba(255,255,255,0.25)', lineWidth: 1.5, lineDash: null }
    },

    // 获取节点大小（统一缩小，防止重叠）
    getNodeSize: function(type) {
      var sizes = {
        ScenicSpot: 42,
        City: 48,
        Person: 34,
        Dynasty: 34,
        Event: 36,
        Show: 36,
        Food: 32,
        Hotel: 32,
        Tag: 28
      }
      return sizes[type] || 36
    },

    // 渲染图谱数据
    renderGraph: function(data, center) {
      if (!graph || !data) return
      var self = this
      console.log('[G6 Render] renderGraph:', data.nodes ? data.nodes.length : 0, 'nodes')

      // 处理节点样式（按文档规范：形状/大小/颜色）
      var nodes = (data.nodes || []).map(function(n) {
        var color = getNodeColor(n.type)
        var shape = self.getNodeShape(n.type)
        var size = self.getNodeSize(n.type)
        var nodeConfig = {
          id: n.id,
          label: n.label || n.name,
          type: shape,
          size: size,
          style: {
            fill: color + '33',
            stroke: color,
            lineWidth: 2,
            shadowColor: color + '66',
            shadowBlur: 12
          },
          labelCfg: {
            style: {
              fill: '#fff',
              fontSize: shape === 'rounded-rect' ? 10 : 11
            }
          }
        }
        return nodeConfig
      })

      // 处理边样式（按文档规范：实线/虚线/颜色）
      var edges = (data.edges || []).map(function(e) {
        var edgeStyle = self.getEdgeStyle(e.type)
        return {
          id: e.id || 'e-' + Math.random().toString(36).substr(2, 8),
          source: e.source,
          target: e.target,
          label: e.label || '',
          type: e.type,
          style: {
            stroke: edgeStyle.stroke,
            lineWidth: edgeStyle.lineWidth,
            lineDash: edgeStyle.lineDash,
            endArrow: {
              path: G6.Arrow.triangle(6, 8, 0),
              fill: edgeStyle.stroke
            }
          }
        }
      })

      // 渲染前先自适应容器尺寸
      self.resizeGraph()

      graph.changeData({
        nodes: nodes,
        edges: edges
      })

      // 只在首次渲染时 fitView，避免打断用户的拖拽/缩放操作
      if (!hasRenderedOnce) {
        graph.fitView(40)
        hasRenderedOnce = true
      } else if (center) {
        // 后续渲染如有中心节点则聚焦，但不改变整体缩放
        graph.focusItem(center, true, { duration: 500 })
      }

      // 确保触摸事件可用
      self.enableCanvasTouch()
    },

    // 增量添加节点
    addNodes: function(data) {
      if (!graph || !data) return
      var self = this
      console.log('[G6 Render] addNodes:', data.nodes ? data.nodes.length : 0, 'nodes')

      var currentData = graph.save()
      var existingIds = {}

      ;(currentData.nodes || []).forEach(function(n) {
        existingIds[n.id] = true
      })

      var newNodes = (data.nodes || []).filter(function(n) {
        return !existingIds[n.id]
      }).map(function(n) {
        var color = getNodeColor(n.type)
        var shape = self.getNodeShape(n.type)
        var size = self.getNodeSize(n.type)
        return {
          id: n.id,
          label: n.label || n.name,
          type: shape,
          size: size,
          style: {
            fill: color + '33',
            stroke: color,
            lineWidth: 2,
            shadowColor: color + '66',
            shadowBlur: 12
          }
        }
      })

      var newEdges = (data.edges || []).filter(function(e) {
        return !existingIds[e.source + e.target + (e.label || '')]
      })

      if (newNodes.length > 0 || newEdges.length > 0) {
        graph.addItems('node', newNodes)
        graph.addItems('edge', newEdges)
      }
    },

    // 聚焦节点
    focusNode: function(id) {
      if (!graph || !id) return
      graph.focusItem(id, true, { duration: 500 })
    },

    // 清除图谱
    clearGraph: function() {
      if (!graph) return
      graph.clear()
    },

    // 自适应容器尺寸
    resize: function() {
      if (!graph) return
      var container = document.getElementById('g6-container')
      if (container) {
        graph.changeSize(container.clientWidth, container.clientHeight)
      }
    }
  }
}
</script>

<style scoped>
@import './index.css';
</style>
