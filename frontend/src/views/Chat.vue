<template>
  <section class="agent-chat" :class="{ 'sidebar-open': sidebarOpen }">
    <button v-if="sidebarOpen" class="sidebar-backdrop" :aria-label="t('chat.closeHistory')" @click="sidebarOpen = false"></button>
    <aside class="chat-sidebar" :aria-label="t('chat.history')">
      <div class="sidebar-brand"><span class="agent-mark"><RiseOutlined /></span><strong>{{ t('chat.title') }}</strong><span class="agent-badge">{{ t('chat.agentBadge') }}</span></div>
      <a-button block class="new-chat" :disabled="busy" @click="newChat"><PlusOutlined />{{ t('chat.newSession') }}</a-button>
      <div class="history-heading">{{ t('chat.history') }}</div>
      <div v-if="sessions.length" class="session-toolbar">
        <a-checkbox :checked="allSelected" :indeterminate="selectedIds.size > 0 && !allSelected" :disabled="busy" @change="toggleAll">{{ t('chat.selectAll') }}</a-checkbox>
        <a-button v-if="selectedIds.size" size="small" type="text" danger :disabled="busy" @click="deleteSelected">{{ t('chat.batchDelete') }}</a-button>
      </div>
      <nav class="session-list">
        <div v-for="session in sessions" :key="session.id" class="session-row" :class="{ active: currentSessionId === session.id }">
          <a-checkbox :checked="selectedIds.has(session.id)" :disabled="busy" :aria-label="t('chat.selectSession', { name: session.sessionName })" @change="toggleSelected(session.id)" />
          <button class="session-link" :disabled="busy" :title="session.sessionName" @click="switchSession(session.id)"><MessageOutlined /><span>{{ session.sessionName }}</span></button>
          <a-button size="small" type="text" :disabled="busy" :aria-label="t('chat.deleteConfirm')" @click="deleteOne(session.id)"><DeleteOutlined /></a-button>
        </div>
        <div v-if="!sessions.length" class="history-empty">{{ t('chat.noSession') }}</div>
      </nav>
      <div class="sidebar-scope"><SafetyCertificateOutlined /><span>{{ t('chat.scopeDetail') }}</span></div>
    </aside>

    <main class="conversation">
      <header class="conversation-header">
        <div class="header-left"><a-button type="text" class="history-toggle" :aria-label="t('chat.history')" @click="sidebarOpen = !sidebarOpen"><MenuOutlined /></a-button><span class="scope-pill"><span></span>{{ t('chat.scope') }}</span></div>
        <span class="state-label" role="status" aria-live="polite"><LoadingOutlined v-if="busy" spin />{{ t(`chat.states.${currentState}`) }}</span>
      </header>
      <div ref="messageListRef" class="message-list" @scroll="trackScroll">
        <div v-if="!messages.length" class="welcome">
          <span class="welcome-mark"><RiseOutlined /></span>
          <h1>{{ t('chat.welcome') }}</h1>
          <p>{{ t('chat.welcomeDetail') }}</p>
          <div class="suggestions"><button v-for="key in exampleKeys" :key="key" :disabled="busy" @click="inputText = t(`chat.examples.${key}`)"><ArrowUpOutlined />{{ t(`chat.examples.${key}`) }}</button></div>
        </div>
        <div v-else ref="messageColumnRef" class="message-column">
          <article v-for="(msg, index) in messages" :key="msg.id" class="message" :class="msg.role">
            <div v-if="msg.role === 'user'" class="user-bubble">{{ msg.content }}</div>
            <template v-else>
              <div class="assistant-heading"><span class="assistant-mark"><RiseOutlined /></span><strong>{{ t('chat.agentName') }}</strong></div>
              <details v-if="msg.steps.length || msg.streaming" class="thinking-card" open>
                <summary><span><LoadingOutlined v-if="msg.streaming" spin /><CheckCircleOutlined v-else />{{ t('chat.thinking') }}</span><DownOutlined /></summary>
                <p class="thinking-hint">{{ t('chat.publicSteps') }}</p>
                <ol class="thinking-steps"><li v-for="(step, stepIndex) in msg.steps" :key="stepIndex" :class="{ failed: step.status === 'failed' }"><span class="step-dot"></span><span>{{ step.content || step.title }}</span><small v-if="step.elapsed != null">{{ (step.elapsed / 1000).toFixed(1) }}s</small></li></ol>
              </details>
              <div v-if="msg.content" class="answer-card" :class="msg.status">
                <div class="answer-heading">{{ t('chat.report') }}</div>
                <div class="answer-body"><MarkdownRender :is-dark="isDark" :content="msg.content" :final="!msg.streaming" /></div>
              </div>
              <div v-if="msg.error" class="message-error" role="alert">{{ msg.error }}</div>
              <div v-if="!msg.streaming" class="answer-actions">
                <span class="answer-state">{{ t(`chat.states.${msg.status || 'completed'}`) }}</span>
                <a-button v-if="msg.content" type="text" size="small" @click="copyAnswer(msg)"><CopyOutlined />{{ t('chat.copy') }}</a-button>
                <a-button v-if="index === messages.length - 1" type="text" size="small" :disabled="busy" @click="regenerate(index)"><ReloadOutlined />{{ t('chat.regenerate') }}</a-button>
              </div>
            </template>
          </article>
        </div>
      </div>
      <div class="composer-area">
        <a-button v-if="!followOutput && messages.length" class="jump-bottom" shape="circle" :aria-label="t('chat.jumpBottom')" @click="jumpBottom"><ArrowDownOutlined /></a-button>
        <div class="composer">
          <a-textarea v-model:value="inputText" :auto-size="{ minRows: 2, maxRows: 6 }" :maxlength="2000" :placeholder="t('chat.inputPlaceholder')" :aria-label="t('chat.inputPlaceholder')" @keydown="handleKeydown" />
          <div class="composer-actions"><span class="research-chip"><NodeIndexOutlined />{{ t('chat.research') }}</span><span class="keyboard-hint">{{ t('chat.enterHint') }}</span><a-button v-if="busy" class="stop-button" :loading="stopping" @click="stopGeneration"><PauseOutlined />{{ t('chat.stop') }}</a-button><a-button v-else type="primary" shape="circle" :disabled="!inputText.trim()" :aria-label="t('chat.send')" @click="send"><ArrowUpOutlined /></a-button></div>
        </div>
        <p class="disclaimer">{{ t('chat.disclaimer') }}</p>
      </div>
    </main>
  </section>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {message, Modal} from 'ant-design-vue'
