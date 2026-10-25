import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { MedicationCatalogPage } from './MedicationCatalogPage'

// 006 màn Danh mục thuốc theo api-006 đã dịch: tra danh mục (FR-001, FR-003), sửa kèm If-Match, Ngừng hiệu lực kèm lý do
// (FR-002).

let api

const AMLODIPIN = {
  id: 2,
  name: 'Amlodipin',
  concentration: '5mg',
  dosageForm: 'Viên nang',
  defaultRoute: 'Uống',
  usageUnit: 'viên',
  allergenIds: [201],
  beforeWindowMinutes: 60,
  afterWindowMinutes: 60,
  specialControl: false,
  active: true,
  missingPricingItem: true,
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/medications', json(200, [AMLODIPIN]))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('MedicationCatalogPage', () => {
  it('FR_003_traDanhMuc_hienDauChuaCoMucVatPham', async () => {
    render(<MedicationCatalogPage role="DOCTOR" />)

    expect(await screen.findByText('Amlodipin')).toBeInTheDocument()
    expect(screen.getByText('5mg')).toBeInTheDocument()
    expect(screen.getByText('Chưa có mục vật phẩm viện')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Hiệu lực' }))
    await screen.findByText('Amlodipin')
    const [url] = api.calls('GET', '/medications').at(-1)
    expect(new URL(url, 'http://localhost').searchParams.get('active')).toBe('true')
  })

  it('FR_002_sua_layETagCuaThuocTruocKhiGui', async () => {
    api.on('GET', '/medications/2', json(200, AMLODIPIN, { ETag: '"3"' }))
    api.on('PUT', '/medications/2', json(200, AMLODIPIN))
    render(<MedicationCatalogPage role="NURSING_HOME_MANAGER" />)

    fireEvent.click(await screen.findByTitle('Sửa thông tin thuốc'))
    fireEvent.change(screen.getByLabelText('Hàm lượng *'), { target: { value: '10mg' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    await vi.waitFor(() => expect(api.calls('PUT', '/medications/2')).toHaveLength(1))
    const [, init] = api.calls('PUT', '/medications/2')[0]
    expect(init.headers['If-Match']).toBe('"3"')
    expect(JSON.parse(init.body)).toMatchObject({ name: 'Amlodipin', concentration: '10mg', allergenIds: [201] })
  })

  it('FR_002_ngungHieuLuc_guiLyDo', async () => {
    api.on('POST', '/medications/2/deactivate', json(200, { ...AMLODIPIN, active: false }))
    render(<MedicationCatalogPage role="NURSING_HOME_MANAGER" />)

    fireEvent.click(await screen.findByTitle('Ngừng hiệu lực thuốc'))
    fireEvent.change(screen.getByLabelText('Lý do ngừng hiệu lực *'), { target: { value: 'Ngừng phân phối' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận ngừng' }))

    await vi.waitFor(() => expect(api.calls('POST', '/medications/2/deactivate')).toHaveLength(1))
    const [, init] = api.calls('POST', '/medications/2/deactivate')[0]
    expect(JSON.parse(init.body)).toEqual({ reason: 'Ngừng phân phối' })
  })
})
