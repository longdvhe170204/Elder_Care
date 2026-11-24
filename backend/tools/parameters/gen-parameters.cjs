// Sinh V000_04__parameter_initial_load.sql và ParameterCode.java từ Phụ lục 25 (000 T030, T031).
// Chạy từ gốc kho: node backend/tools/parameters/gen-parameters.cjs docs/business/nghiep-vu.md backend
// Migration đã áp thì không sửa lại: mã mới hoặc giá trị đổi đi bằng migration mới.
const fs = require('fs');
const path = require('path');
const [, , source, backend] = process.argv;

const rows = fs
  .readFileSync(source, 'utf8')
  .split(/\r?\n/)
  .filter((l) => /^\| CFG-M\d{2}-\d{2}/.test(l))
  .map((l) => l.split('|').slice(1, -1).map((c) => c.trim().replace(/\\/g, '')));
if (rows.length !== 119) throw new Error('Phụ lục 25 có ' + rows.length + ' mã, không phải 119');

const D = (value, unit) => ({ value, unit });
const UNIT = { phút: 'MINUTES', giờ: 'HOURS', ngày: 'DAYS', tháng: 'MONTHS', năm: 'YEARS' };

// Giá trị dạng bảng hoặc đặc biệt, rà tay theo cột "Mặc định" (data-model mục 2).
const OVERRIDES = {
  'CFG-M02-05': {
    kind: 'TABLE',
    value: {
      rows: [
        { residencyType: 'LONG_TERM', absenceType: 'HOME_OR_OUTING', fromDay: 1, toDay: null, feeRatePercent: 100, holdBed: 'YES' },
        { residencyType: 'LONG_TERM', absenceType: 'HOSPITAL', fromDay: 1, toDay: 7, feeRatePercent: 100, holdBed: 'YES' },
        { residencyType: 'LONG_TERM', absenceType: 'HOSPITAL', fromDay: 8, toDay: 30, feeRatePercent: 70, holdBed: 'YES' },
        { residencyType: 'LONG_TERM', absenceType: 'HOSPITAL', fromDay: 31, toDay: null, feeRatePercent: 'MANAGER_DECIDES', holdBed: 'MANAGER_DECIDES' },
        { residencyType: 'SHORT_TERM', absenceType: 'ANY', fromDay: 1, toDay: null, feeRatePercent: 'BY_CONTRACT', holdBed: 'BY_CONTRACT' },
        { residencyType: 'DAY_CARE', absenceType: 'NOTIFIED_IN_ADVANCE', fromDay: null, toDay: null, feeRatePercent: 0, holdBed: null },
        { residencyType: 'DAY_CARE', absenceType: 'UNANNOUNCED', fromDay: null, toDay: null, feeRatePercent: 50, holdBed: null },
      ],
    },
  },
  'CFG-M02-08': { kind: 'TABLE', value: { waitingPeriod: D(7, 'DAYS'), pointsPerWaitingPeriod: 1, careLevelPoints: null, specialSituationPoints: null } },
  'CFG-M03-01': { kind: 'INTEGER', value: null, unit: 'PERSONS' },
  'CFG-M04-03': { kind: 'TABLE', value: { checkTime: '16:00', minPercentOfTarget: 60 } },
  'CFG-M05-02': { kind: 'TABLE', value: { count: 3, window: D(24, 'HOURS') } },
  'CFG-M05-03': { kind: 'TABLE', value: { percent: 5, window: D(30, 'DAYS') } },
  'CFG-M05-06': { kind: 'TABLE', value: { interval: D(4, 'HOURS'), duration: D(72, 'HOURS') } },
  'CFG-M05-08': { kind: 'TABLE', value: { measurement: 'TEMPERATURE', timesPerDay: 2, duration: D(7, 'DAYS') } },
  'CFG-M06-03': { kind: 'TABLE', value: {} },
  'CFG-M07-06': { kind: 'DURATION', value: D(5, 'DAYS') },
  'CFG-M07-07': { kind: 'DURATION', value: D(2, 'DAYS') },
  'CFG-M08-04': { kind: 'TABLE', value: { mealTimes: null, lateDeliveryThreshold: D(30, 'MINUTES') } },
  'CFG-M08-09': { kind: 'DURATION', value: D(1, 'MONTHS') },
  'CFG-M09-01': { kind: 'TABLE', value: null },
  'CFG-M09-07': { kind: 'TABLE', value: { rows: [{ shiftTemplate: 'ALL', scope: 'FLOOR', role: 'NURSING_STAFF', minCount: 1 }] } },
  'CFG-M09-12': { kind: 'TABLE', value: null },
  'CFG-M10-01': { kind: 'TABLE', value: { high: D(24, 'HOURS'), normal: D(72, 'HOURS') } },
  'CFG-M10-03': { kind: 'TABLE', value: { reminderDay: 'MONDAY', approvalDeadline: D(48, 'HOURS') } },
  'CFG-M10-04': { kind: 'TABLE', value: { slots: [{ from: '09:00', to: '11:00' }, { from: '15:00', to: '17:00' }], capacityPerSlot: 20 } },
  'CFG-M10-05': { kind: 'TABLE', value: { min: D(2, 'HOURS'), max: D(14, 'DAYS') } },
  'CFG-M10-11': { kind: 'TABLE', value: { remind: D(48, 'HOURS'), notifyManager: D(96, 'HOURS') } },
  'CFG-M11-01': { kind: 'TABLE', value: { monthsAfterPeriodEnd: 1, dayOfMonth: 'LAST' } },
  'CFG-M11-02': { kind: 'MONEY', value: 500000, unit: 'VND' },
  'CFG-M11-10': { kind: 'TABLE', value: null },
  'CFG-M12-01': { kind: 'TABLE', value: { categories: ['Điện thoại', 'Trang sức', 'Tiền mặt'] } },
  'CFG-M12-04': { kind: 'TABLE', value: { cycle: D(1, 'MONTHS'), completionDeadline: D(3, 'DAYS') } },
  'CFG-M12-06': { kind: 'TABLE', value: { attemptsPerRepresentative: 2, channels: 2 } },
  'CFG-M12-07': { kind: 'TABLE', value: { attempts: 2, minGapParameter: 'CFG-M12-02' } },
  'CFG-M13-02': { kind: 'TABLE', value: { from: '21:00', to: '07:00' } },
  'CFG-M13-05': { kind: 'INTEGER', value: 1, unit: 'TIMES' },
  'CFG-M13-06': { kind: 'TABLE', value: { from: '07:30', to: '17:00', days: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'] } },
  'CFG-M15-01': { kind: 'TABLE', value: { maxFailedAttempts: 5, lockDuration: D(15, 'MINUTES') } },
  'CFG-M15-09': { kind: 'TABLE', value: { minLength: 8, requireLetter: true, requireDigit: true } },
  'CFG-M15-10': { kind: 'TABLE', value: { WEB: D(30, 'MINUTES'), STAFF_APP: D(12, 'HOURS'), FAMILY_APP: D(30, 'DAYS'), RESIDENT_APP: { noLimit: true } } },
};
const EXTRA_CONSTRAINTS = { 'CFG-M15-06': { greaterThan: 'CFG-M15-05' } };

function classify(code, def) {
  if (OVERRIDES[code]) return OVERRIDES[code];
  let m;
  if ((m = def.match(/^±?(\d+) (phút|giờ|ngày|tháng|năm)(?:\s|$)/u))) return { kind: 'DURATION', value: D(+m[1], UNIT[m[2]]) };
  if ((m = def.match(/^(\d+)%/))) return { kind: 'PERCENTAGE', value: +m[1], unit: 'PERCENT' };
  if ((m = def.match(/^(\d+) lần$/))) return { kind: 'INTEGER', value: +m[1], unit: 'TIMES' };
  if ((m = def.match(/^(\d+)$/))) return { kind: 'INTEGER', value: +m[1] };
  throw new Error('Chưa phân loại: ' + code + ' = ' + def);
}

const TECH = { DURATION: { min: 1 }, INTEGER: { min: 0 }, PERCENTAGE: { min: 0, max: 100 }, MONEY: { min: 0 } };
const q = (s) => (s === null || s === undefined ? 'NULL' : "'" + String(s).replace(/'/g, "''") + "'");
const j = (v) => q(JSON.stringify(v === undefined ? null : v));

const params = rows.map(([code, description, def, usedIn]) => {
  const c = classify(code, def);
  const unit = c.kind === 'DURATION' ? c.value.unit : c.unit || null;
  const constraints = c.kind === 'TABLE' ? null : { ...TECH[c.kind], ...(EXTRA_CONSTRAINTS[code] || {}) };
  return { code, description, def, usedIn, kind: c.kind, unit, value: c.value, constraints };
});

const kinds = {};
params.forEach((p) => (kinds[p.kind] = (kinds[p.kind] || 0) + 1));
console.log('Phân loại:', kinds, 'chưa đặt:', params.filter((p) => p.value === null).map((p) => p.code).join(', '));

let sql = `-- 000 T031: nạp ${params.length} tham số của Phụ lục 25 (docs/business/nghiep-vu.md), sinh bằng script và rà tay.
-- Mỗi mã một dòng parameter và một dòng parameter_history (người thực hiện: Bộ lập lịch, actor_id NULL).
-- Giá trị null: "Theo cơ sở" hoặc "Viện nhập khi cài đặt"; Quản lý viện đặt sau khi cài (đọc khi chưa đặt thì lỗi).
-- Dạng TABLE: feature sở hữu có thể chốt lại cấu trúc bằng migration của mình (plan 000, Ghi chú triển khai).
-- constraints chỉ gồm ràng buộc kỹ thuật và ràng buộc có nguồn (CFG-M15-06 > CFG-M15-05; FR-015, Q-315).
`;
for (const p of params) {
  sql += `
-- ${p.code}: ${p.def}
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES (${q(p.code)}, ${q(p.description)}, ${q(p.kind)}, ${q(p.unit)}, ${p.constraints ? j(p.constraints) : 'NULL'}, ${j(p.value)}, ${j(p.value)}, ${q(p.usedIn)}, 0);`;
}
sql += `

INSERT INTO parameter_history (code, value, effective_from, actor_id, reason, is_default_restore)
SELECT code, current_value, UTC_TIMESTAMP(6), NULL, NULL, FALSE FROM parameter;
`;
fs.writeFileSync(path.join(backend, 'src/main/resources/db/migration/V000_04__parameter_initial_load.sql'), sql);

const constants = params.map((p) => `    /** ${p.description.replace(/\*\//g, '* /')} (mặc định: ${p.def}) */\n    ${p.code.replace(/-/g, '_')}`);
const java = `package com.cambae.backend.m15.parameter.api;

/**
 * Mã tham số cấu hình: đủ ${params.length} mã của Phụ lục 25 (docs/business/nghiep-vu.md), sinh từ phụ lục (000 T030).
 * Không có lệnh tạo hay xóa mã (FR-011); thêm mã khi tài liệu nghiệp vụ bổ sung.
 */
public enum ParameterCode {
${constants.join(',\n')};

    /** Mã dạng {@code CFG-M15-05}. */
    public String code() {
        return name().replace('_', '-');
    }

    public static ParameterCode of(String code) {
        return valueOf(code.replace('-', '_'));
    }
}
`;
const enumPath = path.join(backend, 'src/main/java/com/cambae/backend/m15/parameter/api/ParameterCode.java');
fs.mkdirSync(path.dirname(enumPath), { recursive: true });
fs.writeFileSync(enumPath, java);
console.log('ok');
