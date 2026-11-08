import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PrescriptionPanel } from './PrescriptionPanel'

// 006 Đơn thuốc theo api-006 (prescriptions, check, suspend, resume, discontinue, change-dosage, medications) và
// api-001 (danh sách, hồ sơ): chọn người cao tuổi, thẻ đơn theo allowedCommands, kê đơn kèm kiểm tra dị ứng thật, đổi liều,
// ngừng đơn; mọi lệnh ghi gửi Idempotency-Key; lỗi 403, 409.

let api

const RESIDENT = { id: 9, profileCode: 'HS-0009', personalInfo: { fullName: 'Trần Thị Mai' } }
const PROFILE = {
  id: 9,
  personalInfo: { fullName: 'Trần Thị Mai' },
  activeHealthItems: [{ type: 'ALLERGY', status: 'ACTIVE', allergen: { id: 1, name: 'Penicillin' } }],
}
const MEDICATIONS = [
  { id: 1, name: 'Amlodipin', concentration: '5mg', dosageForm: 'Viên nén', defaultRoute: 'Uống', usageUnit: 'viên', allergenIds: [1001] },
  { id: 2, name: 'Amoxicillin', concentration: '500mg', dosageForm: 'Viên nang', defaultRoute: 'Uống', usageUnit: 'viên', allergenIds: [2001] },
]
const ACTIVE = {
  id: 7,
  residentId: 9,
  medicationId: 1,
  dosage: 1,
  route: 'Uống',
  prescriptionType: 'PERIODIC',
  frequency: { type: 'TIMED_SLOTS', timedSlots: ['08:00'] },
  startedAt: '2026-11-01T01:00:00Z',
  medicationSource: 'FACILITY',
  prescriber: { type: 'INTERNAL' },
  status: 'ACTIVE',
  allowedCommands: ['SUSPEND', 'DISCONTINUE', 'CHANGE_DOSE'],
}
const STOPPED = { ...ACTIVE, id: 6, status: 'DISCONTINUED', allowedCommands: [] }

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/residents', json(200, { items: [RESIDENT] }))
  api.on('GET', '/residents/9', json(200, PROFILE))
  api.on('GET', '/residents/9/prescriptions', json(200, [ACTIVE, STOPPED]))
  api.on('GET', '/medications', json(200, MEDICATIONS))
})

afterEach(() => vi.unstubAllGlobals())

