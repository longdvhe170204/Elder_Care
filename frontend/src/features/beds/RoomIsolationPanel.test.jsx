import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { mockRoutes, json, problem } from '@/test/fetchRoutes'
import { RoomIsolationPanel } from './RoomIsolationPanel'

let api
const rooms = [{ id: 3, name: 'Phòng 101', code: 'P101', isolated: false, status: 'ACTIVE' }]
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/rooms/3/isolation-history', json(200, []))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_008_viewerReadsHistoryWithoutIsolationButtons', async () => {
  api.on(
    'GET',
    '/rooms/3/isolation-history',
    json(200, [{ action: 'ISOLATE', occurredAt: '2026-10-25T00:00:00Z', actor: 'Bác sĩ', reason: 'Theo chỉ định' }]),
  )
  render(<RoomIsolationPanel rooms={rooms} />)
  fireEvent.change(screen.getByLabelText('Phòng'), { target: { value: '3' } })
  expect(await screen.findByText('Theo chỉ định')).toBeInTheDocument()
  expect(screen.getByText('25/10/2026 07:00')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Đặt cách ly' })).not.toBeInTheDocument()
})

it('FR_008_isolationPostsReasonAndReloadsParentAfterSuccess', async () => {
  const changed = vi.fn()
  api.on('POST', '/rooms/3/isolate', (_url, init) => {
    expect(JSON.parse(init.body)).toEqual({ reason: 'Theo chỉ định' })
    expect(new Headers(init.headers).get('Idempotency-Key')).toBeTruthy()
    return json(200, { ...rooms[0], isolated: true })
  })
  render(<RoomIsolationPanel rooms={rooms} canIsolate onChanged={changed} />)
  fireEvent.change(screen.getByLabelText('Phòng'), { target: { value: '3' } })
  await screen.findByText('Chưa có lần đặt/gỡ cách ly.')
  fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Theo chỉ định' } })
  fireEvent.click(screen.getByRole('button', { name: 'Đặt cách ly' }))
  expect(await screen.findByText('Đã đặt cách ly phòng.')).toBeInTheDocument()
  expect(changed).toHaveBeenCalledOnce()
})

it('FR_008_failurePreservesReasonAndDoesNotReportSuccess', async () => {
  api.on('POST', '/rooms/3/lift-isolation', problem(503, 'SOURCE_UNAVAILABLE'))
  render(<RoomIsolationPanel rooms={[{ ...rooms[0], isolated: true }]} canLift />)
  fireEvent.change(screen.getByLabelText('Phòng'), { target: { value: '3' } })
  await screen.findByText('Chưa có lần đặt/gỡ cách ly.')
  fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Gỡ theo chỉ định' } })
  fireEvent.click(screen.getByRole('button', { name: 'Gỡ cách ly' }))
  await screen.findByRole('alert')
  expect(screen.getByLabelText('Lý do')).toHaveValue('Gỡ theo chỉ định')
  expect(screen.queryByText('Đã gỡ cách ly phòng.')).not.toBeInTheDocument()
})
