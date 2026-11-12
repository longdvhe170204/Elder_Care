// Nạp seed.sql một lần sau khi Flyway của backend demo chạy xong; đã có người cao tuổi QS-A thì bỏ qua (chạy lại được).
const fs = require('fs');
const path = require('path');
const L = require('./lib.cjs');
if (L.sql("SELECT id FROM resident WHERE profile_code = 'QS-A'").length) {
  console.log('seed: đã có dữ liệu, bỏ qua');
} else {
  L.sql(fs.readFileSync(path.join(__dirname, 'seed.sql'), 'utf8'), { write: true });
  console.log('seed: đã nạp');
}
