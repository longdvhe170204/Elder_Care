// Quickstart 004 mục 3: yêu cầu thay đổi (Q15, Q16, Q42), danh sách chờ (Q18 hành vi mới Q-394, Q20 → Q22), vắng (Q23,
// Q24, Q25 hành vi mới Q-395, Q29), bán trú (Q30). Tự dựng người cao tuổi đã vào ở nên chạy riêng được (run.cjs 2).
const F = require('./fixture.cjs');
const { U, STAFF, call, must, withEtag } = F;
const { check, note, login } = require('./api.cjs');
const db = require('./db.cjs');

const codeOf = (r) => r.body?.errorCode;
const approval = (user, id, cmd, body = {}) => withEtag(user, `/approval-requests/${id}`, 'POST', `/approval-requests/${id}/${cmd}`, body);
const change = (contractId, types, newValue, effective, reason = 'Gia đình đề nghị') =>
  call(U.HC, 'POST', '/residency-change-requests', { contractId, changeTypes: types, newValue, requestedEffectiveDate: effective, reason });
const notices = async (user, eventType, residentId) => {
  const r = await call(user, 'GET', `/me/notifications?eventType=${eventType}${residentId ? '&residentId=' + residentId : ''}`);
  return r.body?.items || r.body?.notifications || [];
};
const statusCmd = (user, residentId, cmd, reason, details) =>
  withEtag(user, `/residents/${residentId}`, 'POST', `/residents/${residentId}/${cmd}`, { reason, details });

// Người cao tuổi đã vào ở (Đang lưu trú) với hợp đồng bắt đầu hôm nay; giường lấy ở room (P305 dùng chung)
async function admitted(ctx, name, bed, roomTypeId, gender = 'FEMALE') {
  const nowIso = await F.now();
  const today = F.day(nowIso);
  const x = await F.readyForAdmission({ name, gender, start: today, end: F.day(today, 365), svc: ctx.svc, bed, roomTypeId, today, nowIso });
  const r = await F.completeAdmission(x.resident.id);
  if (r.status !== 200) throw new Error('Hoàn tất tiếp nhận ' + name + ': ' + JSON.stringify(r.body));
  return x;
}

// Tài khoản cổng của người thân (002): xác minh qua số đã đăng ký, đổi mật khẩu tạm rồi đăng nhập kênh FAMILY_APP
async function familyAccount(familyMemberId, phone) {
  const username = 'qs.nt' + familyMemberId + F.tag.toLowerCase();
  const acc = await must(U.QL, 'POST', '/family-accounts', { familyMemberId, username, verification: { method: 'CALLBACK_REGISTERED_NUMBER', calledPhoneNumber: phone } }, 201);
  await login(username, acc.body.temporaryPassword, 'FAMILY_APP');
  const newPassword = 'Qs!' + F.tag + 'x2026';
  await must(username, 'POST', '/auth/change-password', { currentPassword: acc.body.temporaryPassword, newPassword }, [200, 204]);
  await login(username, newPassword, 'FAMILY_APP');
  return username;
}

// Bản ghi đón của 012 cho lượt tạm vắng, qua API thật: giấy tờ của người thân, yêu cầu bật "được phép đón" kèm bản ký của
// người đại diện, rồi ghi bản ghi đón
async function pickup(residentId, rel) {
  const doc = '0791' + String(rel.familyMemberId).padStart(8, '0');
  const fm = await call(U.HC, 'GET', `/family-members/${rel.familyMemberId}`);
  await must(U.HC, 'PUT', `/family-members/${rel.familyMemberId}`, { fullName: fm.body.familyMember.fullName, phoneNumber: rel.phoneNumber, idDocumentType: 'CCCD', idDocumentNumber: doc }, 200, { 'If-Match': fm.etag });
  const evidenceFileId = (await F.upload(U.HC)).body.fileId;
  const rr = await must(U.HC, 'POST', '/right-change-requests', { residentId, type: 'RIGHT', relationshipId: rel.relationshipId, right: 'PICKUP_ALLOWED', newValue: true, withSignedForm: true, signedByFamilyMemberId: rel.familyMemberId, evidenceFileId, reason: 'Gia đình đăng ký người đón' }, 201);
  if (rr.body?.status && rr.body.status !== 'APPLIED') console.log('  (yêu cầu đổi quyền đón ở trạng thái ' + rr.body.status + ')');
  const r = await call(U.HC, 'POST', `/residents/${residentId}/pickups`, { type: 'TEMPORARY_ABSENCE', familyMemberId: rel.familyMemberId, presentedIdDocument: { type: 'CCCD', number: doc }, handoverStaffId: STAFF.HC });
  if (r.status !== 201) throw new Error('Ghi bản ghi đón: ' + JSON.stringify(r.body));
  return r.body.id;
}

