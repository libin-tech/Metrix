import test from 'node:test'
import assert from 'node:assert/strict'
import {readAgentStream, restoreAgentMessage} from './agentStream.js'

function streamFromBytes(bytes, size) {
  return new ReadableStream({start(controller) {
    for (let i = 0; i < bytes.length; i += size) controller.enqueue(bytes.slice(i, i + size))
    controller.close()
  }})
}

test('preserves UTF-8 JSON, CRLF and multi-line SSE across arbitrary chunks', async () => {
  const wire = ': ping\r\n\r\nevent: thinking\r\ndata: {"stage":"scope",\r\ndata: "content":"沪深A股"}\r\n\r\nevent: final\r\ndata: {"content":"茅台分析","delta":true}\r\n\r\nevent: done\r\ndata: {"status":"completed"}\r\n\r\n'
  const bytes = new TextEncoder().encode(wire)
  for (const size of [1, 2, 7, 23, bytes.length]) {
    const events = []
    const result = await readAgentStream(streamFromBytes(bytes, size), {
      onThinking: data => events.push(data.content), onFinal: data => events.push(data.content), onDone: data => events.push(data.status)
    })
    assert.deepEqual(events, ['沪深A股', '茅台分析', 'completed'])
    assert.equal(result.completed, true)
  }
})

test('reports an interrupted stream without done', async () => {
  await assert.rejects(readAgentStream(streamFromBytes(new TextEncoder().encode('event: thinking\ndata: {"content":"分析"}\n\n'), 1)), /streamInterrupted/)
})

test('abort cancels a stalled reader without a completion error', async () => {
  const controller = new AbortController()
  let cancelled = false
  const stream = new ReadableStream({cancel() { cancelled = true }})
  const promise = readAgentStream(stream, {}, controller.signal)
  controller.abort()
  assert.equal((await promise).aborted, true)
  assert.equal(cancelled, true)
})

test('does not expose legacy model reasoning when restoring history', () => {
  const restored = restoreAgentMessage({content: '【思考过程】私密推理【综合分析报告】公开回复', steps: '[{"title":"行情获取","elapsed":100}]'})
  assert.equal(restored.content, '公开回复')
  assert.equal(restored.steps[0].title, '行情获取')
  assert.equal(restoreAgentMessage({content: '<think>私密推理</think>报告', steps: 'invalid'}).content, '报告')
})

test('restores persisted terminal status separately from public steps', () => {
  const restored = restoreAgentMessage({content: '拒答', steps: JSON.stringify([{stage: 'intent', content: '识别中'}, {stage: 'complete', state: 'refused'}])})
  assert.equal(restored.status, 'refused')
  assert.equal(restored.steps.length, 1)
})
