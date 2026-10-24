import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { BedsPage } from './BedsPage'

let api
const area = { id: 1, code: 'A', name: 'Khu A', status: 'ACTIVE', version: 0 }
const floor = { id: 2, areaId: 1, code: 'T1', name: 'Tầng 1', status: 'ACTIVE' }
const room = {
  id: 3,
  floorId: 2,
  roomTypeId: 4,
  code: 'P101',
  name: 'Phòng 101',
  status: 'ACTIVE',
  genderPolicy: 'UNRESTRICTED',
  allowedCareLevels: ['BASIC'],
}
const roomType = {
  id: 4,
  name: 'Phòng đôi',
  standardBeds: 2,
  defaultCareLevels: ['BASIC'],
  servesLongTerm: true,
  servesShortTerm: true,
  status: 'ACTIVE',
}
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, [{ function: 'ROOM_BED_CONFIGURATION', operation: 'VIEW' }]))
  api.on('GET', '/areas', json(200, [area]))
  api.on('GET', '/floors', json(200, [floor]))
  api.on('GET', '/room-types', json(200, [roomType]))
  api.on('GET', '/rooms', json(200, [room]))
  api.on('GET', '/bed-transfer-reasons', json(200, []))
  api.on(
    'GET',
    '/beds',
    json(200, {
      beds: [{ id: 5, code: 'G1', state: 'EMPTY', location: { roomId: 3, label: 'Khu A – Tầng 1 – Phòng 101 – G1' } }],
      residentsWithoutBed: [],
    }),
  )
  api.on('GET', '/areas/1', json(200, area, { ETag: '"0"' }))
  api.on('GET', '/rooms/3', json(200, room, { ETag: '"0"' }))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_038_configurationViewerCanOpenDayCareTab', async () => {
  api.on('GET', '/day-care-rest-areas', json(200, []))
  api.on('GET', '/day-care-sessions', json(200, []))
  render(<BedsPage />)
  fireEvent.click(await screen.findByRole('tab', { name: 'Bán trú' }))
  expect(await screen.findByRole('heading', { name: 'Khu nghỉ bán trú' })).toBeInTheDocument()
})

it('FR_037_infectionViewerCanOpenHistoryWithoutConfigurationPermission', async () => {
  api.on('GET', '/me/permissions', json(200, [{ function: 'INFECTION_CONTAINMENT', operation: 'CREATE' }]))
  render(<BedsPage />)
  expect(await screen.findByRole('tab', { name: 'Lịch sử vị trí' })).toHaveAttribute('aria-selected', 'true')
  expect(screen.getByRole('heading', { name: 'Lịch sử vị trí' })).toBeInTheDocument()
  expect(screen.queryByRole('tab', { name: 'Cơ cấu' })).not.toBeInTheDocument()
})

