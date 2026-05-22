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
        <div class="session-list">
          <div v-for="session in sessions" :key="session.id"
               class="session-item"
               :class="{ active: currentSessionId === session.id }"
               @click="switchSession(session.id)">
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
          <div v-for="msg in displayMessages" :key="msg.id" class="message-item"
               :class="msg.role === 'user' ? 'user-msg' : 'assistant-msg'">
            <div class="msg-avatar">
              <a-avatar :style="msg.role === 'user' ? { backgroundColor: '#1890ff' } : { backgroundColor: '#52c41a' }">
                {{ msg.role === 'user' ? 'U' : 'AI' }}
              </a-avatar>
            </div>
            <div class="msg-content-wrapper">
              <div class="msg-role-label">{{ msg.role === 'user' ? 'User' : 'Metrix AI' }}</div>
              
              <template v-if="msg.role === 'assistant'">
                <div v-if="msg.isStreaming" class="streaming-container">
                  <details v-if="msg.thinkingContent" class="thinking-box" open>
                    <summary class="thinking-header">
                      <span class="thinking-title">{{ $t('chat.thinking') }}</span>
                      <span class="typing-indicator" v-if="msg.isThinking">
                        <span class="dot"></span>
                        <span class="dot"></span>
                        <span class="dot"></span>
                      </span>
                    </summary>
                    <div class="thinking-content">
                      <MarkdownRender 
                        :content="msg.thinkingContent"
                        :max-live-nodes="0"
                        :batch-rendering="true"
                        class="streaming-markdown"
                      />
                    </div>
                  </details>
                  
                  <div v-if="msg.reportContent || msg.isReporting" class="report-section">
                    <div class="report-header" v-if="msg.isReporting">
                      <span class="report-title">{{ $t('chat.report') }}</span>
                      <span class="typing-indicator">
                        <span class="dot"></span>
                        <span class="dot"></span>
                        <span class="dot"></span>
                      </span>
                    </div>
                    <MarkdownRender 
                      :content="msg.reportContent"
                      :max-live-nodes="0"
                      :batch-rendering="true"
                      class="streaming-markdown"
                    />
                  </div>
                  
                  <div v-if="!msg.thinkingContent && !msg.reportContent" class="msg-content">
                    <div class="thinking-placeholder">
                      <span class="typing-indicator">
                        <span class="dot"></span>
                        <span class="dot"></span>
                        <span class="dot"></span>
                      </span>
                    </div>
                  </div>
                </div>
                
                <template v-else>
                  <details v-if="extractThinking(msg.content)" class="thinking-box">
                    <summary>{{ $t('chat.thinking') }}</summary>
                    <div class="msg-content markdown-body">
                      <MarkdownRender :content="extractThinking(msg.content)" />
                    </div>
                  </details>
                  <div v-if="extractReport(msg.content)" class="msg-content markdown-body">
                    <MarkdownRender :content="extractReport(msg.content)" />
                  </div>
                  <div v-else class="msg-content markdown-body">
                    <MarkdownRender :content="msg.content" />
                  </div>
                </template>
                
                <div v-if="msg.tokens && !msg.isStreaming" class="msg-tokens">Token: {{ msg.tokens }}</div>
              </template>
              
              <div v-else class="msg-content markdown-body">
                <MarkdownRender :content="msg.content" />
              </div>
            </div>
          </div>
        </div>

        <div class="steps-panel" v-if="stepsContent">
          <div class="steps-header">处理进度</div>
          <div class="steps-content markdown-body">
            <MarkdownRender :content="stepsContent" />
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
import { createChatSession, listChatSessions, deleteChatSession, getChatSessionMessages, sendChatMessage } from '../api/index.js'
import MarkdownRender from 'markstream-vue'
import 'markstream-vue/index.css'

const { t } = useI18n()

const THINKING_MARKER = '【思考过程】'
const REPORT_MARKER = '【综合分析报告】'

const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const inputText = ref('')
const isStreaming = ref(false)
const streamingMessage = ref(null)
const stepsContent = ref('')
const messageListRef = ref(null)
const sessionLimitModal = ref(false)

