<template>
  <view class="container">
    <view class="logo-container">
      <image class="logo" src="https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4Fbnp6BwFQV4E9sicVFErUqv6ibGDoThG1aWG0Svw/0" mode="aspectFill" />
      <text class="app-name">随行</text>
      <text class="slogan">您的智能</text>
    </view>

    <view class="register-form">
      <view class="input-container">
        <image class="input-icon" src="https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4Fbnp6BwFQV4E9sicVFErUqv6ibGDoThG1aWG0Svw/0" mode="aspectFit" />
        <input class="input" type="text" placeholder="请输入用户名" placeholder-class="placeholder" :value="username" @input="bindUsernameInput" />
      </view>

      <view class="input-container">
        <image class="input-icon" src="https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4Fbnp6BwFQV4E9sicVFErUqv6ibGDoThG1aWG0Svw/0" mode="aspectFit" />
        <input class="input" type="number" placeholder="请输入手机号" placeholder-class="placeholder" :value="phone" @input="bindPhoneInput" />
      </view>

      <view class="input-container">
        <image class="input-icon" src="https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4Fbnp6BwFQV4E9sicVFErUqv6ibGDoThG1aWG0Svw/0" mode="aspectFit" />
        <input class="input" :type="showPassword ? 'text' : 'password'" placeholder="请输入密码" placeholder-class="placeholder" :value="password" @input="bindPasswordInput" />
        <text class="eye-icon" @tap="togglePasswordVisibility">{{ showPassword ? '👁' : '👁‍🗨' }}</text>
      </view>

      <view class="input-container">
        <image class="input-icon" src="https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4Fbnp6BwFQV4E9sicVFErUqv6ibGDoThG1aWG0Svw/0" mode="aspectFit" />
        <input class="input" :type="showPassword ? 'text' : 'password'" placeholder="请确认密码" placeholder-class="placeholder" :value="confirmPassword" @input="bindConfirmPasswordInput" />
      </view>

      <button class="register-btn" @tap="handleRegister">注册</button>

      <view class="login-container">
        <text class="login-text">已有账号？</text>
        <text class="login-link" @tap="navigateToLogin">立即登录</text>
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
