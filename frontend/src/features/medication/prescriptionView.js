import { todayVN } from './doseRound'
import { hhmm, number } from './doseRoundView'
import { medicationLabel } from './prnView'

// Chuyển dữ liệu hợp đồng (api-006 Prescription, Medication, Issue) sang dữ liệu cho màn Đơn thuốc và dựng thân lệnh
// kê đơn, đổi liều (NewPrescription). Chỉ đọc trường có trong hợp đồng (DESIGN.md mục 13 điều 6): đơn vị liều lấy từ
// danh mục thuốc (usageUnit), người kê nội bộ không có tên trong hợp đồng nên không hiện tên.

const STATUS = {
  PENDING_EFFECTIVE: 'Chờ hiệu lực',
  ACTIVE: 'Hiệu lực',
  SUSPENDED: 'Tạm dừng',
  DISCONTINUED: 'Đã ngừng',
  EXPIRED: 'Hết hạn',
}
export const STATUS_FILTERS = ['Tất cả', ...Object.values(STATUS)]

// Mã ngày theo ISO-8601: 1 = thứ hai … 7 = chủ nhật (chủ dự án chốt 2026-11-08)
export const DAY_LABELS = { 1: 'Thứ 2', 2: 'Thứ 3', 3: 'Thứ 4', 4: 'Thứ 5', 5: 'Thứ 6', 6: 'Thứ 7', 7: 'Chủ nhật' }

const SLOT = /^([01]\d|2[0-3]):[0-5]\d$/

// "08:00, 20:00" → ['08:00', '20:00']; sai định dạng hoặc rỗng → null
export function parseSlots(text) {
  const slots = String(text ?? '')
    .split(/[,\s]+/)
    .filter(Boolean)
  return slots.length > 0 && slots.every((s) => SLOT.test(s)) ? slots : null
}

function frequencyText(p) {
  if (p.prescriptionType === 'PRN')
    return `Khi cần: cách ít nhất ${p.minIntervalMinutes / 60} giờ, tối đa ${p.maxDosesPer24h} lần trong 24 giờ`
  const f = p.frequency
  if (!f) return 'Chưa có tần suất'
  if (f.type === 'TIMED_SLOTS') return `Các mốc giờ: ${(f.timedSlots ?? []).join(', ')}`
  if (f.type === 'EVERY_N_HOURS') return `Mỗi ${f.everyNHours} giờ${f.firstSlot ? `, từ ${hhmm(f.firstSlot)}` : ''}`
  return `${(f.daysOfWeek ?? []).map((d) => DAY_LABELS[d]).join(', ')}: ${(f.timedSlots ?? []).join(', ')}`
}

function prescriberText(prescriber) {
  if (prescriber?.type !== 'EXTERNAL') return 'Bác sĩ của viện'
  return [prescriber.prescriberName, prescriber.facilityName].filter(Boolean).join(', ') || 'Bên ngoài'
}

export function prescriptionView({ prescription: p, medication }) {
  return {
    id: p.id,
    residentId: p.residentId,
    drug: medicationLabel(medication, p.medicationId),
    dose: [number(p.dosage), medication?.usageUnit].filter(Boolean).join(' '),
    route: p.route,
    type: p.prescriptionType,
    typeLabel: p.prescriptionType === 'PERIODIC' ? 'Định kỳ' : 'Khi cần (PRN)',
    status: STATUS[p.status],
    statusCode: p.status,
    frequencyText: frequencyText(p),
    prescriberText: prescriberText(p.prescriber),
    prescribedAt: p.prescriber?.prescribedAt,
    startedAt: p.startedAt,
    instructionNote: p.instructionNote,
    replacesId: p.replacesId,
    replacedById: p.replacedById,
    noMedicationAvailable: p.noMedicationAvailable,
    confirmedIssues: (p.confirmedIssues ?? []).map((i) => ({ issueCode: i.issueCode, text: issueText(i) })),
    allowedCommands: p.allowedCommands ?? [],
    raw: p,
  }
}

