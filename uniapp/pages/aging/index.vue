<template>
  <view class="container">
    <view class="welcome">
      <text class="welcome-text">欢迎回来！</text>
      <text class="user-info">{{ userInfo.nickname || '用户' }}</text>
    </view>

    <view class="content">
      <view class="menu-item" @tap="navigateToHome">
        <text class="menu-icon">🏠</text>
        <text class="menu-text">首页</text>
      </view>

      <view class="menu-item" @tap="navigateToProfile">
        <text class="menu-icon">👤</text>
        <text class="menu-text">个人中心</text>
      </view>

      <view class="menu-item" @tap="navigateToSettings">
        <text class="menu-icon">⚙️</text>
        <text class="menu-text">设置</text>
      </view>

      <view class="menu-item logout" @tap="handleLogout">
        <text class="menu-icon">🚪</text>
        <text class="menu-text">退出登录</text>
      </view>
    </view>

    <view class="footer">
      <text class="footer-text">适老化模式</text>
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
