// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

const BASE_URL = '/api/v1'

// Lỗi theo RFC 9457: problem có errorCode, unmetConditions, currentValue (research R-03)
export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.title || `HTTP ${status}`)
    this.status = status
    this.problem = problem
  }
}

// params: { path, query, body, headers } — path thay {tên} trong đường dẫn
export async function request(method, template, params = {}) {
  const { path = {}, query, body, headers = {} } = params
  let url = BASE_URL + template.replace(/\{(\w+)\}/g, (_, name) => {
    if (path[name] === undefined) throw new Error(`Thiếu tham số đường dẫn ${name}`)
    return encodeURIComponent(path[name])
  })
  if (query) {
    const search = new URLSearchParams()
    for (const [key, value] of Object.entries(query)) {
      if (value === undefined || value === null) continue
      for (const item of Array.isArray(value) ? value : [value]) search.append(key, item)
    }
    const text = search.toString()
    if (text) url += `?${text}`
  }
  const response = await fetch(url, {
    method,
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...(body !== undefined && { 'Content-Type': 'application/json' }),
      // 002 research A-02: lệnh ghi qua cookie phiên phải kèm header này (chặn CSRF)
      ...(!['GET', 'HEAD'].includes(method) && { 'X-Requested-From': 'web' }),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const type = response.headers.get('Content-Type') || ''
  const data = type.includes('json') ? await response.json() : null
  if (!response.ok) throw new ApiError(response.status, data)
  return { data, etag: response.headers.get('ETag'), status: response.status }
}
