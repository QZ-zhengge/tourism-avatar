<template>
  <view class="page">
    <!-- 顶部导航 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
      <view class="nav-back" @tap="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">拍照打卡</view>
      <view class="nav-actions">
        <view class="nav-action" @tap="onMyCheckins">
          <text class="my-icon">📷</text>
        </view>
      </view>
    </view>

    <!-- 滚动内容区 -->
    <scroll-view class="scroll-body" scroll-y>

      <!-- 打卡指南 -->
      <view class="tips-card">
        <view class="tips-header">
          <view class="tips-icon"><u-icon name="camera" size="32" color="#6c5ce7"></u-icon></view>
          <text class="tips-title">打卡攻略</text>
        </view>
        <view class="tips-list">
          <view class="tip-item">
            <text class="tip-num">1</text>
            <text class="tip-text">选择热门景点，拍照留念</text>
          </view>
          <view class="tip-item">
            <text class="tip-num">2</text>
            <text class="tip-text">发布到旅友圈，获赞赢好礼</text>
          </view>
          <view class="tip-item">
            <text class="tip-num">3</text>
            <text class="tip-text">集齐印章，解锁专属奖励</text>
          </view>
        </view>
      </view>

      <!-- 热门打卡点 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title"><u-icon name="star" size="28" color="#ff6b6b"></u-icon> 热门打卡点</view>
        </view>
        <view v-if="loading" class="loading-more">
          <text>加载中...</text>
        </view>
        <view v-else-if="hotspots.length === 0" class="empty-state">
          <u-icon name="camera" size="80" color="#ccc"></u-icon>
          <text class="empty-text">暂无打卡点</text>
        </view>
        <view v-else class="spot-grid">
          <view
            v-for="(spot, index) in hotspots"
            :key="index"
            class="spot-card"
            @tap="onSpotTap(spot)"
          >
            <view class="spot-rank" :class="{ top3: index < 3 }">{{ index + 1 }}</view>
            <view class="spot-cover" :style="{ backgroundColor: getColor(spot.name, index) }">
              <text class="cover-letter">{{ getLetter(spot.name) }}</text>
            </view>
            <view class="spot-info">
              <view class="spot-name">{{ spot.name }}</view>
              <view class="spot-location">{{ spot.location }}</view>
              <view class="spot-stats">
                <text class="stat-item">📷 {{ spot.count }}次</text>
                <text class="stat-item">⭐ {{ spot.rating }}</text>
              </view>
            </view>
            <view class="spot-checkin" @tap.stop="onCheckin(spot)">
              <text>打卡</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 推荐拍照点 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title">✨ 推荐拍照点</view>
          <view class="more-btn" @tap="loadMore">更多 ></view>
        </view>
        <scroll-view scroll-x class="photo-scroll">
          <view
            v-for="(photo, index) in photoSpots"
            :key="index"
            class="photo-card"
            @tap="onCheckin(photo)"
          >
            <view class="photo-cover" :style="{ backgroundColor: getColor(photo.name, index) }">
              <text class="cover-letter-sm">{{ getLetter(photo.name) }}</text>
            </view>
            <view class="photo-mask">
              <view class="photo-name">{{ photo.name }}</view>
              <view class="photo-tips">{{ photo.tip || '点击打卡' }}</view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 打卡成就 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title">🏆 打卡成就</view>
        </view>
        <view class="achievement-list">
          <view
            v-for="(ach, index) in achievements"
            :key="index"
            class="achievement-item"
            :class="{ unlocked: ach.unlocked }"
          >
            <view class="ach-icon">{{ ach.unlocked ? '🔓' : '🔒' }}</view>
            <view class="ach-info">
              <view class="ach-name">{{ ach.name }}</view>
              <view class="ach-desc">{{ ach.desc }}</view>
              <view class="ach-progress">
                <view class="progress-bar">
                  <view class="progress-fill" :style="{ width: (ach.progress || 0) + '%' }"></view>
                </view>
                <text class="progress-text">{{ ach.current }}/{{ ach.target }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="safe-bottom"></view>
    </scroll-view>

    <!-- ========== 打卡弹窗 ========== -->
    <view class="modal-mask" v-if="showCheckinModal" @tap="cancelCheckin">
      <view class="checkin-modal" @tap.stop>
        <!-- 弹窗头部 -->
        <view class="modal-header">
          <text class="modal-title">📍 打卡 · {{ currentSpot && currentSpot.name }}</text>
          <view class="modal-close" @tap="cancelCheckin">✕</view>
        </view>

        <!-- 照片选择区 -->
        <view class="photo-picker">
          <!-- 已选照片预览 -->
          <view v-if="checkinPhoto" class="photo-preview-wrap">
            <image class="photo-preview" :src="checkinPhoto" mode="aspectFill" @tap="previewPhoto(checkinPhoto)"></image>
            <view class="photo-remove" @tap="removePhoto">✕</view>
          </view>

          <!-- 未选照片时显示选择按钮 -->
          <view v-else class="photo-btns">
            <view class="photo-btn" @tap="takePhoto">
              <text class="photo-btn-icon">📷</text>
              <text class="photo-btn-text">拍照</text>
            </view>
            <view class="photo-btn" @tap="chooseFromAlbum">
              <text class="photo-btn-icon">🖼</text>
              <text class="photo-btn-text">从相册选</text>
            </view>
          </view>
        </view>

        <!-- 备注输入 -->
        <view class="note-area">
          <textarea
            class="note-input"
            :value="checkinNote"
            placeholder="写下你的打卡感想（选填）..."
            placeholder-class="note-placeholder"
            maxlength="100"
            @input="e => checkinNote = e.detail.value"
          ></textarea>
          <text class="note-count">{{ checkinNote.length }}/100</text>
        </view>

        <!-- 确认按钮 -->
        <view class="modal-actions">
          <view class="modal-cancel" @tap="cancelCheckin">取消</view>
          <view class="modal-confirm" @tap="confirmCheckin">
            <text>✓ 确认打卡完毕</text>
          </view>
        </view>
      </view>
    </view>

    <!-- ========== 打卡记录弹窗 ========== -->
    <view class="modal-mask" v-if="showRecordsModal" @tap="closeRecordsModal">
      <view class="records-modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">我的打卡记录（{{ checkinRecords.length }}）</text>
          <view class="modal-close" @tap="closeRecordsModal">✕</view>
        </view>

        <scroll-view class="records-scroll" scroll-y>
          <view v-if="checkinRecords.length === 0" class="empty-records">
            <text class="empty-records-icon">📷</text>
            <text class="empty-records-text">还没有打卡记录</text>
            <text class="empty-records-hint">去景点拍照打卡吧~</text>
          </view>
          <view
            v-for="(rec, idx) in checkinRecords"
            :key="rec.id"
            class="record-item"
          >
            <!-- 照片 -->
            <view class="rec-photo-wrap" @tap="rec.photo && previewPhoto(rec.photo)">
              <image v-if="rec.photo" class="rec-photo" :src="rec.photo" mode="aspectFill"></image>
              <view v-else class="rec-photo-empty" :style="{ backgroundColor: getColor(rec.spotName, idx) }">
                <text class="rec-photo-letter">{{ getLetter(rec.spotName) }}</text>
              </view>
            </view>
            <!-- 信息 -->
            <view class="rec-info">
              <text class="rec-name">{{ rec.spotName }}</text>
              <text class="rec-time">{{ rec.date }} {{ rec.time }}</text>
              <text v-if="rec.note" class="rec-note">{{ rec.note }}</text>
            </view>
            <!-- 删除 -->
            <view class="rec-delete" @tap="deleteCheckin(rec)">🗑</view>
          </view>
        </scroll-view>
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
