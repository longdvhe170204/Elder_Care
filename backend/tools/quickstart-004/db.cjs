// Truy vấn trực tiếp schema demo bằng mysql CLI: query() bằng tài khoản quản trị (đọc để kiểm), asApp() bằng tài khoản
// ứng dụng eldercare_app (thử sửa trực tiếp, mong bị chặn). Biến lấy từ backend/.env; schema ở QS_SCHEMA (mặc định
// elder_care_qs2, không bao giờ elder_care).
const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const env = {};
const envFile = path.join(__dirname, '..', '..', '.env');
if (fs.existsSync(envFile)) {
  for (const line of fs.readFileSync(envFile, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^([A-Z_]+)=(.*)$/);
    if (m) env[m[1]] = m[2];
  }
}
const get = (k) => process.env[k] ?? env[k];
const SCHEMA = process.env.QS_SCHEMA || 'elder_care_qs2';
if (SCHEMA === 'elder_care') throw new Error('Không chạy trên schema elder_care');
const MYSQL = process.env.MYSQL_BIN || 'mysql';

function run(user, password, sql) {
  const r = spawnSync(MYSQL, ['-u', user, '-N', '-B', '--default-character-set=utf8mb4', SCHEMA, '-e', sql], {
    env: { ...process.env, MYSQL_PWD: password || '' },
    encoding: 'utf8',
  });
  const err = (r.stderr || '').replace(/^mysql: \[Warning\].*\r?\n?/gm, '').trim();
  return { ok: r.status === 0 && !err, out: (r.stdout || '').trim(), err };
}

const query = (sql) => run(get('TEST_DB_ADMIN_USER') || 'root', get('TEST_DB_ADMIN_PASSWORD') || get('DB_MIGRATION_PASSWORD'), sql);
const asApp = (sql) => run(get('DB_USERNAME') || 'eldercare_app', get('DB_PASSWORD'), sql);

module.exports = { query, asApp, SCHEMA };
