import { getMeShifts } from '@/api/api008'
import { getResidentsById } from '@/api/api001'
import { getResidentsByResidentIdLocation } from '@/api/api003'
import {
  getMedicationDosesRounds,
  postMedicationDosesByDoseKeyAdminister,
  postMedicationDosesByDoseKeyRefuse,
  postMedicationDosesByDoseKeySkip,
} from '@/api/api006'
import { doseRoundView, hhmm } from './doseRoundView'

// Đọc vòng phát thuốc và ghi kết quả liều qua client sinh từ hợp đồng (api-006 dose, api-001 Profile, api-003 vị trí,
// api-008 /me/shifts). Hồ sơ hoặc vị trí không đọc được thì thẻ ghi "chưa có", không chặn vòng thuốc.

const TIME_ZONE = 'Asia/Ho_Chi_Minh'
const TODAY = new Intl.DateTimeFormat('en-CA', { timeZone: TIME_ZONE })

export const todayVN = (now = new Date()) => TODAY.format(now)

async function currentShift(now) {
  try {
    const day = todayVN(now)
    const { data } = await getMeShifts({ query: { from: day, to: day } })
    const shift = (data ?? []).find((s) => new Date(s.startsAt) <= now && now <= new Date(s.endsAt))
    return shift ? { start: hhmm(shift.startsAt), end: hhmm(shift.endsAt) } : null
  } catch {
    return null
  }
}

export async function loadDoseRound(now = new Date()) {
  const { data: doses } = await getMedicationDosesRounds({})
  const ids = [...new Set(doses.map((d) => d.residentId))]
  const profiles = {}
  const locations = {}
  await Promise.all(
    ids.map(async (id) => {
      const [profile, location] = await Promise.allSettled([
        getResidentsById({ path: { id } }),
        getResidentsByResidentIdLocation({ path: { residentId: id } }),
      ])
      if (profile.status === 'fulfilled') profiles[id] = profile.value.data
      if (location.status === 'fulfilled') locations[id] = location.value.data
    }),
  )
  return { ...doseRoundView({ doses, profiles, locations, now }), shift: await currentShift(now) }
}

// results: [{ dose, result, time, reaction, reason, key }] từ khung xác nhận; mỗi liều một lệnh, tuần tự để dừng ở lỗi
// đầu tiên (liều đã lưu giữ nguyên, liều chưa lưu còn trong khung).
export async function recordDoses(results, now = new Date()) {
  const day = todayVN(now)
  for (const entry of results) {
    const path = { doseKey: entry.dose.id }
    const headers = { 'Idempotency-Key': entry.key }
    if (entry.result === 'Đã dùng') {
      await postMedicationDosesByDoseKeyAdminister({
        path,
        headers,
        body: {
          administeredAt: `${day}T${entry.time}:00+07:00`,
          basis: entry.dose.status === 'Mang theo' ? 'LEAVE_MEDICATION' : 'REGULAR',
          ...(entry.reaction?.trim() && { adverseReaction: entry.reaction.trim() }),
        },
      })
    } else if (entry.result === 'Từ chối') {
      await postMedicationDosesByDoseKeyRefuse({ path, headers, body: { reason: entry.reason.trim() } })
    } else {
      await postMedicationDosesByDoseKeySkip({ path, headers, body: { reason: entry.reason.trim() } })
    }
  }
}
