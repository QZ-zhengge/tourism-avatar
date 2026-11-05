<template>
  <view>
    <!-- 语音唤醒反馈遮罩 -->
    <view class="dh-overlay" v-if="showOverlay" @tap="dismiss">
      <view class="dh-overlay-content" @tap.stop>
        <view class="dh-avatar">
          <view class="dh-avatar-ring"></view>
          <text class="dh-avatar-icon">🤖</text>
        </view>
        <view class="dh-greeting">{{ greeting }}</view>
        <view class="dh-subtitle">随行智能导游为您服务</view>
      </view>
    </view>
  </view>
</template>

<script>
import config from '@/config/index.js'

var WS_URL = (config.API_BASE_URL.startsWith('https') ? 'wss' : 'ws') + '://' + config.API_BASE_URL.replace(/https?:\/\//, '') + '/asr/stream'

export default {
  data() {
    return {
      showOverlay: false,
      greeting: '我在！',
      isListening: false,
      recorder: null,
      lastTrigger: 0
    }
  },

  mounted() {
    this.startListening()
  },

  beforeDestroy() {
    this.stopListening()
  },

  methods: {
    // 开始连续监听（不间断录音循环）
    startListening() {
      if (this.isListening) return
      this.isListening = true
      var self = this
      this.recorder = uni.getRecorderManager()

      this.recorder.onStop(function(res) {
        if (!self.isListening) return
        // 立即开始下一段录音（无间隔覆盖）
        self.startRecord()
        // 异步处理上一段音频
        if (res && res.size && res.size > 5000) {
          self.readAudioAndAsr(res.tempFilePath)
        }
      })

      this.recorder.onError(function() {
        if (self.isListening) {
          setTimeout(function() { self.startRecord() }, 200)
        }
      })

      this.startRecord()
    },

    // 开始一段录音（1.2秒，覆盖人声足够）
    startRecord() {
      if (!this.isListening) return
      try {
        this.recorder.start({ format: 'mp3', sampleRate: 16000, channels: 1, duration: 1.5 })
      } catch (e) {
        var self = this
        setTimeout(function() { self.startRecord() }, 200)
      }
    },

    stopListening() {
      this.isListening = false
      try { this.recorder && this.recorder.stop() } catch (e) {}
    },

    // 读取录音文件并送 ASR
    readAudioAndAsr(filePath) {
      var self = this
      // #ifdef APP-PLUS
      plus.io.resolveLocalFileSystemURL(filePath, function(entry) {
        entry.file(function(file) {
          if (file.size < 5000) return
          var reader = new plus.io.FileReader()
          reader.onloadend = function(e) {
            var dataUrl = e.target.result || ''
            var b64 = dataUrl.indexOf(',') >= 0 ? dataUrl.split(',')[1] : dataUrl
            if (b64 && b64.length > 100) self.connectAsr(b64)
          }
          reader.onerror = function() {}
          reader.readAsDataURL(file)
        }, function() {})
      }, function() {})
      // #endif

      // #ifdef MP-WEIXIN
      var fs = wx.getFileSystemManager()
      fs.readFile({
        filePath: filePath,
        success: function(fileRes) {
          var ab = fileRes.data
          if (!ab || ab.byteLength < 5000) return
          var b64 = arrayBufferToBase64(ab)
          self.connectAsr(b64)
        },
        fail: function() {}
      })
      // #endif
    },

    // 后端 ASR 识别
    connectAsr(base64Data) {
      var self = this
      var handled = false
      var asrText = ''

      var sock = uni.connectSocket({ url: WS_URL })
      sock.onOpen(function() {
        sock.send({ data: JSON.stringify({ type: 'config', language: 'zh', format: 'mp3', sampleRate: 16000 }) })
        // 分块发送
        var offset = 0
        function send() {
          if (offset >= base64Data.length) { sock.send({ data: JSON.stringify({ type: 'end' }) }); return }
          var end = Math.min(offset + 8192, base64Data.length)
          sock.send({ data: JSON.stringify({ type: 'audio', data: base64Data.substring(offset, end) }), success: function() { offset = end; setTimeout(send, 30) }, fail: function() { setTimeout(send, 50) } })
        }
        send()
      })

      sock.onMessage(function(m) {
        if (!m.data || handled) return
        try {
          var msg = JSON.parse(m.data)
          if (msg.type === 'final') { handled = true; asrText = msg.text || ''; sock.close(); self.checkWakeWord(asrText) }
          else if (msg.type === 'partial') { asrText = msg.text || '' }
          else if (msg.type === 'ping') { sock.send({ data: JSON.stringify({ type: 'pong' }) }) }
          else if (msg.type === 'error') { handled = true; sock.close() }
        } catch(e) {}
      })
      sock.onError(function() { if (!handled) { handled = true } })
      sock.onClose(function() { if (!handled) { handled = true; if (asrText) self.checkWakeWord(asrText) } })

      setTimeout(function() { if (!handled) { handled = true; try { sock.close() } catch(e) {} } }, 8000)
    },

    // 检测唤醒词
    checkWakeWord(text) {
      if (!text) return
      var kw = ['数字人', '小随', '导游', '随行']
      for (var i = 0; i < kw.length; i++) {
        if (text.indexOf(kw[i]) >= 0) { this.trigger(); return }
      }
    },

    // 触发跳转
    trigger() {
      var now = Date.now()
      if (now - this.lastTrigger < 5000) return
      this.lastTrigger = now
      // 已在数字人页面则不触发
      var pages = getCurrentPages()
      if (pages.length > 0) {
        var route = pages[pages.length - 1].route
        if (route && route.indexOf('digital-human') !== -1) return
      }

      this.stopListening()
      this.greeting = '我在！'
      this.showOverlay = true
      var self = this
      setTimeout(function() {
        self.showOverlay = false
        uni.switchTab({ url: '/pages/digital-human/index' })
        setTimeout(function() { self.startListening() }, 1500)
      }, 800)
    },

    dismiss() {
      this.showOverlay = false
      this.startListening()
    }
  }
}

function arrayBufferToBase64(buf) {
  var b = ''
  var u = new Uint8Array(buf)
  for (var i = 0; i < u.byteLength; i++) b += String.fromCharCode(u[i])
  return btoa(b)
}
</script>

<style>
@import './index.css';
</style>
