// Quickstart 000 mục 3, K1–K21, ở profile demo (000 T103). Chạy backend bằng QuickstartDemoApplication (xem README), rồi
// node tools/quickstart-000/run-k1-k21.cjs. In PASS/FAIL từng kiểm tra. Chạy lại được: đưa CFG-M15-05 về mặc định trước K1.
const { login, call } = require('./api.cjs');
const QLV = 'demo.quanly', QLV2 = 'demo.quanly2', DD = 'demo.dieuduong', TT = 'demo.truongtang', HC_A = 'demo.hanhchinh', HC_B = 'demo.ketoan';
const results = [];
function check(k, label, ok, data) {
  results.push({ k, label, ok: !!ok });
  console.log(`${ok ? 'PASS' : 'FAIL'} ${k} ${label}${ok ? '' : ' :: ' + JSON.stringify(data).slice(0, 600)}`);
}
const USERS = [QLV, QLV2, DD, TT, HC_A, HC_B];
const clock = async (body) => {
  const r = await call(QLV, 'POST', '/testing/clock', body);
  if (body.advanceBy || body.setTo) for (const u of USERS) await login(u); // tua giờ làm phiên hết hạn (CFG-M15-10)
  return r;
};
const run = (...jobs) => clock({ runJobs: jobs });
const control = (s) => call(QLV, 'POST', '/testing/sample-request-control', { conditionUnmet: false, failOnApply: false, allowSelfApproval: true, roleRestriction: null, ...s });
// Ngày theo giờ Việt Nam: lấy phần ngày của chuỗi giờ có múi +07:00 do /testing/clock trả
const day = (iso, plus) => { const d = new Date(iso.slice(0, 10) + 'T00:00:00Z'); d.setUTCDate(d.getUTCDate() + plus); return d.toISOString().slice(0, 10); };
const get = (u, path) => call(u, 'GET', path);
const transition = async (u, id, cmd, body) => {
  const cur = await get(u, `/approval-requests/${id}`);
  return call(u, 'POST', `/approval-requests/${id}/${cmd}`, body, { 'If-Match': cur.etag });
};
// Đọc hết các trang: thông báo của lần chạy trước (đồng hồ đã tua xa hơn) có thể chiếm trang đầu
const notes = async (u) => {
  const items = [];
  let cursor = null;
  do {
    const page = (await get(u, '/me/notifications' + (cursor ? '?cursor=' + encodeURIComponent(cursor) : ''))).body;
    items.push(...(page.items || []));
    cursor = page.nextCursor;
  } while (cursor);
  return { items };
};
const noteTypes = (page) => JSON.stringify(page);
const newRequest = async (u, key, effective, submit = true) =>
  (await call(u, 'POST', '/approval-requests', { type: 'M15.SAMPLE', content: { key }, requestedEffectiveDate: effective, reason: 'Quickstart ' + key, submitImmediately: submit })).body;

