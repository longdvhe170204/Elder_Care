import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { mockRoutes, json } from '@/test/fetchRoutes'
import { IsolationWorkspace } from './IsolationWorkspace'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/room-isolation/rooms', json(200, []))
  api.on('GET', '/me', json(200, { roles: ['AC-03'] }))
  api.on(
    'GET',
    '/room-isolation/pending-stays',
    json(200, [{ id: 9, residentId: 12, location: { label: 'Phòng 101, giường G1' } }]),
  )
})
afterEach(() => vi.unstubAllGlobals())

it('FR_024a_doctorReadsPendingStayAndConfirmsSelectedAllocation', async () => {
  api.on('POST', '/bed-allocations/9/confirm-isolated-stay', (_url, init) => {
    expect(JSON.parse(init.body)).toEqual({ reason: 'Đủ điều kiện ở lại' })
    return json(200, { id: 9 })
  })
  render(<IsolationWorkspace canLift />)
  fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận ở lại hồ sơ 12' }))
  expect(screen.getByText('Phòng 101, giường G1')).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Lý do xác nhận'), { target: { value: 'Đủ điều kiện ở lại' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu xác nhận ở lại' }))
  expect(await screen.findByText('Đã xác nhận ở lại giường.')).toBeInTheDocument()
})

it('BR_M03_21_transferChoosesDoctorAndBedFromAuthorizedLists', async () => {
  api.on('GET', '/bed-allocations', json(200, [{ id: 9, residentId: 12, location: { label: 'Phòng 101, giường G1' } }]))
  api.on('GET', '/bed-allocations/prescribing-doctors', json(200, [{ id: 31, fullName: 'Bác sĩ phụ trách' }]))
  api.on('GET', '/bed-allocations/suitable-beds', (url) => {
    expect(url.searchParams.get('infectionControl')).toBe('true')
    expect(url.searchParams.get('fromAllocationId')).toBe('9')
    return json(200, [{ location: { bedId: 8, label: 'Phòng 102, giường G2' } }])
  })
  api.on('POST', '/bed-allocations/9/infection-control-transfer', (_url, init) => {
    expect(JSON.parse(init.body)).toEqual({ newBedId: 8, prescribingDoctorId: 31, reason: 'Chuyển theo chỉ định' })
    return json(200, { warnings: [] })
  })
  render(<IsolationWorkspace canTransfer />)
  fireEvent.click(await screen.findByRole('button', { name: 'Chuyển kiểm soát lây nhiễm hồ sơ 12' }))
  await screen.findByRole('option', { name: 'Bác sĩ phụ trách' })
  fireEvent.change(screen.getByLabelText('Giường đích'), { target: { value: '8' } })
  fireEvent.change(screen.getByLabelText('Bác sĩ chỉ định'), { target: { value: '31' } })
  fireEvent.change(screen.getByLabelText('Lý do chuyển'), { target: { value: 'Chuyển theo chỉ định' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu chuyển kiểm soát lây nhiễm' }))
  expect(await screen.findByText('Đã chuyển giường theo chỉ định kiểm soát lây nhiễm.')).toBeInTheDocument()
})
