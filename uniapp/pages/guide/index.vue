<template>
  <view class="page">
    <!-- 顶部导航 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
      <view class="nav-back" @tap="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">游玩攻略</view>
      <view class="nav-actions">
        <view class="nav-action" @tap="onSearch">
          <text class="search-icon">🔍</text>
        </view>
      </view>
    </view>

    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input-wrap" @tap="onSearch">
        <text class="search-icon-small">⌕</text>
        <text class="search-placeholder">搜索攻略 / 景点 / 美食...</text>
      </view>
    </view>

    <!-- 热门分类标签 -->
    <view class="tag-bar" v-if="tags.length > 0">
      <scroll-view scroll-x class="tag-scroll" show-scrollbar="false">
        <view
          v-for="(tag, idx) in tags"
          :key="idx"
          class="tag-item"
          :class="{ active: selectedTag === tag }"
          @tap="onTagTap(tag)"
        >
          {{ tag }}
        </view>
      </scroll-view>
    </view>

    <!-- 主内容：帖子列表 -->
    <scroll-view
      class="feed-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <!-- 加载中 -->
      <view class="loading-bar" v-if="loading && results.length === 0">
        <view class="loading-spinner"></view>
        <text class="loading-text">加载攻略中...</text>
      </view>

      <!-- 帖子列表 -->
      <view class="feed-list" v-if="results.length > 0">
        <view
          class="post-card"
          v-for="(item, index) in results"
          :key="'post_' + (item.id || '') + '_' + index"
          @tap="onPostTap(item)"
        >
          <view class="post-cover" :style="{ backgroundColor: getColor(item.title, index) }">
            <text class="cover-letter">{{ getLetter(item.title) }}</text>
          </view>
          <view class="post-body">
            <view class="post-title">{{ item.title }}</view>
            <view class="post-excerpt">{{ item.summary || '' }}</view>
            <view class="post-meta">
              <text class="post-source">{{ getSourceLabel(item.source) }}</text>
              <text class="post-link-hint" v-if="item.link">打开链接 ›</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 空状态（加载完成但无数据） -->
      <view class="empty-state" v-if="!loading && results.length === 0">
        <text class="empty-icon">📖</text>
        <text class="empty-text">暂无攻略数据</text>
      </view>

      <!-- 加载更多 -->
      <view class="load-more" v-if="loading && results.length > 0">
        <view class="loading-spinner"></view>
        <text class="loading-text">加载更多...</text>
      </view>

      <!-- 没有更多 -->
      <view class="no-more" v-if="!loading && results.length > 0 && noMore">
        <text>— 没有更多了 —</text>
      </view>

      <!-- 出行清单 -->
      <view class="checklist-section">
        <view class="checklist-header">
          <view class="checklist-title"><u-icon name="list" size="28" color="#333"></u-icon> 出行清单</view>
          <view class="add-btn" @tap="onAddItem">+ 添加</view>
        </view>
        <view class="checklist-items">
          <view
            v-for="(item, index) in checklist"
            :key="index"
            class="checklist-item"
            :class="{ checked: item.checked }"
            @tap="onToggleItem(index)"
          >
            <view class="check-box">
              <text v-if="item.checked" class="check-icon">✓</text>
            </view>
            <text class="item-text">{{ item.text }}</text>
          </view>
        </view>
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
