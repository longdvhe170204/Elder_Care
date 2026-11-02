import { describe, expect, it } from 'vitest'
import { basisLabel } from './notificationLabels'

// 009 FR-009, FR-010, FR-011: nhãn căn cứ người nhận từ mã backend.

describe('basisLabel', () => {
  it('FR_009_tamNhan', () => {
    expect(basisLabel('Staff:TAM_NHAN')).toBe('Tạm nhận – bàn giao chưa xác nhận')
  })

  it('FR_009_tamNhan_nhanThay', () => {
    expect(basisLabel('SUBSTITUTE:Staff:TAM_NHAN')).toBe('Nhận thay cho: Tạm nhận – bàn giao chưa xác nhận')
  })

  it('canCuKhac_giuNguyen', () => {
    expect(basisLabel('Staff:REQUESTER')).toBe('Nhân viên gắn bản ghi')
    expect(basisLabel('FacilityRole:ACCOUNTANT')).toBe('Theo vai trò ACCOUNTANT')
  })
})
