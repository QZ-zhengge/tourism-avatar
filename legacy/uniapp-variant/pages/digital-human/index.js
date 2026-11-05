/**
 * 数字人模块 - 连接后端API
 * 后端地址: http://192.0.2.1:50080
 * 讯飞数字人服务代理
 *
 * 文档: README(6).md + http://192.0.2.1:50080/docs
 * 参考: C:/Users/developer/Desktop/index.vue
 */

import config from '@/config/index.js'

// ============================================
// 配置常量
// ============================================
const BACKEND_BASE = config.API_BASE_URL  // http://192.0.2.1:50080
const GUIDE_BASE = config.GUIDE_API_BASE  // 模型API http://192.0.2.1:50081
const XUNFEI_VCN = config.XUNFEI_VCN || 'x4_lingxiaoyu_assist'

// 后端 ASR WebSocket 地址（Vosk 引擎）
const BACKEND_WS_URL = (BACKEND_BASE.startsWith('https') ? 'wss' : 'ws') + '://' + BACKEND_BASE.replace(/https?:\/\//, '') + '/asr/stream'

// 全局状态
let recorderManager = null
let currentSession = ''        // 后端返回的 session
let currentStreamUrl = ''      // 后端返回的 stream_url（rtmp）
let pingTimer = null           // 心跳定时器
let dhStatus = 'idle'         // idle | starting | ready | failed
let conversationStatus = 'idle' // idle | asring | thinking | speaking | error
let asrFinalText = ''
let asrUploadCancelled = false
let isProcessing = false  // 防止并发处理
let driveSeq = 0          // 驱动序号，用于检测过期请求

// ============================================
// 控制台日志工具（统一加 [DH] 前缀，方便筛选）
// ============================================
function log(method, ...args) {
    console.log(`[DH] ${method}:`, ...args)
}
function logError(method, ...args) {
    console.error(`[DH] ${method} [ERROR]:`, ...args)
}
function logWarn(method, ...args) {
    console.warn(`[DH] ${method} [WARN]:`, ...args)
}

// ============================================
// 后端 API 请求工具
// 响应格式: { code: 200, msg: "...", data: {...} }
// ============================================
function requestBackend(method, path, data) {
    return new Promise((resolve, reject) => {
        const url = BACKEND_BASE + path
        log(`[HTTP] ${method} ${url}`, data ? JSON.stringify(data) : '')

        uni.request({
            url: url,
            method: method,
            data: data,
            header: { 'Content-Type': 'application/json' },
            success: (res) => {
                log(`[HTTP] ${method} ${path} 响应:`, JSON.stringify(res.data))

                if (res.statusCode === 200) {
                    const d = res.data
                    // code: 200 或 0 表示成功
                    if (d.code === 200 || d.code === 0) {
                        // 返回 data 字段（会话信息在这里）
                        resolve(d.data || d)
                    } else {
                        logError(`[HTTP] ${method} ${path} 业务错误: code=${d.code}, msg=${d.msg || d.message}`)
                        reject(new Error(d.msg || d.message || `业务错误 code=${d.code}`))
                    }
                } else {
                    logError(`[HTTP] ${method} ${path} HTTP错误: status=${res.statusCode}`)
                    reject(new Error(`HTTP ${res.statusCode}`))
                }
            },
            fail: (err) => {
                logError(`[HTTP] ${method} ${path} 请求失败:`, JSON.stringify(err))
                reject(err)
            }
        })
    })
}

// ============================================
// 1. 启动数字人会话（带重试，最多3次）
// POST /avatar/start
// 返回: { session, stream_url, xfyun_code, xfyun_message }
// ============================================
const MAX_START_RETRIES = 3          // 最大重试次数
const RETRY_DELAY_MS = 2000          // 每次重试间隔（毫秒）
let currentRetryCount = 0            // 当前已重试次数

async function startAvatarSession(avatarId) {
    log('[启动] ========== 开始启动数字人会话 ==========')
    log('[启动] 请求后端:', BACKEND_BASE + '/avatar/start')
    log('[启动] avatar_id:', avatarId || '118801001（默认依丹）')
    log('[启动] 最多重试:', MAX_START_RETRIES, '次')
    dhStatus = 'starting'

    for (let attempt = 1; attempt <= MAX_START_RETRIES; attempt++) {
        currentRetryCount = attempt
        log(`[启动] >>> 第 ${attempt}/${MAX_START_RETRIES} 次尝试...`)

        // 通知前端当前重试进度
        uni.$emit('dhStatusChange', {
            status: 'starting',
            attempt: attempt,
            maxRetries: MAX_START_RETRIES
        })

        try {
            const data = await requestBackend('POST', '/avatar/start', {
                avatar_id: avatarId || '118801001',
                width: 1280,
                height: 720,
                protocol: 'rtmp',    // rtmp = live-player 可播放
                bg_custom: true      // 使用后端配置的自定义背景（.env AVATAR_BG_RES_ID）
            })

            log('[启动] raw data:', JSON.stringify(data))

            // 检查是否有 session
            const session = data.session || data.client_id || ''
            const streamUrl = data.stream_url || ''

            if (session) {
                currentSession = session
                currentStreamUrl = streamUrl

                log('[启动] ✅ 成功! 第', attempt, '次尝试获取到 session:', session)
                log('[启动] ✅ stream_url:', streamUrl)
                log('[启动] 讯飞响应: xfyun_code=' + (data.xfyun_code || 'N/A') + ', xfyun_message=' + (data.xfyun_message || 'N/A'))

                dhStatus = 'ready'

                // 启动心跳保活
                startPingTimer()

                return data
            } else {
                // 后端返回200但没有 session，视为失败
                logWarn(`[启动] 第 ${attempt} 次返回成功但无 session，重试...`)
            }
        } catch (err) {
            logError(`[启动] 第 ${attempt} 次失败:`, err.message)
        }

        // 如果不是最后一次，等待后重试
        if (attempt < MAX_START_RETRIES) {
            log(`[启动] 等待 ${RETRY_DELAY_MS}ms 后重试...`)
            await sleep(RETRY_DELAY_MS)
        }
    }

    // 全部重试都失败
    logError('[启动] ❌ 全部', MAX_START_RETRIES, '次尝试均失败，放弃')
    dhStatus = 'failed'
    throw new Error(`数字人启动失败：已重试 ${MAX_START_RETRIES} 次`)
}

// 延迟工具
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}

