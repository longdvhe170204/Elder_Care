import { describe, expect, it } from 'vitest'
import { amountText, holderLabel, kindLabel, statusLabel, toOffsetIso } from './belongingLabels'

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
