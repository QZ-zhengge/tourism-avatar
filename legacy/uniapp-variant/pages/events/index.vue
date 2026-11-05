<template>
  <view class="page">
    <!-- 顶部导航 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
      <view class="nav-back" @tap="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">活动演出</view>
      <view class="nav-placeholder"></view>
    </view>

    <!-- 分类标签 -->
    <view class="category-tabs">
      <scroll-view scroll-x class="tab-scroll" show-scrollbar="false">
        <view
          v-for="(cat, index) in categories"
          :key="index"
          class="tab-item"
          :class="{ active: currentCat === index }"
          @tap="switchTab(index)"
        >
          {{ cat.name }}
        </view>
      </scroll-view>
    </view>

    <!-- 活动列表 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="loading"
      @scrolltolower="loadMore"
    >
      <!-- 加载中 -->
      <view class="loading-bar" v-if="loading && filteredEvents.length === 0">
        <view class="loading-spinner"></view>
        <text class="loading-text">加载中...</text>
      </view>

      <!-- 活动列表 -->
      <view class="event-list" v-if="filteredEvents.length > 0">
        <view
          class="event-card"
          v-for="(event, idx) in filteredEvents"
          :key="event.id || idx"
          @tap="onEventTap(event)"
        >
          <!-- 纯色封面 + 首字母 -->
          <view class="event-cover" :style="{ backgroundColor: getColor(event.title, idx) }">
            <text class="cover-letter">{{ getLetter(event.title) }}</text>
            <view class="cover-tag">{{ event.tag }}</view>
          </view>

          <view class="event-body">
            <view class="event-title">{{ event.title }}</view>
            <view class="event-summary" v-if="event.summary">{{ event.summary.substring(0, 80) }}{{ event.summary.length > 80 ? '...' : '' }}</view>
            <view class="event-footer">
              <text class="event-source" v-if="event.link">查看详情 ›</text>
              <text class="event-source" v-else-if="event.summary">查看简介 ›</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && filteredEvents.length === 0">
        <text class="empty-icon">🎭</text>
        <text class="empty-text">暂无活动信息</text>
      </view>

      <!-- 没有更多 -->
      <view class="no-more" v-if="!loading && filteredEvents.length > 0 && noMore">
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
