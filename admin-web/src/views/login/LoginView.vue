<template>
  <div class="login-page">
    <!-- 背景动画层 -->
    <div class="login-bg">
      <div class="grid-lines"></div>
      <div class="floating-orbs">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <div class="orb orb-3"></div>
      </div>
      <div class="particle-field" ref="particleField"></div>
    </div>

    <!-- 主内容 -->
    <div class="login-container">
      <!-- 左侧：登录/注册卡片 -->
      <div class="login-card" :class="{ 'card-enter': cardVisible }">
        <!-- Logo -->
        <div class="card-logo">
          <div class="logo-icon">
            <img src="/logo-icon.jpg" alt="Logo" width="40" height="40" style="border-radius: 8px; object-fit: cover;" />
          </div>
          <div class="logo-text">
            <div class="logo-title">景区数字人管理平台</div>
            <div class="logo-subtitle">Scenic Digital Human Platform</div>
          </div>
        </div>

        <!-- Tab 切换 -->
        <div class="card-tabs">
          <button
            class="tab-btn"
            :class="{ active: mode === 'login' }"
            @click="switchMode('login')"
          >登录</button>
          <button
            class="tab-btn"
            :class="{ active: mode === 'register' }"
            @click="switchMode('register')"
          >注册</button>
          <div class="tab-indicator" :class="{ right: mode === 'register' }"></div>
        </div>

        <!-- 表单 -->
        <form class="card-form" @submit.prevent="handleSubmit">
          <!-- 用户名（登录和注册都显示） -->
          <div class="form-group">
            <label class="form-label">用户名</label>
            <div class="input-wrap" :class="{ focus: focusField === 'username' }">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <input
                type="text"
                v-model="form.username"
                placeholder="请输入用户名"
                @focus="focusField = 'username'"
                @blur="focusField = ''"
              />
            </div>
          </div>

          <!-- 邮箱（仅注册显示） -->
          <div class="form-group" v-if="mode === 'register'">
            <label class="form-label">邮箱</label>
            <div class="input-wrap" :class="{ focus: focusField === 'email' }">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
              <input
                type="email"
                v-model="form.email"
                placeholder="请输入邮箱"
                @focus="focusField = 'email'"
                @blur="focusField = ''"
              />
            </div>
          </div>

          <!-- 密码 -->
          <div class="form-group">
            <label class="form-label">密码</label>
            <div class="input-wrap" :class="{ focus: focusField === 'password' }">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0110 0v4"/>
              </svg>
              <input
                :type="showPwd ? 'text' : 'password'"
                v-model="form.password"
                placeholder="请输入密码"
                @focus="focusField = 'password'"
                @blur="focusField = ''"
              />
              <button type="button" class="pwd-toggle" @click="showPwd = !showPwd">
                <svg v-if="!showPwd" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                  <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 注册：确认密码 -->
          <div class="form-group" v-if="mode === 'register'">
            <label class="form-label">确认密码</label>
            <div class="input-wrap" :class="{ focus: focusField === 'confirmPwd' }">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
              <input
                :type="showPwd ? 'text' : 'password'"
                v-model="form.confirmPassword"
                placeholder="请再次输入密码"
                @focus="focusField = 'confirmPwd'"
                @blur="focusField = ''"
              />
            </div>
          </div>

          <!-- 记住我 / 忘记密码 -->
          <div class="form-options" v-if="mode === 'login'">
            <label class="remember-me">
              <input type="checkbox" v-model="form.remember" />
              <span class="checkmark"></span>
              <span>记住我</span>
            </label>
            <a href="javascript:void(0)" class="forgot-pwd" @click="showToast('请联系管理员重置密码', 'info')">忘记密码？</a>
          </div>

          <!-- 错误提示 -->
          <div class="form-error" v-if="errorMsg">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
            {{ errorMsg }}
          </div>

          <!-- 提交按钮 -->
          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="!loading">{{ mode === 'login' ? '登 录' : '注 册' }}</span>
            <div v-else class="btn-loading">
              <div class="spinner-sm"></div>
              <span>{{ mode === 'login' ? '登录中...' : '注册中...' }}</span>
            </div>
          </button>
        </form>
      </div>

      <!-- 右侧：图片展示区 -->
      <div class="login-hero">
        <div class="hero-content">
          <div class="hero-badge">数字人管理平台</div>
          <h1 class="hero-title">
            随行<br/>
            <span class="gradient-text">智能景区虚拟向导</span>
          </h1>
          <p class="hero-desc">
            赋能景区管理，打造沉浸式游客体验。<br/>
            智能对话分析、知识图谱辅助、数据分析，一站式管理。
          </p>
          <div class="hero-stats">
            <div class="stat-item">
              <div class="stat-num">24/7</div>
              <div class="stat-label">全天候服务</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-num">AI+</div>
              <div class="stat-label">智能驱动</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-num">100%</div>
              <div class="stat-label">满意度</div>
            </div>
          </div>
        </div>
        <!-- 装饰元素 -->
        <div class="hero-deco-circle deco-1"></div>
        <div class="hero-deco-circle deco-2"></div>
        <div class="hero-deco-line"></div>
      </div>
    </div>

    <!-- Toast -->
    <div class="login-toast" :class="[toast.type, {show: toast.visible}]">{{ toast.message }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 状态
const mode = ref('login')
const loading = ref(false)
const showPwd = ref(false)
const focusField = ref('')
const cardVisible = ref(false)
const errorMsg = ref('')
const particleField = ref(null)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  remember: false
})

