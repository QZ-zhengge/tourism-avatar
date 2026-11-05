// ============================================================
//  讯飞数字人 WebSocket 工具类 (virtual-man.xfyun.cn平台)
//
//  平台：virtual-man.xfyun.cn
//  WebSocket地址：wss://avatar.cn-huadong-1.xf-yun.com/v1/interact
//
//  流程：直接WebSocket连接 → 发送start → 获取stream_url → 播放
// ============================================================

// TextEncoder polyfill（uni-app基座不支持内置TextEncoder）
if (typeof TextEncoder === 'undefined') {
  global.TextEncoder = class {
    constructor(encoding = 'utf-8') {
      this.encoding = encoding
    }
    encode(input) {
      const bytes = []
      for (let i = 0; i < input.length; i++) {
        const c = input.charCodeAt(i)
        if (c < 0x80) bytes.push(c)
        else if (c < 0x800) { bytes.push(0xc0 | (c >> 6)); bytes.push(0x80 | (c & 0x3f)) }
        else if (c < 0xd800) { bytes.push(0xe0 | (c >> 12)); bytes.push(0x80 | ((c >> 6) & 0x3f)); bytes.push(0x80 | (c & 0x3f)) }
        else { const cp = c; i++; const c2 = input.charCodeAt(i); bytes.push(0xf0 | ((cp - 0xd800) >> 10)); bytes.push(0x80 | (((cp - 0xd800) >> 4) & 0x3f)); bytes.push(0x80 | ((c2 - 0xdc00) & 0x3f)); bytes.push(0x80 | (c2 & 0x3f)) }
      }
      return new Uint8Array(bytes)
    }
  }
}

// 讯飞虚拟人 WebSocket 地址（avatar平台）
const XUNFEI_WS_HOST = 'avatar.cn-huadong-1.xf-yun.com'
// 讯飞文档服务地址: /v1/interact_lips_flow
// Java SDK使用: /v1/interact
// 两个都尝试，优先用文档里的
const XUNFEI_WS_PATH = '/v1/interact_lips_flow'
const XUNFEI_WS_URL = `wss://${XUNFEI_WS_HOST}${XUNFEI_WS_PATH}`

// 认证信息
let appId = ''
let apiKey = ''
let apiSecret = ''
let sceneId = ''

// 数字人形象配置（默认值，可由调用方通过connect options覆盖）
let AVATAR_ID = '111260002'  // 雨菲形象ID
let VCN = 'xiaoyan'          // 发音人：雨菲

// WebSocket 实例
let ws = null
let wsCallback = null
let pingInterval = null
let isConnected = false

// -----------------------------------------------
//  纯JS HMAC-SHA256 实现
// -----------------------------------------------

var uint8Array = Uint8Array;
var uint32Array = Uint32Array;
var pow = Math.pow;

var DEFAULT_STATE = new uint32Array(8);
var ROUND_CONSTANTS = [];
var M = new uint32Array(64);

function getFractionalBits(n) {
    return ((n - (n | 0)) * pow(2, 32)) | 0;
}

var n = 2, nPrime = 0;
while (nPrime < 64) {
    var isPrime = true;
    for (var factor = 2; factor <= n / 2; factor++) {
        if (n % factor === 0) {
            isPrime = false;
        }
    }
    if (isPrime) {
        if (nPrime < 8) {
            DEFAULT_STATE[nPrime] = getFractionalBits(pow(n, 1 / 2));
        }
        ROUND_CONSTANTS[nPrime] = getFractionalBits(pow(n, 1 / 3));
        nPrime++;
    }
    n++;
}

var LittleEndian = !!new uint8Array(new uint32Array([1]).buffer)[0];

function convertEndian(word) {
    if (LittleEndian) {
        return (
            (word >>> 24) |
            (((word >>> 16) & 0xff) << 8) |
            ((word & 0xff00) << 8) |
            (word << 24)
        );
    }
    else {
        return word;
    }
}

function rightRotate(word, bits) {
    return (word >>> bits) | (word << (32 - bits));
}

