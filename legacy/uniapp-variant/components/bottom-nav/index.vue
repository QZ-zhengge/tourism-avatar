<template>
  <view class="bottom-nav-wrap">
    <view class="bottom-nav card-blur">
      <view
        v-for="item in items"
        :key="item.key"
        class="nav-item"
        :class="[item.highlight ? 'highlight' : '', current === item.key ? 'active' : '']"
        @tap="handleTap(item.path, item.key)"
      >
        <view class="icon">
          <up-icon :name="item.icon" :size="44" :color="current === item.key ? '#6c5ce7' : '#999'"></up-icon>
        </view>
        <view class="text">{{ item.text }}</view>
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