const toast = reactive({ visible: false, message: '', type: 'info' })
let toastTimer = null

function showToast(msg, type = 'info') {
  toast.message = msg; toast.type = type; toast.visible = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.visible = false }, 2500)
}

function switchMode(m) {
  if (m === mode.value) return
  errorMsg.value = ''
  mode.value = m
  form.username = ''; form.email = ''; form.password = ''; form.confirmPassword = ''
}

function validate() {
  if (!form.username.trim()) {
    errorMsg.value = '请输入用户名'; return false
  }
  if (mode.value === 'register') {
    if (!form.email.trim()) {
      errorMsg.value = '请输入邮箱'; return false
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      errorMsg.value = '邮箱格式不正确'; return false
    }
    if (form.password !== form.confirmPassword) {
      errorMsg.value = '两次密码不一致'; return false
    }
  }
  if (!form.password) {
    errorMsg.value = '请输入密码'; return false
  }
  if (form.password.length < 6) {
    errorMsg.value = '密码至少6位'; return false
  }
  errorMsg.value = ''; return true
}

async function handleSubmit() {
  if (!validate()) return
  loading.value = true; errorMsg.value = ''

  try {
    // ========== 注册 ==========
    if (mode.value === 'register') {
      const res = await fetch('/api/v1/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: form.username,
          email: form.email,
          password: form.password
        })
      })
      const data = await res.json().catch(() => null)
      if (res.ok && data && data.code === 0) {
        showToast('注册成功，请登录', 'success')
        mode.value = 'login'
        form.password = ''; form.confirmPassword = ''
        loading.value = false; return
      }
      // 后端返回的业务错误
      errorMsg.value = (data && data.message) || '注册失败，请稍后重试'
      loading.value = false; return
    }

    // ========== 登录 ==========
    try {
      const res = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: form.username,
          password: form.password
        })
      })
      const data = await res.json().catch(() => null)

      if (res.ok && data && data.code === 0 && data.data?.token) {
        localStorage.setItem('user_token', data.data.token)
        localStorage.setItem('user_email', data.data.user?.email || '')
        localStorage.setItem('user_name', data.data.user?.username || form.username)
        showToast('登录成功', 'success')
        setTimeout(() => { router.push('/dashboard') }, 600)
        loading.value = false; return
      }

      // 后端返回的业务错误（如用户名密码错误）
      if (data && data.message) {
        errorMsg.value = data.message
        loading.value = false; return
      }
    } catch (_) { /* 网络不通，走 fallback */ }

    // ========== 离线 Fallback ==========
    // 硬编码管理员
    if (form.username === 'admin' && form.password === 'admin@') {
      localStorage.setItem('user_token', 'token_fallback_' + Date.now())
      localStorage.setItem('user_email', 'user@example.com')
      localStorage.setItem('user_name', '管理员')
      showToast('登录成功（离线模式）', 'success')
      setTimeout(() => { router.push('/dashboard') }, 600)
      loading.value = false; return
    }

    // localStorage 已注册用户
    const users = JSON.parse(localStorage.getItem('registered_users') || '[]')
    const registered = users.find(u => u.username === form.username && u.password === form.password)
    if (registered) {
      localStorage.setItem('user_token', 'token_fallback_' + Date.now())
      localStorage.setItem('user_email', registered.email || '')
      localStorage.setItem('user_name', form.username)
      showToast('登录成功（离线模式）', 'success')
      setTimeout(() => { router.push('/dashboard') }, 600)
      loading.value = false; return
    }

    errorMsg.value = '用户名或密码错误'
  } catch(e) {
    errorMsg.value = '网络异常，请检查后端服务'
    console.error('Login error:', e)
  } finally {
    loading.value = false
  }
}

