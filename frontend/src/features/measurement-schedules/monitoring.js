import { getResidents } from '@/api/api001'
import {
  getResidentsByResidentIdMeasurementSchedules,
  getThresholds,
  getVitalSignCatalog,
  getVitalSignsMissingPersonalThreshold,
  postMeasurementSchedulesByIdStop,
  postResidentsByResidentIdMeasurementSchedules,
  postThresholds,
  postThresholdsByIdCancel,
  postThresholdsByIdStop,
} from '@/api/api007'
import { residentName } from './monitoringView'

// Đọc và ghi lịch đo, ngưỡng qua client sinh từ hợp đồng (api-007; api-001 danh sách người cao tuổi).

export async function loadOverview() {
  const [residents, catalog, missing] = await Promise.all([
    getResidents({}),
    getVitalSignCatalog({}),
    getVitalSignsMissingPersonalThreshold({}).catch(() => ({ data: [] })),
  ])
  return {
    residents: (residents.data.items ?? []).map((r) => ({ id: r.id, code: r.profileCode, name: residentName(r) })),
    catalog: catalog.data,
    missing: missing.data ?? [],
  }
}

export async function loadResidentSettings(residentId) {
  const [schedules, thresholds] = await Promise.all([
    getResidentsByResidentIdMeasurementSchedules({ path: { residentId } }),
    getThresholds({ query: { scope: 'PERSONAL', residentId } }),
  ])
  return { schedules: schedules.data, thresholds: thresholds.data }
}

const key = () => ({ 'Idempotency-Key': crypto.randomUUID() })

export const createSchedule = (residentId, body) =>
  postResidentsByResidentIdMeasurementSchedules({ path: { residentId }, headers: key(), body })

export const stopSchedule = (id, reason) =>
  postMeasurementSchedulesByIdStop({ path: { id }, headers: key(), body: { reason } })

export const createThreshold = (body) => postThresholds({ headers: key(), body })

export const cancelThreshold = (id, reason) =>
  postThresholdsByIdCancel({ path: { id }, headers: key(), body: { reason } })

export const stopThreshold = (id, reason) => postThresholdsByIdStop({ path: { id }, headers: key(), body: { reason } })
