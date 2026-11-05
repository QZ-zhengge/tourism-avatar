<template>
  <view class="page-root">

    <!-- ========== 主内容区 ========== -->
    <view class="dh-page">

      <!-- 顶部导航栏（inline style 计算，确保所有手机上不遮挡） -->
      <view class="top-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
        <view class="dh-back-btn" @tap="navigateBack">
          <text class="dh-back-icon">←</text>
        </view>
        <text class="page-title">数字人导游</text>
        <view class="nav-right-placeholder"></view>
      </view>

      <!-- ========== 导航目标提示条 ========== -->
      <view v-if="navTarget" class="nav-target-bar">
        <text class="nav-target-icon">&#9673;</text>
        <text class="nav-target-text">正在导航至：{{ navTarget.name }}</text>
        <view class="nav-target-cancel" @tap="clearNavTarget">&times;</view>
      </view>

      <!-- ========== 数字人形象区 ========== -->
      <view class="human-stage">

        <!-- APP-PLUS / H5: 用 video 组件（原生播放器支持 RTMP / HLS） -->
        <!-- #ifdef APP-PLUS || H5 -->
        <video
          v-if="dhStatus === 'ready' && dhVideoUrl"
          id="dhVideoPlayer"
          class="dh-video-player"
          :src="dhVideoUrl"
          :autoplay="true"
          :loop="false"
          :muted="false"
          :controls="false"
          :show-play-btn="false"
          :show-center-play-btn="false"
          :enable-progress-gesture="false"
          object-fit="cover"
          @play="onVideoPlay"
          @error="onVideoError"
          @waiting="onVideoWaiting"
          @loadedmetadata="onVideoLoaded"
        ></video>
        <!-- #endif -->

        <!-- MP-WEIXIN: 用 live-player（小程序直播组件） -->
        <!-- #ifdef MP-WEIXIN -->
        <live-player
          v-if="dhStatus === 'ready' && dhVideoUrl"
          id="dhLivePlayer"
          class="dh-video-player"
          :src="dhVideoUrl"
          :autoplay="true"
          mode="live"
          :muted="false"
          object-fit="cover"
          @statechange="onLiveStateChange"
          @error="onVideoError"
        ></live-player>
        <!-- #endif -->

        <!-- 光球效果（始终显示，ready时在视频上方） -->
        <view class="glow-orb" :class="{ 'connecting': conversationStatus === 'asring' || conversationStatus === 'thinking' || dhStatus === 'starting' }"></view>

        <!-- 启动中/失败 覆盖层 -->
        <view v-if="dhStatus === 'starting'" class="dh-overlay">
          <view class="dh-loading-spinner"></view>
          <text class="dh-overlay-text" v-if="retryCount === 0">数字人启动中...</text>
          <text class="dh-overlay-text" v-else>正在连接 ({{ retryCount }}/{{ maxRetries }})...</text>
        </view>
        <view v-if="dhStatus === 'failed'" class="dh-overlay">
          <text class="dh-overlay-icon">!</text>
          <text class="dh-overlay-text">数字人展示失败</text>
          <view class="dh-retry-btn" @tap="onCameraTap">
            <text>点击重试</text>
          </view>
        </view>
      </view>

      <!-- ========== 状态文字区（仅录音/思考/播放时显示）========== -->
      <view class="status-row" v-if="conversationStatus !== 'idle'">
        <view class="dot-row">
          <view class="wave-dot" :class="{ active: conversationStatus !== 'idle' }"></view>
          <view class="wave-dot delay1" :class="{ active: conversationStatus !== 'idle' }"></view>
          <view class="wave-dot delay2" :class="{ active: conversationStatus !== 'idle' }"></view>
        </view>
      </view>

      <!-- ========== 底部操作栏 ========== -->
      <view class="bottom-bar">
        <view class="action-row">
          <view class="act-btn" :class="{ 'act-btn--active': isRecording }" @touchstart.prevent="onMicPress" @touchend.prevent="onMicRelease" @touchcancel="onMicCancel">
            <up-icon name="mic" size="48" :color="isRecording ? '#fff' : '#555'"></up-icon>
          </view>
          <view class="act-btn">
            <up-icon name="arrow-upward" size="48" color="#555"></up-icon>
          </view>
          <view class="act-btn" @tap="onCameraTap">
            <up-icon name="photo" size="48" color="#555"></up-icon>
          </view>
          <view class="act-btn act-btn--close" @tap="navigateBack">
            <up-icon name="close" size="44" color="#e74c3c"></up-icon>
          </view>
        </view>
      </view>

    </view><!-- /dh-page -->

    <!-- ========== 预订卡片遮罩 - 千问风格 ========== -->
    <view class="booking-overlay" v-if="bookingCardData" @tap="closeBookingCard">
      <view class="booking-card" @tap.stop>
        <view class="booking-close" @tap="closeBookingCard">✕</view>

        <!-- Card header: info only -->
        <view class="bc-header">
          <view class="bc-info">
            <text class="bc-info-title">🏨 {{ bcName || '预订信息' }}</text>
            <view class="bc-info-meta" v-if="bcRating || bcTag">
              <text class="bc-info-rating" v-if="bcRating">⭐ {{ bcRating }}</text>
              <text class="bc-info-tag" v-if="bcTag">{{ bcTag }}</text>
            </view>
            <text class="bc-info-price" v-if="bcPrice">
              {{ bcPrice }}<text class="bc-info-price-unit"> /晚</text>
            </text>
            <text class="bc-info-address" v-if="bcAddress">{{ bcAddress }}</text>
          </view>
        </view>

        <view class="bc-divider"></view>

        <!-- AI hint -->
        <view class="bc-hint" v-if="bookingCardData && bookingCardData.card_data && bookingCardData.card_data.answer">
          <text>{{ bookingCardData.card_data.answer }}</text>
        </view>

        <!-- Editable form -->
        <view class="bc-form">
          <view class="bc-form-item">
            <text class="bc-form-label">酒店名称</text>
            <input class="bc-form-input" :value="bcName" placeholder="请输入酒店名称" @input="e => bcName = e.detail.value" />
          </view>
          <view class="bc-form-item">
            <text class="bc-form-label">入住日期</text>
            <input class="bc-form-input" :value="bcCheckIn" placeholder="如 2026-06-15" @input="e => bcCheckIn = e.detail.value" />
          </view>
          <view class="bc-form-item">
            <text class="bc-form-label">离店日期</text>
            <input class="bc-form-input" :value="bcCheckOut" placeholder="如 2026-06-16" @input="e => bcCheckOut = e.detail.value" />
          </view>
          <view class="bc-form-item">
            <text class="bc-form-label">房型</text>
            <input class="bc-form-input" :value="bcRoomType" placeholder="如 豪华大床房" @input="e => bcRoomType = e.detail.value" />
          </view>
          <view class="bc-form-item">
            <text class="bc-form-label">价格</text>
            <input class="bc-form-input" :value="bcPrice" placeholder="如 ¥388/晚" @input="e => bcPrice = e.detail.value" />
          </view>
          <view class="bc-form-item">
            <text class="bc-form-label">备注</text>
            <input class="bc-form-input" :value="bcNote" placeholder="其他需求（选填）" @input="e => bcNote = e.detail.value" />
          </view>
        </view>

        <view class="bc-actions">
          <view class="bc-act-btn bc-act-confirm" @tap="confirmBooking">✓ 确认添加到行程</view>
          <view class="bc-act-btn bc-act-secondary" @tap="closeBookingCard">取消</view>
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
