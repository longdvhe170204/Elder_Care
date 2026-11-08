import { getResidentsById } from '@/api/api001'
import {
  getMedications,
  getResidentsByResidentIdPrescriptions,
  postPrescriptionsByIdChangeDosage,
  postPrescriptionsByIdDiscontinue,
  postPrescriptionsByIdResume,
  postPrescriptionsByIdSuspend,
  postResidentsByResidentIdPrescriptions,
  postResidentsByResidentIdPrescriptionsCheck,
} from '@/api/api006'
import { residentView } from './doseRoundView'
import { prescriptionView } from './prescriptionView'

// Đọc và ghi đơn thuốc qua client sinh từ hợp đồng (api-006 prescription, medications; api-001 Profile). Danh mục
// hoặc hồ sơ không đọc được thì màn ghi "chưa có", không chặn danh sách đơn; đơn lỗi thì ném lỗi cho màn.

export async function loadPrescriptionBoard(residentId) {
  const [prescriptions, catalog, profile] = await Promise.allSettled([
    getResidentsByResidentIdPrescriptions({ path: { residentId } }),
    getMedications({}),
    getResidentsById({ path: { id: residentId } }),
  ])
  if (prescriptions.status === 'rejected') throw prescriptions.reason
  const medications = catalog.status === 'fulfilled' ? catalog.value.data : []
  return {
    medications,
    resident: residentView(residentId, profile.status === 'fulfilled' ? profile.value.data : undefined, undefined),
    prescriptions: prescriptions.value.data.map((prescription) =>
      prescriptionView({ prescription, medication: medications.find((m) => m.id === prescription.medicationId) }),
    ),
  }
}

export async function checkPrescription(residentId, body) {
  const { data } = await postResidentsByResidentIdPrescriptionsCheck({ path: { residentId }, body })
  return { issues: data.issues ?? [], reviewItems: data.reviewItems ?? [] }
}

export const createPrescription = (residentId, body, key) =>
  postResidentsByResidentIdPrescriptions({ path: { residentId }, headers: { 'Idempotency-Key': key }, body })

export const changeDosage = (id, body, key) =>
  postPrescriptionsByIdChangeDosage({ path: { id }, headers: { 'Idempotency-Key': key }, body })

const COMMANDS = {
  SUSPEND: postPrescriptionsByIdSuspend,
  RESUME: postPrescriptionsByIdResume,
  DISCONTINUE: postPrescriptionsByIdDiscontinue,
}

// command: SUSPEND | RESUME | DISCONTINUE; thân chỉ có lý do (Bác sĩ không cần căn cứ bên ngoài)
export const runPrescriptionCommand = (command, id, reason, key) =>
  COMMANDS[command]({ path: { id }, headers: { 'Idempotency-Key': key }, body: { reason: reason.trim() } })

// Cùng nội dung gửi lại (sau lỗi mạng) dùng lại khóa; nội dung đổi thì khóa mới, tránh trùng khóa khác nội dung
export function idempotencyKeys() {
  let last = null
  return (body) => {
    const text = JSON.stringify(body)
    if (last?.text !== text) last = { text, key: crypto.randomUUID() }
    return last.key
  }
}
