import { build } from 'vite'
import vue from '@vitejs/plugin-vue'

await build({
  plugins: [vue()],
  root: 'Z:/web/vue-app',
  configFile: false,
  build: { emptyOutDir: true }
})
