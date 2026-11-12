// Dữ liệu điều kiện cần của quickstart (mục 1) mà demo/V*_90 chưa có: hồ sơ nhân viên 9001 → 9009 của tài khoản mẫu,
// bộ phận, cơ cấu khu – tầng – phòng – giường (tạo bằng API 003 nên tài sản giường của 019 tạo cùng lệnh), Trưởng tầng,
// ca đã công bố, phân công, người cao tuổi đang lưu trú có hợp đồng và phân bổ giường. Phần không có API tạo trực tiếp
// (hồ sơ nhân viên của tài khoản có sẵn, ca, phân công, hợp đồng Hiệu lực) ghi thẳng như support/RealCareFixture.java.
const q = require('./qs.cjs');
const { sql, one, insertId, s } = q;

const STAFF = {
  'demo.quanly': 9001, 'demo.quanly2': 9002, 'demo.truongtang': 9003, 'demo.bacsi': 9004, 'demo.dieuduong': 9005,
  'demo.chamsoc': 9006, 'demo.bep': 9007, 'demo.hanhchinh': 9008, 'demo.ketoan': 9009,
};
const POSITION = { 9001: 'FACILITY_MANAGER', 9002: 'FACILITY_MANAGER', 9003: 'FLOOR_MANAGER', 9004: 'DOCTOR', 9005: 'NURSING_STAFF', 9006: 'CARE_STAFF', 9007: 'KITCHEN_STAFF', 9008: 'ADMINISTRATIVE_STAFF', 9009: 'ACCOUNTANT' };
const accountId = username => Number(one(`SELECT id FROM account WHERE username=${s(username)}`));
const utc = iso => new Date(iso).toISOString().replace('T', ' ').replace('Z', '');

/** Id vật lý của bộ phận (catalog_item, cột staff_department.department_id) theo tên. */
const department = name => Number(one(`SELECT id FROM catalog_item WHERE catalog_type='M09.DEPARTMENT' AND name=${s(name)}`));

function staffProfiles() {
  for (const [u, id] of Object.entries(STAFF)) {
    if (one(`SELECT COUNT(*) FROM staff_profile WHERE id=${id}`) === '0')
      sql(`INSERT INTO staff_profile(id,full_name,position,account_id,start_date) VALUES (${id},${s('[Demo] ' + u)},${s(POSITION[id])},${accountId(u)},'2026-01-01')`);
  }
  const dep = { 9004: 'Y tế', 9005: 'Y tế', 9003: 'Chăm sóc', 9006: 'Chăm sóc', 9007: 'Bếp', 9008: 'Hành chính – Kế toán', 9009: 'Hành chính – Kế toán' };
  for (const [id, name] of Object.entries(dep)) {
    if (one(`SELECT COUNT(*) FROM staff_department WHERE staff_id=${id} AND ends_at IS NULL`) === '0')
      sql(`INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) VALUES (${id},${department(name)},'2026-01-01 00:00:00',${accountId('demo.quanly')},'[Demo] dữ liệu nghiệm thu')`);
  }
}

/** Phụ trách bộ phận bằng API 008 (Quản lý viện). API dùng mã bộ phận logic (GET /departments), khác id của department(). */
async function assignLead(depName, username) {
  const list = await q.get(q.QLV, '/departments');
  const dep = (list.body ?? []).find(d => d.name === depName);
  if (!dep) throw new Error('không có bộ phận ' + depName + ': ' + JSON.stringify(list.body));
  if (dep.leadStaffId === STAFF[username]) return { status: 200, body: dep };
  return q.post(q.QLV, `/departments/${dep.id}/assign-lead`, { staffId: STAFF[username], reason: '[Demo] nghiệm thu quickstart' });
}

/**
 * Cơ cấu chung qua API 003: Khu A (QA); tầng QT1, QT2; loại phòng 2 giường; mỗi tầng hai phòng (QP101, QP102, QP201,
 * QP202) hai giường G1, G2. Phần nào chưa có thì tạo (chạy lại sau lần chạy dở được). Mức chăm sóc của 003 là
 * BASIC/FREQUENT/SPECIAL (001, 004 dùng REGULAR).
 */
