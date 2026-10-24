import { afterEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { AllocationCorrectionForm } from './AllocationCorrectionForm'
afterEach(() => vi.unstubAllGlobals())
it('FR_026_correctionUsesWhitelistedNumericBedAndVietnamTime', async () => {
  const api = mockRoutes()
  api.on('GET', '/bed-allocations/41', json(200, { id: 41, status: 'CLOSED' }))
  api.on('POST', '/bed-allocations/41/corrections', json(201, null))
  render(<AllocationCorrectionForm allocationId={41} />)
  await screen.findByRole('heading', { name: 'Đính chính phân bổ' })
  fireEvent.change(screen.getByLabelText('Giường đúng'), { target: { value: '9' } })
  fireEvent.change(screen.getByLabelText('Kết thúc đúng'), { target: { value: '2026-01-02T07:00' } })
  fireEvent.change(screen.getByLabelText('Lý do đính chính'), { target: { value: 'Ghi đúng' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))
  expect(await screen.findByRole('status')).toHaveTextContent('Đã tiếp nhận đính chính')
  const call = api.calls('POST', '/bed-allocations/41/corrections')[0]
  expect(JSON.parse(call[1].body)).toEqual({
    type: 'AMEND_CONTENT',
    reason: 'Ghi đúng',
    correctedContent: { bedId: 9, endAt: '2026-01-02T07:00:00+07:00' },
  })
  expect(new Headers(call[1].headers).get('Idempotency-Key')).toBeTruthy()
})

it('FR_026_activeAllocationDoesNotOfferCorrectionForm', async () => {
  const api = mockRoutes()
  api.on('GET', '/bed-allocations/41', json(200, { id: 41, status: 'ACTIVE' }))
  render(<AllocationCorrectionForm allocationId={41} />)
  expect(await screen.findByText('Chỉ đính chính phân bổ đã đóng hoặc đã hủy.')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Lưu đính chính' })).not.toBeInTheDocument()
})
