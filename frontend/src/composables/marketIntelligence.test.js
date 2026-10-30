import test from 'node:test'
import assert from 'node:assert/strict'
import { formatIntelligenceNumber, formatIntelligencePercent, reportIsStale } from './marketIntelligence.js'
import zh from '../i18n/dailyMarket.zh-CN.js'
import en from '../i18n/dailyMarket.en.js'

test('missing financial values never render as zero', () => {
  for (const value of [null, undefined, '', 'NaN', Infinity]) assert.equal(formatIntelligenceNumber(value), '—')
  assert.equal(formatIntelligencePercent(0), '0.00%')
  assert.equal(formatIntelligencePercent(-1.25), '-1.25%')
})
test('stale, future and missing timestamps invalidate the displayed current assessment', () => {
  const now = Date.parse('2026-09-15T16:00:00+08:00')
  assert.equal(reportIsStale({ asOf: '2026-09-15T15:50:00+08:00' }, now), false)
  assert.equal(reportIsStale({ asOf: '2026-09-15T15:40:00+08:00' }, now), true)
  assert.equal(reportIsStale({ asOf: '2026-09-16T15:40:00+08:00' }, now), true)
  assert.equal(reportIsStale({ asOf: null }, now), true)
})
test('both languages provide the same complete translation keys', () => {
  const keys = value => Object.entries(value).flatMap(([key, item]) => typeof item === 'object' ? keys(item).map(child => `${key}.${child}`) : key).sort()
  assert.deepEqual(keys(zh), keys(en))
})
