import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { LiveCostsPage } from './LiveCostsPage'

// 017 T011 phần cổng (FR-003; quickstart Q2): số dư, cọc, giao dịch Đã xác nhận, thông tin nộp tiền và mã QR VietQR
// (không số tiền); thanh toán bảng chỉ có tổng; không có quyền xem chi phí thì báo, không hiện số.

const viewer = { residentId: 41, residentName: 'Nguyễn Thị Lan', perms: { costs: true } }

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('LiveCostsPage', () => {
  it('FR_003_soDu_thongTinNopTien_maQr_giaoDich', async () => {
    api.on(
      'GET',
      '/family-portal/residents/41/balance',
      json(200, {
        balance: -9_100_000,
        depositBalance: 10_000_000,
        debt: 9_100_000,
        transactions: [
          { id: 3, type: 'STATEMENT_PAYMENT', ledger: 'BALANCE', amount: -9_300_000, at: '2026-11-01T01:00:00Z' },
          { id: 2, type: 'CASH_PAYMENT', ledger: 'BALANCE', amount: 200_000, at: '2026-10-20T02:00:00Z' },
        ],
        paymentInstructions: {
          bank: 'Vietcombank',
          accountNumber: '0011004000123',
          accountHolder: 'Viện dưỡng lão An Bình',
          paymentCode: 'NCT7K2M9XA',
          vietQr: '00020101021138570010A000000727',
        },
      }),
    )
    render(<LiveCostsPage viewer={viewer} />)
    expect(await screen.findByText('Còn nợ 9.100.000 đồng')).toBeInTheDocument()
    expect(screen.getByText('10.000.000 đồng')).toBeInTheDocument()
    expect(screen.getByText('NCT7K2M9XA')).toBeInTheDocument()
    expect(screen.getByText('0011004000123')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: /Mã QR chuyển khoản/ })).toBeInTheDocument()
    expect(screen.getByText('Thanh toán bảng chi phí')).toBeInTheDocument()
    expect(screen.getByText('-9.300.000 đồng')).toBeInTheDocument()
  })

  it('FR_003_chuaCoTaiKhoanThu_khongCoMucNopTien', async () => {
    api.on('GET', '/family-portal/residents/41/balance', json(200, { balance: 0, depositBalance: 0, transactions: [] }))
    render(<LiveCostsPage viewer={viewer} />)
    expect(await screen.findByText('Liên hệ Kế toán để biết tài khoản nộp tiền.')).toBeInTheDocument()
    expect(screen.queryByRole('img', { name: /Mã QR/ })).not.toBeInTheDocument()
  })

  it('FR_003_khongCoQuyenXemChiPhi_403', async () => {
    api.on('GET', '/family-portal/residents/41/balance', problem(403, 'PERMISSION_DENIED'))
    render(<LiveCostsPage viewer={viewer} />)
    expect(await screen.findByText(/chưa có quyền xem chi phí/)).toBeInTheDocument()
  })
})
