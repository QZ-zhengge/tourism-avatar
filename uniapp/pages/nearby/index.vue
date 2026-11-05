<template>
  <view class="page">
    <!-- 顶部导航 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
      <view class="nav-back" @tap="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">周边好店</view>
      <view class="nav-placeholder"></view>
    </view>

    <!-- 分类筛选 -->
    <view class="filter-bar">
      <scroll-view scroll-x class="filter-scroll" show-scrollbar="false">
        <view
          v-for="(f, index) in filters"
          :key="index"
          class="filter-item"
          :class="{ active: currentFilter === index }"
          @tap="onFilterChange(index)"
        >
          {{ f.name }}
        </view>
      </scroll-view>
    </view>

    <!-- 列表 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="loading"
      @scrolltolower="loadMore"
    >
      <view class="loading-bar" v-if="loading && filteredShops.length === 0">
        <view class="loading-spinner"></view>
        <text class="loading-text">加载中...</text>
      </view>

      <view class="item-list" v-if="filteredShops.length > 0">
        <view
          class="item-card"
          v-for="(item, idx) in filteredShops"
          :key="item.id || idx"
          @tap="onShopTap(item)"
        >
          <view class="item-cover" :style="{ backgroundColor: getColor(item.title, idx) }">
            <text class="cover-letter">{{ getLetter(item.title) }}</text>
            <view class="cover-tag">{{ item.tag }}</view>
          </view>
          <view class="item-body">
            <view class="item-title">{{ item.title }}</view>
            <view class="item-summary" v-if="item.summary">{{ item.summary.substring(0, 80) }}{{ item.summary.length > 80 ? '...' : '' }}</view>
            <view class="item-footer">
              <text class="item-source" v-if="item.link">查看详情 ›</text>
              <text class="item-source" v-else-if="item.summary">查看简介 ›</text>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="!loading && filteredShops.length === 0">
        <text class="empty-icon">🏪</text>
        <text class="empty-text">暂无周边好店</text>
      </view>

      <view class="no-more" v-if="!loading && filteredShops.length > 0 && noMore">
        <text>— 没有更多了 —</text>
      </view>

      <view class="safe-bottom"></view>
    </scroll-view>
  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<style scoped>
@import './index.css';
</style>
