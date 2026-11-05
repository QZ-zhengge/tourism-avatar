<template>
  <view class="mine-page">

    <!-- ========== 顶部用户信息区 ========== -->
    <view class="user-header">
      <!-- 背景装饰 -->
      <view class="header-bg"></view>

      <!-- 用户卡片 -->
      <view class="user-card" @tap="editProfile">
        <!-- 头像 -->
        <view class="avatar-wrap">
          <image
            class="avatar"
            :src="userInfo.avatar"
            mode="aspectFill"
            @error="onAvatarError"
          ></image>
          <view class="avatar-level" style="background: linear-gradient(135deg, #4ac7ff, #38b2ac);">{{ userInfo.level }}</view>
        </view>

        <!-- 用户信息 -->
        <view class="user-info">
          <view class="nickname-row">
            <text class="nickname" @tap.stop="goLogin">{{ userInfo.isLoggedIn ? userInfo.nickname : '点击登录' }}</text>
            <view class="vip-badge" v-if="userInfo.isLoggedIn">
              <text class="vip-icon">★</text>
              <text class="vip-text">会员</text>
            </view>
          </view>
          <view class="scenic-row" v-if="userInfo.isLoggedIn">
            <text class="scenic-text">📍 {{ userInfo.scenicName }}</text>
          </view>
          <view class="login-hint" v-else>
            <text>登录后享受更多专属服务</text>
          </view>
        </view>

        <!-- 编辑按钮 -->
        <view class="edit-btn" v-if="userInfo.isLoggedIn">
          <text class="edit-icon">✎</text>
        </view>
      </view>

      <!-- 统计数据栏 -->
      <view class="stats-bar" v-if="userInfo.isLoggedIn">
        <view class="stat-item" v-for="stat in statsItems" :key="stat.label">
          <text class="stat-value">{{ stat.value }}</text>
          <text class="stat-label">{{ stat.label }}</text>
        </view>
      </view>

      <!-- 未登录时的简约提示 -->
      <view class="guest-prompt" v-else @tap="goLogin">
        <text class="guest-text">登录查看专属旅行数据</text>
        <text class="guest-arrow">→</text>
      </view>
    </view>

    <!-- ========== 点亮城市地图 ========== -->
    <view class="section-card map-card">
      <view class="map-header">
        <view class="map-title-row">
          <text class="map-title">🌍 点亮城市</text>
          <text class="map-subtitle" v-if="visitedProvinces.length > 0">已点亮 {{ visitedProvinces.length }} 省</text>
        </view>
        <view class="map-controls">
          <view class="map-btn" @tap="zoomMap(-1)">
            <text class="map-btn-text">−</text>
          </view>
          <view class="map-btn" @tap="zoomMap(1)">
            <text class="map-btn-text">+</text>
          </view>
        </view>
      </view>

      <!-- 地图容器 -->
      <view class="map-container" @touchstart="onMapTouchStart" @touchmove="onMapTouchMove" @touchend="onMapTouchEnd">
        <view class="map-wrapper" :style="{ transform: 'scale(' + mapScale + ') translate(' + mapTranslateX + 'px, ' + mapTranslateY + 'px)' }">
          <!-- 简化中国地图 SVG -->
          <svg class="china-map" viewBox="0 0 600 500" xmlns="http://www.w3.org/2000/svg">
            <!-- 东北地区 -->
            <path class="province" :class="{ visited: isVisited('hlj') }" d="M480 60 L520 50 L550 70 L560 100 L540 130 L510 140 L480 120 L470 90 Z" @tap="selectProvince('hlj', '黑龙江')" />
            <path class="province" :class="{ visited: isVisited('jl') }" d="M470 130 L510 140 L520 160 L500 180 L460 170 L450 150 Z" @tap="selectProvince('jl', '吉林')" />
            <path class="province" :class="{ visited: isVisited('ln') }" d="M450 160 L500 180 L510 200 L480 220 L430 210 L420 180 Z" @tap="selectProvince('ln', '辽宁')" />

            <!-- 华北地区 -->
            <path class="province" :class="{ visited: isVisited('bj') }" d="M420 200 L450 190 L460 210 L440 230 L410 225 Z" @tap="selectProvince('bj', '北京')" />
            <path class="province" :class="{ visited: isVisited('tj') }" d="M440 230 L460 210 L470 230 L455 245 L435 240 Z" @tap="selectProvince('tj', '天津')" />
            <path class="province" :class="{ visited: isVisited('heb') }" d="M400 180 L450 160 L470 180 L480 200 L450 220 L420 230 L390 210 Z" @tap="selectProvince('heb', '河北')" />
            <path class="province" :class="{ visited: isVisited('sx') }" d="M360 200 L400 180 L420 200 L410 240 L370 250 L350 230 Z" @tap="selectProvince('sx', '山西')" />
            <path class="province" :class="{ visited: isVisited('nmg') }" d="M280 140 L360 120 L400 180 L380 220 L320 230 L260 200 L250 160 Z" @tap="selectProvince('nmg', '内蒙古')" />

            <!-- 西北地区 -->
            <path class="province" :class="{ visited: isVisited('sx2') }" d="M300 240 L350 230 L370 250 L360 290 L310 300 L280 280 Z" @tap="selectProvince('sx2', '陕西')" />
            <path class="province" :class="{ visited: isVisited('gs') }" d="M220 200 L280 180 L320 230 L300 290 L240 300 L200 260 L190 220 Z" @tap="selectProvince('gs', '甘肃')" />
            <path class="province" :class="{ visited: isVisited('qh') }" d="M180 240 L220 200 L240 300 L220 340 L160 340 L140 290 Z" @tap="selectProvince('qh', '青海')" />
            <path class="province" :class="{ visited: isVisited('nx') }" d="M280 280 L320 260 L330 290 L310 310 L275 305 Z" @tap="selectProvince('nx', '宁夏')" />
            <path class="province" :class="{ visited: isVisited('xj') }" d="M80 120 L180 100 L220 200 L190 280 L140 320 L60 300 L40 200 L50 140 Z" @tap="selectProvince('xj', '新疆')" />

            <!-- 华东地区 -->
            <path class="province" :class="{ visited: isVisited('sd') }" d="M420 230 L460 220 L480 250 L470 290 L430 300 L400 270 Z" @tap="selectProvince('sd', '山东')" />
            <path class="province" :class="{ visited: isVisited('js') }" d="M430 290 L470 290 L480 320 L450 340 L420 330 L415 305 Z" @tap="selectProvince('js', '江苏')" />
            <path class="province" :class="{ visited: isVisited('ah') }" d="M415 330 L450 340 L460 370 L430 390 L400 375 L405 350 Z" @tap="selectProvince('ah', '安徽')" />
            <path class="province" :class="{ visited: isVisited('zj') }" d="M450 370 L480 360 L500 400 L470 430 L440 420 L445 385 Z" @tap="selectProvince('zj', '浙江')" />
            <path class="province" :class="{ visited: isVisited('fj') }" d="M470 430 L510 420 L530 460 L490 480 L460 460 Z" @tap="selectProvince('fj', '福建')" />
            <path class="province" :class="{ visited: isVisited('jx') }" d="M420 390 L460 370 L480 400 L470 440 L430 450 L410 420 Z" @tap="selectProvince('jx', '江西')" />
            <path class="province" :class="{ visited: isVisited('sh') }" d="M455 305 L475 300 L480 325 L460 330 Z" @tap="selectProvince('sh', '上海')" />

            <!-- 华中地区 -->
            <path class="province" :class="{ visited: isVisited('henan') }" d="M380 250 L420 230 L450 260 L440 300 L400 310 L370 280 Z" @tap="selectProvince('henan', '河南')" />
            <path class="province" :class="{ visited: isVisited('hubei') }" d="M370 310 L400 300 L420 340 L400 380 L360 385 L345 350 Z" @tap="selectProvince('hubei', '湖北')" />
            <path class="province" :class="{ visited: isVisited('hunan') }" d="M360 385 L400 380 L420 420 L390 460 L350 455 L340 415 Z" @tap="selectProvince('hunan', '湖南')" />

            <!-- 华南地区 -->
            <path class="province" :class="{ visited: isVisited('gd') }" d="M390 460 L430 450 L470 480 L480 520 L440 540 L380 520 L370 480 Z" @tap="selectProvince('gd', '广东')" />
            <path class="province" :class="{ visited: isVisited('gx') }" d="M300 420 L360 400 L390 460 L370 510 L320 520 L280 480 L275 440 Z" @tap="selectProvince('gx', '广西')" />
            <path class="province" :class="{ visited: isVisited('hn') }" d="M420 480 L470 480 L490 510 L470 540 L450 550 L410 540 L415 505 Z" @tap="selectProvince('hn', '海南')" />

            <!-- 西南地区 -->
            <path class="province" :class="{ visited: isVisited('sc') }" d="M260 320 L320 300 L360 340 L350 400 L300 420 L250 400 L240 350 Z" @tap="selectProvince('sc', '四川')" />
            <path class="province" :class="{ visited: isVisited('cq') }" d="M320 360 L360 340 L380 380 L370 420 L340 440 L310 420 Z" @tap="selectProvince('cq', '重庆')" />
            <path class="province" :class="{ visited: isVisited('gz') }" d="M280 420 L340 400 L360 440 L340 490 L290 500 L260 460 Z" @tap="selectProvince('gz', '贵州')" />
            <path class="province" :class="{ visited: isVisited('yn') }" d="M220 400 L280 380 L300 440 L290 520 L240 560 L180 540 L170 460 Z" @tap="selectProvince('yn', '云南')" />
            <path class="province" :class="{ visited: isVisited('xz') }" d="M100 300 L160 280 L200 340 L180 420 L120 440 L80 380 L70 320 Z" @tap="selectProvince('xz', '西藏')" />

            <!-- 港澳台 -->
            <path class="province hongkong" :class="{ visited: isVisited('hk') }" d="M460 510 L475 505 L480 520 L465 525 Z" @tap="selectProvince('hk', '香港')" />
            <path class="province" :class="{ visited: isVisited('mo') }" d="M435 515 L450 512 L453 525 L438 528 Z" @tap="selectProvince('mo', '澳门')" />
            <path class="province taiwan" :class="{ visited: isVisited('tw') }" d="M510 450 L530 440 L545 470 L525 490 L505 475 Z" @tap="selectProvince('tw', '台湾')" />
          </svg>
        </view>
      </view>

      <!-- 省份详情弹窗 -->
      <view class="province-detail" v-if="selectedProvince" @tap="closeProvinceDetail">
        <view class="province-card" @tap.stop>
          <view class="province-header">
            <text class="province-name">{{ selectedProvince.name }}</text>
            <text class="province-close" @tap="closeProvinceDetail">✕</text>
          </view>
          <view class="province-stats">
            <view class="pstat">
              <text class="pstat-num">{{ getCityCount(selectedProvince) }}</text>
              <text class="pstat-label">到访城市</text>
            </view>
            <view class="pstat">
              <text class="pstat-num">{{ selectedProvince.visitCount }}</text>
              <text class="pstat-label">游玩次数</text>
            </view>
            <view class="pstat">
              <text class="pstat-num">{{ selectedProvince.totalDays }}</text>
              <text class="pstat-label">累计天数</text>
            </view>
          </view>
          <view class="city-tags" v-if="selectedProvince.cities && selectedProvince.cities.length">
            <text class="city-tag" v-for="city in selectedProvince.cities" :key="city">{{ city }}</text>
          </view>
          <view class="province-empty" v-else>
            <text>还没有去过这里哦~</text>
            <text class="province-hint">快去探索吧！</text>
          </view>
        </view>
      </view>

      <!-- 地图底部统计 -->
      <view class="map-footer">
        <view class="map-legend">
          <view class="legend-item">
            <view class="legend-dot lit"></view>
            <text class="legend-text">已点亮</text>
          </view>
          <view class="legend-item">
            <view class="legend-dot unlit"></view>
            <text class="legend-text">未探索</text>
          </view>
        </view>
        <text class="map-tip" v-if="visitedProvinces.length > 0">点击省份查看详情</text>
        <text class="map-tip" v-else>还没有点亮任何城市</text>
      </view>
    </view>

    <!-- ========== 年度旅行报告 ========== -->
    <view class="section-card report-card" v-if="userInfo.isLoggedIn">
      <view class="report-header">
        <text class="report-year">{{ currentYear }} 年度旅行报告</text>
        <text class="report-compare">超越 {{ reportData.percentRank }}% 游客</text>
      </view>
      <view class="report-stats">
        <view class="report-stat">
          <text class="report-num">{{ reportData.travelCount }}</text>
          <text class="report-label">出行次数</text>
        </view>
        <view class="report-divider"></view>
        <view class="report-stat">
          <text class="report-num">{{ reportData.spotsCount }}</text>
          <text class="report-label">打卡景点</text>
        </view>
        <view class="report-divider"></view>
        <view class="report-stat">
          <text class="report-num">{{ reportData.totalHours }}</text>
          <text class="report-label">累计时长(h)</text>
        </view>
        <view class="report-divider"></view>
        <view class="report-stat">
          <text class="report-num">{{ reportData.likesCount }}</text>
          <text class="report-label">获得点赞</text>
        </view>
      </view>
    </view>

    <!-- ========== 环保出行统计 ========== -->
    <view class="section-card eco-card" v-if="userInfo.isLoggedIn">
      <view class="eco-header">
        <text class="eco-title">🌱 低碳出行</text>
        <text class="eco-tag">减少碳排放 {{ ecoData.carbonReduced }} kg</text>
      </view>
      <view class="eco-content">
        <view class="eco-circle">
          <view class="eco-circle-inner" :style="{ borderColor: '#2e9eca' }">
            <text class="eco-rank">{{ ecoData.rank }}</text>
            <text class="eco-rank-label">环保等级</text>
          </view>
        </view>
        <view class="eco-info">
          <view class="eco-row">
            <text class="eco-label">步行</text>
            <text class="eco-value">{{ ecoData.walk }} 次</text>
          </view>
          <view class="eco-row">
            <text class="eco-label">骑行</text>
            <text class="eco-value">{{ ecoData.bike }} 次</text>
          </view>
          <view class="eco-row">
            <text class="eco-label">公共交通</text>
            <text class="eco-value">{{ ecoData.bus }} 次</text>
          </view>
          <view class="eco-row eco-row-highlight">
            <text class="eco-label">本月减排</text>
            <text class="eco-value" :style="{ color: '#2dba98' }">{{ ecoData.monthCarbon }} kg CO₂</text>
          </view>
        </view>
      </view>
    </view>

    <!-- ========== 快速服务入口 ========== -->
    <view class="section-card quick-card">
      <view class="section-header">
        <text class="section-title">常用服务</text>
      </view>
      <view class="quick-grid">
        <view
          v-for="item in quickServices"
          :key="item.id"
          class="quick-item"
          @tap="onMenuTap(item.id)"
        >
          <view class="quick-icon-wrap" :style="{ background: item.color + '18' }">
            <text class="quick-icon" :style="{ color: item.color }">{{ item.icon }}</text>
          </view>
          <text class="quick-name">{{ item.name }}</text>
          <text class="quick-badge" v-if="item.badge">{{ item.badge }}</text>
        </view>
      </view>
    </view>

    <!-- ========== 菜单列表区 ========== -->
    <view class="menu-section">
      <view
        v-for="group in menuGroups"
        :key="group.title"
        class="menu-group"
      >
        <view class="group-title">{{ group.title }}</view>
        <view class="menu-list">
          <view
            v-for="item in group.items"
            :key="item.id"
            class="menu-item"
            @tap="onMenuTap(item.id)"
          >
            <view class="menu-left">
              <view class="menu-icon" :style="{ background: item.iconColor + '20', color: item.iconColor }">
                <text>{{ item.icon }}</text>
              </view>
              <text class="menu-name">{{ item.name }}</text>
            </view>
            <view class="menu-right">
              <text class="menu-badge" v-if="item.badge">{{ item.badge }}</text>
              <text class="menu-arrow" v-if="item.arrow">›</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- ========== 底部功能区 ========== -->
    <view class="bottom-actions">
      <view class="action-item" @tap="callService">
        <text class="action-icon">☎</text>
        <text class="action-text">客服热线</text>
      </view>
      <view class="action-divider"></view>
      <view class="action-item" @tap="logout" v-if="userInfo.isLoggedIn">
        <text class="action-icon logout-icon">⏻</text>
        <text class="action-text logout-text">退出登录</text>
      </view>
      <view class="action-item" @tap="goLogin" v-else>
        <text class="action-icon">→</text>
        <text class="action-text">登录</text>
      </view>
    </view>

    <!-- ========== 版本信息 ========== -->
    <view class="version-info">
      <text>随行 v1.0.0 · 让旅行更简单</text>
    </view>

    <!-- ========== 打卡记录弹窗 ========== -->
    <view class="mine-modal-mask" v-if="showCheckinRecords" @tap="closeCheckinRecords">
      <view class="mine-checkin-modal" @tap.stop>
        <view class="mine-modal-header">
          <text class="mine-modal-title">我的打卡记录（{{ checkinRecords.length }}）</text>
          <view class="mine-modal-close" @tap="closeCheckinRecords">✕</view>
        </view>

        <scroll-view class="mine-records-scroll" scroll-y>
          <view v-if="checkinRecords.length === 0" class="mine-empty-records">
            <text class="mine-empty-icon">📷</text>
            <text class="mine-empty-text">还没有打卡记录</text>
            <text class="mine-empty-hint">去拍照打卡页面打卡吧~</text>
          </view>
          <view
            v-for="(rec, idx) in checkinRecords"
            :key="rec.id || idx"
            class="mine-record-item"
          >
            <!-- 照片 -->
            <view class="mine-rec-photo-wrap" @tap="previewCheckinPhoto(rec.photo)">
              <image v-if="rec.photo" class="mine-rec-photo" :src="rec.photo" mode="aspectFill"></image>
              <view v-else class="mine-rec-photo-empty" :style="{ backgroundColor: getCheckinColor(rec.spotName, idx) }">
                <text class="mine-rec-photo-letter">{{ getCheckinLetter(rec.spotName) }}</text>
              </view>
            </view>
            <!-- 信息 -->
            <view class="mine-rec-info">
              <text class="mine-rec-name">{{ rec.spotName }}</text>
              <text class="mine-rec-time">{{ rec.date }} {{ rec.time }}</text>
              <text v-if="rec.note" class="mine-rec-note">{{ rec.note }}</text>
            </view>
          </view>
        </scroll-view>

        <!-- 跳转到打卡页面 -->
        <view class="mine-go-checkin" @tap="() => { closeCheckinRecords(); uni.navigateTo({ url: '/pages/checkin/index' }) }">
          <text>去拍照打卡 →</text>
        </view>
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
