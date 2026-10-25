import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { BedAllocationsPanel } from './BedAllocationsPanel'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/beds/summary', json(200, { usable: 4, emptyMatching: 2, byState: { EMPTY: 2, OCCUPIED: 2 } }))
  api.on(
    'GET',
    '/bed-allocations',
    json(200, [
      {
        id: 41,
        residentId: 11,
        status: 'FUTURE',
        startAt: '2026-10-24T08:00:00+07:00',
        location: { label: 'Khu A – P201 – G2' },
      },
    ]),
  )
  api.on(
    'GET',
    '/residents',
    json(200, {
      items: [{ id: 12, fullName: '[Họ tên NCT 1]', profileCode: 'NCT12', status: 'ADMISSION_IN_PROGRESS' }],
    }),
  )
  api.on('GET', '/bed-allocations/suitable-beds', json(200, []))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_026_cancelledAllocationCanOpenCorrectionWithoutInventingLocationHistory', async () => {
  api.on(
    'GET',
    '/bed-allocations',
    json(200, [
      {
        id: 41,
        residentId: 11,
        status: 'CANCELLED',
        startAt: '2026-01-01T00:00:00Z',
        location: { label: 'Khu A – P201 – G2' },
      },
    ]),
  )
  api.on('GET', '/bed-allocations/41', json(200, { id: 41, status: 'CANCELLED' }))
  render(<BedAllocationsPanel canCreate />)
  fireEvent.click(await screen.findByRole('button', { name: 'Đính chính phân bổ 41' }))
  expect(await screen.findByRole('form', { name: 'Đính chính phân bổ' })).toBeInTheDocument()
})

it('FR_011_boardShowsRealCapacityAndAllocationsWithoutWritePermission', async () => {
  render(<BedAllocationsPanel />)
  expect(await screen.findByText('Sức chứa sử dụng: 4')).toBeInTheDocument()
  expect(screen.getByText('Khu A – P201 – G2')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Tìm hồ sơ' })).not.toBeInTheDocument()
})

it('FR_022_explicitResidentSelectionOpensUnselectedBedForm', async () => {
  render(<BedAllocationsPanel canCreate />)
  fireEvent.change(screen.getByLabelText('Tìm người cao tuổi'), { target: { value: 'NCT12' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tìm hồ sơ' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Chọn NCT12' }))
  expect(await screen.findByRole('combobox', { name: 'Giường phù hợp' })).toHaveValue('')
  expect(await screen.findByText('Chưa có giường phù hợp tại thời điểm này.')).toBeInTheDocument()
})
