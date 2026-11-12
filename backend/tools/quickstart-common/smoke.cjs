// Khói của quickstart-common: dựng world.base() và một người cao tuổi có giường, đọc lại qua API. Chạy lại được nhiều lần
// (mỗi lần lấy giường trống của cơ cấu demo; hết giường thì giải phóng giường đầu). node tools/quickstart-common/smoke.cjs
const q = require('./qs.cjs');
const world = require('./world.cjs');
const { QLV, check, step, summary } = q;

(async () => {
  for (const u of [QLV, 'demo.bacsi', 'demo.dieuduong', 'demo.truongtang']) {
    const r = await q.login(u);
    if (!r.body?.token) throw new Error('đăng nhập ' + u + ': ' + JSON.stringify(r.body));
  }
  const now = await q.now();
  let w;
  await step('S1', async () => {
    w = await world.base();
    check('S1', 'world.base(): cơ cấu khu – tầng – phòng – giường', w.area > 0 && w.floor1 > 0 && Object.values(w.beds).every(b => b > 0), w);
    const area = await q.get(QLV, `/areas/${w.area}`);
    check('S1', 'đọc lại khu qua API 003', area.status === 200 && area.body.code === 'QA', area);
    check('S1', 'hồ sơ nhân viên 9001 → 9009', q.one(`SELECT COUNT(*) FROM staff_profile WHERE id BETWEEN 9001 AND 9009`) === '9');
  });
  if (!w) return summary('common-smoke');

  await step('S2', async () => {
    const ids = Object.values(w.beds);
    let bed = Number(q.one(`SELECT id FROM bed WHERE id IN (${ids.join(',')}) AND state='EMPTY' ORDER BY id LIMIT 1`) ?? 0);
    if (!bed) { bed = w.beds.P101G1; world.freeBed(bed); }
    const id = world.resident({ sinceIso: now, bedId: bed, careLevel: 'REGULAR' });
    const res = await q.get(QLV, `/residents/${id}`);
    check('S2', 'đọc lại người cao tuổi qua API 001', res.status === 200 && res.body.id === id && res.body.status === 'IN_RESIDENCE', res);
    const loc = await q.get(QLV, `/residents/${id}/location`);
    check('S2', 'vị trí hiện tại là giường vừa phân bổ (API 003)', loc.status === 200 && loc.body.location?.bedId === bed && loc.body.currentAllocation?.status === 'ACTIVE', loc);
    const b = await q.get(QLV, `/beds/${bed}`);
    check('S2', 'giường Đang sử dụng', b.status === 200 && b.body.state === 'OCCUPIED', b);
  });

  await step('S3', async () => {
    const ws = world.shift(w.floor1, now, 8);
    world.member(ws, 'demo.dieuduong');
    world.lead(ws, 'demo.dieuduong', now);
    check('S3', 'ca đã công bố có nhân viên', q.one(`SELECT COUNT(*) FROM shift_staff WHERE shift_id=${ws}`) === '1');
    const rid = world.resident({ sinceIso: now, contract: false });
    world.assignResident(ws, 'demo.dieuduong', 'NURSING_STAFF', rid, now, new Date(Date.parse(now) + 8 * 3600e3).toISOString(), true);
    const r = await q.get('demo.dieuduong', `/residents/${rid}`);
    check('S3', 'điều dưỡng được phân công đọc được hồ sơ', r.status === 200, r);
  });

  await step('S4', async () => {
    world.floorManager('demo.truongtang', w.floor1, now);
    const r = await q.get('demo.truongtang', `/beds?floorId=${w.floor1}`);
    check('S4', 'Trưởng tầng xem bảng giường tầng mình', r.status === 200, r);
    const lead = await world.assignLead('Y tế', 'demo.bacsi');
    check('S4', 'phụ trách bộ phận Y tế (API 008)', lead.status < 300, lead);
  });
  summary('common-smoke');
})();
