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
