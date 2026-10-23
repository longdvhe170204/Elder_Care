import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { BedTransfersPanel } from './BedTransfersPanel'

let api
const allocation = {
  id: 41,
  residentId: 11,
  status: 'ACTIVE',
  startAt: '2026-10-01T08:00:00+07:00',
  location: { bedId: 5, label: 'Khu A – Tầng 1 – P101 – G1' },
}
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/bed-allocations', json(200, [allocation]))
  api.on(
    'GET',
    '/bed-allocations/suitable-beds',
    json(200, [{ location: { bedId: 6, label: 'Khu A – Tầng 2 – P201 – G2' }, state: 'EMPTY' }]),
  )
  api.on(
    'GET',
    '/bed-transfer-reasons',
    json(200, [
      { id: 2, name: 'Theo nguyện vọng', status: 'ACTIVE' },
      { id: 3, name: 'Lý do đã ngừng', status: 'INACTIVE' },
    ]),
  )
  api.on(
    'POST',
    '/bed-allocations/41/transfer',
    json(200, { previousAllocation: allocation, newAllocation: { id: 42 }, warnings: [] }),
  )
})
afterEach(() => vi.unstubAllGlobals())

async function open() {
  render(<BedTransfersPanel canTransfer />)
  fireEvent.click(await screen.findByRole('button', { name: 'Chuyển giường hồ sơ 11' }))
  const dialog = await screen.findByRole('dialog', { name: 'Chuyển giường' })
  await within(dialog).findByRole('option', { name: 'Khu A – Tầng 2 – P201 – G2' })
  return within(dialog)
}
function fill(dialog) {
  fireEvent.change(dialog.getByLabelText('Giường mới'), { target: { value: '6' } })
  fireEvent.change(dialog.getByLabelText('Lý do chuyển'), { target: { value: '2' } })
  fireEvent.change(dialog.getByLabelText('Lý do của lệnh'), { target: { value: 'Chuyển theo yêu cầu đã xác nhận' } })
}

it('FR_022_destinationsAreNotPreselectedAndUseTheCurrentAllocationContext', async () => {
  const dialog = await open()
  expect(dialog.getByLabelText('Giường mới')).toHaveValue('')
  expect(dialog.getByLabelText('Lý do chuyển')).toHaveValue('')
  expect(dialog.getByRole('button', { name: 'Xác nhận chuyển' })).toBeDisabled()
  expect(dialog.queryByRole('option', { name: 'Lý do đã ngừng' })).not.toBeInTheDocument()
  const url = new URL(api.calls('GET', '/bed-allocations/suitable-beds')[0][0], 'http://localhost')
  expect(url.searchParams.get('residentId')).toBe('11')
  expect(url.searchParams.get('fromAllocationId')).toBe('41')
})

it('FR_028_successSendsOnlyContractFieldsAndKeepsWarningsAfterRefreshing', async () => {
  api.on(
    'POST',
    '/bed-allocations/41/transfer',
    json(200, {
      previousAllocation: allocation,
      newAllocation: { id: 42 },
      warnings: [{ code: 'AWAITING_REQUEST', description: 'Cần lập yêu cầu thay đổi lưu trú.' }],
    }),
  )
  const dialog = await open()
  fill(dialog)
  fireEvent.click(dialog.getByRole('button', { name: 'Xác nhận chuyển' }))
  expect(await screen.findByText('Đã chuyển giường.')).toBeInTheDocument()
  expect(screen.getByText('Cần lập yêu cầu thay đổi lưu trú.')).toBeInTheDocument()
  const request = api.calls('POST', '/bed-allocations/41/transfer')[0][1]
  expect(JSON.parse(request.body)).toEqual({
    newBedId: 6,
    transferReasonId: 2,
    reason: 'Chuyển theo yêu cầu đã xác nhận',
  })
  expect(request.headers['Idempotency-Key']).toBeTruthy()
  expect(request.headers['If-Match']).toBeUndefined()
  await waitFor(() => expect(api.calls('GET', '/bed-allocations')).toHaveLength(2))
})

