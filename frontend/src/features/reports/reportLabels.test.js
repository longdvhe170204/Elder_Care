import { describe, expect, it } from 'vitest'
import { breakdownLabel, labelNotes } from './reportLabels'

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

  it('khóa của chỉ tiêu cảnh báo, sự cố, theo dõi, tài sản (T018, T032)', () => {
    expect(breakdownLabel('HEALTH.ALERTS', 'CRITICAL')).toBe('Khẩn cấp')
    expect(breakdownLabel('HEALTH.INCIDENTS', 'LOW')).toBe('Nhẹ')
    expect(breakdownLabel('HEALTH.SOS_ASSISTANCE_ALERTS', 'EMERGENCY_ASSIST')).toBe('Nút hỗ trợ khẩn cấp')
    expect(breakdownLabel('HEALTH.SOS_ASSISTANCE_ALERTS', 'SOS')).toBe('Nút SOS')
    expect(breakdownLabel('HEALTH.FOLLOW_UPS', 'POST_FALL')).toBe('Theo dõi sau ngã')
    expect(breakdownLabel('HEALTH.FOLLOW_UPS', 'CONTACT')).toBe('Theo dõi tiếp xúc')
    expect(breakdownLabel('ASSET.OVERDUE', 'VEHICLE_INSPECTION_OVERDUE')).toBe('Quá hạn đăng kiểm')
  })

  it('khóa của chỉ tiêu người cao tuổi, công việc, hoạt động, kiểm tra', () => {
    expect(breakdownLabel('RES.IN_RESIDENCE', 'TEMPORARILY_ABSENT')).toBe('Tạm vắng')
    expect(breakdownLabel('RES.AT_DATE', 'UNDETERMINED')).toBe('Chưa xác định')
    expect(breakdownLabel('RES.MOVEMENTS', 'RECORD_RETURN:HOSPITALIZED')).toBe('Về viện sau điều trị')
    expect(breakdownLabel('TASK.TODAY', 'COMPLETED_LATE')).toBe('Hoàn thành trễ')
    expect(breakdownLabel('TASK.OVERDUE', 'MANDATORY')).toBe('Bắt buộc')
    expect(breakdownLabel('TASK.BY_STATUS', 'NOT_PERFORMED:SYSTEM:AFTER_HANDOVER')).toBe('Không thực hiện (Hệ thống, AFTER_HANDOVER)')
    expect(breakdownLabel('TASK.BY_STAFF', 'COMMON')).toBe('Việc chung của tầng')
    expect(breakdownLabel('TASK.BY_STAFF', '51')).toBe('Nhân viên 51')
    expect(breakdownLabel('ACT.OVERDUE_TRIPS', 'OVERDUE')).toBe('Quá giờ về')
    expect(breakdownLabel('ACT.OVERDUE_TRIPS', 'OVERDUE_DOUBLE')).toBe('Tới mốc gấp đôi')
    expect(breakdownLabel('QC.BY_CHECKER_ROLE', 'SHIFT_LEAD')).toBe('Người phụ trách ca')
    expect(breakdownLabel('MEAL.LATE_TICKETS', '11')).toBe('Bữa 11')
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

describe('labelNotes', () => {
  it('nhãn dữ liệu đã loại bỏ kèm loại dữ liệu theo module (016 T109, FR-007)', () => {
    expect(
      labelNotes({ labels: ['PURGED_BY_RETENTION'], purgedDataTypes: ['M07.DOSE', 'M07.DOSE_RESULT', 'M04.TASK'] }),
    ).toBe('số liệu không đầy đủ do đã loại bỏ theo lưu trữ (thuốc, chăm sóc hằng ngày)')
    expect(labelNotes({ labels: ['PURGED_BY_RETENTION'] })).toBe('số liệu không đầy đủ do đã loại bỏ theo lưu trữ')
  })
})

describe('breakdownLabel chi tiêu nguồn mới (016 T008, T014, T020, T024)', () => {
  it('chi phí, mua sắm, số dư, khai báo, yêu cầu ca, giờ làm', () => {
    expect(breakdownLabel('COST.SUMMARY', 'RESIDENCY_FEE')).toBe('Phí lưu trú')
    expect(breakdownLabel('COST.EVENT_GENERATED_RATE', 'EVENT_BASED')).toBe('Theo sự kiện')
    expect(breakdownLabel('COST.POST_FINALIZATION_ADJUSTMENTS', '2026-10')).toBe('Kỳ gốc 10/2026')
    expect(breakdownLabel('COST.PURCHASING', 'RECONCILED')).toBe('Đã đối chiếu')
    expect(breakdownLabel('BALANCE.LOW_OR_OWING', 'IN_DEBT')).toBe('Còn nợ')
    expect(breakdownLabel('REGISTRATION.TO_DO', 'TO_DEREGISTER')).toBe('Cần xóa đăng ký')
    expect(breakdownLabel('SHIFT.REQUESTS', 'M09.SHIFT_SWAP:APPROVED')).toBe('Đổi ca, Đã duyệt')
  })
})
