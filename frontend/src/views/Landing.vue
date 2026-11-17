<template>
  <header class="site-header">
    <div class="public-header site-header-inner">
      <MetrixBrand />
      <nav class="site-nav" :aria-label="$t('landing.navigation')">
        <RouterLink
          v-for="item in navigation"
          :key="item.hash"
          :to="{path: '/', hash: item.hash}"
          :aria-current="currentSection === item.hash ? 'location' : undefined"
        >{{ $t(item.label) }}</RouterLink>
      </nav>
      <div class="public-header-actions">
        <ThemeToggle />
        <LanguageToggle />
        <RouterLink class="pill-button header-start" to="/login">{{ $t('landing.startNow') }} <ArrowRightOutlined /></RouterLink>
        <button class="site-login" type="button" @click="loginOpen = true">{{ $t('landing.login') }}</button>
      </div>
    </div>
  </header>

  <main id="top" class="landing-page">
    <section class="hero">
      <div class="section-wrap">
        <div class="hero-copy">
          <h1><img src="/metrix-mark.webp" alt="" width="82" height="86" /> <span>Metrix</span></h1>
          <p>{{ $t('landing.description') }}</p>
          <div class="hero-actions">
            <RouterLink class="pill-button hero-login" to="/login">{{ $t('landing.startNow') }} Metrix <ArrowRightOutlined /></RouterLink>
            <RouterLink class="feature-link" to="/#features">{{ $t('landing.exploreFeatures') }}</RouterLink>
          </div>
        </div>
        <LandingPreview class="hero-preview" featured />
      </div>
    </section>

    <section id="features" class="section-wrap section-block">
      <div class="section-heading centered-heading">
        <h2>{{ $t('landing.featuresTitle') }}</h2>
        <p>{{ $t('landing.featuresDescription') }}</p>
      </div>
      <div class="scene-controls">
        <a-segmented v-model:value="activeScene" :options="sceneOptions" :aria-label="$t('landing.scenesLabel')" />
      </div>
      <div class="scene-card">
        <div class="scene-copy">
          <h3>{{ $t(`landing.scenes.${activeScene}.title`) }}</h3>
          <p>{{ $t(`landing.scenes.${activeScene}.description`) }}</p>
        </div>
        <div class="scene-media"><LandingPreview :key="activeScene" :view="activeScene" /></div>
      </div>
    </section>

    <section class="section-wrap section-block integrations">
      <div class="section-heading centered-heading">
        <h2>{{ $t('landing.integrationsTitle') }}</h2>
        <p>{{ $t('landing.integrationsDescription') }}</p>
      </div>
      <ul class="integration-list">
        <li v-for="provider in providers" :key="provider.label">
          <component :is="provider.icon" aria-hidden="true" /> {{ $t(provider.label) }}
        </li>
      </ul>
    </section>

    <section id="advantages" class="section-wrap">
      <div class="value-section">
        <div class="section-heading value-heading">
          <h2>{{ $t('landing.advantagesTitle') }}</h2>
          <p>{{ $t('landing.advantagesDescription') }}</p>
        </div>
        <div class="feature-grid">
          <article v-for="value in values" :key="value.key" class="value-card">
            <component :is="value.icon" aria-hidden="true" />
            <h3>{{ $t(`landing.advantages.${value.key}.title`) }}</h3>
            <p>{{ $t(`landing.advantages.${value.key}.description`) }}</p>
          </article>
        </div>
      </div>
    </section>

    <section id="guide" class="section-wrap section-block guide-section">
      <div class="section-heading centered-heading"><h2>{{ $t('landing.guideTitle') }}</h2></div>
      <ol class="guide-grid">
        <li v-for="step in guideSteps" :key="step.key">
          <component :is="step.icon" aria-hidden="true" />
          <h3>{{ $t(`landing.guide.${step.key}.title`) }}</h3>
          <p>{{ $t(`landing.guide.${step.key}.description`) }}</p>
        </li>
      </ol>
      <RouterLink class="pill-button guide-login" to="/login">{{ $t('landing.goToLogin') }} <ArrowRightOutlined /></RouterLink>
    </section>
  </main>

  <footer class="site-footer">
    <div class="section-wrap footer-layout">
      <div class="footer-copy"><MetrixBrand /><p>{{ $t('landing.disclaimer') }}</p></div>
      <nav :aria-label="$t('landing.navigation')">
        <RouterLink v-for="item in navigation.slice(1)" :key="item.hash" :to="{path:'/', hash:item.hash}">{{ $t(item.label) }}</RouterLink>
      </nav>
      <div class="footer-contact">
        <img src="/Mertix-lab-wx.jpg" width="72" height="72" loading="lazy" :alt="$t('landing.wechatQrAlt')" />
        <div><strong>{{ $t('landing.contactTitle') }}</strong><p>{{ $t('landing.contactDescription') }}</p></div>
      </div>
    </div>
  </footer>

  <a-modal
    v-model:open="loginOpen"
    wrap-class-name="site-login-layer"
    :width="420"
    :footer="null"
    :body-style="{padding: 0}"
    centered
    destroy-on-close
  >
    <template #title><span class="login-dialog-title">{{ $t('landing.login') }}</span></template>
    <EmbeddedLogin embedded />
  </a-modal>
