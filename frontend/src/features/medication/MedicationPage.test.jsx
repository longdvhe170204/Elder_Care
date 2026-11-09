import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { MedicationPage } from './MedicationPage'

// 006 Phát thuốc theo api-006 (rounds, administer), api-001 (hồ sơ), api-003 (vị trí), api-008 (ca): dải "Cần làm
// ngay" cho liều Trễ (DESIGN.md 6.7), thẻ không viền đỏ, xác nhận liều gửi Idempotency-Key và đúng thân lệnh.

let api

const DOSE = {
  doseKey: '7-202611080900',
  residentId: 9,
  prescriptionId: 7,
  medicationName: 'Metformin 500mg',
  dosage: 1,
  dosageUnit: 'viên',
  route: 'Uống',
  scheduledTime: '2026-11-08T02:00:00Z',
  windowFrom: '2026-11-08T01:30:00Z',
  windowTo: '2026-11-08T02:30:00Z',
  status: 'LATE',
  currentStatus: 'LATE',
  materialized: true,
  floorShared: false,
  requiresOnline: false,
  medicationSource: 'FACILITY',
  usableBatches: [],
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/medication-doses/rounds', json(200, [DOSE]))
  api.on('GET', '/residents/9', json(200, { id: 9, personalInfo: { fullName: 'Trần Thị Mai' }, activeHealthItems: [] }))
  api.on('GET', '/residents/9/location', json(200, { location: { room: 'P201', label: 'Khu A – Tầng 2 – P201 – G1' } }))
  api.on('GET', '/me/shifts', json(200, []))
})

afterEach(() => vi.unstubAllGlobals())

describe('MedicationPage', () => {
  it('FR_026_vongPhatThuoc_hienTenNguoiVaThuocTuApi', async () => {
    render(<MedicationPage role="NURSING_STAFF" />)
    expect(await screen.findByText('Metformin 500mg')).toBeTruthy()
    expect(screen.getAllByText('Trần Thị Mai').length).toBeGreaterThan(0)
    expect(screen.getByText('P201')).toBeTruthy()
  })

  it('DESIGN_6_7_lieuTre_hienOReDaiCanLamNgay_theKhongCoVienDo', async () => {
    const { container } = render(<MedicationPage role="NURSING_STAFF" />)
    expect(await screen.findByRole('region', { name: 'Cần làm ngay' })).toBeTruthy()
    expect(container.querySelector('[class*="ring-[1.5px]"]')).toBeNull()
  })

  it('FR_026_403_hienLoiThayVangTai', async () => {
    api.on('GET', '/medication-doses/rounds', problem(403, 'FORBIDDEN'))
    render(<MedicationPage role="NURSING_STAFF" />)
    expect(await screen.findByText('Không có quyền thực hiện')).toBeTruthy()
  })

  it('FR_026_xacNhanDaDung_guiIdempotencyKeyVaThanLenh', async () => {
    api.on('POST', '/medication-doses/7-202611080900/administer', json(200, { ...DOSE, status: 'ADMINISTERED' }))
    render(<MedicationPage role="NURSING_STAFF" />)
    fireEvent.click(await screen.findByRole('button', { name: 'Phát thuốc' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Đã dùng' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu xác nhận' }))

    await waitFor(() => expect(api.calls('POST', '/medication-doses/7-202611080900/administer')).toHaveLength(1))
    const [, init] = api.calls('POST', '/medication-doses/7-202611080900/administer')[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(JSON.parse(init.body)).toMatchObject({ basis: 'REGULAR' })
    expect(JSON.parse(init.body).administeredAt).toMatch(/T\d{2}:\d{2}:00\+07:00$/)
  })

  it('FR_026_tuChoi_guiLyDo', async () => {
    api.on('POST', '/medication-doses/7-202611080900/refuse', json(200, {}))
    render(<MedicationPage role="NURSING_STAFF" />)
    fireEvent.click(await screen.findByRole('button', { name: 'Phát thuốc' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Từ chối' }))
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Người cao tuổi không muốn uống' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu xác nhận' }))

    await waitFor(() => expect(api.calls('POST', '/medication-doses/7-202611080900/refuse')).toHaveLength(1))
    const [, init] = api.calls('POST', '/medication-doses/7-202611080900/refuse')[0]
    expect(JSON.parse(init.body)).toEqual({ reason: 'Người cao tuổi không muốn uống' })
  })
})
