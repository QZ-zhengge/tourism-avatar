<template>
      <div class="knowledge-page">
        <div class="page-header"><div class="page-title">知识库</div></div>
        <div class="kno-layout">
          <div class="kno-left">
            <div class="kno-upload-panel">
              <div class="kno-upload-title">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                上传文档
              </div>
              <div class="kno-upload-row">
                <select v-model="newDocType"><option value="">文档分类...</option><option v-for="t in docTypeOptions" :key="t.value" :value="t.value">{{ t.label }}</option></select>
                <div class="kno-file-input-wrap">
                  <input type="file" @change="onFileChange" accept=".txt,.md,.docx,.pdf" />
                  <div class="kno-file-fake"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px;vertical-align:-2px;margin-right:2px"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg><span>{{ fileName || '选择文件' }}</span></div>
                </div>
              </div>
              <div style="display:flex;gap:8px">
                <button class="kno-upload-btn" style="flex:1" :disabled="!canUpload || previewing" @click="previewDoc"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px;vertical-align:-2px;margin-right:4px"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>预览切分</button>
                <button class="kno-upload-btn" style="flex:1;background:var(--green)" :disabled="!previewDone" @click="confirmUpload">✅ 确认存储</button>
              </div>
              <div v-if="previewing" class="kno-upload-status"><div class="kno-analysis-loading"></div><span>正在解析文档...</span></div>
              <div v-if="previewDone" class="kno-upload-status" style="color:var(--green)">✅ 预览完成，共 {{ previewChunks.length }} 个切块</div>
              <div v-if="uploadError" class="kno-upload-status" style="color:var(--red)">❌ {{ uploadError }}</div>
            </div>
            <div class="kno-list-panel">
              <div class="kno-list-header">
                <span class="kno-list-title"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align:-2px;margin-right:4px"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>已有文档</span>
                <span style="font-size:11px;color:var(--text-dim)">双击查看</span>
              </div>
              <div class="kno-filter-bar">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px;color:var(--text-dim);flex-shrink:0"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                <input type="text" placeholder="搜索文档名称..." v-model="knoSearch" />
                <select v-model="knoTypeFilter" style="width:70px;padding:3px 6px;border-radius:4px;border:1px solid var(--border);background:var(--bg);color:var(--text);font-size:11px;outline:none;cursor:pointer"><option value="">分类</option><option v-for="t in docTypeOptions" :key="t.value" :value="t.value">{{ t.label.replace(/^[^\s]+\s/, '') }}</option></select>
                <select v-model="knoExtFilter" style="width:70px;padding:3px 6px;border-radius:4px;border:1px solid var(--border);background:var(--bg);color:var(--text);font-size:11px;outline:none;cursor:pointer"><option value="">格式</option><option v-for="ext in docExtOptions" :key="ext" :value="ext">{{ ext.toUpperCase() }}</option></select>
              </div>
              <div class="kno-doc-scroll">
                <div class="kno-doc-item" v-for="d in filteredDocs" :key="d.id" :class="{active: selectedDocId === d.id}" @click="selectedDocId = d.id" @dblclick="viewDocDetail(d)">
                  <div class="kno-doc-icon-svg" :style="{background: docIconBg(d.type)}">
                    <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8" style="width:14px;height:14px">
                      <path v-if="d.type==='guide'" d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path v-if="d.type==='guide'" d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/>
                      <circle v-if="d.type==='history'" cx="12" cy="12" r="10"/><polyline v-if="d.type==='history'" points="12 6 12 12 16 14"/>
                      <circle v-if="d.type==='faq'" cx="12" cy="12" r="10"/><path v-if="d.type==='faq'" d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><line v-if="d.type==='faq'" x1="12" y1="17" x2="12.01" y2="17"/>
                      <path v-if="d.type==='notice'" d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path v-if="d.type==='notice'" d="M13.73 21a2 2 0 01-3.46 0"/>
                    </svg>
                  </div>
                  <div class="kno-doc-info">
                    <div class="kno-doc-name">{{ d.name }} <span v-if="d.stored" style="font-size:10px;color:var(--green)">●</span></div>
                    <div class="kno-doc-meta">{{ d.time }} · {{ d.size }} · {{ d.typeLabel.replace(/^[^\s]+\s/, '') }}</div>
                  </div>
                  <span class="kno-doc-tag" :class="getExt(d.name)">{{ getExt(d.name).toUpperCase() || '--' }}</span>
                  <button class="kno-doc-del" :title="'删除 ' + d.name" @click.stop="deleteDoc(d)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                  </button>
                </div>
                <div style="padding:24px;text-align:center;color:var(--text-dim);font-size:12px" v-if="filteredDocs.length===0">暂无文档</div>
              </div>
            </div>
          </div>

          <!-- ======== 右侧：文档播放器 ======== -->
          <div class="kno-right">

            <!-- 原始文件播放器 -->
            <template v-if="docViewerMode === 'original'">
              <div class="doc-viewer-header">
                <div class="doc-viewer-file-info">
                  <div class="doc-viewer-file-icon" :class="'kno-doc-tag ' + originalFileType">{{ originalFileType.toUpperCase() }}</div>
                  <div class="doc-viewer-file-meta">
                    <div class="doc-viewer-file-name">{{ currentViewDoc ? currentViewDoc.name : fileName }}</div>
                    <div class="doc-viewer-file-status">
                      <span v-if="docViewerBusy" style="color:var(--amber)">⏳ 正在解析文档...</span>
                      <span v-else-if="currentViewDoc" style="color:var(--cyan)">📖 文档内容预览（{{ currentViewDoc.chunks?.length || 0 }} 个知识块）</span>
                      <span v-else style="color:var(--green)">✅ 原文件预览</span>
                    </div>
                  </div>
                </div>
                <button class="photo-btn" @click="closeViewer" style="flex-shrink:0">✕ 关闭</button>
              </div>

              <!-- 工具栏 -->
              <div class="doc-toolbar">
                <div class="doc-toolbar-left">
                  <button class="doc-tb-btn" title="缩小" @click="changeZoom(-10)" :disabled="docZoomLevel <= 40">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
                  </button>
                  <span class="doc-zoom-label" @click="resetZoom" title="点击重置">{{ docZoomLevel }}%</span>
                  <button class="doc-tb-btn" title="放大" @click="changeZoom(10)" :disabled="docZoomLevel >= 200">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
                  </button>
                </div>
                <div class="doc-toolbar-right">
                  <button class="doc-tb-btn" title="全屏" @click="toggleFullscreen">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
                  </button>
                </div>
              </div>

              <!-- 文档内容区 -->
              <div class="doc-viewer-content" :style="{ fontSize: docZoomLevel + '%' }">
                <!-- PDF (vue-office-pdf) -->
                <vue-office-pdf v-if="originalFileType === 'pdf' && originalFileUrl"
                  :src="originalFileUrl"
                  style="width:100%;height:100%;overflow:auto"
                />

                <!-- DOCX (vue-office-docx) -->
                <vue-office-docx v-else-if="originalFileType === 'docx' && originalFileUrl"
                  :src="originalFileUrl"
                  style="width:100%;height:100%;overflow:auto"
                />

                <!-- TXT -->
                <div v-else-if="originalFileType === 'txt'" class="doc-text-view">
                  <div class="doc-text-wrap"><pre class="doc-txt-pre">{{ originalFileContent }}</pre></div>
                </div>

                <!-- MD -->
                <div v-else-if="originalFileType === 'md' && originalFileHtml" class="doc-text-view">
                  <div class="doc-md-body" v-html="originalFileHtml"></div>
                </div>

                <!-- 加载中 -->
                <div v-else-if="docViewerBusy" class="doc-loading">
                  <div class="kno-analysis-loading"></div>
                  <span>正在加载文档内容...</span>
                </div>

                <!-- 不支持 -->
                <div v-else class="kno-empty">
                  <div class="kno-empty-icon">📄</div>
                  <div class="kno-empty-text">暂不支持此文件格式的预览</div>
                  <div style="font-size:11px;margin-top:4px;opacity:0.6">请尝试其他格式或使用"预览切分"</div>
                </div>
              </div>
            </template>

            <!-- 切分预览 / 已有文档详情 -->
            <template v-else-if="docViewerMode === 'chunks'">
              <div class="doc-viewer-header">
                <div class="doc-viewer-file-info">
                  <div v-if="previewChunks.length > 0" style="display:flex;align-items:center;gap:10px">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2" style="flex-shrink:0"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                    <div>
                      <div class="doc-viewer-file-name">预览：{{ previewFileName }}</div>
                      <div class="doc-viewer-file-status">{{ previewChunks.length }} 个知识块 · {{ previewTotalChars }} 字</div>
                    </div>
                  </div>
                  <div v-else-if="currentViewDoc" style="display:flex;align-items:center;gap:10px">
                    <div class="kno-doc-icon-svg" :style="{background:docIconBg(currentViewDoc.type)}" style="width:34px;height:34px;border-radius:7px;flex-shrink:0">
                      <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8" style="width:16px;height:16px">
                        <path v-if="currentViewDoc.type==='guide'" d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path v-if="currentViewDoc.type==='guide'" d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/>
                        <circle v-if="currentViewDoc.type==='history'" cx="12" cy="12" r="10"/><polyline v-if="currentViewDoc.type==='history'" points="12 6 12 12 16 14"/>
                        <circle v-if="currentViewDoc.type==='faq'" cx="12" cy="12" r="10"/><path v-if="currentViewDoc.type==='faq'" d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><line v-if="currentViewDoc.type==='faq'" x1="12" y1="17" x2="12.01" y2="17"/>
                        <path v-if="currentViewDoc.type==='notice'" d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path v-if="currentViewDoc.type==='notice'" d="M13.73 21a2 2 0 01-3.46 0"/>
                      </svg>
                    </div>
                    <div>
                      <div class="doc-viewer-file-name">{{ currentViewDoc.name }}</div>
                      <div class="doc-viewer-file-status">{{ currentViewDoc.typeLabel }} · {{ currentViewDoc.time }} · {{ currentViewDoc.size }}</div>
                    </div>
                  </div>
                </div>
                <div style="display:flex;align-items:center;gap:8px">
                  <span style="font-size:11px;color:var(--text-dim)"><strong style="color:var(--cyan)">{{ previewChunks.length || (currentViewDoc?.chunks?.length || 0) }}</strong> 块 · <strong style="color:var(--cyan)">{{ previewChunks.length ? previewTotalChars : (currentViewDoc ? currentViewDoc.chunks.reduce((s, c) => s + (c.length || c.charCount || 0), 0) : 0) }}</strong> 字</span>
                  <button v-if="currentViewDoc" class="photo-btn" @click="exportDocMD(currentViewDoc)" style="display:flex;align-items:center;gap:4px">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>导出 MD
                  </button>
                  <button v-if="currentViewDoc" class="photo-btn" @click="deleteDoc(currentViewDoc)" style="display:flex;align-items:center;gap:4px;color:var(--red);border-color:rgba(239,68,68,0.3)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>删除
                  </button>
                  <button class="doc-tb-btn" title="全屏" @click="toggleFullscreen" style="width:28px;height:28px">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
                  </button>
                  <button class="photo-btn" @click="closeViewer" style="flex-shrink:0">✕ 关闭</button>
                </div>
              </div>

              <!-- 原文档折叠区（非 PDF/DOCX 时显示） -->
              <div v-if="originalFileContent && originalFileType !== 'pdf' && originalFileType !== 'docx'" class="doc-original-toggle">
                <div class="doc-original-toggle-header" @click="showOriginal = !showOriginal">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :style="{ transform: showOriginal ? 'rotate(90deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }"><polyline points="9 18 15 12 9 6"/></svg>
                  <span>原始文档</span>
                </div>
                <div v-if="showOriginal" class="doc-original-body" style="font-size:12px;line-height:1.8;padding:14px 16px;white-space:pre-wrap;background:var(--bg);border:1px solid var(--border);margin:0 16px 8px">{{ originalFileContent }}</div>
              </div>

              <!-- MD 切块内容 -->
              <div class="kno-chunk-scroll">
                <div class="kno-chunk" v-for="(chunk,i) in (previewChunks.length ? previewChunks : (currentViewDoc?.chunks || []))" :key="'c'+i">
                  <div class="kno-chunk-header">
                    <span style="font-weight:400;color:var(--text-dim);font-size:11px">{{ chunk.charCount || chunk.length || chunk.content?.length || chunk.text?.length || 0 }} 字</span>
                  </div>
                  <div class="kno-chunk-body kno-chunk-md-render" v-html="renderChunkMd(chunk.content || chunk.text || '')"></div>
                </div>
              </div>
            </template>

            <!-- 空状态 -->
            <div class="kno-empty" v-else>
              <div class="kno-empty-icon">📖</div>
              <div class="kno-empty-text">双击左侧文档查看切分详情</div>
              <div style="font-size:11px;margin-top:4px;opacity:0.6">或上传文档 (.pdf .txt .md .docx) 查看原文件</div>
            </div>
          </div>
        </div>
      </div>
