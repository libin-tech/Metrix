<template>
  <div class="chat-container">
    <a-modal v-model:visible="sessionLimitModal" title="提示" :footer="null">
      <p>{{ $t('chat.deleteConfirm') }}</p>
    </a-modal>
    <div class="chat-layout">
      <div class="session-panel">
        <a-button type="primary" block @click="createSession" class="new-session-btn">
          <PlusOutlined />
          {{ $t('chat.newSession') }}
        </a-button>
        <div class="session-toolbar">
          <div class="toolbar-left" @click.stop="toggleSelectAll">
            <a-checkbox :checked="isAllSelected" :indeterminate="isIndeterminate" />
            <span v-if="selectedSessionIds.size > 0" class="selected-count">{{ $t('chat.selectedCount', { count: selectedSessionIds.size }) }}</span>
            <span v-else class="select-all-label">{{ $t('chat.selectAll') }}</span>
          </div>
          <a-button v-if="selectedSessionIds.size > 0" type="primary" danger size="small" @click="confirmBatchDelete">
            <DeleteOutlined />
            {{ $t('chat.batchDelete') }}
          </a-button>
        </div>
        <div class="session-list">
          <div v-for="session in sessions" :key="session.id"
               class="session-item"
               :class="{ active: currentSessionId === session.id, selected: selectedSessionIds.has(session.id) }"
               @click="switchSession(session.id)">
            <div class="session-checkbox" @click.stop="toggleSessionSelect(session.id)">
              <a-checkbox :checked="selectedSessionIds.has(session.id)" />
            </div>
            <div class="session-info">
              <div class="session-name">{{ truncateName(session.sessionName) }}</div>
              <div class="session-meta">
                <span>{{ $t('chat.tokenLabel') }}: {{ session.totalTokens }}</span>
                <span style="margin-left: 8px;">{{ $t('chat.qaCount') }}: {{ session.messageCount }}</span>
              </div>
            </div>
            <a-button type="text" size="small" danger @click.stop="confirmDeleteSession(session.id)">
              <DeleteOutlined />
            </a-button>
          </div>
          <a-empty v-if="sessions.length === 0" :description="$t('chat.noSession')" />
        </div>
      </div>

      <div class="message-panel">
        <div class="message-list" ref="messageListRef">
          <div v-for="msg in messages" :key="'msg-' + msg.id" class="message-item"
               :class="msg.role === 'user' ? 'user-msg' : 'assistant-msg'">
            <div class="msg-avatar">
              <a-avatar v-if="msg.role === 'user'" :style="{ backgroundColor: 'var(--primary-color)' }">U</a-avatar>
              <a-avatar v-else :size="70" :src="'/Metrix-logo.png'" />
            </div>
            <div class="msg-content-wrapper">
              <div class="msg-role-label">{{ msg.role === 'user' ? 'User' : 'Metrix AI' }}</div>
              
              <template v-if="msg.role === 'assistant'">
                <div v-if="msg.isStreaming" class="streaming-container">
                  <div class="step-overlay">
                    <MarkdownRender :content="currentStep || t('chat.thinkingPrompt')" :final="true" />
                    <div class="typing-indicator">
                      <span class="dot"></span>
                      <span class="dot"></span>
                      <span class="dot"></span>
                    </div>
                  </div>
                </div>

                <template v-else>
                  <div v-if="msg.steps && msg.steps.length > 0" class="process-steps-box">
                    <details open>
                      <summary class="process-summary">{{ $t('chat.processingSteps') }} ({{ formatDuration(totalElapsed(msg.steps)) }})</summary>
                      <div v-for="s in msg.steps" :key="s.step" class="step-row">
                        <span class="step-status-icon">{{ s.status === 'completed' ? '✅' : '⚠️' }}</span>
                        <span class="step-label">Step {{ s.step }}/8: {{ s.title }}</span>
                        <span class="step-duration">{{ formatDuration(s.elapsed) }}</span>
                      </div>
                    </details>
                  </div>
                  <details open v-if="extractThinking(msg.content)" class="thinking-box">
                    <summary>{{ $t('chat.thinking') }}</summary>
                    <div class="thinking-content" v-html="renderMarkdown(extractThinking(msg.content))"></div>
                  </details>
                  <div v-if="extractReport(msg.content)" class="report-content markdown-rendered" v-html="renderMarkdown(extractReport(msg.content))"></div>
                  <div v-else class="report-content markdown-rendered" v-html="renderMarkdown(msg.content)"></div>
                </template>

                <div v-if="msg.tokens && !msg.isStreaming" class="msg-tokens">Token: {{ msg.tokens }}</div>
              </template>
              
               <div v-else class="msg-content markdown-body">
                 <MarkdownRender :content="msg.content" :final="true" />
               </div>
            </div>
          </div>
        </div>

         <div class="input-area">
          <a-textarea v-model:value="inputText"
                      :placeholder="$t('chat.inputPlaceholder')"
                      :rows="3"
                      @pressEnter="handleSend"
                      :disabled="isStreaming" />
          <div class="input-actions">
            <span class="input-hint">Enter 发送</span>
            <a-button type="primary" @click="handleSend" :loading="isStreaming" :disabled="!inputText.trim()">
              <SendOutlined />
              {{ $t('chat.send') }}
            </a-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, SendOutlined } from '@ant-design/icons-vue'
