import { describe, expect, it } from 'vitest'
import { DEMO_CHECKLIST, DEMO_TASK_DETAILS, DEMO_TASK_TYPES } from '@/common/demoData'
import { checklistView, resultText } from './checklistView'

describe('checklistView (hợp đồng api-005 → màn Checklist ca)', () => {
  const view = checklistView(DEMO_CHECKLIST, DEMO_TASK_TYPES)
  const task = (id) => view.tasks.find((t) => t.id === id)

  it('đọc đủ trường FR-030 từ CongViec, kèm thỏa thuận theo người cao tuổi', () => {
    expect(task(101)).toMatchObject({
      resident: '[Họ tên NCT 1]',
      bed: '201-A',
      title: 'Hỗ trợ uống nước',
      content: 'Uống 200 ml nước ấm',
      source: 'Kế hoạch chăm sóc',
      plannedAt: '07:00',
      from: '07:00',
      to: '08:00',
      importance: 'Bắt buộc',
      status: 'Quá hạn',
      commands: ['GHI_NHAN', 'KHONG_THUC_HIEN'],
      kind: 'water',
    })
    expect(task(101).riskChoices).toEqual([{ choice: '[Lựa chọn minh họa: vẫn uống trà thường mỗi chiều]', reviewOn: '17/11/2026' }])
  })

  it('không có trường ngoài hợp đồng (dị ứng, kết quả, mã hồ sơ)', () => {
    for (const t of view.tasks) {
      expect(t).not.toHaveProperty('allergies')
      expect(t).not.toHaveProperty('result')
      expect(t).not.toHaveProperty('residentCode')
    }
  })

  it('việc gắn vị trí hiện vị trí thay người cao tuổi; tồn, chờ đo lại theo cờ của hợp đồng', () => {
    expect(task(106)).toMatchObject({ nctId: null, location: 'Phòng sinh hoạt chung, tầng 2', floorTask: true })
    expect(task(102).carry).toBe('tồn từ ca trước')
    expect(task(104).awaitingRemeasure).toBe(true)
    expect(task(105)).toMatchObject({ plannedAt: '09:45', from: '09:30' })
  })

  it('loại lý do chỉ lấy loại đang hiệu lực; kết quả việc đã đóng lấy từ CongViecChiTiet', () => {
    expect(view.reasons[0].ten).toBe('Người cao tuổi từ chối')
    expect(resultText(DEMO_TASK_DETAILS[111])).toBe('Phần lớn, lúc 07:40, [NV chăm sóc 1]')
    expect(resultText(DEMO_TASK_DETAILS[112])).toBe('Người cao tuổi từ chối')
  })
})