it('FR_027_pendingApprovalDoesNotClaimThatTheResidentHasMoved', async () => {
  api.on('POST', '/bed-allocations/41/transfer', json(202, { approvalRequestId: 51, status: 'DRAFT' }))
  const dialog = await open()
  fill(dialog)
  fireEvent.click(dialog.getByRole('button', { name: 'Xác nhận chuyển' }))
  expect(await screen.findByText('Đã lập yêu cầu nhập bù #51, chờ Quản lý viện duyệt.')).toBeInTheDocument()
  expect(screen.queryByText('Đã chuyển giường.')).not.toBeInTheDocument()
  expect(screen.getByText(allocation.location.label)).toBeInTheDocument()
})

it('FR_018_conflictShowsEveryConditionAndReloadClearsTheBedChoice', async () => {
  api.on(
    'POST',
    '/bed-allocations/41/transfer',
    problem(409, 'UNMET_CONDITIONS', {
      unmetConditions: [
        { code: 'ALLOCATION_TIME_OVERLAP', description: 'Giường vừa được phân bổ.' },
        { code: 'ALLOCATION_ISOLATION', description: 'Phòng đang cách ly.' },
      ],
    }),
  )
  const dialog = await open()
  fill(dialog)
  fireEvent.click(dialog.getByRole('button', { name: 'Xác nhận chuyển' }))
  expect(await dialog.findByText('Giường vừa được phân bổ.')).toBeInTheDocument()
  expect(dialog.getByText('Phòng đang cách ly.')).toBeInTheDocument()
  fireEvent.click(dialog.getByRole('button', { name: 'Tải lại giường phù hợp' }))
  await waitFor(() => expect(dialog.getByLabelText('Giường mới')).toHaveValue(''))
  expect(dialog.getByRole('button', { name: 'Xác nhận chuyển' })).toBeDisabled()
})

it('FR_027_backfillTimeUsesVietnamOffsetAndReloadsSuitableBeds', async () => {
  const dialog = await open()
  fireEvent.change(dialog.getByLabelText('Thời điểm chuyển (giờ Việt Nam)'), { target: { value: '2026-10-23T14:00' } })
  await waitFor(() => expect(api.calls('GET', '/bed-allocations/suitable-beds')).toHaveLength(2))
  await dialog.findByRole('option', { name: 'Khu A – Tầng 2 – P201 – G2' })
  fill(dialog)
  fireEvent.click(dialog.getByRole('button', { name: 'Xác nhận chuyển' }))
  await screen.findByText('Đã chuyển giường.')
  expect(JSON.parse(api.calls('POST', '/bed-allocations/41/transfer')[0][1].body).transferAt).toBe(
    '2026-10-23T14:00:00+07:00',
  )
  const url = new URL(api.calls('GET', '/bed-allocations/suitable-beds')[1][0], 'http://localhost')
  expect(url.searchParams.get('startAt')).toBe('2026-10-23T14:00:00+07:00')
})

it('FR_021_viewOnlyDoesNotOfferTheTransferCommand', async () => {
  render(<BedTransfersPanel canTransfer={false} />)
  expect(await screen.findByText(allocation.location.label)).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Chuyển giường hồ sơ 11' })).not.toBeInTheDocument()
  expect(api.calls('GET', '/bed-allocations/suitable-beds')).toHaveLength(0)
})

it('FR_028_inFlightCommandCannotBeDismissedOrSubmittedTwice', async () => {
  let complete
  api.on(
    'POST',
    '/bed-allocations/41/transfer',
    () =>
      new Promise((resolve) => {
        complete = resolve
      }),
  )
  const dialog = await open()
  fill(dialog)
  fireEvent.click(dialog.getByRole('button', { name: 'Xác nhận chuyển' }))
  expect(dialog.queryByRole('button', { name: 'Đóng' })).not.toBeInTheDocument()
  expect(dialog.getByRole('button', { name: 'Hủy' })).toBeDisabled()
  fireEvent.submit(dialog.getByRole('button', { name: 'Đang gửi…' }).closest('form'))
  expect(api.calls('POST', '/bed-allocations/41/transfer')).toHaveLength(1)
  complete(json(200, { newAllocation: { id: 42 }, warnings: [] }))
  expect(await screen.findByText('Đã chuyển giường.')).toBeInTheDocument()
})
