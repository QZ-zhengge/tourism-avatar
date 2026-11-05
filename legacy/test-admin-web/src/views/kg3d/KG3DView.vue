<template>
  <div class="kg-workbench">
    <div class="kg-header">
      <div class="kg-header-left">
        <div class="kg-logo">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>
          <span>图谱工作台</span>
        </div>
        <div class="kg-version-badge">3D</div>
      </div>
      <div class="kg-header-center">
        <div class="kg-tabs" ref="tabsRef">
          <button v-for="(tab, i) in tabs" :key="i" class="kg-tab" :class="{active:activeTab===i}" @click="switchTab(i)">
            <span class="kg-tab-icon" v-html="tab.icon"></span>
            <span>{{ tab.label }}</span>
            <span v-if="tab.label==='图谱编辑'" class="kg-tab-badge">{{ allNodes.length }}</span>
          </button>
          <div class="kg-tab-indicator" :style="{left:tiL+'px',width:tiW+'px'}"></div>
        </div>
      </div>
    </div>
    <div class="kg-content">
      <!-- TAB1: 图谱总览 -->
      <div class="kg-tab-panel" v-show="activeTab===0">
        <div class="kg3d-layout">
          <div class="kg3d-canvas-wrap">
            <div id="kg3d-container"></div>
            <div class="kg3d-labels" id="kg3d-labels"></div>
            <div class="kg3d-loader" v-if="graphLoading"><div class="kg-loader"></div><span>加载 3D 图谱...</span></div>
            <div class="kg3d-toolbar-left">
              <div class="kg3d-tool-section">
                <div class="kg3d-ts-title">类型</div>
                <label class="kg3d-ts-item" :class="{disabled:!t.checked}" v-for="t in nodeTypeFilters" :key="t.value">
                  <input type="checkbox" v-model="t.checked" @change="rebuildGraph" />
                  <span class="kg3d-dot" :style="{background:t.color}"></span>
                  <span>{{ t.short }}</span>
                </label>
              </div>
              <div class="kg3d-tool-section" style="display:flex;gap:3px">
                <button class="kg3d-tool-btn" @click="resetCamera">⊞</button>
                <button class="kg3d-tool-btn" @click="exportGraph3D">⬇</button>
              </div>
            </div>
            <div class="kg3d-search-overlay" v-if="searchResult">
              <div class="kg3d-sr-answer">{{ searchResult.answer }}</div>
              <button @click="clearSearch">✕</button>
            </div>
          </div>
          <div class="kg3d-side" :class="{open:rightOpen}">
            <div class="kg3d-side-inner" v-if="rightOpen">
              <div class="kg3d-side-hd">
                <div class="kg3d-side-av" :style="{background:nodeColor(selectedNode.type)}">{{ (selectedNode.label||'N').charAt(0) }}</div>
                <div class="kg3d-side-n">{{ selectedNode.label||selectedNode.id }}</div>
                <span class="kg3d-tt" :style="{background:nodeColor(selectedNode.type)}">{{ selectedNode.type }}</span>
              </div>
              <div class="kg3d-side-bd">
                <div class="kg3d-side-grp">
                  <div class="kg3d-side-gt">属性</div>
                  <div class="kg3d-pr" v-for="(v,k) in detailProps" :key="k">
                    <span class="kg3d-pk">{{ k }}</span><span class="kg3d-pv">{{ typeof v==='object'?JSON.stringify(v,null,2):String(v) }}</span>
                  </div>
                  <div v-if="!detailProps" class="kg3d-se">暂无</div>
                </div>
                <div class="kg3d-side-grp">
                  <div class="kg3d-side-gt">一度关系 ({{ relatedEdges.length }})</div>
                  <div class="kg3d-rr" v-for="re in relatedEdges" :key="re.id">
                    <span>↔</span><span>{{ getNeighbor(re).label||getNeighbor(re).id }}</span>
                    <span class="kg3d-re">{{ re.label||re.relation }}</span>
                  </div>
                  <div v-if="!relatedEdges.length" class="kg3d-se">无</div>
                </div>
                <div class="kg3d-side-acts">
                  <button @click="openEditNode">✏️编辑</button>
                  <button @click="openAddEdge">+关系</button>
                  <button @click="doDeleteNode">🗑️删除</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- TAB2: 编辑 -->
      <div class="kg-tab-panel" v-show="activeTab===1">
        <div style="padding:14px;height:100%;display:flex;flex-direction:column;overflow:hidden">
          <div style="display:flex;align-items:center;gap:8px;margin-bottom:10px;flex-shrink:0">
            <button class="kg-etab" :class="{active:editorTab===0}" @click="editorTab=0">📦节点({{ allNodes.length }})</button>
            <button class="kg-etab" :class="{active:editorTab===1}" @click="editorTab=1">🔗关系({{ allEdges.length }})</button>
            <div style="flex:1"></div>
            <select v-model="editorTypeFilter" class="kg-sel" v-if="editorTab===0"><option value="">全部</option><option v-for="t in allTypeOpts" :key="t.value" :value="t.value">{{ t.label }}</option></select>
            <button class="kg-add-btn" @click="openAddNode">+新增</button>
          </div>
          <div style="flex:1;overflow-y:auto" v-if="editorTab===0">
            <table class="kg-tbl"><thead><tr><th>ID</th><th>名称</th><th>类型</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="n in filteredEditNodes" :key="n.id">
                <td class="kg-ci">{{ n.id }}</td><td><span class="kg3d-dot" :style="{background:nodeColor(n.type),display:'inline-block',width:7,height:7,borderRadius:4,marginRight:4}"></span>{{ n.label||n.id }}</td>
                <td><span class="kg3d-tt mini" :style="{background:nodeColor(n.type)}">{{ n.type }}</span></td>
                <td><span class="kg-st" :class="n.properties?.status||'open'">{{ n.properties?.status||'open' }}</span></td>
                <td><button class="kg-op" @click="editRow(n)">编辑</button><button class="kg-op d" @click="deleteRow(n)">删除</button></td>
              </tr>
              <tr v-if="!filteredEditNodes.length"><td colspan="5" style="color:var(--text-dim);text-align:center;padding:30px">暂无</td></tr>
            </tbody></table>
          </div>
          <div style="flex:1;overflow-y:auto" v-if="editorTab===1">
            <table class="kg-tbl"><thead><tr><th>源</th><th>关系</th><th>目标</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="e in allEdges" :key="e.id"><td class="kg-ci">{{ e.source }}</td><td><span class="kg3d-rb">{{ e.relation||e.label }}</span></td><td class="kg-ci">{{ e.target }}</td><td><button class="kg-op d" @click="deleteEdge(e)">删除</button></td></tr>
              <tr v-if="!allEdges.length"><td colspan="4" style="color:var(--text-dim);text-align:center;padding:30px">暂无</td></tr>
            </tbody></table>
          </div>
        </div>
      </div>
      <!-- TAB3: 问答 -->
      <div class="kg-tab-panel" v-show="activeTab===2">
        <div style="display:flex;height:100%">
          <div style="width:380px;border-right:1px solid rgba(64,128,255,0.1);display:flex;flex-direction:column;flex-shrink:0">
            <div style="padding:12px 14px;font-size:13px;font-weight:600;color:#e8ecf4;border-bottom:1px solid rgba(64,128,255,0.08)">💬 智能问答</div>
            <div style="flex:1;overflow-y:auto;padding:12px" ref="qaMsgsRef">
              <div class="kg-qa-w" v-if="!qaMessages.length">
                <div style="font-size:12px;color:#5a6a8e;font-weight:600;margin-bottom:8px">快捷问题</div>
                <button v-for="q in quickQuestions" :key="q" class="kg-qa-qb" @click="askQuestion(q)">{{ q }}</button>
              </div>
              <div class="kg-qa-m" v-for="(m,i) in qaMessages" :key="i" :class="m.role">
                <div class="kg-qa-av">{{ m.role==='user'?'🧑':'🤖' }}</div>
                <div class="kg-qa-b"><div class="kg-qa-i" v-if="m.intent">意图:{{ m.intent }}</div><div style="white-space:pre-wrap">{{ m.text }}</div></div>
              </div>
            </div>
            <div style="display:flex;gap:8px;padding:10px 12px;border-top:1px solid rgba(64,128,255,0.08)">
              <input v-model="qaInput" placeholder="输入问题..." @keyup.enter="sendQuestion" style="flex:1;background:rgba(255,255,255,0.03);border:1px solid rgba(64,128,255,0.1);border-radius:8px;padding:8px 12px;color:#e8ecf4;font-size:14px;outline:none" />
              <button :disabled="!qaInput.trim()||qaLoading" @click="sendQuestion" style="width:36px;height:36px;border-radius:8px;border:none;background:rgba(64,128,255,0.1);color:#1890ff;cursor:pointer;font-size:16px;display:flex;align-items:center;justify-content:center">➤</button>
            </div>
          </div>
          <div style="flex:1"><div id="kg-qa3d" style="width:100%;height:100%"></div></div>
        </div>
      </div>
      <!-- TAB4: 日志 -->
      <div class="kg-tab-panel" v-show="activeTab===3">
        <div style="padding:14px;height:100%;display:flex;flex-direction:column;overflow:hidden">
          <div class="kg-log-f" style="margin-bottom:10px"><input v-model="logFilterOperator" placeholder="操作人" class="kg-l-inp" /><input type="date" v-model="logFilterSince" class="kg-l-inp" /></div>
          <div style="flex:1;overflow-y:auto"><table class="kg-tbl"><thead><tr><th>操作人</th><th>时间</th><th>类型</th><th>内容</th><th>涉及</th></tr></thead>
          <tbody><tr v-for="(l,i) in filteredLogs" :key="i"><td><span class="kg-lo">{{ l.operator }}</span></td><td class="kg-ci">{{ l.time }}</td><td><span class="kg-lt" :class="l.type">{{ l.typeLabel||l.type }}</span></td><td class="kg-ci">{{ l.content }}</td><td class="kg-ci">{{ l.target }}</td></tr>
          <tr v-if="!filteredLogs.length"><td colspan="5" style="color:var(--text-dim);text-align:center;padding:30px">暂无</td></tr>
          </tbody></table></div>
        </div>
      </div>
    </div>
    <!-- 弹窗 -->
    <div class="kg-modal-mask" v-if="showEditModal" @click.self="showEditModal=false">
      <div class="kg-modal"><div class="kg-modal-hd"><span>{{ editMode==='add'?'✨ 新增':'✏️ 编辑' }}节点</span><button class="kg-x" @click="showEditModal=false">✕</button></div>
        <div class="kg-modal-bd"><div class="kg-mc"><div class="kg-mr"><label>ID</label><input v-model="editForm.id" :disabled="editMode==='edit'" /></div>
          <div class="kg-mr"><label>名称</label><input v-model="editForm.name" /></div>
          <div class="kg-mr"><label>类型</label><select v-model="editForm.type"><option v-for="t in allTypeOpts" :key="t.value" :value="t.value">{{ t.label }}</option></select></div>
        </div><div class="kg-mc">
          <div class="kg-mr"><label>状态</label><select v-model="editForm.status"><option value="open">开放</option><option value="closed">关闭</option></select></div>
          <div class="kg-mr"><label>开放时间</label><input v-model="editForm.opening_hours" /></div>
          <div class="kg-mr"><label>容量</label><input v-model="editForm.capacity" type="number" /></div>
        </div></div>
        <div class="kg-modal-ft"><button class="kg-tbb" @click="showEditModal=false">取消</button><button class="kg-tbb" style="background:var(--cyan)" @click="saveNode">✅ 保存</button></div>
      </div>
    </div>
    <div class="kg-modal-mask" v-if="showEdgeModal" @click.self="showEdgeModal=false">
      <div class="kg-modal" style="max-width:480px"><div class="kg-modal-hd"><span>🔗 添加关系</span><button class="kg-x" @click="showEdgeModal=false">✕</button></div>
        <div class="kg-modal-bd" style="flex-direction:column">
          <div class="kg-mr"><label>源节点</label><input :value="selectedNode.label||selectedNode.id" disabled /></div>
          <div class="kg-mr"><label>类型</label><select v-model="edgeForm.relation"><option value="PASSES_THROUGH">PASSES_THROUGH</option><option value="HAS_FACILITY">HAS_FACILITY</option><option value="NEAR_BY">NEAR_BY</option><option value="SUITABLE_FOR">SUITABLE_FOR</option><option value="PREFERS">PREFERS</option></select></div>
          <div class="kg-mr"><label>目标</label><select v-model="edgeForm.targetId"><option value="">-- 选择 --</option><option v-for="n in allNodes.filter(n=>n.id!==selectedNode.id)" :key="n.id" :value="n.id">{{ n.label||n.id }}({{ n.type }})</option></select></div>
        </div>
        <div class="kg-modal-ft"><button class="kg-tbb" @click="showEdgeModal=false">取消</button><button class="kg-tbb" style="background:var(--cyan)" @click="saveEdge">✅ 保存</button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, inject, onMounted, onUnmounted, nextTick } from 'vue'
