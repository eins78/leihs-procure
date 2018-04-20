import React from 'react'
import ReactDOM from 'react-dom'
import f from 'lodash'
import { Switch, Route } from 'react-router-dom'
import { BrowserRouter } from 'react-router-dom'
import ApolloClient from 'apollo-boost'
import { ApolloProvider } from 'react-apollo'

// webpack: inject styles
import './styles/index.css'

// import registerServiceWorker from './registerServiceWorker'
import App from './components/App'

// no router, just 1 page:
import RequestsIndex from './pages/RequestsIndex'
import AdminUsers from './pages/AdminUsers'

const client = new ApolloClient({
  uri: '/procure/graphql'
})

const Root = () => (
  <ApolloProvider client={client}>
    <BrowserRouter>
      {/* <React.StrictMode> */}
      <App>
        <Switch>
          <Route exact path="/" component={RequestsIndex} />
          <Route exact path="/admin/users" component={AdminUsers} />
          <Route component={() => '404'} />
        </Switch>
      </App>
      {/* </React.StrictMode> */}
    </BrowserRouter>
  </ApolloProvider>
)
ReactDOM.render(<Root />, document.getElementById('root'))

// registerServiceWorker() // not yet…

// dev helpers
window.f = f
window.debugObj = obj => {
  console.debug(obj) // eslint-disable-line no-console
  return obj
}
