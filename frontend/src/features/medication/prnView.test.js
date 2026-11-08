import { describe, expect, it } from 'vitest'
import { prnOrderView } from './prnView'

// 006 FR-024, FR-025: thẻ thuốc khi cần chỉ hiện trường có trong hợp đồng (api-006 Prescription, PrnHistory,
// Medication; api-001 Profile); tính chặn do backend (canAdministerNow, earliestNextAllowedTime).

const NOW = new Date('2026-11-08T03:00:00Z') // 10:00 giờ Việt Nam

const prescription = { id: 7, residentId: 9, medicationId: 4, dosage: 1, route: 'Uống', minIntervalMinutes: 240, maxDosesPer24h: 3 }
const medication = { id: 4, name: 'Paracetamol', concentration: '500mg', usageUnit: 'viên' }
const profile = {
  personalInfo: { fullName: 'Trần Thị Mai' },
  activeHealthItems: [{ type: 'ALLERGY', status: 'ACTIVE', allergen: { id: 1, name: 'Penicillin' } }],
}
const administration = (at) => ({ id: 1, administeredAt: at, dosage: 1, reason: 'Sốt' })

describe('prnOrderView', () => {
  it('FR_024_hienThuocLieuDuongDungVaGioiHan', () => {
    const v = prnOrderView({ prescription, history: { administrations: [], canAdministerNow: true }, medication, profile, now: NOW })
    expect(v).toMatchObject({ id: 7, residentId: 9, drug: 'Paracetamol 500mg', dose: '1 viên', route: 'Uống', minHours: 4, maxPer24h: 3, canAdministerNow: true })
    expect(v.resident).toMatchObject({ name: 'Trần Thị Mai', allergies: ['Penicillin'] })
  })

  it('FR_025_chan_hienGioSomNhatTheoBackend_vaDemLanTrong24Gio', () => {
    const history = {
      administrations: [administration('2026-11-08T01:00:00Z'), administration('2026-11-06T01:00:00Z')],
      canAdministerNow: false,
      earliestNextAllowedTime: '2026-11-08T05:00:00Z',
    }
    const v = prnOrderView({ prescription, history, medication, profile, now: NOW })
    expect(v.canAdministerNow).toBe(false)
    expect(v.earliest).toBe('12:00')
    expect(v.lastUse).toBe('08:00')
    expect(v.usesIn24h).toBe(1)
  })

  it('FR_024_thieuDanhMucHoacHoSo_khongBia', () => {
    const v = prnOrderView({ prescription, history: { administrations: [] }, medication: undefined, profile: undefined, now: NOW })
    expect(v.drug).toBe('Thuốc #4')
    expect(v.dose).toBe('1')
    expect(v.resident.allergies).toEqual([])
    expect(v.lastUse).toBeNull()
  })
})
