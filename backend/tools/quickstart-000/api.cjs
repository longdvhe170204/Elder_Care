// Gọi API của backend demo (QuickstartDemoApplication) bằng tài khoản mẫu của demo/V002_90 (000 T103).
const BASE = 'http://localhost:8080/api/v1';
const PASSWORD = process.env.DEMO_PASSWORD || 'Demo2026!'; // mật khẩu demo chung ở demo/V002_90
const tokens = {};
const devices = {};
const dev = u => (devices[u] ??= require('crypto').randomUUID());

async function login(username) {
  const r = await fetch(BASE + '/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Device-Id': dev(username) },
    body: JSON.stringify({ username, password: PASSWORD, channel: 'STAFF_APP' }),
  });
  const b = await r.json();
  if (!b.token) throw new Error('login ' + username + ': ' + JSON.stringify(b));
  tokens[username] = b.token;
  return b;
}

async function call(user, method, path, body, headers = {}) {
  const h = { Authorization: 'Bearer ' + tokens[user], 'X-Device-Id': dev(user), ...headers };
  if (body !== undefined) h['Content-Type'] = 'application/json';
  if (['POST', 'PUT', 'PATCH'].includes(method)) h['Idempotency-Key'] ??= require('crypto').randomUUID();
  const r = await fetch(BASE + path, { method, headers: h, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await r.text();
  let json; try { json = text ? JSON.parse(text) : null; } catch { json = text; }
  return { status: r.status, etag: r.headers.get('etag'), body: json };
}

module.exports = { login, call, tokens };

