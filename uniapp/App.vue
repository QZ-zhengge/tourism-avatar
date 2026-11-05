<template>
  <view>
    <!-- 页面内容由 uni-app 自动注入 -->
    <global-dh />
    <floating-assistant />
  </view>
</template>

<script>
export default {
  onLaunch: function() {
    console.log('App Launch')
    // 检查登录状态
    const token = uni.getStorageSync('token')
    if (!token) {
      console.log('用户未登录')
    } else {
      console.log('用户已登录，token:', token)
    }
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
  }
}
</script>

<style>
@import url('./common/uni.css');
@import url('./uni.scss');

page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #333;
  line-height: 1.5;
}

/* 全局通用按钮样式 */
.btn {
  display: block;
  width: 80%;
  margin: 20rpx auto;
  padding: 12rpx 0;
  background-color: #1976d2;
  color: white;
  border-radius: 8rpx;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
}

/* 输入框样式 */
.input {
  width: 80%;
  margin: 10rpx auto;
  padding: 12rpx;
  border: 1px solid #ddd;
  border-radius: 8rpx;
  font-size: 16px;
}

/* 卡片样式 */
.card {
  background-color: white;
  border-radius: 8rpx;
  padding: 20rpx;
  margin: 10rpx 0;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

/* 错误提示 */
.error {
  color: #f44336;
  font-size: 14px;
  margin: 10rpx 0;
  text-align: center;
}

/* 加载动画 */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}
</style>

<style lang="scss">
/* uView 组件样式（全局加载一次） */
@import 'uview-ui/index.scss';
</style>
