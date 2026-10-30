/** 缺失值不转成 0；用于卡片、明细和趋势共用的格式化。 */
export const intelligenceNumber = value => value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value)) ? Number(value) : null
export const formatIntelligenceNumber = (value, locale = 'zh-CN', digits = 2) => {
  const number = intelligenceNumber(value)
  return number === null ? '—' : number.toLocaleString(locale, { maximumFractionDigits: digits, minimumFractionDigits: digits })
}
export const formatIntelligencePercent = (value, locale = 'zh-CN') => intelligenceNumber(value) === null ? '—' : `${formatIntelligenceNumber(value, locale)}%`
export const reportIsStale = (report, now = Date.now()) => {
  const time = Date.parse(report?.asOf)
  return Boolean(report?.dataQuality?.stale) || !Number.isFinite(time) || now - time > 15 * 60_000 || time > now
}
export const riskColor = risk => ({ LOW: 'green', MEDIUM: 'blue', ELEVATED: 'orange', HIGH: 'volcano' }[risk] || 'default')