const showToast = inject('showToast')

// ===== Tabs =====
const activeTab = ref(0)
const tabs = [
  { label:'图谱总览', icon:'<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>' },
  { label:'图谱编辑', icon:'<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>' },
  { label:'智能问答', icon:'<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>' },
  { label:'操作日志', icon:'<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>' },
]
const tiL = ref(0); const tiW = ref(0)
function switchTab(i) { activeTab.value = i; nextTick(() => updateTabIndicator()); if (i === 2 && !qaGraphReady) nextTick(() => initQaGraph()) }
function updateTabIndicator() { if (!tabsRef.value) return; const b = tabsRef.value.querySelectorAll('.kg-tab'); const a = b[activeTab.value]; if (a) { tiL.value = a.offsetLeft; tiW.value = a.offsetWidth } }
const tabsRef = ref(null)

// ===== State =====
const allNodes = ref([]); const allEdges = ref([]); const graphLoading = ref(true)
const rightOpen = ref(false); const selectedNode = ref({}); const relatedEdges = ref([])
const searchQuery = ref(''); const searchResult = ref(null); const searchLoading = ref(false)
const showEditModal = ref(false); const showEdgeModal = ref(false); const editMode = ref('add')
const editForm = ref({ id:'', type:'Spot', name:'', status:'open', opening_hours:'', capacity:'' })
const edgeForm = ref({ relation:'PASSES_THROUGH', targetId:'', extraProps:'{}' })
const editorTab = ref(0); const editorTypeFilter = ref('')
const qaInput = ref(''); const qaMessages = ref([]); const qaLoading = ref(false); const qaMsgsRef = ref(null)
const logFilterOperator = ref(''); const logFilterSince = ref('')
const qaGraphReady = ref(false)

