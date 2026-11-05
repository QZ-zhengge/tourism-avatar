<template>
      <div class="digital-page">
        <div class="page-header">
          <div>
            <div class="page-title">数字人管理</div>
            <div class="page-desc">配置讯飞数字人形象与语音参数，保存后游客端自动同步</div>
          </div>
        </div>

        <!-- 当前数字人 -->
        <div class="avatar-showcase">
          <div class="avatar-showcase-title">当前数字人</div>
          <div class="avatar-current">
            <div class="avatar-current-preview" :style="currentAvatar.bgColor ? 'background:' + currentAvatar.bgColor : ''">
              <img v-if="currentAvatar.icon && !currentAvatar.iconError" :src="currentAvatar.icon" :alt="currentAvatar.name" @error="currentAvatar.iconError=true" />
              <span v-else :style="currentAvatar.textColor ? 'color:'+currentAvatar.textColor : ''">{{ currentAvatar.emoji }}</span>
            </div>
            <div class="avatar-current-info">
              <div class="avatar-current-name">{{ currentAvatar.name }}</div>
              <div class="avatar-current-id">ID: {{ selectedAvatarId }}</div>
              <div class="avatar-status online"><span class="avatar-status-dot"></span>在线</div>
            </div>
            <div class="avatar-hint">
              选中形象将同步至<br/><strong>游客端</strong>
            </div>
          </div>
        </div>

        <!-- 形象照片展示 -->
        <div class="avatar-photo-card" v-if="selectedAvatarData">
          <div class="avatar-photo" :style="'background:' + (selectedAvatarData.bgColor || '#1e293b')">
            <div class="photo-placeholder" v-if="!selectedAvatarData.photoLoaded" :style="'color:'+(selectedAvatarData.textColor||'#fff')">
              {{ selectedAvatarData.emoji }}
            </div>
            <img v-if="selectedAvatarData.photoUrl && !selectedAvatarData.photoError"
                 :src="selectedAvatarData.photoUrl"
                 :alt="selectedAvatarData.name"
                 @load="selectedAvatarData.photoLoaded=true"
                 @error="selectedAvatarData.photoError=true;selectedAvatarData.photoLoaded=true" />
          </div>
          <div class="avatar-photo-info">
            <h3>{{ selectedAvatarData.name }}</h3>
            <p>形象ID: {{ selectedAvatarData.id }} · {{ selectedAvatarData.gender }}</p>
            <div class="avatar-photo-tags">
              <span class="avatar-tag" :class="{active: selectedAvatarData.id === selectedAvatarId}">当前形象</span>
              <span class="avatar-tag">讯飞数字人</span>
              <span class="avatar-tag" v-if="selectedAvatarData.online">在线服务</span>
            </div>
            <div class="avatar-photo-actions">
              <button class="photo-btn" @click="refreshPhoto">🔄 刷新照片</button>
              <button class="photo-btn" @click="showPhotoTips = !showPhotoTips">ℹ️ 照片说明</button>
            </div>
          </div>
        </div>
        <div v-if="showPhotoTips" style="margin-top:12px;padding:12px;background:var(--bg);border:1px solid var(--border);border-radius:8px;font-size:12px;color:var(--text-dim)">
          <strong style="color:var(--cyan)">照片说明：</strong>数字人形象照片由讯飞数字人云平台统一管理，当前显示为平台标准形象照。如需查看实时数字人视频流效果，请在<strong style="color:var(--text)">游客端小程序</strong>中预览。管理员修改配置后，游客端将在5秒内自动同步。
        </div>

        <div class="grid-2">
          <!-- 左侧：形象选择 -->
          <div class="card">
            <div class="card-title">选择数字人形象</div>
            <div class="avatar-grid">
              <div
                v-for="av in avatarList"
                :key="av.id"
                class="avatar-item"
                :class="{selected: selectedAvatarId===av.id, disabled: !av.online}"
                @click="av.online && selectAvatar(av)"
              >
                <div class="avatar-item-icon" :style="av.bgColor ? 'background:'+av.bgColor : ''">
                  <span v-if="!av.icon || av.iconError" :style="av.textColor ? 'color:'+av.textColor : ''">{{ av.emoji }}</span>
                  <img v-else :src="av.icon" :alt="av.name" @error="av.iconError=true" />
                </div>
                <div class="avatar-item-name">{{ av.name }}</div>
                <span class="avatar-item-gender">{{ av.gender }}</span>
              </div>
            </div>
          </div>

          <!-- 右侧：参数配置 -->
          <div class="card">
            <div class="card-title">语音参数配置</div>

            <div class="param-group">
              <div class="param-label">发音人（VCN）</div>
              <select class="param-select" v-model="voiceConfig.vcn">
                <optgroup v-for="grp in vcnGroups" :key="grp.label" :label="grp.label">
                  <option v-for="v in grp.voices" :key="v.id" :value="v.id">{{ v.name }} ({{ v.gender }})</option>
                </optgroup>
              </select>
            </div>

            <div class="param-group">
              <div class="param-label">语速 <span>{{ voiceConfig.speed }}</span></div>
              <input type="range" class="param-slider" min="0" max="100" v-model="voiceConfig.speed" />
            </div>

            <div class="param-group">
              <div class="param-label">音调 <span>{{ voiceConfig.pitch }}</span></div>
              <input type="range" class="param-slider" min="0" max="100" v-model="voiceConfig.pitch" />
            </div>

            <div class="param-group">
              <div class="param-label">音量 <span>{{ voiceConfig.volume }}</span></div>
              <input type="range" class="param-slider" min="0" max="100" v-model="voiceConfig.volume" />
            </div>

            <button class="save-btn" :disabled="isSaving" @click="saveConfig">
              {{ isSaving ? '保存中...' : '保存配置' }}
            </button>
            <button class="reset-btn" @click="resetConfig">重置</button>

            <div style="margin-top:16px;padding:12px;background:var(--bg);border-radius:8px;border:1px solid var(--border)">
              <div style="font-size:12px;color:var(--text-dim);margin-bottom:8px">实时同步状态</div>
              <div style="font-size:13px">
                <span :style="{color: syncStatus==='saved'?'var(--green)':'var(--amber)'}">● {{ syncStatus==='saved'?'已同步':'同步中' }}</span>
                <span style="color:var(--text-dim);margin-left:12px">最后更新: {{ lastUpdateTime }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 场景配置 -->
        <div class="card" style="margin-top:20px">
          <div class="card-title">场景配置</div>
          <div class="grid-3">
            <div class="param-group">
              <div class="param-label">服务ID（service_id）</div>
              <input type="text" class="param-select" v-model="sceneConfig.serviceId" placeholder="例如: 315837983779590144" />
            </div>
            <div class="param-group">
              <div class="param-label">布局模式</div>
              <select class="param-select" v-model="sceneConfig.layout">
                <option value="fullscreen">全屏</option>
                <option value="left">左侧</option>
                <option value="right">右侧</option>
              </select>
            </div>
            <div class="param-group">
              <div class="param-label">显示模式</div>
              <select class="param-select" v-model="sceneConfig.displayMode">
                <option value="head">仅头部</option>
                <option value="upper">上半身</option>
                <option value="full">全身</option>
              </select>
            </div>
          </div>
        </div>
      </div>
</template>

<script setup>
import { inject } from 'vue'

// 从父组件注入状态
const avatarList = inject('avatarList')
const selectedAvatarId = inject('selectedAvatarId')
const selectedAvatarData = inject('selectedAvatarData')
const currentAvatar = inject('currentAvatar')
const voiceConfig = inject('voiceConfig')
const sceneConfig = inject('sceneConfig')
const isSaving = inject('isSaving')
const syncStatus = inject('syncStatus')
const lastUpdateTime = inject('lastUpdateTime')
const showPhotoTips = inject('showPhotoTips')
const vcnGroups = inject('vcnGroups')
const selectAvatar = inject('selectAvatar')
const saveConfig = inject('saveConfig')
const resetConfig = inject('resetConfig')
const refreshPhoto = inject('refreshPhoto')
</script>
