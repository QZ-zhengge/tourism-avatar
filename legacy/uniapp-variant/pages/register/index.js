import api from '@/utils/api.js'

export default {
  data() {
    return {
      username: '',
      phone: '',
      email: '',
      password: '',
      confirmPassword: '',
      showPassword: false,
      loading: false
    }
  },

  methods: {
    bindUsernameInput(e) {
      this.username = e.detail.value
    },

    bindPhoneInput(e) {
      this.phone = e.detail.value
    },

    bindEmailInput(e) {
      this.email = e.detail.value
    },

    bindPasswordInput(e) {
      this.password = e.detail.value
    },

    bindConfirmPasswordInput(e) {
      this.confirmPassword = e.detail.value
    },

    togglePasswordVisibility() {
      this.showPassword = !this.showPassword
    },

    async handleRegister() {
      if (!this.username) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!this.phone) {
        uni.showToast({ title: '请输入手机号', icon: 'none' })
        return
      }
      const phoneRegex = /^1[3-9]\d{9}$/
      if (!phoneRegex.test(this.phone)) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }
      if (!this.email) {
        uni.showToast({ title: '请输入邮箱', icon: 'none' })
        return
      }
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(this.email)) {
        uni.showToast({ title: '请输入正确的邮箱', icon: 'none' })
        return
      }
      if (!this.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }
      if (this.password.length < 6) {
        uni.showToast({ title: '密码长度至少6位', icon: 'none' })
        return
      }
      if (this.password !== this.confirmPassword) {
        uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
        return
      }

      this.loading = true

      try {
        await api.userRegister({
          username: this.username,
          password: this.password,
          confirm_password: this.confirmPassword,
          email: this.email,
          phone_number: this.phone
        })
        uni.showToast({ title: '注册成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateTo({ url: '/pages/login/index' })
        }, 1500)
      } catch (err) {
        console.error('注册失败:', err)
      } finally {
        this.loading = false
      }
    },

    navigateToLogin() {
      uni.navigateTo({ url: '/pages/login/index' })
    }
  }
}
