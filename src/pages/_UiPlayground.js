import React, { Fragment as F } from 'react'
import { Route, Switch, Redirect, Link } from 'react-router-dom'
// import cx from 'classnames'
import f from 'lodash'

// import t from '../locale/translate'
// import Icon from '../components/Icons'
import {
  // Div,
  Row,
  Col,
  // Button,
  // FormGroup,
  // InputText,
  FormField
} from '../components/Bootstrap'
import ControlledForm from '../components/Bootstrap/ControlledForm'
import { MainWithSidebar } from '../components/Layout'

// # DATA

const PAGES = [
  {
    id: 'controlled-form',
    title: 'Controlled Form',
    content: (
      <F>
        <code>{'<ControlledForm/>'}</code>
        <hr />
        <ControlledForm idPrefix="mock-form" values={{ foo: '', bar: '' }}>
          {({ fields, formPropsFor }) => {
            return (
              <F>
                <form
                  id="mock-form"
                  onSubmit={e => {
                    e.preventDefault()
                    window.alert(JSON.stringify(fields, 0, 2))
                  }}
                >
                  <Row>
                    <Col>
                      <FormField label="foo" {...formPropsFor('foo')} />
                      <FormField label="bar" {...formPropsFor('bar')} />
                    </Col>
                  </Row>
                </form>
                <pre>
                  <code>{JSON.stringify(fields, 0, 2)}</code>
                </pre>
              </F>
            )
          }}
        </ControlledForm>
      </F>
    )
  },
  {
    id: 'multiselect',
    title: 'MultiSelect',
    content: ''
  }
]

// # PAGE
//
const UiPlayground = ({ match, location }) => {
  const baseUrl = match.url
  const flashMsg = f.get(location, 'state.flash')
  return (
    <MainWithSidebar sidebar={<TableofContents baseUrl={baseUrl} />}>
      {!!flashMsg && (
        <div className="alert alert-warning" role="alert">
          {flashMsg}
        </div>
      )}

      <Switch>
        <Route
          path={`${match.url}/:pageId`}
          render={p => <PageById {...p} baseUrl={baseUrl} />}
        />
        <Route
          exact
          path={match.url}
          render={() => 'select a page from the menu'}
        />
      </Switch>
    </MainWithSidebar>
  )
}

export default UiPlayground

// # PARTIALS

const titleOrById = (title, id) => title || String(id).toUpperCase()

const TableofContents = ({ baseUrl }) => (
  <ul className="p-2 nav flex-md-column">
    {PAGES.map(({ id, title }) => (
      <li key={id} className="nav-item">
        <Link className="nav-link " key={id} to={`${baseUrl}/${id}`}>
          {titleOrById(title, id)}
        </Link>
      </li>
    ))}
  </ul>
)

const PageById = ({ match, baseUrl }) => {
  const { pageId } = match.params
  const page = f.find(PAGES, { id: pageId })
  if (!page) {
    return (
      <Redirect
        to={{
          pathname: baseUrl,
          state: { flash: `The page ${pageId} doesn't exist!` }
        }}
      />
    )
  }
  return (
    <div>
      <h3>{titleOrById(page.title, pageId)}</h3>
      {page.content}
    </div>
  )
}