async function open(role = 'DOCTOR') {
  render(<PrescriptionPanel role={role} onSaved={() => {}} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Trần Thị Mai, HS-0009' }))
  await screen.findAllByText('Amlodipin 5mg')
}

describe('PrescriptionPanel', () => {
  it('FR_016a_chonNguoi_hienDonVoiTrangThaiLieuTanSuat', async () => {
    await open()
    expect(screen.getAllByText('Các mốc giờ: 08:00').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Hiệu lực').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Amlodipin 5mg')).toHaveLength(2)
    fireEvent.click(screen.getByRole('button', { name: 'Đã ngừng' }))
    expect(screen.getAllByText('Amlodipin 5mg')).toHaveLength(1)
    expect(screen.queryByRole('button', { name: /Đổi liều/ })).toBeNull()
  })

  it('BR_M07_05_dieuDuongKhongCoNutDoiLieuNgung_chiNhapDonBenNgoai', async () => {
    await open('NURSING_STAFF')
    expect(screen.queryByRole('button', { name: /Đổi liều/ })).toBeNull()
    expect(screen.queryByRole('button', { name: /Ngừng/ })).toBeNull()
    expect(screen.getByRole('button', { name: /Nhập đơn bên ngoài/ })).toBeTruthy()
  })

  it('FR_011_ngungDon_guiLyDoVaIdempotencyKey', async () => {
    api.on('POST', '/prescriptions/7/discontinue', json(200, {}))
    await open()
    fireEvent.click(screen.getByRole('button', { name: /Ngừng/ }))
    fireEvent.change(await screen.findByLabelText('Lý do'), { target: { value: 'Hết đợt điều trị' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận ngừng đơn' }))
    await waitFor(() => expect(api.calls('POST', '/prescriptions/7/discontinue')).toHaveLength(1))
    const [, init] = api.calls('POST', '/prescriptions/7/discontinue')[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(JSON.parse(init.body)).toEqual({ reason: 'Hết đợt điều trị' })
  })

  it('FR_011_ngungDon_409_hienLoiTrongKhung', async () => {
    api.on('POST', '/prescriptions/7/discontinue', problem(409, 'INVALID_TRANSITION', { detail: 'Đơn không ở trạng thái ngừng được' }))
    await open()
    fireEvent.click(screen.getByRole('button', { name: /Ngừng/ }))
    fireEvent.change(await screen.findByLabelText('Lý do'), { target: { value: 'Hết đợt' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận ngừng đơn' }))
    expect(await screen.findByText('Đơn không ở trạng thái ngừng được')).toBeTruthy()
  })

  it('FR_007_keDon_kiemTraDiUngTuBackend_vaGuiIssueConfirmations', async () => {
    api.on('POST', '/residents/9/prescriptions/check', json(200, { issues: [{ issueCode: 'ALG-1', issueType: 'ALLERGY_CONFLICT', activeIngredient: 'Penicillin', severity: 'SEVERE' }], reviewItems: [] }))
    api.on('POST', '/residents/9/prescriptions', json(201, { ...ACTIVE, id: 8 }))
    await open()
    fireEvent.click(screen.getByRole('button', { name: /Nhập đơn thuốc mới/ }))
    const sheet = await screen.findByRole('dialog')
    expect(within(sheet).getByText(/Penicillin/)).toBeTruthy()
    fireEvent.change(within(sheet).getByLabelText('Thuốc từ danh mục'), { target: { value: '2' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Kiểm tra ngay' }))
    expect(await within(sheet).findByText('Xung đột dị ứng Penicillin, mức SEVERE')).toBeTruthy()
    fireEvent.change(within(sheet).getByLabelText(/Lý do xác nhận/), { target: { value: 'Đã cân nhắc lợi ích' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu đơn thuốc' }))

    await waitFor(() => expect(api.calls('POST', '/residents/9/prescriptions')).toHaveLength(1))
    const [, init] = api.calls('POST', '/residents/9/prescriptions')[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(JSON.parse(init.body)).toMatchObject({
      medicationId: 2,
      prescriptionType: 'PERIODIC',
      prescriber: { type: 'INTERNAL' },
      issueConfirmations: [{ issueCode: 'ALG-1', reason: 'Đã cân nhắc lợi ích' }],
    })
  })

  it('FR_007_luuKhiChuaXacNhan_409IssuesRequireConfirmation_hienVanDe', async () => {
    api.on('POST', '/residents/9/prescriptions', problem(409, 'ISSUES_REQUIRE_CONFIRMATION', { issues: [{ issueCode: 'DUP-3', issueType: 'DUPLICATE_ACTIVE_INGREDIENT', activeIngredient: 'Amlodipin', duplicatePrescriptionId: 3 }] }))
    await open()
    fireEvent.click(screen.getByRole('button', { name: /Nhập đơn thuốc mới/ }))
    const sheet = await screen.findByRole('dialog')
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu đơn thuốc' }))
    expect(await within(sheet).findByText('Trùng hoạt chất Amlodipin với đơn #3')).toBeTruthy()
  })

  it('FR_016_doiLieu_guiDonThayTheVaLyDo', async () => {
    api.on('POST', '/prescriptions/7/change-dosage', json(201, { ...ACTIVE, id: 9 }))
    await open()
    fireEvent.click(screen.getByRole('button', { name: /Đổi liều/ }))
    const sheet = await screen.findByRole('dialog')
    fireEvent.change(within(sheet).getByLabelText('Liều mới'), { target: { value: '2' } })
    fireEvent.change(within(sheet).getByLabelText(/Lý do đổi liều/), { target: { value: 'Huyết áp chưa đạt' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Xác nhận đổi liều' }))
    await waitFor(() => expect(api.calls('POST', '/prescriptions/7/change-dosage')).toHaveLength(1))
    const [, init] = api.calls('POST', '/prescriptions/7/change-dosage')[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    const body = JSON.parse(init.body)
    expect(body).toMatchObject({ reason: 'Huyết áp chưa đạt', newPrescription: { medicationId: 1, dosage: 2, prescriber: { type: 'INTERNAL' } } })
    expect(body.effectiveFrom).toMatch(/\+07:00$/)
  })

  it('FR_016a_403_hienLoiKhiKhongCoQuyenDocDon', async () => {
    api.on('GET', '/residents/9/prescriptions', problem(403, 'FORBIDDEN'))
    render(<PrescriptionPanel role="DOCTOR" onSaved={() => {}} />)
    fireEvent.click(await screen.findByRole('button', { name: 'Trần Thị Mai, HS-0009' }))
    expect(await screen.findByText('Không có quyền thực hiện')).toBeTruthy()
  })
})
