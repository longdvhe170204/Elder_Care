import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { MealsPage } from './meals/MealsPage'
import { NutritionCatalogPage } from './catalog/NutritionCatalogPage'

let api

function permissions(pairs) {
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      pairs.map(([f, o]) => ({ function: f, operation: o })),
    ),
  )
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/floors', json(200, [{ id: 5, name: 'Tầng 1' }]))
  api.on(
    'GET',
    '/nutrition/diets',
    json(200, [{ id: 1, name: 'Thường', status: 'ACTIVE', restrictions: [], version: 0 }]),
  )
  api.on('GET', '/nutrition/dishes', json(200, []))
  api.on('GET', '/nutrition/meals', json(200, []))
  api.on('GET', '/menus', json(200, []))
  api.on('GET', '/residents', json(200, { items: [] }))
  api.on('GET', (u) => u.pathname.startsWith('/api/v1/catalogs/'), json(200, []))
})

afterEach(() => vi.unstubAllGlobals())

it('Dinh dưỡng chỉ hiện tab theo quyền; bếp thấy nút tạo thực đơn', async () => {
  permissions([
    ['DISH_MENU', 'VIEW'],
    ['DISH_MENU', 'CREATE'],
  ])
  render(
    <PermissionsProvider>
      <NutritionCatalogPage today="2026-11-08" />
    </PermissionsProvider>,
  )
  expect(await screen.findByRole('button', { name: 'Tạo thực đơn tuần' })).toBeTruthy()
  expect(screen.getByText('Thực đơn tuần')).toBeTruthy()
  expect(screen.queryByText('Bữa ăn')).toBeNull()
  expect(screen.queryByText('Loại đồ ăn cấm')).toBeNull()
})

it('Suất ăn: bếp chốt suất sau bước xác nhận, ghi Idempotency-Key', async () => {
  permissions([
    ['MEAL_COUNT_TICKET', 'VIEW'],
    ['MEAL_PREPARATION', 'CREATE'],
  ])
  api.on(
    'GET',
    '/daily-meals',
    json(200, [
      { dailyMealId: 9, mealId: 1, mealName: 'Bữa trưa', date: '2026-11-08', expectedTime: null, closed: false },
    ]),
  )
  api.on(
    'GET',
    '/daily-meals/9/precomputed',
    json(200, {
      portionsByFloorArea: [{ floorAreaId: 5, dietId: 1, portionCount: 4 }],
      specialPortions: 0,
      familyMealPortions: 0,
      flags: [],
      missingData: [],
    }),
  )
  api.on('POST', '/daily-meals/9/close-portions', json(201, {}))
  render(
    <PermissionsProvider>
      <MealsPage initialDate="2026-11-08" />
    </PermissionsProvider>,
  )
  fireEvent.click(await screen.findByRole('button', { name: 'Bữa trưa' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Chốt suất…' }))
  expect(await screen.findByText(/mỗi bữa chỉ chốt một lần/)).toBeTruthy()
  expect(api.calls('POST', '/daily-meals/9/close-portions')).toHaveLength(0)
  fireEvent.click(screen.getByRole('button', { name: 'Chốt suất' }))
  await waitFor(() => expect(api.calls('POST', '/daily-meals/9/close-portions')).toHaveLength(1))
  expect(api.calls('POST', '/daily-meals/9/close-portions')[0][1].headers['Idempotency-Key']).toBeTruthy()
})

it('Suất ăn: người không có quyền xem bữa nhận trạng thái không có quyền', async () => {
  permissions([])
  api.on('GET', '/daily-meals', problem(403, 'PERMISSION_DENIED'))
  render(
    <PermissionsProvider>
      <MealsPage initialDate="2026-11-08" />
    </PermissionsProvider>,
  )
  expect(await screen.findByText(/không có quyền/i)).toBeTruthy()
})
