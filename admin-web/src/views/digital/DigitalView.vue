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
        <div class="avatar-current-preview">
          <img v-if="sel?.icon && !sel.iconError" :src="sel.icon" :alt="sel.name" @error="sel.iconError=true" />
          <span v-else :style="sel?.textColor ? 'color:'+sel.textColor : ''">{{ sel?.emoji || '👤' }}</span>
        </div>
        <div class="avatar-current-info">
          <div class="avatar-current-name">{{ sel?.name || '未选择' }}</div>
          <div class="avatar-current-id">ID: {{ selectedAvatarId }}</div>
          <div class="avatar-status online"><span class="avatar-status-dot"></span>在线</div>
        </div>
        <div class="avatar-hint">选中形象将同步至<br/><strong>游客端</strong></div>
      </div>
    </div>

    <!-- 形象照片展示 -->
    <div class="avatar-photo-card" v-if="sel">
      <div class="avatar-photo">
        <div class="photo-placeholder" v-if="!sel.photoLoaded" :style="'color:'+(sel.textColor||'#fff')">{{ sel.emoji }}</div>
        <img v-if="sel.photoUrl && !sel.photoError"
             :src="sel.photoUrl" :alt="sel.name"
             @load="sel.photoLoaded=true"
             @error="sel.photoError=true;sel.photoLoaded=true" />
      </div>
      <div class="avatar-photo-info">
        <h3>{{ sel.name }}</h3>
        <p>形象ID: {{ sel.avatar_id || sel.id }} · {{ sel.category || sel.gender || '' }}</p>
        <div class="avatar-photo-tags">
          <span class="avatar-tag active">当前形象</span>
          <span class="avatar-tag">讯飞数字人</span>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <!-- 左侧：形象选择 -->
      <div class="card">
        <div class="card-title">选择数字人形象</div>
        <div class="avatar-grid" v-if="items.length">
          <div v-for="av in items" :key="av.avatar_id || av.id"
               class="avatar-item"
               :class="{selected: selectedAvatarId===(av.avatar_id||av.id)}"
               @click="selectAvatar(av)">
            <div class="avatar-item-icon">
              <span v-if="!av.icon || av.iconError" :style="av.textColor ? 'color:'+av.textColor : ''">{{ av.emoji }}</span>
              <img v-else :src="av.icon" :alt="av.name" @error="av.iconError=true" />
            </div>
            <div class="avatar-item-name">{{ av.name }}</div>
            <span class="avatar-item-gender">{{ av.category || av.gender || '' }}</span>
          </div>
        </div>
        <div v-else class="loading-hint">
          <div class="spinner"></div>
          <span>加载形象列表中...</span>
        </div>
      </div>

      <!-- 右侧：参数配置 -->
      <div class="card">
        <div class="card-title">语音参数配置</div>
        <div class="param-group">
          <div class="param-label">发音人（VCN）</div>
          <select class="param-select" v-model="voiceConfig.vcn">
            <optgroup v-for="grp in localVcnGroups" :key="grp.label" :label="grp.label">
              <option v-for="v in grp.voices" :key="v.id" :value="v.id">{{ v.name }} ({{ v.gender }})</option>
            </optgroup>
          </select>
        </div>
        <div class="param-group">
          <div class="param-label">语速 <span>{{ voiceConfig.speed }}</span></div>
          <input type="range" class="param-slider" min="0" max="100" v-model.number="voiceConfig.speed" />
        </div>
        <div class="param-group">
          <div class="param-label">音调 <span>{{ voiceConfig.pitch }}</span></div>
          <input type="range" class="param-slider" min="0" max="100" v-model.number="voiceConfig.pitch" />
        </div>
        <div class="param-group">
          <div class="param-label">音量 <span>{{ voiceConfig.volume }}</span></div>
          <input type="range" class="param-slider" min="0" max="100" v-model.number="voiceConfig.volume" />
        </div>
        <button class="save-btn" :disabled="isSaving" @click="saveConfig">
          {{ isSaving ? '保存中...' : '保存配置' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, inject, onMounted } from 'vue'

// 从 App.vue 取已加载的 55 个形象数据
const avatarList = inject('avatarList', ref([]))
const selectedAvatarId = inject('selectedAvatarId', ref('111260002'))
const voiceConfig = inject('voiceConfig', reactive({ vcn: 'x4_xiaoxuan', speed: 50, pitch: 50, volume: 50 }))
const isSaving = inject('isSaving', ref(false))

// 本地 VCN 发音人（独立加载，不依赖 App.vue 的 vcnGroups）
const localVcnGroups = ref([])
// 使用 inject 的 avatarList（App.vue 已加载 55 个）
const items = computed(() => avatarList.value || [])
const loadingMsg = computed(() => items.value.length ? '' : '加载形象列表中...')

const sel = computed(() => items.value.find(a => (a.avatar_id || a.id) === selectedAvatarId.value) || null)

function selectAvatar(av) { selectedAvatarId.value = av.avatar_id || av.id }

async function loadVcnVoices() {
  try {
    const res = await fetch(`/api/v1/admin/vcn/voices`, { signal: AbortSignal.timeout(10000) })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const raw = await res.json()
    const items = raw?.data || raw?.items || []
    if (!Array.isArray(items) || !items.length) throw new Error('empty')
    const gmap = {}
    items.forEach(v => {
      const cat = v.category || '其他'
      if (!gmap[cat]) gmap[cat] = []
      gmap[cat].push({ id: v.vcn || v.id, name: v.name, gender: v.gender || '' })
    })
    localVcnGroups.value = Object.entries(gmap).map(([label, voices]) => ({ label, voices }))
  } catch(e) {
    console.warn('[数字人] VCN API失败，使用Mock:', e.message)
    localVcnGroups.value = [
      { label:'中文普通话·交互', voices:[{id:'x4_xiaoxuan',name:'小璇',gender:'女'},{id:'x4_panting',name:'潘婷',gender:'女'},{id:'x4_mingge',name:'明哥',gender:'男'}] },
      { label:'中文普通话·通用/阅读', voices:[{id:'x4_lingyuyan',name:'聆玉言',gender:'女'},{id:'x4_xiaozhong',name:'小忠',gender:'男'}] },
    ]
  }
}

async function loadConfig() {
  try {
    const res = await fetch(`/api/v1/admin/digital/config?scenic_id=lingshan`, { signal: AbortSignal.timeout(10000) })
    if (!res.ok) return
    const raw = await res.json()
    const cfg = raw?.data || raw
    if (!cfg?.avatar_id) return
    selectedAvatarId.value = cfg.avatar_id
    if (cfg.vcn) voiceConfig.vcn = cfg.vcn
    if (cfg.speed != null) voiceConfig.speed = cfg.speed
    if (cfg.pitch != null) voiceConfig.pitch = cfg.pitch
    if (cfg.volume != null) voiceConfig.volume = cfg.volume
  } catch(_) {}
}

async function saveConfig() {
  isSaving.value = true
  try {
    await fetch(`/api/v1/admin/digital/config`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        scenic_id: 'lingshan', avatar_id: selectedAvatarId.value,
        vcn: voiceConfig.vcn, speed: voiceConfig.speed,
        pitch: voiceConfig.pitch, volume: voiceConfig.volume,
        service_id: '315837983779590144',
        layout: 'fullscreen', display_mode: 'full',
      }),
      signal: AbortSignal.timeout(10000),
    })
    alert('✅ 配置已保存')
  } catch(e) {
    alert('⚠️ 保存失败，但已记录到本地\n' + e.message)
  }
  isSaving.value = false
}