function sha256(data) {
    var STATE = DEFAULT_STATE.slice();
    var length = data.length;

    var bitLength = length * 8;
    var newBitLength = (512 - ((bitLength + 64) % 512) - 1) + bitLength + 65;

    var bytes = new uint8Array(newBitLength / 8);
    var words = new uint32Array(bytes.buffer);

    bytes.set(data, 0);
    bytes[length] = 0b10000000;
    words[words.length - 1] = convertEndian(bitLength);

    for (var block = 0; block < newBitLength / 32; block += 16) {
        var workingState = STATE.slice();

        for (var round = 0; round < 64; round++) {
            var MRound;
            if (round < 16) {
                MRound = convertEndian(words[block + round]);
            }
            else {
                var gamma0x = M[round - 15];
                var gamma1x = M[round - 2];
                MRound =
                    M[round - 7] + M[round - 16] + (
                        rightRotate(gamma0x, 7) ^
                        rightRotate(gamma0x, 18) ^
                        (gamma0x >>> 3)
                    ) + (
                        rightRotate(gamma1x, 17) ^
                        rightRotate(gamma1x, 19) ^
                        (gamma1x >>> 10)
                    );
            }

            M[round] = MRound |= 0;

            var t1 =
                (
                    rightRotate(workingState[4], 6) ^
                    rightRotate(workingState[4], 11) ^
                    rightRotate(workingState[4], 25)
                ) +
                (
                    (workingState[4] & workingState[5]) ^
                    (~workingState[4] & workingState[6])
                ) + workingState[7] + MRound + ROUND_CONSTANTS[round];
            var t2 =
                (
                    rightRotate(workingState[0], 2) ^
                    rightRotate(workingState[0], 13) ^
                    rightRotate(workingState[0], 22)
                ) +
                (
                    (workingState[0] & workingState[1]) ^
                    (workingState[2] & (workingState[0] ^ workingState[1]))
                );

            for (var i = 7; i > 0; i--) {
                workingState[i] = workingState[i - 1];
            }
            workingState[0] = (t1 + t2) | 0;
            workingState[4] = (workingState[4] + t1) | 0;
        }

        for (round = 0; round < 8; round++) {
            STATE[round] = (STATE[round] + workingState[round]) | 0;
        }
    }

    return new uint8Array(new uint32Array(
        STATE.map(function (val) { return convertEndian(val); })
    ).buffer);
}

function hmac(key, data) {
    if (key.length > 64)
        key = sha256(key);

    if (key.length < 64) {
        const tmp = new Uint8Array(64);
        tmp.set(key, 0);
        key = tmp;
    }

    var innerKey = new uint8Array(64);
    var outerKey = new uint8Array(64);
    for (var i = 0; i < 64; i++) {
        innerKey[i] = 0x36 ^ key[i];
        outerKey[i] = 0x5c ^ key[i];
    }

    var msg = new uint8Array(data.length + 64);
    msg.set(innerKey, 0);
    msg.set(data, 64);

    var result = new uint8Array(64 + 32);
    result.set(outerKey, 0);
    result.set(sha256(msg), 64);

    return sha256(result);
}

const encoder = new TextEncoder('utf-8');

function sign(inputKey, inputData) {
    const key = typeof inputKey === 'string' ? encoder.encode(inputKey) : inputKey;
    const data = typeof inputData === 'string' ? encoder.encode(inputData) : inputData;
    return hmac(key, data);
}

function hex(bin) {
    return bin.reduce((acc, val) =>
        acc + ('00' + val.toString(16)).substr(-2), '');
}

// -----------------------------------------------
//  Base64编码（支持字符串和Uint8Array）
// -----------------------------------------------
function base64Encode(input) {
    const base64Chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
    let result = ''
    const bytes = []
    
    // 统一转为字节数组
    if (typeof input === 'string') {
        for (let i = 0; i < input.length; i++) {
            bytes.push(input.charCodeAt(i) & 0xff)  // 只取低8位！
        }
    } else if (input instanceof Uint8Array) {
        for (let i = 0; i < input.length; i++) {
            bytes.push(input[i])
        }
    }

    let i = 0
    while (i < bytes.length) {
        const b1 = bytes[i++] || 0
        const b2 = bytes[i++] || 0
        const b3 = bytes[i++] || 0

        result += base64Chars[b1 >> 2]
        result += base64Chars[((b1 & 0x3) << 4) | (b2 >> 4)]
        result += (i - 1 < bytes.length ? base64Chars[((b2 & 0xf) << 2) | (b3 >> 6)] : '=')
        result += (i < bytes.length ? base64Chars[b3 & 0x3f] : '=')
    }

    return result
}