</template>

<script setup>
import { ref, inject } from 'vue'
import VueOfficeDocx from '@vue-office/docx'
import VueOfficePdf from '@vue-office/pdf'
import { marked } from 'marked'

// 从父组件注入状态
const docList = inject('docList')
const selectedDocId = inject('selectedDocId')
const selectedDoc = inject('selectedDoc')
const newDocType = inject('newDocType')
const fileName = inject('fileName')
const canUpload = inject('canUpload')
const previewing = inject('previewing')
const previewDone = inject('previewDone')
const previewChunks = inject('previewChunks')
const previewFileName = inject('previewFileName')
const previewTotalChars = inject('previewTotalChars')
const totalChars = inject('totalChars')
const uploadError = inject('uploadError')
const knoSearch = inject('knoSearch')
const knoTypeFilter = inject('knoTypeFilter')
const knoExtFilter = inject('knoExtFilter')
const filteredDocs = inject('filteredDocs')
const docTypeOptions = inject('docTypeOptions')
const docExtOptions = inject('docExtOptions')
const docIconBg = inject('docIconBg')
const onFileChange = inject('onFileChange')
const previewDoc = inject('previewDoc')
const confirmUpload = inject('confirmUpload')
const clearPreview = inject('clearPreview')
const viewDocDetail = inject('viewDocDetail')
const exportDocMD = inject('exportDocMD')
const deleteDoc = inject('deleteDoc')

