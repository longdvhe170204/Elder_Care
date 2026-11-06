import { beforeEach, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { ChargesPage } from './ChargesPage'

let api
let statement
let etag
const event = {
  id: 1,
  costType: 'SERVICE',
  origin: 'EVENT_BASED',
  description: 'Dịch vụ đã thực hiện',
  incurredOn: '2026-10-10',
  quantity: 1,
  unit: 'Lần',
  unitPrice: 300,
  amount: 300,
  flags: [],
  status: 'DRAFT',
}
const missing = {
  ...event,
  id: 2,
  description: 'Dịch vụ chờ đơn giá',
  unitPrice: undefined,
  amount: undefined,
  flags: ['MISSING_UNIT_PRICE'],
}
const manual = { ...event, id: 3, origin: 'MANUAL', description: 'Khoản nhập tay', status: 'CHECKED', amount: 500 }

function permissions(manager = false, viewOnly = false) {
  const keys = [
    { function: 'COST_ADJUSTMENT', operation: 'VIEW' },
    { function: 'PERIOD_CLOSING', operation: 'VIEW' },
  ]
  if (!viewOnly)
    keys.push(
      ...(manager
        ? [
            { function: 'COST_ADJUSTMENT', operation: 'APPROVE' },
            { function: 'PERIOD_CLOSING', operation: 'APPROVE' },
            { function: 'PERIOD_CLOSING', operation: 'FINALIZE' },
          ]
        : [
            { function: 'COST_ADJUSTMENT', operation: 'UPDATE' },
            { function: 'PERIOD_CLOSING', operation: 'UPDATE' },
          ]),
    )
  api.on('GET', '/me/permissions', json(200, keys))
}

beforeEach(() => {
  api = mockRoutes()
  etag = '"3"'
  statement = {
    id: 31,
    period: '2026-10',
    residentId: 5,
    statementType: 'REGULAR',
    finalPeriod: false,
    status: 'OPEN',
    total: 800,
    totalsByType: [{ type: 'SERVICE', total: 800 }],
    items: [event, missing, manual],
  }
  permissions()
  api.on(
    'GET',
    '/costs/periods',
    json(200, [
      { id: 10, month: '2026-10', fromDate: '2026-10-01', throughDate: '2026-10-31', status: 'AWAITING_FINALIZATION' },
    ]),
  )
  api.on('GET', '/costs/statements', () => json(200, [{ ...statement, items: undefined }]))
  api.on('GET', '/costs/statements/31', () => json(200, statement, { ETag: etag }))
  api.on('GET', '/costs/items/3', () =>
    json(200, {
      ...manual,
      commandHistory: [{ operation: 'CHECK', actorId: 70, at: '2026-11-02T01:00:00Z' }],
      recalculationHistory: [],
      adjustments: [],
    }),
  )
})

async function openStatement() {
  render(
    <PermissionsProvider>
      <ChargesPage />
    </PermissionsProvider>,
  )
  await screen.findByRole('option', { name: /10\/2026/ })
  fireEvent.change(screen.getByLabelText('Kỳ chi phí'), { target: { value: '10' } })
  fireEvent.click(await screen.findByRole('button', { name: 'Xem bảng #31' }))
  await screen.findByRole('heading', { name: 'Bảng #31' })
}

it('FR_015_khong_chon_san_kiem_tra_lo_va_hien_khoan_bi_bo_qua', async () => {
  api.on(
    'POST',
    '/costs/items/check',
    json(200, {
      succeededIds: [1],
      skipped: [{ itemId: 2, errorCode: 'CONDITION_NOT_MET', reason: 'Khoản chưa có đơn giá, số tiền' }],
    }),
  )
  await openStatement()
  expect(screen.getByRole('checkbox', { name: 'Chọn khoản #1' })).not.toBeChecked()
  expect(screen.queryByRole('button', { name: 'Duyệt các khoản đã chọn' })).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Chọn khoản #1' }))
  fireEvent.click(screen.getByRole('checkbox', { name: 'Chọn khoản #2' }))
  fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra các khoản đã chọn' }))
  const dialog = screen.getByRole('dialog', { name: 'Xác nhận kiểm tra chi phí' })
  expect(api.calls('POST', '/costs/items/check')).toHaveLength(0)
  fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận kiểm tra' }))
  await screen.findByText('Khoản #2: Khoản chưa có đơn giá, số tiền')
  const call = api.calls('POST', '/costs/items/check')[0][1]
  expect(JSON.parse(call.body)).toEqual({ itemIds: [1, 2] })
  expect(call.headers['Idempotency-Key']).toBeTruthy()
})

it('FR_033_412_giu_noi_dung_tai_lai_etag_roi_moi_cho_chot_lai', async () => {
  permissions(true)
  statement = { ...statement, status: 'AWAITING_FINALIZATION', items: [{ ...event, status: 'APPROVED' }] }
  let attempts = 0
  api.on('POST', '/costs/statements/31/finalize', () => {
    attempts += 1
    if (attempts === 1) {
      etag = '"9"'
      return problem(412, 'VERSION_MISMATCH', { currentValue: { id: 31, status: 'AWAITING_FINALIZATION', version: 9 } })
    }
    return json(200, {})
  })
  await openStatement()
  fireEvent.click(screen.getByRole('button', { name: 'Chốt bảng' }))
  const dialog = screen.getByRole('dialog', { name: 'Xác nhận chốt bảng' })
  fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận chốt bảng' }))
  await within(dialog).findByRole('button', { name: 'Tải lại bảng' })
  expect(within(dialog).getByRole('button', { name: 'Xác nhận chốt bảng' })).toBeDisabled()
  fireEvent.click(within(dialog).getByRole('button', { name: 'Tải lại bảng' }))
  await waitFor(() => expect(within(dialog).getByRole('button', { name: 'Xác nhận chốt bảng' })).toBeEnabled())
  fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận chốt bảng' }))
  await waitFor(() => expect(api.calls('POST', '/costs/statements/31/finalize')).toHaveLength(2))
  expect(api.calls('POST', '/costs/statements/31/finalize')[0][1].headers['If-Match']).toBe('"3"')
  expect(api.calls('POST', '/costs/statements/31/finalize')[1][1].headers['If-Match']).toBe('"9"')
})

it('FR_017_ke_toan_chi_xem_khong_co_lenh_tai_chinh', async () => {
  permissions(false, true)
  await openStatement()
  expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
  expect(
    screen.queryByRole('button', { name: /Kiểm tra các khoản|Duyệt các khoản|Chốt bảng|Gửi chốt/ }),
  ).not.toBeInTheDocument()
})

it('FR_018_xem_lich_su_kiem_tra_cua_tung_khoan', async () => {
  await openStatement()
  fireEvent.click(screen.getByRole('button', { name: 'Xem khoản #3' }))
  expect(await screen.findByText('Tài khoản #70')).toBeInTheDocument()
  expect(screen.getByText('02/11/2026 08:00')).toBeInTheDocument()
})

it('FR_024_duyet_rieng_khoan_vuot_bat_buoc_ly_do', async () => {
  permissions(true)
  statement = { ...statement, items: [{ ...manual, origin: 'PURCHASE_ASSISTANCE', flags: ['URGENT_EXCESS'] }] }
  api.on('POST', '/costs/items/approve', json(200, { succeededIds: [3], skipped: [] }))
  await openStatement()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Chọn khoản #3' }))
  fireEvent.click(screen.getByRole('button', { name: 'Duyệt các khoản đã chọn' }))
  const dialog = screen.getByRole('dialog', { name: 'Xác nhận duyệt chi phí' })
  fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận duyệt' }))
  expect(api.calls('POST', '/costs/items/approve')).toHaveLength(0)
  fireEvent.change(within(dialog).getByLabelText('Lý do chấp nhận phần vượt (bắt buộc)'), {
    target: { value: 'Có chứng từ hợp lệ' },
  })
  fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận duyệt' }))
  await waitFor(() => expect(api.calls('POST', '/costs/items/approve')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/costs/items/approve')[0][1].body)).toEqual({
    itemIds: [3],
    excessApprovalReason: 'Có chứng từ hợp lệ',
  })
})

it('FR_043_403_khong_giu_du_lieu_tren_man', async () => {
  api.on('GET', '/costs/periods', problem(403, 'PERMISSION_DENIED'))
  render(
    <PermissionsProvider>
      <ChargesPage />
    </PermissionsProvider>,
  )
  expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument()
  expect(screen.queryByLabelText('Kỳ chi phí')).not.toBeInTheDocument()
})

it('FR_015_kiem_tra_theo_bo_loc_gioi_han_trong_bang_dang_mo', async () => {
  api.on('POST', '/costs/items/check', json(200, { succeededIds: [], skipped: [] }))
  await openStatement()
  fireEvent.change(screen.getByLabelText('Nguồn khoản'), { target: { value: 'EVENT_BASED' } })
  fireEvent.change(screen.getByLabelText('Cờ cảnh báo'), { target: { value: 'MISSING_UNIT_PRICE' } })
  fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra theo bộ lọc' }))
  fireEvent.click(
    within(screen.getByRole('dialog', { name: 'Xác nhận kiểm tra chi phí' })).getByRole('button', {
      name: 'Xác nhận kiểm tra',
    }),
  )
  await waitFor(() => expect(api.calls('POST', '/costs/items/check')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/costs/items/check')[0][1].body)).toEqual({
    filter: { statementId: 31, origin: 'EVENT_BASED', flags: 'MISSING_UNIT_PRICE' },
  })
})

it('FR_032_gui_chot_hien_canh_bao_ky_truoc_bang_khong_va_ba_loai', async () => {
  api.on(
    'POST',
    '/costs/statements/31/submit-for-finalization',
    json(200, { changeWarning: { topThreeIncreases: ['SERVICE', 'FAMILY_MEAL'] } }),
  )
  await openStatement()
  fireEvent.click(screen.getByRole('button', { name: 'Gửi chốt' }))
  fireEvent.click(
    within(screen.getByRole('dialog', { name: 'Xác nhận gửi chốt bảng' })).getByRole('button', {
      name: 'Xác nhận gửi chốt',
    }),
  )
  expect(await screen.findByText('Chi phí tăng so với kỳ trước từ tổng bằng 0 đồng.')).toBeInTheDocument()
  expect(screen.queryByText(/NaN|Infinity|undefined/)).not.toBeInTheDocument()
  expect(api.calls('POST', '/costs/statements/31/finalize')).toHaveLength(0)
})

it('DBR_17_bang_da_chot_chi_doc_du_co_quyen_cap_nhat', async () => {
  statement = { ...statement, status: 'FINALIZED' }
  await openStatement()
  expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: /Kiểm tra các khoản|Gửi chốt/ })).not.toBeInTheDocument()
})
