// Dựng dữ liệu nền cho script quickstart 004, 005, 008, 015 bằng API thật (demo chỉ có 9 tài khoản demo/V002_90 và
// hồ sơ nhân viên 9001 → 9009 của seed-staff.sql). Mỗi lần chạy tạo đối tượng mới có hậu tố riêng nên chạy lại được.
const { login, call, upload } = require('./api.cjs');

const U = {
  QL: 'demo.quanly', QL2: 'demo.quanly2', TT: 'demo.truongtang', BS: 'demo.bacsi', DD: 'demo.dieuduong',
  CS: 'demo.chamsoc', BEP: 'demo.bep', HC: 'demo.hanhchinh', KT: 'demo.ketoan',
};
const STAFF = { QL: 9001, QL2: 9002, TT: 9003, BS: 9004, DD: 9005, CS: 9006, BEP: 9007, HC: 9008, KT: 9009 };
const ALL = Object.values(U);

async function loginAll() { for (const u of ALL) await login(u); }

// Gọi và ném lỗi khi mã khác mong đợi: chỉ dùng cho bước dựng dữ liệu, không cho bước kiểm
async function must(user, method, path, body, expect = [200, 201], headers) {
  const r = await call(user, method, path, body, headers);
  if (!(Array.isArray(expect) ? expect : [expect]).includes(r.status)) {
    throw new Error(`Dựng dữ liệu lỗi ${method} ${path} → ${r.status} ${JSON.stringify(r.body).slice(0, 800)}`);
  }
  return r;
}
// Lệnh cần If-Match: đọc ETag ở đường dẫn đọc rồi gửi
async function withEtag(user, readPath, method, path, body, headers = {}) {
  const cur = await call(user, 'GET', readPath);
  return call(user, method, path, body, { 'If-Match': cur.etag, ...headers });
}

// Đồng hồ giả lập; tua làm phiên hết hạn nên đăng nhập lại
async function clock(body) {
  const r = await call(U.QL, 'POST', '/testing/clock', body);
  if (r.status !== 200) throw new Error('clock ' + JSON.stringify(r.body));
  if (body.advanceBy || body.setTo) await loginAll();
  return r.body.now;
}
const now = async () => (await call(U.QL, 'GET', '/testing/clock')).body.now;
const runJob = (name) => must(U.QL, 'POST', '/testing/jobs/' + name, undefined, 200);
// Ngày theo giờ Việt Nam (chuỗi +07:00 của /testing/clock)
const day = (iso, plus = 0) => { const d = new Date(iso.slice(0, 10) + 'T00:00:00Z'); d.setUTCDate(d.getUTCDate() + plus); return d.toISOString().slice(0, 10); };
// Thời điểm giờ Việt Nam của ngày d, giờ hh:mm
const at = (d, hhmm) => `${d}T${hhmm}:00+07:00`;

const tag = Date.now().toString(36).slice(-5).toUpperCase();

// Cơ cấu: một khu, một tầng, loại phòng thường và loại phòng khác giá, phòng P201 nữ (Thường xuyên, Đặc biệt) và P305
async function structure(sfx = '') {
  const t = tag + sfx;
  const area = (await must(U.QL, 'POST', '/areas', { code: 'K' + t, name: 'Khu ' + t })).body;
  const floor = (await must(U.QL, 'POST', '/floors', { areaId: area.id, code: 'T' + t, name: 'Tầng ' + t })).body;
  const rtA = (await must(U.QL, 'POST', '/room-types', { name: 'Phòng 2 giường ' + t, standardBeds: 2, defaultCareLevels: ['FREQUENT', 'SPECIAL'], servesLongTerm: true, servesShortTerm: true })).body;
  const rtB = (await must(U.QL, 'POST', '/room-types', { name: 'Phòng 4 giường ' + t, standardBeds: 4, defaultCareLevels: ['BASIC', 'FREQUENT', 'SPECIAL'], servesLongTerm: true, servesShortTerm: true })).body;
  const p201 = (await must(U.QL, 'POST', '/rooms', { floorId: floor.id, code: 'P201' + t, name: 'P201', roomTypeId: rtA.id, allowedCareLevels: ['FREQUENT', 'SPECIAL'], genderPolicy: 'FEMALE' })).body;
  const p305 = (await must(U.QL, 'POST', '/rooms', { floorId: floor.id, code: 'P305' + t, name: 'P305', roomTypeId: rtB.id, allowedCareLevels: ['BASIC', 'FREQUENT', 'SPECIAL'], genderPolicy: 'UNRESTRICTED' })).body;
  const beds = {};
  for (const [room, codes] of [[p201, ['G1', 'G2']], [p305, ['G1', 'G2', 'G3', 'G4']]]) {
    for (const c of codes) beds[room.name + '-' + c] = (await must(U.QL, 'POST', '/beds', { roomId: room.id, code: c })).body.bed;
  }
  return { area, floor, rtA, rtB, p201, p305, beds };
}

