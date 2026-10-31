import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { CarePlanDetail } from './CarePlanDetail'

let api
const plan = {
  id: 17,
  residentId: 77,
  revision: 2,
  status: 'DRAFT',
  items: [],
  riskAgreements: [],
  allowedCommands: ['submit', 'replaceItems', 'replaceRiskAgreements', 'cancel'],
  createdBy: '[Người lập kiểm thử]',
  noEffectivePlan: true,
}
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/care-plans/17', json(200, plan, { ETag: '"4"' }))
  api.on('GET', '/task-types', json(200, []))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_003_guiDuyetGuiNgayDeXuatVaLyDo', async () => {
  api.on('POST', '/care-plans/17/submit', json(200, null))
  render(<CarePlanDetail planId={17} onChanged={vi.fn()} />)
  fireEvent.change(await screen.findByLabelText('Ngày hiệu lực đề xuất'), { target: { value: '2026-11-02' } })
  fireEvent.change(screen.getByLabelText('Lý do thay đổi'), { target: { value: 'Theo đánh giá mới' } })
  fireEvent.click(screen.getByRole('button', { name: 'Gửi duyệt' }))
  await waitFor(() => expect(api.calls('POST', '/care-plans/17/submit')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/care-plans/17/submit')[0][1].body)).toEqual({
    effectiveFrom: '2026-11-02',
    changeReason: 'Theo đánh giá mới',
  })
})

it('FR_007_nutLenhChiTheoAllowedCommands', async () => {
  api.on('GET', '/care-plans/17', json(200, { ...plan, status: 'EFFECTIVE', allowedCommands: [] }, { ETag: '"5"' }))
  render(<CarePlanDetail planId={17} onChanged={vi.fn()} />)
  expect(await screen.findByText('Hiệu lực')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Gửi duyệt' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Lưu mục công việc' })).not.toBeInTheDocument()
})

it('FR_004_412HienGiaTriMoiVaTaiLaiKhongGhiDe', async () => {
  api.on(
    'PUT',
    '/care-plans/17/items',
    problem(412, 'VERSION_MISMATCH', { currentValue: { version: 5, status: 'PENDING_APPROVAL' } }),
  )
  render(<CarePlanDetail planId={17} onChanged={vi.fn()} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Lưu mục công việc' }))
  expect(await screen.findByText('Dữ liệu đã thay đổi')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Tải lại phiên bản' })).toBeInTheDocument()
  const [, request] = api.calls('PUT', '/care-plans/17/items')[0]
  expect(request.headers['If-Match']).toBe('"4"')
  expect(request.headers['Idempotency-Key']).toBeTruthy()
  expect(api.calls('PUT', '/care-plans/17/items')).toHaveLength(1)
})

it('FR_003_huyPhienBanBatBuocLyDoTaiO', async () => {
  render(<CarePlanDetail planId={17} onChanged={vi.fn()} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Hủy phiên bản' }))
  expect(await screen.findByText('Nhập lý do trước khi thực hiện.')).toBeInTheDocument()
  expect(api.calls('POST', '/care-plans/17/cancel')).toHaveLength(0)
})
