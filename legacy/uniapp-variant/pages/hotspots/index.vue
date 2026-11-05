<template>
  <view class="page">
    <!-- ========== 列表模式 ========== -->
    <template v-if="!selectedSpot">
      <view class="nav-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
        <view class="nav-back" @tap="goBack">
          <text class="back-icon">←</text>
        </view>
        <view class="nav-title">热门景点</view>
        <view class="nav-placeholder"></view>
      </view>

      <!-- 分类标签 -->
      <scroll-view scroll-x class="tag-scroll" :show-scrollbar="false">
        <view
          v-for="(tag, index) in categories"
          :key="index"
          class="tag-item"
          :class="{ active: currentTag === tag.key }"
          @tap="switchTag(tag.key)"
        >{{ tag.name }}</view>
      </scroll-view>

      <!-- 景点卡片列表 -->
      <scroll-view scroll-y class="content-scroll" refresher-enabled @refresherrefresh="onRefresh" @scrolltolower="loadMore">
        <view v-if="loading" class="loading-wrap">
          <text class="loading-text">加载中...</text>
        </view>
        <view v-else-if="filteredSpots.length === 0" class="empty-wrap">
          <text class="empty-text">暂无热门景点</text>
        </view>
        <view v-else class="spot-list">
          <view
            v-for="(spot, index) in filteredSpots"
            :key="spot.id || index"
            class="spot-card"
            @tap="onSpotTap(spot)"
          >
            <view class="spot-cover" :style="{ backgroundColor: getColor(spot.name, index) }">
              <text class="cover-letter">{{ getLetter(spot.name) }}</text>
            </view>
            <view class="spot-info">
              <view class="spot-header">
                <text class="spot-name">{{ spot.name }}</text>
              </view>
              <view class="spot-tags-line">
                <text v-for="(tag, ti) in (spot.tags || []).slice(0, 3)" :key="ti" class="spot-tag">{{ tag }}</text>
              </view>
              <text class="spot-desc">{{ spot.summary }}</text>
            </view>
          </view>
        </view>
        <!-- 加载更多 -->
        <view v-if="loadingMore" class="loading-wrap">
          <text class="loading-text">加载更多...</text>
        </view>
        <view v-if="noMore && spots.length > 0" class="no-more-wrap">
          <text class="no-more-text">— 已加载全部 —</text>
        </view>
      </scroll-view>
    </template>

    <!-- ========== 详情模式（小红书风格） ========== -->
    <template v-else>
      <view class="detail-page">
        <view class="detail-nav" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
          <view class="nav-back" @tap="closeDetail">
            <text class="back-icon">←</text>
          </view>
          <view class="nav-title">景点详情</view>
          <view class="nav-placeholder"></view>
        </view>
        <scroll-view scroll-y class="detail-scroll">
          <!-- 封面大图 -->
          <view class="detail-cover" :style="{ backgroundColor: getColor(selectedSpot.name, 0) }">
            <text class="cover-letter-big">{{ getLetter(selectedSpot.name) }}</text>
          </view>

          <!-- 用户信息栏 -->
          <view class="detail-author">
            <view class="author-avatar">
              <text class="author-avatar-text">📌</text>
            </view>
            <view class="author-info">
              <text class="author-name">随行推荐</text>
              <text class="author-meta">景点 · 推荐打卡</text>
            </view>
          </view>

          <!-- 标题 + 结构化内容 -->
          <view class="detail-body">
            <text class="detail-name">{{ selectedSpot.name }}</text>
            <view class="detail-tags">
              <text v-for="(t, i) in selectedSpot.tags" :key="i" class="detail-tag">{{ t }}</text>
            </view>

            <!-- 分段展示内容 -->
            <view v-for="(sec, si) in detailSections" :key="si" class="detail-section">
              <text class="section-label" v-if="sec.type === 'location'">📍 位置</text>
              <text class="section-label" v-else-if="sec.type === 'feature'">✨ 特色</text>
              <text class="section-label" v-else-if="sec.type === 'info'">📌 信息</text>
              <text class="section-label" v-else-if="sec.type === 'tip'">💡 提示</text>
              <text class="section-label" v-else-if="sec.type === 'desc'">📝 介绍</text>
              <text class="section-text">{{ sec.text }}</text>
            </view>
            <view v-if="!selectedSpot.summary" class="detail-section">
              <text class="section-text">暂无详细介绍</text>
            </view>
          </view>

          <!-- 互动栏（全部可点击） -->
          <view class="detail-actions">
            <view class="detail-action-item" @tap="toggleLike">
              <text class="action-icon" :class="{ 'action-icon--active': isCurrentSpotLiked }">{{ isCurrentSpotLiked ? '❤️' : '🤍' }}</text>
              <text class="action-count">{{ selectedSpot.likes || 0 }}</text>
            </view>
            <view class="detail-action-item" @tap="openComment">
              <text class="action-icon">💬</text>
              <text class="action-count">{{ selectedSpot.comments || 0 }}</text>
            </view>
            <view class="detail-action-item" @tap="toggleFav">
              <text class="action-icon" :class="{ 'action-icon--active': isCurrentSpotFaved }">{{ isCurrentSpotFaved ? '⭐' : '☆' }}</text>
              <text class="action-count">收藏</text>
            </view>
            <view class="detail-action-item" @tap="shareSpot">
              <text class="action-icon">↗</text>
              <text class="action-count">分享</text>
            </view>
            <view class="detail-action-item" @tap="openLink" v-if="selectedSpot.link">
              <text class="action-icon">🔗</text>
              <text class="action-count">查看链接</text>
            </view>
          </view>

          <!-- 评论输入框 -->
          <view v-if="showCommentInput" class="comment-overlay">
            <view class="comment-box">
              <textarea class="comment-input" v-model="commentedText" placeholder="写下你的评论..." :maxlength="200" auto-height></textarea>
              <view class="comment-actions">
                <text class="comment-cancel" @tap="cancelComment">取消</text>
                <text class="comment-submit" :class="{ disabled: !commentedText.trim() }" @tap="submitComment">发布</text>
              </view>
            </view>
          </view>

          <!-- 底部安全区 -->
          <view class="detail-bottom-space"></view>
        </scroll-view>
      </view>
    </template>
  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<style scoped>
@import './index.css';
</style>
