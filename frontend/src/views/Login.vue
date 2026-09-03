<template>
  <main class="login-page">
    <header class="login-header">
      <RouterLink class="brand" to="/"><img src="/Metrix-logo.png" alt="Metrix" /><span>Metrix</span></RouterLink>
      <button class="language-button" type="button" @click="switchLanguage"><GlobalOutlined />{{ locale === 'zh-CN' ? 'English' : '中文' }}</button>
    </header>

    <section class="login-shell">
      <aside class="login-aside">
        <p class="aside-eyebrow">METRIX / RESEARCH DESK</p>
        <h1>{{ $t('login.asideTitle') }}</h1>
        <p>{{ $t('login.asideDescription') }}</p>
        <div class="aside-line"></div>
        <small>{{ $t('login.asideNote') }}</small>
      </aside>

      <section class="auth-card" :aria-label="$t('login.title')">
        <template v-if="mode === 'user'">
          <p class="mode-label">{{ $t('login.userEyebrow') }}</p>
          <h2>{{ $t('login.userLoginTitle') }}</h2>
          <p class="mode-description">{{ $t('login.userLoginDescription') }}</p>
          <a-form layout="vertical" :model="userLoginForm" @finish="handleUserLogin">
            <a-form-item :label="$t('login.email')" name="email" :rules="emailRules"><a-input v-model:value="userLoginForm.email" size="large" :placeholder="$t('login.emailPlaceholder')"><template #prefix><MailOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.password')" name="password" :rules="requiredRules"><a-input-password v-model:value="userLoginForm.password" size="large" :placeholder="$t('login.passwordPlaceholder')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <a-form-item v-if="captchaEnabled" :label="$t('login.captcha')" name="captchaCode" :rules="requiredRules"><div class="captcha-field"><a-input v-model:value="userLoginForm.captchaCode" size="large" :maxlength="4" :placeholder="$t('login.captchaPlaceholder')" /><button type="button" class="captcha-image" :aria-label="$t('login.refreshCaptcha')" @click="refreshCaptcha"><img v-if="captcha.image" :src="captcha.image" :alt="$t('login.captcha')" /><ReloadOutlined v-else /></button></div></a-form-item>
            <div class="form-links"><button type="button" @click="changeMode('reset')">{{ $t('login.forgotPassword') }}</button></div>
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">{{ $t('login.login') }}</a-button>
          </a-form>
          <p class="mode-switch">{{ $t('login.noAccount') }} <button type="button" @click="changeMode('register')">{{ $t('login.registerNow') }}</button></p>
        </template>

        <template v-else-if="mode === 'register'">
          <p class="mode-label">{{ $t('login.userEyebrow') }}</p><h2>{{ $t('login.registerTitle') }}</h2><p class="mode-description">{{ $t('login.registerDescription') }}</p>
          <a-form layout="vertical" :model="registerForm" @finish="handleRegister">
            <a-form-item :label="$t('login.nickname')" name="nickname" :rules="nicknameRules"><a-input v-model:value="registerForm.nickname" size="large" :placeholder="$t('login.nicknamePlaceholder')"><template #prefix><UserOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.email')" name="email" :rules="emailRules"><a-input v-model:value="registerForm.email" size="large" :placeholder="$t('login.emailPlaceholder')"><template #prefix><MailOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.password')" name="password" :rules="passwordRules"><a-input-password v-model:value="registerForm.password" size="large" :placeholder="$t('login.passwordPolicy')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <VerificationFields v-model:email-code="registerForm.emailCode" :sending="sendingCode" :countdown="codeCountdown" @send="requestEmailCode('REGISTER', registerForm)" />
            <a-checkbox v-model:checked="registerForm.privacyAgreed" class="privacy-agreement">{{ $t('login.privacyAgreementPrefix') }} <button type="button" @click.prevent.stop="showPrivacyPolicy = true">{{ $t('login.privacyPolicy') }}</button></a-checkbox>
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">{{ $t('login.register') }}</a-button>
          </a-form>
          <p class="mode-switch">{{ $t('login.hasAccount') }} <button type="button" @click="changeMode('user')">{{ $t('login.login') }}</button></p>
        </template>

        <template v-else-if="mode === 'reset'">
          <p class="mode-label">{{ $t('login.userEyebrow') }}</p><h2>{{ $t('login.resetTitle') }}</h2><p class="mode-description">{{ $t('login.resetDescription') }}</p>
          <a-form layout="vertical" :model="resetForm" @finish="handleReset">
            <a-form-item :label="$t('login.email')" name="email" :rules="emailRules"><a-input v-model:value="resetForm.email" size="large" :placeholder="$t('login.emailPlaceholder')"><template #prefix><MailOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.newPassword')" name="password" :rules="passwordRules"><a-input-password v-model:value="resetForm.password" size="large" :placeholder="$t('login.passwordPolicy')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <a-form-item :label="$t('login.confirmPassword')" name="confirmPassword" :rules="confirmPasswordRules"><a-input-password v-model:value="resetForm.confirmPassword" size="large" :placeholder="$t('login.confirmPasswordPlaceholder')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <VerificationFields v-model:email-code="resetForm.emailCode" :sending="sendingCode" :countdown="codeCountdown" @send="requestEmailCode('RESET_PASSWORD', resetForm)" />
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">{{ $t('login.resetPassword') }}</a-button>
          </a-form>
          <p class="mode-switch"><button type="button" @click="changeMode('user')">{{ $t('login.backToLogin') }}</button></p>
        </template>

        <template v-else>
          <p class="mode-label">{{ $t('login.adminEyebrow') }}</p><h2>{{ $t('login.adminLoginTitle') }}</h2><p class="mode-description">{{ $t('login.adminLoginDescription') }}</p>
          <a-form layout="vertical" :model="adminForm" @finish="handleAdminLogin">
            <a-form-item :label="$t('login.username')" name="username" :rules="requiredRules"><a-input v-model:value="adminForm.username" size="large" :placeholder="$t('login.usernamePlaceholder')"><template #prefix><UserOutlined /></template></a-input></a-form-item>
            <a-form-item :label="$t('login.password')" name="password" :rules="requiredRules"><a-input-password v-model:value="adminForm.password" size="large" :placeholder="$t('login.passwordPlaceholder')"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
            <a-form-item v-if="captchaEnabled" :label="$t('login.captcha')" name="captchaCode" :rules="requiredRules"><div class="captcha-field"><a-input v-model:value="adminForm.captchaCode" size="large" :maxlength="4" :placeholder="$t('login.captchaPlaceholder')" /><button type="button" class="captcha-image" :aria-label="$t('login.refreshCaptcha')" @click="refreshCaptcha"><img v-if="captcha.image" :src="captcha.image" :alt="$t('login.captcha')" /><ReloadOutlined v-else /></button></div></a-form-item>
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">{{ $t('login.adminLogin') }}</a-button>
          </a-form>
          <p class="mode-switch"><button type="button" @click="changeMode('user')">{{ $t('login.userLogin') }}</button></p>
        </template>

        <div class="legal-row">{{ $t('login.legalPrefix') }} <button type="button" @click="showDisclaimer = true">{{ $t('login.disclaimer') }}</button></div>
        <button v-if="mode !== 'admin'" type="button" class="admin-entry" @click="changeMode('admin')"><SafetyCertificateOutlined />{{ $t('login.adminEntry') }}</button>
      </section>
    </section>

    <footer>{{ $t('common.copyright', { year: new Date().getFullYear() }) }}</footer>
    <a-modal :title="$t('login.disclaimer')" v-model:open="showDisclaimer" width="720px" :footer="null"><div class="disclaimer-content" v-html="renderedDisclaimer"></div></a-modal>
    <a-modal :title="$t('login.privacyPolicy')" v-model:open="showPrivacyPolicy" width="720px" :footer="null"><div class="disclaimer-content" v-html="renderedPrivacyPolicy"></div></a-modal>
    <a-modal v-model:open="showEmailCaptcha" :title="$t('login.captchaDialogTitle')" :ok-text="$t('login.captchaConfirm')" :cancel-text="$t('login.captchaCancel')" :confirm-loading="sendingCode" :mask-closable="!sendingCode" @ok="confirmEmailCode" @cancel="cancelEmailCaptcha"><p class="captcha-dialog-description">{{ $t('login.captchaDialogDescription') }}</p><div class="captcha-field"><a-input v-model:value="emailCaptchaCode" size="large" :maxlength="4" :placeholder="$t('login.captchaPlaceholder')" @press-enter="confirmEmailCode" /><button type="button" class="captcha-image" :aria-label="$t('login.refreshCaptcha')" @click="refreshCaptcha"><img v-if="captcha.image" :src="captcha.image" :alt="$t('login.captcha')" /><ReloadOutlined v-else /></button></div></a-modal>
  </main>