async function resident(name, gender = 'FEMALE', born = '1945-03-10') {
  const r = await must(U.HC, 'POST', '/residents', { fullName: `${name} ${tag}`, dateOfBirth: born, gender, confirmNotDuplicate: true });
  return r.body.profile;
}

// Danh mục dịch vụ của quickstart 004 mục 1: ba dịch vụ chăm sóc theo mức, phí lưu trú theo hai loại phòng, phục hồi,
// đưa đi khám; mỗi dịch vụ một phiên bản đơn giá từ 01/01
async function services(s) {
  const defs = {
    careB: { type: 'CARE', name: 'Chăm sóc cơ bản', unit: 'tháng', billingPeriod: 'MONTH', careLevelCode: 'BASIC', price: 3000000 },
    careR: { type: 'CARE', name: 'Chăm sóc thường xuyên', unit: 'tháng', billingPeriod: 'MONTH', careLevelCode: 'REGULAR', price: 6000000 },
    careS: { type: 'CARE', name: 'Chăm sóc đặc biệt', unit: 'tháng', billingPeriod: 'MONTH', careLevelCode: 'SPECIAL', price: 8000000 },
    feeA: { type: 'RESIDENCY_FEE', name: 'Phí lưu trú phòng 2 giường', unit: 'tháng', billingPeriod: 'MONTH', roomTypeId: s.rtA.id, price: 5000000 },
    feeB: { type: 'RESIDENCY_FEE', name: 'Phí lưu trú phòng 4 giường', unit: 'tháng', billingPeriod: 'MONTH', roomTypeId: s.rtB.id, price: 4000000 },
    daycare: { type: 'DAY_CARE_SESSION', name: 'Bán trú', unit: 'buổi', billingPeriod: 'SESSION', price: 300000 },
    rehab: { type: 'REHABILITATION', name: 'Phục hồi chức năng', unit: 'buổi', billingPeriod: 'OCCURRENCE', price: 200000 },
    escort: { type: 'MEDICAL_ESCORT', name: 'Đưa đi khám', unit: 'lần', billingPeriod: 'OCCURRENCE', price: 500000 },
  };
  const out = {};
  for (const [k, { price, ...b }] of Object.entries(defs)) {
    const created = await call(U.QL, 'POST', '/services', { ...b, name: `${b.name} ${tag}` });
    if (created.status === 409 && created.body?.errorCode === 'CARE_SERVICE_LEVEL_EXISTS') {
      // mỗi mức chỉ một dịch vụ chăm sóc Hiệu lực (Q-297): lần chạy sau dùng lại dịch vụ của lần trước
      out[k] = (await call(U.QL, 'GET', `/services?type=${b.type}`)).body.find((x) => x.careLevelCode === b.careLevelCode && x.status === 'ACTIVE');
      const versions = (await call(U.QL, 'GET', `/services/${out[k].id}/price-versions`)).body;
      out[k].price = versions[versions.length - 1].unitPrice;
      continue;
    }
    if (created.status !== 201) throw new Error(`Dựng dữ liệu lỗi POST /services → ${created.status} ${JSON.stringify(created.body)}`);
    out[k] = created.body;
    out[k].price = price;
    out[k].priceVersion = (await must(U.QL, 'POST', `/services/${out[k].id}/price-versions`, { unitPrice: price, effectiveFrom: '2026-01-01' })).body;
  }
  return out;
}

// Người thân có quan hệ Hiệu lực; representative = true thì là người đại diện đầu tiên (012)
async function relative(residentId, name, representative = true, relationship = 'CHILD') {
  const phoneNumber = '09' + String(Date.now() + Math.floor(Math.random() * 1e6)).slice(-8);
  const fm = (await must(U.HC, 'POST', '/family-members', { fullName: `${name} ${tag}`, phoneNumber })).body;
  const ev = (await upload(U.HC)).body.fileId;
  const rel = (await must(U.HC, 'POST', `/residents/${residentId}/relationships`, { familyMemberId: fm.id, relationship, primaryContact: representative, setFirstRepresentative: representative, evidenceFileId: ev, reason: 'Quan hệ gia đình' })).body;
  return { familyMemberId: fm.id, relationshipId: rel.id, phoneNumber };
}

