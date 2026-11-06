import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { AbsencesPage } from './AbsencesPage'

// 004 T056 (US6; FR-048 → FR-061, BR-M02-06, BR-M02-07, BR-M02-18): lượt vắng của một người cao tuổi, gia hạn dự kiến trở lại
// (Hành chính, Trưởng tầng), Giải phóng giường chỉ Quản lý viện kèm lý do, đính chính lượt đã đóng; báo vắng bán trú và ngày
// khu bán trú nghỉ. Cho tạm vắng, Ghi nhận trở về là lệnh của 001 ở tab Trạng thái của hồ sơ.

let api

const OPEN = {
  id: 61,
  residentId: 5,
  absenceType: 'HOME_VISIT',
  departedAt: '2026-11-03T01:00:00Z',
  expectedReturnAt: '2026-11-08T10:00:00Z',
  pickedUpBy: 'Trần Văn Minh',
  holdBed: true,
  status: 'OPEN',
}
const CLOSED = {
  ...OPEN,
  id: 62,
  status: 'CLOSED',
  returnedAt: '2026-10-10T10:00:00Z',
  departedAt: '2026-10-05T01:00:00Z',
}

function renderPage(role = 'OFFICE_STAFF') {
  return render(
    <PermissionsProvider>
      <AbsencesPage role={role} />
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
      ['VIEW', 'CREATE', 'UPDATE'].map((operation) => ({ function: 'ABSENCE_RETURN', operation })),
    ),
  )
  api.on(
    'GET',
    '/residents',
    json(200, { items: [{ id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' } }] }),
  )
  api.on('GET', '/absences', json(200, [OPEN, CLOSED]))
  api.on(
    'GET',
    '/absences/61',
    json(200, {
      ...OPEN,
      expectedReturnHistory: [
        {
          oldValue: '2026-11-06T10:00:00Z',
          newValue: '2026-11-08T10:00:00Z',
          reason: 'Gia đình xin thêm',
          at: '2026-11-05T02:00:00Z',
        },
      ],
    }),
  )
  api.on('GET', '/absences/62', json(200, CLOSED))
  api.on(
    'GET',
    '/day-care-closure-days',
    json(200, [{ id: 3, date: '2026-11-20', reason: 'Sửa chữa khu bán trú', status: 'ACTIVE' }]),
  )
})

describe('AbsencesPage', () => {
  it('FR_048_chonNguoiCaoTuoi_hienLuotVangCuaNguoiDo', async () => {
    renderPage()
    await chooseResident()

    expect(await screen.findAllByRole('button', { name: /Về nhà/ })).toHaveLength(2)
    expect(api.calls('GET', '/absences')[0][0]).toContain('residentId=5')
  })

  it('FR_049_giaHan_canLyDo_vaThoiDiemMoi', async () => {
    api.on('POST', '/absences/61/extend-expected-return', json(200, {}))
    renderPage()
    await chooseResident()
    fireEvent.click(await screen.findByRole('button', { name: /Về nhà.*dự kiến trở lại/ }))

    expect(await screen.findByText(/Gia đình xin thêm/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Gia hạn dự kiến trở lại…' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu gia hạn' }))
    expect(await screen.findByText('Nhập thời điểm dự kiến trở lại')).toBeInTheDocument()
    expect(screen.getByText('Nhập lý do')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Dự kiến trở lại'), { target: { value: '2026-11-12T10:00' } })
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Bệnh của người thân' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu gia hạn' }))
    await waitFor(() => expect(api.calls('POST', '/absences/61/extend-expected-return')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/absences/61/extend-expected-return')[0][1].body)).toEqual({
      expectedReturnAt: '2026-11-12T10:00:00+07:00',
      reason: 'Bệnh của người thân',
    })
  })

  it('BR_M02_07_giaiPhongGiuong_chiQuanLyVien', async () => {
    renderPage('OFFICE_STAFF')
    await chooseResident()
    fireEvent.click(await screen.findByRole('button', { name: /Về nhà.*dự kiến trở lại/ }))
    await screen.findByText(/Gia đình xin thêm/)
    expect(screen.queryByRole('button', { name: 'Giải phóng giường…' })).not.toBeInTheDocument()
  })

  it('BR_M02_07_quanLyVienGiaiPhongGiuong_kemLyDo', async () => {
    api.on('POST', '/absences/61/release-bed', json(200, OPEN))
    renderPage('NURSING_HOME_MANAGER')
    await chooseResident()
    fireEvent.click(await screen.findByRole('button', { name: /Về nhà.*dự kiến trở lại/ }))
    fireEvent.click(await screen.findByRole('button', { name: 'Giải phóng giường…' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận giải phóng giường' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Vắng quá 30 ngày' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận giải phóng giường' }))
    await waitFor(() => expect(api.calls('POST', '/absences/61/release-bed')).toHaveLength(1))
  })

  it('BR_M02_18_dinhChinhLuotDaDong_guiLoaiVaNoiDung', async () => {
    api.on('POST', '/absences/62/correct', json(201, {}))
    renderPage()
    await chooseResident()
    fireEvent.click(await screen.findByRole('button', { name: /Về nhà.*Đã đóng/ }))
    fireEvent.click(await screen.findByRole('button', { name: 'Đính chính lượt vắng…' }))
    fireEvent.change(screen.getByLabelText('Thời điểm về đúng'), { target: { value: '2026-10-11T09:00' } })
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Ghi nhầm giờ về' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))
    await waitFor(() => expect(api.calls('POST', '/absences/62/correct')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/absences/62/correct')[0][1].body)).toEqual({
      type: 'EDIT_CONTENT',
      correctedContent: { returnedAt: '2026-10-11T09:00:00+07:00' },
      reason: 'Ghi nhầm giờ về',
    })
  })

  it('FR_060_baoVangBanTru_hienKetQuaDuHan', async () => {
    api.on('POST', '/day-care-absence-notices', json(201, { withinNoticePeriod: false }))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Bán trú' }))
    await screen.findByText('Sửa chữa khu bán trú')
    await chooseResident()
    fireEvent.change(screen.getByLabelText('Ngày vắng'), { target: { value: '2026-11-12' } })
    fireEvent.change(screen.getByLabelText('Gia đình báo lúc'), { target: { value: '2026-11-11T20:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận báo vắng' }))

    expect(await screen.findByText(/Báo muộn/)).toBeInTheDocument()
    expect(JSON.parse(api.calls('POST', '/day-care-absence-notices')[0][1].body)).toEqual({
      residentId: 5,
      date: '2026-11-12',
      familyNotifiedAt: '2026-11-11T20:00:00+07:00',
    })
  })

  it('FR_060a_khaiBaoNgayNghi_ngayDaQuaCanLyDo409HienLoi', async () => {
    api.on(
      'POST',
      '/day-care-closure-days',
      problem(409, 'DAY_ALREADY_DECLARED', { detail: 'Ngày này đã được khai báo' }),
    )
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Bán trú' }))
    await screen.findByText('Sửa chữa khu bán trú')
    fireEvent.change(screen.getByLabelText('Ngày nghỉ'), { target: { value: '2026-11-20' } })
    fireEvent.change(screen.getByLabelText('Lý do nghỉ'), { target: { value: 'Sửa chữa' } })
    fireEvent.click(screen.getByRole('button', { name: 'Khai báo ngày nghỉ' }))
    expect(await screen.findByText('Ngày này đã được khai báo')).toBeInTheDocument()
  })

  it('FR_048_403_hienKhongCoQuyenXem', async () => {
    api.on('GET', '/absences', problem(403, 'PERMISSION_DENIED'))
    renderPage()
    await chooseResident()
    expect(await screen.findByText(/Bạn không có quyền xem/)).toBeInTheDocument()
  })
})
