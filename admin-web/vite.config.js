import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 85 端口 = 后端（API文档中列出的接口）
const BACKEND = 'http://192.0.2.1:50085'
// 81 端口 = 模型服务（知识图谱、仪表盘等，不在API文档中）
const MODEL = 'http://192.0.2.1:50081'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 80,
    host: '0.0.0.0',
    proxy: {
      // ===== 50085 后端接口（API文档明确列出） =====
      '/api/v1/auth/': { target: BACKEND, changeOrigin: true },
      '/api/v1/admin/suggestions': { target: BACKEND, changeOrigin: true },
      '/api/v1/admin/suggestions/': { target: BACKEND, changeOrigin: true },
      '/api/v1/admin/digital/': { target: BACKEND, changeOrigin: true },
      '/api/v1/admin/vcn/': { target: BACKEND, changeOrigin: true },
      '/api/v1/admin/content/': { target: BACKEND, changeOrigin: true },
      '/api/v1/admin/knowledge/': { target: BACKEND, changeOrigin: true },
      '/api/v1/admin/dialogue/': { target: BACKEND, changeOrigin: true },
      // ===== 50081 模型服务（兜底，所有未匹配的 /api 请求） =====
      '/api': { target: MODEL, changeOrigin: true },
    }
  }
})
