// Nghiệm thu quickstart 007 bằng API thật trên backend demo (QuickstartDemoApplication, cổng 8083, schema elder_care_qs3).
// Chỉ chạy các bước mà D:\eldercare-logs\quickstart\007.md ghi MỘT PHẦN / KHÔNG THẤY / KHÔNG CHẮC. Mỗi bước in
// ĐẠT / KHÔNG ĐẠT / CẦN XEM TAY. Cần chạy trước: reset.cjs (schema sạch), khởi động backend, seed.cjs, setup.cjs.
// Thông báo kiểm bằng bảng notification_request, notification (SELECT); job M05, M06 chạy theo lịch mỗi phút thật nên
// sau mỗi lần tua đồng hồ script chờ job (tối đa ~80 giây).
const crypto = require('crypto');
const L = require('./lib.cjs');
const { get, post, call, check, manual, step, j } = L;

// Tài khoản (demo/V002_90 và seed.sql)
const U = {
  QL: 'demo.quanly', T: 'demo.truongtang', B: 'demo.bacsi', D: 'demo.dieuduong', S: 'demo.chamsoc', K: 'demo.bep',
  H: 'demo.hanhchinh', D2: 'demo.dieuduong2', D5: 'demo.dieuduong5', P: 'demo.phutrachca', BS1: 'demo.bacsi1',
  DD: 'demo.dieuduongdem',
};
// Người cao tuổi, nhân viên (staff_id), người thân
const A = 2001, C = 2002, E = 2003, BB = 2004, G = 2005, F = 2006;
const ST = { 9001: 'QL', 9003: 'T', 9004: 'B', 9005: 'D', 9006: 'S', 9007: 'K', 9008: 'H', 9010: 'D2', 9011: 'D5', 9012: 'P', 9013: 'BS1', 9014: 'DD' };
const ACC = { 1: 'QL', 3: 'T', 4: 'B', 5: 'D', 6: 'S', 7: 'K', 8: 'H', 16: 'D2', 17: 'D5', 18: 'P', 19: 'BS1', 20: 'DD' };
const FM = { 4001: 'M', 4002: 'R', 4003: 'N', 4004: 'KF' };
const DEV_A = 'qs007-device-A', DEV_B = 'qs007-device-B';
let IND = {}; // mã chỉ số → id
let TYPE = {}; // mã loại sự cố, diễn biến → id
let PROG = []; // loại diễn biến kèm nhóm

// ---- tiện ích ----
const maxId = (t) => +L.sql(`SELECT COALESCE(MAX(id), 0) m FROM ${t}`)[0].m;
// event_publication có id UUID: mốc theo thời điểm phát
const pubMark = () => L.sql("SELECT COALESCE(MAX(publication_date), '2000-01-01') m FROM event_publication")[0].m;
const pubSince = (m) => L.sql(`SELECT event_type, serialized_event s FROM event_publication WHERE publication_date > '${m}'`);
const fallRows = (resident) => L.sql(`SELECT id, status, effective_from, effective_to, content FROM simple_record WHERE kind = 'M05.POST_FALL_MONITORING' AND resident_id = ${resident} ORDER BY id`);
const alertRow = (id) => L.sql(`SELECT * FROM alert WHERE id = ${+id}`)[0];
const alertsSince = (mark, resident) => L.sql(`SELECT * FROM alert WHERE id > ${mark}${resident ? ' AND resident_id = ' + resident : ''} ORDER BY id`);
const incidentRow = (id) => L.sql(`SELECT * FROM incident WHERE id = ${+id}`)[0];
async function waitFor(fn, ms = 85000, every = 3000) {
  const t0 = Date.now();
  for (;;) {
    const v = await fn();
    if (v) return v;
    if (Date.now() - t0 > ms) return v;
    await L.sleep(every);
  }
}
// Thông báo có yêu cầu id > since; chờ tới khi số dòng ổn định
async function notesSince(since, pred = () => true) {
  let prev = -1, rows = [];
  for (let i = 0; i < 12; i++) {
    await L.sleep(700);
    rows = L.sql(`SELECT r.id rid, r.event_type et, r.source_record_type st, r.source_record_id sid, r.level rl,
        r.short_content sc, r.information_subject subj, r.q354_case rcase, n.recipient_type rt, n.recipient_id rcp,
        n.q354_case ncase, n.visible_parts vp FROM notification_request r LEFT JOIN notification n ON n.request_id = r.id
        WHERE r.id > ${since} ORDER BY r.id`).filter(pred);
    if (rows.length && rows.length === prev) break;
    prev = rows.length;
  }
  return rows;
}
const name = (r) => r.rt === 'STAFF' ? (ST[r.rcp] || ACC[r.rcp] || 'staff' + r.rcp) : r.rt === 'FAMILY_MEMBER' ? (FM[r.rcp] || 'fm' + r.rcp) : 'resident' + r.rcp;
const who = (rows) => new Set(rows.filter((r) => r.rt).map(name));
const has = (set, ...names) => names.every((n) => set.has(n));
const none = (set, ...names) => names.every((n) => !set.has(n));
const brief = (rows) => rows.map((r) => `${r.et}:${r.rt ? name(r) : '-'}${r.ncase ? '(' + r.ncase + ')' : ''}`).join(', ');
const code = (r) => r.body && r.body.errorCode;
const iso = (d) => new Date(d).toISOString();
const ts = (s) => new Date(String(s).replace(' ', 'T') + 'Z'); // cột DATETIME (UTC) từ mysql CLI
const plusMin = (t, m) => iso(new Date(t).getTime() + m * 60000);
const nowIso = async () => iso((await L.now()));
async function advance(dur) { await L.clock({ advanceBy: dur }); }
async function setTo(t) { await L.clock({ setTo: t }); }
const tok = (dev) => ({ 'X-Device-Token': dev });
const sos = (dev, method, p, body, extra = {}) => call('nct', method, p, body, { ...tok(dev), ...extra });

// POST /residents/{id}/vital-signs trả 403 SCOPE với mọi người có phạm vi "được giao" (lỗi backend B1, xem README), nên
// ghi chỉ số qua khung đồng bộ ngoại tuyến của 002 (POST /sync, loại M06.BAN_GHI_CHI_SO; VitalSignOfflineHandler) như app
// nhân viên; đọc lại bằng Bác sĩ (phạm vi toàn viện).
async function vital(user, resident, ind, values, extra = {}) {
  const t = extra.measuredAt || await nowIso();
  const content = { residentId: resident, indicatorId: IND[ind], values, immediateAction: !!extra.immediateAction, taskId: null, note: null, originalRecordId: extra.originalRecordId ?? null };
  const r = await post(user, '/sync', { lastConnectedAt: t, records: [{ idempotencyKey: crypto.randomUUID(), recordType: 'M06.BAN_GHI_CHI_SO', deviceTime: t, content }] });
  const res = r.body && r.body.results && r.body.results[0];
  if (!res || !/^vital_sign:/.test(res.reference || '')) return { status: r.status === 200 ? 422 : r.status, body: { errorCode: res && res.reason, sync: r.body } };
  const id = +res.reference.split(':')[1];
  return { status: 201, body: await readRecord(resident, ind, id) };
}
const remeasure = (user, resident, ind, originalRecordId, values) => vital(user, resident, ind, values, { originalRecordId });
// GET chỉ trả bản ghi đã xác nhận; bản ghi chờ đo lại, đã đo lại đọc từ bảng (SELECT), đặt tên trường như API
async function readRecord(resident, ind, id) {
  const r = await get(U.B, `/residents/${resident}/vital-signs?indicatorId=${IND[ind]}`);
  const found = (Array.isArray(r.body) ? r.body : []).find((x) => x.id === id);
  if (found) return found;
  const x = L.sql(`SELECT * FROM vital_sign_record WHERE id = ${+id}`)[0];
  if (!x) return { id, missing: true };
  const comp = JSON.parse(x.components);
  return { id, fromDb: true, level: x.level, confirmationStatus: x.confirmation_status, labels: (x.labels || '').split(',').filter(Boolean),
    values: Object.fromEntries(Object.entries(comp).map(([k, v]) => [k, v.value])), threshold: { versionId: +x.threshold_version_id } };
}
async function newAlert(user, resident, typeCode, level = 'MEDIUM', description = 'Quickstart 007') {
  return post(user, '/alerts', { residentId: resident, incidentTypeId: TYPE[typeCode], level, description });
}
async function newIncident(user, body) {
  const t = await nowIso();
  // FR-041: đã có sự cố Khẩn cấp mở thì người ghi chọn tạo sự cố mới (createDespiteOpen) trừ khi bước kiểm chặn
  return post(user, '/incidents', { discoveredAt: t, createDespiteOpen: true, ...body, incidentTypeId: TYPE[body.type], type: undefined });
}
async function closeIncidentQuietly(id) {
  await post(U.B, `/incidents/${id}/receive`, {});
  const r = await post(U.B, `/incidents/${id}/close`, { resolution: 'Đóng để chạy bước sau (quickstart)' });
  if (r.status >= 300) console.log(`   (dọn) đóng sự cố ${id}: ${r.status} ${j(r.body)}`);
}
async function closeAlertQuietly(id, user = U.B) {
  await post(user, `/alerts/${id}/receive`, {});
  await post(user, `/alerts/${id}/start-handling`, {});
  const r = await post(user, `/alerts/${id}/close`, { resolution: 'Đóng để chạy bước sau (quickstart)' });
  if (r.status >= 300) console.log(`   (dọn) đóng cảnh báo ${id}: ${r.status} ${j(r.body)}`);
}
// Chờ job M05 (leo thang, mỗi phút) xử lý: điều kiện trên dòng alert
const waitAlert = (id, pred, ms) => waitFor(() => { const a = alertRow(id); return pred(a) ? a : null; }, ms);

// ======================================================================
async function prepare() {
  for (const u of Object.values(U)) await L.loginAs(u);
  const cat = (await get(U.QL, '/vital-sign-catalog')).body;
  IND = Object.fromEntries(cat.map((x) => [x.code, x.id]));
  for (const r of L.sql("SELECT id, code FROM catalog_item WHERE catalog_type IN ('M05.INCIDENT_TYPE', 'M05.PROGRESSION_TYPE')")) TYPE[r.code] = +r.id;
  PROG = (await get(U.QL, '/progression-types')).body;
  // Danh mục demo chưa có loại diễn biến nhóm Đánh giá (FR-045 cần một đánh giá và một xử lý để chuyển theo dõi)
  if (!PROG.some((p) => p.group === 'ACTION' && !p.resuscitation)) {
    const r = await post(U.QL, '/progression-types', { code: 'QS_FIRST_AID', name: 'Xử trí ban đầu', group: 'ACTION', resuscitation: false });
    console.log('   (chuẩn bị) thêm loại diễn biến Xử lý (không hồi sức):', r.status, r.status < 300 ? '' : j(r.body));
    PROG = (await get(U.QL, '/progression-types')).body;
  }
  if (!PROG.some((p) => p.group === 'EVALUATION')) {
    const r = await post(U.QL, '/progression-types', { code: 'QS_ASSESSMENT', name: 'Đánh giá tình trạng', group: 'EVALUATION', resuscitation: false });
    console.log('   (chuẩn bị) thêm loại diễn biến Đánh giá:', r.status, r.status < 300 ? '' : j(r.body));
    PROG = (await get(U.QL, '/progression-types')).body;
  }
  console.log('Đồng hồ', await L.now());
}

