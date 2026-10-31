import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { CarePlanPage } from './CarePlanPage'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/residents', json(200, { items: [{ id: 77, fullName: '[Họ tên kiểm thử]' }], nextCursor: null }))
  api.on(
    'GET',
    '/residents/77/care-plans',
    json(200, [
      { id: 10, revision: 1, status: 'EXPIRED', effectiveFrom: '2026-10-01' },
      { id: 11, revision: 2, status: 'EFFECTIVE', effectiveFrom: '2026-10-20' },
    ]),
  )
  api.on('GET', '/me/permissions', json(200, [{ function: 'CARE_PLAN', operation: 'CREATE' }]))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_001_lapNhapLaLenhNguoiDungVaGuiLuaChonSaoChep', async () => {
  api.on('POST', '/residents/77/care-plans', json(201, { id: 12 }, { ETag: '"0"' }))
  api.on(
    'GET',
    '/care-plans/12',
    json(
      200,
      { id: 12, residentId: 77, revision: 3, status: 'DRAFT', items: [], riskAgreements: [], allowedCommands: [] },
      { ETag: '"0"' },
    ),
  )
  api.on('GET', '/task-types', json(200, []))
  render(
    <PermissionsProvider>
      <CarePlanPage residentId={77} />
    </PermissionsProvider>,
  )
  expect(await screen.findByRole('button', { name: 'Phiên bản 1' })).toBeInTheDocument()
  expect(api.calls('POST', '/residents/77/care-plans')).toHaveLength(0)
  fireEvent.click(await screen.findByRole('button', { name: 'Lập phiên bản nháp' }))
  await waitFor(() => expect(api.calls('POST', '/residents/77/care-plans')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/residents/77/care-plans')[0][1].body)).toEqual({ copyCurrent: true })
})

it('FR_008_soSanhDungHaiPhienBanDaChon', async () => {
  api.on(
    'GET',
    '/care-plans/compare',
    json(200, { added: [{ id: 9, content: 'Mục thêm kiểm thử' }], removed: [], changed: [] }),
  )
  render(
    <PermissionsProvider>
      <CarePlanPage residentId={77} />
    </PermissionsProvider>,
  )
  await screen.findByRole('button', { name: 'Phiên bản 1' })
  fireEvent.change(screen.getByLabelText('So sánh từ phiên bản'), { target: { value: '10' } })
  fireEvent.change(screen.getByLabelText('Đến phiên bản'), { target: { value: '11' } })
  fireEvent.click(screen.getByRole('button', { name: 'So sánh phiên bản' }))
  expect(await screen.findByText('Mục thêm kiểm thử')).toBeInTheDocument()
  const [url] = api.calls('GET', '/care-plans/compare')[0]
  expect(new URL(url, 'http://localhost').searchParams.get('from')).toBe('10')
})

it('FR_007_khongCoQuyenXemThiKhongHienDuLieuHayLenhLap', async () => {
  api.on('GET', '/me/permissions', json(200, []))
  api.on('GET', '/residents/77/care-plans', problem(403, 'PERMISSION_DENIED'))
  render(
    <PermissionsProvider>
      <CarePlanPage residentId={77} />
    </PermissionsProvider>,
  )
  expect(await screen.findByText('Bạn không có quyền xem kế hoạch chăm sóc')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Lập phiên bản nháp' })).not.toBeInTheDocument()
})