// Diễn giải một vấn đề kiểm tra (FR-007, FR-008) từ dữ liệu backend trả; backend không trả câu chữ nên màn dựng
export function issueText(issue) {
  if (issue.issueType === 'DUPLICATE_ACTIVE_INGREDIENT')
    return `Trùng hoạt chất ${issue.activeIngredient} với đơn #${issue.duplicatePrescriptionId}`
  if (issue.issueType === 'ALLERGY_CONFLICT')
    return `Xung đột dị ứng ${issue.activeIngredient}${issue.severity ? `, mức ${issue.severity}` : ''}`
  if (issue.issueType === 'OTHER_ALLERGY_REVIEW')
    return 'Dị ứng khác trong hồ sơ cần xem xét thủ công, hệ thống không tự kiểm'
  return issue.issueCode ?? 'Vấn đề chưa phân loại'
}

const at = (day, time) => `${day}T${time}:00+07:00`

function frequencyBody(form, day) {
  if (form.frequencyType === 'EVERY_N_HOURS')
    return { type: 'EVERY_N_HOURS', everyNHours: Number(form.everyNHours), firstSlot: at(day, form.firstSlot) }
  if (form.frequencyType === 'DAYS_OF_WEEK')
    return { type: 'DAYS_OF_WEEK', daysOfWeek: [...form.daysOfWeek].sort((a, b) => a - b), timedSlots: parseSlots(form.timedSlots) }
  return { type: 'TIMED_SLOTS', timedSlots: parseSlots(form.timedSlots) }
}

function confirmationsOf(confirmations = {}) {
  return Object.entries(confirmations)
    .filter(([, reason]) => reason?.trim())
    .map(([issueCode, reason]) => ({ issueCode, reason: reason.trim() }))
}

// form: giá trị các ô của khung Nhập đơn (chuỗi); thời điểm bắt đầu là bây giờ theo giờ Việt Nam
export function newPrescriptionBody(form, now = new Date()) {
  const day = todayVN(now)
  const periodic = form.prescriptionType === 'PERIODIC'
  const external = form.prescriberType === 'EXTERNAL'
  const confirmations = confirmationsOf(form.confirmations)
  return {
    medicationId: Number(form.medicationId),
    dosage: Number(form.dosage),
    route: form.route,
    prescriptionType: form.prescriptionType,
    ...(periodic && { frequency: frequencyBody(form, day) }),
    ...(!periodic && { minIntervalMinutes: Number(form.minIntervalMinutes), maxDosesPer24h: Number(form.maxDosesPer24h) }),
    startedAt: at(day, hhmm(now)),
    medicationSource: form.medicationSource,
    prescriber: external
      ? { type: 'EXTERNAL', facilityName: form.facilityName.trim(), prescriberName: form.prescriberName.trim(), prescribedAt: form.prescribedAt }
      : { type: 'INTERNAL' },
    ...(form.instructionNote?.trim() && { instructionNote: form.instructionNote.trim() }),
    ...(confirmations.length > 0 && { issueConfirmations: confirmations }),
  }
}

// Đổi liều = ngừng đơn cũ và tạo đơn thay thế trong một lệnh (BR-M07-07): giữ thuốc, loại đơn, nguồn, người kê của
// đơn cũ; form: { dosage, route, timedSlots, instructionNote, reason, effectiveFrom 'YYYY-MM-DDTHH:mm', confirmations }
export function changeDosageBody(prescription, form) {
  const effectiveFrom = `${form.effectiveFrom}:00+07:00`
  const confirmations = confirmationsOf(form.confirmations)
  const periodic = prescription.prescriptionType === 'PERIODIC'
  const frequency =
    periodic && prescription.frequency?.type === 'TIMED_SLOTS'
      ? { ...prescription.frequency, timedSlots: parseSlots(form.timedSlots) }
      : prescription.frequency
  const newPrescription = {
    medicationId: prescription.medicationId,
    dosage: Number(form.dosage),
    route: form.route,
    prescriptionType: prescription.prescriptionType,
    ...(frequency && { frequency }),
    ...(prescription.minIntervalMinutes && { minIntervalMinutes: prescription.minIntervalMinutes }),
    ...(prescription.maxDosesPer24h && { maxDosesPer24h: prescription.maxDosesPer24h }),
    startedAt: effectiveFrom,
    ...(prescription.endedAt && { endedAt: prescription.endedAt }),
    medicationSource: prescription.medicationSource,
    prescriber: prescription.prescriber,
    ...(form.instructionNote?.trim() && { instructionNote: form.instructionNote.trim() }),
    ...(confirmations.length > 0 && { issueConfirmations: confirmations }),
  }
  return { newPrescription, effectiveFrom, reason: form.reason.trim() }
}
