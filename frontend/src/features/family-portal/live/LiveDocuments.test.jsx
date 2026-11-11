import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { LiveDocuments } from './LiveDocuments'

// 012 T073 (FR-046 (3), Q-377 → Q-379; 017 FR-026, FR-027, FR-038; K12): mục chứng từ trên cổng web — phiếu báo phí,
// bảng quyết toán tải đúng file đã lưu theo định dạng; sao kê theo khoảng ngày; không có quyền xem chi phí thì ẩn.

let api

const file = () => new Response(new Blob(['%PDF']), { status: 200, headers: { 'Content-Type': 'application/pdf' } })

beforeEach(() => {
  api = mockRoutes()
  URL.createObjectURL = vi.fn(() => 'blob:x')
  URL.revokeObjectURL = vi.fn()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('LiveDocuments', () => {
  it('K12_danhSachChungTu_taiDungFileTheoDinhDang', async () => {
    api.on(
      'GET',
      '/family-portal/residents/41/documents',
      json(200, [
        {
          type: 'PAYMENT_NOTICE',
          id: 7,
          code: 'PBP-2026-10-041',
          date: '2026-11-01',
          period: '10/2026',
          formats: ['PDF', 'XLSX'],
        },
        { type: 'SETTLEMENT_STATEMENT', id: 9, code: 'v2', date: '2026-11-05', period: null, formats: ['PDF'] },
      ]),
    )
    api.on('GET', '/family-portal/documents/PAYMENT_NOTICE/7/file', file)
    render(<LiveDocuments residentId={41} />)

    expect(await screen.findByText('Phiếu báo phí PBP-2026-10-041')).toBeInTheDocument()
    expect(screen.getByText('Bảng quyết toán v2')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Tải PBP-2026-10-041 dạng XLSX' }))
    await waitFor(() => expect(api.calls('GET', '/family-portal/documents/PAYMENT_NOTICE/7/file')).toHaveLength(1))
    const [url] = api.calls('GET', '/family-portal/documents/PAYMENT_NOTICE/7/file')[0]
    expect(new URL(url, 'http://localhost').searchParams.get('format')).toBe('XLSX')
  })

  it('FR_026_saoKeTheoKhoangNgay_guiIdempotencyKey', async () => {
    api.on('GET', '/family-portal/residents/41/documents', json(200, []))
    api.on('POST', '/family-portal/residents/41/balance-statement', file)
    render(<LiveDocuments residentId={41} />)

    expect(await screen.findByText('Chưa có phiếu báo phí hay bảng quyết toán nào.')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Từ ngày'), { target: { value: '2026-10-01' } })
    fireEvent.change(screen.getByLabelText('Đến ngày'), { target: { value: '2026-10-31' } })
    fireEvent.click(screen.getByRole('button', { name: 'Excel' }))
    fireEvent.click(screen.getByRole('button', { name: 'Tải sao kê' }))

    await waitFor(() => expect(api.calls('POST', '/family-portal/residents/41/balance-statement')).toHaveLength(1))
    const [, init] = api.calls('POST', '/family-portal/residents/41/balance-statement')[0]
    expect(JSON.parse(init.body)).toEqual({ from: '2026-10-01', to: '2026-10-31', format: 'XLSX' })
    expect(init.headers['Idempotency-Key']).toBeTruthy()
  })

  it('FR_046_khongCoQuyenXemChiPhi_anMucChungTu', async () => {
    api.on('GET', '/family-portal/residents/41/documents', problem(403, 'PERMISSION_DENIED'))
    const { container } = render(<LiveDocuments residentId={41} />)
    await waitFor(() => expect(api.calls('GET', '/family-portal/residents/41/documents')).toHaveLength(1))
    await waitFor(() => expect(container).toBeEmptyDOMElement())
  })
})