// 文本净化：移除讯飞内容审核可能屏蔽的词汇
function sanitizeText(text) {
    if (!text) return text
    // 替换宗教敏感词为中性表述
    const replacements = [
        ['佛教文化', '历史文化'],
        ['佛教圣地', '风景名胜'],
        ['佛教艺术', '传统艺术'],
        ['佛教智慧', '古老智慧'],
        ['藏传佛教', '藏族传统'],
        ['释迦牟尼', '创始人'],
        ['转经筒祈福', '转经筒参观'],
        ['祈福', '游览'],
        ['圣水', '清水'],
        ['禅意', '意境'],
        ['禅茶', '清茶'],
        ['佛法', '智慧'],
        ['佛', ''],
    ]
    let result = text
    for (const [from, to] of replacements) {
        result = result.split(from).join(to)
    }
    // 去除多余空格
    result = result.replace(/\s+/g, ' ').trim()
    return result
}

// ============================================
// 2. 文本驱动 - POST /avatar/drive/text
// 让数字人说话（TTS 播报）
// ============================================
async function driveText(text, vcn) {
    if (!currentSession) {
        logError('[文本驱动] 没有活跃 session，无法驱动！')
        throw new Error('No active session')
    }

    log('[文本驱动] ==========')
    log('[文本驱动] session:', currentSession)
    log('[文本驱动] text:', text)
    log('[文本驱动] vcn:', vcn || XUNFEI_VCN)

    try {
        const res = await requestBackend('POST', '/avatar/drive/text', {
            session: currentSession,
            text: text,
            vcn: vcn || XUNFEI_VCN,
            speed: 50,
            pitch: 50,
            volume: 50
        })

        log('[文本驱动] ✅ 成功! xfyun_code=' + (res.xfyun_code || 'N/A') + ', xfyun_message=' + (res.xfyun_message || 'N/A'))
        return res
    } catch (err) {
        logError('[文本驱动] ❌ 失败:', err.message)
        throw err
    }
}

// ============================================
// 3. 心跳保活 - POST /avatar/ping
// 讯飞服务端 60 秒无请求自动断开，建议每 30 秒调用一次
// ============================================
async function sendPing() {
    if (!currentSession) {
        logWarn('[心跳] 没有 session，跳过')
        return
    }

    try {
        const res = await requestBackend('POST', '/avatar/ping', {
            session: currentSession
        })
        log('[心跳] ✅ 成功 xfyun_code=' + (res.xfyun_code || 'N/A'))
    } catch (err) {
        logError('[心跳] ❌ 失败:', err.message)
    }
}

// 启动心跳定时器（每30秒）
function startPingTimer() {
    stopPingTimer()
    log('[心跳] 启动定时器，每30秒发送一次')
    pingTimer = setInterval(() => {
        log('[心跳] >>> 发送 ping...')
        sendPing()
    }, 30000)
}

function stopPingTimer() {
    if (pingTimer) {
        clearInterval(pingTimer)
        pingTimer = null
        log('[心跳] 定时器已停止')
    }
}

// ============================================
// 4. 停止会话 - POST /avatar/stop
// ============================================
async function stopAvatarSession() {
    if (!currentSession) {
        logWarn('[停止] 没有活跃 session')
        return
    }

    log('[停止] ========== 开始停止会话 ==========')
    log('[停止] session:', currentSession)
    stopPingTimer()

    try {
        await requestBackend('POST', '/avatar/stop', {
            session: currentSession
        })
        log('[停止] ✅ 成功!')
    } catch (err) {
        logError('[停止] ❌ 失败:', err.message)
    } finally {
        currentSession = ''
        currentStreamUrl = ''
        dhStatus = 'idle'
    }
}

// ============================================
// 5. 查询会话状态 - GET /avatar/session/{session}
// ============================================
async function querySessionStatus() {
    if (!currentSession) {
        logWarn('[查询状态] 没有 session')
        return null
    }

    try {
        const data = await requestBackend('GET', `/avatar/session/${currentSession}`)
        log('[查询状态] ✅ 结果:', JSON.stringify(data))
        return data
    } catch (err) {
        logError('[查询状态] ❌ 失败:', err.message)
        return null
    }
}

// ============================================
// 6. 后端 ASR 语音识别（Vosk 引擎，WebSocket 连接）
// 协议：发送 base64 PCM → 接收 final 结果
// ============================================
let asrHandled = false

