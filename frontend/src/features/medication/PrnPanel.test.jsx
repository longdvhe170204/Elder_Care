import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PrnPanel } from './PrnPanel'

// 006 Thuốc khi cần theo api-006 (prescriptions, prn-administrations, medications), api-001 (danh sách, hồ sơ):
// chọn người cao tuổi, thẻ đơn PRN, chặn do backend tính, ghi lần dùng gửi Idempotency-Key, lỗi 403, 409.

let api

const RESIDENT = { id: 9, profileCode: 'HS-0009', personalInfo: { fullName: 'Trần Thị Mai' } }
const PRN = { id: 7, residentId: 9, medicationId: 4, dosage: 1, route: 'Uống', prescriptionType: 'PRN', minIntervalMinutes: 240, maxDosesPer24h: 3, medicationSource: 'FACILITY', status: 'ACTIVE' }
const PERIODIC = { ...PRN, id: 8, prescriptionType: 'PERIODIC' }
const HISTORY = { prescriptionId: 7, administrations: [], canAdministerNow: true }

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/residents', json(200, { items: [RESIDENT] }))
  api.on('GET', '/residents/9', json(200, { id: 9, personalInfo: { fullName: 'Trần Thị Mai' }, activeHealthItems: [{ type: 'ALLERGY', status: 'ACTIVE', allergen: { id: 1, name: 'Penicillin' } }] }))
  api.on('GET', '/residents/9/prescriptions', json(200, [PRN, PERIODIC]))
  api.on('GET', '/medications', json(200, [{ id: 4, name: 'Paracetamol', concentration: '500mg', usageUnit: 'viên' }]))
  api.on('GET', '/prescriptions/7/prn-administrations', json(200, HISTORY))
})

afterEach(() => vi.unstubAllGlobals())

async function pickResident() {
  fireEvent.click(await screen.findByRole('button', { name: 'Trần Thị Mai, HS-0009' }))
}

describe('PrnPanel', () => {
  it('FR_024_chonNguoi_hienChiDonKhiCanVaDiUng', async () => {
    render(<PrnPanel canRecord now="10:00" onSaved={() => {}} />)
    await pickResident()
    expect(await screen.findByText('Paracetamol 500mg')).toBeTruthy()
    expect(screen.getByText(/Penicillin/)).toBeTruthy()
    expect(screen.queryByText('#8')).toBeNull()
    expect(api.calls('GET', '/prescriptions/8/prn-administrations')).toHaveLength(0)
    expect(JSON.stringify(api.fetchMock.mock.calls.find(([u]) => u.includes('/prescriptions?')))).toContain('ACTIVE')
  })

  it('FR_025_backendChan_hienGioSomNhatVaKhongCoNutGhi', async () => {
    api.on('GET', '/prescriptions/7/prn-administrations', json(200, { ...HISTORY, canAdministerNow: false, earliestNextAllowedTime: '2026-11-08T05:00:00Z' }))
    render(<PrnPanel canRecord now="10:00" onSaved={() => {}} />)
    await pickResident()
    expect(await screen.findByText(/sớm nhất được dùng lúc/)).toBeTruthy()
    expect(screen.getByText('12:00')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Ghi lần dùng' })).toBeNull()
  })

  it('FR_024_ghiLanDung_guiIdempotencyKeyVaThanLenh', async () => {
    api.on('POST', '/prescriptions/7/prn-administrations', json(201, {}))
    const onSaved = vi.fn()
    render(<PrnPanel canRecord now="10:00" onSaved={onSaved} />)
    await pickResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi lần dùng' }))
    fireEvent.change(await screen.findByLabelText('Lý do dùng'), { target: { value: 'Sốt 38,6' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu lần dùng' }))

    await waitFor(() => expect(api.calls('POST', '/prescriptions/7/prn-administrations')).toHaveLength(1))
    const [, init] = api.calls('POST', '/prescriptions/7/prn-administrations')[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    const body = JSON.parse(init.body)
    expect(body).toMatchObject({ dosage: 1, reason: 'Sốt 38,6' })
    expect(body.administeredAt).toMatch(/T10:00:00\+07:00$/)
    await waitFor(() => expect(onSaved).toHaveBeenCalled())
  })

  it('FR_025_409_vuotGioiHan_hienLoiVaGioSomNhat', async () => {
    api.on('POST', '/prescriptions/7/prn-administrations', problem(409, 'PRN_LIMIT_EXCEEDED', { detail: 'Chưa đủ khoảng cách', earliestAllowedAt: '2026-11-08T05:30:00Z' }))
    render(<PrnPanel canRecord now="10:00" onSaved={() => {}} />)
    await pickResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi lần dùng' }))
    fireEvent.change(await screen.findByLabelText('Lý do dùng'), { target: { value: 'Đau đầu' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu lần dùng' }))
    expect(await screen.findByText('Chưa thực hiện được')).toBeTruthy()
    expect(screen.getByText(/12:30/)).toBeTruthy()
  })

  it('FR_024_403_hienLoiKhiKhongCoQuyenDocDon', async () => {
    api.on('GET', '/residents/9/prescriptions', problem(403, 'FORBIDDEN'))
    render(<PrnPanel canRecord now="10:00" onSaved={() => {}} />)
    await pickResident()
    expect(await screen.findByText('Không có quyền thực hiện')).toBeTruthy()
  })
})