// 文档播放器
const docViewerMode = inject('docViewerMode')
const originalFileType = inject('originalFileType')
const originalFileContent = inject('originalFileContent')
const originalFileHtml = inject('originalFileHtml')
const originalFileUrl = inject('originalFileUrl')
const docZoomLevel = inject('docZoomLevel')
const docViewerBusy = inject('docViewerBusy')
const currentViewDoc = inject('currentViewDoc')
const changeZoom = inject('changeZoom')
const resetZoom = inject('resetZoom')
const toggleFullscreen = inject('toggleFullscreen')
const closeViewer = inject('closeViewer')

const showOriginal = ref(false)

function getExt(name) {
  const m = name.match(/\.(\w+)$/); return m ? m[1] : ''
}

// 使用 marked 标准 Markdown 渲染
function renderChunkMd(text) {
  if (!text) return ''
  // 去掉可能残留的人工标题（第 N 段 / Chunk N），多轮清洗
  let t = String(text).trim()
  let prev
  do {
    prev = t
    t = t.replace(/^[#*]{1,6}\s*(第\s*\d+\s*段|Chunk\s+\d+|第\s*\d+\s*块)\s*[*#]*\s*$/gm, '').trim()
    t = t.replace(/^\s*(第\s*\d+\s*段|Chunk\s+\d+|第\s*\d+\s*块)\s*$/gm, '').trim()
  } while (t !== prev)
  return marked.parse(t, { async: false })
}
</script>