async function structure() {
  const must = (r, what) => { if (r.status >= 300) throw new Error(what + ' ' + r.status + ' ' + JSON.stringify(r.body)); return r.body.id ?? r.body.bed?.id; };
  const levels = ['BASIC', 'FREQUENT', 'SPECIAL'];
  const TYPE = '[Demo] Phòng 2 giường';
  const area = Number(one(`SELECT id FROM area WHERE code='QA'`)) || must(await q.post(q.QLV, '/areas', { code: 'QA', name: 'Khu A (demo)' }), 'area');
  const floor = async (code, name) => Number(one(`SELECT id FROM floor WHERE code=${s(code)}`)) || must(await q.post(q.QLV, '/floors', { areaId: area, code, name }), 'floor');
  const floor1 = await floor('QT1', 'Tầng 1 (demo)');
  const floor2 = await floor('QT2', 'Tầng 2 (demo)');
  const roomType = Number(one(`SELECT id FROM room_type WHERE name=${s(TYPE)}`)) || must(await q.post(q.QLV, '/room-types', { name: TYPE, standardBeds: 2, defaultCareLevels: levels }), 'room type');
  const rooms = {}, beds = {};
  for (const [fl, code] of [[floor1, 'QP101'], [floor1, 'QP102'], [floor2, 'QP201'], [floor2, 'QP202']]) {
    const room = Number(one(`SELECT id FROM room WHERE code=${s(code)}`)) || must(await q.post(q.QLV, '/rooms', { floorId: fl, code, name: code.slice(1), roomTypeId: roomType, allowedCareLevels: levels, genderPolicy: 'UNRESTRICTED' }), 'room');
    rooms[code.slice(1)] = room;
    for (const b of ['G1', 'G2'])
      beds[code.slice(1) + b] = Number(one(`SELECT id FROM bed WHERE room_id=${room} AND code=${s(b)}`)) || must(await q.post(q.QLV, '/beds', { roomId: room, code: b }), 'bed');
  }
  return { area, floor1, floor2, roomType, rooms, beds };
}

/** Giao Trưởng tầng từ hôm qua (theo đồng hồ giả lập), chưa kết thúc. */
function floorManager(username, floorId, fromIso) {
  const st = STAFF[username];
  if (one(`SELECT COUNT(*) FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND staff_id=${st} AND floor_area_id=${floorId} AND status='ACTIVE'`) !== '0') return;
  sql(`INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,assigned_by,status) VALUES ('FLOOR_MANAGER',${st},${floorId},${s(utc(fromIso))},1,'ACTIVE')`);
}

/** Ca đã công bố của tầng từ startIso, dài hours giờ (dùng chung lịch tháng của tầng, lịch duy nhất theo tháng – tầng). */
function shift(floorId, startIso, hours) {
  const start = new Date(startIso);
  const month = new Date(start.getTime() + 7 * 3600e3).toISOString().slice(0, 7);
  const schedule = insertId(`INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (${s(month)},'FLOOR',${floorId},'PUBLISHED') ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)`);
  const tname = '[Mẫu ca demo] #' + q.uid();
  const template = insertId(`INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES ('M09.SHIFT_TEMPLATE','M09',${s(tname)},'ACTIVE',JSON_OBJECT('createdAt',${s(start.toISOString())},'startTime','07:00:00','endTime','15:00:00','scopeType','FLOOR','requiresHandover',CAST('false' AS JSON),'originalName','[Mẫu ca demo]'))`);
  const end = new Date(start.getTime() + hours * 3600e3);
  return insertId(`INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover,status) VALUES (${schedule},${template},'FLOOR',${floorId},${s(utc(start))},${s(utc(end))},FALSE,'PUBLISHED')`);
}
const member = (shiftId, username) => sql(`INSERT INTO shift_staff(shift_id,staff_id) VALUES (${shiftId},${STAFF[username] ?? username})`);
const lead = (shiftId, username, fromIso) => sql(`INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,command,by_system) VALUES ('M09.SHIFT',${shiftId},'LEAD_STAFF','M09','PERIOD',${STAFF[username] ?? username},${s(utc(fromIso))},'SCHEDULED',FALSE)`);
/** Phân công chính người cao tuổi trong ca. */
function assignResident(shiftId, username, role, residentId, fromIso, toIso, nurse = false) {
  return insertId(`INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,assigned_nurse,starts_at,ends_at,status,created_by) VALUES (${shiftId},${STAFF[username] ?? username},'RESIDENT',${residentId},${s(role)},'PRIMARY',${nurse ? 'TRUE' : 'FALSE'},${s(utc(fromIso))},${s(utc(toIso))},'ACTIVE',1)`);
}

