<template>
  <view class="container">
    <!-- Logo 区域 -->
    <view class="logo-section">
      <view class="logo-circle">
        <image class="logo-icon" src="/static/logo.png" mode="aspectFill" />
      </view>
      <text class="app-name">随行</text>
      <text class="app-slogan">智慧旅游导览</text>
    </view>

    <!-- 白色卡片表单 -->
    <view class="form-card">
      <view class="input-row">
        <text class="input-icon">👤</text>
        <input class="input-field" type="text" placeholder="请输入用户名" placeholder-class="input-placeholder" :value="username" @input="bindUsernameInput" />
      </view>

      <view class="input-row">
        <text class="input-icon">📱</text>
        <input class="input-field" type="number" placeholder="请输入手机号" placeholder-class="input-placeholder" :value="phone" @input="bindPhoneInput" />
      </view>

      <view class="input-row">
        <text class="input-icon">🔒</text>
        <input class="input-field" :type="showPassword ? 'text' : 'password'" placeholder="请输入密码" placeholder-class="input-placeholder" :value="password" @input="bindPasswordInput" />
        <text class="eye-icon" @tap="togglePasswordVisibility">{{ showPassword ? '👁' : '👁‍🗨' }}</text>
      </view>

      <view class="input-row">
        <text class="input-icon">🔒</text>
        <input class="input-field" :type="showPassword ? 'text' : 'password'" placeholder="请确认密码" placeholder-class="input-placeholder" :value="confirmPassword" @input="bindConfirmPasswordInput" />
      </view>

      <button class="register-btn" @tap="handleRegister">注册</button>

      <view class="link-row">
        <text class="link-text link-text--gray">已有账号？</text>
        <text class="link-text link-text--green" @tap="navigateToLogin">立即登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<style>
@import './index.css';
</style>