function connectBackendAsr(audioBuffer) {
    return new Promise(function(resolve, reject) {
        log('[ASR-Vosk] ========== 开始后端 ASR 流程 ==========')
        log('[ASR-Vosk] 音频大小:', audioBuffer.byteLength, '字节')

        asrFinalText = ''
        asrHandled = false
        var timeoutId = null

        // 构建 WebSocket URL（http → ws）
        var wsUrl = BACKEND_WS_URL
        log('[ASR-Vosk] WebSocket URL:', wsUrl)

        var socketTask = uni.connectSocket({
            url: wsUrl,
            success: function() { log('[ASR-Vosk] 连接请求已发送') }
        })

        // 连接成功
        socketTask.onOpen(function() {
            log('[ASR-Vosk] ✅ Socket 已连接')

            // 1. 发送配置帧
            socketTask.send({
                data: JSON.stringify({
                    type: 'config',
                    language: 'zh',
                    format: 'pcm',
                    sampleRate: 16000
                }),
                success: function() {
                    log('[ASR-Vosk] ✅ config 已发送')
                },
                fail: function(err) {
                    logError('[ASR-Vosk] 发送 config 失败:', JSON.stringify(err))
                }
            })

            // 2. 发送音频数据（分块 base64）
            var CHUNK_SIZE = 4096
            var sendOffset = 0

            function doSendAudio() {
                if (sendOffset >= audioBuffer.byteLength) {
                    // 3. 发送结束帧
                    log('[ASR-Vosk] 音频发送完毕，发送 end 帧...')
                    socketTask.send({
                        data: JSON.stringify({ type: 'end' }),
                        success: function() {
                            log('[ASR-Vosk] ✅ end 帧已发送')
                        },
                        fail: function(err) {
                            logError('[ASR-Vosk] 发送 end 帧失败:', JSON.stringify(err))
                        }
                    })

                    // 设置超时（15秒）
                    timeoutId = setTimeout(function() {
                        logWarn('[ASR-Vosk] 15秒超时，关闭')
                        if (asrFinalText === '') {
                            socketTask.close()
                            reject(new Error('ASR 超时'))
                        }
                    }, 15000)

                    return
                }

                var end = Math.min(sendOffset + CHUNK_SIZE, audioBuffer.byteLength)
                var chunk = audioBuffer.slice(sendOffset, end)
                sendOffset = end

                // 转 base64 发送
                var b64 = arrayBufferToBase64(chunk)
                socketTask.send({
                    data: JSON.stringify({ type: 'audio', data: b64 }),
                    success: function() {
                        setTimeout(doSendAudio, 50)
                    },
                    fail: function(err) {
                        logError('[ASR-Vosk] 音频发送失败:', JSON.stringify(err))
                        setTimeout(doSendAudio, 100)
                    }
                })
            }

            doSendAudio()
        })

        // 接收消息
        socketTask.onMessage(function(msgRes) {
            if (!msgRes.data) return
            log('[ASR-Vosk] 收到:', msgRes.data)

            try {
                var msg = JSON.parse(msgRes.data)

                if (msg.type === 'partial') {
                    // 中间结果
                    log('[ASR-Vosk] 中间结果:', msg.text)
                    asrFinalText = msg.text
                    uni.$emit('asrTextUpdate', { text: msg.text })
                } else if (msg.type === 'final') {
                    // 最终结果
                    log('[ASR-Vosk] 🎯 最终结果:', msg.text)
                    asrFinalText = msg.text
                    if (timeoutId) { clearTimeout(timeoutId); timeoutId = null }
                    if (!asrHandled) {
                        asrHandled = true
                        setTimeout(function() {
                            socketTask.close()
                            handleAsrResult(msg.text)
                        }, 500)
                    }
                } else if (msg.type === 'ping') {
                    // ★ 后端心跳保活：回复 pong
                    socketTask.send({
                        data: JSON.stringify({ type: 'pong' }),
                        fail: function(err) { logWarn('[ASR-Vosk] pong发送失败:', JSON.stringify(err)) }
                    })
                } else if (msg.type === 'error') {
                    logError('[ASR-Vosk] 错误:', msg.msg)
                    if (!asrHandled) {
                        asrHandled = true
                        socketTask.close()
                        reject(new Error(msg.msg || 'ASR 错误'))
                    }
                }
            } catch (e) {
                logError('[ASR-Vosk] 解析失败:', e.message)
            }
        })

        // 连接错误
        socketTask.onError(function(err) {
            logError('[ASR-Vosk] ❌ Socket 错误:', JSON.stringify(err))
            if (timeoutId) { clearTimeout(timeoutId); timeoutId = null }
            if (!asrHandled) {
                asrHandled = true
                reject(new Error('ASR WebSocket 连接失败'))
            }
        })

        // 连接关闭
        socketTask.onClose(function() {
            log('[ASR-Vosk] Socket 关闭')
            if (timeoutId) { clearTimeout(timeoutId); timeoutId = null }
            if (!asrHandled) {
                asrHandled = true
                if (asrFinalText) {
                    handleAsrResult(asrFinalText)
                    resolve(asrFinalText)
                } else {
                    conversationStatus = 'idle'
                    uni.$emit('conversationStatusChange', { status: 'idle' })
                    reject(new Error('ASR 无结果'))
                }
            }
        })
    })
}

// 工具：ArrayBuffer → Base64（纯 JS，不依赖 window.btoa）
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

// 新 ASR 函数：直接接收 base64 + 格式（AAC/PCM 均可，后端自动转换）
function connectBackendAsrWithFormat(base64Data, audioFormat) {
    return new Promise(function(resolve, reject) {
        log('[ASR-Vosk] 开始后端 ASR, 格式:', audioFormat, '大小:', base64Data.length, '字节(base64)')

        asrFinalText = ''
        asrHandled = false
        var timeoutId = null

        var socketTask = uni.connectSocket({
            url: BACKEND_WS_URL,
            success: function() { log('[ASR-Vosk] 连接请求已发送') }
        })

        socketTask.onOpen(function() {
            log('[ASR-Vosk] ✅ Socket 已连接')
            // 发送配置帧（格式 = aac，后端 ffmpeg 自动转 PCM）
            socketTask.send({
                data: JSON.stringify({
                    type: 'config',
                    language: 'zh',
                    format: audioFormat || 'mp3',
                    sampleRate: 16000
                }),
                success: function() { log('[ASR-Vosk] ✅ config 已发送') },
                fail: function(err) { logError('[ASR-Vosk] config失败:', JSON.stringify(err)) }
            })

            // 发送音频（分块，避免大包卡后端）
            var CHUNK_WS = 8192  // 每块 base64 长度
            var wsOffset = 0
            function wsSendChunk() {
                if (wsOffset >= base64Data.length) {
                    log('[ASR-Vosk] 音频已发完，发 end...')
                    socketTask.send({
                        data: JSON.stringify({ type: 'end' }),
                        success: function() { log('[ASR-Vosk] ✅ end 已发送') },
                        fail: function(err) { logError('[ASR-Vosk] end失败:', JSON.stringify(err)) }
                    })
                    return
                }
                var end = Math.min(wsOffset + CHUNK_WS, base64Data.length)
                socketTask.send({
                    data: JSON.stringify({ type: 'audio', data: base64Data.substring(wsOffset, end) }),
                    success: function() { wsOffset = end; setTimeout(wsSendChunk, 30) },
                    fail: function(err) { logError('[ASR-Vosk] 发送失败:', JSON.stringify(err)); setTimeout(wsSendChunk, 50) }
                })
            }
            wsSendChunk()

            // 超时 20s
            timeoutId = setTimeout(function() {
                logWarn('[ASR-Vosk] 20秒超时')
                if (asrFinalText === '') { socketTask.close(); reject(new Error('ASR超时')) }
            }, 20000)
        })

        socketTask.onMessage(function(msgRes) {
            if (!msgRes.data) return
            try {
                var msg = JSON.parse(msgRes.data)
                if (msg.type === 'partial') {
                    asrFinalText = msg.text
                    uni.$emit('asrTextUpdate', { text: msg.text })
                } else if (msg.type === 'final') {
                    log('[ASR-Vosk] 🎯 最终:', msg.text)
                    if (timeoutId) { clearTimeout(timeoutId) }
                    if (!asrHandled) {
                        asrHandled = true
                        setTimeout(function() { socketTask.close(); handleAsrResult(msg.text) }, 300)
                    }
                } else if (msg.type === 'ping') {
                    // ★ 回复后端心跳
                    socketTask.send({ data: JSON.stringify({ type: 'pong' }) })
                } else if (msg.type === 'error') {
                    logError('[ASR-Vosk] 错误:', msg.msg)
                    if (!asrHandled) { asrHandled = true; socketTask.close(); reject(new Error(msg.msg)) }
                }
            } catch (e) { logError('[ASR-Vosk] 解析:', e.message) }
        })

        socketTask.onError(function(err) {
            logError('[ASR-Vosk] ❌ 错误:', JSON.stringify(err))
            if (!asrHandled) { asrHandled = true; reject(new Error('ASR连接失败')) }
        })

        socketTask.onClose(function() {
            log('[ASR-Vosk] Socket关闭')
            if (!asrHandled) {
                asrHandled = true
                if (asrFinalText) { handleAsrResult(asrFinalText); resolve(asrFinalText) }
                else { conversationStatus = 'idle'; uni.$emit('conversationStatusChange', {status:'idle'}); reject(new Error('ASR无结果')) }
            }
        })
    })
}

