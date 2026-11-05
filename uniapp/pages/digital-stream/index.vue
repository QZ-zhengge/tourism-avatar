<template>
  <view class="container">
    <!-- 顶部栏 -->
    <view class="top-bar">
      <view class="scene-picker" @click="showScenePicker">
        <text>◫ {{ scenes[sceneIndex] }}</text>
      </view>
      <view class="caption-toggle" @click="toggleCaption">
        <text>{{ showCaption ? '字幕开' : '字幕关' }}</text>
      </view>
    </view>

    <!-- 视频区域 -->
    <view class="video-wrap">
      <!-- 空闲状态占位 -->
      <view v-if="status === 'idle'" class="placeholder" @click="startDigitalHuman">
        <text class="placeholder-icon">▶</text>
        <text class="placeholder-text">点击唤醒数字人</text>
      </view>

      <!-- 启动中状态 -->
      <view v-if="status === 'starting'" class="placeholder">
        <text class="placeholder-icon">⏳</text>
        <text class="placeholder-text">数字人启动中，请稍候...</text>
      </view>

      <!-- 失败状态 -->
      <view v-if="status === 'failed'" class="placeholder">
        <text class="placeholder-icon">❌</text>
        <text class="placeholder-text">启动失败</text>
        <view class="retry-btn" @click="startDigitalHuman">
          <text>点击重试</text>
        </view>
      </view>

      <!-- HLS (m3u8) 视频 -->
      <video
        v-if="status === 'ready' && isHlsUrl"
        id="hlsVideo"
        class="video-player"
        :src="videoUrl"
        :autoplay="true"
        :loop="false"
        :controls="true"
        :show-center-play-btn="true"
        :show-loading="true"
        :x5-video-player-type="'h5'"
        :x5-video-player-fullscreen="false"
        :enable-progress-gesture="false"
        object-fit="contain"
        :play-btn-position="'center'"
        @error="onVideoError"
        @statechange="onVideoStateChange"
        @play="onVideoPlay"
        @waiting="onVideoWaiting"
        @ended="onVideoEnded"
      ></video>

      <!-- FLV 视频 -->
      <view
        v-if="status === 'ready' && !isHlsUrl"
        id="flvContainer"
        class="video-player"
      ></view>
    </view>

    <!-- 字幕 -->
    <view v-if="showCaption && status === 'ready'" class="caption">
      <text>{{ captionText || '等待指令...' }}</text>
    </view>
    <view v-else-if="status === 'idle'" class="caption">
      <text>等待指令...</text>
    </view>

    <!-- 快捷问题 -->
    <view class="question-wrap">
      <view
        v-for="(q, index) in quickQuestions"
        :key="index"
        class="question-chip"
        @click="sendText(q)"
      >
        <text>{{ q }}</text>
      </view>
    </view>

    <!-- 控制按钮 -->
    <view class="control-bar">
      <view class="control-item" @click="onVoice">
        <view class="control-icon">
          <text>◉</text>
        </view>
        <text class="control-label">语音</text>
      </view>

      <view class="control-item" @click="onText">
        <view class="control-icon">
          <text>⌨</text>
        </view>
        <text class="control-label">文字</text>
      </view>

      <view class="control-item" @click="startDigitalHuman">
        <view class="control-icon">
          <text>▣</text>
        </view>
        <text class="control-label">视频</text>
      </view>

      <view class="control-item" @click="onEnd">
        <view class="control-icon" style="color: #ff3b30;">
          <text>✕</text>
        </view>
        <text class="control-label">结束</text>
      </view>
    </view>

  </view>
</template>

<script>
import pageLogic from './index.js'

