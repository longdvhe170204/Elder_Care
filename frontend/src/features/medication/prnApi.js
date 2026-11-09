import { getResidentsById } from '@/api/api001'
import {
  getMedications,
  getPrescriptionsByIdPrnAdministrations,
  getResidentsByResidentIdPrescriptions,
  postPrescriptionsByIdPrnAdministrations,
} from '@/api/api006'
import { todayVN } from './doseRound'
import { prnOrderView } from './prnView'

// Đọc đơn thuốc khi cần của một người cao tuổi và ghi lần dùng qua client sinh từ hợp đồng (api-006 prescription,
// prn-administrations, medications; api-001 Profile). Danh mục hoặc hồ sơ không đọc được thì thẻ ghi "chưa có",
// không chặn màn; đơn hoặc lịch sử lỗi thì ném lỗi cho màn hiện ProblemAlert.

export async function loadPrnOrders(residentId, now = new Date()) {
  const [prescriptions, catalog, profile] = await Promise.allSettled([
    getResidentsByResidentIdPrescriptions({ path: { residentId }, query: { status: ['ACTIVE'] } }),
    getMedications({}),
    getResidentsById({ path: { id: residentId } }),
  ])
  if (prescriptions.status === 'rejected') throw prescriptions.reason
  const orders = prescriptions.value.data.filter((p) => p.prescriptionType === 'PRN')
  const histories = await Promise.all(orders.map((p) => getPrescriptionsByIdPrnAdministrations({ path: { id: p.id } })))
  const medications = catalog.status === 'fulfilled' ? catalog.value.data : []
  return orders.map((prescription, i) =>
    prnOrderView({
      prescription,
      history: histories[i].data,
      medication: medications.find((m) => m.id === prescription.medicationId),
      profile: profile.status === 'fulfilled' ? profile.value.data : undefined,
      now,
    }),
  )
}

// entry: { time: 'HH:mm', reason, reaction, key }; liều ghi bằng liều của đơn (không vượt liều đơn)
export async function recordPrn(order, entry, now = new Date()) {
  await postPrescriptionsByIdPrnAdministrations({
    path: { id: order.id },
    headers: { 'Idempotency-Key': entry.key },
    body: {
      administeredAt: `${todayVN(now)}T${entry.time}:00+07:00`,
      dosage: order.dosage,
      reason: entry.reason.trim(),
      ...(entry.reaction?.trim() && { adverseReaction: entry.reaction.trim() }),
    },
  })
}
