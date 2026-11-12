// Quickstart 004 mục 3: tiếp nhận và hợp đồng (Q1 → Q12, Q41), chỉ các bước báo cáo đối chiếu ghi "cần chạy demo".
const F = require('./fixture.cjs');
const { U, call, must, withEtag } = F;
const { check, note } = require('./api.cjs');
const db = require('./db.cjs');

const codeOf = (r) => r.body?.errorCode;
const conds = async (appId) => (await call(U.HC, 'GET', `/admission-applications/${appId}/admission-conditions`)).body;
// Dòng Chưa đạt mang mã của module sở hữu (ví dụ (f) là DBR_02), dòng Đạt mang mã cố định: tìm theo căn cứ
const BASIS = { ADM_A_ASSESSMENT: 'FR-004 (a)', ADM_B_CONTRACT: 'FR-004 (b)', ADM_C_DEPOSIT: 'FR-004 (c)', ADM_D_BED: 'FR-004 (d)', ADM_E_CONTRACT_LEVEL: 'BR-M02-22', ADM_F_CONTACTS: 'DBR-02' };
const condStatus = (c, code) => c.conditions?.find((x) => x.code === code || (BASIS[code] && x.basis?.startsWith(BASIS[code])))?.status;

module.exports = async function part1(ctx) {
  const { s, svc, today, nowIso, start } = ctx;
  const end = F.day(start, 365);

  // Q1, Q2: lượt đăng ký
  const A = await F.resident('Nguyễn Thị A');
  ctx.A = A;
  let r = await call(U.HC, 'POST', '/admission-applications', { residentId: A.id, applicationDate: today, desiredResidenceType: 'LONG_TERM_RESIDENTIAL', expectedCareLevel: 'REGULAR', desiredAdmissionDate: start });
  const appA = r.body;
  ctx.appA = appA;
  const c0 = await conds(appA.id);
  const notMet = (c0.conditions || []).filter((x) => x.status === 'NOT_MET').map((x) => x.code);
  check('Q1', `lập lượt đăng ký: 201 IN_PROGRESS; điều kiện Chưa đạt gồm cả (f) người đại diện [${notMet.join(',')}]`,
    r.status === 201 && appA.status === 'IN_PROGRESS' && notMet.length === 6 && condStatus(c0, 'ADM_F_CONTACTS') === 'NOT_MET', { r, c0 });
  r = await call(U.HC, 'POST', '/admission-applications', { residentId: A.id, applicationDate: today, desiredResidenceType: 'LONG_TERM_RESIDENTIAL', expectedCareLevel: 'REGULAR' });
  check('Q1', 'lượt đăng ký thứ hai: 409 APPLICATION_ALREADY_EXISTS (DA_CO_LUOT_DANG_KY)', r.status === 409 && codeOf(r) === 'APPLICATION_ALREADY_EXISTS', r);
  const r2a = await call(U.TT, 'POST', '/admission-applications', { residentId: A.id, applicationDate: today, desiredResidenceType: 'LONG_TERM_RESIDENTIAL', expectedCareLevel: 'REGULAR' });
  const r2b = await call(U.BS, 'POST', '/admission-applications', { residentId: A.id, applicationDate: today, desiredResidenceType: 'LONG_TERM_RESIDENTIAL', expectedCareLevel: 'REGULAR' });
  check('Q2', 'Trưởng tầng, Bác sĩ lập lượt đăng ký: 403, 403', r2a.status === 403 && r2b.status === 403, { r2a, r2b });

  // Q1a: ý kiến vào ở
  const op1 = await call(U.HC, 'POST', `/admission-applications/${appA.id}/admission-opinion`, { opinion: 'UNABLE_TO_EXPRESS', source: { type: 'OBSERVING_STAFF' } });
  const op2 = await call(U.HC, 'POST', `/admission-applications/${appA.id}/admission-opinion`, { opinion: 'AGREE', source: { type: 'RESIDENT' } });
  const appA2 = (await call(U.HC, 'GET', `/admission-applications/${appA.id}`)).body;
  const first = appA2.admissionOpinion?.find((o) => o.id === op1.body?.id);
  check('Q1a', 'ý kiến "Không bày tỏ được" (nhân viên) rồi "Đồng ý" (người cao tuổi): 201, 201; ý kiến đầu SUPERSEDED, vẫn xem được',
    op1.status === 201 && op2.status === 201 && first?.status === 'SUPERSEDED', { op1, op2, appA2 });
  // người thân không có quan hệ Hiệu lực với A
  const B0 = await F.resident('Người khác');
  const stranger = await F.relative(B0.id, 'Người thân của người khác', false);
  const op3 = await call(U.HC, 'POST', `/admission-applications/${appA.id}/admission-opinion`, { opinion: 'AGREE', source: { type: 'FAMILY_MEMBER', familyMemberId: stranger.familyMemberId } });
  check('Q1a', 'ý kiến nguồn người thân không có quan hệ Hiệu lực: 422 (NGUON_Y_KIEN_KHONG_HOP_LE)', op3.status === 422, op3);

  // Q3: hợp đồng Nháp khi chưa có đánh giá; giá tháng thấp hơn phiên bản; Gửi ký
  r = await call(U.HC, 'POST', '/contracts', { residentId: A.id, residenceType: 'LONG_TERM_RESIDENTIAL', startDate: start, endDate: end, careLevel: 'REGULAR', roomTypeId: s.rtA.id, residencyFeeBillingPeriod: 'MONTH', services: [{ serviceId: svc.careR.id, inPackage: true, quantity: 1 }, { serviceId: svc.feeA.id, inPackage: true, quantity: 1 }], depositAmount: 10000000 });
  // chưa có người đại diện thì không lập được hợp đồng (FR-019): ghi nhận rồi thêm người đại diện
  ctx.repA = await F.relative(A.id, 'Trần Văn R');
  if (r.status === 409 && codeOf(r) === 'REPRESENTATIVE_REQUIRED') {
    note('Q3', 'GHI CHÚ', 'lập hợp đồng khi chưa có người đại diện: 409 REPRESENTATIVE_REQUIRED (FR-019); script thêm người đại diện R rồi lập lại');
    r = await call(U.HC, 'POST', '/contracts', { residentId: A.id, residenceType: 'LONG_TERM_RESIDENTIAL', startDate: start, endDate: end, careLevel: 'REGULAR', roomTypeId: s.rtA.id, residencyFeeBillingPeriod: 'MONTH', services: [{ serviceId: svc.careR.id, inPackage: true, quantity: 1 }, { serviceId: svc.feeA.id, inPackage: true, quantity: 1 }], depositAmount: 10000000 });
  }
  const ctA = r.body?.contract;
  ctx.ctA = ctA;
  const warn = JSON.stringify(r.body?.warnings || []);
  check('Q3', `hợp đồng Nháp khi chưa có đánh giá: 201 kèm cảnh báo chưa đánh giá ${warn}`, r.status === 201 && ctA?.status === 'DRAFT' && /ASSESS|ĐÁNH GIÁ|đánh giá/i.test(warn), r);
  r = await withEtag(U.HC, `/contracts/${ctA.id}`, 'PUT', `/contracts/${ctA.id}`, { residentId: A.id, residenceType: 'LONG_TERM_RESIDENTIAL', startDate: start, endDate: end, careLevel: 'REGULAR', roomTypeId: s.rtA.id, residencyFeeBillingPeriod: 'MONTH', services: [{ serviceId: svc.careR.id, inPackage: true, quantity: 1 }, { serviceId: svc.feeA.id, inPackage: true, quantity: 1, customUnitPrice: svc.feeA.price - 500000 }], depositAmount: 10000000 });
  const nst = (await call(U.HC, 'GET', `/contracts/${ctA.id}/non-standard-terms`)).body;
  check('Q3', 'sửa giá tháng thấp hơn phiên bản: 200, có điều khoản khác chuẩn PRICE', r.status === 200 && Array.isArray(nst) && nst.some((x) => x.type === 'PRICE'), { r, nst });
  r = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/send-for-signature`);
  check('Q3', 'Gửi ký khi điều khoản khác chuẩn chưa duyệt: 409', r.status === 409, r);

  // Q4: duyệt điều khoản
  r = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/submit-terms-for-approval`, { reason: 'Giảm giá phòng cho gia đình khó khăn' });
  const reqId = r.body?.approvalRequestId;
  const ap = reqId ? await withEtag(U.QL, `/approval-requests/${reqId}`, 'POST', `/approval-requests/${reqId}/approve`, { comment: 'Đồng ý' }) : r;
  let send = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/send-for-signature`);
  check('Q4', 'gửi duyệt điều khoản, QL duyệt (api-000): Gửi ký 200', r.status === 201 && ap.status === 200 && send.status === 200, { r, ap, send });
  r = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/return-to-draft`, { reason: 'Sửa giá lần nữa' });
  const upd = await withEtag(U.HC, `/contracts/${ctA.id}`, 'PUT', `/contracts/${ctA.id}`, { residentId: A.id, residenceType: 'LONG_TERM_RESIDENTIAL', startDate: start, endDate: end, careLevel: 'REGULAR', roomTypeId: s.rtA.id, residencyFeeBillingPeriod: 'MONTH', services: [{ serviceId: svc.careR.id, inPackage: true, quantity: 1 }, { serviceId: svc.feeA.id, inPackage: true, quantity: 1, customUnitPrice: svc.feeA.price - 800000 }], depositAmount: 10000000 });
  send = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/send-for-signature`);
  check('Q4', 'Trả về nháp, sửa giá lần nữa, Gửi ký: 409 phải duyệt lại', r.status === 200 && upd.status === 200 && send.status === 409, { r, upd, send });
  // duyệt lại để tiếp tục kịch bản
  r = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/submit-terms-for-approval`, { reason: 'Giá mới' });
  if (r.body?.approvalRequestId) await withEtag(U.QL, `/approval-requests/${r.body.approvalRequestId}`, 'POST', `/approval-requests/${r.body.approvalRequestId}/approve`, { comment: 'Đồng ý' });
  send = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/send-for-signature`);

  // Q5: thiếu dịch vụ chăm sóc; dịch vụ chăm sóc lệch mức
  const E5 = await F.resident('Người Q5');
  const rep5 = await F.relative(E5.id, 'Đại diện Q5');
  const d1 = (await must(U.HC, 'POST', '/contracts', { residentId: E5.id, residenceType: 'LONG_TERM_RESIDENTIAL', startDate: start, endDate: end, careLevel: 'REGULAR', roomTypeId: s.rtA.id, residencyFeeBillingPeriod: 'MONTH', services: [{ serviceId: svc.feeA.id, inPackage: true, quantity: 1 }], depositAmount: 10000000 })).body.contract;
  const s1 = await withEtag(U.HC, `/contracts/${d1.id}`, 'POST', `/contracts/${d1.id}/send-for-signature`);
  await withEtag(U.HC, `/contracts/${d1.id}`, 'POST', `/contracts/${d1.id}/cancel`, { reason: 'Q5' });
  const d2 = (await must(U.HC, 'POST', '/contracts', { residentId: E5.id, residenceType: 'LONG_TERM_RESIDENTIAL', startDate: start, endDate: end, careLevel: 'REGULAR', roomTypeId: s.rtA.id, residencyFeeBillingPeriod: 'MONTH', services: [{ serviceId: svc.careS.id, inPackage: true, quantity: 1 }, { serviceId: svc.feeA.id, inPackage: true, quantity: 1 }], depositAmount: 10000000 })).body.contract;
  const s2 = await withEtag(U.HC, `/contracts/${d2.id}`, 'POST', `/contracts/${d2.id}/send-for-signature`);
  check('Q5', 'Gửi ký thiếu dịch vụ chăm sóc: 409; dịch vụ chăm sóc lệch mức: 409', s1.status === 409 && s2.status === 409, { s1, s2 });
  await withEtag(U.HC, `/contracts/${d2.id}`, 'POST', `/contracts/${d2.id}/cancel`, { reason: 'Q5' });

  // Q6: Ghi nhận đã ký
  const scan = (await F.upload(U.HC)).body.fileId;
  const g1 = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/record-signed`, { signedDate: today, signingRepresentativeId: ctx.repA.familyMemberId });
  const g2 = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/record-signed`, { signedDate: today, signingRepresentativeId: stranger.familyMemberId, scanFileId: scan });
  check('Q6', 'Ghi nhận đã ký thiếu bản scan: 422', g1.status === 422 || g1.status === 400, g1);
  check('Q6', 'người ký không phải người đại diện: 409', g2.status === 409, g2);
  const g3 = await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/record-signed`, { signedDate: today, signingRepresentativeId: ctx.repA.familyMemberId, scanFileId: scan });
  if (g3.status === 200) {
    check('Q6', 'A chưa có phân bổ giường từ ngày bắt đầu: 409 "chưa có giường từ ngày bắt đầu" (FR-021)', false, { expected: 409, actual: g3.status, contractStatus: g3.body?.contract?.status });
  } else {
    check('Q6', 'A chưa có phân bổ giường từ ngày bắt đầu: 409', g3.status === 409, g3);
  }
  ctx.allocA = (await must(U.HC, 'POST', '/bed-allocations', { residentId: A.id, bedId: s.beds['P201-G1'].id, startAt: F.at(start, '08:00'), reason: 'Phân bổ ban đầu' })).body;
  const g4 = g3.status === 200 ? g3 : await withEtag(U.HC, `/contracts/${ctA.id}`, 'POST', `/contracts/${ctA.id}/record-signed`, { signedDate: today, signingRepresentativeId: ctx.repA.familyMemberId, scanFileId: scan });
  const content = (await call(U.HC, 'GET', `/contracts/${ctA.id}/content?date=${start}`)).body;
  check('Q6', 'đủ điều kiện: 200 ACTIVE, phiên bản nội dung 0 có đơn giá đã ghi',
    g4.status === 200 && g4.body?.contract?.status === 'ACTIVE' && content?.version === 0 && JSON.stringify(content).includes('unitPrice'), { g4: g4.status, content });

  // Q7: hợp đồng Hiệu lực bị khóa; UPDATE trực tiếp
  r = await withEtag(U.HC, `/contracts/${ctA.id}`, 'PUT', `/contracts/${ctA.id}`, { residentId: A.id, residenceType: 'LONG_TERM_RESIDENTIAL', startDate: start, endDate: end, careLevel: 'SPECIAL', roomTypeId: s.rtA.id, residencyFeeBillingPeriod: 'MONTH', services: [{ serviceId: svc.careS.id, inPackage: true, quantity: 1 }], depositAmount: 10000000 });
  check('Q7', 'PUT hợp đồng Hiệu lực: 409 CONTRACT_LOCKED (HOP_DONG_DA_KHOA)', r.status === 409 && codeOf(r) === 'CONTRACT_LOCKED', r);
  const sql = db.asApp(`UPDATE contract_version SET care_level='SPECIAL' WHERE contract_id=${ctA.id}`);
  const sql2 = db.asApp(`UPDATE contract SET deposit_amount=1 WHERE id=${ctA.id}`);
  check('Q7', 'UPDATE trực tiếp bằng tài khoản ứng dụng: trigger chặn', !sql.ok && !sql2.ok && /45000|SIGNAL|locked|khóa|denied/i.test(sql.err + sql2.err), { sql, sql2 });

  // Q8: hợp đồng chồng thời gian, nối đuôi, đồng thời
  const mk = async (st, en) => (await must(U.HC, 'POST', '/contracts', { residentId: A.id, residenceType: 'LONG_TERM_RESIDENTIAL', startDate: st, endDate: en, careLevel: 'REGULAR', roomTypeId: s.rtA.id, residencyFeeBillingPeriod: 'MONTH', services: [{ serviceId: svc.careR.id, inPackage: true, quantity: 1 }, { serviceId: svc.feeA.id, inPackage: true, quantity: 1 }], depositAmount: 10000000 })).body.contract;
  const sign = async (c) => {
    await withEtag(U.HC, `/contracts/${c.id}`, 'POST', `/contracts/${c.id}/send-for-signature`);
    return withEtag(U.HC, `/contracts/${c.id}`, 'POST', `/contracts/${c.id}/record-signed`, { signedDate: today, signingRepresentativeId: ctx.repA.familyMemberId, scanFileId: (await F.upload(U.HC)).body.fileId });
  };
  const c2 = await mk(F.day(start, 230), F.day(start, 500));
  const r8a = await sign(c2);
  const c3 = await mk(F.day(end, 1), F.day(end, 365));
  const r8b = await sign(c3);
  check('Q8', `hợp đồng thứ hai chồng thời gian: 409 (${codeOf(r8a)}); kế tiếp nối đuôi: 200`, r8a.status === 409 && r8b.status === 200, { r8a, r8b });
  const c4 = await mk(F.day(end, 800), F.day(end, 900));
  const c5 = await mk(F.day(end, 850), F.day(end, 950));
  for (const c of [c4, c5]) await withEtag(U.HC, `/contracts/${c.id}`, 'POST', `/contracts/${c.id}/send-for-signature`);
  const [e4, e5] = await Promise.all([c4, c5].map(async (c) => (await call(U.HC, 'GET', `/contracts/${c.id}`)).etag));
  const [f1, f2] = await Promise.all([F.upload(U.HC), F.upload(U.HC)]);
  const [x1, x2] = await Promise.all([
    call(U.HC, 'POST', `/contracts/${c4.id}/record-signed`, { signedDate: today, signingRepresentativeId: ctx.repA.familyMemberId, scanFileId: f1.body.fileId }, { 'If-Match': e4 }),
    call(U.HC, 'POST', `/contracts/${c5.id}/record-signed`, { signedDate: today, signingRepresentativeId: ctx.repA.familyMemberId, scanFileId: f2.body.fileId }, { 'If-Match': e5 }),
  ]);
  check('Q8', 'hai lệnh Ghi nhận đã ký đồng thời chồng nhau: đúng một thành công', [x1.status, x2.status].sort().join() === '200,409', { x1, x2 });

  // Q9: đặt cọc (017 thật thay bản giả): Chưa đạt rồi Đạt kèm giao dịch nguồn; HC không có lệnh xác nhận cọc
  let c9 = await conds(appA.id);
  const before = condStatus(c9, 'ADM_C_DEPOSIT');
  const hcPay = await call(U.HC, 'POST', `/residents/${A.id}/cash-payments`, { ledger: 'DEPOSIT', amount: 10000000, payerName: 'R', receivedAt: nowIso });
  const pay = await call(U.KT, 'POST', `/residents/${A.id}/cash-payments`, { ledger: 'DEPOSIT', amount: 10000000, payerName: 'Trần Văn R', receivedAt: nowIso });
  c9 = await conds(appA.id);
  const dep = (await call(U.HC, 'GET', `/contracts/${ctA.id}/deposit`)).body;
  check('Q9', `(c) đặt cọc: ${before} → ${condStatus(c9, 'ADM_C_DEPOSIT')}; HC thu cọc 403`, before === 'NOT_MET' && condStatus(c9, 'ADM_C_DEPOSIT') === 'MET' && hcPay.status === 403 && pay.status === 201, { before, hcPay: hcPay.status, pay: pay.status, c9 });
  check('Q9', 'điều kiện (c) Đạt kèm giao dịch nguồn và người lập (Kế toán)', JSON.stringify(dep).match(/transaction|Transaction/) && JSON.stringify(dep).includes('Kế toán'), dep);

  // Q12: Hoàn tất tiếp nhận trước ngày bắt đầu hợp đồng (đánh giá đã xác nhận ở bước sau Q3)
  await F.confirmAssessment(A.id, 'REGULAR');
  const early = await F.completeAdmission(A.id);
  const c12 = await conds(appA.id);
  check('Q12', `Hoàn tất tiếp nhận trước ngày bắt đầu hợp đồng: 409 (FR-075) [${condStatus(c12, 'ADM_B_CONTRACT')}]`, early.status === 409, early);

  // Chuẩn bị Q1b, Q1c, Q41: ba người đủ điều kiện, cùng ngày bắt đầu
  ctx.B = await F.readyForAdmission({ name: 'Lê Thị B', start, end, svc, bed: s.beds['P305-G1'], roomTypeId: s.rtB.id, today, nowIso });
  ctx.C = await F.readyForAdmission({ name: 'Phạm Thị C', start, end, svc, bed: s.beds['P305-G2'], roomTypeId: s.rtB.id, today, nowIso });
  await call(U.HC, 'POST', `/admission-applications/${ctx.C.app.id}/admission-opinion`, { opinion: 'DISAGREE', source: { type: 'RESIDENT' } });
  ctx.D = await F.readyForAdmission({ name: 'Võ Thị D', start, end, svc, bed: s.beds['P305-G3'], roomTypeId: s.rtB.id, today, nowIso });
  // Q41: người đại diện kết thúc sau khi hợp đồng Hiệu lực
  const endRel = await call(U.HC, 'POST', `/relationships/${ctx.D.rep.relationshipId}/end`, { reason: 'Q41: thử thiếu người đại diện' });
  ctx.endRelD = endRel;
};

