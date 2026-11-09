import { describe, expect, it } from 'vitest'
import { NAV_GROUPS, groupOfPage, navForRole } from './navigation'

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

  it('nhóm nghiệp vụ có id tiếng Anh riêng và tên tiếng Việt; khối đầu không có tên', () => {
    const [first, ...rest] = NAV_GROUPS

    expect(first.label).toBeNull()
    expect(rest.every((g) => /^[a-z]+(-[a-z]+)*$/.test(g.id) && g.label)).toBe(true)
    expect(new Set(NAV_GROUPS.map((g) => g.id)).size).toBe(NAV_GROUPS.length)
  })

  it('bản mẫu: nhóm không còn mục nào sau khi lọc theo vai trò thì ẩn cả nhóm', () => {
    const groups = navForRole('KITCHEN_STAFF')

    expect(groups.map((g) => g.label)).toContain('Dinh dưỡng, bữa ăn')
    expect(groups.map((g) => g.label)).not.toContain('Chăm sóc hằng ngày')
    expect(groups.every((g) => g.items.length > 0)).toBe(true)
  })

  it('có quyền thật: nhóm không có chức năng nào được cấp thì ẩn', () => {
    const labels = navForRole('NURSING_STAFF', (fn) => fn === 'MEDICATION_ADMINISTRATION').map((g) => g.label)

    expect(labels).toEqual([null, 'Thuốc và sức khỏe'])
  })

  it('groupOfPage: trả id nhóm chứa trang', () => {
    expect(groupOfPage('medication')).toBe('medication-health')
    expect(groupOfPage('dashboard')).toBe('general')
    expect(groupOfPage('khong-co')).toBeUndefined()
  })

  it('mỗi mục menu có khóa riêng', () => {
    const all = keys(NAV_GROUPS)

    expect(all.filter((key, i) => all.indexOf(key) !== i)).toEqual([])
  })
})
