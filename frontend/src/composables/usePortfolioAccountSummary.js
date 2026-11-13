import {computed} from 'vue'
import {useI18n} from 'vue-i18n'

export const accountMetrics = [
  {key: 'totalPrincipal', label: 'accountManagement.totalPrincipal'},
  {key: 'totalAssets', label: 'portfolio.totalAssets'},
  {key: 'totalMarketValue', label: 'portfolio.totalMarketValue'},
  {key: 'totalProfitLoss', label: 'portfolio.totalProfitLossAmt', profitLoss: true},
  {key: 'realizedProfitLoss', label: 'accountManagement.realizedProfitLoss', profitLoss: true},
  {key: 'cumulativeProfitLoss', label: 'portfolio.cumulativeProfitLoss', profitLoss: true}
]

export function useAccountAmountFormatter() {
  const {locale} = useI18n()
  const formatter = computed(() => new Intl.NumberFormat(locale.value, {style: 'currency', currency: 'CNY'}))
  return value => value == null ? '—' : formatter.value.format(value)
}