// Phần chạy sau khi tua tới ngày bắt đầu hợp đồng
module.exports.afterStart = async function part1b(ctx) {
  const { A, appA } = ctx;
  let r = await F.completeAdmission(A.id);
  const app = (await call(U.HC, 'GET', `/admission-applications/${appA.id}`)).body;
  check('Q12', 'đúng ngày bắt đầu: 200, lượt đăng ký ADMITTED (DA_TIEP_NHAN)', r.status === 200 && app.status === 'ADMITTED', { r, app });
  // 018 nghe AdmissionCompleted: bản ghi event_publication có completion_date là mọi listener đã xử lý xong
  const ev = db.query(`SELECT COUNT(*), SUM(completion_date IS NOT NULL) FROM event_publication WHERE event_type LIKE '%AdmissionCompleted%' AND serialized_event LIKE '%"residentId":${A.id},%'`);
  const [evAll, evDone] = ev.out.split('\t').map(Number);
  check('Q12', `sự kiện TiepNhanHoanTat (AdmissionCompleted) phát và các listener (018) xử lý xong [${evAll} bản ghi, ${evDone} hoàn tất]`, ev.ok && evAll >= 1 && evDone === evAll, ev);

  // Q1b: B không có ý kiến
  r = await F.completeAdmission(ctx.B.resident.id);
  const w = JSON.stringify(r.body);
  const c1b = await conds(ctx.B.app.id);
  check('Q1b', `Hoàn tất tiếp nhận khi chưa có ý kiến: 200 kèm cảnh báo chưa ghi ý kiến ${JSON.stringify(r.body?.warnings || r.body?.history?.warnings || '')}`,
    r.status === 200 && /OPINION|Ý KIẾN|ý kiến/i.test(w), r);
  check('Q1b', 'danh sách điều kiện có admissionOpinion = null, không có dòng điều kiện ý kiến', c1b.admissionOpinion == null && !(c1b.conditions || []).some((x) => /OPINION/.test(x.code)), c1b);

  // Q1c: C "Không đồng ý"
  r = await withEtag(U.HC, `/residents/${ctx.C.resident.id}`, 'POST', `/residents/${ctx.C.resident.id}/complete-admission`, {});
  check('Q1c', 'Hoàn tất tiếp nhận không lý do khi NCT không đồng ý: 422', r.status === 422 || r.status === 400, r);
  r = await F.completeAdmission(ctx.C.resident.id, 'Gia đình và bác sĩ thống nhất, sẽ trao đổi thêm với bà C');
  const appC = (await call(U.HC, 'GET', `/admission-applications/${ctx.C.app.id}`)).body;
  const warnC = (r.body?.warnings || []).map((w) => w.code);
  check('Q1c', `có lý do: 200 kèm cảnh báo NCT không đồng ý [${warnC.join(',')}]`, r.status === 200 && warnC.some((c) => /DISAGREE/.test(c)), r);
  const hist = db.query(`SELECT reason FROM status_history WHERE entity_type LIKE '%RESIDENT%' AND entity_id = ${ctx.C.resident.id} AND command = 'COMPLETE_ADMISSION'`);
  check('Q1c', 'lịch sử trạng thái lưu lý do (status_history.reason)', hist.ok && hist.out.includes('Gia đình và bác sĩ thống nhất'), hist);
  check('Q1c', `opinionAtAdmissionId (${appC.opinionAtAdmissionId}) trỏ ý kiến Không đồng ý`, appC.opinionAtAdmissionId != null && appC.admissionOpinion?.find((o) => o.id === appC.opinionAtAdmissionId)?.opinion === 'DISAGREE', appC);
  if ((r.body?.history?.hiddenFields || []).includes('reason')) note('Q1c', 'GHI CHÚ', 'phản hồi lệnh ẩn "reason" với chính Hành chính vừa nhập (hiddenFields của 001)');

  // Q41: D đã mất người đại diện
  const condD = await conds(ctx.D.app.id);
  r = await F.completeAdmission(ctx.D.resident.id);
  check('Q41', `chưa có người đại diện: (f) ${condStatus(condD, 'ADM_F_CONTACTS')}, lệnh 409 liệt kê (f)`,
    ctx.endRelD.status === 200 && condStatus(condD, 'ADM_F_CONTACTS') === 'NOT_MET' && r.status === 409 && JSON.stringify(r.body).includes('DBR_02'), { endRel: ctx.endRelD, condD, r });
  await F.relative(ctx.D.resident.id, 'Đại diện mới của D');
  const condD2 = await conds(ctx.D.app.id);
  r = await F.completeAdmission(ctx.D.resident.id);
  check('Q41', 'thêm người đại diện: sáu điều kiện Đạt, 200', (condD2.conditions || []).every((x) => x.status === 'MET') && r.status === 200, { condD2, r });
};