(async () => {
  for (const u of [QLV, QLV2, DD, TT, HC_A, HC_B]) await login(u);
  const tag = Date.now().toString(36);
  await control({});
  const c0 = (await call(QLV, 'GET', '/testing/clock')).body;
  let now = c0.simulated ? c0.now : (await clock({ setTo: new Date().toISOString() })).body.now;
  // Chạy lại: đưa CFG-M15-05 về mặc định 48 giờ trước K1
  const p0 = await get(QLV, '/parameters/CFG-M15-05');
  if (p0.body.currentValue?.value !== 48) await call(QLV, 'POST', '/parameters/CFG-M15-05/change-value', { restoreDefault: true, reason: 'Quickstart: chạy lại từ mặc định' }, { 'If-Match': p0.etag });
  now = (await clock({ advanceBy: 'PT1M' })).body.now;
  console.log('clock', now);

  // K1 → K5: tham số
  let p = await get(QLV, '/parameters/CFG-M15-05');
  const etagBefore = p.etag, beforeK1 = now;
  const histBefore = (await get(QLV, '/parameters/CFG-M15-05/history')).body.length;
  await clock({ advanceBy: 'PT1M' });
  let r = await call(QLV, 'POST', '/parameters/CFG-M15-05/change-value', { value: { value: 24, unit: 'HOURS' }, reason: 'Quickstart K1: nhắc sau 24 giờ' }, { 'If-Match': etagBefore });
  const hist = (await get(QLV, '/parameters/CFG-M15-05/history')).body;
  const log1 = (await get(QLV, '/audit-logs?entityId=CFG-M15-05')).body;
  check('K1', 'đổi CFG-M15-05 = 24 giờ: 200, lịch sử thêm 1 dòng, nhật ký trước 48 sau 24', r.status === 200 && hist.length === histBefore + 1
    && JSON.stringify(log1.items?.[0]).includes('48') && JSON.stringify(log1.items?.[0]).includes('24'), { r, hist: hist.length, histBefore, log: log1.items?.[0] });
  r = await call(QLV, 'POST', '/parameters/CFG-M15-05/change-value', { value: { value: 30, unit: 'HOURS' }, reason: 'Quickstart K2' }, { 'If-Match': etagBefore });
  check('K2', 'If-Match cũ: 412, currentValue = 24', r.status === 412 && JSON.stringify(r.body).includes('24'), r);
  p = await get(QLV, '/parameters/CFG-M15-06');
  r = await call(QLV, 'POST', '/parameters/CFG-M15-06/change-value', { value: { value: 20, unit: 'HOURS' }, reason: 'Quickstart K3' }, { 'If-Match': p.etag });
  check('K3', 'CFG-M15-06 = 20 giờ < CFG-M15-05: 422 INVALID_VALUE', r.status === 422 && r.body.errorCode === 'INVALID_VALUE', r);
  p = await get(QLV, '/parameters/CFG-M15-05');
  r = await call(DD, 'POST', '/parameters/CFG-M15-05/change-value', { value: { value: 12, unit: 'HOURS' }, reason: 'Quickstart K4' }, { 'If-Match': p.etag });
  check('K4', 'Điều dưỡng đổi tham số: 403', r.status === 403, r);
  r = await get(QLV, `/parameters/CFG-M15-05/history?asOf=${encodeURIComponent(beforeK1)}`);
  check('K5', 'lịch sử tại thời điểm trước K1: 48 giờ', r.status === 200 && JSON.stringify(r.body).includes('"value":48'), r);

  // K6 → K8: nhắc, báo Quản lý viện, duyệt đồng thời
  const qlvNotesBefore = JSON.stringify(await notes(QLV)).length;
  const y6 = await newRequest(HC_A, 'k6-' + tag, day(now, 1));
  check('K6', 'Hành chính A lập và gửi duyệt M15.SAMPLE', y6.status === 'PENDING_APPROVAL', y6);
  now = (await clock({ advanceBy: 'PT25H', runJobs: ['REMIND_PENDING_REQUESTS'] })).body.now;
  let n = await notes(QLV);
  const s6 = (await get(HC_A, `/approval-requests/${y6.id}`)).body;
  check('K6', 'quá 24 giờ: Quản lý viện có thông báo nhắc của yêu cầu; vẫn PENDING_APPROVAL', s6.status === 'PENDING_APPROVAL' && noteTypes(n).includes(y6.displayCode), { status: s6.status, notes: n });
  now = (await clock({ advanceBy: 'PT72H', runJobs: ['REMIND_PENDING_REQUESTS'] })).body.now;
  n = await notes(QLV);
  const s7 = (await get(HC_A, `/approval-requests/${y6.id}`)).body;
  check('K7', 'quá 96 giờ: báo Quản lý viện; không tự hủy', s7.status === 'PENDING_APPROVAL' && JSON.stringify(n).length > qlvNotesBefore, { status: s7.status, notes: n });
  const cur = await get(QLV, `/approval-requests/${y6.id}`);
  const [a1, a2] = await Promise.all([
    call(QLV, 'POST', `/approval-requests/${y6.id}/approve`, {}, { 'If-Match': cur.etag }),
    call(QLV2, 'POST', `/approval-requests/${y6.id}/approve`, {}, { 'If-Match': cur.etag }),
  ]);
  const st = [a1.status, a2.status].sort();
  check('K8', 'hai Quản lý viện duyệt cùng If-Match: một 200, một 409 ALREADY_PROCESSED', st[0] === 200 && [409, 412].includes(st[1]), { a1: [a1.status, a1.body?.errorCode], a2: [a2.status, a2.body?.errorCode] });

  // K9: áp dụng ba lần, đúng một kết quả
  for (let i = 0; i < 3; i++) await run('APPLY_DUE_REQUESTS');
  const s9 = (await get(QLV, `/approval-requests/${y6.id}`)).body;
  check('K9', 'chạy job áp dụng 3 lần: APPLIED, một kết quả áp dụng', s9.status === 'APPLIED' && s9.applicationResult?.type === 'M15.SAMPLE_CATALOG', s9);

  // K10: áp dụng bù
  const y10 = await newRequest(HC_A, 'k10-' + tag, day(now, 1));
  r = await transition(QLV, y10.id, 'approve', {});
  now = (await clock({ advanceBy: 'P3D' })).body.now;
  await run('APPLY_DUE_REQUESTS');
  const s10 = (await get(QLV, `/approval-requests/${y10.id}`)).body;
  check('K10', 'Bộ lập lịch tắt 2 ngày qua ngày hiệu lực: catchUpApplied = true', s10.status === 'APPLIED' && s10.catchUpApplied === true, { approve: r.status, s10 });

  // K11: điều kiện không thỏa
  await control({ conditionUnmet: true });
  const y11 = await newRequest(HC_A, 'k11-' + tag, day(now, 1));
  await transition(QLV, y11.id, 'approve', {});
  now = (await clock({ advanceBy: 'P1DT1H', runJobs: ['APPLY_DUE_REQUESTS'] })).body.now;
  const s11 = (await get(QLV, `/approval-requests/${y11.id}`)).body;
  const hcNotes11 = await notes(HC_A);
  check('K11', 'điều kiện không thỏa: APPLY_FAILED, người yêu cầu được báo', s11.status === 'APPLY_FAILED' && JSON.stringify(hcNotes11).includes(y11.displayCode), { s11, hcNotes11 });

  // K12: lỗi khi áp dụng
  await control({ failOnApply: true });
  const y12 = await newRequest(HC_A, 'k12-' + tag, day(now, 1));
  await transition(QLV, y12.id, 'approve', {});
  now = (await clock({ advanceBy: 'P1DT1H', runJobs: ['APPLY_DUE_REQUESTS'] })).body.now;
  const s12a = (await get(QLV, `/approval-requests/${y12.id}`)).body;
  const log12 = (await get(QLV, `/audit-logs?entityId=${y12.id}`)).body;
  await control({});
  await run('APPLY_DUE_REQUESTS');
  const s12b = (await get(QLV, `/approval-requests/${y12.id}`)).body;
  check('K12', 'lỗi khi áp dụng: vẫn APPROVED, nhật ký lỗi áp dụng; tắt lỗi rồi chạy lại: APPLIED', s12a.status === 'APPROVED' && JSON.stringify(log12).includes('APPLY') && s12b.status === 'APPLIED', { a: s12a.status, b: s12b.status, log: log12.items?.map(i => i.action) });

  // K13: tự duyệt (Hành chính, vì Quản lý viện không có quyền lập loại mẫu ở ma trận thật)
  const y13 = await newRequest(HC_A, 'k13-' + tag, day(now, 1));
  const r13a = await transition(HC_A, y13.id, 'approve', {});
  const r13b = await transition(HC_A, y13.id, 'approve', { comment: 'Tự duyệt vì không có người duyệt khác (quickstart K13)' });
  check('K13', 'tự duyệt không ý kiến: 409/422; có ý kiến: 200, selfApproved', [409, 422].includes(r13a.status) && r13b.status === 200 && r13b.body?.selfApproved !== false, { a: [r13a.status, r13a.body?.errorCode], b: [r13b.status, r13b.body] });

  // K14: bản ghi mẫu gắn tầng, đính chính cách A
  const f14 = (await call(DD, 'POST', '/testing/sample-data/floor-records', { floorAreaId: 2, note: 'Ghi nhận mẫu K14' })).body;
  r = await call(DD, 'POST', `/corrections/M15.SAMPLE_RECORD/${f14.id}`, { type: 'AMEND_CONTENT', correctedContent: { note: 'Ghi nhận mẫu K14 (đã sửa)' }, reason: 'Quickstart K14: ghi nhầm nội dung' });
  const c14 = await get(DD, `/corrections/M15.SAMPLE_RECORD/${f14.id}`);
  check('K14', 'Điều dưỡng đính chính bản ghi của mình: 201 cách A; chuỗi có corrected', r.status === 201 && r.body?.creationMethod === 'A' && JSON.stringify(c14.body).includes('"corrected":true'), { r, c14 });

  // K15: sửa, xóa trực tiếp bảng nhóm 3
  r = await call(QLV, 'POST', `/testing/sample-data/floor-records/${f14.id}/direct-change`, {});
  check('K15', 'UPDATE, DELETE trực tiếp bằng eldercare_app bị chặn', r.status === 200 && !String(r.body.update).startsWith('OK') && !String(r.body.delete).startsWith('OK'), r.body);

  // K16: đính chính cách C
  console.log('assign-role', (await roleCommand('assign-role', 'Quickstart K16: Hành chính B (gỡ sau khi chạy)')).status);
  await login(HC_B);
  const o16 = (await call(HC_A, 'POST', '/testing/sample-data/office-records', { note: 'Bản ghi văn phòng K16' })).body;
  r = await call(HC_B, 'POST', `/corrections/M15.SAMPLE_OFFICE_RECORD/${o16.id}`, { type: 'AMEND_CONTENT', correctedContent: { note: 'Bản ghi văn phòng K16 (đã sửa)' }, reason: 'Quickstart K16: sửa bản ghi của người khác' });
  const y16 = r.body;
  const rTT = await call(TT, 'POST', `/corrections/M15.SAMPLE_OFFICE_RECORD/${o16.id}`, { type: 'AMEND_CONTENT', correctedContent: { note: 'x' }, reason: 'Quickstart K16: trưởng tầng thử' });
  let a16 = null, c16 = null;
  if (r.status === 202) {
    a16 = await transition(QLV, y16.id, 'approve', {});
    await run('APPLY_DUE_REQUESTS');
    c16 = (await get(QLV, `/corrections/M15.SAMPLE_OFFICE_RECORD/${o16.id}`)).body;
  }
  check('K16', 'Hành chính B: 202 yêu cầu M15.CORRECTION chờ duyệt; QLV duyệt thì có đính chính cách C; Trưởng tầng 403', r.status === 202 && y16.status === 'PENDING_APPROVAL' && a16?.status === 200 && JSON.stringify(c16).includes('"creationMethod":"C"') && rTT.status === 403, { r: [r.status, y16?.type, y16?.status], a16: a16?.status, c16, tt: rTT.status });

  // K17: hủy ghi nhận rồi sửa
  const f17 = (await call(DD, 'POST', '/testing/sample-data/floor-records', { floorAreaId: 2, note: 'Ghi nhận mẫu K17' })).body;
  const v17 = await call(DD, 'POST', `/corrections/M15.SAMPLE_RECORD/${f17.id}`, { type: 'VOID', reason: 'Quickstart K17: ghi nhầm người' });
  r = await call(DD, 'POST', `/corrections/M15.SAMPLE_RECORD/${f17.id}`, { type: 'AMEND_CONTENT', correctedContent: { note: 'sau khi hủy' }, reason: 'Quickstart K17: sửa sau khi hủy' });
  check('K17', 'VOID rồi AMEND_CONTENT: 409 RECORD_VOIDED', v17.status === 201 && r.status === 409 && r.body?.errorCode === 'RECORD_VOIDED', { v17: v17.status, r });

  // K18: QLV thấy đủ; Hành chính không có phân công nên hồ sơ mẫu ngoài phạm vi (quickstart sửa 2026-11-01)
  const p18 = (await call(QLV, 'POST', '/testing/sample-data/profile-changes', {})).body;
  const q18 = await get(QLV, `/audit-logs/entities/${p18.entityType}/${p18.entityId}`);
  const i18 = q18.body?.items?.[0];
  r = await get(HC_A, `/audit-logs/entities/${p18.entityType}/${p18.entityId}`);
  check('K18', 'QLV: 200, thấy address, allergies và reason; Hành chính: 404', q18.status === 200 && i18?.dataAfter?.address === '34 Trần Phú' && i18?.dataAfter?.allergies === 'Penicillin' && i18?.reason != null && r.status === 404, { qlv: q18, hc: r.status });

  // K19: lọc tự duyệt
  r = await get(QLV, '/audit-logs?selfApproved=true');
  check('K19', 'QLV lọc selfApproved=true thấy lần tự duyệt ở K13', r.status === 200 && JSON.stringify(r.body).includes(String(y13.id)), { status: r.status, items: r.body?.items?.slice(0, 3) });

  // K20: danh mục mẫu
  const c1 = (await call(QLV, 'POST', '/testing/sample-data/catalog-items', { name: 'K20 chưa dùng ' + tag })).body;
  const d1 = await call(QLV, 'DELETE', `/testing/sample-data/catalog-items/${c1.id}`);
  const c2 = (await call(QLV, 'POST', '/testing/sample-data/catalog-items', { name: 'K20 đã dùng ' + tag })).body;
  await call(QLV, 'POST', `/testing/sample-data/catalog-items/${c2.id}/cases`, {});
  const d2 = await call(QLV, 'DELETE', `/testing/sample-data/catalog-items/${c2.id}`);
  const c3 = (await call(QLV, 'POST', '/testing/sample-data/catalog-items', { name: 'K20 trùng ' + tag })).body;
  await call(QLV, 'POST', `/testing/sample-data/catalog-items/${c3.id}/deactivate`, {});
  await call(QLV, 'POST', '/testing/sample-data/catalog-items', { name: 'K20 trùng ' + tag });
  const re3 = await call(QLV, 'POST', `/testing/sample-data/catalog-items/${c3.id}/reactivate`, {});
  check('K20', 'xóa mục chưa dùng 204; mục đã dùng 409 CATALOG_ITEM_IN_USE; kích hoạt lại khi trùng tên 409', d1.status === 204 && d2.status === 409 && d2.body?.errorCode === 'CATALOG_ITEM_IN_USE' && re3.status === 409, { d1: d1.status, d2: [d2.status, d2.body?.errorCode], re3: [re3.status, re3.body?.errorCode] });

  // K21: giới hạn vai trò duyệt
  await control({ roleRestriction: ['NURSING_HOME_MANAGER'] });
  const hcNotesBefore = JSON.stringify(await notes(HC_B));
  const y21 = await newRequest(HC_A, 'k21-' + tag, day(now, 10));
  const v21 = (await get(HC_B, `/approval-requests/${y21.id}`)).body;
  const ap21 = await transition(HC_B, y21.id, 'approve', {});
  const rj21 = await transition(HC_B, y21.id, 'reject', { comment: 'Quickstart K21: thử từ chối' });
  now = (await clock({ advanceBy: 'PT25H', runJobs: ['REMIND_PENDING_REQUESTS'] })).body.now;
  const hcNotesAfter = JSON.stringify(await notes(HC_B));
  const qlvNotes21 = JSON.stringify(await notes(QLV));
  await control({});
  const ap21b = await transition(HC_B, y21.id, 'approve', {});
  check('K21', 'giới hạn QLV: Hành chính không có approve/reject, gọi 403; nhắc chỉ tới QLV; tắt giới hạn thì Hành chính duyệt được',
    !v21.allowedCommands.includes('approve') && !v21.allowedCommands.includes('reject') && ap21.status === 403 && rj21.status === 403
    && !hcNotesAfter.slice(hcNotesBefore.length - 2).includes(y21.displayCode) && qlvNotes21.includes(y21.displayCode) && ap21b.status === 200,
    { allowed: v21.allowedCommands, ap21: ap21.status, rj21: rj21.status, hcGotReminder: hcNotesAfter.includes(y21.displayCode), qlvGot: qlvNotes21.includes(y21.displayCode), ap21b: ap21b.status });

  // Dọn: gỡ vai trò Hành chính của demo.ketoan
  r = await roleCommand('remove-role', 'Quickstart: gỡ vai trò thêm ở K16');
  console.log('cleanup remove-role', r.status);
  console.log(`\n${results.filter(x => x.ok).length}/${results.length} PASS`);
})().catch(e => { console.error(e); process.exit(1); });

async function roleCommand(cmd, reason) {
  const id = await accountId(QLV, HC_B);
  const perms = await call(QLV, 'GET', `/accounts/${id}/permissions`);
  return call(QLV, 'POST', `/accounts/${id}/${cmd}`, { role: 'AC-09', reason }, { 'If-Match': perms.etag });
}

async function accountId(asUser, username) {
  const r = await call(asUser, 'GET', '/accounts?q=' + encodeURIComponent(username));
  const item = (r.body.items || r.body).find(a => a.username === username);
  return item.id;
}