// ============================================
// 处理 ASR 识别结果 → 调用 guide/chat LLM → 用 speech_text 驱动数字人
// ============================================
let guideSessionId = ''  // guide/chat 会话ID

async function handleAsrResult(text) {
    log('[对话] ========== ASR 识别结果 ==========')
    log('[对话] 文字:', text || '（空）')

    // 防止并发处理（快速连点时丢弃旧请求）
    if (isProcessing) {
        logWarn('[对话] 正在处理中，丢弃本次请求')
        return
    }

    if (!text || text.trim() === '') {
        logWarn('[对话] 识别文字为空，跳过')
        conversationStatus = 'idle'
        uni.$emit('conversationStatusChange', { status: 'idle' })
        return
    }

    // 生成或复用会话ID
    if (!guideSessionId) {
        guideSessionId = 'suixing_dh_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6)
    }

    // 清除上次的答案（不清除错误，避免误触）
    uni.$emit('guideAnswerUpdate', { answer: '' })

    // 更新用户说的话到界面
    uni.$emit('asrTextUpdate', { text: text })

    conversationStatus = 'thinking'
    uni.$emit('conversationStatusChange', { status: 'thinking' })

    try {
        isProcessing = true
        const mySeq = ++driveSeq

        // 1. 调用 guide/chat 获取 LLM 回答
        log('[对话] 调用 guide/chat，文本:', text)
        let response
        try {
            response = await callGuideChat(guideSessionId, text)
        } catch (firstErr) {
            logWarn('[对话] guide/chat 第一次失败，重试...')
            response = await callGuideChat(guideSessionId, text)
        }
        log('[对话] LLM 响应:', JSON.stringify(response).substring(0, 200))

        // 如果已被新请求取代，跳过后续处理
        if (mySeq !== driveSeq) {
            logWarn('[对话] 已被新请求取代，跳过')
            isProcessing = false
            return
        }

        // 提取文本和意图
        const answer = response.speech_text || response.answer || '抱歉，暂时无法回答。'
        const intent = response.intent || 'knowledge_qa'

        // 更新AI回答到界面
        uni.$emit('guideAnswerUpdate', { answer: answer })

        // 同步问答记录到 AI 聊天页面
        try {
            const chatKey = 'dh_chat_history'
            const raw = uni.getStorageSync(chatKey) || '[]'
            const history = JSON.parse(raw)
            history.push({
                role: 'user',
                type: 'text',
                content: text,
                time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
            })
            history.push({
                role: 'ai',
                type: 'text',
                content: answer,
                time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
            })
            const trimmed = history.slice(-50)
            uni.setStorageSync(chatKey, JSON.stringify(trimmed))
        } catch (e) {
            logWarn('[对话] 同步历史失败:', e.message)
        }

        // ========== 自动添加到行程 ==========
        // 如果用户说"添加到行程"，解析AI回答并保存
        try {
            var itineraryKeywords = ['添加到行程', '加入行程', '添加到行程中', '记入行程', '规划行程', '帮我安排', '帮我规划', '设计路线', '制定行程']
            var shouldAdd = itineraryKeywords.some(function(kw) { return text.indexOf(kw) >= 0 })
            if (shouldAdd) {
                log('[对话] 检测到行程添加意图，自动解析并保存')
                var itineraryItems = parseItineraryFromText(answer, text)
                if (itineraryItems.length > 0) {
                    var itKey = 'suixing_itinerary'
                    var itRaw = uni.getStorageSync(itKey) || '{}'
                    var itData = JSON.parse(itRaw)
                    itineraryItems.forEach(function(item) {
                        var dk = item.date
                        if (!itData[dk]) itData[dk] = { date: dk, items: [] }
                        var exists = itData[dk].items.some(function(ex) { return ex.title === item.title && ex.time === item.time })
                        if (!exists) itData[dk].items.push(item)
                    })
                    uni.setStorageSync(itKey, JSON.stringify(itData))
                    log('[对话] ✅ 行程已保存，共' + itineraryItems.length + '项')
                }
            }
        } catch (e) {
            logWarn('[对话] 自动添加行程失败:', e.message)
        }

        // 检查是否为 agent_task（订票/订酒店）
        // 检测条件：intent=agent_task，或关键词匹配（订酒店/预订/订餐等）
        const BOOKING_KEYWORDS = ['订酒店', '预订', '订房', '订餐', '订票', '买票', '预约', '订外卖', '定酒店', '住哪', '吃饭', '住宿', '餐厅', '酒店']
        const isBookingIntent = intent === 'agent_task' ||
            BOOKING_KEYWORDS.some(function(kw) { return text.indexOf(kw) >= 0 })

        if (isBookingIntent) {
            log('[对话] 检测到预订意图，显示预订卡片，intent:', intent)
            // 有结构化 card_data 就用，没有就构造空白卡片
            var cardPayload
            if (response.data && response.data.card_data) {
                cardPayload = response.data
            } else {
                // 空白预订卡片，让用户自己填
                cardPayload = {
                    card_data: {
                        type: 'booking_card_blank',
                        answer: answer,
                        userQuery: text
                    }
                }
            }
            uni.$emit('bookingCardUpdate', { data: cardPayload, show: true })
            // 数字人先口头回应
            const driveTextBlank = sanitizeText(answer)
            conversationStatus = 'speaking'
            uni.$emit('conversationStatusChange', { status: 'speaking' })
            try {
                await driveText(driveTextBlank)
            } catch (e) {
                logWarn('[对话] 预订场景 driveText 失败:', e.message)
            }
            conversationStatus = 'idle'
            uni.$emit('conversationStatusChange', { status: 'idle' })
            isProcessing = false
            return
        }

        // 2. 用 speech_text 驱动数字人说话
        const driveTextContent = sanitizeText(answer)
        conversationStatus = 'speaking'
        uni.$emit('conversationStatusChange', { status: 'speaking' })

        log('[对话] 调用 /avatar/drive/text，净化后文本:', driveTextContent)
        try {
            await driveText(driveTextContent)
            log('[对话] ✅ 驱动完成，序号:', mySeq)
        } catch (firstDriveErr) {
            logWarn('[对话] driveText 第一次失败，重试...')
            await driveText(driveTextContent)
            log('[对话] ✅ 驱动完成(重试后)，序号:', mySeq)
        }

        conversationStatus = 'idle'
        uni.$emit('conversationStatusChange', { status: 'idle' })
    } catch (err) {
        logError('[对话] ❌ 处理失败:', err.message)
        // 重试也失败后才显示错误
        conversationStatus = 'error'
        uni.$emit('conversationStatusChange', { status: 'error' })
        uni.$emit('asrError', { desc: err.message })
        // 驱动失败时清除界面上的AI回答（因为数字人没说出口）
        uni.$emit('guideAnswerUpdate', { answer: '' })
    } finally {
        isProcessing = false
    }
}

