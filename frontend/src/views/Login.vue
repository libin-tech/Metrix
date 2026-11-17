<template>
  <component :is="embedded ? 'div' : 'main'" class="login-page" :class="{embedded}">
    <header v-if="!embedded" class="public-header">
      <MetrixBrand />
      <div class="public-header-actions">
        <ThemeToggle />
        <LanguageToggle />
      </div>
    </header>

    <div class="login-shell">
      <section class="auth-card" aria-labelledby="login-heading">
        <MetrixBrand class="login-brand" />
        <template v-if="mode === 'user'">
          <h1 id="login-heading">{{ $t('login.userLoginTitle') }}</h1>
          <p class="mode-description">{{ $t('login.userLoginDescription') }}</p>
          <a-form layout="vertical" :model="userLoginForm" @finish="handleUserLogin">
            <a-form-item :label="$t('login.email')" name="email" :rules="emailRules"><a-input v-model:value="userLoginForm.email" autocomplete="username" size="large" :placeholder="$t('login.emailPlaceholder')"><template #prefix><MailOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.password')" name="password" :rules="requiredRules"><a-input-password v-model:value="userLoginForm.password" autocomplete="current-password" size="large" :placeholder="$t('login.passwordPlaceholder')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <a-form-item v-if="captchaEnabled" :label="$t('login.captcha')" name="captchaCode" :rules="requiredRules"><div class="captcha-field"><a-input v-model:value="userLoginForm.captchaCode" size="large" :maxlength="4" :placeholder="$t('login.captchaPlaceholder')" /><button type="button" class="captcha-image" :aria-label="$t('login.refreshCaptcha')" @click="refreshCaptcha"><img v-if="captcha.image" :src="captcha.image" :alt="$t('login.captcha')" /><ReloadOutlined v-else /></button></div></a-form-item>
            <div class="form-links"><button type="button" @click="changeMode('reset')">{{ $t('login.forgotPassword') }}</button></div>
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">{{ $t('login.login') }}</a-button>
          </a-form>
          <p class="mode-switch">{{ $t('login.noAccount') }} <button type="button" @click="changeMode('register')">{{ $t('login.registerNow') }}</button></p>
        </template>

        <template v-else-if="mode === 'register'">
          <h1 id="login-heading">{{ $t('login.registerTitle') }}</h1><p class="mode-description">{{ $t('login.registerDescription') }}</p>
          <a-form layout="vertical" :model="registerForm" @finish="handleRegister">
            <a-form-item :label="$t('login.nickname')" name="nickname" :rules="nicknameRules"><a-input v-model:value="registerForm.nickname" size="large" :placeholder="$t('login.nicknamePlaceholder')"><template #prefix><UserOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.email')" name="email" :rules="emailRules"><a-input v-model:value="registerForm.email" autocomplete="email" size="large" :placeholder="$t('login.emailPlaceholder')"><template #prefix><MailOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.password')" name="password" :rules="passwordRules"><a-input-password v-model:value="registerForm.password" autocomplete="new-password" size="large" :placeholder="$t('login.passwordPolicy')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <VerificationFields v-model:email-code="registerForm.emailCode" :sending="sendingCode" :countdown="codeCountdown" @send="requestEmailCode('REGISTER', registerForm)" />
            <a-checkbox v-model:checked="registerForm.privacyAgreed" class="privacy-agreement">{{ $t('login.privacyAgreementPrefix') }} <button type="button" @click.prevent.stop="showPrivacyPolicy = true">{{ $t('login.privacyPolicy') }}</button></a-checkbox>
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">{{ $t('login.register') }}</a-button>
          </a-form>
          <p class="mode-switch">{{ $t('login.hasAccount') }} <button type="button" @click="changeMode('user')">{{ $t('login.login') }}</button></p>
        </template>

        <template v-else-if="mode === 'reset'">
          <h1 id="login-heading">{{ $t('login.resetTitle') }}</h1><p class="mode-description">{{ $t('login.resetDescription') }}</p>
          <a-form layout="vertical" :model="resetForm" @finish="handleReset">
            <a-form-item :label="$t('login.email')" name="email" :rules="emailRules"><a-input v-model:value="resetForm.email" autocomplete="email" size="large" :placeholder="$t('login.emailPlaceholder')"><template #prefix><MailOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.newPassword')" name="password" :rules="passwordRules"><a-input-password v-model:value="resetForm.password" autocomplete="new-password" size="large" :placeholder="$t('login.passwordPolicy')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <a-form-item :label="$t('login.confirmPassword')" name="confirmPassword" :rules="confirmPasswordRules"><a-input-password v-model:value="resetForm.confirmPassword" autocomplete="new-password" size="large" :placeholder="$t('login.confirmPasswordPlaceholder')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <VerificationFields v-model:email-code="resetForm.emailCode" :sending="sendingCode" :countdown="codeCountdown" @send="requestEmailCode('RESET_PASSWORD', resetForm)" />
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">{{ $t('login.resetPassword') }}</a-button>
          </a-form>
          <p class="mode-switch"><button type="button" @click="changeMode('user')">{{ $t('login.backToLogin') }}</button></p>
        </template>

        <template v-else>
          <h1 id="login-heading">{{ $t('login.adminLoginTitle') }}</h1><p class="mode-description">{{ $t('login.adminLoginDescription') }}</p>
          <a-form layout="vertical" :model="adminForm" @finish="handleAdminLogin">
            <a-form-item :label="$t('login.username')" name="username" :rules="requiredRules"><a-input v-model:value="adminForm.username" autocomplete="username" size="large" :placeholder="$t('login.usernamePlaceholder')"><template #prefix><UserOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.password')" name="password" :rules="requiredRules"><a-input-password v-model:value="adminForm.password" autocomplete="current-password" size="large" :placeholder="$t('login.passwordPlaceholder')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <a-form-item v-if="captchaEnabled" :label="$t('login.captcha')" name="captchaCode" :rules="requiredRules"><div class="captcha-field"><a-input v-model:value="adminForm.captchaCode" size="large" :maxlength="4" :placeholder="$t('login.captchaPlaceholder')" /><button type="button" class="captcha-image" :aria-label="$t('login.refreshCaptcha')" @click="refreshCaptcha"><img v-if="captcha.image" :src="captcha.image" :alt="$t('login.captcha')" /><ReloadOutlined v-else /></button></div></a-form-item>
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">{{ $t('login.adminLogin') }}</a-button>
          </a-form>
          <p class="mode-switch"><button type="button" @click="changeMode('user')">{{ $t('login.userLogin') }}</button></p>
        </template>

        <div class="legal-row">{{ $t('login.legalPrefix') }} <button type="button" @click="showDisclaimer = true">{{ $t('login.disclaimer') }}</button></div>
        <button v-if="mode !== 'admin'" type="button" class="admin-entry" @click="changeMode('admin')"><SafetyCertificateOutlined />{{ $t('login.adminEntry') }}</button>
      </section>

    </div>

    <footer v-if="!embedded">{{ $t('common.copyright', { year: new Date().getFullYear() }) }}</footer>
    <a-modal :title="$t('login.disclaimer')" v-model:open="showDisclaimer" width="720px" :footer="null"><div class="disclaimer-content" v-html="renderedDisclaimer"></div></a-modal>
    <a-modal :title="$t('login.privacyPolicy')" v-model:open="showPrivacyPolicy" width="720px" :footer="null"><div class="disclaimer-content" v-html="renderedPrivacyPolicy"></div></a-modal>
    <a-modal v-model:open="showEmailCaptcha" :title="$t('login.captchaDialogTitle')" :ok-text="$t('login.captchaConfirm')" :cancel-text="$t('login.captchaCancel')" :confirm-loading="sendingCode" :mask-closable="!sendingCode" @ok="confirmEmailCode" @cancel="cancelEmailCaptcha"><p class="captcha-dialog-description">{{ $t('login.captchaDialogDescription') }}</p><div class="captcha-field"><a-input v-model:value="emailCaptchaCode" size="large" :maxlength="4" :placeholder="$t('login.captchaPlaceholder')" @press-enter="confirmEmailCode" /><button type="button" class="captcha-image" :aria-label="$t('login.refreshCaptcha')" @click="refreshCaptcha"><img v-if="captcha.image" :src="captcha.image" :alt="$t('login.captcha')" /><ReloadOutlined v-else /></button></div></a-modal>
  </component>
