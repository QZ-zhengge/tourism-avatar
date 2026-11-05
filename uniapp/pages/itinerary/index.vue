<template>
  <view class="page">
    <!-- 导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: navTop + 'px', height: navHeight + 'px' }">
      <view class="nav-back" @tap="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">我的行程</view>
      <view class="nav-action-add" @tap="showAddDialog">+ 添加</view>
    </view>

    <!-- 统计概览 -->
    <view class="stats-bar" v-if="dates.length > 0">
      <view class="stat-item">
        <text class="stat-value">{{ stats.days }}</text>
        <text class="stat-label">出行天数</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.items }}</text>
        <text class="stat-label">行程总数</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.routes }}</text>
        <text class="stat-label">路线安排</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.bookings }}</text>
        <text class="stat-label">预订</text>
      </view>
    </view>

    <!-- 内容区 -->
    <scroll-view class="content-scroll" scroll-y refresher-enabled :refresher-triggered="loading" @refresherrefresh="loadData">
      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && dates.length === 0">
        <view class="empty-icon-wrap">
          <text class="empty-icon">🗓</text>
        </view>
        <text class="empty-text">暂无行程安排</text>
        <text class="empty-hint">在数字人对话中确认路线或预订酒店后<br />行程会自动记录到这里</text>
        <view class="empty-btn" @tap="showAddDialog">手动添加行程</view>
      </view>

      <!-- 行程列表 -->
      <view class="itinerary-list" v-else>
        <view class="day-group" v-for="(dateKey, di) in dates" :key="dateKey">
          <!-- 日期头部 -->
          <view class="day-header">
            <text class="day-date">{{ formatDate(dateKey) }}</text>
            <text class="day-week">{{ getWeekDay(dateKey) }}</text>
            <text class="day-badge">{{ dayItemCount(grouped[dateKey]) }}</text>
            <view class="day-progress"></view>
          </view>

          <!-- 时间轴 -->
          <view class="timeline-track">
            <view
              class="iti-item"
              v-for="(item, idx) in (grouped[dateKey] || [])"
              :key="dateKey + '-' + idx"
              @tap="onItemTap(item, dateKey, idx)"
              @longpress="onItemLongPress(item, dateKey, idx)"
            >
              <!-- 时间点和线 -->
              <view class="iti-time-dot">
                <text class="iti-time">{{ item.time || '全天' }}</text>
                <view
                  class="iti-dot"
                  :class="'iti-dot--' + (item.type === 'booking' ? 'booking' : item.type === 'route' ? 'route' : 'note')"
                ></view>
              </view>

              <!-- 卡片 -->
              <view class="iti-card">
                <view class="iti-card-accent" :class="'iti-card-accent--' + (item.type === 'booking' ? 'booking' : item.type === 'route' ? 'route' : 'note')"></view>
                <view class="iti-card-header">
                  <view class="iti-card-icon" :class="'iti-card-icon--' + (item.type === 'booking' ? 'booking' : item.type === 'route' ? 'route' : 'note')">
                    {{ getTypeIcon(item.type, item.kind) }}
                  </view>
                  <view class="iti-card-title-area">
                    <text class="iti-card-title">{{ item.title }}</text>
                    <text class="iti-card-summary" v-if="item.summary">{{ item.summary }}</text>
                  </view>
                </view>

                <!-- 标签 -->
                <view class="iti-card-tags" v-if="item.type || item.price || item.rating">
                  <text class="iti-tag" :class="getTypeTag(item.type, item.kind).cls">{{ getTypeTag(item.type, item.kind).text }}</text>
                  <text class="iti-tag tag-route" v-if="item.price">{{ item.price }}</text>
                  <text class="iti-tag tag-hotel" v-if="item.rating">⭐ {{ item.rating }}</text>
                </view>

                <!-- 酒店入住信息 -->
                <view class="iti-card-hotel-info" v-if="item.type === 'booking' && item.kind === 'hotel' && (item.checkIn || item.roomType)">
                  <text class="hotel-date-range">
                    📅 {{ item.checkIn || '-' }}
                    <text class="arrow">→</text>
                    {{ item.checkOut || '-' }}
                  </text>
                  <text class="hotel-rating" v-if="item.roomType">{{ item.roomType }}</text>
                  <text class="hotel-price" v-if="item.price">{{ item.price }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view class="safe-bottom"></view>
      </view>
    </scroll-view>

    <!-- 添加/编辑行程弹窗 -->
    <view class="add-overlay" v-if="showAdd" @tap="showAdd = false">
      <view class="add-dialog" @tap.stop>
        <view class="add-title">{{ editIndex >= 0 ? '编辑行程' : '添加行程' }}</view>
        <view class="add-field">
          <text class="add-label">标题</text>
          <input class="add-input" v-model="addForm.title" placeholder="如：灵山大佛参观" />
        </view>
        <view class="add-field">
          <text class="add-label">时间</text>
          <input class="add-input" v-model="addForm.time" placeholder="如：09:00（留空为全天）" />
        </view>
        <view class="add-field">
          <text class="add-label">备注</text>
          <input class="add-input" v-model="addForm.summary" placeholder="可填写详情描述" />
        </view>
        <view class="add-field">
          <text class="add-label">类型</text>
          <view class="add-type-row">
            <text class="add-type-tag" :class="{ active: addForm.type === 'route' }" @tap="addForm.type='route'">📍 路线</text>
            <text class="add-type-tag" :class="{ active: addForm.type === 'booking' }" @tap="addForm.type='booking'">🏨 预订</text>
            <text class="add-type-tag" :class="{ active: addForm.type === 'note' }" @tap="addForm.type='note'">📝 备忘</text>
          </view>
        </view>
        <view class="add-actions">
          <view class="add-btn add-btn-cancel" @tap="showAdd = false">取消</view>
          <view class="add-btn add-btn-confirm" @tap="submitAdd">{{ editIndex >= 0 ? '保存修改' : '保存' }}</view>
        </view>
      </view>
    </view>

    <!-- 详情弹窗 -->
    <view class="detail-overlay" v-if="showDetail" @tap="showDetail = false">
      <view class="detail-dialog" @tap.stop>
        <view class="detail-header" :class="'detail-header--' + (detailItem.type === 'booking' ? 'booking' : detailItem.type === 'route' ? 'route' : 'note')">
          <view class="detail-header-icon">{{ getTypeIcon(detailItem.type, detailItem.kind) }}</view>
          <text class="detail-header-title">{{ detailItem.title }}</text>
        </view>
        <view class="detail-body">
          <view class="detail-row" v-if="detailItem.time">
            <text class="detail-row-label">时间</text>
            <text class="detail-row-value">{{ detailItem.time }}</text>
          </view>
          <view class="detail-row" v-if="detailItem.type === 'booking' && detailItem.kind === 'hotel'">
            <text class="detail-row-label">酒店</text>
            <text class="detail-row-value">{{ detailItem.title }}</text>
          </view>
          <view class="detail-row" v-if="detailItem.checkIn">
            <text class="detail-row-label">入住</text>
            <text class="detail-row-value">{{ detailItem.checkIn }}</text>
          </view>
          <view class="detail-row" v-if="detailItem.checkOut">
            <text class="detail-row-label">离店</text>
            <text class="detail-row-value">{{ detailItem.checkOut }}</text>
          </view>
          <view class="detail-row" v-if="detailItem.roomType">
            <text class="detail-row-label">房型</text>
            <text class="detail-row-value">{{ detailItem.roomType }}</text>
          </view>
          <view class="detail-row" v-if="detailItem.price">
            <text class="detail-row-label">价格</text>
            <text class="detail-row-value" style="color:#FF6B6B;font-weight:700">{{ detailItem.price }}</text>
          </view>
          <view class="detail-row" v-if="detailItem.rating">
            <text class="detail-row-label">评分</text>
            <text class="detail-row-value">{{ '⭐'.repeat(Math.round(parseFloat(detailItem.rating) || 0)) }} {{ detailItem.rating }}</text>
          </view>
          <view class="detail-summary" v-if="detailItem.summary">{{ detailItem.summary }}</view>
        </view>
        <view class="detail-footer">
          <view class="detail-close-btn" @tap="showDetail = false">关闭</view>
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