// 粒子动画
let animFrame = null
function initParticles() {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!particleField.value || !ctx) return
  particleField.value.appendChild(canvas)
  canvas.style.cssText = 'width:100%;height:100%;position:absolute;top:0;left:0'

  let w, h, particles = []
  function resize() {
    w = canvas.width = particleField.value.offsetWidth
    h = canvas.height = particleField.value.offsetHeight
    particles = Array.from({ length: 50 }, () => ({
      x: Math.random() * w, y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.4, vy: (Math.random() - 0.5) * 0.4,
      r: Math.random() * 2 + 0.5, a: Math.random() * 0.4 + 0.1
    }))
  }

  function draw() {
    ctx.clearRect(0, 0, w, h)
    particles.forEach(p => {
      p.x += p.vx; p.y += p.vy
      if (p.x < 0 || p.x > w) p.vx *= -1
      if (p.y < 0 || p.y > h) p.vy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(6,182,212,${p.a})`
      ctx.fill()
    })
    // 连线
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(59,130,246,${0.08 * (1 - dist / 120)})`
          ctx.stroke()
        }
      }
    }
    animFrame = requestAnimationFrame(draw)
  }

  resize()
  window.addEventListener('resize', resize)
  draw()
}

onMounted(() => {
  setTimeout(() => { cardVisible.value = true }, 100)
  initParticles()
})

onUnmounted(() => {
  if (animFrame) cancelAnimationFrame(animFrame)
})
</script>

<style scoped>
/* ===== 全局 ===== */
.login-page {
  width: 100vw; height: 100vh;
  background: #060a13;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ===== 背景动画 ===== */
.login-bg {
  position: absolute; inset: 0; z-index: 0;
  overflow: hidden;
}
.grid-lines {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(59,130,246,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59,130,246,0.04) 1px, transparent 1px);
  background-size: 60px 60px;
  animation: gridMove 20s linear infinite;
}
@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(60px, 60px); }
}
.floating-orbs { position: absolute; inset: 0; }
.orb {
  position: absolute; border-radius: 50%;
  filter: blur(80px); opacity: 0.25;
}
.orb-1 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, #3b82f6, transparent 70%);
  top: -10%; left: 30%;
  animation: orbFloat1 12s ease-in-out infinite;
}
.orb-2 {
  width: 300px; height: 300px;
  background: radial-gradient(circle, #06b6d4, transparent 70%);
  bottom: -5%; right: 20%;
  animation: orbFloat2 15s ease-in-out infinite;
}
.orb-3 {
  width: 200px; height: 200px;
  background: radial-gradient(circle, #8b5cf6, transparent 70%);
  top: 50%; left: 10%;
  animation: orbFloat3 18s ease-in-out infinite;
}
@keyframes orbFloat1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, 40px) scale(1.1); }
}
@keyframes orbFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-40px, -30px) scale(1.15); }
}
@keyframes orbFloat3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -50px) scale(0.9); }
}
.particle-field {
  position: absolute; inset: 0; z-index: 1;
}

