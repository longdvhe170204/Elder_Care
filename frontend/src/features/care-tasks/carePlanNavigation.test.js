import { expect, it } from 'vitest'
import { NAV_GROUPS, canSee } from '@/common/layout/navigation'

it('T017_menuKeHoachVaDanhMucTheoDungDongQuyen', () => {
  const plans = NAV_GROUPS.flatMap((group) => group.items).find((item) => item.key === 'care-plans')
  const catalogs = NAV_GROUPS.flatMap((group) => group.items).find((item) => item.key === 'care-catalogs')
  expect(plans).toBeDefined()
  expect(catalogs).toBeDefined()
  expect(canSee(plans, 'DOCTOR')).toBe(true)
  expect(canSee(plans, 'OFFICE_STAFF')).toBe(false)
  expect(canSee(catalogs, 'CAREGIVER')).toBe(true)
  expect(canSee(plans, 'NURSING_STAFF', (fn) => fn === 'CARE_TASK_CATALOG')).toBe(false)
  expect(canSee(catalogs, 'NURSING_STAFF', (fn) => fn === 'CARE_TASK_CATALOG')).toBe(true)
})
