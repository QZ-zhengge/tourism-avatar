<template>
  <view class="container">

    <!-- ===== 顶部导航栏 ===== -->
    <view class="top-nav">
      <text class="top-nav-title">微信登录</text>
      <view class="top-nav-right">
        <text class="top-nav-more">...</text>
        <u-icon name="eye" size="28" color="#999"></u-icon>
      </view>
    </view>

    <!-- ===== Logo 区域 ===== -->
    <view class="logo-section">
      <view class="logo-circle">
        <!-- 绿色S形叶子Logo -->
        <svg class="logo-icon" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="leafGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#66bb6a"/>
              <stop offset="100%" style="stop-color:#43a047"/>
            </linearGradient>
          </defs>
          <!-- 外圆环 -->
          <circle cx="50" cy="50" r="46" fill="none" stroke="url(#leafGrad)" stroke-width="2" opacity="0.3"/>
          <!-- S形叶子 -->
          <path d="M35 30 Q50 20 60 35 Q70 50 55 60 Q45 65 50 75 Q55 85 70 80" 
                fill="none" stroke="url(#leafGrad)" stroke-width="6" stroke-linecap="round"/>
          <!-- 小叶子装饰 -->
          <path d="M45 55 Q35 50 30 58 Q28 65 38 68" 
                fill="none" stroke="#81c784" stroke-width="3" stroke-linecap="round"/>
        </svg>
      </view>
      <text class="app-name">随行</text>
      <text class="app-slogan">— 您的智能景区虚拟向导 —</text>
    </view>

    <!-- ===== 白色卡片表单 ===== -->
    <view class="form-card">

      <!-- 用户名 -->
      <view class="input-row">
        <u-icon name="account" size="28" color="#999"></u-icon>
        <input
          class="input-field"
          type="text"
          placeholder="请输入用户名"
          placeholder-class="input-placeholder"
          :value="username"
          @input="bindUsernameInput"
        />
      </view>

      <!-- 密码 -->
      <view class="input-row">
        <u-icon name="lock" size="28" color="#999"></u-icon>
        <input
          class="input-field"
          :type="showPassword ? 'text' : 'password'"
          placeholder="请输入密码"
          placeholder-class="input-placeholder"
          :value="password"
          @input="bindPasswordInput"
        />
        <u-icon :name="showPassword ? 'eye-off' : 'eye'" size="28" color="#999" @tap="togglePasswordVisibility"></u-icon>
      </view>

      <!-- 登录按钮 -->
      <button class="login-btn" :loading="loading" @tap="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <!-- 链接行 -->
      <view class="link-row">
        <text class="link-text" @tap="handleForgotPassword">忘记密码？</text>
        <text class="link-text link-text--gray">还没有账号？</text>
        <text class="link-text link-text--green" @tap="goRegister">立即注册</text>
      </view>

      <!-- 老年版开关 -->
      <view class="aging-row">
        <view class="aging-left">
          <u-icon name="account" size="28" color="#999"></u-icon>
          <text class="aging-label">老年版</text>
          <text class="aging-desc">字体更大，使用更简单哦</text>
        </view>
        <switch
          class="aging-switch"
          :checked="agingMode"
          color="#66bb6a"
          @change="bindAgingChange"
        />
      </view>

    </view>

    <!-- ===== 其他登录方式 ===== -->
    <view class="divider-section">
      <view class="divider-line"></view>
      <text class="divider-text">其他登录方式</text>
      <view class="divider-line"></view>
    </view>

    <!-- 社交登录图标 -->
    <view class="social-login">
      <view class="social-item" @tap="loginWithWechat">
        <view class="social-circle social-circle--wechat">
          <u-icon name="chat" size="28" color="#fff"></u-icon>
        </view>
        <text class="social-label">微信</text>
      </view>
      <view class="social-item" @tap="loginWithQQ">
        <view class="social-circle social-circle--qq">
          <u-icon name="qq-fill" size="28" color="#fff"></u-icon>
        </view>
        <text class="social-label">QQ</text>
      </view>
      <view class="social-item" @tap="loginWithPhone">
        <view class="social-circle social-circle--phone">
          <u-icon name="phone" size="28" color="#fff"></u-icon>
        </view>
        <text class="social-label">手机</text>
      </view>
    </view>

    <!-- 底部安全区占位 -->
    <view class="safe-bottom"></view>

    <!-- ===== 底部 TabBar ===== -->
    <view class="bottom-tabbar">
      <view class="tab-item" @tap="goHome">
        <u-icon name="home" size="28" color="#999"></u-icon>
        <text class="tab-text">首页</text>
      </view>
      <view class="tab-item">
        <u-icon name="account" size="28" color="#07c160"></u-icon>
        <text class="tab-text tab-text--active">登录</text>
      </view>
    </view>

  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<style scoped>
@import './index.css';
</style>
