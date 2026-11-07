import { describe, expect, it } from 'vitest'
import { breakdownLabel } from './reportLabels'

const floors = [
  { id: 101, name: 'Tầng 1' },
  { id: 102, name: 'Tầng 2' },
]

describe('breakdownLabel', () => {
  it('đổi khóa trạng thái, mức, loại sang chữ tiếng Việt', () => {
    expect(breakdownLabel('BED.STATUS', 'OCCUPIED')).toBe('Đang sử dụng')
    expect(breakdownLabel('BED.STATUS', 'HELD_ABSENT_RESIDENT')).toBe('Đang giữ chỗ (người vắng)')
    expect(breakdownLabel('HEALTH.VITAL_SIGNS', 'DANGEROUS')).toBe('Nguy hiểm')
    expect(breakdownLabel('RES.WAITING_LIST', 'WAITING_CANCELLED')).toBe('Hủy chờ')
    expect(breakdownLabel('RES.WAITING_TIME', 'DAY_CARE')).toBe('Bán trú')
    expect(breakdownLabel('SHIFT.UNCONFIRMED_HANDOVERS', 'HAS_COMMENTS')).toBe('Có ý kiến')
  })

  it('khóa tầng hiện tên tầng; không có trong danh sách thì ghi mã', () => {
    expect(breakdownLabel('BED.AWAITING_READY', '102', floors)).toBe('Tầng 2')
    expect(breakdownLabel('BED.OCCUPANTS', '999', floors)).toBe('Tầng/khu 999')
  })

  it('khóa ca của liều thuốc', () => {
    expect(breakdownLabel('MED.MISSED_RATE', '7')).toBe('Ca 7')
    expect(breakdownLabel('MED.REFUSED_RATE', 'NO_SHIFT')).toBe('Không xác định ca')
  })

  it('khóa chưa biết giữ nguyên', () => {
    expect(breakdownLabel('TEST.X', 'ABC')).toBe('ABC')
  })
})
