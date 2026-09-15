export const MARKET_TURNOVER_THRESHOLD = 2_000_000_000_000

export const unwrapMarketTurnover = turnover => {
  if (!turnover || typeof turnover !== 'object') return null
  return Array.isArray(turnover.history) ? turnover : turnover.data
}

export const getLatestTurnoverItem = turnover => {
  const history = unwrapMarketTurnover(turnover)?.history
  return Array.isArray(history) && history.length ? history[history.length - 1] : null
}

export const hasMarketInsightsData = insights => {
  if (!insights || typeof insights !== 'object') return false

  const sections = [
    insights.dragonTiger,
    insights.fundFlow,
    insights.popularityRank,
    insights.industrySectors,
    insights.limitPools?.limitUp,
    insights.limitPools?.limitDown,
    insights.limitPools?.strong,
    insights.limitPools?.broken
  ]
  return sections.some(section => Array.isArray(section) && section.length > 0)
}

export const hasMarketTurnoverHistory = turnover => {
  const history = unwrapMarketTurnover(turnover)?.history
  return Array.isArray(history) && history.length > 0
}

export const calculateMarketTurnoverAverage = (history, period = 60) => {
  if (!Array.isArray(history) || period <= 0) return null

  const amounts = history
    .slice(-period)
    .map(item => Number(item?.amount))
    .filter(amount => Number.isFinite(amount) && amount > 0)
  if (!amounts.length) return null

  return amounts.reduce((total, amount) => total + amount, 0) / amounts.length
}