import { createChatSession, listChatSessions, deleteChatSession, deleteChatSessions, getChatSessionMessages, sendChatMessage } from '../api/index.js'
import MarkdownRender from 'markstream-vue'
import { marked } from 'marked'

const { t } = useI18n()

const THINKING_MARKER = '【思考过程】'
const REPORT_MARKER = '【综合分析报告】'

const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const inputText = ref('')
const isStreaming = ref(false)
const currentStep = ref('')
const messageListRef = ref(null)
const sessionLimitModal = ref(false)
const selectedSessionIds = ref(new Set())

const isAllSelected = computed(() => {
  return sessions.value.length > 0 && selectedSessionIds.value.size === sessions.value.length
})

const isIndeterminate = computed(() => {
  return selectedSessionIds.value.size > 0 && selectedSessionIds.value.size < sessions.value.length
})

function extractThinking(content) {
  const match = content.match(/【思考过程】([\s\S]*?)(?=【综合分析报告】|$)/)
  return match ? match[1].trim() : ''
}

function extractReport(content) {
  const idx = content.indexOf('【综合分析报告】')
  if (idx === -1) return ''
  return content.slice(idx + '【综合分析报告】'.length).trim()
}

function truncateName(name, maxLen = 10) {
  if (!name) return ''
  return name.length > maxLen ? name.slice(0, maxLen) + '...' : name
}

async function loadSessions() {
  try {
    const res = await listChatSessions()
    sessions.value = res.data || []
  } catch (e) {
    console.error('加载会话列表失败', e)
  }
}

async function createSession() {
  isStreaming.value = false
  currentStep.value = ''
  selectedSessionIds.value = new Set()
  try {
    const now = new Date()
    const name = t('chat.sessionName') + ' ' + now.toLocaleString('zh-CN')
    const res = await createChatSession({ sessionName: name })
    if (res.code === 200) {
      sessions.value.unshift(res.data)
      currentSessionId.value = res.data.id
      messages.value = []
      await loadSessions()
    } else {
      message.error(res.message || '创建失败')
    }
  } catch (e) {
    message.error('创建会话失败')
  }
}