</template>

<script setup>
import {computed, defineAsyncComponent, ref} from 'vue'
import {useRoute} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {
  ApiOutlined, ArrowRightOutlined, BellOutlined, DatabaseOutlined, FileSearchOutlined, FundOutlined,
  LineChartOutlined, LoginOutlined, SafetyCertificateOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import LandingPreview from '../components/LandingPreview.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import MetrixBrand from '../components/MetrixBrand.vue'
import ThemeToggle from '../components/ThemeToggle.vue'

const EmbeddedLogin = defineAsyncComponent(() => import('./Login.vue'))
const {t} = useI18n()
const route = useRoute()
const loginOpen = ref(false)
const activeScene = ref('market')
const navigation = [
  {hash: '#top', label: 'menu.home'},
  {hash: '#features', label: 'landing.navigationFeatures'},
  {hash: '#advantages', label: 'landing.navigationAdvantages'},
  {hash: '#guide', label: 'landing.navigationGuide'}
]
const currentSection = computed(() => route.hash || '#top')
const sceneOptions = computed(() => ['market', 'research', 'portfolio', 'review'].map(view => ({
  value: view, label: t(`landing.scenes.${view}.tab`)
})))
const providers = [
  {label: 'landing.providers.market', icon: FundOutlined},
  {label: 'landing.providers.news', icon: FileSearchOutlined},
  {label: 'landing.providers.models', icon: ApiOutlined},
  {label: 'landing.providers.localModels', icon: DatabaseOutlined},
  {label: 'landing.providers.notifications', icon: BellOutlined}
]
const values = [
  {key: 'focus', icon: FileSearchOutlined},
  {key: 'traceable', icon: LineChartOutlined},
  {key: 'yours', icon: SafetyCertificateOutlined}
]
const guideSteps = [
  {key: 'login', icon: LoginOutlined},
  {key: 'configure', icon: SettingOutlined},
  {key: 'research', icon: LineChartOutlined}
]
</script>

<style scoped>
.landing-page, .site-footer { color: var(--workspace-ink); background: var(--workspace-surface); }
.landing-page { --site-header-height: 66px; }
.landing-page [id], .landing-page { scroll-margin-top: calc(var(--site-header-height) + 14px); }

.site-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--workspace-surface);
  border-bottom: 1px solid var(--workspace-line);
}

.site-header-inner { width: min(100%, 1440px); min-height: 64px; padding-block: 9px; }
.site-nav { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; gap: 0 clamp(12px, 2.5vw, 32px); }
.site-nav a, .feature-link { text-decoration: none; }
.site-nav a { position: relative; padding-block: 9px; color: var(--workspace-muted); font-size: 14px; font-weight: 520; white-space: nowrap; }
.site-nav a:hover, .site-nav a[aria-current='location'] { color: var(--workspace-accent); }
.site-nav a[aria-current='location']::after { position: absolute; inset: auto 0 3px; height: 2px; background: currentColor; content: ''; }

.site-login { padding: 8px 4px; color: inherit; background: transparent; border: 0; cursor: pointer; font-weight: 650; white-space: nowrap; }
.site-login:hover, .feature-link:hover { color: var(--workspace-accent); text-decoration: underline; }
.pill-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 8px 19px;
  color: var(--workspace-on-primary);
  background: var(--workspace-primary);
  border-radius: 999px;
  font-size: 14px;
  font-weight: 620;
  line-height: 1.35;
  text-decoration: none;
  white-space: nowrap;
}