import {ArrowDownOutlined, ArrowUpOutlined, CheckCircleOutlined, CopyOutlined, DeleteOutlined, DownOutlined, LoadingOutlined, MenuOutlined, MessageOutlined, NodeIndexOutlined, PauseOutlined, PlusOutlined, ReloadOutlined, RiseOutlined, SafetyCertificateOutlined} from '@ant-design/icons-vue'
import MarkdownRender from 'markstream-vue'
import {createChatSession, deleteChatSession, deleteChatSessions, getChatSessionMessages, listChatSessions, sendChatMessage, stopChatGeneration} from '../api/index.js'
import {restoreAgentMessage} from '../composables/agentStream.js'
import {useTheme} from '../composables/useTheme'

const {t} = useI18n()
const {isDark} = useTheme()
const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const inputText = ref('')
const busy = ref(false)
const stopping = ref(false)
const sidebarOpen = ref(false)
const selectedIds = ref(new Set())
const messageListRef = ref(null)
const messageColumnRef = ref(null)
const followOutput = ref(true)
const exampleKeys = ['stock', 'compare', 'index']
const allSelected = computed(() => sessions.value.length > 0 && selectedIds.value.size === sessions.value.length)
const currentState = computed(() => messages.value.at(-1)?.status || (busy.value ? 'identifying' : 'idle'))
let controller
let loadVersion = 0
let disposed = false
let resizeObserver