// -----------------------------------------------
//  生成WebSocket鉴权参数
// -----------------------------------------------
function generateAuthParams() {
    const now = new Date()
    const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    const dateStr = `${days[now.getUTCDay()]}, ${String(now.getUTCDate()).padStart(2, '0')} ${months[now.getUTCMonth()]} ${now.getUTCFullYear()} ${String(now.getUTCHours()).padStart(2, '0')}:${String(now.getUTCMinutes()).padStart(2, '0')}:${String(now.getUTCSeconds()).padStart(2, '0')} GMT`

    // 签名原文（RFC 3339格式）
    const signatureOrigin = `host: ${XUNFEI_WS_HOST}\ndate: ${dateStr}\nGET ${XUNFEI_WS_PATH} HTTP/1.1`
    console.log('[XunfeiWS] 签名原文:', signatureOrigin.replace(/\n/g, '\\n'))

    // 1. 用HMAC-SHA256计算签名，得到字节数组
    const signatureBytes = sign(apiSecret, signatureOrigin)
    
    // 2. 签名字节数组base64编码（直接传入Uint8Array，避免字符串转换导致的高位问题）
    const signature = base64Encode(signatureBytes)
    
    // 3. 构造authorization字符串
    const authorizationOrigin = `api_key="${apiKey}", algorithm="hmac-sha256", headers="host date request-line", signature="${signature}"`
    
    // 4. authorization字符串整体base64编码
    const authorization = base64Encode(authorizationOrigin)

    console.log('[XunfeiWS] signature (base64):', signature)
    console.log('[XunfeiWS] authorization (base64):', authorization.substring(0, 50) + '...')

    return {
        date: dateStr,
        authorization: authorization
    }
}

// -----------------------------------------------
//  连接讯飞WebSocket（直接连接，不需要HTTP启动）
// -----------------------------------------------
function connect(options, callback) {
    appId = options.appId || ''
    apiKey = options.apiKey || ''
    apiSecret = options.apiSecret || ''
    sceneId = options.sceneId || ''
    AVATAR_ID = options.avatarId || AVATAR_ID
    VCN = options.vcn || VCN
    wsCallback = callback

    if (!appId || !apiKey || !apiSecret) {
        console.error('[XunfeiWS] 缺少认证参数')
        wsCallback && wsCallback.onError && wsCallback.onError({ message: '缺少认证参数' })
        return false
    }

    console.log('[XunfeiWS] ===== 连接讯飞WebSocket =====')
    console.log('[XunfeiWS] WebSocket URL:', XUNFEI_WS_URL)
    console.log('[XunfeiWS] appId:', appId)
    console.log('[XunfeiWS] apiKey前8位:', apiKey.substring(0, 8) + '...')
    console.log('[XunfeiWS] apiSecret前8位:', apiSecret.substring(0, 8) + '...')
    console.log('[XunfeiWS] apiSecret长度:', apiSecret.length)
    console.log('[XunfeiWS] avatarId:', AVATAR_ID)
    console.log('[XunfeiWS] sceneId:', sceneId)

    // 生成鉴权参数
    const authParams = generateAuthParams()

    // 构造WebSocket URL
    const wsUrl = `${XUNFEI_WS_URL}?authorization=${encodeURIComponent(authParams.authorization)}&date=${encodeURIComponent(authParams.date)}&host=${encodeURIComponent(XUNFEI_WS_HOST)}&appid=${appId}`

    console.log('[XunfeiWS] 完整WebSocket URL:', wsUrl)
    console.log('[XunfeiWS] ===== 尝试建立WebSocket连接 =====')

    try {
        ws = uni.connectSocket({
            url: wsUrl,
            success: (res) => {
                console.log('[XunfeiWS] WebSocket连接请求成功', res)
            },
            fail: (err) => {
                console.error('[XunfeiWS] WebSocket连接失败', err)
                wsCallback && wsCallback.onError && wsCallback.onError(err)
            }
        })

        // 监听打开
        ws.onOpen(() => {
            console.log('[XunfeiWS] WebSocket已打开，发送start协议')
            isConnected = true
            wsCallback && wsCallback.onOpen && wsCallback.onOpen()
            // 连接成功后立即发送start协议
            try {
                sendStart()
            } catch (e) {
                console.error('[XunfeiWS] sendStart执行异常:', e)
            }
        })

        // 监听消息
        ws.onMessage((res) => {
            console.log('[XunfeiWS] 收到消息:', res.data)
            try {
                const data = JSON.parse(res.data)
                handleMessage(data)
            } catch (e) {
                console.error('[XunfeiWS] 消息解析失败', e)
            }
        })

        // 监听关闭
        ws.onClose((res) => {
            console.log('[XunfeiWS] WebSocket已关闭', JSON.stringify(res))
            console.log('[XunfeiWS] close code:', res.code, 'reason:', res.reason)
            isConnected = false
            stopPing()
            wsCallback && wsCallback.onClose && wsCallback.onClose(res)
        })

        // 监听错误
        ws.onError((err) => {
            const errStr = JSON.stringify(err)
            console.error('[XunfeiWS] WebSocket错误详情, err类型:', typeof err, '内容:', errStr)
            console.error('[XunfeiWS] 错误keys:', Object.keys(err))
            console.error('[XunfeiWS] err.errMsg:', err.errMsg, 'err.message:', err.message)
            isConnected = false
            stopPing()
            // 如果err是空对象{}，给出更友好的提示
            const errorMsg = err.errMsg || err.message || (Object.keys(err).length === 0 ? '服务器主动断开连接（检查认证信息是否正确）' : errStr)
            wsCallback && wsCallback.onError && wsCallback.onError({
                code: -1,
                message: errorMsg
            })
        })

    } catch (e) {
        console.error('[XunfeiWS] WebSocket连接异常', e)
        return false
    }

    return true
}

