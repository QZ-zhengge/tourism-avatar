export default {
  name: 'bottom-nav',
  props: {
    current: {
      type: String,
      value: 'home'
    }
  },
  data() {
    return {
      items: [
        { key: 'home', text: '首页', icon: 'home', path: '/pages/home/index' },
        { key: 'map', text: '地图', icon: 'map', path: '/pages/map/index' },
        { key: 'digital', text: '数字人', icon: 'mic', path: '/pages/digital-human/index', highlight: true },
        { key: 'moments', text: '旅友圈', icon: 'chat', path: '/pages/moments/index' },
        { key: 'mine', text: '我的', icon: 'account', path: '/pages/mine/index' }
      ]
    }
  },
  methods: {
    handleTap(path, key) {
      if (key === this.current) return
      uni.reLaunch({ url: path })
    }
  }
}