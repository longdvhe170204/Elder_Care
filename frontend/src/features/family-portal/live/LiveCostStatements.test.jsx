import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { LiveCostStatements } from './LiveCostStatements'
import { LivePurchaseConsent } from './LivePurchaseConsent'

// 010 T031 (FR-037, FR-038, FR-019; BR-M10-10, FR-023a): cổng người thân có bảng chi phí Đã chốt, chi phí tạm tính
// (không chi tiết từng khoản, ghi chú khoản chưa có đơn giá) và đề nghị mua hộ chờ người đại diện quyết định.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const statement = {
  id: 1,
  period: '2026-10',
  statementType: 'REGULAR',
  total: 3_150_000,
  totalsByType: [
    { type: 'RESIDENCY_FEE', total: 3_000_000 },
    { type: 'MEDICATION', total: 150_000 },
  ],
  items: [
    {
      id: 2,
      costType: 'MEDICATION',
      description: 'Thuốc mã 77',
      amount: 150_000,
      incurredOn: '2026-10-01',
      quantity: 2,
      unit: 'viên',
      unitPrice: 75_000,
    },
  ],
}

describe('LiveCostStatements', () => {
  it('FR_037_bangDaChot_hienTongTheoLoai_vaKhoanThuocChiCoMa', async () => {
    api.on('GET', '/residents/41/costs/statements', json(200, [statement]))
    render(<LiveCostStatements residentId={41} />)
    expect(await screen.findByText('Kỳ 10/2026, bảng thường')).toBeInTheDocument()
    expect(screen.getByText('Thuốc mã 77')).toBeInTheDocument()
    expect(screen.getAllByText('Phí lưu trú').length).toBeGreaterThan(0)
    expect(screen.queryByText(/·/)).not.toBeInTheDocument()
  })

  it('FR_038_tamTinh_khongCoChiTiet_vaGhiChuThieuDonGia', async () => {
    api.on('GET', '/residents/41/costs/statements', json(200, []))
    api.on(
      'GET',
      '/residents/41/costs/provisional',
      json(200, { totalsByType: [{ type: 'SERVICE', total: 200_000 }], hasUnpricedItems: true }),
    )
    render(<LiveCostStatements residentId={41} />)
    expect(await screen.findByText('Chưa có bảng chi phí đã chốt.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Xem tạm tính' }))
    expect(await screen.findByText('Còn khoản chưa có đơn giá, chưa được cộng.')).toBeInTheDocument()
    expect(screen.getByText('Tạm tính, chưa chốt')).toBeInTheDocument()
  })
})

const request = {
  id: 9,
  residentId: 41,
  description: 'Máy đo đường huyết',
  quantity: 1,
  estimatedAmount: 800_000,
  status: 'AWAITING_CONSENT',
  insufficientBalance: true,
}

describe('LivePurchaseConsent', () => {
  it('BR_M10_10_dongY_sauKhiXemLai', async () => {
    api.on('GET', '/purchase-assistance-requests', json(200, [request]))
    api.on('POST', '/purchase-assistance-requests/9/agree', json(200, {}))
    render(<LivePurchaseConsent residentId={41} />)
    expect(await screen.findByText('Máy đo đường huyết')).toBeInTheDocument()
    expect(screen.getByText(/Số dư hiện không đủ/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Đồng ý đề nghị #9' }))
    expect(api.calls('POST', '/purchase-assistance-requests/9/agree')).toHaveLength(0)
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đồng ý' }))
    await waitFor(() => expect(api.calls('POST', '/purchase-assistance-requests/9/agree')).toHaveLength(1))
    expect(await screen.findByRole('status')).toHaveTextContent('Đã ghi quyết định')
  })

  it('FR_023_tuChoiCanLyDo_va409BaoDaThayDoi', async () => {
    api.on('GET', '/purchase-assistance-requests', json(200, [request]))
    api.on('POST', '/purchase-assistance-requests/9/reject', problem(409, 'ALREADY_PROCESSED'))
    render(<LivePurchaseConsent residentId={41} />)
    fireEvent.click(await screen.findByRole('button', { name: 'Từ chối đề nghị #9' }))
    expect(screen.getByRole('button', { name: 'Xác nhận từ chối' })).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Lý do từ chối'), { target: { value: 'Gia đình tự mua' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận từ chối' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Đề nghị đã thay đổi')
  })

  it('FR_023a_khongCoDeNghi_baoRo', async () => {
    api.on('GET', '/purchase-assistance-requests', json(200, []))
    render(<LivePurchaseConsent residentId={41} />)
    expect(await screen.findByText('Không có đề nghị mua hộ nào đang chờ bạn quyết định.')).toBeInTheDocument()
  })
})