/**
 * Người cao tuổi ở status (5.5), mức careLevel (mã 001: BASIC/REGULAR/SPECIAL), hợp đồng Hiệu lực loại residenceType từ sinceIso (như hợp đồng đã ký
 * của 004), không giường; bedId thì có phân bổ giường Đang hiệu lực và giường Đang sử dụng.
 */
function resident({ status = 'IN_RESIDENCE', residenceType = 'LONG_TERM_RESIDENTIAL', sinceIso, careLevel = 'BASIC', gender = 'FEMALE', name, bedId, dob = '1940-05-01', contract = true } = {}) {
  const code = 'QS-' + q.uid() + q.uid();
  const fullName = name ?? '[Demo] Người cao tuổi ' + code.slice(3, 9);
  sql(`INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since, current_care_level, created_at, created_by) VALUES (${s(code)}, ${s(fullName)}, ${s(dob)}, ${s(gender)}, ${s(status)}, ${s(utc(sinceIso))}, ${s(careLevel)}, NOW(6), 1)`);
  const id = Number(one(`SELECT id FROM resident WHERE profile_code=${s(code)}`));
  if (status !== 'ADMISSION_IN_PROGRESS')
    sql(`INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value_before, value, starts_at, command, actor_id, by_system) VALUES ('M01.RESIDENT', ${id}, 'STATUS', 'M01', 'POINT', 'ADMISSION_IN_PROGRESS', ${s(status)}, ${s(utc(sinceIso))}, 'SEED', 1, TRUE)`);
  sql(`INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at, actor_id, by_system) VALUES ('M01.RESIDENT', ${id}, 'CARE_LEVEL', 'M01', 'PERIOD', ${s(careLevel)}, ${s(utc(sinceIso))}, 1, TRUE)`);
  if (contract) {
    const start = new Date(new Date(sinceIso).getTime() + 7 * 3600e3).toISOString().slice(0, 10);
    const end = residenceType === 'SHORT_TERM_RESIDENTIAL' ? new Date(Date.parse(start) + 400 * 864e5).toISOString().slice(0, 10) : null;
    const c = insertId(`INSERT INTO contract (resident_id, residence_type, start_date, end_date, care_level, residency_fee_billing_period, status, signed_date, signing_representative_id, scan_file_id, created_by, created_at) VALUES (${id}, ${s(residenceType)}, ${s(start)}, ${s(end)}, ${s(careLevel)}, 'MONTH', 'ACTIVE', ${s(start)}, 1, 1, 1, NOW())`);
    sql(`UPDATE contract SET contract_number = ${s('HD-' + start.slice(0, 4) + '-' + String(c).padStart(6, '0'))} WHERE id = ${c}`);
    sql(`INSERT INTO contract_version (kind, contract_id, services, arrival_schedule, created_at) VALUES ('DRAFT', ${c}, '[]', '[]', UTC_TIMESTAMP(6))`);
    sql(`INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type, care_level, created_at, locked_at) VALUES ('CONTENT', ${c}, 1, ${s(start)}, ${s(JSON.stringify({ residenceType, careLevel, startDate: start, services: [], arrivalSchedule: [] }))}, ${s(residenceType)}, ${s(careLevel)}, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))`);
  }
  if (bedId) {
    sql(`INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES (${id},${bedId},'ACTIVE',${s(utc(sinceIso))},${s(careLevel === 'REGULAR' ? 'FREQUENT' : careLevel)},'CURRENT')`);
    sql(`UPDATE bed SET state='OCCUPIED' WHERE id=${bedId}`);
  }
  return id;
}

/** Giải phóng giường của mọi người cao tuổi demo cũ trên giường (chạy lại script): đóng phân bổ, giường Trống. */
function freeBed(bedId) {
  sql(`UPDATE bed_allocation SET status='CLOSED', end_at=UTC_TIMESTAMP(6) WHERE bed_id=${bedId} AND status IN ('ACTIVE','FUTURE')`);
  sql(`UPDATE bed SET state='EMPTY' WHERE id=${bedId}`);
}

async function base() {
  staffProfiles();
  return structure();
}

module.exports = { STAFF, accountId, department, staffProfiles, assignLead, structure, floorManager, shift, member, lead, assignResident, resident, freeBed, base, utc };
