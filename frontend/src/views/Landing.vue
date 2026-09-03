<template>
  <main class="landing-page">
    <header class="site-header">
      <a class="brand" href="#top" aria-label="Metrix home">
        <img src="/Metrix-logo.png" alt="Metrix" />
        <span>Metrix</span>
      </a>
      <nav class="site-nav" :aria-label="$t('landing.navigation')">
        <a href="#features">{{ $t('landing.navigationFeatures') }}</a>
        <a href="#advantages">{{ $t('landing.navigationAdvantages') }}</a>
        <a href="#guide">{{ $t('landing.navigationGuide') }}</a>
      </nav>
      <div class="header-actions">
        <button class="locale-button" type="button" @click="switchLanguage">
          <GlobalOutlined />
          {{ locale === 'zh-CN' ? 'EN' : '中文' }}
        </button>
        <a class="text-link" href="/login">{{ $t('landing.login') }}</a>
        <a class="primary-button compact" href="#guide">{{ $t('landing.startNow') }} <ArrowRightOutlined /></a>
      </div>
    </header>

    <section id="top" class="hero section-wrap">
      <div class="hero-copy">
        <p class="eyebrow"><span></span>{{ $t('landing.eyebrow') }}</p>
        <h1>{{ $t('landing.titleStart') }}<em>{{ $t('landing.titleEmphasis') }}</em>{{ $t('landing.titleEnd') }}</h1>
        <p class="hero-description">{{ $t('landing.description') }}</p>
        <div class="hero-actions">
          <a class="primary-button" href="#guide">{{ $t('landing.startNow') }} <ArrowRightOutlined /></a>
          <a class="secondary-button" href="#features">{{ $t('landing.exploreFeatures') }} <DownOutlined /></a>
        </div>
        <p class="trust-line"><SafetyCertificateOutlined /> {{ $t('landing.trustLine') }}</p>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <div class="orb orb-one"></div><div class="orb orb-two"></div>
        <div class="workspace-card">
          <div class="panel-top"><span></span><span></span><span></span><strong>METRIX / RESEARCH DESK</strong></div>
          <div class="market-row"><div><small>MARKET PULSE</small><b>+ 2.64%</b><p>Risk appetite is rising</p></div><div class="pulse-ring">72<small>/100</small></div></div>
          <div class="chart-area"><div class="chart-label"><span>Market breadth</span><strong>Positive</strong></div><svg viewBox="0 0 390 116" preserveAspectRatio="none"><defs><linearGradient id="fill" x1="0" x2="0" y1="0" y2="1"><stop stop-color="#ff8a61" stop-opacity=".45"/><stop offset="1" stop-color="#ff8a61" stop-opacity="0"/></linearGradient></defs><path d="M0,99 C22,95 26,68 48,77 S80,94 98,64 S133,72 149,43 S178,61 196,38 S224,59 242,30 S270,42 291,25 S329,39 349,13 S378,20 390,8 L390,116 L0,116Z" fill="url(#fill)"/><path d="M0,99 C22,95 26,68 48,77 S80,94 98,64 S133,72 149,43 S178,61 196,38 S224,59 242,30 S270,42 291,25 S329,39 349,13 S378,20 390,8" fill="none" stroke="#ff7750" stroke-width="3"/></svg></div>
          <div class="insight"><BulbOutlined /><div><small>AI INSIGHT</small><p>Focus on flow, breadth and confirmation.</p></div><ArrowUpOutlined /></div>
        </div>
        <div class="float-card research-card"><FileSearchOutlined /><div><small>RESEARCH</small><b>NVDA · Watchlist</b></div><CheckCircleFilled /></div>
        <div class="float-card signal-card"><RadarChartOutlined /><div><small>SIGNAL</small><b>{{ $t('landing.signal') }}</b></div></div>
      </div>
    </section>

    <section id="features" class="feature-section">
      <div class="section-wrap"><p class="eyebrow"><span></span>{{ $t('landing.featuresEyebrow') }}</p><div class="section-heading"><h2>{{ $t('landing.featuresTitle') }}</h2><p>{{ $t('landing.featuresDescription') }}</p></div>
      <div class="feature-grid"><article v-for="item in featureItems" :key="item.title" class="feature-card"><component :is="item.icon" /><h3>{{ item.title }}</h3><p>{{ item.description }}</p><span>{{ item.note }}</span></article></div></div>
    </section>

    <section id="advantages" class="advantage-section section-wrap">
      <div class="advantage-copy"><p class="eyebrow light"><span></span>{{ $t('landing.advantagesEyebrow') }}</p><h2>{{ $t('landing.advantagesTitle') }}</h2><p>{{ $t('landing.advantagesDescription') }}</p><div class="advantage-list"><div v-for="item in advantageItems" :key="item.title"><CheckCircleFilled /><section><h3>{{ item.title }}</h3><p>{{ item.description }}</p></section></div></div></div>
      <div class="advantage-metric"><p>{{ $t('landing.metricLabel') }}</p><b>01 <span>/ 03</span></b><div class="metric-line"><i></i></div><h3>{{ $t('landing.metricTitle') }}</h3><p>{{ $t('landing.metricDescription') }}</p></div>
    </section>

    <section id="guide" class="guide-section"><div class="section-wrap"><p class="eyebrow"><span></span>{{ $t('landing.guideEyebrow') }}</p><div class="section-heading guide-heading"><h2>{{ $t('landing.guideTitle') }}</h2><a href="/login">{{ $t('landing.goToLogin') }} <ArrowRightOutlined /></a></div>
      <div class="guide-grid"><article v-for="(item, index) in guideItems" :key="item.title" class="guide-card"><span class="step-number">0{{ index + 1 }}</span><component :is="item.icon" /><h3>{{ item.title }}</h3><p>{{ item.description }}</p></article></div></div>
    </section>

    <section class="cta section-wrap"><div><p class="eyebrow"><span></span>METRIX</p><h2>{{ $t('landing.ctaTitle') }}</h2><p>{{ $t('landing.ctaDescription') }}</p></div><a class="primary-button light-button" href="/login">{{ $t('landing.loginToWorkspace') }} <ArrowRightOutlined /></a></section>
    <footer><a class="brand" href="#top"><img src="/Metrix-logo.png" alt="" /><span>Metrix</span></a><div class="footer-copy"><p>{{ $t('landing.footer') }}</p><p class="footer-disclaimer">{{ $t('landing.disclaimer') }}</p></div><section class="footer-contact"><img class="footer-qr" src="/Mertix-lab-wx.jpg" :alt="$t('landing.wechatQrAlt')" /><div class="footer-contact-copy"><strong>{{ $t('landing.contactTitle') }}</strong><p>{{ $t('landing.contactDescription') }}</p></div></section><button type="button" @click="switchLanguage">{{ locale === 'zh-CN' ? 'English' : '中文' }}</button></footer>
  </main>
