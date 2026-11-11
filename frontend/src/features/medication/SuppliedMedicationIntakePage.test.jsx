import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { SuppliedMedicationIntakePage } from './SuppliedMedicationIntakePage'

// 006 T065 (FR-054, FR-055, BR-M07-24; Q-410): Điều dưỡng thấy dòng thuốc đã mua chờ tiếp nhận của người cao tuổi trong
// phạm vi, ghi Đạt hoặc Không đạt (Không đạt bắt buộc sai lệch và lý do), gửi requestLineId và Idempotency-Key.

let api

const LINE = {
  id: 42000001,
  procurementRequestId: 42,
  residentId: 5,
  prescriptionId: 91,
  medicationName: 'Amlodipin',
  activeIngredient: 'Amlodipin',
  strength: '5 mg',
  dosageForm: 'Viên nén',
  quantity: 30,
  unit: 'viên',
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/procurement-medication-lines', json(200, [LINE]))
  api.on(
    'GET',
    '/residents',
    json(200, { items: [{ id: 5, profileCode: 'NCT-005', personalInfo: { fullName: 'Nguyễn Thị Lan' } }] }),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function fill(label, value) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

describe('SuppliedMedicationIntakePage', () => {
  it('FR_054_dat_guiDongVaIdempotencyKey', async () => {
    api.on('POST', '/supplied-medication-intakes', json(201, {}))
    render(<SuppliedMedicationIntakePage />)

    expect(await screen.findByText('Amlodipin 5 mg, Viên nén')).toBeInTheDocument()
    expect(await screen.findByText(/Nguyễn Thị Lan/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Tiếp nhận Amlodipin của Nguyễn Thị Lan' }))
    fill('Hạn dùng', '2027-11-01')
    fill('Tình trạng bao bì', 'Nguyên vỉ')
    fill('Người giao', 'Nhân viên mua A')
    fireEvent.click(screen.getByRole('button', { name: 'Ghi tiếp nhận' }))

    await waitFor(() => expect(api.calls('POST', '/supplied-medication-intakes')).toHaveLength(1))
    const [, init] = api.calls('POST', '/supplied-medication-intakes')[0]
    expect(JSON.parse(init.body)).toEqual({
      requestLineId: 42000001,
      concentration: '5 mg',
      dosageForm: 'Viên nén',
      receivedQuantity: 30,
      unit: 'viên',
      expiryDate: '2027-11-01',
      condition: 'Nguyên vỉ',
      result: 'PASSED',
      delivererName: 'Nhân viên mua A',
    })
    expect(init.headers['Idempotency-Key']).toBeTruthy()
  })

  it('BR_M07_24_khongDat_batBuocSaiLechVaLyDo', async () => {
    api.on('POST', '/supplied-medication-intakes', json(201, {}))
    render(<SuppliedMedicationIntakePage />)
    fireEvent.click(await screen.findByRole('button', { name: /Tiếp nhận Amlodipin/ }))
    fill('Hạn dùng', '2027-11-01')
    fill('Tình trạng bao bì', 'Rách hộp')
    fill('Người giao', 'Nhân viên mua A')
    fireEvent.click(screen.getByRole('radio', { name: 'Không đạt' }))
    fireEvent.click(screen.getByRole('button', { name: 'Ghi tiếp nhận' }))
    expect(await screen.findByText('Nhập sai lệch')).toBeInTheDocument()
    expect(screen.getByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/supplied-medication-intakes')).toHaveLength(0)

    fill('Sai lệch', 'Thiếu 10 viên')
    fill('Lý do', 'Nhà cung cấp giao thiếu')
    fireEvent.click(screen.getByRole('button', { name: 'Ghi tiếp nhận' }))
    await waitFor(() => expect(api.calls('POST', '/supplied-medication-intakes')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/supplied-medication-intakes')[0][1].body)
    expect(body).toMatchObject({
      result: 'NOT_PASSED',
      discrepancy: 'Thiếu 10 viên',
      reason: 'Nhà cung cấp giao thiếu',
    })
  })

  it('Q410_khongCoQuyen_baoKhongCoQuyen', async () => {
    api.on('GET', '/procurement-medication-lines', problem(403, 'PERMISSION_DENIED'))
    render(<SuppliedMedicationIntakePage />)
    expect(await screen.findByText(/không có quyền tiếp nhận thuốc cung ứng/i)).toBeInTheDocument()
  })

  it('Q410_khongCoDong_baoTrong', async () => {
    api.on('GET', '/procurement-medication-lines', json(200, []))
    render(<SuppliedMedicationIntakePage />)
    expect(await screen.findByText('Không có thuốc mua về đang chờ tiếp nhận.')).toBeInTheDocument()
  })
})