// 从AI回答中提取行程项（与AI聊天页共享解析逻辑）
function parseItineraryFromText(answer, userQuery) {
    var items = []
    var lines = answer.split('\n')
    var now = new Date()
    var today = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0')

    var timePattern = /(?:^|\s)([0-2]?[0-9]:[0-5][0-9])\s*(?:[-–—：:]+\s*)?(.+)/
    var numberedPattern = /^\s*(?:\d+[\.\)、]\s*)(.+)/
    var timeWordPattern = /^\s*(上午|下午|中午|傍晚|晚上|早晨|清晨|早上)\s*(.+)/
    var timeMap = { '早晨': '07:00', '早上': '07:00', '清晨': '06:00', '上午': '09:00', '中午': '12:00', '下午': '14:00', '傍晚': '17:00', '晚上': '19:00' }

    lines.forEach(function(line) {
        line = line.trim()
        if (!line) return
        if (/^(方案[一二三]|行程|路线|推荐|建议|以下|第[一二三四五六日]天)/.test(line)) return
        if (line.length < 4) return

        var match

        // 模式1: 带具体时间 "9:00 参观大佛"
        match = line.match(timePattern)
        if (match) {
            items.push({
                type: 'route',
                title: match[2].trim().substring(0, 40),
                summary: line.substring(0, 80),
                time: match[1],
                date: today
            })
            return
        }

        // 模式2: 时间词 "上午参观大佛"
        match = line.match(timeWordPattern)
        if (match) {
            items.push({
                type: 'route',
                title: match[2].trim().substring(0, 40),
                summary: line.substring(0, 80),
                time: timeMap[match[1]] || '09:00',
                date: today
            })
            return
        }

        // 模式3: 编号 "1. 参观大佛"
        match = line.match(numberedPattern)
        if (match) {
            var t = match[1].replace(/^(去|参观|游览|逛|玩|前往|到)\s*/, '').trim()
            if (t.length > 2) {
                items.push({
                    type: 'route',
                    title: t.substring(0, 40),
                    summary: line.substring(0, 80),
                    time: '',
                    date: today
                })
            }
        }
    })

    // 如果没有解析出任何项目，将整个回答作为一条路线备忘
    if (items.length === 0) {
        var title = (userQuery || '').replace(/^(请|帮我|帮我把|把)\s*/, '')
            .replace(/添加(到|至)?行程|加入行程|记入行程|规划行程|安排行程/g, '')
            .replace(/\s+/g, '').trim()
        items.push({
            type: 'route',
            title: title || '旅行安排',
            summary: answer.substring(0, 100) + (answer.length > 100 ? '...' : ''),
            time: '',
            date: today
        })
    }

    return items
}

// 调用 guide/chat API
function callGuideChat(sessionId, query) {
    return new Promise((resolve, reject) => {
        uni.request({
            url: GUIDE_BASE + '/api/v1/guide/chat',
            method: 'POST',
            header: { 'Content-Type': 'application/json' },
            data: {
                session_id: sessionId,
                query: query,
                scenic_id: config.SCENIC_ID,
                scenic_name: config.SCENIC_NAME,
                with_memory: true
            },
            success: (res) => {
                if (res.statusCode === 200 && res.data) {
                    // 返回完整响应，包含 intent、card_data 等
                    resolve(res.data)
                } else {
                    logError('[对话] guide/chat 返回异常:', JSON.stringify(res.data))
                    // 降级：直接用识别文字
                    resolve({ answer: query, speech_text: query, intent: 'knowledge_qa' })
                }
            },
            fail: (err) => {
                logError('[对话] guide/chat 请求失败:', JSON.stringify(err))
                // 降级
                resolve({ answer: query, speech_text: query, intent: 'knowledge_qa' })
            }
        })
    })
}

// ============================================
// 录音管理
// 
// 诊断结论（2026-06-02 最终）：
// 所有通过 plus.android 获取 PCM 的方案经过 6 次迭代均失败：
//   1) byte[] → AudioRecord.read: 不识别参数
//   2) ByteBuffer.direct + get(): 数据全零
//   3) ByteBuffer.direct + Base64: 全零
//   4) short[] → AudioRecord.read: -2 ERROR_BAD_VALUE
//   5) reflectArray byte[] → AudioRecord.read: -1
//   6) ByteBuffer.direct + getInt(): 全零
// 最终方案：用 getRecorderManager 录 AAC，直接发后端 ASR WebSocket
//（后端 ffmpeg 自动转 PCM）
// ============================================
function initRecorder() {
    // #ifdef MP-WEIXIN
    recorderManager = wx.getRecorderManager()
    // #endif
    // #ifndef MP-WEIXIN
    if (!recorderManager) recorderManager = uni.getRecorderManager()
    // #endif

    recorderManager.onStart(function() {
        log('[Recorder] ✅ 录音开始')
    })

    recorderManager.onStop(function(res) {
        log('[Recorder] 录音停止:', res.tempFilePath)
        if (!asrUploadCancelled) {
            readAudioAndSend(res.tempFilePath)
        } else {
            asrUploadCancelled = false
        }
    })

    recorderManager.onError(function(e) {
        logError('[Recorder] ❌ 错误:', JSON.stringify(e))
    })
}

