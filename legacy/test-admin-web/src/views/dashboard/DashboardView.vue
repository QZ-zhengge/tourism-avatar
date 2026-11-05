<template>
      <div class="dashboard-page">
        <div class="db-container">
          <div class="db-header">
            <button class="db-toggle-btn" @click="toggleSidebar" title="打开菜单">☰</button>
            <div class="db-header-title">景区数字人交互数据大屏</div>
            <div class="db-header-sub">SCENIC AREA DIGITAL HUMAN DATA DASHBOARD &nbsp;|&nbsp; {{ currentTime }}</div>
            <div class="db-header-line"></div>
          </div>

          <!-- 大屏浮层侧边栏遮罩 -->
          <div class="sidebar-overlay-backdrop" v-if="sidebarOverlay" @click="toggleSidebar"></div>

          <div class="db-kpi-row">
            <div class="db-kpi-item" v-for="(k, i) in kpiAll" :key="i">
              <div class="db-kpi-label">{{ k.label }}</div>
              <div :style="{color: k.color}">
                <span class="db-kpi-value">{{ k.value }}</span>
                <span class="db-kpi-unit">{{ k.unit }}</span>
              </div>
            </div>
          </div>

          <div class="db-grid">
            <div class="db-panel">
              <div class="db-panel-title">情感趋势分析</div>
              <div class="db-chart" id="chart-sentiment"></div>
            </div>
            <div class="db-panel">
              <div class="db-panel-title">全国游客来源分布</div>
              <div class="db-map-panel" id="chart-map-wrapper">
                <div class="db-map-loading" id="map-loading" v-if="mapLoading">
                  <div class="db-map-loading-spinner"></div>
                  <div class="db-map-loading-text">地图加载中...</div>
                </div>
                <div class="db-map-overlay">
                  <div class="db-map-overlay-title">TOP 5</div>
                  <div class="db-map-overlay-item" v-for="(p, i) in topProvinces" :key="i">
                    <span class="db-map-overlay-rank" :class="{top: i<3}">{{ i+1 }}</span>
                    <span class="db-map-overlay-name">{{ p.name }}</span>
                    <span class="db-map-overlay-val">{{ p.value }}</span>
                  </div>
                </div>
                <div id="chart-map" style="width:100%;height:100%;"></div>
              </div>
            </div>
            <div class="db-panel">
              <div class="db-panel-title">情感分布占比</div>
              <div class="db-chart" id="chart-emotion"></div>
            </div>
          </div>

          <div class="db-grid">
            <div class="db-panel">
              <div class="db-panel-title">热点问题 TOP10</div>
              <div class="db-chart" id="chart-hot-bar"></div>
            </div>
            <div class="db-panel">
              <div class="db-panel-title">游客兴趣雷达</div>
              <div class="db-chart" id="chart-radar"></div>
            </div>
            <div class="db-panel">
              <div class="db-panel-title">访客类型分布</div>
              <div class="db-chart" id="chart-visitor"></div>
            </div>
          </div>

          <div class="db-grid-2">
            <div class="db-panel">
              <div class="db-panel-title">问题分类统计</div>
              <div class="db-chart" id="chart-category"></div>
            </div>
            <div class="db-panel">
              <div class="db-panel-title">高频问题排行榜</div>
              <div style="max-height:220px;overflow-y:auto">
                <table class="db-table">
                  <thead><tr><th>排名</th><th>问题内容</th><th>次数</th><th>趋势</th></tr></thead>
                  <tbody>
                    <tr v-for="(q, i) in hotQuestions" :key="i">
                      <td><span class="db-rank" :class="{top: i<3}">{{ i+1 }}</span></td>
                      <td>{{ q.text }}</td>
                      <td :style="{color:'#06b6d4'}">{{ q.count }}</td>
                      <td :style="{color: q.trend>0?'#10b981':'#ef4444'}">{{ q.trend>0?'▲':'▼'}}{{ Math.abs(q.trend) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
</template>

<script setup>
import { inject } from 'vue'

// 从父组件注入状态
const kpiAll = inject('kpiAll')
const topProvinces = inject('topProvinces')
const hotQuestions = inject('hotQuestions')
const mapLoading = inject('mapLoading')
const sidebarOverlay = inject('sidebarOverlay')
const toggleSidebar = inject('toggleSidebar')
const currentTime = inject('currentTime')
const initCharts = inject('initCharts')
</script>