// -----------------------------------------------
//  处理收到的消息
// -----------------------------------------------
function handleMessage(data) {
    const header = data.header || {}
    const payload = data.payload || {}
    const ctrl = header.ctrl || ''
    const code = header.code

    console.log('[XunfeiWS] 消息类型:', ctrl, '| code:', code)

    if (code !== 0 && code !== '0' && code !== 200) {
        console.error('[XunfeiWS] 错误:', header.message)
        wsCallback && wsCallback.onError && wsCallback.onError({
            code: code,
            message: header.message || '未知错误'
        })
        return
    }

    switch (ctrl) {
        case 'start':
            // start响应，获取stream_url
            let streamUrl = ''
            if (payload.avatar) {
                streamUrl = payload.avatar.stream_url || payload.avatar.play_url || ''
            }
            console.log('[XunfeiWS] 启动成功，streamUrl:', streamUrl)
            wsCallback && wsCallback.onStartSuccess && wsCallback.onStartSuccess({
                streamUrl: streamUrl,
                fullData: data
            })
            startPing()
            break

        case 'text_driver':
        case 'text_interact':
            // 文本驱动响应
            if (payload.text && payload.text.content) {
                wsCallback && wsCallback.onTextResponse && wsCallback.onTextResponse(payload.text.content)
            }
            // 检查是否结束
            if (payload.nlp && payload.nlp.status === 2) {
                wsCallback && wsCallback.onSpeechEnd && wsCallback.onSpeechEnd()
            }
            break

        case 'tts_end':
            // TTS结束
            wsCallback && wsCallback.onSpeechEnd && wsCallback.onSpeechEnd()
            break

        case 'asr':
        case 'asr_result':
        case 'recognize':
            // ASR识别结果（语音转文字）
            let asrText = ''
            if (payload.text && payload.text.content) {
                asrText = payload.text.content
            } else if (payload.asr && payload.asr.text) {
                asrText = payload.asr.text
            } else if (payload.result) {
                asrText = payload.result
            }
            console.log('[XunfeiWS] ASR识别结果:', asrText)
            wsCallback && wsCallback.onAsrResult && wsCallback.onAsrResult(asrText)
            break

        case 'ping':
            console.log('[XunfeiWS] ping响应')
            break

        case 'Stop':
            console.log('[XunfeiWS] 停止响应')
            break

        default:
            console.log('[XunfeiWS] 未知消息类型:', ctrl)
    }
}

// -----------------------------------------------
//  发送start协议
// -----------------------------------------------
function sendStart() {
    if (!ws || !isConnected) {
        console.error('[XunfeiWS] WebSocket未连接')
        return false
    }

    const requestId = 'dh_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)

    // 讯飞start协议 - 参考讯飞数字人API文档和Java Demo
    const startProtocol = {
        header: {
            app_id: appId,
            request_id: requestId,
            ctrl: 'start',
            scene_id: sceneId || '',
            uid: ''
        },
        parameter: {
            avatar: {
                stream: {
                    protocol: 'xrtc',
                    fps: 25,
                    bitrate: 2000,
                    alpha: 1
                },
                avatar_id: AVATAR_ID,
                width: 960,       // Java Demo 用 960
                height: 720,      // Java Demo 用 720
                format: 'rgb24',  // Java Demo 中的格式
                scale: 2          // Java Demo 中的缩放
            },
            tts: {
                vcn: VCN,
                speed: 50,
                volume: 50,
                pitch: 50
            },
            vc: {
                vc: 1,
                vcn: VCN
            },
            asr: {},
            dispatch: {
                interactive_mode: 1,
                content_analysis: 1
            }
        },
        payload: {}
    }

    console.log('[XunfeiWS] >>> 准备发送start协议, sceneId:', sceneId)
    console.log('[XunfeiWS] >>> start协议:', JSON.stringify(startProtocol))
    try {
        ws.send({
            data: JSON.stringify(startProtocol),
            success: () => {
                console.log('[XunfeiWS] start发送成功')
            },
            fail: (err) => {
                console.error('[XunfeiWS] start发送失败', err)
            }
        })
    } catch (e) {
        console.error('[XunfeiWS] ws.send抛异常:', e)
    }

    return true
}

