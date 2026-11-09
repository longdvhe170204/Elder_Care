import { describe, expect, it } from 'vitest'
import { doseRoundView } from './doseRoundView'

// 006 FR-026, FR-018 (api-006 Dose + api-001 Profile + api-003 ResidentLocation): lớp chuyển đổi thẻ Phát thuốc.

const DOSE = {
  doseKey: '7-202611080800',
  residentId: 9,
  prescriptionId: 7,
  medicationName: 'Metformin 500mg',
  dosage: 1.5,
  dosageUnit: 'viên',
  route: 'Uống',
  scheduledTime: '2026-11-08T01:00:00Z',
  windowFrom: '2026-11-08T00:30:00Z',
  windowTo: '2026-11-08T01:30:00Z',
  status: 'DUE',
  currentStatus: 'DUE',
  floorShared: true,
  requiresOnline: true,
  medicationSource: 'FAMILY_SENT',
  usableBatches: [{ batchId: 5, expiryDate: '2026-12-31' }],
}
const PROFILE = {
  id: 9,
  profileCode: 'NCT-0009',
  personalInfo: { fullName: 'Trần Thị Mai' },
  activeHealthItems: [
    { type: 'ALLERGY', allergen: { id: 1, name: 'Penicillin' }, status: 'ACTIVE' },
    { type: 'CHRONIC_CONDITION', freeText: 'Tiểu đường', status: 'ACTIVE' },
    { type: 'ALLERGY', freeText: 'Hải sản', status: 'ACTIVE' },
  ],
}
const LOCATION = { location: { label: 'Khu A – Tầng 2 – P201 – G1', room: 'P201' } }

describe('doseRoundView', () => {
  it('FR_026_lieuDoiNhan_thanhTheTheoGioVietNam_kemTenThuocVaLieu', () => {
    const { doses } = doseRoundView({ doses: [DOSE], profiles: { 9: PROFILE }, locations: { 9: LOCATION } })
    expect(doses).toHaveLength(1)
    expect(doses[0]).toMatchObject({
      id: '7-202611080800',
      time: '08:00',
      residentCode: '9',
      drug: 'Metformin 500mg',
      dose: '1,5 viên',
      route: 'Uống',
      source: 'Gia đình gửi',
      status: 'Đến giờ',
      controlled: true,
      sharedFloor: true,
    })
    expect(doses[0].lots).toEqual([{ code: 'Lô 5', batchId: 5, expiry: '31/12/2026', expired: false }])
  })

  it('FR_026_nguoiCaoTuoi_layHoTenVaDiUngTuHoSo_phongTuViTri', () => {
    const { residents } = doseRoundView({ doses: [DOSE], profiles: { 9: PROFILE }, locations: { 9: LOCATION } })
    expect(residents).toEqual([
      expect.objectContaining({
        code: '9',
        name: 'Trần Thị Mai',
        initials: 'TM',
        room: 'P201',
        allergies: ['Penicillin', 'Hải sản'],
        photo: null,
      }),
    ])
  })

  it('FR_026_thieuHoSoHoacViTri_hienChuaCoThongTinThayVìBịa', () => {
    const { residents } = doseRoundView({ doses: [DOSE], profiles: {}, locations: {} })
    expect(residents[0]).toMatchObject({ name: 'Người cao tuổi chưa tải được', room: '—', allergies: [] })
  })

  it('FR_021_trangThaiLieuDungNhanTiengViet', () => {
    const statuses = ['UPCOMING', 'LATE', 'MISSED', 'ADMINISTERED', 'REFUSED', 'NOT_ADMINISTERED', 'SUSPENDED', 'LEAVE_MEDICATION', 'CANCELLED']
    const { doses } = doseRoundView({
      doses: statuses.map((status, i) => ({ ...DOSE, doseKey: `k${i}`, status, currentStatus: status })),
      profiles: {},
      locations: {},
    })
    expect(doses.map((d) => d.status)).toEqual([
      'Chưa đến giờ',
      'Trễ',
      'Bỏ lỡ',
      'Đã dùng',
      'Từ chối',
      'Không thực hiện',
      'Tạm dừng',
      'Mang theo',
      'Đã hủy',
    ])
  })

  it('FR_030_dungTrangThaiHienHanhSauDinhChinh', () => {
    const { doses } = doseRoundView({
      doses: [{ ...DOSE, status: 'MISSED', currentStatus: 'ADMINISTERED' }],
      profiles: {},
      locations: {},
    })
    expect(doses[0].status).toBe('Đã dùng')
  })
})
