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

// 1. 静态文件 — public/ 目录（优先，包含本地 3D 库等）
const publicDir = path.join(__dirname, 'public')
if (fs.existsSync(publicDir)) app.use(express.static(publicDir))

// 2. API 代理 — 50085 后端接口（API文档明确列出）
app.use('/api/v1/auth', createProxyMiddleware({ target: BACKEND, changeOrigin: true }))
app.use('/api/v1/admin/suggestions', createProxyMiddleware({ target: BACKEND, changeOrigin: true }))
app.use('/api/v1/admin/digital', createProxyMiddleware({ target: BACKEND, changeOrigin: true }))
app.use('/api/v1/admin/vcn', createProxyMiddleware({ target: BACKEND, changeOrigin: true }))
app.use('/api/v1/admin/content', createProxyMiddleware({ target: BACKEND, changeOrigin: true }))
app.use('/api/v1/admin/knowledge', createProxyMiddleware({ target: BACKEND, changeOrigin: true }))
app.use('/api/v1/admin/dialogue', createProxyMiddleware({ target: BACKEND, changeOrigin: true }))

// 3. API 代理 — 50081 模型服务（兜底，所有未匹配的 /api 请求）
app.use('/api', createProxyMiddleware({ target: MODEL, changeOrigin: true }))

// 4. 静态文件 — dist/ 目录
const distDir = path.join(__dirname, 'dist')
app.use(express.static(distDir))

// 5. SPA 回退
app.use((req, res) => {
  const indexPath = path.join(distDir, 'index.html')
  if (fs.existsSync(indexPath)) {
    res.sendFile(indexPath)
  } else {
    res.status(500).send('dist/index.html not found. Please run `npm run build` first.')
  }
})

app.listen(PORT, () => {
  console.log(`\n  🚀 Server running at http://localhost:${PORT}/`)
  console.log(`  📡 50085 后端: /api/v1/auth/*, /api/v1/admin/digital/*, /api/v1/admin/vcn/*, /api/v1/admin/content/*, /api/v1/admin/knowledge/*, /api/v1/admin/dialogue/*, /api/v1/admin/suggestions*`)
  console.log(`  📡 50081 模型: /api/* (兜底)`)
  console.log(`  📂 Static files: ${distDir}\n`)
})