.pill-button:hover { color: var(--workspace-on-primary); background: var(--workspace-primary-hover); }
.section-wrap { width: min(100%, 1260px); margin-inline: auto; padding-inline: var(--workspace-gutter); }
.hero {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  padding-block: clamp(48px, 6.3vw, 90px) clamp(36px, 5.5vw, 78px);
  background:
    radial-gradient(ellipse at 8% 88%, color-mix(in srgb, var(--workspace-halo) 56%, transparent), transparent 63%),
    radial-gradient(ellipse at 92% 84%, color-mix(in srgb, var(--workspace-halo) 60%, transparent), transparent 61%),
    var(--workspace-surface);
}

.hero::before {
  position: absolute;
  z-index: -1;
  inset: 0;
  background: radial-gradient(ellipse at 50% 15%, var(--workspace-surface) 22%, transparent 65%);
  pointer-events: none;
  content: '';
}

.hero-copy { display: grid; justify-items: center; gap: 20px; text-align: center; }
.hero-copy h1 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: clamp(10px, 1.6vw, 21px);
  margin: 0;
  font-size: clamp(3.9rem, 8.25vw, 6.6rem);
  font-weight: 660;
  letter-spacing: -.068em;
  line-height: .98;
}

.hero-copy h1 img { width: clamp(50px, 6.3vw, 82px); height: auto; object-fit: contain; }
.hero-copy p, .section-heading p { max-width: 550px; margin: 0; color: var(--workspace-muted); font-size: 16px; line-height: 1.75; text-wrap: pretty; }
.hero-actions { display: grid; justify-items: center; gap: 14px; padding-top: 8px; }
.hero-login { min-height: 48px; padding-inline: 24px; font-size: 16px; }
.feature-link { color: var(--workspace-accent); font-size: 14px; font-weight: 560; }
.hero-preview { width: min(100%, 960px); margin: clamp(30px, 5vw, 70px) auto 0; }

.section-block { padding-block: clamp(52px, 7.8vw, 108px); }
.section-heading { display: grid; align-content: start; gap: 12px; }
.section-heading h2 { margin: 0; font-size: clamp(2rem, 4.4vw, 3.5rem); font-weight: 570; line-height: 1.16; letter-spacing: -.055em; text-wrap: balance; }
.centered-heading { justify-items: center; margin-bottom: 26px; text-align: center; }
.scene-controls { display: flex; justify-content: center; margin-bottom: 25px; }
.scene-controls :deep(.ant-segmented) { padding: 5px; background: var(--workspace-raised); border: 1px solid var(--workspace-line); border-radius: 999px; }
.scene-controls :deep(.ant-segmented-item), .scene-controls :deep(.ant-segmented-thumb) { border-radius: 999px; }
.scene-controls :deep(.ant-segmented-item-label) { min-width: 57px; line-height: 32px; }
.scene-controls :deep(.ant-segmented-item-selected) { color: var(--workspace-accent); }

.scene-card { display: grid; grid-template-columns: minmax(265px, .44fr) minmax(0, 1fr); overflow: hidden; background: var(--workspace-surface); border: 1px solid var(--workspace-line); border-radius: 22px; }
.scene-copy { display: grid; align-content: center; gap: 14px; padding: clamp(24px, 4vw, 52px); }
.scene-copy h3 { margin: 0; font-size: clamp(1.45rem, 2.5vw, 2rem); line-height: 1.35; font-weight: 570; letter-spacing: -.025em; text-wrap: balance; }
.scene-copy p, .value-card p, .guide-grid p { margin: 0; color: var(--workspace-muted); font-size: 15px; line-height: 1.75; }
.scene-media { display: grid; align-items: center; min-width: 0; min-height: 460px; padding: clamp(18px, 5vw, 72px); background: linear-gradient(145deg, var(--workspace-halo-cool), var(--workspace-raised)); }
.scene-media > * { width: min(100%, 605px); margin-inline: auto; }
.integrations { padding-top: 10px; }
.integration-list { display: flex; justify-content: center; flex-wrap: wrap; gap: 9px 12px; padding: 0; margin: 0; list-style: none; }
.integration-list li { display: flex; align-items: center; gap: 8px; padding: 9px 15px; color: var(--workspace-muted); background: var(--workspace-raised); border: 1px solid var(--workspace-line); border-radius: 999px; font-size: 14px; }
.integration-list .anticon { color: var(--workspace-accent); }

