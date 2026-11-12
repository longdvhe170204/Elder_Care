// Quickstart 004 mục 3 (Q31, Q33, Q35 → Q39) và mục 6 (Q38' → Q43'): kết thúc lưu trú, ngoại lệ (h) quyết toán với 017
// thật, qua đời, đơn giá, quá hạn hợp đồng ngắn ngày. Tự dựng người cao tuổi đã vào ở nên chạy riêng được (run.cjs 3).
const F = require('./fixture.cjs');
const { U, STAFF, call, must, withEtag } = F;
const { check, note } = require('./api.cjs');
const db = require('./db.cjs');

const codeOf = (r) => r.body?.errorCode;
const approval = (user, id, cmd, body = {}) => withEtag(user, `/approval-requests/${id}`, 'POST', `/approval-requests/${id}/${cmd}`, body);
const statusCmd = (user, residentId, cmd, reason, details) =>
  withEtag(user, `/residents/${residentId}`, 'POST', `/residents/${residentId}/${cmd}`, { reason, details });
const notices = async (user, eventType, residentId) => {
  const r = await call(user, 'GET', `/me/notifications?eventType=${eventType}${residentId ? '&residentId=' + residentId : ''}`);
  return r.body?.items || [];
};
const rows = (c) => c?.conditions || c?.items || [];
const row = (c, prefix) => rows(c).find((x) => x.code?.startsWith(prefix));

async function admitted(ctx, name, bed, roomTypeId, opts = {}) {
  const nowIso = await F.now();
  const today = F.day(nowIso);
  const x = await F.readyForAdmission({ name, start: today, end: F.day(today, 365), svc: ctx.svc, bed, roomTypeId, today, nowIso, ...opts });
  const r = await F.completeAdmission(x.resident.id);
  if (r.status !== 200) throw new Error('Hoàn tất tiếp nhận ' + name + ': ' + JSON.stringify(r.body));
  return x;
}

const createEnding = (residentId, expectedEndDate, endCase = 'DISCHARGE') =>
  call(U.HC, 'POST', '/residency-endings', { residentId, endCase, expectedEndDate, requestedBy: 'Con gái', reason: 'Gia đình đưa về chăm sóc tại nhà' });
const conditions = async (endingId) => (await call(U.HC, 'GET', `/residency-endings/${endingId}/conditions`)).body;
const endResidency = (residentId, endingId, effectiveAt) =>
  statusCmd(U.HC, residentId, 'end-residency', 'Kết thúc lưu trú', { residencyEndingId: endingId, effectiveAt });