const streamParser = {
  buffer: '',
  phase: 'init',
  thinkingContent: '',
  reportContent: '',
  
  reset() {
    this.buffer = ''
    this.phase = 'init'
    this.thinkingContent = ''
    this.reportContent = ''
  },
  
  append(token) {
    this.buffer += token
    this.processBuffer()
  },
  
  processBuffer() {
    while (true) {
      if (this.phase === 'init') {
        const thinkingIdx = this.buffer.indexOf(THINKING_MARKER)
        if (thinkingIdx !== -1) {
          const before = this.buffer.substring(0, thinkingIdx)
          this.buffer = this.buffer.substring(thinkingIdx + THINKING_MARKER.length)
          this.phase = 'thinking'
          continue
        }
        
        const reportIdx = this.buffer.indexOf(REPORT_MARKER)
        if (reportIdx !== -1) {
          const before = this.buffer.substring(0, reportIdx)
          this.thinkingContent += before
          this.buffer = this.buffer.substring(reportIdx + REPORT_MARKER.length)
          this.phase = 'report'
          continue
        }
        
        if (this.buffer.length > Math.max(THINKING_MARKER.length, REPORT_MARKER.length) * 2) {
          const safeLen = this.buffer.length - Math.max(THINKING_MARKER.length, REPORT_MARKER.length)
          const toEmit = this.buffer.substring(0, safeLen)
          this.thinkingContent += toEmit
          this.buffer = this.buffer.substring(safeLen)
        }
        break
      }
      
      if (this.phase === 'thinking') {
        const reportIdx = this.buffer.indexOf(REPORT_MARKER)
        if (reportIdx !== -1) {
          const before = this.buffer.substring(0, reportIdx)
          this.thinkingContent += before
          this.buffer = this.buffer.substring(reportIdx + REPORT_MARKER.length)
          this.phase = 'report'
          continue
        }
        
        const keepLen = REPORT_MARKER.length
        if (this.buffer.length > keepLen) {
          const emitLen = this.buffer.length - keepLen
          const toEmit = this.buffer.substring(0, emitLen)
          this.thinkingContent += toEmit
          this.buffer = this.buffer.substring(emitLen)
        }
        break
      }
      
      if (this.phase === 'report') {
        if (this.buffer.length > 0) {
          this.reportContent += this.buffer
          this.buffer = ''
        }
        break
      }
    }
  },
  
  flush() {
    if (this.buffer.length > 0) {
      if (this.phase === 'init' || this.phase === 'thinking') {
        this.thinkingContent += this.buffer
      } else {
        this.reportContent += this.buffer
      }
      this.buffer = ''
    }
  }
}

const displayMessages = computed(() => {
  const result = [...messages.value]
  if (streamingMessage.value) {
    result.push(streamingMessage.value)
  }
  return result
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
  streamingMessage.value = null
  stepsContent.value = ''
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
  streamingMessage.value = null
  stepsContent.value = ''
  currentSessionId.value = id
  try {
    const res = await getChatSessionMessages(id)
    messages.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch (e) {
    console.error('加载消息失败', e)
  }
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
          streamingMessage.value = null
          stepsContent.value = ''
        }
        await loadSessions()
      } catch (e) {
        message.error('删除失败')
      }
    }
  })
}

function initStreamingMessage() {
  streamParser.reset()
  streamingMessage.value = {
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
    thinkingContent: '',
    reportContent: '',
    tokens: 0,
    isStreaming: true,
    isThinking: true,
    isReporting: false,
    createTime: new Date().toISOString()
  }
}

