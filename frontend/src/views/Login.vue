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
          <div class="login-tabs">
            <a-button
              :type="activeTab === 'admin' ? 'primary' : 'default'"
              block
              @click="activeTab = 'admin'"
            >{{ $t('login.adminTab') }}</a-button>
            <a-button
              :type="activeTab === 'wechat' ? 'primary' : 'default'"
              block
              @click="activeTab = 'wechat'"
            >{{ $t('login.userTab') }}</a-button>
          </div>

          <a-form
            v-if="activeTab === 'admin'"
            :model="form"
            layout="vertical"
            @finish="handleAdminLogin"
          >
            <a-form-item
              :label="$t('login.username')"
              name="username"
              :rules="[{ required: true, message: $t('login.usernamePlaceholder') }]"
            >
              <a-input v-model:value="form.username" size="large" :placeholder="$t('login.usernamePlaceholder')">
                <template #prefix><UserOutlined /></template>
              </a-input>
            </a-form-item>
            <a-form-item
              :label="$t('login.password')"
              name="password"
              :rules="[{ required: true, message: $t('login.passwordPlaceholder') }]"
            >
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

          <div v-else class="wechat-login">
            <div class="qr-section">
              <div class="qr-code">
                <img :src="qrCodeSrc" alt="QR" class="qr-image" @error="onQrError" v-show="!qrFailed" />
                <div v-if="qrFailed" class="qr-placeholder">
                  <WechatOutlined style="font-size: 48px; color: #07c160;" />
                  <span>公众号二维码</span>
                </div>
              </div>
              <p class="qr-instruction">{{ $t('login.qrInstruction') }}</p>
            </div>

            <a-input
              v-model:value="wechatCode"
              size="large"
              :placeholder="$t('login.verificationCodePlaceholder')"
              :maxlength="6"
              class="code-input"
              @input="freezeError = ''"
            >
              <template #prefix><SafetyOutlined /></template>
            </a-input>

            <div class="agree-row">
              <a-checkbox v-model:checked="agreed">
                {{ $t('login.agree') }}
                <a @click.stop.prevent="showDisclaimer = true">{{ $t('login.disclaimer') }}</a>
              </a-checkbox>
              <a-button type="link" size="small" @click="showDisclaimer = true">{{ $t('login.preview') }}</a-button>
            </div>

            <a-button
              type="primary"
              block
              size="large"
              :loading="loading"
              :disabled="wechatCode.length !== 6 || !agreed"
              @click="handleWechatLogin"
            >
              {{ $t('login.login') }}
            </a-button>

            <div v-if="freezeError" class="freeze-error">{{ freezeError }}</div>
          </div>

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
import {computed, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {message} from 'ant-design-vue'
import {LockOutlined, SafetyOutlined, UserOutlined, WechatOutlined} from '@ant-design/icons-vue'
import {login, loginByCode} from '../api'
import {marked} from 'marked'

const { locale, t } = useI18n()
const router = useRouter()
const loading = ref(false)
const agreed = ref(false)
const showDisclaimer = ref(false)
const activeTab = ref('wechat')
const wechatCode = ref('')
const qrFailed = ref(false)
const freezeError = ref('')

const form = reactive({
  username: '',
  password: ''
})

const qrCodeSrc = '/Mertix-lab-wx.jpg'

const renderedDisclaimer = computed(() => {
  return marked(t('login.disclaimerContent'))
})

const onQrError = () => {
  qrFailed.value = true
}

const switchLang = () => {
  const next = locale.value === 'zh-CN' ? 'en' : 'zh-CN'
  locale.value = next
  localStorage.setItem('locale', next)
}

const handleAdminLogin = async () => {
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

const handleWechatLogin = async () => {
  if (!agreed.value) {
    message.warning(t('login.agreeRequired'))
    return
  }
  freezeError.value = ''
  loading.value = true
  try {
    const response = await loginByCode(wechatCode.value)
    if (response.code !== 200) {
      if (response.code === 1001) {
        freezeError.value = response.message
      }
      return
    }
    localStorage.setItem('token', response.data.token)
    localStorage.setItem('userId', response.data.userId)
    localStorage.setItem('username', response.data.username)
    const nickname = response.data.nickname || ''
    if (nickname) {
      message.success(`${t('login.wechatLoginSuccess')} ${nickname}`)
    }
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
  height: 100vh;
  position: relative;
  overflow: hidden;
  background: radial-gradient(circle at 12% 12%, #dbe8ff 0, transparent 25%), radial-gradient(circle at 88% 82%, #e8eefb 0, transparent 29%), #f7f9fc;
}

.login-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.login-footer {
  flex-shrink: 0;
}

.login-left {
  display: none;
}

.login-right {
  width: 100%;
  max-width: 533px;
}

.login-form-wrap {
  background: rgba(255, 255, 255, .86);
  padding: 42px 36px;
  border: 1px solid rgba(218, 226, 239, .9);
  border-radius: 18px;
  box-shadow: 0 18px 50px rgba(38, 57, 91, .12);
  backdrop-filter: blur(14px);
}

.form-title {
  text-align: center;
  font-size: 24px;
  font-weight: 600;
  color: #182336;
  margin-bottom: 24px;
}

.login-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.login-tabs :deep(.ant-btn) { border-radius: 9px; }
.login-form-wrap :deep(.ant-input),
.login-form-wrap :deep(.ant-input-affix-wrapper) { border-color: #dce4ee; border-radius: 9px; }
.login-form-wrap :deep(.ant-btn-primary) { background: #5878c2; border-color: #5878c2; box-shadow: none; }

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
  color: #7d8a9c;
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
  color: #718098;
  margin: 20px 0 0;
  line-height: 1.7;
  text-align: center;
}

.wechat-login {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.qr-section {
  text-align: center;
}

.qr-code {
  width: 200px;
  height: 200px;
  margin: 0 auto 12px;
  border: 1px solid #e0e7f1;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.qr-image {
  width: 180px;
  height: 180px;
  object-fit: contain;
}

.qr-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 12px;
}

.qr-instruction {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.code-input {
  text-align: center;
  font-size: 18px;
  letter-spacing: 4px;
}

.freeze-error {
  color: #ff4d4f;
  font-size: 13px;
  text-align: center;
  padding: 8px 12px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 8px;
  line-height: 1.5;
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
    max-width: 420px;
  }

  .brand-icon {
    width: 420px;
    height: 420px;
    margin: 0 auto 18px;
  }

  .brand-logo {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}
</style>