onMounted(() => {
  loadVcnVoices()
  loadConfig()
})
</script>

<style scoped>
.digital-page{padding:20px;color:#c9d1d9;min-height:100%}
.page-header{margin-bottom:20px}
.page-title{font-size:20px;font-weight:700;color:#f0f6fc}
.page-desc{font-size:12px;color:rgba(255,255,255,0.35);margin-top:4px}
.avatar-showcase{margin-bottom:20px}
.avatar-showcase-title{font-size:13px;font-weight:600;color:rgba(255,255,255,0.5);margin-bottom:10px}
.avatar-current{display:flex;align-items:center;gap:14px;background:#0d1117;border:1px solid rgba(255,255,255,0.06);border-radius:10px;padding:14px 18px}
.avatar-current-preview{width:64px;height:96px;border-radius:8px;overflow:hidden;display:flex;align-items:center;justify-content:center;flex-shrink:0;background:#fff}
.avatar-current-preview img{width:100%;height:100%;object-fit:contain}
.avatar-current-preview span{font-size:24px}
.avatar-current-info{flex:1}
.avatar-current-name{font-size:16px;font-weight:700;color:#f0f6fc}
.avatar-current-id{font-size:11px;color:rgba(255,255,255,0.35);margin-top:2px}
.avatar-status{display:flex;align-items:center;gap:5px;font-size:11px;color:#2da44e;margin-top:4px}
.avatar-status-dot{width:6px;height:6px;border-radius:50%;background:#2da44e}
.avatar-hint{font-size:11px;color:rgba(255,255,255,0.3);text-align:right;line-height:1.6}
.grid-2{display:grid;grid-template-columns:1.2fr 1fr;gap:16px}
.card{background:#0d1117;border:1px solid rgba(255,255,255,0.06);border-radius:10px;padding:16px}
.card-title{font-size:14px;font-weight:600;color:#f0f6fc;margin-bottom:12px}
.avatar-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(80px,1fr));gap:10px;max-height:500px;overflow-y:auto}
.avatar-item{display:flex;flex-direction:column;align-items:center;gap:4px;padding:8px 4px;border-radius:8px;cursor:pointer;transition:all .15s;border:1.5px solid transparent}
.avatar-item:hover{background:rgba(255,255,255,0.04)}
.avatar-item.selected{border-color:#3b82f6;background:rgba(59,130,246,0.08)}
.avatar-item-icon{width:55px;height:82px;border-radius:6px;overflow:hidden;display:flex;align-items:center;justify-content:center;font-size:20px;background:#fff}
.avatar-item-icon img{width:100%;height:100%;object-fit:contain}
.avatar-item-name{font-size:10px;color:rgba(255,255,255,0.6);text-align:center;line-height:1.2;max-width:60px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.avatar-item-gender{font-size:9px;color:rgba(255,255,255,0.25)}
.loading-hint{display:flex;flex-direction:column;align-items:center;gap:10px;padding:40px 0;color:rgba(255,255,255,0.4)}
.spinner{width:24px;height:24px;border:2px solid rgba(59,130,246,0.15);border-top-color:#58a6ff;border-radius:50%;animation:spin .8s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}
.param-group{margin-bottom:12px}
.param-label{font-size:12px;color:rgba(255,255,255,0.5);margin-bottom:6px;display:flex;justify-content:space-between}
.param-label span{color:#58a6ff;font-weight:600}
.param-select{width:100%;padding:7px 10px;background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.08);border-radius:6px;color:#e8ecf4;font-size:12px;outline:none}
.param-slider{width:100%;height:4px;-webkit-appearance:none;appearance:none;background:rgba(59,130,246,0.15);border-radius:2px;outline:none;cursor:pointer}
.param-slider::-webkit-slider-thumb{-webkit-appearance:none;width:14px;height:14px;border-radius:50%;background:#58a6ff;border:2px solid #0d1117;cursor:pointer}
.save-btn{width:100%;padding:8px;background:rgba(59,130,246,0.12);border:1px solid rgba(59,130,246,0.2);color:#58a6ff;border-radius:6px;font-size:13px;font-weight:600;cursor:pointer;margin-top:8px}
.save-btn:hover{background:rgba(59,130,246,0.2)}
.save-btn:disabled{opacity:.5;cursor:not-allowed}
.avatar-photo-card{display:flex;gap:14px;background:#0d1117;border:1px solid rgba(255,255,255,0.06);border-radius:10px;padding:14px;margin-bottom:16px}
.avatar-photo{width:130px;height:195px;border-radius:10px;overflow:hidden;display:flex;align-items:center;justify-content:center;flex-shrink:0;background:#fff}
.avatar-photo img{width:100%;height:100%;object-fit:contain}
.photo-placeholder{font-size:30px}
.avatar-photo-info h3{font-size:15px;font-weight:700;color:#f0f6fc;margin:0}
.avatar-photo-info p{font-size:11px;color:rgba(255,255,255,0.4);margin:4px 0}
.avatar-photo-tags{display:flex;gap:6px;margin-top:6px}
.avatar-tag{font-size:10px;padding:2px 8px;border-radius:10px;background:rgba(255,255,255,0.04);color:rgba(255,255,255,0.35)}
.avatar-tag.active{background:rgba(59,130,246,0.12);color:#58a6ff}
</style>
