import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { ServicesPage } from './ServicesPage'

// 004 T068 (US9; FR-034 → FR-036, DBR-08, Q-297): danh mục dịch vụ, phiên bản đơn giá (không sửa phiên bản đã tham chiếu),
// hợp đồng lệch giá (chỉ Quản lý viện), ngừng hiệu lực kèm lý do và If-Match theo version.

let api

const CARE = { id: 3, type: 'CARE', name: 'Chăm sóc thường xuyên', unit: 'tháng', billingPeriod: 'MONTH', careLevelCode: 'REGULAR', status: 'ACTIVE', version: 4 }
const MEAL = { id: 4, type: 'MEAL', name: 'Bữa ăn', unit: 'bữa', billingPeriod: 'OCCURRENCE', status: 'ACTIVE', version: 1 }
const VERSIONS = [
  { id: 1, unitPrice: 5000000, effectiveFrom: '2026-01-01', effectiveTo: '2026-06-30', referenced: true },
  { id: 2, unitPrice: 5500000, effectiveFrom: '2026-07-01', referenced: false },
]

function renderPage() {
  return render(
    <PermissionsProvider>
      <ServicesPage />
    </PermissionsProvider>,
  )
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, ['VIEW', 'CREATE', 'UPDATE'].map((operation) => ({ function: 'SERVICE_PRICE', operation }))))
  api.on('GET', '/services', json(200, [CARE, MEAL]))
  api.on('GET', '/services/3/price-versions', json(200, VERSIONS))
  api.on('GET', '/services/3/mismatched-price-contracts', json(200, [{ contractId: 9, contractNumber: 'HD-009', recordedUnitPrice: 5000000, newUnitPrice: 5500000 }]))
})

describe('ServicesPage', () => {
  it('FR_034_bangDichVu_hienTenLoaiVaTrangThai', async () => {
    renderPage()

    expect(await screen.findByRole('button', { name: 'Chăm sóc thường xuyên' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Bữa ăn' })).toBeInTheDocument()
    expect(screen.getAllByText('Hiệu lực').length).toBeGreaterThan(0)
  })

  it('FR_035_khungChiTiet_hienPhienBanDonGia_vaHopDongLechGia', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Chăm sóc thường xuyên' }))

    expect(await screen.findByText(/đang mở/)).toBeInTheDocument()
    expect(screen.getByText(/Đã được hợp đồng tham chiếu/)).toBeInTheDocument()
    expect(await screen.findByText(/HD-009/)).toBeInTheDocument()
  })

  it('FR_035_taoPhienBanDonGia_guiGiaVaNgayHieuLuc', async () => {
    api.on('POST', '/services/3/price-versions', json(201, { id: 3, unitPrice: 6000000, effectiveFrom: '2026-12-01' }))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Chăm sóc thường xuyên' }))
    await screen.findByText(/đang mở/)

    fireEvent.click(screen.getByRole('button', { name: 'Thêm phiên bản đơn giá' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiên bản đơn giá' }))
    expect(await screen.findByText('Nhập đơn giá')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Đơn giá (đồng)'), { target: { value: '6000000' } })
    fireEvent.change(screen.getByLabelText('Hiệu lực từ'), { target: { value: '2026-12-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiên bản đơn giá' }))

    await waitFor(() => expect(api.calls('POST', '/services/3/price-versions')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/services/3/price-versions')[0][1].body)).toEqual({
      unitPrice: 6000000,
      effectiveFrom: '2026-12-01',
    })
  })

  it('FR_035_409ChongPhienBan_hienLoiTrongKhung', async () => {
    api.on('POST', '/services/3/price-versions', problem(409, 'PRICE_VERSION_OVERLAP', { detail: 'Chồng lên phiên bản đã có' }))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Chăm sóc thường xuyên' }))
    await screen.findByText(/đang mở/)
    fireEvent.click(screen.getByRole('button', { name: 'Thêm phiên bản đơn giá' }))
    fireEvent.change(screen.getByLabelText('Đơn giá (đồng)'), { target: { value: '6000000' } })
    fireEvent.change(screen.getByLabelText('Hiệu lực từ'), { target: { value: '2026-03-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiên bản đơn giá' }))

    expect(await screen.findByText('Chồng lên phiên bản đã có')).toBeInTheDocument()
  })

  it('FR_034_ngungHieuLuc_canLyDo_guiIfMatchTheoVersion', async () => {
    api.on('POST', '/services/3/deactivate', json(200, {}))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Chăm sóc thường xuyên' }))
    await screen.findByText(/đang mở/)

    fireEvent.click(screen.getByRole('button', { name: 'Ngừng hiệu lực dịch vụ' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận ngừng hiệu lực' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Không còn cung cấp' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận ngừng hiệu lực' }))
    await waitFor(() => expect(api.calls('POST', '/services/3/deactivate')).toHaveLength(1))
    expect(api.calls('POST', '/services/3/deactivate')[0][1].headers['If-Match']).toBe('"4"')
  })

  it('FR_034_taoDichVu_dichVuChamSocBatBuocMuc', async () => {
    api.on('POST', '/services', json(201, CARE))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Tạo dịch vụ' }))
    const dialog = screen.getByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText('Tên dịch vụ'), { target: { value: 'Chăm sóc cơ bản' } })
    fireEvent.change(within(dialog).getByLabelText('Đơn vị'), { target: { value: 'tháng' } })
    fireEvent.change(within(dialog).getByLabelText('Loại dịch vụ'), { target: { value: 'CARE' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Tháng' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu dịch vụ' }))
    expect(await within(dialog).findByText('Chọn mức chăm sóc của dịch vụ chăm sóc')).toBeInTheDocument()
    expect(api.calls('POST', '/services')).toHaveLength(0)

    fireEvent.click(within(dialog).getByRole('button', { name: 'Cơ bản' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu dịch vụ' }))
    await waitFor(() => expect(api.calls('POST', '/services')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/services')[0][1].body)).toEqual({
      type: 'CARE',
      name: 'Chăm sóc cơ bản',
      unit: 'tháng',
      billingPeriod: 'MONTH',
      careLevelCode: 'BASIC',
    })
  })

  it('FR_034_403_hienKhongCoQuyenXem', async () => {
    api.on('GET', '/services', problem(403, 'PERMISSION_DENIED'))
    renderPage()
    expect(await screen.findByText(/Bạn không có quyền xem/)).toBeInTheDocument()
  })
})
