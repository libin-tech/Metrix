import assert from 'node:assert/strict'
import test from 'node:test'
import {calculateMarketTurnoverAverage, getLatestTurnoverItem, hasMarketInsightsData, hasMarketTurnoverHistory, MARKET_TURNOVER_THRESHOLD, unwrapMarketTurnover} from './marketData.js'

test('部分市场洞察数据也应视为可展示', () => {
  const insights = {
    dragonTiger: [{code: '000001'}],
    fundFlow: [],
    popularityRank: [],
    industrySectors: [],
    limitPools: {limitUp: [], limitDown: [], strong: [], broken: []}
  }

  assert.equal(hasMarketInsightsData(insights), true)
})

test('所有市场洞察区块为空时不可展示', () => {
  assert.equal(hasMarketInsightsData({limitPools: {}}), false)
})

test('成交额只要存在有效历史点就应展示最新数据', () => {
  const turnover = {history: [{date: '2026-08-28', amount: 123456789}]}

  assert.equal(hasMarketTurnoverHistory(turnover), true)
  assert.deepEqual(getLatestTurnoverItem(turnover), turnover.history[0])
})

test('成交额接口的脚本响应包装层应被正确解包', () => {
  const turnover = {status: 'success', data: {history: [{date: '2026-08-28', amount: 123456789}]}}

  assert.deepEqual(unwrapMarketTurnover(turnover), turnover.data)
  assert.equal(hasMarketTurnoverHistory(turnover), true)
  assert.deepEqual(getLatestTurnoverItem(turnover), turnover.data.history[0])
})

test('成交额均线使用最近 60 个有效交易日计算', () => {
  const history = Array.from({length: 62}, (_, index) => ({amount: (index + 1) * 100}))
  history[61] = {amount: null}

  assert.equal(calculateMarketTurnoverAverage(history), 3200)
  assert.equal(MARKET_TURNOVER_THRESHOLD, 2_000_000_000_000)
})
