// Sinh client API của web từ specs/*/contracts/api-*.yaml (000 T010, research R-03).
// Chỉ dùng hợp đồng đã dịch tên tiếng Anh: thêm mã feature vào TRANSLATED khi hợp đồng của feature đó được duyệt.
// Chạy: npm run gen:api. Không sửa tay file trong src/api/.
import { mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { parse } from 'yaml'

const TRANSLATED = ['000', '001', '002', '003', '009', '016']

const root = fileURLToPath(new URL('../..', import.meta.url))
const specsDir = join(root, 'specs')
const outDir = join(root, 'frontend', 'src', 'api')

const HEADER = '// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.\n'

const CLIENT = `${HEADER}
const BASE_URL = '/api/v1'

// Lỗi theo RFC 9457: problem có errorCode, unmetConditions, currentValue (research R-03)
export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.title || \`HTTP \${status}\`)
    this.status = status
    this.problem = problem
  }
}

// params: { path, query, body, headers } — path thay {tên} trong đường dẫn
export async function request(method, template, params = {}) {
  const { path = {}, query, body, headers = {} } = params
  let url = BASE_URL + template.replace(/\\{(\\w+)\\}/g, (_, name) => {
    if (path[name] === undefined) throw new Error(\`Thiếu tham số đường dẫn \${name}\`)
    return encodeURIComponent(path[name])
  })
  if (query) {
    const search = new URLSearchParams()
    for (const [key, value] of Object.entries(query)) {
      if (value === undefined || value === null) continue
      for (const item of Array.isArray(value) ? value : [value]) search.append(key, item)
    }
    const text = search.toString()
    if (text) url += \`?\${text}\`
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
`

const pascal = (s) => s.replace(/(^|[-_])(\w)/g, (_, __, c) => c.toUpperCase())

// GET /parameters/{code}/history → getParametersByCodeHistory
function functionName(method, path) {
  const parts = path
    .split('/')
    .filter(Boolean)
    .map((seg) => (seg.startsWith('{') ? `By${pascal(seg.slice(1, -1))}` : pascal(seg)))
  return method + parts.join('')
}

function generate(feature, file) {
  const doc = parse(readFileSync(file, 'utf8'))
  const lines = [HEADER, `// Nguồn: specs/${feature}/contracts (${doc.info.title})`, "import { request } from './client.js'", '']
  for (const [path, item] of Object.entries(doc.paths)) {
    for (const [method, op] of Object.entries(item)) {
      if (!['get', 'post', 'put', 'patch', 'delete'].includes(method)) continue
      // 003 duyệt từng phase; chỉ sinh client cho operation có tên tiếng Anh đã được duyệt.
      if (feature.startsWith('003-') && !op['x-implementation-phase']) continue
      const refs = [...(op['x-fr'] || []), ...(op['x-br'] || [])]
      lines.push(`/** ${op.summary}${refs.length ? ` (${refs.join(', ')})` : ''} */`)
      lines.push(
        `export const ${functionName(method, path)} = (params) => request('${method.toUpperCase()}', '${path}', params)`,
      )
      lines.push('')
    }
  }
  return lines.join('\n')
}

rmSync(outDir, { recursive: true, force: true })
mkdirSync(outDir, { recursive: true })
writeFileSync(join(outDir, 'client.js'), CLIENT)
const exports = []
for (const dir of readdirSync(specsDir).sort()) {
  const code = dir.slice(0, 3)
  if (!TRANSLATED.includes(code)) continue
  const contracts = join(specsDir, dir, 'contracts')
  for (const name of readdirSync(contracts).filter((n) => /^api-\d{3}\.yaml$/.test(n))) {
    const target = `api${code}.js`
    writeFileSync(join(outDir, target), generate(dir, join(contracts, name)))
    exports.push(`export * from './${target}'`)
  }
}
writeFileSync(join(outDir, 'index.js'), `${HEADER}export { ApiError, request } from './client.js'\n${exports.join('\n')}\n`)
console.log(`Đã sinh ${exports.length} file vào src/api/`)