watch(messageColumnRef, element => {
  resizeObserver?.disconnect()
  if (!element || typeof ResizeObserver === 'undefined') return
  resizeObserver = new ResizeObserver(scrollToBottom)
  resizeObserver.observe(element)
}, {flush: 'post'})

async function loadSessions() {
  try { sessions.value = (await listChatSessions()).data || [] } catch { /* The shared API interceptor displays the error. */ }
}

function newChat() {
  if (busy.value) return
  loadVersion++
  currentSessionId.value = null
  messages.value = []
  inputText.value = ''
  followOutput.value = true
  sidebarOpen.value = false
}

async function switchSession(id) {
  if (busy.value) return
  const version = ++loadVersion
  try {
    const response = await getChatSessionMessages(id)
    if (version !== loadVersion || busy.value) return
    currentSessionId.value = id
    messages.value = (response.data || []).map(restoreAgentMessage)
    sidebarOpen.value = false
    jumpBottom()
  } catch { /* The shared API interceptor displays the error. */ }
}

function toggleSelected(id) {
  const selection = new Set(selectedIds.value)
  selection.has(id) ? selection.delete(id) : selection.add(id)
  selectedIds.value = selection
}
function toggleAll() { selectedIds.value = allSelected.value ? new Set() : new Set(sessions.value.map(session => session.id)) }

function confirmDelete(ids, action, title) {
  Modal.confirm({ title, content: t('chat.batchDeleteDesc', {count: ids.length}), async onOk() {
    await action()
    if (ids.includes(currentSessionId.value)) newChat()
    selectedIds.value = new Set()
    await loadSessions()
  } })
}
function deleteOne(id) { confirmDelete([id], () => deleteChatSession(id), t('chat.deleteConfirm')) }
function deleteSelected() {
  const ids = [...selectedIds.value]
  confirmDelete(ids, () => deleteChatSessions(ids), t('chat.batchDeleteConfirm'))
}

function handleKeydown(event) {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing || event.keyCode === 229) return
  event.preventDefault()
  if (!busy.value) send()
}

async function send(override, regenerate = false) {
  const text = typeof override === 'string' ? override : inputText.value.trim()
  if (!text || busy.value) return
  busy.value = true
  loadVersion++
  let assistant
  try {
    if (!currentSessionId.value) {
      const response = await createChatSession({sessionName: t('chat.sessionName')})
      currentSessionId.value = response.data.id
    }
    if (disposed) return
    inputText.value = ''
    followOutput.value = true
    if (regenerate) messages.value.pop()
    else messages.value.push({id: `user-${Date.now()}`, role: 'user', content: text})
    messages.value.push({id: `agent-${Date.now()}`, role: 'assistant', content: '', steps: [], status: 'identifying', streaming: true})
    assistant = messages.value.at(-1)
    scrollToBottom()
    controller = new AbortController()
    await sendChatMessage(currentSessionId.value, text, {
      onThinking(step) { assistant.steps.push(step); assistant.status = step.state || 'analyzing'; scrollToBottom() },
      onFinal(data) { assistant.content = data.delta ? assistant.content + data.content : data.content; assistant.status = data.status || 'generating'; scrollToBottom() },
      onDone(data) { assistant.streaming = false; assistant.status = data.status || 'completed'; assistant.steps = (data.steps || assistant.steps).filter(step => step.stage !== 'complete'); scrollToBottom() },
      onError(error) { assistant.status = 'error'; assistant.error = ['streamInterrupted', 'streamError'].includes(error.message) ? t(`chat.${error.message}`) : (error.message || t('chat.streamError')); assistant.streaming = false }
    }, controller.signal, regenerate)
    if (assistant.streaming) { assistant.streaming = false; assistant.status = controller.signal.aborted ? 'stopped' : 'error' }
    await loadSessions()
  } catch (error) {
    if (assistant) { assistant.streaming = false; assistant.status = 'error'; assistant.error = error.message || t('chat.streamError') }
  } finally { busy.value = false; stopping.value = false; controller = null }
}

