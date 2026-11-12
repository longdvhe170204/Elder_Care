// Thư viện chung cho script nghiệm thu quickstart 007, 009, 010, 011 trên profile demo (schema elder_care_qs3, cổng 8083).
// Gọi API thật; đọc cơ sở dữ liệu (chỉ SELECT) bằng mysql CLI khi API không trả trường cần kiểm, ghi rõ ở từng bước.
const crypto = require('crypto');
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const BASE = process.env.QS_BASE || 'http://localhost:8083/api/v1';
const SCHEMA = process.env.QS_SCHEMA || 'elder_care_qs3';
const PASSWORD = process.env.DEMO_PASSWORD || 'Demo2026!'; // mật khẩu demo chung ở demo/V002_90
const tokens = {};
const devices = {};
const dev = (u) => (devices[u] ??= crypto.randomUUID());

function env() {
  const file = path.join(__dirname, '..', '..', '.env');
  const out = {};
  if (fs.existsSync(file))
    for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
      const m = line.match(/^([A-Z_]+)=(.*)$/);
      if (m) out[m[1]] = m[2];
    }
  return { ...out, ...process.env };
}

// SELECT trên schema demo bằng tài khoản quản trị test (TEST_DB_ADMIN_*); trả mảng dòng (cột theo tên).
function sql(query, { write = false, db = SCHEMA } = {}) {
  const e = env();
  if (!write && !/^\s*(SELECT|WITH|SHOW)/i.test(query)) throw new Error('sql(): chỉ SELECT');
  const args = ['-u' + e.TEST_DB_ADMIN_USER, '--default-character-set=utf8mb4', '-B'];
  if (db) args.push(db);
  const outText = execFileSync('mysql', args,
    { input: query, env: { ...process.env, MYSQL_PWD: e.TEST_DB_ADMIN_PASSWORD }, encoding: 'utf8', maxBuffer: 64 << 20 });
  const lines = outText.split(/\r?\n/).filter((l) => l.length);
  if (!lines.length) return [];
  const cols = lines[0].split('\t');
  return lines.slice(1).map((l) => Object.fromEntries(l.split('\t').map((v, i) => [cols[i], v === 'NULL' ? null : v])));
}

async function login(username, channel = 'STAFF_APP') {
  const r = await fetch(BASE + '/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Device-Id': dev(username) },
    body: JSON.stringify({ username, password: PASSWORD, channel }),
  });
  const b = await r.json();
  if (!b.token) throw new Error('login ' + username + ': ' + JSON.stringify(b));
  tokens[username] = b.token;
  return b;
}

async function call(user, method, p, body, headers = {}) {
  const h = { 'X-Device-Id': dev(user), ...headers };
  if (tokens[user]) h.Authorization = 'Bearer ' + tokens[user];
  if (body !== undefined) h['Content-Type'] = 'application/json';
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) && !('Idempotency-Key' in h)) h['Idempotency-Key'] = crypto.randomUUID();
  const t0 = performance.now();
  const r = await fetch(BASE + p, { method, headers: h, body: body === undefined ? undefined : JSON.stringify(body) });
  const ms = performance.now() - t0;
  const buf = Buffer.from(await r.arrayBuffer());
  const text = buf.toString('utf8');
  let json;
  try { json = text ? JSON.parse(text) : null; } catch { json = text; }
  return { status: r.status, etag: r.headers.get('etag'), body: json, bytes: buf, ms, type: r.headers.get('content-type') };
}
const get = (u, p) => call(u, 'GET', p);
const post = (u, p, b, h) => call(u, 'POST', p, b ?? {}, h);
// Lệnh cần If-Match: đọc ETag ở đường dẫn đọc rồi gửi
async function postIfMatch(u, readPath, p, b) {
  const cur = await get(u, readPath);
  return call(u, 'POST', p, b ?? {}, cur.etag ? { 'If-Match': cur.etag } : {});
}

// Kết quả từng bước: ĐẠT / KHÔNG ĐẠT / CẦN XEM TAY (lý do)
const results = [];
function check(step, label, ok, data) {
  results.push({ step, label, res: ok ? 'ĐẠT' : 'KHÔNG ĐẠT', data: ok ? undefined : JSON.parse(JSON.stringify(data ?? null, (k, v) => (k === 'bytes' ? undefined : v))) });
  console.log(`${ok ? 'ĐẠT      ' : 'KHÔNG ĐẠT'} ${step} ${label}${ok ? '' : ' :: ' + JSON.stringify(data, (k, v) => (k === 'bytes' ? undefined : v)).slice(0, 900)}`);
  return !!ok;
}
function manual(step, label, why = '') {
  results.push({ step, label, res: 'CẦN XEM TAY', why });
  console.log(`CẦN XEM TAY ${step} ${label}${why ? ' :: ' + why : ''}`);
}
const skip = manual;
// Chạy một bước, lỗi ngoài dự kiến (exception) ghi KHÔNG ĐẠT rồi đi tiếp
async function step(id, fn) {
  try { await fn(); } catch (e) { check(id, 'lỗi khi chạy script', false, String(e && e.stack || e).slice(0, 500)); }
}
function summary(feature) {
  const count = {};
  for (const r of results) count[r.res] = (count[r.res] || 0) + 1;
  console.log(`\n== ${feature}: ` + Object.entries(count).map(([k, v]) => `${k} ${v}`).join(', '));
  const out = path.join('D:/eldercare-logs', `qs-${feature}-result.json`);
  try { fs.writeFileSync(out, JSON.stringify(results, null, 1)); } catch {}
  return count;
}

// Đồng hồ giả lập; tua giờ làm phiên hết hạn nên đăng nhập lại người dùng đã đăng nhập
async function clock(body, asUser = 'demo.quanly') {
  const r = await call(asUser, 'POST', '/testing/clock', body);
  if (r.status >= 300) throw new Error('clock ' + JSON.stringify(r));
  if (body.advanceBy || body.setTo) for (const u of Object.keys(tokens)) await relogin(u);
  return r.body;
}
const loginChannel = {};
async function relogin(u) { return login(u, loginChannel[u] || 'STAFF_APP'); }
async function loginAs(u, channel = 'STAFF_APP') { loginChannel[u] = channel; return login(u, channel); }
const runJobs = (...jobs) => clock({ runJobs: jobs });
const now = async () => (await get('demo.quanly', '/testing/clock')).body.now;

// Đọc hết các trang của danh sách có nextCursor
async function all(u, p) {
  const items = [];
  let cursor = null;
  do {
    const sep = p.includes('?') ? '&' : '?';
    const r = await get(u, p + (cursor ? sep + 'cursor=' + encodeURIComponent(cursor) : ''));
    if (r.status !== 200) return { status: r.status, items, body: r.body };
    const b = r.body;
    items.push(...(Array.isArray(b) ? b : b.items || []));
    cursor = Array.isArray(b) ? null : b.nextCursor;
  } while (cursor);
  return { status: 200, items };
}
const notes = (u) => all(u, '/me/notifications');
const median = (a) => { const s = [...a].sort((x, y) => x - y); return s[Math.floor((s.length - 1) / 2)]; };
const p95 = (a) => { const s = [...a].sort((x, y) => x - y); return s[Math.min(s.length - 1, Math.ceil(s.length * 0.95) - 1)]; };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const j = (x) => JSON.stringify(x);

module.exports = { BASE, SCHEMA, sql, login, loginAs, call, get, post, postIfMatch, check, skip, manual, step, summary, clock, runJobs, now, all, notes, median, p95, sleep, j, tokens, results };
