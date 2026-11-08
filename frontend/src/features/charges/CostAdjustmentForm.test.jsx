import { expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { postCostsItemsByIdAdjust } from '@/api'
import { CostAdjustmentForm } from './CostAdjustmentForm'

vi.mock('@/api', () => ({ postCostsItemsByIdAdjust: vi.fn().mockResolvedValue({ status: 201 }) }))

it('FR_027_doesNotSuggestAnAmountAndSendsExplicitAdjustmentWithSourceReference', async () => {
  render(<CostAdjustmentForm itemId={7} sourceChangeId={9} />)
  const amount = screen.getByLabelText('Số tiền điều chỉnh (đồng)')
  expect(amount).toHaveValue(null)
  fireEvent.change(amount, { target: { value: '-30000' } })
  fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Hủy ghi nhận nguồn' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lập khoản điều chỉnh' }))
  await waitFor(() =>
    expect(postCostsItemsByIdAdjust).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { id: 7 },
        body: { amount: -30000, adjustmentType: 'SOURCE_CHANGE', reason: 'Hủy ghi nhận nguồn', sourceChangeId: 9 },
      }),
    ),
  )
})