async function stopGeneration() {
  if (!currentSessionId.value || stopping.value) return
  stopping.value = true
  try { await stopChatGeneration(currentSessionId.value); controller?.abort() }
  catch { stopping.value = false }
}

function regenerate(index) {
  const previous = messages.value.slice(0, index).findLast(msg => msg.role === 'user')
  if (previous) send(previous.content, true)
}
async function copyAnswer(msg) {
  try { await navigator.clipboard.writeText(msg.content); message.success(t('chat.copied')) }
  catch { message.error(t('chat.copyFailed')) }
}
function trackScroll() {
  const el = messageListRef.value
  if (el) followOutput.value = el.scrollHeight - el.scrollTop - el.clientHeight < 100
}
function scrollToBottom() {
  if (!followOutput.value) return
  nextTick(() => { const el = messageListRef.value; if (el) el.scrollTop = el.scrollHeight })
}
function jumpBottom() { followOutput.value = true; scrollToBottom() }

onMounted(loadSessions)
onBeforeUnmount(() => { disposed = true; resizeObserver?.disconnect(); loadVersion++; controller?.abort(); if (busy.value && currentSessionId.value) stopChatGeneration(currentSessionId.value).catch(() => {}) })
</script>

<style scoped>
.agent-chat { display: flex; height: calc(100dvh - 138px); min-height: 480px; overflow: hidden; color: var(--theme-text, #242933); background: var(--theme-surface, #fff); border: 1px solid var(--theme-line, #e7e9ee); border-radius: 16px; }
.chat-sidebar { width: 250px; flex-shrink: 0; display: flex; flex-direction: column; padding: 24px 14px 18px; background: var(--theme-raised, #f7f8fa); border-right: 1px solid var(--theme-line, #eceef2); }
.sidebar-brand { display: flex; align-items: center; gap: 9px; margin: 0 8px 26px; font-size: 17px; }
.agent-mark, .assistant-mark, .welcome-mark { display: inline-flex; align-items: center; justify-content: center; color: #4d6bfe; background: #edf1ff; border-radius: 12px; }
.agent-mark { width: 32px; height: 32px; }.agent-badge { font-size: 10px; color: #4d6bfe; border: 1px solid #d8e0ff; padding: 2px 5px; border-radius: 5px; }
.new-chat { height: 42px; border-radius: 10px; color: #4d6bfe; border-color: #dbe2ff; background: var(--theme-surface, #fff); box-shadow: none; }
.history-heading { margin: 26px 10px 12px; color: var(--theme-muted, #89909d); font-size: 12px; }.session-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 0 8px 8px; font-size: 12px; }
.session-list { flex: 1; overflow-y: auto; }.session-row { display: flex; align-items: center; gap: 7px; padding: 4px 8px; border-radius: 9px; margin: 2px 0; }.session-row:hover, .session-row.active { background: var(--theme-blue-surface, #eaf0ff); }
.session-link { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; padding: 9px 0; border: 0; background: transparent; color: inherit; text-align: left; cursor: pointer; font-size: 13px; }.session-link span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.session-link:disabled { opacity: .6; cursor: default; }.session-row :deep(.ant-btn) { color: var(--theme-muted, #89909d); }
.history-empty { padding: 18px 10px; color: var(--theme-muted, #89909d); font-size: 12px; line-height: 1.8; }.sidebar-scope { display: flex; align-items: center; gap: 8px; margin: 18px 8px 0; font-size: 11px; line-height: 1.6; color: var(--theme-muted, #89909d); }
.conversation { position: relative; display: flex; flex-direction: column; flex: 1; min-width: 0; }.conversation-header { display: flex; justify-content: space-between; align-items: center; min-height: 60px; padding: 0 28px; border-bottom: 1px solid var(--theme-line, #f0f1f5); }.header-left { display: flex; align-items: center; gap: 8px; }.scope-pill { display: inline-flex; align-items: center; gap: 7px; font-size: 12px; color: var(--theme-muted, #6b7485); }.scope-pill > span { width: 6px; height: 6px; border-radius: 50%; background: #4d6bfe; }.state-label { display: flex; gap: 7px; align-items: center; font-size: 12px; color: var(--theme-muted, #87909f); }.history-toggle { display: none; }
.message-list { flex: 1; min-height: 0; overflow-y: auto; padding: 26px 28px 20px; scroll-behavior: auto; }.message-column { max-width: 820px; margin: auto; }.welcome { max-width: 650px; margin: clamp(30px, 9vh, 110px) auto 30px; text-align: center; }.welcome-mark { width: 60px; height: 60px; font-size: 30px; border-radius: 20px; }.welcome h1 { margin: 22px 0 12px; font-size: clamp(22px, 2.5vw, 30px); font-weight: 650; color: inherit; }.welcome p { color: var(--theme-muted, #7b8494); font-size: 14px; line-height: 1.8; }.suggestions { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 32px; }.suggestions button { border: 1px solid var(--theme-line, #e4e8ef); background: var(--theme-surface, #fff); color: var(--theme-text, #5f6b7e); padding: 10px 13px; border-radius: 11px; cursor: pointer; font-size: 12px; }.suggestions button:hover { border-color: #4d6bfe; }.suggestions .anticon { margin-right: 7px; color: #4d6bfe; }
.message { margin-bottom: 28px; }.message.user { display: flex; justify-content: flex-end; }.user-bubble { max-width: 85%; padding: 12px 18px; border-radius: 18px; background: var(--theme-blue-surface, #edf2ff); font-size: 14px; line-height: 1.8; white-space: pre-wrap; overflow-wrap: anywhere; }.assistant-heading { display: flex; align-items: center; gap: 9px; font-size: 13px; margin-bottom: 15px; }.assistant-mark { width: 29px; height: 29px; border-radius: 9px; font-size: 16px; }
.thinking-card { margin: 0 0 18px 38px; border: 1px solid var(--theme-line, #e7e9ee); border-radius: 12px; background: var(--theme-raised, #f8f9fb); }.thinking-card summary { display: flex; justify-content: space-between; align-items: center; cursor: pointer; padding: 12px 16px; list-style: none; font-size: 13px; color: var(--theme-muted, #697487); }.thinking-card summary::-webkit-details-marker { display: none; }.thinking-card summary > span { display: flex; align-items: center; gap: 8px; }.thinking-card summary > .anticon { font-size: 10px; transition: transform .2s; }.thinking-card[open] summary > .anticon { transform: rotate(180deg); }.thinking-hint { margin: 0 16px 10px; color: var(--theme-muted, #969eab); font-size: 11px; }.thinking-steps { margin: 0; padding: 0 16px 14px; list-style: none; }.thinking-steps li { display: flex; gap: 10px; align-items: baseline; padding: 7px 0; font-size: 12px; line-height: 1.7; color: var(--theme-muted, #6e788a); overflow-wrap: anywhere; }.thinking-steps li > span:nth-child(2) { flex: 1; }.step-dot { flex-shrink: 0; width: 5px; height: 5px; border-radius: 50%; background: #a0b0d2; }.thinking-steps small { font-size: 10px; white-space: nowrap; }.thinking-steps li.failed { color: var(--theme-amber, #b17c29); }
.answer-card { margin-left: 38px; overflow-wrap: anywhere; }.answer-heading { margin-bottom: 12px; color: #4d6bfe; font-size: 11px; font-weight: 600; letter-spacing: .04em; }.answer-body { font-size: 14px; line-height: 1.9; overflow-x: auto; }.answer-body :deep(h2) { font-size: 18px; font-weight: 650; margin: 22px 0 12px; color: inherit; }.answer-body :deep(table) { display: block; overflow-x: auto; width: 100%; }.answer-body :deep(blockquote) { border-left: 3px solid #dce4ff; padding-left: 14px; color: var(--theme-muted, #8992a0); }.answer-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; margin: 14px 0 0 38px; color: var(--theme-muted, #8992a0); }.answer-actions :deep(.ant-btn) { color: inherit; font-size: 12px; }.answer-state { font-size: 11px; margin-right: 5px; }.message-error { margin: 12px 0 0 38px; color: var(--theme-red, #d34f54); font-size: 13px; }
.composer-area { position: relative; width: 100%; max-width: 876px; align-self: center; padding: 10px 28px 14px; }.composer { padding: 14px 16px 12px; border: 1px solid var(--theme-line, #e5e8ed); border-radius: 20px; background: var(--theme-raised, #f7f8fa); box-shadow: 0 3px 16px rgba(35, 50, 80, .025); }.composer:focus-within { border-color: #b8c7ff; }.composer :deep(textarea.ant-input) { padding: 0; border: 0; background: transparent; box-shadow: none; font-size: 14px; line-height: 1.8; color: inherit; resize: none; }.composer-actions { display: flex; align-items: center; gap: 10px; margin-top: 14px; }.research-chip { display: inline-flex; align-items: center; gap: 7px; border: 1px solid #d6e0ff; background: var(--theme-blue-surface, #edf2ff); color: #4d6bfe; padding: 5px 9px; border-radius: 8px; font-size: 11px; }.keyboard-hint { margin-left: auto; font-size: 10px; color: var(--theme-muted, #959dab); }.composer-actions :deep(.ant-btn-primary) { background: #4d6bfe; box-shadow: none; }.composer-actions :deep(.ant-btn-primary:disabled) { background: var(--theme-line, #dfe3eb); }.stop-button { border-radius: 9px; font-size: 12px; }.disclaimer { margin: 10px 0 0; text-align: center; font-size: 10px; line-height: 1.6; color: var(--theme-muted, #989fab); }.jump-bottom { position: absolute; top: -35px; left: calc(50% - 16px); box-shadow: 0 3px 12px #0000000f; }.sidebar-backdrop { display: none; }
@media (max-width: 900px) { .chat-sidebar { width: 210px; padding-left: 10px; padding-right: 10px; }.conversation-header { padding: 0 18px; }.message-list { padding: 24px 18px 12px; }.composer-area { padding: 10px 18px 12px; }.keyboard-hint { display: none; }.composer-actions :deep(.ant-btn) { margin-left: auto; } }
@media (max-width: 640px) { .agent-chat { height: calc(100dvh - 122px); min-height: 390px; border-radius: 10px; position: relative; }.chat-sidebar { visibility: hidden; position: absolute; z-index: 3; height: 100%; width: min(280px, 85%); transform: translateX(-100%); transition: transform .2s; }.sidebar-open .chat-sidebar { visibility: visible; transform: translateX(0); }.sidebar-backdrop { display: block; position: absolute; z-index: 2; inset: 0; width: 100%; border: 0; background: #00000040; }.history-toggle { display: inline-block; }.conversation-header { min-height: 52px; padding: 0 10px; }.scope-pill, .state-label { font-size: 11px; }.message-list { padding: 20px 14px 8px; }.welcome { margin-top: 32px; }.welcome p { font-size: 12px; }.suggestions { margin-top: 20px; }.thinking-card, .answer-card, .answer-actions, .message-error { margin-left: 0; }.composer-area { padding: 8px 10px 10px; }.composer { padding: 12px; border-radius: 16px; }.user-bubble { max-width: 90%; }.disclaimer { font-size: 9px; } }
@media (prefers-reduced-motion: reduce) { .chat-sidebar, .thinking-card summary > .anticon { transition: none; } }
</style>
