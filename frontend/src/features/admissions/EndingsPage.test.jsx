import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { EndingsPage } from './EndingsPage'

// 004 T061, T107 (US7; FR-062 → FR-067, BR-M02-12; Q-379): lập hồ sơ kết thúc, danh sách điều kiện (a) → (h) với (h) gồm hai
// ý (quyết toán số dư, tiền cọc và Kế toán đã xác nhận), (e) không có ngoại lệ, ghi nhận bàn giao kèm bản ghi đón, đổi ngày,
// hủy hồ sơ. Hợp đồng chưa có danh sách hồ sơ nên mở theo mã hồ sơ.

let api

const ENDING = {
  id: 14,
  residentId: 5,
  endCase: 'DISCHARGE',
  expectedEndDate: '2026-11-20',
  status: 'PREPARING',
}

const CONDITIONS = {
  conditions: [
    { code: 'END_A_COSTS', name: 'Chi phí kỳ cuối đã chốt', status: 'NOT_MET', basis: 'FR-063' },
    { code: 'END_E_HANDOVER', name: 'Đã bàn giao người cao tuổi', status: 'NOT_MET', basis: 'FR-063' },
    { code: 'END_F_CONTRACT', name: 'Hợp đồng được xử lý', status: 'AUTOMATIC', basis: 'FR-063' },
    { code: 'END_H_SETTLEMENT', name: 'Số dư và tiền cọc đã quyết toán', status: 'NOT_MET', basis: 'FR-063' },
    {
      code: 'END_H_SETTLEMENT_CONFIRMATION',
      name: 'Kế toán đã xác nhận quyết toán',
      status: 'NOT_MET',
      basis: 'FR-063',
    },
  ],
  warnings: [],
}

function renderPage(role = 'OFFICE_STAFF') {
  return render(
    <PermissionsProvider>
      <EndingsPage role={role} />
    </PermissionsProvider>,
  )
}