// -----------------------------------------------
//  发送text_interact（文本交互，走语义理解）
// -----------------------------------------------
function sendTextInteract(text) {
    if (!ws || !isConnected) {
        console.error('[XunfeiWS] WebSocket未连接')
        return false
    }

    if (!text || !text.trim()) {
        return false
    }

    const requestId = 'txt_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)

    const protocol = {
        header: {
            app_id: appId,
            request_id: requestId,
            ctrl: 'text_interact'
        },
        parameter: {
            tts: {
                vcn: VCN,
                speed: 50,
                volume: 50
            },
            air: {
                air: 1,
                add_nonsemantic: 1
            }
        },
        payload: {
            text: {
                content: text.trim()
            }
        }
    }

    console.log('[XunfeiWS] 发送text_interact:', text.trim().substring(0, 50))
    ws.send({
        data: JSON.stringify(protocol),
        fail: (err) => {
            console.error('[XunfeiWS] text_interact发送失败', err)
        }
    })

    return true
}

// -----------------------------------------------
//  发送text_driver（文本驱动，不走语义）
// -----------------------------------------------
function sendTextDriver(text) {
    if (!ws || !isConnected) {
        console.error('[XunfeiWS] WebSocket未连接')
        return false
    }

    if (!text || !text.trim()) {
        return false
    }

    const requestId = 'drv_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)

    const protocol = {
        header: {
            app_id: appId,
            request_id: requestId,
            ctrl: 'text_driver'
        },
        parameter: {
            avatar_dispatch: {
                interactive_mode: 1
            },
            tts: {
                vcn: VCN,
                speed: 50,
                volume: 50
            },
            air: {
                air: 1,
                add_nonsemantic: 1
            }
        },
        payload: {
            text: {
                content: text.trim()
            }
        }
    }

    console.log('[XunfeiWS] 发送text_driver:', text.trim().substring(0, 50))
    ws.send({
        data: JSON.stringify(protocol),
        fail: (err) => {
            console.error('[XunfeiWS] text_driver发送失败', err)
        }
    })

    return true
}

// -----------------------------------------------
//  发送ping保活（4秒一次）
// -----------------------------------------------
function sendPing() {
    if (!ws || !isConnected) return

    const protocol = {
        header: {
            app_id: appId,
            request_id: 'ping_' + Date.now(),
            ctrl: 'ping'
        }
    }

    ws.send({
        data: JSON.stringify(protocol),
        fail: (err) => {
            console.error('[XunfeiWS] ping发送失败', err)
        }
    })
}

// -----------------------------------------------
//  发送stop
// -----------------------------------------------
function sendStop() {
    if (!ws || !isConnected) return

    const protocol = {
        header: {
            app_id: appId,
            request_id: 'stop_' + Date.now(),
            ctrl: 'stop'
        }
    }

    ws.send({
        data: JSON.stringify(protocol),
        fail: (err) => {
            console.error('[XunfeiWS] stop发送失败', err)
        }
    })
}

// -----------------------------------------------
//  保活定时器（4秒）
// -----------------------------------------------
function startPing() {
    stopPing()
    pingInterval = setInterval(() => {
        sendPing()
    }, 12000)  // 12秒心跳（讯飞要求15秒无交互断开，留3秒缓冲）
}

function stopPing() {
    if (pingInterval) {
        clearInterval(pingInterval)
        pingInterval = null
    }
}

// -----------------------------------------------
//  关闭连接
// -----------------------------------------------
function close() {
    stopPing()
    if (ws) {
        try {
            sendStop()
            ws.close()
        } catch (e) {
            console.error('[XunfeiWS] 关闭异常', e)
        }
        ws = null
        isConnected = false
    }
}

// -----------------------------------------------
//  判断连接状态
// -----------------------------------------------
function isWsConnected() {
    return isConnected
}

// -----------------------------------------------
//  导出
// -----------------------------------------------
export default {
    connect,
    close,
    sendStart,
    sendTextInteract,
    sendTextDriver,
    sendPing,
    sendStop,
    isConnected: isWsConnected
}
