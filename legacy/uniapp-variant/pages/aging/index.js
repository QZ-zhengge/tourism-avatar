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

    uni.showToast({
      title: '已进入适老化模式',
      icon: 'success',
      duration: 2000
    })
  },

  methods: {
    navigateToHome() {
      uni.showToast({
        title: '正在前往首页',
        icon: 'success',
        duration: 1000
      })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/home/index' })
      }, 1000)
    },

    navigateToProfile() {
      uni.showModal({
        title: '个人中心',
        content: '个人中心功能正在开发中，敬请期待！',
        confirmText: '我知道了',
        showCancel: false
      })
    },

    navigateToSettings() {
      uni.showModal({
        title: '设置',
        content: '设置功能正在开发中，敬请期待！',
        confirmText: '我知道了',
        showCancel: false
      })
    },

    handleLogout() {
      uni.showModal({
        title: '退出登录',
        content: '确定要退出登录吗？',
        confirmText: '确定',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('userInfo')
            uni.removeStorageSync('token')
            uni.removeStorageSync('agingMode')

            uni.showToast({
              title: '已退出登录',
              icon: 'success',
              duration: 1500
            })

            setTimeout(() => {
              uni.redirectTo({
                url: '/pages/login/index'
              })
            }, 1500)
          }
        }
      })
    }
  }
}