async function switchSession(id) {
  isStreaming.value = false
  currentStep.value = ''
  selectedSessionIds.value = new Set()
  currentSessionId.value = id
  try {
    const res = await getChatSessionMessages(id)
    const msgs = (res.data || []).map(m => {
      if (m.steps && typeof m.steps === 'string') {
        try { m.steps = JSON.parse(m.steps) } catch (e) { m.steps = [] }
      }
      return m
    })
    messages.value = msgs
    await nextTick()
    scrollToBottom()
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

function toggleSessionSelect(id) {
  const set = new Set(selectedSessionIds.value)
  if (set.has(id)) {
    set.delete(id)
  } else {
    set.add(id)
  }
  selectedSessionIds.value = set
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedSessionIds.value = new Set()
  } else {
    selectedSessionIds.value = new Set(sessions.value.map(s => s.id))
  }
}

function confirmBatchDelete() {
  if (selectedSessionIds.value.size === 0) return
  Modal.confirm({
    title: t('chat.batchDeleteConfirm'),
    content: t('chat.batchDeleteDesc', { count: selectedSessionIds.value.size }),
    onOk: async () => {
      try {
        const ids = Array.from(selectedSessionIds.value)
        await deleteChatSessions(ids)
        if (currentSessionId.value && ids.includes(currentSessionId.value)) {
          currentSessionId.value = null
          messages.value = []
        }
        selectedSessionIds.value = new Set()
        await loadSessions()
        message.success(t('chat.batchDeleteSuccess', { count: ids.length }))
      } catch (e) {
        message.error('批量删除失败')
      }
    }
  })
}

function confirmDeleteSession(id) {
  Modal.confirm({
    title: t('chat.deleteConfirm'),
    onOk: async () => {
      try {
        await deleteChatSession(id)
        if (currentSessionId.value === id) {
          currentSessionId.value = null
          messages.value = []
          isStreaming.value = false
          currentStep.value = ''
        }
        await loadSessions()
      } catch (e) {
        message.error('删除失败')
      }
    }
  })
}

function renderMarkdown(content) {
  if (!content) return ''
  try {
    const html = marked.parse(content, { async: false })
    return typeof html === 'string' ? html : content
  } catch (e) {
    console.error('renderMarkdown error:', e)
    return content
  }
}

function formatDuration(ms) {
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

function totalElapsed(steps) {
  return steps.reduce((sum, s) => sum + (s.elapsed || 0), 0)
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || isStreaming.value) return

  if (!currentSessionId.value) {
    try {
      const now = new Date()
      const name = t('chat.sessionName') + ' ' + now.toLocaleString('zh-CN')
      const res = await createChatSession({ sessionName: name })
      if (res.code === 200) {
        sessions.value.unshift(res.data)
        currentSessionId.value = res.data.id
      } else {
        message.error(res.message || '创建会话失败')
        return
      }
    } catch (e) {
      message.error('创建会话失败')
      return
    }
  }

  inputText.value = ''
  isStreaming.value = true
  currentStep.value = ''

  const userMsg = {
    id: Date.now(),
    role: 'user',
    content: text,
    tokens: Math.ceil(text.length / 2),
    createTime: new Date().toISOString()
  }
  const assistantMsg = {
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
    tokens: 0,
    isStreaming: true,
    steps: [],
    createTime: new Date().toISOString()
  }
  messages.value.push(userMsg, assistantMsg)

  await nextTick()
  scrollToBottom()

  let fullContent = ''

  await sendChatMessage(
    currentSessionId.value,
    text,
    {
      onStep: (text) => {
        currentStep.value = text
        scrollToBottom()
      },
      onDone: (data) => {
        const msgs = messages.value
        const last = msgs[msgs.length - 1]
        if (last && last.role === 'assistant' && last.isStreaming) {
          last.content = data.content || ''
          last.tokens = data.tokens || Math.ceil((data.content || '').length / 2)
          last.steps = data.steps || []
          last.isStreaming = false
        }
        isStreaming.value = false
        currentStep.value = ''
        loadSessions()
        scrollToBottom()
      },
      onError: (err) => {
        const msgs = messages.value
        const last = msgs[msgs.length - 1]
        if (last && last.role === 'assistant' && last.isStreaming) {
          last.content = last.content || ''
          last.isStreaming = false
        }
        isStreaming.value = false
        currentStep.value = ''
        message.error(typeof err === 'string' ? err : 'AI响应出错')
        scrollToBottom()
      }
    }
  )
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.chat-container {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}

.chat-layout {
  display: flex;
  height: 100%;
  gap: 16px;
}

.session-panel {
  width: 260px;
  flex-shrink: 0;
  background: white;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

.new-session-btn {
  margin-bottom: 12px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
  margin-bottom: 8px;
  background: #fff1f0;
  border-radius: 6px;
  padding: 6px 8px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.selected-count {
  font-size: 12px;
  color: #ff4d4f;
  font-weight: 500;
}

.select-all-label {
  font-size: 12px;
  color: #666;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 8px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.session-item:hover {
  background: #f0f5ff;
}

.session-item.active {
  background: #e6f7ff;
  border-left: 3px solid var(--primary-color);
}

.session-item.selected {
  background: #fff1f0;
}

.session-checkbox {
  flex-shrink: 0;
  margin-right: 6px;
  display: flex;
  align-items: center;
}

.session-info {
  flex: 1;
  overflow: hidden;
}

.session-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.message-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user-msg {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-content-wrapper {
  max-width: 75%;
}

.user-msg .msg-content-wrapper {
  text-align: right;
}

.msg-role-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.msg-content {
  background: #f5f5f5;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.user-msg .msg-content {
  background: #e6f7ff;
  border-radius: 12px 4px 12px 12px;
}

.assistant-msg .msg-content {
  background: #f6ffed;
  border-radius: 4px 12px 12px 12px;
}

.msg-tokens {
  font-size: 11px;
  color: #bbb;
  margin-top: 4px;
  text-align: right;
}

.streaming-container {
  background: #f6ffed;
  border-radius: 4px 12px 12px 12px;
  padding: 12px 16px;
}

.thinking-box {
  margin-bottom: 16px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 8px;
  overflow: hidden;
}

.typing-indicator {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  justify-content: center;
  margin-top: 8px;
}

.typing-indicator .dot {
  width: 6px;
  height: 6px;
  background: #52c41a;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator .dot:nth-child(1) {
  animation-delay: 0s;
}

.typing-indicator .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.step-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  text-align: center;
  font-size: 14px;
  color: #52c41a;
}

.process-steps-box {
  margin-bottom: 16px;
  background: #f0f5ff;
  border: 1px solid #d6e4ff;
  border-radius: 8px;
  overflow: hidden;
}

.process-steps-box summary {
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
  color: var(--primary-color);
  padding: 8px 12px;
  background: #f0f5ff;
}

.process-summary {
  display: block;
}

.step-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  font-size: 13px;
  color: #333;
  border-top: 1px solid #e8e8e8;
}

.step-row:last-child {
  border-bottom: none;
}

.step-status-icon {
  flex-shrink: 0;
  font-size: 14px;
}

.step-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-duration {
  flex-shrink: 0;
  font-size: 12px;
  color: #999;
  font-family: 'Courier New', monospace;
}

.input-area {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: white;
  border-radius: 0 0 8px 8px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-hint {
  font-size: 12px;
  color: #bbb;
}

.thinking-box summary {
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
  color: #d48806;
  padding: 4px 12px;
}

.thinking-content {
  padding: 8px 12px 12px;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  background: #fffef5;
  border-top: 1px solid #ffe58f;
}

.report-content {
  background: #f6ffed;
  padding: 12px 16px;
  border-radius: 4px 12px 12px 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.thinking-box .msg-content {
  background: transparent;
  padding: 8px 0 4px;
  font-size: 13px;
  color: #666;
}

.thinking-box .msg-content :deep(p) {
  margin: 0.2em 0;
}

.markdown-rendered :deep(h1),
.markdown-rendered :deep(h2),
.markdown-rendered :deep(h3),
.markdown-rendered :deep(h4) {
  margin: 1em 0 0.5em;
  font-weight: 600;
  color: #1a1a1a;
}

.markdown-rendered :deep(h1) { font-size: 22px; }
.markdown-rendered :deep(h2) { font-size: 19px; }
.markdown-rendered :deep(h3) { font-size: 16px; }
.markdown-rendered :deep(h4) { font-size: 14px; }

.markdown-rendered :deep(p) {
  margin: 0.5em 0;
  line-height: 1.7;
}

.markdown-rendered :deep(ul),
.markdown-rendered :deep(ol) {
  padding-left: 22px;
  margin: 0.5em 0;
}

.markdown-rendered :deep(li) {
  margin: 0.3em 0;
}

.markdown-rendered :deep(blockquote) {
  border-left: 4px solid #d6e4ff;
  padding: 4px 12px;
  margin: 0.5em 0;
  color: #555;
  background: #f8faff;
}

.markdown-rendered :deep(code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.9em;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.markdown-rendered :deep(pre) {
  background: #f5f5f5;
  padding: 12px 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 0.5em 0;
}

.markdown-rendered :deep(pre code) {
  background: none;
  padding: 0;
  border-radius: 0;
}

.markdown-rendered :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.5em 0;
}

.markdown-rendered :deep(th),
.markdown-rendered :deep(td) {
  border: 1px solid #e0e0e0;
  padding: 8px 12px;
  text-align: left;
}

.markdown-rendered :deep(th) {
  background: #fafafa;
  font-weight: 600;
}

.markdown-rendered :deep(hr) {
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 1em 0;
}

.markdown-rendered :deep(a) {
  color: var(--primary-color);
  text-decoration: none;
}

.markdown-rendered :deep(a:hover) {
  text-decoration: underline;
}

.markdown-rendered :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

</style>
