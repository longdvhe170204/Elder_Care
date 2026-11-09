import { expect, it } from 'vitest'
import {
  assignmentState,
  dishCommands,
  familyFoodCommands,
  mondayOf,
  portionsByDiet,
  slipCommands,
  weekDays,
  weekdayLabel,
} from './nutritionView'

it('lệnh món theo trạng thái và vai trò (FR-004a)', () => {
  expect(dishCommands({ status: 'DRAFT' }, { kitchen: true })).toEqual(['submit', 'cancel'])
  expect(dishCommands({ status: 'PENDING_CONFIRMATION' }, { doctor: true })).toEqual(['confirm', 'return'])
  expect(dishCommands({ status: 'ACTIVE', pendingRevision: {} }, { doctor: true })).toEqual([
    'confirm-revision',
    'return-revision',
    'deactivate',
  ])
  expect(dishCommands({ status: 'CANCELLED' }, { kitchen: true, doctor: true })).toEqual([])
})

it('lệnh phiếu tách phía bếp và phía tầng (FR-047, FR-048)', () => {
  expect(slipCommands({ status: 'PREPARED' }, { kitchen: true })).toEqual(['deliver'])
  expect(slipCommands({ status: 'DELIVERED' }, { kitchen: true })).toEqual([])
  expect(slipCommands({ status: 'DELIVERED' }, { receiver: true })).toEqual(['receive', 'report-discrepancy'])
})

it('lệnh đồ ăn gia đình theo trạng thái (FR-055)', () => {
  expect(familyFoodCommands({ status: 'NEEDS_CONFIRMATION' })).toEqual(['approve-use', 'reject-use'])
  expect(familyFoodCommands({ status: 'ENDED' })).toEqual([])
})

it('tuần bắt đầu thứ Hai, mã ngày ISO', () => {
  expect(mondayOf('2026-11-08')).toBe('2026-11-02')
  expect(weekDays('2026-11-02')).toHaveLength(7)
  expect(weekdayLabel('2026-11-02')).toBe('Thứ Hai, 02/11')
  expect(weekdayLabel('2026-11-08')).toBe('Chủ nhật, 08/11')
})

it('tổng suất theo chế độ ăn và bản gán hiện hành', () => {
  expect(
    portionsByDiet({
      portionsByFloorArea: [
        { floorAreaId: 1, dietId: 5, portionCount: 3 },
        { floorAreaId: 2, dietId: 5, portionCount: 2 },
      ],
    }),
  ).toEqual([{ dietId: 5, portionCount: 5 }])
  const s = assignmentState([
    { id: 1, status: 'DISCONTINUED' },
    { id: 2, status: 'ACTIVE' },
    { id: 3, status: 'PENDING_EFFECTIVE' },
  ])
  expect(s.current.id).toBe(2)
  expect(s.pending.map((a) => a.id)).toEqual([3])
})
