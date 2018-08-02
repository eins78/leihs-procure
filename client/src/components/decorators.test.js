import { DisplayName, RequestTotalAmount } from './decorators'

describe.only('DisplayName', () => {
  describe('type: User', () => {
    const cases = [
      [{ firstname: 'Hans', lastname: 'Meier' }, {}, 'Hans Meier'],
      [{ firstname: 'Hans', lastname: 'Meier' }, { short: true }, 'H. Meier'],
      [{ firstname: 'Hans', lastname: 'Meier' }, { abbr: true }, 'HM'],
      // no firstname
      [{ lastname: 'Meier' }, {}, 'Meier'],
      [{ lastname: 'Meier' }, { short: true }, 'Meier'],
      [{ lastname: 'Meier' }, { abbr: true }, 'M'],
      // no lastname
      [{ firstname: 'Hans' }, {}, 'Hans'],
      [{ firstname: 'Hans' }, { short: true }, 'H.'],
      [{ firstname: 'Hans' }, { abbr: true }, 'H']
      // TODO: no name
    ]

    cases.forEach(([obj, opts, expected], i) =>
      test(`case ${i + 1}`, () => {
        const result = DisplayName({ ...obj, __typename: 'User' }, opts)
        expect(result).toBe(expected)
      })
    )
  })
})

describe('RequestTotalAmount', () => {
  const cases = [
    { fields: {}, result: 0 },
    { fields: { price_cents: 100, requested_quantity: 42 }, result: 4200 },
    {
      fields: {
        price_cents: 100,
        requested_quantity: 42,
        approved_quantity: 23
      },
      result: 2300
    },
    {
      fields: {
        price_cents: 100,
        requested_quantity: 42,
        approved_quantity: 23,
        order_quantity: 5
      },
      result: 500
    },
    {
      fields: {
        price_cents: 100,
        requested_quantity: 42,
        approved_quantity: 23,
        order_quantity: 0
      },
      result: 0
    },

    {
      fields: {
        price_cents: { value: 100 },
        requested_quantity: { value: 42 }
      },
      result: 4200
    },
    {
      fields: {
        price_cents: { value: 100 },
        requested_quantity: { value: 42 },
        approved_quantity: { value: 23 }
      },
      result: 2300
    },
    {
      fields: {
        price_cents: { value: 100 },
        requested_quantity: { value: 42 },
        approved_quantity: { value: 23 },
        order_quantity: { value: 5 }
      },
      result: 500
    },
    {
      fields: {
        price_cents: { value: 250000 },
        requested_quantity: { value: 30 },
        approved_quantity: { value: null },
        order_quantity: { value: null }
      },
      result: 7500000
    },
    {
      fields: {
        price_cents: { value: 250000 },
        requested_quantity: { value: 30 },
        approved_quantity: { value: 20 },
        order_quantity: { value: null }
      },
      result: 5000000
    },
    {
      fields: {
        price_cents: { value: 250000 },
        requested_quantity: { value: 30 },
        approved_quantity: { value: 20 },
        order_quantity: { value: 10 }
      },
      result: 2500000
    },
    {
      fields: {
        price_cents: { value: 250000 },
        requested_quantity: { value: 30 },
        approved_quantity: { value: 20 },
        order_quantity: { value: 0 }
      },
      result: 0
    }
  ]

  cases.forEach(({ fields, result }, i) =>
    test(`case ${i + 1}`, () =>
      expect(RequestTotalAmount(fields)).toEqual(result))
  )
})
