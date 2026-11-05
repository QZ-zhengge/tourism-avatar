<template>
  <view class="page">
    <!-- 顶部导航 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
      <view class="nav-back" @tap="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">美食推荐</view>
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

    <!-- 列表 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="loading"
      @scrolltolower="loadMore"
    >
      <view class="loading-bar" v-if="loading && filteredFoods.length === 0">
        <view class="loading-spinner"></view>
        <text class="loading-text">加载中...</text>
      </view>

      <view class="item-list" v-if="filteredFoods.length > 0">
        <view
          class="item-card"
          v-for="(item, idx) in filteredFoods"
          :key="item.id || idx"
          @tap="onFoodTap(item)"
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

      <view class="empty-state" v-if="!loading && filteredFoods.length === 0">
        <text class="empty-icon">🍽</text>
        <text class="empty-text">暂无美食推荐</text>
      </view>

      <view class="no-more" v-if="!loading && filteredFoods.length > 0 && noMore">
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
