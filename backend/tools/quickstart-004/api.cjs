// Gọi API của backend demo (QuickstartDemoApplication, cổng QS_PORT, mặc định 8082) bằng tài khoản mẫu demo/V002_90.
// Dùng chung cho script quickstart 004, 005, 008, 015. Mẫu: tools/quickstart-000/api.cjs.
const crypto = require('crypto');
const BASE = `http://localhost:${process.env.QS_PORT || 8082}/api/v1`;
const PASSWORD = process.env.DEMO_PASSWORD || 'Demo2026!';
const tokens = {};
const passwords = {};
const devices = {};
const dev = (u) => (devices[u] ??= crypto.randomUUID());

async function login(username, password = passwords[username] || PASSWORD, channel = 'STAFF_APP') {
  const r = await fetch(BASE + '/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Device-Id': dev(username) },
    body: JSON.stringify({ username, password, channel }),
  });
  const b = await r.json();
  if (!b.token) throw new Error('login ' + username + ': ' + JSON.stringify(b));
  tokens[username] = b.token;
  passwords[username] = password;
  return b;
}

async function call(user, method, path, body, headers = {}) {
  const h = { 'X-Device-Id': dev(user), ...headers };
  if (tokens[user]) h.Authorization = 'Bearer ' + tokens[user];
  if (body !== undefined) h['Content-Type'] = 'application/json';
  if (['POST', 'PUT', 'PATCH'].includes(method)) h['Idempotency-Key'] ??= crypto.randomUUID();
  const r = await fetch(BASE + path, { method, headers: h, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await r.text();
  let json;
  try { json = text ? JSON.parse(text) : null; } catch { json = text; }
  return { status: r.status, etag: r.headers.get('etag'), body: json };
}

async function upload(user, name = 'scan.pdf', type = 'application/pdf', purpose) {
  const fd = new FormData();
  fd.append('file', new Blob([Buffer.from('%PDF-1.4 quickstart ' + crypto.randomUUID())], { type }), name);
  if (purpose) fd.append('purpose', purpose);
  const r = await fetch(BASE + '/files', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + tokens[user], 'X-Device-Id': dev(user), 'Idempotency-Key': crypto.randomUUID() },
    body: fd,
  });
  const text = await r.text();
  let json; try { json = JSON.parse(text); } catch { json = text; }
  return { status: r.status, body: json };
}

// Kết quả từng bước: ĐẠT / KHÔNG ĐẠT / CẦN XEM TAY / KHÔNG CHẠY ĐƯỢC
const results = [];
function check(step, label, ok, data) {
  results.push({ step, label, verdict: ok ? 'ĐẠT' : 'KHÔNG ĐẠT', data: ok ? undefined : data });
  console.log(`${ok ? 'ĐẠT      ' : 'KHÔNG ĐẠT'} ${step} ${label}${ok ? '' : ' :: ' + JSON.stringify(data).slice(0, 2500)}`);
  return !!ok;
}
function note(step, verdict, label) {
  results.push({ step, label, verdict });
  console.log(`${verdict.padEnd(9)} ${step} ${label}`);
}
function summary() {
  const by = {};
  for (const r of results) by[r.verdict] = (by[r.verdict] || 0) + 1;
  console.log('\nTổng:', JSON.stringify(by));
  return by;
}

module.exports = { BASE, login, call, upload, tokens, check, note, results, summary, dev };
