import { describe, expect, it } from 'vitest'
import { formatDate, formatDateTime } from './dateTime'

// NFR-11: ngày dd/MM/yyyy, giờ 24h theo Asia/Ho_Chi_Minh (000 T099).
describe('dateTime', () => {
  it('ngayKhongGio_khongLechMuiGio', () => {
    expect(formatDate('2026-10-19')).toBe('19/10/2026')
  })

  it('thoiDiem_doiSangNgayViet', () => {
    expect(formatDate('2026-10-19T18:30:00Z')).toBe('20/10/2026')
    expect(formatDateTime('2026-10-19T18:30:00Z')).toBe('20/10/2026 01:30')
  })

  it('rong_traChuoiRong', () => {
    expect(formatDate(null)).toBe('')
  })
})