async function openEnding() {
  fireEvent.change(await screen.findByLabelText('Mã hồ sơ kết thúc'), { target: { value: '14' } })
  fireEvent.click(screen.getByRole('button', { name: 'Mở hồ sơ' }))
  await screen.findByText('Chi phí kỳ cuối đã chốt')
}

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      ['VIEW', 'CREATE', 'UPDATE'].map((operation) => ({ function: 'RESIDENCY_END_DEATH', operation })),
    ),
  )
  api.on('GET', '/residency-endings/14', json(200, ENDING))
  api.on('GET', '/residency-endings/14/conditions', json(200, CONDITIONS))
  api.on('GET', '/residents/5', json(200, { id: 5, personalInfo: { fullName: 'Nguyễn Thị Lan' } }))
  api.on(
    'GET',
    '/residents/5/relationships',
    json(200, [
      { id: 1, status: 'ACTIVE', relationship: 'CHILD', familyMember: { id: 41, fullName: 'Trần Văn Minh' } },
    ]),
  )
  api.on('GET', '/staff', json(200, [{ id: 9, fullName: 'Hoàng Thu Hà' }]))
  api.on(
    'GET',
    '/residents',
    json(200, { items: [{ id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' } }] }),
  )
})

describe('EndingsPage', () => {
  it('FR_063_moTheoMa_hienDieuKien_vaHaiYCuaMucH', async () => {
    renderPage()
    await openEnding()

    expect(screen.getByRole('heading', { name: 'Nguyễn Thị Lan' })).toBeInTheDocument()
    expect(screen.getByText('Số dư và tiền cọc đã quyết toán')).toBeInTheDocument()
    expect(screen.getByText('Kế toán đã xác nhận quyết toán')).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Điều kiện (h): số dư và tiền cọc đã quyết toán' })).toBeInTheDocument()
    expect(screen.getByText(/Tự động/)).toBeInTheDocument()
  })

  it('FR_064_xinNgoaiLe_khongCoChoDieuKienBanGiao_vaXacNhanQuyetToan', async () => {
    renderPage()
    await openEnding()

    fireEvent.click(screen.getByRole('button', { name: 'Xin ngoại lệ…' }))
    const options = within(screen.getByLabelText('Điều kiện xin ngoại lệ'))
      .getAllByRole('option')
      .map((o) => o.textContent)
    expect(options).toContain('Chi phí kỳ cuối đã chốt')
    expect(options).toContain('Số dư và tiền cọc đã quyết toán')
    expect(options).not.toContain('Đã bàn giao người cao tuổi')
    expect(options).not.toContain('Kế toán đã xác nhận quyết toán')
  })

  it('FR_064_xinNgoaiLe_guiMaDieuKienVaLyDo', async () => {
    api.on('POST', '/residency-endings/14/request-exception', json(201, { approvalRequestId: 90 }))
    renderPage()
    await openEnding()
    fireEvent.click(screen.getByRole('button', { name: 'Xin ngoại lệ…' }))
    fireEvent.change(screen.getByLabelText('Điều kiện xin ngoại lệ'), { target: { value: 'END_A_COSTS' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi yêu cầu ngoại lệ' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Chi phí chờ chứng từ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi yêu cầu ngoại lệ' }))
    await waitFor(() => expect(api.calls('POST', '/residency-endings/14/request-exception')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residency-endings/14/request-exception')[0][1].body)).toEqual({
      conditionCode: 'END_A_COSTS',
      reason: 'Chi phí chờ chứng từ',
    })
  })

  it('FR_063_banGiaoChoNguoiThan_kemBanGhiDon', async () => {
    api.on('POST', '/residency-endings/14/handover', json(201, {}))
    renderPage()
    await openEnding()
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận bàn giao…' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bàn giao' }))
    expect(await screen.findByText('Chọn người nhận')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Người thân' }))
    fireEvent.change(await screen.findByLabelText('Người thân nhận'), { target: { value: '41' } })
    fireEvent.change(screen.getByLabelText('Thời điểm bàn giao'), { target: { value: '2026-11-20T09:00' } })
    fireEvent.change(await screen.findByLabelText('Nhân viên bàn giao'), { target: { value: '9' } })
    fireEvent.change(screen.getByLabelText('Mã bản ghi đón'), { target: { value: '7001' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bàn giao' }))

    await waitFor(() => expect(api.calls('POST', '/residency-endings/14/handover')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residency-endings/14/handover')[0][1].body)).toEqual({
      recipientType: 'FAMILY_MEMBER',
      familyMemberId: 41,
      handoverAt: '2026-11-20T09:00:00+07:00',
      handoverStaffId: 9,
      pickupRecordId: 7001,
    })
  })

  it('FR_062_lapHoSo_chuyenCoSoThieuNoiChuyenDen_baoTaiO', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập hồ sơ kết thúc' }))
    const dialog = screen.getByRole('dialog')
    await within(dialog).findByRole('option', { name: 'Nguyễn Thị Lan (NCT-0005)' })
    fireEvent.change(within(dialog).getByLabelText('Người cao tuổi'), { target: { value: '5' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Chuyển cơ sở' }))
    fireEvent.change(within(dialog).getByLabelText('Ngày kết thúc dự kiến'), { target: { value: '2026-11-30' } })
    fireEvent.change(within(dialog).getByLabelText('Người đề nghị'), { target: { value: 'Con gái' } })
    fireEvent.change(within(dialog).getByLabelText('Lý do'), { target: { value: 'Gia đình chuyển nơi ở' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lập hồ sơ' }))
    expect(await within(dialog).findByText('Nhập nơi chuyển đến')).toBeInTheDocument()
    expect(api.calls('POST', '/residency-endings')).toHaveLength(0)
  })

  it('FR_067_hoSoDaKetThuc_khongCoNutLenh', async () => {
    api.on('GET', '/residency-endings/14', json(200, { ...ENDING, status: 'COMPLETED' }))
    renderPage()
    await openEnding()
    expect(screen.queryByRole('button', { name: 'Ghi nhận bàn giao…' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Xin ngoại lệ…' })).not.toBeInTheDocument()
  })

  it('FR_062_404_hienLoiKhiMaKhongTonTai', async () => {
    api.on('GET', '/residency-endings/99', problem(404, 'NOT_FOUND', { detail: 'Không tìm thấy hồ sơ kết thúc' }))
    renderPage()
    fireEvent.change(await screen.findByLabelText('Mã hồ sơ kết thúc'), { target: { value: '99' } })
    fireEvent.click(screen.getByRole('button', { name: 'Mở hồ sơ' }))
    expect(await screen.findByText('Không tìm thấy hồ sơ kết thúc')).toBeInTheDocument()
  })
})