let forceGraph3D = null; let qaGraph3D = null; let origCamPos = null

const nodeTypeFilters = reactive([
  { label:'景点', value:'Spot', short:'景', color:'#3B82F6', checked:true },
  { label:'线路', value:'Route', short:'线', color:'#10B981', checked:true },
  { label:'设施', value:'Facility', short:'设', color:'#F59E0B', checked:true },
  { label:'美食', value:'Food', short:'食', color:'#EF4444', checked:true },
  { label:'酒店', value:'Hotel', short:'酒', color:'#8B5CF6', checked:true },
  { label:'画像', value:'Persona', short:'客', color:'#06B6D4', checked:true },
  { label:'活动', value:'Event', short:'活', color:'#EC4899', checked:true },
])

const quickQuestions = ['如果关闭灵山大佛会影响哪些路线？','历史文化路线经过哪些景点？','九龙灌浴附近有什么设施？','哪个景点容量风险最高？','适合亲子游的路线有哪些？']

const allTypeOpts = computed(() => nodeTypeFilters.map(t=>({value:t.value,label:t.label})))
const filteredEditNodes = computed(() => editorTypeFilter.value ? allNodes.value.filter(n=>n.type===editorTypeFilter.value) : allNodes.value)
const detailProps = computed(() => { const p=selectedNode.value.properties; return p&&Object.keys(p).length?p:null })
const filteredLogs = computed(() => {
  let l = getMockLogs()
  if (logFilterOperator.value) l = l.filter(x=>x.operator.includes(logFilterOperator.value))
  if (logFilterSince.value) l = l.filter(x=>x.time>=logFilterSince.value)
  return l
})

function nodeColor(type) { return { Spot:'#3B82F6', Route:'#10B981', Facility:'#F59E0B', Food:'#EF4444', Hotel:'#8B5CF6', Persona:'#06B6D4', Event:'#EC4899' }[type]||'#64748b' }
function getNeighbor(e) { const nid = e.source===selectedNode.value.id?e.target:e.source; return allNodes.value.find(n=>n.id===nid)||{id:nid,label:nid,type:'Unknown'} }
function visibleNodes() { const c = new Set(nodeTypeFilters.filter(t=>t.checked).map(t=>t.value)); return allNodes.value.filter(n=>c.has(n.type)) }

// ===== Loader =====
function loadForceGraph() {
  return new Promise((resolve, reject) => {
    if (window.ForceGraph3D) { resolve(window.ForceGraph3D); return }
    const s=document.createElement('script'); s.src='/lib/3d-force-graph.min.js'
    s.onload=()=>{if(window.ForceGraph3D)resolve(window.ForceGraph3D);else reject('加载失败')}
    s.onerror=()=>reject('加载失败'); document.body.appendChild(s)
  })
}

// ===== 3D Init =====
async function init3DGraph() {
  graphLoading.value = true
  try {
    const FG = await loadForceGraph()
    const container = document.getElementById('kg3d-container')
    if (!container) { graphLoading.value = false; return }
    const vn = visibleNodes(); const vids = new Set(vn.map(n=>n.id))
    const ve = allEdges.value.filter(e => vids.has(e.source) && vids.has(e.target))

    forceGraph3D = FG({
      controlType: 'orbit',
      rendererConfig: { antialias: true, alpha: true }
    })(container)
      .graphData({ nodes: vn.map(n => ({ ...n, _color: nodeColor(n.type) })), links: ve })
      .width(container.clientWidth).height(container.clientHeight)
      .backgroundColor('#0a0e27')
      .nodeColor('_color').nodeRelSize(7).nodeVal(n => n.size || 10)
      .nodeLabel(n => `${n.label||n.id} (${n.type})`)
      .linkColor(() => 'rgba(100,160,255,0.6)')
      .linkWidth(2)
      .linkLabel(e => e.label||'')
      .linkDirectionalParticles(2)
      .linkDirectionalParticleWidth(2)
      .linkDirectionalParticleColor(() => '#60a5fa')
      .linkDirectionalParticleSpeed(0.004)
      .warmupTicks(400)
      .cooldownTicks(0)
      .onNodeClick(n => {
        const nd = allNodes.value.find(x => x.id === n.id) || {}
        selectedNode.value = nd
        relatedEdges.value = allEdges.value.filter(e => e.source === n.id || e.target === n.id)
        rightOpen.value = true
      })
      .onEngineTick(() => {
        updateNodeLabels(forceGraph3D, container)
      })

    if (forceGraph3D.d3Force('charge')) forceGraph3D.d3Force('charge').strength(-600)

    const dist = Math.max(container.clientWidth, container.clientHeight) * 1.5
    forceGraph3D.cameraPosition({ x:0, y:dist, z:0.001 }, { x:0, y:0, z:0 })
    origCamPos = { x:0, y:dist, z:0.001 }
    graphLoading.value = false
  } catch(e) { console.error(e); graphLoading.value = false; showToast('❌ 加载失败: '+e.message, 'error') }
}

