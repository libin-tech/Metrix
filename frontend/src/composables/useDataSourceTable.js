import {computed} from 'vue'

export function useDataSourceTable(t, namespace, {withDataType = false} = {}) {
  const columns = computed(() => [
    {title: t('config.id'), dataIndex: 'id', key: 'id', width: 80},
    {title: t(`${namespace}.sourceType`), key: 'sourceName', width: 200},
    {title: t(`${namespace}.apiUrl`), dataIndex: 'apiUrl', key: 'apiUrl', width: 240, ellipsis: true},
    {title: t(`${namespace}.apiKey`), key: 'apiKey', width: 260},
    ...(withDataType ? [{title: t(`${namespace}.dataType`), dataIndex: 'dataType', key: 'dataType', width: 160, ellipsis: true}] : []),
    {title: t(`${namespace}.interval`), dataIndex: 'requestInterval', key: 'requestInterval', width: 130},
    {title: t(`${namespace}.timeout`), dataIndex: 'timeout', key: 'timeout', width: 130},
    {title: t(`${namespace}.remark`), dataIndex: 'remark', key: 'remark', width: 200, ellipsis: true},
    {title: t('config.status'), key: 'isActive', width: 80},
    {title: t('config.action'), key: 'action', width: 200}
  ])
  const scroll = computed(() => ({x: columns.value.reduce((width, column) => width + column.width, 0)}))

  return {columns, scroll}
}
