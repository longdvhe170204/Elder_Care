import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ResidentBalance } from './ResidentBalance'

// Giao dịch của một người cao tuổi: hoàn tiền, điều chỉnh, cấn trừ cọc (017 T027; FR-010 → FR-015, BR-M11-10, 14;
// quickstart Q11 → Q15).

const BALANCE = {
  residentId: 7,
  paymentCode: 'NCT7K2M9X4A',
  balance: -1250000,
  depositBalance: 3000000,
  debt: 1250000,
  status: 'IN_DEBT',
  daysCovered: null,
  unfinalizedTotal: 400000,
  depositStatus: 'SATISFIED',
  remindersStopped: false,
  hiddenFields: [],
}
const TRANSACTIONS = [
  {
    id: 1,
    ledger: 'BALANCE',
    type: 'CASH_PAYMENT',
    amount: 2000000,
    at: '2026-10-05T01:30:00Z',
    method: 'CASH',
    createdBy: 'Kế toán A',
    status: 'CONFIRMED',
    adjusted: false,
  },
  {
    id: 2,
    ledger: 'BALANCE',
    type: 'STATEMENT_PAYMENT',
    amount: -3250000,
    at: '2026-10-31T15:00:00Z',
    method: 'INTERNAL',
    createdBy: 'Hệ thống',
    status: 'CONFIRMED',
    adjusted: false,
  },
  {
    id: 3,
    ledger: 'BALANCE',
    type: 'REFUND',
    amount: -500000,
    at: '2026-11-01T02:00:00Z',
    method: 'BANK_TRANSFER',
    createdBy: 'Kế toán A',
    status: 'PENDING_APPROVAL',
    adjusted: false,
  },
]
const ACCOUNTANT = [
  { function: 'BALANCE_LEDGER', operation: 'VIEW' },
  { function: 'BALANCE_LEDGER', operation: 'CREATE' },
]

function setup({ perms = ACCOUNTANT, balance = BALANCE } = {}) {
  const api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, perms))
  api.on('GET', '/residents/7/balance', json(200, balance))
  api.on('GET', '/residents/7/balance-transactions', json(200, TRANSACTIONS))
  api.on('GET', '/residents/7/settlement', json(200, { conditions: [], hasException: false, versions: [] }))
  api.on('GET', '/residents/7/payment-notices', json(200, []))
  render(
    <PermissionsProvider>
      <ResidentBalance residentId={7} fullName="Nguyễn Thị Ánh" />
    </PermissionsProvider>,
  )
  return api
}

const fill = (label, value) => fireEvent.change(screen.getByLabelText(label), { target: { value } })

