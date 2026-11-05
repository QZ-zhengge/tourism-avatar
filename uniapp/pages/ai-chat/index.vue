<template>
  <view class="ai-chat-page">

    <!-- ========== 侧边栏遮罩 ========== -->
    <view
      class="sidebar-mask"
      :class="{ 'sidebar-mask--show': showSidebar }"
      @tap="toggleSidebar"
    ></view>

    <!-- ========== 侧边栏 ========== -->
    <view class="sidebar" :class="{ 'sidebar--show': showSidebar }">
      <view class="sidebar-header">
        <text class="sidebar-title">历史记录</text>
        <view class="sidebar-close" @tap="toggleSidebar">
          <u-icon name="close" size="28" color="#999"></u-icon>
        </view>
      </view>

      <scroll-view class="sidebar-list" scroll-y>
        <view
          v-for="(item, index) in historyList"
          :key="index"
          class="history-item"
          :class="{ 'history-item--active': currentHistoryIndex === index }"
          @tap="selectHistory(index)"
        >
          <view class="history-icon">
            <u-icon name="chat" size="28" color="#999"></u-icon>
          </view>
          <view class="history-content">
            <text class="history-title">{{ item.title }}</text>
            <text class="history-time">{{ item.time }}</text>
          </view>
        </view>

        <view v-if="historyList.length === 0" class="history-empty">
          <text>暂无历史记录</text>
        </view>
      </scroll-view>

      <view class="sidebar-footer">
        <view class="new-chat-btn" @tap="createNewChat">
          <u-icon name="plus" size="28" color="#2dba98"></u-icon>
          <text class="new-chat-text">新建对话</text>
        </view>
      </view>
    </view>

    <!-- ========== 顶部导航栏 ========== -->
    <view class="chat-header" :style="{ paddingTop: (navTop + 16) + 'px' }">
      <view class="header-left">
        <view class="back-btn" @tap="goBack">
          <u-icon name="arrow-left" size="40" color="#333"></u-icon>
        </view>
      </view>

      <view class="header-center">
        <text class="header-title">随行助手</text>
      </view>

      <view class="header-right">
        <view class="header-icon-btn" @tap="toggleSidebar">
          <u-icon name="list-dot" size="36" color="#555"></u-icon>
        </view>
        <view class="header-icon-btn" @tap="goDigitalHuman">
          <u-icon name="phone" size="36" color="#2dba98"></u-icon>
        </view>
        <view class="header-icon-btn" @tap="showMore">
          <u-icon name="more-dot-fill" size="36" color="#999"></u-icon>
        </view>
      </view>
    </view>

    <!-- ========== 聊天内容区 ========== -->
    <scroll-view
      class="chat-content"
      scroll-y
      :scroll-top="scrollTop"
      enhanced
      show-scrollbar="false"
    >
      <!-- 欢迎语 -->
      <view v-if="messages.length === 0" class="welcome-area">
        <view class="welcome-avatar">
          <u-icon name="chat" size="60" color="#2dba98"></u-icon>
        </view>
        <text class="welcome-text">我是随行助手，可以帮你解答景区相关问题</text>
      </view>

      <!-- 消息列表 -->
      <view
        v-for="(msg, idx) in messages"
        :key="idx"
        v-if="msg && (msg.content || msg.type === 'card' || msg.type === 'hotel-list')"
        class="message-item"
        :class="msg.role === 'user' ? 'message-user' : 'message-ai'"
      >
        <!-- AI头像 -->
        <view v-if="msg.role === 'ai'" class="msg-avatar">
          <u-icon name="chat" size="28" color="#2dba98"></u-icon>
        </view>

        <!-- 消息气泡 -->
        <view class="msg-bubble">
          <view class="msg-text" v-if="msg.type === 'text'">{{ msg.content }}</view>
          <view class="msg-rich" v-else-if="msg.type === 'rich'" v-html="msg.content"></view>

          <!-- ========== 美团风格酒店卡片列表 ========== -->
          <view v-else-if="msg.type === 'hotel-list'" class="hl-container">
            <view class="msg-text" v-if="msg.content">{{ msg.content }}</view>
            <view
              v-for="(card, ci) in msg.cards"
              :key="ci"
              class="hl-card"
              @tap="openHotelLink(card.booking_url)"
            >
              <view class="hl-top">
                <image class="hl-img" :src="card.image" mode="aspectFill" v-if="card.image"></image>
                <view class="hl-img hl-img-placeholder" v-else>
                  <text class="hl-img-letter">{{ getBcLetter(card.title || card.name) }}</text>
                </view>
                <view class="hl-info">
                  <text class="hl-name">{{ card.title || card.name }}</text>
                  <view class="hl-meta">
                    <text class="hl-rating" v-if="card.rating">⭐ {{ card.rating }}</text>
                    <text class="hl-distance" v-if="card.distance">{{ card.distance }}</text>
                  </view>
                  <text class="hl-price">{{ card.price }}</text>
                </view>
              </view>
              <view class="hl-actions" v-if="card.actions && card.actions.length > 0">
                <view
                  v-for="(act, ai) in card.actions"
                  :key="ai"
                  class="hl-btn"
                  :class="act.type === 'book_now' ? 'hl-btn-primary' : 'hl-btn-secondary'"
                  @tap.stop="onHotelAction(act, card)"
                >
                  <text>{{ act.label }}</text>
                </view>
              </view>
              <view class="hl-actions" v-else>
                <view class="hl-btn hl-btn-primary" @tap.stop="openHotelLink(card.booking_url)">立即预订</view>
                <view class="hl-btn hl-btn-secondary" @tap.stop="callHotel(card.phone)" v-if="card.phone">致电咨询</view>
              </view>
            </view>
          </view>
          <view v-else-if="msg.type === 'card'" class="card-booking">
            <view class="msg-text" v-if="msg.content">{{ msg.content }}</view>

            <!-- 顶部信息区：左图右文 -->
            <view class="cb-top">
              <view class="cb-img" :style="{ background: getBcColor(msg.data.name) }">
                <text class="cb-img-text">{{ getBcLetter(msg.data.name) }}</text>
              </view>
              <view class="cb-info">
                <input
                  class="cb-title-input"
                  :value="msg.data.name"
                  placeholder="酒店名称"
                  @input="e => updateCardField(idx, 'name', e.detail.value)"
                />
                <input
                  class="cb-sub-input"
                  :value="msg.data.roomType"
                  placeholder="房型"
                  @input="e => updateCardField(idx, 'roomType', e.detail.value)"
                />
                <input
                  class="cb-price-input"
                  :value="msg.data.price"
                  placeholder="价格"
                  @input="e => updateCardField(idx, 'price', e.detail.value)"
                />
              </view>
            </view>

            <!-- 表单区 -->
            <view class="cb-form">
              <view class="cb-row">
                <text class="cb-label">入住</text>
                <input
                  class="cb-input"
                  :value="msg.data.checkIn"
                  placeholder="如 2026-06-15"
                  @input="e => updateCardField(idx, 'checkIn', e.detail.value)"
                />
              </view>
              <view class="cb-row">
                <text class="cb-label">离店</text>
                <input
                  class="cb-input"
                  :value="msg.data.checkOut"
                  placeholder="如 2026-06-16"
                  @input="e => updateCardField(idx, 'checkOut', e.detail.value)"
                />
              </view>
              <view class="cb-row">
                <text class="cb-label">备注</text>
                <input
                  class="cb-input"
                  :value="msg.data.note"
                  placeholder="其他需求（选填）"
                  @input="e => updateCardField(idx, 'note', e.detail.value)"
                />
              </view>
            </view>

            <!-- 操作按钮 -->
            <view class="cb-actions" v-if="!msg.confirmed">
              <view class="cb-btn cb-confirm" @tap="confirmBookingFromCard(idx)">确认添加到行程</view>
            </view>
            <view class="cb-confirmed" v-else>
              <text class="cb-confirmed-text">✓ 已添加到行程</text>
            </view>
          </view>
        </view>

        <!-- 用户头像 -->
        <view v-if="msg.role === 'user'" class="msg-avatar msg-avatar--user">
          <u-icon name="account" size="28" color="#fff"></u-icon>
        </view>
      </view>

      <!-- AI正在输入 -->
      <view v-if="isTyping" class="message-item message-ai">
        <view class="msg-avatar">
          <u-icon name="chat" size="28" color="#2dba98"></u-icon>
        </view>
        <view class="msg-bubble msg-bubble--typing">
          <view class="typing-dots">
            <view class="typing-dot"></view>
            <view class="typing-dot"></view>
            <view class="typing-dot"></view>
          </view>
        </view>
      </view>

      <!-- 快捷建议问题 -->
      <view v-if="showSuggestions" class="suggestion-list">
        <view
          v-for="(suggestion, index) in suggestions"
          :key="index"
          class="suggestion-item"
          @tap="sendSuggestion(suggestion)"
        >
          <text>{{ suggestion }}</text>
        </view>
      </view>

      <!-- 底部留白 -->
      <view class="chat-bottom-space"></view>
    </scroll-view>

    <!-- ========== 底部快捷功能区 ========== -->
    <view class="quick-actions" v-if="showQuickActions">
      <scroll-view class="quick-scroll" scroll-x enhanced show-scrollbar="false">
        <view class="quick-item" @tap="onQuickAction('fast')">
          <u-icon name="lightning" size="28" color="#666"></u-icon>
          <text class="quick-text">快速</text>
        </view>
        <view class="quick-item" @tap="onQuickAction('photo')">
          <u-icon name="photo" size="28" color="#666"></u-icon>
          <text class="quick-text">拍题答疑</text>
        </view>
        <view class="quick-item" @tap="onQuickAction('video')">
          <u-icon name="phone" size="28" color="#666"></u-icon>
          <text class="quick-text">视频通话</text>
        </view>
        <view class="quick-item" @tap="onQuickAction('itinerary')">
          <u-icon name="checkmark-circle" size="28" color="#2dba98"></u-icon>
          <text class="quick-text" style="color:#2dba98">添加到行程</text>
        </view>
      </scroll-view>
    </view>

    <!-- ========== 输入框区域 ========== -->
    <view class="input-area">
      <view class="input-box">
        <view class="input-icon-btn" @tap="chooseImage">
          <u-icon name="photo" size="36" color="#666"></u-icon>
        </view>

        <input
          class="input-field"
          type="text"
          :value="inputText"
          placeholder="发消息或按住说话..."
          placeholder-class="input-placeholder"
          @input="onInput"
          @confirm="sendMessage"
          confirm-type="send"
        />

        <view class="input-icon-btn" @tap="toggleVoice">
          <u-icon name="mic" size="36" color="#666"></u-icon>
        </view>

        <view class="input-icon-btn input-icon-btn--send" @tap="sendMessage" v-if="inputText">
          <u-icon name="arrow-upward" size="36" color="#fff"></u-icon>
        </view>

        <view class="input-icon-btn" @tap="showMoreInput" v-else>
          <u-icon name="plus" size="36" color="#666"></u-icon>
        </view>
      </view>

      <!-- 语音输入提示 -->
      <view v-if="isRecording" class="voice-hint">
        <text>正在录音，松开发送</text>
      </view>
    </view>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>

  </view>
</template>

<script>
import script from './index.js'
export default script
</script>

<style scoped>
@import './index.css';
</style>