async function confirmAssessment(residentId, careLevel = 'REGULAR', residenceType = 'LONG_TERM_RESIDENTIAL', type = 'INITIAL') {
  const as = await must(U.BS, 'POST', `/residents/${residentId}/assessments`, { type, basis: 'Khám đầu vào' });
  await must(U.BS, 'PUT', `/assessments/${as.body.id}`, { basis: 'Khám', comment: 'Nhận xét của bác sĩ', careLevel, careNeedIds: [], proposedResidenceType: residenceType }, 200, { 'If-Match': as.etag });
  const r = await withEtag(U.BS, `/assessments/${as.body.id}`, 'POST', `/assessments/${as.body.id}/confirm`);
  if (r.status !== 200) throw new Error('confirm assessment ' + JSON.stringify(r.body));
  return as.body.id;
}

const careService = (svc, level) => ({ BASIC: svc.careB, REGULAR: svc.careR, SPECIAL: svc.careS })[level];

// Hợp đồng Nháp → Chờ ký → Hiệu lực (người ký là người đại diện)
async function activeContract(o) {
  const { residentId, start, end, level = 'REGULAR', type = 'LONG_TERM_RESIDENTIAL', svc, roomTypeId, signer, signedDate, extra = {} } = o;
  const services = type === 'DAY_CARE'
    ? [{ serviceId: careService(svc, level).id, inPackage: true, quantity: 1 }, { serviceId: svc.daycare.id, inPackage: true, quantity: 1 }]
    : [{ serviceId: careService(svc, level).id, inPackage: true, quantity: 1 }, { serviceId: (roomTypeId === svc.feeB.roomTypeId ? svc.feeB : svc.feeA).id, inPackage: true, quantity: 1 }];
  const body = { residentId, residenceType: type, startDate: start, endDate: end, careLevel: level, roomTypeId: type === 'DAY_CARE' ? undefined : roomTypeId, residencyFeeBillingPeriod: type === 'DAY_CARE' ? 'SESSION' : 'MONTH', services, depositAmount: 10000000, ...extra };
  // bán trú cần lịch đến theo buổi (FR-019): mọi ngày trong tuần 07:30 → 16:30
  if (type === 'DAY_CARE' && !body.arrivalSchedule) body.arrivalSchedule = [1, 2, 3, 4, 5, 6, 7].map((dayOfWeek) => ({ dayOfWeek, arrivalTime: '07:30', departureTime: '16:30' }));
  const ct = (await must(U.HC, 'POST', '/contracts', body)).body.contract;
  let r = await withEtag(U.HC, `/contracts/${ct.id}`, 'POST', `/contracts/${ct.id}/send-for-signature`);
  if (r.status !== 200) throw new Error('send-for-signature ' + JSON.stringify(r.body));
  const scan = (await upload(U.HC)).body.fileId;
  r = await withEtag(U.HC, `/contracts/${ct.id}`, 'POST', `/contracts/${ct.id}/record-signed`, { signedDate, signingRepresentativeId: signer, scanFileId: scan });
  if (r.status !== 200) throw new Error('record-signed ' + JSON.stringify(r.body));
  return r.body.contract;
}

// Đủ điều kiện tiếp nhận (chưa Hoàn tất): lượt đăng ký, đánh giá, người đại diện, hợp đồng Hiệu lực, giường, cọc
async function readyForAdmission(o) {
  const { name, gender = 'FEMALE', level = 'REGULAR', type = 'LONG_TERM_RESIDENTIAL', start, end, svc, bed, roomTypeId, today, nowIso } = o;
  const res = await resident(name, gender);
  const app = (await must(U.HC, 'POST', '/admission-applications', { residentId: res.id, applicationDate: today, desiredResidenceType: type, expectedCareLevel: level, desiredAdmissionDate: start })).body;
  await confirmAssessment(res.id, level, type);
  const rep = await relative(res.id, 'Người đại diện của ' + name);
  const contract = await activeContract({ residentId: res.id, start, end, level, type, svc, roomTypeId, signer: rep.familyMemberId, signedDate: today });
  let allocation = null;
  if (type !== 'DAY_CARE') allocation = (await must(U.HC, 'POST', '/bed-allocations', { residentId: res.id, bedId: bed.id, startAt: at(start, '08:00'), reason: 'Phân bổ ban đầu' })).body;
  await must(U.KT, 'POST', `/residents/${res.id}/cash-payments`, { ledger: 'DEPOSIT', amount: 10000000, payerName: 'Người đại diện', receivedAt: nowIso });
  return { resident: res, app, rep, contract, allocation };
}

const completeAdmission = (residentId, reason = 'Tiếp nhận vào ở') =>
  withEtag(U.HC, `/residents/${residentId}`, 'POST', `/residents/${residentId}/complete-admission`, { reason });

module.exports = {
  U, STAFF, ALL, loginAll, must, withEtag, clock, now, runJob, day, at, tag, structure, resident, call, login, upload,
  services, relative, confirmAssessment, activeContract, readyForAdmission, completeAdmission, careService,
};
