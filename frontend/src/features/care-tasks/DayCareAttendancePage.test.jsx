import { beforeEach, afterEach, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { DayCareAttendancePage } from './DayCareAttendancePage'
let api
beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(200, [
      { function: 'DAY_CARE_ATTENDANCE', operation: 'VIEW' },
      { function: 'DAY_CARE_ATTENDANCE', operation: 'CREATE' },
    ]),
  )
  api.on(
    'GET',
    '/residents',
    json(200, { items: [{ id: 77, personalInfo: { fullName: '[Người kiểm thử]' } }], nextCursor: null }),
  )
  api.on(
    'GET',
    '/day-care-attendances',
    json(200, [
      {
        id: 5,
        residentId: 77,
        date: '2026-11-01',
        status: 'PRESENT',
        adHocSession: true,
        arrivedAt: '2026-11-01T01:00:00Z',
      },
    ]),
  )
})
afterEach(() => vi.unstubAllGlobals())
const view = () =>
  render(
    <PermissionsProvider>
      <DayCareAttendancePage initialDate="2026-11-01" />
    </PermissionsProvider>,
  )
it('FR_012_hienTrangThaiVaBuoiPhatSinhKhongTuDiemDanh', async () => {
  view()
  expect(await screen.findByText('Có mặt')).toBeInTheDocument()
  expect(screen.getByText('Buổi phát sinh')).toBeInTheDocument()
  expect(screen.getByRole('heading', { name: '[Người kiểm thử]' })).toBeInTheDocument()
  expect(api.calls('POST', '/day-care-attendances/check-in')).toHaveLength(0)
})
it('FR_013_diemDanhDenGiuDuLieuKhiLoiVaGiuKhoaKhiThuLai', async () => {
  api.on(
    'POST',
    '/day-care-attendances/check-in',
    problem(409, 'ATTENDANCE_STATE_CONFLICT', { detail: 'Không thể điểm danh ở trạng thái hiện tại.' }),
  )
  view()
  await screen.findByText('Có mặt')
  fireEvent.change(screen.getByLabelText('Người cao tuổi đến'), { target: { value: '77' } })
  fireEvent.click(screen.getByRole('button', { name: 'Điểm danh đến' }))
  await screen.findByText('Không thể điểm danh ở trạng thái hiện tại.')
  expect(screen.getByLabelText('Người cao tuổi đến')).toHaveValue('77')
  fireEvent.click(screen.getByRole('button', { name: 'Điểm danh đến' }))
  await waitFor(() => expect(api.calls('POST', '/day-care-attendances/check-in')).toHaveLength(2))
  const calls = api.calls('POST', '/day-care-attendances/check-in')
  expect(calls[0][1].headers['Idempotency-Key']).toBe(calls[1][1].headers['Idempotency-Key'])
})
it('BR_M04_02_diemDanhVeGuiBanGhiDonChuKhongTuChonLoaiVe', async () => {
  api.on(
    'POST',
    '/day-care-attendances/5/check-out',
    json(200, { attendance: { id: 5, status: 'LEFT' }, cancelledTaskIds: [21] }),
  )
  view()
  await screen.findByText('Có mặt')
  fireEvent.change(screen.getByLabelText('Bản ghi đón của dòng 5'), { target: { value: '90' } })
  fireEvent.click(screen.getByRole('button', { name: 'Điểm danh về dòng 5' }))
  await waitFor(() => expect(api.calls('POST', '/day-care-attendances/5/check-out')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/day-care-attendances/5/check-out')[0][1].body)).toEqual({ pickupRecordId: 90 })
})
it('FR_049_quyenXemKhongHienLenhGhi', async () => {
  api.on('GET', '/me/permissions', json(200, [{ function: 'DAY_CARE_ATTENDANCE', operation: 'VIEW' }]))
  view()
  await screen.findByText('Có mặt')
  expect(screen.queryByRole('button', { name: 'Điểm danh đến' })).not.toBeInTheDocument()
})

it('FR_013_khoaNgayTrongKhiDangGuiLenhDeKhongTronHaiNgay', async () => {
  let release
  api.on(
    'POST',
    '/day-care-attendances/check-in',
    () =>
      new Promise((resolve) => {
        release = resolve
      }),
  )
  view()
  await screen.findByText('Có mặt')
  fireEvent.change(screen.getByLabelText('Người cao tuổi đến'), { target: { value: '77' } })
  fireEvent.click(screen.getByRole('button', { name: 'Điểm danh đến' }))
  await waitFor(() => expect(release).toBeTypeOf('function'))
  try {
    expect(screen.getByLabelText('Ngày xem trạng thái')).toBeDisabled()
  } finally {
    release(json(200, {}))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Điểm danh đến' })).not.toBeDisabled())
  }
})

it('FR_012_doiNgayAnTrangThaiCuTrongKhiTaiVaKhiLoi', async () => {
  view()
  await screen.findByText('Có mặt')
  let release
  api.on(
    'GET',
    '/day-care-attendances',
    () =>
      new Promise((resolve) => {
        release = resolve
      }),
  )
  fireEvent.change(screen.getByLabelText('Ngày xem trạng thái'), { target: { value: '2026-11-02' } })
  await screen.findByText('Đang tải trạng thái có mặt…')
  try {
    expect(screen.queryByText('Có mặt')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Điểm danh về dòng 5' })).not.toBeInTheDocument()
  } finally {
    release(problem(503, 'SOURCE_UNAVAILABLE', { detail: 'Nguồn lịch chưa sẵn sàng.' }))
    await screen.findByText('Nguồn lịch chưa sẵn sàng.')
  }
  expect(screen.queryByText('Chưa có trạng thái có mặt trong ngày đã chọn.')).not.toBeInTheDocument()
})