function startRecording() {
    log('[Recorder] startRecording()')
    asrFinalText = ''
    asrUploadCancelled = false
    conversationStatus = 'asring'
    uni.$emit('conversationStatusChange', { status: 'asring' })

    recorderManager.start({
        duration: 60000,
        format: 'mp3',
        sampleRate: 16000,
        numberOfChannels: 1
    })
}

function stopRecording() {
    log('[Recorder] stopRecording()')
    if (recorderManager) recorderManager.stop()
}

// 读取音频文件 → 发后端 ASR
function readAudioAndSend(filePath) {
    // #ifdef APP-PLUS
    plus.io.resolveLocalFileSystemURL(filePath, function(entry) {
        entry.file(function(file) {
            log('[Recorder] 文件:', file.size, '字节')
            var reader = new plus.io.FileReader()
            reader.onloadend = function(e) {
                var dataUrl = e.target.result || ''
                var b64 = dataUrl.indexOf(',') >= 0 ? dataUrl.split(',')[1] : dataUrl
                if (!b64 || b64.length === 0) {
                    logError('[Recorder] 空文件')
                    conversationStatus = 'idle'
                    uni.$emit('conversationStatusChange', { status: 'idle' })
                    return
                }
                connectBackendAsrWithFormat(b64, 'mp3').catch(function(err) {
                    logError('[Recorder] ASR失败:', err.message)
                    conversationStatus = 'idle'
                    uni.$emit('conversationStatusChange', { status: 'idle' })
                })
            }
            reader.onerror = function(err) {
                logError('[Recorder] 读取失败:', JSON.stringify(err))
                conversationStatus = 'error'
            }
            reader.readAsDataURL(file)
        })
    })
    // #endif

    // #ifdef MP-WEIXIN
    var fs = wx.getFileSystemManager()
    fs.readFile({
        filePath: filePath,
        success: function(fileRes) {
            var ab = fileRes.data
            if (!ab || ab.byteLength === 0) {
                logError('[Recorder] 空文件')
                conversationStatus = 'idle'
                return
            }
            var b64 = arrayBufferToBase64(ab)
            connectBackendAsrWithFormat(b64, 'mp3').catch(function(err) {
                logError('[Recorder] ASR失败:', err.message)
                conversationStatus = 'idle'
            })
        },
        fail: function(err) {
            logError('[Recorder] 读取失败:', JSON.stringify(err))
            conversationStatus = 'error'
        }
    })
    // #endif

    // #ifdef H5
    logWarn('[Recorder] H5不支持')
    conversationStatus = 'idle'
    // #endif
}
// ============================================
// 读取本地 PCM 文件并发送到 ASR
// ============================================
function base64ToArrayBuffer(base64) {
    const map = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
    const lookup = new Uint8Array(256)
    for (let i = 0; i < map.length; i++) {
        lookup[map.charCodeAt(i)] = i
    }
    let len = base64.length
    if (base64[len - 1] === '=') len--
    if (base64[len - 1] === '=') len--
    const bufferLength = len * 0.75
    const arrayBuffer = new ArrayBuffer(bufferLength)
    const bytes = new Uint8Array(arrayBuffer)
    let p = 0
    for (let i = 0; i < base64.length; i += 4) {
        const encoded1 = lookup[base64.charCodeAt(i)]
        const encoded2 = lookup[base64.charCodeAt(i + 1)]
        const encoded3 = lookup[base64.charCodeAt(i + 2)]
        const encoded4 = lookup[base64.charCodeAt(i + 3)]
        bytes[p++] = (encoded1 << 2) | (encoded2 >> 4)
        if (base64.charAt(i + 2) !== '=') {
            bytes[p++] = ((encoded2 & 15) << 4) | (encoded3 >> 2)
        }
        if (base64.charAt(i + 3) !== '=') {
            bytes[p++] = ((encoded3 & 3) << 6) | encoded4
        }
    }
    return arrayBuffer
}

function uploadAudioFileToAsr(filePath) {
    log('[ASR] 上传文件:', filePath)

    // #ifdef APP-PLUS
    // readAsArrayBuffer 在某些环境中会挂起永不触发 onloadend，故用 readAsDataURL
    plus.io.resolveLocalFileSystemURL(filePath, (entry) => {
        entry.file((file) => {
            log('[ASR] 文件对象获取成功，size:', file.size, '字节，预期时长约', (file.size / 32000).toFixed(1), '秒(16kHz)')

            if (file.size === 0) {
                logError('[ASR] 文件为空，放弃')
                conversationStatus = 'idle'
                uni.$emit('conversationStatusChange', { status: 'idle' })
                return
            }

            const reader = new plus.io.FileReader()
            reader.onloadend = (e) => {
                const dataUrl = e.target.result || ''
                const base64Str = dataUrl.indexOf(',') >= 0 ? dataUrl.split(',')[1] : dataUrl
                log('[ASR] Base64 长度:', base64Str ? base64Str.length : 0)

                if (!base64Str || base64Str.length === 0) {
                    logError('[ASR] 文件为空')
                    conversationStatus = 'idle'
                    uni.$emit('conversationStatusChange', { status: 'idle' })
                    return
                }

                const audioBuffer = base64ToArrayBuffer(base64Str)
                log('[ASR] PCM 大小:', audioBuffer.byteLength, '字节，预期时长约', (audioBuffer.byteLength / 32000).toFixed(1), '秒(16kHz)')

                if (audioBuffer.byteLength < 16000) {
                    logWarn('[ASR] 音频过短(<0.5秒)，可能录到了静音')
                }

                connectBackendAsr(audioBuffer).catch(function(err) {
                    logError('[ASR] 后端ASR失败:', err.message)
                    conversationStatus = 'idle'
                    uni.$emit('conversationStatusChange', { status: 'idle' })
                })
            }
            reader.onerror = (err) => {
                logError('[ASR] FileReader 失败:', JSON.stringify(err))
                conversationStatus = 'error'
                uni.$emit('conversationStatusChange', { status: 'idle' })
            }
            reader.readAsDataURL(file)
        }, (err) => {
            logError('[ASR] 获取文件对象失败:', JSON.stringify(err))
            conversationStatus = 'error'
            uni.$emit('conversationStatusChange', { status: 'idle' })
        })
    }, (err) => {
        logError('[ASR] 解析路径失败:', JSON.stringify(err))
        conversationStatus = 'error'
        uni.$emit('conversationStatusChange', { status: 'idle' })
    })
    // #endif

    // #ifdef MP-WEIXIN
    const fs = wx.getFileSystemManager()
    fs.readFile({
        filePath: filePath,
        success: (fileRes) => {
            const audioBuffer = fileRes.data
            if (!audioBuffer || audioBuffer.byteLength === 0) {
                logError('[ASR] 文件为空')
                conversationStatus = 'idle'
                return
            }
            log('[ASR] 文件大小:', audioBuffer.byteLength, '字节')
            connectBackendAsr(audioBuffer).catch(function(err) {
                logError('[ASR] 后端ASR失败:', err.message)
                conversationStatus = 'idle'
                uni.$emit('conversationStatusChange', { status: 'idle' })
            })
        },
        fail: (err) => {
            logError('[ASR] 读取失败:', JSON.stringify(err))
            conversationStatus = 'error'
        }
    })
    // #endif

    // #ifdef H5
    logWarn('[ASR] H5 不支持录音')
    conversationStatus = 'idle'
    // #endif
}

