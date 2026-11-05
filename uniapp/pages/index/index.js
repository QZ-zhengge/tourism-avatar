export default {
  data() {
    return {
      userInfo: {}
    }
  },

  onLoad() {
    const userInfo = uni.getStorageSync('userInfo')
    if (userInfo) {
      this.userInfo = userInfo
    }
  },

  methods: {
    handleLogout() {
      uni.showModal({
        title: '退出登录',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('userInfo')
            uni.removeStorageSync('token')
            uni.redirectTo({
              url: '/pages/login/index'
            })
          }
        }
      })
    }
  }
}
