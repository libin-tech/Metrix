<template>
  <div class="login-container">
    <div class="login-content">
      <div class="login-left">
        <div class="brand">
          <div class="brand-icon">
            <img src="/Metrix.png" alt="Metrix" class="brand-logo" />
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-form-wrap">
          <h2 class="form-title">{{ $t('login.login') }}</h2>
          <a-form :model="form" layout="vertical" @finish="handleLogin">
            <a-form-item :label="$t('login.username')" name="username"
              :rules="[{ required: true, message: $t('login.usernamePlaceholder') }]">
              <a-input v-model:value="form.username" size="large" :placeholder="$t('login.usernamePlaceholder')">
                <template #prefix><UserOutlined /></template>
              </a-input>
            </a-form-item>
            <a-form-item :label="$t('login.password')" name="password"
              :rules="[{ required: true, message: $t('login.passwordPlaceholder') }]">
              <a-input-password v-model:value="form.password" size="large" :placeholder="$t('login.passwordPlaceholder')">
                <template #prefix><LockOutlined /></template>
              </a-input-password>
            </a-form-item>
            <a-form-item name="agree">
              <div class="agree-row">
                <a-checkbox v-model:checked="agreed">
                  {{ $t('login.agree') }}
                  <a @click.stop.prevent="showDisclaimer = true">{{ $t('login.disclaimer') }}</a>
                </a-checkbox>
                <a-button type="link" size="small" @click="showDisclaimer = true">{{ $t('login.preview') }}</a-button>
              </div>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" block size="large" :loading="loading" :disabled="!agreed">
                {{ $t('login.login') }}
              </a-button>
            </a-form-item>
          </a-form>
          <p class="login-subtitle">{{ $t('login.subtitle') }}</p>
        </div>
      </div>
    </div>
    <div class="login-footer">
      <div class="footer-links">
        <a @click="switchLang">{{ locale === 'zh-CN' ? 'English' : '中文' }}</a>
      </div>
      <div class="footer-copyright">{{ $t('common.copyright', { year: new Date().getFullYear() }) }}</div>
      <div class="footer-chrome">{{ $t('common.recommendChrome') }}</div>
    </div>

    <a-modal :title="$t('login.disclaimer')" v-model:visible="showDisclaimer" width="720px" :footer="null">
      <div class="disclaimer-content" v-html="renderedDisclaimer"></div>
    </a-modal>
  </div>
</template>

<script setup>
import {reactive, ref, computed} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message} from 'ant-design-vue'
import {LockOutlined, UserOutlined} from '@ant-design/icons-vue'
import {login} from '../api'
import {marked} from 'marked'

const {locale, t} = useI18n()
const router = useRouter()
const loading = ref(false)
const agreed = ref(false)
const showDisclaimer = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const renderedDisclaimer = computed(() => {
  return marked(t('login.disclaimerContent'))
})

const switchLang = () => {
  const next = locale.value === 'zh-CN' ? 'en' : 'zh-CN'
  locale.value = next
  localStorage.setItem('locale', next)
}

const handleLogin = async () => {
  if (!agreed.value) {
    message.warning(t('login.agreeRequired'))
    return
  }
  loading.value = true
  try {
    const response = await login(form)
    localStorage.setItem('token', response.data.token)
    localStorage.setItem('userId', response.data.userId)
    localStorage.setItem('username', response.data.username)
    router.push('/')
  } catch (error) {
    message.error(error.response?.data?.message || t('login.failed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f0f2f5;
}

.login-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.login-left {
  display: none;
}

.login-right {
  width: 100%;
  max-width: 400px;
}

.login-form-wrap {
  background: #fff;
  padding: 40px 32px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.form-title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 32px;
}

.agree-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.agree-row a {
  color: #1890ff;
}

.login-footer {
  text-align: center;
  padding: 24px 16px;
  font-size: 13px;
  color: #999;
}

.footer-links {
  margin-bottom: 8px;
}

.footer-links a {
  color: #999;
  cursor: pointer;
  font-size: 13px;
}

.footer-links a:hover {
  color: #1890ff;
}

.footer-copyright {
  margin-bottom: 4px;
}

.disclaimer-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  max-height: 60vh;
  overflow-y: auto;
}

.login-subtitle {
  font-size: 13px;
  color: #888;
  margin: 20px 0 0;
  line-height: 1.7;
  text-align: center;
}

@media (min-width: 768px) {
  .login-content {
    gap: 80px;
  }

  .login-left {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .brand {
    text-align: center;
    max-width: 400px;
  }

  .brand-icon {
    width: 500px;
    height: 500px;
    margin: 0 auto 32px;
  }

  .brand-logo {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}
</style>
