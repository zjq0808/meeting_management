import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import Vant from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'
import store from './store'
import './styles/app.css'
import { initializeWeCom, initResponsiveRem } from './api/wecom'

initResponsiveRem()

initializeWeCom()
  .then(() => {
    createApp(App).use(store).use(router).use(ElementPlus).use(Vant).mount('#app')
  })
  .catch((error: Error) => {
    document.body.innerHTML = `<div class="boot-error">${error.message || '企业微信环境初始化失败'}</div>`
  })
