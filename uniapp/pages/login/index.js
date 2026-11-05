import api from '@/utils/api.js'

export default {
  data() {
    return {
      username: '',
      password: '',
      agingMode: false,
      showPassword: false,
      loading: false
    }
  },

  onLoad() {
    const agingMode = uni.getStorageSync('agingMode')
    if (agingMode !== undefined && agingMode !== '') {
      this.agingMode = agingMode
    }
  },

  methods: {
    bindUsernameInput(e) {
      this.username = e.detail.value
    },

    bindPasswordInput(e) {
      this.password = e.detail.value
    },

    bindAgingChange(e) {
      this.agingMode = e.detail.value
      uni.setStorageSync('agingMode', this.agingMode)
    },

    togglePasswordVisibility() {
      this.showPassword = !this.showPassword
    },

    // ========== 用户登录（对接后端 API）==========
    async handleLogin() {
      if (!this.username) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!this.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }

      this.loading = true

      try {
        const res = await api.userLogin(this.username, this.password)

        // 登录成功，保存用户信息
        const userInfo = res.data
        uni.setStorageSync('userInfo', userInfo)
        uni.setStorageSync('token', 'user-' + userInfo.userId)
        uni.setStorageSync('agingMode', this.agingMode)

        uni.showToast({ title: '登录成功', icon: 'success' })

        setTimeout(() => {
          if (this.agingMode) {
            uni.reLaunch({ url: '/pages/aging/index' })
          } else {
            uni.switchTab({ url: '/pages/home/index' })
          }
        }, 1200)
      } catch (err) {
        // 登录失败，保留输入内容供重试
        console.error('登录失败:', err)
      } finally {
        this.loading = false
      }
    },

    handleForgotPassword() {
      uni.showToast({ title: '忘记密码功能开发中', icon: 'none' })
    },

    goRegister() {
      uni.navigateTo({ url: '/pages/register/index' })
    },

    goHome() {
      uni.switchTab({ url: '/pages/home/index' })
    },

    // ========== 第三方登录（暂未实现）==========
    loginWithWechat() {
      uni.showToast({ title: '微信登录暂未开通', icon: 'none' })
    },

    loginWithQQ() {
      uni.showToast({ title: 'QQ登录功能开发中', icon: 'none' })
    },

    loginWithPhone() {
      uni.showToast({ title: '手机号登录功能开发中', icon: 'none' })
    }
  }
}