</template>

<script setup>
import {computed, defineComponent, h, onBeforeUnmount, onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {Button, Input, message} from 'ant-design-vue'
import {LockOutlined, MailOutlined, ReloadOutlined, SafetyCertificateOutlined, UserOutlined} from '@ant-design/icons-vue'
import {getCaptcha, getVerificationConfig, loginAdmin, loginUser, registerUser, resetUserPassword, sendEmailCode} from '../api'
import {marked} from 'marked'
import LanguageToggle from '../components/LanguageToggle.vue'
import MetrixBrand from '../components/MetrixBrand.vue'
import ThemeToggle from '../components/ThemeToggle.vue'

defineProps({embedded: {type: Boolean, default: false}})

const {t} = useI18n()
const router = useRouter()
const mode = ref('user')
const loading = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)
const captchaEnabled = ref(true)
const showDisclaimer = ref(false)
const showPrivacyPolicy = ref(false)
const showEmailCaptcha = ref(false)
const emailCaptchaCode = ref('')
const pendingEmailCode = ref(null)
const captcha = reactive({captchaId: '', image: ''})
const userLoginForm = reactive({email: '', password: '', captchaCode: ''})
const adminForm = reactive({username: '', password: '', captchaCode: ''})
const registerForm = reactive({nickname: '', email: '', password: '', emailCode: '', privacyAgreed: false})
const resetForm = reactive({email: '', password: '', confirmPassword: '', emailCode: ''})
const EMAIL_CODE_COOLDOWN_SECONDS = 60
let codeCountdownTimer

