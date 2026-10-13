/** Decode SSE frames across arbitrary network chunks, including CRLF and UTF-8 boundaries. */
export async function readAgentStream(stream, callbacks = {}, signal) {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let completed = false
  function dispatch(frame) {
    const lines = frame.split(/\r?\n/)
    const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim() || 'message'
    const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).replace(/^ /, '')).join('\n')
    if (!data && event !== 'done') return
    const handlers = {
      thinking: () => callbacks.onThinking?.(JSON.parse(data)),
      final: () => callbacks.onFinal?.(JSON.parse(data)),
      done: () => { completed = true; callbacks.onDone?.(data ? JSON.parse(data) : {}) },
      error: () => { throw new Error(data || 'streamError') }
    }
    handlers[event]?.()
  }
  function drain() {
    let separator
    while ((separator = /\r?\n\r?\n/.exec(buffer))) {
      const frame = buffer.slice(0, separator.index)
      buffer = buffer.slice(separator.index + separator[0].length)
      dispatch(frame)
    }
  }
  const cancelReader = () => { reader.cancel().catch(() => {}) }
  signal?.addEventListener('abort', cancelReader, { once: true })
  try {
    while (!completed && !signal?.aborted) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      drain()
    }
    buffer += decoder.decode()
    drain()
    if (!completed && !signal?.aborted) throw new Error('streamInterrupted')
    return { completed, aborted: !!signal?.aborted }
  } finally {
    signal?.removeEventListener('abort', cancelReader)
    await reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}

export function restoreAgentMessage(msg) {
  if (msg.role === 'user') return {...msg}
  let steps = msg.steps || []
  try { steps = typeof steps === 'string' ? JSON.parse(steps) : steps } catch { steps = [] }
  if (!Array.isArray(steps)) steps = []
  // Legacy model reasoning is never displayed; only the old report and public tool steps survive.
  const marker = '【综合分析报告】'
  let content = msg.content || ''
  const reportIndex = content.indexOf(marker)
  if (reportIndex >= 0) content = content.slice(reportIndex + marker.length).trim()
  if (content.includes('【思考过程】')) content = ''
  content = content.replace(/<think>[\s\S]*?(?:<\/think>|$)/gi, '')
  const status = steps.findLast(step => step.stage === 'complete')?.state || 'completed'
  return { ...msg, content, status, steps: steps.filter(step => step.stage !== 'complete'), streaming: false }
}
