import { describe, expect, it } from 'vitest'
import { amountText, confirmationFit, holderLabel, kindLabel, statusLabel, toOffsetIso } from './belongingLabels'

// Chép luật ReturnRecipient.matches của backend: lọc ở web chỉ để gợi ý, backend kiểm lại khi trả (FR-017)
describe('confirmationFit (FR-017, Q-253)', () => {
  const NOW = new Date('2026-11-06T05:00:00Z')
  const conf = (o = {}) => ({
    id: 8,
    recipientName: 'Lê Văn Nam',
    recipientIdDocument: 'CCCD 079000000102',
    recipientIsResident: false,
    belongingIds: [18],
    status: 'EFFECTIVE',
    expiresAt: '2026-11-06T07:00:00Z',
    ...o,
  })
  const nam = (o = {}) => ({
    type: 'OTHER_PERSON',
    fullName: 'Lê Văn Nam',
    documentType: 'CCCD',
    documentNumber: '079000000102',
    belongingIds: [18],
    now: NOW,
    ...o,
  })

  it('FR_017_khop_tenChuanHoa_giayToLoaiSoHoacChiSo', () => {
    expect(confirmationFit(conf(), nam())).toEqual({ fits: true, reason: null, extraIds: [] })
    expect(confirmationFit(conf(), nam({ fullName: '  lê  văn NAM ' })).fits).toBe(true)
    expect(confirmationFit(conf({ recipientIdDocument: '079000000102' }), nam()).fits).toBe(true)
    expect(confirmationFit(conf(), nam({ documentType: '' })).fits).toBe(false)
  })

  it('FR_017_khongKhop_khacNguoi_thieuDo_quaHan_choXacNhan', () => {
    expect(confirmationFit(conf(), nam({ documentNumber: '999' })).reason).toBe('person')
    expect(confirmationFit(conf(), nam({ belongingIds: [18, 19] })).reason).toBe('items')
    expect(confirmationFit(conf(), nam({ belongingIds: [19] })).reason).toBe('items')
    expect(confirmationFit(conf({ expiresAt: '2026-11-06T04:00:00Z' }), nam()).reason).toBe('expired')
    expect(confirmationFit(conf({ status: 'PENDING_CONFIRMATION', expiresAt: null }), nam()).reason).toBe('pending')
    expect(confirmationFit(conf({ status: 'USED' }), nam()).fits).toBe(false)
  })

  it('FR_017_xacNhanGomNhieuDoHon_traVeDoConLai', () => {
    expect(confirmationFit(conf({ belongingIds: [18, 19] }), nam())).toEqual({ fits: true, reason: null, extraIds: [19] })
  })

  it('Q_253_nguoiCaoTuoi_chiXetCoNguoiCaoTuoi', () => {
    const lan = { type: 'RESIDENT', belongingIds: [18], now: NOW }
    expect(confirmationFit(conf({ recipientIsResident: true, recipientName: 'Nguyễn Thị Lan' }), lan).fits).toBe(true)
    expect(confirmationFit(conf(), lan).reason).toBe('person')
    expect(confirmationFit(conf({ recipientIsResident: true }), nam()).reason).toBe('person')
  })
})

describe('belongingLabels', () => {
  it('FR_011_trangThaiDoGui_theoTenNghiepVu', () => {
    expect(statusLabel('HELD')).toBe('Đang giữ')
    expect(statusLabel('IN_USE')).toBe('Đang được người cao tuổi sử dụng')
    expect(statusLabel('LOST')).toBe('Thất lạc')
    expect(statusLabel('DAMAGED')).toBe('Hư hỏng')
    expect(statusLabel('RETURNED')).toBe('Đã trả')
    expect(statusLabel('DISPOSED')).toBe('Đã xử lý')
  })

  it('FR_011_loaiBanGhiVaNguoiGiu', () => {
    expect(kindLabel('INTAKE')).toBe('Tiếp nhận')
    expect(kindLabel('RETURN_BLOCKED')).toBe('Lần chặn trả')
    expect(holderLabel('STAFF')).toBe('Nhân viên')
    expect(holderLabel('RESIDENT')).toBe('Người cao tuổi')
    expect(holderLabel(null)).toBe('Không còn người giữ')
  })

  it('FR_005_soTienHienVND_soLuongKemDonVi', () => {
    expect(amountText({ cashAmount: 2000000 })).toBe('2.000.000 đồng')
    expect(amountText({ quantity: 5 }, 'bộ')).toBe('5 bộ')
    expect(amountText({ quantity: 1.5 })).toBe('1,5')
  })

  it('NFR_11_guiThoiDiemDangISO_cong7', () => {
    expect(toOffsetIso('2026-11-06T09:30')).toBe('2026-11-06T09:30:00+07:00')
  })
})
