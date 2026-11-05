<template>
  <view class="map-page">

    <!-- ========== 地图区域 ========== -->
    <view class="map-area" :class="{ 'map-area--compact': showSearchPanel || showRouteOptions, 'map-area--full': isNavigating }">
      <map
        id="mainMap"
        class="the-map"
        :latitude="mapCenter.latitude"
        :longitude="mapCenter.longitude"
        :scale="mapScale"
        :markers="markers"
        :polyline="activePolyline"
        :show-location="true"
        :show-compass="true"
        :include-points="includedPoints"
        @markertap="onMarkerTap"
        @callouttap="onCalloutTap"
        @regionchange="onRegionChange"
        @error="onMapError"
      ></map>

      <!-- 顶部栏 -->
      <view class="map-header">
        <view class="h-left">
          <view class="h-back-btn" @tap="goBack">
            <u-icon name="arrow-left" size="32" color="#333"></u-icon>
          </view>
          <view class="city-tag" @tap="onLocationSelector">
            <text class="city-name">{{ district }}</text>
            <u-icon name="arrow-down" size="20" color="#333"></u-icon>
          </view>
        </view>
        <view class="header-actions">
          <view class="h-btn" @tap="backToLocation">
            <u-icon name="map" size="30" color="#555"></u-icon>
          </view>
        </view>
      </view>

      <!-- 导航模式覆盖层 -->
      <view v-if="isNavigating" class="nav-overlay">
        <view class="nav-top-info">
          <view class="nav-top-left">
            <text class="nav-dist">{{ currentDistance >= 1000 ? (currentDistance / 1000).toFixed(1) : currentDistance }}<text class="nav-dist-unit">{{ currentDistance >= 1000 ? '公里' : '米' }}</text></text>
            <text class="nav-eta">{{ formatDur(estimatedTime) }}</text>
          </view>
          <!-- 导航模式切换（导航中也可切换） -->
          <view class="nav-mode-switch">
            <view class="nm-btn" :class="{ 'nm-btn--active': navMode === 'walking' }" @tap="switchNavMode('walking')">
              <text class="nm-icon">🚶</text>
              <text class="nm-label" :class="{ 'nm-label--active': navMode === 'walking' }">步行</text>
            </view>
            <view class="nm-btn" :class="{ 'nm-btn--active': navMode === 'driving' }" @tap="switchNavMode('driving')">
              <text class="nm-icon">🚗</text>
              <text class="nm-label" :class="{ 'nm-label--active': navMode === 'driving' }">驾车</text>
            </view>
            <view class="nm-btn" :class="{ 'nm-btn--active': navMode === 'cycling' }" @tap="switchNavMode('cycling')">
              <text class="nm-icon">🚴</text>
              <text class="nm-label" :class="{ 'nm-label--active': navMode === 'cycling' }">骑行</text>
            </view>
          </view>
        </view>
        <view class="nav-step-bar">
          <u-icon name="navigation" size="28" color="#2dba98"></u-icon>
          <text class="nav-step-text">{{ navInstruction }}</text>
        </view>
        <!-- 进度条 -->
        <view class="nav-progress-wrap">
          <view class="nav-progress-bar">
            <view class="nav-progress-fill" :style="{ width: (navProgress * 100) + '%' }"></view>
          </view>
          <view class="nav-progress-labels">
            <text class="nav-progress-label">已走 {{ completedDistance >= 1000 ? (completedDistance/1000).toFixed(1) + '公里' : completedDistance + '米' }}</text>
            <text class="nav-progress-label">剩 {{ currentDistance >= 1000 ? (currentDistance/1000).toFixed(1) + '公里' : currentDistance + '米' }}</text>
          </view>
        </view>
        <view class="nav-bottom-bar">
          <view class="nav-bottom-btn nav-bottom-btn--exit" @tap="cancelNavigation">
            <u-icon name="close" size="28" color="#fff"></u-icon>
            <text class="nav-btn-label nav-btn-label--white">退出导航</text>
          </view>
          <view class="nav-bottom-btn" @tap="backToLocation">
            <u-icon name="map" size="28" color="#555"></u-icon>
            <text class="nav-btn-label">我的位置</text>
          </view>
          <view class="nav-bottom-btn" @tap="viewFullRoute">
            <u-icon name="grid" size="28" color="#555"></u-icon>
            <text class="nav-btn-label">路线全览</text>
          </view>
        </view>
      </view>

      <!-- 右侧垂直进度条（导航时显示） -->
      <view v-if="isNavigating" class="nav-progress-vert">
        <view class="nav-progress-vert-track">
          <view class="nav-progress-vert-fill" :style="{ height: (navProgress * 100) + '%' }"></view>
        </view>
        <text class="nav-progress-vert-label">{{ Math.round(navProgress * 100) }}%</text>
      </view>

      <!-- 地图上浮动"去这里"按钮 -->
      <view v-if="selectedPoi && !isNavigating" class="map-float-route" @tap="startNavigation(selectedPoi, navMode)">
        <u-icon name="navigation" size="32" color="#fff"></u-icon>
        <text class="mfr-text">去这里</text>
      </view>
    </view>

    <!-- ========== 工具 + 天气固定条（始终可见，在所有模式之上）========== -->
    <view v-if="!isNavigating" class="tools-weather-strip">

      <!-- 搜索栏 -->
      <view class="bs-search-wrap">
        <view v-if="showSearchPanel || showRouteOptions || selectedPoi" class="bs-search-back" @tap="backToDefault">
          <u-icon name="arrow-left" size="28" color="#333"></u-icon>
        </view>
        <view class="bs-search-box">
          <u-icon name="search" size="24" color="#999"></u-icon>
          <input class="bs-search-input" placeholder="搜索地点、景点、餐饮" placeholder-class="bs-search-placeholder" :value="keyword" @input="onSearchInput" @confirm="onSearchConfirm" @focus="onSearchFocus" />
        </view>
        <view v-if="keyword" class="bs-clear" @tap="clearKeyword">
          <u-icon name="close" size="24" color="#ccc"></u-icon>
        </view>
      </view>

      <!-- 工具网格 -->
      <view class="tool-grid">
        <view v-for="item in quickTools" :key="item.key" class="tool-item" @tap="onQuickToolTap(item.key)">
          <view class="tool-icon" :style="{ background: item.bg }">
            <u-icon :name="item.icon" size="32" color="#fff"></u-icon>
          </view>
          <text class="tool-label">{{ item.name }}</text>
        </view>
      </view>

      <!-- 天气卡片 - 全宽展示全部API数据 -->
      <view v-if="weatherLoaded" class="map-weather-card">
        <view class="mwc-header">
          <text class="mwc-title">☁ 今日{{ weatherCity }}天气</text>
          <text v-if="weatherWind" class="mwc-title-tag">{{ weatherWind }}</text>
        </view>
        <view class="mwc-summary-row">
          <text class="mwc-current-icon">{{ weatherCurrentIcon }}</text>
          <view class="mwc-summary-text">
            <text class="mwc-summary">{{ weatherSummary }}</text>
            <view class="mwc-summary-meta">
              <text v-if="weatherHumidity">💧 {{ weatherHumidity }}</text>
              <text v-if="weatherWindPower">🌬 {{ weatherWindPower }}</text>
            </view>
          </view>
        </view>
        <scroll-view class="mwc-hourly" scroll-x>
          <view v-for="(h, i) in weatherHourly" :key="i" class="mwc-hour-item">
            <text class="mwc-hour-time">{{ h.time }}</text>
            <text class="mwc-hour-icon">{{ h.icon }}</text>
            <text class="mwc-hour-temp">{{ h.temp }}</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- ========== 底部面板（搜索详情/路线等） ========== -->
    <view class="bottom-sheet bottom-sheet--expanded" v-if="showSearchPanel || selectedPoi || showRouteOptions">
      <view class="bs-scrollable">

        <!-- 出行方式选择 -->
        <view v-if="showRouteOptions && selectedPoi" class="route-mode-bar">
          <view class="route-mode-btn" :class="{ 'route-mode-btn--active': navMode === 'driving' }" @tap="previewRoute('driving')">
            <u-icon name="car" size="32" :color="navMode === 'driving' ? '#2dba98' : '#999'"></u-icon>
            <text class="route-mode-label" :class="{ 'route-mode-label--active': navMode === 'driving' }">驾车</text>
            <text class="route-mode-time" :class="{ 'route-mode-time--active': navMode === 'driving' }">{{ routeEstimates.driving || '--' }}</text>
          </view>
          <view class="route-mode-btn" :class="{ 'route-mode-btn--active': navMode === 'walking' }" @tap="previewRoute('walking')">
            <u-icon name="minus-circle" size="32" :color="navMode === 'walking' ? '#2dba98' : '#999'"></u-icon>
            <text class="route-mode-label" :class="{ 'route-mode-label--active': navMode === 'walking' }">步行</text>
            <text class="route-mode-time" :class="{ 'route-mode-time--active': navMode === 'walking' }">{{ routeEstimates.walking || '--' }}</text>
          </view>
          <view class="route-mode-btn" :class="{ 'route-mode-btn--active': navMode === 'cycling' }" @tap="previewRoute('cycling')">
            <u-icon name="car" size="32" :color="navMode === 'cycling' ? '#2dba98' : '#999'"></u-icon>
            <text class="route-mode-label" :class="{ 'route-mode-label--active': navMode === 'cycling' }">骑行</text>
            <text class="route-mode-time" :class="{ 'route-mode-time--active': navMode === 'cycling' }">{{ routeEstimates.cycling || '--' }}</text>
          </view>
        </view>

        <!-- 默认：热门标签 + POI列表 -->
        <view v-if="!showSearchPanel && !selectedPoi && !showRouteOptions">
          <view class="hot-tags">
            <text class="tags-title">附近热门</text>
            <view class="tags-list">
              <view v-for="tag in hotTags" :key="tag" class="tag-item" @tap="searchByTag(tag)">
                <u-icon name="map" size="20" color="#ff6b6b"></u-icon>
                <text>{{ tag }}</text>
              </view>
            </view>
          </view>
          <view class="poi-list">
            <view v-for="poi in filteredPois" :key="poi.id" class="poi-item" @tap="openPoiPanel(poi)">
              <view class="poi-icon" :style="{ background: poiTypeConfig(poi.type).color + '18', color: poiTypeConfig(poi.type).color }">
                <u-icon :name="poiTypeConfig(poi.type).icon" size="28" :color="poiTypeConfig(poi.type).color"></u-icon>
              </view>
              <view class="poi-info">
                <text class="poi-name">{{ poi.name }}</text>
                <text class="poi-desc">{{ poi.description }}</text>
              </view>
              <view v-if="poi.distance" class="poi-dist">
                <text>{{ formatDist(poi.distance) }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 搜索结果 -->
        <scroll-view v-if="showSearchPanel" class="search-scroll" scroll-y>
          <view v-for="result in searchResults" :key="result.id" class="result-item" @tap="selectSearchResult(result)">
            <view class="result-icon" :style="{ background: poiTypeConfig(result.type).color + '18', color: poiTypeConfig(result.type).color }">
              <u-icon :name="poiTypeConfig(result.type).icon" size="28" :color="poiTypeConfig(result.type).color"></u-icon>
            </view>
            <view class="result-info">
              <text class="result-name">{{ result.name }}</text>
              <text class="result-desc">{{ result.description }}</text>
            </view>
            <view v-if="result.distance !== null" class="result-dist">
              <text>{{ formatDist(result.distance) }}</text>
            </view>
          </view>
          <view v-if="showSearchPanel && searchResults.length === 0 && hasSearched" class="result-empty">
            <text>未找到相关地点</text>
          </view>
        </scroll-view>

        <!-- POI详情 -->
        <view v-if="selectedPoi && !showRouteOptions" class="poi-detail-card">
          <text class="pd-title">{{ selectedPoi.name }}</text>
          <text class="pd-addr">{{ selectedPoi.description }}</text>
          <view class="pd-route-btn" @tap="showRouteForPoi(selectedPoi)">
            <u-icon name="navigation" size="28" color="#fff"></u-icon>
            <text class="pd-route-text">查看路线</text>
          </view>
        </view>

        <!-- 开始导航 -->
        <view v-if="showRouteOptions && selectedPoi && !isNavigating" class="start-nav-bar">
          <view class="start-nav-btn" @tap="startNavigation(selectedPoi, navMode)">
            <u-icon name="navigation" size="28" color="#fff"></u-icon>
            <text class="start-nav-text">开始导航</text>
          </view>
        </view>

      </view>
    </view>

  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<style scoped>
@import './index.css';
</style>
