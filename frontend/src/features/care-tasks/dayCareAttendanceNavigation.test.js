import { it, expect } from 'vitest'
import { NAV_GROUPS, canSee } from '@/common/layout/navigation'
it('FR_049_menuDiemDanhDungChucNangVaQuyen', () => {
  const item = NAV_GROUPS.flatMap((g) => g.items).find((i) => i.key === 'day-care-attendance')
  expect(item).toBeDefined()
  expect(item.functions).toEqual(['DAY_CARE_ATTENDANCE'])
  expect(canSee(item, 'OFFICE_STAFF', (fn) => fn === 'DAY_CARE_ATTENDANCE')).toBe(true)
  expect(canSee(item, 'OFFICE_STAFF', () => false)).toBe(false)
})
