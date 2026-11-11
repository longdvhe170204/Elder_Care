import { beforeEach, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { LivePurchaseConsent } from './LivePurchaseConsent'

// 012 T076 (US5; 010 FR-023, FR-023a; Q-251): người đại diện xem đề nghị mua hộ Chờ đồng ý, Đồng ý hoặc Từ chối kèm lý
// do, có bước xem lại; lần xin đồng ý bổ sung hiện số đã đồng ý và số mới.

let api
const PENDING = {
  id: 31,
  residentId: 41,
  description: 'Sữa không đường',
  quantity: 2,
  estimatedAmount: 300000,
  reason: 'Hết sữa',
  status: 'AWAITING_CONSENT',
  additionalConsentPending: false,
  insufficientBalance: true,
}
const ADDITIONAL = {
  ...PENDING,
  id: 32,
  description: 'Tã người lớn',
  estimatedAmount: 200000,
  allowedAmount: 200000,
  additionalConsentPending: true,
  decisions: [{ type: 'REQUEST_ADDITIONAL', newAmount: 260000 }],
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/purchase-assistance-requests', json(200, [PENDING, ADDITIONAL]))
  api.on('POST', '/purchase-assistance-requests/31/agree', json(200, {}))
  api.on('POST', '/purchase-assistance-requests/32/reject', json(200, {}))
})

it('FR_023_dongY_coBuocXemLai_soDuKhongDuChiDeBiet', async () => {
  render(<LivePurchaseConsent residentId={41} />)
  expect(await screen.findAllByText('Số dư hiện không đủ cho số tiền này. Bạn vẫn có thể đồng ý hoặc từ chối.')).toHaveLength(2)
  fireEvent.click(screen.getByRole('button', { name: 'Đồng ý đề nghị #31' }))
  expect(screen.getByText(/Xem lại trước khi đồng ý: Sữa không đường/)).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đồng ý' }))
  await waitFor(() => expect(api.calls('POST', '/purchase-assistance-requests/31/agree')).toHaveLength(1))
})

it('Q_251_xinDongYBoSung_hienSoDaDongYVaSoMoi_tuChoiBatBuocLyDo', async () => {
  render(<LivePurchaseConsent residentId={41} />)
  const card = (await screen.findByText('Tã người lớn')).closest('[data-slot="card"]')
  expect(within(card).getByText('Đã đồng ý 200.000 đồng, nay cần đồng ý 260.000 đồng')).toBeInTheDocument()
  fireEvent.click(within(card).getByRole('button', { name: 'Từ chối đề nghị #32' }))
  expect(within(card).getByRole('button', { name: 'Xác nhận từ chối' })).toBeDisabled()
  fireEvent.change(within(card).getByLabelText('Lý do từ chối'), { target: { value: 'Giá cao' } })
  fireEvent.click(within(card).getByRole('button', { name: 'Xác nhận từ chối' }))
  await waitFor(() => expect(api.calls('POST', '/purchase-assistance-requests/32/reject')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/purchase-assistance-requests/32/reject')[0][1].body)).toEqual({ reason: 'Giá cao' })
})
