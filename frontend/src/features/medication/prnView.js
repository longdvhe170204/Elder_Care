import { hhmm, number, residentView } from './doseRoundView'

// Chuyển dữ liệu hợp đồng (api-006 Prescription, PrnHistory, Medication; api-001 Profile) sang thẻ thuốc khi cần.
// Chặn theo khoảng cách, số lần trong 24 giờ do backend tính (canAdministerNow, earliestNextAllowedTime); màn không
// tự tính lại (FR-025). Thiếu trường thì hiện "chưa có", không bịa.

const DAY_MS = 24 * 60 * 60 * 1000

export function medicationLabel(medication, medicationId) {
  if (!medication) return `Thuốc #${medicationId}`
  return medication.concentration ? `${medication.name} ${medication.concentration}` : medication.name
}

export function prnOrderView({ prescription, history, medication, profile, now = new Date() }) {
  const uses = (history?.administrations ?? []).map((a) => a.administeredAt).sort()
  return {
    id: prescription.id,
    residentId: prescription.residentId,
    resident: residentView(prescription.residentId, profile, undefined),
    drug: medicationLabel(medication, prescription.medicationId),
    dose: [number(prescription.dosage), medication?.usageUnit].filter(Boolean).join(' '),
    dosage: prescription.dosage,
    route: prescription.route,
    minHours: prescription.minIntervalMinutes / 60,
    maxPer24h: prescription.maxDosesPer24h,
    source: prescription.medicationSource === 'FAMILY_SENT' ? 'Gia đình gửi' : 'Viện',
    lastUse: uses.length ? hhmm(uses.at(-1)) : null,
    usesIn24h: uses.filter((at) => now - new Date(at) < DAY_MS && new Date(at) <= now).length,
    canAdministerNow: history?.canAdministerNow !== false,
    earliest: history?.earliestNextAllowedTime ? hhmm(history.earliestNextAllowedTime) : null,
  }
}
