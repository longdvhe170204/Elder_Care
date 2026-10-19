import { describe, expect, it } from 'vitest'
import { formatValue } from './parameterValue'

// 000 T081: hiển thị giá trị tham số theo kiểu (FR-011, NFR-11).
describe('formatValue', () => {
  it('thoiLuong_soVaDonVi', () => {
    expect(formatValue('DURATION', { value: 48, unit: 'HOURS' })).toBe('48 giờ')
    expect(formatValue('DURATION', { value: 90, unit: 'DAYS' })).toBe('90 ngày')
  })

  it('soNguyen_tien_tyLe_gio', () => {
    expect(formatValue('INTEGER', 3, 'TIMES')).toBe('3 lần')
    expect(formatValue('INTEGER', 2, null)).toBe('2')
    expect(formatValue('MONEY', 500000, 'VND')).toBe('500.000 đồng')
    expect(formatValue('PERCENTAGE', 5)).toBe('5%')
    expect(formatValue('TIME_OF_DAY', '16:00')).toBe('16:00')
  })

  it('bang_vaChuaDat', () => {
    expect(formatValue('TABLE', { rows: [{ a: 1 }, { a: 2 }] })).toBe('Bảng 2 dòng')
    expect(formatValue('TABLE', { count: 3 })).toBe('Bảng')
    expect(formatValue('INTEGER', null, 'PERSONS')).toBe('Chưa đặt')
  })
})
