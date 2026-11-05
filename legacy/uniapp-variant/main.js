import App from './App'
import Vue from 'vue'
import uView from 'uview-ui'

Vue.config.productionTip = false
Vue.use(uView)

// 全局注册数字人唤醒组件
import GlobalDh from '@/components/global-dh/index.vue'
Vue.component('global-dh', GlobalDh)

// 全局注册悬浮数字人助手
import FloatingAssistant from '@/components/floating-assistant/index.vue'
console.log('[FA] main.js: 开始注册 floating-assistant')
Vue.component('floating-assistant', FloatingAssistant)
console.log('[FA] main.js: 注册完成')

App.mpType = 'app'

const app = new Vue({
  ...App
})

app.$mount()
