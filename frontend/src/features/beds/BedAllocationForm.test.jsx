import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { BedAllocationForm } from './BedAllocationForm'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/bed-allocations/suitable-beds',
    json(200, [{ location: { bedId: 6, label: 'Khu A – P201 – G2' }, state: 'EMPTY' }]),
  )
  api.on('POST', '/bed-allocations', json(201, { id: 41, status: 'FUTURE' }))
})
afterEach(() => vi.unstubAllGlobals())

function open(onSaved = vi.fn()) {
  render(
    <BedAllocationForm
      resident={{ id: 11, fullName: '[Họ tên NCT 1]', status: 'ADMISSION_IN_PROGRESS' }}
      onSaved={onSaved}
    />,
  )
}

it('FR_022_suitableBedsAreNeverPreselected', async () => {
  open()
  const select = await screen.findByRole('combobox', { name: 'Giường phù hợp' })
  expect(await screen.findByText('Khu A – P201 – G2')).toBeInTheDocument()
  expect(select).toHaveValue('')
  expect(screen.getByRole('button', { name: 'Xác nhận phân bổ' })).toBeDisabled()
  expect(
    new URL(api.calls('GET', '/bed-allocations/suitable-beds')[0][0], 'http://localhost').searchParams.get(
      'residentId',
    ),
  ).toBe('11')
})

it('FR_023_createSendsExplicitBedAndReasonWithIdempotencyKey', async () => {
  const saved = vi.fn()
  open(saved)
  await screen.findByText('Khu A – P201 – G2')
  fireEvent.change(screen.getByLabelText('Giường phù hợp'), { target: { value: '6' } })
  fireEvent.change(screen.getByLabelText('Lý do phân bổ'), { target: { value: 'Bố trí giường' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận phân bổ' }))
  await waitFor(() => expect(saved).toHaveBeenCalledWith({ id: 41, status: 'FUTURE' }))
  const request = api.calls('POST', '/bed-allocations')[0][1]
  expect(JSON.parse(request.body)).toEqual({ residentId: 11, bedId: 6, reason: 'Bố trí giường' })
  expect(request.headers['Idempotency-Key']).toBeTruthy()
})

it('FR_018_conflictShowsAllConditionsAndReloadClearsChoice', async () => {
  api.on(
    'POST',
    '/bed-allocations',
    problem(409, 'UNMET_CONDITIONS', {
      unmetConditions: [
        { code: 'ALLOCATION_GENDER', description: 'Giới tính không phù hợp' },
        { code: 'ALLOCATION_ISOLATION', description: 'Phòng đang cách ly' },
      ],
    }),
  )
  open()
  await screen.findByText('Khu A – P201 – G2')
  fireEvent.change(screen.getByLabelText('Giường phù hợp'), { target: { value: '6' } })
  fireEvent.change(screen.getByLabelText('Lý do phân bổ'), { target: { value: 'Bố trí giường' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận phân bổ' }))
  expect(await screen.findByText('Giới tính không phù hợp')).toBeInTheDocument()
  expect(screen.getByText('Phòng đang cách ly')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Tải lại giường' }))
  await waitFor(() => expect(api.calls('GET', '/bed-allocations/suitable-beds')).toHaveLength(2))
  expect(screen.getByLabelText('Giường phù hợp')).toHaveValue('')
})
