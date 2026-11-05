<template>
      <div class="settings-page">
        <div class="page-header"><div class="page-title">系统设置</div></div>
        <div class="card">
          <div class="settings-group">
            <div class="settings-group-title">服务配置</div>
            <div class="setting-row">
              <div>
                <div class="setting-info-label">VMS 服务地址</div>
                <div class="setting-info-desc">vms.cn-huadong-1.xf-yun.com</div>
              </div>
              <div style="font-size:12px;color:var(--text-dim)">已配置</div>
            </div>
            <div class="setting-row">
              <div>
                <div class="setting-info-label">讯飞 APP_ID</div>
                <div class="setting-info-desc">17f03646</div>
              </div>
              <div style="font-size:12px;color:var(--text-dim)">已配置</div>
            </div>
            <div class="setting-row">
              <div>
                <div class="setting-info-label">游客端轮询间隔</div>
                <div class="setting-info-desc">每5秒检查配置更新</div>
              </div>
              <div :class="['toggle', { on: toggleAuto }]" @click="toggleAuto = !toggleAuto"></div>
            </div>
          </div>
          <div class="settings-group">
            <div class="settings-group-title">通知</div>
            <div class="setting-row">
              <div>
                <div class="setting-info-label">配置变更通知</div>
                <div class="setting-info-desc">数字人配置变更时通知游客端</div>
              </div>
              <div :class="['toggle', { on: toggleAuto }]" @click="toggleAuto = !toggleAuto"></div>
            </div>
            <div class="setting-row">
              <div>
                <div class="setting-info-label">数据异常告警</div>
                <div class="setting-info-desc">负面情绪超过阈值时告警</div>
              </div>
              <div :class="['toggle', { on: toggleAuto }]" @click="toggleAuto = !toggleAuto"></div>
            </div>
          </div>
        </div>
        <div class="card" style="margin-top:20px;padding:16px;text-align:center;">
          <a href="#" target="_blank" style="color:var(--text-dim);font-size:11px;text-decoration:none;transition:color 0.2s"
             @click.prevent="openIcp"
             @mouseenter="$event.target.style.color='var(--accent)'"
             @mouseleave="$event.target.style.color='var(--text-dim)'">
            吉ICP备2026003752号-2
          </a>
        </div>
      </div>
</template>

<script setup>
import { inject } from 'vue'

// 从父组件注入状态
const toggleAuto = inject('toggleAuto')

function openIcp() {
  window.open('https://beian.miit.gov.cn/', '_blank')
}
</script>