export default {
  data() {
    return {
      baseUrl: 'http://192.0.2.1:8000', // 你的后端地址
      status: 'idle',
      clientId: '',
      videoUrl: '',
      captionText: '',
      isRecording: false,
      recorderManager: null,
      innerAudioContext: null,
      recordFilePath: ''
    }
  },

  onLoad() {
    console.log('页面加载完成')
    this.initAppComponents()
  },

  methods: {
    // 1. 初始化 App 原生组件
    initAppComponents() {
      // 初始化录音管理器
      this.recorderManager = uni.getRecorderManager()
      
      this.recorderManager.onStart(() => {
        console.log('App 原生录音开始...')
      })

      this.recorderManager.onStop((res) => {
        console.log('App 原生录音结束, 文件路径:', res.tempFilePath)
        this.recordFilePath = res.tempFilePath
        
        // 录音一结束，立刻把文件送到自己的后端，由后端去转写 ASR
        this.uploadAudioForAsr(res.tempFilePath)
      })

      this.recorderManager.onError((err) => {
        console.error('录音失败:', err)
        uni.showToast({ title: '录音权限未开启或失败', icon: 'none' })
      })

      // 初始化音频播放器（供 TTS 使用）
      this.innerAudioContext = uni.createInnerAudioContext()
    },

    // 2. 麦克风长按事件（原生触控优化）
    onMicPress() {
      if (this.isRecording) return
      this.isRecording = true
      
      // 震动反馈（提升 App 真实手感）
      uni.vibrateShort()

      // App 端最稳妥的录音配置：单声道、16k 采样率、16bit
      this.recorderManager.start({
        duration: 60000,
        sampleRate: 16000,
        numberOfChannels: 1,
        format: 'pcm' // 或者是 mp3，取决于你后端对接讯飞用的格式
      })
    },

    onMicRelease() {
      if (!this.isRecording) return
      this.isRecording = false
      this.recorderManager.stop()
    },

    // 3. 将录音文件上传至自有后端，规避前端复杂的 WebSocket 鉴权报错
    uploadAudioForAsr(filePath) {
      uni.showLoading({ title: '语音识别中...' })
      
      // 使用 uni.uploadFile 原生上传流
      uni.uploadFile({
        url: `${this.baseUrl}/digital/asr/`, // 建议后端暴露一个 ASR 接口
        filePath: filePath,
        name: 'file',
        formData: {
          client_id: this.clientId
        },
        success: (uploadRes) => {
          uni.hideLoading()
          const res = JSON.parse(uploadRes.data)
          if (res.code === 0) {
            console.log('后端识别文字成功:', res.text)
            this.captionText = res.text
            
            // 自动驱动数字人回答
            if (res.text) {
              this.sendText(res.text)
            }
          } else {
            uni.showToast({ title: '识别失败: ' + res.message, icon: 'none' })
          }
        },
        fail: (err) => {
          uni.hideLoading()
          console.error('上传音频失败:', err)
          uni.showToast({ title: '网络上传失败', icon: 'none' })
        }
      })
    },

    // 4. 原生 App 端的 TTS 合成方案 (测试“你好”)
    playTestTTS(text = "你好") {
      uni.showLoading({ title: '语音合成中...' })
      
      // 请求后端：让后端生成一个合规的 MP3 URL 或者 Base64 音频文件
      uni.request({
        url: `${this.baseUrl}/digital/tts/`, // 建议后端暴露一个 TTS 接口
        method: 'POST',
        data: { text: text },
        success: (res) => {
          uni.hideLoading()
          if (res.data.code === 0 && res.data.audioUrl) {
            // 使用 App 原生播放器播放声音
            this.innerAudioContext.src = res.data.audioUrl
            this.innerAudioContext.play()
            console.log('开始播放 TTS 声音')
          } else {
            uni.showToast({ title: '合成失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.hideLoading()
          uni.showToast({ title: '网络请求失败', icon: 'none' })
        }
      })
    },

    // 5. 原有数字人基础控制（保持 App 端最简兼容）
    sendText(text) {
      if (!text.trim()) return
      uni.request({
        url: `${this.baseUrl}/digital/speak/`,
        method: 'POST',
        data: { client_id: this.clientId, text: text },
        success: (res) => {
          if (res.data.code === 0) this.captionText = text
        }
      })
    }
  }
}

</script>

<style>
@import url('./index.css');
</style>
