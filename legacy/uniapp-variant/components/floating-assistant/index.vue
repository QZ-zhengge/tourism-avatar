<template>
  <view class="fa-root">
    <!-- 悬浮按钮 -->
    <view
      class="fa-float-btn"
      :class="{ recording: isRecording, speaking: isSpeaking }"
      :style="'left:' + floatX + 'px;top:' + floatY + 'px;'"
      @touchstart="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
    >
      <text class="fa-btn-icon">{{ isSpeaking ? '🔊' : '🤖' }}</text>
      <!-- 录音波纹 -->
      <view v-if="isRecording" class="fa-ring-anim">
        <view class="fa-ring"></view>
        <view class="fa-ring" style="animation-delay:0.5s;"></view>
        <view class="fa-ring" style="animation-delay:1.0s;"></view>
      </view>
    </view>

    <!-- 对话气泡 -->
    <view v-if="bubbleVisible" class="fa-bubble-wrap" :style="bubblePositionStyle">
      <!-- 气泡头部 -->
      <view class="fa-bubble-header">
        <view class="fa-bubble-avatar">导</view>
        <text class="fa-bubble-title">AI 导游</text>
        <view class="fa-bubble-close" @tap.stop="dismissBubble">
          <text class="fa-close-icon">×</text>
        </view>
      </view>
      <!-- 气泡内容 -->
      <view class="fa-bubble-body">
        <text class="fa-bubble-text" v-if="!isTyping">{{ bubbleDisplayText }}</text>
        <text class="fa-bubble-text fa-typing" v-else>{{ bubbleDisplayText }}<text class="fa-cursor">|</text></text>
      </view>
      <!-- 语音波形 + 播放控制 -->
      <view class="fa-bubble-footer" v-if="bubbleDisplayText && !isTyping">
        <view class="fa-wave-row">
          <view
            v-for="(bar, idx) in waveBars"
            :key="idx"
            class="fa-wave-bar"
            :class="isSpeaking ? 'active' : ''"
            :style="{ height: (isSpeaking ? bar : 3) + 'rpx' }"
          ></view>
        </view>
        <view class="fa-replay-btn" @tap.stop="replaySpeech">
          <text class="fa-replay-icon">{{ isSpeaking ? '⏸' : '▶' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<style>
.fa-root { position: relative; }

/* ===== 悬浮按钮 ===== */
.fa-float-btn {
  position: fixed; z-index: 99999; width: 100rpx; height: 100rpx; border-radius: 50%;
  background: linear-gradient(135deg, #2dba98, #1a9a7a);
  box-shadow: 0 6rpx 28rpx rgba(45,186,152,0.45);
  display: flex; align-items: center; justify-content: center; overflow: hidden;
  border: 3rpx solid rgba(255,255,255,0.8);
  transition: box-shadow 0.3s;
}
.fa-float-btn.recording {
  box-shadow: 0 0 0 10rpx rgba(41,128,185,0.25), 0 6rpx 28rpx rgba(45,186,152,0.45);
}
.fa-float-btn.speaking {
  box-shadow: 0 0 0 10rpx rgba(46,204,113,0.3), 0 6rpx 28rpx rgba(45,186,152,0.45);
  animation: fa-pulse 1.5s ease-in-out infinite;
}
@keyframes fa-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}
.fa-btn-icon { font-size: 40rpx; color: #fff; line-height: 1; }

/* 录音波纹 */
.fa-ring-anim { position: absolute; inset: -10rpx; pointer-events: none; }
.fa-ring {
  position: absolute; inset: 0; border-radius: 50%;
  border: 4rpx solid rgba(41,128,185,0.5);
  animation: fa-ring-expand 1.5s ease-out infinite;
}
@keyframes fa-ring-expand {
  0% { transform: scale(1); opacity: 0.7; }
  100% { transform: scale(1.8); opacity: 0; }
}

/* ===== 气泡 ===== */
.fa-bubble-wrap {
  position: fixed; z-index: 99998;
  width: 520rpx;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 8rpx 40rpx rgba(0,0,0,0.15);
  overflow: hidden;
  animation: fa-bubble-in 0.25s ease;
}
@keyframes fa-bubble-in {
  from { opacity: 0; transform: translateY(10rpx) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.fa-bubble-header {
  display: flex; align-items: center; gap: 12rpx;
  padding: 16rpx 20rpx;
  background: linear-gradient(135deg, #2dba98, #1a9a7a);
}
.fa-bubble-avatar {
  width: 44rpx; height: 44rpx; border-radius: 50%;
  background: rgba(255,255,255,0.25); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 22rpx; font-weight: 600;
}
.fa-bubble-title {
  flex: 1; font-size: 24rpx; color: #fff; font-weight: 500;
}
.fa-bubble-close {
  width: 40rpx; height: 40rpx; display: flex; align-items: center; justify-content: center;
}
.fa-close-icon { font-size: 32rpx; color: rgba(255,255,255,0.8); line-height: 1; }

.fa-bubble-body {
  padding: 18rpx 20rpx;
  min-height: 60rpx;
  max-height: 300rpx;
  overflow-y: auto;
}
.fa-bubble-text {
  font-size: 26rpx; color: #333; line-height: 1.7;
  word-break: break-all;
}
.fa-typing { }
.fa-cursor {
  color: #2dba98;
  animation: fa-blink 0.8s step-end infinite;
  font-weight: 700;
}
@keyframes fa-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ===== 语音波形区域 ===== */
.fa-bubble-footer {
  display: flex; align-items: center; gap: 12rpx;
  padding: 12rpx 20rpx 16rpx;
  border-top: 1rpx solid #f0f0f0;
}
.fa-wave-row {
  flex: 1; display: flex; align-items: center; gap: 4rpx; height: 44rpx;
}
.fa-wave-bar {
  width: 5rpx; border-radius: 3rpx;
  background: rgba(45,186,152,0.2);
  transition: height 0.12s ease;
}
.fa-wave-bar.active {
  background: #2dba98;
}
.fa-replay-btn {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  background: linear-gradient(135deg, #2dba98, #1a9a7a);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2rpx 8rpx rgba(45,186,152,0.3);
}
.fa-replay-btn:active { opacity: 0.8; }
.fa-replay-icon { font-size: 24rpx; color: #fff; }
</style>
