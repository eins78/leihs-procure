import ApolloClient from 'apollo-boost'

const CSRF_COOKIE_NAME = 'leihs-anti-csrf-token'

// TODO: reenable dev fake auth
const isDev = false
// const isDev = process.env.NODE_ENV !== 'production'

// DEV FAKE AUTH
const USER_IDS = {
  mfa: '7da6733c-c819-5613-8cad-2a40f51c90da',
  gasser: 'f721d6b7-8275-5ee0-b225-aa7c13781f45'
}

window.LEIHS_DEV_CURRENT_USER_ID = USER_IDS.gasser

const buildAuthHeaders = () =>
  isDev
    ? { FakeAuthorization: window.LEIHS_DEV_CURRENT_USER_ID }
    : { 'x-csrf-token': getCSRFToken(document.cookie, CSRF_COOKIE_NAME) }

export const apolloClient = new ApolloClient({
  uri: '/procure/graphql',
  credentials: isDev ? 'omit' : 'same-origin', // send the cookie(s)
  request: operation => operation.setContext({ headers: buildAuthHeaders() })
})

const getCSRFToken = (cookies, name) =>
  (cookies || '')
    .split(';')
    .map(s => s.trim())
    .replace(`${name}=`, '')
