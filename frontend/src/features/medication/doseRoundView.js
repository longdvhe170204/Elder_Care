// Chuyển dữ liệu hợp đồng (api-006 Dose, api-001 Profile, api-003 ResidentLocation) sang dữ liệu cho màn Phát thuốc.
// Chỉ đọc trường có trong hợp đồng (DESIGN.md mục 13 điều 6); thiếu thì hiện "chưa có", không bịa.

const STATUS = {
  UPCOMING: 'Chưa đến giờ',
  DUE: 'Đến giờ',
  LATE: 'Trễ',
  MISSED: 'Bỏ lỡ',
  ADMINISTERED: 'Đã dùng',
  REFUSED: 'Từ chối',
  NOT_ADMINISTERED: 'Không thực hiện',
  SUSPENDED: 'Tạm dừng',
  LEAVE_MEDICATION: 'Mang theo',
  CANCELLED: 'Đã hủy',
}
const SOURCE = { FACILITY: 'Viện', FAMILY_SENT: 'Gia đình gửi' }

const HHMM = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Ho_Chi_Minh',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})
export const hhmm = (iso) => HHMM.format(new Date(iso))
const ddmmyyyy = (date) => date.split('-').reverse().join('/')
export const number = (n) => String(n).replace('.', ',')

const initialsOf = (name) =>
  name
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((w) => w[0].toUpperCase())
    .join('')

export function residentView(id, profile, location) {
  const name = profile?.personalInfo?.fullName
  return {
    id,
    code: String(id),
    name: name ?? 'Người cao tuổi chưa tải được',
    initials: name ? initialsOf(name) : '?',
    room: location?.location?.room ?? '—',
    allergies: (profile?.activeHealthItems ?? [])
      .filter((item) => item.type === 'ALLERGY' && item.status !== 'EXCLUDED')
      .map((item) => item.allergen?.name ?? item.freeText)
      .filter(Boolean),
    photo: null,
  }
}

export function doseView(dose, now = new Date()) {
  return {
    id: dose.doseKey,
    time: hhmm(dose.scheduledTime),
    from: hhmm(dose.windowFrom),
    to: hhmm(dose.windowTo),
    residentCode: String(dose.residentId),
    drug: dose.medicationName,
    dose: `${number(dose.dosage)} ${dose.dosageUnit}`,
    route: dose.route,
    source: SOURCE[dose.medicationSource],
    status: STATUS[dose.currentStatus ?? dose.status],
    controlled: dose.requiresOnline,
    sharedFloor: dose.floorShared,
    lots: (dose.usableBatches ?? []).map((batch) => ({
      code: `Lô ${batch.batchId}`,
      batchId: batch.batchId,
      expiry: ddmmyyyy(batch.expiryDate),
      expired: new Date(`${batch.expiryDate}T23:59:59+07:00`) < now,
    })),
    handoverId: dose.handoverId,
    awaitingRecordAfterReturn: dose.awaitingRecordAfterReturn,
  }
}

// doses: mảng Dose của GET /medication-doses/rounds; profiles, locations: theo mã người cao tuổi
export function doseRoundView({ doses, profiles, locations, now }) {
  const ids = [...new Set(doses.map((d) => d.residentId))]
  return {
    doses: doses.map((d) => doseView(d, now)),
    residents: ids.map((id) => residentView(id, profiles[id], locations[id])),
  }
}