// ---------------- Chỉ số, ngưỡng, lịch đo ----------------
async function health() {
  await step('B1', async () => {
    const t = await nowIso();
    const res = [];
    for (const u of [U.S, U.D, U.B]) {
      const r = await post(u, `/residents/${A}/vital-signs`, { indicatorId: IND.SPO2, values: { VALUE: 97 }, measuredAt: t });
      const g = await get(u, `/residents/${A}/vital-signs`);
      res.push([u, r.status, code(r), r.body && r.body.failedLayer, 'GET', g.status]);
    }
    check('B1', 'ghi, xem chỉ số trực tuyến (POST/GET /residents/{id}/vital-signs): S, D trong phạm vi được ghi, xem', res[0][1] === 201 && res[1][1] === 201 && res[1][5] === 200, res);
  });
  await step('Q1', async () => {
    const r1 = await vital(U.S, A, 'SPO2', { VALUE: 101 });
    check('Q1', 'SpO2 101 → 422 ngoài khoảng vật lý, không lưu', r1.status === 422, r1);
    const mark = maxId('alert');
    const r2 = await vital(U.S, A, 'SPO2', { VALUE: 96 });
    check('Q1', 'SpO2 96 → lưu, Bình thường, không cảnh báo', r2.status === 201 && r2.body.level === 'NORMAL' && alertsSince(mark, A).length === 0, r2.body);
    const n0 = maxId('notification_request');
    const t = await L.now();
    const r3 = await vital(U.S, A, 'SPO2', { VALUE: 92 });
    const al = (await waitFor(() => { const x = alertsSince(mark, A); return x.length ? x : null; }, 8000, 1000)) || [];
    const a = al[0];
    const due = a && (ts(a.acknowledgement_deadline) - new Date(t)) / 60000;
    const ns = who(await notesSince(n0));
    check('Q1', 'SpO2 92 → Cảnh báo; cảnh báo Trung bình khóa SPO2, hạn 15 phút, D nhận', r3.body.level === 'WARNING' && al.length === 1 && a.level === 'MEDIUM' && new RegExp('VUOT_NGUONG:(SPO2|' + IND.SPO2 + ')$').test(a.type_lock) && Math.abs(due - 15) < 1.5 && ns.has('D'),
      { level: r3.body.level, alerts: al.map((x) => [x.type, x.type_lock, x.level, x.acknowledgement_deadline]), dueMin: due, notified: [...ns] });
  });

  await step('Q2', async () => {
    const mark = maxId('alert');
    const before = L.sql(`SELECT id, level FROM alert WHERE resident_id = ${A} AND type_lock = 'VUOT_NGUONG:${IND.SPO2}' AND status NOT IN ('CLOSED','CONVERTED_TO_INCIDENT')`);
    const r1 = await vital(U.S, A, 'SPO2', { VALUE: 88 });
    const afterFirst = L.sql(`SELECT id, level FROM alert WHERE resident_id = ${A} AND type_lock = 'VUOT_NGUONG:${IND.SPO2}' AND status NOT IN ('CLOSED','CONVERTED_TO_INCIDENT')`);
    check('Q2', 'SpO2 88 → CHỜ ĐO LẠI, chưa có cảnh báo mới (mức cảnh báo cũ giữ)', r1.status === 201 && r1.body.confirmationStatus === 'AWAITING_REMEASUREMENT' && alertsSince(mark, A).length === 0 && j(before) === j(afterFirst),
      { r1: r1.body, before, afterFirst });
    const n0 = maxId('notification_request');
    const r2 = await remeasure(U.S, A, 'SPO2', r1.body.id, { VALUE: 87 });
    const first = await readRecord(A, 'SPO2', r1.body.id);
    const al = L.sql(`SELECT id, level, occurrence_count FROM alert WHERE resident_id = ${A} AND type_lock = 'VUOT_NGUONG:${IND.SPO2}' AND status NOT IN ('CLOSED','CONVERTED_TO_INCIDENT')`);
    const rows = await notesSince(n0);
    const ns = who(rows);
    const inc = L.sql(`SELECT COUNT(*) c FROM incident WHERE resident_id = ${A}`)[0].c;
    check('Q2', 'đo lại 87 → bản đầu ĐÃ ĐO LẠI, cảnh báo gộp nâng Khẩn cấp; D, P, B, T nhận; M, QL không; không sự cố',
      r2.status === 201 && first && first.confirmationStatus === 'REMEASURED' && al.length === 1 && al[0].level === 'CRITICAL' && has(ns, 'D', 'P', 'B', 'T') && none(ns, 'M', 'QL') && +inc === 0,
      { r2: r2.status, first: first && first.confirmationStatus, alerts: al, notified: brief(rows), inc });
  });

  await step('Q3', async () => {
    const m = maxId('alert');
    const r = await vital(U.S, C, 'SPO2', { VALUE: 88 }, { immediateAction: true });
    const al = alertsSince(m, C);
    check('Q3', 'C: 88 "xử lý ngay" → xác nhận ngay, nhãn IMMEDIATE_ACTION, cảnh báo Khẩn cấp', r.body.confirmationStatus === 'CONFIRMED' && (r.body.labels || []).includes('IMMEDIATE_ACTION') && al.some((a) => a.level === 'CRITICAL'),
      { rec: r.body, alerts: al.map((a) => [a.type_lock, a.level]) });
    const rE = await vital(U.S, E, 'SPO2', { VALUE: 88 });
    const rF = await vital(U.D2, F, 'SPO2', { VALUE: 88 });
    const mF = maxId('alert');
    const rF2 = await remeasure(U.D2, F, 'SPO2', rF.body.id, { VALUE: 95 });
    const firstF = await readRecord(F, 'SPO2', rF.body.id);
    check('Q3', 'F: 88 rồi đo lại 95 → không cảnh báo, bản ghi đầu vẫn xem được (ĐÃ ĐO LẠI)', rF2.status === 201 && alertsSince(mF, F).length === 0 && firstF && firstF.confirmationStatus === 'REMEASURED',
      { rF2: rF2.body, firstF });
    const mE = maxId('alert');
    await advance('PT10M');
    const rec = await waitFor(() => {
      const x = L.sql(`SELECT confirmation_status, labels FROM vital_sign_record WHERE id = ${rE.body.id}`)[0];
      return x.confirmation_status === 'CONFIRMED' ? x : null;
    });
    const alE = alertsSince(mE, E).concat(L.sql(`SELECT * FROM alert WHERE resident_id = ${E} AND type_lock = 'VUOT_NGUONG:${IND.SPO2}'`));
    check('Q3', 'E: 88 không đo lại, tua 10 phút → xác nhận nhãn NOT_REMEASURED, cảnh báo Khẩn cấp', rec && /NOT_REMEASURED/.test(rec.labels) && alE.some((a) => a.level === 'CRITICAL'),
      { rec, alerts: alE.map((a) => [a.id, a.level]) });
  });

  await step('Q5', async () => {
    const r = await vital(U.D5, G, 'BLOOD_PRESSURE', { SYSTOLIC: 150, DIASTOLIC: 95 });
    check('Q5', 'Huyết áp 150/95 → Nguy hiểm, chờ đo lại', r.status === 201 && r.body.level === 'DANGEROUS' && r.body.confirmationStatus === 'AWAITING_REMEASUREMENT', r.body);
    // đo lại về bình thường để không sinh cảnh báo về sau
    await remeasure(U.D5, G, 'BLOOD_PRESSURE', r.body.id, { SYSTOLIC: 120, DIASTOLIC: 75 });
  });

  await step('Q6', async () => {
    const r = await get(U.H, `/residents/${A}/vital-signs`);
    check('Q6', 'Hành chính mở chỉ số của A → 403', r.status === 403, r);
  });

  await step('Q7', async () => {
    const tomorrow8 = '2026-12-02T01:00:00Z';
    const comp = [{ code: 'VALUE', dangerBelow: 35, warningBelow: 35.5, warningAbove: 37.8, dangerAbove: 39 }];
    const r = await post(U.B, '/thresholds', { scope: 'PERSONAL', residentId: A, indicatorId: IND.BODY_TEMPERATURE, effectiveFrom: tomorrow8, components: comp, reason: 'Quickstart Q7' });
    const list = (await get(U.B, `/thresholds?scope=PERSONAL&residentId=${A}&indicatorId=${IND.BODY_TEMPERATURE}`)).body;
    const v = (list || [])[0];
    check('Q7', 'B lưu ngưỡng cá nhân nhiệt độ A hiệu lực 08:00 mai → Chờ hiệu lực', r.status < 300 && v && v.status !== 'EFFECTIVE' && v.effectiveFrom === tomorrow8, { r: r.status, list });
    const bad = await post(U.B, '/thresholds', { scope: 'PERSONAL', residentId: A, indicatorId: IND.BODY_TEMPERATURE, effectiveFrom: tomorrow8, reason: 'x',
      components: [{ code: 'VALUE', dangerBelow: 35, warningBelow: 34, warningAbove: 37.8, dangerAbove: 39 }] });
    check('Q7', 'cận dưới cảnh báo < cận dưới nguy hiểm → 422 INVALID_THRESHOLD', bad.status === 422 && code(bad) === 'INVALID_THRESHOLD', bad);
    const d = await post(U.D, '/thresholds', { scope: 'PERSONAL', residentId: A, indicatorId: IND.BODY_TEMPERATURE, effectiveFrom: tomorrow8, components: comp, reason: 'x' });
    check('Q7', 'Điều dưỡng tạo ngưỡng → 403', d.status === 403, d);
  });

  await step('Q8', async () => {
    const t = await nowIso();
    const comp1 = [{ code: 'VALUE', dangerBelow: 40, warningBelow: 50, warningAbove: 100, dangerAbove: 130 }];
    const v1r = await post(U.B, '/thresholds', { scope: 'PERSONAL', residentId: C, indicatorId: IND.HEART_RATE, effectiveFrom: t, components: comp1, reason: 'Quickstart Q8 v1' });
    let list = (await get(U.B, `/thresholds?scope=PERSONAL&residentId=${C}&indicatorId=${IND.HEART_RATE}`)).body;
    const v1 = list.find((x) => x.status === 'EFFECTIVE');
    const rec = await vital(U.D, C, 'HEART_RATE', { VALUE: 105 });
    await advance('PT1M');
    const t2 = await nowIso();
    const v2r = await post(U.B, '/thresholds', { scope: 'PERSONAL', residentId: C, indicatorId: IND.HEART_RATE, effectiveFrom: t2, components: [{ ...comp1[0], warningAbove: 110 }], reason: 'Quickstart Q8 v2' });
    list = (await get(U.B, `/thresholds?scope=PERSONAL&residentId=${C}&indicatorId=${IND.HEART_RATE}`)).body;
    const old = list.find((x) => v1 && x.id === v1.id);
    const recNow = await readRecord(C, 'HEART_RATE', rec.body.id);
    check('Q8', 'phiên bản 2 hiệu lực ngay → phiên bản 1 hết hiệu lực cùng thời điểm; bản ghi cũ giữ kết quả so theo phiên bản 1',
      v1r.status < 300 && v2r.status < 300 && old && old.status !== 'EFFECTIVE' && old.effectiveUntil === (list.find((x) => x.status === 'EFFECTIVE') || {}).effectiveFrom && recNow && recNow.threshold && recNow.threshold.versionId === v1.id && recNow.level === 'WARNING',
      { v1r: v1r.status, v2r: [v2r.status, v2r.body], old, t2, recNow });
  });

  let sched;
  await step('Q10', async () => {
    const r = await post(U.B, `/residents/${A}/measurement-schedules`, { indicatorIds: [IND.BLOOD_PRESSURE], frequency: { type: 'CLOCK_TIMES', times: ['07:00', '17:00'] }, startAt: '2026-12-01T17:00:00Z', priority: 'MANDATORY' });
    const list = (await get(U.B, `/residents/${A}/measurement-schedules`)).body;
    sched = (list || []).find((x) => (x.indicatorIds || []).includes(IND.BLOOD_PRESSURE));
    const times = sched ? (await get(U.B, `/measurement-schedules/${sched.id}/measurement-times?from=2026-12-01T17:00:00Z&to=2026-12-02T17:00:00Z`)).body : null;
    check('Q10', 'B đặt lịch đo huyết áp 07:00, 17:00 từ mai → lịch có hiệu lực, mốc 07:00 và 17:00', r.status < 300 && sched && j(times) === j(['2026-12-02T00:00:00Z', '2026-12-02T10:00:00Z']),
      { r: r.status, sched, times });
    const tasks = L.sql(`SELECT COUNT(*) c FROM care_task WHERE resident_id = ${A}`)[0].c;
    check('Q10', 'không có công việc tự sinh từ lịch đo', +tasks === 0, { careTasks: tasks });
    manual('Q10', '005 chấp nhận công việc 07:00 (căn cứ LAN_DO) mức Bắt buộc, chặn 09:00', 'cần lập công việc ở 005 bằng màn của Người phụ trách ca; quickstart 005 kiểm');
  });

  await step('Q11', async () => {
    const pub0 = pubMark();
    const r = await post(U.B, `/measurement-schedules/${sched.id}/change`, { indicatorIds: [IND.BLOOD_PRESSURE], frequency: { type: 'CLOCK_TIMES', times: ['07:00', '12:00', '17:00'] }, startAt: '2026-12-01T17:00:00Z', priority: 'MANDATORY', reason: 'Quickstart Q11' });
    const list = (await get(U.B, `/residents/${A}/measurement-schedules`)).body;
    const old = list.find((x) => x.id === sched.id);
    const neu = list.find((x) => x.replaces === sched.id);
    check('Q11', 'đổi lịch sang 3 lần/ngày → lịch cũ Đã ngừng, lịch mới trỏ lịch cũ', r.status < 300 && old && old.status === 'STOPPED' && neu, { r: [r.status, r.body], list });
    const r2 = await post(U.B, `/residents/${C}/measurement-schedules`, { indicatorIds: [IND.SPO2], frequency: { type: 'CLOCK_TIMES', times: ['08:00'] }, startAt: '2026-12-01T17:00:00Z' });
    const other = ((await get(U.B, `/residents/${C}/measurement-schedules`)).body || [])[0];
    const s = await post(U.B, `/measurement-schedules/${other.id}/stop`, { reason: 'Quickstart Q11 ngừng' });
    const after = ((await get(U.B, `/residents/${C}/measurement-schedules`)).body || []).find((x) => x.id === other.id);
    check('Q11', 'B ngừng lịch khác → Đã ngừng kèm lý do', r2.status < 300 && s.status < 300 && after.status === 'STOPPED' && after.stopReason, { s: s.status, after });
    const ev = pubSince(pub0).map((x) => x.event_type.split('.').pop());
    manual('Q11', '005 nhận nguonDaDoi, nguonBiHuy', `sự kiện phát ra (event_publication): ${[...new Set(ev)].join(', ') || 'không có'}; cần xem công việc ở 005`);
  });

  await step('Q40', async () => {
    const t = new Date(await L.now()).getTime();
    const day = 86400000;
    const m = maxId('alert');
    const w0 = await vital(U.S, E, 'WEIGHT', { VALUE: 60 });
    check('Q40', 'lần cân đầu sau tiếp nhận (E) → không chạy quy tắc sút cân', w0.status === 201 && alertsSince(m, E).length === 0, w0.body);
    // Ba lần cân trong 25 ngày: không ghi lùi được (trực tuyến lỗi B1, ngoại tuyến chặn quá CFG-M15-08) nên chạy ở cuối
    // script, cân thật theo đồng hồ giả lập (weightTrend)
  });
}

