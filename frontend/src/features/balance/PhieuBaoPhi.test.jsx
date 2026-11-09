import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { ResidentNotices } from './ResidentNotices'

// Phiếu báo phí đã phát hành của một người cao tuổi: tải lại đúng file PDF, Excel (017 T036; FR-037, BR-M11-23;
// quickstart Q26, Q27).

const NOTICES = [
  {
    id: 5,
    noticeCode: 'PBP-202610-42',
    costStatementId: 42,
    period: '10/2026',
    statementType: 'REGULAR',
    total: 9250000,
    balanceBefore: 8000000,
    balanceAfter: -1250000,
    issuedBy: 'Kế toán A',
    issuedAt: '2026-11-03T03:00:00Z',
  },
]

describe('ResidentNotices (017)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('BR_M11_23_taiPdfVaExcel_goiDungDinhDangCuaPhieu', async () => {
    const api = mockRoutes()
    api.on('GET', '/residents/7/payment-notices', json(200, NOTICES))
    api.on(
      'GET',
      '/payment-notices/5/file',
      () => new Response(new Blob(['f']), { status: 200, headers: { 'Content-Type': 'application/pdf' } }),
    )
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() }))
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    render(
      <PermissionsProvider>
        <ResidentNotices residentId={7} />
      </PermissionsProvider>,
    )
    const row = (await screen.findByText('PBP-202610-42')).closest('tr')
    expect(within(row).getByText('Bảng thường')).toBeInTheDocument()
    expect(within(row).getByText('9.250.000 đ')).toBeInTheDocument()
    expect(within(row).getByText('-1.250.000 đ')).toBeInTheDocument()
    fireEvent.click(within(row).getByRole('button', { name: 'Tải PDF phiếu PBP-202610-42' }))
    await waitFor(() => expect(api.calls('GET', '/payment-notices/5/file')).toHaveLength(1))
    expect(new URL(api.calls('GET', '/payment-notices/5/file')[0][0], 'http://x').searchParams.get('format')).toBe(
      'PDF',
    )
    fireEvent.click(within(row).getByRole('button', { name: 'Tải Excel phiếu PBP-202610-42' }))
    await waitFor(() => expect(api.calls('GET', '/payment-notices/5/file')).toHaveLength(2))
    expect(new URL(api.calls('GET', '/payment-notices/5/file')[1][0], 'http://x').searchParams.get('format')).toBe(
      'XLSX',
    )
  })

  it('FR_031_chuaCoPhieu_hienTrangThaiRong', async () => {
    const api = mockRoutes()
    api.on('GET', '/residents/7/payment-notices', json(200, []))
    render(
      <PermissionsProvider>
        <ResidentNotices residentId={7} />
      </PermissionsProvider>,
    )
    expect(await screen.findByText('Chưa có phiếu báo phí')).toBeInTheDocument()
  })
})