// ===== QA Graph =====
async function initQaGraph() {
  nextTick(async () => {
    const container = document.getElementById('kg-qa3d')
    if (!container || qaGraphReady.value) return
    try {
      const FG = await loadForceGraph()
      if (!FG) return
      qaGraph3D = FG({ controlType:'orbit', rendererConfig:{antialias:true,alpha:true} })('kg-qa3d')
        .width(container.clientWidth).height(container.clientHeight).backgroundColor('#080c20')
        .nodeColor(n=>nodeColor(n.type)).nodeRelSize(6).nodeVal(8)
        .graphData({ nodes: allNodes.value.map(n=>({...n,_color:nodeColor(n.type)})), links: allEdges.value })
        .nodeLabel(n=>`${n.label}(${n.type})`)
        .linkColor(()=>'rgba(100,160,255,0.4)').linkWidth(1.2)
      qaGraph3D.cameraPosition({ x:0, y:500, z:0.001 }, { x:0, y:0, z:0 })
      qaGraphReady.value = true
    } catch(e) { console.error(e) }
  })
}

function rebuildGraph() {
  if (forceGraph3D) {
    const vn = visibleNodes(); const vids = new Set(vn.map(n=>n.id))
    const ve = allEdges.value.filter(e => vids.has(e.source) && vids.has(e.target))
    forceGraph3D.graphData({ nodes: vn.map(n=>({...n,_color:nodeColor(n.type)})), links: ve })
  } else { init3DGraph() }
}

function exportGraph3D() {
  const c = document.querySelector('#kg3d-container canvas')
  if (c) { const a=document.createElement('a'); a.href=c.toDataURL('image/png'); a.download='知识图谱3D.png'; a.click(); showToast('✅ 已导出','success') }
}

function resetCamera() { if (forceGraph3D && origCamPos) forceGraph3D.cameraPosition(origCamPos, {x:0,y:0,z:0}) }

// ===== 节点标签 - HTML 覆盖层 =====
const labelStyle = document.createElement('style')
labelStyle.textContent = '.kg-label-el{position:absolute;font-size:14px;font-weight:700;color:#fff;pointer-events:none;transform:translate(-50%,-100%);padding:3px 10px;border-radius:6px;white-space:nowrap;text-shadow:0 0 12px rgba(0,0,0,0.95),0 0 4px rgba(0,0,0,0.8);transition:opacity 0.15s;z-index:3;letter-spacing:0.3px;border:1px solid rgba(255,255,255,0.08);backdrop-filter:blur(2px)}'
document.head.appendChild(labelStyle)

function updateNodeLabels(graphRef, container) {
  if (!graphRef || !container) return
  const labelsEl = document.getElementById('kg3d-labels')
  if (!labelsEl) return
  const graphData = graphRef.graphData()
  if (!graphData || !graphData.nodes) return
  const camera = graphRef.camera()
  if (!camera) return

  // 从 camera 提取 THREE.Vector3
  const Vec3 = camera.position.constructor
  const w = container.clientWidth
  const h = container.clientHeight

  // 更新或创建标签
  graphData.nodes.forEach(n => {
    // 获取节点3D位置（从内部数据）
    const obj3D = graphRef._engineProps ? null : null
    // 直接使用节点坐标
    const pos = new Vec3(n.x || 0, n.y || 0, n.z || 0)
    pos.project(camera)

    // 如果在相机后面，跳过
    if (pos.z > 1) return

    const sx = (pos.x + 1) / 2 * w
    const sy = -(pos.y - 1) / 2 * h

    // 检查屏幕范围内
    if (sx < -50 || sx > w + 50 || sy < -20 || sy > h + 20) return

    let el = document.getElementById('lbl-' + n.id)
    if (!el) {
      el = document.createElement('div')
      el.id = 'lbl-' + n.id
      el.className = 'kg-label-el'
      labelsEl.appendChild(el)
    }
    el.textContent = (n.label || n.id || '')
    el.style.left = sx + 'px'
    el.style.top = sy + 'px'
    el.style.background = n._color || '#64748b'
    el.style.opacity = '1'
  })

  // 移除不存在的节点标签
  const existingLabels = labelsEl.querySelectorAll('.kg-label-el')
  const nodeIds = new Set(graphData.nodes.map(n => n.id))
  existingLabels.forEach(el => {
    if (!nodeIds.has(el.id.replace('lbl-', ''))) el.remove()
  })
}

// ===== Search =====
async function doSearch() {
  if (!searchQuery.value.trim()) return
  searchLoading.value = true
  try {
    const res = await fetch('/api/graph/search?q='+encodeURIComponent(searchQuery.value), {signal:AbortSignal.timeout(8000)}).catch(()=>null)
    if (res && res.ok) {
      const d = await res.json().catch(()=>null)
      if (d && d.answer) { searchResult.value = d; if (d.highlight_node_ids) highlight3DNode(d.highlight_node_ids) }
      else { searchResult.value = getMockSearchAnswer(searchQuery.value); if (searchResult.value.highlight_node_ids) highlight3DNode(searchResult.value.highlight_node_ids) }
    }
    else { await new Promise(r=>setTimeout(r,300)); const m = getMockSearchAnswer(searchQuery.value); searchResult.value = m; if (m.highlight_node_ids) highlight3DNode(m.highlight_node_ids) }
  } catch(e) { console.warn('[知识图谱] 搜索失败:', e.message); searchResult.value = getMockSearchAnswer(searchQuery.value); if (searchResult.value.highlight_node_ids) highlight3DNode(searchResult.value.highlight_node_ids) }
  searchLoading.value = false
}
function clearSearch() { searchResult.value = null; clearHighlight3D() }
function highlight3DNode(ids) { if (forceGraph3D) forceGraph3D.nodeColor(n => ids.includes(n.id) ? '#ffffff' : nodeColor(n.type)) }
function clearHighlight3D() { if (forceGraph3D) forceGraph3D.nodeColor(n => nodeColor(n.type)) }
async function askQuestion(q) { qaInput.value = q; sendQuestion() }
async function sendQuestion() {
  if (!qaInput.value.trim() || qaLoading.value) return
  const q = qaInput.value.trim(); qaMessages.value.push({role:'user',text:q}); qaLoading.value = true
  let ans='暂无回答',ids=[]
  try {
    const res = await fetch('/api/graph/search?q='+encodeURIComponent(q), {signal:AbortSignal.timeout(8000)}).catch(()=>null)
    if (res && res.ok) {
      const d = await res.json().catch(()=>null)
      if (d && d.answer) { ans=d.answer; ids=d.highlight_node_ids||[] }
      else { const m=getMockSearchAnswer(q); ans=m.answer; ids=m.highlight_node_ids||[] }
    }
    else { await new Promise(r=>setTimeout(r,300)); const m=getMockSearchAnswer(q); ans=m.answer; ids=m.highlight_node_ids||[] }
  } catch(e) { const m=getMockSearchAnswer(q); ans=m.answer; ids=m.highlight_node_ids||[] }
  qaMessages.value.push({role:'assistant',text:ans}); qaLoading.value=false; qaInput.value=''
  nextTick(()=>{if(qaMsgsRef.value)qaMsgsRef.value.scrollTop=qaMsgsRef.value.scrollHeight})
  if (ids.length && qaGraph3D) qaGraph3D.nodeColor(n=>ids.includes(n.id)?'#ffffff':nodeColor(n.type))
}

