// Điều kiện cần của quickstart 007 mục 1 dựng bằng API thật, chạy một lần sau seed.sql: đặt đồng hồ giả lập 2026-12-01 08:00,
// khoảng vật lý CFG-M06-03 (SpO2 50–100…), ngưỡng mặc định của cơ sở (SpO2 cảnh báo dưới 94, nguy hiểm dưới 90),
// dị ứng Penicillin và nguyện vọng cuối đời của A (001). Chạy lại được: bước đã có thì bỏ qua.
const L = require('./lib.cjs');
const QL = 'demo.quanly', B = 'demo.bacsi';
const A = 2001;

const RANGES = {
  SPO2: { VALUE: { min: 50, max: 100 } },
  BLOOD_PRESSURE: { SYSTOLIC: { min: 40, max: 300 }, DIASTOLIC: { min: 20, max: 200 } },
  HEART_RATE: { VALUE: { min: 20, max: 250 } },
  BODY_TEMPERATURE: { VALUE: { min: 30, max: 45 } },
  BLOOD_GLUCOSE: { VALUE: { min: 1, max: 40 } },
  WEIGHT: { VALUE: { min: 20, max: 200 } },
};
const FACILITY = {
  SPO2: [{ code: 'VALUE', dangerBelow: 90, warningBelow: 94 }],
  BLOOD_PRESSURE: [
    { code: 'SYSTOLIC', warningAbove: 140, dangerAbove: 160, warningBelow: 90, dangerBelow: 80 },
    { code: 'DIASTOLIC', warningAbove: 85, dangerAbove: 90, warningBelow: 60, dangerBelow: 50 },
  ],
  HEART_RATE: [{ code: 'VALUE', dangerBelow: 40, warningBelow: 50, warningAbove: 100, dangerAbove: 130 }],
  BODY_TEMPERATURE: [{ code: 'VALUE', dangerBelow: 35, warningBelow: 35.5, warningAbove: 37.5, dangerAbove: 38.5 }],
};

async function main() {
  await L.login(QL);
  const c = (await L.get(QL, '/testing/clock')).body;
  if (!c.simulated || c.now < '2026-12-01') await L.clock({ setTo: '2026-12-01T08:00:00+07:00' });
  await L.login(B);
  const p = await L.get(QL, '/parameters/CFG-M06-03');
  if (!p.body.currentValue || !p.body.currentValue.SPO2) {
    const r = await L.call(QL, 'POST', '/parameters/CFG-M06-03/change-value', { value: RANGES, reason: 'Quickstart 007: khoảng vật lý' }, { 'If-Match': p.etag });
    console.log('CFG-M06-03', r.status, r.status === 200 ? '' : L.j(r.body));
  }
  const catalog = (await L.get(QL, '/vital-sign-catalog')).body;
  const id = Object.fromEntries(catalog.map((x) => [x.code, x.id]));
  const now = (await L.get(QL, '/testing/clock')).body.now;
  for (const [code, components] of Object.entries(FACILITY)) {
    const have = (await L.get(B, `/thresholds?scope=FACILITY&indicatorId=${id[code]}`)).body;
    if (Array.isArray(have) && have.some((t) => t.status === 'EFFECTIVE')) continue;
    const r = await L.post(B, '/thresholds', { scope: 'FACILITY', indicatorId: id[code], effectiveFrom: now, components, reason: 'Quickstart 007: ngưỡng mặc định' });
    console.log('ngưỡng cơ sở', code, r.status, r.status < 300 ? '' : L.j(r.body));
  }
  console.log(L.j({ indicator: id }));
}
main().catch((e) => { console.error(e); process.exit(1); });
