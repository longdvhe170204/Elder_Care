import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { LocationHistoryPanel } from './LocationHistoryPanel'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/residents', json(200, { items: [{ id: 12, profileCode: 'NCT12', fullName: '[Họ tên NCT 1]' }] }))
})
afterEach(() => vi.unstubAllGlobals())

async function selectResident() {
  fireEvent.change(screen.getByLabelText('Tìm người cao tuổi'), { target: { value: 'NCT12' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tìm hồ sơ' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Chọn NCT12' }))
}
function windowInputs() {
  fireEvent.change(screen.getByLabelText('Từ'), { target: { value: '2026-01-01T07:00' } })
  fireEvent.change(screen.getByLabelText('Đến'), { target: { value: '2026-01-02T07:00' } })
}

it('FR_035_lookupShowsCorrectedAndPendingHistoryUsingVietnamTime', async () => {
  api.on('GET', '/location-history/residents/12', (url) => {
    expect(url.searchParams.get('from')).toBe('2026-01-01T07:00:00+07:00')
    return json(200, [
      {
        allocationId: 41,
        location: { label: 'Khu A – P201 – G1', bedId: 9 },
        from: '2026-01-01T00:00:00Z',
        to: '2026-01-02T00:00:00Z',
        corrected: true,
        pendingBackfill: true,
      },
    ])
  })
  render(<LocationHistoryPanel />)
  await selectResident()
  windowInputs()
  fireEvent.click(screen.getByRole('button', { name: 'Tra cứu' }))
  expect(await screen.findByText('Khu A – P201 – G1')).toBeInTheDocument()
  expect(screen.getByText('01/01/2026 07:00')).toBeInTheDocument()
  expect(screen.getByText('Đã đính chính')).toBeInTheDocument()
  expect(screen.getByText('Có nhập bù đang chờ duyệt')).toBeInTheDocument()
})

it('FR_037_queryFailureClearsPreviousResults', async () => {
  api.on(
    'GET',
    '/location-history/residents/12',
    json(200, [{ allocationId: 41, location: { label: 'Khu A – P201 – G1' }, from: '2026-01-01T00:00:00Z', to: null }]),
  )
  render(<LocationHistoryPanel />)
  await selectResident()
  windowInputs()
  fireEvent.click(screen.getByRole('button', { name: 'Tra cứu' }))
  expect(await screen.findByText('Khu A – P201 – G1')).toBeInTheDocument()
  api.on('GET', '/location-history/residents/12', problem(403, 'PERMISSION_DENIED'))
  fireEvent.click(screen.getByRole('button', { name: 'Tra cứu' }))
  await screen.findByText('Bạn không có quyền xem lịch sử vị trí')
  expect(screen.queryByText('Khu A – P201 – G1')).not.toBeInTheDocument()
})

it('FR_036_roommatesShowWhoWasAbsentAndWhy', async () => {
  api.on(
    'GET',
    '/location-history/roommates',
    json(200, [
      {
        residentId: 13,
        fullName: '[Họ tên NCT 2]',
        room: 'Phòng 201',
        overlaps: [{ from: '2026-01-01T00:00:00Z', to: '2026-01-02T00:00:00Z' }],
        absences: [
          { residentId: 12, type: 'TEMPORARILY_ABSENT', from: '2026-01-01T01:00:00Z', to: '2026-01-01T02:00:00Z' },
        ],
        corrected: false,
        pendingBackfill: false,
      },
    ]),
  )
  render(<LocationHistoryPanel />)
  await selectResident()
  fireEvent.change(screen.getByLabelText('Tra cứu theo'), { target: { value: 'roommates' } })
  windowInputs()
  fireEvent.click(screen.getByRole('button', { name: 'Tra cứu' }))
  expect(await screen.findByText('[Họ tên NCT 2]')).toBeInTheDocument()
  expect(screen.getByText(/Tạm vắng/)).toHaveTextContent('[Họ tên NCT 1]')
})