// ===== CRUD =====
async function doDeleteNode() { if (!selectedNode.value.id) return; if (!confirm('确定删除？')) return; try{await fetch('/api/graph/node/'+selectedNode.value.id,{method:'DELETE'}).catch(()=>null)}catch(e){} allNodes.value = allNodes.value.filter(n=>n.id!==selectedNode.value.id); allEdges.value = allEdges.value.filter(e=>e.source!==selectedNode.value.id&&e.target!==selectedNode.value.id); rightOpen.value=false; rebuildGraph() }
function openEditNode() { editMode.value='edit'; const p=selectedNode.value.properties||{}; editForm.value={id:selectedNode.value.id||'',type:selectedNode.value.type||'Spot',name:selectedNode.value.label||p.name||'',status:p.status||'open',opening_hours:p.opening_hours||'',capacity:p.capacity||''}; showEditModal.value=true }
function openAddNode() { editMode.value='add'; editForm.value={id:'',type:'Spot',name:'',status:'open',opening_hours:'',capacity:''}; showEditModal.value=true }
function openAddEdge() { edgeForm.value={relation:'PASSES_THROUGH',targetId:'',extraProps:'{}'}; showEdgeModal.value=true }
async function saveNode() {
  try {
    const props = {status:editForm.value.status,name:editForm.value.name}
    if (editForm.value.opening_hours) props.opening_hours = editForm.value.opening_hours
    if (editForm.value.capacity) props.capacity = Number(editForm.value.capacity)
    if (editMode.value === 'add') {
      const nn = {id:editForm.value.id||('n_'+Date.now()),label:editForm.value.name,type:editForm.value.type,properties:props}
      await fetch('/api/graph/node/add',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(nn)}).catch(()=>null)
      allNodes.value.push(nn)
    } else {
      const n = allNodes.value.find(x=>x.id===editForm.value.id)
      if (n) { n.type=editForm.value.type; n.label=editForm.value.name; n.properties=props }
      await fetch('/api/graph/node/'+editForm.value.id,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({type:editForm.value.type,label:editForm.value.name,...props})}).catch(()=>null)
    }
    showEditModal.value=false; showToast('✅ 已保存','success'); rebuildGraph()
  } catch(e) { showToast('❌ 保存失败','error') }
}
async function saveEdge() {
  if (!edgeForm.value.targetId) { showToast('❌ 请选择目标','error'); return }
  try {
    const props = JSON.parse(edgeForm.value.extraProps||'{}')
    await fetch('/api/graph/edge/add',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({source:selectedNode.value.id,target:edgeForm.value.targetId,relation:edgeForm.value.relation,properties:props})}).catch(()=>null)
    const ne = {id:'e_'+Date.now(),source:selectedNode.value.id,target:edgeForm.value.targetId,relation:edgeForm.value.relation,label:edgeForm.value.relation,properties:props}
    allEdges.value.push(ne); showEdgeModal.value=false; showToast('✅ 已添加','success'); rebuildGraph()
  } catch(e) { showToast('❌ JSON 错误','error') }
}
function editRow(n) { selectedNode.value=n; relatedEdges.value=allEdges.value.filter(e=>e.source===n.id||e.target===n.id); openEditNode() }
function deleteRow(n) { if(!confirm('确定删除？'))return; allNodes.value=allNodes.value.filter(x=>x.id!==n.id); allEdges.value=allEdges.value.filter(e=>e.source!==n.id&&e.target!==n.id); rebuildGraph() }
function deleteEdge(e) { if(!confirm('确定删除？'))return; allEdges.value=allEdges.value.filter(x=>x.id!==e.id); rebuildGraph() }

// ===== Data =====
async function loadSnapshot() {
  graphLoading.value = true
  try {
    const res = await fetch('/api/graph/snapshot', { signal: AbortSignal.timeout(8000) }).catch(()=>null)
    if (res && res.ok) {
      const d = await res.json().catch(()=>null)
      if (d && ((d.nodes?.length > 0) || (d.edges?.length > 0))) {
        allNodes.value = (d.nodes||[]).map(n=>({id:n.id,type:n.type||'Spot',label:n.label||n.name||n.id,properties:Object.fromEntries(Object.entries(n).filter(([k])=>!['id','type','label','name'].includes(k)))}))
        allEdges.value = (d.edges||[]).map(e=>({id:e.id,source:e.source,target:e.target,relation:e.relation||e.label||'',label:e.relation||e.label||'',properties:Object.fromEntries(Object.entries(e).filter(([k])=>!['id','source','target','relation','label'].includes(k)))}))
      } else { useMockData() }
    } else { useMockData() }
  } catch(e) { useMockData() }
  const ts = new Set(allNodes.value.map(n=>n.type))
  nodeTypeFilters.forEach(t => { t.checked = ts.has(t.value) })
  nextTick(() => init3DGraph())
}
function useMockData() {
  allNodes.value = getMockNodes(); allEdges.value = getMockEdges()
}

