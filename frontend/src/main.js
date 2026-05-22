import {createApp} from 'vue'
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import i18n from './i18n'
import {marked} from 'marked'

marked.use({gfm: true, breaks: true})

const app = createApp(App)
app.use(router)
app.use(Antd)
app.use(i18n)
app.mount('#app')
