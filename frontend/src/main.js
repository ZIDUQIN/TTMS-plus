import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './styles/global.css'

const app = createApp(App)

// Register all Element Plus icons globally
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, { locale: zhCn })
app.use(createPinia())
app.use(router)

// 全局缩短消息通知停留时间（默认3000ms → 1200ms）
const SHORT = 800
const wrapMsg = (fn) => (msg, durOrOpts) => {
  if (typeof durOrOpts === 'number') return fn({ message: msg, duration: durOrOpts })
  if (typeof durOrOpts === 'string') return fn({ message: msg, duration: SHORT })
  return fn({ duration: SHORT, ...(typeof msg === 'string' ? { message: msg } : msg) })
}
ElMessage.success = wrapMsg(ElMessage.success.bind(ElMessage))
ElMessage.error   = wrapMsg(ElMessage.error.bind(ElMessage))
ElMessage.warning = wrapMsg(ElMessage.warning.bind(ElMessage))
ElMessage.info    = wrapMsg(ElMessage.info.bind(ElMessage))

app.mount('#app')
