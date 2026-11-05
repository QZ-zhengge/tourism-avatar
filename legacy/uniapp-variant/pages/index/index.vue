<template>
  <view class="container">
    <view class="welcome">
      <text class="welcome-text">欢迎回来！</text>
      <text class="user-info">{{ userInfo.nickname || '用户' }}</text>
    </view>

    <view class="content">
      <text class="content-text">这是首页内容</text>
    </view>

    <button class="logout-btn" @tap="handleLogout">退出登录</button>

    <view class="safe-bottom-space"></view>

  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<style>
@import './index.css';
</style>
