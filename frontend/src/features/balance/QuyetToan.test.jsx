import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { SettlementPanel } from './SettlementPanel'

// Quyết toán: hai điều kiện, Xác nhận quyết toán, phiên bản, tải PDF, bản ký (017 T033; FR-022, FR-034, FR-037,
// BR-M11-22, DBR-38; quickstart Q16 → Q21).

const NOT_MET = {
  conditions: [
    { code: 'END_H_SETTLEMENT', status: 'NOT_MET', waivable: true, reasons: ['Số dư cọc còn 3.000.000 đ'] },
    {
      code: 'END_H_SETTLEMENT_CONFIRMATION',
      status: 'NOT_MET',
      waivable: false,
      reasons: ['Còn 2 giao dịch Chờ duyệt'],
    },
  ],
  hasException: false,
  versions: [],
}
const MET = {
  conditions: [
    { code: 'END_H_SETTLEMENT', status: 'MET', waivable: true, reasons: [] },
    { code: 'END_H_SETTLEMENT_CONFIRMATION', status: 'MET', waivable: false, reasons: [] },
  ],
  hasException: false,
  versions: [
    {
      id: 2,
      version: 2,
      current: true,
      hasException: false,
      debtOrRefundAmount: 0,
      confirmedBy: 'Kế toán A',
      confirmedAt: '2026-11-05T03:00:00Z',
      hasSignedCopy: false,
    },
    {
      id: 1,
      version: 1,
      current: false,
      hasException: false,
      debtOrRefundAmount: 0,
      confirmedBy: 'Kế toán A',
      confirmedAt: '2026-11-01T03:00:00Z',
      hasSignedCopy: true,
    },
  ],
}
const ACCOUNTANT = [
  { function: 'SETTLEMENT_RECORD', operation: 'VIEW' },
  { function: 'SETTLEMENT_RECORD', operation: 'CREATE' },
  { function: 'SETTLEMENT_RECORD', operation: 'UPDATE' },
]

function setup(settlement, perms = ACCOUNTANT) {
  const api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, perms))
  api.on('GET', '/residents/7/settlement', json(200, settlement))
  vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() }))
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  render(
    <PermissionsProvider>
      <SettlementPanel residentId={7} />
    </PermissionsProvider>,
  )
  return api
}

describe('SettlementPanel (017)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('FR_022_haiDieuKien_hienDatChuaDatKemLyDoVaKhongMienDuocYXacNhan', async () => {
    setup(NOT_MET)
    expect(await screen.findByText('Số dư cọc còn 3.000.000 đ')).toBeInTheDocument()
    expect(screen.getByText('Còn 2 giao dịch Chờ duyệt')).toBeInTheDocument()
    expect(screen.getAllByText('Chưa đạt')).toHaveLength(2)
    expect(screen.getByText('Miễn được bằng ngoại lệ')).toBeInTheDocument()
    expect(screen.getByText('Không miễn được')).toBeInTheDocument()
  })

  it('BR_M11_22_xacNhanQuyetToan_guiLenhRoiTaiLaiPhienBan', async () => {
    const api = setup(MET)
    let confirmed = false
    api.on('POST', '/residents/7/settlement/confirm', () => {
      confirmed = true
      return json(201, { id: 3, version: 3, current: true })
    })
    api.on('GET', '/residents/7/settlement', () =>
      json(200, confirmed ? { ...MET, versions: [{ ...MET.versions[0], id: 3, version: 3 }, ...MET.versions] } : MET),
    )
    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận quyết toán' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận' }))
    await waitFor(() => expect(api.calls('POST', '/residents/7/settlement/confirm')).toHaveLength(1))
    expect(api.calls('POST', '/residents/7/settlement/confirm')[0][1].headers['Idempotency-Key']).toMatch(
      /^[0-9a-f-]{36}$/,
    )
    expect(await screen.findByText('Phiên bản 3')).toBeInTheDocument()
  })

  it('BR_M11_22_chuaDuDieuKien_409_hienLyDo', async () => {
    const api = setup(MET)
    api.on(
      'POST',
      '/residents/7/settlement/confirm',
      problem(409, 'SETTLEMENT_CONDITIONS_NOT_MET', { detail: 'Còn giao dịch Chờ duyệt.' }),
    )
    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận quyết toán' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận' }))
    expect(await screen.findByText('Còn giao dịch Chờ duyệt.')).toBeInTheDocument()
  })

  it('DBR_38_phienBanHienHanhNoiBat_taiPdfDungPhienBan', async () => {
    const api = setup(MET)
    api.on(
      'GET',
      '/settlement-statements/2/file',
      () => new Response(new Blob(['pdf']), { status: 200, headers: { 'Content-Type': 'application/pdf' } }),
    )
    const current = (await screen.findByText('Phiên bản 2')).closest('li')
    expect(within(current).getByText('Hiện hành')).toBeInTheDocument()
    const old = screen.getByText('Phiên bản 1').closest('li')
    expect(within(old).queryByText('Hiện hành')).not.toBeInTheDocument()
    expect(within(old).getByText('Đã có bản ký')).toBeInTheDocument()
    fireEvent.click(within(current).getByRole('button', { name: 'Tải PDF phiên bản 2' }))
    await waitFor(() => expect(api.calls('GET', '/settlement-statements/2/file')).toHaveLength(1))
  })

  it('FR_034_dinhKemBanKy_taiTepRoiGuiMaTepChoPhienBanHienHanh', async () => {
    const api = setup(MET)
    api.on('POST', '/files', json(201, { fileId: 88 }))
    api.on('POST', '/settlement-statements/2/signed-copy', json(200, {}))
    const current = (await screen.findByText('Phiên bản 2')).closest('li')
    const file = new File(['x'], 'ban-ky.pdf', { type: 'application/pdf' })
    fireEvent.change(within(current).getByLabelText('Bản ký phiên bản 2'), { target: { files: [file] } })
    fireEvent.click(within(current).getByRole('button', { name: 'Đính kèm bản ký' }))
    await waitFor(() => expect(api.calls('POST', '/settlement-statements/2/signed-copy')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/settlement-statements/2/signed-copy')[0][1].body)).toEqual({ fileId: 88 })
  })

  it('FR_028_khongCoQuyenThucHien_anXacNhanVaDinhKem', async () => {
    setup(MET, [{ function: 'SETTLEMENT_RECORD', operation: 'VIEW' }])
    expect(await screen.findByText('Phiên bản 2')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Xác nhận quyết toán' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Đính kèm bản ký' })).not.toBeInTheDocument()
  })
})