</template>

<script setup>
import {computed, defineComponent, h, onBeforeUnmount, onMounted, reactive, ref} from 'vue'
import {RouterLink, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {Button, Input, message} from 'ant-design-vue'
import {GlobalOutlined, LockOutlined, MailOutlined, ReloadOutlined, SafetyCertificateOutlined, UserOutlined} from '@ant-design/icons-vue'
import {getCaptcha, getVerificationConfig, loginAdmin, loginUser, registerUser, resetUserPassword, sendEmailCode} from '../api'
import {marked} from 'marked'

const {locale, t} = useI18n()
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

const switchLanguage = () => { locale.value = locale.value === 'zh-CN' ? 'en' : 'zh-CN'; localStorage.setItem('locale', locale.value) }
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
.login-page{min-height:100vh;display:flex;flex-direction:column;background:radial-gradient(circle at 5% 20%,#e8f0ff 0,transparent 27%),radial-gradient(circle at 90% 90%,#fce9e2 0,transparent 28%),#f8fafc;color:#17253c}.login-header{width:min(1200px,100%);height:76px;margin:0 auto;padding:0 28px;display:flex;align-items:center;justify-content:space-between}.brand{display:flex;gap:9px;align-items:center;text-decoration:none;color:#17253c;font-weight:800;font-size:19px}.brand img{width:31px;height:31px;object-fit:contain}.language-button,.form-links button,.mode-switch button,.legal-row button,.admin-entry,.privacy-agreement button{background:none;border:0;cursor:pointer;font:inherit}.language-button{display:flex;align-items:center;gap:7px;font-size:13px;color:#56647a}.login-shell{width:min(1020px,100%);margin:auto;display:grid;grid-template-columns:.9fr 1fr;align-items:center;gap:72px;padding:40px 28px 70px}.login-aside{padding:24px}.aside-eyebrow,.mode-label{font:500 11px ui-monospace,SFMono-Regular,Menlo,monospace;letter-spacing:1.5px;color:#e76b55}.login-aside h1{font-size:clamp(37px,4vw,54px);letter-spacing:-2.5px;line-height:1.1;margin:20px 0;color:#1e2d45}.login-aside>p:not(.aside-eyebrow){max-width:330px;font-size:15px;line-height:1.8;color:#657288}.aside-line{height:1px;width:70px;background:#ed785f;margin:35px 0 18px}.login-aside small{font-size:12px;color:#7f8c9d}.auth-card{padding:42px 38px 26px;border:1px solid #e1e7f0;border-radius:19px;background:rgba(255,255,255,.86);box-shadow:0 24px 65px rgba(28,46,74,.12);backdrop-filter:blur(14px)}.auth-card h2{font-size:27px;letter-spacing:-1px;margin:9px 0}.mode-description{font-size:13px;line-height:1.65;color:#738094;margin-bottom:26px}.auth-card :deep(.ant-form-item){margin-bottom:17px}.auth-card :deep(.ant-form-item-label>label),.field-label{font-size:13px;font-weight:600;color:#344259}.auth-card :deep(.ant-input),.auth-card :deep(.ant-input-affix-wrapper){border-radius:9px;border-color:#dbe3ed}.auth-card :deep(.ant-btn-primary){background:#243a5b;border-color:#243a5b;border-radius:9px;height:42px;box-shadow:none}.form-links{display:flex;justify-content:flex-end;margin:-6px 0 19px}.form-links button,.mode-switch button{font-size:13px;color:#537bbd}.mode-switch{text-align:center;margin:19px 0 3px;font-size:13px;color:#778397}.verification-fields{display:grid;gap:8px;margin-bottom:20px}.field-label{margin-top:1px}.captcha-field,.email-code-field{display:flex;gap:9px}.captcha-field :deep(.ant-input),.email-code-field :deep(.ant-input){flex:1}.captcha-image{width:120px;height:40px;padding:0;display:grid;place-content:center;overflow:hidden;border:1px solid #dbe3ed;border-radius:8px;background:#f0f3f7;cursor:pointer}.captcha-image img{display:block;width:120px;height:40px}.email-code-field :deep(.ant-btn){height:40px;border-radius:8px;font-size:12px;white-space:nowrap}.privacy-agreement{display:flex;margin:-3px 0 20px;color:#778397;font-size:12px;line-height:1.6}.privacy-agreement button{padding:0;color:#537bbd}.captcha-dialog-description{margin:0 0 17px;font-size:13px;line-height:1.6;color:#6f7c90}.legal-row{margin:24px 0 13px;text-align:center;font-size:11px;color:#99a3b2}.legal-row button{padding:0;color:#537bbd;font-size:11px}.admin-entry{margin:0 auto;display:flex;gap:6px;align-items:center;color:#758297;font-size:12px}.disclaimer-content{max-height:60vh;overflow:auto;font-size:14px;line-height:1.75;color:#374151}footer{padding:22px;text-align:center;font-size:12px;color:#98a3b2}@media(max-width:760px){.login-header{height:68px;padding:0 20px}.login-shell{display:block;padding:15px 20px 42px}.login-aside{display:none}.auth-card{max-width:520px;margin:auto;padding:34px 25px 23px}.captcha-image{width:105px}.captcha-image img{width:105px}.login-page{min-height:100svh}}
</style>