// ============================================
// Vue 组件导出
// ============================================
export default {
    data() {
        return {
            isRecording: false,
            conversationStatus: 'idle', // idle | asring | thinking | speaking | error
            userText: '',                // 用户说的话（ASR 识别结果）
            lastAnswer: '',              // AI 回答
            lastError: '',
            navTop: 0,                  // 状态栏高度
            navHeight: 88,              // 导航栏总高 = statusBarHeight + 48
            navTarget: null,
            dhStatus: 'idle',           // idle | starting | ready | failed
            dhVideoUrl: '',             // 数字人直播流地址（rtmp）
            captionText: '',             // ASR 实时识别文字
            retryCount: 0,              // 当前启动重试次数
            maxRetries: 3,              // 最大重试次数
            bookingCardData: null,       // 预订卡片数据
            bookingCardAnswer: '',        // 预订卡片文字说明
            conversationLog: [],          // 对话历史（按序显示）
            bcName: '',
            bcCheckIn: '',
            bcCheckOut: '',
            bcRoomType: '',
            bcPrice: '',
            bcNote: '',
            bcRating: '',
            bcTag: '',
            bcAddress: ''
        }
    },

    onLoad() {
        log('[DH] ========== onLoad 页面加载 ==========')
        log('[DH] 后端地址:', BACKEND_BASE)
        log('[DH] 讯飞 VCN:', XUNFEI_VCN)

        // 计算导航栏高度（确保返回键不被遮挡）
        const systemInfo = uni.getSystemInfoSync()
        this.navTop = systemInfo.statusBarHeight || 0
        this.navHeight = (systemInfo.statusBarHeight || 0) + 48

        // 初始化录音
        initRecorder()

        // 启动数字人会话
        this.startAvatar()

        // 同步适老化模式到后端
        this.syncElderlyMode()

        // 注册事件监听
        uni.$on('asrTextUpdate', (res) => {
            log('[DH] 收到 asrTextUpdate:', res.text)
            this.userText = res.text
            // 追加到对话历史
            this.conversationLog.push({ role: 'user', text: res.text })
        })

        uni.$on('conversationStatusChange', (res) => {
            log('[DH] 收到 conversationStatusChange:', res.status)
            this.conversationStatus = res.status
        })

        uni.$on('asrError', (res) => {
            if (!res.desc) return  // 忽略空错误
            logError('[DH] 收到 asrError:', JSON.stringify(res))
            this.lastError = res.desc
        })

        // 监听数字人状态变化
        uni.$on('dhStatusChange', (res) => {
            log('[DH] 收到 dhStatusChange:', JSON.stringify(res))
            this.dhStatus = res.status
            // 接收重试进度
            if (res.attempt !== undefined) {
                this.retryCount = res.attempt
            }
        })

        // 监听 guide/chat LLM 回答（仅记录，文字版只在 AI 问答页显示）
        uni.$on('guideAnswerUpdate', (res) => {
            log('[DH] 收到 guideAnswerUpdate:', res.answer)
            this.lastAnswer = res.answer
        })

        // 监听预订卡片事件
        uni.$on('bookingCardUpdate', (res) => {
            log('[DH] 收到 bookingCardUpdate')
            if (res.show && res.data) {
                this.bookingCardData = res.data
                this.bookingCardAnswer = res.data.card_data ? res.data.card_data.summary || res.data.card_data.answer || '' : ''
                // 重置并预填充字段
                this.bcName = ''
                this.bcCheckIn = ''
                this.bcCheckOut = ''
                this.bcRoomType = ''
                this.bcPrice = ''
                this.bcNote = ''
                this.bcRating = ''
                this.bcTag = ''
                this.bcAddress = ''
                if (res.data.card_data) {
                    var cd = res.data.card_data
                    this.bcName = cd.title || ''
                    this.bcPrice = cd.price || ''
                    this.bcRating = cd.rating || ''
                    this.bcTag = cd.tag || cd.category || ''
                    this.bcAddress = cd.address || cd.distance || ''
                    this.bcNote = cd.summary || ''
                }
            } else {
                this.bookingCardData = null
                this.bookingCardAnswer = ''
            }
        })
    },

    onUnload() {
        log('[DH] ========== onUnload 页面卸载 ==========')
        stopPingTimer()
        stopAvatarSession()

        // 注销事件
        uni.$off('asrTextUpdate')
        uni.$off('conversationStatusChange')
        uni.$off('asrError')
        uni.$off('dhStatusChange')
        uni.$off('guideAnswerUpdate')
        uni.$off('bookingCardUpdate')
    },

    methods: {
        // 启动数字人
        async startAvatar() {
            log('[DH] startAvatar() 开始')
            this.dhStatus = 'starting'
            this.lastError = ''
            this.dhVideoUrl = ''
            this.retryCount = 0

            try {
                const data = await startAvatarSession('118801001')
                log('[DH] startAvatar() 成功, data:', JSON.stringify(data))

                this.dhStatus = 'ready'
                this.retryCount = 0
                this.dhVideoUrl = data.stream_url || ''

                log('[DH] stream_url:', this.dhVideoUrl)
                log('[DH] dhStatus:', this.dhStatus, ', dhVideoUrl:', this.dhVideoUrl ? '已设置' : '空')

                uni.$emit('dhStatusChange', { status: 'ready' })

                // $nextTick 后手动触发播放（部分设备 autoplay 不生效）
                this.$nextTick(() => {
                    log('[DH] 尝试手动触发 video 播放...')
                    try {
                        // #ifdef APP-PLUS
                        const videoCtx = uni.createVideoContext('dhVideoPlayer', this)
                        // #endif
                        // #ifdef MP-WEIXIN
                        const videoCtx = uni.createLivePlayerContext('dhLivePlayer', this)
                        // #endif
                        // #ifdef H5
                        const videoCtx = uni.createVideoContext('dhVideoPlayer', this)
                        // #endif
                        if (videoCtx && videoCtx.play) {
                            videoCtx.play()
                            log('[DH] ✅ videoCtx.play() 已调用')
                        } else {
                            logWarn('[DH] videoCtx 为空或没有 play 方法')
                        }
                    } catch (e) {
                        logError('[DH] 手动播放异常:', e.message || JSON.stringify(e))
                    }
                })
            } catch (err) {
                logError('[DH] startAvatar() 失败:', err.message)
                this.dhStatus = 'failed'

                this.lastError = '数字人展示失败：多次尝试后仍无法连接，请检查网络后重试'
                uni.$emit('dhStatusChange', { status: 'failed' })
            }
        },

        // 同步适老化模式到后端
        syncElderlyMode() {
            var aging = uni.getStorageSync('agingMode')
            if (aging) {
                uni.request({
                    url: GUIDE_BASE + '/api/v1/guide/elderly-mode',
                    method: 'POST',
                    header: { 'Content-Type': 'application/json' },
                    data: { enabled: true, session_id: currentSession },
                    fail: function() { logWarn('[DH] 适老化设置失败') }
                })
            }
        },

        // 返回
        navigateBack() {
            log('[DH] navigateBack()')
            // 从数字人返回时回到 AI 问答界面
            uni.switchTab({ url: '/pages/ai-chat/index' })
        },

        // 清除导航目标
        clearNavTarget() {
            this.navTarget = null
        },

        // 按下麦克风（开始录音）
        onMicPress() {
            log('[DH] onMicPress()')
            if (this.dhStatus !== 'ready') {
                logWarn('[DH] 数字人未就绪，忽略录音')
                uni.showToast({ title: '数字人未就绪', icon: 'none' })
                return
            }
            this.isRecording = true
            this.lastError = ''
            startRecording()
        },

        // 松开麦克风（停止录音，发送识别）
        onMicRelease() {
            log('[DH] onMicRelease()')
            this.isRecording = false
            stopRecording()
        },

        // 取消录音
        onMicCancel() {
            log('[DH] onMicCancel()')
            this.isRecording = false
            asrUploadCancelled = true
            if (recorderManager) recorderManager.stop()
            this.conversationStatus = 'idle'
        },

        // 点击相机（重试启动）
        onCameraTap() {
            log('[DH] onCameraTap() - 重试启动数字人')
            this.startAvatar()
        },

        // live-player 状态变化（仅 MP-WEIXIN）
        onLiveStateChange(e) {
            log('[DH] live-player 状态:', JSON.stringify(e.detail))
            const code = e.detail && e.detail.code
            if (code === 2001) {
                log('[DH] live-player 已连接')
            } else if (code === 2004) {
                log('[DH] live-player 开始播放')
            } else if (code === -2301) {
                logError('[DH] live-player 连接断开')
            } else if (code < 0) {
                logError('[DH] live-player 错误, code:', code)
            }
        },

        // video 事件（APP-PLUS / H5）
        onVideoPlay(e) {
            log('[DH] ✅ video 开始播放!')
        },
        onVideoLoaded(e) {
            log('[DH] video metadata loaded:', JSON.stringify(e.detail))
        },
        onVideoWaiting(e) {
            log('[DH] video 缓冲中...')
        },

        // 播放错误
        onVideoError(e) {
            logError('[DH] 播放错误:', JSON.stringify(e.detail || e))
            this.lastError = '播放错误: ' + (e.detail?.errMsg || JSON.stringify(e.detail || e))
        },

        // ========== 预订卡片方法 - 千问风格 ==========

        // 关闭预订卡片
        closeBookingCard() {
            this.bookingCardData = null
            this.bookingCardAnswer = ''
            this.bcName = ''
            this.bcCheckIn = ''
            this.bcCheckOut = ''
            this.bcRoomType = ''
            this.bcPrice = ''
            this.bcNote = ''
            this.bcRating = ''
            this.bcTag = ''
            this.bcAddress = ''
        },

        // 确认预订并添加到行程
        confirmBooking() {
            if (!this.bcName) {
                uni.showToast({ title: '请填写酒店名称', icon: 'none' })
                return
            }
            var now = new Date()
            var dateStr = this.bcCheckIn || (
                now.getFullYear() + '-' + String(now.getMonth()+1).padStart(2,'0') + '-' + String(now.getDate()).padStart(2,'0')
            )
            var timeStr = String(now.getHours()).padStart(2,'0') + ':' + String(now.getMinutes()).padStart(2,'0')

            var summaryParts = []
            if (this.bcRoomType) summaryParts.push(this.bcRoomType)
            if (this.bcCheckOut) summaryParts.push('离店: ' + this.bcCheckOut)
            if (this.bcPrice) summaryParts.push(this.bcPrice)
            if (this.bcNote) summaryParts.push(this.bcNote)

            var entry = {
                type: 'booking',
                kind: 'hotel',
                title: this.bcName,
                price: this.bcPrice,
                rating: this.bcRating,
                summary: summaryParts.join(' · '),
                time: timeStr,
                date: dateStr,
                checkIn: this.bcCheckIn,
                checkOut: this.bcCheckOut,
                roomType: this.bcRoomType,
                note: this.bcNote
            }

            this.saveToItinerary(dateStr, entry)
            this.closeBookingCard()
            uni.showToast({ title: '已添加到行程', icon: 'success' })
        },

        // 保存到行程（localStorage）
        saveToItinerary(dateStr, entry) {
            try {
                var key = 'suixing_itinerary'
                var raw = uni.getStorageSync(key) || '{}'
                var data = JSON.parse(raw)

                if (!data[dateStr]) {
                    data[dateStr] = { date: dateStr, items: [] }
                }
                data[dateStr].items.push(entry)

                uni.setStorageSync(key, JSON.stringify(data))
                log('[DH] 行程已保存:', dateStr, entry.title)
            } catch(e) {
                logWarn('[DH] 保存行程失败:', e.message)
            }
        },

        // 卡片封面颜色
        getBcColor(title) {
            var colors = [
                'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
                'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
                'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
                'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
                'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)'
            ]
            var hash = 0
            var s = title || '?'
            for (var i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash)
            return colors[Math.abs(hash) % colors.length]
        },

        getBcLetter(title) {
            if (!title) return '?'
            var first = title.trim().charAt(0)
            return /[\u4e00-\u9fa5]/.test(first) ? first : first.toUpperCase()
        }
    }
}
