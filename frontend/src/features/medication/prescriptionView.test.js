import { describe, expect, it } from 'vitest'
import {
  changeDosageBody,
  issueText,
  newPrescriptionBody,
  parseSlots,
  prescriptionView,
} from './prescriptionView'

// 006 FR-004 → FR-008, FR-016a: đơn thuốc chỉ hiện trường có trong api-006 Prescription, Medication; thân lệnh kê đơn,
// đổi liều đúng NewPrescription (không dosageUnit: đơn vị lấy từ danh mục thuốc).

const NOW = new Date('2026-11-08T03:00:00Z') // 10:00 giờ Việt Nam
const medication = { id: 1, name: 'Amlodipin', concentration: '5mg', usageUnit: 'viên' }
const prescription = {
  id: 7,
  residentId: 9,
  medicationId: 1,
  dosage: 1,
  route: 'Uống',
  prescriptionType: 'PERIODIC',
  frequency: { type: 'TIMED_SLOTS', timedSlots: ['08:00', '20:00'] },
  startedAt: '2026-11-01T01:00:00Z',
  medicationSource: 'FACILITY',
  prescriber: { type: 'EXTERNAL', facilityName: 'BV Lão khoa', prescriberName: 'BS. An', prescribedAt: '2026-10-30' },
  instructionNote: 'Sau ăn sáng',
  status: 'ACTIVE',
  allowedCommands: ['SUSPEND', 'DISCONTINUE', 'CHANGE_DOSE'],
}

describe('prescriptionView', () => {
  it('FR_016a_hienTrangThaiLieuTanSuatNguoiKe', () => {
    const v = prescriptionView({ prescription, medication })
    expect(v).toMatchObject({
      id: 7,
      drug: 'Amlodipin 5mg',
      dose: '1 viên',
      status: 'Hiệu lực',
      frequencyText: 'Các mốc giờ: 08:00, 20:00',
      prescriberText: 'BS. An, BV Lão khoa',
      allowedCommands: ['SUSPEND', 'DISCONTINUE', 'CHANGE_DOSE'],
    })
  })

  it('FR_009_thieuDanhMuc_khongBia_vaGiuTrangThaiTamDung', () => {
    const v = prescriptionView({ prescription: { ...prescription, status: 'SUSPENDED', prescriber: { type: 'INTERNAL' } } })
    expect(v.drug).toBe('Thuốc #1')
    expect(v.dose).toBe('1')
    expect(v.status).toBe('Tạm dừng')
    expect(v.prescriberText).toBe('Bác sĩ của viện')
  })

  it('FR_005_tanSuatMoiNGioVaNgayTrongTuan', () => {
    const everyN = { type: 'EVERY_N_HOURS', everyNHours: 6, firstSlot: '2026-11-08T01:00:00Z' }
    const days = { type: 'DAYS_OF_WEEK', daysOfWeek: [1, 3], timedSlots: ['09:00'] }
    expect(prescriptionView({ prescription: { ...prescription, frequency: everyN }, medication }).frequencyText).toBe('Mỗi 6 giờ, từ 08:00')
    expect(prescriptionView({ prescription: { ...prescription, frequency: days }, medication }).frequencyText).toBe('Theo ngày trong tuần (mã ngày 1, 3): 09:00')
    expect(prescriptionView({ prescription: { ...prescription, prescriptionType: 'PRN', frequency: undefined, minIntervalMinutes: 240, maxDosesPer24h: 3 }, medication }).frequencyText).toBe('Khi cần: cách ít nhất 4 giờ, tối đa 3 lần trong 24 giờ')
  })
})

describe('parseSlots', () => {
  it('FR_005_tachMocGio_saiDinhDangThiNull', () => {
    expect(parseSlots('08:00, 20:00')).toEqual(['08:00', '20:00'])
    expect(parseSlots('8h')).toBeNull()
    expect(parseSlots('')).toBeNull()
  })
})

