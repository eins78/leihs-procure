import React, { Fragment as F } from 'react'
import PropTypes from 'prop-types'
import f from 'lodash'
import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'

// import { RequestTotalAmount as TotalAmount } from '../components/decorators'
import { Alert, RoutedStatus } from '../components/Bootstrap'
// import Icon from '../components/Icons'
import RequestForm from '../components/RequestForm'
import * as Fragments from '../graphql-fragments'

const REQUEST_EDIT_QUERY = gql`
  query RequestForEdit($id: [ID!]!) {
    requests(id: $id) {
      ...RequestFieldsForEdit
    }
    # for selecting a new category:
    main_categories {
      id
      name
      categories {
        id
        name
      }
    }
    # for selecting a budget period:
    budget_periods {
      id
      name
    }
  }
  ${Fragments.RequestFieldsForEdit}
`

const UPDATE_REQUEST_MUTATION = gql`
  mutation updateRequest($requestData: RequestInput) {
    request(input_data: $requestData) {
      ...RequestFieldsForEdit
    }
  }
  ${Fragments.RequestFieldsForEdit}
`

class RequestEdit extends React.Component {
  state = { selectNewCategory: false, selectBudgetPeriod: false }

  onSelectNewRequestCategory = () => {
    this.setState(s => ({
      selectNewCategory: !s.selectNewCategory
    }))
  }
  onChangeRequestCategory = newCategory => {
    const requestId = this.props.requestId
    if (!requestId || !newCategory.id) throw new Error()
    window.confirm(`Move to category "${newCategory.name}"?`) &&
      this.props.doChangeRequestCategory(requestId, newCategory.id)
  }

  onSelectNewBudgetPeriod = () => {
    this.setState(s => ({
      selectBudgetPeriod: !s.selectBudgetPeriod
    }))
  }
  onChangeBudgetPeriod = newBudgetPeriod => {
    const requestId = this.props.requestId
    if (!requestId || !newBudgetPeriod.id) throw new Error()
    window.confirm(`Move to Budget Period "${newBudgetPeriod.name}"?`) &&
      this.props.doChangeBudgetPeriod(requestId, newBudgetPeriod.id)
  }

  render({ requestId, onCancel, className, ...props } = this.props) {
    if (!f.isUUID(requestId)) {
      return (
        <RoutedStatus code={400}>
          <Alert color="danger">
            The Request id <samp>{requestId}</samp> is not valid!
          </Alert>
        </RoutedStatus>
      )
    }

    return (
      <Query
        fetchPolicy="network-only"
        query={REQUEST_EDIT_QUERY}
        variables={{ id: [requestId] }}
      >
        {({ error, loading, data }) => {
          if (loading) return <Loading />
          if (error) return <ErrorPanel error={error} data={data} />

          const request = data.requests[0]

          if (!request) {
            return (
              <RoutedStatus code={404}>
                <Alert color="danger">
                  No Request with id <samp>{requestId}</samp> found!
                </Alert>
              </RoutedStatus>
            )
          }

          return (
            <Mutation mutation={UPDATE_REQUEST_MUTATION}>
              {(mutate, mutReq) => {
                if (mutReq.loading) return <Loading />
                if (mutReq.error)
                  return <ErrorPanel error={mutReq.error} data={mutReq.data} />
                return (
                  <F>
                    <RequestForm
                      className={className}
                      request={request}
                      categories={data.main_categories}
                      budgetPeriods={data.budget_periods}
                      onCancel={onCancel}
                      onSubmit={fields =>
                        mutate({
                          variables: {
                            requestData: {
                              id: request.id,
                              ...requestDataFromFields(request, fields)
                            }
                          }
                        })
                      }
                      // action delete
                      doDeleteRequest={
                        !!props.doDeleteRequest &&
                        (e => props.doDeleteRequest(request))
                      }
                      // action move category
                      onSelectNewRequestCategory={
                        this.onSelectNewRequestCategory
                      }
                      isSelectingNewCategory={this.state.selectNewCategory}
                      doChangeRequestCategory={
                        !!props.doChangeRequestCategory &&
                        this.onChangeRequestCategory
                      }
                      // action move budget period
                      onSelectNewBudgetPeriod={this.onSelectNewBudgetPeriod}
                      isSelectingNewBudgetPeriod={this.state.selectBudgetPeriod}
                      doChangeBudgetPeriod={
                        props.doChangeBudgetPeriod && this.onChangeBudgetPeriod
                      }
                    />
                    {window.isDebug && (
                      <pre>{JSON.stringify({ request }, 0, 2)}</pre>
                    )}
                  </F>
                )
              }}
            </Mutation>
          )
        }}
      </Query>
    )
  }
}

export default RequestEdit

RequestEdit.propTypes = {
  doChangeBudgetPeriod: PropTypes.func,
  doChangeRequestCategory: PropTypes.func,
  doDeleteRequest: PropTypes.func
}

export const valueIfWritable = (fields, requestData, reqKey, fieldKey) => {
  fieldKey = fieldKey || reqKey

  if (!f.get(requestData, reqKey)) {
    // eslint-disable-next-line no-debugger
    debugger
  }

  if (f.get(requestData, reqKey).write) {
    return { [fieldKey]: f.get(fields, fieldKey) }
  }
}

export const requestDataFromFields = (request, fields) => {
  const boolify = (key, val) =>
    !val ? false : !val[key] ? null : val[key] === key

  const supplier = valueIfWritable(fields, request, 'supplier')
  const room = valueIfWritable(fields, request, 'room').room
  const requestData = {
    ...valueIfWritable(fields, request, 'article_name'),
    ...valueIfWritable(fields, request, 'article_number'),
    ...valueIfWritable(fields, request, 'receiver'),
    ...valueIfWritable(fields, request, 'price_cents'),

    ...valueIfWritable(fields, request, 'requested_quantity'),
    ...valueIfWritable(fields, request, 'approved_quantity'),
    ...valueIfWritable(fields, request, 'order_quantity'),

    ...valueIfWritable(fields, request, 'motivation'),
    ...valueIfWritable(fields, request, 'priority'),
    ...valueIfWritable(fields, request, 'inspector_priority'),
    ...valueIfWritable(fields, request, 'inspection_comment'),

    ...valueIfWritable(fields, request, 'accounting_type'),
    ...valueIfWritable(fields, request, 'internal_order_number'),

    // FIXME: dont hardcode fallback
    replacement:
      boolify(
        'replacement',
        valueIfWritable(fields, request, 'replacement').replacement
      ) || false,

    attachments: f.map(
      valueIfWritable(fields, request, 'attachments').attachments,
      o => ({ ...f.pick(o, 'id', '__typename'), to_delete: !!o.toDelete })
    ),

    ...(supplier
      ? { supplier: supplier.supplier.id }
      : valueIfWritable(fields, request, 'supplier_name')),

    // NOTE: no building, just room!
    ...(!room ? null : { room })
  }

  return requestData
}