it('FR_022_allocationOnlyViewerCanOpenInitialAllocationBoard', async () => {
  api.on(
    'GET',
    '/me/permissions',
    json(200, [
      { function: 'BED_ALLOCATION', operation: 'VIEW' },
      { function: 'BED_ALLOCATION', operation: 'CREATE' },
    ]),
  )
  api.on('GET', '/beds/summary', json(200, { usable: 4, emptyMatching: 2 }))
  api.on('GET', '/bed-allocations', json(200, []))
  render(<BedsPage />)
  fireEvent.click(await screen.findByRole('tab', { name: 'Phân bổ giường' }))
  expect(await screen.findByRole('tab', { name: 'Phân bổ giường' })).toHaveAttribute('aria-selected', 'true')
  expect(await screen.findByText('Sức chứa sử dụng: 4')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Tìm hồ sơ' })).toBeInTheDocument()
})

it('BR_M03_22_bedCreationKeepsTheSuccessfulWarningVisible', async () => {
  manager()
  api.on(
    'POST',
    '/beds',
    json(201, {
      bed: { id: 6 },
      warnings: [{ code: 'STANDARD_BED_COUNT_EXCEEDED', description: 'Số giường của phòng vượt số giường tiêu chuẩn' }],
    }),
  )
  render(<BedsPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Thêm giường Phòng 101' }))
  fireEvent.change(await screen.findByLabelText('Mã giường'), { target: { value: 'G2' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))
  expect(await screen.findByText('Số giường của phòng vượt số giường tiêu chuẩn')).toBeInTheDocument()
  expect(JSON.parse(api.calls('POST', '/beds')[0][1].body)).toEqual({ roomId: 3, code: 'G2' })
})
function manager() {
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      ['VIEW', 'CREATE', 'UPDATE'].map((operation) => ({ function: 'ROOM_BED_CONFIGURATION', operation })),
    ),
  )
}
it('FR_002_viewPermissionShowsHierarchyAndHidesWriteControls', async () => {
  render(<BedsPage />)
  expect(await screen.findByText('Khu A – Tầng 1 – Phòng 101 – G1')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Thêm khu vực' })).not.toBeInTheDocument()
})
it('US1_1_createAreaSendsOnlyContractFieldsAndIdempotencyKey', async () => {
  manager()
  api.on('POST', '/areas', json(201, { ...area, id: 6, code: 'B', name: 'Khu B' }, { ETag: '"0"' }))
  render(<BedsPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Thêm khu vực' }))
  fireEvent.change(await screen.findByLabelText('Mã khu vực'), { target: { value: 'B' } })
  fireEvent.change(screen.getByLabelText('Tên khu vực'), { target: { value: 'Khu B' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))
  await waitFor(() => expect(api.calls('POST', '/areas')).toHaveLength(1))
  const call = api.calls('POST', '/areas')[0]
  expect(JSON.parse(call[1].body)).toEqual({ code: 'B', name: 'Khu B' })
  expect(call[1].headers['Idempotency-Key']).toBeTruthy()
})
it('FR_005_deactivationShowsAllBlockingConditions', async () => {
  manager()
  api.on(
    'POST',
    '/rooms/3/deactivate',
    problem(409, 'CONDITION_NOT_MET', {
      unmetConditions: [{ code: 'FR-005', description: 'G1 chưa ở Không sử dụng' }],
    }),
  )
  render(<BedsPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Ngừng hiệu lực Phòng 101' }))
  fireEvent.change(await screen.findByLabelText('Lý do'), { target: { value: 'Ngừng phòng' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận' }))
  expect(await screen.findByText('G1 chưa ở Không sử dụng')).toBeInTheDocument()
})
it('FR_021_staleUpdateShowsCurrentValueAndReloadsEtag', async () => {
  manager()
  api.on(
    'PUT',
    '/areas/1',
    problem(412, 'VERSION_MISMATCH', { currentValue: { ...area, name: 'Khu đã đổi', version: 1 } }),
  )
  render(<BedsPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Sửa Khu A' }))
  fireEvent.change(await screen.findByLabelText('Tên khu vực'), { target: { value: 'Tên cũ' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))
  expect(await screen.findByText('Khu đã đổi')).toBeInTheDocument()
  api.on('GET', '/areas/1', json(200, { ...area, name: 'Khu đã đổi', version: 1 }, { ETag: '"1"' }))
  fireEvent.click(screen.getByRole('button', { name: 'Tải lại dữ liệu' }))
  await waitFor(() => expect(screen.getByLabelText('Tên khu vực')).toHaveValue('Khu đã đổi'))
})

it('FR_021_allocationViewerCanOpenTransfersWithoutConfigurationAccess', async () => {
  api.on('GET', '/me/permissions', json(200, [{ function: 'BED_ALLOCATION', operation: 'VIEW' }]))
  api.on('GET', '/bed-allocations', json(200, []))
  render(<BedsPage />)
  expect(await screen.findByRole('tab', { name: 'Chuyển giường' })).toHaveAttribute('aria-selected', 'true')
  expect(await screen.findByText('Chưa có phân bổ đang hiệu lực trong phạm vi được xem.')).toBeInTheDocument()
  expect(screen.queryByRole('tab', { name: 'Cơ cấu' })).not.toBeInTheDocument()
  expect(api.calls('GET', '/areas')).toHaveLength(0)
})
