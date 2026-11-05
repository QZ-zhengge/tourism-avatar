<template>
      <div class="dialogue-page">
        <div class="page-header"><div class="page-title">对话分析</div></div>
        <div class="dlg-layout">
          <!-- 用户列表 -->
          <div class="dlg-user-list">
            <div class="dlg-user-search">
              <input type="text" placeholder="搜索用户ID..." v-model="dlgSearch" />
            </div>
            <div class="dlg-user-scroll">
              <div style="padding:20px;text-align:center;color:var(--text-dim);font-size:12px" v-if="dlgLoading">加载中...</div>
              <div style="padding:20px;text-align:center;color:var(--red);font-size:12px" v-else-if="dlgError">{{ dlgError }}</div>
              <div style="padding:20px;text-align:center;color:var(--text-dim);font-size:12px" v-else-if="filteredUsers.length === 0">暂无对话分析</div>
              <div class="dlg-user-item"
                v-for="u in filteredUsers"
                :key="u.id"
                :class="{active: selectedUserId === u.id}"
                @click="selectUser(u)">
                <div class="dlg-user-avatar">👤</div>
                <div class="dlg-user-info">
                  <div class="dlg-user-name">{{ u.name }}</div>
                  <div class="dlg-user-id">{{ u.id }}</div>
                </div>
                <div class="dlg-user-meta">
                  <div class="dlg-user-time">{{ u.lastTime }}</div>
                  <span class="dlg-user-count">{{ u.msgCount }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 对话卡片区 -->
          <div class="dlg-conversation">
            <template v-if="selectedUserId && selectedUser.sessions">
              <div class="dlg-conv-header">
                <div class="dlg-conv-header-avatar">👤</div>
                <div class="dlg-conv-header-info">
                  <div class="dlg-conv-header-name">{{ selectedUser.name }}</div>
                  <div class="dlg-conv-header-id">{{ selectedUser.id }}</div>
                </div>
                <div class="dlg-conv-stats">
                  <span>共 <strong>{{ selectedUser.msgCount }}</strong> 轮对话</span>
                  <span>满意度 <strong>{{ selectedUser.satisfaction }}</strong></span>
                  <button class="dlg-export-btn" @click="exportDialogue">⬇ 导出分析</button>
                </div>
              </div>

              <div class="dlg-conv-cards">
                <!-- 每条对话一个卡片 -->
                <div class="dlg-card" v-for="(conv, ci) in selectedUser.sessions" :key="ci">
                  <div class="dlg-card-number">{{ ci + 1 }}</div>
                  <div class="dlg-card-body">
                    <!-- 配置：用户提问 -->
                    <div class="dlg-card-row dlg-card-conf">
                      <div class="dlg-card-tag dlg-card-tag--conf">用户</div>
                      <div class="dlg-card-content">
                        <div class="dlg-card-label">输入问题</div>
                        <div class="dlg-card-text">{{ conv.userQuery || '—' }}</div>
                        <div class="dlg-card-meta" v-if="conv.intent || conv.intentCategory">
                          <span v-if="conv.intent" class="dlg-card-chip">{{ conv.intent }}</span>
                          <span v-if="conv.intentCategory" class="dlg-card-chip dlg-card-chip--dim">{{ conv.intentCategory }}</span>
                        </div>
                      </div>
                    </div>

                    <!-- 候选人：数字人回答 -->
                    <div class="dlg-card-row dlg-card-cand">
                      <div class="dlg-card-tag dlg-card-tag--cand">数字人</div>
                      <div class="dlg-card-content">
                        <div class="dlg-card-label">数字人回复</div>
                        <div class="dlg-card-text">{{ conv.botAnswer || '—' }}</div>
                      </div>
                    </div>

                    <!-- 反分析：分析结果 -->
                    <div class="dlg-card-row dlg-card-reverse" v-if="conv.problemDiagnosis || conv.improvementSuggestion || conv.sentimentScore != null">
                      <div class="dlg-card-tag dlg-card-tag--reverse">数据分析</div>
                      <div class="dlg-card-content">
                        <!-- 准确率 -->
                        <div class="dlg-analysis-block" v-if="conv.sentimentScore != null">
                          <span class="dlg-analysis-key">【准确率】</span>
                          <span class="dlg-analysis-val">{{ conv.sentimentScore }}%</span>
                        </div>
                        <!-- 情感标签 -->
                        <div class="dlg-analysis-block" v-if="conv.sentimentLabel">
                          <span class="dlg-analysis-key">【情感】</span>
                          <span class="dlg-analysis-val" :class="emotionClass(conv.sentimentLabel)">{{ sentimentLabelMap[conv.sentimentLabel] || conv.sentimentLabel }}</span>
                        </div>
                        <!-- 核心问题 -->
                        <div class="dlg-analysis-block" v-if="conv.coreQuestion">
                          <span class="dlg-analysis-key">【核心问题】</span>
                          <span class="dlg-analysis-val">{{ conv.coreQuestion }}</span>
                        </div>
                        <!-- 问题诊断：应对 -->
                        <div class="dlg-analysis-block" v-if="conv.problemDiagnosis">
                          <span class="dlg-analysis-key">【应对】</span>
                          <span class="dlg-analysis-val">{{ conv.problemDiagnosis }}</span>
                        </div>
                        <!-- 改进建议 -->
                        <div class="dlg-analysis-block" v-if="conv.improvementSuggestion">
                          <span class="dlg-analysis-key">【改进建议】</span>
                          <div class="dlg-analysis-val">
                            <div class="dlg-suggestion-items">
                              <div class="dlg-suggestion-item" v-for="(line, li) in parseSuggestions(conv.improvementSuggestion)" :key="li">
                                {{ line }}
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <div class="dlg-empty" v-else>
              <div class="dlg-empty-icon">💬</div>
              <div class="dlg-empty-text">请从左侧选择一位用户</div>
              <div class="dlg-empty-sub">查看对话详情与分析</div>
            </div>
          </div>
        </div>
      </div>
</template>

<script setup>
import { inject } from 'vue'

const dlgSearch = inject('dlgSearch')
const dlgLoading = inject('dlgLoading')
const dlgError = inject('dlgError')
const filteredUsers = inject('filteredUsers')
const selectedUserId = inject('selectedUserId')
const selectedUser = inject('selectedUser')
const selectUser = inject('selectUser')
const exportDialogue = inject('exportDialogue')

const sentimentLabelMap = {
  positive: '正向 😊',
  negative: '负向 😟',
  neutral: '中性 😐',
}

function emotionClass(label) {
  if (label === 'positive') return 'dlg-emotion-positive'
  if (label === 'negative') return 'dlg-emotion-negative'
  return 'dlg-emotion-neutral'
}

function parseSuggestions(text) {
  if (!text) return []
  // 尝试按数字序号分割（支持 1. 1、 1) 1） 等格式）
  const parts = text.split(/(\d+[\.、\)）]\s*)/)
  if (parts.length <= 1) {
    // 尝试按换行分割
    const newlines = text.split(/[\n\r]+/).filter(l => l.trim())
    return newlines.length > 1 ? newlines.map(l => l.replace(/^[-\s\d\.、\)）]+/, '').trim()).filter(Boolean) : [text]
  }
  const items = []
  for (let i = 1; i < parts.length; i += 2) {
    const label = parts[i]?.trim() || ''
    const content = (parts[i + 1] || '').trim()
    if (content) items.push(label + ' ' + content)
  }
  return items.length > 0 ? items : [text]
}
</script>
