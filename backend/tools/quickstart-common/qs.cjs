// Thư viện chung cho script nghiệm thu quickstart ở profile demo (backend/tools/quickstart-<NNN>/run.cjs).
// Gọi API thật bằng tài khoản mẫu demo/V002_90; đọc, ghi dữ liệu điều kiện cần (mục 1 của quickstart) bằng tài khoản
// quản trị MySQL trên schema demo riêng. Biến: QS_BASE (mặc định http://localhost:8081/api/v1), QS_DB (mặc định
// elder_care_qs1). Không chạy trên schema elder_care (dữ liệu demo chung của máy).
const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const crypto = require('crypto');

const BASE = process.env.QS_BASE || 'http://localhost:8081/api/v1';
const DB = process.env.QS_DB || 'elder_care_qs1';
const PASSWORD = process.env.DEMO_PASSWORD || 'Demo2026!';
if (DB === 'elder_care') throw new Error('Không chạy script nghiệm thu trên schema elder_care');

const env = {};
for (const line of fs.readFileSync(path.join(__dirname, '..', '..', '.env'), 'utf8').split(/\r?\n/)) {
  const m = line.match(/^\s*([A-Z_]+)=(.*)$/);
  if (m) env[m[1]] = m[2];
}

/** Câu SQL trên schema demo; trả mảng dòng, mỗi dòng mảng chuỗi (NULL thành null). */
function sql(q) {
  const out = cp.execFileSync('mysql', ['-u', env.TEST_DB_ADMIN_USER, '--default-character-set=utf8mb4', '-N', '-B', DB, '-e', q], {
    env: { ...process.env, MYSQL_PWD: env.TEST_DB_ADMIN_PASSWORD }, encoding: 'utf8', maxBuffer: 1 << 28,
  });
  return out.split(/\r?\n/).filter(l => l.length).map(l => l.split('\t').map(v => (v === 'NULL' ? null : v)));
}
const one = q => { const r = sql(q); return r.length ? r[0][0] : null; };
const insertId = q => Number(one(q + '; SELECT LAST_INSERT_ID()'));
const s = v => (v === null || v === undefined ? 'NULL' : typeof v === 'number' ? String(v) : "'" + String(v).replace(/\\/g, '\\\\').replace(/'/g, "''") + "'");

const tokens = {};
const devices = {};
const channels = {};
const dev = u => (devices[u] ??= crypto.randomUUID());

async function login(username, { channel = 'STAFF_APP', password = PASSWORD, device } = {}) {
  if (device) devices[username] = device;
  channels[username] = channel;
  const r = await fetch(BASE + '/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Device-Id': dev(username) },
    body: JSON.stringify({ username, password, channel }),
  });
  const text = await r.text();
  let b; try { b = JSON.parse(text); } catch { b = text; }
  if (!b || !b.token) return { status: r.status, body: b };
  tokens[username] = b.token;
  return { status: r.status, body: b };
}

async function call(user, method, p, body, headers = {}) {
  const h = { 'X-Device-Id': dev(user), ...headers };
  if (tokens[user]) h.Authorization = 'Bearer ' + tokens[user];
  if (body !== undefined) h['Content-Type'] = 'application/json';
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) && !('Idempotency-Key' in headers)) h['Idempotency-Key'] = crypto.randomUUID();
  const t0 = performance.now();
  const r = await fetch(BASE + p, { method, headers: h, body: body === undefined ? undefined : JSON.stringify(body) });
  const buf = Buffer.from(await r.arrayBuffer());
  const ms = performance.now() - t0;
  const text = buf.toString('utf8');
  let json; try { json = text ? JSON.parse(text) : null; } catch { json = text; }
  return { status: r.status, etag: r.headers.get('etag'), body: json, ms, bytes: buf, headers: r.headers };
}
const get = (u, p) => call(u, 'GET', p);
const post = (u, p, body, headers) => call(u, 'POST', p, body ?? {}, headers);
const put = (u, p, body, headers) => call(u, 'PUT', p, body, headers);

const QLV = 'demo.quanly';
/** Đồng hồ giả lập; tua thì đăng nhập lại mọi tài khoản đã đăng nhập (tua làm phiên hết hạn, CFG-M15-10). */
async function clock(body = {}) {
  const r = await call(QLV, 'POST', '/testing/clock', body);
  if (r.status !== 200) throw new Error('clock ' + JSON.stringify(r.body));
  if (body.advanceBy || body.setTo) await relogin();
  return r.body;
}
async function relogin() {
  for (const u of Object.keys(tokens)) {
    const r = await login(u, { channel: channels[u] });
    if (!r.body?.token) delete tokens[u];
  }
}
const now = async () => (await call(QLV, 'GET', '/testing/clock')).body.now;
const runJobs = (...jobs) => clock({ runJobs: jobs });

// Kết quả từng bước
const results = [];
function check(step, label, ok, data) {
  results.push({ step, label, ok: ok ? 'ĐẠT' : 'KHÔNG ĐẠT' });
  console.log(`${ok ? 'ĐẠT' : 'KHÔNG ĐẠT'} ${step} ${label}${ok ? '' : ' :: ' + JSON.stringify(data, (k, v) => (k === 'bytes' || k === 'headers' ? undefined : v)).slice(0, 900)}`);
  return !!ok;
}
function manual(step, label) {
  results.push({ step, label, ok: 'CẦN XEM TAY' });
  console.log(`CẦN XEM TAY ${step} ${label}`);
}
function skip(step, label) {
  results.push({ step, label, ok: 'CHƯA CHẠY ĐƯỢC' });
  console.log(`CHƯA CHẠY ĐƯỢC ${step} ${label}`);
}
function summary(feature) {
  const count = k => results.filter(r => r.ok === k).length;
  console.log(`\n${feature}: ${count('ĐẠT')} ĐẠT, ${count('KHÔNG ĐẠT')} KHÔNG ĐẠT, ${count('CẦN XEM TAY')} cần xem tay, ${count('CHƯA CHẠY ĐƯỢC')} chưa chạy được`);
  try {
    fs.mkdirSync(`D:/eldercare-logs/qs-results/${DB}`, { recursive: true });
    fs.writeFileSync(`D:/eldercare-logs/qs-results/${DB}/qs-${feature}.json`, JSON.stringify(results, null, 1));
  } catch { /* log ngoài kho, không bắt buộc */ }
}
async function step(name, fn) {
  try { await fn(); } catch (e) { check(name, 'lỗi script', false, String(e.stack || e)); }
}

const stats = arr => {
  const a = [...arr].sort((x, y) => x - y);
  const q = p => a[Math.min(a.length - 1, Math.ceil(p * a.length) - 1)];
  return { n: a.length, median: Math.round(q(0.5)), p95: Math.round(q(0.95)), max: Math.round(a[a.length - 1]) };
};
const uid = () => crypto.randomUUID().slice(0, 8);
const day = (iso, plus = 0) => { const d = new Date(iso.slice(0, 10) + 'T00:00:00Z'); d.setUTCDate(d.getUTCDate() + plus); return d.toISOString().slice(0, 10); };
const errorCode = r => r.body?.errorCode;
const has = (r, text) => JSON.stringify(r.body ?? '').includes(text);

module.exports = { BASE, DB, sql, one, insertId, s, login, call, get, post, put, clock, now, runJobs, relogin, tokens, check, manual, skip, summary, step, results, stats, uid, day, errorCode, has, QLV };