/* ===== 主内容 ===== */
.login-container {
  position: relative; z-index: 2;
  display: flex; width: 900px; min-height: 520px;
  background: rgba(13,19,33,0.85);
  border: 1px solid rgba(59,130,246,0.15);
  border-radius: 20px;
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 60px rgba(0,0,0,0.5), 0 0 40px rgba(59,130,246,0.08);
  overflow: hidden;
}

/* ===== 左侧卡片 ===== */
.login-card {
  width: 420px; flex-shrink: 0;
  padding: 40px 36px;
  display: flex; flex-direction: column; justify-content: center;
  opacity: 0; transform: translateX(-30px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.login-card.card-enter {
  opacity: 1; transform: translateX(0);
}

/* Logo */
.card-logo {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 32px;
}
.logo-icon {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
}
.logo-title {
  font-size: 16px; font-weight: 600; color: #e2e8f0;
  letter-spacing: 0.5px;
}
.logo-subtitle {
  font-size: 10px; color: #64748b; margin-top: 2px;
  letter-spacing: 0.3px;
}

/* Tabs */
.card-tabs {
  display: flex; position: relative;
  background: rgba(30,45,74,0.3);
  border-radius: 10px; padding: 3px;
  margin-bottom: 28px;
}
.tab-btn {
  flex: 1; padding: 10px 0;
  background: none; border: none; outline: none;
  font-size: 14px; font-weight: 500;
  color: #64748b; cursor: pointer;
  border-radius: 8px; position: relative; z-index: 1;
  transition: color 0.3s;
}
.tab-btn.active { color: #e2e8f0; }
.tab-indicator {
  position: absolute; top: 3px; left: 3px;
  width: calc(50% - 3px); height: calc(100% - 6px);
  background: rgba(59,130,246,0.2);
  border-radius: 8px;
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.tab-indicator.right { transform: translateX(100%); }

/* Form */
.card-form { display: flex; flex-direction: column; gap: 18px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label {
  font-size: 12px; font-weight: 500; color: #94a3b8;
  letter-spacing: 0.3px;
}
.input-wrap {
  display: flex; align-items: center; gap: 8px;
  background: rgba(15,23,42,0.6);
  border: 1px solid rgba(59,130,246,0.1);
  border-radius: 10px;
  padding: 0 14px; height: 44px;
  transition: all 0.25s;
}
.input-wrap.focus {
  border-color: rgba(59,130,246,0.4);
  box-shadow: 0 0 0 3px rgba(59,130,246,0.08);
}
.input-icon { color: #64748b; flex-shrink: 0; }
.input-wrap input {
  flex: 1; height: 100%;
  background: none; border: none; outline: none;
  color: #e2e8f0; font-size: 13px;
}
.input-wrap input::placeholder { color: #475569; }
.pwd-toggle {
  background: none; border: none; outline: none;
  cursor: pointer; color: #64748b;
  display: flex; align-items: center; padding: 2px;
}
.pwd-toggle:hover { color: #94a3b8; }

/* Options */
.form-options {
  display: flex; justify-content: space-between; align-items: center;
}
.remember-me {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: #94a3b8; cursor: pointer;
}
.remember-me input { display: none; }
.checkmark {
  width: 14px; height: 14px;
  border: 1.5px solid #475569; border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.remember-me input:checked + .checkmark {
  background: #3b82f6; border-color: #3b82f6;
}
.remember-me input:checked + .checkmark::after {
  content: ''; width: 4px; height: 7px;
  border: solid #fff; border-width: 0 1.5px 1.5px 0;
  transform: rotate(45deg) translate(-1px, -1px);
}
.forgot-pwd {
  font-size: 12px; color: #64748b;
  text-decoration: none;
}
.forgot-pwd:hover { color: #06b6d4; }

/* Error */
.form-error {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 12px;
  background: rgba(239,68,68,0.08);
  border: 1px solid rgba(239,68,68,0.2);
  border-radius: 8px;
  font-size: 12px; color: #f87171;
}

/* Submit */
.submit-btn {
  width: 100%; height: 44px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  border: none; border-radius: 10px;
  color: #fff; font-size: 14px; font-weight: 600;
  cursor: pointer; letter-spacing: 2px;
  transition: all 0.3s; position: relative; overflow: hidden;
}
.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59,130,246,0.35);
}
.submit-btn:active:not(:disabled) { transform: translateY(0); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-loading {
  display: flex; align-items: center; justify-content: center; gap: 8px;
}
.spinner-sm {
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 右侧 Hero ===== */
.login-hero {
  flex: 1; position: relative;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, rgba(6,182,212,0.08), rgba(59,130,246,0.12));
  overflow: hidden;
}
.hero-content {
  position: relative; z-index: 2;
  padding: 40px; text-align: center;
}
.hero-badge {
  display: inline-block;
  padding: 5px 14px;
  background: rgba(6,182,212,0.12);
  border: 1px solid rgba(6,182,212,0.2);
  border-radius: 20px;
  font-size: 11px; color: #06b6d4;
  letter-spacing: 1px; margin-bottom: 24px;
}
.hero-title {
  font-size: 28px; font-weight: 700;
  color: #e2e8f0; line-height: 1.4;
  margin-bottom: 16px;
}
.gradient-text {
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.hero-desc {
  font-size: 13px; color: #64748b;
  line-height: 1.8; margin-bottom: 32px;
}
.hero-stats {
  display: flex; align-items: center; justify-content: center; gap: 24px;
}
.stat-item { text-align: center; }
.stat-num {
  font-size: 20px; font-weight: 700;
  color: #06b6d4;
}
.stat-label {
  font-size: 11px; color: #64748b;
  margin-top: 4px;
}
.stat-divider {
  width: 1px; height: 30px;
  background: rgba(59,130,246,0.15);
}

/* 装饰元素 */
.hero-deco-circle {
  position: absolute; border-radius: 50%;
  border: 1px solid rgba(6,182,212,0.1);
}
.deco-1 {
  width: 300px; height: 300px;
  top: -50px; right: -80px;
  animation: decoPulse 8s ease-in-out infinite;
}
.deco-2 {
  width: 200px; height: 200px;
  bottom: -30px; left: -40px;
  animation: decoPulse 10s ease-in-out infinite 2s;
}
@keyframes decoPulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.05); opacity: 0.6; }
}
.hero-deco-line {
  position: absolute;
  width: 200px; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(6,182,212,0.3), transparent);
  top: 30%; right: -20px;
  transform: rotate(-30deg);
}

/* ===== Toast ===== */
.login-toast {
  position: fixed; top: 20px; left: 50%; transform: translateX(-50%) translateY(-20px);
  padding: 10px 20px; border-radius: 8px;
  font-size: 13px; color: #fff; z-index: 9999;
  opacity: 0; transition: all 0.3s;
  backdrop-filter: blur(10px);
  pointer-events: none;
}
.login-toast.show { opacity: 1; transform: translateX(-50%) translateY(0); }
.login-toast.success { background: rgba(16,185,129,0.9); }
.login-toast.error { background: rgba(239,68,68,0.9); }
.login-toast.info { background: rgba(59,130,246,0.9); }
</style>