module.exports = async function part2(ctx) {
  ctx.nowIso = await F.now();
  const today = F.day(ctx.nowIso);
  // cơ cấu phòng riêng của phần 2 (phần 1 đã dùng giường P305); phí lưu trú theo loại phòng mới
  const s = await F.structure('B');
  const svc = { ...ctx.svc };
  for (const [k, rt] of [['feeA', s.rtA], ['feeB', s.rtB]]) {
    svc[k] = (await must(U.QL, 'POST', '/services', { type: 'RESIDENCY_FEE', name: `Phí lưu trú ${k} ${F.tag}B`, unit: 'tháng', billingPeriod: 'MONTH', roomTypeId: rt.id })).body;
    await must(U.QL, 'POST', `/services/${svc[k].id}/price-versions`, { unitPrice: 4500000, effectiveFrom: '2026-01-01' });
    svc[k].price = 4500000;
    svc[k].roomTypeId = rt.id;
  }
  const c2 = { ...ctx, s, svc };

  // ---- Q15, Q42: yêu cầu thay đổi trên hợp đồng của một người đã vào ở ----
  const P = await admitted(c2, 'Người Q15', s.beds['P305-G4'], s.rtB.id);
  const ct = P.contract;
  let r = await change(ct.id, ['CARE_LEVEL'], { careLevel: 'SPECIAL' }, F.day(today, 10));
  const req1 = r.body?.approvalRequestId;
  await approval(U.HC, req1, 'submit');
  const ap1 = await approval(U.QL, req1, 'approve', { comment: 'Đồng ý' });
  r = await change(ct.id, ['CARE_LEVEL'], { careLevel: 'BASIC' }, F.day(today, 12));
  const req2 = r.body?.approvalRequestId;
  await approval(U.HC, req2, 'submit');
  const ap2 = await approval(U.QL, req2, 'approve', { comment: 'Đồng ý' });
  check('Q15', `duyệt yêu cầu đổi mức thứ hai khi yêu cầu đầu chờ hiệu lực: 409 (FR-046) [${ap1.status}, ${ap2.status} ${codeOf(ap2) || ''}]`, ap1.status === 200 && ap2.status === 409, { ap1, ap2 });
  // FR-046: "cho tới khi yêu cầu đầu bị hủy": hủy yêu cầu đầu (phụ lục đóng sau commit, bất đồng bộ) rồi duyệt lại yêu cầu hai
  const cancel1 = await approval(U.QL, req1, 'cancel', { reason: 'Gia đình đổi ý' });
  let ap3;
  for (let i = 0; i < 20; i++) {
    ap3 = await approval(U.QL, req2, 'approve', { comment: 'Đồng ý' });
    if (!JSON.stringify(ap3.body).includes('DUPLICATE_PENDING_ATTRIBUTE')) break;
    await new Promise((res) => setTimeout(res, 500));
  }
  const add1 = db.query(`SELECT addendum_status FROM contract_version WHERE kind = 'ADDENDUM' AND approval_request_id = ${req1}`);
  const ev1 = db.query(`SELECT completion_date IS NOT NULL FROM event_publication WHERE event_type LIKE '%RequestCancelled' AND listener_id LIKE '%ResidencyChangeListeners%' AND serialized_event LIKE '{"request":{"id":${req1},%'`);
  check('Q15', `hủy yêu cầu đầu (${cancel1.status}) giải phóng thuộc tính: phụ lục ${add1.out}, listener RequestCancelled xong = ${ev1.out}; duyệt lại yêu cầu hai ${ap3.status}`,
    cancel1.status === 200 && add1.out === 'NOT_APPLIED' && ap3.status === 200, { cancel1: cancel1.status, add1, ev1, ap3 });
  await approval(U.QL, req2, 'cancel', { reason: 'Q15 xong' });

  // Q42 trên hợp đồng của người khác (không vướng yêu cầu của Q15): yêu cầu chỉ thêm dịch vụ thuộc gói → thay đổi nhỏ, HC
  // tự duyệt; thêm dịch vụ ngoài gói → HC 403, QL 200
  const P2 = await admitted(c2, 'Người Q42', s.beds['P305-G3'], s.rtB.id);
  const ct2 = P2.contract;
  r = await change(ct2.id, ['SERVICE'], { services: [{ serviceId: svc.rehab.id, action: 'ADD', inPackage: true }] }, F.day(today, 3), 'Thêm buổi phục hồi trong gói');
  const m1 = r.body;
  let view = (await call(U.HC, 'GET', `/residency-change-requests/${m1?.approvalRequestId}`)).body;
  await approval(U.HC, m1?.approvalRequestId, 'submit');
  const self = await approval(U.HC, m1?.approvalRequestId, 'approve', { comment: 'Tự duyệt thay đổi nhỏ' });
  const audit = db.query(`SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M15.APPROVAL_REQUEST' AND entity_id = '${m1?.approvalRequestId}' AND action = 'REQUEST_APPROVED' AND JSON_EXTRACT(details, '$.selfApproved') = true`);
  check('Q42', `yêu cầu 1 thêm dịch vụ trong gói: minorChange = ${view?.minorChange}, HC tự duyệt ${self.status}, nhật ký tự duyệt (${audit.out || audit.err})`, view?.minorChange === true && self.status === 200 && Number(audit.out) >= 1, { m1, view, self, audit });
  // yêu cầu 2 trên hợp đồng của Q15 (thuộc tính dịch vụ còn trống): yêu cầu 1 Đã duyệt chờ hiệu lực sẽ chặn yêu cầu cùng
  // loại trên hợp đồng của nó (FR-046), mà hủy yêu cầu không giải phóng được (lỗi ở Q15)
  r = await change(ct.id, ['SERVICE'], { services: [{ serviceId: svc.escort.id, action: 'ADD', inPackage: false }] }, F.day(today, 4), 'Thêm đưa đi khám ngoài gói');
  const m2 = r.body;
  await approval(U.HC, m2?.approvalRequestId, 'submit');
  view = (await call(U.HC, 'GET', `/residency-change-requests/${m2?.approvalRequestId}`)).body;
  const req = await call(U.HC, 'GET', `/approval-requests/${m2?.approvalRequestId}`);
  const hcTry = await approval(U.HC, m2?.approvalRequestId, 'approve', { comment: 'Thử duyệt' });
  const qlOk = await approval(U.QL, m2?.approvalRequestId, 'approve', { comment: 'Đồng ý' });
  const allowed = JSON.stringify(req.body?.allowedCommands || req.body?.permittedCommands || []);
  check('Q42', `yêu cầu 2 ngoài gói: minorChange = ${view?.minorChange}, HC duyệt ${hcTry.status}, lệnh được phép của HC ${allowed}, QL ${qlOk.status}`,
    view?.minorChange === false && hcTry.status === 403 && !/approve/i.test(allowed) && qlOk.status === 200, { view, hcTry, req: req.body, qlOk });

  // Q16: người đại diện lập yêu cầu thêm dịch vụ qua cổng; người thân khác (không phải đại diện) thử
  try {
    const repUser = await familyAccount(P.rep.familyMemberId, P.rep.phoneNumber);
    const other = await F.relative(P.resident.id, 'Người thân khác Q16', false, 'GRANDCHILD');
    const otherUser = await familyAccount(other.familyMemberId, other.phoneNumber);
    const body = { contractId: ct.id, changeTypes: ['SERVICE'], newValue: { services: [{ serviceId: svc.escort.id, action: 'ADD', inPackage: false }] }, requestedEffectiveDate: F.day(today, 20), reason: 'Gia đình muốn thêm đưa đi khám' };
    const byRep = await call(repUser, 'POST', '/residency-change-requests', body);
    const byOther = await call(otherUser, 'POST', '/residency-change-requests', body);
    const st = byRep.body?.approvalRequestId ? (await call(U.HC, 'GET', `/approval-requests/${byRep.body.approvalRequestId}`)).body?.status : undefined;
    const hcN = await notices(U.HC, 'REPRESENTATIVE_REQUEST_NEEDS_COMPLETION', P.resident.id);
    check('Q16', `người đại diện lập qua cổng: ${byRep.status} trạng thái ${st}; HC được báo (${hcN.length}); người thân khác ${byOther.status}`,
      byRep.status === 201 && st === 'DRAFT' && hcN.length >= 1 && byOther.status === 403, { byRep, st, hcN, byOther });
  } catch (e) {
    check('Q16', 'người đại diện lập yêu cầu qua cổng', false, e.message);
  }

  // ---- Danh sách chờ: dọn hồ sơ chờ của lần chạy trước để danh sách phù hợp chỉ có người của lần này ----
  const left = (await call(U.HC, 'GET', '/waiting-list-entries?status=WAITING&status=CONTACTED')).body || [];
  for (const e of left) await call(U.HC, 'POST', `/waiting-list-entries/${e.id}/cancel-waiting`, { reason: 'Dọn dữ liệu lần chạy trước' });
  const waiting = async (name) => {
    const res = await F.resident(name);
    const app = (await must(U.HC, 'POST', '/admission-applications', { residentId: res.id, applicationDate: today, desiredResidenceType: 'LONG_TERM_RESIDENTIAL', expectedCareLevel: 'REGULAR' })).body;
    const entry = (await must(U.HC, 'POST', '/waiting-list-entries', { applicationId: app.id }, 201)).body;
    return { res, app, entry };
  };
  const E = await waiting('Hồ sơ chờ E');
  const K = await waiting('Hồ sơ chờ K');

  // Q18 (hành vi mới Q-394): đánh dấu Ưu tiên bắt buộc lý do, nhật ký giá trị trước sau
  const noReason = await call(U.HC, 'POST', `/waiting-list-entries/${K.entry.id}/mark-priority`, { priority: true });
  const marked = await call(U.HC, 'POST', `/waiting-list-entries/${K.entry.id}/mark-priority`, { priority: true, reason: 'Neo đơn, cần vào sớm' });
  const unmarked = await call(U.HC, 'POST', `/waiting-list-entries/${K.entry.id}/mark-priority`, { priority: false, reason: 'Đã có người chăm' });
  const log18 = db.query(`SELECT COUNT(*) FROM audit_log WHERE entity_id = '${K.entry.id}' AND entity_type LIKE '%WAITING%' AND CAST(data_before AS CHAR) LIKE '%priority%' AND CAST(data_after AS CHAR) LIKE '%priority%'`);
  check('Q18', `đánh dấu Ưu tiên không lý do ${noReason.status}; có lý do ${marked.status} (priority ${marked.body?.priority}); gỡ dấu ${unmarked.status}; nhật ký trước/sau ${log18.out || log18.err}`,
    [400, 422].includes(noReason.status) && marked.status === 200 && marked.body?.priority === true && unmarked.status === 200 && unmarked.body?.priority === false && Number(log18.out) >= 2, { noReason, marked, unmarked, log18 });

  // Q20: giường Trống P201-G2; chọn K (không đứng đầu) không lý do → 422; có lý do → được; chọn E
  const bed = s.beds['P201-G2'];
  const list0 = (await call(U.HC, 'GET', `/beds/${bed.id}/matching-entries`)).body;
  const order = (list0?.entries || list0?.items || []).map((x) => x.id ?? x.entryId);
  let k1 = await call(U.HC, 'POST', `/waiting-list-entries/${K.entry.id}/record-contacted`, { bedId: bed.id });
  const k2 = await call(U.HC, 'POST', `/waiting-list-entries/${K.entry.id}/record-contacted`, { bedId: bed.id, outOfOrderReason: 'Gia đình K đến trước' });
  check('Q20', `danh sách phù hợp [${order}] E trước K; chọn K không lý do ${k1.status}; có lý do ${k2.status}`, order[0] === E.entry.id && order.includes(K.entry.id) && k1.status === 422 && k2.status === 200, { list0, k1, k2 });
  await call(U.HC, 'POST', `/waiting-list-entries/${K.entry.id}/decline-bed`, { reason: 'Gia đình K chưa sẵn sàng' });
  const e1 = await call(U.HC, 'POST', `/waiting-list-entries/${E.entry.id}/record-contacted`, { bedId: bed.id });
  const bedE = (await call(U.HC, 'GET', `/beds/${bed.id}`)).body;
  const holdH = bedE?.holdUntil;
  const hours = holdH ? (new Date(holdH) - new Date(await F.now())) / 3600e3 : NaN;
  check('Q20', `chọn E: ${e1.status} ${e1.body?.status}; giường ${bedE?.state} hạn giữ ${holdH} (~${hours.toFixed?.(1)} giờ, CFG-M02-02 48 giờ)`,
    e1.status === 200 && e1.body?.status === 'CONTACTED' && bedE?.state === 'HELD' && Math.abs(hours - 48) < 0.2, { e1, bedE });

  // Q21: tua 48 giờ 1 phút, job hết hạn giữ chỗ của 003
  ctx.nowIso = await F.clock({ advanceBy: 'PT48H1M' });
  // job 003 trả giường rồi gọi 004 trong cùng transaction (FR-015)
  const job21 = await call(U.QL, 'POST', '/testing/jobs/EXPIRE_BED_HOLDS');
  if (job21.status !== 200) check('Q21', 'job EXPIRE_BED_HOLDS chạy được', false, { status: job21.status, error: String(job21.body?.trace || JSON.stringify(job21.body)).split('\n').slice(0, 6).join(' | ') });
  const eAfter = (await call(U.HC, 'GET', `/waiting-list-entries/${E.entry.id}`)).body;
  const bedAfter = (await call(U.HC, 'GET', `/beds/${bed.id}`)).body;
  const list1 = (await call(U.HC, 'GET', `/beds/${bed.id}/matching-entries`)).body;
  const ids1 = (list1?.entries || list1?.items || []).map((x) => x.id ?? x.entryId);
  const hcNext = (await notices(U.HC, 'BED_AVAILABLE_FOR_WAITING_LIST')).filter((n) => n.sourceRecord?.id === String(bed.id) && n.sentAt > holdH);
  const hcNext2 = hcNext;
  check('Q21', `E về ${eAfter?.status}; giường ${bedAfter?.state}; E không còn trong danh sách lượt này [${ids1}]; HC được báo (${hcNext2.length})`,
    eAfter?.status === 'WAITING' && bedAfter?.state === 'EMPTY' && !ids1.includes(E.entry.id) && ids1.includes(K.entry.id) && hcNext2.length >= 1, { eAfter, bedAfter, list1, hcNext2 });

  // Q22: ghi nhận từ chối hồ sơ của F (có hợp đồng Nháp)
  const Fw = await waiting('Hồ sơ chờ F');
  await F.relative(Fw.res.id, 'Đại diện F');
  const draftF = (await must(U.HC, 'POST', '/contracts', { residentId: Fw.res.id, residenceType: 'LONG_TERM_RESIDENTIAL', startDate: F.day(today, 30), endDate: F.day(today, 395), careLevel: 'REGULAR', roomTypeId: s.rtA.id, residencyFeeBillingPeriod: 'MONTH', services: [{ serviceId: svc.careR.id, inPackage: true, quantity: 1 }, { serviceId: svc.feeA.id, inPackage: true, quantity: 1 }], depositAmount: 10000000 })).body.contract;
  const dec = await call(U.HC, 'POST', `/waiting-list-entries/${Fw.entry.id}/decline`, { reason: 'Gia đình không tiếp tục chờ' });
  const fe = (await call(U.HC, 'GET', `/waiting-list-entries/${Fw.entry.id}`)).body;
  const fa = (await call(U.HC, 'GET', `/admission-applications/${Fw.app.id}`)).body;
  const fr = (await call(U.HC, 'GET', `/residents/${Fw.res.id}`)).body;
  const fc = (await call(U.HC, 'GET', `/contracts/${draftF.id}`)).body;
  const fh = db.query(`SELECT reason FROM status_history WHERE entity_id = ${Fw.res.id} AND command = 'CANCEL_ADMISSION'`);
  check('Q22', `từ chối: ${dec.status}; hồ sơ chờ ${fe?.status}, lượt ${fa?.status}, NCT ${fr?.status}, hợp đồng Nháp ${fc?.contract?.status ?? fc?.status}, lý do 001 "${fh.out}"`,
    dec.status === 200 && fe?.status === 'DECLINED' && fa?.status === 'CANCELLED' && fr?.status === 'ADMISSION_CANCELLED' && (fc?.contract?.status ?? fc?.status) === 'CANCELLED' && fh.out.includes('Gia đình không tiếp tục chờ'), { dec, fe, fa, fr, fc, fh });
  for (const x of [E, K]) await call(U.HC, 'POST', `/waiting-list-entries/${x.entry.id}/cancel-waiting`, { reason: 'Kết thúc kịch bản' });

  // ---- Q23, Q24: tạm vắng Về nhà 5 ngày ----
  ctx.nowIso = await F.now();
  const d0 = F.day(ctx.nowIso);
  const W = await admitted(c2, 'Người tạm vắng Q23', s.beds['P305-G1'], s.rtB.id);
  const ret = F.at(F.day(d0, 4), '16:00');
  const details = (pickupRecordId) => ({ absenceType: 'HOME_VISIT', expectedReturnAt: ret, pickedUpById: W.rep.familyMemberId, pickupRecordId, handoverStaffId: STAFF.HC, takesMedication: false });
  const bad = await statusCmd(U.HC, W.resident.id, 'grant-temporary-absence', 'Gia đình đón về nhà', details(999999999));
  const recId = await pickup(W.resident.id, W.rep);
  const ok = await statusCmd(U.HC, W.resident.id, 'grant-temporary-absence', 'Gia đình đón về nhà', details(recId));
  const abs = ((await call(U.HC, 'GET', `/absences?residentId=${W.resident.id}`)).body || []).find((a) => a.status === 'OPEN');
  const bedW = (await call(U.HC, 'GET', `/beds/${s.beds['P305-G1'].id}`)).body;
  check('Q23', `người đón không hợp lệ ${bad.status} ${codeOf(bad) || ''}; hợp lệ ${ok.status} ${ok.body?.status}; lượt vắng mở ${abs?.id}; giường ${bedW?.state} (giữ)`,
    bad.status === 409 && ok.status === 200 && abs?.status === 'OPEN' && abs?.holdBed !== false, { bad, ok, abs, bedW });

  if (abs) {
    const ext = await call(U.HC, 'POST', `/absences/${abs.id}/extend-expected-return`, { expectedReturnAt: F.at(F.day(d0, 5), '16:00'), reason: 'Gia đình xin ở thêm một ngày' });
    const det = (await call(U.HC, 'GET', `/absences/${abs.id}`)).body;
    check('Q24', `Gia hạn dự kiến trở lại: ${ext.status}; lịch sử mốc ${det?.expectedReturnHistory?.length}`, ext.status === 200 && det?.expectedReturnHistory?.length >= 1, { ext, det });
    ctx.nowIso = await F.clock({ setTo: F.at(F.day(d0, 5), '16:00') });
    await F.runJob('M02_DAILY');
    const back = await statusCmd(U.HC, W.resident.id, 'record-return', 'Về viện', { returnedAt: F.at(F.day(d0, 5), '16:00') });
    const closed = (await call(U.HC, 'GET', `/absences/${abs.id}`)).body;
    check('Q24', `Ghi nhận trở về 16:00: ${back.status}, lượt vắng ${closed?.status}`, back.status === 200 && closed?.status === 'CLOSED', { back, closed });
    note('Q24', 'CẦN XEM TAY', 'ngày trở về 16:00 vẫn là ngày vắng, 100% giữ giường: chỉ có ResidencyDayApi nội bộ (không có HTTP), xem bảng kỳ 010 của người này hoặc ResidencyDayApiIT');
    ctx.absW = { residentId: W.resident.id, absenceId: abs.id };
  }

  // ---- Q25 (hành vi mới Q-395): chuyển viện; ngày CFG-M02-05 báo QL đúng một lần; QL giải phóng giường ----
  const C = await admitted(c2, 'Người chuyển viện Q25', s.beds['P305-G2'], s.rtB.id);
  const tr = await statusCmd(U.BS, C.resident.id, 'transfer-out', 'Chuyển viện cấp cứu', { transferUnit: 'Bệnh viện Đa khoa', handoverStaffId: STAFF.BS, basis: { type: 'FAMILY_INFORMATION', note: 'Gia đình báo' } });
  const absC = ((await call(U.HC, 'GET', `/absences?residentId=${C.resident.id}`)).body || []).find((a) => a.status === 'OPEN');
  const startC = await F.now();
  for (let i = 1; i <= 31; i++) {
    await F.clock({ setTo: F.at(F.day(startC, i), '00:20') });
    await F.runJob('M02_DAILY');
  }
  ctx.nowIso = await F.now();
  const qlN2 = await notices(U.QL, 'LONG_ABSENCE_MANAGER_NOTICE', C.resident.id);
  check('Q25', `chuyển viện ${tr.status}; qua 31 ngày job M02_DAILY: QL được báo đúng một lần (${qlN2.length})`, tr.status === 200 && qlN2.length === 1, { tr, absC, qlN2 });
  const hcRel = absC ? await call(U.HC, 'POST', `/absences/${absC.id}/release-bed`, { reason: 'Thử bằng Hành chính' }) : {};
  const rel = absC ? await call(U.QL, 'POST', `/absences/${absC.id}/release-bed`, { reason: 'Gia đình báo ở viện lâu' }) : {};
  const bedC = (await call(U.HC, 'GET', `/beds/${s.beds['P305-G2'].id}`)).body;
  const evC = db.query(`SELECT COUNT(*) FROM event_publication WHERE event_type LIKE '%ResidencyDaysChanged%' AND serialized_event LIKE '%"residentId":${C.resident.id},%'`);
  check('Q25', `HC giải phóng ${hcRel.status}; QL giải phóng ${rel.status}; giường ${bedC?.state}; 010 nhận NgayLuuTruDaDoi (${evC.out})`,
    hcRel.status === 403 && rel.status === 200 && bedC?.state !== 'OCCUPIED' && Number(evC.out) >= 1, { hcRel, rel, bedC, evC });
  ctx.C25 = { residentId: C.resident.id, absenceId: absC?.id };

  // ---- Q29: đính chính lượt vắng đã đóng (W) đổi giờ trở về muộn thêm một ngày ----
  if (ctx.absW) {
    const before = db.query(`SELECT COUNT(*) FROM event_publication WHERE event_type LIKE '%ResidencyDaysChanged%' AND serialized_event LIKE '%"residentId":${ctx.absW.residentId},%'`).out;
    const cor = await call(U.HC, 'POST', `/absences/${ctx.absW.absenceId}/correct`, { type: 'EDIT_CONTENT', correctedContent: { returnedAt: F.at(F.day(d0, 6), '09:00') }, reason: 'Ghi sai giờ trở về' });
    const after = db.query(`SELECT COUNT(*) FROM event_publication WHERE event_type LIKE '%ResidencyDaysChanged%' AND serialized_event LIKE '%"residentId":${ctx.absW.residentId},%'`).out;
    const det = (await call(U.HC, 'GET', `/absences/${ctx.absW.absenceId}`)).body;
    check('Q29', `đính chính lượt vắng đã đóng: ${cor.status}; trở về ${det?.returnedAt}; 010 nhận NgayLuuTruDaDoi (${before} → ${after})`,
      cor.status === 201 && Number(after) > Number(before), { cor, det });
  }

  // ---- Q30: bán trú: báo vắng trước 25 giờ; khai báo ngày nghỉ cho hôm qua ----
  ctx.nowIso = await F.now();
  const dc = F.day(ctx.nowIso);
  let B;
  try {
    // bán trú cần khu nghỉ Hiệu lực của 003 (DAY_CARE_NOT_CONFIGURED); khu nghỉ có rồi thì 409, bỏ qua
    await call(U.QL, 'POST', '/day-care-rest-areas', { code: 'BT' + F.tag, name: 'Khu nghỉ bán trú ' + F.tag, areaId: s.area.id });
    // và sức chứa mỗi buổi CFG-M03-01 (không có mặc định, DAY_CARE_CAPACITY_UNSET)
    const cap = await call(U.QL, 'GET', '/parameters/CFG-M03-01');
    if (cap.body?.currentValue == null) await call(U.QL, 'POST', '/parameters/CFG-M03-01/change-value', { value: 50, reason: 'Quickstart 004: sức chứa bán trú' }, { 'If-Match': cap.etag });
    B = await F.readyForAdmission({ name: 'Bán trú B', type: 'DAY_CARE', start: dc, end: F.day(dc, 180), svc, today: dc, nowIso: ctx.nowIso });
    const ad = await F.completeAdmission(B.resident.id);
    if (ad.status !== 200) throw new Error('Hoàn tất tiếp nhận bán trú: ' + JSON.stringify(ad.body));
  } catch (e) {
    check('Q30', 'dựng người bán trú đã vào ở', false, e.message);
  }
  if (B) {
    const target = F.day(dc, 2);
    // báo lúc này cho buổi 07:30 ngày kia: trước ít nhất 25 giờ (CFG-M02-06)
    const notice = await call(U.HC, 'POST', '/day-care-absence-notices', { residentId: B.resident.id, date: target, familyNotifiedAt: await F.now() });
    check('Q30', `báo vắng bán trú trước 25 giờ: ${notice.status}, đủ hạn báo = ${notice.body?.withinNoticePeriod}`, notice.status === 201 && notice.body?.withinNoticePeriod === true, notice);
    note('Q30', 'CẦN XEM TAY', 'ngày vắng có báo tính 0%: xem dòng bán trú trong bảng kỳ 010 (004 chỉ có ResidencyDayApi nội bộ)');
    const yest = F.day(dc, -1);
    const c1 = await call(U.HC, 'POST', '/day-care-closure-days', { date: yest });
    const c2 = await call(U.HC, 'POST', '/day-care-closure-days', { date: yest, reason: 'Mất điện cả ngày, khai báo bù' });
    await new Promise((res) => setTimeout(res, 1500));
    // DayCareClosureDayChanged: mỗi listener (010 billing, 011 nutrition, 005 care) một dòng event_publication
    const ev = db.query(`SELECT SUBSTRING_INDEX(SUBSTRING_INDEX(listener_id, '.', 4), '.', -1), completion_date IS NOT NULL FROM event_publication WHERE event_type LIKE '%DayCareClosureDayChanged' AND serialized_event LIKE '%${yest}%' ORDER BY publication_date DESC LIMIT 6`);
    const got = ev.out.split('\n').map((l) => l.trim()).filter(Boolean);
    check('Q30', `ngày nghỉ hôm qua không lý do ${c1.status} (thiếu reason bắt buộc theo hợp đồng); có lý do ${c2.status}; sự kiện tới [${got.join('; ')}]`,
      [400, 422].includes(c1.status) && c2.status === 201 && got.includes('billing\t1') && got.includes('nutrition\t1'), { c1, c2, ev });
    if (c2.body?.id) await call(U.HC, 'POST', `/day-care-closure-days/${c2.body.id}/cancel`, { reason: 'Dọn sau kịch bản' });
  }
};
