import { vi } from 'vitest'

// Giả fetch theo phương thức và đường dẫn cho test màn gọi nhiều API; route không khai báo thì treo (đang tải).
export function json(status, body, headers = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json', ...headers },
  })
}

export function problem(status, errorCode, extra = {}) {
  const titles = {
    403: 'Forbidden',
    404: 'Not Found',
    409: 'Conflict',
    412: 'Precondition Failed',
    422: 'Unprocessable Content',
  }
  return json(status, { type: 'about:blank', title: titles[status], status, errorCode, ...extra })
}

export function mockRoutes() {
  const routes = []
  const fetchMock = vi.fn((url, init = {}) => {
    const parsed = new URL(url, 'http://localhost')
    const method = init.method ?? 'GET'
    const route = routes.find((r) => r.method === method && r.match(parsed))
    if (!route) return new Promise(() => {})
    const response = route.respond(parsed, init)
    return Promise.resolve(typeof response === 'function' ? response() : response)
  })
  vi.stubGlobal('fetch', fetchMock)
  return {
    fetchMock,
    // path: chuỗi so khớp đúng pathname (sau /api/v1) hoặc hàm (URL) => boolean
    on(method, path, respond) {
      const match = typeof path === 'function' ? path : (u) => u.pathname === `/api/v1${path}`
      routes.unshift({ method, match, respond: typeof respond === 'function' ? respond : () => respond.clone() })
    },
    calls(method, path) {
      return fetchMock.mock.calls.filter(([url, init = {}]) => {
        const u = new URL(url, 'http://localhost')
        return (init.method ?? 'GET') === method && u.pathname === `/api/v1${path}`
      })
    },
  }
}
