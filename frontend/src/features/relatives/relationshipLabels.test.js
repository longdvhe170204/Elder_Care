import { describe, expect, it } from 'vitest'
import { RIGHT_ORDER, rightLines, rolesOf } from './relationshipLabels'

// 012 T013 (FR-002, FR-007, FR-011): nhãn vai trò và sáu quyền của một quan hệ; quyền bật mà không có tác dụng ghi rõ.

const relationship = (rights, extra = {}) => ({
  id: 1,
  representative: false,
  primaryContact: false,
  rights,
  ...extra,
})

describe('relationshipLabels', () => {
  it('FR_002_vaiTro_noiBangCau', () => {
    expect(rolesOf(relationship({}, { representative: true, primaryContact: true }))).toBe(
      'Người đại diện, Người liên hệ chính',
    )
    expect(rolesOf(relationship({}))).toBe('')
  })

  it('FR_011_quyenBatMaKhongCoTacDung_ghiRo', () => {
    const lines = rightLines(
      relationship({
        VIEW_HEALTH: { enabled: true, effective: false },
        REGISTER_VISITS: { enabled: true, effective: true },
      }),
    )
    expect(lines).toEqual([
      { right: 'VIEW_HEALTH', label: 'Xem sức khỏe', value: 'Có, không có tác dụng' },
      { right: 'VIEW_COSTS', label: 'Xem chi phí', value: 'Không' },
      { right: 'RECEIVE_URGENT', label: 'Nhận thông báo khẩn', value: 'Không' },
      { right: 'REGISTER_VISITS', label: 'Đăng ký thăm', value: 'Có' },
      { right: 'PICKUP_ALLOWED', label: 'Được phép đón', value: 'Không' },
      { right: 'REQUEST_SERVICES', label: 'Được yêu cầu thay đổi dịch vụ', value: 'Không' },
    ])
    expect(RIGHT_ORDER).toHaveLength(6)
  })
})
