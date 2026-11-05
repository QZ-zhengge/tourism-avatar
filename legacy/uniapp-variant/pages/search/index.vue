<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px' }">
      <view class="nav-inner">
        <view class="nav-back" @tap="goBack">
          <up-icon name="arrow-left" size="20" color="#333"></up-icon>
        </view>
        <view class="search-input-wrap">
          <text class="search-icon">⌕</text>
          <input
            class="search-input"
            v-model="query"
            :focus="autoFocus"
            placeholder="搜索景点 / 攻略 / 美食..."
            placeholder-class="placeholder"
            confirm-type="search"
            @confirm="doSearch"
            @input="onInput"
          />
          <text class="clear-btn" v-if="query.length > 0" @tap="clearQuery">✕</text>
        </view>
        <view class="nav-search-btn" @tap="doSearch">搜索</view>
      </view>
    </view>

    <!-- 搜索结果 -->
    <scroll-view
      class="result-list"
      scroll-y
      :scroll-into-view="scrollInto"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 搜索提示 -->
      <view class="search-hint" v-if="!searched">
        <view class="hint-icon">⌕</view>
        <view class="hint-text">搜索景点、美食、攻略信息</view>
        <view class="hint-tags">
          <view
            class="hint-tag"
            v-for="(tag, idx) in hotTags"
            :key="idx"
            @tap="quickSearch(tag)"
          >
            {{ tag }}
          </view>
        </view>
      </view>

      <!-- 加载中 -->
      <view class="loading-bar" v-if="loading">
        <view class="loading-spinner"></view>
        <text class="loading-text">正在搜索...</text>
      </view>

      <!-- 搜索结果列表 -->
      <view class="result-items" v-if="searched && !loading">
        <view
          class="post-card"
          v-for="(item, index) in results"
          :key="item.id || index"
          @tap="openResult(item)"
        >
          <view class="post-cover" :style="{ backgroundColor: getColor(item.title, index) }">
            <text class="cover-letter">{{ getLetter(item.title) }}</text>
          </view>
          <view class="post-body">
            <view class="post-title">{{ item.title }}</view>
            <view class="post-excerpt">{{ item.summary || item.snippet || '' }}</view>
            <view class="post-meta">
              <text class="post-source">{{ getSourceLabel(item.source) }}</text>
              <text class="post-time" v-if="item.date">{{ item.date }}</text>
            </view>
          </view>
        </view>

        <!-- 空结果 -->
        <view class="empty-state" v-if="results.length === 0">
          <text class="empty-icon">📭</text>
          <text class="empty-text">未找到相关结果</text>
          <text class="empty-hint">试试换个关键词搜索</text>
        </view>
      </view>

      <!-- 没有更多 -->
      <view class="no-more" v-if="searched && !loading && results.length > 0 && noMore">
        <text>— 没有更多结果 —</text>
      </view>

      <!-- 底部安全区 -->
      <view class="safe-bottom"></view>
    </scroll-view>
  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<style>
@import './index.css'
</style>
