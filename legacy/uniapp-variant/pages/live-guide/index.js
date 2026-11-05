/**
 * 实时讲解页面
 * 拍照/选图 → 智能裁剪 → 模型识别 → TTS语音 + 文字展示
 */

const config = require('@/config/index.js')
const GUIDE_BASE = config.GUIDE_API_BASE

export default {
  data() {
    return {
      // 页面状态: camera | crop | result
      stage: 'camera',
      navTop: 0,
      navHeight: 88,

      // 相机相关
      cameraDevice: 'back',
      flashMode: 'off',
      previewSrc: '',
      albumSrc: '',

      // 裁剪相关
      cropImageSrc: '',
      cropBoxLeft: 50,
      cropBoxTop: 100,
      cropBoxWidth: 200,
      cropBoxHeight: 200,
      cropBoxMaxWidth: 280,
      cropBoxMaxHeight: 280,
      imgWidth: 0,
      imgHeight: 0,
      imgLeft: 0,
      imgTop: 0,
      containerWidth: 0,
      containerHeight: 0,
      isDragging: false,
      dragType: '', // move | resize
      dragStartX: 0,
      dragStartY: 0,
      dragStartLeft: 0,
      dragStartTop: 0,
      dragStartWidth: 0,
      dragStartHeight: 0,
      smartCropLoading: false,

      // 识别结果
      resultText: '',
      resultLoading: false,
      resultLoadingText: '正在识别中...',
      resultImageSrc: '',

      // 语音播放
      isPlaying: false,
      audioContext: null,
      audioProgress: 0,
      audioDuration: 0,
      audioTimer: null,

      // 语音波形
      waveBars: [3, 5, 8, 12, 16, 20, 16, 12, 8, 5, 3, 6, 10, 14, 18, 22, 18, 14, 10, 6],
      waveInterval: null,

      // TTS 地址（用户后续填写）
      ttsApiUrl: '',
      modelApiUrl: ''
    }
  },

  onLoad() {
    var sys = uni.getSystemInfoSync()
    this.navTop = sys.statusBarHeight || 0
    this.navHeight = (sys.statusBarHeight || 0) + 48
    this.containerWidth = sys.windowWidth
    this.containerHeight = sys.windowHeight - this.navHeight
  },

  onUnload() {
    this.stopAudio()
    if (this.audioContext) {
      this.audioContext.destroy()
      this.audioContext = null
    }
    if (this.waveInterval) {
      clearInterval(this.waveInterval)
    }
    if (this.audioTimer) {
      clearInterval(this.audioTimer)
    }
  },

  methods: {
    // ========== 相机阶段 ==========
    onCameraError(e) {
      console.error('[LiveGuide] 相机错误:', e)
    },

    onCameraReady() {
      console.log('[LiveGuide] 相机就绪')
    },

    takePhoto() {
      var self = this
      // #ifdef APP-PLUS
      // 优先用 uni.createCameraContext（内嵌相机拍照）
      try {
        var ctx = uni.createCameraContext()
        if (ctx) {
          ctx.takePhoto({
            quality: 'high',
            success: function(res) {
              console.log('[LiveGuide] 拍照成功:', res.tempImagePath)
              self.previewSrc = res.tempImagePath
              self.initCropImage(res.tempImagePath)
            },
            fail: function(err) {
              console.warn('[LiveGuide] createCameraContext 拍照失败:', JSON.stringify(err))
              // 降级：用系统相机
              self.openSystemCamera()
            }
          })
        } else {
          self.openSystemCamera()
        }
      } catch(e) {
        console.warn('[LiveGuide] 相机上下文异常:', e)
        self.openSystemCamera()
      }
      // #endif
      // #ifndef APP-PLUS
      this.chooseFromAlbum()
      // #endif
    },

    // 降级方案：用系统相机
    openSystemCamera() {
      var self = this
      // #ifdef APP-PLUS
      try {
        var cmr = plus.camera.getCamera()
        cmr.captureImage(function(filePath) {
          self.previewSrc = filePath
          self.initCropImage(filePath)
        }, function() {
          // 取消
        }, {
          filename: '_doc/live_' + Date.now() + '.jpg',
          index: 1
        })
      } catch(e) {
        self.chooseFromAlbum()
      }
      // #endif
    },

    // ========== 从相册选择 ==========
    chooseFromAlbum() {
      var self = this
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album'],
        success: function(res) {
          self.previewSrc = res.tempFilePaths[0]
          self.initCropImage(res.tempFilePaths[0])
        },
        fail: function() {
          uni.showToast({ title: '未选择图片', icon: 'none' })
        }
      })
    },

    goBack() {
      if (this.stage === 'crop') {
        this.stage = 'camera'
        this.previewSrc = ''
        return
      }
      if (this.stage === 'result') {
        this.resetToCamera()
        return
      }
      uni.navigateBack({ fail: function() { uni.switchTab({ url: '/pages/home/index' }) } })
    },

    // ==================== 裁剪阶段 ====================
    initCropImage(src) {
      var self = this
      this.cropImageSrc = src
      this.stage = 'crop'

      uni.getImageInfo({
        src: src,
        success: function(info) {
          var maxW = self.containerWidth - 40
          var maxH = self.containerHeight - 200
          var ratio = Math.min(maxW / info.width, maxH / info.height)
          self.imgWidth = Math.floor(info.width * ratio)
          self.imgHeight = Math.floor(info.height * ratio)
          self.imgLeft = Math.floor((self.containerWidth - self.imgWidth) / 2)
          self.imgTop = Math.floor((self.containerHeight - self.imgHeight) / 2 + 40)

          // 默认裁剪框居中
          var boxSize = Math.floor(Math.min(self.imgWidth, self.imgHeight) * 0.6)
          self.cropBoxWidth = boxSize
          self.cropBoxHeight = boxSize
          self.cropBoxLeft = Math.floor((self.containerWidth - boxSize) / 2)
          self.cropBoxTop = Math.floor((self.containerHeight - boxSize) / 2 + 20)
          self.cropBoxMaxWidth = self.imgWidth
          self.cropBoxMaxHeight = self.imgHeight
        }
      })
    },

    onCropTouchStart(e) {
      var touch = e.touches[0]
      this.isDragging = true
      this.dragStartX = touch.clientX
      this.dragStartY = touch.clientY
      this.dragStartLeft = this.cropBoxLeft
      this.dragStartTop = this.cropBoxTop
      this.dragStartWidth = this.cropBoxWidth
      this.dragStartHeight = this.cropBoxHeight
    },

    onCropTouchMove(e) {
      if (!this.isDragging) return
      var touch = e.touches[0]
      var dx = touch.clientX - this.dragStartX
      var dy = touch.clientY - this.dragStartY

      // 移动裁剪框
      var newLeft = this.dragStartLeft + dx
      var newTop = this.dragStartTop + dy

      // 边界限制
      newLeft = Math.max(this.imgLeft, Math.min(newLeft, this.imgLeft + this.imgWidth - this.cropBoxWidth))
      newTop = Math.max(this.imgTop, Math.min(newTop, this.imgTop + this.imgHeight - this.cropBoxHeight))

      this.cropBoxLeft = newLeft
      this.cropBoxTop = newTop
    },

    onCropTouchEnd() {
      this.isDragging = false
    },

    smartCrop() {
      // 智能框选：将裁剪框调整到图片中心 60% 区域
      this.smartCropLoading = true
      var self = this
      var boxSize = Math.floor(Math.min(this.imgWidth, this.imgHeight) * 0.6)
      setTimeout(function() {
        self.cropBoxWidth = boxSize
        self.cropBoxHeight = boxSize
        self.cropBoxLeft = Math.floor((self.containerWidth - boxSize) / 2)
        self.cropBoxTop = Math.floor((self.containerHeight - boxSize) / 2 + 20)
        self.smartCropLoading = false
        uni.showToast({ title: '智能框选完成', icon: 'success' })
      }, 600)
    },

    rotateCrop() {
      // 旋转90度（简单实现：交换宽高）
      var temp = this.cropBoxWidth
      this.cropBoxWidth = this.cropBoxHeight
      this.cropBoxHeight = temp
      uni.showToast({ title: '已旋转', icon: 'none' })
    },

    confirmCrop() {
      // 裁剪图片并进入识别阶段
      var self = this
      this.resultImageSrc = this.previewSrc
      this.stage = 'result'
      this.resultLoading = true
      this.resultText = ''
      this.resultLoadingText = '正在识别中...'

      // TODO: 替换为真实模型 API 地址
      // 目前用模拟数据演示流程
      setTimeout(function() {
        self.resultText = '这是灵山大佛景区的一处著名景点。\n\n灵山大佛高88米，是中国最大的青铜佛像之一，始建于1997年。大佛位于太湖之滨的灵山脚下，是灵山胜境的核心景观。\n\n佛像采用铜质材料铸造，面部面积约有140平方米，共用铜700余吨。大佛右手施"无畏印"，代表除却痛苦；左手施"与愿印"，代表给予幸福。\n\n周边景点包括梵宫、九龙灌浴、五印坛城等，建议游览时间3-4小时。'
        self.resultLoading = false
        // 自动播放语音
        self.playAudio()
      }, 2000)

      // 真实 API 调用（取消注释后使用）
      /*
      this.sendToModel(this.previewSrc, function(text) {
        self.resultText = text
        self.resultLoading = false
        self.playAudio()
      }, function(err) {
        self.resultLoading = false
        uni.showToast({ title: '识别失败: ' + err, icon: 'none' })
      })
      */
    },

    sendToModel(imagePath, onSuccess, onError) {
      // 将图片转为 base64
      var self = this
      // #ifdef APP-PLUS
      plus.io.resolveLocalFileSystemURL(imagePath, function(entry) {
        entry.file(function(file) {
          var reader = new plus.io.FileReader()
          reader.onloadend = function(e) {
            var base64 = e.target.result
            self.doModelRequest(base64, onSuccess, onError)
          }
          reader.readAsDataURL(file)
        })
      })
      // #endif

      // #ifdef H5
      uni.request({
        url: imagePath,
        responseType: 'arraybuffer',
        success: function(res) {
          var arrayBuffer = res.data
          var bytes = new Uint8Array(arrayBuffer)
          var binary = ''
          for (var i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i])
          }
          var base64 = 'data:image/jpeg;base64,' + uni.arrayBufferToBase64(arrayBuffer)
          self.doModelRequest(base64, onSuccess, onError)
        },
        fail: function() {
          if (onError) onError('图片读取失败')
        }
      })
      // #endif
    },

    doModelRequest(base64Data, onSuccess, onError) {
      // TODO: 替换为真实的模型 API 地址和参数
      var modelUrl = this.modelApiUrl || GUIDE_BASE + '/api/v1/guide/image-recognize'
      uni.request({
        url: modelUrl,
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        data: {
          image: base64Data,
          scenic_id: config.SCENIC_ID,
          scenic_name: config.SCENIC_NAME,
          prompt: '请对这张图片中的景点或物品进行详细讲解，包括历史背景、文化意义、建筑特色等信息。'
        },
        success: function(res) {
          if (res.statusCode === 200 && res.data) {
            var text = res.data.answer || res.data.text || res.data.description || ''
            if (text && onSuccess) onSuccess(text)
            else if (onError) onError('未返回有效内容')
          } else {
            if (onError) onError('请求失败')
          }
        },
        fail: function(err) {
          if (onError) onError('网络错误')
        }
      })
    },

    resetToCamera() {
      this.stopAudio()
      this.stage = 'camera'
      this.previewSrc = ''
      this.resultText = ''
      this.resultImageSrc = ''
      this.isPlaying = false
    },

    // ==================== 语音播放 ====================
    playAudio() {
      if (this.isPlaying) {
        this.stopAudio()
        return
      }

      if (!this.resultText) return

      var self = this

      // 如果有真实 TTS API，先获取音频
      if (this.ttsApiUrl) {
        this.fetchTtsAudio(this.resultText, function(audioUrl) {
          self.startPlayback(audioUrl)
        })
        return
      }

      // 模拟 TTS：使用 innerAudioContext（无实际音频，仅演示动画）
      // 实际使用时替换为真实 TTS URL
      this.isPlaying = true
      this.startWaveAnimation()
      this.audioProgress = 0

      // 模拟播放进度
      this.audioDuration = Math.ceil(this.resultText.length / 5) // 粗略估算
      this.audioTimer = setInterval(function() {
        if (self.audioProgress >= self.audioDuration) {
          self.stopAudio()
          return
        }
        self.audioProgress++
      }, 1000)

      // TODO: 真实场景取消注释
      // this.fetchTtsAudio(this.resultText, function(url) {
      //   self.startPlayback(url)
      // })
    },

    fetchTtsAudio(text, callback) {
      // TODO: 替换为真实 TTS API 地址（与数字人相同的后端）
      // var ttsUrl = this.ttsApiUrl || config.API_BASE_URL + '/api/v1/tts'
      // uni.request({
      //   url: ttsUrl,
      //   method: 'POST',
      //   header: { 'Content-Type': 'application/json' },
      //   data: { text: text, vcn: config.XUNFEI_VCN },
      //   success: function(res) {
      //     if (res.data && res.data.audio_url) {
      //       callback(res.data.audio_url)
      //     }
      //   }
      // })
      console.log('[LiveGuide] TTS API 待配置，当前为模拟模式')
    },

    startPlayback(audioUrl) {
      var self = this
      this.isPlaying = true
      this.startWaveAnimation()

      if (!this.audioContext) {
        this.audioContext = uni.createInnerAudioContext()
      }

      this.audioContext.src = audioUrl
      this.audioContext.play()

      this.audioContext.onTimeUpdate(function() {
        self.audioProgress = self.audioContext.currentTime
        self.audioDuration = self.audioContext.duration
      })

      this.audioContext.onEnded(function() {
        self.stopAudio()
      })

      this.audioContext.onError(function(err) {
        console.error('[LiveGuide] 音频播放错误:', err)
        self.stopAudio()
      })
    },

    stopAudio() {
      this.isPlaying = false
      this.audioProgress = 0
      if (this.audioTimer) {
        clearInterval(this.audioTimer)
        this.audioTimer = null
      }
      if (this.audioContext) {
        try { this.audioContext.pause() } catch(e) {}
      }
      this.stopWaveAnimation()
    },

    // ==================== 语音波形动画 ====================
    startWaveAnimation() {
      var self = this
      this.waveInterval = setInterval(function() {
        var bars = []
        for (var i = 0; i < 20; i++) {
          bars.push(Math.floor(Math.random() * 20) + 3)
        }
        self.waveBars = bars
      }, 150)
    },

    stopWaveAnimation() {
      if (this.waveInterval) {
        clearInterval(this.waveInterval)
        this.waveInterval = null
      }
      this.waveBars = [3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3]
    },

    // ==================== 工具 ====================
    formatTime(seconds) {
      if (!seconds || seconds < 0) return '0:00'
      var m = Math.floor(seconds / 60)
      var s = Math.floor(seconds % 60)
      return m + ':' + (s < 10 ? '0' : '') + s
    },

    copyText() {
      if (!this.resultText) return
      uni.setClipboardData({
        data: this.resultText,
        success: function() { uni.showToast({ title: '已复制', icon: 'success' }) }
      })
    }
  }
}
