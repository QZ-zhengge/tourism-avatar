import express from 'express'
import { createProxyMiddleware } from 'http-proxy-middleware'
import { fileURLToPath } from 'url'
import path from 'path'
import fs from 'fs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const app = express()
const PORT = process.env.PORT || 80

// 85 后端（API文档中列出的接口）
const BACKEND = 'http://192.0.2.1:50085'
// 81 模型服务（知识图谱、仪表盘等）
const MODEL = 'http://192.0.2.1:50081'

// ── 代理工具函数 ──
function apiProxy(target, prefix) {
  return createProxyMiddleware({
    target,
    changeOrigin: true,
    pathRewrite: (p) => prefix + p,
  })
}
function exactProxy(target, fullPath) {
  return createProxyMiddleware({
    target,
    changeOrigin: true,
    pathRewrite: () => fullPath,
  })
}

// 1. 静态文件 — public/ 目录
const publicDir = path.join(__dirname, 'public')
if (fs.existsSync(publicDir)) app.use(express.static(publicDir))

// 2. API 代理 — 50085 后端
app.use('/api/v1/auth',              apiProxy(BACKEND, '/api/v1/auth'))
app.use('/api/v1/admin/suggestions', apiProxy(BACKEND, '/api/v1/admin/suggestions'))
app.use('/api/v1/admin/digital',     apiProxy(BACKEND, '/api/v1/admin/digital'))
app.use('/api/v1/admin/vcn',         apiProxy(BACKEND, '/api/v1/admin/vcn'))
app.use('/api/v1/admin/content',     apiProxy(BACKEND, '/api/v1/admin/content'))
app.use('/api/v1/admin/dialogue',    apiProxy(BACKEND, '/api/v1/admin/dialogue'))
// Knowledge: preview/confirm/export 三个端点在 50081
app.use('/api/v1/admin/knowledge/preview',           exactProxy(MODEL, '/api/v1/admin/knowledge/preview'))
app.use('/api/v1/admin/knowledge/confirm',           exactProxy(MODEL, '/api/v1/admin/knowledge/confirm'))
app.use('/api/v1/admin/knowledge/export-to-markdown', exactProxy(MODEL, '/api/v1/admin/knowledge/export-to-markdown'))
app.use('/api/v1/admin/knowledge',   apiProxy(BACKEND, '/api/v1/admin/knowledge'))

// 2b. API 代理 — 50081 模型服务（dashboard / guide）
app.use('/api/v1/admin/dashboard',   apiProxy(MODEL, '/api/v1/admin/dashboard'))
app.use('/api/v1/guide',             apiProxy(MODEL, '/api/v1/guide'))

// 3. API 兜底 — 50081
app.use('/api', createProxyMiddleware({ target: MODEL, changeOrigin: true }))

// 4. 静态文件 — dist/
const distDir = path.join(__dirname, 'dist')
app.use(express.static(distDir))

// 5. SPA 回退
app.use((req, res) => {
  const indexPath = path.join(distDir, 'index.html')
  if (fs.existsSync(indexPath)) {
    res.sendFile(indexPath)
  } else {
    res.status(500).send('dist/index.html not found.')
  }
})

app.listen(PORT, () => {
  console.log(`\n  🚀 Server running at http://localhost:${PORT}/`)
  console.log(`  📡 50085: /api/v1/auth, /api/v1/admin/digital|vcn|content|dialogue|suggestions, /api/v1/admin/knowledge(除preview/confirm/export)`)
  console.log(`  📡 50081: /api/v1/admin/dashboard, /api/v1/guide, /api/v1/admin/knowledge/preview|confirm|export, /api/*`)
  console.log(`  📂 Static: ${distDir}\n`)
})