async function weightTrend() {
  await step('Q40', async () => {
    const r1 = await vital(U.D, C, 'WEIGHT', { VALUE: 50.0 });
    await setTo('2026-12-15T09:00:00+07:00');
    await L.sleep(70000); // job ca (008, mỗi phút) mở ca mới sau khi tua
    // Tua nhiều ngày thì thiết bị coi như mất kết nối quá CFG-M15-08, bản ghi vào "chờ xem lại": đồng bộ rỗng trước
    await post(U.D, '/sync', { lastConnectedAt: await nowIso(), records: [] });
    const r2 = await vital(U.D, C, 'WEIGHT', { VALUE: 48.5 });
    await setTo('2026-12-28T09:00:00+07:00');
    await L.sleep(70000);
    await post(U.D, '/sync', { lastConnectedAt: await nowIso(), records: [] });
    const m2 = maxId('alert');
    const p0 = pubMark();
    const r3 = await vital(U.D, C, 'WEIGHT', { VALUE: 47.4 });
    const al = await waitFor(() => { const x = alertsSince(m2, C); return x.length ? x : null; }, 10000, 1000) || [];
    const ev = pubSince(p0).map((x) => x.event_type.split('.').pop());
    check('Q40', 'C cân 50,0 (03/12) → 48,5 (15/12) → 47,4 kg (28/12) → cảnh báo sút cân Trung bình; sự kiện cho 011',
      [r1, r2, r3].every((x) => x.status === 201) && al.length === 1 && al[0].level === 'MEDIUM' && /WEIGHT/i.test(al[0].type_lock + al[0].type) && ev.some((e) => /WeightLoss/i.test(e)),
      { status: [r1.status, r2.status, r3.status], reasons: [r1, r2, r3].map((x) => x.body.errorCode), sync: [r2, r3].map((x) => x.body.sync), alerts: al.map((a) => [a.type, a.type_lock, a.level]), events: ev });
  });
}

// ---------------- Cảnh báo: leo thang, tiếp nhận, giao ----------------
async function alerts() {
  await step('Q58', async () => {
    const ids = [];
    for (const [u, r] of [[U.D, A], [U.D, C], [U.T, F]]) ids.push((await newAlert(u, r, 'OTHER_INJURY')).body.id);
    await advance('PT1H');
    const t = await L.now();
    const rows = await waitFor(() => { const x = ids.map(alertRow); return x.every((a) => +a.escalation_level >= 1) ? x : null; });
    const all = ids.map(alertRow);
    const dl = all.map((a) => (ts(a.acknowledgement_deadline) - new Date(t)) / 60000);
    check('Q58', 'dừng job 1 giờ (tua 1 giờ một lần) với ba cảnh báo Trung bình → mỗi cảnh báo lên đúng một cấp, hạn mới tính từ lúc chạy lại',
      rows && all.every((a) => +a.escalation_level === 1) && dl.every((d) => d > 13 && d <= 16), { levels: all.map((a) => a.escalation_level), deadlineMinFromNow: dl });
    manual('Q58', 'nhắc bị lỡ gửi một lần', 'cần dừng backend thật; job nhắc của 009 không chạy theo runJobs');
    for (const id of ids) await closeAlertQuietly(id);
  });

  let a12, a13, a14;
  await step('Q12-Q14', async () => {
    a12 = (await newAlert(U.D, C, 'OTHER_HEALTH_CARE', 'MEDIUM', 'Q12')).body.id;
    a13 = (await newAlert(U.T, E, 'OTHER_HEALTH_CARE', 'MEDIUM', 'Q13')).body.id;
    a14 = (await newAlert(U.T, F, 'OTHER_HEALTH_CARE', 'MEDIUM', 'Q14')).body.id;
    const t0 = alertRow(a12).first_at;
    // +15 phút: cấp 1
    let n0 = maxId('notification_request');
    await advance('PT15M');
    await waitAlert(a12, (a) => +a.escalation_level >= 1);
    let x = alertRow(a12);
    let ns = who(await notesSince(n0, (r) => r.sid == a12));
    check('Q12', 'tua 15 phút → cấp 1, T và B nhận, hạn mới +15 phút', +x.escalation_level === 1 && has(ns, 'T', 'B') &&
      Math.abs((ts(x.acknowledgement_deadline) - ts(t0)) / 60000 - 30) < 1.5, { x: [x.escalation_level, x.acknowledgement_deadline, t0], ns: [...ns] });
    const q13a = await post(U.QL, `/alerts/${a13}/receive`, {});
    check('Q13', 'QL tiếp nhận khi cấp 1 → 403', q13a.status === 403, q13a);
    // +5 phút: T tiếp nhận a14 (09:20)
    await advance('PT5M');
    const rcv = await post(U.T, `/alerts/${a14}/receive`, {});
    // +10 phút: cấp 2
    n0 = maxId('notification_request');
    await advance('PT10M');
    await waitAlert(a12, (a) => +a.escalation_level >= 2);
    x = alertRow(a12);
    ns = who(await notesSince(n0, (r) => r.sid == a12));
    check('Q12', 'tua 30 phút → cấp 2, QL nhận', +x.escalation_level === 2 && ns.has('QL'), { level: x.escalation_level, ns: [...ns] });
    const y = alertRow(a14);
    check('Q14', 'T tiếp nhận sau 20 phút; tua tiếp → ĐÃ TIẾP NHẬN, không leo thang thêm; thời gian tới tiếp nhận 20 phút',
      rcv.status < 300 && y.status === 'RECEIVED' && +y.escalation_level === 1 && Math.abs((ts(y.received_at) - ts(y.first_at)) / 60000 - 20) < 1.5,
      { rcv: rcv.status, status: y.status, level: y.escalation_level, receivedAt: y.received_at, firstAt: y.first_at });
    const q13b = await post(U.QL, `/alerts/${a13}/receive`, {});
    const q13c = await post(U.QL, `/alerts/${a13}/assign`, { staffId: 9010, note: 'D2 xử lý giúp' });
    const det = (await get(U.QL, `/alerts/${a13}`)).body;
    const q13d = await post(U.QL, `/alerts/${a13}/close`, { resolution: 'QL thử đóng' });
    check('Q13', 'QL tiếp nhận khi cấp 2 → 200; giao D2 kèm ghi chú → D2 phụ trách; QL đóng → 403',
      q13b.status < 300 && q13c.status < 300 && +alertRow(a13).assignee_id === 16 && q13d.status === 403,
      { receive: q13b.status, assign: [q13c.status, q13c.body], assignee: alertRow(a13).assignee_id, assigneeView: det.assignee, close: q13d.status });
    // +15: leo thang tối đa, nhắc #1; +15: nhắc #2
    await advance('PT15M');
    await waitAlert(a12, (a) => a.max_escalation_at);
    await waitAlert(a12, (a) => +a.max_reminder_count >= 1, 75000);
    const x1 = alertRow(a12);
    await advance('PT15M');
    await waitAlert(a12, (a) => +a.max_reminder_count >= 2);
    const x2 = alertRow(a12);
    check('Q12', 'tua 45, 60 phút → dấu leo thang tối đa, có nhắc tối đa, không thêm cấp leo thang',
      x1.max_escalation_at && +x2.max_reminder_count >= 1 && +x2.escalation_level === 2, { x1: [x1.escalation_level, x1.max_escalation_at, x1.max_reminder_count], x2: [x2.escalation_level, x2.max_reminder_count] });
    manual('Q12', 'số thứ tự nhắc: quickstart ghi #1 lúc 09:45, #2 lúc 10:00', `backend: lúc 09:45 có ${x1.max_reminder_count} lần nhắc, lúc 10:00 có ${x2.max_reminder_count} (Q-248: nhắc "mỗi khoảng bằng thời hạn tiếp nhận" sau khi đạt tối đa); cần chủ dự án chốt cách đếm`);
  });

  await step('Q22', async () => {
    const r = await post(U.S, `/alerts/${a12}/receive`, {});
    check('Q22', 'Nhân viên chăm sóc tiếp nhận cảnh báo → 403', r.status === 403, r);
  });
  for (const id of [a12, a13, a14]) if (id) await closeAlertQuietly(id);

  await step('Q16', async () => {
    const n0 = maxId('notification_request');
    // Phát sinh có dòng nguồn (cảnh báo tạo tay không có): ba lần huyết áp mức Cảnh báo cho F (150/80)
    const m = maxId('alert');
    for (let i = 0; i < 3; i++) await vital(U.D2, F, 'BLOOD_PRESSURE', { SYSTOLIC: 150, DIASTOLIC: 80 });
    const al = await waitFor(() => { const x = alertsSince(m, F); return x.length && +x[0].occurrence_count === 3 ? x : null; }, 10000, 1000) || alertsSince(m, F);
    const a = al[0] || {};
    const rows = await notesSince(n0);
    const rep = rows.filter((r) => /REPEAT/i.test(r.et));
    check('Q16', 'ba phát sinh Trung bình cùng khóa trong 24 giờ → một cảnh báo, số lần 3, thông báo lặp lại cho Điều dưỡng phụ trách (của F: D2, phân công cả tầng 2) và Bác sĩ trực B',
      al.length === 1 && +a.occurrence_count === 3 && has(who(rep), 'B', 'D2'), { alerts: al.map((x) => [x.id, x.type_lock, x.level, x.occurrence_count]), notes: brief(rows) });
    const noReason = await post(U.D2, `/alerts/${a.id}/change-level`, { newLevel: 'CRITICAL' });
    check('Q16', 'Điều dưỡng nâng mức không lý do → 422', noReason.status === 422, noReason);
    const ok = await post(U.D2, `/alerts/${a.id}/change-level`, { newLevel: 'CRITICAL', reason: 'Diễn biến nặng hơn' });
    check('Q16', 'Điều dưỡng nâng mức có lý do → Khẩn cấp', ok.status < 300 && alertRow(a.id).level === 'CRITICAL', ok);
    await closeAlertQuietly(a.id);
  });

  await step('Q17', async () => {
    const id = (await newAlert(U.D, C, 'CHOKING_OR_AIRWAY_OBSTRUCTION')).body.id;
    await post(U.D, `/alerts/${id}/receive`, {});
    await post(U.D, `/alerts/${id}/start-handling`, {});
    const r1 = await post(U.D, `/alerts/${id}/close`, {});
    check('Q17', 'D đóng không kết quả → 422', r1.status === 422, r1);
    const r2 = await post(U.D, `/alerts/${id}/close`, { resolution: 'Đã xử lý' });
    check('Q17', 'D đóng có kết quả → ĐÃ ĐÓNG', r2.status < 300 && alertRow(id).status === 'CLOSED', r2);
    const id2 = (await newAlert(U.D, C, 'CHOKING_OR_AIRWAY_OBSTRUCTION')).body.id;
    check('Q17', 'phát sinh mới cùng khóa → cảnh báo mới', id2 && id2 !== id, { id, id2 });
    await closeAlertQuietly(id2);
  });

  manual('Q18', '008 khiXacNhanBanGiao chuyển cảnh báo của D sang điều dưỡng ca đêm', 'cần xác nhận bàn giao ca ở 008 (seed không có ca yêu cầu bàn giao)');
  manual('Q19', '18:00 bàn giao chưa xác nhận → tạm nhận', 'như Q18, cần dữ liệu bàn giao 008');
  manual('Q20', '005 gửi yêu cầu đóng DA_THUC_HIEN_DUNG_KHUNG', 'cần công việc xoay trở có cảnh báo ở 005');
}