describe('ResidentBalance (017)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('FR_002_hienSoDuCocNoMaNopTienVaTinhTrang', async () => {
    setup()
    expect(await screen.findByText('NCT7K2M9X4A')).toBeInTheDocument()
    expect(screen.getByText('-1.250.000 đ')).toBeInTheDocument()
    expect(screen.getByText('3.000.000 đ')).toBeInTheDocument()
    expect(screen.getByText('Còn nợ')).toBeInTheDocument()
    expect(screen.getByText('Còn nợ 1.250.000 đ')).toBeInTheDocument()
  })

  it('FR_010_bangGiaoDich_hienLoaiSoTienVaTrangThai', async () => {
    setup()
    const refund = (await screen.findByText('Hoàn tiền')).closest('tr')
    expect(within(refund).getByText('Chờ duyệt')).toBeInTheDocument()
    expect(within(refund).getByText('-500.000 đ')).toBeInTheDocument()
    const cash = screen.getByText('Nộp tiền mặt').closest('tr')
    expect(within(cash).getByText('Đã xác nhận')).toBeInTheDocument()
  })

  it('FR_004_hanhChinhChiXemSoDu_anCacTruongBiAnVaNutLenh', async () => {
    setup({
      perms: [{ function: 'BALANCE_LEDGER', operation: 'VIEW' }],
      balance: { ...BALANCE, paymentCode: null, status: null, hiddenFields: ['paymentCode', 'status', 'daysCovered'] },
    })
    expect(await screen.findByText('3.000.000 đ')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Lập hoàn tiền/ })).not.toBeInTheDocument()
    expect(screen.queryByText('Mã nộp tiền')).not.toBeInTheDocument()
  })

  it('FR_014_lapHoanTien_guiNguoiNhanLyDoVaIdempotencyKey', async () => {
    const api = setup()
    api.on('POST', '/balance-transactions', json(201, { id: 9, status: 'PENDING_APPROVAL' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Lập hoàn tiền, điều chỉnh' }))
    fill('Loại giao dịch', 'REFUND')
    fill('Số tiền', '1000000')
    fill('Mã người thân (người đại diện)', '55')
    fill('Phương thức', 'BANK_TRANSFER')
    fill('Lý do', 'Gia đình xin rút bớt tiền')
    fireEvent.click(screen.getByRole('button', { name: 'Gửi duyệt' }))
    await waitFor(() => expect(api.calls('POST', '/balance-transactions')).toHaveLength(1))
    const init = api.calls('POST', '/balance-transactions')[0][1]
    expect(JSON.parse(init.body)).toEqual({
      type: 'REFUND',
      residentId: 7,
      ledger: 'BALANCE',
      amount: 1000000,
      recipient: { familyMemberId: 55 },
      method: 'BANK_TRANSFER',
      reason: 'Gia đình xin rút bớt tiền',
    })
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(await screen.findByText(/Đã gửi duyệt/)).toBeInTheDocument()
  })

  it('FR_014_hoanCoc_ghiVaoSoCoc_nguoiNhanKhacCanHoTenVaGiayTo', async () => {
    const api = setup()
    api.on('POST', '/balance-transactions', json(201, { id: 10, status: 'PENDING_APPROVAL' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Lập hoàn tiền, điều chỉnh' }))
    fill('Loại giao dịch', 'DEPOSIT_REFUND')
    fill('Số tiền', '500000')
    fill('Lý do', 'Hủy hợp đồng')
    fireEvent.click(screen.getByRole('button', { name: 'Gửi duyệt' }))
    expect(
      await screen.findByText('Nhập mã người thân, hoặc họ tên và giấy tờ của người nhận khác.'),
    ).toBeInTheDocument()
    expect(api.calls('POST', '/balance-transactions')).toHaveLength(0)
    fill('Họ tên người nhận khác', 'Trần Văn Khác')
    fill('Giấy tờ người nhận khác', '012345678901')
    fireEvent.click(screen.getByRole('button', { name: 'Gửi duyệt' }))
    await waitFor(() => expect(api.calls('POST', '/balance-transactions')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/balance-transactions')[0][1].body)).toMatchObject({
      type: 'DEPOSIT_REFUND',
      ledger: 'DEPOSIT',
      recipient: { fullName: 'Trần Văn Khác', identityDocument: '012345678901' },
    })
  })

  it('BR_M11_14_vuotGioiHanHoan_409_hienLoiTrongForm', async () => {
    const api = setup()
    api.on(
      'POST',
      '/balance-transactions',
      problem(409, 'REFUND_LIMIT_EXCEEDED', { detail: 'Vượt giới hạn hoàn: tối đa 1.000.000 đ.' }),
    )
    fireEvent.click(await screen.findByRole('button', { name: 'Lập hoàn tiền, điều chỉnh' }))
    fill('Loại giao dịch', 'REFUND')
    fill('Số tiền', '3000000')
    fill('Mã người thân (người đại diện)', '55')
    fill('Lý do', 'Xin rút')
    fireEvent.click(screen.getByRole('button', { name: 'Gửi duyệt' }))
    expect(await screen.findByText('Vượt giới hạn hoàn: tối đa 1.000.000 đ.')).toBeInTheDocument()
  })

  it('BR_M11_10_dieuChinh_choPhepSoAm_vaTroGiaoDichGoc', async () => {
    const api = setup()
    api.on('POST', '/balance-transactions', json(201, { id: 11 }))
    fireEvent.click(await screen.findByRole('button', { name: 'Lập hoàn tiền, điều chỉnh' }))
    fill('Loại giao dịch', 'ADJUSTMENT')
    fill('Sổ', 'BALANCE')
    fill('Số tiền', '-200000')
    fill('Mã giao dịch gốc', '1')
    fill('Lý do', 'Ghi nhầm hồ sơ')
    fireEvent.click(screen.getByRole('button', { name: 'Gửi duyệt' }))
    await waitFor(() => expect(api.calls('POST', '/balance-transactions')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/balance-transactions')[0][1].body)).toEqual({
      type: 'ADJUSTMENT',
      residentId: 7,
      ledger: 'BALANCE',
      amount: -200000,
      originalTransactionId: 1,
      reason: 'Ghi nhầm hồ sơ',
    })
  })

  it('FR_015_canTruCoc_guiSoTienVaTaiLaiSoDu', async () => {
    const api = setup()
    api.on('POST', '/residents/7/deposit-offsets', json(201, [{ id: 20 }, { id: 21 }]))
    fireEvent.click(await screen.findByRole('button', { name: 'Cấn trừ tiền cọc' }))
    fill('Số tiền cấn trừ', '1250000')
    fireEvent.click(screen.getByRole('button', { name: 'Cấn trừ' }))
    await waitFor(() => expect(api.calls('POST', '/residents/7/deposit-offsets')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/7/deposit-offsets')[0][1].body)).toEqual({ amount: 1250000 })
    await waitFor(() => expect(api.calls('GET', '/residents/7/balance').length).toBeGreaterThan(1))
  })

  it('FR_015_canTruCoc_409_dieuKienChuaDu_hienLoi', async () => {
    const api = setup()
    api.on(
      'POST',
      '/residents/7/deposit-offsets',
      problem(409, 'OFFSET_CONDITIONS_NOT_MET', { detail: 'Chưa có hồ sơ kết thúc lưu trú.' }),
    )
    fireEvent.click(await screen.findByRole('button', { name: 'Cấn trừ tiền cọc' }))
    fill('Số tiền cấn trừ', '100')
    fireEvent.click(screen.getByRole('button', { name: 'Cấn trừ' }))
    expect(await screen.findByText('Chưa có hồ sơ kết thúc lưu trú.')).toBeInTheDocument()
  })
})