const requiredRules = [{required: true, message: t('login.required')}]
const emailRules = [{required: true, message: t('login.emailRequired')}, {type: 'email', message: t('login.emailInvalid')}]
const nicknameRules = [{required: true, message: t('login.nicknameRequired')}, {pattern: /^[A-Za-z\u4E00-\u9FFF]+$/, message: t('login.nicknameInvalid')}]
const passwordRules = [{required: true, message: t('login.passwordRequired')}, {pattern: /^(?=.{8,20}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])(?!.*\s).*$/, message: t('login.passwordPolicy')}]
const confirmPasswordRules = [{required: true, message: t('login.confirmPasswordRequired')}, {validator: (_rule, value) => value === resetForm.password ? Promise.resolve() : Promise.reject(t('login.passwordMismatch'))}]
const renderedDisclaimer = computed(() => marked(t('login.disclaimerContent')))
const renderedPrivacyPolicy = computed(() => marked(t('login.privacyPolicyContent')))

const VerificationFields = defineComponent({
  props: {emailCode: {type: String, default: ''}, sending: {type: Boolean, default: false}, countdown: {type: Number, default: 0}},
  emits: ['update:emailCode', 'send'],
  setup(props, {emit}) {
    return () => {
      const fields = []
      fields.push(h('div', {class: 'field-label'}, t('login.emailCode')))
      fields.push(h('div', {class: 'email-code-field'}, [h(Input, {value: props.emailCode, size: 'large', maxLength: 6, placeholder: t('login.emailCodePlaceholder'), 'onUpdate:value': value => emit('update:emailCode', value)}), h(Button, {disabled: props.sending || props.countdown > 0, onClick: () => emit('send')}, () => props.sending ? t('login.sendingCode') : props.countdown > 0 ? t('login.sendCodeCountdown', {seconds: props.countdown}) : t('login.sendCode'))]))
      return h('div', {class: 'verification-fields'}, fields)
    }
  }
})

