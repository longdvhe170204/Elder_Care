import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { DeathsPage } from './DeathsPage'

// 004 T065, T109 (US8; FR-068 → FR-072, FR-071a, BR-M02-21, Q-387): bản ghi qua đời, nguyên nhân bị ẩn với Hành chính, Kế
// toán, bổ sung nguyên nhân bằng đính chính, danh sách việc bắt buộc sau qua đời và nút xin ngoại lệ cho mục quyết toán.

let api

const RECORD = {
  id: 8,
  deathAt: '2026-11-04T05:30:00Z',
  place: 'Phòng 201',
  discoveredBy: 'Hoàng Thu Hà',
  confirmedBy: 'Bác sĩ Lê Minh',
  cause: 'Suy tim',
  corrected: false,
  hiddenFields: [],
}
const TASKS = {
  items: [
    { code: 'BELONGINGS', status: 'COMPLETED', basis: 'Đã trả toàn bộ đồ gửi' },
    { code: 'FINAL_PERIOD_COSTS', status: 'NOT_COMPLETED', basis: 'Bảng kỳ cuối chưa chốt' },
    { code: 'SETTLEMENT', status: 'NOT_COMPLETED', basis: 'Còn nợ 2.000.000 đ' },
  ],
}

function renderPage() {
  return render(
    <PermissionsProvider>
      <DeathsPage />
    </PermissionsProvider>,
  )
}

async function chooseResident() {
  const select = await screen.findByLabelText('Người cao tuổi')
  await screen.findByRole('option', { name: 'Nguyễn Thị Lan (NCT-0005)' })
  fireEvent.change(select, { target: { value: '5' } })
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
  api.on(
    'GET',
    '/residents',
    json(200, { items: [{ id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' } }] }),
  )
  api.on('GET', '/residents/5/death-record', json(200, RECORD))
  api.on('GET', '/residents/5/post-death-tasks', json(200, TASKS))
})

describe('DeathsPage', () => {
  it('FR_069_hienBanGhiQuaDoi_vaViecSauQuaDoiVoiCanCu', async () => {
    renderPage()
    await chooseResident()

    expect(await screen.findByText('Phòng 201')).toBeInTheDocument()
    expect(screen.getByText('Suy tim')).toBeInTheDocument()
    expect(screen.getByText('Đồ gửi')).toBeInTheDocument()
    expect(screen.getByText(/Bảng kỳ cuối chưa chốt/)).toBeInTheDocument()
    expect(screen.getByText(/Còn nợ 2\.000\.000/)).toBeInTheDocument()
  })

  it('BR_M02_21_nguyenNhanBiAn_khongHienOCauNguyenNhan', async () => {
    api.on('GET', '/residents/5/death-record', json(200, { ...RECORD, cause: undefined, hiddenFields: ['cause'] }))
    renderPage()
    await chooseResident()

    expect(await screen.findByText('Phòng 201')).toBeInTheDocument()
    expect(screen.getByText(/Nguyên nhân được ẩn theo quyền/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Đính chính bản ghi…' }))
    expect(screen.queryByLabelText('Nguyên nhân đúng')).not.toBeInTheDocument()
  })

  it('FR_069_dinhChinhNguyenNhan_guiLoaiVaNoiDungDung', async () => {
    api.on('POST', '/death-records/8/correct', json(201, {}))
    renderPage()
    await chooseResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Đính chính bản ghi…' }))

    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Nguyên nhân đúng'), { target: { value: 'Suy hô hấp' } })
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Kết luận của bác sĩ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    await waitFor(() => expect(api.calls('POST', '/death-records/8/correct')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/death-records/8/correct')[0][1].body)).toEqual({
      type: 'EDIT_CONTENT',
      correctedContent: { cause: 'Suy hô hấp' },
      reason: 'Kết luận của bác sĩ',
    })
  })

  it('FR_071a_xinNgoaiLeMucQuyetToan_canLyDo', async () => {
    api.on('POST', '/residents/5/post-death-tasks/request-settlement-exception', json(201, { approvalRequestId: 91 }))
    renderPage()
    await chooseResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Xin ngoại lệ mục quyết toán…' }))
    fireEvent.click(screen.getByRole('button', { name: 'Gửi yêu cầu ngoại lệ' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Gia đình còn nợ, đã cam kết thanh toán' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi yêu cầu ngoại lệ' }))
    await waitFor(() =>
      expect(api.calls('POST', '/residents/5/post-death-tasks/request-settlement-exception')).toHaveLength(1),
    )
    expect(await screen.findByText(/chờ Quản lý viện duyệt/)).toBeInTheDocument()
  })

  it('FR_071_mucQuyetToanDaXong_khongCoNutNgoaiLe', async () => {
    api.on(
      'GET',
      '/residents/5/post-death-tasks',
      json(200, { items: [{ code: 'SETTLEMENT', status: 'COMPLETED' }], closedAt: '2026-11-05T01:00:00Z' }),
    )
    renderPage()
    await chooseResident()
    expect(await screen.findByText('Phòng 201')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Xin ngoại lệ mục quyết toán…' })).not.toBeInTheDocument()
    expect(screen.getByText(/Hồ sơ lưu trú đã đóng/)).toBeInTheDocument()
  })

  it('FR_069_404_chuaCoBanGhiQuaDoi', async () => {
    api.on('GET', '/residents/5/death-record', problem(404, 'NOT_FOUND'))
    renderPage()
    await chooseResident()
    expect(await screen.findByText('Người cao tuổi này chưa có bản ghi qua đời.')).toBeInTheDocument()
  })

  it('FR_069_403_hienKhongCoQuyenXem', async () => {
    api.on('GET', '/residents/5/death-record', problem(403, 'PERMISSION_DENIED'))
    renderPage()
    await chooseResident()
    expect(await screen.findByText(/Bạn không có quyền xem/)).toBeInTheDocument()
  })
})
