import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import test from 'node:test'

const viewPath = new URL('../views/MarketWorkbench.vue', import.meta.url)

test('行情工作台会请求并逐条展示当日异动', async () => {
  const view = await readFile(viewPath, 'utf8')

  assert.match(view, /getMarketWorkbenchAnomalies/)
  assert.match(view, /v-for="anomaly in anomalies"/)
  assert.match(view, /\.anomaly-list \{ max-height: none; overflow-y: visible; \}/)
  assert.match(view, /\.anomaly-item p \{ overflow: visible; text-overflow: clip; white-space: normal; \}/)
})