.value-section {
  display: grid;
  gap: 32px;
  padding: clamp(22px, 5.9vw, 78px);
  background:
    linear-gradient(to right, color-mix(in srgb, var(--workspace-line) 35%, transparent) 1px, transparent 1px),
    linear-gradient(to bottom, color-mix(in srgb, var(--workspace-line) 35%, transparent) 1px, transparent 1px),
    var(--workspace-raised);
  background-size: 32px 32px;
  border: 1px solid var(--workspace-line);
  border-radius: 22px;
}

.value-heading { grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr); align-items: center; gap: 14px 30px; }
.feature-grid, .guide-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: clamp(10px, 1.5vw, 17px); }
.value-card, .guide-grid li { display: grid; align-content: start; gap: 10px; padding: clamp(16px, 2.7vw, 31px); background: var(--workspace-surface); border: 1px solid var(--workspace-line); border-radius: 16px; }
.value-card > .anticon, .guide-grid li > .anticon { justify-self: start; margin-bottom: 7px; color: var(--workspace-accent); font-size: 25px; }
.value-card h3, .guide-grid h3 { margin: 0; font-size: 18px; font-weight: 620; }
.guide-section { display: grid; justify-items: center; }
.guide-grid { width: min(100%, 910px); padding: 0; margin: 0 0 27px; list-style: none; }

.site-footer { padding-block: 32px; border-top: 1px solid var(--workspace-line); }
.footer-layout { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(82px, .55fr) minmax(0, 1fr); gap: 20px 32px; }
.site-footer p { margin: 8px 0 0; color: var(--workspace-muted); font-size: 13px; line-height: 1.65; }
.site-footer nav { display: flex; flex-wrap: wrap; align-content: start; gap: 6px 13px; }
.site-footer nav a { color: var(--workspace-muted); font-size: 13px; text-decoration: none; }
.site-footer nav a:hover { color: var(--workspace-accent); }
.footer-contact { display: flex; align-items: start; gap: 12px; }
.footer-contact img { flex: none; border: 1px solid var(--workspace-line); border-radius: var(--workspace-control-radius); }

.login-dialog-title { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
:global(.site-login-layer .ant-modal-content) { padding: 0; }
:global(.site-login-layer .ant-modal-header) { margin: 0; }

@media (prefers-reduced-motion: no-preference) {
  .hero-copy { animation: landing-enter .45s ease-out both; }
  .hero-preview { animation: landing-enter .6s .08s ease-out both; }
  .scene-media > * { animation: landing-enter .26s ease-out both; }
  .pill-button { transition: background-color .18s ease, transform .18s ease; }

  @keyframes landing-enter {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
  }
}

@media (hover: hover) and (prefers-reduced-motion: no-preference) {
  .pill-button:hover { transform: translateY(-2px); }
}

@media (max-width: 940px) {
  .site-header-inner { flex-wrap: wrap; gap: 3px 12px; }
  .site-nav { order: 1; width: 100%; justify-content: flex-start; }
  .landing-page { --site-header-height: 100px; }
  .scene-media { min-height: 360px; }
}

@media (max-width: 700px) {
  .scene-card, .value-heading, .footer-layout { grid-template-columns: 1fr; }
  .scene-media { min-height: 310px; }
  .footer-layout { gap: 18px; }
}

@media (max-width: 550px) {
  .header-start { display: none; }
  .site-nav { gap: 0 14px; }
  .site-nav a { font-size: 13px; }
  .hero-copy { gap: 15px; }
  .hero-copy p { font-size: 15px; }
  .value-section { gap: 20px; }
  .feature-grid, .guide-grid { grid-template-columns: 1fr; }
  .guide-grid li { grid-template-columns: auto 1fr; gap: 5px 12px; }
  .guide-grid li > .anticon { align-self: start; grid-row: span 2; }
  .scene-controls :deep(.ant-segmented-item-label) { min-width: 45px; padding-inline: 8px; }
}

@media (max-width: 360px) {
  .scene-controls :deep(.ant-segmented-group) { flex-wrap: wrap; justify-content: center; }
}
</style>
