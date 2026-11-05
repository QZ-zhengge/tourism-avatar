<template>
  <view class="page">

    <!-- ===== Hero 区域：轮播图 ===== -->
    <view class="hero">
      <!-- 轮播图外层留边框效果 -->
      <view class="swiper-wrap">
        <swiper class="hero-swiper" autoplay circular :interval="3200" :duration="500" :indicator-dots="true" indicator-color="rgba(255,255,255,0.5)" indicator-active-color="#fff">
          <swiper-item v-for="(item, index) in banners" :key="index">
            <image class="banner-image" :src="item" mode="aspectFill"></image>
          </swiper-item>
        </swiper>
      </view>

      <!-- 天气浮层（左上角） -->
      <view class="weather-box card-blur">
        <view class="district">
          <picker mode="selector" :range="scenicSpots" :range-key="'name'" :value="selectedSpotIndex" @change="onSpotChange">
            <view class="picker-row">
              <text class="district-text">{{ scenicSpots[selectedSpotIndex].name }}</text>
              <text class="picker-arrow">▼</text>
            </view>
          </picker>
        </view>
        <view class="weather-line">
          <text class="weather-icon">{{ weatherIcon }}</text>
          <text>{{ weatherText }}</text>
        </view>
      </view>
    </view>

    <!-- ===== 内容区域 ===== -->
    <view class="content-wrap">

      <!-- 探索服务：2行 × 5列 图标网格 -->
      <view class="section-card function-card">
        <view class="section-title">探索服务</view>
        <view class="function-grid">
          <view
            v-for="(item, index) in functions"
            :key="index"
            class="function-item"
            @tap="onFunctionTap(item)"
          >
            <view class="fi-icon-wrap" :style="{ background: item.color }">
              <u-icon :name="item.icon" size="40" :color="item.iconColor || '#4a90d9'"></u-icon>
            </view>
            <view class="fi-title">{{ item.title }}</view>
          </view>
        </view>
      </view>

      <!-- 快捷操作行 -->
      <view class="action-row">
        <view class="action-btn digital" @tap="goDigitalHuman">
          <view class="action-title">数字人</view>
          <view class="action-sub">智慧陪伴导览</view>
        </view>
        <view class="action-btn atlas" @tap="goItinerary">
          <view class="action-title">我的行程</view>
          <view class="action-sub">路线及预订记录</view>
        </view>
      </view>

      <!-- 旅友圈精选 -->
      <view class="section-card posts-card" id="posts-section">
        <view class="section-header">
          <view class="section-title">旅友圈精选</view>
          <view class="more" @tap="goMoments">查看更多 ></view>
        </view>
        <view
          v-for="(item, index) in posts"
          :key="item.id"
          class="post-item"
          @tap="goMoments"
        >
          <image class="post-cover" :src="item.image" mode="aspectFill"></image>
          <view class="post-content">
            <view class="post-title">{{ item.title }}</view>
            <view class="post-excerpt">{{ item.excerpt }}</view>
          </view>
        </view>
      </view>

      <view class="safe-bottom-space"></view>
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
