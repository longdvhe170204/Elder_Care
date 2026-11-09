import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { BalanceStatuses } from './BalanceStatuses'

// 017 T022 (US5; FR-016 → FR-020): danh sách Sắp hết tiền, Còn nợ; Quản lý viện dừng nhắc có lý do cho hồ sơ trạng
// thái cuối còn nợ, backend chặn hồ sơ chưa ở trạng thái cuối.

const ROWS = [
  {
    residentId: 41,
    paymentCode: 'NCT7K2M9XA',
    balance: 1_200_000,
    status: 'RUNNING_LOW',
    daysCovered: 4,
    remindersStopped: false,
  },
  {
    residentId: 42,
    paymentCode: 'NCT123456Z',
    balance: -900_000,
    debt: 900_000,
    status: 'IN_DEBT',
    remindersStopped: false,
  },
]

let api

function permissions(keys) {
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      keys.map(([fn, op]) => ({ function: fn, operation: op })),
    ),
  )
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/balance-statuses', json(200, ROWS))
  api.on('GET', '/residents/41', json(200, { id: 41, fullName: 'Nguyễn Thị Lan' }))
  api.on('GET', '/residents/42', json(200, { id: 42, fullName: 'Trần Văn Minh' }))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('BalanceStatuses', () => {
  it('FR_017_lietKeSapHetTien_ConNo_locTheoTinhTrang', async () => {
    permissions([['BALANCE_LEDGER', 'VIEW']])
    render(
      <PermissionsProvider>
        <BalanceStatuses />
      </PermissionsProvider>,
    )
    const table = await screen.findByRole('table')
    expect(within(table).getByText('Nguyễn Thị Lan')).toBeInTheDocument()
    expect(within(table).getByText('Còn nợ 900.000 đồng')).toBeInTheDocument()
    expect(within(table).getByText('4 ngày')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Dừng nhắc/ })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Còn nợ' }))
    await waitFor(() =>
      expect(api.calls('GET', '/balance-statuses').some(([url]) => url.includes('status=IN_DEBT'))).toBe(true),
    )
  })

  it('FR_020_quanLyVienDungNhac_batBuocLyDo_va409TuBackend', async () => {
    permissions([
      ['BALANCE_LEDGER', 'VIEW'],
      ['BALANCE_LEDGER', 'APPROVE'],
    ])
    api.on(
      'POST',
      '/residents/42/balance/stop-reminders',
      problem(409, 'NOT_FINAL_STATUS', { detail: 'Chỉ dừng nhắc cho hồ sơ đã ở trạng thái cuối (FR-020)' }),
    )
    render(
      <PermissionsProvider>
        <BalanceStatuses />
      </PermissionsProvider>,
    )
    fireEvent.click(await screen.findByRole('button', { name: 'Dừng nhắc Trần Văn Minh' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Dừng nhắc' }))
    expect(await within(dialog).findByText('Nhập lý do dừng nhắc')).toBeInTheDocument()

    fireEvent.change(within(dialog).getByLabelText('Lý do'), { target: { value: 'Gia đình không còn liên lạc' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Dừng nhắc' }))
    expect(await within(dialog).findByText(/trạng thái cuối/)).toBeInTheDocument()
    const [call] = api.calls('POST', '/residents/42/balance/stop-reminders')
    expect(JSON.parse(call[1].body)).toEqual({ reason: 'Gia đình không còn liên lạc' })
  })
})
