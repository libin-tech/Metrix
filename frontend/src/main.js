import {createApp} from 'vue'
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/workspace.css'
import './styles/theme.css'
import {initializeTheme} from './composables/useTheme'
import i18n from './i18n'
import {marked} from 'marked'
import 'markstream-vue/index.css'

marked.use({gfm: true, breaks: true})

initializeTheme()

const app = createApp(App)
app.use(router)
app.use(Antd)
app.use(i18n)
app.mount('#app')
