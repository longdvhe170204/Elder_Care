import { expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { BillingSources } from './BillingSources'

vi.mock('@/api', () => ({
  getCostsBillingSources: vi
    .fn()
    .mockResolvedValue({
      data: [
        {
          sourceType: 'RESIDENCY_DAY',
          sourceId: '42:2026-11-01',
          billingUnit: 'DAY',
          incurredOn: '2026-11-01',
          quantity: 1,
          unit: 'ngày',
          description: 'Phí lưu trú',
          existingItemId: 9,
        },
      ],
    }),
  postCostsItemsFromEvents: vi.fn(),
}))

it('FR_005_keepsSourcesWithExistingCostsAndDoesNotPreselect', async () => {
  render(<BillingSources />)
  fireEvent.change(screen.getByLabelText('Mã người cao tuổi'), { target: { value: '42' } })
  fireEvent.change(screen.getByLabelText('Từ ngày'), { target: { value: '2026-11-01' } })
  fireEvent.change(screen.getByLabelText('Đến ngày'), { target: { value: '2026-11-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tra cứu nguồn' }))
  expect(await screen.findByText('Khoản #9')).toBeInTheDocument()
  expect(screen.getByRole('checkbox')).not.toBeChecked()
  expect(screen.queryByLabelText('Chưa lập khoản')).not.toBeInTheDocument()
})
