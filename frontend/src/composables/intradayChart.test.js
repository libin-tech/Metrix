import test from 'node:test'
import assert from 'node:assert/strict'
import {buildIntradaySeries, tradingMinutes} from './intradayChart.js'

test('trading axis excludes lunch and covers both sessions', () => {
  const minutes = tradingMinutes()
  assert.equal(minutes.length, 242)
  assert.equal(minutes[0], '09:30')
  assert.equal(minutes[120], '11:30')
  assert.equal(minutes[121], '13:00')
  assert.equal(minutes.at(-1), '15:00')
  assert.ok(!minutes.includes('12:00'))
})

test('missing minutes remain gaps and volume stays per minute', () => {
  const rows = buildIntradaySeries([
    {time: '09:31', price: 10, volume: 1000},
    {time: '09:33', price: 9, volume: 0},
    {time: '13:00', price: 11, volume: null}
  ])
  assert.equal(rows[0].price, null)
  assert.equal(rows[1].volume, 1000)
  assert.equal(rows[2].volume, null)
  assert.equal(rows[3].volume, 0)
  assert.equal(rows[3].up, false)
  assert.equal(rows[121].up, true)
  assert.equal(rows.at(-1).price, null)
})
