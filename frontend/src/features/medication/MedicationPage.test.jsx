import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MedicationPage } from './MedicationPage'

// 006 Phát thuốc theo DESIGN.md 6.7: liều Trễ nổi lên ở dải "Cần làm ngay" (7.2), thẻ không có viền đỏ 1,5px (đỏ chỉ
// dành cho chữ việc gấp). Màn vẫn dùng dữ liệu minh họa của bản mẫu cho tới khi hợp đồng có đủ trường tên thuốc.

describe('MedicationPage', () => {
  it('DESIGN_6_7_lieuTre_hienOReDaiCanLamNgay', () => {
    render(<MedicationPage role="NURSING_STAFF" />)
    expect(screen.getByRole('region', { name: 'Cần làm ngay' })).toBeTruthy()
  })

  it('DESIGN_6_7_theLieuTre_khongCoVienDo', () => {
    const { container } = render(<MedicationPage role="NURSING_STAFF" />)
    expect(container.querySelector('[class*="ring-[1.5px]"]')).toBeNull()
  })
})
