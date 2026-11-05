<template>
      <div class="data-page">
        <div class="page-header"><div class="page-title">改进建议分析</div></div>
        <div class="da-page">

          <!-- 顶部指标卡（可点击跳转） -->
          <div class="da-metrics">
            <div class="da-metric urgent" @click="scrollToType('urgent')">
              <div class="da-metric-top">
                <span class="da-metric-value">{{ urgentItems.length }}</span>
              </div>
              <div class="da-metric-label">紧急问题 · 需立即处理</div>
            </div>
            <div class="da-metric improve" @click="scrollToType('improve')">
              <div class="da-metric-top">
                <span class="da-metric-value">{{ improveItems.length }}</span>
              </div>
              <div class="da-metric-label">服务改进 · 持续优化</div>
            </div>
            <div class="da-metric optimize" @click="scrollToType('optimize')">
              <div class="da-metric-top">
                <span class="da-metric-value">{{ optimizeItems.length }}</span>
              </div>
              <div class="da-metric-label">优化建议 · 可优化项</div>
            </div>
            <div class="da-metric keep" @click="scrollToType('keep')">
              <div class="da-metric-top">
                <span class="da-metric-value">{{ keepItems.length }}</span>
              </div>
              <div class="da-metric-label">正面保持 · 表现良好</div>
            </div>
          </div>

          <!-- 4 类卡片 -->
          <div class="da-cards-row" v-if="analysisLoaded">
            <div class="da-card-col">
              <div class="da-card">
                <div class="da-card-title">
                  <span class="da-card-title-bar" style="background:#ef4444"></span>
                  紧急问题 <span style="font-weight:400;font-size:11px;color:var(--text-dim);margin-left:auto">共 {{ urgentItems.length }} 条</span>
                </div>
                <div class="da-card-body">
                  <div v-for="(item, i) in urgentItems" :key="'u'+i"
                    class="da-row" :class="{open: isOpen(item)}" @click="toggleItem(item)">
                    <span class="da-row-badge" style="background:rgba(239,68,68,0.15);color:#ef4444">高</span>
                    <div class="da-row-text">
                      <div style="font-weight:500;margin-bottom:2px">{{ item.summary || item.text }}</div>
                      <div class="da-row-expand">
                        <div v-for="(val, key) in item.parsed" :key="key" style="margin-bottom:4px">
                          <span style="color:var(--cyan);font-weight:500">{{ key }}</span><br/>
                          <span style="color:var(--text-dim)">{{ val }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div style="padding:24px;text-align:center;color:var(--text-dim);font-size:12px" v-if="urgentItems.length === 0">✅ 暂无紧急问题</div>
                </div>
              </div>
              <div class="da-card">
                <div class="da-card-title">
                  <span class="da-card-title-bar" style="background:#f59e0b"></span>
                  服务改进 <span style="font-weight:400;font-size:11px;color:var(--text-dim);margin-left:auto">共 {{ improveItems.length }} 条</span>
                </div>
                <div class="da-card-body">
                  <div v-for="(item, i) in improveItems" :key="'m'+i"
                    class="da-row" :class="{open: isOpen(item)}" @click="toggleItem(item)">
                    <span class="da-row-badge" style="background:rgba(245,158,11,0.15);color:#f59e0b">中</span>
                    <div class="da-row-text">
                      <div style="font-weight:500;margin-bottom:2px">{{ item.summary || item.text }}</div>
                      <div class="da-row-expand">
                        <div v-for="(val, key) in item.parsed" :key="key" style="margin-bottom:4px">
                          <span style="color:var(--cyan);font-weight:500">{{ key }}</span><br/>
                          <span style="color:var(--text-dim)">{{ val }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div style="padding:24px;text-align:center;color:var(--text-dim);font-size:12px" v-if="improveItems.length === 0">✅ 暂无服务改进项</div>
                </div>
              </div>
            </div>
            <div class="da-card-col">
              <div class="da-card">
                <div class="da-card-title">
                  <span class="da-card-title-bar" style="background:#22c55e"></span>
                  优化建议 <span style="font-weight:400;font-size:11px;color:var(--text-dim);margin-left:auto">共 {{ optimizeItems.length }} 条</span>
                </div>
                <div class="da-card-body">
                  <div v-for="(item, i) in optimizeItems" :key="'o'+i"
                    class="da-row" :class="{open: isOpen(item)}" @click="toggleItem(item)">
                    <span class="da-row-badge" style="background:rgba(34,197,94,0.15);color:#22c55e">低</span>
                    <div class="da-row-text">
                      <div style="font-weight:500;margin-bottom:2px">{{ item.summary || item.text }}</div>
                      <div class="da-row-expand">
                        <div v-for="(val, key) in item.parsed" :key="key" style="margin-bottom:4px">
                          <span style="color:var(--cyan);font-weight:500">{{ key }}</span><br/>
                          <span style="color:var(--text-dim)">{{ val }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div style="padding:24px;text-align:center;color:var(--text-dim);font-size:12px" v-if="optimizeItems.length === 0">✅ 暂无优化建议</div>
                </div>
              </div>
              <div class="da-card">
                <div class="da-card-title">
                  <span class="da-card-title-bar" style="background:#3b82f6"></span>
                  正面保持 <span style="font-weight:400;font-size:11px;color:var(--text-dim);margin-left:auto">共 {{ keepItems.length }} 条</span>
                </div>
                <div class="da-card-body">
                  <div v-for="(item, i) in keepItems" :key="'k'+i"
                    class="da-row" :class="{open: isOpen(item)}" @click="toggleItem(item)">
                    <span class="da-row-badge" style="background:rgba(59,130,246,0.15);color:#3b82f6">保</span>
                    <div class="da-row-text">
                      <div style="font-weight:500;margin-bottom:2px">{{ item.summary || item.text }}</div>
                      <div class="da-row-expand">
                        <div v-for="(val, key) in item.parsed" :key="key" style="margin-bottom:4px">
                          <span style="color:var(--cyan);font-weight:500">{{ key }}</span><br/>
                          <span style="color:var(--text-dim)">{{ val }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div style="padding:24px;text-align:center;color:var(--text-dim);font-size:12px" v-if="keepItems.length === 0">✅ 暂无正面保持项</div>
                </div>
              </div>
            </div>
          </div>
          <div style="text-align:center;padding:40px;color:var(--text-dim);font-size:13px" v-else>
            <div style="width:28px;height:28px;border:2px solid var(--border);border-top-color:var(--cyan);border-radius:50%;animation:spin 0.8s linear infinite;margin:0 auto 12px"></div>
            正在分析对话数据...
          </div>
        </div>
      </div>
</template>

<script setup>
import { inject } from 'vue'

// 从父组件注入状态
const urgentItems = inject('urgentItems')
const improveItems = inject('improveItems')
const optimizeItems = inject('optimizeItems')
const keepItems = inject('keepItems')
const analysisLoaded = inject('analysisLoaded')
const expandedSet = inject('expandedSet')
const isOpen = inject('isOpen')
const toggleItem = inject('toggleItem')
const scrollToType = inject('scrollToType')
</script>
