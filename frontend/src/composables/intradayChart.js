export const tradingMinutes = () => {
  const sessions = [{start: 570, end: 690}, {start: 780, end: 900}]
  return sessions.flatMap(({start, end}) => Array.from({length: end - start + 1}, (_, index) => {
    const minute = start + index
    return `${String(Math.floor(minute / 60)).padStart(2, '0')}:${String(minute % 60).padStart(2, '0')}`
  }))
}

export const buildIntradaySeries = (points = []) => {
  const byTime = new Map(points.map(point => [point.time, point]))
  let previousPrice = null
  return tradingMinutes().map(time => {
    const point = byTime.get(time)
    if (!point) return {time, price: null, volume: null, up: false}
    const up = previousPrice == null || point.price >= previousPrice
    previousPrice = point.price
    return {...point, up}
  })
}