module.exports = async function part3(ctx) {
  const s = await F.structure('C');
  const svc = { ...ctx.svc };
  for (const [k, rt] of [['feeA', s.rtA], ['feeB', s.rtB]]) {
    svc[k] = (await must(U.QL, 'POST', '/services', { type: 'RESIDENCY_FEE', name: `Phí lưu trú ${k} ${F.tag}C`, unit: 'tháng', billingPeriod: 'MONTH', roomTypeId: rt.id })).body;
    await must(U.QL, 'POST', `/services/${svc[k].id}/price-versions`, { unitPrice: 4500000, effectiveFrom: '2026-01-01' });
    svc[k].roomTypeId = rt.id;
  }
  const c3 = { ...ctx, s, svc };
  let nowIso = await F.now();
  let today = F.day(nowIso);

  // ---- Q31, Q33, Q38' → Q42': hồ sơ kết thúc "Xuất viện" của A3 ----
  const A = await admitted(c3, 'Người kết thúc Q31', s.beds['P305-G1'], s.rtB.id);
  const end = await createEnding(A.resident.id, F.day(today, 10));
  const endingId = end.body?.id;
  let c = await conditions(endingId);
  const codes = rows(c).map((x) => `${x.code}:${x.status}`);
  const auto = rows(c).filter((x) => x.status === 'AUTOMATIC').map((x) => x.code);
  // (a) Chưa đạt "Bảng kỳ cuối chưa chốt" là dấu 010 đã nhận lệnh tạo bảng kỳ cuối nháp
  check('Q31', `lập hồ sơ kết thúc ${end.status} ${end.body?.status}; kỳ cuối nháp ở 010 (${row(c, 'END_A')?.basis}); điều kiện [${codes.join(', ')}]`,
    end.status === 201 && end.body?.status === 'PREPARING' && rows(c).length === 8 && auto.length === 2 && /kỳ cuối/.test(row(c, 'END_A')?.basis || ''), { end, c });
  note('Q31', 'CẦN XEM TAY', 'hai mục Chưa đạt do còn đồ gửi (013) và cảnh báo mở (007): script không dựng đồ gửi, cảnh báo thật; xem EndingConditionsIT');

  let r = await call(U.HC, 'POST', `/residency-endings/${endingId}/request-exception`, { conditionCode: 'END_E_HANDOVER', reason: 'Thử ngoại lệ bàn giao' });
  check('Q33', `lập ngoại lệ cho (e) bàn giao: 422 [${r.status} ${codeOf(r) || ''}]`, r.status === 422, r);
  r = await call(U.KT, 'POST', `/residency-endings/${endingId}/request-exception`, { conditionCode: 'END_H_SETTLEMENT_CONFIRMATION', reason: 'Thử ngoại lệ xác nhận' });
  check("Q41'", `lập ngoại lệ với END_H_SETTLEMENT_CONFIRMATION: 422 [${r.status} ${codeOf(r) || ''}]`, r.status === 422, r);

  const h0 = row(c, 'END_H');
  check("Q38'", `dòng (h) ${h0?.status} kèm hai lý do quyết toán và xác nhận: ${JSON.stringify(h0?.reasons || h0?.details || h0?.basis)}`,
    h0?.status === 'NOT_MET' && /SETTLEMENT[^C]/.test(JSON.stringify(h0) + ' ') && /CONFIRMATION|xác nhận/i.test(JSON.stringify(h0)), h0);

  const hcEx = await call(U.HC, 'POST', `/residency-endings/${endingId}/request-exception`, { conditionCode: 'END_H_SETTLEMENT', reason: 'Gia đình còn nợ, Quản lý viện cho ngoại lệ' });
  const ktEx = await call(U.KT, 'POST', `/residency-endings/${endingId}/request-exception`, { conditionCode: 'END_H_SETTLEMENT', reason: 'Gia đình còn nợ, Quản lý viện cho ngoại lệ' });
  const exId = ktEx.body?.approvalRequestId ?? ktEx.body?.id;
  if (exId) await approval(U.KT, exId, 'submit');
  const exAp = exId ? await approval(U.QL, exId, 'approve', { comment: 'Đồng ý ngoại lệ' }) : {};
  await new Promise((res) => setTimeout(res, 1500));
  c = await conditions(endingId);
  const h1 = row(c, 'END_H');
  nowIso = await F.now();
  const end409 = await endResidency(A.resident.id, endingId, nowIso);
  check("Q39'", `HC lập ngoại lệ (h) ${hcEx.status} (FR-064 cho phép); Kế toán lập ${ktEx.status}, QL duyệt ${exAp.status}; (h) ${h1?.status} [${h1?.basis}]; Kết thúc lưu trú ${end409.status}`,
    hcEx.status === 201 && ktEx.status === 201 && exAp.status === 200 && h1?.status === 'NOT_MET' && /xác nhận/.test(h1?.basis || '') && end409.status === 409, { hcEx, ktEx, exAp, h1, end409 });
  if (hcEx.body?.approvalRequestId) await approval(U.HC, hcEx.body.approvalRequestId, 'cancel', { reason: 'Trùng yêu cầu của Kế toán' });
  const st = (await call(U.KT, 'GET', `/residents/${A.resident.id}/settlement`)).body;
  // EndingExceptionApproved là sự kiện đồng bộ (@EventListener của 017, không ghi event_publication): kiểm qua màn quyết toán
  check("Q39'", `017 nhận NgoaiLeKetThucDaDuyet: hasException (coNgoaiLeQuyetToanDaDuyet) = ${st?.hasException}`, st?.hasException === true, st);


  // Q40': Kế toán xác nhận quyết toán (017 thật) → (h) Đạt (ngoại lệ). "Các điều kiện khác đạt": ngoại lệ (a) chi phí kỳ
  // cuối, đổi ngày kết thúc dự kiến về hôm nay (lệnh chỉ nhận thời điểm trong ngày dự kiến, FR-066), bàn giao
  const exA = await call(U.HC, 'POST', `/residency-endings/${endingId}/request-exception`, { conditionCode: 'END_A_COSTS', reason: 'Kỳ cuối chốt sau, Quản lý viện cho ngoại lệ' });
  if (exA.body?.approvalRequestId) {
    await approval(U.HC, exA.body.approvalRequestId, 'submit');
    await approval(U.QL, exA.body.approvalRequestId, 'approve', { comment: 'Đồng ý' });
  }
  const chg = await call(U.HC, 'POST', `/residency-endings/${endingId}/change-date`, { expectedEndDate: F.day(await F.now()), reason: 'Gia đình đón sớm' });
  const conf = await call(U.KT, 'POST', `/residents/${A.resident.id}/settlement/confirm`);
  c = await conditions(endingId);
  const h2 = row(c, 'END_H');
  const ho = await call(U.HC, 'POST', `/residency-endings/${endingId}/handover`, { recipientType: 'FAMILY_MEMBER', familyMemberId: A.rep.familyMemberId, handoverAt: await F.now(), handoverStaffId: STAFF.HC });
  const fin = await endResidency(A.resident.id, endingId, await F.now());
  check("Q40'", `Kế toán xác nhận quyết toán ${conf.status} ${codeOf(conf) || ''}; (h) ${h2?.status}; ngoại lệ (a) ${exA.status}, đổi ngày ${chg.status}, bàn giao ${ho.status}; Kết thúc lưu trú ${fin.status} ${codeOf(fin) || ''}`,
    conf.status === 201 && h2?.status === 'MET_BY_EXCEPTION' && fin.status === 200, { conf, h2, exA, chg, ho, fin, c: rows(c) });
  // Q42': thông tin 017 đọc qua HoSoKetThucDocApi chỉ dùng trong bảng quyết toán (không có trường HTTP riêng)
  const stA = (await call(U.KT, 'GET', `/residents/${A.resident.id}/settlement`)).body;
  note("Q42'", 'CẦN XEM TAY', `thongTinQuyetToan là API Java nội bộ, 017 không trả các trường đó qua HTTP: xem tệp bảng quyết toán GET /settlement-statements/${stA?.versions?.[0]?.id}/file (người cao tuổi ${A.resident.id}) và của người qua đời ở Q35`);

  // ---- Q35, Q37, Q36, Q42' (qua đời), Q43' ----
  const D = await admitted(c3, 'Người qua đời Q35', s.beds['P305-G2'], s.rtB.id);
  const endD = (await createEnding(D.resident.id, F.day(today, 20))).body;
  nowIso = await F.now();
  const death = await statusCmd(U.BS, D.resident.id, 'record-death', 'Ghi nhận qua đời', { deathAt: nowIso, place: 'Phòng P305', discoveredBy: 'Nhân viên chăm sóc trực', confirmedById: STAFF.BS, cause: 'Suy tim' });
  const ctD = (await call(U.HC, 'GET', `/contracts/${D.contract.id}`)).body;
  const endD2 = (await call(U.HC, 'GET', `/residency-endings/${endD?.id}`)).body;
  const tasks = (await call(U.HC, 'GET', `/residents/${D.resident.id}/post-death-tasks`)).body;
  const items = tasks?.items || tasks?.tasks || [];
  const ctStatus = ctD?.contract?.status ?? ctD?.status;
  check('Q35', `Bác sĩ ghi qua đời khi hồ sơ kết thúc đang chuẩn bị: ${death.status}; hợp đồng ${ctStatus} (${ctD?.contract?.terminationReason ?? ctD?.terminationReason ?? ''}); hồ sơ kết thúc ${endD2?.status}; việc sau qua đời [${items.map((x) => `${x.code ?? x.item}:${x.status}`).join(', ')}]`,
    death.status === 200 && ctStatus === 'TERMINATED' && endD2?.status === 'CANCELLED' && items.length >= 5 && items.some((x) => x.status !== 'COMPLETED' && x.status !== 'NOT_APPLICABLE'), { death, ctD, endD2, tasks });

  const hcView = (await call(U.HC, 'GET', `/residents/${D.resident.id}/death-record`)).body;
  const bsView = (await call(U.BS, 'GET', `/residents/${D.resident.id}/death-record`)).body;
  check('Q37', `HC xem bản ghi qua đời: cause = ${hcView?.cause}, ẩn ${JSON.stringify(hcView?.hiddenFields)}; Bác sĩ thấy cause = ${bsView?.cause}`,
    hcView?.cause == null && (hcView?.hiddenFields || []).includes('cause') && bsView?.cause === 'Suy tim', { hcView, bsView });


  // Q35 nhắc sau 1 ngày (CFG-M02-09), Q36 đóng hồ sơ khi mọi mục xong
  // nhắc của 004 là M02Reminder: loại M02_REMINDER_POST_DEATH_TASKS, nguồn là bản ghi qua đời (không gắn người cao tuổi)
  const deathId = String(bsView?.id);
  const rem = async () => (await notices(U.HC, 'M02_REMINDER_POST_DEATH_TASKS')).filter((n) => n.sourceRecord?.id === deathId);
  const before = (await rem()).length;
  nowIso = await F.clock({ setTo: F.at(F.day(nowIso, 1), '00:20') });
  await F.runJob('M02_DAILY');
  const after = (await rem()).length;
  check('Q35', `tua 1 ngày (CFG-M02-09): HC được nhắc việc sau qua đời (${before} → ${after})`, after === before + 1, { before, after, deathId });
  note('Q36', 'CẦN XEM TAY', 'năm mục việc sau qua đời do 006, 007, 010, 013, 017 thật sở hữu: hoàn thành từng mục ở module đó rồi xem hồ sơ lưu trú Đã đóng và dừng nhắc (PostDeathTaskIT)');

  // Q43': B2 qua đời còn nợ; Kế toán lập ngoại lệ mục quyết toán; QL duyệt; 017 nhận sự kiện với hoSoKetThucId rỗng
  const B2 = await admitted(c3, 'Người qua đời còn nợ Q43', s.beds['P305-G3'], s.rtB.id);
  nowIso = await F.now();
  await statusCmd(U.BS, B2.resident.id, 'record-death', 'Ghi nhận qua đời', { deathAt: nowIso, place: 'Phòng P305', discoveredBy: 'Điều dưỡng trực', confirmedById: STAFF.BS });
  const sx = await call(U.KT, 'POST', `/residents/${B2.resident.id}/post-death-tasks/request-settlement-exception`, { reason: 'Gia đình còn nợ 3.000.000, xin ngoại lệ quyết toán' });
  const sxId = sx.body?.approvalRequestId ?? sx.body?.id;
  if (sxId) await approval(U.KT, sxId, 'submit');
  const sxAp = sxId ? await approval(U.QL, sxId, 'approve', { comment: 'Đồng ý' }) : {};
  await new Promise((res) => setTimeout(res, 1500));
  const reqView = sxId ? (await call(U.QL, 'GET', `/approval-requests/${sxId}`)).body : null;
  const stB = (await call(U.KT, 'GET', `/residents/${B2.resident.id}/settlement`)).body;
  check("Q43'", `ngoại lệ mục quyết toán sau qua đời: lập ${sx.status} ${codeOf(sx) || ''}, duyệt ${sxAp.status}; nội dung ${JSON.stringify(reqView?.content || {})} (không có hồ sơ kết thúc); 017 hasException = ${stB?.hasException}`,
    sx.status === 201 && sxAp.status === 200 && reqView?.content?.conditionCode === 'END_H_SETTLEMENT' && reqView?.content?.basis === 'POST_DEATH' && reqView?.content?.residencyEndingId == null && stB?.hasException === true, { sx, sxAp, reqView, stB });
  note("Q43'", 'GHI CHÚ', 'khoản nợ 3.000.000 không dựng (cần bảng kỳ cuối 010 đã chốt); lệnh vẫn nhận vì mục quyết toán chưa xong; hồ sơ lưu trú Đã đóng khi mọi mục xong: xem Q36');
  check("Q43'", 'mục đồ gửi không có endpoint ngoại lệ (api-004 chỉ có request-settlement-exception)',
    (await call(U.HC, 'POST', `/residents/${B2.resident.id}/post-death-tasks/request-belongings-exception`, { reason: 'thử' })).status === 404);

  // ---- Q38: phiên bản đơn giá ----
  // ngày theo đồng hồ demo: phiên bản mới phải hiệu lực sau hôm nay (phiên bản mở đã áp dụng tới hôm nay, DBR-08)
  const d38 = F.day(await F.now());
  const sv = (await must(U.QL, 'POST', '/services', { type: 'REHABILITATION', name: `Phục hồi Q38 ${F.tag}`, unit: 'buổi', billingPeriod: 'OCCURRENCE' })).body;
  await must(U.QL, 'POST', `/services/${sv.id}/price-versions`, { unitPrice: 200000, effectiveFrom: '2026-01-01' });
  const nv = await call(U.QL, 'POST', `/services/${sv.id}/price-versions`, { unitPrice: 250000, effectiveFrom: F.day(d38, 10) });
  let vs = (await call(U.QL, 'GET', `/services/${sv.id}/price-versions`)).body;
  const old = vs.find((v) => v.effectiveFrom === '2026-01-01');
  const ov = await call(U.QL, 'POST', `/services/${sv.id}/price-versions`, { unitPrice: 220000, effectiveFrom: F.day(d38, 5), effectiveTo: F.day(d38, 15) });
  check('Q38', `phiên bản mới từ ${F.day(d38, 10)}: ${nv.status}, phiên bản cũ kết thúc ${old?.effectiveTo}; chồng khoảng: ${ov.status}`, nv.status === 201 && old?.effectiveTo === F.day(d38, 9) && ov.status === 409, { nv, vs, ov });
  const ref = db.query(`SELECT id FROM price_version WHERE referenced = 1 LIMIT 1`);
  const upd = ref.ok && ref.out ? db.asApp(`UPDATE price_version SET unit_price = unit_price + 1 WHERE id = ${ref.out}`) : ref;
  check('Q38', `sửa phiên bản đã tham chiếu (${ref.out}) bằng tài khoản ứng dụng: bị chặn`, !upd.ok && /45000|SIGNAL|denied|tham chiếu/i.test(upd.err), upd);
  const sv2 = (await must(U.QL, 'POST', '/services', { type: 'REHABILITATION', name: `Phục hồi Q38b ${F.tag}`, unit: 'buổi', billingPeriod: 'OCCURRENCE' })).body;
  await must(U.QL, 'POST', `/services/${sv2.id}/price-versions`, { unitPrice: 300000, effectiveFrom: '2026-06-01' });
  const back = await call(U.QL, 'POST', `/services/${sv2.id}/price-versions`, { unitPrice: 280000, effectiveFrom: '2026-01-01' });
  vs = (await call(U.QL, 'GET', `/services/${sv2.id}/price-versions`)).body;
  const bv = vs.find((v) => v.effectiveFrom === '2026-01-01');
  check('Q38', `phiên bản hiệu lực lùi cho khoảng trống: ${back.status}, cắt trước phiên bản kế tiếp (kết thúc ${bv?.effectiveTo})`, back.status === 201 && bv?.effectiveTo === '2026-05-31', { back, vs });

  // ---- Q39: hợp đồng ngắn ngày kết thúc sau 2 ngày; nhắc đúng ngày; dấu quá hạn; quá 7 ngày báo QL đúng một lần ----
  nowIso = await F.now();
  today = F.day(nowIso);
  const S = await admitted(c3, 'Người ngắn ngày Q39', s.beds['P305-G4'], s.rtB.id, { type: 'SHORT_TERM_RESIDENTIAL', end: F.day(today, 2) });
  const endDay = F.day(today, 2);
  const remind = async (type) => (await notices(U.HC, 'M02_REMINDER_' + type)).filter((n) => n.sourceRecord?.id === String(S.contract.id));
  await F.clock({ setTo: F.at(endDay, '00:20') });
  await F.runJob('M02_DAILY');
  const onEnd = await remind('SHORT_TERM_CONTRACT');
  await F.clock({ setTo: F.at(F.day(endDay, 1), '00:20') });
  await F.runJob('M02_DAILY');
  const ctS = (await call(U.HC, 'GET', `/contracts/${S.contract.id}`)).body;
  const od1 = (await remind('CONTRACT_OVERDUE')).length;
  check('Q39', `ngày kết thúc: HC được nhắc (${onEnd.length}); ngày sau: quá hạn từ ${ctS?.overdueSince}, nhắc quá hạn (${od1})`,
    onEnd.length === 1 && ctS?.overdueSince === F.day(endDay, 1) && od1 === 1, { onEnd, ctS });
  for (let i = 2; i <= 9; i++) {
    await F.clock({ setTo: F.at(F.day(endDay, i), '00:20') });
    await F.runJob('M02_DAILY');
  }
  const esc = await notices(U.QL, 'CONTRACT_OVERDUE_ESCALATED', S.resident.id);
  const od = (await remind('CONTRACT_OVERDUE')).length;
  check('Q39', `quá 7 ngày: QL được báo đúng một lần (${esc.length}); HC được nhắc quá hạn mỗi ngày (${od} lần qua 9 ngày)`, esc.length === 1 && od === 9, { esc, od });
  note('Q40', 'CẦN XEM TAY', 'dừng Bộ lập lịch 3 ngày rồi chạy bù: demo không dừng được job; phần còn lại sau Q-394, Q-395 là dấu quá ngày dự kiến (FR_066a_PastExpectedDateIT)');
};
