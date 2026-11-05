<template>
  <view class="page">
    <!-- 通用导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
      <view class="nav-back" @tap="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">实时讲解</view>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- ========== 阶段1：相机 ========== -->
    <view class="camera-stage" v-if="stage === 'camera'">
      <view class="camera-preview-area">
        <camera
          class="camera-preview"
          :device-position="cameraDevice"
          :flash="flashMode"
          @error="onCameraError"
          ref="cameraRef"
        >
          <!-- 顶部控制 -->
          <view class="camera-top-bar">
            <view class="camera-flash-label" @tap="toggleFlash">
              {{ flashMode === 'off' ? '⚡ 关' : '⚡ 开' }}
            </view>
            <view class="camera-top-btn" @tap="switchCamera">
              <text class="top-btn-text">🔄</text>
            </view>
          </view>

          <!-- 底部文字提示 -->
          <view class="camera-hint">
            <text class="camera-hint-text">对准景点或物品拍摄</text>
          </view>
        </camera>
      </view>

      <!-- 底部操作按钮 -->
      <view class="camera-bottom-bar">
        <view class="camera-bottom-btn" @tap="chooseFromAlbum">
          <view class="album-btn">
            <text class="album-btn-icon">🖼</text>
          </view>
          <text class="camera-bottom-btn-text">相册</text>
        </view>

        <view class="shutter-btn" @tap="takePhoto">
          <view class="shutter-btn-inner"></view>
        </view>

        <view class="camera-bottom-btn" style="visibility:hidden">
          <view class="album-btn"></view>
          <text class="camera-bottom-btn-text"></text>
        </view>
      </view>
    </view>

    <!-- ========== 阶段2：裁剪 ========== -->
    <view class="crop-stage" v-if="stage === 'crop'">
      <view class="crop-image-container">
        <image
          class="crop-image"
          :src="cropImageSrc"
          mode="aspectFit"
          :style="{ left: imgLeft + 'px', top: imgTop + 'px', width: imgWidth + 'px', height: imgHeight + 'px' }"
        ></image>
      </view>

      <!-- 裁剪框 -->
      <view
        class="crop-box"
        :style="{
          left: cropBoxLeft + 'px',
          top: cropBoxTop + 'px',
          width: cropBoxWidth + 'px',
          height: cropBoxHeight + 'px'
        }"
        @touchstart="onCropTouchStart"
        @touchmove="onCropTouchMove"
        @touchend="onCropTouchEnd"
      >
        <!-- 四角手柄 -->
        <view class="crop-handle crop-handle-tl"></view>
        <view class="crop-handle crop-handle-tr"></view>
        <view class="crop-handle crop-handle-bl"></view>
        <view class="crop-handle crop-handle-br"></view>
        <!-- 九宫格参考线 -->
        <view class="crop-grid">
          <view class="crop-grid-h1"></view>
          <view class="crop-grid-h2"></view>
          <view class="crop-grid-v1"></view>
          <view class="crop-grid-v2"></view>
        </view>
      </view>

      <!-- 底部操作 -->
      <view class="crop-bottom-bar">
        <view class="crop-action-btn" @tap="rotateCrop">
          <text class="crop-action-icon">🔄</text>
          <text class="crop-action-text">旋转</text>
        </view>
        <view class="crop-action-btn" @tap="smartCrop">
          <text class="crop-action-icon">🔲</text>
          <text class="crop-action-text">智能框选</text>
        </view>
        <view class="crop-confirm-btn" @tap="confirmCrop">
          <text class="crop-confirm-icon">✓</text>
          <text class="crop-confirm-text">开始识别</text>
        </view>
      </view>
    </view>

    <!-- ========== 阶段3：识别结果 ========== -->
    <view class="result-stage" v-if="stage === 'result'">
      <!-- 顶部图片 -->
      <view class="result-image-area">
        <image class="result-image" :src="resultImageSrc" mode="aspectFill"></image>
        <view class="result-image-mask"></view>
        <text class="result-image-label">拍摄的照片</text>
        <text class="result-image-badge">AI 识别</text>
      </view>

      <!-- 文字内容区 -->
      <view class="result-text-area" v-if="!resultLoading">
        <text class="result-text-content">{{ resultText }}</text>
      </view>
      <view class="result-text-area" v-else>
        <view class="result-text-loading">
          <view class="result-loading-spinner"></view>
          <text class="result-loading-text">{{ resultLoadingText }}</text>
        </view>
      </view>

      <!-- 语音波形 + 播放控制 -->
      <view class="audio-control-area" v-if="resultText && !resultLoading">
        <view class="play-btn" @tap="playAudio">
          <text class="play-btn-icon">{{ isPlaying ? '⏸' : '▶' }}</text>
        </view>

        <!-- 波形动画 -->
        <view class="wave-container">
          <view
            v-for="(bar, idx) in waveBars"
            :key="idx"
            class="wave-bar"
            :class="isPlaying ? 'wave-bar--active' : 'wave-bar--inactive'"
            :style="{ height: (isPlaying ? bar : 4) + 'rpx' }"
          ></view>
        </view>

        <text class="audio-time">{{ formatTime(audioProgress) }}</text>
      </view>

      <!-- 底部操作 -->
      <view class="result-bottom-bar">
        <view class="result-bottom-btn" @tap="resetToCamera">
          <text class="result-bottom-icon">📷</text>
          <text class="result-bottom-label">重新拍摄</text>
        </view>
        <view class="result-bottom-btn" @tap="copyText">
          <text class="result-bottom-icon">📋</text>
          <text class="result-bottom-label">复制文本</text>
        </view>
        <view class="result-bottom-btn" @tap="chooseFromAlbum">
          <text class="result-bottom-icon">🖼</text>
          <text class="result-bottom-label">相册选图</text>
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