// ===== Mock =====
function getMockNodes() {
  return [
    {id:'spot_lingshan_buddha',type:'Spot',label:'灵山大佛',properties:{status:'open',capacity:5000,opening_hours:'08:30-17:00'}},
    {id:'spot_dazhao',type:'Spot',label:'灵山大照壁',properties:{status:'open',length:'40米'}},
    {id:'spot_xiangfu',type:'Spot',label:'祥符禅寺',properties:{status:'open',dynasty:'唐'}},
    {id:'spot_nine_dragons',type:'Spot',label:'九龙灌浴',properties:{status:'open',show_time:'10:00/14:00'}},
    {id:'spot_fan_gong',type:'Spot',label:'梵宫',properties:{status:'open',area:'7万㎡'}},
    {id:'spot_five_signs',type:'Spot',label:'五印坛城',properties:{status:'open'}},
    {id:'spot_manjusri',type:'Spot',label:'文殊菩萨',properties:{status:'open'}},
    {id:'spot_lotus',type:'Spot',label:'莲花喷泉',properties:{status:'open'}},
    {id:'fac_toilet_01',type:'Facility',label:'大佛广场厕所',properties:{status:'open',type:'toilet'}},
    {id:'fac_toilet_02',type:'Facility',label:'梵宫东侧厕所',properties:{status:'open',type:'toilet'}},
    {id:'fac_parking',type:'Facility',label:'南山停车场',properties:{status:'open',capacity:800}},
    {id:'fac_shop',type:'Facility',label:'吉祥礼品店',properties:{status:'open',type:'shop'}},
    {id:'food_01',type:'Food',label:'灵山素斋馆',properties:{status:'open',price:'60元/人'}},
    {id:'food_02',type:'Food',label:'梵宫自助餐',properties:{status:'open',price:'88元/人'}},
    {id:'hotel_01',type:'Hotel',label:'灵山精舍',properties:{status:'open',stars:'4'}},
    {id:'hotel_02',type:'Hotel',label:'禅修酒店',properties:{status:'open',stars:'5'}},
    {id:'event_01',type:'Event',label:'新春祈福法会',properties:{status:'open'}},
    {id:'event_02',type:'Event',label:'九龙灌浴表演',properties:{status:'open'}},
    {id:'route_history',type:'Route',label:'历史文化线',properties:{status:'open',duration:'3h'}},
    {id:'route_family',type:'Route',label:'亲子欢乐线',properties:{status:'open',duration:'4h'}},
    {id:'route_culture',type:'Route',label:'深度文化线',properties:{status:'open',duration:'5h'}},
    {id:'persona_family',type:'Persona',label:'亲子游客',properties:{status:'open',age_group:'30-45'}},
    {id:'persona_elderly',type:'Persona',label:'银发游客',properties:{status:'open',age_group:'55+'}},
    {id:'persona_young',type:'Persona',label:'年轻游客',properties:{status:'open',age_group:'18-30'}},
  ]
}
function getMockEdges() {
  return [
    {id:'e1',source:'route_history',target:'spot_dazhao',relation:'PASSES_THROUGH',properties:{order:1}},
    {id:'e2',source:'route_history',target:'spot_xiangfu',relation:'PASSES_THROUGH',properties:{order:2}},
    {id:'e3',source:'route_history',target:'spot_lingshan_buddha',relation:'PASSES_THROUGH',properties:{order:3}},
    {id:'e4',source:'route_history',target:'spot_nine_dragons',relation:'PASSES_THROUGH',properties:{order:4}},
    {id:'e5',source:'route_family',target:'spot_dazhao',relation:'PASSES_THROUGH',properties:{order:1}},
    {id:'e6',source:'route_family',target:'spot_lingshan_buddha',relation:'PASSES_THROUGH',properties:{order:2}},
    {id:'e7',source:'route_family',target:'spot_nine_dragons',relation:'PASSES_THROUGH',properties:{order:3}},
    {id:'e8',source:'route_family',target:'spot_fan_gong',relation:'PASSES_THROUGH',properties:{order:4}},
    {id:'e9',source:'route_culture',target:'spot_xiangfu',relation:'PASSES_THROUGH',properties:{order:1}},
    {id:'e10',source:'route_culture',target:'spot_fan_gong',relation:'PASSES_THROUGH',properties:{order:2}},
    {id:'e11',source:'route_culture',target:'spot_five_signs',relation:'PASSES_THROUGH',properties:{order:3}},
    {id:'e12',source:'spot_lingshan_buddha',target:'fac_toilet_01',relation:'NEAR_BY'},
    {id:'e13',source:'spot_fan_gong',target:'fac_toilet_02',relation:'NEAR_BY'},
    {id:'e14',source:'spot_lingshan_buddha',target:'fac_parking',relation:'NEAR_BY'},
    {id:'e15',source:'spot_dazhao',target:'fac_shop',relation:'NEAR_BY'},
    {id:'e16',source:'route_family',target:'persona_family',relation:'SUITABLE_FOR'},
    {id:'e17',source:'route_history',target:'persona_elderly',relation:'SUITABLE_FOR'},
    {id:'e18',source:'route_culture',target:'persona_young',relation:'SUITABLE_FOR'},
    {id:'e19',source:'persona_family',target:'spot_nine_dragons',relation:'PREFERS'},
    {id:'e20',source:'spot_lingshan_buddha',target:'food_01',relation:'NEAR_BY'},
    {id:'e21',source:'spot_fan_gong',target:'food_02',relation:'HAS_FACILITY'},
    {id:'e22',source:'route_culture',target:'hotel_01',relation:'NEAR_BY'},
    {id:'e23',source:'spot_lingshan_buddha',target:'event_02',relation:'HAS_FACILITY'},
    {id:'e24',source:'event_01',target:'spot_xiangfu',relation:'NEAR_BY'},
  ]
}
function getMockSearchAnswer(q) {
  const l = q.toLowerCase()
  if (l.indexOf('关闭')!==-1||l.indexOf('影响')!==-1) return {answer:'关闭「灵山大佛」将影响 2 条路线（历史文化线、亲子欢乐线）及 8 个关联景点。', intent:'impact_query', highlight_node_ids:['spot_lingshan_buddha','route_history','route_family']}
  if (l.indexOf('历史文化')!==-1) return {answer:'历史文化路线经过：灵山大照壁 → 祥符禅寺 → 灵山大佛 → 九龙灌浴。', intent:'route_detail', highlight_node_ids:['spot_dazhao','spot_xiangfu','spot_lingshan_buddha','spot_nine_dragons']}
  if (l.indexOf('九龙灌浴')!==-1) return {answer:'九龙灌浴附近有：梵宫东侧厕所、吉祥礼品店。每日 10:00/14:00 有表演。', intent:'nearby_search', highlight_node_ids:['spot_nine_dragons','fac_toilet_02','fac_shop']}
  if (l.indexOf('亲子')!==-1) return {answer:'亲子欢乐线全程 4 小时，包含九龙灌浴、梵宫等景点。', intent:'recommend', highlight_node_ids:['route_family','persona_family','spot_nine_dragons']}
  return {answer:'已搜索「'+q+'」', intent:'general_search', highlight_node_ids:[]}
}
function getMockLogs() {
  return [
    {operator:'admin',time:'2026-06-14 10:23',type:'create',typeLabel:'新增',content:'新增灵山素斋馆',target:'food_01'},
    {operator:'admin',time:'2026-06-14 09:45',type:'update',typeLabel:'修改',content:'更新容量5000',target:'spot_lingshan_buddha'},
    {operator:'admin',time:'2026-06-13 16:30',type:'delete',typeLabel:'删除',content:'删除临时停车场',target:'fac_temp_parking'},
    {operator:'admin',time:'2026-06-12 11:00',type:'init',typeLabel:'初始化',content:'知识图谱初始化',target:'—'},
  ]
}

