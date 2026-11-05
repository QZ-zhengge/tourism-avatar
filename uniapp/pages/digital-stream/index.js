/**
 * 数字人直播页面 - JS 逻辑模块
 * 由 index.vue 的 <script> 导出，供 H5 Web 版本独立引用
 */
const config = require('@/config/index.js')

module.exports = {
  // 后端地址
  baseUrl: config.API_BASE_URL,

  // 状态: idle, starting, ready, failed
  status: 'idle',
  clientId: '',
  videoUrl: '',
  isHlsUrl: true,
  captionText: '',
  showCaption: true,
  sceneIndex: 0,

  scenes: ['通用场景', '会议室', '办公室'],
  quickQuestions: ['你好', '请介绍一下你自己', '今天天气怎么样'],

  pollTimer: null,
  hlsVideoContext: null,
  flvPlayer: null,

  // 管理员同步配置
  adminConfig: { avatarId: '111260002', vcn: 'x4_xiaoxuan', speed: 50, pitch: 50, volume: 50 },
  configPollTimer: null
}