const changeMode = nextMode => { mode.value = nextMode; if (captchaEnabled.value && (nextMode === 'admin' || nextMode === 'user' || nextMode === 'register' || nextMode === 'reset')) refreshCaptcha() }
const refreshCaptcha = async () => { if (!captchaEnabled.value) return; try { const response = await getCaptcha(); Object.assign(captcha, response.data) } catch { message.error(t('login.captchaLoadFailed')) } }
const persistSession = response => { localStorage.setItem('token', response.data.token); localStorage.setItem('userId', response.data.userId); localStorage.setItem('username', response.data.username); localStorage.setItem('userRole', response.data.role); router.push('/workspace') }
const handleUserLogin = async () => { loading.value = true; try { persistSession(await loginUser({...userLoginForm, captchaId: captcha.captchaId})) } finally { loading.value = false; if (captchaEnabled.value) refreshCaptcha() } }
const handleAdminLogin = async () => { loading.value = true; try { persistSession(await loginAdmin({...adminForm, captchaId: captcha.captchaId})) } finally { loading.value = false; if (captchaEnabled.value) refreshCaptcha() } }
const requestEmailCode = (purpose, form) => { if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) { message.warning(t('login.emailInvalid')); return } if (!captchaEnabled.value) { deliverEmailCode({email: form.email, purpose}); return } pendingEmailCode.value = {email: form.email, purpose}; emailCaptchaCode.value = ''; showEmailCaptcha.value = true; refreshCaptcha() }
const startCodeCountdown = () => { clearInterval(codeCountdownTimer); codeCountdown.value = EMAIL_CODE_COOLDOWN_SECONDS; codeCountdownTimer = setInterval(() => { codeCountdown.value -= 1; if (codeCountdown.value <= 0) { clearInterval(codeCountdownTimer); codeCountdownTimer = undefined } }, 1000) }
const deliverEmailCode = async ({email, purpose}, captchaCode = '') => { sendingCode.value = true; try { await sendEmailCode({email, purpose, captchaId: captcha.captchaId, captchaCode}); message.success(t('login.codeSent')); startCodeCountdown(); showEmailCaptcha.value = false; pendingEmailCode.value = null } catch (error) { if (!error.notified) { message.error(error.response?.data?.message || error.message || t('login.emailCodeSendFailed')); error.notified = true } } finally { sendingCode.value = false; emailCaptchaCode.value = ''; if (captchaEnabled.value) refreshCaptcha() } }
const confirmEmailCode = async () => { if (!emailCaptchaCode.value) { message.warning(t('login.captchaRequired')); return } if (pendingEmailCode.value) await deliverEmailCode(pendingEmailCode.value, emailCaptchaCode.value) }
const cancelEmailCaptcha = () => { if (sendingCode.value) return; showEmailCaptcha.value = false; emailCaptchaCode.value = ''; pendingEmailCode.value = null }
const handleRegister = async () => { if (!registerForm.privacyAgreed) { message.warning(t('login.privacyAgreementRequired')); return } loading.value = true; try { await registerUser(registerForm); message.success(t('login.registerSuccess')); userLoginForm.email = registerForm.email; changeMode('user') } finally { loading.value = false } }
const handleReset = async () => { loading.value = true; try { await resetUserPassword(resetForm); message.success(t('login.resetSuccess')); userLoginForm.email = resetForm.email; changeMode('user') } finally { loading.value = false } }
onMounted(async () => { try { const response = await getVerificationConfig(); captchaEnabled.value = response.data.captchaEnabled } catch { captchaEnabled.value = true } if (captchaEnabled.value) refreshCaptcha() })
onBeforeUnmount(() => clearInterval(codeCountdownTimer))
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  color: var(--workspace-ink);
  background: radial-gradient(ellipse at 50% 82%, var(--workspace-soft-accent), transparent 68%), var(--workspace-surface);
}

