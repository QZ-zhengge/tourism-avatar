<template>
  <view class="moments-page" :style="{ paddingTop: statusBarHeight + 'px', '--safe-bottom': safeBottom + 'px' }" @tap="onPageTap">
    <!-- 自定义头部 -->
    <view class="moments-header" @tap.stop="noop">
      <view class="header-left" @tap="openComposer">
        <text class="plus-icon">＋</text>
      </view>
      <view class="header-center">
        <text class="header-title">旅友圈</text>
        <text class="header-subtitle">当前景点所有游客都能看到</text>
      </view>
      <view class="header-right-group">
        <view class="header-draft" @tap="openDraftBox">
          <text class="draft-icon">📁</text>
          <view v-if="drafts.length > 0" class="draft-badge">{{ drafts.length > 9 ? '9+' : drafts.length }}</view>
        </view>
        <view class="header-right" @tap="toggleSpotFilter">
          <text class="filter-label">{{ activeSpotOnly ? '当前景点' : '全部动态' }}</text>
        </view>
      </view>
    </view>

    <scroll-view class="moments-scroll" scroll-y show-scrollbar="false">
      <view class="hero-banner">
        <view class="hero-cover hero-cover--gradient"></view>
        <view class="hero-mask"></view>
        <view class="hero-content">
          <text class="hero-title">景点旅友圈</text>
          <text class="hero-desc">{{ activeSpotOnly ? currentSpotName + ' · 只看当前景点' : '查看景区所有旅友动态' }}</text>
        </view>
      </view>

      <view class="feed-shell">
        <view v-for="post in displayFeedList" :key="post.id" class="feed-card">
          <view class="feed-top">
            <view class="avatar">{{ post.avatarText }}</view>
            <view class="feed-main">
              <view class="feed-user-row">
                <text class="feed-name">{{ post.name }}</text>
                <text class="feed-time">{{ post.time }}</text>
              </view>

              <text class="feed-spot">{{ post.spot }}</text>
              <text class="feed-text" @tap="openPostDetail(post.id)">{{ post.content }}</text>

              <view
                v-if="post.images && post.images.length"
                class="image-grid"
                :class="{ 'image-grid--single': post.images.length === 1 }"
              >
                <view
                  v-for="(imgObj, imgIdx) in post.images"
                  :key="imgIdx"
                  class="feed-image-placeholder"
                  :style="{ backgroundColor: imgObj.bgColor }"
                  @tap="openPostDetail(post.id)"
                >
                  <text class="fipl-icon" :style="{ color: imgObj.textColor }">{{ imgObj.label }}</text>
                </view>
              </view>

              <view class="action-row">
                <view class="action-pill" @tap.stop="toggleLike(post.id)">
                  <text class="action-icon">{{ post.liked ? '♥' : '♡' }}</text>
                  <text class="action-text">赞 {{ post.likes }}</text>
                </view>

                <view class="action-pill" @tap.stop="openCommentBar(post.id)">
                  <text class="action-icon">◔</text>
                  <text class="action-text">评论</text>
                </view>

                <view v-if="post.isMine" class="action-pill action-pill--danger" @tap.stop="deletePost(post.id)">
                  <text class="action-icon">✕</text>
                  <text class="action-text">删除</text>
                </view>
              </view>

              <view v-if="post.likesText || (post.comments && post.comments.length)" class="interaction-box">
                <view v-if="post.likesText" class="likes-box">
                  <text class="likes-label">♥</text>
                  <text class="likes-text">{{ post.likesText }}</text>
                </view>

                <view v-for="(comment, cIdx) in post.comments" :key="cIdx" class="comment-item">
                  <view class="comment-avatar" :style="{ backgroundColor: comment.avatarColor }">
                    <text class="comment-avatar-text">{{ comment.avatarText }}</text>
                  </view>
                  <view class="comment-body">
                    <text class="comment-name">{{ comment.user }}</text>
                    <text class="comment-content">{{ comment.text }}</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view class="feed-bottom-space" :class="{ 'feed-bottom-space--commenting': showCommentBar }"></view>
      </view>
    </scroll-view>

    <!-- 发布弹窗 -->
    <view class="composer-mask" v-if="showComposer" @tap="closeComposer"></view>
    <view class="composer-sheet" :class="{ 'composer-sheet--show': showComposer }" @tap.stop="noop">
      <view class="composer-header">
        <text class="composer-cancel" @tap="closeComposer">取消</text>
        <text class="composer-title">发表动态</text>
        <text class="composer-publish" :class="{ 'composer-publish--active': draftText }" @tap="publishPost">发布</text>
      </view>

      <textarea
        class="composer-textarea"
        maxlength="300"
        placeholder="分享一下你今天在景点里的所见所感..."
        :value="draftText"
        @input="onDraftInput"
      ></textarea>

      <view v-if="draftImages && draftImages.length" class="selected-images">
        <view v-for="(draftImg, draftIndex) in draftImages" :key="draftIndex" class="selected-image-wrap">
          <image class="selected-image" :src="draftImg" mode="aspectFill"></image>
          <view class="selected-image-delete" @tap="removeDraftImage(draftIndex)">×</view>
        </view>
      </view>

      <view class="upload-row">
        <view class="upload-button" @tap="chooseImages">
          <text class="upload-plus">＋</text>
          <text class="upload-text">上传照片</text>
        </view>
        <text class="upload-tip">最多选择 9 张</text>
      </view>
    </view>

    <!-- 评论栏 -->
    <view class="comment-mask" v-if="showCommentBar" @tap="closeCommentBar"></view>
    <view
      v-if="showCommentBar"
      class="comment-dock"
      @tap.stop="noop"
    >
      <input
        class="comment-input"
        maxlength="120"
        placeholder="写评论..."
        :value="commentText"
        @input="onCommentInput"
        confirm-type="send"
        @confirm="submitComment"
        :focus="commentFocus"
        :adjust-position="true"
        hold-keyboard
      />
      <view class="comment-send" :class="{ 'comment-send--active': commentText }" @tap="submitComment">发送</view>
    </view>
    <!-- 草稿箱弹层 -->
    <view class="draft-mask" v-if="showDraftBox" @tap="closeDraftBox"></view>
    <view class="draft-sheet" :class="{ 'draft-sheet--show': showDraftBox }" @tap.stop="noop">
      <view class="draft-header">
        <text class="draft-title">草稿箱</text>
        <text class="draft-close" @tap="closeDraftBox">×</text>
      </view>
      <scroll-view class="draft-list" scroll-y v-if="drafts.length > 0">
        <view v-for="draft in drafts" :key="draft.id" class="draft-item">
          <view class="draft-content" @tap="loadDraft(draft)">
            <view class="draft-text">{{ draft.text || '（仅图片）' }}</view>
            <view class="draft-meta">
              <text class="draft-time">{{ draft.time }}</text>
              <text class="draft-images-count" v-if="draft.images && draft.images.length">📷 {{ draft.images.length }}</text>
            </view>
          </view>
          <view class="draft-delete" @tap="deleteDraft(draft.id)">删除</view>
        </view>
      </scroll-view>
      <view class="draft-empty" v-else>
        <text class="draft-empty-text">暂无草稿</text>
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