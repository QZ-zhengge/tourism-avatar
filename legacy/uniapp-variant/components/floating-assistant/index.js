/**
 * 悬浮迷你数字人助手
 * 
 * 功能：
 * - 在所有页面常驻的悬浮按钮
 * - 显示数字人缩小版头像（头肩部）
 * - 长按录音 → ASR → guide/chat → TTS(avatar/drive/text)
 * - 对话泡泡显示文字（流式输出）
 * - 可拖拽到任意位置
 */
import config from '@/config/index.js'

const BACKEND_BASE = config.API_BASE_URL       // http://192.0.2.1:50080
const GUIDE_BASE = config.GUIDE_API_BASE        // http://192.0.2.1:50081
const BACKEND_WS_URL = (BACKEND_BASE.startsWith('https') ? 'wss' : 'ws') + '://' + BACKEND_BASE.replace(/https?:\/\//, '') + '/asr/stream'
const XUNFEI_VCN = config.XUNFEI_VCN || 'x4_lingxiaoyu_assist'

// ==================== 模块级状态 ====================
let recorderManager = null
let faSession = ''          // /avatar/start 返回的 session
let faStreamUrl = ''        // 视频流地址
let faPingTimer = null      // 心跳定时器
let faIsProcessing = false  // 防止并发
let faGuideSessionId = ''   // guide/chat 会话ID
let faAsrHandled = false
let faAsrFinalText = ''

function log(method, ...args) {
  console.log('[FA]', method, ...args)
}
function logError(method, ...args) {
  console.error('[FA]', method, ...args)
}

// ==================== base64 工具 ====================
function arrayBufferToBase64(buffer) {
  var bytes = new Uint8Array(buffer)
  var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
  var result = ''
  for (var i = 0; i < bytes.length; i += 3) {
    var b1 = bytes[i], b2 = bytes[i + 1] || 0, b3 = bytes[i + 2] || 0
    result += chars.charAt(b1 >> 2)
    result += chars.charAt(((b1 & 3) << 4) | (b2 >> 4))
    result += (i + 1 < bytes.length) ? chars.charAt(((b2 & 15) << 2) | (b3 >> 6)) : '='
    result += (i + 2 < bytes.length) ? chars.charAt(b3 & 63) : '='
  }
  return result
}

function base64ToArrayBuffer(base64) {
  var map = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
  var lookup = new Uint8Array(256)
  for (var i = 0; i < map.length; i++) lookup[map.charCodeAt(i)] = i
  var len = base64.length
  if (base64[len - 1] === '=') len--
  if (base64[len - 1] === '=') len--
  var bufLen = len * 0.75
  var arrayBuffer = new ArrayBuffer(bufLen)
  var bytes = new Uint8Array(arrayBuffer)
  var p = 0
  for (var i2 = 0; i2 < len; i2 += 4) {
    var e1 = lookup[base64.charCodeAt(i2)]
    var e2 = lookup[base64.charCodeAt(i2 + 1)]
    var e3 = lookup[base64.charCodeAt(i2 + 2)]
    var e4 = lookup[base64.charCodeAt(i2 + 3)]
    bytes[p++] = (e1 << 2) | (e2 >> 4)
    if (base64.charAt(i2 + 2) !== '=') bytes[p++] = ((e2 & 15) << 4) | (e3 >> 2)
    if (base64.charAt(i2 + 3) !== '=') bytes[p++] = ((e3 & 3) << 6) | e4
  }
  return arrayBuffer
}

// ==================== 后端 HTTP 请求 ====================
function requestBackend(method, path, data) {
  return new Promise(function(resolve, reject) {
    uni.request({
      url: BACKEND_BASE + path,
      method: method,
      data: data,
      header: { 'Content-Type': 'application/json' },
      success: function(res) {
        if (res.statusCode === 200) {
          var d = res.data
          if (d.code === 200 || d.code === 0) {
            resolve(d.data || d)
          } else {
            reject(new Error(d.msg || d.message || '业务错误 code=' + d.code))
          }
        } else {
          reject(new Error('HTTP ' + res.statusCode))
        }
      },
      fail: function(err) {
        reject(err)
      }
    })
  })
}

// ==================== 数字人会话管理 ====================
function sleep(ms) {
  return new Promise(function(resolve) { setTimeout(resolve, ms) })
}

function sanitizeText(text) {
  if (!text) return text
  var r = [['佛教文化', '历史文化'], ['佛教圣地', '风景名胜'], ['佛教艺术', '传统艺术'], ['佛教智慧', '古老智慧'],
           ['藏传佛教', '藏族传统'], ['释迦牟尼', '创始人'], ['转经筒祈福', '转经筒参观'],
           ['祈福', '游览'], ['圣水', '清水'], ['禅意', '意境'], ['禅茶', '清茶'], ['佛法', '智慧'], ['佛', '']]
  var result = text
  for (var i = 0; i < r.length; i++) {
    result = result.split(r[i][0]).join(r[i][1])
  }
  return result.replace(/\s+/g, ' ').trim()
}

// 启动数字人会话（惰性，首次使用时调用）
function startFaSession() {
  return new Promise(function(resolve, reject) {
    log('启动数字人会话...')
    if (faSession) {
      log('已有会话:', faSession)
      resolve({ session: faSession, stream_url: faStreamUrl })
      return
    }
    requestBackend('POST', '/avatar/start', {
      avatar_id: '118801001',
      width: 1280,
      height: 720,
      protocol: 'rtmp',
      bg_custom: true
    }).then(function(data) {
      var session = data.session || data.client_id || ''
      var streamUrl = data.stream_url || ''
      if (session) {
        faSession = session
        faStreamUrl = streamUrl
        log('会话已建立:', session, 'stream:', streamUrl)
        // 启动心跳
        startFaPing()
        resolve(data)
      } else {
        reject(new Error('未获取到 session'))
      }
    }).catch(function(err) {
      logError('启动会话失败:', err.message)
      reject(err)
    })
  })
}

function startFaPing() {
  stopFaPing()
  faPingTimer = setInterval(function() {
    if (faSession) {
      requestBackend('POST', '/avatar/ping', { session: faSession }).then(function() {
        log('心跳OK')
      }).catch(function(err) {
        logError('心跳失败:', err.message)
      })
    }
  }, 30000)
}

function stopFaPing() {
  if (faPingTimer) {
    clearInterval(faPingTimer)
    faPingTimer = null
  }
}

function stopFaSession() {
  if (!faSession) return
  log('停止会话:', faSession)
  stopFaPing()
  requestBackend('POST', '/avatar/stop', { session: faSession }).then(function() {
    log('会话已停止')
  }).catch(function(err) {
    logError('停止会话失败:', err.message)
  })
  faSession = ''
  faStreamUrl = ''
}

// 文本驱动（TTS + 口型，让数字人说话）
function driveFaText(text) {
  if (!faSession) {
    return Promise.reject(new Error('No session'))
  }
  return requestBackend('POST', '/avatar/drive/text', {
    session: faSession,
    text: sanitizeText(text),
    vcn: XUNFEI_VCN,
    speed: 50,
    pitch: 50,
    volume: 50
  })
}

// ==================== guide/chat API ====================
function callGuideChat(query) {
  return new Promise(function(resolve, reject) {
    if (!faGuideSessionId) {
      faGuideSessionId = 'fa_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6)
    }
    uni.request({
      url: GUIDE_BASE + '/api/v1/guide/chat',
      method: 'POST',
      data: {
        session_id: faGuideSessionId,
        query: query,
        scenic_id: config.SCENIC_ID,
        scenic_name: config.SCENIC_NAME,
        with_memory: true
      },
      header: { 'Content-Type': 'application/json' },
      success: function(res) {
        if (res.statusCode === 200 && res.data) {
          if (res.data.code === 200) {
            resolve(res.data.data || res.data)
          } else if (res.data.answer) {
            resolve(res.data)
          } else {
            reject(new Error('响应格式异常'))
          }
        } else {
          reject(new Error('HTTP ' + res.statusCode))
        }
      },
      fail: function(err) {
        reject(err)
      }
    })
  })
}

// ==================== 后端 ASR WebSocket ====================
function doAsr(base64Data, audioFormat) {
  return new Promise(function(resolve, reject) {
    log('开始 ASR, 格式:', audioFormat, '大小:', base64Data.length)
    faAsrFinalText = ''
    faAsrHandled = false
    var timeoutId = null

    var socketTask = uni.connectSocket({
      url: BACKEND_WS_URL,
      success: function() { log('ASR连接已发送') }
    })

    socketTask.onOpen(function() {
      log('ASR Socket已连接')
      // 发送配置
      socketTask.send({
        data: JSON.stringify({ type: 'config', language: 'zh', format: audioFormat || 'mp3', sampleRate: 16000 }),
        success: function() { log('config已发送') },
        fail: function(err) { logError('config失败:', JSON.stringify(err)) }
      })
      // 分块发送音频
      var WS_CHUNK = 8192
      var wsOff = 0
      function sendChunk() {
        if (wsOff >= base64Data.length) {
          socketTask.send({
            data: JSON.stringify({ type: 'end' }),
            success: function() { log('end已发送') },
            fail: function(err) { logError('end失败:', JSON.stringify(err)) }
          })
          return
        }
        var end = Math.min(wsOff + WS_CHUNK, base64Data.length)
        socketTask.send({
          data: JSON.stringify({ type: 'audio', data: base64Data.substring(wsOff, end) }),
          success: function() { wsOff = end; setTimeout(sendChunk, 30) },
          fail: function(err) { logError('发送失败:', JSON.stringify(err)); setTimeout(sendChunk, 50) }
        })
      }
      sendChunk()
      timeoutId = setTimeout(function() {
        logError('ASR 20秒超时')
        if (faAsrFinalText === '') { socketTask.close(); reject(new Error('ASR超时')) }
      }, 20000)
    })

    socketTask.onMessage(function(msgRes) {
      if (!msgRes.data) return
      try {
        var msg = JSON.parse(msgRes.data)
        if (msg.type === 'partial') {
          faAsrFinalText = msg.text
        } else if (msg.type === 'final') {
          log('ASR最终结果:', msg.text)
          if (timeoutId) { clearTimeout(timeoutId) }
          if (!faAsrHandled) {
            faAsrHandled = true
            setTimeout(function() { socketTask.close(); resolve(msg.text) }, 300)
          }
        } else if (msg.type === 'ping') {
          socketTask.send({ data: JSON.stringify({ type: 'pong' }) })
        } else if (msg.type === 'error') {
          logError('ASR错误:', msg.msg)
          if (!faAsrHandled) { faAsrHandled = true; socketTask.close(); reject(new Error(msg.msg)) }
        }
      } catch (e) { logError('ASR解析:', e.message) }
    })

    socketTask.onError(function(err) {
      logError('ASR Socket错误:', JSON.stringify(err))
      if (!faAsrHandled) { faAsrHandled = true; reject(new Error('ASR连接失败')) }
    })

    socketTask.onClose(function() {
      log('ASR Socket关闭')
      if (!faAsrHandled) {
        faAsrHandled = true
        if (faAsrFinalText) { resolve(faAsrFinalText) }
        else { reject(new Error('ASR无结果')) }
      }
    })
  })
}

// ==================== 录音管理 ====================
function initRecorder() {
  if (recorderManager) return
  // #ifndef MP-WEIXIN
  recorderManager = uni.getRecorderManager()
  // #endif
  // #ifdef MP-WEIXIN
  recorderManager = wx.getRecorderManager()
  // #endif
  recorderManager.onStop(function(res) {
    log('录音停止, 文件:', res.tempFilePath)
    // 通过事件通知组件
    uni.$emit('faRecorderStop', { path: res.tempFilePath })
  })
  recorderManager.onError(function(e) {
    logError('录音错误:', JSON.stringify(e))
  })
}

function startFaRecording() {
  initRecorder()
  log('开始录音')
  recorderManager.start({
    duration: 60000,
    format: 'mp3',
    sampleRate: 16000,
    numberOfChannels: 1
  })
}

function stopFaRecording() {
  if (recorderManager) {
    log('停止录音')
    recorderManager.stop()
  }
}

// 读取录音文件 → ASR
function readAudioAndSend(filePath, callbacks) {
  // #ifdef APP-PLUS
  plus.io.resolveLocalFileSystemURL(filePath, function(entry) {
    entry.file(function(file) {
      if (file.size === 0) {
        logError('录音文件为空')
        if (callbacks.onError) callbacks.onError('空文件')
        return
      }
      var reader = new plus.io.FileReader()
      reader.onloadend = function(e) {
        var dataUrl = e.target.result || ''
        var b64 = dataUrl.indexOf(',') >= 0 ? dataUrl.split(',')[1] : dataUrl
        if (!b64 || b64.length === 0) {
          logError('读取文件空')
          if (callbacks.onError) callbacks.onError('空文件')
          return
        }
        doAsr(b64, 'mp3').then(function(text) {
          if (callbacks.onAsrResult) callbacks.onAsrResult(text)
        }).catch(function(err) {
          logError('ASR失败:', err.message)
          if (callbacks.onError) callbacks.onError(err.message)
        })
      }
      reader.onerror = function(err) {
        logError('FileReader失败:', JSON.stringify(err))
        if (callbacks.onError) callbacks.onError('读取失败')
      }
      reader.readAsDataURL(file)
    }, function(err) {
      logError('获取文件对象失败:', JSON.stringify(err))
      if (callbacks.onError) callbacks.onError('获取文件失败')
    })
  }, function(err) {
    logError('解析路径失败:', JSON.stringify(err))
    if (callbacks.onError) callbacks.onError('路径解析失败')
  })
  // #endif

  // #ifdef MP-WEIXIN
  var fs = wx.getFileSystemManager()
  fs.readFile({
    filePath: filePath,
    success: function(fileRes) {
      var ab = fileRes.data
      if (!ab || ab.byteLength === 0) {
        logError('微信录音文件为空')
        if (callbacks.onError) callbacks.onError('空文件')
        return
      }
      var b64 = arrayBufferToBase64(ab)
      doAsr(b64, 'mp3').then(function(text) {
        if (callbacks.onAsrResult) callbacks.onAsrResult(text)
      }).catch(function(err) {
        logError('ASR失败:', err.message)
        if (callbacks.onError) callbacks.onError(err.message)
      })
    },
    fail: function(err) {
      logError('读取录音失败:', JSON.stringify(err))
      if (callbacks.onError) callbacks.onError('读取失败')
    }
  })
  // #endif

  // #ifdef H5
  logError('H5不支持录音')
  if (callbacks.onError) callbacks.onError('H5不支持录音')
  // #endif
}

// ==================== Vue 组件 ====================
export default {
  data() {
    return {
      // 位置
      floatX: 0,
      floatY: 0,
      isDragging: false,
      dragStartX: 0,
      dragStartY: 0,
      btnStartX: 0,
      btnStartY: 0,
      touchStartTime: 0,

      // 状态
      hasSession: false,
      streamUrl: '',
      isRecording: false,
      isSpeaking: false,

      // 对话泡泡
      bubbleVisible: false,
      bubbleText: '',
      bubbleDisplayText: '',
      isTyping: false,
      bubbleArrowDir: 'right',
      typingTimer: null,
      bubbleDismissTimer: null,

      // 语音波形
      waveBars: [3, 5, 8, 12, 16, 20, 16, 12, 8, 5, 3, 6, 10, 14, 18, 22, 18, 14, 10, 6],
      waveInterval: null,
    }
  },

  computed: {
    // 泡泡位置（像素，跟随悬浮按钮位置更新）
    bubblePositionStyle() {
      var style = {}
      var BUBBLE_W = 240           // 泡泡宽度 px（≈ ~480rpx）
      var GAP = 12                  // 按钮与泡泡间距 px

      if (this.floatX + 60 + BUBBLE_W + GAP < this._winWidth) {
        // 按钮右侧有空间
        style.left = (this.floatX + 60 + GAP) + 'px'
        style.top = (this.floatY - 10) + 'px'
      } else {
        // 右侧不够，放左边
        style.left = (this.floatX - BUBBLE_W - GAP) + 'px'
        style.top = (this.floatY - 10) + 'px'
      }
      return style
    }
  },
  watch: {
    // 当按钮位置变化时，同步计算箭头方向
    floatX: function(val) {
      this._updateArrowDir()
    }
  },

  mounted() {
    console.log('[FA] ✅ mounted() 被调用！')
    console.log('[FA] 屏幕信息:', JSON.stringify({ w: this._winWidth, h: this._winHeight }))
    this.initPosition()
    console.log('[FA] 初始位置:', this.floatX, this.floatY)
    console.log('[FA] hasSession:', this.hasSession)
    console.log('[FA] streamUrl:', this.streamUrl)
    // 监听录音停止事件
    this._recorderStopHandler = this._onRecorderStop.bind(this)
    uni.$on('faRecorderStop', this._recorderStopHandler)
  },

  beforeDestroy() {
    // 清理
    uni.$off('faRecorderStop', this._recorderStopHandler)
    if (this.typingTimer) clearInterval(this.typingTimer)
    if (this.bubbleDismissTimer) clearTimeout(this.bubbleDismissTimer)
    if (this.waveInterval) clearInterval(this.waveInterval)
    this.cleanupRecording()
    stopFaSession()
  },

  methods: {

    // ========== 位置初始化 ==========
    initPosition() {
      try {
        var info = uni.getSystemInfoSync()
        this._winWidth = info.windowWidth
        this._winHeight = info.windowHeight
        var sb = info.safeArea ? Math.max(0, info.screenHeight - info.safeArea.bottom) : 0
        this.floatX = info.windowWidth - 130
        this.floatY = info.windowHeight - 220 - sb
      } catch (e) {
        this._winWidth = 375
        this._winHeight = 667
        this.floatX = 250
        this.floatY = 450
      }
      this._updateArrowDir()
    },

    // 根据按钮位置决定泡泡箭头方向
    _updateArrowDir() {
      var BUBBLE_W = 240
      var GAP = 12
      if (this.floatX + 60 + BUBBLE_W + GAP < this._winWidth) {
        this.bubbleArrowDir = 'right'
      } else {
        this.bubbleArrowDir = 'left'
      }
    },

    // ========== 触摸事件：拖拽 + 长按 ==========
    onTouchStart(e) {
      this.isDragging = false
      var touch = e.touches[0]
      this.dragStartX = touch.clientX
      this.dragStartY = touch.clientY
      this.btnStartX = this.floatX
      this.btnStartY = this.floatY
      this.touchStartTime = Date.now()

      // 启动长按定时器（600ms 后触发录音）
      this._longPressTimer = setTimeout(function() {
        if (!this.isDragging) {
          log('长按触发，开始录音')
          this._startFullFlow()
        }
      }.bind(this), 600)
    },

    onTouchMove(e) {
      var touch = e.touches[0]
      var dx = touch.clientX - this.dragStartX
      var dy = touch.clientY - this.dragStartY
      if (Math.abs(dx) > 6 || Math.abs(dy) > 6) {
        this.isDragging = true
        // 取消长按
        if (this._longPressTimer) {
          clearTimeout(this._longPressTimer)
          this._longPressTimer = null
        }
      }
      if (this.isDragging) {
        this.floatX = Math.max(0, Math.min(this._winWidth - 100, this.btnStartX + dx))
        this.floatY = Math.max(60, Math.min(this._winHeight - 140, this.btnStartY + dy))
      }
    },

    onTouchEnd() {
      if (this._longPressTimer) {
        clearTimeout(this._longPressTimer)
        this._longPressTimer = null
      }
      // 如果正在录音，停止录音
      if (this.isRecording) {
        this._stopRecording()
      }
      this.isDragging = false
    },

    // ========== 完整流程：录音 → ASR → Chat → TTS + 泡泡 ==========
    _startFullFlow() {
      if (faIsProcessing) {
        log('正在处理中，跳过')
        return
      }

      // 先确保数字人会话已建立
      var self = this
      if (!faSession) {
        log('首次使用，建立数字人会话...')
        // 先显示录音，但等会话建好才真正开始
        self.isRecording = true
        startFaSession().then(function() {
          log('会话已就绪')
          self.hasSession = true
          self.streamUrl = faStreamUrl
          // 会话就绪后开始录音
          startFaRecording()
        }).catch(function(err) {
          logError('建立会话失败:', err.message)
          self.isRecording = false
          uni.showToast({ title: '数字人连接失败', icon: 'none' })
        })
      } else {
        self.isRecording = true
        startFaRecording()
      }
    },

    // 录音停止回调（通过 uni.$on 接收）
    _onRecorderStop(e) {
      if (!e || !e.path) return
      this.isRecording = false
      var self = this

      // 取消之前的泡泡消失定时器
      if (this.bubbleDismissTimer) {
        clearTimeout(this.bubbleDismissTimer)
        this.bubbleDismissTimer = null
      }

      // 显示"聆听中..."泡泡
      self.bubbleText = '🎧 聆听中...'
      self.bubbleDisplayText = '🎧 聆听中...'
      self.bubbleVisible = true

      readAudioAndSend(e.path, {
        onAsrResult: function(text) {
          if (!text || text.trim() === '') {
            log('ASR结果为空')
            self._showBubble('😅 没有听清，请再说一遍')
            setTimeout(function() { self.dismissBubble() }, 2000)
            return
          }
          log('ASR识别结果:', text)

          // 显示"思考中..."
          self.bubbleText = '💭 ' + text + '...'
          self.bubbleDisplayText = '💭 ' + text + '...'

          // 2. 调用 guide/chat
          faIsProcessing = true
          callGuideChat(text).then(function(response) {
            var answer = response.speech_text || response.answer || '抱歉，暂时无法回答。'
            log('Chat回答:', answer)

            // 先尝试驱动会话（失败也不影响显示文字）
            self.isSpeaking = true
            self.startWaveAnimation()
            if (faSession) {
              driveFaText(answer).then(function() {
                log('驱动成功')
              }).catch(function(err) {
                logError('驱动失败:', err.message)
                // 驱动失败无所谓，但泡泡文字仍然显示
              })
            } else {
              // 没有数字人会话时 fallback 到 system TTS
              self._fallbackTts(answer)
            }

            // 3. 显示泡泡（流式输出）
            self._showBubbleWithTyping(answer, function() {
              // 打字完成后，等语音播完再消失
              var speechDuration = Math.min(answer.length * 150, 15000)
              self.bubbleDismissTimer = setTimeout(function() {
                self.isSpeaking = false
                self.stopWaveAnimation()
                self.dismissBubble()
              }, speechDuration + 1000)
            })
          }).catch(function(err) {
            logError('Chat失败:', err.message)
            self._showBubble('😅 网络开小差了~')
            setTimeout(function() { self.dismissBubble() }, 2000)
          }).finally(function() {
            faIsProcessing = false
          })
        },
        onError: function(errMsg) {
          logError('处理流程失败:', errMsg)
          self._showBubble('😅 出错了: ' + errMsg)
          setTimeout(function() { self.dismissBubble() }, 2000)
          faIsProcessing = false
        }
      })
    },

    _stopRecording() {
      stopFaRecording()
    },

    // ========== 系统 TTS 降级（无数字人会话时用） ==========
    _fallbackTts(text) {
      if (!text) return
      // #ifdef APP-PLUS
      try {
        if (typeof plus !== 'undefined' && plus.speech) {
          plus.speech.speak(text.replace(/<[^>]+>/g, ''), { rate: 1.0 })
        }
      } catch (e) {
        logError('TTS失败:', e)
      }
      // #endif
      // #ifdef H5
      try {
        if (window.speechSynthesis) {
          var utter = new SpeechSynthesisUtterance(text.replace(/<[^>]+>/g, ''))
          utter.lang = 'zh-CN'
          utter.rate = 0.9
          window.speechSynthesis.speak(utter)
        }
      } catch (e) {
        logError('H5 TTS:', e)
      }
      // #endif
    },

    // ========== 对话泡泡管理 ==========
    _showBubble(text) {
      this.bubbleText = text
      this.bubbleDisplayText = text
      this.bubbleVisible = true
      this.isTyping = false
    },

    _showBubbleWithTyping(fullText, onDone) {
      this.bubbleText = fullText
      this.bubbleDisplayText = ''
      this.bubbleVisible = true
      this.isTyping = true

      var self = this
      var idx = 0
      if (this.typingTimer) clearInterval(this.typingTimer)

      this.typingTimer = setInterval(function() {
        if (idx < fullText.length) {
          self.bubbleDisplayText = fullText.substring(0, idx + 1)
          idx++
        } else {
          clearInterval(self.typingTimer)
          self.typingTimer = null
          self.isTyping = false
          if (onDone) onDone()
        }
      }, 50)  // 每50ms出一个字，速度快但不是瞬时
    },

    dismissBubble() {
      this.bubbleVisible = false
      this.bubbleText = ''
      this.bubbleDisplayText = ''
      this.isTyping = false
      this.isSpeaking = false
      this.stopWaveAnimation()
      if (this.typingTimer) {
        clearInterval(this.typingTimer)
        this.typingTimer = null
      }
    },

    // ========== 语音波形动画 ==========
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

    // ========== 重播语音 ==========
    replaySpeech() {
      if (this.isSpeaking) {
        // 停止播放
        this.isSpeaking = false
        this.stopWaveAnimation()
        return
      }
      if (!this.bubbleText) return
      var self = this
      this.isSpeaking = true
      this.startWaveAnimation()
      if (faSession) {
        driveFaText(this.bubbleText).then(function() {
          log('重播驱动成功')
          // 模拟播放时长
          setTimeout(function() {
            self.isSpeaking = false
            self.stopWaveAnimation()
          }, Math.min(self.bubbleText.length * 150, 15000))
        }).catch(function(err) {
          logError('重播驱动失败:', err.message)
          self._fallbackTts(self.bubbleText)
          setTimeout(function() {
            self.isSpeaking = false
            self.stopWaveAnimation()
          }, 3000)
        })
      } else {
        this._fallbackTts(this.bubbleText)
        setTimeout(function() {
          self.isSpeaking = false
          self.stopWaveAnimation()
        }, 3000)
      }
    },

    onVideoError(e) {
      logError('数字人视频错误:', JSON.stringify(e.detail || e))
    },

    cleanupRecording() {
      if (recorderManager) {
        try { recorderManager.stop() } catch (e) {}
      }
    }
  }
}
