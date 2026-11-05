<template>
  <view class="page">
    <!-- 通用导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
      <view class="nav-back" @tap="goBack">
        <u-icon name="arrow-left" size="22" color="#fff"></u-icon>
      </view>
      <view class="nav-title">实时讲解</view>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- ========== 阶段1：相机预览 ========== -->
    <view class="camera-container" v-if="stage === 'camera' || stage === ''">
      <!-- 相机预览：绝对定位，不依赖flex -->
      <camera
        class="camera-live"
        :device-position="cameraDevice"
        :flash="flashMode"
        mode="normal"
        @error="onCameraError"
        @initdone="onCameraReady"
      ></camera>

      <!-- 顶部控制栏 -->
      <view class="cam-ctrl-top">
        <view class="cam-ctrl-btn" @tap="toggleFlash">
          <u-icon :name="flashMode === 'off' ? 'flash-off' : 'flash'" size="22" color="#fff"></u-icon>
          <text class="cam-ctrl-label">{{ flashMode === 'off' ? '关' : '开' }}</text>
        </view>
        <view class="cam-ctrl-btn" @tap="switchCamera">
          <u-icon name="camera" size="22" color="#fff"></u-icon>
          <text class="cam-ctrl-label">翻转</text>
        </view>
      </view>

      <!-- 底部操作栏 -->
      <view class="cam-ctrl-bottom">
        <view class="cam-ctrl-side" @tap="chooseFromAlbum">
          <u-icon name="photo" size="28" color="#fff"></u-icon>
          <text class="cam-ctrl-side-text">相册</text>
        </view>

        <view class="shutter-ring" @tap="takePhoto">
          <view class="shutter-inner"></view>
        </view>

        <view class="cam-ctrl-side" style="opacity:0">
          <u-icon name="photo" size="28" color="#fff"></u-icon>
        </view>
      </view>

      <!-- 底部提示 -->
      <view class="cam-hint">
        <text class="cam-hint-text">对准景点或物品，点击快门拍照</text>
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
        <view class="crop-handle crop-handle-tl"></view>
        <view class="crop-handle crop-handle-tr"></view>
        <view class="crop-handle crop-handle-bl"></view>
        <view class="crop-handle crop-handle-br"></view>
        <view class="crop-grid">
          <view class="crop-grid-h1"></view>
          <view class="crop-grid-h2"></view>
          <view class="crop-grid-v1"></view>
          <view class="crop-grid-v2"></view>
        </view>
      </view>

      <view class="crop-bottom-bar">
        <view class="crop-action-btn" @tap="rotateCrop">
          <u-icon name="reload" size="24" color="rgba(255,255,255,0.85)"></u-icon>
          <text class="crop-action-text">旋转</text>
        </view>
        <view class="crop-action-btn" @tap="smartCrop">
          <u-icon name="grid" size="24" color="rgba(255,255,255,0.85)"></u-icon>
          <text class="crop-action-text">智能框选</text>
        </view>
        <view class="crop-confirm-btn" @tap="confirmCrop">
          <u-icon name="checkmark" size="22" color="#fff"></u-icon>
          <text class="crop-confirm-text">开始识别</text>
        </view>
      </view>
    </view>

    <!-- ========== 阶段3：识别结果 ========== -->
    <view class="result-stage" v-if="stage === 'result'">
      <view class="result-image-area">
        <image class="result-image" :src="resultImageSrc" mode="aspectFill"></image>
        <view class="result-image-mask"></view>
        <text class="result-image-label">拍摄的照片</text>
        <text class="result-image-badge">AI 识别</text>
      </view>

      <view class="result-text-area" v-if="!resultLoading">
        <text class="result-text-content">{{ resultText }}</text>
      </view>
      <view class="result-text-area" v-else>
        <view class="result-text-loading">
          <view class="result-loading-spinner"></view>
          <text class="result-loading-text">{{ resultLoadingText }}</text>
        </view>
      </view>

      <view class="audio-control-area" v-if="resultText && !resultLoading">
        <view class="play-btn" @tap="playAudio">
          <u-icon :name="isPlaying ? 'pause' : 'play-right-fill'" size="28" color="#fff"></u-icon>
        </view>
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

      <view class="result-bottom-bar">
        <view class="result-bottom-btn" @tap="resetToCamera">
          <u-icon name="camera" size="28" color="rgba(255,255,255,0.6)"></u-icon>
          <text class="result-bottom-label">重新拍摄</text>
        </view>
        <view class="result-bottom-btn" @tap="copyText">
          <u-icon name="file-text" size="28" color="rgba(255,255,255,0.6)"></u-icon>
          <text class="result-bottom-label">复制文本</text>
        </view>
        <view class="result-bottom-btn" @tap="chooseFromAlbum">
          <u-icon name="photo" size="28" color="rgba(255,255,255,0.6)"></u-icon>
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
