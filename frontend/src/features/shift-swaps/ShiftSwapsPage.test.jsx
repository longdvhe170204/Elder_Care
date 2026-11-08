import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ShiftSwapsPage } from './ShiftSwapsPage'

const PERMISSIONS = [{ function: 'SHIFT_SWAP_REQUEST', operation: 'VIEW' }]
const SWAP = {
  id: 7,
  status: 'AWAITING_RECIPIENT',
  kind: 'TAKE_OVER',
  giverStaffId: 1,
  recipientStaffId: 2,
  shiftXId: 20,
  reason: '[Lý do riêng tư]',
  selfApproved: false,
  allowedCommands: ['accept', 'decline'],
}
const CHECKS = {
  overlaps: [],
  continuousHours: [],
  nonTransferableAssignments: [],
  leadAfterSwap: { shiftX: { before: 1, after: 2 } },
  overlappingTrips: [],
}
let api
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, PERMISSIONS))
  api.on(
    'GET',
    '/me/shifts',
    json(200, [{ id: 20, status: 'PUBLISHED', startsAt: '2026-11-20T00:00:00Z', endsAt: '2026-11-20T08:00:00Z' }]),
  )
  api.on('GET', '/staff', json(200, [{ id: 2, fullName: '[Điều dưỡng 2]', status: 'WORKING' }]))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_022_hienDanhSachVaChiNutLenhBackendChoPhep', async () => {
  api.on('GET', '/shift-swaps', json(200, [SWAP]))
  render(<ShiftSwapsPage />)
  expect(await screen.findByText('#7')).toBeVisible()
  expect(screen.getByText('Chờ đồng ý')).toBeVisible()
  expect(screen.getByRole('button', { name: 'Đồng ý' })).toBeVisible()
  expect(screen.queryByRole('button', { name: 'Duyệt' })).not.toBeInTheDocument()
})

