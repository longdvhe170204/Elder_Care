import { getResidents } from '@/api/api001'
import {
  getFacilityContact,
  getFacilityLicenses,
  getResidentsByResidentIdExaminations,
  postFacilityLicenses,
  postResidentsByResidentIdExaminations,
  putFacilityContact,
} from '@/api/api007'

// Giấy phép cơ sở, số liên hệ của viện, bản ghi khám và điều trị qua client sinh từ hợp đồng (api-007; api-001 danh
// sách người cao tuổi). Không đọc được một phần thì phần đó báo lỗi riêng, không chặn phần khác.

const key = () => ({ 'Idempotency-Key': crypto.randomUUID() })

export async function loadFacility() {
  const [licenses, contact, residents] = await Promise.allSettled([
    getFacilityLicenses({}),
    getFacilityContact({}),
    getResidents({}),
  ])
  return {
    licenses: licenses.status === 'fulfilled' ? licenses.value.data : null,
    licensesError: licenses.status === 'rejected' ? licenses.reason : null,
    contact: contact.status === 'fulfilled' ? contact.value.data : null,
    residents:
      residents.status === 'fulfilled'
        ? (residents.value.data.items ?? []).map((r) => ({
            id: r.id,
            code: r.profileCode,
            name: r.personalInfo?.fullName ?? 'Người cao tuổi',
          }))
        : [],
  }
}

export const saveLicense = (body) => postFacilityLicenses({ headers: key(), body })

export const changeContact = (version, body) =>
  putFacilityContact({ headers: { ...key(), 'If-Match': `"${version}"` }, body })

export const loadExaminations = async (residentId) =>
  (await getResidentsByResidentIdExaminations({ path: { residentId } })).data

export const recordExamination = (residentId, body) =>
  postResidentsByResidentIdExaminations({ path: { residentId }, headers: key(), body })
