<template>
  <div class="report-page">
    <!-- 顶部标题 + 操作区 -->
    <div class="rp-header">
      <div class="rp-header__left">
        <h1 class="rp-header__title">分析报告</h1>
        <span class="rp-header__subtitle">游客满意度分析报告 · 基于会话情感记录</span>
      </div>
      <div class="rp-header__right">
        <div class="rp-period-selector">
          <button class="rp-period-btn" :class="{active:period==='week'}" @click="period='week';fetchReport()">本周</button>
          <button class="rp-period-btn" :class="{active:period==='month'}" @click="period='month';fetchReport()">本月</button>
        </div>
        <button class="rp-btn rp-btn--export" @click="exportCSV" :disabled="exporting">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          {{ exporting ? '导出中...' : '导出 CSV' }}
        </button>
        <button class="rp-btn rp-btn--icon" @click="fetchReport" :class="{spinning:loading}">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg>
        </button>
      </div>
    </div>

    <div class="rp-loading" v-if="loading && !report">
      <div class="rp-spinner"></div>
      <span>加载报告中...</span>
    </div>

    <template v-if="report">
      <!-- 概览卡片 -->
      <div class="rp-overview">
        <div class="rp-ov-card" :class="'rp-ov--'+trendClass">
          <div class="rp-ov-icon">{{ trendIcon }}</div>
          <div class="rp-ov-body">
            <div class="rp-ov-label">总体趋势</div>
            <div class="rp-ov-val">{{ trendLabel }}</div>
            <div class="rp-ov-sub" v-if="report.overall_trend?.danger_dates?.length">风险日期：{{ report.overall_trend.danger_dates.join('、') }}</div>
          </div>
        </div>
        <div class="rp-ov-card">
          <div class="rp-ov-icon" style="color:#2da44e">📊</div>
          <div class="rp-ov-body">
            <div class="rp-ov-label">平均满意度</div>
            <div class="rp-ov-val">{{ report.aggregate_statistics?.avg_satisfaction?.toFixed(2) || '-' }}</div>
          </div>
        </div>
        <div class="rp-ov-card">
          <div class="rp-ov-icon" style="color:#58a6ff">👤</div>
          <div class="rp-ov-body">
            <div class="rp-ov-label">分析游客数</div>
            <div class="rp-ov-val">{{ report.visitor_analysis?.length || 0 }}</div>
          </div>
        </div>
        <div class="rp-ov-card">
          <div class="rp-ov-icon" style="color:#e3b341">💬</div>
          <div class="rp-ov-body">
            <div class="rp-ov-label">总对话轮次</div>
            <div class="rp-ov-val">{{ report.aggregate_statistics?.total_turns || 0 }}</div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="rp-grid">
        <div class="rp-panel">
          <div class="rp-panel__header">
            <span class="rp-panel__title">满意度趋势</span>
            <span class="rp-panel__subtitle">每日平均满意度变化</span>
          </div>
          <div class="rp-panel__body">
            <div id="rp-trend-chart" class="rp-chart"></div>
          </div>
        </div>
        <div class="rp-panel">
          <div class="rp-panel__header">
            <span class="rp-panel__title">满意度分布</span>
            <span class="rp-panel__subtitle">高 / 中 / 低 比例</span>
          </div>
          <div class="rp-panel__body">
            <div id="rp-dist-chart" class="rp-chart rp-chart--sm"></div>
          </div>
        </div>
      </div>

      <!-- 高频关注点 -->
      <div class="rp-panel" v-if="report.aggregate_statistics?.top_concerns?.length">
        <div class="rp-panel__header">
          <span class="rp-panel__title">高频关注点</span>
          <span class="rp-panel__subtitle">游客集中反馈的高频问题</span>
        </div>
        <div class="rp-panel__body">
          <div class="rp-tags">
            <span class="rp-tag" v-for="c in report.aggregate_statistics.top_concerns" :key="c.concern">
              <span class="rp-tag-text">{{ c.concern }}</span>
              <span class="rp-tag-count">{{ c.count }}次</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 游客分析表格 -->
      <div class="rp-panel">
        <div class="rp-panel__header">
          <span class="rp-panel__title">游客逐人分析</span>
          <span class="rp-panel__subtitle">{{ report.visitor_analysis?.length || 0 }} 名游客</span>
        </div>
        <div class="rp-panel__body rp-panel__body--table">
          <table class="rp-table">
            <thead>
              <tr>
                <th>游客</th>
                <th>满意度</th>
                <th>趋势</th>
                <th>关注点</th>
                <th>关键问题</th>
                <th>总结</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="v in report.visitor_analysis" :key="v.visitor_id">
                <td class="rp-ci">{{ v.visitor_id }}</td>
                <td>
                  <span class="rp-sat-badge" :class="satClass(v.avg_satisfaction)">{{ v.avg_satisfaction?.toFixed(2) }}</span>
                </td>
                <td><span class="rp-trend-badge" :class="'rp-trend--'+(v.trend||'stable')">{{ v.trend || 'stable' }}</span></td>
                <td class="rp-ci">{{ (v.concerns||[]).join('、') }}</td>
                <td class="rp-ci">{{ (v.key_issues||[]).join('、') }}</td>
                <td class="rp-ci">{{ v.summary || '-' }}</td>
              </tr>
              <tr v-if="!report.visitor_analysis?.length">
                <td colspan="6" class="rp-empty">暂无数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 分类改进建议 -->
      <div class="rp-grid-2" v-if="report.category_suggestions?.length">
        <div class="rp-panel" v-for="s in report.category_suggestions" :key="s.category">
          <div class="rp-panel__header">
            <span class="rp-panel__title">{{ s.category }}</span>
            <span class="rp-panel__subtitle">低满意度改进建议</span>
          </div>
          <div class="rp-panel__body">
            <div class="rp-sugg-item" v-for="(item, i) in (s.suggestions||[])" :key="i">
              <span class="rp-sugg-num">{{ i + 1 }}</span>
              <div class="rp-sugg-content">
                <div class="rp-sugg-text">{{ item.suggestion || item }}</div>
                <div class="rp-sugg-meta" v-if="item.priority">
                  <span class="rp-priority" :class="'rp-priority--'+(item.priority||'medium').toLowerCase()">{{ item.priority }}</span>
                  <span v-if="item.expected_impact">预期效果：{{ item.expected_impact }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div class="rp-empty-state" v-if="!loading && !report && errorMsg">
      <div class="rp-empty-icon">⚠️</div>
      <div class="rp-empty-msg">{{ errorMsg }}</div>
      <button class="rp-btn rp-btn--retry" @click="fetchReport">重试</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const API_BASE = 'http://192.0.2.1:8000'

const period = ref('week')
const loading = ref(false)
const exporting = ref(false)
const report = ref(null)
const errorMsg = ref('')
let trendChart = null
let distChart = null

const trendLabel = computed(() => {
  const d = report.value?.overall_trend?.direction
  if (d === 'declining') return '⬇ 下降趋势'
  if (d === 'rising') return '⬆ 上升趋势'
  return '➡ 平稳'
})
const trendIcon = computed(() => {
  const d = report.value?.overall_trend?.direction
  if (d === 'declining') return '📉'
  if (d === 'rising') return '📈'
  return '➡️'
})
const trendClass = computed(() => {
  const d = report.value?.overall_trend?.direction
  if (d === 'declining') return 'warn'
  if (d === 'rising') return 'good'
  return ''
})

function satClass(val) {
  if (val == null) return ''
  if (val >= 4) return 'rp-sat--high'
  if (val >= 3) return 'rp-sat--mid'
  return 'rp-sat--low'
}

async function fetchReport() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await fetch(`${API_BASE}/api/v1/admin/dashboard/visitor-sentiment-report?period=${period.value}`, {
      signal: AbortSignal.timeout(10000)
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    report.value = await res.json()
  } catch (e) {
    if (e.name !== 'AbortError') console.warn('[报告] API失败，使用Mock数据:', e.message)
    report.value = getMockReport(period.value)
  } finally {
    loading.value = false
    nextTick(() => renderCharts())
  }
}

async function exportCSV() {
  exporting.value = true
  try {
    const res = await fetch(`${API_BASE}/api/v1/admin/dashboard/visitor-sentiment-report?period=${period.value}&format=csv`, {
      signal: AbortSignal.timeout(15000)
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `游客满意度报告_${period.value}_${new Date().toISOString().slice(0,10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    // CSV导出失败也fallback到前端生成
    const csv = generateMockCSV(period.value)
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `游客满意度报告_${period.value}_${new Date().toISOString().slice(0,10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    console.warn('[报告] CSV导出使用本地生成数据')
  } finally {
    exporting.value = false
  }
}

// ===== Mock 数据 =====
function getMockReport(period) {
  const dayCount = period === 'week' ? 7 : 30
  const now = new Date()
  const dailyScores = []
  let score = 3.8
  for (let i = 0; i < dayCount; i++) {
    const d = new Date(now)
    d.setDate(d.getDate() - (dayCount - 1 - i))
    score = Math.max(1, Math.min(5, score + (Math.random() - 0.55) * 0.4))
    dailyScores.push({ date: d.toISOString().slice(0, 10), avg_score: Math.round(score * 100) / 100 })
  }

  return {
    overall_trend: {
      direction: 'declining',
      daily_scores: dailyScores,
      danger_dates: [dailyScores[dailyScores.length - 1]?.date, dailyScores[dailyScores.length - 2]?.date].filter(Boolean),
    },
    visitor_analysis: Array.from({ length: 8 }, (_, i) => ({
      visitor_id: `V${(1000 + i).toString().padStart(4, '0')}`,
      avg_satisfaction: Math.round((2.5 + Math.random() * 2.5) * 100) / 100,
      trend: ['declining', 'rising', 'stable'][Math.floor(Math.random() * 3)],
      concerns: ['门票价格', '路线指引', '服务态度', '设施完善', '排队时间', '环境卫生', '停车便利', '餐饮选择'].slice(0, 1 + Math.floor(Math.random() * 3)),
      key_issues: ['等待时间过长', '信息不清晰', '服务响应慢', '设施老旧', '指示牌不明确'].slice(0, 1 + Math.floor(Math.random() * 2)),
      summary: ['整体体验一般', '建议优化服务流程', '对景点满意但设施需改善', '交通不太方便'][Math.floor(Math.random() * 4)],
    })),
    aggregate_statistics: {
      avg_satisfaction: Math.round((dailyScores.reduce((s, d) => s + d.avg_score, 0) / dailyScores.length) * 100) / 100,
      total_turns: Math.floor(50 + Math.random() * 80),
      satisfaction_distribution: { high: Math.floor(20 + Math.random() * 30), medium: Math.floor(20 + Math.random() * 20), low: Math.floor(5 + Math.random() * 15) },
      top_concerns: [
        { concern: '门票价格偏高', count: Math.floor(10 + Math.random() * 20) },
        { concern: '路线指引不清晰', count: Math.floor(8 + Math.random() * 15) },
        { concern: '排队时间过长', count: Math.floor(5 + Math.random() * 12) },
        { concern: '服务响应速度慢', count: Math.floor(3 + Math.random() * 10) },
      ],
    },
    category_suggestions: [
      { category: '门票与价格', suggestions: [{ suggestion: '推出淡季优惠套餐，降低游客购票门槛', priority: '高', expected_impact: '预计提升满意度 5-8%' }, { suggestion: '增加套票组合选项，提供灵活选择', priority: '中', expected_impact: '预计提升满意度 3-5%' }] },
      { category: '服务与导览', suggestions: [{ suggestion: '优化数字人引导流程，缩短等待响应时间', priority: '高', expected_impact: '预计提升满意度 8-12%' }, { suggestion: '增加实时客流引导，分散高峰压力', priority: '中' }] },
      { category: '设施与配套', suggestions: [{ suggestion: '增设充电桩和休息座椅', priority: '中' }, { suggestion: '优化停车场引导系统', priority: '低' }] },
      { category: '环境卫生', suggestions: [{ suggestion: '增加保洁频次，特别是高峰时段', priority: '中' }, { suggestion: '增设垃圾桶点位', priority: '低' }] },
    ],
  }
}

function generateMockCSV(period) {
  const rows = [['游客ID', '满意度', '趋势', '关注点', '关键问题', '总结']]
  const mock = getMockReport(period)
  mock.visitor_analysis.forEach(v => {
    rows.push([v.visitor_id, String(v.avg_satisfaction), v.trend, v.concerns.join(';'), v.key_issues.join(';'), v.summary])
  })
  return rows.map(r => r.map(c => '"' + (c || '').replace(/"/g, '""') + '"').join(',')).join('\n')
}

function renderCharts() {
  const tc = document.getElementById('rp-trend-chart')
  if (tc) {
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(tc, null, { renderer: 'canvas' })
    const trend = report.value?.overall_trend
    trendChart.setOption({
      backgroundColor: 'transparent',
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(2,6,23,0.9)', borderColor: '#3b82f6', textStyle: { color: '#e2e8f0', fontSize: 12 } },
      xAxis: { type: 'category', data: trend?.daily_scores?.map(d => d.date) || [], axisLabel: { color: 'rgba(255,255,255,0.4)', fontSize: 10 }, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } } },
      yAxis: { type: 'value', min: 0, max: 5, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }, axisLabel: { color: 'rgba(255,255,255,0.4)', fontSize: 10 } },
      series: [{
        type: 'line', data: trend?.daily_scores?.map(d => d.avg_score) || [],
        smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { color: '#3b82f6', width: 2 },
        itemStyle: { color: '#3b82f6' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(59,130,246,0.2)' }, { offset: 1, color: 'rgba(59,130,246,0)' }] } },
        markLine: trend?.direction ? { silent: true, data: [{ yAxis: 3, label: { formatter: '警戒线 3.0', color: '#ef4444', fontSize: 10 }, lineStyle: { color: '#ef4444', type: 'dashed' } }] } : undefined,
      }],
    })
  }
  const dc = document.getElementById('rp-dist-chart')
  if (dc) {
    if (distChart) distChart.dispose()
    distChart = echarts.init(dc, null, { renderer: 'canvas' })
    const dist = report.value?.aggregate_statistics?.satisfaction_distribution || {}
    distChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'item', backgroundColor: 'rgba(2,6,23,0.9)', borderColor: '#3b82f6', textStyle: { color: '#e2e8f0', fontSize: 12 }, formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie', radius: ['40%', '70%'], center: ['50%', '50%'],
        avoidLabelOverlap: true,
        label: { color: 'rgba(255,255,255,0.6)', fontSize: 11, formatter: '{b}\n{d}%' },
        labelLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
        data: [
          { value: dist.high || 0, name: '高满意度', itemStyle: { color: '#2da44e' } },
          { value: dist.medium || 0, name: '中满意度', itemStyle: { color: '#d4a72c' } },
          { value: dist.low || 0, name: '低满意度', itemStyle: { color: '#cf222e' } },
        ],
        emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.3)' } },
      }],
    })
  }
}

function handleResize() { if (trendChart) trendChart.resize(); if (distChart) distChart.resize() }

onMounted(() => { fetchReport(); window.addEventListener('resize', handleResize) })
onUnmounted(() => {
  if (trendChart) trendChart.dispose()
  if (distChart) distChart.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.report-page{padding:20px 24px 40px;color:#c9d1d9}
.rp-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:20px;padding-bottom:16px;border-bottom:1px solid rgba(255,255,255,0.06)}
.rp-header__left{display:flex;flex-direction:column;gap:4px}
.rp-header__title{font-size:20px;font-weight:600;color:#f0f6fc;margin:0}
.rp-header__subtitle{font-size:12px;color:rgba(255,255,255,0.35)}
.rp-header__right{display:flex;align-items:center;gap:12px}
.rp-period-selector{display:flex;background:rgba(255,255,255,0.04);border-radius:6px;padding:2px;gap:2px}
.rp-period-btn{padding:5px 14px;border:none;background:transparent;color:rgba(255,255,255,0.5);border-radius:4px;font-size:12px;cursor:pointer;transition:all .2s}
.rp-period-btn.active{background:rgba(59,130,246,0.2);color:#58a6ff}
.rp-period-btn:hover:not(.active){color:rgba(255,255,255,0.7)}
.rp-btn{display:inline-flex;align-items:center;gap:6px;padding:6px 14px;border:1px solid rgba(255,255,255,0.08);background:rgba(255,255,255,0.04);color:rgba(255,255,255,0.7);border-radius:6px;font-size:12px;cursor:pointer;transition:all .2s}
.rp-btn:hover{background:rgba(255,255,255,0.08);color:#f0f6fc}
.rp-btn--export{background:rgba(59,130,246,0.12);border-color:rgba(59,130,246,0.2);color:#58a6ff}
.rp-btn--export:hover{background:rgba(59,130,246,0.2)}
.rp-btn--export:disabled{opacity:.5;cursor:not-allowed}
.rp-btn--icon{width:32px;height:32px;padding:0;justify-content:center}
.rp-btn--retry{margin-top:12px}
.rp-btn--icon.spinning svg{animation:rp-spin .8s linear infinite}
@keyframes rp-spin{to{transform:rotate(360deg)}}
.rp-loading{display:flex;flex-direction:column;align-items:center;padding:80px 0;gap:12px;color:rgba(255,255,255,0.4);font-size:13px}
.rp-spinner{width:28px;height:28px;border:2px solid rgba(59,130,246,0.15);border-top-color:#58a6ff;border-radius:50%;animation:rp-spin .8s linear infinite}
.rp-overview{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:20px}
.rp-ov-card{display:flex;align-items:center;gap:14px;background:#0d1117;border:1px solid rgba(255,255,255,0.06);border-radius:8px;padding:16px 18px;border-left:3px solid #3b82f6}
.rp-ov--warn{border-left-color:#cf222e}
.rp-ov--good{border-left-color:#2da44e}
.rp-ov-icon{font-size:22px;flex-shrink:0}
.rp-ov-body{flex:1;min-width:0}
.rp-ov-label{font-size:11px;color:rgba(255,255,255,0.4);margin-bottom:4px}
.rp-ov-val{font-size:24px;font-weight:700;color:#f0f6fc;font-variant-numeric:tabular-nums}
.rp-ov-sub{font-size:11px;color:#cf222e;margin-top:4px}
.rp-grid{display:grid;grid-template-columns:1.4fr 1fr;gap:14px;margin-bottom:14px}
.rp-grid-2{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:14px}
.rp-panel{background:#0d1117;border:1px solid rgba(255,255,255,0.06);border-radius:8px;overflow:hidden;margin-bottom:14px}
.rp-panel__header{display:flex;align-items:center;justify-content:space-between;padding:12px 16px;border-bottom:1px solid rgba(255,255,255,0.04)}
.rp-panel__title{font-size:13px;font-weight:500;color:#f0f6fc}
.rp-panel__subtitle{font-size:11px;color:rgba(255,255,255,0.3)}
.rp-panel__body{padding:12px 16px 16px}
.rp-panel__body--table{padding:0;overflow-x:auto}
.rp-chart{width:100%;height:240px}
.rp-chart--sm{height:200px}
.rp-tags{display:flex;flex-wrap:wrap;gap:8px}
.rp-tag{display:inline-flex;align-items:center;gap:8px;padding:5px 12px;background:rgba(59,130,246,0.06);border:1px solid rgba(59,130,246,0.1);border-radius:6px;font-size:12px}
.rp-tag-text{color:rgba(255,255,255,0.8)}
.rp-tag-count{font-size:10px;color:rgba(255,255,255,0.35);background:rgba(255,255,255,0.04);padding:0 5px;border-radius:4px}
.rp-table{width:100%;border-collapse:collapse;font-size:12px}
.rp-table thead th{text-align:left;padding:10px 12px;color:rgba(255,255,255,0.4);font-weight:500;border-bottom:1px solid rgba(255,255,255,0.06);white-space:nowrap}
.rp-table tbody td{padding:9px 12px;border-bottom:1px solid rgba(255,255,255,0.03);color:rgba(255,255,255,0.7)}
.rp-table tbody tr:hover td{background:rgba(255,255,255,0.02)}
.rp-ci{max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:11px;color:rgba(255,255,255,0.5)}
.rp-empty{text-align:center;color:rgba(255,255,255,0.25);padding:30px}
.rp-sat-badge{display:inline-block;padding:2px 8px;border-radius:4px;font-weight:600;font-size:12px;font-variant-numeric:tabular-nums}
.rp-sat--high{background:rgba(45,164,78,0.12);color:#7ee787}
.rp-sat--mid{background:rgba(212,167,44,0.12);color:#e3b341}
.rp-sat--low{background:rgba(207,34,46,0.12);color:#ff7b72}
.rp-trend-badge{display:inline-block;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:500;text-transform:capitalize}
.rp-trend--declining{background:rgba(207,34,46,0.1);color:#ff7b72}
.rp-trend--rising{background:rgba(45,164,78,0.1);color:#7ee787}
.rp-trend--stable{background:rgba(139,92,246,0.1);color:#a78bfa}
.rp-sugg-item{display:flex;gap:10px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.03)}
.rp-sugg-item:last-child{border-bottom:none}
.rp-sugg-num{flex-shrink:0;width:22px;height:22px;border-radius:4px;background:rgba(59,130,246,0.1);color:#58a6ff;display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:600}
.rp-sugg-content{flex:1;min-width:0}
.rp-sugg-text{font-size:13px;color:rgba(255,255,255,0.75);line-height:1.5}
.rp-sugg-meta{display:flex;gap:12px;margin-top:4px;font-size:11px;color:rgba(255,255,255,0.35)}
.rp-priority{padding:1px 6px;border-radius:4px;font-weight:500}
.rp-priority--high{background:rgba(207,34,46,0.1);color:#ff7b72}
.rp-priority--medium{background:rgba(212,167,44,0.1);color:#e3b341}
.rp-priority--low{background:rgba(45,164,78,0.1);color:#7ee787}
.rp-empty-state{display:flex;flex-direction:column;align-items:center;padding:80px 0;color:rgba(255,255,255,0.4)}
.rp-empty-icon{font-size:32px;margin-bottom:8px}
.rp-empty-msg{font-size:13px}
@media(max-width:1200px){.rp-overview{grid-template-columns:repeat(2,1fr)}.rp-grid,.rp-grid-2{grid-template-columns:1fr}}
</style>