</template>

<script setup>
import {computed} from 'vue'
import {useI18n} from 'vue-i18n'
import {ArrowRightOutlined, ArrowUpOutlined, BulbOutlined, CheckCircleFilled, DownOutlined, FileSearchOutlined, FundOutlined, GlobalOutlined, RadarChartOutlined, SafetyCertificateOutlined, StockOutlined} from '@ant-design/icons-vue'

const {locale, t} = useI18n()
const switchLanguage = () => {
  locale.value = locale.value === 'zh-CN' ? 'en' : 'zh-CN'
  localStorage.setItem('locale', locale.value)
}
const featureItems = computed(() => [
  {icon: FileSearchOutlined, title: t('landing.features.research.title'), description: t('landing.features.research.description'), note: t('landing.features.research.note')},
  {icon: FundOutlined, title: t('landing.features.review.title'), description: t('landing.features.review.description'), note: t('landing.features.review.note')},
  {icon: StockOutlined, title: t('landing.features.portfolio.title'), description: t('landing.features.portfolio.description'), note: t('landing.features.portfolio.note')},
])
const advantageItems = computed(() => [
  {title: t('landing.advantages.focus.title'), description: t('landing.advantages.focus.description')},
  {title: t('landing.advantages.traceable.title'), description: t('landing.advantages.traceable.description')},
  {title: t('landing.advantages.yours.title'), description: t('landing.advantages.yours.description')},
])
const guideItems = computed(() => [
  {icon: GlobalOutlined, title: t('landing.guide.login.title'), description: t('landing.guide.login.description')},
  {icon: RadarChartOutlined, title: t('landing.guide.configure.title'), description: t('landing.guide.configure.description')},
  {icon: BulbOutlined, title: t('landing.guide.research.title'), description: t('landing.guide.research.description')},
])
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Manrope:wght@400;500;600;700;800&family=Noto+Sans+SC:wght@400;500;600;700;800&display=swap');
.landing-page { --ink:#162239; --muted:#6d7685; --coral:#f2684e; --cream:#fffaf4; color:var(--ink); background:#fffdf9; font-family:Manrope,'Noto Sans SC',sans-serif; overflow:hidden; }
.footer-copy{flex:1}.footer-copy .footer-disclaimer{max-width:760px;margin-top:5px;font-size:10px;line-height:1.55;color:#adb2ba}
.section-wrap,.site-header,footer { max-width:1200px; margin:0 auto; padding-left:32px; padding-right:32px; }.site-header { height:82px; display:flex; align-items:center; justify-content:space-between; gap:28px; }.brand{display:inline-flex;align-items:center;gap:10px;color:var(--ink);font-weight:800;text-decoration:none;font-size:19px;letter-spacing:-.5px}.brand img{width:32px;height:32px;object-fit:contain}.site-nav{display:flex;gap:30px;margin-left:auto}.site-nav a,.text-link{font-size:14px;color:#4f5969;text-decoration:none;font-weight:600}.site-nav a:hover,.text-link:hover{color:var(--coral)}.header-actions{display:flex;align-items:center;gap:18px}.locale-button,footer button{border:0;background:transparent;color:var(--ink);font:600 13px inherit;cursor:pointer;display:flex;gap:6px;align-items:center}.primary-button,.secondary-button{border-radius:999px;padding:14px 21px;display:inline-flex;align-items:center;gap:11px;text-decoration:none;font-size:14px;font-weight:700;transition:.2s}.primary-button{background:var(--ink);color:#fff;box-shadow:0 10px 20px rgba(22,34,57,.14)}.primary-button:hover{background:var(--coral);transform:translateY(-2px)}.primary-button.compact{padding:10px 15px;font-size:13px}.secondary-button{color:var(--ink);border:1px solid #dbe0e7}.secondary-button:hover{background:#f5f0e9}.hero{display:grid;grid-template-columns:1fr 1fr;align-items:center;min-height:620px;gap:42px;padding-top:74px;padding-bottom:86px}.eyebrow{font:500 11px 'DM Mono',monospace;letter-spacing:1.7px;color:var(--coral);display:flex;align-items:center;gap:8px;text-transform:uppercase}.eyebrow span{width:27px;height:1px;background:currentColor}.hero h1{font-size:clamp(42px,5vw,67px);letter-spacing:-3px;line-height:1.08;max-width:660px;margin:18px 0 23px;font-weight:800}.hero h1 em{font-style:normal;color:var(--coral);display:block}.hero-description{color:var(--muted);max-width:540px;font-size:17px;line-height:1.75}.hero-actions{display:flex;gap:12px;margin-top:32px}.trust-line{font-size:12px;color:#7d8591;margin-top:22px;display:flex;align-items:center;gap:8px}.trust-line :deep(svg){color:#5eaf85}.hero-visual{min-height:405px;position:relative}.workspace-card{position:absolute;right:1%;top:12px;width:min(100%,475px);border-radius:20px;background:#19283f;padding:18px;box-shadow:0 26px 70px rgba(18,29,47,.22);color:#fff;transform:rotate(2deg);z-index:2}.panel-top{font:10px 'DM Mono',monospace;color:#8796ad;display:flex;align-items:center;gap:5px}.panel-top span{width:7px;height:7px;border-radius:50%;background:#f26d55}.panel-top span:nth-child(2){background:#f4c95d}.panel-top span:nth-child(3){background:#76bb91}.panel-top strong{margin-left:auto;font-weight:400;font-size:9px}.market-row{display:flex;justify-content:space-between;align-items:center;padding:25px 6px 15px}.market-row small,.insight small{font:10px 'DM Mono',monospace;color:#9eacbf;letter-spacing:.8px}.market-row b{font-size:30px;display:block;letter-spacing:-1px;margin:3px 0;color:#ff9674}.market-row p{font-size:11px;color:#bdc7d4}.pulse-ring{width:69px;height:69px;border-radius:50%;border:6px solid #ed765d;border-left-color:#60728a;display:grid;place-content:center;font-size:20px;font-weight:800}.pulse-ring small{font-size:8px;text-align:center;color:#9eacbf}.chart-area{border-radius:12px;background:#22334c;padding:13px 15px 5px}.chart-label{display:flex;justify-content:space-between;color:#aeb9c8;font-size:11px}.chart-label strong{font-weight:700;color:#9bd8af}.chart-area svg{width:100%;height:110px}.insight{margin-top:13px;padding:13px;border-radius:12px;background:#263952;display:flex;gap:10px;align-items:center}.insight :deep(svg){color:#f8c45c;font-size:17px}.insight div{flex:1}.insight p{font-size:12px;margin-top:3px;color:#d7dfeb}.insight > :last-child{color:#81c89c}.float-card{position:absolute;z-index:3;background:#fff;border:1px solid #e7e9ee;border-radius:12px;box-shadow:0 18px 35px rgba(24,37,58,.12);padding:13px;display:flex;gap:10px;align-items:center}.float-card > :first-child{font-size:19px;color:var(--coral)}.float-card small{display:block;font:9px 'DM Mono',monospace;color:#a2a8b3;letter-spacing:.5px}.float-card b{display:block;font-size:11px;margin-top:3px}.research-card{bottom:0;left:0}.research-card > :last-child{color:#58a578;font-size:14px}.signal-card{right:0;bottom:45px}.orb{position:absolute;border-radius:50%;filter:blur(1px)}.orb-one{width:260px;height:260px;background:#fde3ce;right:-40px;top:-20px}.orb-two{width:180px;height:180px;background:#dce9ea;left:40px;bottom:10px}.feature-section{background:var(--cream);padding:95px 0}.section-heading{display:flex;justify-content:space-between;align-items:flex-end;margin:15px 0 36px;gap:30px}.section-heading h2,.advantage-copy h2,.cta h2{font-size:clamp(30px,3.4vw,46px);letter-spacing:-2px;line-height:1.16}.section-heading p{max-width:410px;color:var(--muted);line-height:1.7;font-size:14px}.feature-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:17px}.feature-card{min-height:255px;background:#fff;padding:28px;border:1px solid #f0ebe5;border-radius:15px;transition:.22s}.feature-card:hover{transform:translateY(-5px);box-shadow:0 18px 40px rgba(36,44,56,.08)}.feature-card :deep(svg){font-size:26px;color:var(--coral);padding:12px;background:#fff0e9;border-radius:10px}.feature-card h3{font-size:18px;margin:28px 0 10px}.feature-card p{font-size:13px;line-height:1.7;color:var(--muted);min-height:65px}.feature-card span{font:10px 'DM Mono',monospace;color:#a2a6ae;letter-spacing:.5px}.advantage-section{margin:74px auto;border-radius:20px;background:#18263c;color:#fff;display:grid;grid-template-columns:1.5fr .8fr;gap:60px;padding-top:62px;padding-bottom:62px}.eyebrow.light{color:#ff9075}.advantage-copy>p:not(.eyebrow){color:#b5c0cf;line-height:1.75;margin:18px 0 30px;max-width:590px}.advantage-list{display:grid;gap:17px}.advantage-list>div{display:flex;gap:13px}.advantage-list :deep(svg){font-size:15px;color:#f78c71;margin-top:3px}.advantage-list h3{font-size:14px}.advantage-list p{font-size:12px;color:#a8b4c4;margin-top:5px;line-height:1.55}.advantage-metric{border-left:1px solid #46556a;padding:8px 0 8px 34px;align-self:center}.advantage-metric>p{font:10px 'DM Mono',monospace;letter-spacing:1px;color:#98a7ba}.advantage-metric b{font-size:45px;display:block;margin:12px 0;color:#ff9277}.advantage-metric b span{font-size:13px;color:#8190a3}.metric-line{height:2px;background:#44556b;margin:15px 0 23px}.metric-line i{display:block;width:33%;height:100%;background:#ff9277}.advantage-metric h3{font-size:16px}.advantage-metric p:last-child{font:12px/1.7 Manrope,'Noto Sans SC',sans-serif;margin-top:7px;color:#aab6c6}.guide-section{padding:72px 0 95px}.guide-heading a{color:var(--coral);font-size:13px;text-decoration:none;font-weight:700;white-space:nowrap}.guide-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:45px;counter-reset:steps}.guide-card{position:relative;border-top:1px solid #dfe3e8;padding-top:26px}.step-number{position:absolute;right:0;top:17px;font:11px 'DM Mono',monospace;color:#bcc2ca}.guide-card :deep(svg){font-size:25px;color:#293953}.guide-card h3{font-size:17px;margin:20px 0 8px}.guide-card p{font-size:13px;line-height:1.7;color:var(--muted);max-width:280px}.cta{margin-bottom:56px;padding-top:55px;padding-bottom:55px;background:#f16d53;border-radius:19px;color:#fff;display:flex;align-items:center;justify-content:space-between;gap:35px}.cta .eyebrow{color:#ffe2d8}.cta h2{margin:13px 0 10px}.cta>div>p:last-child{font-size:14px;color:#fff0eb}.light-button{background:#fff;color:#db5a42;white-space:nowrap}.light-button:hover{background:#18263c;color:#fff}footer{padding-top:10px;padding-bottom:35px;display:flex;align-items:center;gap:25px;border-top:1px solid #ece6df}footer .brand{font-size:16px}footer .brand img{width:25px;height:25px}footer p{font-size:12px;color:#9298a1;flex:1}footer button{color:#657084}@media(max-width:820px){.site-header{height:70px;padding-left:20px;padding-right:20px}.site-nav,.header-actions .text-link,.header-actions .compact{display:none}.header-actions{margin-left:auto}.section-wrap,footer{padding-left:20px;padding-right:20px}.hero{grid-template-columns:1fr;padding-top:55px;gap:30px}.hero-visual{width:min(100%,510px);justify-self:center;min-height:375px}.feature-grid,.guide-grid{grid-template-columns:1fr}.feature-card{min-height:0}.advantage-section{margin:38px 20px;grid-template-columns:1fr;gap:35px;padding:40px 26px}.advantage-metric{border-left:0;border-top:1px solid #46556a;padding:25px 0 0}.section-heading{display:block}.section-heading p{margin-top:14px}.guide-grid{gap:25px}.guide-card{padding-top:18px}.cta{margin-left:20px;margin-right:20px;padding:35px 26px;display:block}.cta .light-button{margin-top:24px}footer{margin:0 20px;padding-left:0;padding-right:0}.hero h1{letter-spacing:-2px}.workspace-card{right:0}.signal-card{right:0}}
.footer-contact{display:flex;align-items:center;gap:12px;padding-left:20px;border-left:1px solid #ece6df}.footer-qr{width:66px;height:66px;object-fit:cover;border:1px solid #ece6df;border-radius:7px}.footer-contact-copy{display:grid;gap:4px;min-width:154px}.footer-contact-copy strong{font-size:12px;color:var(--ink)}.footer-contact-copy p{margin:0;font-size:11px;line-height:1.55;color:#9298a1}.footer-contact-copy p{flex:initial}@media(max-width:820px){footer{flex-wrap:wrap}.footer-contact{width:100%;padding-left:0;border-left:0}.footer-contact-copy{min-width:0}}
</style>