describe('issueText', () => {
  it('FR_007_dienGiaiVanDeTuDuLieuBackend', () => {
    expect(issueText({ issueType: 'DUPLICATE_ACTIVE_INGREDIENT', activeIngredient: 'Amlodipin', duplicatePrescriptionId: 3 })).toBe('Trùng hoạt chất Amlodipin với đơn #3')
    expect(issueText({ issueType: 'ALLERGY_CONFLICT', activeIngredient: 'Penicillin', severity: 'SEVERE' })).toBe('Xung đột dị ứng Penicillin, mức SEVERE')
    expect(issueText({ issueType: 'OTHER_ALLERGY_REVIEW' })).toBe('Dị ứng khác trong hồ sơ cần xem xét thủ công, hệ thống không tự kiểm')
  })
})

describe('newPrescriptionBody', () => {
  const base = { medicationId: 1, dosage: '1.5', route: 'Uống', prescriptionType: 'PERIODIC', frequencyType: 'TIMED_SLOTS', timedSlots: '08:00', medicationSource: 'FACILITY', prescriberType: 'INTERNAL', instructionNote: ' ', confirmations: {} }

  it('FR_004_donDinhKyNoiBo', () => {
    const body = newPrescriptionBody(base, NOW)
    expect(body).toEqual({
      medicationId: 1,
      dosage: 1.5,
      route: 'Uống',
      prescriptionType: 'PERIODIC',
      frequency: { type: 'TIMED_SLOTS', timedSlots: ['08:00'] },
      startedAt: '2026-11-08T10:00:00+07:00',
      medicationSource: 'FACILITY',
      prescriber: { type: 'INTERNAL' },
    })
  })

  it('FR_006_donBenNgoai_vaXacNhanVanDe', () => {
    const body = newPrescriptionBody(
      { ...base, prescriberType: 'EXTERNAL', facilityName: 'BV X', prescriberName: 'BS. Y', prescribedAt: '2026-11-07', instructionNote: 'Sau ăn', confirmations: { C1: ' đã cân nhắc ', C2: '' } },
      NOW,
    )
    expect(body.prescriber).toEqual({ type: 'EXTERNAL', facilityName: 'BV X', prescriberName: 'BS. Y', prescribedAt: '2026-11-07' })
    expect(body.instructionNote).toBe('Sau ăn')
    expect(body.issueConfirmations).toEqual([{ issueCode: 'C1', reason: 'đã cân nhắc' }])
  })

  it('FR_024_donKhiCan_gioiHanVaMoiNGio', () => {
    const prn = newPrescriptionBody({ ...base, prescriptionType: 'PRN', minIntervalMinutes: '240', maxDosesPer24h: '3' }, NOW)
    expect(prn).toMatchObject({ prescriptionType: 'PRN', minIntervalMinutes: 240, maxDosesPer24h: 3 })
    expect(prn.frequency).toBeUndefined()
    const every = newPrescriptionBody({ ...base, frequencyType: 'EVERY_N_HOURS', everyNHours: '8', firstSlot: '06:00' }, NOW)
    expect(every.frequency).toEqual({ type: 'EVERY_N_HOURS', everyNHours: 8, firstSlot: '2026-11-08T06:00:00+07:00' })
  })
})

describe('changeDosageBody', () => {
  it('FR_016_doiLieu_giuNguyenThuocNguonNguoiKe', () => {
    const body = changeDosageBody(prescription, { dosage: '2', route: 'Uống', timedSlots: '08:00', instructionNote: 'Sau ăn tối', reason: ' tăng huyết áp ', effectiveFrom: '2026-11-08T10:30', confirmations: {} })
    expect(body.reason).toBe('tăng huyết áp')
    expect(body.effectiveFrom).toBe('2026-11-08T10:30:00+07:00')
    expect(body.newPrescription).toMatchObject({
      medicationId: 1,
      dosage: 2,
      prescriptionType: 'PERIODIC',
      frequency: { type: 'TIMED_SLOTS', timedSlots: ['08:00'] },
      startedAt: '2026-11-08T10:30:00+07:00',
      medicationSource: 'FACILITY',
      prescriber: prescription.prescriber,
      instructionNote: 'Sau ăn tối',
    })
  })
})
