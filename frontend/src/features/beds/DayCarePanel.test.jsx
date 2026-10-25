import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { DayCarePanel } from './DayCarePanel'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/day-care-rest-areas',
    json(200, [{ id: 1, code: 'D1', name: 'Khu nghỉ', floorId: 2, capacityPerSession: 3, status: 'ACTIVE' }]),
  )
  api.on(
    'GET',
    '/day-care-sessions',
    json(200, [{ id: 3, name: 'Sáng', startTime: '07:00', endTime: '12:00', status: 'ACTIVE' }]),
  )
})
afterEach(() => vi.unstubAllGlobals())

it('FR_038_viewConfigurationUsesBackendCapacityAndHidesCreateWithoutPermission', async () => {
  render(<DayCarePanel catalogs={{ floors: [{ id: 2, name: 'Tầng 2' }], areas: [] }} />)
  expect(await screen.findByText('Khu nghỉ')).toBeInTheDocument()
  expect(screen.getByText('3 chỗ mỗi buổi')).toBeInTheDocument()
  expect(screen.getByText('07:00 – 12:00')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Thêm buổi' })).not.toBeInTheDocument()
})

it('BR_M03_25_createSessionShowsOverCapacityWarningWithoutCancellingArrivals', async () => {
  api.on('POST', '/day-care-sessions', (_url, init) => {
    expect(JSON.parse(init.body)).toEqual({ name: 'Chiều', startTime: '12:00', endTime: '17:00' })
    return json(201, {
      session: { id: 4, name: 'Chiều' },
      overCapacitySessions: [
        { date: '2026-10-25', sessionId: 4, sessionName: 'Chiều', registrations: 4, capacity: 3, remaining: -1 },
      ],
    })
  })
  render(<DayCarePanel canCreate catalogs={{ areas: [], floors: [] }} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Thêm buổi' }))
  fireEvent.change(screen.getByLabelText('Tên buổi'), { target: { value: 'Chiều' } })
  fireEvent.change(screen.getByLabelText('Giờ bắt đầu'), { target: { value: '12:00' } })
  fireEvent.change(screen.getByLabelText('Giờ kết thúc'), { target: { value: '17:00' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu buổi' }))
  expect(await screen.findByText(/Chiều ngày 25\/10\/2026: 4 đăng ký, 3 chỗ/)).toBeInTheDocument()
})

it('FR_039_countsUseDateFiltersAndClearResultsOnFailure', async () => {
  api.on('GET', '/day-care-rest-areas/registration-counts', (url) => {
    expect(url.searchParams.get('from')).toBe('2026-10-25')
    expect(url.searchParams.get('to')).toBe('2026-10-25')
    return json(200, [
      { date: '2026-10-25', sessionId: 3, sessionName: 'Sáng', registrations: 4, capacity: 3, remaining: -1 },
    ])
  })
  render(<DayCarePanel />)
  await screen.findByText('Khu nghỉ')
  fireEvent.change(screen.getByLabelText('Từ ngày'), { target: { value: '2026-10-25' } })
  fireEvent.change(screen.getByLabelText('Đến ngày'), { target: { value: '2026-10-25' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tra cứu số đăng ký' }))
  expect(await screen.findByText('Vượt 1 chỗ')).toBeInTheDocument()
  api.on('GET', '/day-care-rest-areas/registration-counts', problem(503, 'SOURCE_UNAVAILABLE'))
  fireEvent.click(screen.getByRole('button', { name: 'Tra cứu số đăng ký' }))
  await screen.findByRole('alert')
  expect(screen.queryByText('Vượt 1 chỗ')).not.toBeInTheDocument()
})
