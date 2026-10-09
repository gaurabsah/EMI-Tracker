const STORAGE_KEY = 'emi.auth'

export function loadAuth() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY))
  } catch {
    return null
  }
}

export function saveAuth(auth) {
  if (auth) localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
  else localStorage.removeItem(STORAGE_KEY)
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

let onUnauthorized = () => {}

export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

async function request(method, path, body, { authenticated = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const auth = loadAuth()
  if (authenticated && auth) headers.Authorization = `Bearer ${auth.token}`

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (res.status === 401 && authenticated) onUnauthorized()
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      message = (await res.json()).error || message
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(res.status, message)
  }
  return res.status === 204 ? null : res.json()
}

export const api = {
  login: (email, password) => request('POST', '/auth/login', { email, password }, { authenticated: false }),

  listCompanies: () => request('GET', '/admin/companies'),
  createCompany: (body) => request('POST', '/admin/companies', body),
  setCompanyActive: (id, active) => request('PATCH', `/admin/companies/${id}/active`, { active }),

  listPlans: () => request('GET', '/plans'),
  createPlan: (body) => request('POST', '/plans', body),

  listCustomers: () => request('GET', '/customers'),
  createCustomer: (body) => request('POST', '/customers', body),

  listPurchases: () => request('GET', '/purchases'),
  getPurchase: (id) => request('GET', `/purchases/${id}`),
  createPurchase: (body) => request('POST', '/purchases', body),
  payInstalment: (id) => request('POST', `/instalments/${id}/pay`, {}),

  listMyPurchases: () => request('GET', '/me/purchases'),
  getMyPurchase: (id) => request('GET', `/me/purchases/${id}`),
}