it('FR_022_nguoiNhanDongYGuiLenhAccept', async () => {
  api.on('GET', '/shift-swaps', json(200, [SWAP]))
  api.on('GET', '/shift-swaps/7/checks', json(200, CHECKS))
  api.on('POST', '/shift-swaps/7/accept', json(200, {}))
  render(<ShiftSwapsPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Đồng ý' }))
  const form = await screen.findByRole('form', { name: 'Đồng ý yêu cầu #7' })
  fireEvent.click(within(form).getByRole('button', { name: 'Đồng ý' }))
  await waitFor(() => expect(api.calls('POST', '/shift-swaps/7/accept')).toHaveLength(1))
})

it('FR_045_tuChoiBatBuocLyDo_khongGuiKhiTrong', async () => {
  api.on('GET', '/shift-swaps', json(200, [SWAP]))
  api.on('POST', '/shift-swaps/7/decline', json(200, {}))
  render(<ShiftSwapsPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Từ chối' }))
  const form = await screen.findByRole('form', { name: 'Từ chối yêu cầu #7' })
  fireEvent.submit(form)
  expect(within(form).getByLabelText('Lý do')).toHaveAttribute('aria-invalid', 'true')
  expect(api.calls('POST', '/shift-swaps/7/decline')).toHaveLength(0)
  fireEvent.change(within(form).getByLabelText('Lý do'), { target: { value: '[Bận]' } })
  fireEvent.submit(form)
  await waitFor(() => expect(api.calls('POST', '/shift-swaps/7/decline')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/shift-swaps/7/decline')[0][1].body)).toEqual({ reason: '[Bận]' })
})

it('FR_021_duyetCanhBaoGioLienTucPhaiXacNhanVaGuiConfirmWarning', async () => {
  api.on(
    'GET',
    '/shift-swaps',
    json(200, [{ ...SWAP, status: 'PENDING_APPROVAL', allowedCommands: ['approve', 'reject'] }]),
  )
  api.on('GET', '/shift-swaps/7/checks', json(200, { ...CHECKS, continuousHours: [{ staffId: 2, shiftId: 20 }] }))
  api.on('POST', '/shift-swaps/7/approve', json(200, { notTransferredAssignments: [] }))
  render(<ShiftSwapsPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Duyệt' }))
  const form = await screen.findByRole('form', { name: 'Duyệt yêu cầu #7' })
  expect(await within(form).findByText('Chuỗi giờ làm liên tục vượt ngưỡng')).toBeVisible()
  fireEvent.change(within(form).getByLabelText('Lý do'), { target: { value: '[Cần người]' } })
  expect(within(form).getByRole('button', { name: 'Duyệt' })).toBeDisabled()
  fireEvent.click(within(form).getByLabelText('Tôi đã xem cảnh báo'))
  fireEvent.click(within(form).getByRole('button', { name: 'Duyệt' }))
  await waitFor(() => expect(api.calls('POST', '/shift-swaps/7/approve')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/shift-swaps/7/approve')[0][1].body)).toMatchObject({
    confirmWarning: true,
    reason: '[Cần người]',
  })
})

it('FR_019_lapYeuCauNhanThayGuiDungHopDong_vaBaoLoiTrung', async () => {
  api.on('GET', '/shift-swaps', json(200, []))
  api.on('POST', '/shift-swaps', problem(409, 'SHIFT_IN_ANOTHER_REQUEST', { detail: 'Ca đang nằm trong yêu cầu khác' }))
  render(<ShiftSwapsPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Lập yêu cầu đổi ca' }))
  const form = await screen.findByRole('form', { name: 'Lập yêu cầu đổi ca' })
  fireEvent.change(within(form).getByLabelText('Dạng'), { target: { value: 'TAKE_OVER' } })
  await waitFor(() => expect(within(form).getAllByRole('option', { name: /20\/11\/2026/ }).length).toBeGreaterThan(0))
  fireEvent.change(within(form).getByLabelText('Ca của tôi'), { target: { value: '20' } })
  await waitFor(() => expect(within(form).getByRole('option', { name: '[Điều dưỡng 2]' })).toBeInTheDocument())
  fireEvent.change(within(form).getByLabelText('Người nhận'), { target: { value: '2' } })
  fireEvent.change(within(form).getByLabelText('Lý do'), { target: { value: '[Việc gia đình]' } })
  fireEvent.submit(form)
  await waitFor(() => expect(api.calls('POST', '/shift-swaps')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/shift-swaps')[0][1].body)).toEqual({
    kind: 'TAKE_OVER',
    shiftXId: 20,
    recipientId: 2,
    reason: '[Việc gia đình]',
  })
  expect(await screen.findByText('Ca đang nằm trong yêu cầu khác')).toBeVisible()
})

it('FR_028_nghiTheoCaDuyetMotPhanBatBuocLyDo', async () => {
  const LEAVE = {
    id: 9,
    status: 'PENDING_APPROVAL',
    kind: 'BY_SHIFT',
    staffId: 1,
    onBehalf: false,
    reason: '[Ốm]',
    items: [
      { shiftId: 20, result: 'PENDING' },
      { shiftId: 21, result: 'PENDING' },
    ],
    selfApproved: false,
    allowedCommands: ['approve', 'reject'],
  }
  api.on('GET', '/shift-swaps', json(200, []))
  api.on('GET', '/leave-requests', json(200, [LEAVE]))
  api.on('GET', '/leave-requests/9/pre-approval', json(200, []))
  api.on('POST', '/leave-requests/9/approve', json(200, {}))
  render(<ShiftSwapsPage />)
  fireEvent.click(await screen.findByRole('tab', { name: 'Nghỉ đột xuất' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Duyệt' }))
  const form = await screen.findByRole('form', { name: 'Duyệt yêu cầu nghỉ #9' })
  fireEvent.change(within(form).getByLabelText('Quyết định ca 21'), { target: { value: 'REJECTED' } })
  fireEvent.submit(form)
  expect(api.calls('POST', '/leave-requests/9/approve')).toHaveLength(0)
  fireEvent.change(within(form).getByLabelText('Lý do'), { target: { value: '[Thiếu người]' } })
  fireEvent.submit(form)
  await waitFor(() => expect(api.calls('POST', '/leave-requests/9/approve')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/leave-requests/9/approve')[0][1].body)).toEqual({
    reason: '[Thiếu người]',
    items: [
      { shiftId: 20, result: 'APPROVED' },
      { shiftId: 21, result: 'REJECTED' },
    ],
  })
})