// ---------------- Sự cố ----------------
async function incidents() {
  await step('Q23', async () => {
    const r = await newIncident(U.K, { residentId: A, type: 'OTHER_HEALTH_CARE', context: 'EATING', description: 'Bỏ bữa trưa', level: 'LOW', lowerLevelReason: 'Chỉ bỏ một bữa' });
    const id = r.body && r.body.incident && r.body.incident.id;
    const view = id ? (await get(U.QL, `/incidents/${id}`)).body : null;
    check('Q23', 'K (bếp, ngoài phạm vi) ghi sự cố Nhẹ → 201, phản hồi chỉ nhận dạng, ngoài phạm vi',
      r.status === 201 && r.body.identificationOnly === true && !r.body.emergencyCard && view && view.outOfScope === true, { status: r.status, body: r.body, outOfScope: view && view.outOfScope });
    const audit = id ? L.sql(`SELECT action, LEFT(data_after, 200) d FROM audit_log WHERE entity_type = 'M05.INCIDENT' AND entity_id = ${id}`) : [];
    manual('Q23', 'nhật ký có dấu ngoài phạm vi', `audit_log của sự cố ${id}: ${j(audit)}`);
  });

  await step('Q24', async () => {
    let n0 = maxId('notification_request');
    const r1 = await newIncident(U.S, { residentId: C, type: 'OTHER_HEALTH_CARE', context: 'CARE', description: 'Khó ngủ', level: 'LOW', lowerLevelReason: 'Không ảnh hưởng sức khỏe' });
    const id1 = r1.body.incident && r1.body.incident.id;
    const ns1 = who(await notesSince(n0, (r) => r.sid == id1));
    check('Q24', 'S ghi "khó ngủ" Nhẹ → D nhận', r1.status === 201 && ns1.has('D'), { r1: r1.status, ns: [...ns1] });
    n0 = maxId('notification_request');
    const p0 = pubMark();
    const r2 = await newIncident(U.D, { residentId: C, type: 'MEDICATION', context: 'MEDICATION', description: 'Từ chối thuốc', level: 'MEDIUM' });
    const id2 = r2.body.incident && r2.body.incident.id;
    const ns2 = who(await notesSince(n0, (r) => r.sid == id2));
    const ev = pubSince(p0);
    const recorded = ev.find((e) => /IncidentRecorded/.test(e.event_type));
    check('Q24', 'D ghi "từ chối thuốc" Trung bình → D, P nhận; 001 nhận sự kiện ghi nhận cần đánh giá lại',
      r2.status === 201 && has(ns2, 'D', 'P') && (ev.some((e) => /ReassessmentDue/.test(e.event_type)) || (recorded && /reassess[a-zA-Z]*"\s*:\s*true/i.test(recorded.s))),
      { ns: [...ns2], events: ev.map((e) => e.event_type.split('.').pop()), recorded: recorded && recorded.s.slice(0, 300) });
  });

  await step('Q26', async () => {
    const r = await newIncident(U.D, { residentId: C, type: 'OTHER_INJURY', context: 'CARE', description: 'Trầy tay', level: 'MEDIUM' });
    const id = r.body.incident.id;
    await post(U.D, `/incidents/${id}/receive`, {});
    const m1 = await post(U.D, `/incidents/${id}/move-to-monitoring`, {});
    check('Q26', 'chuyển theo dõi khi chưa có diễn biến xử lý → 409', m1.status === 409, m1);
    const t = await nowIso();
    const groups = [...new Set(PROG.map((p) => p.group))];
    for (const g of groups) {
      const p = PROG.find((x) => x.group === g && !x.resuscitation);
      if (p) await post(U.D, `/incidents/${id}/progressions`, { progressionTypeId: p.id, content: 'Diễn biến ' + g, occurredAt: t });
    }
    const m2 = await post(U.D, `/incidents/${id}/move-to-monitoring`, {});
    check('Q26', 'đủ diễn biến → ĐANG THEO DÕI', m2.status < 300 && incidentRow(id).status === 'MONITORING', { m2, groups });
    const o1 = await post(U.D, `/incidents/${id}/reopen`, {});
    check('Q26', 'mở lại không lý do → 422', o1.status === 422, o1);
    const o2 = await post(U.D, `/incidents/${id}/reopen`, { reason: 'Đau tăng' });
    check('Q26', 'mở lại có lý do → ĐANG XỬ LÝ', o2.status < 300 && incidentRow(id).status === 'IN_PROGRESS', o2);
  });

  let fallA, fallC;
  await step('Q35', async () => {
    const tasks0 = L.sql('SELECT COUNT(*) c FROM care_task')[0].c;
    const sch0 = L.sql(`SELECT COUNT(*) c FROM monitoring_setting`)[0].c;
    const bad = await newIncident(U.S, { residentId: A, type: 'FALL', context: 'MOBILITY', description: 'Ngã ở hành lang', level: 'LOW' });
    check('Q35', 'ngã mức Nhẹ (thấp hơn mặc định) không lý do → 400/422', bad.status === 400 || bad.status === 422, bad);
    const n0 = maxId('notification_request');
    const r = await newIncident(U.S, { residentId: A, type: 'FALL', context: 'MOBILITY', description: 'Ngã ở hành lang', level: 'LOW', lowerLevelReason: 'Không chấn thương' });
    fallA = r.body.incident && r.body.incident.id;
    const ns = who(await notesSince(n0));
    const win = fallRows(A).map((x) => { const c = JSON.parse(x.content); return { id: x.id, status: x.status, anchor: c.anchor, endsAt: c.endsAt, hours: (new Date(c.endsAt) - new Date(c.anchor)) / 3600000 }; });
    const tasks1 = L.sql('SELECT COUNT(*) c FROM care_task')[0].c;
    const sch1 = L.sql(`SELECT COUNT(*) c FROM monitoring_setting`)[0].c;
    check('Q35', 'có lý do → B, D, P nhận; mở khoảng theo dõi 72 giờ; không tạo lịch đo, công việc',
      r.status === 201 && has(ns, 'B', 'D', 'P') && win.length === 1 && win[0].hours === 72 && tasks0 === tasks1 && sch0 === sch1,
      { r: r.status, ns: [...ns], win, tasks: [tasks0, tasks1], monitoring: [sch0, sch1] });
  });

  await step('Q36', async () => {
    const tRec = await L.now();
    const r = await newIncident(U.D, { residentId: C, type: 'FALL', context: 'MOBILITY', description: 'Ngã trong đêm', level: 'MEDIUM', lowerLevelReason: 'Không chấn thương nặng', occurredAt: '2026-11-30T19:00:00Z' });
    fallC = r.body.incident && r.body.incident.id;
    const win = fallRows(C);
    const txt = j(win);
    const cw = win.length ? JSON.parse(win[0].content) : {};
    const gap = (new Date(tRec) - new Date(cw.anchor)) / 60000;
    check('Q36', 'ngã lúc 02:00, ghi lúc ' + String(tRec).slice(11, 16) + ' → mốc = lúc ghi − 4 giờ (chu kỳ đo), lần đo đầu lúc ghi; khoảng 72 giờ từ mốc',
      r.status === 201 && win.length === 1 && Math.abs(gap - cw.intervalMinutes) < 2 && (new Date(cw.endsAt) - new Date(cw.anchor)) / 3600000 === 72, { r: [r.status, code(r)], cw, gapMin: gap });
  });

  await step('Q38', async () => {
    const p0 = pubMark();
    const r = await post(U.B, `/incidents/${fallC}/end-post-fall-monitoring`, { reason: 'Ổn định, Bác sĩ kết thúc sớm' });
    const rows = fallRows(C);
    const p = pubSince(p0).map((x) => x.event_type.split('.').pop());
    check('Q38', 'B kết thúc theo dõi sớm → khoảng Kết thúc sớm', r.status < 300 && /EARLY|ENDED/i.test(j(rows)), { r: [r.status, r.body], rows });
    manual('Q38', '005 nhận nguonBiHuy(THEO_DOI_SAU_NGA)', `sự kiện gần nhất: ${[...new Set(p)].join(', ')}`);
  });

  await step('Q59', async () => {
    const r = await newIncident(U.S, { residentId: C, type: 'OTHER_HEALTH_CARE', context: 'CARE', description: 'Đau bụng', level: 'MEDIUM' });
    const id = r.body.incident.id;
    const n0 = maxId('notification_request');
    const a1 = await post(U.T, `/incidents/${id}/assign-handler`, { assigneeId: 5, note: 'D xử lý' });
    const ns = who(await notesSince(n0, (x) => x.sid == id));
    const i1 = incidentRow(id);
    check('Q59', 'T giao cho D kèm ghi chú → ĐANG XỬ LÝ, D nhận, dừng leo thang', a1.status < 300 && i1.status === 'IN_PROGRESS' && ns.has('D') && (+i1.escalation_stopped === 1 || i1.escalation_stopped === '1'),
      { a1: [a1.status, a1.body], i1: [i1.status, i1.assignee_id, i1.escalation_stopped], ns: [...ns] });
    const a2 = await post(U.QL, `/incidents/${id}/assign-handler`, { assigneeId: 4, note: 'Chuyển Bác sĩ' });
    check('Q59', 'QL giao lại cho Bác sĩ B → 200, trạng thái giữ', a2.status < 300 && incidentRow(id).status === 'IN_PROGRESS' && +incidentRow(id).assignee_id === 4, a2);
    const c = await post(U.QL, `/incidents/${id}/close`, { resolution: 'QL thử đóng' });
    check('Q59', 'QL đóng → 403', c.status === 403, c);
    const k = await post(U.K, `/incidents/${id}/assign-handler`, { assigneeId: 5, note: 'K thử giao' });
    check('Q59', 'K giao → 403', k.status === 403, k);
  });

  manual('Q31', 'phong bì ngoại tuyến tạo sự cố → TU_CHOI BAT_BUOC_TRUC_TUYEN', 'cần gửi phong bì đồng bộ từ app nhân viên (hàng đợi ngoại tuyến)');
  return { fallA };
}

// ---------------- Nút SOS ----------------
async function sosSteps() {
  let s;
  await step('S9', async () => {
    const t = await L.now();
    s = await sos(DEV_A, 'POST', '/sos', {});
    const alertId = s.body.alertId;
    const at = (sec) => iso(new Date(t).getTime() + sec * 1000);
    const l1 = await sos(DEV_A, 'POST', `/sos/${s.body.pressId}/location`, { latitude: 21.0285, longitude: 105.8542, accuracyMeters: 15, measuredAt: at(12) });
    await advance('PT30S');
    const l2 = await sos(DEV_A, 'POST', `/sos/${s.body.pressId}/location`, { latitude: 21.0286, longitude: 105.8543, accuracyMeters: 15, measuredAt: await nowIso() });
    const rows = L.sql(`SELECT COUNT(*) c FROM record_event WHERE kind LIKE '%LOCATION%' AND content LIKE '%${s.body.pressId}%'`)[0].c;
    const rows2 = L.sql(`SELECT kind, COUNT(*) c FROM record_event WHERE kind LIKE '%LOCATION%' GROUP BY kind`);
    const det = (await get(U.D, `/alerts/${alertId}`)).body;
    const cur = await sos(DEV_A, 'GET', '/sos/current');
    check('S9', 'SOS tạo cảnh báo ngay; hai lần gửi vị trí lưu; chi tiết có vị trí mới nhất (tham khảo); current guiViTri = true, chu kỳ 30 giây',
      s.status === 201 && alertId && l1.status < 300 && l2.status < 300 && det.latestLocation && cur.body.sendLocation === true && cur.body.locationIntervalSeconds === 30,
      { s: s.body, l: [l1.status, l2.status, l1.body], rowsForPress: rows, rows2, latestLocation: det.latestLocation, cur: cur.body });
  });

  await step('S10', async () => {
    await advance('PT61S');
    const det = (await get(U.D, `/alerts/${s.body.alertId}`)).body;
    check('S10', 'không gửi vị trí 61 giây → coTheDaCu = true', det.latestLocation && det.latestLocation.possiblyStale === true, det.latestLocation);
  });

  await step('S3', async () => {
    const r = await post(U.D, `/alerts/${s.body.alertId}/receive`, {});
    const cur = await sos(DEV_A, 'GET', '/sos/current');
    check('S3', 'D tiếp nhận; A xem /sos/current → ĐÃ TIẾP NHẬN kèm họ tên D', r.status < 300 && cur.body.status === 'RECEIVED' && /Điều dưỡng D\b|Điều dưỡng D$/.test(cur.body.receiverName || ''), { r: r.status, cur: cur.body });
    const p1 = await sos(DEV_A, 'POST', `/sos/${s.body.pressId}/place`, { place: 'RESTROOM' });
    const p2 = await sos(DEV_A, 'POST', `/sos/${s.body.pressId}/place`, { place: 'RESTROOM' });
    check('S3', 'chọn nơi NHA_VE_SINH hai lần → lần hai 409', p1.status < 300 && p2.status === 409, { p1: p1.status, p2 });
  });

  await step('S12', async () => {
    const a = await post(U.D, `/alerts/${s.body.alertId}/mark-arrived`, {});
    const cur = await sos(DEV_A, 'GET', '/sos/current');
    const loc = await sos(DEV_A, 'POST', `/sos/${s.body.pressId}/location`, { latitude: 21.0287, longitude: 105.8544, accuracyMeters: 10, measuredAt: await nowIso() });
    const a2 = await post(U.D, `/alerts/${s.body.alertId}/mark-arrived`, {});
    const row = alertRow(s.body.alertId);
    check('S12', 'D ghi Đã tới nơi → current ĐÃ TỚI kèm họ tên D, guiViTri = false; gửi vị trí 409 STOP_SENDING_LOCATION; ghi lần hai 409',
      a.status < 300 && cur.body.status === 'ARRIVED' && cur.body.sendLocation === false && loc.status === 409 && a2.status === 409,
      { a: a.status, cur: cur.body, loc: [loc.status, code(loc)], a2: a2.status });
    const mins = (ts(row.arrived_at) - ts(row.received_at)) / 60000;
    manual('S12', 'báo cáo thời gian tới nơi (FR-081)', `received_at ${row.received_at}, arrived_at ${row.arrived_at} (${mins.toFixed(1)} phút); xem màn báo cáo`);
  });

  await step('S6', async () => {
    const n0 = maxId('notification_request');
    await post(U.D, `/alerts/${s.body.alertId}/start-handling`, {});
    const c = await post(U.D, `/alerts/${s.body.alertId}/close`, { resolution: 'Hỗ trợ uống nước' });
    const rows = await notesSince(n0);
    const fam = rows.filter((r) => r.rt === 'FAMILY_MEMBER');
    const inc = alertRow(s.body.alertId).incident_id;
    check('S6', 'D đóng cảnh báo SOS "hỗ trợ uống nước" → không sự cố, người thân không được báo', c.status < 300 && !inc && fam.length === 0, { c: c.status, inc, fam: brief(fam) });
    const s2 = await sos(DEV_A, 'POST', '/sos', {});
    await post(U.D, `/alerts/${s2.body.alertId}/receive`, {});
    const n1 = maxId('notification_request');
    const act = await post(U.D, `/alerts/${s2.body.alertId}/activate-emergency`, { incidentTypeId: TYPE.BREATHING_OR_CONSCIOUSNESS, description: 'Khó thở sau SOS' });
    const incId = act.body && act.body.incident && act.body.incident.id;
    const ns = who(await notesSince(n1));
    check('S6', 'D kích hoạt khẩn cấp từ cảnh báo SOS → sự cố Khẩn cấp, người liên hệ chính M được báo', act.status < 300 && incId && act.body.incident.level === 'CRITICAL' && ns.has('M'),
      { act: [act.status, act.body && act.body.incident], ns: [...ns] });
    if (incId) await closeIncidentQuietly(incId);
  });

  await step('S7', async () => {
    const s3 = await sos(DEV_A, 'POST', '/sos', {});
    if (!s3.body || !s3.body.alertId) throw new Error('SOS: ' + j(s3.body));
    // BR-M05-01, FR-031: mức Khẩn cấp không leo thang theo cấp, mọi người nhận được báo cùng lúc; thông báo chưa xác nhận
    // do 009 nhắc theo BR-M13-02 (CFG-M13-01, 5 phút). Quickstart S7 ghi "leo thang theo BR-M05-01" nên kiểm theo BR.
    await advance('PT6M');
    await L.sleep(70000); // chờ job M05, M13 chạy ít nhất một lần
    const a = alertRow(s3.body.alertId);
    const ev = L.sql(`SELECT r.event_type, COUNT(n.id) c FROM notification_request r LEFT JOIN notification n ON n.request_id = r.id WHERE r.source_record_id = '${s3.body.alertId}' GROUP BY r.event_type`);
    check('S7', 'SOS không ai tiếp nhận quá 5 phút → không có hạn, không lên cấp (BR-M05-01: Khẩn cấp báo đồng thời mọi người)', !a.acknowledgement_deadline && +a.escalation_level === 0, { a: [a.acknowledgement_deadline, a.escalation_level, a.status] });
    manual('S7', 'thông báo Khẩn cấp chưa xác nhận được nhắc theo BR-M13-02 (009)', `sự kiện thông báo của cảnh báo: ${j(ev)}; quickstart S7 ghi "leo thang" lệch BR-M05-01`);
    await closeAlertQuietly(s3.body.alertId, U.D);
  });

  await step('S4', async () => {
    const r = await sos(DEV_B, 'POST', '/sos', {});
    check('S4', 'B bán trú chưa đến (thứ 3, lịch thứ 2, 4, 6) bấm SOS → 409 NOT_PRESENT kèm lienHe', r.status === 409 && code(r) === 'NOT_PRESENT' && r.body.contacts, r);
    manual('S4', 'A Tạm vắng bấm SOS → 409 KHONG_O_VIEN', 'đã có FR_047d_SosLocationConditionIT; cần lệnh Cho tạm vắng của 001 trên dữ liệu demo');
  });
  manual('S5', 'A ở Hoạt động bên ngoài: trưởng đoàn, người đi cùng nhận thêm', 'cần chuyến đi đang diễn ra ở 014');
  manual('S11', 'Hoạt động bên ngoài: vị trí chính; dữ liệu cho 012 không có vị trí', 'cần chuyến đi 014 và cổng người thân');
}

// ---------------- Nút hỗ trợ khẩn cấp ----------------
async function assistSteps() {
  let h1;
  await step('H1', async () => {
    const n0 = maxId('notification_request');
    const r = await post(U.S, '/emergency-assist', {});
    h1 = r.body && r.body.alertId;
    const a = h1 && alertRow(h1);
    const rows = await notesSince(n0);
    const ns = who(rows);
    check('H1', 'S (trong ca tầng 2) bấm → cảnh báo Khẩn cấp không người cao tuổi, tầng 2; B, P, nhân viên ca tầng 2 nhận; không người thân',
      r.status === 201 && a && a.level === 'CRITICAL' && !a.resident_id && +a.floor_area_id === 1002 && has(ns, 'B', 'P', 'D') && rows.every((x) => x.rt !== 'FAMILY_MEMBER'),
      { r: r.body, a: a && [a.type, a.level, a.resident_id, a.floor_area_id], ns: brief(rows) });
  });

  await step('H4', async () => {
    const again = await post(U.S, '/emergency-assist', {});
    check('H4', 'S bấm lần hai khi cảnh báo đang mở → gộp (số lần 2)', again.status < 300 && again.body.alertId === h1 && +alertRow(h1).occurrence_count === 2, { again: again.body, count: alertRow(h1).occurrence_count });
    const out = await post(U.DD, '/emergency-assist', {});
    check('H4', 'Điều dưỡng ca đêm bấm ban ngày (ngoài ca) → 409 NOT_ON_SHIFT kèm lienHe', out.status === 409 && code(out) === 'NOT_ON_SHIFT' && out.body.contacts, out);
  });

  await step('H2', async () => {
    const n0 = maxId('notification_request');
    const d1 = await post(U.S, `/emergency-assist/${h1}/details`, { eventType: 'FALL', place: 'RESTROOM', residentId: A });
    const ns = who(await notesSince(n0));
    const d2 = await post(U.S, `/emergency-assist/${h1}/details`, { eventType: 'FALL', place: 'RESTROOM', residentId: C });
    check('H2', 'S gửi chi tiết NGÃ, nhà vệ sinh, A → cảnh báo gắn A, thêm nhóm FR-047a (D, T); lần hai 409',
      d1.status < 300 && +alertRow(h1).resident_id === A && has(ns, 'D', 'T') && d2.status === 409, { d1: d1.status, resident: alertRow(h1).resident_id, ns: [...ns], d2: [d2.status, code(d2)] });
  });

  await step('H5', async () => {
    const r = await post(U.D2, `/alerts/${h1}/receive`, {});
    const m = await post(U.D2, `/alerts/${h1}/mark-arrived`, {});
    const cur = await get(U.S, '/emergency-assist/current');
    check('H5', 'D2 tiếp nhận, ghi đã tới nơi; S xem /emergency-assist/current → ĐÃ TỚI kèm họ tên D2', r.status < 300 && m.status < 300 && cur.body && cur.body.status === 'ARRIVED' && /D2/.test(cur.body.receiverName || ''),
      { r: r.status, m: m.status, cur: cur.body });
  });

  await step('H8', async () => {
    const c = await post(U.D2, `/alerts/${h1}/call-115`, {});
    const res = c.body && c.body.id ? await post(U.D2, `/call-115-attempts/${c.body.id}/result`, { result: 'CONNECTED' }) : null;
    const act = await post(U.D2, `/alerts/${h1}/activate-emergency`, { incidentTypeId: TYPE.FALL, description: 'Ngã, bất tỉnh' });
    const incId = act.body && act.body.incident && act.body.incident.id;
    const progs = incId ? (await get(U.D2, `/incidents/${incId}`)).body.progressions : null;
    check('H8', 'cảnh báo Khẩn cấp chưa có sự cố: gọi 115 Đã gọi được, rồi kích hoạt → sự cố mới có diễn biến gọi cấp cứu',
      c.status < 300 && res && res.status < 300 && act.status < 300 && j(progs || []).includes(String(TYPE.CALL_EMERGENCY)) || /CALL_EMERGENCY|cấp cứu/i.test(j(progs || [])),
      { call: [c.status, c.body], res: res && res.status, act: [act.status, code(act)], progs });
    if (incId) await closeIncidentQuietly(incId);
  });

  await step('H9', async () => {
    const r = await post(U.S, '/emergency-assist', {});
    const id = r.body.alertId;
    await post(U.D2, `/alerts/${id}/receive`, {});
    await post(U.D2, `/alerts/${id}/start-handling`, {});
    const c = await post(U.D2, `/alerts/${id}/close`, { resolution: 'Đã hỗ trợ, không cần thêm' });
    const det = (await get(U.D2, `/alerts/${id}`)).body;
    check('H9', 'đóng cảnh báo nút hỗ trợ khi chưa ghi đã tới nơi → khongGhiMocToiNoi = true', c.status < 300 && det.skipArrivalTime === true, { c: c.status, skip: det.skipArrivalTime });
  });

  let h3;
  await step('H3', async () => {
    const n0 = maxId('notification_request');
    const r = await post(U.K, '/emergency-assist', {});
    h3 = r.body && r.body.alertId;
    const ns1 = who(await notesSince(n0));
    const n1 = maxId('notification_request');
    const d = h3 ? await post(U.K, `/emergency-assist/${h3}/details`, { eventType: 'OTHER', floorAreaId: 1003, place: 'CORRIDOR' }) : null;
    const ns2 = who(await notesSince(n1));
    check('H3', 'K (không tầng) bấm → Người phụ trách ca mọi tầng có ca và Bác sĩ trực; chi tiết tầng 3 → thêm nhân viên ca tầng 3',
      r.status === 201 && has(ns1, 'P', 'D5', 'B') && d && d.status < 300 && ns2.has('D5'), { r: [r.status, r.body], ns1: [...ns1], d: d && d.status, ns2: [...ns2] });
  });

  await step('H6', async () => {
    if (!h3) { const r = await post(U.S, '/emergency-assist', {}); h3 = r.body.alertId; }
    await post(U.B, `/alerts/${h3}/receive`, {});
    const a1 = await post(U.B, `/alerts/${h3}/activate-emergency`, { incidentTypeId: TYPE.BREATHING_OR_CONSCIOUSNESS, description: 'Bất tỉnh' });
    check('H6', 'kích hoạt khẩn cấp không chọn người cao tuổi → 422 RESIDENT_REQUIRED', a1.status === 422 && code(a1) === 'RESIDENT_REQUIRED', a1);
    const n0 = maxId('notification_request');
    const a2 = await post(U.B, `/alerts/${h3}/activate-emergency`, { incidentTypeId: TYPE.BREATHING_OR_CONSCIOUSNESS, description: 'Bất tỉnh', residentId: E });
    const ns = who(await notesSince(n0));
    const incId = a2.body && a2.body.incident && a2.body.incident.id;
    check('H6', 'có người cao tuổi (E) → sự cố Khẩn cấp của E, người liên hệ chính của E được báo', a2.status < 300 && incId && a2.body.incident.level === 'CRITICAL' && ns.has('KF'),
      { a2: [a2.status, code(a2)], ns: [...ns] });
    if (incId) await closeIncidentQuietly(incId);
  });
}

// ---------------- Sự cố Khẩn cấp, gọi 115 ----------------
async function emergency() {
  let i27;
  await step('Q27', async () => {
    const n0 = maxId('notification_request');
    const t0 = Date.now();
    const r = await newIncident(U.D, { residentId: A, type: 'BREATHING_OR_CONSCIOUSNESS', context: 'CARE', description: 'Khó thở', level: 'CRITICAL' });
    const ms = Date.now() - t0;
    i27 = r.body.incident && r.body.incident.id;
    const card = r.body.emergencyCard || {};
    const c2 = await get(U.D, `/incidents/${i27}/emergency-card`);
    const rows = await notesSince(n0);
    const ns = who(rows);
    const toM = rows.filter((x) => x.rt === 'FAMILY_MEMBER' && x.rcp == 4001);
    const keys = ['identification', 'endOfLifeWish', 'allergies', 'currentMedications', 'chronicConditions', 'careNotes', 'primaryContact'];
    check('Q27', 'D tạo sự cố Khẩn cấp "khó thở" cho A → thẻ đủ mục ≤ 3 giây, bản chụp; B, T, QL, M, D, P nhận',
      r.status === 201 && keys.every((k) => k in card) && card.snapshotAt && c2.ms <= 3000 && has(ns, 'B', 'T', 'QL', 'M', 'D', 'P'),
      { status: r.status, ms, cardMs: Math.round(c2.ms), cardKeys: Object.keys(card), allergies: card.allergies, wish: card.endOfLifeWish, ns: [...ns] });
    check('Q27', 'nội dung cho M chỉ tối thiểu (không phần sức khỏe)', toM.length && toM.every((x) => !/HEALTH/.test(x.vp || '')), toM);
    check('K1', 'M không thuộc bản đồng ý → yêu cầu mang VE_NGUOI_CAO_TUOI, mã KHAN_CAP_NGUOI_LIEN_HE_CHINH',
      toM.length && toM.every((x) => x.subj === 'ABOUT_RESIDENT' && (x.ncase === 'PRIMARY_CONTACT_EMERGENCY' || x.rcase === 'PRIMARY_CONTACT_EMERGENCY')), toM);
  });

  await step('Q33', async () => {
    const r = await newIncident(U.D2, { residentId: A, type: 'BREATHING_OR_CONSCIOUSNESS', context: 'CARE', description: 'Khó thở (lần hai)', level: 'CRITICAL', createDespiteOpen: false });
    check('Q33', 'kích hoạt khẩn cấp lần hai khi đang mở → 409 OPEN_EMERGENCY_INCIDENT_EXISTS', r.status === 409 && code(r) === 'OPEN_EMERGENCY_INCIDENT_EXISTS', r);
    const jn = await post(U.D2, `/incidents/${i27}/join-handling`, {});
    const v = (await get(U.D2, `/incidents/${i27}`)).body;
    check('Q33', 'D2 tham gia xử lý → có trong người xử lý', jn.status < 300 && /D2|\b16\b|9010/.test(j(v.handlers)), { jn: [jn.status, jn.body], handlers: v.handlers, keys: Object.keys(v) });
  });

  await step('H7', async () => {
    const info = await get(U.D2, `/incidents/${i27}/call-115-info`);
    const b = info.body || {};
    check('H7', 'thông tin gọi 115: địa chỉ viện, tầng, nơi, họ tên, tuổi, dị ứng', info.status === 200 && b.facility && b.floor && b.resident && b.resident.fullName && b.resident.age && (b.allergies || []).some((x) => /Penicillin/.test(x)), b);
    const c = await post(U.D2, `/incidents/${i27}/call-115`, {});
    const res = c.body && c.body.id ? await post(U.D2, `/call-115-attempts/${c.body.id}/result`, { result: 'CONNECTED' }) : null;
    const progs = (await get(U.D2, `/incidents/${i27}`)).body.progressions;
    check('H7', 'gọi 115, chọn Đã gọi được → lượt gọi có diễn biến gọi cấp cứu ở sự cố', c.status < 300 && res && res.status < 300 && /CALL_EMERGENCY|65/.test(j(progs)), { c: [c.status, c.body], res: res && res.status, progs });
    const sInfo = await get(U.S, `/incidents/${i27}/call-115-info`);
    if (sInfo.status === 200 && (sInfo.body.hiddenFields || []).some((f) => /allerg/i.test(f))) check('H7', 'Nhân viên chăm sóc xem thông tin → không thấy dị ứng (hiddenFields)', true);
    else manual('H7', 'Nhân viên chăm sóc xem thông tin → không thấy dị ứng (truongBiAn)', `S thấy dị ứng ${j(sInfo.body.allergies)}, hiddenFields ${j(sInfo.body.hiddenFields)}: FR-047k ẩn dị ứng với người không có quyền xem sức khỏe, còn S trong phạm vi được xem (FieldVisibility FULL); quickstart giả định S không có quyền, cần đối chiếu Phụ lục 27`);
  });
  return i27;
}

// ---------------- Lây nhiễm ----------------
async function infection() {
  let inf, iso1;
  await step('Q44', async () => {
    const n0 = maxId('notification_request');
    const r = await newIncident(U.D, { residentId: A, type: 'SUSPECTED_INFECTION', context: 'CARE', description: 'Sốt, ho', level: 'MEDIUM' });
    inf = r.body.incident && r.body.incident.id;
    const ns = who(await notesSince(n0));
    const lk = await get(U.D, `/incidents/${inf}/contact-lookup`);
    const groups = (lk.body && lk.body.group) || [];
    const people = (g) => j((groups.find((x) => x.group === g) || {}).people || []);
    check('Q44', 'D ghi sự cố lây nhiễm cho A → D và Bác sĩ nhận yêu cầu lập danh sách; tra cứu trả E cùng phòng, nhân viên chăm sóc theo phân công; không có danh sách tự lập',
      r.status === 201 && has(ns, 'D', 'B') && lk.status === 200 && /Lê Thị E|"subjectId":2003|2003/.test(people('SAME_ROOM')) && /9005|9006/.test(people('CARE_STAFF')) &&
      (await get(U.D, `/incidents/${inf}/contact-list`)).status !== 200,
      { r: [r.status, code(r), r.body && r.body.detail], ns: [...ns], lookup: lk.status === 200 ? { from: lk.body.traceFrom, to: lk.body.traceTo, groups: groups.map((g) => [g.group, (g.people || []).length, g.noData]) } : lk });
    manual('Q44', 'người cùng buổi hoạt động, người thân thăm, khoảng vắng của A', 'seed không có buổi hoạt động (014), lượt thăm (012), lượt vắng (001)');
  });

  let list;
  await step('Q48', async () => {
    const cr = await post(U.D, `/incidents/${inf}/contact-list`, {});
    list = (await get(U.D, `/incidents/${inf}/contact-list`)).body;
    const lk = (await get(U.D, `/incidents/${inf}/contact-lookup`)).body;
    const eRow = ((lk.group || []).find((g) => g.group === 'SAME_ROOM') || { people: [] }).people.find((p) => +p.subjectId === E);
    const add = await post(U.D, `/contact-lists/${list.id}/contacts`, eRow || { type: 'RESIDENT', subjectId: E, group: 'SAME_ROOM', source: 'Cùng phòng', contactFrom: lk.traceFrom, contactTo: lk.traceTo });
    await post(U.D, `/contact-lists/${list.id}/contacts`, { type: 'FAMILY_MEMBER', subjectId: 4003, fullName: 'Người thân N', group: 'OTHER', source: 'Nhân viên ghi', contactFrom: lk.traceFrom, contactTo: lk.traceTo, reason: 'Thăm hôm qua' });
    const cf = await post(U.D, `/contact-lists/${list.id}/confirm`, {});
    list = (await get(U.D, `/incidents/${inf}/contact-list`)).body;
    const e = (list.contacts || []).find((x) => +x.subjectId === E && x.type === 'RESIDENT');
    const p0 = pubMark();
    const ex = e ? await post(U.D, `/contact-lists/${list.id}/contacts/${e.id}/exclude`, { reason: 'Không ở phòng trong khoảng truy vết' }) : null;
    const after = ((await get(U.D, `/incidents/${inf}/contact-list`)).body.contacts || []).find((x) => e && x.id === e.id);
    const ev = pubSince(p0).map((x) => x.event_type.split('.').pop());
    check('Q48', 'loại E khỏi danh sách đã xác nhận → E có thời điểm bị loại (gỡ dấu tiếp xúc)', cr.status < 300 && add.status < 300 && cf.status < 300 && ex && ex.status < 300 && after && after.excludedAt,
      { cr: cr.status, add: [add.status, code(add)], cf: [cf.status, code(cf)], ex: ex && [ex.status, code(ex)], after });
    manual('Q48', '005 nhận nguonBiHuy(THEO_DOI_TIEP_XUC)', `sự kiện phát ra: ${[...new Set(ev)].join(', ') || 'không có'}`);
  });

  await step('Q47', async () => {
    const r = await post(U.B, '/isolations', { scope: { floorIds: [1002] }, incidentIds: [inf], reason: 'Nghi cúm tầng 2' });
    const lst = (await get(U.B, '/isolations')).body;
    iso1 = (Array.isArray(lst) ? lst : lst.items || []).find((x) => (x.incidentIds || []).includes(inf));
    check('Q46', 'B khoanh vùng tầng 2 gắn sự cố A → vùng đang hiệu lực', r.status < 300 && iso1, { r: [r.status, r.body], lst });
    manual('Q46', '003, 012, 014 chặn phân bổ giường P203, đăng ký thăm, đăng ký hoạt động', 'kiểm ở quickstart 003, 012, 014 khi vùng còn hiệu lực');
    await post(U.D, `/incidents/${inf}/receive`, {});
    const cl = await post(U.D, `/incidents/${inf}/close`, { resolution: 'Loại trừ', infectionOutcome: 'RULED_OUT' });
    check('Q47', 'D đóng sự cố khi vùng chỉ gắn sự cố này còn → 409 ISOLATION_ONLY_FOR_THIS_INCIDENT', cl.status === 409 && code(cl) === 'ISOLATION_ONLY_FOR_THIS_INCIDENT', cl);
    const t = await post(U.T, `/isolations/${iso1.id}/lift`, { reason: 'T thử gỡ' });
    check('Q47', 'Trưởng tầng gỡ vùng → 403', t.status === 403, t);
    const n0 = maxId('notification_request');
    const b = await post(U.B, `/isolations/${iso1.id}/lift`, { reason: 'Hết nguy cơ' });
    const rows = await notesSince(n0);
    check('Q47', 'B gỡ có lý do → vùng đã gỡ, có thông báo gỡ', b.status < 300 && rows.length > 0, { b: [b.status, b.body], notes: brief(rows) });
  });
}

// ---------------- Nguy kịch ----------------
async function critical() {
  await step('Q54', async () => {
    const r = await post(U.D, `/residents/${C}/critical-flags`, { type: 'PROVISIONAL', assessment: 'Thở yếu' });
    check('Q54', 'ca ngày D ghi dấu tạm → 409 ON_DUTY_DOCTOR_AVAILABLE', r.status === 409 && code(r) === 'ON_DUTY_DOCTOR_AVAILABLE', r);
  });

  await step('Q55', async () => {
    const f = await post(U.BS1, `/residents/${F}/critical-flags`, { type: 'OFFICIAL', assessment: 'Suy hô hấp' });
    const al = L.sql(`SELECT * FROM alert WHERE resident_id = ${F} AND critical_flag_id IS NOT NULL ORDER BY id DESC LIMIT 1`)[0];
    // Đóng khi chưa xác nhận lại kiểm ở cảnh báo của E (Q56). Sửa B2 (chủ dự án chốt 2026-11-12, US11 #4): cảnh báo Đang
    // xử lý chưa có sự cố, lệnh ghi lựa chọn "chuyển bệnh viện" lập sự cố loại do người xử lý chọn rồi Chuyển viện (FR-049)
    await post(U.B, `/alerts/${al.id}/receive`, {});
    await post(U.B, `/alerts/${al.id}/start-handling`, {});
    const t = await nowIso();
    const rc = await post(U.B, `/alerts/${al.id}/wish-reconfirmation`, { contactedPerson: { fullName: 'Đỗ Văn F' }, channel: 'IN_PERSON', contactedAt: t, result: 'KEPT' });
    const act = { status: 200 }; // không còn bước kích hoạt khẩn cấp trước khi chuyển viện
    const ex = await post(U.B, `/alerts/${al.id}/execute-choice`, { choice: 'TRANSFER_TO_HOSPITAL', incidentTypeId: TYPE.BREATHING_OR_CONSCIOUSNESS, transferOut: { transferUnit: 'Bệnh viện X', transferAt: t, companion: 'Điều dưỡng D', handoverStaffId: 9005, requiredContactReached: true } });
    const res = L.sql(`SELECT status FROM resident WHERE id = ${F}`)[0].status;
    const flag = L.sql(`SELECT critical_flag_id FROM alert WHERE id = ${al.id}`)[0];
    const sum = L.sql(`SELECT COUNT(*) c FROM record_event WHERE kind LIKE '%TRANSFER_SUMMARY%' AND entity_id = ${F}`)[0].c;
    const flagRow = L.sql(`SELECT status FROM simple_record WHERE kind = 'M05.CRITICAL_FLAG' AND resident_id = ${F} ORDER BY id DESC LIMIT 1`)[0];
    const c2 = await post(U.B, `/alerts/${al.id}/close`, { resolution: 'Đã chuyển viện' });
    check('Q55', 'xác nhận lại (giữ) + lựa chọn chuyển bệnh viện → F Điều trị tại bệnh viện, có bản tóm tắt; đóng cảnh báo được',
      f.status < 300 && rc.status < 300 && act.status < 300 && ex.status < 300 && /HOSPITAL/.test(res) && c2.status < 300, { rc: [rc.status, code(rc)], act: [act.status, code(act)], ex: [ex.status, ex.body], res, flag, flagRow, summaries: sum, c2: [c2.status, code(c2)], alert: alertRow(al.id).status });
    manual('Q55', '"nguyện vọng mới" → 001 nhận ghiPhienBan; dấu nguy kịch tự gỡ', 'ghi nguyện vọng mới cần tệp bản ký (đã có BR_M05_16_WishReconfirmationIT); dấu nguy kịch: xem màn nguy kịch của F');
  });

  await step('Q56', async () => {
    const n0 = maxId('notification_request');
    const f = await post(U.BS1, `/residents/${A}/critical-flags`, { type: 'OFFICIAL', assessment: 'Nguy kịch' });
    const rowsFlag = await notesSince(n0);
    const al = L.sql(`SELECT * FROM alert WHERE resident_id = ${A} AND critical_flag_id IS NOT NULL ORDER BY id DESC LIMIT 1`)[0];
    const toR = rowsFlag.filter((x) => x.rt === 'FAMILY_MEMBER' && x.rcp == 4002);
    check('K2', 'dấu nguy kịch: người đại diện R nhận yêu cầu mã XAC_NHAN_LAI_NGUYEN_VONG', f.status < 300 && toR.length && toR.some((x) => x.ncase === 'WISH_RECONFIRMATION' || x.rcase === 'WISH_RECONFIRMATION'), { f: f.status, toR, all: brief(rowsFlag) });
    const t = await nowIso();
    const rc = await post(U.B, `/alerts/${al.id}/wish-reconfirmation`, { contactedPerson: { familyMemberId: 4001, fullName: 'Người thân M' }, channel: 'PHONE_CALL', contactedAt: t, result: 'UNREACHABLE' });
    const fE = await post(U.BS1, `/residents/${E}/critical-flags`, { type: 'OFFICIAL', assessment: 'Nguy kịch' });
    const alE = L.sql(`SELECT id FROM alert WHERE resident_id = ${E} AND critical_flag_id IS NOT NULL ORDER BY id DESC LIMIT 1`)[0];
    await post(U.B, `/alerts/${alE.id}/receive`, {});
    await post(U.B, `/alerts/${alE.id}/start-handling`, {});
    const c1 = await post(U.B, `/alerts/${alE.id}/close`, { resolution: 'Ổn định' });
    check('Q55', 'đóng cảnh báo nguy kịch (E) chưa có kết quả xác nhận lại → 409 WISH_NOT_RECONFIRMED', fE.status < 300 && c1.status === 409 && code(c1) === 'WISH_NOT_RECONFIRMED', { fE: fE.status, c1 });
    const rcE = await post(U.B, `/alerts/${alE.id}/wish-reconfirmation`, { contactedPerson: { familyMemberId: 4004, fullName: 'Người thân K' }, channel: 'PHONE_CALL', contactedAt: t, result: 'UNREACHABLE' });
    const pal = await post(U.B, `/alerts/${alE.id}/execute-choice`, { choice: 'STAY_PALLIATIVE' });
    const pal2 = await post(U.B, `/alerts/${alE.id}/execute-choice`, { choice: 'STAY_PALLIATIVE', reason: 'Không liên lạc được, chưa có nguyện vọng; Bác sĩ quyết định giảm nhẹ' });
    check('Q56', 'E chưa có nguyện vọng, không liên lạc được: chọn giảm nhẹ không lý do → 422; có lý do → ghi được', rc.status < 300 && fE.status < 300 && rcE.status < 300 && pal.status === 422 && pal2.status < 300, { rc: rc.status, fE: fE.status, rcE: [rcE.status, code(rcE)], pal: [pal.status, code(pal)], pal2: [pal2.status, code(pal2)] });
    const abs0 = L.sql(`SELECT COUNT(*) c FROM absence WHERE resident_id = ${A}`)[0].c;
    const n1 = maxId('notification_request');
    const home = await post(U.B, `/alerts/${al.id}/execute-choice`, { choice: 'RETURN_HOME' });
    const ns = who(await notesSince(n1));
    const abs1 = L.sql(`SELECT COUNT(*) c FROM absence WHERE resident_id = ${A}`)[0].c;
    check('Q56', 'lựa chọn "về nhà" → H, T nhận yêu cầu cho tạm vắng về nhà; không có lượt vắng tự tạo', home.status < 300 && has(ns, 'H', 'T') && abs0 === abs1, { home: [home.status, code(home)], ns: [...ns], abs: [abs0, abs1] });
  });
}

// ---------------- Đính chính, hủy, quy tắc ----------------
async function corrections(fallA) {
  await step('Q52', async () => {
    const m = maxId('alert');
    const r = await vital(U.S, E, 'BLOOD_PRESSURE', { SYSTOLIC: 180, DIASTOLIC: 100 }, { immediateAction: true });
    const al = (await waitFor(() => { const x = alertsSince(m, E); return x.length ? x : null; }, 8000, 1000) || [])[0];
    // có người phụ trách để nhận "nguồn đã được đính chính": Trưởng tầng tiếp nhận
    if (al) await post(U.T, `/alerts/${al.id}/receive`, {});
    const n0 = maxId('notification_request');
    const c = await post(U.S, `/corrections/M06.VITAL_SIGN_RECORD/${r.body.id}`, { type: 'AMEND_CONTENT', correctedContent: { values: { SYSTOLIC: 130, DIASTOLIC: 80 } }, reason: 'Nhập nhầm' });
    const v = await readRecord(E, 'BLOOD_PRESSURE', r.body.id);
    const rows = await notesSince(n0, (x) => x.rt === 'STAFF');
    check('Q52', 'S đính chính 180/100 → 130/80: hiện hành 130/80, gốc còn; cảnh báo không tự đóng; người phụ trách nhận "nguồn đã được đính chính"',
      c.status < 300 && v && +v.values.SYSTOLIC === 130 && v.originalValues && +v.originalValues.SYSTOLIC === 180 && al && alertRow(al.id).status !== 'CLOSED' && who(rows).has('T'),
      { c: c.status, v: v && [v.values, v.originalValues], alert: al && alertRow(al.id).status, notes: brief(rows) });
    if (al) await closeAlertQuietly(al.id);
  });

  await step('Q39', async () => {
    const r = await newIncident(U.D, { residentId: C, type: 'FALL', context: 'MOBILITY', description: 'Ghi nhầm người', level: 'MEDIUM', lowerLevelReason: 'Không chấn thương nặng', createDespiteOpen: true });
    if (!r.body.incident) throw new Error('ghi sự cố: ' + j(r.body));
    const id = r.body.incident.id;
    const ev0 = maxId('record_event');
    const n0 = maxId('notification_request');
    const v = await post(U.D, `/corrections/M05.INCIDENT/${id}`, { type: 'VOID', reason: 'Ghi nhầm người' });
    const st = (await get(U.D, `/incidents/${id}`)).body.status;
    const rows = await notesSince(n0);
    const win = L.sql(`SELECT kind FROM record_event WHERE kind LIKE '%FALL%' AND entity_id = ${C} AND id <= ${ev0} ORDER BY id DESC LIMIT 1`);
    check('Q39', 'D hủy ghi nhận sự cố ngã ghi nhầm người → ĐÃ HỦY; D và Bác sĩ nhận danh sách tác động',
      v.status < 300 && st === 'VOIDED' && has(who(rows), 'D', 'B'), { v: [v.status, v.body], st, notes: brief(rows), win });
  });

  await step('Q43', async () => {
    // GET /trend-rules không trả ETag, phiên bản đọc từ bảng
    const ver = () => (L.sql("SELECT version FROM catalog_item WHERE catalog_type = 'M05.TREND_RULE' AND code = 'PROLONGED_INSOMNIA'")[0] || {}).version || 0;
    const r = await call(U.QL, 'PUT', '/trend-rules/PROLONGED_INSOMNIA', { enabled: false, level: 'LOW', reason: 'Tạm tắt theo yêu cầu Bác sĩ' }, { 'If-Match': `"${ver()}"` });
    const audit = L.sql(`SELECT * FROM audit_log ORDER BY id DESC LIMIT 3`);
    check('Q43', 'QL tắt quy tắc mất ngủ có lý do → 200, nhật ký có lý do', r.status < 300 && /Tạm tắt theo yêu cầu/.test(j(audit)), { r: [r.status, r.body], audit: j(audit).slice(0, 400) });
    manual('Q43', 'ba đêm mất ngủ → không cảnh báo', 'cần 005 ghi kết quả ngủ ba đêm (FR_039c_ProlongedInsomniaIT đã kiểm quy tắc tắt)');
    await call(U.QL, 'PUT', '/trend-rules/PROLONGED_INSOMNIA', { enabled: true, level: 'LOW', reason: 'Bật lại sau nghiệm thu' }, { 'If-Match': `"${ver()}"` });
  });
}

// ---------------- Ca đêm, ngày 3 ----------------
async function later(fallA) {
  await setTo('2026-12-01T19:30:00+07:00');
  await step('Q54', async () => {
    const n0 = maxId('notification_request');
    const r = await post(U.D, `/residents/${C}/critical-flags`, { type: 'PROVISIONAL', assessment: 'Thở yếu, không có Bác sĩ trực' });
    const ns = who(await notesSince(n0));
    check('Q54', 'ca đêm (không Bác sĩ trực) D ghi dấu tạm → 201, mọi Bác sĩ nhận', r.status === 201 && has(ns, 'B', 'BS1'), { r: [r.status, r.body], ns: [...ns] });
  });

  await setTo('2026-12-03T08:05:00+07:00');
  await step('Q7', async () => {
    const v = await waitFor(async () => {
      const list = (await get(U.B, `/thresholds?scope=PERSONAL&residentId=${A}&indicatorId=${IND.BODY_TEMPERATURE}`)).body;
      return (list || []).find((x) => x.status === 'EFFECTIVE');
    });
    check('Q7', 'qua 08:00 hôm sau → ngưỡng cá nhân Hiệu lực', !!v, v);
  });

  await step('Q37', async () => {
    const before = fallRows(A).length;
    const r = await newIncident(U.S, { residentId: A, type: 'FALL', context: 'MOBILITY', description: 'Ngã lần hai', level: 'LOW', lowerLevelReason: 'Không chấn thương' });
    const rows = fallRows(A);
    check('Q37', 'A ngã lần nữa sau ~46 giờ → khoảng cũ Được thay, khoảng mới từ lần ngã này', r.status === 201 && rows.length === before + 1 && /REPLACED|SUPERSEDED/i.test(rows[0].status),
      { r: [r.status, code(r), r.body && r.body.detail], rows: rows.map((x) => [x.id, x.status, x.effective_from, x.effective_to]) });
    manual('Q37', '005 chặn công việc theo dõi ngoài khoảng', 'cần lập công việc ở 005');
  });
}

// ---------------- Chuyển viện ----------------
async function transfer() {
  let inc, sum;
  await step('C1', async () => {
    const r = await newIncident(U.D, { residentId: A, type: 'OTHER_HEALTH_CARE', context: 'CARE', description: 'Đau ngực', level: 'MEDIUM' });
    if (!r.body.incident) throw new Error('ghi sự cố: ' + j(r.body));
    inc = r.body.incident.id;
    const info = await get(U.B, `/incidents/${inc}/transfer-out/info`);
    const b = info.body || {};
    check('C1', 'thông tin chuyển viện: phiếu nguyện vọng khi điều trị, người liên hệ chính; không gợi ý cơ sở',
      info.status === 200 && b.treatmentWish && /Bệnh viện X/.test(j(b.treatmentWish)) && b.primaryContact && b.mustAskFirst === true && !('suggestions' in b), b);
  });

  await step('C2', async () => {
    const t = await nowIso();
    const base = { transferUnit: 'Bệnh viện X', transferAt: t, handoverStaffId: 9005, requiredContactReached: false };
    const r1 = await post(U.B, `/incidents/${inc}/transfer-out`, base);
    check('C2', 'chưa liên lạc được người cần báo, không lý do → 422', r1.status === 422, r1);
    const r2 = await post(U.B, `/incidents/${inc}/transfer-out`, { ...base, notAskedFirstReason: 'Gọi R không được', companion: 'Người thân M' });
    sum = r2.body || {};
    const doc = sum.id ? await get(U.B, `/transfer-summaries/${sum.id}/document`) : null;
    check('C2', 'có lý do, người đi kèm → 201, tóm tắt có nguyện vọng khi điều trị, người đi kèm; phiếu PDF',
      r2.status === 201 && sum.treatmentWish && sum.companion === 'Người thân M' && sum.documentFileId && doc && doc.status === 200 && doc.bytes.slice(0, 4).toString() === '%PDF',
      { r2: [r2.status, code(r2)], sum, doc: doc && [doc.status, doc.type, doc.bytes.length] });
    manual('C2', 'hai tệp giấy chuyển viện (tepDinhKemIds) tới 001; PDF có dòng "không thay giấy chuyển cơ sở…"', 'cần tải tệp lên S3; mở PDF xem dòng chữ');
  });

  await step('C3', async () => {
    const a0 = maxId('audit_log');
    const d1 = await get(U.B, `/transfer-summaries/${sum.id}/document`);
    const d2 = await get(U.B, `/transfer-summaries/${sum.id}/document`);
    const rp = await post(U.B, `/transfer-summaries/${sum.id}/reprint`, {});
    const t0 = Date.now(); await get(U.B, `/transfer-summaries/${sum.id}/document`); const ms = Date.now() - t0;
    const audit = L.sql(`SELECT COUNT(*) c FROM audit_log WHERE id > ${a0}`)[0].c;
    check('C3', 'tải phiếu hai lần cùng byte; in lại khác bản gốc (dấu "Bản in lại"); có nhật ký; dựng phiếu ≤ 3 giây',
      d1.status === 200 && Buffer.compare(d1.bytes, d2.bytes) === 0 && rp.status < 300 && rp.bytes.length && Buffer.compare(rp.bytes, d1.bytes) !== 0 && +audit >= 1 && ms <= 3000,
      { d: [d1.status, d1.bytes.length, d2.bytes.length], rp: [rp.status, rp.bytes.length], audit, ms });
  });

  await step('C4', async () => {
    const r = await newIncident(U.D5, { residentId: G, type: 'BREATHING_OR_CONSCIOUSNESS', context: 'CARE', description: 'Ngừng thở', level: 'CRITICAL' });
    const id = r.body.incident && r.body.incident.id;
    const t = await nowIso();
    const tr = await post(U.B, `/incidents/${id}/transfer-out`, { transferUnit: 'Bệnh viện Y', transferAt: t, handoverStaffId: 9011, companion: 'Điều dưỡng D5', requiredContactReached: false });
    check('C4', 'chuyển viện từ sự cố Khẩn cấp, chưa liên lạc, không lý do → 201', tr.status === 201, { r: r.status, tr: [tr.status, code(tr), tr.body && tr.body.detail] });
  });
}

async function unbind() {
  await step('S8', async () => {
    const acc = L.sql("SELECT id FROM account WHERE username = 'demo.nct.a'")[0].id;
    const cur = await get(U.H, `/accounts/${acc}`);
    const u = await call(U.H, 'POST', `/accounts/${acc}/unbind-device`, { reason: 'Mất máy' }, { 'If-Match': cur.etag || '' });
    const r = await sos(DEV_A, 'POST', '/sos', {});
    check('S8', 'Hành chính gỡ thiết bị của A; bấm SOS bằng token cũ → 401 INVALID_DEVICE kèm lienHe', u.status < 300 && r.status === 401 && code(r) === 'INVALID_DEVICE' && r.body.contacts,
      { get: cur.status, u: [u.status, code(u)], r });
  });
}

async function manualOnly() {
  manual('Q57', 'Kết thúc lưu trú khi còn cảnh báo mở → 409 (miễn được); qua đời: lịch đo Đã ngừng, ngưỡng Hết hiệu lực', 'cần lệnh kết thúc lưu trú, ghi nhận qua đời của 001/004 trên dữ liệu demo');
  manual('K3', '012 gọi suCo, choBanTin: M phần chung + sức khỏe, N chỉ phần chung', 'cần cổng người thân (012) xem bản tin sự cố');
}

(async () => {
  await prepare();
  await health();
  await alerts();
  const { fallA } = await incidents();
  await sosSteps();
  await assistSteps();
  await emergency();
  await infection();
  await critical();
  await corrections(fallA);
  await later(fallA);
  await transfer();
  await unbind();
  await weightTrend();
  await manualOnly();
  L.summary('007');
})().catch((e) => { console.error(e); L.summary('007'); process.exit(1); });
