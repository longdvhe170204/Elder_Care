import { describe, expect, it } from 'vitest'
import { sourceCreationBody, sourceSelectionKey, sourceSkipMessage } from './sourceSelection'

const first = { sourceType: 'OVERNIGHT_STAY', sourceId: 'stay-17', billingUnit: '2026-11-04', unit: 'đêm' }
const second = { ...first, billingUnit: '2026-11-05' }

describe('FR_005_chonDonViNguon', () => {
  it('phiLuuTruVanGuiKhoangNgayDaChonThayViSourceUnits', () => {
    expect(sourceCreationBody({ residentId: 42, sourceType: 'RESIDENCY_DAY', sources: [], selection: [],
      dateRange: { from: '2026-11-04', to: '2026-11-06' } })).toEqual({
      residentId: 42, sourceType: 'RESIDENCY_DAY', dateRange: { from: '2026-11-04', to: '2026-11-06' },
    })
    expect(sourceCreationBody({ residentId: 42, sourceType: 'RESIDENCY_DAY', sources: [], selection: [] })).toBeNull()
  })

  it('guiDungDemDaChonVaKhongGuiLuongGiaTien', () => {
    const body = sourceCreationBody({
      residentId: 42, sourceType: 'OVERNIGHT_STAY', sources: [first, second],
      selection: [sourceSelectionKey(42, second)],
    })
    expect(body).toEqual({ residentId: 42, sourceType: 'OVERNIGHT_STAY',
      sourceUnits: [{ sourceId: 'stay-17', billingUnit: '2026-11-05' }] })
  })

  it('khongDungDonViHienThiThayKhoaNguon', () => {
    expect(sourceSelectionKey(42, { ...first, billingUnit: undefined })).toBeNull()
  })

  it('khongMangLuaChonCuaNguoiTruocSangNguoiKhac', () => {
    expect(sourceCreationBody({ residentId: 43, sourceType: 'OVERNIGHT_STAY', sources: [first],
      selection: [sourceSelectionKey(42, first)] })).toBeNull()
  })

  it('khongGuiNguonDaBienMatKhoiKetQuaHienTai', () => {
    expect(sourceCreationBody({ residentId: 42, sourceType: 'OVERNIGHT_STAY', sources: [first],
      selection: [sourceSelectionKey(42, second)] })).toBeNull()
  })

  it('khongGuiLuaChonKhacLoaiCuaTruyVan', () => {
    expect(sourceCreationBody({ residentId: 42, sourceType: 'DOSE', sources: [first],
      selection: [sourceSelectionKey(42, first)] })).toBeNull()
  })

  it('giuNguyenNguonDaCoKhoanDeBackendTraKetQuaTungNguon', () => {
    const source = { ...first, existingItemId: 91 }
    expect(sourceCreationBody({ residentId: 42, sourceType: 'OVERNIGHT_STAY', sources: [source],
      selection: [sourceSelectionKey(42, source)] }).sourceUnits).toHaveLength(1)
  })

  it('khongVaChamKhoaKhiIdHoacDonViCoKyTuPhanCach', () => {
    expect(sourceSelectionKey(42, { ...first, sourceId: 'a:b', billingUnit: 'c' }))
      .not.toBe(sourceSelectionKey(42, { ...first, sourceId: 'a', billingUnit: 'b:c' }))
  })

  it('hienLyDoTiengVietTuBackendVaCoCauDuPhong', () => {
    expect(sourceSkipMessage({ reason: 'Cần chọn đúng đêm.' })).toBe('Cần chọn đúng đêm.')
    expect(sourceSkipMessage({ errorCode: 'COST_ITEM_ALREADY_EXISTS' })).toBe('Nguồn này đã có khoản đang hiệu lực.')
    expect(sourceSkipMessage({ errorCode: 'NEW_UNKNOWN_CODE' })).toBe('Chưa thể lập khoản từ nguồn này.')
  })
})
