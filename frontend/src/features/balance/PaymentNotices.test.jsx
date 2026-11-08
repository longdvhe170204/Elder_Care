import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PaymentNotices } from './PaymentNotices'

// Phiếu báo phí: bảng Đã chốt chờ phát hành (017 T036; FR-031, BR-M11-21, DBR-37; quickstart Q25 → Q27).

const PENDING = [
  {
    costStatementId: 42,
    residentId: 7,
    fullName: 'Nguyễn Thị Ánh',
    period: '10/2026',
    statementType: 'REGULAR',
    total: 9250000,
    finalizedAt: '2026-11-02T03:00:00Z',
  },
  {
    costStatementId: 43,
    residentId: 8,
    fullName: 'Lê Văn Bình',
    period: '11/2026',
    statementType: 'FINAL_PERIOD',
    total: 4620000,
    finalizedAt: '2026-11-05T03:00:00Z',
  },
]
const CAN = [{ function: 'PAYMENT_NOTICE', operation: 'CREATE' }]

function setup(perms = CAN, pending = PENDING) {
  const api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, perms))
  api.on('GET', '/payment-notices/pending-issue', json(200, pending))
  render(
    <PermissionsProvider>
      <PaymentNotices />
    </PermissionsProvider>,
  )
  return api
}

describe('PaymentNotices (017)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('FR_031_bangChoPhatHanh_hienNguoiKyLoaiBangVaTong', async () => {
    setup()
    const row = (await screen.findByText('Nguyễn Thị Ánh')).closest('tr')
    expect(within(row).getByText('10/2026')).toBeInTheDocument()
    expect(within(row).getByText('Bảng thường')).toBeInTheDocument()
    expect(within(row).getByText('9.250.000 đ')).toBeInTheDocument()
    const final = screen.getByText('Lê Văn Bình').closest('tr')
    expect(within(final).getByText('Bảng kỳ cuối')).toBeInTheDocument()
  })

  it('FR_031_phatHanh_guiIdempotencyKey_roiTaiLaiDanhSach', async () => {
    const api = setup()
    let issued = false
    api.on('POST', '/costs/statements/42/payment-notice', () => {
      issued = true
      return json(201, { id: 1, noticeCode: 'PBP-202610-42' })
    })
    api.on('GET', '/payment-notices/pending-issue', () => json(200, issued ? [PENDING[1]] : PENDING))
    fireEvent.click(await screen.findByRole('button', { name: 'Phát hành phiếu của Nguyễn Thị Ánh' }))
    await waitFor(() => expect(screen.queryByText('Nguyễn Thị Ánh')).not.toBeInTheDocument())
    expect(screen.getByText(/PBP-202610-42/)).toBeInTheDocument()
    const init = api.calls('POST', '/costs/statements/42/payment-notice')[0][1]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
  })

  it('BR_M11_21_daPhatHanh_409_baoLoiVaGiuDong', async () => {
    const api = setup()
    api.on(
      'POST',
      '/costs/statements/42/payment-notice',
      problem(409, 'ALREADY_ISSUED', { detail: 'Bảng này đã có phiếu.' }),
    )
    fireEvent.click(await screen.findByRole('button', { name: 'Phát hành phiếu của Nguyễn Thị Ánh' }))
    expect(await screen.findByText('Bảng này đã có phiếu.')).toBeInTheDocument()
    expect(screen.getByText('Nguyễn Thị Ánh')).toBeInTheDocument()
  })

  it('FR_028_khongCoQuyenLap_anNutPhatHanh', async () => {
    setup([{ function: 'PAYMENT_NOTICE', operation: 'VIEW' }])
    expect(await screen.findByText('Nguyễn Thị Ánh')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Phát hành phiếu/ })).not.toBeInTheDocument()
  })

  it('FR_031_khongCoBangChoPhatHanh_hienTrangThaiRong', async () => {
    setup(CAN, [])
    expect(await screen.findByText('Không có bảng chờ phát hành phiếu')).toBeInTheDocument()
  })
})
