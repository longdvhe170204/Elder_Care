import { expect, it, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import * as api from '@/api'
import { PurchaseRequests } from './PurchaseRequests'
import { uploadFile } from '@/common/files'

const permissions = vi.hoisted(() => ({ operations: ['VIEW', 'APPROVE'] }))
vi.mock('@/common/files', () => ({ uploadFile: vi.fn() }))

vi.mock('@/api', async (importOriginal) => ({
  ApiError: (await importOriginal()).ApiError,
  getPurchaseAssistanceRequests: vi.fn(),
  postPurchaseAssistanceRequests: vi.fn(),
  postPurchaseAssistanceRequestsByIdAgree: vi.fn(),
  postPurchaseAssistanceRequestsByIdReject: vi.fn(),
  postPurchaseAssistanceRequestsByIdSend: vi.fn(),
  postPurchaseAssistanceRequestsByIdCancel: vi.fn(),
  postPurchaseAssistanceRequestsByIdRequestAdditionalConsent: vi.fn(),
  postPurchaseAssistanceRequestsByIdWithdrawAdditionalConsent: vi.fn(),
  postPurchaseAssistanceRequestsByIdRecordPurchased: vi.fn(),
}))
vi.mock('@/common/permissions/permissions', () => ({
  usePermissions: () => ({ status: 'ready', can: (_f, op) => permissions.operations.includes(op) }),
}))
beforeEach(() => {
  vi.clearAllMocks()
  permissions.operations = ['VIEW', 'APPROVE']
  api.getPurchaseAssistanceRequests.mockResolvedValue({
    data: [
      {
        id: 9,
        residentId: 7,
        description: 'Đồ dùng cá nhân',
        quantity: 1,
        estimatedAmount: 800000,
        status: 'AWAITING_CONSENT',
        decisions: [],
      },
    ],
  })
  api.postPurchaseAssistanceRequestsByIdAgree.mockResolvedValue({})
})

it('FR_024_uploadsReceiptOnceAndKeepsIdempotencyKeyWhenRetrying', async () => {
  permissions.operations = ['VIEW', 'UPDATE']
  api.getPurchaseAssistanceRequests.mockResolvedValue({
    data: [
      {
        id: 9,
        residentId: 7,
        description: 'Đồ dùng cá nhân',
        quantity: 1,
        estimatedAmount: 300000,
        allowedAmount: 300000,
        status: 'PURCHASE_ALLOWED',
        decisions: [],
      },
    ],
  })
  uploadFile.mockResolvedValue(41)
  api.postPurchaseAssistanceRequestsByIdRecordPurchased.mockRejectedValueOnce({ status: 503 }).mockResolvedValueOnce({})
  render(<PurchaseRequests />)
  fireEvent.click(await screen.findByRole('button', { name: 'Xem đề nghị #9' }))
  fireEvent.click(screen.getByRole('button', { name: 'Ghi đã mua' }))
  fireEvent.change(screen.getByLabelText('Số tiền (đồng, bắt buộc)'), { target: { value: '300000' } })
  fireEvent.change(screen.getByLabelText('Ngày mua (bắt buộc)'), { target: { value: '2026-11-08' } })
  const file = new File(['receipt'], 'receipt.pdf', { type: 'application/pdf' })
  fireEvent.change(screen.getByLabelText('Chứng từ (bắt buộc)'), { target: { files: [file] } })
  fireEvent.submit(screen.getByRole('button', { name: 'Xác nhận ghi đã mua' }).closest('form'))
  await waitFor(() => expect(api.postPurchaseAssistanceRequestsByIdRecordPurchased).toHaveBeenCalledTimes(1))
  await waitFor(() => expect(screen.getByRole('button', { name: 'Xác nhận ghi đã mua' })).not.toBeDisabled())
  fireEvent.submit(screen.getByRole('button', { name: 'Xác nhận ghi đã mua' }).closest('form'))
  await waitFor(() => expect(api.postPurchaseAssistanceRequestsByIdRecordPurchased).toHaveBeenCalledTimes(2))
  expect(uploadFile).toHaveBeenCalledExactlyOnceWith(file)
  const [first, retry] = api.postPurchaseAssistanceRequestsByIdRecordPurchased.mock.calls
  expect(first[0]).toEqual(retry[0])
  expect(first[0].body).toEqual({ actualAmount: 300000, purchasedOn: '2026-11-08', receiptFileId: 41 })
})

it('FR_023_conflictReloadsAndDoesNotAnnounceSuccessfulDecision', async () => {
  api.postPurchaseAssistanceRequestsByIdAgree.mockRejectedValue({ status: 409 })
  render(<PurchaseRequests />)
  fireEvent.click(await screen.findByRole('button', { name: 'Xem đề nghị #9' }))
  fireEvent.click(screen.getByRole('button', { name: 'Đồng ý' }))
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đồng ý' }))
  await waitFor(() => expect(api.getPurchaseAssistanceRequests).toHaveBeenCalledTimes(2))
  expect(screen.getByRole('status')).toHaveTextContent('Đề nghị đã thay đổi')
})
it('FR_023_requiresReviewBeforeConsentAndReloadsAfterDecision', async () => {
  render(<PurchaseRequests />)
  fireEvent.click(await screen.findByRole('button', { name: 'Xem đề nghị #9' }))
  fireEvent.click(screen.getByRole('button', { name: 'Đồng ý' }))
  expect(api.postPurchaseAssistanceRequestsByIdAgree).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đồng ý' }))
  await waitFor(() =>
    expect(api.postPurchaseAssistanceRequestsByIdAgree).toHaveBeenCalledWith(
      expect.objectContaining({ path: { id: 9 }, headers: { 'Idempotency-Key': expect.any(String) } }),
    ),
  )
  await waitFor(() => expect(api.getPurchaseAssistanceRequests).toHaveBeenCalledTimes(2))
})
it('FR_023_rejectionRequiresReasonAnd403ClearsPrivateData', async () => {
  api.postPurchaseAssistanceRequestsByIdReject.mockRejectedValue({ status: 403 })
  render(<PurchaseRequests />)
  fireEvent.click(await screen.findByRole('button', { name: 'Xem đề nghị #9' }))
  fireEvent.click(screen.getByRole('button', { name: 'Từ chối' }))
  expect(screen.getByRole('button', { name: 'Xác nhận từ chối' })).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Lý do (bắt buộc)'), { target: { value: 'Không còn nhu cầu' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận từ chối' }))
  await waitFor(() => expect(screen.queryByText('Đồ dùng cá nhân')).not.toBeInTheDocument())
})
