import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PaymentRecording } from './PaymentRecording'

// 017 T015 (US2, US3; FR-005 → FR-007; quickstart Q4 → Q6): tìm hồ sơ theo mã nộp tiền, ghi chuyển khoản hai dòng cùng
// mã, cảnh báo trùng mã giao dịch ngân hàng rồi ghi lại khi Kế toán xác nhận, ghi tiền mặt kèm ghi chú.

const BALANCE = {
  residentId: 41,
  paymentCode: 'NCT7K2M9XA',
  balance: 0,
  depositBalance: 0,
  status: 'NORMAL',
  depositStatus: 'NOT_SATISFIED',
}

let api

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/balances/by-payment-code', json(200, BALANCE))
  api.on('GET', '/residents/41', json(200, { id: 41, fullName: 'Nguyễn Thị Lan' }))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function type(label, value) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

function body(call) {
  return JSON.parse(call[1].body)
}

async function find() {
  render(<PaymentRecording />)
  type('Mã nộp tiền', 'nct7k2-m9xa')
  fireEvent.click(screen.getByRole('button', { name: 'Tìm hồ sơ' }))
  await screen.findByText('Nguyễn Thị Lan')
}

describe('PaymentRecording', () => {
  it('US3_1_timTheoMa_ghiChuyenKhoanHaiDongCungMa', async () => {
    api.on(
      'POST',
      '/residents/41/bank-transfer-payments',
      json(201, {
        transactions: [],
        balance: { ...BALANCE, balance: 5_000_000, depositBalance: 20_000_000, depositStatus: 'SATISFIED' },
      }),
    )
    await find()
    expect(api.calls('GET', '/balances/by-payment-code')[0][0]).toContain('code=nct7k2-m9xa')

    type('Số tiền vào sổ cọc', '20000000')
    type('Số tiền vào số dư', '5000000')
    type('Mã giao dịch ngân hàng', 'NH002')
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận chuyển khoản' }))

    await screen.findByText(/Đã ghi nhận/)
    const [call] = api.calls('POST', '/residents/41/bank-transfer-payments')
    expect(body(call)).toMatchObject({
      lines: [
        { ledger: 'DEPOSIT', amount: 20_000_000 },
        { ledger: 'BALANCE', amount: 5_000_000 },
      ],
      bankTransactionCode: 'NH002',
      confirmDuplicateCode: false,
    })
    expect(call[1].headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(screen.getByText('Đã đáp ứng')).toBeInTheDocument()
  })

  it('US3_3_trungMaNganHang_hienGiaoDichTruoc_vaGhiLaiKhiXacNhan', async () => {
    let attempt = 0
    api.on('POST', '/residents/41/bank-transfer-payments', () => {
      attempt += 1
      return attempt === 1
        ? problem(409, 'BANK_TRANSACTION_CODE_USED', {
            duplicateTransactions: [
              { id: 7, ledger: 'DEPOSIT', type: 'DEPOSIT_RECEIPT', amount: 10_000_000, at: '2026-11-05T03:00:00Z' },
            ],
          })
        : json(201, { transactions: [], balance: { ...BALANCE, balance: 3_000_000 } })
    })
    await find()
    type('Số tiền vào số dư', '3000000')
    type('Mã giao dịch ngân hàng', 'NH001')
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận chuyển khoản' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/10.000.000 đồng/)).toBeInTheDocument()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Vẫn ghi nhận' }))

    await screen.findByText(/Đã ghi nhận/)
    const calls = api.calls('POST', '/residents/41/bank-transfer-payments')
    expect(calls).toHaveLength(2)
    expect(body(calls[1]).confirmDuplicateCode).toBe(true)
    expect(calls[1][1].headers['Idempotency-Key']).not.toBe(calls[0][1].headers['Idempotency-Key'])
  })

  it('US2_4_tienMat_kemGhiChu_soTienPhaiLonHon0', async () => {
    api.on(
      'POST',
      '/residents/41/cash-payments',
      json(201, { transactions: [], balance: { ...BALANCE, balance: 5_000_000 } }),
    )
    await find()
    fireEvent.click(screen.getByRole('button', { name: 'Tiền mặt' }))
    type('Số tiền', '0')
    type('Người nộp', 'Lê Văn Hải')
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận tiền mặt' }))
    expect(await screen.findByText('Nhập số tiền lớn hơn 0')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/41/cash-payments')).toHaveLength(0)

    type('Số tiền', '5000000')
    type('Ghi chú', 'PT 0123')
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận tiền mặt' }))
    await waitFor(() => expect(api.calls('POST', '/residents/41/cash-payments')).toHaveLength(1))
    expect(body(api.calls('POST', '/residents/41/cash-payments')[0])).toMatchObject({
      ledger: 'BALANCE',
      amount: 5_000_000,
      payerName: 'Lê Văn Hải',
      note: 'PT 0123',
    })
  })

  it('FR_007_maSai_baoKhongTimThay', async () => {
    api.on('GET', '/balances/by-payment-code', problem(404, 'NOT_FOUND'))
    render(<PaymentRecording />)
    type('Mã nộp tiền', 'NCT0000001')
    fireEvent.click(screen.getByRole('button', { name: 'Tìm hồ sơ' }))
    expect(await screen.findByText(/Không tìm thấy hồ sơ với mã này/)).toBeInTheDocument()
  })
})
