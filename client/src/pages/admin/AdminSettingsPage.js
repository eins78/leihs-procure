import React from 'react'
// import cx from 'classnames'
import f from 'lodash'

import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import t from '../../locale/translate'
import Icon from '../../components/Icons'
import { Button, StatefulForm, FormField } from '../../components/Bootstrap'
import Loading from '../../components/Loading'
import { ErrorPanel } from '../../components/Error'
import { MainWithSidebar } from '../../components/Layout'

const mutationErrorHandler = err => {
  // not much we can do on backend error
  // eslint-disable-next-line no-console
  console.error(err)
  window.confirm('Error! ' + err) && window.location.reload()
}

// # DATA & ACTIONS
//
const ADMIN_SETTINGS_FRAGMENTS = {
  props: gql`
    fragment AdminSettingsProps on ProcurementSettings {
      contact_url
      inspection_comments
    }
  `
}

const ADMIN_SETTINGS_PAGE_QUERY = gql`
  query AdminSettings {
    settings {
      ...AdminSettingsProps
    }
  }
  ${ADMIN_SETTINGS_FRAGMENTS.props}
`

const ADMIN_UPDATE_SETTINGS_MUTATION = gql`
  mutation updateSettings($settings: ProcurementSettingsInput) {
    settings(input_data: $settings) {
      ...AdminSettingsProps
    }
  }
  ${ADMIN_SETTINGS_FRAGMENTS.props}
`

const updateSettings = {
  mutation: {
    mutation: ADMIN_UPDATE_SETTINGS_MUTATION,
    onError: mutationErrorHandler,
    update: (cache, { data: { settings } }) => {
      // update the internal cache with the new data we received.
      cache.writeQuery({ query: ADMIN_SETTINGS_PAGE_QUERY, data: { settings } })
    }
  },
  doUpdate: (mutate, fields) => {
    const settings = {
      ...fields,
      inspection_comments: fields.inspection_comments
        .split('\n')
        .filter(l => !f.isEmpty(l))
    }

    mutate({ variables: { settings } })
  }
}

// # PAGE
//
class AdminSettingsPage extends React.Component {
  state = { formKey: Date.now() }
  render() {
    return (
      <Mutation
        {...updateSettings.mutation}
        onCompleted={() => this.setState({ formKey: Date.now() })}
      >
        {(mutate, info) => (
          <Query query={ADMIN_SETTINGS_PAGE_QUERY}>
            {({ loading, error, data }) => {
              if (loading) return <Loading />
              if (error) return <ErrorPanel error={error} data={data} />

              const settings = {
                contact_url: data.settings.contact_url,
                inspection_comments: data.settings.inspection_comments.join(
                  '\n\n'
                )
              }

              return (
                <MainWithSidebar>
                  <h1>Einstellungen</h1>
                  <SettingsTable
                    settings={settings}
                    updateAction={fields =>
                      updateSettings.doUpdate(mutate, fields)
                    }
                    key={this.state.formKey}
                  />
                </MainWithSidebar>
              )
            }}
          </Query>
        )}
      </Mutation>
    )
  }
}

export default AdminSettingsPage

// # VIEW PARTIALS
//

const SettingsTable = ({ settings, updateAction }) => (
  <StatefulForm idPrefix="settings" values={settings}>
    {({ fields, formPropsFor, getValue, setValue }) => {
      const onSave = () => updateAction(fields)

      return (
        <React.Fragment>
          <form>
            <FormField label="contact_url" {...formPropsFor('contact_url')} />
            <FormField
              type="textarea"
              label="inspection_comments"
              {...formPropsFor('inspection_comments')}
            />
          </form>

          <Button color="primary" onClick={onSave}>
            <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
          </Button>

          {window.isDebug && <pre>{JSON.stringify(fields, 0, 2)}</pre>}
        </React.Fragment>
      )
    }}
  </StatefulForm>
)