// ===== Lifecycle =====
onMounted(() => { loadSnapshot(); nextTick(() => setTimeout(updateTabIndicator, 50)) })
onUnmounted(() => { if (forceGraph3D) { forceGraph3D._destructor?.(); forceGraph3D=null } if (qaGraph3D) { qaGraph3D._destructor?.(); qaGraph3D=null } })
</script>

<style scoped>
.kg-workbench{display:flex;flex-direction:column;background:#0a0e27;height:calc(100vh - 104px);overflow:hidden;border-radius:12px}
.kg-header{display:flex;align-items:center;padding:0 16px;height:46px;background:linear-gradient(180deg,#131838,#0d1230);border-bottom:1px solid rgba(64,128,255,0.12);flex-shrink:0;z-index:10}
.kg-header-left{display:flex;align-items:center;gap:8px}.kg-logo{display:flex;align-items:center;gap:7px;color:#e8ecf4;font-weight:700;font-size:14px}
.kg-logo svg{color:#4080ff}.kg-version-badge{font-size:9px;padding:1px 6px;border-radius:8px;background:rgba(64,128,255,0.12);color:#4080ff;font-weight:600;border:1px solid rgba(64,128,255,0.2)}
.kg-header-center{flex:1;display:flex;justify-content:center}.kg-tabs{display:flex;position:relative;gap:1px}
.kg-tab{display:flex;align-items:center;gap:5px;padding:7px 14px;border:none;background:transparent;color:#5a6a8e;cursor:pointer;font-size:12px;font-weight:500;border-radius:6px;transition:all 0.2s;white-space:nowrap}
.kg-tab:hover{color:#8b95b2;background:rgba(64,128,255,0.03)}.kg-tab.active{color:#4096ff}
.kg-tab-badge{font-size:9px;padding:1px 6px;border-radius:8px;background:rgba(64,128,255,0.12);color:#4096ff}
.kg-tab-indicator{position:absolute;bottom:-1px;height:2px;background:linear-gradient(90deg,#4096ff,#1890ff);border-radius:2px;transition:all 0.3s}
.kg-content{flex:1;overflow:hidden;position:relative}.kg-tab-panel{height:100%;overflow:hidden}

.kg3d-layout{display:flex;height:100%;overflow-x:hidden}
.kg3d-canvas-wrap{flex:1;position:relative;overflow:hidden;background:radial-gradient(ellipse at center,rgba(16,40,80,0.3)0%,#0a0e27 70%);min-width:0}
.kg3d-canvas-wrap>#kg3d-container{width:100%;height:100%}
.kg3d-labels{position:absolute;inset:0;pointer-events:none;z-index:2;overflow:hidden}
.kg3d-loader{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;background:rgba(10,14,39,0.85);z-index:10;color:#5a6a8e;font-size:12px}
.kg-loader{width:28px;height:28px;border:2px solid rgba(64,128,255,0.2);border-top-color:#1890ff;border-radius:50%;animation:spin .8s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}

.kg3d-toolbar-left{position:absolute;top:10px;left:10px;z-index:5;display:flex;flex-direction:column;gap:6px}
.kg3d-tool-section{background:rgba(13,18,48,0.92);border:1px solid rgba(64,128,255,0.15);border-radius:8px;padding:6px 8px;backdrop-filter:blur(8px)}
.kg3d-ts-title{font-size:9px;font-weight:600;color:#5a6a8e;text-transform:uppercase;letter-spacing:.05em;margin-bottom:4px}
.kg3d-ts-item{display:flex;align-items:center;gap:4px;padding:2px 0;cursor:pointer;font-size:11px;color:#8b95b2;transition:all .2s}
.kg3d-ts-item.disabled{opacity:0.35;filter:grayscale(0.6)}.kg3d-ts-item:not(.disabled){opacity:1;color:#e8ecf4}
.kg3d-ts-item input{accent-color:#1890ff}.kg3d-dot{width:7px;height:7px;border-radius:50%;flex-shrink:0;display:inline-block}
.kg3d-tool-btn{width:26px;height:26px;border:1px solid rgba(64,128,255,0.15);background:rgba(64,128,255,0.06);color:#5a6a8e;border-radius:5px;cursor:pointer;font-size:10px;transition:all .2s}
.kg3d-tool-btn:hover{background:rgba(64,128,255,0.12);color:#1890ff}

.kg3d-side{width:0;overflow:hidden;transition:width .25s;background:rgba(13,18,48,0.92);border-left:1px solid rgba(64,128,255,0.12)}
.kg3d-side.open{width:320px;flex-shrink:0}.kg3d-side-inner{padding:12px;overflow-y:auto;height:100%}
.kg3d-side-hd{display:flex;align-items:center;gap:8px;margin-bottom:12px}
.kg3d-side-av{width:32px;height:32px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:14px;color:#fff;flex-shrink:0}
.kg3d-side-n{font-size:15px;font-weight:700;color:#e8ecf4;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;letter-spacing:0.5px}
.kg3d-tt{font-size:11px;padding:2px 10px;border-radius:10px;color:#fff;font-weight:600;flex-shrink:0}
.kg3d-side-grp{margin-bottom:14px}.kg3d-side-gt{font-size:12px;font-weight:600;color:#5a6a8e;text-transform:uppercase;margin-bottom:6px;letter-spacing:0.05em}
.kg3d-pr{display:flex;flex-direction:column;gap:2px;padding:4px 0;border-bottom:1px solid rgba(64,128,255,0.06);font-size:12px}
.kg3d-pk{color:#5a6a8e;font-size:11px;font-weight:500}.kg3d-pv{color:#b0bcd4;font-size:13px;line-height:1.6;word-break:break-all;white-space:pre-wrap}
.kg3d-se{font-size:12px;color:#3a4a6e;padding:6px 0}
.kg3d-rr{display:flex;align-items:center;gap:6px;padding:4px 0;border-bottom:1px solid rgba(64,128,255,0.06);font-size:12px;color:#8b95b2}
.kg3d-re{font-size:10px;color:#5a6a8e;background:rgba(64,128,255,0.06);padding:2px 6px;border-radius:4px;margin-left:auto}
.kg3d-side-acts{display:flex;gap:4px;margin-top:10px}
.kg3d-side-acts button{flex:1;padding:6px 0;font-size:12px;border:1px solid rgba(64,128,255,0.15);background:rgba(64,128,255,0.06);color:#8b95b2;border-radius:6px;cursor:pointer;transition:all .2s}
.kg3d-side-acts button:hover{background:rgba(64,128,255,0.12);color:#1890ff}

.kg3d-search-overlay{position:absolute;bottom:0;left:0;right:0;z-index:5;background:rgba(13,18,48,0.96);border-top:1px solid rgba(64,128,255,0.15);padding:8px 12px;font-size:11px;color:#e8ecf4}

.kg-etab{padding:4px 10px;border:1px solid rgba(64,128,255,0.12);background:rgba(13,18,48,0.4);color:#5a6a8e;border-radius:5px;cursor:pointer;font-size:11px}
.kg-etab.active{background:rgba(64,128,255,0.08);border-color:#1890ff;color:#1890ff}
.kg-sel{background:rgba(13,18,48,0.6);border:1px solid rgba(64,128,255,0.12);color:#8b95b2;padding:4px 8px;border-radius:5px;font-size:11px;outline:none}
.kg-add-btn{padding:4px 10px;background:rgba(64,128,255,0.1);color:#1890ff;border:1px solid rgba(64,128,255,0.15);border-radius:5px;cursor:pointer;font-size:11px}
.kg-tbl{width:100%;border-collapse:collapse;font-size:11px}
.kg-tbl th{position:sticky;top:0;background:rgba(13,18,48,0.96);padding:5px 8px;text-align:left;color:#5a6a8e;font-weight:600;border-bottom:1px solid rgba(64,128,255,0.12)}
.kg-tbl td{padding:4px 8px;border-bottom:1px solid rgba(64,128,255,0.06);color:#8b95b2}
.kg-tbl tr:hover td{background:rgba(64,128,255,0.03)}
.kg-ci{font-size:10px;color:#5a6a8e;max-width:150px;overflow:hidden;text-overflow:ellipsis}
.kg-st{font-size:9px;padding:1px 6px;border-radius:8px;font-weight:600}
.kg-st.open{background:rgba(16,185,129,0.1);color:#10b981}
.kg-st.closed{background:rgba(239,68,68,0.1);color:#ef4444}
.kg-op{padding:1px 5px;border:none;background:rgba(64,128,255,0.08);color:#1890ff;border-radius:3px;cursor:pointer;font-size:10px;margin-right:2px}
.kg-op.d{background:rgba(239,68,68,0.08);color:#ef4444}
.kg3d-rb{font-size:9px;padding:1px 6px;background:rgba(64,128,255,0.06);color:#1890ff;border-radius:3px}

.kg-qa-w{padding:8px}.kg-qa-qb{display:block;width:100%;text-align:left;padding:6px 10px;margin-bottom:4px;border:1px solid rgba(64,128,255,0.1);background:rgba(13,18,48,0.4);color:#8b95b2;border-radius:6px;cursor:pointer;font-size:13px;transition:all .2s}
.kg-qa-qb:hover{border-color:#1890ff;color:#1890ff}
.kg-qa-m{display:flex;gap:8px;margin:6px 0}
.kg-qa-m.user{flex-direction:row-reverse}
.kg-qa-av{width:26px;height:26px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:14px;flex-shrink:0}
.kg-qa-b{max-width:88%;padding:8px 12px;border-radius:10px;font-size:14px;line-height:1.6}
.kg-qa-m.user .kg-qa-b{background:rgba(64,128,255,0.08);border:1px solid rgba(64,128,255,0.1);color:#e8ecf4}
.kg-qa-m.assistant .kg-qa-b{background:rgba(26,34,72,0.4);border:1px solid rgba(64,128,255,0.06);color:#c8d0e0}
.kg-qa-i{font-size:11px;color:#5a6a8e;margin-bottom:4px}

.kg-analysis-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:12px;height:100%;overflow-y:auto}
.kg-card{background:rgba(13,18,48,0.5);border:1px solid rgba(64,128,255,0.1);border-radius:8px;overflow:hidden}
.kg-card-hd{display:flex;align-items:center;gap:6px;padding:6px 10px;font-size:11px;font-weight:600;color:#e8ecf4;border-bottom:1px solid rgba(64,128,255,0.08)}
.kg-dot{width:6px;height:6px;border-radius:50%;flex-shrink:0}
.kg-card-bd{padding:8px;max-height:180px;overflow-y:auto}

.kg-log-f{display:flex;gap:8px}.kg-l-inp{background:rgba(13,18,48,0.6);border:1px solid rgba(64,128,255,0.1);color:#8b95b2;padding:4px 8px;border-radius:5px;font-size:11px;outline:none}
.kg-lo{font-size:11px;color:#1890ff}
.kg-lt{font-size:9px;padding:1px 6px;border-radius:6px;font-weight:600}
.kg-lt.create{background:rgba(16,185,129,0.1);color:#10b981}
.kg-lt.update{background:rgba(59,130,246,0.1);color:#3b82f6}
.kg-lt.delete{background:rgba(239,68,68,0.1);color:#ef4444}
.kg-lt.init{background:rgba(139,92,246,0.1);color:#8b5cf6}

.kg-modal-mask{position:fixed;inset:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;backdrop-filter:blur(4px)}
.kg-modal{background:#0d1230;border:1px solid rgba(64,128,255,0.2);border-radius:12px;width:90%;max-width:580px;box-shadow:0 20px 60px rgba(0,0,0,0.5)}
.kg-modal-hd{display:flex;align-items:center;justify-content:space-between;padding:10px 14px;border-bottom:1px solid rgba(64,128,255,0.08);font-size:13px;font-weight:600;color:#e8ecf4}
.kg-x{background:transparent;border:none;color:#5a6a8e;cursor:pointer;font-size:14px}
.kg-modal-bd{display:flex;gap:10px;padding:12px 14px}
.kg-mc{flex:1;display:flex;flex-direction:column;gap:6px}
.kg-mr{display:flex;flex-direction:column;gap:2px}
.kg-mr label{font-size:10px;color:#5a6a8e;font-weight:500}
.kg-mr input,.kg-mr select,.kg-mr textarea{background:rgba(255,255,255,0.03);border:1px solid rgba(64,128,255,0.12);border-radius:5px;padding:4px 8px;color:#e8ecf4;font-size:11px;outline:none}
.kg-mr input:disabled{opacity:0.3}
.kg-modal-ft{display:flex;justify-content:flex-end;gap:8px;padding:8px 14px;border-top:1px solid rgba(64,128,255,0.08)}
.kg-tbb{padding:4px 12px;border:1px solid rgba(64,128,255,0.12);background:rgba(64,128,255,0.06);color:#8b95b2;border-radius:5px;cursor:pointer;font-size:11px}
</style>
