import { describe, expect, it } from 'vitest'
import { SESSION_STATUS, addDays, countByStatus, toOffsetIso, weekOf } from './sessionView'

describe('sessionView', () => {
  it('FR_011_nhanTrangThaiBuoiTheoGlossary', () => {
    expect(SESSION_STATUS).toEqual({
      SCHEDULED: 'Đã lên lịch',
      ATTENDANCE_TAKEN: 'Đã điểm danh',
      NO_ATTENDANCE: 'Không điểm danh',
      CANCELLED: 'Đã hủy',
    })
  })

  it('FR_011_tuanTuThuHaiDenChuNhat', () => {
    // 05/11/2026 là thứ Năm
    expect(weekOf('2026-11-05')).toEqual({ from: '2026-11-02', to: '2026-11-08' })
    expect(addDays('2026-11-02', 7)).toBe('2026-11-09')
  })

  it('FR_011_demTheoTrangThai', () => {
    const counts = countByStatus([{ status: 'SCHEDULED' }, { status: 'SCHEDULED' }, { status: 'CANCELLED' }])
    expect(counts).toEqual({ ALL: 3, SCHEDULED: 2, ATTENDANCE_TAKEN: 0, NO_ATTENDANCE: 0, CANCELLED: 1 })
  })

  it('FR_011_guiGioVietNamCoMuiGio', () => {
    expect(toOffsetIso('2026-11-06T15:00')).toBe('2026-11-06T15:00:00+07:00')
    expect(toOffsetIso('')).toBeUndefined()
  })
})
