import { expect, it, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { postCostsItemsManual } from '@/api'
import { uploadFile } from '@/common/files'
import { ManualCostForm } from './ManualCostForm'

vi.mock('@/api', async (importOriginal) => ({
  ApiError: (await importOriginal()).ApiError,
  postCostsItemsManual: vi.fn(),
}))
vi.mock('@/common/files', () => ({ uploadFile: vi.fn() }))

beforeEach(() => vi.clearAllMocks())

function fill() {
  fireEvent.change(screen.getByLabelText('Mã người cao tuổi'), { target: { value: '7' } })
  fireEvent.change(screen.getByLabelText('Mô tả'), { target: { value: 'Phí chấm dứt sớm' } })
  fireEvent.change(screen.getByLabelText('Ngày phát sinh'), { target: { value: '2026-11-05' } })
  fireEvent.change(screen.getByLabelText('Số lượng'), { target: { value: '1' } })
  fireEvent.change(screen.getByLabelText('Đơn vị'), { target: { value: 'lần' } })
  fireEvent.change(screen.getByLabelText('Đơn giá (đồng)'), { target: { value: '500000' } })
  fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Theo điều khoản hợp đồng' } })
}

it('FR_021_sendsManualItemWithEvidenceUploadedOnce', async () => {
  uploadFile.mockResolvedValue(31)
  postCostsItemsManual.mockResolvedValue({ status: 201 })
  render(<ManualCostForm today="2026-11-10" />)
  fill()
  const file = new File(['x'], 'hop-dong.pdf', { type: 'application/pdf' })
  fireEvent.change(screen.getByLabelText('Bằng chứng (nếu có)'), { target: { files: [file] } })
  fireEvent.click(screen.getByRole('button', { name: 'Lập khoản nhập tay' }))
  await waitFor(() =>
    expect(postCostsItemsManual).toHaveBeenCalledWith(
      expect.objectContaining({
        headers: { 'Idempotency-Key': expect.any(String) },
        body: {
          residentId: 7,
          description: 'Phí chấm dứt sớm',
          incurredOn: '2026-11-05',
          quantity: 1,
          unit: 'lần',
          unitPrice: 500000,
          reason: 'Theo điều khoản hợp đồng',
          fileIds: [31],
        },
      }),
    ),
  )
  expect(await screen.findByRole('status')).toHaveTextContent('Đã lập khoản nhập tay nháp')
  expect(uploadFile).toHaveBeenCalledExactlyOnceWith(file)
})

it('BR_M11_04_catalogConflictKeepsInputAndDoesNotAnnounceSuccess', async () => {
  postCostsItemsManual.mockRejectedValue({
    status: 409,
    body: { title: 'Mục có trong danh mục: Gạc vô trùng; lập khoản theo bản ghi nguồn hoặc đề nghị mua hộ' },
  })
  render(<ManualCostForm today="2026-11-10" />)
  fill()
  fireEvent.click(screen.getByRole('button', { name: 'Lập khoản nhập tay' }))
  await waitFor(() => expect(postCostsItemsManual).toHaveBeenCalledTimes(1))
  expect(screen.getByLabelText('Mô tả')).toHaveValue('Phí chấm dứt sớm')
  expect(screen.queryByRole('status')).toBeNull()
})

it('FR_021_incurredDateCannotBeAfterToday', () => {
  render(<ManualCostForm today="2026-11-10" />)
  expect(screen.getByLabelText('Ngày phát sinh')).toHaveAttribute('max', '2026-11-10')
})
