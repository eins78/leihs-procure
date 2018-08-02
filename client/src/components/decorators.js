import f from 'lodash'
import { formatMoney } from 'accounting-js'

export const DisplayName = (o, { short = false, abbr = false } = {}) => {
  if (short && abbr) throw new Error('Invalid Options!')

  if (!o) return '?'

  switch (o.__typename) {
    case 'User':
      return !short
        ? `${o.firstname || ''} ${o.lastname || ''}`.trim()
        : !abbr
          ? `${f
              .filter([f.first(f.toUpper(o.firstname))])
              .concat('')
              .join('. ')}${o.lastname}`
          : `${o.firstname || ''} ${o.lastname || ''}`
              .split(/\s/)
              .map(s => f.first(s).toUpperCase())
              .filter((s, i, a) => i < 2 || a.length - i <= 3)
              .join(' ')

    case 'Room':
      return short || !o.description
        ? `${o.name}`
        : `${o.name} (${o.description})`

    case 'Organization':
      return short || !o.shortname
        ? `${o.shortname || o.name}`
        : `${o.name} (${o.shortname})`

    default:
      throw new Error(`DisplayName: unknown type '${o.__typename}'!`)
  }
}

export const RequestTotalAmount = fields => {
  const allQuantities = ['requested', 'approved', 'order'].map(k => {
    const v = f.get(fields, [`${k}_quantity`])
    return f.isObject(v) ? v.value : v
  })
  const quantity = f.last(f.filter(allQuantities, f.present))

  const price_cents =
    f.get(fields, 'price_cents.value') || f.get(fields, 'price_cents') || '0'

  const price = parseInt(price_cents, 10)
  return (parseInt(quantity, 10) || 0) * price
}

// TODO: currency config, hardcoded to CH-de for now
// NOTE: `precision: 0` because Procure only supports integers
export const formatCurrency = (n = 0) =>
  formatMoney(n / 100, {
    decimal: '.',
    thousand: "'",
    symbol: 'CHF',
    format: '%v %s',
    precision: 0
  })
