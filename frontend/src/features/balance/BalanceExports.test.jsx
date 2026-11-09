import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { BalanceExports } from './BalanceExports'

// Xuất sao kê và báo cáo thu chi (017 T039; FR-025, FR-027, Q-381, Q-386; quickstart Q29 → Q31).

const CAN = [{ function: 'STATEMENT_REPORT', operation: 'CREATE' }]
const RESIDENTS = {
  items: [{ id: 7, profileCode: 'HS-0007', personalInfo: { fullName: 'Nguyễn Thị Ánh' } }],
}

function setup(perms = CAN) {
  const api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, perms))
  api.on('GET', '/residents', json(200, RESIDENTS))
  vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() }))
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  render(
    <PermissionsProvider>
      <BalanceExports />
    </PermissionsProvider>,
  )
  return api
}

async function ready() {
  await screen.findByRole('button', { name: 'Xuất báo cáo thu chi' })
}

const fill = (label, value) => fireEvent.change(screen.getByLabelText(label), { target: { value } })

async function pickResident() {
  fireEvent.change(screen.getByLabelText('Tìm người cao tuổi'), { target: { value: 'Ánh' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tìm' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Ánh, HS-0007' }))
}

describe('BalanceExports (017)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('FR_025_saoKe_guiDanhSachHoSoKhoangNgayDinhDang_roiTaiFile', async () => {
    const api = setup()
    api.on(
      'POST',
      '/balance-exports/statement',
      json(201, {
        id: 9,
        type: 'BALANCE_STATEMENT',
        format: 'XLSX',
        from: '2026-10-01',
        to: '2026-10-31',
        rowCount: 2,
        at: '2026-11-01T03:00:00Z',
      }),
    )
    api.on(
      'GET',
      '/balance-exports/9/file',
      () => new Response(new Blob(['x']), { status: 200, headers: { 'Content-Type': 'application/octet-stream' } }),
    )
    await ready()
    await pickResident()
    fill('Từ ngày', '2026-10-01')
    fill('Đến ngày', '2026-10-31')
    fireEvent.click(screen.getByRole('button', { name: 'Xuất sao kê' }))
    expect(await screen.findByText('2 dòng')).toBeInTheDocument()
    const init = api.calls('POST', '/balance-exports/statement')[0][1]
    expect(JSON.parse(init.body)).toEqual({ residentIds: [7], from: '2026-10-01', to: '2026-10-31', format: 'XLSX' })
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    fireEvent.click(screen.getByRole('button', { name: /Tải lần xuất/ }))
    await waitFor(() => expect(api.calls('GET', '/balance-exports/9/file')).toHaveLength(1))
  })

  it('FR_025_saoKeChuaChonNguoi_baoLoiTaiOKhongGoiApi', async () => {
    const api = setup()
    await ready()
    fill('Từ ngày', '2026-10-01')
    fill('Đến ngày', '2026-10-31')
    fireEvent.click(screen.getByRole('button', { name: 'Xuất sao kê' }))
    expect(await screen.findByText('Chọn ít nhất một người cao tuổi.')).toBeInTheDocument()
    expect(api.calls('POST', '/balance-exports/statement')).toHaveLength(0)
  })

  it('Q_386_baoCaoThuChi_khongCanChonNguoi_vaDinhDangPdf', async () => {
    const api = setup()
    api.on(
      'POST',
      '/balance-exports/income-expense-report',
      json(201, {
        id: 10,
        type: 'INCOME_EXPENSE',
        format: 'PDF',
        from: '2026-10-01',
        to: '2026-10-31',
        rowCount: 3,
        at: '2026-11-01T03:00:00Z',
      }),
    )
    await ready()
    fill('Từ ngày', '2026-10-01')
    fill('Đến ngày', '2026-10-31')
    fireEvent.change(screen.getByLabelText('Định dạng'), { target: { value: 'PDF' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xuất báo cáo thu chi' }))
    expect(await screen.findByText('3 dòng')).toBeInTheDocument()
    expect(JSON.parse(api.calls('POST', '/balance-exports/income-expense-report')[0][1].body)).toEqual({
      from: '2026-10-01',
      to: '2026-10-31',
      format: 'PDF',
    })
  })

  it('FR_025_vuotKhoangXuat_422_hienLoi', async () => {
    const api = setup()
    api.on(
      'POST',
      '/balance-exports/income-expense-report',
      problem(422, 'EXPORT_RANGE_EXCEEDED', { detail: 'Khoảng xuất dài hơn 12 tháng.' }),
    )
    await ready()
    fill('Từ ngày', '2025-09-01')
    fill('Đến ngày', '2026-10-31')
    fireEvent.click(screen.getByRole('button', { name: 'Xuất báo cáo thu chi' }))
    expect(await screen.findByText('Khoảng xuất dài hơn 12 tháng.')).toBeInTheDocument()
  })

  it('FR_028_khongCoQuyenXuat_anNutXuat', async () => {
    setup([{ function: 'STATEMENT_REPORT', operation: 'VIEW' }])
    expect(await screen.findByText(/Chỉ Kế toán xuất được/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Xuất sao kê' })).not.toBeInTheDocument()
  })
})