.login-shell {
  width: min(100% - 2 * var(--workspace-gutter), 420px);
  margin: auto;
  background: var(--workspace-surface);
  border: 1px solid var(--workspace-line);
  border-radius: var(--workspace-radius);
  box-shadow: var(--workspace-overlay-shadow);
}

.auth-card { padding: clamp(22px, 4.6vw, 36px); }
.login-brand { display: flex; margin: 0 auto 20px; }
.auth-card h1 { margin: 0 0 7px; font-size: 1.55rem; line-height: 1.25; letter-spacing: -.025em; text-align: center; }
.mode-description { margin: 0 0 22px; color: var(--workspace-muted); font-size: 14px; line-height: 1.6; text-align: center; }

.login-page.embedded { min-height: 0; border-radius: inherit; background: transparent; }
.embedded .login-shell { width: 100%; margin: 0; background: transparent; border: 0; box-shadow: none; }

.auth-card :deep(.ant-form-item) { margin-bottom: 16px; }
.auth-card :deep(.ant-form-item-label > label), .field-label { color: var(--workspace-ink); font-size: 13px; font-weight: 630; }
.auth-card :deep(.ant-form-item-label) { padding-bottom: 5px; }
.auth-card :deep(.ant-form-item-control-input) { min-height: 42px; }
.auth-card :deep(.ant-btn-primary) { height: 43px; border-radius: 999px; font-weight: 630; }

.form-links { display: flex; justify-content: flex-end; margin: -5px 0 13px; }
:is(.form-links, .mode-switch, .legal-row, .privacy-agreement) button, .admin-entry {
  padding: 2px 0;
  color: var(--workspace-accent);
  background: transparent;
  border: 0;
  cursor: pointer;
  font: inherit;
}

:is(.form-links, .mode-switch, .legal-row, .privacy-agreement) button:hover, .admin-entry:hover { text-decoration: underline; }
.form-links, .mode-switch { font-size: 13px; }
.mode-switch, .legal-row { color: var(--workspace-muted); text-align: center; }
.mode-switch { margin: 14px 0 0; }
.legal-row { margin: 16px 0 6px; padding-top: 13px; border-top: 1px solid var(--workspace-line); font-size: 12px; }
.admin-entry { display: flex; align-items: center; gap: 6px; margin: 0 auto; font-size: 12px; }

.verification-fields { display: grid; gap: 6px; margin-bottom: 16px; }
.captcha-field, .email-code-field { display: flex; align-items: center; gap: 8px; }
.captcha-field :deep(.ant-input), .email-code-field :deep(.ant-input) { flex: 1; min-width: 0; }
.captcha-image {
  display: grid;
  place-items: center;
  flex: 0 0 clamp(86px, 10vw, 120px);
  height: 40px;
  padding: 0;
  overflow: hidden;
  color: var(--workspace-accent);
  background: var(--workspace-raised);
  border: 1px solid var(--workspace-line);
  border-radius: var(--workspace-control-radius);
  cursor: pointer;
}

.captcha-image img { display: block; width: 100%; height: 100%; object-fit: contain; }
.email-code-field :deep(.ant-btn) { height: 40px; padding-inline: 9px; font-size: 12px; white-space: nowrap; }
.privacy-agreement { display: flex; align-items: center; margin: 0 0 16px; color: var(--workspace-muted); font-size: 12px; }
.captcha-dialog-description { margin: 0 0 16px; color: var(--workspace-muted); font-size: 13px; line-height: 1.6; }
.disclaimer-content { max-height: 60dvh; overflow: auto; color: var(--workspace-ink); font-size: 14px; line-height: 1.75; }
footer { padding: 24px var(--workspace-gutter); color: var(--workspace-muted); font-size: 12px; text-align: center; }

</style>