function updateStreamingMessageFromParser() {
  if (!streamingMessage.value) return
  streamingMessage.value.thinkingContent = streamParser.thinkingContent
  streamingMessage.value.reportContent = streamParser.reportContent
  
  if (streamParser.phase === 'report') {
    streamingMessage.value.isThinking = false
    streamingMessage.value.isReporting = true
  }
  
  scrollToBottom()
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
  stepsContent.value = ''
  initStreamingMessage()

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: text,
    tokens: Math.ceil(text.length / 2),
    createTime: new Date().toISOString()
  })

  await nextTick()
  scrollToBottom()

  let fullContent = ''

  await sendChatMessage(
    currentSessionId.value,
    text,
    {
      onThinking: (token) => {
        if (!streamingMessage.value) return
        fullContent += token
        streamParser.append(token)
        updateStreamingMessageFromParser()
      },
      onReport: (token) => {
        if (!streamingMessage.value) return
        fullContent += token
        streamParser.append(token)
        updateStreamingMessageFromParser()
      },
      onStep: (text) => {
        if (stepsContent.value) {
          stepsContent.value += '\n\n' + text
        } else {
          stepsContent.value = text
        }
        scrollToBottom()
      },
      onDone: (data) => {
        streamParser.flush()
        if (streamingMessage.value) {
          const tokens = data.tokens || Math.ceil(fullContent.length / 2)
          streamingMessage.value.isStreaming = false
          streamingMessage.value.isThinking = false
          streamingMessage.value.isReporting = false
          streamingMessage.value.tokens = tokens
          streamingMessage.value.thinkingContent = streamParser.thinkingContent
          streamingMessage.value.reportContent = streamParser.reportContent
          
          messages.value.push({
            ...streamingMessage.value,
            content: fullContent
          })
          streamingMessage.value = null
        }
        isStreaming.value = false
        stepsContent.value = ''
        loadSessions()
        scrollToBottom()
      },
      onError: (err) => {
        streamParser.flush()
        if (streamingMessage.value && fullContent) {
          streamingMessage.value.isStreaming = false
          streamingMessage.value.isThinking = false
          streamingMessage.value.isReporting = false
          streamingMessage.value.tokens = Math.ceil(fullContent.length / 2)
          streamingMessage.value.thinkingContent = streamParser.thinkingContent
          streamingMessage.value.reportContent = streamParser.reportContent
          
          messages.value.push({
            ...streamingMessage.value,
            content: fullContent
          })
          streamingMessage.value = null
        }
        isStreaming.value = false
        stepsContent.value = ''
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
  border-left: 3px solid #1890ff;
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

.thinking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
  color: #d48806;
  padding: 8px 12px;
  background: #fffbe6;
  list-style: none;
}

.thinking-header::-webkit-details-marker {
  display: none;
}

.thinking-title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.thinking-content {
  padding: 8px 12px 12px;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  background: #fffef5;
  border-top: 1px solid #ffe58f;
}

.thinking-placeholder {
  display: flex;
  justify-content: center;
  padding: 20px;
}

.report-section {
  background: transparent;
}

.report-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.typing-indicator {
  display: inline-flex;
  gap: 4px;
  align-items: center;
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

.steps-panel {
  background: #f0f5ff;
  border-top: 1px solid #d6e4ff;
  padding: 12px 20px;
  max-height: 150px;
  overflow-y: auto;
}

.steps-header {
  font-size: 12px;
  font-weight: 600;
  color: #1890ff;
  margin-bottom: 8px;
}

.steps-content {
  font-size: 13px;
  line-height: 1.8;
  color: #333;
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

:deep(.streaming-markdown h1) { font-size: 1.5em; margin: 0.5em 0; }
:deep(.streaming-markdown h2) { font-size: 1.3em; margin: 0.5em 0; }
:deep(.streaming-markdown h3) { font-size: 1.1em; margin: 0.4em 0; }
:deep(.streaming-markdown p) { margin: 0.3em 0; }
:deep(.streaming-markdown table) { border-collapse: collapse; width: 100%; margin: 0.5em 0; font-size: 13px; }
:deep(.streaming-markdown th) { background: #fafafa; border: 1px solid #e8e8e8; padding: 6px 10px; font-weight: 600; }
:deep(.streaming-markdown td) { border: 1px solid #e8e8e8; padding: 6px 10px; }
:deep(.streaming-markdown code) { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
:deep(.streaming-markdown pre) { background: #f5f5f5; padding: 12px; border-radius: 6px; overflow-x: auto; }
:deep(.streaming-markdown ul) { padding-left: 20px; }
:deep(.streaming-markdown ol) { padding-left: 20px; }
:deep(.streaming-markdown strong) { font-weight: 600; }

.thinking-box summary {
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
  color: #d48806;
  padding: 4px 0;
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
</style>
