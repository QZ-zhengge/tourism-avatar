<template>
      <div class="content-page">
        <div class="page-header">
          <div class="page-title">内容管理</div>
          <div style="font-size:12px;color:var(--text-dim)">上传景点照片，展示在游客端热门景点页面</div>
        </div>
        <div class="cm-layout">
          <!-- 上传区 -->
          <div class="cm-upload">
            <div class="cm-upload-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
              添加热门景点照片
            </div>
            <div class="cm-form">
              <label class="cm-form-label">景点名称</label>
              <input type="text" v-model="cmSpotName" placeholder="例如：灵山大佛、九龙灌浴..." class="cm-input" />
            </div>
            <div class="cm-form">
              <label class="cm-form-label">景点图片</label>
              <div class="cm-upload-row">
                <div class="cm-file-wrap" style="flex:1">
                  <input type="file" accept="image/*" @change="cmOnFileChange" />
                  <div class="cm-file-fake">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;vertical-align:-3px;margin-right:4px"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                    {{ cmFileName || '选择图片' }}
                  </div>
                </div>
                <button class="cm-btn" :disabled="!cmCanAdd" @click="cmAddImage">添加</button>
              </div>
            </div>
            <div class="cm-preview" v-if="cmPreviewUrl">
              <img :src="cmPreviewUrl" class="cm-preview-thumb" @click="cmShowPreview = true" />
              <div class="cm-preview-hint">点击查看大图</div>
            </div>
            <!-- 图片预览弹窗 -->
            <div class="cm-modal" v-if="cmShowPreview && cmPreviewUrl" @click.self="cmShowPreview = false">
              <div class="cm-modal-body">
                <img :src="cmPreviewUrl" style="max-width:100%;height:auto;display:block" />
                <button class="cm-modal-close" @click="cmShowPreview = false">✕</button>
              </div>
            </div>
          </div>
          <!-- 图库 -->
          <div class="cm-gallery">
            <div class="cm-gallery-header">
              <span class="cm-gallery-title">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align:-2px;margin-right:4px"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                景点图库
              </span>
              <span style="font-size:11px;color:var(--text-dim)">共 {{ cmImages.length }} 张</span>
            </div>
            <div class="cm-grid">
              <div v-for="(img, i) in cmImages" :key="i" class="cm-card">
                <img :src="img.url" style="width:100%;max-height:100px;object-fit:contain;display:block;background:#0a0e27" />
                <div class="cm-card-spot">{{ img.spotName }}</div>
                <div class="cm-card-meta">{{ img.time }}</div>
                <div class="cm-card-actions">
                  <button class="cm-sm-btn" @click="cmCopyRef(img)">复制路径</button>
                  <button class="cm-sm-btn cm-del" @click="cmDelete(i)">删除</button>
                </div>
              </div>
            </div>
            <div class="cm-empty" v-if="cmImages.length === 0">
              <svg viewBox="0 0 24 24" fill="none" stroke="var(--text-dim)" stroke-width="1.5" style="width:40px;height:40px;opacity:0.3"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
              <div style="margin-top:8px;font-size:13px;color:var(--text-dim)">暂无景点照片，请上传</div>
            </div>
          </div>
        </div>
      </div>
</template>

<script setup>
import { inject } from 'vue'

// 从父组件注入状态
const cmSpotName = inject('cmSpotName')
const cmFileName = inject('cmFileName')
const cmPreviewUrl = inject('cmPreviewUrl')
const cmCanAdd = inject('cmCanAdd')
const cmImages = inject('cmImages')
const cmShowPreview = inject('cmShowPreview')
const cmOnFileChange = inject('cmOnFileChange')
const cmAddImage = inject('cmAddImage')
const cmCopyRef = inject('cmCopyRef')
const cmDelete = inject('cmDelete')
</script>
