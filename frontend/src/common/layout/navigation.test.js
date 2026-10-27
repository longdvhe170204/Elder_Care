import { describe, expect, it } from 'vitest'
import { NAV_GROUPS, navForRole } from './navigation'

const keys = (groups) => groups.flatMap((g) => g.items.map((i) => i.key))

// 002 FR-042: có phiên thật thì menu theo quyền ở GET /me/permissions; bản mẫu thì theo vai trò (Phụ lục 27).
describe('navForRole', () => {
  it('có quyền thật: chỉ hiện mục có chức năng được cấp, cùng Yêu cầu phê duyệt, Thông báo, Chờ xem lại', () => {
    const granted = new Set(['TASK_CHECKLIST', 'MEDICATION_ADMINISTRATION'])

    expect(keys(navForRole('CAREGIVER', (fn) => granted.has(fn)))).toEqual([
      'approval-requests',
      'notifications',
      'pending-review-records',
      'care-tasks',
      'medication',
    ])
  })

  it('có quyền thật: tài khoản nhiều vai trò thấy mục mà vai trò chính không có', () => {
    // Vai trò chính Nhân viên chăm sóc nhưng tài khoản có thêm quyền Danh mục thuốc
    expect(keys(navForRole('CAREGIVER', (fn) => fn === 'MEDICATION_CATALOG'))).toContain('medication-catalog')
  })

  it('bản mẫu không có backend: lọc theo vai trò', () => {
    const expected = NAV_GROUPS.flatMap((g) => g.items)
      .filter((i) => i.roles.includes('KITCHEN_STAFF'))
      .map((i) => i.key)

    expect(keys(navForRole('KITCHEN_STAFF'))).toEqual(expected)
  })

  it('mục chưa có màn thì ẩn, có màn thì hiện', () => {
    const built = new Set(['care-tasks'])
    const all = () => true

    expect(keys(navForRole('NURSING_STAFF', all, built))).toEqual(['care-tasks'])
    expect(keys(navForRole('NURSING_STAFF', null, built))).toEqual(['care-tasks'])
  })

  it('mục có chức năng đều ghi mã chức năng', () => {
    const withoutFunctions = NAV_GROUPS.flatMap((g) => g.items)
      .filter((i) => !i.functions)
      .map((i) => i.key)

    expect(withoutFunctions).toEqual(['approval-requests', 'notifications', 'pending-review-records'])
  })
})
